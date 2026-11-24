plugins {
    id("java-library")
}

dependencies {
    // L3 lit le tampon (plages absolues) et les jetons de L1, la
    // profondeur de L2 entre dans le producteur : dépendances de
    // compilation, l'annotateur les expose dans son API.
    api(project(":core"))
    api(project(":syntax"))
    testImplementation(libs.archunit)
}
