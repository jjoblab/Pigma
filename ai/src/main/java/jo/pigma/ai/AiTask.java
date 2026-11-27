package jo.pigma.ai;

/**
 * Les tâches que l'intégration de l'IA sait demander (B0) : ce que
 * l'HÔTE déclare, seul cela apparaît dans l'interface de Pigma —
 * une tâche non déclarée ne s'y propose jamais.
 */
public enum AiTask {

    /** Expliquer le symbole ou la sélection sous le curseur (la
     * réponse se LIT : du Markdown rendu, rien ne se pose). */
    EXPLAIN,

    /** Expliquer l'ERREUR diagnostiquée (le message du diagnostic
     * voyage dans la demande ; la réponse se lit aussi). */
    EXPLAIN_ERROR,

    /** Corriger l'erreur diagnostiquée (la réponse se POSE : un
     * remplacement, un aperçu, une unité d'annulation). */
    FIX,

    /** Modifier la sélection selon l'instruction de l'utilisateur
     * (la réponse se pose : un remplacement de la sélection). */
    EDIT_SELECTION,

    /** La complétion en ligne (le texte fantôme au curseur ; la
     * réponse se pose à l'acceptation). */
    INLINE_COMPLETION,

    /** Le commentaire de documentation de la déclaration (la
     * réponse se pose AU-DESSUS d'elle, en aperçu). */
    DOCUMENT,

    /** Le test de la déclaration (le résultat revient À L'HÔTE :
     * lui seul sait où créer un fichier — Pigma n'en crée jamais). */
    WRITE_TEST
}
