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
 * toujours visible. La poignée d'ouverture vit dans la barre
 * supérieure du contenu (l'écran la pose par {@link #setHandle});
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

    /** La couche supérieure par-dessus le contenu (la feuille basse). */
    private View overlay;

    /** La poignée d'ouverture du menu (le bouton de la barre). */
    private View handle;

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
        overlay = null;
        rebuildChildren();
        drawer.setVisibility(opened ? View.VISIBLE : View.GONE);
        requestLayout();
        invalidate();
    }

    /**
     * Pose une vue par-dessus le contenu (la feuille basse vit au
     * dessus de l'éditeur, hors du flux du contenu) : le menu reste
     * le DERNIER enfant — le z-order y suit l'ordre d'ajout, la
     * feuille s'insère avant lui.
     *
     * @param sheet la vue de couche supérieure
     */
    public void addOverlay(View sheet) {
        overlay = sheet;
        rebuildChildren();
        requestLayout();
        invalidate();
    }

    /**
     * Reconstruit l'ordre des enfants : contenu, couche, menu — le
     * menu se dessine et se touche le dernier (au-dessus de tout).
     */
    private void rebuildChildren() {
        removeAllViews();
        if (content != null) {
            addView(content);
        }
        if (overlay != null) {
            addView(overlay);
        }
        if (drawer != null) {
            addView(drawer);
        }
    }

    /**
     * Pose la poignée d'ouverture du menu (le bouton-icône de la
     * barre supérieure du contenu — l'écran l'a déjà posé dans son
     * arbre ; le conteneur n'y fait que le retenir pour le toucher).
     *
     * @param opener la vue qui ouvre le menu
     */
    public void setHandle(View opener) {
        handle = opener;
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
        requestLayout();
        invalidate();
        if (drawerListener != null) {
            drawerListener.onDrawerOpened(opened);
        }
    }

    /**
     * Le toucher : ouvert, le voile referme le menu ; fermé, le
     * toucher descend au contenu (le menu au-dessus ne vit que
     * ouvert).
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
     * L'interception du toucher : le menu ouvert prend TOUT le geste
     * au-dessus de sa largeur (le voile compris) — le contenu ne
     * touche plus à rien tant que le menu vit.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return opened;
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
     * La mesure : le conteneur épouse la contrainte qu'on lui donne
     * (l'écran plein), le contenu et la couche la partagent, le menu
     * demande sa largeur et la hauteur de l'écran.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
        if (content != null) {
            measureChild(content, widthMeasureSpec, heightMeasureSpec);
        }
        if (overlay != null) {
            measureChild(overlay, widthMeasureSpec, heightMeasureSpec);
        }
        if (drawer != null && drawer.getVisibility() != View.GONE) {
            drawer.measure(
                    View.MeasureSpec.makeMeasureSpec(drawerWidthPx(),
                            View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(width, height);
    }

    /**
     * La disposition : le contenu et la couche tiennent l'écran, le
     * menu ouvert se pose par-dessus à gauche.
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (content != null) {
            content.layout(left, top, right, bottom);
        }
        if (overlay != null) {
            overlay.layout(left, top, right, bottom);
        }
        if (drawer != null && drawer.getVisibility() != View.GONE) {
            int drawerWidth = Math.min(drawerWidthPx(), right - left);
            drawer.layout(left, top, left + drawerWidth, bottom);
        }
    }

    /**
     * Le dessin du conteneur : le fond d'abord (le contenu et la
     * couche se dessinent par {@link #dispatchDraw}), le voile et le
     * fond du menu ensuite — les enfants du cadre se dessinent d'eux-
     * mêmes dans l'ordre d'ajout, le menu y reste le dernier.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (!opened) {
            return;
        }
        canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), scrimPaint);
        canvas.drawRect(0.0f, 0.0f, Math.min(drawerWidthPx(), getWidth()),
                getHeight(), drawerPaint);
    }

    /**
     * La poignée d'ouverture vit-elle à l'écran ? Le scénario de la
     * refonte y répond (le bouton-hamburger de la barre supérieure
     * ouvre le menu).
     *
     * @return vrai si la poignée existe
     */
    boolean handleSet() {
        return handle != null;
    }
}
