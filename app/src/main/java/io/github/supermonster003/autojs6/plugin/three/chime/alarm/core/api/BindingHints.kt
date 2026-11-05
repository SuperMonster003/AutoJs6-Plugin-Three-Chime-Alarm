package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.io.AtomicFiles
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
     * Replaces the table of [owner] with the `hints` of [values] (`setBindingHints`) and answers the `unknownIds`: rows
     * whose alarm [known] does not report (timer ids included) are left out of the table. A malformed row refuses the
     * whole call with `INVALID_OPTIONS`: an unknown key or event, a repeated alarm, `leadSeconds` outside 15 - 300 or
     * without the `beforeRing` event (T32); more than [MAX_ROWS] rows is `LIMIT_EXCEEDED`, a failed save `IO_FAILED`.
     * zh-CN: 以 [values] 的 `hints` 替换 [owner] 的提示表 (`setBindingHints`), 返回 `unknownIds`: [known] 不认识的闹钟 (包括计时器 id) 的行不进表.
     * 格式错误的行使整个调用报 `INVALID_OPTIONS`: 未知键或事件, 重复的闹钟, `leadSeconds` 不在 15 - 300 或没有 `beforeRing` 事件 (T32); 超过
     * [MAX_ROWS] 行报 `LIMIT_EXCEEDED`, 保存失败报 `IO_FAILED`.
     */
    fun replace(owner: String, values: Map<String, Any?>, known: (String) -> Boolean): List<String> {
        val rows = parse(values)
        val kept = ArrayList<BindingHint>()
        val unknown = ArrayList<String>()
        for (row in rows) {
            if (AlarmRecord.isValidId(row.alarmId) && known(row.alarmId)) kept += row else unknown += row.alarmId
        }
        synchronized(lock) {
            val next = LinkedHashMap(loaded())
            if (kept.isEmpty()) next.remove(owner) else next[owner] = kept
            save(next)
            tables = next
        }
        return unknown
    }

    /**
     * The host packages that get the trigger broadcast of [event]: a row of the alarm whose events contain the event
     * type, or any row of the alarm for a `change` with action `remove`, so the host can drop the bindings.
     * zh-CN: 应收到 [event] 触发广播的宿主包: 该闹钟有一行且其事件包含该事件类型; 对 action 为 `remove` 的 `change`, 只要该闹钟有任何一行即可,
     * 以便宿主删除绑定.
     */
    fun targets(event: AlarmEvent): List<String> {
        if (event.kind != AlarmContract.KIND_ALARM) return emptyList()
        val removal = event.type == AlarmContract.EVENT_CHANGE && event.action == AlarmContract.CHANGE_REMOVE
        return synchronized(lock) {
            loaded().filterValues { rows -> rows.any { it.alarmId == event.alarmId && (removal || event.type in it.events) } }.keys.toList()
        }
    }

    /** Whether any host has a row for [alarmId] (the snapshot's `hasBindings`). zh-CN: 是否有宿主为 [alarmId] 登记了行. */
    fun hasBindings(alarmId: String): Boolean = synchronized(lock) { loaded().values.any { rows -> rows.any { it.alarmId == alarmId } } }

    /** The table of [owner], empty when it has none. zh-CN: [owner] 的提示表, 没有时为空. */
    fun table(owner: String): List<BindingHint> = synchronized(lock) { loaded()[owner].orEmpty() }

    /** The hosts with a table. zh-CN: 有提示表的宿主. */
    fun owners(): List<String> = synchronized(lock) { loaded().keys.toList() }

    /**
     * Drops the rows of a removed alarm from every table (after its `remove` broadcast went out); a failed save keeps
     * the change in memory and answers false, the next send of the host corrects the file through `unknownIds`.
     * zh-CN: 从所有提示表删除已删除闹钟的行 (在其 `remove` 广播发出之后); 保存失败时只在内存中生效并返回 false, 宿主下一次发送时经 `unknownIds` 纠正文件.
     */
    fun forget(alarmId: String): Boolean = synchronized(lock) {
        val current = loaded()
        if (current.values.none { rows -> rows.any { it.alarmId == alarmId } }) return true
        val next = LinkedHashMap<String, List<BindingHint>>()
        for ((owner, rows) in current) {
            val left = rows.filter { it.alarmId != alarmId }
            if (left.isNotEmpty()) next[owner] = left
        }
        tables = next
        try {
            save(next)
            true
        } catch (_: AlarmException) {
            false
        }
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
                            .filter { AlarmRecord.isValidId(it.alarmId) }
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

        private val ROW_KEYS: Set<String> = linkedSetOf(AlarmOptionKeys.HINT_ALARM_ID, AlarmOptionKeys.EVENTS, AlarmOptionKeys.LEAD_SECONDS)
    }
}
