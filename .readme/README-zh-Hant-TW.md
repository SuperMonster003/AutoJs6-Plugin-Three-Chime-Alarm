<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>鬧鐘應用程式, 支援日曆重複規則, 朗讀響鈴與響鈴時連動指令碼</p>

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
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-HK.md)
- 繁體中文 (台灣) [zh-Hant-TW] # 目前
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

3-Chime Alarm 是 AutoJs6 的鬧鐘外掛, 也是不安裝 AutoJs6 也能獨立使用的鬧鐘應用程式. 鬧鐘按 RFC 5545 日曆規則重複 (例如每月最後一個星期五, 或從某天起每隔 3 天), 響鈴時可以朗讀文字, 也可以啟動指令碼或通知正在執行的指令碼. 計時器與碼錶, 假日略過與關閉挑戰將在後續版本加入.

外掛使用 Android 精確鬧鐘自行排程並響鈴, 因此 AutoJs6 沒有執行時鬧鐘也會準時響. AutoJs6 指令碼透過全域模組 `alarms` 查詢, 新增, 修改與監聽外掛中的鬧鐘; 子命名空間 `alarms.system` 直接呼叫系統時鐘應用程式, 未安裝外掛時也能使用. 外掛也可以接收其他應用程式發出的設定鬧鐘要求.

> 1.0.0 提供鬧鐘, 外掛介面, 外掛工具與 `alarms` 指令碼模組. 在外掛中心啟用本外掛, 使用外掛工具與 `alarms` 模組需要 AutoJs6 6.8.1 (內部版本號 6641) 或更新版本; 在較早的 AutoJs6 上 (包括 6.8.0), 外掛只能作為獨立的鬧鐘應用程式使用. 計時器, 碼錶, 假日與關閉挑戰的進度見開發路線圖.

******

### 功能亮點

******

- 鬧鐘: 單次或重複鬧鐘. 重複規則支援工作日, 每月第幾個星期幾, 月底, 間隔天數, 截止日期與次數, 也可以排除指定日期或只略過下一次響鈴.
- 響鈴: 系統鈴聲或匯入的本機音訊, 朗讀文字, 獨立音量與漸強, 自訂震動, 貪睡與自動停止, 鎖定時顯示全螢幕響鈴畫面; 多個鬧鐘同時到期時依序響鈴.
- 準時可靠: 裝置重新開機, 修改時間或時區, 外掛升級後自動重新排程, 開機後未解鎖也會響鈴; 關機錯過的鬧鐘在 5 分鐘內補響, 超過則留下一則通知.
- 指令碼連動: 鬧鐘可以登記指令碼檔案, 響鈴, 貪睡, 關閉等事件發生時由 AutoJs6 啟動, 指令碼不必常駐; 正在執行的指令碼也可以即時監聽這些事件.
- 外掛工具: 經 MCP Server 外掛或 `plugins.tools.call` 新增, 查詢, 修改與刪除鬧鐘, 處理正在響的鬧鐘 (9 個 `alarm_*` 工具).
- 系統時鐘對接: 可以處理其他應用程式發出的設定, 顯示, 關閉與貪睡鬧鐘要求.
- 介面: 語言, 深色模式與主題色預設跟隨 AutoJs6, 也可以單獨設定; 可以切換啟動器圖示; 設定頁可以設定鈴聲, 震動, 貪睡與自動停止的預設值.
- 計時器與碼錶 (1.1.0 規劃): 多個計時器並行, 可以暫停, 繼續與延長時間; 碼錶支援分圈.
- 假日 (1.2.0 規劃): 內建多地區開源假日資料, 國定假日略過響鈴, 調整放假的補班日照常響鈴; 也可以用 `beforeRing` 指令碼掛鉤決定是否響鈴.
- 關閉挑戰 (1.3.0 規劃): 算術題, 搖一搖, 照打一句話或指令碼條件, 都帶安全閥, 確保鬧鐘總能被關閉.

******

### 螢幕截圖

******

螢幕截圖來自 Sony XQ-DQ72, 上排為淺色主題, 下排為深色主題.

| 鬧鐘清單 | 編輯鬧鐘 | 響鈴 | 設定 |
| :---: | :---: | :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-light.png?raw=true" alt="list-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-light.png?raw=true" alt="edit-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-light.png?raw=true" alt="ring-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-light.png?raw=true" alt="settings-light" width="200" /> |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-dark.png?raw=true" alt="list-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-dark.png?raw=true" alt="edit-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-dark.png?raw=true" alt="ring-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-dark.png?raw=true" alt="settings-dark" width="200" /> |

******

### 使用方法

******

1. 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases) 下載 APK 並安裝. 安裝套件不區分 ABI, 適用於所有裝置.
2. 開啟外掛, 在權限頁逐項處理: 通知, 精確鬧鐘 (Android 12 與 12L 需要手動允許), 全螢幕提醒 (Android 14 起可能未授予), 以及電池最佳化, 背景限制與廠商自啟動. 之後就可以像一般時鐘應用程式一樣使用.
3. 若要在指令碼中使用 `alarms` 模組或使用外掛工具, 請將 AutoJs6 升級到 6.8.1 (內部版本號 6641) 或更新版本, 並在外掛中心啟用本外掛 (安裝精靈的 "工具" 分類).

> 外掛支援 Android 7.0 (API 24) 以上的裝置. 只有一個適用於所有裝置的安裝包; 其中唯一的原生程式庫來自介面框架 Compose, 已按 16 KB 記憶體分頁對齊, 在 16 KB 記憶體分頁的裝置上也能執行.

******

### 腳本 API

******

指令碼透過全域模組 `alarms` (也可寫作 `$alarms`, 僅 Rhino) 使用這些功能, 完整說明見 [AutoJs6 文件](https://docs.autojs6.com/#/alarms):

```javascript
let a = alarms.add({ time: "07:30", repeat: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", label: "Wake up" });
a.bind("./after-wake.js", { events: ["dismiss"] });

alarms.add({ time: "18:00", repeat: "FREQ=MONTHLY;BYDAY=-1FR", label: "Monthly report" });
alarms.add({ time: "06:10", date: "2026-11-04", repeat: { freq: "daily", interval: 3 }, tag: "shift" });

alarms.on("ring", (e) => toast(e.label));

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

- `alarms.add(options)` 新增鬧鐘並傳回鬧鐘快照; `alarms.list`, `get`, `update`, `remove`, `setEnabled`, `skipNext` 與 `cancelSkip` 管理外掛中的所有鬧鐘, 包括在外掛介面建立的鬧鐘.
- `repeat` 接受 RRULE 字串 (例如 `FREQ=MONTHLY;BYDAY=-1FR`), 星期編號陣列或結構化物件; `alarms.preview(options)` 列出接下來的響鈴時間, 方便核對規則.
- `alarms.bind(id, script, options)` (或鬧鐘快照的 `bind`) 登記指令碼, 事件發生時由 AutoJs6 啟動, 事件資料可透過 `alarms.currentEvent()` 讀取; `alarms.on('ring', listener)` 在指令碼執行期間即時監聽.
- `alarms.ringing`, `dismiss` 與 `snooze` 處理正在響的鬧鐘; `alarms.ringtones.list()` 列出可用的鈴聲.
- `alarms.system` 直接呼叫系統時鐘應用程式 (`setAlarm`, `setTimer`, `showAlarms`, `dismissAlarm`, `next` 等), 不需要安裝外掛.

******

### 系統時鐘對接

******

外掛可以作為系統的鬧鐘應用程式, 處理其他應用程式 (例如日曆, 語音助理與自動化工具) 發出的標準時鐘要求. 發出要求的應用程式需要持有 `com.android.alarm.permission.SET_ALARM` 權限:

- 設定鬧鐘 (`SET_ALARM`): 讀取時間, 標籤, 星期, 震動與鈴聲; 要求略過介面時直接儲存, 否則開啟預先填好的編輯頁.
- 顯示鬧鐘 (`SHOW_ALARMS`): 開啟鬧鐘清單.
- 關閉鬧鐘 (`DISMISS_ALARM`): 結束正在響或貪睡中的鬧鐘; 沒有正在響的鬧鐘時, 24 小時內要響的鬧鐘改為略過這一次; 符合多個鬧鐘時由使用者選擇.
- 貪睡 (`SNOOZE_ALARM`): 讓正在響的鬧鐘貪睡.
- 方向相反的 `alarms.system`: 指令碼用它把要求傳給系統預設的時鐘應用程式 (本外掛或其他時鐘應用程式), 不需要安裝外掛; 各品牌時鐘應用程式的差異見 AutoJs6 文件.

******

### 相容性

******

外掛支援 Android 7.0 (API 24) 以上. 在外掛中心啟用本外掛, 使用外掛工具與 `alarms` 模組需要 AutoJs6 6.8.1 (內部版本號 6641) 或更新版本. 在較早的 AutoJs6 上 (例如內部版本號為 6640 的 6.8.0), 外掛中心提示需要更新版本的 AutoJs6 且不能啟用本外掛, 外掛工具與 `alarms` 模組無法使用, 但外掛仍可作為獨立的鬧鐘應用程式使用, 也照常處理系統時鐘要求. `alarms` 模組只支援 Rhino 引擎, 其中的 `alarms.system` 不需要安裝外掛.

1.0.0 發布前在以下裝置上完成驗證 (數據見開發路線圖附錄 D):

| 裝置 | Android | 結果 |
| --- | --- | --- |
| Sony XQ-AT72 | 12 (API 31) | 通過: 到點偏差中位數 44 ms; 鎖定畫面響鈴, 重新開機後到點響鈴, 修改時間, 時區與日光節約時間, 經 MCP Server 呼叫外掛工具 |
| Sony XQ-DQ72 | 13 (API 33) | 通過: 到點偏差中位數 13 ms; 鎖定畫面全螢幕響鈴, 處理程序被清理後到點約 1.1 秒顯示響鈴畫面 |
| Redmi 22120RN86C (MIUI) | 13 (API 33) | 通過: 到點偏差中位數 40 ms; 開機後系統多次結束外掛處理程序, 到點仍準時響鈴 |
| Xiaomi Pad 23046RP50C | 15 (API 35) | 通過: 到點偏差中位數 32 ms; 未授予全螢幕提醒時以浮動通知響鈴, 重新開機後未解鎖也準時響鈴 |
| OnePlus OPD2413 (ColorOS 15) | 15 (API 35) | 通過 (需開啟自動啟動): 到點偏差中位數 23 ms; 在 設定 > 應用程式 > 自動啟動 中開啟後, 處理程序被清理也準時響鈴 (見常見問題) |
| Sony G8441 | 9 (API 28) | 通過: 到點偏差中位數 41 ms; 鎖定畫面響鈴, 重新開機後未解鎖也準時響鈴 |
| Android 模擬器 | 7.0 - 16 (API 24 - 36) | 通過: 最低版本完整流程, 首次解鎖前響鈴, 16 KB 記憶體分頁 |

******

### 常見問題

******

#### 不安裝 AutoJs6 能用嗎?

能. 外掛內建完整的鬧鐘介面, 可以獨立當作鬧鐘應用程式使用. 只有在指令碼需要管理鬧鐘, 連動指令碼或使用外掛工具時才用到 AutoJs6.

#### AutoJs6 沒有執行時鬧鐘還會響嗎?

會. 鬧鐘由外掛透過 Android 精確鬧鐘排程並響鈴, 不依賴 AutoJs6 處理程序. 登記了指令碼時, 外掛在響鈴時喚醒 AutoJs6 啟動該指令碼.

#### 為什麼 AutoJs6 6.8.0 的外掛中心不能啟用本外掛?

外掛中心啟用本外掛, 外掛工具與 `alarms` 模組需要 AutoJs6 6.8.1 (內部版本號 6641) 或更新版本, 而 6.8.0 的內部版本號為 6640. 升級 AutoJs6 後即可啟用; 在此之前外掛可以作為獨立的鬧鐘應用程式使用.

#### 鬧鐘沒有準時響, 應該檢查什麼?

先開啟外掛的權限頁, 確認通知與精確鬧鐘已允許, 並依頁面說明關閉電池最佳化, 解除背景限制, 允許自啟動. 零打擾模式預設允許鬧鐘, 若改過零打擾設定, 請確認鬧鐘仍被允許. 關機或系統延後導致錯過的鬧鐘, 5 分鐘內會補響, 超過則不響並留下一則通知.

#### OPPO, 一加, realme (ColorOS) 上鬧鐘不響怎麼辦?

ColorOS 有兩處互相獨立的開關, 都要為 3-Chime Alarm 開啟: 應用程式資訊中的 "允許自動啟動" / "關聯啟動" / "背景活動", 以及 設定 > 應用程式 > 自動啟動 清單. 只開前者時, 外掛處理程序被清理 (例如在最近使用的應用程式中滑掉) 後, 系統會攔下到點的鬧鐘. 連動指令碼時 AutoJs6 也要在兩處開啟. 另外請不要在最近使用的應用程式中滑掉外掛, 可以把它鎖定. 外掛權限頁在 ColorOS 上有對應的引導列; 設定頁的 "保持背景執行" 會常駐一則靜音通知, 進一步減少外掛被清理的機會, 耗電略有增加.

#### ColorOS 上鎖定畫面響鈴時只有鈴聲和通知, 沒有跳出響鈴畫面?

ColorOS 新安裝應用程式的 "橫幅" 通知預設關閉, 系統因此不顯示全螢幕響鈴畫面. 在外掛的通知設定中開啟 "橫幅" 即可, 權限頁的 "橫幅通知" 一列可以直達該設定; 也可以在權限頁允許 "懸浮視窗" 作為備援.

#### 強制停止外掛後鬧鐘不響了?

"強制停止" 會讓系統清除外掛的所有鬧鐘排程, 停止期間到點不響, 這是 Android 的規則. 從啟動器開啟一次外掛即可恢復排程; 恢復時仍在 5 分鐘內的鬧鐘會補響.

#### 小米 (MIUI, HyperOS) 需要特別設定嗎?

實測 Redmi (MIUI) 在開機後會多次結束外掛處理程序, 到點時系統仍會啟動外掛並準時響鈴. 仍建議依權限頁的說明設定自啟動與省電策略.

#### 裝置重新開機後還沒解鎖, 鬧鐘會響嗎? 連動的指令碼呢?

鬧鐘會響, 外掛在首次解鎖之前也能執行. 連動的指令碼不會啟動, 因為 AutoJs6 在首次解鎖之前不能執行指令碼, 解鎖後也不會補啟動.

******

### 權限與安全

******

外掛申請的權限與用途:

- 精確鬧鐘 (`USE_EXACT_ALARM`, Android 12 與 12L 為 `SCHEDULE_EXACT_ALARM`): 準時響鈴. Android 13 起安裝即授予, Android 12 與 12L 需要在系統設定中允許 "鬧鐘與提醒".
- 通知 (`POST_NOTIFICATIONS`): 顯示正在響的鬧鐘與錯過的鬧鐘. 缺少時鬧鐘照常發聲, 但沒有通知與響鈴畫面.
- 全螢幕提醒 (`USE_FULL_SCREEN_INTENT`): 在鎖定畫面顯示響鈴畫面. Android 14 起可能未授予, 缺少時以浮動通知響鈴.
- 懸浮視窗 (`SYSTEM_ALERT_WINDOW`, 選用): 系統不顯示全螢幕提醒時 (例如 ColorOS 的橫幅通知關閉) 用來開啟響鈴畫面, 只在權限頁由使用者視需要允許.
- 開機啟動 (`RECEIVE_BOOT_COMPLETED`): 重新開機後重新排程, 首次解鎖前也能響鈴.
- 前景服務 (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`): 響鈴期間保持發聲; 設定頁的 "保持背景執行" 開啟時也用它常駐 (預設關閉).
- 震動, 喚醒鎖定與音訊設定 (`VIBRATE`, `WAKE_LOCK`, `MODIFY_AUDIO_SETTINGS`): 震動, 響鈴時保持裝置喚醒, 以及獨立的鬧鐘音量在響鈴後還原.
- 網路 (`INTERNET`, `ACCESS_NETWORK_STATE`): 只用於設定頁的更新檢查 (1.2.0 起也用於更新假日資料). 只在開啟鬧鐘清單時依設定的頻率檢查, 計量付費網路預設不檢查; 排程, 響鈴與 AutoJs6 連動從不連網.
- AutoJs6 外掛權限 (`org.autojs.permission.PLUGIN`): 與 AutoJs6 通訊.

外掛在設計上盡量縮小權限範圍:

- 受保護的入口: 能力服務與啟用入口受 AutoJs6 外掛權限 (`org.autojs.permission.PLUGIN`) 保護, 其他應用程式無法直接呼叫; 接收系統時鐘要求的入口要求呼叫端持有 `com.android.alarm.permission.SET_ALARM`.
- 網路: 只用於檢查外掛更新 (連線到 GitHub, 可在設定頁關閉自動檢查), 不上傳鬧鐘或任何個人資料.
- 儲存空間: 鬧鐘儲存在外掛私有目錄, 不參與雲端備份; 本機鈴聲透過系統檔案選擇器匯入, 不申請儲存權限.
- 指令碼權限: 指令碼可以透過 `alarms` 模組讀寫外掛中的所有鬧鐘, 包括在外掛介面建立的鬧鐘; 登記的指令碼儲存在 AutoJs6 中, 外掛介面只標記鬧鐘有指令碼綁定並可以解除.

******

### 外掛介面

******

以下資訊面向 AutoJs6 主程式與外掛開發者, 主程式透過這些識別碼發現外掛並完成能力協商:

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

功能規劃, 設計決策與完成情況以可勾選清單維護在 ROADMAP.md 中. 未勾選的項目表示規劃意向, 不代表目前版本的能力.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.0.0

_2026/11/07_

- `提示` 外掛中心啟用, 外掛工具與 alarms 指令碼模組需要 AutoJs6 6.8.1 (內部版本號 6641) 或更新版本; 在較早的 AutoJs6 上 (例如 6.8.0) 外掛只能作為獨立的鬧鐘應用程式使用
- `提示` ColorOS (OPPO, 一加, realme) 上請在應用程式資訊與 設定 > 應用程式 > 自動啟動 兩處為外掛開啟自動啟動, 否則外掛處理程序被清理後鬧鐘不會響 (參閱 README > 常見問題)
- `新增` 鬧鐘: 單次或重複鬧鐘, 重複規則按 RFC 5545 日曆規則 (工作日, 每月第幾個星期幾, 月底, 間隔天數, 截止日期與次數), 可以排除日期或只略過下一次
- `新增` 響鈴: 系統鈴聲或匯入的本機音訊, 朗讀文字, 獨立音量與漸強, 自訂震動, 貪睡與自動停止, 鎖定畫面全螢幕響鈴畫面, 多個鬧鐘同時到期時依序響鈴
- `新增` 排程: 重新開機, 修改時間或時區與外掛升級後自動重新排程, 首次解鎖前也能響鈴, 錯過的鬧鐘 5 分鐘內補響, 超過則留下一則通知
- `新增` 外掛介面: 鬧鐘清單, 編輯頁, 響鈴畫面, 權限頁與設定頁; 語言, 深色模式與主題色預設跟隨 AutoJs6, 可以切換啟動器圖示
- `新增` 設定頁: 鈴聲, 震動, 貪睡與自動停止的預設值, 時間格式, 自動檢查更新, 以及選用的 "保持背景執行" (常駐一則靜音通知, 減少外掛被系統清理)
- `新增` 權限頁逐項檢查通知, 精確鬧鐘, 全螢幕提醒, 電池最佳化, 背景限制與廠商自啟動, 在 ColorOS 上另有自動啟動清單與橫幅通知的引導
- `新增` AutoJs6 指令碼模組 alarms / $alarms (需要 AutoJs6 6.8.1, 僅 Rhino): 鬧鐘的新增, 查詢, 修改與刪除, 事件監聽, 登記由 AutoJs6 在事件發生時啟動的指令碼, 鈴聲清單 (參閱 [AutoJs6 文件](https://docs.autojs6.com/#/alarms))
- `新增` 外掛工具 alarm_add, alarm_list, alarm_update, alarm_remove, alarm_set_enabled, alarm_skip_next, alarm_upcoming, alarm_dismiss 與 alarm_snooze, 可經 MCP Server 外掛或 plugins.tools.call 呼叫
- `新增` 系統時鐘對接: 處理其他應用程式發出的設定, 顯示, 關閉與貪睡鬧鐘要求
- `新增` 只有一個適用於所有裝置的 APK, 支援 16 KB 記憶體分頁的裝置
- `相依性` 附加 AndroidX Core KTX 版本 1.15.0
- `相依性` 附加 AndroidX Activity Compose 版本 1.12.2
- `相依性` 附加 Jetpack Compose BOM 版本 2026.09.00 (外掛介面)
- `相依性` 附加 Kotlin Coroutines Android 版本 1.11.0
- `相依性` 附加 Material Color Utilities 版本 4.1.1 (主題色)
- `相依性` 附加 lib-recur 版本 0.17.1 (計算鬧鐘的重複規則)
- `相依性` 附加 Desugar JDK Libs 版本 2.1.5 (讓 Android 7.x 也能使用日期與時區運算)

##### 更多發行歷史可參閱

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-TW.md)

******

### 建置

******

本節面向希望從原始碼建置外掛的開發者.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### 授權

******

專案程式碼採用 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE). 第三方元件與資料的授權見第三方聲明.

- [第三方聲明 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/THIRD_PARTY_NOTICES.md)

******

### 相關連結

******

- AutoJs6 專案: https://github.com/SuperMonster003/AutoJs6
