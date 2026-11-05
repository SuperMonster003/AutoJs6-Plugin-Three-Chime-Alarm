package io.github.supermonster003.autojs6.plugin.three.chime.alarm.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ThreeChimeAlarmContract

/**
 * Alarm capability service. Until the host contract of roadmap P1.1 exists it answers with a placeholder Binder that only
 * carries the `IAlarmPlugin` descriptor and rejects every transaction (the 3-Macaw Speech P0 approach); the plugin copies
 * no AIDL.
 * zh-CN: 闹钟能力服务. 在路线图 P1.1 的宿主契约落地之前, 它返回只携带 `IAlarmPlugin` 描述符的占位 Binder, 拒绝全部事务
 * (3-Macaw Speech P0 的做法); 插件不复制 AIDL.
 */
class ThreeChimeAlarmService : Service() {

    private val binder = Binder().apply { attachInterface(null, ThreeChimeAlarmContract.CAPABILITY_DESCRIPTOR) }

    override fun onBind(intent: Intent?): IBinder = binder

}
