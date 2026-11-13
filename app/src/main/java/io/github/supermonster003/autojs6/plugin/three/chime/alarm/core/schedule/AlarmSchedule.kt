package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmChange
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmOptionsValidator
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

    /**
     * Sets the `beforeRing` wake-up of [alarmId] at [at] (roadmap P8.4, a plain exact wake-up that is not shown as the next
     * alarm) or cancels it when [at] is null.
     * zh-CN: 在 [at] 设置 [alarmId] 的 `beforeRing` 唤醒 (路线图 P8.4, 普通精确唤醒, 不显示为下一个闹钟), [at] 为 null 时取消.
     */
    fun scheduleBeforeRing(alarmId: String, at: Long?) = Unit

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
 * [beforeRingLead] tells for an alarm id whether anyone listens for `beforeRing` and how early (roadmap P8.4).
 * zh-CN: 路线图 P2.3 的调度器 (T3, T8, T16): 让平台唤醒与闹钟列表保持一致 (仓库的每次变更都重排该闹钟), 并在闹钟触发与 T8 各时机运行
 * [AlarmPlanner.reconcile]. 决定先保存再响铃或发送, 两者之间崩溃也不会让同一次响两遍. [beforeRingLead] 按闹钟 id 给出是否有人监听 `beforeRing` 以及提前量 (路线图 P8.4).
 */
class AlarmSchedule(
    private val repository: AlarmRepository,
    private val port: AlarmSchedulePort,
    private val events: AlarmEventBus,
    private val clock: () -> Long = System::currentTimeMillis,
    private val deviceZone: () -> ZoneId = ZoneId::systemDefault,
    private val judge: HolidayJudge = HolidayJudge.NONE,
    private val beforeRingLead: (String) -> Int? = { null },
) {

    private val changeListener: (AlarmChange) -> Unit = { change ->
        val after = change.after
        if (after == null) port.cancel(change.alarmId) else arm(after)
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
            if (schedulable) arm(record) else port.cancel(record.id)
            outcome.events.forEach(events::emit)
            outcome.missed?.let { port.notifyMissed(record, it) }
            outcome.ring?.let { port.ring(record, it) }
        }
        return result
    }

    /** Cancels and sets every wake-up again (the exact alarm permission came back, the plugin started). zh-CN: 全部重排. */
    fun rescheduleAll() {
        val schedulable = port.canScheduleExact()
        repository.list().forEach { if (schedulable) arm(it) else port.cancel(it.id) }
    }

    /**
     * Sets or cancels the `beforeRing` wake-ups of [ids] (every alarm when null) after the listeners changed: a binding
     * hint row or a live callback started or stopped asking for `beforeRing`, or its lead changed (roadmap P8.4).
     * zh-CN: 监听者变化后设置或取消 [ids] (为 null 时为全部闹钟) 的 `beforeRing` 唤醒: 绑定提示行或实时回调开始或不再请求 `beforeRing`, 或提前量改变
     * (路线图 P8.4).
     */
    fun beforeRingListenersChanged(ids: Collection<String>? = null) {
        val now = clock()
        for (record in repository.list()) {
            if (ids == null || record.id in ids) port.scheduleBeforeRing(record.id, AlarmPlanner.beforeRingAt(record, beforeRingLead(record.id), now))
        }
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
        arm(record)
        return record
    }

    /**
     * Ends the ring round of [alarmId] (dismissed, silenced or interrupted, roadmap P2.4): drops its snooze and round,
     * retires a one-shot alarm that has nothing left (T57) and sets the wake-ups again. Null for an unknown alarm.
     * zh-CN: 结束 [alarmId] 的响铃轮次 (关闭, 自动静音或被中断, 路线图 P2.4): 清除贪睡与轮次, 让没有剩余响铃的一次性闹钟退役 (T57), 并重排唤醒. 闹钟未知时为 null.
     */
    fun endRound(alarmId: String): AlarmRecord? {
        val record = repository.settle(listOf(alarmId)) { current ->
            val state = current.state.copy(snoozeUntil = null, snoozeCount = 0, ringSince = null, ringingAt = null, ringOverrides = null)
            AlarmRepository.Settlement(state, !HolidayFilter.repeats(current.spec) && state.nextTrigger == null && state.holidaySkipAt == null, Unit)
        }.firstOrNull()?.first ?: return null
        if (port.canScheduleExact()) arm(record) else port.cancel(record.id)
        return record
    }

    /**
     * The `beforeRing` wake-up of [alarmId] fired (roadmap P8.4): when its planned ring is still ahead of the deadline and
     * someone listens, saves that the event went out and sends it. Null when nothing was sent (a stale or early wake-up
     * sets the wake-up again).
     * zh-CN: [alarmId] 的 `beforeRing` 唤醒到时 (路线图 P8.4): 计划响铃仍在截止时刻之前且有人监听时, 先保存已发出再发送事件. 没有发送时为 null (过期或提前的
     * 唤醒会重新设置).
     */
    fun beforeRing(alarmId: String): AlarmEvent? {
        val now = clock()
        var event: AlarmEvent? = null
        val record = repository.updateState(alarmId) { current ->
            val at = AlarmPlanner.beforeRingAt(current, beforeRingLead(current.id), now)
            val scheduledAt = current.state.nextTrigger
            if (at == null || scheduledAt == null || now < at - AlarmPlanner.EARLY_TOLERANCE_MILLIS) return@updateState current.state
            event = AlarmPlanner.beforeRingEvent(current, scheduledAt, now)
            current.state.copy(beforeRingFor = scheduledAt, beforeRingSkip = null, beforeRingOverrides = null)
        } ?: return null
        if (port.canScheduleExact()) arm(record) else port.cancel(record.id)
        event?.let(events::emit)
        return event
    }

    /**
     * Takes a `beforeRing` decision of a script (roadmap P8.4, `decideBeforeRing`): [skip] with an optional [reason], or
     * `ring` with optional [overrides] ([BeforeRingOverrides]), checked on the alarm with [validator] (`INVALID_OPTIONS`).
     * Answers false when the decision is ignored: an unknown alarm or [occurrence], a decision after the deadline, or one
     * for an instance a skip already decided.
     * zh-CN: 接收脚本的 `beforeRing` 决定 (路线图 P8.4, `decideBeforeRing`): [skip] 加可选的 [reason], 或 `ring` 加可选的 [overrides] ([BeforeRingOverrides]),
     * 以 [validator] 按该闹钟检查 (`INVALID_OPTIONS`). 决定被忽略时返回 false: 闹钟或 [occurrence] 未知, 截止之后的决定, 或已被跳过决定的那一次.
     */
    fun decide(alarmId: String, occurrence: String, skip: Boolean, reason: String?, overrides: Map<String, Any?>?, validator: AlarmOptionsValidator): Boolean {
        val now = clock()
        var taken = false
        repository.updateState(alarmId) { current ->
            val state = current.state
            if (!skip && overrides != null) BeforeRingOverrides.check(current.spec, overrides, validator)
            val scheduledAt = state.beforeRingFor
            if (scheduledAt == null || !current.enabled || AlarmPlanner.occurrenceOf(current.id, scheduledAt) != occurrence) return@updateState state
            if (now >= AlarmPlanner.deadlineOf(scheduledAt) || state.beforeRingSkip != null) return@updateState state
            taken = true
            when {
                skip -> state.copy(beforeRingSkip = reason.orEmpty(), beforeRingOverrides = null)
                overrides.isNullOrEmpty() -> state
                else -> {
                    val merged = BeforeRingOverrides.merge(state.beforeRingOverrides, overrides)
                    try {
                        validator.patch(current.spec, merged)
                    } catch (e: AlarmException) {
                        throw AlarmException(e.code, "with the overrides of earlier decisions: ${e.detail}")
                    }
                    state.copy(beforeRingOverrides = merged)
                }
            }
        }
        return taken
    }

    /** Sets or cancels every wake-up of [record], the `beforeRing` one included. zh-CN: 设置或取消 [record] 的全部唤醒, 含 `beforeRing`. */
    private fun arm(record: AlarmRecord) {
        port.schedule(record)
        port.scheduleBeforeRing(record.id, AlarmPlanner.beforeRingAt(record, beforeRingLead(record.id), clock()))
    }

    /**
     * The alarms whose round was sounding or waiting when the process died: a saved round that is not snoozed.
     * zh-CN: 进程死亡时正在响或在排队的闹钟: 已保存且未在贪睡的轮次.
     */
    fun interrupted(): List<AlarmRecord> = repository.list().filter { it.enabled && it.state.ringingAt != null && it.state.snoozeUntil == null }
}
