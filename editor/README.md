# La bibliothèque d'édition (`:editor`)

La vue d'édition et tout ce qu'un hôte touche : l'éditeur Android
embarquable, les popups, la gouttière, la façade du serveur de
langage, le style des popups, le pincement du zoom. **L'application
hôte ne déclare que ce module** — il expose le cœur (`:core`), le
moteur (`:syntax`), l'annotateur (`:analysis`) et le client LSP
(`:lsp`) en `api`.

## L'intégration minimale (la leçon de l'éditeur de référence)

L'étude du module d'intégration de l'éditeur de référence a porté sa
leçon : là-bas,
la façade `LspEditor` attache le serveur à la vue — l'hôte de la
référence pose la vue, branche, écrit. Ici pareillement — l'hôte
n'a qu'une poignée de lignes :

```java
EditorView editor = new EditorView(context);
// le document d'UN TRAIT : langage, texte, extraits standard, récents
editor.openDocument(Languages.JAVA, "class Bonjour { }",
        "Bonjour.java", null);
// ou l'original nu :
editor.setLanguage(Languages.JAVA, "class Bonjour { }");

// optionnel — le style des popups (tout se règle : le fond
// translucide, le corps des textes, la largeur des cartes, la forme)
editor.setPopupStyle(PopupStyle.builder()
        .backgroundArgb(0xD9F6F8FA).cornerRadiusDp(20).build());

// optionnel — un serveur de langage, DEUX lignes (la façade câble
// tout : le document, le survol, la complétion, la signature, les
// diagnostics, le surlignage des occurrences, l'ampoule avec
// l'aller-à, les références, le renommage, le formatage, les
// corrections rapides — plus rien à porter par l'hôte)
LspEditor lsp = LspEditor.attach(editor);
lsp.setDiagnosticsListener(list -> { /* le panneau de l'hôte */ });
lsp.connectInProcess(myServer, "file:///demo.java", "java");

// les fonctions du protocole vivent sur la façade — l'hôte les
// appelle quand il veut (l'ampoule les porte déjà, sans lui) :
lsp.requestDefinition();      // l'aller-à (saute au document courant)
lsp.requestReferences();       // les usages (se surlignent en bleu)
lsp.requestRename();           // la carte au caret, une unité d'annulation
lsp.formatDocument();          // le formatage du serveur
lsp.setNavigationListener((uri, places) -> { /* les places d'ailleurs */ });

// optionnel — l'IA DE L'HÔTE, TROIS lignes (la façade câble tout :
// les entrées « Expliquer », « Documenter », « Écrire le test » à
// l'ampoule, « Expliquer l'erreur », « Corriger » à l'étage des
// correctifs ; le contexte part PRÉPARÉ et FILTRÉ par les crochets
// de l'hôte ; le vol se coupe au doigt — Pigma n'y connaît aucun
// fournisseur, l'hôte implémente AiBackend)
AiAssistant ai = new AiAssistant(editor, myBackend,
        new ContextBuilder(
                (name, language) -> !name.endsWith(".env"), // la politique
                null));                                      // ou SON filtre
ai.setDocument("Bonjour.java", "java");
ai.setTestListener(test -> { /* l'hôte seul crée le fichier */ });
```

Le clavier, le défilement, la sélection et ses poignées, la loupe,
la recherche, l'annulation, les plis, la complétion, l'historique
local — tout vit dans la vue. Les boutons du sample sont de la
démonstration, pas de l'intégration.

## Ce que le module porte

| Paquet | Rôle |
| --- | --- |
| `EditorView` | la vue : rendu, gestes, clavier, curseur — le pilier |
| `EditorThemes` | les thèmes (clair, sombre) et leurs fonds |
| `popups/` | les vingt-neuf cartes dessinées dans la vue, le `PopupHost` (l'exclusion mutuelle, la fermeture de contexte — un seul état « popup active ») : recherche/remplacement, aller-à (ligne, symbole, fichier), complétion (candidats entiers, panneau de documentation), signature (surcharges), info/survol Markdown défilant, sélection, débordement, historique, diagnostic (correctifs), actions rapides, renommage (aperçu), usages, choix de cibles, problèmes, presse-papiers, structure (filtre), import, définition en aperçu, récents, entourer, extraits, sélecteur de couleur — `CardBase`/`ListCard` portent la pose, le style, le défilement à la traîne, le flot |
| `popups/PopupStyle` | le style PORTÉ PAR L'HÔTE : fond translucide, encres, corps des textes, hauteur des rangées, bornes de largeur, rayon des coins |
| `gutter/` | la gouttière : numéros, marqueurs de diagnostic, l'ampoule des intentions |
| `lsp/LspEditor` | la façade du serveur de langage (l'étude de l'éditeur de référence — voir le README de `:lsp`) : le protocole de navigation, d'édition et de document (symboles, indices, plis du serveur, jetons sémantiques) |
| `ai/AiAssistant` | la façade de l'IA DE L'HÔTE (voir le README de `:ai`) : les entrées à l'ampoule et à l'étage des correctifs, le vol ANNULABLE (la carte d'attente et son bouton), les réponses vérifiées (le texte qui se lit balisé, le remplacement et l'insertion par une unité d'annulation — un texte qui a bougé ne se pose pas) |
| `input/` | le contrat d'entrée et la connexion IME (la frappe route vers le tampon) |
| `folding/` | le modèle des plis et ses bornes (celles du lexer, celles du serveur de langage composées) |
| `selection/` | l'état et le rendu de la sélection (poignées comprises) |
| `render/` | le cache des plans de rendu (partage par contenu, validité par identité) |

## Le pincement (le zoom au deux doigts)

Deux doigts s'écartent : le corps du texte suit le facteur de
l'écart, la ligne sous le foyer demeure sous les doigts. Les bornes
et le corps se posent par l'hôte :

```java
editor.setTextSizeSp(16.0f);       // la ligne au milieu y demeure
editor.setZoomBoundsSp(10.0f, 30.0f); // le pincement s'y serre
float corps = editor.getTextSizeSp();
```

L'appui final du pincement ne vaut pas un tap — le curseur ne
saute pas au dernier doigt levé.

## Les fournisseurs de l'hôte (le complément des fournisseurs de la
façade)

La façade LSP pose les siens ; l'hôte peut ajouter les siens — les
intentions de l'ampoule (`setQuickActionProvider` : la date du jour
du sample y vit) et le style des popups (`setPopupStyle`). Les
fournisseurs bruts (`setInfoProvider`, `setSuggestionProvider`,
`setSignatureProvider`, `setDiagnosticProvider`) demeurent publics :
l'hôte qui câble autre chose qu'un serveur LSP y va (le cache de la
façade ne se paie pas).

## Les tests

Le harnais d'ouverture (`scripts/open-harness/`) éprouve la vue à
l'écran — 36 scénarios : l'ouverture, la frappe au clavier, les
gestes (poignées, loupe, traîné-déposé, pincement), les popups de
seconde génération, la gouttière, la façade LspEditor et le serveur
de langage. `RenderCacheTest` couvre le cache de rendu.
`./gradlew :editor:test`.
