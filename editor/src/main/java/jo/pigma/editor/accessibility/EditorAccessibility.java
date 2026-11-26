package jo.pigma.editor.accessibility;

import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import jo.pigma.editor.EditorView;

/**
 * L'accessibilité de la vue (D5 du rapport) : le service qui parle
 * (TalkBack) y lit la vue comme un éditeur de texte — son contenu
 * borné, sa position de curseur, ses actions de mouvement. Sans ce
 * pont, la vue se taisait : aucun texte, aucune action, aucun
 * événement de frappe.
 *
 * <p>La classe porte les détails du protocole ; la vue n'y expose
 * que ce qu'elle sait (le tampon, la sélection, les lignes). Le
 * texte se BORNE (le service ne lit pas un fichier de 50 000 lignes
 * en une phrase) : la fenêtre autour du curseur y suffit.</p>
 */
public final class EditorAccessibility {

    /** La fenêtre de texte annoncée (le service lit autour, pas tout). */
    private static final int ANNOUNCE_WINDOW = 400;

    private final EditorView view;

    private EditorAccessibility(EditorView view) {
        this.view = view;
    }

    /**
     * Le descripteur de la vue.
     *
     * @param view la vue servie
     * @return le descripteur posé
     */
    public static EditorAccessibility of(EditorView view) {
        return new EditorAccessibility(view);
    }

    /**
     * Remplit le nœud d'accessibilité (le service y lit la vue).
     *
     * @param node le nœud à remplir
     */
    public void fillNode(AccessibilityNodeInfo node) {
        node.setClassName("android.widget.EditText");
        node.setEditable(true);
        node.setText(announcedText());
        if (view.hasSelection()) {
            node.setTextSelection(view.selectionStart(), view.selectionEnd());
        } else {
            node.setTextSelection(view.getCaretOffset(), view.getCaretOffset());
        }
        // les actions du protocole : les mouvements du curseur, la
        // sélection, la coupure, le collage et le lecture suivante
        node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
        node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SELECT);
        node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_SELECTION);
        node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CUT);
        node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_COPY);
        node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_PASTE);
        node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT);
        node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_NEXT_AT_MOVEMENT_GRANULARITY);
        node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY);
    }

    /**
     * L'événement TYPE_VIEW_TEXT_CHANGED se remplit (le service y
     * entend la frappe).
     *
     * @param event l'événement à remplir
     */
    public void fillTextChanged(AccessibilityEvent event) {
        event.setAddedCount(1);
        event.setFromIndex(view.getCaretOffset() - 1);
        event.getText().add(announcedText());
    }

    /**
     * L'événement TYPE_VIEW_TEXT_SELECTION_CHANGED se remplit.
     *
     * @param event l'événement à remplir
     */
    public void fillSelectionChanged(AccessibilityEvent event) {
        event.setFromIndex(Math.min(view.selectionStart(), view.selectionEnd()));
        event.setToIndex(Math.max(view.selectionStart(), view.selectionEnd()));
        event.getText().add(announcedText());
    }

    /**
     * Le texte annoncé : la fenêtre autour du curseur (le service
     * parle une phrase, pas un document).
     *
     * @return le texte borné, avec l'ellipse aux deux bouts
     */
    public CharSequence announcedText() {
        int length = view.getBuffer().length();
        int caret = Math.min(view.getCaretOffset(), length);
        int from = Math.max(0, caret - ANNOUNCE_WINDOW);
        int to = Math.min(length, caret + ANNOUNCE_WINDOW);
        CharSequence window = view.getBuffer().getText(from, to);
        if (from == 0 && to == length) {
            return window;
        }
        StringBuilder builder = new StringBuilder(window.length() + 8);
        if (from > 0) {
            builder.append('…');
        }
        builder.append(window);
        if (to < length) {
            builder.append('…');
        }
        return builder;
    }
}
