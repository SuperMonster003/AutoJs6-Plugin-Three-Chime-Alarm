package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions

/**
 * The rows of the permissions page (roadmap P3.4) in display order; [tag] names the row in test tags and logs.
 * zh-CN: 权限页 (路线图 P3.4) 的各行, 按显示顺序; [tag] 是该行在测试标签与日志中的名字.
 */
internal enum class PermissionRow(val tag: String) {
    EXACT_ALARM("exactAlarm"),
    NOTIFICATIONS("notifications"),
    FULL_SCREEN("fullScreen"),
    BATTERY("battery"),
    BACKGROUND("background"),
    AUTOSTART("autostart"),
}

/** The state a row shows. zh-CN: 一行显示的状态. */
internal enum class RowState {
    /** Allowed, or not restricted. zh-CN: 已允许, 或不受限制. */
    OK,

    /** Missing: alarms do not ring, or ring without their screen (T82). zh-CN: 缺失: 闹钟不响, 或响铃时没有界面 (T82). */
    MISSING,

    /** Recommended only: some systems stop the plugin in the background. zh-CN: 仅为建议: 部分系统会在后台停止插件. */
    ADVISED,

    /** This Android version needs nothing. zh-CN: 此 Android 版本无需设置. */
    NOT_NEEDED,

    /** The system does not report the state (vendor autostart): guidance only. zh-CN: 系统不提供状态 (厂商自启动): 只作引导. */
    UNKNOWN,
}

/**
 * What the permissions page and the alarm list banner show (roadmap P3.4, T82), read again on every resume. The exact
 * alarm row reads the same `canScheduleExact()` as `capabilities().permissions.exactAlarm` (T60); notifications count
 * as missing when the app's notifications or its ring channel are off (T69); the background restriction (API 28+) is
 * missing too, as the system then ignores the ringing foreground service. Battery optimization is advice only: the
 * alarm clock wake-ups are exempt from Doze. [autostart] is the vendor whose autostart page the row opens, or null on
 * systems without one (the row is hidden).
 * zh-CN: 权限页与闹钟列表横幅显示的内容 (路线图 P3.4, T82), 每次恢复时重新读取. 精确闹钟一行与 `capabilities().permissions.exactAlarm` 读同一个
 * `canScheduleExact()` (T60); 应用通知或响铃通知类别关闭时通知算作缺失 (T69); 后台限制 (API 28+) 也算缺失, 此时系统会忽略响铃前台服务. 电池优化只是
 * 建议: 闹钟时钟唤醒不受 Doze 推迟. [autostart] 是该行打开其自启动页的厂商, 没有这类页面的系统为 null (隐藏该行).
 */
internal data class PermissionSnapshot(
    val sdk: Int,
    val exactAlarm: Boolean,
    val notifications: Boolean,
    val ringChannel: Boolean,
    val fullScreen: Boolean,
    val batteryUnrestricted: Boolean,
    val backgroundRestricted: Boolean,
    val autostart: AutostartVendor?,
) {

    /** The visible rows in display order. zh-CN: 按显示顺序的可见行. */
    val rows: List<PermissionRow>
        get() = PermissionRow.entries.filter { it != PermissionRow.AUTOSTART || autostart != null }

    /** The rows the alarm list banner reports (T82). zh-CN: 闹钟列表横幅报告的行 (T82). */
    val missing: List<PermissionRow>
        get() = rows.filter { state(it) == RowState.MISSING }

    fun state(row: PermissionRow): RowState = when (row) {
        PermissionRow.EXACT_ALARM -> when {
            sdk < API_EXACT_ALARM -> RowState.NOT_NEEDED
            exactAlarm -> RowState.OK
            else -> RowState.MISSING
        }
        PermissionRow.NOTIFICATIONS -> if (notifications && ringChannel) RowState.OK else RowState.MISSING
        PermissionRow.FULL_SCREEN -> when {
            sdk < API_FULL_SCREEN -> RowState.NOT_NEEDED
            fullScreen -> RowState.OK
            else -> RowState.MISSING
        }
        PermissionRow.BATTERY -> if (batteryUnrestricted) RowState.OK else RowState.ADVISED
        PermissionRow.BACKGROUND -> when {
            sdk < API_BACKGROUND_RESTRICTION -> RowState.NOT_NEEDED
            backgroundRestricted -> RowState.MISSING
            else -> RowState.OK
        }
        PermissionRow.AUTOSTART -> RowState.UNKNOWN
    }

    /**
     * Whether tapping [row] opens a system page: the exact alarm setting exists on API 31 - 32 only (API 33+ grants
     * USE_EXACT_ALARM at install, so only a missing grant opens the app details); the full screen setting from API 34.
     * zh-CN: 点按 [row] 是否打开系统页面: 精确闹钟设置只存在于 API 31 - 32 (API 33+ 安装时授予 USE_EXACT_ALARM, 只有缺失时才打开应用详情); 全屏设置自
     * API 34 起.
     */
    fun actionable(row: PermissionRow): Boolean = when (row) {
        PermissionRow.EXACT_ALARM -> sdk in API_EXACT_ALARM until API_USE_EXACT_ALARM || state(row) == RowState.MISSING
        PermissionRow.FULL_SCREEN -> sdk >= API_FULL_SCREEN
        PermissionRow.BACKGROUND -> sdk >= API_BACKGROUND_RESTRICTION
        PermissionRow.NOTIFICATIONS, PermissionRow.BATTERY, PermissionRow.AUTOSTART -> true
    }

    /** Whether the notification row asks the runtime permission (API 33+) instead of opening the settings. zh-CN: 通知一行是否申请运行时权限 (API 33+) 而非打开设置. */
    val requestsNotifications: Boolean
        get() = sdk >= API_POST_NOTIFICATIONS && !notifications

    companion object {
        const val API_BACKGROUND_RESTRICTION = 28
        const val API_EXACT_ALARM = 31
        const val API_USE_EXACT_ALARM = 33
        const val API_POST_NOTIFICATIONS = 33
        const val API_FULL_SCREEN = 34
    }
}

/**
 * Vendors whose systems keep a per-app autostart switch, with the pages known to hold it (tried in order; the row
 * falls back to the app details when none opens). The system reports no state for it, so the row only guides.
 * zh-CN: 系统带有按应用的自启动开关的厂商, 以及已知放置该开关的页面 (依次尝试; 都打不开时退回应用详情). 系统不报告其状态, 因此该行只作引导.
 */
internal enum class AutostartVendor(private val brands: Set<String>, val pages: List<Pair<String, String>>) {
    XIAOMI(
        setOf("xiaomi", "redmi", "poco"),
        listOf("com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity"),
    ),
    OPPO(
        setOf("oppo", "oneplus", "realme"),
        listOf(
            "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.oplus.safecenter" to "com.oplus.safecenter.startupapp.StartupAppListActivity",
            "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
        ),
    ),
    VIVO(
        setOf("vivo", "iqoo"),
        listOf(
            "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
        ),
    ),
    HUAWEI(
        setOf("huawei"),
        listOf(
            "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity",
        ),
    ),
    HONOR(
        setOf("honor"),
        listOf("com.hihonor.systemmanager" to "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
    ),
    ASUS(
        setOf("asus"),
        listOf("com.asus.mobilemanager" to "com.asus.mobilemanager.autostart.AutoStartActivity"),
    ),
    ;

    companion object {
        /** The vendor of a device by its manufacturer, else its brand; null elsewhere. zh-CN: 按制造商 (其次品牌) 判断设备厂商; 其他为 null. */
        fun of(manufacturer: String?, brand: String?): AutostartVendor? {
            fun find(name: String?) = name?.trim()?.lowercase()?.let { key -> entries.firstOrNull { key in it.brands } }
            return find(manufacturer) ?: find(brand)
        }
    }
}
