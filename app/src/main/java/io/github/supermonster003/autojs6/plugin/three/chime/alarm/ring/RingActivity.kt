package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.core.view.OneShotPreDrawListener
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge.EmergencyPresses
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingCoordinator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.format.TimeFormats
import kotlinx.coroutines.delay
import org.autojs.plugin.alarm.api.AlarmContract
import kotlin.math.roundToInt

/**
 * The ringing screen (roadmap T5, P2.4, P3.2, T86), shown over the lock screen, turning the screen on and keeping it
 * on: the current time in large type, the label, the snoozes so far and left, a speaking mark while the voice reads,
 * and "N more alarms" while others wait. Snooze is a button, dismiss a slider (an accessibility service dismisses
 * with a click on it); the back key does nothing, as in the system clock. It follows the ring state, shows the next
 * alarm of the queue when one ends, and closes when nothing sounds. Coming to the front it asks the ring for audio focus
 * again (T35). The appearance follows the plugin's other screens, from the first frame and also before the first unlock:
 * the plugin's appearance settings and the remembered host appearance live in device protected storage (T88).
 *
 * With a dismiss challenge in effect (roadmap P9, T286 - T289) the [ChallengeLayer] takes the place of the slider and
 * finishing it dismisses the round `by: 'challenge'`; the snooze button follows `allowSnooze`. The volume keys then no
 * longer change the volume (turning the alarm down would get round the challenge); with the emergency dismiss setting on,
 * five presses within four seconds dismiss the round `by: 'user'` (chime-p9-protocol.md section 5).
 *
 * zh-CN: 响铃界面 (路线图 T5, P2.4, P3.2, T86), 显示在锁屏之上, 点亮屏幕并保持常亮: 大字的当前时间, 标签, 已贪睡与剩余次数, 朗读时的 "正在朗读"
 * 标记, 有其他闹钟排队时的 "还有 N 个闹钟". 贪睡是按钮, 关闭是滑块 (无障碍服务点按滑块即关闭); 返回键不起作用, 与系统时钟一致. 它跟随响铃状态,
 * 一个闹钟结束时显示队列中的下一个, 没有闹钟发声时关闭. 到前台时让响铃再次申请音频焦点 (T35). 外观与插件其他界面一致, 首帧与首次解锁前也是如此:
 * 插件的外观设置与记住的宿主外观都在设备加密存储中 (T88).
 *
 * 有生效的关闭挑战时 (路线图 P9, T286 - T289), [ChallengeLayer] 取代滑块, 完成挑战以 `by: 'challenge'` 关闭本轮; 贪睡按钮遵循 `allowSnooze`. 此时音量键
 * 不再调节音量 (调低音量会绕过挑战); 紧急关闭设置开启时, 4 秒内按 5 次以 `by: 'user'` 关闭本轮 (chime-p9-protocol.md 第 5 节).
 */
class RingActivity : StandaloneActivity() {

    private class Shown(val ring: RingCoordinator.Ring, val queued: Int, val snoozeSchedulable: Boolean, val emergency: Boolean)

    private val main = Handler(Looper.getMainLooper())

    private lateinit var runtime: AlarmRuntime

    private var shown by mutableStateOf<Shown?>(null)

    private val listener: () -> Unit = { main.post(::render) }

    /** The volume key presses of the round [pressesOf]. zh-CN: [pressesOf] 这一轮的音量键按压. */
    private var presses = EmergencyPresses()

    private var pressesOf: String? = null

    override val normalizesLauncherIcons: Boolean get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        runtime = AlarmRuntime.get(this)
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        runtime.rings.addListener(listener)
        render()
        // The first frame of the ringing screen, for the cold start baseline of roadmap P5.6 (`adb logcat -s ChimeRing`):
        // a launch through the full screen intent logs no `Displayed` line. zh-CN: 响铃界面的首帧, 用于路线图 P5.6 的冷启动基线;
        // 经全屏 Intent 的启动不记录 `Displayed` 行.
        OneShotPreDrawListener.add(window.decorView) { Log.i(RingService.TAG, "ring screen drawn ${shown?.ring?.alarmId ?: "(none)"}") }
    }

    override fun onResume() {
        super.onResume()
        Log.i(RingService.TAG, "ring screen resumed ${shown?.ring?.alarmId ?: "(none)"}")
        RingService.focus(this)
    }

    override fun onDestroy() {
        runtime.rings.removeListener(listener)
        main.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    @Composable
    override fun Content() {
        val current = shown ?: return
        val speaking by RingIndicators.speaking.collectAsState()
        val view = RingView.of(current.ring, current.queued, speaking, current.snoozeSchedulable, current.emergency)
        // The ring goes on without the screen, so back must not hide it by accident (as in the system clock).
        // zh-CN: 离开界面响铃也不会停, 因此返回键不能误把它隐藏 (与系统时钟一致).
        BackHandler { Log.i(RingService.TAG, "back ignored on the ring screen") }
        val challenge = view.challenge
        RingScreen(
            view = view,
            onSnooze = { act("snooze", view.alarmId) { runtime.rings.snooze(it, null, AlarmContract.BY_USER) } },
            onDismiss = if (challenge == null) {
                { act("dismiss", view.alarmId) { runtime.rings.dismiss(it, AlarmContract.BY_USER) } }
            } else {
                { act("challenge $challenge solved", view.alarmId) { runtime.rings.solve(view.occurrence) } }
            },
            challenge = if (challenge == null) null else { solved -> ChallengeLayer(view, solved) },
        )
    }

    /**
     * The volume keys while a challenge is in effect: never change the volume, count toward the emergency dismiss when
     * the setting is on (auto repeats do not count). zh-CN: 挑战生效时的音量键: 不调节音量, 设置开启时计入紧急关闭 (长按的自动重复不计).
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val current = shown
        val volume = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!volume || current?.ring?.challenge == null) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 && current.emergency) {
            val occurrence = current.ring.occurrence
            if (pressesOf != occurrence) {
                pressesOf = occurrence
                presses = EmergencyPresses()
            }
            if (presses.press(event.eventTime)) act("emergency dismiss", current.ring.alarmId) { runtime.rings.emergencyDismiss(occurrence) }
        }
        return true
    }

    private fun render() {
        if (isFinishing) return
        val ring = runtime.rings.sounding ?: run {
            finish()
            return
        }
        shown = Shown(ring, runtime.rings.queued.size, runtime.schedule.canSnooze(), runtime.defaults.get().emergencyDismiss)
    }

    private fun act(what: String, alarmId: String, block: (String) -> Boolean) {
        runtime.executor.execute {
            try {
                Log.i(RingService.TAG, "$what on the ring screen: handled=${block(alarmId)}")
            } catch (e: RuntimeException) {
                Log.e(RingService.TAG, "$what failed", e)
            }
        }
    }

    internal companion object {
        const val TAG_TIME = "chime.ring.time"
        const val TAG_LABEL = "chime.ring.label"
        const val TAG_SNOOZE_INFO = "chime.ring.snoozeInfo"
        const val TAG_SPEAKING = "chime.ring.speaking"
        const val TAG_QUEUE = "chime.ring.queue"
        const val TAG_SNOOZE = "chime.ring.snooze"
        const val TAG_DISMISS = "chime.ring.dismiss"
        const val TAG_CHALLENGE = "chime.ring.challenge"
        const val TAG_CHALLENGE_PROBLEM = "chime.ring.challenge.problem"
        const val TAG_CHALLENGE_INPUT = "chime.ring.challenge.input"
        const val TAG_CHALLENGE_CHECK = "chime.ring.challenge.check"
        const val TAG_CHALLENGE_PROGRESS = "chime.ring.challenge.progress"
        const val TAG_CHALLENGE_SCRIPT = "chime.ring.challenge.script"
        const val TAG_CHALLENGE_AUTO_STOP = "chime.ring.challenge.autoStop"
        const val TAG_CHALLENGE_EMERGENCY = "chime.ring.challenge.emergency"
    }
}

/**
 * The layout of the ringing screen for [view]. [challenge] is the slot of the dismiss challenges (roadmap 1.3.0): when
 * set it takes the place of the slider and calls its `onSolved` to dismiss. The content scrolls when it does not fit,
 * for example above the keyboard of a challenge (T288).
 * zh-CN: [view] 的响铃界面布局. [challenge] 是关闭挑战的插槽 (路线图 1.3.0): 设置时取代滑块, 通过其 `onSolved` 关闭. 内容放不下时可以滚动,
 * 例如挑战的键盘弹出时 (T288).
 */
@Composable
internal fun RingScreen(
    view: RingView,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    challenge: (@Composable (onSolved: () -> Unit) -> Unit)? = null,
) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(MINUTE_MILLIS - now % MINUTE_MILLIS)
        }
    }
    // One action per round: the screen changes to the next alarm (or closes) once the ring state follows.
    // zh-CN: 每一轮只执行一次操作: 响铃状态随之改变后界面切到下一个闹钟 (或关闭).
    var acted by remember(view.occurrence) { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp), contentAlignment = Alignment.Center) {
            val compact = maxHeight < COMPACT_HEIGHT
            // widthIn before fillMax*: the content keeps a readable width on tablets in landscape. At least the screen height
            // in a scroll: the weights center the content while it fits. zh-CN: 先 widthIn 再 fillMax*: 横屏平板上内容保持易读的宽度.
            // 滚动容器内至少一屏高: 内容放得下时由权重居中.
            Column(
                Modifier.verticalScroll(rememberScrollState()).heightIn(min = maxHeight).widthIn(max = 480.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    TimeFormats.minuteOf(context, now),
                    Modifier.testTag(RingActivity.TAG_TIME),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = if (compact) 56.sp else 80.sp, lineHeight = if (compact) 64.sp else 92.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
                Text(
                    view.label ?: stringResource(if (view.timer) R.string.timer_default_label else R.string.alarm_default_label),
                    Modifier.padding(top = 8.dp).testTag(RingActivity.TAG_LABEL),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                snoozeInfo(view)?.let {
                    Text(
                        it,
                        Modifier.padding(top = 12.dp).testTag(RingActivity.TAG_SNOOZE_INFO),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                if (view.speaking) {
                    Row(Modifier.padding(top = 12.dp).testTag(RingActivity.TAG_SPEAKING), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_volume_up_24), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.ring_speaking), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (view.queued > 0) {
                    Text(
                        pluralStringResource(R.plurals.ring_queue, view.queued, view.queued),
                        Modifier.padding(top = 12.dp).testTag(RingActivity.TAG_QUEUE),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    view.snoozeMinutes?.let { minutes ->
                        FilledTonalButton(
                            onClick = {
                                if (!acted) {
                                    acted = true
                                    onSnooze()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp).testTag(RingActivity.TAG_SNOOZE),
                            enabled = !acted,
                        ) {
                            // The trailing spacer as wide as the icon and its gap centers the text itself, in line with the lines above.
                            // zh-CN: 末尾与图标及间距等宽的占位让文字本身居中, 与上方各行对齐.
                            Icon(painterResource(R.drawable.ic_snooze_24), null, Modifier.size(BUTTON_ICON))
                            Spacer(Modifier.width(BUTTON_ICON_GAP))
                            Text(
                                pluralStringResource(R.plurals.ring_snooze_minutes, minutes, minutes),
                                Modifier.weight(1f, fill = false),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.width(BUTTON_ICON + BUTTON_ICON_GAP))
                        }
                    }
                    val dismiss = {
                        if (!acted) {
                            acted = true
                            onDismiss()
                        }
                    }
                    key(view.occurrence) {
                        if (challenge != null) challenge(dismiss) else DismissSlider(enabled = !acted, onDismiss = dismiss)
                    }
                }
            }
        }
    }
}

/** "Snoozed 1 time, 2 snoozes left" and its shorter forms; null when the alarm does not snooze. zh-CN: 贪睡次数说明; 闹钟不贪睡时为 null. */
@Composable
private fun snoozeInfo(view: RingView): String? {
    val left = view.snoozesLeft ?: return null
    val remaining = if (left > 0) pluralStringResource(R.plurals.ring_snooze_left, left, left) else stringResource(R.string.ring_snooze_used_up)
    if (view.snoozed <= 0) return remaining
    return stringResource(R.string.ring_snooze_status, pluralStringResource(R.plurals.ring_snoozed_times, view.snoozed, view.snoozed), remaining)
}

/**
 * Dismisses when the thumb is dragged past three quarters of the track toward the end (right in left-to-right
 * languages, left in right-to-left ones); a shorter drag springs back. Accessibility services see one button whose
 * click dismisses.
 * zh-CN: 把滑块向末端 (从左到右的语言为右, 从右到左的语言为左) 拖过轨道的四分之三即关闭, 不足则弹回. 无障碍服务看到的是一个按钮, 点按即关闭.
 */
@Composable
private fun DismissSlider(enabled: Boolean, onDismiss: () -> Unit) {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var trackWidth by remember { mutableIntStateOf(0) }
    var drag by remember { mutableFloatStateOf(0f) }
    val inset = with(density) { SLIDER_INSET.toPx() }
    val range = (trackWidth - with(density) { SLIDER_THUMB.toPx() } - inset * 2).coerceAtLeast(0f)
    val hint = stringResource(R.string.ring_dismiss_hint)
    val label = stringResource(R.string.ring_action_dismiss)
    val state = rememberDraggableState { delta -> drag = (drag + delta).coerceIn(0f, range) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(SLIDER_THUMB + SLIDER_INSET * 2)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { trackWidth = it.width }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = hint
                if (enabled) {
                    onClick(label) {
                        onDismiss()
                        true
                    }
                }
            }
            .draggable(
                state = state,
                orientation = Orientation.Horizontal,
                enabled = enabled,
                reverseDirection = rtl,
                onDragStopped = {
                    if (range > 0f && drag >= range * DISMISS_FRACTION) {
                        drag = range
                        onDismiss()
                    } else {
                        animate(drag, 0f) { value, _ -> drag = value }
                    }
                },
            )
            .testTag(RingActivity.TAG_DISMISS),
        contentAlignment = Alignment.CenterStart,
    ) {
        val progress = if (range > 0f) drag / range else 0f
        // The same room on both sides keeps the hint centered on the track, in line with the lines above.
        // zh-CN: 两侧留出相同空间, 提示文字在轨道上居中, 与上方各行对齐.
        Text(
            hint,
            Modifier.fillMaxWidth().padding(horizontal = SLIDER_THUMB + SLIDER_INSET * 2).alpha(1f - progress),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            Modifier
                .padding(SLIDER_INSET)
                .offset { IntOffset(drag.roundToInt(), 0) }
                .size(SLIDER_THUMB)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_alarm_24), null, tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

private const val MINUTE_MILLIS = 60_000L

private const val DISMISS_FRACTION = 0.75f

private val COMPACT_HEIGHT = 480.dp

private val SLIDER_THUMB = 56.dp

private val SLIDER_INSET = 6.dp

private val BUTTON_ICON = 24.dp

private val BUTTON_ICON_GAP = 8.dp
