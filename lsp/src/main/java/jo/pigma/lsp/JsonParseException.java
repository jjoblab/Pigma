package jo.pigma.lsp;

/**
 * Le texte n'est pas un JSON valable : la position dit où.
 */
public final class JsonParseException extends RuntimeException {

    /**
     * Un refus d'analyse.
     *
     * @param message la cause et la position
     */
    public JsonParseException(String message) {
        super(message, null, false, false);
    }
}
