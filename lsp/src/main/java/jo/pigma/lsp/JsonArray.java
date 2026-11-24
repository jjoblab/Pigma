package jo.pigma.lsp;

import java.util.ArrayList;
import java.util.List;

/**
 * Un tableau JSON : les éléments en ordre d'ajout.
 */
public final class JsonArray implements JsonValue {

    private final List<JsonValue> items = new ArrayList<>();

    /**
     * Un tableau des valeurs données.
     *
     * @param values les éléments
     * @return le tableau construit
     */
    public static JsonArray of(JsonValue... values) {
        JsonArray array = new JsonArray();
        for (JsonValue value : values) {
            array.add(value);
        }
        return array;
    }

    /**
     * Ajoute un élément à la fin (null devient le null JSON).
     *
     * @param value la valeur à ajouter
     */
    public void add(JsonValue value) {
        items.add(value == null ? JsonNull.NULL : value);
    }

    /** Le nombre d'éléments. */
    public int size() {
        return items.size();
    }

    /**
     * L'élément à l'index.
     *
     * @param index l'index, zéro-based
     * @return la valeur
     */
    public JsonValue get(int index) {
        return items.get(index);
    }

    /** Les éléments (la vue de la liste interne, en lecture seule d'usage). */
    public List<JsonValue> items() {
        return items;
    }

    @Override
    public void write(StringBuilder out) {
        out.append('[');
        boolean first = true;
        for (JsonValue item : items) {
            if (!first) {
                out.append(',');
            }
            first = false;
            item.write(out);
        }
        out.append(']');
    }
}
