package io.github.supermonster003.autojs6.plugin.three.chime.alarm.intent

import android.app.Activity
import android.os.Bundle

/**
 * Target of the system clock intents (roadmap D21). The skeleton accepts and ignores them; roadmap P2 handles
 * `SET_ALARM`, `SHOW_ALARMS`, `DISMISS_ALARM` and `SNOOZE_ALARM` with the source `intent`.
 * zh-CN: 系统时钟 Intent 的目标 (路线图 D21). 骨架接收后直接忽略; 路线图 P2 以来源 `intent` 处理 `SET_ALARM`, `SHOW_ALARMS`,
 * `DISMISS_ALARM` 与 `SNOOZE_ALARM`.
 */
class ClockIntentActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }

}
