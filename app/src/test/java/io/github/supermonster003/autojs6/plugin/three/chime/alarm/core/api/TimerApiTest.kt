package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmOptionsValidator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.assertAlarmError
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.AlarmEventBus
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingCoordinator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner.Cause
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedule
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedulePort
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.JsonDocumentFile
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.Stopwatch
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerEngine
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerPort
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerStore
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_OPTIONS
import org.autojs.plugin.alarm.api.AlarmErrorCodes.LIMIT_EXCEEDED
import org.autojs.plugin.alarm.api.AlarmErrorCodes.NOT_FOUND
import org.autojs.plugin.alarm.api.AlarmEvent
import org.autojs.plugin.alarm.api.AlarmResultKeys
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
 * Roadmap P7.3 (T114, T116): the timer and stopwatch methods of `IAlarmPlugin` on value trees over the real timer
 * engine, stopwatch and ring coordinator: request keys and types, the error codes the host sees, the ring path of a
 * timer (`kind: 'timer'` on `ring`, `snooze`, `dismiss` and `silence`) and `cancel` of a ringing or snoozed timer.
 * zh-CN: 路线图 P7.3 (T114, T116): 基于真实计时器引擎, 秒表与响铃协调器的 `IAlarmPlugin` 计时器与秒表值树方法: 请求键与类型, 宿主看到的错误码,
 * 计时器的响铃路径 (`ring`, `snooze`, `dismiss`, `silence` 带 `kind: 'timer'`), 以及取消响铃中或贪睡中的计时器.
 */
class TimerApiTest {

    @get:Rule
    val temp = TemporaryFolder()

    private var now = at("2026-11-12T08:00")
    private var up = 5 * MINUTE

    private val events = ArrayList<AlarmEvent>()
    private val silent = ArrayList<String>()

    private lateinit var timers: TimerEngine
    private lateinit var stopwatch: Stopwatch
    private lateinit var rings: RingCoordinator
    private lateinit var api: TimerApi

    @Before
    fun setUp() {
        val validator = AlarmOptionsValidator()
        lateinit var created: AlarmSchedule
        val repository = AlarmRepository(
            store = AlarmStore(File(temp.root, "alarms"), clock = { now }),
            clock = { now },
            deviceZone = { SHANGHAI },
            planner = { created.plan(it) },
        )
        val bus = AlarmEventBus().also { it.addListener(events::add) }
        created = AlarmSchedule(repository, AlarmPort(), bus, clock = { now }, deviceZone = { SHANGHAI })
        timers = TimerEngine(
            store = TimerStore(File(temp.root, TimerStore.FILE_NAME)) { validator.timerOptions(it) },
            port = Port(),
            clock = { now },
            elapsed = { up },
            boot = { 1 },
        )
        stopwatch = Stopwatch(JsonDocumentFile(File(temp.root, Stopwatch.FILE_NAME), Stopwatch.FORMAT, Stopwatch.VERSION), { now }, { up }, { 1 })
        rings = RingCoordinator(repository, created, bus, clock = { now }, timers = timers)
        api = TimerApi(timers, stopwatch, rings, validator, clock = { now })
    }

    private fun advance(millis: Long) {
        now += millis
        up += millis
    }

    private fun take(): List<AlarmEvent> = events.toList().also { events.clear() }

    @Suppress("UNCHECKED_CAST")
    private fun timerOf(answer: Map<String, Any?>): Map<String, Any?> = answer[AlarmResultKeys.TIMER] as Map<String, Any?>

    private fun start(millis: Long = 10 * MINUTE, vararg extra: Pair<String, Any?>): String =
        timerOf(api.startTimer(linkedMapOf<String, Any?>("durationMillis" to millis, *extra)))["id"] as String

    private fun control(id: String, action: String, millis: Long? = null): Map<String, Any?> =
        api.controlTimer(linkedMapOf<String, Any?>("id" to id, "action" to action).apply { millis?.let { put("millis", it) } })

    /** Lets [id] reach its end and ring. zh-CN: 让 [id] 到点并响铃. */
    private fun ring(id: String) {
        val end = timers.get(id)!!.endsAt!!
        advance(end - now)
        assertEquals(1, timers.reconcile(Cause.TRIGGER))
    }

    @Test
    fun `startTimer answers the snapshot and getTimer and listTimers read it`() {
        val answer = timerOf(api.startTimer(mapOf("durationMillis" to 25 * MINUTE, "label" to "tea", "vibrate" to false)))
        val id = answer["id"] as String
        assertTrue(TimerRecord.isValidId(id))
        assertEquals(25 * MINUTE, answer[AlarmResultKeys.DURATION_MILLIS])
        assertEquals(25 * MINUTE, answer[AlarmResultKeys.REMAINING_MILLIS])
        assertEquals(AlarmContract.TIMER_RUNNING, answer[AlarmResultKeys.STATE])
        assertEquals(now + 25 * MINUTE, answer[AlarmResultKeys.ENDS_AT])
        assertEquals("tea", answer[AlarmResultKeys.LABEL])
        assertEquals(AlarmContract.SOURCE_SCRIPT, timers.get(id)!!.source)
        assertTrue("timers send no change events", take().isEmpty())

        advance(MINUTE)
        val read = timerOf(api.getTimer(mapOf("id" to id)))
        assertEquals(24 * MINUTE, read[AlarmResultKeys.REMAINING_MILLIS])
        assertEquals(emptyMap<String, Any?>(), api.getTimer(mapOf("id" to "t_gone")))
        val second = start(MINUTE)
        @Suppress("UNCHECKED_CAST")
        val listed = api.listTimers(emptyMap())[AlarmResultKeys.TIMERS] as List<Map<String, Any?>>
        assertEquals(listOf(id, second), listed.map { it["id"] })
        assertEquals(read, listed.first())
    }

    @Test
    fun `refused requests carry the codes of appendix A`() {
        assertAlarmError(INVALID_OPTIONS, "durationMillis") { api.startTimer(emptyMap()) }
        assertAlarmError(INVALID_OPTIONS) { api.startTimer(mapOf("durationMillis" to "25m")) }
        assertAlarmError(INVALID_OPTIONS, "1 s - 100 h") { api.startTimer(mapOf("durationMillis" to 999L)) }
        assertAlarmError(INVALID_OPTIONS, "1 s - 100 h") { api.startTimer(mapOf("durationMillis" to 100 * HOUR + 1)) }
        assertAlarmError(INVALID_OPTIONS) { api.startTimer(mapOf("durationMillis" to MINUTE, "repeat" to "FREQ=DAILY")) }
        assertAlarmError(INVALID_OPTIONS) { api.startTimer(mapOf("durationMillis" to MINUTE, "label" to 5)) }
        assertAlarmError(INVALID_OPTIONS) { api.getTimer(emptyMap()) }
        assertAlarmError(INVALID_OPTIONS) { api.listTimers(mapOf("filter" to emptyMap<String, Any?>())) }
        repeat(20) { start(MINUTE + it) }
        assertAlarmError(LIMIT_EXCEEDED) { start() }
        assertEquals(20, timers.list().size)

        val id = timers.list().first().id
        assertAlarmError(INVALID_OPTIONS, "unknown timer action") { control(id, "stop") }
        assertAlarmError(INVALID_OPTIONS, "millis") { control(id, AlarmContract.TIMER_ACTION_ADD_TIME) }
        assertAlarmError(INVALID_OPTIONS, "only goes with") { control(id, AlarmContract.TIMER_ACTION_PAUSE, MINUTE) }
        assertAlarmError(INVALID_OPTIONS) { api.controlTimer(mapOf("id" to id)) }
        assertAlarmError(INVALID_OPTIONS) { api.controlTimer(mapOf("id" to id, "action" to "pause", "extra" to 1)) }
        assertAlarmError(NOT_FOUND) { control("t_gone", AlarmContract.TIMER_ACTION_PAUSE) }
        assertAlarmError(INVALID_OPTIONS, "the time left") { control(id, AlarmContract.TIMER_ACTION_ADD_TIME, -HOUR) }
    }

    @Test
    fun `controlTimer pauses, resumes, adds time, resets and cancels`() {
        val id = start(10 * MINUTE)
        advance(2 * MINUTE)
        val paused = timerOf(control(id, AlarmContract.TIMER_ACTION_PAUSE))
        assertEquals(AlarmContract.TIMER_PAUSED, paused[AlarmResultKeys.STATE])
        assertEquals(8 * MINUTE, paused[AlarmResultKeys.REMAINING_MILLIS])
        assertNull(paused[AlarmResultKeys.ENDS_AT])
        assertEquals(7 * MINUTE, timerOf(control(id, AlarmContract.TIMER_ACTION_ADD_TIME, -MINUTE))[AlarmResultKeys.REMAINING_MILLIS])
        val resumed = timerOf(control(id, AlarmContract.TIMER_ACTION_RESUME))
        assertEquals(now + 7 * MINUTE, resumed[AlarmResultKeys.ENDS_AT])
        assertEquals(now + 9 * MINUTE, timerOf(control(id, AlarmContract.TIMER_ACTION_ADD_TIME, 2 * MINUTE))[AlarmResultKeys.ENDS_AT])
        val reset = timerOf(control(id, AlarmContract.TIMER_ACTION_RESET))
        assertEquals(AlarmContract.TIMER_PAUSED, reset[AlarmResultKeys.STATE])
        assertEquals(10 * MINUTE, reset[AlarmResultKeys.REMAINING_MILLIS])
        assertEquals(mapOf(AlarmResultKeys.REMOVED to true), control(id, AlarmContract.TIMER_ACTION_CANCEL))
        assertEquals(mapOf(AlarmResultKeys.REMOVED to false), control(id, AlarmContract.TIMER_ACTION_CANCEL))
        assertEquals(listOf(id), silent)
        assertTrue(take().isEmpty())
    }

    @Test
    fun `a due timer rings, snoozes and is dismissed with kind timer`() {
        val id = start(MINUTE, "label" to "eggs")
        ring(id)
        val ring = take().single()
        assertEquals(AlarmContract.EVENT_RING, ring.type)
        assertEquals(AlarmContract.KIND_TIMER, ring.kind)
        assertEquals(id, ring.alarmId)
        assertEquals("eggs", ring.label)
        assertEquals(now, ring.scheduledAt)
        assertEquals(id, rings.sounding?.alarmId)
        assertTrue(rings.sounding!!.isTimer)
        assertEquals(listOf(RingingSnapshot(id, AlarmContract.KIND_TIMER, "eggs", now, false, 0)), rings.ringing())
        assertEquals(AlarmContract.TIMER_RINGING, timerOf(api.getTimer(mapOf("id" to id)))[AlarmResultKeys.STATE])
        assertAlarmError(INVALID_OPTIONS, "ringing") { control(id, AlarmContract.TIMER_ACTION_PAUSE) }

        assertTrue("timers snooze with the plugin defaults (T116)", rings.snooze(null, null, AlarmContract.BY_USER))
        val snooze = take().single()
        assertEquals(AlarmContract.EVENT_SNOOZE, snooze.type)
        assertEquals(AlarmContract.KIND_TIMER, snooze.kind)
        assertEquals(10, snooze.minutes)
        assertNull(rings.sounding)
        assertTrue(rings.ringing().single().snoozed)

        advance(10 * MINUTE)
        assertEquals(1, timers.reconcile(Cause.TRIGGER))
        val again = take().single()
        assertEquals(AlarmContract.EVENT_RING, again.type)
        assertEquals(1, again.snoozeCount)

        assertTrue(rings.dismiss(id, AlarmContract.BY_USER))
        val dismiss = take().single()
        assertEquals(AlarmContract.EVENT_DISMISS, dismiss.type)
        assertEquals(AlarmContract.KIND_TIMER, dismiss.kind)
        assertEquals(AlarmContract.BY_USER, dismiss.by)
        assertNull("a timer is removed when its round ends", timers.get(id))
        assertTrue("the event cleans up the bindings, not the port", silent.isEmpty())
    }

    @Test
    fun `cancel ends a ringing timer with dismiss by the caller (T114)`() {
        val id = start(MINUTE)
        ring(id)
        take()
        assertEquals(mapOf(AlarmResultKeys.REMOVED to true), control(id, AlarmContract.TIMER_ACTION_CANCEL))
        val dismiss = take().single()
        assertEquals(AlarmContract.EVENT_DISMISS, dismiss.type)
        assertEquals(AlarmContract.KIND_TIMER, dismiss.kind)
        assertEquals(AlarmContract.BY_SCRIPT, dismiss.by)
        assertNull(timers.get(id))
        assertNull(rings.sounding)

        val snoozed = start(MINUTE)
        ring(snoozed)
        rings.snooze(snoozed, null, AlarmContract.BY_USER)
        take()
        val tool = TimerApi(timers, stopwatch, rings, clock = { now }, source = AlarmContract.SOURCE_TOOL, by = AlarmContract.BY_TOOL)
        assertTrue(tool.cancel(snoozed))
        val ended = take().single()
        assertEquals(AlarmContract.EVENT_DISMISS, ended.type)
        assertEquals(AlarmContract.BY_TOOL, ended.by)
        assertNull(timers.get(snoozed))
        assertFalse(tool.cancel(snoozed))
        assertEquals(AlarmContract.SOURCE_TOOL, tool.start(MINUTE, AlarmOptionsValidator().timerOptions(emptyMap())).source)
    }

    @Test
    fun `auto silence ends a timer with silence`() {
        val id = start(MINUTE, "autoSilenceMinutes" to 1)
        ring(id)
        val occurrence = take().single().occurrence
        advance(MINUTE)
        assertTrue(rings.silence(occurrence))
        val silence = take().single()
        assertEquals(AlarmContract.EVENT_SILENCE, silence.type)
        assertEquals(AlarmContract.KIND_TIMER, silence.kind)
        assertEquals(MINUTE, silence.ringMillis)
        assertNull(timers.get(id))
    }

    @Test
    fun `two timers due together queue like two alarms`() {
        val first = start(MINUTE)
        val second = start(MINUTE)
        advance(MINUTE)
        assertEquals(2, timers.reconcile(Cause.TRIGGER))
        assertEquals(listOf(first, second), take().map { it.alarmId }.plus(rings.queued.map { it.alarmId }))
        assertTrue(rings.dismiss(first, AlarmContract.BY_USER))
        val next = take()
        assertEquals(listOf(AlarmContract.EVENT_DISMISS, AlarmContract.EVENT_RING), next.map { it.type })
        assertEquals(second, rings.sounding?.alarmId)
    }

    @Test
    fun `the stopwatch actions answer its snapshot and a lap`() {
        @Suppress("UNCHECKED_CAST")
        fun state(answer: Map<String, Any?>) = answer[AlarmResultKeys.STOPWATCH] as Map<String, Any?>
        assertEquals(AlarmContract.STOPWATCH_IDLE, state(api.stopwatch(mapOf("action" to "state")))[AlarmResultKeys.STATE])
        assertEquals(AlarmContract.STOPWATCH_RUNNING, state(api.stopwatch(mapOf("action" to "start")))[AlarmResultKeys.STATE])
        advance(3 * SECOND)
        val lapAnswer = api.stopwatch(mapOf("action" to "lap"))
        @Suppress("UNCHECKED_CAST")
        val lap = lapAnswer[AlarmResultKeys.LAP] as Map<String, Any?>
        assertEquals(1, lap[AlarmResultKeys.LAP_INDEX])
        assertEquals(3 * SECOND, lap[AlarmResultKeys.LAP_MILLIS])
        assertEquals(1, (state(lapAnswer)[AlarmResultKeys.LAPS] as List<*>).size)
        val paused = state(api.stopwatch(mapOf("action" to "pause")))
        assertEquals(AlarmContract.STOPWATCH_PAUSED, paused[AlarmResultKeys.STATE])
        assertEquals(3 * SECOND, paused[AlarmResultKeys.ELAPSED_MILLIS])
        assertEquals(AlarmContract.STOPWATCH_RUNNING, state(api.stopwatch(mapOf("action" to "resume")))[AlarmResultKeys.STATE])
        assertEquals(AlarmContract.STOPWATCH_IDLE, state(api.stopwatch(mapOf("action" to "reset")))[AlarmResultKeys.STATE])

        assertAlarmError(INVALID_OPTIONS, "unknown stopwatch action") { api.stopwatch(mapOf("action" to "stop")) }
        assertAlarmError(INVALID_OPTIONS) { api.stopwatch(emptyMap()) }
        assertAlarmError(INVALID_OPTIONS) { api.stopwatch(mapOf("action" to "state", "laps" to 3)) }
        assertAlarmError(INVALID_OPTIONS, "running") { api.stopwatch(mapOf("action" to "lap")) }
    }

    /** Timers ring through the coordinator as on Android. zh-CN: 与 Android 上一样, 计时器经协调器响铃. */
    private inner class Port : TimerPort {
        override fun canScheduleExact() = true
        override fun schedule(record: TimerRecord) = Unit
        override fun cancel(timerId: String) = Unit
        override fun ring(record: TimerRecord, request: AlarmPlanner.RingRequest) = rings.offerTimer(record, request)
        override fun notifyMissed(record: TimerRecord, scheduledAt: Long) = Unit
        override fun removedSilently(timerId: String) {
            silent += timerId
        }
    }

    private class AlarmPort : AlarmSchedulePort {
        override fun canScheduleExact() = true
        override fun schedule(record: AlarmRecord) = Unit
        override fun cancel(alarmId: String) = Unit
        override fun ring(record: AlarmRecord, request: AlarmPlanner.RingRequest) = Unit
        override fun notifyMissed(record: AlarmRecord, event: AlarmEvent) = Unit
    }

    private companion object {
        val SHANGHAI: ZoneId = ZoneId.of("Asia/Shanghai")
        const val SECOND = 1_000L
        const val MINUTE = 60 * SECOND
        const val HOUR = 60 * MINUTE

        fun at(text: String): Long = LocalDateTime.parse(text).atZone(SHANGHAI).toInstant().toEpochMilli()
    }
}
