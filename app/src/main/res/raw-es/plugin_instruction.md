3-Chime Alarm es el complemento de alarmas de AutoJs6 y también una aplicación de reloj completa que funciona sin AutoJs6: alarmas, varios temporizadores de cuenta atrás en paralelo y un cronómetro. Las alarmas se repiten según reglas de calendario RFC 5545 (por ejemplo, el último viernes de cada mes, o cada 3 días a partir de una fecha), pueden omitir los días festivos y pueden iniciar un script o avisar a un script en ejecución cuando suenan.

> Esta es una versión preliminar de desarrollo: el centro de complementos de AutoJs6 puede detectar el complemento, pero las alarmas, los temporizadores, el cronómetro, las pantallas del complemento y el módulo de script `alarms` aún no están disponibles, y el icono es un diseño candidato que aún puede cambiar. El texto siguiente describe los objetivos de diseño de cada versión; consulte la hoja de ruta para ver el progreso.

### Funciones Destacadas

- Alarmas: alarmas únicas o repetidas. Las reglas de repetición cubren días laborables, el enésimo día de la semana del mes, el último día del mes, intervalos en días, fecha de fin y número de repeticiones, y se puede omitir solo la próxima vez.
- Sonido: tonos del sistema o audio local, texto leído en voz alta, volumen propio con aumento gradual, patrones de vibración personalizados, alerta a pantalla completa en la pantalla de bloqueo y posponer.
- Integración con scripts: una alarma puede registrar un archivo de script que AutoJs6 inicia cuando suena, de modo que el script no tiene que seguir en ejecución; un script en ejecución también puede escuchar en tiempo real los eventos de sonar, posponer y descartar.
- Temporizadores y cronómetro (1.1.0): varios temporizadores en paralelo que se pueden pausar, reanudar y ampliar, y que se restauran tras reiniciar; el cronómetro registra vueltas.
- Días festivos (1.2.0): datos abiertos de festivos integrados para muchas regiones, para omitir los festivos y sonar igualmente en los días laborables de recuperación; un gancho de script `beforeRing` también puede decidir si suena una alarma.
- Retos para apagar (1.3.0): cálculos, agitar el dispositivo, volver a escribir una frase o una condición de script, cada uno con una válvula de seguridad para que la alarma siempre se pueda apagar.

El progreso y el diseño están disponibles en la [página del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm).
