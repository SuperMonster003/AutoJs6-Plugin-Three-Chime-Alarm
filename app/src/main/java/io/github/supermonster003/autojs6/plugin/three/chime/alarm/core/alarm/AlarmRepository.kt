package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.NextOccurrence
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.TimeZonePolicy
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import org.autojs.plugin.alarm.api.AlarmSpec
import java.security.SecureRandom
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A change of the alarm list (the source of the `change` event, appendix A section 5.1): [action] is `add`, `update` or
 * `remove`, [source] is who made it (T18). [before] / [after] are the records around the change.
 * zh-CN: 闹钟列表的一次变更 (`change` 事件的来源, 附录 A 第 5.1 节): [action] 为 `add`, `update` 或 `remove`, [source] 为变更者 (T18).
 */
data class AlarmChange(
    val action: String,
    val alarmId: String,
    val source: String,
    val before: AlarmRecord?,
    val after: AlarmRecord?,
)

/**
 * The alarm list of the plugin (roadmap P2.1): every caller (Binder service, plugin tools, system clock intents,
 * the plugin's own screens) goes through it, so the limits (300 alarms, of which 200 enabled, `LIMIT_EXCEEDED`), the
 * change notification and the source record (T18) hold for all of them. Every change is saved before it becomes
 * visible; a failed save throws `IO_FAILED` and leaves the list unchanged.
 *
 * A `time` alarm without `date` gets one filled in (appendix A section 4.2, T48): today for a repeating alarm, the next
 * day the time is reached for a one-shot alarm (in the alarm's `timeZone`, else the device zone). Such an implicit date
 * follows later changes of the time, rule or zone, and a one-shot `time` alarm enabled again after its moment passed
 * moves to the next day the time is reached.
 *
 * [planner] computes the plugin state (`nextTrigger` and the rest) of a record after each change; the scheduler of
 * roadmap P2.3 plugs in there and listens to the changes. [updateState] changes only the state (rings, skips, snoozes)
 * and sends no change.
 *
 * zh-CN: 插件的闹钟列表 (路线图 P2.1): 所有调用方 (Binder 服务, 插件工具, 系统时钟 Intent, 插件自己的界面) 都经过它, 因此上限 (300 条, 其中启用
 * 200 条, `LIMIT_EXCEEDED`), 变更通知与来源记录 (T18) 对所有调用方都成立. 每次变更先保存再生效; 保存失败时报 `IO_FAILED`, 列表不变.
 * 没有 `date` 的 `time` 闹钟会补上日期 (附录 A 第 4.2 节, T48): 重复闹钟取今天, 一次性闹钟取下一个到达该时刻的日子 (按闹钟的 `timeZone`, 否则按设备时区).
 * 这种隐式日期随之后对时刻, 规则或时区的修改而更新; 一次性 `time` 闹钟在时刻已过后再次启用时, 改到下一个到达该时刻的日子.
 * [planner] 在每次变更后计算记录的插件状态 (`nextTrigger` 等), 路线图 P2.3 的调度器接在这里并监听变更. [updateState] 只改状态 (响铃, 跳过, 贪睡),
 * 不发变更通知.
 */
class AlarmRepository(
    private val store: AlarmStore,
    private val validator: AlarmOptionsValidator = AlarmOptionsValidator(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val deviceZone: () -> ZoneId = ZoneId::systemDefault,
    private val idGenerator: () -> String = ::randomId,
    private val planner: (AlarmRecord) -> AlarmRuntimeState = AlarmRecord::state,
) {

    private val lock = Any()

    private var records: LinkedHashMap<String, AlarmRecord>? = null

    private val listeners = CopyOnWriteArrayList<(AlarmChange) -> Unit>()

    /** The result of the first load, for diagnostics and the plugin screens. zh-CN: 首次加载的结果, 供诊断与插件界面使用. */
    @Volatile
    var loadResult: AlarmStore.LoadResult? = null
        private set

    fun addListener(listener: (AlarmChange) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (AlarmChange) -> Unit) {
        listeners -= listener
    }

    /** Alarms in creation order. zh-CN: 按创建顺序排列的闹钟. */
    fun list(filter: AlarmFilter = AlarmFilter.ALL): List<AlarmRecord> = synchronized(lock) {
        loaded().values.filter(filter::matches)
    }

    fun get(id: String): AlarmRecord? = synchronized(lock) { loaded()[id] }

    fun add(values: Map<String, Any?>, source: String): AlarmRecord {
        requireSource(source)
        val spec = validator.options(values)
        val change = synchronized(lock) {
            val all = loaded()
            if (all.size >= AlarmLimits.MAX_ALARMS) throw AlarmException.limitExceeded("at most ${AlarmLimits.MAX_ALARMS} alarms")
            val enabled = spec.enabled != false
            if (enabled) checkEnabledLimit(all)
            val now = clock()
            var id = idGenerator()
            var attempts = 0
            while (id in all || !AlarmRecord.isValidId(id)) {
                if (++attempts > MAX_ID_ATTEMPTS) throw AlarmException(AlarmErrorCodes.INTERNAL, "cannot generate an alarm id")
                id = idGenerator()
            }
            val (resolved, implicit) = resolveDate(spec.copy(enabled = enabled), now, force = false)
            val record = plan(AlarmRecord(id, source, source, now, now, resolved, AlarmRuntimeState(dateImplicit = implicit)))
            commit(all, record)
            AlarmChange(AlarmContract.CHANGE_ADD, id, source, null, record)
        }
        notify(change)
        return change.after!!
    }

    /** Applies a T29 patch ([AlarmOptionsValidator.patch]); an unknown [id] throws `NOT_FOUND`. zh-CN: 应用 T29 patch; 未知 [id] 报 `NOT_FOUND`. */
    fun update(id: String, patch: Map<String, Any?>, by: String): AlarmRecord {
        requireSource(by)
        val change = synchronized(lock) {
            val all = loaded()
            val current = all[id] ?: throw AlarmException.notFound("no alarm with id '$id'")
            val followsTime = DATE_FOLLOWS_KEYS.any(patch::containsKey) && !patch.containsKey(AlarmOptionKeys.DATE)
            val base = if (current.state.dateImplicit && followsTime) current.spec.copy(date = null) else current.spec
            val merged = validator.patch(base, patch)
            val spec = merged.copy(enabled = merged.enabled != false)
            val enabling = !current.enabled && spec.enabled == true
            if (enabling) checkEnabledLimit(all)
            val now = clock()
            val reenable = enabling && !patch.containsKey(AlarmOptionKeys.DATE) && passed(spec, now)
            val (resolved, resolvedNow) = resolveDate(spec, now, force = reenable)
            val implicit = resolvedNow || (current.state.dateImplicit && resolved.time != null && !patch.containsKey(AlarmOptionKeys.DATE))
            val state = stateFor(current.state, spec.enabled == true).copy(dateImplicit = implicit)
            val record = plan(current.copy(updatedBy = by, updatedAt = now, spec = resolved, state = state))
            commit(all, record)
            AlarmChange(AlarmContract.CHANGE_UPDATE, id, by, current, record)
        }
        notify(change)
        return change.after!!
    }

    /** Enables or disables; an unchanged state answers the record as it is, without a change. zh-CN: 启用或禁用; 状态未变时原样返回, 不发变更. */
    fun setEnabled(id: String, enabled: Boolean, by: String): AlarmRecord {
        requireSource(by)
        val change = synchronized(lock) {
            val all = loaded()
            val current = all[id] ?: throw AlarmException.notFound("no alarm with id '$id'")
            if (current.enabled == enabled) return current
            if (enabled) checkEnabledLimit(all)
            val now = clock()
            val spec = current.spec.copy(enabled = enabled)
            val (resolved, resolvedNow) = resolveDate(spec, now, force = enabled && passed(spec, now))
            val state = stateFor(current.state, enabled).let { if (resolvedNow) it.copy(dateImplicit = true) else it }
            val record = plan(current.copy(updatedBy = by, updatedAt = now, spec = resolved, state = state))
            commit(all, record)
            AlarmChange(AlarmContract.CHANGE_UPDATE, id, by, current, record)
        }
        notify(change)
        return change.after!!
    }

    /** Removes the alarm; false when [id] is unknown. zh-CN: 删除闹钟; [id] 未知时返回 false. */
    fun remove(id: String, by: String): Boolean {
        requireSource(by)
        val change = synchronized(lock) {
            val all = loaded()
            val current = all[id] ?: return false
            val next = LinkedHashMap(all).apply { remove(id) }
            store.save(next.values.toList())
            records = next
            AlarmChange(AlarmContract.CHANGE_REMOVE, id, by, current, null)
        }
        notify(change)
        return true
    }

    /**
     * Changes the plugin state of one alarm (the scheduler and the ring service); no change is sent and `updatedBy` /
     * `updatedAt` stay. Answers null for an unknown [id].
     * zh-CN: 修改一个闹钟的插件状态 (调度器与响铃服务使用), 不发变更, `updatedBy` / `updatedAt` 不变. [id] 未知时返回 null.
     */
    fun updateState(id: String, transform: (AlarmRecord) -> AlarmRuntimeState): AlarmRecord? = synchronized(lock) {
        val all = loaded()
        val current = all[id] ?: return null
        val state = transform(current)
        if (state == current.state) return current
        val record = current.copy(state = state)
        commit(all, record)
        record
    }

    /** [updateState] for every alarm with one save (time or zone changes, boot). zh-CN: 对全部闹钟执行 [updateState], 只保存一次. */
    fun updateStates(transform: (AlarmRecord) -> AlarmRuntimeState): List<AlarmRecord> = synchronized(lock) {
        val all = loaded()
        val next = LinkedHashMap<String, AlarmRecord>()
        var changed = false
        for ((id, record) in all) {
            val state = transform(record)
            if (state != record.state) changed = true
            next[id] = if (state == record.state) record else record.copy(state = state)
        }
        if (changed) {
            store.save(next.values.toList())
            records = next
        }
        next.values.toList()
    }

    // ------------------------------------------------------------------ internals

    private fun loaded(): LinkedHashMap<String, AlarmRecord> {
        records?.let { return it }
        val result = store.load(validator::options)
        loadResult = result
        val map = LinkedHashMap<String, AlarmRecord>()
        result.records.forEach { map[it.id] = it }
        records = map
        return map
    }

    private fun commit(all: LinkedHashMap<String, AlarmRecord>, record: AlarmRecord) {
        val next = LinkedHashMap(all).apply { put(record.id, record) }
        store.save(next.values.toList())
        records = next
    }

    private fun plan(record: AlarmRecord): AlarmRecord = record.copy(state = planner(record))

    private fun checkEnabledLimit(all: Map<String, AlarmRecord>) {
        if (all.values.count(AlarmRecord::enabled) >= AlarmLimits.MAX_ENABLED_ALARMS) {
            throw AlarmException.limitExceeded("at most ${AlarmLimits.MAX_ENABLED_ALARMS} enabled alarms")
        }
    }

    /** Disabling drops the skip and the snooze of the alarm. zh-CN: 禁用时清除跳过与贪睡. */
    private fun stateFor(state: AlarmRuntimeState, enabled: Boolean): AlarmRuntimeState =
        if (enabled) state else state.copy(nextTrigger = null, skipAt = null, snoozeUntil = null, snoozeCount = 0)

    private fun zoneOf(spec: AlarmSpec): ZoneId = TimeZonePolicy.zoneOf(spec, deviceZone())

    /** A one-shot `time` alarm whose date and time are not after [now]. zh-CN: 日期与时刻不晚于 [now] 的一次性 `time` 闹钟. */
    private fun passed(spec: AlarmSpec, now: Long): Boolean {
        val time = spec.time ?: return false
        if (spec.repeat != null) return false
        val date = spec.date?.let(LocalDate::parse) ?: return false
        return TimeZonePolicy.millisOf(date.atTime(time.hour, time.minute, time.second), zoneOf(spec)) <= now
    }

    /** Fills in the implicit date (T48); the second value is true when it did. zh-CN: 补上隐式日期 (T48); 第二个值表示是否补了. */
    private fun resolveDate(spec: AlarmSpec, now: Long, force: Boolean): Pair<AlarmSpec, Boolean> {
        if (spec.time == null) return spec to false
        if (spec.date != null && !force) return spec to false
        return spec.copy(date = NextOccurrence.implicitDate(spec, now, zoneOf(spec)).toString()) to true
    }

    private fun notify(change: AlarmChange) {
        for (listener in listeners) {
            try {
                listener(change)
            } catch (_: RuntimeException) {
                // A failing listener must not undo a saved change or starve the other listeners.
                // zh-CN: 监听器出错不能撤销已保存的变更, 也不能影响其他监听器.
            }
        }
    }

    private fun requireSource(source: String) {
        require(source in AlarmContract.SOURCES) { "unknown source '$source'" }
    }

    companion object {

        /** Keys whose change moves an implicit date along (T48). zh-CN: 修改后隐式日期随之更新的键 (T48). */
        private val DATE_FOLLOWS_KEYS: Set<String> = linkedSetOf(
            AlarmOptionKeys.HOUR, AlarmOptionKeys.MINUTE, AlarmOptionKeys.SECOND, AlarmOptionKeys.REPEAT, AlarmOptionKeys.TIME_ZONE,
        )

        private const val MAX_ID_ATTEMPTS = 16

        private const val ID_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz"

        /** Characters after the `a_` prefix: 36^12, about 4.7e18 ids. zh-CN: `a_` 前缀后的字符数. */
        const val ID_RANDOM_CHARS = 12

        private val random = SecureRandom()

        fun randomId(): String = buildString(AlarmContract.ALARM_ID_PREFIX.length + ID_RANDOM_CHARS) {
            append(AlarmContract.ALARM_ID_PREFIX)
            repeat(ID_RANDOM_CHARS) { append(ID_ALPHABET[random.nextInt(ID_ALPHABET.length)]) }
        }
    }
}
