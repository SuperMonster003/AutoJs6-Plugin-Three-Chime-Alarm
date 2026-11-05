package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.assertAlarmError
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RecurrenceRule.Freq
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RecurrenceRule.Until
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RecurrenceRule.WeekdayNum
import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_OPTIONS
import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_RULE
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Roadmap P2.2 (D11, appendix A section 4.3): the three inputs of `repeat` become one canonical RRULE, the same one the
 * host normalizer gives (the cases of the host's `AlarmRepeatRulesTest` are repeated here), and unsupported parts fail
 * with `INVALID_RULE` naming the part.
 * zh-CN: 路线图 P2.2 (D11, 附录 A 第 4.3 节): `repeat` 的三种输入变为同一个规范 RRULE, 与宿主规范化的结果相同 (这里重复了宿主 `AlarmRepeatRulesTest`
 * 的用例); 不支持的部分以 `INVALID_RULE` 失败并写明该部分.
 */
class RecurrenceRuleTest {

    @Test
    fun `the weekday shorthand becomes a weekly rule`() {
        assertEquals("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", RecurrenceRule.normalize(listOf(1.0, 2.0, 3.0, 4.0, 5.0)))
        assertEquals("FREQ=WEEKLY;BYDAY=MO,SU", RecurrenceRule.normalize(listOf(0.0, 1.0, 1.0)))
        assertEquals("FREQ=WEEKLY;BYDAY=SA,SU", RecurrenceRule.normalize(listOf(6, 0)))
        assertAlarmError(INVALID_RULE, "weekday numbers are 0 (Sunday) to 6 (Saturday), got 7") { RecurrenceRule.normalize(listOf(1.0, 7.0)) }
        assertAlarmError(INVALID_RULE, "got 1.5") { RecurrenceRule.normalize(listOf(1.5)) }
        assertAlarmError(INVALID_RULE, "the weekday array is empty") { RecurrenceRule.normalize(emptyList<Any>()) }
    }

    @Test
    fun `RRULE strings are put in canonical form`() {
        assertEquals("FREQ=MONTHLY;BYDAY=-1FR", RecurrenceRule.normalize("FREQ=MONTHLY;BYDAY=-1FR"))
        assertEquals("FREQ=MONTHLY;BYDAY=-1FR", RecurrenceRule.normalize("RRULE:byday=-1fr;freq=monthly"))
        assertEquals("FREQ=DAILY", RecurrenceRule.normalize("FREQ=DAILY;INTERVAL=1;WKST=MO"))
        assertEquals("FREQ=DAILY;INTERVAL=3", RecurrenceRule.normalize("FREQ=DAILY;INTERVAL=3"))
        assertEquals("FREQ=WEEKLY;BYDAY=MO,WE,SU;WKST=SU", RecurrenceRule.normalize("FREQ=WEEKLY;BYDAY=SU,WE,MO,WE;WKST=SU"))
        assertEquals("FREQ=YEARLY;COUNT=5;BYMONTH=2,12;BYMONTHDAY=-1", RecurrenceRule.normalize("FREQ=YEARLY;BYMONTHDAY=-1;BYMONTH=12,2;COUNT=5"))
        assertEquals("FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1", RecurrenceRule.normalize("FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1"))
        assertEquals("FREQ=MONTHLY;BYDAY=MO,-1MO,1MO,2TU", RecurrenceRule.normalize(" RRULE: FREQ=MONTHLY ; BYDAY=2TU,-1MO,+1MO,MO ; "))
        assertEquals("FREQ=YEARLY;UNTIL=20261231T235959Z;BYWEEKNO=-1,1,20", RecurrenceRule.normalize("FREQ=YEARLY;BYWEEKNO=20,1,-1;UNTIL=20261231t235959z"))
        // A canonical rule comes back unchanged. zh-CN: 规范规则原样返回.
        for (rule in listOf("FREQ=DAILY", "FREQ=YEARLY;INTERVAL=2;COUNT=3;BYMONTH=1;BYYEARDAY=-1,100;BYMONTHDAY=1;BYDAY=MO;BYSETPOS=1,2;WKST=SU")) {
            assertEquals(rule, RecurrenceRule.normalize(rule))
            assertEquals(rule, RecurrenceRule.parse(rule).text)
        }
    }

    @Test
    fun `unsupported parts and impossible combinations are named`() {
        assertAlarmError(INVALID_RULE, "BYHOUR is not supported") { RecurrenceRule.normalize("FREQ=DAILY;BYHOUR=7") }
        assertAlarmError(INVALID_RULE, "BYMINUTE is not supported") { RecurrenceRule.normalize("FREQ=DAILY;BYMINUTE=7") }
        assertAlarmError(INVALID_RULE, "BYSECOND is not supported") { RecurrenceRule.normalize("FREQ=DAILY;BYSECOND=7") }
        assertAlarmError(INVALID_RULE, "FREQ=HOURLY is not supported") { RecurrenceRule.normalize("FREQ=HOURLY") }
        assertAlarmError(INVALID_RULE, "FREQ=MINUTELY is not supported") { RecurrenceRule.normalize("FREQ=MINUTELY") }
        assertAlarmError(INVALID_RULE, "FREQ=SECONDLY is not supported") { RecurrenceRule.normalize("FREQ=SECONDLY") }
        assertAlarmError(INVALID_RULE, "only one RRULE line is supported") { RecurrenceRule.normalize("FREQ=DAILY" + 10.toChar() + "EXDATE:20261104") }
        assertAlarmError(INVALID_RULE, "EXDATE is not supported in the rule (use exclude)") { RecurrenceRule.normalize("EXDATE:20261104") }
        assertAlarmError(INVALID_RULE, "RDATE is not supported") { RecurrenceRule.normalize("FREQ=DAILY;RDATE=20261104") }
        assertAlarmError(INVALID_RULE, "RDATE is not supported") { RecurrenceRule.normalize("RDATE:20261104") }
        assertAlarmError(INVALID_RULE, "DTSTART is not supported in the rule (use date for the first day)") { RecurrenceRule.normalize("DTSTART:20261104T061000") }
        assertAlarmError(INVALID_RULE, "COUNT and UNTIL are exclusive") { RecurrenceRule.normalize("FREQ=DAILY;COUNT=3;UNTIL=20261231") }
        assertAlarmError(INVALID_RULE, "needs FREQ=MONTHLY or FREQ=YEARLY") { RecurrenceRule.normalize("FREQ=WEEKLY;BYDAY=2TU") }
        assertAlarmError(INVALID_RULE, "BYMONTHDAY is not allowed with FREQ=WEEKLY") { RecurrenceRule.normalize("FREQ=WEEKLY;BYMONTHDAY=1") }
        assertAlarmError(INVALID_RULE, "BYWEEKNO needs FREQ=YEARLY") { RecurrenceRule.normalize("FREQ=MONTHLY;BYWEEKNO=1") }
        assertAlarmError(INVALID_RULE, "BYYEARDAY is not allowed with FREQ=DAILY") { RecurrenceRule.normalize("FREQ=DAILY;BYYEARDAY=1") }
        assertAlarmError(INVALID_RULE, "not allowed together with BYWEEKNO") { RecurrenceRule.normalize("FREQ=YEARLY;BYWEEKNO=1;BYDAY=1MO") }
        assertAlarmError(INVALID_RULE, "-5 to -1 or 1 to 5 with FREQ=MONTHLY") { RecurrenceRule.normalize("FREQ=MONTHLY;BYDAY=6MO") }
        assertAlarmError(INVALID_RULE, "BYSETPOS needs another BY part") { RecurrenceRule.normalize("FREQ=MONTHLY;BYSETPOS=1") }
        assertAlarmError(INVALID_RULE, "BYMONTHDAY values are -31 to -1 or 1 to 31, got '0'") { RecurrenceRule.normalize("FREQ=MONTHLY;BYMONTHDAY=0") }
        assertAlarmError(INVALID_RULE, "BYMONTH values are 1 to 12, got '13'") { RecurrenceRule.normalize("FREQ=YEARLY;BYMONTH=13") }
        assertAlarmError(INVALID_RULE, "INTERVAL must be a positive integer") { RecurrenceRule.normalize("FREQ=DAILY;INTERVAL=0") }
        assertAlarmError(INVALID_RULE, "UNTIL is not a valid date") { RecurrenceRule.normalize("FREQ=DAILY;UNTIL=20260230") }
        assertAlarmError(INVALID_RULE, "UNTIL must be yyyyMMdd") { RecurrenceRule.normalize("FREQ=DAILY;UNTIL=2026-12-31") }
        assertAlarmError(INVALID_RULE, "COUNT appears more than once") { RecurrenceRule.normalize("FREQ=DAILY;COUNT=1;COUNT=2") }
        assertAlarmError(INVALID_RULE, "FREQ is required") { RecurrenceRule.normalize("BYDAY=MO") }
        assertAlarmError(INVALID_RULE, "unknown part FOO") { RecurrenceRule.normalize("FREQ=DAILY;FOO=1") }
        assertAlarmError(INVALID_RULE, "unknown FREQ=FORTNIGHTLY") { RecurrenceRule.normalize("FREQ=FORTNIGHTLY") }
        assertAlarmError(INVALID_RULE, "unknown property 'VEVENT'") { RecurrenceRule.normalize("VEVENT:FREQ=DAILY") }
        assertAlarmError(INVALID_RULE, "the rule is empty") { RecurrenceRule.normalize("  ") }
    }

    @Test
    fun `structured rules use the same canonical form`() {
        assertEquals("FREQ=DAILY;INTERVAL=3", RecurrenceRule.normalize(mapOf("freq" to "daily", "interval" to 3.0)))
        assertEquals("FREQ=MONTHLY;BYDAY=-1FR", RecurrenceRule.normalize(mapOf("freq" to "monthly", "byDay" to "-1FR")))
        assertEquals("FREQ=WEEKLY;BYDAY=MO,FR", RecurrenceRule.normalize(mapOf("freq" to "weekly", "byDay" to listOf(5.0, 1.0))))
        assertEquals("FREQ=WEEKLY;BYDAY=TU;WKST=SU", RecurrenceRule.normalize(mapOf("freq" to "weekly", "byDay" to listOf("TU"), "weekStart" to 0.0)))
        assertEquals("FREQ=DAILY;UNTIL=20261231", RecurrenceRule.normalize(mapOf("freq" to "daily", "until" to "2026-12-31")))
        assertEquals("FREQ=DAILY;UNTIL=20261104T221000Z", RecurrenceRule.normalize(mapOf("freq" to "daily", "until" to 1_793_830_200_000L)))
        assertEquals("FREQ=YEARLY;BYMONTH=11;BYMONTHDAY=4", RecurrenceRule.normalize(mapOf("freq" to "yearly", "byMonth" to 11.0, "byMonthDay" to listOf(4.0))))
        assertEquals(
            "FREQ=YEARLY;COUNT=4;BYWEEKNO=-1,1;BYYEARDAY=100;BYSETPOS=-1",
            RecurrenceRule.normalize(mapOf("freq" to "YEARLY", "count" to 4, "byWeekNo" to listOf(1, -1), "byYearDay" to 100, "bySetPos" to listOf(-1), "weekStart" to 1)),
        )
        assertAlarmError(INVALID_OPTIONS, "repeat: unknown key every") { RecurrenceRule.normalize(mapOf("freq" to "daily", "every" to 2.0)) }
        assertAlarmError(INVALID_RULE, "BYHOUR is not supported") { RecurrenceRule.normalize(mapOf("freq" to "daily", "byHour" to 7.0)) }
        assertAlarmError(INVALID_RULE, "RDATE is not supported") { RecurrenceRule.normalize(mapOf("freq" to "daily", "rdate" to "20261104")) }
        assertAlarmError(INVALID_RULE, "repeat.interval must be an integer, got 1.5") { RecurrenceRule.normalize(mapOf("freq" to "daily", "interval" to 1.5)) }
        assertAlarmError(INVALID_RULE, "repeat.byDay numbers are 0 (Sunday) to 6 (Saturday), got 9") { RecurrenceRule.normalize(mapOf("freq" to "weekly", "byDay" to 9.0)) }
        assertAlarmError(INVALID_RULE, "repeat.weekStart must be 0 (Sunday) or 1 (Monday), got 3") { RecurrenceRule.normalize(mapOf("freq" to "weekly", "weekStart" to 3.0)) }
        assertAlarmError(INVALID_RULE, "repeat.until is not a valid date") { RecurrenceRule.normalize(mapOf("freq" to "daily", "until" to "2026-02-30")) }
        assertAlarmError(INVALID_OPTIONS, "repeat must be an RRULE string") { RecurrenceRule.normalize(true) }
    }

    @Test
    fun `the typed view reads every part`() {
        val rule = RecurrenceRule.parse("FREQ=MONTHLY;INTERVAL=2;BYMONTH=1,7;BYMONTHDAY=-1;BYDAY=FR,-1FR;BYSETPOS=1;WKST=SU")
        assertEquals(Freq.MONTHLY, rule.freq)
        assertEquals(2, rule.interval)
        assertEquals(listOf(1, 7), rule.byMonth)
        assertEquals(listOf(-1), rule.byMonthDay)
        assertEquals(listOf(WeekdayNum(null, DayOfWeek.FRIDAY), WeekdayNum(-1, DayOfWeek.FRIDAY)), rule.byDay)
        assertEquals(listOf(1), rule.bySetPos)
        assertEquals(DayOfWeek.SUNDAY, rule.weekStart)
        assertEquals("FREQ=MONTHLY;INTERVAL=2;BYMONTH=1,7;BYMONTHDAY=-1;BYDAY=FR,-1FR;BYSETPOS=1;WKST=SU", rule.textWithoutUntil)

        assertEquals(Until.Date(LocalDate.of(2026, 12, 31)), RecurrenceRule.parse("FREQ=DAILY;UNTIL=20261231").until)
        assertEquals(Until.Floating(LocalDateTime.of(2026, 12, 31, 7, 30)), RecurrenceRule.parse("FREQ=DAILY;UNTIL=20261231T073000").until)
        val utc = RecurrenceRule.parse("FREQ=DAILY;UNTIL=20261231T073000Z")
        assertEquals(Until.Utc(Instant.parse("2026-12-31T07:30:00Z")), utc.until)
        assertEquals("FREQ=DAILY", utc.textWithoutUntil)
        assertEquals(10, RecurrenceRule.parse("FREQ=DAILY;COUNT=10").count)
    }
}
