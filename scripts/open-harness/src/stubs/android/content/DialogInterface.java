package android.content;

/**
 * L'interface d'écoute des clics de dialogue (le stub range les
 * écouteurs, l'appareil y déclenche).
 */
public interface DialogInterface {

    /** L'écouteur d'un clic de bouton de dialogue. */
    interface OnClickListener {

        /**
         * Un bouton du dialogue reçoit son clic.
         *
         * @param dialog       le dialogue porteur
         * @param whichButton le bouton cliqué
         */
        void onClick(DialogInterface dialog, int whichButton);
    }
}
