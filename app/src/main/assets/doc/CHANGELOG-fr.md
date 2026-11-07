******

### Historique Des Versions

******

# v1.0.1

###### 2026/11/08

* `Correctif` Quand une autre application ou un script AutoJs6 envoyait coup sur coup deux demandes d'alarme du même type (par exemple deux nouvelles alarmes de suite, ou deux appels d'alarms.system.setAlarm visant ce plugin) alors qu'aucun écran du plugin n'était ouvert, la seconde demande était ignorée sans aucun message et son alarme n'était pas créée; chaque demande est désormais traitée; sous Android 7.0 - 8.1, cela nécessite l'autorisation "réorganiser les applications en cours d'exécution" (accordée à l'installation)

# v1.0.0

###### 2026/11/07

* `Note` L'activation dans le centre des plugins, les outils du plugin et le module de script alarms demandent AutoJs6 6.8.1 (version interne 6641) ou ultérieure; avec une version plus ancienne d'AutoJs6 (par exemple 6.8.0), le plugin ne s'utilise que comme application de réveil autonome
* `Note` Sur ColorOS (OPPO, OnePlus, realme), activez le démarrage automatique du plugin à la fois dans les infos de l'appli et dans Paramètres > Applis > Lancement automatique, sinon les alarmes ne sonnent plus une fois le processus du plugin nettoyé (voir README > FAQ)
* `Fonctionnalité` Alarmes: alarmes uniques ou répétées selon les règles de calendrier RFC 5545 (jours ouvrés, n-ième jour de la semaine du mois, fin du mois, intervalles en jours, date de fin et nombre d'occurrences); on peut exclure des dates ou ne sauter que la prochaine sonnerie
* `Fonctionnalité` Sonnerie: sonnerie du système ou audio local importé, texte lu à voix haute, volume indépendant avec montée progressive, vibration personnalisée, répétition et arrêt automatique, écran de sonnerie plein écran sur l'écran de verrouillage, et alarmes simultanées qui sonnent l'une après l'autre
* `Fonctionnalité` Planification: les alarmes sont replanifiées après un redémarrage, un changement d'heure ou de fuseau horaire et une mise à jour du plugin, sonnent avant le premier déverrouillage, et une alarme manquée sonne dans les 5 minutes ou laisse sinon une notification
* `Fonctionnalité` Écrans du plugin: liste des alarmes, écran de modification, écran de sonnerie, page des autorisations et paramètres; la langue, le mode nuit et la couleur du thème suivent AutoJs6 par défaut, et l'icône du lanceur est interchangeable
* `Fonctionnalité` Paramètres: sonnerie, vibration, répétition et arrêt automatique par défaut, format de l'heure, recherche automatique des mises à jour, et l'option "Rester actif en arrière-plan" (une notification silencieuse qui rend le nettoyage par le système moins probable)
* `Fonctionnalité` La page des autorisations vérifie une à une les notifications, les alarmes exactes, les alertes plein écran, l'optimisation de la batterie, les restrictions en arrière-plan et le démarrage automatique du fabricant, avec des lignes d'aide pour la liste du lancement automatique et les notifications en bannière sur ColorOS
* `Fonctionnalité` Module de script AutoJs6 alarms / $alarms (demande AutoJs6 6.8.1, Rhino uniquement): création, consultation, modification et suppression des alarmes, écouteurs d'événements, scripts enregistrés qu'AutoJs6 lance quand un événement survient, et liste des sonneries (voir la [documentation d'AutoJs6](https://docs.autojs6.com/#/alarms))
* `Fonctionnalité` Outils du plugin alarm_add, alarm_list, alarm_update, alarm_remove, alarm_set_enabled, alarm_skip_next, alarm_upcoming, alarm_dismiss et alarm_snooze, utilisables via le plugin MCP Server ou plugins.tools.call
* `Fonctionnalité` Horloge du système: traite les demandes de réglage, d'affichage, d'arrêt et de répétition d'alarme d'autres applications
* `Fonctionnalité` Un seul APK pour tous les appareils, y compris ceux à pages mémoire de 16 KB
* `Dépendance` Ajout de AndroidX Core KTX 1.15.0
* `Dépendance` Ajout de AndroidX Activity Compose 1.12.2
* `Dépendance` Ajout de Jetpack Compose BOM 2026.09.00 (écrans du plugin)
* `Dépendance` Ajout de Kotlin Coroutines Android 1.11.0
* `Dépendance` Ajout de Material Color Utilities 4.1.1 (couleurs du thème)
* `Dépendance` Ajout de lib-recur 0.17.1 (calcule les règles de répétition des alarmes)
* `Dépendance` Ajout de Desugar JDK Libs 2.1.5 (calculs de date et de fuseau horaire sous Android 7.x)
