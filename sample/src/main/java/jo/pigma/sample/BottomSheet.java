package jo.pigma.sample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Feuille basse façon bottom sheet : le panneau des mesures du
 * sample vit collé au bas de l'écran, avec une poignée à tirer.
 *
 * <p>Trois états : fermé (seule la poignée dépasse), demi-plié (le
 * titre et les mesures tiennent), ouvert (le journal du scénario y
 * vit aussi). Le doigt tire la poignée pour passer d'un état à
 * l'autre ; l'écran peut y forcer l'état par programme. Le contenu
 * de l'éditeur ne bouge pas : la feuille se dessine par-dessus,
 * l'éditeur reste la vue de fond.</p>
 */
public final class BottomSheet extends ViewGroup {

    /** Les états de la feuille. */
    public enum State {

        /** Fermé : seule la poignée dépasse du bas. */
        COLLAPSED,

        /** Demi-plié : titre et mesures. */
        HALF_EXPANDED,

        /** Ouvert : le journal du scénario y vit aussi. */
        EXPANDED
    }

    /** L'auditeur des changements d'état. */
    public interface SheetListener {

        /**
         * L'état de la feuille vient de changer.
         *
         * @param state le nouvel état
         */
        void onSheetStateChanged(State state);
    }

    /** Hauteur de la poignée en dp. */
    private static final float HANDLE_HEIGHT_DP = 4.0f;

    /** Rayon des coins de la feuille en dp. */
    private static final float CORNER_RADIUS_DP = 16.0f;

    /** Le contenu de la feuille (l'enfant unique). */
    private LinearLayout content;

    /** La peinture du fond de la feuille. */
    private final Paint sheetPaint = new Paint();

    /** La peinture de la poignée. */
    private final Paint handlePaint = new Paint();

    /** L'état courant. */
    private State state = State.COLLAPSED;

    /** L'auditeur des changements (null : aucun). */
    private SheetListener sheetListener;

    /** La dernière ordonnée du doigt (le suivi du tiré). */
    private float lastTouchY;

    /**
     * Une feuille neuve, vide.
     *
     * @param context le contexte porteur
     */
    public BottomSheet(Context context) {
        super(context);
        sheetPaint.setColor(0xFFF6F3FA);
        handlePaint.setColor(0xFF49454F);
    }

    /**
     * Pose le contenu de la feuille.
     *
     * @param sheetContent le contenu (titre, mesures, journal)
     */
    public void setSheetContent(LinearLayout sheetContent) {
        content = sheetContent;
        removeAllViews();
        addView(content);
    }

    /**
     * Pose l'auditeur des changements d'état.
     *
     * @param listener l'auditeur (null : aucun)
     */
    public void setSheetListener(SheetListener listener) {
        sheetListener = listener;
    }

    /** L'état courant de la feuille. */
    public State sheetState() {
        return state;
    }

    /**
     * Force l'état de la feuille : l'auditeur y est prévenu.
     *
     * @param target l'état demandé
     */
    public void setSheetState(State target) {
        if (target == null || target == state) {
            return;
        }
        state = target;
        invalidate();
        if (sheetListener != null) {
            sheetListener.onSheetStateChanged(state);
        }
    }

    /**
     * La hauteur que la feuille montre à l'état donné (le calcul du
     * harnais y entre : les états montent par quarts de l'écran).
     *
     * @param target l'état
     * @return la hauteur montrée en pixels
     */
    public float visibleHeightPx(State target) {
        float quarter = getHeight() / 4.0f;
        switch (target) {
            case COLLAPSED:
                return HANDLE_HEIGHT_DP * dp() * 3.0f;
            case HALF_EXPANDED:
                return quarter;
            default:
                return quarter * 3.0f;
        }
    }

    /**
     * Le toucher : la poignée se tire (montée : ouvre d'un cran ;
     * descente : referme d'un cran) — la zone au-dessus de la
     * feuille retourne au contenu.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) {
            return false;
        }
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            lastTouchY = event.getY();
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            lastTouchY = event.getY();
            return true;
        }
        if (action == MotionEvent.ACTION_UP) {
            float pull = lastTouchY - event.getY();
            if (pull < -dp() * 8.0f) {
                stepState(-1);
            } else if (pull > dp() * 8.0f) {
                stepState(1);
            }
            return true;
        }
        return false;
    }

    /**
     * Avance ou recule l'état d'un cran.
     *
     * @param direction +1 ouvre, -1 referme
     */
    private void stepState(int direction) {
        int next = state.ordinal() + direction;
        State[] states = State.values();
        if (next >= 0 && next < states.length) {
            setSheetState(states[next]);
        }
    }

    /**
     * Le dessin : la feuille se dessine du bas de l'écran à la
     * hauteur de son état, coins ronds en haut, poignée au sommet.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        float density = dp();
        float shown = visibleHeightPx(state);
        if (shown <= 0.0f || getWidth() <= 0) {
            return;
        }
        float top = getHeight() - shown;
        canvas.drawRoundRect(0.0f, top, getWidth(), getHeight(),
                CORNER_RADIUS_DP * density, CORNER_RADIUS_DP * density, sheetPaint);
        float handleWidth = getWidth() / 5.0f;
        canvas.drawRoundRect(
                (getWidth() - handleWidth) / 2.0f,
                top + CORNER_RADIUS_DP * density / 2.0f,
                (getWidth() + handleWidth) / 2.0f,
                top + CORNER_RADIUS_DP * density / 2.0f
                        + HANDLE_HEIGHT_DP * density,
                HANDLE_HEIGHT_DP * density / 2.0f,
                HANDLE_HEIGHT_DP * density / 2.0f, handlePaint);
    }

    /** Le facteur de densité de l'écran. */
    private float dp() {
        return getResources().getDisplayMetrics().density;
    }

    /**
     * La disposition des enfants : la feuille de démonstration ne
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
