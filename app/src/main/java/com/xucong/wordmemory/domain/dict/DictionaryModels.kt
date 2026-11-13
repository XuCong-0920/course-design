package com.xucong.wordmemory.domain.dict

/** 一条双语例句：英文原句 + 中文翻译（有道返回的 sentence-pair）。 */
data class DictExample(
    val english: String,
    val chinese: String,
)

/**
 * 在线词典查到的补充信息。只保留 App 真正会展示的字段，避免把整份响应搬进内存。
 */
data class DictEntry(
    val term: String,
    val usPhone: String = "",
    val ukPhone: String = "",
    val definitions: List<String> = emptyList(),
    val examples: List<DictExample> = emptyList(),
    val audioUrl: String = "",
) {

    /** 美音优先，没有就退回英音；都没有返回空串。 */
    val phonetic: String get() = usPhone.ifBlank { ukPhone }

    val hasContent: Boolean
        get() = definitions.isNotEmpty() || examples.isNotEmpty() || phonetic.isNotBlank()
}
