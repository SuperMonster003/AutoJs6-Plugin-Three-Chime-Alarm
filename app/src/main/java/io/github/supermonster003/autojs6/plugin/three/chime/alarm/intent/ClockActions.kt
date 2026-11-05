package io.github.supermonster003.autojs6.plugin.three.chime.alarm.intent

import android.content.Context
import androidx.annotation.VisibleForTesting
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Log
import android.widget.Toast
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import org.autojs.plugin.alarm.api.AlarmContract
import java.time.ZoneId
import java.util.Calendar
import java.util.Date

/**
 * The actions of the system clock intents shared by [ClockIntentActivity] and [ClockDialogActivity] (roadmap P2.6):
 * changes are recorded with source `intent`, dismissals and snoozes with `by` `user` (T66). Each outcome is shown as a
 * toast and logged under [TAG] as one line (`<action> <outcome>`), which the device checks read.
 * zh-CN: [ClockIntentActivity] 与 [ClockDialogActivity] 共用的系统时钟 Intent 操作 (路线图 P2.6): 变更以来源 `intent` 记录, 关闭与贪睡以 `by` `user`
 * 记录 (T66). 每个结果以 toast 显示, 并以一行 (`<action> <outcome>`) 记入日志 [TAG], 供设备检查读取.
 */
internal object ClockActions {

    const val TAG = "ChimeIntent"

    private const val DATE_TIME = DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL

    /** The latest outcome line, for the instrumentation tests. zh-CN: 最近一个结果行, 供仪器测试读取. */
    @Volatile
    @VisibleForTesting
    var lastOutcome: String? = null

    /**
     * Saves [options] as a new alarm, or enables the same stored alarm instead when [reuse] (T79).
     * zh-CN: 把 [options] 保存为新闹钟; [reuse] 为 true 且已有相同的闹钟时改为启用它 (T79).
     */
    fun save(context: Context, action: String, options: Map<String, Any?>, reuse: Boolean) {
        val repository = AlarmRuntime.get(context).repository
        try {
            val same = if (reuse) ClockRequests.sameAlarm(repository.list(), options) else null
            val record = if (same != null) {
                repository.setEnabled(same.id, true, AlarmContract.SOURCE_INTENT)
            } else {
                repository.add(options, AlarmContract.SOURCE_INTENT)
            }
            val outcome = if (same != null) "enabled=${record.id}" else "added=${record.id}"
            report(context, action, outcome, context.getString(R.string.clock_toast_alarm_set, timeOf(context, record)))
        } catch (e: AlarmException) {
            failed(context, action, e)
        }
    }

    /**
     * Dismisses each of [records] as [ClockRequests.dismissPlan] says and reports them together.
     * zh-CN: 按 [ClockRequests.dismissPlan] 关闭 [records] 中的每个闹钟并合并报告.
     */
    fun dismiss(context: Context, action: String, records: List<AlarmRecord>) {
        val runtime = AlarmRuntime.get(context)
        if (records.isEmpty()) {
            report(context, action, "matched=0", context.getString(R.string.clock_toast_no_match))
            return
        }
        val ringing = runtime.rings.ringing().mapTo(HashSet()) { it.id }
        val now = System.currentTimeMillis()
        val done = ArrayList<String>()
        val early = ArrayList<String>()
        val parts = ArrayList<String>()
        for (record in records) {
            try {
                when (val plan = ClockRequests.dismissPlan(record, record.id in ringing, now, ZoneId.systemDefault())) {
                    ClockRequests.DismissPlan.EndRound -> {
                        val handled = runtime.rings.dismiss(record.id, AlarmContract.BY_USER)
                        if (handled) done += timeOf(context, record)
                        parts += "${record.id}:${if (handled) "ended" else "notRinging"}"
                    }
                    is ClockRequests.DismissPlan.Skip -> {
                        runtime.repository.skipNext(record.id, AlarmContract.SOURCE_INTENT)
                        done += timeOf(context, record)
                        parts += "${record.id}:skipped"
                    }
                    ClockRequests.DismissPlan.AlreadySkipped -> {
                        done += timeOf(context, record)
                        parts += "${record.id}:alreadySkipped"
                    }
                    is ClockRequests.DismissPlan.TooEarly -> {
                        early += DateUtils.formatDateTime(context, plan.at, DATE_TIME)
                        parts += "${record.id}:tooEarly"
                    }
                    ClockRequests.DismissPlan.NoRing -> parts += "${record.id}:noRing"
                }
            } catch (e: AlarmException) {
                parts += "${record.id}:error=${e.code}"
            }
        }
        val text = when {
            done.isNotEmpty() -> context.getString(R.string.clock_toast_dismissed, done.joinToString(", "))
            early.isNotEmpty() -> context.getString(R.string.clock_toast_too_early, early.first())
            else -> context.getString(R.string.clock_toast_no_match)
        }
        report(context, action, "matched=${records.size} " + parts.joinToString(" "), text)
    }

    /** Logs `<action> <outcome>` and shows [text] as a toast unless null. zh-CN: 记录 `<action> <outcome>`, [text] 不为 null 时以 toast 显示. */
    fun report(context: Context, action: String, outcome: String, text: String?) {
        val line = "$action $outcome"
        lastOutcome = line
        Log.i(TAG, line)
        text?.let { Toast.makeText(context.applicationContext, it, Toast.LENGTH_SHORT).show() }
    }

    fun failed(context: Context, action: String, e: AlarmException) {
        report(context, action, "error=${e.message}", context.getString(R.string.clock_toast_failed, e.detail.ifEmpty { e.code }))
    }

    /** The clock time of [record] in the device format. zh-CN: [record] 的钟点, 按设备格式. */
    fun timeOf(context: Context, record: AlarmRecord): String {
        val time = record.spec.time ?: return record.spec.at?.let { timeText(context, it) } ?: "--:--"
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
        }
        return DateFormat.getTimeFormat(context).format(calendar.time)
    }

    fun timeText(context: Context, at: Long): String = DateFormat.getTimeFormat(context).format(Date(at))

    /** One line of the alarm chooser: time and label. zh-CN: 闹钟选择列表的一行: 钟点与标签. */
    fun lineOf(context: Context, record: AlarmRecord): String =
        listOfNotNull(timeOf(context, record), record.spec.label?.takeIf { it.isNotBlank() }).joinToString("  ")
}
