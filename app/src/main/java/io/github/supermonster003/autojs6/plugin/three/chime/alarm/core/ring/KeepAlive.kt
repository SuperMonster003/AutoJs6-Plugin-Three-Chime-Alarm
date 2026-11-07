package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord

/**
 * The "Keep running in background" setting of the settings screen (roadmap Q23 (b), T100). Off by default; it lives in
 * device-protected storage, so the process that starts before the first unlock already knows it.
 * zh-CN: 设置页的 "保持后台运行" (路线图 Q23 (b), T100). 默认关闭; 保存在设备加密存储中, 首次解锁前启动的进程也能读到.
 */
class KeepAliveSettings(private val storage: Storage) {

    /** The key-value storage behind the setting. zh-CN: 设置背后的键值存储. */
    interface Storage {
        fun getBoolean(key: String, default: Boolean): Boolean

        /** Writes at once; false when the write failed. zh-CN: 立即写入; 写入失败时为 false. */
        fun putBoolean(key: String, value: Boolean): Boolean
    }

    val enabled: Boolean
        get() = storage.getBoolean(KEY_ENABLED, DEFAULT_ENABLED)

    /** Saves the setting; false when the write failed. zh-CN: 保存设置; 写入失败时为 false. */
    fun setEnabled(value: Boolean): Boolean = storage.putBoolean(KEY_ENABLED, value)

    companion object {
        const val KEY_ENABLED = "enabled"
        const val DEFAULT_ENABLED = false
    }
}

/**
 * When the keep-alive service runs (T100): while the setting is on and at least one alarm is enabled. Without an enabled
 * alarm nothing is due, so the service and its notification go away; the host's `alarms` calls never depend on it.
 * zh-CN: 保活服务何时运行 (T100): 设置打开且至少有一个启用的闹钟时. 没有启用的闹钟时不会有闹钟到点, 服务与其通知随之消失; 宿主的 `alarms` 调用从不依赖它.
 */
object KeepAlivePolicy {

    /** What to do with the service. zh-CN: 对服务的处理. */
    enum class Action { START, STOP, NONE }

    fun shouldRun(enabled: Boolean, alarms: Collection<AlarmRecord>): Boolean = enabled && alarms.any { it.enabled }

    /**
     * The action for a service that is [running] in the foreground now. A running service is never started again, and a
     * service that has not reached the foreground yet is never stopped (stopping it then crashes the app): its own start
     * checks again once it is in the foreground.
     * zh-CN: 对当前是否已在前台 [running] 的服务应采取的处理. 运行中的服务不再重复启动, 尚未进入前台的服务不停止 (此时停止会让应用崩溃): 它进入前台后
     * 自己会再检查一次.
     */
    fun action(shouldRun: Boolean, running: Boolean): Action = when {
        shouldRun && !running -> Action.START
        !shouldRun && running -> Action.STOP
        else -> Action.NONE
    }
}
