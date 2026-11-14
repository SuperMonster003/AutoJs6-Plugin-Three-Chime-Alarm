<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>カレンダー形式の繰り返しルール, 祝日, 停止チャレンジ, 読み上げ付きの鳴動, 鳴動時のスクリプト連携に対応したアラーム, タイマー, ストップウォッチ</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 言語 (Languages)

******

現在の README.md は次の言語に対応しています:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-es.md)
- 日本語 [ja] # 現在
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ar.md)

******

### 概要

******

3-Chime Alarm は AutoJs6 のアラームプラグインで, AutoJs6 がなくても単独で使えるアラームアプリでもあります. アラームは RFC 5545 のカレンダールールで繰り返し (例えば毎月最後の金曜日, ある日から 3 日おき), 鳴動時にテキストを読み上げたり, スクリプトを起動したり, 実行中のスクリプトに通知したりできます. 1.1.0 からは同時に動く複数のタイマーとストップウォッチも使えます. 1.2.0 からはアラームを祝日に鳴らさない, 祝日だけ鳴らす, 振替出勤日を含むすべての勤務日に鳴らす, のいずれかに設定でき, スクリプトが鳴動の前に今回鳴らすかどうかを決めることもできます. 1.3.0 からは, 計算問題に正解する, スマートフォンを振る, 文を入力する, スクリプトが確認する, のいずれかを済ませないとアラームを止められないように設定でき, その場合も自動停止と緊急停止の 2 つの逃げ道は常に残ります.

プラグインは Android の正確なアラームで自らスケジュールして鳴動するため, AutoJs6 が動いていなくてもアラームは時間どおりに鳴ります. AutoJs6 のスクリプトはグローバルモジュール `alarms` でプラグインのアラームを照会, 作成, 変更, 監視し, `alarms.timers` と `alarms.stopwatch` でタイマーとストップウォッチを操作します. サブ名前空間 `alarms.system` はシステムの時計アプリを直接呼び出し, プラグインがなくても使えます. プラグインは他のアプリからのアラームとタイマーの設定リクエストも受け付けます.

> 1.3.0 ではアラーム (祝日ルール, 鳴動前のスクリプトによる判断, 停止チャレンジを含む), タイマーとストップウォッチ, プラグイン画面, プラグインツール, `alarms` スクリプトモジュールを提供します. プラグインセンターでの有効化, プラグインツール, `alarms` モジュールには AutoJs6 6.8.1 (内部バージョン 6641) 以降が必要です. それより前の AutoJs6 (6.8.0 を含む) では, プラグインは単独のアラームアプリとしてのみ使えます.

******

### 機能ハイライト

******

- アラーム: 単発または繰り返しのアラーム. 繰り返しルールは平日, 毎月第 n 何曜日, 月末, 日数間隔, 終了日と回数に対応し, 特定の日を除外したり次の鳴動だけをスキップしたりできます.
- 鳴動: システムの着信音または取り込んだローカル音声, テキストの読み上げ, 独立した音量とフェードイン, カスタムバイブレーション, スヌーズと自動停止, ロック画面での全画面鳴動画面; 同時に鳴るアラームは順番に鳴動します.
- 時間どおり: 再起動, 時刻やタイムゾーンの変更, プラグインの更新の後に自動で再スケジュールし, 再起動後のロック解除前でも鳴動します. 電源オフで逃したアラームは 5 分以内なら鳴動し, それを過ぎると通知を残します.
- スクリプト連携: アラームやタイマーにスクリプトファイルを登録すると, 鳴動, スヌーズ, 停止などのイベントで AutoJs6 が起動するため, スクリプトを常駐させる必要はありません. 実行中のスクリプトもこれらのイベントをリアルタイムで監視できます.
- プラグインツール: MCP Server プラグインや `plugins.tools.call` からアラームの作成, 取得, 変更, 削除 (スクリプト条件以外の停止チャレンジの設定を含む) と鳴動中のアラームの操作, タイマーの開始, 一覧, 操作, 祝日の確認ができます (`alarm_*` ツール 9 個, `timer_*` ツール 3 個と `holiday_check`).
- システムの時計との連携: 他のアプリからのアラームの設定, 表示, 停止, スヌーズの要求と, タイマーの設定, 表示, 停止の要求を処理できます.
- 画面: 言語, ダークモード, テーマカラーは既定で AutoJs6 に従い, 個別にも設定できます. ランチャーアイコンを切り替えられ, 設定画面で着信音, バイブレーション, スヌーズ, 自動停止の既定値と, 緊急停止を許可するかどうかを設定できます.
- タイマーとストップウォッチ: 最大 20 個のタイマーを同時に動かし, 一時停止, 再開, 時間の追加, リセットができます. 動作中のタイマーは通知領域にカウントダウンを表示し, そこから一時停止, 1 分追加, キャンセルができます. 再起動後もカウントを続け, 時間になるとアラームと同じように鳴ります. ストップウォッチはラップ (最大 100 周) を記録し, 最速と最遅のラップに印を付けます. 下部のナビゲーションでアラーム, タイマー, ストップウォッチを切り替えます.
- 祝日: アラームごとに "祝日は鳴らさない", "祝日のみ鳴らす", "勤務日のみ鳴らす" (振替出勤日を含むすべての勤務日に鳴り, 週末と祝日は鳴らない) を選べます. 250 の国と地域 (600 以上の下位地域を含む) の 2020 - 2035 年のデータを内蔵し, データはオープンソースプロジェクト python-holidays から生成しています. 更新は毎週自動で確認し, ダウンロードしたデータは署名を検証します. まだ公表されていない年はルールから推定して "推定" と表示し, データのない年は繰り返しルールだけで鳴ります.
- 鳴動前のスクリプトによる判断: 登録したスクリプトが鳴動の前 (既定 60 秒, 15 秒から 5 分まで) に AutoJs6 から起動され, 今回の鳴動をスキップしたり, 今回だけ別のラベル, アラーム音, 読み上げテキスト, バイブレーションで鳴らしたりできます. スクリプトが時間内に決めなかったとき, エラーになったとき, AutoJs6 が使えないときは通常どおり鳴ります.
- 停止チャレンジ: アラームを止めるには計算問題に正解する (やさしい, ふつう, むずかしいの 3 段階, 既定は 3 問), スマートフォンを振る (既定は 30 回), 文を入力する (自分で決めた文, または内蔵の文からランダムに 1 つ) のいずれかが必要になるよう設定でき, チャレンジ中にスヌーズを許可するかどうかも選べます. チャレンジが有効な間は, 通知の "停止" ボタンや他のアプリからの停止リクエストで鳴動画面のチャレンジが開きます. どのチャレンジにも 2 つの逃げ道があります. 1 つは自動停止で, アラームで個別に設定していなければ設定画面の既定値と 30 分の長いほうが過ぎると止まり, 残り時間は鳴動画面に表示されます. もう 1 つは緊急停止で, 鳴動画面が前面にある間 (ロック画面上でも) 4 秒以内に音量ボタンを 5 回押すと止まります (設定画面でオフにできます). AutoJs6 のスクリプトはスクリプト条件も設定できます. アラームはスクリプトが止めるまで鳴り続け, スクリプトを実行できないとき (例えば再起動後の初回ロック解除前や, スクリプトの終了後) はすぐに指定した代替のチャレンジに切り替わります.

******

### スクリーンショット

******

Sony XQ-DQ72 で撮影したスクリーンショットです. 上段がライトテーマ, 下段がダークテーマです.

| アラーム一覧 | アラームの編集 | 鳴動 | 設定 |
| :---: | :---: | :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-light.png?raw=true" alt="list-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-light.png?raw=true" alt="edit-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-light.png?raw=true" alt="ring-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-light.png?raw=true" alt="settings-light" width="200" /> |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-dark.png?raw=true" alt="list-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-dark.png?raw=true" alt="edit-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-dark.png?raw=true" alt="ring-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-dark.png?raw=true" alt="settings-dark" width="200" /> |

| タイマー | ストップウォッチ |
| :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/timers-light.png?raw=true" alt="timers-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/stopwatch-light.png?raw=true" alt="stopwatch-light" width="200" /> |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/timers-dark.png?raw=true" alt="timers-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/stopwatch-dark.png?raw=true" alt="stopwatch-dark" width="200" /> |

******

### 使用方法

******

1. [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases) から APK をダウンロードしてインストールします. パッケージはすべての ABI に共通の 1 つだけです.
2. プラグインを開き, 権限ページで通知, 正確なアラーム (Android 12 と 12L では手動で許可), 全画面通知 (Android 14 以降は許可されていないことがあります), 電池の最適化, バックグラウンド制限, メーカーの自動起動を順に設定します. あとは普通の時計アプリとして使えます.
3. スクリプトで `alarms` モジュールやプラグインツールを使うには, AutoJs6 を 6.8.1 (内部バージョン 6641) 以降に更新し, プラグインセンターでプラグインを有効にします (インストールウィザードの "ツール" カテゴリ).

> プラグインは Android 7.0 (API 24) 以降の端末に対応しています. パッケージはすべての端末に共通の 1 つだけです. 含まれる唯一のネイティブライブラリは UI フレームワーク Compose のもので, 16 KB メモリページに合わせて配置されているため, 16 KB ページの端末でも動作します.

******

### スクリプト API

******

スクリプトはグローバルモジュール `alarms` (`$alarms` とも書けます, Rhino のみ) でこれらの機能を使います. 詳しくは [AutoJs6 ドキュメント](https://docs.autojs6.com/#/alarms) を参照してください:

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

- `alarms.add(options)` はアラームを作成してそのスナップショットを返します. `alarms.list`, `get`, `update`, `remove`, `setEnabled`, `skipNext`, `cancelSkip` で, プラグイン画面で作ったものを含むすべてのアラームを管理できます.
- `repeat` には RRULE 文字列 (例えば `FREQ=MONTHLY;BYDAY=-1FR`), 曜日番号の配列, 構造化オブジェクトを指定できます. `alarms.preview(options)` は次の鳴動時刻を一覧にするので, ルールの確認に使えます.
- `alarms.bind(id, script, options)` (またはアラームスナップショットの `bind`) はイベント発生時に AutoJs6 が起動するスクリプトを登録し, スクリプトはイベントデータを `alarms.currentEvent()` で読み取ります. `alarms.on('ring', listener)` はスクリプトの実行中にリアルタイムで監視します.
- `alarms.ringing`, `dismiss`, `snooze` は鳴動中のアラームを操作し, `alarms.ringtones.list()` は使える着信音を一覧にします.
- `alarms.timers.start(duration, options)` はタイマーを開始し (時間はミリ秒, または `"25m"`, `"1h 30m"` のような文字列で, 1 秒から 100 時間), `pause`, `resume`, `addTime`, `reset`, `cancel`, `bind` メソッドを持つタイマーのスナップショットを返します. `alarms.stopwatch` はストップウォッチの開始, 一時停止, 再開, ラップ, リセットを行います. どちらもプラグイン 1.1.0 以降が必要です.
- `alarms.holidays` は祝日データを参照します (`regions`, `isHoliday`, `isWorkday`, `list`, `info`, `refresh`). アラームの `holidays` オプション (例: `{ region: 'CN', mode: 'workdays' }`) で祝日ルールを設定し, `alarms.capabilities()` の `warnings` がデータの不足や推定の年を知らせます. プラグイン 1.2.0 以降が必要です.
- `alarms.bind(id, script, { events: ['beforeRing'], leadSeconds: 120 })` は鳴動の前に起動するスクリプトを登録します. スクリプト内で `alarms.currentEvent()` の `skip(reason)` を呼ぶと今回をスキップし, `ring(overrides)` を呼ぶと今回だけ別のラベル, アラーム音, 読み上げ, バイブレーションで鳴らします. `alarms.on('beforeRing', listener)` はリアルタイムで受け取り, イベントにも同じ 2 つのメソッドがあります. プラグイン 1.2.0 以降が必要です.
- `dismiss` オプションで停止チャレンジを設定します. 例えば `alarms.add({ time: '07:00', dismiss: { challenge: 'math', math: { difficulty: 3, count: 5 }, allowSnooze: false } })`. `challenge: 'script'` ではスクリプトが `alarms.dismiss(id)` を呼んで止め, `script.fallback` でスクリプトを実行できないときに使うチャレンジを指定します. `alarms.ringing()` と `ring`, `dismiss` イベントにはその時点で有効な `challenge` が含まれ, チャレンジを完了して止めたときの `dismiss` イベントの `by` は `'challenge'` です. プラグイン 1.3.0 以降が必要です.
- `alarms.system` はシステムの時計アプリを直接呼び出し (`setAlarm`, `setTimer`, `showAlarms`, `dismissAlarm`, `next` など), プラグインは不要です.

******

### システムの時計との連携

******

プラグインはシステムのアラームアプリとして, 他のアプリ (カレンダー, 音声アシスタント, 自動化ツールなど) からの標準の時計の要求を処理できます. 要求するアプリには `com.android.alarm.permission.SET_ALARM` 権限が必要です:

- アラームの設定 (`SET_ALARM`): 時刻, ラベル, 曜日, バイブレーション, 着信音を読み取ります. 画面を省略する要求ならそのまま保存し, それ以外は値を入力済みの編集画面を開きます.
- アラームの表示 (`SHOW_ALARMS`): アラーム一覧を開きます.
- アラームの停止 (`DISMISS_ALARM`): 鳴動中またはスヌーズ中のアラームを終了します. 鳴動中のアラームがなければ, 24 時間以内に鳴るアラームのその回をスキップします. 複数のアラームが該当するときはユーザーが選びます.
- スヌーズ (`SNOOZE_ALARM`): 鳴動中のアラームをスヌーズします.
- タイマーの設定 (`SET_TIMER`): 時間 (1 秒から 100 時間) とラベルを読み取ってタイマーを開始します. 時間とラベルが同じでリセット済みのタイマーがあれば, それを再び開始します. 画面を省略する要求でなければタイマー画面も開き, 時間の指定がなければ新しいタイマーのダイアログを開きます.
- タイマーの表示 (`SHOW_TIMERS`): タイマー画面を開きます.
- タイマーの停止 (`DISMISS_TIMER`): 鳴動中のタイマーを終了し, カウント中のタイマーには影響しません.
- 逆方向の `alarms.system`: スクリプトはこれでシステムの既定の時計アプリ (このプラグインまたは他の時計アプリ) に要求を送り, プラグインは不要です. メーカーごとの時計アプリの違いは AutoJs6 ドキュメントを参照してください.

******

### 互換性

******

プラグインは Android 7.0 (API 24) 以降に対応します. プラグインセンターでの有効化, プラグインツール, `alarms` モジュールには AutoJs6 6.8.1 (内部バージョン 6641) 以降が必要です. それより前の AutoJs6 (例えば内部バージョン 6640 の 6.8.0) では, プラグインセンターは新しい AutoJs6 が必要だと表示してプラグインを有効にできず, プラグインツールと `alarms` モジュールは使えませんが, プラグインは単独のアラームアプリとして使え, システムの時計の要求も通常どおり処理します. `alarms` モジュールは Rhino エンジンのみに対応し, その中の `alarms.system` はプラグインが不要です.

1.0.0 のリリース前に次のデバイスで検証しました (データは開発ロードマップの付録 D). 1.1.0 のタイマーとストップウォッチは Sony XQ-AT72 と Sony G8441 でも検証し, 再起動後もカウントが続くことを確認しました (開発ロードマップの P7). 1.2.0 の祝日と鳴動前のスクリプトによる判断は Sony XQ-AT72 で AutoJs6 と合わせて検証しました (開発ロードマップの P8). 1.3.0 の停止チャレンジは Sony XQ-AT72 で AutoJs6 と合わせて検証し, ロック画面での緊急停止とスクリプト条件のすべての代替への切り替えを確認しました (開発ロードマップの P9):

| デバイス | Android | 結果 |
| --- | --- | --- |
| Sony XQ-AT72 | 12 (API 31) | 合格: 鳴動遅延の中央値 44 ms; ロック画面での鳴動, 再起動後の鳴動, 時刻, タイムゾーン, 夏時間の変更, MCP Server からのプラグインツール呼び出し |
| Sony XQ-DQ72 | 13 (API 33) | 合格: 鳴動遅延の中央値 13 ms; ロック画面での全画面鳴動, プロセス終了後も予定時刻から約 1.1 秒で鳴動画面を表示 |
| Redmi 22120RN86C (MIUI) | 13 (API 33) | 合格: 鳴動遅延の中央値 40 ms; 起動後にシステムがプラグインのプロセスを何度も終了しても時間どおりに鳴動 |
| Xiaomi Pad 23046RP50C | 15 (API 35) | 合格: 鳴動遅延の中央値 32 ms; 全画面通知が許可されていないときはヘッドアップ通知で鳴動, 再起動後のロック解除前も時間どおりに鳴動 |
| OnePlus OPD2413 (ColorOS 15) | 15 (API 35) | 合格 (自動起動の設定が必要): 鳴動遅延の中央値 23 ms; 設定 > アプリ > 自動起動 でオンにすると, プロセスが整理されても時間どおりに鳴動 (よくある質問を参照) |
| Sony G8441 | 9 (API 28) | 合格: 鳴動遅延の中央値 41 ms; ロック画面での鳴動, 再起動後のロック解除前も時間どおりに鳴動 |
| Android エミュレーター | 7.0 - 16 (API 24 - 36) | 合格: 最低バージョンでの全手順, 初回ロック解除前の鳴動, 16 KB メモリページ |

******

### よくある質問

******

#### AutoJs6 なしで使えますか?

はい. プラグインにはアラームの画面が一式あり, 単独でアラームアプリとして使えます. AutoJs6 はスクリプトでアラームを管理したり, 連携したり, プラグインツールを使ったりするときだけ必要です.

#### AutoJs6 が動いていなくてもアラームは鳴りますか?

はい. アラームはプラグインが Android の正確なアラームでスケジュールして鳴らすため, AutoJs6 のプロセスには依存しません. スクリプトが登録されている場合は, 鳴動時にプラグインが AutoJs6 を起こしてスクリプトを起動します.

#### AutoJs6 6.8.0 のプラグインセンターでプラグインを有効にできないのはなぜですか?

プラグインセンターでの有効化, プラグインツール, `alarms` モジュールには AutoJs6 6.8.1 (内部バージョン 6641) 以降が必要ですが, 6.8.0 の内部バージョンは 6640 です. AutoJs6 を更新すれば有効にできます. それまでは単独のアラームアプリとして使えます.

#### アラームが時間どおりに鳴りませんでした. 何を確認すればよいですか?

まずプラグインの権限ページを開き, 通知と正確なアラームが許可されていることを確認し, ページの説明に従って電池の最適化をオフにし, バックグラウンド制限を解除し, 自動起動を許可してください. サイレントモードは既定でアラームを許可します. 設定を変えた場合は, アラームがまだ許可されているか確認してください. 電源オフやシステムの遅延で逃したアラームは 5 分以内なら鳴動し, それを過ぎると鳴らずに通知を残します.

#### OPPO, OnePlus, realme (ColorOS) でアラームが鳴りません. どうすればよいですか?

ColorOS には独立した 2 か所のスイッチがあり, 3-Chime Alarm のためにどちらもオンにする必要があります. アプリ情報の "自動起動" / "関連起動" / "バックグラウンド動作" と, 設定 > アプリ > 自動起動 のリストです. 前者だけだと, プラグインのプロセスが整理された後 (例えば最近のタスクでスワイプして消した後), システムが予定時刻のアラームをブロックします. スクリプト連携を使う場合は AutoJs6 も両方でオンにしてください. また, 最近のタスクでプラグインをスワイプして消さず, ロックしておくこともできます. ColorOS ではプラグインの権限ページに対応する案内の行があり, 設定画面の "バックグラウンドで動作し続ける" をオンにするとサイレント通知を常駐させて整理されにくくできます (電池の消費がわずかに増えます).

#### ColorOS でロック中は着信音と通知だけで, 鳴動画面が表示されません. なぜですか?

ColorOS は新しくインストールしたアプリの "バナー" 通知を既定でオフにしており, そのためシステムが全画面の鳴動画面を表示しません. プラグインの通知設定で "バナー" をオンにしてください. 権限ページの "バナー通知" の行からその設定を直接開けます. 権限ページで "他のアプリの上に重ねて表示" を許可しておくと予備の手段にもなります.

#### プラグインを強制停止したらアラームが鳴らなくなりました.

"強制停止" するとシステムがプラグインのアラームのスケジュールをすべて消去し, 停止中は予定時刻になっても鳴りません. これは Android の規則です. ランチャーからプラグインを一度開けばスケジュールが戻り, 5 分以内の遅れのアラームはそのとき鳴動します.

#### Xiaomi (MIUI, HyperOS) では特別な設定が必要ですか?

Redmi (MIUI) での検証では, 起動後にシステムがプラグインのプロセスを何度も終了しましたが, 予定時刻にはシステムがプラグインを起動して時間どおりに鳴動しました. それでも権限ページの説明に従って自動起動と省電力の設定をしておくことをおすすめします.

#### 再起動後, ロック解除前でもアラームは鳴りますか? 連携スクリプトはどうなりますか?

アラームは鳴ります. プラグインは初回のロック解除前でも動作します. 連携スクリプトは起動しません. AutoJs6 は初回のロック解除前にスクリプトを実行できず, ロック解除後にもあとから起動することはありません. そのため停止チャレンジがスクリプト条件のアラームは, すぐに指定した代替のチャレンジに切り替わります.

#### 再起動後, タイマーとストップウォッチはどうなりますか?

タイマーはシャットダウン前の終了時刻を保ってカウントを続け, 電源オフの間も時間が進みます. 電源オフの間に終了時刻を過ぎたタイマーは, 起動時の超過が 5 分未満なら鳴り, 5 分を超えていれば逃した通知を残します. 起動後にシステムが時刻を補正すると, 終了時刻も数秒ずれることがあります. ストップウォッチは再起動後に一時停止になり, 今回の起動時刻まで計測するため, 電源オフの時間も含まれます (再起動は通常 1 分未満です).

#### 祝日データは正確ですか? 振替出勤日には鳴りますか?

祝日と振替出勤日の正確さは上流のデータ (オープンソースプロジェクト python-holidays) に完全に依存します. "勤務日のみ鳴らす" では振替出勤日にも鳴ります. "祝日は鳴らさない" では繰り返しルールだけで判断します (例えば月曜から金曜のアラームは土曜の振替出勤日には鳴りません). まだ公表されていない年はルールから推定し, 設定画面と編集画面に "推定" と表示し, 公表後のデータ更新で修正されます. データは毎週自動で確認し, 設定画面からすぐに確認することもできます.

#### `beforeRing` のスクリプトが間に合わなかったらどうなりますか?

通常どおり鳴ります. 判断の締め切りは既定で鳴動の 5 秒前です. それまでに判断がないとき, スクリプトがエラーになったとき, AutoJs6 が使えないとき (例えば強制停止された) や再起動後にまだロックを解除していないときは, アラームは通常どおり鳴ります. 祝日ルールですでにスキップされる回には `beforeRing` のスクリプトは起動しません.

#### 停止チャレンジを設定して, 問題が解けない, うまく振れないときはアラームが鳴り続けますか?

鳴り続けません. どのチャレンジにも 2 つの逃げ道があります. アラームで自動停止を個別に設定していなければ, 設定画面の既定値と 30 分の長いほうが過ぎると自動的に止まり, 残り時間は鳴動画面に表示されます. また鳴動画面が前面にある間 (ロック画面上でも), 4 秒以内に音量ボタンを 5 回 (上下どちらでも) 押すとすぐに緊急停止できます. チャレンジ中の音量ボタンは回数を数えるだけで, 音量は変わりません. 緊急停止は既定でオンで, 設定画面の "停止チャレンジ" グループでオフにできます. オフにすると, チャレンジを完了するまでは自動停止でしか鳴動を終えられません.

******

### 権限とセキュリティ

******

プラグインが要求する権限とその用途:

- 正確なアラーム (`USE_EXACT_ALARM`, Android 12 と 12L では `SCHEDULE_EXACT_ALARM`): 時間どおりの鳴動. Android 13 以降はインストール時に許可され, Android 12 と 12L ではシステム設定で "アラームとリマインダー" を許可します.
- 通知 (`POST_NOTIFICATIONS`): 鳴動中のアラームと逃したアラーム, 動作中のタイマーとストップウォッチの表示. ないとアラームとタイマーは鳴りますが, 通知も鳴動画面も表示されません.
- 全画面通知 (`USE_FULL_SCREEN_INTENT`): ロック画面への鳴動画面の表示. Android 14 以降は許可されていないことがあり, ないときはヘッドアップ通知で鳴動します.
- 他のアプリの上に重ねて表示 (`SYSTEM_ALERT_WINDOW`, 任意): システムが全画面通知を表示しないとき (例えば ColorOS でバナー通知がオフのとき) に鳴動画面を開きます. 必要な場合に権限ページからユーザーが許可します.
- 起動時の受信 (`RECEIVE_BOOT_COMPLETED`): 再起動後に再スケジュールし, 初回のロック解除前でも鳴動できるようにします.
- フォアグラウンドサービス (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`): 鳴動中に音を出し続けます. 設定の "バックグラウンドで動作し続ける" がオンのときは常駐にも使います (既定はオフ).
- バイブレーション, ウェイクロック, 音声設定 (`VIBRATE`, `WAKE_LOCK`, `MODIFY_AUDIO_SETTINGS`): 振動, 鳴動中に端末を起こしておくこと, 鳴動後の独立したアラーム音量の復元.
- 実行中のアプリの順序変更 (`REORDER_TASKS`, Android 7.0 - 8.1 のみ): 他のアプリやスクリプトがほぼ同時に複数のアラーム要求を送ったとき, システムが起動しなかった要求を前面に移して処理し, すべての要求が処理されるようにします. インストール時に付与されます.
- ネットワーク (`INTERNET`, `ACCESS_NETWORK_STATE`): 更新確認と祝日データの更新のみ. 更新確認はアラーム一覧を開いたときに設定した頻度で行い, 祝日データは週に 1 回確認します. どちらも従量制ネットワークでは既定で行いません. スケジュール, 鳴動, AutoJs6 との連携ではネットワークを使いません.
- AutoJs6 のプラグイン権限 (`org.autojs.permission.PLUGIN`): AutoJs6 との通信.

プラグインは必要な権限をできるだけ少なくしています:

- 保護された入口: 機能サービスと有効化の入口は AutoJs6 のプラグイン権限 (`org.autojs.permission.PLUGIN`) で保護されており, 他のアプリから直接呼び出すことはできません. システムの時計リクエストを受け付ける入口は, 呼び出し元に `com.android.alarm.permission.SET_ALARM` を要求します.
- ネットワーク: プラグインの更新確認と祝日データ (GitHub に接続, どちらの自動確認も設定画面でオフにできます) にのみ使い, ダウンロードした祝日データは署名を検証します. アラームや個人データはアップロードしません.
- ストレージ: アラームはプラグインの専用ディレクトリに保存され, クラウドバックアップの対象外です. ローカルの着信音はシステムのファイル選択画面から読み込み, ストレージ権限は要求しません.
- スクリプトの権限: スクリプトは `alarms` モジュールを通じて, プラグイン画面で作成したものを含むすべてのアラームとタイマーを読み書きできます. 登録されたスクリプトは AutoJs6 に保存され, プラグイン画面はアラームにスクリプトが紐付いていることを示し, その解除だけができます.

******

### プラグインインターフェース

******

以下の情報は AutoJs6 ホストとプラグイン開発者向けです. ホストはこれらの識別子でプラグインを検出し, 機能ネゴシエーションを行います:

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

### 開発ロードマップ

******

計画, 設計上の決定, 進捗は ROADMAP.md のチェックリストで管理しています. 未チェックの項目は計画であり, 現在のバージョンの機能ではありません.

- [ROADMAP.md を見る](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v1.3.0

_2026/11/14_

- `ヒント` プラグイン画面で設定した停止チャレンジには AutoJs6 は不要です. スクリプトでの停止チャレンジの設定 (アラームの dismiss オプション) とスクリプト条件には AutoJs6 6.8.1 (内部バージョン 6641) 以降と, このプラグインの 1.3.0 以降が必要です. プラグインが 1.2.x 以前のままだと dismiss を指定した呼び出しはエラーになり, プラグインの更新を求めるメッセージが出ます
- `ヒント` 停止チャレンジ付きのアラームも必ず止められます. 自動停止 (個別に設定していなければ 30 分以上) と緊急停止 (鳴動画面で 4 秒以内に音量ボタンを 5 回押す) は常に使えます. 緊急停止は設定画面でオフにでき, オフにするとチャレンジを完了するまでは自動停止でしか鳴動を終えられません
- `機能` 停止チャレンジ: アラームを止めるには計算問題に正解する (やさしい, ふつう, むずかしいの 3 段階, 既定は 3 問), スマートフォンを振る (既定は 30 回), 文を入力する (自分で決めた文, または内蔵の文からランダムに 1 つ) のいずれかが必要になるよう設定でき, チャレンジ中にスヌーズを許可するかどうかも選べます. 編集画面の "停止チャレンジ" 行で設定し, 鳴動画面ではスライダーの代わりにチャレンジが表示されます
- `機能` チャレンジが有効な間は, 通知の "停止" ボタンや他のアプリからの停止リクエストでアラームが直接止まらず, 鳴動画面のチャレンジが開きます. スヌーズを許可しないときは通知に "スヌーズ" ボタンが出ません. 加速度センサーのないデバイスでは, 振るチャレンジが計算問題になります
- `機能` 安全弁: チャレンジ付きのアラームで自動停止を個別に設定していなければ, 自動停止の時間は設定画面の既定値と 30 分の長いほうになり, 残り時間は鳴動画面に表示されます. 鳴動画面が前面にある間 (ロック画面上でも) 4 秒以内に音量ボタンを 5 回押すと緊急停止でき, チャレンジ中の音量ボタンは回数を数えるだけで音量は変わりません. 設定画面に "停止チャレンジ" グループを追加し, 緊急停止をオフにできます (既定はオン)
- `機能` スクリプト条件: AutoJs6 のスクリプトが設定し, アラームはスクリプトが止めるまで鳴り続けます. 再起動後にまだロックを解除していない, スクリプトがない, AutoJs6 を使えない, スクリプトを起動できなかった, スクリプトが終了した, のいずれかのときは, すぐに指定した代替のチャレンジ (既定は通常の停止) に切り替わり, 理由はプラグインのログに記録されます
- `機能` AutoJs6 のスクリプトモジュールにアラームの dismiss オプション (チャレンジの種類, 計算問題の難しさと問題数, 振る回数, 入力する文, スクリプトを実行できないときに使うチャレンジ, スヌーズの許可), alarms.ringing() と ring / dismiss イベントの challenge, dismiss イベントの by: 'challenge' を追加しました (AutoJs6 6.8.1 が必要; [AutoJs6 ドキュメント](https://docs.autojs6.com/#/alarms) を参照)
- `機能` プラグインツール alarm_add と alarm_update が dismiss オプションに対応し, 計算問題, 振る, テキスト入力の 3 種類のチャレンジを設定できます (スクリプト条件を除く)

#### v1.2.0

_2026/11/13_

- `ヒント` スクリプトの alarms.holidays, アラームの holidays オプション, beforeRing イベントには AutoJs6 6.8.1 (内部バージョン 6641) 以降と, このプラグインの 1.2.0 以降が必要です. プラグインが 1.1.x 以前のままだと呼び出しはエラーになり, プラグインの更新を求めるメッセージが出ます
- `ヒント` 祝日と振替出勤日の正確さは上流のデータ (オープンソースプロジェクト python-holidays) に完全に依存します. まだ公表されていない年はルールから推定して "推定" と表示し, 公表後のデータ更新で修正されます
- `機能` 祝日: アラームに "祝日は鳴らさない", "祝日のみ鳴らす", "勤務日のみ鳴らす" の 3 つの選択肢を追加 (勤務日のみ鳴らす = 振替出勤日を含むすべての勤務日に鳴り, 週末と祝日は鳴らない). 250 の国と地域 (600 以上の下位地域を含む) の 2020 - 2035 年の祝日データを内蔵し, 中国本土と台湾は振替出勤日も含みます
- `機能` 設定画面で既定の祝日地域を選べます (最初はシステムの地域から推定). アラームごとに地域を指定することもできます. アラーム一覧には次の鳴動までにスキップされる祝日の数を表示します. 推定の年は "推定" と表示し, 振替休日や振替出勤日がありうることを知らせます
- `機能` 祝日データの更新は毎週自動で確認し (設定画面でオフにでき, 既定では従量制ネットワークでダウンロードしません), 設定画面から手動で確認することもできます. ダウンロードしたデータは署名を検証し, 検証に失敗したときは元のデータを使い続けます. データのない年はアラームが繰り返しルールだけで鳴り, そのために鳴らなくなることはありません
- `機能` 鳴動前のスクリプトによる判断 (beforeRing): 登録したスクリプトが鳴動の前 (既定 60 秒, 15 秒から 5 分まで) に起動され, 今回の鳴動をスキップするか, 今回だけ別のラベル, アラーム音, 読み上げテキスト, バイブレーションで鳴らせます. スクリプトが時間内に決めなかったとき, エラーになったとき, AutoJs6 が使えないときは通常どおり鳴ります
- `機能` AutoJs6 のスクリプトモジュールに alarms.holidays (地域の一覧, 祝日と勤務日の判定, 年間の祝日の一覧, データの確認と更新), アラームの holidays オプション, beforeRing イベントの skip / ring を追加し, alarms.capabilities() の warnings でデータの不足や推定の年を知らせます (AutoJs6 6.8.1 が必要; [AutoJs6 ドキュメント](https://docs.autojs6.com/#/alarms) を参照)
- `機能` プラグインツール holiday_check を追加. MCP Server プラグインや plugins.tools.call から, ある日がある地域で祝日か勤務日か, 祝日の名前を調べられます. alarm_add と alarm_update は holidays オプションに対応しました

#### v1.1.0

_2026/11/12_

- `ヒント` スクリプトの alarms.timers と alarms.stopwatch には AutoJs6 6.8.1 (内部バージョン 6641) 以降と, このプラグインの 1.1.0 以降が必要です. プラグインが 1.0.x のままだと呼び出しはエラーになり, プラグインの更新を求めるメッセージが出ます
- `機能` タイマー: 最大 20 個のタイマーを同時に動かせます. 時間はプリセット (1 分から 1 時間) かカスタム (1 秒から 100 時間) で選び, ラベルも付けられます. 一時停止, 再開, 1 分追加, リセットができ, 時間になるとアラームと同じように鳴ります
- `機能` 動作中のタイマーは通知領域にカウントダウンを表示し, 通知から直接一時停止, 1 分追加, キャンセルができます. 再起動後もタイマーはカウントを続け, 電源オフの間に時間になったタイマーは起動後に鳴ります (5 分を超えて過ぎていた場合は逃した通知だけを残します)
- `機能` ストップウォッチ: 開始, 一時停止, 再開, リセット, 最大 100 周のラップ記録, 最速と最遅のラップの表示. 動作中は通知領域に表示され, 設定の "タイマーとストップウォッチ" グループでオフにできます
- `機能` 下部のナビゲーションでアラーム, タイマー, ストップウォッチの 3 つの画面を切り替えます
- `機能` AutoJs6 のスクリプトモジュールに alarms.timers と alarms.stopwatch を追加 (AutoJs6 6.8.1 が必要): タイマーの開始, 取得, 一時停止, 再開, 時間の追加と短縮, リセット, キャンセル, 鳴動などのイベント発生時に起動するスクリプトのタイマーへの登録, ストップウォッチの操作とラップの記録 ([AutoJs6 ドキュメント](https://docs.autojs6.com/#/alarms) を参照)
- `機能` プラグインツール timer_start, timer_list, timer_control を追加. MCP Server プラグインや plugins.tools.call からタイマーの開始, 一覧, 一時停止, 再開, 時間の追加, リセット, キャンセルができます
- `機能` システムの時計との連携でタイマーの要求も処理します: 他のアプリからプラグインのタイマーを開始し, タイマー画面を開き, 鳴動中のタイマーを停止できます

##### その他のリリース履歴は以下を参照

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/assets/doc/CHANGELOG-ja.md)

******

### ビルド

******

この節はソースからプラグインをビルドしたい開発者向けです.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### ライセンス

******

プロジェクトのコードは [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE) で公開されています. サードパーティのコンポーネントとデータについてはサードパーティ通知を参照してください. 祝日データはオープンソースプロジェクト python-holidays (MIT ライセンス) から生成しており, ライセンス全文と貢献者の一覧はデータとともに公開されています.

- [サードパーティの表示 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/THIRD_PARTY_NOTICES.md)

******

### 関連リンク

******

- AutoJs6 プロジェクト: https://github.com/SuperMonster003/AutoJs6
