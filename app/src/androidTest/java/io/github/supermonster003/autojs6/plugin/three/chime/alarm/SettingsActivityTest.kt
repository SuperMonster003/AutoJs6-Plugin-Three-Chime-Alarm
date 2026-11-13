package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingDefaults
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.holiday.HolidaySettings
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring.RingDefaultsStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.timer.TimerNotifications
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.format.TimeFormat
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.format.TimeFormats
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.holiday.HolidayView
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.settings.SettingsActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.update.AppUpdateStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.update.UpdateFrequency
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The settings screen of the installed plugin (roadmap P3.5, T84): the alarm defaults (vibration, snooze) are written to
 * the device-protected store an alarm reads when it rings, and the time format and the automatic update frequency are
 * saved, and so is the stopwatch notification switch (roadmap P7.4, T122); each row shows the stored value again. With
 * 1.2.0 (roadmap P8.6) the default holiday region chosen from the regions of the data, the weekly holiday check and its
 * metered network switch are saved, and the data status shows the version. The stored values of the device are
 * restored afterwards.
 * zh-CN: 已安装插件的设置页 (路线图 P3.5, T84): 闹钟默认值 (振动, 贪睡) 写入响铃时读取的设备加密存储, 时间格式与自动检查更新的频率得到保存; 各行重新显示
 * 存储值; 秒表通知开关同样得到保存 (路线图 P7.4, T122). 1.2.0 起 (路线图 P8.6) 从数据的地区中选择的默认节假日地区, 每周节假日检查及其计量网络开关得到保存,
 * 数据状态显示版本. 测试结束后恢复设备原有的存储值.
 */
@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {

    // The v2 rule (StandardTestDispatcher) runs what the screens' effects do after a read on Dispatchers.IO on the main
    // thread, as on a device; the v1 rule resumed it on the IO thread, where a state written while the Scaffold content
    // was being subcomposed could be missed and the screen stayed unloaded (CI API 24 / 33, roadmap T106).
    // zh-CN: v2 规则 (StandardTestDispatcher) 让界面副作用在 Dispatchers.IO 读取之后的部分在主线程执行, 与设备上一致; v1 规则让它在 IO 线程继续, 在
    // Scaffold 内容子组合期间写入的状态可能被漏掉, 界面一直停在未加载状态 (CI API 24 / 33, 路线图 T106).
    @get:Rule
    val compose = createEmptyComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var defaults: RingDefaults
    private lateinit var timeFormat: TimeFormat
    private lateinit var frequency: UpdateFrequency
    private var stopwatchShown = true

    /** The raw holiday settings (an absent default region is inferred on first use). zh-CN: 原始节假日设置 (缺省的默认地区在首次使用时推断). */
    private val holidayPreferences
        get() = context.createDeviceProtectedStorageContext().getSharedPreferences(HolidaySettings.PREFERENCES, Context.MODE_PRIVATE)
    private var holidayValues: Map<String, *> = emptyMap<String, Any>()

    @Before
    fun remember() {
        defaults = RingDefaultsStore(context).get()
        timeFormat = TimeFormats.get(context)
        frequency = AppUpdateStore.of(context).frequency
        stopwatchShown = TimerNotifications.stopwatchShown(context)
        holidayValues = HashMap(holidayPreferences.all)
    }

    @After
    fun restore() {
        finishAll()
        RingDefaultsStore(context).set(defaults)
        TimeFormats.set(context, timeFormat)
        AppUpdateStore.of(context).frequency = frequency
        TimerNotifications.setStopwatchShown(context, stopwatchShown)
        val editor = holidayPreferences.edit().clear()
        holidayValues.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Long -> editor.putLong(key, value)
                is Int -> editor.putInt(key, value)
            }
        }
        editor.commit()
        AlarmRuntime.get(context).holidaySettingsChanged()
    }

    @Test
    fun alarmDefaultsAreStoredForTheRing() {
        open()
        val store = RingDefaultsStore(context)

        val vibrate = row("chime.settings.vibrate")
        if (defaults.vibrate) vibrate.assertIsOn() else vibrate.assertIsOff()
        vibrate.performClick()
        compose.waitUntil(WAIT_MILLIS) { store.get().vibrate != defaults.vibrate }
        if (defaults.vibrate) row("chime.settings.vibrate").assertIsOff() else row("chime.settings.vibrate").assertIsOn()
        waitForSnackbarGone()

        row("chime.settings.snooze").performClick()
        val dialog = "chime.settings.snoozeDialog"
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag(dialog).fetchSemanticsNodes().isNotEmpty() }
        // The settings screen has no "Default" choice: off (1) or custom (2). zh-CN: 设置页没有 "默认" 选项: 关闭 (1) 或自定 (2).
        compose.onNodeWithTag(if (defaults.snoozeEnabled) "$dialog.1" else "$dialog.2").performClick()
        compose.onNodeWithTag("$dialog.confirm").performClick()
        compose.waitUntil(WAIT_MILLIS) { store.get().snoozeEnabled != defaults.snoozeEnabled }
        val stored = store.get()
        assertEquals(!defaults.vibrate, stored.vibrate)
        assertEquals(defaults.snoozeMinutes, stored.snoozeMinutes)
        assertEquals(defaults.snoozeMax, stored.snoozeMax)
        assertEquals(defaults.sound, stored.sound)
        assertEquals(defaults.autoSilenceMinutes, stored.autoSilenceMinutes)
    }

    @Test
    fun timeFormatAndUpdateFrequencyAreSaved() {
        open()

        val format = TimeFormat.entries.first { it != timeFormat }
        row("chime.settings.timeFormat").performClick()
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("chime.dialog.timeFormat").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("chime.dialog.timeFormat.${format.ordinal}").performClick()
        compose.onNodeWithTag("chime.dialog.timeFormat.confirm").performClick()
        compose.waitUntil(WAIT_MILLIS) { TimeFormats.get(context) == format }
        waitForSnackbarGone()

        val next = if (frequency == UpdateFrequency.WEEKLY) UpdateFrequency.DAILY else UpdateFrequency.WEEKLY
        row("chime.settings.frequency").performClick()
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("chime.dialog.frequency").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("chime.dialog.frequency.${next.ordinal}").performClick()
        compose.onNodeWithTag("chime.dialog.frequency.confirm").performClick()
        compose.waitUntil(WAIT_MILLIS) { AppUpdateStore.of(context).frequency == next }
        assertTrue("the metered switch is enabled while checks are on", compose.onAllNodes(hasTestTag("chime.settings.metered") and isEnabled()).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun theStopwatchNotificationSwitchIsSaved() {
        open()
        val tag = "chime.settings.stopwatchNotification"
        if (stopwatchShown) row(tag).assertIsOn() else row(tag).assertIsOff()
        row(tag).performClick()
        compose.waitUntil(WAIT_MILLIS) { TimerNotifications.stopwatchShown(context) != stopwatchShown }
        if (stopwatchShown) row(tag).assertIsOff() else row(tag).assertIsOn()
        waitForSnackbarGone()
        row(tag).performClick()
        compose.waitUntil(WAIT_MILLIS) { TimerNotifications.stopwatchShown(context) == stopwatchShown }
    }

    @Test
    fun holidaySettingsAreSaved() {
        open()
        compose.waitUntil(WAIT_MILLIS) { compose.shows(hasTestTag("chime.settings.holidayRegion") and isEnabled()) }
        val view = HolidayView.load(context)
        assertNotNull("the holiday data opens", view)
        val current = view!!.region(null)?.code
        val target = if (current == null) "JP" else null

        row("chime.settings.holidayRegion").performClick()
        val dialog = "chime.dialog.holidayRegion"
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag(dialog).fetchSemanticsNodes().isNotEmpty() }
        if (target == null) {
            compose.onNodeWithTag("$dialog.first").performClick()
        } else {
            compose.onNodeWithTag("$dialog.search").performTextInput(target)
            compose.onNodeWithTag("$dialog.region.$target").performClick()
        }
        compose.onNodeWithTag("$dialog.confirm").performClick()
        compose.waitUntil(WAIT_MILLIS) { holidayPreferences.getString(HolidaySettings.KEY_DEFAULT_REGION, null) == target.orEmpty() }
        val none = context.getString(R.string.settings_holiday_region_none)
        compose.waitUntil(WAIT_MILLIS) {
            val summary = compose.onNodeWithTag("chime.settings.holidayRegion").fetchSemanticsNode().config.getOrElse(SemanticsProperties.Text) { emptyList() }
                .joinToString("\n") { it.text }
            if (target == null) summary.contains(none) else !summary.contains(none)
        }
        waitForSnackbarGone()

        val settings = AlarmRuntime.get(context).holidaySettings
        val auto = settings.autoUpdate
        row("chime.settings.holidayAutoUpdate").performClick()
        compose.waitUntil(WAIT_MILLIS) { settings.autoUpdate != auto }
        val metered = compose.onAllNodes(hasTestTag("chime.settings.holidayMetered") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        assertEquals("the metered switch follows the weekly check", !auto, metered)
        waitForSnackbarGone()
        if (auto) assertFalse(settings.autoUpdate)

        row("chime.settings.holidayCheck").assert(hasText(view.info.version, substring = true))
    }

    private fun open() {
        context.startActivity(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        // The default rows are enabled once the stored values are read. zh-CN: 读到存储值后默认值各行才可用.
        compose.waitUntil(WAIT_MILLIS) { compose.shows(hasTestTag("chime.settings.snooze") and isEnabled()) }
    }

    private fun row(tag: String): SemanticsNodeInteraction = compose.onNodeWithTag(tag).performScrollTo()

    /**
     * Waits until the "Saved" snackbar is gone: on a landscape tablet it covers the row scrolled to the bottom edge, and a
     * click there lands on the snackbar (found on the Xiaomi Pad).
     * zh-CN: 等到 "已保存" 提示消失: 横屏平板上它会盖住滚到底边的行, 点击会落在提示上 (在小米平板上发现).
     */
    private fun waitForSnackbarGone() {
        val saved = context.getString(R.string.settings_saved)
        compose.waitUntil(SNACKBAR_MILLIS) { compose.onAllNodesWithText(saved).fetchSemanticsNodes().isEmpty() }
    }

    private fun finishAll() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.RESUMED, Stage.PAUSED, Stage.STOPPED).flatMap { monitor.getActivitiesInStage(it) }.forEach(Activity::finish)
        }
    }

    companion object {
        private const val WAIT_MILLIS = 10_000L
        private const val SNACKBAR_MILLIS = 20_000L
    }
}
