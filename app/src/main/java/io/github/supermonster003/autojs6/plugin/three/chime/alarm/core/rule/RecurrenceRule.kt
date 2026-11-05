package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * A repeat rule of an alarm (roadmap P2.2, D11, appendix A section 4.3): the RFC 5545 RRULE subset the plugin computes.
 * [normalize] turns the three accepted inputs (an RRULE string with an optional `RRULE:` prefix, a structured
 * `RepeatRule`, or the weekday shorthand `[1, 2, 3, 4, 5]` with `0` Sunday to `6` Saturday) into one canonical RRULE
 * without the prefix, the form the plugin stores and `Alarm.repeat` reads back; [parse] gives the typed view of it.
 * Unsupported parts (`SECONDLY` / `MINUTELY` / `HOURLY`, `BYHOUR` / `BYMINUTE` / `BYSECOND`, `RDATE`, `EXRULE`,
 * `DTSTART` / `EXDATE` inside the rule) and the RFC 5545 combinations the plugin cannot honor fail with `INVALID_RULE`
 * naming the part. Canonical order: `FREQ`, `INTERVAL` (omitted when 1), `COUNT`, `UNTIL`, `BYMONTH`, `BYWEEKNO`,
 * `BYYEARDAY`, `BYMONTHDAY`, `BYDAY` (Monday first), `BYSETPOS`, `WKST` (omitted when `MO`); lists are sorted without
 * repeats. The normalization is the one of the host (`AlarmRepeatRules` of the `alarms` module, roadmap P1.4), so a
 * rule the host sends comes back unchanged.
 *
 * zh-CN: 闹钟的重复规则 (路线图 P2.2, D11, 附录 A 第 4.3 节): 插件计算的 RFC 5545 RRULE 子集. [normalize] 把三种输入 (可带 `RRULE:`
 * 前缀的 RRULE 字符串, 结构化 `RepeatRule`, 星期简写 `[1, 2, 3, 4, 5]`, `0` 为星期日至 `6` 为星期六) 转为一个不带前缀的规范 RRULE,
 * 即插件保存且 `Alarm.repeat` 回读的形式; [parse] 给出其类型化视图. 不支持的部分 (`SECONDLY` / `MINUTELY` / `HOURLY`, `BYHOUR` /
 * `BYMINUTE` / `BYSECOND`, `RDATE`, `EXRULE`, 规则内的 `DTSTART` / `EXDATE`) 与插件无法遵循的 RFC 5545 组合以 `INVALID_RULE` 失败并写明
 * 该部分. 规范顺序: `FREQ`, `INTERVAL` (为 1 时省略), `COUNT`, `UNTIL`, `BYMONTH`, `BYWEEKNO`, `BYYEARDAY`, `BYMONTHDAY`, `BYDAY`
 * (周一在前), `BYSETPOS`, `WKST` (为 `MO` 时省略); 列表排序且去重. 规范化与宿主一致 (`alarms` 模块的 `AlarmRepeatRules`, 路线图 P1.4),
 * 宿主发来的规则原样返回.
 */
data class RecurrenceRule(
    val freq: Freq,
    val interval: Int = 1,
    val count: Int? = null,
    val until: Until? = null,
    val byMonth: List<Int> = emptyList(),
    val byWeekNo: List<Int> = emptyList(),
    val byYearDay: List<Int> = emptyList(),
    val byMonthDay: List<Int> = emptyList(),
    val byDay: List<WeekdayNum> = emptyList(),
    val bySetPos: List<Int> = emptyList(),
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
) {

    enum class Freq { DAILY, WEEKLY, MONTHLY, YEARLY }

    /** A `BYDAY` entry: a weekday with an optional ordinal (`2TU`, `-1FR`). zh-CN: `BYDAY` 的一项: 星期加可选序号. */
    data class WeekdayNum(val ordinal: Int?, val day: DayOfWeek) {
        override fun toString(): String = (ordinal?.toString() ?: "") + codeOf(day)
    }

    /**
     * The `UNTIL` bound: a date (the whole day counts), a floating wall-clock moment, or a UTC instant.
     * zh-CN: `UNTIL` 上限: 日期 (整天都算), 浮动的墙上时间, 或 UTC 时刻.
     */
    sealed class Until {
        abstract val text: String

        /** Whether an occurrence at [local] (wall clock) / [instant] is still inside the bound. zh-CN: 该次是否仍在上限内. */
        abstract fun allows(local: LocalDateTime, instant: Instant): Boolean

        data class Date(val date: LocalDate) : Until() {
            override val text: String get() = date.format(DateTimeFormatter.BASIC_ISO_DATE)
            override fun allows(local: LocalDateTime, instant: Instant) = !local.toLocalDate().isAfter(date)
        }

        data class Floating(val moment: LocalDateTime) : Until() {
            override val text: String get() = moment.format(FLOATING)
            override fun allows(local: LocalDateTime, instant: Instant) = !local.isAfter(moment)
        }

        data class Utc(val instant: Instant) : Until() {
            override val text: String get() = UNTIL_UTC.format(instant)
            override fun allows(local: LocalDateTime, instant: Instant) = !instant.isAfter(this.instant)
        }
    }

    /** The canonical RRULE text. zh-CN: 规范 RRULE 文本. */
    val text: String
        get() = buildList {
            add("FREQ=${freq.name}")
            if (interval != 1) add("INTERVAL=$interval")
            count?.let { add("COUNT=$it") }
            until?.let { add("UNTIL=${it.text}") }
            if (byMonth.isNotEmpty()) add("BYMONTH=${byMonth.joinToString(",")}")
            if (byWeekNo.isNotEmpty()) add("BYWEEKNO=${byWeekNo.joinToString(",")}")
            if (byYearDay.isNotEmpty()) add("BYYEARDAY=${byYearDay.joinToString(",")}")
            if (byMonthDay.isNotEmpty()) add("BYMONTHDAY=${byMonthDay.joinToString(",")}")
            if (byDay.isNotEmpty()) add("BYDAY=${byDay.joinToString(",")}")
            if (bySetPos.isNotEmpty()) add("BYSETPOS=${bySetPos.joinToString(",")}")
            if (weekStart != DayOfWeek.MONDAY) add("WKST=${codeOf(weekStart)}")
        }.joinToString(";")

    /**
     * The rule without `UNTIL`, for lib-recur: it refuses a UTC `UNTIL` with the floating start the plugin uses, so the
     * bound is applied by [until] instead (`COUNT` and `UNTIL` are exclusive, nothing else depends on it).
     * zh-CN: 去掉 `UNTIL` 的规则, 交给 lib-recur: 它拒绝浮动起点配 UTC `UNTIL`, 因此上限改由 [until] 判断 (`COUNT` 与 `UNTIL` 互斥, 别无依赖).
     */
    val textWithoutUntil: String get() = copy(until = null).text

    /** Whether every `BYDAY` entry is a plain weekday. zh-CN: `BYDAY` 是否全为不带序号的星期. */
    val plainDays: Boolean get() = byDay.all { it.ordinal == null }

    override fun toString(): String = text

    companion object {

        /** RRULE weekday codes by script weekday number (`0` Sunday ... `6` Saturday). zh-CN: 按脚本星期编号排列的 RRULE 星期代码. */
        val WEEKDAYS: List<String> = listOf("SU", "MO", "TU", "WE", "TH", "FR", "SA")

        private val MONDAY_FIRST = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
        private val FREQS = Freq.entries.map { it.name }
        private val SUB_DAILY_FREQS = setOf("SECONDLY", "MINUTELY", "HOURLY")
        private val TIME_PARTS = setOf("BYHOUR", "BYMINUTE", "BYSECOND")
        private val PROPERTIES = setOf("DTSTART", "EXDATE", "RDATE", "EXRULE")
        private val ORDER = listOf("FREQ", "INTERVAL", "COUNT", "UNTIL", "BYMONTH", "BYWEEKNO", "BYYEARDAY", "BYMONTHDAY", "BYDAY", "BYSETPOS", "WKST")
        private val BY_PARTS = setOf("BYMONTH", "BYWEEKNO", "BYYEARDAY", "BYMONTHDAY", "BYDAY")

        private val STRUCTURED_KEYS = mapOf(
            "freq" to "FREQ", "interval" to "INTERVAL", "count" to "COUNT", "until" to "UNTIL",
            "byMonth" to "BYMONTH", "byWeekNo" to "BYWEEKNO", "byYearDay" to "BYYEARDAY", "byMonthDay" to "BYMONTHDAY",
            "byDay" to "BYDAY", "bySetPos" to "BYSETPOS", "weekStart" to "WKST",
        )
        private val STRUCTURED_UNSUPPORTED = mapOf(
            "byHour" to "BYHOUR", "byMinute" to "BYMINUTE", "bySecond" to "BYSECOND",
            "rdate" to "RDATE", "exrule" to "EXRULE", "dtstart" to "DTSTART", "exdate" to "EXDATE",
        )

        private val BY_DAY_PATTERN = Regex("^([+-]?[0-9]{1,2})?(SU|MO|TU|WE|TH|FR|SA)$")
        private val UNTIL_PATTERN = Regex("^([0-9]{8})(?:T([0-9]{6})(Z)?)?$")
        private val ISO_DATE_PATTERN = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
        private val UNTIL_UTC: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
        private val FLOATING: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
        private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmmss")

        /** The RRULE code of [day]. zh-CN: [day] 的 RRULE 代码. */
        fun codeOf(day: DayOfWeek): String = MONDAY_FIRST[day.value - 1]

        private fun dayOf(code: String): DayOfWeek = DayOfWeek.of(MONDAY_FIRST.indexOf(code) + 1)

        /**
         * The canonical rule of a `repeat` value: a String (RRULE), a List (weekday shorthand) or a Map (structured
         * rule); numbers may be any [Number].
         * zh-CN: `repeat` 值的规范规则: 字符串 (RRULE), 列表 (星期简写) 或映射 (结构化规则); 数字可为任意 [Number].
         */
        fun normalize(value: Any?): String = when (value) {
            is String -> fromString(value)
            is List<*> -> fromWeekdays(value)
            is Map<*, *> -> fromStructured(value)
            else -> throw AlarmException.invalidOptions("repeat must be an RRULE string, a rule object or an array of weekday numbers (0 - 6)")
        }

        /** The typed view of an RRULE string, normalized first. zh-CN: RRULE 字符串的类型化视图, 先规范化. */
        fun parse(rule: String): RecurrenceRule {
            val parts = fromString(rule).split(';').associate {
                val eq = it.indexOf('=')
                it.substring(0, eq) to it.substring(eq + 1)
            }
            fun ints(name: String) = parts[name]?.split(',')?.map { it.toInt() } ?: emptyList()
            return RecurrenceRule(
                freq = Freq.valueOf(parts.getValue("FREQ")),
                interval = parts["INTERVAL"]?.toInt() ?: 1,
                count = parts["COUNT"]?.toInt(),
                until = parts["UNTIL"]?.let(::untilOf),
                byMonth = ints("BYMONTH"),
                byWeekNo = ints("BYWEEKNO"),
                byYearDay = ints("BYYEARDAY"),
                byMonthDay = ints("BYMONTHDAY"),
                byDay = parts["BYDAY"]?.split(',')?.map { entry ->
                    val match = BY_DAY_PATTERN.matchEntire(entry)!!
                    WeekdayNum(match.groupValues[1].takeIf { it.isNotEmpty() }?.toInt(), dayOf(match.groupValues[2]))
                } ?: emptyList(),
                bySetPos = ints("BYSETPOS"),
                weekStart = parts["WKST"]?.let(::dayOf) ?: DayOfWeek.MONDAY,
            )
        }

        /** The canonical form of an RRULE string. zh-CN: RRULE 字符串的规范形式. */
        fun fromString(text: String): String {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) throw invalidRule("repeat: the rule is empty")
            if (trimmed.any { it.code == 10 || it.code == 13 }) {
                throw invalidRule("repeat: only one RRULE line is supported (use date for the first day and exclude for excluded dates)")
            }
            var body = trimmed
            val colon = body.indexOf(':')
            if (colon >= 0) {
                val name = body.substring(0, colon).trim().uppercase()
                when {
                    name == "RRULE" -> body = body.substring(colon + 1)
                    name in PROPERTIES -> throw unsupportedProperty(name)
                    else -> throw invalidRule("repeat: unknown property '${body.substring(0, colon).trim()}'")
                }
            }
            val parts = LinkedHashMap<String, String>()
            for (segment in body.split(';')) {
                val part = segment.trim()
                if (part.isEmpty()) continue
                val eq = part.indexOf('=')
                if (eq <= 0) throw invalidRule("repeat: malformed part '$part'")
                val name = part.substring(0, eq).trim().uppercase()
                val partValue = part.substring(eq + 1).trim()
                if (name in parts) throw invalidRule("repeat: $name appears more than once")
                if (partValue.isEmpty()) throw invalidRule("repeat: $name has no value")
                parts[name] = partValue
            }
            return build(parts)
        }

        /** `FREQ=WEEKLY;BYDAY=...` of the weekday shorthand. zh-CN: 星期简写对应的 `FREQ=WEEKLY;BYDAY=...`. */
        fun fromWeekdays(days: List<*>): String {
            if (days.isEmpty()) throw invalidRule("repeat: the weekday array is empty")
            val codes = days.map { day ->
                val number = (day as? Number)?.toDouble()
                if (number == null || number != Math.floor(number) || number < 0 || number > 6) {
                    throw invalidRule("repeat: weekday numbers are 0 (Sunday) to 6 (Saturday), got ${describe(day)}")
                }
                WEEKDAYS[number.toInt()]
            }
            return build(linkedMapOf("FREQ" to "WEEKLY", "BYDAY" to codes.joinToString(",")))
        }

        /** The canonical form of a structured rule (appendix A 4.3 `RepeatRule`). zh-CN: 结构化规则的规范形式. */
        fun fromStructured(rule: Map<*, *>): String {
            rule.keys.firstNotNullOfOrNull { STRUCTURED_UNSUPPORTED[it] }?.let { throw unsupportedPart(it) }
            val unknown = rule.keys.filter { it !in STRUCTURED_KEYS }
            if (unknown.isNotEmpty()) throw AlarmException.invalidOptions("repeat: unknown key${if (unknown.size > 1) "s" else ""} ${unknown.joinToString(", ")}")
            val parts = LinkedHashMap<String, String>()
            for ((key, part) in STRUCTURED_KEYS) {
                val value = rule[key] ?: continue
                parts[part] = when (key) {
                    "freq" -> (value as? String)?.trim()?.uppercase() ?: throw invalidRule("repeat.freq must be 'daily', 'weekly', 'monthly' or 'yearly'")
                    "interval", "count" -> integerText("repeat.$key", value)
                    "until" -> untilText(value)
                    "byDay" -> listOf(value).flatMapList().joinToString(",") { byDayText(it) }
                    "weekStart" -> weekStartText(value)
                    else -> listOf(value).flatMapList().joinToString(",") { integerText("repeat.$key", it) }
                }
            }
            return build(parts)
        }

        private fun build(parts: Map<String, String>): String {
            parts.keys.firstOrNull { it in TIME_PARTS }?.let { throw unsupportedPart(it) }
            parts.keys.firstOrNull { it in PROPERTIES }?.let { throw unsupportedProperty(it) }
            parts.keys.firstOrNull { it !in ORDER }?.let { throw invalidRule("repeat: unknown part $it") }
            val freq = parts["FREQ"]?.uppercase() ?: throw invalidRule("repeat: FREQ is required")
            if (freq in SUB_DAILY_FREQS) throw invalidRule("repeat: FREQ=$freq is not supported (an alarm rings once a day at its time; use DAILY, WEEKLY, MONTHLY or YEARLY)")
            if (freq !in FREQS) throw invalidRule("repeat: unknown FREQ=$freq")
            val out = HashMap<String, String>()
            out["FREQ"] = freq
            parts["INTERVAL"]?.let { val n = positive("INTERVAL", it); if (n != 1) out["INTERVAL"] = n.toString() }
            val count = parts["COUNT"]?.let { positive("COUNT", it) }
            val until = parts["UNTIL"]?.let { until(it) }
            if (count != null && until != null) throw invalidRule("repeat: COUNT and UNTIL are exclusive")
            count?.let { out["COUNT"] = it.toString() }
            until?.let { out["UNTIL"] = it }
            parts["BYMONTH"]?.let { out["BYMONTH"] = numbers("BYMONTH", it, 12, signed = false) }
            parts["BYWEEKNO"]?.let {
                if (freq != "YEARLY") throw invalidRule("repeat: BYWEEKNO needs FREQ=YEARLY")
                out["BYWEEKNO"] = numbers("BYWEEKNO", it, 53, signed = true)
            }
            parts["BYYEARDAY"]?.let {
                if (freq != "YEARLY") throw invalidRule("repeat: BYYEARDAY is not allowed with FREQ=$freq")
                out["BYYEARDAY"] = numbers("BYYEARDAY", it, 366, signed = true)
            }
            parts["BYMONTHDAY"]?.let {
                if (freq == "WEEKLY") throw invalidRule("repeat: BYMONTHDAY is not allowed with FREQ=WEEKLY")
                out["BYMONTHDAY"] = numbers("BYMONTHDAY", it, 31, signed = true)
            }
            parts["BYDAY"]?.let { out["BYDAY"] = byDay(it, freq, "BYWEEKNO" in parts) }
            parts["BYSETPOS"]?.let {
                if (BY_PARTS.none { part -> part in parts }) throw invalidRule("repeat: BYSETPOS needs another BY part")
                out["BYSETPOS"] = numbers("BYSETPOS", it, 366, signed = true)
            }
            parts["WKST"]?.let {
                val day = it.uppercase()
                if (day !in WEEKDAYS) throw invalidRule("repeat: WKST must be one of ${MONDAY_FIRST.joinToString(", ")}")
                if (day != "MO") out["WKST"] = day
            }
            return ORDER.mapNotNull { key -> out[key]?.let { "$key=$it" } }.joinToString(";")
        }

        private fun positive(name: String, text: String): Int {
            val n = text.removePrefix("+").toIntOrNull()
            if (n == null || n <= 0) throw invalidRule("repeat: $name must be a positive integer, got '$text'")
            return n
        }

        private fun until(text: String): String {
            val upper = text.uppercase()
            val match = UNTIL_PATTERN.matchEntire(upper) ?: throw invalidRule("repeat: UNTIL must be yyyyMMdd or yyyyMMddTHHmmss[Z], got '$text'")
            try {
                LocalDate.parse(match.groupValues[1], DateTimeFormatter.BASIC_ISO_DATE)
                match.groupValues[2].takeIf { it.isNotEmpty() }?.let { LocalTime.parse(it, CLOCK) }
            } catch (_: Exception) {
                throw invalidRule("repeat: UNTIL is not a valid date, got '$text'")
            }
            return upper
        }

        private fun untilOf(text: String): Until {
            val match = UNTIL_PATTERN.matchEntire(text)!!
            val date = LocalDate.parse(match.groupValues[1], DateTimeFormatter.BASIC_ISO_DATE)
            if (match.groupValues[2].isEmpty()) return Until.Date(date)
            val moment = LocalDateTime.of(date, LocalTime.parse(match.groupValues[2], CLOCK))
            return if (match.groupValues[3].isEmpty()) Until.Floating(moment) else Until.Utc(moment.toInstant(ZoneOffset.UTC))
        }

        private fun numbers(name: String, text: String, max: Int, signed: Boolean): String {
            val values = text.split(',').map { item ->
                val trimmed = item.trim()
                val n = trimmed.removePrefix("+").toIntOrNull()
                val valid = n != null && n != 0 && (if (signed) Math.abs(n) <= max else n in 1..max)
                if (!valid) throw invalidRule("repeat: $name values are ${if (signed) "-$max to -1 or 1" else "1"} to $max, got '$trimmed'")
                n!!
            }
            return values.distinct().sorted().joinToString(",")
        }

        private fun byDay(text: String, freq: String, hasWeekNo: Boolean): String {
            val days = text.split(',').map { item ->
                val trimmed = item.trim().uppercase()
                val match = BY_DAY_PATTERN.matchEntire(trimmed) ?: throw invalidRule("repeat: BYDAY values are MO ... SU with an optional ordinal (2TU, -1FR), got '${item.trim()}'")
                val ordinal = match.groupValues[1].takeIf { it.isNotEmpty() }?.removePrefix("+")?.toInt()
                if (ordinal != null) {
                    val limit = when (freq) {
                        "MONTHLY" -> 5
                        "YEARLY" -> 53
                        else -> throw invalidRule("repeat: BYDAY with an ordinal ('${item.trim()}') needs FREQ=MONTHLY or FREQ=YEARLY")
                    }
                    if (freq == "YEARLY" && hasWeekNo) throw invalidRule("repeat: BYDAY with an ordinal is not allowed together with BYWEEKNO")
                    if (ordinal == 0 || Math.abs(ordinal) > limit) throw invalidRule("repeat: the BYDAY ordinal is -$limit to -1 or 1 to $limit with FREQ=$freq, got '${item.trim()}'")
                }
                ordinal to match.groupValues[2]
            }
            return days.distinct()
                .sortedWith(compareBy<Pair<Int?, String>>({ MONDAY_FIRST.indexOf(it.second) }, { it.first != null }, { it.first ?: 0 }))
                .joinToString(",") { (ordinal, day) -> (ordinal?.toString() ?: "") + day }
        }

        private fun integerText(name: String, value: Any?): String {
            val number = (value as? Number)?.toDouble() ?: (value as? String)?.trim()?.toDoubleOrNull()
            if (number == null || number != Math.floor(number) || number.isInfinite()) throw invalidRule("$name must be an integer, got ${describe(value)}")
            return number.toLong().toString()
        }

        private fun byDayText(value: Any?): String = when (value) {
            is Number -> {
                val number = value.toDouble()
                if (number != Math.floor(number) || number < 0 || number > 6) throw invalidRule("repeat.byDay numbers are 0 (Sunday) to 6 (Saturday), got ${describe(value)}")
                WEEKDAYS[number.toInt()]
            }
            is String -> value.trim()
            else -> throw invalidRule("repeat.byDay entries are 0 - 6 or RRULE weekdays such as 'MO', '2TU', '-1FR', got ${describe(value)}")
        }

        private fun untilText(value: Any?): String = when (value) {
            is Number -> {
                val millis = value.toDouble()
                if (millis.isNaN() || millis.isInfinite()) throw invalidRule("repeat.until must be a date, got ${describe(value)}")
                UNTIL_UTC.format(Instant.ofEpochMilli(millis.toLong()))
            }
            is String -> {
                val trimmed = value.trim()
                if (ISO_DATE_PATTERN.matches(trimmed)) {
                    try {
                        LocalDate.parse(trimmed).format(DateTimeFormatter.BASIC_ISO_DATE)
                    } catch (_: Exception) {
                        throw invalidRule("repeat.until is not a valid date, got '$trimmed'")
                    }
                } else trimmed
            }
            else -> throw invalidRule("repeat.until must be 'YYYY-MM-DD', a Date or a timestamp, got ${describe(value)}")
        }

        private fun weekStartText(value: Any?): String = when ((value as? Number)?.toDouble()) {
            0.0 -> "SU"
            1.0 -> "MO"
            else -> throw invalidRule("repeat.weekStart must be 0 (Sunday) or 1 (Monday), got ${describe(value)}")
        }

        private fun List<Any?>.flatMapList(): List<Any?> = flatMap { if (it is List<*>) it else listOf(it) }

        private fun unsupportedPart(name: String): AlarmException = when (name) {
            "RDATE", "EXRULE", "DTSTART", "EXDATE" -> unsupportedProperty(name)
            else -> invalidRule("repeat: $name is not supported (an alarm has one ring time per day, set by time)")
        }

        private fun unsupportedProperty(name: String): AlarmException = when (name) {
            "DTSTART" -> invalidRule("repeat: DTSTART is not supported in the rule (use date for the first day)")
            "EXDATE" -> invalidRule("repeat: EXDATE is not supported in the rule (use exclude)")
            else -> invalidRule("repeat: $name is not supported")
        }

        private fun describe(value: Any?): String = when (value) {
            null -> "null"
            is String -> "'${value.take(64)}'"
            is Number -> value.toDouble().let { if (it == Math.floor(it) && !it.isInfinite()) it.toLong().toString() else it.toString() }
            is Boolean -> value.toString()
            is List<*> -> "an array"
            is Map<*, *> -> "an object"
            else -> value.toString()
        }

        private fun invalidRule(message: String): AlarmException = AlarmException.invalidRule(message)
    }
}
