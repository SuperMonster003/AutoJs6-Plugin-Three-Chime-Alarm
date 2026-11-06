package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions

import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ThreeChimeAlarmApplication
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring.RingService

/**
 * Reading the permission state and opening the system pages of the permissions page (roadmap P3.4). Every page is
 * started from the plugin's own screen, tried in order, and falls back to the app details; a page that does not exist
 * or is not exported on a system only moves on to the next one.
 * zh-CN: 权限页 (路线图 P3.4) 读取权限状态并打开系统页面. 每个页面都从插件自己的界面启动, 依次尝试, 最后退回应用详情; 某系统上不存在或未导出的页面
 * 只会转到下一个.
 */
internal object Permissions {

    const val TAG = "ChimePermissions"

    /** Name of the last page a row opened, for the device probe. zh-CN: 最近一次某行打开的页面名, 供设备探针读取. */
    @Volatile
    var lastOpened: String? = null
        private set

    /** Reads the state off the main thread (the first use builds the alarm runtime). zh-CN: 在主线程之外读取状态 (首次使用会建立闹钟运行时). */
    fun read(context: Context): PermissionSnapshot {
        val app = context.applicationContext
        val compat = NotificationManagerCompat.from(app)
        val ringChannel = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            compat.getNotificationChannel(ThreeChimeAlarmApplication.CHANNEL_RING)?.importance != NotificationManager.IMPORTANCE_NONE
        val background = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            app.getSystemService(ActivityManager::class.java).isBackgroundRestricted
        return PermissionSnapshot(
            sdk = Build.VERSION.SDK_INT,
            exactAlarm = AlarmRuntime.get(app).port.canScheduleExact(),
            notifications = compat.areNotificationsEnabled(),
            ringChannel = ringChannel,
            fullScreen = RingService.fullScreenAllowed(app),
            overlay = Settings.canDrawOverlays(app),
            batteryUnrestricted = app.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(app.packageName),
            backgroundRestricted = background,
            autostart = AutostartVendor.of(Build.MANUFACTURER, Build.BRAND),
        )
    }

    /** The pages of [row] in the order they are tried, before the app details. zh-CN: [row] 的页面, 按尝试顺序, 应用详情之前. */
    fun pagesOf(context: Context, row: PermissionRow, snapshot: PermissionSnapshot): List<Intent> {
        val packageName = context.packageName
        val packageUri = Uri.fromParts("package", packageName, null)
        return when (row) {
            PermissionRow.EXACT_ALARM -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                listOf(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri))
            } else {
                emptyList()
            }
            PermissionRow.NOTIFICATIONS -> when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O -> emptyList()
                snapshot.notifications && !snapshot.ringChannel -> listOf(
                    Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        .putExtra(Settings.EXTRA_CHANNEL_ID, ThreeChimeAlarmApplication.CHANNEL_RING),
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
                )
                else -> listOf(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            }
            PermissionRow.FULL_SCREEN -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                listOf(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri))
            } else {
                emptyList()
            }
            PermissionRow.VENDOR_BANNER -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                listOf(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            } else {
                emptyList()
            }
            PermissionRow.OVERLAY -> listOf(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri),
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
            )
            PermissionRow.BATTERY -> listOf(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            PermissionRow.BACKGROUND -> emptyList()
            PermissionRow.AUTOSTART -> snapshot.autostart?.pages.orEmpty().map { (pkg, cls) -> Intent().setComponent(ComponentName(pkg, cls)) }
        }
    }

    /** Opens the page of [row]; false when not even the app details opened. zh-CN: 打开 [row] 的页面; 连应用详情也打不开时为 false. */
    fun open(activity: Activity, row: PermissionRow, snapshot: PermissionSnapshot): Boolean {
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null))
        val pages = pagesOf(activity, row, snapshot) + details
        pages.forEachIndexed { index, intent ->
            val name = intent.component?.flattenToShortString() ?: intent.action.orEmpty()
            try {
                activity.startActivity(intent)
                val fallback = index == pages.lastIndex && pages.size > 1
                Log.i(TAG, "open ${row.tag} -> $name${if (fallback) " (fallback)" else ""}")
                lastOpened = name
                return true
            } catch (e: ActivityNotFoundException) {
                Log.i(TAG, "open ${row.tag}: no $name")
            } catch (e: SecurityException) {
                Log.i(TAG, "open ${row.tag}: $name refused (${e.message})")
            }
        }
        Log.w(TAG, "open ${row.tag}: nothing opened")
        lastOpened = null
        return false
    }
}
