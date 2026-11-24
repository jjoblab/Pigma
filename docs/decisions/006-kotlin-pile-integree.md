# Décision 006 — Kotlin par la voie générée, la pile internée dans l'état

**Date** : 24 novembre 2026. **Statut** : acceptée (le propriétaire a
choisi le chantier recommandé à l'issue du chantier 4).

## Contexte

La conception (décision 001, `conception-coloration.md`) prévoyait :
« Kotlin suit la voie générée de Java », et pour le contrat du lexeur :
« l'état est un petit entier qui décrit tout le contexte ; quand le
contexte est une pile (gabarit dans chaîne), la pile est internée :
deux piles égales, même entier ». Le chantier 4 (fenêtre paresseuse)
a livré le moteur qui transporte ces états par ligne ; le premier
retour d'installation a également apporté le bouton « Copier les
mesures » pour les relevés appareil. Restait le langage : Kotlin
imbrique — un gabarit `${...}` dans une chaîne peut contenir des
chaînes elles-mêmes gabarits, et des commentaires de bloc eux-mêmes
imbriqués — ce qu'un état plat ne décrit pas.

## Décision

1. **Kotlin par la voie générée** (JFlex, code versionné), cinquième
   lexeur du registre : cinq états traversants, descriptions et
   approximations documentées dans `kotlin.flex` comme pour Java.
2. **La pile s'encode canoniquement dans l'entier d'état** : l'état
   plat dans les 4 bits bas, la pile au-dessus (4 bits par contexte,
   fond vers sommet, profondeur 6). Encodage bijectif — l'internement
   de la conception, au sens strict, sans table ni ordre de rencontre
   — deux piles égales rendent le même entier, l'inverse reconstruit
   la pile exacte. Le cœur, le moteur, la règle d'arrêt et les points
   de contrôle restent inchangés : l'état de Kotlin est resté un
   entier.
3. **Les chaînes classiques meurent à la frontière de fenêtre** (elles
   ne traversent pas les lignes, comme en Java) : tout contexte ouvert
   au-dessus d'une telle chaîne meurt avec elle. Approximation
   documentée, testée, cohérente avec le lexeur Java existant.
4. Portées fines servies par le repli : `string.raw` et
   `string.escape` replient sur `string` — aucun changement de thème
   ni de moteur.

## Alternatives écartées

- **Table d'internement** (pile → identifiant par compteur) :
  dépendante de l'ordre de rencontre, donc de l'ordre des tests dans
  une même machine virtuelle — les sorties figées auraient dérivé.
  L'encodage bijectif est déterministe par construction.
- **États plats seulement** (gabarits non imbriqués, commentaires non
  imbriqués) : aurait décoloré les constructions réelles de Kotlin —
  le langage se prête justement à l'imbrication.
- **Famille générique paramétrée** : ne peut pas décrire les gabarits
  imbriqués ni les chaînes brutes multi-lignes ; la voie générée reste
  pour les langages de tête.
