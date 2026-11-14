<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>鬧鐘, 計時器與秒錶, 支援日曆重複規則, 節假日與關閉挑戰, 朗讀響鈴與響鈴時聯動腳本</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 語言 (Languages)

******

目前 README.md 支援以下語言:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hans.md)
- 繁體中文 (香港) [zh-Hant-HK] # 目前
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ar.md)

******

### 簡介

******

3-Chime Alarm 是 AutoJs6 的鬧鐘插件, 也是不安裝 AutoJs6 也能獨立使用的鬧鐘應用程式. 鬧鐘按 RFC 5545 日曆規則重複 (例如每月最後一個星期五, 或從某天起每隔 3 天), 響鈴時可以朗讀文字, 也可以啟動腳本或通知正在運行的腳本. 1.1.0 起亦提供多個同時運行的計時器與一個秒錶. 1.2.0 起鬧鐘可以在節假日不響, 只在節假日響, 或在每個工作日 (包括調休補班日) 響, 腳本也可以在響鈴前決定這一次響不響. 1.3.0 起鬧鐘可以要求先答對算術題, 搖動手機, 輸入一句文字或由腳本確認才能關閉, 同時始終保留自動停止與緊急關閉兩條出路.

插件使用 Android 精確鬧鐘自行排程並響鈴, 因此 AutoJs6 沒有執行時鬧鐘亦會準時響. AutoJs6 腳本透過全域模組 `alarms` 查詢, 新增, 修改與監聽插件中的鬧鐘, 透過 `alarms.timers` 與 `alarms.stopwatch` 使用計時器與秒錶; 子命名空間 `alarms.system` 直接調用系統時鐘應用程式, 未安裝插件時亦可使用. 插件亦可以接收其他應用程式發出的設定鬧鐘與計時器請求.

> 1.3.0 提供鬧鐘 (包括節假日規則, 響鈴前由腳本決定與關閉挑戰), 計時器與秒錶, 插件介面, 插件工具與 `alarms` 腳本模組. 在插件中心啟用本插件, 使用插件工具與 `alarms` 模組需要 AutoJs6 6.8.1 (內部版本號 6641) 或更高版本; 在較早的 AutoJs6 上 (包括 6.8.0), 插件只能作為獨立的鬧鐘應用程式使用.

******

### 功能亮點

******

- 鬧鐘: 單次或重複鬧鐘. 重複規則支援工作日, 每月第幾個星期幾, 月底, 間隔日數, 截止日期與次數, 也可以排除指定日期或只略過下一次響鈴.
- 響鈴: 系統鈴聲或匯入的本機音訊, 朗讀文字, 獨立音量與漸強, 自訂震動, 貪睡與自動停止, 上鎖時顯示全螢幕響鈴介面; 多個鬧鐘同時到期時依次響鈴.
- 準時可靠: 裝置重新開機, 修改時間或時區, 插件升級後自動重新排程, 開機後未解鎖也會響鈴; 關機錯過的鬧鐘在 5 分鐘內補響, 超過則留下一則通知.
- 腳本聯動: 鬧鐘與計時器可以登記腳本檔案, 響鈴, 貪睡, 關閉等事件發生時由 AutoJs6 啟動, 腳本不必常駐; 正在運行的腳本也可以即時監聽這些事件.
- 插件工具: 經 MCP Server 插件或 `plugins.tools.call` 新增, 查詢, 修改與刪除鬧鐘 (可以設定關閉挑戰, 腳本條件除外), 處理正在響的鬧鐘, 開始, 查看與控制計時器, 查詢節假日 (9 個 `alarm_*` 工具, 3 個 `timer_*` 工具與 `holiday_check`).
- 系統時鐘對接: 可以處理其他應用程式發出的設定, 顯示, 關閉與貪睡鬧鐘要求, 以及設定, 顯示與關閉計時器要求.
- 介面: 語言, 夜間模式與主題色預設跟隨 AutoJs6, 也可以單獨設定; 可以切換啟動器圖標; 設定頁可以設定鈴聲, 震動, 貪睡與自動停止的預設值, 以及是否允許緊急關閉.
- 計時器與秒錶: 最多 20 個計時器同時運行, 可以暫停, 繼續, 加時與重置; 運行中的計時器在通知欄顯示倒數, 可以直接暫停, 加 1 分鐘或取消; 重新開機後繼續計時, 到時與鬧鐘一樣響鈴. 秒錶支援計圈 (最多 100 圈), 標出最快與最慢的一圈. 底部導覽列在鬧鐘, 計時器與秒錶之間切換.
- 節假日: 鬧鐘可以選 "節假日不響", "只在節假日響" 或 "只在工作日響" (每個工作日都響, 包括補班日, 週末與節假日不響). 內置 250 個國家和地區 (包括 600 多個下級地區) 2020 - 2035 年的數據, 由開源項目 python-holidays 生成; 每週自動檢查更新, 下載的數據經簽名驗證. 尚未公佈的年份按規則推算並標為 "推算", 某年沒有數據時只按重複規則響.
- 響鈴前由腳本決定: 登記的腳本在響鈴前 (預設 60 秒, 可設 15 秒至 5 分鐘) 由 AutoJs6 啟動, 可以略過這一次, 或照常響鈴並臨時改用其他標籤, 鈴聲, 朗讀文字或震動; 腳本沒有及時決定, 出錯或 AutoJs6 無法使用時照常響鈴.
- 關閉挑戰: 鬧鐘可以要求先答對算術題 (簡單, 中等, 困難三檔, 預設 3 題), 搖動手機 (預設 30 次) 或輸入一句文字 (自訂, 或隨機選一句內置句子) 才能關閉, 並可以設定挑戰期間是否允許貪睡; 挑戰生效時, 通知的 "關閉" 按鈕與其他應用程式的關閉請求改為打開響鈴畫面的挑戰. 每種挑戰都保留兩條出路: 自動停止 (未單獨設定時取設定頁預設值與 30 分鐘中較長者, 響鈴畫面顯示剩餘時間) 與緊急關閉 (響鈴畫面在前台時, 包括在鎖屏上, 4 秒內按 5 次音量鍵; 可在設定頁關閉). AutoJs6 腳本還可以設定腳本條件: 鬧鐘一直響到腳本關閉它, 腳本無法執行時 (例如開機後尚未解鎖, 或腳本已結束) 立即退回到設定的挑戰.

******

### 截圖

******

截圖來自 Sony XQ-DQ72, 上排為淺色主題, 下排為深色主題.

| 鬧鐘列表 | 編輯鬧鐘 | 響鈴 | 設定 |
| :---: | :---: | :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-light.png?raw=true" alt="list-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-light.png?raw=true" alt="edit-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-light.png?raw=true" alt="ring-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-light.png?raw=true" alt="settings-light" width="200" /> |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-dark.png?raw=true" alt="list-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-dark.png?raw=true" alt="edit-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-dark.png?raw=true" alt="ring-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-dark.png?raw=true" alt="settings-dark" width="200" /> |

| 計時器 | 秒錶 |
| :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/timers-light.png?raw=true" alt="timers-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/stopwatch-light.png?raw=true" alt="stopwatch-light" width="200" /> |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/timers-dark.png?raw=true" alt="timers-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/stopwatch-dark.png?raw=true" alt="stopwatch-dark" width="200" /> |

******

### 使用方法

******

1. 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases) 下載 APK 並安裝. 安裝包不區分 ABI, 適用於所有裝置.
2. 開啟插件, 在權限頁逐項處理: 通知, 精確鬧鐘 (Android 12 與 12L 需要手動允許), 全螢幕提醒 (Android 14 起可能未授予), 以及電池優化, 後台限制與廠商自啟動. 之後就可以像一般時鐘應用程式一樣使用.
3. 如要在腳本中使用 `alarms` 模組或使用插件工具, 請將 AutoJs6 升級到 6.8.1 (內部版本號 6641) 或更高版本, 並在插件中心啟用本插件 (安裝精靈的 "工具" 分類).

> 插件支援 Android 7.0 (API 24) 或以上的裝置. 只有一個適用於全部裝置的安裝包; 其中唯一的原生庫來自介面框架 Compose, 已按 16 KB 記憶體分頁對齊, 在 16 KB 記憶體分頁的裝置上亦可運行.

******

### 腳本 API

******

腳本透過全局模組 `alarms` (也可寫作 `$alarms`, 僅 Rhino) 使用這些功能, 完整說明見 [AutoJs6 文件](https://docs.autojs6.com/#/alarms):

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

- `alarms.add(options)` 新增鬧鐘並傳回鬧鐘快照; `alarms.list`, `get`, `update`, `remove`, `setEnabled`, `skipNext` 與 `cancelSkip` 管理插件中的所有鬧鐘, 包括在插件介面建立的鬧鐘.
- `repeat` 接受 RRULE 字串 (例如 `FREQ=MONTHLY;BYDAY=-1FR`), 星期編號陣列或結構化物件; `alarms.preview(options)` 列出接下來的響鈴時間, 方便核對規則.
- `alarms.bind(id, script, options)` (或鬧鐘快照的 `bind`) 登記腳本, 事件發生時由 AutoJs6 啟動, 事件資料可透過 `alarms.currentEvent()` 讀取; `alarms.on('ring', listener)` 在腳本運行期間即時監聽.
- `alarms.ringing`, `dismiss` 與 `snooze` 處理正在響的鬧鐘; `alarms.ringtones.list()` 列出可用的鈴聲.
- `alarms.timers.start(duration, options)` 開始計時器 (時長為毫秒數或 `"25m"`, `"1h 30m"` 這樣的字串, 1 秒至 100 小時), 返回帶 `pause`, `resume`, `addTime`, `reset`, `cancel` 與 `bind` 方法的計時器快照; `alarms.stopwatch` 提供秒錶的開始, 暫停, 繼續, 計圈與重置. 兩者都需要插件 1.1.0 或更高版本.
- `alarms.holidays` 查詢節假日數據 (`regions`, `isHoliday`, `isWorkday`, `list`, `info`, `refresh`); 鬧鐘的 `holidays` 選項 (例如 `{ region: 'CN', mode: 'workdays' }`) 設定節假日規則, `alarms.capabilities()` 的 `warnings` 提示缺少數據或推算年份. 需要插件 1.2.0 或更高版本.
- `alarms.bind(id, script, { events: ['beforeRing'], leadSeconds: 120 })` 登記在響鈴前啟動的腳本, 腳本中 `alarms.currentEvent()` 的 `skip(reason)` 略過這一次, `ring(overrides)` 照常響鈴並臨時改用其他標籤, 鈴聲, 朗讀或震動; `alarms.on('beforeRing', listener)` 即時監聽, 事件物件同樣有這兩個方法. 需要插件 1.2.0 或更高版本.
- `dismiss` 選項設定關閉挑戰, 例如 `alarms.add({ time: '07:00', dismiss: { challenge: 'math', math: { difficulty: 3, count: 5 }, allowSnooze: false } })`; `challenge: 'script'` 時由腳本呼叫 `alarms.dismiss(id)` 關閉, `script.fallback` 指定腳本無法執行時改用的挑戰. `alarms.ringing()` 與 `ring`, `dismiss` 事件帶此刻生效的 `challenge`, 完成挑戰關閉時 `dismiss` 事件的 `by` 為 `'challenge'`. 需要插件 1.3.0 或更高版本.
- `alarms.system` 直接呼叫系統時鐘應用程式 (`setAlarm`, `setTimer`, `showAlarms`, `dismissAlarm`, `next` 等), 不需要安裝插件.

******

### 系統時鐘對接

******

插件可以作為系統的鬧鐘應用程式, 處理其他應用程式 (例如日曆, 語音助理與自動化工具) 發出的標準時鐘要求. 發出要求的應用程式需要持有 `com.android.alarm.permission.SET_ALARM` 權限:

- 設定鬧鐘 (`SET_ALARM`): 讀取時間, 標籤, 星期, 震動與鈴聲; 要求略過介面時直接儲存, 否則開啟預填的編輯頁.
- 顯示鬧鐘 (`SHOW_ALARMS`): 開啟鬧鐘列表.
- 關閉鬧鐘 (`DISMISS_ALARM`): 結束正在響或貪睡中的鬧鐘; 沒有正在響的鬧鐘時, 24 小時內要響的鬧鐘改為略過這一次; 符合多個鬧鐘時由用戶選擇.
- 貪睡 (`SNOOZE_ALARM`): 讓正在響的鬧鐘貪睡.
- 設定計時器 (`SET_TIMER`): 讀取時長 (1 秒至 100 小時) 與標籤並開始計時器; 已有時長與標籤都相同且已重置的計時器時重新開始它; 要求沒有略過介面時同時開啟計時器頁, 沒有時長時開啟新增計時器對話框.
- 顯示計時器 (`SHOW_TIMERS`): 開啟計時器頁.
- 關閉計時器 (`DISMISS_TIMER`): 結束正在響的計時器, 不影響仍在計時的計時器.
- 方向相反的 `alarms.system`: 腳本用它把要求發給系統預設的時鐘應用程式 (本插件或其他時鐘應用程式), 不需要安裝插件; 各品牌時鐘應用程式的差異見 AutoJs6 文件.

******

### 兼容性

******

插件支援 Android 7.0 (API 24) 或以上. 在插件中心啟用本插件, 使用插件工具與 `alarms` 模組需要 AutoJs6 6.8.1 (內部版本號 6641) 或更高版本. 在較早的 AutoJs6 上 (例如內部版本號為 6640 的 6.8.0), 插件中心提示需要更高版本的 AutoJs6 且不能啟用本插件, 插件工具與 `alarms` 模組不可用, 但插件仍可作為獨立的鬧鐘應用程式使用, 也照常處理系統時鐘要求. `alarms` 模組只支援 Rhino 引擎, 其中的 `alarms.system` 不需要安裝插件.

1.0.0 發佈前在以下裝置上完成驗證 (數據見開發路線圖附錄 D); 1.1.0 的計時器與秒錶另在 Sony XQ-AT72 與 Sony G8441 上驗證, 包括重新開機後繼續計時 (見開發路線圖 P7); 1.2.0 的節假日與響鈴前由腳本決定在 Sony XQ-AT72 上與 AutoJs6 聯合驗證 (見開發路線圖 P8); 1.3.0 的關閉挑戰在 Sony XQ-AT72 上與 AutoJs6 聯合驗證, 包括鎖屏上的緊急關閉與腳本條件的各種退回 (見開發路線圖 P9):

| 裝置 | Android | 結果 |
| --- | --- | --- |
| Sony XQ-AT72 | 12 (API 31) | 通過: 到點偏差中位數 44 ms; 上鎖響鈴, 重新開機後到點響鈴, 修改時間, 時區與夏令時間, 經 MCP Server 呼叫插件工具 |
| Sony XQ-DQ72 | 13 (API 33) | 通過: 到點偏差中位數 13 ms; 上鎖全螢幕響鈴, 程序被清理後到點約 1.1 秒顯示響鈴介面 |
| Redmi 22120RN86C (MIUI) | 13 (API 33) | 通過: 到點偏差中位數 40 ms; 開機後系統多次結束插件程序, 到點仍準時響鈴 |
| Xiaomi Pad 23046RP50C | 15 (API 35) | 通過: 到點偏差中位數 32 ms; 未授予全螢幕提醒時以浮動通知響鈴, 重新開機後未解鎖也準時響鈴 |
| OnePlus OPD2413 (ColorOS 15) | 15 (API 35) | 通過 (需開啟自動啟動): 到點偏差中位數 23 ms; 在 設定 > 應用程式 > 自動啟動 中開啟後, 程序被清理也準時響鈴 (見常見問題) |
| Sony G8441 | 9 (API 28) | 通過: 到點偏差中位數 41 ms; 上鎖響鈴, 重新開機後未解鎖也準時響鈴 |
| Android 模擬器 | 7.0 - 16 (API 24 - 36) | 通過: 最低版本完整流程, 首次解鎖前響鈴, 16 KB 記憶體分頁 |

******

### 常見問題

******

#### 不安裝 AutoJs6 能用嗎?

能. 插件自帶完整的鬧鐘介面, 可以獨立當作鬧鐘應用程式使用. 只有在腳本需要管理鬧鐘, 聯動腳本或使用插件工具時才用到 AutoJs6.

#### AutoJs6 沒有運行時鬧鐘還會響嗎?

會. 鬧鐘由插件透過 Android 精確鬧鐘排程並響鈴, 不依賴 AutoJs6 程序. 登記了腳本時, 插件在響鈴時喚醒 AutoJs6 啟動該腳本.

#### 為甚麼 AutoJs6 6.8.0 的插件中心不能啟用本插件?

插件中心啟用本插件, 插件工具與 `alarms` 模組需要 AutoJs6 6.8.1 (內部版本號 6641) 或更高版本, 而 6.8.0 的內部版本號為 6640. 升級 AutoJs6 後即可啟用; 在此之前插件可以作為獨立的鬧鐘應用程式使用.

#### 鬧鐘沒有準時響, 應該檢查甚麼?

先開啟插件的權限頁, 確認通知與精確鬧鐘已允許, 並按頁面說明關閉電池優化, 解除後台限制, 允許自啟動. 請勿打擾模式預設允許鬧鐘, 如果改過請勿打擾設定, 請確認鬧鐘仍被允許. 關機或系統延後導致錯過的鬧鐘, 5 分鐘內會補響, 超過則不響並留下一則通知.

#### OPPO, 一加, realme (ColorOS) 上鬧鐘不響怎麼辦?

ColorOS 有兩處互相獨立的開關, 都要為 3-Chime Alarm 開啟: 應用程式詳情內的 "允許自動啟動" / "關聯啟動" / "後台活動", 以及 設定 > 應用程式 > 自動啟動 列表. 只開前者時, 插件程序被清理 (例如在最近任務中掃走) 後, 系統會攔下到點的鬧鐘. 聯動腳本時 AutoJs6 也要在兩處開啟. 另外請不要在最近任務中掃走插件, 可以把它鎖定. 插件權限頁在 ColorOS 上有對應的引導行; 設定頁的 "保持後台運行" 會常駐一則靜默通知, 進一步減少插件被清理的機會, 耗電略有增加.

#### ColorOS 上鎖屏響鈴時只有鈴聲和通知, 沒有彈出響鈴介面?

ColorOS 新安裝應用程式的 "橫幅" 通知預設關閉, 系統因此不顯示全螢幕響鈴介面. 在插件的通知設定中開啟 "橫幅" 即可, 權限頁的 "橫幅通知" 一行可以直達該設定; 也可以在權限頁允許 "懸浮視窗" 作為後備.

#### 強制停止插件後鬧鐘不響了?

"強制停止" 會讓系統清除插件的所有鬧鐘排程, 停止期間到點不響, 這是 Android 的規則. 從啟動器開啟一次插件即可恢復排程; 恢復時仍在 5 分鐘內的鬧鐘會補響.

#### 小米 (MIUI, HyperOS) 需要特別設定嗎?

實測 Redmi (MIUI) 在開機後會多次結束插件程序, 到點時系統仍會啟動插件並準時響鈴. 仍建議按權限頁的說明設定自啟動與省電策略.

#### 裝置重新開機後還沒解鎖, 鬧鐘會響嗎? 聯動的腳本呢?

鬧鐘會響, 插件在首次解鎖之前也能運行. 聯動的腳本不會啟動, 因為 AutoJs6 在首次解鎖之前不能運行腳本, 解鎖後也不會補啟動; 關閉挑戰為腳本條件的鬧鐘因此會立即退回到設定的挑戰.

#### 裝置重新開機後, 計時器和秒錶會怎樣?

計時器按關機前的到時時刻繼續計時, 關機期間亦計算在內. 關機期間已到時的計時器, 開機時過時不足 5 分鐘的會補響, 超過 5 分鐘的留下一條錯過的通知. 開機後系統校準時間時, 到時時刻可能隨之移動幾秒. 秒錶重新開機後轉為暫停, 讀數計到本次開機為止, 因此會多計關機的那段時間 (重新開機通常不到 1 分鐘).

#### 節假日數據準確嗎? 調休補班日會響嗎?

節假日與補班日是否準確完全取決於上游數據 (開源項目 python-holidays). 選 "只在工作日響" 時補班日會響; 選 "節假日不響" 時只按重複規則判斷 (例如星期一至五的鬧鐘在星期六的補班日不響). 尚未公佈的年份按規則推算, 設定頁與編輯頁會標出 "推算", 公佈後隨數據更新修正; 數據每週自動檢查一次, 也可以在設定頁手動檢查.

#### `beforeRing` 腳本來不及決定會怎樣?

照常響鈴. 決定的截止時刻預設為響鈴前 5 秒; 到時仍沒有決定, 腳本出錯, AutoJs6 無法使用 (例如被強行停止) 或重新開機後尚未解鎖時, 鬧鐘都照常響鈴. 已被節假日規則略過的那一次不會啟動 `beforeRing` 腳本.

#### 設了關閉挑戰, 解不出題或搖不動時, 鬧鐘會一直響嗎?

不會. 每種挑戰都保留兩條出路: 未單獨設定自動停止時, 鬧鐘在設定頁的預設值與 30 分鐘中較長的時間過後自動停止, 響鈴畫面顯示剩餘時間; 響鈴畫面在前台時 (包括在鎖屏上), 4 秒內按 5 次音量鍵 (加或減都算) 即可緊急關閉. 挑戰期間音量鍵只用於計數, 不調節音量. 緊急關閉預設開啟, 可以在設定頁的 "關閉挑戰" 組關閉, 關閉後完成挑戰之前只有自動停止能結束響鈴.

******

### 權限與安全

******

插件申請的權限與用途:

- 精確鬧鐘 (`USE_EXACT_ALARM`, Android 12 與 12L 為 `SCHEDULE_EXACT_ALARM`): 準時響鈴. Android 13 起安裝即授予, Android 12 與 12L 需要在系統設定中允許 "鬧鐘和提醒".
- 通知 (`POST_NOTIFICATIONS`): 顯示正在響的鬧鐘與錯過的鬧鐘, 以及運行中的計時器與秒錶. 缺少時鬧鐘與計時器照常發聲, 但沒有通知與響鈴介面.
- 全螢幕提醒 (`USE_FULL_SCREEN_INTENT`): 在上鎖畫面顯示響鈴介面. Android 14 起可能未授予, 缺少時以浮動通知響鈴.
- 懸浮視窗 (`SYSTEM_ALERT_WINDOW`, 可選): 系統不顯示全螢幕提醒時 (例如 ColorOS 的橫幅通知關閉) 用來開啟響鈴介面, 只在權限頁由用戶按需允許.
- 開機啟動 (`RECEIVE_BOOT_COMPLETED`): 重新開機後重新排程, 首次解鎖前也能響鈴.
- 前景服務 (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`): 響鈴期間保持發聲; 設定頁的 "保持後台運行" 開啟時也用它常駐 (預設關閉).
- 震動, 喚醒鎖與音訊設定 (`VIBRATE`, `WAKE_LOCK`, `MODIFY_AUDIO_SETTINGS`): 震動, 響鈴時保持裝置喚醒, 以及獨立的鬧鐘音量在響鈴後還原.
- 重新排列正在執行的應用程式 (`REORDER_TASKS`, 僅 Android 7.0 - 8.1): 其他應用程式或腳本幾乎同時發出多個鬧鐘要求時, 把系統沒有啟動的要求調到前台處理, 讓每個要求都得到處理. 安裝時即授予.
- 網絡 (`INTERNET`, `ACCESS_NETWORK_STATE`): 只用於更新檢查與節假日數據更新. 更新檢查在開啟鬧鐘列表時按設定的頻率進行, 節假日數據每週檢查一次, 兩者在按用量收費的網絡上預設都不檢查; 排程, 響鈴與 AutoJs6 聯動從不連網.
- AutoJs6 插件權限 (`org.autojs.permission.PLUGIN`): 與 AutoJs6 通訊.

插件在設計上盡量收窄權限範圍:

- 受保護的入口: 能力服務與啟動入口受 AutoJs6 插件權限 (`org.autojs.permission.PLUGIN`) 保護, 其他應用程式無法直接調用; 接收系統時鐘請求的入口要求調用方持有 `com.android.alarm.permission.SET_ALARM`.
- 網絡: 只用於檢查插件更新與節假日數據 (連接 GitHub, 兩者的自動檢查都可在設定頁關閉), 下載的節假日數據經簽名驗證, 不上傳鬧鐘或任何個人資料.
- 儲存: 鬧鐘保存在插件私有目錄, 不參與雲端備份; 本機鈴聲透過系統檔案選擇器匯入, 不申請儲存權限.
- 腳本權限: 腳本可以透過 `alarms` 模組讀寫插件中的全部鬧鐘與計時器, 包括在插件介面建立的鬧鐘與計時器; 登記的腳本保存在 AutoJs6 中, 插件介面只標記鬧鐘有腳本綁定並可以解除.

******

### 外掛接口

******

以下資訊面向 AutoJs6 宿主與外掛開發者, 宿主透過這些標識發現外掛並完成能力協商:

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

### 開發路線圖

******

功能規劃, 設計決策與完成情況以可勾選清單維護在 ROADMAP.md 中. 未勾選條目表示規劃意向, 不代表目前版本能力.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.3.0

_2026/11/14_

- `提示` 在插件介面設定的關閉挑戰不需要 AutoJs6; 在腳本中設定關閉挑戰 (鬧鐘的 dismiss 選項) 與使用腳本條件需要 AutoJs6 6.8.1 (內部版本號 6641) 或更高版本, 並需要本插件 1.3.0 或更高版本; 插件仍為 1.2.x 或更早時給出 dismiss 會報錯並提示升級插件
- `提示` 設了關閉挑戰的鬧鐘總能結束: 自動停止 (未單獨設定時至少 30 分鐘) 與緊急關閉 (響鈴畫面 4 秒內按 5 次音量鍵) 始終可用; 緊急關閉可以在設定頁關閉, 關閉後完成挑戰之前只有自動停止能結束響鈴
- `新增` 關閉挑戰: 鬧鐘可以要求先答對算術題 (簡單, 中等, 困難三檔, 預設 3 題), 搖動手機 (預設 30 次) 或輸入一句文字 (自訂, 或隨機選一句內置句子) 才能關閉, 可以設定挑戰期間是否允許貪睡; 在編輯頁的 "關閉挑戰" 一行設定, 響鈴畫面的挑戰取代關閉滑塊
- `新增` 挑戰生效時, 通知的 "關閉" 按鈕與其他應用程式的關閉請求不再直接關閉鬧鐘, 而是打開響鈴畫面的挑戰; 不允許貪睡時通知不顯示 "貪睡" 按鈕; 裝置沒有加速度計時搖動改為算術題
- `新增` 安全閥: 設了挑戰而未單獨設定自動停止時, 自動停止取設定頁的預設值與 30 分鐘中較長的一個, 響鈴畫面顯示剩餘時間; 響鈴畫面在前台時 (包括在鎖屏上) 4 秒內按 5 次音量鍵即可緊急關閉, 挑戰期間音量鍵只用於計數, 不調節音量; 設定頁新增 "關閉挑戰" 組, 可以關閉緊急關閉 (預設開啟)
- `新增` 腳本條件: 由 AutoJs6 腳本設定, 鬧鐘一直響到腳本關閉它; 裝置開機後尚未解鎖, 沒有腳本, AutoJs6 不可用, 腳本未能啟動或已經結束時, 立即退回到設定的挑戰 (預設為普通關閉), 原因記錄在插件日誌中
- `新增` AutoJs6 腳本模組新增鬧鐘的 dismiss 選項 (挑戰種類, 算術題難度與題數, 搖動次數, 要輸入的文字, 腳本無法執行時改用的挑戰, 是否允許貪睡), alarms.ringing() 與 ring / dismiss 事件的 challenge, 以及 dismiss 事件的 by: 'challenge' (需要 AutoJs6 6.8.1; 參閱 [AutoJs6 文件](https://docs.autojs6.com/#/alarms))
- `新增` 插件工具 alarm_add 與 alarm_update 支援 dismiss 選項, 可以設定算術題, 搖動與輸入文字三種挑戰 (不含腳本條件)

#### v1.2.0

_2026/11/13_

- `提示` 腳本中的 alarms.holidays, 鬧鐘的 holidays 選項與 beforeRing 事件需要 AutoJs6 6.8.1 (內部版本號 6641) 或更高版本, 並需要本插件 1.2.0 或更高版本; 插件仍為 1.1.x 或更早時呼叫會報錯並提示升級插件
- `提示` 節假日與調休補班日是否準確完全取決於上游數據 (開源項目 python-holidays); 尚未公佈的年份按規則推算並標為 "推算", 公佈後隨數據更新修正
- `新增` 節假日: 鬧鐘新增 "節假日不響", "只在節假日響" 與 "只在工作日響" 三種選項 (只在工作日響即每個工作日都響, 包括調休補班日, 週末與節假日不響); 內置 250 個國家和地區 (包括 600 多個下級地區) 2020 - 2035 年的節假日數據, 中國內地與台灣包括調休補班日
- `新增` 設定頁可以選擇預設節假日地區 (首次按系統地區推斷), 每個鬧鐘也可以單獨指定地區; 鬧鐘列表顯示下一次響鈴略過了幾個節假日; 推算的年份標為 "推算", 提示可能有調休或補班
- `新增` 節假日數據每週自動檢查更新 (可在設定頁關閉, 預設不在計量網絡上下載), 也可以在設定頁手動檢查; 下載的數據經簽名驗證, 不通過時繼續使用原有數據; 某年沒有數據時鬧鐘只按重複規則響, 不會因此不響
- `新增` 響鈴前由腳本決定 (beforeRing): 登記的腳本在響鈴前 (預設 60 秒, 可設 15 秒至 5 分鐘) 被喚起, 可以略過這一次, 或照常響鈴並臨時改用其他標籤, 鈴聲, 朗讀文字或震動; 腳本沒有及時決定, 出錯或 AutoJs6 無法使用時照常響鈴
- `新增` AutoJs6 腳本模組新增 alarms.holidays (查詢地區, 判斷節假日與工作日, 列出全年節假日, 查看與更新數據), 鬧鐘的 holidays 選項與 beforeRing 事件的 skip / ring, alarms.capabilities() 新增 warnings 提示缺少數據或推算年份 (需要 AutoJs6 6.8.1; 參閱 [AutoJs6 文件](https://docs.autojs6.com/#/alarms))
- `新增` 插件工具新增 holiday_check, 可經 MCP Server 插件或 plugins.tools.call 查詢某天在某地區是否節假日或工作日及節假日名稱; alarm_add 與 alarm_update 支援 holidays 選項

#### v1.1.0

_2026/11/12_

- `提示` 腳本中的 alarms.timers 與 alarms.stopwatch 需要 AutoJs6 6.8.1 (內部版本號 6641) 或更高版本, 並需要本插件 1.1.0 或更高版本; 插件仍為 1.0.x 時呼叫會報錯並提示升級插件
- `新增` 計時器: 最多 20 個計時器同時運行, 時長可以選預設 (1 分鐘至 1 小時) 或自訂 (1 秒至 100 小時), 可以加標籤, 可以暫停, 繼續, 加 1 分鐘與重置; 到時後與鬧鐘一樣響鈴
- `新增` 運行中的計時器在通知欄顯示倒數, 可以直接在通知上暫停, 加 1 分鐘或取消; 裝置重新開機後計時器繼續計時, 關機期間到時的計時器在開機後補響 (過時超過 5 分鐘的只留下一條錯過的通知)
- `新增` 秒錶: 開始, 暫停, 繼續與重置, 最多記錄 100 圈, 標出最快與最慢的一圈; 運行時在通知欄顯示, 可以在設定頁的 "計時器與秒錶" 分組中關閉
- `新增` 底部導覽列在鬧鐘, 計時器與秒錶三個頁面之間切換
- `新增` AutoJs6 腳本模組新增 alarms.timers 與 alarms.stopwatch (需要 AutoJs6 6.8.1): 開始, 查詢, 暫停, 繼續, 加減時間, 重置與取消計時器, 為計時器登記在響鈴等事件發生時啟動的腳本, 控制秒錶並記錄計圈 (參閱 [AutoJs6 文件](https://docs.autojs6.com/#/alarms))
- `新增` 插件工具新增 timer_start, timer_list 與 timer_control, 可經 MCP Server 插件或 plugins.tools.call 開始, 查看, 暫停, 繼續, 加時, 重置與取消計時器
- `新增` 系統時鐘對接新增計時器要求: 其他應用程式可以讓插件開始計時器, 開啟計時器頁或關閉正在響的計時器

##### 更多發行歷史可參閱

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-HK.md)

******

### 構建

******

本節面向希望從源碼構建外掛的開發者.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### 許可

******

項目代碼使用 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE). 第三方元件與數據的授權見第三方聲明. 節假日數據由開源項目 python-holidays (MIT 授權) 生成, 授權全文與貢獻者名單隨數據一同發佈.

- [第三方聲明 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/THIRD_PARTY_NOTICES.md)

******

### 相關連結

******

- AutoJs6 項目: https://github.com/SuperMonster003/AutoJs6
