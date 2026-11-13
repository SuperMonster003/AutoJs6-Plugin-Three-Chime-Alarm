package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RecurrenceRule
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmHolidayFilter
import org.autojs.plugin.alarm.api.AlarmSpec
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The holiday filter of the rule engine (roadmap P8.3, T179 - T184): which dates of a repeating alarm ring.
 *
 * - `skip`: every date of the rule except holidays.
 * - `only`: the dates of the rule that are holidays.
 * - `workdays`: every workday (Q46 (a)): a day outside the region's weekend (by year and subdivision, T165) that is no
 *   holiday, or a makeup workday. `repeat` is omitted (every day) or `FREQ=DAILY` with interval 1 (T180).
 *
 * The date judged is the local date of the ring in the zone of the alarm. An estimated year (T162) is judged like any
 * other (Q47 (a)). A year without data, or a region the data does not know, never silences an alarm (P8.2): `skip`
 * and `only` then ring by the rule alone, `workdays` by the weekend of the region (Saturday and Sunday when even the
 * region is unknown).
 *
 * Only an instance a holiday takes away gives a `skip` event with reason `holiday` ([Verdict.HOLIDAY]): a holiday in
 * `skip` mode, a holiday on a regular workday in `workdays` mode. The days `only` leaves out and the weekends of
 * `workdays` are simply not instances of the alarm (T181, Q50).
 *
 * zh-CN: 规则引擎的节假日过滤 (路线图 P8.3, T179 - T184): 重复闹钟的哪些日期响铃. `skip`: 规则的日期中除去节假日. `only`: 规则的日期中属于节假日的.
 * `workdays`: 每个工作日 (Q46 (a)): 不在地区周末 (按年份与下级地区, T165) 且不是节假日, 或是补班日; `repeat` 省略 (每天) 或为间隔 1 的 `FREQ=DAILY`
 * (T180). 判定的是响铃时刻在闹钟时区中的本地日期. 推算年份 (T162) 照常判定 (Q47 (a)). 没有数据的年份或数据不认识的地区不会让闹钟不响 (P8.2):
 * `skip` 与 `only` 只按规则响, `workdays` 按该地区的周末 (连地区也不认识时按周六日). 只有被节假日拿掉的那一次发 `skip` 事件 (原因 `holiday`,
 * [Verdict.HOLIDAY]): `skip` 模式的节假日, `workdays` 模式中落在平常工作日的节假日. `only` 不响的日子与 `workdays` 的周末根本不算闹钟的一次 (T181, Q50).
 */
object HolidayFilter {

    /** What the filter makes of one date of the rule. zh-CN: 过滤对规则中某个日期的结论. */
    enum class Verdict {
        /** The alarm rings. zh-CN: 响铃. */
        RING,

        /** A holiday takes the instance away: a `skip` event with reason `holiday`. zh-CN: 节假日拿掉这一次: 发原因为 `holiday` 的 `skip`. */
        HOLIDAY,

        /** Not an instance of the alarm. zh-CN: 不是闹钟的一次. */
        NONE,
    }

    /** The weekend when nothing is known about the region. zh-CN: 对地区一无所知时的周末. */
    private val DEFAULT_WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    /**
     * True when [spec] rings on more than one date: it has `repeat`, or it is a `time` alarm in `workdays` mode (`repeat`
     * omitted means every workday).
     * zh-CN: [spec] 在不止一个日期响铃时为真: 带 `repeat`, 或是 `workdays` 模式的 `time` 闹钟 (省略 `repeat` 即每个工作日).
     */
    fun repeats(spec: AlarmSpec): Boolean = spec.repeat != null || (spec.time != null && spec.holidays?.mode == AlarmContract.HOLIDAY_MODE_WORKDAYS)

    /** The rule the dates come from: `repeat`, or every day for [repeats] without it. zh-CN: 日期所依据的规则. */
    fun baseRule(spec: AlarmSpec): String? = spec.repeat ?: DAILY.takeIf { repeats(spec) }

    /**
     * True when [rule] may go with `workdays` (T180): `FREQ=DAILY`, interval 1, no `COUNT` and no `BY*` part; `UNTIL`
     * may end it.
     * zh-CN: [rule] 可与 `workdays` 同用时为真 (T180): `FREQ=DAILY`, 间隔 1, 没有 `COUNT` 与任何 `BY*` 部分; 可以用 `UNTIL` 结束.
     */
    fun allowsWorkdays(rule: RecurrenceRule): Boolean = rule.freq == RecurrenceRule.Freq.DAILY && rule.interval == 1 && rule.count == null &&
        rule.byMonth.isEmpty() && rule.byWeekNo.isEmpty() && rule.byYearDay.isEmpty() && rule.byMonthDay.isEmpty() &&
        rule.byDay.isEmpty() && rule.bySetPos.isEmpty()

    /** What [filter] makes of [date] (see the class comment). zh-CN: [filter] 对 [date] 的结论 (见类注释). */
    fun verdict(filter: AlarmHolidayFilter?, date: LocalDate, judge: HolidayJudge): Verdict {
        filter ?: return Verdict.RING
        val day = judge.day(date, filter.region)
        return when (filter.mode) {
            AlarmContract.HOLIDAY_MODE_SKIP -> if (day?.holiday == true) Verdict.HOLIDAY else Verdict.RING
            AlarmContract.HOLIDAY_MODE_ONLY -> if (day == null || day.missing || day.holiday) Verdict.RING else Verdict.NONE
            AlarmContract.HOLIDAY_MODE_WORKDAYS -> when {
                day == null -> if (date.dayOfWeek in DEFAULT_WEEKEND) Verdict.NONE else Verdict.RING
                day.workday -> Verdict.RING
                day.holiday && !day.weekend -> Verdict.HOLIDAY
                else -> Verdict.NONE
            }
            else -> Verdict.RING
        }
    }

    /**
     * [spec] with an explicit `holidays.region` in the canonical form [region] gives (T182); an omitted region stays
     * omitted and follows the default region of the settings.
     * zh-CN: 把显式的 `holidays.region` 换成 [region] 给出的规范形式后的 [spec] (T182); 省略的地区保持省略, 随设置中的默认地区.
     */
    fun canonical(spec: AlarmSpec, region: (String) -> String): AlarmSpec {
        val filter = spec.holidays ?: return spec
        val given = filter.region ?: return spec
        val canonical = region(given)
        return if (canonical == given) spec else spec.copy(holidays = filter.copy(region = canonical))
    }

    private const val DAILY = "FREQ=DAILY"
}
