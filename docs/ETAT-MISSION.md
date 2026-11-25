# État de la mission Pigma

Mise à jour : session 42 (le registre partagé, multi-éditeur), le 2026-11-25. Rester factuel et
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

## Session 42 — le registre partagé, le multi-éditeur (2026-11-25)

0. **Le multi-éditeur vit** (la demande du propriétaire : « je ne
   veux plus être mono-éditeur, comme Sora et même mieux ») :
   `LspWorkspace` (module :lsp) porte le REGISTRE des serveurs d'un
   espace de travail — un serveur PAR LANGAGE, monté par le PREMIER
   éditeur attaché (la poignée de main n'y court qu'une fois), les
   suivants s'y RACCROCHENT ; les documents partent par didOpen au
   serveur vivant, les avis reviennent DISPATCHÉS PAR URI (l'avis
   du document XML n'atteint QUE l'éditeur du document — jamais le
   voisin), le DERNIER détaché ferme le serveur (la durée de vie du
   processus suit les documents ouverts, pas les écrans).

0. **La façade préservée** : `LspEditor.attachToWorkspace(registry,
   definition, uri, languageId)` raccroche la façade de la session
   40 au registre — la machine à états (ConnectionStatus, l'écouteur,
   les toasts) se transporte TELLE QUELLE, `disconnect` détache du
   registre. La voie directe (connectInProcess/connectProcess) vit
   toujours : un éditeur seul n'y change rien.

0. **Le protocole s'achève** : `LspSession.didClose(uri)` ferme le
   document au serveur (le registre le demande au dernier lecteur
   parti) — l'ombre de synchronisation s'y range, une réouverture
   repart du texte entier.

0. **L'app éprouve au doigt** : le bouton « Duo » du tiroir branche
   DEUX éditeurs (le Java de l'onglet + un document XML) au MÊME
   registre — un seul serveur de démo (multi-documents maintenant :
   table URI→texte, chaque fonction du protocole lit le document
   DEMANDÉ). Le re-tap détache tout.

0. **Les preuves** : le harnais passe à 47 scénarios (le neuf :
   registre partagé — deux éditeurs, UN serveur vivant, deux
   documents ouverts, l'avis du XML au second éditeur SEUL, le
   détachement ferme) ; le test `LspWorkspaceTest` (module :lsp)
   éprouve la même mécanique sans l'écran (2 attach, 1 poignée de
   main, dispatch par URI, dernier-parti ferme). Tests des cinq
   modules verts, politique OK (152 fichiers), APK rebâti (8,2 Mo).

0. **Deux enseignements de conception** (à retenir pour la suite) :
   un abonné tardif au cycle des serveurs doit lire l'état COURANT
   (l'événement de naissance est parti sans lui — le raccrochage
   d'un serveur déjà vivant paraissait « jamais connecté ») ; les
   fabriques du registre portent le PONT des publications (la liaison
   montée sans écouteur perdait les diagnostics — personne ne
   route au dispatch).

## Les étapes restantes du rapport (la liste à jour)

1. **Le serveur Lua réel sur l'appareil** — la panne du log du
   propriétaire (EmmyLua meurt à l'initialisation : Stream closed)
   reste à reproduire sur l'appareil ; le toast de la session 40
   (débranché + dernière ligne de stderr) dira la cause exacte ; le
   branchement Lua par PROCESSUS au REGISTRE (ofProcess) reste à
   brancher au bouton LSP Lua (la voie directe y vit aujourd'hui).
2. **La CI manuelle** (workflow_dispatch posé en session 39) — le
   secret GitHub du registre de paquets reste à poser par le
   propriétaire, le pipeline passera alors au run 7.
3. **Les mesures de performances appareil** — le panneau vit, la
   capture systématique sur l'écran physique manque.
4. **Les thèmes de l'API publique** — les six palettes vivent ; la
   doc README d'un hôte apportant SA palette reste à écrire.
5. **La persistance du registre** — aujourd'hui le registre vit en
   mémoire (un état d'app) ; le jour où l'app hôte survit à la
   rotation, le registre s'y restaurera (les serveurs relancés,
   les didOpen rejoués du texte courant).

## Session 41 — les thèmes de plus, la complétion de base sans serveur (2026-11-25)

0. **La réponse à la question d'architecture** (le propriétaire a
   demandé si notre approche peut faire « comme Sora Editor, avec
   le registre d'espace partagé ») : OUI ELLE POURRAIT — mais ce
   n'est pas ce qui manque. Le LspProject de la référence est un
   REGISTRE : définitions de serveurs par extension, processus
   PARTAGÉS entre N éditeurs, diagnostics communs. Notre
   bibliothèque mono-éditeur embarquable (un LspEditor par vue)
   n'en a pas l'usage aujourd'hui. Le jour où Pigma porte PLUSIEURS
   éditeurs sur le MÊME espace de travail, la façade se scinde en
   deux : un registre (l'espace, les serveurs, la durée de vie des
   processus) et la façade par document — l'API de la session 40
   (ConnectionStatus, écouteur, hasConnection) se transporte telle
   quelle : le registre porterait les connexions, la façade
   déléguerait. Rien à refaire, tout à brancher.

0. **Les thèmes** : quatre palettes de plus (Solaire papier chaud,
   Forêt vert profond, Océan bleu nuit, Café brun chaud — chacun
   son fond, ses portées et ses natures sémantiques propres, le
   registre SEMANTIC par thème remplace les deux cartes en dur).
   L'app porte six boutons de thème au tiroir. Aucune
   retokenisation (le scénario du harnais le prouve par les six).

0. **La complétion de base SANS serveur** (la demande explicite du
   propriétaire, à la leçon de la référence : sa liste statique
   sKeywords + les identifiants du document) :
   `LanguageKeywords` (module syntax) porte les mots-clés des six
   langages livrés (Java, XML, JSON, Kotlin, Lua avec sa
   bibliothèque standard, SQL) ; la fusion au bandeau se fait dans
   `localCompletionEntries` — les mots-clés (KIND_KEYWORD, 12
   max) d'abord, l'index des symboles ensuite. Le Lua sans
   serveur propose « local » au préfixe « loc » — le harnais le
   vérifie.

0. **Le garde des gestes** (la panne du harnais après la fusion) :
   le bandeau ne vit NI pendant un geste de sélection (le tap
   suivant devait atteindre le texte — la carte interceptait le
   DOWN : le double-tap mourait, la loupe n'escamotait plus rien)
   NI sur une sélection vivante (le mot n'y se tape pas).

0. **Le harnais passe à 46 scénarios** (le scénario neuf :
   complétion sans serveur Java et Lua, bandeau vidé au retour),
   tests des cinq modules verts, politique OK (150 fichiers), le
   push du commit c7545c7 (le token du propriétaire, credential
   store posé — le prochain recyclage du bac n'y perdra plus
   l'accès).

## Les étapes restantes du rapport (la liste à jour)

1. **Le serveur Lua réel sur l'appareil** — la panne du log du
   propriétaire (le processus EmmyLua meurt juste après
   l'initialisation : Stream closed) reste à reproduire sur
   l'appareil ; le toast de la session 40 (débranché + la
   dernière ligne de stderr) dira la cause exacte ; les correctifs
   candidats (rootUri propre, capacités client réelles) sont déjà
   poussés — la vérification demandera un nouveau log appareil.
2. **La CI manuelle** (workflow_dispatch posé en session 39) — le
   secret GitHub du registre de paquets reste à poser par le
   propriétaire, le pipeline passera alors au run 7.
3. **Les mesures de performances** — le rendu des 52 004 lignes
   tient (2,6 ms d'ouverture au harnais JVM) ; reste la mesure
   appareil réelle (le panneau de mesures vit, la capture
   systématique sur l'écran physique manque).
4. **Les thèmes de l'API publique** — les six palettes vivent ;
   un hôte tiers doit pouvoir apporter la sienne (l'API
   applyTheme l'accepte, la doc du README reste à écrire).
5. **Le registre multi-serveurs** — le chantier conditionné du
   point 0 ci-dessus : le jour où l'app hôte ouvre plusieurs
   éditeurs sur le même espace de travail.

## Session 40 — la connexion LSP gérée proprement, l'état observable, le toast (2026-11-25)

0. **Le verdict sur LspProject** (question du propriétaire : notre
   approche sans classe LspProject est-elle en faute ?) : NON — la
   référence s'en sert de REGISTRE D'ESPACE DE TRAVAIL
   (définitions de serveurs par extension, wrappers de processus
   PARTAGÉS entre N éditeurs, conteneur de diagnostics commun,
   fermer un éditeur sans tuer le serveur). Notre bibliothèque
   mono-éditeur embarquable n'en a pas l'usage. Ce qui NOUS
   manquait est ailleurs : la machine à états de leur LspEditor
   (CONNECTING/CONNECTED/DISCONNECTED + onStatusChanged) et la
   détection de la mort du canal de leur wrapper. Les deux y sont
   portées.

0. **Le nouveau log du propriétaire dit la panne exacte** : le
   handshake RÉUSSIT (initialize répond !) puis le processus
   EmmyLua MEURT juste après (« Stream closed » sur le
   initialized), et les didChange fuient dans le canal mort
   pendant 30 secondes — aucune détection, aucun état observable.
   Trois causes corrigées :

   - la racine envoyée hier portait « /.. » (l'URI document
     amputée naïvement) : la racine est maintenant le DOSSIER
     du document, une URI propre (la référence pose l'URI du
     projet tout droit) ;
   - le ClientCapabilities VIDE : les capacités RÉELLES du
     client se déclarent maintenant (synchronisation, survol,
     complétion, signature, diagnostics, surlignage, renommage,
     aller-à, références, formatage, inlays, actions — le
     miroir de ce que la façade sait) ;
   - la mort du canal restait invisible : le lecteur lsp4j se
     surveille (sa future finit), l'écouteur onStopped y naît.

0. **La machine à états de la façade** : ConnectionStatus
   (CONNECTING/CONNECTED/DISCONNECTED) + setConnectionListener
   + connectionStatus() + hasConnection() (une liaison posée,
   vivante OU morte — le bouton détache une morte avant de
   rebrancher). isConnected() lit l'état, plus la présence.
   Les envois s'arrêtent sur la liaison morte (la pompe
   didChange se tait — plus de fuite de 30 secondes).

0. **Le toast demandé** : « connexion au serveur… », « serveur
   connecté ✔ », « serveur déconnecté — {raison} » (la stderr
   d'EmmyLua y complète : sa dernière ligne dit la cause exacte
   de la mort — l'écran du propriétaire dira ENFIN pourquoi le
   serveur Lua meurt). Les boutons suivent l'état réel.

0. **Le scénario harnais du cycle** : naissance dite, vie,
   mort portée à l'écouteur, envois arrêtés, débranchement d'une
   liaison morte muet. Le CrashJournal crée son répertoire à
   l'installation (un stockage recyclé ne perd plus les plantages
   en silence).

Harnais 45/45 scénario(s) vert(s) (le cycle de connexion neuf),
tests des cinq modules verts, politique OK (149 fichiers), APK
rebâti (8,2 Mo). Le dépôt a été restauré par re-clone (le bac a
été recyclé) ; le credential de push a disparu avec lui — le
commit reste local tant que le propriétaire n'a pas reposé
l'accès en écriture.

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
