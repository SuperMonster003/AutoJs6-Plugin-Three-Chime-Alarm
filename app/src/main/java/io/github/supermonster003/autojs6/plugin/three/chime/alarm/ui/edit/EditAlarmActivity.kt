package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingDefaults
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RepeatText
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.intent.ClockActions
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.intent.ClockIntentActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.service.SystemRingtones
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.ChimeScreen
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.MessageDialog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.SectionHeader
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.SettingsRow
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.SwitchRow
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.TopBarAction
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.format.TimeFormats
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home.PermissionBanner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions.PermissionRow
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions.Permissions
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions.PermissionsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.plugin.alarm.api.AlarmBundles
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmSpec
import org.autojs.plugin.alarm.api.RingtoneSnapshot
import java.time.LocalDate
import java.time.ZoneId

/**
 * The alarm edit page (roadmap P3.1, T83): the time on top with the next ring below it, then the schedule (repeat
 * presets and the advanced rule editor, the date, the skipped dates, the time zone) and the ringing (label, ringtone,
 * vibration, speech, snooze, auto stop). It edits a new alarm (the add button of the list), a stored one (a tap on a
 * list row) or the draft of a system clock `SET_ALARM` intent ([ClockDraftActivity], source `intent`, T79). Options the
 * page leaves at "Default" are not written, so the plugin defaults apply (P3.5). Saving writes only what changed; a
 * changed schedule also enables a disabled alarm. A banner opens the permissions page while the exact alarm,
 * notification or full screen permission is missing (P3.4). The page never plays a ringtone or speaks.
 * zh-CN: 闹钟编辑页 (路线图 P3.1, T83): 顶部是时间与下次响铃, 下面是日程 (重复预设与高级规则编辑器, 日期, 排除日期, 时区) 与响铃 (标签, 铃声, 振动,
 * 朗读, 贪睡, 自动停止). 可编辑新闹钟 (列表的新建按钮), 已保存的闹钟 (点击列表行), 或系统时钟 `SET_ALARM` Intent 的草稿 ([ClockDraftActivity],
 * 来源 `intent`, T79). 保持 "默认" 的选项不写入, 由插件默认值决定 (P3.5). 保存时只写入改动; 日程有改动时同时启用已停用的闹钟. 缺少精确闹钟, 通知或
 * 全屏权限时, 横幅打开权限页 (P3.4). 本页不播放铃声, 也不朗读.
 */
open class EditAlarmActivity : StandaloneActivity() {

    private var dirty = false

    @Composable
    override fun Content() {
        val mode = remember { modeOf(intent) }
        EditScreen(mode, onDirtyChange = { dirty = it }, onClose = ::finish)
    }

    override fun holdsAppearance(): Boolean = dirty

    companion object {

        private const val EXTRA_ID = "io.github.supermonster003.autojs6.plugin.three.chime.alarm.extra.EDIT_ALARM_ID"
        private const val EXTRA_OPTIONS = "io.github.supermonster003.autojs6.plugin.three.chime.alarm.extra.EDIT_ALARM_OPTIONS"
        private const val EXTRA_CLOCK_INTENT = "io.github.supermonster003.autojs6.plugin.three.chime.alarm.extra.EDIT_ALARM_CLOCK_INTENT"

        /** The page of a new alarm. zh-CN: 新闹钟的编辑页. */
        fun create(context: Context): Intent = Intent(context, EditAlarmActivity::class.java)

        /** The page of the stored alarm [id]. zh-CN: 已保存闹钟 [id] 的编辑页. */
        fun edit(context: Context, id: String): Intent = Intent(context, EditAlarmActivity::class.java).putExtra(EXTRA_ID, id)

        /**
         * The new alarm page of `SET_ALARM`, filled in with [options] (from `ClockRequests.setAlarm`) when given.
         * zh-CN: `SET_ALARM` 的新建闹钟页, 给出 [options] (来自 `ClockRequests.setAlarm`) 时预先填好.
         */
        fun draft(context: Context, options: Map<String, Any?>?): Intent = Intent(context, ClockDraftActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(EXTRA_CLOCK_INTENT, true)
            .apply { options?.let { putExtra(EXTRA_OPTIONS, AlarmBundles.toBundle(it)) } }

        internal fun modeOf(intent: Intent): EditMode {
            if (intent.getBooleanExtra(EXTRA_CLOCK_INTENT, false)) {
                return EditMode.ClockIntent(intent.getBundleExtra(EXTRA_OPTIONS)?.let { AlarmBundles.fromBundle(it) })
            }
            return intent.getStringExtra(EXTRA_ID)?.let { EditMode.Existing(it) } ?: EditMode.New
        }
    }
}

/**
 * The edit page of a system clock `SET_ALARM` intent: in a task of its own and out of the recent apps, as the clock
 * intent's caller expects (the window [ClockIntentActivity] used to open, roadmap P2.6).
 * zh-CN: 系统时钟 `SET_ALARM` Intent 的编辑页: 位于单独的任务中且不出现在最近任务里, 符合时钟 Intent 调用方的预期 (此前由 [ClockIntentActivity] 打开的窗口,
 * 路线图 P2.6).
 */
class ClockDraftActivity : EditAlarmActivity()

/** What the page edits. zh-CN: 编辑页编辑的对象. */
internal sealed class EditMode {
    data object New : EditMode()
    data class Existing(val id: String) : EditMode()
    data class ClockIntent(val options: Map<String, Any?>?) : EditMode()
}

/** An installed speech engine. zh-CN: 已安装的语音引擎. */
internal data class SpeechEngine(val packageName: String, val label: String)

private const val TAG = "ChimeEdit"
private const val PAGE_MAIN = 0
private const val PAGE_RULE = 1
private const val PREVIEW_COUNT = 5
private val SET_ALARM = ClockIntentActivity.nameOf(AlarmClock.ACTION_SET_ALARM)

/** The banner rows of this page: the ones a single alarm needs to ring. zh-CN: 本页横幅显示的行: 单个闹钟响铃所需的权限. */
private val BANNER_ROWS = setOf(PermissionRow.EXACT_ALARM, PermissionRow.NOTIFICATIONS, PermissionRow.FULL_SCREEN)

internal object EditDialogs {
    const val TIME = "time"
    const val REPEAT = "repeat"
    const val DAYS = "days"
    const val DATE = "date"
    const val EXCLUDE = "exclude"
    const val ZONE = "zone"
    const val LABEL = "label"
    const val SOUND = "sound"
    const val SPEAK = "speak"
    const val SNOOZE = "snooze"
    const val AUTO_SILENCE = "autoSilence"
    const val DISCARD = "discard"
}

/**
 * Keeps a draft across recreation and process death as its raw values, so a restored draft equals the saved one.
 * zh-CN: 以原始值在重建与进程终止之间保存草稿, 恢复后的草稿与保存时完全相同.
 */
private val DraftSaver = Saver<AlarmDraft?, Bundle>(
    save = { draft ->
        draft?.let {
            Bundle().apply {
                putBundle("spec", AlarmBundles.toBundle(it.spec.toValues()))
                putBoolean("dateAuto", it.dateAuto)
            }
        }
    },
    restore = { bundle ->
        bundle.getBundle("spec")?.let { spec ->
            runCatching { AlarmDraft(AlarmSpec.fromValues(AlarmBundles.fromBundle(spec)), bundle.getBoolean("dateAuto")) }.getOrNull()
        }
    },
)

@Composable
private fun EditScreen(mode: EditMode, onDirtyChange: (Boolean) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val deviceZone = remember { ZoneId.systemDefault() }
    var start by rememberSaveable(stateSaver = DraftSaver) { mutableStateOf<AlarmDraft?>(null) }
    var draft by rememberSaveable(stateSaver = DraftSaver) { mutableStateOf<AlarmDraft?>(null) }
    var page by rememberSaveable { mutableIntStateOf(PAGE_MAIN) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var missing by remember { mutableStateOf(emptyList<PermissionRow>()) }
    var ringtones by remember { mutableStateOf<List<RingtoneSnapshot>?>(null) }
    var defaults by remember { mutableStateOf(RingDefaults.PROTOCOL) }
    var engines by remember { mutableStateOf(emptyList<SpeechEngine>()) }

    LaunchedEffect(Unit) {
        if (draft != null && start != null) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) { initialDraft(context, mode, System.currentTimeMillis(), deviceZone) }
        if (loaded == null) {
            withContext(Dispatchers.Main.immediate) {
                Toast.makeText(context.applicationContext, R.string.edit_missing, Toast.LENGTH_SHORT).show()
                onClose()
            }
            return@LaunchedEffect
        }
        start = loaded
        draft = loaded
    }
    LaunchedEffect(Unit) {
        val (tones, speech, plugin) = withContext(Dispatchers.IO) {
            val runtime = AlarmRuntime.get(context)
            Triple(SystemRingtones.list(context) + runtime.ringtones.list(), speechEngines(context), runtime.defaults.get())
        }
        ringtones = tones
        engines = speech
        defaults = plugin
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L - System.currentTimeMillis() % 60_000L)
            now = System.currentTimeMillis()
        }
    }
    OnResume {
        now = System.currentTimeMillis()
        scope.launch { missing = withContext(Dispatchers.IO) { Permissions.read(context).missing.filter { it in BANNER_ROWS } } }
    }

    val dirty = draft != null && draft != start
    SideEffect { onDirtyChange(dirty) }

    fun save() {
        val current = draft ?: return
        if (saving) return
        saving = true
        // The result is reported on the main thread explicitly: a test dispatcher may resume the scope elsewhere, and the
        // toast needs a looper. zh-CN: 结果明确在主线程报告: 测试调度器可能在其他线程恢复协程, 而 toast 需要 Looper.
        scope.launch(Dispatchers.Main.immediate) {
            val result = withContext(Dispatchers.IO) { runCatching { store(context, mode, current) } }
            saving = false
            result.onSuccess { saved ->
                saved.report(context)
                onDirtyChange(false)
                onClose()
            }.onFailure { e ->
                Log.w(TAG, "save failed: ${e.message}")
                if (mode is EditMode.ClockIntent && e is AlarmException) ClockActions.report(context, SET_ALARM, "error=${e.message}", null)
                snackbar.showSnackbar(messageOf(context, e))
            }
        }
    }

    val current = draft
    if (current != null && page == PAGE_RULE) {
        RuleEditor(
            initial = current.spec.repeat,
            start = current.dateOf(now, deviceZone),
            preview = { rule -> current.preview(PREVIEW_COUNT, now, deviceZone, rule) },
            onApply = { rule ->
                draft = current.withRepeat(rule, deviceZone)
                page = PAGE_MAIN
            },
            onBack = { page = PAGE_MAIN },
        )
        return
    }

    BackHandler(enabled = dirty) { dialog = EditDialogs.DISCARD }
    val title = stringResource(if (mode is EditMode.Existing) R.string.edit_title else R.string.edit_new_alarm)
    ChimeScreen(
        title = title,
        onBack = { if (dirty) dialog = EditDialogs.DISCARD else onClose() },
        snackbarHostState = snackbar,
        actions = {
            if (current != null) TopBarAction(R.drawable.ic_check_24, stringResource(R.string.edit_save), "chime.edit.save") { save() }
        },
    ) { padding ->
        if (current == null) {
            Box(Modifier.padding(padding).fillMaxSize())
            return@ChimeScreen
        }
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).testTag("chime.edit.content")) {
            if (missing.isNotEmpty()) {
                PermissionBanner(missing, { context.startActivity(Intent(context, PermissionsActivity::class.java)) }, "chime.edit.permissionBanner")
            }
            EditContent(current, ringtones, defaults, now, deviceZone, onDialog = { dialog = it }, onChange = { draft = it })
        }
    }

    if (current == null) return
    val close = { dialog = null }
    fun apply(next: AlarmDraft) {
        draft = next
        dialog = null
    }
    val today = LocalDate.now(deviceZone)
    when (dialog) {
        EditDialogs.TIME -> TimeDialog(current.timeOf(deviceZone), TimeFormats.is24Hour(context), close) { hour, minute ->
            apply(current.withTime(hour, minute, deviceZone))
        }
        EditDialogs.REPEAT -> RepeatDialog(current.spec.repeat, current.dateOf(now, deviceZone), close) { preset ->
            when (preset) {
                RepeatPreset.CUSTOM -> dialog = EditDialogs.DAYS
                RepeatPreset.ADVANCED -> {
                    dialog = null
                    page = PAGE_RULE
                }
                else -> apply(current.withRepeat(RepeatPreset.ruleOf(preset), deviceZone))
            }
        }
        EditDialogs.DAYS -> DaysDialog(RepeatPreset.daysOf(current.spec.repeat), close) { days ->
            apply(current.withRepeat(RepeatPreset.ruleOf(days), deviceZone))
        }
        EditDialogs.DATE -> DateDialog(
            initial = current.dateOf(now, deviceZone),
            minDate = today,
            tag = "chime.edit.dateDialog",
            onDismiss = close,
            onConfirm = { apply(current.withDate(it, deviceZone)) },
            neutralText = stringResource(R.string.edit_date_auto_button),
            onNeutral = { apply(current.withDate(null, deviceZone)) },
        )
        EditDialogs.EXCLUDE -> ExcludeDialog(current.spec.exclude.orEmpty().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }, today, close) {
            apply(current.withExclude(it))
        }
        EditDialogs.ZONE -> ZoneDialog(current.spec.timeZone, deviceZone, close) { apply(current.withTimeZone(it, deviceZone)) }
        EditDialogs.LABEL -> LabelDialog(current.spec.label, close) { apply(current.withLabel(it)) }
        EditDialogs.SOUND -> SoundDialog(current.spec.sound?.source, defaults.sound, ringtones.orEmpty(), close, { apply(current.withSound(it)) })
        EditDialogs.SPEAK -> SpeakDialog(current.spec.speak, engines, close) { apply(current.withSpeak(it)) }
        EditDialogs.SNOOZE -> SnoozeDialog(current.spec.snooze, defaults, close, { apply(current.withSnooze(it)) })
        EditDialogs.AUTO_SILENCE -> AutoSilenceDialog(current.spec.autoSilenceMinutes, defaults.autoSilenceFor(current.hasChallenge), close, { apply(current.withAutoSilence(it)) })
        EditDialogs.DISCARD -> MessageDialog(
            title = stringResource(R.string.edit_discard_title),
            message = stringResource(R.string.edit_discard_message),
            confirmText = stringResource(R.string.edit_discard),
            onDismissRequest = close,
            onConfirm = {
                dialog = null
                onDirtyChange(false)
                onClose()
            },
            tag = "chime.edit.discardDialog",
        )
    }
}

/** The time, the next ring and the rows of the main page. zh-CN: 主页面的时间, 下次响铃与各行. */
@Composable
private fun EditContent(
    draft: AlarmDraft,
    ringtones: List<RingtoneSnapshot>?,
    defaults: RingDefaults,
    now: Long,
    deviceZone: ZoneId,
    onDialog: (String) -> Unit,
    onChange: (AlarmDraft) -> Unit,
) {
    val context = LocalContext.current
    val spec = draft.spec
    val time = draft.timeOf(deviceZone)
    val date = draft.dateOf(now, deviceZone)
    val today = LocalDate.now(deviceZone)
    Text(
        text = TimeFormats.time(context, time.hour, time.minute, time.second),
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.edit_time)) { onDialog(EditDialogs.TIME) }
            .padding(top = 24.dp, bottom = 8.dp)
            .testTag("chime.edit.time"),
    )
    val next = remember(draft, now) { draft.preview(1, now, deviceZone).firstOrNull() }
    Text(
        text = if (next == null) {
            stringResource(R.string.edit_never_rings)
        } else {
            stringResource(R.string.home_next_ring, TimeFormats.dayOf(context, next), TimeFormats.timeOf(context, next), TimeFormats.remaining(context, next, now))
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 16.dp).testTag("chime.edit.next"),
    )

    SectionHeader(stringResource(R.string.edit_section_schedule))
    SettingsRow(
        title = stringResource(R.string.edit_repeat),
        summary = repeatSummary(spec.repeat, date),
        icon = R.drawable.ic_repeat_24,
        modifier = Modifier.testTag("chime.edit.repeat"),
        onClick = { onDialog(EditDialogs.REPEAT) },
    )
    SettingsRow(
        title = stringResource(if (spec.repeat == null) R.string.edit_date else R.string.edit_start_date),
        summary = if (draft.dateAuto) stringResource(R.string.edit_date_auto, dayText(date, today)) else TimeFormats.date(context, date),
        icon = R.drawable.ic_event_24,
        modifier = Modifier.testTag("chime.edit.date"),
        onClick = { onDialog(EditDialogs.DATE) },
    )
    val exclude = spec.exclude.orEmpty()
    if (spec.repeat != null || exclude.isNotEmpty()) {
        SettingsRow(
            title = stringResource(R.string.edit_exclude),
            summary = if (exclude.isEmpty()) stringResource(R.string.edit_none) else pluralStringResource(R.plurals.edit_exclude_count, exclude.size, exclude.size),
            icon = R.drawable.ic_event_24,
            modifier = Modifier.testTag("chime.edit.exclude"),
            onClick = { onDialog(EditDialogs.EXCLUDE) },
        )
    }
    SettingsRow(
        title = stringResource(R.string.edit_time_zone),
        summary = spec.timeZone?.let { zoneText(it) } ?: stringResource(R.string.edit_time_zone_device, deviceZone.id),
        icon = R.drawable.ic_public_24,
        modifier = Modifier.testTag("chime.edit.timeZone"),
        onClick = { onDialog(EditDialogs.ZONE) },
    )

    SectionHeader(stringResource(R.string.edit_section_ring))
    SettingsRow(
        title = stringResource(R.string.edit_label),
        summary = spec.label ?: stringResource(R.string.edit_none),
        icon = R.drawable.ic_label_24,
        modifier = Modifier.testTag("chime.edit.label"),
        onClick = { onDialog(EditDialogs.LABEL) },
    )
    SettingsRow(
        title = stringResource(R.string.edit_sound),
        summary = spec.sound?.source?.let { soundText(it, ringtones) } ?: defaultText(soundText(defaults.sound, ringtones)),
        icon = R.drawable.ic_music_note_24,
        modifier = Modifier.testTag("chime.edit.sound"),
        onClick = { onDialog(EditDialogs.SOUND) },
    )
    SwitchRow(
        title = stringResource(R.string.edit_vibrate),
        checked = spec.vibrate ?: defaults.vibrate,
        onToggle = { onChange(draft.withVibrate(it)) },
        icon = R.drawable.ic_vibration_24,
        modifier = Modifier.testTag("chime.edit.vibrate"),
    )
    SettingsRow(
        title = stringResource(R.string.edit_speak),
        summary = spec.speak?.let { stringResource(R.string.edit_speak_summary, stringResource(speakModeLabel(it.mode)), it.text) } ?: stringResource(R.string.edit_off),
        icon = R.drawable.ic_record_voice_over_24,
        modifier = Modifier.testTag("chime.edit.speak"),
        onClick = { onDialog(EditDialogs.SPEAK) },
    )
    val snooze = spec.snooze
    SettingsRow(
        title = stringResource(R.string.edit_snooze),
        summary = when (snooze) {
            null -> defaultText(snoozeText(defaults.snoozeEnabled, defaults.snoozeMinutes, defaults.snoozeMax))
            else -> snoozeText(snooze.enabled, snooze.minutes ?: defaults.snoozeMinutes, snooze.max ?: defaults.snoozeMax)
        },
        icon = R.drawable.ic_snooze_24,
        modifier = Modifier.testTag("chime.edit.snooze"),
        onClick = { onDialog(EditDialogs.SNOOZE) },
    )
    SettingsRow(
        title = stringResource(R.string.edit_auto_silence),
        summary = spec.autoSilenceMinutes?.let { autoSilenceText(it) } ?: defaultText(autoSilenceText(defaults.autoSilenceFor(draft.hasChallenge))),
        icon = R.drawable.ic_timer_24,
        modifier = Modifier.testTag("chime.edit.autoSilence"),
        onClick = { onDialog(EditDialogs.AUTO_SILENCE) },
    )
    Spacer(Modifier.height(24.dp))
}

/** The repeat row: the rule described from its first date, or "Once". zh-CN: 重复一行: 从首个日期描述规则, 或 "一次". */
@Composable
private fun repeatSummary(repeat: String?, start: LocalDate): String {
    repeat ?: return stringResource(R.string.home_once)
    val locale = LocalConfiguration.current.locales[0]
    return remember(repeat, start, locale) { runCatching { RepeatText.of(repeat, start, locale) }.getOrDefault(repeat) }
}

/** "Today", "Tomorrow" or the date. zh-CN: "今天", "明天" 或日期. */
@Composable
internal fun dayText(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.home_day_today)
    today.plusDays(1) -> stringResource(R.string.home_day_tomorrow)
    else -> TimeFormats.date(LocalContext.current, date)
}

/** A fixed zone with its current offset, such as `Asia/Tokyo (UTC+09:00)`. zh-CN: 固定时区及其当前偏移. */
internal fun zoneText(id: String): String = runCatching {
    val offset = ZoneId.of(id).rules.getOffset(java.time.Instant.now()).id
    "$id (UTC${if (offset == "Z") "" else offset})"
}.getOrDefault(id)

internal fun speakModeLabel(mode: String?): Int = when (mode) {
    AlarmContract.SPEAK_MODE_BEFORE -> R.string.edit_speak_mode_before
    AlarmContract.SPEAK_MODE_INTERLEAVE -> R.string.edit_speak_mode_interleave
    AlarmContract.SPEAK_MODE_ONLY -> R.string.edit_speak_mode_only
    else -> R.string.edit_speak_mode_after
}

private fun initialDraft(context: Context, mode: EditMode, now: Long, deviceZone: ZoneId): AlarmDraft? = when (mode) {
    EditMode.New -> AlarmDraft.forNew(now, deviceZone)
    is EditMode.ClockIntent -> AlarmDraft.fromOptions(mode.options, now, deviceZone)
    is EditMode.Existing -> AlarmRuntime.get(context).repository.get(mode.id)?.let(AlarmDraft::of)
}

private fun speechEngines(context: Context): List<SpeechEngine> {
    val packageManager = context.packageManager
    return runCatching {
        packageManager.queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0)
            .map { SpeechEngine(it.serviceInfo.packageName, it.loadLabel(packageManager).toString()) }
            .distinctBy { it.packageName }
    }.getOrDefault(emptyList())
}

/** What a save did, reported on the main thread. zh-CN: 一次保存的结果, 在主线程报告. */
private sealed class Saved {

    abstract fun report(context: Context)

    /** A user save; [record] is null when nothing changed. zh-CN: 用户保存; 没有改动时 [record] 为 null. */
    class User(private val record: AlarmRecord?) : Saved() {
        override fun report(context: Context) {
            val next = record?.takeIf { it.enabled }?.state?.nextTrigger ?: return
            val text = context.getString(R.string.edit_toast_rings_in, TimeFormats.remaining(context, next))
            Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT).show()
        }
    }

    class Clock(private val stored: ClockActions.Stored) : Saved() {
        override fun report(context: Context) = ClockActions.saved(context, SET_ALARM, stored)
    }
}

/** Saves [draft] off the main thread. zh-CN: 在主线程之外保存 [draft]. */
private fun store(context: Context, mode: EditMode, draft: AlarmDraft): Saved {
    val repository = AlarmRuntime.get(context).repository
    return when (mode) {
        EditMode.New -> Saved.User(repository.add(draft.toOptions(), AlarmContract.SOURCE_USER))
        is EditMode.ClockIntent -> Saved.Clock(ClockActions.store(context, draft.toOptions(), reuse = draft.reusable))
        is EditMode.Existing -> {
            val record = repository.get(mode.id) ?: throw AlarmException.notFound("alarm ${mode.id} no longer exists")
            val update = draft.toUpdate(record)
            Saved.User(if (update.isEmpty) null else repository.update(mode.id, update.patch, AlarmContract.SOURCE_USER, update.implicitDate))
        }
    }
}

private fun messageOf(context: Context, e: Throwable): String {
    if (e !is AlarmException) return context.getString(R.string.edit_error, e.javaClass.simpleName)
    return when (e.code) {
        AlarmErrorCodes.PERMISSION_REQUIRED -> context.getString(R.string.edit_error_permission)
        AlarmErrorCodes.NOT_FOUND -> context.getString(R.string.edit_missing)
        AlarmErrorCodes.LIMIT_EXCEEDED -> if (e.detail.contains("enabled")) {
            context.resources.getQuantityString(R.plurals.home_limit_enabled, AlarmLimits.MAX_ENABLED_ALARMS, AlarmLimits.MAX_ENABLED_ALARMS)
        } else {
            context.getString(R.string.edit_error, e.detail.ifEmpty { e.code })
        }
        else -> context.getString(R.string.edit_error, e.detail.ifEmpty { e.code })
    }
}

/** Runs [action] at every resume. zh-CN: 每次恢复时运行 [action]. */
@Composable
private fun OnResume(action: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val current by rememberUpdatedState(action)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) current() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}
