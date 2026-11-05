<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>カレンダー形式の繰り返しルール, 祝日スキップ, 鳴動時のスクリプト連携に対応したアラーム, タイマー, ストップウォッチ</p>

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

3-Chime Alarm は AutoJs6 のアラームプラグインで, AutoJs6 がなくても使える本格的な時計アプリでもあります: アラーム, 同時に動かせる複数のカウントダウンタイマー, ストップウォッチを備えています. アラームは RFC 5545 のカレンダー規則で繰り返せ (毎月最後の金曜日, ある日から 3 日ごとなど), 祝日をスキップでき, 鳴動時にスクリプトを起動したり実行中のスクリプトに通知したりできます.

プラグインは Android の正確なアラームで自らスケジュールして鳴動するため, AutoJs6 が動いていなくてもアラームは時間どおりに鳴ります. AutoJs6 のスクリプトはグローバルモジュール `alarms` でプラグインのアラームを照会, 作成, 変更, 監視します. サブ名前空間 `alarms.system` はシステムの時計アプリを直接呼び出し, プラグインがなくても使えます. プラグインは他のアプリからのアラーム設定リクエストも受け付けます.

> 現在は開発プレビュー版です: AutoJs6 のプラグインセンターはプラグインを検出できますが, アラーム, タイマー, ストップウォッチ, プラグイン画面, `alarms` スクリプトモジュールはまだ提供されていません. アイコンも候補のデザインで, 今後変わる可能性があります. 以下は各バージョンの設計目標です. 進捗はロードマップを参照してください.

******

### 機能ハイライト

******

- アラーム: 一回限りまたは繰り返しのアラーム. 繰り返し規則は平日, 毎月第 n 何曜日, 月末, 日数間隔, 終了日, 回数に対応し, 次回の鳴動だけをスキップすることもできます.
- 鳴動: システムの着信音またはローカル音声, テキスト読み上げ, 個別の音量と段階的な音量上昇, カスタム振動パターン, ロック画面での全画面通知, スヌーズ.
- スクリプト連携: アラームにスクリプトファイルを登録すると鳴動時に AutoJs6 が起動するため, スクリプトを常駐させる必要はありません. 実行中のスクリプトは鳴動, スヌーズ, 停止のイベントをリアルタイムで監視することもできます.
- タイマーとストップウォッチ (1.1.0): 複数のタイマーを同時に動かし, 一時停止, 再開, 延長ができ, 再起動後も復元されます. ストップウォッチはラップを記録します.
- 祝日 (1.2.0): 多地域のオープンな祝日データを内蔵し, 祝日はスキップし振替出勤日は通常どおり鳴らします. `beforeRing` スクリプトフックで鳴らすかどうかを決めることもできます.
- 停止チャレンジ (1.3.0): 計算問題, 端末を振る, 文の入力, スクリプト条件. いずれもアラームを必ず止められるよう安全弁があります.

******

### 使用方法

******

1. [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases) から APK をダウンロードしてインストールします. パッケージはすべての ABI 共通の 1 つだけです.
2. プラグインを開き, 求められたら通知, 正確なアラーム, 全画面通知の権限を許可します. あとは普通の時計アプリとして使えます.
3. スクリプトで `alarms` モジュールを使うには, AutoJs6 を内部バージョン 6640 (6.8.1) 以降に更新し, プラグインセンターでプラグインを有効にします.

> プラグインは Android 7.0 (API 24) 以降の端末に対応しています. APK にはネイティブライブラリが含まれないため, パッケージは 1 つだけで, 16 KB メモリページサイズの検査も対象外です.

******

### スクリプト API

******

スクリプトはグローバルモジュール `alarms` (`$alarms` とも書けます) からこれらの機能を使います (1.0.0 の設計草案です. リリース版のドキュメントが優先されます):

```javascript
let a = alarms.add({ time: "07:30", repeat: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", label: "Wake up" });
a.bind("./after-wake.js", { events: ["dismiss"] });

alarms.add({ time: "18:00", repeat: "FREQ=MONTHLY;BYDAY=-1FR", label: "Monthly report" });
alarms.add({ time: "06:10", date: "2026-11-04", repeat: { freq: "daily", interval: 3 }, tag: "shift" });

alarms.on("ring", (e) => toast(e.label));

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

- `alarms.add(options)` はアラームを作成してアラームオブジェクトを返します. `alarms.list`, `get`, `update`, `remove`, `setEnabled`, `skipNext` はプラグイン画面で作成したものを含むすべてのアラームを管理します.
- `repeat` は RRULE 文字列 (`FREQ=MONTHLY;BYDAY=-1FR` など) または構造化オブジェクトを受け付けます. `alarms.preview(options)` は今後の鳴動時刻を一覧にし, 規則の確認に使えます.
- `alarm.bind(script, { events })` は鳴動時に AutoJs6 が起動するスクリプトを登録し, イベントは `alarms.currentEvent()` で取得できます. `alarms.on('ring', listener)` はスクリプトの実行中に監視します.
- `alarms.system` はシステムの時計アプリを直接呼び出し (`setAlarm`, `setTimer`, `showAlarms`, `next` など), プラグインがなくても使えます.

******

### よくある質問

******

#### AutoJs6 なしで使えますか?

はい. プラグインには時計の画面一式があり, 単体でアラーム, タイマー, ストップウォッチとして使えます. AutoJs6 はスクリプトでアラームを管理したり連携したりするときだけ必要です.

#### AutoJs6 が動いていなくてもアラームは鳴りますか?

はい. アラームはプラグインが Android の正確なアラームでスケジュールして鳴らすため, AutoJs6 のプロセスには依存しません. スクリプトが登録されている場合は, 鳴動時にプラグインが AutoJs6 を起こしてスクリプトを起動します.

#### 正確なアラームと全画面通知の権限が必要なのはなぜですか?

正確なアラームは鳴動時刻を正確に保ち (アラームアプリはインストール時にこの権限を得ます), 全画面通知はロック画面に鳴動画面を表示し, 通知は鳴動中のアラームを表示します. システムがいずれかを取り消した場合は, プラグインの権限ページで再び有効にする方法を案内します.

******

### 権限とセキュリティ

******

プラグインは必要な権限をできるだけ少なくしています:

- 保護された入口: 機能サービスと有効化の入口は AutoJs6 のプラグイン権限 (`org.autojs.permission.PLUGIN`) で保護されており, 他のアプリから直接呼び出すことはできません. システムの時計リクエストを受け付ける入口は, 呼び出し元に `com.android.alarm.permission.SET_ALARM` を要求します.
- ネットワーク: 1.0.0 はネットワーク権限を要求せず, アラーム, タイマー, ストップウォッチはすべて端末内で動作します.
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
minimum host build: 6640
native libraries: none (a single APK for every ABI; the 16 KB page size check does not apply)
```

******

### 開発ロードマップ

******

計画, 設計上の決定, 進捗は ROADMAP.md のチェックリストで管理しています. 未チェックの項目は計画であり, 現在のバージョンの機能ではありません.

- [ROADMAP.md を見る](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v1.0.0

_2026/11/05_

- `ヒント` 開発プレビュー: AutoJs6 のプラグインセンターはプラグインを検出できます. アラーム, タイマー, ストップウォッチ, プラグイン画面, alarms スクリプトモジュールはまだ提供されていません
- `ヒント` AutoJs6 6.8.1 (6640) 以降が必要です (正確な最低バージョンはホスト側の変更が反映された後に記入されます)
- `ヒント` 現在のアイコンは候補のデザインで, 今後変わる可能性があります
- `機能` プラグインの骨組み: Wake 有効化プロトコル, プラグインセンターの識別サービス, アラーム機能サービス
- `機能` ネイティブライブラリを含まず, すべての端末で使える APK は 1 つだけです
- `機能` 10 言語の README, プラグインセンターの説明, 変更履歴を JSON ソースから一括生成
- `依存関係` AndroidX Core KTX 1.15.0 を追加
- `依存関係` lib-recur 0.17.1 を追加 (アラームの繰り返しルールを計算)

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
