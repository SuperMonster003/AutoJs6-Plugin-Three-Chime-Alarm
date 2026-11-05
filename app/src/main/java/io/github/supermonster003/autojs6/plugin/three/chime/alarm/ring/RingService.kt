package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Ringing foreground service (roadmap T6, P2.3). Its type is the one roadmap P0.2 measured; the skeleton stops at once.
 * zh-CN: 响铃前台服务 (路线图 T6, P2.3). 类型取路线图 P0.2 的实测结论; 骨架收到启动后立即停止.
 */
class RingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf(startId)
        return START_NOT_STICKY
    }

}
