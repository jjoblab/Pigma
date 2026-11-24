package jo.pigma.bench.generated;

/*
 * Description du lexeur JSON du banc E0 : faisabilite de la generation.
 * Le JSON se laisse lexter sans etat : c'est le format le plus simple
 * des trois, et il sert d'etalon bas pour la mesure de poids et de
 * vitesse. Les cles (chaines suivies de deux-points) ne forment pas un
 * type distinct ici : le moteur reel s'appuiera sur le contexte.
 */

%%

%class JsonLexer
%public
%implements LexerApi
%unicode
%char
%type int

%eofval{
    return YYEOF;
%eofval}

%{
    /* Types de jeton du banc : ponctuation, chaine, nombre, litteral,
       blancs. */
    public static final int PUNCT = 1;
    public static final int STRING = 2;
    public static final int NUMBER = 3;
    public static final int LITERAL = 4;
    public static final int WHITESPACE = 5;
    public static final int ERROR = 99;
%}

StringLiteral = \" ([^\\\"\n] | \\ ([\"\\\/bfnrt] | u [0-9a-fA-F]{4}))* \"
Int = "0" | [1-9] [0-9]*
Number = \-? {Int} ("." [0-9]+)? ([eE] [+\-]? [0-9]+)?
Literal = true | false | null
WhiteSpace = [ \t\r\n]+

%%

<YYINITIAL> {
    {StringLiteral} { return STRING; }
    {Number}        { return NUMBER; }
    {Literal}       { return LITERAL; }
    "{"             { return PUNCT; }
    "}"             { return PUNCT; }
    "["             { return PUNCT; }
    "]"             { return PUNCT; }
    ":"             { return PUNCT; }
    ","             { return PUNCT; }
    {WhiteSpace}    { return WHITESPACE; }
    [^]             { return ERROR; }
}
