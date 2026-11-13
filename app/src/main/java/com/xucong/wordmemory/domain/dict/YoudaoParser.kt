package com.xucong.wordmemory.domain.dict

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * 解析有道 `https://dict.youdao.com/jsonapi?q=<word>` 的响应。
 *
 * 用 Gson 的 JsonObject 树做防御式取值：字段缺失、类型不对、JSON 损坏都返回 null 或空，
 * 不抛异常——词典是锦上添花的功能，不能因为它把详情页搞崩。
 */
object YoudaoParser {

    const val MAX_EXAMPLES = 3

    const val MAX_DEFINITIONS = 4

    fun parse(term: String, json: String): DictEntry? {
        if (term.isBlank() || json.isBlank()) return null
        val root = runCatching { JsonParser.parseString(json) }.getOrNull()
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
            ?: return null

        // ec（英汉）优先，simple（简明释义）兜底
        val wordNode = firstWordNode(root.obj("ec")) ?: firstWordNode(root.obj("simple"))
        val entry = DictEntry(
            term = term.trim(),
            usPhone = wordNode.element("usphone").string(),
            ukPhone = wordNode.element("ukphone").string(),
            definitions = readDefinitions(wordNode),
            examples = readExamples(root.obj("blng_sents_part")),
            audioUrl = audioUrlOf(term),
        )
        return entry.takeIf { it.hasContent }
    }

    /** 发音音频地址：有道的 dictvoice 直接按单词返回 mp3，不需要再请求一次 JSON。type=2 美音、1 英音。 */
    fun audioUrlOf(term: String, type: Int = 2): String =
        "https://dict.youdao.com/dictvoice?audio=${java.net.URLEncoder.encode(term.trim(), "UTF-8")}&type=$type"

    // ------------------------------------------------------------ 内部取值

    private fun JsonObject?.obj(key: String): JsonObject? =
        this?.get(key)?.takeIf { it.isJsonObject }?.asJsonObject

    /**
     * 取原始节点，不做类型收窄。
     *
     * 这里必须用 `get` 而不是 `obj`：有道把音标、例句正文放在**字符串**里，
     * 把释义放在**数组**里，只有 `ec` / `simple` / `blng_sents_part` 是对象。
     */
    private fun JsonObject?.element(key: String): JsonElement? = this?.get(key)

    private fun JsonObject?.arr(key: String): JsonArray? =
        this?.get(key)?.takeIf { it.isJsonArray }?.asJsonArray

    private fun JsonElement?.string(): String = when {
        this == null || isJsonNull -> ""
        isJsonPrimitive -> asString
        else -> ""
    }

    /** `/ec/word/0` 或 `/simple/word/0`。 */
    private fun firstWordNode(section: JsonObject?): JsonObject? =
        section?.arr("word")?.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject

    private fun readDefinitions(wordNode: JsonObject?): List<String> {
        val result = ArrayList<String>()
        val trs = wordNode?.arr("trs") ?: return result
        for (trsItem in trs) {
            val translations = trsItem.asJsonObject.arr("tr") ?: continue
            for (translation in translations) {
                val lines = translation.asJsonObject.obj("l").element("i")
                // 有道这里既可能是数组也可能直接是字符串
                val texts = when {
                    lines == null -> emptyList()
                    lines.isJsonArray -> lines.asJsonArray.map { it.string() }
                    else -> listOf(lines.string())
                }
                texts.map { it.trim() }.filter { it.isNotEmpty() }.forEach { text ->
                    result += text
                }
                if (result.size >= MAX_DEFINITIONS) return result.take(MAX_DEFINITIONS)
            }
        }
        return result
    }

    private fun readExamples(section: JsonObject?): List<DictExample> {
        val pairs = section?.arr("sentence-pair") ?: return emptyList()
        val result = ArrayList<DictExample>()
        for (pair in pairs) {
            val obj = pair.asJsonObject
            val english = obj.element("sentence").string().trim()
            if (english.isEmpty()) continue
            result += DictExample(
                english = english,
                chinese = obj.element("sentence-translation").string().trim(),
            )
            if (result.size >= MAX_EXAMPLES) break
        }
        return result
    }
}
