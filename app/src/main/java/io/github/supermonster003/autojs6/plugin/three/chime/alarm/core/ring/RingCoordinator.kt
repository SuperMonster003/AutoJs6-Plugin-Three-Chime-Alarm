package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmChange
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmOptionsValidator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.AlarmEventBus
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedule
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.BeforeRingOverrides
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerRounds
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmEvent
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.RingingSnapshot
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The ringing state of the plugin process (roadmap P2.4, T64 - T67), free of Android so the JVM tests cover it.
 *
 * One alarm sounds at a time ([sounding]); alarms due meanwhile wait in order ([queued]) and the next one sounds as
 * soon as the previous one is snoozed, dismissed or silenced. `ring` is sent when an alarm starts sounding; `snooze`,
 * `dismiss` and `silence` carry the `occurrence` of the round they end. A snooze is saved through
 * [AlarmSchedule.snooze] (the scheduler rings it again as a new round); a dismissed or silenced round ends through
 * [AlarmSchedule.endRound], which retires a spent one-shot alarm. An alarm removed or disabled while ringing stops with
 * `dismiss` by the source of the change. The Android ring service follows [sounding] through [addListener] and asks
 * [silence] when the auto silence time of the round has passed.
 *
 * A dismiss challenge (1.3.0, roadmap P9, T285) is a property of the round ([Ring.challenge], the challenge in effect):
 * the user cannot dismiss it with [dismiss] (the ringing screen calls [solve] when the challenge is done, or
 * [emergencyDismiss]), scripts and tools still can; `dismiss.allowSnooze: false` refuses the user's snoozes only; the
 * `script` challenge changes to its fallback through [fallBack]. `ring` and `dismiss` carry the `challenge` in effect.
 *
 * zh-CN: 插件进程的响铃状态 (路线图 P2.4, T64 - T67), 不依赖 Android, 由 JVM 测试覆盖. 同一时间只有一个闹钟发声 ([sounding]); 期间到时的闹钟按顺序排队
 * ([queued]), 前一个被贪睡, 关闭或自动静音后下一个立即发声. 闹钟开始发声时发 `ring`; `snooze`, `dismiss` 与 `silence` 带它们结束的那一轮的
 * `occurrence`. 贪睡经 [AlarmSchedule.snooze] 保存 (调度器把它作为新一轮再次响铃); 关闭或自动静音的一轮经 [AlarmSchedule.endRound] 结束, 用完的一次性闹钟随之退役.
 * 响铃中的闹钟被删除或禁用时以变更来源发 `dismiss` 并停止. Android 响铃服务通过 [addListener] 跟随 [sounding], 本轮自动停止时间到后调用 [silence].
 * 关闭挑战 (1.3.0, 路线图 P9, T285) 是一轮响铃的属性 ([Ring.challenge], 生效的挑战): 用户不能用 [dismiss] 关闭 (响铃界面在挑战完成时调用 [solve], 或调用
 * [emergencyDismiss]), 脚本与工具仍可以; `dismiss.allowSnooze: false` 只拒绝用户的贪睡; `script` 挑战经 [fallBack] 改为其 fallback. `ring` 与 `dismiss`
 * 带生效的 `challenge`.
 */
class RingCoordinator(
    private val repository: AlarmRepository,
    private val schedule: AlarmSchedule,
    private val events: AlarmEventBus,
    private val clock: () -> Long = System::currentTimeMillis,
    private val defaults: () -> RingDefaults = { RingDefaults.PROTOCOL },
    private val timers: TimerRounds? = null,
    private val validator: AlarmOptionsValidator = AlarmOptionsValidator(),
) {

    /**
     * One due ring: [request] from the scheduler, the [record] and [settings] it rings with, [since] (when the instance
     * first became due, kept through its snoozes) and [startedAt] (when this round began to sound, null while queued).
     * A timer rings the same way (roadmap P7.1, T111): [kind] is `timer`, [record] is null and [label] comes from the timer.
     * [challenge] is the dismiss challenge in effect (1.3.0): the alarm's own, or the fallback of `script` (null for `none`).
     * zh-CN: 一次到时的响铃: 调度器给出的 [request], 响铃所用的 [record] 与 [settings], [since] (该次响铃首次到时的时刻, 贪睡期间保留) 与 [startedAt]
     * (本轮开始发声的时刻, 排队时为 null). 计时器以同样方式响铃 (路线图 P7.1, T111): [kind] 为 `timer`, [record] 为 null, [label] 取自计时器.
     * [challenge] 为生效的关闭挑战 (1.3.0): 闹钟自己的, 或 `script` 退回的挑战 (`none` 时为 null).
     */
    data class Ring(
        val record: AlarmRecord?,
        val request: AlarmPlanner.RingRequest,
        val settings: RingSettings,
        val since: Long,
        val startedAt: Long? = null,
        val kind: String = AlarmContract.KIND_ALARM,
        val label: String? = record?.spec?.label,
        val challenge: String? = settings.challenge,
    ) {
        val alarmId: String get() = request.alarmId
        val isTimer: Boolean get() = kind == AlarmContract.KIND_TIMER
        val occurrence: String get() = request.occurrence

        /** When the sounding round stops by itself. zh-CN: 发声的一轮自动停止的时刻. */
        val silenceAt: Long? get() = startedAt?.plus(settings.autoSilenceMillis)

        /** Snoozing is allowed and the snoozes of the instance are below the limit. zh-CN: 允许贪睡且次数未达上限. */
        val canSnooze: Boolean get() = settings.snoozeEnabled && request.snoozeCount < settings.snoozeMax

        /**
         * The user may snooze: [canSnooze], and a challenge in effect allows it (`dismiss.allowSnooze`).
         * zh-CN: 用户可以贪睡: [canSnooze], 且生效的挑战允许 (`dismiss.allowSnooze`).
         */
        val userCanSnooze: Boolean get() = canSnooze && (challenge == null || settings.allowSnooze)
    }

    private val lock = Any()

    private var current: Ring? = null

    private val queue = ArrayList<Ring>()

    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    init {
        repository.addListener(::onChange)
    }

    /** The alarm that is sounding, null when none is. zh-CN: 正在发声的闹钟, 没有时为 null. */
    val sounding: Ring? get() = synchronized(lock) { current }

    /** The alarms waiting for their turn, in order. zh-CN: 按顺序排队等待的闹钟. */
    val queued: List<Ring> get() = synchronized(lock) { queue.toList() }

    /**
     * [listener] runs after every change of the ring state (on the thread that made it); it reads [sounding] itself.
     * zh-CN: 响铃状态每次改变后运行 [listener] (在做出改变的线程上); 它自行读取 [sounding].
     */
    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: () -> Unit) {
        listeners -= listener
    }

    /**
     * Takes a due ring from the scheduler: it sounds at once when nothing else does and waits otherwise. A new round
     * of an alarm already sounding or waiting replaces its old round; the same round again changes nothing.
     * zh-CN: 接收调度器给出的到时响铃: 没有其他闹钟发声时立即发声, 否则排队. 正在发声或排队的闹钟又来新的一轮时取代旧的一轮; 同一轮再来不做改变.
     */
    fun offer(record: AlarmRecord, request: AlarmPlanner.RingRequest) {
        // The beforeRing overrides of the instance change only this ring (roadmap P8.4). zh-CN: 该次的 beforeRing 覆盖只改变这次响铃 (路线图 P8.4).
        val spec = BeforeRingOverrides.apply(record.spec, request.overrides, validator)
        act { batch -> enqueue(Ring(record, request, RingSettings.of(spec, defaults()), record.state.ringSince ?: request.firedAt, label = spec.label), batch) }
    }

    /**
     * Takes a due timer (roadmap P7.1): it rings like an alarm, with `kind: 'timer'` on its events.
     * zh-CN: 接收到点的计时器 (路线图 P7.1): 与闹钟一样响铃, 事件带 `kind: 'timer'`.
     */
    fun offerTimer(record: TimerRecord, request: AlarmPlanner.RingRequest) {
        act { batch ->
            val settings = RingSettings.ofTimer(record.spec, defaults())
            enqueue(Ring(null, request, settings, record.ringSince ?: request.firedAt, kind = AlarmContract.KIND_TIMER, label = record.spec.label), batch)
        }
    }

    /**
     * Snoozes the alarm [id] (every sounding or waiting one when null) for [minutes] (the alarm's own snooze length
     * when null). Refused for an alarm without snoozing, at its snooze limit, already snoozed, or when the snooze
     * cannot be scheduled (T65), or for the user ([by] `user`) when the challenge forbids it (1.3.0); answers whether any
     * alarm was snoozed. Throws `INVALID_OPTIONS` for [minutes] outside 1 - 60.
     * zh-CN: 让闹钟 [id] (null 为全部发声与排队的闹钟) 贪睡 [minutes] 分钟 (null 用闹钟自己的贪睡时长). 不允许贪睡, 已达贪睡上限, 已在贪睡或无法排程时拒绝 (T65),
     * 挑战禁止贪睡时拒绝用户 ([by] 为 `user`) 的贪睡 (1.3.0);
     * 返回是否有闹钟被贪睡. [minutes] 不在 1 - 60 时报 `INVALID_OPTIONS`.
     */
    fun snooze(id: String?, minutes: Int?, by: String): Boolean {
        if (minutes != null && minutes !in AlarmLimits.MIN_SNOOZE_MINUTES..AlarmLimits.MAX_SNOOZE_MINUTES) {
            throw AlarmException.invalidOptions("snooze minutes must be within ${AlarmLimits.MIN_SNOOZE_MINUTES} - ${AlarmLimits.MAX_SNOOZE_MINUTES}")
        }
        return act { batch ->
            val now = clock()
            var handled = false
            for (ring in active(id)) {
                if (!(if (by == AlarmContract.BY_USER) ring.userCanSnooze else ring.canSnooze)) continue
                val length = minutes ?: ring.settings.snoozeMinutes
                val count = ring.request.snoozeCount + 1
                val until = now + length * MINUTE_MILLIS
                if (ring.isTimer) {
                    if (timers?.snooze(ring.alarmId, until, count) != true) continue
                } else {
                    schedule.snooze(ring.alarmId, until, count) ?: continue
                }
                drop(ring)
                batch += event(AlarmContract.EVENT_SNOOZE, ring, now).copy(by = by, minutes = length, snoozeCount = count)
                handled = true
            }
            handled
        }
    }

    /**
     * Dismisses the alarm [id] (every ringing one when null): a sounding or waiting round, or a running snooze, ends
     * with `dismiss`. The user ([by] `user`) cannot dismiss an alarm with a dismiss challenge this way (1.3.0, T285):
     * see [solve], [emergencyDismiss] and [challenged]. Answers whether any alarm was dismissed.
     * zh-CN: 关闭闹钟 [id] (null 为全部正在响铃的闹钟): 发声或排队的一轮, 或正在进行的贪睡, 以 `dismiss` 结束. 用户 ([by] 为 `user`) 不能这样关闭有关闭挑战的
     * 闹钟 (1.3.0, T285): 见 [solve], [emergencyDismiss] 与 [challenged]. 返回是否有闹钟被关闭.
     */
    fun dismiss(id: String?, by: String): Boolean = act { batch ->
        val now = clock()
        val user = by == AlarmContract.BY_USER
        var handled = false
        for (ring in active(id)) {
            if (user && ring.challenge != null) continue
            drop(ring)
            endRound(ring)
            batch += event(AlarmContract.EVENT_DISMISS, ring, now).copy(by = by, challenge = ring.challenge)
            handled = true
        }
        for (record in snoozed(id)) {
            val challenge = challengeOf(record)
            if (user && challenge != null) continue
            schedule.endRound(record.id)
            batch += snoozedEvent(record, now).copy(by = by, challenge = challenge)
            handled = true
        }
        for (record in timers?.snoozed(id).orEmpty()) {
            timers?.end(record.id)
            batch += snoozedEvent(record, now).copy(by = by)
            handled = true
        }
        handled
    }

    /**
     * Whether the alarm [id] (any when null) is ringing or snoozed with a dismiss challenge, so a dismiss by the user
     * must go through the ringing screen (1.3.0, T285).
     * zh-CN: 闹钟 [id] (null 为任意) 是否正以关闭挑战响铃或贪睡, 用户关闭时须经响铃界面 (1.3.0, T285).
     */
    fun challenged(id: String?): Boolean =
        active(id).any { it.challenge != null } || snoozed(id).any { challengeOf(it) != null }

    /**
     * The user finished the challenge of the sounding round [occurrence] (1.3.0, T285): it ends with `dismiss`, `by:
     * 'challenge'` and the finished challenge. Refused for another round, no challenge or `script` (a script dismisses it).
     * zh-CN: 用户完成了发声的一轮 [occurrence] 的挑战 (1.3.0, T285): 以 `dismiss` 结束, `by: 'challenge'`, 带完成的挑战. 其他轮次, 无挑战或 `script`
     * (由脚本关闭) 时拒绝.
     */
    fun solve(occurrence: String): Boolean = act { batch ->
        val ring = current?.takeIf { it.occurrence == occurrence } ?: return@act false
        val challenge = ring.challenge?.takeIf { it != AlarmContract.CHALLENGE_SCRIPT } ?: return@act false
        val now = clock()
        drop(ring)
        endRound(ring)
        batch += event(AlarmContract.EVENT_DISMISS, ring, now).copy(by = AlarmContract.BY_CHALLENGE, challenge = challenge)
        true
    }

    /**
     * The emergency dismiss of the sounding round [occurrence] (1.3.0, roadmap P9.3, T283): it ends with `dismiss`,
     * `by: 'user'` and the challenge in effect, whatever the challenge.
     * zh-CN: 发声的一轮 [occurrence] 的紧急关闭 (1.3.0, 路线图 P9.3, T283): 不论何种挑战, 以 `dismiss` 结束, `by: 'user'`, 带生效的挑战.
     */
    fun emergencyDismiss(occurrence: String): Boolean = act { batch ->
        val ring = current?.takeIf { it.occurrence == occurrence } ?: return@act false
        val now = clock()
        drop(ring)
        endRound(ring)
        batch += event(AlarmContract.EVENT_DISMISS, ring, now).copy(by = AlarmContract.BY_USER, challenge = ring.challenge)
        true
    }

    /**
     * The `script` challenge of the sounding round [occurrence] falls back to its `script.fallback` (1.3.0, roadmap
     * P9.2, T284): the ringing screen shows that challenge (none for `none`) for the rest of the round; no event.
     * Answers false when the round is not sounding or not waiting for a script.
     * zh-CN: 发声的一轮 [occurrence] 的 `script` 挑战退回其 `script.fallback` (1.3.0, 路线图 P9.2, T284): 本轮余下的时间响铃界面显示该挑战 (`none`
     * 时无挑战); 不发事件. 该轮不在发声或不在等待脚本时返回 false.
     */
    fun fallBack(occurrence: String): Boolean = act {
        val ring = current?.takeIf { it.occurrence == occurrence && it.challenge == AlarmContract.CHALLENGE_SCRIPT } ?: return@act false
        val fallback = ring.settings.dismiss?.scriptFallback?.takeIf { it != AlarmContract.CHALLENGE_NONE && it != AlarmContract.CHALLENGE_SCRIPT }
        current = ring.copy(challenge = fallback)
        true
    }

    /**
     * Ends the sounding round [occurrence] after its auto silence time with `silence` (the ring service keeps the
     * timer); a round that is no longer sounding is left alone.
     * zh-CN: 发声的一轮 [occurrence] 到自动停止时间后以 `silence` 结束 (计时由响铃服务负责); 已不在发声的一轮不做处理.
     */
    fun silence(occurrence: String): Boolean = act { batch ->
        val ring = current?.takeIf { it.occurrence == occurrence } ?: return@act false
        val now = clock()
        drop(ring)
        endRound(ring)
        batch += event(AlarmContract.EVENT_SILENCE, ring, now).copy(ringMillis = (now - (ring.startedAt ?: now)).coerceAtLeast(0L))
        true
    }

    /**
     * The `ringing()` answer: the sounding alarm, the waiting ones and the snoozed ones (appendix A section 4.1).
     * zh-CN: `ringing()` 的结果: 发声, 排队与贪睡中的闹钟 (附录 A 第 4.1 节).
     */
    fun ringing(): List<RingingSnapshot> {
        val active = synchronized(lock) { listOfNotNull(current) + queue }
        val ids = active.mapTo(HashSet()) { it.alarmId }
        val rings = active.map { RingingSnapshot(it.alarmId, it.kind, it.label, it.since, false, it.request.snoozeCount, it.challenge) }
        val snoozes = snoozed(null).filter { it.id !in ids }.map {
            RingingSnapshot(it.id, AlarmContract.KIND_ALARM, labelOf(it), it.state.ringSince ?: it.state.snoozeUntil!!, true, it.state.snoozeCount, challengeOf(it))
        }
        val timerSnoozes = timers?.snoozed(null).orEmpty().filter { it.id !in ids }.map {
            RingingSnapshot(it.id, AlarmContract.KIND_TIMER, it.spec.label, it.ringSince ?: it.snoozeUntil!!, true, it.snoozeCount)
        }
        return rings + snoozes + timerSnoozes
    }

    /**
     * Picks up the rounds a dead process left sounding or waiting (T67): a round that began less than its auto silence
     * time ago rings again with the same `occurrence` (the host starts nothing twice), an older one ends without an
     * event. Answers how many ring again.
     * zh-CN: 接续进程死亡时留下的正在发声或排队的轮次 (T67): 开始时间未超过自动停止时长的一轮以相同的 `occurrence` 再次响铃 (宿主不会重复启动), 更早的一轮
     * 结束且不发事件. 返回再次响铃的数量.
     */
    fun resume(): Int {
        val now = clock()
        var resumed = 0
        for (record in schedule.interrupted()) {
            if (active(record.id).isNotEmpty()) continue
            val ringingAt = record.state.ringingAt ?: continue
            if (now - ringingAt < RingSettings.of(record.spec, defaults()).autoSilenceMillis) {
                val occurrence = record.state.occurrence ?: AlarmPlanner.occurrenceOf(record.id, ringingAt, record.state.snoozeCount)
                offer(record, AlarmPlanner.RingRequest(record.id, ringingAt, now, occurrence, record.state.snoozeCount, record.state.ringOverrides))
                resumed++
            } else {
                schedule.endRound(record.id)
            }
        }
        for (record in timers?.interrupted().orEmpty()) {
            if (active(record.id).isNotEmpty()) continue
            val ringingAt = record.ringingAt ?: continue
            if (now - ringingAt < RingSettings.ofTimer(record.spec, defaults()).autoSilenceMillis) {
                val occurrence = record.occurrence ?: AlarmPlanner.occurrenceOf(record.id, ringingAt, record.snoozeCount)
                offerTimer(record, AlarmPlanner.RingRequest(record.id, ringingAt, now, occurrence, record.snoozeCount))
                resumed++
            } else {
                timers?.end(record.id, silent = true)
            }
        }
        return resumed
    }

    // ------------------------------------------------------------------ internals

    /** Runs [block] under the lock, then sends the events it collected and tells the listeners. */
    private fun <T> act(block: (MutableList<AlarmEvent>) -> T): T {
        val batch = ArrayList<AlarmEvent>(2)
        val result: T
        val changed: Boolean
        synchronized(lock) {
            val before = current to queue.toList()
            result = block(batch)
            val next = current
            if (next == null && queue.isNotEmpty()) start(queue.removeAt(0), batch)
            changed = before != (current to queue.toList())
        }
        batch.forEach(events::emit)
        if (changed) listeners.forEach { listener -> runCatching(listener) }
        return result
    }

    /** Lets [ring] sound now, or queues it; see [offer]. zh-CN: 让 [ring] 立即发声或排队, 见 [offer]. */
    private fun enqueue(ring: Ring, batch: MutableList<AlarmEvent>) {
        val sounding = current
        if (sounding != null && sounding.alarmId == ring.alarmId) {
            if (sounding.occurrence != ring.occurrence) start(ring, batch)
        } else {
            val index = queue.indexOfFirst { it.alarmId == ring.alarmId }
            if (index >= 0) queue[index] = ring else queue += ring
        }
    }

    /** Ends the round of [ring]: an alarm through the scheduler, a timer is removed. zh-CN: 结束 [ring] 的一轮: 闹钟经调度器, 计时器被删除. */
    private fun endRound(ring: Ring) {
        if (ring.isTimer) timers?.end(ring.alarmId) else schedule.endRound(ring.alarmId)
    }

    /** Lets [ring] sound now (it leaves the queue). zh-CN: 让 [ring] 立即发声 (离开队列). */
    private fun start(ring: Ring, batch: MutableList<AlarmEvent>) {
        queue.removeAll { it.alarmId == ring.alarmId }
        val now = clock()
        // A new round starts with the alarm's own challenge (a `script` fallback lasts one round). zh-CN: 新一轮以闹钟自己的挑战开始 (`script` 的退回只持续一轮).
        val started = ring.copy(startedAt = now, challenge = ring.settings.challenge)
        current = started
        batch += event(AlarmContract.EVENT_RING, started, now).copy(snoozeCount = ring.request.snoozeCount, challenge = started.challenge)
    }

    private fun drop(ring: Ring) {
        if (current?.alarmId == ring.alarmId) current = null
        queue.removeAll { it.alarmId == ring.alarmId }
    }

    private fun active(id: String?): List<Ring> = synchronized(lock) {
        (listOfNotNull(current) + queue).filter { id == null || it.alarmId == id }
    }

    private fun snoozed(id: String?): List<AlarmRecord> =
        repository.list().filter { it.enabled && it.state.snoozeUntil != null && (id == null || it.id == id) }

    /** A removed or disabled alarm stops ringing with `dismiss` by the source of the change (T66). */
    private fun onChange(change: AlarmChange) {
        val after = change.after
        val before = change.before
        if (after != null && after.enabled) {
            // Other changes keep the round as it sounds; the record shows the new label. zh-CN: 其他变更不改变发声, 记录显示新标签.
            act {
                current = current?.let { if (it.alarmId == after.id) it.copy(record = after, label = labelOf(after, it.request.overrides)) else it }
                queue.replaceAll { if (it.alarmId == after.id) it.copy(record = after, label = labelOf(after, it.request.overrides)) else it }
            }
            return
        }
        val by = byOf(change.source)
        act { batch ->
            val now = clock()
            val rings = active(change.alarmId)
            for (ring in rings) {
                drop(ring)
                batch += event(AlarmContract.EVENT_DISMISS, ring, now).copy(by = by, challenge = ring.challenge)
            }
            if (rings.isEmpty() && before != null && before.enabled && before.state.snoozeUntil != null) {
                batch += snoozedEvent(before, now).copy(by = by, challenge = challengeOf(before))
            }
            Unit
        }
    }

    private fun event(type: String, ring: Ring, now: Long) = AlarmEvent(
        type = type,
        kind = ring.kind,
        alarmId = ring.alarmId,
        occurrence = ring.occurrence,
        firedAt = now,
        scheduledAt = ring.request.scheduledAt,
        label = ring.label,
    )

    /** The `dismiss` of a snoozed timer. zh-CN: 贪睡中计时器的 `dismiss`. */
    private fun snoozedEvent(record: TimerRecord, now: Long): AlarmEvent {
        val until = record.snoozeUntil ?: now
        return AlarmEvent(
            type = AlarmContract.EVENT_DISMISS,
            kind = AlarmContract.KIND_TIMER,
            alarmId = record.id,
            occurrence = record.occurrence ?: AlarmPlanner.occurrenceOf(record.id, until),
            firedAt = now,
            scheduledAt = until,
            label = record.spec.label,
        )
    }

    /** An event of a snoozed alarm: the round it ends is the one that was snoozed. zh-CN: 贪睡中闹钟的事件, 结束的是被贪睡的那一轮. */
    private fun snoozedEvent(record: AlarmRecord, now: Long): AlarmEvent {
        val until = record.state.snoozeUntil ?: now
        return AlarmEvent(
            type = AlarmContract.EVENT_DISMISS,
            kind = AlarmContract.KIND_ALARM,
            alarmId = record.id,
            occurrence = record.state.occurrence ?: AlarmPlanner.occurrenceOf(record.id, until),
            firedAt = now,
            scheduledAt = until,
            label = labelOf(record),
        )
    }

    /** The dismiss challenge [record] rings with, null without one. zh-CN: [record] 响铃所用的关闭挑战, 没有时为 null. */
    private fun challengeOf(record: AlarmRecord): String? =
        record.spec.dismiss?.challenge?.takeIf { it != AlarmContract.CHALLENGE_NONE }

    /**
     * The label [record] rings with: its own, or the `beforeRing` override of the instance (roadmap P8.4).
     * zh-CN: [record] 响铃所用的标签: 自己的, 或该次的 `beforeRing` 覆盖 (路线图 P8.4).
     */
    private fun labelOf(record: AlarmRecord, overrides: Map<String, Any?>? = record.state.ringOverrides): String? =
        BeforeRingOverrides.apply(record.spec, overrides, validator).label

    companion object {

        private const val MINUTE_MILLIS = 60_000L

        /**
         * The `by` of an event caused by a change from [source]: a system clock intent counts as the user (T66).
         * zh-CN: 由来源 [source] 的变更引起的事件的 `by`: 系统时钟 Intent 视为用户 (T66).
         */
        fun byOf(source: String): String = when (source) {
            AlarmContract.SOURCE_SCRIPT -> AlarmContract.BY_SCRIPT
            AlarmContract.SOURCE_TOOL -> AlarmContract.BY_TOOL
            else -> AlarmContract.BY_USER
        }
    }
}
