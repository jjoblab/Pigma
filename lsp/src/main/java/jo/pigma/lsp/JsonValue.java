package jo.pigma.lsp;

/**
 * Valeur JSON : le protocole LSP parle JSON, le client n'ajoute aucune
 * dépendance pour l'entendre. Le modèle est scellé — un objet, un
 * tableau, une chaîne, un nombre, un booléen, le null — et se lit
 * comme il s'écrit : {@link #parse(CharSequence)} du texte vers la
 * valeur, {@link #write(StringBuilder)} de la valeur vers le texte.
 */
public sealed interface JsonValue permits JsonObject, JsonArray, JsonString,
        JsonNumber, JsonBoolean, JsonNull {

    /**
     * Sérialise la valeur : les caractères d'échappement du JSON
     * sortent repliés, les nombres restent des nombres.
     *
     * @param out la destination, jamais {@code null}
     */
    void write(StringBuilder out);

    /**
     * La chaîne JSON d'une valeur (le diagnostic l'affiche, le
     * protocole la cadre).
     *
     * @return le texte JSON de la valeur
     */
    default String toJson() {
        StringBuilder out = new StringBuilder();
        write(out);
        return out.toString();
    }

    /**
     * Analyse un texte JSON complet : la grammaire stricte (les
     * espaces blanches autour, aucune virgule traînante), les
     * échappements usuels y compris les paires de substitution UTF-16.
     *
     * @param text le texte à analyser, jamais {@code null}
     * @return la valeur lue
     * @throws JsonParseException si le texte n'est pas un JSON valable
     */
    static JsonValue parse(CharSequence text) {
        Parser parser = new Parser(text);
        JsonValue value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw parser.fail("du texte suit la valeur");
        }
        return value;
    }
}
