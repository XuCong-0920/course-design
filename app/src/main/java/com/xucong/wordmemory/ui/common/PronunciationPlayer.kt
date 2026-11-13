package com.xucong.wordmemory.ui.common

import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * 在线发音播放器：直接流式播放 mp3，不落盘。
 *
 * 生命周期由调用方管（Fragment.onDestroyView 里 release），
 * 因为 MediaPlayer 持有解码器资源，靠 GC 回收太晚。
 */
class PronunciationPlayer {

    private var player: MediaPlayer? = null

    fun play(url: String, onError: (String) -> Unit) {
        stop()
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                setDataSource(url)
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, what, extra ->
                    onError("解码失败（$what/$extra）")
                    true
                }
                prepareAsync()
            }
        } catch (error: Exception) {
            stop()
            onError(error.message ?: error.javaClass.simpleName)
        }
    }

    fun stop() {
        player?.runCatching {
            if (isPlaying) stop()
            release()
        }
        player = null
    }
}
