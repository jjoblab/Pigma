package jo.pigma.lsp;

/**
 * Le message du protocole : une enveloppe JSON-RPC — une requête porte
 * un identifiant et une méthode, la réponse rapporte le résultat à cet
 * identifiant, la notification ne rapporte rien. Le client ne connaît
 * que ces trois formes.
 */
public final class LspMessage {

    /** Le code d'erreur JSON-RPC renvoyé par un serveur qui refuse. */
    public record Error(int code, String message) {
    }

    private final Integer id;

    private final String method;

    private final JsonObject params;

    private final JsonValue result;

    private final Error error;

    private LspMessage(Integer id, String method, JsonObject params,
            JsonValue result, Error error) {
        this.id = id;
        this.method = method;
        this.params = params;
        this.result = result;
        this.error = error;
    }

    /** Une requête : l'identifiant corrélara la réponse. */
    public static LspMessage request(int id, String method, JsonObject params) {
        return new LspMessage(id, method, params, null, null);
    }

    /** Une notification : aucun retour ne viendra. */
    public static LspMessage notification(String method, JsonObject params) {
        return new LspMessage(null, method, params, null, null);
    }

    /** Une réponse : le résultat (ou l'erreur) d'une requête passée. */
    public static LspMessage response(int id, JsonValue result, Error error) {
        return new LspMessage(id, null, null, result, error);
    }

    /** L'identifiant (null pour une notification). */
    public Integer id() {
        return id;
    }

    /** La méthode (null pour une réponse). */
    public String method() {
        return method;
    }

    /** Les paramètres bruts (null si absents). */
    public JsonObject params() {
        return params;
    }

    /** Le résultat brut (null si absent ou erreur). */
    public JsonValue result() {
        return result;
    }

    /** L'erreur de la réponse (null si succès). */
    public Error error() {
        return error;
    }

    /** Le message en texte JSON prêt à cadrer. */
    public String toJson() {
        JsonObject object = new JsonObject();
        object.put("jsonrpc", JsonString.of("2.0"));
        if (id != null) {
            object.put("id", JsonNumber.of(id));
        }
        if (method != null) {
            object.put("method", JsonString.of(method));
            if (params != null) {
                object.put("params", params);
            }
        } else {
            object.put("id", JsonNumber.of(id));
            if (error != null) {
                object.put("error", JsonObject.of(
                        "code", JsonNumber.of(error.code()),
                        "message", JsonString.of(error.message())));
            } else {
                object.put("result", result == null ? JsonNull.NULL : result);
            }
        }
        return object.toJson();
    }

    /**
     * Lit une enveloppe : les champs absents restent null, une forme
     * inconnue lève — le protocole ne se devine pas.
     *
     * @param object le message JSON
     * @return l'enveloppe typée
     * @throws IllegalArgumentException si la forme n'est ni requête, ni
     *         notification, ni réponse
     */
    public static LspMessage fromJson(JsonObject object) {
        String jsonrpc = object.getString("jsonrpc");
        if (!"2.0".equals(jsonrpc)) {
            throw new IllegalArgumentException("pas une enveloppe JSON-RPC 2.0");
        }
        Double id = object.getNumber("id");
        String method = object.getString("method");
        JsonObject params = object.getObject("params");
        if (method != null) {
            return new LspMessage(id == null ? null : (int) (double) id,
                    method, params, null, null);
        }
        if (id == null) {
            throw new IllegalArgumentException("réponse sans identifiant");
        }
        JsonObject errorObject = object.getObject("error");
        Error error = null;
        if (errorObject != null) {
            Double code = errorObject.getNumber("code");
            error = new Error(code == null ? 0 : (int) (double) code,
                    errorObject.getString("message"));
        }
        JsonValue result = object.get("result");
        return new LspMessage((int) (double) id, null, null, result, error);
    }
}
