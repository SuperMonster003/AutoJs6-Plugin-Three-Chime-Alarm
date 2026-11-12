package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.assertAlarmError
import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_OPTIONS
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Roadmap P7.1 (T107): the duration notation of timers and tools ("25m", "1h30m", "90s"), the 1 s - 100 h range and
 * the clock reading of the notifications.
 * zh-CN: 路线图 P7.1 (T107): 计时器与工具的时长写法 ("25m", "1h30m", "90s"), 1 s - 100 h 的范围, 以及通知里的钟面读数.
 */
class TimerDurationsTest {

    @Test
    fun `the h m s notation reads as milliseconds`() {
        assertEquals(25 * MINUTE, TimerDurations.parse("25m"))
        assertEquals(HOUR + 30 * MINUTE, TimerDurations.parse("1h30m"))
        assertEquals(90 * SECOND, TimerDurations.parse("90s"))
        assertEquals(HOUR + 30 * MINUTE + 15 * SECOND, TimerDurations.parse("1h30m15s"))
        assertEquals("case and spaces do not matter", HOUR + 5 * SECOND, TimerDurations.parse(" 1H 5S "))
        assertEquals(100 * HOUR, TimerDurations.parse("100h"))
        assertEquals("a part may exceed its unit", 150 * MINUTE, TimerDurations.parse("150m"))
        assertEquals("zero parses, the range check refuses it", 0L, TimerDurations.parse("0s"))
    }

    @Test
    fun `any other notation is refused`() {
        for (text in listOf("", "  ", "m", "25", "30m1h", "1h1h", "1.5h", "1d", "-5m", "+5m", "5 m", "1h-30m", "1h30", "abc")) {
            assertAlarmError(INVALID_OPTIONS, "duration") { TimerDurations.parse(text) }
        }
        assertAlarmError(INVALID_OPTIONS, "too long") { TimerDurations.parse("99999999999999999999h") }
        assertAlarmError(INVALID_OPTIONS, "too long") { TimerDurations.parse("9223372036854775807h") }
    }

    @Test
    fun `the signed form adds a leading minus for addTime`() {
        assertEquals(-MINUTE, TimerDurations.parseSigned("-1m"))
        assertEquals(-(HOUR + 30 * MINUTE), TimerDurations.parseSigned(" -1h30m"))
        assertEquals(90 * SECOND, TimerDurations.parseSigned("90s"))
        assertAlarmError(INVALID_OPTIONS, "millis") { TimerDurations.parseSigned("--1m") }
        assertAlarmError(INVALID_OPTIONS, "millis") { TimerDurations.parseSigned("-") }
    }

    @Test
    fun `durations are 1 s to 100 h inclusive`() {
        assertEquals(1_000L, TimerDurations.requireInRange(1_000L))
        assertEquals(360_000_000L, TimerDurations.requireInRange(360_000_000L))
        assertEquals(TimerDurations.MAX_MILLIS, TimerDurations.requireInRange(TimerDurations.parse("100h")))
        for (millis in listOf(999L, 0L, -1L, 360_000_001L, Long.MAX_VALUE, Long.MIN_VALUE)) {
            assertAlarmError(INVALID_OPTIONS, "1 s - 100 h") { TimerDurations.requireInRange(millis) }
        }
        assertAlarmError(INVALID_OPTIONS) { TimerDurations.requireInRange(TimerDurations.parse("100h1s")) }
        assertAlarmError(INVALID_OPTIONS, "the time left") { TimerDurations.requireInRange(0L, "the time left") }
    }

    @Test
    fun `the clock reading rounds whole seconds up`() {
        assertEquals("0:00", TimerDurations.clockText(0L))
        assertEquals("0:01", TimerDurations.clockText(1L))
        assertEquals("0:59", TimerDurations.clockText(59 * SECOND))
        assertEquals("25:00", TimerDurations.clockText(25 * MINUTE))
        assertEquals("1:00:00", TimerDurations.clockText(HOUR))
        assertEquals("1:30:00", TimerDurations.clockText(HOUR + 29 * MINUTE + 59 * SECOND + 1))
        assertEquals("100:00:00", TimerDurations.clockText(100 * HOUR))
        assertEquals("0:00", TimerDurations.clockText(-5 * SECOND))
    }

    private companion object {
        const val SECOND = 1_000L
        const val MINUTE = 60 * SECOND
        const val HOUR = 60 * MINUTE
    }
}
