3-Chime Alarm est une application de réveil, de minuteurs et de chronomètre qui fonctionne aussi seule, sans AutoJs6. Les alarmes peuvent se répéter selon des règles de calendrier, éviter les jours fériés ou ne sonner que ces jours-là, lire un texte à voix haute en sonnant et exiger qu'un défi soit relevé avant de pouvoir être arrêtées. Avec AutoJs6, les scripts peuvent gérer les alarmes et les minuteurs, et des scripts choisis peuvent démarrer automatiquement quand une alarme sonne ou qu'un autre événement survient.

### Prise en main

1. Ouvrez 3-Chime Alarm. Si l'écran d'accueil affiche l'avis "Les alarmes risquent de ne pas sonner correctement", touchez-le et traitez la page "Autorisations et arrière-plan" point par point.
2. Touchez le bouton "Nouvelle alarme" en bas à droite, réglez l'heure, la récurrence et le son, puis touchez "Enregistrer".
3. Passez à "Minuteurs" ou "Chronomètre" avec la navigation du bas; touchez le bouton "Paramètres" de la barre supérieure pour définir les valeurs par défaut comme le son, le vibreur et le report.
4. Pour utiliser ce plugin dans des scripts, activez-le dans le "Centre des plugins" d'AutoJs6.

### Fonctionnalités principales

- **Récurrence**: choisissez "Tous les jours", "Du lundi au vendredi", "Le week-end" ou "Choisir les jours"; la "Règle avancée" prend en charge des règles de calendrier comme le n-ième jour de la semaine du mois, la fin du mois ou un intervalle de jours, et des "Dates ignorées" peuvent être ajoutées.
- **Sonnerie**: utilisez une sonnerie du système ou un fichier audio local importé, et réglez le texte de "Lecture à voix haute", le vibreur et le report; un écran de sonnerie plein écran s'affiche sur l'écran de verrouillage, et les alarmes qui arrivent à échéance en même temps sonnent l'une après l'autre.
- **Jours fériés**: la ligne "Jours fériés" de la page de modification propose "Sauter les jours fériés", "Seulement les jours fériés" ou "Seulement les jours ouvrés" (jours ouvrés de récupération compris). Les données de 250 pays et régions sont intégrées, et les mises à jour sont vérifiées chaque semaine.
- **Défi d'arrêt**: la ligne "Défi d'arrêt" peut exiger de réussir "Calculs", "Secouer" ou "Saisir un texte" avant de pouvoir arrêter l'alarme. Chaque défi garde deux issues: "Arrêt automatique" et "Arrêt d'urgence".
- **Minuteurs et chronomètre**: jusqu'à 20 minuteurs fonctionnent en même temps et se contrôlent depuis la notification avec "Pause", "+1 min" ou "Annuler"; le chronomètre a un bouton "Tour" et marque le tour le plus rapide et le plus lent.
- **Ponctualité**: les alarmes sont reprogrammées automatiquement après un redémarrage ou un changement d'heure ou de fuseau horaire, et sonnent même avant le premier déverrouillage; une alarme manquée pendant que l'appareil était éteint sonne quand même s'il est rallumé dans les 5 minutes, sinon une notification reste.
- **Demandes d'horloge du système**: le plugin traite les demandes d'autres applications, comme les agendas et les assistants vocaux, pour régler une alarme, régler un minuteur ou arrêter une alarme.
- **Apparence**: "Langue", "Mode nuit" et "Couleur du thème" suivent AutoJs6 par défaut et peuvent aussi être réglés à part dans les paramètres; le réglage "Icône du lanceur" permet aussi de changer d'icône.
- **Outils du plugin**: créez, consultez et modifiez des alarmes, contrôlez les minuteurs et consultez les jours fériés via le plugin MCP Server ou `plugins.tools.call`.

### Utilisation dans les scripts

Dans AutoJs6, les scripts Rhino utilisent les alarmes et les minuteurs via le module global `alarms`. L'exemple ci-dessous crée une alarme à 7:30 les jours ouvrés, enregistre un script à lancer quand l'alarme est arrêtée, puis démarre un minuteur de 25 minutes:

```js
let a = alarms.add({ time: '07:30', repeat: [1, 2, 3, 4, 5], label: 'Réveil' });
console.log(a.repeatText, new Date(a.nextTrigger));
a.bind('./after-wake.js', { events: ['dismiss'] });

let t = alarms.timers.start('25m', { label: 'Pomodoro' });
console.log(t.id, new Date(t.endsAt).toLocaleTimeString());
```

- Les alarmes et les minuteurs créés par des scripts apparaissent aussi dans l'interface du plugin et peuvent y être modifiés; les scripts peuvent aussi lire et modifier les alarmes créées dans l'interface.
- Un script enregistré avec `bind` est lancé par AutoJs6 quand l'événement survient et n'a pas besoin de rester actif; un script en cours d'exécution peut écouter en temps réel avec `alarms.on('ring', ...)`.
- Les scripts Node.js et Bun n'ont pas de module `alarms`; ils peuvent utiliser `engines.execScript` pour lancer un script Rhino qui l'utilise.
- L'utilisation complète, dont les jours fériés, la décision d'un script avant la sonnerie et les défis d'arrêt, est décrite dans [Documentation AutoJs6 > alarms](https://docs.autojs6.com/#/alarms).

### Prérequis

- Android 7.0 ou version ultérieure. Les alarmes, les minuteurs et le chronomètre n'ont pas besoin d'AutoJs6.
- Le module `alarms` et les outils du plugin nécessitent AutoJs6 6.8.1 ou version ultérieure, avec ce plugin activé dans le "Centre des plugins".
- L'autorisation des alarmes exactes permet de sonner à l'heure, et sur Android 12 et 12L il faut autoriser "Alarmes et rappels" dans les paramètres du système; l'autorisation des notifications sert à afficher les sonneries et les minuteurs en cours.
- L'autorisation des alertes en plein écran affiche l'écran de sonnerie sur l'écran de verrouillage; l'autorisation de superposition est facultative et ouvre l'écran de sonnerie quand le système n'affiche pas les alertes en plein écran.
- Sur la page "Autorisations et arrière-plan", il est conseillé de désactiver l'optimisation de la batterie, de lever les restrictions en arrière-plan et d'autoriser le démarrage automatique, pour que le système n'arrête pas le plugin en arrière-plan.

### Questions fréquentes

**Sonnerie quand AutoJs6 n'est pas lancé**

Le plugin planifie lui-même les alarmes, qui sonnent normalement quand AutoJs6 n'est pas lancé. Les scripts enregistrés sont lancés par AutoJs6, que le plugin réveille; après un arrêt forcé d'AutoJs6, les scripts ne démarrent pas avant la réouverture d'AutoJs6.

**Le Centre des plugins d'AutoJs6 6.8.0 ne peut pas activer ce plugin**

Activer ce plugin dans le Centre des plugins nécessite AutoJs6 6.8.1 ou version ultérieure. Avec une version antérieure d'AutoJs6, le plugin reste utilisable comme application de réveil autonome.

**Alarmes qui ne sonnent pas à l'heure**

Touchez l'avis de l'écran d'accueil et traitez la page "Autorisations et arrière-plan" point par point; sous ColorOS, traitez aussi la ligne "Liste du lancement automatique" de la même page. Après un arrêt forcé du plugin, les alarmes dues ne sonnent pas; ouvrir le plugin une fois suffit à les rétablir.

**Défi d'arrêt impossible à réussir**

Quand l'écran de sonnerie est au premier plan (y compris sur l'écran de verrouillage), appuyer 5 fois sur une touche de volume en 4 secondes arrête l'alarme en urgence; cette fonction peut être désactivée dans les paramètres. Sinon, l'alarme s'arrête après la durée d'arrêt automatique, d'au moins 30 minutes si elle n'est pas réglée à part.

Les autorisations, les demandes d'horloge du système et les données des jours fériés sont détaillées sur la [page du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm).
