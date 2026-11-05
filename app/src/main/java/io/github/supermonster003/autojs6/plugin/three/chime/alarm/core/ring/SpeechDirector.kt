package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring

import org.autojs.plugin.alarm.api.AlarmContract

/**
 * When the ring speaks and how loud the sound is meanwhile (roadmap P2.4, appendix A section 4.4, T62), free of
 * Android so the JVM tests cover it. The ring service calls [start] once, [due] when [Step.wakeAt] has come, [spoken]
 * when an utterance ended (or its watchdog gave up) and [unavailable] when the speech engine failed; each answers the
 * step to perform.
 *
 * - `before`: speaks at once without sound, then the sound plays; later utterances lower it.
 * - `after` (default): the sound plays for [AFTER_DELAY_MILLIS], then is lowered while speaking.
 * - `interleave`: the sound plays for a segment ([repeatIntervalSeconds], [INTERLEAVE_SEGMENT_MILLIS] when 0), then
 *   stops while speaking, and so on.
 * - `only`: speaks without any sound.
 *
 * Except for `interleave`, the text is spoken once when [repeatIntervalSeconds] is 0, otherwise again that long after
 * the previous utterance ended.
 *
 * zh-CN: 响铃何时朗读以及朗读期间铃声多大 (路线图 P2.4, 附录 A 第 4.4 节, T62), 不依赖 Android, 由 JVM 测试覆盖. 响铃服务调用一次 [start],
 * 到 [Step.wakeAt] 时调用 [due], 一次朗读结束 (或看门狗放弃) 时调用 [spoken], 朗读引擎失败时调用 [unavailable]; 每个调用返回要执行的一步.
 * `before`: 先在无铃声时朗读, 然后播放铃声, 之后的朗读压低铃声. `after` (默认): 铃声先响 [AFTER_DELAY_MILLIS], 朗读时压低. `interleave`: 铃声响一段
 * ([repeatIntervalSeconds], 为 0 时 [INTERLEAVE_SEGMENT_MILLIS]) 后停下朗读, 如此交替. `only`: 只朗读, 没有铃声. 除 `interleave` 外,
 * [repeatIntervalSeconds] 为 0 时只读一次, 否则在上一次朗读结束后隔这么久再读.
 */
class SpeechDirector(private val mode: String, private val repeatIntervalSeconds: Int) {

    /** How loud the sound is: full, lowered under speech ([DUCK_GAIN]) or silent. zh-CN: 铃声大小: 正常, 朗读时压低 ([DUCK_GAIN]) 或无声. */
    enum class Sound { FULL, DUCKED, OFF }

    /**
     * A step: the [sound] level, whether to [speak] an utterance now, and when to call [due] (null for no call).
     * zh-CN: 一步: 铃声大小 [sound], 是否现在开始朗读 [speak], 以及何时调用 [due] (null 为不调用).
     */
    data class Step(val sound: Sound, val speak: Boolean = false, val wakeAt: Long? = null)

    private var step: Step = Step(Sound.FULL)

    private var speaking = false

    private var done = false

    /** The step in force. zh-CN: 当前生效的一步. */
    val current: Step get() = step

    fun start(now: Long): Step = when (mode) {
        AlarmContract.SPEAK_MODE_BEFORE -> speak(Sound.OFF)
        AlarmContract.SPEAK_MODE_ONLY -> speak(Sound.OFF)
        AlarmContract.SPEAK_MODE_INTERLEAVE -> wait(Sound.FULL, now + segmentMillis)
        else -> wait(Sound.FULL, now + AFTER_DELAY_MILLIS)
    }

    /** [Step.wakeAt] has come: the next utterance starts. zh-CN: 到 [Step.wakeAt]: 开始下一次朗读. */
    fun due(now: Long): Step {
        if (done || speaking) return step
        val wake = step.wakeAt ?: return step
        if (now < wake) return step
        return speak(
            when (mode) {
                AlarmContract.SPEAK_MODE_ONLY, AlarmContract.SPEAK_MODE_INTERLEAVE -> Sound.OFF
                else -> Sound.DUCKED
            },
        )
    }

    /** The utterance ended. zh-CN: 一次朗读结束. */
    fun spoken(now: Long): Step {
        if (done || !speaking) return step
        speaking = false
        val sound = if (mode == AlarmContract.SPEAK_MODE_ONLY) Sound.OFF else Sound.FULL
        return when {
            mode == AlarmContract.SPEAK_MODE_INTERLEAVE -> wait(sound, now + segmentMillis)
            repeatIntervalSeconds > 0 -> wait(sound, now + repeatIntervalSeconds * SECOND_MILLIS)
            else -> finish(sound)
        }
    }

    /**
     * The speech engine failed: no more speech and the full sound from now on (an alarm without any sound of its own
     * then plays one, see [RingSettings.soundSource]).
     * zh-CN: 朗读引擎失败: 此后不再朗读, 铃声恢复正常 (本身没有铃声的闹钟此时改为播放铃声, 见 [RingSettings.soundSource]).
     */
    fun unavailable(): Step {
        speaking = false
        return finish(Sound.FULL)
    }

    private val segmentMillis: Long
        get() = if (repeatIntervalSeconds > 0) repeatIntervalSeconds * SECOND_MILLIS else INTERLEAVE_SEGMENT_MILLIS

    private fun speak(sound: Sound): Step {
        speaking = true
        step = Step(sound, speak = true)
        return step
    }

    private fun wait(sound: Sound, wakeAt: Long): Step {
        step = Step(sound, wakeAt = wakeAt)
        return step
    }

    private fun finish(sound: Sound): Step {
        done = true
        step = Step(sound)
        return step
    }

    companion object {

        /** How long the sound plays before `after` speaks (T62). zh-CN: `after` 朗读前铃声先响的时长 (T62). */
        const val AFTER_DELAY_MILLIS = 5_000L

        /** The sound segment of `interleave` without `repeatInterval` (T62). zh-CN: 没有 `repeatInterval` 时 `interleave` 每段铃声的时长 (T62). */
        const val INTERLEAVE_SEGMENT_MILLIS = 10_000L

        /** The gain of the sound lowered under speech (T62). zh-CN: 朗读时压低后的铃声增益 (T62). */
        const val DUCK_GAIN = 0.25f

        private const val SECOND_MILLIS = 1_000L
    }
}
