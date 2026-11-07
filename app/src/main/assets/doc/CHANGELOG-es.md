******

### Historial De Versiones

******

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
