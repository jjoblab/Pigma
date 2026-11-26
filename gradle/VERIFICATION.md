# La chaîne d'approvisionnement (D11)

`gradle/verification-metadata.xml` épingle les sommes SHA-256 et
SHA-512 des 401 artefacts de la construction (le catalogue des
versions reste l'unique source des numéros de version — le
métadonné les gèle une fois résolus).

## La vérification stricte

Le mode se passe en ligne de commande (jamais dans un fichier
commité qui briderait les contributeurs sans trousseau) :

    ./gradlew check --dependency-verification=strict

Toute dépendance dont la somme manque (nouvelle version, nouvel
artefact) échoue la construction : la mise à jour du catalogue
s'accompagne de la régénération.

## La régénération

Après un changement de version dans `gradle/libs.versions.toml` :

    ./gradlew --write-verification-metadata sha256,sha512 check

La régénération lit les sommes des artefacts TÉLÉCHARGÉS (elle fait
confiance au canal du moment) : le diff se relit AVANT le commit —
une somme qui change sans changement de version est une alerte.

## Les signatures

`verify-signatures` vit à `false` (les sommes suffisent à
l'intégrité ; la vérification des signatures exigerait un trousseau
PGP et un réseau de clés au poste). L'hôte qui la veut :

    ./gradlew --write-verification-metadata sha256,sha512 --export-keys check

pose `gradle/verification-keyring.gpg` et le bloc de configuration
passe `verify-signatures` à `true`.
