package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmValues
import org.autojs.plugin.alarm.api.AlarmValues.putIfPresent
import org.autojs.plugin.alarm.api.StopwatchLap
import org.autojs.plugin.alarm.api.StopwatchSnapshot
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The single stopwatch (roadmap P7.2, T118 - T120): `idle`, `running` or `paused`, counted on the elapsed-realtime clock
 * inside one boot so a clock change does not move it, with at most 100 laps (oldest first). It is never scheduled.
 *
 * After a reboot a running stopwatch reads as paused (T120, Q28): the running segment counts up to the moment the
 * device booted again (wall clock), at least up to the last moment the plugin saw it running (its start or last lap),
 * so the short time the device was off counts and a clock change before the reboot never makes it go back.
 *
 * zh-CN: 唯一的秒表 (路线图 P7.2, T118 - T120): 状态为 `idle` / `running` / `paused`, 同一次开机内按开机时长时钟计时, 改系统时间不影响, 至多 100 圈
 * (从早到晚). 秒表不排程. 重启后运行中的秒表转为暂停 (T120, Q28): 运行段计到设备再次开机的时刻 (墙钟), 至少计到插件最后一次看到它运行的时刻 (开始或
 * 最近一圈), 关机的短暂时间计入, 重启前改系统时间也不会让它倒退.
 */
class Stopwatch(
    private val file: JsonDocumentFile,
    private val clock: () -> Long = System::currentTimeMillis,
    private val elapsed: () -> Long = { System.nanoTime() / 1_000_000L },
    private val boot: () -> Int? = { null },
) {

    /**
     * The stored state: [accumulated] milliseconds before the running segment, which started at [startElapsed] (and at
     * the wall-clock [startWall]) in boot [boot]; [seenElapsed] is the last moment of that segment the plugin saw.
     * zh-CN: 存储的状态: 运行段之前累计的 [accumulated] 毫秒; 运行段于第 [boot] 次开机的 [startElapsed] (墙钟 [startWall]) 开始, [seenElapsed] 为
     * 插件最后看到的该段时刻.
     */
    private data class State(
        val state: String = AlarmContract.STOPWATCH_IDLE,
        val accumulated: Long = 0L,
        val startElapsed: Long? = null,
        val startWall: Long? = null,
        val seenElapsed: Long? = null,
        val boot: Int? = null,
        val laps: List<StopwatchLap> = emptyList(),
    ) {
        val running: Boolean get() = state == AlarmContract.STOPWATCH_RUNNING
    }

    private val lock = Any()
    private var current: State? = null
    private val listeners = CopyOnWriteArrayList<(StopwatchSnapshot) -> Unit>()

    /** Called with the new snapshot after every change of state or laps. zh-CN: 状态或圈数每次变化后以新快照调用. */
    fun addListener(listener: (StopwatchSnapshot) -> Unit) {
        listeners += listener
    }

    /** Removes a listener of [addListener] (the stopwatch page, roadmap P7.4). zh-CN: 移除 [addListener] 的监听器 (秒表页, 路线图 P7.4). */
    fun removeListener(listener: (StopwatchSnapshot) -> Unit) {
        listeners -= listener
    }

    fun state(): StopwatchSnapshot = update { it }

    /** Starts from idle, or continues a paused stopwatch (T119). zh-CN: 从 idle 开始, 或继续已暂停的秒表 (T119). */
    fun start(): StopwatchSnapshot = update { state ->
        if (state.running) {
            state
        } else {
            val now = elapsed()
            val base = if (state.state == AlarmContract.STOPWATCH_IDLE) State() else state
            State(AlarmContract.STOPWATCH_RUNNING, base.accumulated, now, clock(), now, boot(), base.laps)
        }
    }

    fun resume(): StopwatchSnapshot = start()

    fun pause(): StopwatchSnapshot = update { state ->
        if (state.running) State(AlarmContract.STOPWATCH_PAUSED, totalAt(state, elapsed()), laps = state.laps) else state
    }

    /** Records a lap of the running stopwatch; at most 100. zh-CN: 为运行中的秒表记一圈, 至多 100 圈. */
    fun lap(): Pair<StopwatchSnapshot, StopwatchLap> {
        var lap: StopwatchLap? = null
        val snapshot = update { state ->
            if (!state.running) throw AlarmException.invalidOptions("lap needs a running stopwatch, it is ${state.state}")
            if (state.laps.size >= AlarmLimits.MAX_STOPWATCH_LAPS) {
                throw AlarmException.limitExceeded("at most ${AlarmLimits.MAX_STOPWATCH_LAPS} laps")
            }
            val now = elapsed()
            val total = totalAt(state, now)
            val next = StopwatchLap(state.laps.size + 1, total - (state.laps.lastOrNull()?.totalMillis ?: 0L), total)
            lap = next
            state.copy(seenElapsed = now, laps = state.laps + next)
        }
        return snapshot to lap!!
    }

    fun reset(): StopwatchSnapshot = update { State() }

    /** The running segment counted up to [now] on the elapsed-realtime clock. zh-CN: 运行段按开机时长时钟计到 [now]. */
    private fun totalAt(state: State, now: Long): Long {
        val start = state.startElapsed ?: return state.accumulated
        return state.accumulated + (now - start).coerceAtLeast(0L)
    }

    /**
     * Loads the state, settles a reboot (a running stopwatch of an earlier boot becomes paused), applies [block], saves
     * when anything changed and tells the listeners.
     * zh-CN: 读取状态, 处理重启 (更早一次开机的运行中秒表转为暂停), 执行 [block], 有变化时保存并通知监听器.
     */
    private fun update(block: (State) -> State): StopwatchSnapshot {
        val (snapshot, changed) = synchronized(lock) {
            val before = loaded()
            val next = block(settled(before))
            if (next != before) {
                file.write(toValues(next))
                current = next
            }
            snapshotOf(next) to (next != before)
        }
        if (changed) listeners.forEach { listener -> runCatching { listener(snapshot) } }
        return snapshot
    }

    private fun settled(state: State): State {
        if (!state.running) return state
        val now = elapsed()
        val start = state.startElapsed ?: now
        val currentBoot = boot()
        val rebooted = if (currentBoot != null && state.boot != null) currentBoot != state.boot else now < (state.seenElapsed ?: start)
        if (!rebooted) return state
        val seen = ((state.seenElapsed ?: start) - start).coerceAtLeast(0L)
        val untilBoot = state.startWall?.let { (clock() - now) - it }?.coerceAtLeast(0L) ?: 0L
        return State(AlarmContract.STOPWATCH_PAUSED, state.accumulated + maxOf(seen, untilBoot), laps = state.laps)
    }

    private fun snapshotOf(state: State) = StopwatchSnapshot(state.state, if (state.running) totalAt(state, elapsed()) else state.accumulated, state.laps)

    private fun loaded(): State = current ?: (file.read()?.let(::fromValues) ?: State()).also { current = it }

    private fun toValues(state: State): Map<String, Any?> = LinkedHashMap<String, Any?>().apply {
        put(KEY_STATE, state.state)
        put(KEY_ACCUMULATED, state.accumulated)
        putIfPresent(KEY_START, state.startElapsed)
        putIfPresent(KEY_START_WALL, state.startWall)
        putIfPresent(KEY_SEEN, state.seenElapsed)
        putIfPresent(KEY_BOOT, state.boot)
        put(KEY_LAPS, state.laps.map { it.toValues() })
    }

    private fun fromValues(values: Map<String, Any?>): State? = runCatching {
        val state = AlarmValues.string(values, KEY_STATE)?.takeIf { it in STATES } ?: return null
        val running = state == AlarmContract.STOPWATCH_RUNNING
        val start = AlarmValues.long(values, KEY_START)
        if (running && start == null) return null
        val laps = if (state == AlarmContract.STOPWATCH_IDLE) {
            emptyList()
        } else {
            AlarmValues.mapList(values, KEY_LAPS).orEmpty().map { StopwatchLap.fromValues(it) }.take(AlarmLimits.MAX_STOPWATCH_LAPS)
        }
        State(
            state = state,
            accumulated = AlarmValues.long(values, KEY_ACCUMULATED)?.coerceAtLeast(0L) ?: 0L,
            startElapsed = start.takeIf { running },
            startWall = AlarmValues.long(values, KEY_START_WALL).takeIf { running },
            seenElapsed = AlarmValues.long(values, KEY_SEEN).takeIf { running },
            boot = AlarmValues.int(values, KEY_BOOT).takeIf { running },
            laps = laps,
        )
    }.getOrNull()

    companion object {
        const val FILE_NAME = "stopwatch.json"
        const val FORMAT = "three-chime-alarm.stopwatch"
        const val VERSION = 1
        private const val KEY_STATE = "state"
        private const val KEY_ACCUMULATED = "accumulated"
        private const val KEY_START = "startElapsed"
        private const val KEY_START_WALL = "startWall"
        private const val KEY_SEEN = "seenElapsed"
        private const val KEY_BOOT = "boot"
        private const val KEY_LAPS = "laps"
        private val STATES = setOf(AlarmContract.STOPWATCH_IDLE, AlarmContract.STOPWATCH_RUNNING, AlarmContract.STOPWATCH_PAUSED)
    }
}
