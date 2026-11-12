package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import org.autojs.plugin.alarm.api.AlarmLimits

/**
 * Timer durations (roadmap P7.1, appendix A section 6, T107): a whole number of milliseconds, or a text of `h` / `m` /
 * `s` parts in this order, each at most once, with whole numbers ("25m", "1h30m", "90s", "1h30m15s"); case does not
 * matter and spaces may separate the parts. [parseSigned] also takes a leading `-` (the `addTime` amount). The same
 * notation as the host (its T130), so a script and a tool read a duration the same way.
 * zh-CN: 计时器时长 (路线图 P7.1, 附录 A 第 6 节, T107): 整数毫秒, 或由 `h` / `m` / `s` 按此顺序各至多出现一次的整数段组成的文本 ("25m", "1h30m",
 * "90s", "1h30m15s"), 不分大小写, 段间可有空格. [parseSigned] 另接受前导 `-` (`addTime` 的加时量). 写法与宿主 (其 T130) 相同, 脚本与工具读法一致.
 */
object TimerDurations {

    const val MIN_MILLIS: Long = AlarmLimits.MIN_TIMER_MILLIS
    const val MAX_MILLIS: Long = AlarmLimits.MAX_TIMER_MILLIS

    private val PATTERN = Regex("""^(?:(\d+)h)?\s*(?:(\d+)m)?\s*(?:(\d+)s)?$""", RegexOption.IGNORE_CASE)

    /** Milliseconds of [text] (no sign); throws `INVALID_OPTIONS` for any other notation. zh-CN: [text] 的毫秒数 (无符号), 其他写法抛 `INVALID_OPTIONS`. */
    fun parse(text: String, name: String = "duration"): Long {
        val trimmed = text.trim()
        val match = PATTERN.matchEntire(trimmed)
        if (trimmed.isEmpty() || match == null || match.groupValues.drop(1).all { it.isEmpty() }) {
            throw AlarmException.invalidOptions("$name must be milliseconds or a text like \"25m\", \"1h30m\" or \"90s\", got \"${clip(text)}\"")
        }
        val (hours, minutes, seconds) = match.destructured
        return try {
            val total = part(hours, HOUR_MILLIS) + part(minutes, MINUTE_MILLIS) + part(seconds, SECOND_MILLIS)
            if (total < 0) throw ArithmeticException()
            total
        } catch (_: ArithmeticException) {
            throw AlarmException.invalidOptions("$name is too long: \"${clip(text)}\"")
        } catch (_: NumberFormatException) {
            throw AlarmException.invalidOptions("$name is too long: \"${clip(text)}\"")
        }
    }

    /** Like [parse] with an optional leading `-`. zh-CN: 与 [parse] 相同, 另接受前导 `-`. */
    fun parseSigned(text: String, name: String = "millis"): Long {
        val trimmed = text.trim()
        return if (trimmed.startsWith("-")) -parse(trimmed.substring(1), name) else parse(trimmed, name)
    }

    /** [millis] when it is a timer duration (1 s - 100 h), else `INVALID_OPTIONS`. zh-CN: [millis] 为合法计时器时长 (1 s - 100 h) 时返回, 否则 `INVALID_OPTIONS`. */
    fun requireInRange(millis: Long, name: String = "durationMillis"): Long {
        if (millis !in MIN_MILLIS..MAX_MILLIS) {
            throw AlarmException.invalidOptions("$name must be in $MIN_MILLIS - $MAX_MILLIS ms (1 s - 100 h), got $millis")
        }
        return millis
    }

    /**
     * [millis] as a clock reading, whole seconds rounded up: `1:30:00` from one hour on, else `25:00` or `0:59`.
     * zh-CN: 把 [millis] 写成钟面读数, 秒向上取整: 满一小时为 `1:30:00`, 否则为 `25:00` 或 `0:59`.
     */
    fun clockText(millis: Long): String {
        val seconds = (millis.coerceAtLeast(0L) + SECOND_MILLIS - 1) / SECOND_MILLIS
        val hours = seconds / 3600
        val minutes = seconds / 60 % 60
        val rest = seconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(java.util.Locale.ROOT, hours, minutes, rest) else "%d:%02d".format(java.util.Locale.ROOT, minutes, rest)
    }

    private fun part(digits: String, unit: Long): Long = if (digits.isEmpty()) 0L else Math.multiplyExact(digits.toLong(), unit)

    private fun clip(text: String): String = if (text.length <= 64) text else text.substring(0, 64) + "..."

    private const val SECOND_MILLIS = 1_000L
    private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
    private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
}
