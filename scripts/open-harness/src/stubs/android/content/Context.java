package android.content;

import android.content.res.Resources;
import android.view.inputmethod.InputMethodManager;

/**
 * Contexte de démonstration : les services se servent depuis un
 * registre fixe (presse-papiers, méthode de saisie), les ressources
 * sont uniques.
 */
public class Context {

    /** Nom du service presse-papiers. */
    public static final String CLIPBOARD_SERVICE = "clipboard";

    /** Nom du service de méthode de saisie. */
    public static final String INPUT_METHOD_SERVICE = "input_method";

    private static final ClipboardManager CLIPBOARD = new ClipboardManager();
    private static final InputMethodManager INPUT_METHOD = new InputMethodManager();
    private static final Resources RESOURCES = new Resources();

    /**
     * Le service demandé.
     *
     * @param name nom du service
     * @return l'instance du registre, ou null si inconnu
     */
    public Object getSystemService(String name) {
        if (CLIPBOARD_SERVICE.equals(name)) {
            return CLIPBOARD;
        }
        if (INPUT_METHOD_SERVICE.equals(name)) {
            return INPUT_METHOD;
        }
        return null;
    }

    /** Les ressources (métriques fixes). */
    public Resources getResources() {
        return RESOURCES;
    }

    /** Le presse-papiers du registre (le harnais y lit la copie). */
    public static ClipboardManager harnessClipboard() {
        return CLIPBOARD;
    }

    /** La méthode de saisie du registre. */
    public static InputMethodManager harnessInputMethod() {
        return INPUT_METHOD;
    }
}
