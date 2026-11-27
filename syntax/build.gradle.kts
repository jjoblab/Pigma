plugins {
    id("java-library")
    `maven-publish`
}

// La publication Maven : JitPack construit le dépôt sur demande et
// l'artefact vit sous com.github.jjoblab.Pigma:syntax:VERSION (le group
// posé ici, JitPack le remappe au sien — les dépendances api du
// POM s'y résolvent entre modules publiés).
publishing {
    publications {
        register<MavenPublication>("syntax") {
            groupId = "jo.pigma"
            artifactId = "syntax"
            version = "1.0.0"

            from(components.getByName("java"))
        }
    }
}

dependencies {
    // Le moteur consomme le tampon (lecture, lignes, écoute des
    // remplacements) et l'expose dans son API : dépendance de compilation.
    api(project(":core"))
    testImplementation(libs.archunit)
}
