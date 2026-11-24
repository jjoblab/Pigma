package jo.pigma.sample;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jo.pigma.core.TextBuffer;
import jo.pigma.core.TextListener;
import jo.pigma.core.TextPosition;
import jo.pigma.core.UndoStack;
import jo.pigma.syntax.LanguageSyntax;
import jo.pigma.syntax.Languages;
import jo.pigma.syntax.SyntaxDocument;
import jo.pigma.syntax.SyntaxTheme;

/**
 * Écran principal : l'éditeur coloré au-dessus du modèle de texte du
 * cœur et du moteur de coloration.
 *
 * <p>Deux rangées de sélecteurs : le langage (Java, XML, JSON générés,
 * SQL par la famille générique, plus le gros fichier Java de la fenêtre
 * paresseuse — le texte de démonstration change avec) et le thème
 * (clair, sombre — changer ne retokenise rien, la mesure de première
 * coloration ne bouge pas). Le panneau de diagnostic suit les mesures
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
        }
    };

    /**
     * Le veilleur du panneau : tant que la pompe de remplissage avance,
     * il rafraîchit les mesures (lignes lexées qui montent) ; la fin du
     * remplissage s'inscrit au journal.
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
                logScenario("remplissage fini - " + currentScenario);
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

        setContentView(buildLayout());
        editorView.getBuffer().addListener(statusListener);
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
                languageButton("SQL", Languages.SQL, DemoTexts.SQL),
                languageButton("Gros", Languages.JAVA, DemoTexts.bigFile())));
        root.addView(row(margin,
                themeButton("Clair", SampleThemes.LIGHT, SampleThemes.LIGHT_BACKGROUND),
                themeButton("Sombre", SampleThemes.DARK, SampleThemes.DARK_BACKGROUND),
                undoButton, redoButton));

        root.addView(row(margin, copyButton));

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
            currentScenario = "Gros".equals(label) ? "gros fichier Java" : label + " (demo)";
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
     * Le journal du scénario : une ligne par événement, les mesures du
     * moteur au moment où il arrive (première coloration, remplissage
     * fini, bascule de thème, copie).
     */
    private void logScenario(String event) {
        SyntaxDocument syntax = editorView.getSyntaxDocument();
        String row = String.format(Locale.ROOT,
                "| %s | %.1f ms | %.2f ms (%d l.) | %.2f ms (%d l. relues) | %d/%d | %d |",
                event,
                syntax.firstLexNanos() / 1_000_000.0,
                syntax.lastRequestNanos() / 1_000_000.0,
                syntax.lastRequestLines(),
                syntax.lastEditNanos() / 1_000_000.0,
                syntax.lastRelexedLines(),
                syntax.lexedLineCount(),
                syntax.lineCount(),
                syntax.tokenCount());
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
        text.append("| événement | première | demande | frappe | lignes lexées | jetons |\n");
        text.append("| --- | --- | --- | --- | --- | --- |\n");
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
        editorView.removeCallbacks(diagnosticsWatcher);
        if (editorView.getSyntaxDocument().remainingLines() > 0) {
            editorView.postDelayed(diagnosticsWatcher, DIAGNOSTIC_WATCH_MS);
        }
    }

    private void updateDiagnostics() {
        SyntaxDocument syntax = editorView.getSyntaxDocument();
        String text = String.format(
                "coloration : première %.1f ms · demande %.2f ms (%d l.) "
                        + "· dernière frappe %.2f ms (%d ligne(s) relue(s)) · %d/%d lignes lexées "
                        + "· %d jetons",
                syntax.firstLexNanos() / 1_000_000.0,
                syntax.lastRequestNanos() / 1_000_000.0,
                syntax.lastRequestLines(),
                syntax.lastEditNanos() / 1_000_000.0,
                syntax.lastRelexedLines(),
                syntax.lexedLineCount(),
                syntax.lineCount(),
                syntax.tokenCount());
        if (syntax.inBigFileMode()) {
            text += " · mode gros fichier";
        }
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
