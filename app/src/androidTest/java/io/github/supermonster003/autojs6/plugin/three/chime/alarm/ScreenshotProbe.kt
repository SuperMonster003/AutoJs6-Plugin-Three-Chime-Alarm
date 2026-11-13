package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.app.Activity
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring.RingActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring.RingService
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.appearance.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.appearance.HostAppearanceReader
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.EditAlarmActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.HomeActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.HomeIntents
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.HomePage
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.ringtones.RingtonePicker
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.ringtones.RingtoneRequest
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.settings.SettingsActivity
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.TimerSpec
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The screenshots of roadmap P3.6 and of the E4 checks of P3.1 - P3.5, run by `.python/device_screens.py`: adds a few
 * sample alarms (labels in Chinese or English after the plugin language), then for each night mode named by
 * `chimeModes` (`light,dark` by default) captures the pages named by `chimeScreens` (`pages` for the list, the editor,
 * the ringtones and the settings page; `clock` for the timers page, its custom duration dialog and the stopwatch page of
 * 1.1.0 (roadmap P7.4), with sample timers (running, paused, ready) and, while the stopwatch is idle, four sample laps;
 * `notice` for the ring notification shown while the device is in use, once in the system theme; `ring` for the ringing
 * screen; `all` for all four; `holidays`, not part of `all`, for the holiday screens of 1.2.0 (roadmap P8.6): the list
 * with the holiday data card, the editor of a holiday alarm and its holiday rows, the holiday mode dialog, the settings
 * page at its holiday rows and the region dialog, with their own sample alarms) with `screencap` into [DEVICE_DIR] as
 * `<page>-<mode>.png`. The ringing screen is captured on the lock screen: the screen is turned off
 * before a silent sample alarm rings, so the full-screen intent opens it there; when it does not show within
 * [RING_SHOW_MILLIS] the screen is started directly and marked `direct` (`chimeRingDirectSeconds` waits longer for it,
 * for example while the maintainer answers a lock screen prompt of the system). The plugin night mode is restored and the
 * sample alarms and timers are removed and a stopwatch the probe started is reset at the end, also when a step fails. Without the argument the test is skipped, so
 * `connectedDebugAndroidTest` neither changes the night mode nor turns the screen off.
 * zh-CN: 路线图 P3.6 与 P3.1 - P3.5 的 E4 核对所用截图, 由 `.python/device_screens.py` 运行: 添加几个示例闹钟 (标签按插件语言取中文或英文), 然后
 * 对 `chimeModes` 所列的每种夜间模式 (默认 `light,dark`), 用 `screencap` 把 `chimeScreens` 所列的页面 (`pages` 为列表, 编辑页, 铃声页与设置页;
 * `clock` 为 1.1.0 的计时器页, 其自定义时长对话框与秒表页 (路线图 P7.4), 带示例计时器 (运行, 暂停, 就绪), 秒表空闲时另计四个示例圈; `notice` 为设备使用中时的响铃通知,
 * 按系统主题截一次; `ring` 为响铃界面; `all` 为四者; `holidays` 不属于 `all`, 为 1.2.0 的节假日界面 (路线图 P8.6): 带节假日数据卡片的列表, 节假日闹钟的编辑页
 * 及其节假日行, 节假日模式对话框, 停在节假日各行的设置页与地区对话框, 使用单独的示例闹钟) 存到 [DEVICE_DIR], 文件名 `<页面>-<模式>.png`. 响铃界面在锁屏上截取: 静音示例闹钟响铃前先关屏, 由全屏 Intent
 * 在锁屏上打开它; [RING_SHOW_MILLIS] 内未出现时直接启动并标为 `direct` (`chimeRingDirectSeconds` 可延长对它的等待, 例如维护者应答系统的锁屏提示时). 结束时 (含中途失败) 恢复插件夜间模式, 删除示例闹钟与计时器, 并重置探针启动的秒表. 没有该参数时跳过, 因此
 * `connectedDebugAndroidTest` 不会改夜间模式, 也不会关屏.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotProbe {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val runtime: AlarmRuntime get() = AlarmRuntime.get(context)
    private val appearance = AppearancePreferences.file(context)
    private val added = mutableListOf<String>()
    private val timers = mutableListOf<String>()
    private var stopwatchStarted = false

    @Test
    fun capture() {
        val screens = InstrumentationRegistry.getArguments().getString(ARGUMENT)
        assumeTrue("run by .python/device_screens.py", screens != null)
        val modes = (InstrumentationRegistry.getArguments().getString(ARGUMENT_MODES) ?: "light,dark").split(',').filter { it.isNotBlank() }
        val out = Bundle()
        val previous = appearance.getString(AppearancePreferences.KEY_DARK_MODE, null)
        try {
            shell("mkdir -p $DEVICE_DIR")
            val chinese = AppearancePreferences.resolve(context, HostAppearanceReader.read(context)).language.startsWith("zh")
            out.putString("chime.language", if (chinese) "zh" else "en")
            out.putString("chime.others", runtime.repository.list().size.toString())
            val main = if (screens == HOLIDAYS) holidaySamples(chinese) else samples(chinese)
            if (screens == "clock" || screens == "all") clockSamples(chinese, out)
            for (mode in modes) {
                setNightMode(mode)
                if (screens == "pages" || screens == "all") pages(mode, main, out)
                if (screens == "clock" || screens == "all") clock(mode, out)
                if (screens == HOLIDAYS) holidays(mode, main, out)
            }
            if (screens == "notice" || screens == "ring" || screens == "all") {
                // `am instrument` leaves the plugin stopped; starting an activity first lets the alarm be delivered.
                // zh-CN: `am instrument` 会让插件处于停止状态; 先启动一个 Activity, 闹钟才能送达.
                // Behind the keyguard the list never shows; the start alone is enough. zh-CN: 锁屏时列表不会显示, 只启动即可.
                if (context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
                    context.startActivity(Intent(context, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    SystemClock.sleep(SETTLE_MILLIS)
                } else {
                    show(Intent(context, HomeActivity::class.java), HOME_TAGS)
                }
                finishAll()
            }
            if (screens == "notice" || screens == "all") notice(chinese, out)
            if (screens == "ring" || screens == "all") {
                for (mode in modes) {
                    setNightMode(mode)
                    ring(mode, chinese, out)
                }
            }
        } catch (error: Throwable) {
            out.putString("chime.error", "${error.javaClass.simpleName}: ${error.message.orEmpty().take(160)}")
        } finally {
            finishAll()
            added.forEach { id ->
                if (runtime.rings.ringing().any { it.id == id }) runtime.rings.dismiss(id, AlarmContract.BY_USER)
                runtime.repository.remove(id, AlarmContract.SOURCE_TOOL)
            }
            timers.forEach { id -> runCatching { runtime.userTimerApi.cancel(id) } }
            if (stopwatchStarted) runtime.stopwatch.reset()
            appearance.edit().apply { if (previous == null) remove(AppearancePreferences.KEY_DARK_MODE) else putString(AppearancePreferences.KEY_DARK_MODE, previous) }.commit()
            out.putString("chime.restored", (appearance.getString(AppearancePreferences.KEY_DARK_MODE, null) == previous).toString())
            instrumentation.sendStatus(0, out)
        }
    }

    /** Adds the sample alarms and returns the one the editor shows. zh-CN: 添加示例闹钟, 返回编辑页展示的那一个. */
    private fun samples(chinese: Boolean): String {
        fun text(zh: String, en: String) = if (chinese) zh else en
        val tomorrow = LocalDate.now().plusDays(1).atTime(LocalTime.of(14, 0)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val main = add(
            mapOf(
                "hour" to 6, "minute" to 45, "repeat" to "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", "label" to text("起床", "Wake up"),
                "snooze" to mapOf("minutes" to 10, "max" to 3), "speak" to mapOf("text" to text("早上好, 该起床了", "Good morning, time to get up")),
            ),
            AlarmContract.SOURCE_USER,
        )
        add(mapOf("hour" to 8, "minute" to 30, "repeat" to "FREQ=DAILY", "label" to text("吃药", "Take medicine")), AlarmContract.SOURCE_SCRIPT)
        add(mapOf("at" to tomorrow, "label" to text("项目会议", "Project meeting")), AlarmContract.SOURCE_TOOL)
        add(mapOf("hour" to 22, "minute" to 30, "repeat" to "FREQ=WEEKLY;BYDAY=SA,SU", "label" to text("周末阅读", "Weekend reading"), "enabled" to false), AlarmContract.SOURCE_USER)
        return main
    }

    /**
     * The holiday sample alarms (roadmap P8.6), all in the bundled CN data: workdays, daily skipping holidays from New
     * Year's Day of next year (a year the data estimates, T162, so the editor shows the hint), and daily on holidays only.
     * Returns the one the editor shows.
     * zh-CN: 节假日示例闹钟 (路线图 P8.6), 都使用内置 CN 数据: 工作日; 自明年元旦起每天并跳过节假日 (数据推算的年份, T162, 编辑页因此显示提示); 每天但只在节假日.
     * 返回编辑页展示的那一个.
     */
    private fun holidaySamples(chinese: Boolean): String {
        fun text(zh: String, en: String) = if (chinese) zh else en
        fun filter(mode: String) = mapOf("region" to "CN", "mode" to mode)
        add(mapOf("hour" to 7, "minute" to 30, "label" to text("上班", "Work"), "holidays" to filter(AlarmContract.HOLIDAY_MODE_WORKDAYS)), AlarmContract.SOURCE_USER)
        val main = add(
            mapOf(
                "hour" to 8, "minute" to 0, "date" to LocalDate.now().plusYears(1).withDayOfYear(1).toString(), "repeat" to "FREQ=DAILY",
                "label" to text("晨跑", "Morning run"), "holidays" to filter(AlarmContract.HOLIDAY_MODE_SKIP),
            ),
            AlarmContract.SOURCE_SCRIPT,
        )
        add(mapOf("hour" to 9, "minute" to 0, "repeat" to "FREQ=DAILY", "label" to text("节日问候", "Holiday greeting"), "holidays" to filter(AlarmContract.HOLIDAY_MODE_ONLY)), AlarmContract.SOURCE_TOOL)
        return main
    }

    private fun holidays(mode: String, main: String, out: Bundle) {
        shot("holidays-list-$mode", Intent(context, HomeActivity::class.java), listOf("chime.home.holidays"), out)
        shot("holidays-edit-$mode", EditAlarmActivity.edit(context, main), listOf("chime.edit.estimated", "chime.edit.noRegion"), out)
        shot("holidays-edit-rows-$mode", EditAlarmActivity.edit(context, main), listOf("chime.edit.content"), out, "chime.edit.holidayRegion") {
            compose.onNodeWithTag("chime.edit.holidayRegion").performScrollTo()
        }
        shot("holidays-mode-$mode", EditAlarmActivity.edit(context, main), listOf("chime.edit.content"), out) {
            compose.onNodeWithTag("chime.edit.holidays").performScrollTo().performClick()
            compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("chime.edit.holidaysDialog").fetchSemanticsNodes().isNotEmpty() }
        }
        shot("holidays-settings-$mode", Intent(context, SettingsActivity::class.java), listOf("chime.settings"), out, "chime.settings.holidayRegion") {
            compose.onNodeWithTag("chime.settings.holidayCheck").performScrollTo()
        }
        shot("holidays-region-$mode", Intent(context, SettingsActivity::class.java), listOf("chime.settings"), out, "chime.settings.holidayRegion") {
            compose.onNodeWithTag("chime.settings.holidayRegion").performScrollTo().performClick()
            compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("chime.dialog.holidayRegion").fetchSemanticsNodes().isNotEmpty() }
        }
    }

    private fun add(options: Map<String, Any>, source: String): String = runtime.repository.add(options, source).id.also { added += it }

    private fun pages(mode: String, main: String, out: Bundle) {
        shot("list-$mode", Intent(context, HomeActivity::class.java), HOME_TAGS, out)
        shot("edit-$mode", EditAlarmActivity.edit(context, main), listOf("chime.edit.content"), out)
        // Both pages read the ringtones in the background and keep their rows disabled until then.
        // zh-CN: 两个页面都在后台读取铃声, 读完之前相关行保持禁用.
        shot("ringtones-$mode", RingtonePicker().createIntent(context, RingtoneRequest(null, AlarmContract.SOUND_DEFAULT)), listOf("chime.ringtones.list"), out, "chime.ringtones.import")
        shot("settings-$mode", Intent(context, SettingsActivity::class.java), listOf("chime.settings"), out, "chime.settings.vibrate")
    }

    /**
     * The sample timers (newest first on the page: a running one, a ready one at its whole length, one paused part way)
     * and, while the stopwatch is idle, four laps of differing lengths on a running stopwatch.
     * zh-CN: 示例计时器 (页面上从新到旧: 运行中, 停在完整时长的就绪, 中途暂停) 与秒表空闲时运行中秒表上的四个长度不同的圈.
     */
    private fun clockSamples(chinese: Boolean, out: Bundle) {
        fun text(zh: String, en: String) = if (chinese) zh else en
        fun start(minutes: Long, label: String) = runtime.userTimerApi.start(minutes * 60_000L, TimerSpec(label = label)).id.also { timers += it }
        out.putString("chime.timers.others", runtime.timers.list().size.toString())
        val nap = start(20, text("午休", "Nap"))
        runtime.timers.reset(start(5, text("煮鸡蛋", "Boil eggs")))
        if (runtime.stopwatch.state().state == AlarmContract.STOPWATCH_IDLE) {
            stopwatchStarted = true
            runtime.stopwatch.start()
            for (pause in listOf(1_300L, 2_100L, 900L, 1_600L)) {
                SystemClock.sleep(pause)
                runtime.stopwatch.lap()
            }
        }
        out.putString("chime.stopwatch.sample", stopwatchStarted.toString())
        SystemClock.sleep(SETTLE_MILLIS)
        runtime.timers.pause(nap)
        start(25, text("泡茶", "Tea"))
    }

    private fun clock(mode: String, out: Bundle) {
        shot("timers-$mode", HomeIntents.of(context, HomePage.TIMERS), listOf("chime.timers.list"), out, "chime.timers.add")
        shot("timers-dialog-$mode", HomeIntents.of(context, HomePage.TIMERS, customTimer = true), listOf("chime.timers.customDialog"), out)
        shot("stopwatch-$mode", HomeIntents.of(context, HomePage.STOPWATCH), listOf("chime.stopwatch.time"), out)
    }

    /** Shows [intent], runs [action] (a scroll or a dialog) when given, and captures the screen. zh-CN: 显示 [intent], 给出 [action] (滚动或打开对话框) 时先执行, 然后截屏. */
    private fun shot(name: String, intent: Intent, tags: List<String>, out: Bundle, ready: String? = null, action: (() -> Unit)? = null) {
        out.putString(
            "chime.shot.$name",
            runCatching {
                val started = SystemClock.uptimeMillis()
                show(intent, tags, ready)
                if (ready != null) out.putString("chime.ready.$name", (SystemClock.uptimeMillis() - started - SETTLE_MILLIS).toString())
                if (action != null) {
                    action()
                    compose.waitForIdle()
                    SystemClock.sleep(SETTLE_MILLIS)
                }
                capture(name)
            }.getOrElse { "error:${it.javaClass.simpleName}: ${it.message.orEmpty().take(120)}" },
        )
        finishAll()
    }

    /**
     * Turns the screen off, lets a silent sample alarm ring and captures the ringing screen on the lock screen, then ends
     * the round. zh-CN: 关屏, 让静音示例闹钟响铃, 在锁屏上截取响铃界面, 然后结束这次响铃.
     */
    private fun ring(mode: String, chinese: Boolean, out: Bundle) {
        val name = "ring-$mode"
        val at = (System.currentTimeMillis() / 1000 + RING_SECONDS) * 1000
        val id = add(
            mapOf(
                "at" to at, "label" to if (chinese) "起床" else "Wake up", "snooze" to mapOf("minutes" to 10, "max" to 3),
                "sound" to mapOf("source" to AlarmContract.SOUND_SILENT), "vibrate" to false,
            ),
            AlarmContract.SOURCE_USER,
        )
        try {
            shell("input keyevent KEYCODE_SLEEP")
            waitFor("the sample alarm rings", RING_WAIT_MILLIS) { runtime.rings.sounding?.alarmId == id }
            // Whether the plugin may use a full-screen intent and whether the posted ring notification carries one.
            // zh-CN: 插件能否使用全屏 Intent, 以及已发出的响铃通知是否带有它.
            val manager = context.getSystemService(NotificationManager::class.java)
            within(WAIT_MILLIS) { manager.activeNotifications.any { it.notification.channelId == ThreeChimeAlarmApplication.CHANNEL_RING } }
            out.putString("chime.fullScreen.$mode", RingService.fullScreenAllowed(context).toString())
            out.putString(
                "chime.fullScreenIntent.$mode",
                manager.activeNotifications.filter { it.notification.channelId == ThreeChimeAlarmApplication.CHANNEL_RING }
                    .any { it.notification.fullScreenIntent != null }.toString(),
            )
            SystemClock.sleep(SETTLE_MILLIS)
            out.putString("chime.record.$mode", ringRecord())
            var how = "lock"
            if (!within(RING_SHOW_MILLIS) { resumedRingScreen() != null }) {
                how = "direct"
                shell("input keyevent KEYCODE_WAKEUP")
                context.startActivity(Intent(context, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                if (!within(PROMPT_MILLIS) { resumedRingScreen() != null }) {
                    // A system prompt in front of the lock screen (ColorOS asks whether the app may show there).
                    // zh-CN: 锁屏前的系统提示 (ColorOS 询问是否允许该应用在锁屏上显示).
                    out.putString("chime.shot.prompt-$mode", capture("prompt-$mode"))
                }
                val seconds = InstrumentationRegistry.getArguments().getString(ARGUMENT_DIRECT_SECONDS)?.toLongOrNull()
                waitFor("the ring screen resumes", seconds?.times(1000)?.coerceAtLeast(WAIT_MILLIS) ?: WAIT_MILLIS) { resumedRingScreen() != null }
            }
            compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag(RingActivity.TAG_LABEL).fetchSemanticsNodes().isNotEmpty() }
            SystemClock.sleep(SETTLE_MILLIS)
            out.putString("chime.shot.$name", capture(name))
            out.putString("chime.ring.$mode", how)
        } catch (error: Throwable) {
            out.putString("chime.shot.$name", "error:${error.javaClass.simpleName}: ${error.message.orEmpty().take(120)}")
        } finally {
            runtime.rings.dismiss(id, AlarmContract.BY_USER)
            within(WAIT_MILLIS) { resumedRingScreen() == null }
        }
    }

    /**
     * Lets a silent sample alarm ring while the device is in use on its home screen: the system shows the ring
     * notification as a heads-up (small icon, time, snooze and dismiss actions) instead of the ring screen; captures it
     * and the expanded shade in the system theme, then ends the round.
     * zh-CN: 设备在桌面上使用时让静音示例闹钟响铃: 系统以浮动通知 (小图标, 时间, 贪睡与关闭动作) 代替响铃界面; 截取它与展开的通知栏 (系统主题),
     * 然后结束这次响铃.
     */
    private fun notice(chinese: Boolean, out: Bundle) {
        shell("input keyevent KEYCODE_HOME")
        val at = (System.currentTimeMillis() / 1000 + RING_SECONDS) * 1000
        val id = add(
            mapOf(
                "at" to at, "label" to if (chinese) "起床" else "Wake up", "snooze" to mapOf("minutes" to 10, "max" to 3),
                "sound" to mapOf("source" to AlarmContract.SOUND_SILENT), "vibrate" to false,
            ),
            AlarmContract.SOURCE_USER,
        )
        try {
            waitFor("the sample alarm rings", RING_WAIT_MILLIS) { runtime.rings.sounding?.alarmId == id }
            SystemClock.sleep(NOTICE_MILLIS)
            out.putString("chime.notice.screen", (resumedRingScreen() != null).toString())
            out.putString("chime.shot.notice-system", capture("notice-system"))
            shell("cmd statusbar expand-notifications")
            SystemClock.sleep(NOTICE_MILLIS)
            out.putString("chime.shot.shade-system", capture("shade-system"))
        } catch (error: Throwable) {
            out.putString("chime.shot.notice-system", "error:${error.javaClass.simpleName}: ${error.message.orEmpty().take(120)}")
        } finally {
            shell("cmd statusbar collapse")
            runtime.rings.dismiss(id, AlarmContract.BY_USER)
            within(WAIT_MILLIS) { resumedRingScreen() == null }
            finishAll()
        }
    }

    private fun setNightMode(mode: String) {
        require(mode == AppearancePreferences.LIGHT || mode == AppearancePreferences.DARK) { "night mode $mode" }
        appearance.edit().putString(AppearancePreferences.KEY_DARK_MODE, mode).commit()
    }

    /** Starts [intent], waits for one of [tags] and, when given, for the [ready] node to be enabled. zh-CN: 启动 [intent], 等待 [tags] 之一出现, 给出 [ready] 时再等该节点变为可用. */
    private fun show(intent: Intent, tags: List<String>, ready: String? = null) {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        compose.waitUntil(WAIT_MILLIS) { tags.any { compose.onAllNodesWithTag(it).fetchSemanticsNodes().isNotEmpty() } }
        if (ready != null) {
            compose.waitUntil(READY_MILLIS) {
                compose.onAllNodesWithTag(ready).fetchSemanticsNodes().any { SemanticsProperties.Disabled !in it.config }
            }
        }
        compose.waitForIdle()
        SystemClock.sleep(SETTLE_MILLIS)
    }

    private fun capture(name: String): String {
        val path = "$DEVICE_DIR/$name.png"
        shell("screencap -p $path")
        return path
    }

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { String(it.readBytes()) }

    /**
     * The system record of the ring notification (`dumpsys notification`): its ranked importance, the reason and any
     * assistant adjustments, as one line. Some systems lower the importance after posting, which stops the full-screen intent.
     * zh-CN: 响铃通知的系统记录 (`dumpsys notification`): 排序后的重要性, 原因与助手调整, 合成一行. 部分系统在发出后调低重要性, 全屏 Intent 因此不触发.
     */
    private fun ringRecord(): String {
        val lines = shell("dumpsys notification --noredact").lines()
        val start = lines.indexOfFirst { "NotificationRecord(" in it && "pkg=${context.packageName} " in it && " id=1 " in it }
        if (start < 0) return "none"
        val end = (start + 1 until lines.size).firstOrNull { "NotificationRecord(" in lines[it] } ?: lines.size
        val fields = Regex("\\b(\\w*(?:[Ii]mportance|Explanation|ShowBanner|[Aa]djustments)\\w*)=([^,|\\s}]+)")
            .findAll(lines.subList(start, end).joinToString("\n"))
            .map { "${it.groupValues[1]}=${it.groupValues[2]}" }.distinct().toList()
        return fields.joinToString(" ").take(900)
    }

    private fun finishAll() {
        instrumentation.runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.RESUMED, Stage.PAUSED, Stage.STOPPED).flatMap { monitor.getActivitiesInStage(it) }.forEach(Activity::finish)
        }
        SystemClock.sleep(500)
    }

    private fun resumedRingScreen(): RingActivity? {
        var found: RingActivity? = null
        instrumentation.runOnMainSync {
            found = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<RingActivity>().firstOrNull()
        }
        return found
    }

    private fun within(millis: Long, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + millis
        while (!condition()) {
            if (SystemClock.uptimeMillis() > deadline) return false
            SystemClock.sleep(200)
        }
        return true
    }

    private fun waitFor(what: String, millis: Long, condition: () -> Boolean) {
        if (!within(millis, condition)) throw AssertionError("$what: not within $millis ms")
    }

    companion object {
        const val ARGUMENT = "chimeScreens"
        const val ARGUMENT_MODES = "chimeModes"
        const val ARGUMENT_DIRECT_SECONDS = "chimeRingDirectSeconds"
        const val DEVICE_DIR = "/data/local/tmp/chime-screens"
        const val HOLIDAYS = "holidays"
        private val HOME_TAGS = listOf("chime.home.list", "chime.emptyState")
        private const val WAIT_MILLIS = 10_000L
        private const val SETTLE_MILLIS = 1_500L
        private const val READY_MILLIS = 30_000L
        private const val NOTICE_MILLIS = 2_500L
        private const val RING_SECONDS = 12
        private const val RING_WAIT_MILLIS = 90_000L
        private const val RING_SHOW_MILLIS = 15_000L
        private const val PROMPT_MILLIS = 4_000L
    }
}
