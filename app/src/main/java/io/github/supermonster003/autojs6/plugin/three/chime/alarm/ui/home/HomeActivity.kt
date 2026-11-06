package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmChange
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RepeatText
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.TimeZonePolicy
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.ChimeScreen
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.EmptyAction
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.EmptyState
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.MessageDialog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.RowDefaults
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.TopBarAction
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.EditAlarmActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.format.TimeFormat
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.format.TimeFormats
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions.PermissionRow
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions.Permissions
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions.PermissionsActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions.titleOf
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.release.ReleaseHistoryActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.settings.SettingsActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.update.AppUpdateController
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.update.AppUpdateStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.update.UpdateDialogs
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.update.UpdateNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes
import org.autojs.plugin.alarm.api.AlarmLimits
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The alarm list (roadmap P3.1, D7, T18, D20), the launcher target: every alarm with its time, repeat, label and next
 * ring ("in 9 hr, 3 min"), its enable switch, who created and last changed it, and a mark when AutoJs6 scripts are
 * bound to it (tap to unbind, T27). The overflow menu skips or restores the next ring and deletes; a long press starts
 * the multi-select delete. A banner on top opens the permissions page while a permission the alarms need is missing
 * (P3.4, T82). A tap on a row, the add button and the empty state open the edit page (P3.1, T83); the top bar opens the
 * settings, and the list runs the automatic update check when it opens (P3.5, T84). Repository calls run off the main
 * thread; the list and the banner reload on resume, on repository and ring changes, and at every minute (state changes
 * such as a new next ring send no change event).
 * zh-CN: 闹钟列表 (路线图 P3.1, D7, T18, D20), 启动器目标: 每个闹钟的时间, 重复, 标签与下次响铃 ("还有 9 小时 3 分"), 启用开关, 创建与最后修改来源,
 * 以及 AutoJs6 脚本绑定标记 (点击解除绑定, T27). 溢出菜单跳过或恢复下次响铃并删除; 长按进入多选删除. 缺少闹钟所需的权限时, 顶部横幅打开权限页 (P3.4,
 * T82). 点击列表行, 新建按钮与空状态打开编辑页 (P3.1, T83); 顶栏打开设置页, 列表打开时进行自动更新检查 (P3.5, T84). 仓库调用不在主线程; 列表与横幅
 * 在恢复, 仓库与响铃变化以及每分钟时重新加载 (新的下次响铃等状态变化不发变更事件).
 */
class HomeActivity : StandaloneActivity() {

    private lateinit var updates: AppUpdateController

    override fun onCreate(savedInstanceState: Bundle?) {
        updates = AppUpdateController(AppUpdateStore.of(this), BuildConfig.VERSION_NAME)
        super.onCreate(savedInstanceState)
        // The automatic update check runs when the list opens at the chosen frequency (roadmap P3.5, T84), not on a
        // recreation. zh-CN: 自动更新检查在列表打开时按所选频率进行 (路线图 P3.5, T84), 重建时不检查.
        if (savedInstanceState == null) updates.checkAutomaticallyIfDue(UpdateNetwork.of(this))
    }

    override fun onDestroy() {
        if (isFinishing) updates.cancel()
        super.onDestroy()
    }

    override fun holdsAppearance(): Boolean = updates.state != null

    @Composable
    override fun Content() {
        HomeScreen()
        UpdateDialogs(
            updates,
            onOpenRelease = { url -> runCatching { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) } },
            onReleaseHistory = { startActivity(Intent(this, ReleaseHistoryActivity::class.java)) },
        )
    }
}

/** One row of the list. zh-CN: 列表的一行. */
internal data class AlarmItem(val record: AlarmRecord, val repeatText: String?, val bound: Boolean, val ringing: Boolean)

/** The list in display order: by time of day, then by creation. zh-CN: 按显示顺序的列表: 先按钟点, 再按创建时间. */
internal fun loadItems(context: Context): List<AlarmItem> {
    val runtime = AlarmRuntime.get(context)
    val ringing = runtime.rings.ringing().mapTo(HashSet()) { it.id }
    val locale = context.resources.configuration.locales[0]
    val deviceZone = ZoneId.systemDefault()
    return runtime.repository.list().map { record ->
        val repeat = record.spec.repeat?.let { rule -> runCatching { RepeatText.of(rule, record.spec.date?.let(LocalDate::parse), locale) }.getOrNull() }
        AlarmItem(record, repeat, runtime.hints.hasBindings(record.id), record.id in ringing)
    }.sortedWith(compareBy<AlarmItem> { minuteOfDay(it.record, deviceZone) }.thenBy { it.record.createdAt })
}

private fun minuteOfDay(record: AlarmRecord, deviceZone: ZoneId): Int {
    record.spec.time?.let { return it.hour * 60 + it.minute }
    val at = record.spec.at ?: return 0
    val local = Instant.ofEpochMilli(at).atZone(TimeZonePolicy.zoneOf(record.spec, deviceZone)).toLocalTime()
    return local.hour * 60 + local.minute
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var items by remember { mutableStateOf<List<AlarmItem>?>(null) }
    var missing by remember { mutableStateOf(emptyList<PermissionRow>()) }
    var generation by remember { mutableIntStateOf(0) }
    var selection by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var unbinding by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<List<String>?>(null) }
    var timeFormat by remember { mutableStateOf(TimeFormat.SYSTEM) }
    val selecting = selection.isNotEmpty()

    LaunchedEffect(generation) {
        val (loaded, lacking, format) = withContext(Dispatchers.IO) { Triple(loadItems(context), Permissions.read(context).missing, TimeFormats.get(context)) }
        items = loaded
        missing = lacking
        timeFormat = format
        val ids = loaded.mapTo(HashSet()) { it.record.id }
        if (selection.any { it !in ids }) selection = selection.filter { it in ids }
    }
    ReloadTriggers { generation++ }

    BackHandler(enabled = selecting) { selection = emptyList() }

    val failed = stringResource(R.string.home_action_failed)
    val limit = pluralStringResource(R.plurals.home_limit_enabled, AlarmLimits.MAX_ENABLED_ALARMS, AlarmLimits.MAX_ENABLED_ALARMS)
    fun act(block: (AlarmRuntime) -> Unit) = scope.launch {
        val error = withContext(Dispatchers.IO) {
            try {
                block(AlarmRuntime.get(context))
                null
            } catch (e: AlarmException) {
                Log.w(TAG, "action failed: ${e.message}")
                if (e.code == AlarmErrorCodes.LIMIT_EXCEEDED) limit else failed
            }
        }
        generation++
        if (error != null) snackbar.showSnackbar(error)
    }

    val title = if (selecting) pluralStringResource(R.plurals.home_selected_count, selection.size, selection.size) else stringResource(R.string.app_name)
    val newAlarm = stringResource(R.string.edit_new_alarm)
    val create = { context.startActivity(EditAlarmActivity.create(context)) }
    ChimeScreen(
        title = title,
        onBack = if (selecting) ({ selection = emptyList() }) else null,
        snackbarHostState = snackbar,
        floatingActionButton = {
            if (!selecting && items != null) {
                FloatingActionButton(onClick = create, modifier = Modifier.testTag("chime.home.add")) {
                    Icon(painterResource(R.drawable.ic_add_24), contentDescription = newAlarm)
                }
            }
        },
        actions = {
            if (selecting) {
                TopBarAction(R.drawable.ic_select_all_24, stringResource(R.string.home_select_all), "chime.home.selectAll") {
                    selection = items.orEmpty().map { it.record.id }
                }
                TopBarAction(R.drawable.ic_delete_24, stringResource(R.string.home_delete), "chime.home.deleteSelected") {
                    deleting = selection
                }
            } else {
                TopBarAction(R.drawable.ic_settings_24, stringResource(R.string.settings_title), "chime.home.settings") {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                }
            }
        },
    ) { padding ->
        val list = items
        val banner = missing.isNotEmpty() && !selecting
        val openPermissions = { context.startActivity(Intent(context, PermissionsActivity::class.java)) }
        // A clock format chosen in the settings rebuilds the rows (P3.5). zh-CN: 在设置中选择的时钟格式会重建各行 (P3.5).
        key(timeFormat) {
        when {
            list == null -> Box(Modifier.padding(padding).fillMaxSize())
            list.isEmpty() -> Column(Modifier.padding(padding)) {
                if (banner) PermissionBanner(missing, openPermissions)
                EmptyState(
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(R.string.home_empty_body),
                    icon = R.drawable.ic_alarm_24,
                    primary = EmptyAction(newAlarm, create, "chime.home.emptyAdd"),
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("chime.home.list"),
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 88.dp),
            ) {
                if (banner) item(key = "permissionBanner") { PermissionBanner(missing, openPermissions) }
                items(list, key = { it.record.id }) { item ->
                    val id = item.record.id
                    AlarmRow(
                        item = item,
                        selecting = selecting,
                        selected = id in selection,
                        onClick = {
                            if (selecting) selection = if (id in selection) selection - id else selection + id
                            else context.startActivity(EditAlarmActivity.edit(context, id))
                        },
                        onLongClick = { if (!selecting) selection = listOf(id) },
                        onToggle = { enabled -> act { it.repository.setEnabled(id, enabled, AlarmContract.SOURCE_USER) } },
                        onSkip = { act { it.repository.skipNext(id, AlarmContract.SOURCE_USER) } },
                        onCancelSkip = { act { it.repository.cancelSkip(id, AlarmContract.SOURCE_USER) } },
                        onUnbind = { unbinding = id },
                        onDelete = { deleting = listOf(id) },
                    )
                    Hairline()
                }
            }
        }
        }
    }

    unbinding?.let { id ->
        UnbindDialog(
            onDismiss = { unbinding = null },
            onConfirm = {
                unbinding = null
                unbind(context, scope, snackbar, id) { generation++ }
            },
        )
    }
    deleting?.let { ids ->
        val bound = items.orEmpty().any { it.record.id in ids && it.bound }
        MessageDialog(
            title = pluralStringResource(R.plurals.home_delete_title, ids.size, ids.size),
            message = stringResource(if (bound) R.string.home_delete_message_bound else R.string.home_delete_message),
            confirmText = stringResource(R.string.home_delete),
            onDismissRequest = { deleting = null },
            onConfirm = {
                deleting = null
                selection = emptyList()
                act { runtime -> ids.forEach { runtime.repository.remove(it, AlarmContract.SOURCE_USER) } }
            },
            tag = "chime.home.deleteDialog",
        )
    }
}

/**
 * The banner of the missing permissions (roadmap P3.1, P3.4, T82): names them and opens the permissions page; the edit
 * page shows it too (T83).
 * zh-CN: 缺失权限横幅 (路线图 P3.1, P3.4, T82): 列出缺失项并打开权限页; 编辑页也显示它 (T83).
 */
@Composable
internal fun PermissionBanner(missing: List<PermissionRow>, onClick: () -> Unit, tag: String = "chime.home.permissionBanner") {
    val names = missing.map { stringResource(titleOf(it)) }.joinToString(", ")
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag(tag),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_warning_24), contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_permission_banner_title), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    stringResource(R.string.home_permission_banner_summary, names),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                painterResource(R.drawable.ic_chevron_right_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = RowDefaults.CHEVRON_ALPHA),
            )
        }
    }
}

/**
 * Reloads the list on resume, on repository and ring changes and at every minute boundary while the screen is started.
 * zh-CN: 在恢复, 仓库与响铃变化时, 以及界面处于启动状态时的每个整分重新加载列表.
 */
@Composable
private fun ReloadTriggers(reload: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var started by remember { mutableStateOf(false) }
    DisposableEffect(lifecycle) {
        val runtime = AlarmRuntime.get(context)
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        val changed: (AlarmChange) -> Unit = { main.post(reload) }
        val rang: () -> Unit = { main.post(reload) }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> reload()
                Lifecycle.Event.ON_START -> started = true
                Lifecycle.Event.ON_STOP -> started = false
                else -> Unit
            }
        }
        runtime.repository.addListener(changed)
        runtime.rings.addListener(rang)
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            runtime.rings.removeListener(rang)
            runtime.repository.removeListener(changed)
        }
    }
    LaunchedEffect(started) {
        while (started) {
            delay(60_000L - System.currentTimeMillis() % 60_000L)
            reload()
        }
    }
}

private fun unbind(context: Context, scope: CoroutineScope, snackbar: SnackbarHostState, id: String, reload: () -> Unit) {
    AlarmRuntime.get(context).triggers.unbind(id) { result ->
        reload()
        val text = when {
            result.failed.isNotEmpty() -> context.getString(R.string.home_unbind_failed)
            result.removed == 0 -> context.getString(R.string.home_unbind_none)
            else -> context.resources.getQuantityString(R.plurals.home_unbind_done, result.removed, result.removed)
        }
        scope.launch { snackbar.showSnackbar(text) }
    }
}

@Composable
private fun UnbindDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    MessageDialog(
        title = stringResource(R.string.home_unbind_title),
        message = stringResource(R.string.home_unbind_message),
        confirmText = stringResource(R.string.home_unbind_confirm),
        onDismissRequest = onDismiss,
        onConfirm = onConfirm,
        tag = "chime.home.unbindDialog",
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmRow(
    item: AlarmItem,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onSkip: () -> Unit,
    onCancelSkip: () -> Unit,
    onUnbind: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val record = item.record
    val enabled = record.enabled
    val tag = "chime.home.alarm.${record.id}"
    val time = timeText(context, record)
    val selectedText = stringResource(R.string.home_selected)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, role = if (selecting) Role.Checkbox else null)
            .semantics { if (selecting && selected) stateDescription = selectedText }
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecting) {
            Checkbox(checked = selected, onCheckedChange = null, modifier = Modifier.padding(end = 16.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val strong = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            Text(time, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Normal, color = strong, modifier = Modifier.testTag("$tag.time"))
            Text(
                listOfNotNull(scheduleText(context, record, item.repeatText), record.spec.label?.takeIf { it.isNotBlank() }).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = strong,
                modifier = Modifier.testTag("$tag.schedule"),
            )
            statusText(context, item)?.let { status ->
                Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("$tag.status"))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    originText(context, record),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false).testTag("$tag.origin"),
                )
                if (item.bound) {
                    Spacer(Modifier.width(8.dp))
                    BindingMark(enabled = !selecting, onClick = onUnbind, tag = "$tag.bound")
                }
            }
        }
        if (!selecting) {
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.semantics { contentDescription = time }.testTag("$tag.switch"),
            )
            AlarmMenu(item, onSkip, onCancelSkip, onUnbind, onDelete, tag)
        }
    }
}

@Composable
private fun BindingMark(enabled: Boolean, onClick: () -> Unit, tag: String) {
    val label = stringResource(R.string.home_bound)
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 32.dp)
            .combinedClickableIf(enabled, onClick)
            .semantics(mergeDescendants = true) {}
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_link_24), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableIf(enabled: Boolean, onClick: () -> Unit): Modifier =
    if (enabled) combinedClickable(onClick = onClick, role = Role.Button) else this

@Composable
private fun AlarmMenu(item: AlarmItem, onSkip: () -> Unit, onCancelSkip: () -> Unit, onUnbind: () -> Unit, onDelete: () -> Unit, tag: String) {
    var open by remember { mutableStateOf(false) }
    val record = item.record
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.testTag("$tag.menu")) {
            Icon(painterResource(R.drawable.ic_more_vert_24), contentDescription = stringResource(R.string.home_more_actions))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (record.state.skipping) {
                MenuItem(R.string.home_cancel_skip, R.drawable.ic_skip_next_24, "$tag.cancelSkip") { open = false; onCancelSkip() }
            } else if (record.enabled && record.spec.repeat != null && record.state.nextTrigger != null) {
                MenuItem(R.string.home_skip_next, R.drawable.ic_skip_next_24, "$tag.skip") { open = false; onSkip() }
            }
            if (item.bound) MenuItem(R.string.home_unbind_title, R.drawable.ic_link_off_24, "$tag.unbind") { open = false; onUnbind() }
            MenuItem(R.string.home_delete, R.drawable.ic_delete_24, "$tag.delete") { open = false; onDelete() }
        }
    }
}

@Composable
private fun MenuItem(text: Int, icon: Int, tag: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(text)) },
        leadingIcon = { Icon(painterResource(icon), contentDescription = null) },
        onClick = onClick,
        modifier = Modifier.testTag(tag),
    )
}

/** The time of day of [record]: its `time`, or the clock time of its `at` instant. zh-CN: 闹钟的钟点. */
internal fun timeText(context: Context, record: AlarmRecord): String {
    record.spec.time?.let { return TimeFormats.time(context, it.hour, it.minute, it.second) }
    val at = record.spec.at ?: return ""
    return TimeFormats.timeOf(context, at, TimeZonePolicy.zoneOf(record.spec, ZoneId.systemDefault()))
}

/** The repeat text, or the date of a one-shot alarm ("Once" without one). zh-CN: 重复描述, 或单次闹钟的日期 (没有日期时为 "仅一次"). */
internal fun scheduleText(context: Context, record: AlarmRecord, repeatText: String?): String {
    if (repeatText != null) return repeatText
    val date = record.spec.date?.takeUnless { record.state.dateImplicit }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    if (date != null) return TimeFormats.date(context, date)
    record.spec.at?.let { at -> return TimeFormats.date(context, Instant.ofEpochMilli(at).atZone(TimeZonePolicy.zoneOf(record.spec, ZoneId.systemDefault())).toLocalDate()) }
    return context.getString(R.string.home_once)
}

/** Ringing, snoozed, skipped or the next ring with the time left; null for a disabled alarm. zh-CN: 响铃, 贪睡, 跳过或下次响铃. */
internal fun statusText(context: Context, item: AlarmItem, now: Long = System.currentTimeMillis()): String? {
    val record = item.record
    val state = record.state
    if (item.ringing) return context.getString(R.string.home_ringing)
    state.snoozeUntil?.takeIf { it > now }?.let { until ->
        return context.getString(R.string.home_snoozed, TimeFormats.timeOf(context, until), TimeFormats.remaining(context, until, now))
    }
    if (!record.enabled) return null
    val next = state.nextTrigger ?: return context.getString(R.string.home_no_next)
    val nextText = context.getString(R.string.home_next_ring, TimeFormats.dayOf(context, next), TimeFormats.timeOf(context, next), TimeFormats.remaining(context, next, now))
    val skipped = state.skipAt ?: return nextText
    return context.getString(R.string.home_skipped, TimeFormats.dayOf(context, skipped), TimeFormats.timeOf(context, skipped)) + "\n" + nextText
}

/** "Created by: script" or "Created by: script, changed by: you" (T18). zh-CN: 创建与最后修改来源 (T18). */
internal fun originText(context: Context, record: AlarmRecord): String {
    val created = sourceName(context, record.source)
    if (record.updatedBy == record.source || record.updatedAt == record.createdAt) return context.getString(R.string.home_origin, created)
    return context.getString(R.string.home_origin_changed, created, sourceName(context, record.updatedBy))
}

internal fun sourceName(context: Context, source: String): String = context.getString(
    when (source) {
        AlarmContract.SOURCE_SCRIPT -> R.string.source_script
        AlarmContract.SOURCE_TOOL -> R.string.source_tool
        AlarmContract.SOURCE_INTENT -> R.string.source_intent
        else -> R.string.source_user
    },
)

private const val TAG = "ChimeHome"
