package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.ChimeScreen
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.RowDefaults
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.SectionHeader
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.SettingsRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The permissions page (roadmap P3.4, T82), reached from the alarm list banner and from `alarms.openPlugin('permissions')`
 * (T81). Every row shows its state and what does not work without it, and opens its system page: exact alarms (API
 * 31 - 32), notifications (the runtime request on API 33+ until the system stops showing it, then the notification
 * settings; the ring channel settings when only that channel is off), the full screen intent (API 34+), the battery
 * optimization list, the background restriction (API 28+, app details) and the vendor autostart page (guidance only).
 * The state is read again on every resume.
 * zh-CN: 权限页 (路线图 P3.4, T82), 从闹钟列表横幅与 `alarms.openPlugin('permissions')` (T81) 进入. 每行显示状态与缺少它时什么不能用, 并打开对应
 * 的系统页面: 精确闹钟 (API 31 - 32), 通知 (API 33+ 申请运行时权限, 直到系统不再显示申请, 之后打开通知设置; 只有响铃通知类别关闭时打开该类别的设置),
 * 全屏通知 (API 34+), 电池优化列表, 后台限制 (API 28+, 应用详情) 与厂商自启动页 (只作引导). 每次恢复时重新读取状态.
 */
class PermissionsActivity : StandaloneActivity() {

    private var requests by mutableIntStateOf(0)

    private val notificationRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Log.i(Permissions.TAG, "notification request granted=$granted")
        requests++
    }

    @Composable
    override fun Content() {
        PermissionsScreen(requests = requests, onBack = ::finish, onOpen = ::open)
    }

    private fun open(row: PermissionRow, snapshot: PermissionSnapshot): Boolean {
        if (row == PermissionRow.NOTIFICATIONS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && snapshot.requestsNotifications) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            // The system stops showing the request after the second denial; the first request has no rationale yet.
            // zh-CN: 第二次拒绝后系统不再显示申请; 首次申请时还没有 rationale.
            val preferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            if (!preferences.getBoolean(KEY_REQUESTED, false) || shouldShowRequestPermissionRationale(permission)) {
                preferences.edit { putBoolean(KEY_REQUESTED, true) }
                Log.i(Permissions.TAG, "open ${row.tag} -> runtime request")
                notificationRequest.launch(permission)
                return true
            }
        }
        return Permissions.open(this, row, snapshot)
    }

    private companion object {
        const val PREFERENCES = "ui-permissions"
        const val KEY_REQUESTED = "notificationsRequested"
    }
}

@Composable
private fun PermissionsScreen(requests: Int, onBack: () -> Unit, onOpen: (PermissionRow, PermissionSnapshot) -> Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var snapshot by remember { mutableStateOf<PermissionSnapshot?>(null) }
    var resumes by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) resumes++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(resumes, requests) {
        snapshot = withContext(Dispatchers.IO) { Permissions.read(context) }
    }
    val failed = stringResource(R.string.permissions_open_failed)
    ChimeScreen(title = stringResource(R.string.permissions_title), onBack = onBack, snackbarHostState = snackbar) { padding ->
        val current = snapshot
        if (current == null) {
            Box(Modifier.padding(padding).fillMaxSize())
        } else {
            val open: (PermissionRow) -> Unit = { row -> if (!onOpen(row, current)) scope.launch { snackbar.showSnackbar(failed) } }
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("chime.permissions.list"),
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                item(key = "intro") {
                    Text(
                        text = stringResource(R.string.permissions_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = RowDefaults.horizontalPadding, vertical = 8.dp),
                    )
                }
                group(R.string.permissions_section_ringing, RINGING_ROWS, current, open)
                group(R.string.permissions_section_background, BACKGROUND_ROWS, current, open)
            }
        }
    }
}

private val RINGING_ROWS = listOf(PermissionRow.EXACT_ALARM, PermissionRow.NOTIFICATIONS, PermissionRow.FULL_SCREEN)
private val BACKGROUND_ROWS = listOf(PermissionRow.BATTERY, PermissionRow.BACKGROUND, PermissionRow.AUTOSTART)

private fun LazyListScope.group(@StringRes title: Int, rows: List<PermissionRow>, snapshot: PermissionSnapshot, open: (PermissionRow) -> Unit) {
    val visible = rows.filter { it in snapshot.rows }
    if (visible.isEmpty()) return
    item(key = "header.$title") { SectionHeader(stringResource(title)) }
    visible.forEachIndexed { index, row ->
        item(key = row.tag) {
            if (index > 0) Hairline()
            PermissionItem(row, snapshot, open)
        }
    }
}

@Composable
private fun PermissionItem(row: PermissionRow, snapshot: PermissionSnapshot, open: (PermissionRow) -> Unit) {
    val state = snapshot.state(row)
    SettingsRow(
        title = stringResource(titleOf(row)),
        summary = stringResource(summaryOf(row, snapshot)),
        icon = iconOf(row),
        summaryColor = if (state == RowState.MISSING) MaterialTheme.colorScheme.error else Color.Unspecified,
        onClick = if (snapshot.actionable(row)) ({ open(row) }) else null,
        modifier = Modifier.testTag("chime.permissions.${row.tag}"),
    )
}

/** The title of [row]; also the names the alarm list banner joins. zh-CN: [row] 的标题; 也是闹钟列表横幅连接的名称. */
@StringRes
internal fun titleOf(row: PermissionRow): Int = when (row) {
    PermissionRow.EXACT_ALARM -> R.string.permissions_exact_alarm
    PermissionRow.NOTIFICATIONS -> R.string.permissions_notifications
    PermissionRow.FULL_SCREEN -> R.string.permissions_full_screen
    PermissionRow.BATTERY -> R.string.permissions_battery
    PermissionRow.BACKGROUND -> R.string.permissions_background
    PermissionRow.AUTOSTART -> R.string.permissions_autostart
}

@DrawableRes
private fun iconOf(row: PermissionRow): Int = when (row) {
    PermissionRow.EXACT_ALARM -> R.drawable.ic_alarm_24
    PermissionRow.NOTIFICATIONS -> R.drawable.ic_notifications_24
    PermissionRow.FULL_SCREEN -> R.drawable.ic_fullscreen_24
    PermissionRow.BATTERY -> R.drawable.ic_battery_saver_24
    PermissionRow.BACKGROUND -> R.drawable.ic_tune_24
    PermissionRow.AUTOSTART -> R.drawable.ic_power_24
}

@StringRes
private fun summaryOf(row: PermissionRow, snapshot: PermissionSnapshot): Int {
    val state = snapshot.state(row)
    return when (row) {
        PermissionRow.EXACT_ALARM -> when (state) {
            RowState.NOT_NEEDED -> R.string.permissions_not_needed
            RowState.OK -> if (snapshot.sdk >= PermissionSnapshot.API_USE_EXACT_ALARM) R.string.permissions_allowed_at_install else R.string.permissions_allowed
            else -> R.string.permissions_exact_alarm_missing
        }
        PermissionRow.NOTIFICATIONS -> when {
            state == RowState.OK -> R.string.permissions_allowed
            !snapshot.notifications -> R.string.permissions_notifications_missing
            else -> R.string.permissions_notifications_channel_off
        }
        PermissionRow.FULL_SCREEN -> when (state) {
            RowState.NOT_NEEDED -> R.string.permissions_not_needed
            RowState.OK -> R.string.permissions_allowed
            else -> R.string.permissions_full_screen_missing
        }
        PermissionRow.BATTERY -> if (state == RowState.OK) R.string.permissions_battery_unrestricted else R.string.permissions_battery_optimized
        PermissionRow.BACKGROUND -> when (state) {
            RowState.NOT_NEEDED -> R.string.permissions_not_needed
            RowState.OK -> R.string.permissions_background_unrestricted
            else -> R.string.permissions_background_restricted
        }
        PermissionRow.AUTOSTART -> R.string.permissions_autostart_guide
    }
}
