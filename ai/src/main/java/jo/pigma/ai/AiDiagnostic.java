package jo.pigma.ai;

/**
 * Un diagnostic qui voyage dans la demande (B0) : le message, la
 * plage et la sévérité — ce que l'analyse locale sait déjà, rien
 * de plus (Pigma ne diagnostique pas pour l'IA, il lui RACONTE ce
 * qu'il a trouvé).
 *
 * @param severity la sévérité, jamais {@code null}
 * @param message  le message du diagnostic, jamais {@code null}
 *                 (vide permis)
 * @param start    le début de la plage (décalage dans le document,
 *                 positif ou nul)
 * @param end      la fin de la plage (décalage, jamais avant le
 *                 début)
 */
public record AiDiagnostic(Severity severity, String message,
        int start, int end) {

    /** La forme canonique : le nul se vide, les bornes se vérifient. */
    public AiDiagnostic {
        if (severity == null) {
            throw new NullPointerException("severity must not be null");
        }
        message = message == null ? "" : message;
        if (start < 0 || end < start) {
            throw new IllegalArgumentException(
                    "range must be 0 <= start <= end, got " + start
                            + ".." + end);
        }
    }

    /** La sévérité d'un diagnostic. */
    public enum Severity {

        /** L'erreur (la ligne s'en pare au rouge). */
        ERROR,

        /** L'avertissement (le triangle ambre). */
        WARNING,

        /** Le renseignement (le « i » à l'accent). */
        INFO
    }
}
