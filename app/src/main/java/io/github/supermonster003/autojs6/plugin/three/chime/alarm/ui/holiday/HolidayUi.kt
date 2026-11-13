package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.holiday

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayCatalog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayData
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayJudge
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday.HolidayRegionRef
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.Choice
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.ChimeDialog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.ChoiceRow
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.DialogDefaults
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.RowDefaults
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.format.TimeFormats
import java.text.Collator
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * What the screens show of the holiday data (roadmap P8.6): the regions of the catalog, the default region, the
 * version and source of the data in use (T173) and the judgement for previews. Read with [load] off the main thread,
 * since the first use opens the data and verifies its signature.
 * zh-CN: 界面显示的节假日数据 (路线图 P8.6): 目录中的地区, 默认地区, 正在使用的数据的版本与来源 (T173), 以及预览用的判定. 用 [load] 在主线程之外读取,
 * 因为首次使用会打开数据并校验签名.
 */
internal class HolidayView(
    val regions: List<HolidayCatalog.Region>,
    val defaultRegion: String?,
    val info: HolidayData.Info,
    val downloaded: Boolean,
    val lastCheckAt: Long?,
    private val data: HolidayData,
) {

    /** The judgement of the rule engine for this data. zh-CN: 基于这份数据的规则引擎判定. */
    val judge: HolidayJudge get() = data

    /** [code] (null: the default region) canonical, or null when the data does not know it. zh-CN: 规范化的 [code] (null 为默认地区), 数据不认识时为 null. */
    fun region(code: String?): HolidayRegionRef? = runCatching { data.resolve(code) }.getOrNull()

    /** See [HolidayData.issues]. zh-CN: 见 [HolidayData.issues]. */
    fun issues(regions: Collection<String?>, year: Int): List<HolidayData.Issue> = data.issues(regions, year)

    /**
     * True when the ring at [at] falls in an estimated year (T162) of [region] (null: the default region), read in [zone].
     * zh-CN: [at] 那次响铃落在 [region] (null 为默认地区) 的推算年份 (T162) 时为真, 按 [zone] 读取日期.
     */
    fun estimated(at: Long, region: String?, zone: ZoneId): Boolean =
        data.day(Instant.ofEpochMilli(at).atZone(zone).toLocalDate(), region)?.estimated == true

    companion object {

        private const val TAG = "ChimeHolidays"

        fun load(context: Context): HolidayView? = try {
            val runtime = AlarmRuntime.get(context)
            val data = runtime.holidays
            val set = data.set
            HolidayView(set.catalog.regions, runtime.holidaySettings.defaultRegion(set.catalog), data.info(), set.directory != null, runtime.holidaySettings.lastCheckAt, data)
        } catch (e: Exception) {
            Log.w(TAG, "holiday data unavailable", e)
            null
        }
    }
}

/** The name of [region] in [locale], else its catalog name. zh-CN: [region] 在 [locale] 下的名称, 否则用目录中的名称. */
internal fun regionName(region: HolidayCatalog.Region, locale: Locale): String {
    val name = runCatching { Locale.Builder().setRegion(region.code).build().getDisplayCountry(locale) }.getOrNull()
    return if (name.isNullOrEmpty() || name.equals(region.code, ignoreCase = true)) region.name else name
}

/** "China" or "United States - California" for a canonical region. zh-CN: 规范地区的显示名, 如 "中国" 或 "美国 - California". */
internal fun regionText(ref: HolidayRegionRef, locale: Locale): String {
    val country = regionName(ref.region, locale)
    val sub = ref.subdivision ?: return country
    return "$country - ${ref.region.subdivisions.firstOrNull { it.code == sub }?.name ?: sub}"
}

/**
 * The years of [region], whether it has makeup workdays and from which year it is estimated (roadmap P8.6).
 * zh-CN: [region] 的数据年份, 是否含补班日, 以及自哪一年起为推算 (路线图 P8.6).
 */
internal fun regionFacts(context: Context, region: HolidayCatalog.Region): String = buildList {
    add(context.getString(R.string.holiday_region_years, region.firstYear, region.lastYear))
    if (region.hasMakeupWorkdays) add(context.getString(R.string.holiday_region_makeup))
    region.estimatedFrom?.let { add(context.getString(R.string.holiday_region_estimated_from, it)) }
}.joinToString(", ")

/** One fact of [HolidayData.issues] in the language of the user (T173). zh-CN: 以用户语言表述的一条 [HolidayData.issues] (T173). */
internal fun issueText(context: Context, issue: HolidayData.Issue, name: (String) -> String): String = when (issue) {
    is HolidayData.Issue.NoData -> context.getString(R.string.holiday_issue_no_data, name(issue.region))
    is HolidayData.Issue.MissingYear -> context.getString(R.string.holiday_issue_missing_year, name(issue.region), issue.year, issue.firstYear, issue.lastYear)
    is HolidayData.Issue.Estimated -> context.getString(R.string.holiday_issue_estimated, name(issue.region), issue.from)
}

/**
 * The update status of the data (T173): its version and source, the last check, and the facts for [regions] (null:
 * the default region) this year.
 * zh-CN: 数据的更新状态 (T173): 版本与来源, 上次检查, 以及 [regions] (null 为默认地区) 今年的相关事实.
 */
internal fun statusLines(context: Context, view: HolidayView, regions: Collection<String?>, now: Long = System.currentTimeMillis()): List<String> {
    val locale = context.resources.configuration.locales[0]
    val source = context.getString(if (view.downloaded) R.string.holiday_data_downloaded else R.string.holiday_data_bundled)
    val head = context.getString(R.string.holiday_data_summary, view.info.version, source)
    val checked = view.lastCheckAt?.let { context.getString(R.string.holiday_data_checked, TimeFormats.dateTimeOf(context, it)) }
        ?: context.getString(R.string.holiday_data_never_checked)
    val year = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).year
    val name = { code: String -> view.region(code)?.let { regionText(it, locale) } ?: code }
    return listOf(head, checked) + view.issues(regions, year).map { issueText(context, it, name) }
}

/**
 * The holiday data card at the top of the alarm list while an alarm filters by holidays (roadmap P8.6, T173); a tap
 * opens the settings. Facts that need attention tint its border.
 * zh-CN: 有闹钟使用节假日过滤时, 闹钟列表顶部的节假日数据卡片 (路线图 P8.6, T173); 点按打开设置. 有需要注意的事实时边框着色.
 */
@Composable
internal fun HolidayStatusCard(lines: List<String>, attention: Boolean, onClick: () -> Unit, tag: String = "chime.home.holidays") {
    val accent = if (attention) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag(tag),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_event_24),
                contentDescription = null,
                tint = if (attention) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.holiday_data), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                lines.forEachIndexed { index, line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (index >= 2) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("$tag.line.$index"),
                    )
                }
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
 * A holiday region (roadmap P8.6): the first row gives null ([firstLabel], such as "use the default region"), then the
 * countries of the catalog by their names in the language of the user, searchable by name or code; a selected country
 * with subdivisions lists them below it.
 * zh-CN: 选择节假日地区 (路线图 P8.6): 第一行给出 null ([firstLabel], 例如 "跟随默认地区"), 然后是目录中的国家, 按用户语言的名称排列, 可按名称或代码搜索;
 * 选中有下级地区的国家时在其下列出下级地区.
 */
@Composable
internal fun RegionDialog(
    view: HolidayView,
    current: String?,
    title: String,
    firstLabel: String,
    firstNote: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    tag: String,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var pending by rememberSaveable { mutableStateOf(current.orEmpty()) }
    var query by rememberSaveable { mutableStateOf("") }
    val all = remember(view, locale) {
        val collator = Collator.getInstance(locale)
        view.regions.map { it to regionName(it, locale) }.sortedWith { a, b -> collator.compare(a.second, b.second) }
    }
    val needle = query.trim()
    val shown = remember(all, needle) {
        if (needle.isEmpty()) all
        else all.filter { (region, name) -> name.contains(needle, ignoreCase = true) || region.name.contains(needle, ignoreCase = true) || region.code.equals(needle, ignoreCase = true) }
    }
    val country = pending.substringBefore('-')
    // The list holds about two hundred regions sorted by name, so it opens at the selected one.
    // zh-CN: 列表按名称排列约两百个地区, 因此打开时定位到已选的那一个.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = shown.indexOfFirst { it.first.code == country }.coerceAtLeast(0))
    ChimeDialog(
        title = title,
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.ui_dialog_confirm),
        onConfirm = { onConfirm(pending.ifEmpty { null }) },
        dismissText = stringResource(R.string.ui_dialog_cancel),
        scrollable = false,
        tag = tag,
    ) {
        ChoiceRow(Choice(firstLabel, note = firstNote), selected = pending.isEmpty(), onSelect = { pending = "" }, modifier = Modifier.testTag("$tag.first"))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = { Text(stringResource(R.string.holiday_region_search)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = DialogDefaults.contentPadding, vertical = 8.dp).testTag("$tag.search"),
        )
        LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 360.dp).testTag("$tag.list"), state = listState) {
            items(shown, key = { it.first.code }) { (region, name) ->
                Column {
                    ChoiceRow(
                        Choice(name, note = "${region.code}, ${regionFacts(context, region)}"),
                        selected = pending == region.code,
                        onSelect = { pending = region.code },
                        modifier = Modifier.testTag("$tag.region.${region.code}"),
                    )
                    if (country == region.code && region.subdivisions.isNotEmpty()) {
                        region.subdivisions.forEach { sub ->
                            val code = "${region.code}-${sub.code}"
                            ChoiceRow(
                                Choice(sub.name, note = code),
                                selected = pending == code,
                                onSelect = { pending = code },
                                modifier = Modifier.padding(start = 24.dp).testTag("$tag.region.$code"),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Tells the weekly check and the scheduler about a change of the holiday settings, off the main thread. zh-CN: 在主线程外通知节假日设置的改动. */
internal fun holidaySettingsChanged(context: Context) {
    val runtime = AlarmRuntime.get(context)
    runtime.executor.execute { runtime.holidaySettingsChanged() }
}
