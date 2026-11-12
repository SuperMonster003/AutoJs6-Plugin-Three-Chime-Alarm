package io.github.supermonster003.autojs6.plugin.three.chime.alarm.timer

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ThreeChimeAlarmApplication
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerDurations
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.appearance.screenTexts
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.format.TimeFormats
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.HomeIntents
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.HomePage
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.StopwatchSnapshot

/**
 * The ongoing notifications of the timers and the stopwatch (roadmap P7.1 / P7.2, T121 - T123), on the silent `timer`
 * channel of low importance:
 *
 * - one per running or paused timer (tag `timer`): a running one counts down on the system chronometer to its end, a
 *   paused one shows the time left; actions pause or resume, add 1 minute, cancel. A ringing timer has none (the ring
 *   notification takes over).
 * - one for a running or paused stopwatch while the setting allows it (on by default, T122): a running one counts up on
 *   the chronometer; actions pause or resume, lap (running) or reset (paused).
 *
 * Tapping opens the alarm list until the timer screens of roadmap P7.4 exist (T123). Without the notification
 * permission (API 33+) nothing shows and the timers still ring.
 *
 * zh-CN: 计时器与秒表的常驻通知 (路线图 P7.1 / P7.2, T121 - T123), 在低重要性的静音 `timer` 渠道上: 每个运行中或已暂停的计时器一条 (tag `timer`),
 * 运行中的以系统计时器倒数到点, 已暂停的显示剩余时间, 动作为暂停或继续, 加 1 分钟, 取消; 响铃中的计时器没有 (由响铃通知接替). 设置允许时 (默认开启,
 * T122) 运行中或已暂停的秒表一条, 运行中的以系统计时器正计时, 动作为暂停或继续, 计圈 (运行中) 或重置 (已暂停). 在路线图 P7.4 的计时器页面出现之前,
 * 点按打开闹钟列表 (T123). 没有通知权限 (API 33+) 时不显示, 计时器照常响铃.
 */
object TimerNotifications {

    private const val TAG = AndroidTimerPort.TAG
    private const val TAG_TIMER = "timer"
    private const val TAG_STOPWATCH = "stopwatch"

    /** Distinct from the ring (1) and keep-alive (2) notifications. zh-CN: 与响铃 (1), 保活 (2) 通知不同. */
    private const val STOPWATCH_ID = 3

    private const val PREFERENCES = "timer-notifications"
    private const val KEY_STOPWATCH = "stopwatch"

    /**
     * Whether the stopwatch shows its notification (T122): on by default, switched in the "Timers and stopwatch" group
     * of the settings page (roadmap P7.4).
     * zh-CN: 秒表是否显示通知 (T122): 默认开启, 在设置页的 "计时器与秒表" 分组中切换 (路线图 P7.4).
     */
    fun stopwatchShown(context: Context): Boolean = runCatching { preferences(context).getBoolean(KEY_STOPWATCH, true) }.getOrDefault(true)

    @SuppressLint("ApplySharedPref", "UseKtx")
    fun setStopwatchShown(context: Context, value: Boolean): Boolean =
        runCatching { preferences(context).edit().putBoolean(KEY_STOPWATCH, value).commit() }.getOrDefault(false)

    private fun preferences(context: Context) =
        context.createDeviceProtectedStorageContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    /** Posts, updates or removes the notification of [id] ([record] null once removed). zh-CN: 发出, 更新或移除 [id] 的通知 ([record] 为 null 表示已删除). */
    fun timer(context: Context, id: String, record: TimerRecord?, now: Long = System.currentTimeMillis()) {
        val manager = NotificationManagerCompat.from(context)
        if (record == null || record.ringing) {
            manager.cancel(TAG_TIMER, id.hashCode())
            return
        }
        val texts = screenTexts(context)
        val label = record.spec.label?.takeIf { it.isNotBlank() } ?: texts.getString(R.string.timer_default_label)
        val builder = base(context, HomePage.TIMERS)
            .setContentTitle(label)
            .setSortKey("%020d".format(java.util.Locale.ROOT, record.createdAt))
        val end = record.endsAt
        if (record.running && end != null) {
            builder.setContentText(texts.getString(R.string.timer_notification_ends, TimeFormats.timeOf(texts, end)))
                .setWhen(end)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(0, texts.getString(R.string.timer_action_pause), action(context, ACTION_TIMER_PAUSE, id))
        } else {
            builder.setContentText(texts.getString(R.string.timer_notification_paused, TimerDurations.clockText(record.toSnapshot(now).remainingMillis)))
                .setShowWhen(false)
                .addAction(0, texts.getString(R.string.timer_action_resume), action(context, ACTION_TIMER_RESUME, id))
        }
        builder.addAction(0, texts.getString(R.string.timer_action_add_minute), action(context, ACTION_TIMER_ADD_MINUTE, id))
            .addAction(0, texts.getString(R.string.timer_action_cancel), action(context, ACTION_TIMER_CANCEL, id))
        notify(manager, TAG_TIMER, id.hashCode(), builder)
    }

    /** Posts, updates or removes the stopwatch notification. zh-CN: 发出, 更新或移除秒表通知. */
    fun stopwatch(context: Context, snapshot: StopwatchSnapshot, now: Long = System.currentTimeMillis()) {
        val manager = NotificationManagerCompat.from(context)
        if (snapshot.state == AlarmContract.STOPWATCH_IDLE || !stopwatchShown(context)) {
            manager.cancel(TAG_STOPWATCH, STOPWATCH_ID)
            return
        }
        val texts = screenTexts(context)
        val builder = base(context, HomePage.STOPWATCH).setContentTitle(texts.getString(R.string.stopwatch_title))
        if (snapshot.state == AlarmContract.STOPWATCH_RUNNING) {
            builder.setWhen(now - snapshot.elapsedMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .addAction(0, texts.getString(R.string.timer_action_pause), action(context, ACTION_STOPWATCH_PAUSE, null))
                .addAction(0, texts.getString(R.string.stopwatch_action_lap), action(context, ACTION_STOPWATCH_LAP, null))
        } else {
            val shown = TimerDurations.clockText(snapshot.elapsedMillis / 1000 * 1000)
            builder.setContentText(texts.getString(R.string.stopwatch_notification_paused, shown))
                .setShowWhen(false)
                .addAction(0, texts.getString(R.string.timer_action_resume), action(context, ACTION_STOPWATCH_RESUME, null))
                .addAction(0, texts.getString(R.string.stopwatch_action_reset), action(context, ACTION_STOPWATCH_RESET, null))
        }
        if (snapshot.laps.isNotEmpty()) builder.setSubText(texts.resources.getQuantityString(R.plurals.stopwatch_laps, snapshot.laps.size, snapshot.laps.size))
        notify(manager, TAG_STOPWATCH, STOPWATCH_ID, builder)
    }

    /**
     * Brings every notification in line with [timers] and the [stopwatch] (process start, reboot), removing those of timers
     * that are gone.
     * zh-CN: 让全部通知与 [timers] 和 [stopwatch] 一致 (进程启动, 重启后), 并移除已不存在的计时器的通知.
     */
    fun refreshAll(context: Context, timers: List<TimerRecord>, stopwatch: StopwatchSnapshot) {
        val known = timers.mapTo(HashSet()) { it.id.hashCode() }
        runCatching {
            context.getSystemService(NotificationManager::class.java).activeNotifications
                .filter { it.tag == TAG_TIMER && it.id !in known }
                .forEach { NotificationManagerCompat.from(context).cancel(TAG_TIMER, it.id) }
        }
        timers.forEach { timer(context, it.id, it) }
        stopwatch(context, stopwatch)
    }

    /**
     * A tap on a timer notification opens the timers page and one on the stopwatch notification the stopwatch page (T123,
     * roadmap P7.4).
     * zh-CN: 点按计时器通知打开计时器页, 点按秒表通知打开秒表页 (T123, 路线图 P7.4).
     */
    private fun base(context: Context, page: HomePage): NotificationCompat.Builder = NotificationCompat.Builder(context, ThreeChimeAlarmApplication.CHANNEL_TIMER)
        .setSmallIcon(R.drawable.ic_alarm_24)
        .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setLocalOnly(true)
        .setContentIntent(HomeIntents.pending(context, page))

    private fun notify(manager: NotificationManagerCompat, tag: String, id: Int, builder: NotificationCompat.Builder) {
        try {
            manager.notify(tag, id, builder.build())
        } catch (e: SecurityException) {
            // Notifications are not allowed (API 33+). zh-CN: 未授予通知权限 (API 33+).
            Log.w(TAG, "$tag notification refused: ${e.message}")
        }
    }

    private fun action(context: Context, action: String, timerId: String?): PendingIntent {
        val intent = Intent(context, TimerActionReceiver::class.java).setAction(action)
        if (timerId != null) intent.data = AndroidTimerPort.uriOf(timerId)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private const val ACTION_PREFIX = "io.github.supermonster003.autojs6.plugin.three.chime.alarm.action."
    const val ACTION_TIMER_PAUSE = ACTION_PREFIX + "TIMER_PAUSE"
    const val ACTION_TIMER_RESUME = ACTION_PREFIX + "TIMER_RESUME"
    const val ACTION_TIMER_ADD_MINUTE = ACTION_PREFIX + "TIMER_ADD_MINUTE"
    const val ACTION_TIMER_CANCEL = ACTION_PREFIX + "TIMER_CANCEL"
    const val ACTION_STOPWATCH_PAUSE = ACTION_PREFIX + "STOPWATCH_PAUSE"
    const val ACTION_STOPWATCH_RESUME = ACTION_PREFIX + "STOPWATCH_RESUME"
    const val ACTION_STOPWATCH_LAP = ACTION_PREFIX + "STOPWATCH_LAP"
    const val ACTION_STOPWATCH_RESET = ACTION_PREFIX + "STOPWATCH_RESET"
}
