package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring

import kotlin.math.roundToInt

/**
 * The independent volume of roadmap P2.4 (T14, T68): a ring with `sound.volume` sets the alarm stream for as long as it
 * rings, and the user's own volume comes back afterwards. The volume to come back is saved in [memory] before the
 * first change and only cleared by [restore], so a process that died while ringing still restores it on the next start,
 * and a ring that follows another one with its own volume does not take the borrowed volume for the user's.
 * zh-CN: 路线图 P2.4 的独立音量 (T14, T68): 带 `sound.volume` 的响铃在响铃期间设定闹钟流音量, 结束后恢复用户自己的音量. 要恢复的音量在第一次改动前存入
 * [memory], 只由 [restore] 清除, 所以响铃中死亡的进程在下次启动时仍会恢复, 紧接着另一个带音量的响铃也不会把借用的音量当成用户的音量.
 */
class RingVolume(private val stream: Stream, private val memory: Memory) {

    /** The alarm stream: its index range and current index. zh-CN: 闹钟流: 音量级范围与当前音量级. */
    interface Stream {
        val min: Int
        val max: Int
        var index: Int
    }

    /** Where the user's volume waits to come back (persisted on Android). zh-CN: 用户音量等待恢复时的存放处 (Android 上持久化). */
    interface Memory {
        var saved: Int?
    }

    /**
     * Sets the stream for a ring with [volume] (0 - 1); a ring without a volume plays at the user's volume, so a borrowed
     * one is given back first.
     * zh-CN: 为音量 [volume] (0 - 1) 的响铃设定闹钟流; 不带音量的响铃使用用户的音量, 所以先归还借用的音量.
     */
    @Synchronized
    fun apply(volume: Double?) {
        if (volume == null) {
            restore()
            return
        }
        if (memory.saved == null) memory.saved = stream.index
        stream.index = indexOf(volume, stream.min, stream.max)
    }

    /** Gives the user's volume back, if any was borrowed. zh-CN: 若借用过音量则归还用户的音量. */
    @Synchronized
    fun restore() {
        val saved = memory.saved ?: return
        stream.index = saved.coerceIn(stream.min, stream.max)
        memory.saved = null
    }

    companion object {

        /** The stream index of [volume] in [min] - [max]. zh-CN: [volume] 在 [min] - [max] 中对应的音量级. */
        fun indexOf(volume: Double, min: Int, max: Int): Int = min + (volume.coerceIn(0.0, 1.0) * (max - min)).roundToInt()

        /**
         * The player gain [elapsedMillis] into a fade in of [rampSeconds]: grows with the square of the time from 0.01
         * to 1 (T68); 1 without a fade in.
         * zh-CN: 渐强 [rampSeconds] 秒进行到 [elapsedMillis] 时的播放增益: 按时间的平方从 0.01 增至 1 (T68); 没有渐强时为 1.
         */
        fun rampGain(elapsedMillis: Long, rampSeconds: Int): Float {
            if (rampSeconds <= 0) return 1f
            val t = (elapsedMillis.toDouble() / (rampSeconds * 1000.0)).coerceIn(0.0, 1.0)
            return (t * t).coerceAtLeast(MIN_RAMP_GAIN).toFloat()
        }

        /**
         * The player gain of [volume]: 0 mutes the player, since the lowest index of the alarm stream is often above
         * silence (T68).
         * zh-CN: 音量 [volume] 对应的播放增益: 0 时让播放器静音, 因为闹钟流的最低音量级往往不是无声 (T68).
         */
        fun playerGain(volume: Double?): Float = if (volume != null && volume <= 0.0) 0f else 1f

        private const val MIN_RAMP_GAIN = 0.01
    }
}
