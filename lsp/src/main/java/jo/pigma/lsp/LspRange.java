package jo.pigma.lsp;

/**
 * Intervalle du protocole : de la position de début (incluse) à la
 * position de fin (exclue).
 *
 * @param start le début
 * @param end   la fin
 */
public record LspRange(LspPosition start, LspPosition end) {

    static LspRange fromJson(JsonObject object) {
        JsonObject start = object.getObject("start");
        JsonObject end = object.getObject("end");
        if (start == null || end == null) {
            throw new IllegalArgumentException("intervalle sans bornes");
        }
        return new LspRange(LspPosition.fromJson(start), LspPosition.fromJson(end));
    }

    /**
     * L'intervalle en JSON.
     *
     * @return l'objet JSON de l'intervalle
     */
    public JsonObject toJson() {
        return JsonObject.of("start", start.toJson(), "end", end.toJson());
    }
}
