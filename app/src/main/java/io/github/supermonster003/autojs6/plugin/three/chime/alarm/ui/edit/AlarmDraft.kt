package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmOptionsValidator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayFilter
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayJudge
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.NextOccurrence
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RecurrenceRule
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RecurrenceRule.Freq
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RecurrenceRule.WeekdayNum
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.TimeZonePolicy
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmDismiss
import org.autojs.plugin.alarm.api.AlarmHolidayFilter
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import org.autojs.plugin.alarm.api.AlarmSnooze
import org.autojs.plugin.alarm.api.AlarmSound
import org.autojs.plugin.alarm.api.AlarmSpeak
import org.autojs.plugin.alarm.api.AlarmSpec
import org.autojs.plugin.alarm.api.AlarmTime
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * The alarm being edited on the edit page (roadmap P3.1, T83). [dateAuto] means the date follows the time as for a new
 * alarm (T48): while it is set, a change of the time, the repeat or the time zone drops the date so that it is filled in
 * again on save, as the repository does for an implicit date. An `at` alarm (a fixed instant from a script) becomes a
 * `time` alarm with an explicit date on the first change of its schedule, read in its own time zone. Options the page
 * leaves at "default" stay null, so the alarm follows the plugin defaults (roadmap P3.5). The holiday filter (roadmap
 * P8.6) changes the schedule like the repeat does.
 * zh-CN: 编辑页正在编辑的闹钟 (路线图 P3.1, T83). [dateAuto] 表示日期像新建闹钟那样跟随时刻 (T48): 设置时, 修改时刻, 重复或时区会丢弃日期, 保存时重新补上,
 * 与仓库处理隐式日期的方式一致. `at` 闹钟 (脚本给出的固定时刻) 在第一次修改日程时按自身时区转为带显式日期的 `time` 闹钟. 页面上保持 "默认" 的选项为 null,
 * 闹钟因此跟随插件默认值 (路线图 P3.5). 节假日过滤 (路线图 P8.6) 与重复一样会改变日程.
 */
internal data class AlarmDraft(val spec: AlarmSpec, val dateAuto: Boolean) {

    /**
     * The alarm has a dismiss challenge (1.3.0, roadmap P9.5: chosen on this page, or `script` set by a script), which
     * lengthens the default auto silence.
     * zh-CN: 闹钟带关闭挑战 (1.3.0, 路线图 P9.5: 在本页选择, 或由脚本设置的 `script`), 它会延长默认的自动停止时长.
     */
    val hasChallenge: Boolean get() = spec.dismiss?.challenge.let { it != null && it != AlarmContract.CHALLENGE_NONE }

    /** The time shown on the page; an `at` alarm shows its instant in its zone. zh-CN: 页面显示的时刻; `at` 闹钟显示其时刻在其时区的时间. */
    fun timeOf(deviceZone: ZoneId): AlarmTime = spec.time ?: localOfAt(deviceZone)!!.let { AlarmTime(it.hour, it.minute, it.second) }

    /**
     * The date shown on the page: the stored or chosen date, else the date a new alarm would get at [now].
     * zh-CN: 页面显示的日期: 已存或已选的日期, 否则为新建闹钟在 [now] 时会得到的日期.
     */
    fun dateOf(now: Long, deviceZone: ZoneId): LocalDate = when {
        spec.at != null -> localOfAt(deviceZone)!!.toLocalDate()
        spec.date != null -> LocalDate.parse(spec.date)
        else -> NextOccurrence.implicitDate(spec, now, TimeZonePolicy.zoneOf(spec, deviceZone))
    }

    /**
     * The next [count] rings as saved now, holidays judged by [judge] (roadmap P8.6); an empty list when the rule never
     * rings. A [repeat] other than the current one previews the draft [withRepeat] would give.
     * zh-CN: 按现在保存时接下来的 [count] 次响铃, 节假日按 [judge] 判定 (路线图 P8.6); 规则不再响铃时为空. 给出与当前不同的 [repeat] 时, 预览 [withRepeat]
     * 会得到的草稿.
     */
    fun preview(count: Int, now: Long, deviceZone: ZoneId, repeat: String? = spec.repeat, judge: HolidayJudge = HolidayJudge.NONE): List<Long> = try {
        val draft = if (repeat == spec.repeat) this else withRepeat(repeat, deviceZone)
        NextOccurrence.preview(draft.saved(), count, now, deviceZone, judge)
    } catch (_: AlarmException) {
        emptyList()
    } catch (_: IllegalArgumentException) {
        emptyList()
    }

    /**
     * How many instances a holiday takes away between [now] and [next] (roadmap P8.6), or 0 when that cannot be computed.
     * zh-CN: [now] 与 [next] 之间被节假日拿掉的次数 (路线图 P8.6), 无法计算时为 0.
     */
    fun holidaySkips(now: Long, next: Long, deviceZone: ZoneId, judge: HolidayJudge): Int = try {
        NextOccurrence.holidaySkips(saved(), now, next, deviceZone, judge)
    } catch (_: AlarmException) {
        0
    } catch (_: IllegalArgumentException) {
        0
    }

    /**
     * The rule the repeat dialog shows: `repeat`, or every day for a `workdays` alarm without one (T221).
     * zh-CN: 重复对话框显示的规则: `repeat`, 没有 `repeat` 的 `workdays` 闹钟为每天 (T221).
     */
    val shownRepeat: String? get() = HolidayFilter.baseRule(spec)

    fun withTime(hour: Int, minute: Int, deviceZone: ZoneId): AlarmDraft = scheduled(deviceZone).let { base ->
        val old = base.spec.time
        val second = if (old != null && old.hour == hour && old.minute == minute) old.second else 0
        base.rescheduled(base.spec.copy(time = AlarmTime(hour, minute, second)))
    }

    /** A null [date] lets the date follow the time again. zh-CN: [date] 为 null 时日期重新跟随时刻. */
    fun withDate(date: LocalDate?, deviceZone: ZoneId): AlarmDraft = scheduled(deviceZone).let { base ->
        if (date == null) base.copy(spec = base.spec.copy(date = null), dateAuto = true)
        else base.copy(spec = base.spec.copy(date = date.toString()), dateAuto = false)
    }

    /**
     * A new repeat rule. In `workdays` mode (T221): every day keeps the mode (`repeat` omitted), once drops the holiday
     * filter, and any other rule `workdays` does not allow (T180) turns the mode into `skip`.
     * zh-CN: 新的重复规则. `workdays` 模式下 (T221): 每天保持该模式 (省略 `repeat`), 一次删除节假日过滤, `workdays` 不允许的其他规则 (T180) 把模式改为 `skip`.
     */
    fun withRepeat(repeat: String?, deviceZone: ZoneId): AlarmDraft = scheduled(deviceZone).let { base ->
        val filter = base.spec.holidays
        if (filter?.mode != AlarmContract.HOLIDAY_MODE_WORKDAYS) return@let base.rescheduled(base.spec.copy(repeat = repeat))
        val holidays = when {
            repeat == null -> null
            allowsWorkdays(repeat) -> filter
            else -> filter.copy(mode = AlarmContract.HOLIDAY_MODE_SKIP)
        }
        val rule = if (holidays?.mode == AlarmContract.HOLIDAY_MODE_WORKDAYS && repeat == DAILY) null else repeat
        base.rescheduled(base.spec.copy(repeat = rule, holidays = holidays))
    }

    /**
     * The holiday filter (roadmap P8.6, T220); null removes it. The rule the dates come from stays: `workdays` keeps a
     * rule it allows and drops any other (`repeat` omitted: every workday, T180), an `at` alarm becoming a `time` alarm;
     * leaving `workdays` makes its every day explicit (`FREQ=DAILY`).
     * zh-CN: 节假日过滤 (路线图 P8.6, T220); null 为删除. 日期所依据的规则保持不变: `workdays` 保留它允许的规则, 丢弃其他规则 (省略 `repeat`: 每个工作日,
     * T180), `at` 闹钟转为 `time` 闹钟; 离开 `workdays` 时把它的每天写成显式的 `FREQ=DAILY`.
     */
    fun withHolidays(filter: AlarmHolidayFilter?, deviceZone: ZoneId): AlarmDraft {
        val old = spec.holidays
        if (filter == old) return this
        val workdays = filter?.mode == AlarmContract.HOLIDAY_MODE_WORKDAYS
        val wasWorkdays = old?.mode == AlarmContract.HOLIDAY_MODE_WORKDAYS
        if (workdays == wasWorkdays) return copy(spec = spec.copy(holidays = filter))
        return scheduled(deviceZone).let { base ->
            val repeat = base.spec.repeat
            val rule = when {
                workdays -> repeat?.takeIf { allowsWorkdays(it) }
                base.spec.time != null && repeat == null -> DAILY
                else -> repeat
            }
            base.rescheduled(base.spec.copy(repeat = rule, holidays = filter))
        }
    }

    /** A null [id] follows the device zone; the wall-clock time stays. zh-CN: [id] 为 null 时跟随设备时区; 墙上时间不变. */
    fun withTimeZone(id: String?, deviceZone: ZoneId): AlarmDraft =
        scheduled(deviceZone).let { it.rescheduled(it.spec.copy(timeZone = id)) }

    fun withExclude(dates: Collection<LocalDate>): AlarmDraft =
        copy(spec = spec.copy(exclude = dates.toSortedSet().map(LocalDate::toString).ifEmpty { null }))

    fun withLabel(text: String): AlarmDraft = copy(spec = spec.copy(label = text.trim().ifEmpty { null }))

    /** The volume and ramp of the stored sound stay. zh-CN: 保留已存铃声的音量与渐强. */
    fun withSound(source: String?): AlarmDraft {
        val sound = (spec.sound ?: AlarmSound()).copy(source = source)
        return copy(spec = spec.copy(sound = sound.takeUnless { it == AlarmSound() }))
    }

    /** Turning vibration off drops the pattern, as the validator does. zh-CN: 关闭振动时丢弃振动模式, 与校验器一致. */
    fun withVibrate(on: Boolean): AlarmDraft =
        copy(spec = spec.copy(vibrate = on, vibratePattern = if (on) spec.vibratePattern else null))

    fun withSpeak(speak: AlarmSpeak?): AlarmDraft = copy(spec = spec.copy(speak = speak))

    fun withSnooze(snooze: AlarmSnooze?): AlarmDraft = copy(spec = spec.copy(snooze = snooze))

    fun withAutoSilence(minutes: Int?): AlarmDraft = copy(spec = spec.copy(autoSilenceMinutes = minutes))

    /** The dismiss challenge (roadmap P9.5, T291); null drops it. zh-CN: 关闭挑战 (路线图 P9.5, T291); null 表示移除. */
    fun withDismiss(dismiss: AlarmDismiss?): AlarmDraft = copy(spec = spec.copy(dismiss = dismiss))

    /** The options of a new alarm. zh-CN: 新建闹钟的选项. */
    fun toOptions(): Map<String, Any?> = saved().toValues()

    /**
     * Whether a system clock `SET_ALARM` may enable an equal stored alarm instead of adding one (T79): only while the
     * draft has nothing beyond what that comparison looks at.
     * zh-CN: 系统时钟 `SET_ALARM` 能否改为启用相同的已存闹钟而不新建 (T79): 仅当草稿没有超出该比较范围的内容时.
     */
    val reusable: Boolean
        get() = with(saved()) {
            time?.second == 0 && date == null && at == null && exclude == null && timeZone == null && holidays == null &&
                vibratePattern == null && speak == null && snooze == null && autoSilenceMinutes == null && dismiss == null &&
                sound?.let { it == AlarmSound(source = it.source) } != false
        }

    /**
     * The change from [record]: a T29 patch with only what differs, and whether the date becomes implicit again. A
     * disabled alarm whose schedule changes is enabled.
     * zh-CN: 相对 [record] 的修改: 只含差异的 T29 patch, 以及日期是否重新变为隐式. 日程有修改的已禁用闹钟会被启用.
     */
    fun toUpdate(record: AlarmRecord): EditUpdate {
        val old = record.spec
        val new = spec
        val patch = LinkedHashMap<String, Any?>()
        val time = new.time
        if (time != null && (time != old.time || old.at != null)) {
            patch[AlarmOptionKeys.HOUR] = time.hour
            patch[AlarmOptionKeys.MINUTE] = time.minute
            patch[AlarmOptionKeys.SECOND] = time.second
        }
        val implicitDate = dateAuto && time != null && new.date == null
        if (!dateAuto && time != null && (record.state.dateImplicit || new.date != old.date || old.at != null)) {
            patch[AlarmOptionKeys.DATE] = new.date
        }
        if (new.repeat != old.repeat) patch[AlarmOptionKeys.REPEAT] = new.repeat
        if (new.exclude.orEmpty() != old.exclude.orEmpty()) patch[AlarmOptionKeys.EXCLUDE] = new.exclude?.ifEmpty { null }
        if (new.timeZone != old.timeZone) patch[AlarmOptionKeys.TIME_ZONE] = new.timeZone
        if (new.holidays != old.holidays) patch[AlarmOptionKeys.HOLIDAYS] = new.holidays?.toValues()
        val schedule = patch.isNotEmpty() || implicitDate
        if (new.label != old.label) patch[AlarmOptionKeys.LABEL] = new.label
        if (new.sound != old.sound) patch[AlarmOptionKeys.SOUND] = new.sound?.toValues()
        if ((new.vibrate ?: true) != (old.vibrate ?: true)) patch[AlarmOptionKeys.VIBRATE] = new.vibrate ?: true
        if (new.speak != old.speak) patch[AlarmOptionKeys.SPEAK] = new.speak?.toValues()
        if (new.snooze != old.snooze) patch[AlarmOptionKeys.SNOOZE] = new.snooze?.toValues()
        if (new.dismiss != old.dismiss) patch[AlarmOptionKeys.DISMISS] = new.dismiss?.toValues()
        if (new.autoSilenceMinutes != old.autoSilenceMinutes) patch[AlarmOptionKeys.AUTO_SILENCE_MINUTES] = new.autoSilenceMinutes
        if (schedule && !record.enabled) patch[AlarmOptionKeys.ENABLED] = true
        return EditUpdate(patch, implicitDate)
    }

    /** The spec as it is saved: an automatic date is left to the repository. zh-CN: 保存时的 spec: 自动日期交给仓库补上. */
    private fun saved(): AlarmSpec = if (dateAuto && spec.time != null) spec.copy(date = null) else spec

    private fun localOfAt(deviceZone: ZoneId): LocalDateTime? =
        spec.at?.let { Instant.ofEpochMilli(it).atZone(TimeZonePolicy.zoneOf(spec, deviceZone)).toLocalDateTime() }

    /** An `at` alarm as a `time` alarm with its date. zh-CN: 把 `at` 闹钟转为带日期的 `time` 闹钟. */
    private fun scheduled(deviceZone: ZoneId): AlarmDraft {
        val local = localOfAt(deviceZone) ?: return this
        val time = AlarmTime(local.hour, local.minute, local.second)
        return AlarmDraft(spec.copy(at = null, time = time, date = local.toLocalDate().toString()), dateAuto = false)
    }

    private fun rescheduled(changed: AlarmSpec): AlarmDraft = copy(spec = if (dateAuto) changed.copy(date = null) else changed)

    companion object {

        private const val DAILY = "FREQ=DAILY"

        private fun allowsWorkdays(rule: String): Boolean = runCatching { HolidayFilter.allowsWorkdays(RecurrenceRule.parse(rule)) }.getOrDefault(false)

        fun of(record: AlarmRecord): AlarmDraft = AlarmDraft(record.spec, record.state.dateImplicit)

        /**
         * A new alarm: the next whole hour in the device zone, once, the date following the time (T83).
         * zh-CN: 新建闹钟: 设备时区的下一个整点, 只响一次, 日期跟随时刻 (T83).
         */
        fun forNew(now: Long, deviceZone: ZoneId): AlarmDraft {
            val next = Instant.ofEpochMilli(now).atZone(deviceZone).truncatedTo(ChronoUnit.HOURS).plusHours(1)
            return AlarmDraft(AlarmSpec(time = AlarmTime(next.hour, 0)), dateAuto = true)
        }

        /**
         * The draft of a system clock `SET_ALARM` without `SKIP_UI` (T79): its options, else a new alarm when it gave no
         * valid time.
         * zh-CN: 不带 `SKIP_UI` 的系统时钟 `SET_ALARM` 的草稿 (T79): 取其选项, 未给出有效时刻时为新建闹钟.
         */
        fun fromOptions(options: Map<String, Any?>?, now: Long, deviceZone: ZoneId): AlarmDraft {
            val spec = options?.let { runCatching { AlarmOptionsValidator().options(it) }.getOrNull() }
                ?: return forNew(now, deviceZone)
            return AlarmDraft(spec, dateAuto = spec.time != null && spec.date == null)
        }
    }
}

/** A change for [io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository.update]. zh-CN: 交给仓库 `update` 的修改. */
internal data class EditUpdate(val patch: Map<String, Any?>, val implicitDate: Boolean) {
    val isEmpty: Boolean get() = patch.isEmpty() && !implicitDate
}

/**
 * The repeat presets of the edit page (T83): once, every day, Monday to Friday, weekends, chosen weekdays (each a plain
 * weekly rule starting on Monday weeks), and the advanced rule editor for everything else.
 * zh-CN: 编辑页的重复预设 (T83): 一次, 每天, 周一至周五, 周末, 自选星期 (均为周从周一开始的普通每周规则), 其余由高级规则编辑器处理.
 */
internal enum class RepeatPreset {
    ONCE, DAILY, WEEKDAYS, WEEKEND, CUSTOM, ADVANCED;

    companion object {

        val WEEKDAY_SET: Set<DayOfWeek> = DayOfWeek.entries.take(5).toSet()
        val WEEKEND_SET: Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

        fun of(repeat: String?): RepeatPreset {
            repeat ?: return ONCE
            val rule = runCatching { RecurrenceRule.parse(repeat) }.getOrNull() ?: return ADVANCED
            val simple = rule.interval == 1 && rule.count == null && rule.until == null && rule.byMonth.isEmpty() &&
                rule.byWeekNo.isEmpty() && rule.byYearDay.isEmpty() && rule.byMonthDay.isEmpty() && rule.bySetPos.isEmpty() &&
                rule.weekStart == DayOfWeek.MONDAY
            return when {
                !simple -> ADVANCED
                rule.freq == Freq.DAILY -> if (rule.byDay.isEmpty()) DAILY else ADVANCED
                rule.freq != Freq.WEEKLY || rule.byDay.isEmpty() || !rule.plainDays -> ADVANCED
                else -> when (rule.byDay.mapTo(HashSet()) { it.day }) {
                    WEEKDAY_SET -> WEEKDAYS
                    WEEKEND_SET -> WEEKEND
                    else -> CUSTOM
                }
            }
        }

        /** The weekdays of a [CUSTOM] (or weekday preset) rule. zh-CN: [CUSTOM] (或星期预设) 规则的星期. */
        fun daysOf(repeat: String?): Set<DayOfWeek> = when (of(repeat)) {
            DAILY -> DayOfWeek.entries.toSet()
            WEEKDAYS, WEEKEND, CUSTOM -> RecurrenceRule.parse(repeat!!).byDay.mapTo(LinkedHashSet()) { it.day }
            else -> emptySet()
        }

        /** The rule of a preset; [CUSTOM] and [ADVANCED] have none of their own. zh-CN: 预设的规则; [CUSTOM] 与 [ADVANCED] 没有固定规则. */
        fun ruleOf(preset: RepeatPreset): String? = when (preset) {
            ONCE -> null
            DAILY -> RULE_DAILY
            WEEKDAYS -> ruleOf(WEEKDAY_SET)
            WEEKEND -> ruleOf(WEEKEND_SET)
            CUSTOM, ADVANCED -> throw IllegalArgumentException("$preset has no rule of its own")
        }

        /** The canonical rule of chosen weekdays: all seven are every day. zh-CN: 所选星期的规范规则: 七天全选即每天. */
        fun ruleOf(days: Set<DayOfWeek>): String {
            require(days.isNotEmpty()) { "no weekday" }
            if (days.size == 7) return RULE_DAILY
            return RecurrenceRule.fromString("FREQ=WEEKLY;BYDAY=" + days.sorted().joinToString(",", transform = RecurrenceRule::codeOf))
        }

        private const val RULE_DAILY = "FREQ=DAILY"
    }
}

/**
 * The fields of the advanced rule editor (roadmap P3.1): frequency, interval, weekdays (entries with an ordinal such as
 * `2TU` stay as they are; the weekday chips toggle only plain days), month days, set positions and the end (never, after
 * a count, until a date). The parts the editor has no field for (`BYMONTH`, `BYWEEKNO`, `BYYEARDAY`, `WKST`, an `UNTIL`
 * with a time) stay as in [base] and remain editable as RRULE text.
 * zh-CN: 高级规则编辑器的字段 (路线图 P3.1): 频率, 间隔, 星期 (带序号的项如 `2TU` 原样保留; 星期芯片只切换不带序号的星期), 每月日期, 位置筛选, 结束方式
 * (永不, 次数, 截至日期). 编辑器没有字段的部分 (`BYMONTH`, `BYWEEKNO`, `BYYEARDAY`, `WKST`, 带时刻的 `UNTIL`) 保持 [base] 中的值, 仍可通过 RRULE 文本编辑.
 */
internal data class RuleForm(
    val base: RecurrenceRule,
    val freq: Freq = base.freq,
    val interval: String = base.interval.toString(),
    val days: List<WeekdayNum> = base.byDay,
    val monthDays: String = base.byMonthDay.joinToString(", "),
    val setPos: String = base.bySetPos.joinToString(", "),
    val end: End = when {
        base.count != null -> End.COUNT
        base.until != null -> End.UNTIL
        else -> End.NEVER
    },
    val count: String = base.count?.toString().orEmpty(),
    val until: RecurrenceRule.Until? = base.until,
) {

    enum class End { NEVER, COUNT, UNTIL }

    /** A field the user has to fix, or [RULE] with the rule engine's message. zh-CN: 需要修正的字段, 或 [RULE] 并附规则引擎的消息. */
    enum class Problem { INTERVAL, MONTH_DAYS, SET_POS, COUNT, UNTIL, RULE }

    sealed class Result {
        data class Valid(val rule: String) : Result()
        data class Invalid(val problem: Problem, val detail: String? = null) : Result()
    }

    /** Whether the plain weekday [day] is chosen. zh-CN: 是否选择了不带序号的星期 [day]. */
    fun hasDay(day: DayOfWeek): Boolean = days.any { it.ordinal == null && it.day == day }

    fun toggleDay(day: DayOfWeek): RuleForm =
        copy(days = if (hasDay(day)) days.filterNot { it.ordinal == null && it.day == day } else days + WeekdayNum(null, day))

    /** The canonical RRULE text of the fields, or the first problem. zh-CN: 字段对应的规范 RRULE 文本, 或第一个问题. */
    fun build(): Result {
        val interval = interval.trim().removePrefix("+").toIntOrNull()?.takeIf { it >= 1 } ?: return Result.Invalid(Problem.INTERVAL)
        val monthDays = numbers(monthDays) ?: return Result.Invalid(Problem.MONTH_DAYS)
        val setPos = numbers(setPos) ?: return Result.Invalid(Problem.SET_POS)
        val count = when (end) {
            End.COUNT -> count.trim().removePrefix("+").toIntOrNull()?.takeIf { it >= 1 } ?: return Result.Invalid(Problem.COUNT)
            else -> null
        }
        val until = when (end) {
            End.UNTIL -> until ?: return Result.Invalid(Problem.UNTIL)
            else -> null
        }
        val rule = base.copy(
            freq = freq, interval = interval, count = count, until = until,
            byMonthDay = monthDays, byDay = days, bySetPos = setPos,
        )
        return try {
            Result.Valid(RecurrenceRule.fromString(rule.text))
        } catch (e: AlarmException) {
            Result.Invalid(Problem.RULE, e.detail)
        }
    }

    companion object {

        /** The form of [repeat]; a one-shot alarm starts from every day. zh-CN: [repeat] 的表单; 一次性闹钟从每天开始. */
        fun of(repeat: String?): RuleForm = RuleForm(repeat?.let { runCatching { RecurrenceRule.parse(it) }.getOrNull() } ?: RecurrenceRule(Freq.DAILY))

        /** Comma or space separated integers; blank is none, anything else not a number is null. zh-CN: 逗号或空格分隔的整数; 空白为无, 非数字为 null. */
        private fun numbers(text: String): List<Int>? {
            val items = text.split(',', ' ').map(String::trim).filter(String::isNotEmpty)
            return items.map { it.removePrefix("+").toIntOrNull() ?: return null }
        }
    }
}
