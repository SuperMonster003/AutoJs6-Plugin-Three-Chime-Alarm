package io.github.supermonster003.autojs6.plugin.three.chime.alarm.service

import android.content.Context
import android.os.Binder
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring.RingService
import org.autojs.plugin.alarm.api.AlarmBundles
import org.autojs.plugin.alarm.api.AlarmCapabilityKeys
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes
import org.autojs.plugin.alarm.api.IAlarmEventCallback
import org.autojs.plugin.alarm.api.IAlarmPlugin
import org.autojs.plugin.alarm.api.IAlarmProgressCallback
import org.autojs.plugin.common.api.PluginInfo
import java.util.concurrent.atomic.AtomicInteger

/**
 * The `IAlarmPlugin` Binder of [ThreeChimeAlarmService] (roadmap P2.5, appendix A section 4): decodes the request
 * Bundles, runs [io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.AlarmApi] and encodes the answer.
 * A failure becomes an error answer (`errorCode`, `errorMessage`), never an exception across Binder: a contract error
 * keeps its code, a value outside the value tree is `INVALID_OPTIONS`, anything else `INTERNAL` (logged under
 * `ChimeService`). The methods of the features this build does not declare (`timers`, `stopwatch`, `holidays`,
 * `beforeRing`, `challenges`) answer `UNSUPPORTED` (T25).
 * zh-CN: [ThreeChimeAlarmService] 的 `IAlarmPlugin` Binder (路线图 P2.5, 附录 A 第 4 节): 解码请求 Bundle, 调用 AlarmApi 并编码应答. 失败一律变为
 * 错误应答 (`errorCode`, `errorMessage`), 不跨 Binder 抛异常: 契约错误保留其错误码, 超出值树的值报 `INVALID_OPTIONS`, 其他报 `INTERNAL` (以
 * `ChimeService` 记录). 本构建未声明的功能 (`timers`, `stopwatch`, `holidays`, `beforeRing`, `challenges`) 的方法返回 `UNSUPPORTED` (T25).
 */
class AlarmPluginBinder(context: Context) : IAlarmPlugin.Stub() {

    private val context: Context = context.applicationContext

    private val runtime: AlarmRuntime = AlarmRuntime.get(this.context)

    private val operations = AtomicInteger()

    override fun getInfo(): PluginInfo = context.pluginRuntimeFields().toPluginInfo()

    override fun getCapabilities(): Bundle = answering("getCapabilities") {
        val capabilities = context.pluginRuntimeFields().toPluginInfo().capabilities ?: Bundle()
        capabilities.apply {
            putInt(AlarmContract.KEY_CONTRACT_VERSION, AlarmContract.CONTRACT_VERSION)
            putBundle(AlarmCapabilityKeys.PERMISSIONS, Bundle().apply {
                putBoolean(AlarmContract.PERMISSION_EXACT_ALARM, runtime.port.canScheduleExact())
                putBoolean(AlarmContract.PERMISSION_NOTIFICATIONS, NotificationManagerCompat.from(context).areNotificationsEnabled())
                putBoolean(AlarmContract.PERMISSION_FULL_SCREEN, RingService.fullScreenAllowed(context))
            })
        }
    }

    override fun addAlarm(request: Bundle?): Bundle = call("addAlarm", request) { runtime.api.addAlarm(it) }

    override fun getAlarm(request: Bundle?): Bundle = call("getAlarm", request) { runtime.api.getAlarm(it) }

    override fun listAlarms(query: Bundle?): Bundle = call("listAlarms", query) { runtime.api.listAlarms(it) }

    override fun updateAlarm(request: Bundle?): Bundle = call("updateAlarm", request) { runtime.api.updateAlarm(it) }

    override fun removeAlarm(request: Bundle?): Bundle = call("removeAlarm", request) { runtime.api.removeAlarm(it) }

    override fun setAlarmEnabled(request: Bundle?): Bundle = call("setAlarmEnabled", request) { runtime.api.setAlarmEnabled(it) }

    override fun skipNext(request: Bundle?): Bundle = call("skipNext", request) { runtime.api.skipNext(it) }

    override fun cancelSkip(request: Bundle?): Bundle = call("cancelSkip", request) { runtime.api.cancelSkip(it) }

    override fun upcoming(request: Bundle?): Bundle = call("upcoming", request) { runtime.api.upcoming(it) }

    override fun preview(request: Bundle?): Bundle = call("preview", request) { runtime.api.preview(it) }

    override fun ringing(request: Bundle?): Bundle = call("ringing", request) { runtime.api.ringing(it) }

    override fun dismiss(request: Bundle?): Bundle = call("dismiss", request) { runtime.api.dismiss(it) }

    override fun snooze(request: Bundle?): Bundle = call("snooze", request) { runtime.api.snooze(it) }

    override fun listRingtones(query: Bundle?): Bundle = call("listRingtones", query) { runtime.api.listRingtones(it) }

    /**
     * The descriptor is closed when the call ends, whatever its outcome (T15).
     * zh-CN: 调用结束时无论结果如何都关闭描述符 (T15).
     */
    override fun importRingtone(file: ParcelFileDescriptor?, request: Bundle?): Bundle = try {
        call("importRingtone", request) { values ->
            val descriptor = file ?: throw AlarmException.invalidOptions("missing file descriptor")
            runtime.api.importRingtone(values) { ParcelFileDescriptor.AutoCloseInputStream(descriptor) }
        }
    } finally {
        runCatching { file?.close() }
    }

    override fun registerEventCallback(callback: IAlarmEventCallback?, options: Bundle?): Bundle = call("registerEventCallback", options) { values ->
        val target = callback ?: throw AlarmException.invalidOptions("missing callback")
        runtime.callbacks.register(target, runtime.api.eventsOf(values))
        emptyMap()
    }

    override fun unregisterEventCallback(callback: IAlarmEventCallback?) {
        callback?.let { runCatching { runtime.callbacks.unregister(it) } }
    }

    override fun setBindingHints(hints: Bundle?): Bundle {
        val owner = callingPackage()
        return call("setBindingHints", hints) { values ->
            runtime.api.setBindingHints(owner ?: throw AlarmException(AlarmErrorCodes.INTERNAL, "unknown calling package"), values)
        }
    }

    // ------------------------------------------------------------------ features this build does not declare (T25)

    override fun decideBeforeRing(decision: Bundle?): Bundle = unsupported("decideBeforeRing", "beforeRing")

    override fun startTimer(request: Bundle?): Bundle = unsupported("startTimer", "timers")

    override fun getTimer(request: Bundle?): Bundle = unsupported("getTimer", "timers")

    override fun listTimers(query: Bundle?): Bundle = unsupported("listTimers", "timers")

    override fun controlTimer(request: Bundle?): Bundle = unsupported("controlTimer", "timers")

    override fun stopwatch(request: Bundle?): Bundle = unsupported("stopwatch", "stopwatch")

    override fun holidayRegions(query: Bundle?): Bundle = unsupported("holidayRegions", "holidays")

    override fun checkDate(request: Bundle?): Bundle = unsupported("checkDate", "holidays")

    override fun listHolidays(request: Bundle?): Bundle = unsupported("listHolidays", "holidays")

    override fun holidayInfo(query: Bundle?): Bundle = unsupported("holidayInfo", "holidays")

    /**
     * Answers an operation id and reports `UNSUPPORTED` through [callback] at once.
     * zh-CN: 返回一个操作 id, 并立即经 [callback] 报告 `UNSUPPORTED`.
     */
    override fun refreshHolidays(request: Bundle?, callback: IAlarmProgressCallback?): Int {
        val op = operations.incrementAndGet()
        runCatching { callback?.onFailed(op, unsupported("refreshHolidays", "holidays")) }
        return op
    }

    override fun cancelOperation(opId: Int) {
        // zh-CN: 本构建没有可取消的长操作.
    }

    // ------------------------------------------------------------------ internals

    private fun call(method: String, request: Bundle?, block: (Map<String, Any?>) -> Map<String, Any?>): Bundle = answering(method) {
        AlarmBundles.of(block(AlarmBundles.fromBundle(request)))
    }

    private fun answering(method: String, block: () -> Bundle): Bundle = try {
        block()
    } catch (e: AlarmException) {
        AlarmBundles.error(e.code, e.detail)
    } catch (e: IllegalArgumentException) {
        val error = AlarmException.of(e)
        AlarmBundles.error(error.code, error.detail)
    } catch (e: RuntimeException) {
        Log.e(TAG, "$method failed", e)
        AlarmBundles.error(AlarmErrorCodes.INTERNAL, "$method failed: ${e.javaClass.simpleName}")
    }

    private fun unsupported(method: String, feature: String): Bundle =
        AlarmBundles.error(AlarmErrorCodes.UNSUPPORTED, "$method needs the feature '$feature', which this plugin build does not declare")

    /** The package of the calling host (the owner of its hint table). zh-CN: 调用方宿主的包名 (其提示表的归属). */
    private fun callingPackage(): String? =
        context.packageManager.getPackagesForUid(Binder.getCallingUid())?.sorted()?.firstOrNull()

    private companion object {
        const val TAG = "ChimeService"
    }
}
