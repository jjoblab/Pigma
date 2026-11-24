# Mesures de performance du moteur de coloration

Ce document reçoit les mesures du plan : d'abord celles que la machine
de build sait prendre (JVM, tests du module `:syntax`), puis celles de
l'appareil, relevées avec le **panneau de diagnostic du sample** (le
texte sous l'éditeur affiche première coloration, coût de la dernière
frappe, lignes relues, jetons). Les nombres JVM servent de plancher et
de garde-fou ; seuls les nombres appareil décident des plafonds.

## Machine de build (JVM x86-64, JDK 17)

Moteur complet (`SyntaxDocument`, lexeur Java généré), corpus Java de
2 010 lignes / 60 154 caractères, test `EngineMeasureTest` du module
`syntax` :

| Mesure | Valeur |
| --- | --- |
| Premier lexage complet (paresseux à la première demande) | 11,8 ms |
| Jetons produits | 26 480 |
| Frappe ordinaire (une ligne relue, règle d'arrêt immédiate) | 0,03 ms |
| Frappe propagée (fermeture de commentaire retirée, 7 lignes relues) | 0,03 ms |

Lexeurs seuls (banc E0, `bench/`, mêmes corpus, passes rechauffées) :
1,7 à 3,5 ms pour ~2 000 lignes, 14 à 47 Mo/s selon le langage ; à JVM
froide, 3 à 16 ms. Poids DEX par langage (d8, min-api 26) : 18 788
octets (Java), 7 804 (XML), 7 420 (JSON) — l'APK de démonstration
complet (moteur, trois lexeurs générés, famille générique, interface
du sample) tient en 67 Ko.

Changer de thème ne retokenise rien : les jetons portent des portées,
la couleur se résout au dessin (le panneau le montre : la mesure de
première coloration ne bouge pas quand on bascule clair/sombre).

## Appareil (à relever par le propriétaire)

Le bac à sable n'a pas d'appareil : les lignes suivantes se remplissent
depuis le panneau de diagnostic (`adb install
sample/build/outputs/apk/debug/sample-debug.apk`, ouvrir l'application,
taper, lire le panneau) — chaque langue et chaque thème y passent.

| Mesure | Méthode | Valeur |
| --- | --- | --- |
| Première coloration, texte de démonstration | ouvrir l'application, lire le panneau | à relever |
| Frappe ordinaire (recoloration d'une ligne) | taper, lire « dernière frappe » | à relever |
| Frappe propagée (commentaire ouvert) | supprimer un `*/`, lire les lignes relues | à relever |
| Bascule de thème | basculer : la première coloration ne doit pas bouger | à relever |
| Budget par image (tracé d'une trame) | à instrumenter avec la vue complète | à relever |

Les plafonds du plan (ligne de 20 000 colonnes non colorée, dessin
limité à 10 000, mode gros fichier au-delà de 2,5 M caractères ou
50 000 lignes) s'ajusteront sur ces relevés — ils ne sont pas encore
codés (étape E3 et suivantes).
