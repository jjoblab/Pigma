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
    // Le client du serveur de langage y vit aussi : la façade
    // editor/lsp/LspEditor câble le serveur à la vue à la place de
    // l'hôte (l'étude de Sora editor — son module editor-lsp attache
    // pareillement le serveur à la vue). Le client demeure un module
    // à part dans les sources ; l'hôte qui n'en veut pas ne branche
    // jamais la façade, lsp4j dort.
    api(project(":lsp"))
    testImplementation(libs.junit)
}
