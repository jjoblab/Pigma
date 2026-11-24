# Banc d'essai (hors build courant)

Ce répertoire porte les bancs d'essai du plan : le **banc E0**
(faisabilité de la génération de lexeurs) et le **profil du chargement
de grammaires** du dépôt de référence. Rien ici n'entre dans
`./gradlew check` : les bancs se lancent à la main, leurs sorties vont
dans `bench/out/` (ignoré par git — le code généré n'est pas committé,
la décision appartient au propriétaire, voir
`docs/etude-faisabilite-lexeurs.md`).

## Prérequis

- JDK 17 (bac à sable : `/home/z/my-project/tools/jdk-17`) ;
- JFlex 1.9.1 + sa dépendance cup au moment de la génération
  (`jflex.jar`, `java-cup-runtime-11b-20160615-3.jar`) ;
- `d8` et `android.jar` du SDK (poids DEX) ;
- un clone en lecture seule du dépôt de référence pour le profil.

Les variables `TOOLS`, `D8`, `ANDROID_JAR`, `JAVA_HOME` surchargent les
chemins par défaut du bac à sable.

## Banc E0 : `bench/scripts/run-e0.sh`

```
sh bench/scripts/run-e0.sh .
```

Enchaîne : génération des trois lexeurs (temps et mémoire mesurés par
`bench/scripts/time-run.py`, le bac n'ayant pas `/usr/bin/time`),
compilation, vérifications (**doré** contre `bench/data/*-golden.txt`,
**couverture** sans trou ni chevauchement, **redémarrage** depuis les
positions en état initial), vitesse (JVM froide puis rechauffée) et
**poids DEX par langage**. Le rapport complet va dans
`bench/out/rapport-e0.txt`.

- Les descriptions de lexeurs vivent dans `bench/lexers/*.flex`
  (commentaires en français) ; les types de jeton sont des entiers sans
  couleur, la table de portées viendra avec le moteur.
- Les corpus (`bench/data/*-2000.*`) sont produits par
  `bench/data/make-data.py`, de façon déterministe.
- Les fichiers de référence (`bench/data/*-reference.*`) sont écrits à
  la main ; la sortie figée (`*-golden.txt`) se régénère par
  `java -cp bench/out/classes jo.pigma.bench.BenchRunner dump .` après
  une relance du banc — **relire le diff avant de committer** : c'est
  le contrôle d'exactitude.

## Profil du dépôt de référence : `bench/scripts/run-profil-reference.sh`

```
sh bench/scripts/run-profil-reference.sh <racine-du-clone-du-depot-de-reference>
```

Mesure, pour chaque grammaire TextMate du dépôt de référence (27
intégrées au module de langages, 20 « du commerce » dans les ressources
de banc) : l'analyse JSON (gson, même version que le dépôt de
référence) et la compilation joni de chaque motif (mêmes options que sa
fabrique d'expressions). Deux passes : JIT froide (le cas du téléphone
au premier chargement) puis rechauffée. Les sorties vont dans
`bench/out/profil-*.txt`. La mesure minore le coût réel : le tissage
des règles du chargeur (inclusions, mots réservés) s'ajoute en pratique.
