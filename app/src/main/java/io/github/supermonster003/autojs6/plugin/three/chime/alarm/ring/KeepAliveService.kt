package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ThreeChimeAlarmApplication
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.KeepAlivePolicy
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.KeepAliveSettings
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.appearance.screenTexts
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.settings.SettingsActivity

/**
 * The optional keep-alive foreground service of "Keep running in background" (roadmap Q23 (b), T100 - T102). Some
 * systems (ColorOS with the app off in Settings > Apps > Auto launch) block the alarm broadcast that would start a dead
 * process, so the alarm does not ring once the process was killed in the background; a foreground service keeps the
 * process out of the background kills. It does nothing else: no wake lock, no timer, a notification of the minimum
 * importance on its own channel. It runs while the setting is on and an alarm is enabled ([KeepAlivePolicy]), is
 * started again by the system after its process died (sticky), and is independent of [RingService], which still rings
 * every alarm. Its type is the `systemExempted` of [RingService] (T6): the plugin holds the exact alarm permission, the
 * type needs no further permission and may start after the boot. A start refused in the background (API 31+) is
 * retried once from the alarm wake-up of [RingKickReceiver] (T97).
 *
 * zh-CN: "保持后台运行" 的可选保活前台服务 (路线图 Q23 (b), T100 - T102). 部分系统 (ColorOS 在 设置 > 应用 > 自动启动 里关闭本应用时) 会拦下拉起已死
 * 进程的闹钟广播, 进程在后台被杀后闹钟就不响; 前台服务让进程不在后台清理之列. 它不做别的事: 不持唤醒锁, 不设定时, 单独的渠道上一条最低重要性的通知.
 * 设置打开且有启用的闹钟时运行 ([KeepAlivePolicy]), 进程死亡后由系统重新启动 (sticky), 与 [RingService] 互不依赖, 响铃仍由后者负责. 类型与
 * [RingService] 相同为 `systemExempted` (T6): 插件持有精确闹钟权限, 该类型无需其他权限, 开机后也能启动. 在后台 (API 31+) 启动被拒时, 由
 * [RingKickReceiver] 的闹钟唤醒再试一次 (T97).
 */
class KeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        foreground()
        // The setting or the alarms may have changed while the start was on its way. zh-CN: 启动途中设置或闹钟可能已变化.
        val context = applicationContext
        AlarmRuntime.get(context).executor.execute { sync(context, rearm = false) }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        Log.i(TAG, "keep-alive stopped")
        super.onDestroy()
    }

    private fun foreground() {
        val notification = notification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            running = true
            Log.i(TAG, "keep-alive running")
        } catch (e: RuntimeException) {
            Log.e(TAG, "keep-alive startForeground refused", e)
            stopSelf()
        }
    }

    private fun notification(): Notification {
        // The texts follow the language of the plugin's screens (roadmap Q21). zh-CN: 文字跟随插件界面语言 (路线图 Q21).
        val texts = screenTexts(this)
        val settings = PendingIntent.getActivity(
            this,
            0,
            Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, ThreeChimeAlarmApplication.CHANNEL_KEEP_ALIVE)
            .setSmallIcon(R.drawable.ic_alarm_24)
            .setContentTitle(texts.getString(R.string.keep_alive_notification_title))
            .setContentText(texts.getString(R.string.keep_alive_notification_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(texts.getString(R.string.keep_alive_notification_text)))
            .setContentIntent(settings)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .build()
    }

    companion object {

        const val TAG = "ChimeKeepAlive"

        /** Distinct from the ringing notification of [RingService] (1). zh-CN: 与 [RingService] 的响铃通知 (1) 不同. */
        private const val NOTIFICATION_ID = 2

        private const val PREFERENCES = "keep-alive"

        /**
         * Whether the service runs in the foreground in this process: set once `startForeground` returned, cleared when it is
         * stopped. A stop is only sent after that, as stopping a service that was started with `startForegroundService` and
         * has not called `startForeground` yet crashes the app.
         * zh-CN: 服务是否已在本进程中以前台运行: `startForeground` 返回后置位, 停止时清除. 只在置位之后才发出停止, 因为停止一个以
         * `startForegroundService` 启动且尚未调用 `startForeground` 的服务会让应用崩溃.
         */
        @Volatile
        var running = false
            private set

        /** The setting in device-protected storage (T100). zh-CN: 设备加密存储中的设置 (T100). */
        fun settings(context: Context): KeepAliveSettings {
            val preferences = context.createDeviceProtectedStorageContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            return KeepAliveSettings(object : KeepAliveSettings.Storage {
                override fun getBoolean(key: String, default: Boolean) = runCatching { preferences.getBoolean(key, default) }.getOrDefault(default)

                // The KTX `edit` drops the result of `commit`. zh-CN: KTX 的 `edit` 会丢掉 `commit` 的结果.
                @SuppressLint("ApplySharedPref", "UseKtx")
                override fun putBoolean(key: String, value: Boolean) = runCatching { preferences.edit().putBoolean(key, value).commit() }.getOrDefault(false)
            })
        }

        /**
         * Starts or stops the service as [KeepAlivePolicy] says; called after every change of the alarm list, after the
         * start reconciliation of the process, when the setting changes and from the kick wake-up. A start refused
         * because the process is in the background without an exemption arms the kick when [rearm] (T97).
         * zh-CN: 按 [KeepAlivePolicy] 启动或停止服务; 在闹钟列表每次变化后, 进程启动对账后, 设置变化时以及补启动唤醒中调用. 进程在后台且没有豁免而启动被拒时,
         * 若 [rearm] 则设置补启动唤醒 (T97).
         */
        fun sync(context: Context, rearm: Boolean = true) {
            val app = context.applicationContext
            val runtime = AlarmRuntime.get(app)
            val shouldRun = KeepAlivePolicy.shouldRun(settings(app).enabled, runtime.repository.list(), runtime.timers.active())
            when (KeepAlivePolicy.action(shouldRun, running)) {
                KeepAlivePolicy.Action.START -> try {
                    ContextCompat.startForegroundService(app, Intent(app, KeepAliveService::class.java))
                    Log.i(TAG, "keep-alive start")
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "keep-alive start refused: ${e.message}")
                    if (rearm) RingKickReceiver.arm(app)
                }
                KeepAlivePolicy.Action.STOP -> {
                    // Cleared first, so a start right after this one creates the service again. zh-CN: 先清除, 紧接着的启动会重新创建服务.
                    running = false
                    app.stopService(Intent(app, KeepAliveService::class.java))
                    Log.i(TAG, "keep-alive stop")
                }
                KeepAlivePolicy.Action.NONE -> Unit
            }
        }
    }
}
