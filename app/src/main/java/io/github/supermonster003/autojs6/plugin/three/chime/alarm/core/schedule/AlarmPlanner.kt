package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRuntimeState
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayFilter
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayJudge
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.NextOccurrence
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmEvent
import org.autojs.plugin.alarm.api.AlarmLimits
import java.time.ZoneId

/**
 * The scheduling decisions of roadmap P2.3 (T3, T8, T16, T55 - T58), free of Android so the JVM tests cover them.
 *
 * Only the next ring of an enabled alarm is planned ([AlarmRuntimeState.nextTrigger]); a running snooze is kept apart in
 * [AlarmRuntimeState.snoozeUntil] and the wake-up is the earlier of the two ([AlarmRuntimeState.wakeAt]). The next ring
 * is the first one after the later of now and [AlarmRuntimeState.handledThrough] (the last ring that rang, was skipped or
 * was missed), so a clock set back does not ring the same instance twice; a handled instance more than
 * [REPLAY_GUARD_MILLIS] ahead of the clock is forgotten (the clock was set back on purpose, T56).
 *
 * [reconcile] runs when an alarm fires and at every T8 moment: a planned ring at most
 * [AlarmLimits.MISSED_RING_WINDOW_MILLIS] (5 minutes) late still rings, a later one does not and gives a `miss` event
 * (`off` after a boot, `late` otherwise) plus one notification (T16); the skipped instance of `skipNext` gives a `skip`
 * event once its time has come. A planned ring that has not come yet is computed again, so a new time zone or clock
 * applies at once.
 *
 * The rings follow the holiday filter of the alarm (roadmap P8.3) as the [HolidayJudge] passed in sees it. The first
 * instance a holiday takes away before the next ring is kept in [AlarmRuntimeState.holidaySkipAt] and gives a `skip`
 * event with reason `holiday` when its time comes (T181); a one-shot alarm whose only instance is such a holiday then
 * retires.
 *
 * A planned ring whose `beforeRing` went out (roadmap P8.4, [AlarmRuntimeState.beforeRingFor]) follows the decisions:
 * a `skip` gives a `skip` event with reason `script` instead of the ring (also when the ring would be late), `ring`
 * overrides go with the [RingRequest] and stay with the rounds of that instance ([AlarmRuntimeState.ringOverrides]).
 *
 * zh-CN: 路线图 P2.3 的排程决策 (T3, T8, T16, T55 - T58), 不依赖 Android, 由 JVM 测试覆盖. 启用的闹钟只计划下一次响铃
 * ([AlarmRuntimeState.nextTrigger]); 正在进行的贪睡另存于 [AlarmRuntimeState.snoozeUntil], 唤醒时刻取两者中较早者 ([AlarmRuntimeState.wakeAt]).
 * 下一次响铃取现在与 [AlarmRuntimeState.handledThrough] (最近一次已响, 已跳过或已错过的响铃) 中较晚者之后的第一次, 因此时钟往回调时同一次不会响两遍;
 * 比时钟超前 [REPLAY_GUARD_MILLIS] 以上的已处理时刻被忽略 (时钟是被有意往回调的, T56). [reconcile] 在闹钟触发与 T8 的各个时机运行: 计划的响铃
 * 迟到不超过 [AlarmLimits.MISSED_RING_WINDOW_MILLIS] (5 分钟) 时照常补响, 超过则不响, 发 `miss` 事件 (开机后为 `off`, 其他为 `late`) 并留一条通知 (T16);
 * `skipNext` 跳过的那次到时发 `skip` 事件. 还没到的计划响铃重新计算, 新的时区或时钟立即生效. 响铃按传入的 [HolidayJudge] 所见的闹钟节假日过滤计算
 * (路线图 P8.3); 下一次响铃之前第一个被节假日拿掉的那一次存于 [AlarmRuntimeState.holidaySkipAt], 到时发原因为 `holiday` 的 `skip` 事件 (T181);
 * 唯一一次就是这种节假日的一次性闹钟随后退役. 已发出 `beforeRing` 的计划响铃 (路线图 P8.4, [AlarmRuntimeState.beforeRingFor]) 按决定处理: `skip` 以原因为
 * `script` 的 `skip` 事件代替响铃 (响铃迟到时也是), `ring` 的覆盖随 [RingRequest] 交出, 并在该次的各轮中保留 ([AlarmRuntimeState.ringOverrides]).
 */
object AlarmPlanner {

    /** A handled ring this far ahead of the clock is forgotten (T56). zh-CN: 比时钟超前这么多的已处理响铃被忽略 (T56). */
    const val REPLAY_GUARD_MILLIS = 24L * 60 * 60 * 1000

    /** An alarm that fires this much before its planned ring counts as due (clock rounding). zh-CN: 提前这么多触发也算到时. */
    const val EARLY_TOLERANCE_MILLIS = 2_000L

    /** Why [reconcile] runs. zh-CN: [reconcile] 运行的原因. */
    enum class Cause {
        /** The alarm clock or the skip wake-up of the alarm fired. zh-CN: 闹钟或跳过的唤醒到时. */
        TRIGGER,

        /**
         * `BOOT_COMPLETED` / `LOCKED_BOOT_COMPLETED` and the first process start after a boot (T96, [BootMarker]): rings
         * missed while the device was off. zh-CN: 开机广播与开机后的第一次进程启动 (T96): 关机期间错过的响铃.
         */
        BOOT,

        /** `TIME_SET` / `TIMEZONE_CHANGED`. zh-CN: 改时间或时区. */
        CLOCK,

        /**
         * `MY_PACKAGE_REPLACED`, the exact alarm permission, a start of the plugin process that is not the first since the
         * boot. zh-CN: 升级, 精确闹钟权限, 非开机后首次的插件进程启动.
         */
        RESTORE,

        /**
         * New holiday data or another default region (roadmap P8.2, T8 the sixth moment): replans like [RESTORE].
         * zh-CN: 节假日数据更新或默认地区改变 (路线图 P8.2, T8 第六个时机): 与 [RESTORE] 一样重排.
         */
        HOLIDAYS,
    }

    /**
     * A ring the ring service starts (roadmap P2.4): [scheduledAt] is the planned instant (the end of the snooze for a
     * ring after a snooze), [snoozeCount] the snoozes before this round, [overrides] the `beforeRing` overrides of the
     * instance (roadmap P8.4, [BeforeRingOverrides]).
     * zh-CN: 交给响铃服务的一次响铃 (路线图 P2.4): [scheduledAt] 为计划时刻 (贪睡后再响为贪睡结束时刻), [snoozeCount] 为本轮之前的贪睡次数,
     * [overrides] 为该次的 `beforeRing` 覆盖 (路线图 P8.4, [BeforeRingOverrides]).
     */
    data class RingRequest(
        val alarmId: String,
        val scheduledAt: Long,
        val firedAt: Long,
        val occurrence: String,
        val snoozeCount: Int,
        val overrides: Map<String, Any?>? = null,
    )

    /**
     * What [reconcile] decided for one alarm: the new [state], whether the alarm is retired (a one-shot alarm that has
     * nothing left, T57), the [ring] to start and the [events] to send (`miss`, `skip`); [missed] is the `miss` event,
     * which also leaves a notification.
     * zh-CN: [reconcile] 对一个闹钟的决定: 新的 [state], 是否退役 (没有剩余响铃的一次性闹钟, T57), 要启动的 [ring] 与要发送的 [events] (`miss`, `skip`);
     * [missed] 为 `miss` 事件, 同时留一条通知.
     */
    data class Outcome(
        val state: AlarmRuntimeState,
        val retire: Boolean = false,
        val ring: RingRequest? = null,
        val events: List<AlarmEvent> = emptyList(),
    ) {
        val missed: AlarmEvent? get() = events.firstOrNull { it.type == AlarmContract.EVENT_MISS }
    }

    /**
     * The `occurrence` of the round that starts with the planned ring at [scheduledAt] (T31): the same value for the
     * `skip`, `miss` and first `ring` of one instance, so a broadcast sent again after a restart is recognized. A round
     * after the n-th snooze appends `~n`.
     * zh-CN: 从 [scheduledAt] 的计划响铃开始的那一轮的 `occurrence` (T31): 同一次的 `skip`, `miss` 与首次 `ring` 取相同的值, 重启后再发的广播可被识别.
     * 第 n 次贪睡后的一轮追加 `~n`.
     */
    fun occurrenceOf(alarmId: String, scheduledAt: Long, snoozeCount: Int = 0): String =
        if (snoozeCount == 0) "$alarmId@$scheduledAt" else "$alarmId@$scheduledAt~$snoozeCount"

    /**
     * The state after a change of the alarm (the planner of [io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository]):
     * the next ring, the skip kept only while the skipped instance is still a future ring of the alarm (T58), and the
     * next holiday skip before the next ring. A rule that never gives a date throws `INVALID_RULE`, so the change is
     * refused.
     * zh-CN: 闹钟变更后的状态 (仓库的 planner): 下一次响铃; 被跳过的那次仍是闹钟将来的一次响铃时才保留跳过 (T58); 以及下一次响铃之前的节假日跳过.
     * 永远不产生日期的规则报 `INVALID_RULE`, 变更被拒绝.
     */
    fun plan(record: AlarmRecord, now: Long, zone: ZoneId, judge: HolidayJudge = HolidayJudge.NONE): AlarmRuntimeState {
        val state = record.state
        if (!record.enabled) return idle(state)
        val handled = guarded(state.handledThrough, now)
        val skipAt = state.skipAt?.takeIf { it > now && isRing(record, it, zone, judge) }
        val from = maxOf(now, handled ?: now)
        val next = NextOccurrence.next(record.spec, from, zone, skipAt, judge)?.at
        val holidaySkipAt = NextOccurrence.nextHolidaySkip(record.spec, from, next, zone, judge)
        return beforeRingKept(state.copy(nextTrigger = next, skipAt = skipAt, holidaySkipAt = holidaySkipAt, handledThrough = handled))
    }

    /**
     * When the `beforeRing` wake-up of [record] is due (roadmap P8.4): [leadSeconds] before its planned ring, null when
     * nobody listens ([leadSeconds] null), the alarm is disabled or has no planned ring, the `beforeRing` of that ring went
     * out already, or [now] is past its deadline. An instant already passed means "as soon as possible".
     * zh-CN: [record] 的 `beforeRing` 唤醒时刻 (路线图 P8.4): 计划响铃之前 [leadSeconds] 秒; 无人监听 ([leadSeconds] 为 null), 闹钟已禁用或没有计划响铃,
     * 该次的 `beforeRing` 已经发出, 或 [now] 已过截止时刻时为 null. 已经过去的时刻表示 "尽快".
     */
    fun beforeRingAt(record: AlarmRecord, leadSeconds: Int?, now: Long): Long? {
        if (!record.enabled || leadSeconds == null) return null
        val scheduledAt = record.state.nextTrigger ?: return null
        if (record.state.beforeRingFor == scheduledAt) return null
        if (now >= deadlineOf(scheduledAt)) return null
        return scheduledAt - leadSeconds * 1000L
    }

    /** The decision deadline of the planned ring at [scheduledAt]. zh-CN: [scheduledAt] 的计划响铃的决定截止时刻. */
    fun deadlineOf(scheduledAt: Long): Long = scheduledAt - AlarmLimits.BEFORE_RING_DEADLINE_MARGIN_MILLIS

    /**
     * The `beforeRing` event of the planned ring at [scheduledAt] (roadmap P8.4): the `occurrence` of that instance and
     * its `deadline`.
     * zh-CN: [scheduledAt] 的计划响铃的 `beforeRing` 事件 (路线图 P8.4): 带该次的 `occurrence` 与 `deadline`.
     */
    fun beforeRingEvent(record: AlarmRecord, scheduledAt: Long, now: Long): AlarmEvent =
        event(record, AlarmContract.EVENT_BEFORE_RING, scheduledAt, now).copy(deadline = deadlineOf(scheduledAt))

    /**
     * The instance `skipNext` skips: the first planned ring from now, the snooze not counted; null when the alarm is
     * disabled or will not ring again.
     * zh-CN: `skipNext` 跳过的那一次: 从现在起的第一次计划响铃 (不计贪睡); 闹钟已禁用或不再响时为 null.
     */
    fun skipTarget(record: AlarmRecord, now: Long, zone: ZoneId, judge: HolidayJudge = HolidayJudge.NONE): Long? {
        if (!record.enabled) return null
        val handled = guarded(record.state.handledThrough, now)
        return NextOccurrence.next(record.spec, maxOf(now, handled ?: now), zone, judge = judge)?.at
    }

    /**
     * Decides what happens to [record] at [now] (see the class comment). A stored rule that cannot be computed plans no
     * ring. [cause] picks the `miss` reason; only [Cause.TRIGGER] accepts a ring [EARLY_TOLERANCE_MILLIS] early.
     * zh-CN: 决定 [record] 在 [now] 的处理 (见类注释). 无法计算的已存规则不计划响铃. [cause] 决定 `miss` 的原因; 只有 [Cause.TRIGGER] 接受提前
     * [EARLY_TOLERANCE_MILLIS] 的响铃.
     */
    fun reconcile(record: AlarmRecord, now: Long, zone: ZoneId, cause: Cause, judge: HolidayJudge = HolidayJudge.NONE): Outcome {
        val state = record.state
        if (!record.enabled) return Outcome(idle(state))
        val due = now + if (cause == Cause.TRIGGER) EARLY_TOLERANCE_MILLIS else 0L
        val window = AlarmLimits.MISSED_RING_WINDOW_MILLIS
        val events = ArrayList<AlarmEvent>(2)
        var handled = guarded(state.handledThrough, now)
        var skipAt = state.skipAt
        var snoozeUntil = state.snoozeUntil
        var snoozeCount = state.snoozeCount
        var occurrence = state.occurrence
        var ring: RingRequest? = null
        var ended = false

        // A snooze that has come rings as a new round; one that came more than the window ago ends its round (T57).
        // zh-CN: 已到的贪睡作为新一轮响铃; 超过窗口的贪睡结束本轮 (T57).
        val end = snoozeUntil
        if (end != null && end <= due) {
            if (now - end <= window) {
                val base = state.occurrence?.substringBefore(SNOOZE_SEPARATOR) ?: occurrenceOf(record.id, end)
                val value = "$base$SNOOZE_SEPARATOR$snoozeCount".take(AlarmLimits.MAX_OCCURRENCE_CHARS)
                ring = RingRequest(record.id, end, now, value, snoozeCount, state.ringOverrides)
                occurrence = value
            } else {
                snoozeCount = 0
                ended = true
            }
            snoozeUntil = null
        }

        // The skip of skipNext and the holiday skip that have come, in time order (T181).
        // zh-CN: 已到的 skipNext 跳过与节假日跳过, 按时间顺序处理 (T181).
        val skipped = skipAt?.takeIf { it <= due }
        val holiday = state.holidaySkipAt?.takeIf { it <= due }
        for (at in listOfNotNull(skipped, holiday).distinct().sorted()) {
            if (handled != null && at <= handled) continue
            val reason = when {
                at == skipped -> AlarmContract.SKIP_REASON_SKIP_NEXT
                isHolidaySkip(record, at, zone, judge) -> AlarmContract.SKIP_REASON_HOLIDAY
                else -> continue
            }
            events += event(record, AlarmContract.EVENT_SKIP, at, now).copy(reason = reason)
            handled = at
            if (reason == AlarmContract.SKIP_REASON_HOLIDAY) ended = true
        }
        if (skipped != null) skipAt = null

        val planned = state.nextTrigger
        var ringOverrides = state.ringOverrides
        if (planned != null && planned <= due && (handled == null || planned > handled)) {
            handled = planned
            val decided = state.beforeRingFor == planned
            val scriptSkip = state.beforeRingSkip?.takeIf { decided }
            if (scriptSkip != null) {
                // A skip decision of beforeRing wins (roadmap P8.4), late or not. zh-CN: beforeRing 的跳过决定优先 (路线图 P8.4), 迟到与否都是.
                events += event(record, AlarmContract.EVENT_SKIP, planned, now).copy(reason = AlarmContract.SKIP_REASON_SCRIPT, detail = scriptSkip.ifEmpty { null })
                ended = true
            } else if (now - planned <= window) {
                // A planned ring supersedes a snooze of the round before. zh-CN: 计划响铃取代上一轮的贪睡.
                val value = occurrenceOf(record.id, planned)
                val overrides = state.beforeRingOverrides?.takeIf { decided }
                ring = RingRequest(record.id, planned, now, value, 0, overrides)
                ringOverrides = overrides
                occurrence = value
                snoozeUntil = null
                snoozeCount = 0
                ended = false
            } else {
                val reason = if (cause == Cause.BOOT) AlarmContract.MISS_REASON_OFF else AlarmContract.MISS_REASON_LATE
                events += event(record, AlarmContract.EVENT_MISS, planned, now).copy(reason = reason)
                ended = true
            }
        }

        if (skipAt != null && !isRing(record, skipAt, zone, judge)) skipAt = null
        val from = maxOf(now, handled ?: now)
        val next = try {
            NextOccurrence.next(record.spec, from, zone, skipAt, judge)?.at
        } catch (_: AlarmException) {
            null
        }
        val holidaySkipAt = try {
            NextOccurrence.nextHolidaySkip(record.spec, from, next, zone, judge)
        } catch (_: AlarmException) {
            null
        }
        // A round in the ring queue keeps its place; a new round starts the instance or continues it after a snooze.
        // zh-CN: 响铃队列中的一轮保持不变; 新的一轮开始该次响铃, 或在贪睡后继续.
        val ringingAt = ring?.scheduledAt ?: state.ringingAt
        val ringSince = when {
            ring != null && ring.snoozeCount == 0 -> now
            ring != null -> state.ringSince ?: now
            snoozeUntil != null || ringingAt != null -> state.ringSince
            else -> null
        }
        val retire = ended && !HolidayFilter.repeats(record.spec) && next == null && holidaySkipAt == null && ring == null && snoozeUntil == null && ringingAt == null
        val result = beforeRingKept(state.copy(
            nextTrigger = next,
            skipAt = skipAt,
            holidaySkipAt = holidaySkipAt,
            handledThrough = handled,
            occurrence = occurrence,
            snoozeUntil = snoozeUntil,
            snoozeCount = snoozeCount,
            ringSince = ringSince,
            ringingAt = ringingAt,
            ringOverrides = ringOverrides.takeIf { ringSince != null },
        ))
        return Outcome(result, retire, ring, events)
    }

    /** True when [at] is a ring of [record], the holiday filter applied. zh-CN: [at] 是 [record] 的一次响铃时为 true (已应用节假日过滤). */
    fun isRing(record: AlarmRecord, at: Long, zone: ZoneId, judge: HolidayJudge = HolidayJudge.NONE): Boolean = try {
        NextOccurrence.rings(record.spec, zone, judge, at - 1).takeWhile { it.at <= at }.any { it.at == at }
    } catch (_: AlarmException) {
        false
    }

    /** True when a holiday takes away the instance of [record] at [at]. zh-CN: [record] 在 [at] 的那一次被节假日拿掉时为 true. */
    private fun isHolidaySkip(record: AlarmRecord, at: Long, zone: ZoneId, judge: HolidayJudge): Boolean = try {
        NextOccurrence.instances(record.spec, zone, judge, at - 1)
            .takeWhile { it.first.at <= at }
            .any { it.first.at == at && it.second == HolidayFilter.Verdict.HOLIDAY }
    } catch (_: AlarmException) {
        false
    }

    /**
     * The state of a disabled alarm (T50): no planned ring, skip, snooze or ring round.
     * zh-CN: 禁用闹钟的状态 (T50): 没有计划响铃, 跳过, 贪睡与响铃轮次.
     */
    fun idle(state: AlarmRuntimeState): AlarmRuntimeState = state.copy(
        nextTrigger = null, skipAt = null, holidaySkipAt = null, snoozeUntil = null, snoozeCount = 0, ringSince = null, ringingAt = null,
        beforeRingFor = null, beforeRingSkip = null, beforeRingOverrides = null, ringOverrides = null,
    )

    /**
     * [state] with the `beforeRing` decisions kept only while their instance is still the next planned ring (roadmap P8.4).
     * zh-CN: 只在 `beforeRing` 决定所属的那一次仍是下一次计划响铃时保留这些决定 (路线图 P8.4).
     */
    private fun beforeRingKept(state: AlarmRuntimeState): AlarmRuntimeState =
        if (state.beforeRingFor == null || state.beforeRingFor == state.nextTrigger) state
        else state.copy(beforeRingFor = null, beforeRingSkip = null, beforeRingOverrides = null)

    /** [handled] unless it is more than [REPLAY_GUARD_MILLIS] ahead of [now]. zh-CN: 超前 [now] 超过 [REPLAY_GUARD_MILLIS] 时为 null. */
    private fun guarded(handled: Long?, now: Long): Long? = handled?.takeIf { it - now <= REPLAY_GUARD_MILLIS }

    private fun event(record: AlarmRecord, type: String, scheduledAt: Long, now: Long) = AlarmEvent(
        type = type,
        kind = AlarmContract.KIND_ALARM,
        alarmId = record.id,
        occurrence = occurrenceOf(record.id, scheduledAt),
        firedAt = now,
        scheduledAt = scheduledAt,
        label = record.spec.label,
    )

    private const val SNOOZE_SEPARATOR = "~"
}
