package jo.pigma.ai;

import java.util.regex.Pattern;

/**
 * LE FILTRE PAR DÉFAUT (B1) — un FILET, JAMAIS UNE POLITIQUE : il
 * masque les formes les plus communes de secret dans les parties de
 * CONTEXTE de la demande (le préfixe, le suffixe, la sélection, le
 * bloc englobant, les extraits d'autres documents) — les clefs
 * privées PEM, les affectations de mot de passe/secret/jeton, les
 * porteurs Bearer. Il NE REMPLACE PAS la politique de l'hôte : la
 * liste est courte par principe (une liste de motifs « suffisante »
 * serait un mensonge), l'hôte qui manie de vrais secrets pose SON
 * filtre — celui-ci ne vit que de ne rien envoyer de GROSSIÈREMENT
 * sensible par accident.
 */
public final class DefaultContextFilter implements ContextBuilder.ContextFilter {

    /** Le masque qui remplace le secret. */
    private static final String MASK = "…masqué…";
    /** La clef privée PEM : le bloc entier (du BEGIN au END) ou
     * l'en-tête seul (le bloc coupé par la troncature — tout ce qui
     * suit le BEGIN se masque jusqu'au bout du contexte). */
    private static final Pattern PEM_KEY = Pattern.compile(
            "-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?"
                    + "(?:-----END [A-Z ]*PRIVATE KEY-----|$)");
    /** L'affectation d'un secret (mot de passe, clef, jeton…). */
    private static final Pattern ASSIGNMENT = Pattern.compile(
            "(?i)(password|passwd|secret|token|api[_-]?key)\\s*[=:]\\s*\\S+");

    /** Le porteur Bearer (l'en-tête d'autorisation). */
    private static final Pattern BEARER = Pattern.compile(
            "(?i)bearer\\s+[A-Za-z0-9._~+/=-]{16,}");

    /** Le filtre par défaut, sans état (le même pour tous). */
    public DefaultContextFilter() {
    }

    /**
     * La demande se filtre : les parties de contexte se masquent
     * (les formes communes de secret), le reste demeure. La demande
     * ne se REFUSE jamais ici — un filet retient, il ne juge pas.
     *
     * @param request la demande construite
     * @return la demande au contexte masqué
     */
    @Override
    public AiRequest filter(AiRequest request) {
        return new AiRequest(request.requestId(), request.task(),
                request.languageId(), request.documentName(),
                mask(request.prefix()), mask(request.suffix()),
                mask(request.selection()), mask(request.instruction()),
                request.diagnostics(), mask(request.enclosingBlock()),
                maskExcerpts(request.otherDocuments()),
                request.maxContextChars());
    }

    /** Masque les formes communes d'un texte de contexte. */
    private static String mask(String context) {
        if (context == null || context.isEmpty()) {
            return "";
        }
        String masked = PEM_KEY.matcher(context).replaceAll(MASK);
        masked = ASSIGNMENT.matcher(masked).replaceAll("$1=" + MASK);
        masked = BEARER.matcher(masked).replaceAll("Bearer " + MASK);
        return masked;
    }

    /** Masque les extraits d'autres documents. */
    private static java.util.List<AiDocumentExcerpt> maskExcerpts(
            java.util.List<AiDocumentExcerpt> excerpts) {
        java.util.List<AiDocumentExcerpt> masked =
                new java.util.ArrayList<>(excerpts.size());
        for (AiDocumentExcerpt excerpt : excerpts) {
            masked.add(new AiDocumentExcerpt(excerpt.name(),
                    mask(excerpt.content())));
        }
        return java.util.List.copyOf(masked);
    }
}
