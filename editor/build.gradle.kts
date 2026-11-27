plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
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

    // Les tests JVM des cartes du paquet popups : les pinceaux s'y
    // construisent (le style s'y pose au berceau) — leurs méthodes
    // hors logique rendent les valeurs par défaut au lieu de lever
    // (la logique des cartes — la pose, les listes, les sélecteurs —
    // se prouve SANS la mesure, qui vit au harnais réel du sample).
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    // La publication Maven du variant release (AGP ne l'expose que
    // s'il y est dit de publier) : le composant « release » naît de
    // cette déclaration, la publication s'y attache dessous.
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // La bibliothèque d'édition expose tout ce qu'un hôte touche :
    // l'application hôte ne déclare QU'ELLE (le cas courant de
    // l'architecture — une seule dépendance)
    api(project(":core"))
    api(project(":syntax"))
    api(project(":analysis"))
    // La couche d'intégration de l'IA y vit aussi : la façade
    // editor/ai/AiAssistant câble le moteur de l'hôte à la vue à la
    // place de l'hôte (le contrat que le module :ai définit — Pigma
    // n'y connaît aucune intelligence artificielle). Le module
    // demeure pur ; l'hôte qui n'en veut pas ne branche jamais la
    // façade, rien ne se propose.
    api(project(":ai"))
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

// La publication Maven : JitPack construit le dépôt sur demande et
// l'artefact vit sous com.github.jjoblab.Pigma:editor:VERSION (le
// group posé ici, JitPack le remappe au sien ; le variant publié
// est le release, sa source l'accompagne). La bibliothèque ne se
// consomme qu'ainsi hors du dépôt : un hôte n'a QUE cette
// dépendance à déclarer. Le composant « release » n'existe
// qu'après la configuration du plugin Android : la publication
// s'y attache à l'évaluation close (la façon du guide JitPack).
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("editor") {
                groupId = "jo.pigma"
                artifactId = "editor"
                version = "1.0.0"

                from(components["release"])
            }
        }
    }
}
