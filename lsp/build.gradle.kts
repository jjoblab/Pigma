plugins {
    id("java-library")
}

dependencies {
    // Le client LSP convertit les positions du protocole (ligne,
    // colonne UTF-16) en offsets du tampon : la dépendance est
    // déclarée, l'hôte n'ajoute que :editor (qui l'expose).
    api(project(":core"))
    testImplementation(libs.archunit)
}
