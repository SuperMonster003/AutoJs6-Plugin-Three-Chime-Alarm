package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RecurrenceRule
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RecurrenceRule.Freq
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RepeatText
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.ChimeScreen
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.Choice
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.ChoiceRow
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.SectionHeader
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.SettingsRow
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.TopBarAction
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.format.TimeFormats
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle

/**
 * Keeps the fields of the rule editor, invalid ones included, across recreation.
 * zh-CN: 在重建之间保存规则编辑器的字段, 包括尚未有效的字段.
 */
private val RuleFormSaver = Saver<RuleForm, ArrayList<String>>(
    save = { form ->
        arrayListOf(
            form.base.text, form.freq.name, form.interval, form.days.joinToString(","), form.monthDays, form.setPos,
            form.end.name, form.count, form.until?.text.orEmpty(),
        )
    },
    restore = { values ->
        runCatching {
            val base = RecurrenceRule.parse(values[0])
            RuleForm(
                base = base,
                freq = Freq.valueOf(values[1]),
                interval = values[2],
                days = if (values[3].isEmpty()) emptyList() else RecurrenceRule.parse("FREQ=YEARLY;BYDAY=${values[3]}").byDay,
                monthDays = values[4],
                setPos = values[5],
                end = RuleForm.End.valueOf(values[6]),
                count = values[7],
                until = if (values[8].isEmpty()) null else RecurrenceRule.parse("FREQ=DAILY;UNTIL=${values[8]}").until,
            )
        }.getOrNull()
    },
)

/**
 * The advanced rule editor (roadmap P3.1, T83): fields for `FREQ`, `INTERVAL`, `BYDAY`, `BYMONTHDAY`, `BYSETPOS`,
 * `COUNT` and `UNTIL`, the RRULE text they make (also editable), its description as the list shows it (`repeatText`)
 * and the next five rings. A field change rewrites the text; a valid text rewrites the fields. Back discards, the check
 * mark applies the rule to the draft.
 * zh-CN: 高级规则编辑器 (路线图 P3.1, T83): `FREQ`, `INTERVAL`, `BYDAY`, `BYMONTHDAY`, `BYSETPOS`, `COUNT` 与 `UNTIL` 的字段, 它们组成的 RRULE
 * 文本 (也可直接编辑), 与列表一致的描述 (`repeatText`) 以及接下来 5 次响铃. 修改字段会改写文本; 有效的文本会改写字段. 返回即放弃, 勾号把规则应用到草稿.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RuleEditor(initial: String?, start: LocalDate, preview: (String) -> List<Long>, onApply: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val locale = currentLocale()
    var form by rememberSaveable(stateSaver = RuleFormSaver) { mutableStateOf(RuleForm.of(initial)) }
    var text by rememberSaveable { mutableStateOf((form.build() as? RuleForm.Result.Valid)?.rule ?: initial.orEmpty()) }
    var typed by rememberSaveable { mutableStateOf(false) }
    var picking by rememberSaveable { mutableStateOf(false) }

    val formResult = form.build()
    val textResult = remember(text) {
        try {
            RuleForm.Result.Valid(RecurrenceRule.fromString(text))
        } catch (e: AlarmException) {
            RuleForm.Result.Invalid(RuleForm.Problem.RULE, e.detail)
        }
    }
    val result = if (typed) textResult else formResult
    val rule = (result as? RuleForm.Result.Valid)?.rule
    val problem = (formResult as? RuleForm.Result.Invalid)?.problem.takeUnless { typed }

    fun updateForm(next: RuleForm) {
        form = next
        typed = false
        (next.build() as? RuleForm.Result.Valid)?.let { text = it.rule }
    }

    fun updateText(next: String) {
        text = next
        typed = true
        val parsed = runCatching { RecurrenceRule.parse(next) }.getOrNull() ?: return
        form = RuleForm(parsed)
    }

    BackHandler(onBack = onBack)
    ChimeScreen(
        title = stringResource(R.string.edit_rule_title),
        onBack = onBack,
        actions = {
            TopBarAction(R.drawable.ic_check_24, stringResource(R.string.edit_rule_apply), "chime.edit.rule.apply", enabled = rule != null) {
                rule?.let(onApply)
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding().verticalScroll(rememberScrollState()).testTag("chime.edit.rule.content")) {
            SectionHeader(stringResource(R.string.edit_rule_frequency))
            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Freq.entries.forEach { freq ->
                    FilterChip(
                        selected = form.freq == freq,
                        onClick = { updateForm(form.copy(freq = freq)) },
                        label = { Text(stringResource(freqLabel(freq))) },
                        modifier = Modifier.testTag("chime.edit.rule.freq.${freq.name}"),
                    )
                }
            }
            RuleField(
                value = form.interval,
                onValueChange = { updateForm(form.copy(interval = it)) },
                label = stringResource(R.string.edit_rule_interval),
                hint = stringResource(R.string.edit_rule_interval_hint),
                error = if (problem == RuleForm.Problem.INTERVAL) stringResource(R.string.edit_rule_error_positive) else null,
                number = true,
                tag = "chime.edit.rule.interval",
            )

            SectionHeader(stringResource(R.string.edit_rule_days))
            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weekOf(locale).forEach { day ->
                    FilterChip(
                        selected = form.hasDay(day),
                        onClick = { updateForm(form.toggleDay(day)) },
                        label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                        modifier = Modifier.testTag("chime.edit.rule.day.${RecurrenceRule.codeOf(day)}"),
                    )
                }
            }
            val ordinalDays = form.days.filter { it.ordinal != null }
            if (ordinalDays.isNotEmpty()) {
                Text(
                    stringResource(R.string.edit_rule_days_more, ordinalDays.joinToString(", ")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp).testTag("chime.edit.rule.daysMore"),
                )
            }
            val monthly = form.freq == Freq.MONTHLY || form.freq == Freq.YEARLY
            if (monthly || form.monthDays.isNotBlank()) {
                RuleField(
                    value = form.monthDays,
                    onValueChange = { updateForm(form.copy(monthDays = it)) },
                    label = stringResource(R.string.edit_rule_month_days),
                    hint = stringResource(R.string.edit_rule_month_days_hint),
                    error = if (problem == RuleForm.Problem.MONTH_DAYS) stringResource(R.string.edit_rule_error_numbers) else null,
                    tag = "chime.edit.rule.monthDays",
                )
            }
            if (monthly || form.setPos.isNotBlank()) {
                RuleField(
                    value = form.setPos,
                    onValueChange = { updateForm(form.copy(setPos = it)) },
                    label = stringResource(R.string.edit_rule_set_pos),
                    hint = stringResource(R.string.edit_rule_set_pos_hint),
                    error = if (problem == RuleForm.Problem.SET_POS) stringResource(R.string.edit_rule_error_numbers) else null,
                    tag = "chime.edit.rule.setPos",
                )
            }

            SectionHeader(stringResource(R.string.edit_rule_end))
            RuleForm.End.entries.forEach { end ->
                ChoiceRow(
                    Choice(stringResource(endLabel(end))),
                    selected = form.end == end,
                    onSelect = { updateForm(form.copy(end = end)) },
                    modifier = Modifier.testTag("chime.edit.rule.end.${end.name}"),
                )
            }
            when (form.end) {
                RuleForm.End.COUNT -> RuleField(
                    value = form.count,
                    onValueChange = { updateForm(form.copy(count = it)) },
                    label = stringResource(R.string.edit_rule_count),
                    hint = null,
                    error = if (problem == RuleForm.Problem.COUNT) stringResource(R.string.edit_rule_error_positive) else null,
                    number = true,
                    tag = "chime.edit.rule.count",
                )
                RuleForm.End.UNTIL -> SettingsRow(
                    title = stringResource(R.string.edit_rule_until),
                    summary = when (val until = form.until) {
                        null -> stringResource(R.string.edit_rule_until_pick)
                        is RecurrenceRule.Until.Date -> TimeFormats.date(context, until.date)
                        else -> until.text
                    },
                    summaryColor = if (problem == RuleForm.Problem.UNTIL) MaterialTheme.colorScheme.error else Color.Unspecified,
                    icon = R.drawable.ic_event_24,
                    modifier = Modifier.testTag("chime.edit.rule.until"),
                    onClick = { picking = true },
                )
                RuleForm.End.NEVER -> Unit
            }

            SectionHeader(stringResource(R.string.edit_rule_text))
            val textError = (textResult as? RuleForm.Result.Invalid)?.takeIf { typed }
            OutlinedTextField(
                value = text,
                onValueChange = ::updateText,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                isError = textError != null,
                supportingText = textError?.let {
                    { Text(stringResource(R.string.edit_rule_error_rule, it.detail.orEmpty()), modifier = Modifier.testTag("chime.edit.rule.error")) }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).testTag("chime.edit.rule.rrule"),
            )

            SectionHeader(stringResource(R.string.edit_rule_description))
            val description = remember(rule, start, locale) { rule?.let { runCatching { RepeatText.of(it, start, locale) }.getOrNull() } }
            Text(
                description ?: stringResource(R.string.edit_rule_invalid),
                style = MaterialTheme.typography.bodyLarge,
                color = if (description == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp).testTag("chime.edit.rule.text"),
            )

            SectionHeader(stringResource(R.string.edit_rule_preview))
            val rings = remember(rule) { rule?.let(preview).orEmpty() }
            if (rule != null && rings.isEmpty()) {
                Text(
                    stringResource(R.string.edit_never_rings),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp).testTag("chime.edit.rule.never"),
                )
            }
            val zone = remember { ZoneId.systemDefault() }
            rings.forEachIndexed { index, at ->
                val date = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
                Text(
                    stringResource(R.string.edit_rule_preview_item, TimeFormats.date(context, date), TimeFormats.timeOf(context, at, zone)),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp).testTag("chime.edit.rule.preview.$index"),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (picking) {
        DateDialog(
            initial = (form.until as? RecurrenceRule.Until.Date)?.date,
            minDate = start,
            tag = "chime.edit.rule.untilDialog",
            onDismiss = { picking = false },
            onConfirm = { date ->
                updateForm(form.copy(until = RecurrenceRule.Until.Date(date)))
                picking = false
            },
        )
    }
}

@Composable
private fun RuleField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String?,
    error: String?,
    tag: String,
    number: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = { Text(label) },
        isError = error != null,
        supportingText = (error ?: hint)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Number else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp).testTag(tag),
    )
}

private fun freqLabel(freq: Freq): Int = when (freq) {
    Freq.DAILY -> R.string.edit_rule_daily
    Freq.WEEKLY -> R.string.edit_rule_weekly
    Freq.MONTHLY -> R.string.edit_rule_monthly
    Freq.YEARLY -> R.string.edit_rule_yearly
}

private fun endLabel(end: RuleForm.End): Int = when (end) {
    RuleForm.End.NEVER -> R.string.edit_rule_end_never
    RuleForm.End.COUNT -> R.string.edit_rule_end_count
    RuleForm.End.UNTIL -> R.string.edit_rule_end_until
}
