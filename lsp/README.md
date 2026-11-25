# Le client de serveur de langage (`:lsp`)

La liaison au serveur : lsp4j porte le protocole (JSON-RPC, gson en
transitif), le module n'ajoute que le vocabulaire du dépôt — les
types du domaine, l'écouteur des notifications, la synchronisation
du document par delta, l'arrêt propre. **La seule dépendance
extérieure du dépôt.** Pur Java.

## Les trois raccordements

```java
// le serveur au MÊME processus (les tubes — la démonstration)
LspSession session = LspSession.beside(server, listener);

// le serveur lancé PAR PROCESSUS (l'exécutable hors machine virtuelle)
LspSession session = LspSession.overProcess(process, listener);

// des flux donnés (TCP, socket locale — le raccordement libre)
LspSession session = LspSession.over(input, output, listener);
```

L'écouteur reçoit les diagnostics publiés (la liste ENTIÈRE remplace
la précédente — le protocole est à remplacement) et les messages de
journal ; les méthodes y courent sur le fil de lecture de lsp4j.

## Ce que le module porte

| Classe | Rôle |
| --- | --- |
| `LspSession` | la liaison : initialisation (négociation des capacités), didOpen/didChange (l'ombre du texte porte le delta), requêtes, arrêt |
| `LspCapabilities` | ce que le serveur a déclaré savoir — la façade n'y offre que le déclaré (demander une fonction non déclarée offense le protocole) |
| `LspDiagnostic` | un diagnostic du serveur (plage, sévérité, message) |
| `LspPosition` / `LspRange` | les positions du protocole (ligne, colonne UTF-16) |
| `LspLocation` | une place pointée (l'aller-à la définition, les références) |
| `LspTextEdit` | un remplacement de plage (la correction, le renommage, le formatage) |
| `LspCodeAction` | une correction rapide (titre + remplacements du document) |
| `LspSignatureHelp` | l'aide aux paramètres (libellé, paramètre actif) |

## Les requêtes asynchrones

```java
session.initialize("mon-application");
session.didOpen(uri, text, "java");
CompletableFuture<String> survol = session.hover(uri, position);
CompletableFuture<List<String>> mots = session.completion(uri, position);
CompletableFuture<LspSignatureHelp> aide = session.signatureHelp(uri, position);
CompletableFuture<List<LspLocation>> def = session.definition(uri, position);
CompletableFuture<List<LspLocation>> refs = session.references(uri, position, true);
CompletableFuture<List<LspRange>> occurrences = session.documentHighlight(uri, position);
CompletableFuture<LspRange> renommable = session.prepareRename(uri, position);
CompletableFuture<List<LspTextEdit>> renomme = session.rename(uri, position, "nomNeuf");
CompletableFuture<List<LspCodeAction>> corrections = session.codeActions(uri, range, diagnostics);
CompletableFuture<List<LspTextEdit>> formate = session.formatting(uri, 4, true);
session.didChange(uri, texteEntier);   // la liaison n'envoie que la plage qui a bougé
session.capabilities();                // ce que le serveur a déclaré savoir
session.shutdown();                    // l'arrêt propre (le processus meurt)
```

La synchronisation négocie son genre à l'initialisation :
incrémentale si le serveur le déclare, intégrale sinon (EmmyLua, la
démonstration) — l'ombre du dernier texte envoyé calcule le delta,
les plages se lisent dans les coordonnées du document AVANT le
changement (le protocole l'exige).

## La façade qui porte le câblage

L'hôte ne câble pas la liaison lui-même — la façade
`jo.pigma.editor.lsp.LspEditor` (paquet `:editor`) s'en charge :
l'initialisation, le document, les fournisseurs, les caches, les
fonctions coupées une à une. Le module `:lsp` reste le CLIENT seul ;
la façade vit dans `:editor` (le README de `:editor` détaille
l'intégration minimale). C'est la leçon du module `editor-lsp` de
Sora editor : là-bas aussi, la façade vit au-dessus du client et
l'hôte n'y touche pas.

## Les tests

`src/test/java` — 18 tests (la liaison par tubes et par processus,
les deltas incrémentaux, les requêtes, la négociation des capacités,
l'aller-à, les références, les corrections, le renommage, le
surlignage, le formatage, l'arrêt ; le serveur EmmyLua réel s'y
éprouve par processus). `scripts/run-lsp-tests.sh` ou
`./gradlew :lsp:test`.
