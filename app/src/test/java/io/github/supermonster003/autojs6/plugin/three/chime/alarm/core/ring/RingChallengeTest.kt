package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmOptionsValidator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.optionsOf
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.AlarmEventBus
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner.Cause
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedule
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedulePort
import org.autojs.plugin.alarm.api.AlarmContract.BY_CHALLENGE
import org.autojs.plugin.alarm.api.AlarmContract.BY_SCRIPT
import org.autojs.plugin.alarm.api.AlarmContract.BY_TOOL
import org.autojs.plugin.alarm.api.AlarmContract.BY_USER
import org.autojs.plugin.alarm.api.AlarmEvent
import org.autojs.plugin.alarm.api.AlarmFeatures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Roadmap P9.1 - P9.3 (T283 - T285): the dismiss challenge in the ring state machine: the user cannot dismiss a challenge
 * alarm directly, finishing the challenge dismisses it `by: 'challenge'`, the emergency dismiss and scripts or tools
 * always can, `allowSnooze: false` refuses the user's snoozes only, the auto silence defaults to 30 minutes, `script`
 * falls back for one round, and `ring` / `dismiss` / `ringing()` carry the challenge in effect.
 * zh-CN: 路线图 P9.1 - P9.3 (T283 - T285): 响铃状态机中的关闭挑战: 用户不能直接关闭挑战闹钟, 完成挑战以 `by: 'challenge'` 关闭, 紧急关闭与脚本或工具总能关闭,
 * `allowSnooze: false` 只拒绝用户的贪睡, 自动停止默认 30 分钟, `script` 的退回只持续一轮, `ring` / `dismiss` / `ringing()` 带生效的挑战.
 */
class RingChallengeTest {

    @get:Rule
    val temp = TemporaryFolder()

    private var now = at("2026-11-14T08:00")

    private val events = ArrayList<AlarmEvent>()

    private lateinit var repository: AlarmRepository
    private lateinit var schedule: AlarmSchedule
    private lateinit var rings: RingCoordinator

    @Before
    fun setUp() {
        lateinit var created: AlarmSchedule
        repository = AlarmRepository(
            store = AlarmStore(File(temp.root, "alarms"), clock = { now }),
            validator = AlarmOptionsValidator(features = setOf(AlarmFeatures.ALARMS, AlarmFeatures.CHALLENGES)),
            clock = { now },
            deviceZone = { SHANGHAI },
            planner = { created.plan(it) },
        )
        val bus = AlarmEventBus().also { it.addListener(events::add) }
        created = AlarmSchedule(repository, Port(), bus, clock = { now }, deviceZone = { SHANGHAI })
        schedule = created
        rings = RingCoordinator(repository, schedule, bus, clock = { now })
    }

    private fun daily(hour: Int, dismiss: Map<String, Any?>?, vararg extra: Pair<String, Any?>): AlarmRecord =
        repository.add(optionsOf(hour, 0, "repeat" to "FREQ=DAILY", *extra).let { if (dismiss != null) it + ("dismiss" to dismiss) else it }, "script")

    private fun fire(vararg alarms: AlarmRecord) = schedule.reconcile(Cause.TRIGGER, alarms.map { it.id })

    private fun take(): List<AlarmEvent> = events.toList().also { events.clear() }

    private fun ringAt(hour: Int, dismiss: Map<String, Any?>?): AlarmRecord {
        val alarm = daily(hour, dismiss)
        now = at("2026-11-15T%02d:00".format(hour))
        fire(alarm)
        return alarm
    }

    @Test
    fun `a challenge alarm rings with its challenge and a 30 minute auto silence`() {
        val alarm = ringAt(7, mapOf("challenge" to "math"))
        val ring = take().single()
        assertEquals("ring", ring.type)
        assertEquals("math", ring.challenge)
        assertEquals("math", rings.sounding?.challenge)
        assertEquals(now + 30 * MINUTE, rings.sounding?.silenceAt)
        assertEquals("math", rings.ringing().single().challenge)
        assertTrue(rings.challenged(alarm.id))
    }

    @Test
    fun `an alarm without a challenge carries none`() {
        val alarm = ringAt(7, null)
        assertNull(take().single().challenge)
        assertNull(rings.ringing().single().challenge)
        assertFalse(rings.challenged(alarm.id))
        assertEquals(now + 10 * MINUTE, rings.sounding?.silenceAt)
        // `none` is no challenge either. zh-CN: `none` 也是无挑战.
        val other = daily(9, mapOf("challenge" to "none"))
        assertNull(RingSettings.of(other.spec).challenge)
        assertTrue(rings.dismiss(alarm.id, BY_USER))
        assertNull(take().single().challenge)
    }

    @Test
    fun `the user cannot dismiss a challenge alarm, finishing the challenge does`() {
        val alarm = ringAt(7, mapOf("challenge" to "shake", "shakeCount" to 5))
        take()
        assertFalse(rings.dismiss(alarm.id, BY_USER))
        assertFalse(rings.dismiss(null, BY_USER))
        assertTrue(take().isEmpty())
        assertFalse(rings.solve("other@1"))
        assertTrue(rings.solve(rings.sounding!!.occurrence))
        val dismiss = take().single()
        assertEquals("dismiss", dismiss.type)
        assertEquals(BY_CHALLENGE, dismiss.by)
        assertEquals("shake", dismiss.challenge)
        assertNull(rings.sounding)
    }

    @Test
    fun `scripts, tools and the emergency dismiss always end a challenge round`() {
        val alarm = ringAt(7, mapOf("challenge" to "type", "typeText" to "awake"))
        take()
        assertTrue(rings.dismiss(alarm.id, BY_SCRIPT))
        assertEquals(BY_SCRIPT to "type", take().single().let { it.by to it.challenge })

        now += DAY
        fire(alarm)
        take()
        assertTrue(rings.dismiss(alarm.id, BY_TOOL))
        assertEquals(BY_TOOL to "type", take().single().let { it.by to it.challenge })

        now += DAY
        fire(alarm)
        take()
        assertFalse(rings.emergencyDismiss("other@1"))
        assertTrue(rings.emergencyDismiss(rings.sounding!!.occurrence))
        assertEquals(BY_USER to "type", take().single().let { it.by to it.challenge })
    }

    @Test
    fun `allowSnooze false refuses the user's snooze but not a script's`() {
        val alarm = ringAt(7, mapOf("challenge" to "math", "allowSnooze" to false))
        take()
        assertFalse(rings.sounding!!.userCanSnooze)
        assertFalse(rings.snooze(alarm.id, null, BY_USER))
        assertTrue(take().isEmpty())
        assertTrue(rings.snooze(alarm.id, 5, BY_SCRIPT))
        val snooze = take().single()
        assertEquals("snooze", snooze.type)
        assertNull(snooze.challenge)
        // A snoozed challenge alarm: the user cannot dismiss it, its snapshot shows the challenge. zh-CN: 贪睡中的挑战闹钟: 用户不能关闭, 快照带挑战.
        assertTrue(rings.challenged(alarm.id))
        assertEquals("math", rings.ringing().single().challenge)
        assertFalse(rings.dismiss(alarm.id, BY_USER))
        assertTrue(rings.dismiss(alarm.id, BY_SCRIPT))
        assertEquals("math", take().single().challenge)
    }

    @Test
    fun `allowSnooze omitted lets the user snooze a challenge alarm`() {
        val alarm = ringAt(7, mapOf("challenge" to "math"))
        take()
        assertTrue(rings.sounding!!.userCanSnooze)
        assertTrue(rings.snooze(alarm.id, null, BY_USER))
    }

    @Test
    fun `script falls back to its fallback for the rest of the round only`() {
        val alarm = ringAt(7, mapOf("challenge" to "script", "scriptFallback" to "math"))
        assertEquals("script", take().single().challenge)
        // Finishing is for the built in challenges; a script dismisses `script`. zh-CN: 完成挑战只用于内置挑战; `script` 由脚本关闭.
        assertFalse(rings.solve(rings.sounding!!.occurrence))
        assertFalse(rings.fallBack("other@1"))
        assertTrue(rings.fallBack(rings.sounding!!.occurrence))
        assertTrue(take().isEmpty())
        assertEquals("math", rings.sounding?.challenge)
        assertEquals("math", rings.ringing().single().challenge)
        // Only a round still waiting for a script falls back. zh-CN: 只有仍在等脚本的一轮才会退回.
        assertFalse(rings.fallBack(rings.sounding!!.occurrence))
        assertTrue(rings.snooze(alarm.id, 5, BY_SCRIPT))
        take()

        now += 5 * MINUTE
        fire(alarm)
        assertEquals("script", take().single().challenge)
        assertEquals("script", rings.sounding?.challenge)
    }

    @Test
    fun `script falling back to none dismisses like an alarm without a challenge`() {
        val alarm = ringAt(7, mapOf("challenge" to "script"))
        take()
        assertTrue(rings.fallBack(rings.sounding!!.occurrence))
        assertNull(rings.sounding?.challenge)
        assertFalse(rings.challenged(alarm.id))
        assertTrue(rings.dismiss(alarm.id, BY_USER))
        val dismiss = take().single()
        assertEquals(BY_USER, dismiss.by)
        assertNull(dismiss.challenge)
    }

    @Test
    fun `a script dismiss while waiting carries script`() {
        val alarm = ringAt(7, mapOf("challenge" to "script", "scriptFallback" to "type"))
        take()
        assertTrue(rings.dismiss(alarm.id, BY_SCRIPT))
        assertEquals(BY_SCRIPT to "script", take().single().let { it.by to it.challenge })
    }

    @Test
    fun `an explicit auto silence stays with a challenge`() {
        val alarm = daily(7, mapOf("challenge" to "math"), "autoSilenceMinutes" to 5)
        now = at("2026-11-15T07:00")
        fire(alarm)
        assertEquals(now + 5 * MINUTE, rings.sounding?.silenceAt)
        val occurrence = rings.sounding!!.occurrence
        now += 5 * MINUTE
        assertTrue(rings.silence(occurrence))
        val events = take()
        assertEquals("silence", events.last().type)
        assertNull(events.last().challenge)
    }

    private inner class Port : AlarmSchedulePort {
        override fun canScheduleExact() = true

        override fun schedule(record: AlarmRecord) = Unit

        override fun cancel(alarmId: String) = Unit

        override fun ring(record: AlarmRecord, request: AlarmPlanner.RingRequest) = rings.offer(record, request)

        override fun notifyMissed(record: AlarmRecord, event: AlarmEvent) = Unit
    }

    private companion object {
        val SHANGHAI: ZoneId = ZoneId.of("Asia/Shanghai")
        const val MINUTE = 60_000L
        const val DAY = 24 * 60 * MINUTE

        fun at(text: String): Long = LocalDateTime.parse(text).atZone(SHANGHAI).toInstant().toEpochMilli()
    }
}
