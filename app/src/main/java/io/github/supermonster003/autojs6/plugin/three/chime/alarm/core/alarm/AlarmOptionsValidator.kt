package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm

import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmDismiss
import org.autojs.plugin.alarm.api.AlarmFeatures
import org.autojs.plugin.alarm.api.AlarmHolidayFilter
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import org.autojs.plugin.alarm.api.AlarmSnooze
import org.autojs.plugin.alarm.api.AlarmSound
import org.autojs.plugin.alarm.api.AlarmSpeak
import org.autojs.plugin.alarm.api.AlarmSpec
import org.autojs.plugin.alarm.api.AlarmTime
import org.autojs.plugin.alarm.api.AlarmValues
import java.time.DateTimeException
import java.time.LocalDate
import java.time.ZoneId

/**
 * The plugin-side check of the alarm options (protocol "Alarm options", appendix A sections 4.2 and 11): the host
 * normalizes script options before the call, the plugin validates them again and is the authority on limits.
 *
 * - [options] checks a complete option tree (`addAlarm`, `preview`, a stored record) and answers the typed view; a key
 *   present with a null value counts as absent.
 * - [patch] applies a T29 patch to a stored alarm and checks the result as a whole.
 *
 * Unknown keys (top level and inside the groups), mistyped values and out-of-range values throw [AlarmException] with
 * `INVALID_OPTIONS` naming the key; text over its limit, more than 366 excluded dates or more than 64 vibration entries
 * throw `LIMIT_EXCEEDED`; an option of a feature the plugin does not declare (`holidays`, `dismiss` in 1.0.0) throws
 * `UNSUPPORTED`. [normalizeRule] turns `repeat` into the normalized RRULE (roadmap P2.2) and throws `INVALID_RULE`.
 *
 * zh-CN: 插件侧的闹钟选项校验 (协议 "Alarm options", 附录 A 第 4.2 与 11 节): 宿主在调用前规范化脚本选项, 插件再校验一次, 限制以插件为准.
 * [options] 校验完整的选项树 (`addAlarm`, `preview`, 已存记录) 并返回类型化视图, 值为 null 的键视同缺省; [patch] 把 T29 patch 应用到已存闹钟上,
 * 并对结果整体校验. 未知键 (顶层与组内), 类型不符与超出范围报 `INVALID_OPTIONS` 并写明键名; 文本超长, 排除日期超过 366 个, 振动节奏超过 64 项报
 * `LIMIT_EXCEEDED`; 插件未声明的功能的选项 (1.0.0 的 `holidays`, `dismiss`) 报 `UNSUPPORTED`. [normalizeRule] 把 `repeat` 转为规范化的
 * RRULE (路线图 P2.2), 规则不合法时报 `INVALID_RULE`.
 */
class AlarmOptionsValidator(
    private val features: Set<String> = setOf(AlarmFeatures.ALARMS),
    private val normalizeRule: (String) -> String = { it },
) {

    fun options(values: Map<String, Any?>): AlarmSpec {
        val reader = Reader(values, "")
        reader.rejectUnknown(AlarmSpec.KEYS)
        val time = time(reader)
        val at = reader.long(AlarmOptionKeys.AT, 0, Long.MAX_VALUE)
        if (time != null && at != null) throw invalid("hour / minute and at are exclusive")
        if (time == null && at == null) throw invalid("an alarm needs hour and minute, or at")
        val date = reader.date(AlarmOptionKeys.DATE)
        val repeat = reader.string(AlarmOptionKeys.REPEAT)?.let { rule ->
            if (rule.isBlank()) throw AlarmException.invalidRule("repeat is empty")
            normalizeRule(rule)
        }
        if (at != null && repeat != null) throw invalid("at is a one-shot moment and cannot repeat (use hour and minute with repeat)")
        if (at != null && date != null) throw invalid("at already holds the date (use hour and minute with date)")
        val exclude = reader.stringList(AlarmOptionKeys.EXCLUDE)?.let { dates ->
            dates.forEachIndexed { index, text -> if (!isDate(text)) throw invalid("exclude[$index] must be a valid date YYYY-MM-DD, got \"${clip(text)}\"") }
            val sorted = dates.toSortedSet().toList()
            if (sorted.size > AlarmLimits.MAX_EXCLUDE_DATES) throw limit("exclude has ${sorted.size} dates, at most ${AlarmLimits.MAX_EXCLUDE_DATES}")
            sorted
        }
        val timeZone = reader.string(AlarmOptionKeys.TIME_ZONE)?.let { zone ->
            if (zone.length > MAX_NAME_CHARS || !isZone(zone)) throw invalid("timeZone must be an IANA zone id, got \"${clip(zone)}\"")
            zone
        }
        val holidays = reader.group(AlarmOptionKeys.HOLIDAYS)?.let { holidays(it) }
        val label = reader.text(AlarmOptionKeys.LABEL, AlarmLimits.MAX_LABEL_CHARS)
        val enabled = reader.boolean(AlarmOptionKeys.ENABLED)
        val tag = reader.text(AlarmOptionKeys.TAG, AlarmLimits.MAX_TAG_CHARS)
        val sound = reader.group(AlarmOptionKeys.SOUND)?.let { sound(it) }
        val vibrate = reader.boolean(AlarmOptionKeys.VIBRATE)
        val vibratePattern = vibratePattern(reader)
        if (vibrate == false && vibratePattern != null) throw invalid("vibratePattern implies vibrate, but vibrate is false")
        val speak = reader.group(AlarmOptionKeys.SPEAK)?.let { speak(it) }
        val snooze = reader.group(AlarmOptionKeys.SNOOZE)?.let { snooze(it) }
        val autoSilence = reader.int(AlarmOptionKeys.AUTO_SILENCE_MINUTES, AlarmLimits.MIN_AUTO_SILENCE_MINUTES, AlarmLimits.MAX_AUTO_SILENCE_MINUTES)
        val dismiss = reader.group(AlarmOptionKeys.DISMISS)?.let { dismiss(it) }
        return AlarmSpec(
            time = time,
            date = date,
            at = at,
            repeat = repeat,
            exclude = exclude,
            timeZone = timeZone,
            holidays = holidays,
            label = label,
            enabled = enabled,
            tag = tag,
            sound = sound,
            vibrate = vibrate,
            vibratePattern = vibratePattern,
            speak = speak,
            snooze = snooze,
            autoSilenceMinutes = autoSilence,
            dismiss = dismiss,
        )
    }

    /**
     * Applies [patch] (protocol "Patch semantics") to [stored]: an absent key keeps the stored value, null clears a
     * clearable option, a group replaces the stored group, `at` drops the stored time and date, `hour` / `minute`
     * replace the stored time as a whole and drop `at`, and `vibrate: false` drops a stored pattern. The merged tree is
     * then checked by [options]; `at` on a repeating alarm must clear `repeat` in the same patch.
     *
     * zh-CN: 把 [patch] (协议 "Patch semantics") 应用到 [stored]: 缺省的键保留原值, null 清除可清除的选项, 分组整体替换, `at` 去掉已存的时间与日期,
     * `hour` / `minute` 整体替换已存时间并去掉 `at`, `vibrate: false` 去掉已存的振动节奏. 合并后的树再由 [options] 校验; 重复闹钟改用 `at` 时必须在
     * 同一个 patch 中清除 `repeat`.
     */
    fun patch(stored: AlarmSpec, patch: Map<String, Any?>): AlarmSpec {
        Reader(patch, "").rejectUnknown(AlarmSpec.KEYS)
        if (patch.isEmpty()) throw invalid("patch has nothing to change")
        for (key in NOT_CLEARABLE_KEYS) if (patch.containsKey(key) && patch[key] == null) throw invalid("$key cannot be null")
        val merged = LinkedHashMap(stored.toValues())
        val timeKeys = TIME_KEYS.filter(patch::containsKey)
        if (timeKeys.isNotEmpty()) {
            if (!patch.containsKey(AlarmOptionKeys.HOUR) || !patch.containsKey(AlarmOptionKeys.MINUTE)) {
                throw invalid("a patch of the time needs both hour and minute")
            }
            if (patch.containsKey(AlarmOptionKeys.AT)) throw invalid("hour / minute and at are exclusive")
            TIME_KEYS.forEach(merged::remove)
            merged.remove(AlarmOptionKeys.AT)
        }
        if (patch.containsKey(AlarmOptionKeys.AT)) {
            TIME_KEYS.forEach(merged::remove)
            merged.remove(AlarmOptionKeys.DATE)
            if (merged[AlarmOptionKeys.REPEAT] != null && !patch.containsKey(AlarmOptionKeys.REPEAT)) {
                throw invalid("at is a one-shot moment; clear repeat (null) in the same patch to turn a repeating alarm into a one-shot one")
            }
        }
        if (patch[AlarmOptionKeys.VIBRATE] == false && !patch.containsKey(AlarmOptionKeys.VIBRATE_PATTERN)) {
            merged.remove(AlarmOptionKeys.VIBRATE_PATTERN)
        }
        for ((key, value) in patch) {
            if (value == null) merged.remove(key) else merged[key] = value
        }
        return options(merged)
    }

    // ------------------------------------------------------------------ groups

    private fun time(reader: Reader): AlarmTime? {
        val hour = reader.int(AlarmOptionKeys.HOUR, 0, 23)
        val minute = reader.int(AlarmOptionKeys.MINUTE, 0, 59)
        val second = reader.int(AlarmOptionKeys.SECOND, 0, 59)
        if (hour == null && minute == null) {
            if (second != null) throw invalid("second needs hour and minute")
            return null
        }
        if (hour == null || minute == null) throw invalid("hour and minute must be given together")
        return AlarmTime(hour, minute, second ?: 0)
    }

    private fun holidays(values: Map<String, Any?>): AlarmHolidayFilter {
        if (AlarmFeatures.HOLIDAYS !in features) throw AlarmException.unsupported("holidays needs the holidays feature (3-Chime Alarm 1.2.0)")
        val reader = Reader(values, AlarmOptionKeys.HOLIDAYS + ".")
        reader.rejectUnknown(HOLIDAY_KEYS)
        val region = reader.name(AlarmOptionKeys.HOLIDAY_REGION)
        val mode = reader.string(AlarmOptionKeys.HOLIDAY_MODE) ?: throw invalid("holidays.mode is required")
        if (mode !in AlarmContract.HOLIDAY_MODES) throw invalid("holidays.mode must be one of ${AlarmContract.HOLIDAY_MODES.joinToString(" / ")}, got \"${clip(mode)}\"")
        return AlarmHolidayFilter(region, mode)
    }

    private fun sound(values: Map<String, Any?>): AlarmSound {
        val reader = Reader(values, AlarmOptionKeys.SOUND + ".")
        reader.rejectUnknown(SOUND_KEYS)
        val source = reader.string(AlarmOptionKeys.SOUND_SOURCE)?.also { source ->
            val known = source == AlarmContract.SOUND_DEFAULT || source == AlarmContract.SOUND_SILENT
            if (!known && (source.length > MAX_URI_CHARS || !URI_SCHEME.containsMatchIn(source))) {
                throw invalid("sound.source must be \"default\", \"silent\" or a ringtone uri, got \"${clip(source)}\"")
            }
        }
        val volume = reader.double(AlarmOptionKeys.SOUND_VOLUME)?.also { volume ->
            if (volume.isNaN() || volume < AlarmLimits.MIN_VOLUME || volume > AlarmLimits.MAX_VOLUME) {
                throw invalid("sound.volume must be in ${AlarmLimits.MIN_VOLUME} - ${AlarmLimits.MAX_VOLUME}, got $volume")
            }
        }
        val ramp = reader.int(AlarmOptionKeys.SOUND_RAMP_SECONDS, 0, AlarmLimits.MAX_RAMP_SECONDS)
        return AlarmSound(source, volume, ramp)
    }

    private fun vibratePattern(reader: Reader): List<Long>? {
        val key = AlarmOptionKeys.VIBRATE_PATTERN
        val raw = reader.values[key] ?: return null
        val pattern = AlarmValues.longList(reader.values, key) ?: throw typeError(key, "an array of milliseconds", raw)
        if (pattern.isEmpty()) throw invalid("$key needs at least one entry")
        if (pattern.size > AlarmLimits.MAX_VIBRATE_PATTERN) throw limit("$key has ${pattern.size} entries, at most ${AlarmLimits.MAX_VIBRATE_PATTERN}")
        pattern.forEachIndexed { index, millis -> if (millis < 0) throw invalid("$key[$index] must not be negative, got $millis") }
        if (pattern.all { it == 0L }) throw invalid("$key needs at least one non-zero entry")
        return pattern
    }

    private fun speak(values: Map<String, Any?>): AlarmSpeak {
        val reader = Reader(values, AlarmOptionKeys.SPEAK + ".")
        reader.rejectUnknown(SPEAK_KEYS)
        val text = reader.text(AlarmOptionKeys.SPEAK_TEXT, AlarmLimits.MAX_SPEAK_TEXT_CHARS) ?: throw invalid("speak.text is required")
        if (text.isBlank()) throw invalid("speak.text must not be blank")
        val mode = reader.string(AlarmOptionKeys.SPEAK_MODE)?.also { mode ->
            if (mode !in AlarmContract.SPEAK_MODES) throw invalid("speak.mode must be one of ${AlarmContract.SPEAK_MODES.joinToString(" / ")}, got \"${clip(mode)}\"")
        }
        return AlarmSpeak(
            text = text,
            engine = reader.name(AlarmOptionKeys.SPEAK_ENGINE),
            language = reader.name(AlarmOptionKeys.SPEAK_LANGUAGE),
            rate = reader.positive(AlarmOptionKeys.SPEAK_RATE),
            pitch = reader.positive(AlarmOptionKeys.SPEAK_PITCH),
            mode = mode,
            repeatInterval = reader.int(AlarmOptionKeys.SPEAK_REPEAT_INTERVAL, 0, AlarmLimits.MAX_SPEAK_REPEAT_INTERVAL_SECONDS),
        )
    }

    private fun snooze(values: Map<String, Any?>): AlarmSnooze {
        val reader = Reader(values, AlarmOptionKeys.SNOOZE + ".")
        reader.rejectUnknown(SNOOZE_KEYS)
        return AlarmSnooze(
            enabled = reader.boolean(AlarmOptionKeys.SNOOZE_ENABLED) ?: true,
            minutes = reader.int(AlarmOptionKeys.SNOOZE_MINUTES, AlarmLimits.MIN_SNOOZE_MINUTES, AlarmLimits.MAX_SNOOZE_MINUTES),
            max = reader.int(AlarmOptionKeys.SNOOZE_MAX, 1, AlarmLimits.MAX_SNOOZE_MAX),
        )
    }

    private fun dismiss(values: Map<String, Any?>): AlarmDismiss {
        if (AlarmFeatures.CHALLENGES !in features) throw AlarmException.unsupported("dismiss needs the challenges feature (3-Chime Alarm 1.3.0)")
        val reader = Reader(values, AlarmOptionKeys.DISMISS + ".")
        reader.rejectUnknown(DISMISS_KEYS)
        val challenge = reader.string(AlarmOptionKeys.CHALLENGE)?.also { challenge ->
            if (challenge !in AlarmContract.CHALLENGES) throw invalid("dismiss.challenge must be one of ${AlarmContract.CHALLENGES.joinToString(" / ")}, got \"${clip(challenge)}\"")
        }
        val fallback = reader.string(AlarmOptionKeys.SCRIPT_FALLBACK)?.also { fallback ->
            if (fallback !in AlarmContract.CHALLENGE_FALLBACKS) {
                throw invalid("dismiss.scriptFallback must be one of ${AlarmContract.CHALLENGE_FALLBACKS.joinToString(" / ")}, got \"${clip(fallback)}\"")
            }
        }
        return AlarmDismiss(
            challenge = challenge,
            mathDifficulty = reader.int(AlarmOptionKeys.MATH_DIFFICULTY, AlarmLimits.MIN_MATH_DIFFICULTY, AlarmLimits.MAX_MATH_DIFFICULTY),
            mathCount = reader.int(AlarmOptionKeys.MATH_COUNT, 1, AlarmLimits.MAX_MATH_COUNT),
            shakeCount = reader.int(AlarmOptionKeys.SHAKE_COUNT, 1, AlarmLimits.MAX_SHAKE_COUNT),
            typeText = reader.text(AlarmOptionKeys.TYPE_TEXT, AlarmLimits.MAX_TYPE_TEXT_CHARS),
            scriptFallback = fallback,
            allowSnooze = reader.boolean(AlarmOptionKeys.ALLOW_SNOOZE),
        )
    }

    // ------------------------------------------------------------------ reader

    /** Typed access to one level of the option tree; [prefix] names the group in messages. zh-CN: 选项树一层的类型化读取, [prefix] 用于消息中的组名. */
    private class Reader(val values: Map<String, Any?>, private val prefix: String) {

        fun rejectUnknown(keys: Set<String>) {
            val unknown = values.keys.firstOrNull { it !in keys } ?: return
            throw invalid("unknown option '$prefix${clip(unknown)}'")
        }

        fun string(key: String): String? {
            val raw = values[key] ?: return null
            return raw as? String ?: throw typeError(prefix + key, "a string", raw)
        }

        /** A short non-empty identifier (zone, engine, language, region). zh-CN: 短的非空标识 (时区, 引擎, 语言, 地区). */
        fun name(key: String): String? = string(key)?.also {
            if (it.isBlank() || it.length > MAX_NAME_CHARS) throw invalid("$prefix$key must be a non-empty name of at most $MAX_NAME_CHARS characters")
        }

        /** Text limited by code points (the script counts characters). zh-CN: 按码点计长度的文本 (脚本按字符计数). */
        fun text(key: String, maxChars: Int): String? = string(key)?.also {
            val chars = it.codePointCount(0, it.length)
            if (chars > maxChars) throw limit("$prefix$key has $chars characters, at most $maxChars")
        }

        fun boolean(key: String): Boolean? {
            val raw = values[key] ?: return null
            return raw as? Boolean ?: throw typeError(prefix + key, "a boolean", raw)
        }

        fun int(key: String, min: Int, max: Int): Int? {
            val raw = values[key] ?: return null
            val value = AlarmValues.int(values, key) ?: throw typeError(prefix + key, "an integer", raw)
            if (value < min || value > max) throw invalid("$prefix$key must be in $min - $max, got $value")
            return value
        }

        fun long(key: String, min: Long, max: Long): Long? {
            val raw = values[key] ?: return null
            val value = AlarmValues.long(values, key) ?: throw typeError(prefix + key, "an integer", raw)
            if (value < min || value > max) throw invalid("$prefix$key must be in $min - $max, got $value")
            return value
        }

        fun double(key: String): Double? {
            val raw = values[key] ?: return null
            if (raw !is Number) throw typeError(prefix + key, "a number", raw)
            return AlarmValues.double(values, key) ?: throw typeError(prefix + key, "a number", raw)
        }

        fun positive(key: String): Double? = double(key)?.also {
            if (it.isNaN() || it.isInfinite() || it <= 0.0) throw invalid("$prefix$key must be a positive number, got $it")
        }

        fun date(key: String): String? = string(key)?.also {
            if (!isDate(it)) throw invalid("$prefix$key must be a valid date YYYY-MM-DD, got \"${clip(it)}\"")
        }

        fun stringList(key: String): List<String>? {
            val raw = values[key] ?: return null
            return AlarmValues.stringList(values, key) ?: throw typeError(prefix + key, "an array of strings", raw)
        }

        fun group(key: String): Map<String, Any?>? {
            val raw = values[key] ?: return null
            return AlarmValues.map(values, key) ?: throw typeError(prefix + key, "an object", raw)
        }
    }

    companion object {

        /** Engine, language, region and zone names; longer values are never valid ids. zh-CN: 引擎, 语言, 地区与时区名; 更长的值不会是合法 id. */
        const val MAX_NAME_CHARS = 128

        /** A ringtone uri (`content:`, `android.resource:` or the plugin's own imported uri). zh-CN: 铃声 uri. */
        const val MAX_URI_CHARS = 2048

        private val TIME_KEYS = listOf(AlarmOptionKeys.HOUR, AlarmOptionKeys.MINUTE, AlarmOptionKeys.SECOND)

        /** Keys a patch may not clear with null (protocol "Patch semantics"). zh-CN: patch 中不能用 null 清除的键. */
        val NOT_CLEARABLE_KEYS: Set<String> = linkedSetOf(
            AlarmOptionKeys.HOUR, AlarmOptionKeys.MINUTE, AlarmOptionKeys.SECOND, AlarmOptionKeys.DATE, AlarmOptionKeys.AT,
            AlarmOptionKeys.ENABLED, AlarmOptionKeys.VIBRATE,
        )

        private val HOLIDAY_KEYS: Set<String> = linkedSetOf(AlarmOptionKeys.HOLIDAY_REGION, AlarmOptionKeys.HOLIDAY_MODE)

        private val SOUND_KEYS: Set<String> = linkedSetOf(AlarmOptionKeys.SOUND_SOURCE, AlarmOptionKeys.SOUND_VOLUME, AlarmOptionKeys.SOUND_RAMP_SECONDS)

        private val SPEAK_KEYS: Set<String> = linkedSetOf(
            AlarmOptionKeys.SPEAK_TEXT, AlarmOptionKeys.SPEAK_ENGINE, AlarmOptionKeys.SPEAK_LANGUAGE, AlarmOptionKeys.SPEAK_RATE,
            AlarmOptionKeys.SPEAK_PITCH, AlarmOptionKeys.SPEAK_MODE, AlarmOptionKeys.SPEAK_REPEAT_INTERVAL,
        )

        private val SNOOZE_KEYS: Set<String> = linkedSetOf(AlarmOptionKeys.SNOOZE_ENABLED, AlarmOptionKeys.SNOOZE_MINUTES, AlarmOptionKeys.SNOOZE_MAX)

        private val DISMISS_KEYS: Set<String> = linkedSetOf(
            AlarmOptionKeys.CHALLENGE, AlarmOptionKeys.MATH_DIFFICULTY, AlarmOptionKeys.MATH_COUNT, AlarmOptionKeys.SHAKE_COUNT,
            AlarmOptionKeys.TYPE_TEXT, AlarmOptionKeys.SCRIPT_FALLBACK, AlarmOptionKeys.ALLOW_SNOOZE,
        )

        private val DATE_PATTERN = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")

        private val URI_SCHEME = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")

        fun isDate(text: String): Boolean = DATE_PATTERN.matches(text) && try {
            LocalDate.parse(text)
            true
        } catch (_: DateTimeException) {
            false
        }

        fun isZone(text: String): Boolean = try {
            ZoneId.of(text)
            true
        } catch (_: DateTimeException) {
            false
        }

        private fun invalid(detail: String) = AlarmException.invalidOptions(detail)

        private fun limit(detail: String) = AlarmException.limitExceeded(detail)

        private fun typeError(name: String, expected: String, value: Any): AlarmException = invalid("$name must be $expected, got ${describe(value)}")

        private fun describe(value: Any): String = when (value) {
            is String -> "a string"
            is Boolean -> "a boolean"
            is Number -> "a number"
            is LongArray, is IntArray -> "an array of numbers"
            is List<*> -> "an array"
            is Map<*, *> -> "an object"
            else -> value.javaClass.simpleName
        }

        /** Echoes at most 64 characters of caller text in a message. zh-CN: 消息中至多回显 64 个字符的调用方文本. */
        private fun clip(text: String): String = if (text.length <= 64) text else text.substring(0, 64) + "..."
    }
}
