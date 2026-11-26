package jo.pigma.syntax;

/**
 * La règle d'appariement des signes (B1 du rapport) : un ouvrant se
 * referme par SON fermant — « ( » attend « ) », « { » attend « } »,
 * « [ » attend « ] ». Les profondeurs seules ne suffisent pas : la
 * marche qui ne compte que les crans y encadre « (foo] » comme une
 * paire valide.
 *
 * <p>La classe vit sans état : chaque méthode statique reçoit le
 * signe et répond. Les trois consommateurs (l'encadrement du
 * curseur, les diagnostics, l'annotateur sémantique) y lisent LA
 * même règle — le facteur commun du rapport.</p>
 */
public final class BracketMatcher {

    private BracketMatcher() {
    }

    /**
     * Le signe est-il un ouvrant ?
     *
     * @param sign le caractère examiné
     * @return vrai pour « ( », « { » et « [ »
     */
    public static boolean isOpening(char sign) {
        return sign == '(' || sign == '{' || sign == '[';
    }

    /**
     * Le signe est-il un fermant ?
     *
     * @param sign le caractère examiné
     * @return vrai pour « ) », « } » et « ] »
     */
    public static boolean isClosing(char sign) {
        return sign == ')' || sign == '}' || sign == ']';
    }

    /**
     * Le fermant attendu d'un ouvrant.
     *
     * @param opening l'ouvrant (« ( », « { » ou « [ »)
     * @return son fermant, 0 si le signe n'ouvre pas
     */
    public static char closerOf(char opening) {
        switch (opening) {
            case '(':
                return ')';
            case '{':
                return '}';
            case '[':
                return ']';
            default:
                return 0;
        }
    }

    /**
     * L'ouvrant attendu d'un fermant.
     *
     * @param closing le fermant (« ) », « } » ou « ] »)
     * @return son ouvrant, 0 si le signe ne ferme pas
     */
    public static char openerOf(char closing) {
        switch (closing) {
            case ')':
                return '(';
            case '}':
                return '{';
            case ']':
                return '[';
            default:
                return 0;
        }
    }

    /**
     * Un fermant referme-t-il un ouvrant ? La règle du type : les
     * signes doivent venir de la même famille.
     *
     * @param opening l'ouvrant proposé
     * @param closing le fermant proposé
     * @return vrai si le fermant referme CET ouvrant
     */
    public static boolean closes(char opening, char closing) {
        return closing != 0 && closerOf(opening) == closing;
    }
}
