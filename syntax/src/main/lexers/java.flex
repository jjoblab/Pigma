package jo.pigma.syntax.lexers.generated;

/*
 * Description du lexeur Java du moteur de coloration.
 *
 * Le modele est celui de l'etude : le lexeur decoupe une FENETRE
 * (une ligne du document) et rend un etat de fin qui ouvre la fenetre
 * suivante. Les constructions qui traversent les lignes — commentaire
 * de bloc, commentaire de documentation — vivent donc en ETATS, pas en
 * expressions d'un seul tenant : un commentaire couvert se decoupe en
 * morceaux, tous du meme type, et l'etat porte la suite. Seul
 * YYINITIAL est redemarrable.
 *
 * Approximations documentees : une chaine non fermee dans la fenetre
 * rend un jeton erreur sur l'ouvrant (les chaines Java ne traversent
 * pas les lignes) ; les formes de nombres couvrent l'usage courant.
 */

%%

%class JavaLexer
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
    public static final int COMMENT_LINE = 5;
    public static final int COMMENT_BLOCK = 6;
    public static final int COMMENT_DOC = 7;
    public static final int OPERATOR = 8;
    public static final int PUNCT = 9;
    public static final int WHITESPACE = 10;
    public static final int ERROR = 99;
%}

%state BLOCK, DOC

LineTerminator = \r\n | [\r\n]
WhiteSpace = {LineTerminator} | [ \t\f\x0B]

StringLiteral = \" ([^\\\"\n] | \\ [^\n])* \"
CharLiteral = \' ([^\'\n] | \\ [^\n]{1,8}) \'

Keyword = abstract | assert | boolean | break | byte | case | catch |
           char | class | const | continue | default | do | double |
           else | enum | extends | final | finally | float | for |
           goto | if | implements | import | instanceof | int |
           interface | long | native | new | package | private |
           protected | public | record | return | sealed | short |
           static | strictfp | super | switch | synchronized | this |
           throw | throws | transient | try | var | void | volatile |
           while | yield | permits

Identifier = [:jletter:] [:jletterdigit:]*

HexLiteral = 0 [xX] [0-9a-fA-F_]+ [lL]?
BinLiteral = 0 [bB] [01_]+ [lL]?
OctLiteral = 0 [0-7_]+ [lL]?
DecInt = [0-9] [0-9_]*
Number = {DecInt} ("." {DecInt})? ([eE] [+\-]? {DecInt})? [fFdDlL]?
        | "." {DecInt} ([eE] [+\-]? {DecInt})? [fFdD]?

Operator = ">>>=" | ">>>" | "<<=" | ">>=" | "==" | "!=" | "<=" | ">=" |
           "&&" | "||" | "++" | "--" | "+=" | "-=" | "*=" | "/=" |
           "%=" | "&=" | "|=" | "^=" | "<<" | ">>" | "->" | "::" |
           "+" | "-" | "*" | "/" | "%" | "=" | "<" | ">" | "!" |
           "&" | "|" | "^" | "~" | "?" | ":"

Punct = "(" | ")" | "{" | "}" | "[" | "]" | ";" | "," | "." | "@" | "#"

%%

<YYINITIAL> {
    "/**"           { yybegin(DOC); return COMMENT_DOC; }
    "/*"            { yybegin(BLOCK); return COMMENT_BLOCK; }
    "//" [^\r\n]*   { return COMMENT_LINE; }
    {WhiteSpace}    { return WHITESPACE; }
    {StringLiteral} { return STRING; }
    {CharLiteral}   { return STRING; }
    {HexLiteral}    { return NUMBER; }
    {BinLiteral}    { return NUMBER; }
    {OctLiteral}    { return NUMBER; }
    {Number}        { return NUMBER; }
    {Keyword}       { return KEYWORD; }
    {Identifier}    { return IDENTIFIER; }
    {Operator}      { return OPERATOR; }
    {Punct}         { return PUNCT; }
    [^]             { return ERROR; }
}

<BLOCK> {
    "*/"                                { yybegin(YYINITIAL); return COMMENT_BLOCK; }
    ([^*] | \*+ [^*/])+                 { return COMMENT_BLOCK; }
    "*"                                 { return COMMENT_BLOCK; }
}

<DOC> {
    "*/"                                { yybegin(YYINITIAL); return COMMENT_DOC; }
    ([^*] | \*+ [^*/])+                 { return COMMENT_DOC; }
    "*"                                 { return COMMENT_DOC; }
}
