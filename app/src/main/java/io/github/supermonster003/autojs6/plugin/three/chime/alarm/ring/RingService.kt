package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Ringing foreground service (roadmap T6, T35, P2.4). Roadmap P0.2 measured the type on API 34 - 37: `systemExempted`
 * starts from the `setAlarmClock` receiver in every app state. P2.4 calls `startForeground` with that type on API 34+
 * and without a type below it, posts the notification with `FOREGROUND_SERVICE_IMMEDIATE` (the system defers it by up
 * to 10 seconds otherwise), and keeps ringing when audio focus is denied (T35). The skeleton stops at once.
 * zh-CN: 响铃前台服务 (路线图 T6, T35, P2.4). 路线图 P0.2 在 API 34 - 37 实测了类型: `systemExempted` 在任何应用状态
 * 下都能从 `setAlarmClock` 的接收器启动. P2.4 在 API 34+ 以该类型调用 `startForeground`, 更低版本不带类型; 通知用
 * `FOREGROUND_SERVICE_IMMEDIATE` 发出 (否则系统最多推迟 10 秒); 音频焦点被拒时照常响铃 (T35). 骨架收到启动后立即停止.
 */
class RingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf(startId)
        return START_NOT_STICKY
    }

}
