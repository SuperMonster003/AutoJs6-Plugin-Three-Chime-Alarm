package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * The alarm list side of the `ui` mode of `.python/device_contract.py` (roadmap D20, T27, P2 wrap-up): opens the alarm
 * list, unbinds the alarm named by the `chimeUnbind` argument through its overflow menu and the confirmation dialog,
 * as a user would, and reports whether the plugin still holds a binding hint for it (`chime.*` status keys); the
 * device script then reads the host side. Without the argument the test is skipped, so `connectedDebugAndroidTest`
 * does not touch the alarm list.
 * zh-CN: `.python/device_contract.py` 的 `ui` 模式在闹钟列表一侧的部分 (路线图 D20, T27, P2 收尾): 打开闹钟列表, 像用户一样经溢出菜单与确认对话框解除
 * `chimeUnbind` 参数所指闹钟的绑定, 报告插件是否仍持有它的绑定提示 (`chime.*` 状态键); 设备脚本随后读取宿主一侧. 没有该参数时跳过, 因此
 * `connectedDebugAndroidTest` 不会改动闹钟列表.
 */
@RunWith(AndroidJUnit4::class)
class HomeUnbindProbe {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun unbind() {
        val id = InstrumentationRegistry.getArguments().getString(ARGUMENT)
        assumeTrue("run by .python/device_contract.py", id != null)
        id!!
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val runtime = AlarmRuntime.get(context)
        runtime.executor.submit {}.get(30, TimeUnit.SECONDS)
        val out = Bundle()
        val bound = runCatching { compose.waitUntil(WAIT_MILLIS) { runtime.hints.hasBindings(id) } }.isSuccess
        out.putString("chime.boundBefore", bound.toString())
        context.startActivity(Intent(context, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val tag = "chime.home.alarm.$id"
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("chime.home.list").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("chime.home.list").performScrollToKey(id)
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("$tag.menu").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("$tag.menu").performClick()
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("$tag.unbind").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("$tag.unbind").performClick()
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("chime.home.unbindDialog.confirm").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("chime.home.unbindDialog.confirm").performClick()
        val unbound = runCatching { compose.waitUntil(WAIT_MILLIS) { !runtime.hints.hasBindings(id) } }.isSuccess
        out.putString("chime.boundAfter", (!unbound).toString())
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag("$tag.bound").fetchSemanticsNodes().isEmpty() }
        out.putString("chime.markGone", "true")
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).forEach(Activity::finish)
        }
        instrumentation.sendStatus(0, out)
    }

    companion object {
        const val ARGUMENT = "chimeUnbind"
        private const val WAIT_MILLIS = 20_000L
    }
}
