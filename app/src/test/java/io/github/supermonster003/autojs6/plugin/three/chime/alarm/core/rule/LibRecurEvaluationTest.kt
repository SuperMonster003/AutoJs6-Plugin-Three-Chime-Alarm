package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule

import org.dmfs.rfc5545.DateTime
import org.dmfs.rfc5545.RecurrenceSet
import org.dmfs.rfc5545.recur.RecurrenceRule
import org.dmfs.rfc5545.recurrenceset.Difference
import org.dmfs.rfc5545.recurrenceset.OfList
import org.dmfs.rfc5545.recurrenceset.OfRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone

/**
 * Roadmap T10 (P0.2): dmfs lib-recur computes the repeat rules the alarm core needs, from a wall-clock start in a time
 * zone. These cases are the acceptance examples of the evaluation; the rule engine of roadmap P2.2 adds its own tests.
 * zh-CN: 路线图 T10 (P0.2): dmfs lib-recur 能从某时区的墙上时间起点计算闹钟核心需要的重复规则. 这些用例是评估的验收
 * 示例; 路线图 P2.2 的规则引擎另有自己的测试.
 */
class LibRecurEvaluationTest {

    @Test
    fun `the last Friday of every month, written with a negative ordinal or with BYSETPOS`() {
        val expected = listOf(
            "2026-01-30T07:30",
            "2026-02-27T07:30",
            "2026-03-27T07:30",
            "2026-04-24T07:30",
            "2026-05-29T07:30",
        )
        // The start is a Thursday, which is not an instance: OfRule yields rule instances only.
        val start = DateTime(SHANGHAI, 2026, 0, 1, 7, 30, 0)
        assertEquals(expected, OfRule(RecurrenceRule("FREQ=MONTHLY;BYDAY=-1FR"), start).take(5, SHANGHAI))
        assertEquals(expected, OfRule(RecurrenceRule("FREQ=MONTHLY;BYDAY=FR;BYSETPOS=-1"), start).take(5, SHANGHAI))
    }

    @Test
    fun `every 3 days from a given day, across a month end`() {
        val start = DateTime(SHANGHAI, 2026, 1, 26, 6, 0, 0)
        assertEquals(
            listOf("2026-02-26T06:00", "2026-03-01T06:00", "2026-03-04T06:00", "2026-03-07T06:00"),
            OfRule(RecurrenceRule("FREQ=DAILY;INTERVAL=3"), start).take(4, SHANGHAI),
        )
    }

    @Test
    fun `COUNT and UNTIL end the series`() {
        val counted = RecurrenceRule("FREQ=DAILY;INTERVAL=3;COUNT=3").iterator(DateTime(SHANGHAI, 2026, 1, 26, 6, 0, 0))
        val instances = buildList { while (counted.hasNext()) add(counted.nextDateTime().local(SHANGHAI)) }
        assertEquals(listOf("2026-02-26T06:00", "2026-03-01T06:00", "2026-03-04T06:00"), instances)
        assertFalse(counted.hasNext())

        // UNTIL is a UTC instant: 2026-03-18 23:59:59Z is 2026-03-19 07:59:59 in Shanghai, so 03-18 08:00 is the last one.
        val until = OfRule(RecurrenceRule("FREQ=WEEKLY;BYDAY=MO,WE;UNTIL=20260318T235959Z"), DateTime(SHANGHAI, 2026, 2, 2, 8, 0, 0))
        assertEquals(
            listOf("2026-03-02T08:00", "2026-03-04T08:00", "2026-03-09T08:00", "2026-03-11T08:00", "2026-03-16T08:00", "2026-03-18T08:00"),
            until.take(10, SHANGHAI),
        )
    }

    @Test
    fun `excluded dates are removed from the series`() {
        val series = Difference(
            OfRule(RecurrenceRule("FREQ=DAILY;INTERVAL=3"), DateTime(SHANGHAI, 2026, 1, 26, 6, 0, 0)),
            OfList(SHANGHAI, "20260304T060000"),
        )
        assertEquals(
            listOf("2026-02-26T06:00", "2026-03-01T06:00", "2026-03-07T06:00", "2026-03-10T06:00"),
            series.take(4, SHANGHAI),
        )
    }

    private fun RecurrenceSet.take(count: Int, zone: TimeZone): List<String> {
        val iterator = iterator()
        return buildList { while (size < count && iterator.hasNext()) add(iterator.next().local(zone)) }
    }

    private fun DateTime.local(zone: TimeZone): String =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.of(zone.id)).toLocalDateTime().toString()

    private companion object {
        val SHANGHAI: TimeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }

}
