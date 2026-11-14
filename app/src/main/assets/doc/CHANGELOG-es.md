******

### Historial De Versiones

******

# v1.3.0

###### 2026/11/14

* `Aviso` Los desafíos para descartar fijados en las pantallas del complemento no requieren AutoJs6; fijarlos en un script (la opción dismiss de las alarmas) y la condición de script requieren AutoJs6 6.8.1 (compilación interna 6641) o posterior y este complemento en la versión 1.3.0 o posterior; con el complemento 1.2.x o anterior, una llamada con la opción dismiss falla con un mensaje que pide actualizar el complemento
* `Aviso` Una alarma con desafío para descartar siempre se puede terminar: la detención automática (al menos 30 minutos si no se ajusta en la alarma) y el descarte de emergencia (5 pulsaciones de un botón de volumen en 4 segundos en la pantalla de la alarma) están siempre disponibles; el descarte de emergencia se puede desactivar en los ajustes, y entonces solo la detención automática termina el aviso hasta completar el desafío
* `Función` Desafíos para descartar: una alarma puede exigir resolver problemas de cálculo (dificultad fácil, media o difícil, 3 problemas por defecto), agitar el teléfono (30 sacudidas por defecto) o escribir una frase (la suya, o una frase integrada elegida al azar) antes de poder descartarla, y puede permitir o no posponer durante el desafío; se ajusta en la fila "Desafío para descartar" de la pantalla de edición, y el desafío ocupa el lugar del control deslizante en la pantalla de la alarma
* `Función` Mientras un desafío está activo, el botón "Descartar" de la notificación y las solicitudes de descarte de otras aplicaciones ya no descartan la alarma directamente, sino que abren el desafío en la pantalla de la alarma; si no se permite posponer, la notificación no muestra el botón "Posponer"; en un dispositivo sin acelerómetro el desafío de agitar se convierte en problemas de cálculo
* `Función` Válvulas de seguridad: cuando una alarma con desafío no ajusta la detención automática, esta toma la mayor entre el valor predeterminado de los ajustes y 30 minutos, y la pantalla de la alarma muestra el tiempo restante; 5 pulsaciones de un botón de volumen en 4 segundos mientras la pantalla de la alarma está en primer plano (también en la pantalla de bloqueo) descartan la alarma en caso de emergencia, y durante un desafío los botones de volumen solo cuentan las pulsaciones sin cambiar el volumen; el nuevo grupo "Desafíos para descartar" de los ajustes permite desactivar el descarte de emergencia (activado de forma predeterminada)
* `Función` Condición de script: fijada por un script de AutoJs6, la alarma suena hasta que un script la descarta; cuando el dispositivo no se ha desbloqueado desde un reinicio, no hay ningún script, AutoJs6 no está disponible, o los scripts no pudieron iniciarse o ya terminaron, la alarma pasa de inmediato al desafío alternativo elegido (un descarte normal de forma predeterminada), y el motivo queda anotado en el registro del complemento
* `Función` Novedades del módulo de scripts de AutoJs6: la opción dismiss de las alarmas (el tipo de desafío, la dificultad y el número de problemas de cálculo, el número de sacudidas, el texto que hay que escribir, el desafío que se usa cuando ningún script puede ejecutarse y si se permite posponer), el challenge de alarms.ringing() y de los eventos ring / dismiss, y by: 'challenge' en el evento dismiss (requiere AutoJs6 6.8.1; consulte la [documentación de AutoJs6](https://docs.autojs6.com/#/alarms))
* `Función` Las herramientas del complemento alarm_add y alarm_update aceptan la opción dismiss y pueden fijar los desafíos de cálculo, agitar y escribir texto (salvo la condición de script)

# v1.2.0

###### 2026/11/13

* `Aviso` alarms.holidays, la opción holidays de las alarmas y el evento beforeRing en los scripts requieren AutoJs6 6.8.1 (compilación interna 6641) o posterior y este complemento en la versión 1.2.0 o posterior; con el complemento 1.1.x o anterior las llamadas fallan con un mensaje que pide actualizar el complemento
* `Aviso` Los festivos y los días laborables de recuperación son tan exactos como los datos de origen (el proyecto de código abierto python-holidays); los años aún no anunciados se estiman según las reglas, se marcan como "estimado" y se corrigen con una actualización de los datos tras el anuncio
* `Función` Festivos: las alarmas tienen las opciones "Omitir festivos", "Solo en festivos" y "Solo en días laborables" (suena todos los días laborables, incluidos los de recuperación, nunca en fines de semana ni festivos); incluye datos de festivos de 250 países y regiones (con más de 600 subdivisiones) de 2020 a 2035, con los días laborables de recuperación de China continental y Taiwán
* `Función` En los ajustes se elige la región de festivos predeterminada (deducida al principio de la región del sistema), y cada alarma puede usar su propia región; la lista de alarmas muestra cuántos festivos omite el próximo sonido; los años estimados se marcan como "estimado", ya que aún pueden moverse días libres o de recuperación
* `Función` Las actualizaciones de los datos de festivos se buscan cada semana (se puede desactivar en los ajustes; de forma predeterminada no se descarga en redes de uso medido) y también se pueden buscar a mano en los ajustes; se verifica la firma de los datos descargados y, si falla, se siguen usando los datos actuales; en un año sin datos la alarma suena solo según su regla de repetición y nunca deja de sonar por ello
* `Función` Decisión de un script antes de sonar (beforeRing): un script registrado se inicia antes de sonar (60 segundos de forma predeterminada, de 15 segundos a 5 minutos) y puede omitir esta vez o dejarla sonar con otra etiqueta, otro sonido, otro texto leído u otra vibración solo esta vez; si el script no decide a tiempo, falla o AutoJs6 no está disponible, la alarma suena como siempre
* `Función` Novedades del módulo de scripts de AutoJs6: alarms.holidays (listar regiones, saber si un día es festivo o laborable, listar los festivos de un año, consultar y actualizar los datos), la opción holidays de las alarmas, skip / ring del evento beforeRing y los warnings de alarms.capabilities() sobre datos que faltan y años estimados (requiere AutoJs6 6.8.1; consulte la [documentación de AutoJs6](https://docs.autojs6.com/#/alarms))
* `Función` Nueva herramienta del complemento holiday_check para saber, mediante el complemento MCP Server o plugins.tools.call, si un día es festivo o laborable en una región y el nombre del festivo; alarm_add y alarm_update aceptan la opción holidays

# v1.1.0

###### 2026/11/12

* `Aviso` alarms.timers y alarms.stopwatch en los scripts requieren AutoJs6 6.8.1 (compilación interna 6641) o posterior y este complemento en la versión 1.1.0 o posterior; con el complemento 1.0.x las llamadas fallan con un mensaje que pide actualizar el complemento
* `Función` Temporizadores: hasta 20 temporizadores a la vez, con una duración predefinida (de 1 minuto a 1 hora) o personalizada (de 1 segundo a 100 horas) y una etiqueta opcional; se pueden pausar, reanudar, alargar 1 minuto y restablecer, y al terminar suenan como las alarmas
* `Función` Un temporizador en marcha muestra su cuenta atrás en el panel de notificaciones, desde donde se puede pausar, alargar 1 minuto o cancelar; los temporizadores siguen contando tras un reinicio, y uno que terminó con el dispositivo apagado suena después del arranque (si lleva más de 5 minutos de retraso, solo deja una notificación de perdido)
* `Función` Cronómetro: iniciar, pausar, reanudar y restablecer, hasta 100 vueltas, con la vuelta más rápida y la más lenta señaladas; mientras está en marcha aparece en el panel de notificaciones, lo que se puede desactivar en el grupo "Temporizadores y cronómetro" de los ajustes
* `Función` La barra de navegación inferior cambia entre las páginas de alarmas, temporizadores y cronómetro
* `Función` Nuevos alarms.timers y alarms.stopwatch en el módulo de scripts de AutoJs6 (requiere AutoJs6 6.8.1): iniciar, consultar, pausar, reanudar, añadir o quitar tiempo, restablecer y cancelar temporizadores, scripts registrados en un temporizador que se inician al sonar y en otros eventos, y control del cronómetro con sus vueltas (consulte la [documentación de AutoJs6](https://docs.autojs6.com/#/alarms))
* `Función` Nuevas herramientas del complemento timer_start, timer_list y timer_control para iniciar, listar, pausar, reanudar, alargar, restablecer y cancelar temporizadores mediante el complemento MCP Server o plugins.tools.call
* `Función` Reloj del sistema: también atiende solicitudes de temporizador; otras aplicaciones pueden hacer que el complemento inicie un temporizador, abra la pantalla de temporizadores o apague un temporizador que suena

# v1.0.1

###### 2026/11/08

* `Corrección` Cuando otra aplicación o un script de AutoJs6 enviaba seguidas dos solicitudes de alarma del mismo tipo (por ejemplo, dos alarmas nuevas una tras otra, o dos llamadas a alarms.system.setAlarm dirigidas a este complemento) sin ninguna pantalla del complemento abierta, la segunda solicitud se ignoraba sin ningún aviso y su alarma no se creaba; ahora se atiende cada solicitud; en Android 7.0 - 8.1 esto requiere el permiso "reordenar aplicaciones en ejecución" (concedido al instalar)

# v1.0.0

###### 2026/11/07

* `Aviso` Activar el complemento en el centro de complementos, las herramientas del complemento y el módulo de scripts alarms requieren AutoJs6 6.8.1 (compilación interna 6641) o posterior; con versiones anteriores de AutoJs6 (por ejemplo 6.8.0), el complemento solo funciona como aplicación de alarmas independiente
* `Aviso` En ColorOS (OPPO, OnePlus, realme), active el inicio automático del complemento tanto en la información de la app como en Ajustes > Aplicaciones > Inicio automático; si no, las alarmas no suenan cuando se limpia el proceso del complemento (consulte README > Preguntas frecuentes)
* `Función` Alarmas: alarmas únicas o repetidas con reglas de calendario RFC 5545 (días laborables, n-ésimo día de la semana del mes, fin de mes, intervalos de días, fecha de fin y número de repeticiones); se pueden excluir fechas o saltar solo la próxima
* `Función` Sonido: tono del sistema o audio local importado, texto leído en voz alta, volumen independiente con aumento gradual, vibración personalizada, posponer y silencio automático, pantalla de alarma a pantalla completa sobre la pantalla de bloqueo, y alarmas coincidentes que suenan una tras otra
* `Función` Programación: las alarmas se reprograman tras un reinicio, un cambio de hora o de zona horaria y una actualización del complemento, suenan antes del primer desbloqueo, y una alarma perdida suena en los 5 minutos siguientes o, si no, deja una notificación
* `Función` Pantallas del complemento: lista de alarmas, pantalla de edición, pantalla de alarma, página de permisos y ajustes; el idioma, el modo nocturno y el color del tema siguen a AutoJs6 de forma predeterminada, y el icono del lanzador se puede cambiar
* `Función` Ajustes: tono, vibración, aplazamiento y silencio automático predeterminados, formato de hora, búsqueda automática de actualizaciones, y la opción "Mantener en segundo plano" (una notificación silenciosa que hace menos probable que el sistema limpie el complemento)
* `Función` La página de permisos comprueba una a una las notificaciones, las alarmas exactas, las alertas a pantalla completa, la optimización de batería, las restricciones en segundo plano y el inicio automático del fabricante, con filas de ayuda para la lista de inicio automático y las notificaciones de banner en ColorOS
* `Función` Módulo de scripts de AutoJs6 alarms / $alarms (requiere AutoJs6 6.8.1, solo Rhino): crear, consultar, modificar y eliminar alarmas, escuchas de eventos, scripts registrados que AutoJs6 inicia cuando ocurre un evento, y la lista de tonos (consulte la [documentación de AutoJs6](https://docs.autojs6.com/#/alarms))
* `Función` Herramientas del complemento alarm_add, alarm_list, alarm_update, alarm_remove, alarm_set_enabled, alarm_skip_next, alarm_upcoming, alarm_dismiss y alarm_snooze, que se pueden llamar mediante el complemento MCP Server o plugins.tools.call
* `Función` Reloj del sistema: atiende las solicitudes de configurar, mostrar, apagar y posponer alarmas de otras aplicaciones
* `Función` Un único APK para todos los dispositivos, incluidos los que usan páginas de memoria de 16 KB
* `Dependencia` Se agrega AndroidX Core KTX 1.15.0
* `Dependencia` Se agrega AndroidX Activity Compose 1.12.2
* `Dependencia` Se agrega Jetpack Compose BOM 2026.09.00 (pantallas del complemento)
* `Dependencia` Se agrega Kotlin Coroutines Android 1.11.0
* `Dependencia` Se agrega Material Color Utilities 4.1.1 (colores del tema)
* `Dependencia` Se agrega lib-recur 0.17.1 (calcula las reglas de repetición de las alarmas)
* `Dependencia` Se agrega Desugar JDK Libs 2.1.5 (cálculos de fecha y zona horaria en Android 7.x)
