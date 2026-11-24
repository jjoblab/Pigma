plugins {
    id("java-library")
}

dependencies {
    // Le moteur consomme le tampon (lecture, lignes, écoute des
    // remplacements) et l'expose dans son API : dépendance de compilation.
    api(project(":core"))
    testImplementation(libs.archunit)
}
