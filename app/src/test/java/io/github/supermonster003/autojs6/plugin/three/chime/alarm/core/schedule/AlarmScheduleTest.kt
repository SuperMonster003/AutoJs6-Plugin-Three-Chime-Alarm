package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmChange
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.assertAlarmError
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.optionsOf
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.AlarmEventBus
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner.Cause
import org.autojs.plugin.alarm.api.AlarmErrorCodes.PERMISSION_REQUIRED
import org.autojs.plugin.alarm.api.AlarmEvent
import org.autojs.plugin.alarm.api.AlarmLimits
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
 * Roadmap P2.3 (T3, T8, T16, T55 - T58): only the next ring is planned, the 5 minute catch-up window, `miss` reasons,
 * clock and time zone changes, `skipNext` / `cancelSkip`, snoozes, one-shot retirement and the exact alarm permission.
 * zh-CN: 路线图 P2.3 (T3, T8, T16, T55 - T58): 只计划下一次, 5 分钟补响窗口, `miss` 原因, 时钟与时区改变, `skipNext` / `cancelSkip`, 贪睡,
 * 一次性闹钟退役与精确闹钟权限.
 */
class AlarmScheduleTest {

    @get:Rule
    val temp = TemporaryFolder()

    private var now = at("2026-11-05T08:00")
    private var zone: ZoneId = SHANGHAI
    private var exact = true

    private val port = FakePort()
    private val events = ArrayList<AlarmEvent>()
    private val changes = ArrayList<AlarmChange>()

    private lateinit var repository: AlarmRepository
    private lateinit var schedule: AlarmSchedule

    @Before
    fun setUp() {
        lateinit var created: AlarmSchedule
        repository = AlarmRepository(
            store = AlarmStore(File(temp.root, "alarms"), clock = { now }),
            clock = { now },
            deviceZone = { zone },
            planner = { created.plan(it) },
            schedulingAllowed = { exact },
        )
        created = AlarmSchedule(repository, port, AlarmEventBus().also { it.addListener(events::add) }, clock = { now }, deviceZone = { zone })
        schedule = created
        repository.addListener(changes::add)
    }

    private fun daily(hour: Int = 7, vararg extra: Pair<String, Any?>): AlarmRecord =
        repository.add(optionsOf(hour, 0, "repeat" to "FREQ=DAILY", *extra), "script")

    private fun state(id: String) = repository.get(id)!!.state

    @Test
    fun `only the next ring is planned and a ring plans the one after`() {
        val alarm = daily()
        assertEquals(at("2026-11-06T07:00"), alarm.state.nextTrigger)
        assertEquals(at("2026-11-06T07:00"), port.wakes[alarm.id])

        now = at("2026-11-06T07:00")
        val outcome = schedule.reconcile(Cause.TRIGGER, listOf(alarm.id)).getValue(alarm.id)
        val ring = outcome.ring!!
        assertEquals(at("2026-11-06T07:00"), ring.scheduledAt)
        assertEquals("${alarm.id}@${at("2026-11-06T07:00")}", ring.occurrence)
        assertEquals(0, ring.snoozeCount)
        assertEquals(listOf(ring), port.rings)
        assertEquals(at("2026-11-06T07:00"), state(alarm.id).handledThrough)
        assertEquals(at("2026-11-07T07:00"), state(alarm.id).nextTrigger)
        assertEquals(at("2026-11-07T07:00"), port.wakes[alarm.id])

        // The same broadcast again (a restarted process) rings nothing. zh-CN: 同一广播再来一次 (进程重启后) 不再响铃.
        schedule.reconcile(Cause.TRIGGER, listOf(alarm.id))
        assertEquals(1, port.rings.size)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `a ring up to 5 minutes late still rings, a later one is missed`() {
        val onTime = daily()
        val late = daily()
        val off = daily()
        val t = at("2026-11-06T07:00")

        now = t + AlarmLimits.MISSED_RING_WINDOW_MILLIS
        assertEquals(t, schedule.reconcile(Cause.TRIGGER, listOf(onTime.id)).getValue(onTime.id).ring?.scheduledAt)

        now = t + AlarmLimits.MISSED_RING_WINDOW_MILLIS + 1
        val missed = schedule.reconcile(Cause.TRIGGER, listOf(late.id)).getValue(late.id)
        assertNull(missed.ring)
        val event = missed.missed!!
        assertEquals("miss", event.type)
        assertEquals("late", event.reason)
        assertEquals(t, event.scheduledAt)
        assertEquals(now, event.firedAt)
        assertEquals("${late.id}@$t", event.occurrence)
        assertEquals(listOf(late.id to event), port.missed)
        assertEquals(at("2026-11-07T07:00"), state(late.id).nextTrigger)

        // Off for three days: each alarm gives one `miss` with reason `off` for its planned ring (the rings after it are
        // not listed), then the next ring from now. zh-CN: 关机三天: 每个闹钟对其计划的那次发一个原因为 `off` 的 `miss` (之后的几次不逐一列出),
        // 然后从现在起计划下一次.
        now = at("2026-11-09T12:00")
        val boot = schedule.reconcile(Cause.BOOT)
        assertEquals(listOf("off", "off", "off"), boot.values.map { it.missed?.reason })
        assertEquals(t, boot.getValue(off.id).missed?.scheduledAt)
        assertEquals(at("2026-11-07T07:00"), boot.getValue(onTime.id).missed?.scheduledAt)
        assertEquals(at("2026-11-10T07:00"), state(off.id).nextTrigger)
        assertEquals(4, events.count { it.type == "miss" })
        assertEquals(4, port.missed.size)
        assertEquals(1, port.rings.size)
    }

    @Test
    fun `a one-shot alarm is retired when it is missed, not when it rings`() {
        val missed = repository.add(optionsOf(9, 0), "user")
        val rang = repository.add(optionsOf(9, 0), "user")
        assertEquals(at("2026-11-05T09:00"), missed.state.nextTrigger)
        val changesBefore = changes.size

        now = at("2026-11-05T09:00")
        assertEquals(at("2026-11-05T09:00"), schedule.reconcile(Cause.TRIGGER, listOf(rang.id)).getValue(rang.id).ring?.scheduledAt)
        val afterRing = repository.get(rang.id)!!
        assertTrue(afterRing.enabled)
        assertNull(afterRing.state.nextTrigger)
        assertNull(port.wakes[rang.id])

        now = at("2026-11-05T10:00")
        val outcome = schedule.reconcile(Cause.BOOT, listOf(missed.id)).getValue(missed.id)
        assertTrue(outcome.retire)
        val retired = repository.get(missed.id)!!
        assertFalse(retired.enabled)
        assertEquals("user", retired.updatedBy)
        assertEquals(missed.updatedAt, retired.updatedAt)
        assertEquals(changesBefore, changes.size)
        assertNull(port.wakes[missed.id])

        // Enabled again, it moves to the next day the time is reached (T48). zh-CN: 再次启用时改到下一个到达该时刻的日子 (T48).
        assertEquals(at("2026-11-06T09:00"), repository.setEnabled(missed.id, true, "user").state.nextTrigger)
    }

    @Test
    fun `a clock set back does not ring the same instance twice, one set back over a day forgets it`() {
        val alarm = daily()
        now = at("2026-11-06T07:00")
        schedule.reconcile(Cause.TRIGGER, listOf(alarm.id))

        now = at("2026-11-06T06:30")
        assertNull(schedule.reconcile(Cause.CLOCK).getValue(alarm.id).ring)
        assertEquals(at("2026-11-07T07:00"), state(alarm.id).nextTrigger)

        now = at("2026-11-05T06:00")
        schedule.reconcile(Cause.CLOCK)
        assertNull(state(alarm.id).handledThrough)
        assertEquals(at("2026-11-05T07:00"), state(alarm.id).nextTrigger)

        // A clock set forward past the planned ring: `miss` with reason `late`. zh-CN: 时钟往前调过计划响铃: `miss`, 原因 `late`.
        now = at("2026-11-05T09:00")
        assertEquals("late", schedule.reconcile(Cause.CLOCK).getValue(alarm.id).missed?.reason)
        assertEquals(at("2026-11-06T07:00"), state(alarm.id).nextTrigger)
    }

    @Test
    fun `a new time zone moves the rings that follow the device zone`() {
        val follows = daily()
        val fixed = daily(7, "timeZone" to "Asia/Shanghai")
        zone = TOKYO
        schedule.reconcile(Cause.CLOCK)
        assertEquals(LocalDateTime.parse("2026-11-06T07:00").atZone(TOKYO).toInstant().toEpochMilli(), state(follows.id).nextTrigger)
        assertEquals(at("2026-11-06T07:00"), state(fixed.id).nextTrigger)
        assertEquals(state(follows.id).nextTrigger, port.wakes[follows.id])
        assertTrue(events.isEmpty())
    }

    @Test
    fun `skipNext skips one instance and sends skip when it passes`() {
        val alarm = daily()
        val skipped = repository.skipNext(alarm.id, "script")
        assertTrue(skipped.toSnapshot().skipping)
        assertEquals(at("2026-11-06T07:00"), skipped.state.skipAt)
        assertEquals(at("2026-11-07T07:00"), skipped.state.nextTrigger)
        assertEquals(at("2026-11-07T07:00"), port.wakes[alarm.id])
        assertEquals(at("2026-11-06T07:00"), port.skips[alarm.id])
        assertEquals("update", changes.last().action)
        assertEquals("script", changes.last().source)

        val count = changes.size
        assertEquals(skipped, repository.skipNext(alarm.id, "tool"))
        assertEquals(count, changes.size)

        val restored = repository.cancelSkip(alarm.id, "tool")
        assertNull(restored.state.skipAt)
        assertEquals(at("2026-11-06T07:00"), restored.state.nextTrigger)
        assertNull(port.skips[alarm.id])
        assertEquals(restored, repository.cancelSkip(alarm.id, "tool"))

        repository.skipNext(alarm.id, "script")
        now = at("2026-11-06T07:00")
        val outcome = schedule.reconcile(Cause.TRIGGER, listOf(alarm.id)).getValue(alarm.id)
        assertNull(outcome.ring)
        val event = outcome.events.single()
        assertEquals("skip", event.type)
        assertEquals("skipNext", event.reason)
        assertEquals(at("2026-11-06T07:00"), event.scheduledAt)
        assertEquals("${alarm.id}@${at("2026-11-06T07:00")}", event.occurrence)
        assertFalse(repository.get(alarm.id)!!.state.skipping)
        assertEquals(at("2026-11-07T07:00"), state(alarm.id).nextTrigger)
        assertTrue(port.missed.isEmpty())
    }

    @Test
    fun `skipNext disables a one-shot alarm and a changed time drops the skip`() {
        val oneShot = repository.add(optionsOf(9, 0), "user")
        val disabled = repository.skipNext(oneShot.id, "script")
        assertFalse(disabled.enabled)
        assertEquals("script", changes.last().source)
        assertNull(port.wakes[oneShot.id])

        val alarm = daily()
        repository.skipNext(alarm.id, "script")
        val moved = repository.update(alarm.id, mapOf("hour" to 8, "minute" to 0), "user")
        assertNull(moved.state.skipAt)
        assertEquals(at("2026-11-05T08:00") + 24 * HOUR, moved.state.nextTrigger)

        assertAlarmError("NOT_FOUND") { repository.skipNext("a_unknown", "script") }
        assertAlarmError("NOT_FOUND") { repository.cancelSkip("a_unknown", "script") }
    }

    @Test
    fun `a snooze rings as a new round and one missed by more than the window ends the round`() {
        val alarm = daily()
        val t = at("2026-11-06T07:00")
        now = t
        schedule.reconcile(Cause.TRIGGER, listOf(alarm.id))
        // The ring service of roadmap P2.4 snoozes: zh-CN: 路线图 P2.4 的响铃服务贪睡:
        val snoozed = repository.updateState(alarm.id) { it.state.copy(snoozeUntil = t + 10 * MINUTE, snoozeCount = 1) }!!
        assertEquals(t + 10 * MINUTE, snoozed.toSnapshot().nextTrigger)
        assertEquals(at("2026-11-07T07:00"), snoozed.state.nextTrigger)

        now = t + 10 * MINUTE
        val ring = schedule.reconcile(Cause.TRIGGER, listOf(alarm.id)).getValue(alarm.id).ring!!
        assertEquals("${alarm.id}@$t~1", ring.occurrence)
        assertEquals(t + 10 * MINUTE, ring.scheduledAt)
        assertEquals(1, ring.snoozeCount)
        assertNull(state(alarm.id).snoozeUntil)

        repository.updateState(alarm.id) { it.state.copy(snoozeUntil = now + 10 * MINUTE, snoozeCount = 2) }
        now += 10 * MINUTE + AlarmLimits.MISSED_RING_WINDOW_MILLIS + 1
        val ended = schedule.reconcile(Cause.BOOT).getValue(alarm.id)
        assertNull(ended.ring)
        assertTrue(ended.events.isEmpty())
        assertEquals(0, state(alarm.id).snoozeCount)
        assertEquals(at("2026-11-07T07:00"), state(alarm.id).nextTrigger)
    }

    @Test
    fun `only a trigger accepts a ring slightly early`() {
        val early = daily()
        val clock = daily()
        now = at("2026-11-06T07:00") - 1_500L
        assertEquals(at("2026-11-06T07:00"), schedule.reconcile(Cause.TRIGGER, listOf(early.id)).getValue(early.id).ring?.scheduledAt)
        assertNull(schedule.reconcile(Cause.CLOCK, listOf(clock.id)).getValue(clock.id).ring)
        assertEquals(at("2026-11-06T07:00"), state(clock.id).nextTrigger)
    }

    @Test
    fun `without the exact alarm permission nothing enabled is added and nothing is scheduled`() {
        val alarm = daily()
        exact = false
        assertAlarmError(PERMISSION_REQUIRED, "exact alarm") { daily() }
        assertEquals(1, repository.list().size)
        val disabled = repository.add(optionsOf(9, 0, "enabled" to false), "script")
        assertAlarmError(PERMISSION_REQUIRED) { repository.setEnabled(disabled.id, true, "script") }
        assertAlarmError(PERMISSION_REQUIRED) { repository.update(alarm.id, mapOf("label" to "x"), "script") }
        assertFalse(repository.get(disabled.id)!!.enabled)
        assertNull(repository.get(alarm.id)!!.spec.label)

        // A revoked permission cancels the wake-ups, the planned ring stays. zh-CN: 权限被撤销时取消唤醒, 计划的响铃保留.
        schedule.reconcile(Cause.RESTORE)
        assertTrue(alarm.id in port.cancelled)
        assertNull(port.wakes[alarm.id])
        assertEquals(at("2026-11-06T07:00"), state(alarm.id).nextTrigger)
        assertFalse(repository.setEnabled(alarm.id, false, "script").enabled)

        exact = true
        repository.setEnabled(alarm.id, true, "script")
        schedule.rescheduleAll()
        assertEquals(at("2026-11-06T07:00"), port.wakes[alarm.id])
        assertNull(port.wakes[disabled.id])
    }

    @Test
    fun `removing an alarm cancels its wake-ups and occurrences stay within the limit`() {
        val alarm = daily()
        assertTrue(repository.remove(alarm.id, "tool"))
        assertTrue(alarm.id in port.cancelled)
        val longest = AlarmPlanner.occurrenceOf("a_" + "z".repeat(AlarmRepository.ID_RANDOM_CHARS), Long.MAX_VALUE, AlarmLimits.MAX_SNOOZE_MAX)
        assertTrue(longest.length <= AlarmLimits.MAX_OCCURRENCE_CHARS)
    }

    private inner class FakePort : AlarmSchedulePort {
        val wakes = HashMap<String, Long?>()
        val skips = HashMap<String, Long?>()
        val cancelled = LinkedHashSet<String>()
        val rings = ArrayList<AlarmPlanner.RingRequest>()
        val missed = ArrayList<Pair<String, AlarmEvent>>()

        override fun canScheduleExact() = exact

        override fun schedule(record: AlarmRecord) {
            wakes[record.id] = record.state.wakeAt.takeIf { record.enabled }
            skips[record.id] = record.state.skipAt.takeIf { record.enabled }
        }

        override fun cancel(alarmId: String) {
            cancelled += alarmId
            wakes[alarmId] = null
            skips[alarmId] = null
        }

        override fun ring(record: AlarmRecord, request: AlarmPlanner.RingRequest) {
            rings += request
        }

        override fun notifyMissed(record: AlarmRecord, event: AlarmEvent) {
            missed += record.id to event
        }
    }

    private companion object {
        val SHANGHAI: ZoneId = ZoneId.of("Asia/Shanghai")
        val TOKYO: ZoneId = ZoneId.of("Asia/Tokyo")
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE

        fun at(text: String): Long = LocalDateTime.parse(text).atZone(SHANGHAI).toInstant().toEpochMilli()
    }
}
