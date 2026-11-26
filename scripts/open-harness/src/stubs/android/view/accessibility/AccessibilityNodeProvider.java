package android.view.accessibility;

import android.os.Bundle;

import java.util.List;

/**
 * Le fournisseur de nœuds virtuels du harnais à stubs : la vue y
 * expose ses rangées comme enfants (le service qui parle les y
 * lit — le vrai cadre y vit, ici la forme suffit au harnais).
 */
public abstract class AccessibilityNodeProvider {

    /** Le nœud d'un identifiant virtuel (le service y lit). */
    public abstract AccessibilityNodeInfo createAccessibilityNodeInfo(
            int virtualViewId);

    /** L'action d'un nœud virtuel (le service y joue). */
    public boolean performAction(int virtualViewId, int action,
            Bundle arguments) {
        return false;
    }

    /** Le nœud portant le focus (le vrai cadre y vit, la nullité suffit). */
    public AccessibilityNodeInfo findFocus(int focus) {
        return null;
    }

    /** Les nœuds portant le texte (la nullité suffit au harnais). */
    public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(
            String text, int virtualViewId) {
        return List.of();
    }
}
