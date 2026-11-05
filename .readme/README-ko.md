<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>달력 반복 규칙, 공휴일 건너뛰기, 알람이 울릴 때 스크립트 연동을 지원하는 알람, 타이머 및 스톱워치</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 언어 (Languages)

******

현재 README.md 는 다음 언어를 지원합니다:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ja.md)
- 한국어 [ko] # 현재
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ar.md)

******

### 소개

******

3-Chime Alarm은 AutoJs6의 알람 플러그인이자 AutoJs6 없이도 쓸 수 있는 완전한 시계 앱입니다: 알람, 동시에 여러 개를 돌릴 수 있는 카운트다운 타이머, 스톱워치를 제공합니다. 알람은 RFC 5545 달력 규칙으로 반복되며 (예: 매월 마지막 금요일, 특정 날짜부터 3일마다), 공휴일을 건너뛸 수 있고, 울릴 때 스크립트를 실행하거나 실행 중인 스크립트에 알릴 수 있습니다.

플러그인은 Android의 정확한 알람으로 직접 일정을 잡고 알람을 울리므로, AutoJs6가 실행 중이 아니어도 알람이 제시간에 울립니다. AutoJs6 스크립트는 전역 모듈 `alarms`로 플러그인의 알람을 조회, 생성, 변경, 감시합니다. 하위 네임스페이스 `alarms.system`은 시스템 시계 앱을 직접 호출하며 플러그인 없이도 쓸 수 있습니다. 플러그인은 다른 앱의 알람 설정 요청도 받습니다.

> 현재는 개발 미리보기 버전입니다: AutoJs6 플러그인 센터가 플러그인을 인식할 수 있지만, 알람, 타이머, 스톱워치, 플러그인 화면, `alarms` 스크립트 모듈은 아직 제공되지 않으며, 아이콘도 후보 디자인이라 이후 바뀔 수 있습니다. 아래 내용은 각 버전의 설계 목표이며, 진행 상황은 로드맵을 참고하세요.

******

### 기능 하이라이트

******

- 알람: 일회성 또는 반복 알람. 반복 규칙은 평일, 매월 n번째 요일, 월말, 일 간격, 종료일, 횟수를 지원하며, 다음 한 번만 건너뛸 수도 있습니다.
- 울림: 시스템 벨소리 또는 로컬 오디오, 텍스트 읽어 주기, 별도 음량과 점점 커지는 소리, 사용자 지정 진동, 잠금 화면의 전체 화면 알림, 다시 알림.
- 스크립트 연동: 알람에 스크립트 파일을 등록하면 울릴 때 AutoJs6가 실행하므로 스크립트를 계속 켜 둘 필요가 없습니다. 실행 중인 스크립트는 울림, 다시 알림, 끄기 이벤트를 실시간으로 감시할 수도 있습니다.
- 타이머와 스톱워치 (1.1.0): 여러 타이머를 동시에 돌리고 일시정지, 재개, 연장할 수 있으며 재부팅 후에도 복원됩니다. 스톱워치는 랩을 기록합니다.
- 공휴일 (1.2.0): 여러 지역의 공개 공휴일 데이터를 내장해 공휴일에는 건너뛰고 대체 근무일에는 그대로 울립니다. `beforeRing` 스크립트 훅으로 울릴지 정할 수도 있습니다.
- 끄기 도전 (1.3.0): 계산 문제, 흔들기, 문장 따라 입력, 스크립트 조건. 모두 안전장치가 있어 알람을 언제든 끌 수 있습니다.

******

### 사용 방법

******

1. [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases)에서 APK를 내려받아 설치합니다. 모든 ABI에 공통인 패키지 하나만 있습니다.
2. 플러그인을 열고 요청이 나오면 알림, 정확한 알람, 전체 화면 알림 권한을 허용합니다. 그다음에는 일반 시계 앱처럼 쓸 수 있습니다.
3. 스크립트에서 `alarms` 모듈을 쓰려면 AutoJs6를 내부 빌드 6641 (6.8.1) 이상으로 업데이트하고 플러그인 센터에서 플러그인을 활성화하세요.

> 플러그인은 Android 7.0 (API 24) 이상 기기를 지원합니다. APK에 네이티브 라이브러리가 없으므로 패키지는 하나뿐이며 16 KB 메모리 페이지 크기 검사도 해당되지 않습니다.

******

### 스크립트 API

******

스크립트는 전역 모듈 `alarms` (`$alarms`로도 쓸 수 있음)로 이 기능을 사용합니다 (1.0.0 설계 초안이며, 출시 버전의 문서가 우선합니다):

```javascript
let a = alarms.add({ time: "07:30", repeat: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", label: "Wake up" });
a.bind("./after-wake.js", { events: ["dismiss"] });

alarms.add({ time: "18:00", repeat: "FREQ=MONTHLY;BYDAY=-1FR", label: "Monthly report" });
alarms.add({ time: "06:10", date: "2026-11-04", repeat: { freq: "daily", interval: 3 }, tag: "shift" });

alarms.on("ring", (e) => toast(e.label));

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

- `alarms.add(options)`는 알람을 만들고 알람 객체를 반환합니다. `alarms.list`, `get`, `update`, `remove`, `setEnabled`, `skipNext`는 플러그인 화면에서 만든 것을 포함해 플러그인의 모든 알람을 관리합니다.
- `repeat`는 RRULE 문자열 (예: `FREQ=MONTHLY;BYDAY=-1FR`) 또는 구조화된 객체를 받습니다. `alarms.preview(options)`는 앞으로의 울림 시각을 나열해 규칙을 확인할 수 있게 합니다.
- `alarm.bind(script, { events })`는 알람이 울릴 때 AutoJs6가 실행할 스크립트를 등록하며, 이벤트는 `alarms.currentEvent()`로 읽습니다. `alarms.on('ring', listener)`는 스크립트 실행 중에 실시간으로 감시합니다.
- `alarms.system`은 시스템 시계 앱을 직접 호출하며 (`setAlarm`, `setTimer`, `showAlarms`, `next` 등) 플러그인 없이도 쓸 수 있습니다.

******

### 자주 묻는 질문

******

#### AutoJs6 없이도 쓸 수 있나요?

네. 플러그인에는 완전한 시계 화면이 있어 단독으로 알람, 타이머, 스톱워치로 쓸 수 있습니다. AutoJs6는 스크립트로 알람을 관리하거나 연동할 때만 필요합니다.

#### AutoJs6가 실행 중이 아니어도 알람이 울리나요?

네. 알람은 플러그인이 Android의 정확한 알람으로 일정을 잡고 울리므로 AutoJs6 프로세스에 의존하지 않습니다. 스크립트가 등록된 경우 울릴 때 플러그인이 AutoJs6를 깨워 스크립트를 실행합니다.

#### 정확한 알람과 전체 화면 알림 권한이 왜 필요한가요?

정확한 알람은 울림 시각을 정확하게 지키고 (알람 앱은 설치할 때 이 권한을 받습니다), 전체 화면 알림은 잠금 화면에 울림 화면을 보여 주며, 알림은 울리고 있는 알람을 표시합니다. 시스템이 이 중 하나를 철회하면 플러그인의 권한 페이지에서 다시 켜는 방법을 안내합니다.

******

### 권한과 보안

******

플러그인은 권한 범위를 최대한 좁게 유지합니다:

- 보호된 진입점: 기능 서비스와 활성화 진입점은 AutoJs6 플러그인 권한 (`org.autojs.permission.PLUGIN`)으로 보호되어 다른 앱이 직접 호출할 수 없습니다. 시스템 시계 요청을 받는 진입점은 호출자에게 `com.android.alarm.permission.SET_ALARM`을 요구합니다.
- 네트워크: 1.0.0은 네트워크 권한을 요청하지 않으며 알람, 타이머, 스톱워치는 모두 기기 안에서 동작합니다.
- 저장소: 알람은 플러그인 전용 디렉터리에 저장되며 클라우드 백업에서 제외됩니다. 로컬 벨소리는 시스템 파일 선택기로 가져오므로 저장소 권한을 요청하지 않습니다.
- 스크립트 권한: 스크립트는 `alarms` 모듈로 플러그인 화면에서 만든 것을 포함해 플러그인의 모든 알람을 읽고 바꿀 수 있습니다. 등록된 스크립트는 AutoJs6에 저장되며, 플러그인 화면은 알람에 스크립트가 연결되어 있음을 표시하고 연결을 해제하는 것만 할 수 있습니다.

******

### 플러그인 인터페이스

******

다음 정보는 AutoJs6 호스트와 플러그인 개발자를 위한 것으로, 호스트는 이 식별자들로 플러그인을 발견하고 기능을 협상합니다:

```text
application id: io.github.supermonster003.autojs6.plugin.three.chime.alarm
plugin id: three-chime-alarm
engine: alarm
variant: default
info service: org.autojs.plugin.INFO (category three-chime-alarm)
capability service: org.autojs.plugin.ALARM (category alarm)
wake action: org.autojs.plugin.action.WAKE
minimum host build: 6641
native libraries: none (a single APK for every ABI; the 16 KB page size check does not apply)
```

******

### 개발 로드맵

******

계획, 설계 결정, 진행 상황은 ROADMAP.md의 체크리스트로 관리합니다. 체크되지 않은 항목은 계획이며 현재 버전의 기능이 아닙니다.

- [ROADMAP.md 보기](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/ROADMAP.md)

******

### 릴리스 기록

******

#### v1.0.0

_2026/11/05_

- `힌트` 개발 미리보기: AutoJs6 플러그인 센터가 플러그인을 인식할 수 있습니다. 알람, 타이머, 스톱워치, 플러그인 화면, alarms 스크립트 모듈은 아직 제공되지 않습니다
- `힌트` AutoJs6 6.8.1 (6641) 이상이 필요합니다 (정확한 최소 버전은 호스트 쪽 변경이 반영된 뒤 기입됩니다)
- `힌트` 현재 아이콘은 후보 디자인이며 이후 바뀔 수 있습니다
- `기능` 플러그인 골격: Wake 활성화 프로토콜, 플러그인 센터 식별 서비스, 알람 기능 서비스
- `기능` 네이티브 라이브러리가 없어 모든 기기에 쓰는 APK 하나만 제공됩니다
- `기능` 10개 언어의 README, 플러그인 센터 안내, 변경 기록을 JSON 원본에서 일괄 생성
- `의존성` AndroidX Core KTX 1.15.0 추가
- `의존성` lib-recur 0.17.1 추가 (알람 반복 규칙 계산)
- `의존성` Desugar JDK Libs 2.1.5 추가 (Android 7.x에서도 날짜와 시간대 계산 지원)

##### 더 많은 릴리스 기록은 다음을 참고

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/assets/doc/CHANGELOG-ko.md)

******

### 빌드

******

이 절은 소스에서 플러그인을 빌드하려는 개발자를 위한 것입니다.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### 라이선스

******

프로젝트 코드는 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE)으로 배포됩니다. 서드파티 구성 요소와 데이터는 서드파티 고지에서 확인할 수 있습니다.

- [제3자 고지 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/THIRD_PARTY_NOTICES.md)

******

### 관련 링크

******

- AutoJs6 프로젝트: https://github.com/SuperMonster003/AutoJs6
