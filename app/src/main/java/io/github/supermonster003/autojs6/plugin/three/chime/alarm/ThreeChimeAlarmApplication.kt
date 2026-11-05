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
        getSystemService(NotificationManager::class.java).createNotificationChannel(ring)
    }

    internal companion object {
        const val CHANNEL_RING = "ring"
    }

}
