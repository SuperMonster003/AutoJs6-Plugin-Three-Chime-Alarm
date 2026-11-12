package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.assertAlarmError
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.optionsOf
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.AlarmEventBus
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner.Cause
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedule
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedulePort
import org.autojs.plugin.alarm.api.AlarmContract.BY_SCRIPT
import org.autojs.plugin.alarm.api.AlarmContract.BY_TOOL
import org.autojs.plugin.alarm.api.AlarmContract.BY_USER
import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_OPTIONS
import org.autojs.plugin.alarm.api.AlarmEvent
import org.autojs.plugin.alarm.api.RingingSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Roadmap P2.4 (T64 - T67): the ring state machine on top of the real repository and scheduler: snoozes and their
 * limits, the queue of alarms due together, dismiss, auto silence, alarms removed while ringing and rounds left by a
 * dead process.
 * zh-CN: 路线图 P2.4 (T64 - T67): 基于真实仓库与调度器的响铃状态机: 贪睡及其限制, 同时到点的闹钟排队, 关闭, 自动静音, 响铃中被删除的闹钟,
 * 进程死亡时留下的轮次.
 */
class RingCoordinatorTest {

    @get:Rule
    val temp = TemporaryFolder()

    private var now = at("2026-11-05T08:00")
    private var exact = true
    private var defaults = RingDefaults.PROTOCOL

    private val events = ArrayList<AlarmEvent>()
    private var notified = 0
    private val wakes = HashMap<String, Long?>()

    private lateinit var repository: AlarmRepository
    private lateinit var schedule: AlarmSchedule
    private lateinit var rings: RingCoordinator

    @Before
    fun setUp() = boot()

    /** Builds the plugin's objects on the saved alarms, as a new process does. zh-CN: 像新进程一样在已存闹钟上建立插件对象. */
    private fun boot() {
        events.clear()
        lateinit var created: AlarmSchedule
        repository = AlarmRepository(
            store = AlarmStore(File(temp.root, "alarms"), clock = { now }),
            clock = { now },
            deviceZone = { SHANGHAI },
            planner = { created.plan(it) },
            schedulingAllowed = { exact },
        )
        val bus = AlarmEventBus().also { it.addListener(events::add) }
        created = AlarmSchedule(repository, Port(), bus, clock = { now }, deviceZone = { SHANGHAI })
        schedule = created
        rings = RingCoordinator(repository, schedule, bus, clock = { now }, defaults = { defaults })
        rings.addListener { notified++ }
    }

    /** A new process: the plugin start reconciles first (T8), then picks up the rounds. zh-CN: 新进程: 插件启动先对账 (T8), 再接续轮次. */
    private fun restart() {
        boot()
        schedule.reconcile(Cause.RESTORE)
        assertTrue(take().isEmpty())
    }

    private fun daily(hour: Int, vararg extra: Pair<String, Any?>): AlarmRecord =
        repository.add(optionsOf(hour, 0, "repeat" to "FREQ=DAILY", *extra), "script")

    private fun fire(vararg alarms: AlarmRecord) {
        schedule.reconcile(Cause.TRIGGER, alarms.map { it.id })
    }

    private fun state(alarm: AlarmRecord) = repository.get(alarm.id)!!.state

    private fun take(): List<AlarmEvent> = events.toList().also { events.clear() }

    @Test
    fun `a ring takes the settings defaults of its time for the options it leaves out (T84)`() {
        val alarm = daily(7)
        defaults = RingDefaults(snoozeMinutes = 4, snoozeMax = 1, autoSilenceMinutes = 3)
        val t = at("2026-11-06T07:00")
        now = t
        fire(alarm)
        take()
        assertEquals(t + 3 * MINUTE, rings.sounding?.silenceAt)

        now = t + 30 * SECOND
        assertTrue(rings.snooze(null, null, BY_USER))
        assertEquals(4, take().single().minutes)
        now += 4 * MINUTE
        fire(alarm)
        take()
        // snoozeMax 1: the second round may not snooze again. zh-CN: snoozeMax 为 1: 第二轮不能再贪睡.
        assertFalse(rings.snooze(null, null, BY_USER))
    }

    @Test
    fun `a ring snoozes and rings again as a new round of the same instance`() {
        val alarm = daily(7, "label" to "wake")
        val t = at("2026-11-06T07:00")
        now = t
        fire(alarm)
        val ring = take().single()
        assertEquals("ring", ring.type)
        assertEquals("${alarm.id}@$t", ring.occurrence)
        assertEquals(t, ring.scheduledAt)
        assertEquals(t, ring.firedAt)
        assertEquals(0, ring.snoozeCount)
        assertEquals("wake", ring.label)
        assertEquals(alarm.id, rings.sounding?.alarmId)
        assertEquals(t + 10 * MINUTE, rings.sounding?.silenceAt)
        assertEquals(t, state(alarm).ringingAt)
        assertEquals(listOf(RingingSnapshot(alarm.id, "alarm", "wake", t, false, 0)), rings.ringing())
        assertEquals(1, notified)

        now = t + 30 * SECOND
        assertTrue(rings.snooze(null, null, BY_USER))
        val snooze = take().single()
        assertEquals("snooze", snooze.type)
        assertEquals(BY_USER, snooze.by)
        assertEquals(10, snooze.minutes)
        assertEquals(1, snooze.snoozeCount)
        assertEquals("${alarm.id}@$t", snooze.occurrence)
        assertNull(rings.sounding)
        val until = now + 10 * MINUTE
        assertEquals(until, state(alarm).snoozeUntil)
        assertNull(state(alarm).ringingAt)
        assertEquals(until, wakes[alarm.id])
        assertEquals(listOf(RingingSnapshot(alarm.id, "alarm", "wake", t, true, 1)), rings.ringing())

        now = until
        fire(alarm)
        val again = take().single()
        assertEquals("ring", again.type)
        assertEquals("${alarm.id}@$t~1", again.occurrence)
        assertEquals(until, again.scheduledAt)
        assertEquals(1, again.snoozeCount)
        assertEquals(listOf(RingingSnapshot(alarm.id, "alarm", "wake", t, false, 1)), rings.ringing())

        assertTrue(rings.dismiss(alarm.id, BY_SCRIPT))
        val dismiss = take().single()
        assertEquals("dismiss", dismiss.type)
        assertEquals(BY_SCRIPT, dismiss.by)
        assertEquals("${alarm.id}@$t~1", dismiss.occurrence)
        val ended = state(alarm)
        assertNull(ended.snoozeUntil)
        assertEquals(0, ended.snoozeCount)
        assertNull(ended.ringSince)
        assertNull(ended.ringingAt)
        assertEquals(at("2026-11-07T07:00"), wakes[alarm.id])
        assertTrue(rings.ringing().isEmpty())
        assertFalse(rings.dismiss(null, BY_USER))
    }

    @Test
    fun `snoozes stop at the limit, without snoozing and without the exact alarm permission`() {
        val limited = daily(7, "snooze" to mapOf("minutes" to 5, "max" to 1))
        val never = daily(7, "snooze" to mapOf("enabled" to false))
        val t = at("2026-11-06T07:00")
        now = t
        fire(limited, never)
        assertEquals(listOf("ring"), take().map { it.type })

        assertAlarmError(INVALID_OPTIONS, "snooze minutes") { rings.snooze(null, 0, BY_SCRIPT) }
        assertAlarmError(INVALID_OPTIONS, "snooze minutes") { rings.snooze(null, 61, BY_SCRIPT) }
        assertFalse(rings.snooze(never.id, null, BY_SCRIPT))
        exact = false
        assertFalse(rings.snooze(limited.id, null, BY_SCRIPT))
        exact = true
        assertTrue(take().isEmpty())

        assertTrue(rings.snooze(limited.id, null, BY_SCRIPT))
        assertEquals(listOf("snooze" to limited.id, "ring" to never.id), take().map { it.type to it.alarmId })
        assertEquals(t + 5 * MINUTE, state(limited).snoozeUntil)
        // A snoozed alarm is not snoozed again. zh-CN: 贪睡中的闹钟不会再次贪睡.
        assertFalse(rings.snooze(limited.id, 7, BY_SCRIPT))
        assertTrue(rings.dismiss(never.id, BY_USER))
        take()

        now = t + 5 * MINUTE
        fire(limited)
        assertEquals(1, take().single().snoozeCount)
        assertFalse(rings.snooze(null, null, BY_USER))
        assertEquals(limited.id, rings.sounding?.alarmId)
        assertFalse(rings.sounding!!.canSnooze)
    }

    @Test
    fun `alarms due together ring one at a time and dismiss without an id covers them all`() {
        val early = daily(6, "snooze" to mapOf("minutes" to 60))
        now = at("2026-11-06T06:00")
        fire(early)
        now = at("2026-11-06T06:30")
        rings.snooze(early.id, null, BY_USER)
        val first = daily(7, "label" to "first")
        val second = daily(7, "label" to "second")
        take()

        val t = at("2026-11-06T07:00")
        now = t
        fire(first, second)
        assertEquals(listOf("ring" to first.id), take().map { it.type to it.alarmId })
        assertEquals(listOf(second.id), rings.queued.map { it.alarmId })
        assertEquals(t, state(second).ringingAt)
        assertEquals(listOf(first.id to false, second.id to false, early.id to true), rings.ringing().map { it.id to it.snoozed })

        now = t + MINUTE
        assertTrue(rings.dismiss(first.id, BY_USER))
        val (dismiss, ring) = take()
        assertEquals("dismiss" to first.id, dismiss.type to dismiss.alarmId)
        assertEquals("ring" to second.id, ring.type to ring.alarmId)
        assertEquals(t, ring.scheduledAt)
        assertEquals(now, ring.firedAt)
        assertEquals(now + 10 * MINUTE, rings.sounding?.silenceAt)

        assertTrue(rings.dismiss(null, BY_TOOL))
        val dismissed = take()
        assertEquals(listOf(second.id, early.id), dismissed.map { it.alarmId })
        assertTrue(dismissed.all { it.type == "dismiss" && it.by == BY_TOOL })
        val snoozed = dismissed.last()
        assertEquals("${early.id}@${at("2026-11-06T06:00")}", snoozed.occurrence)
        assertEquals(at("2026-11-06T07:30"), snoozed.scheduledAt)
        assertNull(state(early).snoozeUntil)
        assertEquals(at("2026-11-07T06:00"), wakes[early.id])
        assertTrue(rings.ringing().isEmpty())
        assertNull(rings.sounding)
    }

    @Test
    fun `auto silence ends the sounding round and retires a one-shot alarm`() {
        val alarm = repository.add(optionsOf(9, 0, "autoSilenceMinutes" to 1), "user")
        val t = at("2026-11-05T09:00")
        now = t
        fire(alarm)
        take()
        val sounding = rings.sounding!!
        assertEquals(t + MINUTE, sounding.silenceAt)
        assertTrue(repository.get(alarm.id)!!.enabled)

        assertFalse(rings.silence("${alarm.id}@0"))
        now = t + MINUTE + 1_500
        assertTrue(rings.silence(sounding.occurrence))
        val silence = take().single()
        assertEquals("silence", silence.type)
        assertEquals(MINUTE + 1_500, silence.ringMillis)
        assertEquals(sounding.occurrence, silence.occurrence)
        val retired = repository.get(alarm.id)!!
        assertFalse(retired.enabled)
        assertNull(retired.state.ringingAt)
        assertFalse(rings.silence(sounding.occurrence))
    }

    @Test
    fun `an alarm removed or disabled while ringing stops with dismiss by the source`() {
        val removed = daily(7)
        val disabled = daily(7)
        val relabeled = daily(7)
        val snoozed = daily(6)
        now = at("2026-11-06T06:00")
        fire(snoozed)
        rings.snooze(snoozed.id, null, BY_USER)
        now = at("2026-11-06T07:00")
        fire(removed, disabled, relabeled)
        take()

        repository.update(relabeled.id, mapOf("label" to "new"), "user")
        assertEquals("new", rings.queued.single { it.alarmId == relabeled.id }.label)
        assertTrue(take().isEmpty())

        assertTrue(repository.remove(removed.id, "intent"))
        val (dismiss, ring) = take()
        assertEquals(Triple("dismiss", removed.id, BY_USER), Triple(dismiss.type, dismiss.alarmId, dismiss.by))
        assertEquals("ring" to disabled.id, ring.type to ring.alarmId)

        repository.setEnabled(disabled.id, false, "tool")
        assertEquals(listOf(Triple("dismiss", disabled.id, BY_TOOL), Triple("ring", relabeled.id, null)), take().map { Triple(it.type, it.alarmId, it.by) })
        assertNull(state(disabled).ringingAt)

        repository.setEnabled(snoozed.id, false, "script")
        val stopped = take().single()
        assertEquals(Triple("dismiss", snoozed.id, BY_SCRIPT), Triple(stopped.type, stopped.alarmId, stopped.by))
        assertEquals("${snoozed.id}@${at("2026-11-06T06:00")}", stopped.occurrence)
        assertEquals(listOf(relabeled.id), rings.ringing().map { it.id })
        assertEquals("new", rings.ringing().single().label)
    }

    @Test
    fun `a round left by a dead process rings again within its auto silence time and ends silently after it`() {
        val daily = daily(9)
        val oneShot = repository.add(optionsOf(9, 0), "user")
        val t = at("2026-11-05T09:00")
        now = t
        fire(daily, oneShot)
        val rang = take().single()

        now = t + 3 * MINUTE
        restart()
        assertEquals(2, rings.resume())
        val resumed = take().single()
        assertEquals(rang.occurrence, resumed.occurrence)
        assertEquals(t, resumed.scheduledAt)
        assertEquals(now, resumed.firedAt)
        assertEquals(daily.id, rings.sounding?.alarmId)
        assertEquals(now + 10 * MINUTE, rings.sounding?.silenceAt)
        assertEquals(listOf(oneShot.id), rings.queued.map { it.alarmId })
        assertEquals(0, rings.resume())

        now = t + 11 * MINUTE
        restart()
        assertEquals(0, rings.resume())
        assertTrue(take().isEmpty())
        assertNull(state(daily).ringingAt)
        assertNull(state(daily).ringSince)
        assertTrue(repository.get(daily.id)!!.enabled)
        assertFalse(repository.get(oneShot.id)!!.enabled)
    }

    @Test
    fun `a missed snooze round is resumed from its own snooze`() {
        val alarm = daily(7)
        val t = at("2026-11-06T07:00")
        now = t
        fire(alarm)
        rings.snooze(alarm.id, null, BY_USER)
        now = t + 10 * MINUTE
        fire(alarm)
        take()

        now += MINUTE
        restart()
        assertEquals(1, rings.resume())
        val resumed = take().single()
        assertEquals("${alarm.id}@$t~1", resumed.occurrence)
        assertEquals(1, resumed.snoozeCount)
        assertEquals(t, rings.ringing().single().since)
    }

    private inner class Port : AlarmSchedulePort {
        override fun canScheduleExact() = exact

        override fun schedule(record: AlarmRecord) {
            wakes[record.id] = record.state.wakeAt.takeIf { record.enabled }
        }

        override fun cancel(alarmId: String) {
            wakes[alarmId] = null
        }

        override fun ring(record: AlarmRecord, request: AlarmPlanner.RingRequest) = rings.offer(record, request)

        override fun notifyMissed(record: AlarmRecord, event: AlarmEvent) = Unit
    }

    private companion object {
        val SHANGHAI: ZoneId = ZoneId.of("Asia/Shanghai")
        const val SECOND = 1_000L
        const val MINUTE = 60 * SECOND

        fun at(text: String): Long = LocalDateTime.parse(text).atZone(SHANGHAI).toInstant().toEpochMilli()
    }
}
