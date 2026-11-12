package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.app.Activity
import android.app.Instrumentation
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.timer.TimerNotifications
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.HomeActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.HomeIntents
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.HomePage
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.stopwatch.StopwatchView
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.TimerSpec
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
 * The home pages of 1.1.0 in the installed plugin (roadmap P7.4, T136 - T139, T122, T123): the bottom navigation switches
 * between the alarms, the timers and the stopwatch and back returns to the alarms; a preset starts a timer (source `user`)
 * whose card pauses, adds a minute, resets, starts and deletes it; the custom duration dialog keeps its start button off
 * outside 1 s - 100 h and starts a labelled timer; the stopwatch starts, takes laps, marks the fastest and the slowest,
 * pauses and resets; a tap on a timer or the stopwatch notification opens its page.
 *
 * Timers made here are cancelled afterwards; the stopwatch steps run only while the stopwatch is idle and reset it at
 * the end, and the stored stopwatch notification setting is restored.
 *
 * zh-CN: 已安装插件中 1.1.0 的首页各页 (路线图 P7.4, T136 - T139, T122, T123): 底部导航在闹钟, 计时器与秒表之间切换, 返回键回到闹钟页; 预设开始一个计时器
 * (来源 `user`), 其卡片可以暂停, 加一分钟, 重置, 开始与删除; 自定义时长对话框在 1 秒 - 100 小时之外时开始按钮不可用, 并能开始带标签的计时器; 秒表开始, 计次,
 * 标出最快与最慢圈, 暂停并重置; 点按计时器或秒表通知打开对应页面. 这里创建的计时器在结束后取消; 秒表步骤只在秒表空闲时运行, 结束时重置, 并恢复存储的秒表通知设置.
 */
@RunWith(AndroidJUnit4::class)
class HomePagesTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation: Instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val context: Context
        get() = instrumentation.targetContext

    private val runtime: AlarmRuntime
        get() = AlarmRuntime.get(context)

    private val label = "p74-probe-${UUID.randomUUID().toString().take(8)}"

    /** Timers that existed before the test. zh-CN: 测试之前已有的计时器. */
    private lateinit var before: Set<String>
    private var stopwatchShown = true
    private var stopwatchUsed = false

    @Before
    fun remember() {
        before = runtime.timers.list().mapTo(HashSet()) { it.id }
        stopwatchShown = TimerNotifications.stopwatchShown(context)
    }

    @After
    fun restore() {
        finishAll()
        runtime.timers.list().filter { it.id !in before }.forEach { runCatching { runtime.userTimerApi.cancel(it.id) } }
        if (stopwatchUsed) runtime.stopwatch.reset()
        TimerNotifications.setStopwatchShown(context, stopwatchShown)
    }

    @Test
    fun theBottomNavigationSwitchesPagesAndBackReturnsToTheAlarms() {
        val home = open(HomePage.ALARMS)
        waitShown("chime.home.add")
        compose.onNodeWithTag("chime.nav.timers").performClick()
        waitShown("chime.timers.add")
        compose.onNodeWithTag("chime.nav.stopwatch").performClick()
        waitShown("chime.stopwatch.time")
        instrumentation.runOnMainSync { home.onBackPressedDispatcher.onBackPressed() }
        waitShown("chime.home.add")
        assertTrue("back on another page keeps the screen", !home.isFinishing)
    }

    @Test
    fun aPresetStartsATimerThatItsCardControls() {
        assumeTrue("room for a timer", runtime.timers.list().size < AlarmLimits.MAX_TIMERS)
        open(HomePage.TIMERS)
        waitShown("chime.timers.preset.60")
        compose.onNodeWithTag("chime.timers.preset.60").performClick()
        val id = waitForNewTimer()
        val record = runtime.timers.get(id)!!
        assertEquals(AlarmContract.SOURCE_USER, record.source)
        assertEquals(60_000L, record.durationMillis)
        assertTrue(record.running)

        val card = "chime.timers.timer.$id"
        tap(id, "$card.pause")
        waitFor("paused") { runtime.timers.get(id)?.state == AlarmContract.TIMER_PAUSED }
        tap(id, "$card.add_minute")
        waitFor("one more minute") { (runtime.timers.get(id)?.remainingMillis ?: 0L) > 100_000L }
        tap(id, "$card.reset")
        waitFor("reset") { runtime.timers.get(id)?.let { !it.running && it.remainingMillis == it.durationMillis } == true }
        tap(id, "$card.start")
        waitFor("running again") { runtime.timers.get(id)?.running == true }
        tap(id, "$card.delete")
        waitFor("deleted") { runtime.timers.get(id) == null }
        compose.waitUntil(WAIT_MILLIS) { !compose.shows(hasTestTag(card)) }
    }

    @Test
    fun theCustomDialogKeepsTheRangeAndStartsALabelledTimer() {
        assumeTrue("room for a timer", runtime.timers.list().size < AlarmLimits.MAX_TIMERS)
        open(HomePage.TIMERS)
        waitShown("chime.timers.add")
        compose.onNodeWithTag("chime.timers.add").performClick()
        val dialog = "chime.timers.customDialog"
        waitShown(dialog)
        compose.onNodeWithTag("$dialog.confirm").assertIsNotEnabled()
        compose.onNodeWithTag("$dialog.hours").performTextInput("101")
        compose.onNodeWithTag("$dialog.confirm").assertIsNotEnabled()
        compose.onNodeWithTag("$dialog.hours").performTextClearance()
        compose.onNodeWithTag("$dialog.minutes").performTextInput("1")
        compose.onNodeWithTag("$dialog.seconds").performTextInput("30")
        compose.onNodeWithTag("$dialog.label").performTextInput(label)
        compose.onNodeWithTag("$dialog.confirm").assertIsEnabled().performClick()
        compose.waitUntil(WAIT_MILLIS) { !compose.shows(hasTestTag(dialog)) }

        val id = waitForNewTimer()
        val record = runtime.timers.get(id)!!
        assertEquals(label, record.spec.label)
        assertEquals(90_000L, record.durationMillis)
        assertTrue(record.running)
        waitShown("chime.timers.timer.$id")
    }

    @Test
    fun theStopwatchTakesLapsAndMarksTheFastestAndTheSlowest() {
        assumeTrue("the stopwatch is in use", runtime.stopwatch.state().state == AlarmContract.STOPWATCH_IDLE)
        stopwatchUsed = true
        open(HomePage.STOPWATCH)
        waitShown("chime.stopwatch.start")
        compose.onNodeWithTag("chime.stopwatch.start").performClick()
        waitFor("running") { runtime.stopwatch.state().state == AlarmContract.STOPWATCH_RUNNING }
        for ((i, pause) in listOf(300L, 900L, 300L).withIndex()) {
            SystemClock.sleep(pause)
            waitShown("chime.stopwatch.lap")
            compose.onNodeWithTag("chime.stopwatch.lap").performClick()
            waitFor("lap ${i + 1}") { runtime.stopwatch.state().laps.size == i + 1 }
        }
        val marks = StopwatchView.marks(runtime.stopwatch.state().laps)
        assertNotNull("three laps of different lengths have a fastest", marks.fastest)
        waitShown("chime.stopwatch.lap.${marks.fastest}.fastest")
        waitShown("chime.stopwatch.lap.${marks.slowest}.slowest")
        waitShown("chime.stopwatch.currentLap")

        compose.onNodeWithTag("chime.stopwatch.pause").performClick()
        waitFor("paused") { runtime.stopwatch.state().state == AlarmContract.STOPWATCH_PAUSED }
        waitShown("chime.stopwatch.reset")
        compose.onNodeWithTag("chime.stopwatch.reset").performClick()
        waitFor("reset") { runtime.stopwatch.state().let { it.state == AlarmContract.STOPWATCH_IDLE && it.laps.isEmpty() } }
        waitShown("chime.stopwatch.start")
    }

    @Test
    fun aTapOnATimerOrTheStopwatchNotificationOpensItsPage() {
        assumeTrue("notifications are allowed", NotificationManagerCompat.from(context).areNotificationsEnabled())
        assumeTrue("room for a timer", runtime.timers.list().size < AlarmLimits.MAX_TIMERS)
        open(HomePage.ALARMS)
        waitShown("chime.home.add")

        val timer = runtime.userTimerApi.start(10 * 60_000L, TimerSpec(label = label))
        val timerNotice = waitForNotification(TAG_TIMER, timer.id.hashCode())
        assertEquals(HomeIntents.pending(context, HomePage.TIMERS), timerNotice.contentIntent)
        timerNotice.contentIntent.send()
        waitShown("chime.timers.timer.${timer.id}")

        if (runtime.stopwatch.state().state == AlarmContract.STOPWATCH_IDLE) {
            stopwatchUsed = true
            TimerNotifications.setStopwatchShown(context, true)
            runtime.stopwatch.start()
            val stopwatchNotice = waitForNotification(TAG_STOPWATCH, STOPWATCH_ID)
            assertEquals(HomeIntents.pending(context, HomePage.STOPWATCH), stopwatchNotice.contentIntent)
            stopwatchNotice.contentIntent.send()
            waitShown("chime.stopwatch.lap")
        }
    }

    private fun open(page: HomePage): HomeActivity {
        val monitor = instrumentation.addMonitor(HomeActivity::class.java.name, null, false)
        try {
            context.startActivity(HomeIntents.of(context, page))
            return instrumentation.waitForMonitorWithTimeout(monitor, WAIT_MILLIS) as? HomeActivity ?: throw AssertionError("the home screen did not open")
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    private fun waitShown(tag: String) = compose.waitUntil(WAIT_MILLIS) { compose.shows(hasTestTag(tag)) }

    /** Scrolls the timer list to the card of [id] and clicks [tag]. zh-CN: 把计时器列表滚动到 [id] 的卡片并点击 [tag]. */
    private fun tap(id: String, tag: String) {
        compose.onNodeWithTag("chime.timers.list").performScrollToKey(id)
        waitShown(tag)
        compose.onNodeWithTag(tag).performClick()
    }

    private fun waitForNewTimer(): String {
        var id: String? = null
        waitFor("a new timer") { id = runtime.timers.list().firstOrNull { it.id !in before }?.id; id != null }
        return id!!
    }

    private fun waitForNotification(tag: String, id: Int): Notification {
        val manager = context.getSystemService(NotificationManager::class.java)
        var found: Notification? = null
        waitFor("notification $tag") { found = manager.activeNotifications.firstOrNull { it.tag == tag && it.id == id }?.notification; found != null }
        return found!!
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + WAIT_MILLIS
        while (!condition()) {
            if (SystemClock.uptimeMillis() > deadline) throw AssertionError("$what: not within $WAIT_MILLIS ms")
            SystemClock.sleep(100)
        }
    }

    private fun finishAll() {
        instrumentation.runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.RESUMED, Stage.PAUSED, Stage.STOPPED).flatMap { monitor.getActivitiesInStage(it) }.forEach(Activity::finish)
        }
    }

    companion object {
        private const val WAIT_MILLIS = 10_000L

        // The tags and the id TimerNotifications posts under. zh-CN: TimerNotifications 发出通知时使用的标签与 id.
        private const val TAG_TIMER = "timer"
        private const val TAG_STOPWATCH = "stopwatch"
        private const val STOPWATCH_ID = 3
    }
}
