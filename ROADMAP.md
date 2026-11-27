# Feuille de route — Pigma

La feuille de route du dépôt : ce qui est fait, ce qui vient, dans quel
ordre et pourquoi. **Elle se met à jour à chaque chantier terminé** —
chaque ligne « fait » pointe son document de chantier et son commit.

Source du plan courant : le rapport d'analyse du propriétaire
(`RAPPORT-ANALYSE-PIGMA`, 2026-11-25, version analysée `5e0de36`)
puis, au-delà du 87, le rapport des popups (`RAPPORT-POPUPS-PIGMA.md`,
2026-11-26, version analysée `8a636c0`). Les numéros `D*` (défauts) et
`B*` (bugs signalés) viennent du rapport ; les `R*` sont les refontes
qu'il propose.

## Règles de conduite

- Un chantier à la fois ; aucun chantier suivant sans validation du
  propriétaire quand il l'a demandée.
- Chantier terminé = tests verts, politique verte, harnais vert,
  `./gradlew check` complet, démonstration dans `docs/chantiers/<n>.md`,
  commit(s) explicatif(s) en français, **push**, puis **mise à jour de
  cette feuille** (tableau ci-dessous) pushée avec le chantier ou dans
  la foulée immédiate.
- La CI reste **désactivée sur push/PR** : c'est le choix du
  propriétaire (lancement manuel `workflow_dispatch` uniquement).
  Ne pas la réactiver sans sa demande.
- L'historique des commits porte TOUJOURS `jjoblab
  <olson12jb@gmail.com>` — vérifier `git log --format='%an %ae'` avant
  chaque push.

## État des chantiers

| # | Chantier | Rapport | État | Document | Commit |
|---|----------|---------|------|----------|--------|
| 64 | Harnais sur les vraies classes Android (Robolectric remplace les stubs maison) — 11 scénarios sur l'activité réelle ; D1 corrigé au passage (SavedState du cadre) | R5, D7, D1, R2 | **fait** | `docs/chantiers/64.md` | à pousser |
| 65 | Guide d'intégration « 5 minutes » à la racine + capture (rendu réel au mode graphique natif) | — | **fait** | `INTEGRATION.md` | à pousser |
| 66 | Entrée et composition : clearComposing avant l'insertion (sendKeyEvent et performEditorAction) | B6 | **fait** | `docs/chantiers/66.md` | à pousser |
| 67 | Minimap : zone tactile bornée à ce qui se dessine (mapBottom), setMinimapEnabled/isMinimapEnabled publics | B2 | **fait** | `docs/chantiers/67.md` | à pousser |
| 68 | Plis : le placeholder `{…}` de l'en-tête replié se tape et rouvre le bloc (foldPlaceholderLeft) | B5 | **fait** | `docs/chantiers/68.md` | à pousser |
| 69 | ~~Sauvegarde d'état : BaseSavedState + super~~ réglé au chantier 64 | D1, R2 | **fait** | `docs/chantiers/64.md` | chantier 64 |
| — | *Numéro 70 appelé « 69 » dans la numérotation d'origine de la feuille, décalé d'un par la fusion du 69 au 64.* | | | | |
| 70 | Version LSP par document (Map URI → compteur, stricte croissance) | D3 | **fait** | `docs/chantiers/70.md` | à pousser |
| 71 | Silence du fond : échecs comptés et lus (sliceFailures, lastFailure), montre refermée, journal une fois | D9 | **fait** | `docs/chantiers/71.md` | à pousser |
| 72 | Écouteurs du tampon : CopyOnWriteArrayList (le partant pendant sa notification ne lève plus) | D10 | **fait** | `docs/chantiers/72.md` | à pousser |
| 73 | Binaires natifs : le .so x86_64 sort du dépôt (7,9 Mo), provenance consignée au arm64 | D12 | **fait** | `docs/chantiers/73.md` | à pousser |
| 74 | Pont IME complet : getTextBefore/AfterCursor, setComposingRegion, getCursorCapsMode | D2, R3 | **fait** | `docs/chantiers/74.md` | à pousser |
| 75 | Appariement par type de signes : BracketMatcher (isOpening/closerOf/closes), les deux marches et les diagnostics typés | B1 | **fait** | `docs/chantiers/75.md` | à pousser |
| 76 | Processus LSP : stderr drainé (fil de vidange), arrêt en deux temps (grâce 500 ms puis tue-loup) | D8 | **fait** | `docs/chantiers/76.md` | à pousser |
| 77 | Accessibilité : package editor.accessibility (nœud éditable, fenêtre de texte bornée, actions, événements de frappe et sélection) | D5 | **fait** | `docs/chantiers/77.md` | à pousser |
| 78 | Historique local repensé : versions par geste (pause 2 s), plage occupée + texte d'avant, budget en caractères, restauration de plage exacte ; usages : extrait de la ligne par rangée | B7 | **fait** | `docs/chantiers/78.md` | à pousser |
| 79 | Chaîne d'approvisionnement : verification-metadata.xml (401 composants, sha256+sha512), mode strict documenté | D11 | **fait** | `docs/chantiers/79.md` | à pousser |
| 80 | Fins de ligne : CRLF au tampon et au protocole — la paire `"\r\n"` est UN terminateur (règle LSP), paires coupées/recollées aux deux bords de l'index incrémental, positionIn et countBreaks alignés | D13 | **fait** | `docs/chantiers/80.md` | à pousser |
| 81 | Bugs d'usage résiduels : B6 (l'Entrée exige un choix explicite), B5 (le placeholder naît sur l'ouvrant), B4 (la pastille d'erreur du tap) | B6, B5, B4 | **fait** | `docs/chantiers/81.md` | à pousser |
| 82 | Découpage, premier composant : LayoutModel (jo.pigma.editor.internal.layout) — la géométrie offset↔pixel, plis et soft wrap, bornes de défilement, extraite et testée en JVM (13 tests) ; la vue délègue — puis le retour de terrain sur les plis : le chevron de l'ouvert renaît (il avait disparu avec celui de la fermante), la coupe de l'en-tête replié s'applique VRAIMENT (le « {…} » ne se peint plus par-dessus), le placeholder sans ouvrant vit à la fin du texte et ne dérive plus au défilement — FoldRenderingIT (rendu natif, aux pixels) éprouve chacun | D6, R1 | **fait** | `docs/chantiers/82.md` | à pousser |
| 83 | Retour de terrain : la barre annuler/rétablir COIFFE la minimap (les deux boutons EN HAUT de la région, la carte dessous — la demande d'origine enfin tenue) ; l'icône du diagnostic AU TRAIT à la référence (SeverityIcon — rouge/ambre/accent, gouttière, carte et pastille ; MARKER_INFO naît, le marqueur d'erreur retrouve SA colonne) ; le pop de la référence (BadgePop pour la pastille, l'échelle 0,96 pour la carte) ; l'ampoule de l'étage des correctifs à sa COLONNE FIXE (elle dérivait à `cy`) — DiagnosticRenderingIT éprouve chacun aux pixels | — | **fait** | `docs/chantiers/83.md` | à pousser |
| 84 | Découpage, le composant de DESSIN : Painter (jo.pigma.editor.internal.render) — les glyphes du plan cuit, les chips, les ligatures, le placeholder, le soulignement de composition, extraits (437 lignes) ; LinePlan quitte son nid pour render/ ; la vue tranche les fenêtres et délègue (7 388 → 7 155), le pinceau de texte PARTAGÉ ; la coquille du #RGBA (sa 4e paire, l'alpha, se lit désormais) — PainterTest (JVM) + le rendu aux pixels inchangé | D6, R1 | **fait** | `docs/chantiers/84.md` | à pousser |
| 85 | Découpage : SearchController (internal.search — la requête, les correspondances au plafond, le cycle qui boucle ; le cœur pur testé en JVM) et ScrollController (internal.scroll — le balayage aux bords du traîné, l'échéance qui se reprogramme, l'annulation nette) ; la vue : 7 163 → 7 122 lignes, RESTE TouchController (les gestes) | D6, R1 | **fait** | `docs/chantiers/85.md` | à pousser |
| 86 | Découpage : le PINCEMENT, première brique du contrôleur des gestes (internal.touch.PinchController — l'écart porte le facteur, le milieu porte le foyer, la ligne sous le foyer y demeure ; le contrat maigre et le cœur testé en JVM) ; la vue : 7 122 → 7 178 lignes (le branchement se paie) | D6, R1 | **fait** | `docs/chantiers/86.md` | à pousser |
| 87 | Découpage, la suite du contrôleur des gestes : L'ÉTAT DU TOUCHER (internal.touch.TouchController — la mémoire du contact : l'appui, le tap, le seuil de glisse, la poignée traînée et sa fixe, le point de dépôt, le doigt vivant ; les verdicts : durée, double-tap, seuil, rangement) — LE DÉCOUPAGE DE LA VUE S'ACHÈVE (82→87) ; le retour de terrain : la pastille du diagnostic NAÎT AU-DESSUS DU MOT (l'ampoule au trait de l'étage des correctifs, BulbIcon partagé, jamais dans la gouttière qui porte déjà le marqueur) | D6, R1 | **fait** | `docs/chantiers/87.md` | `60b1673` |

L'ordre suit le plan d'action du rapport (priorité 1 d'abord : les bugs
d'usage B6/B2/B5, puis les défauts de haut risque D1/D3, puis le reste),
amputé de ce qui est déjà fait.

Le plan des chantiers 88 à 93 suit le rapport des popups du
propriétaire (`RAPPORT-POPUPS-PIGMA.md`, 2026-11-26, version analysée
`8a636c0`) : ses défauts s'y notent `D-P*`, ses doublons `R-*`, ses
popups manquantes `M*` — la table suit son §9.

| 88 | Popups, l'étape 1 du rapport : le survol ASYNCHRONE (la question PART — le contrat InfoPopupProvider/InfoAnswer — la popup vit immédiate avec « … », la réponse la remplace à son arrivée, le null rend la place à l'index local ; `hover.get` meurt avec `setRequestTimeoutMs`) ; LA COMMANDE UNIQUE des usages (R-C — UsageProvider : la carte vit immédiate au repli textuel, les références sémantiques du serveur y REMPLACENT la liste, les surlignages demeurent, plus de saut aveugle) ; l'ActionRegistry (R-A — les cinq actions d'édition définies UNE FOIS, les deux cartes y mènent leurs rangées) | D-P1, R-A, R-C | **fait** | `docs/chantiers/88.md` | `4bea262` |
| 89 | Popups, l'étape 2 : les modèles LSP COMPLETS — LspCompletionItem (kind, detail, documentation, insertText/textEdit, sortText, deprecated ; `completionItem/resolve` à la sélection), LspSignatureHelp (les surcharges, la documentation du paramètre), LspHover (le Markdown) | D-P2, D-P5, D-P6, M8, M9 | **fait** | `docs/chantiers/89.md` | `398b845` |
| 90 | Popups, l'étape 3 : PopupHost (le gestionnaire — un seul état « popup active », l'exclusion mutuelle, la fermeture au changement de contexte, la fin des 23 fermetures manuelles et du popup fantôme) ; CardBase/ListCard (la pose, le style, le défilement, la croix) ; migration d'une carte à la fois (UsageCard et DiagnosticCard d'abord) | D-P4, R-E, R-F | **fait** | `docs/chantiers/90.md` | `f945ac0` |
| 91 | Popups, l'étape 4 — les P1 manquantes : le CHOIX DE CIBLES (plusieurs définitions), la STRUCTURE DU FICHIER (l'arbre des symboles, le filtre à la frappe), la LISTE DES PROBLÈMES (parcourir les erreurs), l'HISTORIQUE DU PRESSE-PAPIERS ; la suggestion d'IMPORT (M14) part à l'étape suivante (le serveur de démo Lua n'en rend aucun correctif — l'éprouver y serait mensonge) | M1, M2, M13, M20, D-P3, R-D | **fait** | `docs/chantiers/91.md` | `d046c97` |
| 92 | Popups, l'étape 5 — les P2 ET LA SUGGESTION D'IMPORT (M14, la bulle « Importer x.y.Z ? » — les correctifs du serveur s'y surfacent) : la définition en APERÇU, l'aller au symbole/fichier, les fichiers récents, l'APERÇU du renommage, l'entourer de, les extraits | M3–M5, M14, M15, M18, M21 | **fait** | `docs/chantiers/92.md` | `cbae5dc` |
| 93 | Popups, l'étape 6 : l'ACCESSIBILITÉ des cartes (annonce à l'ouverture — la source unique du gestionnaire —, rangées en NŒUDS VIRTUEUX, cible ≥ 48 dp) et les chaînes LOCALISÉES (PopupStrings posé par l'hôte — la source unique, les quatorze cartes migrées) | D-P8, D-P9 | **fait** | `docs/chantiers/93.md` | `382a3d5` |
| 94 | LE RETOUR DE TERRAIN des popups : le placeholder du pli en PUCE lisible (sa taille ne se posait JAMAIS — cause racine), le FOND de sélection de la complétion (0x33 + le candidat courant dès la naissance — B6 tenu par le drapeau `chosen`), LE GESTE DES CARTES (le tir au-delà du seuil ne joue plus sa rangée — reproduit au harnais avant correctif — et la traîne DÉFILE les listes), LE CURSEUR QUI CLIGNOTE aux champs (recherche, remplacer, aller-à, renommage), LA COMPLÉTION se range devant une barre à champ (et ne re-naît pas à la trame), LE FLOT de pose (140 ms d'ease-out — barres, douze cartes, complétion) ; LA REVUE ENTIÈRE des popups contre le rapport y vit | — | **fait** | `docs/chantiers/94.md` | `c1aad51` |


## Déjà couvert avant cette feuille (constaté au code, au 2026-11-26)

- **D4 CI** — volontairement off (`workflow_dispatch` seul) : choix du
  propriétaire, inscrit aux règles de conduite.
- **B3 boutons annuler/rétablir** — la rangée horizontale et les icônes
  de la vague 48 (`645cfde`).
- **B4 zone de tap du diagnostic** — le tap précis à étages et les
  correctifs locaux de la vague 49 (`5e0de36`).
- **D16 (partiel)** — `ARCHITECTURE.md` décrit toujours
  `accessibility/` à créer (chantier 77 ci-dessus).
- **D19/D20** — à reprendre au fil des chantiers touchés (le coquille
  `MEMORRY_ANCHOR_INTERVAL` et l'historique dans les commentaires se
  corrigent en passant).

| 95 | L'INTÉGRATION FACILE (le retour du terrain : « trop de méthodes dans le sample qui devraient être dans l'éditeur — l'intégration doit être facile ») : `openDocument` d'UN TRAIT (langage + texte + extraits standard + récents), `connectEmmyLua` d'UN TRAIT (espace de travail + document + processus + liaison — `lastEmmyLuaError` pour le toast) ; MainActivity 1329 → 1250 lignes, le mode d'emploi porte les deux traits | — | **fait** | `docs/chantiers/95.md` | `6213b8c` |

| 96 | LA PAPIERASSE RATTRAPE LE CODE (« va pour D16, D19/D20 » — et « les READMEs doivent refléter réellement le code, le mode d'utilisation md aussi ») : D16 réglé (ARCHITECTURE.md porte l'arbre RÉEL du module éditeur — l'accessibilité, les vingt-neuf cartes, lsp, internal), D19/D20 réglé (la coquille MEMORRY_ANCHOR_INTERVAL meurt, l'historique mort du 95 part avec son code), les README reflètent le code (editor/README : la rangée popups/ complète + openDocument ; INTEGRATION.md : les deux traits du 95) | D16, D19/D20 | **fait** | `docs/chantiers/96.md` | `09171be` |

Le plan des chantiers 97 à 110 suit le prompt du propriétaire
(`PROMPT-PIGMA-BUG-ET-IA`, 2026-11-27 — le bug des onglets puis la
couche d'intégration de l'IA) ; ses causes s'y notent `A*` (le bug
des onglets), ses étapes `B*` (la couche IA — Pigma ne connaît
AUCUNE intelligence artificielle : l'hôte implémente, paie et
décide). Le 97 est le retour de terrain du propriétaire sur la
gouttière et la minimap ; le 110 son retour sur les icônes de
l'ampoule.

| 97 | LE RETOUR DE TERRAIN de la gouttière et de la minimap : la gouttière MAIGRIT et perd son ombre (l'élévation cachait les icônes de pli), la minimap se tait PAR DÉFAUT (l'hôte seul la demande), la carte de diagnostic DÉFILE au doigt (le défaut du terrain : « je n'arrive pas à scroller ») | — | **fait** | `docs/chantiers/97.md` | `627c2b3` |
| 98 | A1 — LA PASTILLE meurt au changement de contexte : elle s'inscrit au gestionnaire (rôle ANCRE — nulle seconde liste de fermeture ne naît), `setLanguage`/`reopenSameDocument`/l'édition qui déplace la ligne la rangent ; le tap dans le nouvel onglet n'ouvre plus la carte d'un diagnostic d'avant | A1 | **fait** | `docs/chantiers/98.md` | `fc8bbd2` |
| 99 | A3 — L'ÉTAT PAR DOCUMENT : `DocumentState` (texte, curseur et sélection, défilement, plis, pile d'annulation, identité) avec `saveState`/`restoreState` qui ÉCHANGENT les documents sans rien perdre ; `setLanguage` garde son comportement (compatibilité), les deux chemins rangent le contexte | A3 | **fait** | `docs/chantiers/99.md` | `f40173e` |
| 100 | A2 — LE SAMPLE : un document PAR ONGLET (son URI, son langage, son état) ; le changement d'onglet sauvegarde/restaure et change le document côté LSP (`switchDocument`) ; le serveur de démonstration ne voit que des documents Java | A2 | **fait** | `docs/chantiers/100.md` | `124624c` |
| 101 | A4 — LES RÉPONSES TARDIVES se jettent : l'inventaire de chaque `view.post` qui ouvre une carte, la génération de document les garde (la réponse d'un ancien document n'ouvre RIEN) | A4 | **fait** | `docs/chantiers/101.md` | `b79ca59` |
| 102 | B0 — LE MODULE `:ai`, PUR : le contrat de l'hôte (`AiBackend`, `AiRequest` immuable, `AiResult` scellé — une DONNÉE jamais une instruction, `AiError` par famille), le test de pureté, aucun nom de fournisseur ni de modèle | B0 | **fait** | `docs/chantiers/102.md` | `69c2c16` |
| 103 | B1 — LA PRÉPARATION DU CONTEXTE : `ContextBuilder` (préfixe/suffixe bornés au budget en caractères, sélection, bloc englobant, diagnostics, `otherDocuments` par callback de l'hôte), les crochets TOUJOURS appliqués (`AiPolicy` exclut avant de proposer, `ContextFilter` modifie ou refuse), le filet `DefaultContextFilter` documenté comme un filet | B1 | **fait** | `docs/chantiers/103.md` | `c25a472` |
| 104 | B2 — L'ASSISTANT ENTRE DANS LA VUE : la façade `AiAssistant` (l'ampoule porte Expliquer/Documenter/Écrire le test aux capacités déclarées, l'étage des correctifs porte Expliquer l'erreur/Corriger), le vol ANNULABLE (l'attente et son bouton, la fermeture et l'édition l'abandonnent, la réponse du vol tué se jette), les réponses VÉRIFIÉES (la révision du tampon s'éprouve avant la pose, UNE unité d'annulation), le bloc englobant se calcule (`StructureDocument.scopeAt`), les quinze chaînes à `PopupStrings` | B2 | **fait** | `docs/chantiers/104.md` | `d4d8237` |
| 105 | B3 — LA MODIFICATION PAR INSTRUCTION : la pastille « Modifier avec l'IA… » à la barre de sélection (aux capacités, à la politique, à la sélection), la barre d'instruction `AiEditBar` (famille KeyboardCard — l'instruction voyage avec la sélection), L'APERÇU des trois verdicts `AiDiffCard` (Accepter pose en UNE unité après vérification, Rejeter jette, Réessayer repose la question, le contenu non fiable se borne), les ERREURS NÉES se signalent (la passe d'analyse close les compare — `setAnalysisListener` naît), la correction du 104 migre à l'aperçu (le critère : toute modification passe par un aperçu) | B3 | **fait** | `docs/chantiers/105.md` | `5fea497` |
| 106 | B4 — LE TEXTE FANTÔME : la machine pure `GhostTextState` (l'attente, le vol, l'affichage — la frappe reprend tout, la réponse périmée se jette, l'acceptation PARTIELLE mot à mot), le calque au caret (jamais écrit au tampon avant l'acceptation, les masques obligatoires éprouvés à chaque trame : complétion, composition IME, sélection, pli, document, éditeur figé), TAB pose tout, les balayages décident (le mot, tout, le rejet), l'annonce « Suggestion disponible », DÉSACTIVÉE par défaut | B4 | **fait** | `docs/chantiers/106.md` | `a300f0e` |
| 107 | B5 — LES TÂCHES OPTIONNELLES : le commentaire de documentation se pose AU-DESSUS DE LA DÉCLARATION (la structure dit le point de pose — `declarationLineAt`, la plus interne gagne), l'insertion vit l'aperçu du 105 ; le test écrit se rend À L'HÔTE (`setTestListener`) et se lit à la popup — Pigma n'en crée jamais le fichier | B5 | **fait** | `docs/chantiers/107.md` | `a3e0d28` |
| 108 | B6 — L'« IA DE DÉMONSTRATION » DU SAMPLE : `FakeAiBackend` déterministe (les réponses parlent de la demande, le flux par morceaux à la file du thread principal, aucun réseau, la poignée retire les morceaux non joués), les états joués (normale, indisponible, délai, refus, muette, démesurée), l'INTERRUPTEUR au drawer (éteinte par défaut — aucune action d'IA ne se propose sans lui), le manifeste demeure sans permission | B6 | **fait** | `docs/chantiers/108.md` | `d2a34fa` |
| 109 | B7 — LA VOIX ET LA CIBLE : l'annonce DÉDIÉE de l'assistant (« Assistant ouvert » — `showAssistantPopup`, la documentation garde sa voix), la cible du bouton ANNULER respire à 48 dp (bornée à la boîte), la croix de la barre d'instruction pareil, les trois verdicts y vivaient déjà ; ce qui vivait se consigne (les réponses sont des données, les vingt-neuf chaînes, aucune journalisation, aucun réseau) | B7 | **fait** | `docs/chantiers/109.md` | `3c22aea` |
| 110 | LE RETOUR DE TERRAIN DES ICÔNES : « les options du popup de l'ampoule ont toutes la même icône » — chaque action porte SA NATURE, dix-neuf glyphes AU TRAIT à la grille 24 (la façon de SeverityIcon) : l'éditeur (le commentaire //, les casses, le double, la poubelle), l'entourage, les extraits, l'hôte (le calendrier, la loupe), le serveur (la flèche à la barre, l'œil, les guillemets, le crayon, l'alignement, l'arbre), l'assistant (la bulle, la feuille, la coche) ; l'éclair d'action demeure le défaut | — | **fait** | `docs/chantiers/110.md` | `c38b934` |

## Journal des mises à jour

- 2026-11-27 — LA FEUILLE RATTRAPE LES CHANTIERS 97 à 110 : la table
  porte les quatorze rangées (le bug des onglets A1–A4, la couche IA
  B0–B7, les deux retours de terrain du propriétaire — la gouttière
  et les icônes de l'ampoule) ; l'historique des commits de la
  période fut réattribué à `jjoblab <olson12jb@gmail.com>` et poussé
  (force) — la règle de conduite y retrouve son compte. LA MISSION
  DU PROMPT « BUG D'ONGLETS, PUIS COUCHE D'INTÉGRATION DE L'IA » SE
  REFERME : la partie A (le bug des onglets) et la partie B (la
  couche IA — B0 à B7) sont faites, hors périmètre demeure
  l'explicitement exclu (tout client réseau, chat contextuel, mode
  agent, arbre syntaxique avancé).

- 2026-11-27 — chantier 110 fermé : le retour de terrain des icônes
  — chaque option de l'ampoule porte SA NATURE au trait (dix-neuf
  glyphes à la grille 24, la façon de SeverityIcon) : l'éditeur,
  l'entourage, les extraits, l'hôte, le serveur et l'assistant s'y
  lisent d'un coup d'œil ; l'éclair d'action demeure le défaut des
  entrées qui ne disent rien (preuve aux pixels : natures distinctes
  peignent distinct, QuickIconIT au mode graphique natif).

- 2026-11-27 — chantier 109 fermé : B7 — la voix de l'assistant
  (« Assistant ouvert » — sa popup porte SON annonce, la
  documentation garde la sienne) et la cible à 48 dp (le bouton
  ANNULER de l'attente respire, borné à la boîte ; la croix de la
  barre d'instruction pareil) ; les réponses demeurent des DONNÉES,
  les vingt-neuf chaînes localisées, aucune journalisation, aucun
  réseau.

- 2026-11-27 — chantier 108 fermé : B6 — l'« IA de démonstration »
  du sample (FakeAiBackend déterministe, le flux par morceaux à la
  file du thread principal, aucun réseau) et son INTERRUPTEUR au
  drawer — éteinte par défaut, aucune action d'IA ne se propose sans
  lui ; les états joués (indisponible, délai, refus, muette,
  démesurée) éprouvent les phrases de panne au doigt.

- 2026-11-27 — chantier 107 fermé : B5 — le commentaire de
  documentation se pose AU-DESSUS DE LA DÉCLARATION (la structure
  dit le point de pose, le moteur rend le texte) et vit l'aperçu du
  105 ; le test écrit se rend À L'HÔTE et se lit à la popup — Pigma
  n'en crée jamais le fichier.

- 2026-11-27 — chantier 106 fermé : B4 — le TEXTE FANTÔME : la
  machine pure GhostTextState (JVM, l'horloge injectée), le calque
  au caret (jamais écrit au tampon avant l'acceptation, les masques
  obligatoires à chaque trame), TAB pose tout, les balayages posent
  le mot ou tout ou rejettent, l'annonce « Suggestion disponible » —
  désactivée par défaut, l'hôte seul l'arme.

- 2026-11-27 — chantier 105 fermé : B3 — la modification par
  instruction (la pastille à la barre de sélection, la barre
  d'instruction, la sélection et l'instruction voyagent) et L'APERÇU
  des trois verdicts (Accepter / Rejeter / Réessayer — l'acceptation
  seule pose, UNE unité d'annulation, le contenu non fiable se
  borne) ; les ERREURS NÉES se signalent à la passe d'analyse close
  ; la correction du 104 migre à l'aperçu (le critère du rapport :
  toute modification passe par un aperçu).

- 2026-11-27 — chantiers 98 à 101 fermés : LE BUG DES ONGLETS (partie
  A du prompt) — la pastille du diagnostic meurt au changement de
  contexte (A1, inscrite au gestionnaire), l'état PAR DOCUMENT vit
  (A3 — DocumentState, l'annulation et les plis survivent au
  voyage), le sample porte un document PAR ONGLET (A2 — le serveur
  ne voit jamais le contenu d'un onglet sous l'URI d'un autre), les
  réponses tardives d'un ancien document SE JETTENT (A4 — la
  génération de document les garde).

- 2026-11-27 — chantiers 102 à 104 fermés : LA COUCHE IA (B0 à B2 du
  prompt) — le module :ai PUR (le contrat de l'hôte, une DONNÉE
  jamais une instruction, aucun nom de fournisseur), la préparation
  du contexte et ses crochets TOUJOURS appliqués (la politique
  exclut avant de proposer), la façade AiAssistant (l'ampoule et
  l'étage des correctifs aux capacités déclarées, le vol annulable,
  les réponses vérifiées, le bloc englobant de la profondeur
  d'accolades).

- 2026-11-27 — chantier 97 fermé : le retour de terrain de la
  gouttière — la gouttière MAIGRIT sans ombre (les icônes de pli se
  lisent), la minimap se tait PAR DÉFAUT (l'hôte seul la demande),
  la carte de diagnostic DÉFILE au doigt.

- 2026-11-27 — chantier 96 fermé : la paperasse rattrape le code —
  D16 (l'arbre du module :editor dans ARCHITECTURE.md porte les
  packages réels, accessibility/ compris), D19/D20 (la coquille
  MEMORRY_ANCHOR_INTERVAL corrigée au commentaire), editor/README
  porte la rangée popups/ entière (vingt-neuf cartes, le PopupHost)
  et l'extrait openDocument, INTEGRATION.md porte les deux traits du
  95 ; les README relus ne portaient rien de faux.

- 2026-11-27 — chantier 95 fermé : l'intégration facile —
  `EditorView.openDocument` (le langage, le texte, les extraits
  standard du langage, le document aux récents, d'un seul appel) et
  `LspEditor.connectEmmyLua` (l'espace de travail, le document, le
  processus, la liaison, d'un seul appel — la stderr parlante rendue
  par `lastEmmyLuaError`) ; l'écran de démo ne garde que ce qui
  revient à l'hôte ; INTEGRATION.md reflète les deux traits ; le
  stub du vieux harnais gagne `getCacheDir`/`getApplicationInfo`.

- 2026-11-27 — chantier 94 fermé : le retour de terrain du plan des
  popups — huit défauts signalés, trois causes racines (le
  placeholder sans taille, le tir qui jouait sa rangée — reproduit
  au harnais AVANT correctif (`PopupTouchIT`), la complétion qui
  couvrait les barres), le geste des cartes refondu (désarmement au
  seuil, DÉFILEMENT à la traîne — le doigt sert enfin les barres de
  défilement dessinées), le curseur des champs clignote
  (`FieldCaret`), le flot de pose glisse (`PopupFloat` — JVM pur
  éprouvé), la revue entière des popups contre le rapport y est
  consignée (D-P7 et M7 n'étaient au plan d'aucune étape — à la
  feuille prochaine).

- 2026-11-27 — chantier 93 fermé : la sixième étape du plan des
  popups, la dernière — D-P8 réglé (PopupStrings : la source unique
  des chaînes posée par l'hôte, immuable au bâtisseur — les
  quatorze cartes à libellés fixes y vivent, les entrées de la
  façade du serveur aussi ; sans consigne, rien ne change), D-P9
  réglé (les rangées en NŒUDS VIRTUELS : le contrat VirtualRows sur
  la base des listes, le fournisseur de nœuds à la vue, la carte
  vivante s'y lit et s'y joue ; la cible tactile auditée à
  48 dp — la bulle d'import et le pied de l'aperçu grandissent).
  LE PLAN DES POPUPS DU RAPPORT SE REFERME (88–93).

- 2026-11-27 — chantier 92 fermé : la cinquième étape du plan des
  popups — M14 réglé (la bulle de la suggestion d'import : compacte
  AU-DESSUS DU MOT, ouverte en liste à plusieurs candidats, les
  correctifs d'import du serveur y surfacent — le genre du protocole
  les y distingue, le serveur de démo en rend deux à un mot non
  résolu), M3 née (la définition en aperçu — les lignes de la
  déclaration se lisent, « Ouvrir » y saute), M4 née (l'aller au
  symbole — `workspace/symbol` à la session, le filtre à la
  frappe, les deux sources), M5 née (les fichiers récents — la
  mémoire plafonnée, les emplacements suivis aux sauts), M15 née
  (l'aperçu du renommage — les cases retiennent, « Appliquer » pose
  les retenues), M18 née (l'entourer de — les paires et les blocs du
  langage, une unité d'annulation), M21 née (les extraits — le
  registre de l'hôte, le champ posé sélectionné).

- 2026-11-27 — chantier 91 fermé : la quatrième étape du plan des
  popups — D-P3 réglé (le choix de cibles : l'aller-à à plusieurs
  résultats ouvre LA carte, plus de saut aveugle à la première),
  R-D réglé (la structure du fichier vit SA carte dédiée — les
  glyphes de nature, le symbole courant au gras, le FILTRE à la
  frappe —, le détournement de la carte d'actions meurt), M13 née
  (la liste des problèmes groupée, le choix y navigue), M20 né
  (l'historique du presse-papiers : la copie d'avant se colle au
  doigt). M14 (la suggestion d'import) part à l'étape suivante — le
  serveur de démo Lua n'en rend aucun correctif, l'éprouver y
  serait mensonge.
- 2026-11-27 — chantier 90 fermé : la troisième étape du plan des
  popups — D-P4 réglé (PopupHost : un seul état « popup active »,
  l'exclusion mutuelle des flottantes et des barres à champ, la
  fermeture de contexte qui range la liste inscrite ENTIÈRE — le
  popup fantôme meurt là où il naissait : la liste de fermeture
  qui oubliait une carte), R-F entamé (CardBase/ListCard : la
  pose, les murs, la croix, la fenêtre de rangées et la barre de
  défilement partagées — UsageCard et DiagnosticCard migrées,
  les suivantes à la file).
- 2026-11-27 — chantier 89 fermé : la deuxième étape du plan des
  popups — D-P2/D-P6/D-P5 réglés (les candidats de complétion
  ENTIERS et résolus, les surcharges de signature vivent, le survol
  porte son balisage), M8/M9 nés (le panneau de documentation de
  complétion, le corps Markdown qui défile au doigt).
- 2026-11-27 — chantier 88 fermé : la première étape du plan des
  popups — D-P1 réglé (le survol n'attend plus sur le fil
  d'interface : la popup vit immédiate, la réponse la remplace),
  R-C réglé (la commande unique des usages : la carte vit immédiate
  au repli, le serveur y remplace sa liste, plus de saut aveugle),
  R-A réglé (ActionRegistry : les cinq actions d'édition définies
  une seule fois, lues par les deux cartes).
- 2026-11-27 — chantier 87 fermé : D6/R1 ACHÈVENT le découpage de
  la vue (TouchController — l'état du toucher : la mémoire du
  contact et ses verdicts, horloge injectée, JVM pur) ; le retour
  de terrain : la pastille du diagnostic naît AU-DESSUS DU MOT
  (l'ampoule au trait de l'étage des correctifs — BulbIcon
  partagé, hors gouttière qui porte déjà le marqueur) ; la feuille
  s'ouvre sur le plan des popups du second rapport (chantiers
  88-93, son §9).
- 2026-11-26 — chantier 82 refermé sur un retour de terrain : les
  deux régressions des plis réparées — le chevron du pli ouvert
  renaît (le correctif du chevron bas l'avait emporté avec lui :
  plus AUCUNE icône), la coupe de l'en-tête replié s'applique
  VRAIMENT (reveal se calculait puis personne ne le lisait : le
  « {…} » se peignait par-dessus l'en-tête entier), le placeholder
  sans ouvrant vit à la fin du texte visible (plus dans la
  gouttière, à la colonne −1) et ne dérive plus au défilement
  horizontal (le scrollX s'y comptait deux fois). FoldRenderingIT
  (rendu natif, à la leçon de la capture) éprouve les trois faits
  aux pixels — chacun échoue sur le code d'avant.
- 2026-11-26 — création ; historique des 113 commits réattribué à
  `jjoblab <olson12jb@gmail.com>` et poussé (force) — les 3 commits
  « Z User » de la période des vagues 44-46 rewriting.
- 2026-11-26 — chantier 79 fermé : D11 réglé — les 401 artefacts
  de la construction se pinillonnent par sommes SHA (le mode
  strict passe le check et l'APK), la régénération et le passage
  aux signatures se documentent au gradle/VERIFICATION.md.
- 2026-11-26 — chantier 78 fermé : B7 (historique) réglé — les
  versions par geste remplacent les traces par caractère (pause de
  2 s, plage occupée après le geste, texte d'avant, budget de
  32 768 caractères) ; la restauration remet la plage exacte (plus
  d'écrasement de ligne décalée, plus de troncature à 400). Les
  usages portent leur extrait de ligne.
- 2026-11-26 — chantier 77 fermé : D5 réglé — le package
  accessibility/ vit (le nœud se remplit : classe d'éditeur,
  fenêtre de texte bornée autour du curseur, sélection, huit
  actions ; les événements de frappe et de sélection se remplissent).
- 2026-11-26 — chantier 76 fermé : D8 réglé — le stderr du serveur
  se vide au fil de fond (un serveur bavard ne fige plus le tuyau),
  l'arrêt accorde sa grâce puis son tue-loup ; le test du serveur
  bavard (512 Ko) l'éprouve.
- 2026-11-26 — chantier 75 fermé : B1 réglé — le contrôle de type
  vit dans BracketMatcher (syntax), les marches avant/arrière de
  l'encadrement le vérifient, les diagnostics portent la pile des
  signes et nomment la faute.
- 2026-11-26 — chantier 74 fermé : D2/R3 réglés — le pont IME
  se complète (lectures contextuelles, région de composition par
  le clavier, majuscules contextuelles) ; les stubs du vieux
  harnais suivent.
- 2026-11-26 — chantier 73 fermé : D12 réglé — le binaire x86_64
  (7,9 Mo, émulateur seul) sort du dépôt, sa provenance se consigne
  au arm64 restant.
- 2026-11-26 — chantier 72 fermé : D10 réglé — les deux listes
  d'écouteurs du tampon passent aux listes copiées à l'écriture ;
  le test du partant pendant sa notification l'éprouve.
- 2026-11-26 — chantier 71 fermé : D9 réglé — le fil de fond
  compte ses échecs (AtomicLong + message gardé), la montre se
  referme à l'échec, le journal en dit la cause.
- 2026-11-26 — chantier 70 fermé (le 69 a fusionné au 64) : D3
  réglé — la version de document croît strictement PAR URI
  (ConcurrentHashMap), didOpen repart à 1, didClose range sa
  version ; le test des deux documents alternés l'éprouve.
- 2026-11-26 — chantier 68 fermé : B5 réglé — le tap au-delà de
  la fin visible d'un en-tête replié rouvre le bloc (le placeholder
  se clique à la leçon de la référence) ; les chevrons vivaient déjà
  au panneau de la gouttière depuis la vague 49.
- 2026-11-26 — chantier 67 fermé : B2 réglé — la zone tactile de
  la minimap se borne au bas du document rendu (`mapBottom`), le
  drapeau public `setMinimapEnabled` la retire entière ; le
  scénario du harnais réel éprouve le tap sous la carte.
- 2026-11-26 — chantier 66 fermé : B6 réglé — l'Entrée (par
  événements et par touche d'action) clôt la composition AVANT
  d'insérer le saut de ligne ; le scénario du harnais réel
  l'éprouve (composition posée, Entrée, un seul saut, aucun
  fantôme).
- 2026-11-26 — chantier 65 fermé : `INTEGRATION.md` à la racine
  (dépendance, vue, langage, thème, LSP, la capture) ; la capture
  vient du test `ScreenshotIT` — Robolectric en mode graphique
  NATIF rasterise les vraies polices, le PNG de 61 Ko se régénère
  par `:sample:testDebugUnitTest`.
- 2026-11-26 — chantier 64 fermé : le harnais réel (Robolectric)
  vit aux tests du sample, D1/R2 réglés dedans. Le check complet
  exige `--max-workers=1` dans le bac (4 Go : le démon et le test
  du vrai cadre s'entre-tuent sinon).
