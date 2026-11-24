package jo.pigma.bench.generated;

import java.io.IOException;

/**
 * Interface commune des lexeurs generes du banc : chaque description
 * la declare ({@code %implements LexerApi}) pour que le pilote pilote
 * les trois langages sans distinction.
 */
public interface LexerApi {

    /**
     * Avance d'un jeton et rend son type ; {@code YYEOF} a la fin.
     *
     * @return le type du jeton courant
     * @throws IOException si la lecture echoue
     */
    int yylex() throws IOException;

    /**
     * Texte du jeton courant.
     *
     * @return le texte reconnu, jamais {@code null}
     */
    String yytext();

    /**
     * Longueur du jeton courant.
     *
     * @return la longueur en caracteres
     */
    int yylength();

    /**
     * Caractere du jeton courant a la position relative donnee.
     *
     * @param position position relative, 0 etant le debut du jeton
     * @return le caractere a cette position
     */
    char yycharat(int position);

    /**
     * Etat courant du lexeur (petit entier).
     *
     * @return l'etat apres le dernier jeton reconnu
     */
    int yystate();
}
