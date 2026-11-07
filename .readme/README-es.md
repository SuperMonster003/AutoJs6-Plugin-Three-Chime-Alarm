<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Alarma con reglas de repetición de calendario, lectura en voz alta y scripts al sonar la alarma</p>

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

3-Chime Alarm es el complemento de alarmas de AutoJs6 y también una aplicación de alarmas que funciona por sí sola, sin AutoJs6. Las alarmas se repiten según las reglas de calendario RFC 5545 (por ejemplo, el último viernes de cada mes o cada 3 días a partir de una fecha); una alarma que suena puede leer un texto en voz alta, iniciar un script o avisar a un script en ejecución. Los temporizadores y el cronómetro, la omisión de festivos y los retos para apagar llegarán en versiones posteriores.

El complemento programa y hace sonar las alarmas por sí mismo con las alarmas exactas de Android, así que las alarmas suenan a tiempo aunque AutoJs6 no esté en ejecución. Los scripts de AutoJs6 consultan, crean, modifican y escuchan las alarmas del complemento mediante el módulo global `alarms`; el subespacio de nombres `alarms.system` se comunica directamente con la aplicación de reloj del sistema y funciona sin el complemento. El complemento también acepta solicitudes para configurar alarmas enviadas por otras aplicaciones.

> La versión 1.0.0 ofrece las alarmas, las pantallas del complemento, las herramientas del complemento y el módulo de scripts `alarms`. Activar el complemento en el centro de complementos, las herramientas del complemento y el módulo `alarms` requieren AutoJs6 6.8.1 (compilación interna 6641) o posterior; con versiones anteriores de AutoJs6 (incluida 6.8.0), el complemento solo se puede usar como aplicación de alarmas independiente. El avance de los temporizadores, el cronómetro, los festivos y los retos para apagar se encuentra en la hoja de ruta.

******

### Funciones Destacadas

******

- Alarmas: alarmas únicas o repetidas. Las reglas de repetición cubren días laborables, el n-ésimo día de la semana del mes, el fin de mes, intervalos de días, fecha de fin y número de repeticiones; se pueden excluir fechas o saltar solo la próxima alarma.
- Sonido: tono del sistema o audio local importado, texto leído en voz alta, volumen independiente con aumento gradual, vibración personalizada, posponer y silencio automático, y pantalla de alarma a pantalla completa sobre la pantalla de bloqueo; las alarmas que coinciden suenan una tras otra.
- Puntualidad: las alarmas se reprograman automáticamente tras un reinicio, un cambio de hora o de zona horaria y una actualización del complemento, y suenan incluso antes del primer desbloqueo; una alarma perdida con el dispositivo apagado suena en los 5 minutos siguientes, después solo deja una notificación.
- Scripts vinculados: una alarma puede registrar archivos de script que AutoJs6 inicia al sonar, posponer, apagar y otros eventos, sin que tengan que seguir en ejecución; un script en ejecución también puede escuchar estos eventos en directo.
- Herramientas del complemento: crear, consultar, modificar y eliminar alarmas y atender una alarma que suena mediante el complemento MCP Server o `plugins.tools.call` (nueve herramientas `alarm_*`).
- Reloj del sistema: el complemento puede atender las solicitudes de configurar, mostrar, apagar y posponer alarmas que envían otras aplicaciones.
- Pantallas: el idioma, el modo nocturno y el color del tema siguen a AutoJs6 de forma predeterminada y se pueden ajustar por separado; el icono del lanzador se puede cambiar; la pantalla de ajustes fija el tono, la vibración, el aplazamiento y el silencio automático predeterminados.
- Temporizadores y cronómetro (previstos para 1.1.0): varios temporizadores en paralelo con pausa, reanudación y tiempo extra; vueltas en el cronómetro.
- Festivos (previstos para 1.2.0): datos abiertos de festivos de muchas regiones; las alarmas omiten los festivos y suenan en los días laborables de recuperación; un gancho de script `beforeRing` también puede decidir si suena.
- Retos para apagar (previstos para 1.3.0): aritmética, agitar, escribir una frase o una condición de script, todos con una válvula de seguridad para que la alarma siempre se pueda apagar.

******

### Capturas De Pantalla

******

Capturas tomadas en un Sony XQ-DQ72; el tema claro arriba y el tema oscuro abajo.

| Lista de alarmas | Editar alarma | Alarma sonando | Ajustes |
| :---: | :---: | :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-light.png?raw=true" alt="list-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-light.png?raw=true" alt="edit-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-light.png?raw=true" alt="ring-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-light.png?raw=true" alt="settings-light" width="200" /> |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-dark.png?raw=true" alt="list-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-dark.png?raw=true" alt="edit-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-dark.png?raw=true" alt="ring-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-dark.png?raw=true" alt="settings-dark" width="200" /> |

******

### Uso

******

1. Descargue el APK desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases) e instálelo. Hay un único paquete para todas las ABI.
2. Abra el complemento y revise la página de permisos: notificaciones, alarmas exactas (hay que permitirlas a mano en Android 12 y 12L), alertas a pantalla completa (puede que no estén concedidas desde Android 14), y la optimización de batería, las restricciones en segundo plano y el inicio automático del fabricante. Después funciona como una aplicación de reloj normal.
3. Para usar el módulo `alarms` en scripts o las herramientas del complemento, actualice AutoJs6 a 6.8.1 (compilación interna 6641) o posterior y active el complemento en el centro de complementos (categoría "Herramientas" del asistente de instalación).

> El complemento es compatible con dispositivos con Android 7.0 (API 24) o posterior. Hay un único paquete para todos los dispositivos; su única biblioteca nativa procede del kit de interfaz Compose y está alineada a páginas de memoria de 16 KB, por lo que también funciona en dispositivos con páginas de 16 KB.

******

### API De Script

******

Los scripts usan estas funciones mediante el módulo global `alarms` (también `$alarms`, solo Rhino); la referencia completa está en la [documentación de AutoJs6](https://docs.autojs6.com/#/alarms):

```javascript
let a = alarms.add({ time: "07:30", repeat: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", label: "Wake up" });
a.bind("./after-wake.js", { events: ["dismiss"] });

alarms.add({ time: "18:00", repeat: "FREQ=MONTHLY;BYDAY=-1FR", label: "Monthly report" });
alarms.add({ time: "06:10", date: "2026-11-04", repeat: { freq: "daily", interval: 3 }, tag: "shift" });

alarms.on("ring", (e) => toast(e.label));

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

- `alarms.add(options)` crea una alarma y devuelve su instantánea; `alarms.list`, `get`, `update`, `remove`, `setEnabled`, `skipNext` y `cancelSkip` gestionan todas las alarmas del complemento, incluidas las creadas en sus pantallas.
- `repeat` acepta una cadena RRULE (por ejemplo `FREQ=MONTHLY;BYDAY=-1FR`), un arreglo de números de día o un objeto estructurado; `alarms.preview(options)` enumera las próximas horas de alarma para comprobar una regla.
- `alarms.bind(id, script, options)` (o `bind` de una instantánea de alarma) registra un script que AutoJs6 inicia cuando ocurre un evento; el script lee el evento con `alarms.currentEvent()`. `alarms.on('ring', listener)` escucha en directo mientras se ejecuta un script.
- `alarms.ringing`, `dismiss` y `snooze` atienden una alarma que suena; `alarms.ringtones.list()` enumera los tonos disponibles.
- `alarms.system` llama directamente a la aplicación de reloj del sistema (`setAlarm`, `setTimer`, `showAlarms`, `dismissAlarm`, `next` y más) y no necesita el complemento.

******

### Reloj Del Sistema

******

El complemento puede actuar como aplicación de alarmas del sistema y atender las solicitudes de reloj estándar de otras aplicaciones (por ejemplo calendarios, asistentes de voz y herramientas de automatización). La aplicación que envía la solicitud debe tener el permiso `com.android.alarm.permission.SET_ALARM`:

- Configurar alarma (`SET_ALARM`): lee la hora, la etiqueta, los días, la vibración y el tono; una solicitud que omite la interfaz se guarda directamente, si no se abre una pantalla de edición rellenada.
- Mostrar alarmas (`SHOW_ALARMS`): abre la lista de alarmas.
- Apagar alarma (`DISMISS_ALARM`): termina una alarma que suena o está pospuesta; si no suena ninguna, una alarma que debe sonar en las próximas 24 horas omite esa vez; si coinciden varias, elige el usuario.
- Posponer (`SNOOZE_ALARM`): pospone la alarma que suena.
- `alarms.system` funciona en sentido contrario: un script envía solicitudes a la aplicación de reloj predeterminada del sistema (este complemento u otro reloj) sin el complemento; las diferencias entre los relojes de cada fabricante se describen en la documentación de AutoJs6.

******

### Compatibilidad

******

El complemento es compatible con Android 7.0 (API 24) y posteriores. Activarlo en el centro de complementos, las herramientas del complemento y el módulo `alarms` requieren AutoJs6 6.8.1 (compilación interna 6641) o posterior. Con versiones anteriores de AutoJs6 (por ejemplo 6.8.0, compilación interna 6640), el centro de complementos indica que se necesita un AutoJs6 más reciente y no puede activar el complemento, y las herramientas del complemento y el módulo `alarms` no están disponibles; el complemento sigue funcionando como aplicación de alarmas independiente y sigue atendiendo las solicitudes del reloj del sistema. El módulo `alarms` solo es compatible con el motor Rhino, y su `alarms.system` no necesita el complemento.

Verificado en estos dispositivos antes de publicar la versión 1.0.0 (datos en el apéndice D de la hoja de ruta):

| Dispositivo | Android | Resultado |
| --- | --- | --- |
| Sony XQ-AT72 | 12 (API 31) | Superado: retraso mediano de la alarma 44 ms; suena en la pantalla de bloqueo, tras un reinicio, tras cambios de hora, zona horaria y horario de verano, herramientas del complemento llamadas mediante MCP Server |
| Sony XQ-DQ72 | 13 (API 33) | Superado: retraso mediano 13 ms; alarma a pantalla completa sobre la pantalla de bloqueo, pantalla de alarma mostrada unos 1,1 s después de la hora prevista con el proceso cerrado |
| Redmi 22120RN86C (MIUI) | 13 (API 33) | Superado: retraso mediano 40 ms; el sistema cerró varias veces el proceso del complemento tras el arranque y la alarma sonó a tiempo igualmente |
| Xiaomi Pad 23046RP50C | 15 (API 35) | Superado: retraso mediano 32 ms; sin el permiso de alerta a pantalla completa suena con una notificación emergente, suena a tiempo tras un reinicio antes del desbloqueo |
| OnePlus OPD2413 (ColorOS 15) | 15 (API 35) | Superado (requiere inicio automático): retraso mediano 23 ms; tras activar el complemento en Ajustes > Aplicaciones > Inicio automático, las alarmas suenan a tiempo aunque se limpie el proceso (consulte las preguntas frecuentes) |
| Sony G8441 | 9 (API 28) | Superado: retraso mediano 41 ms; suena en la pantalla de bloqueo, suena a tiempo tras un reinicio antes del desbloqueo |
| Emuladores Android | 7.0 - 16 (API 24 - 36) | Superado: flujo completo en la versión mínima, alarma antes del primer desbloqueo, páginas de memoria de 16 KB |

******

### Preguntas Frecuentes

******

#### Se puede usar sin AutoJs6?

Sí. El complemento tiene pantallas de alarma completas y funciona por sí solo como aplicación de alarmas. AutoJs6 solo hace falta cuando los scripts gestionan alarmas, reaccionan a ellas o usan las herramientas del complemento.

#### Suenan las alarmas si AutoJs6 no está en ejecución?

Sí. El complemento programa y hace sonar las alarmas con las alarmas exactas de Android, sin depender del proceso de AutoJs6. Cuando hay un script registrado, el complemento despierta a AutoJs6 para iniciarlo al sonar la alarma.

#### Por qué el centro de complementos de AutoJs6 6.8.0 no puede activar el complemento?

Activarlo en el centro de complementos, las herramientas del complemento y el módulo `alarms` requieren AutoJs6 6.8.1 (compilación interna 6641) o posterior, y 6.8.0 es la compilación interna 6640. Actualice AutoJs6 para activarlo; mientras tanto, el complemento funciona como aplicación de alarmas independiente.

#### Una alarma no sonó a tiempo. Qué debo comprobar?

Abra primero la página de permisos del complemento, compruebe que las notificaciones y las alarmas exactas están permitidas, y desactive la optimización de batería, quite las restricciones en segundo plano y permita el inicio automático como se indica. El modo No molestar deja pasar las alarmas de forma predeterminada; si cambió sus ajustes, compruebe que las alarmas siguen permitidas. Una alarma perdida porque el dispositivo estaba apagado o el sistema la retrasó suena en los 5 minutos siguientes; después no suena y deja una notificación.

#### Las alarmas no suenan en OPPO, OnePlus o realme (ColorOS). Qué puedo hacer?

ColorOS tiene dos ajustes separados y ambos deben estar activados para 3-Chime Alarm: los interruptores de inicio automático, inicio asociado y actividad en segundo plano en la información de la app, y la lista Ajustes > Aplicaciones > Inicio automático. Con solo los primeros, el sistema bloquea las alarmas pendientes cuando se limpia el proceso del complemento (por ejemplo al deslizarlo en las tareas recientes). Con scripts vinculados, AutoJs6 también debe estar activado en ambos. No deslice el complemento en las tareas recientes; puede bloquearlo allí. En ColorOS, la página de permisos del complemento tiene las filas de ayuda correspondientes, y "Mantener en segundo plano" en los ajustes mantiene una notificación silenciosa para que la limpieza sea menos probable, con un consumo de batería algo mayor.

#### En ColorOS, con el dispositivo bloqueado solo suena el tono y aparece una notificación, sin la pantalla de alarma. Por qué?

ColorOS desactiva las notificaciones "Banner" de las aplicaciones recién instaladas, y entonces el sistema retiene la pantalla de alarma a pantalla completa. Active "Banner" en los ajustes de notificación del complemento; la fila "Notificaciones de banner" de la página de permisos lleva directamente allí. Permitir "Mostrar sobre otras apps" en la página de permisos también sirve de alternativa.

#### Las alarmas dejaron de sonar después de forzar la detención del complemento?

"Forzar detención" hace que el sistema borre todas las alarmas programadas del complemento, y nada suena mientras siga detenido; es una regla de Android. Abra el complemento una vez desde el lanzador para restaurar la programación; las alarmas con menos de 5 minutos de retraso suenan entonces.

#### Xiaomi (MIUI, HyperOS) necesita ajustes especiales?

En un dispositivo de prueba Redmi (MIUI), el sistema cerró varias veces el proceso del complemento tras el arranque, y aun así lo volvió a iniciar a la hora prevista y la alarma sonó a tiempo. Aun así se recomienda configurar el inicio automático y el ahorro de batería como indica la página de permisos.

#### Suenan las alarmas tras un reinicio antes de desbloquear el dispositivo? Y los scripts vinculados?

Las alarmas suenan, porque el complemento funciona antes del primer desbloqueo. Los scripts vinculados no se inician, porque AutoJs6 no puede ejecutar scripts antes del primer desbloqueo, y tampoco se inician después.

******

### Permisos Y Seguridad

******

Permisos que solicita el complemento y para qué sirven:

- Alarmas exactas (`USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` en Android 12 y 12L): sonar a tiempo. Se concede al instalar desde Android 13; en Android 12 y 12L hay que permitir "Alarmas y recordatorios" en los ajustes del sistema.
- Notificaciones (`POST_NOTIFICATIONS`): mostrar las alarmas que suenan y las perdidas. Sin él, las alarmas suenan igualmente, pero sin notificación ni pantalla de alarma.
- Alertas a pantalla completa (`USE_FULL_SCREEN_INTENT`): mostrar la pantalla de alarma sobre la pantalla de bloqueo. Puede que no esté concedido desde Android 14; sin él, las alarmas suenan con una notificación emergente.
- Mostrar sobre otras apps (`SYSTEM_ALERT_WINDOW`, opcional): abre la pantalla de alarma cuando el sistema retiene la alerta a pantalla completa (por ejemplo con las notificaciones de banner de ColorOS desactivadas); solo lo concede el usuario desde la página de permisos cuando hace falta.
- Arranque (`RECEIVE_BOOT_COMPLETED`): reprograma tras un reinicio para sonar incluso antes del primer desbloqueo.
- Servicio en primer plano (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`): mantiene el sonido mientras suena la alarma; también sirve para seguir activo cuando "Mantener en segundo plano" está activado en los ajustes (desactivado de forma predeterminada).
- Vibración, bloqueo de activación y ajustes de audio (`VIBRATE`, `WAKE_LOCK`, `MODIFY_AUDIO_SETTINGS`): vibrar, mantener el dispositivo despierto mientras suena y restaurar después el volumen de alarma independiente.
- Reordenar aplicaciones en ejecución (`REORDER_TASKS`, solo Android 7.0 - 8.1): cuando otras aplicaciones o scripts envían varias solicitudes de alarma casi al mismo tiempo, trae al frente una solicitud que el sistema no inició, para que se atienda cada solicitud. Se concede al instalar.
- Red (`INTERNET`, `ACCESS_NETWORK_STATE`): solo para la búsqueda de actualizaciones de la pantalla de ajustes (desde 1.2.0, también para actualizar los datos de festivos). La búsqueda solo ocurre al abrir la lista de alarmas, con la frecuencia elegida, y evita las redes de uso medido de forma predeterminada; la programación, el sonido y el vínculo con AutoJs6 nunca usan la red.
- Permiso de complemento de AutoJs6 (`org.autojs.permission.PLUGIN`): comunicarse con AutoJs6.

El complemento reduce al máximo sus permisos:

- Entradas protegidas: el servicio de capacidad y la entrada de activación exigen el permiso de complemento de AutoJs6 (`org.autojs.permission.PLUGIN`), así que otras aplicaciones no pueden llamarlos directamente; la entrada que recibe solicitudes del reloj del sistema exige que quien llama tenga `com.android.alarm.permission.SET_ALARM`.
- Red: solo se usa para buscar actualizaciones del complemento (en GitHub; la búsqueda automática se puede desactivar en los ajustes); las alarmas y los datos personales nunca se envían.
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
minimum host build: 6641
native libraries: libandroidx.graphics.path.so only (Compose graphics-path, 4 ABIs, uncompressed, 16 KB aligned; a single APK for every ABI)
```

******

### Hoja De Ruta

******

Los planes, las decisiones de diseño y el progreso se registran como lista de verificación en ROADMAP.md. Los elementos sin marcar son planes, no funciones de la versión actual.

- [Ver ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/ROADMAP.md)

******

### Historial De Versiones

******

#### v1.0.1

_2026/11/08_

- `Corrección` Cuando otra aplicación o un script de AutoJs6 enviaba seguidas dos solicitudes de alarma del mismo tipo (por ejemplo, dos alarmas nuevas una tras otra, o dos llamadas a alarms.system.setAlarm dirigidas a este complemento) sin ninguna pantalla del complemento abierta, la segunda solicitud se ignoraba sin ningún aviso y su alarma no se creaba; ahora se atiende cada solicitud; en Android 7.0 - 8.1 esto requiere el permiso "reordenar aplicaciones en ejecución" (concedido al instalar)

#### v1.0.0

_2026/11/07_

- `Aviso` Activar el complemento en el centro de complementos, las herramientas del complemento y el módulo de scripts alarms requieren AutoJs6 6.8.1 (compilación interna 6641) o posterior; con versiones anteriores de AutoJs6 (por ejemplo 6.8.0), el complemento solo funciona como aplicación de alarmas independiente
- `Aviso` En ColorOS (OPPO, OnePlus, realme), active el inicio automático del complemento tanto en la información de la app como en Ajustes > Aplicaciones > Inicio automático; si no, las alarmas no suenan cuando se limpia el proceso del complemento (consulte README > Preguntas frecuentes)
- `Función` Alarmas: alarmas únicas o repetidas con reglas de calendario RFC 5545 (días laborables, n-ésimo día de la semana del mes, fin de mes, intervalos de días, fecha de fin y número de repeticiones); se pueden excluir fechas o saltar solo la próxima
- `Función` Sonido: tono del sistema o audio local importado, texto leído en voz alta, volumen independiente con aumento gradual, vibración personalizada, posponer y silencio automático, pantalla de alarma a pantalla completa sobre la pantalla de bloqueo, y alarmas coincidentes que suenan una tras otra
- `Función` Programación: las alarmas se reprograman tras un reinicio, un cambio de hora o de zona horaria y una actualización del complemento, suenan antes del primer desbloqueo, y una alarma perdida suena en los 5 minutos siguientes o, si no, deja una notificación
- `Función` Pantallas del complemento: lista de alarmas, pantalla de edición, pantalla de alarma, página de permisos y ajustes; el idioma, el modo nocturno y el color del tema siguen a AutoJs6 de forma predeterminada, y el icono del lanzador se puede cambiar
- `Función` Ajustes: tono, vibración, aplazamiento y silencio automático predeterminados, formato de hora, búsqueda automática de actualizaciones, y la opción "Mantener en segundo plano" (una notificación silenciosa que hace menos probable que el sistema limpie el complemento)
- `Función` La página de permisos comprueba una a una las notificaciones, las alarmas exactas, las alertas a pantalla completa, la optimización de batería, las restricciones en segundo plano y el inicio automático del fabricante, con filas de ayuda para la lista de inicio automático y las notificaciones de banner en ColorOS
- `Función` Módulo de scripts de AutoJs6 alarms / $alarms (requiere AutoJs6 6.8.1, solo Rhino): crear, consultar, modificar y eliminar alarmas, escuchas de eventos, scripts registrados que AutoJs6 inicia cuando ocurre un evento, y la lista de tonos (consulte la [documentación de AutoJs6](https://docs.autojs6.com/#/alarms))
- `Función` Herramientas del complemento alarm_add, alarm_list, alarm_update, alarm_remove, alarm_set_enabled, alarm_skip_next, alarm_upcoming, alarm_dismiss y alarm_snooze, que se pueden llamar mediante el complemento MCP Server o plugins.tools.call
- `Función` Reloj del sistema: atiende las solicitudes de configurar, mostrar, apagar y posponer alarmas de otras aplicaciones
- `Función` Un único APK para todos los dispositivos, incluidos los que usan páginas de memoria de 16 KB
- `Dependencia` Se agrega AndroidX Core KTX 1.15.0
- `Dependencia` Se agrega AndroidX Activity Compose 1.12.2
- `Dependencia` Se agrega Jetpack Compose BOM 2026.09.00 (pantallas del complemento)
- `Dependencia` Se agrega Kotlin Coroutines Android 1.11.0
- `Dependencia` Se agrega Material Color Utilities 4.1.1 (colores del tema)
- `Dependencia` Se agrega lib-recur 0.17.1 (calcula las reglas de repetición de las alarmas)
- `Dependencia` Se agrega Desugar JDK Libs 2.1.5 (cálculos de fecha y zona horaria en Android 7.x)

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
