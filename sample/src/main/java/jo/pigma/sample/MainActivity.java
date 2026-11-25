package jo.pigma.sample;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jo.pigma.editor.EditorThemes;
import jo.pigma.editor.EditorView;
import jo.pigma.editor.lsp.LspEditor;
import jo.pigma.editor.popups.PopupStyle;
import jo.pigma.editor.popups.QuickActionCard;
import jo.pigma.core.TextBuffer;
import jo.pigma.core.TextListener;
import jo.pigma.core.TextPosition;
import jo.pigma.core.UndoStack;
import jo.pigma.syntax.LanguageSyntax;
import jo.pigma.syntax.Languages;
import jo.pigma.syntax.StructureDocument;
import jo.pigma.syntax.SyntaxDocument;
import jo.pigma.syntax.SyntaxTheme;

/**
 * Écran principal refondu : l'éditeur plein écran, une barre d'onglets
 * au-dessus (un onglet par document de démonstration), un menu coulissant
 * à gauche (le drawer : thème, serveurs de langage, hors normes) et une
 * feuille basse (le journal des mesures, à tirer par sa poignée).
 *
 * <p>La refonte suit la règle de l'instrument de test : chaque commande
 * reste à l'écran SEULEMENT si la vue de bibliothèque ne la porte pas
 * déjà. La recherche et l'aller-à-la-ligne vivent en popups flottantes
 * de l'éditeur (la barre de sélection ouvre la recherche par sa pastille,
 * le menu de débordement porte les deux) ; la carte de signature vit au
 * caret ; copier et sélectionner vivent à la barre de sélection. L'écran
 * ne garde que ce qu'aucune popup ne peut porter : les onglets, le menu,
 * la feuille des mesures, l'annulation.</p>
 *
 * <p>La feuille basse garde le panneau de mesures du moteur (première
 * coloration, coût de demande, coût de frappe, lignes lexées, trame) et
 * le journal du scénario — le bouton de copie y vit, le relevé appareil
 * de {@code docs/perf.md} se colle depuis la feuille ouverte.</p>
 */
public final class MainActivity extends Activity {

    /** Délai entre deux rafraîchissements du panneau pendant le remplissage. */
    private static final long DIAGNOSTIC_WATCH_MS = 200L;

    /** Lignes du journal au-delà desquelles les plus anciennes tombent. */
    private static final int LOG_LIMIT = 24;

    /** Les étiquettes des onglets de documents. */
    private static final String[] TAB_LABELS =
            {"Java", "XML", "JSON", "Kotlin", "Lua", "SQL"};

    /** L'onglet choisi et son texte : l'état sauvegardé s'y pose. */
    private static final String STATE_TAB = "pigma.tab";
    private static final String STATE_TEXT = "pigma.text";

    @Override
    protected void onSaveInstanceState(android.os.Bundle outState) {
        super.onSaveInstanceState(outState);
        // l'onglet et le texte : la rotation ne perd plus le
        // document à l'écran (le scénario suit à la réouverture)
        outState.putInt(STATE_TAB, selectedTabIndex());
        outState.putString(STATE_TEXT, editorView.getBuffer().toString());
    }

    /**
     * La restauration : l'onglet et le texte reviennent (l'écran
     * se reconstruit sinon au document de démonstration par défaut,
     * un comportement valide — le retour d'état prime).
     *
     * @param savedInstanceState l'état sauvé, null à la création pure
     */
    @Override
    protected void onRestoreInstanceState(android.os.Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState == null) {
            return;
        }
        int tab = savedInstanceState.getInt(STATE_TAB, 0);
        String text = savedInstanceState.getString(STATE_TEXT);
        if (text != null) {
            // le texte revient comme un contenu posé (pas une
            // édition à défaire : l'histoire repart vide)
            editorView.setLanguage(languageOf(TAB_LABELS[tab]), text);
            editorView.getUndoStack().clear();
            currentScenario = TAB_LABELS[tab] + " (démo)";
            CrashJournal.setSection(currentScenario);
            loggedFirstColoration = false;
            updateStatus();
            watchDiagnostics();
        }
    }

    /**
     * L'accessibilité de base : l'éditeur dessiné au canevas est
     * invisible au lecteur d'écran sans description — la vue y
     * pose la sienne (le rôle, le contenu, le curseur), les
     * boutons portent leur libellé (le cadre les y lit déjà).
     */
    private void setupAccessibility() {
        editorView.setContentDescription("Éditeur de code Pigma");
        toolbar.setNavigationContentDescription("Ouvrir le menu");
        statusView.setContentDescription("Statut de l'éditeur");
    }

    private EditorView editorView;
    private TextView statusView;
    private TextView diagnosticsView;
    private TabLayout tabLayout;
    private com.google.android.material.appbar.AppBarLayout appBar;
    private com.google.android.material.appbar.MaterialToolbar toolbar;
    private androidx.drawerlayout.widget.DrawerLayout drawerLayout;
    private LinearLayout drawerMenu;
    private BottomSheetBehavior<LinearLayout> sheetBehavior;
    private Button undoButton;
    private Button redoButton;
    private Button copyButton;

    /** Le bouton LSP : branche (et débranche) le serveur de démonstration. */
    private Button lspButton;

    /** Le bouton LSP Lua : branche le serveur EmmyLua réel par processus. */
    private Button lspLuaButton;

    /**
     * La façade du serveur de langage : l'ÉCRAN ne câble plus rien —
     * la bibliothèque porte la synchronisation du document, les
     * fournisseurs (survol, complétion, signature, diagnostics) et
     * leurs caches.
     */
    private LspEditor lsp;

    /** L'étiquette du serveur branché (la feuille la lit). */
    private String lspLabel;

    /** Le document de la démo Java (l'URI stable du protocole). */
    private static final String DOC_URI = "file:///demo.java";

    private final List<String> scenarioLog = new ArrayList<>();
    private String currentScenario = "Java (démo)";
    private boolean loggedFirstColoration;
    private boolean filling;

    private final TextListener statusListener = new TextListener() {
        @Override
        public void textChanged(TextBuffer buffer, int start, int removedLength,
                                int insertedLength, int revision) {
            // un orage d'éditions (« tout remplacer » : dix mille
            // remplacements d'un coup) ne doit pas payer dix mille
            // rafraîchissements de feuille — le rafraîchissement se
            // reprogramme, la file ne le porte qu'une fois après
            // l'orage ; la frappe ordinaire ne voit pas la différence
            // (il suit la trame). Le document vers le serveur vit
            // dans la façade (elle se programme toute seule).
            editorView.removeCallbacks(statusSync);
            editorView.post(statusSync);
        }
    };

    /** Le rafraîchissement coalescé de la feuille (un par salve d'éditions). */
    private final Runnable statusSync = this::runStatusSync;

    /** La feuille se rafraîchit : statut, mesures, veilleur. */
    private void runStatusSync() {
        updateStatus();
        watchDiagnostics();
    }

    /**
     * Le veilleur de la feuille : tant que le fil de fond avance, il
     * rafraîchit les mesures (lignes lexées qui montent) ; la fin du
     * remplissage s'inscrit au journal avec sa montre.
     */
    private final Runnable diagnosticsWatcher = new Runnable() {
        @Override
        public void run() {
            updateDiagnostics();
            if (editorView.getSyntaxDocument().remainingLines() > 0) {
                filling = true;
                editorView.postDelayed(this, DIAGNOSTIC_WATCH_MS);
            } else if (filling) {
                filling = false;
                logScenario(String.format(Locale.ROOT,
                        "remplissage fini - %s (fond %d ms, %d tranche(s))",
                        currentScenario,
                        editorView.backgroundFillNanos() / 1_000_000L,
                        editorView.backgroundFillSlices()));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // le filet des crashs d'ABORD : un plantage pendant la
        // construction de l'écran s'écrit au journal (la pile, le
        // fil, la section « ouverture ») avant la fermeture —
        // l'installation ne lève jamais, la section courante suit
        CrashJournal.install(getFilesDir());
        CrashJournal.setSection("ouverture");
        editorView = new EditorView(this);
        statusView = new TextView(this);
        diagnosticsView = new TextView(this);
        diagnosticsView.setTypeface(Typeface.MONOSPACE);
        diagnosticsView.setTextSize(11.0f);
        undoButton = new Button(this);
        redoButton = new Button(this);
        undoButton.setText("Annuler");
        redoButton.setText("Rétablir");
        undoButton.setOnClickListener(this::onUndoClicked);
        redoButton.setOnClickListener(this::onRedoClicked);
        copyButton = new Button(this);
        copyButton.setText("Copier les mesures");
        copyButton.setOnClickListener(this::onCopyClicked);
        lspButton = new Button(this);
        lspButton.setText("LSP");
        lspButton.setOnClickListener(this::onLspToggleClicked);
        lspLuaButton = new Button(this);
        lspLuaButton.setText("LSP Lua");
        lspLuaButton.setOnClickListener(this::onLspLuaClicked);

        setContentView(buildLayout());
        // l'accessibilité de base : l'éditeur dessiné au canevas y
        // gagne sa description (le lecteur d'écran le lit)
        setupAccessibility();
        editorView.getBuffer().addListener(statusListener);
        // la vue de bibliothèque démarre vide : l'hôte charge son
        // contenu — ici la démo Java (le chargement n'est pas une
        // édition à défaire, l'histoire repart vide)
        editorView.setLanguage(Languages.JAVA, DemoTexts.JAVA);
        // le style des popups : l'hôte pose le sien — fond translucide
        // un peu plus léger, coins plus ronds, largeur accrue (l'API
        // de la bibliothèque, tout y se règle)
        editorView.setPopupStyle(PopupStyle.builder()
                .backgroundArgb(0xD9F6F8FA)
                .borderArgb(0xA8B5B5B5)
                .cornerRadiusDp(20)
                .minCardWidthDp(400)
                .maxCardWidthDp(500)
                .build());
        // LE CÂBLAGE DU SERVEUR DE LANGAGE VIT DANS LA FAÇADE : deux
        // lignes d'écran — l'attache (l'écouteur de la feuille) et le
        // branchement (au clic du menu). Tout ce qui suit n'est plus
        // de l'écran : la synchronisation du document, le survol, la
        // complétion, la signature, les diagnostics à la carte — la
        // bibliothèque le porte.
        lsp = LspEditor.attach(editorView);
        lsp.setDiagnosticsListener(fresh -> runOnUiThread(() -> {
            updateDiagnostics();
            editorView.invalidate();
        }));
        // les intentions de l'hôte : l'ampoule jaune de la gouttière
        // les porte avec celles de l'éditeur
        editorView.setQuickActionProvider(this::hostQuickActions);
        editorView.getUndoStack().clear();
        updateStatus();
        updateDiagnostics();
        // la première coloration est paresseuse (au premier dessin) et
        // le remplissage avance par tranches : la feuille suit
        editorView.post(this::watchDiagnostics);
        // les plantages d'avant (un force close s'est écrit au
        // journal) s'annoncent à la réouverture : l'utilisateur les
        // voit, les copie, les efface — le diagnostic part avec eux
        showPendingCrashes();
    }

    /**
     * Les plantages non consommés s'affichent : une boîte de
     * dialogue par plantage du journal (la pile complète, la
     * section où il est arrivé). L'utilisateur copie le texte (le
     * diagnostic en a besoin) ou l'efface ; les plantages affichés
     * sont consommés — la prochaine ouverture ne les remontre pas.
     */
    private void showPendingCrashes() {
        java.util.List<String> crashes =
                CrashJournal.pendingCrashes(getFilesDir());
        if (crashes.isEmpty()) {
            return;
        }
        for (int i = 0; i < crashes.size(); i++) {
            String crash = crashes.get(i);
            String body = crash + "\n(section : "
                    + sectionOfCrash(crash) + ")";
            android.app.AlertDialog.Builder dialog =
                    new android.app.AlertDialog.Builder(this);
            dialog.setTitle("Plantage " + (i + 1) + " sur "
                    + crashes.size() + " — copiez-le, il dit la cause");
            // le message du dialogue : le plantage au complet, la
            // sélection y reste possible (copier au long-pression)
            android.widget.TextView crashView = new android.widget.TextView(this);
            crashView.setTypeface(Typeface.MONOSPACE);
            crashView.setTextSize(11.0f);
            crashView.setText(body);
            crashView.setPadding(dp(12), dp(8), dp(12), dp(8));
            dialog.setView(crashView);
            dialog.setPositiveButton("Effacer", (which, whichButton) ->
                    CrashJournal.clear(getFilesDir()));
            dialog.setNeutralButton("Garder", (which, whichButton) -> {
            });
            dialog.show();
        }
    }

    /**
     * La section d'un plantage écrit : la ligne « section : … » du
     * journal s'y lit (le champ va avec la pile).
     *
     * @param crash l'entrée du journal
     * @return la section citée, « ? » si absente
     */
    private static String sectionOfCrash(String crash) {
        for (String line : crash.split("\n")) {
            if (line.startsWith("section : ")) {
                return line.substring("section : ".length());
            }
        }
        return "?";
    }

    /**
     * Le choix d'onglet : le document de démonstration correspondant
     * s'ouvre (le scénario courant et le journal suivent).
     *
     * @param index l'indice de l'onglet choisi
     */
    private void onDocumentTabSelected(int index) {
        openDocument(TAB_LABELS[index]);
    }

    /** Ouvre le document de l'onglet courant (le harnais y repasse). */
    void harnessReopenCurrent() {
        openDocument(TAB_LABELS[java.lang.Math.max(0, selectedTabIndex())]);
    }

    /** L'indice de l'onglet choisi (0 à la première ouverture). */
    int selectedTabIndex() {
        int selected = tabLayout.getSelectedTabPosition();
        return selected < 0 ? 0 : selected;
    }

    /** Choisit l'onglet d'indice donné (le harnais y passe). */
    void harnessSelectTab(int index) {
        TabLayout.Tab tab = tabLayout.getTabAt(index);
        if (tab != null) {
            tab.select();
        }
    }

    /**
     * Ouvre le document de démonstration d'un langage.
     *
     * @param name le nom du langage (Java, XML, JSON, Kotlin, Lua, SQL)
     */
    private void openDocument(String name) {
        editorView.setLanguage(languageOf(name), textOf(name));
        currentScenario = name + " (démo)";
        CrashJournal.setSection(currentScenario);
        loggedFirstColoration = false;
        updateStatus();
        watchDiagnostics();
    }

    /**
     * Le texte de démonstration d'un langage.
     *
     * @param name le nom du langage
     * @return le texte de démonstration
     */
    private String textOf(String name) {
        switch (name) {
            case "XML":
                return DemoTexts.XML;
            case "JSON":
                return DemoTexts.JSON;
            case "Kotlin":
                return DemoTexts.KOTLIN;
            case "Lua":
                return DemoTexts.LUA;
            case "SQL":
                return DemoTexts.SQL;
            default:
                return DemoTexts.JAVA;
        }
    }

    /**
     * Les intentions de l'hôte à une ligne (l'ampoule les porte) : la
     * date du jour au curseur, l'ouverture de la recherche.
     *
     * @param line   l'indice de la ligne
     * @param column la colonne du curseur
     * @return les actions de l'hôte, jamais null
     */
    private List<QuickActionCard.Entry> hostQuickActions(int line, int column) {
        return List.of(
                new QuickActionCard.Entry("Insérer la date du jour", this::insertToday),
                new QuickActionCard.Entry("Ouvrir la recherche", editorView::toggleSearchPopup));
    }

    /**
     * L'intention de date : la date du jour se pose au curseur (une
     * unité d'annulation).
     */
    private void insertToday() {
        editorView.insertAtCaret(java.time.LocalDate.now().toString());
    }

    /**
     * La mise en page des vrais conteneurs du cadre : l'app-bar
     * (Toolbar + TabLayout) au sommet du contenu, l'éditeur plein
     * écran dessous, la feuille basse (BottomSheetBehavior du
     * CoordinatorLayout) par-dessus, le menu coulissant dans le
     * DrawerLayout — la mesure et la disposition y sont celles du
     * cadre, aucune géométrie maison.
     *
     * @return la racine du contenu
     */
    private View buildLayout() {
        // l'app-bar : la barre du menu (l'icône d'ouverture du
        // drawer y vit, la navigation y répond) au-dessus des onglets
        toolbar = new com.google.android.material.appbar.MaterialToolbar(this);
        toolbar.setTitle("Pigma");
        // l'icône d'ouverture du menu : le glyphe standard du
        // cadre (l'hamburger), la barre le porte au premier plan
        toolbar.setNavigationIcon(getResources().getDrawable(
                androidx.appcompat.R.drawable.abc_ic_ab_back_material, getTheme()));
        toolbar.setNavigationContentDescription("Ouvrir le menu");
        toolbar.setNavigationOnClickListener(view ->
                openCloseDrawer());
        tabLayout = new TabLayout(this);
        for (String label : TAB_LABELS) {
            tabLayout.addTab(tabLayout.newTab().setText(label));
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                onDocumentTabSelected(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                onDocumentTabSelected(tab.getPosition());
            }
        });
        appBar = new com.google.android.material.appbar.AppBarLayout(this);
        appBar.addView(toolbar, new com.google.android.material.appbar.AppBarLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        appBar.addView(tabLayout, new com.google.android.material.appbar.AppBarLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // le contenu : l'app-bar au sommet, l'éditeur plein écran
        // dessous, le statut au bas
        LinearLayout contentColumn = new LinearLayout(this);
        contentColumn.setOrientation(LinearLayout.VERTICAL);
        contentColumn.addView(appBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams editorParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        contentColumn.addView(editorView, editorParams);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = dp(8);
        statusParams.setMargins(dp(16), 0, margin, margin);
        contentColumn.addView(statusView, statusParams);

        // la feuille basse : les mesures et le journal y vivent
        LinearLayout sheetColumn = new LinearLayout(this);
        sheetColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams measuresParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        measuresParams.setMargins(dp(16), dp(12), margin, 0);
        sheetColumn.addView(diagnosticsView, measuresParams);
        sheetColumn.addView(row(margin, copyButton));

        // le coordinateur porte le contenu et la feuille : le
        // comportement de feuille se pose DANS les paramètres du
        // panneau avant l'ajout (l'attachement du cadre — c'est lui
        // qui porte la glisse et la translation sur appareil), la
        // feuille vit par-dessus l'éditeur
        androidx.coordinatorlayout.widget.CoordinatorLayout coordinator =
                new androidx.coordinatorlayout.widget.CoordinatorLayout(this);
        coordinator.addView(contentColumn, new androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams sheetParams =
                new androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sheetBehavior = new com.google.android.material.bottomsheet.BottomSheetBehavior<>();
        sheetParams.setBehavior(sheetBehavior);
        coordinator.addView(sheetColumn, sheetParams);
        sheetBehavior.setPeekHeight(dp(120));
        sheetBehavior.setSkipCollapsed(false);
        // la feuille part demi-pliée : les mesures se lisent de
        // plain-pied à l'ouverture (le replié ne montre que la
        // hauteur d'aperçu)
        sheetBehavior.setState(
                com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED);

        // le menu coulissant : thème, hors normes, serveurs de langage
        LinearLayout menuColumn = new LinearLayout(this);
        menuColumn.setOrientation(LinearLayout.VERTICAL);
        menuColumn.addView(row(margin,
                themeButton("Clair", EditorThemes.LIGHT, EditorThemes.LIGHT_BACKGROUND),
                themeButton("Sombre", EditorThemes.DARK, EditorThemes.DARK_BACKGROUND)));
        menuColumn.addView(row(margin,
                languageButton("Gros", Languages.JAVA, DemoTexts.bigFile()),
                languageButton("50k l.", Languages.JAVA, DemoTexts.hugeFile()),
                hugeCycleButton()));
        menuColumn.addView(row(margin, undoButton, redoButton));
        menuColumn.addView(row(margin, lspButton, lspLuaButton));
        menuColumn.addView(row(margin, journalButton()));

        // le drawer : le contenu et la feuille dessous, le menu par-
        // dessus depuis la gauche (le cadre porte le voile, le geste
        // et la largeur)
        drawerLayout = new androidx.drawerlayout.widget.DrawerLayout(this);
        drawerMenu = menuColumn;
        androidx.drawerlayout.widget.DrawerLayout.LayoutParams contentParams =
                new androidx.drawerlayout.widget.DrawerLayout.LayoutParams(
                        androidx.drawerlayout.widget.DrawerLayout.LayoutParams.MATCH_PARENT,
                        androidx.drawerlayout.widget.DrawerLayout.LayoutParams.MATCH_PARENT);
        drawerLayout.addView(coordinator, contentParams);
        androidx.drawerlayout.widget.DrawerLayout.LayoutParams menuParams =
                new androidx.drawerlayout.widget.DrawerLayout.LayoutParams(
                        dp(280),
                        androidx.drawerlayout.widget.DrawerLayout.LayoutParams.MATCH_PARENT,
                        Gravity.LEFT);
        drawerLayout.addView(menuColumn, menuParams);
        return drawerLayout;
    }

    /**
     * Le drawer s'ouvre ou se referme (la barre du menu y mène).
     */
    private void openCloseDrawer() {
        if (drawerLayout.isDrawerOpen(drawerMenu)) {
            drawerLayout.closeDrawer(drawerMenu);
        } else {
            drawerLayout.openDrawer(drawerMenu);
        }
    }

    private LinearLayout row(int margin, Button... buttons) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (Button button : buttons) {
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            row.addView(button, params);
        }
        return row;
    }

    /**
     * Le bouton du journal des crashs : les plantages capturés s'y
     * relisent à la demande (la pile au complet, la section où
     * chacun est arrivé) et s'effacent d'un clic — la consultation
     * manuelle complète l'annonce automatique à la réouverture.
     *
     * @return le bouton, posé au menu
     */
    private Button journalButton() {
        Button button = new Button(this);
        button.setText("Journal des crashs");
        button.setOnClickListener(view -> {
            java.util.List<String> crashes =
                    CrashJournal.pendingCrashes(getFilesDir());
            CrashJournal.setSection("journal des crashs");
            if (crashes.isEmpty()) {
                Toast.makeText(this, "aucun plantage au journal — "
                        + "la section courante est « "
                        + CrashJournal.currentSection() + " »",
                        Toast.LENGTH_LONG).show();
                return;
            }
            StringBuilder body = new StringBuilder();
            for (String crash : crashes) {
                body.append(crash.trim()).append("\n\n");
            }
            android.app.AlertDialog.Builder dialog =
                    new android.app.AlertDialog.Builder(this);
            dialog.setTitle(crashes.size() + " plantage(s) capturé(s)");
            android.widget.TextView crashView = new android.widget.TextView(this);
            crashView.setTypeface(Typeface.MONOSPACE);
            crashView.setTextSize(11.0f);
            crashView.setText(body.toString().trim());
            crashView.setPadding(dp(12), dp(8), dp(12), dp(8));
            dialog.setView(crashView);
            dialog.setPositiveButton("Effacer", (which, whichButton) -> {
                CrashJournal.clear(getFilesDir());
                Toast.makeText(this, "journal effacé", Toast.LENGTH_SHORT)
                        .show();
            });
            dialog.setNeutralButton("Garder", (which, whichButton) -> {
            });
            dialog.show();
        });
        return button;
    }

    private Button languageButton(String label, LanguageSyntax language, String text) {

        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(view -> {
            editorView.setLanguage(language, text);
            currentScenario = "Gros".equals(label) ? "gros fichier Java"
                    : "50k l.".equals(label) ? "fichier 52 000 lignes"
                    : label + " (démo)";
            CrashJournal.setSection(currentScenario);
            loggedFirstColoration = false;
            updateStatus();
            watchDiagnostics();
        });
        return button;
    }

    /** La consolidation : le cycle des langages du hors normes. */
    private static final String[] HUGE_CYCLE = {"XML", "JSON", "KOTLIN", "LUA", "SQL", "JAVA"};

    /** La position dans le cycle du hors normes par langage. */
    private int hugeCycleAt;

    /**
     * Le bouton du hors normes par langage : chaque clic ouvre le
     * langage suivant du cycle (XML, JSON, Kotlin, SQL, Java…) — le
     * texte se génère au clic (jamais à l'ouverture de
     * l'application), la consolidation y éprouve le moteur sur
     * chacun.
     */
    private Button hugeCycleButton() {
        Button button = new Button(this);
        button.setText("50k ⨯");
        button.setOnClickListener(view -> {
            String key = HUGE_CYCLE[hugeCycleAt % HUGE_CYCLE.length];
            hugeCycleAt++;
            editorView.setLanguage(languageOf(key), DemoTexts.hugeFile(key));
            currentScenario = "fichier 52 000 lignes (" + key + ")";
            CrashJournal.setSection("fichier 52 000 lignes (" + key + ")");
            loggedFirstColoration = false;
            updateStatus();
            watchDiagnostics();
        });
        return button;
    }

    /**
     * Le langage d'un nom du dépôt (l'onglet ou la clef du cycle).
     *
     * @param name le nom ("Java", "XML", "JSON", "Kotlin", "Lua", "SQL",
     *             ou la clef du cycle en capitales)
     * @return le langage correspondant, Java par défaut
     */
    private LanguageSyntax languageOf(String name) {
        switch (name) {
            case "XML":
            case "xml":
                return Languages.XML;
            case "JSON":
                return Languages.JSON;
            case "Kotlin":
            case "KOTLIN":
                return Languages.KOTLIN;
            case "Lua":
            case "LUA":
                return Languages.LUA;
            case "SQL":
                return Languages.SQL;
            default:
                return Languages.JAVA;
        }
    }

    private Button themeButton(String label, SyntaxTheme theme, int background) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(view -> {
            editorView.applyTheme(theme, background);
            // aucune retokenisation : la ligne au journal le prouve
            // (la première coloration ne bouge pas)
            logScenario("thème " + label);
        });
        return button;
    }

    private void onUndoClicked(View button) {
        // le bouton passe par la vue : le geste porte son étiquette à
        // l'historique local (annulation)
        editorView.undo();
        updateStatus();
    }

    private void onRedoClicked(View button) {
        editorView.redo();
        updateStatus();
    }

    private void onCopyClicked(View button) {
        logScenario("copie - " + currentScenario);
        String measures = buildCopyText();
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("mesures Pigma", measures));
        Toast.makeText(this, "Mesures copiées (" + scenarioLog.size() + " lignes)",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Branche le serveur de démonstration : UNE ligne d'écran (la
     * façade négocie l'initialisation, envoie le document, pose les
     * fournisseurs, reçoit les diagnostics — tout ce que l'écran
     * câblait avant elle). Débrancher coupe tout (le même
     * détachement que le serveur Lua).
     */
    private void onLspToggleClicked(View button) {
        if (lsp.isConnected()) {
            detachLsp();
            return;
        }
        try {
            lsp.connectInProcess(new DemoLanguageServer(DOC_URI), DOC_URI, "java");
        } catch (java.io.IOException noPipe) {
            Toast.makeText(this, "la liaison LSP ne monte pas : " + noPipe.getMessage(),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        lspLabel = "serveur de démo";
        CrashJournal.setSection("LSP démo (branché)");
        lspButton.setText("LSP ✔");
        updateDiagnostics();
    }

    /**
     * Branche le serveur Lua RÉEL — EmmyLua 0.25.1, un exécutable
     * indépendant embarqué dans l'application (le paquetage Android
     * l'extrait vers {@code nativeLibraryDir}, seul répertoire où un
     * exécutable vit depuis l'appareil) : la liaison le lance par
     * processus, lsp4j cadre les messages sur son entrée et sa sortie
     * standard, la négociation y apprend la synchronisation intégrale
     * (le texte entier part à chaque changement amorti). L'espace de
     * travail vit dans le cache : la bibliothèque standard Lua s'y
     * extrait, le document y est un vrai fichier. Débrancher coupe le
     * processus.
     */
    private void onLspLuaClicked(View button) {
        if (lsp.isConnected()) {
            detachLsp();
            return;
        }
        // l'éditeur passe au Lua : le serveur analyse ce que l'écran
        // montre, la coloration suit (le lexeur Lua généré)
        editorView.setLanguage(Languages.LUA, DemoTexts.LUA);
        editorView.getUndoStack().clear();
        currentScenario = "Lua (démo)";
        CrashJournal.setSection("LSP Lua (branchement)");
        loggedFirstColoration = false;
        updateStatus();
        watchDiagnostics();

        File executable = new File(getApplicationInfo().nativeLibraryDir,
                "libemmylua_ls.so");
        if (!executable.canExecute()) {
            Toast.makeText(this, "le serveur EmmyLua manque pour cette architecture ("
                            + Build.SUPPORTED_ABIS[0] + ")", Toast.LENGTH_LONG).show();
            return;
        }
        File workspace = new File(getCacheDir(), "emmylua");
        File resources = new File(workspace, "resources");
        File document = new File(workspace, "demo.lua");
        try {
            // l'espace de travail naît ici (le cache le laisse absent au
            // premier lancement) — sans lui l'écriture du démo échoue et
            // le serveur ne part jamais
            java.nio.file.Files.createDirectories(resources.toPath());
            // writeString n'existe pas dans android.jar (Java 11, jamais
            // porté) — l'écriture classique y va, byte par byte
            java.io.FileOutputStream out = new java.io.FileOutputStream(document);
            try {
                out.write(DemoTexts.LUA.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8));
            } finally {
                out.close();
            }
        } catch (java.io.IOException noFile) {
            Toast.makeText(this, "l'espace de travail ne s'écrit pas : "
                    + noFile.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        Process server;
        try {
            server = new ProcessBuilder(executable.getAbsolutePath(),
                            "--resources-path", resources.getAbsolutePath())
                    .directory(workspace)
                    .redirectError(new File(resources, "stderr.log"))
                    .start();
        } catch (java.io.IOException noProcess) {
            Toast.makeText(this, "le serveur ne part pas : " + noProcess.getMessage(),
                    Toast.LENGTH_LONG).show();
            return;
        }
        lsp.connectProcess(server, document.toURI().toString(), "lua");
        lspLabel = "EmmyLua hors processus";
        CrashJournal.setSection("LSP Lua (branché)");
        lspLuaButton.setText("LSP Lua ✔");
        updateDiagnostics();
    }

    /**
     * Débranche la liaison vivante (le serveur de démo ou le processus
     * EmmyLua) : la façade coupe tout (les fournisseurs se retirent,
     * la liaison s'arrête, le processus meurt, les caches se
     * rangent) — l'écran ne range que ses boutons et sa feuille.
     */
    private void detachLsp() {
        lsp.disconnect();
        lspLabel = null;
        CrashJournal.setSection(currentScenario);
        lspButton.setText("LSP");
        lspLuaButton.setText("LSP Lua");
        updateDiagnostics();
    }

    /**
     * La façade LSP du harnais : le scénario y règle ses fonctions
     * (les coupes une à une — l'éprouve de l'étude de l'éditeur de
     * référence).
     *
     * @return la façade branchée à la vue, jamais null
     */
    LspEditor harnessLsp() {
        return lsp;
    }

    /**
     * La barre d'onglets du harnais : le scénario de la refonte y
     * choisit ses onglets par programme.
     *
     * @return la barre d'onglets, jamais null
     */
    TabLayout harnessTabLayout() {
        return tabLayout;
    }

    /**
     * Le drawer du harnais : le scénario de la refonte y ouvre et y
     * referme le menu.
     *
     * @return le panneau à drawer, jamais null
     */
    androidx.drawerlayout.widget.DrawerLayout harnessDrawer() {
        return drawerLayout;
    }

    /** Le drawer s'ouvre par le harnais (le clic de navigation y mène). */
    void harnessOpenDrawer() {
        openCloseDrawer();
    }

    /**
     * Le menu coulissant du drawer (le scénario y cherche ses
     * boutons).
     *
     * @return le panneau du menu, jamais null
     */
    LinearLayout harnessDrawerMenu() {
        return drawerMenu;
    }

    /**
     * La feuille basse du harnais : le scénario de la refonte y lit
     * l'état et le tire.
     *
     * @return le comportement de la feuille, jamais null
     */
    BottomSheetBehavior<LinearLayout> harnessSheetBehavior() {
        return sheetBehavior;
    }

    /**
     * Le journal du scénario : une ligne par événement, les mesures du
     * moteur au moment où il arrive (première coloration, remplissage
     * fini, bascule de thème, copie).
     */
    private void logScenario(String event) {
        SyntaxDocument syntax = editorView.getSyntaxDocument();
        String row = String.format(Locale.ROOT,
                "| %s | %.1f ms | %.2f ms (%d l.) | %.2f ms (%d l. relues) | "
                        + "%d/%d | %d | trame %.2f ms (moteur %.2f, pire %.2f"
                        + " ; à la pire : moteur %.2f, Δ mémoire %+.1f Mo) |",
                event,
                syntax.firstLexNanos() / 1_000_000.0,
                syntax.lastRequestNanos() / 1_000_000.0,
                syntax.lastRequestLines(),
                syntax.lastEditNanos() / 1_000_000.0,
                syntax.lastRelexedLines(),
                syntax.lexedLineCount(),
                syntax.lineCount(),
                syntax.tokenCount(),
                editorView.lastFrameNanos() / 1_000_000.0,
                editorView.lastFrameEngineNanos() / 1_000_000.0,
                editorView.worstFrameNanos() / 1_000_000.0,
                editorView.worstFrameEngineNanos() / 1_000_000.0,
                editorView.worstFrameMemoryDelta() / 1_048_576.0);
        if (scenarioLog.size() >= LOG_LIMIT) {
            scenarioLog.remove(0);
        }
        scenarioLog.add(row);
    }

    /**
     * Le texte à coller : en-tête, tableau des lignes du journal — la
     * colonne « premiere » ne doit pas bouger à une bascule de thème,
     * « demande » porte le saut lointain, « frappe » les relues.
     */
    private String buildCopyText() {
        StringBuilder text = new StringBuilder();
        text.append("Relevés appareil Pigma (à coller dans docs/perf.md ou à renvoyer tel quel)\n");
        text.append("appareil : à compléter (modèle, version d'Android)\n\n");
        text.append("| événement | première | demande | frappe | lignes lexées | jetons | trame |\n");
        text.append("| --- | --- | --- | --- | --- | --- | --- |\n");
        for (String row : scenarioLog) {
            text.append(row).append('\n');
        }
        return text.toString();
    }

    private void updateStatus() {
        UndoStack stack = editorView.getUndoStack();
        TextBuffer buffer = editorView.getBuffer();
        int offset = Math.min(editorView.getCaretOffset(), buffer.length());
        TextPosition position = buffer.positionOfOffset(offset);
        statusView.setText("rév. " + buffer.getRevision() + " · ligne " + (position.line() + 1)
                + ", col. " + (position.column() + 1) + " · " + buffer.length() + " car.");
        undoButton.setEnabled(stack.canUndo());
        redoButton.setEnabled(stack.canRedo());
    }

    /**
     * Lance le veilleur de la feuille : une passe immédiate, puis une par
     * intervalle tant que le remplissage avance.
     */
    private void watchDiagnostics() {
        updateDiagnostics();
        // le remplissage vient-il de finir ? C'est le moment de lancer
        // l'analyse (l'index et les diagnostics partent par tranches
        // postées, la trame qui suit ne les paie pas) — le dernier tick
        // avant l'arrêt du veilleur s'en charge
        editorView.ensureAnalysis();
        editorView.removeCallbacks(diagnosticsWatcher);
        if (editorView.getSyntaxDocument().remainingLines() > 0) {
            editorView.postDelayed(diagnosticsWatcher, DIAGNOSTIC_WATCH_MS);
        }
    }

    private void updateDiagnostics() {
        SyntaxDocument syntax = editorView.getSyntaxDocument();
        StructureDocument structure = editorView.getStructureDocument();
        int caretLine = editorView.caretLine();
        int depth = caretLine == 0 ? 0 : structure.structureOf(caretLine - 1).depthOut();
        String text = String.format(
                "coloration : première %.1f ms · demande %.2f ms (%d l.) "
                        + "· dernière frappe %.2f ms (%d ligne(s) relue(s)) · %d/%d lignes lexées "
                        + "· %d jetons · trame %.2f ms (pire %.2f, moteur %.2f)"
                        + " · à la pire : moteur %.2f, Δ mémoire %+.1f Mo"
                        + " · fond %d ms (%d tr.)"
                        + " · structure : %d/%d lignes, profondeur %d, %d rompu(s)",
                syntax.firstLexNanos() / 1_000_000.0,
                syntax.lastRequestNanos() / 1_000_000.0,
                syntax.lastRequestLines(),
                syntax.lastEditNanos() / 1_000_000.0,
                syntax.lastRelexedLines(),
                syntax.lexedLineCount(),
                syntax.lineCount(),
                syntax.tokenCount(),
                editorView.lastFrameNanos() / 1_000_000.0,
                editorView.worstFrameNanos() / 1_000_000.0,
                editorView.lastFrameEngineNanos() / 1_000_000.0,
                editorView.worstFrameEngineNanos() / 1_000_000.0,
                editorView.worstFrameMemoryDelta() / 1_048_576.0,
                editorView.backgroundFillNanos() / 1_000_000L,
                editorView.backgroundFillSlices(),
                structure.analyzedLineCount(),
                syntax.lineCount(),
                depth,
                structure.mismatchCount());
        if (syntax.inBigFileMode()) {
            text += " · mode gros fichier";
        }
        if (editorView.hiddenLineCount() > 0) {
            text += String.format(" · replié : %d pli(s), %d ligne(s) cachée(s)",
                    editorView.foldedCount(), editorView.hiddenLineCount());
        }
        if (lsp.isConnected()) {
            text += " · LSP : " + lsp.diagnosticCount() + " diagnostic(s) — " + lspLabel;
        }
        text += String.format(" · L3 : %d annotation(s) (fenêtre)"
                        + " · symboles : %d (%d mots) · diagnostics : %d",
                editorView.semanticVisibleCount(),
                editorView.symbolCount(),
                editorView.wordCount(),
                editorView.diagnosticCount());
        diagnosticsView.setText(text);
        if (!loggedFirstColoration && syntax.firstLexNanos() > 0L) {
            loggedFirstColoration = true;
            logScenario("première coloration - " + currentScenario);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
