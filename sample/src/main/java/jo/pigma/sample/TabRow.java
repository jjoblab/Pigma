package jo.pigma.sample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Barre d'onglets façon atelier moderne : une rangée horizontale de
 * segments, l'actif porte l'indicateur souligné, le reste reste plat.
 *
 * <p>Le sample y pose ses documents de démonstration (un onglet par
 * scénario de texte : Java, XML, JSON, Kotlin, Lua, SQL). Le clic
 * choisit l'onglet et notifie l'auditeur ; la sélection se change
 * aussi par programme. Le dessin reste sobre : le texte au centre du
 * segment, une barre courte sous l'actif, aucun relief.</p>
 */
public final class TabRow extends View {

    /** L'auditeur du choix d'onglet. */
    public interface TabListener {

        /**
         * L'onglet choisi au doigt ou par programme.
         *
         * @param index l'indice de l'onglet
         */
        void onTabSelected(int index);
    }

    /** Hauteur de la barre en dp (l'apparence d'une TabLayout). */
    private static final float HEIGHT_DP = 48.0f;

    /** Hauteur de l'indicateur de l'onglet actif en dp.. */
    private static final float INDICATOR_HEIGHT_DP = 3.0f;

    /** Les étiquettes des onglets. */
    private final List<String> labels = new ArrayList<>();

    /** L'auditeur du choix (null : aucun). */
    private TabListener listener;

    /** L'onglet choisi. */
    private int selected = -1;

    /** La peinture de l'encre des onglets inactifs (le texte). */
    private final Paint textPaint = new Paint();

    /** La peinture de l'indicateur de l'onglet actif. */
    private final Paint indicatorPaint = new Paint();

    /**
     * Une barre d'onglets neuve, vide.
     *
     * @param context le contexte porteur
     */
    public TabRow(Context context) {
        super(context);
        float density = getResources().getDisplayMetrics().density;
        textPaint.setTextSize(14.0f * density);
        indicatorPaint.setColor(0xFF3D5AFE);
    }

    /**
     * Pose les étiquettes des onglets (la sélection se remet à zéro,
     * le premier onglet devient l'actif — la méthode notifie).
     *
     * @param names les étiquettes, jamais null
     */
    public void setTabs(List<String> names) {
        labels.clear();
        labels.addAll(names);
        selected = labels.isEmpty() ? -1 : 0;
        invalidate();
    }

    /**
     * Pose l'auditeur du choix d'onglet.
     *
     * @param tabListener l'auditeur (null : aucun)
     */
    public void setTabListener(TabListener tabListener) {
        listener = tabListener;
    }

    /**
     * Choisit l'onglet par programme : l'auditeur y est prévenu
     * (le choix de l'écran se fait par la même voie que le doigt).
     *
     * @param index l'indice de l'onglet
     */
    public void selectTab(int index) {
        if (index < 0 || index >= labels.size() || index == selected) {
            return;
        }
        selected = index;
        invalidate();
        if (listener != null) {
            listener.onTabSelected(index);
        }
    }

    /** L'indice de l'onglet actif (aucun : -1). */
    public int selectedTab() {
        return selected;
    }

    /** Le nombre d'onglets. */
    public int tabCount() {
        return labels.size();
    }

    /**
     * L'étiquette d'un onglet.
     *
     * @param index l'indice de l'onglet
     * @return l'étiquette, ou la chaîne vide
     */
    public String tabLabel(int index) {
        if (index < 0 || index >= labels.size()) {
            return "";
        }
        return labels.get(index);
    }

    /**
     * Le toucher choisit l'onglet sous le doigt : le segment tenu
     * par la largeur du parent divisée par le nombre d'onglets.
     */
    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (event == null || labels.isEmpty()) {
            return false;
        }
        int index = (int) (event.getX() / Math.max(1.0f, getWidth())
                * labels.size());
        if (index >= 0 && index < labels.size()) {
            selectTab(index);
            performClick();
            return true;
        }
        return false;
    }

    /**
     * Le dessin : chaque segment porte son étiquette, l'actif porte
     * l'indicateur souligné et l'encre pleine, l'inactif l'encre
     * secondaire.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (labels.isEmpty()) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        float segment = getWidth() / (float) labels.size();
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float textCenter = HEIGHT_DP * density / 2.0f;
        for (int index = 0; index < labels.size(); index++) {
            textPaint.setColor(index == selected ? 0xFF1D1B20 : 0xFF49454F);
            float x = segment * index + segment / 2.0f;
            float y = textCenter + (metrics.ascent + metrics.descent) / -2.0f;
            canvas.drawText(labels.get(index), x, y, textPaint);
            if (index == selected) {
                canvas.drawRect(
                        segment * index + segment / 4.0f,
                        getHeight() - INDICATOR_HEIGHT_DP * density,
                        segment * index + segment * 3.0f / 4.0f,
                        getHeight(), indicatorPaint);
            }
        }
    }

    /**
     * La hauteur demandée : la barre la veut fixe (la disposition
     * linéaire verticale la reçoit comme paramètre).
     *
     * @return la hauteur en pixels
     */
    public int preferredHeightPx() {
        return (int) (HEIGHT_DP * getResources().getDisplayMetrics().density);
    }
}
