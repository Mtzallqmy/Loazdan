package com.lughzdan.games

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import com.lughzdan.engine.FeedbackCue
import java.util.Collections

/** Decodes small local cues asynchronously and releases native resources with the screen. */
internal class GameAudio(context: Context) {
    private val ready = Collections.synchronizedSet(mutableSetOf<Int>())
    private val fallback = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 25) }.getOrNull()
    private val pool = runCatching {
        SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        ).build()
    }.getOrNull()
    private val sounds: Map<FeedbackCue, Int>

    init {
        pool?.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready.add(id) }
        sounds = mapOf(
            FeedbackCue.SUCCESS to (pool?.load(context, R.raw.answer_success, 1) ?: 0),
            FeedbackCue.RETRY to (pool?.load(context, R.raw.answer_retry, 1) ?: 0),
            FeedbackCue.DISCOVERY to (pool?.load(context, R.raw.word_discovery, 1) ?: 0),
        )
    }

    fun play(cue: FeedbackCue) {
        val id = sounds[cue] ?: return
        if (id in ready) pool?.play(id, 0.65f, 0.65f, 1, 0, 1f)
        else fallback?.startTone(when (cue) {
            FeedbackCue.SUCCESS -> ToneGenerator.TONE_PROP_ACK
            FeedbackCue.RETRY -> ToneGenerator.TONE_PROP_NACK
            FeedbackCue.DISCOVERY -> ToneGenerator.TONE_PROP_BEEP
        }, 120)
    }

    fun close() {
        pool?.setOnLoadCompleteListener(null)
        pool?.release()
        fallback?.release()
        ready.clear()
    }
}
