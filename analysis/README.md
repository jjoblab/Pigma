# L'annotateur sémantique (`:analysis`)

La couche L3 : les déclarations et les usages tirés des jetons de L1
et de l'appariement de L2 — les annotations sémantiques (la fusion
de couleur), l'index des symboles, les diagnostics locaux. Pur Java,
il lit le tampon et les jetons.

## Ce que le module porte

| Classe | Rôle |
| --- | --- |
| `SemanticDocument` | la passe sémantique par tranches postées (jamais dans la trame de dessin) |
| `SemanticAnnotator` | l'interface d'un annotateur (un langage, une façon de lire les déclarations) |
| `JavaDeclarations` | l'annotateur du Java : classes, méthodes, champs, paramètres |
| `SymbolIndex` | l'index des symboles du document (la complétion locale y puise) |
| `DiagnosticsDocument` | les diagnostics locaux (les signes mésappariés de L2 y deviennent erreurs) |

## Les règles de l'annotateur

- **La passe hors trame** : l'analyse court par tranches postées ;
  la trame de dessin ne paie jamais la passe (l'annotateur se
  suspend au changement de document, il ne découpe jamais les
  anciens jetons dans le nouveau texte).
- **La révision borne tout** : l'index et les diagnostics portent la
  révision de la passe ; le lecteur périmé se refuse (`revision()` à
  comparer à celle du tampon).
- **L'index est une indication** : la complétion locale s'y trompe
  sans dommage (les noms restent bons, les positions peuvent être
  anciennes) — le serveur de langage prime toujours quand il est
  branché.

## L'usage depuis l'éditeur

```java
SemanticDocument semantic = SemanticDocument.open(buffer, syntax,
        structure, new JavaDeclarations());
List<SemanticAnnotation> window = semantic.annotationsAt(3);
SymbolIndex symbols = SymbolIndex.of(buffer, syntax, structure,
        new JavaDeclarations());
List<SymbolIndex.Candidate> found = symbols.queryDetailed("gr", 8);
```

## Les tests

`src/test/java` — les déclarations du Java (l'index et les
diagnostics), la pureté. `./gradlew :analysis:test`. Le harnais
d'ouverture éprouve la fusion de couleur à l'écran
(`scripts/open-harness/`).
