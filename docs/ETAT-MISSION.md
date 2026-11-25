# État de la mission Pigma

Mise à jour : session 34, le 2026-11-25. Rester factuel et
vérifiable face à `git log` : chaque affirmation ci-dessous se
retrouve dans le dépôt ou dans un journal de build.

## Point d'arrêt

**Le chantier 65 poussé sur `origin/main` — les fonctions du document
à la couverture complétée : les symboles (la table des matières à
l'ampoule), les indices posés dans le texte, les plis du serveur,
les jetons sémantiques (la coloration du serveur par-dessus le
lexer). Le nom de l'éditeur de référence ne vit plus NULLE PART dans
le dépôt (la consigne du propriétaire) — le contrôle local
`check-forbidden-refs` y veille. L'APK reconstruit et livré dans
download/.**

## Session 34 — le chantier 65, les fonctions du document (2026-11-25)

1. **La consigne du propriétaire d'abord** : « tu ne dois pas
   mentionner [l'éditeur de référence] dans le code » — les onze
   mentions (commentaires, README, journaux) deviennent « l'éditeur
   de référence » ; le contrôle `check-forbidden-refs` reçoit le
   motif en local (`config/forbidden-refs.local`, jamais versionné —
   le nom ne revit pas non plus dans le contrôle) : **OK (298
   fichiers examinés)**, et la consigne y reste gardée.
2. **Les quatre fonctions du document au module `:lsp`** : les types
   du domaine s'y posent (`LspDocumentSymbol` — le nom, le genre,
   l'étendue et la sélection ; `LspFoldingRange` — l'en-tête et la
   fin ; `LspInlayHint` — la position et l'étiquette ;
   `LspSemanticToken` — la plage, le genre et les modificateurs), les
   quatre requêtes s'y ajoutent (`documentSymbol` aux deux formes du
   protocole, `foldingRange`, `inlayHint` à la plage interrogée,
   `semanticTokens` décodé de l'encodage RELATIF à la légende de la
   négociation — `LspSession` la capture à l'initialisation),
   `LspCapabilities` porte les quatre déclarations — **22/22 tests**
   (les symboles aux deux formes, le pli, l'indice, le décodage à la
   légende, les capacités à la déclaration).
3. **La pompe du document** (`LspEditor`) : les quatre fonctions
   décrivent le DOCUMENT, pas le curseur — elles se redemandent à
   CHAQUE synchronisation (la connexion y démarre, le `didChange`
   amorti y suit ; la génération déclasse les réponses du document
   d'avant) ; l'ampoule y porte « Les symboles du document » (sur sa
   capacité déclarée), son choix ouvre la CARTE DES SYMBOLES (chaque
   rangée porte le nom et sa ligne, le choix y fait sauter le curseur
   à la sélection — la façon du panneau de structure de l'atelier) ;
   le débranchement range tout (les avis du serveur mort ne valent
   plus rien).
4. **Les indices dans le texte** (`EditorView.drawInlayHint`) :
   l'étiquette se dessine au bout du contenu de la ligne, l'encre
   secondaire (0xFF8A8A93, le corps à 85 %) — le texte n'en bouge
   pas ; sans place à l'écran, l'indice se tait.
5. **Les plis du serveur** (`FoldBounds` au paquet folding/) : les
   bornes du lexer s'y lisent par interface, la vue y compose les
   deux (l'ouvrant local d'abord, la région du serveur complète —
   `setServerFoldRegions`) ; la gouttière y porte les marqueurs des
   deux familles, `toggleFoldAt` y replie les deux pareillement.
6. **Les jetons sémantiques** (la fusion L4 à la cuisson) : le jeton
   du serveur qui couvre un span y porte sa couleur (le genre à la
   palette — « function » la méthode, « constant » la constante,
   « variable » le champ), la DÉCLARATION y porte le gras ; un genre
   inconnu laisse la couleur d'avant (la chaîne ouverte ne casse
   jamais le rendu) ; les plans de rendu se recuisent à l'arrivée
   (c'est un re-rendu, jamais une retokenisation — les jetons du
   moteur ne bougent pas).
7. **Le serveur de démonstration étendu** : les onze capacités s'y
   déclarent ; les symboles s'y rendent à chaque déclaration (le
   genre méthode ou champ), les plis aux blocs d'accolades appariées,
   les indices au compte des usages au bout des déclarations (la
   plage interrogée y borne la réponse), les jetons sémantiques à la
   légende posée (« keyword », « variable », « function »,
   « constant » — le modificateur « declaration » au bit près).
8. **La carte qui défile** (`QuickActionCard.showRow`) : les fonctions
   neuves remplissent l'ampoule au-delà du plafond de rangées — la
   fenêtre s'y déplace vers la rangée demandée (la rangée sort de la
   fenêtre, le doigt la vise quand même).
9. **L'éprouve** : harnais **38/38** (un scénario neuf — « fonctions
   du document : symboles, indices, plis du serveur, jetons
   sémantiques » : les quatre capacités se négocient, la pompe verse
   ses quatre avis, l'indice compte les usages de la déclaration, le
   jeton de « MAX » porte « constant » et « declaration », le pli du
   serveur replie sa région (une région que le lexer n'a pas — les
   lignes du commentaire de tête), l'ampoule porte la table des
   matières, le choix d'un symbole y saute le curseur, le
   débranchement range tout). Syntaxe **26/26**, LSP **22/22**,
   politique **OK** (136 fichiers). **APK rebâti — 6,99 Mo, signé
   debug, livré dans download/pigma-sample-release.apk** (les quatre
   types neufs et `FoldBounds` y vérifiés au dex).

## Ce qui n'a PAS pu être fait (honnêteté)

- Les fonctions neuves sur appareil (les indices au doigt, la table
  des matières au doigt, les jetons du serveur réel EmmyLua) attendent
  leur contre-épreuve de terrain — la démonstration en processus y
  répond seule.
- La couverture du protocole côté client est close (navigation,
  édition, document) ; ce qui n'y vit pas : les requêtes de
  l'espace de travail au-delà du document ouvert (`workspace/symbol`,
  `references` multi-documents dans la façade — l'écouteur
  `setNavigationListener` y tient déjà le relais), le delta des
  jetons sémantiques (`semanticTokens/full/delta` — la requête pleine
  s'y suffit).

## Le programme du relevé 22 (le cycle d'appareil)

1. Installer l'APK reconstruit : brancher « LSP » — l'ampoule de
   la gouttière porte l'aller-à, les références, le renommage, le
   formatage, les symboles du document ; le curseur posé sur un
   mot, ses occurrences se teintent en bleu (la recherche reste
   ambre), les déclarations portent l'indice du compte des usages,
   le gras de la coloration du serveur.
2. Le renommage au doigt : « Renommer le symbole » — le nom neuf
   démarre du symbole, la validation remplace chaque occurrence,
   UN geste annule tout.
3. Les popups translucides et le style de l'hôte (le relevé 20
   attend toujours sa contre-épreuve de terrain).
4. Le pincement du chantier 63 (le facteur sous les doigts, la
   ligne qui y demeure, les bornes qui serrent).
5. Le hors normes Java après la compaction : le Δ mémoire par
   remplissage et le temps de fond (la rangée du tableau).
6. « Tout remplacer » sur le hors normes (le correctif du
   chantier 33 attend sa contre-épreuve).
7. Appareil : modèle et version d'Android (la ligne du tableau
   copié les demande depuis le premier relevé).

## Journal des sessions précédentes (l'essentiel, le détail dans git)

- **Session 33** (`4fb02fa`) : le chantier 64 — les fonctions
  étendues du protocole (capacités, aller-à, références, surlignage,
  renommage, correctifs, formatage), la carte de renommage.
- **Session 32** (`c44d3c8`) : le chantier 63 — la façade LspEditor
  (l'hôte ne câble plus rien), le pincement du zoom au deux doigts,
  un README par module.
- **Session 31** (`802024b`) : le chantier 62 — la carte de
  diagnostic, l'ampoule jaune, le style de l'hôte.
- **Session 30** (`fd3f9e4`) : le chantier 61 — les popups de
  seconde génération.
- **Session 29** (`346f41b`) : les chantiers 56 à 60 poussés ; la
  pousse réparée à l'identité du propriétaire.
- **Sessions 24 à 28** : les paquets input/, folding/, selection/ ;
  la recherche et l'aller-à à la facture de l'atelier ; les
  migrations finies.
- Les chantiers 0 à 37 : la voie complète du moteur — le détail
  vit dans `docs/chantiers/` et l'historique git.
