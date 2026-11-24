package jo.pigma.lsp;

import java.util.Map;

/**
 * Un objet JSON : les clés en ordre d'insertion, jamais null en clé.
 * Les accès typés ({@code getString}, {@code getNumber}…) ne devinent
 * jamais : un membre du mauvais type rend null.
 */
public final class JsonObject implements JsonValue {

    private final Map<String, JsonValue> members = new java.util.LinkedHashMap<>();

    /**
     * Un objet de paires clé-valeur alternées : {@code of("a", v1, "b", v2)}.
     *
     * @param pairs les clés (String) et les valeurs (JsonValue) alternées
     * @return l'objet construit
     */
    public static JsonObject of(Object... pairs) {
        JsonObject object = new JsonObject();
        for (int at = 0; at + 1 < pairs.length; at += 2) {
            object.put((String) pairs[at], (JsonValue) pairs[at + 1]);
        }
        return object;
    }

    /**
     * Pose (ou remplace) un membre.
     *
     * @param key   la clé, jamais null
     * @param value la valeur, null pour le null JSON
     */
    public void put(String key, JsonValue value) {
        members.put(key, value == null ? JsonNull.NULL : value);
    }

    /**
     * Le membre demandé.
     *
     * @param key la clé cherchée
     * @return la valeur, ou {@code null} si la clé manque
     */
    public JsonValue get(String key) {
        return members.get(key);
    }

    /** Le membre chaîne, null si la clé manque ou n'est pas une chaîne. */
    public String getString(String key) {
        JsonValue value = members.get(key);
        return value instanceof JsonString text ? text.value() : null;
    }

    /** Le membre nombre, null si la clé manque ou n'est pas un nombre. */
    public Double getNumber(String key) {
        JsonValue value = members.get(key);
        return value instanceof JsonNumber number ? number.value() : null;
    }

    /** Le membre objet, null si la clé manque ou n'est pas un objet. */
    public JsonObject getObject(String key) {
        JsonValue value = members.get(key);
        return value instanceof JsonObject object ? object : null;
    }

    /** Le membre tableau, null si la clé manque ou n'est pas un tableau. */
    public JsonArray getArray(String key) {
        JsonValue value = members.get(key);
        return value instanceof JsonArray array ? array : null;
    }

    /** Le membre booléen, null si la clé manque ou n'est pas un booléen. */
    public Boolean getBoolean(String key) {
        JsonValue value = members.get(key);
        return value instanceof JsonBoolean bool ? bool.value() : null;
    }

    @Override
    public void write(StringBuilder out) {
        out.append('{');
        boolean first = true;
        for (Map.Entry<String, JsonValue> member : members.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            JsonString.escape(member.getKey(), out);
            out.append(':');
            member.getValue().write(out);
        }
        out.append('}');
    }
}
