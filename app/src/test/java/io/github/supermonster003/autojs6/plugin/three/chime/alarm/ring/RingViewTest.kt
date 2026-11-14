package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingCoordinator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingSettings
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import org.autojs.plugin.alarm.api.AlarmDismiss
import org.autojs.plugin.alarm.api.AlarmSpec
import org.autojs.plugin.alarm.api.AlarmTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P3.2 (T86), P9.5 (T285, T286): what the ringing screen shows for a ring: the label, the snooze button and its
 * length, the snoozes so far and left, the speaking mark, the queue and the dismiss challenge in effect.
 * zh-CN: 路线图 P3.2 (T86), P9.5 (T285, T286): 响铃界面为一次响铃显示的内容: 标签, 贪睡按钮及其时长, 已贪睡与剩余次数, 朗读标记, 排队数与生效的关闭挑战.
 */
class RingViewTest {

    private fun ring(label: String? = null, snoozed: Int = 0, settings: RingSettings = RingSettings(), startedAt: Long? = null): RingCoordinator.Ring {
        val record = AlarmRecord("a1", "script", "script", 0L, 0L, AlarmSpec(time = AlarmTime(7, 0, 0), label = label))
        val request = AlarmPlanner.RingRequest("a1", 1_000L, 1_000L, "a1@1000", snoozed)
        return RingCoordinator.Ring(record, request, settings, since = 1_000L, startedAt = startedAt)
    }

    @Test
    fun `a fresh ring offers its snooze length and every snooze`() {
        val view = RingView.of(ring(label = "wake"), queued = 0, speaking = null, snoozeSchedulable = true)
        assertEquals("a1", view.alarmId)
        assertEquals("a1@1000", view.occurrence)
        assertEquals("wake", view.label)
        assertEquals(RingSettings().snoozeMinutes, view.snoozeMinutes)
        assertEquals(0, view.snoozed)
        assertEquals(RingSettings().snoozeMax, view.snoozesLeft)
        assertFalse(view.speaking)
        assertEquals(0, view.queued)
    }

    @Test
    fun `a blank label shows the default one`() {
        assertNull(RingView.of(ring(label = "  "), 0, null, true).label)
        assertNull(RingView.of(ring(label = null), 0, null, true).label)
    }

    @Test
    fun `snoozes so far count down the snoozes left and the last round hides the button`() {
        val settings = RingSettings(snoozeMinutes = 5, snoozeMax = 3)
        val second = RingView.of(ring(snoozed = 2, settings = settings), 0, null, true)
        assertEquals(5, second.snoozeMinutes)
        assertEquals(2, second.snoozed)
        assertEquals(1, second.snoozesLeft)

        val last = RingView.of(ring(snoozed = 3, settings = settings), 0, null, true)
        assertNull(last.snoozeMinutes)
        assertEquals(0, last.snoozesLeft)

        // A lower limit set after the snoozes never shows a negative count. zh-CN: 贪睡后改小的上限不会显示负数.
        assertEquals(0, RingView.of(ring(snoozed = 5, settings = settings), 0, null, true).snoozesLeft)
    }

    @Test
    fun `an alarm without snooze shows neither the button nor the count`() {
        val view = RingView.of(ring(settings = RingSettings(snoozeEnabled = false)), 0, null, true)
        assertNull(view.snoozeMinutes)
        assertNull(view.snoozesLeft)
    }

    @Test
    fun `without the exact alarm permission the button hides but the count stays (T65)`() {
        val view = RingView.of(ring(), 0, null, snoozeSchedulable = false)
        assertNull(view.snoozeMinutes)
        assertEquals(RingSettings().snoozeMax, view.snoozesLeft)
    }

    @Test
    fun `the speaking mark follows only the voice of this round`() {
        assertTrue(RingView.of(ring(), 0, "a1@1000", true).speaking)
        assertFalse(RingView.of(ring(), 0, "a1@2000", true).speaking)
        assertFalse(RingView.of(ring(), 0, null, true).speaking)
    }

    @Test
    fun `the queue counts the alarms waiting behind`() {
        assertEquals(2, RingView.of(ring(), 2, null, true).queued)
        assertEquals(0, RingView.of(ring(), -1, null, true).queued)
    }

    @Test
    fun `a challenge round carries its challenge, options, auto silence and emergency setting (T286)`() {
        val dismiss = AlarmDismiss(challenge = "math", mathDifficulty = 3, mathCount = 5)
        val settings = RingSettings(autoSilenceMinutes = 30, dismiss = dismiss)
        val view = RingView.of(ring(settings = settings, startedAt = 2_000L), 0, null, true, emergency = true)
        assertEquals("math", view.challenge)
        assertEquals(dismiss, view.dismiss)
        assertEquals(2_000L + 30 * 60_000L, view.silenceAt)
        assertTrue(view.emergency)
        assertEquals(RingView.seedOf("a1@1000", 2_000L), view.seed)
        // The same round keeps its problems, the next one gets new ones. zh-CN: 同一轮题目不变, 下一轮换新题.
        assertNotEquals(view.seed, RingView.seedOf("a1@1300000", 2_000L))
        assertNotEquals(view.seed, RingView.seedOf("a1@1000", 9_000L))

        val plain = RingView.of(ring(), 0, null, true)
        assertNull(plain.challenge)
        assertNull(plain.dismiss)
        assertFalse(plain.emergency)
    }

    @Test
    fun `allowSnooze false hides the snooze button of a challenge but keeps the count (T285)`() {
        val settings = RingSettings(dismiss = AlarmDismiss(challenge = "shake", allowSnooze = false))
        val view = RingView.of(ring(settings = settings), 0, null, true)
        assertNull(view.snoozeMinutes)
        assertEquals(RingSettings().snoozeMax, view.snoozesLeft)
        val allowed = RingView.of(ring(settings = RingSettings(dismiss = AlarmDismiss(challenge = "shake"))), 0, null, true)
        assertEquals(RingSettings().snoozeMinutes, allowed.snoozeMinutes)
        // The script fallback to none lifts the challenge and its snooze limit. zh-CN: script 退回 none 后挑战与贪睡限制都解除.
        val fellBack = ring(settings = RingSettings(dismiss = AlarmDismiss(challenge = "script", allowSnooze = false))).copy(challenge = null)
        assertEquals(RingSettings().snoozeMinutes, RingView.of(fellBack, 0, null, true).snoozeMinutes)
    }

    @Test
    fun `the speaking indicator keeps the latest voice and ignores the end of an older one`() {
        RingIndicators.setSpeaking("x@1", true)
        RingIndicators.setSpeaking("x@2", true)
        RingIndicators.setSpeaking("x@1", false)
        assertEquals("x@2", RingIndicators.speaking.value)
        RingIndicators.setSpeaking("x@2", false)
        assertNull(RingIndicators.speaking.value)
    }
}
