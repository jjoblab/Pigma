# Registre de portage

`jjoblab/celeris` est la **référence à consulter, en lecture seule** : on
s'en inspire, on ne le copie jamais. Pour chaque fonctionnalité candidate :
relire le comportement, comprendre ce qu'il fait, le **réécrire** dans la
nouvelle structure. Aucun fichier, aucun commentaire, aucune arborescence
de packages n'est repris tel quel. Les tests qui capturent un comportement
utile peuvent être réécrits en respectant la politique de langue.

Ce document, et lui seul, peut citer celeris.

## Règle des références externes

La règle « Références externes » de l'`AGENT.md` de celeris est importée
telle quelle : les projets tiers se nomment librement dans la
documentation (mention factuelle, licence portée au `NOTICE` le cas
échéant), sauf **deux projets interdits** dont les noms, artefacts et
productions ne se committent jamais. Leurs motifs vivent dans
l'environnement local (variable `PIGMA_FORBIDDEN_REFS` ou fichier non
versionné `config/forbidden-refs.local`) et dans le secret de la CI le
jour où elle existera ; le contrôle est `scripts/check-forbidden-refs`.
Les noms eux-mêmes ne figurent pas dans ce document.

## Candidats à examiner

Liste non limitative, extraite du plan de mission — chaque ligne sera
décidée avec le propriétaire (reprise / réécriture / abandon) **avant** le
travail concerné, puis le statut sera mis à jour ici. L'examen se fait
dans le dépôt de référence en lecture seule ; le module d'origine sera
consigné au moment de la décision.

| Fonctionnalité | Décision | Chantier cible |
| --- | --- | --- |
| Popups (feuille du bas/haut, popup ancrée) | **réécrit** (chantier 24) : la feuille du bas porte la recherche et le remplacement (sample), la popup ancrée se dessine dans la vue (info de symbole, réponse de survol d'un serveur de langage) — la feuille du haut reste à faire si un besoin la justifie | 8 |
| Gouttière (numéros, marqueurs) | **réécrit** (chantiers 14-15) : marqueurs de plis, guides de profondeur | 6 |
| Pliage | **réécrit** (chantier 14) : plis par la gouttière, la bande de portée les borde | 6–7 |
| Multi-curseurs | non repris (aucun besoin exprimé) | 6 |
| Sélection en colonne | non repris (aucun besoin exprimé) | 6 |
| Saisie / IME | **réécrit** (chantiers 10-11, 17) : Entrée et Suppr réparés, l'entrelacement tenu | 6 |
| Accessibilité | non repris (aucun besoin exprimé) | 6 |
| Texte bidirectionnel | non repris (aucun besoin exprimé) | 6 |
| Guides d'indentation | **réécrit** (chantier 14) : la chaîne d'appariement de L2 guide, pas les espaces | 8 |
| Recherche / remplacement | **réécrit** (chantier 24) : la recherche teinte la fenêtre (plafond de 10 000 correspondances), la navigation boucle, remplacer la courante (une unité d'annulation) et tout remplacer (de la fin vers le début, une seule unité) | 8 |
| Types du domaine LSP | **réécrit** (chantiers 25 puis 26) : `:lsp` sur lsp4j 1.0.0 (la décision du propriétaire — jsonrpc et gson en transitif, `NOTICE.md` les consigne) ; les adaptateurs du domaine (position, intervalle, diagnostic) convertissent vers le tampon | 9 |
| Ordonnancement des requêtes LSP | **réécrit** (chantiers 25 puis 26) : la corrélation par identifiant vit dans lsp4j, la liaison tient le vocabulaire du dépôt, trois raccordements (en processus par tubes, processus externe, flux) ; l'arrêt ferme proprement | 9 |
| Catalogue des langages | examiné au chantier 2 : réécriture proposée — un langage = un descripteur (extensions, lexeur, table jeton → portée, analyseur et annotateur facultatifs), famille générique pour la longue traîne ; validation avec le propriétaire | 3 |
| Coloration par grammaires TextMate (chargement, tissage, expressions) | abandon proposé — hors du cœur par le plan (§4.8) ; le profil du chantier 2 mesure le coût de chargement qui le justifie (~1 s JVM x86 pour 27 grammaires, plusieurs secondes sur téléphone) ; modules optionnels seulement si un besoin réel apparaît | — |

Toutes les lignes des chantiers livrés portent leur décision (réécrit
ou non repris) ; la ligne du catalogue des langages attend la
validation du propriétaire avec le document de conception du moteur
(`conception-coloration.md`), la feuille de route §8 est épuisée —
les reprises futures naîtront d'un besoin exprimé.
