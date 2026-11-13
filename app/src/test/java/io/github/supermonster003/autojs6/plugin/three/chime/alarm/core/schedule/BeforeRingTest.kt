package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmOptionsValidator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRuntimeState
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.assertAlarmError
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.optionsOf
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.AlarmApi
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.BindingHints
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.AlarmEventBus
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayDay
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayJudge
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingCoordinator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingtoneLibrary
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner.Cause
import org.autojs.plugin.alarm.api.AlarmContract.BY_USER
import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_OPTIONS
import org.autojs.plugin.alarm.api.AlarmErrorCodes.UNSUPPORTED
import org.autojs.plugin.alarm.api.AlarmEvent
import org.autojs.plugin.alarm.api.AlarmFeatures
import org.autojs.plugin.alarm.api.AlarmJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/**
 * Roadmap P8.4 evidence (plugin side, appendix A section 5.4): the `beforeRing` wake-up follows who listens and how
 * early; the event carries the `occurrence` and the `deadline`; any `skip` replaces the ring with a `skip` event with
 * reason `script`; `ring` overrides merge in arrival order and change only that instance, its snooze rounds included;
 * late, unknown and post-skip decisions are ignored; no decision rings as configured; an instance filtered out by
 * holidays gets no `beforeRing`; malformed decisions are `INVALID_OPTIONS`; the decisions survive the store.
 * zh-CN: 路线图 P8.4 证据 (插件侧, 附录 A 第 5.4 节): `beforeRing` 唤醒跟随监听者与提前量; 事件带 `occurrence` 与 `deadline`; 任何 `skip` 以原因为
 * `script` 的 `skip` 事件代替响铃; `ring` 覆盖按到达顺序合并, 只改变该次 (含其贪睡轮次); 迟到, 未知与跳过之后的决定被忽略; 无决定照常响;
 * 被节假日过滤的那一次没有 `beforeRing`; 格式错误的决定报 `INVALID_OPTIONS`; 决定随存储保留.
 */
class BeforeRingTest {

    @get:Rule
    val temp = TemporaryFolder()

    private var now = at("2026-11-05T08:00")

    /** The lead each alarm is listened with; absent means nobody listens. zh-CN: 各闹钟被监听的提前量; 没有表示无人监听. */
    private val leads = HashMap<String, Int>()

    private val holidays = HashSet<LocalDate>()

    private val judge = HolidayJudge { date, _ ->
        HolidayDay(date, date in holidays, makeupWorkday = false, weekend = false, names = emptyList(), estimated = false, missing = false)
    }

    private val validator = AlarmOptionsValidator(features = setOf(AlarmFeatures.ALARMS, AlarmFeatures.HOLIDAYS, AlarmFeatures.BEFORE_RING))

    private val port = Port()
    private val events = ArrayList<AlarmEvent>()

    private lateinit var repository: AlarmRepository
    private lateinit var schedule: AlarmSchedule
    private lateinit var rings: RingCoordinator
    private lateinit var api: AlarmApi

    @Before
    fun setUp() = boot()

    /** Builds the plugin's objects on the saved alarms, as a new process does. zh-CN: 像新进程一样在已存闹钟上建立插件对象. */
    private fun boot() {
        events.clear()
        lateinit var created: AlarmSchedule
        repository = AlarmRepository(
            store = AlarmStore(File(temp.root, "alarms"), clock = { now }),
            validator = validator,
            clock = { now },
            deviceZone = { SHANGHAI },
            planner = { created.plan(it) },
            holidayJudge = judge,
        )
        val bus = AlarmEventBus().also { it.addListener(events::add) }
        created = AlarmSchedule(repository, port, bus, clock = { now }, deviceZone = { SHANGHAI }, judge = judge, beforeRingLead = { leads[it] })
        schedule = created
        rings = RingCoordinator(repository, schedule, bus, clock = { now }, validator = validator)
        api = AlarmApi(
            repository, rings, BindingHints(File(temp.root, BindingHints.FILE_NAME)), RingtoneLibrary(temp.root, { null }, { now }), { emptyList() },
            validator = validator, clock = { now }, deviceZone = { SHANGHAI }, locale = { Locale.ENGLISH }, judge = judge, schedule = schedule,
        )
    }

    private fun daily(hour: Int, vararg extra: Pair<String, Any?>): AlarmRecord =
        repository.add(optionsOf(hour, 0, "repeat" to "FREQ=DAILY", *extra), "script")

    private fun state(alarm: AlarmRecord): AlarmRuntimeState = repository.get(alarm.id)!!.state

    private fun take(): List<AlarmEvent> = events.toList().also { events.clear() }

    private fun fire(alarm: AlarmRecord): AlarmPlanner.Outcome = schedule.reconcile(Cause.TRIGGER, listOf(alarm.id)).getValue(alarm.id)

    private fun decide(alarm: AlarmRecord, occurrence: String, decision: String, vararg extra: Pair<String, Any?>): Map<String, Any?> =
        api.decideBeforeRing(linkedMapOf<String, Any?>("occurrence" to occurrence, "alarmId" to alarm.id, "decision" to decision).apply { extra.forEach { (k, v) -> put(k, v) } })

    /** Sends the `beforeRing` of [alarm] at its wake-up and answers the event. zh-CN: 在 [alarm] 的唤醒时刻发出其 `beforeRing` 并返回事件. */
    private fun beforeRing(alarm: AlarmRecord): AlarmEvent {
        now = port.beforeRingWakes.getValue(alarm.id)!!
        val event = schedule.beforeRing(alarm.id)!!
        assertEquals(event, take().single())
        return event
    }

    @Test
    fun `the wake-up follows the lead of the listeners and nobody listening sets none`() {
        val alarm = daily(7)
        val t = at("2026-11-06T07:00")
        assertNull("nobody listens", port.beforeRingWakes[alarm.id])
        assertEquals(t, port.wakes[alarm.id])

        leads[alarm.id] = 15
        schedule.beforeRingListenersChanged(listOf(alarm.id))
        assertEquals(t - 15 * SECOND, port.beforeRingWakes[alarm.id])
        leads[alarm.id] = 60
        schedule.rescheduleAll()
        assertEquals(t - MINUTE, port.beforeRingWakes[alarm.id])

        // An edit of the time moves the wake-up. zh-CN: 修改时间会移动唤醒.
        repository.update(alarm.id, mapOf("hour" to 6, "minute" to 30), "script")
        assertEquals(at("2026-11-06T06:30") - MINUTE, port.beforeRingWakes[alarm.id])

        leads.remove(alarm.id)
        schedule.beforeRingListenersChanged()
        assertNull(port.beforeRingWakes[alarm.id])

        // A disabled alarm and one past its deadline get none. zh-CN: 已禁用的闹钟与已过截止时刻的没有唤醒.
        leads[alarm.id] = 60
        repository.setEnabled(alarm.id, false, "script")
        assertNull(port.beforeRingWakes[alarm.id])
        repository.setEnabled(alarm.id, true, "script")
        now = at("2026-11-06T06:30") - 4 * SECOND
        schedule.beforeRingListenersChanged()
        assertNull("past the deadline", port.beforeRingWakes[alarm.id])
        now = at("2026-11-06T06:30") - 30 * SECOND
        schedule.beforeRingListenersChanged()
        assertEquals("a lead already passed means as soon as possible", at("2026-11-06T06:30") - MINUTE, port.beforeRingWakes[alarm.id])
    }

    @Test
    fun `the event carries the occurrence and the deadline and goes out once`() {
        val alarm = daily(7, "label" to "wake")
        leads[alarm.id] = 30
        schedule.beforeRingListenersChanged()
        val t = at("2026-11-06T07:00")

        now = t - 40 * SECOND
        assertNull("too early", schedule.beforeRing(alarm.id))
        assertTrue(take().isEmpty())

        val event = beforeRing(alarm)
        assertEquals("beforeRing", event.type)
        assertEquals("alarm", event.kind)
        assertEquals("${alarm.id}@$t", event.occurrence)
        assertEquals(t, event.scheduledAt)
        assertEquals(t - 30 * SECOND, event.firedAt)
        assertEquals(t - 5 * SECOND, event.deadline)
        assertEquals("wake", event.label)
        assertEquals(t, state(alarm).beforeRingFor)
        assertNull("sent once per instance", port.beforeRingWakes[alarm.id])
        assertNull(schedule.beforeRing(alarm.id))
        assertTrue(take().isEmpty())

        // No decision: the alarm rings as configured, and the next instance gets its own wake-up.
        // zh-CN: 没有决定: 闹钟照常响, 下一次有自己的唤醒.
        now = t
        val ring = fire(alarm).ring!!
        assertNull(ring.overrides)
        assertEquals("ring", take().single().type)
        assertEquals("wake", rings.sounding?.label)
        assertNull(state(alarm).beforeRingFor)
        assertEquals(at("2026-11-07T07:00") - 30 * SECOND, port.beforeRingWakes[alarm.id])
    }

    @Test
    fun `any skip replaces the ring with a skip event with reason script`() {
        val alarm = daily(7)
        leads[alarm.id] = 60
        schedule.beforeRingListenersChanged()
        val t = at("2026-11-06T07:00")
        val occurrence = beforeRing(alarm).occurrence

        assertTrue(decide(alarm, occurrence, "ring", "overrides" to mapOf("label" to "first")).isEmpty())
        assertTrue(decide(alarm, occurrence, "skip", "reason" to "day off").isEmpty())
        // After a skip, later decisions are ignored. zh-CN: 跳过之后, 后来的决定被忽略.
        decide(alarm, occurrence, "ring", "overrides" to mapOf("label" to "late"))
        decide(alarm, occurrence, "skip", "reason" to "second")
        assertEquals("day off", state(alarm).beforeRingSkip)
        assertNull(state(alarm).beforeRingOverrides)

        now = t
        val outcome = fire(alarm)
        assertNull(outcome.ring)
        assertTrue(port.rings.isEmpty())
        val skip = take().single()
        assertEquals("skip", skip.type)
        assertEquals("script", skip.reason)
        assertEquals("day off", skip.detail)
        assertEquals(occurrence, skip.occurrence)
        assertEquals(at("2026-11-07T07:00"), state(alarm).nextTrigger)
        assertNull(state(alarm).beforeRingSkip)

        // A skip without a reason; a one-shot alarm retires. zh-CN: 不带原因的跳过; 一次性闹钟随之退役.
        val once = repository.add(optionsOf(9, 0, "date" to "2026-11-07"), "script")
        leads[once.id] = 15
        schedule.beforeRingListenersChanged()
        val onceOccurrence = beforeRing(once).occurrence
        decide(once, onceOccurrence, "skip")
        now = at("2026-11-07T09:00")
        val retired = fire(once)
        assertTrue(retired.retire)
        val onceSkip = take().single()
        assertEquals("script", onceSkip.reason)
        assertNull(onceSkip.detail)
        assertFalse(repository.get(once.id)!!.enabled)
        assertNull(port.beforeRingWakes[once.id])
    }

    @Test
    fun `a skip wins even when the ring comes late and no missed notice follows`() {
        val alarm = daily(7)
        leads[alarm.id] = 60
        schedule.beforeRingListenersChanged()
        val occurrence = beforeRing(alarm).occurrence
        decide(alarm, occurrence, "skip", "reason" to "asleep anyway")
        now = at("2026-11-06T07:20")
        fire(alarm)
        assertEquals(listOf("skip"), take().map { it.type })
        assertTrue(port.missed.isEmpty())
    }

    @Test
    fun `ring overrides merge in arrival order and stay with the snooze rounds of that instance only`() {
        val alarm = daily(7, "label" to "wake", "vibrate" to true, "vibratePattern" to listOf(0, 100, 100))
        leads[alarm.id] = 60
        schedule.beforeRingListenersChanged()
        val t = at("2026-11-06T07:00")
        val occurrence = beforeRing(alarm).occurrence

        decide(alarm, occurrence, "ring", "overrides" to mapOf("label" to "rain", "sound" to SILENT))
        decide(alarm, occurrence, "ring", "overrides" to mapOf("label" to "storm", "vibrate" to false))
        decide(alarm, occurrence, "ring")
        decide(alarm, occurrence, "ring", "overrides" to emptyMap<String, Any?>())
        assertEquals(mapOf("label" to "storm", "sound" to SILENT, "vibrate" to false), state(alarm).beforeRingOverrides)

        now = t
        val ring = fire(alarm).ring!!
        assertEquals(mapOf("label" to "storm", "sound" to SILENT, "vibrate" to false), ring.overrides)
        val event = take().single()
        assertEquals("ring", event.type)
        assertEquals("storm", event.label)
        val sounding = rings.sounding!!
        assertEquals("storm", sounding.label)
        assertEquals("silent", sounding.settings.source)
        assertNull(sounding.settings.vibration)
        assertEquals("the stored alarm is unchanged", "wake", repository.get(alarm.id)!!.spec.label)
        assertEquals("storm", rings.ringing().single().label)

        // The snooze round of the same instance keeps the overrides. zh-CN: 同一次的贪睡轮次保留覆盖.
        now = t + 30 * SECOND
        assertTrue(rings.snooze(null, null, BY_USER))
        assertEquals("storm", take().single().label)
        assertEquals("storm", rings.ringing().single().label)
        now = t + 10 * MINUTE + 30 * SECOND
        fire(alarm)
        assertEquals("storm", take().single().label)
        assertEquals("silent", rings.sounding!!.settings.source)

        // A process restart resumes the round with them. zh-CN: 进程重启后以覆盖接续该轮.
        boot()
        schedule.reconcile(Cause.RESTORE)
        take()
        assertEquals(1, rings.resume())
        assertEquals("storm", take().single().label)
        assertEquals("silent", rings.sounding!!.settings.source)

        // The next instance rings as configured. zh-CN: 下一次照常响.
        assertTrue(rings.dismiss(null, BY_USER))
        take()
        assertNull(state(alarm).ringOverrides)
        now = at("2026-11-07T07:00")
        fire(alarm)
        assertEquals("wake", take().single().label)
        assertEquals(listOf(0L, 100L, 100L), rings.sounding!!.settings.vibration)
    }

    @Test
    fun `late unknown and stale decisions are ignored and an edit drops the decisions`() {
        val alarm = daily(7)
        leads[alarm.id] = 60
        schedule.beforeRingListenersChanged()
        val t = at("2026-11-06T07:00")

        // Before the beforeRing went out, the occurrence is unknown. zh-CN: beforeRing 发出之前, occurrence 未知.
        decide(alarm, "${alarm.id}@$t", "skip")
        assertNull(state(alarm).beforeRingSkip)

        val occurrence = beforeRing(alarm).occurrence
        decide(alarm, "${alarm.id}@${t + DAY}", "skip")
        decide(alarm, "other@$t", "skip")
        assertTrue(decide(unknownAlarm(), occurrence, "skip").isEmpty())
        assertNull(state(alarm).beforeRingSkip)

        now = t - 5 * SECOND
        decide(alarm, occurrence, "skip", "reason" to "too late")
        assertNull("at the deadline it is too late", state(alarm).beforeRingSkip)

        now = t - 20 * SECOND
        decide(alarm, occurrence, "ring", "overrides" to mapOf("label" to "early"))
        // An edit of the time makes another instance: the decisions go and a new beforeRing is due.
        // zh-CN: 修改时间产生另一次: 决定作废, 新的 beforeRing 待发.
        repository.update(alarm.id, mapOf("hour" to 7, "minute" to 5), "script")
        assertNull(state(alarm).beforeRingFor)
        assertNull(state(alarm).beforeRingOverrides)
        assertEquals(at("2026-11-06T07:05") - MINUTE, port.beforeRingWakes[alarm.id])
        now = at("2026-11-06T07:05")
        assertNull(fire(alarm).ring!!.overrides)
    }

    @Test
    fun `an instance filtered out by holidays gets no beforeRing`() {
        holidays += LocalDate.parse("2026-11-06")
        val alarm = daily(7, "holidays" to mapOf("region" to "CN", "mode" to "skip"))
        leads[alarm.id] = 60
        schedule.beforeRingListenersChanged()
        assertEquals(at("2026-11-06T07:00"), state(alarm).holidaySkipAt)
        assertEquals(at("2026-11-07T07:00") - MINUTE, port.beforeRingWakes[alarm.id])

        now = at("2026-11-06T06:59")
        assertNull(schedule.beforeRing(alarm.id))
        now = at("2026-11-06T07:00")
        fire(alarm)
        val skip = take().single()
        assertEquals("holiday", skip.reason)
        assertEquals(at("2026-11-07T07:00") - MINUTE, port.beforeRingWakes[alarm.id])
    }

    @Test
    fun `malformed decisions are INVALID_OPTIONS`() {
        val alarm = daily(7, "label" to "wake")
        leads[alarm.id] = 60
        schedule.beforeRingListenersChanged()
        val occurrence = beforeRing(alarm).occurrence

        assertAlarmError(INVALID_OPTIONS, "decision") { decide(alarm, occurrence, "maybe") }
        assertAlarmError(INVALID_OPTIONS, "decision") { api.decideBeforeRing(mapOf("occurrence" to occurrence, "alarmId" to alarm.id)) }
        assertAlarmError(INVALID_OPTIONS, "occurrence") { api.decideBeforeRing(mapOf("alarmId" to alarm.id, "decision" to "skip")) }
        assertAlarmError(INVALID_OPTIONS, "occurrence") { decide(alarm, "x".repeat(129), "skip") }
        assertAlarmError(INVALID_OPTIONS, "when") { decide(alarm, occurrence, "skip", "when" to 1) }
        assertAlarmError(INVALID_OPTIONS, "reason") { decide(alarm, occurrence, "skip", "reason" to "x".repeat(101)) }
        assertAlarmError(INVALID_OPTIONS, "reason") { decide(alarm, occurrence, "ring", "reason" to "why") }
        assertAlarmError(INVALID_OPTIONS, "overrides") { decide(alarm, occurrence, "skip", "overrides" to mapOf("label" to "x")) }
        assertAlarmError(INVALID_OPTIONS, "overrides.hour") { decide(alarm, occurrence, "ring", "overrides" to mapOf("hour" to 8)) }
        assertAlarmError(INVALID_OPTIONS, "vibrate") { decide(alarm, occurrence, "ring", "overrides" to mapOf("vibrate" to null)) }
        assertAlarmError(INVALID_OPTIONS) { decide(alarm, occurrence, "ring", "overrides" to mapOf("speak" to "loud")) }
        assertNull("nothing was taken", state(alarm).beforeRingOverrides)
        assertNull(state(alarm).beforeRingSkip)

        // The overrides that stand are checked together with the earlier ones. zh-CN: 生效的覆盖与先前的一起检查.
        decide(alarm, occurrence, "ring", "overrides" to mapOf("vibrate" to true, "vibratePattern" to listOf(0, 200)))
        decide(alarm, occurrence, "ring", "overrides" to mapOf("label" to null))
        assertEquals(mapOf("vibrate" to true, "vibratePattern" to listOf(0, 200), "label" to null), state(alarm).beforeRingOverrides)
        decide(alarm, occurrence, "skip", "reason" to "x".repeat(100))
        assertEquals(100, state(alarm).beforeRingSkip!!.length)

        // The instance of the plugin tools has no decideBeforeRing (T17). zh-CN: 插件工具所用的实例没有 decideBeforeRing (T17).
        val tools = AlarmApi(repository, rings, BindingHints(File(temp.root, "tools.json")), RingtoneLibrary(temp.root, { null }, { now }), { emptyList() })
        assertAlarmError(UNSUPPORTED) { tools.decideBeforeRing(mapOf("occurrence" to occurrence, "alarmId" to alarm.id, "decision" to "skip")) }
    }

    @Test
    fun `the decisions survive the store`() {
        val state = AlarmRuntimeState(
            nextTrigger = 20L, beforeRingFor = 20L, beforeRingSkip = "", beforeRingOverrides = linkedMapOf("label" to null, "vibrate" to false),
            ringOverrides = linkedMapOf("sound" to SILENT),
        )
        val stored = AlarmRuntimeState.fromValues(AlarmJson.parse(AlarmJson.write(state.toValues()))!!)
        assertEquals(state, stored)
        assertEquals("", stored.beforeRingSkip)
        assertTrue(stored.beforeRingOverrides!!.containsKey("label"))

        val alarm = daily(7)
        leads[alarm.id] = 60
        schedule.beforeRingListenersChanged()
        val occurrence = beforeRing(alarm).occurrence
        decide(alarm, occurrence, "ring", "overrides" to mapOf("label" to "kept"))
        boot()
        assertEquals(mapOf("label" to "kept"), state(alarm).beforeRingOverrides)
        assertNull("the beforeRing of this instance went out already", port.beforeRingWakes[alarm.id])
        now = at("2026-11-06T07:00")
        fire(alarm)
        assertEquals("kept", take().single().label)
    }

    @Test
    fun `binding hints give the smallest lead of the rows that ask for beforeRing`() {
        val hints = BindingHints(File(temp.root, "leads.json"))
        val changes = ArrayList<Set<String>>()
        hints.onBeforeRingChange = { changes += it }
        val known: (String) -> Boolean = { true }
        hints.replace("host.a", mapOf("hints" to listOf(mapOf("alarmId" to "a_1", "events" to listOf("beforeRing")), mapOf("alarmId" to "a_2", "events" to listOf("ring")))), known)
        assertEquals(60, hints.beforeRingLead("a_1"))
        assertNull(hints.beforeRingLead("a_2"))
        assertEquals(listOf(setOf("a_1")), changes)

        hints.replace("host.b", mapOf("hints" to listOf(mapOf("alarmId" to "a_1", "events" to listOf("beforeRing", "ring"), "leadSeconds" to 20))), known)
        assertEquals(20, hints.beforeRingLead("a_1"))
        // The same table again changes nothing. zh-CN: 再次发送同一张表不改变任何东西.
        hints.replace("host.b", mapOf("hints" to listOf(mapOf("alarmId" to "a_1", "events" to listOf("beforeRing", "ring"), "leadSeconds" to 20))), known)
        assertEquals(listOf(setOf("a_1"), setOf("a_1")), changes)

        hints.forget("a_1", only = "host.b")
        assertEquals(60, hints.beforeRingLead("a_1"))
        hints.forget("a_1")
        assertNull(hints.beforeRingLead("a_1"))
        assertEquals(4, changes.size)
        hints.forget("a_2")
        assertEquals("a row without beforeRing changes no lead", 4, changes.size)
    }

    private fun unknownAlarm(): AlarmRecord = daily(8).also { repository.remove(it.id, "script") }

    private inner class Port : AlarmSchedulePort {
        val wakes = HashMap<String, Long?>()
        val beforeRingWakes = HashMap<String, Long?>()
        val rings = ArrayList<AlarmPlanner.RingRequest>()
        val missed = ArrayList<Pair<String, AlarmEvent>>()

        override fun canScheduleExact() = true

        override fun schedule(record: AlarmRecord) {
            wakes[record.id] = record.state.wakeAt.takeIf { record.enabled }
        }

        override fun scheduleBeforeRing(alarmId: String, at: Long?) {
            beforeRingWakes[alarmId] = at
        }

        override fun cancel(alarmId: String) {
            wakes[alarmId] = null
            beforeRingWakes[alarmId] = null
        }

        override fun ring(record: AlarmRecord, request: AlarmPlanner.RingRequest) {
            rings += request
            this@BeforeRingTest.rings.offer(record, request)
        }

        override fun notifyMissed(record: AlarmRecord, event: AlarmEvent) {
            missed += record.id to event
        }
    }

    private companion object {
        val SHANGHAI: ZoneId = ZoneId.of("Asia/Shanghai")
        const val SECOND = 1_000L
        const val MINUTE = 60 * SECOND
        const val DAY = 24 * 60 * MINUTE
        val SILENT: Map<String, Any?> = mapOf("source" to "silent")

        fun at(text: String): Long = LocalDateTime.parse(text).atZone(SHANGHAI).toInstant().toEpochMilli()
    }
}
