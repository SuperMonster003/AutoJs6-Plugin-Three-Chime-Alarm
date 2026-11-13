package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.RepositoryFiles
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmOptionsValidator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.optionsOf
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.CatalogSignatureVerifier
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.DirectorySource
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayData
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayData.Issue
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayDataSet
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayJudge
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.NextOccurrence
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RepeatText
import org.autojs.plugin.alarm.api.AlarmContract.HOLIDAY_MODE_SKIP
import org.autojs.plugin.alarm.api.AlarmContract.HOLIDAY_MODE_WORKDAYS
import org.autojs.plugin.alarm.api.AlarmFeatures
import org.autojs.plugin.alarm.api.AlarmHolidayFilter
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import org.autojs.plugin.alarm.api.AlarmSpec
import org.autojs.plugin.alarm.api.AlarmTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/**
 * Roadmap P8.6 evidence on the bundled snapshot (T220 - T223): the holiday option of the edit page keeps the rule the
 * dates come from, the repeat dialog in `workdays` mode, the next ring and the holidays skipped before it, the list text
 * of `workdays` alarms (T184), and the facts behind the data status. Every patch is saved through a real repository with
 * the holiday validator.
 * zh-CN: 路线图 P8.6 证据, 基于内置快照 (T220 - T223): 编辑页的节假日选项保持日期所依据的规则, `workdays` 模式下的重复对话框, 下一次响铃及其之前跳过的
 * 节假日, `workdays` 闹钟的列表文案 (T184), 以及数据状态背后的事实. 每个 patch 都经带节假日校验的真实仓库保存.
 */
class AlarmDraftHolidayTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val snapshot = DirectorySource(RepositoryFiles.path("app/src/main/assets/holiday-calendars").toFile())

    private val set: HolidayDataSet by lazy { HolidayStore(temp.newFolder("holidays"), snapshot, CatalogSignatureVerifier.official()).load() }

    private var defaultRegion: String? = "CN"

    private val data: HolidayData by lazy { HolidayData(set) { defaultRegion } }

    private val judge = HolidayJudge { date, region -> data.day(date, region) }

    private val validator = AlarmOptionsValidator(features = setOf(AlarmFeatures.ALARMS, AlarmFeatures.HOLIDAYS))

    private val now = at("2026-09-30T08:00")

    private val repository by lazy {
        AlarmRepository(
            store = AlarmStore(File(temp.root, "alarms"), clock = { now }),
            validator = validator,
            clock = { now },
            deviceZone = { SHANGHAI },
            planner = AlarmRecord::state,
            holidayJudge = judge,
            holidayRegion = { set.region(it).code },
        )
    }

    private fun draft(vararg extra: Pair<String, Any?>): AlarmDraft = AlarmDraft(validator.options(optionsOf(7, 0, *extra)), dateAuto = true)

    private fun filter(mode: String, region: String? = "CN") = AlarmHolidayFilter(region = region, mode = mode)

    private fun texts(times: List<Long>): List<String> = times.map { Instant.ofEpochMilli(it).atZone(SHANGHAI).toLocalDateTime().toString() }

    private fun save(draft: AlarmDraft): AlarmSpec = repository.add(draft.toOptions(), "user").spec

    @Test
    fun `entering workdays keeps a rule it allows and drops any other`() {
        val once = draft().withHolidays(filter(HOLIDAY_MODE_WORKDAYS), SHANGHAI)
        assertNull("once becomes every workday", once.spec.repeat)
        assertEquals(HOLIDAY_MODE_WORKDAYS, once.spec.holidays?.mode)
        assertEquals("FREQ=DAILY", once.shownRepeat)
        assertEquals(listOf("2026-10-08T07:00", "2026-10-09T07:00", "2026-10-10T07:00"), texts(once.preview(3, now, SHANGHAI, judge = judge)))
        assertEquals(HOLIDAY_MODE_WORKDAYS, save(once).holidays?.mode)

        val weekly = draft("repeat" to "FREQ=WEEKLY;BYDAY=MO,WE").withHolidays(filter(HOLIDAY_MODE_WORKDAYS), SHANGHAI)
        assertNull("a weekly rule is not allowed with workdays (T180)", weekly.spec.repeat)
        save(weekly)

        val daily = draft("repeat" to "FREQ=DAILY;UNTIL=20261231").withHolidays(filter(HOLIDAY_MODE_WORKDAYS), SHANGHAI)
        assertEquals("FREQ=DAILY;UNTIL=20261231", daily.spec.repeat)
        assertEquals("FREQ=DAILY;UNTIL=20261231", save(daily).repeat)

        val at = AlarmDraft(validator.options(mapOf("at" to at("2026-10-02T09:15"))), dateAuto = false).withHolidays(filter(HOLIDAY_MODE_WORKDAYS), SHANGHAI)
        assertNull("an at alarm becomes a time alarm", at.spec.at)
        assertEquals(AlarmTime(9, 15), at.spec.time)
        save(at)
    }

    @Test
    fun `leaving workdays makes its every day explicit and a change between other modes keeps the rule`() {
        val workdays = draft("holidays" to mapOf("region" to "CN", "mode" to HOLIDAY_MODE_WORKDAYS))
        val skip = workdays.withHolidays(filter(HOLIDAY_MODE_SKIP), SHANGHAI)
        assertEquals("FREQ=DAILY", skip.spec.repeat)
        assertEquals(HOLIDAY_MODE_SKIP, skip.spec.holidays?.mode)
        assertEquals(
            "the data lists 10-01 to 10-03 and 10-05 to 10-07, Sunday 10-04 is an ordinary weekend day",
            listOf("2026-10-04T07:00", "2026-10-08T07:00", "2026-10-09T07:00", "2026-10-10T07:00"),
            texts(skip.preview(4, now, SHANGHAI, judge = judge)),
        )
        save(skip)

        val removed = workdays.withHolidays(null, SHANGHAI)
        assertEquals("FREQ=DAILY", removed.spec.repeat)
        assertNull(removed.spec.holidays)
        save(removed)

        val weekly = draft("repeat" to "FREQ=WEEKLY;BYDAY=TH", "holidays" to mapOf("region" to "CN", "mode" to HOLIDAY_MODE_SKIP))
        val region = weekly.withHolidays(filter(HOLIDAY_MODE_SKIP, "JP"), SHANGHAI)
        assertEquals("FREQ=WEEKLY;BYDAY=TH", region.spec.repeat)
        assertEquals("JP", region.spec.holidays?.region)
        assertEquals(weekly, weekly.withHolidays(weekly.spec.holidays, SHANGHAI))
    }

    @Test
    fun `the repeat dialog in workdays mode keeps, drops or turns the mode into skip`() {
        val workdays = draft("holidays" to mapOf("region" to "CN", "mode" to HOLIDAY_MODE_WORKDAYS))
        val daily = workdays.withRepeat("FREQ=DAILY", SHANGHAI)
        assertNull("every day stays every workday", daily.spec.repeat)
        assertEquals(HOLIDAY_MODE_WORKDAYS, daily.spec.holidays?.mode)

        val once = workdays.withRepeat(null, SHANGHAI)
        assertNull(once.spec.repeat)
        assertNull("once drops the filter", once.spec.holidays)

        val weekly = workdays.withRepeat("FREQ=WEEKLY;BYDAY=SA", SHANGHAI)
        assertEquals("FREQ=WEEKLY;BYDAY=SA", weekly.spec.repeat)
        assertEquals(HOLIDAY_MODE_SKIP, weekly.spec.holidays?.mode)
        assertEquals("CN", weekly.spec.holidays?.region)
        save(weekly)

        val previewed = workdays.preview(2, now, SHANGHAI, repeat = "FREQ=WEEKLY;BYDAY=SA", judge = judge)
        assertEquals("Saturday 10-03 is a holiday, 10-10 is a makeup workday", listOf("2026-10-10T07:00", "2026-10-17T07:00"), texts(previewed))
    }

    @Test
    fun `the next ring skips the holidays and counts them`() {
        val skip = draft("repeat" to "FREQ=DAILY", "holidays" to mapOf("region" to "CN", "mode" to HOLIDAY_MODE_SKIP))
        val next = skip.preview(1, now, SHANGHAI, judge = judge).single()
        assertEquals("2026-10-04T07:00", texts(listOf(next)).single())
        assertEquals("National Day: 10-01 to 10-03", 3, skip.holidaySkips(now, next, SHANGHAI, judge))
        assertEquals("and 10-05 to 10-07 before 10-08", 6, skip.holidaySkips(now, at("2026-10-08T07:00"), SHANGHAI, judge))
        assertEquals("without data nothing is skipped", listOf("2026-10-01T07:00"), texts(skip.preview(1, now, SHANGHAI)))
        assertEquals(0, skip.holidaySkips(now, next, SHANGHAI, HolidayJudge.NONE))

        val record = repository.add(skip.toOptions(), "user")
        assertEquals(3, NextOccurrence.holidaySkips(record.spec, now, next, SHANGHAI, judge))
        assertEquals("the count stops at the limit", 2, NextOccurrence.holidaySkips(record.spec, now, next, SHANGHAI, judge, limit = 2))
        assertEquals(0, NextOccurrence.holidaySkips(record.spec, next, now, SHANGHAI, judge))
        val plain = repository.add(optionsOf(7, 0, "repeat" to "FREQ=DAILY"), "user")
        assertEquals("no filter, nothing counted", 0, NextOccurrence.holidaySkips(plain.spec, now, next, SHANGHAI, judge))

        val workdays = draft("holidays" to mapOf("region" to "CN", "mode" to HOLIDAY_MODE_WORKDAYS))
        assertEquals("weekend holidays are no instances of workdays", 5, workdays.holidaySkips(now, at("2026-10-08T07:00"), SHANGHAI, judge))
    }

    @Test
    fun `a holiday change enables a disabled alarm and removing the filter patches null`() {
        val record = repository.add(optionsOf(7, 0, "repeat" to "FREQ=DAILY"), "user").let { repository.setEnabled(it.id, false, "user") }
        val update = AlarmDraft.of(record).withHolidays(filter(HOLIDAY_MODE_SKIP), SHANGHAI).toUpdate(record)
        assertEquals(mapOf("region" to "CN", "mode" to HOLIDAY_MODE_SKIP), update.patch[AlarmOptionKeys.HOLIDAYS])
        assertEquals(true, update.patch[AlarmOptionKeys.ENABLED])
        val saved = repository.update(record.id, update.patch, "user", update.implicitDate)
        assertTrue(saved.enabled)
        assertEquals(HOLIDAY_MODE_SKIP, saved.spec.holidays?.mode)

        val removal = AlarmDraft.of(saved).withHolidays(null, SHANGHAI).toUpdate(saved)
        assertTrue(removal.patch.containsKey(AlarmOptionKeys.HOLIDAYS))
        assertNull(removal.patch[AlarmOptionKeys.HOLIDAYS])
        assertNull(repository.update(saved.id, removal.patch, "user", removal.implicitDate).spec.holidays)
    }

    @Test
    fun `the list text of workdays alarms is the one of the script API`() {
        val workdays = save(draft().withHolidays(filter(HOLIDAY_MODE_WORKDAYS), SHANGHAI))
        assertEquals("Workdays", RepeatText.ofAlarm(workdays, Locale.ENGLISH))
        assertEquals("工作日", RepeatText.ofAlarm(workdays, Locale.SIMPLIFIED_CHINESE))
        val until = save(draft("repeat" to "FREQ=DAILY;UNTIL=20261231").withHolidays(filter(HOLIDAY_MODE_WORKDAYS), SHANGHAI))
        val text = RepeatText.ofAlarm(until, Locale.ENGLISH)
        assertTrue("an end date keeps the rule text: $text", text != null && text != "Workdays")
        assertNull("once has no repeat text", RepeatText.ofAlarm(save(draft()), Locale.ENGLISH))
        assertEquals("Every day", RepeatText.ofAlarm(save(draft("repeat" to "FREQ=DAILY")), Locale.ENGLISH))
    }

    @Test
    fun `the data status names estimated years, missing years and regions without data`() {
        assertEquals(listOf(Issue.Estimated("CN", 2027)), data.issues(listOf(null, "CN"), 2026))
        assertEquals(listOf(Issue.Estimated("CN", 2030)), data.issues(listOf("CN"), 2030))
        assertEquals(listOf(Issue.MissingYear("CN", 2040, 2020, 2035)), data.issues(listOf("CN"), 2040))
        val unknown = data.issues(listOf("ZZ"), 2026).single()
        assertTrue(unknown is Issue.NoData && unknown.region == "ZZ")
        assertTrue("a region without estimates has no issue in a covered year", data.issues(listOf("JP"), 2026).isEmpty())
        defaultRegion = null
        assertTrue("no default region on purpose is no issue", data.issues(listOf(null), 2026).isEmpty())
        defaultRegion = "ZZ"
        assertFalse("a default region the data lacks is one", data.issues(listOf(null), 2026).isEmpty())
    }

    private companion object {
        val SHANGHAI: ZoneId = ZoneId.of("Asia/Shanghai")

        fun at(text: String): Long = LocalDateTime.parse(text).atZone(SHANGHAI).toInstant().toEpochMilli()
    }
}
