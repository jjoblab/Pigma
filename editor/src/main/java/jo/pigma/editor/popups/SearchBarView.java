package jo.pigma.editor.popups;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import jo.pigma.editor.EditorView;

/**
 * La barre de recherche et remplacement, à la façon d'Android
 * Studio : une carte compacte flottant en haut à droite de
 * l'éditeur (EditorView la porte en PopupWindow), le champ suivi
 * du compte « i/n » — rouge quand rien ne correspond —, la
 * navigation par chevrons, la fermeture par croix. Le
 * remplacement se replie sous la recherche : le rabat à deux
 * chevrons le montre, comme le Ctrl+R de l'atelier.
 *
 * <p>La recherche est incrémentale (amortie de 150 ms — les frappes
 * rapides n'envoient que la dernière) ; remplacer la correspondance
 * courante fait une unité d'annulation, tout remplacer une seule
 * (les remplacements vont de la fin vers le début — les positions
 * antérieures restent valides).</p>
 */
public final class SearchBarView extends LinearLayout {

    /** L'amortissement de la recherche incrémentale (ms). */
    private static final long SEARCH_DEBOUNCE_MS = 150L;

    private final EditText queryField;
    private final EditText replacementField;

    /** Le compte « i/n » — rouge quand rien ne correspond. */
    private final TextView matchView;

    /** La rangée du remplacement, repliée au départ (le rabat la montre). */
    private final LinearLayout replaceRow;

    private final Runnable searchSync = this::runIncrementalSearch;

    private EditorView editor;

    public SearchBarView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setBackground(card());
        setPadding(dp(8), dp(4), dp(4), dp(6));

        queryField = new EditText(context);
        queryField.setHint("chercher…");
        // la recherche est incrémentale : chaque frappe du champ
        // relance la recherche (amortie — les frappes rapides
        // n'envoient que la dernière)
        queryField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                scheduleIncrementalSearch();
            }
        });
        replacementField = new EditText(context);
        replacementField.setHint("remplacer par…");
        matchView = new TextView(context);
        matchView.setText("0/0");
        matchView.setTextSize(12.0f);
        matchView.setTextColor(0xFF5F6368);

        // la rangée de recherche : le rabat, le champ, le compte,
        // la navigation, la fermeture
        LinearLayout searchRow = new LinearLayout(context);
        searchRow.setOrientation(HORIZONTAL);
        searchRow.addView(new IconButton(context, IconButton.Icon.TOGGLE,
                "montrer ou cacher le remplacement", this::toggleReplace));
        LayoutParams queryParams =
                new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f);
        searchRow.addView(queryField, queryParams);
        LayoutParams matchParams =
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        matchParams.setMargins(0, dp(10), 0, 0);
        searchRow.addView(matchView, matchParams);
        searchRow.addView(new IconButton(context, IconButton.Icon.CHEVRON_UP,
                "correspondance précédente", this::previous));
        searchRow.addView(new IconButton(context, IconButton.Icon.CHEVRON_DOWN,
                "correspondance suivante", this::next));
        searchRow.addView(new IconButton(context, IconButton.Icon.CLOSE,
                "fermer la recherche", this::close));
        addView(searchRow);

        // la rangée du remplacement : le champ, remplacer, tout
        replaceRow = new LinearLayout(context);
        replaceRow.setOrientation(HORIZONTAL);
        LayoutParams replaceParams =
                new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f);
        replaceRow.addView(replacementField, replaceParams);
        replaceRow.addView(new IconButton(context, IconButton.Icon.REPLACE,
                "remplacer la correspondance courante", this::replaceOne));
        replaceRow.addView(new IconButton(context, IconButton.Icon.REPLACE_ALL,
                "remplacer toutes les correspondances", this::replaceAll));
        replaceRow.setVisibility(GONE);
        addView(replaceRow);
    }

    /**
     * Branche la barre à son éditeur : les actions y parlent, la
     * fermeture lui revient.
     *
     * @param editor l'éditeur recherché et remplacé
     */
    public void bind(EditorView editor) {
        this.editor = editor;
    }

    /** La popup s'ouvre : le champ prend le focus, le compte se pose. */
    public void opened() {
        queryField.requestFocus();
        updateCount();
    }

    /** La popup se ferme : la recherche amortie se range avec elle. */
    public void closed() {
        if (editor != null) {
            editor.removeCallbacks(searchSync);
        }
    }

    /** Le rabat : la rangée du remplacement se montre ou se replie. */
    private void toggleReplace(View button) {
        replaceRow.setVisibility(
                replaceRow.getVisibility() == VISIBLE ? GONE : VISIBLE);
    }

    private void previous(View button) {
        editor.searchPrevious();
        updateCount();
    }

    private void next(View button) {
        editor.searchNext();
        updateCount();
    }

    private void close(View button) {
        editor.dismissSearchPopup();
    }

    /**
     * Remplace la correspondance courante : une unité d'annulation, la
     * recherche se relance (le compte se met à jour).
     */
    private void replaceOne(View button) {
        editor.replaceCurrent(replacementField.getText().toString());
        updateCount();
    }

    /**
     * Remplace toutes les correspondances : une seule unité
     * d'annulation (les remplacements vont de la fin vers le début).
     */
    private void replaceAll(View button) {
        int count = editor.replaceAll(replacementField.getText().toString());
        updateCount();
        Toast.makeText(getContext(), count + " remplacement(s)",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Une frappe du champ arrive : la recherche se reprogramme — les
     * frappes rapides n'envoient qu'une recherche, la dernière.
     */
    private void scheduleIncrementalSearch() {
        if (editor == null) {
            return;
        }
        editor.removeCallbacks(searchSync);
        editor.postDelayed(searchSync, SEARCH_DEBOUNCE_MS);
    }

    /**
     * La recherche incrémentale : le champ vient de bouger, la
     * recherche le suit (le champ vide range la recherche). La
     * navigation va à la correspondance la plus proche du curseur —
     * le comportement attendu d'une recherche qui suit la frappe.
     */
    private void runIncrementalSearch() {
        String query = queryField.getText().toString();
        if (query.isEmpty()) {
            editor.clearSearch();
        } else {
            editor.search(query);
        }
        updateCount();
    }

    /** Le compte « i/n », rouge quand rien ne correspond. */
    private void updateCount() {
        if (editor == null) {
            return;
        }
        int total = editor.searchMatchCount();
        int current = total == 0 ? 0 : editor.searchCurrentIndex() + 1;
        matchView.setText(String.format(Locale.ROOT, "%d/%d", current, total));
        matchView.setTextColor(total == 0 ? 0xFFC62828 : 0xFF5F6368);
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
