package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmChange
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.AlarmEventBus
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmSchedule
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
 * zh-CN: 插件进程的响铃状态 (路线图 P2.4, T64 - T67), 不依赖 Android, 由 JVM 测试覆盖. 同一时间只有一个闹钟发声 ([sounding]); 期间到时的闹钟按顺序排队
 * ([queued]), 前一个被贪睡, 关闭或自动静音后下一个立即发声. 闹钟开始发声时发 `ring`; `snooze`, `dismiss` 与 `silence` 带它们结束的那一轮的
 * `occurrence`. 贪睡经 [AlarmSchedule.snooze] 保存 (调度器把它作为新一轮再次响铃); 关闭或自动静音的一轮经 [AlarmSchedule.endRound] 结束, 用完的一次性闹钟随之退役.
 * 响铃中的闹钟被删除或禁用时以变更来源发 `dismiss` 并停止. Android 响铃服务通过 [addListener] 跟随 [sounding], 本轮自动停止时间到后调用 [silence].
 */
class RingCoordinator(
    private val repository: AlarmRepository,
    private val schedule: AlarmSchedule,
    private val events: AlarmEventBus,
    private val clock: () -> Long = System::currentTimeMillis,
    private val defaults: () -> RingDefaults = { RingDefaults.PROTOCOL },
) {

    /**
     * One due ring: [request] from the scheduler, the [record] and [settings] it rings with, [since] (when the instance
     * first became due, kept through its snoozes) and [startedAt] (when this round began to sound, null while queued).
     * zh-CN: 一次到时的响铃: 调度器给出的 [request], 响铃所用的 [record] 与 [settings], [since] (该次响铃首次到时的时刻, 贪睡期间保留) 与 [startedAt]
     * (本轮开始发声的时刻, 排队时为 null).
     */
    data class Ring(
        val record: AlarmRecord,
        val request: AlarmPlanner.RingRequest,
        val settings: RingSettings,
        val since: Long,
        val startedAt: Long? = null,
    ) {
        val alarmId: String get() = request.alarmId
        val occurrence: String get() = request.occurrence

        /** When the sounding round stops by itself. zh-CN: 发声的一轮自动停止的时刻. */
        val silenceAt: Long? get() = startedAt?.plus(settings.autoSilenceMillis)

        /** Snoozing is allowed and the snoozes of the instance are below the limit. zh-CN: 允许贪睡且次数未达上限. */
        val canSnooze: Boolean get() = settings.snoozeEnabled && request.snoozeCount < settings.snoozeMax
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
        act { batch ->
            val ring = Ring(record, request, RingSettings.of(record.spec, defaults()), record.state.ringSince ?: request.firedAt)
            val sounding = current
            if (sounding != null && sounding.alarmId == ring.alarmId) {
                if (sounding.occurrence != ring.occurrence) start(ring, batch)
            } else {
                val index = queue.indexOfFirst { it.alarmId == ring.alarmId }
                if (index >= 0) queue[index] = ring else queue += ring
            }
        }
    }

    /**
     * Snoozes the alarm [id] (every sounding or waiting one when null) for [minutes] (the alarm's own snooze length
     * when null). Refused for an alarm without snoozing, at its snooze limit, already snoozed, or when the snooze
     * cannot be scheduled (T65); answers whether any alarm was snoozed. Throws `INVALID_OPTIONS` for [minutes] outside 1 - 60.
     * zh-CN: 让闹钟 [id] (null 为全部发声与排队的闹钟) 贪睡 [minutes] 分钟 (null 用闹钟自己的贪睡时长). 不允许贪睡, 已达贪睡上限, 已在贪睡或无法排程时拒绝 (T65);
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
                if (!ring.canSnooze) continue
                val length = minutes ?: ring.settings.snoozeMinutes
                val count = ring.request.snoozeCount + 1
                schedule.snooze(ring.alarmId, now + length * MINUTE_MILLIS, count) ?: continue
                drop(ring)
                batch += event(AlarmContract.EVENT_SNOOZE, ring, now).copy(by = by, minutes = length, snoozeCount = count)
                handled = true
            }
            handled
        }
    }

    /**
     * Dismisses the alarm [id] (every ringing one when null): a sounding or waiting round, or a running snooze, ends
     * with `dismiss`. Answers whether any alarm was dismissed.
     * zh-CN: 关闭闹钟 [id] (null 为全部正在响铃的闹钟): 发声或排队的一轮, 或正在进行的贪睡, 以 `dismiss` 结束. 返回是否有闹钟被关闭.
     */
    fun dismiss(id: String?, by: String): Boolean = act { batch ->
        val now = clock()
        var handled = false
        for (ring in active(id)) {
            drop(ring)
            schedule.endRound(ring.alarmId)
            batch += event(AlarmContract.EVENT_DISMISS, ring, now).copy(by = by)
            handled = true
        }
        for (record in snoozed(id)) {
            schedule.endRound(record.id)
            batch += snoozedEvent(record, now).copy(by = by)
            handled = true
        }
        handled
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
        schedule.endRound(ring.alarmId)
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
        val rings = active.map { RingingSnapshot(it.alarmId, AlarmContract.KIND_ALARM, it.record.spec.label, it.since, false, it.request.snoozeCount) }
        val snoozes = snoozed(null).filter { it.id !in ids }.map {
            RingingSnapshot(it.id, AlarmContract.KIND_ALARM, it.spec.label, it.state.ringSince ?: it.state.snoozeUntil!!, true, it.state.snoozeCount)
        }
        return rings + snoozes
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
                offer(record, AlarmPlanner.RingRequest(record.id, ringingAt, now, occurrence, record.state.snoozeCount))
                resumed++
            } else {
                schedule.endRound(record.id)
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

    /** Lets [ring] sound now (it leaves the queue). zh-CN: 让 [ring] 立即发声 (离开队列). */
    private fun start(ring: Ring, batch: MutableList<AlarmEvent>) {
        queue.removeAll { it.alarmId == ring.alarmId }
        val now = clock()
        val started = ring.copy(startedAt = now)
        current = started
        batch += event(AlarmContract.EVENT_RING, started, now).copy(snoozeCount = ring.request.snoozeCount)
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
                current = current?.let { if (it.alarmId == after.id) it.copy(record = after) else it }
                queue.replaceAll { if (it.alarmId == after.id) it.copy(record = after) else it }
            }
            return
        }
        val by = byOf(change.source)
        act { batch ->
            val now = clock()
            val rings = active(change.alarmId)
            for (ring in rings) {
                drop(ring)
                batch += event(AlarmContract.EVENT_DISMISS, ring, now).copy(by = by)
            }
            if (rings.isEmpty() && before != null && before.enabled && before.state.snoozeUntil != null) {
                batch += snoozedEvent(before, now).copy(by = by)
            }
            Unit
        }
    }

    private fun event(type: String, ring: Ring, now: Long) = AlarmEvent(
        type = type,
        kind = AlarmContract.KIND_ALARM,
        alarmId = ring.alarmId,
        occurrence = ring.occurrence,
        firedAt = now,
        scheduledAt = ring.request.scheduledAt,
        label = ring.record.spec.label,
    )

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
            label = record.spec.label,
        )
    }

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
