# Mesures de performance du moteur de coloration

Ce document reçoit les mesures du plan : d'abord celles que la machine
de build sait prendre (JVM, tests du module `:syntax`), puis celles de
l'appareil, relevées avec le **panneau de diagnostic du sample** (le
texte sous l'éditeur affiche première coloration, coût de la dernière
demande — le saut lointain s'y lit —, coût de la dernière frappe,
lignes relues, lignes lexées sur le total, jetons) : le bouton
**« Copier les mesures »** met le journal du scénario au presse-papiers
en tableau prêt à coller ici. Les
nombres JVM servent de plancher et de garde-fou ; seuls les nombres
appareil décident des plafonds.

## Machine de build (JVM x86-64, JDK 17)

Moteur complet (`SyntaxDocument`, lexeur Java généré), corpus Java de
2 010 lignes / 60 154 caractères, test `EngineMeasureTest` du module
`syntax` — la fenêtre paresseuse (E3) est en place : la première
couleur ne paie que la fenêtre visible :

| Mesure | E2 (tout le document) | E3 (fenêtre paresseuse) |
| --- | --- | --- |
| Première couleur (40 lignes visibles) | — | **1,91 ms** |
| Remplissage complet (tranches de 256 lignes) | — | 10,7 ms |
| Premier lexage complet (ancien modèle) | 11,8 ms | — |
| Jetons produits | 26 480 | 26 480 |
| Frappe ordinaire (une ligne relue) | 0,03 ms | 0,05 ms |
| Frappe propagée (fermeture de commentaire retirée, 7 lignes relues) | 0,03 ms | 0,03 ms |

Un saut lointain dans une zone libérée repart du point de contrôle le
plus proche : le test `LazyWindowTest.farJumpRestartsFromCheckpoint`
sert la ligne 500 d'un document de 2 010 lignes après libération en
ne lexant que 245 lignes (depuis le point de contrôle à 256), et rend
des jetons égaux à ceux d'un document neuf.

Lexeurs seuls (banc E0, `bench/`, mêmes corpus, passes rechauffées) :
1,7 à 3,5 ms pour ~2 000 lignes, 14 à 47 Mo/s selon le langage ; à JVM
froide, 3 à 16 ms. Le lexeur Kotlin (chantier 5, corpus de 1 982
lignes / 73 155 caractères, meilleure passe rechauffée) : **0,95 ms**,
73,6 Mo/s. Poids DEX par langage (d8, min-api 26) : 18 788 octets
(Java), 7 804 (XML), 7 420 (JSON), **21 512 (Kotlin** — la pile de
contextes est dans ce prix) — l'APK de démonstration complet (moteur,
quatre lexeurs générés, famille générique, interface du sample avec
défilement, fil de fond, copie des mesures et cache de rendu) tient
sous 82 Ko (79,6 Ko en construction propre après le chantier 6).

Changer de thème ne retokenise rien : les jetons portent des portées,
la couleur se résout au dessin (le panneau le montre : la mesure de
première coloration ne bouge pas quand on bascule clair/sombre).

## Appareil (relevés du propriétaire, 2026-11-24)

Le bac à sable n'a pas d'appareil : les lignes suivantes se remplissent
depuis le panneau de diagnostic et le bouton « Copier les mesures »
(journal du scénario au presse-papiers, tableau prêt à coller). Cinq
relevés reçus le même jour — chantier 5, chantier 6 (colonne
« trame »), chantier 7 (fil de fond annulable), chantier 8 (odomètre de
trame), chantiers 9+10 (recul du fond, structure L2) ; l'appareil
n'est pas précisé (modèle et version d'Android à compléter — la ligne
du tableau copié les demande).

| Mesure | Méthode | Valeur |
| --- | --- | --- |
| Première coloration, texte de démonstration | ouvrir l'application, lire le panneau | Java 0,5 · XML 0,2–0,5 · JSON 0,1 · Kotlin 0,2 · SQL 0,1 ms (première demande : la fenêtre visible seule, jamais le document) |
| Première coloration, gros fichier (bouton « Gros ») | appuyer, lire le panneau : seule la fenêtre visible se paie | **0,3 ms** (première demande, deux relevés) ; 1 066–1 322/1 780 lignes déjà lexées au premier tick du panneau (0,2 s) — le remplissage seul (aux relevés : pompe d'interface ; depuis le chantier 7, fil de fond), la couleur est déjà là |
| Frappe ordinaire (recoloration d'une ligne) | taper, lire « dernière frappe » | **0,26 ms, 1 ligne relue** (cinquième relevé, gros fichier — un caractère tapé avant la copie ; la trame de la frappe : 10,09 ms, moteur 0,21, pire 10,94 — le coût est au rendu, pas au moteur, et tient le budget 60 Hz) |
| Frappe propagée (commentaire ouvert) | supprimer un `*/`, lire les lignes relues | à relever : idem, dans la démo Java (supprimer un `*/` du bloc) |
| Remplissage du gros fichier | lire « lignes lexées » monter après l'ouverture (aux deux premiers relevés : pompe par tranches sur le fil d'interface ; au troisième : fil de fond annulable, clichés révision-vérifiés) | **1 780/1 780 lignes, 18 095 jetons** ; au troisième relevé, complet **avant le premier tick (0,2 s)** — le fond remplit en ~150 ms ce que la pompe étalait sur ~1 s |
| Bascule de thème | basculer : la première coloration ne doit pas bouger | **première inchangée (0,3 ms), jetons identiques (18 095), deux relevés et trois bascules : zéro retokenisation constaté sur appareil** |
| Budget par image (tracé d'une trame) | lire « trame » au panneau ; la colonne « trame » du journal copié l'emporte | **première trame 16,0 ms** (démo Java : cuisson des plans + fenêtre) · **11,9 ms** à l'ouverture du gros fichier · **4,9–6,3 ms en régime établi** (recuisson au thème 5,1–6,3, copie 4,9) · troisième relevé (chantier 7) : 15,5–17,1 ms sur le gros fichier — voir la note · quatrième relevé (chantier 8, odomètre) : régime établi confirmé à 4,5–5,5 ms, moteur 0,09–0,23 ms ; ouverture du gros fichier **moteur 9,23 ms** (réouverture tiède : 4,95) — voir la note · cinquième relevé (chantiers 9+10) : **recul du fond confirmé — ouverture 13,51 ms (moteur 5,93), réouverture 8,49 (moteur 3,24)** ; recuisson **3,46 ms (moteur 0,06)** ; trame de frappe 10,09 (moteur 0,21) — voir la note |

Deux lectures de ce relevé. D'abord, ne pas comparer la colonne
appareil aux nombres JVM du haut sans précaution : le test JVM paie
une fenêtre de 40 lignes d'un coup (1,91 ms), le panneau appareil
mesure la première demande (une à deux lignes) — la paresse se voit
précisément en ce que la première couleur ne paie pas le document.

Ensuite, le relevé porte le coût interprété de la famille générique :
une ligne relue paie moins de 0,2 ms pour les quatre lexeurs générés
(Java 0,18, XML 0,07, JSON 0,04, Kotlin 0,04) mais **0,50 à 0,78 ms
pour le profil SQL** (les deux relevés) — chaque fenêtre réinterprète
son profil, exactement le poids que l'étude de faisabilité voyait sur
le démarrage. C'est la
longueur délibérée de la traîne : les langages de tête restent
générés, la famille générique sert les langages rares ; pour un gros
fichier SQL, il faudra soit l'accepter, soit générer le profil.

Le deuxième relevé porte la colonne « trame » (chantier 6) : le budget
par image est mesuré sur appareil. Première trame de la démo **16,0 ms**
(la première cuisson des plans et la fenêtre initiale s'y paient),
ouverture du gros fichier **11,9 ms**, puis **4,9 à 6,3 ms en régime
établi** — une bascule de thème, qui recuit tous les plans visibles
(re-rendu, jamais retokenisation), tient dans la même fourchette.
Construction debug et appareil non précisés : ces nombres donnent
l'ordre de grandeur, pas des plafonds ; la mesure de référence viendra
de la construction release — le fil de fond a déchargé le fil
d'interface (chantier 7) : un prochain relevé dira si la trame du
remplissage en profite.

Le troisième relevé porte le fil de fond (chantier 7) : le
remplissage complet du gros fichier est là **avant le premier tick du
panneau** (1 780/1 780) — la promesse d'E6 tient sur appareil. Mais
la **trame du gros fichier monte à 15,5–17,1 ms** (contre 11,9 à
l'ouverture et 4,9–6,3 en régime établi au relevé précédent) alors
que les démons restent au niveau (XML 3,7 · JSON 5,3 · Kotlin 10,3)
et que la demande moteur monte elle aussi de 0,25 à 0,40 ms (2
lignes). Le chemin de dessin n'a pas changé entre les deux APK (le
moteur ne fait que s'ajouter un verrou court, jamais tenu pendant le
lexage). Hypothèse principale : un état de l'appareil — la salve du
fond (~150 ms à plein régime, là où la pompe passait au ralenti)
peut faire baisser la fréquence ou monter la température, et tout le
reste ralentit avec. Le chantier 8 répond par la mesure : la cellule
trame du journal porte désormais **« (moteur, pire) »** — le temps
moteur de la trame (verrou compris) et la pire trame du scénario ;
moteur bas et trame haute innocente le moteur, moteur haute désigne
le verrou. La montre du fond (« remplissage fini (fond X ms, N
tranches) ») chronomètre la passe. Le relevé suivant tranche.

Le quatrième relevé tranche. **En régime établi, le moteur est
innocent** : les recuissons de thème tiennent 4,46–5,51 ms avec un
temps moteur de **0,09–0,23 ms** (la régression du troisième relevé
était un état transitoire de l'appareil — son pic à 13,72 ms affiche
encore moteur 0,23). Reste l'ouverture du gros fichier : **moteur
9,23 ms** sur 16,08 — la salve du fond, relancée au `textChanged` du
remplacement, chevauchait la première trame (le verrou du moteur y
attend les tranches) ; à cela s'ajoute l'interprétation à froid du
premier lexage (la réouverture, tiède, tombe à moteur 4,95 ms ; la
démo Kotlin, première ouverte, paie 2,15 ms pour 28 lignes). Le
chantier 9 corrige la part actionnable : le remplissage ne part plus
à l'édition mais **à la fin de la trame qui suit** (le `ensureFill`
de `onDraw`), la salve ne chevauche plus jamais le dessin — le
premier tick du panneau garde 1 780/1 780, la montre du fond
chronométrera la passe décalée. Toujours aucune frappe dans le
relevé : les lignes « frappe » attendent.

Le cinquième relevé apporte les deux réponses attendues. **La
frappe, enfin** : un caractère tapé dans le gros fichier avant la
copie — **0,26 ms, 1 ligne relue** (la ligne « copie » la porte, comme
prévu) ; la trame de la frappe monte à 10,09 ms avec un moteur à
0,21 : le coût d'une frappe est au rendu (le canevas de la fenêtre et
la recuisson de la ligne), pas au moteur — et le pire du scénario
(10,94) tient le budget 60 Hz. **Le recul du fond, confirmé** :
l'ouverture du gros fichier tombe à 13,51 ms (moteur 5,93) puis
8,49 (moteur 3,24) en réouverture — contre 16,08 (9,23) et 10,99
(4,95) au relevé précédent : la salve ne chevauche plus la première
trame. Le moteur restant à l'ouverture est l'interprétation de la
fenêtre visible (construction debug — le chantier 13 livre la
construction release de mesure). Et le régime établi se précise :
recuisson **3,46 ms, moteur 0,06**. Une lecture de journal : la
bascule de thème redessine APRÈS le clic — la ligne « thème » porte
la dernière trame terminée au moment du clic, la recuisson se lit
sur la ligne suivante (première ligne Sombre : la dernière trame
JSON ; deuxième : la recuisson précédente, 3,46 ms).

Les plafonds du plan sont codés (E3) : ligne de plus de 20 000 colonnes
non colorée (état reporté, voisines intactes), dessin limité à 10 000
colonnes (vue), mode gros fichier au-delà de 2,5 M caractères ou
50 000 lignes (`inBigFileMode()` : la coloration L1 n'y est jamais
coupée, L2 et L3 s'y couperont). Ils s'ajusteront sur ces relevés.
