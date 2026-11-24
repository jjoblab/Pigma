# Pigma

Bibliothèque d'éditeur de code embarquable pour Android, en Java 17.

La documentation de conception vit dans `docs/` : architecture cible,
étude du moteur de coloration, registre de portage, décisions et
démonstrations par étape de travail (`docs/chantiers/`).

## Compiler

Prérequis : JDK 17 ou plus récent, SDK Android (fichier `local.properties`
à la racine avec `sdk.dir=...`, ou variable `ANDROID_HOME`), Python 3 pour
les contrôles de politique.

    ./gradlew :sample:assembleDebug

L'APK de démonstration est produit dans `sample/build/outputs/apk/debug/`.

## Lancer le sample

Installer l'APK sur un appareil ou un émulateur :

    adb install sample/build/outputs/apk/debug/sample-debug.apk

## Tester et vérifier

    ./gradlew test      # tests unitaires de tous les modules
    ./gradlew check     # tests + style + politique de langue + références interdites

`check` exécute les tests, Checkstyle (configuration minimale), le contrôle
de politique de langue et de commentaires (`scripts/check-code-policy`), le
contrôle des références interdites (`scripts/check-forbidden-refs`) et le
contrôle d'à-jour des lexeurs générés (`scripts/check-lexers-fresh`).

## Harnais d'ouverture

Le harnais JVM (`scripts/open-harness/`) éprouve l'application de
démonstration sans appareil : il compile le dépôt contre des stubs
Android minimaux (horloge virtuelle pour les différés, canevas qui
enregistre les dessins au lieu de peindre) et déroule les scénarios —
ouverture, remplissage du fond, langages, thème, frappe et clavier
par événements, sélection et poignées, appui long, recherche et
remplacement, fichier hors normes, plis, fusion sémantique, serveur
de langage, copie des mesures :

    bash scripts/open-harness/run.sh

Prérequis : un JDK et les jars du client LSP dans un cache Gradle
(`GRADLE_USER_HOME`, `~/.gradle` ou `$TOOLS/gradle-home` — un
`./gradlew check` préalable les y dépose). Le code de sortie est non
nul si un scénario échoue ; la décision 007 consigne pourquoi le
harnais vit dans le dépôt.

## Lexeurs générés

Les descriptions de lexeurs vivent dans `syntax/src/main/lexers/`
(commentaires en français) ; le **code généré est versionné** dans
`syntax/src/main/java/jo/pigma/syntax/lexers/generated/` (décision 005 :
le build sur téléphone ne télécharge aucun outil). Retoucher une
description exige de régénérer :

    java -cp jflex.jar:java-cup-runtime-11b-20160615-3.jar jflex.Main \
        --nobak -d syntax/src/main/java/jo/pigma/syntax/lexers/generated \
        syntax/src/main/lexers/<langage>.flex

`check` compare ensuite code généré et descriptions **si JFlex est
trouvable** (variable `JFLEX_CP`, ou `TOOLS` pointant un répertoire avec
`jflex.jar` et son jar cup) ; sans lui, le contrôle s'ignore en le
disant — les machines d'outillage le gardent actif.

## Banc d'essai

Les bancs (faisabilité de la génération de lexeurs, profil du dépôt de
référence) vivent dans `bench/`, hors du build courant : voir
`bench/README.md`.

## Crochet pre-commit

Après le clonage, installer le crochet pour que chaque commit passe les
contrôles de politique :

    cp scripts/hooks/pre-commit .git/hooks/pre-commit
