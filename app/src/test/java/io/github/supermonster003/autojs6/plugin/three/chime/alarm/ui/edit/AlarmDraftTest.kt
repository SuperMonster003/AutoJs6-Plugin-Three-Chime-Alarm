package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.optionsOf
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RecurrenceRule
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RecurrenceRule.Freq
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.RuleForm.End
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.RuleForm.Problem
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.RuleForm.Result
import org.autojs.plugin.alarm.api.AlarmSnooze
import org.autojs.plugin.alarm.api.AlarmSpec
import org.autojs.plugin.alarm.api.AlarmTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The draft model of the edit page (roadmap P3.1, T83), saved through a real repository so that every patch is one the
 * validator accepts.
 * zh-CN: 编辑页的草稿模型 (路线图 P3.1, T83), 经真实仓库保存, 确保每个 patch 都能通过校验.
 */
class AlarmDraftTest {

    @get:Rule
    val temp = TemporaryFolder()

    /** Thursday 2026-11-05 08:00 in Asia/Shanghai. zh-CN: 上海时间 2026-11-05 (周四) 08:00. */
    private val now = Instant.parse("2026-11-05T00:00:00Z").toEpochMilli()

    private val repository by lazy {
        AlarmRepository(
            store = AlarmStore(File(temp.root, "alarms"), clock = { now }),
            clock = { now },
            deviceZone = { SHANGHAI },
            planner = AlarmRecord::state,
        )
    }

    @Test
    fun `a new alarm rings once at the next whole hour and its date follows the time`() {
        val draft = AlarmDraft.forNew(now, SHANGHAI)
        assertEquals(AlarmTime(9, 0), draft.spec.time)
        assertTrue(draft.dateAuto)
        assertEquals(mapOf("hour" to 9, "minute" to 0, "second" to 0), draft.toOptions())
        assertEquals(LocalDate.parse("2026-11-05"), draft.dateOf(now, SHANGHAI))
        assertEquals(listOf(Instant.parse("2026-11-05T01:00:00Z").toEpochMilli()), draft.preview(5, now, SHANGHAI))

        val earlier = draft.withTime(7, 30, SHANGHAI).withLabel("  Wake up  ")
        assertEquals("07:30 has passed today", LocalDate.parse("2026-11-06"), earlier.dateOf(now, SHANGHAI))
        val record = repository.add(earlier.toOptions(), "user")
        assertEquals("2026-11-06", record.spec.date)
        assertTrue(record.state.dateImplicit)
        assertEquals("Wake up", record.spec.label)
        assertNull("a blank label is none", earlier.withLabel("   ").spec.label)

        // The picker has no seconds: they stay only while the hour and minute do.
        // zh-CN: 选择器没有秒: 只有时与分不变时才保留秒.
        val withSeconds = AlarmDraft(AlarmSpec(time = AlarmTime(7, 30, 15)), dateAuto = true)
        assertEquals(15, withSeconds.withTime(7, 30, SHANGHAI).spec.time!!.second)
        assertEquals(0, withSeconds.withTime(7, 31, SHANGHAI).spec.time!!.second)
    }

    @Test
    fun `the draft of a system clock intent keeps its options and falls back to a new alarm`() {
        val draft = AlarmDraft.fromOptions(mapOf("hour" to 6, "minute" to 5, "label" to "Run", "repeat" to "FREQ=WEEKLY;BYDAY=MO"), now, SHANGHAI)
        assertEquals(AlarmTime(6, 5), draft.spec.time)
        assertEquals("Run", draft.spec.label)
        assertEquals("FREQ=WEEKLY;BYDAY=MO", draft.spec.repeat)
        assertTrue(draft.dateAuto)
        assertTrue(draft.reusable)
        assertTrue(draft.withSound("silent").reusable)
        assertFalse(draft.withSnooze(AlarmSnooze.DISABLED).reusable)
        assertFalse(draft.withDate(LocalDate.parse("2026-12-01"), SHANGHAI).reusable)

        val fresh = AlarmDraft.forNew(now, SHANGHAI)
        assertEquals(fresh, AlarmDraft.fromOptions(null, now, SHANGHAI))
        assertEquals(fresh, AlarmDraft.fromOptions(mapOf("hour" to 25, "minute" to 0), now, SHANGHAI))
    }

    @Test
    fun `an unchanged draft changes nothing and an edit patches only what differs`() {
        val sound = mapOf("source" to "silent", "volume" to 0.5)
        val record = repository.add(optionsOf(7, 30, "label" to "a", "repeat" to "FREQ=DAILY", "sound" to sound), "script")
        val draft = AlarmDraft.of(record)
        assertTrue(draft.toUpdate(record).isEmpty)
        assertTrue("vibration on is the default", draft.withVibrate(true).toUpdate(record).isEmpty)
        assertTrue(draft.withSound("silent").toUpdate(record).isEmpty)

        val edited = draft.withLabel("b").withSound(null).withVibrate(false).withSnooze(AlarmSnooze(minutes = 5)).withAutoSilence(3)
        val update = edited.toUpdate(record)
        assertEquals(setOf("label", "sound", "vibrate", "snooze", "autoSilenceMinutes"), update.patch.keys)
        assertFalse(update.implicitDate)
        val saved = repository.update(record.id, update.patch, "user", update.implicitDate)
        assertEquals("the volume stays with the default source", edited.spec, saved.spec)
        assertTrue(AlarmDraft.of(saved).toUpdate(saved).isEmpty)

        val cleared = AlarmDraft.of(saved).withSnooze(null).withAutoSilence(null).withLabel("")
        val back = repository.update(saved.id, cleared.toUpdate(saved).patch, "user")
        assertNull(back.spec.snooze)
        assertNull(back.spec.autoSilenceMinutes)
        assertNull(back.spec.label)
    }

    @Test
    fun `a schedule change moves an implicit date along and turns a disabled alarm on`() {
        val record = repository.add(optionsOf(9, 0), "user")
        val off = repository.setEnabled(record.id, false, "user")
        val draft = AlarmDraft.of(off)
        assertEquals("the stored date is kept until the schedule changes", "2026-11-05", draft.spec.date)
        val moved = draft.withTime(7, 0, SHANGHAI)
        assertNull("the date follows the time", moved.spec.date)
        val update = moved.toUpdate(off)
        assertEquals(mapOf("hour" to 7, "minute" to 0, "second" to 0, "enabled" to true), update.patch)
        assertTrue(update.implicitDate)
        val saved = repository.update(off.id, update.patch, "user", update.implicitDate)
        assertEquals("2026-11-06", saved.spec.date)
        assertTrue(saved.enabled)
        assertTrue(saved.state.dateImplicit)

        // Other changes leave a disabled alarm off. zh-CN: 其他修改不会启用已禁用的闹钟.
        val again = repository.setEnabled(record.id, false, "user")
        assertEquals(mapOf<String, Any?>("label" to "x"), AlarmDraft.of(again).withLabel("x").toUpdate(again).patch)
    }

    @Test
    fun `a chosen date is pinned and Automatic makes it implicit again`() {
        val record = repository.add(optionsOf(9, 0), "user")
        val pinned = AlarmDraft.of(record).withDate(LocalDate.parse("2026-11-05"), SHANGHAI)
        val update = pinned.toUpdate(record)
        assertEquals("the same date, now chosen", mapOf<String, Any?>("date" to "2026-11-05"), update.patch)
        val saved = repository.update(record.id, update.patch, "user", update.implicitDate)
        assertFalse(saved.state.dateImplicit)

        val draft = AlarmDraft.of(saved)
        assertFalse(draft.dateAuto)
        assertEquals("a chosen date stays when the time changes", "2026-11-05", draft.withTime(7, 0, SHANGHAI).spec.date)
        val back = draft.withDate(null, SHANGHAI).toUpdate(saved)
        assertTrue(back.patch.isEmpty())
        assertTrue(back.implicitDate)
        assertFalse(back.isEmpty)
        val implicit = repository.update(saved.id, back.patch, "user", back.implicitDate)
        assertTrue(implicit.state.dateImplicit)
        assertEquals("2026-11-05", implicit.spec.date)
    }

    @Test
    fun `an at alarm becomes a time alarm on its first schedule change`() {
        val at = Instant.parse("2026-11-10T01:02:03Z").toEpochMilli()
        val record = repository.add(mapOf("at" to at, "label" to "x"), "script")
        val draft = AlarmDraft.of(record)
        assertEquals(AlarmTime(9, 2, 3), draft.timeOf(SHANGHAI))
        assertEquals(LocalDate.parse("2026-11-10"), draft.dateOf(now, SHANGHAI))
        assertEquals(listOf(at), draft.preview(5, now, SHANGHAI))
        assertEquals(mapOf<String, Any?>("label" to "y"), draft.withLabel("y").toUpdate(record).patch)

        val daily = draft.withRepeat("FREQ=DAILY", SHANGHAI)
        assertNull(daily.spec.at)
        assertEquals(AlarmTime(9, 2, 3), daily.spec.time)
        assertEquals("2026-11-10", daily.spec.date)
        assertFalse(daily.dateAuto)
        val update = daily.toUpdate(record)
        assertEquals(
            mapOf("hour" to 9, "minute" to 2, "second" to 3, "date" to "2026-11-10", "repeat" to "FREQ=DAILY"),
            update.patch,
        )
        val saved = repository.update(record.id, update.patch, "user", update.implicitDate)
        assertNull(saved.spec.at)
        assertEquals("FREQ=DAILY", saved.spec.repeat)
        assertEquals(AlarmTime(9, 2, 3), saved.spec.time)
        assertEquals("2026-11-10", saved.spec.date)
    }

    @Test
    fun `a fixed time zone keeps the wall clock time`() {
        val draft = AlarmDraft.forNew(now, SHANGHAI).withTime(21, 0, SHANGHAI).withTimeZone("America/New_York", SHANGHAI)
        assertEquals(AlarmTime(21, 0), draft.spec.time)
        // 19:00 on 2026-11-04 in New York, so 21:00 there is still ahead. zh-CN: 纽约此时为 2026-11-04 19:00, 当地 21:00 尚未到达.
        assertEquals(listOf(Instant.parse("2026-11-05T02:00:00Z").toEpochMilli()), draft.preview(1, now, SHANGHAI))
        assertEquals(LocalDate.parse("2026-11-04"), draft.dateOf(now, SHANGHAI))
        assertNull(draft.withTimeZone(null, SHANGHAI).spec.timeZone)
    }

    @Test
    fun `excluded dates are sorted and unique and skip their rings`() {
        val draft = AlarmDraft.forNew(now, SHANGHAI).withRepeat("FREQ=DAILY", SHANGHAI)
            .withExclude(listOf(LocalDate.parse("2026-11-07"), LocalDate.parse("2026-11-06"), LocalDate.parse("2026-11-07")))
        assertEquals(listOf("2026-11-06", "2026-11-07"), draft.spec.exclude)
        val days = draft.preview(3, now, SHANGHAI).map { Instant.ofEpochMilli(it).atZone(SHANGHAI).toLocalDate().toString() }
        assertEquals(listOf("2026-11-05", "2026-11-08", "2026-11-09"), days)
        assertNull(draft.withExclude(emptyList()).spec.exclude)
        assertEquals("a rule that never rings has no preview", emptyList<Long>(), draft.preview(3, now, SHANGHAI, "FREQ=DAILY;UNTIL=20261101"))
    }

    @Test
    fun `repeat presets`() {
        assertEquals(RepeatPreset.ONCE, RepeatPreset.of(null))
        assertEquals(RepeatPreset.DAILY, RepeatPreset.of("FREQ=DAILY"))
        assertEquals(RepeatPreset.WEEKDAYS, RepeatPreset.of("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"))
        assertEquals(RepeatPreset.WEEKEND, RepeatPreset.of("FREQ=WEEKLY;BYDAY=SA,SU"))
        assertEquals(RepeatPreset.CUSTOM, RepeatPreset.of("FREQ=WEEKLY;BYDAY=MO,WE"))
        assertEquals(RepeatPreset.CUSTOM, RepeatPreset.of("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR,SA,SU"))
        for (rule in listOf(
            "FREQ=WEEKLY", "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO", "FREQ=WEEKLY;COUNT=3;BYDAY=MO", "FREQ=WEEKLY;BYDAY=MO;WKST=SU",
            "FREQ=DAILY;BYDAY=MO", "FREQ=DAILY;INTERVAL=2", "FREQ=MONTHLY", "FREQ=YEARLY", "not a rule",
        )) assertEquals(rule, RepeatPreset.ADVANCED, RepeatPreset.of(rule))

        assertEquals(setOf(MONDAY, WEDNESDAY), RepeatPreset.daysOf("FREQ=WEEKLY;BYDAY=MO,WE"))
        assertEquals(7, RepeatPreset.daysOf("FREQ=DAILY").size)
        assertEquals(emptySet<Any>(), RepeatPreset.daysOf(null))
        assertEquals("FREQ=DAILY", RepeatPreset.ruleOf(RepeatPreset.daysOf("FREQ=DAILY")))
        assertEquals("FREQ=WEEKLY;BYDAY=MO,FR", RepeatPreset.ruleOf(setOf(FRIDAY, MONDAY)))
        assertEquals("FREQ=WEEKLY;BYDAY=SA,SU", RepeatPreset.ruleOf(setOf(SUNDAY, SATURDAY)))
        for (preset in listOf(RepeatPreset.ONCE, RepeatPreset.DAILY, RepeatPreset.WEEKDAYS, RepeatPreset.WEEKEND)) {
            assertEquals(preset, RepeatPreset.of(RepeatPreset.ruleOf(preset)))
        }
    }

    @Test
    fun `the rule form builds canonical rules and keeps the parts it has no field for`() {
        assertEquals(Result.Valid("FREQ=DAILY"), RuleForm.of(null).build())
        val weekly = RuleForm.of(null).copy(freq = Freq.WEEKLY, interval = "2").toggleDay(FRIDAY).toggleDay(MONDAY)
        assertEquals(Result.Valid("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,FR"), weekly.build())
        assertEquals(Result.Valid("FREQ=WEEKLY;INTERVAL=2;BYDAY=FR"), weekly.toggleDay(MONDAY).build())

        val monthly = RuleForm.of("FREQ=MONTHLY;BYMONTH=3,1;BYDAY=-1FR")
        assertFalse("an ordinal entry is not a plain weekday", monthly.hasDay(FRIDAY))
        val both = (monthly.toggleDay(FRIDAY).build() as Result.Valid).rule
        assertEquals(listOf(1, 3), RecurrenceRule.parse(both).byMonth)
        assertEquals(setOf("-1FR", "FR"), RecurrenceRule.parse(both).byDay.map { it.toString() }.toSet())

        val counted = RuleForm.of(null).copy(freq = Freq.MONTHLY, monthDays = "15, 1 ,-1", end = End.COUNT, count = "3")
        assertEquals(Result.Valid(RecurrenceRule.fromString("FREQ=MONTHLY;COUNT=3;BYMONTHDAY=15,1,-1")), counted.build())
        val until = RuleForm.of("FREQ=DAILY;UNTIL=20261231")
        assertEquals(End.UNTIL, until.end)
        assertEquals(Result.Valid("FREQ=DAILY;UNTIL=20261231"), until.build())
        assertEquals(Result.Valid("FREQ=DAILY"), until.copy(end = End.NEVER).build())

        for (rule in listOf(
            "FREQ=YEARLY;BYWEEKNO=1;BYDAY=MO;WKST=SU", "FREQ=DAILY;UNTIL=20261231T120000Z", "FREQ=MONTHLY;BYDAY=MO,TU;BYSETPOS=-1",
            "FREQ=YEARLY;BYYEARDAY=100", "FREQ=WEEKLY;COUNT=4;BYDAY=TU",
        )) assertEquals(rule, Result.Valid(RecurrenceRule.fromString(rule)), RuleForm.of(rule).build())

        val base = RuleForm.of(null)
        assertEquals(Problem.INTERVAL, (base.copy(interval = "0").build() as Result.Invalid).problem)
        assertEquals(Problem.INTERVAL, (base.copy(interval = "x").build() as Result.Invalid).problem)
        assertEquals(Problem.MONTH_DAYS, (base.copy(monthDays = "1,a").build() as Result.Invalid).problem)
        assertEquals(Problem.SET_POS, (base.copy(setPos = "?").build() as Result.Invalid).problem)
        assertEquals(Problem.COUNT, (base.copy(end = End.COUNT).build() as Result.Invalid).problem)
        assertEquals(Problem.UNTIL, (base.copy(end = End.UNTIL).build() as Result.Invalid).problem)
        val engine = base.copy(freq = Freq.WEEKLY, monthDays = "1").build() as Result.Invalid
        assertEquals(Problem.RULE, engine.problem)
        assertTrue(engine.detail!!, engine.detail.contains("BYMONTHDAY"))
    }

    companion object {
        private val SHANGHAI: ZoneId = ZoneId.of("Asia/Shanghai")
    }
}
