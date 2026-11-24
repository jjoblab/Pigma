#!/bin/sh
# Banc E0 : faisabilite de la generation de lexeurs (JFlex).
#
# Enchaine : generation des trois lexeurs (temps et memoire mesures),
# compilation, verifications (dore, couverture, redemarrage), mesures
# de vitesse (JVM froide puis rechauffee) et poids DEX par langage.
# Le resultat complet va dans bench/out/rapport-e0.txt.
#
# Usage : bench/scripts/run-e0.sh [racine-du-depot]
# Variables surchargeables : TOOLS (JDK, JFlex, d8), JAVA_HOME.

set -e

ROOT=${1:-.}
TOOLS=${TOOLS:-/home/z/my-project/tools}
JAVA_HOME_DIR=${JAVA_HOME:-$TOOLS/jdk-17}
JAVA=$JAVA_HOME_DIR/bin/java
JAVAC=$JAVA_HOME_DIR/bin/javac
JFLEX_CP="$TOOLS/jflex.jar:$TOOLS/java-cup-runtime-11b-20160615-3.jar"
D8=${D8:-$TOOLS/android-sdk/build-tools/34.0.0/d8}
ANDROID_JAR=${ANDROID_JAR:-$TOOLS/android-sdk/platforms/android-35/android.jar}
OUT=$ROOT/bench/out
GEN=$OUT/generated
CLASSES=$OUT/classes
COLD_RUNS=${COLD_RUNS:-5}

echo "== banc E0 : JFlex 1.9.1, JDK $($JAVA -version 2>&1 | head -1) =="

mkdir -p "$GEN" "$CLASSES"

# --- generation des trois lexeurs, temps et memoire -------------------

for lang in java xml json; do
    echo "== generation $lang =="
    # Pas de /usr/bin/time dans tous les environnements : le lanceur
    # Python mesure elapsed et memoire maximale (rusage des enfants).
    python3 "$ROOT/bench/scripts/time-run.py" "$JAVA" -cp "$JFLEX_CP" \
        jflex.Main --nobak -d "$GEN" "$ROOT/bench/lexers/$lang.flex" \
        > "$OUT/gen-$lang.log" 2> "$OUT/gen-$lang.time"
    grep -E "states in minimized|Writing" "$OUT/gen-$lang.log" || true
    grep "MESUREGEN" "$OUT/gen-$lang.time" | sed "s/^/gen-$lang /"
done
ls -la "$GEN" | grep -v '^total' | grep Lexer

# --- compilation et verifications -------------------------------------

"$JAVAC" --release 17 -d "$CLASSES" "$GEN"/*.java \
    "$ROOT/bench/src/jo/pigma/bench/generated/LexerApi.java" \
    "$ROOT/bench/src/jo/pigma/bench/BenchRunner.java"
echo "== verifications =="
"$JAVA" -cp "$CLASSES" jo.pigma.bench.BenchRunner checks "$ROOT"

# --- vitesse : JVM froide, puis rechauffee ----------------------------

echo "== passes uniques a JVM froide ($COLD_RUNS executions) =="
for run in $(seq 1 "$COLD_RUNS"); do
    "$JAVA" -cp "$CLASSES" jo.pigma.bench.BenchRunner cold "$ROOT"
done
echo "== passes rechauffees =="
"$JAVA" -cp "$CLASSES" jo.pigma.bench.BenchRunner warm "$ROOT"

# --- poids DEX par langage (d8, min-api 26) ---------------------------

echo "== poids DEX (d8, min-api 26) =="
rm -rf "$OUT"/dex-*
for lang in java xml json; do
    mkdir -p "$OUT/dex-src-$lang" "$OUT/dex-$lang"
    # Bytecode version modeste : le code genere est compatible Java 8,
    # d8 n'a rien a desucrer ici.
    "$JAVAC" --release 8 -d "$OUT/dex-src-$lang" \
        "$GEN/$(echo "$lang" | sed 's/^./\U&/')Lexer.java" \
        "$ROOT/bench/src/jo/pigma/bench/generated/LexerApi.java"
    # d8 prend des fichiers de programme, pas un repertoire.
    classes=$(find "$OUT/dex-src-$lang" -name '*.class' | tr '\n' ' ')
    "$D8" --release --min-api 26 --lib "$ANDROID_JAR" \
        --output "$OUT/dex-$lang" $classes
    bytes=$(stat -c %s "$OUT/dex-$lang/classes.dex")
    echo "poids DEX $lang : $bytes octets"
done
mkdir -p "$OUT/dex-src-tous" "$OUT/dex-tous"
"$JAVAC" --release 8 -d "$OUT/dex-src-tous" "$GEN"/*.java \
    "$ROOT/bench/src/jo/pigma/bench/generated/LexerApi.java"
classes=$(find "$OUT/dex-src-tous" -name '*.class' | tr '\n' ' ')
"$D8" --release --min-api 26 --lib "$ANDROID_JAR" \
    --output "$OUT/dex-tous" $classes
bytes=$(stat -c %s "$OUT/dex-tous/classes.dex")
echo "poids DEX java+xml+json (un dex, interface comprise) : $bytes octets"

echo "== fin du banc E0 =="
