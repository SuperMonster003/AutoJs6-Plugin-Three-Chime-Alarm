package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge

/**
 * The emergency dismiss of a challenge (roadmap P9.3, T283): [required] volume key presses (up or down) within
 * [windowMillis] dismiss the alarm at once, so a challenge the user cannot finish always has a way out besides the
 * auto silence.
 * zh-CN: 挑战的紧急关闭 (路线图 P9.3, T283): [windowMillis] 内按 [required] 次音量键 (加减都算) 立即关闭闹钟, 用户完成不了挑战时除自动停止外总有出路.
 */
class EmergencyPresses(val required: Int = REQUIRED, val windowMillis: Long = WINDOW_MILLIS) {

    private val presses = ArrayDeque<Long>()

    /** The presses inside the window so far. zh-CN: 窗口内已按的次数. */
    val count: Int get() = presses.size

    /**
     * Takes one press at [timeMillis] (any monotonic clock); answers true on the press that completes the sequence,
     * which starts a new one.
     * zh-CN: 接收 [timeMillis] (任意单调时钟) 的一次按键; 凑齐次数的那一次返回 true, 并重新开始计数.
     */
    fun press(timeMillis: Long): Boolean {
        while (presses.isNotEmpty() && timeMillis - presses.first() >= windowMillis) presses.removeFirst()
        presses.addLast(timeMillis)
        if (presses.size < required) return false
        presses.clear()
        return true
    }

    companion object {
        const val REQUIRED = 5
        const val WINDOW_MILLIS = 4_000L
    }
}
