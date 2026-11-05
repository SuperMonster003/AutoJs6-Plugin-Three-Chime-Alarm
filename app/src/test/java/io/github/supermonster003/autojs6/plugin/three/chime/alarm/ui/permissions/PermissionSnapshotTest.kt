package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P3.4 (T82): the states, the banner rows and the tappable rows of the permissions page on plain values, per
 * Android version, and the vendors whose autostart page the page opens.
 * zh-CN: 路线图 P3.4 (T82): 以普通值按 Android 版本检查权限页各行的状态, 横幅列出的行与可点按的行, 以及权限页会打开其自启动页的厂商.
 */
class PermissionSnapshotTest {

    private fun snapshot(
        sdk: Int,
        exactAlarm: Boolean = true,
        notifications: Boolean = true,
        ringChannel: Boolean = true,
        fullScreen: Boolean = true,
        batteryUnrestricted: Boolean = true,
        backgroundRestricted: Boolean = false,
        autostart: AutostartVendor? = null,
    ) = PermissionSnapshot(sdk, exactAlarm, notifications, ringChannel, fullScreen, batteryUnrestricted, backgroundRestricted, autostart)

    @Test
    fun `everything allowed shows no banner on every version`() {
        for (sdk in listOf(24, 28, 31, 33, 34, 36)) {
            assertEquals("API $sdk", emptyList<PermissionRow>(), snapshot(sdk).missing)
        }
    }

    @Test
    fun `rows older versions do not have are not needed`() {
        val old = snapshot(24, exactAlarm = false, fullScreen = false, backgroundRestricted = true)
        assertEquals(RowState.NOT_NEEDED, old.state(PermissionRow.EXACT_ALARM))
        assertEquals(RowState.NOT_NEEDED, old.state(PermissionRow.FULL_SCREEN))
        assertEquals(RowState.NOT_NEEDED, old.state(PermissionRow.BACKGROUND))
        assertEquals(emptyList<PermissionRow>(), old.missing)
        assertEquals(RowState.NOT_NEEDED, snapshot(30, exactAlarm = false).state(PermissionRow.EXACT_ALARM))
        assertEquals(RowState.NOT_NEEDED, snapshot(33, fullScreen = false).state(PermissionRow.FULL_SCREEN))
        assertEquals(RowState.NOT_NEEDED, snapshot(27, backgroundRestricted = true).state(PermissionRow.BACKGROUND))
    }

    @Test
    fun `the banner lists every missing row in display order`() {
        val all = snapshot(
            34,
            exactAlarm = false,
            notifications = false,
            fullScreen = false,
            batteryUnrestricted = false,
            backgroundRestricted = true,
            autostart = AutostartVendor.XIAOMI,
        )
        assertEquals(
            listOf(PermissionRow.EXACT_ALARM, PermissionRow.NOTIFICATIONS, PermissionRow.FULL_SCREEN, PermissionRow.BACKGROUND),
            all.missing,
        )
        assertEquals("battery optimization is advice only", RowState.ADVISED, all.state(PermissionRow.BATTERY))
        assertEquals("autostart reports no state", RowState.UNKNOWN, all.state(PermissionRow.AUTOSTART))
    }

    @Test
    fun `notifications are missing when the app or its ring channel is off`() {
        assertEquals(RowState.MISSING, snapshot(33, notifications = false).state(PermissionRow.NOTIFICATIONS))
        assertEquals(RowState.MISSING, snapshot(26, ringChannel = false).state(PermissionRow.NOTIFICATIONS))
        assertEquals(RowState.OK, snapshot(26).state(PermissionRow.NOTIFICATIONS))
    }

    @Test
    fun `the notification row asks the runtime permission from API 33 only`() {
        assertTrue(snapshot(33, notifications = false).requestsNotifications)
        assertFalse(snapshot(32, notifications = false).requestsNotifications)
        assertFalse(snapshot(33).requestsNotifications)
        assertFalse("only the channel is off", snapshot(33, ringChannel = false).requestsNotifications)
    }

    @Test
    fun `tappable rows follow the system pages of each version`() {
        val s31 = snapshot(31)
        assertTrue(s31.actionable(PermissionRow.EXACT_ALARM))
        assertFalse(s31.actionable(PermissionRow.FULL_SCREEN))
        assertTrue(s31.actionable(PermissionRow.BACKGROUND))
        // API 33+ grants USE_EXACT_ALARM at install: only a missing grant opens the app details.
        assertFalse(snapshot(33).actionable(PermissionRow.EXACT_ALARM))
        assertTrue(snapshot(33, exactAlarm = false).actionable(PermissionRow.EXACT_ALARM))
        assertFalse(snapshot(30).actionable(PermissionRow.EXACT_ALARM))
        assertTrue(snapshot(34).actionable(PermissionRow.FULL_SCREEN))
        assertFalse(snapshot(27).actionable(PermissionRow.BACKGROUND))
        for (sdk in listOf(24, 34)) {
            assertTrue(snapshot(sdk).actionable(PermissionRow.NOTIFICATIONS))
            assertTrue(snapshot(sdk).actionable(PermissionRow.BATTERY))
            assertTrue(snapshot(sdk).actionable(PermissionRow.AUTOSTART))
        }
    }

    @Test
    fun `the autostart row shows on known vendors only`() {
        assertEquals(PermissionRow.entries - PermissionRow.AUTOSTART, snapshot(35).rows)
        assertEquals(PermissionRow.entries.toList(), snapshot(35, autostart = AutostartVendor.OPPO).rows)
    }

    @Test
    fun `vendors are found by manufacturer, then by brand`() {
        assertEquals(AutostartVendor.XIAOMI, AutostartVendor.of("Xiaomi", "Redmi"))
        assertEquals(AutostartVendor.XIAOMI, AutostartVendor.of("unknown", " POCO "))
        assertEquals(AutostartVendor.OPPO, AutostartVendor.of("OnePlus", "OnePlus"))
        assertEquals(AutostartVendor.OPPO, AutostartVendor.of("realme", null))
        assertEquals(AutostartVendor.VIVO, AutostartVendor.of("vivo", "iQOO"))
        assertEquals(AutostartVendor.HUAWEI, AutostartVendor.of("HUAWEI", "HUAWEI"))
        assertEquals(AutostartVendor.HONOR, AutostartVendor.of("HONOR", "HONOR"))
        assertEquals(AutostartVendor.ASUS, AutostartVendor.of("asus", "asus"))
        assertNull(AutostartVendor.of("Sony", "Sony"))
        assertNull(AutostartVendor.of("Google", "google"))
        assertNull(AutostartVendor.of(null, null))
    }

    @Test
    fun `every vendor names at least one fully qualified page`() {
        for (vendor in AutostartVendor.entries) {
            assertTrue(vendor.name, vendor.pages.isNotEmpty())
            vendor.pages.forEach { (pkg, cls) ->
                assertTrue("$vendor $pkg", pkg.count { it == '.' } >= 2)
                assertTrue("$vendor $cls", cls.count { it == '.' } >= 3)
            }
        }
    }
}
