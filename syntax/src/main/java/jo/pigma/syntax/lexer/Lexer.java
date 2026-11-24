package jo.pigma.syntax.lexer;

/**
 * Contrat du lexeur L1 : une machine à petits états qui découpe une
 * fenêtre de texte en jetons typés, sans couleur ni interprétation.
 *
 * <p>La fenêtre {@code [at, end)} d'un même texte peut être rexée
 * plusieurs fois ; l'{@code état} initial rendu par {@link #state()} à
 * la fin d'une fenêtre ouvre la fenêtre suivante — c'est ainsi qu'un
 * commentaire de bloc ou une balise ouverte traversent les lignes.
 * L'état est un petit entier déclaré par le langage ; un état est
 * redémarrable si relexer depuis n'importe quelle fenêtre démarrant
 * dans cet état redonne les mêmes jetons que le flux complet (par
 * défaut, seul l'état initial l'est).</p>
 *
 * <p>Le type {@code 0} marque l'absence de jeton courant (fin de
 * fenêtre) ; les types utiles commencent à 1. Un jeton occupe la plage
 * {@code [tokenStart(), tokenEnd())} en offsets absolus du texte
 * donné. Aucune méthode ne rend {@code null}.</p>
 */
public interface Lexer {

    /** Type du jeton absent (fin de fenêtre atteinte). */
    int NO_TOKEN = 0;

    /**
     * Prépare le lexage d'une fenêtre du texte dans l'état donné.
     *
     * @param text  texte porteur, jamais modifié
     * @param at    début de la fenêtre, inclus
     * @param end   fin de la fenêtre, exclue
     * @param state état initial du lexeur pour cette fenêtre
     */
    void start(CharSequence text, int at, int end, int state);

    /**
     * Avance d'un jeton ; à la fin de la fenêtre, le type devient
     * {@link #NO_TOKEN} et les appels suivants restent sans effet.
     */
    void advance();

    /**
     * Type du jeton courant, ou {@link #NO_TOKEN} après la fin.
     *
     * @return le type, jamais négatif
     */
    int tokenType();

    /**
     * Début du jeton courant en offset absolu du texte.
     *
     * @return le début, égal à {@code end} après la fin
     */
    int tokenStart();

    /**
     * Fin du jeton courant en offset absolu du texte.
     *
     * @return la fin, égale à {@code end} après la fin
     */
    int tokenEnd();

    /**
     * État du lexeur après le dernier jeton reconnu : à reporter sur
     * la fenêtre suivante.
     *
     * @return l'état courant
     */
    int state();
}
