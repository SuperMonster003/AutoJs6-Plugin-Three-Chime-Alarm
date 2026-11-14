package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge

import kotlin.math.sqrt

/**
 * Counts the shakes of the `shake` dismiss challenge (roadmap P9.1, T281) from accelerometer samples (m/s², gravity
 * included): a shake is counted when the acceleration rises to [triggerG] or more, at least [minIntervalMillis] after
 * the previous count, and the next one needs the acceleration to fall below [rearmG] first (a phone at rest reads 1 g),
 * so holding the phone still or tilting it never counts and one hard jerk counts once.
 * zh-CN: 由加速度计采样 (m/s², 含重力) 计算关闭挑战 `shake` 的摇动次数 (路线图 P9.1, T281): 加速度升到 [triggerG] 及以上, 且距上次计数至少
 * [minIntervalMillis] 时计一次; 下一次要先降到 [rearmG] 以下 (静止的手机读数为 1 g), 因此拿稳或倾斜手机不会计数, 一次猛甩只计一次.
 */
class ShakeCounter(
    val target: Int,
    private val triggerG: Double = 2.0,
    private val rearmG: Double = 1.5,
    private val minIntervalMillis: Long = 300L,
    count: Int = 0,
) {

    /** The shakes counted so far, at most [target]. zh-CN: 已计的摇动次数, 最多 [target]. */
    var count: Int = count.coerceIn(0, target)
        private set

    val done: Boolean get() = count >= target

    private var armed = true

    private var last = Long.MIN_VALUE / 2

    /**
     * Takes one sample at [timeMillis] (any monotonic clock); answers whether it counted a shake.
     * zh-CN: 接收 [timeMillis] (任意单调时钟) 的一个采样; 返回是否计了一次摇动.
     */
    fun sample(x: Float, y: Float, z: Float, timeMillis: Long): Boolean {
        val g = sqrt((x * x + y * y + z * z).toDouble()) / STANDARD_GRAVITY
        if (g < rearmG) {
            armed = true
            return false
        }
        if (done || !armed || g < triggerG || timeMillis - last < minIntervalMillis) return false
        armed = false
        last = timeMillis
        count++
        return true
    }

    companion object {
        /** m/s² per g. zh-CN: 每 g 的 m/s². */
        const val STANDARD_GRAVITY = 9.80665
    }
}
