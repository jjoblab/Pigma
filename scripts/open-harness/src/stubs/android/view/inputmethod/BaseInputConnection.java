package android.view.inputmethod;

import android.view.KeyEvent;
import android.view.View;

/**
 * Connexion factice de démonstration : tout se refuse — les vues
 * éditables surchargent les méthodes utiles, le reste reste muet
 * (le mode factice ne consomme rien par lui-même).
 */
public class BaseInputConnection implements InputConnection {

    /** La vue cible. */
    protected final View targetView;

    /**
     * Une connexion factice.
     *
     * @param targetView la vue cible
     * @param fullEditor vrai pour un éditeur complet (sans effet ici)
     */
    public BaseInputConnection(View targetView, boolean fullEditor) {
        this.targetView = targetView;
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        return false;
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        return false;
    }

    @Override
    public boolean deleteSurroundingText(int leftLength, int rightLength) {
        return false;
    }

    @Override
    public boolean finishComposingText() {
        return false;
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        return false;
    }

    @Override
    public boolean performEditorAction(int actionCode) {
        return false;
    }

    @Override
    public boolean setSelection(int start, int end) {
        return false;
    }

    @Override
    public CharSequence getSelectedText(int flags) {
        return null;
    }
}
