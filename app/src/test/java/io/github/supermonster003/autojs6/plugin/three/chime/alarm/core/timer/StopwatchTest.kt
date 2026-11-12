package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.assertAlarmError
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_OPTIONS
import org.autojs.plugin.alarm.api.AlarmErrorCodes.LIMIT_EXCEEDED
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.StopwatchLap
import org.autojs.plugin.alarm.api.StopwatchSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Roadmap P7.2 (T118 - T120, Q28): the single stopwatch: idle, running and paused, laps (at most 100), a wall-clock
 * change that does not move it, a new process inside one boot that keeps it running, and a reboot that turns a running
 * stopwatch into a paused one counted up to the moment the device booted again.
 * zh-CN: 路线图 P7.2 (T118 - T120, Q28): 唯一的秒表: idle / running / paused, 计圈 (至多 100 圈), 改墙钟不影响, 同一次开机内的新进程保持运行, 重启后
 * 运行中的秒表转为暂停, 计到设备再次开机的时刻.
 */
class StopwatchTest {

    @get:Rule
    val temp = TemporaryFolder()

    private var now = START
    private var up = 10 * MINUTE
    private var bootCount: Int? = 3

    private val seen = ArrayList<StopwatchSnapshot>()

    private lateinit var stopwatch: Stopwatch

    @Before
    fun setUp() = newProcess()

    private fun newProcess() {
        seen.clear()
        stopwatch = Stopwatch(
            JsonDocumentFile(File(temp.root, Stopwatch.FILE_NAME), Stopwatch.FORMAT, Stopwatch.VERSION),
            clock = { now },
            elapsed = { up },
            boot = { bootCount },
        )
        stopwatch.addListener { seen += it }
    }

    private fun advance(millis: Long) {
        now += millis
        up += millis
    }

    private fun reboot(offMillis: Long, upMillis: Long = 5 * SECOND) {
        now += offMillis + upMillis
        up = upMillis
        bootCount = bootCount?.plus(1)
        newProcess()
    }

    @Test
    fun `the stopwatch starts idle, runs, laps, pauses, resumes and resets`() {
        assertEquals(StopwatchSnapshot(AlarmContract.STOPWATCH_IDLE, 0L, emptyList()), stopwatch.state())
        assertTrue("reading changes nothing", seen.isEmpty())
        assertTrue(!File(temp.root, Stopwatch.FILE_NAME).exists())

        assertEquals(AlarmContract.STOPWATCH_RUNNING, stopwatch.start().state)
        advance(5 * SECOND)
        assertEquals(5 * SECOND, stopwatch.state().elapsedMillis)
        val (afterLap, first) = stopwatch.lap()
        assertEquals(StopwatchLap(1, 5 * SECOND, 5 * SECOND), first)
        assertEquals(listOf(first), afterLap.laps)
        advance(3 * SECOND)
        assertEquals(StopwatchLap(2, 3 * SECOND, 8 * SECOND), stopwatch.lap().second)

        advance(2 * SECOND)
        val paused = stopwatch.pause()
        assertEquals(AlarmContract.STOPWATCH_PAUSED, paused.state)
        assertEquals(10 * SECOND, paused.elapsedMillis)
        advance(HOUR)
        assertEquals(10 * SECOND, stopwatch.state().elapsedMillis)
        assertEquals("pausing again changes nothing", paused, stopwatch.pause())
        assertAlarmError(INVALID_OPTIONS, "running") { stopwatch.lap() }

        val resumed = stopwatch.resume()
        assertEquals(AlarmContract.STOPWATCH_RUNNING, resumed.state)
        assertEquals(2, resumed.laps.size)
        advance(SECOND)
        assertEquals(11 * SECOND, stopwatch.state().elapsedMillis)
        assertEquals(StopwatchLap(3, 3 * SECOND, 11 * SECOND), stopwatch.lap().second)
        val count = seen.size
        stopwatch.start()
        assertEquals("starting a running stopwatch changes nothing", count, seen.size)

        assertEquals(StopwatchSnapshot(AlarmContract.STOPWATCH_IDLE, 0L, emptyList()), stopwatch.reset())
        assertAlarmError(INVALID_OPTIONS, "idle") { stopwatch.lap() }
        advance(MINUTE)
        stopwatch.start()
        advance(SECOND)
        assertEquals("a start from idle begins at zero", StopwatchSnapshot(AlarmContract.STOPWATCH_RUNNING, SECOND, emptyList()), stopwatch.state())
    }

    @Test
    fun `at most 100 laps (T118)`() {
        stopwatch.start()
        repeat(AlarmLimits.MAX_STOPWATCH_LAPS) {
            advance(SECOND)
            stopwatch.lap()
        }
        assertEquals(100, stopwatch.state().laps.size)
        assertEquals(StopwatchLap(100, SECOND, 100 * SECOND), stopwatch.state().laps.last())
        advance(SECOND)
        assertAlarmError(LIMIT_EXCEEDED, "100") { stopwatch.lap() }
        assertEquals(100, stopwatch.state().laps.size)
        assertEquals("the stopwatch keeps running", 101 * SECOND, stopwatch.state().elapsedMillis)
        assertEquals(0, stopwatch.reset().laps.size)
    }

    @Test
    fun `a wall-clock change does not move it`() {
        stopwatch.start()
        advance(10 * SECOND)
        now += HOUR
        assertEquals(10 * SECOND, stopwatch.state().elapsedMillis)
        now -= 2 * HOUR
        assertEquals(10 * SECOND, stopwatch.state().elapsedMillis)
    }

    @Test
    fun `a new process inside one boot keeps it running with its laps`() {
        stopwatch.start()
        advance(4 * SECOND)
        stopwatch.lap()
        advance(6 * SECOND)
        newProcess()
        val state = stopwatch.state()
        assertEquals(AlarmContract.STOPWATCH_RUNNING, state.state)
        assertEquals(10 * SECOND, state.elapsedMillis)
        assertEquals(listOf(StopwatchLap(1, 4 * SECOND, 4 * SECOND)), state.laps)

        stopwatch.pause()
        newProcess()
        assertEquals(StopwatchSnapshot(AlarmContract.STOPWATCH_PAUSED, 10 * SECOND, listOf(StopwatchLap(1, 4 * SECOND, 4 * SECOND))), stopwatch.state())
    }

    @Test
    fun `after a reboot a running stopwatch is paused at the moment the device booted again (T120, Q28)`() {
        stopwatch.start()
        advance(20 * SECOND)
        stopwatch.lap()
        advance(10 * SECOND)
        // Off for 15 s, then up for 5 s: it booted again 45 s after the start. zh-CN: 关机 15 s, 再开机 5 s: 再次开机时距开始 45 s.
        reboot(offMillis = 15 * SECOND)
        val state = stopwatch.state()
        assertEquals(AlarmContract.STOPWATCH_PAUSED, state.state)
        assertEquals(45 * SECOND, state.elapsedMillis)
        assertEquals(1, state.laps.size)
        assertEquals("the change is saved and told once", 1, seen.size)
        advance(MINUTE)
        assertEquals(45 * SECOND, stopwatch.state().elapsedMillis)
        newProcess()
        assertEquals(StopwatchSnapshot(AlarmContract.STOPWATCH_PAUSED, 45 * SECOND, state.laps), stopwatch.state())

        stopwatch.resume()
        advance(SECOND)
        assertEquals(46 * SECOND, stopwatch.state().elapsedMillis)
    }

    @Test
    fun `a clock set back before the reboot never makes it go back below what the plugin saw`() {
        stopwatch.start()
        advance(20 * SECOND)
        stopwatch.lap()
        now -= HOUR
        reboot(offMillis = 15 * SECOND)
        assertEquals(StopwatchSnapshot(AlarmContract.STOPWATCH_PAUSED, 20 * SECOND, listOf(StopwatchLap(1, 20 * SECOND, 20 * SECOND))), stopwatch.state())
    }

    @Test
    fun `without a boot count an elapsed clock behind the last seen moment means a reboot`() {
        bootCount = null
        newProcess()
        stopwatch.start()
        advance(30 * SECOND)
        stopwatch.lap()
        reboot(offMillis = 10 * SECOND, upMillis = 5 * SECOND)
        assertEquals(AlarmContract.STOPWATCH_PAUSED, stopwatch.state().state)
        assertEquals("30 s running and 10 s off", 40 * SECOND, stopwatch.state().elapsedMillis)
    }

    @Test
    fun `a missing, foreign or damaged file reads as idle`() {
        val file = File(temp.root, Stopwatch.FILE_NAME)
        file.writeText("{\"format\":\"other\",\"version\":1,\"state\":\"running\"}")
        newProcess()
        assertEquals(AlarmContract.STOPWATCH_IDLE, stopwatch.state().state)
        file.writeText("{\"format\":\"${Stopwatch.FORMAT}\",\"version\":${Stopwatch.VERSION},\"state\":\"running\",\"accumulated\":5}")
        newProcess()
        assertEquals("a running stopwatch without its start", AlarmContract.STOPWATCH_IDLE, stopwatch.state().state)
        file.writeText("{\"format\":\"${Stopwatch.FORMAT}\",\"version\":${Stopwatch.VERSION},\"state\":\"paused\",\"accumulated\":5000,\"laps\":[]}")
        newProcess()
        assertEquals(StopwatchSnapshot(AlarmContract.STOPWATCH_PAUSED, 5 * SECOND, emptyList()), stopwatch.state())
        file.writeText("garbage")
        newProcess()
        assertEquals(AlarmContract.STOPWATCH_IDLE, stopwatch.state().state)
    }

    private companion object {
        const val SECOND = 1_000L
        const val MINUTE = 60 * SECOND
        const val HOUR = 60 * MINUTE
        const val START = 1_793_000_000_000L
    }
}
