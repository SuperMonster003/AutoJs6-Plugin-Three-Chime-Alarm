package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingCoordinator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingVolume
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.SpeechDirector

/**
 * One sounding round (roadmap P2.4): the sound, the fade in, the vibration, the voice and the auto silence timer of
 * [ring], from [begin] to [stop]. The steps of [SpeechDirector] decide when the voice speaks and how loud the sound is
 * meanwhile; a ring that is silent by itself plays a sound once the voice turns out unavailable (T63). Used on the main
 * thread only.
 *
 * zh-CN: 发声的一轮 (路线图 P2.4): [ring] 的铃声, 渐强, 振动, 朗读与自动停止计时, 从 [begin] 到 [stop]. 由 [SpeechDirector] 的每一步决定何时朗读以及
 * 朗读期间铃声多大; 本身不出声的响铃在朗读不可用时改为播放铃声 (T63). 只在主线程使用.
 */
internal class RingSession(context: Context, private val runtime: AlarmRuntime, var ring: RingCoordinator.Ring) : RingSpeaker.Listener {

    val occurrence: String = ring.occurrence

    private val settings = ring.settings

    private val main = Handler(Looper.getMainLooper())

    private val player = RingPlayer(context)

    private val vibrator = RingVibrator(context)

    private val speaker: RingSpeaker? = settings.speak?.let { RingSpeaker(context, it, this) }

    private val director: SpeechDirector? = settings.speak?.let { SpeechDirector(settings.speakMode, it.repeatInterval ?: 0) }

    private var step = SpeechDirector.Step(SpeechDirector.Sound.FULL)

    private var speechFailed = false

    private var soundSince: Long? = null

    private var stopped = false

    private val due = Runnable { director?.let { perform(it.due(now())) } }

    private val ramp = object : Runnable {
        override fun run() {
            applySound()
            val since = soundSince
            if (since != null && now() - since < settings.rampSeconds * 1000L) main.postDelayed(this, RAMP_TICK_MILLIS)
        }
    }

    private val silence = Runnable {
        runtime.executor.execute {
            try {
                runtime.rings.silence(occurrence)
            } catch (e: RuntimeException) {
                Log.e(RingService.TAG, "auto silence failed", e)
            }
        }
    }

    fun begin() {
        runtime.volume.apply(settings.volume)
        settings.vibration?.let(vibrator::start)
        perform(director?.start(now()) ?: SpeechDirector.Step(SpeechDirector.Sound.FULL))
        val silenceAt = ring.silenceAt ?: (System.currentTimeMillis() + settings.autoSilenceMillis)
        main.postDelayed(silence, (silenceAt - System.currentTimeMillis()).coerceAtLeast(0L))
        Log.i(
            RingService.TAG,
            "ring session ${ring.alarmId} occurrence=$occurrence sound=${settings.soundSource(false) != null} player=${player.playingWhat} " +
                "vibrate=${settings.vibration != null} speak=${settings.speak?.let { settings.speakMode } ?: "none"} volume=${settings.volume} " +
                "ramp=${settings.rampSeconds} snooze=${ring.canSnooze} silenceIn=${silenceAt - System.currentTimeMillis()}",
        )
    }

    /** Asks for audio focus again (the ringing screen came to the front, T35). zh-CN: 再次申请音频焦点 (响铃界面到前台, T35). */
    fun requestFocus() {
        if (!stopped && soundSince != null) player.requestFocus()
    }

    fun stop() {
        if (stopped) return
        stopped = true
        main.removeCallbacks(due)
        main.removeCallbacks(ramp)
        main.removeCallbacks(silence)
        player.release()
        vibrator.stop()
        speaker?.shutdown()
        Log.i(RingService.TAG, "ring session ${ring.alarmId} ended")
    }

    override fun onSpoken() {
        director?.let { perform(it.spoken(now())) }
    }

    override fun onUnavailable() {
        speechFailed = true
        director?.let { perform(it.unavailable()) }
    }

    private fun perform(next: SpeechDirector.Step) {
        if (stopped) return
        step = next
        applySound()
        if (next.speak) speaker?.speak()
        main.removeCallbacks(due)
        next.wakeAt?.let { main.postDelayed(due, (it - now()).coerceAtLeast(0L)) }
    }

    private fun applySound() {
        if (stopped) return
        val source = settings.soundSource(speechFailed)?.takeIf { step.sound != SpeechDirector.Sound.OFF }
        if (source == null) {
            player.set(null, 0f)
            return
        }
        val since = soundSince ?: now().also {
            soundSince = it
            if (settings.rampSeconds > 0) main.postDelayed(ramp, RAMP_TICK_MILLIS)
        }
        var gain = RingVolume.rampGain(now() - since, settings.rampSeconds) * RingVolume.playerGain(settings.volume)
        if (step.sound == SpeechDirector.Sound.DUCKED) gain *= SpeechDirector.DUCK_GAIN
        player.set(source, gain)
    }

    private fun now(): Long = SystemClock.elapsedRealtime()

    private companion object {
        const val RAMP_TICK_MILLIS = 200L
    }
}
