package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.Arrays;
import java.util.List;

/**
 * Le bandeau de complétion (le paquet des popups) : les candidats du
 * mot courant côte à côte, en bas de la vue. L'état (les mots, les
 * abscisses gardées pour le toucher) et le dessin vivent ici ; la vue
 * fournit la géométrie à la trame et le toucher lit les abscisses.
 */
public final class SuggestionBand {

    /** Les candidats montrés (vide : le bandeau se range). */
    private List<String> words = List.of();

    /** L'abscisse de chaque candidat dessiné (le toucher y vise). */
    private float[] xs = new float[0];

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint background = new Paint();

    /**
     * Un bandeau neuf, ses pinceaux propres.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public SuggestionBand(float density) {
        paint.setColor(0xFF1B1B1F);
        paint.setTextSize(13.0f * density);
        background.setStyle(Paint.Style.FILL);
        background.setColor(0x11000000);
    }

    /**
     * Les candidats du bandeau (la copie défensive protège les
     * abscisses : elles ne se calculent qu'au dessin).
     *
     * @param candidates les mots à montrer, jamais null
     */
    public void set(List<String> candidates) {
        words = List.copyOf(candidates);
        xs = new float[0];
    }

    /** Les candidats montrés. */
    public List<String> words() {
        return words;
    }

    /** L'abscisse du candidat d'indice donné (le dessin la garde). */
    public float xAt(int index) {
        return xs[index];
    }

    /** Le nombre de candidats dessinés (le toucher y borne). */
    public int drawnCount() {
        return xs.length;
    }

    /**
     * Dessine le bandeau : les candidats côte à côte en bas de la vue,
     * les abscisses se gardent pour le toucher — les candidats qui ne
     * tiennent pas se taisent.
     *
     * @param canvas  le canevas de la trame
     * @param metrics la géométrie de la vue
     */
    public void draw(Canvas canvas, PopupMetrics metrics) {
        if (words.isEmpty()) {
            xs = new float[0];
            return;
        }
        float density = metrics.density();
        float height = 28.0f * density;
        float top = metrics.viewHeight() - height - metrics.paddingBottom();
        if (top < 0.0f) {
            xs = new float[0];
            return;
        }
        canvas.drawRect(0.0f, top, metrics.viewWidth(), top + height, background);
        float x = metrics.paddingLeft();
        float limit = metrics.viewWidth() - metrics.paddingRight();
        float[] laid = new float[words.size()];
        int drawn = 0;
        for (String candidate : words) {
            float width = paint.measureText(candidate) + 16.0f * density;
            if (x + width > limit) {
                break;
            }
            laid[drawn] = x;
            canvas.drawText(candidate, x + 8.0f * density,
                    top + height / 2.0f + 4.0f * density, paint);
            x += width;
            drawn++;
        }
        xs = drawn == laid.length ? laid : Arrays.copyOf(laid, drawn);
    }
}
