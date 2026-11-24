plugins {
    id("java-library")
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
    testImplementation(libs.archunit)
}
