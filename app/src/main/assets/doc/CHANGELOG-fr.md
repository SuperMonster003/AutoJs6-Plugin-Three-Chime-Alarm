******

### Historique Des Versions

******

# v1.3.0

###### 2026/11/14

* `Note` Les défis d'arrêt réglés dans les écrans du plugin ne demandent pas AutoJs6; les régler dans un script (l'option dismiss des alarmes) et la condition de script demandent AutoJs6 6.8.1 (version interne 6641) ou ultérieure et ce plugin en version 1.3.0 ou ultérieure; avec le plugin 1.2.x ou antérieur, un appel avec l'option dismiss échoue avec un message invitant à mettre à jour le plugin
* `Note` Une alarme avec un défi d'arrêt peut toujours être arrêtée: l'arrêt automatique (au moins 30 minutes s'il n'est pas réglé sur l'alarme) et l'arrêt d'urgence (5 appuis sur une touche de volume en 4 secondes sur l'écran de sonnerie) restent disponibles; l'arrêt d'urgence peut être désactivé dans les paramètres, et seul l'arrêt automatique met alors fin à la sonnerie tant que le défi n'est pas réussi
* `Fonctionnalité` Défis d'arrêt: une alarme peut exiger de résoudre des calculs (facile, moyen ou difficile, 3 calculs par défaut), de secouer le téléphone (30 secousses par défaut) ou de saisir une phrase (la vôtre, ou une phrase intégrée tirée au hasard) avant de pouvoir être arrêtée, et peut autoriser ou non le report pendant le défi; le réglage se fait sur la ligne "Défi d'arrêt" de l'écran de modification, et le défi remplace le curseur d'arrêt sur l'écran de sonnerie
* `Fonctionnalité` Tant qu'un défi est actif, le bouton "Arrêter" de la notification et les demandes d'arrêt des autres applications n'arrêtent plus l'alarme directement mais ouvrent le défi sur l'écran de sonnerie; quand le report n'est pas autorisé, la notification n'affiche pas de bouton "Répéter"; sur un appareil sans accéléromètre, le défi de secousse devient un défi de calcul
* `Fonctionnalité` Soupapes de sécurité: quand une alarme avec défi ne règle pas l'arrêt automatique, celui-ci prend la plus longue des deux durées entre la valeur par défaut des paramètres et 30 minutes, et l'écran de sonnerie affiche le temps restant; 5 appuis sur une touche de volume en 4 secondes tant que l'écran de sonnerie est au premier plan (y compris sur l'écran de verrouillage) arrêtent l'alarme en urgence, et pendant un défi les touches de volume ne servent qu'à compter les appuis sans changer le volume; le nouveau groupe "Défis d'arrêt" des paramètres permet de désactiver l'arrêt d'urgence (activé par défaut)
* `Fonctionnalité` Condition de script: définie par un script AutoJs6, l'alarme sonne jusqu'à ce qu'un script l'arrête; quand l'appareil n'a pas été déverrouillé depuis un redémarrage, qu'il n'y a pas de script, qu'AutoJs6 n'est pas disponible, ou que les scripts n'ont pas pu démarrer ou sont terminés, l'alarme passe aussitôt au défi de repli choisi (un arrêt normal par défaut), et la raison est notée dans le journal du plugin
* `Fonctionnalité` Nouveautés du module de script AutoJs6: l'option dismiss des alarmes (le type de défi, la difficulté et le nombre de calculs, le nombre de secousses, le texte à saisir, le défi utilisé quand aucun script ne peut s'exécuter et l'autorisation du report), le challenge de alarms.ringing() et des événements ring / dismiss, et by: 'challenge' dans l'événement dismiss (demande AutoJs6 6.8.1; voir la [documentation d'AutoJs6](https://docs.autojs6.com/#/alarms))
* `Fonctionnalité` Les outils du plugin alarm_add et alarm_update acceptent l'option dismiss et peuvent régler les défis de calcul, de secousse et de saisie (sauf la condition de script)

# v1.2.0

###### 2026/11/13

* `Note` alarms.holidays, l'option holidays des alarmes et l'événement beforeRing dans les scripts demandent AutoJs6 6.8.1 (version interne 6641) ou ultérieure et ce plugin en version 1.2.0 ou ultérieure; avec le plugin 1.1.x ou antérieur, les appels échouent avec un message invitant à mettre à jour le plugin
* `Note` Les jours fériés et les jours ouvrés de récupération sont aussi exacts que les données en amont (le projet open source python-holidays); les années pas encore annoncées sont estimées d'après les règles et marquées "estimé", puis corrigées par une mise à jour des données après l'annonce
* `Fonctionnalité` Jours fériés: les alarmes gagnent les options "Sauter les jours fériés", "Seulement les jours fériés" et "Seulement les jours ouvrés" (sonne chaque jour ouvré, jours ouvrés de récupération compris, jamais le week-end ni les jours fériés); les données de 250 pays et régions (dont plus de 600 subdivisions) de 2020 à 2035 sont intégrées, avec les jours ouvrés de récupération de la Chine continentale et de Taïwan
* `Fonctionnalité` L'écran des paramètres choisit la région de jours fériés par défaut (déduite d'abord de la région du système), et chaque alarme peut avoir sa propre région; la liste des alarmes indique combien de jours fériés la prochaine sonnerie saute; les années estimées sont marquées "estimé", car des jours de congé ou de récupération peuvent encore être déplacés
* `Fonctionnalité` Les mises à jour des données des jours fériés sont recherchées chaque semaine (désactivable dans les paramètres; pas de téléchargement sur les réseaux limités par défaut) et peuvent être recherchées à la main dans les paramètres; la signature des données téléchargées est vérifiée et les données actuelles restent en service en cas d'échec; une année sans données, l'alarme sonne selon sa seule règle de répétition et ne se tait jamais pour cette raison
* `Fonctionnalité` Décision d'un script avant la sonnerie (beforeRing): un script enregistré est lancé avant la sonnerie (60 secondes par défaut, de 15 secondes à 5 minutes) et peut sauter cette sonnerie, ou la laisser sonner avec un autre libellé, son, texte lu ou vibreur pour cette fois; si le script ne décide pas à temps, échoue ou si AutoJs6 n'est pas disponible, l'alarme sonne comme prévu
* `Fonctionnalité` Nouveautés du module de script AutoJs6: alarms.holidays (lister les régions, savoir si un jour est férié ou ouvré, lister les jours fériés d'une année, consulter et actualiser les données), l'option holidays des alarmes, skip / ring de l'événement beforeRing, et les warnings de alarms.capabilities() sur les données manquantes et les années estimées (demande AutoJs6 6.8.1; voir la [documentation d'AutoJs6](https://docs.autojs6.com/#/alarms))
* `Fonctionnalité` Nouvel outil du plugin holiday_check pour savoir, via le plugin MCP Server ou plugins.tools.call, si un jour est férié ou ouvré dans une région et le nom du jour férié; alarm_add et alarm_update acceptent l'option holidays

# v1.1.0

###### 2026/11/12

* `Note` alarms.timers et alarms.stopwatch dans les scripts demandent AutoJs6 6.8.1 (version interne 6641) ou ultérieure et ce plugin en version 1.1.0 ou ultérieure; avec le plugin 1.0.x, les appels échouent avec un message invitant à mettre à jour le plugin
* `Fonctionnalité` Minuteurs: jusqu'à 20 minuteurs simultanés, avec une durée prédéfinie (de 1 minute à 1 heure) ou personnalisée (de 1 seconde à 100 heures) et un libellé facultatif; ils se mettent en pause, reprennent, se prolongent d'une minute et se réinitialisent, et sonnent comme les alarmes à la fin
* `Fonctionnalité` Un minuteur en cours affiche son compte à rebours dans le volet des notifications, d'où il peut être mis en pause, prolongé d'une minute ou annulé; les minuteurs continuent après un redémarrage, et un minuteur arrivé à échéance pendant que l'appareil était éteint sonne après le démarrage (avec plus de 5 minutes de retard, il ne laisse qu'une notification manquée)
* `Fonctionnalité` Chronomètre: démarrage, pause, reprise et réinitialisation, jusqu'à 100 tours, avec le tour le plus rapide et le plus lent signalés; pendant qu'il tourne, il s'affiche dans le volet des notifications, ce qui se désactive dans le groupe "Minuteurs et chronomètre" des paramètres
* `Fonctionnalité` La barre de navigation du bas passe entre les pages des alarmes, des minuteurs et du chronomètre
* `Fonctionnalité` Nouveaux alarms.timers et alarms.stopwatch dans le module de script AutoJs6 (demande AutoJs6 6.8.1): démarrage, consultation, pause, reprise, ajout ou retrait de temps, réinitialisation et annulation des minuteurs, scripts enregistrés sur un minuteur et lancés à la sonnerie et à d'autres événements, et contrôle du chronomètre avec ses tours (voir la [documentation d'AutoJs6](https://docs.autojs6.com/#/alarms))
* `Fonctionnalité` Nouveaux outils du plugin timer_start, timer_list et timer_control pour démarrer, lister, mettre en pause, reprendre, prolonger, réinitialiser et annuler des minuteurs via le plugin MCP Server ou plugins.tools.call
* `Fonctionnalité` Horloge du système: les demandes de minuteur sont aussi traitées; d'autres applications peuvent faire démarrer un minuteur au plugin, ouvrir l'écran des minuteurs ou arrêter un minuteur qui sonne

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
