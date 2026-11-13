package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingDefaults
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RepeatText
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.ChimeDialog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.Choice
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.ChoiceDialog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.ChoiceRow
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.DialogDefaults
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.MultiChoiceDialog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.format.TimeFormats
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmSnooze
import org.autojs.plugin.alarm.api.AlarmSpeak
import org.autojs.plugin.alarm.api.AlarmTime
import org.autojs.plugin.alarm.api.RingtoneSnapshot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

// The dialogs of the edit page (roadmap P3.1, T83). Each one changes nothing until confirm; cancel, back and a tap
// outside keep the draft as it was (specification section 8).
// zh-CN: 编辑页的对话框 (路线图 P3.1, T83). 确定之前不改变任何内容; 取消, 返回与点击外部保持草稿不变 (规范第 8 节).

private const val DAY_MILLIS = 86_400_000L

/** The time, on the dial or typed in. zh-CN: 时刻, 用表盘选择或直接输入. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeDialog(initial: AlarmTime, is24Hour: Boolean, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val tag = "chime.edit.timeDialog"
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour)
    var typing by rememberSaveable { mutableStateOf(false) }
    ChimeDialog(
        title = stringResource(R.string.edit_time),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.ui_dialog_confirm),
        onConfirm = { onConfirm(state.hour, state.minute) },
        dismissText = stringResource(R.string.ui_dialog_cancel),
        neutralText = stringResource(if (typing) R.string.edit_time_dial else R.string.edit_time_input),
        onNeutral = { typing = !typing },
        tag = tag,
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            if (typing) TimeInput(state, Modifier.testTag("$tag.input")) else TimePicker(state, Modifier.testTag("$tag.dial"))
        }
    }
}

/** The repeat presets; "Chosen weekdays" and "Advanced" continue elsewhere. zh-CN: 重复预设; "自选星期" 与 "高级" 在别处继续. */
@Composable
internal fun RepeatDialog(repeat: String?, start: LocalDate, onDismiss: () -> Unit, onChoose: (RepeatPreset) -> Unit) {
    val current = RepeatPreset.of(repeat)
    val locale = currentLocale()
    val described = remember(repeat, start, locale) { repeat?.let { runCatching { RepeatText.of(it, start, locale) }.getOrNull() } }
    val presets = RepeatPreset.entries
    val options = presets.map { preset ->
        Choice(
            label = stringResource(repeatPresetLabel(preset)),
            note = if (preset == current && (preset == RepeatPreset.CUSTOM || preset == RepeatPreset.ADVANCED)) described else null,
        )
    }
    ChoiceDialog(
        title = stringResource(R.string.edit_repeat),
        options = options,
        selectedIndex = current.ordinal,
        onDismissRequest = onDismiss,
        onConfirm = { onChoose(presets[it]) },
        tag = "chime.edit.repeatDialog",
    )
}

internal fun repeatPresetLabel(preset: RepeatPreset): Int = when (preset) {
    RepeatPreset.ONCE -> R.string.home_once
    RepeatPreset.DAILY -> R.string.edit_repeat_daily
    RepeatPreset.WEEKDAYS -> R.string.edit_repeat_weekdays
    RepeatPreset.WEEKEND -> R.string.edit_repeat_weekend
    RepeatPreset.CUSTOM -> R.string.edit_repeat_custom
    RepeatPreset.ADVANCED -> R.string.edit_repeat_advanced
}

/** Chosen weekdays, in the order of the locale's week. zh-CN: 自选星期, 按当前语言区域的一周顺序排列. */
@Composable
internal fun DaysDialog(days: Set<DayOfWeek>, onDismiss: () -> Unit, onConfirm: (Set<DayOfWeek>) -> Unit) {
    val ordered = weekOf(currentLocale())
    val locale = currentLocale()
    MultiChoiceDialog(
        title = stringResource(R.string.edit_repeat_days_title),
        options = ordered.map { Choice(it.getDisplayName(TextStyle.FULL, locale)) },
        selected = ordered.indices.filterTo(HashSet()) { ordered[it] in days },
        onDismissRequest = onDismiss,
        onConfirm = { chosen -> onConfirm(chosen.mapTo(HashSet()) { ordered[it] }) },
        requireOne = true,
        tag = "chime.edit.daysDialog",
    )
}

/** The seven weekdays from the locale's first day. zh-CN: 从当前语言区域的首日开始的七天. */
internal fun weekOf(locale: Locale): List<DayOfWeek> {
    val first = WeekFields.of(locale).firstDayOfWeek
    return (0L until 7L).map { first.plus(it) }
}

/**
 * A date on the calendar, never before [minDate]; [onNeutral] adds a third button such as "Automatic".
 * zh-CN: 在日历上选日期, 不早于 [minDate]; [onNeutral] 增加第三个按钮, 例如 "自动".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateDialog(
    initial: LocalDate?,
    minDate: LocalDate?,
    tag: String,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    neutralText: String? = null,
    onNeutral: (() -> Unit)? = null,
) {
    val selectable = remember(minDate) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                minDate == null || !LocalDate.ofEpochDay(Math.floorDiv(utcTimeMillis, DAY_MILLIS)).isBefore(minDate)

            override fun isSelectableYear(year: Int): Boolean = minDate == null || year >= minDate.year
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.toEpochDay()?.times(DAY_MILLIS),
        selectableDates = selectable,
    )
    val selected = state.selectedDateMillis?.let { LocalDate.ofEpochDay(Math.floorDiv(it, DAY_MILLIS)) }
    DatePickerDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(tag),
        confirmButton = {
            TextButton(
                onClick = { selected?.let(onConfirm) },
                enabled = selected != null,
                modifier = Modifier.defaultMinSize(minHeight = DialogDefaults.buttonMinHeight).testTag("$tag.confirm"),
            ) { Text(stringResource(R.string.ui_dialog_confirm)) }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (neutralText != null && onNeutral != null) {
                    TextButton(onClick = onNeutral, modifier = Modifier.defaultMinSize(minHeight = DialogDefaults.buttonMinHeight).testTag("$tag.neutral")) {
                        Text(neutralText)
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(minHeight = DialogDefaults.buttonMinHeight).testTag("$tag.cancel")) {
                    Text(stringResource(R.string.ui_dialog_cancel))
                }
            }
        },
    ) {
        DatePicker(state = state)
    }
}

/** The skipped dates of a repeating alarm. zh-CN: 重复闹钟的排除日期. */
@Composable
internal fun ExcludeDialog(dates: List<LocalDate>, today: LocalDate, onDismiss: () -> Unit, onConfirm: (List<LocalDate>) -> Unit) {
    val tag = "chime.edit.excludeDialog"
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf(dates.sorted().joinToString(",")) }
    val pending = text.split(',').filter(String::isNotEmpty).map(LocalDate::parse)
    var adding by rememberSaveable { mutableStateOf(false) }
    val full = pending.size >= AlarmLimits.MAX_EXCLUDE_DATES
    ChimeDialog(
        title = stringResource(R.string.edit_exclude),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.ui_dialog_confirm),
        onConfirm = { onConfirm(pending) },
        dismissText = stringResource(R.string.ui_dialog_cancel),
        neutralText = if (full) null else stringResource(R.string.edit_exclude_add),
        onNeutral = { adding = true },
        tag = tag,
    ) {
        if (pending.isEmpty()) {
            Text(
                stringResource(R.string.edit_exclude_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = DialogDefaults.contentPadding, vertical = 8.dp).testTag("$tag.empty"),
            )
        }
        pending.forEach { date ->
            val label = TimeFormats.date(context, date)
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = DialogDefaults.contentPadding, end = 12.dp).defaultMinSize(minHeight = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { text = (pending - date).joinToString(",") },
                    modifier = Modifier.testTag("$tag.remove.$date"),
                ) {
                    Icon(painterResource(R.drawable.ic_close_24), contentDescription = stringResource(R.string.edit_exclude_remove, label))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    if (adding) {
        DateDialog(
            initial = null,
            minDate = today,
            tag = "chime.edit.excludeDate",
            onDismiss = { adding = false },
            onConfirm = { date ->
                text = (pending + date).toSortedSet().joinToString(",")
                adding = false
            },
        )
    }
}

/** Follow the device zone or keep a fixed one; the wall-clock time stays. zh-CN: 跟随设备时区或固定时区; 墙上时间不变. */
@Composable
internal fun ZoneDialog(current: String?, deviceZone: ZoneId, onDismiss: () -> Unit, onConfirm: (String?) -> Unit) {
    val tag = "chime.edit.zoneDialog"
    var pending by rememberSaveable { mutableStateOf(current.orEmpty()) }
    var query by rememberSaveable { mutableStateOf("") }
    val all = remember { ZoneId.getAvailableZoneIds().filterNot { it.startsWith("SystemV/") }.sorted() }
    val needle = query.trim().replace(' ', '_')
    val zones = remember(needle) { if (needle.isEmpty()) all else all.filter { it.contains(needle, ignoreCase = true) } }
    ChimeDialog(
        title = stringResource(R.string.edit_time_zone),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.ui_dialog_confirm),
        onConfirm = { onConfirm(pending.ifEmpty { null }) },
        dismissText = stringResource(R.string.ui_dialog_cancel),
        scrollable = false,
        tag = tag,
    ) {
        ChoiceRow(
            Choice(stringResource(R.string.edit_time_zone_device_choice), note = zoneText(deviceZone.id)),
            selected = pending.isEmpty(),
            onSelect = { pending = "" },
            modifier = Modifier.testTag("$tag.device"),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = { Text(stringResource(R.string.edit_time_zone_search)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = DialogDefaults.contentPadding, vertical = 8.dp).testTag("$tag.search"),
        )
        LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 360.dp).testTag("$tag.list")) {
            items(zones, key = { it }) { id ->
                ChoiceRow(Choice(zoneText(id)), selected = pending == id, onSelect = { pending = id }, modifier = Modifier.testTag("$tag.zone.$id"))
            }
        }
    }
}

/** The holiday modes in dialog order; null is no filter. zh-CN: 对话框中的节假日模式顺序; null 为不过滤. */
private val HOLIDAY_MODES: List<String?> = listOf(null, AlarmContract.HOLIDAY_MODE_SKIP, AlarmContract.HOLIDAY_MODE_ONLY, AlarmContract.HOLIDAY_MODE_WORKDAYS)

/** The name of a holiday mode (roadmap P8.6). zh-CN: 节假日模式的名称 (路线图 P8.6). */
internal fun holidayModeLabel(mode: String?): Int = when (mode) {
    AlarmContract.HOLIDAY_MODE_SKIP -> R.string.edit_holidays_skip
    AlarmContract.HOLIDAY_MODE_ONLY -> R.string.edit_holidays_only
    AlarmContract.HOLIDAY_MODE_WORKDAYS -> R.string.edit_holidays_workdays
    else -> R.string.edit_holidays_none
}

/**
 * The holiday mode (roadmap P8.6): none, or one of the three modes of the contract, each with what it does.
 * zh-CN: 节假日模式 (路线图 P8.6): 不过滤, 或契约的三种模式之一, 每项说明其作用.
 */
@Composable
internal fun HolidayModeDialog(mode: String?, onDismiss: () -> Unit, onConfirm: (String?) -> Unit) {
    val notes = mapOf(
        AlarmContract.HOLIDAY_MODE_SKIP to R.string.edit_holidays_skip_note,
        AlarmContract.HOLIDAY_MODE_ONLY to R.string.edit_holidays_only_note,
        AlarmContract.HOLIDAY_MODE_WORKDAYS to R.string.edit_holidays_workdays_note,
    )
    ChoiceDialog(
        title = stringResource(R.string.edit_holidays),
        options = HOLIDAY_MODES.map { Choice(stringResource(holidayModeLabel(it)), note = notes[it]?.let { note -> stringResource(note) }) },
        selectedIndex = HOLIDAY_MODES.indexOf(mode).coerceAtLeast(0),
        onDismissRequest = onDismiss,
        onConfirm = { onConfirm(HOLIDAY_MODES[it]) },
        tag = "chime.edit.holidaysDialog",
    )
}

/** The label, at most [AlarmLimits.MAX_LABEL_CHARS] characters. zh-CN: 标签, 最多 [AlarmLimits.MAX_LABEL_CHARS] 个字符. */
@Composable
internal fun LabelDialog(label: String?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val tag = "chime.edit.labelDialog"
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(label.orEmpty(), TextRange(label.orEmpty().length)))
    }
    val focus = remember { FocusRequester() }
    ChimeDialog(
        title = stringResource(R.string.edit_label),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.ui_dialog_confirm),
        onConfirm = { onConfirm(value.text) },
        dismissText = stringResource(R.string.ui_dialog_cancel),
        tag = tag,
    ) {
        LimitedTextField(
            value = value,
            onValueChange = { value = it },
            limit = AlarmLimits.MAX_LABEL_CHARS,
            singleLine = true,
            onDone = { onConfirm(value.text) },
            modifier = Modifier.focusRequester(focus).testTag("$tag.field"),
        )
        Spacer(Modifier.height(8.dp))
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/** "Default - value": a choice or a summary that follows the plugin settings (T84). zh-CN: "默认 - 值": 跟随插件设置的选项或摘要 (T84). */
@Composable
internal fun defaultText(value: String): String = stringResource(R.string.edit_default_value, value)

/**
 * The name of a sound [source]: the system alarm sound, silence or a ringtone of [ringtones] (empty while they load).
 * zh-CN: 铃声 [source] 的名称: 系统闹钟铃声, 静音或 [ringtones] 中的铃声 (加载期间为空).
 */
@Composable
internal fun soundText(source: String, ringtones: List<RingtoneSnapshot>?): String = when (source) {
    AlarmContract.SOUND_DEFAULT -> stringResource(R.string.edit_sound_system)
    AlarmContract.SOUND_SILENT -> stringResource(R.string.edit_sound_silent)
    else -> when (val found = ringtones?.firstOrNull { it.uri == source }) {
        null -> if (ringtones == null) "" else stringResource(R.string.edit_sound_missing)
        else -> found.title.ifEmpty { source }
    }
}

/** "Off" or "Every 10 minutes, at most 3 times". zh-CN: "关闭" 或 "每 10 分钟, 最多 3 次". */
@Composable
internal fun snoozeText(enabled: Boolean, minutes: Int, max: Int): String =
    if (!enabled) {
        stringResource(R.string.edit_off)
    } else {
        stringResource(
            R.string.edit_snooze_summary,
            pluralStringResource(R.plurals.edit_snooze_every, minutes, minutes),
            pluralStringResource(R.plurals.edit_snooze_limit, max, max),
        )
    }

@Composable
internal fun autoSilenceText(minutes: Int): String = pluralStringResource(R.plurals.edit_auto_silence_summary, minutes, minutes)

/**
 * Speech: the text, the engine and when it is spoken; turning it off drops it. The page never speaks.
 * zh-CN: 朗读: 文字, 引擎与朗读时机; 关闭即移除. 本页不朗读.
 */
@Composable
internal fun SpeakDialog(speak: AlarmSpeak?, engines: List<SpeechEngine>, onDismiss: () -> Unit, onConfirm: (AlarmSpeak?) -> Unit) {
    val tag = "chime.edit.speakDialog"
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(speak?.text.orEmpty(), TextRange(speak?.text.orEmpty().length)))
    }
    var engine by rememberSaveable { mutableStateOf(speak?.engine.orEmpty()) }
    var mode by rememberSaveable { mutableStateOf(speak?.mode ?: AlarmContract.SPEAK_MODE_AFTER) }
    val text = value.text.trim()
    fun result(): AlarmSpeak {
        val chosenMode = if (mode == AlarmContract.SPEAK_MODE_AFTER && speak?.mode == null) null else mode
        return (speak ?: AlarmSpeak(text = text)).copy(text = text, engine = engine.ifEmpty { null }, mode = chosenMode)
    }
    ChimeDialog(
        title = stringResource(R.string.edit_speak),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.ui_dialog_confirm),
        onConfirm = { if (text.isNotEmpty()) onConfirm(result()) },
        dismissText = stringResource(R.string.ui_dialog_cancel),
        confirmEnabled = text.isNotEmpty(),
        neutralText = if (speak != null) stringResource(R.string.edit_speak_turn_off) else null,
        onNeutral = { onConfirm(null) },
        tag = tag,
    ) {
        LimitedTextField(
            value = value,
            onValueChange = { value = it },
            limit = AlarmLimits.MAX_SPEAK_TEXT_CHARS,
            singleLine = false,
            label = stringResource(R.string.edit_speak_text),
            modifier = Modifier.testTag("$tag.text"),
        )
        DialogSubheader(stringResource(R.string.edit_speak_engine))
        ChoiceRow(Choice(stringResource(R.string.edit_speak_engine_default)), engine.isEmpty(), { engine = "" }, Modifier.testTag("$tag.engine.default"))
        engines.forEach {
            ChoiceRow(Choice(it.label, note = it.packageName), engine == it.packageName, { engine = it.packageName }, Modifier.testTag("$tag.engine.${it.packageName}"))
        }
        if (engine.isNotEmpty() && engines.none { it.packageName == engine }) {
            ChoiceRow(Choice(stringResource(R.string.edit_speak_engine_missing), note = engine), true, {}, Modifier.testTag("$tag.engine.missing"))
        }
        DialogSubheader(stringResource(R.string.edit_speak_mode))
        AlarmContract.SPEAK_MODES.forEach { m ->
            ChoiceRow(Choice(stringResource(speakModeLabel(m))), mode == m, { mode = m }, Modifier.testTag("$tag.mode.$m"))
        }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Snooze: the plugin default ([fallback]; null on the settings screen, which has no such choice, T84), off, or a length
 * and a maximum count. The sliders start at the values of [snooze], else of the default.
 * zh-CN: 贪睡: 插件默认 ([fallback]; 设置页没有这一项, 此时为 null, T84), 关闭, 或自定时长与次数上限. 滑块初始为 [snooze] 的值, 否则为默认值.
 */
@Composable
internal fun SnoozeDialog(
    snooze: AlarmSnooze?,
    fallback: RingDefaults?,
    onDismiss: () -> Unit,
    onConfirm: (AlarmSnooze?) -> Unit,
    title: String = stringResource(R.string.edit_snooze),
    tag: String = "chime.edit.snoozeDialog",
) {
    var choice by rememberSaveable {
        mutableIntStateOf(
            when {
                snooze == null -> if (fallback != null) 0 else 2
                !snooze.enabled -> 1
                else -> 2
            },
        )
    }
    val start = fallback ?: RingDefaults.PROTOCOL
    var minutes by rememberSaveable { mutableFloatStateOf((snooze?.minutes ?: start.snoozeMinutes).toFloat()) }
    var max by rememberSaveable { mutableFloatStateOf((snooze?.max ?: start.snoozeMax).toFloat()) }
    ChimeDialog(
        title = title,
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.ui_dialog_confirm),
        onConfirm = {
            onConfirm(
                when (choice) {
                    0 -> null
                    1 -> AlarmSnooze.DISABLED
                    else -> AlarmSnooze(enabled = true, minutes = minutes.roundToInt(), max = max.roundToInt())
                },
            )
        },
        dismissText = stringResource(R.string.ui_dialog_cancel),
        tag = tag,
    ) {
        if (fallback != null) {
            ChoiceRow(
                Choice(defaultText(snoozeText(fallback.snoozeEnabled, fallback.snoozeMinutes, fallback.snoozeMax)), note = stringResource(R.string.edit_default_note)),
                choice == 0,
                { choice = 0 },
                Modifier.testTag("$tag.0"),
            )
        }
        ChoiceRow(Choice(stringResource(R.string.edit_off)), choice == 1, { choice = 1 }, Modifier.testTag("$tag.1"))
        ChoiceRow(Choice(stringResource(R.string.edit_custom)), choice == 2, { choice = 2 }, Modifier.testTag("$tag.2"))
        if (choice == 2) {
            LabeledSlider(
                title = stringResource(R.string.edit_snooze_interval),
                value = minutes,
                valueText = pluralStringResource(R.plurals.edit_minutes, minutes.roundToInt(), minutes.roundToInt()),
                range = AlarmLimits.MIN_SNOOZE_MINUTES..AlarmLimits.MAX_SNOOZE_MINUTES,
                onValueChange = { minutes = it },
                tag = "$tag.minutes",
            )
            LabeledSlider(
                title = stringResource(R.string.edit_snooze_max_title),
                value = max,
                valueText = max.roundToInt().toString(),
                range = 1..AlarmLimits.MAX_SNOOZE_MAX,
                onValueChange = { max = it },
                tag = "$tag.max",
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Auto stop: the plugin default ([fallback], in minutes; null on the settings screen, which then shows the slider
 * only, T84) or a number of minutes.
 * zh-CN: 自动停止: 插件默认 ([fallback], 以分钟计; 设置页为 null, 此时只显示滑块, T84) 或指定分钟数.
 */
@Composable
internal fun AutoSilenceDialog(
    minutes: Int?,
    fallback: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit,
    title: String = stringResource(R.string.edit_auto_silence),
    tag: String = "chime.edit.autoSilenceDialog",
) {
    var custom by rememberSaveable { mutableStateOf(minutes != null || fallback == null) }
    var value by rememberSaveable { mutableFloatStateOf((minutes ?: fallback ?: AlarmLimits.DEFAULT_AUTO_SILENCE_MINUTES).toFloat()) }
    ChimeDialog(
        title = title,
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.ui_dialog_confirm),
        onConfirm = { onConfirm(if (custom) value.roundToInt() else null) },
        dismissText = stringResource(R.string.ui_dialog_cancel),
        tag = tag,
    ) {
        if (fallback != null) {
            ChoiceRow(Choice(defaultText(autoSilenceText(fallback)), note = stringResource(R.string.edit_default_note)), !custom, { custom = false }, Modifier.testTag("$tag.0"))
            ChoiceRow(Choice(stringResource(R.string.edit_custom)), custom, { custom = true }, Modifier.testTag("$tag.1"))
        }
        if (custom) {
            LabeledSlider(
                title = stringResource(R.string.edit_auto_silence_after),
                value = value,
                valueText = pluralStringResource(R.plurals.edit_minutes, value.roundToInt(), value.roundToInt()),
                range = AlarmLimits.MIN_AUTO_SILENCE_MINUTES..AlarmLimits.MAX_AUTO_SILENCE_MINUTES,
                onValueChange = { value = it },
                tag = "$tag.minutes",
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** A text field that refuses input beyond [limit] code points and shows the count. zh-CN: 超过 [limit] 个码点即拒绝输入并显示计数的文本框. */
@Composable
private fun LimitedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    limit: Int,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    label: String? = null,
    onDone: (() -> Unit)? = null,
) {
    val count = value.text.codePointCount(0, value.text.length)
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.text.codePointCount(0, it.text.length) <= limit) onValueChange(it) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        maxLines = if (singleLine) 1 else 5,
        label = label?.let { { Text(it) } },
        supportingText = { Text("$count / $limit", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End) },
        keyboardOptions = KeyboardOptions(imeAction = if (onDone != null) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = onDone?.let { { it() } }),
        modifier = modifier.fillMaxWidth().padding(horizontal = DialogDefaults.contentPadding),
    )
}

@Composable
private fun DialogSubheader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() }.padding(start = DialogDefaults.contentPadding, end = DialogDefaults.contentPadding, top = 16.dp, bottom = 4.dp),
    )
}

/** A slider over whole numbers with its title and value. zh-CN: 整数滑块, 带标题与当前值. */
@Composable
private fun ColumnScope.LabeledSlider(title: String, value: Float, valueText: String, range: IntRange, onValueChange: (Float) -> Unit, tag: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = DialogDefaults.contentPadding, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(valueText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("$tag.value"))
        }
        Slider(
            value = value,
            onValueChange = { onValueChange(it.roundToInt().toFloat()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
            modifier = Modifier.testTag(tag),
        )
    }
}

@Composable
internal fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
