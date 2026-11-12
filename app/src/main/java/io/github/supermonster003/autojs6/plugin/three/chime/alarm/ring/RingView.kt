package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingCoordinator

/**
 * What the ringing screen shows for the sounding alarm (roadmap P3.2, T86), free of Android so the JVM tests cover it:
 * the [label] (null for the default label), the length of the snooze button ([snoozeMinutes], null hides the button),
 * the snoozes so far ([snoozed]) and those left ([snoozesLeft], null when the alarm does not snooze), whether its voice
 * [speaking] now, how many alarms wait behind it ([queued]) and whether it is a [timer] (roadmap P7.1: its default label
 * differs).
 *
 * zh-CN: 响铃界面为正在发声的闹钟显示的内容 (路线图 P3.2, T86), 不依赖 Android, 由 JVM 测试覆盖: 标签 [label] (默认标签时为 null), 贪睡按钮的时长
 * [snoozeMinutes] (null 时不显示按钮), 已贪睡次数 [snoozed] 与剩余次数 [snoozesLeft] (闹钟不贪睡时为 null), 此刻是否在朗读 [speaking], 以及排在它
 * 后面的闹钟数 [queued], 以及是否为计时器 [timer] (路线图 P7.1: 默认标签不同).
 */
internal data class RingView(
    val alarmId: String,
    val occurrence: String,
    val label: String?,
    val snoozeMinutes: Int?,
    val snoozed: Int,
    val snoozesLeft: Int?,
    val speaking: Boolean,
    val queued: Int,
    val timer: Boolean = false,
) {

    companion object {

        /**
         * The view of [ring] with [queued] alarms waiting, the speaking `occurrence` [speaking] and whether a snooze
         * can be scheduled at all ([snoozeSchedulable]: the exact alarm permission, T65). The button shows only while
         * the ring itself may snooze ([RingCoordinator.Ring.canSnooze]) and the snooze can be scheduled.
         * zh-CN: [ring] 的显示内容, 排队 [queued] 个闹钟, 正在朗读的 `occurrence` 为 [speaking], [snoozeSchedulable] 表示能否安排贪睡 (精确闹钟权限,
         * T65). 只有本次响铃允许贪睡 ([RingCoordinator.Ring.canSnooze]) 且能安排贪睡时才显示按钮.
         */
        fun of(ring: RingCoordinator.Ring, queued: Int, speaking: String?, snoozeSchedulable: Boolean): RingView {
            val settings = ring.settings
            val snoozed = ring.request.snoozeCount
            return RingView(
                alarmId = ring.alarmId,
                occurrence = ring.occurrence,
                label = ring.label?.takeIf { it.isNotBlank() },
                snoozeMinutes = settings.snoozeMinutes.takeIf { ring.canSnooze && snoozeSchedulable },
                snoozed = snoozed,
                snoozesLeft = if (settings.snoozeEnabled) (settings.snoozeMax - snoozed).coerceAtLeast(0) else null,
                speaking = speaking != null && speaking == ring.occurrence,
                queued = queued.coerceAtLeast(0),
                timer = ring.isTimer,
            )
        }
    }
}
