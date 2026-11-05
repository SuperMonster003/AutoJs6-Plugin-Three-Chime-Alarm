package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Vibrates a ring (roadmap P2.4, T61): the pattern in `device.vibrate` notation (wait, vibrate, wait, ...) repeats from
 * its start until [stop], with alarm attributes so silent mode and Do Not Disturb treat it as an alarm.
 * zh-CN: 响铃振动 (路线图 P2.4, T61): `device.vibrate` 写法的节奏 (等待, 振动, 等待, ...) 从头循环直到 [stop], 使用闹钟属性, 因此静音模式与勿扰模式
 * 按闹钟对待.
 */
internal class RingVibrator(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }

    private var on = false

    fun start(pattern: List<Long>) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: run {
            Log.i(RingService.TAG, "vibration skipped: no vibrator")
            return
        }
        val timings = pattern.toLongArray()
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> vibrator.vibrate(
                    VibrationEffect.createWaveform(timings, 0),
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
                )
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> @Suppress("DEPRECATION") vibrator.vibrate(VibrationEffect.createWaveform(timings, 0), ATTRIBUTES)
                else -> @Suppress("DEPRECATION") vibrator.vibrate(timings, 0, ATTRIBUTES)
            }
            on = true
            Log.i(RingService.TAG, "vibration started (${timings.size} timings)")
        } catch (e: RuntimeException) {
            Log.w(RingService.TAG, "vibration refused: ${e.message}")
        }
    }

    fun stop() {
        if (on) vibrator?.cancel()
        on = false
    }

    private companion object {
        val ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}
