# 3-Chime Alarm Roadmap

更新日期: 2026-11-05

本文是 `AutoJs6-Plugin-Three-Chime-Alarm` (显示名 `3-Chime Alarm`; 为 AutoJs6 脚本提供自建闹钟, 计时器与秒表, 响铃时联动脚本, 并以 `alarms.system` 子命名空间对接系统时钟应用; 插件本身是一个不装 AutoJs6 也能用的完整时钟应用) 的开发路线图. 立项来源: 2026-11-03 的 API 设计稿 (原 `docs/design/api-draft.md`, 内容已并入本文的第 1 节, 第 2 节, 第 4 节与附录 A / F, 原文件随本次立项删除), 其中 D1 - D19 经五轮选择题确定, 待定项 Q1 - Q6 于同日拍板为 D20 - D25.

基线 (2026-11-05): 宿主 `AutoJs6 master@e195d5c7f8` (`VERSION_NAME=6.8.1` 开发中, 6.8.0 已发布; `VERSION_BUILD=6640`, 宿主现在以 `VERSION_BUILD` 直接作为 versionCode, 不再有 `VERSION_CODE_OFFSET`), 平台版本插件 `1.9.1`. 宿主已有可直接参照的先例: `plugin-api/speech-api` 与 `core/plugin/speech` (契约模块与宿主桥接模板), `core/plugin/mail/MailTriggerReceiver` (插件以签名权限广播唤醒宿主并启动脚本, 事件成为 `execArgv.mail`), `AidlPluginHost`, 插件中心两张表与安装向导目录 (`TOOLS` 分类). 宿主侧改动只落本地提交, 由维护者决定随哪个宿主版本发布 (D25: 插件可先于宿主发布).

使用方式:

1. 每次会话开始时, 从 "阶段总览" (第 5 节) 选取一个或多个未完成条目, 优先级按阶段顺序; 单次会话可完成多个小节, 除非单个小节已足够繁杂.
2. 条目完成后勾选 `[x]`, 并在条目后追加证据 (提交 hash / 测试类名 / 设备型号与 API / 度量值), 证据等级见附录 C. 未勾选条目属于规划意向, 不代表当前版本能力.
3. 条目前缀标明主要落点: `(插件)` 本仓库, `(宿主)` `D:/idea-projects/AutoJs6`, `(资产)` `D:/idea-projects/AutoJs6-Shared-Assets`, `(索引)` `D:/idea-projects/AutoJs6-Official-Plugins-Index`, `(文档)` AutoJs6-Documentation / AutoJs6-TypeScript-Declarations / AutoJs6-Plugin-Ace-Editor / AutoJs6-Plugin-Offline-Docs, `(测试)` 真机或 AVD 验证, `(发布)` 版本, tag, Release 与索引.
4. 涉及宿主公开契约 (alarm 契约, 脚本 API) 的条目, 完成后必须同步宿主 `docs/dev/alarm-plugin-protocol-v1.md`, 宿主 `.changelog` (10 种语言, 本轮先只写 zh-Hans, 其他语言由维护者同步) 与本仓库 `.changelog`. 宿主侧提交只落本地, 由维护者合并与推送. 宿主工作区多会话共用: 只提交自己的文件, 不推送, 不切分支, 不碰其他会话在改的文件, 宿主构建与其他会话排队.
5. 第 1 节 "固定决策" (D1 - D25) 由维护者确认, 后续阶段不再重新讨论; 第 2 节 "派生技术决策" (T1 - T25) 可在进入对应阶段前推翻 (推翻点已逐条标注), 之后视同固定; 附录 B "待决事项" (Q7 起; Q1 - Q6 已转为 D20 - D25, 编号不复用) 在进入对应阶段前由维护者拍板 (调度会话用选择题提问), 拍板结果回填到附录 B 与对应决策行. 子会话新增的派生决策从 T26 起编号, 新增的待决事项从 Q10 起编号.
6. 本仓库骨架 (Gradle / Manifest / 资源 / CI) 在 P0 按 `D:/idea-projects/AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 生成, 其裁剪版即本仓库 `AGENTS.md`; 命名与身份派生按 `AUTOJS6_PLUGIN_THREE_SERIES_RENAME_AGENTS.md` 第 3 节 (Chime 为 5 字母单词); 图标按 `AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md`; 独立界面按 `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md`; 全部用户可见文案只用 ASCII 标点, 阿拉伯语里的品牌名 `3-Chime Alarm` 用 FSI / PDI 包裹 (Three 系列既有做法).
7. 2026-11-05 的立项会话只落盘本路线图并 `git init` + 首次提交, 同时建看板 `D:/idea-projects/.claude/boards/3-chime.md` 与调度会话; 之后按 `AGENTS.md` 的提交规则工作 (每笔提交前 `VERSION_BUILD` = 当前提交数 + 1).

---

## 1. 固定决策

D1 - D19 由维护者经五轮选择题确定 (2026-11-03), D20 - D25 由同日的待定项拍板得出 (原待定表见附录 F). 后续阶段不再重新讨论.

| 编号 | 事项 | 决定 |
| --- | --- | --- |
| D1 | 用途 | 叫醒 / 提醒, 计时器 (倒计时响铃), 响铃时联动脚本, 管理系统时钟的闹钟, 四者都要 |
| D2 | 层次 | 自建闹钟 (插件自己响铃, 可查询 / 修改 / 删除 / 监听) + 对接系统时钟应用 |
| D3 | 归属 | 独立 Three 插件; 宿主只提供契约模块与脚本模块 (同 3-Macaw Speech 的模式), 不含响铃实现 |
| D4 | 与 `tasks` | 完全独立: 不共用存储, 调度与重复规则代码 |
| D5 | 脚本入口 | 宿主全局模块 + 插件工具 (供 MCP Server 与 `plugins.tools.call`) |
| D6 | 联动方式 | 两种都支持: 闹钟登记脚本文件, 到时由宿主启动 (脚本不必常驻); 运行中的脚本可实时监听 |
| D7 | 插件界面 | 完整时钟应用: 闹钟列表, 新建 / 编辑, 计时器, 秒表, 铃声选择, 响铃界面, 设置; 不装 AutoJs6 也能当闹钟用 |
| D8 | 系统时钟层 | 宿主直接发 `AlarmClock` Intent, 放在同一模块的子命名空间 `alarms.system`, 未装插件也能用; 宿主声明 `SET_ALARM` |
| D9 | 名称 | 显示名 `3-Chime Alarm`, 仓库 `AutoJs6-Plugin-Three-Chime-Alarm` (Chime 为 5 个字母, 合规) |
| D10 | 模块名 | `alarms` (另有 `$alarms`) |
| D11 | 重复规则 | 完整日历规则: RFC 5545 RRULE 子集 (见附录 A 第 4.3 节) |
| D12 | 节假日 | 内置节假日数据 + `beforeRing` 钩子, 两者都做 |
| D13 | 关闭方式 | 普通关闭, 内置算术题 / 摇一摇 / 照打一句话, 以及 "脚本条件"; 一律有安全阀 |
| D14 | 计时器 | 多个并行, 暂停 / 续时 / 加时, 设备重启后恢复; 另加秒表 (计次) |
| D15 | 脚本权限 | 脚本对全部闹钟可读写, 包括用户在插件界面建的 |
| D16 | 节假日数据 | 多地区开源数据集 |
| D17 | 调休补班 | 完全以开源数据为准, 不做人工核对 |
| D18 | 响铃内容 | 系统铃声与本地音频, 朗读文字 (TTS), 渐强与独立音量, 自定义振动, 四者都要 |
| D19 | 版本切分 | 1.0.0 闹钟核心; 1.1.0 计时器与秒表; 1.2.0 节假日与 `beforeRing`; 1.3.0 关闭挑战 (见附录 A 第 8 节) |
| D20 | 登记脚本的绑定存在哪 (原 Q1) | 宿主侧 (T12). 插件界面只显示 "有脚本绑定" 并可解除, 不能在界面里选脚本 |
| D21 | 插件是否接收系统时钟 Intent (原 Q2) | 接收: `SET_ALARM`, `SHOW_ALARMS`, `DISMISS_ALARM`, `SNOOZE_ALARM` 随 1.0.0; `SET_TIMER`, `SHOW_TIMERS`, `DISMISS_TIMER` 随 1.1.0. 来源记为 `intent` |
| D22 | `bind` 的默认事件 (原 Q3) | `['ring']` |
| D23 | `alarms.system` 的 `skipUi` 默认值 (原 Q4) | `true` |
| D24 | 闹钟 id 的形态 (原 Q5) | 字符串, 插件生成, 闹钟 `a_` / 计时器 `t_` 前缀, 备份恢复不变 |
| D25 | 发布节奏 (原 Q6) | 插件可先于宿主发布: 界面与插件工具先可用, `alarms` 模块与联动脚本在新宿主发布后生效, README 标明所需宿主版本 |

---

## 2. 派生技术决策

T1 - T18 来自设计稿 (2026-11-03), T19 - T25 为立项时补充, T26 起由子会话在实施中派生. 每条可在 "推翻点" 所在阶段开始前推翻, 之后视同固定; 推翻时在本表改写并在对应条目注明.

| 编号 | 决策 | 理由 | 推翻点 |
| --- | --- | --- | --- |
| T1 | 最低 Android 与宿主相同 (API 24) | 与全系列插件一致; 直接启动 (Direct Boot) 也正好从 API 24 起可用 | P0.1 |
| T2 | 包名按 Three 系列最新做法用点分: `io.github.supermonster003.autojs6.plugin.three.chime.alarm` | 3-Stove Agent 以后的惯例 | P0.1 |
| T3 | 闹钟用 `AlarmManager.setAlarmClock` 调度, 每个启用的闹钟只排 "下一次", 响过后再算下一次 | 不受 Doze 推迟, 状态栏显示闹钟图标, 锁屏显示下一个闹钟, `getNextAlarmClock()` 能看到 | P2.3 |
| T4 | 权限: `USE_EXACT_ALARM` (API 33+, 安装即授予) + `SCHEDULE_EXACT_ALARM` (`maxSdkVersion="32"`); `USE_FULL_SCREEN_INTENT`; `POST_NOTIFICATIONS`; `VIBRATE`; `WAKE_LOCK`; `RECEIVE_BOOT_COMPLETED`; 前台服务及其类型 | 插件是闹钟应用, 精确闹钟权限的两种声明正好覆盖 API 31 - 32 与 33+ | P0.1 |
| T5 | 全屏响铃: API 34+ 先查 `NotificationManager.canUseFullScreenIntent()`, 未授予时在插件里引导到 `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`, 期间退化为高优先级通知 + 声音 | Android 14 起该权限可能不是默认授予 | P2.4 |
| T6 | 响铃前台服务用 `systemExempted` (权限 `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`; API 34 以下没有前台服务类型, `startForeground` 不带类型), 通知以 `FOREGROUND_SERVICE_IMMEDIATE` 发出. P0.2 实测 (2026-11-05, targetSdk 37 的 debug 探针, 提交前已删除: `setAlarmClock` 触发的接收器调用 `startForegroundService`, 服务以候选类型 `startForeground`, 再以 `USAGE_ALARM` 循环播放并申请 `AUDIOFOCUS_GAIN_TRANSIENT`; 场景为前台, 后台 (HOME), 被杀 (`am kill`), 熄屏拔电强制 idle (模拟器只进到 INACT); 设备为 API 34 临时 AVD (`default` x86_64, 测完已删除), `AVD_API_35_16K`, `AVD_API_36.1_16K`, `AVD_API_37.1_16K`; 触发偏差 3 - 1386 ms): 启动: `systemExempted` 与 `mediaPlayback` 在 4 个 API 的全部场景都能启动, 没有 `ForegroundServiceStartNotAllowedException` (系统日志 "Background started FGS: Allowed ... code:ALARM_MANAGER_ALARM_CLOCK", 临时白名单 10 s); 音频焦点: API 34 / 36 / 37 全部授予, API 35 只在前台授予, 后台 / 被杀 / idle (importance 125, procState 4) 被 Android 15 的音频加固拒绝 ("AS.HardeningEnforcer: Focus request DENIED"), 两种类型相同, 但 `USAGE_ALARM` 播放器仍为 started 且未静音, 铃声照常发声 (处理见 T35); 通知: 安装后 `systemExempted` 的首个前台服务通知在 1.5 s 时仍未显示 (系统最多推迟 10 s, `CATEGORY_ALARM` 不豁免), `mediaPlayback` 立即显示, 设 `FOREGROUND_SERVICE_IMMEDIATE` 后 API 34 / 36 / 37 首次即显示 | 两种类型实测的启动与焦点行为相同; `systemExempted` 是给持有精确闹钟权限, 用前台服务继续响铃的闹钟应用准备的类型, 而 Android 15 起 (targetSdk 35+) `BOOT_COMPLETED` 接收器不能启动 `mediaPlayback` 前台服务, 开机补响 (T16) 会受限 | 已固定 (P0.2 实测) |
| T7 | 直接启动: 调度, 响铃, 计时器组件 `directBootAware`, 数据放设备加密存储; 重启后未解锁也能响 | 闹钟应用的基本要求 | P2.1 |
| T8 | 重新排程的时机: `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`, 以及节假日数据更新后 | 改时间, 跨时区, 升级都不能丢闹钟 | P2.3 |
| T9 | 时间按 "墙上时间 + 时区" 保存: 默认跟随设备时区, 可固定时区 (`timeZone: "Asia/Shanghai"`); 夏令时跳过的时刻顺延到跳变后第一刻, 重复的时刻只响第一次 | 与系统时钟的行为一致 | P2.2 |
| T10 | RRULE 计算用 dmfs `lib-recur` 0.17.1 (`org.dmfs:lib-recur`, Maven 最新版, 2024-04 发布), P0.2 已接入. 核对结论: Apache-2.0; 依赖 `org.dmfs:rfc5545-datetime` 0.3 与 `org.dmfs:jems2` 2.23.1 (同为 Apache-2.0, 无更多依赖; jems2 的 NOTICE 已转录进 `THIRD_PARTY_NOTICES.md`); 三个 jar 合计 340,683 B, class 文件版本 52, 不引用 `java.time` / `java.util.function` (lambda 由 D8 脱糖), API 24 可用; 三者全量 d8 (`--release --min-api 24`) 后 dex 212,660 B (1672 个方法引用), release 包经 R8 以代表性用法 (解析, `OfRule` / `Difference` / `OfList`, `fastForward`) 实测 APK +29,755 B (classes.dex +53,408 B); 支持 `BYSETPOS`, 负序号, `COUNT` / `UNTIL`, `EXDATE` (recurrenceset 的 `Difference` + `OfList`), 带时区的 `DateTime`; 起点不匹配规则时 `OfRule` 只给规则实例 (要把起点也算一次用 `OfRuleAndFirst`) | 递推, `BYSETPOS`, 负序号等边界多, 自写易错 | 已固定 (P0.2 核对并接入; 证据 `LibRecurEvaluationTest` 4 例) |
| T11 | 联动通道照 3-Stamp Mail P8: 插件发受签名权限 `org.autojs.permission.PLUGIN` 保护的广播 `org.autojs.autojs6.action.ALARM_TRIGGER` 唤醒宿主, 宿主启动登记的脚本, 事件 JSON 成为 `engines.myEngine().execArgv.alarm` | 现成先例 (`MailTriggerReceiver`), 只有同签名的包能唤醒脚本 | P1.2 |
| T12 | 登记脚本的绑定 (闹钟 id -> 脚本路径 + 事件集合) 存在宿主侧, 插件只知道 "该闹钟有绑定, 哪些事件需要通知" | 插件进程不能让宿主运行任意路径, 与 Mail P8 的安全姿态一致; 已由 D20 确认 | 已固定 (D20) |
| T13 | 实时监听经 AIDL 回调: 有脚本调用 `alarms.on()` 时宿主向插件注册一个 oneway 回调, 宿主再分发给各脚本; 最后一个监听者退出时注销 | 一个宿主进程只占一条绑定 | P1.2 |
| T14 | 响铃走 `USAGE_ALARM` 音频属性 (闹钟音量, 勿扰模式默认放行闹钟); "独立音量" 是响铃期间临时设定闹钟音量, 结束后恢复 | 不受媒体音量影响 | P2.4 |
| T15 | 脚本给出的铃声文件由宿主以只读 pfd 交给插件, 插件复制一份 (上限 20 MiB), 原文件删掉也能响 | 插件没有宿主目录的访问权; 同 speech 的文件传递方式 | P1.1 |
| T16 | 错过的闹钟: 设备关机或系统推迟导致超过触发时刻时, 5 分钟内补响, 超过则不响, 发 `miss` 事件并留一条通知 | 半夜开机突然响铃比漏响更糟 | P2.3 |
| T17 | 插件工具不开放脚本绑定与 `beforeRing` | 不让远端 AI 助手安排宿主运行脚本 | P4.1 |
| T18 | 插件界面标出每个闹钟的创建来源与最后修改来源 (用户 / 脚本 / 工具 / 系统 Intent) | D15 让脚本能改用户的闹钟, 用户至少要看得到是谁改的 | P3.1 |
| T19 | 契约 AIDL 主接口命名 `IAlarmPlugin` (`getInfo` 居首), 回调 `IAlarmEventCallback` / `IAlarmProgressCallback`; 设计稿中的 `IChimeAlarm` 不采用 | 契约包 `org.autojs.plugin.alarm.api` 不绑定某个插件的名字, 与 `ISpeechPlugin` / `IMailPlugin` 的惯例一致 | P1.1 |
| T20 | 单 APK, 不拆 ABI: 插件只有 Java / Kotlin 字节码与普通资源 (RRULE 库为纯 Java), 没有原生库; `supportedAbis = emptyArray()`, 16 KB 页校验不适用. P0.2 核对: `:app:appendDigestToReleasedFiles` 产出 `autojs6-plugin-three-chime-alarm-v1.0.0-53a16e43.apk` (versionCode 3, 324,761 B, 78 个条目; 骨架尚未调用 lib-recur, R8 将其整个移除), `unzip -l` 无 `lib/` 与 `.so`. 引入 Compose 后的做法见 T34 | 新仓库规范 5.5: 拆分无收益时省略, 理由须可由 APK 内容验证 | 已固定 (P0.2 核对); P3.1 起由 T34 补充 |
| T21 | 闹钟 / 计时器 / 秒表存储用 JSON 文档 + fsync + 原子替换 (同 3-Stamp Mail `TriggerStore`), 不引入 Room / KSP; 文件放设备加密存储 (`createDeviceProtectedStorageContext()`) | 数据量上限 300 条, 查询都在内存完成; 少一套注解处理与数据库迁移; 直接启动前即可读 | P2.1 |
| T22 | 独立界面用 Compose + Material 3, 跟随宿主的语言 / 夜间模式 / 主题 (`AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md`), Compose BOM 与 3-Macaw Speech 当前值一致 | Three 系列独立界面的现行做法 (3-Stone AI, 3-Prism Camera, 3-Maple Image) | P3.1 |
| T23 | 安装向导分类 `TOOLS` (与 3-Macaw Speech / 3-Stone AI / 3-Stove Agent 同列), `PluginInstallWizardCatalog` 加 `entry(official("three.chime.alarm"), "Three Chime Alarm", TOOLS)` | 宿主零枚举改动; 闹钟不属于 IMAGE / OCR 等既有专业分类 | P1.3 |
| T24 | 契约请求与结果以 `Bundle` 传输 (键集中在 `AlarmOptionKeys` / `AlarmResultKeys` / `AlarmEventKeys`), 不走 JSON 字符串; 事件回调 `oneway`; 列表类结果单次往返并受 `AlarmLimits` 约束 | 同 speech-api D21 的做法; Binder 输入边界可逐键校验 | P1.1 |
| T25 | 契约 v1 一次定义 1.0.0 - 1.3.0 的全部方法 (闹钟, 响铃, 铃声, 计时器, 秒表, 节假日, `beforeRing`, 挑战), 插件以能力键 `alarmFeatures` (`alarms` / `timers` / `stopwatch` / `holidays` / `beforeRing` / `challenges`) 声明已实现的功能; 未声明的方法宿主不调用, 插件返回 `UNSUPPORTED`; 1.1.0 - 1.3.0 不改 AIDL 只加 feature | 避免 3-Macaw Speech 契约 v2 那样的版本协商返工 (Macaw D136); 宿主 `requireFeature` 已有现成做法 | P1.1 |
| T26 | 契约 v1 没有打包应用 (PACKAGED) 入口: 打包应用与宿主一样使用已安装的插件 | 闹钟要比脚本进程活得久, 打包应用内嵌一份插件没有意义 | 已固定 (P1.1) |
| T27 | "有绑定的闹钟与事件集合" 提示表用独立方法 `setBindingHints` 整表替换, 应答带 `unknownIds` (宿主据此删除对应绑定); 插件界面解除绑定时发 `ALARM_TRIGGER` 广播 (`request=unbind` + `alarmId`) | 整表替换避免增量同步的漂移; 插件不能直接改宿主侧绑定表 (T12) | 已固定 (P1.1 / P1.2) |
| T28 | `ALARM_TRIGGER` 是有序广播, 宿主以结果 extras 应答 (`launched` / `duplicates` / `missing` / `removed` / `scriptSkipped`); 没有应答时插件按自身状态记 `scriptSkipped: 'locked'` / `'unavailable'`; 宿主接收器不是 `directBootAware` | 插件需要知道脚本是否启动, 才能在界面与事件里如实显示; 宿主数据不在设备加密存储, 未解锁时不能启动脚本 | 已固定 (P1.2) |
| T29 | `update` 的 patch 语义: 键存在且值为 `null` 表示清除并回到默认值, 分组 (如铃声, 贪睡) 整体替换, `at` 与 `hour` / `minute` 互相替换 | 与附录 A 第 4 节的选项结构一致, 避免深合并的歧义 | 已固定 (P1.1) |
| T30 | 契约编解码为共享值树 + 无依赖的 `AlarmJson`; `Bundle` 桥只在插件 P2.7 的真机契约测试中验证 (宿主没有 Robolectric) | 宿主与插件共用一套编解码, JVM 测试可覆盖值树往返 | 已固定 (P1.1); P2.7 验证 Bundle 桥 |
| T31 | `occurrence` 按 "一轮响铃" 发放 (贪睡后再响为新的 `occurrence`, 与 P5.5 一致), `change` 事件不带 `scheduledAt`; 宿主去重键为 (`occurrence`, 事件, 脚本) | 去重键不必再含贪睡次数 | 已固定 (P1.2) |
| T32 | 绑定的 `leadSeconds` 只在 `events` 含 `beforeRing` 时允许, 否则返回 `INVALID_OPTIONS` (不静默忽略) | 静默忽略会让脚本以为已生效 | 已固定 (P1.2) |
| T33 | 插件临时 `requiresHostVersion` = 6641: P1.1 - P1.3 提交时宿主 `VERSION_BUILD` 为 6640, 而 6640 同时是不含这些提交的基线构建 (`e195d5c7f8`), 取下一个构建号 (之后的宿主构建都 >= 6641 且包含这些提交); 宿主 `AlarmPluginIds.REQUIRED_HOST_VERSION_CODE` 同为 6641 | 低于 6641 的宿主没有 `alarm-api`, 不应启用插件 | P6.4 回填为包含 P1 全部宿主提交的最终值 |
| T34 | 引入 Compose (T22) 后仍是单个通用 APK: Compose `ui-graphics` 的传递依赖 `androidx.graphics:graphics-path` 会把 `libandroidx.graphics.path.so` 放进 4 个 ABI 的 `lib/` (3-Batch Rename 1.1.1 实测: arm64-v8a 10,096 B, armeabi-v7a 7,252 B, x86 9,284 B, x86_64 10,760 B, 合计 37,392 B, 未压缩存储), T20 的 "APK 无 `lib/`" 从那时起不成立. 做法照 3-Batch Rename D28 / 3-Maple Image / 3-Block Compose: `supportedAbis` 仍为显式空数组, Manifest 声明 `NATIVE_PAGE_ALIGNMENT=16384`, `appendDigestToReleasedFiles` 的校验从 "无 `lib/` 与 `.so`" 改为 "`lib/` 只含 4 个 ABI 的这一个库 (白名单), 未压缩且 16 KB 对齐", `AGENTS.md` 与 README 的 "无原生库" 同步改写 | 该库是 `androidx.graphics.path` (API 34 以下的 `PathIterator` 实现) 的原生部分, 由 Compose 传递引入, 不宜排除; 按 ABI 拆分每个 APK 只省约 28 KB, 不值得多出 4 个发布文件 | P3.1 (引入 Compose 的同一提交里改校验) |
| T35 | 响铃不以音频焦点为前提: `RingPlayer` 先申请 `AUDIOFOCUS_GAIN_TRANSIENT` (`USAGE_ALARM`), 被拒时照常播放并记日志, `RingActivity` 到前台后再申请一次; 焦点只用来让其他媒体暂停或压低 | P0.2 实测 API 35 后台响铃时焦点被 Android 15 的音频加固拒绝, 而 `USAGE_ALARM` 播放器未被静音, 铃声照常发声 (T6); 以拿到焦点为响铃前提会漏响 | P2.4 |
| T36 | 宿主模块位于 `runtime/api/modules/alarms/` (KSP `@ScriptModule`), P1.4 条目写的 `runtime/api/augment/alarms/` 为旧路径 | 宿主新模块统一走 KSP 注册的 modules 目录 | 已固定 (P1.4, chime-p1-script) |
| T37 | `Alarm` 快照对象的方法为 `update` / `remove` / `enable` / `disable` / `skipNext` / `cancelSkip` / `bind` / `unbind` | 与 `alarms` 的同名函数一一对应, 快照只是携带 id 的便捷入口 | 已固定 (P1.4) |
| T38 | `alarms.on` 返回 `{ event, off() }` 句柄 | 多个监听共存时可以单独注销 | 已固定 (P1.4) |
| T39 | `alarms.currentEvent` 在 1.0.0 返回普通对象 (不带快照方法) | 事件来自 `execArgv.alarm` 的 JSON, 脚本启动时插件不一定可绑定 | 已固定 (P1.4) |
| T40 | `alarms.system.handlers(action)` 接受 7 个动作名, 省略时为 `setAlarm` | 与 `alarms.system` 的 7 个方法同名 | 已固定 (P1.5) |
| T41 | `alarms.system.dismissAlarm` 未写 `mode` 时为 `'next'`; `'time'` 需要 `hour` + `minute`, `'label'` 需要 `label`, 其他组合报 `INVALID_OPTIONS` | 与 `AlarmClock.EXTRA_ALARM_SEARCH_MODE` 的四种模式一致, 缺参时不静默退化 | 已固定 (P1.5) |
| T42 | `repeat` 的空星期数组报 `INVALID_RULE` | 空数组既不是一次性也不是重复, 静默当一次性会误导脚本 | 已固定 (P1.4) |
| T43 | `snooze.minutes` 范围 1 - 60 (`AlarmLimits.MIN_SNOOZE_MINUTES` / `MAX_SNOOZE_MINUTES`) | 两侧共用契约常量 | 已固定 (P1.4) |
| T44 | 带铃声文件的 `add` / `update` 先 `importRingtone` 再提交, 提交失败不回滚已导入的铃声 | 导入的铃声本身可复用, 回滚需要额外的删除接口 | 已固定 (P1.4) |
| T45 | `bind` 只按脚本目录解析相对路径, 不检查文件是否存在 | 文件可能稍后才写入; 触发时宿主跳过不存在的文件并记 `missing` (T28) | 已固定 (P1.4) |
| T46 | 宿主样例的工作日用星期数组 `[1, 2, 3, 4, 5]`, 1.2.0 节假日上线后补节假日样例 | 1.0.0 没有 `holidays` 功能 | 已固定 (P1.6) |
| T47 | 核心库脱糖 (`desugar_jdk_libs` 2.1.5, 与宿主及 3-Stamp Mail 同版), 闹钟核心直接使用 `java.time` | API 24 / 25 没有 `java.time`, 自写日期运算或引入 ThreeTen 都更重; 脱糖库按 GPL-2.0 with the Classpath Exception 写进 `THIRD_PARTY_NOTICES.md` | 已固定 (P2.1) |
| T48 | 隐式 `date`: 有 `time` 无 `date` 时插件补上日期并记 `dateImplicit` (重复闹钟取今天; 一次性闹钟取当天, 当天该时刻不晚于当前时刻时取明天; 按闹钟的 `timeZone`, 否则按设备时区), 快照显示补上的日期. 隐式日期在 patch 改了时刻 / 规则 / 时区而未给 `date` 时重新计算, patch 给了 `date` 即转为显式; 一次性 `time` 闹钟在时刻已过后再次启用 (`setAlarmEnabled`, 或 patch 带 `enabled: true` 且未给 `date`) 时改到下一个到达该时刻的日子 | 附录 A 第 4.2 节的省略规则需要落到存储上; 已响过的一次性闹钟在界面上重新打开应当再响一次 (同系统时钟) | P3.1 |
| T49 | 存储损坏处理: 单条记录校验失败, 重复 id, 坏 id / 来源, 超过 300 条时跳过该条; 整个文档不可解析, 格式不符或无法迁移时按空列表处理; 更新版本 (降级后) 的文档丢弃不认识的顶层选项后照常读取; 启用超过 200 条时按创建顺序把多出的改为禁用. 以上情况都先把原文件复制到 `quarantine/alarms-<毫秒时间戳>.json` (保留最新 5 份), 再写回清理后的文档; 单纯的旧版本迁移不留副本 | 损坏一条不影响其他闹钟; 原文件留作日后人工恢复, 下次启动即为干净状态 | 已固定 (P2.1) |
| T50 | 插件侧补充校验与语义 (插件为准, 宿主不必重复): `vibratePattern` 至少一项非 0 (`VibrationEffect.createWaveform` 拒绝全 0); 引擎 / 语言 / 地区 / 时区名非空且至多 128 个字符, 铃声 uri 至多 2048 个字符; patch 的 `hour` 与 `minute` 必须同时给出 (`second` 缺省为 0); 重复闹钟改用 `at` 时须在同一个 patch 中把 `repeat` 设为 `null`, 否则报 `INVALID_OPTIONS`; `setAlarmEnabled` 状态未变时原样返回, 不发 `change`; 禁用闹钟时清除 `skipNext` 与贪睡状态 | 这些组合在设备上会失败或语义不明, 由插件统一拒绝, 脚本得到写明键名的错误; P2.7 对照协议文档时补进 "Patch semantics" | 已固定 (P2.1) |
| T51 | `repeatText` 文案放在 Kotlin 表 (`core/rule/RepeatText.kt`) 而非字符串资源, 按调用方 locale 选语言 (10 种, 其他语言用英语); 中文的周一至周五写 "周一至周五" 而非 "工作日" (1.2.0 的 `holidays` 工作日模式才是真正的工作日); 其他形态 (`COUNT` / `UNTIL`, `BYWEEKNO`, `BYYEARDAY`, 多个月份, 按月的间隔等) 回退为规范化 RRULE 原文 | 文案要按规则形态拼接序数, 复数与星期名, JVM 测试不需要 Android 即可逐语言核对; 避免与 1.2.0 的节假日语义混淆 | P3.1 |
| T52 | 日期计算分工: lib-recur 计算 DAILY / WEEKLY / MONTHLY 与不带 `BYYEARDAY` / `BYSETPOS` 的 YEARLY; `BYWEEKNO` 规则与带 `BYYEARDAY` 或 `BYSETPOS` 的 YEARLY 规则由插件逐年展开; lib-recur 给出的日期再按规则核对一次; `UNTIL` 由插件判断 (日期按整天, 浮动时刻按墙上时间, UTC 按时刻) | lib-recur 0.17.1 实测偏差: 52 周的年份把第 53 周当作第 52 周, 弄混跨年前后的周; YEARLY 带 `BYMONTH` 或 `BYYEARDAY` 时先去掉起点之前的日期再应用 `BYSETPOS` (例: 2026-04-15 起 `BYMONTH=2,5;BYMONTHDAY=1;BYSETPOS=2` 给出 2027-05-01, 应为 2026-05-01); `BYYEARDAY` 与 `BYDAY` 同用时抛 `ArrayIndexOutOfBoundsException`; 浮动起点拒绝 UTC 的 `UNTIL`. 其余情形与 1000 个随机规则的参考实现对拍一致 | 已固定 (P2.2) |
| T53 | 规则语义 (RFC 5545 未写明处): 起点不符合规则时不算一次响铃; `exclude` 在 `COUNT` 之后去掉日期 (被排除的日期占用 `COUNT` 名额, 不补回); 只有 `BYMONTHDAY` 的 YEARLY 规则停留在起点月份 (RFC 5545 "规则未给出的部分取自 DTSTART", 与 lib-recur 一致; dateutil 取全年各月); 只有 `BYDAY` 的 YEARLY 规则覆盖全年; 带序号的 `BYDAY` 在 MONTHLY 或带 `BYMONTH` 的 YEARLY 中按月计数, 否则按年; `BYSETPOS` 作用于整个周期 (含起点之前的日子) 后再去掉起点之前的日期; 从起点起永远不产生日期 (连续 400 个周期没有日期) 的规则报 `INVALID_RULE` 并写明, 已结束的规则不是错误 (`next` 为空) | 与 RFC 5545 的例子及 lib-recur 的已验证部分一致, 脚本得到可预期的结果; 永远不响的闹钟在保存时就报错 | P6.1 |
| T54 | `preview` / `upcoming` 细节: `preview` 按新建闹钟的方式补隐式日期 (T48), 只列晚于现在的响铃; `upcoming` 只列已启用的闹钟, 去掉 `skipNext` 跳过的那次, 贪睡中的闹钟在贪睡结束时刻另列一次, 同一时刻按列表顺序, 规则无法计算的闹钟不列出 (由调度器报告); 时区按闹钟的 `timeZone`, 否则按设备时区 (T9) | 附录 A 第 4.3 节未写明的组合; 与界面上 "接下来的闹钟" 一致 | P3.1 |
| T55 | `skipNext` 的实现: 重复闹钟把下一次计划响铃记为 `skipAt`, `nextTrigger` 改为其后的一次; 被跳过的时刻另排一个 `setExactAndAllowWhileIdle` 唤醒 (不占系统的 "下一个闹钟"), 到时发 `skip` 事件 (`reason: 'skipNext'`) 并记为已处理; `cancelSkip` 在那一刻之前撤销; 一次性闹钟的 `skipNext` 等同于 `setAlarmEnabled(false)` (附录 A) | 跳过不改闹钟的规则, 脚本仍能知道哪一次被跳过; 锁屏与状态栏显示的是真正会响的那次 | 已固定 (P2.3) |
| T56 | 已处理时刻 `handledThrough` (最近一次已响, 已跳过或已错过的响铃): 下一次响铃取 "现在" 与它中较晚者之后的第一次; 它比时钟超前 24 小时以上时忽略 | 自动对时或手动把时钟往回调几分钟时, 同一次不会响两遍; 有意往回调一天以上 (测试, 改错日期) 时照常响 | 已固定 (P2.3) |
| T57 | 一轮响铃的结束: 计划响铃错过 (超过 5 分钟窗口) 或贪睡在窗口外才被处理时, 本轮静默结束 (贪睡不补响, 不另发 `miss`); 一次性闹钟本轮结束且没有下一次时由调度器禁用 (退役, 不发 `change`); 响铃中的一次性闹钟保持启用, 由 P2.4 的停止 / 自动静音结束本轮后退役 | 与系统时钟一致: 列表里不留永远不会再响的启用闹钟; 关机期间到期的贪睡开机后不再突然响 | P2.3 / P2.4 |
| T58 | 跳过失效: 闹钟的时刻, 规则, 日期或时区改了, 被跳过的那次不再是将来的一次响铃时清除跳过 (`skipping` 变为 false), 不发 `skip` | 改过时间的闹钟按新时间响, 不会因旧的跳过而漏掉新的第一次 | 已固定 (P2.3) |
| T59 | `miss` 的报告方式: 每个闹钟每次对账只为其计划的那一次报一次 `miss` 并留一条通知 (关机多天也不逐次补报, 其后错过的实例直接算入已处理); 开机对账 (`BOOT_COMPLETED` / `LOCKED_BOOT_COMPLETED`) 时原因为 `off`, 其他时机为 `late`; 闹钟自身的唤醒接受提前至多 2 秒 | 一条通知足以提醒, 逐次补报只会刷屏; `AlarmManager` 的唤醒偶尔略早于设定时刻 | 已固定 (P2.3) |
| T60 | 排程的可靠性与边界: 插件进程每次启动 (`Application.onCreate`) 对全部闹钟对账一次; 调度接收器 `exported=false` (T8 广播来自系统 uid, 不受影响; 唤醒用显式 `PendingIntent`), 对账在单线程执行器上运行并用 `goAsync` 结束广播; `AlarmClockInfo` 的展示 Intent 暂时打开主页面, P3.1 起改为闹钟列表; `capabilities().permissions.exactAlarm` 与 `PERMISSION_REQUIRED` 读同一个 `canScheduleExact()` (API 30 及以下恒为 true, API 33 起 `USE_EXACT_ALARM` 安装即授予) | 强行停止会清掉 `AlarmManager` 中的闹钟且没有广播通知; 其他应用不能伪造触发或重排; 权限判断只有一处 | 已固定 (P2.3), 展示 Intent P3.1 |
| T61 | 振动: 省略 `vibrate` 或为 `true` 时按默认节奏 `[0, 600, 400]` 循环, 给出 `vibratePattern` 时按其循环 (`device.vibrate` 写法: 等待, 振动, 等待, ...), `vibrate: false` 不振动; 振动以闹钟用途发出 (API 33+ `VibrationAttributes.USAGE_ALARM`, 更低版本 `AudioAttributes.USAGE_ALARM`), 设备没有振动器时只记日志 | 与系统时钟的节奏相近; 以闹钟用途发出, 勿扰与关闭触摸振动时仍振动 | 已固定 (P2.4) |
| T62 | 朗读时序 (附录 A 第 4.4 节只给了四个名称): `after` (默认) 铃声先响 5 秒再朗读, 朗读时铃声压到 0.25 倍; `before` 先在无铃声时朗读, 读完再响铃, 之后的朗读同样压低; `interleave` 铃声响一段 (`repeatInterval` 秒, 为 0 时 10 秒) 后停下朗读, 如此交替; `only` 只朗读; 除 `interleave` 外, `repeatInterval` 为 0 时只读一次, 否则在上一次读完后隔这么久再读; 计时用 `elapsedRealtime` | 压低而不停止让 `after` 仍像闹钟, `interleave` 停下铃声让朗读听得清; 调整系统时钟不影响朗读节奏 | 已固定 (P2.4) |
| T63 | 朗读不可用时照常发声: 指定的 `engine` 未安装 (不改用系统引擎), 引擎 10 秒内未就绪, 缺少 `language` 的数据或不支持, `speak` 被拒都算不可用, 记一行日志后放弃本轮朗读; 本身不出声的响铃 (`silent`, `only`) 此时播放其铃声, `silent` 播放默认铃声; 一次朗读 3 分钟未报告结束视为读完 | 闹钟不能因为朗读引擎出错而无声; 明确指定的引擎被替换会读出意料之外的声音, 不如只放铃声 | 已固定 (P2.4) |
| T64 | 同时到期的闹钟排队: 同一时间只有一个闹钟发声, 其他按到期顺序等待, 前一个贪睡, 关闭或自动停止后下一个开始发声并发 `ring`; 同一闹钟的新一轮替换其旧的一轮; `ringing()` 依次列出发声的, 排队的, 贪睡中的闹钟 | 两个铃声与两段朗读叠在一起听不清; 脚本能看到每个闹钟的状态 | 已固定 (P2.4) |
| T65 | 贪睡被拒的情形: 闹钟关闭了贪睡 (`snooze: false`), 已达 `max` 次, 已在贪睡中, 或没有精确闹钟权限而无法排程时不贪睡 (`snooze()` 返回 false, 响铃界面与通知不显示贪睡按钮); `minutes` 超出 1 - 60 报 `INVALID_OPTIONS` | 无法按时再响的贪睡等于静默关闭闹钟, 不如让用户只能关闭 | 已固定 (P2.4) |
| T66 | 响铃中的闹钟被删除或禁用时以 `dismiss` 结束本轮, `by` 取变更来源 (`script` / `tool` / `user`, 系统时钟 Intent 算作 `user`) | 被删除的闹钟不应继续响; 事件里能看出是谁停的 | 已固定 (P2.4) |
| T67 | 进程在响铃中死亡后的接续: 插件进程再次启动时, 开始时间未超过自动停止时长的一轮以相同的 `occurrence` 再次发声并再发一次 `ring` (宿主按 `occurrence` 去重, 不会重复启动绑定脚本, 订阅事件的脚本能知道又响了), 更早的一轮静默结束, 不发事件 | 系统回收或崩溃不应让闹钟哑掉; 超过自动停止时长本来也会停 | 已固定 (P2.4) |
| T68 | 独立音量与渐强: 带 `sound.volume` 的响铃在首次改动前把用户的闹钟流音量立即写入设备加密存储, 只在没有闹钟发声时恢复 (接连两个带音量的响铃不会把借来的音量当成用户的), 进程死亡后下次启动时恢复; 不带音量的响铃也会先恢复借来的音量; `volume` 0 让播放器静音 (闹钟流的最低音量级往往不是无声); 渐强按时间的平方从 0.01 增至 1 (每 200 毫秒一次), 只作用于播放器增益 | 用户的音量总能回来; 平方曲线在低音量段更平缓, 不会一开始就很响 | 已固定 (P2.4) |
| T69 | 权限不全时的响铃: 没有全屏通知权限 (API 34+ `canUseFullScreenIntent()` 为 false) 时以悬浮通知响铃, 点通知打开响铃界面; 没有通知权限 (API 33+) 时前台服务照常运行, 铃声, 振动与朗读照常, 但没有通知与响铃界面, 只能由脚本, 自动停止或删除闹钟结束; 引导在 P3.4 | 声音是闹钟最重要的部分, 不因权限缺失而放弃; 系统不允许后台应用自行打开界面 | 已固定 (P2.4), 引导 P3.4 |
| T70 | 铃声来源与回退: `default` 为系统默认闹钟铃声; 导入的铃声为 `chime-ringtone://imported/<id>` (id 为 1 - 64 个字母, 数字, `_`, `-`), 对应设备加密存储 `filesDir/ringtones/<id>`, 首次解锁前也能播放; 其他为 uri 原文; 无法播放时依次回退到默认闹钟铃声, 实际默认闹钟铃声, 默认来电铃声, 默认通知音, 最后用闹钟流的 `ToneGenerator` 提示音, 每次回退记一行日志 | id 不含路径分隔符, uri 不会指向目录之外; 无论文件损坏, 权限丢失还是媒体库未解锁, 闹钟都有声音 | 已固定 (P2.4), 导入 P2.5 |
| T71 | 绑定提示表: 每个宿主包一张 (以调用方 uid 的包名排序后第一个为准, AutoJs6 与打包应用互不覆盖), 存在设备加密存储 `filesDir/hints.json`, 跨进程保留, 文件损坏时从空表开始; `setBindingHints` 整表替换, 任一行格式错误 (未知键, 未知事件, `leadSeconds` 没有 `beforeRing` 或超出 15 - 300, 同一闹钟出现两次) 报 `INVALID_OPTIONS` 并保留旧表, 超过 1000 行报 `LIMIT_EXCEEDED`; 应答 `unknownIds` 列出不存在的 id (含计时器 id); `hasBindings` 为任一宿主表中有该闹钟; `leadSeconds` 先存下, `beforeRing` 的排程在 P8 | 多个宿主各自同步自己的表; 宿主下次连接会重发, 丢表只影响到那时为止的触发 | 已固定 (P2.5) |
| T72 | 铃声导入: 复制进设备加密存储 `filesDir/ringtones/<SHA-256 前 32 位十六进制>`, 标题记在同目录 `index.json`; 超过 20 MiB 报 `LIMIT_EXCEEDED` (与宿主预检一致; 协议文档原写 `IO_FAILED`, P2.7 修正文档), 空文件或 `MediaExtractor` 找不到音轨报 `IO_FAILED`; 同一内容只存一份并保留首次的标题; 标题取 `displayName` 去掉路径与扩展名, 至多 100 个字符, 为空时为 `ringtone`; 至多 500 个; `mimeType` 只作提示不参与校验; 描述符在调用结束时关闭 | 内容寻址让重复导入不占空间, 文件名不含用户输入; 原文件删除后仍可响 (T15) | 已固定 (P2.5) |
| T73 | `change` 事件: 插件内任何来源的增删改都发 (仓库监听器, T18), `occurrence` 为 `<id>@change~<firedAt>~<6 位随机>` (每次变更都不同, 不超过 128 个字符), `remove` 的 `label` 取删除前的值, `scheduledAt` 为空 | 宿主按 `occurrence` 去重, 同一毫秒的两次变更也不会被合并 | 已固定 (P2.5) |
| T74 | `listRingtones`: 先列系统闹钟铃声 (`RingtoneManager.TYPE_ALARM`, 默认铃声标 `default`, 默认铃声不在列表中时补在最前), 再按导入时间列导入的铃声, 合计至多 500 个; 系统铃声查询失败时只列导入的并记日志 | 脚本能直接取到可用于 `sound.source` 的 uri | 已固定 (P2.5) |
| T75 | 未知 id: `getAlarm` 返回不带 `alarm` 的应答 (脚本得到 `null`), `removeAlarm` 返回 `removed: false`, `dismiss` / `snooze` 返回 `handled: false`, `updateAlarm` / `setAlarmEnabled` / `skipNext` / `cancelSkip` 报 `NOT_FOUND` | 查询与删除是幂等的, 修改不存在的闹钟是脚本错误 | 已固定 (P2.5) |
| T76 | 实时回调不等广播结果; `ALARM_TRIGGER` 有序广播的应答 (`launched` / `duplicates` / `missing` / `removed`) 或 `scriptSkipped` (`locked` / `unavailable`) 只记日志 (`adb logcat -s ChimeTrigger`) 与进程内最近结果, 不重试; 事件 JSON 超过 16 KiB 时不发并记日志; `remove` 的广播发出后忘记该闹钟的提示行 | 宿主不在或未解锁时重试会让脚本在事后很久才启动; 日志足够排查 | 已固定 (P2.5) |
| T77 | 事件回调: 以回调的 Binder 为键, 同一回调再次注册时替换其事件集; 省略 `events` 为全部事件, 空数组也接受 (登记但不收事件), 未知事件报 `INVALID_OPTIONS`; 至多 8 个, 超出报 `LIMIT_EXCEEDED`; 回调抛出异常或 `linkToDeath` 报告死亡时移除; 不假定只有一个宿主进程 | 宿主重连时重复注册不会占满名额; 死掉的宿主不留下回调 | 已固定 (P2.5) |
| T78 | Binder 调用记来源 `script` (`source`, `updatedBy`, `dismiss` / `snooze` 的 `by`); `tool` / `intent` / `user` 由插件工具 (P4), 系统时钟 Intent (P2.6) 与界面 (P3) 各自传入 (T18); 1.1.0 - 1.3.0 的方法 (计时器, 秒表, 节假日, `decideBeforeRing`) 返回 `UNSUPPORTED` 并说明所需功能, `refreshHolidays` 立即经回调报告失败 | 来源只在入口确定一次; 宿主按 `alarmFeatures` 判断可用功能 (T25) | 已固定 (P2.5) |
| T79 | 系统时钟 `SET_ALARM`: `EXTRA_HOUR` 0 - 23 与 `EXTRA_MINUTES` 0 - 59 (缺省为 0) 映射为 `hour` / `minute`, `EXTRA_MESSAGE` 去掉首尾空白并截到 100 个字符作 `label`, `EXTRA_DAYS` (`Calendar` 的星期 1 - 7, 无效值忽略) 规范为 `FREQ=WEEKLY;BYDAY=...`, `EXTRA_VIBRATE` 为 `vibrate`, `EXTRA_RINGTONE` 为 `sound.source` (`silent` 为静音, 其他为铃声 uri); 小时缺失或越界, 分钟越界时打开新建页 (不报错); `EXTRA_SKIP_UI` 为 false (默认) 时打开预填的编辑页 (P3.1 之前为内部对话框 `ClockDialogActivity`, 确定后保存), 为 true 时直接保存, 已有同一钟点, 无自定日期 (T48 自动补上的日期不算), 重复与标签相同 (给出振动与铃声时也相同) 的闹钟则改为启用它而不再添加; 保存与启用都记来源 `intent`; 调用方须持有 `com.android.alarm.permission.SET_ALARM` (同 AOSP 时钟; G8441 的 shell 没有该权限, Xiaomi Pad (HyperOS 2) 的 shell 有) | 与 AOSP 时钟的行为一致, 语音助手重复下达同一闹钟时不堆积; 越界时间按缺失处理也是 AOSP 的做法 | 已固定 (P2.6) |
| T80 | 系统时钟 `DISMISS_ALARM`: 候选为已启用或正在响 (发声, 排队, 贪睡) 的闹钟; 无搜索模式取正在响的, 没有则取全部候选; `android.next` 取正在响的, 否则取最早一次响铃的闹钟; `android.time` (`EXTRA_HOUR` 必需, `EXTRA_MINUTES` 缺省为 0, `EXTRA_IS_PM` 把小于 12 的小时移到下午) 比较钟点 (`at` 闹钟取设备时区的钟点); `android.label` 忽略大小写比较标签; 未知模式, 时间模式没有有效时间, 标签模式没有标签报 `INVALID_OPTIONS`; 匹配多个且模式不是 `android.all` 时由用户在列表中选一个; 正在响或在贪睡的结束本轮 (`by` `user`), 否则下一次响铃在 24 小时内时跳过它 (`skipNext`, 来源 `intent`, 一次性闹钟随之禁用), 更远的不动并提示其时刻, 已跳过的不重复跳过. `SNOOZE_ALARM`: 作用于发声中的响铃 (`by` `user`), `EXTRA_ALARM_SNOOZE_DURATION` 1 - 60 分钟只覆盖本次贪睡时长, 缺省用闹钟自己的; 越界或类型不符报 `INVALID_OPTIONS`; 没有发声的响铃或贪睡被拒 (达到次数上限) 时只提示 | 照搬 AOSP 时钟的提前关闭窗口与多匹配选择, 语音命令不会误关一周以后的闹钟 | 已固定 (P2.6) |

---

## 3. 范围与非目标

版本切分按 D19:

| 版本 | 范围 |
| --- | --- |
| 1.0.0 | 闹钟核心: `alarms` 模块的闹钟方法 (附录 A 第 4 节), 事件 `ring` / `snooze` / `dismiss` / `silence` / `miss` / `skip` / `change` 与两种联动 (附录 A 第 5.1 - 5.3 节; `beforeRing` 除外), `alarms.system` (附录 A 第 9 节, 宿主侧, 不需要插件), 插件接收 `SET_ALARM` / `SHOW_ALARMS` / `DISMISS_ALARM` / `SNOOZE_ALARM` (D21), 插件工具 `alarm_*` (附录 A 第 10 节), 独立界面的闹钟部分 (列表, 新建 / 编辑, 响铃界面, 铃声选择, 权限页, 设置页, 发行历史), 响铃内容 (铃声 / 本地音频 / 朗读 / 渐强 / 独立音量 / 振动), 贪睡, 自动停止, 错过处理 |
| 1.1.0 | 计时器与秒表 (附录 A 第 6 节), `SET_TIMER` / `SHOW_TIMERS` / `DISMISS_TIMER`, 工具 `timer_*`, 计时器与秒表页 |
| 1.2.0 | 节假日 (附录 A 第 7 节, Shared-Assets 新类别 `holiday-calendars`) 与 `beforeRing` (附录 A 第 5.4 节), 工具 `holiday_check` |
| 1.3.0 | 关闭挑战 (附录 A 第 8 节) 与安全阀 |

非目标 (任何版本都不做):

- 世界时钟, 睡眠追踪, 智能唤醒 (按睡眠周期提前), 云同步与跨设备, Wear OS.
- 关机闹钟 (需要厂商接口).
- 读取或修改其他应用 (含系统时钟) 已有的闹钟: 公开接口只有 `alarms.system.next()` 与 Intent.
- 宿主内的任何响铃实现; 未装插件时只有 `alarms.system` 可用.

---

## 4. 目标架构

### 4.1 数据流

```text
 脚本 --alarms.*--> 宿主 alarms 模块 --AIDL (plugin-api/alarm-api)--> 3-Chime Alarm 能力服务
                      |                                                   |
                      |                                                   +- setAlarmClock / 计时器排程
                      |                                                   +- 响铃前台服务 + 全屏响铃界面
                      |                                                   +- 闹钟 / 计时器 / 秒表 / 节假日存储 (设备加密存储)
                      |
                      +- alarms.system --AlarmClock Intent--> 系统时钟应用 (不需要插件)
                      |
                      <-- ALARM_TRIGGER 签名广播 (beforeRing / ring / snooze / dismiss / silence / miss / skip)
                      +- 按宿主侧绑定启动脚本, 事件 -> execArgv.alarm

 MCP Server / plugins.tools.call --插件工具 alarm_* / timer_* / holiday_check--> 3-Chime Alarm
```

### 4.2 宿主侧改动 (仅本地提交)

- `plugin-api/alarm-api`: 常量, key, 限制值, 错误码, AIDL (`IAlarmPlugin`, `IAlarmEventCallback`, `IAlarmProgressCallback`), `consumer-rules.pro`, 协议文档 `docs/dev/alarm-plugin-protocol-v1.md`.
- `core/plugin/alarm/`: 发现 / 绑定 / 退化 (`PLUGIN_UNAVAILABLE` / `PLUGIN_DISABLED` / `PLUGIN_INCOMPATIBLE`), `AlarmTriggerReceiver` (签名权限导出), 宿主侧绑定表与分发器.
- `runtime/api/augment/alarms/`: 脚本模块; `alarms.system` 的 Intent 封装.
- Manifest: `com.android.alarm.permission.SET_ALARM`, `<queries>` 中的 `SET_ALARM` / `SET_TIMER` / `SHOW_ALARMS` / `DISMISS_ALARM` 与插件包名; 插件中心两张表与安装向导条目.
- 文档站 `api/alarms.md`, TypeScript 声明, Ace / Offline Docs, zh-Hans changelog; api-coverage 矩阵登记 `alarms` 根.

### 4.3 契约要点 (P1.1 的依据, 细节在协议文档定稿)

- `IAlarmPlugin` 方法族与脚本 API 一一对应 (附录 A 第 4.1, 6, 7, 8 节): `getInfo` 居首; 闹钟 (`add` / `get` / `list` / `update` / `remove` / `setEnabled` / `skipNext` / `cancelSkip` / `upcoming` / `preview`), 响铃 (`ringing` / `dismiss` / `snooze`), 铃声 (`listRingtones` / `importRingtone(pfd, Bundle)`), 事件 (`registerEventCallback` / `unregisterEventCallback`, 带 "哪些闹钟有绑定, 需要哪些事件" 的提示表, T12), 计时器与秒表, 节假日 (`refreshHolidays` 带 `IAlarmProgressCallback`), 挑战相关字段. 全部方法在 v1 一次定义 (T25).
- 请求与结果为 `Bundle` (T24); `Alarm` / `Timer` / `Ringing` / `Occurrence` 等快照各有键族; 错误以 `AlarmErrorCodes` 返回 (附录 A 第 11 节), 宿主映射为 `alarms.AlarmError`.
- 能力键: `alarmContractVersion` (1), `alarmFeatures` (T25), `alarmPermissions` (精确闹钟 / 通知 / 全屏的当前状态, 供 `capabilities()` 与 `PERMISSION_REQUIRED` 提示).
- 事件: 8 种 (附录 A 第 5.1 节) 的共同字段 `type` / `kind` / `alarmId` / `label` / `scheduledAt` / `firedAt` / `occurrence`; 经 `IAlarmEventCallback` 投递给已注册的宿主进程; 有绑定的闹钟同时发 `ALARM_TRIGGER` 广播 (显式包名, 受 `org.autojs.permission.PLUGIN` 保护, extras 带同一事件 JSON 与 `occurrence`, T11).
- 限制值集中在 `AlarmLimits` (附录 A 第 11 节表), 宿主与插件两侧都校验, 以插件为准.

### 4.4 插件包结构 (建议, P0 / P2 可调)

```text
io.github.supermonster003.autojs6.plugin.three.chime.alarm
  ThreeChimeAlarmApplication
  WakeActivity                         激活协议 (新仓库规范第 6 节)
  service/   ThreeChimeAlarmInfoService (org.autojs.plugin.INFO)
             ThreeChimeAlarmService     IAlarmPlugin.Stub, Bundle <-> core 编解码, 输入边界, 回调表与 linkToDeath
  core/alarm/    Alarm, AlarmSource, AlarmStore (JSON 原子写, 设备加密存储, T21), AlarmRepository (上限, change 通知)
  core/rule/     RecurrenceRule (RRULE 子集解析与规范化, repeatText), NextOccurrence (lib-recur 或自写, T10), TimeZonePolicy (T9)
  core/schedule/ AlarmScheduler (setAlarmClock, 只排下一次, T3), Rescheduler (T8 的六个时机), MissedPolicy (T16), 接收器 (directBootAware, T7)
  ring/          RingService (前台, T6), RingPlayer (USAGE_ALARM, 渐强, 独立音量, T14), RingVibrator, RingSpeaker (TTS 四种 mode), SnoozePolicy, AutoSilence
  host/          HostEventSink (IAlarmEventCallback 分发), HostTriggerSender (ALARM_TRIGGER 广播, T11), BindingHints (T12)
  intent/        ClockIntentActivity (SET_ALARM / SHOW_ALARMS / DISMISS_ALARM / SNOOZE_ALARM; 1.1.0 加三个 TIMER, D21)
  tools/         PluginToolService (alarm_*; 1.1.0 timer_*; 1.2.0 holiday_check; 短名 chime, T17)
  timer/         (1.1.0) TimerStore, TimerScheduler, Stopwatch
  holiday/       (1.2.0) HolidayData, HolidayRefresh, HolidayFilter, BeforeRing
  challenge/     (1.3.0) MathChallenge, ShakeChallenge, TypeChallenge, ScriptCondition, EmergencyDismiss
  ui/            alarms/ (列表, 编辑, RRULE 编辑器), ring/ (全屏响铃), ringtones/, permissions/, settings/ (含发行历史), timers/ stopwatch/ (1.1.0)
```

### 4.5 身份派生表

| 项 | 值 |
| --- | --- |
| `{PROJECT_NAME}` | `AutoJs6-Plugin-Three-Chime-Alarm` |
| `{ROOT_PROJECT_NAME}` | `autojs6-plugin-three-chime-alarm` |
| `{APP_NAME}` (不可翻译) | `3-Chime Alarm` |
| `{APPLICATION_ID}` / namespace / Kotlin 包 | `io.github.supermonster003.autojs6.plugin.three.chime.alarm` (T2) |
| `{PLUGIN_ID}` / `{PLUGIN_ENGINE}` / `{PLUGIN_VARIANT}` | `three-chime-alarm` / `alarm` / `default` |
| 身份类 | `ThreeChimeAlarmContract`, `ThreeChimeAlarmService`, `ThreeChimeAlarmInfoService`, `ThreeChimeAlarmApplication`, `Theme.ThreeChimeAlarm`, 测试类同名后缀; 其他组件按 4.4 命名 (`RingService`, `RingActivity`, `ClockIntentActivity`, `WakeActivity`) |
| 插件中心身份服务 | `ThreeChimeAlarmInfoService`, action `org.autojs.plugin.INFO`, category `three-chime-alarm` |
| 能力服务 | `ThreeChimeAlarmService`, action `org.autojs.plugin.ALARM`, category `alarm`, 受 `org.autojs.permission.PLUGIN` 保护 |
| 前台服务 | `RingService`, `foregroundServiceType` 按 T6 实测结论 (`systemExempted` 或 `mediaPlayback`), 不导出; 1.1.0 计时器倒计时通知复用 |
| `{CAPABILITY_API}` | `alarm-api` (宿主 `plugin-api/alarm-api`, AAR 复制到 `libs/alarm-api.aar`; 另需 `libs/common-plugin-api.aar`; 全部登记在 `locks/host-api-aars.lock`) |
| 宿主契约包 | `org.autojs.plugin.alarm.api` (`AlarmPluginActions`, `AlarmPluginIds`, `AlarmContract`, `AlarmCapabilityKeys`, `AlarmFeatures`, `AlarmLimits`, `AlarmOptionKeys` / `AlarmResultKeys` / `AlarmEventKeys` / `AlarmErrorCodes`, AIDL 三个接口) |
| 宿主桥接包 | `org.autojs.autojs.core.plugin.alarm` (`AlarmPluginHost`, `AlarmBindingStore`, `AlarmTriggerReceiver`, `AlarmTriggerDispatcher`) |
| 联动广播 | action `org.autojs.autojs6.action.ALARM_TRIGGER`, 受 `org.autojs.permission.PLUGIN` 保护, 事件进 `engines.myEngine().execArgv.alarm` (T11) |
| 脚本 API | `alarms` (`$alarms` 同对象), 子命名空间 `alarms.system` / `alarms.ringtones` / `alarms.timers` / `alarms.stopwatch` / `alarms.holidays`; 宿主目录 `runtime/api/augment/alarms/` |
| 插件工具 | 短名 `chime`, 工具名见附录 A 第 10 节 |
| `{REQUIRES_HOST_VERSION}` | 临时值 6641 (T33; 基线 6640 为宿主 `e195d5c7f8`, `VERSION_BUILD` 即 versionCode, 不含 P1.1 - P1.3 的宿主提交), P6.4 回填为包含 P1 全部宿主提交的最终值 |
| 向导目录 | `PluginInstallWizardCatalog` `official("three.chime.alarm")`, 回退标题 `Three Chime Alarm`, 分类 `TOOLS` (T23) |
| `plugin_description` (zh-Hans) | `闹钟, 计时器与秒表, 支持日历重复规则, 节假日跳过与响铃时联动脚本` (句尾无点号, 不含 "AutoJs6"; P0.1 定稿, 10 语言见 `values*/strings.xml`) |
| GitHub 仓库描述 | `Alarm clock, timers and stopwatch with calendar recurrence rules, holiday skipping and script hooks for AutoJs6` (功能性描述, 不复述名称) |
| 发布文件名 | `autojs6-plugin-three-chime-alarm-v{VERSION_NAME}-{CRC32}.apk` (单 APK, T20) |
| 索引清单 | `release-manifests/io.github.supermonster003.autojs6.plugin.three.chime.alarm/<versionCode>.json` (`supportedAbis` 空数组) |
| 图标 | `.python/icons/three-chime-ic-launcher-light.png` / `-dark.png` (1024 px 黑白源图, P0.3 生成候选, 维护者选定), Icon Studio 合成 |
| 许可证 | 源码 MPL-2.0; `lib-recur` 许可与节假日数据集许可在 P0.2 核对后写入 `THIRD_PARTY_NOTICES.md` |

---

## 5. 阶段总览

| 阶段 | 版本 | 主题 | 状态 |
| --- | --- | --- | --- |
| P0 | 1.0.0 | 仓库骨架: 构建链与身份, 评估 (RRULE 库, 节假日数据源, 前台服务类型) 与单 APK 策略, 图标, 测试与 CI | 完成 (P0.1 - P0.4) |
| P1 | 宿主 (6.8.0 之后的版本) | 宿主契约 `alarm-api`, `AlarmPluginHost` / `AlarmTriggerReceiver` / 绑定表, 注册与协议文档, 脚本 API `alarms`, `alarms.system`, 样例 | 完成 (P1.1 - P1.6; P1.4 第 2 条的设备补证在 P2.7 完成) |
| P2 | 1.0.0 | 闹钟核心: 存储, 重复规则引擎, 排程与恢复, 响铃, Binder 服务端与宿主联动, 系统时钟 Intent 接收, 契约一致性测试 | 进行中 (P2.1 - P2.3, P2.5, P2.7 完成; P2.4 / P2.6 只缺 Xiaomi Pad 的设备证据, 等维护者解锁并授通知权限) |
| P3 | 1.0.0 | 独立界面: 闹钟列表与编辑, 响铃界面, 铃声选择, 权限页, 设置页与发行历史, 图标核对与截图 | 未开始 |
| P4 | 1.0.0 | 插件工具 `alarm_*` 与 MCP Server 实机调用 | 未开始 |
| P5 | 1.0.0 | 验证: AVD 矩阵, 真机矩阵, 时间与系统事件场景, ColorOS 激活, 联动边界, 基线 | 未开始 |
| P6 | 1.0.0 | 文档, 声明, 四仓库同步, README / changelog / 截图, 发布与索引登记 | 未开始 |
| P7 | 1.1.0 | 计时器与秒表 | 未开始 |
| P8 | 1.2.0 | 节假日与 `beforeRing` | 未开始 |
| P9 | 1.3.0 | 关闭挑战 | 未开始 |

依赖顺序:

```text
P0 (插件骨架, 含 P0.2 评估)          P1.1 - P1.3 (宿主契约, 桥接, 注册) 可与 P0 并行 (不同仓库; 附录 A 与 4.3 为共同依据)
 -> P2 (闹钟核心)                    依赖 P0 与 P1.1 (libs/alarm-api.aar 来自宿主契约模块)
     P1.4 - P1.6 (脚本 API, system, 样例) 依赖 P1.1 - P1.3; P1.5 不依赖插件, 可最早做; 联调需要 P2.5 的 debug 包
     -> P3 (界面)                    依赖 P2.1 - P2.4
     -> P4 (工具)                    依赖 P2.5; 与 P3 并行 (不同目录)
         -> P5 (验证)                依赖 P1 - P4 全部; P5.1 的 CI job 骨架在 P0.4 先搭
             -> P6 (文档, 发布)      依赖 P5 全部通过
                 -> P7 (1.1.0) -> P8 (1.2.0) -> P9 (1.3.0)   P8.1 数据管线可在 Q8 拍板后提前做
```

发布门禁 (每个版本的 "(发布)" 条目都指向本清单):

- R1 `version.properties` 的 `VERSION_NAME` 更新, `VERSION_BUILD` = 提交数 + 1; 10 种语言 `.changelog/lang_*.json` 补齐该版本的 `hint` / `feature` / `fix` / `improvement` / `dependency`, 日期为发布当日.
- R2 `.readme/lang_*.json` (功能, 安装, 权限理由, 脚本示例, 系统时钟对接, 兼容性与所需宿主版本, 常见问题, 第三方致谢), `plugin_instruction` 10 语言, `strings.xml` 描述与 README 一致性校验通过; 界面有变化时重采 `docs/images/screenshots` (XQ-DQ72, 1096 x 2560, 明暗各一).
- R3 `py D:/idea-projects/prepush_check.py` 通过 (文档校验, `check_repository.py`, 构建号容差, Gradle 单元测试); 附录 D 设备矩阵中每台在线设备完成冒烟 (安装, 激活, 授权 (精确闹钟 / 通知 / 全屏), `alarms.add` 1 分钟后响, 响铃界面贪睡与关闭, `alarms.on('ring')` 收到事件, 绑定脚本被启动且 `execArgv.alarm` 正确, 重启后闹钟仍在并按时响, `alarms.system.setAlarm` 建进系统时钟), 记录型号 / API / APK 摘要 / 触发偏差.
- R4 release APK 经 `appendDigestToReleasedFiles` 收集 (单 APK), APK 内无 `lib/` 目录, 生成 `SHA256SUMS`, 创建轻量 tag `vX.Y.Z` 与 GitHub Release (APK + SHA256SUMS), 复核下载产物哈希.
- R5 官方索引: `official-repositories.json` 加入 `AutoJs6-Plugin-Three-Chime-Alarm` (首次, 数量以索引仓库当前值 + 1 为准), `release-manifests/.../<versionCode>.json` 登记 (`supportedAbis` 空数组, `requiresHostVersion` 与实际一致), `catalog-icons` 图稿登记, 运行 `tools/generate_official_plugin_index.py` 与索引仓库的 `generate_markdown.py`; 注意索引仓库的并发 (推送后 CI 会重生成, 变基后用自己的生成文件逐条比对再推).

---

## P0: 仓库骨架

目标: 可构建, 可测试, 能被宿主插件中心识别的空壳插件 (能力服务只应答 `getInfo`), 满足 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 全部 MUST; 完成三项评估 (T6, T10, Q8) 并把结论写回本文. 设备: 任一真机或 AVD (安装与插件中心识别); T6 实测需 API 35 / 36.

### P0.1 构建链与身份 (T1, T2, T4)

- [x] (插件) 按 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 第 2 节占位表生成骨架: `settings.gradle.kts` (平台插件 `1.9.1` 或建仓当日已发布的更高稳定版, 位于 `includeBuild` 之前), `build-logic/` (从 OpenCC / Macaw 当前实现复制后精简), `app/build.gradle.kts` (`globalApplicationId`, `requiredHostVersion` 临时值 6640, 无 `splits`, `buildFeatures.aidl = true`), `version.properties` (`VERSION_NAME=1.0.0`, `VERSION_BUILD` = 提交数 + 1, compileSdk / targetSdk 与 3-Macaw Speech 当前值一致, minSdk 24), `gradle.properties`, `AGENTS.md` 裁剪版, `LICENSE` (MPL-2.0), `THIRD_PARTY_NOTICES.md`, `.editorconfig`; 从宿主复制 `.gitignore` / `sign.properties` / `app/sm003.jks` (后两者被忽略, `git check-ignore` 验证). 证据: 提交 hash; 规范 5.1 的 Temurin 验收命令 (`:app:assembleDebug :app:testDebugUnitTest`) 通过, 日志只有一段版本决策. 证据 (E1 + E2, 2026-11-05): 提交 `9a63e7a`; Temurin 21.0.12.1+1 验收命令 `:app:assembleDebug :app:testDebugUnitTest` 通过 (P0.2 复跑同样通过), 日志只有一段 "Version information for IDE platform and Gradle plugins" (AGP 9.3.3, Kotlin 2.3.20, Gradle 9.5.0); `sign.properties` / `app/sm003.jks` / `local.properties` 经 `git check-ignore` 确认被忽略.
- [x] (插件) Manifest: `ThreeChimeAlarmApplication`, `WakeActivity` 与 `org.autojs.plugin.WAKE_ACTIVITY` meta-data, `ThreeChimeAlarmInfoService` (`org.autojs.plugin.INFO` / `three-chime-alarm`), `ThreeChimeAlarmService` (`org.autojs.plugin.ALARM` / `alarm`, `org.autojs.permission.PLUGIN`; 骨架期返回只携带 `org.autojs.plugin.alarm.api.IAlarmPlugin` 描述符的占位 Binder, 拒绝全部事务, 同 Macaw P0 做法, 契约在 P1.1 才有, 插件内不复制 AIDL), `RingService` (前台, 不导出, 类型按 P0.2 结论), `RingActivity` (`showWhenLocked` / `turnScreenOn`), `ClockIntentActivity` (D21 的四个 intent-filter), 启动器入口 (D7), 权限 T4 (`USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` `maxSdkVersion=32`, `USE_FULL_SCREEN_INTENT`, `POST_NOTIFICATIONS`, `VIBRATE`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE` 及类型权限), 调度与响铃组件 `directBootAware` (T7), `allowBackup="false"`, `<queries>` 宿主包名与 `android.intent.action.TTS_SERVICE`. 证据: 提交 hash; 宿主插件中心列出插件并显示 10 语言描述 (任一设备). 证据 (E1 + E3 + E4, 2026-11-05): 提交 `9a63e7a`; debug APK (versionCode 3) 经 `device_smoke.py` 安装后, 宿主插件中心在 G8441 (API 28, 宿主 5935, 英文, 720x1280) 与 XQ-DQ72 (API 33, 宿主 6623, 简体中文, 1096x2560) 以及 `AVD_API_24` (宿主 5835, 英文, 1440x2560) 都列出 3-Chime Alarm 并显示对应语言的描述; 10 语言 `plugin_description` 由 `StringResourceParityTest` 覆盖; 新装行标 "启用失败" 是因为这些宿主低于 requiresHostVersion (宿主 `PluginCenterFragment.scheduleAutoEnableAfterInstall` 的版本判断), 骨架期预期如此, 在 P1 宿主构建上复核; 截图在 共享笔记目录 `side-session-notes/local_daee5399-90f5-4c35-8d70-f8150ec53e21/chime-p0-screenshots/` (`plugin-center-*.png`).
- [x] (插件) 10 语言 `strings.xml` (`plugin_name` 不可翻译为 `3-Chime Alarm`, `plugin_description`, `plugin_instruction`, 通知渠道文案), `.readme/lang_*.json` 与 `.changelog/lang_*.json` 初始条目, `generate_markdown.py` 生成 README 10 语言; ASCII 标点, 阿拉伯语品牌名 FSI / PDI 包裹. 证据: `check_repository.py` 与 `check_markdown.bat` 通过. 证据 (E1 + E2, 2026-11-05): 提交 `9a63e7a`; `check_repository.py` 与 `check_markdown.bat` 通过 (`MARKDOWN_OK languages=10 artifacts=36`), `StringResourceParityTest` 6 例与 `ApplicationTextPunctuationTest` 2 例通过.
- [x] (插件) `PluginInfo` 元数据映射与 JVM 测试 (名称, 描述, 作者, 三个身份字段, 包版本, 日期, `supportedAbis` 为显式空数组, 最低宿主版本, `alarmContractVersion=1`, `alarmFeatures=[]`; 键名暂存于 `ThreeChimeAlarmContract`, P2.5 改用契约常量). 证据: 测试类名与用例数. 证据 (E1 + E2, 2026-11-05): 提交 `9a63e7a`; `PluginRuntimeInfoTest` 5 例通过 (同轮 `ManifestContractTest` 10 例, `HostApiAarLockTest` 5 例).

### P0.2 评估与单 APK 策略 (T6, T10, T20, T34, 附录 B Q8)

- [x] (插件) RRULE 库评估 (T10): 核对 dmfs `lib-recur` 的 Maven 坐标与版本, 许可, 方法数 / dex 体积, 对 `BYSETPOS` / 负序号 / 月末 / `EXDATE` / `COUNT` / `UNTIL` / 时区的支持, API 24 可用性 (不依赖 `java.time` 以外的 API 26+ 类); 合适则接入并写 `THIRD_PARTY_NOTICES.md`, 不合适则按 T10 自写子集 (范围按附录 A 第 4.3 节). 证据: 评估结论写进 T10 行 (版本, 许可, 体积); 一个 JVM 测试跑通 "每月最后一个周五" 与 "从某日起每 3 天". 证据 (E1 + E2, 2026-11-05): 提交 `aeb162e`; 结论写进 T10 行 (0.17.1, Apache-2.0, 两个 Apache-2.0 依赖, 全量 dex 212,660 B, R8 实测 APK +29,755 B); `LibRecurEvaluationTest` 4 例通过 (每月最后一个周五的负序号与 `BYSETPOS` 两种写法, 从某日起每 3 天跨月, `COUNT` / `UNTIL`, `EXDATE`); `THIRD_PARTY_NOTICES.md` 新增 "Recurrence rule libraries" 一节 (含 jems2 的 NOTICE), 10 种语言 changelog 加一条依赖.
- [x] (插件) 节假日数据源评估 (供 Q8 拍板, 1.2.0 用): python-holidays, date-holidays 及其他候选的许可 (是否允许再分发), 地区数, 是否含中国大陆补班日 (调休), 更新频率, 构建期生成紧凑数据的可行性与体积估算 (按地区 / 年份). 证据: 评估表写进附录 B Q8 的选项说明 (关键数字), 本阶段不接入. 证据 (2026-11-05): 评估表与关键数字写进附录 B Q8 (推荐 (a) python-holidays; 实测 holidays 0.108 wheel, date-holidays 3.38.0, holiday-cn master), 本阶段未接入.
- [x] (测试) 响铃前台服务类型实测 (T6): 在 API 35 (`AVD_API_35_16K` 或 Xiaomi Pad / OnePlus) 与 API 36 (`AVD_API_36.1_16K`) 上, 从 `setAlarmClock` 触发的 `BroadcastReceiver` (应用处于后台 / 被杀后) 分别启动 `systemExempted` 与 `mediaPlayback` 类型的前台服务, 记录是否抛 `ForegroundServiceStartNotAllowedException`, 通知与音频焦点行为; API 34 以临时 AVD 实测 (结束后删除) 或以 35 覆盖并注明. 证据: 结论写进 T6 行 (选定类型 + 各 API 结果). 证据 (E3, 2026-11-05): 结论写进 T6 行 (选定 `systemExempted`, 4 个 API 的启动 / 焦点 / 通知结果); 设备为 API 34 临时 AVD (测完已删除), `AVD_API_35_16K`, `AVD_API_36.1_16K`, `AVD_API_37.1_16K` (后三者只读启动); 每个 API 跑 2 种类型 x 4 个场景, 另以全新安装 + `FOREGROUND_SERVICE_IMMEDIATE` 复测首个通知 (API 34 / 36 / 37), API 35 复跑一轮记录焦点被拒时的播放器状态; 触发偏差 3 - 1386 ms, 前台服务启动被拒 0 次; 新派生 T35 (焦点被拒照常响铃).
- [x] (插件) 单 APK (T20): `appendDigestToReleasedFiles` 按 3-Block Compose 形态实现 (`{ROOT_PROJECT_NAME}-v{VERSION_NAME}-{CRC32}.apk`), 任务内校验 APK 无 `lib/` 目录与 `.so`; `PluginInfo.supportedAbis = emptyArray()`; `AGENTS.md` 与 README 记录 "无原生库, 不拆 ABI, 16 KB 页校验不适用". 证据: release APK 文件名与 `unzip -l` 结果. 证据 (E1 + E5 预检, 2026-11-05): 提交 `aeb162e`; `releases/autojs6-plugin-three-chime-alarm-v1.0.0-53a16e43.apk` (versionCode 3, 324,761 B, SHA-256 前 16 位 `f221f7783817ea2b`), `unzip -l` 78 个条目, 无 `lib/` 与 `.so`, 任务内的签名 / 单 APK / 无原生库校验通过; `supportedAbis` 显式空数组 (`PluginRuntimeInfoTest`); `AGENTS.md` 与 README 已记录; 新派生 T34 (引入 Compose 后的做法).

### P0.3 图标

- [x] (插件) 生成 2 - 3 个候选的黑白亮暗源图 (`.python/icons/three-chime-ic-launcher-light.png` / `-dark.png`, 1024 px; 图案体现闹钟 / 铃, 与 Three 系列既有图标可区分), 先以第一候选接入, 维护者在看板 "维护者待办" 里改选时换源图重生成; `.icons/recipe.json` 双源 `grayscale` 配方 (亮 #272727 / 暗 #D8D8D8, 背景 #FAFAFA / #212121, `alphaMode: alpha`), `generate_icon_studio.py` + `icon_studio_runtime.py` 生成 `mipmap` 全套与 `ic_launcher.png`, `--check` 通过; `icon-studio.yml` 工作流. 证据: 提交 hash, `--check` 输出, 候选图路径, 启动器与插件中心实机截图 (明暗各一). 证据 (E1 + E4, 2026-11-05): 源图, 配方与 mipmap 在提交 `9a63e7a`, `icon-studio.yml` 在提交 `21fa07c`; `generate_icon_studio.py --check` 输出 "Verified 28 Icon Studio resources" (Pillow 12.3.0); 候选: `.python/icons/three-chime-ic-launcher-{light,dark}-candidate-1.png` (双铃帽闹钟, 已接入), `-candidate-2.png` (手摇铃 + 三道声弧), `-candidate-3.png` (横杆上的三根管钟), 对照图 `icon-candidates.png`; 启动器: G8441 系统亮色 `launcher-BH900ASK9E-light.png` (720x1280), XQ-DQ72 系统暗色 `launcher-QV770340J7-dark.png` (1096x2560, 显示暗色变体); 插件中心: 亮 `plugin-center-BH900ASK9E-hostlight.png` / `plugin-center-QV770340J7-hostlight.png`, 暗 `plugin-center-AVD_API_24-hostdark.png` (插件中心跟随宿主主题, 两台真机的宿主设为亮色, 未改维护者的宿主设置); 截图都在 共享笔记目录 `side-session-notes/local_daee5399-90f5-4c35-8d70-f8150ec53e21/chime-p0-screenshots/`. 改选图标列入看板 "维护者待办". 图标选定 (2026-11-05 15:25 维护者拍板): 候选 1 双铃帽闹钟, 保持已接入状态, 无需换源图重生成.

### P0.4 测试与 CI

- [x] (插件) JVM 单元测试骨架 (契约常量镜像, `PluginInfo`, `ManifestContractTest`: Wake Activity / exported / permission / action / category / meta-data), instrumentation 骨架 (按正式 action + category 发现且只命中一个 Service, 显式绑定并校验 descriptor, Wake Activity 契约); `.github/workflows/build.yml` (构建, 单元测试, androidTest APK, release 产物校验), `markdown.yml`; `connected-avd` job 骨架 (用例在 P5.1 填), 默认只在 `master` 推送与 PR 运行. 证据: 提交 hash, 本地 `gradlew test` 通过; 远端运行待首次推送 (GitHub 建仓为维护者待办). 证据 (E1 + E2, 2026-11-05): 提交 `0a0bc52`; 本地 `:app:testDebugUnitTest` 通过 (6 个类 32 例: `ManifestContractTest` 10, `PluginRuntimeInfoTest` 5, `HostApiAarLockTest` 5, `StringResourceParityTest` 6, `ApplicationTextPunctuationTest` 2, `LibRecurEvaluationTest` 4), `:app:lintDebug` 0 个错误 11 个警告, `:app:assembleDebugAndroidTest` 通过; instrumentation `ThreeChimeAlarmContractTest` 4 例在 `AVD_API_24` (只读启动, 宿主 5835) 通过 (`OK (4 tests)`): Wake Activity 契约, INFO 与 ALARM 各只命中一个服务, 显式绑定与描述符, 以宿主事务号经 Parcel 编组的 `getInfo()`, 占位 Binder 拒绝首个事务; `build.yml` (verify-build: 仓库检查, 单元测试, debug / androidTest / release APK, lint, release 单 APK 与无原生库校验; connected-avd: API 24 / 33 经 emulator-runner 跑 `connectedDebugAndroidTest`, 用例在 P5.1 补) 与 `markdown.yml`, 触发都是 `master` 推送, PR 与手动运行; 远端运行待首次推送 (GitHub 建仓为维护者待办).
- [x] (插件) `.python/device_smoke.py` 骨架 (从 Macaw 裁剪: 安装, 激活, 调用 `getInfo`), 输出只保留每项通过 / 失败与关键数值, 不打印完整工具清单. 证据: 脚本在任一设备上跑通. 证据 (E3, 2026-11-05): 提交 `0a0bc52`; debug APK (versionCode 3, 2,915,029 B, SHA-256 前 16 位 `0d2fd686977c4de1`) 在 `AVD_API_24` (API 24, 宿主 5835, getInfo 114 ms), G8441 (API 28, 宿主 5935, 65 ms), XQ-DQ72 (API 33, 宿主 6623, 21 ms) 上都输出 `SMOKE PASS` (安装, 两组发现各 1 个服务, Wake 启动日志, getInfo 身份与能力键, 占位 Binder 描述符且拒绝首个事务, `--screenshot` 打开插件中心截图); 宿主低于 requiresHostVersion 时只报告不判失败.

---

## P1: 宿主契约, 联动与脚本 API

目标: 宿主能发现, 绑定并以 `alarms` 模块调用插件; 联动广播接收器与宿主侧绑定表就绪; `alarms.system` 不依赖插件即可用; 插件未安装 / 未启用 / 不兼容时合理退化. 落点宿主仓库, 只落本地提交. 联调设备: 任一真机 (与 P2 的 debug 包).

### P1.1 契约模块 `plugin-api/alarm-api` (T11 - T13, T15, T19, T24, T25)

- [x] (宿主) 包 `org.autojs.plugin.alarm.api`: `AlarmPluginActions.ALARM = "org.autojs.plugin.ALARM"`, `AlarmPluginIds` (官方 ID / 包名, `ENGINE = "alarm"`, `CATEGORY = "alarm"`), `AlarmContract.CONTRACT_VERSION = 1`, `AlarmCapabilityKeys` (`alarmContractVersion`, `alarmFeatures`, `alarmPermissions`), `AlarmFeatures`, `AlarmLimits` (附录 A 第 11 节), `AlarmOptionKeys` / `AlarmResultKeys` / `AlarmEventKeys` / `AlarmErrorCodes`; AIDL `IAlarmPlugin` (`getInfo` 居首, 1.0.0 - 1.3.0 全部方法, T25), `IAlarmEventCallback` (oneway), `IAlarmProgressCallback`; `Bundle` 传输 (T24); `consumer-rules.pro`; 协议文档 `docs/dev/alarm-plugin-protocol-v1.md` (方法, Bundle 键, 限制, 错误, 事件去重 `occurrence`, `ALARM_TRIGGER` 广播的 extras, 未解锁 / 宿主不可用时的行为). 证据: 宿主提交 hash; AAR 复制到插件 `libs/` 并登记 `locks/host-api-aars.lock` (P2 开始时做). 证据 (E1, 2026-11-05): 宿主提交 `0924ecefcc` (仅本地); AIDL `IAlarmPlugin` 32 个方法, 顺序已冻结 (`AlarmAidlOrderTest`); 协议文档 `docs/dev/alarm-plugin-protocol-v1.md`; AAR `alarm-api-release.aar` 128,727 B (SHA-256 前 16 位 `f2727527de88f981`); P2 开工时已暂存为插件 `libs/alarm-api.aar` 并登记 `locks/host-api-aars.lock` (与 `common-plugin-api.aar` 同取自 `0924ecefcc`; 隔离克隆在该提交重建的 `classes.jar` 相同, 只有 `proguard.txt` 行尾不同, 见 `libs/README.md`); 派生 T26, T27, T29, T30.
- [x] (宿主) 契约 JVM 测试: 常量快照, `Bundle` 编解码往返 (`Alarm` / `Timer` / `Ringing` / 事件), 限制值. 证据: 测试类名与用例数. 证据 (E2, 2026-11-05): 宿主提交 `0924ecefcc` (仅本地); `:plugin-api:alarm-api:testDebugUnitTest` 18 例通过 (`AlarmContractTest` 8, `AlarmCodecTest` 9, `AlarmAidlOrderTest` 1).

### P1.2 `AlarmPluginHost`, `AlarmTriggerReceiver` 与绑定表 (T11, T12, T13, T16)

- [x] (宿主) `core/plugin/alarm/`: `AlarmPluginHost` (以 `SpeechPluginHost` 为模板: 发现, 绑定, 超时, 退化三码, `requireFeature`), `AlarmBindingStore` (闹钟 id -> 脚本绝对路径 + 事件集合 + `leadSeconds`; 持久化; 删除闹钟时同步清理; 每闹钟上限 5), `AlarmTriggerReceiver` (Manifest 接收器, exported, 受 `org.autojs.permission.PLUGIN` 保护, action `org.autojs.autojs6.action.ALARM_TRIGGER`; 形态同 `MailTriggerReceiver`, 动态接收器永不注册该 action), `AlarmTriggerDispatcher` (按 (`occurrence`, 事件, 脚本) 去重; 按绑定启动脚本, 事件 JSON 经 `ExecutionConfig.setArgument("alarm", ...)` 成为 `execArgv.alarm`; 设备未解锁时不启动并回传 `scriptSkipped: 'locked'`; 脚本文件不存在时跳过并记日志), 实时监听的回调注册 / 注销 (最后一个监听者退出时注销, T13), 向插件同步 "有绑定的闹钟与事件集合" 提示表 (T12). 证据: 宿主提交 hash; JVM 测试 (去重, 绑定表上限与清理, 退化三码). 证据 (E1 + E2, 2026-11-05): 宿主提交 `ba52b806a0` (仅本地); `AlarmBindingTableTest` 7, `AlarmTriggerDispatcherTest` 6, `AlarmHostLogicTest` 10 与 `StaticReferenceGuardTest` 9 通过 (静态引用白名单 +2 行); 绑定存于宿主 `filesDir/alarm/bindings.json`; 派生 T28, T31, T32.

### P1.3 注册, 协议文档, changelog 与版本回填 (T23)

- [x] (宿主) `InstalledPluginRepository` 两张表加入 `AlarmPluginHost.ACTION_ALARM`; `PluginInstallWizardCatalog` 加 `entry(official("three.chime.alarm"), "Three Chime Alarm", TOOLS)`; `AndroidManifest.xml`: `<queries>` 加插件包名与 `SET_ALARM` / `SET_TIMER` / `SHOW_ALARMS` / `SHOW_TIMERS` / `DISMISS_ALARM` / `SNOOZE_ALARM` / `DISMISS_TIMER` 的 intent, `uses-permission com.android.alarm.permission.SET_ALARM` (D8); R8 规则 (由 `alarm-api` 的 `consumer-rules.pro` 保留契约类); 相关 JVM 测试 (两张表, 向导条目数 + 1). 证据: 宿主提交 hash, 测试通过. 证据 (E1 + E2, 2026-11-05): 宿主提交 `8b24e2c6b4` (仅本地); `PluginCenterAlarmRegistrationTest` 7 + `PluginInstallWizardCatalogTest` 5 及回归通过 (与 P1.2 同一轮共 59 例), 向导条目 50 -> 51; `<queries>` 加插件包名, ALARM 服务 intent 与 7 个系统时钟 intent, `SET_ALARM` 权限原已声明.
- [x] (宿主) zh-Hans changelog 条目 (`alarms` 模块, `alarms.system`, 联动; 其他语言由维护者同步); 协议文档入 `docs/dev/` 目录索引; 插件 `requiresHostVersion` 临时值 = 本阶段最后一笔宿主提交后的 `VERSION_BUILD` (P6.4 回填为最终值). 证据: 提交 hash. 证据 (E1, 2026-11-05): 宿主提交 `8b24e2c6b4` (仅本地); zh-Hans 6.8.1 `feature` 3 条并生成 (其他 9 种语言为维护者待办); 协议文档不入索引 (附录 B Q10 已决 (a), `docs/dev/` 没有目录索引); 临时 `requiresHostVersion` 6641 (T33), 插件 `app/build.gradle.kts`, `ThreeChimeAlarmContract` 与 `.readme/common.json` 已同步.

### P1.4 脚本 API `alarms` (D4, D5, D10, D15, 附录 A 第 4 - 5 节, 附录 B Q7)

- [x] (宿主) `runtime/api/augment/alarms/` (实际位置 `runtime/api/modules/alarms/`, 见 T36): `isAvailable` / `capabilities` / `openPlugin`; `add` / `get` / `list` / `update` / `remove` / `setEnabled` / `skipNext` / `cancelSkip` / `upcoming` / `preview` / `ringing` / `dismiss` / `snooze`; `on` / `once` / `off` / `currentEvent`; `bind` / `unbind` / `bindings`; `ringtones.list` / `ringtones.getDefault`; `alarms.AlarmError` (`code`, `hint`); `$alarms` 别名; 选项规范化与校验在宿主侧先判 (`INVALID_OPTIONS` 按键名拒绝未知键, `INVALID_RULE` 写明不支持的部分, 星期数组简写转 RRULE, 相对路径按脚本目录解析, 星期编号 0 - 6, 铃声文件以只读 pfd 传给插件 (T15)); `Alarm` 快照对象带方法; `on` 让脚本保持运行并在退出时注销 (同 `events.on`). 不改 `tasks` 模块 (D4). Node / Bun 运行时是否提供按 Q7. 证据: 宿主提交 hash; JVM 测试 (选项规范化, RRULE 简写与结构化规则转字符串, 错误码映射); 设备测试 (与 P2.5 debug 包联调: `add` 1 分钟后 `ring` 事件到脚本, `bind` 的脚本被启动). 证据 (E1 + E2, 2026-11-05, chime-p1-script): 宿主提交 `4226933503` (仅本地); JVM 测试 `AlarmRepeatRulesTest`, `AlarmScriptOptionsTest`, `AlarmsJsErrorsTest` 与模块门禁 (`ScriptModuleOrderTest`, `ScriptModuleInstallsTest`, `ScriptModuleArityTest`, `ScriptSurfaceGateTest`, `StaticReferenceGuardTest`) 通过; 设备联调 (`ring` 事件到脚本, `bind` 的脚本被启动) 需要 P2.5 的 debug 包, 在 P2.7 补证; 派生 T36 - T39, T42 - T45.
- [x] (宿主) 退化: 缺插件 `PLUGIN_UNAVAILABLE` 并带安装向导提示 (复用 `error_missing_required_plugin_for_module_label`), 未启用 / 未授权 `PLUGIN_DISABLED`, 版本不足或不应答契约 (如 P0 骨架) `PLUGIN_INCOMPATIBLE`; 宿主不含任何响铃实现 (D3). 证据: 宿主提交 hash; 任一设备上卸载 / 禁用插件后的三种错误. 部分证据 (2026-11-05, chime-p1-script): 宿主提交 `4226933503`; `PLUGIN_UNAVAILABLE` (带安装向导提示) 在 OnePlus 63751f63, XQ-AT72 QV710AF65F, Xiaomi Pad 968e9f18 上验证; `PLUGIN_DISABLED` / `PLUGIN_INCOMPATIBLE` 目前只有 JVM 映射测试, 设备触发在 P2.7 补齐后勾选. 证据补齐 (E3, 2026-11-05, chime-p2 P2.7): XQ-AT72 (API 31, 宿主 6641) 上插件中心开关关闭后 `PLUGIN_DISABLED` (带提示), 装 `requiresHostVersion` 6642 的变体后 `PLUGIN_INCOMPATIBLE` (带提示), 均由 `device_smoke.py --contract` 触发; 未装插件时 `PLUGIN_UNAVAILABLE` 同批再验.
- [x] (宿主) api-coverage 矩阵登记 `alarms` 根 (函数级, 随宿主提交一并登记; 做法同 imgui Q14). 证据: 矩阵提交 hash. 证据 (E1, 2026-11-05, chime-p1-script): 随宿主提交 `4226933503` 登记 `alarms` 35 个导出 (函数级); 文档 / d.ts / Ace 名称门以 `policy.json` 的 `deferredNameGate` 推迟到 P6.1 / P6.2 (附录 B Q11).

### P1.5 系统时钟层 `alarms.system` (D8, D23, 附录 A 第 9 节, 附录 B Q9)

- [x] (宿主) `setAlarm` / `setTimer` / `showAlarms` / `showTimers` / `dismissAlarm` / `snoozeAlarm` / `dismissTimer` / `next` / `handlers`: `AlarmClock` Intent 封装 (`days` 转 `Calendar` 常量, `skipUi` 默认 `true` (D23), `package` 转显式组件, 未指定 `package` 时的解析按 Q9); 低于所需 API 报 `UNSUPPORTED`, 无应用可处理报 `NO_HANDLER`; 不做 ROM 兼容分支, 已知差异进文档 (P6.1). 不依赖插件, 可在 P1 最早实现. 证据: 宿主提交 hash; JVM 测试 (Intent action / extras 快照); 设备: XQ-AT72, Xiaomi Pad, OnePlus 各建一个系统闹钟并用 `next()` 读回. 证据 (E1 + E2 + E3, 2026-11-05, chime-p1-script): 宿主提交 `62add2b946` (仅本地); `AlarmSystemIntentsTest` 6 例 (Intent action / extras 快照); 设备检查脚本 `tools/alarms/device_alarms_check.py` (宿主提交 `c5ac0d1802`): OnePlus 63751f63 (API 35, ColorOS, 隐式解析) 14 / 15 PASS, `next()` 读回误差 0 ms, `dismissAlarm` 被接收但闹钟未关闭; XQ-AT72 QV710AF65F (API 31, `--package com.android.deskclock`) 15 / 15 PASS; Xiaomi Pad 968e9f18 (API 35) failed = 0, `next()` 被其他应用早于现在的 `AlarmClockInfo` 占住, 改以 `dumpsys alarm` 核对测试闹钟已排程 (附录 B Q13); ROM 差异 (ColorOS 与小米时钟收下 `DISMISS_ALARM` 不关闭, 两者都没有 `DISMISS_TIMER` 处理者, Sony 装第三方时钟后隐式请求出选择器) 留给 P6.1; 派生 T40, T41.

### P1.6 宿主样例脚本

- [x] (宿主) 按宿主样例目录惯例加入 4 个样例: 工作日 7:30 起床 + `after-wake.js` (bind `dismiss`), 每月最后一个周五 18:00, 从某日起每 3 天 (轮班), 系统时钟 (`alarms.system.setAlarm` + `next`); 1.2.0 后补节假日样例. 证据: 提交 hash, 样例在任一设备跑通. 证据 (E1 + E3, 2026-11-05, chime-p1-script): 宿主提交 `0d47856881` (目录 `app/src/main/assets-app/sample/3-Chime Alarm/`, 四个样例 + `after-wake.js`, 均标 `[v6.8.1+]`) 与 `1da3ac2a41` (样例 4 加可选 `package`, 注明部分时钟应用的 `dismissAlarm` 只打开列表); 样例 4 在三台真机跑通, 样例 1 - 3 在未装插件的设备按预期报 `PLUGIN_UNAVAILABLE` (装插件后的运行在 P2.7 补证); 工作日用星期数组 (T46). 装插件后的运行 (E3, 2026-11-05, chime-p2 P2.7): XQ-AT72 (API 31, 宿主 6641, 插件 debug versionCode 15) 上 `device_alarms_check.py --package <Chime 包名>` 未装插件时 9 项 PASS (alarms.system 跳过), 装插件后 11 项 PASS (样例 1 - 3 建出闹钟并核对 nextTrigger / preview, 样例 1 绑定 after-wake.js; 样例 4 与 next / dismissAlarm 指向 Chime, dismissAlarm 按标签取下该闹钟).

---

## P2: 闹钟核心

目标: 插件独立可用的闹钟 (无宿主也能按时响), 并通过 `IAlarmPlugin` 向宿主提供全部 1.0.0 方法与事件. 依赖 P0 与 P1.1. 设备: XQ-AT72 (API 31, `SCHEDULE_EXACT_ALARM` 需用户授权), XQ-DQ72 / Redmi (33, `USE_EXACT_ALARM`), Xiaomi Pad / OnePlus (35, 全屏权限与 ColorOS), `AVD_API_24` (直接启动与最低 API).

### P2.1 存储与模型 (T7, T18, T21, D24)

- [x] (插件) 数据模型 (`Alarm`: id `a_` 前缀 (D24), `source` / `updatedBy` / `createdAt` / `updatedAt`, 附录 A 第 4.2 节全部选项; 内部状态: `nextTrigger`, `skipping`, 贪睡状态, 本次 `occurrence`); `AlarmStore`: JSON 文档 + fsync + 原子替换 (T21), 放设备加密存储, 启动时校验与版本迁移, 损坏条目单独跳过; 上限 300 / 启用 200 (`LIMIT_EXCEEDED`); 变更通知 (`change` 事件源, 带 `action` 与 `source`). 证据: 提交 hash; JVM 测试 (往返, 损坏恢复, 上限, 来源记录). 证据 (E1 + E2, 2026-11-05, chime-p2): 提交 `4f5c1d0`; 包 `core/alarm/` (`AlarmRecord` / `AlarmRuntimeState`, `AlarmOptionsValidator`, `AlarmStore` (`alarms.json`, 设备加密存储 `filesDir/alarms/`, 临时文件 fsync 后改名再同步目录, 隔离副本见 T49), `AlarmRepository` (上限, 来源, `change` 监听, 隐式日期 T48), `AlarmFilter`, `AlarmException`); 新派生 T47 - T50; `:app:testDebugUnitTest` 中 `AlarmOptionsValidatorTest` 11, `AlarmStoreTest` 7 (往返, 损坏条目跳过与隔离, 不可读文档, 更新版本, 加载时 300 / 200 上限, 临时文件, 隔离副本保留 5 份), `AlarmRepositoryTest` 7 (id / 来源 / 时间戳 / `change` 事件, 未知 id 与来源, 300 / 200 上限, 跨实例持久化与保存失败回滚, 状态更新, 监听器异常, 隐式日期) 共 25 例通过.

### P2.2 重复规则引擎 (D11, T9, T10, 附录 A 第 4.3 节)

- [x] (插件) `RecurrenceRule`: RRULE 字符串 (可带 `RRULE:` 前缀) / 结构化 `RepeatRule` / 星期数组三种输入 -> 规范化 RRULE 字符串; 不支持项 (`SECONDLY` / `MINUTELY` / `HOURLY`, `BYHOUR` / `BYMINUTE` / `BYSECOND`, `RDATE`) 报 `INVALID_RULE` 并写明; `repeatText` 本地化 (10 语言; 覆盖每天, 工作日, 周末, 每周 X, 每 N 周, 每月 N 日, 每月第 N 个 / 最后一个 X, 每 N 天, 每年; 其他形态回退为规范化 RRULE 原文); `NextOccurrence` (`DTSTART` 取 `date`, 时刻取 `time`, `EXDATE`, `COUNT` / `UNTIL`, 不存在的日期按 RFC 5545 跳过, `byMonthDay` 负数); `TimeZonePolicy` (跟随设备或固定时区; DST 跳过的时刻顺延到跳变后第一刻, 重复的时刻只响第一次); `preview` (至多 366) 与 `upcoming` (至多 100, 计入 `skipNext` 与排除; 1.2.0 再计入节假日). 证据: 提交 hash; JVM 测试 (附录 A 第 12 节每条示例; RFC 5545 边界: `BYSETPOS`, `-1FR`, 31 日与 2 月, 闰年, DST 两个方向 (`America/New_York`, `Europe/Berlin`), 固定时区 `Asia/Shanghai`; 与 lib-recur 或参考实现对拍 1000 个随机规则). 证据 (E1 + E2, 2026-11-05, chime-p2): 提交 be6101f; 包 `core/rule/` (`RecurrenceRule` 三种输入与规范化顺序, 不支持项与非法组合报 `INVALID_RULE` 并写明部分; `TimeZonePolicy`; `NextOccurrence` (隐式日期 T48, `next` / `preview` / `upcoming`, 日期计算分工 T52, 语义 T53 / T54); `RepeatText` 10 语言 (T51)), `AlarmOptionsValidator` 改用 `RecurrenceRule` 规范化, `AlarmRepository` 改用 `TimeZonePolicy` 与隐式日期; 新派生 T51 - T54. `:app:testDebugUnitTest` 全部 82 例通过, 其中 `RecurrenceRuleTest` 5 (三种输入, 规范化顺序, 不支持项与非法组合), `NextOccurrenceTest` 13 (附录 A 第 12 节 4 例: 每月最后一个星期五 18:00, 每 3 天的班次, `skipNext` 与 `upcoming` (含番茄钟一次性闹钟与禁用闹钟), 节假日 `UNSUPPORTED` 与一次性闹钟的隐式日期; `BYSETPOS` / `-1FR` / 31 日 / 2 月月末; 闰年与第 53 周; YEARLY `BYSETPOS` 与 `BYYEARDAY` + `BYDAY` (lib-recur 偏差回归); `COUNT` / `UNTIL` / `exclude`; 永不产生日期报 `INVALID_RULE`; DST `America/New_York` 与 `Europe/Berlin` 两个方向; 固定时区 `Asia/Shanghai`; 跳过计算与从头计算一致; `preview` 366 / `upcoming` 100 上限), `RepeatTextTest` 5 (11 种形态 x 10 语言, 复数与序数, 19 个回退规则, 语言映射, ASCII 标点), `RecurrenceOracleTest` 2 (RFC 5545 第 3.3.10 节 15 个例子; 测试代码内独立参考实现对拍 1000 个随机规则 (种子 20261105, 起点 1995 - 2035, 每条前 40 次响铃, 100 年内, 并核对任意时刻后的 `next` 与跳过一次后的 `next`): 905 条逐次一致, 共 28877 次响铃, 95 条双方都无日期 (插件报 `INVALID_RULE`), 0 处差异; 覆盖 YEARLY 242, `BYSETPOS` 167, `BYWEEKNO` 55, `BYYEARDAY` 39, 序号 `BYDAY` 77, `UNTIL` 298, `COUNT` 182, `exclude` 217), `AlarmOptionsValidatorTest` 11.

### P2.3 排程与恢复 (T3, T7, T8, T16)

- [x] (插件) `AlarmScheduler`: 每个启用闹钟只排 "下一次" (`setAlarmClock`, `AlarmClockInfo` 的展示 Intent 打开闹钟列表), 响过 / 跳过 / 贪睡后重算; 精确闹钟权限缺失时 (API 31 - 32 未授权) 不排程并在 `capabilities().permissions` 与 `PERMISSION_REQUIRED` 中体现, 收到 `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` 后重排; 重排时机 T8 (`BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`; 1.2.0 加节假日数据更新); 接收器 `directBootAware` (T7); 错过处理 T16 (超过触发时刻 5 分钟内补响, 超过则不响, 发 `miss` 事件 (`reason: 'off' | 'late'`) 并留一条通知); `skipNext` / `cancelSkip`. 证据: 提交 hash; JVM 测试 (排程决策纯逻辑, 错过窗口); 设备: XQ-AT72 改系统时间 / 时区后 `nextTrigger` 正确, 重启后闹钟仍在排程 (`adb shell dumpsys alarm` 可见 `setAlarmClock` 条目), 锁屏显示下一个闹钟. 证据 (E1 + E2 + E3, 2026-11-05, chime-p2): 提交 fab0a31 (实现为 `core/schedule/` 的 `AlarmPlanner` (纯逻辑决策) 与 `AlarmSchedule` (对账, 唤醒与事件), `schedule/` 的 `AlarmReceivers` 与 `AndroidAlarmSchedulePort`; 新派生 T55 - T60) 与本条勾选提交; `:app:testDebugUnitTest` 全部 94 例通过, 其中 `AlarmScheduleTest` 11 (只排下一次且响后排其后一次, 无精确闹钟权限不加启用闹钟也不排程, 迟到 5 分钟内补响 / 更晚为 `miss`, 时钟回拨不重响 / 回拨超过一天照常响, 一次性闹钟错过即退役 / 响铃不退役, 换时区后跟随设备时区的响铃平移, 只有触发接受提前, `skipNext` 跳过一次并到时发 `skip`, 一次性闹钟 `skipNext` 即禁用且改时刻清除跳过, 贪睡新一轮与窗口外结束本轮, 删除取消唤醒), `ManifestContractTest` 11 (含接收器 `exported=false` 与 `directBootAware`), `:app:lintDebug` 通过. 设备脚本 `.python/device_schedule.py` (`run` / `reboot` / `finish`, 探针 `ScheduleDeviceProbe` 只在带 `chimeProbe` 参数时运行): Redmi 22120RN86C (API 33) `run` 全部 PASS (未授通知权限, 错过通知一项未查); XQ-AT72 (API 31) `run` 全部 PASS: 探针 B 每天 19:30 / A 每天 20:30 的 `nextTrigger` 与 `dumpsys alarm` 的 `setAlarmClock` 条目一致, 时区改为 `Asia/Tokyo` 后两者按东京的同一墙上时间排程, 改回原时区后复原, 时钟调到 B + 2 分钟时 `TIME_SET` 对账迟到补响 (`ring`), 调到 A + 10 分钟时 `miss` (`reason: 'late'`) 且只留 1 条错过通知, 两者都排到次日, 恢复真实时间后仍在次日 (漂移 0 秒, 不重响, T56); `appops` 收回精确闹钟权限后系统闹钟 0 条, `add` 报 `PERMISSION_REQUIRED`, 恢复 (默认) 后收到权限变化广播并重排; `reboot`: 重启前 4 条 `setAlarmClock`, 首次解锁前凭据存储锁定时 `LOCKED_BOOT_COMPLETED` 即到达插件并重排, 4 条 (11-05 19:00 / 20:00, 11-06 19:30 / 20:30) 不变, 系统下一个闹钟 1793876400000 (11-05 19:00), 锁屏截图在时钟下显示 "THU 19:00"; `finish`: 解锁后 `BOOT_COMPLETED` 到达插件, 探针 `nextTrigger` 与重启前一致, 探针与通知已删, 测试包已卸载; 自动时间 / 自动时区 / 时区已还原. `capabilities().permissions.exactAlarm` 在 P2.5 接到同一个 `canScheduleExact()` (T60).

### P2.4 响铃 (D18, T4 - T6, T14, T35)

- [ ] (插件) `RingService` (前台, 类型按 P0.2 结论; 持 `WAKE_LOCK` 到响铃结束), 全屏响铃: API 34+ `canUseFullScreenIntent()` 检查, 未授予时退化为高优先级通知 + 声音 (T5), `RingActivity` `showWhenLocked` / `turnScreenOn`; `RingPlayer`: `USAGE_ALARM` 音频属性, 来源 `default` / `silent` / 铃声 uri / 导入的本地音频, 渐强 `rampSeconds` (0 - 300), 独立音量 (响铃期间临时设定闹钟流音量, 结束恢复, T14), 循环播放, 文件损坏时回退默认铃声并记日志; `RingVibrator` (默认节奏 / 自定义数组, 与 `device.vibrate` 同写法); `RingSpeaker` (`TextToSpeech`, `engine` 可指定, `before` / `after` / `interleave` / `only` 四种 mode, `repeatInterval`, 引擎不可用时跳过朗读不影响铃声); 贪睡 (`minutes` / `max`, 默认 10 / 3, `false` 禁止); `autoSilenceMinutes` (默认 10, 1 - 60, 到点发 `silence`); 多个闹钟同时到点时排队响 (一次一个, 后到的在前一个关闭后立即响). 证据: 提交 hash; JVM 测试 (响铃状态机, 音量恢复, 朗读调度, 排队); 设备: XQ-DQ72 锁屏下全屏响铃 + 贪睡 + 关闭, Xiaomi Pad (35) 未授全屏权限时的退化, TTS 用 3-Macaw Speech 引擎 (指定 `engine`) 与系统引擎各一次, 勿扰模式下仍响.

### P2.5 Binder 服务端与宿主联动 (T11, T13, T15, T17)

- [x] (插件) `ThreeChimeAlarmService` 实现 `IAlarmPlugin` 全部 1.0.0 方法 (1.1.0 - 1.3.0 的方法返回 `UNSUPPORTED`, `alarmFeatures` 只声明 `alarms`); 输入边界 (附录 A 第 11 节限制, 未知键拒绝, 文本上限, 枚举值); 铃声文件: 接收宿主只读 pfd, 复制进插件私有目录 (上限 20 MiB, 用 `MediaExtractor` 校验可解码, 失败报 `IO_FAILED`), 原文件删除后仍可响 (T15); 事件回调 `IAlarmEventCallback` (oneway, `linkToDeath` 清理, 不假定只有一个宿主进程); `ALARM_TRIGGER` 签名广播 (显式包名, extras 带事件 JSON 与 `occurrence`, 只对宿主提示表中 "有绑定且需要该事件" 的闹钟发, T12); `change` 事件 (任何来源增删改); 来源记录: 宿主调用记 `script`, 插件工具记 `tool`, 系统 Intent 记 `intent`, 界面记 `user` (T18). 证据: 提交 hash; instrumentation (发现, 绑定, happy path `add` -> `list` -> `update` -> `remove`, 非法参数, 上限, 错误传播, 回调注册 / 注销不泄漏 `ServiceConnection` 与 fd); 与宿主 P1.4 联调 (`ring` 事件到脚本, 绑定脚本被启动). 证据 (E1 + E2 + E3, 2026-11-05, chime-p2): 提交 cbdb1c7 (T71 - T78, Q14 登记); `AlarmPluginBinderTest` 6 与 `ThreeChimeAlarmContractTest` 4 经 `MarshallingBinder` 走 Parcel, `.python/device_binder.py` 在 G8441 (API 28) 与 Xiaomi Pad 23046RP50C (API 35) 各 10 / 10 PASS, JVM 与 `:app:lintDebug` 通过; 与宿主联调 (P2.7, XQ-AT72 API 31, 宿主 6641): `ring` 事件在到期后 0.1 s 到达脚本监听, 绑定脚本为 `change` / `ring` / `dismiss` 各启动一次 (`alarms.currentEvent()` 类型与闹钟 id 一致), 铃声导入的 `IO_FAILED` / `LIMIT_EXCEEDED` 与 300 个闹钟上限在设备上触发.

### P2.6 系统时钟 Intent 接收 (D21)

- [ ] (插件) `ClockIntentActivity`: `SET_ALARM` (`EXTRA_HOUR` / `EXTRA_MINUTES` / `EXTRA_MESSAGE` / `EXTRA_DAYS` / `EXTRA_VIBRATE` / `EXTRA_RINGTONE` / `EXTRA_SKIP_UI`; 缺 `EXTRA_HOUR` 时打开新建页), `SHOW_ALARMS`, `DISMISS_ALARM` (`EXTRA_ALARM_SEARCH_MODE` 的 `next` / `all` / `time` / `label`; 多个匹配时按系统时钟惯例让用户选), `SNOOZE_ALARM` (`EXTRA_ALARM_SNOOZE_DURATION`); 来源记 `intent`; `skipUi=false` 时进入编辑页预填. 证据: 提交 hash; instrumentation (`am start` 四个 action 的结果); 设备: XQ-AT72 上宿主 `alarms.system.setAlarm({ package: <Chime 包名> })` 建进 Chime 并出现在列表. 部分证据 (E3, 2026-11-05, chime-p2 P2.7): XQ-AT72 (API 31) 上宿主 6641 的 `alarms.system.setAlarm({ package: <Chime 包名> })` 建进 Chime 并出现在 `alarms.list({ source: 'intent' })` 中 (启用); 待 Xiaomi Pad 解锁后跑 `ClockIntentActivityTest` 再勾选.

### P2.7 契约一致性测试

- [x] (测试) 宿主 debug 包 + 插件 debug 包: 附录 A 第 12 节的 1.0.0 示例全部在 XQ-AT72 跑通 (番茄钟示例改用一次性闹钟); 错误码表逐项触发 (卸载 / 禁用 / 不兼容 / 缺权限 / 非法选项 / 非法规则 / `NOT_FOUND` / `LIMIT_EXCEEDED` / `IO_FAILED`); 协议文档与两侧实现对照无差异 (有差异先改文档再改代码). 证据: `device_smoke.py` 输出 (每项通过 / 失败), 差异清单为空或已修. 证据 (E1 + E3, 2026-11-05, chime-p2): 提交 f132eef (`.python/device_contract.py`, `device_smoke.py --contract [--incompatible-apk <变体>]`); 协议文档 `docs/dev/alarm-plugin-protocol-v1.md` 对照两侧实现改 25 处, 宿主提交 `f778f26516` (仅本地): T50 patch 语义, 铃声超过 20 MiB 为 `LIMIT_EXCEEDED` (T72), 本宿主 `removeAlarm` 与 `change` 绑定 (Q14), 隐式日期 (T48), 取值检查与限制表, 未实现功能的选项, 未知 id (T75), `dismiss` / `snooze` 目标 (T65), `scriptSkipped` 只记日志 (T76), 每个宿主一张提示表 (T71), `contractVersion` 戳, 错误码表 "Raised by" 一栏; 附录 A 第 5.3 / 11 节同步 4 行; 两侧实现之间无冲突, 代码侧只剩 2 项随 P3 补上 (插件还不发 `unbind`, 还没有 `OPEN` 页面, `alarms.openPlugin` 与 `PERMISSION_REQUIRED` 提示报 `UNSUPPORTED`). XQ-AT72 (QV710AF65F, API 31) 装宿主隔离克隆 `c5ac0d1802` 的 debug 包 (临时 VERSION_BUILD 6641, 只在克隆里, Q12) 与插件 debug (versionCode 15, SHA-256 e528b228...): `device_smoke.py --contract` 契约 54 项全 PASS. 未装插件时 `isAvailable()` false, `add` / `list` 报 `PLUGIN_UNAVAILABLE` (带提示), 未知键仍先报 `INVALID_OPTIONS`. 附录 A 第 12 节示例: 示例 1 原文的 `holidays` 由宿主先报 `PLUGIN_INCOMPATIBLE` (1.2.0 功能), 改用工作日规则后 `nextTrigger` 为工作日 07:30, `bind('./after-wake.js', { events: ['dismiss'] })` 后 `hasBindings` true; 示例 2 的每月最后一个周五 18:00 与从 2026-11-04 起每 3 天由 `preview()` 核对, `alarms.holidays` 不存在, `list({ source: 'user', enabled: true })` 只读 (用户闹钟不动), `skipNext` / `cancelSkip` 在 轮班测试闹钟上跑; 示例 3 `alarms.timers` 不存在, 番茄钟改为 25 分钟后的一次性闹钟 (`speak` `only`), `alarms.system.setAlarm({ package: <Chime 包名> })` 建进 Chime (`source` `intent`, 启用), `alarms.system.next()` 指向 Chime. 错误码: `INVALID_OPTIONS` (未知键, `time` 与 `at` 同给), `INVALID_RULE` (未知 FREQ), `NOT_FOUND` (宿主侧 `update`, 插件侧 `skipNext`), `get` 未知 id 为 null, `IO_FAILED` (宿主: 文件不存在; 插件: 非音频文件), `LIMIT_EXCEEDED` (宿主: 21 MiB 铃声; 101 字符标签; 插件: 第 301 个闹钟, 批量禁用闹钟随后删除); `appops` 临时收回精确闹钟后 `capabilities().permissions.exactAlarm` false, 启用的闹钟与 `setEnabled(true)` 报 `PERMISSION_REQUIRED` (带提示), 禁用的闹钟照常添加, appop 已恢复 default; 插件中心开关关闭后 `isAvailable()` false, `list` / `add` 报 `PLUGIN_DISABLED` (带提示), 打开后恢复; 装 `requiresHostVersion` 6642 的变体 (只在临时克隆里构建) 后 `isAvailable()` false, `add` / `list` 报 `PLUGIN_INCOMPATIBLE` (带提示), 随后装回正常包. 静音一次性闹钟 75 s 后响: `ring` 监听在到期后 0.1 s 收到, `ringing()` 1 条, 监听里 `dismiss()` 返回 true 且 `dismiss` 事件 `by` `script`, 绑定脚本分别以 `change` / `ring` / `dismiss` 的 `currentEvent()` 启动, 一次性闹钟响后禁用; 测试闹钟全部删除. 宿主检查 `tools/alarms/device_alarms_check.py --package <Chime 包名>` 未装插件时 9 项 PASS (alarms.system 跳过), 装插件后 11 项 PASS (样例 1 - 3 建出闹钟并核对 nextTrigger / preview, 样例 1 绑定 after-wake.js; 样例 4 与 next / dismissAlarm 指向 Chime, dismissAlarm 按标签取下该闹钟) (样例 1 - 3 与 `alarms.system`). 测完卸载插件 (原本未装), 装回原宿主 5935 (base.apk md5 一致).

---

## P3: 独立界面

目标: 不装 AutoJs6 也能当闹钟用的完整时钟应用 (1.0.0 只含闹钟部分; 计时器 / 秒表页随 1.1.0, D7). Compose + Material 3 (T22), 按 `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md`. 依赖 P2.1 - P2.4. 设备: XQ-DQ72 (截图), Xiaomi Pad (平板布局), OnePlus (ColorOS), 无障碍 / RTL / 大字体各核一次.

### P3.1 闹钟列表与编辑 (T18, D20)

- [ ] (插件) 列表 (下一次时刻与 "还有 N 小时 M 分", 启用开关, 来源与最后修改来源标签 (T18), "有脚本绑定" 标记与解除 (D20, 不能选脚本), 跳过下一次, 多选删除, 空态); 编辑页 (时间, 日期, 重复: 预设 (一次 / 每天 / 工作日 / 周末 / 自选星期) 与高级 RRULE 编辑器 (`freq` / `interval` / `byDay` / `byMonthDay` / `bySetPos` / `count` / `until`, 预览接下来 5 次), 排除日期, 时区 (跟随设备 / 固定), 标签, 铃声, 振动, 朗读 (文字, 引擎, mode), 贪睡, 自动停止); 缺精确闹钟 / 通知 / 全屏权限时顶部横幅跳 P3.4. 证据: 提交 hash; E4 (XQ-DQ72 明暗截图); instrumentation (编辑往返, RRULE 编辑器与 `repeatText` 一致).

### P3.2 响铃界面

- [ ] (插件) 全屏 (锁屏上, 常亮), 大字时间与标签, 滑动或按钮关闭与贪睡 (显示贪睡次数与剩余), 正在朗读指示, 多个闹钟排队提示 ("还有 N 个闹钟"), 通知形态 (未授全屏权限时) 的 "关闭" / "贪睡" 动作; 1.3.0 挑战层预留插槽. 证据: 提交 hash; E4 (锁屏响铃截图, XQ-DQ72 与 OnePlus).

### P3.3 铃声选择与导入

- [ ] (插件) 系统闹钟铃声列表 (`RingtoneManager.TYPE_ALARM`, 试听), 导入本地音频 (SAF, 复制进插件, 上限 20 MiB, 可删除), 默认铃声设置; 列表与 `ringtones.list()` 一致 (脚本导入的文件也出现). 证据: 提交 hash; E4.

### P3.4 权限页

- [ ] (插件) 精确闹钟 (API 31 - 32: `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`), 通知 (33+), 全屏 (34+: `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`), 电池优化与厂商自启动引导 (只引导不强制, 文案说明不响铃的后果), 每项当前状态与跳转; `alarms.openPlugin('permissions')` 直达. 证据: 提交 hash; 设备: XQ-AT72 (31), Xiaomi Pad (35), OnePlus (ColorOS) 三台走完全部跳转.

### P3.5 设置页与发行历史

- [ ] (插件) 按 STANDALONE_SETTINGS 规范: 外观四项 (默认跟随宿主), 默认贪睡 / 默认自动停止 / 默认铃声 / 默认振动, 时间格式 (跟随系统 / 12 / 24 小时), 发行历史 (内置 `doc/CHANGELOG-<lang>.md`), 更新检查 (超时 / 取消 / 失败提示 / 忽略版本 / 频率 / 计量网络), 关于与第三方声明; 1.2.0 默认地区与 1.3.0 紧急关闭开关的位置预留. 证据: 提交 hash; E4.

### P3.6 图标核对与截图

- [ ] (插件) 启动器 / 插件中心 / 通知小图标明暗核对 (P0.3 选定的源图); `docs/images/screenshots` 采集 (XQ-DQ72, 1096 x 2560, 明暗各一: 列表, 编辑, 响铃, 设置). 证据: 截图路径.

---

## P4: 插件工具

目标: MCP Server 与 `plugins.tools.call` 可以增删改查闹钟并处理正在响的闹钟 (D5); 不开放脚本绑定, `beforeRing` 与秒表 (T17). 依赖 P2.5; 与 P3 并行 (不同目录).

### P4.1 工具目录与处理器 (附录 A 第 10 节)

- [ ] (插件) 短名 `chime`; 1.0.0 工具 `alarm_add` / `alarm_list` / `alarm_update` / `alarm_remove` / `alarm_set_enabled` / `alarm_skip_next` / `alarm_upcoming` / `alarm_dismiss` / `alarm_snooze` (参数 schema 同 `AlarmOptions`, 不含本地音频文件路径, 不含 `dismiss.challenge: 'script'`); 来源记 `tool`; 错误码映射; 按 `plugin-tool-api` 协议实现 (参照 3-Macaw Speech 的 `speech_*` 工具). 证据: 提交 hash; JVM 测试 (schema 与参数校验); instrumentation (`plugins.tools.call` 往返).

### P4.2 MCP Server 实机调用

- [ ] (测试) 经 MCP Server 插件从 PC 调用 `alarm_add` / `alarm_list` / `alarm_upcoming` / `alarm_remove` (XQ-AT72); 配对确认由维护者在手机上操作. 证据: 调用结果摘要 (不含令牌与完整工具清单).

---

## P5: 验证

目标: 1.0.0 在设备矩阵与系统事件场景下按时响铃且联动可靠. 依赖 P1 - P4.

### P5.1 AVD 矩阵

- [ ] (测试) `AVD_API_24`: 安装, 直接启动 (重启后不解锁, 到点应响), 最低 API 全流程; `AVD_API_33`: CI 用例集 (`connected-avd` job 的用例定稿); `AVD_API_35_16K` / `AVD_API_36.1_16K`: 全屏权限未授予的退化, 前台服务类型复核 (T6), 16 KB 页环境下安装与运行 (无原生库, 只核对). 证据: `device_smoke.py` 输出 + 日期.

### P5.2 真机矩阵

- [ ] (测试) 附录 D 每台在线设备: R3 冒烟清单, 记录型号 / API / APK 摘要 / 触发偏差 (ms). 证据: 结果填入附录 D 矩阵的 "1.0.0 结果" 列.

### P5.3 时间与系统事件场景 (T7 - T9, T16)

- [ ] (测试) XQ-AT72: 改系统时间 (向前 / 向后), 改时区, DST (设 `America/New_York` 并把时间拨到跳变前 2 分钟, 两个方向), 升级安装 (`MY_PACKAGE_REPLACED` 后闹钟仍在排程), 关机错过 (关机 10 分钟越过触发点, 开机后应 `miss` 不响并有通知; 关机 2 分钟越过应补响), Doze (`adb shell dumpsys deviceidle force-idle` 后 `setAlarmClock` 不被推迟), 杀进程 (`am force-stop` 后到点仍响). 证据: 每项通过 / 失败 + logcat 关键行.

### P5.4 ColorOS 激活与唤醒 (OnePlus OPD2413)

- [ ] (测试) 安装后不打开插件, 插件中心出现 "激活" 并可启用; 杀死进程后到点仍响 (记录是否需要自启动 / 后台限制设置, 写进 README 常见问题); 宿主被杀后经 `ALARM_TRIGGER` 唤醒并启动绑定脚本. 证据: 步骤与结果, 未执行项如实记录.

### P5.5 联动边界 (附录 A 第 5.3 节)

- [ ] (测试) 宿主被 "强行停止" (广播按系统规则不投递, 插件照常响铃), 宿主未安装 / 插件被禁用时的脚本错误三码, 设备未解锁 (`scriptSkipped: 'locked'`, 解锁后不补启动), 绑定脚本文件不存在 (跳过 + 日志), 同一 `occurrence` 不重复启动 (贪睡后再响为新 `occurrence`), 同一闹钟多脚本绑定, 实时监听者退出后回调注销 (`dumpsys activity services` 查绑定数). 证据: 每项通过 / 失败.

### P5.6 基线

- [ ] (测试) 响铃触发偏差 (`scheduledAt` 与实际 `firedAt`, 每台 10 次, P50 / 最大), 待机一夜电量 (无闹钟 vs 3 个启用闹钟, 同一台机), 冷启动到响铃界面可见的时长; 填附录 D 基线表. 证据: 表格数值.

---

## P6: 文档, 声明, 四仓库同步与 1.0.0 发布

目标: 用户与脚本作者能从文档与 README 独立完成安装, 授权, 建闹钟与联动; 1.0.0 发布并登记索引 (D25: 不等宿主公开). 依赖 P5.

### P6.1 文档 (AutoJs6-Documentation)

- [ ] (文档) `api/alarms.md`: 模块总览与可用性 (插件 / 宿主版本), 闹钟选项, 重复规则 (RRULE 子集, 不支持项, 常见写法表), 响铃内容, 事件与两种联动 (`execArgv.alarm`, `currentEvent`, 未解锁 / 宿主被停止时的行为), `alarms.system` (ROM 已知差异表: `skipUi`, `days`, `ringtone`, `dismissAlarm` 搜索模式), 错误码与限制表, 示例; 首屏提示 "脚本可以读写用户在插件界面建的闹钟 (D15), 插件界面会标出修改来源". changelog 条目 (10 语言 JSON 同步). 证据: 文档仓库提交 hash (仅本地, 推送时机由维护者定). 运行时支持表: 页面用文档仓库的 `node generator/runtime-support.js --insert alarms:alarms` 插入 runtime-support 块; 推迟期间 `check.py --docs-support` 把缺块的 alarms 记入 `deferredRoots` 而不判失败, 删掉 policy.json `deferredNameGate` 的 alarms 行 (Q11 (a)) 之后缺块即失败 (宿主 48efe0e65e).

### P6.2 声明与编辑器 (AutoJs6-TypeScript-Declarations, Ace Editor, Offline Docs)

- [ ] (文档) d.ts (附录 A 全部类型, `execArgv.alarm` 的类型), Ace 补全, Offline Docs 同步; 各仓库按惯例本地提交. 证据: 提交 hash.

### P6.3 README, changelog, 兼容矩阵与截图

- [ ] (插件) `.readme` 10 语言 (功能, 安装, 权限理由 (精确闹钟 / 全屏 / 通知 / 开机启动 / 前台服务 / 振动), 脚本示例, 系统时钟对接, 兼容性 (所需宿主版本与 D25 的说明: 未升级宿主时界面与工具可用, 脚本模块不可用), 常见问题 (不响铃排查: 权限 / 电池优化 / 自启动 / 勿扰 / 错过规则), 第三方致谢), `plugin_instruction` 10 语言, changelog v1.0.0 10 语言, 截图 (P3.6). 证据: `generate_markdown.py --check` 通过.

### P6.4 1.0.0 发布

- [ ] (发布) 门禁 R1 - R4; `requiresHostVersion` 回填为包含 P1 全部宿主提交的 `VERSION_BUILD`; GitHub 建仓 (维护者待办) 后推送 `master`, tag `v1.0.0`, Release. 证据: E5.

### P6.5 索引登记

- [ ] (索引) 门禁 R5. 证据: 索引仓库提交 hash 与清单文件名.

---

## P7: 1.1.0 计时器与秒表 (D14, D21, 附录 A 第 6 节)

目标: 多个并行计时器与一个秒表, 重启可恢复, 脚本 / 工具 / 系统 Intent 三个入口齐备. 依赖 P6.4 (1.0.0 已发布). 契约不改 (T25), `alarmFeatures` 加 `timers` / `stopwatch`.

### P7.1 计时器核心

- [ ] (插件) `Timer` (id `t_` 前缀, `durationMillis` / `remainingMillis` / `state` / `endsAt`), 至多 20 个并行; `start` (毫秒或 `"25m"` / `"1h30m"` / `"90s"`, 1 s - 100 h) / `pause` / `resume` / `addTime` (可负, 结果须在范围内) / `reset` / `cancel`; 到点用 `setAlarmClock` 排程, 进入与闹钟相同的响铃路径 (`kind: 'timer'`); 运行中显示常驻倒计时通知 (动作: 暂停 / 加 1 分钟 / 取消); 重启按 "关机前的到点时刻" 恢复 (关机期间计入), 已过点的按 T16; 关闭后删除. 证据: 提交 hash; JVM 测试 (时长解析, 状态机, 恢复); 设备: XQ-AT72 重启恢复.

### P7.2 秒表

- [ ] (插件) 单一秒表 (`idle` / `running` / `paused`), `lap` 至多 100 圈 (`lapMillis` / `totalMillis`), 重启后转为暂停且关机期间不计时; 运行中可选常驻通知. 证据: 提交 hash; JVM 测试.

### P7.3 Binder 与脚本 API

- [ ] (插件) `IAlarmPlugin` 的计时器 / 秒表方法实现, `alarmFeatures` 加 `timers` / `stopwatch`; `ring` / `snooze` / `dismiss` / `silence` 事件 `kind: 'timer'`; `Timer.bind`. (宿主) `alarms.timers.*` / `alarms.stopwatch.*` 与 `Timer` 快照对象 (宿主按 feature 门, 1.0.0 插件报 `PLUGIN_INCOMPATIBLE` 并提示升级插件); `TimerOptions`; 文档与 d.ts. 证据: 两侧提交 hash; 联调.

### P7.4 界面

- [ ] (插件) 计时器页 (多卡片, 预设时长, 自定义时长, 加 1 分钟, 暂停 / 重置 / 取消), 秒表页 (计次列表, 最快 / 最慢圈标记), 响铃界面复用; 底部导航 (闹钟 / 计时器 / 秒表). 证据: 提交 hash; E4.

### P7.5 系统 Intent (D21)

- [ ] (插件) `SET_TIMER` (`EXTRA_LENGTH` / `EXTRA_MESSAGE` / `EXTRA_SKIP_UI`), `SHOW_TIMERS`, `DISMISS_TIMER`; (宿主) `alarms.system.setTimer` / `showTimers` / `dismissTimer` 已在 P1.5, 本阶段只补联调. 证据: instrumentation.

### P7.6 工具

- [ ] (插件) `timer_start` / `timer_list` / `timer_control` (`action`: `pause` / `resume` / `add` / `reset` / `cancel`); 秒表不开放 (T17). 证据: 提交 hash; `plugins.tools.call` 往返.

### P7.7 文档与 1.1.0 发布

- [ ] (文档) `api/alarms.md` 计时器与秒表章节, d.ts, Ace, Offline Docs; (插件) README / changelog 10 语言; (发布) R1 - R5. 证据: 提交 hash, E5.

---

## P8: 1.2.0 节假日与 `beforeRing` (D12, D16, D17, 附录 A 第 5.4 / 7 节, 附录 B Q8)

目标: 闹钟能按多地区节假日数据跳过 / 只在节假日 / 按工作日响铃, 并在响铃前让脚本决定是否响与怎么响. 依赖 P7.7 与 Q8 拍板. 契约不改 (T25), `alarmFeatures` 加 `holidays` / `beforeRing`.

### P8.1 数据管线 (资产)

- [ ] (资产) Shared-Assets 新类别 `holiday-calendars` (目录 / 签名 / 镜像惯例照 `speech-models`), 构建期从 Q8 选定的数据源生成紧凑数据 (按地区 / 年份, 含补班日标记 `hasMakeupWorkdays`), 许可与来源记录, 校验脚本; 首个目录 `holiday-calendars-v1` 由维护者推送. 证据: 资产仓库提交 hash, 数据体积 (每地区每年字节数).

### P8.2 插件节假日存储与刷新

- [ ] (插件) 内置发布时的数据快照, 每周检查一次更新 (计量网络策略与开关同 Macaw 目录刷新), `holidays.info` / `refresh` / `refreshAsync` 与 `IAlarmProgressCallback`, 数据更新后重排 (T8 第六个时机); 当年数据缺失时该年只按星期规则响并在 `capabilities()` 给出警告 (不因缺数据而不响). 证据: 提交 hash; JVM 测试 (快照解析, 缺年退化); 设备: 一次真实刷新.

### P8.3 过滤进规则引擎

- [ ] (插件) `HolidayFilter` 三种 `mode` (`skip` / `only` / `workdays`): `workdays` 加入补班日且限制 `repeat` 只能省略或 `daily` 间隔 1 (否则 `INVALID_OPTIONS`), 无补班日地区退化为 "周一至周五且非节假日"; `upcoming` / `preview` 计入; 被过滤的那次发 `skip` (`reason: 'holiday'`), 不触发 `beforeRing`; `region` 省略时用设置页默认地区 (首次按系统区域推断); `holidays.regions` / `isHoliday` / `isWorkday` / `list`. 证据: 提交 hash; JVM 测试 (中国大陆 2026 年调休表: 补班日响, 节假日不响).

### P8.4 `beforeRing`

- [ ] (插件) 在 `scheduledAt - leadSeconds` (15 - 300, 默认 60) 用第二个精确闹钟唤醒自己, 发 `beforeRing` 给宿主 (回调 + 有绑定时的广播), `deadline` 默认 `scheduledAt - 5 s`; 收到任何 `skip` 本次不响 (发 `skip`, `reason: 'script'`); `ring(overrides)` 只改本次 `label` / `sound` / `speak` / `vibrate`, 多个脚本按到达顺序合并; 到 `deadline` 无决定, 宿主不可用, 设备未解锁或脚本出错一律照常响. (宿主) `AlarmEvent.skip` / `ring` 方法, `BindOptions.leadSeconds`, 绑定脚本的启动与超时. 证据: 两侧提交 hash; 设备: XQ-AT72 上附录 A 第 5.4 节的 `weather-brief.js` 示例 (改为本地假数据) 两个分支各跑一次, 以及宿主被杀时照常响.

### P8.5 脚本 API 与能力

- [ ] (宿主) `alarms.holidays.*`, `AlarmOptions.holidays`, `BindOptions.leadSeconds`, 事件 `beforeRing`; 插件 `alarmFeatures` 加 `holidays` / `beforeRing`; 文档与 d.ts. 证据: 提交 hash.

### P8.6 界面

- [ ] (插件) 设置页默认地区 (列表来自 `regions()`, 显示数据年份范围与是否含补班日), 编辑页节假日选项 (三种 mode 与地区), 列表显示 "下一次 (已跳过 N 个节假日)" 与数据更新状态. 证据: 提交 hash; E4.

### P8.7 工具

- [ ] (插件) `holiday_check` (日期, 地区 -> 是否节假日 / 工作日与名称). 证据: 提交 hash; 往返.

### P8.8 文档与 1.2.0 发布

- [ ] (文档) 节假日与 `beforeRing` 章节 (写明补班日准确性完全取决于上游数据, D17), d.ts, Ace, Offline Docs; (插件) README / changelog; (发布) R1 - R5. 证据: 提交 hash, E5.

---

## P9: 1.3.0 关闭挑战 (D13, 附录 A 第 8 节)

目标: 四种关闭挑战与一致的安全阀, 不让用户被困在响铃里. 依赖 P8.8. 契约不改 (T25), `alarmFeatures` 加 `challenges`.

### P9.1 内置挑战

- [ ] (插件) `math` (难度 1 - 3, 题数默认 3), `shake` (加速度计, 默认 30 次, 进度显示), `type` (随机一句或指定文字, 忽略大小写与首尾空白); 挑战进行中铃声继续, 可按 `allowSnooze` 贪睡. 证据: 提交 hash; JVM 测试 (题目生成与判定); E4.

### P9.2 脚本条件

- [ ] (插件 / 宿主) `challenge: 'script'`: 一直响到绑定脚本或任何脚本调用 `alarms.dismiss(id)`; `fallback` (默认 `none`) 的触发条件: 宿主不可用, 既无绑定脚本也无实时监听者, 设备未解锁, 绑定脚本启动失败或结束运行时仍未关闭. 证据: 两侧提交 hash; 设备: 每个 fallback 条件各触发一次.

### P9.3 安全阀

- [ ] (插件) 挑战模式下 `autoSilenceMinutes` 默认 30, 上限 60, 到点发 `silence`; 紧急关闭 (连按 5 次音量键, 设置页默认开可关); 挑战界面始终显示剩余自动停止时间. 证据: 提交 hash; 设备: 紧急关闭在锁屏响铃时可用.

### P9.4 脚本 API 与事件

- [ ] (宿主) `DismissOptions`, `Ringing.challenge`, `dismiss` 事件 `by: 'challenge'` 与 `challenge` 字段; 工具 `alarm_add` 不接受 `script` 挑战 (T17); 插件 `alarmFeatures` 加 `challenges`; 文档与 d.ts. 证据: 提交 hash.

### P9.5 界面

- [ ] (插件) 响铃界面的挑战层 (P3.2 预留插槽), 编辑页挑战选项与说明, 设置页紧急关闭开关. 证据: 提交 hash; E4.

### P9.6 文档与 1.3.0 发布

- [ ] (文档) 挑战章节 (安全阀写在首段), d.ts, Ace, Offline Docs; (插件) README / changelog; (发布) R1 - R5. 证据: 提交 hash, E5.

---

## 附录 A. 脚本 API 草案 (设计稿第 4 - 12 节)

本附录原文保留 2026-11-03 设计稿的节号 (4 - 12), 本文其他地方以 "附录 A 第 N 节" 引用; 附录内部出现的 "见 4.3", "第 8 节" 等也指附录内的节. 实现时以本附录为接口依据, 行为细节由协议文档 (`docs/dev/alarm-plugin-protocol-v1.md`) 定稿; 两者有出入时先改文档再改代码, 并在对应阶段条目注明.

### 4. `alarms` 模块: 闹钟 (1.0.0)

#### 4.1 接口一览

```ts
declare namespace alarms {
    /** 插件已安装, 已启用且契约兼容. 不抛异常. */
    function isAvailable(): boolean;
    /** 插件版本, 契约版本, 权限状态, 支持的功能与限制值. */
    function capabilities(): Capabilities;
    /** 打开插件界面; page 省略时打开闹钟列表. */
    function openPlugin(page?: 'alarms' | 'timers' | 'stopwatch' | 'permissions' | 'settings'): void;

    function add(options: AlarmOptions): Alarm;
    function get(id: string): Alarm | null;
    function list(filter?: AlarmFilter): Alarm[];
    function update(id: string, patch: Partial<AlarmOptions>): Alarm;
    function remove(id: string): boolean;
    function setEnabled(id: string, enabled: boolean): Alarm;
    /** 跳过下一次 (重复闹钟); 一次性闹钟等同于关闭. 再调用一次 cancelSkip 撤销. */
    function skipNext(id: string): Alarm;
    function cancelSkip(id: string): Alarm;

    /** 全部启用闹钟中接下来的 count 次响铃 (已计入节假日与跳过), 默认 1, 至多 100. */
    function upcoming(count?: number, filter?: AlarmFilter): Occurrence[];
    /** 不建闹钟, 只按规则推算接下来的响铃时刻 (时间戳), 用于检验 RRULE. count 默认 10, 至多 366. */
    function preview(options: AlarmOptions, count?: number): number[];

    /** 正在响铃 (含贪睡中) 的闹钟与计时器. */
    function ringing(): Ringing[];
    /** 关闭响铃; id 省略时关闭全部正在响的. 关闭挑战中的闹钟同样可由脚本关闭. */
    function dismiss(id?: string): boolean;
    function snooze(id?: string, minutes?: number): boolean;

    function on(event: AlarmEventName, listener: (e: AlarmEvent) => void): Subscription;
    function once(event: AlarmEventName, listener: (e: AlarmEvent) => void): Subscription;
    function off(event?: AlarmEventName, listener?: Function): void;
    /** 本脚本由闹钟事件启动时返回该事件 (带 skip / ring 等方法), 否则返回 null. */
    function currentEvent(): AlarmEvent | null;

    function bind(id: string, script: string, options?: BindOptions): Binding;
    function unbind(id: string, script?: string): number;
    function bindings(id?: string): Binding[];

    namespace ringtones {
        /** 系统闹钟铃声 (RingtoneManager.TYPE_ALARM) 与插件里导入过的铃声. */
        function list(): Ringtone[];
        function getDefault(): Ringtone | null;
    }

    const AlarmError: AlarmErrorConstructor;
}
```

约定 (沿用宿主现有写法):

- 星期编号 `0` 至 `6` 依次为星期日到星期六 (与 `tasks.timeFlagToDays`, `Date#getDay` 一致).
- 相对路径按当前脚本运行目录解析.
- 方法都是同步的 (Binder 调用, 有宿主侧超时); 只有可能耗时的 `holidays.refresh` 另有 `refreshAsync`.
- 未知的选项键按名拒绝 (`INVALID_OPTIONS`), 而不是静默忽略.

#### 4.2 `AlarmOptions`

```ts
interface AlarmOptions {
    /** 响铃的墙上时间: "07:30", "07:30:15", 或 { hour, minute, second? }. 与 at 二选一. */
    time?: string | { hour: number; minute: number; second?: number };
    /** 一次性闹钟的日期, 或重复规则的起始日 (DTSTART), "YYYY-MM-DD" / Date / 时间戳. 省略时: 一次性闹钟取下一个到达 time 的日子, 重复闹钟取今天. */
    date?: string | Date | number;
    /** 一次性闹钟的绝对时刻, 与 time 二选一. */
    at?: Date | number;
    /** 重复规则: RRULE 字符串, 结构化规则, 或星期编号数组的简写 ([1, 2, 3, 4, 5] 即工作日). 省略即一次性. */
    repeat?: string | RepeatRule | number[];
    /** 排除的日期 (EXDATE), "YYYY-MM-DD" 数组. */
    exclude?: string[];
    /** IANA 时区; null 或省略为跟随设备. */
    timeZone?: string | null;
    /** 节假日过滤 (1.2.0). */
    holidays?: HolidayFilter | null;

    label?: string;             // 至多 100 个字符
    enabled?: boolean;          // 默认 true
    /** 脚本自用的分组标签, list({ tag }) 可筛选; 插件界面显示为小标签. */
    tag?: string;

    sound?: SoundOptions;
    vibrate?: boolean | number[]; // true 用默认节奏; 数组与 device.vibrate 同写法 (等待, 振动, 等待, 振动, ...)
    speak?: SpeakOptions;
    snooze?: { minutes?: number; max?: number } | false; // 默认 { minutes: 10, max: 3 }; false 不允许贪睡
    /** 无人处理时响多久自动停 (分钟), 默认 10, 范围 1 - 60. 停止时发 silence 事件. */
    autoSilenceMinutes?: number;
    /** 关闭方式 (1.3.0). */
    dismiss?: DismissOptions;
}
```

#### 4.3 重复规则

```ts
interface RepeatRule {
    freq: 'daily' | 'weekly' | 'monthly' | 'yearly';
    interval?: number;            // 每 N 个周期, 默认 1
    byDay?: (number | string)[];  // 0 - 6, 或 RRULE 写法 "MO", "2TU" (第 2 个周二), "-1FR" (最后一个周五)
    byMonthDay?: number[];        // 1 - 31, 负数从月末倒数 (-1 为最后一天)
    byMonth?: number[];           // 1 - 12
    byYearDay?: number[];
    byWeekNo?: number[];
    bySetPos?: number[];
    count?: number;               // 总次数, 与 until 二选一
    until?: string | Date | number;
    weekStart?: 0 | 1;            // 0 星期日, 1 星期一 (默认)
}
```

- 字符串写法接受 RRULE 原文 (`"FREQ=MONTHLY;BYDAY=-1FR"`, 可带 `RRULE:` 前缀); `Alarm.repeat` 总是回读为规范化的 RRULE 字符串, 另有 `repeatText` 给出本地化的描述 ("每月最后一个星期五").
- 不支持: `SECONDLY` / `MINUTELY` / `HOURLY`, `BYHOUR` / `BYMINUTE` / `BYSECOND` (一个闹钟只有一个响铃时刻, 由 `time` 决定), `RDATE`. 遇到时报 `INVALID_RULE`, 信息里写明不支持的部分.
- 每月 31 日这类不存在的日期按 RFC 5545 跳过 (2 月不响); 想要 "每月最后一天" 用 `byMonthDay: [-1]`.
- 轮班: "每 3 天" 即 `{ freq: 'daily', interval: 3 }`, 起点由 `date` 决定.

#### 4.4 响铃内容

```ts
interface SoundOptions {
    /** "default" 系统默认闹钟铃声 (默认); "silent" 不出声 (只振动或只朗读); ringtones.list() 给出的 uri; 或本地音频文件路径 (复制进插件). */
    source?: string;
    /** 0 - 1 的独立音量; 省略时用当前闹钟音量. */
    volume?: number;
    /** 渐强时长 (秒), 0 关闭, 默认 0, 至多 300. */
    rampSeconds?: number;
}

interface SpeakOptions {
    text: string;                 // 至多 500 个字符
    /** TTS 引擎包名, 例如 3-Macaw Speech 的 "io.github.supermonster003.autojs6.plugin.three.macaw.speech"; 省略用系统默认引擎. */
    engine?: string;
    language?: string;            // BCP 47
    rate?: number;
    pitch?: number;
    /** before: 先读再响铃; after: 铃声响 N 秒后压低铃声朗读 (默认); interleave: 铃声与朗读交替; only: 只朗读. */
    mode?: 'before' | 'after' | 'interleave' | 'only';
    /** 重复朗读的间隔 (秒), 0 只读一次. */
    repeatInterval?: number;
}
```

#### 4.5 `Alarm` 与查询

```ts
interface Alarm extends Readonly<AlarmOptions> {
    readonly id: string;                         // 插件生成的字符串, 闹钟以 "a_" 开头, 计时器以 "t_" 开头; 备份恢复后不变 (D24)
    readonly source: AlarmSource;                // 创建来源
    readonly updatedBy: AlarmSource;             // 最后修改来源
    readonly createdAt: number;
    readonly updatedAt: number;
    readonly repeat: string | null;              // 规范化的 RRULE
    readonly repeatText: string | null;          // 本地化描述
    readonly nextTrigger: number | null;         // 下一次响铃 (已计入节假日, 跳过与排除)
    readonly skipping: boolean;                  // 是否已跳过下一次
    readonly hasBindings: boolean;

    update(patch: Partial<AlarmOptions>): Alarm;
    remove(): boolean;
    enable(): Alarm;
    disable(): Alarm;
    skipNext(): Alarm;
    bind(script: string, options?: BindOptions): Binding;
}

type AlarmSource = 'user' | 'script' | 'tool' | 'intent';

interface Occurrence { readonly alarmId: string; readonly label: string | null; readonly at: number; }

interface Ringing {
    readonly id: string;                         // 闹钟或计时器的 id
    readonly kind: 'alarm' | 'timer';
    readonly label: string | null;
    readonly since: number;
    readonly snoozed: boolean;                   // 贪睡中
    readonly snoozeCount: number;
    readonly challenge: string | null;           // 1.3.0
}

interface AlarmFilter {
    source?: AlarmSource | AlarmSource[];
    tag?: string;
    enabled?: boolean;
    label?: string | RegExp;
}
```

`Alarm` 是调用时刻的快照; 方法会向插件发起调用并返回新的快照.

### 5. 事件与脚本联动

#### 5.1 事件

| 事件 | 时机 | 主要字段 |
|---|---|---|
| `beforeRing` | 响铃前 (1.2.0), 默认提前 60 s | `deadline`, 方法 `skip(reason?)`, `ring(overrides?)` |
| `ring` | 开始响铃 (含贪睡后再响) | `snoozeCount` |
| `snooze` | 贪睡 | `by`, `minutes`, `snoozeCount` |
| `dismiss` | 关闭 | `by` (`user` / `script` / `tool` / `challenge`), `challenge` |
| `silence` | 无人处理, 响满 `autoSilenceMinutes` 自动停 | `ringMillis` |
| `miss` | 错过触发时刻且超过补响窗口 (T16) | `scheduledAt`, `reason` (`off` / `late`) |
| `skip` | 本次未响: `skipNext`, 节假日过滤或 `beforeRing` 决定跳过 | `reason` (`skipNext` / `holiday` / `script`) |
| `change` | 闹钟被增删改 (任何来源) | `action` (`add` / `update` / `remove`), `alarmId`, `source` |

`AlarmEventName` 即上表第一列的 8 个名字. 共同字段: `type`, `kind` (`alarm` / `timer`), `alarmId` (计时器为其 id), `label`, `scheduledAt` (本次应响时刻), `firedAt`, `occurrence` (本次的唯一标识, 用于去重).

计时器到点同样发 `ring` / `snooze` / `dismiss` / `silence`, `kind` 为 `timer`, 因此一个监听器就能同时处理闹钟与计时器.

#### 5.2 实时监听

```js
alarms.on('ring', (e) => {
    if (e.kind === 'timer') toast(`${e.label} 到时间了`);
});
```

`on` 会让脚本保持运行 (与 `events.on` 相同); 脚本退出时自动注销.

#### 5.3 登记脚本

```ts
interface BindOptions {
    /** 需要启动脚本的事件, 默认 ['ring'] (D22). */
    events?: AlarmEventName[];
    /** beforeRing 的提前量 (秒), 默认 60, 范围 15 - 300. 仅 events 含 beforeRing 时有效. */
    leadSeconds?: number;
}

interface Binding {
    readonly alarmId: string;
    readonly script: string;           // 绝对路径
    readonly events: AlarmEventName[];
    readonly leadSeconds?: number;
}
```

- 每个 `(occurrence, 事件, 脚本)` 只启动一次. 同一闹钟可绑定多个脚本.
- 被启动的脚本里: `engines.myEngine().execArgv.alarm` 是事件的纯 JSON (同 `execArgv.mail`), `alarms.currentEvent()` 返回带方法的事件对象.
- 绑定存在宿主 (T12). 删除闹钟时宿主同步删除其绑定; 脚本文件不存在时跳过并在日志里记一条.
- 宿主在设备首次解锁前不能运行脚本: 此时照常响铃, 绑定脚本不启动, 解锁后也不补启动; 原因只记在插件日志 (`adb logcat -s ChimeTrigger`, `locked` / `unavailable`), 事件不带 `scriptSkipped` (T76).

#### 5.4 `beforeRing` (1.2.0)

流程: 插件在 `scheduledAt - leadSeconds` 用第二个精确闹钟唤醒自己, 发 `beforeRing` 给宿主; 宿主启动绑定脚本并分发给实时监听者; 脚本在 `deadline` (默认 `scheduledAt - 5 s`) 之前作出决定.

```js
// weather-brief.js, 绑定在 beforeRing 上
let e = alarms.currentEvent();
let w = http.get('https://example.invalid/weather').body.json();
if (w.dayOff) {
    e.skip('day off');
} else {
    e.ring({ speak: { text: `早上好, 今天${w.text}, 最高 ${w.high} 度`, mode: 'after' } });
}
```

- `skip()` 优先: 收到任何一个 `skip` 本次就不响 (发 `skip` 事件, `reason: 'script'`).
- `ring(overrides)` 只能改本次的 `label`, `sound`, `speak`, `vibrate`, 多个脚本的覆盖按到达顺序合并.
- 到 `deadline` 仍无决定, 或宿主未安装 / 未启用 / 设备未解锁 / 脚本出错, 一律照常响铃.
- 被节假日过滤掉的那次不触发 `beforeRing`, 直接发 `skip` (`reason: 'holiday'`).

### 6. 计时器与秒表 (1.1.0)

```ts
declare namespace alarms.timers {
    /** duration: 毫秒数, 或 "25m", "1h30m", "90s" 这类写法; 1 s 至 100 h. */
    function start(duration: number | string, options?: TimerOptions): Timer;
    function get(id: string): Timer | null;
    function list(): Timer[];
    function pause(id: string): Timer;
    function resume(id: string): Timer;
    /** 加时 (可为负), 结果须在 1 s 至 100 h 之间. */
    function addTime(id: string, millis: number | string): Timer;
    /** 回到初始时长并暂停. */
    function reset(id: string): Timer;
    function cancel(id: string): boolean;
}

interface TimerOptions {
    label?: string;
    sound?: SoundOptions;
    vibrate?: boolean | number[];
    speak?: SpeakOptions;
    autoSilenceMinutes?: number;
}

interface Timer {
    readonly id: string;
    readonly label: string | null;
    readonly durationMillis: number;
    readonly remainingMillis: number;
    readonly state: 'running' | 'paused' | 'ringing';
    readonly endsAt: number | null;   // running 时的到点时刻
    pause(): Timer; resume(): Timer; addTime(millis: number | string): Timer; reset(): Timer; cancel(): boolean;
    bind(script: string, options?: BindOptions): Binding;
}

declare namespace alarms.stopwatch {
    function state(): StopwatchState;
    function start(): StopwatchState;
    function pause(): StopwatchState;
    function resume(): StopwatchState;
    function lap(): Lap;
    function reset(): StopwatchState;
}

interface StopwatchState {
    readonly state: 'idle' | 'running' | 'paused';
    readonly elapsedMillis: number;
    readonly laps: Lap[];             // 至多 100 圈
}
interface Lap { readonly index: number; readonly lapMillis: number; readonly totalMillis: number; }
```

- 计时器至多 20 个并行; 运行中显示常驻通知 (倒计时), 到点进入与闹钟相同的响铃界面. 计时器关闭后即删除.
- 设备重启: 运行中的计时器按 "关机前的到点时刻" 恢复, 关机期间的时间计入; 恢复时已过点的, 按 T16 处理.
- 秒表只有一个 (与常见时钟应用相同), 不排程; 重启后转为暂停, 关机期间不计时.

### 7. 节假日 (1.2.0)

```ts
interface HolidayFilter {
    region: string;   // ISO 3166-1 alpha-2, 数据支持时可带下级地区 ("US-CA")
    /** skip: 从重复规则产生的日子里去掉节假日; only: 只保留节假日; workdays: 不看星期规则, 每个工作日都响 (周一至周五且非节假日, 加上数据里的补班日). */
    mode: 'skip' | 'only' | 'workdays';
}

declare namespace alarms.holidays {
    function regions(): { code: string; name: string; years: [number, number]; hasMakeupWorkdays: boolean }[];
    function isHoliday(date?: string | Date | number, region?: string): boolean;
    function isWorkday(date?: string | Date | number, region?: string): boolean;
    function list(region: string, year: number): { date: string; name: string; type: 'holiday' | 'workday' }[];
    function info(): { version: string; source: string; updatedAt: number };
    function refresh(): HolidayInfo;
    function refreshAsync(): Promise<HolidayInfo>;
}
```

- 数据来自开源节假日数据集 (D16, D17), 构建期生成紧凑的数据文件, 经 Shared-Assets 新类别 (暂名 `holiday-calendars`) 分发, 插件内置发布时的快照, 每周检查一次更新. 数据源在 P0 评估, 条件: 允许再分发的许可, 覆盖地区数, 是否含补班日 (中国大陆调休), 更新频率. 候选: python-holidays (MIT), date-holidays (许可待核).
- `workdays` 会加进星期规则以外的补班日, 所以此时 `repeat` 只能省略 (省略即 "每个工作日") 或为 `daily` 且间隔为 1, 否则报 `INVALID_OPTIONS`.
- `region` 省略时用插件设置里的默认地区 (首次按系统区域推断).
- 数据不含补班日的地区, `workdays` 退化为 "周一至周五且非节假日", `regions()` 的 `hasMakeupWorkdays` 为 false. 补班日是否准确完全取决于上游数据 (D17), 文档里写明.
- 当年数据缺失 (例如上游尚未发布次年安排) 时, 该年只按星期规则响铃, `capabilities()` 中给出警告; 不因缺数据而不响.

### 8. 关闭挑战 (1.3.0)

```ts
interface DismissOptions {
    challenge?: 'none' | 'math' | 'shake' | 'type' | 'script';  // 默认 none
    math?: { difficulty?: 1 | 2 | 3; count?: number };          // 默认 2 级, 3 题
    shake?: { count?: number };                                 // 默认 30 次
    type?: { text?: string };                                   // 省略时随机一句
    /** 脚本条件: 一直响到绑定脚本或任何脚本调用 alarms.dismiss(id). */
    script?: { fallback?: 'none' | 'math' | 'shake' | 'type' };
    allowSnooze?: boolean;                                      // 默认 true
}
```

安全阀 (所有挑战都适用):

- 挑战模式下 `autoSilenceMinutes` 默认 30, 上限 60, 到点自动停并发 `silence`.
- `script` 条件: 宿主不可用, 既无绑定脚本也无实时监听者, 设备未解锁, 或绑定脚本启动失败 / 结束运行时仍未调用 `dismiss`, 一律退回 `fallback` (默认 `none`, 即普通关闭).
- 插件设置里有 "紧急关闭" (连按 5 次音量键), 默认开启, 可关.

### 9. 系统时钟层 `alarms.system` (1.0.0, 不需要插件)

```ts
declare namespace alarms.system {
    /** ACTION_SET_ALARM. 返回 true 只表示有应用接收了 Intent, 不保证闹钟已建好. */
    function setAlarm(options: {
        hour: number; minute: number;
        label?: string;
        days?: number[];              // 0 - 6, 宿主转换为 Calendar 常量
        vibrate?: boolean;
        ringtone?: string | 'silent';
        skipUi?: boolean;             // 默认 true (D23)
        package?: string;             // 指定时钟应用
    }): boolean;
    /** ACTION_SET_TIMER, seconds 为 1 - 86400. */
    function setTimer(options: { seconds: number; label?: string; skipUi?: boolean; package?: string }): boolean;
    function showAlarms(options?: { package?: string }): boolean;
    /** API 26+. */
    function showTimers(options?: { package?: string }): boolean;
    /** API 23+. mode 省略为 next. */
    function dismissAlarm(options?: { mode?: 'next' | 'all' | 'time' | 'label'; hour?: number; minute?: number; label?: string; package?: string }): boolean;
    /** API 23+. */
    function snoozeAlarm(options?: { minutes?: number; package?: string }): boolean;
    /** API 28+. */
    function dismissTimer(options?: { package?: string }): boolean;
    /** AlarmManager.getNextAlarmClock(): 系统中任何应用的下一个闹钟, 没有则为 null. */
    function next(): { triggerTime: number; packageName: string | null } | null;
    /** 能处理某个动作的应用包名列表. */
    function handlers(action?: 'setAlarm' | 'setTimer' | 'showAlarms' | 'dismissAlarm'): string[];
}
```

- 低于所需 API 时报 `UNSUPPORTED`, 没有应用能处理时报 `NO_HANDLER`.
- 各 ROM 的时钟应用对 `skipUi`, `days`, `ringtone`, `dismissAlarm` 的搜索模式支持不一, 文档列出已知差异, 不在宿主里做兼容分支.
- 3-Chime Alarm 也接收这些 Intent (D21). 用户把 Chime 设为默认时钟, 或在选择器里选了 Chime 时, `alarms.system` 建的闹钟进入 Chime (来源记为 `intent`); 要指定某个时钟应用, 传 `package`.

### 10. 插件工具

插件短名 `chime` (须符合 `^[a-z][a-z0-9]{1,15}$`). 工具:

| 工具 | 作用 |
|---|---|
| `alarm_add` | 新建闹钟 (参数同 `AlarmOptions`, 不含本地音频文件路径与 `dismiss.challenge: 'script'`) |
| `alarm_list` | 列出闹钟 (可按来源, 标签, 启用状态筛选) |
| `alarm_update` / `alarm_remove` / `alarm_set_enabled` / `alarm_skip_next` | 修改 |
| `alarm_upcoming` | 接下来的 N 次响铃 |
| `alarm_dismiss` / `alarm_snooze` | 处理正在响的闹钟 |
| `timer_start` / `timer_list` / `timer_control` | 计时器 (1.1.0); `timer_control` 的 `action` 为 pause / resume / add / reset / cancel |
| `holiday_check` | 查询某日是否节假日 / 工作日 (1.2.0) |

不开放: 脚本绑定, `beforeRing`, 秒表 (T17).

### 11. 错误与限制

`alarms.AlarmError` 带 `code`:

| code | 含义 |
|---|---|
| `PLUGIN_UNAVAILABLE` / `PLUGIN_DISABLED` / `PLUGIN_INCOMPATIBLE` | 同 speech 的退化语义, 文案提示安装 / 启用 3-Chime Alarm |
| `PERMISSION_REQUIRED` | Android 12 - 12L 缺精确闹钟权限, 且 `add` / `update` / `setEnabled` 会留下启用的闹钟 (T60, T69); 缺通知 / 全屏权限不让调用失败, 闹钟照常响; `hint` 给出 `alarms.openPlugin('permissions')` |
| `INVALID_OPTIONS` / `INVALID_RULE` | 参数或 RRULE 不合法 (宿主侧先判) |
| `NOT_FOUND` | id 不存在 |
| `LIMIT_EXCEEDED` | 超出下表限制 |
| `NO_HANDLER` / `UNSUPPORTED` | 系统时钟层; `UNSUPPORTED` 也表示插件未实现该功能 (插件缺某功能时, 宿主对该功能的方法与选项 (如 1.0.0 的 `holidays`) 先报 `PLUGIN_INCOMPATIBLE`) |
| `IO_FAILED` | 铃声文件不存在, 读不到, 为空或没有可解码的音轨 (超过 20 MiB 是 `LIMIT_EXCEEDED`, T72), 或插件存不下数据 |
| `HOLIDAY_DATA_MISSING` | 请求的地区没有数据 |
| `TIMEOUT` / `SESSION_CLOSED` / `INTERNAL` | 宿主侧 |

限制 (提议值):

| 项 | 上限 |
|---|---|
| 闹钟总数 | 300, 其中启用的 200 (每个最多占 2 个 AlarmManager 槽位, 系统每 UID 上限 500) |
| 计时器并行数 | 20 |
| 秒表圈数 | 100 |
| 每个闹钟的绑定脚本 | 5 |
| `label` / `speak.text` | 100 / 500 个字符 |
| 铃声文件 | 20 MiB |
| `preview` / `upcoming` 条数 | 366 / 100 |

### 12. 示例

```js
// 工作日 7:30 起床, 按中国大陆节假日与调休 (省略 repeat 即每个工作日), 关掉后运行另一个脚本
let a = alarms.add({
    time: '07:30',
    holidays: { region: 'CN', mode: 'workdays' },
    label: '起床',
    sound: { source: 'default', volume: 0.8, rampSeconds: 30 },
    vibrate: [0, 500, 500, 500],
    snooze: { minutes: 9, max: 3 },
});
a.bind('./after-wake.js', { events: ['dismiss'] });

// after-wake.js
let e = alarms.currentEvent();
if (e && e.type === 'dismiss') {
    // 开灯, 播报日程 ...
}
```

```js
// 每月最后一个周五 18:00
alarms.add({ time: '18:00', repeat: 'FREQ=MONTHLY;BYDAY=-1FR', label: '写月报' });

// 轮班: 从 11 月 4 日起每 3 天一次
alarms.add({ time: '06:10', date: '2026-11-04', repeat: { freq: 'daily', interval: 3 }, tag: 'shift' });

// 明天放假就跳过所有用户闹钟 (D15)
let tomorrow = Date.now() + 864e5;
if (alarms.holidays.isHoliday(tomorrow, 'CN')) {
    alarms.list({ source: 'user', enabled: true }).forEach((x) => x.skipNext());
}
```

```js
// 番茄钟
let t = alarms.timers.start('25m', { label: '番茄钟', speak: { text: '休息一下', mode: 'only' } });

// 系统时钟
alarms.system.setAlarm({ hour: 6, minute: 50, label: '赶飞机' });
let next = alarms.system.next();
if (next) log(new Date(next.triggerTime), next.packageName);
```

---

## 附录 B. 待决事项

Q1 - Q6 已于 2026-11-03 拍板为 D20 - D25 (原表见附录 F), 编号不复用. 下表从 Q7 起; 调度会话在 "拍板时点" 之前用选择题请维护者拍板, 结果回填本表 "状态" 列与受影响的决策行. 子会话新增的事项从 Q10 起编号, 同时登记到看板 `D:/idea-projects/.claude/boards/3-chime.md` 的待决事项区 (写明编号, 问题, 选项 (标出推荐项与是否已先行), 影响的条目, 改选的返工量).

| 编号 | 问题 | 选项 (推荐项在前) | 影响的条目 | 改选的返工量 | 拍板时点 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| Q7 | Node / Bun 运行时是否在 1.0.0 提供 `alarms` 模块 | (a) 不提供, 1.0.0 只有 Rhino (推荐: Three 系列插件模块首发的惯例, 宿主侧只维护一套事件分发); (b) 同时提供 (需在 node-runtime / bun-runtime 的 shim 里桥接事件回调与 `execArgv.alarm`, P1.4 工作量约增加一半) | P1.4, P6.1, P6.2 | 改选 (b): P1.4 另加一个子条目, 文档加 "运行时可用性" 一段 | P1.4 开始前 | 已决 (2026-11-05 13:20, 维护者选 (a): 1.0.0 只有 Rhino) |
| Q8 | 1.2.0 节假日数据源 | P0.2 评估 (2026-11-05 核对; 实测 holidays 0.108 wheel, date-holidays 3.38.0, holiday-cn master): (a) python-holidays (推荐: MIT, 数据写在代码里; 0.108 (2026-11-02), 约双周发版 (2024 / 2025 各 24 版); 250 个国家 / 地区, 629 个下级行政区; 中国大陆补班日是结构化数据 (`weekend_workdays`), 2025 (5 天) / 2026 (6 天) 与国务院通知及 holiday-cn 逐日一致, 通知入库滞后 6 - 13 天; 另有 13 国带补班 (RU BY VN KZ UZ TW HU LV AZ KG AM MM UA); 已发通知之后的年份是推算的 (CN 2027 补班为 0), 生成器须截到已确认年份 (CN 现为 2026, 之后不输出或标 "未确认"), 每年 11 月通知后重建; 实测每地区每年 147 - 532 B gzip (CN 978 B raw / 321 B gzip), 250 国 x 2020 - 2035 全类别每地区一个文件 gzip 合计 324 KB (平均 1.3 KB), 下级行政区另加约 16 KB / 年 gzip; 构建期离线生成约 8 秒; 节日名多为本地语言 + en_US (214 国有英文), 其他 UI 语言需回退英文或自建翻译表; 署名为 MIT 全文与版权行); (b) date-holidays (代码 ISC, 数据 CC BY-SA 3.0: 导出的数据也要按 BY-SA 发布并保留约 217 条 Wikipedia 来源署名; 3.38.0, 207 国; 没有补班日类型, CN 特殊日停在 2021; CN 每年 206 B gzip); (c) python-holidays 出全量, 中国部分用 holiday-cn (MIT, 通知当天发版, 仅中国大陆 2007 - 2026) 覆盖并逐日比对, 不一致时构建失败 (多一个固定上游与一份 MIT 声明, 约 60 - 100 行合并脚本; 收益是通知当天即可重建 CN 数据). 已排除: workalendar (2023 年后停更), Nager.Date (离线包需 license key), OpenHolidays API (无中国), Google 日历 / timeanddate (条款不允许再分发), chinese-calendar (仅中国, 与 holiday-cn 同类) | P8.1 - P8.3, `THIRD_PARTY_NOTICES.md` | 对数据源的依赖集中在 P8.1 的生成脚本, 改选只重做 P8.1 | P8 开始前 | 已决 (2026-11-05 15:25, 维护者选 (a): python-holidays 内置生成, P8.1 用) |
| Q9 | `alarms.system.*` 未指定 `package` 时的目标 | (a) 交给系统解析 (推荐: 隐式 `AlarmClock` Intent, 与任何第三方应用的行为一致, 由用户在系统选择器或默认应用设置里决定); (b) 已安装 3-Chime Alarm 时改为显式指向它 (`alarms` 与 `alarms.system` 的行为更一致, 但绕过用户选定的默认时钟); (c) 优先非 Chime 的时钟应用 (保证 "system" 的字面含义, 需枚举 `handlers()` 并排除 Chime, 多个候选时仍要选择器) | P1.5, P6.1 (`alarms.system` 一节), P2.6 联调 | 三者都只改 `alarms.system` 的目标解析一处与文档一段 | P1.5 开始前 | 已决 (2026-11-05 13:20, 维护者选 (a): 交给系统解析) |
| Q10 | ROADMAP P1.3 要求协议文档入 `docs/dev/` 目录索引, 但 `docs/dev` 没有索引文件 (约 200 个文件, 其他协议文档也未入索引) | (a) 不建索引, 协议文档由 `AlarmPluginHost` KDoc, 本 roadmap 与共享笔记引用 (推荐, chime-p1-host 已先行); (b) 新建 `docs/dev/README.md`, 只列各插件协议文档; (c) 为 `docs/dev` 全部文件建完整索引 | P1.3 | (b) 约 30 行新文件; (c) 较大, 需逐个分类 | P1.3 | 已决 (2026-11-05 13:20, 维护者选 (a): 保持现状, 已实施) |
| Q11 | `alarms` 的 api-coverage 门禁 (文档 / d.ts / Ace 名称门) 在 P6.1 / P6.2 前如何处理 | (a) `policy.json` 的 `deferredNameGate` 推迟 `alarms`, P6.1 / P6.2 落地后删除该行 (推荐, chime-p1-script 已先行); (b) 现在就在文档站与 d.ts 补占位, 不推迟 | P1.4 第 3 条, P6.1, P6.2 | (b) 提前做 P6 的一部分, 三个仓库各一笔 | P6.1 开始前 | 已决 (2026-11-05 15:35, 维护者选 (a), 已实施) |
| Q12 | 宿主 debug 构建 `VERSION_BUILD` 仍为 6640, 低于插件 `requiresHostVersion` 6641, P2.7 联调时插件中心会 "启用失败" | (a) P2.7 子会话在隔离克隆里临时把 `VERSION_BUILD` 改为 6641 构建, 不提交 (推荐, 未先行); (b) 宿主提交把 `VERSION_BUILD` 升到 6641 (共享 checkout, 其他会话的构建随之变化); (c) 插件 `requiresHostVersion` 降为 6640 (不含 `alarm-api` 的基线构建也会被接受) | P2.7, P6.4 | 小, 一行配置 | P2.7 开始前 | 已决 (2026-11-05 15:35, 维护者选 (a): P2.7 在隔离克隆里临时改为 6641 构建, 不提交, 插件保持 6641) |
| Q13 | Xiaomi Pad 的 `next()` 读回证据 | (a) 接受 `dumpsys alarm` 核对 (时钟 `RTC_WAKEUP` 15:30 已排程) 作为该机证据, 差异写进 P6.1 (推荐, chime-p1-script 已先行); (b) 等陈旧 `AlarmClockInfo` 消失后在该机重跑脚本补 `next()` 读回 | P1.5 证据 | (b) 重跑一次脚本约 10 分钟, 需临时装测试宿主 | P6.1 开始前 | 已决 (2026-11-05 15:35, 维护者选 (a), 已实施) |
| Q14 | 脚本自己 `alarms.remove()` 时, 绑定 `change` 的脚本可能收不到这次 `remove`: 宿主 `AlarmPluginHost` 在 `removeAlarm` 返回 `removed: true` 后立即删除该闹钟的绑定并重发提示表, 插件的 `ALARM_TRIGGER` 广播在返回前已发出, 但经系统转发到达宿主接收器时绑定多半已删 (界面, 工具, Intent 与其他宿主的删除不受影响) | (a) 保持现状, 在协议文档与 P6.1 的 `change` 一节写明 "由本宿主脚本删除时不触发本宿主绑定的 `change` 脚本" (推荐, 已先行: 插件与宿主均未改); (b) 宿主 `removeAlarm` 后不立即删绑定, 由接收器处理 `remove` 广播时删除 (已有逻辑), 另加 10 秒兜底删除 (宿主 `core/plugin/alarm` 约 15 行 + 测试) | P1.4, P2.5, P2.7 (协议文档), P6.1 | (b) 宿主一处约 15 行与一个 JVM 用例, 插件不变 | P6.1 开始前 | 待决 (chime-p2 登记, 不阻塞) |

---

## 附录 C. 证据等级

| 等级 | 含义 | 写法 |
| --- | --- | --- |
| E1 | 代码落地 | 本地提交 hash (宿主与文档类仓库只本地提交时注明 "仅本地") |
| E2 | 自动化测试 | 测试类名与用例数, 本地或 CI 通过; instrumentation 注明设备或 AVD |
| E3 | 设备实测 | 设备型号, API, APK 摘要或构建号, 日期, 关键数值 (触发偏差, 时长等); 凭据为 `device_smoke.py` 输出或 logcat 关键行, 不贴全文 |
| E4 | 界面核对 | 截图路径 (明暗各一), 设备与分辨率 |
| E5 | 发布凭据 | tag, Release 链接, APK 文件名与 SHA256, 索引清单文件名与索引仓库提交 hash |

条目证据按最高适用等级写, 一个条目可以同时有多个等级. 未执行的验证如实写 "未执行" 与原因, 不以推测代替.

---

## 附录 D. 设备矩阵与基线

### D.1 设备矩阵 (1.0.0)

| 设备 | serial | API | 角色 | 1.0.0 结果 |
| --- | --- | --- | --- | --- |
| Sony XQ-AT72 | QV710AF65F | 31 | 主联调机; `SCHEDULE_EXACT_ALARM` 需用户授权的路径; P5.3 时间与系统事件场景; P4.2 MCP 调用 | 待测 |
| Sony XQ-DQ72 | QV770340J7 | 33 | `USE_EXACT_ALARM` 路径; 截图机 (1096 x 2560); 锁屏全屏响铃 | 待测 |
| Redmi 22120RN86C | bek749scrwv4wo8h | 33 | MIUI 后台限制与自启动引导文案核对 | 待测 |
| Xiaomi Pad 23046RP50C | 968e9f18 | 35 | API 34+ 全屏权限未授予的退化; 平板布局; T6 复核 | 待测 |
| OnePlus OPD2413 | 63751f63 | 35 | ColorOS 激活与唤醒 (P5.4) | 待测 |
| Sony G8441 | BH900ASK9E | 28 | 低 API 真机冒烟 (原装宿主包, 不开新模块层开关) | 待测 |
| AVD_API_24 | emulator | 24 | 最低 API 与直接启动 | 待测 |
| AVD_API_33 | emulator | 33 | CI 用例集 (`connected-avd`) | 待测 |
| AVD_API_35_16K / AVD_API_36.1_16K | emulator | 35 / 36 | T6 实测, 全屏权限退化, 16 KB 页环境 (无原生库, 只核对安装与运行) | 待测 |
| 租用机 SM-A566B / SM-F971B | 在线 (端口以实际为准) | 36 / 37 | 按需: 新 API 行为抽查 (租用机的 adb shell 经代理过滤, 长命令用 detached 方案) | 按需 |

设备协调: 同一时间一台设备只给一个会话用; 构建宿主前与其他会话排队; 手机上的授权对话框 (精确闹钟, 通知, 全屏, MCP 配对) 由维护者操作, 子会话停下说明要确认什么.

### D.2 基线 (P5.6 填, 之后每个版本复测一次)

| 指标 | 设备 | 1.0.0 | 备注 |
| --- | --- | --- | --- |
| 响铃触发偏差 P50 / 最大 (ms, 10 次) | XQ-AT72 | 待测 | `scheduledAt` 与 `firedAt` 之差 |
| 响铃触发偏差 P50 / 最大 (ms, 10 次) | Xiaomi Pad | 待测 | |
| 待机一夜电量差 (无闹钟 vs 3 个启用闹钟, 百分点) | 同一台机 | 待测 | |
| 进程被杀后到点至响铃界面可见 (ms) | XQ-DQ72 | 待测 | `am force-stop` 后 |
| release APK 体积 (MiB) | - | 待测 | 单 APK, 无 `lib/` |

---

## 附录 E. 已评估暂缓

与第 3 节 "非目标" 不同, 本表的事项将来可能做, 只是当前版本范围不含; 到复评时点再决定.

| 事项 | 结论 | 复评时点 |
| --- | --- | --- |
| 闹钟导出 / 导入 (JSON 备份) | 1.0.0 不做; 数据模型已用稳定字符串 id (D24) 并记录来源, 为日后导入导出留余地 | 1.1.0 发布后 |
| 自写 RRULE 引擎 | 以 lib-recur 为首选 (T10), 评估不合格时才自写; P2.2 起 `BYWEEKNO` 与带 `BYYEARDAY` / `BYSETPOS` 的 YEARLY 规则由插件展开 (T52), 其余仍用 lib-recur | lib-recur 新版本发布时 |
| Room / SQLite 存储 | 用 JSON 文档 (T21); 300 条上限下内存查询足够 | 需求超过上限时 |
| `alarms.system` 的 ROM 兼容分支 | 不做, 只在文档列已知差异 (P6.1) | 用户反馈集中时 |
| 从系统日历事件生成提醒 | 不做, 接近非目标 (读取其他应用的数据) | 不计划 |
| 插件界面里直接选脚本绑定 | D20 决定绑定存宿主侧, 界面只显示与解除 | 不计划 |

---

## 附录 F. 设计稿原待定表 (Q1 - Q6, 已于 2026-11-03 拍板为 D20 - D25)

六项都按推荐项决定, 结果见第 1 节 D20 - D25. 原表留作记录:

| 编号 | 问题 | 选项 (推荐项在前) | 影响 |
| --- | --- | --- | --- |
| Q1 | 登记脚本的绑定存在哪 | (a) 宿主侧 (推荐, T12): 插件不能让宿主运行任意路径, 插件界面只显示 "有脚本绑定" 并可解除; (b) 插件侧随闹钟保存: 插件界面可直接选脚本, 但宿主要信任插件传来的路径 | 附录 A 第 5.3 节, 协议文档, 插件界面 |
| Q2 | 插件是否作为系统时钟的 Intent 目标 | (a) 是 (推荐): 声明 `SET_ALARM` / `SET_TIMER` / `SHOW_ALARMS` / `DISMISS_ALARM` 等 intent-filter, 语音助手 (含 3-Macaw Speech 的语音命令) 与 `alarms.system` 可以把闹钟建进 Chime, 来源记为 `intent`; (b) 否 | 1.0.0 范围约增加一项 |
| Q3 | `bind` 的默认事件 | (a) `['ring']` (推荐); (b) 不给默认, 必须写 `events` | 附录 A 第 5.3 节 |
| Q4 | `alarms.system` 的 `skipUi` 默认值 | (a) true (推荐, 脚本自动化通常不想停在时钟界面); (b) false, 与 Android 默认一致 | 附录 A 第 9 节 |
| Q5 | 闹钟 id 的形态 | (a) 字符串 (推荐, 插件生成, 备份恢复不变); (b) 数字自增, 与 `tasks` 的 `TimedTask.id` 相同 | 全部接口 |
| Q6 | 插件自身的版本节奏与宿主的关系 | (a) 插件 1.0.0 可先于宿主发布, 界面独立可用, 脚本模块等宿主新版本 (推荐, 同 Macaw); (b) 等宿主新版本一起发布 | 发布计划 |

设计稿第 15 节 "立项时的阶段草图" 已由第 5 节的阶段总览取代, 不再保留.
