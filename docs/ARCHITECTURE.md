# Architecture

Pigma est une bibliothèque d'éditeur de code embarquable pour Android. La
règle cardinale : **peu de modules, découpés par fonctionnalité**, pour
qu'une application hôte n'ajoute qu'une ou deux dépendances. Les
conventions communes vivent dans le `build.gradle.kts` racine — aucun
`build-logic`, aucun `buildSrc`, aucun `includeBuild`.

## Modules cibles (six au maximum)

| Module | Nature | Contenu | État |
| --- | --- | --- | --- |
| `:core` | JVM pur | modèle de texte (tampon à trou, positions UTF-16, lignes, transactions, annulation), contrats communs | **présent** |
| `:syntax` | JVM pur | moteur de coloration : contrat `SyntaxEngine`, lexeurs compilés par langage, famille générique paramétrable, portées, thème, cache de révisions | prévu |
| `:analysis` | JVM pur | analyseur tolérant (L2), annotateurs sémantiques (L3), symboles, index, diagnostics, complétion de base | prévu |
| `:editor` | bibliothèque Android | vue d'édition (Canvas), saisie/IME, sélection, défilement, gouttière, pliage, popups, façade hôte | prévu |
| `:lsp` | optionnel | client LSP (types du domaine, session, transport) | prévu |
| `:sample` | application | instrument de test du projet, jamais publiée | **présent** |

Aucun module n'est créé avant que la fonctionnalité qui le justifie
n'arrive. Le cœur du modèle de texte est en place ; le modèle de jetons et
de spans rejoindra `:core` avec le moteur de coloration.

## Le cas d'une application hôte

- cas courant : une seule dépendance, `:editor` (qui expose en `api` ce
  qu'il faut de `:core`, `:syntax` et `:analysis`) ;
- optionnel : `:lsp`, une ligne de plus ;
- les langages sont livrés **dans** `:editor` : aucune dépendance par
  langage à déclarer, aucune bibliothèque native.

## Moteur de coloration : trois niveaux

```
┌────────────────────────────────────────────────────┐
│ :editor (vue, rendu, cache de lignes)              │
│   fusion : L3 > L2 > L1                            │
│ ┌──────────────┐  ┌──────────────┐  ┌───────────┐  │
│ │ L3 annotateur│  │ L2 analyseur │  │ L1 lexeur │  │
│ │ sémantique   │  │ tolérant     │  │ compilé   │  │
│ │ (:analysis,  │  │ (:analysis)  │  │ (:syntax) │  │
│ │  :lsp)       │  │              │  │           │  │
│ └──────────────┘  └──────────────┘  └───────────┘  │
│   couleur incertaine,     arbre,           jetons, │
│   remplace un neutre      blocs            portées │
└────────────────────────────────────────────────────┘
```

Principe de cohérence : la couleur **certaine** (L1) arrive tout de suite
et ne change jamais ; la couleur **incertaine** (L2, L3) arrive plus tard
et ne remplace qu'un identifiant neutre. La conception complète est dans
`docs/etude-codeassist.md` (§5) ; les critères mesurables (exactitude,
cohérence incrémentale, budgets de latence) y sont détaillés (§6).

## Pureté du cœur

`:core`, `:syntax` et `:analysis` n'importent jamais `android.*`. Cette
pureté sera garantie par des tests ArchUnit sur les packages, pas par un
module par couche — chaque module JVM supplémentaire les aura dès son
premier fichier.

## Découpage interne

Un package par fonctionnalité, plafond de 25 fichiers par package ; tout
ce qui n'est pas l'API publique vit dans un package `internal`. Exemple
pour `:editor` :

```
editor/src/main/java/jo/pigma/editor/
  EditorView.java            (API publique)
  Editor.java                (façade hôte)
  render/                    (dessin, métriques, cache de lignes)
  input/                     (clavier, IME, gestes)
  selection/                 (curseurs, sélection, multi-curseurs)
  gutter/                    (numéros, marqueurs)
  folding/                   (plis)
  popups/                    (complétion, infobulles, menus)
  accessibility/
  internal/                  (le reste)
```

Les décisions sont consignées une par une dans `docs/decisions/`.
