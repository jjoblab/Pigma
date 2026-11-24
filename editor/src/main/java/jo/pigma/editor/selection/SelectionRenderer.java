package jo.pigma.editor.selection;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Le rendu de la sélection (le paquet selection, l'arborescence du
 * prompt) : la teinte d'une sélection posée, rectangulaire ligne par
 * ligne. Le pinceau y vit ; la vue fournit le texte (les bornes des
 * lignes, la sélection), la visibilité (les plis) et la géométrie à
 * la trame — par le contrat {@link Lines}.
 */
public final class SelectionRenderer {

    /**
     * Le contrat interne du paquet selection : ce dont le rendu a
     * besoin du texte. HORS API publique — il ne se documente pas, il
     * peut changer.
     */
    public interface Lines {

        /** Le décalage du début de la ligne. */
        int lineStart(int line);

        /** Le décalage de la fin de la ligne. */
        int lineEnd(int line);

        /** La ligne est-elle cachée par un pli. */
        boolean isHidden(int line);

        /** L'indice visible de la ligne (les plis compactent). */
        int visibleIndexOf(int line);
    }

    /**
     * La géométrie de la vue au dessin (le paquet ne connaît la vue
     * que par ces nombres).
     *
     * @param paddingLeft la marge gauche (px)
     * @param paddingTop  la marge haute (px)
     * @param lineHeight  la hauteur d'une ligne (px)
     * @param charWidth   l'avance d'un caractère (px, chasse fixe)
     * @param scrollY     le décalage vertical du défilement (px)
     */
    public record Geometry(float paddingLeft, float paddingTop, float lineHeight,
            float charWidth, float scrollY) {
    }

    private final Paint paint = new Paint();

    /**
     * Un rendu neuf, son pinceau propre (la teinte translucide du
     * bleu de sélection).
     */
    public SelectionRenderer() {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x332979FF);
    }

    /**
     * Le pinceau du rendu — la loupe et la pastille de sélection le
     * partagent (les drapeaux s'y posent avant l'usage, comme avant
     * l'extraction).
     *
     * @return le pinceau de la teinte de sélection
     */
    public Paint paint() {
        return paint;
    }

    /**
     * Dessine la sélection : un rectangle par ligne traversée — les
     * lignes cachées par un pli ne se teintent pas, la sélection
     * n'élargit pas la fenêtre visible.
     *
     * @param canvas   le canevas de la trame
     * @param lines    le contrat du texte (bornes, visibilité)
     * @param from     le bord gauche de la sélection (décalage)
     * @param to       le bord droit de la sélection (décalage)
     * @param first    la première ligne visible
     * @param last     la dernière ligne visible
     * @param count    le nombre de lignes du document
     * @param geometry la géométrie de la vue
     */
    public void draw(Canvas canvas, Lines lines, int from, int to,
            int first, int last, int count, Geometry geometry) {
        for (int line = first; line <= last && line < count; line++) {
            if (lines.isHidden(line)) {
                continue;
            }
            int lineStart = lines.lineStart(line);
            int lineEnd = lines.lineEnd(line);
            int start = Math.max(from, lineStart);
            int end = Math.min(to, lineEnd);
            if (end <= start) {
                continue;
            }
            float left = geometry.paddingLeft() + (start - lineStart) * geometry.charWidth();
            float right = geometry.paddingLeft() + (end - lineStart) * geometry.charWidth();
            float top = geometry.paddingTop() + lines.visibleIndexOf(line) * geometry.lineHeight()
                    - geometry.scrollY();
            canvas.drawRect(left, top, right, top + geometry.lineHeight(), paint);
        }
    }
}
