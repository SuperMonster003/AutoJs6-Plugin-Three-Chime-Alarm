package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmOptionsValidator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.AlarmApi
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.BindingHints
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.HolidayApi
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.TimerApi
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.AlarmEventBus
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.ChangeEvents
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.CatalogSignatureVerifier
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayData
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayJudge
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayRefresher
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.net.CancelToken
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.net.UrlConnectionTransport
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingCoordinator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingVolume
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingtoneLibrary
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedule
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.BootMarker
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.JsonDocumentFile
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.Stopwatch
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerEngine
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.holiday.AndroidAssetSource
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.holiday.HolidaySettings
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.holiday.HolidayUpdateJob
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring.AlarmStreamVolume
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring.KeepAliveService
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring.RingDefaultsStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring.RingService
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring.StoredVolumeMemory
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.schedule.AndroidAlarmSchedulePort
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.service.AlarmEventCallbacks
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.service.AlarmTriggerSender
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.service.AudioVerifier
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.service.SystemRingtones
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.timer.AndroidTimerPort
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.timer.TimerNotifications
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmLimits
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The alarm objects of the plugin process: one [repository] on device-protected storage (roadmap T7, usable before
 * the first unlock), the [schedule] that keeps the system alarm clocks in step with it, the [rings] it hands due rings
 * to (roadmap P2.4) and the [events] of the process. The first use reconciles every alarm once, which also restores
 * the wake-ups a force stop cleared, then picks up the rounds a dead process left ringing (T67). The Binder service
 * (roadmap P2.5) adds the [hints] of the hosts, the imported [ringtones], the live [callbacks], the [triggers] of bound
 * scripts and the [api] its methods run on. Since 1.1.0 (roadmap P7.1 - P7.3) the [timers] ring through the same [rings],
 * and the [stopwatch] counts; [timerApi] serves their Binder methods.
 * zh-CN: 插件进程的闹钟对象: 设备加密存储上的一个 [repository] (路线图 T7, 首次解锁前可用), 让系统闹钟与之保持一致的 [schedule], 接收到时响铃的 [rings]
 * (路线图 P2.4), 以及进程内的 [events]. 首次使用时对全部闹钟对账一次, 同时恢复被强行停止清掉的唤醒, 再接续进程死亡时留下的响铃 (T67). Binder 服务
 * (路线图 P2.5) 另加各宿主的 [hints], 导入的 [ringtones], 实时 [callbacks], 绑定脚本的 [triggers] 以及其方法所用的 [api]. 自 1.1.0 起 (路线图
 * P7.1 - P7.3) [timers] 经同一个 [rings] 响铃, 另有 [stopwatch]; [timerApi] 提供其 Binder 方法.
 */
class AlarmRuntime private constructor(context: Context) {

    val context: Context = context.applicationContext

    /** Device-protected `filesDir` (T7, usable before the first unlock). zh-CN: 设备加密存储的 `filesDir` (T7, 首次解锁前可用). */
    private val deviceFilesDir: File = this.context.createDeviceProtectedStorageContext().filesDir

    val events = AlarmEventBus()

    /** The holiday settings (roadmap P8.2 / P8.3, T176). zh-CN: 节假日设置 (路线图 P8.2 / P8.3, T176). */
    val holidaySettings = HolidaySettings(this.context)

    /**
     * The bundled snapshot and the downloaded holiday set (roadmap P8.2, T174), opened on first use.
     * zh-CN: 内置快照与下载的节假日数据集 (路线图 P8.2, T174), 首次使用时打开.
     */
    private val holidayStore: HolidayStore by lazy {
        HolidayStore(File(deviceFilesDir, HOLIDAY_DIRECTORY), AndroidAssetSource(this.context.assets), CatalogSignatureVerifier.official()) { message, error ->
            Log.w(HolidayUpdateJob.TAG, message, error)
        }
    }

    /**
     * The holiday data in use (roadmap P8.2): loaded on first use, which verifies the signature of the catalog once.
     * zh-CN: 正在使用的节假日数据 (路线图 P8.2): 首次使用时加载, 此时校验一次目录签名.
     */
    val holidays: HolidayData by lazy { HolidayData(holidayStore.load()) { holidaySettings.defaultRegion(holidays.set.catalog) } }

    /**
     * The judgement of the rule engine (roadmap P8.3): unknown (the weekday rules) when the data cannot be opened.
     * zh-CN: 规则引擎的判定 (路线图 P8.3): 数据无法打开时为未知 (按星期规则).
     */
    val holidayJudge = HolidayJudge { date, region ->
        try {
            holidays.day(date, region)
        } catch (e: Exception) {
            val now = SystemClock.elapsedRealtime()
            if (now - holidayFailureLoggedAt > HOLIDAY_FAILURE_LOG_INTERVAL_MILLIS || holidayFailureLoggedAt == 0L) {
                holidayFailureLoggedAt = now
                Log.e(HolidayUpdateJob.TAG, "holiday data unavailable", e)
            }
            null
        }
    }

    /** When the last failure of [holidayJudge] was logged (one line a minute at most). zh-CN: 上次记录 [holidayJudge] 失败的时刻 (至多每分钟一行). */
    @Volatile
    private var holidayFailureLoggedAt = 0L

    /**
     * The canonical form of an explicit `holidays.region` (T182): an unknown or malformed region throws as
     * `HolidayDataSet.region` does; the region is kept as given when the data cannot be opened at all.
     * zh-CN: 显式 `holidays.region` 的规范形式 (T182): 未知或格式错误的地区按 `HolidayDataSet.region` 抛出; 数据完全无法打开时保持原样.
     */
    private val holidayRegion: (String) -> String = { text ->
        try {
            holidays.set.region(text).code
        } catch (e: AlarmException) {
            throw e
        } catch (e: Exception) {
            text
        }
    }

    /** The validator of the alarm options, with the features of this build (T25). zh-CN: 闹钟选项的校验器, 带本构建的功能 (T25). */
    private val alarmValidator = AlarmOptionsValidator(features = ThreeChimeAlarmContract.FEATURES.toSet())

    val port = AndroidAlarmSchedulePort(this.context) { record, request ->
        rings.offer(record, request)
        RingService.start(this.context)
    }

    val repository: AlarmRepository = AlarmRepository(
        store = AlarmStore(File(deviceFilesDir, STORE_DIRECTORY)),
        validator = alarmValidator,
        planner = { schedule.plan(it) },
        schedulingAllowed = port::canScheduleExact,
        holidayJudge = holidayJudge,
        holidayRegion = holidayRegion,
    )

    val schedule: AlarmSchedule = AlarmSchedule(repository, port, events, judge = holidayJudge, beforeRingLead = ::beforeRingLead)

    /** The plugin defaults of the settings screen (T84). zh-CN: 设置页的插件默认值 (T84). */
    val defaults = RingDefaultsStore(this.context)

    private val validator = AlarmOptionsValidator()

    /** The boot count, which tells a reboot from a process restart (T96, T110). zh-CN: 开机次数, 用于区分重启与进程重启 (T96, T110). */
    private val bootCount: () -> Int? = { runCatching { Settings.Global.getInt(this.context.contentResolver, Settings.Global.BOOT_COUNT) }.getOrNull() }

    /**
     * The Android side of the timers: a due timer joins the ring queue like an alarm (roadmap P7.1), a timer removed
     * without an event asks the hosts to drop its bindings (T113).
     * zh-CN: 计时器的 Android 部分: 到点的计时器像闹钟一样进入响铃队列 (路线图 P7.1), 无事件删除的计时器请宿主删除其绑定 (T113).
     */
    private val timerPort = AndroidTimerPort(
        this.context,
        onRing = { record, request ->
            rings.offerTimer(record, request)
            RingService.start(this.context)
        },
        onRemovedSilently = { id -> triggers.unbind(id) {} },
    )

    /** The timers (roadmap P7.1, T109: `timers.json` next to the alarms). zh-CN: 计时器 (路线图 P7.1, T109: `timers.json` 与闹钟同在). */
    val timers: TimerEngine = TimerEngine(
        store = TimerStore(File(deviceFilesDir, TimerStore.FILE_NAME)) { validator.timerOptions(it) },
        port = timerPort,
        boot = bootCount,
    )

    /** The stopwatch (roadmap P7.2, T118). zh-CN: 秒表 (路线图 P7.2, T118). */
    val stopwatch: Stopwatch = Stopwatch(JsonDocumentFile(File(deviceFilesDir, Stopwatch.FILE_NAME), Stopwatch.FORMAT, Stopwatch.VERSION), boot = bootCount)

    /** The ringing state of alarms and timers (roadmap P2.4, P7.1). zh-CN: 闹钟与计时器的响铃状态 (路线图 P2.4, P7.1). */
    val rings: RingCoordinator = RingCoordinator(repository, schedule, events, defaults = defaults::get, timers = timers, validator = alarmValidator)

    /** The independent volume of rings (T14, T68). zh-CN: 响铃的独立音量 (T14, T68). */
    val volume: RingVolume = RingVolume(AlarmStreamVolume(this.context), StoredVolumeMemory(this.context))

    /** The binding hint tables of the hosts (T12, roadmap P2.5). zh-CN: 各宿主的绑定提示表 (T12, 路线图 P2.5). */
    val hints = BindingHints(File(deviceFilesDir, BindingHints.FILE_NAME))

    /** The imported ringtones (T15, T72). zh-CN: 导入的铃声 (T15, T72). */
    val ringtones = RingtoneLibrary(deviceFilesDir, AudioVerifier::mimeOf)

    /** The live event callbacks of the hosts (T13). zh-CN: 各宿主的实时事件回调 (T13). */
    val callbacks = AlarmEventCallbacks(events)

    /** The `ALARM_TRIGGER` broadcasts of bound scripts (T11, T12). zh-CN: 绑定脚本的 `ALARM_TRIGGER` 广播 (T11, T12). */
    val triggers = AlarmTriggerSender(this.context, hints, events)

    /** The `IAlarmPlugin` methods on value trees (roadmap P2.5). zh-CN: 以值树实现的 `IAlarmPlugin` 方法 (路线图 P2.5). */
    val api = AlarmApi(
        repository, rings, hints, ringtones, { SystemRingtones.list(this.context) }, validator = alarmValidator,
        timerExists = ::timerExists, judge = holidayJudge, holidayRegion = holidayRegion, schedule = schedule,
    )

    /**
     * The same methods for the plugin tools (roadmap P4.1): changes are recorded with source `tool` and dismissals with
     * `by` `tool` (T18, T78).
     * zh-CN: 供插件工具使用的同一组方法 (路线图 P4.1): 变更以来源 `tool` 记录, 关闭以 `by` `tool` 记录 (T18, T78).
     */
    val toolApi = AlarmApi(
        repository, rings, hints, ringtones, { SystemRingtones.list(this.context) },
        validator = alarmValidator, source = AlarmContract.SOURCE_TOOL, by = AlarmContract.BY_TOOL, timerExists = ::timerExists,
        judge = holidayJudge, holidayRegion = holidayRegion,
    )

    /**
     * The holiday methods (roadmap P8.2 / P8.3); the warnings cover the default region and the regions of the enabled alarms
     * that filter by holidays.
     * zh-CN: 节假日方法 (路线图 P8.2 / P8.3); 警告涵盖默认地区以及按节假日过滤的已启用闹钟所用的地区.
     */
    val holidayApi: HolidayApi by lazy {
        HolidayApi(holidays, alarmRegions = { repository.list().filter { it.enabled }.mapNotNull { record -> record.spec.holidays?.let { listOf(it.region) } }.flatten() })
    }

    private val holidayRefresher: HolidayRefresher by lazy {
        HolidayRefresher(holidayStore, holidays, UrlConnectionTransport(userAgent = "Three-Chime-Alarm-holidays/${BuildConfig.VERSION_NAME}"))
    }

    /** One thread for holiday refreshes, apart from the scheduler work. zh-CN: 节假日刷新所用的单线程, 与调度工作分开. */
    private val holidayExecutor: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "chime-holidays") }

    /**
     * Refreshes the holiday data in the background (roadmap P8.2: script `holidays.refresh` and the weekly check) and
     * answers through [done] on the holiday thread. A newer catalog replans the alarms (T8, the sixth moment); a check that
     * succeeds records its time.
     * zh-CN: 在后台刷新节假日数据 (路线图 P8.2: 脚本的 `holidays.refresh` 与每周检查), 在节假日线程上经 [done] 返回结果. 更新的目录会重排闹钟 (T8
     * 第六个时机); 成功的检查记录其时间.
     */
    fun refreshHolidays(cancel: CancelToken, progress: HolidayRefresher.Progress?, done: (Result<HolidayRefresher.Result>) -> Unit) {
        holidayExecutor.execute {
            val result = try {
                Result.success(holidayRefresher.refresh(cancel, progress ?: HolidayRefresher.Progress { _, _ -> }))
            } catch (e: Exception) {
                Result.failure(e)
            }
            result.getOrNull()?.let {
                holidaySettings.lastCheckAt = System.currentTimeMillis()
                if (it.updated) replanHolidays("holiday data ${it.info.version}")
            }
            done(result)
        }
    }

    /**
     * Applies a change of the holiday settings (roadmap P8.6): the weekly check follows its switches, and the alarms are
     * replanned for another default region.
     * zh-CN: 应用节假日设置的改动 (路线图 P8.6): 每周检查跟随其开关, 并为新的默认地区重排闹钟.
     */
    fun holidaySettingsChanged() {
        HolidayUpdateJob.sync(context, holidaySettings)
        replanHolidays("holiday settings")
    }

    private fun replanHolidays(why: String) {
        executor.execute {
            try {
                log(why, schedule.reconcile(AlarmPlanner.Cause.HOLIDAYS).values)
            } catch (e: RuntimeException) {
                Log.e(AndroidAlarmSchedulePort.TAG, "$why: reconcile failed", e)
            }
        }
    }

    /** The timer and stopwatch methods for host scripts (roadmap P7.3). zh-CN: 供宿主脚本使用的计时器与秒表方法 (路线图 P7.3). */
    val timerApi = TimerApi(timers, stopwatch, rings, validator)

    /** The same for the plugin tools (roadmap P7.6, T18). zh-CN: 供插件工具使用的同一组方法 (路线图 P7.6, T18). */
    val toolTimerApi = TimerApi(timers, stopwatch, rings, validator, source = AlarmContract.SOURCE_TOOL, by = AlarmContract.BY_TOOL)

    /** The same for the notification actions and the screens (`user`). zh-CN: 供通知动作与界面使用的同一组方法 (`user`). */
    val userTimerApi = TimerApi(timers, stopwatch, rings, validator, source = AlarmContract.SOURCE_USER, by = AlarmContract.BY_USER)

    /**
     * The same for the system clock intents (roadmap P7.5): timers start with source `intent`, dismissals are `by`
     * `user` (T66, T140).
     * zh-CN: 供系统时钟 Intent 使用的同一组方法 (路线图 P7.5): 计时器以来源 `intent` 启动, 关闭以 `by` `user` 记录 (T66, T140).
     */
    val intentTimerApi = TimerApi(timers, stopwatch, rings, validator, source = AlarmContract.SOURCE_INTENT, by = AlarmContract.BY_USER)

    private fun timerExists(id: String): Boolean = timers.get(id) != null

    /**
     * The `beforeRing` lead of an alarm (roadmap P8.4): the smallest lead of the binding hint rows that ask for it, or the
     * default 60 s when only a live callback listens; null when nobody does, so no wake-up is set.
     * zh-CN: 闹钟的 `beforeRing` 提前量 (路线图 P8.4): 请求该事件的绑定提示行中最小的提前量, 只有实时回调监听时为默认的 60 秒; 无人监听时为 null,
     * 不设唤醒.
     */
    private fun beforeRingLead(alarmId: String): Int? =
        hints.beforeRingLead(alarmId) ?: AlarmLimits.DEFAULT_LEAD_SECONDS.takeIf { callbacks.listens(AlarmContract.EVENT_BEFORE_RING) }

    init {
        // Every change of the alarm list, whoever made it, becomes a `change` event (roadmap P2.5, T18).
        // zh-CN: 闹钟列表的每次变更, 无论由谁发起, 都成为 `change` 事件 (路线图 P2.5, T18).
        repository.addListener { events.emit(ChangeEvents.of(it, System.currentTimeMillis())) }
        // The optional keep-alive service follows the enabled alarms (roadmap Q23 (b), T100). zh-CN: 可选的保活服务跟随启用的闹钟 (路线图 Q23 (b), T100).
        repository.addListener { executor.execute { KeepAliveService.sync(this.context) } }
        // The ongoing notifications follow the timers and the stopwatch (T121); a running timer also keeps the keep-alive
        // service on (T117). zh-CN: 常驻通知跟随计时器与秒表 (T121); 运行中的计时器也让保活服务保持运行 (T117).
        timers.addListener { id, record ->
            TimerNotifications.timer(this.context, id, record)
            executor.execute { KeepAliveService.sync(this.context) }
        }
        stopwatch.addListener { TimerNotifications.stopwatch(this.context, it) }
        // The beforeRing wake-ups follow who listens (roadmap P8.4). zh-CN: beforeRing 唤醒跟随监听者 (路线图 P8.4).
        hints.onBeforeRingChange = { ids -> executor.execute { schedule.beforeRingListenersChanged(ids) } }
        callbacks.onListenedChange = { executor.execute { schedule.beforeRingListenersChanged() } }
    }

    /** One thread for the scheduler work of receivers. zh-CN: 接收器的调度工作所用的单线程. */
    val executor: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "chime-schedule") }

    /**
     * The first start since the boot reconciles as a boot, since it runs before the boot broadcast that started the
     * process (roadmap P5.3, T96). zh-CN: 开机后的第一次启动按开机对账, 因为它先于启动进程的开机广播运行 (路线图 P5.3, T96).
     */
    private val bootMarker = BootMarker(
        current = bootCount,
        stored = { bootPreferences().let { if (it.contains(KEY_BOOT_COUNT)) it.getInt(KEY_BOOT_COUNT, -1) else null } },
        store = { bootPreferences().edit().putInt(KEY_BOOT_COUNT, it).commit() },
    )

    private fun bootPreferences() = context.createDeviceProtectedStorageContext().getSharedPreferences(BOOT_PREFERENCES, Context.MODE_PRIVATE)

    private fun start() {
        executor.execute {
            try {
                val cause = bootMarker.startCause()
                log(if (cause == AlarmPlanner.Cause.BOOT) "start after boot" else "start", schedule.reconcile(cause).values)
                Log.i(AndroidTimerPort.TAG, "start: ${timers.reconcile(cause)} timers rang")
                TimerNotifications.refreshAll(context, timers.list(), stopwatch.state())
                val resumed = rings.resume()
                if (resumed > 0) Log.i(RingService.TAG, "start: $resumed interrupted rings resumed")
                if (rings.sounding != null) RingService.start(context) else volume.restore()
                KeepAliveService.sync(context)
                HolidayUpdateJob.sync(context, holidaySettings)
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

        /** The downloaded holiday sets under device-protected `filesDir` (T174). zh-CN: 下载的节假日数据集所在目录 (T174). */
        const val HOLIDAY_DIRECTORY = "holidays"

        /** Device-protected preferences with the boot count of the previous process start (T96). zh-CN: 保存上次进程启动时开机次数的设备加密偏好. */
        private const val BOOT_PREFERENCES = "boot"
        private const val KEY_BOOT_COUNT = "bootCount"

        /** At most one log line a minute while the holiday data cannot be opened. zh-CN: 节假日数据无法打开时至多每分钟记一行日志. */
        private const val HOLIDAY_FAILURE_LOG_INTERVAL_MILLIS = 60_000L

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
