package jo.pigma.editor.accessibility;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import java.util.List;

import jo.pigma.editor.EditorView;
import jo.pigma.editor.popups.CardBase;
import jo.pigma.editor.popups.VirtualRows;

/**
 * LE FOURNISSEUR DES RANGÉES VIRTUELLES (D-P9 du rapport des popups
 * — « exposer les rangées comme nœuds virtuels ») : le service qui
 * parle y lit les rangées de la CARTE VIVANTE — chaque rangée
 * s'expose comme un enfant du nœud de l'éditeur (le libellé s'y
 * lit, la bornes s'y posent, l'action s'y joue — la MÊME action
 * que le doigt). La carte vivante s'y trouve ({@link VirtualRows})
 * : les cartes à rangées portent l'interface, la vue sait laquelle
 * vit.
 *
 * <p>Le nœud de l'ÉDITEUR garde sa place (le service y lit l'éditeur
 * — le texte, le curseur, ses actions : {@link EditorAccessibility})
 * ; les rangées s'y AJOUTENT comme enfants virtuels — la lecture
 * suit : le titre de la carte, chaque rangée, l'action à la rangée.</p>
 */
public final class PopupAccessibility {

    /** L'identifiant du nœur racine (l'éditeur lui-même). */
    private static final int HOST_ID = 0;

    /** La vue porteuse (la carte vivante s'y lit). */
    private final EditorView view;

    private PopupAccessibility(EditorView view) {
        this.view = view;
    }

    /**
     * Le descripteur de la vue.
     *
     * @param view la vue servie
     * @return le descripteur posé
     */
    public static PopupAccessibility of(EditorView view) {
        return new PopupAccessibility(view);
    }

    /**
     * Le fournisseur des nœuds virtuels (les rangées de la carte
     * vivante s'y exposent comme enfants — le service qui parle y
     * navigue au doigt du lecteur d'écran).
     *
     * @return le fournisseur, null quand aucune carte à rangées ne
     *         vit (le nœud de l'éditeur reste seul)
     */
    public AccessibilityNodeProvider provider() {
        VirtualRows card = view.activeVirtualRows();
        if (card == null) {
            return null;
        }
        return new AccessibilityNodeProvider() {
            @Override
            public AccessibilityNodeInfo createAccessibilityNodeInfo(
                    int virtualViewId) {
                if (virtualViewId == HOST_ID) {
                    // le nœud racine reste à l'éditeur (le service y
                    // lit le texte — l'autre descripteur le remplit)
                    return AccessibilityNodeInfo.obtain(view);
                }
                return rowNode(card, virtualViewId - 1);
            }

            @Override
            public boolean performAction(int virtualViewId, int action,
                    Bundle arguments) {
                if (action == AccessibilityNodeInfo.ACTION_CLICK
                        && virtualViewId > HOST_ID) {
                    return card.virtualRowActivate(virtualViewId - 1);
                }
                return false;
            }

            @Override
            public AccessibilityNodeInfo findFocus(int focus) {
                return null;
            }

            @Override
            public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(
                    String text, int virtualViewId) {
                return List.of();
            }
        };
    }

    /**
     * Le nœud d'une rangée : le LIBELLÉ s'y lit (le service qui parle
     * le range), les bornes s'y posent (la place de la rangée à
     * l'écran), l'action s'y joue (le clic — la MÊME action que le
     * doigt).
     *
     * @param card  la carte vivante
     * @param index l'indice de la rangée VISIBLE
     * @return le nœud de la rangée, null hors fenêtre
     */
    private AccessibilityNodeInfo rowNode(VirtualRows card, int index) {
        String label = card.virtualRowLabel(index);
        if (index < 0 || label == null || label.isEmpty()) {
            return null;
        }
        AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain();
        node.setClassName("android.widget.Button");
        node.setText(label);
        node.setContentDescription(label);
        node.setEnabled(true);
        node.setClickable(true);
        node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
        node.setVisibleToUser(true);
        // les bornes de la rangée : la carte les porte (la rangée y
        // vit à sa place — la moitié haute et basse du cran)
        if (card instanceof CardBase) {
            CardBase drawn = (CardBase) card;
            float cy = card.virtualRowCenterY(index);
            float side = 24.0f * view.getContext().getResources()
                    .getDisplayMetrics().density;
            node.setBoundsInParent(new Rect(
                    (int) drawn.poseX(), (int) (cy - side),
                    (int) (drawn.poseX() + drawn.width()),
                    (int) (cy + side)));
        }
        return node;
    }
}
