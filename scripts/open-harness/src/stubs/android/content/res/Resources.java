package android.content.res;

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
}
