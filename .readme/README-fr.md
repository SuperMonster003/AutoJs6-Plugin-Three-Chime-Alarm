<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Réveil avec règles de répétition de calendrier, sonnerie parlée et scripts AutoJs6 déclenchés à la sonnerie</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Langues (Languages)

******

Le README.md actuel prend en charge les langues suivantes:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-en.md)
- Français [fr] # actuel
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ar.md)

******

### Introduction

******

3-Chime Alarm est le plugin de réveil d'AutoJs6 et aussi une application de réveil qui fonctionne seule, sans AutoJs6. Les alarmes se répètent selon les règles de calendrier RFC 5545 (par exemple le dernier vendredi de chaque mois, ou tous les 3 jours à partir d'une date); une alarme qui sonne peut lire un texte à voix haute, lancer un script ou prévenir un script en cours d'exécution. Les minuteurs et le chronomètre, le saut des jours fériés et les défis d'arrêt arriveront dans des versions ultérieures.

Le plugin programme et fait sonner les alarmes lui-même grâce aux alarmes exactes d'Android, si bien que les alarmes sonnent à l'heure même quand AutoJs6 n'est pas lancé. Les scripts AutoJs6 consultent, créent, modifient et écoutent les alarmes du plugin via le module global `alarms`; le sous-espace de noms `alarms.system` s'adresse directement à l'application d'horloge du système et fonctionne sans le plugin. Le plugin accepte aussi les demandes de réglage d'alarme envoyées par d'autres applications.

> La version 1.0.0 fournit les alarmes, les écrans du plugin, les outils du plugin et le module de script `alarms`. L'activation dans le centre des plugins, les outils du plugin et le module `alarms` demandent AutoJs6 6.8.1 (version interne 6641) ou ultérieure; avec une version plus ancienne d'AutoJs6 (y compris 6.8.0), le plugin ne s'utilise que comme application de réveil autonome. L'avancement des minuteurs, du chronomètre, des jours fériés et des défis d'arrêt figure dans la feuille de route.

******

### Points Forts

******

- Alarmes: alarmes uniques ou répétées. Les règles de répétition couvrent les jours ouvrés, le n-ième jour de la semaine du mois, la fin du mois, les intervalles en jours, une date de fin et un nombre d'occurrences; on peut exclure des dates ou ne sauter que la prochaine sonnerie.
- Sonnerie: sonnerie du système ou audio local importé, texte lu à voix haute, volume indépendant avec montée progressive, vibration personnalisée, répétition et arrêt automatique, écran de sonnerie plein écran sur l'écran de verrouillage; les alarmes dues en même temps sonnent l'une après l'autre.
- À l'heure: les alarmes sont replanifiées automatiquement après un redémarrage, un changement d'heure ou de fuseau horaire et une mise à jour du plugin, et sonnent même avant le premier déverrouillage; une alarme manquée appareil éteint sonne dans les 5 minutes, au-delà elle laisse seulement une notification.
- Scripts liés: une alarme peut enregistrer des fichiers de script qu'AutoJs6 lance lors de la sonnerie, de la répétition, de l'arrêt et d'autres événements, sans qu'ils restent en mémoire; un script en cours d'exécution peut aussi écouter ces événements en direct.
- Outils du plugin: créer, consulter, modifier et supprimer des alarmes et gérer une alarme qui sonne via le plugin MCP Server ou `plugins.tools.call` (neuf outils `alarm_*`).
- Horloge du système: le plugin peut traiter les demandes de réglage, d'affichage, d'arrêt et de répétition d'alarme envoyées par d'autres applications.
- Écrans: la langue, le mode nuit et la couleur du thème suivent AutoJs6 par défaut et peuvent être réglés séparément; l'icône du lanceur est interchangeable; l'écran des paramètres fixe la sonnerie, la vibration, la répétition et l'arrêt automatique par défaut.
- Minuteurs et chronomètre (prévus en 1.1.0): plusieurs minuteurs en parallèle avec pause, reprise et temps supplémentaire; tours pour le chronomètre.
- Jours fériés (prévus en 1.2.0): données ouvertes de jours fériés pour de nombreuses régions; les alarmes sautent les jours fériés et sonnent les jours ouvrés de récupération; un crochet de script `beforeRing` peut aussi décider de sonner ou non.
- Défis d'arrêt (prévus en 1.3.0): calcul, secousse, recopie d'une phrase ou condition de script, chacun avec une soupape de sécurité pour que l'alarme puisse toujours être arrêtée.

******

### Captures D'Écran

******

Captures prises sur un Sony XQ-DQ72; le thème clair en haut, le thème sombre en bas.

| Liste des alarmes | Modifier une alarme | Sonnerie | Paramètres |
| :---: | :---: | :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-light.png?raw=true" alt="list-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-light.png?raw=true" alt="edit-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-light.png?raw=true" alt="ring-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-light.png?raw=true" alt="settings-light" width="200" /> |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-dark.png?raw=true" alt="list-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-dark.png?raw=true" alt="edit-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-dark.png?raw=true" alt="ring-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-dark.png?raw=true" alt="settings-dark" width="200" /> |

******

### Utilisation

******

1. Téléchargez l'APK depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases) et installez-le. Il n'y a qu'un seul paquet pour tous les ABI.
2. Ouvrez le plugin et passez en revue la page des autorisations: notifications, alarmes exactes (à autoriser à la main sur Android 12 et 12L), alertes plein écran (parfois non accordées depuis Android 14), puis optimisation de la batterie, restrictions en arrière-plan et démarrage automatique du fabricant. Il s'utilise ensuite comme une application d'horloge ordinaire.
3. Pour utiliser le module `alarms` dans les scripts ou les outils du plugin, mettez à jour AutoJs6 vers 6.8.1 (version interne 6641) ou ultérieure et activez le plugin dans le centre des plugins (catégorie "Outils" de l'assistant d'installation).

> Le plugin prend en charge les appareils sous Android 7.0 (API 24) ou ultérieur. Il n'y a qu'un seul paquet pour tous les appareils; sa seule bibliothèque native provient de la boîte à outils d'interface Compose et est alignée sur des pages mémoire de 16 KB, il fonctionne donc aussi sur les appareils à pages de 16 KB.

******

### API De Script

******

Les scripts utilisent ces fonctions via le module global `alarms` (aussi `$alarms`, Rhino uniquement); la référence complète se trouve dans la [documentation d'AutoJs6](https://docs.autojs6.com/#/alarms):

```javascript
let a = alarms.add({ time: "07:30", repeat: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", label: "Wake up" });
a.bind("./after-wake.js", { events: ["dismiss"] });

alarms.add({ time: "18:00", repeat: "FREQ=MONTHLY;BYDAY=-1FR", label: "Monthly report" });
alarms.add({ time: "06:10", date: "2026-11-04", repeat: { freq: "daily", interval: 3 }, tag: "shift" });

alarms.on("ring", (e) => toast(e.label));

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

- `alarms.add(options)` crée une alarme et renvoie son instantané; `alarms.list`, `get`, `update`, `remove`, `setEnabled`, `skipNext` et `cancelSkip` gèrent toutes les alarmes du plugin, y compris celles créées dans ses écrans.
- `repeat` accepte une chaîne RRULE (par exemple `FREQ=MONTHLY;BYDAY=-1FR`), un tableau de numéros de jours ou un objet structuré; `alarms.preview(options)` liste les prochaines sonneries pour vérifier une règle.
- `alarms.bind(id, script, options)` (ou `bind` d'un instantané d'alarme) enregistre un script qu'AutoJs6 lance quand un événement survient; le script lit l'événement avec `alarms.currentEvent()`. `alarms.on('ring', listener)` écoute en direct pendant l'exécution d'un script.
- `alarms.ringing`, `dismiss` et `snooze` gèrent une alarme qui sonne; `alarms.ringtones.list()` liste les sonneries disponibles.
- `alarms.system` appelle directement l'application d'horloge du système (`setAlarm`, `setTimer`, `showAlarms`, `dismissAlarm`, `next`, etc.) et ne demande aucun plugin.

******

### Horloge Du Système

******

Le plugin peut servir d'application de réveil du système et traiter les demandes d'horloge standard d'autres applications (par exemple agendas, assistants vocaux et outils d'automatisation). L'application qui envoie la demande doit détenir l'autorisation `com.android.alarm.permission.SET_ALARM`:

- Régler une alarme (`SET_ALARM`): lit l'heure, le libellé, les jours, la vibration et la sonnerie; une demande qui saute l'interface est enregistrée directement, sinon un écran de modification prérempli s'ouvre.
- Afficher les alarmes (`SHOW_ALARMS`): ouvre la liste des alarmes.
- Arrêter une alarme (`DISMISS_ALARM`): termine une alarme qui sonne ou en répétition; si rien ne sonne, une alarme due dans les 24 heures saute cette sonnerie; si plusieurs alarmes correspondent, l'utilisateur choisit.
- Répéter (`SNOOZE_ALARM`): met l'alarme qui sonne en répétition.
- `alarms.system` fonctionne dans l'autre sens: un script envoie des demandes à l'application d'horloge par défaut du système (ce plugin ou une autre horloge) sans plugin; les différences entre les horloges des fabricants sont décrites dans la documentation d'AutoJs6.

******

### Compatibilité

******

Le plugin prend en charge Android 7.0 (API 24) et ultérieur. L'activation dans le centre des plugins, les outils du plugin et le module `alarms` demandent AutoJs6 6.8.1 (version interne 6641) ou ultérieure. Avec une version plus ancienne d'AutoJs6 (par exemple 6.8.0, version interne 6640), le centre des plugins indique qu'une version plus récente d'AutoJs6 est requise et ne peut pas activer le plugin, et les outils du plugin et le module `alarms` ne sont pas disponibles; le plugin reste utilisable comme application de réveil autonome et traite toujours les demandes de l'horloge du système. Le module `alarms` ne prend en charge que le moteur Rhino, et son `alarms.system` ne demande aucun plugin.

Vérifié sur ces appareils avant la sortie de la version 1.0.0 (données dans l'annexe D de la feuille de route):

| Appareil | Android | Résultat |
| --- | --- | --- |
| Sony XQ-AT72 | 12 (API 31) | Réussi: retard médian de sonnerie 44 ms; sonnerie sur l'écran de verrouillage, après un redémarrage, après un changement d'heure, de fuseau et d'heure d'été, outils du plugin appelés via MCP Server |
| Sony XQ-DQ72 | 13 (API 33) | Réussi: retard médian 13 ms; sonnerie plein écran sur l'écran de verrouillage, écran de sonnerie affiché environ 1,1 s après l'heure prévue quand le processus avait été arrêté |
| Redmi 22120RN86C (MIUI) | 13 (API 33) | Réussi: retard médian 40 ms; le système a arrêté plusieurs fois le processus du plugin après le démarrage, l'alarme a quand même sonné à l'heure |
| Xiaomi Pad 23046RP50C | 15 (API 35) | Réussi: retard médian 32 ms; sonne avec une notification flottante sans l'autorisation d'alerte plein écran, sonne à l'heure après un redémarrage avant le déverrouillage |
| OnePlus OPD2413 (ColorOS 15) | 15 (API 35) | Réussi (lancement automatique requis): retard médian 23 ms; une fois le plugin activé dans Paramètres > Applis > Lancement automatique, les alarmes sonnent à l'heure même après le nettoyage du processus (voir la FAQ) |
| Sony G8441 | 9 (API 28) | Réussi: retard médian 41 ms; sonnerie sur l'écran de verrouillage, sonne à l'heure après un redémarrage avant le déverrouillage |
| Émulateurs Android | 7.0 - 16 (API 24 - 36) | Réussi: parcours complet sur la version minimale, sonnerie avant le premier déverrouillage, pages mémoire de 16 Ko |

******

### FAQ

******

#### Puis-je l'utiliser sans AutoJs6?

Oui. Le plugin a des écrans d'alarme complets et fonctionne seul comme application de réveil. AutoJs6 n'est nécessaire que pour gérer les alarmes depuis des scripts, y réagir ou utiliser les outils du plugin.

#### Les alarmes sonnent-elles quand AutoJs6 n'est pas lancé?

Oui. Le plugin planifie et fait sonner les alarmes avec les alarmes exactes d'Android, sans dépendre du processus d'AutoJs6. Quand un script est enregistré, le plugin réveille AutoJs6 pour le lancer au moment de la sonnerie.

#### Pourquoi le centre des plugins d'AutoJs6 6.8.0 ne peut-il pas activer le plugin?

L'activation dans le centre des plugins, les outils du plugin et le module `alarms` demandent AutoJs6 6.8.1 (version interne 6641) ou ultérieure, alors que 6.8.0 correspond à la version interne 6640. Mettez à jour AutoJs6 pour l'activer; en attendant, le plugin s'utilise comme application de réveil autonome.

#### Une alarme n'a pas sonné à l'heure. Que vérifier?

Ouvrez d'abord la page des autorisations du plugin, vérifiez que les notifications et les alarmes exactes sont autorisées, puis désactivez l'optimisation de la batterie, levez les restrictions en arrière-plan et autorisez le démarrage automatique comme indiqué. Le mode Ne pas déranger laisse passer les alarmes par défaut; si vous avez modifié ses réglages, vérifiez que les alarmes sont toujours autorisées. Une alarme manquée parce que l'appareil était éteint ou que le système l'a retardée sonne dans les 5 minutes; au-delà, elle reste silencieuse et laisse une notification.

#### Les alarmes ne sonnent pas sur OPPO, OnePlus ou realme (ColorOS). Que faire?

ColorOS a deux réglages distincts, et les deux doivent être activés pour 3-Chime Alarm: les interrupteurs démarrage automatique, lancement associé et activité en arrière-plan dans les infos de l'appli, et la liste Paramètres > Applis > Lancement automatique. Avec seulement les premiers, le système bloque les alarmes dues une fois le processus du plugin nettoyé (par exemple balayé dans les tâches récentes). Avec des scripts liés, AutoJs6 doit aussi être activé aux deux endroits. Ne balayez pas le plugin dans les tâches récentes; vous pouvez l'y verrouiller. Sur ColorOS, la page des autorisations du plugin propose les lignes d'aide correspondantes, et "Rester actif en arrière-plan" dans les paramètres garde une notification silencieuse pour rendre le nettoyage moins probable, au prix d'une consommation légèrement plus élevée.

#### Sur ColorOS, un appareil verrouillé ne fait que jouer la sonnerie et afficher une notification, sans écran de sonnerie. Pourquoi?

ColorOS désactive les notifications "Bannière" des applications nouvellement installées, et le système retient alors l'écran de sonnerie plein écran. Activez "Bannière" dans les réglages de notification du plugin; la ligne "Notifications en bannière" de la page des autorisations y mène directement. Autoriser "Affichage par-dessus les autres applis" sur la page des autorisations sert aussi de solution de secours.

#### Les alarmes ne sonnent plus depuis que j'ai forcé l'arrêt du plugin?

"Forcer l'arrêt" fait effacer par le système toutes les planifications d'alarmes du plugin, et rien ne sonne tant qu'il reste arrêté; c'est une règle d'Android. Ouvrez le plugin une fois depuis le lanceur pour rétablir les planifications; les alarmes en retard de moins de 5 minutes sonnent alors.

#### Xiaomi (MIUI, HyperOS) demande-t-il des réglages particuliers?

Sur un appareil de test Redmi (MIUI), le système a arrêté plusieurs fois le processus du plugin après le démarrage, puis l'a relancé à l'heure prévue et l'alarme a sonné à l'heure. Il reste conseillé de régler le démarrage automatique et l'économiseur de batterie comme l'indique la page des autorisations.

#### Les alarmes sonnent-elles après un redémarrage avant le déverrouillage? Et les scripts liés?

Les alarmes sonnent, car le plugin fonctionne avant le premier déverrouillage. Les scripts liés ne sont pas lancés, car AutoJs6 ne peut pas exécuter de scripts avant le premier déverrouillage, et ils ne sont pas lancés après coup non plus.

******

### Permissions Et Sécurité

******

Autorisations demandées par le plugin et leur usage:

- Alarmes exactes (`USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` sur Android 12 et 12L): sonner à l'heure. Accordée à l'installation depuis Android 13; sur Android 12 et 12L, autorisez "Alarmes et rappels" dans les paramètres du système.
- Notifications (`POST_NOTIFICATIONS`): afficher les alarmes qui sonnent et les alarmes manquées. Sans elle, les alarmes sonnent quand même, mais sans notification ni écran de sonnerie.
- Alertes plein écran (`USE_FULL_SCREEN_INTENT`): afficher l'écran de sonnerie sur l'écran de verrouillage. Parfois non accordée depuis Android 14; sans elle, les alarmes sonnent avec une notification flottante.
- Affichage par-dessus les autres applis (`SYSTEM_ALERT_WINDOW`, facultatif): ouvre l'écran de sonnerie quand le système retient l'alerte plein écran (par exemple avec les notifications en bannière de ColorOS désactivées); accordée seulement par l'utilisateur depuis la page des autorisations, au besoin.
- Démarrage (`RECEIVE_BOOT_COMPLETED`): replanifie après un redémarrage pour sonner même avant le premier déverrouillage.
- Service de premier plan (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`): maintient le son pendant la sonnerie; sert aussi à rester actif quand "Rester actif en arrière-plan" est activé dans les paramètres (désactivé par défaut).
- Vibration, verrou de réveil et réglages audio (`VIBRATE`, `WAKE_LOCK`, `MODIFY_AUDIO_SETTINGS`): vibrer, garder l'appareil éveillé pendant la sonnerie et rétablir le volume d'alarme indépendant ensuite.
- Réseau (`INTERNET`, `ACCESS_NETWORK_STATE`): uniquement pour la recherche de mises à jour de l'écran des paramètres (à partir de 1.2.0, aussi pour actualiser les données des jours fériés). La recherche n'a lieu qu'à l'ouverture de la liste des alarmes, à la fréquence choisie, et évite par défaut les réseaux limités; la planification, la sonnerie et le lien avec AutoJs6 n'utilisent jamais le réseau.
- Autorisation de plugin AutoJs6 (`org.autojs.permission.PLUGIN`): communiquer avec AutoJs6.

Le plugin réduit autant que possible ses autorisations:

- Entrées protégées: le service de capacité et l'entrée d'activation exigent l'autorisation de plugin d'AutoJs6 (`org.autojs.permission.PLUGIN`), les autres applications ne peuvent donc pas les appeler directement; l'entrée qui reçoit les demandes de l'horloge système exige que l'appelant détienne `com.android.alarm.permission.SET_ALARM`.
- Réseau: utilisé uniquement pour rechercher les mises à jour du plugin (sur GitHub; la recherche automatique peut être désactivée dans les paramètres); les alarmes et les données personnelles ne sont jamais envoyées.
- Stockage: les alarmes sont enregistrées dans le répertoire privé du plugin et exclues de la sauvegarde dans le cloud; les sonneries locales sont importées via le sélecteur de fichiers du système, sans autorisation de stockage.
- Accès des scripts: via le module `alarms`, les scripts peuvent lire et modifier toutes les alarmes du plugin, y compris celles créées depuis ses écrans; les scripts enregistrés sont stockés dans AutoJs6, et les écrans du plugin se contentent d'indiquer qu'une alarme a un script lié et de permettre de supprimer ce lien.

******

### Interface Du Plugin

******

Les informations suivantes s'adressent aux développeurs de l'hôte AutoJs6 et de plugins; l'hôte utilise ces identifiants pour découvrir le plugin et négocier les capacités:

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

### Feuille De Route

******

Les plans, décisions de conception et l'avancement sont suivis sous forme de liste à cocher dans ROADMAP.md. Les éléments non cochés sont des intentions, pas des fonctionnalités de la version actuelle.

- [Voir ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/ROADMAP.md)

******

### Historique Des Versions

******

#### v1.0.0

_2026/11/07_

- `Note` L'activation dans le centre des plugins, les outils du plugin et le module de script alarms demandent AutoJs6 6.8.1 (version interne 6641) ou ultérieure; avec une version plus ancienne d'AutoJs6 (par exemple 6.8.0), le plugin ne s'utilise que comme application de réveil autonome
- `Note` Sur ColorOS (OPPO, OnePlus, realme), activez le démarrage automatique du plugin à la fois dans les infos de l'appli et dans Paramètres > Applis > Lancement automatique, sinon les alarmes ne sonnent plus une fois le processus du plugin nettoyé (voir README > FAQ)
- `Fonctionnalité` Alarmes: alarmes uniques ou répétées selon les règles de calendrier RFC 5545 (jours ouvrés, n-ième jour de la semaine du mois, fin du mois, intervalles en jours, date de fin et nombre d'occurrences); on peut exclure des dates ou ne sauter que la prochaine sonnerie
- `Fonctionnalité` Sonnerie: sonnerie du système ou audio local importé, texte lu à voix haute, volume indépendant avec montée progressive, vibration personnalisée, répétition et arrêt automatique, écran de sonnerie plein écran sur l'écran de verrouillage, et alarmes simultanées qui sonnent l'une après l'autre
- `Fonctionnalité` Planification: les alarmes sont replanifiées après un redémarrage, un changement d'heure ou de fuseau horaire et une mise à jour du plugin, sonnent avant le premier déverrouillage, et une alarme manquée sonne dans les 5 minutes ou laisse sinon une notification
- `Fonctionnalité` Écrans du plugin: liste des alarmes, écran de modification, écran de sonnerie, page des autorisations et paramètres; la langue, le mode nuit et la couleur du thème suivent AutoJs6 par défaut, et l'icône du lanceur est interchangeable
- `Fonctionnalité` Paramètres: sonnerie, vibration, répétition et arrêt automatique par défaut, format de l'heure, recherche automatique des mises à jour, et l'option "Rester actif en arrière-plan" (une notification silencieuse qui rend le nettoyage par le système moins probable)
- `Fonctionnalité` La page des autorisations vérifie une à une les notifications, les alarmes exactes, les alertes plein écran, l'optimisation de la batterie, les restrictions en arrière-plan et le démarrage automatique du fabricant, avec des lignes d'aide pour la liste du lancement automatique et les notifications en bannière sur ColorOS
- `Fonctionnalité` Module de script AutoJs6 alarms / $alarms (demande AutoJs6 6.8.1, Rhino uniquement): création, consultation, modification et suppression des alarmes, écouteurs d'événements, scripts enregistrés qu'AutoJs6 lance quand un événement survient, et liste des sonneries (voir la [documentation d'AutoJs6](https://docs.autojs6.com/#/alarms))
- `Fonctionnalité` Outils du plugin alarm_add, alarm_list, alarm_update, alarm_remove, alarm_set_enabled, alarm_skip_next, alarm_upcoming, alarm_dismiss et alarm_snooze, utilisables via le plugin MCP Server ou plugins.tools.call
- `Fonctionnalité` Horloge du système: traite les demandes de réglage, d'affichage, d'arrêt et de répétition d'alarme d'autres applications
- `Fonctionnalité` Un seul APK pour tous les appareils, y compris ceux à pages mémoire de 16 KB
- `Dépendance` Ajout de AndroidX Core KTX 1.15.0
- `Dépendance` Ajout de AndroidX Activity Compose 1.12.2
- `Dépendance` Ajout de Jetpack Compose BOM 2026.09.00 (écrans du plugin)
- `Dépendance` Ajout de Kotlin Coroutines Android 1.11.0
- `Dépendance` Ajout de Material Color Utilities 4.1.1 (couleurs du thème)
- `Dépendance` Ajout de lib-recur 0.17.1 (calcule les règles de répétition des alarmes)
- `Dépendance` Ajout de Desugar JDK Libs 2.1.5 (calculs de date et de fuseau horaire sous Android 7.x)

##### Pour plus d'historique des versions

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

******

### Compilation

******

Cette section s'adresse aux développeurs souhaitant compiler le plugin depuis les sources.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### Licence

******

Le code du projet est distribué sous [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE). Les composants et données tiers sont répertoriés dans les mentions tierces.

- [Mentions tierces (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/THIRD_PARTY_NOTICES.md)

******

### Liens

******

- Projet AutoJs6: https://github.com/SuperMonster003/AutoJs6
