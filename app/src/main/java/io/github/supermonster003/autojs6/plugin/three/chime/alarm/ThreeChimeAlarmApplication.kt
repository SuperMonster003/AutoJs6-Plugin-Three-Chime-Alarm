package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * The application. Ringing components are direct boot aware (roadmap T7), so this class runs before the first unlock and
 * must not touch credential encrypted storage.
 * zh-CN: 应用类. 响铃组件支持直接启动 (路线图 T7), 因此本类可能在首次解锁前运行, 不得访问凭据加密存储.
 */
class ThreeChimeAlarmApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        // Every process start reconciles the alarms once: a force stop clears the wake-ups and no broadcast says so.
        // zh-CN: 每次进程启动都对账一次: 强行停止会清掉唤醒, 且没有广播通知.
        AlarmRuntime.get(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        // The ringing channel carries the full screen intent and the ringing notification (roadmap T5); the sound comes
        // from the ringing service, so the channel itself is silent.
        // zh-CN: 响铃渠道承载全屏 Intent 与响铃通知 (路线图 T5); 声音由响铃服务播放, 渠道本身静音.
        val ring = NotificationChannel(CHANNEL_RING, getString(R.string.notification_channel_ring_name), NotificationManager.IMPORTANCE_HIGH).apply {
            description = getString(R.string.notification_channel_ring_description)
            setSound(null, null)
            enableVibration(false)
        }
        // One notification per missed ring (roadmap T16). zh-CN: 每次错过的响铃一条通知 (路线图 T16).
        val missed = NotificationChannel(CHANNEL_MISSED, getString(R.string.notification_channel_missed_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = getString(R.string.notification_channel_missed_description)
        }
        // The optional keep-alive service (roadmap Q23 (b), T100): minimum importance, no sound, no status bar icon.
        // zh-CN: 可选的保活服务 (路线图 Q23 (b), T100): 最低重要性, 无声音, 不在状态栏显示图标.
        val keepAlive = NotificationChannel(CHANNEL_KEEP_ALIVE, getString(R.string.notification_channel_keep_alive_name), NotificationManager.IMPORTANCE_MIN).apply {
            description = getString(R.string.notification_channel_keep_alive_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        // The ongoing timer and stopwatch notifications (roadmap P7.1 / P7.2, T121): low importance, silent, no badge.
        // zh-CN: 计时器与秒表的常驻通知 (路线图 P7.1 / P7.2, T121): 低重要性, 静音, 不显示角标.
        val timer = NotificationChannel(CHANNEL_TIMER, getString(R.string.notification_channel_timer_name), NotificationManager.IMPORTANCE_LOW).apply {
            description = getString(R.string.notification_channel_timer_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(ring, missed, keepAlive, timer))
    }

    internal companion object {
        const val CHANNEL_RING = "ring"
        const val CHANNEL_MISSED = "missed"
        const val CHANNEL_KEEP_ALIVE = "keep-alive"
        const val CHANNEL_TIMER = "timer"
    }

}
