# Décision 001 — package racine `jo.pigma`

## Contexte

Le document de mission demande un package racine court de la forme
`jo.<nom>` pour tous les identifiants. Le nom du dépôt est Pigma.

## Choix

Package racine unique : **`jo.pigma`**. Chaque module décline ensuite son
territoire (`jo.pigma.core`, `jo.pigma.syntax`, `jo.pigma.analysis`,
`jo.pigma.editor`, `jo.pigma.lsp`, `jo.pigma.sample`).

## Conséquences

- l'API publique de la bibliothèque vit sous `jo.pigma.editor` (cas
  courant : une seule dépendance pour l'hôte) ;
- les noms de modules Gradle (`:core`, `:syntax`…) restent courts, le
  `namespace` Android des modules concernés suivra le package ;
- aucun identifiant accentué, aucun mot français : la politique de langue
  (`scripts/check-code-policy`) contrôle chaque fichier.
