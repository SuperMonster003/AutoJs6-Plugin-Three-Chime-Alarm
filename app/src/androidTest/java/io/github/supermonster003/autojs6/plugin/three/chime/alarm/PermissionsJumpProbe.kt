package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.HomeActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions.PermissionRow
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions.Permissions
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions.PermissionsActivity
import org.autojs.plugin.alarm.api.AlarmPluginActions
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The device walk of the permissions page (roadmap P3.4), run by `.python/device_permissions.py`: reports every row's
 * state, checks that the alarm list shows its banner exactly while a row is missing and that the banner opens the page,
 * opens the page through the OPEN action as `alarms.openPlugin('permissions')` does, then taps each
 * tappable row named by the `chimePermissions` argument (`all` for every one) and reports the page it opened (the
 * component on top and the page the row chose, with fallbacks to the app details marked), returning to the page in
 * between. The notification row is skipped while it would show the runtime permission dialog, which only the
 * maintainer answers. Without the argument the test is skipped, so `connectedDebugAndroidTest` opens no system page.
 * zh-CN: 权限页的设备巡检 (路线图 P3.4), 由 `.python/device_permissions.py` 运行: 报告每行的状态, 检查闹钟列表恰在有缺失行时显示横幅且横幅打开权限页,
 * 像 `alarms.openPlugin('permissions')` 那样经 OPEN action 打开权限页, 然后点按 `chimePermissions` 参数所列的每个可点按行 (`all` 为全部), 报告它打开的页面 (位于顶部的组件与该行选择的页面, 退回应用详情的
 * 会标出), 中间回到权限页. 通知一行在会弹出运行时权限对话框时跳过, 该对话框只由维护者作答. 没有该参数时跳过, 因此 `connectedDebugAndroidTest` 不会打开
 * 系统页面.
 */
@RunWith(AndroidJUnit4::class)
class PermissionsJumpProbe {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun walk() {
        val argument = InstrumentationRegistry.getArguments().getString(ARGUMENT)
        assumeTrue("run by .python/device_permissions.py", argument != null)
        val out = Bundle()
        val snapshot = Permissions.read(context)
        out.putString("chime.sdk", snapshot.sdk.toString())
        out.putString("chime.rows", snapshot.rows.joinToString(",") { it.tag })
        out.putString("chime.missing", snapshot.missing.joinToString(",") { it.tag })
        snapshot.rows.forEach { row ->
            out.putString("chime.state.${row.tag}", snapshot.state(row).name)
            out.putString("chime.actionable.${row.tag}", snapshot.actionable(row).toString())
        }
        // The alarm list shows the banner exactly while a row is missing, and the banner opens the page.
        context.startActivity(Intent(context, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        compose.waitUntil(WAIT_MILLIS) { listOf("chime.home.list", "chime.emptyState").any { compose.onAllNodesWithTag(it).fetchSemanticsNodes().isNotEmpty() } }
        val banner = compose.onAllNodesWithTag(BANNER).fetchSemanticsNodes().isNotEmpty()
        out.putString("chime.banner", banner.toString())
        if (banner) {
            compose.onNodeWithTag(BANNER).performClick()
            out.putString("chime.bannerOpens", waitForPage().toString())
        }
        context.startActivity(
            Intent(AlarmPluginActions.OPEN).setPackage(context.packageName)
                .putExtra(AlarmPluginActions.EXTRA_PAGE, AlarmPluginActions.PAGE_PERMISSIONS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        out.putString("chime.direct", waitForPage().toString())
        val wanted = if (argument == "all") snapshot.rows else snapshot.rows.filter { it.tag in argument!!.split(',') }
        for (row in wanted) {
            val key = "chime.jump.${row.tag}"
            when {
                !snapshot.actionable(row) -> out.putString(key, "notActionable")
                row == PermissionRow.NOTIFICATIONS && snapshot.requestsNotifications -> out.putString(key, "skipped:runtimeRequest")
                else -> out.putString(key, jump(row))
            }
        }
        instrumentation.runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.RESUMED, Stage.PAUSED, Stage.STOPPED).flatMap { monitor.getActivitiesInStage(it) }.forEach(Activity::finish)
        }
        instrumentation.sendStatus(0, out)
    }

    /** Taps [row] and reports `<opened page>|<component on top>[|fallback]`. zh-CN: 点按 [row] 并报告 `<打开的页面>|<顶部组件>[|fallback]`. */
    private fun jump(row: PermissionRow): String {
        compose.onNodeWithTag("chime.permissions.list").performScrollToKey(row.tag)
        compose.onNodeWithTag("chime.permissions.${row.tag}").performClick()
        val top = waitForTop { it != null && !it.startsWith("${context.packageName}/") }
        val opened = Permissions.lastOpened ?: "none"
        val fallback = opened == Settings.ACTION_APPLICATION_DETAILS_SETTINGS && row != PermissionRow.BACKGROUND
        // Back to the page: it is singleTop, so CLEAR_TOP removes anything started over it in the plugin's task.
        context.startActivity(
            Intent(context, PermissionsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        waitForPage()
        return listOfNotNull(opened, top ?: "unknown", "fallback".takeIf { fallback }).joinToString("|")
    }

    private fun waitForPage(): Boolean = runCatching {
        compose.waitUntil(WAIT_MILLIS) {
            resumed().any { it is PermissionsActivity } &&
                compose.onAllNodesWithTag("chime.permissions.list").fetchSemanticsNodes().isNotEmpty()
        }
    }.isSuccess

    private fun resumed(): Collection<Activity> {
        var activities: Collection<Activity> = emptyList()
        instrumentation.runOnMainSync { activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).toList() }
        return activities
    }

    private fun waitForTop(condition: (String?) -> Boolean): String? {
        val deadline = SystemClock.uptimeMillis() + WAIT_MILLIS
        var top = topActivity()
        while (!condition(top) && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(200)
            top = topActivity()
        }
        return top
    }

    /** The resumed activity on top, from `dumpsys activity activities`. zh-CN: 位于顶部的已恢复 Activity, 取自 `dumpsys activity activities`. */
    private fun topActivity(): String? {
        val descriptor = instrumentation.uiAutomation.executeShellCommand("dumpsys activity activities")
        val text = ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes().decodeToString() }
        return RESUMED.find(text)?.groupValues?.get(1)
    }

    companion object {
        const val ARGUMENT = "chimePermissions"
        private const val BANNER = "chime.home.permissionBanner"
        private const val WAIT_MILLIS = 10_000L
        private val RESUMED = Regex("""[Rr]esumedActivity[:=]\s*ActivityRecord\{\S+ u\d+ ([^\s}]+)""")
    }
}
