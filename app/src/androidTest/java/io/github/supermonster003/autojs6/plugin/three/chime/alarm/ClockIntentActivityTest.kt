package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.app.Activity
import android.app.AlertDialog
import android.app.Instrumentation
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.SystemClock
import android.provider.AlarmClock
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.intent.ClockActions
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.intent.ClockDialogActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.intent.ClockIntentActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.intent.ClockRequests
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.ClockDraftActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.HomeActivity
import org.autojs.plugin.alarm.api.AlarmContract
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.UUID

/**
 * The system clock intents of the installed plugin (roadmap P2.6, D21, T79, T80), started as intents at
 * [ClockIntentActivity] and read back from its outcome line and the alarm list: `SET_ALARM` saves once and enables the
 * same alarm the second time (source `intent`), without `EXTRA_SKIP_UI` or a time it opens the edit page (roadmap P3.1,
 * T83), which saves with source `intent` or closes without saving; `SHOW_ALARMS` opens the alarm list; `DISMISS_ALARM` skips a ring within a day, refuses one further away, finds
 * nothing for an unknown label and lets the user choose among several matches; `SNOOZE_ALARM` without a ringing alarm
 * and the refused extras; a ringing alarm is snoozed for one minute by `SNOOZE_ALARM` and its round ended by
 * `DISMISS_ALARM` without a search mode.
 *
 * Every alarm here carries the label prefix `p26-probe`; [cleanUp] removes them, so a run leaves the alarm list as
 * it found it. Nothing here touches the device's other alarms (no `all` search on a device).
 *
 * zh-CN: 已安装插件的系统时钟 Intent (路线图 P2.6, D21, T79, T80), 以 Intent 启动 [ClockIntentActivity], 再从其结果行与闹钟列表读回: `SET_ALARM`
 * 只保存一次, 第二次改为启用同一闹钟 (来源 `intent`), 没有 `EXTRA_SKIP_UI` 或时间时打开编辑页 (路线图 P3.1, T83), 以来源 `intent` 保存或不保存直接关闭; `SHOW_ALARMS` 打开闹钟列表; `DISMISS_ALARM`
 * 跳过一天之内的响铃, 拒绝更远的, 未知标签找不到闹钟, 多个匹配时由用户选择; `SNOOZE_ALARM` 在没有闹钟响铃时的结果与被拒的 extras; 正在响的闹钟经
 * `SNOOZE_ALARM` 贪睡一分钟, 再由不带搜索模式的 `DISMISS_ALARM` 结束本轮. 这里的闹钟都带标签前缀 `p26-probe`; [cleanUp] 删除它们, 运行后闹钟
 * 列表保持原样. 不触碰设备上的其他闹钟 (设备上不做 `all` 搜索).
 */
@RunWith(AndroidJUnit4::class)
class ClockIntentActivityTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation: Instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val context: Context
        get() = instrumentation.targetContext

    private val runtime: AlarmRuntime
        get() = AlarmRuntime.get(context)

    private val label = "$LABEL_PREFIX-${UUID.randomUUID().toString().take(8)}"

    private val monitors = ArrayList<Instrumentation.ActivityMonitor>()

    /** The page [dialogOf] found last (the monitor forgets it once waited for). zh-CN: [dialogOf] 最近找到的页面 (监视器在等待后即不再保留). */
    private var page: ClockDialogActivity? = null

    @After
    fun cleanUp() {
        monitors.forEach { instrumentation.removeMonitor(it) }
        runtime.repository.list().filter { it.spec.label?.startsWith(LABEL_PREFIX) == true }.forEach { runtime.repository.remove(it.id, AlarmContract.SOURCE_TOOL) }
    }

    @Test
    fun setAlarmSavesOnceAndEnablesTheSameAlarm() {
        val extras = arrayOf(
            AlarmClock.EXTRA_HOUR to 6, AlarmClock.EXTRA_MINUTES to 41, AlarmClock.EXTRA_MESSAGE to label,
            AlarmClock.EXTRA_DAYS to arrayListOf(Calendar.MONDAY, Calendar.FRIDAY), AlarmClock.EXTRA_VIBRATE to false,
            AlarmClock.EXTRA_SKIP_UI to true,
        )
        val first = send(AlarmClock.ACTION_SET_ALARM, *extras)
        assertTrue(first, first.startsWith("setAlarm added="))
        val id = first.substringAfter("added=")
        val record = runtime.repository.get(id)!!
        assertEquals(AlarmContract.SOURCE_INTENT, record.source)
        assertEquals(label, record.spec.label)
        assertEquals(6, record.spec.time!!.hour)
        assertEquals(41, record.spec.time!!.minute)
        assertEquals(ClockRequests.repeatOf(arrayListOf(Calendar.MONDAY, Calendar.FRIDAY)), record.spec.repeat)
        assertEquals(false, record.spec.vibrate)

        runtime.repository.setEnabled(id, false, AlarmContract.SOURCE_TOOL)
        assertEquals("setAlarm enabled=$id", send(AlarmClock.ACTION_SET_ALARM, *extras))
        val again = runtime.repository.get(id)!!
        assertTrue(again.enabled)
        assertEquals(AlarmContract.SOURCE_INTENT, again.updatedBy)
        assertEquals(1, probes(label).size)

        val other = send(AlarmClock.ACTION_SET_ALARM, *extras, AlarmClock.EXTRA_MESSAGE to "$label-other")
        assertTrue(other, other.startsWith("setAlarm added="))
        assertEquals(2, probes(label).size)
    }

    @Test
    fun setAlarmWithoutSkipUiOpensTheFilledPage() {
        val page = monitor(ClockDraftActivity::class.java)
        assertEquals("setAlarm draft=filled", send(AlarmClock.ACTION_SET_ALARM, AlarmClock.EXTRA_HOUR to 6, AlarmClock.EXTRA_MINUTES to 5, AlarmClock.EXTRA_MESSAGE to label))
        val activity = editPage(page)
        compose.onNodeWithTag("chime.edit.label").assert(hasText(label, substring = true))
        ClockActions.lastOutcome = null
        compose.onNodeWithTag("chime.edit.save").performClick()
        val saved = outcome()
        assertTrue(saved, saved.startsWith("setAlarm added="))
        val record = runtime.repository.get(saved.substringAfter("added="))!!
        assertEquals(AlarmContract.SOURCE_INTENT, record.source)
        assertEquals(label, record.spec.label)
        assertEquals(6, record.spec.time!!.hour)
        assertEquals(5, record.spec.time!!.minute)
        waitFor("the page closes after saving", WAIT_MILLIS) { activity.isFinishing || activity.isDestroyed }
    }

    @Test
    fun setAlarmWithoutTimeOpensTheNewPage() {
        val page = monitor(ClockDraftActivity::class.java)
        assertEquals("setAlarm draft=new", send(AlarmClock.ACTION_SET_ALARM, AlarmClock.EXTRA_MESSAGE to label, AlarmClock.EXTRA_SKIP_UI to true))
        val activity = editPage(page)
        instrumentation.runOnMainSync { activity.onBackPressedDispatcher.onBackPressed() }
        waitFor("an unchanged page closes on back", WAIT_MILLIS) { activity.isFinishing || activity.isDestroyed }
        assertEquals(0, probes(label).size)
    }

    @Test
    fun showAlarmsOpensTheList() {
        val list = monitor(HomeActivity::class.java)
        assertEquals("showAlarms shown", send(AlarmClock.ACTION_SHOW_ALARMS))
        val activity = instrumentation.waitForMonitorWithTimeout(list, WAIT_MILLIS)
        assertNotNull("the alarm list opened", activity)
        activity.finish()
    }

    @Test
    fun dismissAlarmSkipsWithinADayOnly() {
        val now = System.currentTimeMillis()
        val soon = runtime.repository.add(mapOf("at" to now + 2 * HOUR, "label" to "$label-soon"), AlarmContract.SOURCE_TOOL)
        val local = LocalDateTime.now(ZoneId.systemDefault())
        val day = local.plusDays(3).dayOfWeek.name.take(2)
        val far = runtime.repository.add(
            mapOf("hour" to local.hour, "minute" to local.minute, "repeat" to "FREQ=WEEKLY;BYDAY=$day", "label" to "$label-far"),
            AlarmContract.SOURCE_TOOL,
        )
        assertEquals("dismissAlarm matched=1 ${soon.id}:skipped", dismissByLabel("$label-soon"))
        assertFalse("a skipped one-shot alarm is disabled", runtime.repository.get(soon.id)!!.enabled)
        assertEquals(AlarmContract.SOURCE_INTENT, runtime.repository.get(soon.id)!!.updatedBy)

        assertEquals("dismissAlarm matched=1 ${far.id}:tooEarly", dismissByLabel("$label-far"))
        val kept = runtime.repository.get(far.id)!!
        assertTrue(kept.enabled)
        assertNull(kept.state.skipAt)

        assertEquals("dismissAlarm matched=0", dismissByLabel("$label-none"))
    }

    @Test
    fun dismissAlarmLetsTheUserChooseAmongSeveral() {
        val first = runtime.repository.add(mapOf("hour" to 3, "minute" to 17, "repeat" to "FREQ=DAILY", "label" to "$label-a"), AlarmContract.SOURCE_TOOL)
        val second = runtime.repository.add(mapOf("hour" to 3, "minute" to 17, "repeat" to "FREQ=DAILY", "label" to "$label-b"), AlarmContract.SOURCE_TOOL)
        val search = ClockRequests.Search.Time(3, 17)
        val found = ClockRequests.matches(search, runtime.repository.list(), emptySet(), System.currentTimeMillis(), ZoneId.systemDefault())
        val page = monitor(ClockDialogActivity::class.java)
        val outcome = send(
            AlarmClock.ACTION_DISMISS_ALARM, AlarmClock.EXTRA_ALARM_SEARCH_MODE to AlarmClock.ALARM_SEARCH_MODE_TIME,
            AlarmClock.EXTRA_HOUR to 3, AlarmClock.EXTRA_MINUTES to 17, AlarmClock.EXTRA_IS_PM to false,
        )
        assertEquals("dismissAlarm matched=${found.size} choose", outcome)
        val dialog = dialogOf(page)
        val index = found.indexOfFirst { it.id == first.id }
        assertEquals(found.size, dialog.listView.adapter.count)
        ClockActions.lastOutcome = null
        instrumentation.runOnMainSync { dialog.listView.performItemClick(null, index, index.toLong()) }
        assertEquals("dismissAlarm matched=1 ${first.id}:skipped", outcome())
        assertNotNull(runtime.repository.get(first.id)!!.state.skipAt)
        assertNull(runtime.repository.get(second.id)!!.state.skipAt)
    }

    @Test
    fun snoozeWithoutRingAndRefusedExtras() {
        if (runtime.rings.ringing().none { !it.snoozed }) {
            assertEquals("snoozeAlarm ringing=0", send(AlarmClock.ACTION_SNOOZE_ALARM))
        }
        val snooze = send(AlarmClock.ACTION_SNOOZE_ALARM, AlarmClock.EXTRA_ALARM_SNOOZE_DURATION to 0)
        assertTrue(snooze, snooze.startsWith("snoozeAlarm error=INVALID_OPTIONS"))
        val dismiss = send(AlarmClock.ACTION_DISMISS_ALARM, AlarmClock.EXTRA_ALARM_SEARCH_MODE to "soon")
        assertTrue(dismiss, dismiss.startsWith("dismissAlarm error=INVALID_OPTIONS"))
        val noLabel = send(AlarmClock.ACTION_DISMISS_ALARM, AlarmClock.EXTRA_ALARM_SEARCH_MODE to AlarmClock.ALARM_SEARCH_MODE_LABEL)
        assertTrue(noLabel, noLabel.startsWith("dismissAlarm error=INVALID_OPTIONS"))
    }

    @Test
    fun snoozeAndDismissARingingAlarm() {
        assumeTrue("another alarm rings", runtime.rings.ringing().isEmpty())
        // Starting an activity first clears the stopped state that `am instrument` leaves, so the alarm is delivered.
        assertTrue(send(AlarmClock.ACTION_SNOOZE_ALARM, AlarmClock.EXTRA_ALARM_SNOOZE_DURATION to 0).startsWith("snoozeAlarm error="))
        val at = (System.currentTimeMillis() / 1000 + RING_SECONDS) * 1000
        val record = runtime.repository.add(
            mapOf("at" to at, "label" to "$label-ring", "snooze" to mapOf("minutes" to 5, "max" to 3)),
            AlarmContract.SOURCE_TOOL,
        )
        waitFor("the alarm rings", RING_WAIT_MILLIS) { runtime.rings.ringing().any { it.id == record.id && !it.snoozed } }

        assertEquals("snoozeAlarm snoozed ringing=1 minutes=1", send(AlarmClock.ACTION_SNOOZE_ALARM, AlarmClock.EXTRA_ALARM_SNOOZE_DURATION to 1))
        val snoozedAt = System.currentTimeMillis()
        waitFor("the ring is snoozed", WAIT_MILLIS) { runtime.rings.ringing().any { it.id == record.id && it.snoozed } }
        val state = runtime.repository.get(record.id)!!.state
        assertEquals(1, state.snoozeCount)
        val until = state.snoozeUntil ?: throw AssertionError("no snoozeUntil")
        assertTrue("snoozed for one minute, not the 5 of the alarm: ${until - snoozedAt} ms", until - snoozedAt in 50_000L..65_000L)

        assertEquals("dismissAlarm matched=1 ${record.id}:ended", send(AlarmClock.ACTION_DISMISS_ALARM))
        waitFor("the round ended", WAIT_MILLIS) { runtime.rings.ringing().none { it.id == record.id } }
        assertNull(runtime.repository.get(record.id)!!.state.snoozeUntil)
    }

    private fun waitFor(what: String, millis: Long, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + millis
        while (!condition()) {
            if (SystemClock.uptimeMillis() > deadline) throw AssertionError("$what: not within $millis ms")
            SystemClock.sleep(200)
        }
    }

    private fun probes(prefix: String): List<AlarmRecord> = runtime.repository.list().filter { it.spec.label?.startsWith(prefix) == true }

    private fun dismissByLabel(text: String): String =
        send(AlarmClock.ACTION_DISMISS_ALARM, AlarmClock.EXTRA_ALARM_SEARCH_MODE to AlarmClock.ALARM_SEARCH_MODE_LABEL, AlarmClock.EXTRA_MESSAGE to text)

    /** Starts [ClockIntentActivity] with [action] and [extras] and answers its outcome line. zh-CN: 启动并返回结果行. */
    private fun send(action: String, vararg extras: Pair<String, Any>): String {
        awaitNoClockIntentActivity()
        ClockActions.lastOutcome = null
        val intent = Intent(action).setClass(context, ClockIntentActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        for ((key, value) in extras) {
            when (value) {
                is Int -> intent.putExtra(key, value)
                is Boolean -> intent.putExtra(key, value)
                is String -> intent.putExtra(key, value)
                is ArrayList<*> -> intent.putIntegerArrayListExtra(key, ArrayList(value.map { it as Int }))
                else -> error("unsupported extra $key")
            }
        }
        context.startActivity(intent)
        return outcome()
    }

    /**
     * Waits until the [ClockIntentActivity] of the previous [send] is destroyed. The outcome line is set inside its
     * `onCreate`, before `finish()`; a `FLAG_ACTIVITY_NEW_TASK` start of the same action in that window finds the task it
     * still roots and only brings that task to the front, so no activity and no outcome follow (CI API 33, 2026-11-07:
     * the second `send` of two tests). zh-CN: 等上一次 [send] 的 [ClockIntentActivity] 销毁. 结果行在其 `onCreate` 里, `finish()` 之前写入;
     * 在这段时间内以 `FLAG_ACTIVITY_NEW_TASK` 启动同一 action, 会找到它仍作为根的任务并只把该任务移到前台, 不会创建 activity, 也没有结果行
     * (CI API 33, 2026-11-07: 两个用例的第二次 `send`).
     */
    private fun awaitNoClockIntentActivity() {
        val live = Stage.values().filter { it != Stage.DESTROYED }
        waitFor("the previous ClockIntentActivity is destroyed", WAIT_MILLIS) {
            var alive = false
            instrumentation.runOnMainSync {
                val registry = ActivityLifecycleMonitorRegistry.getInstance()
                alive = live.any { stage -> registry.getActivitiesInStage(stage).any { it is ClockIntentActivity } }
            }
            !alive
        }
    }

    private fun outcome(): String {
        val deadline = SystemClock.uptimeMillis() + OUTCOME_WAIT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            ClockActions.lastOutcome?.let { return it }
            SystemClock.sleep(50)
        }
        throw AssertionError("no outcome within $OUTCOME_WAIT_MILLIS ms")
    }

    private fun monitor(type: Class<out Activity>): Instrumentation.ActivityMonitor =
        instrumentation.addMonitor(type.name, null, false).also { monitors += it }

    /** The edit page [page] opened, once its content shows. zh-CN: [page] 打开的编辑页, 等到其内容显示. */
    private fun editPage(page: Instrumentation.ActivityMonitor): ClockDraftActivity {
        val activity = instrumentation.waitForMonitorWithTimeout(page, WAIT_MILLIS) as? ClockDraftActivity
            ?: throw AssertionError("the edit page did not open")
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("chime.edit.content").fetchSemanticsNodes().isNotEmpty() }
        return activity
    }

    /** The dialog the [ClockDialogActivity] of [page] shows. zh-CN: [page] 中 [ClockDialogActivity] 显示的对话框. */
    private fun dialogOf(page: Instrumentation.ActivityMonitor): AlertDialog {
        val activity = instrumentation.waitForMonitorWithTimeout(page, WAIT_MILLIS) as? ClockDialogActivity
            ?: throw AssertionError("the dialog page did not open")
        this.page = activity
        val deadline = SystemClock.uptimeMillis() + WAIT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            var shown: AlertDialog? = null
            instrumentation.runOnMainSync { shown = activity.dialog?.takeIf { it.isShowing } }
            shown?.let { return it }
            SystemClock.sleep(50)
        }
        throw AssertionError("the dialog did not show")
    }

    companion object {
        const val LABEL_PREFIX = "p26-probe"
        private const val WAIT_MILLIS = 10_000L

        /**
         * The wait for a skip-UI answer: the first local run on AVD_API_33 (roadmap P5.1) missed the 10 s wait three times
         * in a row and passed in the two runs after it, so the CI job gets more room. Those misses were most likely the
         * start race [awaitNoClockIntentActivity] now waits out; the wait stays generous for slow emulators.
         * zh-CN: 等待跳过界面的应答: AVD_API_33 的首次本地运行 (路线图 P5.1) 连续三次超过 10 s, 之后两次通过, 因此为 CI 留出更多余量. 那几次超时
         * 多半就是 [awaitNoClockIntentActivity] 现在等过的启动竞态; 等待时间仍为慢速模拟器保持宽裕.
         */
        private const val OUTCOME_WAIT_MILLIS = 30_000L
        private const val HOUR = 60 * 60 * 1000L
        private const val RING_SECONDS = 12
        private const val RING_WAIT_MILLIS = 90_000L
    }
}
