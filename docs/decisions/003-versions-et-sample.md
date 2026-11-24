# Décision 003 — versions de départ et sample sans AndroidX

## Contexte

Le build doit rester léger et prévisible : le développement se fait sur
téléphone (Termux / AndroidIDE). Aucune porte lourde dans le build
courant ; le sample doit assembler un APK avec une seule commande.

## Choix

- **AGP 8.7.3**, **Gradle 8.10.2** (distribution épinglée par somme de
  contrôle SHA-256 dans `gradle/wrapper/gradle-wrapper.properties`),
  **JDK 17 ou plus récent** pour lancer Gradle, code produit en Java 17 ;
  `compileSdk` 35, `minSdk` 26, `build-tools` 34.0.0 ;
- toutes les versions dans `gradle/libs.versions.toml`, aucune ailleurs ;
- le sample n'utilise **aucune dépendance AndroidX** : `Activity` du
  cadre de travail, thème Material du système (`Theme.Material.Light`),
  interface construite en Java. L'APK reste minimal et la chaîne de build
  n'a que `google()` et `mavenCentral()` comme dépôts.

## Conséquences

- l'APK du sample pèse quelques dizaines de kilo-octets et s'assemble
  sans traitement de ressources ;
- le jour où une fonctionnalité exigera AndroidX (accessibilité riche,
  fragments), une décision explicite l'inscrira ici ;
- le verrou de somme de contrôle interdit tout changement silencieux de
  distribution Gradle : changer de version suppose de recalculer et
  consigner la somme.
