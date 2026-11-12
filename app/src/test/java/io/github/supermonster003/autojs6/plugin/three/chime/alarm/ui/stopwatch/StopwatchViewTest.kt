package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.stopwatch

import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.StopwatchLap
import org.autojs.plugin.alarm.api.StopwatchSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P7.4 (T138): the stopwatch page on plain snapshots. The reading is cut to hundredths, the buttons follow the
 * state, a lap is taken only while running and below 100 laps, the fastest and the slowest lap are marked from two
 * differing laps on, the laps list newest first, and the reading of a running stopwatch moves on with the clock.
 * zh-CN: 路线图 P7.4 (T138): 以普通快照检查秒表页. 读数截断到百分之一秒, 按钮跟随状态, 只有运行中且不足 100 圈时才能计次, 至少两圈且不相同时标出最快与最慢圈,
 * 圈按从新到旧排列, 运行中秒表的读数随时钟前进.
 */
class StopwatchViewTest {

    private fun laps(vararg millis: Long): List<StopwatchLap> {
        var total = 0L
        return millis.mapIndexed { i, lap -> total += lap; StopwatchLap(i + 1, lap, total) }
    }

    @Test
    fun theReadingIsCutToHundredths() {
        assertEquals("0:00.00", StopwatchView.text(0L))
        assertEquals("0:00.00", StopwatchView.text(-5L))
        assertEquals("0:05.12", StopwatchView.text(5_129L))
        assertEquals("12:34.56", StopwatchView.text(754_569L))
        assertEquals("1:02:03.45", StopwatchView.text(3_723_459L))
    }

    @Test
    fun buttonsAndLapsFollowTheState() {
        assertEquals(listOf(StopwatchButton.START), StopwatchView.buttonsOf(AlarmContract.STOPWATCH_IDLE))
        assertEquals(listOf(StopwatchButton.LAP, StopwatchButton.PAUSE), StopwatchView.buttonsOf(AlarmContract.STOPWATCH_RUNNING))
        assertEquals(listOf(StopwatchButton.RESET, StopwatchButton.RESUME), StopwatchView.buttonsOf(AlarmContract.STOPWATCH_PAUSED))

        val almost = laps(*LongArray(AlarmLimits.MAX_STOPWATCH_LAPS - 1) { 1_000L })
        val full = laps(*LongArray(AlarmLimits.MAX_STOPWATCH_LAPS) { 1_000L })
        assertTrue(StopwatchView.canLap(StopwatchSnapshot(AlarmContract.STOPWATCH_RUNNING, 99_000L, almost)))
        assertFalse(StopwatchView.lapLimitReached(StopwatchSnapshot(AlarmContract.STOPWATCH_RUNNING, 99_000L, almost)))
        assertFalse(StopwatchView.canLap(StopwatchSnapshot(AlarmContract.STOPWATCH_RUNNING, 100_000L, full)))
        assertTrue(StopwatchView.lapLimitReached(StopwatchSnapshot(AlarmContract.STOPWATCH_PAUSED, 100_000L, full)))
        assertFalse(StopwatchView.canLap(StopwatchSnapshot(AlarmContract.STOPWATCH_PAUSED, 1_000L, laps(1_000L))))
    }

    @Test
    fun fastestAndSlowestLapsAreMarked() {
        assertEquals(StopwatchView.Marks(null, null), StopwatchView.marks(emptyList()))
        assertEquals(StopwatchView.Marks(null, null), StopwatchView.marks(laps(3_000L)))
        assertEquals(StopwatchView.Marks(null, null), StopwatchView.marks(laps(2_000L, 2_000L, 2_000L)))
        assertEquals(StopwatchView.Marks(2, 3), StopwatchView.marks(laps(3_000L, 2_000L, 5_000L)))
        assertEquals("a tie goes to the earlier lap", StopwatchView.Marks(1, 3), StopwatchView.marks(laps(2_000L, 2_000L, 4_000L, 4_000L)))
        assertEquals(listOf(3, 2, 1), StopwatchView.listed(laps(1L, 2L, 3L)).map { it.index })
    }

    @Test
    fun aRunningReadingMovesOnWithTheClock() {
        val running = Reading(StopwatchSnapshot(AlarmContract.STOPWATCH_RUNNING, 1_000L, emptyList()), readAt = 500L)
        assertEquals(1_300L, running.elapsedAt(800L))
        assertEquals("a clock behind the read never goes back", 1_000L, running.elapsedAt(400L))
        val paused = Reading(StopwatchSnapshot(AlarmContract.STOPWATCH_PAUSED, 1_000L, emptyList()), readAt = 500L)
        assertEquals(1_000L, paused.elapsedAt(9_000L))
    }
}
