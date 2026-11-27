plugins {
    id("java-library")
    `maven-publish`
}

// La publication Maven : JitPack construit le dépôt sur demande et
// l'artefact vit sous com.github.jjoblab.Pigma:analysis:VERSION (le group
// posé ici, JitPack le remappe au sien — les dépendances api du
// POM s'y résolvent entre modules publiés).
publishing {
    publications {
        register<MavenPublication>("analysis") {
            groupId = "jo.pigma"
            artifactId = "analysis"
            version = "1.0.0"

            from(components.getByName("java"))
        }
    }
}

dependencies {
    // L3 lit le tampon (plages absolues) et les jetons de L1, la
    // profondeur de L2 entre dans le producteur : dépendances de
    // compilation, l'annotateur les expose dans son API.
    api(project(":core"))
    api(project(":syntax"))
    testImplementation(libs.archunit)
}
