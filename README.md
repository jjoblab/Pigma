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
de politique de langue et de commentaires (`scripts/check-code-policy`) et
le contrôle des références interdites (`scripts/check-forbidden-refs`).

## Crochet pre-commit

Après le clonage, installer le crochet pour que chaque commit passe les
contrôles de politique :

    cp scripts/hooks/pre-commit .git/hooks/pre-commit
