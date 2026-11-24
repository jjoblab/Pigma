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
(journal du scénario au presse-papiers, tableau prêt à coller). Dix-
sept relevés reçus le même jour — chantier 5, chantier 6 (colonne
« trame »), chantier 7 (fil de fond annulable), chantier 8 (odomètre de
trame), chantiers 9+10 (recul du fond, structure L2), chantier 13
(construction release de mesure — le sixième relevé en vient),
chantiers 14-16 (le septième — né de deux plantages d'Entrée et de
Suppr, corrigés juste après), chantiers 17-19 (le huitième — le pire
de 108 ms y est né, corrigé au chantier 20 juste après), chantier 20
(le neuvième — la frappe y est enfin mesurée, et le pire de 25 ms
y trouve son correctif au chantier 21), chantiers 21-23 (le
dixième — **la borne du chantier 21 confirmée : pire 4,93** ; le
fichier hors normes y est mesuré : ouverture 11,97 ms, fond 329 ms),
chantiers 24-25 (le onzième — **le hors normes au nouvel APK : pire
13,77 sur la trame de fin de remplissage, sous le budget** ; la
feuille de recherche n'y a pas été ouverte, sa mesure viendra au
prochain relevé), chantier 26 (le douzième — **l'APK à lsp4j tient :
la frappe mesurée sur la démo Kotlin : 0,14 ms, 1 ligne relue — la
paresse de ligne tient aussi sur petit fichier** ; trames 1,01-7,35,
pire 10,26 — la feuille de recherche et le LSP n'y ont pas été
exercés, ni le hors normes), chantier 27 (le treizième — **l'APK aux
poignées vit : le pire du parcours, 10,10, reste l'ordre de l'APK
précédent sans poignées (10,26) — le geste de la copie ne paie rien
de plus** ; la frappe avant la copie : 0,19 ms, 1 ligne relue),
chantier 28 (le quatorzième — **le hors normes ENFIN exercé en
longueur : onze réouvertures, trames 1,08-4,47, thème sur le
document plein (pire 3,08), frappe 0,56 ms, 1 ligne relue sur
52 004 lignes — mais le pire du geste de copie, 31,48, dépasse le
budget : le chantier 29 ouvre le diagnostic**), chantiers 29-30
(le quinzième — **l'APK instrumenté parle : le pire du geste de
copie (39,29) porte moteur 0,13 et Δ mémoire −0,0 — ni le moteur,
ni un collecteur qui rend : le dessin reste en cause (une pause de
collecteur concurrent peut s'y cacher — le Δ ne voit que la
mémoire rendue) ; UNE TRAME DE 762 ms à la fin d'un remplissage de
742 ms (moteur 0,10, Δ +6,3 — un collecteur y a couru pendant que
le fil de fond tenait le processeur) ; les fonds ralentis à
436-1101 ms : sept réouvertures du MÊME texte y ont relancé le
fond — ~50 Mo de jetons à recycler par passe ; SQL enfin
exploré** ; le chantier 31 en vient : la réouverture du même texte
ne re-remplit plus, le fil de fond s'efface), chantiers 31-32 (le
seizième — **le chemin doux CONFIRMÉ sur appareil : quatre
réouvertures du même texte à 52 004/52 004 sans un seul
« remplissage fini » — deux fonds seulement (437 et 199 ms) pour
sept ouvertures, contre sept fonds au relevé 15 ; 199 ms : le
remplissage le plus rapide jamais relevé (la priorité minimale ne
retient pas l'appareil quand l'interface respire) ; le gros
fichier Java enfin exercé au passage (1 780 lignes, première
3,3 ms) ; la copie : pire 16,55 (moteur 0,07, Δ +0,0) — 0,55 ms au-dessus du
budget, une seule trame au début du geste** ; le
39,29 ne s'est pas revu (deux fonds contre sept : la pression est
tombée) ; **et le premier PLANTAGE signalé hors tableau : « tout
remplacer » sur le hors normes gèle puis ferme l'application (ANR)
— le chantier 33 le découpe au dépôt** ; la feuille de recherche a
donc bien été ouverte (c'est par elle que le parcours est passé) ;
le LSP reste non exercé, le modèle d'appareil absent), chantiers
33-37 (le dix-septième — **la consolidation MESURÉE : le « 50k ⨯ »
a ouvert le XML hors normes — 52 005 lignes, 375 573 jetons,
première 0,4 ms, fond 851 ms (203 tranches, trame de fin 7,33 —
le fond le plus lent relevé : la famille générique paie plus par
jeton que les lexeurs générés, mais la trame de fin tient) ;
LA COPIE GUÉRIE : ses trames 2,53 et 2,13 — le pire du scénario
n'est plus le geste (16,31 : le thème, 0,31 au-dessus du budget,
la fenêtre re-rastérisée en une trame — à surveiller) ; la
frappe XML : 0,54 puis 0,05 ms, 1 ligne relue — la paresse tient
sur la famille générique** ; le « tout remplacer » corrigé n'a
pas été re-essayé (aucune rangée ne le porte : le prochain relevé
le dira) ; le LSP reste non exercé, le modèle d'appareil absent ;
l'appareil n'est pas précisé
(modèle et version d'Android à compléter — la ligne du tableau copié
les demande).

| Mesure | Méthode | Valeur |
| --- | --- | --- |
| Première coloration, texte de démonstration | ouvrir l'application, lire le panneau | Java 0,5 · XML 0,2–0,5 · JSON 0,1 · Kotlin 0,2 · SQL 0,1 ms (première demande : la fenêtre visible seule, jamais le document) |
| Première coloration, gros fichier (bouton « Gros ») | appuyer, lire le panneau : seule la fenêtre visible se paie | **0,3 ms** (première demande, deux relevés) ; 1 066–1 322/1 780 lignes déjà lexées au premier tick du panneau (0,2 s) — le remplissage seul (aux relevés : pompe d'interface ; depuis le chantier 7, fil de fond), la couleur est déjà là |
| Frappe ordinaire (recoloration d'une ligne) | taper, lire « dernière frappe » | **0,26 ms, 1 ligne relue** (cinquième relevé, gros fichier — un caractère tapé avant la copie ; la trame de la frappe : 10,09 ms, moteur 0,21, pire 10,94 — le coût est au rendu, pas au moteur, et tient le budget 60 Hz) ; **0,20 ms en release** (sixième relevé, trame 2,43 ms, moteur 0,03 — voir la note sur le pire) ; au septième relevé, **pas de frappe possible : Entrée fermait l'application, Suppr ne faisait rien** — les deux touches étaient le bug (corrigé au chantier 17) ; au huitième, **aucune frappe relevée non plus** (aucun caractère tapé avant la copie) ; **au neuvième, ENFIN : 0,15 ms, 1 ligne relue, en release sur le gros fichier** — la paresse de la ligne tient (le pire de la trame, lui, vient de la marche d'appariement — voir la note) ; **au douzième, sur la démo Kotlin (petit fichier) : 0,14 ms, 1 ligne relue** — le même ordre sur 28 lignes que sur 52 004 : le coût de la frappe ne grandit pas avec le document ; **au treizième, même démo Kotlin sur l'APK aux poignées : 0,19 ms, 1 ligne relue** — l'ordre tient d'un APK à l'autre ; **au quatorzième, sur le fichier hors normes lui-même : 0,56 ms, 1 ligne relue** — la paresse tient du plus petit au plus grand document (0,14-0,19 démo, 0,15 gros, 0,56 hors normes : le coût croît un peu, le nombre de lignes relues jamais) ; **au quinzième, encore : 0,66 ms, 1 ligne relue, toujours sur le hors normes** — la paresse tient de relevé en relevé ; **au dix-septième, sur le hors normes XML : 0,54 puis 0,05 ms, 1 ligne relue — la paresse tient aussi sur la famille générique** |
| Frappe propagée (commentaire ouvert) | supprimer un `*/`, lire les lignes relues | à relever : idem, dans la démo Java (supprimer un `*/` du bloc) |
| Remplissage du gros fichier | lire « lignes lexées » monter après l'ouverture (aux deux premiers relevés : pompe par tranches sur le fil d'interface ; au troisième : fil de fond annulable, clichés révision-vérifiés) | **1 780/1 780 lignes, 18 095 jetons** ; au troisième relevé, complet **avant le premier tick (0,2 s)** — le fond remplit en ~150 ms ce que la pompe étalait sur ~1 s |
| Bascule de thème | basculer : la première coloration ne doit pas bouger | **première inchangée (0,3 ms), jetons identiques (18 095), deux relevés et trois bascules : zéro retokenisation constaté sur appareil** |
| Budget par image (tracé d'une trame) | lire « trame » au panneau ; la colonne « trame » du journal copié l'emporte | **première trame 16,0 ms** (démo Java : cuisson des plans + fenêtre) · **11,9 ms** à l'ouverture du gros fichier · **4,9–6,3 ms en régime établi** (recuisson au thème 5,1–6,3, copie 4,9) · troisième relevé (chantier 7) : 15,5–17,1 ms sur le gros fichier — voir la note · quatrième relevé (chantier 8, odomètre) : régime établi confirmé à 4,5–5,5 ms, moteur 0,09–0,23 ms ; ouverture du gros fichier **moteur 9,23 ms** (réouverture tiède : 4,95) — voir la note · cinquième relevé (chantiers 9+10) : **recul du fond confirmé — ouverture 13,51 ms (moteur 5,93), réouverture 8,49 (moteur 3,24)** ; recuisson **3,46 ms (moteur 0,06)** ; trame de frappe 10,09 (moteur 0,21) — voir la note · sixième relevé (chantier 13, **release**) : **ouverture 6,53 ms, moteur 1,65** — l'interprétation à froid tranchée ; frappe 2,43 (moteur 0,03), pire 24,06 — voir la note · septième relevé (chantiers 14-16 : portée, plis, L3 à la fenêtre) : **ouverture 4,36 ms, moteur 0,02 ; pire 12,18** — sous le budget, la marche d'appariement du relevé 6 ne s'est pas revue (pas de frappe : les touches étaient le bug) · huitième relevé (chantiers 17-19 : Entrée et Suppr réparés — aucune fermeture signalée) : **ouverture 2,68 ms, moteur 0,02, copie 8,43 (moteur 0,08) ; pire 108,14** — la première construction d'analyse (index de symboles + diagnostics, chantier 19) courait dans une trame et forçait le lexage du gros fichier entier : **corrigé au chantier 20** (l'analyse attend le remplissage du fond, puis se construit par tranches postées — jamais dans une trame) · neuvième relevé (chantier 20) : **frappe 0,15 ms, 1 ligne relue — ENFIN mesurée** (un caractère tapé dans le gros fichier avant la copie) ; ouverture 2,34 ms (moteur 0,02), copie 8,32 (moteur 0,01), **pire 25,02** — la première marche d'appariement (le relevé 6 l'avait annoncée) : **corrigé au chantier 21** (la borne de marche passe de 4 000 à 512 lignes) · dixième relevé (chantiers 21-23) : **pire 4,93** — la borne du chantier 21 confirmée sur l'appareil ; **le hors normes mesuré : 52 004 lignes, ouverture 11,97 ms (moteur 0,09), fond 329 ms (203 tranches, 624 002 jetons), trames 2,91-3,99** — le défilement du §4.2 tient sur le hors normes · onzième relevé (chantiers 24-25) : **le hors normes au nouvel APK : fond 367 ms, trame de fin de remplissage 13,77 ms (moteur 0,09) — pire du scénario, sous le budget** ; recuissons au thème 1,79-6,83, démes des langages 1,41-4,28, copie Kotlin 3,87 — la feuille de recherche n'y a pas été ouverte (la mesure viendra au prochain) · douzième relevé (chantier 26, l'APK à lsp4j) : **frappe 0,14 ms, 1 ligne relue — la paresse de ligne tient sur la démo Kotlin (petit fichier)** ; trames de démonstration 1,01-7,35 (moteur 0,01-0,25), pire 10,26 — sous le budget, aucun plantage avec lsp4j embarqué ; la recherche, le LSP et le hors normes n'y ont pas été exercés · treizième relevé (chantier 27, l'APK aux poignées) : **pire 10,10 pendant le scénario de copie — l'ordre exact de l'APK précédent sans poignées (10,26)** : le parcours de la copie (double-tap qui sème la sélection, traînement de poignée le cas échéant, copie) ne paie rien de plus ; trames de démonstration 1,29-1,73 (moteur 0,01), première coloration Java 1,1 ms (3/18 lignes — la fenêtre visible seule) · quatorzième relevé (chantier 28, l'APK à la loupe) : **le hors normes exercé en longueur — onze réouvertures alternées (52 004 lignes / gros fichier), trames 1,08-4,47, fond 345 ms (203 tranches), thème sur le document plein : pire 3,08 ; mais le pire du geste de copie sur le hors normes, 31,48, DÉPASSE le budget de 16** — moteur 0,01 à la dernière trame : le coût n'est pas au moteur, il est dans le geste (poignées + loupe pendant le traînement) — le chantier 29 ouvre le diagnostic · quinzième relevé (chantiers 29-30, **l'APK instrumenté — chaque pire porte son attribution**) : **le pire du geste de copie, 39,29, porte moteur 0,13 et Δ mémoire −0,0 : NI le moteur, NI un collecteur qui rend — le dessin reste en cause** (une pause de collecteur concurrent peut s'y cacher : le Δ ne voit que la mémoire rendue) ; **une trame de 762,11 ms** à la fin du remplissage de 742 ms (moteur 0,10, Δ +6,3 — la trame a été payée PENDANT que le fil de fond tenait le processeur : le chantier 31 met le fond à la priorité minimale) ; les autres fins de remplissage tiennent (6,56-9,94) ; les fonds ralentis à 436-1101 ms (contre 345 au quatorzième) : **sept réouvertures du MÊME texte ont relancé le fond à chaque fois — le chantier 31 y répond : la réouverture du même texte ne re-remplit plus** ; la frappe 0,66/1 ligne ; SQL exploré (11/11 lignes, 24 jetons) · seizième relevé (chantiers 31-32, **l'APK du chemin doux — le relevé le plus sain depuis le début**) : **quatre réouvertures du même texte à 52 004/52 004 sans un seul « remplissage fini » — deux fonds (437 et 199 ms — le plus rapide jamais relevé) pour sept ouvertures, contre sept au relevé 15** ; gros fichier Java enfin passé (première 3,3 ms) ; la copie : pire 16,55 (moteur 0,07, Δ +0,0 — 0,55 ms au-dessus du budget, une trame au début du geste ; le 39,29 ne s'est pas revu) ; **et le premier plantage du terrain, hors tableau : « tout remplacer » sur le hors normes → gel → ANR → force close (dix mille correspondances : le chantier 33 corrige la table de lignes copiée sur elle-même dix mille fois, l'annulation comprise)** · dix-septième relevé (chantiers 34-37, **la consolidation mesurée**) : **le XML hors normes — 52 005 lignes, 375 573 jetons, première 0,4 ms (38 lignes visibles), fond 851 ms (203 tranches), trame de fin 7,33 (moteur 0,07, Δ −0,1) — le fond le plus lent relevé (la famille générique paie plus par jeton que les lexeurs générés : 375 573 jetons y mettent 851 ms là où 624 002 jetons Java en prenaient 199-437 — mais la trame de fin tient) ; thème sur le document plein 16,31 (moteur 0,21) — 0,31 au-dessus du budget, à surveiller (le Java y paie 3,08) ; LA COPIE GUÉRIE : trames du geste 2,53 puis 2,13 — le pire du scénario n'est plus le geste (16,31 : le thème), le blit de la loupe du chantier 37 confirmé sur appareil** |

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

Le sixième relevé vient de l'APK **release** (chantier 13) — et il
tranche la question ouverte depuis le relevé 4. **Le poids de
l'interprétation à froid** : l'ouverture du gros fichier tombe à
**6,53 ms, moteur 1,65** — contre 13,51/5,93 puis 8,49/3,24 en
debug au même parcours : la construction release divise le moteur
d'ouverture par plus de trois. Ce qui restait après le chantier 9
était bien l'interprétation, pas le code — la promesse du chantier
13 tient, la mesure de référence est là. La frappe suit :
**0,20 ms, 1 ligne relue** (0,26 en debug), trame 2,43 ms, moteur
0,03. **Une trame à 24,06 ms (le pire du scénario) dépasse le
budget** : le suspect désigné est la première marche
d'appariement — la frappe déplace le curseur, la paire se
recalcule, la marche suit les jetons jusqu'au partenaire (~1 780
lignes au pire) sur un chemin jamais encore exécuté, donc
interprété — le même phénomène que l'ouverture, payé une fois par
position de curseur, puis le régime tient (2,43). Si le prochain
relevé le porte encore, la marche passera au rebut borné (la borne
de 4 000 lignes existe, il suffira de la baisser). La ligne
« thème Sombre » porte la trame d'ouverture (6,53/1,65) — l'artefact
documenté au relevé 5, pas une recuisson. Et le document porte la
frappe : 1 781/1 781 lignes, 18 103 jetons.

Le septième relevé est né des deux plantages qu'il rapporte :
**Entrée fermait parfois l'application, Suppr ne faisait rien** —
reproduits au harnais, corrigés au chantier 17. L'Entrée : après un
changement de langage, les documents ré-ouverts s'enregistraient en
FIN de chaîne d'écoute — le panneau de diagnostic lisait des jetons
périmés au beau milieu du remplacement, l'annotateur découpait hors
bornes (le « parfois » : seulement après un changement de langage,
seulement si la lecture tombait sur des jetons dépassant le nouveau
texte). Le correctif est structurel : les documents s'enregistrent
EN TÊTE (`addListenerFront` dans :core) — l'ordre
document-avant-interface tient quoi qu'il arrive. Le Suppr : les
claviers qui passent par les événements tombaient dans le mode
factice de la connexion — la connexion traduit maintenant Suppr,
Suppr avant, Entrée et les flèches elle-même. Le relevé lui-même :
l'ouverture du gros fichier **4,36 ms, moteur 0,02**, remplissage
complet (18 095 jetons), **pire 12,18 ms — sous le budget** ; et la
marche d'appariement du relevé 6 ne s'est pas revue (aucune frappe :
les touches étaient le bug). À relever à nouveau sur l'APK corrigé —
la frappe, la frappe propagée, un pli par la gouttière.

Le huitième relevé confirme la réparation — **aucune fermeture
signalée** — et apporte un nouveau pire : **108,14 ms**. Diagnostic :
la première construction de l'analyse (l'index de symboles et les
diagnostics du chantier 19) courait DANS la trame qui suit
l'ouverture du gros fichier, et son « force le document » y lexait
les ~1 740 lignes restantes d'un coup — le garde-fou des 300 ms du
chantier 19 ne couvrait que les reconstructions, jamais la première.
Correctif au chantier 20 : l'analyse **attend que le fond ait rempli
le document**, puis se construit **par tranches postées** (256 lignes
par post, jamais dans une trame) ; le panneau cadence le départ au
tick qui constate la fin du remplissage, et une édition pendant la
passe l'abandonne (le ralenti reprendra — l'ancien index reste une
indication de noms). Le reste du relevé tient : ouverture **2,68 ms
(moteur 0,02)**, copie 8,43 (moteur 0,08), remplissage complet, et la
première coloration de la démo ne paie que **3 lignes/18** — la
paresse de la fenêtre. La frappe reste à relever : aucune ligne relue
au huitième — **taper un caractère dans le gros fichier avant de
copier** et la ligne viendra.

Le neuvième relevé l'a tapée, ce caractère : **frappe 0,15 ms, 1
ligne relue** — la mesure manquante depuis le relevé 5 est là, la
paresse de la ligne tient sur l'appareil (l'ouverture 2,34/0,02 et
la copie 8,32/0,01 confirment le régime du chantier 20). Reste le
**pire 25,02 ms** : la première marche d'appariement, exactement ce
que le relevé 6 avait annoncé — le curseur déplacé, la paire
recalculée, la marche lit le texte de CHAQUE ligne traversée sur un
chemin jamais exécuté (interprété à froid : ~15 ms sur 1 780 lignes,
par-dessus les ~10 ms de rendu de la frappe). La correction était
promise dès le relevé 6 : **chantier 21, la borne de marche passe de
4 000 à 512 lignes** — la première marche tient le budget, les paires
qui importent (un corps de méthode, un bloc) restent couvertes, la
bande de portée continue de montrer l'étendue du bloc englobant. La
mesure au prochain relevé dira si le pire descend sous 17.

Le dixième relevé (chantiers 21-23) y répond : **pire 4,93 ms** —
la borne du chantier 21 est confirmée sur l'appareil (le 25,02 du
relevé 9 ne se voit plus, le pire de tout le scénario reste sous
5 ms). Et le relevé ouvre le hors normes du chantier 23 : **52 004
lignes, ouverture 11,97 ms (moteur 0,09)** — la fenêtre seule se
paie, sous le budget de première peinture ; **le fond remplit en
329 ms (203 tranches, 624 002 jetons, 52 004/52 004 lignes)** —
le compte du harnais JVM se retrouve à l'identique sur l'appareil ;
la trame de fin de remplissage 3,99 ms, la copie 2,91, pire 4,93 :
le défilement du §4.2 tient le budget sur le hors normes. Le régime
ordinaire ne bouge pas (démo 0,2 ms de première, gros fichier
1,39 ms de première trame au même parcours).

Le onzième relevé (chantiers 24-25, le nouvel APK) revient sur le
hors normes avec les chantiers nouveaux à bord : **fond 367 ms
(203 tranches, même compte), trame de fin de remplissage 13,77 ms
(moteur 0,09) — pire du scénario, sous le budget** ; l'ouverture
y descend à 2,37 ms (la fenêtre seule, tiède). Les recuissons au
thème sur le hors normes tiennent (1,79-6,83 ms, moteur 0,03-0,15)
— la recherche du chantier 24 ne s'y est pas vue : la feuille
n'a pas été ouverte dans ce parcours, la mesure viendra au
prochain. Les démos des langages se peignent (Kotlin 1,41-1,65,
Java 4,28, JSON 2,34 ms), la copie Kotlin 3,87 (pire 8,43). Aucune
frappe dans le gros fichier cette fois (aucune ligne relue) ; le
modèle d'appareil reste à compléter. Le 13,77 de la fin de
remplissage reste sous le budget — c'est la trame qui dessine
l'état arrivé du fond (624 002 jetons), le dixième la mesurait à
3,99 : la variance de l'appareil, pas une marche du code.

Le douzième relevé (chantier 26, l'APK à lsp4j embarqué) fait
vivre les démos seules — et c'est la mesure qui manquait depuis le
neuvième : **la frappe sur un PETIT fichier, 0,14 ms, 1 ligne
relue** (un caractère tapé dans la démo Kotlin avant la copie, 308
jetons après le coup) — la paresse de la ligne tient au même ordre
sur 28 lignes que sur 52 004 (le neuvième mesurait 0,15 sur le
gros fichier) : le coût de la frappe ne grandit pas avec le
document. Les trames des démes se peignent sous le budget (Java
1,01, Kotlin 7,00-7,35 ms, moteurs 0,01-0,25), pire du parcours
10,26 — l'application vit avec lsp4j et gson dans l'APK (481 Ko
debug), aucune fermeture. La feuille de recherche, le bouton LSP
et le hors normes n'ont pas été exercés dans ce parcours : leurs
mesures attendent le relevé 13, avec le modèle d'appareil.

Le treizième relevé (chantier 27, l'APK aux poignées de sélection)
est court — trois lignes, le parcours minimal (démo Java, démo
Kotlin, un caractère tapé puis la copie). **Ce qu'il dit : les
poignées ne coûtent rien au geste que le propriétaire éprouve** —
le pire du scénario de copie tombe à 10,10 ms, l'ordre exact de
l'APK précédent qui n'avait pas de poignées (10,26 au douzième) :
le double-tap qui sème la sélection et le traînement de la poignée
— si le doigt l'a traînée, le tableau copié ne le distingue pas —
vivent dans la même enveloppe de budget. La frappe avant la copie :
**0,19 ms, 1 ligne relue** sur la démo Kotlin — la paresse de ligne
tient une troisième fois (0,15 gros fichier au neuvième, 0,14 et
0,19 sur la démo), toujours le même ordre de grandeur. Les démos se
peignent seules (Java : première coloration 1,1 ms, 3/18 lignes
lexées — la fenêtre visible paie, jamais le document ; Kotlin :
0,2 ms, trames 1,29-1,73, moteurs 0,01) et l'application vit (489
Ko debug, aucune fermeture signalée). Ce que le relevé ne porte
toujours pas, une troisième fois : la feuille de recherche, le
bouton LSP, le hors normes, le saut lointain — et le modèle
d'appareil reste « à compléter » dans le tableau copié. Le chantier
28 (la loupe de précision, le compagnon du traînement de poignée)
n'est pas dans ce relevé : le scénario de copie l'exercera au
prochain.

Le quatorzième relevé (chantier 28, l'APK à la loupe de précision)
vit sur le hors normes — onze réouvertures alternées du fichier de
52 004 lignes et du gros fichier, le thème basculé sur le document
plein, un caractère tapé, puis le geste de copie complet (frappe,
double-tap, traînement de poignée — la loupe y vit —, copie). **Ce
qu'il dit de bon : le hors normes enfin mesuré en longueur.** La
première ouverture paie la fenêtre (40/52 004 lignes, première
coloration 0,1 ms) pendant que le fond remplit (345 ms, 203 tranches,
624 002 jetons — l'ordre exact des relevés 10 et 11) ; les
réouvertures tiennent (trames 1,08-4,47, la fenêtre visible seule à
chaque première coloration) ; le thème sur le document plein ne
coûte pas plus (pire 3,08 — la recuisson reste celle de la fenêtre) ;
et **la frappe sur le plus gros fichier enfin mesurée : 0,56 ms,
1 ligne relue** — la paresse de la ligne tient du plus petit au plus
grand document. **Ce qu'il dit de moins bon : le pire du geste de
copie, 31,48 ms, dépasse le budget de 16** — une seule trame, le
moteur n'y est pour rien (0,01 ms à la dernière) ; le geste y
concatène le traînement de poignée et la loupe qui l'accompagne,
sur un document de 624 002 jetons : le chantier 29 ouvre le
diagnostic (reproduction au harnais, lecture du chemin de trame).
**Le chantier 29 a livré l'instrument et l'exonération partielle :
le harnais rejoue le geste complet sur le hors normes (pire du
traînement 1,86 ms sur trente trames, moyenne 0,13 — le chemin
algorithmique est propre), et l'odomètre porte désormais
l'attribution de la pire trame (sa part moteur et la mémoire rendue
pendant la trame) — la colonne « trame » du relevé 15 dira si le
coupable est le ramasse-miettes (un Δ mémoire positif), le moteur,
ou la rastérisation (ni l'un ni l'autre).**
La feuille de recherche, le bouton LSP et le modèle d'appareil
restent non portés une quatrième fois.

Le quinzième relevé (chantiers 29-30, l'APK instrumenté) est le
premier où **chaque pire porte son attribution** — et l'instrument
parle. **Le pire du geste de copie, 39,29 ms, porte moteur 0,13 et
Δ mémoire −0,0** : ni le moteur, ni un collecteur qui rend pendant
la trame — le dessin reste en cause (la loupe y dessine du texte à
l'échelle 2, des glyphes neufs à rastériser ; une pause de
collecteur concurrent peut aussi s'y cacher : le Δ ne voit que la
mémoire rendue, la pause d'un collecteur concurrent précède son
balayage). **Une trame de 762,11 ms, elle, s'est payée pendant le
remplissage de 742 ms** (moteur 0,10, Δ +6,3 — un collecteur y a
couru) : la trame a été dessinée pendant que le fil de fond tenait
le processeur — les quatre autres fins de remplissage tiennent
(6,56-9,94). **Le troisième fait : les fonds ont ralenti**
(436-1101 ms, contre 345 au quatorzième) — le parcours a réouvert
SEPT FOIS le même texte vierge, relançant à chaque fois le lexage
des 624 002 jetons (~50 Mo à recycler par passe) : la pression du
collecteur a tout ralenti. Le reste tient : la frappe 0,66 ms,
1 ligne relue ; les réouvertures 5,58-13,59 ; le SQL enfin exploré
(11/11 lignes, 24 jetons — le panneau des langages s'y éprouve).
**le chantier 31 y répond : la réouverture du même texte ne
re-remplit plus (le document est déjà exact — seuls l'état de la
vue et l'historique repartent), et le fil de fond passe à la
priorité minimale (le fil d'interface garde son processeur)**. La
recherche, le LSP et le modèle d'appareil restent non portés une
cinquième fois.

Le seizième relevé (chantiers 31-32) **confirme le chemin doux sur
l'appareil** : quatre réouvertures du même texte hors normes s'y
lisent à 52 004/52 004 lignes lexées sans un seul « remplissage
fini » — deux fonds seulement (437 et 199 ms) pour sept ouvertures,
contre sept fonds au relevé 15 ; **199 ms : le remplissage le plus
rapide jamais relevé** (la priorité minimale du chantier 31 ne
retient pas l'appareil quand l'interface respire). Le gros fichier
Java est enfin passé (1 780 lignes, première coloration 3,3 ms) ;
la copie paie 16,55 au pire (moteur 0,07, Δ +0,0 — 0,55 ms
au-dessus du budget, une trame au début du geste : le 39,29 du
relevé 15 ne s'est pas revu, la pression des fonds est tombée).
**Et le premier plantage du terrain arrive hors tableau : « tout
remplacer » sur le hors normes gèle l'application puis la ferme
(ANR)** — la feuille de recherche a donc bien été ouverte (c'est
par elle que le parcours est passé ; le journal ne la porte pas).
Le diagnostic au dépôt : dix mille correspondances remplacées dans
une transaction — et la table de lignes du tampon copiée sur
elle-même à CHAQUE remplacement (un demi-million d'opérations de
suite). Le chantier 33 corrige, l'annulation comprise. Aucune
frappe relevée (aucun caractère tapé avant la copie) ; le LSP
reste non exercé, le modèle d'appareil toujours absent.

Le dix-septième relevé (chantiers 34-37) **porte la consolidation
mesurée et la copie guérie** : le « 50k ⨯ » y a ouvert le XML hors
normes — 52 005 lignes, 375 573 jetons, première coloration 0,4 ms
(38 lignes visibles), fond 851 ms en 203 tranches, trame de fin
7,33 ms (moteur 0,07, Δ −0,1). Le fond XML est le plus lent relevé
(la famille générique paie plus par jeton que les lexeurs générés :
375 573 jetons pour 851 ms, là où 624 002 jetons Java en
demandaient 199-437) — la ligne de lexeur, pas la mémoire (Δ nul),
et la trame de fin tient le budget. **La frappe y relit UNE ligne**
(0,54 puis 0,05 ms) : la paresse de ligne tient aussi sur la
famille générique. Le thème basculé sur le document plein paie
16,31 ms (moteur 0,21) — 0,31 au-dessus du budget, la fenêtre
re-rastérisée en une trame ; à surveiller au prochain relevé (le
Java y paie 3,08). **Et le geste de copie est guéri : ses trames
sont à 2,53 et 2,13 ms — le pire du scénario n'est plus le sien
(16,31 : le thème, avant les copies)** : le blit de la loupe du
chantier 37 est confirmé sur l'appareil, le 31,48 et le 39,29 ne
se sont pas revus. Le « tout remplacer » corrigé (chantier 33)
n'a pas été re-essayé — aucune rangée ne le porte, le prochain
relevé le dira ; le LSP reste non exercé, le modèle d'appareil
toujours absent (le prochain APK porte le serveur EmmyLua réel :
le LSP y aura sa rangée).

Les plafonds du plan sont codés (E3) : ligne de plus de 20 000 colonnes
non colorée (état reporté, voisines intactes), dessin limité à 10 000
colonnes (vue), mode gros fichier au-delà de 2,5 M caractères ou
50 000 lignes (`inBigFileMode()` : la coloration L1 n'y est jamais
coupée, L2 et L3 s'y couperont). Les dixième et onzième relevés les
mettent à l'épreuve sans les contredire — la coupe de L2/L3 en mode
gros fichier (le plan E3) n'a pas eu à s'activer : la structure et
l'analyse ont tenu seules.
