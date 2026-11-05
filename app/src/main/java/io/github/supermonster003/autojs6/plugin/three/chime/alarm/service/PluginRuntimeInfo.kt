package io.github.supermonster003.autojs6.plugin.three.chime.alarm.service

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Bundle
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ThreeChimeAlarmContract
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.common.api.PluginInfo

/**
 * The PluginInfo fields as plain values, so JVM tests can freeze the mapping without Android.
 * zh-CN: 以纯值表示的 PluginInfo 字段, 使 JVM 测试无需 Android 即可冻结映射.
 */
internal data class PluginRuntimeFields(
    val name: String,
    val description: String,
    val instruction: String,
    val author: String,
    val id: String,
    val engine: String,
    val variant: String,
    val versionName: String,
    val versionCode: Long,
    val versionDate: String,
    /**
     * Always empty: the APK carries no native code (roadmap T20), so it is ABI independent.
     * zh-CN: 恒为空: APK 不含原生代码 (路线图 T20), 与 ABI 无关.
     */
    val supportedAbis: List<String>,
    val requiredHostVersion: Long,
    /** Alarm contract version and features (roadmap T25). zh-CN: 闹钟契约版本与功能集合 (路线图 T25). */
    val contractVersion: Int,
    val features: List<String>,
)

/**
 * Assembles the fields; [id], [engine] and [variant] come from the build resources, which the JVM tests keep equal to
 * [ThreeChimeAlarmContract] and the README sources.
 * zh-CN: 组装字段; [id], [engine] 与 [variant] 来自构建资源, JVM 测试保证其与 [ThreeChimeAlarmContract] 及 README 源一致.
 */
internal fun pluginRuntimeFields(
    name: String,
    description: String,
    author: String,
    id: String,
    engine: String,
    variant: String,
    versionName: String,
    versionCode: Long,
    versionDate: String,
): PluginRuntimeFields = PluginRuntimeFields(
    name = name,
    description = description,
    instruction = ThreeChimeAlarmContract.INSTRUCTION_REFERENCE,
    author = author,
    id = id,
    engine = engine,
    variant = variant,
    versionName = versionName,
    versionCode = versionCode,
    versionDate = versionDate,
    supportedAbis = emptyList(),
    requiredHostVersion = ThreeChimeAlarmContract.REQUIRED_HOST_VERSION,
    contractVersion = ThreeChimeAlarmContract.CONTRACT_VERSION,
    features = ThreeChimeAlarmContract.FEATURES,
)

/**
 * The identity answered by the INFO service (and by `IAlarmPlugin.getInfo` once roadmap P2.5 lands).
 * zh-CN: INFO 服务返回的身份 (路线图 P2.5 落地后 `IAlarmPlugin.getInfo` 也返回它).
 */
internal fun PluginRuntimeFields.toPluginInfo(): PluginInfo = PluginInfo().also { info ->
    info.name = name
    info.description = description
    info.instruction = instruction
    info.author = author
    info.id = id
    info.engine = engine
    info.variant = variant
    info.versionName = versionName
    info.versionCode = versionCode
    info.versionDate = versionDate
    // ABI independent: an explicit empty array, never null (new repository specification 5.5, roadmap T20).
    // zh-CN: 与 ABI 无关: 显式空数组, 不为 null (新仓库规范 5.5, 路线图 T20).
    info.supportedAbis = supportedAbis.toTypedArray()
    info.capabilities = Bundle().apply {
        putLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION, requiredHostVersion)
        putInt(ThreeChimeAlarmContract.CAPABILITY_CONTRACT_VERSION, contractVersion)
        putStringArray(ThreeChimeAlarmContract.CAPABILITY_FEATURES, features.toTypedArray())
    }
}

/**
 * Reads the localized description, build resources and the installed package version.
 * zh-CN: 读取本地化描述, 构建资源与已安装包的版本.
 */
internal fun Context.pluginRuntimeFields(): PluginRuntimeFields {
    val appContext = applicationContext
    val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
    return pluginRuntimeFields(
        name = appContext.getString(R.string.plugin_name),
        description = appContext.getString(R.string.plugin_description),
        author = appContext.getString(R.string.plugin_author),
        id = appContext.getString(R.string.plugin_id),
        engine = appContext.getString(R.string.plugin_engine),
        variant = appContext.getString(R.string.plugin_variant),
        versionName = packageInfo.versionName.orEmpty(),
        versionCode = packageInfo.versionCodeCompat(),
        versionDate = appContext.getString(R.string.plugin_version_date),
    )
}

private fun PackageInfo.versionCodeCompat(): Long {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return longVersionCode
    @Suppress("DEPRECATION")
    return versionCode.toLong()
}
