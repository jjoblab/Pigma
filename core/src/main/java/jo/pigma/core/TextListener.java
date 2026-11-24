package jo.pigma.core;

/**
 * Observateur des modifications appliquées à un tampon de texte.
 *
 * <p>Un événement est émis après chaque remplacement effectif (édition,
 * annulation, rétablissement, retour arrière de transaction) : une édition
 * sans effet (plage vide et texte vide) n'émet rien. L'écouteur est
 * appelé sur le fil qui applique le changement ; il ne doit pas modifier
 * le texte en retour, sous peine de récursion.</p>
 */
public interface TextListener {

    /**
     * Appelé après l'application d'un remplacement au tampon.
     *
     * @param buffer         tampon modifié, jamais {@code null}
     * @param start          offset de début du remplacement effectué
     * @param removedLength  nombre d'unités de code retirées
     * @param insertedLength nombre d'unités de code insérées
     * @param revision       révision du texte après l'application
     */
    void textChanged(TextBuffer buffer, int start, int removedLength,
                     int insertedLength, int revision);
}
