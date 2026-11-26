package jo.pigma.editor.render;

/**
 * Plan de rendu d'une ligne : textes tranchés, couleurs résolues
 * (fusion L3 comprise), colonnes de début, abscisses, gras.
 *
 * <p>Le plan vit au paquet du rendu (le {@code Painter} du paquet
 * interne y lit ses segments — la vue le cuit par son
 * {@link RenderCache}).</p>
 */
public record LinePlan(String[] texts, int[] colors, int[] cols,
        float[] xs, boolean[] bolds) {
}
