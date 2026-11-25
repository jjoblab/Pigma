#!/usr/bin/env bash
# Contournement de build : le module :syntax est du pur Java, ses tests
# courent par javac + JUnit quand le réseau limite la résolution Gradle
# (le plugin Android du projet racine n'a pas besoin d'être résolu pour
# ce module). Usage : scripts/run-syntax-tests.sh [classes de test…]
set -euo pipefail
cd "$(dirname "$0")/.."

GRADLE_HOME=/home/z/my-project/tools/gradle-home
JUNIT="$GRADLE_HOME/caches/modules-2/files-2.1/junit/junit/4.13.2/8ac9e16d933b6fb43bc7f576336b8f4d7eb5ba12/junit-4.13.2.jar"
HAMCREST="$GRADLE_HOME/caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/42a25dc3219429f0e5d060061f71acb49bf010a0/hamcrest-core-1.3.jar"
ARCHUNIT="/home/z/my-project/tools/gradle-home/caches/modules-2/files-2.1/com.tngtech.archunit/archunit/1.3.0/4f4c504ec5369491d9d5c16afa6fc5261a5d81/archunit-1.3.0.jar"
SLF4J="/home/z/my-project/tools/gradle-home/caches/modules-2/files-2.1/org.slf4j/slf4j-api/2.0.12/48f109a2a6d8f446c794f3e3fa0d86df0cdfa312/slf4j-api-2.0.12.jar"
BUILD=/tmp/pigma-syntax-build
TESTS=/tmp/pigma-syntax-tests
if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/javac" ]; then
    JAVAC="${JAVA_HOME}/bin/javac"; JAVA="${JAVA_HOME}/bin/java"
else
    JAVAC=javac; JAVA=java
fi

rm -rf "$BUILD" "$TESTS"
mkdir -p "$BUILD" "$TESTS"
find syntax/src/main/java -name '*.java' > /tmp/pigma-syntax-sources.txt
$JAVAC -nowarn -d "$BUILD" -cp core/build/classes/java/main @/tmp/pigma-syntax-sources.txt
find syntax/src/test/java -name '*.java' > /tmp/pigma-syntax-tests.txt
$JAVAC -nowarn -d "$TESTS" -cp "$BUILD:core/build/classes/java/main:$JUNIT:$HAMCREST:$ARCHUNIT:$SLF4J" @/tmp/pigma-syntax-tests.txt

CP="$TESTS:$BUILD:$(pwd)/core/build/classes/java/main:$(pwd)/syntax/src/test/resources:$JUNIT:$HAMCREST:$ARCHUNIT:$SLF4J"
if [ "$#" -gt 0 ]; then
  $JAVA -cp "$CP" org.junit.runner.JUnitCore "$@"
else
  cd syntax && $JAVA -cp "$CP" org.junit.runner.JUnitCore \
    jo.pigma.syntax.LexersGoldenTest \
    jo.pigma.syntax.LexersRestartTest \
    jo.pigma.syntax.KotlinPileTest \
    jo.pigma.syntax.LazyWindowTest
fi
