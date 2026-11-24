package android.widget;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Notification de démonstration : les affichages se collectent — le
 * harnais lit ce qui fut notifié.
 */
public class Toast {

    /** Durée courte. */
    public static final int LENGTH_SHORT = 0;

    /** Durée longue. */
    public static final int LENGTH_LONG = 1;

    private static final List<CharSequence> SHOWN = new ArrayList<>();

    private final CharSequence text;

    private Toast(CharSequence text) {
        this.text = text;
    }

    /**
     * Fabrique une notification.
     *
     * @param context le contexte (sans effet ici)
     * @param text le texte notifié
     * @param duration durée indicative
     * @return la notification
     */
    public static Toast makeText(Context context, CharSequence text, int duration) {
        return new Toast(text);
    }

    /** Affiche la notification (l'enregistre). */
    public void show() {
        SHOWN.add(text);
    }

    /** Les textes notifiés depuis l'origine. */
    public static List<CharSequence> harnessShown() {
        return new ArrayList<>(SHOWN);
    }
}
