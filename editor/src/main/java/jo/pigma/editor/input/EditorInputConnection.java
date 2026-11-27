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
                    // la composition se clôt D'ABORD (le défaut du
                    // terrain : l'Entrée d'un clavier par événements
                    // laissait le texte composé fantôme derrière le
                    // saut de ligne — l'IME croyait sa composition
                    // vivante)
                    editor.clearComposing();
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
                case KeyEvent.KEYCODE_TAB:
                    // LE FANTÔME SE POSE (B4) : la touche TAB accepte la
                    // proposition entière au caret (rien ne vivait : la
                    // touche reprend sa vie d'avant)
                    if (editor.acceptGhostText()) {
                        return true;
                    }
                    break;
                default:
                    break;
            }
        }
        return super.sendKeyEvent(event);
    }

    /**
     * La touche action du clavier (Entrée) : un saut de ligne —
     * l'éditeur ne déclare pas d'action spéciale. La composition en
     * cours se clôt d'abord (le même fantôme que l'Entrée par
     * événements s'y posait).
     */
    @Override
    public boolean performEditorAction(int actionCode) {
        editor.clearComposing();
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

    /**
     * Le texte AVANT le curseur (le pont complet du protocole) : la
     * prédiction du clavier s'y nourrit (la début de phrase, le mot
     * en cours). Sans ces lectures, un clavier qui demande le contexte
     * recevait une chaîne vide du mode factice de la classe de base —
     * ses prédictions tombaient.
     *
     * @param length le nombre de caractères demandés
     * @param flags  les drapeaux (ignorés : la zone de composition se
     *               retourne telle quelle, l'éditeur n'a pas de
     *               marqueurs de style)
     * @return le texte avant le curseur (tronqué au début du document)
     */
    @Override
    public CharSequence getTextBeforeCursor(int length, int flags) {
        int caret = editor.composingStart() >= 0
                ? editor.composingStart() : editor.caret();
        int start = Math.max(0, caret - Math.max(0, length));
        return editor.textBetween(start, caret);
    }

    /**
     * Le texte APRÈS le curseur (le pont complet du protocole).
     *
     * @param length le nombre de caractères demandés
     * @param flags  les drapeaux (ignorés)
     * @return le texte après le curseur (tronqué à la fin du document)
     */
    @Override
    public CharSequence getTextAfterCursor(int length, int flags) {
        int caret = editor.composingEnd() >= 0
                ? editor.composingEnd() : editor.caret();
        int end = Math.min(editor.length(), caret + Math.max(0, length));
        return editor.textBetween(caret, end);
    }

    /**
     * La zone de composition se REPOSE (le clavier y surligne le mot
     * qu'il prédict) : l'éditeur en tenait une par ses mises à jour,
     * le clavier peut en poser une lui-même (Gboard au corrigeur
     * gestuel y passe).
     *
     * @param start le début de la zone
     * @param end   la fin de la zone
     * @return vrai (la zone se pose)
     */
    @Override
    public boolean setComposingRegion(int start, int end) {
        int from = Math.max(0, Math.min(start, end));
        int to = Math.min(editor.length(), Math.max(start, end));
        if (from >= to) {
            editor.clearComposing();
            return true;
        }
        editor.setComposing(from, to);
        return true;
    }

    /**
     * Les majuscules contextuelles (le pont complet du protocole) : la
     * classe de base ne regarde que le vrai champ d'EditText — ici le
     * tampon se lit, la phrase se devine au point précédent.
     *
     * @param reqModes les modes demandés (le clavier n'en veut qu'un à
     *                 la fois, le protocole y passe un masque)
     * @return les modes satisfaits (le masque entrant y est masqué)
     */
    @Override
    public int getCursorCapsMode(int reqModes) {
        int caret = editor.composingStart() >= 0
                ? editor.composingStart() : editor.caret();
        int scan = Math.max(0, caret - 64);
        CharSequence head = editor.textBetween(scan, caret);
        boolean sentenceStart = true;
        for (int at = head.length() - 1; at >= 0; at--) {
            char sign = head.charAt(at);
            if (sign == ' ' || sign == '\n' || sign == '\t') {
                continue;
            }
            sentenceStart = sign == '.' || sign == '!' || sign == '?'
                    || sign == ':';
            break;
        }
        if (sentenceStart) {
            return reqModes & android.text.TextUtils.CAP_MODE_SENTENCES;
        }
        boolean wordStart = true;
        for (int at = head.length() - 1; at >= 0; at--) {
            char sign = head.charAt(at);
            if (sign == ' ' || sign == '\n' || sign == '\t') {
                break;
            }
            wordStart = false;
            break;
        }
        return wordStart ? reqModes & android.text.TextUtils.CAP_MODE_WORDS : 0;
    }
}
