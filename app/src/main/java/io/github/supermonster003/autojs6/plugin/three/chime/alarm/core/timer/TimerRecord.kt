package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmValues
import org.autojs.plugin.alarm.api.AlarmValues.putIfPresent
import org.autojs.plugin.alarm.api.TimerSnapshot
import org.autojs.plugin.alarm.api.TimerSpec

/**
 * One stored timer (roadmap P7.1, T108):
 *
 * - [durationMillis]: the start duration; `reset` returns to it and `addTime` leaves it as it is.
 * - [state]: `running`, `paused` or `ringing`.
 * - [remainingMillis]: the time left while paused (0 while ringing; informational while running).
 * - [endsAt]: wall-clock end while running; [endsElapsed] / [boot] hold the same end on the elapsed-realtime clock of
 *   boot [boot], so a clock change inside one boot does not move the end (T110).
 * - [occurrence] / [ringingAt] / [snoozeUntil] / [snoozeCount] / [ringSince]: the ring round, as for an alarm.
 *
 * zh-CN: 一个已存储的计时器 (路线图 P7.1, T108): [durationMillis] 为开始时长, `reset` 回到它, `addTime` 不改它; [state] 为 `running` / `paused` /
 * `ringing`; [remainingMillis] 为暂停时的剩余时间 (响铃时为 0, 运行时仅供参考); [endsAt] 为运行时的墙钟到点时刻, [endsElapsed] / [boot] 为同一到点
 * 时刻在第 [boot] 次开机的开机时长时钟上的值, 同一次开机内改系统时间不移动到点 (T110); [occurrence] / [ringingAt] / [snoozeUntil] / [snoozeCount] /
 * [ringSince] 为响铃轮次, 与闹钟相同.
 */
data class TimerRecord(
    val id: String,
    val source: String,
    val createdAt: Long,
    val spec: TimerSpec,
    val durationMillis: Long,
    val state: String,
    val remainingMillis: Long,
    val endsAt: Long? = null,
    val endsElapsed: Long? = null,
    val boot: Int? = null,
    val occurrence: String? = null,
    val ringingAt: Long? = null,
    val snoozeUntil: Long? = null,
    val snoozeCount: Int = 0,
    val ringSince: Long? = null,
) {

    val running: Boolean get() = state == AlarmContract.TIMER_RUNNING
    val paused: Boolean get() = state == AlarmContract.TIMER_PAUSED
    val ringing: Boolean get() = state == AlarmContract.TIMER_RINGING

    /** The contract snapshot at [now]. zh-CN: [now] 时刻的契约快照. */
    fun toSnapshot(now: Long): TimerSnapshot = TimerSnapshot(
        id = id,
        durationMillis = durationMillis,
        remainingMillis = when {
            running -> ((endsAt ?: now) - now).coerceAtLeast(0L)
            ringing -> 0L
            else -> remainingMillis
        },
        state = state,
        endsAt = endsAt.takeIf { running },
        spec = spec,
    )

    fun toValues(): Map<String, Any?> = LinkedHashMap<String, Any?>().apply {
        put(KEY_ID, id)
        put(KEY_SOURCE, source)
        put(KEY_CREATED_AT, createdAt)
        put(KEY_OPTIONS, spec.toValues())
        put(KEY_DURATION, durationMillis)
        put(KEY_STATE, state)
        put(KEY_REMAINING, remainingMillis)
        putIfPresent(KEY_ENDS_AT, endsAt)
        putIfPresent(KEY_ENDS_ELAPSED, endsElapsed)
        putIfPresent(KEY_BOOT, boot)
        putIfPresent(KEY_OCCURRENCE, occurrence)
        putIfPresent(KEY_RINGING_AT, ringingAt)
        putIfPresent(KEY_SNOOZE_UNTIL, snoozeUntil)
        if (snoozeCount != 0) put(KEY_SNOOZE_COUNT, snoozeCount)
        putIfPresent(KEY_RING_SINCE, ringSince)
    }

    companion object {

        private const val KEY_ID = "id"
        private const val KEY_SOURCE = "source"
        private const val KEY_CREATED_AT = "createdAt"
        private const val KEY_OPTIONS = "options"
        private const val KEY_DURATION = "durationMillis"
        private const val KEY_STATE = "state"
        private const val KEY_REMAINING = "remainingMillis"
        private const val KEY_ENDS_AT = "endsAt"
        private const val KEY_ENDS_ELAPSED = "endsElapsed"
        private const val KEY_BOOT = "boot"
        private const val KEY_OCCURRENCE = "occurrence"
        private const val KEY_RINGING_AT = "ringingAt"
        private const val KEY_SNOOZE_UNTIL = "snoozeUntil"
        private const val KEY_SNOOZE_COUNT = "snoozeCount"
        private const val KEY_RING_SINCE = "ringSince"

        private val STATES = setOf(AlarmContract.TIMER_RUNNING, AlarmContract.TIMER_PAUSED, AlarmContract.TIMER_RINGING)

        /** Ids are `t_` plus lower-case letters and digits (D24). zh-CN: id 为 `t_` 加小写字母与数字 (D24). */
        fun isValidId(id: String?): Boolean =
            id != null && id.length in 3..64 && id.startsWith(AlarmContract.TIMER_ID_PREFIX) &&
                id.substring(AlarmContract.TIMER_ID_PREFIX.length).all { it in 'a'..'z' || it in '0'..'9' }

        /**
         * Reads a stored timer; the options are validated again by [validate]. Throws [AlarmException] for a damaged
         * entry, which the store skips.
         * zh-CN: 读取已存储的计时器, 选项由 [validate] 再校验一次. 条目损坏时抛 [AlarmException], 由存储跳过.
         */
        fun fromValues(values: Map<String, Any?>, validate: (Map<String, Any?>) -> TimerSpec): TimerRecord {
            val id = AlarmValues.string(values, KEY_ID)?.takeIf(::isValidId) ?: throw damaged("timer id is missing or malformed")
            val source = AlarmValues.string(values, KEY_SOURCE)?.takeIf { it in AlarmContract.SOURCES } ?: throw damaged("timer $id: source")
            val createdAt = AlarmValues.long(values, KEY_CREATED_AT)?.takeIf { it >= 0 } ?: throw damaged("timer $id: createdAt")
            val options = AlarmValues.map(values, KEY_OPTIONS) ?: throw damaged("timer $id: options")
            val duration = AlarmValues.long(values, KEY_DURATION)?.takeIf { it in TimerDurations.MIN_MILLIS..TimerDurations.MAX_MILLIS }
                ?: throw damaged("timer $id: durationMillis")
            val state = AlarmValues.string(values, KEY_STATE)?.takeIf { it in STATES } ?: throw damaged("timer $id: state")
            val remaining = AlarmValues.long(values, KEY_REMAINING)?.takeIf { it in 0..TimerDurations.MAX_MILLIS } ?: throw damaged("timer $id: remainingMillis")
            val endsAt = AlarmValues.long(values, KEY_ENDS_AT)?.takeIf { it >= 0 }
            if (state == AlarmContract.TIMER_RUNNING && endsAt == null) throw damaged("timer $id: a running timer needs endsAt")
            return TimerRecord(
                id = id,
                source = source,
                createdAt = createdAt,
                spec = validate(options),
                durationMillis = duration,
                state = state,
                remainingMillis = remaining,
                endsAt = endsAt,
                endsElapsed = AlarmValues.long(values, KEY_ENDS_ELAPSED),
                boot = AlarmValues.int(values, KEY_BOOT),
                occurrence = AlarmValues.string(values, KEY_OCCURRENCE)?.takeIf { it.isNotEmpty() && it.length <= AlarmLimits.MAX_OCCURRENCE_CHARS },
                ringingAt = AlarmValues.long(values, KEY_RINGING_AT)?.takeIf { it >= 0 },
                snoozeUntil = AlarmValues.long(values, KEY_SNOOZE_UNTIL)?.takeIf { it >= 0 },
                snoozeCount = AlarmValues.int(values, KEY_SNOOZE_COUNT)?.takeIf { it in 0..AlarmLimits.MAX_SNOOZE_MAX } ?: 0,
                ringSince = AlarmValues.long(values, KEY_RING_SINCE)?.takeIf { it >= 0 },
            )
        }

        private fun damaged(detail: String) = AlarmException.invalidOptions(detail)
    }
}
