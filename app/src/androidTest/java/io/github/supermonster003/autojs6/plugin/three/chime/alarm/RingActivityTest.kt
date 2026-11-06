package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.app.Instrumentation
import android.content.Context
import android.content.Intent
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * The ringing screen of the installed plugin (roadmap P3.2, T86) on real rings: the label, the snoozes left, the snooze
 * button with its length, the speaking mark, back doing nothing, snooze closing the screen; two alarms due together
 * show "1 more alarm", the slider dismisses the first and shows the second, and a click from an accessibility service
 * on the slider dismisses that one and closes the screen.
 *
 * The probe alarms carry the label prefix `p32-probe`, ring silently without vibration, and [cleanUp] ends their rounds
 * and removes them. The screen is started directly: the full-screen intent opens it only while the device is not in
 * use.
 *
 * zh-CN: 已安装插件的响铃界面 (路线图 P3.2, T86), 用真实响铃验证: 标签, 剩余贪睡次数, 带时长的贪睡按钮, 朗读标记, 返回键无效, 贪睡后界面关闭;
 * 两个同时到点的闹钟显示 "还有 1 个闹钟", 滑块关闭第一个后显示第二个, 无障碍服务点按滑块关闭第二个并关闭界面. 探针闹钟带标签前缀 `p32-probe`,
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
        val sounding = runtime.rings.sounding!!.record.spec.label!!
        val waiting = if (sounding == first.spec.label) second.spec.label!! else first.spec.label!!

        compose.onNodeWithTag(RingActivity.TAG_LABEL).assert(hasText(sounding))
        compose.onNodeWithTag(RingActivity.TAG_QUEUE).assert(hasText(screen.resources.getQuantityString(R.plurals.ring_queue, 1, 1)))

        val rtl = screen.window.decorView.layoutDirection == View.LAYOUT_DIRECTION_RTL
        compose.onNodeWithTag(RingActivity.TAG_DISMISS).performTouchInput { if (rtl) swipeLeft() else swipeRight() }
        waitFor("the second alarm sounds", WAIT_MILLIS) { runtime.rings.sounding?.record?.spec?.label == waiting }
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

    /** The next whole second at least [RING_SECONDS] away. zh-CN: 至少 [RING_SECONDS] 秒之后的整秒. */
    private fun ringAt(): Long = (System.currentTimeMillis() / 1000 + RING_SECONDS) * 1000

    private fun probe(text: String, at: Long): AlarmRecord = runtime.repository.add(
        mapOf(
            "at" to at, "label" to text, "snooze" to mapOf("minutes" to 5, "max" to 3),
            "sound" to mapOf("source" to "silent"), "vibrate" to false,
        ),
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
    }
}
