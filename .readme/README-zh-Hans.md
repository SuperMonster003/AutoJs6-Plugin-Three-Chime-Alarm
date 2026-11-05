<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>闹钟, 计时器与秒表, 支持日历重复规则, 节假日跳过与响铃时联动脚本</p>

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

3-Chime Alarm 是 AutoJs6 的闹钟插件, 也是不装 AutoJs6 也能单独使用的完整时钟应用: 闹钟, 多个并行的倒计时器与秒表. 闹钟按 RFC 5545 日历规则重复 (例如每月最后一个周五, 或从某天起每隔 3 天), 可以跳过法定节假日, 响铃时可以启动脚本或通知正在运行的脚本.

插件使用 Android 精确闹钟自行调度并响铃, 因此 AutoJs6 没有运行时闹钟也会准时响. AutoJs6 脚本通过全局模块 `alarms` 查询, 新建, 修改与监听插件中的闹钟; 子命名空间 `alarms.system` 直接调用系统时钟应用, 未安装插件时也能使用. 插件也可以接收其他应用发出的设置闹钟请求.

> 当前为开发预览版: 插件可以被 AutoJs6 插件中心识别, 但闹钟, 计时器, 秒表, 插件界面与 `alarms` 脚本模块尚未交付, 图标也是候选图案, 之后可能更换. 下文描述的是各版本的设计目标, 进度见开发路线图.

******

### 功能亮点

******

- 闹钟: 单次或重复闹钟. 重复规则支持工作日, 每月第几个星期几, 月末, 间隔天数, 截止日期与次数, 也可以只跳过下一次响铃.
- 响铃: 系统铃声或本地音频, 朗读文字, 独立音量与渐强, 自定义振动, 锁屏时显示全屏响铃界面, 支持贪睡.
- 脚本联动: 闹钟可以登记一个脚本文件, 响铃时由 AutoJs6 启动, 脚本不必常驻; 正在运行的脚本也可以实时监听响铃, 贪睡与关闭事件.
- 计时器与秒表 (1.1.0): 多个计时器并行, 可以暂停, 继续与加时, 设备重启后恢复; 秒表支持计次.
- 节假日 (1.2.0): 内置多地区开源节假日数据, 法定节假日跳过响铃, 调休补班日照常响铃; 也可以用 `beforeRing` 脚本钩子决定是否响铃.
- 关闭挑战 (1.3.0): 算术题, 摇一摇, 照打一句话或脚本条件, 都带安全阀, 确保闹钟总能被关闭.

******

### 使用方法

******

1. 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases) 下载 APK 并安装. 安装包不区分 ABI, 适用于全部设备.
2. 打开插件, 按提示授予通知, 精确闹钟与全屏提醒权限, 之后就可以像普通时钟应用一样使用.
3. 要在脚本中使用 `alarms` 模块, 请将 AutoJs6 升级到内部版本号 6640 (6.8.1) 或更高版本, 并在插件中心启用本插件.

> 插件支持 Android 7.0 (API 24) 及以上的设备. APK 不含原生库, 因此只有一个安装包, 也不涉及 16 KB 内存页大小的校验.

******

### 脚本 API

******

脚本通过全局模块 `alarms` (也可写作 `$alarms`) 使用这些能力 (1.0.0 设计草案, 以发布版本的文档为准):

```javascript
let a = alarms.add({ time: "07:30", repeat: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", label: "Wake up" });
a.bind("./after-wake.js", { events: ["dismiss"] });

alarms.add({ time: "18:00", repeat: "FREQ=MONTHLY;BYDAY=-1FR", label: "Monthly report" });
alarms.add({ time: "06:10", date: "2026-11-04", repeat: { freq: "daily", interval: 3 }, tag: "shift" });

alarms.on("ring", (e) => toast(e.label));

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

- `alarms.add(options)` 新建闹钟并返回闹钟对象; `alarms.list`, `get`, `update`, `remove`, `setEnabled` 与 `skipNext` 管理插件中的全部闹钟, 包括在插件界面建的闹钟.
- `repeat` 接受 RRULE 字符串 (例如 `FREQ=MONTHLY;BYDAY=-1FR`) 或结构化对象; `alarms.preview(options)` 列出接下来的响铃时间, 便于核对规则.
- `alarm.bind(script, { events })` 登记响铃时由 AutoJs6 启动的脚本, 事件数据可通过 `alarms.currentEvent()` 读取; `alarms.on('ring', listener)` 在脚本运行期间实时监听.
- `alarms.system` 直接调用系统时钟应用 (`setAlarm`, `setTimer`, `showAlarms`, `next` 等), 未安装插件时也能使用.

******

### 常见问题

******

#### 不装 AutoJs6 能用吗?

能. 插件自带完整的时钟界面, 可以单独当作闹钟, 计时器与秒表使用. 只有在脚本需要管理闹钟或联动脚本时才用到 AutoJs6.

#### AutoJs6 没有运行时闹钟还会响吗?

会. 闹钟由插件通过 Android 精确闹钟调度并响铃, 不依赖 AutoJs6 进程. 登记了脚本时, 插件在响铃时唤醒 AutoJs6 启动该脚本.

#### 为什么需要精确闹钟与全屏提醒权限?

精确闹钟保证准时响铃 (闹钟类应用在安装时即可获得), 全屏提醒用于在锁屏上显示响铃界面, 通知用于显示正在响铃的闹钟. 如果系统撤销了其中某项权限, 插件的权限页会说明如何重新开启.

******

### 权限与安全

******

插件在设计上尽量收窄权限面:

- 受保护的入口: 能力服务与激活入口受 AutoJs6 插件权限 (`org.autojs.permission.PLUGIN`) 保护, 其他应用无法直接调用; 接收系统时钟请求的入口要求调用方持有 `com.android.alarm.permission.SET_ALARM`.
- 网络: 1.0.0 不申请网络权限, 闹钟, 计时器与秒表全部在本机完成.
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
minimum host build: 6640
native libraries: none (a single APK for every ABI; the 16 KB page size check does not apply)
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

_2026/11/05_

- `提示` 开发预览: 插件可以被 AutoJs6 插件中心识别, 闹钟, 计时器, 秒表, 插件界面与 alarms 脚本模块尚未交付
- `提示` 需要 AutoJs6 6.8.1 (6640) 或更高版本 (准确的最低版本号待宿主侧改动落地后回填)
- `提示` 当前图标为候选图案, 之后可能更换
- `新增` 插件骨架: Wake 激活协议, 插件中心身份服务与闹钟能力服务
- `新增` 安装包不含原生库, 只有一个适用于全部设备的 APK
- `新增` 10 种语言的 README, 插件中心说明与更新日志, 由 JSON 源文件统一生成
- `依赖` 附加 AndroidX Core KTX 版本 1.15.0
- `依赖` 附加 lib-recur 版本 0.17.1 (计算闹钟的重复规则)

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
