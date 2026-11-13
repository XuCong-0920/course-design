package com.xucong.wordmemory.ui.common

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * 从 SAF 拿到的 Uri 里读文本。
 *
 * 两个必须防的坑：
 * 1. 用户可能选中云盘里的 5GB 文件 —— 加 [MAX_BYTES] 上限，超了直接报错而不是 OOM；
 * 2. 文件名要走 `OpenableColumns.DISPLAY_NAME` 查，不能拿 Uri 最后一段当名字（那种是一串数字）。
 *
 * 都是阻塞 IO，调用方要放在 Dispatchers.IO 上执行。
 */
object DocumentTextReader {

    /** 词库文件再大不该超过 1MB（一万个词大约 300KB）；超过说明选错文件了。 */
    private const val MAX_BYTES = 1_000_000

    fun displayName(context: Context, uri: Uri): String {
        val fromQuery = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                }
        }.getOrNull()

        return fromQuery?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "未知文件"
    }

    @Throws(IOException::class)
    fun readText(context: Context, uri: Uri): String {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("无法打开这个文件")
        stream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_BYTES) {
                    throw IOException("文件超过 ${MAX_BYTES / 1000} KB，请确认选的是词库文本")
                }
                output.write(buffer, 0, read)
            }
            return output.toString(Charsets.UTF_8.name())
        }
    }
}
