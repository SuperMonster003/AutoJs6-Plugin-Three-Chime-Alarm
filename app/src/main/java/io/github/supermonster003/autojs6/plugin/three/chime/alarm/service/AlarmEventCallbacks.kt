package io.github.supermonster003.autojs6.plugin.three.chime.alarm.service

import android.os.IBinder
import android.os.RemoteException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.AlarmEventBus
import org.autojs.plugin.alarm.api.AlarmBundles
import org.autojs.plugin.alarm.api.AlarmEvent
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.IAlarmEventCallback

/**
 * The live event callbacks of the hosts (roadmap P2.5, appendix A section 5.2): every registered `IAlarmEventCallback`
 * gets the events it asked for, in the order of [AlarmEventBus]. A callback is one entry per Binder (registering it
 * again replaces its events, T77), at most [AlarmLimits.MAX_EVENT_CALLBACKS] in the process across all hosts; a dead
 * host process drops its entries through `linkToDeath`, a failing call drops the entry too. Calls are oneway, so a slow
 * host does not hold up the ring. [onListenedChange] tells the scheduler when the union of the events asked for changes, so
 * `beforeRing` wake-ups follow whether anyone listens (roadmap P8.4).
 * zh-CN: 各宿主的实时事件回调 (路线图 P2.5, 附录 A 第 5.2 节): 每个已注册的 `IAlarmEventCallback` 按 [AlarmEventBus] 的顺序收到它请求的事件. 每个
 * Binder 一项 (再次注册替换其事件, T77), 进程内所有宿主合计至多 [AlarmLimits.MAX_EVENT_CALLBACKS] 项; 宿主进程死亡时经 `linkToDeath` 删除其项, 调用失败
 * 同样删除. 调用是 oneway 的, 慢的宿主不会拖住响铃. 请求的事件并集变化时 [onListenedChange] 通知调度器, 使 `beforeRing` 唤醒跟随是否有人监听
 * (路线图 P8.4).
 */
class AlarmEventCallbacks(bus: AlarmEventBus) {

    private class Entry(val callback: IAlarmEventCallback, val events: Set<String>, val death: IBinder.DeathRecipient)

    private val lock = Any()

    private val entries = LinkedHashMap<IBinder, Entry>()

    init {
        bus.addListener(::deliver)
    }

    /**
     * Called outside the lock after a register, unregister or drop changed the union of the events asked for (roadmap P8.4).
     * zh-CN: 注册, 注销或删除改变了请求的事件并集之后, 在锁外调用 (路线图 P8.4).
     */
    @Volatile
    var onListenedChange: (() -> Unit)? = null

    /** The registered callbacks (the leak checks of the instrumentation tests). zh-CN: 已注册的回调数 (仪器测试的泄漏检查). */
    val size: Int get() = synchronized(lock) { entries.size }

    /** Whether a registered callback asked for events of [type]. zh-CN: 是否有已注册的回调请求了 [type] 类型的事件. */
    fun listens(type: String): Boolean = synchronized(lock) { entries.values.any { type in it.events } }

    fun register(callback: IAlarmEventCallback, events: Set<String>) {
        val binder = callback.asBinder()
        val changed = synchronized(lock) {
            val before = listened()
            val current = entries[binder]
            if (current != null) {
                entries[binder] = Entry(callback, events, current.death)
            } else {
                if (entries.size >= AlarmLimits.MAX_EVENT_CALLBACKS) {
                    throw AlarmException.limitExceeded("at most ${AlarmLimits.MAX_EVENT_CALLBACKS} event callbacks")
                }
                val death = IBinder.DeathRecipient { drop(binder) }
                try {
                    binder.linkToDeath(death, 0)
                } catch (_: RemoteException) {
                    return
                }
                entries[binder] = Entry(callback, events, death)
            }
            before != listened()
        }
        if (changed) onListenedChange?.invoke()
    }

    fun unregister(callback: IAlarmEventCallback) {
        drop(callback.asBinder())
    }

    private fun drop(binder: IBinder) {
        var changed = false
        val entry = synchronized(lock) {
            val before = listened()
            entries.remove(binder)?.also { changed = before != listened() }
        } ?: return
        runCatching { binder.unlinkToDeath(entry.death, 0) }
        if (changed) onListenedChange?.invoke()
    }

    private fun listened(): Set<String> = entries.values.flatMapTo(HashSet()) { it.events }

    private fun deliver(event: AlarmEvent) {
        val targets = synchronized(lock) { entries.values.filter { event.type in it.events } }
        if (targets.isEmpty()) return
        val bundle = AlarmBundles.of(event.toValues())
        for (entry in targets) {
            try {
                entry.callback.onEvent(bundle)
            } catch (_: RemoteException) {
                drop(entry.callback.asBinder())
            } catch (_: RuntimeException) {
                drop(entry.callback.asBinder())
            }
        }
    }
}
