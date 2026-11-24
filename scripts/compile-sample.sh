#!/usr/bin/env bash
# Contournement de build : compiler le sample par javac contre
# android.jar et les classes des modules quand le réseau limite la
# résolution Gradle. Vérifie la compilation SANS produire l'APK (les
# ressources Android ne s'y traitent pas — l'APK reste à Gradle).
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"

ANDROID=/home/z/my-project/tools/android-sdk/platforms/android-35/android.jar
GRADLE_HOME=/home/z/my-project/tools/gradle-home
LSP4J="$GRADLE_HOME/caches/modules-2/files-2.1/org.eclipse.lsp4j/org.eclipse.lsp4j/1.0.0/414d75c051310033de34af07c0cec3705d6d0ab5/org.eclipse.lsp4j-1.0.0.jar"
JSONRPC="$GRADLE_HOME/caches/modules-2/files-2.1/org.eclipse.lsp4j/org.eclipse.lsp4j.jsonrpc/1.0.0/11580adce31a287dda1ed6eb4fb8f76f1a2cc815/org.eclipse.lsp4j.jsonrpc-1.0.0.jar"
GSON="$GRADLE_HOME/caches/modules-2/files-2.1/com.google.code.gson/gson/2.15.0/3c8f3903bd90f774de74f126be756bbc1408abcb/gson-2.15.0.jar"
JAVAC=/home/z/my-project/tools/jdk-17.0.21.1+2/bin/javac
EDITOR="$ROOT/editor/build/intermediates/runtime_library_classes_dir/release/bundleLibRuntimeToDirRelease"
BUILD=/tmp/pigma-sample-check

rm -rf "$BUILD"
mkdir -p "$BUILD"
# Les classes fraîches de :syntax et :lsp viennent des scripts de test
# (run-syntax-tests.sh, run-lsp-tests.sh) — le sample y lit Lua et la
# négociation ; les compiler d'abord si /tmp est vide.
CP="$ANDROID:$EDITOR:$ROOT/core/build/classes/java/main:/tmp/pigma-syntax-build:/tmp/pigma-lsp-build:$LSP4J:$JSONRPC:$GSON"
find sample/src/main/java -name '*.java' > /tmp/pigma-sample-sources.txt
$JAVAC -nowarn -d "$BUILD" -cp "$CP" \
    @/tmp/pigma-sample-sources.txt
echo "sample : compilation OK ("$(wc -l < /tmp/pigma-sample-sources.txt)" fichiers)"
