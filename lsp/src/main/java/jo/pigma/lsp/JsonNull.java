package jo.pigma.lsp;

/**
 * Le null JSON : une seule instance, l'énumération l'exprime.
 */
public enum JsonNull implements JsonValue {
    /** Le null. */
    NULL;

    @Override
    public void write(StringBuilder out) {
        out.append("null");
    }
}
