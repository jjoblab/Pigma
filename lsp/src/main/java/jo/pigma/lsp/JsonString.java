package jo.pigma.lsp;

/**
 * Une chaîne JSON : la valeur dépliée, l'échappement se fait à
 * l'écriture.
 *
 * @param value le texte, jamais null
 */
public record JsonString(String value) implements JsonValue {

    /**
     * Une chaîne JSON.
     *
     * @param value le texte
     * @return la valeur
     */
    public static JsonString of(String value) {
        return new JsonString(value);
    }

    @Override
    public void write(StringBuilder out) {
        escape(value, out);
    }

    /** Échappe et cadre une chaîne : les guillemets, le solide, les commandes. */
    static void escape(String value, StringBuilder out) {
        out.append('"');
        for (int at = 0; at < value.length(); at++) {
            char c = value.charAt(at);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
