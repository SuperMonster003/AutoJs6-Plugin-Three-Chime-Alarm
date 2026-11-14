package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ThreeChimeAlarmApplication
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingCoordinator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.appearance.screenTexts
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.format.TimeFormats
import org.autojs.plugin.alarm.api.AlarmContract

/**
 * Ringing foreground service (roadmap T5, T6, T35, P2.4). Roadmap P0.2 measured the type on API 34 - 37: `systemExempted`
 * starts from the `setAlarmClock` receiver in every app state, so the service calls `startForeground` with that type on
 * API 34+ and without a type below it, and posts the notification with `FOREGROUND_SERVICE_IMMEDIATE` (the system
 * defers it by up to 10 seconds otherwise). The service follows [RingCoordinator.sounding]: one [RingSession] per
 * sounding round, a partial wake lock while anything sounds, and it stops when nothing does. The notification opens
 * [RingActivity] as its full screen intent when the system allows one (API 34+ `canUseFullScreenIntent()`, T5),
 * otherwise it is a heads-up notification while the sound plays; it carries snooze and dismiss actions. When the
 * plugin may display over other apps and the device is not in use, the service also opens [RingActivity] directly
 * (roadmap Q20, T87), as some systems hold the full screen intent back. Without the notification permission (API 33+)
 * the service still runs and the ring sounds without a notification or screen (T69).
 *
 * zh-CN: 响铃前台服务 (路线图 T5, T6, T35, P2.4). 路线图 P0.2 在 API 34 - 37 实测了类型: `systemExempted` 在任何应用状态下都能从 `setAlarmClock`
 * 的接收器启动, 因此服务在 API 34+ 以该类型调用 `startForeground`, 更低版本不带类型; 通知用 `FOREGROUND_SERVICE_IMMEDIATE` 发出 (否则系统最多推迟
 * 10 秒). 服务跟随 [RingCoordinator.sounding]: 每个发声的一轮一个 [RingSession], 有闹钟发声时持有部分唤醒锁, 没有时停止. 系统允许时 (API 34+
 * `canUseFullScreenIntent()`, T5) 通知以全屏 Intent 打开 [RingActivity], 否则在铃声播放期间作为悬浮通知; 通知带贪睡与关闭按钮. 插件可显示在其他应用
 * 上层且设备未被使用时, 服务还会直接打开 [RingActivity] (路线图 Q20, T87), 因为部分系统会压下全屏 Intent. 没有通知权限 (API 33+) 时服务照常运行,
 * 响铃没有通知与界面但照常发声 (T69).
 */
class RingService : Service() {

    private val main = Handler(Looper.getMainLooper())

    private lateinit var runtime: AlarmRuntime

    private lateinit var wakeLock: PowerManager.WakeLock

    private var session: RingSession? = null

    private var lastStartId = 0

    private val listener: () -> Unit = { main.post(::sync) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        runtime = AlarmRuntime.get(this)
        wakeLock = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
        }
        runtime.rings.addListener(listener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        foreground(runtime.rings.sounding)
        val alarmId = intent?.getStringExtra(EXTRA_ALARM_ID)
        when (intent?.action) {
            ACTION_SNOOZE -> act("snooze") { runtime.rings.snooze(alarmId, null, AlarmContract.BY_USER) }
            ACTION_DISMISS -> act("dismiss") { runtime.rings.dismiss(alarmId, AlarmContract.BY_USER) }
            ACTION_FOCUS -> session?.requestFocus()
        }
        // Runs after the scheduler work queued before this start: the ring that started the service, or the resume
        // after a restart of the process. zh-CN: 在本次启动之前排队的调度工作之后运行: 启动服务的那次响铃, 或进程重启后的接续.
        runtime.executor.execute { main.post(::sync) }
        return START_STICKY
    }

    override fun onDestroy() {
        runtime.rings.removeListener(listener)
        main.removeCallbacksAndMessages(null)
        session?.stop()
        session = null
        if (runtime.rings.sounding == null) runtime.volume.restore()
        if (wakeLock.isHeld) wakeLock.release()
        super.onDestroy()
    }

    private fun act(what: String, block: () -> Boolean) {
        runtime.executor.execute {
            try {
                Log.i(TAG, "$what by user: handled=${block()}")
            } catch (e: RuntimeException) {
                Log.e(TAG, "$what failed", e)
            }
        }
    }

    /** Follows the ring state (main thread). zh-CN: 跟随响铃状态 (主线程). */
    private fun sync() {
        val ring = runtime.rings.sounding
        if (ring == null) {
            idle()
            return
        }
        val current = session
        val fresh = current == null || current.occurrence != ring.occurrence
        if (fresh) {
            current?.stop()
            wakeLock.acquire(ring.settings.autoSilenceMillis + WAKE_LOCK_EXTRA_MILLIS)
            session = RingSession(this, runtime, ring).also { it.begin() }
        } else {
            current.ring = ring
        }
        foreground(ring)
        if (fresh) openDirectly(ring)
    }

    /**
     * Opens the ringing screen of a new round directly when [opensDirectly] says so (roadmap Q20, T87). ColorOS caps the
     * ring channel to the default importance while the app's "Banner" notifications are off and then drops the full
     * screen intent, although `canUseFullScreenIntent()` is true; the overlay permission exempts this start from the
     * background activity limits. A second start of the single instance screen only brings it forward.
     * zh-CN: [opensDirectly] 允许时直接打开新一轮的响铃界面 (路线图 Q20, T87). 应用的 "横幅" 通知关闭时, ColorOS 把响铃通知类别压到默认重要性并丢弃全屏
     * Intent, 尽管 `canUseFullScreenIntent()` 为 true; 悬浮窗权限让这次启动不受后台启动 Activity 的限制. 单实例界面被再次启动时只会回到前台.
     */
    private fun openDirectly(ring: RingCoordinator.Ring) {
        if (!opensDirectly(this)) return
        try {
            startActivity(Intent(this, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION))
            Log.i(TAG, "ring screen ${ring.alarmId} opened directly")
        } catch (e: RuntimeException) {
            Log.w(TAG, "ring screen direct open refused: ${e.message}")
        }
    }

    private fun idle() {
        session?.stop()
        session = null
        runtime.volume.restore()
        if (wakeLock.isHeld) wakeLock.release()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        // A start that came in meanwhile keeps the service. zh-CN: 期间到来的启动请求会保留服务.
        stopSelf(lastStartId)
    }

    private fun foreground(ring: RingCoordinator.Ring?) {
        val notification = notification(ring)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "startForeground refused", e)
        }
    }

    private fun notification(ring: RingCoordinator.Ring?): Notification {
        // The texts follow the language of the plugin's screens, not the system (roadmap Q21). zh-CN: 文字跟随插件界面语言而非系统 (路线图 Q21).
        val texts = screenTexts(this)
        val builder = NotificationCompat.Builder(this, ThreeChimeAlarmApplication.CHANNEL_RING)
            // A 24 dp glyph: the monochrome layer of the adaptive icon keeps its safe zone and shows at half the size of the
            // system icons (roadmap Q22). zh-CN: 24 dp 图形: 自适应图标的单色层带着安全区留白, 显示时只有系统图标的一半大 (路线图 Q22).
            .setSmallIcon(R.drawable.ic_alarm_24)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
        if (ring == null) return builder.setContentTitle(texts.getString(R.string.alarm_default_label)).build()
        val label = ring.label?.takeIf { it.isNotBlank() } ?: texts.getString(if (ring.isTimer) R.string.timer_default_label else R.string.alarm_default_label)
        val screen = ringScreen()
        val fullScreen = fullScreenAllowed(this)
        val snooze = ring.userCanSnooze && runtime.schedule.canSnooze()
        val queued = runtime.rings.queued.size
        builder.setContentTitle(label)
            .setContentText(TimeFormats.minuteOf(texts, ring.request.scheduledAt))
            .setContentIntent(screen)
        if (queued > 0) builder.setSubText(texts.resources.getQuantityString(R.plurals.ring_queue, queued, queued))
        if (fullScreen) builder.setFullScreenIntent(screen, true)
        if (snooze) builder.addAction(0, texts.getString(R.string.ring_action_snooze), action(ACTION_SNOOZE, ring.alarmId))
        // Under a dismiss challenge "Dismiss" opens the challenge layer (chime-p9-protocol.md section 3, T289).
        // zh-CN: 关闭挑战下 "关闭" 打开挑战层 (chime-p9-protocol.md 第 3 节, T289).
        builder.addAction(0, texts.getString(R.string.ring_action_dismiss), if (ring.challenge != null) screen else action(ACTION_DISMISS, ring.alarmId))
        if (ring.occurrence != shown) {
            shown = ring.occurrence
            Log.i(TAG, "ring notification ${ring.alarmId} posted=${NotificationManagerCompat.from(this).areNotificationsEnabled()} fullScreen=$fullScreen snooze=$snooze challenge=${ring.challenge ?: "none"}")
        }
        return builder.build()
    }

    private var shown: String? = null

    private fun ringScreen(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun action(action: String, alarmId: String): PendingIntent {
        val intent = Intent(this, RingService::class.java).setAction(action).putExtra(EXTRA_ALARM_ID, alarmId)
        val code = if (action == ACTION_SNOOZE) REQUEST_SNOOZE else REQUEST_DISMISS
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(this, code, intent, flags)
        } else {
            PendingIntent.getService(this, code, intent, flags)
        }
    }

    companion object {

        const val TAG = "ChimeRing"

        const val ACTION_SNOOZE = "io.github.supermonster003.autojs6.plugin.three.chime.alarm.action.RING_SNOOZE"
        const val ACTION_DISMISS = "io.github.supermonster003.autojs6.plugin.three.chime.alarm.action.RING_DISMISS"
        const val ACTION_FOCUS = "io.github.supermonster003.autojs6.plugin.three.chime.alarm.action.RING_FOCUS"

        /** The alarm a snooze or dismiss action is for. zh-CN: 贪睡或关闭按钮所针对的闹钟. */
        const val EXTRA_ALARM_ID = "alarmId"

        private const val NOTIFICATION_ID = 1
        private const val REQUEST_SNOOZE = 1
        private const val REQUEST_DISMISS = 2
        private const val WAKE_LOCK_TAG = "ThreeChimeAlarm:ring"

        /** The wake lock outlives the auto silence time by this much. zh-CN: 唤醒锁比自动停止时间多持有这么久. */
        private const val WAKE_LOCK_EXTRA_MILLIS = 60_000L

        /**
         * Starts the service for a ring that was just offered. The start is refused when the process is in the
         * background without an exemption (a resume after the process died, a late ring of a process that a binding
         * started); [rearm] then lets [RingKickReceiver] start it from an alarm wake-up a second later (T97).
         * zh-CN: 为刚收到的响铃启动服务. 进程在后台且没有豁免时 (进程死亡后的接续, 由绑定拉起的进程中的补响) 启动会被拒绝; [rearm] 时由
         * [RingKickReceiver] 在一秒后的闹钟唤醒中启动 (T97).
         */
        fun start(context: Context, rearm: Boolean = true) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, RingService::class.java))
            } catch (e: IllegalStateException) {
                Log.w(TAG, "ring service start refused: ${e.message}")
                if (rearm) RingKickReceiver.arm(context)
            }
        }

        /** Asks the sounding round for audio focus again (T35). zh-CN: 让发声的一轮再次申请音频焦点 (T35). */
        fun focus(context: Context) {
            try {
                context.startService(Intent(context, RingService::class.java).setAction(ACTION_FOCUS))
            } catch (e: IllegalStateException) {
                Log.w(TAG, "ring focus refused: ${e.message}")
            }
        }

        /**
         * Whether a new ring opens its screen directly (roadmap Q20, T87): the plugin may display over other apps and the
         * device is not in use (screen off or locked). While the device is in use the heads-up notification is the alert.
         * zh-CN: 新的响铃是否直接打开界面 (路线图 Q20, T87): 插件可显示在其他应用上层, 且设备未被使用 (熄屏或锁屏). 设备使用中时提醒是悬浮通知.
         */
        fun opensDirectly(context: Context): Boolean {
            if (!Settings.canDrawOverlays(context)) return false
            val interactive = context.getSystemService(PowerManager::class.java).isInteractive
            return !interactive || context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
        }

        /** Whether the ringing screen may open over the lock screen by itself (T5). zh-CN: 响铃界面能否自行在锁屏上打开 (T5). */
        fun fullScreenAllowed(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }
}
