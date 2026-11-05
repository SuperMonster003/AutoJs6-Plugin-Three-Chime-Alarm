3-Chime Alarm est le plugin d'alarme d'AutoJs6 et aussi une application d'horloge complète qui fonctionne sans AutoJs6: alarmes, plusieurs minuteurs à rebours en parallèle et chronomètre. Les alarmes se répètent selon les règles de calendrier RFC 5545 (par exemple le dernier vendredi de chaque mois, ou tous les 3 jours à partir d'une date donnée), peuvent ignorer les jours fériés et peuvent lancer un script ou prévenir un script en cours d'exécution lorsqu'elles sonnent.

> Il s'agit d'une préversion de développement: le centre des plugins d'AutoJs6 peut détecter le plugin, mais les alarmes, les minuteurs, le chronomètre, les écrans du plugin et le module de script `alarms` ne sont pas encore livrés, et l'icône est une proposition qui peut encore changer. La suite décrit les objectifs de conception de chaque version; consultez la feuille de route pour l'avancement.

### Points Forts

- Alarmes: alarmes ponctuelles ou répétées. Les règles de répétition couvrent les jours ouvrés, le n-ième jour de la semaine du mois, le dernier jour du mois, les intervalles en jours, une date de fin et un nombre d'occurrences, et il est possible d'ignorer seulement la prochaine occurrence.
- Sonnerie: sonneries du système ou fichiers audio locaux, texte lu à voix haute, volume propre avec augmentation progressive, vibrations personnalisées, alerte plein écran sur l'écran de verrouillage et rappel différé (snooze).
- Liaison avec les scripts: une alarme peut enregistrer un fichier de script qu'AutoJs6 lance quand elle sonne, le script n'a donc pas besoin de rester actif; un script en cours d'exécution peut aussi écouter en temps réel les événements de sonnerie, de rappel différé et d'arrêt.
- Minuteurs et chronomètre (1.1.0): plusieurs minuteurs en parallèle qu'on peut mettre en pause, reprendre et prolonger, et qui sont rétablis après un redémarrage; le chronomètre enregistre les tours.
- Jours fériés (1.2.0): données ouvertes de jours fériés intégrées pour de nombreuses régions, pour ignorer les jours fériés tout en sonnant les jours ouvrés de récupération; un crochet de script `beforeRing` peut aussi décider si une alarme sonne.
- Défis d'arrêt (1.3.0): calcul, secouer l'appareil, recopier une phrase ou condition de script, chacun avec une soupape de sécurité pour qu'une alarme puisse toujours être arrêtée.

L'avancement et la conception sont disponibles sur la [page du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm).
