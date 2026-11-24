package jo.pigma.bench.generated;

/*
 * Description du lexeur Java du banc E0 : faisabilite de la generation.
 * La syntaxe est celle de JFlex (declarations puis regles). Les
 * commentaires de description restent en francais ; le code genere vit
 * hors depot (bench/out/, ignore par git) tant que la decision de
 * committer le code genere n'est pas prise.
 *
 * Le lexeur du banc est sans etat : tout se lexte depuis YYINITIAL,
 * seul etat redemarrable — la forme minimale du test de redemarrage.
 * Le moteur reel portera des etats (commentaire de bloc, gabarit de
 * chaine) ; le banc mesure la generation, pas la richesse.
 */

%%

%class JavaLexer
%public
%implements LexerApi
%unicode
%char
%type int

%eofval{
    return YYEOF;
%eofval}

%{
    /*
     * Types de jeton du banc : petits entiers sans couleur. La table
     * jeton vers portee viendra avec le moteur de coloration ; ici, on
     * mesure la generation, le poids et la vitesse.
     */
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

LineTerminator = \r\n | [\r\n]
WhiteSpace = {LineTerminator} | [ \t\f\x0B]

DocComment = "/**" ([^*] | \*+ [^*/])* \*+ "/"
BlockComment = "/*" ([^*] | \*+ [^*/])* \*+ "/"
LineComment = "//" [^\r\n]*

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
    {WhiteSpace}     { return WHITESPACE; }
    {DocComment}     { return COMMENT_DOC; }
    {BlockComment}   { return COMMENT_BLOCK; }
    {LineComment}    { return COMMENT_LINE; }
    {StringLiteral}  { return STRING; }
    {CharLiteral}    { return STRING; }
    {HexLiteral}     { return NUMBER; }
    {BinLiteral}     { return NUMBER; }
    {OctLiteral}     { return NUMBER; }
    {Number}         { return NUMBER; }
    {Keyword}        { return KEYWORD; }
    {Identifier}     { return IDENTIFIER; }
    {Operator}       { return OPERATOR; }
    {Punct}          { return PUNCT; }
    [^]              { return ERROR; }
}
