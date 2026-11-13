package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday

import org.autojs.plugin.alarm.api.AlarmJson
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Holiday data that is malformed, wrongly signed, rolled back, too large or not ours (roadmap P8.2). The refresh reports
 * it as `IO_FAILED`; the previous complete data set stays in use.
 * zh-CN: 格式错误, 签名不符, 版本回退, 过大或来源不符的节假日数据 (路线图 P8.2). 刷新将其报告为 `IO_FAILED`; 之前完整的数据集继续使用.
 */
class HolidayDataException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * The signed catalog of the Shared-Assets family `holiday-calendars` (schema 1, roadmap P8.2, T162): one summary per
 * region and the release asset of its calendar file. [parse] checks family, repository and schema, the year ranges,
 * that region codes are sorted and unique, and that every file entry is a release asset of this repository named
 * after its code and hash; it does not check the signature ([CatalogSignatureVerifier] does that first).
 *
 * zh-CN: Shared-Assets `holiday-calendars` 族的已签名目录 (schema 1, 路线图 P8.2, T162): 每个地区一条摘要及其日历文件的 Release 资源.
 * [parse] 检查族, 仓库与 schema, 年份范围, 地区代码有序且唯一, 每个文件条目都是本仓库以代码与哈希命名的 Release 资源; 它不检查签名
 * (由 [CatalogSignatureVerifier] 先行检查).
 */
class HolidayCatalog private constructor(
    val version: Int,
    val generatedAt: Long,
    val releaseTag: String,
    val source: Source,
    val regions: List<Region>,
) {

    data class Source(val name: String, val version: String, val license: String, val homepage: String) {
        /** `python-holidays 0.108`. zh-CN: `python-holidays 0.108`. */
        val label: String get() = "$name $version"
    }

    data class Subdivision(val code: String, val name: String)

    data class FileRef(val name: String, val releaseTag: String, val sha256: String, val size: Long, val url: String)

    data class Region(
        val code: String,
        val name: String,
        val firstYear: Int,
        val lastYear: Int,
        val estimatedFrom: Int?,
        val hasMakeupWorkdays: Boolean,
        val subdivisions: List<Subdivision>,
        val file: FileRef,
    ) {
        fun covers(year: Int): Boolean = year in firstYear..lastYear

        fun estimated(year: Int): Boolean = estimatedFrom != null && year >= estimatedFrom
    }

    private val byCode: Map<String, Region> = regions.associateBy { it.code }

    fun region(code: String): Region? = byCode[code]

    companion object {

        const val FAMILY = "holiday-calendars"
        const val REPOSITORY = "SuperMonster003/AutoJs6-Shared-Assets"
        const val SCHEMA_VERSION = 1L
        const val CATALOG_NAME = "catalog-v1.json"
        const val SIGNATURE_NAME = "catalog-v1.sig.json"

        /** Catalog and calendar files at most 4 MiB, the envelope at most 4 KiB (Shared-Assets README). zh-CN: 大小上限. */
        const val MAX_FILE_BYTES = 4L * 1024 * 1024
        const val MAX_SIGNATURE_BYTES = 4L * 1024

        private val CODE = Regex("[A-Z]{2}")
        private val SHA256 = Regex("[0-9a-f]{64}")
        private val RELEASE_TAG = Regex("holiday-calendars-v[1-9][0-9]*")

        /** Throws [HolidayDataException] for anything outside schema 1. zh-CN: schema 1 以外的内容抛出 [HolidayDataException]. */
        @Throws(HolidayDataException::class)
        fun parse(bytes: ByteArray): HolidayCatalog {
            if (bytes.size > MAX_FILE_BYTES) throw HolidayDataException("catalog exceeds $MAX_FILE_BYTES bytes")
            val root = HolidayJson.parse(bytes, "catalog")
            return HolidayJson.reading("catalog") {
                check(long(root, "schemaVersion") == SCHEMA_VERSION) { "schemaVersion is not $SCHEMA_VERSION" }
                check(string(root, "family") == FAMILY) { "family is not $FAMILY" }
                check(string(root, "repository") == REPOSITORY) { "repository is not $REPOSITORY" }
                val version = long(root, "catalogVersion")
                check(version in 1..Int.MAX_VALUE) { "bad catalogVersion" }
                val releaseTag = string(root, "releaseTag")
                check(releaseTag == "holiday-calendars-v$version") { "releaseTag does not match catalogVersion" }
                val generatedAt = try {
                    Instant.parse(string(root, "generatedAt")).toEpochMilli()
                } catch (e: DateTimeParseException) {
                    throw IllegalStateException("bad generatedAt", e)
                }
                val sourceMap = map(root, "source")
                val source = Source(string(sourceMap, "name"), string(sourceMap, "version"), string(sourceMap, "license"), string(sourceMap, "homepage"))
                val years = years(root)
                val regions = list(root, "regions").map { region(asMap(it), years) }
                check(regions.isNotEmpty()) { "no regions" }
                check(regions.zipWithNext().all { (a, b) -> a.code < b.code }) { "regions are not sorted and unique" }
                HolidayCatalog(version.toInt(), generatedAt, releaseTag, source, regions)
            }
        }

        private fun HolidayJson.Scope.region(values: Map<String, Any?>, catalogYears: IntRange): Region {
            val code = string(values, "code")
            check(CODE.matches(code)) { "bad region code" }
            val years = years(values)
            check(years.first >= catalogYears.first && years.last <= catalogYears.last) { "$code years outside the catalog" }
            val estimatedFrom = optionalLong(values, "estimatedFrom")?.toInt()
            val subdivisions = list(values, "subdivisions").map { item ->
                val sub = asMap(item)
                Subdivision(string(sub, "code").also { check(it.isNotEmpty() && it.length <= 64) { "$code: bad subdivision code" } }, string(sub, "name"))
            }
            check(subdivisions.map { it.code }.toSet().size == subdivisions.size) { "$code: duplicate subdivision" }
            val fileMap = map(values, "file")
            val sha256 = string(fileMap, "sha256")
            check(SHA256.matches(sha256)) { "$code: bad sha256" }
            val name = string(fileMap, "name")
            check(name == "holiday-calendar-$code-${sha256.substring(0, 8)}.json") { "$code: bad file name" }
            val releaseTag = string(fileMap, "releaseTag")
            check(RELEASE_TAG.matches(releaseTag)) { "$code: bad releaseTag" }
            val url = string(fileMap, "url")
            check(url == "https://github.com/$REPOSITORY/releases/download/$releaseTag/$name") { "$code: url is not a release asset" }
            val size = long(fileMap, "size")
            check(size in 1..MAX_FILE_BYTES) { "$code: bad size" }
            return Region(
                code = code,
                name = string(values, "name"),
                firstYear = years.first,
                lastYear = years.last,
                estimatedFrom = estimatedFrom,
                hasMakeupWorkdays = boolean(values, "hasMakeupWorkdays"),
                subdivisions = subdivisions,
                file = FileRef(name, releaseTag, sha256, size, url),
            )
        }
    }
}

/**
 * Strict reading of the holiday JSON on top of [AlarmJson] (the plugin has no other JSON library on the JVM test path):
 * every failure becomes [HolidayDataException] naming the document.
 * zh-CN: 基于 [AlarmJson] 的节假日 JSON 严格读取 (JVM 测试路径上插件没有其他 JSON 库): 任何失败都变为指明文档的 [HolidayDataException].
 */
internal object HolidayJson {

    fun parse(bytes: ByteArray, what: String): Map<String, Any?> {
        val text = try {
            Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        } catch (e: java.nio.charset.CharacterCodingException) {
            throw HolidayDataException("$what is not UTF-8", e)
        }
        if (text.isNotEmpty() && text[0].code == 0xFEFF) throw HolidayDataException("$what starts with a byte order mark")
        return AlarmJson.parse(text) ?: throw HolidayDataException("$what is not a JSON object")
    }

    fun <T> reading(what: String, block: Scope.() -> T): T = try {
        Scope.block()
    } catch (e: IllegalStateException) {
        throw HolidayDataException("$what: ${e.message}", e)
    } catch (e: ClassCastException) {
        throw HolidayDataException("$what: unexpected value type", e)
    } catch (e: NumberFormatException) {
        throw HolidayDataException("$what: bad number", e)
    }

    object Scope {

        fun string(values: Map<String, Any?>, key: String): String = values[key] as? String ?: error("missing string $key")

        fun long(values: Map<String, Any?>, key: String): Long = values[key] as? Long ?: error("missing integer $key")

        fun optionalLong(values: Map<String, Any?>, key: String): Long? = when (val value = values[key]) {
            null -> if (key in values) null else error("missing $key")
            is Long -> value
            else -> error("bad integer $key")
        }

        fun boolean(values: Map<String, Any?>, key: String): Boolean = values[key] as? Boolean ?: error("missing boolean $key")

        fun map(values: Map<String, Any?>, key: String): Map<String, Any?> = asMap(values[key] ?: error("missing object $key"))

        fun list(values: Map<String, Any?>, key: String): List<Any?> = asList(values[key] ?: error("missing array $key"))

        fun optionalList(values: Map<String, Any?>, key: String): List<Any?>? = values[key]?.let { asList(it) }

        @Suppress("UNCHECKED_CAST")
        fun asMap(value: Any?): Map<String, Any?> = value as? Map<String, Any?> ?: error("expected an object")

        fun asList(value: Any?): List<Any?> = when (value) {
            is List<*> -> value
            is LongArray -> value.toList()
            else -> error("expected an array")
        }

        fun ints(value: Any?): List<Int> = asList(value).map { (it as? Long)?.toInt() ?: error("expected integers") }

        fun strings(value: Any?): List<String> = asList(value).map { it as? String ?: error("expected strings") }

        fun years(values: Map<String, Any?>): IntRange {
            val years = ints(values["years"] ?: error("missing years"))
            check(years.size == 2 && years[0] in 1900..2200 && years[0] <= years[1] && years[1] <= 2200) { "bad years" }
            return years[0]..years[1]
        }
    }
}
