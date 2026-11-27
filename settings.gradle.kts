// Dépôts de plugins et de dépendances : déclarés ici, une seule fois pour tout le dépôt.
// Le miroir Google de Maven Central précède mavenCentral() : repo.maven.apache.org
// peut refuser (429) alors que le miroir sert les mêmes artefacts ; Gradle y
// trouve la même somme de contrôle, l'ordre ne change pas les versions résolues.

pluginManagement {
    repositories {
        google()
        maven(url = "https://maven-central.storage-download.googleapis.com/maven2")
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        maven(url = "https://maven-central.storage-download.googleapis.com/maven2")
        mavenCentral()
    }
}

rootProject.name = "pigma"

// Un module par fonctionnalité ; aucun module vide n'est créé pour l'avenir.
// Chaque module arrive avec la fonctionnalité qui le justifie : le cœur du
// modèle de texte (:core), le moteur de coloration (:syntax), l'annotateur
// sémantique (:analysis), la couche d'intégration de l'IA (:ai — les types
// et les interfaces que l'HÔTE implémente : Pigma n'y connaît aucune
// intelligence artificielle, ni fournisseur, ni modèle, ni réseau), la
// bibliothèque d'édition (:editor — la vue et la façade du serveur de
// langage, l'hôte n'a qu'elle à déclarer : elle expose le reste en api),
// le client de serveur de langage (:lsp, le client seul — l'hôte qui
// câble sans façade y va) et l'application de démonstration (:sample).
include(":core")
include(":syntax")
include(":analysis")
include(":ai")
include(":editor")
include(":lsp")
include(":sample")
