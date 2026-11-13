package io.github.supermonster003.autojs6.plugin.three.chime.alarm.schedule

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.timer.AndroidTimerPort
import java.util.TimeZone

/**
 * Receives the wake-ups of [AndroidAlarmSchedulePort] (roadmap P2.3, T7: direct boot aware, not exported) and
 * reconciles that alarm: rings it, sends `skip` or `miss`, and plans the next ring. The `beforeRing` wake-up sends that
 * event instead (roadmap P8.4).
 * zh-CN: 接收 [AndroidAlarmSchedulePort] 的唤醒 (路线图 P2.3, T7: 支持直接启动, 不导出), 对该闹钟对账: 响铃, 发送 `skip` 或 `miss`, 并计划下一次响铃.
 * `beforeRing` 唤醒改为发送该事件 (路线图 P8.4).
 */
class AlarmTriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = AndroidAlarmSchedulePort.alarmIdOf(intent) ?: return
        if (intent.action == AndroidAlarmSchedulePort.ACTION_BEFORE_RING) {
            AlarmRuntime.get(context).run(goAsync(), "beforeRing $alarmId") {
                val event = schedule.beforeRing(alarmId)
                Log.i(AndroidAlarmSchedulePort.TAG, "beforeRing $alarmId: ${event?.let { "sent, deadline=${it.deadline}" } ?: "nothing to send"}")
                emptyList()
            }
            return
        }
        AlarmRuntime.get(context).run(goAsync(), "trigger $alarmId") {
            schedule.reconcile(AlarmPlanner.Cause.TRIGGER, listOf(alarmId)).values
        }
    }
}

/**
 * Reschedules every alarm at the T8 moments (roadmap P2.3, T7: direct boot aware): boot (rings missed while the device
 * was off give `miss` with reason `off`), a new clock time or time zone, a plugin update and a granted exact alarm
 * permission (API 31 - 32). These broadcasts are protected, only the system sends them.
 * zh-CN: 在 T8 各时机重排全部闹钟 (路线图 P2.3, T7: 支持直接启动): 开机 (关机期间错过的响铃发 `miss`, 原因 `off`), 时钟时间或时区改变, 插件升级,
 * 授予精确闹钟权限 (API 31 - 32). 这些广播受保护, 只有系统能发.
 */
class AlarmRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val cause = when (action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_LOCKED_BOOT_COMPLETED -> AlarmPlanner.Cause.BOOT
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> AlarmPlanner.Cause.CLOCK
            Intent.ACTION_MY_PACKAGE_REPLACED, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> AlarmPlanner.Cause.RESTORE
            else -> return
        }
        // The default zone of a running process may still be the old one. zh-CN: 运行中的进程的默认时区可能还是旧的.
        if (action == Intent.ACTION_TIMEZONE_CHANGED) TimeZone.setDefault(null)
        AlarmRuntime.get(context).run(goAsync(), "reschedule $action") {
            schedule.reconcile(cause).values.also {
                // The timers too (roadmap P7.1, T110). zh-CN: 计时器也对账 (路线图 P7.1, T110).
                Log.i(AndroidTimerPort.TAG, "reschedule $action: ${timers.reconcile(cause)} timers rang")
            }
        }
    }
}

/** Runs [block] off the main thread, logs its decisions and finishes the broadcast. zh-CN: 在主线程外运行 [block], 记录其决定并结束广播. */
private fun AlarmRuntime.run(pending: BroadcastReceiver.PendingResult, what: String, block: AlarmRuntime.() -> Collection<AlarmPlanner.Outcome>) {
    executor.execute {
        try {
            log(what, block())
        } catch (e: RuntimeException) {
            Log.e(AndroidAlarmSchedulePort.TAG, "$what failed", e)
        } finally {
            pending.finish()
        }
    }
}
