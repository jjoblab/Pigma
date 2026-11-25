#!/usr/bin/env bash
# Contournement de build : le module :lsp est du pur Java, ses tests
# courent par javac + JUnit quand le réseau limite la résolution
# Gradle. Le binaire EmmyLua de bureau (EMMYLUA_LS) s'y éprouve par
# processus. Usage : scripts/run-lsp-tests.sh [classes de test…]
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"

GRADLE_HOME=/home/z/my-project/tools/gradle-home
# Les jars se cherchent dans le cache plutôt qu'un chemin en dur : le
# réempoilage du cache change la somme du répertoire (la leçon de la
# session 29), la version reste la clé.
jar_du_cache() {
    find "$GRADLE_HOME/caches/modules-2/files-2.1/$1" \
        -name "$2" ! -name '*-sources*' ! -name '*-javadoc*' 2>/dev/null \
        | sort | tail -1
}
JUNIT="$(jar_du_cache junit/junit junit-4.13.2.jar)"
HAMCREST="$(jar_du_cache org.hamcrest/hamcrest-core hamcrest-core-1.3.jar)"
LSP4J="$(jar_du_cache org.eclipse.lsp4j/org.eclipse.lsp4j org.eclipse.lsp4j-1.0.0.jar)"
JSONRPC="$(jar_du_cache org.eclipse.lsp4j/org.eclipse.lsp4j.jsonrpc org.eclipse.lsp4j.jsonrpc-1.0.0.jar)"
GSON="$(jar_du_cache com.google.code.gson/gson gson-2.15.0.jar)"
ARCHUNIT="$(jar_du_cache com.tngtech.archunit/archunit archunit-1.3.0.jar)"
SLF4J="$(jar_du_cache org.slf4j/slf4j-api slf4j-api-2.0.12.jar)"
for j in "$JUNIT" "$HAMCREST" "$LSP4J" "$JSONRPC" "$GSON" "$ARCHUNIT" "$SLF4J"; do
    [ -n "$j" ] || { echo "run-lsp-tests.sh : jar introuvable dans le cache Gradle ($GRADLE_HOME)" >&2; exit 2; }
done
if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/javac" ]; then
    JAVAC="${JAVA_HOME}/bin/javac"; JAVA="${JAVA_HOME}/bin/java"
else
    JAVAC=javac; JAVA=java
fi
BUILD=/tmp/pigma-lsp-build
TESTS=/tmp/pigma-lsp-tests

rm -rf "$BUILD" "$TESTS"
mkdir -p "$BUILD" "$TESTS"
find lsp/src/main/java -name '*.java' > /tmp/pigma-lsp-sources.txt
$JAVAC -nowarn -d "$BUILD" -cp "$LSP4J:$JSONRPC:$GSON:$ROOT/core/build/classes/java/main" @/tmp/pigma-lsp-sources.txt
find lsp/src/test/java -name '*.java' > /tmp/pigma-lsp-tests.txt
$JAVAC -nowarn -d "$TESTS" -cp "$BUILD:$ROOT/core/build/classes/java/main:$LSP4J:$JSONRPC:$GSON:$JUNIT:$HAMCREST:$ARCHUNIT:$SLF4J" @/tmp/pigma-lsp-tests.txt

CP="$TESTS:$BUILD:$ROOT/core/build/classes/java/main:$LSP4J:$JSONRPC:$GSON:$JUNIT:$HAMCREST:$ARCHUNIT:$SLF4J"
cd lsp
if [ "$#" -gt 0 ]; then
  $JAVA -cp "$CP" org.junit.runner.JUnitCore "$@"
else
  $JAVA -cp "$CP" org.junit.runner.JUnitCore \
    jo.pigma.lsp.LspSessionTest \
    jo.pigma.lsp.LspProcessTransportTest \
    jo.pigma.lsp.EmmyLuaProcessTest
fi
