package jo.pigma.sample;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Cache de rendu ligne par ligne, les deux mécanismes de la conception :
 * la validité par identité d'entrée (la clé rapide d'une ligne est
 * l'objet d'entrée de jetons courant — un rendu périmé est impossible,
 * la comparaison est une identité d'objet) et le partage par contenu
 * (deux lignes identiques, un seul rendu, borné par eviction du plus
 * ancien). Le fournisseur de contenu n'est touché que sur rechamp : le
 * chemin rapide d'une trame ne lit ni ne recopie rien.
 *
 * <p>Un changement de thème ou de langage invalide tout : les plans
 * portent des couleurs résolues. C'est un re-rendu, jamais une
 * retokenisation — les jetons du moteur ne bougent pas.</p>
 *
 * @param <L> type du plan de rendu d'une ligne
 */
public final class RenderCache<L> {

    /**
     * Fabrique du plan d'une ligne, appelée uniquement sur un vide du
     * partage (le contenu y est déjà résolu).
     *
     * @param <L> type du plan de rendu
     */
    public interface PlanBaker<L> {

        /**
         * Cuit le plan d'une ligne.
         *
         * @param line    indice de la ligne
         * @param stamp   entrée de jetons de la ligne (identité de clé)
         * @param content texte de la ligne
         * @return le plan, jamais {@code null}
         */
        L bake(int line, Object stamp, CharSequence content);
    }

    private final LinkedHashMap<String, L> shared;

    private Object[] stamps = new Object[16];
    private L[] plans = newArray(16);
    private long bakes;
    private long keyHits;
    private long slotHits;

    /**
     * Un cache au partage borné.
     *
     * @param sharedCapacity nombre maximal de plans partagés par contenu
     */
    public RenderCache(int sharedCapacity) {
        shared = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<String, L> eldest) {
                return size() > sharedCapacity;
            }
        };
    }

    /**
     * Invalide tout (changement de thème ou de langage) : les tampons
     * de lignes se vident, le partage aussi — les plans recuisent à la
     * demande, les jetons ne bougent pas.
     */
    public void invalidate() {
        Arrays.fill(stamps, null);
        Arrays.fill(plans, null);
        shared.clear();
    }

    /**
     * Le plan d'une ligne pour cette trame : chemin rapide par identité
     * d'entrée (aucune lecture du texte), sinon reprise par contenu —
     * cuisson seulement si personne n'a encore rendu ce contenu.
     *
     * @param line    indice de la ligne, 0 ou plus
     * @param stamp   entrée de jetons de la ligne (identité de clé)
     * @param content fournisseur paresseux du texte de la ligne,
     *                touché seulement hors chemin rapide
     * @param baker   cuisson au vide du partage
     * @return le plan de rendu, jamais {@code null}
     */
    public L frame(int line, Object stamp, Supplier<CharSequence> content,
            PlanBaker<L> baker) {
        Objects.requireNonNull(stamp);
        Objects.requireNonNull(content);
        Objects.requireNonNull(baker);
        if (line < 0) {
            throw new IndexOutOfBoundsException("ligne négative : " + line);
        }
        growSlots(line);
        if (stamps[line] == stamp) {
            slotHits++;
            return plans[line];
        }
        CharSequence text = Objects.requireNonNull(content.get());
        String key = text.toString();
        L plan = shared.get(key);
        if (plan == null) {
            plan = Objects.requireNonNull(baker.bake(line, stamp, text));
            shared.put(key, plan);
            bakes++;
        } else {
            keyHits++;
        }
        stamps[line] = stamp;
        plans[line] = plan;
        return plan;
    }

    /**
     * Cuissons effectuées depuis la construction (ou la dernière
     * invalidation) — le harnais et le panneau les lisent.
     *
     * @return le nombre de cuissons
     */
    public long bakes() {
        return bakes;
    }

    /**
     * Reprises par contenu (contenu déjà rendu ailleurs) depuis la
     * dernière invalidation.
     *
     * @return le nombre de reprises
     */
    public long keyHits() {
        return keyHits;
    }

    /**
     * Passages par le chemin rapide (aucune lecture de texte) depuis la
     * dernière invalidation.
     *
     * @return le nombre de passages
     */
    public long slotHits() {
        return slotHits;
    }

    @SuppressWarnings("unchecked")
    private static <L> L[] newArray(int length) {
        return (L[]) new Object[length];
    }

    private void growSlots(int line) {
        if (line < stamps.length) {
            return;
        }
        int capacity = Math.max(stamps.length * 2, line + 1);
        stamps = Arrays.copyOf(stamps, capacity);
        plans = Arrays.copyOf(plans, capacity);
    }
}
