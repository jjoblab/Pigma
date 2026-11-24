package jo.pigma.syntax;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Thème de coloration : une table de portées vers des couleurs ARGB,
 * au repli par parent — une portée sans entrée prend la couleur de son
 * parent, et une branche entière sans entré prend la couleur par
 * défaut portée par le thème. Une clé absente signifie « pas d'avis »,
 * jamais « pas de couleur » : le repli décide. Changer de thème ne
 * retokenise rien : les jetons portent des portées, jamais des
 * couleurs.
 */
public final class SyntaxTheme {

    private final Map<Scope, Integer> colors;
    private final int defaultColor;

    private SyntaxTheme(Map<Scope, Integer> colors, int defaultColor) {
        this.colors = colors;
        this.defaultColor = defaultColor;
    }

    /**
     * Un thème construit d'une table de noms de portées vers des
     * couleurs ARGB. Les noms inconnus d'une portée demandée plus tard
     * ne posent aucun problème : le repli par parent s'applique au
     * moment de la demande.
     *
     * @param entries      table nom de portée vers couleur ARGB, jamais
     *                     {@code null} (une table vide ne colore rien)
     * @param defaultColor couleur ARGB des jetons sans portée ou sans
     *                     entrée atteignable
     * @return le thème, prêt à rendre des couleurs
     */
    public static SyntaxTheme of(Map<String, Integer> entries, int defaultColor) {
        Objects.requireNonNull(entries);
        Map<Scope, Integer> colors = new HashMap<>();
        for (Map.Entry<String, Integer> entry : entries.entrySet()) {
            if (entry.getValue() == null) {
                throw new IllegalArgumentException(
                        "null color for scope: " + entry.getKey());
            }
            colors.put(Scope.of(entry.getKey()), entry.getValue());
        }
        return new SyntaxTheme(colors, defaultColor);
    }

    /**
     * Couleur ARGB de la portée : la sienne, sinon celle du parent le
     * plus proche qui en a une, sinon la couleur par défaut.
     *
     * @param scope portée du jeton, éventuellement {@code null} (jeton
     *              neutre : la couleur par défaut)
     * @return la couleur ARGB résolue
     */
    public int colorOf(Scope scope) {
        for (Scope current = scope; current != null; current = current.parent()) {
            Integer color = colors.get(current);
            if (color != null) {
                return color;
            }
        }
        return defaultColor;
    }

    /**
     * La couleur par défaut du thème.
     *
     * @return la couleur ARGB des jetons sans avis
     */
    public int defaultColor() {
        return defaultColor;
    }
}
