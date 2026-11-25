package jo.pigma.lsp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.InlayHint;

/**
 * Un indice posé dans le texte ({@code textDocument/inlayHint}) : le
 * serveur y rend une étiquette à dessiner DANS la ligne, à la position
 * donnée — le compte des usages d'une déclaration, le type deviné
 * d'une variable, le nom du paramètre d'un appel. Le texte n'en fait
 * pas partie (le document ne bouge pas), l'encre secondaire le dessine
 * au bout du contenu.
 *
 * @param position la position de l'indice (la colonne y porte le bout
 *                  du contenu, la fin de ligne d'ordinaire)
 * @param label     l'étiquette à dessiner, jamais vide
 */
public record LspInlayHint(LspPosition position, String label) {

    /**
     * L'adaptation d'un indice du protocole (l'étiquette en deux
     * formes — la chaîne seule ou les morceaux ; les morceaux se
     * collent bout à bout).
     *
     * @param hint l'indice du protocole
     * @return l'indice du domaine, null si incomplet
     */
    public static LspInlayHint fromLsp(InlayHint hint) {
        if (hint == null || hint.getPosition() == null || hint.getLabel() == null) {
            return null;
        }
        String label;
        if (hint.getLabel().isLeft()) {
            label = hint.getLabel().getLeft();
        } else if (hint.getLabel().isRight() && hint.getLabel().getRight() != null) {
            StringBuilder joined = new StringBuilder();
            for (org.eclipse.lsp4j.InlayHintLabelPart part : hint.getLabel().getRight()) {
                if (part != null && part.getValue() != null) {
                    joined.append(part.getValue());
                }
            }
            label = joined.toString();
        } else {
            return null;
        }
        if (label == null || label.isEmpty()) {
            return null;
        }
        return new LspInlayHint(LspPosition.fromLsp(hint.getPosition()), label);
    }

    /**
     * L'adaptation de la réponse du protocole.
     *
     * @param answer les indices du protocole
     * @return les indices du domaine, jamais null
     */
    public static List<LspInlayHint> fromLsp(List<InlayHint> answer) {
        List<LspInlayHint> hints = new ArrayList<>();
        if (answer != null) {
            for (InlayHint hint : answer) {
                LspInlayHint adapted = fromLsp(hint);
                if (adapted != null) {
                    hints.add(adapted);
                }
            }
        }
        return hints;
    }
}
