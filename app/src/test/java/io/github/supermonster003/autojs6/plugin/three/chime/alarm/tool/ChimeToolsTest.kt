package io.github.supermonster003.autojs6.plugin.three.chime.alarm.tool

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.AlarmApi
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.BindingHints
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.AlarmEventBus
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.ChangeEvents
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingCoordinator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingtoneLibrary
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedule
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedulePort
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes
import org.autojs.plugin.alarm.api.AlarmEvent
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/**
 * Roadmap P4.1 (T18, T89 - T95): every 1.0.0 tool on the real repository, scheduler and ring state through the JSON a
 * provider receives and answers: the `AlarmOptions` shapes, the source `tool`, filters and limits, the update merge and
 * `clear`, rings handled by `tool`, and the alarm errors as tool errors.
 * zh-CN: 路线图 P4.1 (T18, T89 - T95): 在真实仓库, 调度器与响铃状态上, 经提供方收发的 JSON 测试每个 1.0.0 工具: `AlarmOptions` 的各种写法, 来源
 * `tool`, 筛选与上限, 修改的合并与 `clear`, 以 `tool` 处理响铃, 以及闹钟错误到工具错误的转换.
 */
class ChimeToolsTest {

    @get:Rule
    val temp = TemporaryFolder()

    private var now = at("2026-11-05T08:00")
    private var exactAllowed = true
    private val events = ArrayList<AlarmEvent>()

    private lateinit var schedule: AlarmSchedule
    private lateinit var rings: RingCoordinator
    private lateinit var tools: ChimeTools

    @Before
    fun setUp() {
        lateinit var created: AlarmSchedule
        val repository = AlarmRepository(
            store = AlarmStore(File(temp.root, "alarms"), clock = { now }),
            clock = { now },
            deviceZone = { SHANGHAI },
            planner = { created.plan(it) },
            schedulingAllowed = { exactAllowed },
        )
        val bus = AlarmEventBus().also { it.addListener(events::add) }
        created = AlarmSchedule(repository, Port(), bus, clock = { now }, deviceZone = { SHANGHAI })
        schedule = created
        rings = RingCoordinator(repository, schedule, bus, clock = { now })
        repository.addListener { bus.emit(ChangeEvents.of(it, now)) }
        val hints = BindingHints(File(temp.root, BindingHints.FILE_NAME))
        val library = RingtoneLibrary(temp.root, { "audio/mpeg" }, { now })
        val api = AlarmApi(
            repository, rings, hints, library, { emptyList() }, clock = { now }, deviceZone = { SHANGHAI }, locale = { Locale.ENGLISH },
            source = AlarmContract.SOURCE_TOOL, by = AlarmContract.BY_TOOL,
        )
        tools = ChimeTools(api) { SHANGHAI }
    }

    /** Calls [tool] with [json] arguments as the provider does and answers the decoded result. zh-CN: 像提供方那样调用. */
    @Suppress("UNCHECKED_CAST")
    private fun call(tool: String, json: String = "{}"): Map<String, Any?> =
        ToolJson.parseObject(ToolJson.encode(tools.call(tool, ToolJson.parseObject(json))))

    @Suppress("UNCHECKED_CAST")
    private fun alarm(answer: Map<String, Any?>): Map<String, Any?> = answer["alarm"] as Map<String, Any?>

    private fun add(json: String): Map<String, Any?> = alarm(call(ChimeToolCatalog.TOOL_ADD, json))

    private fun failure(code: String, mention: String? = null, block: () -> Unit): PluginToolException {
        try {
            block()
        } catch (e: PluginToolException) {
            assertEquals("tool error code of '${e.message}'", code, e.code)
            if (mention != null) assertTrue("'${e.message}' should mention '$mention'", e.message.contains(mention))
            return e
        }
        fail("expected $code")
        throw AssertionError()
    }

    @Test
    fun `alarm_add takes the AlarmOptions shapes and records the tool as source`() {
        val weekdays = add("""{"time": "07:30", "repeat": [1, 2, 3, 4, 5], "label": "work", "tag": "t1"}""")
        assertEquals("07:30", weekdays["time"])
        assertEquals("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", weekdays["repeat"])
        assertEquals("Monday to Friday", weekdays["repeatText"])
        assertEquals(AlarmContract.SOURCE_TOOL, weekdays["source"])
        assertEquals(AlarmContract.SOURCE_TOOL, weekdays["updatedBy"])
        assertEquals(true, weekdays["enabled"])
        assertEquals(at("2026-11-06T07:30"), weekdays["nextTrigger"])
        assertEquals("2026-11-06T07:30:00+08:00", weekdays["nextTriggerTime"])
        assertEquals(false, weekdays["skipping"])
        assertEquals(false, weekdays["hasBindings"])
        assertEquals(AlarmContract.SOURCE_TOOL, events.last().source)

        val monthly = add("""{"time": {"hour": 18, "minute": 5, "second": 30}, "repeat": {"freq": "monthly", "byDay": ["-1FR"]}, "timeZone": "Europe/Berlin"}""")
        assertEquals("18:05:30", monthly["time"])
        assertEquals("FREQ=MONTHLY;BYDAY=-1FR", monthly["repeat"])
        assertEquals("Europe/Berlin", monthly["timeZone"])
        assertTrue((monthly["nextTriggerTime"] as String).endsWith("+01:00"))

        val rrule = add("""{"time": "06:00", "repeat": "RRULE:freq=daily;interval=3", "date": 1762473600000}""")
        assertEquals("FREQ=DAILY;INTERVAL=3", rrule["repeat"])
        assertEquals("2025-11-07", rrule["date"])

        val structured = add("""{"time": "06:00", "repeat": {"freq": "yearly", "byMonth": [12], "byMonthDay": [-1], "until": "2030-12-31", "weekStart": 0}}""")
        assertEquals("FREQ=YEARLY;UNTIL=20301231;BYMONTH=12;BYMONTHDAY=-1;WKST=SU", structured["repeat"])

        val offset = add("""{"at": "2026-11-07T07:30:00+09:00", "label": "trip"}""")
        assertEquals(at("2026-11-07T06:30"), offset["at"])
        assertEquals("2026-11-07T06:30:00+08:00", offset["atTime"])
        assertNull(offset["time"])
        val local = add("""{"at": "2026-11-07T07:30", "timeZone": "UTC"}""")
        assertEquals(at("2026-11-07T15:30"), local["at"])
        val millis = add("""{"at": ${at("2026-11-08T10:00")}}""")
        assertEquals(at("2026-11-08T10:00"), millis["at"])
    }

    @Test
    fun `alarm_add keeps sound, vibration, speech and snooze in the option shape`() {
        val full = add(
            """{"time": "07:00", "enabled": false, "sound": {"source": "content://media/internal/audio/media/7", "volume": 0.5, "rampSeconds": 30},
                "vibrate": [0, 500, 200, 500], "speak": {"text": "Wake up", "language": "en", "mode": "after"},
                "snooze": {"minutes": 5}, "autoSilenceMinutes": 15}""",
        )
        assertEquals(false, full["enabled"])
        assertEquals(mapOf("source" to "content://media/internal/audio/media/7", "volume" to 0.5, "rampSeconds" to 30L), full["sound"])
        assertEquals(listOf(0L, 500L, 200L, 500L), full["vibrate"])
        assertEquals(mapOf("text" to "Wake up", "language" to "en", "mode" to "after"), full["speak"])
        assertEquals(mapOf("minutes" to 5L), full["snooze"])
        assertEquals(15L, full["autoSilenceMinutes"])
        assertNull(full["nextTrigger"])

        val plain = add("""{"time": "07:00", "vibrate": false, "snooze": false, "sound": {"source": "silent"}}""")
        assertEquals(false, plain["vibrate"])
        assertEquals(false, plain["snooze"])
        assertEquals(mapOf("source" to "silent"), plain["sound"])
        val defaults = add("""{"time": "07:00"}""")
        listOf("sound", "vibrate", "speak", "snooze", "autoSilenceMinutes", "label", "tag", "repeat", "repeatText", "timeZone").forEach {
            assertFalse("$it is not set", defaults.containsKey(it))
        }
    }

    @Test
    fun `alarm_add refuses local files, missing times, bad rules and long labels`() {
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "no local files") { call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "sound": {"source": "/sdcard/a.mp3"}}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "no local files") { call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "sound": {"source": "file:///sdcard/a.mp3"}}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "INVALID_OPTIONS: give time") { call(ChimeToolCatalog.TOOL_ADD, """{"label": "x"}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "time and at are exclusive") { call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "at": 1}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "time must be HH:mm") { call(ChimeToolCatalog.TOOL_ADD, """{"time": "7 am"}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "hour must be in 0 - 23") { call(ChimeToolCatalog.TOOL_ADD, """{"time": "24:00"}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "INVALID_RULE: ") { call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "repeat": "FREQ=HOURLY"}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "INVALID_RULE: ") { call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "repeat": [7]}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "unknown argument holidays") { call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "holidays": {"mode": "skip"}}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "at must be an ISO 8601") { call(ChimeToolCatalog.TOOL_ADD, """{"at": "tomorrow morning"}""") }
        failure(PluginToolErrorCodes.LIMIT_EXCEEDED, "LIMIT_EXCEEDED: label has 101 characters") { call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "label": "${"x".repeat(101)}"}""") }
        assertTrue("nothing was added", events.isEmpty())
    }

    @Test
    fun `a missing exact alarm permission is NOT_READY with what to do`() {
        exactAllowed = false
        val e = failure(PluginToolErrorCodes.NOT_READY, "PERMISSION_REQUIRED: ") { call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00"}""") }
        assertTrue(e.message.contains("permissions page"))
        assertEquals(false, add("""{"time": "07:00", "enabled": false}""")["enabled"])
    }

    @Test
    fun `alarm_list filters, limits and reports the total`() {
        val ids = (1..4).map { add("""{"time": "0$it:00", "tag": "${if (it % 2 == 0) "even" else "odd"}", "enabled": ${it != 4}}""")["id"] }
        val all = call(ChimeToolCatalog.TOOL_LIST)
        assertEquals(ids, (all["alarms"] as List<*>).map { (it as Map<*, *>)["id"] })
        assertEquals(4L, all["total"])
        assertEquals(false, all["truncated"])
        val limited = call(ChimeToolCatalog.TOOL_LIST, """{"limit": 3}""")
        assertEquals(3, (limited["alarms"] as List<*>).size)
        assertEquals(4L, limited["total"])
        assertEquals(true, limited["truncated"])
        assertEquals(listOf(ids[1]), (call(ChimeToolCatalog.TOOL_LIST, """{"tag": "even", "enabled": true}""")["alarms"] as List<*>).map { (it as Map<*, *>)["id"] })
        assertEquals(4L, call(ChimeToolCatalog.TOOL_LIST, """{"source": "tool"}""")["total"])
        assertEquals(4L, call(ChimeToolCatalog.TOOL_LIST, """{"source": ["user", "tool"]}""")["total"])
        assertEquals(0L, call(ChimeToolCatalog.TOOL_LIST, """{"source": "script"}""")["total"])
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "source must be one of") { call(ChimeToolCatalog.TOOL_LIST, """{"source": "robot"}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "limit must be 1 - 300") { call(ChimeToolCatalog.TOOL_LIST, """{"limit": 0}""") }
    }

    @Test
    fun `alarm_update changes only what it names, merges groups and clears options`() {
        val id = add("""{"time": "07:00", "label": "a", "repeat": "FREQ=DAILY", "sound": {"source": "silent", "volume": 0.3}, "snooze": {"minutes": 5, "max": 2}}""")["id"]
        now += 60_000
        val changed = alarm(call(ChimeToolCatalog.TOOL_UPDATE, """{"id": "$id", "label": "b", "sound": {"rampSeconds": 10}, "snooze": {"max": 4}}"""))
        assertEquals("b", changed["label"])
        assertEquals("FREQ=DAILY", changed["repeat"])
        assertEquals(mapOf("source" to "silent", "volume" to 0.3, "rampSeconds" to 10L), changed["sound"])
        assertEquals(mapOf("minutes" to 5L, "max" to 4L), changed["snooze"])
        assertEquals(AlarmContract.SOURCE_TOOL, changed["updatedBy"])
        assertEquals(now, changed["updatedAt"])

        val cleared = alarm(call(ChimeToolCatalog.TOOL_UPDATE, """{"id": "$id", "time": "08:15", "clear": ["repeat", "sound", "label"]}"""))
        assertEquals("08:15", cleared["time"])
        listOf("repeat", "sound", "label").forEach { assertFalse("$it was cleared", cleared.containsKey(it)) }
        assertEquals(mapOf("minutes" to 5L, "max" to 4L), cleared["snooze"])

        val moment = alarm(call(ChimeToolCatalog.TOOL_UPDATE, """{"id": "$id", "at": "2026-11-09T09:00"}"""))
        assertEquals(at("2026-11-09T09:00"), moment["at"])
        assertNull(moment["time"])

        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "both given and cleared") { call(ChimeToolCatalog.TOOL_UPDATE, """{"id": "$id", "label": "c", "clear": ["label"]}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "nothing to change") { call(ChimeToolCatalog.TOOL_UPDATE, """{"id": "$id"}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "clear: \"enabled\" is not one of") { call(ChimeToolCatalog.TOOL_UPDATE, """{"id": "$id", "clear": ["enabled"]}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "NOT_FOUND: ") { call(ChimeToolCatalog.TOOL_UPDATE, """{"id": "a_missing", "label": "x"}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "NOT_FOUND: ") { call(ChimeToolCatalog.TOOL_UPDATE, """{"id": "a_missing", "sound": {"volume": 1}}""") }
    }

    @Test
    fun `alarm_remove, alarm_set_enabled and alarm_skip_next act on one alarm`() {
        val id = add("""{"time": "07:00", "repeat": "FREQ=DAILY"}""")["id"]
        val off = alarm(call(ChimeToolCatalog.TOOL_SET_ENABLED, """{"id": "$id", "enabled": false}"""))
        assertEquals(false, off["enabled"])
        assertNull(off["nextTrigger"])
        assertEquals(true, alarm(call(ChimeToolCatalog.TOOL_SET_ENABLED, """{"id": "$id", "enabled": true}"""))["enabled"])

        val skipped = alarm(call(ChimeToolCatalog.TOOL_SKIP_NEXT, """{"id": "$id"}"""))
        assertEquals(true, skipped["skipping"])
        assertEquals(at("2026-11-07T07:00"), skipped["nextTrigger"])
        val back = alarm(call(ChimeToolCatalog.TOOL_SKIP_NEXT, """{"id": "$id", "cancel": true}"""))
        assertEquals(false, back["skipping"])
        assertEquals(at("2026-11-06T07:00"), back["nextTrigger"])

        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "NOT_FOUND: ") { call(ChimeToolCatalog.TOOL_SET_ENABLED, """{"id": "a_missing", "enabled": true}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "NOT_FOUND: ") { call(ChimeToolCatalog.TOOL_SKIP_NEXT, """{"id": "a_missing"}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "unknown argument") { call(ChimeToolCatalog.TOOL_REMOVE, """{"id": "$id", "all": true}""") }

        assertEquals(mapOf("id" to id, "removed" to true), call(ChimeToolCatalog.TOOL_REMOVE, """{"id": "$id"}"""))
        assertEquals(mapOf("id" to id, "removed" to false), call(ChimeToolCatalog.TOOL_REMOVE, """{"id": "$id"}"""))
        assertEquals(AlarmContract.CHANGE_REMOVE, events.last().action)
        assertEquals(AlarmContract.SOURCE_TOOL, events.last().source)
    }

    @Test
    fun `alarm_upcoming lists the next rings with their zone time`() {
        add("""{"time": "07:00", "repeat": "FREQ=DAILY", "label": "daily"}""")
        add("""{"time": "09:00", "timeZone": "UTC", "tag": "utc"}""")
        val five = call(ChimeToolCatalog.TOOL_UPCOMING)["occurrences"] as List<*>
        assertEquals(5, five.size)
        val first = five.first() as Map<*, *>
        assertEquals(at("2026-11-05T17:00"), first["at"])
        assertEquals("2026-11-05T09:00:00Z", first["time"])
        assertFalse(first.containsKey("label"))
        val second = five[1] as Map<*, *>
        assertEquals("daily", second["label"])
        assertEquals("2026-11-06T07:00:00+08:00", second["time"])
        val times = five.map { (it as Map<*, *>)["at"] as Long }
        assertEquals(times.sorted(), times)
        assertEquals(1, (call(ChimeToolCatalog.TOOL_UPCOMING, """{"count": 3, "tag": "utc"}""")["occurrences"] as List<*>).size)
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "count must be 1 - 100") { call(ChimeToolCatalog.TOOL_UPCOMING, """{"count": 101}""") }
    }

    @Test
    fun `alarm_snooze and alarm_dismiss handle a ringing alarm by tool`() {
        assertEquals(mapOf("handled" to false), call(ChimeToolCatalog.TOOL_DISMISS))
        assertEquals(mapOf("handled" to false), call(ChimeToolCatalog.TOOL_SNOOZE, """{"id": "a_missing"}"""))
        val id = add("""{"time": "09:00", "repeat": "FREQ=DAILY"}""")["id"] as String
        now = at("2026-11-05T09:00")
        schedule.reconcile(AlarmPlanner.Cause.TRIGGER, listOf(id))
        assertEquals(AlarmContract.EVENT_RING, events.last().type)

        assertEquals(mapOf("handled" to true), call(ChimeToolCatalog.TOOL_SNOOZE, """{"id": "$id", "minutes": 5}"""))
        assertEquals(AlarmContract.EVENT_SNOOZE, events.last().type)
        assertEquals(AlarmContract.BY_TOOL, events.last().by)
        assertEquals(5, events.last().minutes)
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "minutes must be 1 - 60") { call(ChimeToolCatalog.TOOL_SNOOZE, """{"minutes": 61}""") }

        assertEquals(mapOf("handled" to true), call(ChimeToolCatalog.TOOL_DISMISS))
        assertEquals(AlarmContract.EVENT_DISMISS, events.last().type)
        assertEquals(AlarmContract.BY_TOOL, events.last().by)
        assertEquals(mapOf("handled" to false), call(ChimeToolCatalog.TOOL_DISMISS, """{"id": "$id"}"""))
    }

    @Test
    fun `alarm codes map onto tool codes and unknown tools are refused`() {
        val expected = mapOf(
            AlarmErrorCodes.INVALID_OPTIONS to PluginToolErrorCodes.INVALID_ARGUMENTS,
            AlarmErrorCodes.INVALID_RULE to PluginToolErrorCodes.INVALID_ARGUMENTS,
            AlarmErrorCodes.NOT_FOUND to PluginToolErrorCodes.INVALID_ARGUMENTS,
            AlarmErrorCodes.UNSUPPORTED to PluginToolErrorCodes.INVALID_ARGUMENTS,
            AlarmErrorCodes.LIMIT_EXCEEDED to PluginToolErrorCodes.LIMIT_EXCEEDED,
            AlarmErrorCodes.PERMISSION_REQUIRED to PluginToolErrorCodes.NOT_READY,
            AlarmErrorCodes.IO_FAILED to PluginToolErrorCodes.FAILED,
            AlarmErrorCodes.INTERNAL to PluginToolErrorCodes.FAILED,
        )
        expected.forEach { (alarm, tool) -> assertEquals(alarm, tool, ChimeTools.code(alarm)) }
        val e = ChimeTools.error(AlarmException.ioFailed("disk full"))
        assertEquals(PluginToolErrorCodes.FAILED, e.code)
        assertEquals("IO_FAILED: disk full", e.message)
        failure(PluginToolErrorCodes.UNKNOWN_TOOL) { call("timer_start") }
    }

    private inner class Port : AlarmSchedulePort {
        override fun canScheduleExact() = true
        override fun schedule(record: AlarmRecord) = Unit
        override fun cancel(alarmId: String) = Unit
        override fun ring(record: AlarmRecord, request: AlarmPlanner.RingRequest) = rings.offer(record, request)
        override fun notifyMissed(record: AlarmRecord, event: AlarmEvent) = Unit
    }

    private companion object {
        val SHANGHAI: ZoneId = ZoneId.of("Asia/Shanghai")

        fun at(text: String): Long = LocalDateTime.parse(text).atZone(SHANGHAI).toInstant().toEpochMilli()
    }
}
