package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule

/**
 * Tells whether a start of the plugin process is the first one since the device booted (roadmap P5.3, T96). The boot
 * broadcasts start the process, and its own start reconciliation runs before the broadcast is handled, so the start has
 * to reconcile as [AlarmPlanner.Cause.BOOT] itself: otherwise a ring missed while the device was off is reported with
 * reason `late` instead of `off` (T59). [current] reads the system boot count (`Settings.Global.BOOT_COUNT`, null when
 * unreadable), [stored] / [store] keep the count of the previous start in device-protected storage.
 *
 * zh-CN: 判断插件进程的本次启动是否为设备开机后的第一次 (路线图 P5.3, T96). 开机广播会启动进程, 而进程自身的启动对账先于广播处理运行,
 * 因此启动对账本身要按 [AlarmPlanner.Cause.BOOT] 进行: 否则关机期间错过的响铃会以 `late` 而不是 `off` 报告 (T59). [current] 读取系统开机次数
 * (`Settings.Global.BOOT_COUNT`, 读不到时为 null), [stored] / [store] 在设备加密存储中保存上次启动时的次数.
 */
class BootMarker(
    private val current: () -> Int?,
    private val stored: () -> Int?,
    private val store: (Int) -> Unit,
) {

    /**
     * The cause of the start reconciliation: [AlarmPlanner.Cause.BOOT] when the boot count differs from the stored one,
     * else [AlarmPlanner.Cause.RESTORE]. The first start after an install (nothing stored) and an unreadable count are
     * [AlarmPlanner.Cause.RESTORE]: the boot broadcast that follows a reboot still reconciles as a boot.
     * zh-CN: 启动对账的原因: 开机次数与保存值不同时为 BOOT, 否则为 RESTORE. 安装后的首次启动 (无保存值) 与读不到次数都为 RESTORE.
     */
    fun startCause(): AlarmPlanner.Cause {
        val count = current() ?: return AlarmPlanner.Cause.RESTORE
        val previous = stored()
        if (previous != count) store(count)
        return if (previous != null && previous != count) AlarmPlanner.Cause.BOOT else AlarmPlanner.Cause.RESTORE
    }
}
