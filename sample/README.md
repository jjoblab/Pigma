# L'application de démonstration (`:sample`)

L'hôte de démonstration — et la PREUVE de l'intégration minimale :
l'écran ne câble plus rien du serveur de langage (la façade
`LspEditor` de `:editor` le porte), il ne garde que ses boutons, son
panneau de mesures et sa mise en page. Android pur (l'activité ne
déclare que la bibliothèque d'édition).

## Ce que l'écran montre

- **Les sélecteurs de langage** : Java, XML, JSON, Kotlin, Lua
  généré, SQL générique ; le gros fichier (1 780 lignes) et le hors
  normes (52 000 lignes) éprouvent la fenêtre paresseuse et le mode
  gros fichier.
- **Le thème** : clair, sombre — basculer ne retokenise rien.
- **La recherche et l'aller-à** : les cartes dessinées dans la vue.
- **Les boutons LSP** : la démonstration (le serveur de langage au
  même processus, les réponses du serveur prime sur l'index local) et
  EmmyLua réel (le serveur Lua hors processus, embarqué en arm64,
  l'espace de travail dans le cache).
- **Le panneau de mesures** : première coloration, coût de demande,
  coût de frappe, lignes lexées, trame — le journal du scénario se
  copie au presse-papiers en tableau prêt à coller (le relevé
  appareil de `docs/perf.md`).
- **Le style des popups** : l'hôte pose le sien (fond translucide,
  coins plus ronds, largeur accrue) — la démonstration de l'API.
- **Les intentions de l'hôte** : l'ampoule jaune de la gouttière
  porte la date du jour et la recherche — la démonstration de
  `setQuickActionProvider`.

## La construction

    ./gradlew :sample:assembleDebug     # l'APK de démonstration
    ./gradlew :sample:assembleRelease    # la version de mesure (signée debug)

L'APK n'embarque que arm64-v8a (le serveur natif EmmyLua y descend
seul). Le serveur de démonstration (`DemoLanguageServer`) est le
serveur Java au même processus : parenthèses non appariées en
diagnostics, mots-clefs documentés en survols, complétion et
signature de la démo Java.

## Les tests

Le harnais d'ouverture (`scripts/open-harness/`) compile TOUT le
dépôt (le sample compris) contre les stubs Android et déroule les
scénarios sur cette activité réelle : 36 scénarios, la sortie non
nulle échoue. `bash scripts/open-harness/run.sh`.
