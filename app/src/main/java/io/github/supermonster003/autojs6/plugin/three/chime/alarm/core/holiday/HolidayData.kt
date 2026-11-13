package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import org.autojs.plugin.alarm.api.AlarmErrorCodes
import java.security.MessageDigest
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * One complete, verified holiday data set (roadmap P8.2): the catalog and access to its calendar files. A calendar is
 * read on first use and checked against the size and SHA-256 of the catalog before it is parsed, so a damaged file is
 * never used; it then stays in memory.
 * zh-CN: 一套完整且已校验的节假日数据 (路线图 P8.2): 目录及其日历文件的读取. 日历在首次使用时读取, 解析前按目录核对大小与 SHA-256, 损坏的文件
 * 不会被使用; 之后常驻内存. [directory] 是下载数据集所在目录 (内置快照为 null).
 */
class HolidayDataSet(
    val catalog: HolidayCatalog,
    val directory: java.io.File? = null,
    private val read: (HolidayCatalog.FileRef) -> ByteArray?,
) {

    private val calendars = ConcurrentHashMap<String, HolidayCalendar>()

    @Throws(HolidayDataException::class)
    fun calendar(region: HolidayCatalog.Region): HolidayCalendar = calendars[region.code] ?: run {
        val bytes = read(region.file) ?: throw HolidayDataException("calendar ${region.code} is missing")
        checkIntegrity(bytes, region.file)
        HolidayCalendar.parse(bytes, region.code).also { calendars[region.code] = it }
    }

    /**
     * Canonical region of [text] (`CC` or `CC-<subdivision>`, T170): the country case-insensitively, the subdivision exactly
     * as the catalog writes it (`AU-NSW Western`, `BE-Flemish`), else its single case-insensitive match. A malformed code
     * is `INVALID_OPTIONS`, a region or subdivision without data `HOLIDAY_DATA_MISSING`.
     * zh-CN: [text] 的规范地区 (`CC` 或 `CC-<下级地区>`, T170): 国家不区分大小写, 下级地区按目录原样 (`AU-NSW Western`, `BE-Flemish`), 否则取唯一
     * 一个不区分大小写的匹配. 格式错误报 `INVALID_OPTIONS`, 没有数据的地区或下级地区报 `HOLIDAY_DATA_MISSING`.
     */
    fun region(text: String): HolidayRegionRef {
        val trimmed = text.trim()
        if (trimmed.length < 2 || trimmed.length > MAX_REGION_CHARS || !trimmed[0].isAsciiLetter() || !trimmed[1].isAsciiLetter() ||
            (trimmed.length > 2 && (trimmed[2] != '-' || trimmed.length == 3))
        ) {
            throw AlarmException.invalidOptions("holiday region must be an ISO 3166-1 alpha-2 code with an optional subdivision, as \"US\" or \"US-CA\"")
        }
        val code = trimmed.substring(0, 2).uppercase(Locale.ROOT)
        val region = catalog.region(code) ?: throw AlarmException(AlarmErrorCodes.HOLIDAY_DATA_MISSING, "no holiday data for region $code")
        if (trimmed.length == 2) return HolidayRegionRef(region, null)
        val wanted = trimmed.substring(3)
        val subdivision = region.subdivisions.firstOrNull { it.code == wanted }
            ?: region.subdivisions.filter { it.code.equals(wanted, ignoreCase = true) }.singleOrNull()
            ?: throw AlarmException(AlarmErrorCodes.HOLIDAY_DATA_MISSING, "no holiday data for region $code-$wanted")
        return HolidayRegionRef(region, subdivision.code)
    }

    private fun Char.isAsciiLetter(): Boolean = this in 'A'..'Z' || this in 'a'..'z'

    companion object {

        const val MAX_REGION_CHARS = 68

        @Throws(HolidayDataException::class)
        fun checkIntegrity(bytes: ByteArray, file: HolidayCatalog.FileRef) {
            if (bytes.size.toLong() != file.size) throw HolidayDataException("${file.name}: size ${bytes.size} instead of ${file.size}")
            if (sha256Hex(bytes) != file.sha256) throw HolidayDataException("${file.name}: SHA-256 does not match the catalog")
        }

        fun sha256Hex(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
    }
}

/** A canonical holiday region: a catalog region and an optional subdivision code. zh-CN: 规范的节假日地区: 目录地区与可选的下级地区代码. */
data class HolidayRegionRef(val region: HolidayCatalog.Region, val subdivision: String?) {
    val code: String get() = if (subdivision == null) region.code else "${region.code}-$subdivision"
}

/**
 * The holiday judgement the rule engine uses (roadmap P8.3): what [date] is in [region] (null: the default region).
 * Null when nothing is known about the region (no default region, a region the data no longer has, a damaged file);
 * the engine then treats every year as missing and rings by the weekday rules (never silent because of data, P8.2).
 * zh-CN: 规则引擎使用的节假日判定 (路线图 P8.3): [date] 在 [region] (null 为默认地区) 中是什么日子. 对该地区一无所知 (没有默认地区, 数据中已无该
 * 地区, 文件损坏) 时为 null; 引擎随后把每一年都当作缺失, 按星期规则响铃 (不因数据而不响, P8.2).
 */
fun interface HolidayJudge {

    fun day(date: LocalDate, region: String?): HolidayDay?

    companion object {
        val NONE = HolidayJudge { _, _ -> null }
    }
}

/**
 * The holiday data in use (roadmap P8.2 / P8.3): the current [HolidayDataSet], swapped whole after a refresh, and the
 * default region of the settings. The Binder answers, the plugin tool and the rule engine all read through it.
 * zh-CN: 正在使用的节假日数据 (路线图 P8.2 / P8.3): 当前的 [HolidayDataSet] (刷新后整套替换) 与设置中的默认地区. Binder 应答, 插件工具与规则引擎
 * 都经由它读取.
 */
class HolidayData(initial: HolidayDataSet, private val defaultRegion: () -> String?) : HolidayJudge {

    data class Info(val version: String, val source: String, val updatedAt: Long, val estimatedFrom: Map<String, Int>)

    @Volatile
    var set: HolidayDataSet = initial
        private set

    fun replace(next: HolidayDataSet) {
        set = next
    }

    /** [region] or the default region, canonical; see [HolidayDataSet.region]. zh-CN: [region] 或默认地区, 规范化后返回. */
    fun resolve(region: String?): HolidayRegionRef {
        val text = region ?: defaultRegion()
            ?: throw AlarmException(AlarmErrorCodes.HOLIDAY_DATA_MISSING, "no holiday region given and no default region in the plugin settings")
        return set.region(text)
    }

    @Throws(HolidayDataException::class)
    fun dayIn(date: LocalDate, region: String?, locale: Locale): HolidayDay {
        val ref = resolve(region)
        return set.calendar(ref.region).day(date, ref.subdivision, locale)
    }

    @Throws(HolidayDataException::class)
    fun entries(year: Int, region: String?, locale: Locale = Locale.getDefault()): List<HolidayCalendar.Entry> {
        val ref = resolve(region)
        return set.calendar(ref.region).entries(year, ref.subdivision, locale)
    }

    fun regions(): List<HolidayCatalog.Region> = set.catalog.regions

    fun info(): Info = set.catalog.let { catalog ->
        Info(catalog.releaseTag, catalog.source.label, catalog.generatedAt, catalog.regions.mapNotNull { r -> r.estimatedFrom?.let { r.code to it } }.toMap())
    }

    override fun day(date: LocalDate, region: String?): HolidayDay? = try {
        dayIn(date, region, Locale.ROOT)
    } catch (_: AlarmException) {
        null
    } catch (_: HolidayDataException) {
        null
    }

    /**
     * Warnings of `capabilities()` for the regions in [regions] (null: the default region) in [year] (roadmap P8.2,
     * Q47 (a), T173): a year without data rings by the weekday rules only; an estimated year may still get makeup
     * workdays or adjusted days off; a region without data is named too. English, like the other protocol texts.
     * zh-CN: `capabilities()` 中关于 [regions] (null 为默认地区) 在 [year] 的警告 (路线图 P8.2, Q47 (a), T173): 没有数据的年份只按星期规则响铃;
     * 推算年份仍可能有补班或调休; 没有数据的地区也会列出. 与其他协议文字一样用英文.
     */
    fun warnings(regions: Collection<String?>, year: Int): List<String> = issues(regions, year).map { issue ->
        when (issue) {
            is Issue.NoData -> "Holiday region ${issue.region}: ${issue.detail}; alarms with it ring by their weekday rules only"
            is Issue.MissingYear -> "Holiday data for ${issue.region} has no year ${issue.year} (it covers ${issue.firstYear} - ${issue.lastYear}); alarms with it ring by their weekday rules only this year"
            is Issue.Estimated -> "Holiday data for ${issue.region} from ${issue.from} on is estimated: makeup workdays and adjusted days off are not announced yet and may change"
        }
    }.distinct()

    /**
     * The facts behind [warnings], for texts in the language of the user (roadmap P8.6, T173): each region in [regions]
     * (null: the default region) gives at most one issue for [year].
     * zh-CN: [warnings] 背后的事实, 供界面按用户语言显示 (路线图 P8.6, T173): [regions] (null 为默认地区) 中每个地区在 [year] 至多一条.
     */
    fun issues(regions: Collection<String?>, year: Int): List<Issue> {
        val out = LinkedHashSet<Issue>()
        for (text in regions.toSet()) {
            val ref = try {
                resolve(text)
            } catch (e: AlarmException) {
                if (text != null || e.code == AlarmErrorCodes.HOLIDAY_DATA_MISSING && defaultRegion() != null) out += Issue.NoData(text ?: defaultRegion()!!, e.detail)
                continue
            }
            val region = ref.region
            when {
                !region.covers(year) -> out += Issue.MissingYear(region.code, year, region.firstYear, region.lastYear)
                region.estimated(year) || region.estimated(year + 1) -> out += Issue.Estimated(region.code, maxOf(region.estimatedFrom!!, year))
            }
        }
        return out.toList()
    }

    /** One fact behind a warning of [warnings]. zh-CN: [warnings] 中一条警告背后的事实. */
    sealed interface Issue {

        val region: String

        /** The data has no [region] (or it is damaged): [detail] says why. zh-CN: 数据中没有 [region] (或已损坏), [detail] 说明原因. */
        data class NoData(override val region: String, val detail: String) : Issue

        /** [region] has no data for [year]. zh-CN: [region] 没有 [year] 的数据. */
        data class MissingYear(override val region: String, val year: Int, val firstYear: Int, val lastYear: Int) : Issue

        /** The data of [region] is estimated from [from] on (T162). zh-CN: [region] 自 [from] 年起的数据为推算 (T162). */
        data class Estimated(override val region: String, val from: Int) : Issue
    }

    companion object {

        /**
         * The default region guessed from [locale] on first use (roadmap P8.3): its country when the catalog has it, else
         * null (no default until the user picks one).
         * zh-CN: 首次使用时从 [locale] 推断的默认地区 (路线图 P8.3): 目录中有该国家时取之, 否则为 null (用户选择前没有默认地区).
         */
        fun inferRegion(catalog: HolidayCatalog, locale: Locale): String? =
            locale.country.uppercase(Locale.ROOT).takeIf { it.length == 2 && catalog.region(it) != null }
    }
}
