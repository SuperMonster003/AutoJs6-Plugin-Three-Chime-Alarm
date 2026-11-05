<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Réveil, minuteurs et chronomètre avec règles de répétition de calendrier, saut des jours fériés et scripts déclenchés à la sonnerie</p>

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

3-Chime Alarm est le plugin d'alarme d'AutoJs6 et aussi une application d'horloge complète qui fonctionne sans AutoJs6: alarmes, plusieurs minuteurs à rebours en parallèle et chronomètre. Les alarmes se répètent selon les règles de calendrier RFC 5545 (par exemple le dernier vendredi de chaque mois, ou tous les 3 jours à partir d'une date donnée), peuvent ignorer les jours fériés et peuvent lancer un script ou prévenir un script en cours d'exécution lorsqu'elles sonnent.

Le plugin programme et fait sonner les alarmes lui-même grâce aux alarmes exactes d'Android, si bien que les alarmes sonnent à l'heure même quand AutoJs6 n'est pas lancé. Les scripts AutoJs6 consultent, créent, modifient et écoutent les alarmes du plugin via le module global `alarms`; le sous-espace de noms `alarms.system` s'adresse directement à l'application d'horloge du système et fonctionne sans le plugin. Le plugin accepte aussi les demandes de réglage d'alarme envoyées par d'autres applications.

> Il s'agit d'une préversion de développement: le centre des plugins d'AutoJs6 peut détecter le plugin, mais les alarmes, les minuteurs, le chronomètre, les écrans du plugin et le module de script `alarms` ne sont pas encore livrés, et l'icône est une proposition qui peut encore changer. La suite décrit les objectifs de conception de chaque version; consultez la feuille de route pour l'avancement.

******

### Points Forts

******

- Alarmes: alarmes ponctuelles ou répétées. Les règles de répétition couvrent les jours ouvrés, le n-ième jour de la semaine du mois, le dernier jour du mois, les intervalles en jours, une date de fin et un nombre d'occurrences, et il est possible d'ignorer seulement la prochaine occurrence.
- Sonnerie: sonneries du système ou fichiers audio locaux, texte lu à voix haute, volume propre avec augmentation progressive, vibrations personnalisées, alerte plein écran sur l'écran de verrouillage et rappel différé (snooze).
- Liaison avec les scripts: une alarme peut enregistrer un fichier de script qu'AutoJs6 lance quand elle sonne, le script n'a donc pas besoin de rester actif; un script en cours d'exécution peut aussi écouter en temps réel les événements de sonnerie, de rappel différé et d'arrêt.
- Minuteurs et chronomètre (1.1.0): plusieurs minuteurs en parallèle qu'on peut mettre en pause, reprendre et prolonger, et qui sont rétablis après un redémarrage; le chronomètre enregistre les tours.
- Jours fériés (1.2.0): données ouvertes de jours fériés intégrées pour de nombreuses régions, pour ignorer les jours fériés tout en sonnant les jours ouvrés de récupération; un crochet de script `beforeRing` peut aussi décider si une alarme sonne.
- Défis d'arrêt (1.3.0): calcul, secouer l'appareil, recopier une phrase ou condition de script, chacun avec une soupape de sécurité pour qu'une alarme puisse toujours être arrêtée.

******

### Utilisation

******

1. Téléchargez l'APK depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases) et installez-le. Il n'existe qu'un seul paquet pour toutes les ABI.
2. Ouvrez le plugin et accordez les autorisations de notification, d'alarme exacte et d'alerte plein écran lorsqu'elles sont demandées; il fonctionne ensuite comme une application d'horloge ordinaire.
3. Pour utiliser le module `alarms` dans les scripts, mettez à jour AutoJs6 vers la version interne 6641 (6.8.1) ou ultérieure et activez le plugin dans le centre des plugins.

> Le plugin prend en charge les appareils sous Android 7.0 (API 24) ou ultérieur. Il n'y a qu'un seul paquet pour tous les appareils; sa seule bibliothèque native provient de la boîte à outils d'interface Compose et est alignée sur des pages mémoire de 16 KB, il fonctionne donc aussi sur les appareils à pages de 16 KB.

******

### API De Script

******

Les scripts utilisent ces fonctions via le module global `alarms` (également disponible sous le nom `$alarms`) (projet de conception de la 1.0.0; la documentation de la version publiée fait foi):

```javascript
let a = alarms.add({ time: "07:30", repeat: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", label: "Wake up" });
a.bind("./after-wake.js", { events: ["dismiss"] });

alarms.add({ time: "18:00", repeat: "FREQ=MONTHLY;BYDAY=-1FR", label: "Monthly report" });
alarms.add({ time: "06:10", date: "2026-11-04", repeat: { freq: "daily", interval: 3 }, tag: "shift" });

alarms.on("ring", (e) => toast(e.label));

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

- `alarms.add(options)` crée une alarme et renvoie un objet alarme; `alarms.list`, `get`, `update`, `remove`, `setEnabled` et `skipNext` gèrent toutes les alarmes du plugin, y compris celles créées depuis ses écrans.
- `repeat` accepte une chaîne RRULE (par exemple `FREQ=MONTHLY;BYDAY=-1FR`) ou un objet structuré; `alarms.preview(options)` liste les prochaines heures de sonnerie pour vérifier une règle.
- `alarm.bind(script, { events })` enregistre un script qu'AutoJs6 lance quand l'alarme sonne, l'événement étant disponible via `alarms.currentEvent()`; `alarms.on('ring', listener)` écoute pendant l'exécution du script.
- `alarms.system` appelle directement l'application d'horloge du système (`setAlarm`, `setTimer`, `showAlarms`, `next` et d'autres) et fonctionne sans le plugin.

******

### FAQ

******

#### Puis-je l'utiliser sans AutoJs6?

Oui. Le plugin dispose d'écrans d'horloge complets et fonctionne seul comme réveil, minuteur et chronomètre. AutoJs6 n'est nécessaire que lorsque des scripts gèrent les alarmes ou y réagissent.

#### Les alarmes sonnent-elles quand AutoJs6 n'est pas lancé?

Oui. Le plugin programme et fait sonner les alarmes avec les alarmes exactes d'Android et ne dépend pas du processus d'AutoJs6. Lorsqu'un script est enregistré, le plugin réveille AutoJs6 pour le lancer à l'heure de la sonnerie.

#### Pourquoi le plugin a-t-il besoin des autorisations d'alarme exacte et d'alerte plein écran?

Les alarmes exactes garantissent la précision des heures de sonnerie (les applications de réveil reçoivent cette autorisation dès l'installation), l'alerte plein écran affiche l'écran de sonnerie sur l'écran de verrouillage et les notifications affichent l'alarme qui sonne. Si le système retire l'une d'elles, la page des autorisations du plugin explique comment la réactiver.

******

### Permissions Et Sécurité

******

Le plugin réduit autant que possible ses autorisations:

- Entrées protégées: le service de capacité et l'entrée d'activation exigent l'autorisation de plugin d'AutoJs6 (`org.autojs.permission.PLUGIN`), les autres applications ne peuvent donc pas les appeler directement; l'entrée qui reçoit les demandes de l'horloge système exige que l'appelant détienne `com.android.alarm.permission.SET_ALARM`.
- Réseau: la version 1.0.0 ne demande aucune autorisation réseau; les alarmes, les minuteurs et le chronomètre fonctionnent entièrement sur l'appareil.
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

_2026/11/05_

- `Note` Préversion de développement: le centre des plugins d'AutoJs6 peut détecter le plugin; les alarmes, les minuteurs, le chronomètre, les écrans du plugin et le module de script alarms ne sont pas encore livrés
- `Note` Nécessite AutoJs6 6.8.1 (6641) ou ultérieur (la version minimale exacte sera renseignée une fois les modifications de l'hôte intégrées)
- `Note` L'icône actuelle est une proposition et peut encore changer
- `Fonctionnalité` Squelette du plugin: protocole d'activation Wake, service d'identité du centre des plugins et service de capacité d'alarme
- `Fonctionnalité` Un seul APK pour tous les appareils, y compris ceux à pages mémoire de 16 KB
- `Fonctionnalité` README, instructions du centre des plugins et journaux des modifications en 10 langues, générés à partir de sources JSON
- `Dépendance` Ajout de AndroidX Core KTX 1.15.0
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
