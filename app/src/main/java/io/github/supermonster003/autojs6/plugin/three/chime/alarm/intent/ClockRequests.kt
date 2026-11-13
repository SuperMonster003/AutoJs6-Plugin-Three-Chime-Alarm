package io.github.supermonster003.autojs6.plugin.three.chime.alarm.intent

import android.provider.AlarmClock
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayJudge
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.NextOccurrence
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RecurrenceRule
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerDurations
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerRecord
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * The system clock intents on plain values (roadmap P2.6, D21), so JVM tests cover the extras mapping and the alarm
 * search; [ClockIntentActivity] reads the intent, acts and shows the outcome. The keys are the `AlarmClock` extras.
 *
 * - `SET_ALARM` ([setAlarm]): `EXTRA_HOUR` 0 - 23 and `EXTRA_MINUTES` 0 - 59 (absent: 0) give the time; without a
 *   valid hour the new alarm page opens. `EXTRA_MESSAGE` becomes the label (trimmed, at most 100 characters),
 *   `EXTRA_DAYS` (`Calendar.SUNDAY` 1 - `Calendar.SATURDAY` 7, an integer list or array, other values ignored) a weekly
 *   rule, `EXTRA_VIBRATE` the vibration, `EXTRA_RINGTONE` the sound (`silent` or a uri). `EXTRA_SKIP_UI` false (the
 *   default) opens the edit page filled in; true saves at once, and an alarm with the same time, no date of its own, the
 *   same repeat and label (and the same vibration and sound when given) is enabled instead of added again (T79).
 * - `DISMISS_ALARM` ([search], [matches], [dismissPlan]): see T80.
 * - `SNOOZE_ALARM` ([snoozeMinutes]): `EXTRA_ALARM_SNOOZE_DURATION` 1 - 60 minutes, absent: the alarm's own length.
 * - `SET_TIMER` ([setTimer], [sameTimer], roadmap P7.5, T140): `EXTRA_LENGTH` in seconds, 1 s - 100 h (the plugin
 *   range, T107, wider than the 24 h the platform documents, as the AOSP clock); without it the new timer dialog opens
 *   and `EXTRA_SKIP_UI` does not apply. `EXTRA_MESSAGE` becomes the label as for alarms. A timer that stands reset with
 *   the same length and label starts again instead of a new one (as the AOSP clock). `EXTRA_SKIP_UI` false (the
 *   default) also opens the timers page.
 * - `SHOW_TIMERS` ([ACTION_SHOW_TIMERS]) opens the timers page; `DISMISS_TIMER` ([ACTION_DISMISS_TIMER]) ends every
 *   ringing or snoozed timer (T141).
 *
 * zh-CN: 以普通值处理系统时钟 Intent (路线图 P2.6, D21), 使 JVM 测试覆盖 extras 映射与闹钟搜索; [ClockIntentActivity] 读取 Intent, 执行并显示结果.
 * 键为 `AlarmClock` 的 extras.
 *
 * - `SET_ALARM` ([setAlarm]): `EXTRA_HOUR` 0 - 23 与 `EXTRA_MINUTES` 0 - 59 (缺省为 0) 给出时间; 没有有效的小时则打开新建页. `EXTRA_MESSAGE`
 *   成为标签 (去掉首尾空白, 至多 100 个字符), `EXTRA_DAYS` (`Calendar.SUNDAY` 1 至 `Calendar.SATURDAY` 7, 整数列表或数组, 其他值忽略) 成为
 *   每周规则, `EXTRA_VIBRATE` 为振动, `EXTRA_RINGTONE` 为铃声 (`silent` 或 uri). `EXTRA_SKIP_UI` 为 false (默认) 时打开已填好的编辑页; 为 true
 *   时直接保存, 已有同一时间, 无自定日期 (仅有自动补上的日期), 重复与标签相同 (给出振动与铃声时也相同) 的闹钟则改为启用它而不重复添加 (T79).
 * - `DISMISS_ALARM` ([search], [matches], [dismissPlan]): 见 T80.
 * - `SNOOZE_ALARM` ([snoozeMinutes]): `EXTRA_ALARM_SNOOZE_DURATION` 1 - 60 分钟, 缺省时用闹钟自己的时长.
 * - `SET_TIMER` ([setTimer], [sameTimer], 路线图 P7.5, T140): `EXTRA_LENGTH` 以秒计, 1 秒至 100 小时 (插件的范围, T107, 与 AOSP 时钟一样宽于平台文档的
 *   24 小时); 缺省时打开新建计时器对话框, `EXTRA_SKIP_UI` 不适用. `EXTRA_MESSAGE` 与闹钟一样成为标签. 已有处于重置状态, 时长与标签都相同的计时器时
 *   重新开始它而不新建 (与 AOSP 时钟一致). `EXTRA_SKIP_UI` 为 false (默认) 时另打开计时器页.
 * - `SHOW_TIMERS` ([ACTION_SHOW_TIMERS]) 打开计时器页; `DISMISS_TIMER` ([ACTION_DISMISS_TIMER]) 结束全部正在响铃或贪睡的计时器 (T141).
 */
object ClockRequests {

    /**
     * `AlarmClock.ACTION_SHOW_TIMERS` (API 26) and `ACTION_DISMISS_TIMER` (API 28) as literals, since the plugin starts at
     * API 24 and the constants are inlined anyway.
     * zh-CN: `AlarmClock.ACTION_SHOW_TIMERS` (API 26) 与 `ACTION_DISMISS_TIMER` (API 28) 的字面值, 因为插件从 API 24 起支持, 而这些常量本就会被内联.
     */
    const val ACTION_SHOW_TIMERS = "android.intent.action.SHOW_TIMERS"

    /** See [ACTION_SHOW_TIMERS]. zh-CN: 见 [ACTION_SHOW_TIMERS]. */
    const val ACTION_DISMISS_TIMER = "android.intent.action.DISMISS_TIMER"

    /** The longest `EXTRA_LENGTH` in seconds (100 h, T107). zh-CN: `EXTRA_LENGTH` 的最大秒数 (100 小时, T107). */
    const val MAX_TIMER_SECONDS: Long = TimerDurations.MAX_MILLIS / 1000L

    /** A dismissal of a ring that is not sounding yet reaches at most this far ahead (T80). zh-CN: 尚未响铃的关闭至多提前这么久 (T80). */
    const val DISMISS_AHEAD_MILLIS: Long = 24 * 60 * 60 * 1000L

    /**
     * A `SET_ALARM` request: the alarm [options] (null when the intent has no valid time, so the new alarm page
     * opens) and [skipUi].
     * zh-CN: 一个 `SET_ALARM` 请求: 闹钟选项 [options] (Intent 没有有效时间时为 null, 此时打开新建页) 与 [skipUi].
     */
    data class SetAlarm(val options: Map<String, Any?>?, val skipUi: Boolean)

    /**
     * A `SET_TIMER` request: the length in [millis] (null when the intent has none, so the new timer dialog opens), the
     * [label] and [skipUi].
     * zh-CN: 一个 `SET_TIMER` 请求: 时长 [millis] (Intent 未给出时为 null, 此时打开新建计时器对话框), 标签 [label] 与 [skipUi].
     */
    data class SetTimer(val millis: Long?, val label: String?, val skipUi: Boolean)

    /** How a `DISMISS_ALARM` intent picks alarms. zh-CN: `DISMISS_ALARM` Intent 挑选闹钟的方式. */
    sealed interface Search {
        /** No search mode: what rings now, else every enabled alarm (the user chooses). zh-CN: 无搜索模式. */
        data object Unspecified : Search

        data object Next : Search

        data object All : Search

        data class Time(val hour: Int, val minute: Int) : Search

        data class Label(val text: String) : Search
    }

    /** What dismissing one alarm does (T80). zh-CN: 关闭一个闹钟的做法 (T80). */
    sealed interface DismissPlan {
        /** The alarm rings or snoozes: end the round. zh-CN: 闹钟在响或在贪睡: 结束本轮. */
        data object EndRound : DismissPlan

        /** Skip the ring at [at] (a one-shot alarm is disabled). zh-CN: 跳过 [at] 的响铃 (一次性闹钟改为禁用). */
        data class Skip(val at: Long) : DismissPlan

        /** The next ring is already skipped. zh-CN: 下一次响铃已被跳过. */
        data object AlreadySkipped : DismissPlan

        /** The next ring at [at] is more than a day away. zh-CN: 下一次响铃 [at] 在一天以后. */
        data class TooEarly(val at: Long) : DismissPlan

        /** The alarm will not ring again. zh-CN: 闹钟不会再响. */
        data object NoRing : DismissPlan
    }

    fun setAlarm(extras: Map<String, Any?>): SetAlarm {
        val skipUi = extras[AlarmClock.EXTRA_SKIP_UI] as? Boolean ?: false
        val hour = (extras[AlarmClock.EXTRA_HOUR] as? Int)?.takeIf { it in 0..23 } ?: return SetAlarm(null, skipUi)
        val minute = when (val value = extras[AlarmClock.EXTRA_MINUTES]) {
            null -> 0
            is Int -> value.takeIf { it in 0..59 } ?: return SetAlarm(null, skipUi)
            else -> return SetAlarm(null, skipUi)
        }
        val options = linkedMapOf<String, Any?>(AlarmOptionKeys.HOUR to hour, AlarmOptionKeys.MINUTE to minute)
        labelOf(extras[AlarmClock.EXTRA_MESSAGE])?.let { options[AlarmOptionKeys.LABEL] = it }
        repeatOf(extras[AlarmClock.EXTRA_DAYS])?.let { options[AlarmOptionKeys.REPEAT] = it }
        (extras[AlarmClock.EXTRA_VIBRATE] as? Boolean)?.let { options[AlarmOptionKeys.VIBRATE] = it }
        (extras[AlarmClock.EXTRA_RINGTONE] as? String)?.trim()?.takeIf { it.isNotEmpty() }?.let { ringtone ->
            val source = if (ringtone == AlarmClock.VALUE_RINGTONE_SILENT) AlarmContract.SOUND_SILENT else ringtone
            options[AlarmOptionKeys.SOUND] = mapOf(AlarmOptionKeys.SOUND_SOURCE to source)
        }
        return SetAlarm(options, skipUi)
    }

    /**
     * The `SET_TIMER` extras; throws `INVALID_OPTIONS` for an `EXTRA_LENGTH` outside 1 - [MAX_TIMER_SECONDS] seconds or
     * of another type.
     * zh-CN: `SET_TIMER` 的 extras; `EXTRA_LENGTH` 不在 1 - [MAX_TIMER_SECONDS] 秒或类型不符时报 `INVALID_OPTIONS`.
     */
    fun setTimer(extras: Map<String, Any?>): SetTimer {
        val millis = when (val value = extras[AlarmClock.EXTRA_LENGTH]) {
            null -> null
            is Int, is Long -> (value as Number).toLong().takeIf { it in 1..MAX_TIMER_SECONDS }?.let { it * 1000L }
                ?: throw AlarmException.invalidOptions("EXTRA_LENGTH must be 1 - $MAX_TIMER_SECONDS seconds, got $value")
            else -> throw AlarmException.invalidOptions("EXTRA_LENGTH must be an integer number of seconds")
        }
        return SetTimer(millis, labelOf(extras[AlarmClock.EXTRA_MESSAGE]), extras[AlarmClock.EXTRA_SKIP_UI] as? Boolean ?: false)
    }

    /**
     * The timer a `SET_TIMER` of [millis] and [label] starts again instead of adding one: it stands reset (not running,
     * ringing or snoozed, the whole length left) with the same length and label, as the AOSP clock reuses timers.
     * zh-CN: `SET_TIMER` ([millis], [label]) 重新开始而不新建的计时器: 处于重置状态 (未运行, 未响铃, 未贪睡, 剩余整个时长), 时长与标签都相同, 与 AOSP 时钟复用计时器的做法一致.
     */
    fun sameTimer(records: List<TimerRecord>, millis: Long, label: String?): TimerRecord? = records.firstOrNull {
        !it.running && !it.ringing && it.snoozeUntil == null && it.remainingMillis == it.durationMillis &&
            it.durationMillis == millis && it.spec.label == label
    }

    /**
     * The label of `EXTRA_MESSAGE`: trimmed, cut to [AlarmLimits.MAX_LABEL_CHARS] characters, null when blank.
     * zh-CN: `EXTRA_MESSAGE` 的标签: 去掉首尾空白, 截到 [AlarmLimits.MAX_LABEL_CHARS] 个字符, 空白时为 null.
     */
    fun labelOf(value: Any?): String? {
        val text = (value as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val count = text.codePointCount(0, text.length)
        return if (count <= AlarmLimits.MAX_LABEL_CHARS) text else text.substring(0, text.offsetByCodePoints(0, AlarmLimits.MAX_LABEL_CHARS)).trimEnd()
    }

    /**
     * The canonical weekly rule of `EXTRA_DAYS` (`Calendar` weekdays), null when it names no valid weekday.
     * zh-CN: `EXTRA_DAYS` (`Calendar` 的星期) 的规范每周规则, 没有有效星期时为 null.
     */
    fun repeatOf(value: Any?): String? {
        val days = when (value) {
            is IntArray -> value.toList()
            is Collection<*> -> value.filterIsInstance<Int>()
            else -> return null
        }.filter { it in 1..7 }.map { it - 1 }.distinct()
        return if (days.isEmpty()) null else RecurrenceRule.normalize(days)
    }

    /**
     * The stored alarm a `SET_ALARM` with [options] (from [setAlarm]) would repeat, or null (T79). The date the
     * repository fills in for a `time` alarm (T48) does not count as a date of its own.
     * zh-CN: 以 [options] (来自 [setAlarm]) 执行 `SET_ALARM` 时会重复的已存闹钟, 没有时为 null (T79). 仓库为 `time` 闹钟自动补上的日期 (T48) 不算自定日期.
     */
    fun sameAlarm(records: List<AlarmRecord>, options: Map<String, Any?>): AlarmRecord? = records.firstOrNull { record ->
        val spec = record.spec
        val time = spec.time ?: return@firstOrNull false
        time.hour == options[AlarmOptionKeys.HOUR] && time.minute == options[AlarmOptionKeys.MINUTE] && time.second == 0 &&
            (spec.date == null || record.state.dateImplicit) && spec.at == null && spec.exclude.isNullOrEmpty() && spec.holidays == null &&
            spec.repeat == options[AlarmOptionKeys.REPEAT] && spec.label == options[AlarmOptionKeys.LABEL] &&
            (options[AlarmOptionKeys.VIBRATE] as? Boolean)?.let { (spec.vibrate ?: true) == it } != false &&
            (options[AlarmOptionKeys.SOUND] as? Map<*, *>)?.let { (spec.sound?.source ?: AlarmContract.SOUND_DEFAULT) == it[AlarmOptionKeys.SOUND_SOURCE] } != false
    }

    /**
     * The search of a `DISMISS_ALARM` intent; `EXTRA_IS_PM` moves an hour below 12 to the afternoon. Throws
     * `INVALID_OPTIONS` for an unknown mode, a time mode without a valid time, or a label mode without a label.
     * zh-CN: `DISMISS_ALARM` Intent 的搜索; `EXTRA_IS_PM` 把小于 12 的小时移到下午. 未知模式, 时间模式没有有效时间, 标签模式没有标签时报 `INVALID_OPTIONS`.
     */
    fun search(extras: Map<String, Any?>): Search = when (val mode = extras[AlarmClock.EXTRA_ALARM_SEARCH_MODE]) {
        null -> Search.Unspecified
        AlarmClock.ALARM_SEARCH_MODE_NEXT -> Search.Next
        AlarmClock.ALARM_SEARCH_MODE_ALL -> Search.All
        AlarmClock.ALARM_SEARCH_MODE_TIME -> {
            val hour = (extras[AlarmClock.EXTRA_HOUR] as? Int)?.takeIf { it in 0..23 }
                ?: throw AlarmException.invalidOptions("search mode 'time' needs EXTRA_HOUR 0 - 23")
            val minute = when (val value = extras[AlarmClock.EXTRA_MINUTES]) {
                null -> 0
                is Int -> value.takeIf { it in 0..59 } ?: throw AlarmException.invalidOptions("EXTRA_MINUTES must be 0 - 59, got $value")
                else -> throw AlarmException.invalidOptions("EXTRA_MINUTES must be an integer")
            }
            val pm = extras[AlarmClock.EXTRA_IS_PM] as? Boolean ?: false
            Search.Time(if (pm && hour < 12) hour + 12 else hour, minute)
        }
        AlarmClock.ALARM_SEARCH_MODE_LABEL -> Search.Label(
            (extras[AlarmClock.EXTRA_MESSAGE] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw AlarmException.invalidOptions("search mode 'label' needs EXTRA_MESSAGE"),
        )
        else -> throw AlarmException.invalidOptions("unknown search mode '$mode'")
    }

    /**
     * The alarms [search] picks among [records] (T80): the enabled ones and those in [ringing] (sounding, waiting or
     * snoozed). [Search.Unspecified] and [Search.Next] take what rings now first; [Search.Next] otherwise takes the
     * alarms of the earliest coming ring; [Search.Time] compares the clock time (an `at` alarm: its time in [zone]);
     * [Search.Label] compares labels ignoring case. The coming rings count the holiday filter through [judge] (P8.3).
     * zh-CN: [search] 在 [records] 中挑出的闹钟 (T80): 已启用的与 [ringing] 中的 (发声, 排队或贪睡). [Search.Unspecified] 与 [Search.Next]
     * 先取正在响的; 否则 [Search.Next] 取最早一次响铃的闹钟; [Search.Time] 比较钟点 (`at` 闹钟取其在 [zone] 中的钟点); [Search.Label] 忽略大小写比较标签. 即将到来的响铃经 [judge] 计入节假日过滤 (P8.3).
     */
    fun matches(search: Search, records: List<AlarmRecord>, ringing: Set<String>, now: Long, zone: ZoneId, judge: HolidayJudge = HolidayJudge.NONE): List<AlarmRecord> {
        val candidates = records.filter { it.enabled || it.id in ringing }
        val sounding = candidates.filter { it.id in ringing }
        return when (search) {
            Search.Unspecified -> sounding.ifEmpty { candidates }
            Search.All -> candidates
            Search.Next -> sounding.ifEmpty {
                val coming = NextOccurrence.upcoming(candidates, AlarmLimits.MAX_UPCOMING_COUNT, now, zone, judge)
                val first = coming.firstOrNull()?.at ?: return emptyList()
                val ids = coming.filter { it.at == first }.mapTo(HashSet()) { it.alarmId }
                candidates.filter { it.id in ids }
            }
            is Search.Time -> candidates.filter { record ->
                val time = record.spec.time
                if (time != null) time.hour == search.hour && time.minute == search.minute
                else record.spec.at?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() }?.let { it.hour == search.hour && it.minute == search.minute } == true
            }
            is Search.Label -> candidates.filter { it.spec.label?.trim()?.lowercase(Locale.ROOT) == search.text.lowercase(Locale.ROOT) }
        }
    }

    /**
     * What dismissing [record] does (T80): a ringing or snoozed alarm ends its round; otherwise its next ring is
     * skipped when it comes within [DISMISS_AHEAD_MILLIS] (as the AOSP clock does).
     * zh-CN: 关闭 [record] 的做法 (T80): 正在响或在贪睡的闹钟结束本轮; 否则下一次响铃在 [DISMISS_AHEAD_MILLIS] 之内时跳过它 (与 AOSP 时钟一致).
     */
    fun dismissPlan(record: AlarmRecord, ringing: Boolean, now: Long, zone: ZoneId, judge: HolidayJudge = HolidayJudge.NONE): DismissPlan {
        if (ringing) return DismissPlan.EndRound
        if (record.state.skipAt != null) return DismissPlan.AlreadySkipped
        val at = NextOccurrence.upcoming(listOf(record), 1, now, zone, judge).firstOrNull()?.at ?: return DismissPlan.NoRing
        return if (at - now > DISMISS_AHEAD_MILLIS) DismissPlan.TooEarly(at) else DismissPlan.Skip(at)
    }

    /**
     * `EXTRA_ALARM_SNOOZE_DURATION`, null when absent; throws `INVALID_OPTIONS` outside 1 - 60 or for another type.
     * zh-CN: `EXTRA_ALARM_SNOOZE_DURATION`, 缺省时为 null; 不在 1 - 60 或类型不符时报 `INVALID_OPTIONS`.
     */
    fun snoozeMinutes(extras: Map<String, Any?>): Int? = when (val value = extras[AlarmClock.EXTRA_ALARM_SNOOZE_DURATION]) {
        null -> null
        is Int -> value.takeIf { it in AlarmLimits.MIN_SNOOZE_MINUTES..AlarmLimits.MAX_SNOOZE_MINUTES }
            ?: throw AlarmException.invalidOptions("EXTRA_ALARM_SNOOZE_DURATION must be ${AlarmLimits.MIN_SNOOZE_MINUTES} - ${AlarmLimits.MAX_SNOOZE_MINUTES}, got $value")
        else -> throw AlarmException.invalidOptions("EXTRA_ALARM_SNOOZE_DURATION must be an integer")
    }

    /** The `AlarmClock` extras [ClockRequests] reads. zh-CN: [ClockRequests] 读取的 `AlarmClock` extras. */
    val EXTRA_KEYS: List<String> = listOf(
        AlarmClock.EXTRA_HOUR, AlarmClock.EXTRA_MINUTES, AlarmClock.EXTRA_MESSAGE, AlarmClock.EXTRA_DAYS, AlarmClock.EXTRA_VIBRATE,
        AlarmClock.EXTRA_RINGTONE, AlarmClock.EXTRA_SKIP_UI, AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.EXTRA_IS_PM,
        AlarmClock.EXTRA_ALARM_SNOOZE_DURATION, AlarmClock.EXTRA_LENGTH,
    )
}
