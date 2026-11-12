package io.github.supermonster003.autojs6.plugin.three.chime.alarm.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ThreeChimeAlarmApplication
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerPort
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.appearance.screenTexts
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.format.TimeFormats
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.HomeActivity

/**
 * The Android side of the timers (roadmap P7.1, T3): one `setAlarmClock` per running timer at its end, or per snoozed
 * timer at the end of the snooze, so Doze does not defer it; the trigger receiver decides from the stored state, so a
 * stale wake-up rings nothing. A missed timer (the device was off past the 5 minute window, T16) gets a notification on
 * the missed channel and no event (T115).
 *
 * zh-CN: 计时器的 Android 部分 (路线图 P7.1, T3): 每个运行中的计时器在到点时刻设一个 `setAlarmClock`, 贪睡中的计时器在贪睡结束时刻设一个, 不受 Doze
 * 推迟; 触发接收器按已存状态决定, 过期的唤醒不会响铃. 错过的计时器 (关机超过 5 分钟窗口, T16) 在错过渠道上发一条通知, 不发事件 (T115).
 */
class AndroidTimerPort(
    private val context: Context,
    private val onRing: (TimerRecord, AlarmPlanner.RingRequest) -> Unit = { _, _ -> },
    private val onRemovedSilently: (String) -> Unit = {},
) : TimerPort {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    override fun schedule(record: TimerRecord) {
        val wake = when {
            record.running -> record.endsAt
            record.ringing -> record.snoozeUntil
            else -> null
        }?.takeIf { canScheduleExact() }
        if (wake == null) {
            cancel(record.id)
        } else {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(wake, showIntent()), pending(record.id, wake))
        }
    }

    override fun cancel(timerId: String) {
        val intent = PendingIntent.getBroadcast(context, 0, intent(timerId), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            ?: return
        alarmManager.cancel(intent)
        intent.cancel()
    }

    override fun ring(record: TimerRecord, request: AlarmPlanner.RingRequest) {
        Log.i(TAG, "ring ${request.alarmId} scheduledAt=${request.scheduledAt} late=${request.firedAt - request.scheduledAt}")
        onRing(record, request)
    }

    override fun notifyMissed(record: TimerRecord, scheduledAt: Long) {
        Log.i(TAG, "missed ${record.id} scheduledAt=$scheduledAt")
        // The texts follow the language of the plugin's screens (roadmap Q21). zh-CN: 文字跟随插件界面语言 (路线图 Q21).
        val texts = screenTexts(context)
        val label = record.spec.label?.takeIf { it.isNotBlank() } ?: texts.getString(R.string.timer_default_label)
        val text = texts.getString(R.string.notification_timer_missed_text, TimeFormats.dateTimeOf(texts, scheduledAt))
        val notification = NotificationCompat.Builder(context, ThreeChimeAlarmApplication.CHANNEL_MISSED)
            .setSmallIcon(R.drawable.ic_alarm_24)
            .setContentTitle(texts.getString(R.string.notification_timer_missed_title, label))
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
            Log.w(TAG, "missed timer notification refused: ${e.message}")
        }
    }

    override fun removedSilently(timerId: String) = onRemovedSilently(timerId)

    private fun pending(timerId: String, at: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent(timerId).putExtra(EXTRA_AT, at),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun intent(timerId: String): Intent = Intent(context, TimerTriggerReceiver::class.java)
        .setAction(ACTION_RING)
        .setData(uriOf(timerId))

    /**
     * Opens the alarm list until the timer screen of roadmap P7.4 exists.
     * zh-CN: 在路线图 P7.4 的计时器页面出现之前打开闹钟列表.
     */
    private fun showIntent(): PendingIntent = TimerNotifications.homeIntent(context)

    companion object {

        const val TAG = "ChimeTimer"

        const val ACTION_RING = "io.github.supermonster003.autojs6.plugin.three.chime.alarm.action.TIMER_RING"

        /** The wake-up instant the intent was set for (diagnostics). zh-CN: 唤醒设定的时刻 (诊断用). */
        const val EXTRA_AT = "at"

        private const val URI_SCHEME = "chime-alarm"
        private const val URI_AUTHORITY = "timer"
        private const val TAG_MISSED = "missed-timer"

        /** The data uri that tells the pending intents of one timer apart. zh-CN: 区分各计时器 PendingIntent 的 data uri. */
        fun uriOf(timerId: String): Uri = Uri.Builder().scheme(URI_SCHEME).authority(URI_AUTHORITY).appendPath(timerId).build()

        /** The timer id of a trigger or action intent. zh-CN: 触发或动作 Intent 中的计时器 id. */
        fun timerIdOf(intent: Intent): String? =
            intent.data?.takeIf { it.scheme == URI_SCHEME && it.authority == URI_AUTHORITY }?.lastPathSegment?.takeIf { TimerRecord.isValidId(it) }
    }
}
