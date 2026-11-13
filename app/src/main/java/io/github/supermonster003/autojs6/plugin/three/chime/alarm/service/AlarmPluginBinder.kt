package io.github.supermonster003.autojs6.plugin.three.chime.alarm.service

import android.content.Context
import android.os.Binder
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayRefresher
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.net.CancelToken
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.net.OperationCancelledException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.holiday.HolidayUpdateJob
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring.RingService
import org.autojs.plugin.alarm.api.AlarmBundles
import org.autojs.plugin.alarm.api.AlarmCapabilityKeys
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes
import org.autojs.plugin.alarm.api.AlarmResultKeys
import org.autojs.plugin.alarm.api.IAlarmEventCallback
import org.autojs.plugin.alarm.api.IAlarmPlugin
import org.autojs.plugin.alarm.api.IAlarmProgressCallback
import org.autojs.plugin.common.api.PluginInfo
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
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

    /** The running holiday refreshes by operation id (roadmap P8.2). zh-CN: 按操作 id 记录的进行中的节假日刷新 (路线图 P8.2). */
    private val refreshes = ConcurrentHashMap<Int, CancelToken>()

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
            holidayCapabilities(this)
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

    override fun startTimer(request: Bundle?): Bundle = call("startTimer", request) { runtime.timerApi.startTimer(it) }

    override fun getTimer(request: Bundle?): Bundle = call("getTimer", request) { runtime.timerApi.getTimer(it) }

    override fun listTimers(query: Bundle?): Bundle = call("listTimers", query) { runtime.timerApi.listTimers(it) }

    override fun controlTimer(request: Bundle?): Bundle = call("controlTimer", request) { runtime.timerApi.controlTimer(it) }

    override fun stopwatch(request: Bundle?): Bundle = call("stopwatch", request) { runtime.timerApi.stopwatch(it) }

    override fun holidayRegions(query: Bundle?): Bundle = call("holidayRegions", query) { runtime.holidayApi.regions(it) }

    override fun checkDate(request: Bundle?): Bundle = call("checkDate", request) { runtime.holidayApi.checkDate(it) }

    override fun listHolidays(request: Bundle?): Bundle = call("listHolidays", request) { runtime.holidayApi.listHolidays(it) }

    override fun holidayInfo(query: Bundle?): Bundle = call("holidayInfo", query) { runtime.holidayApi.info(it) }

    /**
     * Starts a refresh of the whole holiday data (roadmap P8.2, T175) and answers its operation id; progress, the new
     * `info` or the failure arrive through [callback]. A request that cannot start (unknown key, malformed region) throws
     * `IllegalArgumentException` with the encoded error, which Binder carries to the host (appendix A section 4.1).
     * zh-CN: 开始刷新整套节假日数据 (路线图 P8.2, T175) 并返回操作 id; 进度, 新的 `info` 或失败经 [callback] 送达. 无法开始的请求 (未知键, 地区格式
     * 错误) 抛出带编码错误的 `IllegalArgumentException`, 由 Binder 传给宿主 (附录 A 第 4.1 节).
     */
    override fun refreshHolidays(request: Bundle?, callback: IAlarmProgressCallback?): Int {
        try {
            runtime.holidayApi.checkRefresh(AlarmBundles.fromBundle(request))
        } catch (e: AlarmException) {
            throw IllegalArgumentException(e.message)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(AlarmException.of(e).message)
        }
        val op = operations.incrementAndGet()
        val cancel = CancelToken()
        refreshes[op] = cancel
        val progress = callback?.let { target -> HolidayRefresher.Progress { done, total -> runCatching { target.onProgress(op, done, total) } } }
        runtime.refreshHolidays(cancel, progress) { result ->
            refreshes.remove(op)
            val answer = result.fold(
                onSuccess = { AlarmBundles.of(mapOf(AlarmResultKeys.HOLIDAY_INFO to runtime.holidayApi.infoValues(it.info))) },
                onFailure = ::refreshError,
            )
            runCatching { if (result.isSuccess) callback?.onCompleted(op, answer) else callback?.onFailed(op, answer) }
                .onFailure { Log.w(HolidayUpdateJob.TAG, "refresh $op: the host callback failed: ${it.javaClass.simpleName}") }
        }
        return op
    }

    override fun cancelOperation(opId: Int) {
        refreshes.remove(opId)?.cancel()
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

    /**
     * The holiday entries of `getCapabilities` (roadmap P8.2, T173): the data version and the warnings about missing or
     * estimated years. Data that cannot be opened is a warning too, never a failed answer.
     * zh-CN: `getCapabilities` 中的节假日条目 (路线图 P8.2, T173): 数据版本, 以及缺失或推算年份的警告. 数据无法打开时也只是一条警告, 不让应答失败.
     */
    private fun holidayCapabilities(capabilities: Bundle) {
        try {
            capabilities.putString(AlarmCapabilityKeys.HOLIDAY_DATA_VERSION, runtime.holidays.info().version)
            val warnings = runtime.holidayApi.warnings()
            if (warnings.isNotEmpty()) capabilities.putStringArray(AlarmCapabilityKeys.WARNINGS, warnings.toTypedArray())
        } catch (e: Exception) {
            Log.e(TAG, "holiday capabilities failed", e)
            capabilities.putStringArray(AlarmCapabilityKeys.WARNINGS, arrayOf("Holiday data cannot be opened (${e.javaClass.simpleName}); alarms with holiday rules ring by their weekday rules only"))
        }
    }

    private fun refreshError(error: Throwable): Bundle = when (error) {
        is AlarmException -> AlarmBundles.error(error.code, error.detail)
        is OperationCancelledException -> AlarmBundles.error(AlarmErrorCodes.IO_FAILED, "the holiday refresh was cancelled")
        is IOException -> AlarmBundles.error(AlarmErrorCodes.IO_FAILED, "holiday refresh failed: ${error.message ?: error.javaClass.simpleName}")
        else -> {
            Log.e(TAG, "refreshHolidays failed", error)
            AlarmBundles.error(AlarmErrorCodes.INTERNAL, "refreshHolidays failed: ${error.javaClass.simpleName}")
        }
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
