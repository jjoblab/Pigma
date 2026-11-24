# Décision 007 — le harnais d'ouverture vit dans le dépôt

**Date** : 24 novembre 2026. **Statut** : acceptée (reconstruction
après effacement).

## Contexte

Le harnais d'ouverture est né au moment du premier signalement de
fermeture à l'installation : éprouver le chemin d'ouverture de
l'application de démonstration sur machine virtuelle, sans appareil
ni émulateur, contre des stubs Android minimaux. Il a grandi avec
chaque chantier — le remplissage du fond, la structure, les plis,
l'IME, la recherche, le serveur de langage, les poignées — et c'est
lui qui a épinglé les plantages réels (l'ordre des écouteurs, la
fenêtre de transition du changement de langage, le remplacement
d'ensemble, la course du fil de fond).

Il vivait hors du dépôt, dans l'environnement de travail seul. Un
recyclage complet de cet environnement l'a effacé : le dépôt Git
tenait tout le code et toute la documentation, mais pas
l'instrument qui les vérifiait — la démonstration de chaque chantier
citait `scripts/open-harness/run.sh`, un chemin qui n'existait pas
dans le dépôt. Les leçons du passé (vérifier les outils AVANT de
croire à une régression du code) s'appliquaient à l'instrument
lui-même.

## Décision

1. **Le harnais entre au dépôt** à `scripts/open-harness/` : les
   stubs Android (`src/stubs/`), les scénarios (`src/OpenHarness.java`,
   dans le paquetage du sample), et le lanceur (`run.sh`). Le chemin
   que citent déjà les démonstrations de chantier devient réel.
2. **Les stubs restent minimaux et enregistrants** : le canevas
   enregistre les dessins (positions, échelles, découpages) au lieu
   de peindre ; la vue porte une horloge virtuelle et une file de
   différés que le harnais draine — rien ne tick sans lui, tout est
   déterministe ; le presse-papiers et les notifications se lisent.
   Les signatures suivent l'API Android réelle (c'est le contrat du
   bytecode) ; les extensions propres au harnais portent le préfixe
   `harness`.
3. **Le harnais est soumis à la politique du dépôt** : identifiants
   ASCII en anglais, commentaires en français, Javadoc sur les types
   publics — `check-code-policy` le contrôle comme le reste du code.
4. **La reconstruction est fidèle en intention, pas en code** : les
   scénarios couvrent les comportements documentés (les journaux de
   chantier les décrivent), les comptes attendus sont ceux des
   relevés (307 jetons pour la démo Kotlin, 18 095 pour le gros
   fichier, 624 002 pour le hors normes) — le code lui-même est
   réécrit, l'original étant perdu.

## Conséquences

- Un recyclage de l'environnement ne perd plus l'instrument : le
  clonage du dépôt le rend.
- Le lanceur cherche les jars du client LSP dans les caches Gradle
  (un `./gradlew check` préalable les y dépose) — la machine de
  build seule sait le lancer complet.
- Les stubs grossiront avec l'API utilisée : toute méthode Android
  nouvelle appelée par la vue doit d'abord exister au stub (leçon
  déjà consignée pour `drawCircle`).
- La numérotation des scénarios est continue et documentée à
  l'écran ; le code de sortie dit vert ou non.
