package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.settings

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.net.CancelToken
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.KeepAliveSettings
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingDefaults
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring.KeepAliveService
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring.RingDefaultsStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.service.SystemRingtones
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.timer.TimerNotifications
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.about.NoticesActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.appearance.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.appearance.HostAppearanceReader
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.appearance.LauncherIconMode
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.appearance.LauncherIcons
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.appearance.ThemeColorValue
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.ChimeScreen
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.Choice
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.ChoiceDialog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.MultiChoiceDialog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.RowDefaults
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.SectionHeader
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.SettingsRow
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.SwitchRow
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.ThemeColorDialog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.AutoSilenceDialog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.SnoozeDialog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.autoSilenceText
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.snoozeText
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.soundText
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.format.TimeFormat
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.format.TimeFormats
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.holiday.HolidayView
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.holiday.RegionDialog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.holiday.holidaySettingsChanged
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.holiday.regionFacts
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.holiday.regionText
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.holiday.statusLines
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.release.ReleaseHistoryActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.ringtones.RingtonePicker
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.ringtones.RingtoneRequest
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.update.AppUpdateController
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.update.AppUpdateStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.update.UpdateDialogs
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.update.UpdateFrequency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.autojs.plugin.alarm.api.AlarmSnooze
import org.autojs.plugin.alarm.api.RingtoneSnapshot
import java.util.Locale

/**
 * The settings screen (roadmap P3.5, T84, standalone settings specification), adapted from 3-Macaw Speech: appearance
 * (language, night mode, theme color, launcher icon; the first three follow AutoJs6 by default); the alarm defaults
 * (sound, vibration, snooze, auto stop) that every alarm leaving an option at "Default" uses when it rings; region and
 * format (the clock format of the screens, the default holiday region); holiday data (the weekly update check, its
 * metered network choice, the data status and a manual check); and about (version, release history, the update check with
 * its automatic frequency and metered network choice, ignored versions, notices). Every choice dialog edits a draft and
 * saves once on confirm. Since 1.3.0 a dismiss challenges group follows the defaults with the emergency dismiss switch.
 * zh-CN: 设置页 (路线图 P3.5, T84, 独立设置规范), 改编自 3-Macaw Speech: 外观 (语言, 夜间模式, 主题色, 启动器图标; 前三项默认跟随 AutoJs6); 闹钟默认值
 * (铃声, 振动, 贪睡, 自动停止), 每个把选项留在 "默认" 的闹钟响铃时都使用它们; 区域与格式 (界面的时钟格式, 默认节假日地区); 节假日数据 (每周更新检查,
 * 其计量网络选项, 数据状态与手动检查); 以及关于 (版本, 发行历史, 更新检查及其自动检查频率与计量网络选项, 已忽略的版本, 声明). 每个选择对话框编辑草稿,
 * 确定时保存一次. 自 1.3.0 起默认值之后是关闭挑战分组, 含紧急关闭开关.
 */
class SettingsActivity : StandaloneActivity() {

    private enum class Dialog { LANGUAGE, DARK_MODE, THEME_COLOR, LAUNCHER_ICON, SNOOZE, AUTO_SILENCE, TIME_FORMAT, HOLIDAY_REGION, FREQUENCY, IGNORED }

    private lateinit var store: AppUpdateStore
    private lateinit var updates: AppUpdateController

    /** The store the ring coordinator reads the defaults from (T84). zh-CN: 响铃协调器读取默认值的存储 (T84). */
    private val ringDefaults by lazy { RingDefaultsStore(applicationContext) }
    private var defaults by mutableStateOf<RingDefaults?>(null)
    private var ringtones by mutableStateOf<List<RingtoneSnapshot>?>(null)
    private var resumed by mutableIntStateOf(0)
    private var revision by mutableIntStateOf(0)
    private val snackbar = SnackbarHostState()
    private var pendingMessage by mutableStateOf<String?>(null)
    private var dialog by mutableStateOf<Dialog?>(null)

    /** The holiday data and its status lines, loaded off the main thread (roadmap P8.6). zh-CN: 节假日数据及其状态行, 在主线程外读取 (路线图 P8.6). */
    private var holidays by mutableStateOf<HolidayView?>(null)
    private var holidayLines by mutableStateOf<List<String>>(emptyList())
    private var holidayRevision by mutableIntStateOf(0)
    private var holidayCheck: CancelToken? = null
    private var holidayChecking by mutableStateOf(false)

    /**
     * The ringtone page without "Default" (T85); the choice is applied to the stored defaults, which the page may have
     * changed by deleting the default ringtone.
     * zh-CN: 不带 "默认" 的铃声页 (T85); 所选项写入存储的默认值, 铃声页删除默认铃声时可能已改动过它.
     */
    private val soundPicker = registerForActivityResult(RingtonePicker()) { choice ->
        val source = choice?.source ?: return@registerForActivityResult
        saveDefaults(ringDefaults.get().copy(sound = source))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        store = AppUpdateStore.of(this)
        updates = AppUpdateController(store, BuildConfig.VERSION_NAME)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        // Ringtones may have been imported or deleted meanwhile (P3.3). zh-CN: 期间可能导入或删除了铃声 (P3.3).
        resumed++
    }

    override fun onDestroy() {
        if (isFinishing) {
            updates.cancel()
            holidayCheck?.cancel()
        }
        super.onDestroy()
    }

    override fun holdsAppearance(): Boolean = dialog != null || updates.state != null

    @Composable
    override fun Content() {
        val message = pendingMessage
        LaunchedEffect(message) {
            if (message != null) {
                snackbar.showSnackbar(message)
                pendingMessage = null
            }
        }
        LaunchedEffect(resumed) {
            val app = applicationContext
            val (loaded, tones) = withContext(Dispatchers.IO) {
                ringDefaults.get() to runCatching { SystemRingtones.list(app) + AlarmRuntime.get(app).ringtones.list() }.getOrDefault(emptyList())
            }
            defaults = loaded
            ringtones = tones
        }
        LaunchedEffect(resumed, holidayRevision) {
            val app = applicationContext
            val loaded = withContext(Dispatchers.IO) {
                HolidayView.load(app)?.let { view ->
                    val regions = runCatching {
                        AlarmRuntime.get(app).repository.list().filter { it.enabled }.mapNotNull { it.spec.holidays?.region }
                    }.getOrDefault(emptyList())
                    view to statusLines(app, view, listOf<String?>(null) + regions)
                }
            }
            holidays = loaded?.first
            holidayLines = loaded?.second.orEmpty()
        }
        val scroll = rememberScrollState()
        ChimeScreen(title = stringResource(R.string.settings_title), onBack = ::finish, snackbarHostState = snackbar) { padding ->
            // A saved choice bumps the revision and rebuilds the rows from the stored values. zh-CN: 保存后修订号增加, 各行按存储值重建.
            key(revision) {
                Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scroll).testTag("chime.settings")) {
                    AppearanceGroup()
                    DefaultsGroup()
                    ChallengesGroup()
                    TimersGroup()
                    BackgroundGroup()
                    FormatGroup()
                    HolidayGroup()
                    AboutGroup()
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        Dialogs()
        UpdateDialogs(updates, onOpenRelease = ::open, onReleaseHistory = { startActivity(Intent(this, ReleaseHistoryActivity::class.java)) })
    }

    @Composable
    private fun AppearanceGroup() {
        val preferences = AppearancePreferences.read(this)
        SectionHeader(stringResource(R.string.settings_appearance))
        SettingsRow(stringResource(R.string.settings_language), summary = languageSummary(preferences.language), icon = R.drawable.ic_language_24,
            onClick = { dialog = Dialog.LANGUAGE }, modifier = Modifier.testTag("chime.settings.language"))
        Hairline()
        SettingsRow(stringResource(R.string.settings_night_mode), summary = darkModeSummary(preferences.darkMode), icon = R.drawable.ic_dark_mode_24,
            onClick = { dialog = Dialog.DARK_MODE }, modifier = Modifier.testTag("chime.settings.night"))
        Hairline()
        SettingsRow(stringResource(R.string.settings_theme_color), summary = colorSummary(preferences.color), icon = R.drawable.ic_palette_24,
            onClick = { dialog = Dialog.THEME_COLOR }, modifier = Modifier.testTag("chime.settings.color"))
        Hairline()
        SettingsRow(stringResource(R.string.settings_launcher_icon), summary = stringResource(iconLabel(LauncherIcons.current(this))), icon = R.drawable.ic_apps_24,
            onClick = { dialog = Dialog.LAUNCHER_ICON }, modifier = Modifier.testTag("chime.settings.icon"))
    }

    /**
     * The plugin defaults (T84): what an alarm that leaves an option at "Default" uses when it rings; the rows wait for the
     * stored values.
     * zh-CN: 插件默认值 (T84): 把选项留在 "默认" 的闹钟响铃时使用的值; 各行等存储值读到后才可用.
     */
    @Composable
    private fun DefaultsGroup() {
        val loaded = defaults
        val current = loaded ?: RingDefaults.PROTOCOL
        SectionHeader(stringResource(R.string.settings_alarm_defaults))
        SettingsRow(stringResource(R.string.edit_sound), summary = soundText(current.sound, ringtones), icon = R.drawable.ic_music_note_24,
            enabled = loaded != null && ringtones != null, onClick = { soundPicker.launch(RingtoneRequest(current.sound, null)) },
            modifier = Modifier.testTag("chime.settings.sound"))
        Hairline()
        SwitchRow(stringResource(R.string.edit_vibrate), current.vibrate, { saveDefaults(current.copy(vibrate = it)) }, icon = R.drawable.ic_vibration_24,
            enabled = loaded != null, modifier = Modifier.testTag("chime.settings.vibrate"))
        Hairline()
        SettingsRow(stringResource(R.string.edit_snooze), summary = snoozeText(current.snoozeEnabled, current.snoozeMinutes, current.snoozeMax),
            icon = R.drawable.ic_snooze_24, enabled = loaded != null, onClick = { dialog = Dialog.SNOOZE }, modifier = Modifier.testTag("chime.settings.snooze"))
        Hairline()
        SettingsRow(stringResource(R.string.edit_auto_silence), summary = autoSilenceText(current.autoSilenceMinutes), icon = R.drawable.ic_timer_24,
            enabled = loaded != null, onClick = { dialog = Dialog.AUTO_SILENCE }, modifier = Modifier.testTag("chime.settings.autoSilence"))
        Text(
            stringResource(R.string.settings_defaults_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = RowDefaults.textStart, end = RowDefaults.horizontalPadding, top = 4.dp),
        )
    }

    /**
     * "Dismiss challenges" (roadmap P9.3 / P9.5, T283): the emergency dismiss, on by default; it applies to every alarm
     * with a challenge, kept in the plugin defaults.
     * zh-CN: "关闭挑战" (路线图 P9.3 / P9.5, T283): 紧急关闭, 默认开启; 适用于所有带挑战的闹钟, 存于插件默认值.
     */
    @Composable
    private fun ChallengesGroup() {
        val loaded = defaults
        val current = loaded ?: RingDefaults.PROTOCOL
        SectionHeader(stringResource(R.string.settings_challenges))
        SwitchRow(stringResource(R.string.settings_emergency_dismiss), current.emergencyDismiss, { saveDefaults(current.copy(emergencyDismiss = it)) },
            summary = stringResource(R.string.settings_emergency_dismiss_summary), icon = R.drawable.ic_volume_up_24,
            enabled = loaded != null, modifier = Modifier.testTag("chime.settings.emergencyDismiss"))
    }

    /**
     * "Keep running in background" (roadmap Q23 (b), T100): off by default; a change starts or stops the keep-alive
     * service at once (the screen is in the foreground, so the start is allowed).
     * zh-CN: "保持后台运行" (路线图 Q23 (b), T100): 默认关闭; 修改后立即启动或停止保活服务 (界面在前台, 允许启动).
     */
    @Composable
    private fun BackgroundGroup() {
        SectionHeader(stringResource(R.string.settings_background))
        val settings = remember { KeepAliveService.settings(this) }
        SwitchRow(stringResource(R.string.settings_keep_alive), settings.enabled, { saveKeepAlive(settings, it) },
            summary = stringResource(R.string.settings_keep_alive_summary), icon = R.drawable.ic_power_24,
            modifier = Modifier.testTag("chime.settings.keepAlive"))
    }

    /**
     * "Timers and stopwatch" (roadmap P7.4, T122): the stopwatch notification, on by default; a change shows or removes it
     * at once. Timer notifications always show, since their buttons are the way to reach a running timer from outside.
     * zh-CN: "计时器与秒表" (路线图 P7.4, T122): 秒表通知, 默认开启; 修改后立即显示或移除. 计时器通知始终显示, 因为其按钮是在界面外操作运行中计时器的途径.
     */
    @Composable
    private fun TimersGroup() {
        SectionHeader(stringResource(R.string.settings_timers))
        val shown = remember { TimerNotifications.stopwatchShown(this) }
        SwitchRow(stringResource(R.string.settings_stopwatch_notification), shown, ::saveStopwatchNotification,
            summary = stringResource(R.string.settings_stopwatch_notification_summary), icon = R.drawable.ic_timer_24,
            modifier = Modifier.testTag("chime.settings.stopwatchNotification"))
    }

    private fun saveStopwatchNotification(value: Boolean) {
        if (!TimerNotifications.setStopwatchShown(this, value)) {
            pendingMessage = getString(R.string.settings_save_failed)
            return
        }
        val app = applicationContext
        AlarmRuntime.get(app).executor.execute { TimerNotifications.stopwatch(app, AlarmRuntime.get(app).stopwatch.state()) }
        saved()
    }

    private fun saveKeepAlive(settings: KeepAliveSettings, value: Boolean) {
        if (!settings.setEnabled(value)) {
            pendingMessage = getString(R.string.settings_save_failed)
            return
        }
        val app = applicationContext
        AlarmRuntime.get(app).executor.execute { KeepAliveService.sync(app) }
        saved()
    }

    @Composable
    private fun FormatGroup() {
        SectionHeader(stringResource(R.string.settings_region_format))
        SettingsRow(stringResource(R.string.settings_time_format), summary = timeFormatSummary(TimeFormats.get(this)), icon = R.drawable.ic_schedule_24,
            onClick = { dialog = Dialog.TIME_FORMAT }, modifier = Modifier.testTag("chime.settings.timeFormat"))
        Hairline()
        val view = holidays
        SettingsRow(stringResource(R.string.settings_holiday_region), summary = view?.let { defaultRegionSummary(it) }, icon = R.drawable.ic_beach_access_24,
            enabled = view != null, onClick = { dialog = Dialog.HOLIDAY_REGION }, modifier = Modifier.testTag("chime.settings.holidayRegion"))
    }

    /**
     * Holiday data (roadmap P8.6, T173 / T176): the weekly check and its metered network choice, then the data status
     * (version, source, last check, facts that need attention) with a manual check.
     * zh-CN: 节假日数据 (路线图 P8.6, T173 / T176): 每周检查及其计量网络选项, 然后是数据状态 (版本, 来源, 上次检查, 需要注意的事实) 与手动检查.
     */
    @Composable
    private fun HolidayGroup() {
        val settings = AlarmRuntime.get(applicationContext).holidaySettings
        SectionHeader(stringResource(R.string.holiday_data))
        val auto = settings.autoUpdate
        SwitchRow(stringResource(R.string.settings_holiday_auto_update), auto, { settings.autoUpdate = it; holidaySaved() },
            summary = stringResource(R.string.settings_holiday_auto_update_summary), icon = R.drawable.ic_update_24,
            modifier = Modifier.testTag("chime.settings.holidayAutoUpdate"))
        Hairline()
        SwitchRow(stringResource(R.string.settings_holiday_metered), settings.allowMeteredDownloads, { settings.allowMeteredDownloads = it; holidaySaved() },
            summary = stringResource(R.string.settings_holiday_metered_summary), icon = R.drawable.ic_cloud_24, enabled = auto,
            modifier = Modifier.testTag("chime.settings.holidayMetered"))
        Hairline()
        SettingsRow(stringResource(R.string.settings_holiday_check),
            summary = if (holidayChecking) stringResource(R.string.holiday_checking) else holidayLines.joinToString("\n").ifEmpty { null },
            icon = R.drawable.ic_event_24, enabled = holidays != null && !holidayChecking, onClick = ::checkHolidays,
            modifier = Modifier.testTag("chime.settings.holidayCheck"))
    }

    @Composable
    private fun AboutGroup() {
        SectionHeader(stringResource(R.string.settings_about))
        SettingsRow(stringResource(R.string.settings_version),
            summary = stringResource(R.string.settings_version_value, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, stringResource(R.string.plugin_version_date)),
            icon = R.drawable.ic_info_24, modifier = Modifier.testTag("chime.settings.version"))
        Hairline()
        SettingsRow(stringResource(R.string.release_history_title), summary = stringResource(R.string.settings_release_history_summary), icon = R.drawable.ic_history_24,
            onClick = { startActivity(Intent(this, ReleaseHistoryActivity::class.java)) }, modifier = Modifier.testTag("chime.settings.releaseHistory"))
        Hairline()
        SettingsRow(stringResource(R.string.update_check), summary = stringResource(R.string.settings_update_summary), icon = R.drawable.ic_update_24,
            onClick = updates::checkManually, modifier = Modifier.testTag("chime.settings.update"))
        Hairline()
        val frequency = store.frequency
        SettingsRow(stringResource(R.string.update_automatic), summary = stringResource(frequencyLabel(frequency)), icon = R.drawable.ic_notifications_24,
            onClick = { dialog = Dialog.FREQUENCY }, modifier = Modifier.testTag("chime.settings.frequency"))
        Hairline()
        var metered by remember { mutableStateOf(store.checkOnMetered) }
        SwitchRow(stringResource(R.string.update_metered), metered, { metered = it; store.checkOnMetered = it },
            summary = stringResource(R.string.update_metered_summary), icon = R.drawable.ic_cloud_24, enabled = frequency != UpdateFrequency.OFF,
            modifier = Modifier.testTag("chime.settings.metered"))
        Hairline()
        val ignored = store.ignoredTags.size
        SettingsRow(stringResource(R.string.update_ignored),
            summary = if (ignored == 0) stringResource(R.string.update_ignored_none) else pluralStringResource(R.plurals.update_ignored_count, ignored, ignored),
            icon = R.drawable.ic_close_24, enabled = ignored > 0, onClick = { dialog = Dialog.IGNORED }, modifier = Modifier.testTag("chime.settings.ignored"))
        Hairline()
        SettingsRow(stringResource(R.string.notices_title), summary = stringResource(R.string.settings_notices_summary), icon = R.drawable.ic_license_24,
            onClick = { startActivity(Intent(this, NoticesActivity::class.java)) }, modifier = Modifier.testTag("chime.settings.notices"))
    }

    @Composable
    private fun Dialogs() {
        val close = { dialog = null }
        when (dialog) {
            null -> Unit
            Dialog.LANGUAGE -> {
                val current = AppearancePreferences.read(this)
                ChoiceDialog(
                    title = stringResource(R.string.settings_language),
                    options = AppearancePreferences.LANGUAGES.map { Choice(languageLabel(it), followNote(it == AppearancePreferences.FOLLOW_HOST)) },
                    selectedIndex = AppearancePreferences.LANGUAGES.indexOf(current.language).coerceAtLeast(0),
                    onDismissRequest = close,
                    onConfirm = { index -> close(); saveAppearance(current.copy(language = AppearancePreferences.LANGUAGES[index])) },
                    tag = "chime.dialog.language",
                )
            }
            Dialog.DARK_MODE -> {
                val current = AppearancePreferences.read(this)
                ChoiceDialog(
                    title = stringResource(R.string.settings_night_mode),
                    options = AppearancePreferences.DARK_MODES.map { Choice(darkModeLabel(it), followNote(it == AppearancePreferences.FOLLOW_HOST)) },
                    selectedIndex = AppearancePreferences.DARK_MODES.indexOf(current.darkMode).coerceAtLeast(0),
                    onDismissRequest = close,
                    onConfirm = { index -> close(); saveAppearance(current.copy(darkMode = AppearancePreferences.DARK_MODES[index])) },
                    tag = "chime.dialog.night",
                )
            }
            Dialog.THEME_COLOR -> {
                val current = AppearancePreferences.read(this)
                ThemeColorDialog(
                    currentColor = current.color,
                    hostColor = HostAppearanceReader.cached(this)?.primary ?: AppearancePreferences.DEFAULT_COLOR,
                    onDismissRequest = close,
                    onConfirm = { color -> close(); saveAppearance(current.copy(color = color)) },
                )
            }
            Dialog.LAUNCHER_ICON -> ChoiceDialog(
                title = stringResource(R.string.settings_launcher_icon),
                options = LauncherIconMode.entries.map { mode ->
                    Choice(stringResource(iconLabel(mode)), when (mode) {
                        LauncherIconMode.AUTO -> stringResource(R.string.launcher_icon_auto_note)
                        LauncherIconMode.TRANSPARENT -> stringResource(R.string.launcher_icon_transparent_note)
                        else -> null
                    })
                },
                selectedIndex = LauncherIcons.current(this).ordinal,
                onDismissRequest = close,
                onConfirm = { index ->
                    close()
                    val result = runCatching { LauncherIcons.select(this, LauncherIconMode.entries[index]) }
                    revision++
                    pendingMessage = getString(if (result.isSuccess) R.string.launcher_icon_applied_note else R.string.launcher_icon_failed)
                },
                tag = "chime.dialog.icon",
            )
            Dialog.SNOOZE -> defaults?.let { current ->
                SnoozeDialog(AlarmSnooze(current.snoozeEnabled, current.snoozeMinutes, current.snoozeMax), null, close, { snooze ->
                    close()
                    when {
                        snooze == null -> Unit
                        !snooze.enabled -> saveDefaults(current.copy(snoozeEnabled = false))
                        else -> saveDefaults(
                            current.copy(
                                snoozeEnabled = true,
                                snoozeMinutes = snooze.minutes ?: current.snoozeMinutes,
                                snoozeMax = snooze.max ?: current.snoozeMax,
                            ),
                        )
                    }
                }, tag = "chime.settings.snoozeDialog")
            }
            Dialog.AUTO_SILENCE -> defaults?.let { current ->
                AutoSilenceDialog(current.autoSilenceMinutes, null, close, { minutes ->
                    close()
                    if (minutes != null) saveDefaults(current.copy(autoSilenceMinutes = minutes))
                }, tag = "chime.settings.autoSilenceDialog")
            }
            Dialog.TIME_FORMAT -> {
                val current = TimeFormats.get(this)
                ChoiceDialog(
                    title = stringResource(R.string.settings_time_format),
                    options = TimeFormat.entries.map { Choice(timeFormatLabel(it)) },
                    selectedIndex = current.ordinal,
                    onDismissRequest = close,
                    onConfirm = { index ->
                        close()
                        val next = TimeFormat.entries[index]
                        if (next != current) {
                            if (TimeFormats.set(this, next)) saved() else pendingMessage = getString(R.string.settings_save_failed)
                        }
                    },
                    tag = "chime.dialog.timeFormat",
                )
            }
            Dialog.HOLIDAY_REGION -> holidays?.let { view ->
                RegionDialog(
                    view = view,
                    current = view.region(null)?.code,
                    title = stringResource(R.string.settings_holiday_region),
                    firstLabel = stringResource(R.string.settings_holiday_region_none),
                    firstNote = stringResource(R.string.settings_holiday_region_none_note),
                    onDismiss = close,
                    onConfirm = { region -> close(); saveDefaultRegion(view, region) },
                    tag = "chime.dialog.holidayRegion",
                )
            }
            Dialog.FREQUENCY -> ChoiceDialog(
                title = stringResource(R.string.update_automatic),
                options = UpdateFrequency.entries.map { Choice(stringResource(frequencyLabel(it))) },
                selectedIndex = store.frequency.ordinal,
                onDismissRequest = close,
                onConfirm = { index ->
                    close()
                    val next = UpdateFrequency.entries[index]
                    if (next != store.frequency) {
                        store.frequency = next
                        saved()
                    }
                },
                message = stringResource(R.string.update_frequency_message),
                tag = "chime.dialog.frequency",
            )
            Dialog.IGNORED -> {
                val tags = store.ignoredTags.sorted()
                MultiChoiceDialog(
                    title = stringResource(R.string.update_ignored),
                    message = stringResource(R.string.update_ignored_message),
                    options = tags.map { Choice(it) },
                    selected = emptySet(),
                    onDismissRequest = close,
                    onConfirm = { chosen -> close(); store.stopIgnoring(chosen.map(tags::get)); revision++ },
                    confirmText = stringResource(R.string.update_stop_ignoring),
                    requireOne = true,
                    tag = "chime.dialog.ignored",
                )
            }
        }
    }

    // Actions -------------------------------------------------------------------------------------------------------

    private fun saveAppearance(next: AppearancePreferences) {
        if (next == AppearancePreferences.read(this)) return
        if (next.save(this)) {
            revision++
            applyAppearanceChange()
        } else {
            pendingMessage = getString(R.string.settings_save_failed)
        }
    }

    /**
     * Saves the plugin defaults at once (T84): an alarm may ring right after the change.
     * zh-CN: 立即保存插件默认值 (T84): 修改后可能马上就有闹钟响铃.
     */
    private fun saveDefaults(next: RingDefaults) {
        if (next == defaults) return
        if (ringDefaults.set(next)) {
            defaults = ringDefaults.get()
            saved()
        } else {
            pendingMessage = getString(R.string.settings_save_failed)
        }
    }

    private fun saved() {
        revision++
        pendingMessage = getString(R.string.settings_saved)
    }

    /**
     * Applies a holiday setting (roadmap P8.6): the weekly check follows the switches and the alarms are replanned.
     * zh-CN: 应用节假日设置 (路线图 P8.6): 每周检查跟随开关, 闹钟重新排程.
     */
    private fun holidaySaved() {
        holidaySettingsChanged(applicationContext)
        holidayRevision++
        saved()
    }

    private fun saveDefaultRegion(view: HolidayView, region: String?) {
        val current = view.region(null)?.code
        if (region == current) return
        AlarmRuntime.get(applicationContext).holidaySettings.setDefaultRegion(region)
        holidaySaved()
    }

    /**
     * Checks for newer holiday data now, the same refresh as the weekly check and the script `holidays.refresh`.
     * zh-CN: 立即检查较新的节假日数据, 与每周检查及脚本的 `holidays.refresh` 相同.
     */
    private fun checkHolidays() {
        if (holidayChecking) return
        val cancel = CancelToken()
        holidayCheck = cancel
        holidayChecking = true
        AlarmRuntime.get(applicationContext).refreshHolidays(cancel, null) { result ->
            runOnUiThread {
                if (cancel.isCancelled) return@runOnUiThread
                holidayChecking = false
                holidayCheck = null
                holidayRevision++
                pendingMessage = result.fold(
                    onSuccess = { if (it.updated) getString(R.string.holiday_check_updated, it.info.version) else getString(R.string.holiday_check_up_to_date) },
                    onFailure = { getString(R.string.holiday_check_failed) },
                )
            }
        }
    }

    private fun open(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
    }

    // Texts ---------------------------------------------------------------------------------------------------------

    @Composable
    private fun followNote(isFollow: Boolean): String? =
        if (isFollow && HostAppearanceReader.cached(this) == null) stringResource(R.string.settings_follow_autojs6_unavailable) else null

    @Composable
    private fun languageSummary(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_HOST -> stringResource(R.string.settings_follow_autojs6_summary, languageLabel(appearance.language))
        else -> languageLabel(value)
    }

    @Composable
    private fun darkModeSummary(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_HOST -> stringResource(
            R.string.settings_follow_autojs6_summary,
            stringResource(if (appearance.dark) R.string.settings_dark_mode_dark else R.string.settings_dark_mode_light),
        )
        else -> darkModeLabel(value)
    }

    @Composable
    private fun colorSummary(color: Int?): String =
        if (color == null) stringResource(R.string.settings_follow_autojs6_summary, ThemeColorValue.hex(appearance.primary)) else ThemeColorValue.hex(color)

    @Composable
    private fun languageLabel(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_HOST -> stringResource(R.string.settings_follow_autojs6)
        AppearancePreferences.FOLLOW_SYSTEM -> stringResource(R.string.settings_follow_system)
        "zh-Hans" -> stringResource(R.string.settings_language_zh_hans)
        "zh-Hant-HK" -> stringResource(R.string.settings_language_zh_hant_hk)
        "zh-Hant-TW" -> stringResource(R.string.settings_language_zh_hant_tw)
        "en" -> stringResource(R.string.settings_language_en)
        "fr" -> stringResource(R.string.settings_language_fr)
        "es" -> stringResource(R.string.settings_language_es)
        "ja" -> stringResource(R.string.settings_language_ja)
        "ko" -> stringResource(R.string.settings_language_ko)
        "ru" -> stringResource(R.string.settings_language_ru)
        "ar" -> stringResource(R.string.settings_language_ar)
        else -> Locale.forLanguageTag(value).getDisplayName(Locale.forLanguageTag(value)).ifBlank { value }
    }

    @Composable
    private fun darkModeLabel(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_SYSTEM -> stringResource(R.string.settings_follow_system)
        AppearancePreferences.LIGHT -> stringResource(R.string.settings_dark_mode_light)
        AppearancePreferences.DARK -> stringResource(R.string.settings_dark_mode_dark)
        else -> stringResource(R.string.settings_follow_autojs6)
    }

    /** "Follow system - 24-hour", "12-hour" or "24-hour". zh-CN: "跟随系统 - 24 小时制", "12 小时制" 或 "24 小时制". */
    @Composable
    private fun timeFormatSummary(format: TimeFormat): String = when (format) {
        TimeFormat.SYSTEM -> stringResource(
            R.string.settings_follow_system_summary,
            timeFormatLabel(if (DateFormat.is24HourFormat(this)) TimeFormat.H24 else TimeFormat.H12),
        )
        else -> timeFormatLabel(format)
    }

    @Composable
    private fun timeFormatLabel(format: TimeFormat): String = when (format) {
        TimeFormat.SYSTEM -> stringResource(R.string.settings_follow_system)
        TimeFormat.H12 -> stringResource(R.string.settings_time_format_12)
        TimeFormat.H24 -> stringResource(R.string.settings_time_format_24)
    }

    /**
     * The default holiday region with its facts ("China, 2008 - 2027, with makeup workdays"), or "No default region".
     * zh-CN: 默认节假日地区及其事实 ("中国, 2008 - 2027 年, 含补班日"), 或 "不设默认地区".
     */
    @Composable
    private fun defaultRegionSummary(view: HolidayView): String {
        val ref = view.region(null) ?: return stringResource(R.string.settings_holiday_region_none)
        return regionText(ref, LocalConfiguration.current.locales[0]) + "\n" + regionFacts(this, ref.region)
    }

    @StringRes
    private fun frequencyLabel(frequency: UpdateFrequency): Int = when (frequency) {
        UpdateFrequency.OFF -> R.string.update_frequency_off
        UpdateFrequency.TWELVE_HOURS -> R.string.update_frequency_12h
        UpdateFrequency.DAILY -> R.string.update_frequency_daily
        UpdateFrequency.WEEKLY -> R.string.update_frequency_weekly
    }

    @StringRes
    private fun iconLabel(mode: LauncherIconMode): Int = when (mode) {
        LauncherIconMode.LIGHT -> R.string.launcher_icon_light
        LauncherIconMode.DARK -> R.string.launcher_icon_dark
        LauncherIconMode.AUTO -> R.string.launcher_icon_auto
        LauncherIconMode.TRANSPARENT -> R.string.launcher_icon_transparent
    }
}
