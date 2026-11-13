package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.RepositoryFiles
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
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingCoordinator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingtoneLibrary
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.NextOccurrence
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RepeatText
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner.Cause
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedule
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedulePort
import org.autojs.plugin.alarm.api.AlarmContract.HOLIDAY_MODE_ONLY
import org.autojs.plugin.alarm.api.AlarmContract.HOLIDAY_MODE_SKIP
import org.autojs.plugin.alarm.api.AlarmContract.HOLIDAY_MODE_WORKDAYS
import org.autojs.plugin.alarm.api.AlarmErrorCodes.HOLIDAY_DATA_MISSING
import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_OPTIONS
import org.autojs.plugin.alarm.api.AlarmErrorCodes.UNSUPPORTED
import org.autojs.plugin.alarm.api.AlarmEvent
import org.autojs.plugin.alarm.api.AlarmFeatures
import org.autojs.plugin.alarm.api.AlarmResultKeys
import org.autojs.plugin.alarm.api.AlarmSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/**
 * Roadmap P8.3 evidence (T179 - T184, Q46 (a), Q47 (a), Q50) on the bundled snapshot: in `workdays` mode the CN mainland
 * 2026 makeup workdays ring and the holidays do not; `skip` and `only`; a year without data and an unknown region ring
 * by the weekday rules; `repeat` with `workdays`; `skip` events with reason `holiday` through the scheduler, a one-shot
 * alarm retired after its holiday; `upcoming` / `preview` filtered; explicit regions canonicalized; `repeatText`.
 * zh-CN: 路线图 P8.3 证据 (T179 - T184, Q46 (a), Q47 (a), Q50), 基于内置快照: `workdays` 模式下中国大陆 2026 年的补班日响, 节假日不响; `skip` 与
 * `only`; 没有数据的年份与未知地区按星期规则响; `workdays` 与 `repeat`; 经调度器发出原因为 `holiday` 的 `skip` 事件, 一次性闹钟在节假日后退役;
 * `upcoming` / `preview` 计入过滤; 显式地区规范化; `repeatText`.
 */
class HolidayFilterTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val snapshot = DirectorySource(RepositoryFiles.path("app/src/main/assets/holiday-calendars").toFile())

    private val set: HolidayDataSet by lazy { HolidayStore(temp.newFolder("holidays"), snapshot, CatalogSignatureVerifier.official()).load() }

    private var defaultRegion: String? = "CN"

    private val data: HolidayData by lazy { HolidayData(set) { defaultRegion } }

    private val judge = HolidayJudge { date, region -> data.day(date, region) }

    private val region: (String) -> String = { set.region(it).code }

    private val validator = AlarmOptionsValidator(features = setOf(AlarmFeatures.ALARMS, AlarmFeatures.HOLIDAYS))

    private var now = at("2026-09-30T08:00")

    private val port = Port()
    private val events = ArrayList<AlarmEvent>()

    private lateinit var repository: AlarmRepository
    private lateinit var schedule: AlarmSchedule
    private lateinit var api: AlarmApi

    @Before
    fun setUp() {
        lateinit var created: AlarmSchedule
        repository = AlarmRepository(
            store = AlarmStore(File(temp.root, "alarms"), clock = { now }),
            validator = validator,
            clock = { now },
            deviceZone = { SHANGHAI },
            planner = { created.plan(it) },
            holidayJudge = judge,
            holidayRegion = region,
        )
        val bus = AlarmEventBus().also { it.addListener(events::add) }
        created = AlarmSchedule(repository, port, bus, clock = { now }, deviceZone = { SHANGHAI }, judge = judge)
        schedule = created
        api = AlarmApi(
            repository, RingCoordinator(repository, schedule, bus, clock = { now }), BindingHints(File(temp.root, BindingHints.FILE_NAME)),
            RingtoneLibrary(temp.root, { null }, { now }), { emptyList() }, validator = validator, clock = { now }, deviceZone = { SHANGHAI },
            locale = { Locale.ENGLISH }, judge = judge, holidayRegion = region,
        )
    }

    private fun holidays(mode: String, region: String? = "CN"): Map<String, Any?> =
        linkedMapOf<String, Any?>().apply { region?.let { put("region", it) }; put("mode", mode) }

    private fun spec(mode: String, region: String? = "CN", vararg extra: Pair<String, Any?>): AlarmSpec =
        validator.options(optionsOf(7, 0, "holidays" to holidays(mode, region), *extra))

    private fun texts(times: List<Long>): List<String> = times.map { Instant.ofEpochMilli(it).atZone(SHANGHAI).toLocalDateTime().toString() }

    private fun state(id: String): AlarmRuntimeState = repository.get(id)!!.state

    @Test
    fun `workdays rings on the CN 2026 makeup workdays and never on a holiday`() {
        val spec = spec(HOLIDAY_MODE_WORKDAYS)
        assertEquals(
            "National Day: 10-01 to 10-07 off, Saturday 10-10 is a makeup workday",
            listOf("2026-10-08T07:00", "2026-10-09T07:00", "2026-10-10T07:00", "2026-10-12T07:00", "2026-10-13T07:00"),
            texts(NextOccurrence.preview(spec, 5, at("2026-09-30T08:00"), SHANGHAI, judge)),
        )
        assertEquals(
            "Spring Festival: Saturday 02-14 is a makeup workday, 02-15 to 02-23 off",
            listOf("2026-02-14T07:00", "2026-02-24T07:00", "2026-02-25T07:00"),
            texts(NextOccurrence.preview(spec, 3, at("2026-02-13T08:00"), SHANGHAI, judge)),
        )
        assertEquals(listOf("2026-01-04T07:00"), texts(NextOccurrence.preview(spec, 1, at("2026-01-03T08:00"), SHANGHAI, judge)))

        val filter = spec.holidays
        for (text in listOf("2026-01-04", "2026-02-14", "2026-02-28", "2026-05-09", "2026-09-20", "2026-10-10")) {
            val date = LocalDate.parse(text)
            assertTrue("$text falls on a weekend", date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY)
            assertEquals("makeup workday $text rings", HolidayFilter.Verdict.RING, HolidayFilter.verdict(filter, date, judge))
        }
        var date = LocalDate.of(2026, 1, 1)
        var holidays = 0
        var weekdayHolidays = 0
        while (date.year == 2026) {
            val day = data.day(date, "CN")!!
            val verdict = HolidayFilter.verdict(filter, date, judge)
            if (day.holiday) {
                holidays++
                assertNotEquals("holiday $date is silent", HolidayFilter.Verdict.RING, verdict)
                if (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) {
                    weekdayHolidays++
                    assertEquals("a holiday on a weekday is a skip with reason holiday: $date", HolidayFilter.Verdict.HOLIDAY, verdict)
                } else {
                    assertEquals("a holiday on a weekend is no instance: $date", HolidayFilter.Verdict.NONE, verdict)
                }
            } else if (day.makeupWorkday) {
                assertEquals(HolidayFilter.Verdict.RING, verdict)
            } else if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
                assertEquals("an ordinary weekend day is no instance: $date", HolidayFilter.Verdict.NONE, verdict)
            } else {
                assertEquals("an ordinary weekday rings: $date", HolidayFilter.Verdict.RING, verdict)
            }
            date = date.plusDays(1)
        }
        assertTrue("CN 2026 has its holidays: $holidays", holidays >= 20)
        assertTrue("several of them on weekdays: $weekdayHolidays", weekdayHolidays >= 10)
    }

    @Test
    fun `skip leaves out the holidays and only keeps them`() {
        val thursdays = spec(HOLIDAY_MODE_SKIP, "CN", "repeat" to "FREQ=WEEKLY;BYDAY=TH", "date" to "2026-09-30")
        assertEquals(listOf("2026-10-08T07:00", "2026-10-15T07:00"), texts(NextOccurrence.preview(thursdays, 2, now, SHANGHAI, judge)))
        assertEquals(at("2026-10-01T07:00"), NextOccurrence.nextHolidaySkip(thursdays, now, at("2026-10-08T07:00"), SHANGHAI, judge))
        assertNull("no holiday skip before the next ring", NextOccurrence.nextHolidaySkip(thursdays, at("2026-10-01T08:00"), at("2026-10-08T07:00"), SHANGHAI, judge))

        val only = spec(HOLIDAY_MODE_ONLY, "CN", "repeat" to "FREQ=DAILY", "date" to "2026-09-30")
        assertEquals(listOf("2026-10-01T07:00", "2026-10-02T07:00", "2026-10-03T07:00"), texts(NextOccurrence.preview(only, 3, now, SHANGHAI, judge)))
        assertNull("only sends no skip events (Q50)", NextOccurrence.nextHolidaySkip(only, at("2026-10-08T08:00"), at("2027-03-01T00:00"), SHANGHAI, judge))
        val after = NextOccurrence.preview(only, 1, at("2026-10-08T08:00"), SHANGHAI, judge).single()
        val date = Instant.ofEpochMilli(after).atZone(SHANGHAI).toLocalDate()
        assertTrue("the next ring after National Day is a holiday: $date", data.day(date, "CN")!!.holiday)
    }

    @Test
    fun `a year without data or an unknown region rings by the weekday rules`() {
        // LK has no data for 2027 (P8.2): workdays rings Monday to Friday, skip and only ring every day of the rule.
        // zh-CN: LK 没有 2027 年的数据 (P8.2): workdays 周一至周五响, skip 与 only 按规则每天响.
        val start = at("2027-01-14T08:00")
        assertEquals(listOf("2027-01-15T07:00", "2027-01-18T07:00", "2027-01-19T07:00"), texts(NextOccurrence.preview(spec(HOLIDAY_MODE_WORKDAYS, "LK"), 3, start, SHANGHAI, judge)))
        val daily = listOf("2027-01-15T07:00", "2027-01-16T07:00", "2027-01-17T07:00")
        assertEquals(daily, texts(NextOccurrence.preview(spec(HOLIDAY_MODE_SKIP, "LK", "repeat" to "FREQ=DAILY"), 3, start, SHANGHAI, judge)))
        assertEquals(daily, texts(NextOccurrence.preview(spec(HOLIDAY_MODE_ONLY, "LK", "repeat" to "FREQ=DAILY"), 3, start, SHANGHAI, judge)))

        // Data that cannot be opened (the judge knows nothing): workdays falls back to Saturday and Sunday.
        // zh-CN: 数据无法打开 (判定一无所知): workdays 退回周六日为周末.
        val workdays = spec(HOLIDAY_MODE_WORKDAYS)
        assertEquals(
            listOf("2026-10-02T07:00", "2026-10-05T07:00", "2026-10-06T07:00"),
            texts(NextOccurrence.preview(workdays, 3, at("2026-10-01T08:00"), SHANGHAI, HolidayJudge.NONE)),
        )
        assertEquals(daily, texts(NextOccurrence.preview(spec(HOLIDAY_MODE_SKIP, "CN", "repeat" to "FREQ=DAILY"), 3, start, SHANGHAI, HolidayJudge.NONE)))

        // An omitted region follows the default region, and none at all is unknown. zh-CN: 省略地区时随默认地区, 没有默认地区即未知.
        val omitted = spec(HOLIDAY_MODE_WORKDAYS, null)
        assertEquals(listOf("2026-10-08T07:00"), texts(NextOccurrence.preview(omitted, 1, now, SHANGHAI, judge)))
        defaultRegion = null
        assertEquals(listOf("2026-10-01T07:00"), texts(NextOccurrence.preview(omitted, 1, now, SHANGHAI, judge)))
    }

    @Test
    fun `workdays takes hour and minute with repeat omitted or daily`() {
        fun options(vararg extra: Pair<String, Any?>) = optionsOf(7, 0, "holidays" to holidays(HOLIDAY_MODE_WORKDAYS), *extra)
        assertNull(validator.options(options()).repeat)
        assertEquals("FREQ=DAILY", validator.options(options("repeat" to "FREQ=DAILY")).repeat)
        validator.options(options("repeat" to "FREQ=DAILY;UNTIL=20261231"))
        for (rule in listOf("FREQ=WEEKLY", "FREQ=DAILY;INTERVAL=2", "FREQ=DAILY;COUNT=5", "FREQ=DAILY;BYDAY=MO,TU", "FREQ=DAILY;BYMONTH=1", "FREQ=MONTHLY")) {
            assertAlarmError(INVALID_OPTIONS, "workdays") { validator.options(options("repeat" to rule)) }
        }
        assertAlarmError(INVALID_OPTIONS, "workdays") { validator.options(mapOf("at" to now + 3_600_000L, "holidays" to holidays(HOLIDAY_MODE_WORKDAYS))) }
        validator.options(mapOf("at" to now + 3_600_000L, "holidays" to holidays(HOLIDAY_MODE_SKIP)))
        validator.options(optionsOf(7, 0, "repeat" to "FREQ=WEEKLY;BYDAY=MO", "holidays" to holidays(HOLIDAY_MODE_ONLY)))
        assertAlarmError(UNSUPPORTED, "holidays") { AlarmOptionsValidator().options(options()) }

        assertTrue(HolidayFilter.repeats(validator.options(options())))
        assertFalse(HolidayFilter.repeats(validator.options(optionsOf(7, 0, "holidays" to holidays(HOLIDAY_MODE_SKIP)))))
    }

    @Test
    fun `the scheduler sends skip with reason holiday and rings the makeup workday`() {
        val alarm = repository.add(optionsOf(7, 0, "holidays" to holidays(HOLIDAY_MODE_WORKDAYS)), "script")
        assertEquals("2026-09-30", alarm.spec.date)
        assertEquals(at("2026-10-08T07:00"), alarm.state.nextTrigger)
        assertEquals(at("2026-10-01T07:00"), alarm.state.holidaySkipAt)
        assertEquals(at("2026-10-01T07:00"), port.skipWakes[alarm.id])
        assertEquals(listOf("2026-10-08T07:00"), texts(listOf(alarm.state.nextTrigger!!)))
        assertEquals(at("2026-10-08T07:00"), port.wakes[alarm.id])

        for (day in listOf("2026-10-01", "2026-10-02", "2026-10-05", "2026-10-06", "2026-10-07")) {
            val t = at("${day}T07:00")
            assertEquals(t, state(alarm.id).holidaySkipAt)
            now = t
            val outcome = schedule.reconcile(Cause.TRIGGER, listOf(alarm.id)).getValue(alarm.id)
            assertNull("no ring on holiday $day", outcome.ring)
            val event = outcome.events.single()
            assertEquals("skip", event.type)
            assertEquals("holiday", event.reason)
            assertEquals(t, event.scheduledAt)
            assertEquals(alarm.id, event.alarmId)
            // The same broadcast again sends nothing. zh-CN: 同一广播再来一次不再发事件.
            assertTrue(schedule.reconcile(Cause.TRIGGER, listOf(alarm.id)).getValue(alarm.id).events.isEmpty())
        }
        assertNull(state(alarm.id).holidaySkipAt)
        assertEquals(5, events.count { it.type == "skip" && it.reason == "holiday" })
        assertTrue("no skip for the weekend of the holidays", events.none { it.scheduledAt == at("2026-10-03T07:00") || it.scheduledAt == at("2026-10-04T07:00") })

        for (day in listOf("2026-10-08", "2026-10-09", "2026-10-10")) {
            now = at("${day}T07:00")
            assertEquals(now, schedule.reconcile(Cause.TRIGGER, listOf(alarm.id)).getValue(alarm.id).ring?.scheduledAt)
        }
        assertEquals("Sunday 10-11 is no instance", at("2026-10-12T07:00"), state(alarm.id).nextTrigger)
        assertEquals(3, port.rings.size)
        assertTrue(port.missed.isEmpty())
        assertTrue(repository.get(alarm.id)!!.enabled)
    }

    @Test
    fun `a one-shot alarm on a holiday sends skip and retires`() {
        val alarm = repository.add(optionsOf(9, 0, "date" to "2026-10-01", "holidays" to holidays(HOLIDAY_MODE_SKIP)), "user")
        assertTrue(alarm.enabled)
        assertNull(alarm.state.nextTrigger)
        assertEquals(at("2026-10-01T09:00"), alarm.state.holidaySkipAt)

        // The implicit date of a one-shot alarm is not moved past holidays (T183). zh-CN: 一次性闹钟的隐式日期不跳过节假日 (T183).
        val implicit = repository.add(optionsOf(7, 0, "holidays" to holidays(HOLIDAY_MODE_SKIP)), "user")
        assertEquals("2026-10-01", implicit.spec.date)
        assertEquals(at("2026-10-01T07:00"), implicit.state.holidaySkipAt)

        now = at("2026-10-01T09:00")
        val outcome = schedule.reconcile(Cause.TRIGGER, listOf(alarm.id)).getValue(alarm.id)
        assertEquals("holiday", outcome.events.single().reason)
        assertTrue(outcome.retire)
        val retired = repository.get(alarm.id)!!
        assertFalse(retired.enabled)
        assertNull(port.wakes[alarm.id])
        assertNull(port.skipWakes[alarm.id])

        // skipNext of a filtered alarm skips the next ring, not a holiday. zh-CN: 过滤闹钟的 skipNext 跳过下一次响铃, 而非节假日.
        val workdays = repository.add(optionsOf(7, 0, "holidays" to holidays(HOLIDAY_MODE_WORKDAYS)), "user")
        assertEquals(at("2026-10-08T07:00"), repository.skipNext(workdays.id, "script").state.skipAt)
        assertEquals(at("2026-10-09T07:00"), state(workdays.id).nextTrigger)
    }

    @Test
    fun `explicit regions are canonical and the api counts the filter`() {
        val added = repository.add(optionsOf(7, 0, "holidays" to holidays(HOLIDAY_MODE_WORKDAYS, " cn ")), "script")
        assertEquals("CN", added.spec.holidays?.region)
        assertNull(repository.add(optionsOf(7, 0, "holidays" to holidays(HOLIDAY_MODE_WORKDAYS, null)), "script").spec.holidays?.region)
        assertAlarmError(HOLIDAY_DATA_MISSING, "ZZ") { repository.add(optionsOf(7, 0, "holidays" to holidays(HOLIDAY_MODE_SKIP, "ZZ")), "script") }
        assertAlarmError(INVALID_OPTIONS, "region") { repository.add(optionsOf(7, 0, "holidays" to holidays(HOLIDAY_MODE_SKIP, "C1")), "script") }
        assertEquals("CN", repository.update(added.id, mapOf("holidays" to holidays(HOLIDAY_MODE_SKIP, "cn")), "script").spec.holidays?.region)
        repository.remove(added.id, "script")
        repository.list().forEach { repository.remove(it.id, "script") }

        @Suppress("UNCHECKED_CAST")
        val alarm = api.addAlarm(optionsOf(7, 0, "holidays" to holidays(HOLIDAY_MODE_WORKDAYS, "cn")))[AlarmResultKeys.ALARM] as Map<String, Any?>
        assertEquals("Workdays", alarm["repeatText"])
        assertEquals("CN", (alarm["holidays"] as Map<*, *>)["region"])
        @Suppress("UNCHECKED_CAST")
        val occurrences = api.upcoming(mapOf("count" to 3))[AlarmResultKeys.OCCURRENCES] as List<Map<String, Any?>>
        assertEquals(listOf("2026-10-08T07:00", "2026-10-09T07:00", "2026-10-10T07:00"), texts(occurrences.map { it["at"] as Long }))
        val preview = api.preview(mapOf("options" to optionsOf(7, 0, "holidays" to holidays(HOLIDAY_MODE_WORKDAYS, "cn")), "count" to 2))
        assertEquals(listOf("2026-10-08T07:00", "2026-10-09T07:00"), texts((preview[AlarmResultKeys.TIMES] as LongArray).toList()))
        assertAlarmError(HOLIDAY_DATA_MISSING) { api.preview(mapOf("options" to optionsOf(7, 0, "holidays" to holidays(HOLIDAY_MODE_SKIP, "ZZ")))) }

        @Suppress("UNCHECKED_CAST")
        val until = api.addAlarm(optionsOf(7, 0, "repeat" to "FREQ=DAILY;UNTIL=20261231", "holidays" to holidays(HOLIDAY_MODE_WORKDAYS)))[AlarmResultKeys.ALARM] as Map<String, Any?>
        assertNotEquals("an end date keeps the rule text", "Workdays", until["repeatText"])
    }

    @Test
    fun `the holiday skip survives the store and repeat texts are ASCII`() {
        val state = AlarmRuntimeState(nextTrigger = 20L, skipAt = 30L, holidaySkipAt = 10L)
        assertEquals(state, AlarmRuntimeState.fromValues(state.toValues()))
        assertEquals(10L, state.skipWakeAt)
        assertEquals(30L, state.copy(holidaySkipAt = null).skipWakeAt)
        assertNull(AlarmRuntimeState().skipWakeAt)

        val texts = RepeatText.LANGUAGES.associateWith { RepeatText.workdays(Locale.forLanguageTag(it)) }
        assertEquals("Workdays", texts["en"])
        assertEquals("工作日", texts["zh-Hans"])
        assertEquals("the three Chinese scripts share the word", 8, texts.values.toSet().size)
        for ((language, text) in texts) {
            assertTrue("$language: $text", text.isNotBlank() && text.none { it.code in FULL_WIDTH_PUNCTUATION })
        }
    }

    private inner class Port : AlarmSchedulePort {
        val wakes = HashMap<String, Long?>()
        val skipWakes = HashMap<String, Long?>()
        val rings = ArrayList<AlarmPlanner.RingRequest>()
        val missed = ArrayList<Pair<String, AlarmEvent>>()

        override fun canScheduleExact() = true

        override fun schedule(record: AlarmRecord) {
            wakes[record.id] = record.state.wakeAt.takeIf { record.enabled }
            skipWakes[record.id] = record.state.skipWakeAt.takeIf { record.enabled }
        }

        override fun cancel(alarmId: String) {
            wakes[alarmId] = null
            skipWakes[alarmId] = null
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

        fun at(text: String): Long = LocalDateTime.parse(text).atZone(SHANGHAI).toInstant().toEpochMilli()

        /** Full-width comma, period, colon, parentheses, Arabic comma and question mark. zh-CN: 全角逗号, 句号, 冒号, 括号, 阿拉伯逗号与问号. */
        val FULL_WIDTH_PUNCTUATION = setOf(0xFF0C, 0x3002, 0xFF1A, 0xFF08, 0xFF09, 0x060C, 0x061F)
    }
}
