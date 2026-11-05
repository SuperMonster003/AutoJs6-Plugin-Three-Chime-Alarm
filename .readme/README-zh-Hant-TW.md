<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>鬧鐘, 計時器與碼錶, 支援日曆重複規則, 假日略過與響鈴時連動指令碼</p>

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

3-Chime Alarm 是 AutoJs6 的鬧鐘外掛, 也是不安裝 AutoJs6 也能單獨使用的完整時鐘應用程式: 鬧鐘, 多個並行的倒數計時器與碼錶. 鬧鐘依 RFC 5545 日曆規則重複 (例如每月最後一個星期五, 或從某天起每隔 3 天), 可以略過國定假日, 響鈴時可以啟動指令碼或通知正在執行的指令碼.

外掛使用 Android 精確鬧鐘自行排程並響鈴, 因此 AutoJs6 沒有執行時鬧鐘也會準時響. AutoJs6 指令碼透過全域模組 `alarms` 查詢, 新增, 修改與監聽外掛中的鬧鐘; 子命名空間 `alarms.system` 直接呼叫系統時鐘應用程式, 未安裝外掛時也能使用. 外掛也可以接收其他應用程式發出的設定鬧鐘要求.

> 目前為開發預覽版: 外掛可以被 AutoJs6 外掛中心辨識, 但鬧鐘, 計時器, 碼錶, 外掛介面與 `alarms` 指令碼模組尚未交付, 圖示也是候選圖案, 之後可能更換. 下文描述的是各版本的設計目標, 進度見開發路線圖.

******

### 功能亮點

******

- 鬧鐘: 單次或重複鬧鐘. 重複規則支援工作日, 每月第幾個星期幾, 月底, 間隔天數, 截止日期與次數, 也可以只略過下一次響鈴.
- 響鈴: 系統鈴聲或本機音訊, 朗讀文字, 獨立音量與漸強, 自訂震動, 鎖定畫面時顯示全螢幕響鈴介面, 支援貪睡.
- 指令碼聯動: 鬧鐘可以登記一個指令碼檔案, 響鈴時由 AutoJs6 啟動, 指令碼不必常駐; 正在執行的指令碼也可以即時監聽響鈴, 貪睡與關閉事件.
- 計時器與碼錶 (1.1.0): 多個計時器並行, 可以暫停, 繼續與加時, 裝置重新啟動後恢復; 碼錶支援計圈.
- 假日 (1.2.0): 內建多地區開源假日資料, 國定假日略過響鈴, 補班日照常響鈴; 也可以用 `beforeRing` 指令碼掛鉤決定是否響鈴.
- 關閉挑戰 (1.3.0): 算術題, 搖一搖, 照打一句話或指令碼條件, 都設有安全閥, 確保鬧鐘總能被關閉.

******

### 使用方法

******

1. 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases) 下載 APK 並安裝. 安裝包不區分 ABI, 適用於所有裝置.
2. 開啟外掛, 依提示授予通知, 精確鬧鐘與全螢幕提醒權限, 之後就可以像一般時鐘應用程式一樣使用.
3. 若要在指令碼中使用 `alarms` 模組, 請將 AutoJs6 升級到內部版本號 6640 (6.8.1) 或更新版本, 並在外掛中心啟用本外掛.

> 外掛支援 Android 7.0 (API 24) 以上的裝置. APK 不含原生程式庫, 因此只有一個安裝包, 也不涉及 16 KB 記憶體分頁大小的檢查.

******

### 腳本 API

******

指令碼透過全域模組 `alarms` (也可寫作 `$alarms`) 使用這些功能 (1.0.0 設計草案, 以發布版本的文件為準):

```javascript
let a = alarms.add({ time: "07:30", repeat: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", label: "Wake up" });
a.bind("./after-wake.js", { events: ["dismiss"] });

alarms.add({ time: "18:00", repeat: "FREQ=MONTHLY;BYDAY=-1FR", label: "Monthly report" });
alarms.add({ time: "06:10", date: "2026-11-04", repeat: { freq: "daily", interval: 3 }, tag: "shift" });

alarms.on("ring", (e) => toast(e.label));

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

- `alarms.add(options)` 新增鬧鐘並傳回鬧鐘物件; `alarms.list`, `get`, `update`, `remove`, `setEnabled` 與 `skipNext` 管理外掛中的所有鬧鐘, 包括在外掛介面建立的鬧鐘.
- `repeat` 接受 RRULE 字串 (例如 `FREQ=MONTHLY;BYDAY=-1FR`) 或結構化物件; `alarms.preview(options)` 列出接下來的響鈴時間, 方便核對規則.
- `alarm.bind(script, { events })` 登記響鈴時由 AutoJs6 啟動的指令碼, 事件資料可透過 `alarms.currentEvent()` 讀取; `alarms.on('ring', listener)` 在指令碼執行期間即時監聽.
- `alarms.system` 直接呼叫系統時鐘應用程式 (`setAlarm`, `setTimer`, `showAlarms`, `next` 等), 未安裝外掛時也能使用.

******

### 常見問題

******

#### 不安裝 AutoJs6 能用嗎?

能. 外掛內建完整的時鐘介面, 可以單獨當作鬧鐘, 計時器與碼錶使用. 只有在指令碼需要管理鬧鐘或聯動指令碼時才用到 AutoJs6.

#### AutoJs6 沒有執行時鬧鐘還會響嗎?

會. 鬧鐘由外掛透過 Android 精確鬧鐘排程並響鈴, 不依賴 AutoJs6 程序. 登記了指令碼時, 外掛在響鈴時喚醒 AutoJs6 啟動該指令碼.

#### 為什麼需要精確鬧鐘與全螢幕提醒權限?

精確鬧鐘確保準時響鈴 (鬧鐘類應用程式在安裝時即可取得), 全螢幕提醒用於在鎖定畫面上顯示響鈴介面, 通知用於顯示正在響鈴的鬧鐘. 如果系統撤銷了其中某項權限, 外掛的權限頁會說明如何重新開啟.

******

### 權限與安全

******

外掛在設計上盡量縮小權限範圍:

- 受保護的入口: 能力服務與啟用入口受 AutoJs6 外掛權限 (`org.autojs.permission.PLUGIN`) 保護, 其他應用程式無法直接呼叫; 接收系統時鐘要求的入口要求呼叫端持有 `com.android.alarm.permission.SET_ALARM`.
- 網路: 1.0.0 不申請網路權限, 鬧鐘, 計時器與碼錶全部在本機完成.
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
minimum host build: 6640
native libraries: none (a single APK for every ABI; the 16 KB page size check does not apply)
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

_2026/11/05_

- `提示` 開發預覽: 外掛可以被 AutoJs6 外掛中心辨識, 鬧鐘, 計時器, 碼錶, 外掛介面與 alarms 指令碼模組尚未交付
- `提示` 需要 AutoJs6 6.8.1 (6640) 或更高版本 (確切的最低版本號待宿主端變更完成後回填)
- `提示` 目前圖示為候選圖案, 之後可能更換
- `新增` 外掛骨架: Wake 啟用協定, 外掛中心身分服務與鬧鐘能力服務
- `新增` 安裝包不含原生程式庫, 只有一個適用於所有裝置的 APK
- `新增` 10 種語言的 README, 外掛中心說明與更新日誌, 由 JSON 來源檔案統一產生
- `相依性` 附加 AndroidX Core KTX 版本 1.15.0
- `相依性` 附加 lib-recur 版本 0.17.1 (計算鬧鐘的重複規則)

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
