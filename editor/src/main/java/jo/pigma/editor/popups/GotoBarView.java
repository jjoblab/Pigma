package jo.pigma.editor.popups;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

import jo.pigma.editor.EditorView;

/**
 * La barre « aller à la ligne » — un VRAI View du paquet popups,
 * portée par EditorView en PopupWindow flottante (toggleGotoPopup).
 * Le champ ne prend que des chiffres, la touche « Aller » du clavier
 * vaut le bouton, et le numéro se borne au fichier (une ligne
 * au-delà va à la dernière — le retour le dit).
 */
public final class GotoBarView extends LinearLayout {

    private final EditText lineField;
    private final TextView feedbackView;

    private EditorView editor;

    public GotoBarView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        int margin = dp(8);

        lineField = new EditText(context);
        lineField.setHint("numéro de ligne…");
        lineField.setInputType(InputType.TYPE_CLASS_NUMBER);
        lineField.setImeOptions(EditorInfo.IME_ACTION_GO);
        lineField.setOnEditorActionListener((view, actionId, event) -> {
            go();
            return true;
        });
        feedbackView = new TextView(context);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 2.0f);
        fieldParams.setMargins(0, 0, margin / 2, 0);
        row.addView(lineField, fieldParams);
        LinearLayout.LayoutParams goParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        goParams.setMargins(0, 0, margin / 2, 0);
        row.addView(button("Aller", "aller à la ligne", this::go), goParams);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        row.addView(button("✕", "fermer la barre", this::close), closeParams);
        addView(row);

        LinearLayout.LayoutParams feedbackParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        feedbackParams.setMargins(dp(16), 0, margin, 0);
        addView(feedbackView, feedbackParams);
    }

    /**
     * Branche la barre à son éditeur : l'aller-y lui parle.
     *
     * @param editor l'éditeur parcouru
     */
    public void bind(EditorView editor) {
        this.editor = editor;
    }

    /** La popup s'ouvre : le champ prend le focus, le fichier se compte. */
    public void opened() {
        lineField.requestFocus();
        feedbackView.setText(String.format(Locale.ROOT,
                "sur %d ligne(s)", editor.getBuffer().getLineCount()));
    }

    /**
     * L'aller-y : le numéro se borne (1 à la dernière ligne), le
     * curseur s'y pose — caretTo l'assure visible — et le retour
     * dit où il est tombé.
     */
    private void go(View button) {
        go();
    }

    private void go() {
        if (editor == null) {
            return;
        }
        String raw = lineField.getText().toString().trim();
        int lineCount = editor.getBuffer().getLineCount();
        if (raw.isEmpty() || lineCount == 0) {
            feedbackView.setText("ligne ?");
            return;
        }
        int line;
        try {
            line = Integer.parseInt(raw);
        } catch (NumberFormatException notANumber) {
            feedbackView.setText("ligne ?");
            return;
        }
        int bounded = Math.max(1, Math.min(line, lineCount));
        editor.caretTo(editor.getBuffer().getLineStart(bounded - 1));
        feedbackView.setText(String.format(Locale.ROOT,
                "ligne %d · sur %d", bounded, lineCount));
    }

    private void close(View button) {
        editor.dismissGotoPopup();
    }

    private Button button(String label, String description,
                          View.OnClickListener listener) {
        Button button = new Button(getContext());
        button.setText(label);
        button.setContentDescription(description);
        button.setOnClickListener(listener);
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
