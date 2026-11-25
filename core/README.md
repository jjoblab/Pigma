# Le cœur du modèle de texte (`:core`)

Le tampon éditable sur lequel tout le dépôt repose — l'éditeur, le
moteur de coloration, l'annotateur et le client de serveur de langage
y lisent et y écrivent. Pur Java, aucune dépendance Android : le
module vit partout où un JDK vit.

## Ce que le module porte

| Classe | Rôle |
| --- | --- |
| `TextBuffer` | le tampon : texte, lignes, révisions, lecture par plage |
| `TextTransaction` | une édition atomique (une unité d'annulation, les écouteurs avertis après application) |
| `UndoStack` | l'annulation et le rétablissement (les transactions y retournent) |
| `TextListener` | l'écoute des éditions (décalage, longueurs, révision) |
| `TextPosition` | une position (ligne, colonne) et ses allers-retours en décalage |

## Les règles du modèle

- **Une révision par édition** : chaque remplacement fait avancer la
  révision du tampon — les couches au-dessus s'y aperçoivent (une
  analyse périmée ne se lit plus).
- **L'annulation par transaction** : une édition appliquée hors
  transaction ne s'annule pas ; l'éditeur ne pose que des
  transactions (la frappe d'un mot reste UNE unité tant que le fil du
  clavier ne se rompt pas).
- **Les coordonnées en unités de code UTF-16** : celles de `String`
  (le protocole LSP parle le même idiome, la conversion est directe).
- **L'écoute après application** : les écouteurs courent quand le
  tampon est redevenu cohérent — ils peuvent le lire.

## L'usage depuis l'éditeur

```java
TextBuffer buffer = TextBuffer.create("");
try (TextTransaction transaction = undoStack.newTransaction()) {
    transaction.replace(0, 0, "du texte");
}   // l'écouteur s'y entend, l'annulation y retourne
int line = buffer.lineOfOffset(4);
String first = buffer.getText(0, buffer.getLineEnd(0)).toString();
```

## Les tests

`src/test/java` — le tampon aléatoire (les invariants : le texte
concordé, les lignes continues), les transactions, l'annulation, la
pureté (aucun identifiant français, `scripts/check-code-policy`).
`./gradlew :core:test`.
