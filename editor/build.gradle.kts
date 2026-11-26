plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "jo.pigma.editor"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        // Les règles consommateur : un hôte qui minifie sa release
        // garde le client LSP vivant (lsp4j et Gson chargent leurs
        // protocoles par réflexion — le rapport d'analyse l'a demandé,
        // le fichier vit au module)
        consumerProguardFiles("consumer-rules.pro")
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
    // Le client du serveur de langage y vit aussi : la façade
    // editor/lsp/LspEditor câble le serveur à la vue à la place de
    // l'hôte (l'étude de l'éditeur de référence — son module
    // d'intégration attache pareillement le serveur à la vue). Le
    // client demeure un module
    // à part dans les sources ; l'hôte qui n'en veut pas ne branche
    // jamais la façade, lsp4j dort.
    api(project(":lsp"))
    testImplementation(libs.junit)
}
