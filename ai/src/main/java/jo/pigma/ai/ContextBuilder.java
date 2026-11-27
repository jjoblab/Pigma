package jo.pigma.ai;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LE PRÉPARATEUR DU CONTEXTE (B1) : construit la demande de ce que
 * Pigma sait DÉJÀ — le préfixe et le suffixe autour de la position
 * (tronqués au budget en caractères), la sélection, le bloc
 * englobant, les diagnostics, le langage — et les extraits
 * d'AUTRES documents par le callback de l'hôte (Pigma ne connaît
 * pas son système de fichiers, il ne lit rien de lui-même).
 *
 * <p>SANS ARBRE SYNTAXIQUE : l'analyse syntaxique avancée est hors
 * périmètre — le bloc englobant se reçoit de la profondeur
 * d'accolades déjà calculée (vide s'il n'est pas déterminable), les
 * diagnostics se reçoivent de l'analyse locale.</p>
 *
 * <p>LES CROCHETS DE L'HÔTE s'y appliquent TOUJOURS, dans l'ordre :
 * la POLITIQUE d'abord (un document refusé ne construit RIEN — la
 * politique exclut les fichiers que l'hôte ne veut pas voir partir :
 * les secrets, l'environnement) ; le FILTRE ensuite (la demande
 * construite, il la MODIFIE ou la REFUSE). Un refus se dit par un
 * retour NUL : rien ne part.</p>
 */
public final class ContextBuilder {

    /** Le compteur des demandes (la clef de corrélation « pigma-N »). */
    private static final AtomicLong REQUESTS = new AtomicLong();

    /**
     * La politique de l'hôte : les documents autorisés à partir. Un
     * document refusé ne construit RIEN (le contexte ne se lit même
     * pas — les extraits ne se demandent pas).
     */
    public interface AiPolicy {

        /**
         * Le document est-il autorisé à partir vers l'IA ?
         *
         * @param documentName le nom du document
         * @param languageId   le langage du document
         * @return vrai quand il peut partir
         */
        boolean isAllowed(String documentName, String languageId);
    }

    /**
     * Le filtre de l'hôte : la demande construite passe par sa main
     * AVANT l'envoi — il la modifie (le contexte se resserre, les
     * secrets se masquent) ou la REFUSE (retour nul : rien ne part).
     */
    public interface ContextFilter {

        /**
         * La demande se filtre.
         *
         * @param request la demande construite, jamais {@code null}
         * @return la demande filtrée (la même permise), ou
         *         {@code null} pour refuser
         */
        AiRequest filter(AiRequest request);
    }

    /**
     * Le callback des autres documents (B1) : l'HÔTE seul fournit
     * les extraits qui voyagent — Pigma ne connaît pas son système
     * de fichiers.
     */
    public interface OtherDocumentsSupplier {

        /**
         * Les extraits d'autres documents pour cette demande (vide :
         * aucun ne voyage).
         *
         * @param documentName le nom du document courant
         * @param languageId   le langage du document courant
         * @return les extraits, jamais {@code null}
         */
        List<AiDocumentExcerpt> otherDocuments(String documentName,
                String languageId);
    }

    /**
     * Le document source (B1) : ce que Pigma sait déjà — le texte,
     * la position visée, la sélection vivante, les diagnostics de la
     * position, le bloc englobant déduit de la profondeur
     * d'accolades (vide s'il n'est pas déterminable).
     *
     * @param documentName   le nom du document (son étiquette)
     * @param languageId     le langage du document
     * @param text           le texte du document
     * @param caret          la position visée (le curseur)
     * @param selectionStart le début de la sélection (-1 : aucune)
     * @param selectionEnd   la fin de la sélection (-1 : aucune)
     * @param diagnostics    les diagnostics de la position
     * @param enclosingBlock le bloc englobant la position (vide si
     *                       non déterminable)
     */
    public record Source(String documentName, String languageId,
            CharSequence text, int caret, int selectionStart,
            int selectionEnd, List<AiDiagnostic> diagnostics,
            String enclosingBlock) {

        /** La forme canonique : les nuls se vident, les listes se
         * figent. */
        public Source {
            documentName = documentName == null ? "" : documentName;
            languageId = languageId == null ? "" : languageId;
            text = text == null ? "" : text;
            diagnostics = diagnostics == null
                    ? List.of() : List.copyOf(diagnostics);
            enclosingBlock = enclosingBlock == null ? "" : enclosingBlock;
        }
    }

    /** La politique de l'hôte (null : tout est permis). */
    private final AiPolicy policy;

    /** Le filtre de l'hôte (null : aucune modification). */
    private final ContextFilter filter;

    /**
     * Un préparateur aux crochets de l'hôte (null : le crochet ne
     * vit pas — le filtre par défaut est un FILET, jamais une
     * politique).
     *
     * @param policy la politique des documents autorisés, null pour
     *               tout permettre
     * @param filter le filtre des demandes, null pour ne rien
     *               filtrer
     */
    public ContextBuilder(AiPolicy policy, ContextFilter filter) {
        this.policy = policy;
        this.filter = filter;
    }

    /**
     * PRÉPARE la demande (B1) : la politique s'éprouve d'abord (un
     * document refusé rend NUL — rien ne se construit, les extraits
     * ne se demandent pas) ; le contexte se construit (le préfixe et
     * le suffixe tronqués au budget autour de la position, la
     * sélection, le bloc englobant, les diagnostics, le langage, les
     * extraits du callback) ; le filtre s'applique enfin (il
     * modifie ou refuse — un refus rend NUL : rien ne part).
     *
     * @param task            la tâche demandée
     * @param instruction     l'instruction de l'utilisateur (vide
     *                        si la tâche n'en porte pas)
     * @param source          le document source
     * @param maxContextChars le budget de contexte en caractères
     *                        (zéro : le contexte ne voyage pas)
     * @param others          le callback des autres documents
     *                        (null : aucun extrait)
     * @return la demande prête, ou {@code null} quand un crochet
     *         refuse
     */
    public AiRequest prepare(AiTask task, String instruction,
            Source source, int maxContextChars,
            OtherDocumentsSupplier others) {
        if (policy != null && !policy.isAllowed(source.documentName(),
                source.languageId())) {
            return null;
        }
        CharSequence text = source.text();
        int caret = Math.max(0, Math.min(source.caret(), text.length()));
        int from = source.selectionStart() >= 0
                ? Math.max(0, Math.min(source.selectionStart(), text.length()))
                : caret;
        int to = source.selectionEnd() >= source.selectionStart()
                ? Math.min(source.selectionEnd(), text.length())
                : from;
        if (to < from) {
            to = from;
        }
        String selection = from < to
                ? text.subSequence(from, to).toString() : "";
        List<AiDocumentExcerpt> excerpts = List.of();
        if (others != null) {
            List<AiDocumentExcerpt> supplied = others.otherDocuments(
                    source.documentName(), source.languageId());
            if (supplied != null) {
                excerpts = List.copyOf(supplied);
            }
        }
        // LES BORNES DU CONTEXTE : la moitié du budget de chaque côté
        // d'abord, le solde inemployé revient à l'autre (le curseur
        // au bord donne tout à l'autre côté)
        int prefixSpan = Math.min(from, maxContextChars / 2);
        int suffixSpan = Math.min(text.length() - to,
                Math.max(maxContextChars - maxContextChars / 2,
                        maxContextChars - prefixSpan));
        prefixSpan = Math.min(from,
                Math.max(prefixSpan, maxContextChars - suffixSpan));
        String prefix = text.subSequence(from - prefixSpan, from).toString();
        String suffix = text.subSequence(to, to + suffixSpan).toString();
        AiRequest request = new AiRequest(
                "pigma-" + REQUESTS.incrementAndGet(), task,
                source.languageId(), source.documentName(),
                prefix, suffix,
                selection, instruction == null ? "" : instruction,
                source.diagnostics(), source.enclosingBlock(),
                excerpts, maxContextChars);
        if (filter == null) {
            return request;
        }
        return filter.filter(request);
    }
}
