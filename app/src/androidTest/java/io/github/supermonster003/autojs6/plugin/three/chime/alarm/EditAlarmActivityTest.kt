package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.NextOccurrence
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RecurrenceRule
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RepeatText
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.EditAlarmActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.RepeatPreset
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmHolidayFilter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * The edit page of the installed plugin (roadmap P3.1, T83): an edit round trip (label, vibration, the Weekdays preset;
 * saved with source `user`, shown again on reopening, and an unchanged page saves nothing), and the advanced rule
 * editor, whose description equals `RepeatText` of the rule in the RRULE field, whose preview lists the next five
 * rings, whose form follows the typed rule and writes it back, and whose rule is what the page saves; and the holiday
 * option of 1.2.0 (roadmap P8.6): mode and region saved, the next ring with the holidays skipped before it and the hint
 * of an estimated year, and `workdays` shown in the repeat row with the text of the script API (T184).
 *
 * The alarms here start disabled and carry the label prefix `p31-probe`; changing the repeat turns them on (a schedule
 * change enables a disabled alarm, T83), for a time that does not come during the test, and [cleanUp] removes them.
 *
 * zh-CN: 已安装插件的编辑页 (路线图 P3.1, T83): 编辑往返 (标签, 振动, "工作日" 预设; 以来源 `user` 保存, 重新打开时显示, 未改动的页面不保存任何内容),
 * 以及高级规则编辑器: 说明与 RRULE 字段中规则的 `RepeatText` 一致, 预览列出接下来 5 次响铃, 表单跟随输入的规则并写回, 页面保存的正是该规则; 以及 1.2.0
 * 的节假日选项 (路线图 P8.6): 保存模式与地区, 下一次响铃及其之前跳过的节假日, 推算年份的提示, 重复行以脚本 API 的文案显示 `workdays` (T184). 这里的闹钟
 * 起初都处于关闭状态, 带标签前缀 `p31-probe`; 修改重复规则会启用它们 (日程有修改的已禁用闹钟会被启用, T83), 其时刻不在测试期间, [cleanUp] 删除它们.
 */
@RunWith(AndroidJUnit4::class)
class EditAlarmActivityTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val runtime: AlarmRuntime
        get() = AlarmRuntime.get(context)

    private val label = "$LABEL_PREFIX-${UUID.randomUUID().toString().take(8)}"

    @After
    fun cleanUp() {
        finishAll()
        runtime.repository.list().filter { it.spec.label?.startsWith(LABEL_PREFIX) == true }.forEach { runtime.repository.remove(it.id, AlarmContract.SOURCE_TOOL) }
    }

    @Test
    fun editRoundTrip() {
        val record = runtime.repository.add(
            mapOf("hour" to 7, "minute" to 30, "repeat" to "FREQ=DAILY", "label" to label, "enabled" to false),
            AlarmContract.SOURCE_TOOL,
        )
        val edited = "$label-edited"
        open(record.id)
        row("chime.edit.label").performClick()
        compose.onNodeWithTag("chime.edit.labelDialog.field").performTextReplacement(edited)
        compose.onNodeWithTag("chime.edit.labelDialog.confirm").performClick()
        row("chime.edit.vibrate").assertIsOn().performClick()
        row("chime.edit.repeat").performClick()
        compose.onNodeWithTag("chime.edit.repeatDialog.${RepeatPreset.WEEKDAYS.ordinal}").performClick()
        compose.onNodeWithTag("chime.edit.repeatDialog.confirm").performClick()
        compose.onNodeWithTag("chime.edit.save").performClick()
        waitForClosed()

        val saved = runtime.repository.get(record.id)!!
        assertEquals(edited, saved.spec.label)
        assertEquals(false, saved.spec.vibrate)
        assertEquals(RepeatPreset.ruleOf(RepeatPreset.WEEKDAYS), saved.spec.repeat)
        assertEquals(7, saved.spec.time!!.hour)
        assertEquals(30, saved.spec.time!!.minute)
        assertEquals(AlarmContract.SOURCE_USER, saved.updatedBy)
        assertTrue("a schedule change turns a disabled alarm on", saved.enabled)

        open(record.id)
        row("chime.edit.label").assert(hasText(edited, substring = true))
        row("chime.edit.vibrate").assertIsOff()
        row("chime.edit.repeat").assert(hasText(context.getString(R.string.edit_repeat_weekdays), substring = true))
        compose.onNodeWithTag("chime.edit.save").performClick()
        waitForClosed()
        assertEquals("an unchanged page saves nothing", saved.updatedAt, runtime.repository.get(record.id)!!.updatedAt)
    }

    @Test
    fun ruleEditorMatchesRepeatText() {
        val start = LocalDate.now().plusDays(1)
        val record = runtime.repository.add(
            mapOf("hour" to 8, "minute" to 0, "date" to start.toString(), "repeat" to "FREQ=DAILY", "label" to label, "enabled" to false),
            AlarmContract.SOURCE_TOOL,
        )
        val locale = context.resources.configuration.locales[0]
        open(record.id)
        row("chime.edit.repeat").performClick()
        compose.onNodeWithTag("chime.edit.repeatDialog.${RepeatPreset.ADVANCED.ordinal}").performClick()
        compose.onNodeWithTag("chime.edit.repeatDialog.confirm").performClick()
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("chime.edit.rule.content").fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag("chime.edit.rule.rrule").performScrollTo().performTextReplacement(TYPED)
        compose.onNodeWithTag("chime.edit.rule.text").performScrollTo().assert(hasText(RepeatText.of(TYPED, start, locale)))
        (0 until PREVIEW).forEach { compose.onNodeWithTag("chime.edit.rule.preview.$it").performScrollTo() }
        assertEquals(0, compose.onAllNodesWithTag("chime.edit.rule.preview.$PREVIEW").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithTag("chime.edit.rule.error").fetchSemanticsNodes().size)

        // The form follows the typed rule and writes the interval back into the RRULE field.
        compose.onNodeWithTag("chime.edit.rule.interval").performScrollTo().performTextReplacement("2")
        val written = fieldText("chime.edit.rule.rrule")
        assertEquals(RecurrenceRule.parse(EXPECTED), RecurrenceRule.parse(written))
        compose.onNodeWithTag("chime.edit.rule.text").performScrollTo().assert(hasText(RepeatText.of(written, start, locale)))
        (0 until PREVIEW).forEach { compose.onNodeWithTag("chime.edit.rule.preview.$it").performScrollTo() }

        compose.onNodeWithTag("chime.edit.rule.apply").performClick()
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("chime.edit.content").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("chime.edit.save").performClick()
        waitForClosed()
        assertEquals(RecurrenceRule.parse(EXPECTED), RecurrenceRule.parse(runtime.repository.get(record.id)!!.spec.repeat!!))
    }

    @Test
    fun holidayModeAndRegionRoundTrip() {
        // New Year's Day of next year: a holiday of the bundled CN data, in a year it estimates (T162).
        // zh-CN: 明年元旦: 内置 CN 数据中的节假日, 落在其推算年份 (T162).
        val newYear = LocalDate.now().plusYears(1).withDayOfYear(1)
        val record = runtime.repository.add(
            mapOf("hour" to 8, "minute" to 0, "date" to newYear.toString(), "repeat" to "FREQ=DAILY", "label" to label, "enabled" to false),
            AlarmContract.SOURCE_TOOL,
        )
        val locale = context.resources.configuration.locales[0]
        open(record.id)
        row("chime.edit.holidays").performClick()
        compose.onNodeWithTag("chime.edit.holidaysDialog.1").performClick()
        compose.onNodeWithTag("chime.edit.holidaysDialog.confirm").performClick()
        row("chime.edit.holidayRegion").performClick()
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("chime.edit.regionDialog").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("chime.edit.regionDialog.search").performTextInput("CN")
        compose.onNodeWithTag("chime.edit.regionDialog.region.CN").performClick()
        compose.onNodeWithTag("chime.edit.regionDialog.confirm").performClick()
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("chime.edit.estimated").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("chime.edit.save").performClick()
        waitForClosed()

        val saved = runtime.repository.get(record.id)!!
        assertEquals(AlarmHolidayFilter(region = "CN", mode = AlarmContract.HOLIDAY_MODE_SKIP), saved.spec.holidays)
        assertTrue("a schedule change turns a disabled alarm on", saved.enabled)
        val next = saved.state.nextTrigger!!
        val skipped = NextOccurrence.holidaySkips(saved.spec, System.currentTimeMillis(), next, ZoneId.systemDefault(), runtime.holidayJudge)
        assertTrue("New Year's Day is skipped", skipped > 0)

        open(record.id)
        row("chime.edit.holidays").assert(hasText(context.getString(R.string.edit_holidays_skip), substring = true))
        val nextText = text("chime.edit.next")
        val parts = context.resources.getQuantityString(R.plurals.home_next_holidays, skipped, MARK, skipped).split(MARK).filter { it.isNotBlank() }
        assertTrue("'$nextText' names $skipped skipped", parts.all { nextText.contains(it.trim()) })
        row("chime.edit.estimated").assert(hasText(context.getString(R.string.holiday_estimated_next)))

        row("chime.edit.holidays").performClick()
        compose.onNodeWithTag("chime.edit.holidaysDialog.3").performClick()
        compose.onNodeWithTag("chime.edit.holidaysDialog.confirm").performClick()
        row("chime.edit.repeat").assert(hasText(RepeatText.workdays(locale), substring = true))
        compose.onNodeWithTag("chime.edit.save").performClick()
        waitForClosed()
        val workdays = runtime.repository.get(record.id)!!.spec
        assertEquals(AlarmContract.HOLIDAY_MODE_WORKDAYS, workdays.holidays?.mode)
        assertEquals(RepeatText.workdays(locale), RepeatText.ofAlarm(workdays, locale))
    }

    private fun text(tag: String): String =
        compose.onNodeWithTag(tag).performScrollTo().fetchSemanticsNode().config[SemanticsProperties.Text].joinToString("\n") { it.text }

    private fun open(id: String) {
        context.startActivity(EditAlarmActivity.edit(context, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("chime.edit.content").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun row(tag: String): SemanticsNodeInteraction = compose.onNodeWithTag(tag).performScrollTo()

    private fun fieldText(tag: String): String = compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    /** Waits until no edit page is open any more. zh-CN: 等到不再有打开的编辑页. */
    private fun waitForClosed() {
        val deadline = SystemClock.uptimeMillis() + WAIT_MILLIS
        while (openPages().isNotEmpty()) {
            if (SystemClock.uptimeMillis() > deadline) throw AssertionError("the edit page did not close within $WAIT_MILLIS ms")
            SystemClock.sleep(100)
        }
    }

    private fun openPages(): List<Activity> {
        var pages = emptyList<Activity>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            pages = listOf(Stage.RESUMED, Stage.PAUSED, Stage.STARTED, Stage.CREATED)
                .flatMap { monitor.getActivitiesInStage(it) }
                .filter { it is EditAlarmActivity && !it.isFinishing }
        }
        return pages
    }

    private fun finishAll() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.RESUMED, Stage.PAUSED, Stage.STOPPED).flatMap { monitor.getActivitiesInStage(it) }.forEach(Activity::finish)
        }
    }

    companion object {
        const val LABEL_PREFIX = "p31-probe"
        private const val MARK = "\u0001"
        private const val WAIT_MILLIS = 10_000L
        private const val PREVIEW = 5
        private const val TYPED = "FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1"
        private const val EXPECTED = "FREQ=MONTHLY;INTERVAL=2;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1"
    }
}
