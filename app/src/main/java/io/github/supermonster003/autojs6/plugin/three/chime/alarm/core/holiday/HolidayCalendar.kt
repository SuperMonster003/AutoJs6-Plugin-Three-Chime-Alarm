package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday

import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * What the holiday data says about one date in one region (roadmap P8.2 / P8.3). [missing] is true when the data has no
 * entry for the year (outside the region's `years`); then only [weekend] is known (the region's default weekend) and the
 * date is neither a holiday nor a makeup workday. [estimated] marks a year from the region's `estimatedFrom` on (T162):
 * judged like any other year, but makeup workdays and ad hoc days off are not known yet (Q47 (a)).
 *
 * zh-CN: 节假日数据对某地区某一天的结论 (路线图 P8.2 / P8.3). 数据没有该年 (超出地区的 `years`) 时 [missing] 为真, 此时只知道 [weekend]
 * (地区默认周末), 该日既不是节假日也不是补班日. [estimated] 标记地区 `estimatedFrom` 起的年份 (T162): 照常判定, 但补班日与临时放假尚未可知
 * (Q47 (a)).
 */
data class HolidayDay(
    val date: LocalDate,
    val holiday: Boolean,
    val makeupWorkday: Boolean,
    val weekend: Boolean,
    val names: List<String>,
    val estimated: Boolean,
    val missing: Boolean,
) {
    /** A day off: a holiday, or a weekend day that is not a makeup workday. zh-CN: 休息日: 节假日, 或不是补班日的周末. */
    val dayOff: Boolean get() = holiday || (weekend && !makeupWorkday)

    val workday: Boolean get() = !dayOff

    /** Several holidays on one day are joined with "; " (T171). zh-CN: 同一天的多个节日以 "; " 连接 (T171). */
    val name: String? get() = names.takeIf { it.isNotEmpty() }?.joinToString(NAME_SEPARATOR)

    companion object {
        const val NAME_SEPARATOR = "; "
    }
}

/**
 * One calendar file of the `holiday-calendars` family (schema 1, Shared-Assets `holiday-calendars/README.md`): national
 * holidays per year with name indexes, makeup workdays, the weekend (file default, per-year override) and subdivision
 * overlays (`holidays`, `removed`, `weekend`, T165). [parse] validates the whole document so a damaged file is refused
 * before it replaces a good one; resolved years are cached.
 *
 * zh-CN: `holiday-calendars` 族的一个日历文件 (schema 1, Shared-Assets `holiday-calendars/README.md`): 每年的全国节假日 (名称索引), 补班日,
 * 周末 (文件默认, 按年覆盖) 与下级地区覆盖 (`holidays`, `removed`, `weekend`, T165). [parse] 校验整份文档, 损坏的文件替换好文件之前就被
 * 拒绝; 解析后的年份会缓存.
 */
class HolidayCalendar private constructor(
    val region: String,
    val name: String,
    val firstYear: Int,
    val lastYear: Int,
    val estimatedFrom: Int?,
    /** ISO weekdays, 1 = Monday ... 7 = Sunday. zh-CN: ISO 星期, 1 = 周一 ... 7 = 周日. */
    val weekend: Set<Int>,
    val hasMakeupWorkdays: Boolean,
    private val defaultLanguage: String,
    private val names: List<Map<String, String>>,
    private val years: Map<Int, YearEntry>,
    private val subdivisions: Map<String, Map<Int, Overlay>>,
) {

    /** A date of the year as `month * 100 + day`. zh-CN: 年内日期记作 `month * 100 + day`. */
    private class YearEntry(val holidays: Map<Int, IntArray>, val workdays: Set<Int>, val weekend: Set<Int>?)

    private class Overlay(val holidays: Map<Int, IntArray>, val removed: Set<Int>, val weekend: Set<Int>?)

    private class Resolved(val holidays: Map<Int, IntArray>, val workdays: Set<Int>, val weekend: Set<Int>)

    data class Entry(val date: LocalDate, val names: List<String>, val type: Type) {
        enum class Type { HOLIDAY, WORKDAY }

        val name: String get() = names.joinToString(HolidayDay.NAME_SEPARATOR)
    }

    private val cache = ConcurrentHashMap<String, Resolved>()

    val subdivisionCodes: Set<String> get() = subdivisions.keys

    fun covers(year: Int): Boolean = year in firstYear..lastYear

    fun estimated(year: Int): Boolean = estimatedFrom != null && year >= estimatedFrom

    /** [subdivision] must be one of [subdivisionCodes] or null. zh-CN: [subdivision] 须为 [subdivisionCodes] 之一或 null. */
    fun day(date: LocalDate, subdivision: String? = null, locale: Locale = Locale.getDefault()): HolidayDay {
        val iso = date.dayOfWeek.value
        val resolved = resolve(date.year, subdivision)
            ?: return HolidayDay(date, holiday = false, makeupWorkday = false, weekend = iso in weekend, names = emptyList(), estimated = false, missing = true)
        val key = date.monthValue * 100 + date.dayOfMonth
        val indexes = resolved.holidays[key]
        return HolidayDay(
            date = date,
            holiday = indexes != null,
            makeupWorkday = key in resolved.workdays,
            weekend = iso in resolved.weekend,
            names = indexes?.map { nameOf(it, locale) } ?: emptyList(),
            estimated = estimated(date.year),
            missing = false,
        )
    }

    /** Holidays and makeup workdays of [year] in date order; empty when the year has no data. zh-CN: 按日期排序; 无数据时为空. */
    fun entries(year: Int, subdivision: String? = null, locale: Locale = Locale.getDefault()): List<Entry> {
        val resolved = resolve(year, subdivision) ?: return emptyList()
        val holidays = resolved.holidays.map { (key, indexes) -> Entry(dateOf(year, key), indexes.map { nameOf(it, locale) }, Entry.Type.HOLIDAY) }
        val workdays = resolved.workdays.map { Entry(dateOf(year, it), emptyList(), Entry.Type.WORKDAY) }
        return (holidays + workdays).sortedBy { it.date }
    }

    private fun dateOf(year: Int, key: Int): LocalDate = LocalDate.of(year, key / 100, key % 100)

    private fun resolve(year: Int, subdivision: String?): Resolved? {
        val entry = years[year] ?: return null
        val cacheKey = "$year/${subdivision ?: ""}"
        cache[cacheKey]?.let { return it }
        val holidays = LinkedHashMap(entry.holidays)
        var weekendOfYear = entry.weekend ?: weekend
        val overlay = subdivision?.let { subdivisions[it]?.get(year) }
        if (overlay != null) {
            overlay.removed.forEach { holidays.remove(it) }
            holidays.putAll(overlay.holidays)
            overlay.weekend?.let { weekendOfYear = it }
        }
        return Resolved(holidays, entry.workdays, weekendOfYear).also { cache[cacheKey] = it }
    }

    /** The language chain of the Shared-Assets README ("Choosing a name language"). zh-CN: Shared-Assets README 的语言选择链. */
    private fun nameOf(index: Int, locale: Locale): String {
        val texts = names[index]
        for (tag in candidates(locale)) {
            texts.entries.firstOrNull { it.key.equals(tag, ignoreCase = true) }?.let { return it.value }
        }
        val language = locale.language.lowercase(Locale.ROOT)
        if (language.isNotEmpty()) {
            val region = locale.country
            if (region.isNotEmpty()) texts.entries.firstOrNull { it.key.equals("$language-$region", ignoreCase = true) }?.let { return it.value }
            texts.entries.firstOrNull { it.key.equals(language, ignoreCase = true) }?.let { return it.value }
            texts.keys.filter { it.substringBefore('-').equals(language, ignoreCase = true) }.minOrNull()?.let { return texts.getValue(it) }
        }
        return texts.getValue(defaultLanguage)
    }

    companion object {

        private val MONTH_DAY = Regex("(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])")

        /**
         * Region tags to try first for [locale]: script tags map to the region tags of the data (`zh-Hans` -> `zh-CN`,
         * `zh-Hant-HK` -> `zh-HK`, `zh-TW`, ...); a Chinese locale without script uses its region the same way (T171).
         * zh-CN: 对 [locale] 先尝试的地区标签: 文字标签映射为数据使用的地区标签; 不带文字标签的中文区域按其地区同样处理 (T171).
         */
        internal fun candidates(locale: Locale): List<String> {
            val language = locale.language.lowercase(Locale.ROOT)
            val script = locale.script
            val region = locale.country.uppercase(Locale.ROOT)
            if (language == "zh") {
                val traditional = script.equals("Hant", ignoreCase = true) || (script.isEmpty() && region in setOf("TW", "HK", "MO"))
                return if (!traditional) {
                    if (script.isEmpty() && region.isNotEmpty() && region != "CN") listOf("zh-$region", "zh-CN") else listOf("zh-CN")
                } else when (region) {
                    "HK" -> listOf("zh-HK", "zh-TW")
                    "MO" -> listOf("zh-MO", "zh-HK", "zh-TW")
                    else -> listOf("zh-TW")
                }
            }
            return listOfNotNull(locale.toLanguageTag().takeIf { it != "und" })
        }

        @Throws(HolidayDataException::class)
        fun parse(bytes: ByteArray, expectedRegion: String): HolidayCalendar {
            if (bytes.size > HolidayCatalog.MAX_FILE_BYTES) throw HolidayDataException("calendar $expectedRegion exceeds ${HolidayCatalog.MAX_FILE_BYTES} bytes")
            val root = HolidayJson.parse(bytes, "calendar $expectedRegion")
            return HolidayJson.reading("calendar $expectedRegion") {
                check(long(root, "schemaVersion") == HolidayCatalog.SCHEMA_VERSION) { "schemaVersion is not ${HolidayCatalog.SCHEMA_VERSION}" }
                check(string(root, "region") == expectedRegion) { "region does not match" }
                val range = years(root)
                val estimatedFrom = optionalLong(root, "estimatedFrom")?.toInt()
                val weekend = weekdays(root["weekend"])
                val defaultLanguage = string(root, "defaultLanguage")
                val names = list(root, "names").map { item ->
                    asMap(item).mapValues { (_, text) -> text as? String ?: error("bad name text") }.also {
                        check(defaultLanguage in it) { "a name lacks the default language" }
                    }
                }
                fun indexes(value: Any?): IntArray = ints(value).also { list ->
                    check(list.isNotEmpty() && list.all { it in names.indices }) { "bad name index" }
                }.toIntArray()
                fun holidays(value: Any?): Map<Int, IntArray> = asMap(value).entries.associate { (key, item) -> monthDay(key) to indexes(item) }
                fun dates(value: Any?): Set<Int> = strings(value).map(::monthDay).toSet()

                val calendar = map(root, "calendar")
                check(calendar.size == range.last - range.first + 1) { "calendar does not cover exactly the years" }
                val years = calendar.entries.associate { (key, item) ->
                    val year = key.toIntOrNull()?.takeIf { key.length == 4 && it in range } ?: error("bad year $key")
                    val entry = asMap(item)
                    year to YearEntry(holidays(entry["holidays"] ?: error("missing holidays")), dates(entry["workdays"] ?: error("missing workdays")), entry["weekend"]?.let(::weekdays))
                }
                val subdivisions = map(root, "subdivisions").mapValues { (_, item) ->
                    val sub = asMap(item)
                    string(sub, "name")
                    map(sub, "calendar").entries.associate { (key, overlay) ->
                        val year = key.toIntOrNull()?.takeIf { key.length == 4 && it in range } ?: error("bad subdivision year $key")
                        val values = asMap(overlay)
                        year to Overlay(values["holidays"]?.let(::holidays) ?: emptyMap(), values["removed"]?.let(::dates) ?: emptySet(), values["weekend"]?.let(::weekdays))
                    }
                }
                HolidayCalendar(
                    region = expectedRegion,
                    name = string(root, "name"),
                    firstYear = range.first,
                    lastYear = range.last,
                    estimatedFrom = estimatedFrom,
                    weekend = weekend,
                    hasMakeupWorkdays = boolean(root, "hasMakeupWorkdays"),
                    defaultLanguage = defaultLanguage,
                    names = names,
                    years = years,
                    subdivisions = subdivisions,
                )
            }
        }

        private fun monthDay(text: String): Int {
            check(MONTH_DAY.matches(text)) { "bad date $text" }
            return text.substring(0, 2).toInt() * 100 + text.substring(3).toInt()
        }

        private fun weekdays(value: Any?): Set<Int> {
            // An empty weekend is legal (a region without weekly rest days). zh-CN: 空周末合法 (没有每周休息日的地区).
            val days = HolidayJson.Scope.ints(value ?: error("missing weekend"))
            check(days.all { it in 1..7 } && days.toSet().size == days.size) { "bad weekend" }
            return days.toSet()
        }
    }
}
