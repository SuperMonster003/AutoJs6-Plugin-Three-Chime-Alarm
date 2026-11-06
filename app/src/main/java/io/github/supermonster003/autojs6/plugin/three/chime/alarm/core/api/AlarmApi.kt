package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmFilter
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmOptionsValidator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingCoordinator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingtoneLibrary
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.NextOccurrence
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RepeatText
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import org.autojs.plugin.alarm.api.AlarmResultKeys
import org.autojs.plugin.alarm.api.AlarmSnapshot
import org.autojs.plugin.alarm.api.AlarmSpec
import org.autojs.plugin.alarm.api.RingtoneSnapshot
import java.io.InputStream
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * The 1.0.0 methods of `IAlarmPlugin` on value trees (roadmap P2.5, appendix A section 4.1), so JVM tests cover the
 * whole request and answer mapping; the Binder service only decodes and encodes Bundles. The Binder service's instance
 * serves host scripts: changes are recorded with [source] `script` and dismissals with [by] `script` (T18); the plugin
 * tools (roadmap P4.1) use a second instance with `tool`. Requests are checked as
 * appendix A section 11 says: unknown keys and mistyped values are `INVALID_OPTIONS`, counts above their limit
 * `LIMIT_EXCEEDED`; `getAlarm` of an unknown id answers no `alarm`, `dismiss` / `snooze` of an alarm that is not
 * ringing answer `handled` false (T75).
 *
 * zh-CN: 以值树实现 `IAlarmPlugin` 的 1.0.0 方法 (路线图 P2.5, 附录 A 第 4.1 节), 使 JVM 测试覆盖完整的请求与应答映射; Binder 服务只负责 Bundle 的
 * 解码与编码. Binder 服务所用的实例服务于宿主脚本: 变更以 [source] `script` 记录, 关闭以 [by] `script` 记录 (T18); 插件工具 (路线图 P4.1)
 * 另用一个记 `tool` 的实例. 请求按附录 A 第 11 节检查: 未知键与类型不符报
 * `INVALID_OPTIONS`, 数量超出上限报 `LIMIT_EXCEEDED`; `getAlarm` 查不到 id 时应答不含 `alarm`, 对未在响铃的闹钟 `dismiss` / `snooze` 应答 `handled`
 * 为 false (T75).
 */
class AlarmApi(
    private val repository: AlarmRepository,
    private val rings: RingCoordinator,
    private val hints: BindingHints,
    private val library: RingtoneLibrary,
    private val systemRingtones: () -> List<RingtoneSnapshot>,
    private val validator: AlarmOptionsValidator = AlarmOptionsValidator(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val deviceZone: () -> ZoneId = ZoneId::systemDefault,
    private val locale: () -> Locale = Locale::getDefault,
    private val source: String = AlarmContract.SOURCE_SCRIPT,
    private val by: String = AlarmContract.BY_SCRIPT,
) {

    fun addAlarm(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, null)
        return alarmAnswer(repository.add(request.values, source))
    }

    fun getAlarm(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, ID_KEYS)
        val record = repository.get(request.requireString(AlarmOptionKeys.ID)) ?: return emptyMap()
        return alarmAnswer(record)
    }

    fun listAlarms(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, LIST_KEYS)
        val filter = AlarmFilter.fromValues(request.map(AlarmOptionKeys.FILTER))
        return mapOf(AlarmResultKeys.ALARMS to repository.list(filter).map { snapshot(it).toValues() })
    }

    fun updateAlarm(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, UPDATE_KEYS)
        val id = request.requireString(AlarmOptionKeys.ID)
        return alarmAnswer(repository.update(id, request.requireMap(AlarmOptionKeys.PATCH), source))
    }

    fun removeAlarm(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, ID_KEYS)
        return mapOf(AlarmResultKeys.REMOVED to repository.remove(request.requireString(AlarmOptionKeys.ID), source))
    }

    fun setAlarmEnabled(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, ENABLED_KEYS)
        val id = request.requireString(AlarmOptionKeys.ID)
        return alarmAnswer(repository.setEnabled(id, request.requireBoolean(AlarmOptionKeys.ENABLED), source))
    }

    fun skipNext(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, ID_KEYS)
        return alarmAnswer(repository.skipNext(request.requireString(AlarmOptionKeys.ID), source))
    }

    fun cancelSkip(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, ID_KEYS)
        return alarmAnswer(repository.cancelSkip(request.requireString(AlarmOptionKeys.ID), source))
    }

    fun upcoming(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, UPCOMING_KEYS)
        val count = request.count(AlarmOptionKeys.COUNT, 1, AlarmLimits.MAX_UPCOMING_COUNT, AlarmLimits.DEFAULT_UPCOMING_COUNT)
        val filter = AlarmFilter.fromValues(request.map(AlarmOptionKeys.FILTER))
        val occurrences = NextOccurrence.upcoming(repository.list(filter), count, clock(), deviceZone())
        return mapOf(AlarmResultKeys.OCCURRENCES to occurrences.map { it.toValues() })
    }

    fun preview(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, PREVIEW_KEYS)
        val options = request.requireMap(AlarmOptionKeys.OPTIONS)
        val count = request.count(AlarmOptionKeys.COUNT, 1, AlarmLimits.MAX_PREVIEW_COUNT, AlarmLimits.DEFAULT_PREVIEW_COUNT)
        val spec = validator.options(options)
        return mapOf(AlarmResultKeys.TIMES to NextOccurrence.preview(spec, count, clock(), deviceZone()).toLongArray())
    }

    fun ringing(values: Map<String, Any?>): Map<String, Any?> {
        AlarmRequest.of(values, NO_KEYS)
        return mapOf(AlarmResultKeys.RINGING to rings.ringing().map { it.toValues() })
    }

    fun dismiss(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, ID_KEYS)
        return mapOf(AlarmResultKeys.HANDLED to rings.dismiss(request.string(AlarmOptionKeys.ID), by))
    }

    fun snooze(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, SNOOZE_KEYS)
        val id = request.string(AlarmOptionKeys.ID)
        return mapOf(AlarmResultKeys.HANDLED to rings.snooze(id, request.int(AlarmOptionKeys.MINUTES), by))
    }

    /**
     * The system alarm ringtones first, then the imported ones, at most [AlarmLimits.MAX_RINGTONES] (T74).
     * zh-CN: 先列系统闹钟铃声, 再列导入的铃声, 至多 [AlarmLimits.MAX_RINGTONES] 个 (T74).
     */
    fun listRingtones(values: Map<String, Any?>): Map<String, Any?> {
        AlarmRequest.of(values, NO_KEYS)
        val all = (systemRingtones() + library.list()).take(AlarmLimits.MAX_RINGTONES)
        return mapOf(AlarmResultKeys.RINGTONES to all.map { it.toValues() })
    }

    /**
     * Checks the request before [open] is called, then copies the file ([RingtoneLibrary.import]); the given
     * `mimeType` is accepted but the plugin trusts only what it can decode.
     * zh-CN: 先检查请求再调用 [open], 然后复制文件 ([RingtoneLibrary.import]); 接受传入的 `mimeType`, 但插件只信任自己能解码的内容.
     */
    fun importRingtone(values: Map<String, Any?>, open: () -> InputStream): Map<String, Any?> {
        val request = AlarmRequest.of(values, IMPORT_KEYS)
        val displayName = request.requireString(AlarmOptionKeys.DISPLAY_NAME)
        request.string(AlarmOptionKeys.MIME_TYPE)
        val ringtone = open().use { library.import(it, displayName) }
        return mapOf(AlarmResultKeys.RINGTONE to ringtone.toValues())
    }

    /**
     * The events of a `registerEventCallback` request: every event when `events` is absent (T77).
     * zh-CN: `registerEventCallback` 请求的事件: 没有 `events` 时为全部事件 (T77).
     */
    fun eventsOf(values: Map<String, Any?>): Set<String> {
        val request = AlarmRequest.of(values, EVENTS_KEYS)
        val events = request.stringList(AlarmOptionKeys.EVENTS) ?: return AlarmContract.EVENTS
        events.firstOrNull { it !in AlarmContract.EVENTS }?.let { throw AlarmException.invalidOptions("unknown event '$it'") }
        return AlarmContract.EVENTS.filterTo(LinkedHashSet()) { it in events }
    }

    /** `setBindingHints` of the host package [owner]. zh-CN: 宿主包 [owner] 的 `setBindingHints`. */
    fun setBindingHints(owner: String, values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, HINTS_KEYS)
        val unknown = hints.replace(owner, request.values) { repository.get(it) != null }
        return mapOf(AlarmResultKeys.UNKNOWN_IDS to unknown)
    }

    /** The contract snapshot of [record]. zh-CN: [record] 的契约快照. */
    fun snapshot(record: AlarmRecord): AlarmSnapshot = record.toSnapshot(repeatTextOf(record.spec), hints.hasBindings(record.id))

    private fun alarmAnswer(record: AlarmRecord): Map<String, Any?> = mapOf(AlarmResultKeys.ALARM to snapshot(record).toValues())

    private fun repeatTextOf(spec: AlarmSpec): String? = spec.repeat?.let { rule ->
        runCatching { RepeatText.of(rule, spec.date?.let(LocalDate::parse), locale()) }.getOrNull()
    }

    private companion object {

        val NO_KEYS: Set<String> = emptySet()
        val ID_KEYS: Set<String> = setOf(AlarmOptionKeys.ID)
        val LIST_KEYS: Set<String> = setOf(AlarmOptionKeys.FILTER)
        val UPDATE_KEYS: Set<String> = setOf(AlarmOptionKeys.ID, AlarmOptionKeys.PATCH)
        val ENABLED_KEYS: Set<String> = setOf(AlarmOptionKeys.ID, AlarmOptionKeys.ENABLED)
        val UPCOMING_KEYS: Set<String> = setOf(AlarmOptionKeys.COUNT, AlarmOptionKeys.FILTER)
        val PREVIEW_KEYS: Set<String> = setOf(AlarmOptionKeys.OPTIONS, AlarmOptionKeys.COUNT)
        val SNOOZE_KEYS: Set<String> = setOf(AlarmOptionKeys.ID, AlarmOptionKeys.MINUTES)
        val IMPORT_KEYS: Set<String> = setOf(AlarmOptionKeys.DISPLAY_NAME, AlarmOptionKeys.MIME_TYPE)
        val EVENTS_KEYS: Set<String> = setOf(AlarmOptionKeys.EVENTS)
        val HINTS_KEYS: Set<String> = setOf(AlarmOptionKeys.HINTS)
    }
}
