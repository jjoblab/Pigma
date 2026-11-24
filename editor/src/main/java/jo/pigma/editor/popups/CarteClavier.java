package jo.pigma.editor.popups;

/**
 * Le contrat clavier d'une carte de popup, à la façon de celeris
 * (KeyboardPopup) : la carte ne prend JAMAIS le focus (PIEGE-27) —
 * le clavier logiciel et la connexion d'entrée restent à l'éditeur,
 * qui rend à la carte les verdicts des touches. Vrai : la carte a
 * pris la touche ; faux : la touche retourne à la vie normale de
 * l'éditeur.
 */
public interface CarteClavier {

    /**
     * Le texte tapé va à la carte (la requête de recherche s'y
     * écrit, les chiffres de l'aller-à s'y posent) — un insert
     * NEUF, pas une composition.
     *
     * @param texte le texte commis par le clavier
     * @return vrai quand la carte l'a pris
     */
    boolean surTexte(CharSequence texte);

    /**
     * La composition du clavier remplace ce qu'elle avait posé (le
     * clavier logiciel compose mot à mot — « g », « gr »,
     * « gri »… chaque mise à jour REMPLACE la précédente, et la
     * validation finale aussi).
     *
     * @param texte l'état courant de la composition
     * @return vrai quand la carte l'a pris
     */
    boolean surComposition(CharSequence texte);

    /**
     * Le retour arrière efface dans la carte.
     *
     * @return vrai quand la carte l'a pris
     */
    boolean surRetourArriere();

    /**
     * La flèche verticale déléguée navigue la carte (les
     * correspondances de la recherche).
     *
     * @param pas négatif vers le haut, positif vers le bas
     * @return vrai quand la carte l'a prise
     */
    boolean surFleche(int pas);

    /**
     * La validation choisit (la correspondance suivante, l'aller-à
     * posé).
     *
     * @return vrai quand la carte l'a prise
     */
    boolean surValidation();
}
