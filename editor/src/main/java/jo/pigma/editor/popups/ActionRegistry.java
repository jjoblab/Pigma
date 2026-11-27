package jo.pigma.editor.popups;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Le REGISTRE DES ACTIONS d'édition (R-A du rapport des popups) : la
 * carte d'actions rapides et l'étage « Édition » du débordement
 * servaient les MÊMES actions, chacune SA liste — la dérive y était
 * garantie (une action ajoutée à l'une manquait à l'autre). La
 * définition vit ICI, UNE SEULE FOIS : le libellé, la course, la
 * disponibilité (le contexte : une sélection vivante, un langage qui
 * commente) ; les deux cartes y mènent leurs rangées.
 *
 * <p>Le registre ne CONNAÎT ni le tampon ni la sélection : la
 * disponibilité se pose en prédicat (la vue y met SA condition du
 * moment), la course en {@link Runnable} (la vue y met SA méthode) —
 * pur et éprouvé en JVM.</p>
 */
public final class ActionRegistry {

    /** Une action d'édition : son libellé, sa nature, sa course, sa
     * disponibilité. */
    public static final class Action {

        /** Le libellé montré (les deux cartes le portent tel quel). */
        private final String label;

        /** La nature de l'action (son glyphe au trait à la carte
         * d'actions rapides — le débordement n'en montre pas). */
        private final QuickActionCard.Glyph glyph;

        /** La course de l'action (la méthode de la vue). */
        private final Runnable run;

        /** La disponibilité (le contexte du moment : sélection, langage). */
        private final BooleanSupplier available;

        private Action(String label, QuickActionCard.Glyph glyph,
                Runnable run, BooleanSupplier available) {
            this.label = label;
            this.glyph = glyph;
            this.run = run;
            this.available = available;
        }

        /** Le libellé de l'action. */
        public String label() {
            return label;
        }

        /** La nature de l'action (son glyphe au trait). */
        public QuickActionCard.Glyph glyph() {
            return glyph;
        }

        /** La course de l'action. */
        public void run() {
            run.run();
        }

        /** La disponibilité de l'action (vrai : les cartes la montrent). */
        public boolean isAvailable() {
            return available.getAsBoolean();
        }
    }

    /** Les actions enregistrées (l'ordre d'ajout y demeure). */
    private final List<Action> actions = new ArrayList<>();

    /**
     * Une action SANS nature dite s'enregistre (l'éclair d'action la
     * porte — la chaîne s'y prête).
     *
     * @param label     le libellé montré, jamais vide
     * @param run       la course, jamais null
     * @param available la disponibilité (le contexte du moment)
     * @return le registre (la chaîne)
     */
    public ActionRegistry add(String label, Runnable run,
            BooleanSupplier available) {
        return add(label, QuickActionCard.Glyph.ACTION, run, available);
    }

    /**
     * Une action s'enregistre avec SA NATURE (son glyphe au trait à
     * la carte d'actions rapides) — la chaîne s'y prête (les cinq se
     * posent d'un trait).
     *
     * @param label     le libellé montré, jamais vide
     * @param glyph     la nature de l'action (son icône au trait)
     * @param run       la course, jamais null
     * @param available la disponibilité (le contexte du moment)
     * @return le registre (la chaîne)
     */
    public ActionRegistry add(String label, QuickActionCard.Glyph glyph,
            Runnable run, BooleanSupplier available) {
        if (label != null && !label.isEmpty() && run != null
                && available != null) {
            actions.add(new Action(label,
                    glyph == null ? QuickActionCard.Glyph.ACTION : glyph,
                    run, available));
        }
        return this;
    }

    /**
     * Les actions VIVES : leur disponibilité le dit (les cartes n'ont
     * plus à connaître les conditions — l'étage se construit tel quel).
     *
     * @return les actions disponibles, jamais null
     */
    public List<Action> live() {
        List<Action> alive = new ArrayList<>();
        for (Action action : actions) {
            if (action.isAvailable()) {
                alive.add(action);
            }
        }
        return alive;
    }
}
