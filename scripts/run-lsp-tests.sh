#!/usr/bin/env bash
# Contournement de build : le module :lsp est du pur Java, ses tests
# courent par javac + JUnit quand le réseau limite la résolution
# Gradle. Le binaire EmmyLua de bureau (EMMYLUA_LS) s'y éprouve par
# processus. Usage : scripts/run-lsp-tests.sh [classes de test…]
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"

GRADLE_HOME=/home/z/my-project/tools/gradle-home
JUNIT="$GRADLE_HOME/caches/modules-2/files-2.1/junit/junit/4.13.2/8ac9e16d933b6fb43bc7f576336b8f4d7eb5ba12/junit-4.13.2.jar"
HAMCREST="$GRADLE_HOME/caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/42a25dc3219429f0e5d060061f71acb49bf010a0/hamcrest-core-1.3.jar"
LSP4J="$GRADLE_HOME/caches/modules-2/files-2.1/org.eclipse.lsp4j/org.eclipse.lsp4j/1.0.0/414d75c051310033de34af07c0cec3705d6d0ab5/org.eclipse.lsp4j-1.0.0.jar"
JSONRPC="$GRADLE_HOME/caches/modules-2/files-2.1/org.eclipse.lsp4j/org.eclipse.lsp4j.jsonrpc/1.0.0/11580adce31a287dda1ed6eb4fb8f76f1a2cc815/org.eclipse.lsp4j.jsonrpc-1.0.0.jar"
GSON="$GRADLE_HOME/caches/modules-2/files-2.1/com.google.code.gson/gson/2.15.0/3c8f3903bd90f774de74f126be756bbc1408abcb/gson-2.15.0.jar"
ARCHUNIT="$GRADLE_HOME/caches/modules-2/files-2.1/com.tngtech.archunit/archunit/1.3.0/4f4c504ec5369491d9d5c16afa6fc5261a5d81/archunit-1.3.0.jar"
SLF4J="$GRADLE_HOME/caches/modules-2/files-2.1/org.slf4j/slf4j-api/2.0.12/48f109a2a6d8f446c794f3e3fa0d86df0cdfa312/slf4j-api-2.0.12.jar"
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
