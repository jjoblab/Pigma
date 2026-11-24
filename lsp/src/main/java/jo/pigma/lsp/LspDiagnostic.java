package jo.pigma.lsp;

/**
 * Le diagnostic rapporté par un serveur : un message, un intervalle,
 * une sévérité (1 erreur, 2 avertissement, 3 information, 4 indice —
 * le protocole ne dit pas plus).
 *
 * @param range    l'intervalle concerné
 * @param severity la sévérité, 0 si absente
 * @param message  le texte du diagnostic
 */
public record LspDiagnostic(LspRange range, int severity, String message) {

    static LspDiagnostic fromJson(JsonObject object) {
        JsonObject range = object.getObject("range");
        if (range == null) {
            throw new IllegalArgumentException("diagnostic sans intervalle");
        }
        Double severity = object.getNumber("severity");
        String message = object.getString("message");
        return new LspDiagnostic(LspRange.fromJson(range),
                severity == null ? 0 : (int) (double) severity,
                message == null ? "" : message);
    }

    /**
     * Le diagnostic en JSON (un serveur de démonstration le rapporte
     * tel quel).
     *
     * @return l'objet JSON du diagnostic
     */
    public JsonObject toJson() {
        return JsonObject.of("range", range.toJson(),
                "severity", JsonNumber.of(severity),
                "message", JsonString.of(message));
    }
}
