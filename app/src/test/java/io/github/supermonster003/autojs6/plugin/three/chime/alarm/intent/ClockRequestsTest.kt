package io.github.supermonster003.autojs6.plugin.three.chime.alarm.intent

import android.provider.AlarmClock
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmOptionsValidator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRuntimeState
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.assertAlarmError
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.astral
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.NextOccurrence
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RecurrenceRule
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.intent.ClockRequests.DismissPlan
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.intent.ClockRequests.Search
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_OPTIONS
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Roadmap P2.6 (D21, T79, T80): the extras of the system clock intents on plain values. `SET_ALARM` maps to valid
 * alarm options (time, label, `Calendar` weekdays, vibration, ringtone, `skipUi`) and finds the alarm it repeats;
 * `DISMISS_ALARM` picks alarms by mode (`next` / `all` / `time` / `label`, none) and dismisses within a day only;
 * `SNOOZE_ALARM` takes 1 - 60 minutes.
 * zh-CN: 路线图 P2.6 (D21, T79, T80): 以普通值检查系统时钟 Intent 的 extras. `SET_ALARM` 映射为有效的闹钟选项 (时间, 标签, `Calendar` 星期, 振动, 铃声,
 * `skipUi`) 并找到它会重复的闹钟; `DISMISS_ALARM` 按模式 (`next` / `all` / `time` / `label`, 无) 挑选闹钟, 只在一天之内关闭; `SNOOZE_ALARM` 接受 1 - 60 分钟.
 */
class ClockRequestsTest {

    private val validator = AlarmOptionsValidator()

    /** 2026-11-05 (Thursday) 10:00 in Shanghai. zh-CN: 上海时间 2026-11-05 (星期四) 10:00. */
    private val now = LocalDateTime.parse("2026-11-05T10:00").atZone(SHANGHAI).toInstant().toEpochMilli()

    /** A stored alarm: a `time` alarm without a date gets the implicit date as the repository fills it in (T48). */
    private fun record(id: String, options: Map<String, Any?>, state: AlarmRuntimeState = AlarmRuntimeState()): AlarmRecord {
        val spec = validator.options(options)
        if (spec.time == null || spec.date != null) return AlarmRecord(id, AlarmContract.SOURCE_INTENT, AlarmContract.SOURCE_INTENT, 1L, 1L, spec, state)
        val filled = spec.copy(date = NextOccurrence.implicitDate(spec, now, SHANGHAI).toString())
        return AlarmRecord(id, AlarmContract.SOURCE_INTENT, AlarmContract.SOURCE_INTENT, 1L, 1L, filled, state.copy(dateImplicit = true))
    }

    @Test
    fun setAlarmMapsTheExtrasToValidOptions() {
        val request = ClockRequests.setAlarm(
            mapOf(
                AlarmClock.EXTRA_HOUR to 6, AlarmClock.EXTRA_MINUTES to 45, AlarmClock.EXTRA_MESSAGE to "  Gym  ",
                AlarmClock.EXTRA_DAYS to arrayListOf(2, 4, 6), AlarmClock.EXTRA_VIBRATE to false,
                AlarmClock.EXTRA_RINGTONE to AlarmClock.VALUE_RINGTONE_SILENT, AlarmClock.EXTRA_SKIP_UI to true,
            ),
        )
        assertTrue(request.skipUi)
        val options = request.options!!
        assertEquals(6, options[AlarmOptionKeys.HOUR])
        assertEquals(45, options[AlarmOptionKeys.MINUTE])
        assertEquals("Gym", options[AlarmOptionKeys.LABEL])
        assertEquals(RecurrenceRule.normalize(listOf(1, 3, 5)), options[AlarmOptionKeys.REPEAT])
        assertEquals(false, options[AlarmOptionKeys.VIBRATE])
        assertEquals(mapOf(AlarmOptionKeys.SOUND_SOURCE to AlarmContract.SOUND_SILENT), options[AlarmOptionKeys.SOUND])
        val spec = validator.options(options)
        assertEquals(6, spec.time!!.hour)
        assertEquals(RecurrenceRule.normalize(listOf(1, 3, 5)), spec.repeat)
        assertEquals(AlarmContract.SOUND_SILENT, spec.sound!!.source)
    }

    @Test
    fun setAlarmDefaultsAndTheNewAlarmPage() {
        val plain = ClockRequests.setAlarm(mapOf(AlarmClock.EXTRA_HOUR to 23))
        assertFalse(plain.skipUi)
        assertEquals(mapOf(AlarmOptionKeys.HOUR to 23, AlarmOptionKeys.MINUTE to 0), plain.options)
        assertNull(ClockRequests.setAlarm(emptyMap()).options)
        assertNull(ClockRequests.setAlarm(mapOf(AlarmClock.EXTRA_HOUR to 24)).options)
        assertNull(ClockRequests.setAlarm(mapOf(AlarmClock.EXTRA_HOUR to -1)).options)
        assertNull(ClockRequests.setAlarm(mapOf(AlarmClock.EXTRA_HOUR to "7")).options)
        assertNull(ClockRequests.setAlarm(mapOf(AlarmClock.EXTRA_HOUR to 7, AlarmClock.EXTRA_MINUTES to 60)).options)
        assertNull(ClockRequests.setAlarm(mapOf(AlarmClock.EXTRA_HOUR to 7, AlarmClock.EXTRA_MINUTES to 5L)).options)
        assertTrue(ClockRequests.setAlarm(mapOf(AlarmClock.EXTRA_SKIP_UI to true)).skipUi)
    }

    @Test
    fun setAlarmCleansLabelDaysAndRingtone() {
        assertNull(ClockRequests.labelOf("   "))
        assertNull(ClockRequests.labelOf(42))
        val long = ClockRequests.labelOf(astral(AlarmLimits.MAX_LABEL_CHARS + 20))!!
        assertEquals(AlarmLimits.MAX_LABEL_CHARS, long.codePointCount(0, long.length))
        assertEquals(RecurrenceRule.normalize(listOf(0, 6)), ClockRequests.repeatOf(intArrayOf(1, 7, 7, 0, 8)))
        assertNull(ClockRequests.repeatOf(arrayListOf(0, 8)))
        assertNull(ClockRequests.repeatOf(arrayListOf<Int>()))
        assertNull(ClockRequests.repeatOf("MO"))
        val uri = ClockRequests.setAlarm(mapOf(AlarmClock.EXTRA_HOUR to 7, AlarmClock.EXTRA_RINGTONE to " content://media/internal/audio/media/12 "))
        assertEquals(mapOf(AlarmOptionKeys.SOUND_SOURCE to "content://media/internal/audio/media/12"), uri.options!![AlarmOptionKeys.SOUND])
        val blank = ClockRequests.setAlarm(mapOf(AlarmClock.EXTRA_HOUR to 7, AlarmClock.EXTRA_RINGTONE to " ", AlarmClock.EXTRA_DAYS to arrayListOf(9)))
        assertEquals(setOf(AlarmOptionKeys.HOUR, AlarmOptionKeys.MINUTE), blank.options!!.keys)
    }

    @Test
    fun sameAlarmFindsTheAlarmARequestRepeats() {
        val options = ClockRequests.setAlarm(mapOf(AlarmClock.EXTRA_HOUR to 7, AlarmClock.EXTRA_MESSAGE to "Work", AlarmClock.EXTRA_DAYS to arrayListOf(2, 3))).options!!
        val stored = record("a_same", options)
        assertSame(stored, ClockRequests.sameAlarm(listOf(stored), options))
        assertSame(stored, ClockRequests.sameAlarm(listOf(stored), options + (AlarmOptionKeys.VIBRATE to true)))
        assertSame(stored, ClockRequests.sameAlarm(listOf(stored), options + (AlarmOptionKeys.SOUND to mapOf(AlarmOptionKeys.SOUND_SOURCE to AlarmContract.SOUND_DEFAULT))))
        assertNull(ClockRequests.sameAlarm(listOf(stored), options + (AlarmOptionKeys.VIBRATE to false)))
        assertNull(ClockRequests.sameAlarm(listOf(stored), options + (AlarmOptionKeys.SOUND to mapOf(AlarmOptionKeys.SOUND_SOURCE to AlarmContract.SOUND_SILENT))))
        assertNull(ClockRequests.sameAlarm(listOf(stored), options + (AlarmOptionKeys.LABEL to "Gym")))
        assertNull(ClockRequests.sameAlarm(listOf(stored), options - AlarmOptionKeys.REPEAT))
        assertNull(ClockRequests.sameAlarm(listOf(stored), options + (AlarmOptionKeys.MINUTE to 1)))
        assertNull(ClockRequests.sameAlarm(listOf(record("a_dated", options + ("date" to "2026-11-10"))), options))
        assertNull(ClockRequests.sameAlarm(listOf(record("a_second", options + ("second" to 30))), options))
        val disabled = record("a_off", options + ("enabled" to false))
        assertSame(disabled, ClockRequests.sameAlarm(listOf(disabled), options))
    }

    @Test
    fun searchReadsTheModes() {
        assertSame(Search.Unspecified, ClockRequests.search(emptyMap()))
        assertSame(Search.Next, ClockRequests.search(mapOf(AlarmClock.EXTRA_ALARM_SEARCH_MODE to AlarmClock.ALARM_SEARCH_MODE_NEXT)))
        assertSame(Search.All, ClockRequests.search(mapOf(AlarmClock.EXTRA_ALARM_SEARCH_MODE to AlarmClock.ALARM_SEARCH_MODE_ALL)))
        val time = mapOf<String, Any?>(AlarmClock.EXTRA_ALARM_SEARCH_MODE to AlarmClock.ALARM_SEARCH_MODE_TIME, AlarmClock.EXTRA_HOUR to 7)
        assertEquals(Search.Time(7, 0), ClockRequests.search(time))
        assertEquals(Search.Time(19, 15), ClockRequests.search(time + mapOf(AlarmClock.EXTRA_MINUTES to 15, AlarmClock.EXTRA_IS_PM to true)))
        assertEquals(Search.Time(13, 0), ClockRequests.search(time + mapOf(AlarmClock.EXTRA_HOUR to 13, AlarmClock.EXTRA_IS_PM to true)))
        assertEquals(Search.Label("Work"), ClockRequests.search(mapOf(AlarmClock.EXTRA_ALARM_SEARCH_MODE to AlarmClock.ALARM_SEARCH_MODE_LABEL, AlarmClock.EXTRA_MESSAGE to " Work ")))
        assertAlarmError(INVALID_OPTIONS, "EXTRA_HOUR") { ClockRequests.search(time - AlarmClock.EXTRA_HOUR) }
        assertAlarmError(INVALID_OPTIONS, "EXTRA_MINUTES") { ClockRequests.search(time + (AlarmClock.EXTRA_MINUTES to 61)) }
        assertAlarmError(INVALID_OPTIONS, "EXTRA_MESSAGE") { ClockRequests.search(mapOf(AlarmClock.EXTRA_ALARM_SEARCH_MODE to AlarmClock.ALARM_SEARCH_MODE_LABEL)) }
        assertAlarmError(INVALID_OPTIONS, "soon") { ClockRequests.search(mapOf(AlarmClock.EXTRA_ALARM_SEARCH_MODE to "soon")) }
    }

    @Test
    fun matchesPicksByMode() {
        val early = record("a_early", mapOf("hour" to 7, "minute" to 30, "repeat" to "FREQ=DAILY"))
        val work = record("a_work", mapOf("hour" to 8, "minute" to 0, "repeat" to "FREQ=DAILY", "label" to "Work"))
        val off = record("a_off", mapOf("hour" to 7, "minute" to 30, "enabled" to false))
        val once = record("a_once", mapOf("at" to now + 3 * HOUR))
        val records = listOf(early, work, off, once)
        fun ids(search: Search, ringing: Set<String> = emptySet()) = ClockRequests.matches(search, records, ringing, now, SHANGHAI).map { it.id }

        assertEquals(listOf("a_once"), ids(Search.Next))
        assertEquals(listOf("a_work"), ids(Search.Next, setOf("a_work")))
        assertEquals(listOf("a_early", "a_work", "a_once"), ids(Search.Unspecified))
        assertEquals(listOf("a_off"), ids(Search.Unspecified, setOf("a_off")))
        assertEquals(listOf("a_early", "a_work", "a_off", "a_once"), ids(Search.All, setOf("a_off")))
        assertEquals(listOf("a_early"), ids(Search.Time(7, 30)))
        assertEquals(listOf("a_once"), ids(Search.Time(13, 0)))
        assertEquals(listOf("a_work"), ids(Search.Label("WORK")))
        assertEquals(emptyList<String>(), ids(Search.Label("Gym")))
        assertEquals(emptyList<String>(), ClockRequests.matches(Search.Next, listOf(off), emptySet(), now, SHANGHAI))
    }

    @Test
    fun dismissPlanEndsSkipsOrWaits() {
        val daily = record("a_daily", mapOf("hour" to 7, "minute" to 30, "repeat" to "FREQ=DAILY"))
        assertSame(DismissPlan.EndRound, ClockRequests.dismissPlan(daily, true, now, SHANGHAI))
        val next = NextOccurrence.upcoming(listOf(daily), 1, now, SHANGHAI).single().at
        assertEquals(DismissPlan.Skip(next), ClockRequests.dismissPlan(daily, false, now, SHANGHAI))
        assertSame(DismissPlan.AlreadySkipped, ClockRequests.dismissPlan(daily.copy(state = daily.state.copy(skipAt = next)), false, now, SHANGHAI))
        val monday = record("a_monday", mapOf("hour" to 7, "minute" to 0, "repeat" to "FREQ=WEEKLY;BYDAY=MO"))
        val plan = ClockRequests.dismissPlan(monday, false, now, SHANGHAI)
        assertTrue("$plan", plan is DismissPlan.TooEarly && plan.at - now > ClockRequests.DISMISS_AHEAD_MILLIS)
        assertSame(DismissPlan.NoRing, ClockRequests.dismissPlan(record("a_gone", mapOf("at" to now + HOUR, "enabled" to false)), false, now, SHANGHAI))
        assertEquals(DismissPlan.Skip(now + HOUR), ClockRequests.dismissPlan(record("a_soon", mapOf("at" to now + HOUR)), false, now, SHANGHAI))
    }

    @Test
    fun snoozeMinutesChecksTheRange() {
        assertNull(ClockRequests.snoozeMinutes(emptyMap()))
        assertEquals(1, ClockRequests.snoozeMinutes(mapOf(AlarmClock.EXTRA_ALARM_SNOOZE_DURATION to 1)))
        assertEquals(60, ClockRequests.snoozeMinutes(mapOf(AlarmClock.EXTRA_ALARM_SNOOZE_DURATION to 60)))
        assertAlarmError(INVALID_OPTIONS, "0") { ClockRequests.snoozeMinutes(mapOf(AlarmClock.EXTRA_ALARM_SNOOZE_DURATION to 0)) }
        assertAlarmError(INVALID_OPTIONS, "61") { ClockRequests.snoozeMinutes(mapOf(AlarmClock.EXTRA_ALARM_SNOOZE_DURATION to 61)) }
        assertAlarmError(INVALID_OPTIONS, "integer") { ClockRequests.snoozeMinutes(mapOf(AlarmClock.EXTRA_ALARM_SNOOZE_DURATION to "5")) }
    }

    private companion object {
        val SHANGHAI: ZoneId = ZoneId.of("Asia/Shanghai")
        const val HOUR = 60 * 60 * 1000L
    }
}
