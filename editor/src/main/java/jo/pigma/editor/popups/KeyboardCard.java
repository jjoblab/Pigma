package jo.pigma.editor.popups;

/**
 * Le contrat clavier d'une carte de popup : la carte ne prend JAMAIS
 * le focus — le clavier logiciel et la connexion d'entrée restent à
 * l'éditeur, qui rend à la carte les verdicts des touches. Vrai : la
 * carte a pris la touche ; faux : la touche retourne à la vie normale
 * de l'éditeur.
 */
public interface KeyboardCard {

    /**
     * Le texte tapé va à la carte (la requête de recherche s'y
     * écrit, les chiffres de l'aller-à s'y posent) — un insert
     * NEUF, pas une composition.
     *
     * @param text le texte commis par le clavier
     * @return vrai quand la carte l'a pris
     */
    boolean onText(CharSequence text);

    /**
     * La composition du clavier remplace ce qu'elle avait posé (le
     * clavier logiciel compose mot à mot — « g », « gr »,
     * « gri »… chaque mise à jour REMPLACE la précédente, et la
     * validation finale aussi).
     *
     * @param text l'état courant de la composition
     * @return vrai quand la carte l'a pris
     */
    boolean onComposition(CharSequence text);

    /**
     * Le retour arrière efface dans la carte.
     *
     * @return vrai quand la carte l'a pris
     */
    boolean onBackspace();

    /**
     * La flèche verticale déléguée navigue la carte (les
     * correspondances de la recherche).
     *
     * @param step négatif vers le haut, positif vers le bas
     * @return vrai quand la carte l'a prise
     */
    boolean onArrow(int step);

    /**
     * La validation choisit (la correspondance suivante, l'aller-à
     * posé).
     *
     * @return vrai quand la carte l'a prise
     */
    boolean onEnter();
}
