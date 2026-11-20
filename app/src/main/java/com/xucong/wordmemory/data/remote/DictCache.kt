package com.xucong.wordmemory.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.xucong.wordmemory.domain.dict.DictEntry
import com.xucong.wordmemory.domain.dict.DictExample

/**
 * 在线词典结果的本地缓存。
 *
 * 为什么放 SharedPreferences 而不是 Room：这是**可丢弃的缓存**，不是用户数据。
 * 塞进 Room 就得为它改 schema、写 Migration，还要在「清缓存」时区别对待；
 * 而缓存丢了最多是再联网查一次，代价接近零。
 *
 * 序列化用 Gson：一行 `toJson/fromJson`，不必手写字段名常量。
 */
interface DictCache {
    fun get(term: String): DictEntry?

    fun put(entry: DictEntry, savedAt: Long)
}

class PrefsDictCache(
    context: Context,
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
    private val gson: Gson = Gson(),
) : DictCache {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun get(term: String): DictEntry? {
        val raw = prefs.getString(key(term), null) ?: return null
        val payload = try {
            gson.fromJson(raw, CachedEntry::class.java)
        } catch (syntax: JsonSyntaxException) {
            null
        } ?: return null

        if (payload.savedAt <= 0L) return null
        if (System.currentTimeMillis() - payload.savedAt > ttlMillis) return null

        return DictEntry(
            term = payload.term.ifBlank { term },
            usPhone = payload.us,
            ukPhone = payload.uk,
            definitions = payload.defs,
            examples = payload.examples.map { DictExample(english = it.en, chinese = it.zh) },
            audioUrl = payload.audio,
        ).takeIf { it.hasContent }
    }

    override fun put(entry: DictEntry, savedAt: Long) {
        val payload = CachedEntry(
            term = entry.term,
            us = entry.usPhone,
            uk = entry.ukPhone,
            defs = entry.definitions,
            examples = entry.examples.map { CachedExample(en = it.english, zh = it.chinese) },
            audio = entry.audioUrl,
            savedAt = savedAt,
        )
        prefs.edit().putString(key(entry.term), gson.toJson(payload)).apply()
    }

    private fun key(term: String): String = "entry:" + term.trim().lowercase()

    /** 清空缓存；「我的」页的清缓存入口与联调用例都会用到。 */
    fun clear() {
        prefs.edit().clear().apply()
    }

    /** 已缓存的词条数量，仅用于「我的」页展示，不参与任何逻辑判断。 */
    fun size(): Int = prefs.all.size

    private data class CachedEntry(
        val term: String = "",
        val us: String = "",
        val uk: String = "",
        val defs: List<String> = emptyList(),
        val examples: List<CachedExample> = emptyList(),
        val audio: String = "",
        val savedAt: Long = 0L,
    )

    private data class CachedExample(
        val en: String = "",
        val zh: String = "",
    )

    companion object {
        private const val PREFS_NAME = "dict_cache"
        private const val DEFAULT_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}
