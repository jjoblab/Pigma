// Le module de l'intégration de l'IA : PUR JVM, aucun Android —
// des types de données et des interfaces, rien d'autre. Pigma n'y
// connaît AUCUNE intelligence artificielle : l'hôte implémente
// l'interface, Pigma prépare le contexte et applique les
// modifications en sécurité. Le test de pureté y veille comme au
// cœur.
plugins {
    id("java-library")
}

dependencies {
    testImplementation(libs.archunit)
}
