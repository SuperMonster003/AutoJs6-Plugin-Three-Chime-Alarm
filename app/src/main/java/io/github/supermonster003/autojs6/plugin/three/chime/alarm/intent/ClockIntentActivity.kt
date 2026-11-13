package io.github.supermonster003.autojs6.plugin.three.chime.alarm.intent

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.format.TimeFormats
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.EditAlarmActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.HomeActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.HomeIntents
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.HomePage
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.TimerSpec
import java.time.ZoneId

/**
 * Answers the system clock intents (roadmap D21, P2.6) without a window of its own: `SET_ALARM`, `SHOW_ALARMS`,
 * `DISMISS_ALARM` and `SNOOZE_ALARM` (the extras are mapped by [ClockRequests]). Callers hold
 * `com.android.alarm.permission.SET_ALARM` (Manifest). Changes are recorded with source `intent`; dismissals and
 * snoozes with `by` `user` (T66). A `SET_ALARM` without `SKIP_UI` opens the edit page ([EditAlarmActivity.draft], roadmap
 * P3.1), the alarm chooser is [ClockDialogActivity], the alarm list is [HomeActivity]. Each outcome is a toast and one
 * line of `adb logcat -s ChimeIntent` ([ClockActions]). Every request gets a task of its own (`documentLaunchMode`,
 * roadmap Q25, T104); on API 24 - 27 [ClockRescue] starts a request the system left behind the home screen (Q27, T105).
 * Since 1.1.0 (roadmap P7.5) also `SET_TIMER`, `SHOW_TIMERS` and `DISMISS_TIMER` ([ClockRequests.setTimer], T140,
 * T141): timers are started with source `intent` and dismissed `by` `user`; the timers page is the second page of
 * [HomeActivity].
 * zh-CN:不带自身窗口地应答系统时钟 Intent (路线图 D21, P2.6): `SET_ALARM`, `SHOW_ALARMS`, `DISMISS_ALARM` 与 `SNOOZE_ALARM` (extras 由
 * [ClockRequests] 映射). 调用方须持有 `com.android.alarm.permission.SET_ALARM` (Manifest). 变更以来源 `intent` 记录; 关闭与贪睡以 `by` `user`
 * 记录 (T66). 不带 `SKIP_UI` 的 `SET_ALARM` 打开编辑页 ([EditAlarmActivity.draft], 路线图 P3.1), 闹钟选择由 [ClockDialogActivity] 提供, 闹钟列表为
 * [HomeActivity]. 每个结果为一个 toast 与 `adb logcat -s ChimeIntent` 的一行 ([ClockActions]). 每个请求各有自己的任务 (`documentLaunchMode`, 路线图
 * Q25, T104); 在 API 24 - 27 上, 被系统留在桌面后面的请求由 [ClockRescue] 启动 (Q27, T105). 自 1.1.0 起 (路线图 P7.5) 另应答 `SET_TIMER`,
 * `SHOW_TIMERS` 与 `DISMISS_TIMER` ([ClockRequests.setTimer], T140, T141): 计时器以来源 `intent` 启动, 以 `by` `user` 关闭; 计时器页为
 * [HomeActivity] 的第二页.
 */
class ClockIntentActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handle(intent)
        ClockRescue.schedule(this)
        finish()
    }

    private fun handle(intent: Intent) {
        val action = nameOf(intent.action)
        try {
            val extras = extrasOf(intent)
            when (intent.action) {
                AlarmClock.ACTION_SET_ALARM -> setAlarm(action, extras)
                AlarmClock.ACTION_SHOW_ALARMS -> {
                    startActivity(HomeIntents.of(this, HomePage.ALARMS))
                    ClockActions.report(this, action, "shown", null)
                }
                AlarmClock.ACTION_DISMISS_ALARM -> dismissAlarm(action, extras)
                AlarmClock.ACTION_SNOOZE_ALARM -> snoozeAlarm(action, extras)
                AlarmClock.ACTION_SET_TIMER -> setTimer(action, extras)
                ClockRequests.ACTION_SHOW_TIMERS -> {
                    startActivity(HomeIntents.of(this, HomePage.TIMERS))
                    ClockActions.report(this, action, "shown", null)
                }
                ClockRequests.ACTION_DISMISS_TIMER -> dismissTimer(action)
                else -> ClockActions.report(this, action, "ignored", null)
            }
        } catch (e: AlarmException) {
            ClockActions.failed(this, action, e)
        }
    }

    private fun setAlarm(action: String, extras: Map<String, Any?>) {
        val request = ClockRequests.setAlarm(extras)
        val options = request.options
        if (options == null || !request.skipUi) {
            startActivity(EditAlarmActivity.draft(this, options))
            ClockActions.report(this, action, if (options == null) "draft=new" else "draft=filled", null)
        } else {
            ClockActions.save(this, action, options, reuse = true)
        }
    }

    private fun dismissAlarm(action: String, extras: Map<String, Any?>) {
        val search = ClockRequests.search(extras)
        val runtime = AlarmRuntime.get(this)
        val ringing = runtime.rings.ringing().mapTo(HashSet()) { it.id }
        val found = ClockRequests.matches(search, runtime.repository.list(), ringing, System.currentTimeMillis(), ZoneId.systemDefault(), runtime.holidayJudge)
        if (found.size > 1 && search != ClockRequests.Search.All) {
            startActivity(ClockDialogActivity.choose(this, found.map { it.id }))
            ClockActions.report(this, action, "matched=${found.size} choose", null)
        } else {
            ClockActions.dismiss(this, action, found)
        }
    }

    private fun snoozeAlarm(action: String, extras: Map<String, Any?>) {
        val minutes = ClockRequests.snoozeMinutes(extras)
        val rings = AlarmRuntime.get(this).rings
        val sounding = rings.ringing().count { !it.snoozed }
        when {
            sounding == 0 -> ClockActions.report(this, action, "ringing=0", getString(R.string.clock_toast_not_ringing))
            rings.snooze(null, minutes, AlarmContract.BY_USER) ->
                ClockActions.report(this, action, "snoozed ringing=$sounding minutes=${minutes ?: "default"}", getString(R.string.clock_toast_snoozed))
            else -> ClockActions.report(this, action, "refused ringing=$sounding", getString(R.string.clock_toast_snooze_refused))
        }
    }

    private fun setTimer(action: String, extras: Map<String, Any?>) {
        val request = ClockRequests.setTimer(extras)
        val millis = request.millis
        if (millis == null) {
            startActivity(HomeIntents.of(this, HomePage.TIMERS, customTimer = true))
            ClockActions.report(this, action, "draft=new", null)
            return
        }
        val runtime = AlarmRuntime.get(this)
        val same = ClockRequests.sameTimer(runtime.timers.list(), millis, request.label)
        val record = if (same != null) runtime.timers.resume(same.id) else runtime.intentTimerApi.start(millis, TimerSpec(label = request.label))
        if (!request.skipUi) startActivity(HomeIntents.of(this, HomePage.TIMERS))
        val outcome = if (same != null) "resumed=${record.id}" else "started=${record.id}"
        ClockActions.report(this, action, outcome, getString(R.string.clock_toast_timer_set, TimeFormats.duration(this, millis)))
    }

    private fun dismissTimer(action: String) {
        val rings = AlarmRuntime.get(this).rings
        val timers = rings.ringing().filter { it.kind == AlarmContract.KIND_TIMER }
        val done = timers.filter { rings.dismiss(it.id, AlarmContract.BY_USER) }
        val text = if (done.isEmpty()) {
            getString(R.string.clock_toast_timer_not_ringing)
        } else {
            getString(R.string.clock_toast_timer_dismissed, done.joinToString(", ") { it.label ?: getString(R.string.timer_default_label) })
        }
        ClockActions.report(this, action, "ringing=${timers.size} dismissed=${done.size}", text)
    }

    companion object {

        /** The short action name of the log lines. zh-CN: 日志行中的简短 action 名. */
        fun nameOf(action: String?): String = when (action) {
            AlarmClock.ACTION_SET_ALARM -> "setAlarm"
            AlarmClock.ACTION_SHOW_ALARMS -> "showAlarms"
            AlarmClock.ACTION_DISMISS_ALARM -> "dismissAlarm"
            AlarmClock.ACTION_SNOOZE_ALARM -> "snoozeAlarm"
            AlarmClock.ACTION_SET_TIMER -> "setTimer"
            ClockRequests.ACTION_SHOW_TIMERS -> "showTimers"
            ClockRequests.ACTION_DISMISS_TIMER -> "dismissTimer"
            else -> action ?: "none"
        }

        /**
         * The `AlarmClock` extras of [intent]; an extra that cannot be read (a foreign Parcelable) leaves all of them out.
         * zh-CN: [intent] 中的 `AlarmClock` extras; 有无法读取的 extra (外来的 Parcelable) 时全部忽略.
         */
        fun extrasOf(intent: Intent): Map<String, Any?> = try {
            val bundle = intent.extras ?: Bundle.EMPTY
            ClockRequests.EXTRA_KEYS.filter { bundle.containsKey(it) }.associateWith {
                @Suppress("DEPRECATION")
                bundle.get(it)
            }
        } catch (_: RuntimeException) {
            emptyMap()
        }
    }
}
