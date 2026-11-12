package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R

/**
 * The bottom navigation of the home pages (roadmap P7.4, T139): alarms, timers and stopwatch, neutral like the rest of
 * the screens; the selected page uses the accent's container color.
 * zh-CN: 首页各页的底部导航 (路线图 P7.4, T139): 闹钟, 计时器与秒表, 与其他界面一样使用中性色; 选中的页面使用强调色的容器色.
 */
@Composable
internal fun HomeNavigationBar(current: HomePage, onSelect: (HomePage) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = MaterialTheme.colorScheme.onSurface) {
        HomePage.entries.forEach { page ->
            NavigationBarItem(
                selected = page == current,
                onClick = { if (page != current) onSelect(page) },
                icon = { Icon(painterResource(iconOf(page)), contentDescription = null) },
                label = { Text(stringResource(labelOf(page))) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.testTag("chime.nav.${page.key}"),
            )
        }
    }
}

private fun iconOf(page: HomePage): Int = when (page) {
    HomePage.ALARMS -> R.drawable.ic_alarm_24
    HomePage.TIMERS -> R.drawable.ic_hourglass_24
    HomePage.STOPWATCH -> R.drawable.ic_timer_24
}

internal fun labelOf(page: HomePage): Int = when (page) {
    HomePage.ALARMS -> R.string.home_tab_alarms
    HomePage.TIMERS -> R.string.home_tab_timers
    HomePage.STOPWATCH -> R.string.stopwatch_title
}
