package jo.pigma.lsp;

/**
 * Un booléen JSON.
 *
 * @param value la valeur
 */
public record JsonBoolean(boolean value) implements JsonValue {

    /** Le vrai. */
    public static final JsonBoolean TRUE = new JsonBoolean(true);

    /** Le faux. */
    public static final JsonBoolean FALSE = new JsonBoolean(false);

    @Override
    public void write(StringBuilder out) {
        out.append(value ? "true" : "false");
    }
}
