package jo.pigma.ai;

import java.util.Set;

/**
 * Ce que l'implémentation de l'hôte accepte (B0) : les tâches
 * déclarées seules apparaissent dans l'interface de Pigma, le flux
 * ({@code onDelta}) ne vit que si l'implémentation le gère. Sans
 * backend, RIEN n'apparaît.
 *
 * @param tasks     les tâches acceptées, jamais {@code null}
 *                  (vide : rien n'apparaît)
 * @param streaming vrai quand l'implémentation envoie des morceaux
 *                  par {@code onDelta} (l'interface l'annonce au
 *                  chargement)
 */
public record AiCapabilities(Set<AiTask> tasks, boolean streaming) {

    /** Les capacités vides : aucune tâche, aucun flux (le défaut
     * du « désactivé par défaut »). */
    public static final AiCapabilities NONE = new AiCapabilities(
            Set.of(), false);

    /** La forme canonique : les nuls se vident, la copie se fige. */
    public AiCapabilities {
        tasks = tasks == null ? Set.of() : Set.copyOf(tasks);
    }

    /**
     * La tâche est-elle acceptée ? (Seule une tâche acceptée se
     * propose dans l'interface.)
     *
     * @param task la tâche questionnée
     * @return vrai quand elle est déclarée
     */
    public boolean accepts(AiTask task) {
        return tasks.contains(task);
    }
}
