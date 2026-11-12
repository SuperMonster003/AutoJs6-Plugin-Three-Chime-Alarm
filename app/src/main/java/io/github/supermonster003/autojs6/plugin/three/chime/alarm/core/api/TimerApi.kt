package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmOptionsValidator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingCoordinator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.Stopwatch
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerEngine
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerRecord
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import org.autojs.plugin.alarm.api.AlarmResultKeys
import org.autojs.plugin.alarm.api.StopwatchSnapshot
import org.autojs.plugin.alarm.api.TimerSpec

/**
 * The timer and stopwatch methods of `IAlarmPlugin` on value trees (roadmap P7.3, appendix A section 6): `startTimer`,
 * `getTimer`, `listTimers`, `controlTimer` and `stopwatch`. Timers are created with [source]; a ringing timer cancelled
 * here ends through the ring coordinator with a `dismiss` event `by` [by] (T114).
 * zh-CN: 以值树实现的 `IAlarmPlugin` 计时器与秒表方法 (路线图 P7.3, 附录 A 第 6 节): `startTimer`, `getTimer`, `listTimers`, `controlTimer` 与
 * `stopwatch`. 计时器以 [source] 创建; 在这里取消响铃中的计时器时经响铃协调器结束, 发出 `by` 为 [by] 的 `dismiss` 事件 (T114).
 */
class TimerApi(
    private val timers: TimerEngine,
    private val stopwatch: Stopwatch,
    private val rings: RingCoordinator,
    private val validator: AlarmOptionsValidator = AlarmOptionsValidator(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val source: String = AlarmContract.SOURCE_SCRIPT,
    private val by: String = AlarmContract.BY_SCRIPT,
) {

    fun startTimer(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, START_KEYS)
        val duration = request.requireLong(AlarmOptionKeys.DURATION_MILLIS)
        return timerAnswer(start(duration, validator.timerOptions(request.values - AlarmOptionKeys.DURATION_MILLIS)))
    }

    /** Starts a timer of [durationMillis] (the tools call it with a parsed duration). zh-CN: 启动 [durationMillis] 的计时器 (工具以解析后的时长调用). */
    fun start(durationMillis: Long, spec: TimerSpec): TimerRecord = timers.start(durationMillis, spec, source)

    fun getTimer(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, ID_KEYS)
        val record = timers.get(request.requireString(AlarmOptionKeys.ID)) ?: return emptyMap()
        return timerAnswer(record)
    }

    fun listTimers(values: Map<String, Any?>): Map<String, Any?> {
        AlarmRequest.of(values, NO_KEYS)
        val now = clock()
        return mapOf(AlarmResultKeys.TIMERS to timers.list().map { it.toSnapshot(now).toValues() })
    }

    /**
     * `pause`, `resume`, `addTime` (with `millis`, which no other action takes), `reset` or `cancel` (answers `removed`).
     * zh-CN: `pause`, `resume`, `addTime` (带 `millis`, 其他动作不接受), `reset` 或 `cancel` (返回 `removed`).
     */
    fun controlTimer(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, CONTROL_KEYS)
        val id = request.requireString(AlarmOptionKeys.ID)
        val action = request.requireString(AlarmOptionKeys.ACTION)
        if (action !in AlarmContract.TIMER_ACTIONS) {
            throw AlarmException.invalidOptions("unknown timer action '$action', expected one of ${AlarmContract.TIMER_ACTIONS.joinToString()}")
        }
        val millis = request.long(AlarmOptionKeys.MILLIS)
        if (action == AlarmContract.TIMER_ACTION_ADD_TIME && millis == null) throw AlarmException.invalidOptions("missing option '${AlarmOptionKeys.MILLIS}'")
        if (action != AlarmContract.TIMER_ACTION_ADD_TIME && millis != null) {
            throw AlarmException.invalidOptions("'${AlarmOptionKeys.MILLIS}' only goes with the action '${AlarmContract.TIMER_ACTION_ADD_TIME}'")
        }
        return when (action) {
            AlarmContract.TIMER_ACTION_PAUSE -> timerAnswer(timers.pause(id))
            AlarmContract.TIMER_ACTION_RESUME -> timerAnswer(timers.resume(id))
            AlarmContract.TIMER_ACTION_ADD_TIME -> timerAnswer(timers.addTime(id, millis!!))
            AlarmContract.TIMER_ACTION_RESET -> timerAnswer(timers.reset(id))
            else -> mapOf(AlarmResultKeys.REMOVED to cancel(id))
        }
    }

    /**
     * Removes a timer; a ringing or snoozed one is dismissed `by` [by] first, so its round ends with a `dismiss` event
     * (T114). False for an unknown id.
     * zh-CN: 删除计时器; 响铃中或贪睡中的先以 `by` [by] 关闭, 其一轮以 `dismiss` 事件结束 (T114). id 未知时返回 false.
     */
    fun cancel(id: String): Boolean {
        val record = timers.get(id) ?: return false
        if (!record.ringing) {
            try {
                return timers.cancel(id)
            } catch (e: AlarmException) {
                // It began to ring meanwhile. zh-CN: 期间开始响铃.
                if (timers.get(id)?.ringing != true) throw e
            }
        }
        if (!rings.dismiss(id, by)) timers.end(id, silent = true)
        return true
    }

    fun stopwatch(values: Map<String, Any?>): Map<String, Any?> {
        val request = AlarmRequest.of(values, STOPWATCH_KEYS)
        val action = request.requireString(AlarmOptionKeys.ACTION)
        return when (action) {
            AlarmContract.STOPWATCH_ACTION_STATE -> stopwatchAnswer(stopwatch.state())
            AlarmContract.STOPWATCH_ACTION_START -> stopwatchAnswer(stopwatch.start())
            AlarmContract.STOPWATCH_ACTION_PAUSE -> stopwatchAnswer(stopwatch.pause())
            AlarmContract.STOPWATCH_ACTION_RESUME -> stopwatchAnswer(stopwatch.resume())
            AlarmContract.STOPWATCH_ACTION_RESET -> stopwatchAnswer(stopwatch.reset())
            AlarmContract.STOPWATCH_ACTION_LAP -> {
                val (snapshot, lap) = stopwatch.lap()
                mapOf(AlarmResultKeys.STOPWATCH to snapshot.toValues(), AlarmResultKeys.LAP to lap.toValues())
            }
            else -> throw AlarmException.invalidOptions("unknown stopwatch action '$action', expected one of ${AlarmContract.STOPWATCH_ACTIONS.joinToString()}")
        }
    }

    private fun timerAnswer(record: TimerRecord): Map<String, Any?> = mapOf(AlarmResultKeys.TIMER to record.toSnapshot(clock()).toValues())

    private fun stopwatchAnswer(snapshot: StopwatchSnapshot): Map<String, Any?> = mapOf(AlarmResultKeys.STOPWATCH to snapshot.toValues())

    private companion object {
        val NO_KEYS: Set<String> = emptySet()
        val ID_KEYS: Set<String> = setOf(AlarmOptionKeys.ID)
        val START_KEYS: Set<String> = linkedSetOf(AlarmOptionKeys.DURATION_MILLIS) + TimerSpec.KEYS
        val CONTROL_KEYS: Set<String> = setOf(AlarmOptionKeys.ID, AlarmOptionKeys.ACTION, AlarmOptionKeys.MILLIS)
        val STOPWATCH_KEYS: Set<String> = setOf(AlarmOptionKeys.ACTION)
    }
}
