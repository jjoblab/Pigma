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
(journal du scénario au presse-papiers, tableau prêt à coller). Deux
relevés reçus le même jour — le premier (APK du chantier 5), le
second (APK du chantier 6, avec la colonne « trame ») ; l'appareil
n'est pas précisé (modèle et version d'Android à compléter — la ligne
du tableau copié les demande).

| Mesure | Méthode | Valeur |
| --- | --- | --- |
| Première coloration, texte de démonstration | ouvrir l'application, lire le panneau | Java 0,5 · XML 0,2–0,5 · JSON 0,1 · Kotlin 0,2 · SQL 0,1 ms (première demande : la fenêtre visible seule, jamais le document) |
| Première coloration, gros fichier (bouton « Gros ») | appuyer, lire le panneau : seule la fenêtre visible se paie | **0,3 ms** (première demande, deux relevés) ; 1 066–1 322/1 780 lignes déjà lexées au premier tick du panneau (0,2 s) — le remplissage seul (aux relevés : pompe d'interface ; depuis le chantier 7, fil de fond), la couleur est déjà là |
| Frappe ordinaire (recoloration d'une ligne) | taper, lire « dernière frappe » | à relever : aucune frappe dans les deux relevés (taper puis « Copier les mesures », la ligne « copie » porte la dernière frappe) |
| Frappe propagée (commentaire ouvert) | supprimer un `*/`, lire les lignes relues | à relever : idem, dans la démo Java (supprimer un `*/` du bloc) |
| Remplissage du gros fichier | lire « lignes lexées » monter après l'ouverture (aux relevés : pompe par tranches sur le fil d'interface ; depuis le chantier 7 : fil de fond annulable, clichés révision-vérifiés) | **1 780/1 780 lignes, 18 095 jetons** (deux relevés) |
| Bascule de thème | basculer : la première coloration ne doit pas bouger | **première inchangée (0,3 ms), jetons identiques (18 095), deux relevés et trois bascules : zéro retokenisation constaté sur appareil** |
| Budget par image (tracé d'une trame) | lire « trame » au panneau ; la colonne « trame » du journal copié l'emporte | **première trame 16,0 ms** (démo Java : cuisson des plans + fenêtre) · **11,9 ms** à l'ouverture du gros fichier · **4,9–6,3 ms en régime établi** (recuisson au thème 5,1–6,3, copie 4,9) |

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

Les plafonds du plan sont codés (E3) : ligne de plus de 20 000 colonnes
non colorée (état reporté, voisines intactes), dessin limité à 10 000
colonnes (vue), mode gros fichier au-delà de 2,5 M caractères ou
50 000 lignes (`inBigFileMode()` : la coloration L1 n'y est jamais
coupée, L2 et L3 s'y couperont). Ils s'ajusteront sur ces relevés.
