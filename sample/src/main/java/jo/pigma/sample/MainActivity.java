package jo.pigma.sample;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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
 * SQL par la famille générique — le texte de démonstration change
 * avec) et le thème (clair, sombre — changer ne retokenise rien, la
 * mesure du premier lexage ne bouge pas). Le panneau de diagnostic
 * affiche les mesures du moteur : première coloration, coût de la
 * dernière frappe, lignes relues, jetons. La ligne d'état garde la
 * révision, la position et la longueur.</p>
 */
public final class MainActivity extends Activity {

    private EditorView editorView;
    private TextView statusView;
    private TextView diagnosticsView;
    private Button undoButton;
    private Button redoButton;

    private final TextListener statusListener = new TextListener() {
        @Override
        public void textChanged(TextBuffer buffer, int start, int removedLength,
                                int insertedLength, int revision) {
            updateStatus();
            updateDiagnostics();
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

        setContentView(buildLayout());
        editorView.getBuffer().addListener(statusListener);
        updateStatus();
        updateDiagnostics();
        // le premier lexage est paresseux (au premier dessin) : le
        // panneau se rafraîchit juste après la première trame
        editorView.post(this::updateDiagnostics);
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
                languageButton("SQL", Languages.SQL, DemoTexts.SQL)));
        root.addView(row(margin,
                themeButton("Clair", SampleThemes.LIGHT, SampleThemes.LIGHT_BACKGROUND),
                themeButton("Sombre", SampleThemes.DARK, SampleThemes.DARK_BACKGROUND),
                undoButton, redoButton));

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
            updateStatus();
            updateDiagnostics();
        });
        return button;
    }

    private Button themeButton(String label, SyntaxTheme theme, int background) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(view ->
                editorView.applyTheme(theme, background));
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

    private void updateDiagnostics() {
        SyntaxDocument syntax = editorView.getSyntaxDocument();
        diagnosticsView.setText(String.format(
                "coloration : première %.1f ms · dernière frappe %.2f ms "
                        + "(%d ligne(s) relue(s)) · %d jetons · %d lignes",
                syntax.firstLexNanos() / 1_000_000.0,
                syntax.lastEditNanos() / 1_000_000.0,
                syntax.lastRelexedLines(),
                syntax.tokenCount(),
                syntax.lineCount()));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
