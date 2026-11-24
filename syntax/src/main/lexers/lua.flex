package jo.pigma.syntax.lexers.generated;

/*
 * Description du lexeur Lua du moteur de coloration (voie generee).
 *
 * Le modele est celui de l'etude : le lexeur decoupe une FENETRE (une
 * ligne du document) et rend un etat de fin qui ouvre la fenetre
 * suivante. Les constructions qui traversent les lignes vivent donc en
 * ETATS : les longues chaines (« [[ ... ]] ») et les longues
 * commentaires (« --[[ ... ]] »), chacune a son niveau de crochets
 * (« [==[ ... ]==] »). Seul YYINITIAL est redemarrable.
 *
 * Le commentaire long demande une decision : apres « -- », un crochet
 * long ouvert (« [ » « = »* « [ ») ouvre la longue commentaire, tout
 * le reste de la ligne est commentaire court. L'appariement le plus
 * long pencherait pour le commentaire court (il avale la ligne) :
 * la decision vit donc dans l'etat APRES_TIRETS — le « -- » part
 * comme jeton de commentaire court, l'etat tranche ce qui suit.
 *
 * Le niveau (nombre de « = ») voyage avec l'etat plat dans l'entier
 * transporte par le moteur (4 bits d'etat, le niveau au-dessus) : une
 * construction ouverte en fenetre N se ferme en fenetre N+k au meme
 * niveau. Les chaines classiques ne traversent pas les lignes (une
 * chaine non fermee en fin de fenetre retombe a l'etat initial).
 *
 * Approximations documentees : une echappe « \ » suivie du saut de
 * ligne (continuation valide en Lua) meurt avec la fenetre comme les
 * chaines non fermees ; l'echappe Unicode « u » accolée (u{...})
 * colore l'echappe puis le corps comme du contenu ; les niveaux
 * au-dela de 2^27-1 (pathologique) se rabattent a ce plafond.
 */

%%

%class LuaLexer
%public
%implements jo.pigma.syntax.lexer.FlexLexer
%unicode
%type int

%eofval{
    return YYEOF;
%eofval}

%{
    /* Types de jeton : petits entiers sans couleur, la table de
       portees du langage fait le reste. */
    public static final int KEYWORD = 1;
    public static final int IDENTIFIER = 2;
    public static final int NUMBER = 3;
    public static final int STRING = 4;
    public static final int LONG_STRING = 5;
    public static final int STRING_ESCAPE = 6;
    public static final int COMMENT_LINE = 7;
    public static final int COMMENT_BLOCK = 8;
    public static final int OPERATOR = 9;
    public static final int PUNCT = 10;
    public static final int WHITESPACE = 11;
    public static final int ERROR = 99;

    /** Plafond du niveau de crochets (l'entier d'etat le transporte). */
    private static final int MAX_LEVEL = 0x07FFFFFF;

    /** Le niveau de la longue construction ouverte (« = » du crochet). */
    private int level;

    /**
     * Le niveau d'un crochet long dans le texte : les « = » apres le
     * crochet d'ouverture donne.
     *
     * @param text   le jeton (« [ », « = »*, « [ » ou « ] », « = »*, « ] »)
     * @param openAt l'offset du premier crochet
     * @return le nombre de « = »
     */
    private static int bracketLevel(CharSequence text, int openAt) {
        int count = 0;
        for (int at = openAt + 1; at < text.length(); at++) {
            char candidate = text.charAt(at);
            if (candidate == '=') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * L'etat compose a transporter a la fenetre suivante : plat dans
     * les 4 bits bas, le niveau au-dessus. Les chaines classiques et
     * la decision de commentaire ne traversent pas la fin de fenetre :
     * elles retombent a l'etat initial.
     *
     * @return l'etat compose, 0 hors construction traversante
     */
    public int contextState() {
        if (yystate() == YYINITIAL || yystate() == STR_D
                || yystate() == STR_S || yystate() == APRES_TIRETS) {
            return YYINITIAL;
        }
        return yystate() | (level << 4);
    }

    /**
     * Restore l'etat compose apres yyreset : le plat reprend, le
     * niveau se relit au-dessus des 4 bits.
     *
     * @param composite l'etat rendu par contextState
     */
    public void restoreContext(int composite) {
        yybegin(composite & 0xF);
        level = composite >>> 4;
    }
%}

%state LONGSTR, LONGCOMMENT, STR_D, STR_S, APRES_TIRETS

LineTerminator = \r\n | [\r\n]
WhiteSpace = {LineTerminator} | [ \t\f\x0B]

Identifier = [:jletter:] [:jletterdigit:]*

Keyword = and | break | do | else | elseif | end | false | for |
          function | goto | if | in | local | nil | not | or |
          repeat | return | then | true | until | while

HexDigits = [0-9a-fA-F]+
DecDigits = [0-9]+
HexFloat = 0 [xX] {HexDigits} ( "." {HexDigits}? )? [pP] [+\-]? {DecDigits}
HexInt = 0 [xX] {HexDigits}
DecFloat = {DecDigits} "." {DecDigits}? ([eE] [+\-]? {DecDigits})?
         | "." {DecDigits} ([eE] [+\-]? {DecDigits})?
         | {DecDigits} [eE] [+\-]? {DecDigits}
Number = {HexFloat} | {HexInt} | {DecFloat} | {DecDigits}

Operator = "==" | "~=" | "<=" | ">=" | "<<" | ">>" | "//" | "..." |
           ".." | "::" | "+" | "-" | "*" | "/" | "%" | "^" | "#" |
           "=" | "<" | ">" | "&" | "|" | "~"

Punct = "(" | ")" | "{" | "}" | "[" | "]" | ";" | ":" | "," | "."

%%

<YYINITIAL> {
    "--"                         { yybegin(APRES_TIRETS); return COMMENT_LINE; }
    "[" "="* "["                 { level = Math.min(bracketLevel(yytext(), 0), MAX_LEVEL);
                                   yybegin(LONGSTR); return LONG_STRING; }
    {WhiteSpace}                 { return WHITESPACE; }
    \"                           { yybegin(STR_D); return STRING; }
    \'                           { yybegin(STR_S); return STRING; }
    {Number}                     { return NUMBER; }
    {Keyword}                    { return KEYWORD; }
    {Identifier}                 { return IDENTIFIER; }
    {Operator}                   { return OPERATOR; }
    {Punct}                      { return PUNCT; }
    [^]                          { return ERROR; }
}

<APRES_TIRETS> {
    "[" "="* "["                 { level = Math.min(bracketLevel(yytext(), 0), MAX_LEVEL);
                                   yybegin(LONGCOMMENT); return COMMENT_BLOCK; }
    "[" "="*                     { return COMMENT_LINE; }
    [^\[\r\n] [^\r\n]*           { return COMMENT_LINE; }
    {LineTerminator}             { yybegin(YYINITIAL); return WHITESPACE; }
    [^]                          { return COMMENT_LINE; }
}

<LONGSTR> {
    "]" "="* "]"                 { if (bracketLevel(yytext(), 0) == level) {
                                       yybegin(YYINITIAL);
                                   }
                                   return LONG_STRING; }
    "]"                          { return LONG_STRING; }
    [^\]]+                       { return LONG_STRING; }
    [^]                          { return LONG_STRING; }
}

<LONGCOMMENT> {
    "]" "="* "]"                 { if (bracketLevel(yytext(), 0) == level) {
                                       yybegin(YYINITIAL);
                                   }
                                   return COMMENT_BLOCK; }
    "]"                          { return COMMENT_BLOCK; }
    [^\]]+                       { return COMMENT_BLOCK; }
    [^]                          { return COMMENT_BLOCK; }
}

<STR_D> {
    \\ [^\r\n]                   { return STRING_ESCAPE; }
    \"                           { yybegin(YYINITIAL); return STRING; }
    [^\"\\]+                     { return STRING; }
    [^]                          { return STRING; }
}

<STR_S> {
    \\ [^\r\n]                   { return STRING_ESCAPE; }
    \'                           { yybegin(YYINITIAL); return STRING; }
    [^\'\\]+                     { return STRING; }
    [^]                          { return STRING; }
}
