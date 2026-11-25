# État de la mission Pigma

Mise à jour : session 35 (correctif appareil), le 2026-11-25. Rester factuel et
vérifiable face à `git log` : chaque affirmation ci-dessous se
retrouve dans le dépôt ou dans un journal de build.

## Point d'arrêt

**La refonte du module :sample poussée sur `origin/main` (7ce4d5a) :
les onglets (TabRow) portent les documents de démonstration, le
drawer (DrawerPane) porte le menu (thème, hors normes, annulation,
serveurs de langage), la feuille basse (BottomSheet à trois états)
porte les mesures et le journal du relevé — les options que les
popups de la vue portaient déjà quittent l'écran (recherche,
aller-à, signature, sélection, copie du texte). Le harnais
39/39 verts, l'APK reconstruit (6,99 Mo, signé debug) et livré dans
download/.**

## Session 39 — la CI en pause, le défilement à deux axes, les points d'arrêt, le branchement Lua corrigé (2026-11-25)

0. **La CI en pause** (demande du propriétaire : le dépôt n'est pas
   encore stable) : les déclencheurs push et pull-request se
   retirent, le lancement reste manuel (workflow_dispatch) — la
   restauration se fait en redéclenchant les lignes du workflow.

0. **Le défilement horizontal** : le champ scrollX vit au côté de
   scrollY. La ligne large se révèle — le pincement agrandit le
   corps (le foyer X y demeure sous les doigts comme le foyer Y),
   le geste plat défile aux deux axes, le traîné de sélection au
   bord latéral y pousse (l'auto-scroll aux bords porte X aussi),
   les conversions point→colonne se décalent (le tap, le mot du
   double-tap, la poignée traînée), la trame de texte se
   translate (la gouttière et les cartes demeurent posées à la
   vue), la loupe suit (son rendu comme son centre). Le corps
   revenu, le défilement se reborne à zéro. Le scénario du
   harnais y éprouve le pincement, le geste, le traîné, le reborn.

0. **Les points d'arrêt de la gouttière** : l'appui long immobile
   à la colonne des marqueurs pose le point (le rond plein à
   l'accent — le style du débogueur), le re-geste le retire. Le
   tap simple reste au pli (le geste des plis ne se perd pas),
   le double-tap texte reste au mot. L'API publique breakpoints()
   expose les lignes à l'hôte (le débogueur à venir s'y
   branchera). Le scénario y éprouve pose, ordre, retrait au
   geste, hors borne.

0. **Le branchement Lua corrigé** (le serveur EmmyLua « non
   connecté ») : trois défauts au câblage, corrigés à la lecture
   du module app de Sora Editor (LGPL — les idées y viennent,
   pas le code) :

   - l'initialize ne portait NI rootUri NI workspaceFolders :
     EmmyLua s'y oriente (sa racine de travail). La liaison les
     pose maintenant (l'URI du dossier du document) ;
   - le didOpen partait PENDANT la négociation : EmmyLua refuse
     le premier document avant la poignée de main complète. Il
     part maintenant de la future complétée ;
   - la pompe didChange vivait sans garde : les frappes pendant
     la négociation partaient en didChange sans document ouvert
     (le protocole n'y connaît rien, EmmyLua perd le texte de
     vue). Le drapeau documentOpened verrouille la pompe.

   Le scénario du serveur in-process au harnais éprouve
   l'ordonnancement corrigé (44/44) ; l'exécutable EmmyLua
   lui-même (lié au linker Android) ne se lance qu'à l'appareil —
   le prochain run sur matériel dira la liaison vivante.

Harnais 44/44 scénario(s) vert(s) (défilement à deux axes et
points d'arrêt neufs), tests des cinq modules verts, politique de
langue OK (149 fichiers), APK rebâti (8,2 Mo).

## Session 38 — la CI rouge, la copie des logs, les deux crashs du journal de l'appareil (2026-11-25)

0. **La CI réparée** : le premier run GitHub Actions échouait à
   l'action SDK Android (son sdkmanager --install "tools" casse :
   le paquet n'existe plus au dépôt). Le workflow s'appuie
   maintenant sur le SDK préinstallé du runner (la plateforme 35
   s'y installe par sdkmanager), le cache Gradle se peuple avant
   le harnais (:lsp:compileJava d'abord — les jars lsp4j doivent
   exister au premier run d'une machine neuve).

0. **La copie des logs** (demande du propriétaire) : le dialogue
   des plantages porte un bouton « Copier les logs » — la pile au
   complet part au presse-papiers d'un clic (toast de
   confirmation, la taille y dit combien part) et le journal
   s'efface (le plantage copié est consommé). Le bouton du menu
   « Journal des crashs » y gagne la même copie (l'effacement
   reste au bouton sien).

0. **Les deux crashs du journal de l'utilisateur** (log.txt, la
   demande dit sa valeur — deux piles réelles de l'appareil) :

   - NullPointerException à askCodeActions (LspEditor) : le
     serveur mort rend la future échouée, la réponse null, la
     boucle des correctifs levait sur le fil principal. La garde
     failure/answer-null y court ; le scénario 39 du harnais
     reproduit le serveur mort (canaux fermés sous la façade,
     sans débranchement) et prouve que l'écran survit.

   - IndexOutOfBoundsException à drawServerHighlights
     (EditorView/TextBuffer) : les positions du serveur datent
     du texte qu'il a vu — raccourcies depuis, la colonne
     dépassait la ligne. La conversion se borne au texte vivant
     (offsetOfPositionClamped : ligne hors tampon ignorée,
     colonne bornée à la fin de ligne) ; applyTextEdits suit le
     même bornage (un correctif aux bornes périmées s'applique
     borné ou s'ignore). Le scénario y pose les positions
     hostiles (colonnes 999 999, lignes 999 999, colonne
     négative) : le dessin ne lève plus.

0. **P0-1 du rapport, vérifié déjà au dépôt** : le verrou
   editLock du tampon (replace + snapshot d'un seul tenant) et
   la révision volatile sont en place, le test de stress
   ConcurrentBufferTest aussi — la course de données du rapport
   est déjà couverte ; rien à reprendre.

Harnais 42/42 scénarios verts (le scénario 39 « positions
hostiles » neuf), tests des cinq modules verts, politique de
langue OK (149 fichiers), APK rebâti (8,2 Mo).
   Suite de la CI (runs 2 à 6) : le SDK du runner par sdkmanager
   (l'action setup-android cassait au paquet tools disparu —
   run 2) ; JFlex par le zip de la release GitHub (le jar nu
   n'existe ni aux assets ni à l'URL Maven devinée — run 3 ; le
   zip porte lib/jflex-full-1.9.1.jar, le jar OMBRAGÉ : jflex.Main
   et le runtime CUP en un seul jar) ; les cinq lexeurs du dépôt
   RÉGÉNÉRÉS par ce jar canonique (le code versionné datait d'une
   build 1.9.1 différente — mêmes en-têtes, tables DFA
   divergentes ; tests syntax/editor verts sur le régénéré) ; le
   chemin TOOLS par GITHUB_ENV (le contexte env.HOME d'étape était
   vide — run 5). Au run 6, tout le pipeline passe sauf le secret
   PIGMA_FORBIDDEN_REFS (absent du dépôt GitHub — le propriétaire
   seul le pose, la démarche vit au PORTAGE) ; sans lui, le mode
   exigeant échoue à dessein.


## Session 37 — le journal des crashs, les chantiers 5 à 8 du rapport (2026-11-25)

0. **Le journal des crashs** (demande du propriétaire) : un
   plantage sur appareil s'écrit désormais AVANT la fermeture —
   CrashJournal installe le gestionnaire d'exceptions non
   interceptées en tête d'écran, la pile complète, le fil, la
   section courante et l'appareil s'y consignent (8 plantages
   gardés, le fichier tourne). La section suit l'écran à chaque
   changement (onglet, gros fichier, LSP branché/débranché,
   journal). À la réouverture, les plantages non consommés
   s'affichent (boîte de dialogue, copie possible) ; le bouton
   « Journal des crashs » du menu les relit et les efface.
   Le scénario du harnais y éprouve la capture de bout en bout
   (un plantage lancé sur le vrai gestionnaire, la relecture,
   l'effacement) — l'utilisateur garde la pile pour l'analyse.

0. **Chantier 5 du rapport** : LICENSE (MIT) posé ; NOTICE
   complété — le binaire EmmyLua embarqué y est consigné (origine
   l'analyseur Rust, licence MIT vérifiée au dépôt, empreintes
   SHA-256 des deux architectures) ; les cinq scripts de migration
   à usage unique déménagent vers scripts/archive/.

0. **Chantier 6 du rapport** : la CI GitHub Actions (.github/
   workflows/check.yml — JDK 17, SDK Android, JFlex téléchargé aux
   versions épinglées) déroule ./gradlew check au complet. Le
   harnais d'ouverture s'y branche (tâche checkOpenHarness) ;
   les contrôles maison y deviennent exigeants : sans motif de
   références interdites (secret PIGMA_FORBIDDEN_REFS) ou sans
   JFlex, la machine de build échoue au lieu de s'ignorer.

0. **Chantier 7 du rapport** : la sauvegarde d'état — la rotation
   ne perd plus rien. EditorView écrit son paquet (curseur, ancre,
   défilement), l'écran le sien (onglet, texte) ; la restauration
   borne tout (un état corrompu ne lève jamais). Le scénario
   d'état du harnais (n° 41) prouve le retour du texte tapé, du
   curseur et du défilement, puis l'innocuité d'un état hostile.
   L'accessibilité de base : l'éditeur, la barre et le statut
   portent leur description de contenu.

0. **Chantier 8 du rapport (partie)** : Gson 2.15.0 épinglé au
   catalogue (l'intervalle de lsp4j ne décide plus seul), la
   dépendance explicite au module :lsp l'y fixe.

41/41 scénarios verts au harnais, tests des cinq modules verts,
politique de langue OK (149 fichiers), APK rebâti (8,2 Mo).

## Session 36 — le force close d'ouverture et les priorités du rapport (2026-11-25)

0. **Le force close à l'ouverture** : l'écran portait des vues de
   la bibliothèque Material (MaterialToolbar, TabLayout) sous le
   thème CADRE du manifeste — ThemeEnforcement de la bibliothèque
   lève à la construction de la première vue. Le thème passe à
   Material3 (res/values/themes.xml, l'attribut du manifeste suit),
   l'icône d'ouverture du menu devient un vrai drawable du pont
   (la description d'accessibilité la suit), le comportement de la
   feuille basse se pose DANS les paramètres du panneau avant
   l'ajout au coordinateur (setBehavior — l'attachement réel du
   cadre, c'est lui qui porte la glisse et la translation).

0. **P0-2 du rapport** : useLegacyPackaging posé (jniLibs) — le
   manifeste fusionné porte extractNativeLibs=true, le serveur
   EmmyLua vivra dans nativeLibraryDir et canExecute() deviendra
   vrai (vérifié au paquet).

0. **P0-3 du rapport** : la fermeture de la liaison LSP quitte le
   fil d'interface — un exécuteur démon dédié (pigma-lsp-stop) y
   porte la poignée de main d'arrêt, la session se détache
   immédiatement côté écran, un rebranchement relance l'exécuteur.

0. **P0-1 du rapport** : la course cœur/coloration se ferme — le
   tampon porte un verrou de mutation (le remplacement entier y
   passe), le cliché du moteur s'y prend par TextBuffer.snapshot
   (texte, bornes et révision d'un seul tenant), la révision devient
   volatile. Un test de stress (ConcurrentBufferTest : 1,5 s d'un
   fil éditeur contre un fil de clichés) éprouve la cohérence.

0. **P0-4 du rapport (partie)** : la lecture de mémoire libre ne
   se paie plus à chaque image — une ancre échantillonnée (une trame
   sur 32) porte la mesure, la pire trame s'y compare.

## Session 35 — la refonte du module :sample (2026-11-25)

0. **Les vrais conteneurs du cadre** : plus AUCUN ViewGroup maison
   à l'écran — TabRow, DrawerPane et BottomSheet partent, remplacés
   par les widgets réels de la bibliothèque Material (1.12.0) :
   MaterialToolbar + TabLayout dans l'AppBarLayout au sommet,
   DrawerLayout d'androidx.drawerlayout pour le menu (le voile, le
   geste et la largeur viennent du cadre), CoordinatorLayout
   d'androidx.coordinatorlayout avec BottomSheetBehavior pour la
   feuille (états EXPANDED/HALF_EXPANDED/COLLAPSED du cadre, la
   feuille part demi-pliée à hauteur d'aperçu). La mesure et la
   disposition deviennent celles du cadre : l'écran blanc ne peut
   plus se reproduire. Le harnais gagne les stubs des classes du
   cadre utilisées (même surface, vraies signatures — vérifiées au
   javap contre l'AAR réel), le scénario 39 y éprouve les vraies
   API (getSelectedTabPosition, getTabAt, isDrawerOpen, setState).
   La compile du sample se fait contre l'AAR réel, 39/39 verts,
   politique OK, l'APK rebâti (11,5 Mo) livré.

0. **Le correctif de l'appareil (e12b36e)** : l'écran restait blanc
   sur téléphone — DrawerPane et BottomSheet portaient des onLayout
   VIDES (le harnais dimensionne la vue qu'il éprouve ; sur appareil,
   plus personne ne disposait les enfants). Les deux conteneurs
   portent la vraie mécanique : onMeasure/onLayout réels, le z-order
   par l'ordre des enfants (contenu, feuille, menu), l'interception
   du toucher par le menu ouvert, le tiroir de poignée de la feuille
   seule, la poignée-hamburger de la barre, la feuille part
   demi-pliée. Les stubs du harnais gagnent la surface du cadre
   (onMeasure, measureChild, dispatchDraw, dispatchTouchEvent,
   onInterceptTouchEvent, MeasureSpec, translationY) — 39/39 verts
   encore, l'APK rebâti et livré.

1. **La consigne du propriétaire** : « une refonte totale du module
   sample, plus modern (tablayout, drawer, bottomsheet behavior pour
   les logs, certaines options ne sont plus nécessaires pour la
   refonte puisque les popups l'ont déjà ».
2. **Trois conteneurs neufs au sample** (Android pur, aucune
   dépendance Material — l'application hôte ne déclare que la
   bibliothèque d'édition) : `TabRow` (la barre d'onglets : un
   onglet par document, l'indicateur souligné, le choix au doigt ou
   par programme), `DrawerPane` (le panneau à menu coulissant : le
   contenu à l'écran, le voile et le menu par-dessus, l'ouverture
   par le bouton), `BottomSheet` (la feuille basse : trois états —
   replié, demi-plié, ouvert — la poignée se tire d'un cran, les
   mesures et le journal y vivent).
3. **L'écran réécrit** (`MainActivity`) : les onglets ouvrent les
   documents de démonstration (Java, XML, JSON, Kotlin, Lua, SQL) ;
   le menu du drawer porte le thème, les hors normes (gros, 50k l.,
   le cycle 50k ⨯), l'annulation et les serveurs de langage ; la
   feuille porte le panneau de mesures et le bouton de copie.
4. **Les options redondantes quittent l'écran** (la règle de
   l'instrument de test : chaque commande reste à l'écran seulement
   si la vue de bibliothèque ne la porte pas) : « Rechercher » et
   « Aller à » (les popups flottantes de la vue), « Signature » (la
   carte au caret), « Tout sélectionner »/« Quitter la sélection »
   et « Copier texte » (la barre de sélection et le menu de
   débordement). L'écran ne garde que ce qu'aucune popup ne porte :
   les onglets, le menu, la feuille, l'annulation.
5. **Le harnais adapté** : les clics des boutons disparus deviennent
   les appels directs à la vue (selectAll, setSelection(0,0), le
   presse-papiers, toggleSearchPopup/toggleGotoPopup) ; un
   scénario neuf — « refonte de l'écran : onglets, drawer, feuille
   basse » : les six onglets vivent, le choix d'onglet ouvre le
   document, les boutons redondants n'existent plus, le drawer
   s'ouvre et porte le menu, la feuille monte d'un cran et porte
   les mesures, le retour à l'onglet Java rend le document de démo.
   **39/39 scénarios verts.**
6. **Le build réparé au passage** : repo.maven.apache.org refusait
   (429) — le miroir Google de Maven Central précède mavenCentral()
   dans settings.gradle.kts, les mêmes sommes de contrôle y sont
   trouvées ; les stubs du harnais gagnent removeAllViews,
   removeViewAt et onLayout (le contrat du ViewGroup du cadre).
7. **L'éprouve** : harnais **39/39**, politique **OK (139
   fichiers)**, tests **core/syntax/analysis/lsp verts**.
   **APK rebâti — 6,99 Mo, signé debug, livré dans
   download/pigma-sample-release.apk**.

## Ce qui n'a PAS pu être fait (honnêteté)

- `:editor:test` ne compile pas sur le dépôt AVANT la refonte (le
  test `RenderCacheTest` importe `jo.pigma.editor.RenderCache`,
  la classe vit dans `render/` — préexistant, sans lien avec la
  refonte du sample ; à reprendre au prochain chantier).
- Les conteneurs neufs sur appareil (le drawer au doigt, la feuille
  tirée, les onglets) attendent leur contre-épreuve de terrain —
  le harnais en processus y répond seul.
- La contrainte « aucune dépendance Material » : TabLayout,
  DrawerLayout et BottomSheetBehavior de la bibliothèque AndroidX
  exigeraient une dépendance de plus au sample — trois conteneurs
  maison en portent l'apparence et le comportement à la place.

## Le programme du relevé 22 (le cycle d'appareil) — inchangé

1. Installer l'APK reconstruit : les onglets ouvrent les documents,
   le menu du drawer porte le thème et les hors normes, la feuille
   basse porte les mesures.
2. Le renommage au doigt, les popups translucides, le pincement
   (les contre-épreuves des chantiers 33, 20, 63).
3. Le hors normes Java après compaction : Δ mémoire et temps de
   fond (la rangée du tableau).
4. « Tout remplacer » sur le hors normes (le correctif du relevé
   16 attend sa contre-épreuve).
5. Appareil : modèle et version d'Android (la ligne du tableau).

## Journal des sessions précédentes (l'essentiel, le détail dans git)

- **Session 34** (`fdd825d`) : le chantier 65 — les fonctions du
  document (symboles, indices, plis du serveur, jetons sémantiques),
  la consigne du nom.
- **Session 33** (`4fb02fa`) : le chantier 64 — le protocole étendu.
- **Session 32** (`c44d3c8`) : le chantier 63 — la façade LspEditor.
- Les chantiers 0 à 62 : la voie complète du moteur — le détail
  vit dans `docs/chantiers/` et l'historique git.
