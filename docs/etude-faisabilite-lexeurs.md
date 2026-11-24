# Étude de faisabilité : la génération de lexeurs (banc E0)

> **Cadre.** Test de faisabilité exigé par le plan avant le chantier 3
> (moteur de coloration) : générer des lexeurs pour Java, XML et JSON,
> mesurer la génération, la vitesse, le poids et l'exactitude, profiler
> le dépôt de référence (le nom ne se cite qu'ici-bas et dans
> `PORTAGE.md`), et préparer les décisions du propriétaire. Le banc vit
> dans `bench/` (voir `bench/README.md` pour le rejouer).

## 1. Méthode et environnement

Mesures sur la machine de build du bac (JVM x86-64, JDK 17, JFlex
1.9.1, d8 du SDK 34, min-api 26) — **pas sur appareil** : ce que le
banc prouve, c'est que la génération marche, que le code généré est
léger, rapide et exact ; les mesures appareil (première peinture réelle,
budget par image, plafonds) viendront avec le panneau de diagnostic du
sample, chantier 3 et suivants. Les corpus font environ 2 000 lignes
(60 154 / 107 846 / 49 178 caractères, 27 154 / 24 926 / 12 878
jetons), produits de façon déterministe par `bench/data/make-data.py`.

## 2. Résultats du banc E0

### (a) La génération

| Langage | Temps de génération | Mémoire maximale | DFA minimisé | Code généré |
| --- | --- | --- | --- | --- |
| Java | 0,46 s | 73 Mo | 228 états | 48 465 octets |
| XML | 0,26 s | 64 Mo | 48 états | 22 038 octets |
| JSON | 0,26 s | 64 Mo | 30 états | 20 351 octets |

La génération est **légère et rapide** : moins d'une demi-seconde et
moins de 75 Mo par lexeur sur JVM x86. Sur téléphone, elle devrait
rester dans les secondes — **à vérifier par le propriétaire sur
AndroidIDE ou Termux** :

```
java -cp jflex.jar:java-cup-runtime-11b-20160615-3.jar jflex.Main \
    --nobak -d <sortie> bench/lexers/java.flex
```

Si elle n'y passe pas, la consigne du plan s'applique : générer
ailleurs (la machine de build) et ne committer que le code généré —
c'est de toute façon l'option recommandée en §5.

### (b) Vitesse de tokenisation (proxy de première peinture)

| Langage | Passe unique, JVM froide (médiane de 5) | Rechauffée (meilleure de 20) | Débit |
| --- | --- | --- | --- |
| Java | 16,1 ms | 1,7 ms | 33 Mo/s |
| XML | 10,5 ms | 2,2–3,4 ms | 30–47 Mo/s |
| JSON | 3,1 ms | 1,2–3,5 ms | 14–38 Mo/s |

L'écart entre exécutions vient d'une machine partagée ; l'ordre de
grandeur est stable. **La tokenisation complète d'un fichier de 2 000
lignes prend quelques millisecondes** : la première peinture réelle ne
lexe que la fenêtre visible (le plan : tokenisation paresseuse par
fenêtre), donc un fragment — le lexeur n'est pas le goulot. Sur appareil
(2 à 4 fois plus lent), il reste sous la dizaine de millisecondes par
fenêtre.

### (c) Poids ajouté à l'APK par langage (d8, min-api 26)

| Contenu | Octets DEX |
| --- | --- |
| JavaLexer seul | 18 788 |
| XmlLexer seul | 7 804 |
| JsonLexer seul | 7 420 |
| Les trois + interface du banc, un seul dex | **27 612** |

Environ **9 à 19 Ko par langage**, 27 Ko pour les trois : négligeable
devant le Mo qu'embarque une seule grammaire TextMate « du commerce »
(voir §3). Non compris : la table jeton → portée (des centaines
d'octets par langage) et le moteur commun (le chantier 3 le mesurera
sur l'APK réel du sample).

### (d) Test de redémarrage

Vert sur les trois lexeurs : relexer depuis chaque point de contrôle en
état initial redonne **exactement** la suite des jetons d'un lexage
depuis le début (202–205 points contrôlés par langage ; le XML, seul à
porter un état non redémarrable — l'intérieur de balise —, en compte
8 978 positions éligibles sur 24 926). Le moteur réel portera des états
supplémentaires (commentaires de bloc, gabarits de chaîne) : le test de
redémarrage devra alors couvrir les états **déclarés redémarrables**
(par défaut : l'état initial), comme le fait IntelliJ.

### (e) Exactitude

Trois garanties : la **sortie figée** (`bench/data/*-golden.txt`) a été
relue jeton par jeton à la main avant d'être commitée (812 jetons Java,
190 XML, 186 JSON — le format échappe les jetons multi-lignes) ; la
**couverture** vérifie que les jetons tapissent le texte sans trou ni
chevauchement (0 erreur sur les corpus) ; la relecture a fait remonter
et corriger deux vrais défauts (la règle des instructions de traitement
était avide — le premier jeton engloutissait la déclaration XML — et la
tokenisation des attributs était impossible sans état). Toute retouche
future d'une description casse le test : c'est le filet de sécurité du
moteur.

## 3. Profil du dépôt de référence (l'hypothèse des grammaires)

La chaîne de chargement, chaque maillon avec fichier et ligne :
les paquets intégrés vivent en ressources de classpath
(`Packs.java:44-48`) ; le chargeur lit, valide **et compile** à
l'appel (`PackLoader.java:84-118`, empreinte SHA-256 en cache) ; la
plateforme offre les deux voies, compilation au chargement ou
chargement résumé différé (`LanguagePlatform.java:130-162`) ; le registre
des grammaires compilées est paresseux par portée racine
(`PackGrammars.java:44-52`) ; chaque motif se compile par le moteur
d'expressions régulières, portage Java d'Oniguruma
(`Expressions.java:51-60`, `Option.CAPTURE_GROUP`).

Mesure (mêmes bibliothèques et mêmes options, `bench/scripts/run-profil-reference.sh`) :
27 grammaires intégrées (1,33 Mo, 4 435 motifs) coûtent
**parse 64 ms + compilation 938–1 144 ms** à JIT froide — **environ
une seconde sur JVM x86 rapide** ; les 20 grammaires « du commerce »
des ressources de banc (2,3 Mo, 6 538 motifs) coûtent 146 + 1 009 ms.
La plus lourde, le C++ (1 083 motifs), paie 48 + 213 ms ; TypeScript
515 motifs, environ 120 ms. Sur un téléphone milieu de gamme (CPU 2 à
4 fois plus lent, démarrage JIT plus coûteux sur ART), l'ensemble
atteint **plusieurs secondes** — et chaque langage ouvert pour la
première fois paie sa part (centaines de millisecondes à plus d'une
seconde).

**Verdict : l'hypothèse de l'étude est confirmée.** Le retard au
démarrage et à la première ouverture vient bien du chargement et de la
compilation des grammaires, pas de la tokenisation ligne à ligne ; la
voie retenue (lexeurs compilés à la construction, rien à charger à
l'exécution) évite ce coût par construction. À noter : le dépôt de
référence embarque aussi une voie de chargement résumé puis paresseux
(les mêmes fichiers, lignes ci-dessus) — l'optimisation existe là-bas,
le coût de base reste.

## 4. Licences

- **JFlex 1.9.1** : BSD-3. Outil de **compilation uniquement** — le jar
  n'entre jamais dans l'APK. Le code généré ne porte **aucun en-tête de
  licence** (en-tête minimal « Generated by JFlex »), il est autonome :
  aucune dépendance au moment de l'exécution. Le jour où le code généré
  est committé, une ligne d'attribution au `NOTICE` reste la prudence
  élémentaire. Ce n'est pas un avis juridique.
- **Descriptions de lexeurs de la communauté IntelliJ** :
  `intellij-community` est sous Apache 2.0 — reprise fichier par
  fichier permise avec attribution au `NOTICE`. La recommandation reste
  de réécrire (le banc a prouvé que c'est peu coûteux), la reprise
  restant possible au cas par cas.
- **CodeAssist (GPL v3)** et l'éditeur tiers de la règle d'arrêt
  (LGPL 2.1) : idées et algorithmes seulement, aucun code — déjà acté
  par l'étude de conception.

## 5. Décisions à soumettre au propriétaire

1. **Générateur** — *recommandé : JFlex existant (BSD-3, outil de
   build, code généré autonome, 0,3–0,5 s et ~70 Mo par lexeur, testé
   par le banc)*. L'alternative (générateur écrit par nous) n'apporte
   d'intérêt que le jour où JFlex deviendra un bloc — à réévaluer
   alors.
2. **Code généré committé ?** — *recommandé : oui, commité* (le build
   sur téléphone reste léger : aucun jar à télécharger ni Java
   supplémentaire ; les fichiers générés sont exclus des contrôles de
   langue — le répertoire `generated` l'est déjà — et un contrôle
   d'à-jour vérifiera la correspondance description → généré). Sinon :
   généré au build, JFlex alors requis sur la machine de construction.
3. **Langages de départ** — *recommandé : Java, Kotlin, XML, JSON en
   lexeurs générés (le banc couvre Java, XML, JSON ; Kotlin suit la
   même voie), famille générique paramétrable pour la longue traîne.*
4. **Reprise des descriptions IntelliJ** — *recommandé : par défaut
   réécrire ; reprise fichier par fichier permise (Apache 2.0) avec
   attribution au `NOTICE`, au cas par cas, jamais en masse.*

Avec ces quatre réponses, le document de conception
(`conception-coloration.md`) peut être validé et le chantier 3 lancé.

## 6. Ce qui reste à mesurer sur appareil

La génération sur AndroidIDE/Termux (commande en §2a) ; la première
peinture colorée réelle de la zone visible ; le budget par image et
les plafonds (20 000 colonnes, dessin à 10 000) ; la taille des points
de contrôle d'état. Ces mesures ont leur place dans `docs/perf.md` dès
que le sample saura les afficher (plan : panneau de diagnostic).
