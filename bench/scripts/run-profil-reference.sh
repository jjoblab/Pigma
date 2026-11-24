#!/bin/sh
# Profil du chargement de grammaires du depot de reference (lecture
# seule) : temps d'analyse JSON et de compilation joni par grammaire,
# pour les grammaires integrees du module de langages puis les
# grammaires du commerce des ressources de banc du module de grammaires.
#
# Usage : bench/scripts/run-profil-reference.sh <racine-du-clone-du-depot-de-reference>
# Variables surchargeables : TOOLS (JDK, gson, joni, jcodings).

set -e

DEPOT=${1:?usage : run-profil-reference.sh <racine-du-clone-du-depot-de-reference>}
TOOLS=${TOOLS:-/home/z/my-project/tools}
JAVA=$TOOLS/jdk-17/bin/java
JAVAC=$TOOLS/jdk-17/bin/javac
CP="$TOOLS/gson-2.15.0.jar:$TOOLS/joni-2.2.10.jar:$TOOLS/jcodings-1.0.65.jar"
OUT=bench/out/profil

mkdir -p "$OUT"
"$JAVAC" --release 17 -cp "$CP" -d "$OUT" \
    bench/profil-reference/ProfileGrammars.java
echo "== grammaires integrees (paquets de langages) =="
"$JAVA" -cp "$OUT:$CP" jo.pigma.bench.ProfileGrammars \
    "$DEPOT/core-language/src/main/resources/langages"
echo "== grammaires du commerce (resources de banc) =="
"$JAVA" -cp "$OUT:$CP" jo.pigma.bench.ProfileGrammars \
    "$DEPOT/core-syntax-grammars/src/test/resources/grammaires-commerce"
