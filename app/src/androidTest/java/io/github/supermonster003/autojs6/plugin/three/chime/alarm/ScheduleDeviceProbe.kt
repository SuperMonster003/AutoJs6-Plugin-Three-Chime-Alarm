package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.app.AlarmManager
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * The alarm list side of `.python/device_schedule.py` (roadmap P2.3): adds, skips, reports and removes probe alarms in
 * the plugin process and reports their state, the exact alarm permission and the system's next alarm clock. The device
 * script reads the reports from the instrumentation status lines (`chime.*` keys). Without the `chimeProbe` argument the
 * test is skipped, so `connectedDebugAndroidTest` does not touch the alarm list.
 *
 * zh-CN: `.python/device_schedule.py` (路线图 P2.3) 的闹钟列表一侧: 在插件进程中添加, 跳过, 报告与删除探针闹钟, 并报告其状态, 精确闹钟权限与系统的
 * 下一个闹钟. 设备脚本从 instrumentation 状态行 (`chime.*` 键) 读取报告. 没有 `chimeProbe` 参数时跳过, 因此 `connectedDebugAndroidTest`
 * 不会改动闹钟列表.
 */
@RunWith(AndroidJUnit4::class)
class ScheduleDeviceProbe {

    @Test
    fun probe() {
        val arguments = InstrumentationRegistry.getArguments()
        val operation = arguments.getString(ARGUMENT_OPERATION)
        assumeTrue("run by .python/device_schedule.py", operation != null)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val runtime = AlarmRuntime.get(instrumentation.targetContext)
        // The start reconciliation of the process has run. zh-CN: 进程启动时的对账已完成.
        runtime.executor.submit {}.get(30, TimeUnit.SECONDS)
        val repository = runtime.repository
        val out = Bundle()
        when (operation) {
            "add" -> {
                val label = arguments.getString("label").orEmpty()
                require(label.startsWith(LABEL_PREFIX)) { "probe labels start with $LABEL_PREFIX" }
                val options = linkedMapOf<String, Any?>(
                    AlarmOptionKeys.HOUR to arguments.getString("hour")!!.toInt(),
                    AlarmOptionKeys.MINUTE to arguments.getString("minute")!!.toInt(),
                    AlarmOptionKeys.LABEL to label,
                )
                arguments.getString("repeat")?.let { options[AlarmOptionKeys.REPEAT] = it }
                try {
                    out.putString("chime.added", repository.add(options, "tool").id)
                } catch (e: AlarmException) {
                    out.putString("chime.error", e.code)
                }
            }
            "skip" -> repository.skipNext(arguments.getString("id")!!, "tool")
            "clear" -> {
                repository.list().filter(::isProbe).forEach { repository.remove(it.id, "tool") }
                // The missed alarm notifications of the probes. zh-CN: 探针留下的错过通知.
                NotificationManagerCompat.from(instrumentation.targetContext).cancelAll()
            }
            "state" -> Unit
            else -> throw IllegalArgumentException("unknown probe operation '$operation'")
        }
        runtime.executor.submit {}.get(30, TimeUnit.SECONDS)
        for (record in repository.list().filter(::isProbe)) {
            val snapshot = record.toSnapshot()
            out.putString(
                "chime.alarm.${record.spec.label}",
                "id=${record.id} enabled=${record.enabled} next=${snapshot.nextTrigger} skipping=${snapshot.skipping} handled=${record.state.handledThrough}",
            )
        }
        val alarmManager = instrumentation.targetContext.getSystemService(AlarmManager::class.java)
        val next = alarmManager.nextAlarmClock
        out.putString("chime.exact", runtime.port.canScheduleExact().toString())
        out.putString("chime.nextClock", "${next?.triggerTime} ${next?.showIntent?.creatorPackage}")
        out.putString("chime.now", System.currentTimeMillis().toString())
        out.putString("chime.zone", ZoneId.systemDefault().id)
        instrumentation.sendStatus(STATUS_REPORT, out)
    }

    private fun isProbe(record: AlarmRecord) = record.spec.label?.startsWith(LABEL_PREFIX) == true

    private companion object {
        const val ARGUMENT_OPERATION = "chimeProbe"
        const val LABEL_PREFIX = "p23-probe"
        const val STATUS_REPORT = 7
    }
}
