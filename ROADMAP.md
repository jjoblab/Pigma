# Feuille de route — Pigma

La feuille de route du dépôt : ce qui est fait, ce qui vient, dans quel
ordre et pourquoi. **Elle se met à jour à chaque chantier terminé** —
chaque ligne « fait » pointe son document de chantier et son commit.

Source du plan courant : le rapport d'analyse du propriétaire
(`RAPPORT-ANALYSE-PIGMA`, 2026-11-25, version analysée `5e0de36`)
et les demandes qui l'ont suivies. Les numéros `D*` (défauts) et
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
| 68 | Plis : chevron au bon calque, un seul chevron, placeholder `{…}` tapable | B5 | prévu | — | — |
| 69 | ~~Sauvegarde d'état : BaseSavedState + super~~ réglé au chantier 64 | D1, R2 | **fait** | `docs/chantiers/64.md` | chantier 64 |
| 70 | Version LSP par document (Map URI → compteur, stricte croissance) | D3 | prévu | — | — |
| 71 | Silence du fond : échecs comptés et remontés, passBeginNanos remis | D9 | prévu | — | — |
| 72 | Écouteurs du tampon : CopyOnWriteArrayList | D10 | prévu | — | — |
| 73 | Binaires natifs : le .so x86_64 sort du dépôt, provenance consignée | D12 | prévu | — | — |
| 74 | Pont IME complet : lectures contextuelles, setComposingRegion, drapeaux | D2, R3 | prévu | — | — |
| 75 | Appariement par type de signes : BracketMatcher factorisé | B1 | prévu | — | — |
| 76 | Processus LSP : stderr drainé, arrêt en deux temps | D8 | prévu | — | — |
| 77 | Accessibilité bibliothèque : AccessibilityNodeInfo, actions, événements | D5 | prévu | — | — |
| 78 | Historique local et usages repensés (versions, diffs, extraits) | B7 | prévu | — | — |
| 79 | Chaîne d'approvisionnement : verification-metadata, versions épinglées | D11 | prévu | — | — |
| 80 | Fins de ligne : CRLF au tampon et au protocole | D13 | prévu | — | — |
| 81 | Découpage de EditorView par composants (LayoutModel d'abord) | D6, R1 | prévu | — | — |

L'ordre suit le plan d'action du rapport (priorité 1 d'abord : les bugs
d'usage B6/B2/B5, puis les défauts de haut risque D1/D3, puis le reste),
amputé de ce qui est déjà fait.

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

## Journal des mises à jour

- 2026-11-26 — création ; historique des 113 commits réattribué à
  `jjoblab <olson12jb@gmail.com>` et poussé (force) — les 3 commits
  « Z User » de la période des vagues 44-46 rewriting.
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
