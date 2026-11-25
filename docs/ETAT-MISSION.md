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

## Session 35 — la refonte du module :sample (2026-11-25)

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
