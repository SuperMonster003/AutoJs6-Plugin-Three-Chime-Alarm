package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring.RingActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring.RingIndicators
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The ringing screen of the installed plugin (roadmap P3.2, T86) on real rings: the label, the snoozes left, the snooze
 * button with its length, the speaking mark, back doing nothing, snooze closing the screen; two alarms due together
 * show "1 more alarm", the slider dismisses the first and shows the second, and a click from an accessibility service
 * on the slider dismisses that one and closes the screen. The dismiss challenges of 1.3.0 (roadmap P9.1, P9.3, P9.5):
 * `math` takes the place of the slider, keeps the problem on a wrong answer, always shows the time left until the auto
 * stop and ends `by: 'challenge'`; `type` accepts its text ignoring case and blanks at both ends; five volume key presses
 * dismiss `by: 'user'` only while emergency dismiss is on, and the volume keys never change the alarm volume.
 *
 * The probe alarms carry the label prefix `p32-probe`, ring silently without vibration, and [cleanUp] ends their rounds
 * and removes them. The screen is started directly: the full-screen intent opens it only while the device is not in
 * use.
 *
 * zh-CN: 已安装插件的响铃界面 (路线图 P3.2, T86), 用真实响铃验证: 标签, 剩余贪睡次数, 带时长的贪睡按钮, 朗读标记, 返回键无效, 贪睡后界面关闭;
 * 两个同时到点的闹钟显示 "还有 1 个闹钟", 滑块关闭第一个后显示第二个, 无障碍服务点按滑块关闭第二个并关闭界面. 1.3.0 的关闭挑战 (路线图 P9.1,
 * P9.3, P9.5): `math` 取代滑块, 答错时题目不变, 始终显示距自动停止的剩余时间, 以 `by: 'challenge'` 结束; `type` 忽略大小写与首尾空白接受其文字;
 * 只有紧急关闭开启时连按 5 次音量键才以 `by: 'user'` 关闭, 音量键从不改变闹钟音量. 探针闹钟带标签前缀 `p32-probe`,
 * 静音且不振动, [cleanUp] 结束其响铃并删除. 界面由测试直接启动: 全屏 Intent 只在设备未被使用时才会打开它.
 */
@RunWith(AndroidJUnit4::class)
class RingActivityTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation: Instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val context: Context
        get() = instrumentation.targetContext

    private val runtime: AlarmRuntime
        get() = AlarmRuntime.get(context)

    private val label = "$LABEL_PREFIX-${UUID.randomUUID().toString().take(8)}"

    private val listeners = mutableListOf<(AlarmEvent) -> Unit>()

    @Before
    fun setUp() {
        assumeTrue("another alarm rings", runtime.rings.ringing().isEmpty())
        // Starting an activity first clears the stopped state that `am instrument` leaves, so the alarm is delivered;
        // with nothing sounding the ring screen closes at once.
        // zh-CN: 先启动一个 Activity, 清除 `am instrument` 留下的停止状态, 闹钟才能送达; 没有闹钟发声时响铃界面会立即关闭.
        openRingScreen()
        waitFor("the empty ring screen closes", WAIT_MILLIS) { resumedRingScreen() == null }
    }

    @After
    fun cleanUp() {
        listeners.forEach { runtime.events.removeListener(it) }
        RingIndicators.speaking.value?.let { RingIndicators.setSpeaking(it, false) }
        val probes = runtime.repository.list().filter { it.spec.label?.startsWith(LABEL_PREFIX) == true }
        probes.forEach { probe ->
            if (runtime.rings.ringing().any { it.id == probe.id }) runtime.rings.dismiss(probe.id, AlarmContract.BY_USER)
            runtime.repository.remove(probe.id, AlarmContract.SOURCE_TOOL)
        }
    }

    @Test
    fun showsTheRingIgnoresBackAndSnoozes() {
        val record = probe("$label-snooze", ringAt())
        waitFor("the alarm rings", RING_WAIT_MILLIS) { runtime.rings.sounding?.alarmId == record.id }
        val screen = showRingScreen()
        val resources = screen.resources

        compose.onNodeWithTag(RingActivity.TAG_LABEL).assert(hasText("$label-snooze"))
        compose.onNodeWithTag(RingActivity.TAG_TIME).assertExists()
        compose.onNodeWithTag(RingActivity.TAG_SNOOZE_INFO).assert(hasText(resources.getQuantityString(R.plurals.ring_snooze_left, 3, 3)))
        compose.onNodeWithTag(RingActivity.TAG_SNOOZE).assert(hasText(resources.getQuantityString(R.plurals.ring_snooze_minutes, 5, 5)))
        assertTrue(compose.onAllNodesWithTag(RingActivity.TAG_QUEUE).fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodesWithTag(RingActivity.TAG_SPEAKING).fetchSemanticsNodes().isEmpty())

        val occurrence = runtime.rings.sounding!!.occurrence
        RingIndicators.setSpeaking(occurrence, true)
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag(RingActivity.TAG_SPEAKING).fetchSemanticsNodes().isNotEmpty() }
        RingIndicators.setSpeaking(occurrence, false)
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag(RingActivity.TAG_SPEAKING).fetchSemanticsNodes().isEmpty() }

        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        SystemClock.sleep(1_000)
        assertNotNull("back closed the ring screen", resumedRingScreen())
        assertEquals(record.id, runtime.rings.sounding?.alarmId)

        compose.onNodeWithTag(RingActivity.TAG_SNOOZE).performClick()
        waitFor("the ring is snoozed", WAIT_MILLIS) { runtime.rings.ringing().any { it.id == record.id && it.snoozed } }
        waitFor("the ring screen closes", WAIT_MILLIS) { resumedRingScreen() == null }
        assertEquals(1, runtime.repository.get(record.id)!!.state.snoozeCount)
    }

    @Test
    fun showsTheQueueAndDismissesBySlideAndByClick() {
        val at = ringAt()
        val first = probe("$label-q1", at)
        val second = probe("$label-q2", at)
        val ids = setOf(first.id, second.id)
        waitFor("both alarms ring", RING_WAIT_MILLIS) { runtime.rings.sounding?.alarmId in ids && runtime.rings.queued.size == 1 }
        val screen = showRingScreen()
        val sounding = runtime.rings.sounding!!.label!!
        val waiting = if (sounding == first.spec.label) second.spec.label!! else first.spec.label!!

        compose.onNodeWithTag(RingActivity.TAG_LABEL).assert(hasText(sounding))
        compose.onNodeWithTag(RingActivity.TAG_QUEUE).assert(hasText(screen.resources.getQuantityString(R.plurals.ring_queue, 1, 1)))

        val rtl = screen.window.decorView.layoutDirection == View.LAYOUT_DIRECTION_RTL
        compose.onNodeWithTag(RingActivity.TAG_DISMISS).performTouchInput { if (rtl) swipeLeft() else swipeRight() }
        waitFor("the second alarm sounds", WAIT_MILLIS) { runtime.rings.sounding?.label == waiting }
        compose.waitUntil(WAIT_MILLIS) {
            compose.onAllNodesWithTag(RingActivity.TAG_LABEL).fetchSemanticsNodes().any { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == waiting } == true
            }
        }
        assertTrue(compose.onAllNodesWithTag(RingActivity.TAG_QUEUE).fetchSemanticsNodes().isEmpty())

        compose.onNodeWithTag(RingActivity.TAG_DISMISS).performSemanticsAction(SemanticsActions.OnClick)
        waitFor("both rounds ended", WAIT_MILLIS) { runtime.rings.ringing().none { it.id in ids } }
        waitFor("the ring screen closes", WAIT_MILLIS) { resumedRingScreen() == null }
    }

    @Test
    fun mathChallengeShowsTheAutoStopAndEndsByChallenge() {
        val events = listen()
        val record = probe("$label-math", ringAt(), mapOf("challenge" to AlarmContract.CHALLENGE_MATH, "mathDifficulty" to 1, "mathCount" to MATH_COUNT))
        waitFor("the alarm rings", RING_WAIT_MILLIS) { runtime.rings.sounding?.alarmId == record.id }
        val screen = showChallenge()
        assertTrue("the challenge takes the place of the slider", compose.onAllNodesWithTag(RingActivity.TAG_DISMISS).fetchSemanticsNodes().isEmpty())
        compose.onNodeWithTag(RingActivity.TAG_SNOOZE).assertExists()
        val minutes = runtime.defaults.get().autoSilenceFor(true).toLong()
        val counting = (0L until 60L).map { screen.getString(R.string.ring_challenge_auto_stop, minutes - 1, it) } +
            screen.getString(R.string.ring_challenge_auto_stop, minutes, 0L)
        val autoStop = text(RingActivity.TAG_CHALLENGE_AUTO_STOP)
        assertTrue("'$autoStop' counts down from $minutes minutes", autoStop in counting)
        val emergency = runtime.defaults.get().emergencyDismiss
        assertEquals(emergency, compose.onAllNodesWithTag(RingActivity.TAG_CHALLENGE_EMERGENCY).fetchSemanticsNodes().isNotEmpty())

        val first = text(RingActivity.TAG_CHALLENGE_PROBLEM)
        answer((solve(first) + 1).toString())
        assertEquals("a wrong answer keeps the problem", first, text(RingActivity.TAG_CHALLENGE_PROBLEM))
        assertEquals(record.id, runtime.rings.sounding?.alarmId)
        repeat(MATH_COUNT) { answer(solve(text(RingActivity.TAG_CHALLENGE_PROBLEM)).toString()) }
        waitFor("the round ends", WAIT_MILLIS) { runtime.rings.ringing().none { it.id == record.id } }
        val dismiss = dismissOf(events, record.id)
        assertEquals(AlarmContract.BY_CHALLENGE, dismiss.by)
        assertEquals(AlarmContract.CHALLENGE_MATH, dismiss.challenge)
        waitFor("the ring screen closes", WAIT_MILLIS) { resumedRingScreen() == null }
    }

    @Test
    fun typeChallengeAcceptsItsTextIgnoringCase() {
        val events = listen()
        val record = probe("$label-type", ringAt(), mapOf("challenge" to AlarmContract.CHALLENGE_TYPE, "typeText" to "Rise And Shine", "allowSnooze" to false))
        waitFor("the alarm rings", RING_WAIT_MILLIS) { runtime.rings.sounding?.alarmId == record.id }
        showChallenge()
        assertTrue("allowSnooze false hides snooze", compose.onAllNodesWithTag(RingActivity.TAG_SNOOZE).fetchSemanticsNodes().isEmpty())
        assertEquals("Rise And Shine", text(RingActivity.TAG_CHALLENGE_PROBLEM))
        answer("  rise and shine ")
        waitFor("the round ends", WAIT_MILLIS) { runtime.rings.ringing().none { it.id == record.id } }
        val dismiss = dismissOf(events, record.id)
        assertEquals(AlarmContract.BY_CHALLENGE, dismiss.by)
        assertEquals(AlarmContract.CHALLENGE_TYPE, dismiss.challenge)
    }

    @Test
    fun volumeKeysDismissOnlyWhileEmergencyDismissIsOn() {
        val events = listen()
        val defaults = runtime.defaults.get()
        val audio = context.getSystemService(AudioManager::class.java)
        val userVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        try {
            runtime.defaults.set(defaults.copy(emergencyDismiss = false))
            val record = probe("$label-keys", ringAt(), mapOf("challenge" to AlarmContract.CHALLENGE_MATH))
            waitFor("the alarm rings", RING_WAIT_MILLIS) { runtime.rings.sounding?.alarmId == record.id }
            val screen = showChallenge()
            assertTrue("no hint while off", compose.onAllNodesWithTag(RingActivity.TAG_CHALLENGE_EMERGENCY).fetchSemanticsNodes().isEmpty())
            // While it rings the alarm stream holds the ring volume (the silent probe gives the minimum), so volume up
            // would show an adjustment; after the round the user's volume comes back.
            // zh-CN: 响铃期间闹钟音量流是响铃音量 (静音探针为最小值), 所以按音量加键能看出是否被调节; 本轮结束后恢复用户音量.
            val volume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            repeat(PRESSES) { instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_UP) }
            SystemClock.sleep(1_000)
            assertEquals("off: the round goes on", record.id, runtime.rings.sounding?.alarmId)
            assertEquals("off: the volume keys do not change the volume", volume, audio.getStreamVolume(AudioManager.STREAM_ALARM))

            // The screen reads the setting when the ring state changes; recreate it after switching on.
            // zh-CN: 界面在响铃状态变化时读取设置; 开启后重建界面.
            runtime.defaults.set(defaults.copy(emergencyDismiss = true))
            instrumentation.runOnMainSync { screen.recreate() }
            compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag(RingActivity.TAG_CHALLENGE_EMERGENCY).fetchSemanticsNodes().isNotEmpty() }
            repeat(PRESSES - 1) { instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_UP) }
            SystemClock.sleep(300)
            assertEquals("on: ${PRESSES - 1} presses keep the round", record.id, runtime.rings.sounding?.alarmId)
            assertEquals("on: the volume keys do not change the volume", volume, audio.getStreamVolume(AudioManager.STREAM_ALARM))
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_DOWN)
            waitFor("the round ends", WAIT_MILLIS) { runtime.rings.ringing().none { it.id == record.id } }
            val dismiss = dismissOf(events, record.id)
            assertEquals(AlarmContract.BY_USER, dismiss.by)
            assertEquals(AlarmContract.CHALLENGE_MATH, dismiss.challenge)
            waitFor("the user's alarm volume is back", WAIT_MILLIS) { audio.getStreamVolume(AudioManager.STREAM_ALARM) == userVolume }
            assertFalse(events.any { it.type == AlarmContract.EVENT_DISMISS && it.by == AlarmContract.BY_CHALLENGE })
            waitFor("the ring screen closes", WAIT_MILLIS) { resumedRingScreen() == null }
        } finally {
            runtime.defaults.set(defaults)
        }
    }

    /** The events of the runtime from now on; [cleanUp] removes the listener. zh-CN: 从现在起运行时的事件; [cleanUp] 移除监听器. */
    private fun listen(): List<AlarmEvent> {
        val events = CopyOnWriteArrayList<AlarmEvent>()
        val listener: (AlarmEvent) -> Unit = { events += it }
        runtime.events.addListener(listener)
        listeners += listener
        return events
    }

    private fun dismissOf(events: List<AlarmEvent>, alarmId: String): AlarmEvent {
        waitFor("the dismiss event", WAIT_MILLIS) { events.any { it.type == AlarmContract.EVENT_DISMISS && it.alarmId == alarmId } }
        return events.first { it.type == AlarmContract.EVENT_DISMISS && it.alarmId == alarmId }
    }

    /** The answer of a problem text `a op b` or `a x b op c`. zh-CN: 题目文字 `a op b` 或 `a x b op c` 的答案. */
    private fun solve(problem: String): Long {
        val parts = problem.split(" ")
        fun apply(a: Long, op: String, b: Long) = when (op) {
            "+" -> a + b
            "-" -> a - b
            else -> a * b
        }
        return if (parts.size == 3) {
            apply(parts[0].toLong(), parts[1], parts[2].toLong())
        } else {
            apply(parts[0].toLong() * parts[2].toLong(), parts[3], parts[4].toLong())
        }
    }

    private fun answer(input: String) {
        compose.onNodeWithTag(RingActivity.TAG_CHALLENGE_INPUT).performTextInput(input)
        compose.onNodeWithTag(RingActivity.TAG_CHALLENGE_CHECK).performClick()
        compose.waitForIdle()
    }

    private fun text(tag: String): String =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString("") { it.text }

    /** Opens the ring screen and waits for its challenge layer. zh-CN: 打开响铃界面并等待其挑战层. */
    private fun showChallenge(): RingActivity {
        val screen = showRingScreen()
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag(RingActivity.TAG_CHALLENGE).fetchSemanticsNodes().isNotEmpty() }
        return screen
    }

    /** The next whole second at least [RING_SECONDS] away. zh-CN: 至少 [RING_SECONDS] 秒之后的整秒. */
    private fun ringAt(): Long = (System.currentTimeMillis() / 1000 + RING_SECONDS) * 1000

    private fun probe(text: String, at: Long, dismiss: Map<String, Any>? = null): AlarmRecord = runtime.repository.add(
        mapOf(
            "at" to at, "label" to text, "snooze" to mapOf("minutes" to 5, "max" to 3),
            "sound" to mapOf("source" to "silent", "volume" to 0), "vibrate" to false,
        ) + (if (dismiss != null) mapOf("dismiss" to dismiss) else emptyMap()),
        AlarmContract.SOURCE_TOOL,
    )

    private fun openRingScreen() {
        context.startActivity(Intent(context, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Opens the ring screen (the full-screen intent may have opened it already) and waits for its content. zh-CN: 打开响铃界面并等待其内容. */
    private fun showRingScreen(): RingActivity {
        openRingScreen()
        waitFor("the ring screen resumes", WAIT_MILLIS) { resumedRingScreen() != null }
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag(RingActivity.TAG_LABEL).fetchSemanticsNodes().isNotEmpty() }
        return resumedRingScreen()!!
    }

    private fun resumedRingScreen(): RingActivity? {
        var found: RingActivity? = null
        instrumentation.runOnMainSync {
            found = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<RingActivity>().firstOrNull()
        }
        return found
    }

    private fun waitFor(what: String, millis: Long, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + millis
        while (!condition()) {
            if (SystemClock.uptimeMillis() > deadline) throw AssertionError("$what: not within $millis ms")
            SystemClock.sleep(200)
        }
    }

    companion object {
        const val LABEL_PREFIX = "p32-probe"
        private const val WAIT_MILLIS = 10_000L
        private const val RING_SECONDS = 12
        private const val RING_WAIT_MILLIS = 90_000L
        private const val MATH_COUNT = 2
        private const val PRESSES = 5
    }
}
