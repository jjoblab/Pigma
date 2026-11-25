# Le moteur de coloration (`:syntax`)

La coloration par lexage incrémental — L1, les jetons ; L2,
l'appariement tolérant. Pur Java, il lit le tampon du cœur. La
première coloration ne lex que la FENÊTRE visible ; le fond remplit
le reste par tranches sans jamais toucher la trame.

## Ce que le module porte

| Classe | Rôle |
| --- | --- |
| `SyntaxDocument` | le moteur L1 : jetons d'une ligne, relexage des lignes remuées, la fenêtre paresseuse d'abord |
| `StructureDocument` | le moteur L2 : profondeurs d'imbrication, plis, signes mésappariés (sans jamais rompre) |
| `LanguageSyntax` | l'interface d'un langage (lexeur, préfixe de commentaire, bornes de mots) |
| `Languages` | la famille : Java, XML, JSON, Kotlin, Lua générés, SQL par la famille générique |
| `SyntaxTheme` | les couleurs par nature de jeton (clair, sombre — basculer ne retokenise rien) |
| `BackgroundFill` | la pompe de remplissage du fond (par tranches, postées) |

Les lexeurs générés vivent dans `src/main/java/.../lexers/generated/`
(les descriptions dans `src/main/lexers/`, commentées en français ;
le code généré est versionné — le build sur téléphone ne télécharge
rien, voir `scripts/check-lexers-fresh`).

## Les règles du moteur

- **La fenêtre d'abord** : le premier dessin ne paie que les lignes
  visibles ; `remainingLines()` dit ce que le fond doit encore
  remplir.
- **Le relexage au plus près** : une frappe ne relit que les lignes
  remuées (une seule, d'ordinaire) — les jetons d'alentour se
  recollent sans relire.
- **Le gros fichier** : au-delà d'un seuil, le mode hors normes
  s'engage (`inBigFileMode()`), les jetons se compactent, la mémoire
  reste plate.
- **L2 ne rompt jamais** : le texte mal formé garde une profondeur
  (l'appariement tolérant), les plis restent utilisables, les
  mésappariés se marquent.

## L'usage depuis l'éditeur

```java
SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
SyntaxTheme theme = EditorThemes.DARK;      // le paquet editor le porte
SyntaxDocument.LineTokens line = syntax.lineTokens(3); // jetons de la ligne 3
StructureDocument structure = StructureDocument.open(buffer, syntax);
int depth = structure.structureOf(3).depthOut();
```

## Les tests

`src/test/java` — 26 tests (le lexage des langages, le redémarrage
des états de lexeurs, la fenêtre paresseuse, le remplissage du fond,
le hors normes, la structure). `scripts/run-syntax-tests.sh` ou
`./gradlew :syntax:test`. Les mesures de performance se consignent
dans `docs/perf.md`.
