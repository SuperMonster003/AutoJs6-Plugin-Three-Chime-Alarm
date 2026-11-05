package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmOptionsValidator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRuntimeState
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.assertAlarmError
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.optionsOf
import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_RULE
import org.autojs.plugin.alarm.api.AlarmErrorCodes.UNSUPPORTED
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmSpec
import org.autojs.plugin.alarm.api.AlarmTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Roadmap P2.2 (T9, T10, appendix A sections 4.2, 4.3 and 12): the rings of an alarm. The examples of appendix A
 * section 12 that the rule engine serves, the RFC 5545 edges (`BYSETPOS`, `-1FR`, the 31st and February, leap years,
 * week 53), `COUNT` / `UNTIL` / `exclude`, daylight saving in both directions in New York and Berlin, a fixed zone
 * (`Asia/Shanghai`) and the limits of `preview` / `upcoming`.
 * zh-CN: 路线图 P2.2 (T9, T10, 附录 A 第 4.2, 4.3, 12 节): 闹钟的各次响铃. 附录 A 第 12 节中由规则引擎负责的示例, RFC 5545 边界 (`BYSETPOS`, `-1FR`,
 * 31 日与 2 月, 闰年, 第 53 周), `COUNT` / `UNTIL` / `exclude`, 纽约与柏林两个方向的夏令时, 固定时区 (`Asia/Shanghai`), 以及 `preview` / `upcoming`
 * 的上限.
 */
class NextOccurrenceTest {

    private val validator = AlarmOptionsValidator()

    /** 2026-11-05 (Thursday) 10:00 in Shanghai. zh-CN: 上海时间 2026-11-05 (星期四) 10:00. */
    private val now = at("2026-11-05T10:00", SHANGHAI)

    private fun at(local: String, zone: ZoneId): Long = LocalDateTime.parse(local).atZone(zone).toInstant().toEpochMilli()

    private fun local(millis: Long, zone: ZoneId): String = Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime().toString()

    private fun spec(time: String, date: String?, repeat: String? = null, exclude: List<String>? = null, timeZone: String? = null): AlarmSpec {
        val (hour, minute) = time.split(':').map(String::toInt)
        return AlarmSpec(time = AlarmTime(hour, minute), date = date, repeat = repeat?.let(RecurrenceRule::normalize), exclude = exclude, timeZone = timeZone)
    }

    private fun dates(spec: AlarmSpec, count: Int, zone: ZoneId = SHANGHAI): List<String> =
        NextOccurrence.occurrences(spec, zone).take(count).map { it.local.toLocalDate().toString() }.toList()

    @Test
    fun `appendix A 12 - the last Friday of every month at 18 00`() {
        val options = validator.options(optionsOf(18, 0, "repeat" to "FREQ=MONTHLY;BYDAY=-1FR", "label" to "写月报"))
        val spec = options.copy(date = NextOccurrence.implicitDate(options, now, SHANGHAI).toString())
        assertEquals("2026-11-05", spec.date)
        assertEquals(
            listOf("2026-11-27T18:00", "2026-12-25T18:00", "2027-01-29T18:00", "2027-02-26T18:00", "2027-03-26T18:00"),
            NextOccurrence.preview(options, 5, now, SHANGHAI).map { local(it, SHANGHAI) },
        )
        assertEquals("每月最后一个星期五", RepeatText.of(spec.repeat!!, LocalDate.parse(spec.date), java.util.Locale.SIMPLIFIED_CHINESE))
    }

    @Test
    fun `appendix A 12 - a shift every 3 days from November 4`() {
        val spec = validator.options(optionsOf(6, 10, "date" to "2026-11-04", "repeat" to "FREQ=DAILY;INTERVAL=3", "tag" to "shift"))
        assertEquals("FREQ=DAILY;INTERVAL=3", spec.repeat)
        assertEquals(listOf("2026-11-04", "2026-11-07", "2026-11-10", "2026-11-13"), dates(spec, 4))
        assertEquals("2026-11-07T06:10", local(NextOccurrence.next(spec, now, SHANGHAI)!!.at, SHANGHAI))
        assertEquals(
            listOf("2026-11-07T06:10", "2026-11-10T06:10", "2026-11-13T06:10"),
            NextOccurrence.preview(spec, 3, now, SHANGHAI).map { local(it, SHANGHAI) },
        )
    }

    @Test
    fun `appendix A 12 - skipNext leaves the next ring out of upcoming`() {
        val weekdays = validator.options(optionsOf(7, 30, "date" to "2026-11-05", "repeat" to "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"))
        val next = NextOccurrence.next(weekdays, now, SHANGHAI)!!
        assertEquals("2026-11-06T07:30", local(next.at, SHANGHAI))
        val skipped = NextOccurrence.next(weekdays, now, SHANGHAI, skipAt = next.at)!!
        assertEquals("2026-11-09T07:30", local(skipped.at, SHANGHAI))

        val records = listOf(
            AlarmRecord("a_weekdays", "user", "user", 1L, 1L, weekdays, AlarmRuntimeState(skipAt = next.at)),
            AlarmRecord("a_once", "script", "script", 2L, 2L, validator.options(mapOf("at" to now + 25 * 60_000L, "label" to "番茄钟"))),
            AlarmRecord("a_off", "user", "user", 3L, 3L, weekdays.copy(enabled = false)),
        )
        val upcoming = NextOccurrence.upcoming(records, 3, now, SHANGHAI)
        assertEquals(listOf("a_once", "a_weekdays", "a_weekdays"), upcoming.map { it.alarmId })
        assertEquals(listOf("2026-11-05T10:25", "2026-11-09T07:30", "2026-11-10T07:30"), upcoming.map { local(it.at, SHANGHAI) })
        assertEquals("番茄钟", upcoming[0].label)
    }

    @Test
    fun `appendix A 12 - holidays wait for 1_2_0 and one-shot alarms fill in their date`() {
        assertAlarmError(UNSUPPORTED, "holidays") {
            validator.options(optionsOf(7, 30, "holidays" to mapOf("region" to "CN", "mode" to "workdays")))
        }
        // A one-shot alarm whose time has passed today rings tomorrow; one still ahead rings today.
        // zh-CN: 今天时刻已过的一次性闹钟明天响, 时刻未到的今天响.
        assertEquals(LocalDate.of(2026, 11, 6), NextOccurrence.implicitDate(spec("07:30", null), now, SHANGHAI))
        assertEquals(LocalDate.of(2026, 11, 5), NextOccurrence.implicitDate(spec("10:01", null), now, SHANGHAI))
        assertEquals(LocalDate.of(2026, 11, 5), NextOccurrence.implicitDate(spec("07:30", null, "FREQ=DAILY"), now, SHANGHAI))
        val once = spec("07:30", "2026-11-06")
        assertEquals(listOf("2026-11-06"), dates(once, 5))
        assertNull(NextOccurrence.next(once, at("2026-11-06T07:30", SHANGHAI), SHANGHAI))
        assertEquals(emptyList<String>(), dates(spec("07:30", "2026-11-06", exclude = listOf("2026-11-06")), 5))
    }

    @Test
    fun `BYSETPOS, negative ordinals and month ends`() {
        assertEquals(
            listOf("2026-01-30", "2026-02-27", "2026-03-31", "2026-04-30", "2026-05-29", "2026-06-30"),
            dates(spec("09:00", "2026-01-01", "FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1"), 6),
        )
        assertEquals(
            listOf("2026-01-30", "2026-02-27", "2026-03-27"),
            dates(spec("09:00", "2026-01-01", "FREQ=MONTHLY;BYDAY=FR;BYSETPOS=-1"), 3),
        )
        assertEquals(listOf("2026-01-13", "2026-02-10", "2026-03-10"), dates(spec("09:00", "2026-01-01", "FREQ=MONTHLY;BYDAY=2TU"), 3))
        // The 31st is skipped in shorter months, written out or taken from the start date.
        // zh-CN: 较短的月份跳过 31 日, 无论是写明的还是取自起始日的.
        val thirtyFirsts = listOf("2026-01-31", "2026-03-31", "2026-05-31", "2026-07-31", "2026-08-31", "2026-10-31", "2026-12-31")
        assertEquals(thirtyFirsts, dates(spec("09:00", "2026-01-31", "FREQ=MONTHLY;BYMONTHDAY=31"), 7))
        assertEquals(thirtyFirsts, dates(spec("09:00", "2026-01-31", "FREQ=MONTHLY"), 7))
        assertEquals(
            listOf("2027-12-31", "2028-01-31", "2028-02-29", "2028-03-31", "2028-04-30"),
            dates(spec("09:00", "2027-12-01", "FREQ=MONTHLY;BYMONTHDAY=-1"), 5),
        )
    }

    @Test
    fun `leap years and week 53`() {
        assertEquals(listOf("2024-02-29", "2028-02-29", "2032-02-29"), dates(spec("09:00", "2024-02-29", "FREQ=YEARLY"), 3))
        assertEquals(listOf("2026-02-28", "2027-02-28", "2028-02-29"), dates(spec("09:00", "2026-02-28", "FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=-1"), 3))
        assertEquals(listOf("2024-12-31", "2025-12-31", "2026-12-31"), dates(spec("09:00", "2024-12-31", "FREQ=YEARLY;BYYEARDAY=-1"), 3))
        // Only years with 53 weeks have week 53 (lib-recur 0.17.1 gives week 52 in the others).
        // zh-CN: 只有 53 周的年份才有第 53 周 (lib-recur 0.17.1 在其他年份给出第 52 周).
        assertEquals(
            listOf("2020-12-28", "2026-12-28", "2032-12-27", "2037-12-28", "2043-12-28"),
            dates(spec("09:00", "2020-01-01", "FREQ=YEARLY;BYWEEKNO=53;BYDAY=MO"), 5),
        )
        // Week 1 reaches back into December (lib-recur 0.17.1 gives the days of week 53 instead).
        // zh-CN: 第 1 周会伸回到 12 月 (lib-recur 0.17.1 给出的是第 53 周的日子).
        assertEquals(
            listOf("2024-12-30", "2024-12-31", "2025-12-29", "2025-12-30", "2025-12-31"),
            dates(spec("09:00", "2020-06-10", "FREQ=YEARLY;BYWEEKNO=1;BYMONTH=12;BYDAY=MO,TU,WE,TH,FR,SA,SU"), 5),
        )
        assertEquals(LocalDate.of(2025, 12, 29), NextOccurrence.firstWeekStart(2026, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 1, 4), NextOccurrence.firstWeekStart(2026, DayOfWeek.SUNDAY))
    }

    @Test
    fun `yearly BYSETPOS picks from the whole year, the days before the start included`() {
        // lib-recur 0.17.1 drops the days before the start first and so picks another day or a later year (T52).
        // zh-CN: lib-recur 0.17.1 先去掉起点之前的日子, 因而选中别的日子或晚一年 (T52).
        assertEquals(listOf("2026-05-01", "2027-05-01"), dates(spec("09:00", "2026-04-15", "FREQ=YEARLY;BYMONTH=2,5;BYMONTHDAY=1;BYSETPOS=2"), 2))
        assertEquals(listOf("2026-07-19", "2027-07-19", "2028-07-18"), dates(spec("09:00", "2026-03-01", "FREQ=YEARLY;BYYEARDAY=1,200;BYSETPOS=2"), 3))
        assertEquals(listOf("2027-02-01", "2028-02-07"), dates(spec("09:00", "2026-04-15", "FREQ=YEARLY;BYMONTH=2,5;BYDAY=MO;BYSETPOS=1"), 2))
        assertEquals(listOf("2026-03-29", "2027-03-28"), dates(spec("09:00", "2026-01-01", "FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU;BYSETPOS=-1"), 2))
        // An ordinal without BYMONTH counts within the year. zh-CN: 没有 BYMONTH 时序号按年计数.
        assertEquals(listOf("2026-12-28", "2027-12-27"), dates(spec("09:00", "2026-01-01", "FREQ=YEARLY;BYDAY=-1MO;BYSETPOS=1"), 2))
        // BYYEARDAY with BYDAY makes lib-recur 0.17.1 fail; -366 exists only in leap years.
        // zh-CN: BYYEARDAY 与 BYDAY 同用会让 lib-recur 0.17.1 出错; -366 只在闰年存在.
        assertEquals(
            listOf("2028-12-12", "2033-12-13", "2036-01-01", "2039-12-13"),
            dates(spec("09:00", "2026-01-01", "FREQ=YEARLY;BYYEARDAY=-366,347;BYDAY=TU"), 4),
        )
    }

    @Test
    fun `COUNT, UNTIL and exclude`() {
        // An excluded date still uses up one of the COUNT dates. zh-CN: 被排除的日期仍占用 COUNT 中的一次.
        assertEquals(
            listOf("2026-11-04", "2026-11-06", "2026-11-07", "2026-11-08"),
            dates(spec("07:30", "2026-11-04", "FREQ=DAILY;COUNT=5", exclude = listOf("2026-11-05")), 10),
        )
        assertEquals(listOf("2026-11-04", "2026-11-05", "2026-11-06"), dates(spec("07:30", "2026-11-04", "FREQ=DAILY;UNTIL=20261106"), 10))
        assertEquals(listOf("2026-11-04", "2026-11-05"), dates(spec("07:30", "2026-11-04", "FREQ=DAILY;UNTIL=20261105T073000"), 10))
        // 07:30 in Shanghai is 23:30 UTC the day before. zh-CN: 上海 07:30 是前一天 UTC 23:30.
        assertEquals(listOf("2026-11-04", "2026-11-05"), dates(spec("07:30", "2026-11-04", "FREQ=DAILY;UNTIL=20261105T230000Z"), 10))
        assertEquals(listOf("2026-11-04", "2026-11-05"), dates(spec("07:30", "2026-11-04", RecurrenceRule.normalize(mapOf("freq" to "daily", "until" to at("2026-11-05T07:30", SHANGHAI)))), 10))
        // The start counts only when it matches the rule. zh-CN: 起始日只在符合规则时才算.
        assertEquals(listOf("2026-11-09", "2026-11-16"), dates(spec("07:30", "2026-11-05", "FREQ=WEEKLY;BYDAY=MO;COUNT=2"), 10))
        assertEquals(listOf("2026-11-05", "2026-11-12"), dates(spec("07:30", "2026-11-05", "FREQ=WEEKLY;COUNT=2"), 10))
    }

    @Test
    fun `a rule that never gives a date is INVALID_RULE`() {
        for ((rule, date) in listOf(
            "FREQ=YEARLY;BYMONTHDAY=31" to "2026-02-10",
            "FREQ=MONTHLY;BYMONTH=2;BYMONTHDAY=30" to "2026-01-01",
            "FREQ=YEARLY;BYWEEKNO=53;BYMONTH=6" to "2026-01-01",
            // Every fourth year from 2025 is never a leap year. zh-CN: 从 2025 年起每四年一次的年份都不是闰年.
            "FREQ=YEARLY;INTERVAL=4;BYYEARDAY=366" to "2025-01-01",
        )) {
            assertAlarmError(INVALID_RULE, "gives no date") { NextOccurrence.next(spec("07:30", date, rule), now, SHANGHAI) }
        }
        // A finished rule is not an error. zh-CN: 已结束的规则不是错误.
        assertNull(NextOccurrence.next(spec("07:30", "2026-01-01", "FREQ=DAILY;COUNT=3"), now, SHANGHAI))
        assertNull(NextOccurrence.next(spec("07:30", "2026-01-01", "FREQ=DAILY;UNTIL=20260105"), now, SHANGHAI))
    }

    @Test
    fun `a time skipped by daylight saving rings when the clock jumps, a repeated one rings once`() {
        val newYork = ZoneId.of("America/New_York")
        val spring = NextOccurrence.occurrences(spec("02:30", "2026-03-07", "FREQ=DAILY"), newYork).take(3).map { Instant.ofEpochMilli(it.at).toString() }.toList()
        assertEquals(listOf("2026-03-07T07:30:00Z", "2026-03-08T07:00:00Z", "2026-03-09T06:30:00Z"), spring)
        assertEquals("2026-03-08T03:00", local(Instant.parse(spring[1]).toEpochMilli(), newYork))
        val autumn = NextOccurrence.occurrences(spec("01:30", "2026-10-31", "FREQ=DAILY"), newYork).take(3).map { Instant.ofEpochMilli(it.at).toString() }.toList()
        assertEquals(listOf("2026-10-31T05:30:00Z", "2026-11-01T05:30:00Z", "2026-11-02T06:30:00Z"), autumn)

        val berlin = ZoneId.of("Europe/Berlin")
        val berlinSpring = NextOccurrence.occurrences(spec("02:30", "2026-03-28", "FREQ=DAILY"), berlin).take(3).map { Instant.ofEpochMilli(it.at).toString() }.toList()
        assertEquals(listOf("2026-03-28T01:30:00Z", "2026-03-29T01:00:00Z", "2026-03-30T00:30:00Z"), berlinSpring)
        val berlinAutumn = NextOccurrence.occurrences(spec("02:30", "2026-10-24", "FREQ=DAILY"), berlin).take(3).map { Instant.ofEpochMilli(it.at).toString() }.toList()
        assertEquals(listOf("2026-10-24T00:30:00Z", "2026-10-25T00:30:00Z", "2026-10-26T01:30:00Z"), berlinAutumn)

        assertEquals(Instant.parse("2026-03-29T01:00:00Z"), TimeZonePolicy.instantOf(LocalDateTime.parse("2026-03-29T02:00"), berlin))
        assertEquals(Instant.parse("2026-03-29T01:00:00Z"), TimeZonePolicy.instantOf(LocalDateTime.parse("2026-03-29T02:59:59"), berlin))
        assertEquals(Instant.parse("2026-10-25T00:00:00Z"), TimeZonePolicy.instantOf(LocalDateTime.parse("2026-10-25T02:00"), berlin))
    }

    @Test
    fun `a fixed time zone does not follow the device`() {
        val newYork = ZoneId.of("America/New_York")
        val fixed = spec("07:30", "2026-11-05", timeZone = "Asia/Shanghai")
        val floating = spec("07:30", "2026-11-05")
        for (device in listOf(newYork, SHANGHAI, ZoneOffset.UTC)) {
            assertEquals("2026-11-04T23:30:00Z", Instant.ofEpochMilli(NextOccurrence.occurrences(fixed, device).single().at).toString())
        }
        assertEquals("2026-11-05T12:30:00Z", Instant.ofEpochMilli(NextOccurrence.occurrences(floating, newYork).single().at).toString())
        assertEquals("2026-11-04T23:30:00Z", Instant.ofEpochMilli(NextOccurrence.occurrences(floating, SHANGHAI).single().at).toString())
        // The implicit date of a fixed-zone alarm is today in that zone. zh-CN: 固定时区闹钟的隐式日期是该时区的今天.
        val lateEvening = at("2026-11-05T22:00", newYork)
        val fixedLater = spec("07:30", null, timeZone = "Asia/Shanghai")
        assertEquals(LocalDate.of(2026, 11, 7), NextOccurrence.implicitDate(fixedLater, lateEvening, TimeZonePolicy.zoneOf(fixedLater, newYork)))
        val floatingLater = spec("07:30", null)
        assertEquals(LocalDate.of(2026, 11, 6), NextOccurrence.implicitDate(floatingLater, lateEvening, TimeZonePolicy.zoneOf(floatingLater, newYork)))
        assertEquals("2026-11-06T23:30:00Z", Instant.ofEpochMilli(NextOccurrence.preview(fixedLater, 1, lateEvening, newYork).single()).toString())
    }

    @Test
    fun `skipping ahead gives the same rings as counting from the start`() {
        val far = at("2031-06-15T12:00", SHANGHAI)
        for (rule in listOf(
            "FREQ=DAILY", "FREQ=WEEKLY;INTERVAL=3;BYDAY=TU,SA", "FREQ=MONTHLY;BYDAY=-1FR", "FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=29", "FREQ=DAILY;COUNT=12000",
            "FREQ=YEARLY;INTERVAL=3;BYYEARDAY=1,-1", "FREQ=YEARLY;BYWEEKNO=1,53;BYDAY=TH", "FREQ=YEARLY;INTERVAL=2;BYMONTH=2,5;BYDAY=MO;BYSETPOS=1,-1",
        )) {
            val alarm = spec("07:30", "2001-03-04", rule)
            val slow = NextOccurrence.occurrences(alarm, SHANGHAI).first { it.at > far }
            assertEquals(rule, slow, NextOccurrence.next(alarm, far, SHANGHAI))
        }
    }

    @Test
    fun `preview and upcoming stay within their limits`() {
        val daily = spec("07:30", "2026-11-05", "FREQ=DAILY")
        assertEquals(AlarmLimits.MAX_PREVIEW_COUNT, NextOccurrence.preview(daily, 1000, now, SHANGHAI).size)
        assertEquals(AlarmLimits.DEFAULT_PREVIEW_COUNT, NextOccurrence.preview(daily, AlarmLimits.DEFAULT_PREVIEW_COUNT, now, SHANGHAI).size)
        assertEquals(emptyList<Long>(), NextOccurrence.preview(daily, 0, now, SHANGHAI))
        // preview fills in the date like a new alarm: a daily alarm at 07:30 starts tomorrow.
        // zh-CN: preview 像新建闹钟一样补上日期: 07:30 的每日闹钟从明天开始.
        assertEquals("2026-11-06T07:30", local(NextOccurrence.preview(spec("07:30", null, "FREQ=DAILY"), 1, now, SHANGHAI).single(), SHANGHAI))

        val records = (1..150).map { AlarmRecord("a_$it", "script", "script", it.toLong(), it.toLong(), daily) }
        val upcoming = NextOccurrence.upcoming(records, 500, now, SHANGHAI)
        assertEquals(AlarmLimits.MAX_UPCOMING_COUNT, upcoming.size)
        assertTrue(upcoming.all { local(it.at, SHANGHAI) == "2026-11-06T07:30" })
        assertEquals((1..100).map { "a_$it" }, upcoming.map { it.alarmId })

        val snoozed = AlarmRecord("a_snooze", "user", "user", 1L, 1L, daily, AlarmRuntimeState(snoozeUntil = now + 60_000L))
        assertEquals(listOf(now + 60_000L, at("2026-11-06T07:30", SHANGHAI)), NextOccurrence.upcoming(listOf(snoozed), 2, now, SHANGHAI).map { it.at })
    }

    companion object {
        val SHANGHAI: ZoneId = ZoneId.of("Asia/Shanghai")
    }
}
