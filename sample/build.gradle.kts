plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "jo.pigma.sample"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "jo.pigma.sample"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        // la construction release de mesure : signée debug (installable
        // par adb pour les relevés), sans minification — la seule
        // variable qui change face au debug est le mode de compilation,
        // la mesure y répond (l'interprétation à froid des relevés)
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
    }
}

dependencies {
    // l'hôte ne déclare que la bibliothèque d'édition : elle expose en
    // api le cœur, le moteur et l'annotateur (le cas courant de
    // l'architecture — une seule dépendance)
    implementation(project(":editor"))
    // le client de serveur de langage est OPTIONNEL : l'hôte qui le
    // veut l'ajoute, celui qui n'en veut pas ne le voit jamais
    implementation(project(":lsp"))
}
