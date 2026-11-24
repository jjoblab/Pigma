package jo.pigma.syntax.lexers.generated;

/*
 * Description du lexeur JSON du moteur de coloration.
 *
 * Aucune construction JSON ne traverse les lignes : le lexeur est sans
 * etat, tout se lexe depuis YYINITIAL, seul etat redemarrable. Les
 * cles (chaine suivie du deux-points sur la meme ligne) forment un
 * type distinct par contexte arriere — la chaine seule reste STRING.
 */

%%

%class JsonLexer
%public
%implements jo.pigma.syntax.lexer.FlexLexer
%unicode
%type int

%eofval{
    return YYEOF;
%eofval}

%{
    /* Types de jeton : ponctuation, chaine, nombre, litteral, blancs,
       cle (chaine suivie de deux-points). */
    public static final int PUNCT = 1;
    public static final int STRING = 2;
    public static final int NUMBER = 3;
    public static final int LITERAL = 4;
    public static final int WHITESPACE = 5;
    public static final int KEY = 6;
    public static final int ERROR = 99;
%}

StringLiteral = \" ([^\\\"\n] | \\ ([\"\\\/bfnrt] | u [0-9a-fA-F]{4}))* \"
Int = "0" | [1-9] [0-9]*
Number = \-? {Int} ("." [0-9]+)? ([eE] [+\-]? [0-9]+)?
Literal = true | false | null
WhiteSpace = [ \t\r\n]+

%%

<YYINITIAL> {
    {StringLiteral} / [ \t]* ":"    { return KEY; }
    {StringLiteral}                 { return STRING; }
    {Number}                        { return NUMBER; }
    {Literal}                       { return LITERAL; }
    "{"                             { return PUNCT; }
    "}"                             { return PUNCT; }
    "["                             { return PUNCT; }
    "]"                             { return PUNCT; }
    ":"                             { return PUNCT; }
    ","                             { return PUNCT; }
    {WhiteSpace}                    { return WHITESPACE; }
    [^]                             { return ERROR; }
}
