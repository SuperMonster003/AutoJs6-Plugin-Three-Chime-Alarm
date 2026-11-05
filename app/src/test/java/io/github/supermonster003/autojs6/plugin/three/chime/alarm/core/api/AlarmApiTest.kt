package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.assertAlarmError
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.optionsOf
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.AlarmEventBus
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.ChangeEvents
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingCoordinator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingtoneLibrary
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedule
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedulePort
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_OPTIONS
import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_RULE
import org.autojs.plugin.alarm.api.AlarmErrorCodes.IO_FAILED
import org.autojs.plugin.alarm.api.AlarmErrorCodes.LIMIT_EXCEEDED
import org.autojs.plugin.alarm.api.AlarmErrorCodes.NOT_FOUND
import org.autojs.plugin.alarm.api.AlarmErrorCodes.UNSUPPORTED
import org.autojs.plugin.alarm.api.AlarmEvent
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmResultKeys
import org.autojs.plugin.alarm.api.RingtoneSnapshot
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/**
 * Roadmap P2.5 (T18, T72 - T78): the `IAlarmPlugin` 1.0.0 methods on value trees over the real repository, scheduler
 * and ring state: request keys and types, limits, the error codes the host sees, sources and `by`, ringtones and hints.
 * zh-CN: 路线图 P2.5 (T18, T72 - T78): 基于真实仓库, 调度器与响铃状态的 `IAlarmPlugin` 1.0.0 值树方法: 请求键与类型, 上限, 宿主看到的错误码,
 * 来源与 `by`, 铃声与提示表.
 */
class AlarmApiTest {

    @get:Rule
    val temp = TemporaryFolder()

    private var now = at("2026-11-05T08:00")

    private val events = ArrayList<AlarmEvent>()
    private val opened = ArrayList<String>()
    private var system: List<RingtoneSnapshot> = listOf(RingtoneSnapshot("content://media/internal/audio/media/7", "Bell", AlarmContract.RINGTONE_SYSTEM, true))

    private lateinit var repository: AlarmRepository
    private lateinit var schedule: AlarmSchedule
    private lateinit var rings: RingCoordinator
    private lateinit var hints: BindingHints
    private lateinit var library: RingtoneLibrary
    private lateinit var api: AlarmApi

    @Before
    fun setUp() {
        lateinit var created: AlarmSchedule
        repository = AlarmRepository(
            store = AlarmStore(File(temp.root, "alarms"), clock = { now }),
            clock = { now },
            deviceZone = { SHANGHAI },
            planner = { created.plan(it) },
        )
        val bus = AlarmEventBus().also { it.addListener(events::add) }
        created = AlarmSchedule(repository, Port(), bus, clock = { now }, deviceZone = { SHANGHAI })
        schedule = created
        rings = RingCoordinator(repository, schedule, bus, clock = { now })
        repository.addListener { bus.emit(ChangeEvents.of(it, now)) }
        hints = BindingHints(File(temp.root, BindingHints.FILE_NAME))
        library = RingtoneLibrary(temp.root, { if (it.readBytes().first() == AUDIO) "audio/mpeg" else null }, { now }, maxBytes = 64)
        api = AlarmApi(repository, rings, hints, library, { system }, clock = { now }, deviceZone = { SHANGHAI }, locale = { Locale.ENGLISH })
    }

    private fun take(): List<AlarmEvent> = events.toList().also { events.clear() }

    @Suppress("UNCHECKED_CAST")
    private fun alarmOf(answer: Map<String, Any?>): Map<String, Any?> = answer[AlarmResultKeys.ALARM] as Map<String, Any?>

    private fun add(vararg extra: Pair<String, Any?>): String =
        alarmOf(api.addAlarm(optionsOf(9, 0, *extra)))[AlarmResultKeys.ID] as String

    @Test
    fun `add, list, update and remove record the script as source and send a change each`() {
        val added = alarmOf(api.addAlarm(optionsOf(9, 0, "label" to "work", "repeat" to "FREQ=DAILY", "contractVersion" to 1)))
        val id = added[AlarmResultKeys.ID] as String
        assertEquals(AlarmContract.SOURCE_SCRIPT, added[AlarmResultKeys.SOURCE])
        assertEquals(AlarmContract.SOURCE_SCRIPT, added[AlarmResultKeys.UPDATED_BY])
        assertEquals("Every day", added[AlarmResultKeys.REPEAT_TEXT])
        assertEquals(at("2026-11-05T09:00"), added[AlarmResultKeys.NEXT_TRIGGER])
        assertEquals(false, added[AlarmResultKeys.HAS_BINDINGS])
        val change = take().single()
        assertEquals(AlarmContract.EVENT_CHANGE, change.type)
        assertEquals(AlarmContract.CHANGE_ADD, change.action)
        assertEquals(AlarmContract.SOURCE_SCRIPT, change.source)
        assertEquals("work", change.label)

        assertEquals(added, alarmOf(api.getAlarm(mapOf("id" to id))))
        @Suppress("UNCHECKED_CAST")
        val listed = api.listAlarms(mapOf("filter" to mapOf("sources" to listOf("script"))))[AlarmResultKeys.ALARMS] as List<Map<String, Any?>>
        assertEquals(listOf(id), listed.map { it[AlarmResultKeys.ID] })
        assertEquals(emptyList<Any>(), api.listAlarms(mapOf("filter" to mapOf("sources" to listOf("user"))))[AlarmResultKeys.ALARMS])

        now += 60_000
        val updated = alarmOf(api.updateAlarm(mapOf("id" to id, "patch" to mapOf("label" to "gym"))))
        assertEquals("gym", updated["label"])
        assertEquals(AlarmContract.SOURCE_SCRIPT, updated[AlarmResultKeys.UPDATED_BY])
        assertEquals(now, updated[AlarmResultKeys.UPDATED_AT])
        assertEquals(AlarmContract.CHANGE_UPDATE, take().single().action)

        assertEquals(false, alarmOf(api.setAlarmEnabled(mapOf("id" to id, "enabled" to false)))["enabled"])
        assertEquals(AlarmContract.CHANGE_UPDATE, take().single().action)

        assertEquals(mapOf(AlarmResultKeys.REMOVED to true), api.removeAlarm(mapOf("id" to id)))
        val removal = take().single()
        assertEquals(AlarmContract.CHANGE_REMOVE, removal.action)
        assertEquals(AlarmContract.SOURCE_SCRIPT, removal.source)
        assertEquals("gym", removal.label)
        assertEquals(mapOf(AlarmResultKeys.REMOVED to false), api.removeAlarm(mapOf("id" to id)))
        assertTrue(take().isEmpty())
    }

    @Test
    fun `unknown ids answer no alarm, NOT_FOUND or false as appendix A says`() {
        assertEquals(emptyMap<String, Any?>(), api.getAlarm(mapOf("id" to "a_missing")))
        assertAlarmError(NOT_FOUND) { api.updateAlarm(mapOf("id" to "a_missing", "patch" to mapOf("label" to "x"))) }
        assertAlarmError(NOT_FOUND) { api.setAlarmEnabled(mapOf("id" to "a_missing", "enabled" to true)) }
        assertAlarmError(NOT_FOUND) { api.skipNext(mapOf("id" to "a_missing")) }
        assertAlarmError(NOT_FOUND) { api.cancelSkip(mapOf("id" to "a_missing")) }
        assertEquals(mapOf(AlarmResultKeys.HANDLED to false), api.dismiss(mapOf("id" to "a_missing")))
        assertEquals(mapOf(AlarmResultKeys.HANDLED to false), api.snooze(emptyMap()))
    }

    @Test
    fun `requests refuse unknown keys, missing keys, wrong types and other contract versions`() {
        val id = add()
        assertAlarmError(INVALID_OPTIONS, "unknown option 'name'") { api.getAlarm(mapOf("id" to id, "name" to "x")) }
        assertAlarmError(INVALID_OPTIONS, "missing option 'id'") { api.getAlarm(emptyMap()) }
        assertAlarmError(INVALID_OPTIONS, "'id' must be a string") { api.removeAlarm(mapOf("id" to 3)) }
        assertAlarmError(INVALID_OPTIONS, "missing option 'patch'") { api.updateAlarm(mapOf("id" to id)) }
        assertAlarmError(INVALID_OPTIONS, "'patch' must be an object") { api.updateAlarm(mapOf("id" to id, "patch" to "x")) }
        assertAlarmError(INVALID_OPTIONS, "'enabled' must be a boolean") { api.setAlarmEnabled(mapOf("id" to id, "enabled" to "yes")) }
        assertAlarmError(INVALID_OPTIONS, "unknown option 'filter.colour'") { api.listAlarms(mapOf("filter" to mapOf("colour" to "red"))) }
        assertAlarmError(INVALID_OPTIONS, "unknown option 'x'") { api.ringing(mapOf("x" to 1)) }
        assertAlarmError(INVALID_OPTIONS, "unknown option 'x'") { api.listRingtones(mapOf("x" to 1)) }
        assertAlarmError(INVALID_OPTIONS) { api.addAlarm(optionsOf(9, 0, "colour" to "red")) }
        assertAlarmError(LIMIT_EXCEEDED, "at most 100") { api.addAlarm(optionsOf(9, 0, "label" to "x".repeat(AlarmLimits.MAX_LABEL_CHARS + 1))) }
        assertAlarmError(INVALID_OPTIONS) { api.addAlarm(optionsOf(25, 0)) }
        assertAlarmError(INVALID_RULE) { api.addAlarm(optionsOf(9, 0, "repeat" to "FREQ=SECONDLY")) }
        assertAlarmError(UNSUPPORTED, "contract version 2") { api.getAlarm(mapOf("id" to id, "contractVersion" to 2)) }
        assertAlarmError(UNSUPPORTED) { api.getAlarm(mapOf("id" to id, "contractVersion" to "1")) }
        assertAlarmError(INVALID_OPTIONS, "unknown option 'x'") { api.getAlarm(mapOf("id" to id, "x" to null)) }
        assertEquals(id, alarmOf(api.getAlarm(mapOf("id" to id, "contractVersion" to 1)))[AlarmResultKeys.ID])
    }

    @Test
    fun `upcoming and preview count within their limits`() {
        add("repeat" to "FREQ=DAILY")
        @Suppress("UNCHECKED_CAST")
        fun upcoming(values: Map<String, Any?>) = api.upcoming(values)[AlarmResultKeys.OCCURRENCES] as List<Map<String, Any?>>
        assertEquals(1, upcoming(emptyMap()).size)
        assertEquals(at("2026-11-05T09:00"), upcoming(emptyMap()).single()[AlarmResultKeys.AT])
        assertEquals(5, upcoming(mapOf("count" to 5)).size)
        assertEquals(100, upcoming(mapOf("count" to 100)).size)
        assertAlarmError(LIMIT_EXCEEDED, "'count' is at most 100") { api.upcoming(mapOf("count" to 101)) }
        assertAlarmError(INVALID_OPTIONS, "'count' must be at least 1") { api.upcoming(mapOf("count" to 0)) }
        assertAlarmError(INVALID_OPTIONS, "'count' must be an integer") { api.upcoming(mapOf("count" to 1.5)) }

        val options = optionsOf(6, 30, "repeat" to "FREQ=WEEKLY;BYDAY=MO")
        val times = api.preview(mapOf("options" to options))[AlarmResultKeys.TIMES] as LongArray
        assertEquals(10, times.size)
        assertEquals(at("2026-11-09T06:30"), times.first())
        assertEquals(366, (api.preview(mapOf("options" to options, "count" to 366))[AlarmResultKeys.TIMES] as LongArray).size)
        assertAlarmError(LIMIT_EXCEEDED) { api.preview(mapOf("options" to options, "count" to 367)) }
        assertAlarmError(INVALID_OPTIONS, "missing option 'options'") { api.preview(emptyMap()) }
        assertAlarmError(INVALID_RULE) { api.preview(mapOf("options" to optionsOf(6, 30, "repeat" to "FREQ=HOURLY"))) }
        assertTrue(take().none { it.type != AlarmContract.EVENT_CHANGE })
    }

    @Test
    fun `dismiss and snooze from a script are recorded with by script`() {
        val id = add("repeat" to "FREQ=DAILY")
        take()
        now = at("2026-11-05T09:00")
        schedule.reconcile(AlarmPlanner.Cause.TRIGGER, listOf(id))
        assertEquals(AlarmContract.EVENT_RING, take().single().type)
        @Suppress("UNCHECKED_CAST")
        val ringing = api.ringing(emptyMap())[AlarmResultKeys.RINGING] as List<Map<String, Any?>>
        assertEquals(listOf(id), ringing.map { it[AlarmResultKeys.ID] })

        assertAlarmError(INVALID_OPTIONS) { api.snooze(mapOf("minutes" to 61)) }
        assertEquals(mapOf(AlarmResultKeys.HANDLED to true), api.snooze(mapOf("id" to id, "minutes" to 5)))
        val snooze = take().single()
        assertEquals(AlarmContract.BY_SCRIPT, snooze.by)
        assertEquals(5, snooze.minutes)

        assertEquals(mapOf(AlarmResultKeys.HANDLED to true), api.dismiss(emptyMap()))
        assertEquals(AlarmContract.BY_SCRIPT, take().single().by)
        assertEquals(emptyList<Any>(), api.ringing(emptyMap())[AlarmResultKeys.RINGING])
    }

    @Test
    fun `ringtones list the system ones first and import a decodable copy`() {
        assertAlarmError(INVALID_OPTIONS, "missing option 'displayName'") { api.importRingtone(emptyMap()) { stream("a") } }
        assertAlarmError(INVALID_OPTIONS, "'mimeType' must be a string") {
            api.importRingtone(mapOf("displayName" to "a.mp3", "mimeType" to 3)) { stream("a") }
        }
        assertAlarmError(INVALID_OPTIONS, "unknown option 'path'") { api.importRingtone(mapOf("displayName" to "a.mp3", "path" to "/x")) { stream("a") } }
        assertTrue("the file is not opened for a refused request", opened.isEmpty())

        @Suppress("UNCHECKED_CAST")
        val imported = api.importRingtone(mapOf("displayName" to "/sdcard/Music/Morning Song.mp3", "mimeType" to "audio/mpeg")) { stream("morning") }[AlarmResultKeys.RINGTONE] as Map<String, Any?>
        assertEquals("Morning Song", imported[AlarmResultKeys.TITLE])
        assertEquals(AlarmContract.RINGTONE_IMPORTED, imported[AlarmResultKeys.RINGTONE_SOURCE])
        assertEquals(false, imported[AlarmResultKeys.IS_DEFAULT])
        assertTrue((imported[AlarmResultKeys.URI] as String).startsWith("chime-ringtone://imported/"))
        assertEquals(listOf("morning"), opened)

        assertAlarmError(LIMIT_EXCEEDED) { api.importRingtone(mapOf("displayName" to "big.mp3")) { stream("x".repeat(65)) } }
        assertAlarmError(IO_FAILED) { api.importRingtone(mapOf("displayName" to "text.mp3")) { ByteArrayInputStream("not audio".toByteArray()) } }
        assertAlarmError(IO_FAILED) { api.importRingtone(mapOf("displayName" to "empty.mp3")) { ByteArrayInputStream(ByteArray(0)) } }

        @Suppress("UNCHECKED_CAST")
        val listed = api.listRingtones(emptyMap())[AlarmResultKeys.RINGTONES] as List<Map<String, Any?>>
        assertEquals(listOf("Bell", "Morning Song"), listed.map { it[AlarmResultKeys.TITLE] })
        assertEquals(listOf(true, false), listed.map { it[AlarmResultKeys.IS_DEFAULT] })

        val id = add("sound" to mapOf("source" to imported[AlarmResultKeys.URI]))
        assertEquals(imported[AlarmResultKeys.URI], (alarmOf(api.getAlarm(mapOf("id" to id)))["sound"] as Map<*, *>)["source"])

        system = List(AlarmLimits.MAX_RINGTONES) { RingtoneSnapshot("content://media/internal/audio/media/$it", "Tone $it", AlarmContract.RINGTONE_SYSTEM, it == 0) }
        assertEquals(AlarmLimits.MAX_RINGTONES, (api.listRingtones(emptyMap())[AlarmResultKeys.RINGTONES] as List<*>).size)
    }

    @Test
    fun `event sets default to every event and refuse unknown ones`() {
        assertEquals(AlarmContract.EVENTS, api.eventsOf(emptyMap()))
        assertEquals(linkedSetOf("ring", "change"), api.eventsOf(mapOf("events" to listOf("change", "ring", "ring"))))
        assertEquals(emptySet<String>(), api.eventsOf(mapOf("events" to emptyList<String>())))
        assertAlarmError(INVALID_OPTIONS, "unknown event 'wake'") { api.eventsOf(mapOf("events" to listOf("wake"))) }
        assertAlarmError(INVALID_OPTIONS, "unknown option 'filter'") { api.eventsOf(mapOf("filter" to mapOf<String, Any?>())) }
    }

    @Test
    fun `binding hints mark the snapshots and answer the unknown ids`() {
        val id = add()
        val answer = api.setBindingHints(HOST, mapOf("hints" to listOf(mapOf("alarmId" to id, "events" to listOf("ring")), mapOf("alarmId" to "a_gone", "events" to listOf("ring")))))
        assertEquals(mapOf(AlarmResultKeys.UNKNOWN_IDS to listOf("a_gone")), answer)
        assertEquals(true, alarmOf(api.getAlarm(mapOf("id" to id)))[AlarmResultKeys.HAS_BINDINGS])
        assertAlarmError(INVALID_OPTIONS, "missing option 'hints'") { api.setBindingHints(HOST, emptyMap()) }
        assertAlarmError(INVALID_OPTIONS, "unknown option 'table'") { api.setBindingHints(HOST, mapOf("table" to emptyList<Any>())) }
        api.setBindingHints(HOST, mapOf("hints" to emptyList<Any>()))
        assertEquals(false, alarmOf(api.getAlarm(mapOf("id" to id)))[AlarmResultKeys.HAS_BINDINGS])
    }

    @Test
    fun `a one-shot skipNext disables and cancelSkip of a repeating alarm restores the next ring`() {
        val once = add()
        assertEquals(false, alarmOf(api.skipNext(mapOf("id" to once)))["enabled"])
        val daily = add("repeat" to "FREQ=DAILY")
        val skipped = alarmOf(api.skipNext(mapOf("id" to daily)))
        assertEquals(true, skipped[AlarmResultKeys.SKIPPING])
        assertEquals(at("2026-11-06T09:00"), skipped[AlarmResultKeys.NEXT_TRIGGER])
        val restored = alarmOf(api.cancelSkip(mapOf("id" to daily)))
        assertEquals(false, restored[AlarmResultKeys.SKIPPING])
        assertEquals(at("2026-11-05T09:00"), restored[AlarmResultKeys.NEXT_TRIGGER])
        assertFalse(take().any { it.source != AlarmContract.SOURCE_SCRIPT })
    }

    private fun stream(text: String) = ByteArrayInputStream(byteArrayOf(AUDIO) + text.toByteArray()).also { opened += text }

    private inner class Port : AlarmSchedulePort {
        override fun canScheduleExact() = true
        override fun schedule(record: AlarmRecord) = Unit
        override fun cancel(alarmId: String) = Unit
        override fun ring(record: AlarmRecord, request: AlarmPlanner.RingRequest) = rings.offer(record, request)
        override fun notifyMissed(record: AlarmRecord, event: AlarmEvent) = Unit
    }

    private companion object {
        val SHANGHAI: ZoneId = ZoneId.of("Asia/Shanghai")
        const val HOST = "org.autojs.autojs6"
        const val AUDIO: Byte = 0x49

        fun at(text: String): Long = LocalDateTime.parse(text).atZone(SHANGHAI).toInstant().toEpochMilli()
    }
}
