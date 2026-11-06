package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.RepositoryFiles.android
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.RepositoryFiles.androidOrNull
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.RepositoryFiles.child
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.RepositoryFiles.children
import org.autojs.plugin.alarm.api.AlarmPluginActions
import org.autojs.plugin.common.api.PluginActions
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Keeps `AndroidManifest.xml` and [ThreeChimeAlarmContract] from drifting apart: the host discovers the plugin through
 * the Manifest (Wake activity, INFO and ALARM services), while the services and the tests use the Kotlin constants
 * (roadmap 4.5, T4, T6, D21).
 * zh-CN: 防止 `AndroidManifest.xml` 与 [ThreeChimeAlarmContract] 漂移: 宿主经 Manifest 发现插件 (Wake Activity, INFO 与 ALARM
 * 服务), 服务与测试则使用 Kotlin 常量 (路线图 4.5, T4, T6, D21).
 */
class ManifestContractTest {

    private val manifest: Element by lazy { RepositoryFiles.xml("app/src/main/AndroidManifest.xml") }

    private val application: Element get() = manifest.child("application")

    @Test
    fun `the permissions are exactly the plugin permission, the roadmap T4 set and the Q16 (a) network pair`() {
        assertEquals(
            listOf(
                PLUGIN_PERMISSION,
                "android.permission.USE_EXACT_ALARM",
                "android.permission.SCHEDULE_EXACT_ALARM",
                "android.permission.USE_FULL_SCREEN_INTENT",
                "android.permission.POST_NOTIFICATIONS",
                "android.permission.VIBRATE",
                "android.permission.WAKE_LOCK",
                "android.permission.RECEIVE_BOOT_COMPLETED",
                "android.permission.FOREGROUND_SERVICE",
                RING_SERVICE_PERMISSION,
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE",
            ),
            manifest.children("uses-permission").map { it.android("name") },
        )
        val exact = manifest.children("uses-permission").single { it.android("name") == "android.permission.SCHEDULE_EXACT_ALARM" }
        assertEquals("SCHEDULE_EXACT_ALARM only below API 33 (roadmap T4)", "32", exact.android("maxSdkVersion"))
        val permissions = manifest.children("uses-permission").map { it.android("name") }
        assertTrue("no storage permission (roadmap T4)", permissions.none { it.contains("STORAGE") || it.contains("MEDIA") })
        assertEquals(
            "only the update check uses the network (roadmap Q16 (a), T4)",
            listOf("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE"),
            permissions.filter { it.contains("INTERNET") || it.contains("NETWORK") },
        )
        assertTrue("no uses-sdk override: SDK levels come from version.properties", manifest.children("uses-sdk").isEmpty())
        assertTrue("no feature requirements", manifest.children("uses-feature").isEmpty())
    }

    @Test
    fun `package queries name the host and the text-to-speech engines only`() {
        val queries = manifest.child("queries")
        assertEquals(listOf("org.autojs.autojs6"), queries.children("package").map { it.android("name") })
        assertEquals(
            listOf("android.intent.action.TTS_SERVICE"),
            queries.children("intent").flatMap { it.children("action") }.map { it.android("name") },
        )
    }

    @Test
    fun `the application disables backups and uses the generated title and system icon`() {
        assertEquals("false", application.android("allowBackup"))
        assertEquals("false", application.android("fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", application.android("dataExtractionRules"))
        assertEquals("@string/app_name", application.android("label"))
        assertEquals("true", application.android("supportsRtl"))
        assertEquals("@mipmap/ic_icon_studio_application", application.android("icon"))
        assertEquals("@mipmap/ic_icon_studio_application", application.android("roundIcon"))
        assertEquals(".ThreeChimeAlarmApplication", application.android("name"))
        assertNull("native libraries stay uncompressed through the packaging option (roadmap T34)", application.androidOrNull("extractNativeLibs"))
    }

    @Test
    fun `application meta-data names the wake activity, the author and the native page alignment`() {
        val metaData = application.children("meta-data").associate { it.android("name") to it.android("value") }
        assertEquals(
            mapOf(
                "org.autojs.plugin.WAKE_ACTIVITY" to ".WakeActivity",
                "org.autojs.plugin.info.AUTHOR" to "@string/plugin_author",
                // Roadmap T34: the Compose graphics-path library is the only native code and is 16 KB page aligned.
                PluginCapabilityKeys.NATIVE_PAGE_ALIGNMENT to "16384",
            ),
            metaData,
        )
    }

    @Test
    fun `the wake activity follows the activation contract`() {
        val wake = activity(".WakeActivity")
        assertEquals("true", wake.android("exported"))
        assertEquals("true", wake.android("excludeFromRecents"))
        assertEquals("true", wake.android("finishOnTaskLaunch"))
        assertEquals(PLUGIN_PERMISSION, wake.android("permission"))
        assertEquals("@android:style/Theme.NoDisplay", wake.android("theme"))
        assertFilter(wake, action = "org.autojs.plugin.action.WAKE", category = DEFAULT_CATEGORY)
    }

    @Test
    fun `the activities are the wake, home, permissions, settings, edit, open, clock intent and ring screens`() {
        assertEquals(
            listOf(
                ".WakeActivity",
                ".ui.home.HomeActivity",
                ".ui.permissions.PermissionsActivity",
                ".ui.settings.SettingsActivity",
                ".ui.release.ReleaseHistoryActivity",
                ".ui.about.NoticesActivity",
                ".ui.about.LegalDocumentActivity",
                ".ui.edit.EditAlarmActivity",
                ".ui.edit.ClockDraftActivity",
                ".ui.OpenPluginActivity",
                ".intent.ClockIntentActivity",
                ".intent.ClockDialogActivity",
                ".ring.RingActivity",
            ),
            application.children("activity").map { it.android("name") },
        )
        assertEquals("false", activity(".ui.home.HomeActivity").android("exported"))
        // Roadmap P3.4: the permissions page opens from the alarm list banner and the OPEN trampoline only.
        val permissions = activity(".ui.permissions.PermissionsActivity")
        assertEquals("false", permissions.android("exported"))
        assertEquals("singleTop", permissions.android("launchMode"))
        // Roadmap P3.5: the settings screen and the pages it opens are internal as well.
        for (name in listOf(".ui.settings.SettingsActivity", ".ui.release.ReleaseHistoryActivity", ".ui.about.NoticesActivity", ".ui.about.LegalDocumentActivity")) {
            assertEquals(name, "false", activity(name).android("exported"))
        }
        // Roadmap T81: the window-less trampoline of alarms.openPlugin answers the frozen OPEN action without a permission.
        val open = activity(".ui.OpenPluginActivity")
        assertEquals("true", open.android("exported"))
        assertEquals("true", open.android("excludeFromRecents"))
        assertEquals("no task affinity: as the root of the plugin's task a second call would only bring it to the front", "", open.android("taskAffinity"))
        assertNull(open.androidOrNull("permission"))
        assertEquals("@android:style/Theme.NoDisplay", open.android("theme"))
        assertFilter(open, action = AlarmPluginActions.OPEN, category = DEFAULT_CATEGORY)
        // Roadmap P3.1: the edit page opens from the alarm list; its SET_ALARM copy stays out of the recent apps in a task of its own.
        assertEquals("false", activity(".ui.edit.EditAlarmActivity").android("exported"))
        val draft = activity(".ui.edit.ClockDraftActivity")
        assertEquals("false", draft.android("exported"))
        assertEquals("true", draft.android("excludeFromRecents"))
        assertEquals("", draft.android("taskAffinity"))
        // The chooser of the clock intents (roadmap P2.6) opens only from ClockIntentActivity.
        val dialog = activity(".intent.ClockDialogActivity")
        assertEquals("false", dialog.android("exported"))
        assertEquals("true", dialog.android("excludeFromRecents"))
        val ring = activity(".ring.RingActivity")
        assertEquals("false", ring.android("exported"))
        assertEquals("true", ring.android("showWhenLocked"))
        assertEquals("true", ring.android("turnScreenOn"))
        assertEquals("true", ring.android("directBootAware"))
    }

    @Test
    fun `four launcher aliases point at the home screen and only the automatic one is enabled`() {
        val aliases = application.children("activity-alias")
        assertEquals(
            listOf(
                ".launcher.AdaptiveLightIconAlias" to "@mipmap/ic_launcher_system_light",
                ".launcher.AdaptiveDarkIconAlias" to "@mipmap/ic_launcher_system",
                ".launcher.AdaptiveAutoIconAlias" to "@mipmap/ic_launcher_system_auto",
                ".launcher.TransparentIconAlias" to "@mipmap/ic_launcher",
            ),
            aliases.map { it.android("name") to it.android("icon") },
        )
        aliases.forEach { alias ->
            assertEquals(".ui.home.HomeActivity", alias.android("targetActivity"))
            assertEquals(alias.android("icon"), alias.android("roundIcon"))
            assertEquals("true", alias.android("exported"))
            assertEquals(listOf("android.intent.action.MAIN"), alias.child("intent-filter").children("action").map { it.android("name") })
            assertEquals(listOf(LAUNCHER_CATEGORY), alias.child("intent-filter").children("category").map { it.android("name") })
        }
        assertEquals(listOf(".launcher.AdaptiveAutoIconAlias"), aliases.filter { it.android("enabled") == "true" }.map { it.android("name") })
    }

    @Test
    fun `the clock intent activity answers the alarm intents behind SET_ALARM`() {
        val clock = activity(".intent.ClockIntentActivity")
        assertEquals("true", clock.android("exported"))
        assertEquals(SET_ALARM_PERMISSION, clock.android("permission"))
        assertEquals("@android:style/Theme.NoDisplay", clock.android("theme"))
        val filters = clock.children("intent-filter")
        assertEquals(
            listOf(
                "android.intent.action.SET_ALARM",
                "android.intent.action.SHOW_ALARMS",
                "android.intent.action.DISMISS_ALARM",
                "android.intent.action.SNOOZE_ALARM",
            ),
            filters.flatMap { filter -> filter.children("action").map { it.android("name") } },
        )
        filters.forEach { filter -> assertEquals(listOf(DEFAULT_CATEGORY), filter.children("category").map { it.android("name") }) }
    }

    @Test
    fun `the INFO and ALARM services match the discovery constants and the ring service stays internal`() {
        assertEquals(
            listOf(".ring.RingService", ".service.ThreeChimeAlarmInfoService", ".service.ThreeChimeAlarmService"),
            application.children("service").map { it.android("name") },
        )
        val info = service(".service.ThreeChimeAlarmInfoService")
        assertFilter(info, action = PluginActions.INFO, category = ThreeChimeAlarmContract.INFO_CATEGORY)
        assertEquals(ThreeChimeAlarmContract.INFO_ACTION, PluginActions.INFO)
        assertRequiresHostVersion(info)

        val capability = service(".service.ThreeChimeAlarmService")
        assertFilter(capability, action = ThreeChimeAlarmContract.SERVICE_ACTION, category = ThreeChimeAlarmContract.SERVICE_CATEGORY)
        assertRequiresHostVersion(capability)

        listOf(info, capability).forEach { service ->
            assertEquals("true", service.android("exported"))
            assertEquals(PLUGIN_PERMISSION, service.android("permission"))
            assertNull("services run in the plugin's default process", service.androidOrNull("process"))
            assertNull("discovery services are not foreground services", service.androidOrNull("foregroundServiceType"))
        }

        val ring = service(".ring.RingService")
        assertEquals("false", ring.android("exported"))
        assertEquals("true", ring.android("directBootAware"))
        // Roadmap T6 (measured in P0.2): the type and its permission change together.
        assertEquals(RING_SERVICE_TYPE, ring.android("foregroundServiceType"))
    }

    @Test
    fun `every exported component requires a permission`() {
        val components = listOf("activity", "service", "receiver", "provider").flatMap { application.children(it) }
        val exported = components.filter { it.androidOrNull("exported") == "true" }
        val unprotected = exported.filter { it.androidOrNull("permission") != PLUGIN_PERMISSION }
        // The OPEN trampoline only shows the plugin's own screens (roadmap T81); the clock intents follow the AOSP clock
        // (roadmap D21): any app holding SET_ALARM may call them.
        assertEquals(listOf(".ui.OpenPluginActivity", ".intent.ClockIntentActivity"), unprotected.map { it.android("name") })
        assertEquals("com.android.alarm.permission.SET_ALARM", activity(".intent.ClockIntentActivity").android("permission"))
        assertTrue("no providers are declared", application.children("provider").isEmpty())
        // Launcher aliases are the only components exported without a permission: the launcher must start them.
        application.children("activity-alias").forEach { assertNull(it.androidOrNull("permission")) }
    }

    @Test
    fun `the scheduling receivers are private, direct boot aware and hear the T8 broadcasts`() {
        val receivers = application.children("receiver").associateBy { it.android("name") }
        assertEquals(
            listOf(".schedule.AlarmTriggerReceiver", ".schedule.AlarmRescheduleReceiver", ".ui.appearance.LauncherIconUpdateReceiver"),
            receivers.keys.toList(),
        )
        listOf(".schedule.AlarmTriggerReceiver", ".schedule.AlarmRescheduleReceiver").map(receivers::getValue).forEach {
            assertEquals("false", it.android("exported"))
            assertEquals("true", it.android("directBootAware"))
        }
        // The launcher icon choice (roadmap P3.5) is applied again after an update; it needs no direct boot.
        val icon = receivers.getValue(".ui.appearance.LauncherIconUpdateReceiver")
        assertEquals("false", icon.android("exported"))
        assertEquals(listOf("android.intent.action.MY_PACKAGE_REPLACED"), icon.child("intent-filter").children("action").map { it.android("name") })
        assertTrue(receivers.getValue(".schedule.AlarmTriggerReceiver").children("intent-filter").isEmpty())
        assertEquals(
            listOf(
                "android.intent.action.BOOT_COMPLETED",
                "android.intent.action.LOCKED_BOOT_COMPLETED",
                "android.intent.action.TIME_SET",
                "android.intent.action.TIMEZONE_CHANGED",
                "android.intent.action.MY_PACKAGE_REPLACED",
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            ),
            receivers.getValue(".schedule.AlarmRescheduleReceiver").child("intent-filter").children("action").map { it.android("name") },
        )
    }

    private fun activity(name: String): Element = application.children("activity").single { it.android("name") == name }

    private fun service(name: String): Element = application.children("service").single { it.android("name") == name }

    private fun assertFilter(component: Element, action: String, category: String) {
        val filter = component.child("intent-filter")
        assertEquals(listOf(action), filter.children("action").map { it.android("name") })
        assertEquals(listOf(category), filter.children("category").map { it.android("name") })
        assertTrue(filter.children("data").isEmpty())
    }

    private fun assertRequiresHostVersion(service: Element) {
        val metaData = service.children("meta-data").associate { it.android("name") to it.android("value") }
        assertEquals(mapOf(PluginCapabilityKeys.REQUIRES_HOST_VERSION to "@string/plugin_requires_host_version"), metaData)
    }

    private companion object {
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"
        const val SET_ALARM_PERMISSION = "com.android.alarm.permission.SET_ALARM"
        const val DEFAULT_CATEGORY = "android.intent.category.DEFAULT"
        const val LAUNCHER_CATEGORY = "android.intent.category.LAUNCHER"
        const val RING_SERVICE_TYPE = "systemExempted"
        const val RING_SERVICE_PERMISSION = "android.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED"
    }

}
