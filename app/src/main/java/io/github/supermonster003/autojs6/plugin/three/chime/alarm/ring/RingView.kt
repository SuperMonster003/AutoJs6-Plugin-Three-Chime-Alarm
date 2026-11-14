package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingCoordinator
import org.autojs.plugin.alarm.api.AlarmDismiss

/**
 * What the ringing screen shows for the sounding alarm (roadmap P3.2, T86), free of Android so the JVM tests cover it:
 * the [label] (null for the default label), the length of the snooze button ([snoozeMinutes], null hides the button),
 * the snoozes so far ([snoozed]) and those left ([snoozesLeft], null when the alarm does not snooze), whether its voice
 * [speaking] now, how many alarms wait behind it ([queued]) and whether it is a [timer] (roadmap P7.1: its default label
 * differs). With a dismiss challenge in effect (roadmap P9.5, T286) it also carries the [challenge], its [dismiss]
 * options, the auto silence time [silenceAt] the challenge layer counts down to, the [seed] of the round's problems and
 * whether the [emergency] dismiss is on.
 *
 * zh-CN: 响铃界面为正在发声的闹钟显示的内容 (路线图 P3.2, T86), 不依赖 Android, 由 JVM 测试覆盖: 标签 [label] (默认标签时为 null), 贪睡按钮的时长
 * [snoozeMinutes] (null 时不显示按钮), 已贪睡次数 [snoozed] 与剩余次数 [snoozesLeft] (闹钟不贪睡时为 null), 此刻是否在朗读 [speaking], 以及排在它
 * 后面的闹钟数 [queued], 以及是否为计时器 [timer] (路线图 P7.1: 默认标签不同). 有生效的关闭挑战时 (路线图 P9.5, T286) 还带挑战 [challenge], 其选项
 * [dismiss], 挑战层倒计时的自动停止时刻 [silenceAt], 本轮题目的种子 [seed], 以及紧急关闭是否开启 [emergency].
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
    val challenge: String? = null,
    val dismiss: AlarmDismiss? = null,
    val silenceAt: Long? = null,
    val seed: Long = 0L,
    val emergency: Boolean = false,
) {

    companion object {

        /**
         * The view of [ring] with [queued] alarms waiting, the speaking `occurrence` [speaking] and whether a snooze
         * can be scheduled at all ([snoozeSchedulable]: the exact alarm permission, T65), [emergency] for the emergency
         * dismiss setting. The button shows only while the user may snooze the ring ([RingCoordinator.Ring.userCanSnooze]:
         * `allowSnooze: false` of a challenge refuses it, T285) and the snooze can be scheduled.
         * zh-CN: [ring] 的显示内容, 排队 [queued] 个闹钟, 正在朗读的 `occurrence` 为 [speaking], [snoozeSchedulable] 表示能否安排贪睡 (精确闹钟权限,
         * T65), [emergency] 为紧急关闭设置. 只有用户可以贪睡本次响铃 ([RingCoordinator.Ring.userCanSnooze]: 挑战的 `allowSnooze: false` 会拒绝, T285)
         * 且能安排贪睡时才显示按钮.
         */
        fun of(ring: RingCoordinator.Ring, queued: Int, speaking: String?, snoozeSchedulable: Boolean, emergency: Boolean = false): RingView {
            val settings = ring.settings
            val snoozed = ring.request.snoozeCount
            return RingView(
                alarmId = ring.alarmId,
                occurrence = ring.occurrence,
                label = ring.label?.takeIf { it.isNotBlank() },
                snoozeMinutes = settings.snoozeMinutes.takeIf { ring.userCanSnooze && snoozeSchedulable },
                snoozed = snoozed,
                snoozesLeft = if (settings.snoozeEnabled) (settings.snoozeMax - snoozed).coerceAtLeast(0) else null,
                speaking = speaking != null && speaking == ring.occurrence,
                queued = queued.coerceAtLeast(0),
                timer = ring.isTimer,
                challenge = ring.challenge,
                dismiss = settings.dismiss,
                silenceAt = ring.silenceAt,
                seed = seedOf(ring.occurrence, ring.startedAt),
                emergency = emergency,
            )
        }

        /**
         * The seed of a round's problems and sentence: the same while the round sounds (a recreated screen shows the same
         * problems), new for the next round. zh-CN: 一轮题目与句子的种子: 本轮发声期间不变 (界面重建后题目相同), 下一轮重新生成.
         */
        fun seedOf(occurrence: String, startedAt: Long?): Long = occurrence.hashCode().toLong() * 1_000_003L + (startedAt ?: 0L)
    }
}
