package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event

import org.autojs.plugin.alarm.api.AlarmEvent
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The events of the plugin process (appendix A section 5.1) before they leave it: the scheduler (roadmap P2.3) and the
 * ring service (P2.4) [emit] them, the Binder service (P2.5) delivers them to the registered host callbacks and the
 * trigger broadcast. A failing listener does not stop the others.
 * zh-CN: 插件进程内离开进程前的事件 (附录 A 第 5.1 节): 调度器 (路线图 P2.3) 与响铃服务 (P2.4) 调用 [emit], Binder 服务 (P2.5) 把它们投递给已注册的
 * 宿主回调与触发广播. 一个监听器出错不影响其他监听器.
 */
class AlarmEventBus {

    private val listeners = CopyOnWriteArrayList<(AlarmEvent) -> Unit>()

    fun addListener(listener: (AlarmEvent) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (AlarmEvent) -> Unit) {
        listeners -= listener
    }

    fun emit(event: AlarmEvent) {
        for (listener in listeners) {
            try {
                listener(event)
            } catch (_: RuntimeException) {
                // zh-CN: 监听器出错不影响其他监听器.
            }
        }
    }
}
