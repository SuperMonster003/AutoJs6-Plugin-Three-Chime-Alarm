package io.github.supermonster003.autojs6.plugin.three.chime.alarm.holiday

import android.content.Context
import android.content.res.AssetManager
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayCatalog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayData
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidaySource
import java.util.Locale

/**
 * The holiday settings of the plugin (roadmap P8.2 / P8.3, T176, device-protected like the alarms): the default region
 * (guessed once from the system locale, then kept), the weekly automatic update check (on by default), whether that check
 * may use a metered network (off by default, the switch and default of 3-Macaw Speech `allowMeteredDownloads`) and the time
 * of the last successful check. The settings screen (roadmap P8.6) edits them; a change of the default region or of the
 * check switches is applied through [io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime.holidaySettingsChanged].
 * zh-CN: 插件的节假日设置 (路线图 P8.2 / P8.3, T176, 与闹钟一样放在设备加密存储): 默认地区 (首次从系统语言区域推断一次, 之后保持), 每周自动检查
 * 更新 (默认开启), 该检查能否使用计量网络 (默认关闭, 开关与默认值同 3-Macaw Speech 的 `allowMeteredDownloads`), 以及上次成功检查的时间. 设置页
 * (路线图 P8.6) 编辑这些值; 默认地区或检查开关的改动经 AlarmRuntime.holidaySettingsChanged 生效.
 */
class HolidaySettings(context: Context) {

    private val preferences = context.createDeviceProtectedStorageContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    /**
     * The default region, or null when there is none: the stored choice, else the country of [locale] when [catalog] has
     * it, which is then stored so a later change of the system language does not move alarms to another calendar.
     * zh-CN: 默认地区, 没有时为 null: 已保存的选择, 否则取 [locale] 的国家 ([catalog] 中有该国家时), 并随即保存, 以免之后更改系统语言时闹钟换到另一份日历.
     */
    fun defaultRegion(catalog: HolidayCatalog, locale: Locale = Locale.getDefault()): String? {
        if (preferences.contains(KEY_DEFAULT_REGION)) return preferences.getString(KEY_DEFAULT_REGION, null)?.ifEmpty { null }
        val inferred = HolidayData.inferRegion(catalog, locale) ?: return null
        preferences.edit().putString(KEY_DEFAULT_REGION, inferred).apply()
        return inferred
    }

    /** Stores [region] (null: no default region on purpose). zh-CN: 保存 [region] (null 表示有意不设默认地区). */
    fun setDefaultRegion(region: String?) {
        preferences.edit().putString(KEY_DEFAULT_REGION, region.orEmpty()).apply()
    }

    var autoUpdate: Boolean
        get() = preferences.getBoolean(KEY_AUTO_UPDATE, true)
        set(value) = preferences.edit().putBoolean(KEY_AUTO_UPDATE, value).apply()

    var allowMeteredDownloads: Boolean
        get() = preferences.getBoolean(KEY_ALLOW_METERED, false)
        set(value) = preferences.edit().putBoolean(KEY_ALLOW_METERED, value).apply()

    var lastCheckAt: Long?
        get() = preferences.getLong(KEY_LAST_CHECK_AT, 0L).takeIf { it > 0L }
        set(value) = preferences.edit().putLong(KEY_LAST_CHECK_AT, value ?: 0L).apply()

    companion object {
        const val PREFERENCES = "holiday-settings"
        const val KEY_DEFAULT_REGION = "defaultRegion"
        const val KEY_AUTO_UPDATE = "autoUpdate"
        const val KEY_ALLOW_METERED = "allowMeteredDownloads"
        const val KEY_LAST_CHECK_AT = "lastCheckAt"
    }
}

/**
 * The holiday snapshot bundled in `assets/holiday-calendars` (roadmap P8.2, `.python/sync_holiday_snapshot.py`).
 * zh-CN: 内置于 `assets/holiday-calendars` 的节假日快照 (路线图 P8.2, `.python/sync_holiday_snapshot.py`).
 */
class AndroidAssetSource(private val assets: AssetManager) : HolidaySource {

    private val names: Set<String> by lazy { assets.list(DIRECTORY)?.toSet().orEmpty() }

    override fun read(name: String): ByteArray? = if (name in names) assets.open("$DIRECTORY/$name").use { it.readBytes() } else null

    override fun has(name: String): Boolean = name in names

    companion object {
        const val DIRECTORY = "holiday-calendars"
    }
}
