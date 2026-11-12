package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.autojs.plugin.alarm.api.AlarmPluginActions

/**
 * The three pages of the bottom navigation (roadmap P7.4, T139), named like the `alarms.openPlugin(page)` pages.
 * zh-CN: 底部导航的三个页面 (路线图 P7.4, T139), 名称与 `alarms.openPlugin(page)` 的页面相同.
 */
internal enum class HomePage(val key: String) {
    ALARMS(AlarmPluginActions.PAGE_ALARMS),
    TIMERS(AlarmPluginActions.PAGE_TIMERS),
    STOPWATCH(AlarmPluginActions.PAGE_STOPWATCH),
    ;

    companion object {
        fun of(key: String?): HomePage? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Intents that open [HomeActivity] on a page (roadmap P7.4, T139, T123). The extra has the name of
 * [AlarmPluginActions.EXTRA_PAGE]; [EXTRA_CUSTOM_TIMER] also opens the custom duration dialog of the timers page (the
 * `SET_TIMER` intent without a length, roadmap P7.5). Each page has its own pending intent request code, since pending
 * intents that differ only in their extras are one and the same.
 * zh-CN: 打开 [HomeActivity] 指定页面的 Intent (路线图 P7.4, T139, T123). extra 名称同 [AlarmPluginActions.EXTRA_PAGE]; [EXTRA_CUSTOM_TIMER] 另外打开
 * 计时器页的自定义时长对话框 (不带时长的 `SET_TIMER` Intent, 路线图 P7.5). 每个页面的 PendingIntent 使用各自的请求码, 因为只有 extras 不同的 PendingIntent 是同一个.
 */
internal object HomeIntents {

    const val EXTRA_PAGE = AlarmPluginActions.EXTRA_PAGE
    const val EXTRA_CUSTOM_TIMER = "io.github.supermonster003.autojs6.plugin.three.chime.alarm.extra.CUSTOM_TIMER"

    fun of(context: Context, page: HomePage, customTimer: Boolean = false): Intent =
        Intent(context, HomeActivity::class.java)
            .putExtra(EXTRA_PAGE, page.key)
            .apply { if (customTimer) putExtra(EXTRA_CUSTOM_TIMER, true) }
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Request codes 0 (alarms, as before 1.1.0), 1 (timers) and 2 (stopwatch). zh-CN: 请求码 0 (闹钟, 与 1.1.0 之前相同), 1 (计时器), 2 (秒表). */
    fun pending(context: Context, page: HomePage): PendingIntent = PendingIntent.getActivity(
        context,
        page.ordinal,
        of(context, page),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
