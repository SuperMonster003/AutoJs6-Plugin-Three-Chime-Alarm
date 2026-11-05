<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>منبه ومؤقتات وساعة إيقاف مع قواعد تكرار تقويمية وتخطي العطلات وتشغيل نصوص برمجية عند الرنين</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=534BAE&label=License"/></a>
  </p>
</div>

******

### اللغات (Languages)

******

يدعم README.md الحالي اللغات التالية:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ru.md)
- العربية [ar] # الحالية

******

### مقدمة

******

⁨3-Chime Alarm⁩ هي إضافة المنبه في AutoJs6, وهي أيضا تطبيق ساعة متكامل يعمل دون AutoJs6: منبهات, وعدة مؤقتات عد تنازلي تعمل معا, وساعة إيقاف. تتكرر المنبهات وفق قواعد التقويم RFC 5545 (مثل آخر جمعة من كل شهر, أو كل 3 أيام بدءا من تاريخ معين), ويمكنها تخطي العطل الرسمية, وتشغيل سكربت أو إشعار سكربت قيد التشغيل عند الرنين.

تجدول الإضافة المنبهات وتطلق رنينها بنفسها عبر المنبهات الدقيقة في Android, لذلك ترن المنبهات في موعدها حتى عندما لا يعمل AutoJs6. تستعلم سكربتات AutoJs6 عن منبهات الإضافة وتنشئها وتعدلها وتراقبها عبر الوحدة العامة `alarms`, أما مساحة الأسماء الفرعية `alarms.system` فتستدعي تطبيق الساعة في النظام مباشرة وتعمل دون الإضافة. تقبل الإضافة أيضا طلبات ضبط المنبه من التطبيقات الأخرى.

> هذا إصدار معاينة للتطوير: يستطيع مركز الإضافات في AutoJs6 اكتشاف الإضافة, لكن المنبهات والمؤقتات وساعة الإيقاف وشاشات الإضافة ووحدة السكربت `alarms` لم تُسلَّم بعد, والأيقونة تصميم مرشح وقد تتغير لاحقا. يصف ما يلي أهداف تصميم كل إصدار, وراجع خارطة الطريق لمعرفة التقدم.

******

### أبرز الميزات

******

- المنبهات: منبهات لمرة واحدة أو متكررة. تشمل قواعد التكرار أيام العمل, واليوم الأسبوعي رقم n من الشهر, وآخر يوم في الشهر, والفواصل بالأيام, وتاريخ الانتهاء, وعدد المرات, ويمكن تخطي المرة القادمة فقط.
- الرنين: نغمات النظام أو ملفات صوتية محلية, وقراءة نص بصوت مسموع, ومستوى صوت مستقل مع ارتفاع تدريجي, وأنماط اهتزاز مخصصة, وتنبيه بملء الشاشة على شاشة القفل, والغفوة.
- ربط السكربتات: يمكن للمنبه تسجيل ملف سكربت يشغله AutoJs6 عند الرنين, فلا يلزم أن يبقى السكربت قيد التشغيل, ويمكن أيضا لسكربت قيد التشغيل مراقبة أحداث الرنين والغفوة والإيقاف لحظة بلحظة.
- المؤقتات وساعة الإيقاف (1.1.0): عدة مؤقتات معا يمكن إيقافها مؤقتا واستئنافها وتمديدها, وتُستعاد بعد إعادة التشغيل, وتسجل ساعة الإيقاف الأشواط.
- العطل (1.2.0): بيانات عطل مفتوحة مدمجة لمناطق كثيرة لتخطي العطل الرسمية مع الرنين في أيام العمل التعويضية, ويمكن أيضا لخطاف السكربت `beforeRing` أن يقرر ما إذا كان المنبه سيرن.
- تحديات الإيقاف (1.3.0): مسائل حسابية أو هز الجهاز أو إعادة كتابة جملة أو شرط سكربت, ولكل منها صمام أمان حتى يمكن إيقاف المنبه دائما.

******

### الاستخدام

******

1. نزّل ملف APK من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases) وثبّته. توجد حزمة واحدة لجميع واجهات ABI.
2. افتح الإضافة وامنح أذونات الإشعارات والمنبهات الدقيقة والتنبيه بملء الشاشة عند الطلب, ثم استخدمها مثل أي تطبيق ساعة عادي.
3. لاستخدام الوحدة `alarms` في السكربتات, حدّث AutoJs6 إلى الإصدار الداخلي 6641 (6.8.1) أو أحدث وفعّل الإضافة في مركز الإضافات.

> تدعم الإضافة الأجهزة التي تعمل بنظام Android 7.0 (API 24) أو أحدث. لا يحتوي ملف APK على مكتبات أصلية, لذا توجد حزمة واحدة فقط ولا ينطبق فحص حجم صفحة الذاكرة 16 KB.

******

### واجهة برمجة النصوص

******

تستخدم السكربتات هذه الميزات عبر الوحدة العامة `alarms` (ويمكن كتابتها أيضا `$alarms`) (مسودة تصميم الإصدار 1.0.0, والمرجع هو توثيق الإصدار المنشور):

```javascript
let a = alarms.add({ time: "07:30", repeat: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", label: "Wake up" });
a.bind("./after-wake.js", { events: ["dismiss"] });

alarms.add({ time: "18:00", repeat: "FREQ=MONTHLY;BYDAY=-1FR", label: "Monthly report" });
alarms.add({ time: "06:10", date: "2026-11-04", repeat: { freq: "daily", interval: 3 }, tag: "shift" });

alarms.on("ring", (e) => toast(e.label));

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

- `alarms.add(options)` تنشئ منبها وتعيد كائن منبه, وتدير `alarms.list`, `get`, `update`, `remove`, `setEnabled`, `skipNext` جميع منبهات الإضافة, بما فيها المنبهات المنشأة من شاشات الإضافة.
- `repeat` يقبل نص RRULE (مثل `FREQ=MONTHLY;BYDAY=-1FR`) أو كائنا منظما, وتعرض `alarms.preview(options)` أوقات الرنين القادمة للتحقق من القاعدة.
- `alarm.bind(script, { events })` تسجل سكربتا يشغله AutoJs6 عند رنين المنبه, ويُقرأ الحدث عبر `alarms.currentEvent()`, أما `alarms.on('ring', listener)` فتراقب أثناء تشغيل السكربت.
- `alarms.system` تستدعي تطبيق الساعة في النظام مباشرة (`setAlarm`, `setTimer`, `showAlarms`, `next` وغيرها) وتعمل دون الإضافة.

******

### الأسئلة الشائعة

******

#### هل يمكن استخدامها دون AutoJs6?

نعم. تحتوي الإضافة على شاشات ساعة كاملة وتعمل وحدها كمنبه ومؤقت وساعة إيقاف. لا يلزم AutoJs6 إلا عندما تدير السكربتات المنبهات أو تتفاعل معها.

#### هل ترن المنبهات عندما لا يعمل AutoJs6?

نعم. تجدول الإضافة المنبهات وتطلق رنينها عبر المنبهات الدقيقة في Android ولا تعتمد على عملية AutoJs6. وعند تسجيل سكربت, توقظ الإضافة AutoJs6 لتشغيله وقت الرنين.

#### لماذا تحتاج الإضافة إلى إذني المنبهات الدقيقة والتنبيه بملء الشاشة?

تحافظ المنبهات الدقيقة على دقة أوقات الرنين (تحصل تطبيقات المنبه على هذا الإذن عند التثبيت), ويعرض التنبيه بملء الشاشة شاشة الرنين على شاشة القفل, وتعرض الإشعارات المنبه الذي يرن. إذا سحب النظام أحدها, تشرح صفحة الأذونات في الإضافة كيفية إعادة تفعيله.

******

### الأذونات والأمان

******

تبقي الإضافة أذوناتها في أضيق نطاق ممكن:

- نقاط دخول محمية: تتطلب خدمة القدرات ونقطة التفعيل إذن إضافات AutoJs6 (`org.autojs.permission.PLUGIN`), فلا يمكن للتطبيقات الأخرى استدعاؤها مباشرة, وتتطلب نقطة الدخول التي تستقبل طلبات ساعة النظام أن يملك المستدعي `com.android.alarm.permission.SET_ALARM`.
- الشبكة: لا يطلب الإصدار 1.0.0 إذن الشبكة, وتعمل المنبهات والمؤقتات وساعة الإيقاف بالكامل على الجهاز.
- التخزين: تُحفظ المنبهات في المجلد الخاص بالإضافة وتُستثنى من النسخ الاحتياطي السحابي, وتُستورد النغمات المحلية عبر منتقي الملفات في النظام دون إذن تخزين.
- وصول السكربتات: يمكن للسكربتات عبر الوحدة `alarms` قراءة جميع منبهات الإضافة وتعديلها, بما فيها المنشأة من شاشاتها, وتُحفظ السكربتات المسجلة في AutoJs6, ولا تفعل شاشات الإضافة سوى الإشارة إلى أن للمنبه سكربتا مربوطا وإتاحة إزالة الربط.

******

### واجهة المكون الإضافي

******

المعلومات التالية موجهة لمطوري مضيف AutoJs6 والمكونات الإضافية; يستخدم المضيف هذه المعرفات لاكتشاف المكون والتفاوض على القدرات:

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

### خارطة الطريق

******

تتابع الخطط وقرارات التصميم والتقدم في شكل قائمة تحقق في ROADMAP.md. العناصر غير المحددة خطط وليست ميزات في الإصدار الحالي.

- [عرض ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/ROADMAP.md)

******

### سجل الإصدارات

******

#### v1.0.0

_2026/11/05_

- `تلميح` معاينة للتطوير: يستطيع مركز الإضافات في AutoJs6 اكتشاف الإضافة, ولم تُسلَّم بعد المنبهات والمؤقتات وساعة الإيقاف وشاشات الإضافة ووحدة السكربت alarms
- `تلميح` يتطلب AutoJs6 6.8.1 (6641) أو أحدث (سيُحدَّد الحد الأدنى الدقيق للإصدار بعد اكتمال تغييرات المضيف)
- `تلميح` الأيقونة الحالية تصميم مرشح وقد تتغير لاحقا
- `ميزة` هيكل الإضافة: بروتوكول التفعيل Wake, وخدمة الهوية في مركز الإضافات, وخدمة قدرات المنبه
- `ميزة` لا مكتبات أصلية: ملف APK واحد لجميع الأجهزة
- `ميزة` ملفات README وتعليمات مركز الإضافات وسجلات التغييرات بعشر لغات, مولَّدة من مصادر JSON
- `تبعية` إرفاق AndroidX Core KTX 1.15.0
- `تبعية` إرفاق lib-recur 0.17.1 (لحساب قواعد تكرار المنبهات)

##### لمزيد من سجل الإصدارات

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/assets/doc/CHANGELOG-ar.md)

******

### البناء

******

هذا القسم موجه للمطورين الراغبين في بناء المكون من المصدر.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### الترخيص

******

شيفرة المشروع مرخصة بموجب [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE). المكونات والبيانات من أطراف ثالثة مذكورة في إشعارات الأطراف الثالثة.

- [إشعارات الأطراف الثالثة (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/THIRD_PARTY_NOTICES.md)

******

### روابط

******

- مشروع AutoJs6: https://github.com/SuperMonster003/AutoJs6
