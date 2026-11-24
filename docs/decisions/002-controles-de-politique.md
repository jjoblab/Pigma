# Décision 002 — contrôles automatiques de politique

## Contexte

La mission exige des garde-fous légers, exécutables sur téléphone
(Termux / AndroidIDE), qui échouent en cas de violation : langue des
identifiants, propreté des commentaires, mots français dans les noms,
Javadoc des types publics.

## Choix

Un script Python 3 sans dépendance, `scripts/check-code-policy`, branché
dans `./gradlew check` (tâche `checkCodePolicy`) et dans le crochet
`scripts/hooks/pre-commit`. Il vérifie :

1. **ASCII des identifiants** : un mini-lexeur sépare code, commentaires
   et littéraux ; tout caractère non ASCII dans le code échoue (les
   commentaires français et les chaînes peuvent être accentués) ;
2. **motifs interdits dans les commentaires** : numéros de décision
   (ADR), codes de suivi, jalons `M\d`, dates ISO, mentions de pilotage
   (« propriétaire », « audit », « revue », « chantier », « session »,
   « étape »), dépôt d'origine, mentions de portage, historique. Le
   pattern du dépôt d'origine vit dans le script en tant que donnée de
   contrôle, exigé tel quel par la mission ;
3. **mots français dans les identifiants** : liste maintenue à la main,
   `scripts/french-words.txt`, uniquement des mots sans homonyme anglais
   courant ;
4. **Javadoc obligatoire** pour tout type public.

Le script a un mode `--selftest` : les fixtures
`scripts/policy-fixtures/Violations.java` contiennent volontairement une
violation de chaque catégorie ; le banc échoue si une catégorie n'est pas
détectée. Les fixtures et le code généré sont exclus du balayage normal.

Le contrôle des références interdites est séparé :
`scripts/check-forbidden-refs` lit ses motifs dans l'environnement
(variable `PIGMA_FORBIDDEN_REFS` ou fichier non versionné
`config/forbidden-refs.local`) — les noms des deux projets interdits ne
figurent jamais dans le dépôt.

## Conséquences

- collision connue, à trancher avec le propriétaire le jour où elle se
  présente : « session » et « étape » sont aussi des mots du domaine
  d'un éditeur (session LSP, étape de lexage). La mission interdit ces
  mots dans les commentaires ; si le domaine les exige vraiment, il
  faudra une exception explicite, pas un retrait silencieux ;
- le retrait de la liste `french-words.txt` de tout mot provoquant un
  faux positif doit rester une décision motivée (le fichier porte les
  règles de curation en en-tête).
