package io.github.supermonster003.autojs6.plugin.three.chime.alarm.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Alarm capability service (roadmap P2.5): answers the `IAlarmPlugin` Binder ([AlarmPluginBinder]) to the hosts that
 * hold the plugin signature permission. The contract AIDL comes from the host's `alarm-api` AAR; the plugin copies none.
 * zh-CN: 闹钟能力服务 (路线图 P2.5): 向持有插件签名权限的宿主返回 `IAlarmPlugin` Binder ([AlarmPluginBinder]). 契约 AIDL 来自宿主的 `alarm-api`
 * AAR, 插件不复制 AIDL.
 */
class ThreeChimeAlarmService : Service() {

    private val binder by lazy { AlarmPluginBinder(this) }

    override fun onBind(intent: Intent?): IBinder = binder

}
