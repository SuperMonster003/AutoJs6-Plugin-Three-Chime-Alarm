<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>カレンダー形式の繰り返しルール, 読み上げ付きの鳴動, 鳴動時のスクリプト連携に対応したアラーム</p>

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

3-Chime Alarm は AutoJs6 のアラームプラグインで, AutoJs6 がなくても単独で使えるアラームアプリでもあります. アラームは RFC 5545 のカレンダールールで繰り返し (例えば毎月最後の金曜日, ある日から 3 日おき), 鳴動時にテキストを読み上げたり, スクリプトを起動したり, 実行中のスクリプトに通知したりできます. タイマーとストップウォッチ, 祝日スキップ, 停止チャレンジは今後のバージョンで追加されます.

プラグインは Android の正確なアラームで自らスケジュールして鳴動するため, AutoJs6 が動いていなくてもアラームは時間どおりに鳴ります. AutoJs6 のスクリプトはグローバルモジュール `alarms` でプラグインのアラームを照会, 作成, 変更, 監視します. サブ名前空間 `alarms.system` はシステムの時計アプリを直接呼び出し, プラグインがなくても使えます. プラグインは他のアプリからのアラーム設定リクエストも受け付けます.

> 1.0.0 ではアラーム, プラグイン画面, プラグインツール, `alarms` スクリプトモジュールを提供します. プラグインセンターでの有効化, プラグインツール, `alarms` モジュールには AutoJs6 6.8.1 (内部バージョン 6641) 以降が必要です. それより前の AutoJs6 (6.8.0 を含む) では, プラグインは単独のアラームアプリとしてのみ使えます. タイマー, ストップウォッチ, 祝日, 停止チャレンジの進捗は開発ロードマップを参照してください.

******

### 機能ハイライト

******

- アラーム: 単発または繰り返しのアラーム. 繰り返しルールは平日, 毎月第 n 何曜日, 月末, 日数間隔, 終了日と回数に対応し, 特定の日を除外したり次の鳴動だけをスキップしたりできます.
- 鳴動: システムの着信音または取り込んだローカル音声, テキストの読み上げ, 独立した音量とフェードイン, カスタムバイブレーション, スヌーズと自動停止, ロック画面での全画面鳴動画面; 同時に鳴るアラームは順番に鳴動します.
- 時間どおり: 再起動, 時刻やタイムゾーンの変更, プラグインの更新の後に自動で再スケジュールし, 再起動後のロック解除前でも鳴動します. 電源オフで逃したアラームは 5 分以内なら鳴動し, それを過ぎると通知を残します.
- スクリプト連携: アラームにスクリプトファイルを登録すると, 鳴動, スヌーズ, 停止などのイベントで AutoJs6 が起動するため, スクリプトを常駐させる必要はありません. 実行中のスクリプトもこれらのイベントをリアルタイムで監視できます.
- プラグインツール: MCP Server プラグインや `plugins.tools.call` からアラームの作成, 取得, 変更, 削除と鳴動中のアラームの操作ができます (`alarm_*` ツール 9 個).
- システムの時計との連携: 他のアプリからのアラームの設定, 表示, 停止, スヌーズの要求を処理できます.
- 画面: 言語, ダークモード, テーマカラーは既定で AutoJs6 に従い, 個別にも設定できます. ランチャーアイコンを切り替えられ, 設定画面で着信音, バイブレーション, スヌーズ, 自動停止の既定値を設定できます.
- タイマーとストップウォッチ (1.1.0 で予定): 複数のタイマーの並行動作, 一時停止, 再開, 時間の追加; ストップウォッチのラップ.
- 祝日 (1.2.0 で予定): 複数地域のオープンな祝日データを内蔵し, 祝日は鳴動をスキップし, 振替出勤日は通常どおり鳴動します. `beforeRing` スクリプトフックで鳴動するかどうかを決めることもできます.
- 停止チャレンジ (1.3.0 で予定): 計算問題, 振る動作, 文の入力, スクリプト条件. どれも安全弁付きで, アラームは必ず止められます.

******

### スクリーンショット

******

Sony XQ-DQ72 で撮影したスクリーンショットです. 上段がライトテーマ, 下段がダークテーマです.

| アラーム一覧 | アラームの編集 | 鳴動 | 設定 |
| :---: | :---: | :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-light.png?raw=true" alt="list-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-light.png?raw=true" alt="edit-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-light.png?raw=true" alt="ring-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-light.png?raw=true" alt="settings-light" width="200" /> |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-dark.png?raw=true" alt="list-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-dark.png?raw=true" alt="edit-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-dark.png?raw=true" alt="ring-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-dark.png?raw=true" alt="settings-dark" width="200" /> |

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

alarms.on("ring", (e) => toast(e.label));

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

- `alarms.add(options)` はアラームを作成してそのスナップショットを返します. `alarms.list`, `get`, `update`, `remove`, `setEnabled`, `skipNext`, `cancelSkip` で, プラグイン画面で作ったものを含むすべてのアラームを管理できます.
- `repeat` には RRULE 文字列 (例えば `FREQ=MONTHLY;BYDAY=-1FR`), 曜日番号の配列, 構造化オブジェクトを指定できます. `alarms.preview(options)` は次の鳴動時刻を一覧にするので, ルールの確認に使えます.
- `alarms.bind(id, script, options)` (またはアラームスナップショットの `bind`) はイベント発生時に AutoJs6 が起動するスクリプトを登録し, スクリプトはイベントデータを `alarms.currentEvent()` で読み取ります. `alarms.on('ring', listener)` はスクリプトの実行中にリアルタイムで監視します.
- `alarms.ringing`, `dismiss`, `snooze` は鳴動中のアラームを操作し, `alarms.ringtones.list()` は使える着信音を一覧にします.
- `alarms.system` はシステムの時計アプリを直接呼び出し (`setAlarm`, `setTimer`, `showAlarms`, `dismissAlarm`, `next` など), プラグインは不要です.

******

### システムの時計との連携

******

プラグインはシステムのアラームアプリとして, 他のアプリ (カレンダー, 音声アシスタント, 自動化ツールなど) からの標準の時計の要求を処理できます. 要求するアプリには `com.android.alarm.permission.SET_ALARM` 権限が必要です:

- アラームの設定 (`SET_ALARM`): 時刻, ラベル, 曜日, バイブレーション, 着信音を読み取ります. 画面を省略する要求ならそのまま保存し, それ以外は値を入力済みの編集画面を開きます.
- アラームの表示 (`SHOW_ALARMS`): アラーム一覧を開きます.
- アラームの停止 (`DISMISS_ALARM`): 鳴動中またはスヌーズ中のアラームを終了します. 鳴動中のアラームがなければ, 24 時間以内に鳴るアラームのその回をスキップします. 複数のアラームが該当するときはユーザーが選びます.
- スヌーズ (`SNOOZE_ALARM`): 鳴動中のアラームをスヌーズします.
- 逆方向の `alarms.system`: スクリプトはこれでシステムの既定の時計アプリ (このプラグインまたは他の時計アプリ) に要求を送り, プラグインは不要です. メーカーごとの時計アプリの違いは AutoJs6 ドキュメントを参照してください.

******

### 互換性

******

プラグインは Android 7.0 (API 24) 以降に対応します. プラグインセンターでの有効化, プラグインツール, `alarms` モジュールには AutoJs6 6.8.1 (内部バージョン 6641) 以降が必要です. それより前の AutoJs6 (例えば内部バージョン 6640 の 6.8.0) では, プラグインセンターは新しい AutoJs6 が必要だと表示してプラグインを有効にできず, プラグインツールと `alarms` モジュールは使えませんが, プラグインは単独のアラームアプリとして使え, システムの時計の要求も通常どおり処理します. `alarms` モジュールは Rhino エンジンのみに対応し, その中の `alarms.system` はプラグインが不要です.

1.0.0 のリリース前に次のデバイスで検証しました (データは開発ロードマップの付録 D):

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

アラームは鳴ります. プラグインは初回のロック解除前でも動作します. 連携スクリプトは起動しません. AutoJs6 は初回のロック解除前にスクリプトを実行できず, ロック解除後にもあとから起動することはありません.

******

### 権限とセキュリティ

******

プラグインが要求する権限とその用途:

- 正確なアラーム (`USE_EXACT_ALARM`, Android 12 と 12L では `SCHEDULE_EXACT_ALARM`): 時間どおりの鳴動. Android 13 以降はインストール時に許可され, Android 12 と 12L ではシステム設定で "アラームとリマインダー" を許可します.
- 通知 (`POST_NOTIFICATIONS`): 鳴動中のアラームと逃したアラームの表示. ないとアラームは鳴りますが, 通知も鳴動画面も表示されません.
- 全画面通知 (`USE_FULL_SCREEN_INTENT`): ロック画面への鳴動画面の表示. Android 14 以降は許可されていないことがあり, ないときはヘッドアップ通知で鳴動します.
- 他のアプリの上に重ねて表示 (`SYSTEM_ALERT_WINDOW`, 任意): システムが全画面通知を表示しないとき (例えば ColorOS でバナー通知がオフのとき) に鳴動画面を開きます. 必要な場合に権限ページからユーザーが許可します.
- 起動時の受信 (`RECEIVE_BOOT_COMPLETED`): 再起動後に再スケジュールし, 初回のロック解除前でも鳴動できるようにします.
- フォアグラウンドサービス (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`): 鳴動中に音を出し続けます. 設定の "バックグラウンドで動作し続ける" がオンのときは常駐にも使います (既定はオフ).
- バイブレーション, ウェイクロック, 音声設定 (`VIBRATE`, `WAKE_LOCK`, `MODIFY_AUDIO_SETTINGS`): 振動, 鳴動中に端末を起こしておくこと, 鳴動後の独立したアラーム音量の復元.
- 実行中のアプリの順序変更 (`REORDER_TASKS`, Android 7.0 - 8.1 のみ): 他のアプリやスクリプトがほぼ同時に複数のアラーム要求を送ったとき, システムが起動しなかった要求を前面に移して処理し, すべての要求が処理されるようにします. インストール時に付与されます.
- ネットワーク (`INTERNET`, `ACCESS_NETWORK_STATE`): 設定画面の更新確認のみ (1.2.0 からは祝日データの更新にも使用). 確認はアラーム一覧を開いたときに設定した頻度でのみ行い, 従量制ネットワークでは既定で行いません. スケジュール, 鳴動, AutoJs6 との連携ではネットワークを使いません.
- AutoJs6 のプラグイン権限 (`org.autojs.permission.PLUGIN`): AutoJs6 との通信.

プラグインは必要な権限をできるだけ少なくしています:

- 保護された入口: 機能サービスと有効化の入口は AutoJs6 のプラグイン権限 (`org.autojs.permission.PLUGIN`) で保護されており, 他のアプリから直接呼び出すことはできません. システムの時計リクエストを受け付ける入口は, 呼び出し元に `com.android.alarm.permission.SET_ALARM` を要求します.
- ネットワーク: プラグインの更新確認 (GitHub に接続, 自動確認は設定画面でオフにできます) にのみ使い, アラームや個人データはアップロードしません.
- ストレージ: アラームはプラグインの専用ディレクトリに保存され, クラウドバックアップの対象外です. ローカルの着信音はシステムのファイル選択画面から読み込み, ストレージ権限は要求しません.
- スクリプトの権限: スクリプトは `alarms` モジュールを通じて, プラグイン画面で作成したものを含むすべてのアラームを読み書きできます. 登録されたスクリプトは AutoJs6 に保存され, プラグイン画面はアラームにスクリプトが紐付いていることを示し, その解除だけができます.

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

#### v1.0.1

_2026/11/08_

- `修正` 他のアプリや AutoJs6 のスクリプトが同じ種類のアラーム要求を短い間隔で続けて送ったとき (例えばアラームを続けて 2 つ作成する, このプラグインを指定して alarms.system.setAlarm を続けて呼び出す), プラグインの画面が開いていなければ 2 つ目の要求が何の表示もなく無視され, アラームが作成されませんでした. 現在はすべての要求が処理されます. Android 7.0 - 8.1 では, このために "実行中のアプリの順序変更" の権限 (インストール時に自動で付与) を使います

#### v1.0.0

_2026/11/07_

- `ヒント` プラグインセンターでの有効化, プラグインツール, alarms スクリプトモジュールには AutoJs6 6.8.1 (内部バージョン 6641) 以降が必要です. それより前の AutoJs6 (例えば 6.8.0) では, プラグインは単独のアラームアプリとしてのみ使えます
- `ヒント` ColorOS (OPPO, OnePlus, realme) では, アプリ情報と 設定 > アプリ > 自動起動 の両方でプラグインの自動起動をオンにしてください. オフのままだと, プラグインのプロセスが整理された後にアラームが鳴りません (README > よくある質問 を参照)
- `機能` アラーム: 単発または繰り返しのアラーム. 繰り返しは RFC 5545 のカレンダールール (平日, 毎月第 n 何曜日, 月末, 日数間隔, 終了日と回数) に従い, 日付の除外や次の 1 回だけのスキップができます
- `機能` 鳴動: システムの着信音または取り込んだローカル音声, テキストの読み上げ, 独立した音量とフェードイン, カスタムバイブレーション, スヌーズと自動停止, ロック画面の全画面鳴動画面. 同時に鳴るアラームは順番に鳴動します
- `機能` スケジュール: 再起動, 時刻やタイムゾーンの変更, プラグインの更新の後に自動で再スケジュールし, 初回のロック解除前でも鳴動します. 逃したアラームは 5 分以内なら鳴動し, それを過ぎると通知を残します
- `機能` プラグイン画面: アラーム一覧, 編集画面, 鳴動画面, 権限ページ, 設定. 言語, ダークモード, テーマカラーは既定で AutoJs6 に従い, ランチャーアイコンを切り替えられます
- `機能` 設定: 着信音, バイブレーション, スヌーズ, 自動停止の既定値, 時刻の形式, 更新の自動確認, 任意の "バックグラウンドで動作し続ける" (サイレント通知を常駐させ, システムに整理されにくくします)
- `機能` 権限ページで通知, 正確なアラーム, 全画面通知, 電池の最適化, バックグラウンド制限, メーカーの自動起動を順に確認でき, ColorOS では自動起動リストとバナー通知の案内も表示します
- `機能` AutoJs6 のスクリプトモジュール alarms / $alarms (AutoJs6 6.8.1 が必要, Rhino のみ): アラームの作成, 取得, 変更, 削除, イベントの監視, イベント発生時に AutoJs6 が起動するスクリプトの登録, 着信音の一覧 ([AutoJs6 ドキュメント](https://docs.autojs6.com/#/alarms) を参照)
- `機能` プラグインツール alarm_add, alarm_list, alarm_update, alarm_remove, alarm_set_enabled, alarm_skip_next, alarm_upcoming, alarm_dismiss, alarm_snooze. MCP Server プラグインや plugins.tools.call から呼び出せます
- `機能` システムの時計との連携: 他のアプリからのアラームの設定, 表示, 停止, スヌーズの要求を処理します
- `機能` すべての端末で使える APK は 1 つだけで, 16 KB メモリページの端末にも対応します
- `依存関係` AndroidX Core KTX 1.15.0 を追加
- `依存関係` AndroidX Activity Compose 1.12.2 を追加
- `依存関係` Jetpack Compose BOM 2026.09.00 を追加 (プラグイン画面)
- `依存関係` Kotlin Coroutines Android 1.11.0 を追加
- `依存関係` Material Color Utilities 4.1.1 を追加 (テーマカラー)
- `依存関係` lib-recur 0.17.1 を追加 (アラームの繰り返しルールを計算)
- `依存関係` Desugar JDK Libs 2.1.5 を追加 (Android 7.x でも日付とタイムゾーンの計算を可能にする)

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

プロジェクトのコードは [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE) で公開されています. サードパーティのコンポーネントとデータについてはサードパーティ通知を参照してください.

- [サードパーティの表示 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/THIRD_PARTY_NOTICES.md)

******

### 関連リンク

******

- AutoJs6 プロジェクト: https://github.com/SuperMonster003/AutoJs6
