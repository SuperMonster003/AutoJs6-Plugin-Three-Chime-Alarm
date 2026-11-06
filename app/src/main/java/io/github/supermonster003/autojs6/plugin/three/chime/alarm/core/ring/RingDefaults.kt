package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring

import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmLimits

/**
 * The plugin defaults of the settings screen (roadmap P3.5, T84): what an alarm rings with when it does not set the
 * option itself (the "Default" choice of the edit page, or a script that omits the option). Out-of-range values read
 * from storage are clamped by [sanitized]. The protocol defaults of appendix A section 4.4 are the initial values.
 * zh-CN: 设置页的插件默认值 (路线图 P3.5, T84): 闹钟自身未设置某项时 (编辑页选 "默认", 或脚本省略该项) 响铃所用的值. 从存储读到的越界值由 [sanitized]
 * 收回范围内. 初始值即附录 A 第 4.4 节的协议默认值.
 */
data class RingDefaults(
    /** `default` (the system alarm sound), `silent` or a ringtone uri. zh-CN: `default` (系统闹钟铃声), `silent` 或铃声 uri. */
    val sound: String = AlarmContract.SOUND_DEFAULT,
    val vibrate: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = AlarmLimits.DEFAULT_SNOOZE_MINUTES,
    val snoozeMax: Int = AlarmLimits.DEFAULT_SNOOZE_MAX,
    val autoSilenceMinutes: Int = AlarmLimits.DEFAULT_AUTO_SILENCE_MINUTES,
) {

    /** These defaults with every number inside the protocol limits. zh-CN: 各数值都在协议范围内的默认值. */
    fun sanitized(): RingDefaults = copy(
        sound = sound.ifBlank { AlarmContract.SOUND_DEFAULT },
        snoozeMinutes = snoozeMinutes.coerceIn(AlarmLimits.MIN_SNOOZE_MINUTES, AlarmLimits.MAX_SNOOZE_MINUTES),
        snoozeMax = snoozeMax.coerceIn(1, AlarmLimits.MAX_SNOOZE_MAX),
        autoSilenceMinutes = autoSilenceMinutes.coerceIn(AlarmLimits.MIN_AUTO_SILENCE_MINUTES, AlarmLimits.MAX_AUTO_SILENCE_MINUTES),
    )

    /**
     * The auto silence of an alarm that leaves it unset: a dismiss challenge (1.3.0) needs at least 30 minutes, so it
     * takes the longer of the two.
     * zh-CN: 未设置自动停止的闹钟所用的时长: 关闭挑战 (1.3.0) 至少需要 30 分钟, 因此取两者中较长的一个.
     */
    fun autoSilenceFor(challenge: Boolean): Int =
        if (challenge) maxOf(autoSilenceMinutes, AlarmLimits.DEFAULT_CHALLENGE_AUTO_SILENCE_MINUTES) else autoSilenceMinutes

    companion object {
        /** The protocol defaults. zh-CN: 协议默认值. */
        val PROTOCOL = RingDefaults()
    }
}
