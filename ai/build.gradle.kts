// Le module de l'intégration de l'IA : PUR JVM, aucun Android —
// des types de données et des interfaces, rien d'autre. Pigma n'y
// connaît AUCUNE intelligence artificielle : l'hôte implémente
// l'interface, Pigma prépare le contexte et applique les
// modifications en sécurité. Le test de pureté y veille comme au
// cœur.
plugins {
    id("java-library")
    `maven-publish`
}

// La publication Maven : JitPack construit le dépôt sur demande et
// l'artefact vit sous com.github.jjoblab.Pigma:ai:VERSION (le group
// posé ici, JitPack le remappe au sien — les dépendances api du
// POM s'y résolvent entre modules publiés).
publishing {
    publications {
        register<MavenPublication>("ai") {
            groupId = "jo.pigma"
            artifactId = "ai"
            version = "1.0.0"

            from(components.getByName("java"))
        }
    }
}

dependencies {
    testImplementation(libs.archunit)
}
