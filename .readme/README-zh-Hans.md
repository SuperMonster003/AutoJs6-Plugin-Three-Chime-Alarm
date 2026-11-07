<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>闹钟应用, 支持日历重复规则, 朗读响铃与响铃时联动脚本</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 语言 (Languages)

******

当前 README.md 支持以下语言:

- 简体中文 [zh-Hans] # 当前
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ar.md)

******

### 简介

******

3-Chime Alarm 是 AutoJs6 的闹钟插件, 也是不装 AutoJs6 也能单独使用的闹钟应用. 闹钟按 RFC 5545 日历规则重复 (例如每月最后一个星期五, 或从某天起每隔 3 天), 响铃时可以朗读文字, 也可以启动脚本或通知正在运行的脚本. 计时器与秒表, 节假日跳过与关闭挑战将在后续版本加入.

插件使用 Android 精确闹钟自行调度并响铃, 因此 AutoJs6 没有运行时闹钟也会准时响. AutoJs6 脚本通过全局模块 `alarms` 查询, 新建, 修改与监听插件中的闹钟; 子命名空间 `alarms.system` 直接调用系统时钟应用, 未安装插件时也能使用. 插件也可以接收其他应用发出的设置闹钟请求.

> 1.0.0 提供闹钟, 插件界面, 插件工具与 `alarms` 脚本模块. 在插件中心启用本插件, 使用插件工具与 `alarms` 模块需要 AutoJs6 6.8.1 (内部版本号 6641) 或更高版本; 在更早的 AutoJs6 上 (包括 6.8.0), 插件只能作为独立的闹钟应用使用. 计时器, 秒表, 节假日与关闭挑战的进度见开发路线图.

******

### 功能亮点

******

- 闹钟: 单次或重复闹钟. 重复规则支持工作日, 每月第几个星期几, 月末, 间隔天数, 截止日期与次数, 也可以排除指定日期或只跳过下一次响铃.
- 响铃: 系统铃声或导入的本地音频, 朗读文字, 独立音量与渐强, 自定义振动, 贪睡与自动停止, 锁屏时显示全屏响铃界面; 多个闹钟同时到期时依次响铃.
- 准时可靠: 设备重启, 修改时间或时区, 插件升级后自动重新排程, 开机后未解锁也会响铃; 关机错过的闹钟在 5 分钟内补响, 超过则留下一条通知.
- 脚本联动: 闹钟可以登记脚本文件, 响铃, 贪睡, 关闭等事件发生时由 AutoJs6 启动, 脚本不必常驻; 正在运行的脚本也可以实时监听这些事件.
- 插件工具: 经 MCP Server 插件或 `plugins.tools.call` 新建, 查询, 修改与删除闹钟, 处理正在响的闹钟 (9 个 `alarm_*` 工具).
- 系统时钟对接: 可以处理其他应用发出的设置, 显示, 关闭与贪睡闹钟请求.
- 界面: 语言, 夜间模式与主题色默认跟随 AutoJs6, 也可以单独设置; 可以切换启动器图标; 设置页可以设定铃声, 振动, 贪睡与自动停止的默认值.
- 计时器与秒表 (1.1.0 规划): 多个计时器并行, 可以暂停, 继续与加时; 秒表支持计次.
- 节假日 (1.2.0 规划): 内置多地区开源节假日数据, 法定节假日跳过响铃, 调休补班日照常响铃; 也可以用 `beforeRing` 脚本钩子决定是否响铃.
- 关闭挑战 (1.3.0 规划): 算术题, 摇一摇, 照打一句话或脚本条件, 都带安全阀, 确保闹钟总能被关闭.

******

### 截图

******

截图来自 Sony XQ-DQ72, 上排为浅色主题, 下排为深色主题.

| 闹钟列表 | 编辑闹钟 | 响铃 | 设置 |
| :---: | :---: | :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-light.png?raw=true" alt="list-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-light.png?raw=true" alt="edit-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-light.png?raw=true" alt="ring-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-light.png?raw=true" alt="settings-light" width="200" /> |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-dark.png?raw=true" alt="list-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-dark.png?raw=true" alt="edit-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-dark.png?raw=true" alt="ring-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-dark.png?raw=true" alt="settings-dark" width="200" /> |

******

### 使用方法

******

1. 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases) 下载 APK 并安装. 安装包不区分 ABI, 适用于全部设备.
2. 打开插件, 在权限页逐项处理: 通知, 精确闹钟 (Android 12 与 12L 需要手动允许), 全屏提醒 (Android 14 起可能未授予), 以及电池优化, 后台限制与厂商自启动. 之后就可以像普通时钟应用一样使用.
3. 要在脚本中使用 `alarms` 模块或使用插件工具, 请将 AutoJs6 升级到 6.8.1 (内部版本号 6641) 或更高版本, 并在插件中心启用本插件 (安装向导的 "工具" 分类).

> 插件支持 Android 7.0 (API 24) 及以上的设备. 只有一个适用于全部设备的安装包; 其中唯一的原生库来自界面框架 Compose, 已按 16 KB 内存页对齐, 在 16 KB 内存页的设备上也能运行.

******

### 脚本 API

******

脚本通过全局模块 `alarms` (也可写作 `$alarms`, 仅 Rhino) 使用这些能力, 完整说明见 [AutoJs6 文档](https://docs.autojs6.com/#/alarms):

```javascript
let a = alarms.add({ time: "07:30", repeat: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", label: "Wake up" });
a.bind("./after-wake.js", { events: ["dismiss"] });

alarms.add({ time: "18:00", repeat: "FREQ=MONTHLY;BYDAY=-1FR", label: "Monthly report" });
alarms.add({ time: "06:10", date: "2026-11-04", repeat: { freq: "daily", interval: 3 }, tag: "shift" });

alarms.on("ring", (e) => toast(e.label));

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

- `alarms.add(options)` 新建闹钟并返回闹钟快照; `alarms.list`, `get`, `update`, `remove`, `setEnabled`, `skipNext` 与 `cancelSkip` 管理插件中的全部闹钟, 包括在插件界面建的闹钟.
- `repeat` 接受 RRULE 字符串 (例如 `FREQ=MONTHLY;BYDAY=-1FR`), 星期编号数组或结构化对象; `alarms.preview(options)` 列出接下来的响铃时间, 便于核对规则.
- `alarms.bind(id, script, options)` (或闹钟快照的 `bind`) 登记脚本, 事件发生时由 AutoJs6 启动, 事件数据可通过 `alarms.currentEvent()` 读取; `alarms.on('ring', listener)` 在脚本运行期间实时监听.
- `alarms.ringing`, `dismiss` 与 `snooze` 处理正在响的闹钟; `alarms.ringtones.list()` 列出可用的铃声.
- `alarms.system` 直接调用系统时钟应用 (`setAlarm`, `setTimer`, `showAlarms`, `dismissAlarm`, `next` 等), 不需要安装插件.

******

### 系统时钟对接

******

插件可以作为系统的闹钟应用, 处理其他应用 (例如日历, 语音助手与自动化工具) 发出的标准时钟请求. 发出请求的应用需要持有 `com.android.alarm.permission.SET_ALARM` 权限:

- 设置闹钟 (`SET_ALARM`): 读取时间, 标签, 星期, 振动与铃声; 请求要求跳过界面时直接保存, 否则打开预填的编辑页.
- 显示闹钟 (`SHOW_ALARMS`): 打开闹钟列表.
- 关闭闹钟 (`DISMISS_ALARM`): 结束正在响或贪睡中的闹钟; 没有正在响的闹钟时, 24 小时内要响的闹钟改为跳过这一次; 匹配到多个闹钟时由用户选择.
- 贪睡 (`SNOOZE_ALARM`): 让正在响的闹钟贪睡.
- 方向相反的 `alarms.system`: 脚本用它把请求发给系统默认的时钟应用 (本插件或其他时钟应用), 不需要安装插件; 各品牌时钟应用的差异见 AutoJs6 文档.

******

### 兼容性

******

插件支持 Android 7.0 (API 24) 及以上. 在插件中心启用本插件, 使用插件工具与 `alarms` 模块需要 AutoJs6 6.8.1 (内部版本号 6641) 或更高版本. 在更早的 AutoJs6 上 (例如内部版本号为 6640 的 6.8.0), 插件中心提示需要更高版本的 AutoJs6 且不能启用本插件, 插件工具与 `alarms` 模块不可用, 但插件仍可作为独立的闹钟应用使用, 也照常处理系统时钟请求. `alarms` 模块只支持 Rhino 引擎, 其中的 `alarms.system` 不需要安装插件.

1.0.0 发布前在以下设备上完成验证 (数据见开发路线图附录 D):

| 设备 | Android | 结果 |
| --- | --- | --- |
| Sony XQ-AT72 | 12 (API 31) | 通过: 到点偏差中位数 44 ms; 锁屏响铃, 重启后到点响铃, 修改时间, 时区与夏令时, 经 MCP Server 调用插件工具 |
| Sony XQ-DQ72 | 13 (API 33) | 通过: 到点偏差中位数 13 ms; 锁屏全屏响铃, 进程被清理后到点约 1.1 秒显示响铃界面 |
| Redmi 22120RN86C (MIUI) | 13 (API 33) | 通过: 到点偏差中位数 40 ms; 开机后系统多次结束插件进程, 到点仍按时响铃 |
| Xiaomi Pad 23046RP50C | 15 (API 35) | 通过: 到点偏差中位数 32 ms; 未授予全屏提醒时以悬浮通知响铃, 重启后未解锁也按时响铃 |
| OnePlus OPD2413 (ColorOS 15) | 15 (API 35) | 通过 (需打开自动启动): 到点偏差中位数 23 ms; 在 设置 > 应用 > 自动启动 中打开后, 进程被清理也按时响铃 (见常见问题) |
| Sony G8441 | 9 (API 28) | 通过: 到点偏差中位数 41 ms; 锁屏响铃, 重启后未解锁也按时响铃 |
| Android 模拟器 | 7.0 - 16 (API 24 - 36) | 通过: 最低版本全流程, 首次解锁前响铃, 16 KB 内存页 |

******

### 常见问题

******

#### 不装 AutoJs6 能用吗?

能. 插件自带完整的闹钟界面, 可以单独当作闹钟应用使用. 只有在脚本需要管理闹钟, 联动脚本或使用插件工具时才用到 AutoJs6.

#### AutoJs6 没有运行时闹钟还会响吗?

会. 闹钟由插件通过 Android 精确闹钟调度并响铃, 不依赖 AutoJs6 进程. 登记了脚本时, 插件在响铃时唤醒 AutoJs6 启动该脚本.

#### 为什么 AutoJs6 6.8.0 的插件中心不能启用本插件?

插件中心启用本插件, 插件工具与 `alarms` 模块需要 AutoJs6 6.8.1 (内部版本号 6641) 或更高版本, 而 6.8.0 的内部版本号为 6640. 升级 AutoJs6 后即可启用; 在此之前插件可以作为独立的闹钟应用使用.

#### 闹钟没有按时响, 应该检查什么?

先打开插件的权限页, 确认通知与精确闹钟已允许, 并按页面说明关闭电池优化, 解除后台限制, 允许自启动. 勿扰模式默认允许闹钟, 如果改过勿扰设置, 请确认闹钟仍被允许. 关机或系统推迟导致错过的闹钟, 5 分钟内会补响, 超过则不响并留下一条通知.

#### OPPO, 一加, realme (ColorOS) 上闹钟不响怎么办?

ColorOS 有两处相互独立的开关, 都要为 3-Chime Alarm 打开: 应用详情里的 "允许自启动" / "关联启动" / "后台活动", 以及 设置 > 应用 > 自动启动 列表. 只开前者时, 插件进程被清理 (例如在最近任务里划掉) 后, 系统会拦下到点的闹钟. 联动脚本时 AutoJs6 也要在两处打开. 另外请不要在最近任务里划掉插件, 可以把它锁定. 插件权限页在 ColorOS 上有对应的引导行; 设置页的 "保持后台运行" 会常驻一条静默通知, 进一步减少插件被清理的机会, 耗电略有增加.

#### ColorOS 上锁屏响铃时只有铃声和通知, 没有弹出响铃界面?

ColorOS 新装应用的 "横幅" 通知默认关闭, 系统因此不显示全屏响铃界面. 在插件的通知设置里打开 "横幅" 即可, 权限页的 "横幅通知" 一行可以直达该设置; 也可以在权限页允许 "悬浮窗" 作为后备.

#### 强行停止插件后闹钟不响了?

"强行停止" 会让系统清除插件的全部闹钟排程, 停止期间到点不响, 这是 Android 的规则. 从启动器打开一次插件即可恢复排程; 恢复时尚在 5 分钟内的闹钟会补响.

#### 小米 (MIUI, HyperOS) 需要特别设置吗?

实测 Redmi (MIUI) 在开机后会多次结束插件进程, 到点时系统仍会拉起插件并按时响铃. 仍建议按权限页的说明设置自启动与省电策略.

#### 设备重启后还没解锁, 闹钟会响吗? 联动的脚本呢?

闹钟会响, 插件在首次解锁之前也能运行. 联动的脚本不会启动, 因为 AutoJs6 在首次解锁之前不能运行脚本, 解锁后也不会补启动.

******

### 权限与安全

******

插件申请的权限与用途:

- 精确闹钟 (`USE_EXACT_ALARM`, Android 12 与 12L 为 `SCHEDULE_EXACT_ALARM`): 按时响铃. Android 13 起安装即授予, Android 12 与 12L 需要在系统设置中允许 "闹钟和提醒".
- 通知 (`POST_NOTIFICATIONS`): 显示正在响的闹钟与错过的闹钟. 缺少时闹钟照常发声, 但没有通知与响铃界面.
- 全屏提醒 (`USE_FULL_SCREEN_INTENT`): 在锁屏上显示响铃界面. Android 14 起可能未授予, 缺少时以悬浮通知响铃.
- 悬浮窗 (`SYSTEM_ALERT_WINDOW`, 可选): 系统不显示全屏提醒时 (例如 ColorOS 的横幅通知关闭) 用来打开响铃界面, 只在权限页由用户按需允许.
- 开机启动 (`RECEIVE_BOOT_COMPLETED`): 重启后重新排程, 首次解锁前也能响铃.
- 前台服务 (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`): 响铃期间保持发声; 设置页的 "保持后台运行" 开启时也用它常驻 (默认关闭).
- 振动, 唤醒锁与音频设置 (`VIBRATE`, `WAKE_LOCK`, `MODIFY_AUDIO_SETTINGS`): 振动, 响铃时保持设备唤醒, 以及独立的闹钟音量在响铃后还原.
- 网络 (`INTERNET`, `ACCESS_NETWORK_STATE`): 只用于设置页的更新检查 (1.2.0 起也用于刷新节假日数据). 只在打开闹钟列表时按设定的频率检查, 计量网络默认不检查; 排程, 响铃与 AutoJs6 联动从不联网.
- AutoJs6 插件权限 (`org.autojs.permission.PLUGIN`): 与 AutoJs6 通信.

插件在设计上尽量收窄权限面:

- 受保护的入口: 能力服务与激活入口受 AutoJs6 插件权限 (`org.autojs.permission.PLUGIN`) 保护, 其他应用无法直接调用; 接收系统时钟请求的入口要求调用方持有 `com.android.alarm.permission.SET_ALARM`.
- 网络: 只用于检查插件更新 (连接 GitHub, 可在设置页关闭自动检查), 不上传闹钟或任何个人数据.
- 存储: 闹钟保存在插件私有目录, 不参与云备份; 本地铃声通过系统文件选择器导入, 不申请存储权限.
- 脚本权限: 脚本可以通过 `alarms` 模块读写插件中的全部闹钟, 包括在插件界面建的闹钟; 登记的脚本保存在 AutoJs6 中, 插件界面只标记闹钟有脚本绑定并可以解除.

******

### 插件接口

******

以下信息面向 AutoJs6 宿主与插件开发者, 宿主通过这些标识发现插件并完成能力协商:

```text
application id: io.github.supermonster003.autojs6.plugin.three.chime.alarm
plugin id: three-chime-alarm
engine: alarm
variant: default
info service: org.autojs.plugin.INFO (category three-chime-alarm)
capability service: org.autojs.plugin.ALARM (category alarm)
wake action: org.autojs.plugin.action.WAKE
minimum host build: 6641
native libraries: libandroidx.graphics.path.so only (Compose graphics-path, 4 ABIs, uncompressed, 16 KB aligned; a single APK for every ABI)
```

******

### 开发路线图

******

功能规划, 设计决策与完成情况以可勾选清单维护在 ROADMAP.md 中. 未勾选条目表示规划意向, 不代表当前版本能力.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/ROADMAP.md)

******

### 发行历史

******

#### v1.0.0

_2026/11/07_

- `提示` 插件中心启用, 插件工具与 alarms 脚本模块需要 AutoJs6 6.8.1 (内部版本号 6641) 或更高版本; 在更早的 AutoJs6 上 (例如 6.8.0) 插件只能作为独立的闹钟应用使用
- `提示` ColorOS (OPPO, 一加, realme) 上请在应用详情与 设置 > 应用 > 自动启动 两处为插件打开自启动, 否则插件进程被清理后闹钟不会响 (参阅 README > 常见问题)
- `新增` 闹钟: 单次或重复闹钟, 重复规则按 RFC 5545 日历规则 (工作日, 每月第几个星期几, 月末, 间隔天数, 截止日期与次数), 可以排除日期或只跳过下一次
- `新增` 响铃: 系统铃声或导入的本地音频, 朗读文字, 独立音量与渐强, 自定义振动, 贪睡与自动停止, 锁屏全屏响铃界面, 多个闹钟同时到期时依次响铃
- `新增` 排程: 重启, 修改时间或时区与插件升级后自动重新排程, 首次解锁前也能响铃, 错过的闹钟 5 分钟内补响, 超过则留下一条通知
- `新增` 插件界面: 闹钟列表, 编辑页, 响铃界面, 权限页与设置页; 语言, 夜间模式与主题色默认跟随 AutoJs6, 可以切换启动器图标
- `新增` 设置页: 铃声, 振动, 贪睡与自动停止的默认值, 时间格式, 自动检查更新, 以及可选的 "保持后台运行" (常驻一条静默通知, 减少插件被系统清理)
- `新增` 权限页逐项检查通知, 精确闹钟, 全屏提醒, 电池优化, 后台限制与厂商自启动, 在 ColorOS 上另有自动启动列表与横幅通知的引导
- `新增` AutoJs6 脚本模块 alarms / $alarms (需要 AutoJs6 6.8.1, 仅 Rhino): 闹钟的新建, 查询, 修改与删除, 事件监听, 登记由 AutoJs6 在事件发生时启动的脚本, 铃声列表 (参阅 [AutoJs6 文档](https://docs.autojs6.com/#/alarms))
- `新增` 插件工具 alarm_add, alarm_list, alarm_update, alarm_remove, alarm_set_enabled, alarm_skip_next, alarm_upcoming, alarm_dismiss 与 alarm_snooze, 可经 MCP Server 插件或 plugins.tools.call 调用
- `新增` 系统时钟对接: 处理其他应用发出的设置, 显示, 关闭与贪睡闹钟请求
- `新增` 只有一个适用于全部设备的 APK, 支持 16 KB 内存页的设备
- `依赖` 附加 AndroidX Core KTX 版本 1.15.0
- `依赖` 附加 AndroidX Activity Compose 版本 1.12.2
- `依赖` 附加 Jetpack Compose BOM 版本 2026.09.00 (插件界面)
- `依赖` 附加 Kotlin Coroutines Android 版本 1.11.0
- `依赖` 附加 Material Color Utilities 版本 4.1.1 (主题色)
- `依赖` 附加 lib-recur 版本 0.17.1 (计算闹钟的重复规则)
- `依赖` 附加 Desugar JDK Libs 版本 2.1.5 (让 Android 7.x 也能使用日期与时区运算)

##### 更多发行历史可参阅

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hans.md)

******

### 构建

******

本节面向希望从源码构建插件的开发者.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### 许可

******

项目代码使用 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE). 第三方组件与数据的许可见第三方声明.

- [第三方声明 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/THIRD_PARTY_NOTICES.md)

******

### 相关链接

******

- AutoJs6 项目: https://github.com/SuperMonster003/AutoJs6
