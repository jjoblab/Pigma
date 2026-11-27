package jo.pigma.ai;

/**
 * La réponse de l'IA (B0), scellée à trois formes — le texte qui se
 * LIT (du Markdown rendu, aucune pose), le REMPLACEMENT d'une plage
 * (la correction, la modification de sélection : un aperçu, une
 * unité d'annulation), l'INSERTION à une position (le commentaire
 * de documentation, la complétion acceptée).
 *
 * <p>La réponse est une DONNÉE, jamais une instruction : Pigma n'y
 * exécute rien, n'y suit aucun lien automatiquement, borne sa
 * taille à l'affichage. La plage visée par un remplacement se
 * VÉRIFIE avant de poser : un texte qui a bougé depuis la demande
 * ne se pose pas.</p>
 */
public sealed interface AiResult permits AiResult.Text,
        AiResult.Replacement, AiResult.Insertion {

    /**
     * La réponse qui se LIT : du Markdown (titres, listes, code) —
     * l'explication, l'explication d'erreur. Rien ne se pose dans
     * le document.
     *
     * @param markdown le corps balisé, jamais {@code null} (vide
     *                 permis — la réponse muette)
     */
    record Text(String markdown) implements AiResult {

        /** La forme canonique : le nul se vide. */
        public Text {
            markdown = markdown == null ? "" : markdown;
        }
    }

    /**
     * La réponse qui REMPLACE une plage : la correction (la plage du
     * diagnostic), la modification de sélection (la plage de la
     * sélection). La plage se vérifie avant la pose : le texte qui a
     * bougé depuis la demande ne se pose pas.
     *
     * @param start le début de la plage (décalage dans le document
     *              au moment de la demande, positif ou nul)
     * @param end   la fin de la plage (décalage, jamais avant le
     *              début)
     * @param text  le texte de remplacement, jamais {@code null}
     */
    record Replacement(int start, int end, String text) implements AiResult {

        /** La forme canonique : les bornes se vérifient, le nul se
         * vide. */
        public Replacement {
            if (start < 0 || end < start) {
                throw new IllegalArgumentException(
                        "range must be 0 <= start <= end, got " + start
                                + ".." + end);
            }
            text = text == null ? "" : text;
        }
    }

    /**
     * La réponse qui S'INSÈRE à une position : le commentaire de
     * documentation (au-dessus de la déclaration), la complétion
     * acceptée (au curseur).
     *
     * @param offset la position d'insertion (décalage dans le
     *               document au moment de la demande, positif ou
     *               nul)
     * @param text   le texte inséré, jamais {@code null}
     */
    record Insertion(int offset, String text) implements AiResult {

        /** La forme canonique : la position se vérifie, le nul se
         * vide. */
        public Insertion {
            if (offset < 0) {
                throw new IllegalArgumentException(
                        "offset must be >= 0, got " + offset);
            }
            text = text == null ? "" : text;
        }
    }
}
