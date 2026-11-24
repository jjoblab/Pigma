# Décision 004 — tampon de texte, transactions et annulation

## Contexte

Le cœur de la bibliothèque est un modèle de texte. Exigences du plan :
offsets UTF-16, plages, transactions, annulation/rétablissement, et une
vue d'édition minimale dans le sample qui tape et annule dessus. Les
exigences de cohérence à venir (couleur jamais périmée, annulation
exacte après des éditions quelconques) reposent déjà sur ces mécanismes.

## Choix

1. **Tampon à trou** (`gap buffer`) : les éditions autour de la position
   courante sont quasi constantes, sans recopie globale à chaque frappe.
   L'**index de lignes** est maintenu incrémentalement (recherche
   binaire + décalage des débuts de ligne au-delà de l'édition), jamais
   recalculé en entier.
2. **Sémantique UTF-16 explicite** : les offsets, colonnes et longueurs
   sont des unités de code UTF-16, comme dans `String`. Un remplacement à
   cheval sur une paire de substitution est licite au niveau unité ;
   annulé, il restitue le texte exact d'avant (garanti par le test
   différentiel aléatoire).
3. **Lignes = séparateur `'\n'` seul** : `'\r'` est un caractère
   ordinaire (fins de ligne Windows portées par deux lignes logiques).
4. **Révision monotone** : incrémentée à chaque remplacement effectif,
   portée par chaque événement envoyé aux écouteurs. Les couches
   supérieures (lexeur, analyseur) s'en serviront pour jeter leurs
   résultats périmés — c'est le contrat de version du texte.
5. **Écritures publiques par transaction seulement** : une transaction
   applique immédiatement au tampon, enregistre les textes ôtés, et
   s'engage en une unité d'annulation. Le retour arrière rejoue les
   inverses (le tampon retrouve exactement son état, la révision avance,
   les écouteurs sont notifiés). Une seule transaction ouverte à la fois ;
   annuler au milieu d'une transaction ouverte est interdit.
6. **`close()` engage** (compatible try-with-resources) ; le retour
   arrière est explicite dans le `catch` — une exception dans le bloc try
   n'annule rien. Choix délibéré, documenté dans la Javadoc : prévisible
   plutôt que magique.
7. **Pile bornée** (256 unités par défaut) : au-delà, les unités les plus
   anciennes sont oubliées. Une nouvelle édition engage efface la branche
   de rétablissement. `undo()` et `redo()` renvoient les changements
   appliqués au tampon : la vue s'en servira pour replacer le curseur.
8. **Une transaction par retour d'IME** dans le sample : chaque
   commit/effacement/composition du clavier est une unité d'annulation ;
   la granularité d'une composition tapée lettre à lettre suit donc les
   mises à jour de l'IME (comportement minimal, affiné avec la vraie vue
   d'édition).

## Conséquences

- L'éditeur minimal du sample fonctionne sans AndroidX ni EditText : la
  vue dessine le tampon, l'IME écrit dedans par transactions.
- La pureté Android du cœur est garantie par un test ArchUnit sur les
  packages (aucune dépendance `android.*`), pas par un module par couche.
- Le test différentiel aléatoire (graines fixes, 600 opérations par
  graine) compare tampon + pile d'annulation à un modèle de référence
  écrit sur `StringBuilder` : texte, lignes, conversions offset/ligne et
  indicateurs d'état doivent coïncider après chaque opération. C'est
  l'embryon du futur test de propriété de cohérence incrémentale.
