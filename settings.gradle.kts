// Dépôts de plugins et de dépendances : déclarés ici, une seule fois pour tout le dépôt.

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "pigma"

// Un module par fonctionnalité ; aucun module vide n'est créé pour l'avenir.
// Chaque module arrive avec la fonctionnalité qui le justifie : le cœur du
// modèle de texte (:core), le moteur de coloration (:syntax), l'annotateur
// sémantique (:analysis), la bibliothèque d'édition (:editor — la vue et la
// façade du serveur de langage, l'hôte n'a qu'elle à déclarer : elle expose
// le reste en api), le client de serveur de langage (:lsp, le client seul
// — l'hôte qui câble sans façade y va) et l'application de démonstration
// (:sample).
include(":core")
include(":syntax")
include(":analysis")
include(":editor")
include(":lsp")
include(":sample")
