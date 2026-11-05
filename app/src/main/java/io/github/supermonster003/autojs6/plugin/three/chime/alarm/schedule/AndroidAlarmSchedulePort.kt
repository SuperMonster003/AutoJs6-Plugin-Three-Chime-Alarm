package io.github.supermonster003.autojs6.plugin.three.chime.alarm.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ThreeChimeAlarmApplication
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedulePort
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.HomeActivity
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmEvent

/**
 * The Android side of the scheduler (roadmap P2.3, T3): one `setAlarmClock` per enabled alarm at its next wake-up, its
 * show intent opening the alarm list, so Doze does not defer it and the status bar, the lock screen and
 * `getNextAlarmClock()` show it. The skipped instance of `skipNext` gets a plain exact wake-up (not shown as the next
 * alarm) that sends the `skip` event. Each alarm has its own pending intents (the data uri carries the id), and the
 * trigger receiver decides from the stored state, so a stale wake-up rings nothing.
 *
 * zh-CN: 调度器的 Android 部分 (路线图 P2.3, T3): 每个启用的闹钟在下一次唤醒时刻设一个 `setAlarmClock`, 展示 Intent 打开闹钟列表, 因此不受 Doze 推迟,
 * 状态栏, 锁屏与 `getNextAlarmClock()` 都能看到. `skipNext` 跳过的那次另设普通精确唤醒 (不显示为下一个闹钟), 用于发送 `skip` 事件. 每个闹钟有自己的
 * PendingIntent (data uri 带 id), 触发接收器按已存状态决定, 过期的唤醒不会响铃.
 */
class AndroidAlarmSchedulePort(private val context: Context) : AlarmSchedulePort {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    override fun schedule(record: AlarmRecord) {
        val exact = canScheduleExact()
        val wake = record.state.wakeAt?.takeIf { record.enabled && exact }
        if (wake == null) {
            cancel(record.id, ACTION_RING)
        } else {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(wake, showIntent()), pending(record.id, ACTION_RING, wake))
        }
        val skipAt = record.state.skipAt?.takeIf { record.enabled && exact && (wake == null || it < wake) }
        if (skipAt == null) {
            cancel(record.id, ACTION_SKIP)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, skipAt, pending(record.id, ACTION_SKIP, skipAt))
        }
    }

    override fun cancel(alarmId: String) {
        cancel(alarmId, ACTION_RING)
        cancel(alarmId, ACTION_SKIP)
    }

    override fun ring(record: AlarmRecord, request: AlarmPlanner.RingRequest) {
        // Roadmap P2.4 starts the ring service here. zh-CN: 路线图 P2.4 在这里启动响铃服务.
        Log.i(TAG, "ring ${request.alarmId} scheduledAt=${request.scheduledAt} late=${request.firedAt - request.scheduledAt}")
    }

    override fun notifyMissed(record: AlarmRecord, event: AlarmEvent) {
        val scheduledAt = event.scheduledAt ?: return
        val label = record.spec.label?.takeIf { it.isNotBlank() } ?: context.getString(R.string.alarm_default_label)
        val flags = DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or
            (if (DateFormat.is24HourFormat(context)) DateUtils.FORMAT_24HOUR else DateUtils.FORMAT_12HOUR)
        val time = DateUtils.formatDateTime(context, scheduledAt, flags)
        val text = context.getString(
            if (event.reason == AlarmContract.MISS_REASON_OFF) R.string.notification_missed_text_off else R.string.notification_missed_text_late,
            time,
        )
        val notification = NotificationCompat.Builder(context, ThreeChimeAlarmApplication.CHANNEL_MISSED)
            .setSmallIcon(R.mipmap.ic_icon_studio_application_monochrome)
            .setContentTitle(context.getString(R.string.notification_missed_title, label))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setWhen(scheduledAt)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(showIntent())
            .build()
        try {
            NotificationManagerCompat.from(context).notify(TAG_MISSED, record.id.hashCode(), notification)
        } catch (e: SecurityException) {
            // Notifications are not allowed (API 33+); the `miss` event still went out. zh-CN: 未授予通知权限, `miss` 事件照常发出.
            Log.w(TAG, "missed alarm notification refused: ${e.message}")
        }
    }

    private fun cancel(alarmId: String, action: String) {
        val intent = PendingIntent.getBroadcast(context, 0, intent(alarmId, action), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            ?: return
        alarmManager.cancel(intent)
        intent.cancel()
    }

    private fun pending(alarmId: String, action: String, at: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent(alarmId, action).putExtra(EXTRA_AT, at),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun intent(alarmId: String, action: String): Intent = Intent(context, AlarmTriggerReceiver::class.java)
        .setAction(action)
        .setData(Uri.Builder().scheme(URI_SCHEME).authority(URI_AUTHORITY).appendPath(alarmId).build())

    /** Opens the alarm list (the show intent of the alarm clock). zh-CN: 打开闹钟列表 (闹钟时钟的展示 Intent). */
    private fun showIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {

        const val TAG = "ChimeSchedule"

        const val ACTION_RING = "io.github.supermonster003.autojs6.plugin.three.chime.alarm.action.RING"
        const val ACTION_SKIP = "io.github.supermonster003.autojs6.plugin.three.chime.alarm.action.SKIP"

        /** The wake-up instant the intent was set for (diagnostics). zh-CN: 唤醒设定的时刻 (诊断用). */
        const val EXTRA_AT = "at"

        private const val URI_SCHEME = "chime-alarm"
        private const val URI_AUTHORITY = "alarm"
        private const val TAG_MISSED = "missed"

        /** The alarm id of a trigger intent. zh-CN: 触发 Intent 中的闹钟 id. */
        fun alarmIdOf(intent: Intent): String? = intent.data?.takeIf { it.scheme == URI_SCHEME }?.lastPathSegment
    }
}
