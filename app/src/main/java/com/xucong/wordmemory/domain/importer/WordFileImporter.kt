package com.xucong.wordmemory.domain.importer

/**
 * 词库文件导入的**纯 Kotlin** 解析层：不碰 Context / Uri / InputStream，
 * 因此可以在 JVM 单元测试里用字符串把各种坏文件试一遍，不需要模拟器。
 *
 * 支持的两种格式：
 *
 * ```csv
 * term,phonetic,meaning,example        # 表头可选
 * abandon,/əˈbændən/,v. 放弃；抛弃,He abandoned the plan.
 * ```
 *
 * ```json
 * { "name": "四级高频", "words": [ { "term": "abandon", "phonetic": "…", "meaning": "…" } ] }
 * ```
 * JSON 也接受直接的数组：`[ { "term": … }, … ]`。
 *
 * 设计原则：**坏行不抛异常，只记录下来**。导入五十个词，不能因为第 7 行少个逗号就整份失败。
 */
data class ImportWord(
    val term: String,
    val phonetic: String,
    val meaning: String,
    val example: String,
)

/** 一行/一个对象为什么被跳过；[line] 是文件里的行号（JSON 用对象序号），方便用户回去改。 */
data class ImportProblem(
    val line: Int,
    val reason: String,
)

data class ImportPreview(
    val deckName: String,
    val words: List<ImportWord>,
    val problems: List<ImportProblem>,
    /** 文件内部自己重复的词（同一份文件里出现两次）。 */
    val duplicateCount: Int,
) {
    val isEmpty: Boolean get() = words.isEmpty()

    /** 用户看到的「共读到 N 个，其中 M 个可用」。 */
    val total: Int get() = words.size + problems.size + duplicateCount
}

object WordFileImporter {

    const val MAX_TERM_LENGTH = 64
    private const val CSV_EXTENSION = "csv"
    private const val JSON_EXTENSION = "json"
    private const val FALLBACK_DECK_NAME = "导入的词库"
    private const val MAX_EXAMPLE_LENGTH = 300

    private val HEADER_HINTS = setOf("term", "word", "单词", "词汇")

    /**
     * @param fileName 只用于：推断格式（扩展名）与默认词库名（去掉扩展名）
     * @param content  文件全文
     */
    fun parse(fileName: String, content: String): ImportPreview {
        val text = content.removePrefix("\uFEFF")
        val nameFromFile = deckNameFromFile(fileName)
        return when {
            looksLikeJson(text) -> parseJson(nameFromFile, text)
            else -> parseCsv(nameFromFile, text)
        }
    }

    private fun looksLikeJson(text: String): Boolean {
        val first = text.firstOrNull { !it.isWhitespace() } ?: return false
        return first == '{' || first == '['
    }

    private fun deckNameFromFile(fileName: String): String {
        val base = fileName.substringAfterLast('/').substringAfterLast('\\')
        val withoutExtension = base.substringBeforeLast('.', base).trim()
        return withoutExtension.ifBlank { FALLBACK_DECK_NAME }
    }

    // ---------------------------------------------------------------- CSV

    private fun parseCsv(deckName: String, text: String): ImportPreview {
        val lines = text.split("\r\n", "\n", "\r")
        val words = ArrayList<ImportWord>()
        val problems = ArrayList<ImportProblem>()
        val seen = HashSet<String>()
        var duplicates = 0
        var headerSkipped = false
        var firstContentLine = true

        lines.forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            if (rawLine.isBlank()) return@forEachIndexed

            val cells = splitCsvLine(rawLine)

            // 表头：只在第一行内容上判断，且第一格长得像「单词」列名
            if (firstContentLine && !headerSkipped) {
                firstContentLine = false
                val first = cells.firstOrNull()?.trim()?.lowercase()
                if (first != null && first in HEADER_HINTS) {
                    headerSkipped = true
                    return@forEachIndexed
                }
            }

            if (cells.size < 2) {
                problems += ImportProblem(lineNumber, "至少需要「单词,释义」两列")
                return@forEachIndexed
            }

            val term = cells[0].trim()
            // 两列时第二列就是释义；三列以上时第二列是音标、第三列是释义
            val phonetic = if (cells.size >= 3) cells[1].trim() else ""
            val meaning = if (cells.size >= 3) cells[2].trim() else cells[1].trim()
            val example = if (cells.size >= 4) cells[3].trim() else ""

            val problem = validate(term, meaning)
            if (problem != null) {
                problems += ImportProblem(lineNumber, problem)
                return@forEachIndexed
            }

            val key = term.lowercase()
            if (!seen.add(key)) {
                duplicates++
                return@forEachIndexed
            }

            words += ImportWord(
                term = term,
                phonetic = phonetic,
                meaning = meaning,
                example = example.take(MAX_EXAMPLE_LENGTH),
            )
        }

        return ImportPreview(deckName, words, problems, duplicates)
    }

    /** 处理引号包裹、字段内逗号、转义双引号（""）与 CRLF，够用且不引第三方 CSV 库。 */
    private fun splitCsvLine(line: String): List<String> {
        val cells = ArrayList<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                inQuotes && ch == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }

                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    cells += current.toString()
                    current.setLength(0)
                }

                else -> current.append(ch)
            }
            i++
        }
        cells += current.toString()
        return cells
    }

    // --------------------------------------------------------------- JSON

    private fun parseJson(deckName: String, text: String): ImportPreview {
        val cursor = JsonCursor(text)
        return try {
            cursor.skipWhitespace()
            when (cursor.peek()) {
                '[' -> parseJsonArray(deckName, cursor)
                '{' -> parseJsonObject(deckName, cursor)
                else -> ImportPreview(
                    deckName = deckName,
                    words = emptyList(),
                    problems = listOf(ImportProblem(1, "既不是数组也不是对象")),
                    duplicateCount = 0,
                )
            }
        } catch (error: JsonFormatException) {
            ImportPreview(
                deckName = deckName,
                words = emptyList(),
                problems = listOf(ImportProblem(error.line ?: 1, error.message ?: "JSON 格式错误")),
                duplicateCount = 0,
            )
        }
    }

    private fun parseJsonArray(deckName: String, cursor: JsonCursor): ImportPreview {
        val words = ArrayList<ImportWord>()
        val problems = ArrayList<ImportProblem>()
        val seen = HashSet<String>()
        var duplicates = 0
        var index = 0

        cursor.expect('[')
        cursor.skipWhitespace()
        if (cursor.peek() == ']') {
            cursor.index++
            return ImportPreview(deckName, words, problems, 0)
        }
        while (true) {
            index++
            val fields = cursor.readObject()
            val term = fields["term"].orEmpty().trim()
            val meaning = fields["meaning"].orEmpty().trim()
            val problem = validate(term, meaning)
            if (problem != null) {
                problems += ImportProblem(index, problem)
            } else if (!seen.add(term.lowercase())) {
                duplicates++
            } else {
                words += ImportWord(
                    term = term,
                    phonetic = fields["phonetic"].orEmpty().trim(),
                    meaning = meaning,
                    example = fields["example"].orEmpty().trim().take(MAX_EXAMPLE_LENGTH),
                )
            }
            cursor.skipWhitespace()
            when (cursor.peek()) {
                ',' -> {
                    cursor.index++
                    cursor.skipWhitespace()
                }

                ']' -> {
                    cursor.index++
                    return ImportPreview(deckName, words, problems, duplicates)
                }

                else -> throw JsonFormatException(1, "数组元素之间缺少逗号")
            }
        }
    }

    private fun parseJsonObject(deckName: String, cursor: JsonCursor): ImportPreview {
        // 对象形态：{ "name": "...", "words": [ ... ] }
        // 先整体读成字符串键值；words 是数组，单独处理
        var resolvedName = deckName
        var arrayPreview: ImportPreview? = null

        cursor.expect('{')
        cursor.skipWhitespace()
        if (cursor.peek() == '}') {
            cursor.index++
            return ImportPreview(deckName, emptyList(), emptyList(), 0)
        }
        while (true) {
            val key = cursor.readString().trim()
            cursor.skipWhitespace()
            cursor.expect(':')
            cursor.skipWhitespace()
            when (key.lowercase()) {
                "name", "deck", "title" -> resolvedName = cursor.readString().trim().ifBlank { deckName }
                "words", "items", "list" -> {
                    if (cursor.peek() != '[') throw JsonFormatException(null, "words 应该是数组")
                    arrayPreview = parseJsonArray(resolvedName, cursor)
                }

                else -> cursor.skipValue()
            }
            cursor.skipWhitespace()
            when (cursor.peek()) {
                ',' -> {
                    cursor.index++
                    cursor.skipWhitespace()
                }

                '}' -> {
                    cursor.index++
                    return arrayPreview?.copy(deckName = resolvedName)
                        ?: ImportPreview(resolvedName, emptyList(), emptyList(), 0)
                }

                else -> throw JsonFormatException(null, "对象字段之间缺少逗号")
            }
        }
    }

    // ------------------------------------------------------------- 共用

    private fun validate(term: String, meaning: String): String? = when {
        term.isBlank() -> "单词为空"
        meaning.isBlank() -> "释义为空"
        term.length > MAX_TERM_LENGTH -> "单词超过 $MAX_TERM_LENGTH 个字符"
        else -> null
    }
}

private class JsonFormatException(val line: Int?, message: String) : Exception(message)

/**
 * 只为这份文件格式服务的极简 JSON 游标：不引任何 JSON 库，
 * 因为 domain 层要保持纯 Kotlin（能在 JVM 单测里跑），而 org.json 只有 Android 运行时有。
 */
private class JsonCursor(private val text: String) {

    var index: Int = 0

    fun peek(): Char {
        if (index >= text.length) throw JsonFormatException(null, "JSON 意外结束")
        return text[index]
    }

    fun expect(ch: Char) {
        if (peek() != ch) throw JsonFormatException(null, "这里应该是 '$ch'")
        index++
    }

    fun skipWhitespace() {
        while (index < text.length && text[index].isWhitespace()) index++
    }

    fun readString(): String {
        if (peek() != '"') throw JsonFormatException(null, "这里应该是一个字符串")
        index++
        val builder = StringBuilder()
        while (true) {
            if (index >= text.length) throw JsonFormatException(null, "字符串没有闭合")
            val ch = text[index]
            when (ch) {
                '"' -> {
                    index++
                    return builder.toString()
                }

                '\\' -> {
                    index++
                    if (index >= text.length) throw JsonFormatException(null, "转义符后没有字符")
                    when (val escaped = text[index]) {
                        'n' -> builder.append('\n')
                        't' -> builder.append('\t')
                        'r' -> builder.append('\r')
                        '"', '\\', '/' -> builder.append(escaped)
                        'u' -> {
                            if (index + 4 >= text.length) {
                                throw JsonFormatException(null, "\\u 转义不完整")
                            }
                            val hex = text.substring(index + 1, index + 5)
                            builder.append(hex.toInt(16).toChar())
                            index += 4
                        }

                        else -> throw JsonFormatException(null, "不认识的转义 \\$escaped")
                    }
                    index++
                }

                else -> {
                    builder.append(ch)
                    index++
                }
            }
        }
    }

    /** 读一个对象，值只接受字符串（够用：词条字段全是文本）；其他类型跳过。 */
    fun readObject(): Map<String, String> {
        val fields = HashMap<String, String>()
        expect('{')
        skipWhitespace()
        if (peek() == '}') {
            index++
            return fields
        }
        while (true) {
            skipWhitespace()
            val key = readString().trim().lowercase()
            skipWhitespace()
            expect(':')
            skipWhitespace()
            if (peek() == '"') {
                fields[key] = readString()
            } else {
                skipValue()
            }
            skipWhitespace()
            when (peek()) {
                ',' -> {
                    index++
                    skipWhitespace()
                }

                '}' -> {
                    index++
                    return fields
                }

                else -> throw JsonFormatException(null, "对象字段之间缺少逗号")
            }
        }
    }

    fun skipValue() {
        skipWhitespace()
        if (index >= text.length) throw JsonFormatException(null, "JSON 意外结束")
        when (val ch = text[index]) {
            '"' -> readString()
            '{', '[' -> {
                var depth = 0
                var inString = false
                while (index < text.length) {
                    val current = text[index]
                    when {
                        inString && current == '\\' -> index++
                        current == '"' -> inString = !inString
                        !inString && current in "{[" -> depth++
                        !inString && current in "}]" -> {
                            depth--
                            if (depth == 0) {
                                index++
                                return
                            }
                        }
                    }
                    index++
                }
                throw JsonFormatException(null, "嵌套结构没有闭合")
            }

            else -> {
                // 数字 / true / false / null
                while (index < text.length && !text[index].isWhitespace() && text[index] !in ",}]") index++
                if (ch.isWhitespace()) throw JsonFormatException(null, "缺少数值")
            }
        }
    }
}
