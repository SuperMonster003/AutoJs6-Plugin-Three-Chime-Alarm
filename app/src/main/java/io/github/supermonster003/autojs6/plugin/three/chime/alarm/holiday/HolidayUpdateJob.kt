package io.github.supermonster003.autojs6.plugin.three.chime.alarm.holiday

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.net.CancelToken
import java.util.concurrent.ConcurrentHashMap

/**
 * The weekly holiday data check (roadmap P8.2, T176): a persisted periodic job of the system job scheduler, every
 * [INTERVAL_MILLIS], on an unmetered network, or on any network when "allow metered downloads" is on (the switch and
 * default of 3-Macaw Speech). The system picks the moment within the period and keeps the job over reboots; a failed check
 * asks for a retry with the scheduler's back-off. Off when the automatic check is off. A newer catalog replaces the data
 * and replans the alarms (T8, the sixth moment).
 * zh-CN: 每周一次的节假日数据检查 (路线图 P8.2, T176): 系统作业调度器的持久周期作业, 每 [INTERVAL_MILLIS] 一次, 在不计量网络上运行, 开启 "允许
 * 计量网络下载" 时在任何网络上运行 (开关与默认值同 3-Macaw Speech). 系统在周期内选择时机, 重启后作业仍在; 检查失败时按调度器的退避重试. 关闭自动
 * 检查时不安排. 更新的目录替换数据并重排闹钟 (T8 第六个时机).
 */
class HolidayUpdateJob : JobService() {

    private val running = ConcurrentHashMap<Int, CancelToken>()

    override fun onStartJob(params: JobParameters): Boolean {
        val runtime = AlarmRuntime.get(this)
        if (!runtime.holidaySettings.autoUpdate) {
            sync(this, runtime.holidaySettings)
            return false
        }
        val cancel = CancelToken()
        running[params.jobId] = cancel
        runtime.refreshHolidays(cancel, null) { result ->
            running.remove(params.jobId)
            result.exceptionOrNull()?.let { Log.w(TAG, "weekly holiday check failed: ${it.javaClass.simpleName}: ${it.message}") }
            result.getOrNull()?.let { Log.i(TAG, "weekly holiday check: ${if (it.updated) "updated to ${it.info.version}, ${it.files} files" else "up to date"}") }
            if (!cancel.isCancelled) jobFinished(params, result.isFailure)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        running.remove(params.jobId)?.cancel()
        return true
    }

    companion object {

        const val TAG = "ChimeHolidays"
        const val JOB_ID = 0x43484401
        const val INTERVAL_MILLIS = 7L * 24 * 60 * 60 * 1000

        /**
         * Schedules, updates or cancels the job after the settings. Keeps a pending job whose network rule is already
         * right, so its period is not restarted at every process start.
         * zh-CN: 按设置安排, 更新或取消作业. 网络规则已正确的待运行作业保持不动, 以免每次进程启动都重新开始周期.
         */
        fun sync(context: Context, settings: HolidaySettings) {
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
            if (!settings.autoUpdate) {
                scheduler.cancel(JOB_ID)
                return
            }
            val network = if (settings.allowMeteredDownloads) JobInfo.NETWORK_TYPE_ANY else JobInfo.NETWORK_TYPE_UNMETERED
            val pending = scheduler.getPendingJob(JOB_ID)
            @Suppress("DEPRECATION")
            if (pending != null && pending.networkType == network && pending.intervalMillis == INTERVAL_MILLIS && pending.isPersisted) return
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, HolidayUpdateJob::class.java))
                .setPeriodic(INTERVAL_MILLIS)
                .setRequiredNetworkType(network)
                .setPersisted(true)
                .build()
            if (scheduler.schedule(job) != JobScheduler.RESULT_SUCCESS) Log.w(TAG, "the weekly holiday check could not be scheduled")
        }
    }
}
