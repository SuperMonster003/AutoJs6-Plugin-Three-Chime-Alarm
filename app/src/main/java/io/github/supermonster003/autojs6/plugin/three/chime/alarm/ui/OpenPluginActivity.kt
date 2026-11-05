package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.HomeActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions.PermissionsActivity
import org.autojs.plugin.alarm.api.AlarmPluginActions

/**
 * The `alarms.openPlugin(page)` entry (roadmap T81, appendix A section 9): the host starts the frozen action
 * [AlarmPluginActions.OPEN] with the optional [AlarmPluginActions.EXTRA_PAGE]; this window-less trampoline is exported
 * without a permission (it only shows the plugin's own screens) and forwards to the screen of the page: `permissions`
 * opens the permissions page (P3.4); the settings page joins with P3.5 and the timers and stopwatch pages with 1.1.0,
 * and until then they, an absent page and an unknown page open the alarm list. The screens are not exported
 * themselves, so only this trampoline and the launcher reach them from outside.
 * zh-CN: `alarms.openPlugin(page)` 的入口 (路线图 T81, 附录 A 第 9 节): 宿主以可选的 [AlarmPluginActions.EXTRA_PAGE] 启动冻结的 action
 * [AlarmPluginActions.OPEN]; 这个无窗口的跳板无权限导出 (只显示插件自己的界面), 转到对应页面: `permissions` 打开权限页 (P3.4); 设置页随 P3.5,
 * 计时器与秒表页随 1.1.0 提供, 在此之前它们, 缺省的页面与未知页面都打开闹钟列表. 各页面本身不导出, 外部只能经由这个跳板与启动器进入.
 */
class OpenPluginActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val page = intent?.getStringExtra(AlarmPluginActions.EXTRA_PAGE)
            Log.i(TAG, "open page=${page ?: "(none)"}")
            startActivity(targetOf(this, page).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }

    companion object {

        const val TAG = "ChimeOpen"

        /** The screen of [page]. zh-CN: [page] 对应的页面. */
        fun targetOf(context: Context, page: String?): Intent {
            // The settings screen (roadmap P3.5) joins here as it lands. zh-CN: 设置页 (路线图 P3.5) 落地后加入.
            if (page == AlarmPluginActions.PAGE_PERMISSIONS) return Intent(context, PermissionsActivity::class.java)
            if (page != null && page !in AlarmPluginActions.PAGES) Log.w(TAG, "unknown page '$page', opening the alarm list")
            return Intent(context, HomeActivity::class.java)
        }
    }
}
