package io.github.supermonster003.autojs6.plugin.three.chime.alarm.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.autojs.plugin.common.api.IPluginInfoProvider
import org.autojs.plugin.common.api.PluginInfo

/**
 * Answers `org.autojs.plugin.INFO` (category `three-chime-alarm`) so the Plugin Center lists the alarm identity of this
 * package before and independently of the capability service.
 * zh-CN: 响应 `org.autojs.plugin.INFO` (category `three-chime-alarm`), 使插件中心独立于能力服务显示本包的闹钟身份.
 */
class ThreeChimeAlarmInfoService : Service() {

    private val binder = object : IPluginInfoProvider.Stub() {
        override fun getInfo(): PluginInfo = pluginRuntimeFields().toPluginInfo()
    }

    override fun onBind(intent: Intent?): IBinder = binder

}
