package com.google.android.material.appbar;

import android.content.Context;
import android.view.View;

/**
 * Barre d'outils de démonstration : le titre et l'icône de
 * navigation s'y posent, le clic de navigation déclenche
 * l'auditeur — aucune mesure réelle (le harnais dimensionne la
 * vue qu'il éprouve).
 */
public class MaterialToolbar extends View {

    /** Le titre posé (le harnais n'y écrit rien). */
    private CharSequence title;

    /** L'auditeur du clic de navigation (null : aucun). */
    private View.OnClickListener navigationListener;

    /**
     * Une barre d'outils neuve.
     *
     * @param context le contexte porteur
     */
    public MaterialToolbar(Context context) {
        super(context);
    }

    /**
     * Pose le titre.
     *
     * @param text le titre
     */
    public void setTitle(CharSequence text) {
        title = text;
    }

    /** Le titre posé. */
    public CharSequence getTitle() {
        return title;
    }

    /**
     * Pose l'icône de navigation (la ressource se range, sans
     * effet au harnais).
     *
     * @param resId l'identifiant de ressource (0 : aucune)
     */
    public void setNavigationIcon(int resId) {
    }

    /**
     * Gonfle un menu (accepté, sans effet au harnais).
     *
     * @param resId l'identifiant du menu (0 : aucun)
     */
    public void inflateMenu(int resId) {
    }

    /**
     * Pose l'auditeur du clic de navigation.
     *
     * @param listener l'auditeur (null : aucun)
     */
    public void setNavigationOnClickListener(OnClickListener listener) {
        navigationListener = listener;
    }

    /**
     * Le clic de navigation : l'auditeur y répond (le bouton du
     * harnais le déclenche).
     *
     * @return vrai si un auditeur a couru
     */
    public boolean performNavigationClick() {
        if (navigationListener != null) {
            navigationListener.onClick(this);
            return true;
        }
        return false;
    }
}
