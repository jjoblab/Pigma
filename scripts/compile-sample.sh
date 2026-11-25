#!/usr/bin/env bash
# Contournement de build : compiler le sample par javac contre
# android.jar et les classes des modules quand le réseau limite la
# résolution Gradle. Vérifie la compilation SANS produire l'APK (les
# ressources Android ne s'y traitent pas — l'APK reste à Gradle).
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"

ANDROID=${PIGMA_ANDROID_JAR:-/home/z/my-project/tools/android-sdk/platforms/android-35/android.jar}
GRADLE_HOME=/home/z/my-project/tools/gradle-home
# Les jars se cherchent dans le cache (le réempoilage change la somme du
# répertoire, la version reste la clé).
jar_du_cache() {
    find "$GRADLE_HOME/caches/modules-2/files-2.1/$1" \
        -name "$2" ! -name '*-sources*' ! -name '*-javadoc*' 2>/dev/null \
        | sort | tail -1
}
LSP4J="$(jar_du_cache org.eclipse.lsp4j/org.eclipse.lsp4j org.eclipse.lsp4j-1.0.0.jar)"
JSONRPC="$(jar_du_cache org.eclipse.lsp4j/org.eclipse.lsp4j.jsonrpc org.eclipse.lsp4j.jsonrpc-1.0.0.jar)"
GSON="$(jar_du_cache com.google.code.gson/gson gson-2.15.0.jar)"
if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/javac" ]; then
    JAVAC="${JAVA_HOME}/bin/javac"
else
    JAVAC=javac
fi
EDITOR="${PIGMA_EDITOR_CLASSES:-$ROOT/editor/build/intermediates/runtime_library_classes_dir/release/bundleLibRuntimeToDirRelease}"
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
