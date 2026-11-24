package jo.pigma.editor.popups;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import jo.pigma.editor.EditorView;

/**
 * La barre de recherche et remplacement — un VRAI View du paquet
 * popups (l'ancienne voie : la feuille bricolée par l'hôte, collée à
 * son layout). La barre se bâtit en code (la bibliothèque ne connaît
 * pas de XML) : la question, le remplacement, la navigation et le
 * compte des correspondances.
 *
 * <p>L'hôte ne pilote que l'ouverture : EditorView la porte en
 * PopupWindow flottante (toggleSearchPopup), tout le reste vit ici —
 * la recherche incrémentale amortie de 150 ms (les frappes rapides
 * n'envoient que la dernière), le remplacement de l'occurrence
 * courante, le remplacement total (une unité d'annulation, de la fin
 * vers le début — les positions antérieures restent valides).</p>
 *
 * <p>Le compteur se lit comme la feuille d'origine :
 * « n correspondance(s) · courante i/n », ou « aucune
 * correspondance ».</p>
 */
public final class SearchBarView extends LinearLayout {

    /** L'amortissement de la recherche incrémentale (ms). */
    private static final long SEARCH_DEBOUNCE_MS = 150L;

    private final EditText queryField;
    private final EditText replacementField;
    private final TextView matchView;
    private final Runnable searchSync = this::runIncrementalSearch;

    private EditorView editor;

    public SearchBarView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        int margin = dp(8);

        queryField = new EditText(context);
        queryField.setHint("chercher…");
        // la recherche est incrémentale : chaque frappe du champ
        // relance la recherche (amortie — les frappes rapides
        // n'envoient que la dernière), le bouton « Chercher » garde
        // sa voie explicite
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

        addView(row(margin, queryField,
                button("Chercher", "lancer la recherche", this::doSearch),
                button("‹", "correspondance précédente", this::previous),
                button("›", "correspondance suivante", this::next),
                button("✕", "fermer la recherche", this::close)));
        addView(row(margin, replacementField,
                button("Remplacer", "remplacer la correspondance courante", this::replaceOne),
                button("Tout", "remplacer toutes les correspondances", this::replaceAll)));
        LayoutParams matchParams =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        matchParams.setMargins(dp(16), 0, margin, 0);
        addView(matchView, matchParams);
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

    private void doSearch(View button) {
        editor.search(queryField.getText().toString());
        updateCount();
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

    /** Le compte des correspondances, à la façon de la feuille d'origine. */
    private void updateCount() {
        if (editor == null) {
            return;
        }
        int total = editor.searchMatchCount();
        if (total == 0) {
            matchView.setText("aucune correspondance");
        } else {
            matchView.setText(String.format(Locale.ROOT,
                    "%d correspondance(s) · courante %d/%d",
                    total, editor.searchCurrentIndex() + 1, total));
        }
    }

    /**
     * La rangée mixte (un champ de saisie parmi les boutons) : le
     * champ pèse le double d'un bouton, la saisie garde la place.
     */
    private LinearLayout row(int margin, View... views) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        for (View view : views) {
            float weight = view instanceof EditText ? 2.0f : 1.0f;
            LayoutParams params =
                    new LayoutParams(0, LayoutParams.WRAP_CONTENT, weight);
            params.setMargins(0, 0, margin / 2, 0);
            row.addView(view, params);
        }
        return row;
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
