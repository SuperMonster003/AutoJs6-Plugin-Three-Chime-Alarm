package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.content.Context
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.AlarmEventBus
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedule
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.schedule.AndroidAlarmSchedulePort
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The alarm objects of the plugin process: one [repository] on device-protected storage (roadmap T7, usable before
 * the first unlock), the [schedule] that keeps the system alarm clocks in step with it, and the [events] of the
 * process. The first use reconciles every alarm once, which also restores the wake-ups a force stop cleared.
 * zh-CN: 插件进程的闹钟对象: 设备加密存储上的一个 [repository] (路线图 T7, 首次解锁前可用), 让系统闹钟与之保持一致的 [schedule], 以及进程内的 [events].
 * 首次使用时对全部闹钟对账一次, 同时恢复被强行停止清掉的唤醒.
 */
class AlarmRuntime private constructor(context: Context) {

    val context: Context = context.applicationContext

    val events = AlarmEventBus()

    val port = AndroidAlarmSchedulePort(this.context)

    val repository: AlarmRepository = AlarmRepository(
        store = AlarmStore(File(this.context.createDeviceProtectedStorageContext().filesDir, STORE_DIRECTORY)),
        planner = { schedule.plan(it) },
        schedulingAllowed = port::canScheduleExact,
    )

    val schedule: AlarmSchedule = AlarmSchedule(repository, port, events)

    /** One thread for the scheduler work of receivers. zh-CN: 接收器的调度工作所用的单线程. */
    val executor: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "chime-schedule") }

    private fun start() {
        executor.execute {
            try {
                log("start", schedule.reconcile(AlarmPlanner.Cause.RESTORE).values)
            } catch (e: RuntimeException) {
                Log.e(AndroidAlarmSchedulePort.TAG, "start reconcile failed", e)
            }
        }
    }

    /**
     * Logs one reconciliation: the count, then the rings and events only (the device checks of roadmap P2.3 read these
     * lines, `adb logcat -s ChimeSchedule`).
     * zh-CN: 记录一次对账: 数量, 然后只记响铃与事件 (路线图 P2.3 的设备检查读取这些行).
     */
    internal fun log(what: String, outcomes: Collection<AlarmPlanner.Outcome>) {
        val decided = outcomes.flatMap { outcome ->
            listOfNotNull(outcome.ring?.let { "ring ${it.alarmId}" }) + outcome.events.map { "${it.type} ${it.alarmId} ${it.reason}" }
        }
        Log.i(AndroidAlarmSchedulePort.TAG, "$what: ${outcomes.size} reconciled" + decided.joinToString("") { ", $it" })
    }

    companion object {

        /** The directory of `alarms.json` under device-protected `filesDir` (roadmap P2.1). zh-CN: `alarms.json` 所在目录. */
        const val STORE_DIRECTORY = "alarms"

        @Volatile
        private var instance: AlarmRuntime? = null

        fun get(context: Context): AlarmRuntime = instance ?: synchronized(this) {
            instance ?: AlarmRuntime(context).also {
                instance = it
                it.start()
            }
        }
    }
}
