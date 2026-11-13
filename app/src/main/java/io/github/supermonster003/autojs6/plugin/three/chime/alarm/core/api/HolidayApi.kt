package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayCalendar
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayCatalog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayData
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayDataException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayRegionRef
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import org.autojs.plugin.alarm.api.AlarmResultKeys
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * The holiday methods of `IAlarmPlugin` on value trees (feature `holidays`, roadmap P8.2 / P8.3, appendix A section 4.1):
 * `holidayRegions`, `checkDate`, `listHolidays`, `holidayInfo` and the request check of `refreshHolidays`. A region is
 * canonicalized by [HolidayData.resolve] (T170); a date or year without data is `HOLIDAY_DATA_MISSING` (T177), a damaged
 * calendar file `IO_FAILED`. Answers carry extra keys the host passes through to scripts (T178): `estimatedFrom` and
 * `subdivisions` per region, `estimated` in the info, `estimated` in `checkDate`.
 *
 * zh-CN: 以值树实现的 `IAlarmPlugin` 节假日方法 (功能 `holidays`, 路线图 P8.2 / P8.3, 附录 A 第 4.1 节): `holidayRegions`, `checkDate`,
 * `listHolidays`, `holidayInfo` 以及 `refreshHolidays` 的请求检查. 地区由 [HolidayData.resolve] 规范化 (T170); 没有数据的日期或年份报
 * `HOLIDAY_DATA_MISSING` (T177), 损坏的日历文件报 `IO_FAILED`. 应答带有宿主透传给脚本的额外键 (T178): 每个地区的 `estimatedFrom` 与
 * `subdivisions`, 信息中的 `estimated`, `checkDate` 中的 `estimated`.
 */
class HolidayApi(
    private val data: HolidayData,
    private val alarmRegions: () -> Collection<String?> = { emptyList() },
    private val clock: () -> Long = System::currentTimeMillis,
    private val deviceZone: () -> ZoneId = ZoneId::systemDefault,
    private val locale: () -> Locale = Locale::getDefault,
) {

    fun regions(values: Map<String, Any?>): Map<String, Any?> {
        AlarmRequest.of(values, emptySet())
        return mapOf(AlarmResultKeys.REGIONS to data.regions().map(::regionValues))
    }

    fun checkDate(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, CHECK_KEYS)
        val date = parseDate(request.requireString(AlarmOptionKeys.DATE))
        val ref = data.resolve(request.string(AlarmOptionKeys.REGION))
        requireYear(ref, date.year)
        val day = reading { data.set.calendar(ref.region).day(date, ref.subdivision, locale()) }
        val out = linkedMapOf<String, Any?>(AlarmResultKeys.IS_HOLIDAY to day.holiday, AlarmResultKeys.IS_WORKDAY to day.workday)
        day.name?.let { out[AlarmResultKeys.NAME] = it }
        out[KEY_ESTIMATED] = day.estimated
        return out
    }

    fun listHolidays(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, LIST_KEYS)
        val year = request.int(AlarmOptionKeys.YEAR) ?: throw AlarmException.invalidOptions("missing option '${AlarmOptionKeys.YEAR}'")
        if (year !in 1..9999) throw AlarmException.invalidOptions("'${AlarmOptionKeys.YEAR}' must be a year, got $year")
        val ref = data.resolve(request.string(AlarmOptionKeys.REGION))
        requireYear(ref, year)
        val entries = reading { data.set.calendar(ref.region).entries(year, ref.subdivision, locale()) }
        return mapOf(AlarmResultKeys.HOLIDAY_ENTRIES to entries.map(::entryValues))
    }

    fun info(values: Map<String, Any?>): Map<String, Any?> {
        AlarmRequest.of(values, emptySet())
        return mapOf(AlarmResultKeys.HOLIDAY_INFO to infoValues(data.info()))
    }

    /**
     * Checks a `refreshHolidays` request; the refresh itself always updates the whole data set (T175).
     * zh-CN: 检查 `refreshHolidays` 请求; 刷新本身总是更新整套数据 (T175).
     */
    fun checkRefresh(values: Map<String, Any?>) {
        val request = AlarmRequest.of(values, REFRESH_KEYS)
        request.string(AlarmOptionKeys.REGION)?.let { data.resolve(it) }
    }

    /** `capabilities()` warnings for the default region and the regions of alarms (T173). zh-CN: 默认地区与闹钟所用地区的警告 (T173). */
    fun warnings(): List<String> {
        val year = Instant.ofEpochMilli(clock()).atZone(deviceZone()).year
        return data.warnings(listOf<String?>(null) + alarmRegions(), year)
    }

    fun infoValues(info: HolidayData.Info): Map<String, Any?> = linkedMapOf(
        AlarmResultKeys.VERSION to info.version,
        AlarmResultKeys.DATA_SOURCE to info.source,
        AlarmResultKeys.UPDATED_AT to info.updatedAt,
        KEY_ESTIMATED to data.regions().filter { it.estimatedFrom != null }.map { region ->
            mapOf(KEY_REGION to region.code, KEY_YEARS to longArrayOf(region.estimatedFrom!!.toLong(), region.lastYear.toLong()))
        },
    )

    private fun regionValues(region: HolidayCatalog.Region): Map<String, Any?> {
        val out = linkedMapOf<String, Any?>(
            AlarmResultKeys.CODE to region.code,
            AlarmResultKeys.NAME to region.name,
            AlarmResultKeys.FIRST_YEAR to region.firstYear,
            AlarmResultKeys.LAST_YEAR to region.lastYear,
            AlarmResultKeys.HAS_MAKEUP_WORKDAYS to region.hasMakeupWorkdays,
        )
        region.estimatedFrom?.let { out[KEY_ESTIMATED_FROM] = it }
        out[KEY_SUBDIVISIONS] = region.subdivisions.map { mapOf(AlarmResultKeys.CODE to it.code, AlarmResultKeys.NAME to it.name) }
        return out
    }

    /** A makeup workday has no name in the data; its `name` is empty (T178). zh-CN: 数据中补班日没有名称, 其 `name` 为空串 (T178). */
    private fun entryValues(entry: HolidayCalendar.Entry): Map<String, Any?> = linkedMapOf(
        AlarmResultKeys.DATE to entry.date.toString(),
        AlarmResultKeys.NAME to entry.name,
        AlarmResultKeys.TYPE to if (entry.type == HolidayCalendar.Entry.Type.HOLIDAY) AlarmContract.HOLIDAY_TYPE_HOLIDAY else AlarmContract.HOLIDAY_TYPE_WORKDAY,
    )

    private fun requireYear(ref: HolidayRegionRef, year: Int) {
        if (!ref.region.covers(year)) {
            throw AlarmException(AlarmErrorCodes.HOLIDAY_DATA_MISSING, "holiday data for ${ref.code} covers ${ref.region.firstYear} - ${ref.region.lastYear}, not $year")
        }
    }

    private fun <T> reading(block: () -> T): T = try {
        block()
    } catch (e: HolidayDataException) {
        throw AlarmException(AlarmErrorCodes.IO_FAILED, "holiday data is damaged: ${e.message}")
    }

    companion object {

        const val KEY_ESTIMATED = "estimated"
        const val KEY_ESTIMATED_FROM = "estimatedFrom"
        const val KEY_SUBDIVISIONS = "subdivisions"
        const val KEY_REGION = "region"
        const val KEY_YEARS = "years"

        private val CHECK_KEYS = setOf(AlarmOptionKeys.DATE, AlarmOptionKeys.REGION)
        private val LIST_KEYS = setOf(AlarmOptionKeys.YEAR, AlarmOptionKeys.REGION)
        private val REFRESH_KEYS = setOf(AlarmOptionKeys.REGION)
        private val DATE = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")

        /** `YYYY-MM-DD` (the host converts a Date or timestamp, T201). zh-CN: `YYYY-MM-DD` (Date 或时间戳由宿主转换, T201). */
        fun parseDate(text: String): LocalDate {
            if (!DATE.matches(text)) throw AlarmException.invalidOptions("'${AlarmOptionKeys.DATE}' must be YYYY-MM-DD")
            return try {
                LocalDate.parse(text)
            } catch (_: DateTimeParseException) {
                throw AlarmException.invalidOptions("'${AlarmOptionKeys.DATE}' is not a calendar date: $text")
            }
        }
    }
}
