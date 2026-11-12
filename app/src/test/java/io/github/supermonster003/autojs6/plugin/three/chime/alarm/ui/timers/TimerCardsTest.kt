package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.timers

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerDurations
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerRecord
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.TimerSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P7.4 (T136, T137): the timer cards on plain records. The phase follows the record (running, paused part way,
 * ready at the whole length, ringing, snoozed), the buttons follow the phase (a ringing or snoozed timer only stops), one
 * more minute keeps the time left within 100 h, ringing cards come first; the presets and the custom form stay within
 * 1 s - 100 h.
 * zh-CN: 路线图 P7.4 (T136, T137): 以普通记录检查计时器卡片. 阶段跟随记录 (运行, 中途暂停, 停在完整时长, 响铃, 贪睡), 按钮跟随阶段 (响铃或贪睡中的计时器只能停止),
 * 加一分钟后剩余时间不超过 100 小时, 响铃卡片排在前面; 预设与自定义表单都在 1 秒 - 100 小时之内.
 */
class TimerCardsTest {

    private val now = 1_800_000_000_000L

    private fun record(
        id: String,
        state: String,
        duration: Long = 10 * MINUTE,
        remaining: Long = duration,
        endsAt: Long? = null,
        snoozeUntil: Long? = null,
        createdAt: Long = now - HOUR,
        ringingAt: Long? = null,
        label: String? = null,
    ) = TimerRecord(
        id, AlarmContract.SOURCE_USER, createdAt, TimerSpec(label = label), duration, state, remaining,
        endsAt = endsAt, snoozeUntil = snoozeUntil, ringingAt = ringingAt,
    )

    @Test
    fun phasesAndButtonsFollowTheRecord() {
        val running = TimerCard.of(record("t_run", AlarmContract.TIMER_RUNNING, endsAt = now + 4 * MINUTE, label = "Tea"), now)
        assertEquals(TimerPhase.RUNNING, running.phase)
        assertEquals(4 * MINUTE, running.remainingMillis)
        assertEquals(now + 4 * MINUTE, running.endsAt)
        assertEquals("4:00", running.clockText)
        assertEquals(0.4f, running.progress, 0.0001f)
        assertEquals(listOf(TimerAction.RESET, TimerAction.ADD_MINUTE, TimerAction.PAUSE), running.actions)
        assertEquals("Tea", running.label)
        assertTrue(running.deletable)

        val paused = TimerCard.of(record("t_half", AlarmContract.TIMER_PAUSED, remaining = 3 * MINUTE), now)
        assertEquals(TimerPhase.PAUSED, paused.phase)
        assertNull(paused.endsAt)
        assertEquals(listOf(TimerAction.RESET, TimerAction.ADD_MINUTE, TimerAction.RESUME), paused.actions)

        val ready = TimerCard.of(record("t_ready", AlarmContract.TIMER_PAUSED, label = "  "), now)
        assertEquals(TimerPhase.READY, ready.phase)
        assertEquals(1f, ready.progress, 0f)
        assertEquals(listOf(TimerAction.ADD_MINUTE, TimerAction.START), ready.actions)
        assertNull("a blank label shows the default one", ready.label)

        val ringing = TimerCard.of(record("t_ring", AlarmContract.TIMER_RINGING, remaining = 0L, ringingAt = now - 5_000L), now)
        assertEquals(TimerPhase.RINGING, ringing.phase)
        assertEquals(0L, ringing.remainingMillis)
        assertEquals(0f, ringing.progress, 0f)
        assertEquals(listOf(TimerAction.STOP), ringing.actions)
        assertFalse(ringing.deletable)
        assertFalse(ringing.canAddMinute)

        val snoozed = TimerCard.of(record("t_snooze", AlarmContract.TIMER_RINGING, remaining = 0L, snoozeUntil = now + MINUTE), now)
        assertEquals(TimerPhase.SNOOZED, snoozed.phase)
        assertEquals(now + MINUTE, snoozed.snoozeUntil)
        assertEquals(listOf(TimerAction.STOP), snoozed.actions)

        val due = TimerCard.of(record("t_due", AlarmContract.TIMER_RINGING, remaining = 0L, snoozeUntil = now - 1L), now)
        assertEquals("a snooze that ran out rings", TimerPhase.RINGING, due.phase)
        assertNull(due.snoozeUntil)
    }

    @Test
    fun oneMoreMinuteStaysWithinOneHundredHours() {
        val max = TimerDurations.MAX_MILLIS
        assertTrue(TimerCard.of(record("t_a", AlarmContract.TIMER_PAUSED, duration = max, remaining = max - MINUTE), now).canAddMinute)
        assertFalse(TimerCard.of(record("t_b", AlarmContract.TIMER_PAUSED, duration = max, remaining = max - 30_000L), now).canAddMinute)
        assertFalse(TimerCard.of(record("t_c", AlarmContract.TIMER_RUNNING, duration = max, endsAt = now + max), now).canAddMinute)
    }

    @Test
    fun ringingCardsComeFirstThenTheNewest() {
        val cards = TimerCard.cardsOf(
            listOf(
                record("t_old", AlarmContract.TIMER_PAUSED, createdAt = now - 3 * HOUR),
                record("t_ring_late", AlarmContract.TIMER_RINGING, remaining = 0L, ringingAt = now - 1_000L),
                record("t_new", AlarmContract.TIMER_RUNNING, endsAt = now + MINUTE, createdAt = now - MINUTE),
                record("t_ring_early", AlarmContract.TIMER_RINGING, remaining = 0L, ringingAt = now - 60_000L, snoozeUntil = now + MINUTE),
            ),
            now,
        )
        assertEquals(listOf("t_ring_early", "t_ring_late", "t_new", "t_old"), cards.map { it.id })
    }

    @Test
    fun presetsAndTheCustomFormStayInRange() {
        assertEquals(listOf(1L, 3L, 5L, 10L, 15L, 30L, 60L).map { it * MINUTE }, TimerPresets.MILLIS)
        assertTrue(TimerPresets.MILLIS.all { it in TimerDurations.MIN_MILLIS..TimerDurations.MAX_MILLIS })

        assertEquals("123", TimerInput.digits("1a2 b3c4"))
        assertEquals("", TimerInput.digits("-:."))
        assertNull("a zero total is no timer", TimerInput.millisOf("", "", ""))
        assertNull(TimerInput.millisOf("0", "0", "0"))
        assertEquals(1_000L, TimerInput.millisOf("", "", "1"))
        assertEquals(59_000L, TimerInput.millisOf("", "", "59"))
        assertEquals(90 * MINUTE + 15_000L, TimerInput.millisOf("1", "30", "15"))
        assertEquals(TimerDurations.MAX_MILLIS, TimerInput.millisOf("100", "", ""))
        assertNull("over 100 h", TimerInput.millisOf("100", "0", "1"))
        assertNull(TimerInput.millisOf("101", "", ""))
        assertNull(TimerInput.millisOf("", "60", ""))
        assertNull(TimerInput.millisOf("", "", "60"))
        assertNull(TimerInput.millisOf("0001", "", ""))
        assertNull(TimerInput.millisOf("1x", "", ""))
    }

    private companion object {
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
    }
}
