package io.github.supermonster003.autojs6.plugin.three.chime.alarm

/**
 * Identity and protocol constants of the plugin (roadmap 4.5).
 *
 * The capability action, category, Binder descriptor and capability keys live here only until the host contract module
 * `plugin-api/alarm-api` exists (roadmap P1.1); roadmap P2.5 replaces them with the contract constants so the host and the
 * plugin cannot drift apart.
 *
 * zh-CN: 插件的身份与协议常量 (路线图 4.5). 能力服务的 action, category, Binder 描述符与能力键只在宿主契约模块
 * `plugin-api/alarm-api` 落地 (路线图 P1.1) 之前暂存于此; 路线图 P2.5 改用契约常量, 避免宿主与插件两侧漂移.
 */
internal object ThreeChimeAlarmContract {

    const val PLUGIN_ID = "three-chime-alarm"
    const val ENGINE = "alarm"
    const val VARIANT = "default"

    /** Plugin Center identity service (roadmap 4.5). zh-CN: 插件中心身份服务 (路线图 4.5). */
    const val INFO_ACTION = "org.autojs.plugin.INFO"
    const val INFO_CATEGORY = PLUGIN_ID

    /** Capability service discovery (roadmap 4.5). zh-CN: 能力服务的发现 action 与 category (路线图 4.5). */
    const val SERVICE_ACTION = "org.autojs.plugin.ALARM"
    const val SERVICE_CATEGORY = "alarm"

    /** Descriptor of the future `IAlarmPlugin` (roadmap 4.3). zh-CN: 未来 `IAlarmPlugin` 的描述符 (路线图 4.3). */
    const val CAPABILITY_DESCRIPTOR = "org.autojs.plugin.alarm.api.IAlarmPlugin"

    /**
     * Temporary minimum host build (roadmap 4.5, T33): the first host build with the alarm contract of roadmap P1.1 - P1.3,
     * mirrored by `plugin_requires_host_version` in `app/build.gradle.kts`; roadmap P6.4 replaces both together.
     * zh-CN: 临时最低宿主构建号 (路线图 4.5, T33): 第一个包含路线图 P1.1 - P1.3 闹钟契约的宿主构建, 与 `app/build.gradle.kts` 中的
     * `plugin_requires_host_version` 一致; 路线图 P6.4 同时替换两者.
     */
    const val REQUIRED_HOST_VERSION = 6641L

    /** Alarm contract version (roadmap T25). zh-CN: 闹钟契约版本 (路线图 T25). */
    const val CONTRACT_VERSION = 1

    /**
     * Capability keys of roadmap 4.3; roadmap P2.5 switches to `AlarmCapabilityKeys`.
     * zh-CN: 路线图 4.3 的能力键; 路线图 P2.5 改用 `AlarmCapabilityKeys`.
     */
    const val CAPABILITY_CONTRACT_VERSION = "alarmContractVersion"
    const val CAPABILITY_FEATURES = "alarmFeatures"

    /**
     * Features this build implements (roadmap T25). The skeleton implements none; `alarms` (1.0.0), `timers` and
     * `stopwatch` (1.1.0), `holidays` and `beforeRing` (1.2.0) and `challenges` (1.3.0) join as each one lands, and the
     * host never calls a method whose feature is not advertised.
     * zh-CN: 本构建实现的功能 (路线图 T25). 骨架尚未实现任何功能; `alarms` (1.0.0), `timers` 与 `stopwatch` (1.1.0),
     * `holidays` 与 `beforeRing` (1.2.0), `challenges` (1.3.0) 在各自落地时加入, 宿主不调用未声明功能的方法.
     */
    val FEATURES: List<String> = emptyList()

    /** Resource reference of the localized Plugin Center instruction. zh-CN: 插件中心本地化说明的资源引用. */
    const val INSTRUCTION_REFERENCE = "@raw/plugin_instruction"

}
