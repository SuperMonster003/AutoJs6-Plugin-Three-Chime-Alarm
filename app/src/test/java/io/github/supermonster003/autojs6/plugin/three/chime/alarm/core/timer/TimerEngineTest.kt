package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmOptionsValidator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.assertAlarmError
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner.Cause
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_OPTIONS
import org.autojs.plugin.alarm.api.AlarmErrorCodes.LIMIT_EXCEEDED
import org.autojs.plugin.alarm.api.AlarmErrorCodes.NOT_FOUND
import org.autojs.plugin.alarm.api.AlarmErrorCodes.PERMISSION_REQUIRED
import org.autojs.plugin.alarm.api.AlarmJson
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.TimerSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Roadmap P7.1 (T108 - T116): the timer state machine on its real store with a fake port and fake clocks: start,
 * pause, resume, addTime (negative too, the result within 1 s - 100 h), reset, cancel, the limit of 20, restoring in a
 * new process and after a reboot (the time the device was off counts), clock changes inside one boot, ends passed
 * while the process was gone (rung late within 5 minutes, missed after, T16) and the ring rounds.
 * zh-CN: 路线图 P7.1 (T108 - T116): 计时器状态机, 使用真实存储, 假端口与假时钟: 启动, 暂停, 继续, addTime (可为负, 结果须在 1 s - 100 h 内), 重置,
 * 取消, 20 个上限, 在新进程中与重启后恢复 (关机期间计入), 同一次开机内改系统时间, 进程不在时已过的到点时刻 (5 分钟内补响, 更晚算错过, T16), 以及响铃轮次.
 */
class TimerEngineTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val validator = AlarmOptionsValidator()

    /** Wall clock, elapsed-realtime clock and boot count. zh-CN: 墙钟, 开机时长时钟与开机次数. */
    private var now = START
    private var up = 10 * MINUTE
    private var bootCount: Int? = 7
    private var exact = true

    private val port = FakePort()
    private val changes = ArrayList<Pair<String, TimerRecord?>>()
    private var ids = 0

    private lateinit var engine: TimerEngine

    @Before
    fun setUp() = newProcess()

    /** A new process on the saved timers. zh-CN: 在已保存的计时器上启动新进程. */
    private fun newProcess() {
        changes.clear()
        engine = TimerEngine(
            store = TimerStore(File(temp.root, TimerStore.FILE_NAME)) { validator.timerOptions(it) },
            port = port,
            clock = { now },
            elapsed = { up },
            boot = { bootCount },
            idGenerator = { "t_test${ids++}" },
        )
        engine.addListener { id, record -> changes += id to record }
    }

    /** Both clocks move on by [millis]. zh-CN: 两个时钟同时前进 [millis]. */
    private fun advance(millis: Long) {
        now += millis
        up += millis
    }

    /** The device is off for [offMillis] and boots again; the elapsed clock starts over. zh-CN: 关机 [offMillis] 后重新开机, 开机时长时钟从头计. */
    private fun reboot(offMillis: Long) {
        now += offMillis
        up = 30 * SECOND
        bootCount = bootCount?.plus(1)
        newProcess()
    }

    private fun spec(label: String? = null): TimerSpec = validator.timerOptions(if (label == null) emptyMap() else mapOf("label" to label))

    private fun start(duration: Long = 10 * MINUTE, label: String? = null) = engine.start(duration, spec(label), AlarmContract.SOURCE_SCRIPT)

    private fun remaining(id: String) = engine.get(id)!!.toSnapshot(now).remainingMillis

    @Test
    fun `a started timer runs to its end and is armed`() {
        val timer = start(25 * MINUTE, "tea")
        assertEquals("t_test0", timer.id)
        assertEquals(AlarmContract.TIMER_RUNNING, timer.state)
        assertEquals(25 * MINUTE, timer.durationMillis)
        assertEquals(START + 25 * MINUTE, timer.endsAt)
        assertEquals(up + 25 * MINUTE, timer.endsElapsed)
        assertEquals(7, timer.boot)
        assertEquals(AlarmContract.SOURCE_SCRIPT, timer.source)
        assertEquals("tea", timer.spec.label)
        assertEquals(START + 25 * MINUTE, port.armed[timer.id])
        assertEquals(listOf(timer.id to timer), changes)

        advance(5 * MINUTE)
        val snapshot = engine.get(timer.id)!!.toSnapshot(now)
        assertEquals(20 * MINUTE, snapshot.remainingMillis)
        assertEquals(START + 25 * MINUTE, snapshot.endsAt)
        assertEquals(listOf(timer), engine.list())
        assertTrue(engine.active())
    }

    @Test
    fun `pause keeps the time left and resume runs it from now`() {
        val timer = start()
        advance(4 * MINUTE)
        val paused = engine.pause(timer.id)
        assertEquals(AlarmContract.TIMER_PAUSED, paused.state)
        assertEquals(6 * MINUTE, paused.remainingMillis)
        assertNull(paused.endsAt)
        assertNull("a paused timer has no wake-up", port.armed[timer.id])
        assertFalse(engine.active())
        assertEquals("pausing again changes nothing", paused, engine.pause(timer.id))

        advance(HOUR)
        assertEquals(6 * MINUTE, remaining(timer.id))
        val resumed = engine.resume(timer.id)
        assertEquals(AlarmContract.TIMER_RUNNING, resumed.state)
        assertEquals(now + 6 * MINUTE, resumed.endsAt)
        assertEquals(now + 6 * MINUTE, port.armed[timer.id])
        assertEquals("resuming a running timer changes nothing", resumed, engine.resume(timer.id))
        assertEquals("the start duration stays", 10 * MINUTE, resumed.durationMillis)
    }

    @Test
    fun `addTime may be negative and the time left must stay within 1 s - 100 h (T112)`() {
        val timer = start(10 * MINUTE)
        advance(MINUTE)
        assertEquals(now + 10 * MINUTE, engine.addTime(timer.id, MINUTE).endsAt)
        assertEquals(now + 7 * MINUTE, engine.addTime(timer.id, -3 * MINUTE).endsAt)
        assertEquals(now + 7 * MINUTE, port.armed[timer.id])
        assertEquals("addTime leaves the start duration", 10 * MINUTE, engine.get(timer.id)!!.durationMillis)

        assertAlarmError(INVALID_OPTIONS, "the time left") { engine.addTime(timer.id, -7 * MINUTE) }
        assertAlarmError(INVALID_OPTIONS) { engine.addTime(timer.id, -7 * MINUTE + 999) }
        assertEquals(SECOND, remaining(engine.addTime(timer.id, -7 * MINUTE + SECOND).id))
        assertAlarmError(INVALID_OPTIONS, "1 s - 100 h") { engine.addTime(timer.id, 100 * HOUR) }
        assertAlarmError(INVALID_OPTIONS) { engine.addTime(timer.id, Long.MAX_VALUE) }
        assertAlarmError(INVALID_OPTIONS) { engine.addTime(timer.id, Long.MIN_VALUE) }
        assertEquals("a refused addTime changes nothing", SECOND, remaining(timer.id))
        assertEquals(100 * HOUR, remaining(engine.addTime(timer.id, 100 * HOUR - SECOND).id))

        val paused = engine.pause(timer.id)
        val shorter = engine.addTime(timer.id, -HOUR)
        assertEquals(AlarmContract.TIMER_PAUSED, shorter.state)
        assertEquals(paused.remainingMillis - HOUR, shorter.remainingMillis)
        assertNull(shorter.endsAt)
    }

    @Test
    fun `reset goes back to the start duration, paused`() {
        val timer = start(10 * MINUTE)
        advance(3 * MINUTE)
        engine.addTime(timer.id, 5 * MINUTE)
        val reset = engine.reset(timer.id)
        assertEquals(AlarmContract.TIMER_PAUSED, reset.state)
        assertEquals(10 * MINUTE, reset.remainingMillis)
        assertNull(reset.endsAt)
        assertNull(port.armed[timer.id])
        assertEquals(now + 10 * MINUTE, engine.resume(timer.id).endsAt)
    }

    @Test
    fun `cancel removes a timer without an event and the host bindings are cleaned up (T113)`() {
        val timer = start()
        assertTrue(engine.cancel(timer.id))
        assertNull(engine.get(timer.id))
        assertTrue(engine.list().isEmpty())
        assertNull(port.armed[timer.id])
        assertEquals(listOf(timer.id), port.cancelled)
        assertEquals(listOf(timer.id), port.silent)
        assertEquals(timer.id to null, changes.last())
        assertFalse("an unknown id answers false", engine.cancel(timer.id))
        assertAlarmError(NOT_FOUND) { engine.pause(timer.id) }
        assertAlarmError(NOT_FOUND) { engine.resume("t_none") }
        assertAlarmError(NOT_FOUND) { engine.addTime("t_none", SECOND) }
        assertAlarmError(NOT_FOUND) { engine.reset("t_none") }
    }

    @Test
    fun `at most 20 timers, a duration of 1 s - 100 h and the exact alarm permission`() {
        repeat(AlarmLimits.MAX_TIMERS) { start(SECOND + it) }
        assertEquals(20, engine.list().size)
        assertAlarmError(LIMIT_EXCEEDED, "20") { start() }
        engine.cancel(engine.list().first().id)
        start(100 * HOUR)
        assertEquals(20, engine.list().size)
        engine.list().take(2).forEach { engine.cancel(it.id) }

        assertAlarmError(INVALID_OPTIONS, "1 s - 100 h") { start(999) }
        assertAlarmError(INVALID_OPTIONS) { start(100 * HOUR + 1) }
        val paused = engine.pause(start().id)
        exact = false
        assertAlarmError(PERMISSION_REQUIRED) { start() }
        assertAlarmError(PERMISSION_REQUIRED) { engine.resume(paused.id) }
        assertEquals("pausing and resetting need no permission", AlarmContract.TIMER_PAUSED, engine.reset(paused.id).state)
        assertEquals(19, engine.list().size)
    }

    @Test
    fun `a new process restores the timers and arms them again`() {
        val running = start(10 * MINUTE, "pasta")
        val paused = engine.pause(start(20 * MINUTE).id)
        advance(2 * MINUTE)
        port.armed.clear()
        newProcess()
        assertEquals(listOf(running.id, paused.id), engine.list().map { it.id })
        assertEquals("pasta", engine.get(running.id)!!.spec.label)
        assertEquals(0, engine.reconcile(Cause.RESTORE))
        assertEquals(running.endsAt, port.armed[running.id])
        assertNull(port.armed[paused.id])
        assertEquals(8 * MINUTE, remaining(running.id))
        assertEquals(20 * MINUTE, remaining(paused.id))
    }

    @Test
    fun `after a reboot the time the device was off counts`() {
        val timer = start(10 * MINUTE)
        val paused = engine.pause(start(10 * MINUTE).id)
        advance(2 * MINUTE)
        reboot(offMillis = 3 * MINUTE)
        assertEquals(0, engine.reconcile(Cause.BOOT))
        val restored = engine.get(timer.id)!!
        assertEquals("the wall-clock end stays", START + 10 * MINUTE, restored.endsAt)
        assertEquals(5 * MINUTE, remaining(timer.id))
        assertEquals("the end moves to the new boot's clock", up + 5 * MINUTE, restored.endsElapsed)
        assertEquals(8, restored.boot)
        assertEquals(START + 10 * MINUTE, port.armed[timer.id])
        assertEquals("a paused timer keeps its time", 10 * MINUTE, remaining(paused.id))

        advance(5 * MINUTE)
        assertEquals(1, engine.reconcile(Cause.TRIGGER))
        assertEquals(AlarmContract.TIMER_RINGING, engine.get(timer.id)!!.state)
    }

    @Test
    fun `a clock change inside one boot does not move the end (T110)`() {
        val timer = start(10 * MINUTE)
        advance(MINUTE)
        now += HOUR
        assertEquals("nothing rings although the wall clock passed the end", 0, engine.reconcile(Cause.CLOCK))
        assertEquals(now + 9 * MINUTE, engine.get(timer.id)!!.endsAt)
        assertEquals(now + 9 * MINUTE, port.armed[timer.id])
        now -= 2 * HOUR
        engine.reconcile(Cause.CLOCK)
        assertEquals(9 * MINUTE, remaining(timer.id))

        // Without a boot count the wall clock decides. zh-CN: 没有开机次数时以墙钟为准.
        bootCount = null
        newProcess()
        engine.reconcile(Cause.RESTORE)
        now += 10 * MINUTE
        assertEquals(1, engine.reconcile(Cause.CLOCK))
    }

    @Test
    fun `an end passed while the process was gone rings late within 5 minutes and is missed after (T16, T115)`() {
        val late = start(10 * MINUTE, "late")
        val missed = start(5 * MINUTE, "missed")
        val endLate = late.endsAt!!
        val endMissed = missed.endsAt!!
        reboot(offMillis = 14 * MINUTE)
        assertEquals(1, engine.reconcile(Cause.BOOT))

        val ring = port.rings.single()
        assertEquals(late.id, ring.first.id)
        assertEquals(AlarmPlanner.RingRequest(late.id, endLate, now, "${late.id}@$endLate", 0), ring.second)
        val ringing = engine.get(late.id)!!
        assertEquals(AlarmContract.TIMER_RINGING, ringing.state)
        assertEquals(endLate, ringing.ringingAt)
        assertEquals(now, ringing.ringSince)
        assertEquals(0L, ringing.toSnapshot(now).remainingMillis)
        assertNull(ringing.toSnapshot(now).endsAt)

        assertNull("9 minutes past the end: missed", engine.get(missed.id))
        assertEquals(listOf(missed.id to endMissed), port.missed.map { it.first.id to it.second })
        assertEquals(listOf(missed.id), port.silent)
        assertEquals(1, engine.list().size)
    }

    @Test
    fun `the trigger wake-up rings a timer up to 2 s early and the rest are armed again`() {
        val soon = start(MINUTE)
        val later = start(HOUR)
        advance(MINUTE - AlarmPlanner.EARLY_TOLERANCE_MILLIS)
        assertEquals(1, engine.reconcile(Cause.TRIGGER))
        assertEquals(soon.id, port.rings.single().first.id)
        assertEquals(later.endsAt, port.armed[later.id])
        assertEquals("the trigger tolerance is for TRIGGER only", 0, engine.reconcile(Cause.CLOCK))
    }

    @Test
    fun `a ringing timer snoozes, rings again as a new round and ends when its round ends`() {
        val timer = start(MINUTE)
        advance(MINUTE)
        engine.reconcile(Cause.TRIGGER)
        val end = timer.endsAt!!
        assertEquals(listOf(timer.id), engine.interrupted().map { it.id })
        assertAlarmError(INVALID_OPTIONS, "ringing") { engine.pause(timer.id) }
        assertAlarmError(INVALID_OPTIONS, "ringing") { engine.addTime(timer.id, MINUTE) }
        assertAlarmError(INVALID_OPTIONS, "ringing") { engine.reset(timer.id) }
        assertAlarmError(INVALID_OPTIONS, "ringing") { engine.cancel(timer.id) }

        assertTrue(engine.canSnooze())
        assertTrue(engine.snooze(timer.id, now + 5 * MINUTE, 1))
        assertEquals(now + 5 * MINUTE, port.armed[timer.id])
        assertEquals(listOf(timer.id), engine.snoozed(null).map { it.id })
        assertTrue(engine.snoozed("t_other").isEmpty())
        assertTrue(engine.interrupted().isEmpty())
        assertTrue("a waiting snooze keeps the service alive (T117)", engine.active())

        advance(5 * MINUTE)
        assertEquals(1, engine.reconcile(Cause.TRIGGER))
        val again = port.rings.last()
        assertEquals("${timer.id}@$end~1", again.second.occurrence)
        assertEquals(1, again.second.snoozeCount)
        assertNull(engine.get(timer.id)!!.snoozeUntil)

        exact = false
        assertFalse("no snooze without the exact alarm permission", engine.snooze(timer.id, now + MINUTE, 2))
        assertFalse(engine.snooze("t_none", now + MINUTE, 1))

        engine.end(timer.id)
        assertNull(engine.get(timer.id))
        assertTrue("an ending with an event leaves the bindings to the event", port.silent.isEmpty())
        engine.end(timer.id)
    }

    @Test
    fun `a snooze passed long ago ends the timer without a ring`() {
        val timer = start(MINUTE)
        advance(MINUTE)
        engine.reconcile(Cause.TRIGGER)
        engine.snooze(timer.id, now + 5 * MINUTE, 1)
        reboot(offMillis = HOUR)
        assertEquals(0, engine.reconcile(Cause.BOOT))
        assertNull(engine.get(timer.id))
        assertEquals(listOf(timer.id), port.silent)
    }

    @Test
    fun `a silent end cleans up the bindings`() {
        val timer = start(MINUTE)
        advance(MINUTE)
        engine.reconcile(Cause.TRIGGER)
        engine.end(timer.id, silent = true)
        assertEquals(listOf(timer.id), port.silent)
    }

    @Test
    fun `a damaged entry is skipped and the others load`() {
        val good = start(10 * MINUTE)
        val file = File(temp.root, TimerStore.FILE_NAME)
        val document = requireNotNull(AlarmJson.parse(file.readText())).toMutableMap()
        document["timers"] = listOf(mapOf("id" to "t_broken", "state" to "running"), mapOf("id" to "BAD")) + (document["timers"] as List<*>)
        file.writeText(AlarmJson.write(document))
        newProcess()
        assertEquals(listOf(good.id), engine.list().map { it.id })

        file.writeText("not json")
        newProcess()
        assertTrue("a corrupt file starts empty", engine.list().isEmpty())
        assertEquals("t_test1", start().id)
    }

    @Test
    fun `ids are t_ plus ten lower-case letters or digits`() {
        val id = TimerEngine.randomId()
        assertTrue(id, Regex("t_[0-9a-z]{10}").matches(id))
        assertTrue(TimerRecord.isValidId(id))
        assertFalse(TimerRecord.isValidId("a_alarm"))
        assertFalse(TimerRecord.isValidId("t_UPPER"))
    }

    private inner class FakePort : TimerPort {
        val armed = HashMap<String, Long?>()
        val cancelled = ArrayList<String>()
        val rings = ArrayList<Pair<TimerRecord, AlarmPlanner.RingRequest>>()
        val missed = ArrayList<Pair<TimerRecord, Long>>()
        val silent = ArrayList<String>()

        override fun canScheduleExact() = exact

        override fun schedule(record: TimerRecord) {
            armed[record.id] = if (record.running) record.endsAt else record.snoozeUntil
        }

        override fun cancel(timerId: String) {
            armed.remove(timerId)
            cancelled += timerId
        }

        override fun ring(record: TimerRecord, request: AlarmPlanner.RingRequest) {
            rings += record to request
        }

        override fun notifyMissed(record: TimerRecord, scheduledAt: Long) {
            missed += record to scheduledAt
        }

        override fun removedSilently(timerId: String) {
            silent += timerId
        }
    }

    private companion object {
        const val SECOND = 1_000L
        const val MINUTE = 60 * SECOND
        const val HOUR = 60 * MINUTE
        const val START = 1_793_000_000_000L
    }
}
