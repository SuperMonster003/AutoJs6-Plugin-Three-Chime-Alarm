package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import android.app.Activity
import android.os.Bundle

/**
 * Full screen ringing screen (roadmap T5, P2.3), shown over the lock screen. The skeleton only declares it.
 * zh-CN: 全屏响铃界面 (路线图 T5, P2.3), 显示在锁屏之上. 骨架只做声明.
 */
class RingActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }

}
