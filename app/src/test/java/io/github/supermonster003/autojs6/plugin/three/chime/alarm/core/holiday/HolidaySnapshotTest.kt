package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.RepositoryFiles
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.assertAlarmError
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.HolidayApi
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes.HOLIDAY_DATA_MISSING
import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_OPTIONS
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import org.autojs.plugin.alarm.api.AlarmResultKeys
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/**
 * The bundled holiday snapshot and the read side of P8.2 (roadmap P8.2 evidence): the snapshot verifies with the official
 * key (Q45 (a)) and every calendar matches its catalog hash and parses; CN mainland 2026 with its makeup workdays; a year
 * without data (LK 2027) is reported missing, never as a holiday or a workday; estimated years (CN from 2027, Q47 (a)) are
 * judged normally but marked; regions are canonicalized (T170).
 * zh-CN: 内置节假日快照与 P8.2 的读取侧 (路线图 P8.2 证据): 快照用官方公钥校验通过 (Q45 (a)), 每份日历与目录哈希一致且能解析; 中国大陆 2026 年及其
 * 补班; 没有数据的年份 (LK 2027) 报告为缺失, 既不当节假日也不当工作日; 推算年份 (CN 自 2027 年起, Q47 (a)) 正常判定但带标记; 地区规范化 (T170).
 */
class HolidaySnapshotTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val snapshot = DirectorySource(RepositoryFiles.path("app/src/main/assets/holiday-calendars").toFile())

    private val set: HolidayDataSet by lazy { HolidayStore(temp.newFolder("holidays"), snapshot, CatalogSignatureVerifier.official()).load() }

    private fun data(default: String? = "CN") = HolidayData(set) { default }

    private fun api(default: String? = "CN", regions: List<String?> = emptyList(), year: Int = 2026) = HolidayApi(
        data(default),
        alarmRegions = { regions },
        clock = { ZonedDateTime.of(year, 6, 1, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli() },
        deviceZone = { ZoneId.of("UTC") },
        locale = { Locale.US },
    )

    @Test
    fun bundledSnapshotVerifiesAndEveryCalendarMatchesItsHash() {
        val catalog = set.catalog
        assertEquals(1, catalog.version)
        assertEquals("holiday-calendars-v1", catalog.releaseTag)
        assertEquals("python-holidays 0.108", catalog.source.label)
        assertNull("the bundled set is not a downloaded one", set.directory)
        val files = RepositoryFiles.path("app/src/main/assets/holiday-calendars").toFile().list().orEmpty().filter { it.startsWith("holiday-calendar-") }.toSet()
        assertEquals("one calendar file per region and nothing else", catalog.regions.map { it.file.name }.toSet(), files)
        for (region in catalog.regions) {
            val calendar = set.calendar(region)
            assertEquals(region.code, calendar.region)
            assertEquals(region.code, region.firstYear, calendar.firstYear)
            assertEquals(region.code, region.lastYear, calendar.lastYear)
            assertEquals(region.code, region.estimatedFrom, calendar.estimatedFrom)
            assertEquals(region.code, region.hasMakeupWorkdays, calendar.hasMakeupWorkdays)
            assertEquals(region.code, region.subdivisions.map { it.code }.toSet(), calendar.subdivisionCodes)
        }
    }

    @Test
    fun bundledSnapshotRefusesAnotherKey() {
        val other = HolidayStoreTest.TestKey()
        assertThrowsHolidayData { HolidayStore(temp.newFolder("other"), snapshot, other.verifier).load() }
    }

    @Test
    fun chinaMainland2026FollowsTheAnnouncedMakeupWorkdays() {
        val data = data()
        fun day(text: String) = data.dayIn(LocalDate.parse(text), null, Locale.US)
        for (text in listOf("2026-01-01", "2026-02-16", "2026-02-20", "2026-02-23", "2026-05-05", "2026-06-19", "2026-09-25", "2026-10-01", "2026-10-07")) {
            val day = day(text)
            assertTrue("$text is a holiday", day.holiday)
            assertFalse("$text is not a workday", day.workday)
            assertNotNull("$text has a name", day.name)
        }
        assertTrue(day("2026-10-01").name!!.contains("National Day"))
        for (text in listOf("2026-01-04", "2026-02-14", "2026-02-28", "2026-05-09", "2026-09-20", "2026-10-10")) {
            val day = day(text)
            assertTrue("$text falls on a weekend", day.weekend)
            assertTrue("$text is a makeup workday", day.makeupWorkday)
            assertTrue("$text is a workday", day.workday)
            assertFalse(day.holiday)
        }
        assertTrue("an ordinary Sunday is a day off", day("2026-10-11").dayOff)
        assertTrue("an ordinary Monday is a workday", day("2026-03-02").workday)
        assertFalse(day("2026-03-02").estimated)
        assertFalse(day("2026-03-02").missing)
    }

    @Test
    fun aYearWithoutDataIsMissingNeverAHolidayOrAWorkdayAnswer() {
        val data = data("LK")
        val lk = set.catalog.region("LK")!!
        assertEquals("LK is the region whose data ends first", 2026, lk.lastYear)
        val day = data.day(LocalDate.of(2027, 1, 15), null)
        assertNotNull("the judge knows the region, so it answers", day)
        assertTrue(day!!.missing)
        assertFalse(day.holiday)
        assertFalse(day.makeupWorkday)
        assertAlarmError(HOLIDAY_DATA_MISSING, "2027") { api("LK").checkDate(mapOf(AlarmOptionKeys.DATE to "2027-01-15")) }
        assertAlarmError(HOLIDAY_DATA_MISSING, "2027") { api("LK").listHolidays(mapOf(AlarmOptionKeys.YEAR to 2027L, AlarmOptionKeys.REGION to "LK")) }
        val warnings = api("CN", regions = listOf("LK"), year = 2027).warnings()
        assertTrue(warnings.toString(), warnings.any { it.contains("LK has no year 2027") && it.contains("weekday rules only") })
        assertTrue("2026 is still covered", api("CN", regions = listOf("LK"), year = 2026).warnings().none { it.contains("LK") })
    }

    @Test
    fun estimatedYearsAreJudgedNormallyAndMarked() {
        val cn = set.catalog.region("CN")!!
        assertEquals(2027, cn.estimatedFrom)
        val day = data().dayIn(LocalDate.of(2027, 10, 1), null, Locale.US)
        assertTrue("an estimated holiday still counts", day.holiday)
        assertTrue(day.estimated)
        val answer = api().checkDate(mapOf(AlarmOptionKeys.DATE to "2027-10-01"))
        assertEquals(true, answer[AlarmResultKeys.IS_HOLIDAY])
        assertEquals(true, answer[HolidayApi.KEY_ESTIMATED])
        assertEquals(false, api().checkDate(mapOf(AlarmOptionKeys.DATE to "2026-10-01"))[HolidayApi.KEY_ESTIMATED])
        val warnings2026 = api(year = 2026).warnings()
        assertTrue(warnings2026.toString(), warnings2026.any { it.contains("CN from 2027 on is estimated") })
        assertTrue(api(year = 2028).warnings().any { it.contains("CN from 2028 on is estimated") })
        assertTrue("no warning before the year ahead is estimated", api(year = 2025).warnings().isEmpty())
        @Suppress("UNCHECKED_CAST")
        val estimated = (api().info(emptyMap())[AlarmResultKeys.HOLIDAY_INFO] as Map<String, Any?>)[HolidayApi.KEY_ESTIMATED] as List<Map<String, Any?>>
        val entry = estimated.single { it[HolidayApi.KEY_REGION] == "CN" }
        assertArrayEquals(longArrayOf(2027, cn.lastYear.toLong()), entry[HolidayApi.KEY_YEARS] as LongArray)
    }

    @Test
    fun regionsAreCanonicalized() {
        val data = data()
        assertEquals("CN", data.resolve("cn").code)
        assertEquals("CN", data.resolve(null).code)
        assertEquals("US-CA", data.resolve("us-ca").code)
        assertEquals("AU-NSW Western", data.resolve("AU-nsw western").code)
        assertEquals("BE-Flemish", data.resolve("BE-Flemish").code)
        assertAlarmError(HOLIDAY_DATA_MISSING, "XX") { data.resolve("XX") }
        assertAlarmError(HOLIDAY_DATA_MISSING, "US-ZZ") { data.resolve("US-ZZ") }
        assertAlarmError(INVALID_OPTIONS) { data.resolve("U") }
        assertAlarmError(INVALID_OPTIONS) { data.resolve("US-") }
        assertAlarmError(INVALID_OPTIONS) { data.resolve("USA") }
        assertAlarmError(HOLIDAY_DATA_MISSING, "default region") { data(null).resolve(null) }
        assertNull("the rule engine gets null without a default region", data(null).day(LocalDate.of(2026, 1, 1), null))
    }

    @Test
    fun theDefaultRegionIsInferredFromTheLocaleCountry() {
        assertEquals("CN", HolidayData.inferRegion(set.catalog, Locale.CHINA))
        assertEquals("US", HolidayData.inferRegion(set.catalog, Locale.US))
        assertNull(HolidayData.inferRegion(set.catalog, Locale.ENGLISH))
        assertNull(HolidayData.inferRegion(set.catalog, Locale("en", "001")))
    }

    @Test
    fun holidayMethodsAnswerTheirContractShapes() {
        val api = api()
        @Suppress("UNCHECKED_CAST")
        val regions = api.regions(emptyMap())[AlarmResultKeys.REGIONS] as List<Map<String, Any?>>
        assertEquals(set.catalog.regions.size, regions.size)
        val cn = regions.single { it[AlarmResultKeys.CODE] == "CN" }
        assertEquals(true, cn[AlarmResultKeys.HAS_MAKEUP_WORKDAYS])
        assertEquals(2027, cn[HolidayApi.KEY_ESTIMATED_FROM])
        assertEquals(2020, cn[AlarmResultKeys.FIRST_YEAR])
        val us = regions.single { it[AlarmResultKeys.CODE] == "US" }
        @Suppress("UNCHECKED_CAST")
        assertTrue((us[HolidayApi.KEY_SUBDIVISIONS] as List<Map<String, Any?>>).any { it[AlarmResultKeys.CODE] == "CA" })
        assertAlarmError(INVALID_OPTIONS) { api.regions(mapOf("region" to "CN")) }

        val workday = api.checkDate(mapOf(AlarmOptionKeys.DATE to "2026-10-10", AlarmOptionKeys.REGION to "cn"))
        assertEquals(false, workday[AlarmResultKeys.IS_HOLIDAY])
        assertEquals(true, workday[AlarmResultKeys.IS_WORKDAY])
        assertFalse(AlarmResultKeys.NAME in workday)
        assertAlarmError(INVALID_OPTIONS) { api.checkDate(mapOf(AlarmOptionKeys.DATE to "2026-02-30")) }
        assertAlarmError(INVALID_OPTIONS) { api.checkDate(mapOf(AlarmOptionKeys.DATE to "2026-2-3")) }
        assertAlarmError(INVALID_OPTIONS) { api.checkDate(emptyMap()) }
        assertAlarmError(HOLIDAY_DATA_MISSING, "2019") { api.checkDate(mapOf(AlarmOptionKeys.DATE to "2019-10-01")) }

        @Suppress("UNCHECKED_CAST")
        val entries = api.listHolidays(mapOf(AlarmOptionKeys.YEAR to 2026L))[AlarmResultKeys.HOLIDAY_ENTRIES] as List<Map<String, Any?>>
        assertEquals("entries are sorted by date", entries.map { it[AlarmResultKeys.DATE] as String }.sorted(), entries.map { it[AlarmResultKeys.DATE] })
        val makeup = entries.filter { it[AlarmResultKeys.TYPE] == AlarmContract.HOLIDAY_TYPE_WORKDAY }.map { it[AlarmResultKeys.DATE] }
        assertEquals(listOf("2026-01-04", "2026-02-14", "2026-02-28", "2026-05-09", "2026-09-20", "2026-10-10"), makeup)
        assertTrue(entries.filter { it[AlarmResultKeys.TYPE] == AlarmContract.HOLIDAY_TYPE_WORKDAY }.all { it[AlarmResultKeys.NAME] == "" })
        assertTrue(entries.any { it[AlarmResultKeys.DATE] == "2026-10-01" && it[AlarmResultKeys.TYPE] == AlarmContract.HOLIDAY_TYPE_HOLIDAY })
        assertAlarmError(INVALID_OPTIONS) { api.listHolidays(emptyMap()) }
        assertAlarmError(INVALID_OPTIONS) { api.listHolidays(mapOf(AlarmOptionKeys.YEAR to 0L)) }

        @Suppress("UNCHECKED_CAST")
        val info = api.info(emptyMap())[AlarmResultKeys.HOLIDAY_INFO] as Map<String, Any?>
        assertEquals("holiday-calendars-v1", info[AlarmResultKeys.VERSION])
        assertEquals("python-holidays 0.108", info[AlarmResultKeys.DATA_SOURCE])
        assertEquals(set.catalog.generatedAt, info[AlarmResultKeys.UPDATED_AT])

        api.checkRefresh(emptyMap())
        api.checkRefresh(mapOf(AlarmOptionKeys.REGION to "jp"))
        assertAlarmError(HOLIDAY_DATA_MISSING) { api.checkRefresh(mapOf(AlarmOptionKeys.REGION to "XX")) }
        assertAlarmError(INVALID_OPTIONS) { api.checkRefresh(mapOf(AlarmOptionKeys.DATE to "2026-01-01")) }
    }

    @Test
    fun subdivisionsOverlayTheCountryCalendar() {
        val data = data("US")
        val federal = data.dayIn(LocalDate.of(2026, 7, 3), "US", Locale.US)
        assertTrue("Independence Day observed", federal.holiday)
        val country = data.entries(2026, "US", Locale.US).map { it.date }.toSet()
        val california = data.entries(2026, "US-CA", Locale.US).map { it.date }.toSet()
        assertTrue("a state adds or removes days", country != california)
    }

    private fun assertThrowsHolidayData(block: () -> Unit) {
        try {
            block()
        } catch (_: HolidayDataException) {
            return
        }
        throw AssertionError("expected HolidayDataException")
    }
}
