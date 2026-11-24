import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.tasks.compile.JavaCompile

// Conventions communes de tous les modules, sans build-logic ni buildSrc :
// tout se déclare ici, dans un bloc subprojects court et lisible.

plugins {
    alias(libs.plugins.android.application) apply false
}

// Politique de langue et de commentaires (identifiants anglais ASCII,
// commentaires français, aucun motif de suivi ou d'historique).
val checkCodePolicy = tasks.register("checkCodePolicy", Exec::class) {
    group = "verification"
    description = "Vérifie la politique de langue et de commentaires du code Java."
    workingDir = rootDir
    commandLine("python3", rootProject.file("scripts/check-code-policy").absolutePath)
}

// Références interdites : les motifs vivent dans l'environnement local
// (variable PIGMA_FORBIDDEN_REFS ou fichier config/forbidden-refs.local),
// jamais dans les fichiers versionnés.
val checkForbiddenRefs = tasks.register("checkForbiddenRefs", Exec::class) {
    group = "verification"
    description = "Vérifie qu'aucune référence interdite ne figure dans les fichiers versionnés."
    workingDir = rootDir
    commandLine("python3", rootProject.file("scripts/check-forbidden-refs").absolutePath)
}

// Captures du catalogue de versions : l'accesseur n'existe que pour le
// script racine, pas dans subprojects.
val checkstyleVersion = libs.versions.checkstyle.get()
val junitLibrary = libs.junit
val javaRelease = libs.versions.java.get().toInt()

subprojects {
    apply(plugin = "checkstyle")
    extensions.configure<CheckstyleExtension> {
        toolVersion = checkstyleVersion
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    }
    tasks.withType<Checkstyle>().configureEach {
        maxWarnings = 0
        maxErrors = 0
    }

    // Tâche Checkstyle unique et explicite, identique pour tout module
    // (JVM ou Android) : un répertoire de sources inexistant est simplement
    // ignoré, un module sans sources produit une vérification vide.
    val checkstyleSources = tasks.register("checkstyleSources", Checkstyle::class) {
        group = "verification"
        description = "Vérifie le style des sources Java du module."
        source("src/main/java", "src/test/java")
        classpath = files()
    }

    // Modules JVM purs : Java 17 strict (drapeau release) et JUnit commun.
    plugins.withType<JavaPlugin> {
        tasks.withType<JavaCompile>().configureEach {
            options.release.set(javaRelease)
        }
        dependencies {
            "testImplementation"(junitLibrary)
        }
    }

    // Modules Android : JUnit commun, les autres dépendances restent dans
    // le module concerné.
    plugins.withId("com.android.application") {
        dependencies {
            "testImplementation"(junitLibrary)
        }
    }

    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(checkCodePolicy, checkForbiddenRefs, checkstyleSources)
    }
}

// Raccourcis racine : « ./gradlew check » et « ./gradlew test » parcourent
// tout le dépôt sans viser un module en particulier.
tasks.register("check") {
    group = "verification"
    description = "Toutes les vérifications : tests, style, politique, références interdites."
    dependsOn(checkCodePolicy, checkForbiddenRefs)
    dependsOn(subprojects.map { "${it.path}:check" })
}

tasks.register("test") {
    group = "verification"
    description = "Tous les tests unitaires du dépôt."
    dependsOn(subprojects.map { "${it.path}:test" })
}
