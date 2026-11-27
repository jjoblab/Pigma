plugins {
    id("java-library")
    `maven-publish`
}

// La publication Maven : JitPack construit le dépôt sur demande et
// l'artefact vit sous com.github.jjoblab.Pigma:core:VERSION (le group
// posé ici, JitPack le remappe au sien — les dépendances api du
// POM s'y résolvent entre modules publiés).
publishing {
    publications {
        register<MavenPublication>("core") {
            groupId = "jo.pigma"
            artifactId = "core"
            version = "1.0.0"

            from(components.getByName("java"))
        }
    }
}

dependencies {
    testImplementation(libs.archunit)
}
