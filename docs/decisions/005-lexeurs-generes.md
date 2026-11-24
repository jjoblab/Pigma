# Décision 005 — les lexeurs générés : JFlex, code versionné

**Date** : 24 novembre 2026. **Statut** : acceptée (recommandations de
l'étude de faisabilité validées par le propriétaire).

## Contexte

Le chantier 2 (banc E0) a mesuré la voie des lexeurs générés : la
génération d'un lexeur coûte moins d'une demi-seconde et 75 Mo sur JVM
(JFlex 1.9.1), le code généré est autonome (aucune dépendance au
moment de l'exécution), léger (7 à 19 Ko de DEX par langage) et rapide
(1,7 à 3,5 ms pour 2 000 lignes rechauffées). Le profil du dépôt de
référence a confirmé que le coût des grammaires interprétées
(analyse JSON plus compilation des motifs : environ une seconde sur
JVM x86 rapide, plusieurs secondes sur téléphone) est bien ce qui
pesait sur le démarrage. Quatre décisions restaient au propriétaire.

## Décision

1. **Générateur : JFlex existant** (BSD-3), outil de compilation
   uniquement — le jar n'entre jamais dans l'application. L'alternative
   (générateur écrit par nous) ne se justifie que si JFlex devient un
   bloc.
2. **Le code généré est versionné** avec sa description : le build sur
   téléphone ne télécharge ni JFlex, ni Java supplémentaire. Les
   descriptions vivent dans `syntax/src/main/lexers/` (commentaires en
   français) ; le code généré dans
   `syntax/src/main/java/jo/pigma/syntax/lexers/generated/`, hors des
   contrôles de langue (le mot `generated` est exclu par le contrôle).
   Un contrôle d'à-jour (`scripts/check-lexers-fresh`, tâche
   `checkLexersFresh` de `./gradlew check`) régénère chaque description
   et compare : une description retouchée sans régénération échoue.
3. **Langages de départ : Java, Kotlin, XML, JSON** par la voie générée
   (Kotlin suivra Java), plus la **famille générique paramétrable**
   (`LexerProfile`) pour la longue traîne — le sample la montre avec
   SQL.
4. **Reprise des descriptions de la communauté IntelliJ** : par défaut
   réécrire ; la reprise fichier par fichier reste permise
   (`intellij-community` est sous Apache 2.0) avec attribution au
   `NOTICE`, au cas par cas.

## Conséquences

- `./gradlew check` vérifie l'à-jour des lexeurs **si JFlex est
  présent** (variable `JFLEX_CP` ou `TOOLS`) et s'ignore sinon, en le
  disant : le build sur téléphone reste vert, au prix d'un contrôle
  actif seulement sur les machines d'outillage — documenté au README.
- Le jour d'une publication, une ligne d'attribution JFlex au `NOTICE`
  restera la prudence élémentaire (le code généré ne porte aucun
  en-tête de licence).
- Un lexeur livré apporte ses **sorties figées** (relues à la main puis
  committées) et son **test de redémarrage** : c'est le filet de
  sécurité commun à toute la voie générée.
