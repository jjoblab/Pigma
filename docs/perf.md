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
défilement, pompe et copie des mesures) tient en 82 Ko.

Changer de thème ne retokenise rien : les jetons portent des portées,
la couleur se résout au dessin (le panneau le montre : la mesure de
première coloration ne bouge pas quand on bascule clair/sombre).

## Appareil (à relever par le propriétaire)

Le bac à sable n'a pas d'appareil : les lignes suivantes se remplissent
depuis le panneau de diagnostic (`adb install
sample/build/outputs/apk/debug/sample-debug.apk`, ouvrir l'application,
taper, lire le panneau) — chaque langue, le gros fichier et chaque
thème y passent.

| Mesure | Méthode | Valeur |
| --- | --- | --- |
| Première coloration, texte de démonstration | ouvrir l'application, lire le panneau | à relever |
| Première coloration, gros fichier (bouton « Gros ») | appuyer, lire le panneau : seule la fenêtre visible se paie | à relever |
| Frappe ordinaire (recoloration d'une ligne) | taper, lire « dernière frappe » | à relever |
| Frappe propagée (commentaire ouvert) | supprimer un `*/`, lire les lignes relues | à relever |
| Remplissage du gros fichier | lire « lignes lexées » monter après l'ouverture (pompe par tranches) | à relever |
| Bascule de thème | basculer : la première coloration ne doit pas bouger | à relever |
| Budget par image (tracé d'une trame) | à instrumenter avec la vue complète | à relever |

Les plafonds du plan sont codés (E3) : ligne de plus de 20 000 colonnes
non colorée (état reporté, voisines intactes), dessin limité à 10 000
colonnes (vue), mode gros fichier au-delà de 2,5 M caractères ou
50 000 lignes (`inBigFileMode()` : la coloration L1 n'y est jamais
coupée, L2 et L3 s'y couperont). Ils s'ajusteront sur ces relevés.
