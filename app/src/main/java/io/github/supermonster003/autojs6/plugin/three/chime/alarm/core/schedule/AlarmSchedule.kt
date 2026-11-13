package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmChange
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.AlarmEventBus
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayFilter
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayJudge
import org.autojs.plugin.alarm.api.AlarmEvent
import java.time.ZoneId

/**
 * What the scheduler needs from the platform (roadmap P2.3): the Android implementation sets one `setAlarmClock` per
 * enabled alarm, starts the ring service and posts the missed alarm notification; the JVM tests record the calls.
 * zh-CN: 调度器对平台的需求 (路线图 P2.3): Android 实现为每个启用的闹钟设一个 `setAlarmClock`, 启动响铃服务并发出错过通知; JVM 测试记录这些调用.
 */
interface AlarmSchedulePort {

    /** False on API 31 - 32 without the exact alarm permission. zh-CN: API 31 - 32 未授予精确闹钟权限时为 false. */
    fun canScheduleExact(): Boolean

    /**
     * Sets the wake-ups of [record] ([io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRuntimeState.wakeAt]
     * as an alarm clock, the skipped instance ([io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRuntimeState.skipWakeAt],
     * `skipNext` or a holiday) as a plain exact wake-up when it comes first) or cancels them when it has none.
     * zh-CN: 设置 [record] 的唤醒 (`wakeAt` 作为闹钟时钟, 被跳过的那次 (`skipWakeAt`, `skipNext` 或节假日) 先到时另设普通精确唤醒), 没有时取消.
     */
    fun schedule(record: AlarmRecord)

    /** Cancels every wake-up of [alarmId]. zh-CN: 取消 [alarmId] 的全部唤醒. */
    fun cancel(alarmId: String)

    /**
     * Hands a due ring to the ring service (roadmap P2.4); called on the thread of [AlarmSchedule.reconcile], inside the
     * window in which the system lets the alarm start a foreground service.
     * zh-CN: 把到时的响铃交给响铃服务 (路线图 P2.4); 在 [AlarmSchedule.reconcile] 的线程上调用, 处于系统允许闹钟启动前台服务的时间窗内.
     */
    fun ring(record: AlarmRecord, request: AlarmPlanner.RingRequest)

    /** Leaves the one notification of a missed ring (T16). zh-CN: 为错过的响铃留一条通知 (T16). */
    fun notifyMissed(record: AlarmRecord, event: AlarmEvent)
}

/**
 * The scheduler of roadmap P2.3 (T3, T8, T16): keeps the platform wake-ups in step with the alarm list (every change
 * of [repository] reschedules that alarm) and runs [AlarmPlanner.reconcile] when an alarm fires and at the T8 moments.
 * Decisions are saved before anything rings or is sent, so a crash between the two never rings an instance twice.
 * zh-CN: 路线图 P2.3 的调度器 (T3, T8, T16): 让平台唤醒与闹钟列表保持一致 (仓库的每次变更都重排该闹钟), 并在闹钟触发与 T8 各时机运行
 * [AlarmPlanner.reconcile]. 决定先保存再响铃或发送, 两者之间崩溃也不会让同一次响两遍.
 */
class AlarmSchedule(
    private val repository: AlarmRepository,
    private val port: AlarmSchedulePort,
    private val events: AlarmEventBus,
    private val clock: () -> Long = System::currentTimeMillis,
    private val deviceZone: () -> ZoneId = ZoneId::systemDefault,
    private val judge: HolidayJudge = HolidayJudge.NONE,
) {

    private val changeListener: (AlarmChange) -> Unit = { change ->
        val after = change.after
        if (after == null) port.cancel(change.alarmId) else port.schedule(after)
    }

    init {
        repository.addListener(changeListener)
    }

    /** The planner of [repository]. zh-CN: [repository] 的 planner. */
    fun plan(record: AlarmRecord) = AlarmPlanner.plan(record, clock(), deviceZone(), judge)

    /**
     * Reconciles [ids] (every alarm when null), then reschedules them: rings what is due, sends `miss` / `skip`,
     * retires spent one-shot alarms. Answers the outcomes by alarm id.
     * zh-CN: 对 [ids] (null 为全部) 对账后重排: 响铃到时的闹钟, 发送 `miss` / `skip`, 让用完的一次性闹钟退役. 返回按闹钟 id 的结果.
     */
    fun reconcile(cause: AlarmPlanner.Cause, ids: Collection<String>? = null): Map<String, AlarmPlanner.Outcome> {
        val now = clock()
        val zone = deviceZone()
        val settled = repository.settle(ids) { record ->
            val outcome = AlarmPlanner.reconcile(record, now, zone, cause, judge)
            AlarmRepository.Settlement(outcome.state, outcome.retire, outcome)
        }
        val schedulable = port.canScheduleExact()
        val result = LinkedHashMap<String, AlarmPlanner.Outcome>()
        for ((record, outcome) in settled) {
            result[record.id] = outcome
            if (schedulable) port.schedule(record) else port.cancel(record.id)
            outcome.events.forEach(events::emit)
            outcome.missed?.let { port.notifyMissed(record, it) }
            outcome.ring?.let { port.ring(record, it) }
        }
        return result
    }

    /** Cancels and sets every wake-up again (the exact alarm permission came back, the plugin started). zh-CN: 全部重排. */
    fun rescheduleAll() {
        val schedulable = port.canScheduleExact()
        repository.list().forEach { if (schedulable) port.schedule(it) else port.cancel(it.id) }
    }

    /** False when a snooze could not be scheduled (T60). zh-CN: 无法排程贪睡时为 false (T60). */
    fun canSnooze(): Boolean = port.canScheduleExact()

    /**
     * Snoozes the sounding round of [alarmId] until [until] as its [count]-th snooze (roadmap P2.4) and sets the wake-up;
     * null for an unknown or disabled alarm, or when the exact alarm permission is missing.
     * zh-CN: 让 [alarmId] 正在响的一轮贪睡到 [until], 记为第 [count] 次贪睡 (路线图 P2.4), 并设置唤醒; 闹钟未知或已禁用, 或缺精确闹钟权限时为 null.
     */
    fun snooze(alarmId: String, until: Long, count: Int): AlarmRecord? {
        if (!port.canScheduleExact()) return null
        if (repository.get(alarmId)?.enabled != true) return null
        val record = repository.updateState(alarmId) { it.state.copy(snoozeUntil = until, snoozeCount = count, ringingAt = null) } ?: return null
        port.schedule(record)
        return record
    }

    /**
     * Ends the ring round of [alarmId] (dismissed, silenced or interrupted, roadmap P2.4): drops its snooze and round,
     * retires a one-shot alarm that has nothing left (T57) and sets the wake-ups again. Null for an unknown alarm.
     * zh-CN: 结束 [alarmId] 的响铃轮次 (关闭, 自动静音或被中断, 路线图 P2.4): 清除贪睡与轮次, 让没有剩余响铃的一次性闹钟退役 (T57), 并重排唤醒. 闹钟未知时为 null.
     */
    fun endRound(alarmId: String): AlarmRecord? {
        val record = repository.settle(listOf(alarmId)) { current ->
            val state = current.state.copy(snoozeUntil = null, snoozeCount = 0, ringSince = null, ringingAt = null)
            AlarmRepository.Settlement(state, !HolidayFilter.repeats(current.spec) && state.nextTrigger == null && state.holidaySkipAt == null, Unit)
        }.firstOrNull()?.first ?: return null
        if (port.canScheduleExact()) port.schedule(record) else port.cancel(record.id)
        return record
    }

    /**
     * The alarms whose round was sounding or waiting when the process died: a saved round that is not snoozed.
     * zh-CN: 进程死亡时正在响或在排队的闹钟: 已保存且未在贪睡的轮次.
     */
    fun interrupted(): List<AlarmRecord> = repository.list().filter { it.enabled && it.state.ringingAt != null && it.state.snoozeUntil == null }
}
