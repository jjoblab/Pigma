package jo.pigma.ai;

/**
 * L'erreur d'une demande (B0) : une CATÉGORIE par famille de panne
 * (l'interface dit UNE phrase par catégorie — l'hôte n'y traduit
 * rien), le message porte le détail de l'implémentation.
 *
 * @param category la famille de la panne, jamais {@code null}
 * @param message  le détail de l'implémentation, jamais
 *                 {@code null} (vide permis — la catégorie parle
 *                 seule)
 */
public record AiError(Category category, String message) {

    /** La forme canonique : la catégorie se exige, le nul se vide. */
    public AiError {
        if (category == null) {
            throw new NullPointerException("category must not be null");
        }
        message = message == null ? "" : message;
    }

    /** Les familles de panne (une phrase d'interface par famille). */
    public enum Category {

        /** L'implémentation ne sait pas (ou ne veut plus) servir :
         * aucun fournisseur derrière, l'interrupteur de l'hôte
         * éteint. */
        UNAVAILABLE,

        /** La demande fut annulée par sa poignée. */
        CANCELLED,

        /** Le temps de vol a dépassé ce que l'hôte accorde. */
        TIMEOUT,

        /** La demande fut refusée (la politique de l'hôte, la sienne
         * — le message dit pourquoi). */
        REFUSED,

        /** La panne interne de l'implémentation (le message dit
         * laquelle). */
        INTERNAL
    }
}
