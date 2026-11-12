package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.timers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.ChimeDialog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.ChimeScreen
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.DialogDefaults
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.EmptyAction
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.EmptyState
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.SectionHeader
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.TopBarAction
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.format.TimeFormats
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.PermissionBanner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions.PermissionRow
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions.Permissions
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions.PermissionsActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.settings.SettingsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.plugin.alarm.api.AlarmErrorCodes
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.TimerSpec

/**
 * The timers page (roadmap P7.4, T136, T137): one card per timer (at most 20) with the time left, its end or state, a
 * progress bar and the buttons of [TimerCard.actions]; the presets start a timer with one tap and the add button opens
 * the custom duration dialog (also opened by a `SET_TIMER` request without a length, roadmap P7.5). Changes run off the
 * main thread through the engine like the notification buttons (source `user`); a ringing timer stops through the ring
 * coordinator (T114). The page reloads on resume and on timer and ring changes, and ticks while a timer runs.
 * zh-CN: 计时器页 (路线图 P7.4, T136, T137): 每个计时器一张卡片 (至多 20 个), 显示剩余时间, 到点时刻或状态, 进度条与 [TimerCard.actions] 的按钮; 预设一点即
 * 启动计时器, 新建按钮打开自定义时长对话框 (不带时长的 `SET_TIMER` 请求也会打开它, 路线图 P7.5). 变更不在主线程, 与通知按钮一样经引擎执行 (来源 `user`);
 * 响铃中的计时器经响铃协调器停止 (T114). 页面在恢复以及计时器与响铃变化时重新加载, 有计时器运行时逐秒刷新.
 */
@Composable
internal fun TimersScreen(bottomBar: @Composable () -> Unit, customRequested: Boolean, onCustomTimerShown: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var records by remember { mutableStateOf<List<TimerRecord>?>(null) }
    var missing by remember { mutableStateOf(emptyList<PermissionRow>()) }
    var generation by remember { mutableIntStateOf(0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var custom by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(customRequested) {
        if (customRequested) {
            custom = true
            onCustomTimerShown()
        }
    }
    LaunchedEffect(generation) {
        val (loaded, lacking) = withContext(Dispatchers.IO) { AlarmRuntime.get(context).timers.list() to Permissions.read(context).banner }
        records = loaded
        missing = lacking
        now = System.currentTimeMillis()
    }
    TimerReloads { generation++ }
    Ticker(active = records.orEmpty().any { it.running || it.snoozeUntil != null }, periodMillis = TICK_MILLIS) { now = System.currentTimeMillis() }

    val failed = stringResource(R.string.home_action_failed)
    val limit = pluralStringResource(R.plurals.timers_limit, AlarmLimits.MAX_TIMERS, AlarmLimits.MAX_TIMERS)
    val exact = stringResource(R.string.timers_exact_required)
    fun act(block: (AlarmRuntime) -> Unit) = scope.launch {
        val error = withContext(Dispatchers.IO) {
            try {
                block(AlarmRuntime.get(context))
                null
            } catch (e: AlarmException) {
                Log.w(TAG, "action failed: ${e.message}")
                when (e.code) {
                    AlarmErrorCodes.LIMIT_EXCEEDED -> limit
                    AlarmErrorCodes.PERMISSION_REQUIRED -> exact
                    else -> failed
                }
            }
        }
        generation++
        if (error != null) snackbar.showSnackbar(error)
    }
    val start = { millis: Long, label: String? -> act { it.userTimerApi.start(millis, TimerSpec(label = label)) } }

    val newTimer = stringResource(R.string.timers_new)
    ChimeScreen(
        title = stringResource(R.string.home_tab_timers),
        snackbarHostState = snackbar,
        bottomBar = bottomBar,
        floatingActionButton = {
            if (records != null) {
                FloatingActionButton(onClick = { custom = true }, modifier = Modifier.testTag("chime.timers.add")) {
                    Icon(painterResource(R.drawable.ic_add_24), contentDescription = newTimer)
                }
            }
        },
        actions = {
            TopBarAction(R.drawable.ic_settings_24, stringResource(R.string.settings_title), "chime.timers.settings") {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }
        },
    ) { padding ->
        val list = records
        if (list == null) {
            Box(Modifier.padding(padding).fillMaxSize())
        } else {
            val cards = TimerCard.cardsOf(list, now)
            val full = list.size >= AlarmLimits.MAX_TIMERS
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("chime.timers.list"),
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 88.dp),
            ) {
                if (missing.isNotEmpty()) {
                    item(key = "permissionBanner") {
                        PermissionBanner(missing, { context.startActivity(Intent(context, PermissionsActivity::class.java)) }, tag = "chime.timers.permissionBanner")
                    }
                }
                item(key = "presets") { Presets(enabled = !full, onStart = { start(it, null) }) }
                if (cards.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            title = stringResource(R.string.timers_empty_title),
                            body = stringResource(R.string.timers_empty_body),
                            icon = R.drawable.ic_hourglass_24,
                            primary = EmptyAction(newTimer, { custom = true }, "chime.timers.emptyAdd"),
                        )
                    }
                }
                items(cards, key = { it.id }) { card ->
                    TimerCardView(card, now) { action ->
                        act { runtime ->
                            when (action) {
                                TimerAction.PAUSE -> runtime.timers.pause(card.id)
                                TimerAction.RESUME, TimerAction.START -> runtime.timers.resume(card.id)
                                TimerAction.RESET -> runtime.timers.reset(card.id)
                                TimerAction.ADD_MINUTE -> runtime.timers.addTime(card.id, TimerCard.ADD_MINUTE_MILLIS)
                                TimerAction.STOP, TimerAction.DELETE -> runtime.userTimerApi.cancel(card.id)
                            }
                        }
                    }
                }
            }
        }
    }

    if (custom) {
        CustomTimerDialog(
            onDismiss = { custom = false },
            onStart = { millis, label ->
                custom = false
                start(millis, label)
            },
        )
    }
}

@Composable
private fun Presets(enabled: Boolean, onStart: (Long) -> Unit) {
    val context = LocalContext.current
    Column {
        SectionHeader(stringResource(R.string.timers_presets))
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TimerPresets.MILLIS.forEach { millis ->
                SuggestionChip(
                    onClick = { onStart(millis) },
                    label = { Text(TimeFormats.duration(context, millis)) },
                    enabled = enabled,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("chime.timers.preset.${millis / 1000}"),
                )
            }
        }
    }
}

@Composable
private fun TimerCardView(card: TimerCard, now: Long, onAction: (TimerAction) -> Unit) {
    val context = LocalContext.current
    val tag = "chime.timers.timer.${card.id}"
    val accent = if (card.ringing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).testTag(tag),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text(
                        card.label ?: stringResource(R.string.timer_default_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        modifier = Modifier.testTag("$tag.title"),
                    )
                    Text(
                        TimeFormats.duration(context, card.durationMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("$tag.duration"),
                    )
                }
                if (card.deletable) {
                    IconButton(onClick = { onAction(TimerAction.DELETE) }, modifier = Modifier.testTag("$tag.delete")) {
                        Icon(painterResource(R.drawable.ic_delete_24), contentDescription = stringResource(R.string.home_delete))
                    }
                }
            }
            Column(Modifier.semantics(mergeDescendants = true) {}) {
                Text(
                    card.clockText,
                    fontSize = 44.sp,
                    lineHeight = 52.sp,
                    color = if (card.ringing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("$tag.time"),
                )
                Text(
                    statusOf(context, card, now),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (card.ringing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("$tag.status"),
                )
            }
            LinearProgressIndicator(
                progress = { card.progress },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, end = 12.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                drawStopIndicator = {},
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val actions = card.actions
                actions.forEachIndexed { index, action ->
                    val text = stringResource(labelOf(action))
                    val modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("$tag.${action.name.lowercase()}")
                    if (index == actions.lastIndex) {
                        Button(onClick = { onAction(action) }, modifier = modifier) { Text(text) }
                    } else {
                        TextButton(onClick = { onAction(action) }, enabled = action != TimerAction.ADD_MINUTE || card.canAddMinute, modifier = modifier) { Text(text) }
                    }
                }
            }
        }
    }
}

private fun labelOf(action: TimerAction): Int = when (action) {
    TimerAction.RESET -> R.string.timer_action_reset
    TimerAction.ADD_MINUTE -> R.string.timer_action_add_minute
    TimerAction.PAUSE -> R.string.timer_action_pause
    TimerAction.RESUME -> R.string.timer_action_resume
    TimerAction.START -> R.string.timer_action_start
    TimerAction.STOP -> R.string.timer_action_stop
    TimerAction.DELETE -> R.string.home_delete
}

/** The line under the time: the end, paused, ready, time's up or snoozed until. zh-CN: 时间下方的一行: 到点时刻, 已暂停, 就绪, 时间到或贪睡至. */
internal fun statusOf(context: Context, card: TimerCard, now: Long): String = when (card.phase) {
    TimerPhase.RUNNING -> context.getString(R.string.timer_notification_ends, TimeFormats.timeOf(context, card.endsAt ?: now))
    TimerPhase.PAUSED -> context.getString(R.string.timer_state_paused)
    TimerPhase.READY -> context.getString(R.string.timer_state_ready)
    TimerPhase.RINGING -> context.getString(R.string.timer_state_ringing)
    TimerPhase.SNOOZED -> context.getString(R.string.timer_state_snoozed, TimeFormats.timeOf(context, card.snoozeUntil ?: now))
}

/**
 * The custom duration dialog (T137): hours, minutes and seconds (digits only) and an optional label; start stays disabled
 * until the total lies within 1 s - 100 h. The fields survive recreation.
 * zh-CN: 自定义时长对话框 (T137): 小时, 分钟与秒 (只接受数字) 以及可选的标签; 总时长在 1 秒 - 100 小时内之前开始按钮不可用. 字段在重建后保留.
 */
@Composable
private fun CustomTimerDialog(onDismiss: () -> Unit, onStart: (Long, String?) -> Unit) {
    var hours by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("") }
    var seconds by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }
    val millis = TimerInput.millisOf(hours, minutes, seconds)
    val tag = "chime.timers.customDialog"
    ChimeDialog(
        title = stringResource(R.string.timers_new),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.timer_action_start),
        onConfirm = { if (millis != null) onStart(millis, label.trim().takeIf { it.isNotEmpty() }) },
        dismissText = stringResource(R.string.ui_dialog_cancel),
        confirmEnabled = millis != null,
        tag = tag,
    ) {
        Row(Modifier.padding(horizontal = DialogDefaults.contentPadding), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(hours, { hours = TimerInput.digits(it) }, stringResource(R.string.timers_hours), "$tag.hours", Modifier.weight(1f))
            NumberField(minutes, { minutes = TimerInput.digits(it) }, stringResource(R.string.timers_minutes), "$tag.minutes", Modifier.weight(1f))
            NumberField(seconds, { seconds = TimerInput.digits(it) }, stringResource(R.string.timers_seconds), "$tag.seconds", Modifier.weight(1f))
        }
        Text(
            stringResource(R.string.timers_range_hint),
            style = MaterialTheme.typography.bodySmall,
            color = if (millis == null && (hours + minutes + seconds).isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = DialogDefaults.contentPadding).padding(top = 4.dp).testTag("$tag.hint"),
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = label,
            onValueChange = { label = it.take(AlarmLimits.MAX_LABEL_CHARS) },
            label = { Text(stringResource(R.string.timers_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = DialogDefaults.contentPadding).testTag("$tag.label"),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, tag: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1) },
        placeholder = { Text("0") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.testTag(tag),
    )
}

/**
 * Reloads on resume and on timer and ring changes (the listeners run off the main thread).
 * zh-CN: 在恢复以及计时器与响铃变化时重新加载 (监听器不在主线程运行).
 */
@Composable
private fun TimerReloads(reload: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val current by rememberUpdatedState(reload)
    DisposableEffect(lifecycle) {
        val runtime = AlarmRuntime.get(context)
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        val changed: (String, TimerRecord?) -> Unit = { _, _ -> main.post { current() } }
        val rang: () -> Unit = { main.post { current() } }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) current() }
        runtime.timers.addListener(changed)
        runtime.rings.addListener(rang)
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            runtime.rings.removeListener(rang)
            runtime.timers.removeListener(changed)
        }
    }
}

/**
 * Calls [onTick] every [periodMillis] while [active] and the screen is started (the stopwatch page uses it too).
 * zh-CN: 在 [active] 且界面处于启动状态时, 每 [periodMillis] 调用一次 [onTick] (秒表页也使用).
 */
@Composable
internal fun Ticker(active: Boolean, periodMillis: Long, onTick: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val tick by rememberUpdatedState(onTick)
    LaunchedEffect(active, lifecycle, periodMillis) {
        if (!active) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                tick()
                delay(periodMillis)
            }
        }
    }
}

private const val TICK_MILLIS = 250L
private const val TAG = "ChimeTimers"
