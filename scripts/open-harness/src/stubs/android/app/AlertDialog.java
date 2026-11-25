package android.app;

import android.view.View;

/**
 * Le dialogue du stub : la boîte des plantages du journal s'y
 * construit (le vrai cadre la pose par-dessus l'écran). Le harnais
 * y retient le dernier dialogue montré — le scénario du journal
 * des crashs y lit son contenu.
 */
public final class AlertDialog {

    /** Le dernier dialogue montré au harnais (null si aucun). */
    private static AlertDialog shown;

    /** Le corps du dialogue (le plantage affiché). */
    private final String title;

    /** Le texte porté par la vue du dialogue. */
    private final String body;

    private AlertDialog(String title, String body) {
        this.title = title;
        this.body = body;
    }

    /** Le dernier dialogue montré (null si aucun). */
    public static AlertDialog harnessShown() {
        return shown;
    }

    /** Le titre du dialogue. */
    public String harnessTitle() {
        return title;
    }

    /** Le texte du dialogue. */
    public String harnessBody() {
        return body;
    }

    /** Construit un dialogue (le Builder du cadre). */
    public static final class Builder {

        /** Le titre posé. */
        private String title;

        /** Le texte de la vue posée. */
        private String body = "";

        /** Un constructeur de dialogue. */
        public Builder(android.content.Context context) {
        }

        /**
         * Pose le titre.
         *
         * @param text le titre
         * @return le constructeur (chaîné)
         */
        public Builder setTitle(CharSequence text) {
            title = text == null ? "" : text.toString();
            return this;
        }

        /**
         * Pose la vue du dialogue : une vue de texte du stub y
         * retient son contenu (le plantage affiché).
         *
         * @param view la vue du corps
         * @return le constructeur (chaîné)
         */
        public Builder setView(View view) {
            if (view instanceof android.widget.TextView) {
                body = ((android.widget.TextView) view).getText()
                        .toString();
            }
            return this;
        }

        /**
         * Pose un bouton (rangé sans effet au harnais).
         *
         * @param which    le libellé du bouton
         * @param listener l'écouteur du clic
         * @return le constructeur (chaîné)
         */
        public Builder setPositiveButton(CharSequence which,
                android.content.DialogInterface.OnClickListener listener) {
            return this;
        }

        /**
         * Pose un bouton neutre (rangé sans effet au harnais).
         *
         * @param which    le libellé du bouton
         * @param listener l'écouteur du clic
         * @return le constructeur (chaîné)
         */
        public Builder setNeutralButton(CharSequence which,
                android.content.DialogInterface.OnClickListener listener) {
            return this;
        }

        /** Monte le dialogue (le harnais le retient). */
        public void show() {
            shown = new AlertDialog(title, body);
        }
    }
}
