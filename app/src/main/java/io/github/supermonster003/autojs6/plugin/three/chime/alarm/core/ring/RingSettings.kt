package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring

import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmSpeak
import org.autojs.plugin.alarm.api.AlarmSpec

/**
 * What one alarm sounds like (roadmap P2.4, appendix A section 4.4), the plugin defaults applied to the stored options:
 * the sound [source] (`default`, `silent` or a ringtone uri), the alarm stream [volume] (null keeps the user's volume,
 * T14), the [rampSeconds] of the fade in, the [vibration] pattern (null without vibration, T61), the [speak] options,
 * the snooze limits and [autoSilenceMinutes].
 * zh-CN: 一个闹钟的响铃内容 (路线图 P2.4, 附录 A 第 4.4 节), 已存选项套用插件默认值后的结果: 铃声 [source] (`default`, `silent` 或铃声 uri),
 * 闹钟流音量 [volume] (null 保持用户的音量, T14), 渐强时长 [rampSeconds], 振动节奏 [vibration] (null 不振动, T61), 朗读 [speak], 贪睡限制与
 * [autoSilenceMinutes].
 */
data class RingSettings(
    val source: String = AlarmContract.SOUND_DEFAULT,
    val volume: Double? = null,
    val rampSeconds: Int = 0,
    val vibration: List<Long>? = DEFAULT_VIBRATION,
    val speak: AlarmSpeak? = null,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = AlarmLimits.DEFAULT_SNOOZE_MINUTES,
    val snoozeMax: Int = AlarmLimits.DEFAULT_SNOOZE_MAX,
    val autoSilenceMinutes: Int = AlarmLimits.DEFAULT_AUTO_SILENCE_MINUTES,
) {

    /** The speech mode, `after` when omitted (appendix A section 4.4). zh-CN: 朗读方式, 省略时为 `after` (附录 A 第 4.4 节). */
    val speakMode: String get() = speak?.mode ?: AlarmContract.SPEAK_MODE_AFTER

    val autoSilenceMillis: Long get() = autoSilenceMinutes * MINUTE_MILLIS

    /**
     * The sound to play: null when the alarm makes no sound of its own (`silent`, or speech `only`). When the speech
     * engine failed ([speechFailed]) an alarm that would make no sound at all plays its source, or the default
     * ringtone for `silent` (T63), so it never goes unheard.
     * zh-CN: 要播放的铃声: 闹钟本身不出声 (`silent` 或朗读 `only`) 时为 null. 朗读引擎失败 ([speechFailed]) 时, 原本完全不出声的闹钟改为播放其铃声,
     * `silent` 时播放默认铃声 (T63), 不会无声无息.
     */
    fun soundSource(speechFailed: Boolean): String? = when {
        speechFailed -> source.takeIf { it != AlarmContract.SOUND_SILENT } ?: AlarmContract.SOUND_DEFAULT
        speak != null && speakMode == AlarmContract.SPEAK_MODE_ONLY -> null
        source == AlarmContract.SOUND_SILENT -> null
        else -> source
    }

    companion object {

        /**
         * The default vibration in `device.vibrate` notation (wait, vibrate, wait, ...): 600 ms on, 400 ms off.
         * zh-CN: 默认振动, `device.vibrate` 写法 (等待, 振动, 等待, ...): 振 600 ms, 停 400 ms.
         */
        val DEFAULT_VIBRATION: List<Long> = listOf(0L, 600L, 400L)

        private const val MINUTE_MILLIS = 60_000L

        /**
         * The settings of [spec], an option it leaves unset taking the plugin [defaults] of the settings screen (T84):
         * a `vibratePattern` vibrates with it (T61), `vibrate` vibrates with the default pattern or not at all, and
         * `vibrate` omitted follows the default; an omitted `snooze` takes the default snooze, an omitted `minutes` or
         * `max` the default value; the auto silence default is at least 30 minutes with a dismiss challenge (1.3.0).
         * zh-CN: [spec] 的响铃设置, 未设置的项取设置页的插件默认值 [defaults] (T84): 给出 `vibratePattern` 时按其振动 (T61), `vibrate` 为 true 按默认节奏
         * 振动, 为 false 不振动, 省略时跟随默认值; 省略 `snooze` 取默认贪睡, 省略其 `minutes` 或 `max` 取默认值; 有关闭挑战 (1.3.0) 时自动停止默认至少
         * 30 分钟.
         */
        fun of(spec: AlarmSpec, defaults: RingDefaults = RingDefaults.PROTOCOL): RingSettings {
            val sound = spec.sound
            val snooze = spec.snooze
            val challenge = spec.dismiss?.challenge?.takeIf { it != AlarmContract.CHALLENGE_NONE }
            return RingSettings(
                source = sound?.source ?: defaults.sound,
                volume = sound?.volume,
                rampSeconds = sound?.rampSeconds ?: 0,
                vibration = when {
                    spec.vibratePattern != null -> spec.vibratePattern
                    spec.vibrate ?: defaults.vibrate -> DEFAULT_VIBRATION
                    else -> null
                },
                speak = spec.speak?.takeIf { it.text.isNotBlank() },
                snoozeEnabled = snooze?.enabled ?: defaults.snoozeEnabled,
                snoozeMinutes = snooze?.minutes ?: defaults.snoozeMinutes,
                snoozeMax = snooze?.max ?: defaults.snoozeMax,
                autoSilenceMinutes = spec.autoSilenceMinutes ?: defaults.autoSilenceFor(challenge != null),
            )
        }
    }
}
