<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Будильник, таймеры и секундомер с календарными правилами повтора, пропуском праздников и запуском скриптов при звонке</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Языки (Languages)

******

Текущий README.md поддерживает следующие языки:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ko.md)
- Русский [ru] # текущий
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ar.md)

******

### Введение

******

3-Chime Alarm - плагин будильника для AutoJs6 и одновременно полноценное приложение часов, которое работает и без AutoJs6: будильники, несколько параллельных таймеров обратного отсчёта и секундомер. Будильники повторяются по календарным правилам RFC 5545 (например, в последнюю пятницу каждого месяца или каждые 3 дня начиная с заданной даты), могут пропускать праздники и при срабатывании запускать скрипт или уведомлять уже работающий скрипт.

Плагин сам планирует будильники и подаёт сигнал с помощью точных будильников Android, поэтому будильники звонят вовремя, даже когда AutoJs6 не запущен. Скрипты AutoJs6 запрашивают, создают, изменяют и отслеживают будильники плагина через глобальный модуль `alarms`; вложенное пространство имён `alarms.system` обращается напрямую к системному приложению часов и работает без плагина. Плагин также принимает запросы на установку будильника от других приложений.

> Это предварительная версия для разработки: центр плагинов AutoJs6 может обнаружить плагин, но будильники, таймеры, секундомер, экраны плагина и модуль скриптов `alarms` ещё не готовы, а значок - вариант-кандидат и ещё может измениться. Ниже описаны цели каждой версии; ход работы смотрите в плане развития.

******

### Основные Возможности

******

- Будильники: одноразовые или повторяющиеся. Правила повтора охватывают будни, n-й день недели месяца, последний день месяца, интервалы в днях, дату окончания и число повторов; можно пропустить только ближайшее срабатывание.
- Сигнал: системные мелодии или локальные аудиофайлы, чтение текста вслух, собственная громкость с плавным нарастанием, настраиваемая вибрация, полноэкранное оповещение на экране блокировки и отложенный повтор.
- Связь со скриптами: будильник может зарегистрировать файл скрипта, который AutoJs6 запускает при срабатывании, так что скрипту не нужно работать постоянно; работающий скрипт также может в реальном времени отслеживать события срабатывания, отложенного повтора и выключения.
- Таймеры и секундомер (1.1.0): несколько параллельных таймеров с паузой, продолжением и добавлением времени, которые восстанавливаются после перезагрузки; секундомер записывает круги.
- Праздники (1.2.0): встроенные открытые данные о праздниках для многих регионов, чтобы пропускать праздничные дни и звонить в перенесённые рабочие дни; скриптовый хук `beforeRing` тоже может решать, звонить ли будильнику.
- Задания для выключения (1.3.0): арифметика, встряхивание, повторный набор фразы или условие скрипта, у каждого есть предохранитель, чтобы будильник всегда можно было выключить.

******

### Использование

******

1. Скачайте APK со страницы [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases) и установите его. Для всех ABI есть один общий пакет.
2. Откройте плагин и по запросу предоставьте разрешения на уведомления, точные будильники и полноэкранные оповещения; после этого он работает как обычное приложение часов.
3. Чтобы использовать модуль `alarms` в скриптах, обновите AutoJs6 до внутренней сборки 6641 (6.8.1) или новее и включите плагин в центре плагинов.

> Плагин поддерживает устройства с Android 7.0 (API 24) и новее. APK не содержит нативных библиотек, поэтому пакет один, а проверка размера страницы памяти 16 KB не требуется.

******

### API Скриптов

******

Скрипты используют эти возможности через глобальный модуль `alarms` (также доступен как `$alarms`) (черновик проекта версии 1.0.0; основной считается документация выпущенной версии):

```javascript
let a = alarms.add({ time: "07:30", repeat: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", label: "Wake up" });
a.bind("./after-wake.js", { events: ["dismiss"] });

alarms.add({ time: "18:00", repeat: "FREQ=MONTHLY;BYDAY=-1FR", label: "Monthly report" });
alarms.add({ time: "06:10", date: "2026-11-04", repeat: { freq: "daily", interval: 3 }, tag: "shift" });

alarms.on("ring", (e) => toast(e.label));

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

- `alarms.add(options)` создаёт будильник и возвращает объект будильника; `alarms.list`, `get`, `update`, `remove`, `setEnabled` и `skipNext` управляют всеми будильниками плагина, включая созданные на его экранах.
- `repeat` принимает строку RRULE (например, `FREQ=MONTHLY;BYDAY=-1FR`) или структурированный объект; `alarms.preview(options)` перечисляет ближайшие моменты срабатывания, чтобы проверить правило.
- `alarm.bind(script, { events })` регистрирует скрипт, который AutoJs6 запускает при срабатывании будильника, а событие доступно через `alarms.currentEvent()`; `alarms.on('ring', listener)` отслеживает события, пока скрипт работает.
- `alarms.system` напрямую вызывает системное приложение часов (`setAlarm`, `setTimer`, `showAlarms`, `next` и другие) и работает без плагина.

******

### Частые Вопросы

******

#### Можно ли пользоваться им без AutoJs6?

Да. В плагине есть полноценные экраны часов, и он работает самостоятельно как будильник, таймер и секундомер. AutoJs6 нужен, только когда скрипты управляют будильниками или реагируют на них.

#### Звонят ли будильники, когда AutoJs6 не запущен?

Да. Плагин планирует будильники и подаёт сигнал с помощью точных будильников Android и не зависит от процесса AutoJs6. Если зарегистрирован скрипт, плагин в момент срабатывания будит AutoJs6, чтобы запустить его.

#### Зачем плагину разрешения на точные будильники и полноэкранные оповещения?

Точные будильники обеспечивают точное время срабатывания (приложения-будильники получают это разрешение при установке), полноэкранное оповещение показывает экран сигнала на экране блокировки, а уведомления показывают звонящий будильник. Если система отзовёт одно из разрешений, страница разрешений плагина объяснит, как включить его снова.

******

### Разрешения И Безопасность

******

Плагин запрашивает минимально необходимые разрешения:

- Защищённые точки входа: служба возможностей и точка активации требуют разрешения плагинов AutoJs6 (`org.autojs.permission.PLUGIN`), поэтому другие приложения не могут вызвать их напрямую; точка входа, принимающая запросы системных часов, требует, чтобы у вызывающей стороны было `com.android.alarm.permission.SET_ALARM`.
- Сеть: версия 1.0.0 не запрашивает доступ к сети; будильники, таймеры и секундомер полностью работают на устройстве.
- Хранилище: будильники хранятся в закрытом каталоге плагина и исключены из облачного резервного копирования; локальные мелодии импортируются через системный выбор файлов без разрешения на доступ к хранилищу.
- Доступ скриптов: через модуль `alarms` скрипты могут читать и изменять все будильники плагина, включая созданные на его экранах; зарегистрированные скрипты хранятся в AutoJs6, а экраны плагина только отмечают, что к будильнику привязан скрипт, и позволяют удалить привязку.

******

### Интерфейс Плагина

******

Следующая информация предназначена разработчикам хоста AutoJs6 и плагинов; хост использует эти идентификаторы для обнаружения плагина и согласования возможностей:

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

### Дорожная Карта

******

Планы, проектные решения и ход работ ведутся в виде списка с отметками в ROADMAP.md. Неотмеченные пункты являются планами, а не возможностями текущей версии.

- [Открыть ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/ROADMAP.md)

******

### История Выпусков

******

#### v1.0.0

_2026/11/05_

- `Подсказка` Предварительная версия для разработки: центр плагинов AutoJs6 может обнаружить плагин; будильники, таймеры, секундомер, экраны плагина и модуль скриптов alarms ещё не готовы
- `Подсказка` Требуется AutoJs6 6.8.1 (6641) или новее (точная минимальная версия будет указана после внесения изменений в хост)
- `Подсказка` Текущий значок - вариант-кандидат и ещё может измениться
- `Функция` Каркас плагина: протокол активации Wake, служба идентификации для центра плагинов и служба возможностей будильника
- `Функция` Без нативных библиотек: один APK для всех устройств
- `Функция` README, инструкции для центра плагинов и журналы изменений на 10 языках, создаваемые из исходников JSON
- `Зависимость` Добавлен AndroidX Core KTX 1.15.0
- `Зависимость` Добавлен lib-recur 0.17.1 (вычисляет правила повтора будильников)
- `Зависимость` Добавлен Desugar JDK Libs 2.1.5 (вычисления дат и часовых поясов на Android 7.x)

##### Больше истории выпусков

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/assets/doc/CHANGELOG-ru.md)

******

### Сборка

******

Этот раздел предназначен разработчикам, желающим собрать плагин из исходников.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### Лицензия

******

Код проекта распространяется по лицензии [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE). Сторонние компоненты и данные перечислены в уведомлениях о сторонних компонентах.

- [Сведения о сторонних компонентах (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/THIRD_PARTY_NOTICES.md)

******

### Ссылки

******

- Проект AutoJs6: https://github.com/SuperMonster003/AutoJs6
