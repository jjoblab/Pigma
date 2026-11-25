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
        ndk {
            // l'APK ne porte que arm64-v8a : le serveur natif EmmyLua y
            // descend seul (x86_64 ne servait qu'à l'émulateur — la
            // demande est arm64, le poids des .so s'en trouve réduit)
            abiFilters += "arm64-v8a"
        }
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

    packaging {
        jniLibs {
            // le serveur natif EmmyLua est un EXÉCUTABLE : il doit
            // vivre dans nativeLibraryDir, seul répertoire exécutable
            // d'une application — sans l'extraction à l'ancienne, le
            // .so reste compressé dans l'APK, canExecute() est faux
            // et le bouton « LSP Lua » annonce le serveur manquant
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // l'hôte ne déclare QUE la bibliothèque d'édition : elle expose en
    // api le cœur, le moteur, l'annotateur ET le client LSP (la façade
    // LspEditor câble le serveur à la vue à la place de l'hôte — le
    // cas courant de l'architecture : une seule dépendance, tout vit)
    implementation(project(":editor"))
    // les vrais conteneurs de l'écran : TabLayout (les onglets),
    // DrawerLayout (le menu), CoordinatorLayout et BottomSheetBehavior
    // (la feuille basse) — aucune géométrie maison, le cadre porte
    // la mesure et la disposition.
    implementation(libs.material)
}
