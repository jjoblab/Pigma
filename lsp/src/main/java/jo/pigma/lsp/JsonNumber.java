package jo.pigma.lsp;

/**
 * Un nombre JSON : le double le porte, le littéral d'origine se garde
 * pour l'écriture exacte (les identifiants du protocole restent des
 * entiers à l'aller-retour).
 *
 * @param value   la valeur numérique
 * @param literal le texte d'origine (ou la forme écrite)
 */
public record JsonNumber(double value, String literal) implements JsonValue {

    /**
     * Un nombre : les entiers s'écrivent entiers, les autres en
     * décimal.
     *
     * @param value la valeur
     * @return la valeur JSON
     */
    public static JsonNumber of(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return new JsonNumber(value, Long.toString((long) value));
        }
        return new JsonNumber(value, Double.toString(value));
    }

    @Override
    public void write(StringBuilder out) {
        out.append(literal);
    }
}
