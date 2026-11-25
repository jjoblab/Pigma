package androidx.drawerlayout.widget;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

/**
 * Tiroir de démonstration : le contenu et le menu s'y posent, le
 * menu se dit ouvert ou fermé, l'ouverture et la fermeture
 * préviennent l'auditeur — aucune coulisse réelle (le harnais
 * dimensionne la vue qu'il éprouve, le cadre réel porte le voile,
 * le geste et la largeur du menu).
 */
public class DrawerLayout extends ViewGroup {

    /** L'auditeur des mouvements du tiroir. */
    public interface DrawerListener {

        /**
         * Le tiroir vient de s'ouvrir ou de se refermer.
         *
         * @param drawerView le panneau du menu
         * @param isOpen     vrai si ouvert
         */
        void onDrawerOpened(View drawerView, boolean isOpen);
    }

    /**
     * Paramètres de disposition du tiroir : la gravité y règle (le
     * bord d'où le menu coulisse).
     */
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        /** La gravité du panneau (LEFT : le menu coulisse de gauche). */
        public int gravity;

        /**
         * Des paramètres sans gravité (le contenu du tiroir :
         * plein parent, le tiroir l'y pose).
         *
         * @param width  largeur demandée
         * @param height hauteur demandée
         */
        public LayoutParams(int width, int height) {
            super(width, height);
            this.gravity = Gravity.NO_GRAVITY;
        }

        /**
         * Des paramètres à gravité.
         *
         * @param width   largeur demandée
         * @param height  hauteur demandée
         * @param gravity le bord du panneau
         */
        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }
    }

    /** L'auditeur des mouvements (null : aucun). */
    private DrawerListener drawerListener;

    /** Le panneau du menu (celui à gravité gauche). */
    private View drawerPanel;

    /** L'état ouvert du menu. */
    private boolean open;

    /**
     * Un tiroir neuf, vide.
     *
     * @param context le contexte porteur
     */
    public DrawerLayout(Context context) {
        super(context);
    }

    /**
     * Pose l'auditeur des mouvements du tiroir.
     *
     * @param listener l'auditeur (null : aucun)
     */
    public void addDrawerListener(DrawerListener listener) {
        drawerListener = listener;
    }

    /** Le menu du tiroir est-il ouvert ? */
    public boolean isDrawerOpen(View drawer) {
        return open && drawer == drawerPanel;
    }

    /**
     * Ouvre le menu : l'auditeur y est prévenu.
     *
     * @param drawer le panneau du menu
     */
    public void openDrawer(View drawer) {
        drawerPanel = drawer;
        open = true;
        if (drawerListener != null) {
            drawerListener.onDrawerOpened(drawer, true);
        }
    }

    /**
     * Referme le menu : l'auditeur y est prévenu.
     *
     * @param drawer le panneau du menu
     */
    public void closeDrawer(View drawer) {
        open = false;
        if (drawerListener != null) {
            drawerListener.onDrawerOpened(drawer, false);
        }
    }
}
