package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.OpenPluginActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.HomeActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions.PermissionsActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.settings.SettingsActivity
import org.autojs.plugin.alarm.api.AlarmPluginActions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The `alarms.openPlugin(page)` trampoline of the installed plugin (roadmap T81): the frozen OPEN action resolves to
 * [OpenPluginActivity] alone, without a permission and without a task affinity, and every page opens its screen:
 * `permissions` the permissions page (P3.4), `settings` the settings screen (P3.5); the 1.1.0 pages and unknown values
 * open the alarm list.
 * zh-CN: 已安装插件的 `alarms.openPlugin(page)` 跳板 (路线图 T81): 冻结的 OPEN action 只解析到 [OpenPluginActivity], 不要求权限, 没有任务
 * 亲和性, 每个页面打开对应界面: `permissions` 打开权限页 (P3.4), `settings` 打开设置页 (P3.5); 1.1.0 的页面与未知值打开闹钟列表.
 */
@RunWith(AndroidJUnit4::class)
class OpenPluginActivityTest {

    private val instrumentation: Instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val context: Context
        get() = instrumentation.targetContext

    private val monitors = ArrayList<Instrumentation.ActivityMonitor>()

    @After
    fun cleanUp() {
        monitors.forEach { instrumentation.removeMonitor(it) }
    }

    @Test
    fun theOpenActionResolvesToTheTrampolineWithoutAPermission() {
        val intent = Intent(AlarmPluginActions.OPEN).setPackage(context.packageName)
        val matches = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        assertEquals(listOf(OpenPluginActivity::class.java.name), matches.map { it.activityInfo.name })
        val info = matches.single().activityInfo
        assertTrue("exported", info.exported)
        assertNull("no permission", info.permission)
        assertNull("no task affinity", info.taskAffinity)
    }

    /**
     * Every call reaches the trampoline with its page while the alarm list stays open, as when a script calls
     * `alarms.openPlugin('settings')` after `alarms.openPlugin()`: with the default task affinity the trampoline was the
     * root of the plugin's task, so the second call only brought that task to the front (found on XQ-AT72, P2 wrap-up).
     * zh-CN: 闹钟列表保持打开时, 每次调用都以其页面到达跳板 (如脚本在 `alarms.openPlugin()` 之后调用 `alarms.openPlugin('settings')`): 使用默认任务
     * 亲和性时跳板是插件任务的根, 第二次调用只会把该任务带到前台 (P2 收尾时在 XQ-AT72 上发现).
     */
    @Test
    fun everyPageReachesTheTrampolineAndOpensItsScreen() {
        var list: Activity? = null
        try {
            for (page in listOf(null) + AlarmPluginActions.PAGES.toList() + "unknown-page") {
                val trampoline = instrumentation.addMonitor(OpenPluginActivity::class.java.name, null, false).also { monitors += it }
                val home = instrumentation.addMonitor(HomeActivity::class.java.name, null, false).also { monitors += it }
                val permissions = instrumentation.addMonitor(PermissionsActivity::class.java.name, null, false).also { monitors += it }
                val settings = instrumentation.addMonitor(SettingsActivity::class.java.name, null, false).also { monitors += it }
                val intent = Intent(AlarmPluginActions.OPEN).setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                page?.let { intent.putExtra(AlarmPluginActions.EXTRA_PAGE, it) }
                context.startActivity(intent)
                val opened = instrumentation.waitForMonitorWithTimeout(trampoline, WAIT_MILLIS)
                assertNotNull("page $page reached the trampoline", opened)
                assertEquals(page, opened.intent.getStringExtra(AlarmPluginActions.EXTRA_PAGE))
                if (page == AlarmPluginActions.PAGE_PERMISSIONS) {
                    val screen = instrumentation.waitForMonitorWithTimeout(permissions, WAIT_MILLIS)
                    assertNotNull("page $page opened the permissions page", screen)
                    assertTrue("page $page resumed the permissions page", waitUntil { resumed().any { it is PermissionsActivity } })
                    close(screen)
                } else if (page == AlarmPluginActions.PAGE_SETTINGS) {
                    val screen = instrumentation.waitForMonitorWithTimeout(settings, WAIT_MILLIS)
                    assertNotNull("page $page opened the settings screen", screen)
                    assertTrue("page $page resumed the settings screen", waitUntil { resumed().any { it is SettingsActivity } })
                    close(screen)
                } else {
                    if (list == null) list = instrumentation.waitForMonitorWithTimeout(home, WAIT_MILLIS)
                    assertNotNull("page $page opened the alarm list", list)
                    assertTrue("page $page resumed the alarm list", waitUntil { resumed().any { it is HomeActivity } })
                }
                listOf(trampoline, home, permissions, settings).forEach { instrumentation.removeMonitor(it); monitors -= it }
            }
        } finally {
            list?.let(::close)
        }
    }

    private fun resumed(): Collection<Activity> {
        var activities: Collection<Activity> = emptyList()
        instrumentation.runOnMainSync { activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).toList() }
        return activities
    }

    private fun waitUntil(condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + WAIT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return true
            SystemClock.sleep(50)
        }
        return condition()
    }

    private fun close(activity: Activity) {
        activity.finish()
        assertTrue("${activity.javaClass.simpleName} closed", waitUntil { instrumentation.waitForIdleSync(); activity.isDestroyed })
    }

    companion object {
        private const val WAIT_MILLIS = 10_000L
    }
}
