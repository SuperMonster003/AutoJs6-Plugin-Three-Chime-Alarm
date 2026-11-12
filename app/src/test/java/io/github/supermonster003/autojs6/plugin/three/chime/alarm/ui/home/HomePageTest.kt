package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home

import org.autojs.plugin.alarm.api.AlarmPluginActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P7.4 (T139, T123): the pages of the bottom navigation carry the names of `alarms.openPlugin(page)`, other
 * names pick no page, and the alarm list keeps request code 0 of its pending intent from before 1.1.0.
 * zh-CN: 路线图 P7.4 (T139, T123): 底部导航的页面使用 `alarms.openPlugin(page)` 的名称, 其他名称不选择页面, 闹钟列表的 PendingIntent 保持 1.1.0 之前的请求码 0.
 */
class HomePageTest {

    @Test
    fun pagesCarryTheOpenPluginNames() {
        assertEquals(HomePage.ALARMS, HomePage.of(AlarmPluginActions.PAGE_ALARMS))
        assertEquals(HomePage.TIMERS, HomePage.of(AlarmPluginActions.PAGE_TIMERS))
        assertEquals(HomePage.STOPWATCH, HomePage.of(AlarmPluginActions.PAGE_STOPWATCH))
        assertNull(HomePage.of(null))
        assertNull(HomePage.of(AlarmPluginActions.PAGE_PERMISSIONS))
        assertNull(HomePage.of(AlarmPluginActions.PAGE_SETTINGS))
        assertNull(HomePage.of("Timers"))
        assertTrue(HomePage.entries.all { it.key in AlarmPluginActions.PAGES })
        assertEquals(AlarmPluginActions.EXTRA_PAGE, HomeIntents.EXTRA_PAGE)
    }

    @Test
    fun theAlarmListKeepsItsRequestCode() {
        assertEquals(listOf(0, 1, 2), listOf(HomePage.ALARMS, HomePage.TIMERS, HomePage.STOPWATCH).map { it.ordinal })
    }
}
