package jo.pigma.ai;

/**
 * La poignée d'une demande en vol (B0) : l'hôte qui part l'annule.
 * L'annulation n'est pas une attente — la réponse annulée, si elle
 * vient, se dit par l'erreur {@link AiError.Category#CANCELLED} à
 * l'écouteur (ou ne se dit pas du tout : l'implémentation décide).
 */
public interface AiCall {

    /** Annule la demande en vol (idempotent : une demande finie
     * n'y rouvre rien). */
    void cancel();
}
