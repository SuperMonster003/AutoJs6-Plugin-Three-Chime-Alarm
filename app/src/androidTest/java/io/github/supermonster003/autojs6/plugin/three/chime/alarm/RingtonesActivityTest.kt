package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingDefaults
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring.RingDefaultsStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.service.SystemRingtones
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.EditAlarmActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.ringtones.RingtoneChoice
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.ringtones.RingtonePicker
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.ringtones.RingtoneRequest
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.ringtones.RingtonesActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.settings.SettingsActivity
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.RingtoneSnapshot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.random.Random

/**
 * The ringtone page of the installed plugin (roadmap P3.3, T85): it lists the same ringtones as `ringtones.list()`
 * (system and imported, a script import included), deletes an imported ringtone (the plugin default that named it goes
 * back to the system alarm sound, an alarm that names it keeps its uri), returns the confirmed choice and nothing when
 * left, asks before discarding a changed choice, and hands the choice to the edit page and the settings screen. No row
 * is previewed, so the tests play no sound. Probe ringtones carry the title prefix `p33-probe` and probe alarms the
 * label prefix; both are removed afterwards, and the plugin defaults of the device are restored.
 * zh-CN: 已安装插件的铃声页 (路线图 P3.3, T85): 列出与 `ringtones.list()` 相同的铃声 (系统与导入, 包括脚本导入的), 删除导入的铃声 (指向它的插件默认值改回
 * 系统闹钟铃声, 引用它的闹钟保留其 uri), 返回确定的选择, 直接离开时不返回, 所选项有改动时先询问再放弃, 并把选择交给编辑页与设置页. 不试听任何一行, 因此测试
 * 不发声. 探针铃声带标题前缀 `p33-probe`, 探针闹钟带同名标签前缀; 两者在测试后删除, 并恢复设备原有的插件默认值.
 */
@RunWith(AndroidJUnit4::class)
class RingtonesActivityTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val runtime: AlarmRuntime
        get() = AlarmRuntime.get(context)

    private lateinit var defaults: RingDefaults

    @Before
    fun remember() {
        defaults = RingDefaultsStore(context).get()
    }

    @After
    fun cleanUp() {
        finishAll()
        RingDefaultsStore(context).set(defaults)
        runtime.repository.list().filter { it.spec.label?.startsWith(PREFIX) == true }.forEach { runtime.repository.remove(it.id, AlarmContract.SOURCE_TOOL) }
        runtime.ringtones.list().filter { it.title.startsWith(PREFIX) }.forEach { runtime.ringtones.delete(it.uri) }
    }

    @Test
    fun listsDeletesAndReturnsTheChoice() {
        val probe = importProbe()
        val user = runtime.repository.add(
            mapOf("hour" to 6, "minute" to 0, "label" to "$PREFIX-alarm", "enabled" to false, "sound" to mapOf("source" to probe.uri)),
            AlarmContract.SOURCE_TOOL,
        )
        val store = RingDefaultsStore(context)
        store.set(defaults.copy(sound = probe.uri))

        val scenario = open(RingtoneRequest(AlarmContract.SOUND_SILENT, AlarmContract.SOUND_DEFAULT))
        // Every ringtone of ringtones.list() has its row. zh-CN: ringtones.list() 的每个铃声都有对应的行.
        (SystemRingtones.list(context) + runtime.ringtones.list()).forEach { item(itemTag(it)) }
        item("chime.ringtones.fallback").assertIsNotSelected()
        item("chime.ringtones.item.${AlarmContract.SOUND_SILENT}").assertIsSelected()

        item(itemTag(probe)).performClick().assertIsSelected()
        compose.onNodeWithTag("${itemTag(probe)}.delete").performClick()
        val dialog = "chime.ringtones.deleteDialog"
        val usage = context.resources.getQuantityString(R.plurals.ringtones_delete_used, 1, 1)
        val isDefault = context.getString(R.string.ringtones_delete_default)
        compose.waitUntil(WAIT_MILLIS) {
            compose.onAllNodes(hasTestTag("$dialog.message") and hasText(usage, substring = true) and hasText(isDefault, substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("$dialog.confirm").performClick()
        compose.waitUntil(WAIT_MILLIS) { runtime.ringtones.list().none { it.uri == probe.uri } }
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag(itemTag(probe)).fetchSemanticsNodes().isEmpty() }
        assertEquals("the plugin default goes back to the system alarm sound", AlarmContract.SOUND_DEFAULT, store.get().sound)
        assertEquals("the alarm keeps its uri (it rings the default alarm sound, T70)", probe.uri, runtime.repository.get(user.id)!!.spec.sound?.source)
        // The deleted choice falls back to "Default", which now stands for the system alarm sound.
        // zh-CN: 被删除的选择回到 "默认", 它现在代表系统闹钟铃声.
        item("chime.ringtones.fallback").assertIsSelected()

        item("chime.ringtones.item.${AlarmContract.SOUND_DEFAULT}").performClick()
        compose.onNodeWithTag("chime.ringtones.confirm").performClick()
        val result = scenario.result
        assertEquals(RingtoneChoice(AlarmContract.SOUND_DEFAULT), RingtonePicker().parseResult(result.resultCode, result.resultData))
        scenario.close()
    }

    @Test
    fun settingsModeAsksBeforeDiscardingAndShowsAMissingSound() {
        val scenario = open(RingtoneRequest(MISSING, null))
        assertEquals("the plugin default has no Default choice", 0, compose.onAllNodesWithTag("chime.ringtones.fallback").fetchSemanticsNodes().size)
        item("chime.ringtones.missing").assertIsSelected().assert(hasText(MISSING))

        item("chime.ringtones.item.${AlarmContract.SOUND_SILENT}").performClick()
        compose.onNodeWithTag("chime.topBar.back").performClick()
        val dialog = "chime.ringtones.discardDialog"
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag(dialog).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("$dialog.cancel").performClick()
        item("chime.ringtones.item.${AlarmContract.SOUND_SILENT}").assertIsSelected()
        compose.onNodeWithTag("chime.topBar.back").performClick()
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag(dialog).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("$dialog.confirm").performClick()
        val result = scenario.result
        assertEquals(Activity.RESULT_CANCELED, result.resultCode)
        assertNull("leaving returns no choice", RingtonePicker().parseResult(result.resultCode, result.resultData))
        scenario.close()
    }

    @Test
    fun theEditPageAndTheSettingsScreenTakeTheChoice() {
        val record = runtime.repository.add(mapOf("hour" to 6, "minute" to 30, "label" to "$PREFIX-edit", "enabled" to false), AlarmContract.SOURCE_TOOL)
        context.startActivity(EditAlarmActivity.edit(context, record.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("chime.edit.content").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("chime.edit.sound").performScrollTo().performClick()
        waitForList()
        item("chime.ringtones.fallback").assertIsSelected()
        item("chime.ringtones.item.${AlarmContract.SOUND_SILENT}").performClick()
        compose.onNodeWithTag("chime.ringtones.confirm").performClick()
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("chime.ringtones.list").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("chime.edit.sound").performScrollTo().assert(hasText(context.getString(R.string.edit_sound_silent), substring = true))
        compose.onNodeWithTag("chime.edit.save").performClick()
        compose.waitUntil(WAIT_MILLIS) { runtime.repository.get(record.id)?.spec?.sound?.source == AlarmContract.SOUND_SILENT }

        val store = RingDefaultsStore(context)
        val next = if (store.get().sound == AlarmContract.SOUND_SILENT) AlarmContract.SOUND_DEFAULT else AlarmContract.SOUND_SILENT
        context.startActivity(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodes(hasTestTag("chime.settings.sound") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("chime.settings.sound").performScrollTo().performClick()
        waitForList()
        item("chime.ringtones.item.$next").performClick()
        compose.onNodeWithTag("chime.ringtones.confirm").performClick()
        compose.waitUntil(WAIT_MILLIS) { store.get().sound == next }
        assertEquals("the other defaults are kept", defaults, store.get().copy(sound = defaults.sound))
    }

    private fun open(request: RingtoneRequest): ActivityScenario<RingtonesActivity> {
        val scenario = ActivityScenario.launchActivityForResult<RingtonesActivity>(RingtonePicker().createIntent(context, request))
        waitForList()
        return scenario
    }

    /** Waits until both lists are read: the import row is enabled then. zh-CN: 等到两个列表都已读取: 此时导入行可用. */
    private fun waitForList() {
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("chime.ringtones.list").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(WAIT_MILLIS) {
            runCatching { compose.onNodeWithTag("chime.ringtones.list").performScrollToNode(hasTestTag("chime.ringtones.import") and isEnabled()) }.isSuccess
        }
    }

    private fun item(tag: String): SemanticsNodeInteraction {
        compose.onNodeWithTag("chime.ringtones.list").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }

    private fun itemTag(ringtone: RingtoneSnapshot) = "chime.ringtones.item.${ringtone.uri}"

    /** A script import of a short WAV with unique content. zh-CN: 以脚本方式导入内容唯一的短 WAV. */
    private fun importProbe(): RingtoneSnapshot =
        runtime.ringtones.import(ByteArrayInputStream(wav(Random.nextInt(1, 1_000_000))), "$PREFIX-${UUID.randomUUID().toString().take(8)}.wav")

    private fun wav(seed: Int): ByteArray {
        val samples = 800
        val data = ByteBuffer.allocate(samples * 2).order(ByteOrder.LITTLE_ENDIAN)
        repeat(samples) { data.putShort(((it * 37 + seed) % 2000 - 1000).toShort()) }
        return ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + samples * 2); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1); putInt(8000); putInt(16000); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(samples * 2); put(data.array())
        }.array()
    }

    private fun finishAll() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.RESUMED, Stage.PAUSED, Stage.STOPPED).flatMap { monitor.getActivitiesInStage(it) }.forEach(Activity::finish)
        }
    }

    companion object {
        private const val PREFIX = "p33-probe"
        private const val MISSING = "content://media/internal/audio/media/2147483647"
        private const val WAIT_MILLIS = 10_000L
    }
}
