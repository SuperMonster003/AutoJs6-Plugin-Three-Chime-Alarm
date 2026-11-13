<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>闹钟, 计时器与秒表, 支持日历重复规则与节假日, 朗读响铃与响铃时联动脚本</p>

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

3-Chime Alarm 是 AutoJs6 的闹钟插件, 也是不装 AutoJs6 也能单独使用的闹钟应用. 闹钟按 RFC 5545 日历规则重复 (例如每月最后一个星期五, 或从某天起每隔 3 天), 响铃时可以朗读文字, 也可以启动脚本或通知正在运行的脚本. 1.1.0 起还提供多个同时运行的计时器与一个秒表. 1.2.0 起闹钟可以在节假日不响, 只在节假日响, 或在每个工作日 (含调休补班日) 响, 脚本也可以在响铃前决定这一次响不响. 关闭挑战将在后续版本加入.

插件使用 Android 精确闹钟自行调度并响铃, 因此 AutoJs6 没有运行时闹钟也会准时响. AutoJs6 脚本通过全局模块 `alarms` 查询, 新建, 修改与监听插件中的闹钟, 通过 `alarms.timers` 与 `alarms.stopwatch` 使用计时器与秒表; 子命名空间 `alarms.system` 直接调用系统时钟应用, 未安装插件时也能使用. 插件也可以接收其他应用发出的设置闹钟与计时器请求.

> 1.2.0 提供闹钟 (含节假日规则与响铃前由脚本决定), 计时器与秒表, 插件界面, 插件工具与 `alarms` 脚本模块. 在插件中心启用本插件, 使用插件工具与 `alarms` 模块需要 AutoJs6 6.8.1 (内部版本号 6641) 或更高版本; 在更早的 AutoJs6 上 (包括 6.8.0), 插件只能作为独立的闹钟应用使用. 关闭挑战的进度见开发路线图.

******

### 功能亮点

******

- 闹钟: 单次或重复闹钟. 重复规则支持工作日, 每月第几个星期几, 月末, 间隔天数, 截止日期与次数, 也可以排除指定日期或只跳过下一次响铃.
- 响铃: 系统铃声或导入的本地音频, 朗读文字, 独立音量与渐强, 自定义振动, 贪睡与自动停止, 锁屏时显示全屏响铃界面; 多个闹钟同时到期时依次响铃.
- 准时可靠: 设备重启, 修改时间或时区, 插件升级后自动重新排程, 开机后未解锁也会响铃; 关机错过的闹钟在 5 分钟内补响, 超过则留下一条通知.
- 脚本联动: 闹钟与计时器可以登记脚本文件, 响铃, 贪睡, 关闭等事件发生时由 AutoJs6 启动, 脚本不必常驻; 正在运行的脚本也可以实时监听这些事件.
- 插件工具: 经 MCP Server 插件或 `plugins.tools.call` 新建, 查询, 修改与删除闹钟, 处理正在响的闹钟, 开始, 查看与控制计时器, 查询节假日 (9 个 `alarm_*` 工具, 3 个 `timer_*` 工具与 `holiday_check`).
- 系统时钟对接: 可以处理其他应用发出的设置, 显示, 关闭与贪睡闹钟请求, 以及设置, 显示与关闭计时器请求.
- 界面: 语言, 夜间模式与主题色默认跟随 AutoJs6, 也可以单独设置; 可以切换启动器图标; 设置页可以设定铃声, 振动, 贪睡与自动停止的默认值.
- 计时器与秒表: 最多 20 个计时器同时运行, 可以暂停, 继续, 加时与重置; 运行中的计时器在通知栏显示倒计时, 可以直接暂停, 加 1 分钟或取消; 重启后继续计时, 到点与闹钟一样响铃. 秒表支持计次 (最多 100 圈), 标出最快与最慢的一圈. 底部导航在闹钟, 计时器与秒表之间切换.
- 节假日: 闹钟可以选 "节假日不响", "只在节假日响" 或 "只在工作日响" (每个工作日都响, 包括调休补班日, 周末与节假日不响). 内置 250 个国家和地区 (含 600 多个下级地区) 2020 - 2035 年的数据, 由开源项目 python-holidays 生成; 每周自动检查更新, 下载的数据经签名校验. 尚未公布的年份按规则推算并标为 "推算", 某年没有数据时只按重复规则响.
- 响铃前由脚本决定: 登记的脚本在响铃前 (默认 60 秒, 可设 15 秒至 5 分钟) 由 AutoJs6 启动, 可以跳过这一次, 或照常响铃并临时改用别的标签, 铃声, 朗读文字或振动; 脚本没有及时决定, 出错或 AutoJs6 不可用时照常响铃.
- 关闭挑战 (1.3.0 规划): 算术题, 摇一摇, 照打一句话或脚本条件, 都带安全阀, 确保闹钟总能被关闭.

******

### 截图

******

截图来自 Sony XQ-DQ72, 上排为浅色主题, 下排为深色主题.

| 闹钟列表 | 编辑闹钟 | 响铃 | 设置 |
| :---: | :---: | :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-light.png?raw=true" alt="list-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-light.png?raw=true" alt="edit-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-light.png?raw=true" alt="ring-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-light.png?raw=true" alt="settings-light" width="200" /> |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-dark.png?raw=true" alt="list-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-dark.png?raw=true" alt="edit-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-dark.png?raw=true" alt="ring-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-dark.png?raw=true" alt="settings-dark" width="200" /> |

| 计时器 | 秒表 |
| :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/timers-light.png?raw=true" alt="timers-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/stopwatch-light.png?raw=true" alt="stopwatch-light" width="200" /> |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/timers-dark.png?raw=true" alt="timers-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/stopwatch-dark.png?raw=true" alt="stopwatch-dark" width="200" /> |

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

let w = alarms.add({ time: "07:30", holidays: { region: "CN", mode: "workdays" }, label: "Work" });
w.bind("./weather-check.js", { events: ["beforeRing"], leadSeconds: 120 });

alarms.on("ring", (e) => toast(e.label));

alarms.timers.start("25m", { label: "Tea" });

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

- `alarms.add(options)` 新建闹钟并返回闹钟快照; `alarms.list`, `get`, `update`, `remove`, `setEnabled`, `skipNext` 与 `cancelSkip` 管理插件中的全部闹钟, 包括在插件界面建的闹钟.
- `repeat` 接受 RRULE 字符串 (例如 `FREQ=MONTHLY;BYDAY=-1FR`), 星期编号数组或结构化对象; `alarms.preview(options)` 列出接下来的响铃时间, 便于核对规则.
- `alarms.bind(id, script, options)` (或闹钟快照的 `bind`) 登记脚本, 事件发生时由 AutoJs6 启动, 事件数据可通过 `alarms.currentEvent()` 读取; `alarms.on('ring', listener)` 在脚本运行期间实时监听.
- `alarms.ringing`, `dismiss` 与 `snooze` 处理正在响的闹钟; `alarms.ringtones.list()` 列出可用的铃声.
- `alarms.timers.start(duration, options)` 开始计时器 (时长为毫秒数或 `"25m"`, `"1h 30m"` 这样的字符串, 1 秒至 100 小时), 返回带 `pause`, `resume`, `addTime`, `reset`, `cancel` 与 `bind` 方法的计时器快照; `alarms.stopwatch` 提供秒表的开始, 暂停, 继续, 计圈与重置. 两者都需要插件 1.1.0 或更高版本.
- `alarms.holidays` 查询节假日数据 (`regions`, `isHoliday`, `isWorkday`, `list`, `info`, `refresh`); 闹钟的 `holidays` 选项 (例如 `{ region: 'CN', mode: 'workdays' }`) 设定节假日规则, `alarms.capabilities()` 的 `warnings` 提示缺少数据或推算年份. 需要插件 1.2.0 或更高版本.
- `alarms.bind(id, script, { events: ['beforeRing'], leadSeconds: 120 })` 登记在响铃前启动的脚本, 脚本中 `alarms.currentEvent()` 的 `skip(reason)` 跳过这一次, `ring(overrides)` 照常响铃并临时改用别的标签, 铃声, 朗读或振动; `alarms.on('beforeRing', listener)` 实时监听, 事件对象同样有这两个方法. 需要插件 1.2.0 或更高版本.
- `alarms.system` 直接调用系统时钟应用 (`setAlarm`, `setTimer`, `showAlarms`, `dismissAlarm`, `next` 等), 不需要安装插件.

******

### 系统时钟对接

******

插件可以作为系统的闹钟应用, 处理其他应用 (例如日历, 语音助手与自动化工具) 发出的标准时钟请求. 发出请求的应用需要持有 `com.android.alarm.permission.SET_ALARM` 权限:

- 设置闹钟 (`SET_ALARM`): 读取时间, 标签, 星期, 振动与铃声; 请求要求跳过界面时直接保存, 否则打开预填的编辑页.
- 显示闹钟 (`SHOW_ALARMS`): 打开闹钟列表.
- 关闭闹钟 (`DISMISS_ALARM`): 结束正在响或贪睡中的闹钟; 没有正在响的闹钟时, 24 小时内要响的闹钟改为跳过这一次; 匹配到多个闹钟时由用户选择.
- 贪睡 (`SNOOZE_ALARM`): 让正在响的闹钟贪睡.
- 设置计时器 (`SET_TIMER`): 读取时长 (1 秒至 100 小时) 与标签并开始计时器; 已有时长与标签都相同且已重置的计时器时重新开始它; 请求没有要求跳过界面时同时打开计时器页, 没有时长时打开新建计时器对话框.
- 显示计时器 (`SHOW_TIMERS`): 打开计时器页.
- 关闭计时器 (`DISMISS_TIMER`): 结束正在响的计时器, 不影响仍在计时的计时器.
- 方向相反的 `alarms.system`: 脚本用它把请求发给系统默认的时钟应用 (本插件或其他时钟应用), 不需要安装插件; 各品牌时钟应用的差异见 AutoJs6 文档.

******

### 兼容性

******

插件支持 Android 7.0 (API 24) 及以上. 在插件中心启用本插件, 使用插件工具与 `alarms` 模块需要 AutoJs6 6.8.1 (内部版本号 6641) 或更高版本. 在更早的 AutoJs6 上 (例如内部版本号为 6640 的 6.8.0), 插件中心提示需要更高版本的 AutoJs6 且不能启用本插件, 插件工具与 `alarms` 模块不可用, 但插件仍可作为独立的闹钟应用使用, 也照常处理系统时钟请求. `alarms` 模块只支持 Rhino 引擎, 其中的 `alarms.system` 不需要安装插件.

1.0.0 发布前在以下设备上完成验证 (数据见开发路线图附录 D); 1.1.0 的计时器与秒表另在 Sony XQ-AT72 与 Sony G8441 上验证, 包括重启后继续计时 (见开发路线图 P7); 1.2.0 的节假日与响铃前由脚本决定在 Sony XQ-AT72 上与 AutoJs6 联调验证 (见开发路线图 P8):

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

#### 设备重启后, 计时器和秒表会怎样?

计时器按关机前的到点时刻继续计时, 关机期间也计入. 关机期间已到点的计时器, 开机时过点不到 5 分钟的会补响, 超过 5 分钟的留下一条错过的通知. 开机后系统校准时间时, 到点时刻可能随之移动几秒. 秒表重启后转为暂停, 读数算到本次开机为止, 因此会多计关机的那段时间 (重启通常不到 1 分钟).

#### 节假日数据准吗? 调休补班日会响吗?

节假日与补班日是否准确完全取决于上游数据 (开源项目 python-holidays). 选 "只在工作日响" 时补班日会响; 选 "节假日不响" 时只按重复规则判断 (例如周一至周五的闹钟在周六的补班日不响). 尚未公布的年份按规则推算, 设置页与编辑页会标出 "推算", 公布后随数据更新修正; 数据每周自动检查一次, 也可以在设置页手动检查.

#### `beforeRing` 脚本来不及决定会怎样?

照常响铃. 决定的截止时刻默认为响铃前 5 秒; 到时仍没有决定, 脚本出错, AutoJs6 不可用 (例如被强行停止) 或重启后还没解锁时, 闹钟都照常响铃. 已被节假日规则跳过的那一次不会启动 `beforeRing` 脚本.

******

### 权限与安全

******

插件申请的权限与用途:

- 精确闹钟 (`USE_EXACT_ALARM`, Android 12 与 12L 为 `SCHEDULE_EXACT_ALARM`): 按时响铃. Android 13 起安装即授予, Android 12 与 12L 需要在系统设置中允许 "闹钟和提醒".
- 通知 (`POST_NOTIFICATIONS`): 显示正在响的闹钟与错过的闹钟, 以及运行中的计时器与秒表. 缺少时闹钟与计时器照常发声, 但没有通知与响铃界面.
- 全屏提醒 (`USE_FULL_SCREEN_INTENT`): 在锁屏上显示响铃界面. Android 14 起可能未授予, 缺少时以悬浮通知响铃.
- 悬浮窗 (`SYSTEM_ALERT_WINDOW`, 可选): 系统不显示全屏提醒时 (例如 ColorOS 的横幅通知关闭) 用来打开响铃界面, 只在权限页由用户按需允许.
- 开机启动 (`RECEIVE_BOOT_COMPLETED`): 重启后重新排程, 首次解锁前也能响铃.
- 前台服务 (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`): 响铃期间保持发声; 设置页的 "保持后台运行" 开启时也用它常驻 (默认关闭).
- 振动, 唤醒锁与音频设置 (`VIBRATE`, `WAKE_LOCK`, `MODIFY_AUDIO_SETTINGS`): 振动, 响铃时保持设备唤醒, 以及独立的闹钟音量在响铃后还原.
- 对正在运行的应用重新排序 (`REORDER_TASKS`, 仅 Android 7.0 - 8.1): 其他应用或脚本几乎同时发来多个闹钟请求时, 把系统没有启动的请求调到前台处理, 使每个请求都得到处理. 安装即授予.
- 网络 (`INTERNET`, `ACCESS_NETWORK_STATE`): 只用于更新检查与节假日数据刷新. 更新检查在打开闹钟列表时按设定的频率进行, 节假日数据每周检查一次, 两者在计量网络上默认都不检查; 排程, 响铃与 AutoJs6 联动从不联网.
- AutoJs6 插件权限 (`org.autojs.permission.PLUGIN`): 与 AutoJs6 通信.

插件在设计上尽量收窄权限面:

- 受保护的入口: 能力服务与激活入口受 AutoJs6 插件权限 (`org.autojs.permission.PLUGIN`) 保护, 其他应用无法直接调用; 接收系统时钟请求的入口要求调用方持有 `com.android.alarm.permission.SET_ALARM`.
- 网络: 只用于检查插件更新与节假日数据 (连接 GitHub, 两者的自动检查都可在设置页关闭), 下载的节假日数据经签名校验, 不上传闹钟或任何个人数据.
- 存储: 闹钟保存在插件私有目录, 不参与云备份; 本地铃声通过系统文件选择器导入, 不申请存储权限.
- 脚本权限: 脚本可以通过 `alarms` 模块读写插件中的全部闹钟与计时器, 包括在插件界面建的闹钟与计时器; 登记的脚本保存在 AutoJs6 中, 插件界面只标记闹钟有脚本绑定并可以解除.

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

#### v1.2.0

_2026/11/13_

- `提示` 脚本中的 alarms.holidays, 闹钟的 holidays 选项与 beforeRing 事件需要 AutoJs6 6.8.1 (内部版本号 6641) 或更高版本, 并需要本插件 1.2.0 或更高版本; 插件仍为 1.1.x 或更早时调用会报错并提示升级插件
- `提示` 节假日与调休补班日是否准确完全取决于上游数据 (开源项目 python-holidays); 尚未公布的年份按规则推算并标为 "推算", 公布后随数据更新修正
- `新增` 节假日: 闹钟新增 "节假日不响", "只在节假日响" 与 "只在工作日响" 三种选项 (只在工作日响即每个工作日都响, 包括调休补班日, 周末与节假日不响); 内置 250 个国家和地区 (含 600 多个下级地区) 2020 - 2035 年的节假日数据, 中国大陆与台湾含调休补班日
- `新增` 设置页可以选择默认节假日地区 (首次按系统地区推断), 每个闹钟也可以单独指定地区; 闹钟列表显示下一次响铃跳过了几个节假日; 推算的年份标为 "推算", 提示可能有调休或补班
- `新增` 节假日数据每周自动检查更新 (可在设置页关闭, 默认不用计量网络下载), 也可以在设置页手动检查; 下载的数据经签名校验, 不通过时继续用原有数据; 某年没有数据时闹钟只按重复规则响, 不会因此不响
- `新增` 响铃前由脚本决定 (beforeRing): 登记的脚本在响铃前 (默认 60 秒, 可设 15 秒至 5 分钟) 被唤起, 可以跳过这一次, 或照常响铃并临时改用别的标签, 铃声, 朗读文字或振动; 脚本没有及时决定, 出错或 AutoJs6 不可用时照常响铃
- `新增` AutoJs6 脚本模块新增 alarms.holidays (查询地区, 判断节假日与工作日, 列出全年节假日, 查看与刷新数据), 闹钟的 holidays 选项与 beforeRing 事件的 skip / ring, alarms.capabilities() 新增 warnings 提示缺少数据或推算年份 (需要 AutoJs6 6.8.1; 参阅 [AutoJs6 文档](https://docs.autojs6.com/#/alarms))
- `新增` 插件工具新增 holiday_check, 可经 MCP Server 插件或 plugins.tools.call 查询某天在某地区是否节假日或工作日及节假日名称; alarm_add 与 alarm_update 支持 holidays 选项

#### v1.1.0

_2026/11/12_

- `提示` 脚本中的 alarms.timers 与 alarms.stopwatch 需要 AutoJs6 6.8.1 (内部版本号 6641) 或更高版本, 并需要本插件 1.1.0 或更高版本; 插件仍为 1.0.x 时调用会报错并提示升级插件
- `新增` 计时器: 最多 20 个计时器同时运行, 时长可以选预设 (1 分钟至 1 小时) 或自定义 (1 秒至 100 小时), 可以加标签, 可以暂停, 继续, 加 1 分钟与重置; 到点后与闹钟一样响铃
- `新增` 运行中的计时器在通知栏显示倒计时, 可以直接在通知上暂停, 加 1 分钟或取消; 设备重启后计时器继续计时, 关机期间到点的计时器在开机后补响 (过点超过 5 分钟的只留下一条错过的通知)
- `新增` 秒表: 开始, 暂停, 继续与重置, 最多记录 100 圈, 标出最快与最慢的一圈; 运行时在通知栏显示, 可以在设置页的 "计时器与秒表" 分组中关闭
- `新增` 底部导航在闹钟, 计时器与秒表三个页面之间切换
- `新增` AutoJs6 脚本模块新增 alarms.timers 与 alarms.stopwatch (需要 AutoJs6 6.8.1): 开始, 查询, 暂停, 继续, 加减时间, 重置与取消计时器, 为计时器登记在响铃等事件发生时启动的脚本, 控制秒表并记录计圈 (参阅 [AutoJs6 文档](https://docs.autojs6.com/#/alarms))
- `新增` 插件工具新增 timer_start, timer_list 与 timer_control, 可经 MCP Server 插件或 plugins.tools.call 开始, 查看, 暂停, 继续, 加时, 重置与取消计时器
- `新增` 系统时钟对接新增计时器请求: 其他应用可以让插件开始计时器, 打开计时器页或关闭正在响的计时器

#### v1.0.1

_2026/11/08_

- `修复` 其他应用或 AutoJs6 脚本很快地连续发来两个同类的闹钟请求 (例如连续两次新建闹钟, 或连续调用 alarms.system.setAlarm 并指定本插件) 时, 如果插件当时没有打开的界面, 第二个请求会被忽略, 闹钟没有建成, 也没有任何提示; 现在每个请求都会处理; 在 Android 7.0 - 8.1 上, 为此需要 "对正在运行的应用重新排序" 权限 (安装时自动授予)

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

项目代码使用 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE). 第三方组件与数据的许可见第三方声明. 节假日数据由开源项目 python-holidays (MIT 许可) 生成, 许可全文与贡献者名单随数据一同发布.

- [第三方声明 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/THIRD_PARTY_NOTICES.md)

******

### 相关链接

******

- AutoJs6 项目: https://github.com/SuperMonster003/AutoJs6
