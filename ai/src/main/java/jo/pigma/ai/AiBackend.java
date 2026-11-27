package jo.pigma.ai;

/**
 * CE QUE L'HÔTE IMPLÉMENTE (B0) : la seule intelligence artificielle
 * que Pigma connaît. Pigma ne sait RIEN des fournisseurs, des
 * modèles, des clés, des protocoles ni du réseau — il PRÉPARE la
 * demande (le contexte, les filtres de l'hôte) et APPLIQUE la
 * réponse en sécurité ; l'implémentation décide où partent les
 * données et qui les paie.
 *
 * <p>Le contrat : {@link #capabilities()} dit les tâches que cette
 * implémentation accepte (SEULES celles-ci apparaissent dans
 * l'interface — sans implémentation, AUCUNE action d'IA ne vit) ;
 * {@link #start(AiRequest, AiListener)} démarre une demande et rend
 * IMMÉDIATEMENT une poignée annulable — les rappels de l'écouteur
 * peuvent venir de N'IMPORTE QUEL FIL (l'hôte marshalle s'il veut
 * la vue). Une demande ne rend qu'UNE issue : {@code onComplete}
 * OU {@code onError} (l'annulation se dit par l'erreur
 * {@link AiError.Category#CANCELLED CANCELLED}).</p>
 */
public interface AiBackend {

    /**
     * Les tâches que cette implémentation accepte — seules
     * celles-ci apparaissent dans l'interface de Pigma.
     *
     * @return les capacités, jamais {@code null} (vide : aucune
     *         action d'IA ne vivra)
     */
    AiCapabilities capabilities();

    /**
     * Démarre une demande : la réponse revient par l'écouteur, la
     * poignée rendue ANNULE à volonté. La méthode rend vite (le vol
     * appartient à l'implémentation — jamais au fil appelant).
     *
     * @param request  la demande préparée, jamais {@code null}
     * @param listener l'écouteur des rappels, jamais {@code null}
     * @return la poignée annulable, jamais {@code null}
     */
    AiCall start(AiRequest request, AiListener listener);
}
