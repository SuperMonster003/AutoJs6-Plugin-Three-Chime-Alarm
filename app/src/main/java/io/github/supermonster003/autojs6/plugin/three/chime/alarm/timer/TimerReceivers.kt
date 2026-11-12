package io.github.supermonster003.autojs6.plugin.three.chime.alarm.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import org.autojs.plugin.alarm.api.AlarmContract

/**
 * Receives the wake-ups of [AndroidTimerPort] (roadmap P7.1, T7: direct boot aware, not exported) and reconciles the
 * timers: the due ones ring, the others are armed again.
 * zh-CN: 接收 [AndroidTimerPort] 的唤醒 (路线图 P7.1, T7: 支持直接启动, 不导出) 并对计时器对账: 到点的响铃, 其余重新排程.
 */
class TimerTriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val timerId = AndroidTimerPort.timerIdOf(intent) ?: return
        val pending = goAsync()
        val runtime = AlarmRuntime.get(context)
        runtime.executor.execute {
            try {
                Log.i(AndroidTimerPort.TAG, "trigger $timerId: ${runtime.timers.reconcile(AlarmPlanner.Cause.TRIGGER)} rang")
            } catch (e: RuntimeException) {
                Log.e(AndroidTimerPort.TAG, "trigger $timerId failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * The actions of the timer and stopwatch notifications ([TimerNotifications], roadmap P7.1 / P7.2; not exported, only
 * the plugin's own pending intents reach it), done `by: 'user'`.
 * zh-CN: 计时器与秒表通知的动作 ([TimerNotifications], 路线图 P7.1 / P7.2; 不导出, 只有插件自己的 PendingIntent 能到达), 以 `by: 'user'` 执行.
 */
class TimerActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val timerId = AndroidTimerPort.timerIdOf(intent)
        val pending = goAsync()
        val runtime = AlarmRuntime.get(context)
        runtime.executor.execute {
            try {
                when (action) {
                    TimerNotifications.ACTION_TIMER_PAUSE -> timerId?.let(runtime.timers::pause)
                    TimerNotifications.ACTION_TIMER_RESUME -> timerId?.let(runtime.timers::resume)
                    TimerNotifications.ACTION_TIMER_ADD_MINUTE -> timerId?.let { runtime.timers.addTime(it, MINUTE_MILLIS) }
                    TimerNotifications.ACTION_TIMER_CANCEL -> timerId?.let { runtime.userTimerApi.cancel(it) }
                    TimerNotifications.ACTION_STOPWATCH_PAUSE -> runtime.stopwatch.pause()
                    TimerNotifications.ACTION_STOPWATCH_RESUME -> runtime.stopwatch.resume()
                    TimerNotifications.ACTION_STOPWATCH_LAP -> runtime.stopwatch.lap()
                    TimerNotifications.ACTION_STOPWATCH_RESET -> runtime.stopwatch.reset()
                    else -> null
                }
                Log.i(AndroidTimerPort.TAG, "${action.substringAfterLast('.')} ${timerId.orEmpty()} by ${AlarmContract.BY_USER}")
            } catch (e: AlarmException) {
                // A stale notification (the timer rang or was removed meanwhile), or a time left above 100 h.
                // zh-CN: 过期的通知 (计时器期间已响铃或已删除), 或剩余时间超过 100 h.
                Log.w(AndroidTimerPort.TAG, "${action.substringAfterLast('.')} ${timerId.orEmpty()}: ${e.code} ${e.detail}")
                timerId?.let { TimerNotifications.timer(context, it, runtime.timers.get(it)) }
            } catch (e: RuntimeException) {
                Log.e(AndroidTimerPort.TAG, "$action failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
