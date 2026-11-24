package jo.pigma.syntax.lexer;

import java.io.IOException;

/**
 * Forme publique des lexeurs générés par JFlex : les méthodes que le
 * squelette génère pour une description {@code %type int}, telles que
 * l'adaptateur les appelle. Chaque description de lexeur du dépôt
 * déclare {@code %implements} de cette interface ; le code généré
 * n'ajoute ni ne retire rien.
 */
public interface FlexLexer {

    /**
     * Valeur que JFlex rend en fin de fenêtre pour {@code %type int} :
     * l'adaptateur la traduit en {@link Lexer#NO_TOKEN}.
     */
    int FLEX_EOF = -1;

    /**
     * Reconnaît le jeton suivant et rend son type.
     *
     * @return le type, ou {@link #FLEX_EOF} en fin de fenêtre
     * @throws IOException si la lecture échoue (jamais en pratique :
     *                     la fenêtre vit en mémoire)
     */
    int yylex() throws IOException;

    /**
     * Longueur du jeton courant.
     *
     * @return la longueur en caractères
     */
    int yylength();

    /**
     * Remplace la fenêtre de lecture sans changer l'état.
     *
     * @param reader la nouvelle fenêtre, jamais {@code null}
     */
    void yyreset(java.io.Reader reader);

    /**
     * Entre dans l'état donné.
     *
     * @param state l'état, constante de la classe générée
     */
    void yybegin(int state);

    /**
     * État courant du lexeur.
     *
     * @return l'état
     */
    int yystate();
}
