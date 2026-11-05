package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm

import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_OPTIONS
import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_RULE
import org.autojs.plugin.alarm.api.AlarmErrorCodes.LIMIT_EXCEEDED
import org.autojs.plugin.alarm.api.AlarmErrorCodes.UNSUPPORTED
import org.autojs.plugin.alarm.api.AlarmFeatures
import org.autojs.plugin.alarm.api.AlarmSnooze
import org.autojs.plugin.alarm.api.AlarmSound
import org.autojs.plugin.alarm.api.AlarmSpeak
import org.autojs.plugin.alarm.api.AlarmSpec
import org.autojs.plugin.alarm.api.AlarmTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Roadmap P2.1: the plugin-side option check (protocol "Alarm options" and "Patch semantics", appendix A section 11).
 * zh-CN: 路线图 P2.1: 插件侧的选项校验 (协议 "Alarm options" 与 "Patch semantics", 附录 A 第 11 节).
 */
class AlarmOptionsValidatorTest {

    private val validator = AlarmOptionsValidator()

    @Test
    fun `complete options answer the typed view`() {
        val spec = validator.options(FULL)
        assertEquals(AlarmTime(6, 45, 15), spec.time)
        assertEquals("2026-11-06", spec.date)
        assertEquals("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", spec.repeat)
        assertEquals(listOf("2026-12-24", "2026-12-25"), spec.exclude)
        assertEquals("Asia/Shanghai", spec.timeZone)
        assertEquals(AlarmSound("content://media/internal/audio/media/12", 0.5, 30), spec.sound)
        assertEquals(listOf(0L, 400L, 200L, 400L), spec.vibratePattern)
        assertEquals(AlarmSpeak("Good morning", "org.example.tts", "zh-CN", 1.0, 1.25, "interleave", 30), spec.speak)
        assertEquals(AlarmSnooze(true, 5, 2), spec.snooze)
        assertEquals(15, spec.autoSilenceMinutes)
        assertEquals("work", spec.tag)
        assertEquals(true, spec.vibrate)
    }

    @Test
    fun `an at alarm and null values counted as absent`() {
        val spec = validator.options(mapOf("at" to 1_800_000_000_000L, "label" to null, "sound" to null))
        assertEquals(AlarmSpec(at = 1_800_000_000_000L), spec)
    }

    @Test
    fun `unknown keys are rejected at the top level and inside groups`() {
        assertAlarmError(INVALID_OPTIONS, "lable") { validator.options(optionsOf(7, 30, "lable" to "x")) }
        assertAlarmError(INVALID_OPTIONS, "sound.loud") { validator.options(optionsOf(7, 30, "sound" to mapOf("loud" to true))) }
        assertAlarmError(INVALID_OPTIONS, "speak.voice") { validator.options(optionsOf(7, 30, "speak" to mapOf("text" to "hi", "voice" to "x"))) }
        assertAlarmError(INVALID_OPTIONS, "snooze.count") { validator.options(optionsOf(7, 30, "snooze" to mapOf("count" to 2))) }
    }

    @Test
    fun `mistyped values name the key`() {
        assertAlarmError(INVALID_OPTIONS, "hour") { validator.options(mapOf("hour" to "7", "minute" to 30)) }
        assertAlarmError(INVALID_OPTIONS, "label") { validator.options(optionsOf(7, 30, "label" to 12)) }
        assertAlarmError(INVALID_OPTIONS, "enabled") { validator.options(optionsOf(7, 30, "enabled" to "yes")) }
        assertAlarmError(INVALID_OPTIONS, "sound") { validator.options(optionsOf(7, 30, "sound" to "default")) }
        assertAlarmError(INVALID_OPTIONS, "sound.volume") { validator.options(optionsOf(7, 30, "sound" to mapOf("volume" to "loud"))) }
        assertAlarmError(INVALID_OPTIONS, "exclude") { validator.options(optionsOf(7, 30, "exclude" to "2026-01-01")) }
        assertAlarmError(INVALID_OPTIONS, "vibratePattern") { validator.options(optionsOf(7, 30, "vibratePattern" to listOf("a"))) }
    }

    @Test
    fun `time and at rules`() {
        assertAlarmError(INVALID_OPTIONS, "hour") { validator.options(optionsOf(24, 0)) }
        assertAlarmError(INVALID_OPTIONS, "minute") { validator.options(optionsOf(7, 60)) }
        assertAlarmError(INVALID_OPTIONS, "second") { validator.options(optionsOf(7, 30, "second" to 60)) }
        assertAlarmError(INVALID_OPTIONS, "together") { validator.options(mapOf("hour" to 7)) }
        assertAlarmError(INVALID_OPTIONS, "second needs") { validator.options(mapOf("second" to 5, "at" to 1L)) }
        assertAlarmError(INVALID_OPTIONS, "needs hour and minute") { validator.options(mapOf("label" to "x")) }
        assertAlarmError(INVALID_OPTIONS, "exclusive") { validator.options(optionsOf(7, 30, "at" to 1L)) }
        assertAlarmError(INVALID_OPTIONS, "cannot repeat") { validator.options(mapOf("at" to 1L, "repeat" to "FREQ=DAILY")) }
        assertAlarmError(INVALID_OPTIONS, "already holds the date") { validator.options(mapOf("at" to 1L, "date" to "2026-01-01")) }
        assertAlarmError(INVALID_OPTIONS, "at") { validator.options(mapOf("at" to -1L)) }
        assertEquals(AlarmTime(7, 30, 0), validator.options(optionsOf(7, 30)).time)
    }

    @Test
    fun `dates, excluded dates and zones`() {
        assertAlarmError(INVALID_OPTIONS, "date") { validator.options(optionsOf(7, 30, "date" to "2026-02-30")) }
        assertAlarmError(INVALID_OPTIONS, "date") { validator.options(optionsOf(7, 30, "date" to "2026-2-3")) }
        assertAlarmError(INVALID_OPTIONS, "exclude[1]") { validator.options(optionsOf(7, 30, "exclude" to listOf("2026-01-01", "tomorrow"))) }
        val sorted = validator.options(optionsOf(7, 30, "exclude" to listOf("2026-03-01", "2026-01-01", "2026-03-01"))).exclude
        assertEquals(listOf("2026-01-01", "2026-03-01"), sorted)
        val days = (0 until 367).map { LocalDate.of(2026, 1, 1).plusDays(it.toLong()).toString() }
        assertAlarmError(LIMIT_EXCEEDED, "exclude") { validator.options(optionsOf(7, 30, "exclude" to days)) }
        assertEquals(366, validator.options(optionsOf(7, 30, "exclude" to days.take(366) + days.take(10))).exclude!!.size)
        assertAlarmError(INVALID_OPTIONS, "timeZone") { validator.options(optionsOf(7, 30, "timeZone" to "Mars/Olympus")) }
        assertEquals("Europe/Berlin", validator.options(optionsOf(7, 30, "timeZone" to "Europe/Berlin")).timeZone)
    }

    @Test
    fun `text limits count code points`() {
        assertEquals(astral(100), validator.options(optionsOf(7, 30, "label" to astral(100))).label)
        assertAlarmError(LIMIT_EXCEEDED, "label") { validator.options(optionsOf(7, 30, "label" to "x".repeat(101))) }
        assertAlarmError(LIMIT_EXCEEDED, "tag") { validator.options(optionsOf(7, 30, "tag" to astral(101))) }
        assertAlarmError(LIMIT_EXCEEDED, "speak.text") { validator.options(optionsOf(7, 30, "speak" to mapOf("text" to "y".repeat(501)))) }
        assertEquals(500, validator.options(optionsOf(7, 30, "speak" to mapOf("text" to "y".repeat(500)))).speak!!.text.length)
    }

    @Test
    fun `sound, vibration, speech, snooze and auto silence ranges`() {
        assertAlarmError(INVALID_OPTIONS, "sound.source") { validator.options(optionsOf(7, 30, "sound" to mapOf("source" to "bell.mp3"))) }
        assertEquals("silent", validator.options(optionsOf(7, 30, "sound" to mapOf("source" to "silent"))).sound!!.source)
        assertAlarmError(INVALID_OPTIONS, "sound.volume") { validator.options(optionsOf(7, 30, "sound" to mapOf("volume" to 1.5))) }
        assertAlarmError(INVALID_OPTIONS, "sound.volume") { validator.options(optionsOf(7, 30, "sound" to mapOf("volume" to Double.NaN))) }
        assertEquals(1.0, validator.options(optionsOf(7, 30, "sound" to mapOf("volume" to 1L))).sound!!.volume!!, 0.0)
        assertAlarmError(INVALID_OPTIONS, "sound.rampSeconds") { validator.options(optionsOf(7, 30, "sound" to mapOf("rampSeconds" to 301))) }

        assertAlarmError(LIMIT_EXCEEDED, "vibratePattern") { validator.options(optionsOf(7, 30, "vibratePattern" to LongArray(65) { 100L })) }
        assertAlarmError(INVALID_OPTIONS, "vibratePattern") { validator.options(optionsOf(7, 30, "vibratePattern" to LongArray(0))) }
        assertAlarmError(INVALID_OPTIONS, "vibratePattern[1]") { validator.options(optionsOf(7, 30, "vibratePattern" to longArrayOf(0, -5))) }
        assertAlarmError(INVALID_OPTIONS, "non-zero") { validator.options(optionsOf(7, 30, "vibratePattern" to longArrayOf(0, 0))) }
        assertAlarmError(INVALID_OPTIONS, "implies vibrate") {
            validator.options(optionsOf(7, 30, "vibrate" to false, "vibratePattern" to longArrayOf(0, 100)))
        }

        assertAlarmError(INVALID_OPTIONS, "speak.text") { validator.options(optionsOf(7, 30, "speak" to mapOf("mode" to "before"))) }
        assertAlarmError(INVALID_OPTIONS, "speak.text") { validator.options(optionsOf(7, 30, "speak" to mapOf("text" to "   "))) }
        assertAlarmError(INVALID_OPTIONS, "speak.mode") { validator.options(optionsOf(7, 30, "speak" to mapOf("text" to "hi", "mode" to "loud"))) }
        assertAlarmError(INVALID_OPTIONS, "speak.rate") { validator.options(optionsOf(7, 30, "speak" to mapOf("text" to "hi", "rate" to 0.0))) }
        assertAlarmError(INVALID_OPTIONS, "speak.engine") { validator.options(optionsOf(7, 30, "speak" to mapOf("text" to "hi", "engine" to ""))) }
        assertAlarmError(INVALID_OPTIONS, "speak.repeatInterval") {
            validator.options(optionsOf(7, 30, "speak" to mapOf("text" to "hi", "repeatInterval" to 3601)))
        }

        assertAlarmError(INVALID_OPTIONS, "snooze.minutes") { validator.options(optionsOf(7, 30, "snooze" to mapOf("minutes" to 0))) }
        assertAlarmError(INVALID_OPTIONS, "snooze.minutes") { validator.options(optionsOf(7, 30, "snooze" to mapOf("minutes" to 61))) }
        assertAlarmError(INVALID_OPTIONS, "snooze.max") { validator.options(optionsOf(7, 30, "snooze" to mapOf("max" to 11))) }
        assertEquals(AlarmSnooze.DISABLED, validator.options(optionsOf(7, 30, "snooze" to mapOf("enabled" to false))).snooze)
        assertAlarmError(INVALID_OPTIONS, "autoSilenceMinutes") { validator.options(optionsOf(7, 30, "autoSilenceMinutes" to 0)) }
        assertAlarmError(INVALID_OPTIONS, "autoSilenceMinutes") { validator.options(optionsOf(7, 30, "autoSilenceMinutes" to 61)) }
    }

    @Test
    fun `options of later features are unsupported in 1_0_0`() {
        assertAlarmError(UNSUPPORTED, "holidays") { validator.options(optionsOf(7, 30, "holidays" to mapOf("mode" to "skip"))) }
        assertAlarmError(UNSUPPORTED, "dismiss") { validator.options(optionsOf(7, 30, "dismiss" to mapOf("challenge" to "math"))) }
        val later = AlarmOptionsValidator(features = setOf(AlarmFeatures.ALARMS, AlarmFeatures.HOLIDAYS, AlarmFeatures.CHALLENGES))
        assertEquals("skip", later.options(optionsOf(7, 30, "holidays" to mapOf("mode" to "skip"))).holidays!!.mode)
        assertAlarmError(INVALID_OPTIONS, "holidays.mode") { later.options(optionsOf(7, 30, "holidays" to mapOf("region" to "CN"))) }
        assertEquals("math", later.options(optionsOf(7, 30, "dismiss" to mapOf("challenge" to "math"))).dismiss!!.challenge)
    }

    @Test
    fun `the rule normalizer runs and its error passes through`() {
        // The default normalizer is the one of the rule engine (roadmap P2.2).
        // zh-CN: 默认的规范化函数来自规则引擎 (路线图 P2.2).
        assertEquals("FREQ=DAILY", validator.options(optionsOf(7, 30, "repeat" to "RRULE:freq=daily;interval=1")).repeat)
        assertAlarmError(INVALID_RULE, "HOURLY") { validator.options(optionsOf(7, 30, "repeat" to "FREQ=HOURLY")) }
        assertAlarmError(INVALID_RULE, "repeat") { validator.options(optionsOf(7, 30, "repeat" to " ")) }
        val custom = AlarmOptionsValidator(normalizeRule = { throw AlarmException.invalidRule("custom") })
        assertAlarmError(INVALID_RULE, "custom") { custom.options(optionsOf(7, 30, "repeat" to "FREQ=DAILY")) }
    }

    @Test
    fun `patch semantics`() {
        val stored = validator.options(FULL)
        // An absent key keeps, null clears, a group replaces.
        val cleared = validator.patch(stored, mapOf("label" to null, "sound" to mapOf("source" to "silent")))
        assertNull(cleared.label)
        assertEquals(AlarmSound("silent"), cleared.sound)
        assertEquals(stored.speak, cleared.speak)
        // at drops the time and the date; a repeating alarm has to clear repeat in the same patch.
        assertAlarmError(INVALID_OPTIONS, "clear repeat") { validator.patch(stored, mapOf("at" to 1_800_000_000_000L)) }
        val oneShot = validator.patch(stored, mapOf("at" to 1_800_000_000_000L, "repeat" to null))
        assertEquals(1_800_000_000_000L, oneShot.at)
        assertNull(oneShot.time)
        assertNull(oneShot.date)
        assertNull(oneShot.repeat)
        // hour and minute replace at and the stored second.
        val back = validator.patch(oneShot, mapOf("hour" to 9, "minute" to 0))
        assertEquals(AlarmTime(9, 0, 0), back.time)
        assertNull(back.at)
        assertAlarmError(INVALID_OPTIONS, "both hour and minute") { validator.patch(stored, mapOf("hour" to 9)) }
        // vibrate false drops a stored pattern.
        val quiet = validator.patch(stored, mapOf("vibrate" to false))
        assertEquals(false, quiet.vibrate)
        assertNull(quiet.vibratePattern)
        // Not clearable, empty and unknown.
        assertAlarmError(INVALID_OPTIONS, "enabled cannot be null") { validator.patch(stored, mapOf("enabled" to null)) }
        assertAlarmError(INVALID_OPTIONS, "hour cannot be null") { validator.patch(stored, mapOf("hour" to null, "minute" to 0)) }
        assertAlarmError(INVALID_OPTIONS, "nothing to change") { validator.patch(stored, emptyMap()) }
        assertAlarmError(INVALID_OPTIONS, "color") { validator.patch(stored, mapOf("color" to "red")) }
        // The merged tree is checked as a whole.
        assertAlarmError(INVALID_OPTIONS, "already holds the date") { validator.patch(oneShot, mapOf("date" to "2026-12-01")) }
        assertAlarmError(LIMIT_EXCEEDED, "label") { validator.patch(stored, mapOf("label" to "z".repeat(101))) }
    }

    companion object {

        val FULL: Map<String, Any?> = linkedMapOf(
            "hour" to 6,
            "minute" to 45,
            "second" to 15,
            "date" to "2026-11-06",
            "repeat" to "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR",
            "exclude" to listOf("2026-12-25", "2026-12-24"),
            "timeZone" to "Asia/Shanghai",
            "label" to "Wake up",
            "enabled" to true,
            "tag" to "work",
            "sound" to mapOf("source" to "content://media/internal/audio/media/12", "volume" to 0.5, "rampSeconds" to 30),
            "vibrate" to true,
            "vibratePattern" to longArrayOf(0, 400, 200, 400),
            "speak" to mapOf(
                "text" to "Good morning",
                "engine" to "org.example.tts",
                "language" to "zh-CN",
                "rate" to 1.0,
                "pitch" to 1.25,
                "mode" to "interleave",
                "repeatInterval" to 30,
            ),
            "snooze" to mapOf("enabled" to true, "minutes" to 5, "max" to 2),
            "autoSilenceMinutes" to 15,
        )
    }
}
