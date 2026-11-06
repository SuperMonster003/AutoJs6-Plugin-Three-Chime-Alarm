package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import android.annotation.SuppressLint
import android.content.Context
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingDefaults

/**
 * The plugin defaults of the settings screen (roadmap P3.5, T84) in device-protected storage, so an alarm that rings
 * before the first unlock already uses them. A missing or unreadable store reads as the protocol defaults.
 * zh-CN: 设置页的插件默认值 (路线图 P3.5, T84), 保存在设备加密存储中, 首次解锁前响铃的闹钟也能用上. 存储缺失或无法读取时视为协议默认值.
 */
class RingDefaultsStore(context: Context) {

    private val preferences = context.createDeviceProtectedStorageContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun get(): RingDefaults = runCatching {
        val protocol = RingDefaults.PROTOCOL
        RingDefaults(
            sound = preferences.getString(KEY_SOUND, null) ?: protocol.sound,
            vibrate = preferences.getBoolean(KEY_VIBRATE, protocol.vibrate),
            snoozeEnabled = preferences.getBoolean(KEY_SNOOZE_ENABLED, protocol.snoozeEnabled),
            snoozeMinutes = preferences.getInt(KEY_SNOOZE_MINUTES, protocol.snoozeMinutes),
            snoozeMax = preferences.getInt(KEY_SNOOZE_MAX, protocol.snoozeMax),
            autoSilenceMinutes = preferences.getInt(KEY_AUTO_SILENCE_MINUTES, protocol.autoSilenceMinutes),
        ).sanitized()
    }.getOrDefault(RingDefaults.PROTOCOL)

    /**
     * Saves [defaults]; false when the write failed. Written at once, an alarm may ring right after the change.
     * zh-CN: 保存 [defaults]; 写入失败时返回 false. 立即落盘, 修改后可能马上就有闹钟响铃.
     */
    // The KTX `edit` drops the result of `commit`. zh-CN: KTX 的 `edit` 会丢掉 `commit` 的结果.
    @SuppressLint("ApplySharedPref", "UseKtx")
    fun set(defaults: RingDefaults): Boolean = runCatching {
        val value = defaults.sanitized()
        preferences.edit()
            .putString(KEY_SOUND, value.sound)
            .putBoolean(KEY_VIBRATE, value.vibrate)
            .putBoolean(KEY_SNOOZE_ENABLED, value.snoozeEnabled)
            .putInt(KEY_SNOOZE_MINUTES, value.snoozeMinutes)
            .putInt(KEY_SNOOZE_MAX, value.snoozeMax)
            .putInt(KEY_AUTO_SILENCE_MINUTES, value.autoSilenceMinutes)
            .commit()
    }.getOrDefault(false)

    private companion object {
        const val PREFERENCES = "ring-defaults"
        const val KEY_SOUND = "sound"
        const val KEY_VIBRATE = "vibrate"
        const val KEY_SNOOZE_ENABLED = "snoozeEnabled"
        const val KEY_SNOOZE_MINUTES = "snoozeMinutes"
        const val KEY_SNOOZE_MAX = "snoozeMax"
        const val KEY_AUTO_SILENCE_MINUTES = "autoSilenceMinutes"
    }
}
