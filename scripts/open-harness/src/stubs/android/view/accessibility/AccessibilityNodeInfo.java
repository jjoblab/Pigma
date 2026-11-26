package android.view.accessibility;

import java.util.ArrayList;
import java.util.List;

/**
 * Le nœud d'accessibilité du harnais à stubs : la vue éditeur y
 * remplit sa description (le service qui parle la lit — le vrai
 * cadre y vit, ici la forme suffit au harnais).
 */
public class AccessibilityNodeInfo {

    /** Une action du nœud (la forme du vrai cadre, la valeur suffit). */
    public static final class AccessibilityAction {

        /** L'action de cliquer. */
        public static final AccessibilityAction ACTION_CLICK =
                new AccessibilityAction("click");
        /** L'action de sélectionner. */
        public static final AccessibilityAction ACTION_SELECT =
                new AccessibilityAction("select");
        /** L'action de ranger la sélection. */
        public static final AccessibilityAction ACTION_CLEAR_SELECTION =
                new AccessibilityAction("clearSelection");
        /** L'action de couper. */
        public static final AccessibilityAction ACTION_CUT =
                new AccessibilityAction("cut");
        /** L'action de copier. */
        public static final AccessibilityAction ACTION_COPY =
                new AccessibilityAction("copy");
        /** L'action de coller. */
        public static final AccessibilityAction ACTION_PASTE =
                new AccessibilityAction("paste");
        /** L'action de poser le texte. */
        public static final AccessibilityAction ACTION_SET_TEXT =
                new AccessibilityAction("setText");
        /** L'action du prochain cran de mouvement. */
        public static final AccessibilityAction ACTION_NEXT_AT_MOVEMENT_GRANULARITY =
                new AccessibilityAction("nextAtMovementGranularity");
        /** L'action du cran précédent. */
        public static final AccessibilityAction ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY =
                new AccessibilityAction("previousAtMovementGranularity");

        private final String name;

        private AccessibilityAction(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private CharSequence className;
    private boolean editable;
    private CharSequence text;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private final List<AccessibilityAction> actions = new ArrayList<>();

    /** La classe déclarée du nœud. */
    public void setClassName(CharSequence name) {
        this.className = name;
    }

    /** La classe déclarée. */
    public CharSequence getClassName() {
        return className;
    }

    /** La vue est-elle éditable ? */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /** La vue est-elle éditable ? */
    public boolean isEditable() {
        return editable;
    }

    /** Le texte du nœud. */
    public void setText(CharSequence text) {
        this.text = text;
    }

    /** Le texte du nœud. */
    public CharSequence getText() {
        return text;
    }

    /** La sélection posée. */
    public void setTextSelection(int start, int end) {
        this.selectionStart = start;
        this.selectionEnd = end;
    }

    /** Le début de la sélection. */
    public int getTextSelectionStart() {
        return selectionStart;
    }

    /** La fin de la sélection. */
    public int getTextSelectionEnd() {
        return selectionEnd;
    }

    /** Une action s'ajoute. */
    public void addAction(AccessibilityAction action) {
        actions.add(action);
    }

    /** Les actions du nœud. */
    public List<AccessibilityAction> getActionList() {
        return actions;
    }
}
