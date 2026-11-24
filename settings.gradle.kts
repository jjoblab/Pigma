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
// Le chantier en cours n'exige que le sample : les autres arriveront chacun
// avec la fonctionnalité qui les justifie.
include(":sample")
