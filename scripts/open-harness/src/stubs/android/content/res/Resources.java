package android.content.res;

import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

/**
 * Ressources de démonstration : les métriques d'écran, densité
 * fixée à 2 (un téléphone courant).
 */
public final class Resources {

    private final DisplayMetrics metrics = new DisplayMetrics();

    /** Les ressources de démonstration : densité 2. */
    public Resources() {
        metrics.density = 2.0f;
        metrics.scaledDensity = 2.0f;
    }

    /** Les métriques d'écran. */
    public DisplayMetrics getDisplayMetrics() {
        return metrics;
    }

    /**
     * Le drawable d'identifiant donné : un drawable vide de
     * démonstration (l'appareil y charge l'icône réelle).
     *
     * @param id l'identifiant de ressource
     * @return un drawable quelconque, jamais null
     */
    public Drawable getDrawable(int id) {
        return new Drawable() {
        };
    }

    /**
     * Le drawable d'identifiant donné, pour un thème : la variante
     * du cadre, le thème y est rangé sans effet.
     *
     * @param id    l'identifiant de ressource
     * @param theme le thème porteur (ignoré au harnais)
     * @return un drawable quelconque, jamais null
     */
    public Drawable getDrawable(int id, Object theme) {
        return new Drawable() {
        };
    }
}
