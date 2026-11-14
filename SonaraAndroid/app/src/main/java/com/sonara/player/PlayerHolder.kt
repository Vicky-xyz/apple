package com.sonara.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Visualizer
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

/** One ExoPlayer shared by the UI and the background service, plus fade, sleep timer and audio effects. */
object PlayerHolder {
    private var player: ExoPlayer? = null
    private var eq: Equalizer? = null
    private var boost: BassBoost? = null
    private var eqPreset = 0
    private var boostStrength = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var appCtx: Context? = null

    // audio-reactive visualizer
    var vizLevels by mutableStateOf(FloatArray(32))
    @Volatile var vizVisible = true
    private var viz: Visualizer? = null
    private var vizWanted = false
    private var fft = ByteArray(0)
    private val smooth = FloatArray(32)

    fun peek(): ExoPlayer? = player

    var fadeMs by mutableLongStateOf(0L)
    var sleepEndAt by mutableLongStateOf(0L)
    var sleepAtTrackEnd by mutableStateOf(false)

    fun get(ctx: Context): ExoPlayer {
        player?.let { return it }
        appCtx = ctx.applicationContext
        val p = ExoPlayer.Builder(ctx.applicationContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        p.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                rebuildEffects()
                rebuildVisualizer()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                refreshWidget()
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                refreshWidget()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                refreshWidget()
                val auto = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
                if (sleepAtTrackEnd && auto) {
                    p.pause()
                    sleepAtTrackEnd = false
                }
            }
        })
        player = p
        rebuildEffects()
        scope.launch { volumeLoop(p) }
        scope.launch { vizLoop(p) }
        return p
    }

    fun startSleep(minutes: Int) {
        sleepAtTrackEnd = false
        sleepEndAt = SystemClock.elapsedRealtime() + minutes * 60_000L
    }

    fun sleepAtEndOfTrack() {
        sleepEndAt = 0L
        sleepAtTrackEnd = true
    }

    fun cancelSleep() {
        sleepEndAt = 0L
        sleepAtTrackEnd = false
    }

    fun setEffects(preset: Int, bass: Int) {
        eqPreset = preset
        boostStrength = bass
        rebuildEffects()
    }

    private fun refreshWidget() {
        try {
            appCtx?.let { SonaraWidget.update(it) }
        } catch (_: Throwable) {
        }
    }

    fun setVisualizer(on: Boolean) {
        vizWanted = on
        rebuildVisualizer()
    }

    private fun rebuildVisualizer() {
        try { viz?.release() } catch (_: Throwable) {}
        viz = null
        if (!vizWanted) return
        val p = player ?: return
        val id = p.audioSessionId
        if (id == C.AUDIO_SESSION_ID_UNSET) return
        try {
            val v = Visualizer(id)
            val range = Visualizer.getCaptureSizeRange()
            v.setCaptureSize(1024.coerceIn(range[0], range[1]))
            v.setEnabled(true)
            fft = ByteArray(v.captureSize)
            viz = v
        } catch (_: Throwable) {
        }
    }

    private suspend fun vizLoop(p: ExoPlayer) {
        val n = 32
        while (true) {
            delay(40)
            val v = viz
            if (v == null || !vizVisible || !p.isPlaying) continue
            if (v.getFft(fft) != Visualizer.SUCCESS) continue
            val bins = fft.size / 2
            val usable = (bins * 0.6f).toInt().coerceAtLeast(n + 2)
            val out = FloatArray(n)
            for (i in 0 until n) {
                val lo = 1 + ((usable - 1) * (i.toFloat() / n).pow(2)).toInt()
                val hi = maxOf(lo + 1, 1 + ((usable - 1) * ((i + 1).toFloat() / n).pow(2)).toInt())
                var sum = 0f
                var cnt = 0
                for (k in lo until minOf(hi, bins)) {
                    val re = fft[2 * k].toInt().toFloat()
                    val im = fft[2 * k + 1].toInt().toFloat()
                    sum += hypot(re, im)
                    cnt++
                }
                val mag = if (cnt > 0) sum / cnt else 0f
                val lvl = sqrt((mag / 70f).coerceIn(0f, 1f))
                smooth[i] = maxOf(lvl, smooth[i] * 0.82f)
                out[i] = smooth[i]
            }
            vizLevels = out
        }
    }

    private suspend fun volumeLoop(p: ExoPlayer) {
        while (true) {
            delay(120)
            var v = 1f
            val fade = fadeMs
            if (fade > 0 && p.isPlaying) {
                val pos = p.currentPosition
                val dur = p.duration
                if (pos < fade) v = minOf(v, (pos.toFloat() / fade).coerceIn(0f, 1f))
                if (dur > 0 && dur - pos < fade) v = minOf(v, ((dur - pos).toFloat() / fade).coerceIn(0f, 1f))
            }
            if (sleepEndAt > 0) {
                val left = sleepEndAt - SystemClock.elapsedRealtime()
                if (left <= 0) {
                    p.pause()
                    sleepEndAt = 0L
                    v = 1f
                } else if (left < 8000) {
                    v = minOf(v, left / 8000f)
                }
            }
            if (abs(p.volume - v) > 0.01f) p.volume = v
        }
    }

    private fun rebuildEffects() {
        val p = player ?: return
        val id = p.audioSessionId
        if (id == C.AUDIO_SESSION_ID_UNSET) return
        try { eq?.release() } catch (_: Throwable) {}
        try { boost?.release() } catch (_: Throwable) {}
        eq = null
        boost = null
        try {
            val e = Equalizer(0, id)
            val n = e.numberOfBands.toInt()
            val range = e.bandLevelRange
            val curve = EQ_PRESETS[eqPreset.coerceIn(0, EQ_PRESETS.lastIndex)].gains
            for (b in 0 until n) {
                val t = if (n <= 1) 0f else b.toFloat() / (n - 1) * (curve.size - 1)
                val lo = t.toInt().coerceAtMost(curve.size - 2)
                val db = curve[lo] + (curve[lo + 1] - curve[lo]) * (t - lo)
                val mb = (db * 100).toInt().coerceIn(range[0].toInt(), range[1].toInt())
                e.setBandLevel(b.toShort(), mb.toShort())
            }
            e.setEnabled(true)
            eq = e
        } catch (_: Throwable) {
        }
        try {
            val bb = BassBoost(0, id)
            if (bb.strengthSupported) {
                bb.setStrength(boostStrength.coerceIn(0, 1000).toShort())
                bb.setEnabled(boostStrength > 0)
            }
            boost = bb
        } catch (_: Throwable) {
        }
    }
}
