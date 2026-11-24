package jo.pigma.sample;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jo.pigma.editor.EditorThemes;
import jo.pigma.editor.EditorView;
import jo.pigma.core.TextBuffer;
import jo.pigma.core.TextListener;
import jo.pigma.core.TextPosition;
import jo.pigma.core.UndoStack;
import jo.pigma.lsp.LspDiagnostic;
import jo.pigma.lsp.LspPosition;
import jo.pigma.lsp.LspSession;
import jo.pigma.syntax.LanguageSyntax;
import jo.pigma.syntax.Languages;
import jo.pigma.syntax.StructureDocument;
import jo.pigma.syntax.SyntaxDocument;
import jo.pigma.syntax.SyntaxTheme;

/**
 * Écran principal : l'éditeur coloré au-dessus du modèle de texte du
 * cœur et du moteur de coloration.
 *
 * <p>Deux rangées de sélecteurs : le langage (Java, XML, JSON et
 * Kotlin générés, SQL par la famille générique — le texte de
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

    /** Le bouton LSP : branche (et débranche) le serveur de démonstration. */
    private Button lspButton;

    /** La liaison LSP branchée (null : aucune). */
    private LspSession lspSession;

    /** Le document courant côté serveur (l'URI stable du protocole). */
    private static final String DOC_URI = "file:///demo.java";

    /** Les diagnostics du dernier avis du serveur (le panneau les lit). */
    private volatile int lspDiagnosticCount = -1;

    /** Le report du changement de document vers le serveur (la synchronisation intégrale se amortit). */
    private final Runnable lspSync = this::pushDocumentToServer;

    /** Le report de la frappe du champ vers la recherche (incrémentale). */
    private final Runnable searchSync = this::runIncrementalSearch;

    /** La feuille du bas : la recherche et le remplacement y vivent. */
    private LinearLayout searchSheet;

    private EditText queryField;

    private EditText replacementField;

    private TextView matchView;

    private final List<String> scenarioLog = new ArrayList<>();
    private String currentScenario = "Java (démo)";
    private boolean loggedFirstColoration;
    private boolean filling;

    private final TextListener statusListener = new TextListener() {
        @Override
        public void textChanged(TextBuffer buffer, int start, int removedLength,
                                int insertedLength, int revision) {
            updateStatus();
            watchDiagnostics();
            scheduleLspSync();
        }
    };

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
        searchButton.setOnClickListener(this::onSearchToggleClicked);
        lspButton = new Button(this);
        lspButton.setText("LSP");
        lspButton.setOnClickListener(this::onLspToggleClicked);
        queryField = new EditText(this);
        // la recherche est incrémentale : chaque frappe du champ
        // relance la recherche (amortie de 150 ms — les frappes
        // rapides n'envoient que la dernière), le bouton « Chercher »
        // garde sa voie explicite
        queryField.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                scheduleIncrementalSearch();
            }
        });
        replacementField = new EditText(this);
        matchView = new TextView(this);

        setContentView(buildLayout());
        editorView.getBuffer().addListener(statusListener);
        // la vue de bibliothèque démarre vide : l'hôte charge son
        // contenu — ici la démo Java (le chargement n'est pas une
        // édition à défaire, l'histoire repart vide)
        editorView.setLanguage(Languages.JAVA, DemoTexts.JAVA);
        editorView.getUndoStack().clear();
        updateStatus();
        updateDiagnostics();
        // la première coloration est paresseuse (au premier dessin) et
        // le remplissage avance par tranches : le panneau suit
        editorView.post(this::watchDiagnostics);
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
                languageButton("SQL", Languages.SQL, DemoTexts.SQL)));
        root.addView(row(margin,
                languageButton("Gros", Languages.JAVA, DemoTexts.bigFile()),
                languageButton("50k l.", Languages.JAVA, DemoTexts.hugeFile()),
                themeButton("Clair", EditorThemes.LIGHT, EditorThemes.LIGHT_BACKGROUND),
                themeButton("Sombre", EditorThemes.DARK, EditorThemes.DARK_BACKGROUND),
                undoButton, redoButton));

        root.addView(row(margin, copyButton, selectButton, copyTextButton, searchButton, lspButton));

        // la feuille du bas (masquée au départ) : la recherche et le
        // remplacement — elle descend sous la rangée d'actions, la
        // fermeture range la recherche
        searchSheet = new LinearLayout(this);
        searchSheet.setOrientation(LinearLayout.VERTICAL);
        searchSheet.addView(row(margin,
                field(queryField, "chercher…"),
                sheetButton("Chercher", this::onDoSearchClicked),
                sheetButton("‹", this::onSearchPreviousClicked),
                sheetButton("›", this::onSearchNextClicked),
                sheetButton("✕", this::onSearchCloseClicked)));
        searchSheet.addView(row(margin,
                field(replacementField, "remplacer par…"),
                sheetButton("Remplacer", this::onReplaceOneClicked),
                sheetButton("Tout", this::onReplaceAllClicked)));
        LinearLayout.LayoutParams matchParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        matchParams.setMargins(dp(16), 0, margin, 0);
        searchSheet.addView(matchView, matchParams);
        searchSheet.setVisibility(View.GONE);
        LinearLayout.LayoutParams sheetParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        root.addView(searchSheet, sheetParams);

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

    /**
     * La rangée mixte (un champ de saisie parmi les boutons) : le
     * champ pèse le double d'un bouton, la saisie garde la place.
     */
    private LinearLayout row(int margin, Object... views) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (Object view : views) {
            boolean isField = view instanceof EditText;
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT,
                            isField ? 2.0f : 1.0f);
            row.addView((View) view, params);
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
        editorView.getUndoStack().undo();
        updateStatus();
    }

    private void onRedoClicked(View button) {
        editorView.getUndoStack().redo();
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
     * La feuille du bas s'ouvre (le champ prend le focus, la recherche
     * précédente se range) ou se ferme (la recherche se range aussi).
     */
    private void onSearchToggleClicked(View button) {
        if (searchSheet.getVisibility() == View.VISIBLE) {
            closeSearchSheet();
        } else {
            searchSheet.setVisibility(View.VISIBLE);
            queryField.requestFocus();
            updateMatchView();
        }
    }

    /** Cherche le contenu du champ : les teintes et le compte suivent. */
    private void onDoSearchClicked(View button) {
        editorView.search(queryField.getText().toString());
        updateMatchView();
    }

    /**
     * Une frappe du champ arrive : la recherche se reprogramme de
     * 150 ms — les frappes rapides n'envoient qu'une recherche, la
     * dernière.
     */
    private void scheduleIncrementalSearch() {
        if (searchSheet == null || searchSheet.getVisibility() != View.VISIBLE) {
            return;
        }
        editorView.removeCallbacks(searchSync);
        editorView.postDelayed(searchSync, 150L);
    }

    /**
     * La recherche incrémentale : le champ vient de bouger, la
     * recherche le suit (le champ vide range la recherche). La
     * navigation va à la correspondance la plus proche du curseur —
     * le comportement attendu d'une recherche qui suit la frappe.
     */
    private void runIncrementalSearch() {
        if (searchSheet.getVisibility() != View.VISIBLE) {
            return;
        }
        String query = queryField.getText().toString();
        if (query.isEmpty()) {
            editorView.clearSearch();
        } else {
            editorView.search(query);
        }
        updateMatchView();
    }

    private void onSearchNextClicked(View button) {
        editorView.searchNext();
        updateMatchView();
    }

    private void onSearchPreviousClicked(View button) {
        editorView.searchPrevious();
        updateMatchView();
    }

    private void onSearchCloseClicked(View button) {
        closeSearchSheet();
    }

    /**
     * Remplace la correspondance courante : une unité d'annulation, la
     * recherche se relance (le compte se met à jour).
     */
    private void onReplaceOneClicked(View button) {
        editorView.replaceCurrent(replacementField.getText().toString());
        updateMatchView();
    }

    /**
     * Remplace toutes les correspondances : une seule unité
     * d'annulation (les remplacements vont de la fin vers le début —
     * les positions antérieures restent valides).
     */
    private void onReplaceAllClicked(View button) {
        int count = editorView.replaceAll(replacementField.getText().toString());
        updateMatchView();
        Toast.makeText(this, count + " remplacement(s)",
                Toast.LENGTH_SHORT).show();
    }

    private void closeSearchSheet() {
        searchSheet.setVisibility(View.GONE);
        editorView.clearSearch();
        updateMatchView();
    }

    /**
     * Branche le serveur de démonstration : la liaison négocie
     * l'initialisation, le document part au serveur, ses diagnostics
     * arrivent au panneau, la popup d'info interroge le survol (la
     * réponse du serveur prime sur l'index local — le fournisseur
     * rend null quand le serveur ne sait pas). Débrancher coupe tout.
     */
    private void onLspToggleClicked(View button) {
        if (lspSession != null) {
            editorView.setInfoProvider(null);
            lspSession.shutdown();
            lspSession = null;
            lspDiagnosticCount = -1;
            lspButton.setText("LSP");
            updateDiagnostics();
            return;
        }
        // la liaison monte le serveur au même processus : lsp4j porte
        // le protocole (JSON-RPC et gson dessous), les tubes ne
        // quittent pas la machine
        try {
            lspSession = LspSession.beside(new DemoLanguageServer(DOC_URI),
                    new LspSession.Listener() {
                        @Override
                        public void onDiagnostics(String uri,
                                java.util.List<LspDiagnostic> diagnostics) {
                            lspDiagnosticCount = diagnostics.size();
                            runOnUiThread(MainActivity.this::updateDiagnostics);
                        }
                    });
        } catch (java.io.IOException noPipe) {
            Toast.makeText(this, "la liaison LSP ne monte pas : " + noPipe.getMessage(),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        lspSession.initialize("pigma-sample");
        pushDocumentToServer();
        // le survol du serveur prime : la réponse attend au plus
        // 200 ms — le serveur de démonstration répond dans le
        // processus, instantanément ; un serveur réel demanderait la
        // voie asynchrone (la réponse remplace la popup quand elle
        // arrive)
        editorView.setInfoProvider((line, column, word) -> {
            if (lspSession == null) {
                return null;
            }
            try {
                return lspSession.hover(DOC_URI, new LspPosition(line, column))
                        .get(200, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (Exception noAnswer) {
                return null;
            }
        });
        lspButton.setText("LSP ✔");
        updateDiagnostics();
    }

    /**
     * Le document part au serveur — la liaison n'envoie que la plage
     * qui a bougé (l'ombre de la dernière synchronisation tient le
     * reste) ; chaque édition le reprogramme de 300 ms : les frappes
     * rapides n'envoient qu'un delta, le dernier.
     */
    private void pushDocumentToServer() {
        if (lspSession != null) {
            lspSession.didChange(DOC_URI, editorView.getBuffer().toString());
        }
    }

    /** Une édition arrive : le report du document se reprogramme. */
    private void scheduleLspSync() {
        if (lspSession == null) {
            return;
        }
        editorView.removeCallbacks(lspSync);
        editorView.postDelayed(lspSync, 300L);
    }

    /** La ligne du compte de correspondances (« 3/12 » ou « aucune »). */
    private void updateMatchView() {
        int total = editorView.searchMatchCount();
        if (total == 0) {
            matchView.setText("aucune correspondance");
        } else {
            matchView.setText(String.format(Locale.ROOT,
                    "%d correspondance(s) · courante %d/%d",
                    total, editorView.searchCurrentIndex() + 1, total));
        }
    }

    /** Un champ de saisie de la feuille (l'indice dit son rôle). */
    private EditText field(EditText input, String hint) {
        input.setHint(hint);
        return input;
    }

    /** Un bouton compact de la feuille du bas. */
    private Button sheetButton(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(listener);
        return button;
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
        if (lspDiagnosticCount >= 0) {
            text += " · LSP : " + lspDiagnosticCount + " diagnostic(s) du serveur de démo";
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
