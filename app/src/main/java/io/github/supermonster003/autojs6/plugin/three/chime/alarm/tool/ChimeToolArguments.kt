package io.github.supermonster003.autojs6.plugin.three.chime.alarm.tool

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerDurations
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/**
 * The tool arguments as the alarm options of the contract (roadmap P4.1, T90): the `AlarmOptions` shape of appendix A
 * section 4.2 (`time` text or object, `date` text or timestamp, `at` timestamp or ISO 8601 text, `repeat` as RRULE, weekday
 * array or structured rule, `vibrate` boolean or pattern, `snooze` boolean or object) becomes the wire tree the host would
 * send (`hour` / `minute` / `second`, a `date` text, an `at` timestamp, an RRULE text, `vibratePattern`, `snooze.enabled`),
 * so the plugin's own validator and repository take it as they take a script call. A date from a timestamp and an `at`
 * text without offset are read in `timeZone`, the stored zone of the alarm (update) or the phone's zone. `sound.source`
 * takes `default`, `silent` or a URI other than `file:` (T91). For `alarm_update` the partial groups `sound`, `speak` and
 * `snooze` are merged over the stored group (T29 as the host does it) and `clear` names options to send as null (T93).
 * Since 1.2.0 the `holidays` group (`mode`, optional `region`) is sent as given and replaces the stored one as a whole,
 * so an omitted region keeps following the default region of the settings (roadmap P8.7, T193).
 * Type and range problems the host's schema check lets through fail with `INVALID_OPTIONS` naming the argument; the
 * validator checks the rest. Pure Kotlin.
 * zh-CN: 把工具参数转为契约的闹钟选项 (路线图 P4.1, T90): 附录 A 第 4.2 节 `AlarmOptions` 的形式 (`time` 文本或对象, `date` 文本或时间戳, `at`
 * 时间戳或 ISO 8601 文本, `repeat` 为 RRULE, 星期数组或结构化规则, `vibrate` 布尔或节奏, `snooze` 布尔或对象) 转为宿主会发送的线上树 (`hour` /
 * `minute` / `second`, `date` 文本, `at` 时间戳, RRULE 文本, `vibratePattern`, `snooze.enabled`), 插件自己的校验器与仓库像处理脚本调用一样处理它.
 * 由时间戳得到的日期与不带偏移的 `at` 文本按 `timeZone`, 闹钟已存的时区 (修改时) 或手机时区解读. `sound.source` 接受 `default`, `silent` 或
 * `file:` 以外的 URI (T91). `alarm_update` 中不完整的 `sound`, `speak`, `snooze` 分组与已存分组合并 (同宿主的 T29 做法), `clear` 列出以 null
 * 发送的选项 (T93). 1.2.0 起 `holidays` 分组 (`mode`, 可选 `region`) 原样发送并整体替换已存分组, 因此省略的地区继续随设置中的默认地区 (路线图 P8.7,
 * T193). 宿主 schema 检查放过的类型与范围问题以 `INVALID_OPTIONS` 失败并写明参数名, 其余由校验器检查. 纯 Kotlin.
 */
internal class ChimeToolArguments(private val deviceZone: () -> ZoneId = ZoneId::systemDefault) {

    /** The options of `alarm_add`. zh-CN: `alarm_add` 的选项. */
    fun add(args: Map<String, Any?>): Map<String, Any?> {
        rejectUnknown(args, OPTION_KEYS)
        if (args["time"] == null && args["at"] == null) throw invalid("give time (a wall-clock time) or at (a moment)")
        return options(args, null)
    }

    /**
     * The patch of `alarm_update` without `id`; [stored] answers the stored wire alarm (its `spec` values) and is called
     * at most once, only when needed.
     * zh-CN: `alarm_update` 去掉 `id` 后的 patch; [stored] 返回已存闹钟的线上值, 至多调用一次且只在需要时调用.
     */
    fun patch(args: Map<String, Any?>, stored: () -> Map<String, Any?>): Map<String, Any?> {
        rejectUnknown(args, OPTION_KEYS + "clear")
        val cleared = args["clear"]?.let { value ->
            val list = value as? List<*> ?: throw invalid("clear must be an array of option names")
            list.map { name ->
                val text = name as? String
                if (text == null || text !in ChimeToolCatalog.CLEARABLE) throw invalid("clear: ${describe(name)} is not one of ${ChimeToolCatalog.CLEARABLE.joinToString(", ")}")
                if (args[text] != null) throw invalid("$text is both given and cleared")
                text
            }.toSet()
        } ?: emptySet()
        val cache = lazy(stored)
        val wire = options(args - "clear", cache)
        cleared.forEach { wire[it] = null }
        if (wire.isEmpty()) throw invalid("nothing to change: give an option or clear")
        return wire
    }

    /**
     * The `startTimer` request of `timer_start` (roadmap P7.6, T126): `durationMillis` from [duration] and the ring options
     * `label`, `sound`, `vibrate`, `speak` and `autoSilenceMinutes` in the wire shape of the alarm options.
     * zh-CN: `timer_start` 的 `startTimer` 请求 (路线图 P7.6, T126): 由 [duration] 得到的 `durationMillis`, 以及线上形式与闹钟选项相同的响铃选项
     * `label`, `sound`, `vibrate`, `speak`, `autoSilenceMinutes`.
     */
    fun timer(args: Map<String, Any?>): Map<String, Any?> {
        rejectUnknown(args, TIMER_KEYS)
        val value = args["duration"] ?: throw invalid("duration is required: milliseconds, or text such as 25m, 1h30m or 90s")
        val wire = linkedMapOf<String, Any?>(AlarmOptionKeys.DURATION_MILLIS to TimerDurations.requireInRange(duration("duration", value), "duration"))
        wire.putAll(options(args - "duration", null))
        return wire
    }

    /**
     * T107: a duration argument, an integer in milliseconds or the `h m s` text ("25m", "1h30m", "90s"); [signed] allows a
     * negative integer or a leading minus (`timer_control` with `add`). The range is checked by the caller.
     * zh-CN: T107: 时长参数, 毫秒整数或 `h m s` 文本 ("25m", "1h30m", "90s"); [signed] 允许负整数或前导减号 (`timer_control` 的 `add`). 范围由调用方检查.
     */
    fun duration(name: String, value: Any, signed: Boolean = false): Long = when (value) {
        is String -> if (signed) TimerDurations.parseSigned(value, name) else TimerDurations.parse(value, name)
        is Number -> integral(name, value)
        else -> throw invalid("$name must be milliseconds or text such as 25m, 1h30m or 90s, got ${describe(value)}")
    }

    /**
     * The day of `holiday_check` as `YYYY-MM-DD`: text as given (the holiday API checks it) or the day of a Unix time in
     * milliseconds in the phone's zone; null when absent (T192).
     * zh-CN: `holiday_check` 的日子, 形如 `YYYY-MM-DD`: 文本原样 (由节假日 API 检查), 或毫秒时间戳在手机时区中的那一天; 缺省时为 null (T192).
     */
    fun day(args: Map<String, Any?>): String? = args["date"]?.let { date("date", it, deviceZone) }

    /** The `filter` group of `listAlarms` and `upcoming`, null when empty. zh-CN: `listAlarms` 与 `upcoming` 的 `filter` 分组, 为空时 null. */
    fun filter(args: Map<String, Any?>): Map<String, Any?>? {
        val out = LinkedHashMap<String, Any?>()
        args["source"]?.let { value ->
            val sources = (if (value is List<*>) value else listOf(value)).map { source ->
                val text = source as? String
                if (text == null || text !in AlarmContract.SOURCES) throw invalid("source must be one of ${AlarmContract.SOURCES.joinToString(", ")}, got ${describe(source)}")
                text
            }.distinct()
            if (sources.isEmpty()) throw invalid("source is an empty array")
            out[AlarmOptionKeys.FILTER_SOURCES] = sources
        }
        args["tag"]?.let { out[AlarmOptionKeys.FILTER_TAG] = string("tag", it) }
        args["enabled"]?.let { out[AlarmOptionKeys.FILTER_ENABLED] = bool("enabled", it) }
        args["label"]?.let { out[AlarmOptionKeys.FILTER_LABEL] = string("label", it) }
        return out.takeIf { it.isNotEmpty() }
    }

    fun id(args: Map<String, Any?>): String {
        val text = (args["id"] as? String)?.trim()
        if (text.isNullOrEmpty()) throw invalid("id must be a non-empty string, got ${describe(args["id"])}")
        return text
    }

    fun optionalId(args: Map<String, Any?>): String? = args["id"]?.let { id(args) }

    /** An integer argument within [min] - [max], [default] when absent. zh-CN: [min] - [max] 内的整数参数, 缺省时为 [default]. */
    fun int(args: Map<String, Any?>, name: String, min: Int, max: Int, default: Int?): Int? {
        val value = args[name] ?: return default
        val number = integral(name, value)
        if (number < min || number > max) throw invalid("$name must be $min - $max, got $number")
        return number.toInt()
    }

    fun boolean(args: Map<String, Any?>, name: String, default: Boolean): Boolean = args[name]?.let { bool(name, it) } ?: default

    // ------------------------------------------------------------------ options

    private fun options(args: Map<String, Any?>, stored: Lazy<Map<String, Any?>>?): MutableMap<String, Any?> {
        val patch = stored != null
        fun has(key: String) = args[key] != null
        if (has("time") && has("at")) throw invalid("time and at are exclusive")
        if (has("at") && has("date")) throw invalid("at already holds the date (use time with date)")
        val zone: ZoneId by lazy {
            args["timeZone"]?.let { zoneOf(it) }
                ?: (stored?.value?.get(AlarmOptionKeys.TIME_ZONE) as? String)?.let { runCatching { ZoneId.of(it) }.getOrNull() }
                ?: deviceZone()
        }
        val wire = LinkedHashMap<String, Any?>()
        args["time"]?.let { value ->
            val (hour, minute, second) = time(value)
            wire[AlarmOptionKeys.HOUR] = hour
            wire[AlarmOptionKeys.MINUTE] = minute
            wire[AlarmOptionKeys.SECOND] = second
        }
        args["date"]?.let { wire[AlarmOptionKeys.DATE] = date("date", it) { zone } }
        args["at"]?.let { wire[AlarmOptionKeys.AT] = moment(it) { zone } }
        args["repeat"]?.let { wire[AlarmOptionKeys.REPEAT] = repeat(it) }
        args["exclude"]?.let { value ->
            val list = value as? List<*> ?: throw invalid("exclude must be an array of YYYY-MM-DD dates")
            wire[AlarmOptionKeys.EXCLUDE] = list.map { string("exclude[]", it) }
        }
        args["holidays"]?.let { wire[AlarmOptionKeys.HOLIDAYS] = holidays(it) }
        args["timeZone"]?.let { wire[AlarmOptionKeys.TIME_ZONE] = string("timeZone", it).trim() }
        args["label"]?.let { wire[AlarmOptionKeys.LABEL] = string("label", it) }
        args["enabled"]?.let { wire[AlarmOptionKeys.ENABLED] = bool("enabled", it) }
        args["tag"]?.let { wire[AlarmOptionKeys.TAG] = string("tag", it) }
        args["sound"]?.let { wire[AlarmOptionKeys.SOUND] = merge(AlarmOptionKeys.SOUND, sound(it), SOUND_GROUP, stored) }
        args["vibrate"]?.let { value ->
            when (value) {
                is Boolean -> {
                    wire[AlarmOptionKeys.VIBRATE] = value
                    if (patch) wire[AlarmOptionKeys.VIBRATE_PATTERN] = null
                }
                is List<*> -> {
                    wire[AlarmOptionKeys.VIBRATE] = true
                    wire[AlarmOptionKeys.VIBRATE_PATTERN] = value.mapIndexed { index, millis -> integral("vibrate[$index]", millis) }
                }
                else -> throw invalid("vibrate must be a boolean or an array of milliseconds, got ${describe(value)}")
            }
        }
        args["speak"]?.let { value ->
            val merged = merge(AlarmOptionKeys.SPEAK, group("speak", value, SPEAK_KEYS), SPEAK_GROUP, stored)
            if (merged[AlarmOptionKeys.SPEAK_TEXT] == null) throw invalid("speak.text is required")
            wire[AlarmOptionKeys.SPEAK] = merged
        }
        args["snooze"]?.let { wire[AlarmOptionKeys.SNOOZE] = merge(AlarmOptionKeys.SNOOZE, snooze(it), SNOOZE_GROUP, stored) }
        args["autoSilenceMinutes"]?.let { wire[AlarmOptionKeys.AUTO_SILENCE_MINUTES] = integral("autoSilenceMinutes", it) }
        return wire
    }

    /**
     * [partial] over the stored group of [key] when it is a patch and [partial] lacks some of [groupKeys].
     * zh-CN: 当为 patch 且 [partial] 缺少 [groupKeys] 中的某些键时, 把 [partial] 合并到 [key] 的已存分组之上.
     */
    private fun merge(key: String, partial: Map<String, Any?>, groupKeys: Set<String>, stored: Lazy<Map<String, Any?>>?): Map<String, Any?> {
        val out = LinkedHashMap<String, Any?>()
        if (stored != null && !partial.keys.containsAll(groupKeys)) {
            (stored.value[key] as? Map<*, *>)?.forEach { (k, v) -> out[k.toString()] = v }
        }
        out.putAll(partial)
        out.entries.removeAll { it.value == null }
        return out
    }

    private fun time(value: Any): Triple<Long, Long, Long> = when (value) {
        is String -> {
            val match = TIME_PATTERN.matchEntire(value.trim()) ?: throw invalid("time must be HH:mm or HH:mm:ss, got ${describe(value)}")
            Triple(match.groupValues[1].toLong(), match.groupValues[2].toLong(), match.groupValues[3].takeIf { it.isNotEmpty() }?.toLong() ?: 0L)
        }
        is Map<*, *> -> {
            rejectUnknown(value, TIME_KEYS, "time.")
            Triple(
                integral("time.hour", value["hour"] ?: throw invalid("time.hour is required")),
                integral("time.minute", value["minute"] ?: throw invalid("time.minute is required")),
                value["second"]?.let { integral("time.second", it) } ?: 0L,
            )
        }
        else -> throw invalid("time must be HH:mm, HH:mm:ss or {hour, minute, second}, got ${describe(value)}")
    }

    private fun date(name: String, value: Any, zone: () -> ZoneId): String = when (value) {
        is String -> value.trim()
        is Number -> Instant.ofEpochMilli(integral(name, value)).atZone(zone()).toLocalDate().toString()
        else -> throw invalid("$name must be YYYY-MM-DD or a Unix time in milliseconds, got ${describe(value)}")
    }

    private fun moment(value: Any, zone: () -> ZoneId): Long = when (value) {
        is Number -> integral("at", value)
        is String -> {
            val text = value.trim()
            try {
                if (OFFSET_PATTERN.containsMatchIn(text)) OffsetDateTime.parse(text).toInstant().toEpochMilli()
                else LocalDateTime.parse(text).atZone(zone()).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) {
                throw invalid("at must be an ISO 8601 date and time such as 2026-11-07T07:30:00+08:00, got ${describe(value)}")
            }
        }
        else -> throw invalid("at must be a Unix time in milliseconds or an ISO 8601 date and time, got ${describe(value)}")
    }

    /** The rule as RRULE text; the plugin's rule parser normalizes and checks it. zh-CN: 规则的 RRULE 文本; 由插件的规则解析器规范化并检查. */
    private fun repeat(value: Any): String = when (value) {
        is String -> value
        is List<*> -> {
            if (value.isEmpty()) throw invalidRule("repeat: the weekday array is empty")
            "FREQ=WEEKLY;BYDAY=" + value.joinToString(",") { weekday("repeat[]", it) }
        }
        is Map<*, *> -> structured(value)
        else -> throw invalid("repeat must be an RRULE string, an array of weekday numbers (0 - 6) or a rule object, got ${describe(value)}")
    }

    private fun structured(rule: Map<*, *>): String {
        rejectUnknown(rule, STRUCTURED_PARTS.keys, "repeat.")
        val freq = (rule["freq"] as? String)?.trim()?.uppercase() ?: throw invalidRule("repeat.freq must be daily, weekly, monthly or yearly")
        val parts = ArrayList<String>()
        parts += "FREQ=$freq"
        for ((key, part) in STRUCTURED_PARTS) {
            if (key == "freq") continue
            val value = rule[key] ?: continue
            val text = when (key) {
                "until" -> until(value)
                "weekStart" -> when (integral("repeat.weekStart", value)) {
                    0L -> "SU"
                    1L -> "MO"
                    else -> throw invalidRule("repeat.weekStart must be 0 (Sunday) or 1 (Monday)")
                }
                "interval", "count" -> integral("repeat.$key", value).toString()
                "byDay" -> list("repeat.byDay", value).joinToString(",") { day ->
                    if (day is Number) weekday("repeat.byDay[]", day) else string("repeat.byDay[]", day).trim()
                }
                else -> list("repeat.$key", value).joinToString(",") { integral("repeat.$key[]", it).toString() }
            }
            parts += "$part=$text"
        }
        return parts.joinToString(";")
    }

    private fun until(value: Any): String = when (value) {
        is Number -> UNTIL_UTC.format(Instant.ofEpochMilli(integral("repeat.until", value)).atZone(ZoneId.of("UTC")))
        is String -> {
            val text = value.trim()
            if (ISO_DATE_PATTERN.matches(text)) {
                try {
                    LocalDate.parse(text).toString().replace("-", "")
                } catch (_: DateTimeException) {
                    throw invalidRule("repeat.until is not a valid date, got ${describe(value)}")
                }
            } else text
        }
        else -> throw invalidRule("repeat.until must be YYYY-MM-DD, an RRULE UNTIL or a Unix time in milliseconds, got ${describe(value)}")
    }

    private fun weekday(name: String, value: Any?): String {
        val number = (value as? Number)?.toDouble()
        if (number == null || number != Math.floor(number) || number < 0 || number > 6) throw invalidRule("$name: weekdays are 0 (Sunday) to 6 (Saturday), got ${describe(value)}")
        return WEEKDAYS[number.toInt()]
    }

    private fun sound(value: Any): Map<String, Any?> {
        val map = group("sound", value, SOUND_KEYS)
        map[AlarmOptionKeys.SOUND_SOURCE]?.let { raw ->
            val source = string("sound.source", raw).trim()
            val known = source == AlarmContract.SOUND_DEFAULT || source == AlarmContract.SOUND_SILENT
            val scheme = URI_SCHEME.find(source)?.groupValues?.get(1)?.lowercase()
            if (!known && (scheme == null || scheme == "file")) {
                throw invalid("sound.source must be default, silent or a ringtone URI; the tools take no local files (import the file on the ringtone page of 3-Chime Alarm first)")
            }
            return map + (AlarmOptionKeys.SOUND_SOURCE to source)
        }
        return map
    }

    /** T193: the whole `holidays` group; the validator checks `mode` and the repository the region. zh-CN: T193: 整个 `holidays` 分组. */
    private fun holidays(value: Any): Map<String, Any?> {
        val map = group("holidays", value, HOLIDAY_KEYS)
        if (map[AlarmOptionKeys.HOLIDAY_MODE] == null) throw invalid("holidays.mode is required: skip, only or workdays")
        map[AlarmOptionKeys.HOLIDAY_MODE] = string("holidays.mode", map[AlarmOptionKeys.HOLIDAY_MODE]).trim()
        map[AlarmOptionKeys.HOLIDAY_REGION]?.let { map[AlarmOptionKeys.HOLIDAY_REGION] = string("holidays.region", it).trim() }
        return map
    }

    private fun snooze(value: Any): Map<String, Any?> = when (value) {
        is Boolean -> linkedMapOf(AlarmOptionKeys.SNOOZE_ENABLED to value)
        is Map<*, *> -> {
            val members = group("snooze", value, setOf(AlarmOptionKeys.SNOOZE_MINUTES, AlarmOptionKeys.SNOOZE_MAX))
            linkedMapOf<String, Any?>(AlarmOptionKeys.SNOOZE_ENABLED to true).apply { putAll(members) }
        }
        else -> throw invalid("snooze must be a boolean or {minutes, max}, got ${describe(value)}")
    }

    // ------------------------------------------------------------------ value checks

    private fun group(name: String, value: Any, keys: Set<String>): MutableMap<String, Any?> {
        val map = value as? Map<*, *> ?: throw invalid("$name must be an object, got ${describe(value)}")
        rejectUnknown(map, keys, "$name.")
        return LinkedHashMap<String, Any?>().apply { map.forEach { (key, member) -> if (member != null) put(key.toString(), member) } }
    }

    private fun rejectUnknown(map: Map<*, *>, allowed: Set<String>, prefix: String = "") {
        val unknown = map.keys.map { it.toString() }.filter { it !in allowed }
        if (unknown.isNotEmpty()) throw invalid("unknown argument${if (unknown.size > 1) "s" else ""} ${unknown.joinToString(", ") { prefix + it }}")
    }

    private fun zoneOf(value: Any): ZoneId = try {
        ZoneId.of(string("timeZone", value).trim())
    } catch (_: DateTimeException) {
        throw invalid("timeZone must be an IANA zone id such as Asia/Shanghai, got ${describe(value)}")
    }

    private fun list(name: String, value: Any): List<Any?> = value as? List<*> ?: throw invalidRule("$name must be an array, got ${describe(value)}")

    private fun string(name: String, value: Any?): String = value as? String ?: throw invalid("$name must be a string, got ${describe(value)}")

    private fun bool(name: String, value: Any?): Boolean = value as? Boolean ?: throw invalid("$name must be true or false, got ${describe(value)}")

    private fun integral(name: String, value: Any?): Long {
        val number = (value as? Number)?.toDouble()
        if (number == null || number.isNaN() || number.isInfinite() || number != Math.floor(number)) throw invalid("$name must be an integer, got ${describe(value)}")
        return number.toLong()
    }

    companion object {

        /**
         * The `AlarmOptions` keys the tools take (appendix A 4.2 without `dismiss`; `holidays` since 1.2.0, T193).
         * zh-CN: 工具接受的选项键 (附录 A 4.2 去掉 `dismiss`; `holidays` 自 1.2.0 起, T193).
         */
        val OPTION_KEYS: Set<String> = linkedSetOf(
            "time", "date", "at", "repeat", "exclude", "holidays", "timeZone", "label", "enabled", "tag", "sound", "vibrate", "speak", "snooze",
            "autoSilenceMinutes",
        )

        /** T126: the arguments of `timer_start`. zh-CN: T126: `timer_start` 的参数. */
        val TIMER_KEYS: Set<String> = linkedSetOf("duration", "label", "sound", "vibrate", "speak", "autoSilenceMinutes")

        private val TIME_KEYS = setOf("hour", "minute", "second")
        private val SOUND_KEYS = setOf(AlarmOptionKeys.SOUND_SOURCE, AlarmOptionKeys.SOUND_VOLUME, AlarmOptionKeys.SOUND_RAMP_SECONDS)
        private val SPEAK_KEYS = setOf(
            AlarmOptionKeys.SPEAK_TEXT, AlarmOptionKeys.SPEAK_ENGINE, AlarmOptionKeys.SPEAK_LANGUAGE, AlarmOptionKeys.SPEAK_RATE,
            AlarmOptionKeys.SPEAK_PITCH, AlarmOptionKeys.SPEAK_MODE, AlarmOptionKeys.SPEAK_REPEAT_INTERVAL,
        )
        private val SOUND_GROUP = SOUND_KEYS
        private val SPEAK_GROUP = SPEAK_KEYS
        private val HOLIDAY_KEYS = setOf(AlarmOptionKeys.HOLIDAY_MODE, AlarmOptionKeys.HOLIDAY_REGION)
        private val SNOOZE_GROUP = setOf(AlarmOptionKeys.SNOOZE_ENABLED, AlarmOptionKeys.SNOOZE_MINUTES, AlarmOptionKeys.SNOOZE_MAX)

        private val STRUCTURED_PARTS = linkedMapOf(
            "freq" to "FREQ", "interval" to "INTERVAL", "count" to "COUNT", "until" to "UNTIL", "byMonth" to "BYMONTH", "byWeekNo" to "BYWEEKNO",
            "byYearDay" to "BYYEARDAY", "byMonthDay" to "BYMONTHDAY", "byDay" to "BYDAY", "bySetPos" to "BYSETPOS", "weekStart" to "WKST",
        )

        /** RRULE weekday codes by weekday number (0 Sunday ... 6 Saturday). zh-CN: 按星期编号排列的 RRULE 星期代码. */
        private val WEEKDAYS = listOf("SU", "MO", "TU", "WE", "TH", "FR", "SA")

        private val TIME_PATTERN = Regex("^(\\d{1,2}):(\\d{2})(?::(\\d{2}))?$")
        private val ISO_DATE_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        private val OFFSET_PATTERN = Regex("(Z|[+-]\\d{2}:?\\d{2})$", RegexOption.IGNORE_CASE)
        private val URI_SCHEME = Regex("^([A-Za-z][A-Za-z0-9+.-]*):")
        private val UNTIL_UTC = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

        fun describe(value: Any?): String = when (value) {
            null -> "null"
            is String -> "\"" + (if (value.length <= 64) value else value.substring(0, 64) + "...") + "\""
            is Number, is Boolean -> value.toString()
            is List<*> -> "an array"
            is Map<*, *> -> "an object"
            else -> value.javaClass.simpleName
        }

        private fun invalid(detail: String) = AlarmException.invalidOptions(detail)

        private fun invalidRule(detail: String) = AlarmException.invalidRule(detail)
    }
}
