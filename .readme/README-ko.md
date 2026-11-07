<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>달력 반복 규칙, 음성 읽기 알람, 알람이 울릴 때 스크립트 연동을 지원하는 알람 앱</p>

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

3-Chime Alarm은 AutoJs6의 알람 플러그인이자 AutoJs6 없이도 단독으로 쓸 수 있는 알람 앱입니다. 알람은 RFC 5545 달력 규칙으로 반복되며 (예: 매월 마지막 금요일, 특정 날짜부터 3일마다), 울릴 때 텍스트를 읽어 주거나 스크립트를 시작하거나 실행 중인 스크립트에 알릴 수 있습니다. 타이머와 스톱워치, 공휴일 건너뛰기, 끄기 도전 과제는 이후 버전에서 추가됩니다.

플러그인은 Android의 정확한 알람으로 직접 일정을 잡고 알람을 울리므로, AutoJs6가 실행 중이 아니어도 알람이 제시간에 울립니다. AutoJs6 스크립트는 전역 모듈 `alarms`로 플러그인의 알람을 조회, 생성, 변경, 감시합니다. 하위 네임스페이스 `alarms.system`은 시스템 시계 앱을 직접 호출하며 플러그인 없이도 쓸 수 있습니다. 플러그인은 다른 앱의 알람 설정 요청도 받습니다.

> 1.0.0은 알람, 플러그인 화면, 플러그인 도구와 `alarms` 스크립트 모듈을 제공합니다. 플러그인 센터에서 플러그인을 활성화하고 플러그인 도구와 `alarms` 모듈을 쓰려면 AutoJs6 6.8.1 (내부 빌드 6641) 이상이 필요합니다. 그보다 이전의 AutoJs6 (6.8.0 포함)에서는 플러그인을 독립 알람 앱으로만 쓸 수 있습니다. 타이머, 스톱워치, 공휴일, 끄기 도전 과제의 진행 상황은 개발 로드맵을 참고하세요.

******

### 기능 하이라이트

******

- 알람: 일회성 또는 반복 알람. 반복 규칙은 평일, 매월 n번째 요일, 월말, 일 간격, 종료 날짜와 횟수를 지원하며, 특정 날짜를 제외하거나 다음 한 번만 건너뛸 수 있습니다.
- 울림: 시스템 벨소리 또는 가져온 로컬 오디오, 텍스트 읽기, 독립 볼륨과 점점 커지는 소리, 사용자 지정 진동, 다시 알림과 자동 정지, 잠금 화면의 전체 화면 알람 화면; 동시에 울리는 알람은 차례로 울립니다.
- 정시 보장: 재부팅, 시간이나 시간대 변경, 플러그인 업데이트 후 자동으로 다시 예약하며, 재부팅 후 잠금 해제 전에도 울립니다. 전원이 꺼져 놓친 알람은 5분 이내면 울리고, 그 이후에는 알림만 남깁니다.
- 스크립트 연동: 알람에 스크립트 파일을 등록하면 울림, 다시 알림, 끄기 등의 이벤트 때 AutoJs6가 시작하므로 스크립트를 상주시킬 필요가 없습니다. 실행 중인 스크립트도 이 이벤트를 실시간으로 받을 수 있습니다.
- 플러그인 도구: MCP Server 플러그인이나 `plugins.tools.call`로 알람을 추가, 조회, 변경, 삭제하고 울리는 알람을 처리합니다 (`alarm_*` 도구 9개).
- 시스템 시계 연동: 다른 앱이 보내는 알람 설정, 표시, 끄기, 다시 알림 요청을 처리할 수 있습니다.
- 화면: 언어, 야간 모드, 테마 색상은 기본적으로 AutoJs6를 따르며 따로 설정할 수도 있습니다. 런처 아이콘을 바꿀 수 있고, 설정 화면에서 벨소리, 진동, 다시 알림, 자동 정지의 기본값을 정할 수 있습니다.
- 타이머와 스톱워치 (1.1.0 예정): 여러 타이머 동시 실행, 일시 정지, 재개, 시간 추가; 스톱워치 랩.
- 공휴일 (1.2.0 예정): 여러 지역의 오픈 공휴일 데이터를 내장해 공휴일에는 건너뛰고 대체 근무일에는 그대로 울립니다. `beforeRing` 스크립트 훅으로 울릴지 정할 수도 있습니다.
- 끄기 도전 과제 (1.3.0 예정): 산수 문제, 흔들기, 문장 따라 입력, 스크립트 조건. 모두 안전장치가 있어 알람은 항상 끌 수 있습니다.

******

### 스크린샷

******

Sony XQ-DQ72에서 찍은 스크린샷입니다. 윗줄은 라이트 테마, 아랫줄은 다크 테마입니다.

| 알람 목록 | 알람 편집 | 울림 | 설정 |
| :---: | :---: | :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-light.png?raw=true" alt="list-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-light.png?raw=true" alt="edit-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-light.png?raw=true" alt="ring-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-light.png?raw=true" alt="settings-light" width="200" /> |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-dark.png?raw=true" alt="list-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-dark.png?raw=true" alt="edit-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-dark.png?raw=true" alt="ring-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-dark.png?raw=true" alt="settings-dark" width="200" /> |

******

### 사용 방법

******

1. [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases)에서 APK를 내려받아 설치합니다. 패키지는 모든 ABI에 공통인 하나뿐입니다.
2. 플러그인을 열고 권한 페이지에서 알림, 정확한 알람 (Android 12와 12L에서는 직접 허용), 전체 화면 알림 (Android 14부터는 허용되지 않았을 수 있음), 배터리 최적화, 백그라운드 제한, 제조사 자동 시작을 차례로 처리합니다. 그다음에는 일반 시계 앱처럼 쓸 수 있습니다.
3. 스크립트에서 `alarms` 모듈이나 플러그인 도구를 쓰려면 AutoJs6를 6.8.1 (내부 빌드 6641) 이상으로 업데이트하고 플러그인 센터에서 플러그인을 활성화하세요 (설치 마법사의 "도구" 분류).

> 플러그인은 Android 7.0 (API 24) 이상 기기를 지원합니다. 모든 기기에 쓰는 패키지 하나만 제공됩니다. 포함된 유일한 네이티브 라이브러리는 UI 프레임워크 Compose의 것으로 16 KB 메모리 페이지에 맞춰 정렬되어 있어 16 KB 페이지 기기에서도 동작합니다.

******

### 스크립트 API

******

스크립트는 전역 모듈 `alarms` (`$alarms`로도 쓸 수 있음, Rhino 전용)로 이 기능을 사용합니다. 자세한 내용은 [AutoJs6 문서](https://docs.autojs6.com/#/alarms)를 참고하세요:

```javascript
let a = alarms.add({ time: "07:30", repeat: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", label: "Wake up" });
a.bind("./after-wake.js", { events: ["dismiss"] });

alarms.add({ time: "18:00", repeat: "FREQ=MONTHLY;BYDAY=-1FR", label: "Monthly report" });
alarms.add({ time: "06:10", date: "2026-11-04", repeat: { freq: "daily", interval: 3 }, tag: "shift" });

alarms.on("ring", (e) => toast(e.label));

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

- `alarms.add(options)`는 알람을 만들고 그 스냅숏을 반환합니다. `alarms.list`, `get`, `update`, `remove`, `setEnabled`, `skipNext`, `cancelSkip`으로 플러그인 화면에서 만든 것을 포함한 모든 알람을 관리합니다.
- `repeat`에는 RRULE 문자열 (예: `FREQ=MONTHLY;BYDAY=-1FR`), 요일 번호 배열 또는 구조화된 객체를 쓸 수 있습니다. `alarms.preview(options)`는 다음 울림 시각을 나열해 규칙을 확인하기 쉽게 합니다.
- `alarms.bind(id, script, options)` (또는 알람 스냅숏의 `bind`)는 이벤트가 일어날 때 AutoJs6가 시작할 스크립트를 등록하며, 스크립트는 이벤트 데이터를 `alarms.currentEvent()`로 읽습니다. `alarms.on('ring', listener)`는 스크립트 실행 중에 실시간으로 받습니다.
- `alarms.ringing`, `dismiss`, `snooze`는 울리는 알람을 처리하고, `alarms.ringtones.list()`는 쓸 수 있는 벨소리를 나열합니다.
- `alarms.system`은 시스템 시계 앱을 직접 호출하며 (`setAlarm`, `setTimer`, `showAlarms`, `dismissAlarm`, `next` 등), 플러그인이 필요 없습니다.

******

### 시스템 시계 연동

******

플러그인은 시스템의 알람 앱으로서 다른 앱 (예: 캘린더, 음성 비서, 자동화 도구)이 보내는 표준 시계 요청을 처리할 수 있습니다. 요청하는 앱은 `com.android.alarm.permission.SET_ALARM` 권한이 있어야 합니다:

- 알람 설정 (`SET_ALARM`): 시간, 라벨, 요일, 진동, 벨소리를 읽습니다. 화면을 건너뛰라는 요청이면 바로 저장하고, 아니면 값이 채워진 편집 화면을 엽니다.
- 알람 표시 (`SHOW_ALARMS`): 알람 목록을 엽니다.
- 알람 끄기 (`DISMISS_ALARM`): 울리거나 다시 알림 중인 알람을 끝냅니다. 울리는 알람이 없으면 24시간 안에 울릴 알람의 그 회차를 건너뜁니다. 여러 알람이 해당하면 사용자가 고릅니다.
- 다시 알림 (`SNOOZE_ALARM`): 울리는 알람을 다시 알림으로 미룹니다.
- 반대 방향의 `alarms.system`: 스크립트는 이것으로 시스템 기본 시계 앱 (이 플러그인 또는 다른 시계 앱)에 요청을 보내며 플러그인이 필요 없습니다. 제조사별 시계 앱의 차이는 AutoJs6 문서를 참고하세요.

******

### 호환성

******

플러그인은 Android 7.0 (API 24) 이상을 지원합니다. 플러그인 센터에서 활성화하고 플러그인 도구와 `alarms` 모듈을 쓰려면 AutoJs6 6.8.1 (내부 빌드 6641) 이상이 필요합니다. 그보다 이전의 AutoJs6 (예: 내부 빌드 6640인 6.8.0)에서는 플러그인 센터가 더 새로운 AutoJs6가 필요하다고 표시하고 플러그인을 활성화할 수 없으며 플러그인 도구와 `alarms` 모듈도 쓸 수 없지만, 플러그인은 독립 알람 앱으로 쓸 수 있고 시스템 시계 요청도 그대로 처리합니다. `alarms` 모듈은 Rhino 엔진만 지원하며, 그중 `alarms.system`은 플러그인이 필요 없습니다.

1.0.0 출시 전에 다음 기기에서 검증했습니다 (데이터는 개발 로드맵 부록 D):

| 기기 | Android | 결과 |
| --- | --- | --- |
| Sony XQ-AT72 | 12 (API 31) | 통과: 울림 지연 중앙값 44 ms; 잠금 화면 울림, 재부팅 후 울림, 시간, 시간대, 일광 절약 시간 변경, MCP Server로 플러그인 도구 호출 |
| Sony XQ-DQ72 | 13 (API 33) | 통과: 울림 지연 중앙값 13 ms; 잠금 화면 전체 화면 울림, 프로세스가 종료된 뒤에도 예정 시각 약 1.1초 후 알람 화면 표시 |
| Redmi 22120RN86C (MIUI) | 13 (API 33) | 통과: 울림 지연 중앙값 40 ms; 부팅 후 시스템이 플러그인 프로세스를 여러 번 종료해도 제시간에 울림 |
| Xiaomi Pad 23046RP50C | 15 (API 35) | 통과: 울림 지연 중앙값 32 ms; 전체 화면 알림 권한이 없으면 헤드업 알림으로 울림, 재부팅 후 잠금 해제 전에도 제시간에 울림 |
| OnePlus OPD2413 (ColorOS 15) | 15 (API 35) | 통과 (자동 시작 필요): 울림 지연 중앙값 23 ms; 설정 > 앱 > 자동 시작에서 켜면 프로세스가 정리되어도 제시간에 울림 (자주 묻는 질문 참고) |
| Sony G8441 | 9 (API 28) | 통과: 울림 지연 중앙값 41 ms; 잠금 화면 울림, 재부팅 후 잠금 해제 전에도 제시간에 울림 |
| Android 에뮬레이터 | 7.0 - 16 (API 24 - 36) | 통과: 최저 버전 전체 흐름, 첫 잠금 해제 전 울림, 16 KB 메모리 페이지 |

******

### 자주 묻는 질문

******

#### AutoJs6 없이 쓸 수 있나요?

네. 플러그인에는 알람 화면이 모두 있어 단독으로 알람 앱처럼 쓸 수 있습니다. AutoJs6는 스크립트로 알람을 관리하거나 연동하거나 플러그인 도구를 쓸 때만 필요합니다.

#### AutoJs6가 실행 중이 아니어도 알람이 울리나요?

네. 알람은 플러그인이 Android 정확한 알람으로 예약하고 울리므로 AutoJs6 프로세스에 의존하지 않습니다. 스크립트가 등록되어 있으면 알람이 울릴 때 플러그인이 AutoJs6를 깨워 스크립트를 시작합니다.

#### AutoJs6 6.8.0의 플러그인 센터에서 플러그인을 활성화할 수 없는 이유는 무엇인가요?

플러그인 센터에서 활성화하고 플러그인 도구와 `alarms` 모듈을 쓰려면 AutoJs6 6.8.1 (내부 빌드 6641) 이상이 필요한데, 6.8.0의 내부 빌드는 6640입니다. AutoJs6를 업데이트하면 활성화할 수 있으며, 그 전까지는 독립 알람 앱으로 쓸 수 있습니다.

#### 알람이 제시간에 울리지 않았습니다. 무엇을 확인해야 하나요?

먼저 플러그인의 권한 페이지를 열어 알림과 정확한 알람이 허용되어 있는지 확인하고, 페이지 안내에 따라 배터리 최적화를 끄고 백그라운드 제한을 풀고 자동 시작을 허용하세요. 방해 금지 모드는 기본적으로 알람을 허용합니다. 설정을 바꿨다면 알람이 여전히 허용되는지 확인하세요. 전원이 꺼져 있거나 시스템이 늦춰 놓친 알람은 5분 이내면 울리고, 그 이후에는 울리지 않고 알림을 남깁니다.

#### OPPO, OnePlus, realme (ColorOS)에서 알람이 울리지 않으면 어떻게 하나요?

ColorOS에는 서로 독립된 스위치가 두 곳 있으며, 3-Chime Alarm을 위해 둘 다 켜야 합니다. 앱 정보의 "자동 시작" / "연관 시작" / "백그라운드 활동"과 설정 > 앱 > 자동 시작 목록입니다. 앞쪽만 켜면 플러그인 프로세스가 정리된 뒤 (예: 최근 작업에서 밀어서 지운 뒤) 시스템이 예정된 알람을 막습니다. 스크립트 연동을 쓸 때는 AutoJs6도 두 곳 모두 켜야 합니다. 또한 최근 작업에서 플러그인을 밀어서 지우지 말고 잠가 둘 수 있습니다. ColorOS에서는 플러그인 권한 페이지에 해당 안내 행이 있으며, 설정 화면의 "백그라운드에서 계속 실행"을 켜면 조용한 알림을 상주시켜 정리될 가능성을 줄일 수 있습니다 (배터리 소모가 조금 늘어납니다).

#### ColorOS에서 잠금 상태일 때 벨소리와 알림만 있고 알람 화면이 뜨지 않습니다. 왜 그런가요?

ColorOS는 새로 설치한 앱의 "배너" 알림을 기본으로 꺼 두며, 그래서 시스템이 전체 화면 알람 화면을 띄우지 않습니다. 플러그인 알림 설정에서 "배너"를 켜세요. 권한 페이지의 "배너 알림" 행에서 그 설정을 바로 열 수 있습니다. 권한 페이지에서 "다른 앱 위에 표시"를 허용해 두면 대체 수단이 됩니다.

#### 플러그인을 강제 종료한 뒤 알람이 울리지 않습니다.

"강제 종료"하면 시스템이 플러그인의 알람 예약을 모두 지우며, 멈춰 있는 동안에는 예정 시각이 되어도 울리지 않습니다. 이것은 Android의 규칙입니다. 런처에서 플러그인을 한 번 열면 예약이 복구되고, 5분 이내로 늦은 알람은 그때 울립니다.

#### Xiaomi (MIUI, HyperOS)에서 특별한 설정이 필요한가요?

Redmi (MIUI) 테스트에서는 부팅 후 시스템이 플러그인 프로세스를 여러 번 종료했지만, 예정 시각에는 시스템이 플러그인을 다시 시작해 제시간에 울렸습니다. 그래도 권한 페이지 안내에 따라 자동 시작과 절전 정책을 설정해 두는 것을 권장합니다.

#### 재부팅 후 잠금 해제 전에도 알람이 울리나요? 연동된 스크립트는요?

알람은 울립니다. 플러그인은 첫 잠금 해제 전에도 동작합니다. 연동된 스크립트는 시작되지 않습니다. AutoJs6는 첫 잠금 해제 전에는 스크립트를 실행할 수 없고, 잠금을 해제한 뒤에도 나중에 시작하지 않습니다.

******

### 권한과 보안

******

플러그인이 요청하는 권한과 용도:

- 정확한 알람 (`USE_EXACT_ALARM`, Android 12와 12L에서는 `SCHEDULE_EXACT_ALARM`): 제시간 울림. Android 13부터는 설치 시 허용되며, Android 12와 12L에서는 시스템 설정에서 "알람 및 리마인더"를 허용해야 합니다.
- 알림 (`POST_NOTIFICATIONS`): 울리는 알람과 놓친 알람 표시. 없으면 알람은 울리지만 알림과 알람 화면이 없습니다.
- 전체 화면 알림 (`USE_FULL_SCREEN_INTENT`): 잠금 화면에 알람 화면 표시. Android 14부터는 허용되지 않았을 수 있으며, 없으면 헤드업 알림으로 울립니다.
- 다른 앱 위에 표시 (`SYSTEM_ALERT_WINDOW`, 선택): 시스템이 전체 화면 알림을 띄우지 않을 때 (예: ColorOS에서 배너 알림이 꺼져 있을 때) 알람 화면을 엽니다. 필요할 때 권한 페이지에서 사용자가 허용합니다.
- 부팅 수신 (`RECEIVE_BOOT_COMPLETED`): 재부팅 후 다시 예약해 첫 잠금 해제 전에도 울릴 수 있게 합니다.
- 포그라운드 서비스 (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`): 울리는 동안 소리를 유지합니다. 설정의 "백그라운드에서 계속 실행"이 켜져 있으면 상주에도 씁니다 (기본값은 꺼짐).
- 진동, 절전 해제 잠금, 오디오 설정 (`VIBRATE`, `WAKE_LOCK`, `MODIFY_AUDIO_SETTINGS`): 진동, 울리는 동안 기기를 깨워 두기, 울린 뒤 독립 알람 볼륨 복원.
- 실행 중인 앱 순서 변경 (`REORDER_TASKS`, Android 7.0 - 8.1만): 다른 앱이나 스크립트가 거의 동시에 여러 알람 요청을 보낼 때, 시스템이 시작하지 않은 요청을 앞으로 가져와 처리하여 모든 요청이 처리되게 합니다. 설치할 때 부여됩니다.
- 네트워크 (`INTERNET`, `ACCESS_NETWORK_STATE`): 설정 화면의 업데이트 확인에만 씁니다 (1.2.0부터는 공휴일 데이터 갱신에도 사용). 알람 목록을 열 때 정한 빈도로만 확인하며, 데이터 요금제 네트워크에서는 기본적으로 확인하지 않습니다. 예약, 울림, AutoJs6 연동은 네트워크를 쓰지 않습니다.
- AutoJs6 플러그인 권한 (`org.autojs.permission.PLUGIN`): AutoJs6와 통신합니다.

플러그인은 권한 범위를 최대한 좁게 유지합니다:

- 보호된 진입점: 기능 서비스와 활성화 진입점은 AutoJs6 플러그인 권한 (`org.autojs.permission.PLUGIN`)으로 보호되어 다른 앱이 직접 호출할 수 없습니다. 시스템 시계 요청을 받는 진입점은 호출자에게 `com.android.alarm.permission.SET_ALARM`을 요구합니다.
- 네트워크: 플러그인 업데이트 확인 (GitHub에 연결, 자동 확인은 설정 화면에서 끌 수 있음)에만 쓰며, 알람이나 개인 데이터는 업로드하지 않습니다.
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
native libraries: libandroidx.graphics.path.so only (Compose graphics-path, 4 ABIs, uncompressed, 16 KB aligned; a single APK for every ABI)
```

******

### 개발 로드맵

******

계획, 설계 결정, 진행 상황은 ROADMAP.md의 체크리스트로 관리합니다. 체크되지 않은 항목은 계획이며 현재 버전의 기능이 아닙니다.

- [ROADMAP.md 보기](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/ROADMAP.md)

******

### 릴리스 기록

******

#### v1.0.1

_2026/11/08_

- `수정` 다른 앱이나 AutoJs6 스크립트가 같은 종류의 알람 요청을 짧은 간격으로 연달아 보내면 (예: 알람 두 개를 연달아 추가하거나, 이 플러그인을 지정해 alarms.system.setAlarm을 연달아 호출) 플러그인 화면이 열려 있지 않을 때 두 번째 요청이 아무 안내 없이 무시되어 알람이 만들어지지 않았습니다. 이제 모든 요청을 처리합니다. Android 7.0 - 8.1에서는 이를 위해 "실행 중인 앱 순서 변경" 권한이 필요합니다 (설치할 때 자동으로 부여됨)

#### v1.0.0

_2026/11/07_

- `힌트` 플러그인 센터에서 활성화하고 플러그인 도구와 alarms 스크립트 모듈을 쓰려면 AutoJs6 6.8.1 (내부 빌드 6641) 이상이 필요합니다. 그보다 이전의 AutoJs6 (예: 6.8.0)에서는 플러그인을 독립 알람 앱으로만 쓸 수 있습니다
- `힌트` ColorOS (OPPO, OnePlus, realme)에서는 앱 정보와 설정 > 앱 > 자동 시작 두 곳에서 플러그인의 자동 시작을 켜세요. 그렇지 않으면 플러그인 프로세스가 정리된 뒤 알람이 울리지 않습니다 (README > 자주 묻는 질문 참고)
- `기능` 알람: 일회성 또는 반복 알람. 반복은 RFC 5545 달력 규칙 (평일, 매월 n번째 요일, 월말, 일 간격, 종료 날짜와 횟수)을 따르며, 날짜를 제외하거나 다음 한 번만 건너뛸 수 있습니다
- `기능` 울림: 시스템 벨소리 또는 가져온 로컬 오디오, 텍스트 읽기, 독립 볼륨과 점점 커지는 소리, 사용자 지정 진동, 다시 알림과 자동 정지, 잠금 화면의 전체 화면 알람 화면, 동시에 울리는 알람은 차례로 울림
- `기능` 예약: 재부팅, 시간이나 시간대 변경, 플러그인 업데이트 후 자동으로 다시 예약하고 첫 잠금 해제 전에도 울리며, 놓친 알람은 5분 이내면 울리고 그 이후에는 알림을 남깁니다
- `기능` 플러그인 화면: 알람 목록, 편집 화면, 알람 화면, 권한 페이지, 설정. 언어, 야간 모드, 테마 색상은 기본적으로 AutoJs6를 따르며 런처 아이콘을 바꿀 수 있습니다
- `기능` 설정: 벨소리, 진동, 다시 알림, 자동 정지의 기본값, 시간 형식, 업데이트 자동 확인, 선택 사항인 "백그라운드에서 계속 실행" (조용한 알림을 상주시켜 시스템에 의해 정리될 가능성을 줄임)
- `기능` 권한 페이지에서 알림, 정확한 알람, 전체 화면 알림, 배터리 최적화, 백그라운드 제한, 제조사 자동 시작을 차례로 확인하며, ColorOS에서는 자동 시작 목록과 배너 알림 안내도 보여 줍니다
- `기능` AutoJs6 스크립트 모듈 alarms / $alarms (AutoJs6 6.8.1 필요, Rhino 전용): 알람의 추가, 조회, 변경, 삭제, 이벤트 수신, 이벤트가 일어날 때 AutoJs6가 시작할 스크립트 등록, 벨소리 목록 ([AutoJs6 문서](https://docs.autojs6.com/#/alarms) 참고)
- `기능` 플러그인 도구 alarm_add, alarm_list, alarm_update, alarm_remove, alarm_set_enabled, alarm_skip_next, alarm_upcoming, alarm_dismiss, alarm_snooze. MCP Server 플러그인이나 plugins.tools.call로 호출할 수 있습니다
- `기능` 시스템 시계 연동: 다른 앱이 보내는 알람 설정, 표시, 끄기, 다시 알림 요청을 처리합니다
- `기능` 모든 기기에 쓰는 APK 하나만 제공되며 16 KB 메모리 페이지 기기도 지원합니다
- `의존성` AndroidX Core KTX 1.15.0 추가
- `의존성` AndroidX Activity Compose 1.12.2 추가
- `의존성` Jetpack Compose BOM 2026.09.00 추가 (플러그인 화면)
- `의존성` Kotlin Coroutines Android 1.11.0 추가
- `의존성` Material Color Utilities 4.1.1 추가 (테마 색상)
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
