package jo.pigma.sample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Conteneur à deux panneaux façon drawer : le contenu occupe
 * l'écran, le menu se coulisse par-dessus depuis la gauche (un
 * voile assombri couvre le contenu quand il est ouvert).
 *
 * <p>Le sample y range son menu : le thème, les serveurs de langage,
 * les documents hors normes — tout ce qui n'a pas besoin d'être
 * toujours visible. Le bouton-icône de la barre supérieure ouvre ;
 * le voile ou une action du menu referme. Le menu vit au-dessus du
 * contenu, jamais dans le flux : la disposition du contenu ne bouge
 * pas quand le menu s'ouvre.</p>
 */
public final class DrawerPane extends ViewGroup {

    /** L'auditeur des mouvements du menu. */
    public interface DrawerListener {

        /**
         * Le menu vient de s'ouvrir ou de se refermer.
         *
         * @param opened vrai si ouvert
         */
        void onDrawerOpened(boolean opened);
    }

    /** Largeur du menu en dp (l'apparence d'un drawer d'atelier). */
    private static final float DRAWER_WIDTH_DP = 280.0f;

    /** Le contenu de l'écran (le premier enfant). */
    private View content;

    /** Le menu coulissant (le second enfant). */
    private LinearLayout drawer;

    /** Le voile au-dessus du contenu quand le menu est ouvert. */
    private final Paint scrimPaint = new Paint();

    /** La peinture du fond du menu. */
    private final Paint drawerPaint = new Paint();

    /** L'état ouvert du menu. */
    private boolean opened;

    /** L'auditeur des mouvements (null : aucun). */
    private DrawerListener drawerListener;

    /**
     * Un conteneur neuf : le contenu et le menu s'y posent ensuite.
     *
     * @param context le contexte porteur
     */
    public DrawerPane(Context context) {
        super(context);
        scrimPaint.setColor(0x66000000);
        drawerPaint.setColor(0xFFF7F2FA);
    }

    /**
     * Pose le contenu et le menu : le contenu se dessine dessous, le
     * menu par-dessus. Deux enfants exactement.
     *
     * @param mainContent le contenu de l'écran
     * @param menu        le panneau du menu
     */
    public void setPanes(View mainContent, LinearLayout menu) {
        content = mainContent;
        drawer = menu;
        removeAllViews();
        addView(content);
        addView(drawer);
        drawer.setVisibility(opened ? View.VISIBLE : View.GONE);
    }

    /**
     * Pose une vue par-dessus le contenu (la feuille basse vit au
     * dessus de l'éditeur, hors du flux du contenu).
     *
     * @param overlay la vue de couche supérieure
     */
    public void addOverlay(View overlay) {
        addView(overlay);
    }

    /**
     * Pose l'auditeur des mouvements du menu.
     *
     * @param listener l'auditeur (null : aucun)
     */
    public void setDrawerListener(DrawerListener listener) {
        drawerListener = listener;
    }

    /** Le menu est-il ouvert ? */
    public boolean isDrawerOpen() {
        return opened;
    }

    /**
     * Ouvre ou referme le menu : l'auditeur y est prévenu du
     * changement.
     *
     * @param open vrai pour ouvrir
     */
    public void setDrawerOpen(boolean open) {
        if (opened == open) {
            return;
        }
        opened = open;
        if (drawer != null) {
            drawer.setVisibility(opened ? View.VISIBLE : View.GONE);
        }
        invalidate();
        if (drawerListener != null) {
            drawerListener.onDrawerOpened(opened);
        }
    }

    /**
     * Le toucher : ouvert, le voile referme le menu ; fermé, le
     * toucher descend au contenu.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (opened) {
            setDrawerOpen(false);
            return true;
        }
        return false;
    }

    /**
     * La largeur du menu en pixels (la densité y entre).
     *
     * @return la largeur du menu
     */
    public int drawerWidthPx() {
        return (int) (DRAWER_WIDTH_DP * getResources().getDisplayMetrics().density);
    }

    /**
     * Le dessin : le contenu d'abord, le voile et le menu ensuite
     * quand il est ouvert (le menu se dessine à gauche).
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (content == null || drawer == null) {
            return;
        }
        if (opened) {
            canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), scrimPaint);
            canvas.drawRect(0.0f, 0.0f, Math.min(drawerWidthPx(), getWidth()),
                    getHeight(), drawerPaint);
        }
    }

    /**
     * La disposition des enfants : le conteneur de démonstration ne
     * dispose rien (le harnais dimensionne la vue qu'il éprouve, le
     * cadre applicatif réel dispose l'arbre).
     *
     * @param changed la géométrie a-t-elle changé
     * @param left    bord gauche
     * @param top     bord haut
     * @param right   bord droit
     * @param bottom  bord bas
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    }
}
