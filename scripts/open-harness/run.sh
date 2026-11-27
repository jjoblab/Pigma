#!/usr/bin/env bash
# Harnais d'ouverture : compile les sources du dépôt contre les
# stubs Android du répertoire (src/stubs/), puis déroule les
# scénarios de src/OpenHarness.java. Prérequis :
#   - un JDK (17 ou plus) : JAVA_HOME s'il est posé, le sinon le
#     chemin ;
#   - les jars du client LSP (lsp4j, jsonrpc, gson) dans un cache
#     Gradle : GRADLE_USER_HOME, ~/.gradle, ou $TOOLS/gradle-home —
#     un `./gradlew check` préalable les y dépose.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
BUILD="$HERE/build"

if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/javac" ]; then
    JAVA_BIN="$JAVA_HOME/bin/java"
    JAVAC_BIN="$JAVA_HOME/bin/javac"
else
    JAVA_BIN="java"
    JAVAC_BIN="javac"
fi

if ! command -v "$JAVAC_BIN" >/dev/null 2>&1; then
    echo "run.sh : javac introuvable (poser JAVA_HOME sur un JDK)" >&2
    exit 2
fi

# Les jars du client LSP : le premier trouvé dans les caches possibles.
CANDIDATES=""
[ -n "${GRADLE_USER_HOME:-}" ] && CANDIDATES="$CANDIDATES ${GRADLE_USER_HOME}"
[ -n "${TOOLS:-}" ] && CANDIDATES="$CANDIDATES ${TOOLS}/gradle-home"
CANDIDATES="$CANDIDATES ${HOME}/.gradle"

JARS=""
for module in "org.eclipse.lsp4j/org.eclipse.lsp4j" \
              "org.eclipse.lsp4j/org.eclipse.lsp4j.jsonrpc" \
              "com.google.code.gson/gson"; do
    found=""
    for base in $CANDIDATES; do
        found="$(find "$base/caches/modules-2/files-2.1/$module" -name '*.jar' 2>/dev/null \
                | grep -v -- '-sources' | sort | tail -1)"
        [ -n "$found" ] && break
    done
    if [ -z "$found" ]; then
        echo "run.sh : jar introuvable pour $module — lancer ./gradlew check une fois pour peupler le cache" >&2
        exit 2
    fi
    JARS="$JARS:$found"
done
JARS="${JARS#:}"

rm -rf "$BUILD"
mkdir -p "$BUILD/classes"

find "$HERE/src/stubs" \
     "$ROOT/core/src/main/java" \
     "$ROOT/syntax/src/main/java" \
     "$ROOT/analysis/src/main/java" \
     "$ROOT/ai/src/main/java" \
     "$ROOT/editor/src/main/java" \
     "$ROOT/lsp/src/main/java" \
     "$ROOT/sample/src/main/java" \
     -name '*.java' > "$BUILD/sources.txt"
find "$HERE/src" -maxdepth 1 -name '*.java' >> "$BUILD/sources.txt"

"$JAVAC_BIN" -encoding UTF-8 -d "$BUILD/classes" -cp "$JARS" "@$BUILD/sources.txt"

"$JAVA_BIN" -Xmx1536m -cp "$BUILD/classes:$JARS" jo.pigma.sample.OpenHarness
