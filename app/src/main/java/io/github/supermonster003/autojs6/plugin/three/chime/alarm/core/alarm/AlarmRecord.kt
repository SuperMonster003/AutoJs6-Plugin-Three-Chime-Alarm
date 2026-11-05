package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm

import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmSnapshot
import org.autojs.plugin.alarm.api.AlarmSpec
import org.autojs.plugin.alarm.api.AlarmValues
import org.autojs.plugin.alarm.api.AlarmValues.putIfPresent

/**
 * One stored alarm (roadmap P2.1): the contract options in their validated wire form ([spec]), who created and last
 * changed it (T18) and the plugin's own [state]. [spec] keeps absent options absent; the plugin defaults (appendix A
 * section 4) apply where the alarm is used. `enabled` is always explicit so the snapshot carries it.
 *
 * zh-CN: 一个已存储的闹钟 (路线图 P2.1): 校验后的线上形态选项 ([spec]), 创建来源与最后修改来源 (T18), 以及插件自己的 [state]. [spec] 中缺省的
 * 选项保持缺省, 使用时再套用插件默认值 (附录 A 第 4 节). `enabled` 总是显式保存, 快照中一定带上.
 */
data class AlarmRecord(
    val id: String,
    val source: String,
    val updatedBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val spec: AlarmSpec,
    val state: AlarmRuntimeState = AlarmRuntimeState(),
) {

    val enabled: Boolean get() = spec.enabled != false

    /** The contract snapshot; [repeatText] comes from the rule engine and [hasBindings] from the hint table. */
    fun toSnapshot(repeatText: String? = null, hasBindings: Boolean = false) = AlarmSnapshot(
        id = id,
        source = source,
        updatedBy = updatedBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        repeatText = repeatText,
        nextTrigger = state.wakeAt.takeIf { enabled },
        skipping = enabled && state.skipping,
        hasBindings = hasBindings,
        spec = spec,
    )

    /** The record document of [AlarmStore]. zh-CN: [AlarmStore] 中的记录文档. */
    fun toValues(): Map<String, Any?> = linkedMapOf(
        KEY_ID to id,
        KEY_SOURCE to source,
        KEY_UPDATED_BY to updatedBy,
        KEY_CREATED_AT to createdAt,
        KEY_UPDATED_AT to updatedAt,
        KEY_OPTIONS to spec.toValues(),
        KEY_STATE to state.toValues(),
    )

    companion object {

        const val KEY_ID = "id"
        const val KEY_SOURCE = "source"
        const val KEY_UPDATED_BY = "updatedBy"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_UPDATED_AT = "updatedAt"
        const val KEY_OPTIONS = "options"
        const val KEY_STATE = "state"

        /** Ids are `a_` plus lower-case letters and digits (D24). zh-CN: id 为 `a_` 加小写字母与数字 (D24). */
        fun isValidId(id: String?): Boolean =
            id != null && id.length in MIN_ID_LENGTH..MAX_ID_LENGTH && id.startsWith(AlarmContract.ALARM_ID_PREFIX) &&
                id.substring(AlarmContract.ALARM_ID_PREFIX.length).all { it in 'a'..'z' || it in '0'..'9' }

        private const val MIN_ID_LENGTH = 3
        private const val MAX_ID_LENGTH = 64

        /**
         * Reads a record document; the options are validated again by [validate] (the full options check of
         * [AlarmOptionsValidator]). Throws [AlarmException] for a damaged record, which [AlarmStore] skips.
         * zh-CN: 读取记录文档, 选项由 [validate] 再校验一次 ([AlarmOptionsValidator] 的完整选项校验). 记录损坏时抛 [AlarmException], 由 [AlarmStore] 跳过.
         */
        fun fromValues(values: Map<String, Any?>, validate: (Map<String, Any?>) -> AlarmSpec): AlarmRecord {
            val id = AlarmValues.string(values, KEY_ID)?.takeIf(::isValidId) ?: throw AlarmException.invalidOptions("record id is missing or malformed")
            val source = AlarmValues.string(values, KEY_SOURCE)?.takeIf { it in AlarmContract.SOURCES }
                ?: throw AlarmException.invalidOptions("record $id: source is missing or unknown")
            val updatedBy = AlarmValues.string(values, KEY_UPDATED_BY)?.takeIf { it in AlarmContract.SOURCES }
                ?: throw AlarmException.invalidOptions("record $id: updatedBy is missing or unknown")
            val createdAt = AlarmValues.long(values, KEY_CREATED_AT)?.takeIf { it >= 0 } ?: throw AlarmException.invalidOptions("record $id: createdAt is missing")
            val updatedAt = AlarmValues.long(values, KEY_UPDATED_AT)?.takeIf { it >= 0 } ?: throw AlarmException.invalidOptions("record $id: updatedAt is missing")
            val options = AlarmValues.map(values, KEY_OPTIONS) ?: throw AlarmException.invalidOptions("record $id: options are missing")
            val spec = try {
                validate(options)
            } catch (e: AlarmException) {
                throw AlarmException(e.code, "record $id: ${e.detail}")
            }
            val state = AlarmValues.map(values, KEY_STATE)?.let(AlarmRuntimeState::fromValues) ?: AlarmRuntimeState()
            return AlarmRecord(id, source, updatedBy, createdAt, updatedAt, spec.copy(enabled = spec.enabled != false), state)
        }
    }
}

/**
 * The plugin's own state of an alarm, never part of the options (roadmap P2.1):
 *
 * - [nextTrigger]: epoch milliseconds of the next planned ring (skips, exclusions applied, a snooze not counted), or
 *   null when the alarm will not ring again.
 * - [skipAt]: the occurrence `skipNext` covers; `skipping` is true while it is set.
 * - [handledThrough]: the latest occurrence that rang, was skipped or was missed; the next one is computed after it.
 * - [occurrence]: id of the current ring round (appendix A section 5.1, T31), at most 128 characters.
 * - [snoozeUntil] / [snoozeCount]: the running snooze of the current round.
 * - [dateImplicit]: `date` was filled in by the plugin (roadmap T48) rather than given by the caller.
 *
 * Unknown or mistyped fields read as their defaults: the state can always be recomputed.
 *
 * zh-CN: 插件自己的闹钟状态, 不属于选项 (路线图 P2.1): [nextTrigger] 为调度器计划的下一次响铃 (已计入跳过与排除, 不计贪睡), 不再响时为 null;
 * [skipAt] 为 `skipNext` 跳过的那一次, 设置期间 `skipping` 为 true; [handledThrough] 为最近一次已响, 已跳过或已错过的时刻, 下一次在其之后计算;
 * [occurrence] 为本轮响铃的 id (附录 A 第 5.1 节, T31), 至多 128 个字符; [snoozeUntil] / [snoozeCount] 为本轮正在进行的贪睡;
 * [dateImplicit] 表示 `date` 由插件补上 (路线图 T48) 而非调用方给出. 未知或类型不符的字段读为默认值, 状态总能重新计算.
 */
data class AlarmRuntimeState(
    val nextTrigger: Long? = null,
    val skipAt: Long? = null,
    val handledThrough: Long? = null,
    val occurrence: String? = null,
    val snoozeUntil: Long? = null,
    val snoozeCount: Int = 0,
    val dateImplicit: Boolean = false,
) {

    val skipping: Boolean get() = skipAt != null

    /**
     * The next wake-up: the end of a running snooze when it comes before the planned ring (roadmap P2.3); the snapshot
     * shows it as `nextTrigger`.
     * zh-CN: 下一次唤醒: 正在进行的贪睡先于计划响铃结束时取贪睡结束时刻 (路线图 P2.3); 快照中作为 `nextTrigger` 显示.
     */
    val wakeAt: Long? get() = listOfNotNull(nextTrigger, snoozeUntil).minOrNull()

    fun toValues(): Map<String, Any?> = LinkedHashMap<String, Any?>().apply {
        putIfPresent(KEY_NEXT_TRIGGER, nextTrigger)
        putIfPresent(KEY_SKIP_AT, skipAt)
        putIfPresent(KEY_HANDLED_THROUGH, handledThrough)
        putIfPresent(KEY_OCCURRENCE, occurrence)
        putIfPresent(KEY_SNOOZE_UNTIL, snoozeUntil)
        if (snoozeCount != 0) put(KEY_SNOOZE_COUNT, snoozeCount)
        if (dateImplicit) put(KEY_DATE_IMPLICIT, true)
    }

    companion object {

        const val KEY_NEXT_TRIGGER = "nextTrigger"
        const val KEY_SKIP_AT = "skipAt"
        const val KEY_HANDLED_THROUGH = "handledThrough"
        const val KEY_OCCURRENCE = "occurrence"
        const val KEY_SNOOZE_UNTIL = "snoozeUntil"
        const val KEY_SNOOZE_COUNT = "snoozeCount"
        const val KEY_DATE_IMPLICIT = "dateImplicit"

        fun fromValues(values: Map<String, Any?>) = AlarmRuntimeState(
            nextTrigger = AlarmValues.long(values, KEY_NEXT_TRIGGER)?.takeIf { it >= 0 },
            skipAt = AlarmValues.long(values, KEY_SKIP_AT)?.takeIf { it >= 0 },
            handledThrough = AlarmValues.long(values, KEY_HANDLED_THROUGH)?.takeIf { it >= 0 },
            occurrence = AlarmValues.string(values, KEY_OCCURRENCE)?.takeIf { it.isNotEmpty() && it.length <= AlarmLimits.MAX_OCCURRENCE_CHARS },
            snoozeUntil = AlarmValues.long(values, KEY_SNOOZE_UNTIL)?.takeIf { it >= 0 },
            snoozeCount = AlarmValues.int(values, KEY_SNOOZE_COUNT)?.takeIf { it in 0..AlarmLimits.MAX_SNOOZE_MAX } ?: 0,
            dateImplicit = AlarmValues.boolean(values, KEY_DATE_IMPLICIT) ?: false,
        )
    }
}
