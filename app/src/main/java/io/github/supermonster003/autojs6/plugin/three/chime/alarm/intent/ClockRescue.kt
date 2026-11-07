package io.github.supermonster003.autojs6.plugin.three.chime.alarm.intent

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log

/**
 * Starts the clock requests that the system left behind the home screen (roadmap Q27, T105). On API 24 - 27, when two
 * requests arrive together (above all while another page of the plugin is closing), the task of one of them can end up
 * behind the home screen with its [ClockIntentActivity] never started, so the request is lost; `documentLaunchMode`
 * (T104) does not prevent it there. A single request is always started, so every lost request comes with one that runs
 * [ClockIntentActivity], which calls [schedule]: [DELAYS_MILLIS] later [rescue] looks for tasks of this app whose only
 * activity is a [ClockIntentActivity] that is not running and moves them to the front, which starts the activity and
 * answers the request. A started request finishes at once and is never moved. API 28+ starts every request; the
 * permission (`REORDER_TASKS`, `maxSdkVersion` 27) is not requested there.
 * zh-CN: 启动被系统留在桌面后面的时钟请求 (路线图 Q27, T105). 在 API 24 - 27 上, 两个请求同时到达时 (尤其插件另一个页面正在关闭时), 其中一个请求的任务
 * 可能被压到桌面后面, 它的 [ClockIntentActivity] 从未启动, 请求因此丢失; `documentLaunchMode` (T104) 在这些版本上挡不住. 单个请求总会启动, 所以每个丢失
 * 的请求都伴随一个运行了 [ClockIntentActivity] 的请求, 由它调用 [schedule]: [DELAYS_MILLIS] 之后 [rescue] 查找本应用中唯一活动是未运行的
 * [ClockIntentActivity] 的任务并调到前台, 由此启动该活动并应答请求. 已启动的请求会立即结束, 不会被调动. API 28+ 会启动每个请求, 权限
 * (`REORDER_TASKS`, `maxSdkVersion` 27) 在那里不申请.
 */
internal object ClockRescue {

    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val token = Any()

    /**
     * Schedules [rescue] after each delay of [DELAYS_MILLIS] (restarted by the next request); nothing on API 28+.
     * zh-CN: 在 [DELAYS_MILLIS] 的每个延迟之后安排 [rescue] (下一个请求重新计时); API 28+ 不做任何事.
     */
    fun schedule(context: Context) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) return
        val app = context.applicationContext
        handler.removeCallbacksAndMessages(token)
        val now = SystemClock.uptimeMillis()
        for (delay in DELAYS_MILLIS) handler.postAtTime({ rescue(app) }, token, now + delay)
    }

    /**
     * Moves the tasks of clock requests that were never started to the front and answers how many it moved.
     * zh-CN: 把从未启动的时钟请求的任务调到前台, 返回调动的个数.
     */
    @Suppress("DEPRECATION")
    fun rescue(context: Context): Int {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return 0
        val name = ClockIntentActivity::class.java.name
        val tasks = try {
            manager.getRunningTasks(Int.MAX_VALUE)
        } catch (e: SecurityException) {
            Log.w(TAG, "rescue: tasks unavailable: ${e.javaClass.simpleName}")
            return 0
        }
        var moved = 0
        for (task in tasks) {
            if (task.baseActivity?.className != name || task.topActivity?.className != name || task.numRunning != 0) continue
            try {
                manager.moveTaskToFront(task.id, 0)
                moved++
            } catch (e: SecurityException) {
                Log.w(TAG, "rescue: task ${task.id} not moved: ${e.javaClass.simpleName}")
            }
        }
        if (moved > 0) Log.i(TAG, "rescue: moved=$moved")
        return moved
    }

    /**
     * The first look catches the usual case; the later ones cover a slow system and the five seconds after the Home key,
     * in which the system does not move a task to the front for an app.
     * zh-CN: 第一次查找覆盖常见情形; 后面几次覆盖系统较慢的情形, 以及按下主屏幕键后系统不为应用调动任务的五秒.
     */
    private val DELAYS_MILLIS = longArrayOf(500L, 2_000L, 6_000L)

    private const val TAG = "ChimeIntent"
}
