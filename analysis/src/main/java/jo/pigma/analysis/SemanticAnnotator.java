package jo.pigma.analysis;

import java.util.List;

import jo.pigma.syntax.SyntaxDocument;

/**
 * L3 : un producteur d'annotations, par ligne. Il lit les jetons de
 * L1 et la profondeur de L2 ; la fusion ({@code L3 > L2 > L1}) ne
 * colore qu'un jeton neutre de L1 — un producteur fautif ne peut pas
 * casser une couleur déjà affichée, c'est la règle qui protège la
 * fusion (elle vit au dessin, pas ici).
 */
public interface SemanticAnnotator {

    /**
     * Les annotations d'une ligne. Une plage peut déborder de la ligne
     * (le modèle la porte) ; le producteur de démonstration ne
     * l'utilise pas.
     *
     * @param line      indice de la ligne
     * @param depthIn   profondeur d'appariement en entrée de ligne (L2)
     * @param entry     les jetons de la ligne (L1)
     * @param text      le texte de la ligne
     * @param lineStart l'offset absolu du début de ligne
     * @return les annotations, jamais {@code null} (vide admis)
     */
    List<SemanticAnnotation> annotateLine(int line, int depthIn,
            SyntaxDocument.LineTokens entry, CharSequence text, int lineStart);
}
