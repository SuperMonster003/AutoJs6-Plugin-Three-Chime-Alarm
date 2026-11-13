package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.service.ThreeChimeAlarmService
import org.autojs.plugin.alarm.api.AlarmBundles
import org.autojs.plugin.alarm.api.AlarmCapabilityKeys
import org.autojs.plugin.alarm.api.AlarmResultKeys
import org.autojs.plugin.alarm.api.IAlarmPlugin
import org.autojs.plugin.alarm.api.IAlarmProgressCallback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * The real holiday data refresh of `.python/device_holidays.py` (roadmap P8.2, T172 - T175): binds the capability
 * service as the host does, reads `holidayInfo` and the `holidayDataVersion` capability, starts `refreshHolidays` with a
 * progress callback, waits for its outcome over the network (the signed catalog on raw.githubusercontent.com, the
 * calendar files from the GitHub release) and reads both again. The script reads the report from the instrumentation
 * status lines (`chime.*` keys). Without the `chimeHolidayRefresh` argument the test is skipped, so
 * `connectedDebugAndroidTest` stays offline.
 *
 * zh-CN: `.python/device_holidays.py` 的真实节假日数据刷新 (路线图 P8.2, T172 - T175): 像宿主一样绑定能力服务, 读取 `holidayInfo` 与能力
 * `holidayDataVersion`, 带进度回调开始 `refreshHolidays`, 等待它经网络 (raw.githubusercontent.com 上的签名目录, GitHub Release 中的日历文件)
 * 得出结果, 再读取一次两者. 设备脚本从 instrumentation 状态行 (`chime.*` 键) 读取报告. 没有 `chimeHolidayRefresh` 参数时跳过, 因此
 * `connectedDebugAndroidTest` 不联网.
 */
@RunWith(AndroidJUnit4::class)
class HolidayRefreshProbe {

    @Test
    fun refresh() {
        assumeTrue("run by .python/device_holidays.py", InstrumentationRegistry.getArguments().getString(ARGUMENT) != null)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val out = Bundle()
        val outcome = AtomicReference<Bundle>()
        val failed = AtomicReference<Boolean>()
        var before: Map<*, *>? = null
        var after: Map<*, *>? = null
        var versionAfter: String? = null
        withPlugin(instrumentation.targetContext) { plugin ->
            before = info(plugin)
            out.putString("chime.before", describe(before))
            out.putString("chime.capabilityBefore", plugin.capabilities.getString(AlarmCapabilityKeys.HOLIDAY_DATA_VERSION).toString())
            val updates = AtomicInteger()
            val last = AtomicReference("none")
            val finished = CountDownLatch(1)
            val callback = object : IAlarmProgressCallback.Stub() {
                override fun onProgress(opId: Int, done: Long, total: Long) {
                    updates.incrementAndGet()
                    last.set("$done/$total")
                }

                override fun onCompleted(opId: Int, result: Bundle?) = settle(false, result)

                override fun onFailed(opId: Int, error: Bundle?) = settle(true, error)

                private fun settle(failure: Boolean, bundle: Bundle?) {
                    failed.set(failure)
                    outcome.set(bundle)
                    finished.countDown()
                }
            }
            val started = SystemClock.elapsedRealtime()
            val op = plugin.refreshHolidays(AlarmBundles.of(linkedMapOf<String, Any?>()), callback)
            val settled = finished.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!settled) plugin.cancelOperation(op)
            out.putString("chime.op", op.toString())
            out.putString("chime.settled", settled.toString())
            out.putString("chime.elapsedMs", (SystemClock.elapsedRealtime() - started).toString())
            out.putString("chime.progress", "${updates.get()} updates, last ${last.get()}")
            val error = AlarmBundles.errorOf(outcome.get())
            out.putString("chime.outcome", if (!settled) "timeout" else if (failed.get() == true) "failed ${error?.first}: ${error?.second}" else "completed")
            if (settled && failed.get() == false) {
                out.putString("chime.answer", describe(AlarmBundles.fromBundle(outcome.get())[AlarmResultKeys.HOLIDAY_INFO] as? Map<*, *>))
            }
            after = info(plugin)
            out.putString("chime.after", describe(after))
            versionAfter = plugin.capabilities.getString(AlarmCapabilityKeys.HOLIDAY_DATA_VERSION)
            out.putString("chime.capabilityAfter", versionAfter.toString())
        }
        out.putString("chime.done", "true")
        instrumentation.sendStatus(STATUS_REPORT, out)
        assertEquals("the refresh settled", "completed", out.getString("chime.outcome"))
        assertNull("no error", AlarmBundles.errorOf(outcome.get()))
        assertEquals("the capability names the data version", after?.get(AlarmResultKeys.VERSION), versionAfter)
        assertTrue("the data version does not go back", version(after) >= version(before))
    }

    private fun info(plugin: IAlarmPlugin): Map<*, *> {
        val answer = plugin.holidayInfo(AlarmBundles.of(linkedMapOf<String, Any?>()))
        assertNull("holidayInfo answers", AlarmBundles.errorOf(answer))
        return AlarmBundles.fromBundle(answer)[AlarmResultKeys.HOLIDAY_INFO] as Map<*, *>
    }

    private fun describe(info: Map<*, *>?): String = if (info == null) "null" else listOf(AlarmResultKeys.VERSION, AlarmResultKeys.DATA_SOURCE, AlarmResultKeys.UPDATED_AT)
        .joinToString(" ") { "$it=${info[it]}" }

    /** The catalog version inside the data version (`holiday-calendars-v<N>...`), 0 when absent. zh-CN: 数据版本中的目录版本号, 没有时为 0. */
    private fun version(info: Map<*, *>?): Int = Regex("""v(\d+)""").find(info?.get(AlarmResultKeys.VERSION)?.toString().orEmpty())?.groupValues?.get(1)?.toInt() ?: 0

    private fun withPlugin(context: Context, block: (IAlarmPlugin) -> Unit) {
        val binder = AtomicReference<IBinder>()
        val connected = CountDownLatch(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binder.set(service)
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit
        }
        val intent = Intent().setComponent(ComponentName(context, ThreeChimeAlarmService::class.java))
        assertTrue(context.bindService(intent, connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("timed out binding the alarm service", connected.await(10, TimeUnit.SECONDS))
            block(IAlarmPlugin.Stub.asInterface(MarshallingBinder(binder.get())))
        } finally {
            context.unbindService(connection)
        }
    }

    private companion object {
        const val ARGUMENT = "chimeHolidayRefresh"
        const val STATUS_REPORT = 7
        const val TIMEOUT_SECONDS = 120L
    }
}
