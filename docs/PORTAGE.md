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
| Popups (feuille du bas/haut, popup ancrée) | à examiner | 8 |
| Gouttière (numéros, marqueurs) | à examiner | 6 |
| Pliage | à examiner | 6–7 |
| Multi-curseurs | à examiner | 6 |
| Sélection en colonne | à examiner | 6 |
| Saisie / IME | à examiner | 6 |
| Accessibilité | à examiner | 6 |
| Texte bidirectionnel | à examiner | 6 |
| Guides d'indentation | à examiner | 8 |
| Recherche / remplacement | à examiner | 8 |
| Types du domaine LSP | à examiner | 9 |
| Ordonnancement des requêtes LSP | à examiner | 9 |
| Catalogue des langages | à examiner | 3 |

Aucune décision n'est encore prise : le chantier 0 ne porte que le
squelette, l'examen des candidats commence avec les chantiers concernés.
