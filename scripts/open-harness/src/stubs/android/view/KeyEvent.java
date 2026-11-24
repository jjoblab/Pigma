package android.view;

/**
 * Événement de touche de démonstration : action et code — le harnais
 * en fabrique pour éprouver le clavier par événements.
 */
public class KeyEvent {

    /** La touche s'enfonce. */
    public static final int ACTION_DOWN = 0;

    /** La touche se relâche. */
    public static final int ACTION_UP = 1;

    /** Flèche haut. */
    public static final int KEYCODE_DPAD_UP = 19;

    /** Flèche bas. */
    public static final int KEYCODE_DPAD_DOWN = 20;

    /** Flèche gauche. */
    public static final int KEYCODE_DPAD_LEFT = 21;

    /** Flèche droite. */
    public static final int KEYCODE_DPAD_RIGHT = 22;

    /** Entrée principale. */
    public static final int KEYCODE_ENTER = 66;

    /** Retour arrière. */
    public static final int KEYCODE_DEL = 67;

    /** Suppression avant. */
    public static final int KEYCODE_FORWARD_DEL = 112;

    /** Entrée du pavé numérique. */
    public static final int KEYCODE_NUMPAD_ENTER = 160;

    private final int action;
    private final int keyCode;

    /**
     * Un événement de touche.
     *
     * @param action l'action (DOWN ou UP)
     * @param keyCode le code de touche
     */
    public KeyEvent(int action, int keyCode) {
        this.action = action;
        this.keyCode = keyCode;
    }

    /** L'action. */
    public int getAction() {
        return action;
    }

    /** Le code de touche. */
    public int getKeyCode() {
        return keyCode;
    }
}
