package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.stopwatch

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.ChimeScreen
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.TopBarAction
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.settings.SettingsActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.timers.Ticker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.StopwatchLap
import org.autojs.plugin.alarm.api.StopwatchSnapshot

/**
 * The stopwatch page (roadmap P7.4, T138): the reading with hundredths, the buttons of the state (start; lap and pause;
 * reset and resume) and the laps newest first with the fastest and the slowest marked. The single stopwatch is the one
 * scripts control (`alarms.stopwatch`), so the page follows its changes; the reading moves on its own between them.
 * zh-CN: 秒表页 (路线图 P7.4, T138): 带百分之一秒的读数, 当前状态的按钮 (开始; 计次与暂停; 重置与继续), 以及从新到旧排列并标出最快与最慢的圈.
 * 唯一的秒表也是脚本控制的那一个 (`alarms.stopwatch`), 页面跟随它的变化; 两次变化之间读数自行走动.
 */
@Composable
internal fun StopwatchScreen(bottomBar: @Composable () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var reading by remember { mutableStateOf<Reading?>(null) }
    var generation by remember { mutableIntStateOf(0) }
    var clock by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(generation) {
        reading = withContext(Dispatchers.IO) { Reading(AlarmRuntime.get(context).stopwatch.state(), SystemClock.elapsedRealtime()) }
        clock = SystemClock.elapsedRealtime()
    }
    StopwatchReloads { generation++ }
    val running = reading?.snapshot?.state == AlarmContract.STOPWATCH_RUNNING
    Ticker(active = running, periodMillis = FRAME_MILLIS) { clock = SystemClock.elapsedRealtime() }

    val failed = stringResource(R.string.home_action_failed)
    val limit = pluralStringResource(R.plurals.stopwatch_lap_limit, AlarmLimits.MAX_STOPWATCH_LAPS, AlarmLimits.MAX_STOPWATCH_LAPS)
    fun act(button: StopwatchButton) = scope.launch {
        val error = withContext(Dispatchers.IO) {
            val stopwatch = AlarmRuntime.get(context).stopwatch
            try {
                when (button) {
                    StopwatchButton.START -> stopwatch.start()
                    StopwatchButton.LAP -> stopwatch.lap()
                    StopwatchButton.PAUSE -> stopwatch.pause()
                    StopwatchButton.RESUME -> stopwatch.resume()
                    StopwatchButton.RESET -> stopwatch.reset()
                }
                null
            } catch (e: AlarmException) {
                Log.w(TAG, "action failed: ${e.message}")
                if (e.code == org.autojs.plugin.alarm.api.AlarmErrorCodes.LIMIT_EXCEEDED) limit else failed
            }
        }
        generation++
        if (error != null) snackbar.showSnackbar(error)
    }

    ChimeScreen(
        title = stringResource(R.string.stopwatch_title),
        snackbarHostState = snackbar,
        bottomBar = bottomBar,
        actions = {
            TopBarAction(R.drawable.ic_settings_24, stringResource(R.string.settings_title), "chime.stopwatch.settings") {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }
        },
    ) { padding ->
        val current = reading
        if (current == null) {
            Box(Modifier.padding(padding).fillMaxSize())
        } else {
            StopwatchContent(current, clock, padding, limit) { act(it) }
        }
    }
}

@Composable
private fun StopwatchContent(current: Reading, clock: Long, padding: PaddingValues, limit: String, act: (StopwatchButton) -> Unit) {
    val snapshot = current.snapshot
    val elapsed = current.elapsedAt(clock)
    val marks = StopwatchView.marks(snapshot.laps)
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("chime.stopwatch.list"),
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "reading") {
            Column(Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    StopwatchView.text(elapsed),
                    fontSize = 56.sp,
                    lineHeight = 64.sp,
                    style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("chime.stopwatch.time"),
                )
                val lapStart = snapshot.laps.lastOrNull()?.totalMillis
                if (lapStart != null) {
                    Text(
                        stringResource(R.string.stopwatch_lap_number, snapshot.laps.size + 1) + "  " + StopwatchView.text(elapsed - lapStart),
                        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp).testTag("chime.stopwatch.currentLap"),
                    )
                }
            }
        }
        item(key = "buttons") {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StopwatchView.buttonsOf(snapshot.state).forEach { button ->
                    val modifier = Modifier.widthIn(min = 120.dp).defaultMinSize(minHeight = 48.dp).testTag("chime.stopwatch.${button.name.lowercase()}")
                    val text = stringResource(labelOf(button))
                    when (button) {
                        StopwatchButton.LAP, StopwatchButton.RESET ->
                            FilledTonalButton(onClick = { act(button) }, enabled = button != StopwatchButton.LAP || StopwatchView.canLap(snapshot), modifier = modifier) { Text(text) }
                        else -> Button(onClick = { act(button) }, modifier = modifier) { Text(text) }
                    }
                }
            }
        }
        if (StopwatchView.lapLimitReached(snapshot)) {
            item(key = "limit") {
                Text(
                    limit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp).padding(top = 12.dp).testTag("chime.stopwatch.lapLimit"),
                )
            }
        }
        if (snapshot.laps.isNotEmpty()) {
            item(key = "lapsTop") { Hairline(Modifier.padding(top = 24.dp)) }
        }
        items(StopwatchView.listed(snapshot.laps), key = { it.index }) { lap ->
            LapRow(lap, fastest = lap.index == marks.fastest, slowest = lap.index == marks.slowest)
        }
    }
}

@Composable
private fun LapRow(lap: StopwatchLap, fastest: Boolean, slowest: Boolean) {
    val tag = "chime.stopwatch.lap.${lap.index}"
    val digits = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
    Column(Modifier.fillMaxWidth().testTag(tag)) {
        Row(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.stopwatch_lap_number, lap.index), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                val mark = when {
                    fastest -> stringResource(R.string.stopwatch_fastest)
                    slowest -> stringResource(R.string.stopwatch_slowest)
                    else -> null
                }
                if (mark != null) {
                    Text(
                        mark,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (fastest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(if (fastest) "$tag.fastest" else "$tag.slowest"),
                    )
                }
            }
            Text(StopwatchView.text(lap.lapMillis), style = digits, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.testTag("$tag.lap"))
            Text(
                StopwatchView.text(lap.totalMillis),
                style = digits,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 112.dp).testTag("$tag.total"),
            )
        }
        Hairline()
    }
}

private fun labelOf(button: StopwatchButton): Int = when (button) {
    StopwatchButton.START -> R.string.timer_action_start
    StopwatchButton.LAP -> R.string.stopwatch_action_lap
    StopwatchButton.PAUSE -> R.string.timer_action_pause
    StopwatchButton.RESUME -> R.string.timer_action_resume
    StopwatchButton.RESET -> R.string.stopwatch_action_reset
}

/**
 * A snapshot and the boot clock when it was read, so the reading of a running stopwatch moves on between changes.
 * zh-CN: 快照及读取它时的开机时钟, 使运行中秒表的读数在两次变化之间继续走动.
 */
internal data class Reading(val snapshot: StopwatchSnapshot, val readAt: Long) {
    fun elapsedAt(clock: Long): Long =
        if (snapshot.state == AlarmContract.STOPWATCH_RUNNING) snapshot.elapsedMillis + (clock - readAt).coerceAtLeast(0L) else snapshot.elapsedMillis
}

/**
 * Reloads on resume and on stopwatch changes (scripts and the notification change it too).
 * zh-CN: 在恢复以及秒表变化时重新加载 (脚本与通知也会改变它).
 */
@Composable
private fun StopwatchReloads(reload: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val current by rememberUpdatedState(reload)
    DisposableEffect(lifecycle) {
        val stopwatch = AlarmRuntime.get(context).stopwatch
        val main = Handler(Looper.getMainLooper())
        val changed: (StopwatchSnapshot) -> Unit = { main.post { current() } }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) current() }
        stopwatch.addListener(changed)
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            stopwatch.removeListener(changed)
        }
    }
}

private const val FRAME_MILLIS = 33L
private const val TAG = "ChimeStopwatch"
