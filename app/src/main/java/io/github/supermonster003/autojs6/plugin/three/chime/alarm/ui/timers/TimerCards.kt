package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.timers

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerDurations
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerRecord

/**
 * What a timer card shows (roadmap P7.4, T136): [RUNNING] counts down, [PAUSED] holds part of the time, [READY] holds the
 * whole start duration (a new reset), [RINGING] rings and [SNOOZED] waits to ring again; the last two only offer stop.
 * zh-CN: 计时器卡片显示的阶段 (路线图 P7.4, T136): [RUNNING] 倒计时, [PAUSED] 暂停在中途, [READY] 停在完整的开始时长 (刚重置), [RINGING] 响铃中,
 * [SNOOZED] 贪睡等待再响; 后两者只能停止.
 */
internal enum class TimerPhase { RUNNING, PAUSED, READY, RINGING, SNOOZED }

/** The buttons of a card. zh-CN: 卡片上的按钮. */
internal enum class TimerAction { RESET, ADD_MINUTE, PAUSE, RESUME, START, STOP, DELETE }

/**
 * One card of the timers page, computed from a [TimerRecord] at a given time so that the page logic is testable on the
 * JVM (roadmap P7.4).
 * zh-CN: 计时器页的一张卡片, 由 [TimerRecord] 在给定时刻算出, 使页面逻辑可在 JVM 上测试 (路线图 P7.4).
 */
internal data class TimerCard(
    val id: String,
    val label: String?,
    val durationMillis: Long,
    val remainingMillis: Long,
    val phase: TimerPhase,
    val endsAt: Long?,
    val snoozeUntil: Long?,
    val createdAt: Long,
    val ringingAt: Long?,
) {

    val ringing: Boolean get() = phase == TimerPhase.RINGING || phase == TimerPhase.SNOOZED

    /** The share of the start duration still left, 0 - 1. zh-CN: 开始时长中剩余的比例, 0 - 1. */
    val progress: Float get() = if (durationMillis <= 0L) 0f else (remainingMillis.toFloat() / durationMillis).coerceIn(0f, 1f)

    /** The clock reading, whole seconds rounded up ("25:00", "1:30:00"). zh-CN: 钟面读数, 秒向上取整. */
    val clockText: String get() = TimerDurations.clockText(remainingMillis)

    /** One more minute keeps the time left within 100 h (T112). zh-CN: 加一分钟后剩余时间仍不超过 100 小时 (T112). */
    val canAddMinute: Boolean get() = !ringing && remainingMillis + ADD_MINUTE_MILLIS <= TimerDurations.MAX_MILLIS

    /**
     * The buttons in display order: a ringing or snoozed timer only stops (the engine refuses the rest, T114); a ready one
     * starts; the others reset, add a minute and pause or resume. Delete sits apart in the card's corner.
     * zh-CN: 按显示顺序的按钮: 响铃或贪睡中的计时器只能停止 (引擎拒绝其余操作, T114); 就绪的可以开始; 其余可重置, 加一分钟, 暂停或继续. 删除单独放在卡片角上.
     */
    val actions: List<TimerAction>
        get() = when (phase) {
            TimerPhase.RINGING, TimerPhase.SNOOZED -> listOf(TimerAction.STOP)
            TimerPhase.READY -> listOf(TimerAction.ADD_MINUTE, TimerAction.START)
            TimerPhase.PAUSED -> listOf(TimerAction.RESET, TimerAction.ADD_MINUTE, TimerAction.RESUME)
            TimerPhase.RUNNING -> listOf(TimerAction.RESET, TimerAction.ADD_MINUTE, TimerAction.PAUSE)
        }

    /** Whether the corner delete button shows (not while ringing: stop ends and removes it). zh-CN: 是否显示角上的删除按钮 (响铃时不显示: 停止即结束并删除). */
    val deletable: Boolean get() = !ringing

    companion object {

        const val ADD_MINUTE_MILLIS = 60_000L

        fun of(record: TimerRecord, now: Long): TimerCard {
            val snapshot = record.toSnapshot(now)
            val phase = when {
                record.ringing && (record.snoozeUntil ?: 0L) > now -> TimerPhase.SNOOZED
                record.ringing -> TimerPhase.RINGING
                record.running -> TimerPhase.RUNNING
                snapshot.remainingMillis == record.durationMillis -> TimerPhase.READY
                else -> TimerPhase.PAUSED
            }
            return TimerCard(
                id = record.id,
                label = record.spec.label?.takeIf { it.isNotBlank() },
                durationMillis = record.durationMillis,
                remainingMillis = snapshot.remainingMillis,
                phase = phase,
                endsAt = snapshot.endsAt,
                snoozeUntil = record.snoozeUntil.takeIf { phase == TimerPhase.SNOOZED },
                createdAt = record.createdAt,
                ringingAt = record.ringingAt,
            )
        }

        /**
         * The cards in display order (T136): ringing and snoozed timers first (the earliest ring on top), then the others
         * newest first, so a timer just started shows at the top next to the presets.
         * zh-CN: 按显示顺序排列的卡片 (T136): 响铃与贪睡中的在前 (最早响铃的在最上), 其余按创建时间从新到旧, 刚启动的计时器显示在顶部, 靠近预设.
         */
        fun cardsOf(records: List<TimerRecord>, now: Long): List<TimerCard> = records.map { of(it, now) }
            .sortedWith(compareBy<TimerCard> { if (it.ringing) 0 else 1 }.thenBy { if (it.ringing) it.ringingAt ?: it.createdAt else 0L }.thenByDescending { it.createdAt }.thenBy { it.id })
    }
}

/**
 * The preset durations of the timers page (roadmap P7.4, T137): 1, 3, 5, 10, 15 and 30 minutes and 1 hour; one tap
 * starts a timer.
 * zh-CN: 计时器页的预设时长 (路线图 P7.4, T137): 1, 3, 5, 10, 15, 30 分钟与 1 小时; 点一下即启动计时器.
 */
internal object TimerPresets {
    val MILLIS: List<Long> = listOf(1L, 3L, 5L, 10L, 15L, 30L, 60L).map { it * 60_000L }
}

/**
 * The custom duration form (roadmap P7.4, T137): hours 0 - 100, minutes and seconds 0 - 59, digits only; the total must
 * lie within 1 s - 100 h like every timer (T107).
 * zh-CN: 自定义时长表单 (路线图 P7.4, T137): 小时 0 - 100, 分钟与秒 0 - 59, 只接受数字; 总时长须在 1 秒 - 100 小时内, 与所有计时器相同 (T107).
 */
internal object TimerInput {

    const val MAX_HOURS = 100
    const val MAX_DIGITS = 3

    /** Keeps the digits of a typed field, at most [MAX_DIGITS] of them. zh-CN: 保留输入字段中的数字, 至多 [MAX_DIGITS] 位. */
    fun digits(text: String): String = text.filter { it in '0'..'9' }.take(MAX_DIGITS)

    /**
     * The milliseconds of the three fields (an empty field counts as 0), or null when a field is out of range or the
     * total is not a timer duration.
     * zh-CN: 三个字段的毫秒数 (空字段按 0 计), 字段越界或总时长不是合法计时器时长时为 null.
     */
    fun millisOf(hours: String, minutes: String, seconds: String): Long? {
        val h = field(hours, MAX_HOURS) ?: return null
        val m = field(minutes, 59) ?: return null
        val s = field(seconds, 59) ?: return null
        val total = ((h * 60L + m) * 60L + s) * 1000L
        return total.takeIf { it in TimerDurations.MIN_MILLIS..TimerDurations.MAX_MILLIS }
    }

    private fun field(text: String, max: Int): Int? {
        if (text.isEmpty()) return 0
        if (text.length > MAX_DIGITS || text.any { it !in '0'..'9' }) return null
        return text.toInt().takeIf { it <= max }
    }
}
