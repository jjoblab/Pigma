# État de la mission Pigma

Mise à jour : session 33, le 2026-11-25. Rester factuel et
vérifiable face à `git log` : chaque affirmation ci-dessous se
retrouve dans le dépôt ou dans un journal de build.

## Point d'arrêt

**Le chantier 64 poussé sur `origin/main` — les fonctions étendues
du protocole à la couverture de la référence (Sora editor) : la
négociation des capacités, l'aller-à la définition, les références,
le surlignage des occurrences, le renommage à la carte, les
corrections rapides du serveur, le formatage. L'APK reconstruit et
livré dans download/.**

## Session 33 — le chantier 64, le protocole étendu (2026-11-25)

1. **L'étude de la référence** : la façade de Sora editor porte le
   renommage, l'aller-à, les références, le formatage — la leçon
   s'y applique au module `:lsp` : la négociation des capacités
   (`LspCapabilities`) y décide ce que la façade OFFRE (demander une
   fonction non déclarée offense le protocole), les types du domaine
   s'y posent (`LspLocation`, `LspTextEdit`, `LspCodeAction`), les
   six requêtes s'y ajoutent (`definition`, `references`,
   `codeActions`, `prepareRename`, `rename`, `documentHighlight`,
   `formatting`) — **18/18 tests** (l'aller-à aux deux formes, les
   références à la déclaration près, les corrections aux deux formes
   de l'édition d'espace de travail, le renommage pareillement, le
   surlignage, le formatage, les capacités à la déclaration).
2. **La façade porte les fonctions** (`LspEditor`) : l'ampoule les
   porte APRÈS les intentions de l'hôte (le second fournisseur —
   `setServerQuickActionProvider` : « Aller à la définition »,
   « Trouver les références », « Renommer le symbole », « Formater
   le document », chacun sur sa capacité déclarée) ; le surlignage
   des occurrences suit le curseur (amorti aux 250 ms, la génération
   déclasse la réponse d'avant, `setDocumentHighlightEnabled` la
   coupe et la REPOSE) ; le renommage s'éprouve par la carte au
   caret (`requestRename` — la préparation du protocole vérifie le
   symbole, la validation part, les remplacements se posent en UNE
   unité d'annulation) ; l'aller-à et les références naviguent (la
   place du MÊME document fait sauter le curseur, celle d'un autre
   document part à l'écouteur de l'hôte — `setNavigationListener`)
   ; le formatage (`formatDocument`) et les corrections rapides du
   serveur (le cache par ligne, la carte ouverte se rouvre fraîche)
   posent leurs remplacements pareillement.
3. **La carte de renommage** (`RenameCard`, dessinée dans la vue) :
   posée au caret, le clavier délégué (la famille de la recherche et
   de l'aller-à), le nom neuf démarre du symbole (la façon
   d'Android Studio — il se corrige, il ne se retape pas), la
   validation part au porteur, la croix la range.
4. **Les remplacements du protocole** (`applyTextEdits` sur la
   vue) : chaque plage se remplace en une transaction annulable
   d'un geste, posée du DERNIER au PREMIER (les plages du protocole
   vivent dans le document d'avant). Le surlignage serveur vit en
   bleu (0x332979FF) — la recherche demeure ambre, les deux
   familles se lisent d'un coup d'œil.
5. **Le serveur de démonstration étendu** : les sept capacités s'y
   déclarent, la déclaration se lit aux méthodes ET aux champs
   finaux (le mot suivi du signe égal, la ligne porte « final »),
   les occurrences se comptent aux bornes de mot près, la
   correction coupe les espaces de fin de ligne, le formatage coupe
   ceux de toutes les lignes.
6. **L'éprouve** : harnais **37/37** (un scénario neuf — « protocole
   étendu : capacités, surlignage, aller-à, renommage, correctif,
   formatage » : les capacités se négocient, les deux occurrences
   se teintent, la fonction coupée range la teinte et le retour la
   repose, l'ampoule porte les quatre fonctions, l'aller-à saute de
   l'usage à la déclaration, les références surlignent chaque
   usage, « MAX » devient « PLAFOND » aux DEUX occurrences d'une
   seule unité d'annulation, la correction coupe les espaces de la
   ligne, le formatage coupe ceux de toutes). Syntaxe **26/26**,
   LSP **18/18**, politique **OK** (131 fichiers). **APK rebâti —
   6,97 Mo, signé debug, livré dans download/pigma-sample-release.apk**
   (la façade, la carte de renommage et les requêtes neuves y
   vérifiées au dex).
7. **La sauvegarde en cours de chantier** : le socle interrompu y
   fut poussé d'abord (`772a0db` — la demande de l'utilisateur, le
   travail protégé d'une autre interruption) avant le chantier
   complet.

## Ce qui n'a PAS pu être fait (honnêteté)

- Les fonctions neuves sur appareil (l'ampoule du serveur réel
  EmmyLua, le renommage au doigt) attendent leur contre-épreuve de
  terrain — la démonstration en processus y répond seule.
- Les fonctions du protocole qui restent à la référence :
  l'inlay hint, les jetons sémantiques, les symboles du document,
  les plis du serveur — la couverture courante porte les fonctions
  de NAVIGATION et d'ÉDITION, la coloration demeure locale.

## Le programme du relevé 22 (le cycle d'appareil)

1. Installer l'APK reconstruit : brancher « LSP » — l'ampoule de
   la gouttière porte l'aller-à, les références, le renommage, le
   formatage ; le curseur posé sur un mot, ses occurrences se
   teintent en bleu (la recherche reste ambre).
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
