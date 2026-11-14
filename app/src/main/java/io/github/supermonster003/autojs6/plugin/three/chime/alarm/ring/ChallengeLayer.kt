package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge.MathChallenge
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge.ShakeCounter
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge.TypeChallenge
import kotlinx.coroutines.delay
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmLimits

/**
 * The challenge layer of the ringing screen (roadmap P9.1, P9.3, P9.5, T286 - T288), in the slot of the dismiss slider
 * while a dismiss challenge is in effect: `math` asks the round's problems one after another, `shake` counts shakes of
 * the accelerometer with a progress bar, `type` asks for the alarm's own text or a sentence of the screen language, and
 * `script` shows that it waits for a script. Every challenge also shows the time left until the auto silence and, when
 * the setting is on, how the emergency dismiss works (the two ways out of every challenge, chime-p9-protocol.md section
 * 5). The progress of a round survives a recreated screen; the next round starts over. [onSolved] dismisses the round
 * `by: 'challenge'`.
 *
 * zh-CN: 响铃界面的挑战层 (路线图 P9.1, P9.3, P9.5, T286 - T288), 关闭挑战生效时取代关闭滑块: `math` 依次出本轮的题, `shake` 用加速度计计数并显示进度条,
 * `type` 要求输入闹钟自己的文字或界面语言的一句话, `script` 显示正在等待脚本. 每种挑战都显示距自动停止的剩余时间, 设置开启时还说明紧急关闭的用法
 * (每种挑战的两条出路, chime-p9-protocol.md 第 5 节). 一轮的进度在界面重建后保留, 下一轮从头开始. [onSolved] 以 `by: 'challenge'` 关闭本轮.
 */
@Composable
internal fun ChallengeLayer(view: RingView, onSolved: () -> Unit) {
    val challenge = view.challenge ?: return
    Column(
        Modifier.fillMaxWidth().testTag(RingActivity.TAG_CHALLENGE),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (challenge) {
            AlarmContract.CHALLENGE_MATH -> MathLayer(view, onSolved)
            AlarmContract.CHALLENGE_SHAKE -> ShakeLayer(view, onSolved)
            AlarmContract.CHALLENGE_TYPE -> TypeLayer(view, onSolved)
            else -> ScriptLayer()
        }
        AutoStop(view.silenceAt)
        if (view.emergency) {
            Text(
                stringResource(R.string.ring_challenge_emergency_hint),
                Modifier.testTag(RingActivity.TAG_CHALLENGE_EMERGENCY),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The round's problems one after another; a wrong answer clears the field and keeps the problem. zh-CN: 依次出本轮的题; 答错清空输入, 题目不变. */
@Composable
private fun MathLayer(view: RingView, onSolved: () -> Unit, difficulty: Int? = view.dismiss?.mathDifficulty, count: Int? = view.dismiss?.mathCount) {
    val round = remember(view.seed, difficulty, count) { MathChallenge(view.seed, difficulty, count) }
    var solved by rememberSaveable(view.occurrence) { mutableIntStateOf(0) }
    var input by rememberSaveable(view.occurrence) { mutableStateOf("") }
    var wrong by rememberSaveable(view.occurrence) { mutableStateOf(false) }
    val problem = round.problems[solved.coerceIn(0, round.count - 1)]
    val check = {
        val right = MathChallenge.check(problem, input)
        input = ""
        wrong = !right
        if (right) {
            if (solved + 1 >= round.count) onSolved() else solved += 1
        }
    }
    Text(stringResource(R.string.ring_challenge_math_title, solved + 1, round.count), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    // Left to right in every language: a right-to-left paragraph would show `56 - 12` as `12 - 56`.
    // zh-CN: 所有语言都从左到右: 从右到左的段落会把 `56 - 12` 显示成 `12 - 56`.
    Text(
        problem.text,
        Modifier.testTag(RingActivity.TAG_CHALLENGE_PROBLEM),
        style = MaterialTheme.typography.headlineMedium.copy(textDirection = TextDirection.Ltr),
        color = MaterialTheme.colorScheme.onBackground,
    )
    AnswerField(
        value = input,
        onValueChange = {
            input = it
            wrong = false
        },
        label = stringResource(R.string.ring_challenge_math_answer),
        error = if (wrong) stringResource(R.string.ring_challenge_math_wrong) else null,
        keyboard = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        onDone = check,
    )
}

/**
 * Counts shakes until the target; a device without an accelerometer asks the default problems instead (T287).
 * zh-CN: 计数直到目标次数; 没有加速度计的设备改为出默认的算术题 (T287).
 */
@Composable
private fun ShakeLayer(view: RingView, onSolved: () -> Unit) {
    val context = LocalContext.current
    val sensors = remember { context.getSystemService(SensorManager::class.java) }
    val sensor = remember { sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    if (sensors == null || sensor == null) {
        LaunchedEffect(view.occurrence) { Log.w(RingService.TAG, "shake challenge ${view.alarmId}: no accelerometer, math instead") }
        Text(
            stringResource(R.string.ring_challenge_shake_no_sensor),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        MathLayer(view, onSolved, difficulty = null, count = null)
        return
    }
    val target = (view.dismiss?.shakeCount ?: AlarmLimits.DEFAULT_SHAKE_COUNT).coerceIn(1, AlarmLimits.MAX_SHAKE_COUNT)
    var count by rememberSaveable(view.occurrence) { mutableIntStateOf(0) }
    val solved by rememberUpdatedState(onSolved)
    DisposableEffect(view.occurrence, target) {
        val counter = ShakeCounter(target, count = count)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (counter.done) return
                if (counter.sample(event.values[0], event.values[1], event.values[2], event.timestamp / NANOS_PER_MILLI)) {
                    count = counter.count
                    if (counter.done) solved()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensors.unregisterListener(listener) }
    }
    Text(stringResource(R.string.ring_challenge_shake_title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    LinearProgressIndicator(
        progress = { count / target.toFloat() },
        modifier = Modifier.fillMaxWidth().height(8.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        drawStopIndicator = {},
    )
    Text(
        stringResource(R.string.ring_challenge_shake_progress, count, target),
        Modifier.testTag(RingActivity.TAG_CHALLENGE_PROGRESS),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

/** The alarm's own text or a sentence of the screen language, matched ignoring case and blanks at both ends. zh-CN: 闹钟自己的文字或界面语言的一句话, 忽略大小写与首尾空白比对. */
@Composable
private fun TypeLayer(view: RingView, onSolved: () -> Unit) {
    val sentences = stringArrayResource(R.array.ring_challenge_type_sentences)
    val own = view.dismiss?.typeText
    val challenge = remember(view.seed, own, sentences.size) { TypeChallenge.of(own, sentences.toList(), view.seed) }
    var input by rememberSaveable(view.occurrence) { mutableStateOf("") }
    var wrong by rememberSaveable(view.occurrence) { mutableStateOf(false) }
    val check = {
        if (challenge.matches(input)) onSolved() else wrong = true
    }
    Text(stringResource(R.string.ring_challenge_type_title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Text(
            challenge.text,
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp).testTag(RingActivity.TAG_CHALLENGE_PROBLEM),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )
    }
    AnswerField(
        value = input,
        onValueChange = {
            input = it
            wrong = false
        },
        label = stringResource(R.string.ring_challenge_type_input),
        error = if (wrong) stringResource(R.string.ring_challenge_type_wrong) else null,
        keyboard = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
        onDone = check,
    )
}

/** The wait for a script to call `alarms.dismiss(id)`. zh-CN: 等待脚本调用 `alarms.dismiss(id)`. */
@Composable
private fun ScriptLayer() {
    Row(Modifier.testTag(RingActivity.TAG_CHALLENGE_SCRIPT), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(R.string.ring_challenge_script_waiting), style = MaterialTheme.typography.titleMedium)
    }
}

/** The field and check button of `math` and `type`. zh-CN: `math` 与 `type` 的输入框与检查按钮. */
@Composable
private fun AnswerField(value: String, onValueChange: (String) -> Unit, label: String, error: String?, keyboard: KeyboardOptions, onDone: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(MAX_INPUT_CHARS)) },
        modifier = Modifier.fillMaxWidth().testTag(RingActivity.TAG_CHALLENGE_INPUT),
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = keyboard,
        keyboardActions = KeyboardActions(onDone = { if (value.isNotBlank()) onDone() }),
    )
    Button(onClick = onDone, enabled = value.isNotBlank(), modifier = Modifier.fillMaxWidth().height(48.dp).testTag(RingActivity.TAG_CHALLENGE_CHECK)) {
        Text(stringResource(R.string.ring_challenge_check))
    }
}

/**
 * The time left until the auto silence, `m:ss`, counted down every second; always shown while a challenge is in effect
 * (roadmap P9.3). zh-CN: 距自动停止的剩余时间 `m:ss`, 每秒倒数; 挑战生效时始终显示 (路线图 P9.3).
 */
@Composable
private fun AutoStop(silenceAt: Long?) {
    silenceAt ?: return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(silenceAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(SECOND_MILLIS - now % SECOND_MILLIS)
        }
    }
    val left = ((silenceAt - now + SECOND_MILLIS - 1) / SECOND_MILLIS).coerceAtLeast(0L)
    Text(
        stringResource(R.string.ring_challenge_auto_stop, left / 60, left % 60),
        Modifier.testTag(RingActivity.TAG_CHALLENGE_AUTO_STOP),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

private const val SECOND_MILLIS = 1_000L

private const val NANOS_PER_MILLI = 1_000_000L

/** Room for the longest `typeText` and some slack. zh-CN: 足够容纳最长的 `typeText` 并留余量. */
private const val MAX_INPUT_CHARS = AlarmLimits.MAX_TYPE_TEXT_CHARS + 50
