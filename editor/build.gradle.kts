plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "jo.pigma.editor"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // La bibliothèque d'édition expose tout ce qu'un hôte touche :
    // l'application hôte ne déclare QU'ELLE (le cas courant de
    // l'architecture — une seule dépendance)
    api(project(":core"))
    api(project(":syntax"))
    api(project(":analysis"))
    testImplementation(libs.junit)
}
