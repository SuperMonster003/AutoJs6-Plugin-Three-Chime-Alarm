package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.format

import android.content.Context
import android.content.SharedPreferences
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.text.format.DateFormat
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The clock format of the plugin's screens (roadmap P3.5): the system setting, or 12 or 24 hours chosen in the plugin.
 * zh-CN: 插件界面的时钟格式 (路线图 P3.5): 跟随系统, 或在插件中选择 12 小时或 24 小时.
 */
enum class TimeFormat(val key: String) {
    SYSTEM("system"),
    H12("12"),
    H24("24");

    companion object {
        fun of(key: String?): TimeFormat = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/**
 * Time texts of the alarm list, the edit page and the ring screen. The choice lives in device-protected storage, so the
 * ring screen reads it before the first unlock too; the patterns come from the locale (`getBestDateTimePattern`), so
 * every language keeps its own order and day period marks. Remaining times use ICU `MeasureFormat` (roadmap P3.1).
 * zh-CN: 闹钟列表, 编辑页与响铃界面的时间文字. 选择保存在设备加密存储中, 因此响铃界面在首次解锁前也能读到; 模式取自区域 (`getBestDateTimePattern`),
 * 各语言保留各自的顺序与上下午标记. 剩余时间使用 ICU `MeasureFormat` (路线图 P3.1).
 */
object TimeFormats {

    private const val PREFERENCES = "ui-format"
    private const val KEY_TIME_FORMAT = "timeFormat"

    private fun preferences(context: Context): SharedPreferences =
        context.createDeviceProtectedStorageContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun get(context: Context): TimeFormat = TimeFormat.of(runCatching { preferences(context).getString(KEY_TIME_FORMAT, null) }.getOrNull())

    /** Saves [format]; false when the write failed. zh-CN: 保存 [format]; 写入失败时返回 false. */
    fun set(context: Context, format: TimeFormat): Boolean = runCatching {
        preferences(context).edit().putString(KEY_TIME_FORMAT, format.key).commit()
    }.getOrDefault(false)

    fun is24Hour(context: Context): Boolean = when (get(context)) {
        TimeFormat.SYSTEM -> DateFormat.is24HourFormat(context)
        TimeFormat.H12 -> false
        TimeFormat.H24 -> true
    }

    private fun locale(context: Context): Locale = context.resources.configuration.locales[0]

    private fun formatter(context: Context, skeleton: String): DateTimeFormatter {
        val locale = locale(context)
        return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
    }

    /** "07:30" or "7:30 AM". zh-CN: "07:30" 或 "上午7:30". */
    fun time(context: Context, hour: Int, minute: Int, second: Int = 0): String {
        val skeleton = (if (is24Hour(context)) "Hm" else "hm") + if (second != 0) "s" else ""
        return formatter(context, skeleton).format(java.time.LocalTime.of(hour, minute, second))
    }

    /** The time of day of [millis] in [zone]. zh-CN: [millis] 在 [zone] 中的时刻. */
    fun timeOf(context: Context, millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val local = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()
        return time(context, local.hour, local.minute, local.second)
    }

    /**
     * The time of day of [millis] in [zone] to the minute, as the ring screen and its notification show it.
     * zh-CN: [millis] 在 [zone] 中精确到分钟的时刻, 用于响铃界面与响铃通知.
     */
    fun minuteOf(context: Context, millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val local = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()
        return time(context, local.hour, local.minute)
    }

    /**
     * The date and time of day of [millis] in [zone] to the minute ("Nov 6, 07:30"), as the missed alarm notification
     * shows it.
     * zh-CN: [millis] 在 [zone] 中的日期与精确到分钟的时刻 ("11月6日 07:30"), 用于错过闹钟通知.
     */
    fun dateTimeOf(context: Context, millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        formatter(context, "MMMd" + if (is24Hour(context)) "Hm" else "hm").format(Instant.ofEpochMilli(millis).atZone(zone))

    /** "Today", "Tomorrow" or "Wed, Nov 12" for the day of [millis]. zh-CN: [millis] 所在日期: "今天", "明天" 或 "11月12日 周三". */
    fun dayOf(context: Context, millis: Long, zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): String {
        val day = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        return when (day) {
            today -> context.getString(R.string.home_day_today)
            today.plusDays(1) -> context.getString(R.string.home_day_tomorrow)
            else -> formatter(context, if (day.year == today.year) "MMMEd" else "yMMMEd").format(day)
        }
    }

    /** A date as on the edit page ("Wed, Nov 12, 2026"). zh-CN: 编辑页上的日期. */
    fun date(context: Context, date: LocalDate): String = formatter(context, "yMMMEd").format(date)

    /**
     * The time left until [at], rounded up to the minute: days and hours from one day on, else hours and minutes.
     * zh-CN: 距 [at] 的剩余时间, 向上取整到分钟: 一天以上显示天与小时, 否则显示小时与分钟.
     */
    fun remaining(context: Context, at: Long, now: Long = System.currentTimeMillis()): String {
        val minutes = maxOf(1L, (at - now + 59_999L) / 60_000L)
        val days = minutes / (24 * 60)
        val hours = minutes / 60 % 24
        val rest = minutes % 60
        val measures = when {
            days > 0 -> listOfNotNull(Measure(days, MeasureUnit.DAY), hours.takeIf { it > 0 }?.let { Measure(it, MeasureUnit.HOUR) })
            hours > 0 -> listOfNotNull(Measure(hours, MeasureUnit.HOUR), rest.takeIf { it > 0 }?.let { Measure(it, MeasureUnit.MINUTE) })
            else -> listOf(Measure(rest, MeasureUnit.MINUTE))
        }
        return MeasureFormat.getInstance(locale(context), MeasureFormat.FormatWidth.SHORT).formatMeasures(*measures.toTypedArray())
    }
}
