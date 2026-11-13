package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayFilter
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayJudge
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmSpec
import org.autojs.plugin.alarm.api.OccurrenceSnapshot
import org.dmfs.rfc5545.DateTime
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import org.dmfs.rfc5545.recur.RecurrenceRule as LibRecurRule

/**
 * The rings of an alarm (roadmap P2.2, T10, appendix A sections 4.2 and 4.3): an `at` alarm rings once at its instant,
 * a `time` alarm without `repeat` once on its `date`, a repeating alarm on every date of its rule from `date` (the
 * DTSTART; the start itself only when it matches the rule) at `time`, in the zone of [TimeZonePolicy]. `COUNT` counts
 * the dates of the rule, `exclude` (EXDATE) then removes dates without giving them back, `UNTIL` ends the series
 * (a date counts as a whole day, a UTC instant is compared with the ring instant). Dates that do not exist (the 31st
 * in a 30-day month, February 29 outside leap years) are skipped as RFC 5545 says.
 *
 * lib-recur computes the dates, except for rules with `BYWEEKNO` (week 1 is the week starting on `WKST` that holds
 * January 4; a year has 52 or 53 weeks, a week number the year does not have gives no date) and yearly rules with
 * `BYYEARDAY` or `BYSETPOS`, which the plugin expands itself (T52): lib-recur 0.17.1 maps week 53 onto week 52 in
 * 52-week years, mixes up weeks around the new year, drops the dates before the start before it applies `BYSETPOS`
 * in a yearly rule with `BYMONTH` or `BYYEARDAY`, and fails on `BYYEARDAY` with `BYDAY`. The rule is also given to
 * lib-recur without `UNTIL`, which it refuses with a floating start. Each date lib-recur gives is checked against the
 * rule once more before it is used.
 *
 * A rule that can never give a date from its start fails with `INVALID_RULE` (for example `FREQ=YEARLY;BYMONTHDAY=31`
 * from a date in February: a yearly rule with `BYMONTHDAY` alone stays in the month of the start).
 *
 * From 1.2.0 the holiday filter of the alarm (roadmap P8.3, [HolidayFilter]) decides which of these dates ring: [rings]
 * and the methods built on it take a [HolidayJudge]; a `time` alarm in `workdays` mode without `repeat` uses every day.
 *
 * zh-CN: 闹钟的各次响铃 (路线图 P2.2, T10, 附录 A 第 4.2, 4.3 节): `at` 闹钟在该时刻响一次, 无 `repeat` 的 `time` 闹钟在 `date` 当天响一次,
 * 重复闹钟从 `date` (DTSTART; 起点本身只在符合规则时才算) 起按规则的每个日期在 `time` 响, 时区按 [TimeZonePolicy]. `COUNT` 计规则的日期, `exclude`
 * (EXDATE) 随后去掉日期且不补回, `UNTIL` 结束序列 (日期按整天算, UTC 时刻与响铃时刻比较). 不存在的日期 (30 天月份的 31 日, 非闰年的 2 月 29 日) 按
 * RFC 5545 跳过. 自 1.2.0 起由闹钟的节假日过滤 (路线图 P8.3, [HolidayFilter]) 决定这些日期中哪些响铃: [rings] 及基于它的方法接收 [HolidayJudge];
 * 没有 `repeat` 的 `workdays` 模式 `time` 闹钟按每天计算. 日期由 lib-recur 计算, 带 `BYWEEKNO` 的规则 (第 1 周是从 `WKST` 起算且含 1 月 4 日的那一周; 一年 52 或 53 周, 该年没有的周号不产生日期)
 * 与带 `BYYEARDAY` 或 `BYSETPOS` 的按年规则除外, 由插件自行展开 (T52): lib-recur 0.17.1 在 52 周的年份把第 53 周当作第 52 周, 弄混跨年前后的周,
 * 在带 `BYMONTH` 或 `BYYEARDAY` 的按年规则中先去掉起点之前的日期再应用 `BYSETPOS`, 并且在 `BYYEARDAY` 与 `BYDAY` 同用时出错. 交给 lib-recur 的规则去掉 `UNTIL`
 * (浮动起点时它拒绝 UTC 的 `UNTIL`). lib-recur 给出的每个日期使用前再按规则核对一次. 从起点起永远不产生日期的规则报 `INVALID_RULE` (例如从 2 月某日起的
 * `FREQ=YEARLY;BYMONTHDAY=31`: 只有 `BYMONTHDAY` 的按年规则停留在起点所在月份).
 */
object NextOccurrence {

    /** At most this many rule dates are looked at for one question. zh-CN: 每次查询至多检查这么多个规则日期. */
    const val MAX_SCANNED = 200_000

    /**
     * Years without a date after which a `BYWEEKNO` rule is taken as finished (week patterns repeat every 400 years).
     * zh-CN: 连续这么多年没有日期时, 视为 `BYWEEKNO` 规则已结束 (周的排列每 400 年重复一次).
     */
    const val MAX_EMPTY_YEARS = 400

    private const val LAST_YEAR = 9999

    /** The most holidays [holidaySkips] counts. zh-CN: [holidaySkips] 至多计数的节假日个数. */
    const val MAX_COUNTED_SKIPS = 99

    /** One ring: the wall-clock moment and its instant. zh-CN: 一次响铃: 墙上时间与对应时刻. */
    data class Occurrence(val local: LocalDateTime, val at: Long)

    /**
     * The date a `time` alarm without `date` gets (appendix A section 4.2, T48): today for a repeating alarm (a
     * `workdays` alarm included), the next day the time is reached after [now] for a one-shot alarm, holidays not
     * considered (T183).
     * zh-CN: 没有 `date` 的 `time` 闹钟得到的日期 (附录 A 第 4.2 节, T48): 重复闹钟 (含 `workdays` 闹钟) 取今天, 一次性闹钟取 [now] 之后第一个到达该时刻的
     * 日子, 不考虑节假日 (T183).
     */
    fun implicitDate(spec: AlarmSpec, now: Long, zone: ZoneId): LocalDate {
        val time = spec.time ?: throw IllegalArgumentException("an at alarm has no date")
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return when {
            HolidayFilter.repeats(spec) -> today
            TimeZonePolicy.millisOf(today.atTime(time.hour, time.minute, time.second), zone) > now -> today
            else -> today.plusDays(1)
        }
    }

    /**
     * Every date of [spec] in time order, the holiday filter not applied (see [rings]). [after] only lets the
     * computation skip ahead: rings at or before it may still be in the sequence. The `date` of a `time` alarm must be
     * filled in (see [implicitDate]).
     * zh-CN: [spec] 按时间顺序的每个日期, 未应用节假日过滤 (见 [rings]). [after] 只用于跳过前面的计算, 序列里仍可能有不晚于它的响铃. `time` 闹钟的 `date`
     * 必须已补上 (见 [implicitDate]).
     */
    fun occurrences(spec: AlarmSpec, deviceZone: ZoneId, after: Long? = null): Sequence<Occurrence> {
        val zone = TimeZonePolicy.zoneOf(spec, deviceZone)
        spec.at?.let { return sequenceOf(Occurrence(Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime(), it)) }
        val time = spec.time ?: return emptySequence()
        val clock = LocalTime.of(time.hour, time.minute, time.second)
        val date = LocalDate.parse(spec.date ?: throw IllegalArgumentException("the date of the alarm is not filled in"))
        val excluded = spec.exclude.orEmpty().mapTo(HashSet(), LocalDate::parse)
        val repeat = HolidayFilter.baseRule(spec)
        if (repeat == null) {
            if (date in excluded) return emptySequence()
            val local = date.atTime(clock)
            return sequenceOf(Occurrence(local, TimeZonePolicy.millisOf(local, zone)))
        }
        val rule = RecurrenceRule.parse(repeat)
        // One day of margin on each side of the zone offset is enough to never skip a ring at or after [after].
        // zh-CN: 时区偏移两侧各留一天余量, 不会跳过不早于 [after] 的响铃.
        val from = after?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate().minusDays(1) }
        val until = rule.until
        return dates(rule, date.atTime(clock), from)
            .filter { it !in excluded }
            .map { day -> day.atTime(clock).let { Occurrence(it, TimeZonePolicy.millisOf(it, zone)) } }
            .takeWhile { until == null || until.allows(it.local, Instant.ofEpochMilli(it.at)) }
    }

    /**
     * The instances of [spec] in time order with what the holiday filter makes of them ([HolidayFilter.Verdict.RING] or
     * [HolidayFilter.Verdict.HOLIDAY]); dates that are not instances are left out. Without a filter every date rings.
     * zh-CN: [spec] 按时间顺序的各次及节假日过滤的结论 ([HolidayFilter.Verdict.RING] 或 [HolidayFilter.Verdict.HOLIDAY]); 不算一次的日期不列出.
     * 没有过滤时每个日期都响.
     */
    fun instances(spec: AlarmSpec, deviceZone: ZoneId, judge: HolidayJudge, after: Long? = null): Sequence<Pair<Occurrence, HolidayFilter.Verdict>> {
        val filter = spec.holidays ?: return occurrences(spec, deviceZone, after).map { it to HolidayFilter.Verdict.RING }
        return occurrences(spec, deviceZone, after)
            .map { it to HolidayFilter.verdict(filter, it.local.toLocalDate(), judge) }
            .filter { it.second != HolidayFilter.Verdict.NONE }
    }

    /**
     * The rings of [spec] in time order: its dates the holiday filter keeps (roadmap P8.3). [after] as for [occurrences].
     * zh-CN: [spec] 按时间顺序的响铃: 节假日过滤保留的日期 (路线图 P8.3). [after] 同 [occurrences].
     */
    fun rings(spec: AlarmSpec, deviceZone: ZoneId, judge: HolidayJudge, after: Long? = null): Sequence<Occurrence> =
        if (spec.holidays == null) occurrences(spec, deviceZone, after)
        else instances(spec, deviceZone, judge, after).filter { it.second == HolidayFilter.Verdict.RING }.map { it.first }

    /**
     * The first ring after [after] that is not the skipped one ([skipAt], from `skipNext`), or null when the alarm has
     * no more rings.
     * zh-CN: [after] 之后第一个不是被跳过的那次 ([skipAt], 来自 `skipNext`) 的响铃; 没有更多响铃时为 null.
     */
    fun next(spec: AlarmSpec, after: Long, deviceZone: ZoneId, skipAt: Long? = null, judge: HolidayJudge = HolidayJudge.NONE): Occurrence? =
        rings(spec, deviceZone, judge, after).firstOrNull { it.at > after && it.at != skipAt }

    /**
     * The first instance after [after] and before [before] (no bound when null) that a holiday takes away (roadmap P8.3,
     * T181), or null; always null without a holiday filter.
     * zh-CN: [after] 之后, [before] 之前 (null 为不限) 第一个被节假日拿掉的那一次 (路线图 P8.3, T181), 没有时为 null; 没有节假日过滤时总为 null.
     */
    fun nextHolidaySkip(spec: AlarmSpec, after: Long, before: Long?, deviceZone: ZoneId, judge: HolidayJudge): Long? {
        if (spec.holidays == null) return null
        return instances(spec, deviceZone, judge, after)
            .filter { it.first.at > after }
            .takeWhile { before == null || it.first.at < before }
            .firstOrNull { it.second == HolidayFilter.Verdict.HOLIDAY }
            ?.first?.at
    }

    /**
     * How many instances of [spec] after [after] and before [before] a holiday takes away, at most [limit] (roadmap P8.6):
     * the "N holidays skipped" of the home list and the edit page. Zero without a holiday filter; the date of a `time`
     * alarm without one is filled in as in [preview].
     * zh-CN: [spec] 在 [after] 之后, [before] 之前被节假日拿掉的次数, 至多 [limit] (路线图 P8.6): 首页列表与编辑页的 "已跳过 N 个节假日". 没有节假日过滤时
     * 为 0; 没有 `date` 的 `time` 闹钟按 [preview] 的方式补上日期.
     */
    fun holidaySkips(spec: AlarmSpec, after: Long, before: Long, deviceZone: ZoneId, judge: HolidayJudge, limit: Int = MAX_COUNTED_SKIPS): Int {
        if (spec.holidays == null || before <= after) return 0
        return instances(withDate(spec, after, deviceZone), deviceZone, judge, after)
            .filter { it.first.at > after }
            .takeWhile { it.first.at < before }
            .filter { it.second == HolidayFilter.Verdict.HOLIDAY }
            .take(limit)
            .count()
    }

    /**
     * The next [count] ring instants of an alarm that is not stored (`alarms.preview`, at most 366), the date filled
     * in as for a new alarm, the holiday filter applied.
     * zh-CN: 未保存的闹钟接下来 [count] 次响铃的时刻 (`alarms.preview`, 至多 366), 日期按新建闹钟的方式补上, 已应用节假日过滤.
     */
    fun preview(spec: AlarmSpec, count: Int, now: Long, deviceZone: ZoneId, judge: HolidayJudge = HolidayJudge.NONE): List<Long> {
        return rings(withDate(spec, now, deviceZone), deviceZone, judge, now)
            .filter { it.at > now }
            .take(count.coerceIn(0, AlarmLimits.MAX_PREVIEW_COUNT))
            .map { it.at }
            .toList()
    }

    /** [spec] with the date of a `time` alarm filled in as for a new alarm ([implicitDate]). zh-CN: 按新建闹钟的方式补上日期的 [spec]. */
    private fun withDate(spec: AlarmSpec, now: Long, deviceZone: ZoneId): AlarmSpec =
        if (spec.time != null && spec.date == null) spec.copy(date = implicitDate(spec, now, TimeZonePolicy.zoneOf(spec, deviceZone)).toString()) else spec

    /**
     * The next [count] rings of the enabled alarms among [records] after [now] (`alarms.upcoming`, at most 100), in
     * time order (alarms in list order at the same instant). A skipped ring and the instances the holiday filter takes
     * away are left out, a snoozed alarm rings again at the end of its snooze; an alarm whose rule fails is left out.
     * zh-CN: [records] 中已启用的闹钟在 [now] 之后接下来的 [count] 次响铃 (`alarms.upcoming`, 至多 100), 按时间排序 (同一时刻按列表顺序).
     * 被跳过的那次与被节假日过滤拿掉的各次不列出, 贪睡中的闹钟在贪睡结束时再响; 规则计算失败的闹钟不列出.
     */
    fun upcoming(records: List<AlarmRecord>, count: Int, now: Long, deviceZone: ZoneId, judge: HolidayJudge = HolidayJudge.NONE): List<OccurrenceSnapshot> {
        val limit = count.coerceIn(0, AlarmLimits.MAX_UPCOMING_COUNT)
        if (limit == 0) return emptyList()
        val all = ArrayList<Pair<Int, OccurrenceSnapshot>>()
        records.forEachIndexed { index, record ->
            if (!record.enabled) return@forEachIndexed
            val label = record.spec.label
            record.state.snoozeUntil?.takeIf { it > now }?.let { all.add(index to OccurrenceSnapshot(record.id, label, it)) }
            try {
                rings(record.spec, deviceZone, judge, now)
                    .filter { it.at > now && it.at != record.state.skipAt }
                    .take(limit)
                    .forEach { all.add(index to OccurrenceSnapshot(record.id, label, it.at)) }
            } catch (_: AlarmException) {
                // A stored rule that cannot be computed: the scheduler reports it, upcoming leaves it out.
                // zh-CN: 已保存但无法计算的规则: 由调度器报告, upcoming 不列出.
            }
        }
        return all.sortedWith(compareBy({ it.second.at }, { it.first })).take(limit).map { it.second }
    }

    /**
     * The dates of [rule] from [start] (the DTSTART at the ring time), `COUNT` applied, `UNTIL` not. [from] lets
     * the computation skip ahead when the rule has no `COUNT`.
     * zh-CN: [rule] 从 [start] (响铃时刻的 DTSTART) 起的日期, 已计入 `COUNT`, 未计入 `UNTIL`. 规则没有 `COUNT` 时, [from] 允许跳过前面的计算.
     */
    internal fun dates(rule: RecurrenceRule, start: LocalDateTime, from: LocalDate? = null): Sequence<LocalDate> {
        val day = start.toLocalDate()
        // Skipping ahead is only safe without COUNT, which counts from the start. zh-CN: 只有没有 COUNT 时才能跳过, COUNT 从起点开始计数.
        val skip = from?.takeIf { rule.count == null && it.isAfter(day) }
        val dates = when {
            rule.byWeekNo.isNotEmpty() -> yearlyDates(rule, day, skip) { weekNumberYear(rule, it, day.dayOfWeek) }
            rule.freq == RecurrenceRule.Freq.YEARLY && (rule.byYearDay.isNotEmpty() || rule.bySetPos.isNotEmpty()) ->
                yearlyDates(rule, day, skip) { calendarYear(rule, it, day) }
            else -> libRecurDates(rule, start, skip)
        }
        return rule.count?.let { dates.take(it) } ?: dates
    }

    private fun libRecurDates(rule: RecurrenceRule, start: LocalDateTime, from: LocalDate?): Sequence<LocalDate> = sequence {
        val iterator = try {
            LibRecurRule(rule.textWithoutUntil).iterator(DateTime(start.year, start.monthValue - 1, start.dayOfMonth, start.hour, start.minute, start.second))
        } catch (e: IllegalArgumentException) {
            // lib-recur looks for the first date at once and gives up after many that do not pass its filters.
            // zh-CN: lib-recur 立即查找第一个日期, 在大量日期都不满足过滤条件后放弃.
            throw never(rule, start.toLocalDate())
        } catch (e: Exception) {
            throw cannotCompute(rule, e)
        }
        if (from != null) iterator.fastForward(DateTime(from.year, from.monthValue - 1, from.dayOfMonth, 0, 0, 0))
        var produced = false
        var scanned = 0
        while (scanned < MAX_SCANNED) {
            val next = try {
                if (!iterator.hasNext()) break
                iterator.nextDateTime()
            } catch (e: IllegalArgumentException) {
                // lib-recur gives up after many dates that do not pass its filters.
                // zh-CN: lib-recur 在大量日期都不满足过滤条件后放弃.
                if (!produced) throw never(rule, start.toLocalDate())
                break
            } catch (e: RuntimeException) {
                throw cannotCompute(rule, e)
            }
            scanned++
            if (next.year > LAST_YEAR) break
            val date = LocalDate.of(next.year, next.month + 1, next.dayOfMonth)
            if (!matches(rule, date)) continue
            produced = true
            yield(date)
        }
    }

    /**
     * The dates of a yearly rule the plugin expands itself, one period at a time: [year] gives the dates of one year
     * (`BYSETPOS` applied to the whole year), dates before [start] are dropped afterwards. [from] skips whole periods.
     * The calendar repeats every 400 years, so [MAX_EMPTY_YEARS] periods without a date mean there is no date left.
     * zh-CN: 由插件自行展开的按年规则的日期, 逐个周期计算: [year] 给出一年的日期 (`BYSETPOS` 作用于整年), 随后去掉早于 [start] 的日期. [from] 跳过整个周期.
     * 日历每 400 年重复一次, 因此连续 [MAX_EMPTY_YEARS] 个周期没有日期即表示不再有日期.
     */
    private fun yearlyDates(rule: RecurrenceRule, start: LocalDate, from: LocalDate?, year: (Int) -> List<LocalDate>): Sequence<LocalDate> = sequence {
        // A week-numbering year starts up to 3 days before its calendar year, so the period before [from] is kept.
        // zh-CN: 周编号年最多比日历年早 3 天开始, 因此保留 [from] 之前的那个周期.
        val skipped = from?.let { maxOf(0, (it.year - 1 - start.year) / rule.interval) } ?: 0
        var current = start.year + skipped * rule.interval
        var empty = 0
        var produced = false
        while (current <= LAST_YEAR) {
            val dates = year(current).filter { !it.isBefore(start) }
            if (dates.isEmpty()) {
                if (++empty >= MAX_EMPTY_YEARS) {
                    if (!produced) throw never(rule, start)
                    break
                }
            } else {
                empty = 0
                produced = true
                yieldAll(dates)
            }
            current += rule.interval
        }
    }

    /**
     * The dates a `FREQ=YEARLY` rule without `BYWEEKNO` gives in the calendar year [year], `BYSETPOS` applied
     * (RFC 5545 section 3.3.10: `BYYEARDAY`, else `BYMONTHDAY`, else `BYDAY` expands the year or the months of
     * `BYMONTH`; the later parts then limit). What the rule leaves out comes from [start]: the month for `BYMONTHDAY`
     * without `BYMONTH`, the month and the day without any day part. An ordinal `BYDAY` counts within the month when
     * the rule has `BYMONTH`, else within the year. lib-recur 0.17.1 drops the dates before the start before it
     * applies `BYSETPOS` and fails on `BYYEARDAY` with `BYDAY`, so these rules do not go through it.
     * zh-CN: 不带 `BYWEEKNO` 的 `FREQ=YEARLY` 规则在日历年 [year] 给出的日期, 已应用 `BYSETPOS` (RFC 5545 第 3.3.10 节: 由 `BYYEARDAY`, 否则由
     * `BYMONTHDAY`, 否则由 `BYDAY` 展开整年或 `BYMONTH` 的各月, 其余部分再做限制). 规则省略的部分取自 [start]: 有 `BYMONTHDAY` 而无 `BYMONTH`
     * 时取其月份, 没有任何日期部分时取其月份与日. 带序号的 `BYDAY` 在规则有 `BYMONTH` 时按月计数, 否则按年计数. lib-recur 0.17.1 先去掉起点之前的日期再应用
     * `BYSETPOS`, 并且在 `BYYEARDAY` 与 `BYDAY` 同用时出错, 因此这些规则不交给它.
     */
    internal fun calendarYear(rule: RecurrenceRule, year: Int, start: LocalDate): List<LocalDate> {
        val months = rule.byMonth.ifEmpty { listOf(start.monthValue) }.map { YearMonth.of(year, it) }
        val inMonth = rule.byMonth.isNotEmpty()
        val candidates: List<LocalDate> = when {
            rule.byYearDay.isNotEmpty() -> {
                val length = Year.of(year).length()
                rule.byYearDay.map { if (it > 0) it else length + 1 + it }.filter { it in 1..length }.map { LocalDate.ofYearDay(year, it) }
            }
            rule.byMonthDay.isNotEmpty() -> months.flatMap { month ->
                rule.byMonthDay.map { if (it > 0) it else month.lengthOfMonth() + 1 + it }.filter { it in 1..month.lengthOfMonth() }.map(month::atDay)
            }
            rule.byDay.isNotEmpty() -> if (inMonth) {
                months.flatMap { weekdays(rule.byDay, it.atDay(1), it.atEndOfMonth()) }
            } else {
                weekdays(rule.byDay, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
            }
            else -> months.filter { start.dayOfMonth <= it.lengthOfMonth() }.map { it.atDay(start.dayOfMonth) }
        }
        val days = candidates.filter { date ->
            matches(rule, date) && (rule.byDay.isEmpty() || rule.byDay.any { it.day == date.dayOfWeek && ordinalAllows(it.ordinal, date, inMonth) })
        }.distinct().sorted()
        return setPositions(days, rule.bySetPos)
    }

    /**
     * The days from [first] to [last] that [byDay] names: every such weekday, or only the n-th from the start (the
     * n-th from the end for a negative n).
     * zh-CN: [first] 到 [last] 之间 [byDay] 指定的日子: 该星期的每一天, 或只取从头数第 n 个 (n 为负时从尾数).
     */
    private fun weekdays(byDay: List<RecurrenceRule.WeekdayNum>, first: LocalDate, last: LocalDate): List<LocalDate> = byDay.flatMap { (ordinal, day) ->
        val head = first.with(TemporalAdjusters.nextOrSame(day))
        val tail = last.with(TemporalAdjusters.previousOrSame(day))
        when {
            ordinal == null -> generateSequence(head) { it.plusWeeks(1) }.takeWhile { !it.isAfter(last) }.toList()
            ordinal > 0 -> listOfNotNull(head.plusWeeks(ordinal - 1L).takeIf { !it.isAfter(last) })
            else -> listOfNotNull(tail.minusWeeks(-ordinal - 1L).takeIf { !it.isBefore(first) })
        }
    }

    /** Whether [date] is the [ordinal]-th of its weekday in its month ([inMonth]) or year; any date without one. */
    private fun ordinalAllows(ordinal: Int?, date: LocalDate, inMonth: Boolean): Boolean {
        if (ordinal == null) return true
        val first = if (inMonth) date.withDayOfMonth(1) else date.withDayOfYear(1)
        val last = if (inMonth) date.withDayOfMonth(date.lengthOfMonth()) else date.withDayOfYear(date.lengthOfYear())
        return date in weekdays(listOf(RecurrenceRule.WeekdayNum(ordinal, date.dayOfWeek)), first, last)
    }

    /**
     * The dates a `BYWEEKNO` rule gives for the week-numbering year [year], `BYSETPOS` applied. Without `BYDAY`,
     * `BYMONTHDAY` and `BYYEARDAY` the weekday is the one of the start. The other parts are checked against each date's
     * own calendar date, so a week that reaches into the next or the previous calendar year keeps those days.
     * zh-CN: `BYWEEKNO` 规则在周编号年 [year] 给出的日期, 已应用 `BYSETPOS`. 没有 `BYDAY`, `BYMONTHDAY` 与 `BYYEARDAY` 时, 星期取起点的星期.
     * 其他部分按各日期自身的日历日期核对, 因此伸进上一个或下一个日历年的那一周保留这些日子.
     */
    internal fun weekNumberYear(rule: RecurrenceRule, year: Int, startDay: DayOfWeek): List<LocalDate> {
        val first = firstWeekStart(year, rule.weekStart)
        val weeks = (ChronoUnit.DAYS.between(first, firstWeekStart(year + 1, rule.weekStart)) / 7).toInt()
        val implied = rule.byDay.isEmpty() && rule.byMonthDay.isEmpty() && rule.byYearDay.isEmpty()
        val days = rule.byWeekNo
            .map { if (it > 0) it else weeks + 1 + it }
            .filter { it in 1..weeks }
            .distinct()
            .flatMap { week -> (0L until 7L).map { first.plusDays(7L * (week - 1) + it) } }
            .filter { date -> (!implied || date.dayOfWeek == startDay) && matches(rule, date) }
            .sorted()
        return setPositions(days, rule.bySetPos)
    }

    /** The first day of week 1 of [year]: the [weekStart] on or before January 4. zh-CN: [year] 第 1 周的第一天: 1 月 4 日当天或之前的 [weekStart]. */
    internal fun firstWeekStart(year: Int, weekStart: DayOfWeek): LocalDate =
        LocalDate.of(year, 1, 4).with(TemporalAdjusters.previousOrSame(weekStart))

    private fun <T> setPositions(sorted: List<T>, positions: List<Int>): List<T> {
        if (positions.isEmpty()) return sorted
        return positions
            .mapNotNull { p -> (if (p > 0) p - 1 else sorted.size + p).takeIf { it in sorted.indices } }
            .distinct()
            .sorted()
            .map { sorted[it] }
    }

    /**
     * Whether [date] passes the `BYMONTH`, `BYMONTHDAY`, `BYYEARDAY` and plain `BYDAY` weekday parts of [rule].
     * zh-CN: [date] 是否满足 [rule] 的 `BYMONTH`, `BYMONTHDAY`, `BYYEARDAY` 与 `BYDAY` 的星期部分.
     */
    internal fun matches(rule: RecurrenceRule, date: LocalDate): Boolean {
        if (rule.byMonth.isNotEmpty() && date.monthValue !in rule.byMonth) return false
        if (rule.byMonthDay.isNotEmpty()) {
            val length = date.lengthOfMonth()
            if (rule.byMonthDay.none { (if (it > 0) it else length + 1 + it) == date.dayOfMonth }) return false
        }
        if (rule.byYearDay.isNotEmpty()) {
            val length = date.lengthOfYear()
            if (rule.byYearDay.none { (if (it > 0) it else length + 1 + it) == date.dayOfYear }) return false
        }
        if (rule.byDay.isNotEmpty() && rule.byDay.none { it.day == date.dayOfWeek }) return false
        return true
    }

    private fun never(rule: RecurrenceRule, start: LocalDate): AlarmException =
        AlarmException.invalidRule("repeat: ${rule.text} gives no date from $start")

    private fun cannotCompute(rule: RecurrenceRule, e: Exception): AlarmException =
        AlarmException.invalidRule("repeat: ${rule.text} cannot be computed (${e.javaClass.simpleName}${e.message?.let { ": $it" } ?: ""})")
}
