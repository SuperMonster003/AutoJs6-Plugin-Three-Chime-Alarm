3-Chime Alarm es una aplicación de alarmas, temporizadores y cronómetro que también funciona por sí sola, sin AutoJs6. Las alarmas pueden repetirse según reglas de calendario, evitar los festivos o sonar solo en festivos, leer un texto en voz alta al sonar y exigir completar un desafío antes de poder descartarlas. Con AutoJs6, los scripts pueden gestionar alarmas y temporizadores, y los scripts elegidos pueden iniciarse automáticamente cuando suena una alarma u ocurre otro evento.

### Primeros pasos

1. Abre 3-Chime Alarm. Si la pantalla principal muestra el aviso "Es posible que las alarmas no suenen correctamente", tócalo y revisa uno por uno los puntos de la página "Permisos y segundo plano".
2. Toca el botón "Nueva alarma" en la esquina inferior derecha, ajusta la hora, la repetición y el sonido, y luego toca "Guardar".
3. Usa la navegación inferior para cambiar a "Temporizadores" o "Cronómetro"; toca el botón "Ajustes" de la barra superior para definir valores predeterminados como el sonido, la vibración y la opción de posponer.
4. Para usar este complemento en scripts, actívalo en el "Centro de plugins" de AutoJs6.

### Funciones principales

- **Repetición**: elige "Todos los días", "De lunes a viernes", "Los fines de semana" o "Elegir días"; la "Regla avanzada" admite reglas de calendario como el enésimo día de la semana del mes, el final de mes o cada cierto número de días, y se pueden añadir "Fechas omitidas".
- **Sonido**: usa un tono del sistema o un archivo de audio local importado, y ajusta el texto de "Leer en voz alta", la vibración y la opción de posponer; en la pantalla de bloqueo aparece una pantalla de alarma completa, y las alarmas que vencen a la vez suenan una tras otra.
- **Festivos**: la fila "Festivos" de la página de edición ofrece "Omitir festivos", "Solo en festivos" o "Solo en días laborables" (incluidos los días laborables de recuperación). Incluye datos de 250 países y regiones y busca actualizaciones cada semana.
- **Desafío para descartar**: la fila "Desafío para descartar" puede exigir completar "Problemas de cálculo", "Agitar" o "Escribir texto" antes de descartar la alarma. Cada desafío conserva dos salidas: "Detención automática" y "Descarte de emergencia".
- **Temporizadores y cronómetro**: hasta 20 temporizadores funcionan a la vez y se controlan desde la notificación con "Pausar", "+1 min" o "Cancelar"; el cronómetro tiene un botón "Vuelta" y marca la vuelta más rápida y la más lenta.
- **Puntualidad**: las alarmas se reprograman automáticamente tras reiniciar o cambiar la hora o la zona horaria, y suenan incluso antes del primer desbloqueo; una alarma perdida con el dispositivo apagado suena igualmente si se enciende en los 5 minutos siguientes, y si no, queda una notificación.
- **Solicitudes del reloj del sistema**: atiende las solicitudes de otras aplicaciones, como calendarios y asistentes de voz, para poner una alarma, poner un temporizador o descartar una alarma.
- **Apariencia**: "Idioma", "Modo nocturno" y "Color del tema" siguen a AutoJs6 de forma predeterminada y también se pueden ajustar por separado en los ajustes; además se puede cambiar el "Icono del lanzador".
- **Herramientas del complemento**: crea, consulta y modifica alarmas, controla temporizadores y consulta festivos mediante el complemento MCP Server o `plugins.tools.call`.

### Uso en scripts

Los scripts Rhino de AutoJs6 usan las alarmas y los temporizadores mediante el módulo global `alarms`. El ejemplo siguiente crea una alarma a las 7:30 para los días laborables, registra un script que se ejecuta al descartar la alarma y luego inicia un temporizador de 25 minutos:

```js
let a = alarms.add({ time: '07:30', repeat: [1, 2, 3, 4, 5], label: 'Despertar' });
console.log(a.repeatText, new Date(a.nextTrigger));
a.bind('./after-wake.js', { events: ['dismiss'] });

let t = alarms.timers.start('25m', { label: 'Pomodoro' });
console.log(t.id, new Date(t.endsAt).toLocaleTimeString());
```

- Las alarmas y los temporizadores creados por scripts también aparecen en la interfaz del complemento y se pueden modificar allí; los scripts también pueden leer y modificar las alarmas creadas en la interfaz.
- AutoJs6 inicia el script registrado con `bind` cuando ocurre el evento, sin que tenga que seguir en ejecución; un script en ejecución puede escuchar en tiempo real con `alarms.on('ring', ...)`.
- Los scripts de Node.js y Bun no tienen el módulo `alarms`; pueden usar `engines.execScript` para ejecutar un script Rhino que lo use.
- El uso completo, incluidos los festivos, la decisión de un script antes de sonar y los desafíos para descartar, está en [Documentación de AutoJs6 > alarms](https://docs.autojs6.com/#/alarms).

### Requisitos

- Android 7.0 o posterior. Las alarmas, los temporizadores y el cronómetro no necesitan AutoJs6.
- El módulo `alarms` y las herramientas del complemento necesitan AutoJs6 6.8.1 o posterior, con este complemento activado en el "Centro de plugins".
- El permiso de alarmas exactas sirve para sonar a tiempo, y en Android 12 y 12L hay que permitir "Alarmas y recordatorios" en los ajustes del sistema; el permiso de notificaciones sirve para mostrar las alarmas que suenan y los temporizadores en marcha.
- El permiso de avisos a pantalla completa muestra la pantalla de alarma sobre la pantalla de bloqueo; el permiso de superposición es opcional y abre la pantalla de alarma cuando el sistema no muestra avisos a pantalla completa.
- Se recomienda desactivar la optimización de batería, quitar la restricción en segundo plano y permitir el inicio automático en la página "Permisos y segundo plano", para que el sistema no detenga el complemento en segundo plano.

### Preguntas frecuentes

**Alarmas cuando AutoJs6 no está en ejecución**

El complemento programa las alarmas por sí mismo, así que suenan con normalidad aunque AutoJs6 no esté en ejecución. Los scripts registrados los inicia AutoJs6, al que despierta el complemento; tras forzar la detención de AutoJs6, los scripts no se inician hasta volver a abrir AutoJs6.

**El Centro de plugins de AutoJs6 6.8.0 no puede activar este complemento**

Activar este complemento en el Centro de plugins requiere AutoJs6 6.8.1 o posterior. En versiones anteriores de AutoJs6, el complemento sigue funcionando como aplicación de alarmas independiente.

**Alarmas que no suenan a tiempo**

Toca el aviso de la pantalla principal y revisa uno por uno los puntos de la página "Permisos y segundo plano"; en ColorOS, revisa también la fila "Lista de inicio automático" de la misma página. Tras forzar la detención del complemento, las alarmas pendientes no suenan; basta con abrir el complemento una vez para recuperarlas.

**Desafío para descartar que no se puede completar**

Con la pantalla de alarma en primer plano (también sobre la pantalla de bloqueo), pulsar 5 veces una tecla de volumen en 4 segundos descarta la alarma de emergencia; esta función se puede desactivar en los ajustes. Si no, la alarma se detiene al pasar el tiempo de detención automática, que es de al menos 30 minutos si no se ajusta por separado.

Los permisos, las solicitudes del reloj del sistema y los datos de festivos se detallan en la [página del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm).
