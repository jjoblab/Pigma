package com.google.android.material.appbar;

import android.content.Context;
import android.graphics.drawable.Drawable;
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

    /** L'icône de navigation posée (le harnais n'en dessine pas). */
    private Drawable navigationIcon;

    /** La description d'accessibilité de l'icône posée. */
    private CharSequence navigationDescription;

    /**
     * Pose l'icône de navigation (un drawable du cadre : le
     * glyphe du hamburger sur l'écran réel).
     *
     * @param icon l'icône (null : aucune)
     */
    public void setNavigationIcon(Drawable icon) {
        navigationIcon = icon;
    }

    /** L'icône de navigation posée (null : aucune). */
    public Drawable getNavigationIcon() {
        return navigationIcon;
    }

    /**
     * Pose la description d'accessibilité de l'icône de
     * navigation.
     *
     * @param description la description
     */
    public void setNavigationContentDescription(CharSequence description) {
        navigationDescription = description;
    }

    /** La description d'accessibilité posée. */
    public CharSequence getNavigationContentDescription() {
        return navigationDescription;
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
