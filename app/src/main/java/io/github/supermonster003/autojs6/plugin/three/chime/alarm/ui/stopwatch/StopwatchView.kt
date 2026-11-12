package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.stopwatch

import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.StopwatchLap
import org.autojs.plugin.alarm.api.StopwatchSnapshot
import java.util.Locale

/** The buttons of the stopwatch page. zh-CN: 秒表页的按钮. */
internal enum class StopwatchButton { START, LAP, PAUSE, RESUME, RESET }

/**
 * What the stopwatch page shows for a [StopwatchSnapshot] (roadmap P7.4, T138), kept apart from Compose so that it is
 * testable on the JVM: the reading, the buttons of the state, whether a lap may still be taken (at most 100, T120), and
 * the laps newest first with the fastest and the slowest one marked.
 * zh-CN: 秒表页对 [StopwatchSnapshot] 的显示 (路线图 P7.4, T138), 与 Compose 分开以便在 JVM 上测试: 读数, 当前状态的按钮, 是否还能计次 (至多 100 圈,
 * T120), 以及按从新到旧排列并标出最快与最慢的圈.
 */
internal object StopwatchView {

    /**
     * [millis] with hundredths, cut (not rounded) so the reading never runs ahead: "0:05.12", "12:34.56", "1:02:03.45".
     * zh-CN: 带百分之一秒的 [millis], 截断而非四舍五入, 读数不会超前: "0:05.12", "12:34.56", "1:02:03.45".
     */
    fun text(millis: Long): String {
        val value = millis.coerceAtLeast(0L)
        val hundredths = value / 10 % 100
        val seconds = value / 1000
        val hours = seconds / 3600
        val minutes = seconds / 60 % 60
        val rest = seconds % 60
        return if (hours > 0) "%d:%02d:%02d.%02d".format(Locale.ROOT, hours, minutes, rest, hundredths)
        else "%d:%02d.%02d".format(Locale.ROOT, minutes, rest, hundredths)
    }

    fun buttonsOf(state: String): List<StopwatchButton> = when (state) {
        AlarmContract.STOPWATCH_RUNNING -> listOf(StopwatchButton.LAP, StopwatchButton.PAUSE)
        AlarmContract.STOPWATCH_PAUSED -> listOf(StopwatchButton.RESET, StopwatchButton.RESUME)
        else -> listOf(StopwatchButton.START)
    }

    /** A lap is taken only while running and below the limit (T120). zh-CN: 只有运行中且未达上限时才能计次 (T120). */
    fun canLap(snapshot: StopwatchSnapshot): Boolean =
        snapshot.state == AlarmContract.STOPWATCH_RUNNING && snapshot.laps.size < AlarmLimits.MAX_STOPWATCH_LAPS

    fun lapLimitReached(snapshot: StopwatchSnapshot): Boolean = snapshot.laps.size >= AlarmLimits.MAX_STOPWATCH_LAPS

    /**
     * The indexes of the fastest and the slowest lap (T138): only from two laps on and only while they differ; a tie goes
     * to the earlier lap.
     * zh-CN: 最快与最慢圈的序号 (T138): 至少两圈且两者不同才标出; 相同时取较早的圈.
     */
    fun marks(laps: List<StopwatchLap>): Marks {
        if (laps.size < 2) return Marks(null, null)
        val fastest = laps.minBy { it.lapMillis }
        val slowest = laps.maxBy { it.lapMillis }
        if (fastest.lapMillis == slowest.lapMillis) return Marks(null, null)
        return Marks(fastest.index, slowest.index)
    }

    /** The laps newest first, as listed. zh-CN: 按列表显示顺序 (从新到旧) 排列的圈. */
    fun listed(laps: List<StopwatchLap>): List<StopwatchLap> = laps.sortedByDescending { it.index }

    data class Marks(val fastest: Int?, val slowest: Int?)
}
