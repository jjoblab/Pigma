package jo.pigma.editor.popups;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

import jo.pigma.editor.EditorView;

/**
 * La barre « aller à la ligne », à la façon d'Android Studio : une
 * carte compacte centrée en haut de l'éditeur (EditorView la porte
 * en PopupWindow), l'invite « Aller à la ligne : » suivie du champ —
 * la touche « Aller » du clavier vaut la validation, comme le Ctrl+G
 * de l'atelier. Le numéro se borne au fichier (une ligne au-delà va
 * à la dernière — le retour le dit).
 */
public final class GotoBarView extends LinearLayout {

    private final EditText lineField;
    private final TextView feedbackView;

    private EditorView editor;

    public GotoBarView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setBackground(card());
        setPadding(dp(8), dp(4), dp(4), dp(6));

        lineField = new EditText(context);
        lineField.setHint("numéro");
        lineField.setInputType(InputType.TYPE_CLASS_NUMBER);
        lineField.setImeOptions(EditorInfo.IME_ACTION_GO);
        lineField.setOnEditorActionListener((view, actionId, event) -> {
            go();
            return true;
        });
        feedbackView = new TextView(context);
        feedbackView.setTextSize(12.0f);
        feedbackView.setTextColor(0xFF5F6368);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        TextView label = new TextView(context);
        label.setText("Aller à la ligne :");
        label.setTextSize(14.0f);
        label.setTextColor(0xFF202124);
        row.addView(label);
        LayoutParams fieldParams =
                new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f);
        row.addView(lineField, fieldParams);
        row.addView(new IconButton(context, IconButton.Icon.CLOSE,
                "fermer la barre", this::close));
        addView(row);

        LayoutParams feedbackParams =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        feedbackParams.setMargins(dp(4), 0, dp(8), 0);
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

    /**
     * La carte flottante : coins arrondis, fond clair, liseré gris —
     * la façon de l'atelier.
     */
    private GradientDrawable card() {
        GradientDrawable card = new GradientDrawable();
        card.setCornerRadius(dp(10));
        card.setColor(0xFFF3F3F3);
        card.setStroke(dp(1), 0xFFB5B5B5);
        return card;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
