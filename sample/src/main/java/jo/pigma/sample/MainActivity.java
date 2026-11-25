package jo.pigma.sample;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
 * Écran principal : l'éditeur coloré au-dessus du modèle de texte du
 * cœur et du moteur de coloration.
 *
 * <p>Deux rangées de sélecteurs : le langage (Java, XML, JSON, Kotlin
 * et Lua générés, SQL par la famille générique — le texte de
 * démonstration change avec) puis le gros fichier Java de la fenêtre
 * paresseuse et le thème (clair, sombre — changer ne retokenise rien,
 * la mesure de première coloration ne bouge pas). Le panneau de
 * diagnostic suit les mesures
 * du moteur : première coloration (la fenêtre visible, pas le document
 * entier), coût de la dernière demande (un saut lointain s'y lit),
 * coût de la dernière frappe, lignes relues, lignes lexées sur le
 * total — il se rafraîchit pendant que la pompe de remplissage
 * avance. La ligne d'état garde la révision, la position et la
 * longueur.</p>
 *
 * <p>Le bouton « Copier les mesures » collecte le journal du scénario
 * (première coloration, remplissage fini, bascules de thème, relevés à
 * la copie) dans le presse-papiers en tableau prêt à coller : c'est le
 * relevé appareil de {@code docs/perf.md}, la machine de build n'a pas
 * d'appareil.</p>
 */
public final class MainActivity extends Activity {

    /** Délai entre deux rafraîchissements du panneau pendant le remplissage. */
    private static final long DIAGNOSTIC_WATCH_MS = 200L;

    /** Lignes du journal au-delà desquelles les plus anciennes tombent. */
    private static final int LOG_LIMIT = 24;

    private EditorView editorView;
    private TextView statusView;
    private TextView diagnosticsView;
    private Button undoButton;
    private Button redoButton;
    private Button copyButton;

    private Button selectButton;

    private Button copyTextButton;

    private Button searchButton;

    /** Le bouton « Signature » : le curseur aux paramètres de la démo. */
    private Button signatureButton;

    /** Le bouton LSP : branche (et débranche) le serveur de démonstration. */
    private Button lspButton;

    /** Le bouton LSP Lua : branche le serveur EmmyLua réel par processus. */
    private Button lspLuaButton;

    /**
     * La façade du serveur de langage : l'ÉCRAN ne câble plus rien —
     * la bibliothèque porte la synchronisation du document, les
     * fournisseurs (survol, complétion, signature, diagnostics) et
     * leurs caches (l'étude de Sora editor y a mené, sa façade
     * LspEditor). L'écran ne garde que ses boutons et son panneau.
     */
    private LspEditor lsp;

    /** L'étiquette du serveur branché (le panneau la lit). */
    private String lspLabel;

    /** Le document de la démo Java (l'URI stable du protocole). */
    private static final String DOC_URI = "file:///demo.java";

    /** Le bouton « Aller à » (la popup aller-à-la-ligne de la vue). */
    private Button gotoButton;

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
            // rafraîchissements de panneau — chacun y lexait la
            // fenêtre visible : le rafraîchissement se reprogramme,
            // la file ne le porte qu'une fois après l'orage ; la
            // frappe ordinaire ne voit pas la différence (il suit la
            // trame). Le document vers le serveur vit dans la façade
            // (elle se programme toute seule).
            editorView.removeCallbacks(statusSync);
            editorView.post(statusSync);
        }
    };

    /** Le rafraîchissement coalescé du panneau (un par salve d'éditions). */
    private final Runnable statusSync = this::runStatusSync;

    /** Le panneau se rafraîchit : statut, diagnostics, veilleur. */
    private void runStatusSync() {
        updateStatus();
        watchDiagnostics();
    }

    /**
     * Le veilleur du panneau : tant que le fil de fond avance, il
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
        selectButton = new Button(this);
        selectButton.setText("Tout sélectionner");
        selectButton.setOnClickListener(this::onSelectAllClicked);
        copyTextButton = new Button(this);
        copyTextButton.setText("Copier texte");
        copyTextButton.setOnClickListener(this::onCopyTextClicked);
        searchButton = new Button(this);
        searchButton.setText("Rechercher");
        // la recherche vit en popup flottante — la vue de bibliothèque
        // la porte (l'ancienne feuille bricolée ici a rejoint le paquet
        // popups)
        searchButton.setOnClickListener(view -> editorView.toggleSearchPopup());
        gotoButton = new Button(this);
        gotoButton.setText("Aller à");
        gotoButton.setOnClickListener(view -> editorView.toggleGotoPopup());
        signatureButton = new Button(this);
        signatureButton.setText("Signature");
        signatureButton.setOnClickListener(this::onSignatureClicked);
        lspButton = new Button(this);
        lspButton.setText("LSP");
        lspButton.setOnClickListener(this::onLspToggleClicked);
        lspLuaButton = new Button(this);
        lspLuaButton.setText("LSP Lua");
        lspLuaButton.setOnClickListener(this::onLspLuaClicked);

        setContentView(buildLayout());
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
        // lignes d'écran — l'attache (l'écouteur du panneau) et le
        // branchement (au clic du bouton). Tout ce qui suit n'est plus
        // de l'écran : la synchronisation du document, le survol, la
        // complétion, la signature, les diagnostics à la carte — la
        // bibliothèque le porte (la leçon de Sora editor, sa façade
        // LspEditor)
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
        // le remplissage avance par tranches : le panneau suit
        editorView.post(this::watchDiagnostics);
    }

    /**
     * Les intentions de l'hôte à une ligne (l'ampoule les porte) : la
     * date du jour au curseur, les statistiques du document.
     *
     * @param line   l'indice de la ligne
     * @param column la colonne du curseur
     * @return les actions de l'hôte, jamais null
     */
    private java.util.List<QuickActionCard.Entry> hostQuickActions(int line, int column) {
        return java.util.List.of(
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

    private View buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int margin = dp(8);

        LinearLayout.LayoutParams editorParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        editorParams.setMargins(margin, margin, margin, margin);
        root.addView(editorView, editorParams);

        root.addView(row(margin,
                languageButton("Java", Languages.JAVA, DemoTexts.JAVA),
                languageButton("XML", Languages.XML, DemoTexts.XML),
                languageButton("JSON", Languages.JSON, DemoTexts.JSON),
                languageButton("Kotlin", Languages.KOTLIN, DemoTexts.KOTLIN),
                languageButton("Lua", Languages.LUA, DemoTexts.LUA),
                languageButton("SQL", Languages.SQL, DemoTexts.SQL)));
        root.addView(row(margin,
                languageButton("Gros", Languages.JAVA, DemoTexts.bigFile()),
                languageButton("50k l.", Languages.JAVA, DemoTexts.hugeFile()),
                hugeCycleButton(),
                themeButton("Clair", EditorThemes.LIGHT, EditorThemes.LIGHT_BACKGROUND),
                themeButton("Sombre", EditorThemes.DARK, EditorThemes.DARK_BACKGROUND),
                undoButton, redoButton));

        root.addView(row(margin, copyButton, selectButton, copyTextButton,
                searchButton, gotoButton, signatureButton, lspButton, lspLuaButton));

        LinearLayout.LayoutParams diagnosticsParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        diagnosticsParams.setMargins(dp(16), 0, margin, 0);
        root.addView(diagnosticsView, diagnosticsParams);

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(dp(16), 0, margin, margin);
        root.addView(statusView, statusParams);
        return root;
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

    private Button languageButton(String label, LanguageSyntax language, String text) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(view -> {
            editorView.setLanguage(language, text);
            currentScenario = "Gros".equals(label) ? "gros fichier Java"
                    : "50k l.".equals(label) ? "fichier 52 000 lignes"
                    : label + " (demo)";
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
            loggedFirstColoration = false;
            updateStatus();
            watchDiagnostics();
        });
        return button;
    }

    /**
     * Le langage d'une clef du cycle du hors normes.
     *
     * @param key la clef ("XML", "JSON", "KOTLIN", "SQL")
     * @return le langage correspondant, Java par défaut
     */
    private LanguageSyntax languageOf(String key) {
        switch (key) {
            case "XML":
                return Languages.XML;
            case "JSON":
                return Languages.JSON;
            case "KOTLIN":
                return Languages.KOTLIN;
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
     * Tout sélectionner, ou quitter la sélection si elle est déjà
     * entière : le bouton démontre la sélection (la frappe suivante
     * remplacera, Suppr l'effacera, « Copier texte » la lira).
     */
    private void onSelectAllClicked(View button) {
        if (editorView.hasSelection()
                && editorView.selectionStart() == 0
                && editorView.selectionEnd() == editorView.getBuffer().length()) {
            editorView.caretTo(0);
            selectButton.setText("Tout sélectionner");
        } else {
            editorView.selectAll();
            selectButton.setText("Quitter la sélection");
        }
    }

    /**
     * Le curseur aux paramètres de la méthode de démonstration : la
     * carte de signature s'y montre — les virgules y déplacent le
     * paramètre actif, la fermeture de l'appel la range.
     */
    private void onSignatureClicked(View button) {
        String text = editorView.getBuffer().toString();
        int args = text.indexOf("int graine");
        if (args >= 0) {
            editorView.caretTo(args + 3);
        }
    }

    /**
     * Copie le texte sélectionné (le presse-papiers système) : la
     * taille dit si la sélection tient.
     */
    private void onCopyTextClicked(View button) {
        CharSequence selected = editorView.selectedText();
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("sélection Pigma", selected));
        Toast.makeText(this, selected.length() + " caractère(s) copié(s)",
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
        lspLuaButton.setText("LSP Lua ✔");
        updateDiagnostics();
    }

    /**
     * Débranche la liaison vivante (le serveur de démo ou le processus
     * EmmyLua) : la façade coupe tout (les fournisseurs se retirent,
     * la liaison s'arrête, le processus meurt, les caches se
     * rangent) — l'écran ne range que ses boutons et son panneau.
     */
    private void detachLsp() {
        lsp.disconnect();
        lspLabel = null;
        lspButton.setText("LSP");
        lspLuaButton.setText("LSP Lua");
        updateDiagnostics();
    }

    /**
     * La façade LSP du harnais : le scénario y règle ses fonctions
     * (les coupes une à une — l'éprouve de l'étude de Sora editor).
     *
     * @return la façade branchée à la vue, jamais null
     */
    LspEditor harnessLsp() {
        return lsp;
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
     * Lance le veilleur du panneau : une passe immédiate, puis une par
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
