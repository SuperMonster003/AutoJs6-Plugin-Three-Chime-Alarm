package io.github.supermonster003.autojs6.plugin.three.chime.alarm.tool

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.RepositoryFiles
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmOptionsValidator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.AlarmApi
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.BindingHints
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.HolidayApi
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.TimerApi
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.AlarmEventBus
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.ChangeEvents
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.CatalogSignatureVerifier
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.DirectorySource
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayData
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayJudge
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingCoordinator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingtoneLibrary
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedule
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedulePort
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.JsonDocumentFile
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.Stopwatch
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerEngine
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerPort
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerStore
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import org.autojs.plugin.alarm.api.AlarmErrorCodes
import org.autojs.plugin.alarm.api.AlarmEvent
import org.autojs.plugin.alarm.api.AlarmFeatures
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
 * Roadmap P4.1 (T18, T89 - T95), P7.6 (T126 - T129), P8.7 (T192 - T195) and P9.4 (T290): every tool on the real repository,
 * scheduler, timer engine, ring state and bundled holiday data through the JSON a provider receives and answers: the
 * `AlarmOptions` shapes, the source `tool`, filters and limits, the update merge and `clear`, rings handled by `tool`,
 * the timer durations and actions, `holiday_check` and the `holidays` option, the `dismiss` option without `script`, and
 * the alarm errors as tool errors.
 * zh-CN: 路线图 P4.1 (T18, T89 - T95), P7.6 (T126 - T129), P8.7 (T192 - T195) 与 P9.4 (T290): 在真实仓库, 调度器, 计时器引擎, 响铃状态与内置节假日数据上,
 * 经提供方收发的 JSON 测试每个工具: `AlarmOptions` 的各种写法, 来源 `tool`, 筛选与上限, 修改的合并与 `clear`, 以 `tool` 处理响铃, 计时器的时长与动作,
 * `holiday_check` 与 `holidays` 选项, 不含 `script` 的 `dismiss` 选项, 以及闹钟错误到工具错误的转换.
 */
class ChimeToolsTest {

    @get:Rule
    val temp = TemporaryFolder()

    private var now = at("2026-11-05T08:00")
    private var up = 5 * MINUTE
    private var exactAllowed = true
    private val events = ArrayList<AlarmEvent>()

    private lateinit var repository: AlarmRepository
    private lateinit var schedule: AlarmSchedule
    private lateinit var rings: RingCoordinator
    private lateinit var timers: TimerEngine
    private lateinit var tools: ChimeTools

    @Before
    fun setUp() {
        val set = HolidayStore(
            temp.newFolder("holidays"), DirectorySource(RepositoryFiles.path("app/src/main/assets/holiday-calendars").toFile()),
            CatalogSignatureVerifier.official(),
        ).load()
        val data = HolidayData(set) { "CN" }
        val judge = HolidayJudge { date, region -> data.day(date, region) }
        val region: (String) -> String = { set.region(it).code }
        val validator = AlarmOptionsValidator(features = setOf(AlarmFeatures.ALARMS, AlarmFeatures.HOLIDAYS, AlarmFeatures.CHALLENGES))
        lateinit var created: AlarmSchedule
        repository = AlarmRepository(
            store = AlarmStore(File(temp.root, "alarms"), clock = { now }),
            validator = validator,
            clock = { now },
            deviceZone = { SHANGHAI },
            planner = { created.plan(it) },
            schedulingAllowed = { exactAllowed },
            holidayJudge = judge,
            holidayRegion = region,
        )
        val bus = AlarmEventBus().also { it.addListener(events::add) }
        created = AlarmSchedule(repository, Port(), bus, clock = { now }, deviceZone = { SHANGHAI }, judge = judge)
        schedule = created
        timers = TimerEngine(TimerStore(File(temp.root, TimerStore.FILE_NAME)) { validator.timerOptions(it) }, Timers(), clock = { now }, elapsed = { up }, boot = { 1 })
        rings = RingCoordinator(repository, schedule, bus, clock = { now }, timers = timers)
        repository.addListener { bus.emit(ChangeEvents.of(it, now)) }
        val hints = BindingHints(File(temp.root, BindingHints.FILE_NAME))
        val library = RingtoneLibrary(temp.root, { "audio/mpeg" }, { now })
        val api = AlarmApi(
            repository, rings, hints, library, { emptyList() }, validator = validator, clock = { now }, deviceZone = { SHANGHAI },
            locale = { Locale.ENGLISH }, source = AlarmContract.SOURCE_TOOL, by = AlarmContract.BY_TOOL, judge = judge, holidayRegion = region,
        )
        val stopwatch = Stopwatch(JsonDocumentFile(File(temp.root, Stopwatch.FILE_NAME), Stopwatch.FORMAT, Stopwatch.VERSION), { now }, { up }, { 1 })
        val timerApi = TimerApi(timers, stopwatch, rings, validator, clock = { now }, source = AlarmContract.SOURCE_TOOL, by = AlarmContract.BY_TOOL)
        val holidayApi = HolidayApi(data, clock = { now }, deviceZone = { SHANGHAI }, locale = { Locale.ENGLISH })
        tools = ChimeTools(api, timerApi, holidayApi) { SHANGHAI }
    }

    /** Calls [tool] with [json] arguments as the provider does and answers the decoded result. zh-CN: 像提供方那样调用. */
    @Suppress("UNCHECKED_CAST")
    private fun call(tool: String, json: String = "{}"): Map<String, Any?> =
        ToolJson.parseObject(ToolJson.encode(tools.call(tool, ToolJson.parseObject(json))))

    @Suppress("UNCHECKED_CAST")
    private fun alarm(answer: Map<String, Any?>): Map<String, Any?> = answer["alarm"] as Map<String, Any?>

    private fun add(json: String): Map<String, Any?> = alarm(call(ChimeToolCatalog.TOOL_ADD, json))

    @Suppress("UNCHECKED_CAST")
    private fun timer(answer: Map<String, Any?>): Map<String, Any?> = answer["timer"] as Map<String, Any?>

    private fun startTimer(json: String): Map<String, Any?> = timer(call(ChimeToolCatalog.TOOL_TIMER_START, json))

    private fun control(id: String, action: String, duration: String? = null): Map<String, Any?> =
        call(ChimeToolCatalog.TOOL_TIMER_CONTROL, """{"id": "$id", "action": "$action"${duration?.let { ", \"duration\": $it" } ?: ""}}""")

    @Suppress("UNCHECKED_CAST")
    private fun listedTimers(): List<Map<String, Any?>> = call(ChimeToolCatalog.TOOL_TIMER_LIST)["timers"] as List<Map<String, Any?>>

    private fun advance(millis: Long) {
        now += millis
        up += millis
    }

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
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "unknown argument beforeRing") { call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "beforeRing": {"minutes": 5}}""") }
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
    fun `holiday_check answers holidays, makeup workdays and its defaults (T192)`() {
        val national = call(ChimeToolCatalog.TOOL_HOLIDAY_CHECK, """{"date": "2026-10-01", "region": "cn"}""")
        assertEquals("2026-10-01", national["date"])
        assertEquals("the region is the canonical code", "CN", national["region"])
        assertEquals(true, national["isHoliday"])
        assertEquals(false, national["isWorkday"])
        assertTrue("name in the phone's language: ${national["name"]}", (national["name"] as String).contains("National Day"))
        assertEquals(false, national["estimated"])

        val makeup = call(ChimeToolCatalog.TOOL_HOLIDAY_CHECK, """{"date": "2026-10-10", "region": "CN"}""")
        assertEquals("Saturday 10-10 is a makeup workday", true, makeup["isWorkday"])
        assertEquals(false, makeup["isHoliday"])
        assertFalse("a makeup workday has no name", makeup.containsKey("name"))

        val today = call(ChimeToolCatalog.TOOL_HOLIDAY_CHECK)
        assertEquals("today in the phone's zone", "2026-11-05", today["date"])
        assertEquals("the settings default region", "CN", today["region"])
        assertEquals(true, today["isWorkday"])
        assertEquals(false, today["isHoliday"])

        val moment = call(ChimeToolCatalog.TOOL_HOLIDAY_CHECK, """{"date": ${at("2026-10-02T23:30")}}""")
        assertEquals("a timestamp counts by its day in the phone's zone", "2026-10-02", moment["date"])
        assertEquals(true, moment["isHoliday"])

        val estimated = call(ChimeToolCatalog.TOOL_HOLIDAY_CHECK, """{"date": "2027-10-01"}""")
        assertEquals(true, estimated["isHoliday"])
        assertEquals("CN 2027 is computed from the usual rules", true, estimated["estimated"])

        val subdivision = call(ChimeToolCatalog.TOOL_HOLIDAY_CHECK, """{"date": "2026-07-04", "region": "us-ca"}""")
        assertEquals("US-CA", subdivision["region"])
        assertEquals(true, subdivision["isHoliday"])
        assertEquals("a Saturday holiday is no workday", false, subdivision["isWorkday"])
    }

    @Test
    fun `holiday_check refuses years outside the data, unknown regions and bad arguments (T194)`() {
        val missing = failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "HOLIDAY_DATA_MISSING: ") {
            call(ChimeToolCatalog.TOOL_HOLIDAY_CHECK, """{"date": "2027-01-01", "region": "LK"}""")
        }
        assertTrue("the message gives the years covered: ${missing.message}", missing.message.contains("2026"))
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "HOLIDAY_DATA_MISSING: ") { call(ChimeToolCatalog.TOOL_HOLIDAY_CHECK, """{"region": "ZZ"}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "INVALID_OPTIONS: ") { call(ChimeToolCatalog.TOOL_HOLIDAY_CHECK, """{"region": "C1"}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "date") { call(ChimeToolCatalog.TOOL_HOLIDAY_CHECK, """{"date": "2026-13-01"}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "region must be a string") { call(ChimeToolCatalog.TOOL_HOLIDAY_CHECK, """{"region": 86}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "unknown argument mode") { call(ChimeToolCatalog.TOOL_HOLIDAY_CHECK, """{"mode": "skip"}""") }
    }

    @Test
    fun `the holidays option is taken, replaced whole and cleared (T193)`() {
        now = at("2026-09-30T08:00")
        val thursdays = add("""{"time": "07:00", "repeat": [4], "holidays": {"mode": "skip", "region": "cn"}}""")
        val id = thursdays["id"] as String
        assertEquals("an explicit region is canonical", mapOf("mode" to "skip", "region" to "CN"), thursdays["holidays"])
        assertEquals("National Day 10-01 is left out", at("2026-10-08T07:00"), thursdays["nextTrigger"])

        val implicit = alarm(call(ChimeToolCatalog.TOOL_UPDATE, """{"id": "$id", "holidays": {"mode": "only"}}"""))
        assertEquals("the whole filter is replaced, the region follows the default again", mapOf("mode" to "only"), implicit["holidays"])
        assertEquals(at("2026-10-01T07:00"), implicit["nextTrigger"])

        val cleared = alarm(call(ChimeToolCatalog.TOOL_UPDATE, """{"id": "$id", "clear": ["holidays"]}"""))
        assertFalse("no holiday filter", cleared.containsKey("holidays"))
        assertEquals(at("2026-10-01T07:00"), cleared["nextTrigger"])

        val workdays = add("""{"time": "07:00", "holidays": {"mode": "workdays"}}""")
        assertEquals("10-08 is the first workday after National Day", at("2026-10-08T07:00"), workdays["nextTrigger"])

        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "holidays.mode is required") { call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "holidays": {"region": "CN"}}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "unknown argument holidays.date") {
            call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "holidays": {"mode": "skip", "date": "2026-10-01"}}""")
        }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "INVALID_OPTIONS: ") { call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "holidays": {"mode": "never"}}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "INVALID_OPTIONS: ") {
            call(ChimeToolCatalog.TOOL_ADD, """{"at": "2026-10-09T07:00", "holidays": {"mode": "workdays"}}""")
        }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "HOLIDAY_DATA_MISSING: ") { call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "holidays": {"mode": "skip", "region": "ZZ"}}""") }
    }

    @Test
    fun `the dismiss option takes the built-in challenges, merges on update and is cleared (T290)`() {
        val math = add("""{"time": "07:00", "repeat": "FREQ=DAILY", "dismiss": {"challenge": "math", "math": {"difficulty": 3, "count": 5}, "allowSnooze": false}}""")
        val id = math["id"] as String
        assertEquals(mapOf("challenge" to "math", "math" to mapOf("difficulty" to 3L, "count" to 5L), "allowSnooze" to false), math["dismiss"])

        val merged = alarm(call(ChimeToolCatalog.TOOL_UPDATE, """{"id": "$id", "dismiss": {"allowSnooze": true}}"""))
        assertEquals("members left out keep their value", mapOf("challenge" to "math", "math" to mapOf("difficulty" to 3L, "count" to 5L), "allowSnooze" to true), merged["dismiss"])
        val typed = alarm(call(ChimeToolCatalog.TOOL_UPDATE, """{"id": "$id", "dismiss": {"challenge": "type", "type": {"text": "Good morning"}}}"""))
        assertEquals(
            mapOf("challenge" to "type", "math" to mapOf("difficulty" to 3L, "count" to 5L), "type" to mapOf("text" to "Good morning"), "allowSnooze" to true),
            typed["dismiss"],
        )
        val cleared = alarm(call(ChimeToolCatalog.TOOL_UPDATE, """{"id": "$id", "clear": ["dismiss"]}"""))
        assertFalse("no challenge", cleared.containsKey("dismiss"))

        val shake = add("""{"time": "07:00", "dismiss": {"challenge": "shake", "shake": {"count": 60}}, "autoSilenceMinutes": 45}""")
        assertEquals(mapOf("challenge" to "shake", "shake" to mapOf("count" to 60L)), shake["dismiss"])
        assertEquals(45L, shake["autoSilenceMinutes"])

        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, ChimeToolArguments.SCRIPT_NOT_FOR_TOOLS) {
            call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "dismiss": {"challenge": "script"}}""")
        }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, ChimeToolArguments.SCRIPT_NOT_FOR_TOOLS) {
            call(ChimeToolCatalog.TOOL_UPDATE, """{"id": "$id", "dismiss": {"script": {"fallback": "math"}}}""")
        }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "dismiss.challenge must be one of none / math / shake / type") {
            call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "dismiss": {"challenge": "puzzle"}}""")
        }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "unknown argument dismiss.math.level") {
            call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "dismiss": {"challenge": "math", "math": {"level": 2}}}""")
        }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "dismiss must be an object") { call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "dismiss": "math"}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "INVALID_OPTIONS: ") {
            call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "dismiss": {"challenge": "math", "math": {"difficulty": 4}}}""")
        }
        failure(PluginToolErrorCodes.LIMIT_EXCEEDED, "LIMIT_EXCEEDED: ") {
            call(ChimeToolCatalog.TOOL_ADD, """{"time": "07:00", "dismiss": {"challenge": "type", "type": {"text": "${"x".repeat(201)}"}}}""")
        }
    }

    @Test
    fun `a script challenge of a script shows, survives a partial update and is dismissed by tool (T290)`() {
        val id = repository.add(
            mapOf(
                AlarmOptionKeys.HOUR to 9, AlarmOptionKeys.MINUTE to 0, AlarmOptionKeys.REPEAT to "FREQ=DAILY",
                AlarmOptionKeys.DISMISS to mapOf(AlarmOptionKeys.CHALLENGE to AlarmContract.CHALLENGE_SCRIPT, AlarmOptionKeys.SCRIPT_FALLBACK to AlarmContract.CHALLENGE_MATH),
            ),
            AlarmContract.SOURCE_SCRIPT,
        ).id
        @Suppress("UNCHECKED_CAST")
        val listed = (call(ChimeToolCatalog.TOOL_LIST)["alarms"] as List<Map<String, Any?>>).single { it["id"] == id }
        assertEquals(mapOf("challenge" to "script", "script" to mapOf("fallback" to "math")), listed["dismiss"])

        val kept = alarm(call(ChimeToolCatalog.TOOL_UPDATE, """{"id": "$id", "dismiss": {"allowSnooze": false}}"""))
        assertEquals("the script challenge stays", mapOf("challenge" to "script", "script" to mapOf("fallback" to "math"), "allowSnooze" to false), kept["dismiss"])

        now = at("2026-11-05T09:00")
        schedule.reconcile(AlarmPlanner.Cause.TRIGGER, listOf(id))
        assertEquals(AlarmContract.EVENT_RING, events.last().type)
        assertEquals(AlarmContract.CHALLENGE_SCRIPT, events.last().challenge)
        assertEquals(mapOf("handled" to true), call(ChimeToolCatalog.TOOL_DISMISS, """{"id": "$id"}"""))
        assertEquals(AlarmContract.EVENT_DISMISS, events.last().type)
        assertEquals(AlarmContract.BY_TOOL, events.last().by)
        assertEquals("the event names the challenge in effect", AlarmContract.CHALLENGE_SCRIPT, events.last().challenge)

        val switched = alarm(call(ChimeToolCatalog.TOOL_UPDATE, """{"id": "$id", "dismiss": {"challenge": "shake"}}"""))
        assertEquals("the fallback goes with the script challenge", mapOf("challenge" to "shake", "allowSnooze" to false), switched["dismiss"])
    }

    @Test
    fun `alarm codes map onto tool codes and unknown tools are refused`() {
        val expected = mapOf(
            AlarmErrorCodes.INVALID_OPTIONS to PluginToolErrorCodes.INVALID_ARGUMENTS,
            AlarmErrorCodes.INVALID_RULE to PluginToolErrorCodes.INVALID_ARGUMENTS,
            AlarmErrorCodes.NOT_FOUND to PluginToolErrorCodes.INVALID_ARGUMENTS,
            AlarmErrorCodes.UNSUPPORTED to PluginToolErrorCodes.INVALID_ARGUMENTS,
            AlarmErrorCodes.HOLIDAY_DATA_MISSING to PluginToolErrorCodes.INVALID_ARGUMENTS,
            AlarmErrorCodes.LIMIT_EXCEEDED to PluginToolErrorCodes.LIMIT_EXCEEDED,
            AlarmErrorCodes.PERMISSION_REQUIRED to PluginToolErrorCodes.NOT_READY,
            AlarmErrorCodes.IO_FAILED to PluginToolErrorCodes.FAILED,
            AlarmErrorCodes.INTERNAL to PluginToolErrorCodes.FAILED,
        )
        expected.forEach { (alarm, tool) -> assertEquals(alarm, tool, ChimeTools.code(alarm)) }
        val e = ChimeTools.error(AlarmException.ioFailed("disk full"))
        assertEquals(PluginToolErrorCodes.FAILED, e.code)
        assertEquals("IO_FAILED: disk full", e.message)
        failure(PluginToolErrorCodes.UNKNOWN_TOOL) { call("stopwatch_start") }
    }

    @Test
    fun `timer_start takes milliseconds or h m s text and records the tool as source`() {
        val tea = startTimer("""{"duration": "25m", "label": "tea", "vibrate": false}""")
        val id = tea["id"] as String
        assertTrue(id, id.startsWith("t_"))
        assertEquals("tea", tea["label"])
        assertEquals(AlarmContract.TIMER_RUNNING, tea["state"])
        assertEquals(25 * MINUTE, tea["durationMillis"])
        assertEquals(25 * MINUTE, tea["remainingMillis"])
        assertEquals("25:00", tea["remaining"])
        assertEquals(now + 25 * MINUTE, tea["endsAt"])
        assertEquals("2026-11-05T08:25:00+08:00", tea["endsAtTime"])
        assertEquals(false, tea["vibrate"])
        assertEquals(AlarmContract.SOURCE_TOOL, timers.get(id)!!.source)

        val short = startTimer("""{"duration": 90000}""")
        assertEquals(90_000L, short["durationMillis"])
        assertEquals("1:30", short["remaining"])
        assertFalse("options never set are left out", short.containsKey("label") || short.containsKey("vibrate"))
        val long = startTimer("""{"duration": "1h30m", "sound": {"source": "silent"}, "speak": {"text": "done"}, "autoSilenceMinutes": 5, "vibrate": [0, 300]}""")
        assertEquals("1:30:00", long["remaining"])
        assertEquals(mapOf("source" to "silent"), long["sound"])
        assertEquals("done", (long["speak"] as Map<*, *>)["text"])
        assertEquals(5L, long["autoSilenceMinutes"])
        assertEquals(listOf(0L, 300L), long["vibrate"])

        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "duration is required") { call(ChimeToolCatalog.TOOL_TIMER_START) }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "INVALID_OPTIONS: duration must be") { call(ChimeToolCatalog.TOOL_TIMER_START, """{"duration": "25"}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "1 s - 100 h") { call(ChimeToolCatalog.TOOL_TIMER_START, """{"duration": 999}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "1 s - 100 h") { call(ChimeToolCatalog.TOOL_TIMER_START, """{"duration": "100h1s"}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "must be an integer") { call(ChimeToolCatalog.TOOL_TIMER_START, """{"duration": 1.5}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "milliseconds or text") { call(ChimeToolCatalog.TOOL_TIMER_START, """{"duration": true}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "unknown argument repeat") { call(ChimeToolCatalog.TOOL_TIMER_START, """{"duration": "5m", "repeat": "FREQ=DAILY"}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "no local files") { call(ChimeToolCatalog.TOOL_TIMER_START, """{"duration": "5m", "sound": {"source": "/sdcard/a.mp3"}}""") }

        repeat(17) { startTimer("""{"duration": "1m"}""") }
        failure(PluginToolErrorCodes.LIMIT_EXCEEDED, "20 timers") { call(ChimeToolCatalog.TOOL_TIMER_START, """{"duration": "1m"}""") }
        assertEquals(20, listedTimers().size)
    }

    @Test
    fun `timer_list lists the timers in the order they were started`() {
        assertEquals(mapOf("timers" to emptyList<Any?>()), call(ChimeToolCatalog.TOOL_TIMER_LIST))
        val first = startTimer("""{"duration": "10m"}""")["id"]
        val second = startTimer("""{"duration": "5m", "label": "eggs"}""")["id"]
        advance(MINUTE)
        val listed = listedTimers()
        assertEquals(listOf(first, second), listed.map { it["id"] })
        assertEquals(listOf("9:00", "4:00"), listed.map { it["remaining"] })
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "unknown argument limit") { call(ChimeToolCatalog.TOOL_TIMER_LIST, """{"limit": 5}""") }
    }

    @Test
    fun `timer_control pauses, adds time, resumes, resets and cancels (T127)`() {
        val id = startTimer("""{"duration": "10m"}""")["id"] as String
        advance(2 * MINUTE)
        val paused = timer(control(id, "pause"))
        assertEquals(AlarmContract.TIMER_PAUSED, paused["state"])
        assertEquals("8:00", paused["remaining"])
        assertFalse("a paused timer has no end", paused.containsKey("endsAt"))
        assertEquals(7 * MINUTE, timer(control(id, "add", "\"-1m\""))["remainingMillis"])
        assertEquals(9 * MINUTE, timer(control(id, "add", "120000"))["remainingMillis"])
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "the time left") { control(id, "add", "\"-9m\"") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "the time left") { control(id, "add", "\"100h\"") }
        val resumed = timer(control(id, "resume"))
        assertEquals(AlarmContract.TIMER_RUNNING, resumed["state"])
        assertEquals(now + 9 * MINUTE, resumed["endsAt"])
        val reset = timer(control(id, "reset"))
        assertEquals(AlarmContract.TIMER_PAUSED, reset["state"])
        assertEquals("10:00", reset["remaining"])

        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "action must be one of pause, resume, add, reset, cancel") { control(id, "stop") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "action must be one of") { call(ChimeToolCatalog.TOOL_TIMER_CONTROL, """{"id": "$id"}""") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "action add needs duration") { control(id, "add") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "duration only goes with action add") { control(id, "pause", "60000") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "NOT_FOUND: ") { control("t_missing", "pause") }
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "unknown argument millis") { call(ChimeToolCatalog.TOOL_TIMER_CONTROL, """{"id": "$id", "action": "add", "millis": 1000}""") }

        assertEquals(mapOf("id" to id, "removed" to true), control(id, "cancel"))
        assertEquals(mapOf("id" to id, "removed" to false), control(id, "cancel"))
        assertTrue("timers send no change events", events.isEmpty())
    }

    @Test
    fun `a ringing timer is snoozed and cancelled by tool, or dismissed with alarm_dismiss`() {
        val id = startTimer("""{"duration": "1m", "label": "tea"}""")["id"] as String
        advance(MINUTE)
        assertEquals(1, timers.reconcile(AlarmPlanner.Cause.TRIGGER))
        assertEquals(AlarmContract.EVENT_RING, events.last().type)
        assertEquals(AlarmContract.KIND_TIMER, events.last().kind)
        val ringing = listedTimers().single()
        assertEquals(AlarmContract.TIMER_RINGING, ringing["state"])
        assertEquals("0:00", ringing["remaining"])
        failure(PluginToolErrorCodes.INVALID_ARGUMENTS, "ringing") { control(id, "pause") }

        assertEquals(mapOf("handled" to true), call(ChimeToolCatalog.TOOL_SNOOZE))
        assertEquals(AlarmContract.EVENT_SNOOZE, events.last().type)
        assertEquals(AlarmContract.KIND_TIMER, events.last().kind)
        assertEquals(AlarmContract.BY_TOOL, events.last().by)
        assertEquals(mapOf("id" to id, "removed" to true), control(id, "cancel"))
        assertEquals(AlarmContract.EVENT_DISMISS, events.last().type)
        assertEquals(AlarmContract.KIND_TIMER, events.last().kind)
        assertEquals(AlarmContract.BY_TOOL, events.last().by)
        assertEquals(emptyList<Any?>(), listedTimers())

        val second = startTimer("""{"duration": "30s"}""")["id"] as String
        advance(30 * SECOND)
        timers.reconcile(AlarmPlanner.Cause.TRIGGER)
        assertEquals(mapOf("handled" to true), call(ChimeToolCatalog.TOOL_DISMISS, """{"id": "$second"}"""))
        assertEquals(AlarmContract.EVENT_DISMISS, events.last().type)
        assertNull(timers.get(second))
    }

    @Test
    fun `a missing exact alarm permission stops timer_start with NOT_READY`() {
        exactAllowed = false
        val e = failure(PluginToolErrorCodes.NOT_READY, "PERMISSION_REQUIRED: ") { call(ChimeToolCatalog.TOOL_TIMER_START, """{"duration": "5m"}""") }
        assertTrue(e.message.contains("permissions page"))
        assertFalse("a timer has no enabled option", e.message.contains("enabled false"))
    }

    /** Timers ring through the coordinator as on Android. zh-CN: 与 Android 上一样, 计时器经协调器响铃. */
    private inner class Timers : TimerPort {
        override fun canScheduleExact() = exactAllowed
        override fun schedule(record: TimerRecord) = Unit
        override fun cancel(timerId: String) = Unit
        override fun ring(record: TimerRecord, request: AlarmPlanner.RingRequest) = rings.offerTimer(record, request)
        override fun notifyMissed(record: TimerRecord, scheduledAt: Long) = Unit
        override fun removedSilently(timerId: String) = Unit
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
        const val SECOND = 1_000L
        const val MINUTE = 60 * SECOND

        fun at(text: String): Long = LocalDateTime.parse(text).atZone(SHANGHAI).toInstant().toEpochMilli()
    }
}
