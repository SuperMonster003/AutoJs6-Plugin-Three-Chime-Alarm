package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.TimerSpec
import java.security.SecureRandom
import java.util.concurrent.CopyOnWriteArrayList

/**
 * What the timers need from Android (roadmap P7.1): exact wake-ups by `setAlarmClock` (T3), the ring path, the missed
 * notification and the clean-up of the host bindings of a timer removed without an event (T113).
 * zh-CN: 计时器所需的 Android 能力 (路线图 P7.1): 用 `setAlarmClock` 精确唤醒 (T3), 响铃路径, 错过通知, 以及无事件删除的计时器的宿主绑定清理 (T113).
 */
interface TimerPort {
    fun canScheduleExact(): Boolean

    /** Arms the next wake-up of [record] (the end of a running timer or of a snooze), or cancels it. zh-CN: 排定或取消 [record] 的下一次唤醒. */
    fun schedule(record: TimerRecord)

    fun cancel(timerId: String)

    fun ring(record: TimerRecord, request: AlarmPlanner.RingRequest)

    fun notifyMissed(record: TimerRecord, scheduledAt: Long)

    /** [timerId] was removed without a `dismiss` / `silence` event (T113). zh-CN: [timerId] 在没有 `dismiss` / `silence` 事件的情况下被删除 (T113). */
    fun removedSilently(timerId: String)
}

/**
 * The ring rounds of timers as the ring coordinator sees them (roadmap P7.1, T111): a timer that rang can be snoozed
 * with the plugin defaults, and ends (is removed) when its round ends.
 * zh-CN: 响铃协调器眼中的计时器响铃轮次 (路线图 P7.1, T111): 响过的计时器可按插件默认值贪睡, 一轮结束即删除.
 */
interface TimerRounds {
    fun canSnooze(): Boolean
    fun snooze(timerId: String, until: Long, count: Int): Boolean

    /** Ends the round and removes the timer; [silent] when no `dismiss` / `silence` event goes out. zh-CN: 结束本轮并删除计时器; 不发事件时 [silent] 为 true. */
    fun end(timerId: String, silent: Boolean = false)
    fun snoozed(timerId: String?): List<TimerRecord>
    fun interrupted(): List<TimerRecord>
}

/**
 * The timers (roadmap P7.1): at most 20, each `running`, `paused` or `ringing`, kept in [store] and armed through
 * [port]. Every method is thread safe; the port and the listeners run after the lock is released, so the ring
 * coordinator may call back in (T111).
 * zh-CN: 计时器 (路线图 P7.1): 至多 20 个, 状态为 `running` / `paused` / `ringing`, 保存在 [store] 中并通过 [port] 排程. 各方法线程安全; 端口与监听器在
 * 释放锁之后执行, 响铃协调器可以回调进来 (T111).
 */
class TimerEngine(
    private val store: TimerStore,
    private val port: TimerPort,
    private val clock: () -> Long = System::currentTimeMillis,
    private val elapsed: () -> Long = { System.nanoTime() / 1_000_000L },
    private val boot: () -> Int? = { null },
    private val idGenerator: () -> String = ::randomId,
) : TimerRounds {

    private val lock = Any()
    private var records: LinkedHashMap<String, TimerRecord>? = null
    private val listeners = CopyOnWriteArrayList<(String, TimerRecord?) -> Unit>()

    /** Called with the id and the new record (null once removed) after every change. zh-CN: 每次变更后以 id 与新记录 (删除后为 null) 调用. */
    fun addListener(listener: (String, TimerRecord?) -> Unit) {
        listeners += listener
    }

    fun get(id: String): TimerRecord? = synchronized(lock) { loaded()[id] }

    fun list(): List<TimerRecord> = synchronized(lock) { loaded().values.toList() }

    /** A timer runs or a snooze waits, so a wake-up is armed (keep-alive, T117). zh-CN: 有计时器在运行或有贪睡在等待, 即排着唤醒 (保活, T117). */
    fun active(): Boolean = list().any { it.running || it.snoozeUntil != null }

    fun start(durationMillis: Long, spec: TimerSpec, source: String): TimerRecord = change { map, effects ->
        TimerDurations.requireInRange(durationMillis)
        if (map.size >= AlarmLimits.MAX_TIMERS) throw AlarmException.limitExceeded("at most ${AlarmLimits.MAX_TIMERS} timers, ${map.size} exist")
        requireExact()
        val now = clock()
        var id = idGenerator()
        while (id in map) id = idGenerator()
        val record = runningFrom(
            TimerRecord(id, source, now, spec, durationMillis, AlarmContract.TIMER_RUNNING, durationMillis),
            durationMillis,
            now,
        )
        map[id] = record
        effects.changed[id] = record
        record
    }

    fun pause(id: String): TimerRecord = change { map, effects ->
        val record = require(map, id)
        if (!record.running) return@change record
        val remaining = ((record.endsAt ?: clock()) - clock()).coerceIn(0L, TimerDurations.MAX_MILLIS)
        record.copy(state = AlarmContract.TIMER_PAUSED, remainingMillis = remaining, endsAt = null, endsElapsed = null, boot = null)
            .also { put(map, effects, it) }
    }

    fun resume(id: String): TimerRecord = change { map, effects ->
        val record = require(map, id)
        if (!record.paused) return@change record
        requireExact()
        runningFrom(record, record.remainingMillis, clock()).also { put(map, effects, it) }
    }

    /** Adds [millis] (may be negative) to the time left, which must stay within 1 s - 100 h (T112). zh-CN: 剩余时间加 [millis] (可为负), 结果须在 1 s - 100 h 内 (T112). */
    fun addTime(id: String, millis: Long): TimerRecord = change { map, effects ->
        val record = require(map, id)
        val now = clock()
        val left = if (record.running) ((record.endsAt ?: now) - now).coerceAtLeast(0L) else record.remainingMillis
        val next = try {
            Math.addExact(left, millis)
        } catch (_: ArithmeticException) {
            Long.MAX_VALUE
        }
        TimerDurations.requireInRange(next, "the time left after addTime ($left ms ${if (millis < 0) "-" else "+"} ${kotlin.math.abs(millis)} ms)")
        val updated = if (record.running) runningFrom(record, next, now) else record.copy(remainingMillis = next)
        updated.also { put(map, effects, it) }
    }

    /** Back to the start duration, paused. zh-CN: 回到开始时长并暂停. */
    fun reset(id: String): TimerRecord = change { map, effects ->
        val record = require(map, id)
        record.copy(state = AlarmContract.TIMER_PAUSED, remainingMillis = record.durationMillis, endsAt = null, endsElapsed = null, boot = null)
            .also { put(map, effects, it) }
    }

    /**
     * Removes a timer that is not ringing (a ringing one ends through the ring coordinator, T114); false for an unknown id.
     * zh-CN: 删除未在响铃的计时器 (响铃中的经响铃协调器结束, T114); id 未知时返回 false.
     */
    fun cancel(id: String): Boolean = change { map, effects ->
        val record = map[id] ?: return@change false
        if (record.ringing) throw ringingError(id)
        map.remove(id)
        effects.changed[id] = null
        effects.silent += id
        true
    }

    /**
     * Rings the timers that are due and re-arms the others (roadmap P7.1, T110): the end is first realigned (same boot:
     * from the elapsed-realtime clock; after a reboot: the wall-clock end from before the shutdown, so the time the device
     * was off counts); a due end within the 5 minute window rings late, a later one is missed (T16, T115).
     * zh-CN: 让到点的计时器响铃并重新排程其余计时器 (路线图 P7.1, T110): 先校准到点时刻 (同一次开机: 按开机时长时钟; 重启后: 用关机前的墙钟到点时刻,
     * 关机期间计入); 到点不超过 5 分钟的补响, 更晚的算错过 (T16, T115).
     */
    fun reconcile(cause: AlarmPlanner.Cause): Int = change { map, effects ->
        val now = clock()
        val due = now + if (cause == AlarmPlanner.Cause.TRIGGER) AlarmPlanner.EARLY_TOLERANCE_MILLIS else 0L
        val window = AlarmLimits.MISSED_RING_WINDOW_MILLIS
        var rang = 0
        for (record in map.values.toList()) {
            val current = realign(record, now)
            val end = current.endsAt
            val snoozeEnd = current.snoozeUntil
            when {
                current.running && end != null && end <= due -> if (now - end <= window) {
                    val occurrence = AlarmPlanner.occurrenceOf(current.id, end)
                    val ringing = current.copy(
                        state = AlarmContract.TIMER_RINGING, remainingMillis = 0L, endsElapsed = null, boot = null,
                        occurrence = occurrence, ringingAt = end, snoozeUntil = null, snoozeCount = 0, ringSince = now,
                    )
                    put(map, effects, ringing)
                    effects.rings += ringing to AlarmPlanner.RingRequest(current.id, end, now, occurrence, 0)
                    rang++
                } else {
                    map.remove(current.id)
                    effects.changed[current.id] = null
                    effects.missed += current to end
                    effects.silent += current.id
                }
                current.ringing && snoozeEnd != null && snoozeEnd <= due -> if (now - snoozeEnd <= window) {
                    val base = current.occurrence?.substringBefore(SNOOZE_SEPARATOR) ?: AlarmPlanner.occurrenceOf(current.id, snoozeEnd)
                    val occurrence = "$base$SNOOZE_SEPARATOR${current.snoozeCount}".take(AlarmLimits.MAX_OCCURRENCE_CHARS)
                    val ringing = current.copy(occurrence = occurrence, ringingAt = snoozeEnd, snoozeUntil = null)
                    put(map, effects, ringing)
                    effects.rings += ringing to AlarmPlanner.RingRequest(current.id, snoozeEnd, now, occurrence, current.snoozeCount)
                    rang++
                } else {
                    map.remove(current.id)
                    effects.changed[current.id] = null
                    effects.silent += current.id
                }
                else -> put(map, effects, current, always = true)
            }
        }
        rang
    }

    override fun canSnooze(): Boolean = port.canScheduleExact()

    override fun snooze(timerId: String, until: Long, count: Int): Boolean = change { map, effects ->
        val record = map[timerId]?.takeIf { it.ringing } ?: return@change false
        if (!port.canScheduleExact()) return@change false
        put(map, effects, record.copy(snoozeUntil = until, snoozeCount = count, ringingAt = null))
        true
    }

    override fun end(timerId: String, silent: Boolean) {
        change { map, effects ->
            if (map.remove(timerId) != null) {
                effects.changed[timerId] = null
                if (silent) effects.silent += timerId
            }
        }
    }

    override fun snoozed(timerId: String?): List<TimerRecord> =
        list().filter { it.ringing && it.snoozeUntil != null && (timerId == null || it.id == timerId) }

    override fun interrupted(): List<TimerRecord> = list().filter { it.ringing && it.ringingAt != null && it.snoozeUntil == null }

    // ------------------------------------------------------------------ internals

    private class Effects {
        val changed = LinkedHashMap<String, TimerRecord?>()
        val rings = ArrayList<Pair<TimerRecord, AlarmPlanner.RingRequest>>()
        val missed = ArrayList<Pair<TimerRecord, Long>>()
        val silent = ArrayList<String>()
    }

    /**
     * Runs [block] on a copy of the timers under the lock and saves it when anything changed (the old list stays when the
     * save fails); the port calls and listeners follow outside the lock.
     * zh-CN: 在锁内对计时器副本执行 [block], 有变化时保存 (保存失败则保留旧列表); 端口调用与监听器在锁外执行.
     */
    private fun <T> change(block: (LinkedHashMap<String, TimerRecord>, Effects) -> T): T {
        val effects = Effects()
        val result = synchronized(lock) {
            val map = LinkedHashMap(loaded())
            val value = block(map, effects)
            if (map != loaded()) {
                store.save(map.values)
                records = map
            }
            value
        }
        for ((id, after) in effects.changed) {
            if (after == null) port.cancel(id) else port.schedule(after)
        }
        effects.missed.forEach { (record, at) -> port.notifyMissed(record, at) }
        effects.silent.forEach(port::removedSilently)
        effects.rings.forEach { (record, request) -> port.ring(record, request) }
        for ((id, after) in effects.changed) listeners.forEach { listener -> runCatching { listener(id, after) } }
        return result
    }

    private fun put(map: MutableMap<String, TimerRecord>, effects: Effects, record: TimerRecord, always: Boolean = false) {
        if (always || map[record.id] != record) effects.changed[record.id] = record
        map[record.id] = record
    }

    private fun runningFrom(record: TimerRecord, left: Long, now: Long): TimerRecord = record.copy(
        state = AlarmContract.TIMER_RUNNING,
        remainingMillis = left,
        endsAt = now + left,
        endsElapsed = elapsed() + left,
        boot = boot(),
    )

    private fun realign(record: TimerRecord, now: Long): TimerRecord {
        if (!record.running) return record
        val currentBoot = boot()
        val endsElapsed = record.endsElapsed
        val sameBoot = currentBoot != null && record.boot == currentBoot && endsElapsed != null
        return if (sameBoot) {
            record.copy(endsAt = now + (endsElapsed - elapsed()))
        } else {
            val end = record.endsAt ?: now
            record.copy(endsElapsed = elapsed() + (end - now), boot = currentBoot)
        }
    }

    private fun require(map: Map<String, TimerRecord>, id: String): TimerRecord {
        val record = map[id] ?: throw AlarmException.notFound("no timer with id '$id'")
        if (record.ringing) throw ringingError(id)
        return record
    }

    private fun requireExact() {
        if (!port.canScheduleExact()) throw AlarmException.permissionRequired("a running timer needs the exact alarm permission")
    }

    private fun ringingError(id: String) = AlarmException.invalidOptions("timer '$id' is ringing; dismiss, snooze or cancel it")

    private fun loaded(): LinkedHashMap<String, TimerRecord> =
        records ?: LinkedHashMap<String, TimerRecord>().also { map ->
            store.load().forEach { map[it.id] = it }
            records = map
        }

    companion object {

        private const val SNOOZE_SEPARATOR = "~"
        private const val ID_RANDOM_CHARS = 10
        private const val ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz"
        private val random = SecureRandom()

        fun randomId(): String = buildString(AlarmContract.TIMER_ID_PREFIX.length + ID_RANDOM_CHARS) {
            append(AlarmContract.TIMER_ID_PREFIX)
            repeat(ID_RANDOM_CHARS) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
        }
    }
}
