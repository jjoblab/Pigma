plugins {
    id("java-library")
    `maven-publish`
}

// La publication Maven : JitPack construit le dépôt sur demande et
// l'artefact vit sous com.github.jjoblab.Pigma:lsp:VERSION (le group
// posé ici, JitPack le remappe au sien — les dépendances api du
// POM s'y résolvent entre modules publiés).
publishing {
    publications {
        register<MavenPublication>("lsp") {
            groupId = "jo.pigma"
            artifactId = "lsp"
            version = "1.0.0"

            from(components.getByName("java"))
        }
    }
}

dependencies {
    // Le client LSP convertit les positions du protocole (ligne,
    // colonne UTF-16) en offsets du tampon : la dépendance est
    // déclarée, l'hôte n'ajoute que :editor (qui l'expose). lsp4j
    // porte le protocole et le JSON-RPC (gson en transitif) — la
    // seule dépendance extérieure du dépôt, décision du propriétaire
    // au chantier 26 : le client ne réécrit plus le protocole.
    api(project(":core"))
    api(libs.lsp4j)
    api(libs.lsp4j.jsonrpc)
    // gson ÉPINGLÉ : lsp4j le veut en intervalle, le chiffre exact
    // l'y fixe (un build repris reprend sa Gson — le rapport
    // d'analyse y voyait une version non figée)
    api(libs.gson)
    testImplementation(libs.archunit)
}
