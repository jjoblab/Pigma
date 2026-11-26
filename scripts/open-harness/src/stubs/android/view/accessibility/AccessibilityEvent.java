package android.view.accessibility;

import java.util.ArrayList;
import java.util.List;

/**
 * L'événement d'accessibilité du harnais à stubs : la frappe et le
 * déplacement de sélection s'y remplissent (le vrai cadre y vit).
 */
public class AccessibilityEvent {

    /** Le texte a changé. */
    public static final int TYPE_VIEW_TEXT_CHANGED = 0x00000010;

    /** La sélection de texte a changé. */
    public static final int TYPE_VIEW_TEXT_SELECTION_CHANGED = 0x00000020;

    private int eventType;
    private int addedCount;
    private int fromIndex;
    private int toIndex;
    private final List<CharSequence> text = new ArrayList<>();

    /**
     * Un événement du type donné.
     *
     * @param type le type de l'événement
     * @return l'événement prêt
     */
    public static AccessibilityEvent obtain(int type) {
        AccessibilityEvent event = new AccessibilityEvent();
        event.eventType = type;
        return event;
    }

    /** Le type de l'événement. */
    public int getEventType() {
        return eventType;
    }

    /** Le nombre de caractères ajoutés. */
    public void setAddedCount(int count) {
        this.addedCount = count;
    }

    /** Le nombre de caractères ajoutés. */
    public int getAddedCount() {
        return addedCount;
    }

    /** L'indice de départ. */
    public void setFromIndex(int index) {
        this.fromIndex = index;
    }

    /** L'indice de départ. */
    public int getFromIndex() {
        return fromIndex;
    }

    /** L'indice d'arrivée. */
    public void setToIndex(int index) {
        this.toIndex = index;
    }

    /** L'indice d'arrivée. */
    public int getToIndex() {
        return toIndex;
    }

    /** Les textes de l'événement. */
    public List<CharSequence> getText() {
        return text;
    }

    /** L'événement se rend au réservoir (sans effet ici). */
    public void recycle() {
    }
}
