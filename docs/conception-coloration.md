# Conception du moteur de coloration

> **À valider par le propriétaire avant le chantier 3.** Une à deux
> pages, conformes au plan et à l'étude de conception : le moteur suit
> la voie des lexeurs compilés (JFlex, décision D1), trois niveaux
> L1/L2/L3, et les mécanismes rapides de l'étude autour du lexeur.

## 1. Le contrat du lexeur (L1)

Un lexeur par langage, généré à la construction depuis une description
(`syntax/src/main/lexers/*.flex`, commentaires en français) :

- `start(text, début, fin, étatInitial)`, `advance()`, type du jeton,
  début et fin courants, `getState()` — l'interface du banc E0
  (`bench/src/.../LexerApi.java`) en est l'embryon ;
- **l'état est un petit entier** qui décrit tout le contexte ; quand le
  contexte est une pile (gabarit dans chaîne), la pile est **internée** :
  deux piles égales, même entier ;
- `isRestartableState(état)` déclaré par langage — par défaut, seul
  l'état initial est redémarrable ; le test de redémarrage du banc est
  **obligatoire** pour chaque lexeur livré (relexer depuis chaque état
  redémarrable redonne les jetons du lexage complet) ;
- le lexeur ne devine pas : un identifiant est neutre. Les types de
  jeton sont sans couleur ; la table jeton → portée (une par langage)
  et le thème font le reste — changer de thème ne retokenise rien.

## 2. Les couches et leur fusion

- **L1** (ce chantier) : lexeurs générés par langage + **famille
  générique paramétrable** (mots-clés, délimiteurs, marques de
  commentaires, formes de nombres — de la donnée, aucune génération)
  pour la longue traîne.
- **L2** (`:analysis`) : analyseur tolérant par langage, arbre à genre
  ouvert, ne lève jamais sur un texte à moitié tapé, reparsing du plus
  petit bloc englobant réanalysable.
- **L3** (`:analysis`, puis `:lsp`) : annotateur sémantique — plage,
  nature en chaîne ouverte, modificateurs ; les jetons d'un serveur de
  langage s'y branchent.
- **Fusion : L3 > L2 > L1.** Sans L2 ni L3, L1 reste juste ; une couleur
  L1 affichée ne change jamais, seul un neutre peut se colorer.

## 3. Le découpage incrémental (autour du lexeur)

Repris de l'étude : **tokenisation paresseuse par fenêtre** (ouvrir ne
lexe que le préfixe jusqu'à la dernière ligne demandée) ; **règle
d'arrêt** après édition (on relexe les lignes touchées puis on continue
tant que l'état de fin de ligne change) ; **révision unique par ligne**
(valide le cache de rendu par comparaison d'entier, un résultat périmé
est impossible) ; cache par contenu (les lignes identiques se partagent
leur rendu) ; **points de contrôle d'état toutes les ~256 lignes** en
arrière-plan (un saut lointain colore aussitôt, se corrige ensuite) ;
**budget de temps sur le fil d'interface**, le reste en arrière-plan
**annulable**, les résultats ne s'appliquent que si la **version du
texte** (la révision du tampon, chantier 1) correspond ; **plafonds**
(ligne > 20 000 colonnes non colorée, dessin limité à 10 000, mode gros
fichier au-delà de 2,5 M caractères ou 50 000 lignes : L2 et L3
coupées, L1 jamais).

## 4. Portées et thème

Convention **courte, documentée, versionnée** — jeu initial :
`comment` (+ `comment.doc`), `string` (+ `string.escape`), `number`,
`keyword` (+ `keyword.control`), `operator`, `punct`, `ident` (neutre),
`tag`, `attribute`, `entity` pour XML, `key` pour JSON. Le thème est
une table creuse à **repli par parent** ; les clés fines naissent sans
couleur propre ; tout est aplati à la construction. Une clé absente
signifie « pas d'avis ».

## 5. Paquet de langage et extension

Un langage = un **descripteur** (extensions, lexeur, table jeton →
portée, analyseur et annotateur facultatifs), ajouté sans toucher au
cœur. La famille générique sert le bout de chaîne. Kotlin suit la voie
générée de Java après celui-ci.

## 6. Ce que le chantier 3 livre (étapes E1–E2 de l'étude)

Le contrat `SyntaxEngine`, les portées, le thème à repli, le cache de
révisions ; les lexeurs Java, XML, JSON générés (+ le test de
redémarrage et les sorties figées, transposés du banc) ; la famille
générique ; dans le sample : **trois langages colorés instantanément +
un langage de la famille générique, thème modifiable à chaud sans
retokenisation, et le panneau de diagnostic** (première peinture,
recoloration d'une frappe) qui prendra les mesures appareil de
`docs/perf.md`. La tokenisation paresseuse et les points de contrôle
(E3) suivent au chantier suivant.
