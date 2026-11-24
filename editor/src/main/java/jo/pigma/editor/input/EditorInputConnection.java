package jo.pigma.editor.input;

import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;

/**
 * Connexion IME (le paquet input, l'arborescence du prompt) : chaque
 * retour du clavier est une transaction, donc une unité d'annulation.
 * La zone de composition est remplacée à chaque mise à jour de l'IME
 * puis validée telle quelle au commit.
 *
 * <p>La vue n'est ici que par le contrat ({@link EditorContract}) —
 * jamais par ses membres.</p>
 */
public final class EditorInputConnection extends BaseInputConnection {

    private final EditorContract editor;

    /**
     * Une connexion neuve.
     *
     * @param target la vue cible (le port du clavier)
     * @param contract le contrat interne de l'éditeur
     */
    public EditorInputConnection(View target, EditorContract contract) {
        super(target, false);
        editor = contract;
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        int start = editor.composingStart() >= 0 ? editor.composingStart()
                : editor.hasSelection() ? editor.selectionStart() : editor.caret();
        int end = editor.composingEnd() >= 0 ? editor.composingEnd()
                : editor.hasSelection() ? editor.selectionEnd() : editor.caret();
        editor.applyEdit(start, end, text);
        editor.clearComposing();
        return true;
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        int start = editor.composingStart() >= 0 ? editor.composingStart()
                : editor.caret();
        int end = editor.composingEnd() >= 0 ? editor.composingEnd()
                : editor.caret();
        editor.applyEdit(start, end, text);
        editor.setComposing(start,
                Math.min(start + text.length(), editor.length()));
        return true;
    }

    @Override
    public boolean deleteSurroundingText(int leftLength, int rightLength) {
        editor.deleteAround(leftLength, rightLength);
        return true;
    }

    @Override
    public boolean finishComposingText() {
        editor.clearComposing();
        return true;
    }

    /**
     * Les claviers qui passent par les événements (Suppr, Entrée,
     * flèches) : le mode factice de la classe de base les perdrait —
     * la connexion traduit, le clavier ne peut pas être muet.
     */
    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DEL:
                    editor.deleteAround(1, 0);
                    return true;
                case KeyEvent.KEYCODE_FORWARD_DEL:
                    editor.deleteAround(0, 1);
                    return true;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    editor.applyEdit(editor.caret(), editor.caret(), "\n");
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    editor.moveCaret(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    editor.moveCaret(1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    editor.moveCaretVertically(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    editor.moveCaretVertically(1);
                    return true;
                default:
                    break;
            }
        }
        return super.sendKeyEvent(event);
    }

    /**
     * La touche action du clavier (Entrée) : un saut de ligne —
     * l'éditeur ne déclare pas d'action spéciale.
     */
    @Override
    public boolean performEditorAction(int actionCode) {
        editor.applyEdit(editor.hasSelection()
                ? editor.selectionStart() : editor.caret(),
                editor.hasSelection() ? editor.selectionEnd() : editor.caret(),
                "\n");
        return true;
    }

    /**
     * Le clavier déplace la sélection (le champ de recherche du
     * clavier, la poignée du texte sélectionné) : l'ancre et le
     * curseur suivent, la sélection vient.
     */
    @Override
    public boolean setSelection(int start, int end) {
        editor.setSelection(start, end);
        return true;
    }

    /**
     * Le texte sélectionné (le copier du clavier le demande).
     */
    @Override
    public CharSequence getSelectedText(int flags) {
        return editor.hasSelection() ? editor.selectedText() : null;
    }
}
