package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime

/**
 * Starts the ring service again from an alarm wake-up (roadmap P5.3, T97). A ring offered while the process runs in the
 * background without an exemption (started by a host binding or a tool call after a force stop, for example) cannot
 * start the foreground service; [arm] then sets an exact wake-up [DELAY_MILLIS] ahead, and the alarm broadcast, which
 * may start a foreground service, starts it if a ring still sounds. A start refused here is not armed again.
 *
 * zh-CN: 由闹钟唤醒再次启动响铃服务 (路线图 P5.3, T97). 进程在后台且没有豁免时 (例如强行停止后由宿主绑定或工具调用拉起) 收到的响铃无法启动前台服务;
 * 此时 [arm] 在 [DELAY_MILLIS] 后设一个精确唤醒, 闹钟广播可以启动前台服务, 届时仍有发声的一轮就启动它. 在这里启动仍被拒绝时不再重设.
 */
class RingKickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val runtime = AlarmRuntime.get(context)
        val pending = goAsync()
        runtime.executor.execute {
            try {
                val sounding = runtime.rings.sounding
                Log.i(RingService.TAG, "ring kick: sounding=${sounding?.alarmId ?: "none"}")
                if (sounding != null) RingService.start(context, rearm = false)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {

        /** The delay of the wake-up. zh-CN: 唤醒的延迟. */
        private const val DELAY_MILLIS = 1_000L

        /** Sets the wake-up; exact when the plugin may schedule exact alarms (T60). zh-CN: 设置唤醒; 可排精确闹钟时为精确唤醒. */
        fun arm(context: Context) {
            val alarms = context.getSystemService(AlarmManager::class.java)
            val intent = PendingIntent.getBroadcast(
                context, 0, Intent(context, RingKickReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val at = System.currentTimeMillis() + DELAY_MILLIS
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
            if (exact) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            }
            Log.i(RingService.TAG, "ring kick armed (exact=$exact)")
        }
    }
}
