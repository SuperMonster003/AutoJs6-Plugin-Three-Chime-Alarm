package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.io.AtomicFiles
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerRecord
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmEvent
import org.autojs.plugin.alarm.api.AlarmJson
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import org.autojs.plugin.alarm.api.AlarmValues
import org.autojs.plugin.alarm.api.BindingHint
import java.io.File
import java.io.IOException

/**
 * The binding hint tables of the hosts (roadmap T12, P2.5): every host package that called `setBindingHints` has its
 * own table (the plugin does not assume a single host), one row per alarm with at least one bound script. The tables
 * decide which `ALARM_TRIGGER` broadcasts are sent ([targets]) and the `hasBindings` of the snapshots; they are kept in
 * [file] on device-protected storage, so an alarm that rings in a fresh plugin process before any host binds still
 * reaches its bound scripts. A file that cannot be read starts empty; the host sends its table again at its next call.
 *
 * zh-CN: 各宿主的绑定提示表 (路线图 T12, P2.5): 每个调用过 `setBindingHints` 的宿主包各有一张表 (插件不假定只有一个宿主), 每个至少绑定了一个脚本的闹钟
 * 一行. 提示表决定发送哪些 `ALARM_TRIGGER` 广播 ([targets]) 以及快照的 `hasBindings`; 保存在设备加密存储的 [file] 中, 因此在任何宿主绑定之前于新的插件
 * 进程中响铃的闹钟仍能启动其绑定脚本. 无法读取的文件从空表开始; 宿主在下一次调用时会再次发送提示表.
 */
class BindingHints(private val file: File) {

    private val lock = Any()

    private var tables: LinkedHashMap<String, List<BindingHint>>? = null

    /**
     * Called outside the lock with the alarm ids whose `beforeRing` lead ([beforeRingLead]) changed through [replace] or
     * [forget], so the scheduler sets or cancels their `beforeRing` wake-ups (roadmap P8.4).
     * zh-CN: [replace] 或 [forget] 改变了 `beforeRing` 提前量 ([beforeRingLead]) 时, 在锁外以这些闹钟 id 调用, 由调度器设置或取消其 `beforeRing`
     * 唤醒 (路线图 P8.4).
     */
    @Volatile
    var onBeforeRingChange: ((Set<String>) -> Unit)? = null

    /**
     * Replaces the table of [owner] with the `hints` of [values] (`setBindingHints`) and answers the `unknownIds`: rows
     * whose alarm or timer [known] does not report are left out of the table (an existing timer id is kept since 1.1.0,
     * T124; 1.0.x reported every timer id). A malformed row refuses the
     * whole call with `INVALID_OPTIONS`: an unknown key or event, a repeated alarm, `leadSeconds` outside 15 - 300 or
     * without the `beforeRing` event (T32); more than [MAX_ROWS] rows is `LIMIT_EXCEEDED`, a failed save `IO_FAILED`.
     * zh-CN: 以 [values] 的 `hints` 替换 [owner] 的提示表 (`setBindingHints`), 返回 `unknownIds`: [known] 不认识的闹钟或计时器的行不进表 (自 1.1.0 起
     * 存在的计时器 id 保留, T124; 1.0.x 报告全部计时器 id).
     * 格式错误的行使整个调用报 `INVALID_OPTIONS`: 未知键或事件, 重复的闹钟, `leadSeconds` 不在 15 - 300 或没有 `beforeRing` 事件 (T32); 超过
     * [MAX_ROWS] 行报 `LIMIT_EXCEEDED`, 保存失败报 `IO_FAILED`.
     */
    fun replace(owner: String, values: Map<String, Any?>, known: (String) -> Boolean): List<String> {
        val rows = parse(values)
        val kept = ArrayList<BindingHint>()
        val unknown = ArrayList<String>()
        for (row in rows) {
            if (isValidId(row.alarmId) && known(row.alarmId)) kept += row else unknown += row.alarmId
        }
        val changed = synchronized(lock) {
            val before = loaded()
            val next = LinkedHashMap(before)
            if (kept.isEmpty()) next.remove(owner) else next[owner] = kept
            save(next)
            tables = next
            changedLeads(before, next)
        }
        if (changed.isNotEmpty()) onBeforeRingChange?.invoke(changed)
        return unknown
    }

    /**
     * The `beforeRing` lead of [alarmId] in seconds (roadmap P8.4): the smallest among the rows of every host whose events
     * contain `beforeRing` (a row without `leadSeconds` counts as the default 60), null when no row has the event.
     * zh-CN: [alarmId] 的 `beforeRing` 提前秒数 (路线图 P8.4): 各宿主事件含 `beforeRing` 的行中最小的那个 (没有 `leadSeconds` 的行按默认 60 计),
     * 没有任何行含该事件时为 null.
     */
    fun beforeRingLead(alarmId: String): Int? = synchronized(lock) { leadsOf(loaded())[alarmId] }

    /**
     * The host packages that get the trigger broadcast of [event]: a row of the alarm or timer whose events contain the
     * event type, or any row of it for an event that [ends] it (a `change` with action `remove`, the `dismiss` or
     * `silence` of a timer, T125), so the host can drop the bindings.
     * zh-CN: 应收到 [event] 触发广播的宿主包: 该闹钟或计时器有一行且其事件包含该事件类型; 对 [ends] 的事件 (action 为 `remove` 的 `change`, 计时器的
     * `dismiss` 或 `silence`, T125), 只要有任何一行即可, 以便宿主删除绑定.
     */
    fun targets(event: AlarmEvent): List<String> {
        val removal = ends(event)
        return synchronized(lock) {
            loaded().filterValues { rows -> rows.any { it.alarmId == event.alarmId && (removal || event.type in it.events) } }.keys.toList()
        }
    }

    /**
     * Whether [event] ends its alarm or timer, after which the rows are dropped: a `change` with action `remove`, or
     * the `dismiss` or `silence` of a timer, which removes it (T125).
     * zh-CN: [event] 是否结束其闹钟或计时器 (之后删除其行): action 为 `remove` 的 `change`, 或计时器的 `dismiss` / `silence` (计时器随之删除, T125).
     */
    fun ends(event: AlarmEvent): Boolean = when (event.kind) {
        AlarmContract.KIND_ALARM -> event.type == AlarmContract.EVENT_CHANGE && event.action == AlarmContract.CHANGE_REMOVE
        AlarmContract.KIND_TIMER -> event.type == AlarmContract.EVENT_DISMISS || event.type == AlarmContract.EVENT_SILENCE
        else -> false
    }

    /** Whether any host has a row for [alarmId] (the snapshot's `hasBindings`). zh-CN: 是否有宿主为 [alarmId] 登记了行. */
    fun hasBindings(alarmId: String): Boolean = synchronized(lock) { loaded().values.any { rows -> rows.any { it.alarmId == alarmId } } }

    /** The table of [owner], empty when it has none. zh-CN: [owner] 的提示表, 没有时为空. */
    fun table(owner: String): List<BindingHint> = synchronized(lock) { loaded()[owner].orEmpty() }

    /** The hosts with a table. zh-CN: 有提示表的宿主. */
    fun owners(): List<String> = synchronized(lock) { loaded().keys.toList() }

    /**
     * Drops the rows of a removed alarm from every table (after its `remove` broadcast went out), or only from the table
     * of [only] (a host that answered `unbind`, roadmap D20); a failed save keeps the change in memory and answers false,
     * the next send of the host corrects the file.
     * zh-CN: 从所有提示表删除已删除闹钟的行 (在其 `remove` 广播发出之后), 或只从 [only] 的提示表删除 (应答了 `unbind` 的宿主, 路线图 D20);
     * 保存失败时只在内存中生效并返回 false, 宿主下一次发送时纠正文件.
     */
    fun forget(alarmId: String, only: String? = null): Boolean {
        var changed = emptySet<String>()
        val saved = synchronized(lock) {
            val current = loaded()
            if (current.filterKeys { only == null || it == only }.values.none { rows -> rows.any { it.alarmId == alarmId } }) return true
            val next = LinkedHashMap<String, List<BindingHint>>()
            for ((owner, rows) in current) {
                val left = if (only == null || owner == only) rows.filter { it.alarmId != alarmId } else rows
                if (left.isNotEmpty()) next[owner] = left
            }
            tables = next
            changed = changedLeads(current, next)
            try {
                save(next)
                true
            } catch (_: AlarmException) {
                false
            }
        }
        if (changed.isNotEmpty()) onBeforeRingChange?.invoke(changed)
        return saved
    }

    // ------------------------------------------------------------------ internals

    private fun parse(values: Map<String, Any?>): List<BindingHint> {
        if (values[AlarmOptionKeys.HINTS] == null) throw AlarmException.invalidOptions("missing option '${AlarmOptionKeys.HINTS}'")
        val list = AlarmValues.mapList(values, AlarmOptionKeys.HINTS)
            ?: throw AlarmException.invalidOptions("'${AlarmOptionKeys.HINTS}' must be an array of objects")
        if (list.size > MAX_ROWS) throw AlarmException.limitExceeded("at most $MAX_ROWS binding hint rows, got ${list.size}")
        val seen = HashSet<String>()
        return list.mapIndexed { index, row ->
            val name = "${AlarmOptionKeys.HINTS}[$index]"
            row.keys.firstOrNull { it !in ROW_KEYS }?.let { throw AlarmException.invalidOptions("unknown option '$name.$it'") }
            val alarmId = row[AlarmOptionKeys.HINT_ALARM_ID] as? String
                ?: throw AlarmException.invalidOptions("$name.${AlarmOptionKeys.HINT_ALARM_ID} must be a string")
            if (row[AlarmOptionKeys.EVENTS] == null) throw AlarmException.invalidOptions("missing option '$name.${AlarmOptionKeys.EVENTS}'")
            val events = AlarmValues.stringList(row, AlarmOptionKeys.EVENTS)
                ?: throw AlarmException.invalidOptions("$name.${AlarmOptionKeys.EVENTS} must be an array of strings")
            events.firstOrNull { it !in AlarmContract.EVENTS }?.let { throw AlarmException.invalidOptions("$name.${AlarmOptionKeys.EVENTS}: unknown event '$it'") }
            val lead = row[AlarmOptionKeys.LEAD_SECONDS]?.let {
                AlarmValues.int(row, AlarmOptionKeys.LEAD_SECONDS)
                    ?: throw AlarmException.invalidOptions("$name.${AlarmOptionKeys.LEAD_SECONDS} must be an integer")
            }
            if (lead != null) {
                if (AlarmContract.EVENT_BEFORE_RING !in events) {
                    throw AlarmException.invalidOptions("$name.${AlarmOptionKeys.LEAD_SECONDS} needs the ${AlarmContract.EVENT_BEFORE_RING} event")
                }
                if (lead !in AlarmLimits.MIN_LEAD_SECONDS..AlarmLimits.MAX_LEAD_SECONDS) {
                    throw AlarmException.invalidOptions(
                        "$name.${AlarmOptionKeys.LEAD_SECONDS} must be in ${AlarmLimits.MIN_LEAD_SECONDS} - ${AlarmLimits.MAX_LEAD_SECONDS}, got $lead",
                    )
                }
            }
            if (!seen.add(alarmId)) throw AlarmException.invalidOptions("$name: alarm '$alarmId' appears twice")
            BindingHint(alarmId, AlarmContract.EVENTS.filterTo(LinkedHashSet()) { it in events }, lead)
        }
    }

    private fun changedLeads(before: Map<String, List<BindingHint>>, after: Map<String, List<BindingHint>>): Set<String> {
        val old = leadsOf(before)
        val new = leadsOf(after)
        return (old.keys + new.keys).filterTo(LinkedHashSet()) { old[it] != new[it] }
    }

    private fun leadsOf(tables: Map<String, List<BindingHint>>): Map<String, Int> {
        val leads = HashMap<String, Int>()
        for (rows in tables.values) {
            for (row in rows) {
                if (AlarmContract.EVENT_BEFORE_RING !in row.events) continue
                val lead = row.leadSeconds ?: AlarmLimits.DEFAULT_LEAD_SECONDS
                leads[row.alarmId] = leads[row.alarmId]?.let { minOf(it, lead) } ?: lead
            }
        }
        return leads
    }

    private fun loaded(): LinkedHashMap<String, List<BindingHint>> {
        tables?.let { return it }
        val map = LinkedHashMap<String, List<BindingHint>>()
        try {
            if (file.isFile) {
                val document = AlarmJson.parse(file.readText(Charsets.UTF_8))
                if (document != null && AlarmValues.int(document, KEY_VERSION) == VERSION) {
                    for (entry in AlarmValues.mapList(document, KEY_OWNERS).orEmpty()) {
                        val owner = AlarmValues.string(entry, KEY_PACKAGE)?.takeIf { it.isNotEmpty() } ?: continue
                        val rows = AlarmValues.mapList(entry, AlarmOptionKeys.HINTS).orEmpty()
                            .mapNotNull { runCatching { BindingHint.fromValues(it) }.getOrNull() }
                            .filter { isValidId(it.alarmId) }
                        if (rows.isNotEmpty()) map[owner] = rows
                    }
                }
            }
        } catch (_: IOException) {
            // zh-CN: 读不出的文件从空表开始, 宿主下一次调用时会重新发送.
        } catch (_: RuntimeException) {
            // zh-CN: 格式错误的文件同样从空表开始.
        }
        tables = map
        return map
    }

    private fun save(next: Map<String, List<BindingHint>>) {
        val document = linkedMapOf(
            KEY_FORMAT to FORMAT,
            KEY_VERSION to VERSION,
            KEY_OWNERS to next.map { (owner, rows) -> linkedMapOf(KEY_PACKAGE to owner, AlarmOptionKeys.HINTS to rows.map { it.toValues() }) },
        )
        try {
            AtomicFiles.write(file, AlarmJson.write(document).toByteArray(Charsets.UTF_8), TEMP_PREFIX)
        } catch (e: IOException) {
            throw AlarmException.ioFailed("cannot save ${file.name}: ${e.message}")
        }
    }

    companion object {

        const val FILE_NAME = "hints.json"

        /**
         * Rows per call (T71): far above what a host can bind (300 alarms), so only a broken host meets it.
         * zh-CN: 每次调用的行数上限 (T71): 远高于宿主能绑定的数量 (300 个闹钟), 只有出错的宿主才会碰到.
         */
        const val MAX_ROWS = 1000

        private const val FORMAT = "three-chime-alarm.hints"
        private const val VERSION = 1
        private const val KEY_FORMAT = "format"
        private const val KEY_VERSION = "version"
        private const val KEY_OWNERS = "owners"
        private const val KEY_PACKAGE = "package"
        private const val TEMP_PREFIX = ".hints-"

        /** An alarm id or, since 1.1.0, a timer id (T124). zh-CN: 闹钟 id, 或自 1.1.0 起的计时器 id (T124). */
        private fun isValidId(id: String): Boolean = AlarmRecord.isValidId(id) || TimerRecord.isValidId(id)

        private val ROW_KEYS: Set<String> = linkedSetOf(AlarmOptionKeys.HINT_ALARM_ID, AlarmOptionKeys.EVENTS, AlarmOptionKeys.LEAD_SECONDS)
    }
}
