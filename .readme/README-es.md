<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Alarma, temporizadores y cronómetro con reglas de repetición de calendario, omisión de festivos y scripts al sonar la alarma</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Idiomas (Languages)

******

El README.md actual admite los siguientes idiomas:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-fr.md)
- Español [es] # actual
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ar.md)

******

### Introducción

******

3-Chime Alarm es el complemento de alarmas de AutoJs6 y también una aplicación de reloj completa que funciona sin AutoJs6: alarmas, varios temporizadores de cuenta atrás en paralelo y un cronómetro. Las alarmas se repiten según reglas de calendario RFC 5545 (por ejemplo, el último viernes de cada mes, o cada 3 días a partir de una fecha), pueden omitir los días festivos y pueden iniciar un script o avisar a un script en ejecución cuando suenan.

El complemento programa y hace sonar las alarmas por sí mismo con las alarmas exactas de Android, así que las alarmas suenan a tiempo aunque AutoJs6 no esté en ejecución. Los scripts de AutoJs6 consultan, crean, modifican y escuchan las alarmas del complemento mediante el módulo global `alarms`; el subespacio de nombres `alarms.system` se comunica directamente con la aplicación de reloj del sistema y funciona sin el complemento. El complemento también acepta solicitudes para configurar alarmas enviadas por otras aplicaciones.

> Esta es una versión preliminar de desarrollo: el centro de complementos de AutoJs6 puede detectar el complemento, pero las alarmas, los temporizadores, el cronómetro, las pantallas del complemento y el módulo de script `alarms` aún no están disponibles, y el icono es un diseño candidato que aún puede cambiar. El texto siguiente describe los objetivos de diseño de cada versión; consulte la hoja de ruta para ver el progreso.

******

### Funciones Destacadas

******

- Alarmas: alarmas únicas o repetidas. Las reglas de repetición cubren días laborables, el enésimo día de la semana del mes, el último día del mes, intervalos en días, fecha de fin y número de repeticiones, y se puede omitir solo la próxima vez.
- Sonido: tonos del sistema o audio local, texto leído en voz alta, volumen propio con aumento gradual, patrones de vibración personalizados, alerta a pantalla completa en la pantalla de bloqueo y posponer.
- Integración con scripts: una alarma puede registrar un archivo de script que AutoJs6 inicia cuando suena, de modo que el script no tiene que seguir en ejecución; un script en ejecución también puede escuchar en tiempo real los eventos de sonar, posponer y descartar.
- Temporizadores y cronómetro (1.1.0): varios temporizadores en paralelo que se pueden pausar, reanudar y ampliar, y que se restauran tras reiniciar; el cronómetro registra vueltas.
- Días festivos (1.2.0): datos abiertos de festivos integrados para muchas regiones, para omitir los festivos y sonar igualmente en los días laborables de recuperación; un gancho de script `beforeRing` también puede decidir si suena una alarma.
- Retos para apagar (1.3.0): cálculos, agitar el dispositivo, volver a escribir una frase o una condición de script, cada uno con una válvula de seguridad para que la alarma siempre se pueda apagar.

******

### Uso

******

1. Descargue el APK desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases) e instálelo. Hay un único paquete para todas las ABI.
2. Abra el complemento y conceda los permisos de notificaciones, alarmas exactas y alertas a pantalla completa cuando se soliciten; después funciona como una aplicación de reloj normal.
3. Para usar el módulo `alarms` en scripts, actualice AutoJs6 a la compilación interna 6640 (6.8.1) o posterior y active el complemento en el centro de complementos.

> El complemento es compatible con dispositivos con Android 7.0 (API 24) o posterior. El APK no contiene bibliotecas nativas, por lo que hay un único paquete y la comprobación del tamaño de página de memoria de 16 KB no se aplica.

******

### API De Script

******

Los scripts usan estas funciones mediante el módulo global `alarms` (también disponible como `$alarms`) (borrador de diseño de la 1.0.0; prevalece la documentación de la versión publicada):

```javascript
let a = alarms.add({ time: "07:30", repeat: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", label: "Wake up" });
a.bind("./after-wake.js", { events: ["dismiss"] });

alarms.add({ time: "18:00", repeat: "FREQ=MONTHLY;BYDAY=-1FR", label: "Monthly report" });
alarms.add({ time: "06:10", date: "2026-11-04", repeat: { freq: "daily", interval: 3 }, tag: "shift" });

alarms.on("ring", (e) => toast(e.label));

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

- `alarms.add(options)` crea una alarma y devuelve un objeto de alarma; `alarms.list`, `get`, `update`, `remove`, `setEnabled` y `skipNext` gestionan todas las alarmas del complemento, incluidas las creadas en sus pantallas.
- `repeat` acepta una cadena RRULE (como `FREQ=MONTHLY;BYDAY=-1FR`) o un objeto estructurado; `alarms.preview(options)` enumera las próximas horas de sonido para comprobar una regla.
- `alarm.bind(script, { events })` registra un script que AutoJs6 inicia cuando suena la alarma, con el evento disponible en `alarms.currentEvent()`; `alarms.on('ring', listener)` escucha mientras el script se ejecuta.
- `alarms.system` llama directamente a la aplicación de reloj del sistema (`setAlarm`, `setTimer`, `showAlarms`, `next` y otras) y funciona sin el complemento.

******

### Preguntas Frecuentes

******

#### Puedo usarlo sin AutoJs6?

Sí. El complemento tiene pantallas de reloj completas y funciona por sí solo como despertador, temporizador y cronómetro. AutoJs6 solo es necesario cuando los scripts gestionan las alarmas o reaccionan a ellas.

#### Suenan las alarmas cuando AutoJs6 no está en ejecución?

Sí. El complemento programa y hace sonar las alarmas con las alarmas exactas de Android y no depende del proceso de AutoJs6. Cuando hay un script registrado, el complemento despierta AutoJs6 para iniciarlo a la hora de sonar.

#### Por qué necesita el complemento los permisos de alarmas exactas y alertas a pantalla completa?

Las alarmas exactas mantienen la precisión de las horas de sonido (las aplicaciones de despertador reciben este permiso al instalarse), la alerta a pantalla completa muestra la pantalla de alarma en la pantalla de bloqueo y las notificaciones muestran la alarma que está sonando. Si el sistema revoca alguno, la página de permisos del complemento explica cómo volver a activarlo.

******

### Permisos Y Seguridad

******

El complemento reduce al máximo sus permisos:

- Entradas protegidas: el servicio de capacidad y la entrada de activación exigen el permiso de complemento de AutoJs6 (`org.autojs.permission.PLUGIN`), así que otras aplicaciones no pueden llamarlos directamente; la entrada que recibe solicitudes del reloj del sistema exige que quien llama tenga `com.android.alarm.permission.SET_ALARM`.
- Red: la versión 1.0.0 no solicita permiso de red; las alarmas, los temporizadores y el cronómetro funcionan por completo en el dispositivo.
- Almacenamiento: las alarmas se guardan en el directorio privado del complemento y se excluyen de la copia de seguridad en la nube; los tonos locales se importan con el selector de archivos del sistema, sin permiso de almacenamiento.
- Acceso de scripts: mediante el módulo `alarms`, los scripts pueden leer y modificar todas las alarmas del complemento, incluidas las creadas en sus pantallas; los scripts registrados se guardan en AutoJs6, y las pantallas del complemento solo indican que una alarma tiene un script vinculado y permiten quitar ese vínculo.

******

### Interfaz Del Plugin

******

La siguiente información está dirigida a los desarrolladores del host AutoJs6 y de plugins; el host usa estos identificadores para descubrir el plugin y negociar capacidades:

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

### Hoja De Ruta

******

Los planes, las decisiones de diseño y el progreso se registran como lista de verificación en ROADMAP.md. Los elementos sin marcar son planes, no funciones de la versión actual.

- [Ver ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/ROADMAP.md)

******

### Historial De Versiones

******

#### v1.0.0

_2026/11/05_

- `Aviso` Versión preliminar de desarrollo: el centro de complementos de AutoJs6 puede detectar el complemento; las alarmas, los temporizadores, el cronómetro, las pantallas del complemento y el módulo de script alarms aún no están disponibles
- `Aviso` Requiere AutoJs6 6.8.1 (6640) o posterior (la versión mínima exacta se indicará cuando se integren los cambios del anfitrión)
- `Aviso` El icono actual es un diseño candidato y aún puede cambiar
- `Función` Esqueleto del complemento: protocolo de activación Wake, servicio de identidad del centro de complementos y servicio de capacidad de alarmas
- `Función` Sin bibliotecas nativas: un único APK para todos los dispositivos
- `Función` README, instrucciones del centro de complementos y registros de cambios en 10 idiomas, generados a partir de fuentes JSON
- `Dependencia` Se agrega AndroidX Core KTX 1.15.0

##### Para más historial de versiones

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/assets/doc/CHANGELOG-es.md)

******

### Compilación

******

Esta sección está dirigida a desarrolladores que quieran compilar el plugin desde el código fuente.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### Licencia

******

El código del proyecto se distribuye bajo la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE). Los componentes y datos de terceros se enumeran en los avisos de terceros.

- [Avisos de terceros (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/THIRD_PARTY_NOTICES.md)

******

### Enlaces

******

- Proyecto AutoJs6: https://github.com/SuperMonster003/AutoJs6
