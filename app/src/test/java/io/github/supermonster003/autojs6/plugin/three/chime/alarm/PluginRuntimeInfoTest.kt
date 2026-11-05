package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.service.pluginRuntimeFields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pure PluginInfo field mapping and the identity published by the build, the Manifest and the README sources
 * (roadmap 4.5, T19, T20, T25). The Android-backed `toPluginInfo()` mapping is exercised by the device contract test.
 * zh-CN: 纯数据的 PluginInfo 字段映射, 以及构建, Manifest 与 README 源发布的身份 (路线图 4.5, T19, T20, T25). 依赖 Android 的
 * `toPluginInfo()` 映射由设备契约测试覆盖.
 */
class PluginRuntimeInfoTest {

    @Test
    fun `runtime fields keep the identity, the package version, no ABI and the alarm capabilities`() {
        val fields = pluginRuntimeFields(
            name = "3-Chime Alarm",
            description = "Alarms",
            author = "SuperMonster003",
            id = "three-chime-alarm",
            engine = "alarm",
            variant = "default",
            versionName = "1.0.0",
            versionCode = 2L,
            versionDate = "Nov 5, 2026",
        )
        assertEquals("3-Chime Alarm", fields.name)
        assertEquals("Alarms", fields.description)
        assertEquals("@raw/plugin_instruction", fields.instruction)
        assertEquals(ThreeChimeAlarmContract.INSTRUCTION_REFERENCE, fields.instruction)
        assertEquals("SuperMonster003", fields.author)
        assertEquals("three-chime-alarm", fields.id)
        assertEquals("alarm", fields.engine)
        assertEquals("default", fields.variant)
        assertEquals("1.0.0", fields.versionName)
        assertEquals(2L, fields.versionCode)
        assertEquals("Nov 5, 2026", fields.versionDate)
        // Roadmap T20: no native code, so the APK is ABI independent and reports an empty list.
        assertEquals(emptyList<String>(), fields.supportedAbis)
        // Roadmap 4.5: the placeholder floor is the roadmap baseline; P1.3 and P6.4 raise it.
        assertEquals(6640L, fields.requiredHostVersion)
        assertEquals(ThreeChimeAlarmContract.REQUIRED_HOST_VERSION, fields.requiredHostVersion)
        assertEquals(1, fields.contractVersion)
        assertEquals("the skeleton advertises no feature yet (roadmap T25)", emptyList<String>(), fields.features)
    }

    @Test
    fun `identity constants follow roadmap 4_5`() {
        assertEquals("three-chime-alarm", ThreeChimeAlarmContract.PLUGIN_ID)
        assertEquals("alarm", ThreeChimeAlarmContract.ENGINE)
        assertEquals("default", ThreeChimeAlarmContract.VARIANT)
        assertEquals("org.autojs.plugin.INFO", ThreeChimeAlarmContract.INFO_ACTION)
        assertEquals(ThreeChimeAlarmContract.PLUGIN_ID, ThreeChimeAlarmContract.INFO_CATEGORY)
        assertEquals("org.autojs.plugin.ALARM", ThreeChimeAlarmContract.SERVICE_ACTION)
        assertEquals("alarm", ThreeChimeAlarmContract.SERVICE_CATEGORY)
        assertEquals("org.autojs.plugin.alarm.api.IAlarmPlugin", ThreeChimeAlarmContract.CAPABILITY_DESCRIPTOR)
        assertEquals("alarmContractVersion", ThreeChimeAlarmContract.CAPABILITY_CONTRACT_VERSION)
        assertEquals("alarmFeatures", ThreeChimeAlarmContract.CAPABILITY_FEATURES)
        assertEquals(1, ThreeChimeAlarmContract.CONTRACT_VERSION)
    }

    @Test
    fun `the build publishes the same identity as the constants and builds one APK`() {
        val build = RepositoryFiles.text("app/build.gradle.kts")
        assertTrue(build.contains("val globalApplicationId = \"$PACKAGE_NAME\""))
        assertTrue(build.contains("namespace = globalApplicationId"))
        assertTrue(build.contains("applicationId = globalApplicationId"))
        assertTrue(build.contains("resValue(\"string\", \"app_name\", \"3-Chime Alarm\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_name\", \"3-Chime Alarm\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_author\", \"SuperMonster003\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_id\", \"${ThreeChimeAlarmContract.PLUGIN_ID}\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_engine\", \"${ThreeChimeAlarmContract.ENGINE}\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_variant\", \"${ThreeChimeAlarmContract.VARIANT}\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_requires_host_version\", \"\$requiredHostVersion\")"))
        // The minimum host build is changed in the build script, the constant and the README sources together.
        assertTrue(build.contains("val requiredHostVersion = ${ThreeChimeAlarmContract.REQUIRED_HOST_VERSION}\n"))
        // Roadmap T20: one APK, no ABI splits, and the release task rejects native code.
        assertFalse("no ABI splits (roadmap T20)", build.contains("splits {") || build.contains("abi {"))
        assertTrue(build.contains("register(\"appendDigestToReleasedFiles\")"))
        assertTrue(build.contains("it.startsWith(\"lib/\") || it.endsWith(\".so\")"))
        assertTrue(build.contains("ApkVerifier.Builder(apk).build().verify()"))

        val settings = RepositoryFiles.text("settings.gradle.kts")
        assertTrue(settings.contains("rootProject.name = \"autojs6-plugin-three-chime-alarm\""))
    }

    @Test
    fun `the README sources publish the same identity as the constants`() {
        val common = RepositoryFiles.text(".readme/common.json")
        mapOf(
            "plugin_application_id" to PACKAGE_NAME,
            "plugin_id" to ThreeChimeAlarmContract.PLUGIN_ID,
            "plugin_engine" to ThreeChimeAlarmContract.ENGINE,
            "plugin_variant" to ThreeChimeAlarmContract.VARIANT,
            "info_action" to ThreeChimeAlarmContract.INFO_ACTION,
            "info_category" to ThreeChimeAlarmContract.INFO_CATEGORY,
            "discovery_action" to ThreeChimeAlarmContract.SERVICE_ACTION,
            "discovery_category" to ThreeChimeAlarmContract.SERVICE_CATEGORY,
            "required_host_version_code" to ThreeChimeAlarmContract.REQUIRED_HOST_VERSION.toString(),
            "app_name" to "3-Chime Alarm",
        ).forEach { (key, value) ->
            assertTrue(".readme/common.json must publish $key = $value", common.contains("\"$key\": \"$value\""))
        }
    }

    @Test
    fun `the package of the Kotlin sources is the application id`() {
        assertEquals(PACKAGE_NAME, ThreeChimeAlarmContract::class.java.packageName)
    }

    private companion object {
        const val PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.three.chime.alarm"
    }

}
