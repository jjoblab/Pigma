# Étude : la coloration syntaxique de CodeAssist et notre moteur par défaut

> **Note de recopie.** Document recopié tel quel depuis la source de la
> mission, avec une seule retouche : deux mentions d'un projet dont le nom
> ne se committent pas (règle des références interdites, importée du dépôt
> de référence) ont été neutralisées en « éditeur tiers ». L'original
> intégral vit hors du dépôt.

Source étudiée : dépôt `tyron12233/CodeAssist`, branche `main`, commit
`04ee33d` (lecture du 23 novembre 2026). Les conclusions portent sur ce que
j'ai lu : la documentation (`docs/language-support.md`,
`docs/editor-color-schemes.md`), les fichiers `LineTokens.kt`,
`LineStyles.kt`, `LargeFile.kt`, `LineRenderCache.kt` (en-tête),
`SemanticHighlight.kt` et la définition de `SyntaxFamily`. Je n'ai pas lu le
corps des scanners Java/Kotlin/XML, ni la fusion des couches au rendu, ni le
démon d'analyse. Rien n'a été exécuté ni mesuré sur appareil.

> **Licence.** CodeAssist est sous GPL v3 ; l'exception de liaison ne couvre
> que `plugin-api` et `platform-core`. On s'inspire des **idées et des
> algorithmes** ; on ne copie **aucun code**. Ce n'est pas un avis juridique.

---

## 1. Ce que CodeAssist fait réellement

Contrairement à ce qu'on pourrait supposer, CodeAssist n'utilise **ni
TextMate ni tree-sitter** pour colorer. Il superpose **deux couches**.

### Couche 1 — lexicale (rapide, synchrone, devine par la forme)
- Des **scanners écrits à la main**, un par *famille de syntaxe* : famille C
  (Java, Kotlin, AIDL, C++…), XML, Markdown, commentaires par `#`, texte brut.
  Une langue de la famille C se définit surtout par sa liste de mots-clés.
- Chaque ligne est découpée en **jetons** (début, fin, type) à partir de
  l'**état de fin de la ligne précédente**. L'état est un **petit entier** :
  code, commentaire de bloc, chaîne XML, chaîne brute Kotlin, bloc Markdown,
  commentaire de documentation, CDATA, bloc de texte Java.
- Les types de jetons sont des **formes lexicales** (mot-clé, chaîne,
  nombre, opérateur, parenthèse…) et non des concepts de langage. La couleur
  n'est choisie qu'au dessin : **changer de thème ne retokenise rien**.
- Le scanner **devine** : un mot en majuscule initiale est un type, un mot
  suivi de `(` est un appel.

### Couche 2 — sémantique (lente, asynchrone, dit la vérité)
- Un service du langage (Eclipse JDT pour Java, un hôte PSI pour Kotlin)
  analyse le fichier hors du fil d'interface, avec délai anti-rebond et
  annulation si une tâche plus prioritaire arrive (la complétion).
- Il renvoie une liste de **jetons sémantiques** : plage de texte, *nature*
  (classe, interface, méthode, paramètre, variable locale, constante…) et
  *modificateurs* (statique, extension, obsolète…). La nature est une
  **chaîne ouverte** : un langage peut en ajouter sans toucher au cœur.
- Ces jetons sont **superposés** à ceux de la couche 1 ; le sémantique gagne
  en cas de chevauchement. Ce que la couche 2 ne sait pas classer reste
  coloré par la couche 1 : le repli est toujours défendable.

---

## 2. Les mécanismes qui rendent le tout rapide

1. **Tokenisation paresseuse par fenêtre.** Ouvrir un fichier ne lexe rien.
   Seul le préfixe contigu jusqu'à la dernière ligne demandée est lexé, car
   l'état d'une ligne dépend de toutes les précédentes. Ouvrir un énorme
   fichier est en O(1) ; défiler ne lexe que jusqu'à la zone visible.
2. **Reprise incrémentale avec règle d'arrêt.** Après une édition, on relexe
   les lignes modifiées, puis on continue **tant que l'état de sortie d'une
   ligne change**. Une frappe ordinaire s'arrête après une ligne ; ouvrir un
   commentaire de bloc se propage jusqu'à ce que l'état se stabilise. Le code
   attribue lui-même cette règle à l'éditeur tiers (`AsyncIncrementalAnalyzeManager`).
3. **Tampons parallèles redimensionnés une seule fois.** Les insertions et
   suppressions de nombreuses lignes déplacent la queue des listes en une
   copie, pas une par ligne (collage massif : de plusieurs secondes à
   instantané).
4. **Numéro de révision unique par ligne.** Chaque relexage produit un
   numéro jamais réutilisé ; une ligne dont les jetons sont identiques après
   une propagation **garde** son numéro. Le cache de rendu valide une entrée
   par **une comparaison d'entier** : un résultat périmé est impossible.
5. **Cache de rendu par ligne, plus cache par contenu.** Un quart des lignes
   d'un vrai fichier sont identiques à une autre (`}`, lignes vides). Mesuré
   par les auteurs sur appareil : environ 0,32 ms sans cache contre 0,13 ms
   avec, quelle que soit la taille entre 16 et 256 entrées ; ils en retiennent 64.
6. **Plafonds anti-blocage.** Une ligne de plus de 20 000 colonnes n'est pas
   colorée ; une ligne de plus de 10 000 colonnes n'est dessinée que sur ces
   10 000 premières (cause d'ANR constatée sur des fichiers minifiés).
7. **Mode « gros fichier »** au-delà de 2,5 millions de caractères ou
   50 000 lignes : on coupe l'analyse, la coloration sémantique, le pliage,
   les indices en ligne ; **la coloration lexicale reste**.
8. **Thème en clés d'attribut avec chaîne de repli.** Un thème est une table
   creuse ; une clé absente signifie « pas d'avis ». Une clé résout par :
   la valeur du thème, sa valeur par défaut, puis celle de son **parent**,
   jusqu'à la couleur du texte. Les clés fines (`keyword.control` sous
   `keyword`) naissent **sans couleur propre** : ajouter une distinction ne
   repeint pas le code de personne. Tout est aplati une fois à la
   construction du thème.

---

## 3. Forces et limites, pour nous

### À retenir
- Aucun chargement de grammaire : **première peinture colorée quasi
  immédiate**, sans « texte blanc pendant plusieurs secondes ».
- La **règle d'arrêt** et les **révisions uniques** rendent l'incrémental
  simple et prouvable.
- La **séparation jeton / couleur** et le **repli par parent** rendent les
  thèmes bon marché.
- Les **plafonds** sont issus de plantages réels, pas de théorie.
- Le contrat sémantique **ouvert** (nature en chaîne) évite de modifier le
  cœur pour chaque langage.

### Limites à ne pas reproduire
| Limite | Conséquence |
| --- | --- |
| Un scanner à la main par famille | chaque langage nouveau = du code ; la famille C est une approximation |
| La couche 1 **devine** (majuscule = type) | couleur qui change quand la couche 2 arrive, ou qui reste fausse sans elle |
| État = **entier plat**, sans pile | impossible d'exprimer les imbrications : `${…}` imbriqués, HTML/JS injectés, here-documents |
| Lexage **sur le fil principal**, au dessin | acceptable pour un scanner linéaire simple, **pas** pour une grammaire plus riche |
| Préfixe contigu obligatoire | sauter à la ligne 40 000 coûte O(n) synchrone |
| Couche sémantique = analyseurs lourds (JDT, PSI) | inutilisable pour la longue traîne de langages |
| Je n'ai pas trouvé de tests du lexeur dans le dossier de tests du cœur de l'éditeur (à confirmer) | aucune garantie d'exactitude visible |

---

## 4. Ce que cela dit de `celeris`

Hypothèse à **vérifier par profilage**, non démontrée : le retard de
plusieurs secondes de `celeris` vient probablement du **chargement et de la
compilation des grammaires** (lecture, expressions régulières, démarrage à
froid), pas de la tokenisation ligne par ligne. CodeAssist évite ce coût par
construction, puisqu'il n'a pas de grammaires pilotées par des données.

Notre moteur doit donc garder la **vitesse de démarrage de CodeAssist**
(rien à charger ni à compiler à l'ouverture) avec la **rigueur d'IntelliJ**
(lexeurs compilés, analyseur, annotateur) ; voir §5.

---

## 5. Approche retenue : celle d'IntelliJ (Android Studio)

Décision du propriétaire : s'inspirer d'**IntelliJ**, la plateforme sous
Android Studio. TextMate et tree-sitter sortent du cœur ; ils pourront
revenir comme modules optionnels, ou ne jamais revenir.

### Ce que fait IntelliJ (documentation du SDK de la plateforme)
- **Trois niveaux** de coloration : le **lexeur**, l'**analyseur syntaxique**
  (qui produit un arbre), puis l'**annotateur** (qui s'appuie sur l'arbre et
  les index pour la sémantique et les erreurs).
- Le lexeur est **généré à la compilation** (JFlex) : chaque langage décrit
  ses règles, un outil les compile en code Java. Aucune expression régulière
  n'est interprétée à l'exécution.
- Le même lexeur sert la **coloration** et la **construction de l'arbre**.
- **Relexage incrémental** : le lexeur de coloration peut ne traiter que la
  partie modifiée. Son **état tient dans un seul entier** et, par défaut, la
  coloration ne **redémarre qu'aux jetons dont l'état est l'état initial** ;
  une interface dédiée permet de déclarer d'autres états redémarrables
  (utile pour les chaînes, gabarits, commentaires).
- La **couleur** n'est pas dans le lexeur : le lexeur produit un type de
  jeton, une table associe chaque type à une clé de style, le thème associe
  la clé à une couleur.

### Notre traduction

| Niveau IntelliJ | Notre couche | Où |
| --- | --- | --- |
| Lexeur généré | **L1** : lexeur compilé, un par langage | `:syntax` |
| Analyseur syntaxique | **L2** : analyseur tolérant écrit par nous, par langage, qui produit un arbre | `:analysis` |
| Annotateur | **L3** : classification sémantique et diagnostics | `:analysis` |

Plus de tree-sitter ni de TextMate dans le cœur : **aucune bibliothèque
native**, un APK plus léger, et un seul modèle mental du lexeur à
l'annotateur.

### Ce que cela coûte, honnêtement
- **Un lexeur par langage** : l'étendue de langages se paie en travail, pas
  en données téléchargeables. VS Code a choisi TextMate justement pour
  s'épargner cela.
- **Un analyseur tolérant par langage** pour L2 : c'est le chantier le plus
  lourd. Il n'est écrit que pour les langages qui le méritent (Java, Kotlin,
  XML, JSON d'abord).
- **Le build** doit savoir générer les lexeurs (voir §8, décisions 1 et 2).

### Comment on garde une large couverture malgré tout
1. **Une famille générique paramétrable** : un seul lexeur, configuré par
   des données (mots-clés, délimiteurs de chaînes, marques de commentaires,
   formes de nombres), pour la longue traîne (scripts, fichiers de
   configuration, langages proches du C). Aucune génération de code.
2. **Un paquet de langage** : un langage = un descripteur (extensions,
   lexeur, table jeton → portée, analyseur et annotateur facultatifs). On
   ajoute un langage sans toucher au cœur.
3. **Des lexeurs générés pour les langages importants**, un fichier de
   description par langage, avec des tests de référence.
4. **L'existant comme source d'idées** : les descriptions de lexeurs de la
   communauté IntelliJ servent de modèle. **Vérifier la licence fichier par
   fichier avant toute reprise** ; par défaut, réécrire.

### Principe de cohérence (inchangé)
> La couleur **certaine** arrive tout de suite et ne change jamais.
> La couleur **incertaine** arrive plus tard et remplace un neutre.

Le lexeur L1 **ne devine pas** : un identifiant est neutre. L2 puis L3 le
précisent (déclaration, type, appel…).

### Contrat du lexeur (identifiants en anglais)
- `start(texte, début, fin, étatInitial)`, `advance()`, type, début et fin du
  jeton courant, `getState()`.
- **L'état est un entier** qui décrit **tout** le contexte du lexeur. Quand
  le contexte est une pile (chaîne dans un gabarit dans une chaîne), la pile
  est **internée** : deux piles égales ont le même entier.
- `isRestartableState(état)` : déclaré par le langage. Un état est
  redémarrable si le lexeur peut reprendre avec cet entier seul.
- **Test obligatoire de redémarrage** : relexer depuis chaque état
  redémarrable doit redonner exactement les mêmes jetons qu'un lexage depuis
  le début (IntelliJ propose un test de ce genre pour ses lexeurs).

### Ce que nous reprenons de CodeAssist (couches autour du lexeur)
- **Tokenisation paresseuse** par fenêtre, **règle d'arrêt** (on s'arrête
  quand l'état de fin d'une ligne ne change plus), **révision unique par
  ligne** validant le cache de rendu, cache par contenu.
- **Points de contrôle d'état** toutes les ~256 lignes, calculés en arrière-
  plan : un saut lointain colore aussitôt, puis se corrige si l'état réel
  diffère.
- **Budget de temps** sur le fil d'interface ; le reste en arrière-plan,
  annulable à chaque édition, appliqué seulement si la version du texte
  correspond.
- **Plafonds** (ligne de 20 000 colonnes non colorée, dessin limité à 10 000
  colonnes, mode « gros fichier » au-delà de 2,5 M de caractères ou 50 000
  lignes : on coupe L2 et L3, jamais L1). VS Code applique le même plafond de
  20 000 colonnes.
- **Portées nommées** (`keyword.control`, `string.escape`…) et **thème** à
  clés avec repli par parent, clés fines sans couleur propre par défaut.

### L2 et L3
- **L2** : analyseur tolérant (ne lève jamais d'exception sur un texte à
  moitié tapé), qui produit un arbre dont les nœuds portent un genre ouvert
  (chaîne). **Reparsing incrémental** en ne reparsant que le plus petit bloc
  englobant réanalysable (corps de méthode, bloc `{ }`, élément XML), à la
  manière des éléments « paresseux » d'IntelliJ ; à étudier avant de fixer
  la découpe.
- **L3** : classification sémantique via un **contrat ouvert** (plage + nature
  en chaîne + modificateurs), alimentée par `:analysis`, et branchable aussi
  sur des jetons sémantiques venant d'un serveur de langage (`:lsp`).
- **Fusion** : L3 > L2 > L1. Sans L2 ni L3, L1 reste juste.

### Ce que nous ne reprenons pas
- Un scanner à la main par langage (remplacé par des lexeurs générés).
- L'état d'analyse plat sans pile.
- Le registre de langages global au processus.
- Toute analyse lourde sur le fil d'interface.
- TextMate et tree-sitter dans le cœur.

---

## 6. Exactitude et mesures (critères d'acceptation)

- **Fichiers de référence** par langage : sortie L1 figée, relue à chaque
  changement. Toute différence casse le test.
- **Test par propriété** : après une suite d'éditions aléatoires (insertion,
  suppression, collage, annulation, `\r\n`, texte hors plan multilingue
  de base, fin de fichier sans saut de ligne), le résultat incrémental
  **égale** une analyse complète.
- **Test de redémarrage** : relexer depuis chaque état redémarrable donne
  exactement les mêmes jetons qu'un lexage depuis le début du fichier.
- **Test des points de contrôle** : un saut lointain donne, une fois le
  passage en arrière-plan terminé, le même résultat qu'un lexage depuis le haut.
- **Test de stabilité** : une couleur L1 affichée ne change **jamais** quand
  L2 ou L3 arrive (seul un identifiant neutre peut se colorer).
- **Mesures sur appareil** (à consigner dans `docs/perf.md`) : première
  peinture colorée zone visible, recoloration d'une frappe, saut à la ligne
  40 000, ouverture d'un fichier de 50 000 lignes, ligne de 1 Mo.

---

## 7. Plan en étapes (chacune démontrée dans le `:sample`)

| Étape | Contenu | Visible dans le sample |
| --- | --- | --- |
| E0 | **Test de faisabilité de la génération** (voir §8) | décision écrite |
| E1 | Contrat `SyntaxEngine`, portées, thème avec repli, cache de révisions, contrat du lexeur | thème modifiable à chaud, sans retokenisation |
| E2 | L1 : lexeurs générés pour Java, XML, JSON ; famille générique paramétrable ; test de redémarrage | trois langages colorés instantanément + un langage de la famille générique |
| E3 | Tokenisation paresseuse, règle d'arrêt, points de contrôle, budget de temps, arrière-plan annulable | saut à la ligne 40 000 sans attente |
| E4 | Banc : fichiers de référence, propriétés, mesures | tableau de mesures dans le panneau de diagnostic |
| E5 | L2 : analyseur tolérant et reparsing incrémental (Java, XML, JSON, puis Kotlin) | identifiants qui se précisent sans toucher aux couleurs L1 |
| E6 | L3 : annotateur branché sur `:analysis` ; jetons sémantiques d'un serveur de langage (optionnel) | natures précises, modificateurs |
| E7 | Paquets de langages supplémentaires (générés ou famille générique) | langues ajoutées sans toucher au cœur |
| E8 | **Seulement si un besoin réel apparaît** : modules optionnels TextMate ou tree-sitter | à décider ; peut ne jamais exister |

---

## 8. Vérifications et décisions avant de coder

### Test de faisabilité (E0), à faire en premier
Générer un lexeur pour Java, XML et JSON avec un générateur de lexeurs, et
mesurer :
1. **La génération fonctionne sur le téléphone** du propriétaire (AndroidIDE
   ou Termux) : temps et mémoire. Sinon, la faire ailleurs (CI) et **ne
   commiter que le code généré**.
2. **Première peinture colorée** de la zone visible, fichier de 2 000 lignes.
3. **Poids** ajouté à l'APK par langage.
4. **Test de redémarrage** vert sur chaque lexeur.
5. **Exactitude** contre des fichiers de référence.

### Décisions à prendre, avec le propriétaire
1. **Générateur** : un générateur existant de lexeurs pour Java (licence à
   vérifier, outil de compilation uniquement) **ou** un générateur écrit par
   nous. Le premier va plus vite ; le second évite une dépendance de build.
2. **Code généré** : commité (le build sur téléphone reste léger ; il faut
   alors exclure ces fichiers des contrôles de langue et de commentaires, et
   vérifier qu'ils sont à jour) **ou** généré à chaque build.
3. **Langages de départ** : proposition Java, Kotlin, XML, JSON, plus la
   famille générique pour le reste.
4. **Reprise des descriptions IntelliJ** : autorisée seulement si la licence
   le permet fichier par fichier, avec attribution dans `NOTICE`.

### Autres vérifications
- **Profiler `celeris`** (démarrage à froid, une grammaire, ouverture d'un
  onglet) pour confirmer ou infirmer l'hypothèse du §4.
- **Lire le corps des scanners** de CodeAssist pour calibrer le test d'états
  à pile.
- **Étudier l'éditeur tiers de la règle d'arrêt** (LGPL 2.1 : idées seulement).
- **Mesurer** les valeurs de départ (budget par image, taille des points de
  contrôle, plafonds) sur un appareil milieu de gamme.
- **Fixer la convention de portées** avant d'écrire le thème (liste courte,
  documentée, versionnée).
