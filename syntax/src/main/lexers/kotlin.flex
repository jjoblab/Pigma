package jo.pigma.syntax.lexers.generated;

/*
 * Description du lexeur Kotlin du moteur de coloration (voie generee).
 *
 * Le modele est celui de l'etude : le lexeur decoupe une FENETRE (une
 * ligne du document) et rend un etat de fin qui ouvre la fenetre
 * suivante. Les constructions qui traversent les lignes vivent donc en
 * ETATS : les chaines brutes (triple guillemet), les commentaires de
 * bloc et de documentation. Seul YYINITIAL (pile vide) est
 * redemarrable.
 *
 * Kotlin imbrique : un gabarit de chaine ouvre une expression (« ${
 * ... } ») qui peut contenir des chaines, elles-memes gabarits, et des
 * commentaires de bloc eux-memes imbriques. Ce contexte est une PILE :
 * le contrat du lexeur ne transporte que des petits entiers, la pile
 * est donc encodee canoniquement dans l'entier d'etat (4 bits par
 * contexte, profondeur 6) — deux piles egales, meme entier, l'inverse
 * reconstruit la pile exacte. Au-dela de la profondeur 6 (pathologique),
 * les ouvertures les plus profondes s'ignorent : la couleur ne change
 * pas, la fermeture la plus profonde ferme le contexte courant.
 *
 * Approximations documentees : une chaine classique non fermee dans la
 * fenetre retombe a l'etat initial (les chaines Kotlin non brutes ne
 * traversent pas les lignes — tout contexte vivant au-dessus d'une
 * telle chaine meurt avec elle en fin de fenetre) ; le contenu des
 * gabarits se lexe comme du code ; les formes de nombres couvrent
 * l'usage courant.
 */

%%

%class KotlinLexer
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
    public static final int RAW_STRING = 5;
    public static final int STRING_ESCAPE = 6;
    public static final int COMMENT_LINE = 7;
    public static final int COMMENT_BLOCK = 8;
    public static final int COMMENT_DOC = 9;
    public static final int OPERATOR = 10;
    public static final int PUNCT = 11;
    public static final int WHITESPACE = 12;
    public static final int ERROR = 99;

    /* ---- pile de contextes, encodee dans l'entier d'etat ---- */

    /** Profondeur maximale : 4 bits par contexte, 31 bits d'etat. */
    private static final int MAX_PILE_DEPTH = 6;

    private final java.util.ArrayDeque<Integer> pile =
            new java.util.ArrayDeque<>();

    /**
     * Ouvre un contexte : l'etat courant reprendra a sa fermeture.
     * Au-dela de la profondeur maximale, l'ouverture s'ignore.
     */
    private void pushState() {
        if (pile.size() < MAX_PILE_DEPTH) {
            pile.addLast(Integer.valueOf(yystate()));
        }
    }

    /**
     * Ferme un contexte.
     *
     * @return l'etat a reprendre, YYINITIAL si la pile est vide
     */
    private int popState() {
        return pile.isEmpty() ? YYINITIAL : pile.removeLast().intValue();
    }

    /**
     * L'etat compose a transporter a la fenetre suivante : plat dans
     * les 4 bits bas, la pile au-dessus (fond vers sommet, 4 bits par
     * contexte). Encodage bijectif : deux piles egales, meme entier.
     *
     * @return l'etat compose, 0 hors construction traversante
     */
    public int contextState() {
        if (!survivesLineBoundary()) {
            pile.clear();
            yybegin(YYINITIAL);
            return YYINITIAL;
        }
        int code = 0;
        for (int element : pile) {
            code = (code << 4) | element;
        }
        return yystate() | (code << 4);
    }

    /**
     * Restore l'etat compose apres yyreset : le plat reprend, la pile
     * se reconstruit du sommet au fond.
     *
     * @param composite l'etat rendu par contextState
     */
    public void restoreContext(int composite) {
        int flat = composite & 0xF;
        int code = composite >>> 4;
        pile.clear();
        while (code != 0) {
            pile.addFirst(Integer.valueOf(code & 0xF));
            code >>>= 4;
        }
        yybegin(flat);
    }

    /**
     * Les chaines classiques ne traversent pas les lignes : un tel
     * contexte (et tout ce qui vit au-dessus) meurt a la frontiere.
     *
     * @return vrai si l'etat compose traverse la fin de fenetre
     */
    private boolean survivesLineBoundary() {
        if (yystate() == STR) {
            return false;
        }
        for (int element : pile) {
            if (element == STR) {
                return false;
            }
        }
        return true;
    }
%}

%state BLOCK, DOC, RAW, STR, TEMPLATE

LineTerminator = \r\n | [\r\n]
WhiteSpace = {LineTerminator} | [ \t\f\x0B]

Identifier = [:jletter:] [:jletterdigit:]*

Keyword = as | abstract | actual | annotation | break | by | catch |
           class | companion | const | constructor | continue |
           crossinline | data | delegate | do | dynamic | else | enum |
           expect | external | false | field | final | finally | for |
           fun | get | if | import | in | infix | init | inline |
           inner | interface | internal | is | lateinit | noinline |
           null | object | open | operator | out | override | package |
           param | private | property | protected | public | receiver |
           reified | return | sealed | set | setparam | super |
           suspend | tailrec | this | throw | true | try | typealias |
           typeof | val | var | vararg | when | where | while

DecDigits = [0-9] [0-9_]*
Exponent = [eE] [+\-]? {DecDigits}
IntSuffix = [uU] [lL]? | [lL] [uU]?
CharLiteral = \' ( [^\'\n] | \\ [^\'\n]{1,5} ) \'
HexLiteral = 0 [xX] [0-9a-fA-F_]+ {IntSuffix}?
BinLiteral = 0 [bB] [01_]+ {IntSuffix}?
DecLiteral = {DecDigits} {IntSuffix}?
Number = {DecLiteral} ("." {DecDigits})? {Exponent}? [fF]?
        | "." {DecDigits} {Exponent}? [fF]?
        | {HexLiteral}
        | {BinLiteral}

Operator = "?:" | "?." | "::" | "..<" | ".." | "==" | "!=" | "<=" |
           ">=" | "&&" | "||" | "++" | "--" | "+=" | "-=" | "*=" |
           "/=" | "%=" | "->" | "!!" | "+" | "-" | "*" | "/" | "%" |
           "=" | "<" | ">" | "!" | "&" | "|" | "^" | "?" | ":"

Punct = "(" | ")" | "{" | "}" | "[" | "]" | ";" | "," | "." | "@"

%%

<YYINITIAL> {
    "/**"           { pushState(); yybegin(DOC); return COMMENT_DOC; }
    "/*"            { pushState(); yybegin(BLOCK); return COMMENT_BLOCK; }
    "//" [^\r\n]*   { return COMMENT_LINE; }
    {WhiteSpace}    { return WHITESPACE; }
    "\"\"\""        { pushState(); yybegin(RAW); return RAW_STRING; }
    \"              { pushState(); yybegin(STR); return STRING; }
    {HexLiteral}    { return NUMBER; }
    {BinLiteral}    { return NUMBER; }
    {Number}        { return NUMBER; }
    {CharLiteral}   { return STRING; }
    {CharLiteral}   { return STRING; }
    {Keyword}       { return KEYWORD; }
    {Identifier}    { return IDENTIFIER; }
    {Operator}      { return OPERATOR; }
    {Punct}         { return PUNCT; }
    [^]             { return ERROR; }
}

<BLOCK> {
    "/*"                    { pushState(); return COMMENT_BLOCK; }
    "*/"                    { yybegin(popState()); return COMMENT_BLOCK; }
    "/"                     { return COMMENT_BLOCK; }
    ([^*/] | \*+ [^*/])+    { return COMMENT_BLOCK; }
    "*"                     { return COMMENT_BLOCK; }
    [^]                     { return COMMENT_BLOCK; }
}

<DOC> {
    "/*"                    { pushState(); return COMMENT_DOC; }
    "*/"                    { yybegin(popState()); return COMMENT_DOC; }
    "/"                     { return COMMENT_DOC; }
    ([^*/] | \*+ [^*/])+    { return COMMENT_DOC; }
    "*"                     { return COMMENT_DOC; }
    [^]                     { return COMMENT_DOC; }
}

<RAW> {
    "\"\"\""                          { yybegin(popState()); return RAW_STRING; }
    "${"                              { pushState(); yybegin(TEMPLATE); return RAW_STRING; }
    "$" [:jletter:] [:jletterdigit:]* { return RAW_STRING; }
    \"                                { return RAW_STRING; }
    [^\"$]+                           { return RAW_STRING; }
    [^]                               { return RAW_STRING; }
}

<STR> {
    "${"                              { pushState(); yybegin(TEMPLATE); return STRING; }
    "$" [:jletter:] [:jletterdigit:]* { return STRING; }
    \\ [^\r\n]                        { return STRING_ESCAPE; }
    \"                                { yybegin(popState()); return STRING; }
    [^\"$\\]+                         { return STRING; }
    [^]                               { return STRING; }
}

<TEMPLATE> {
    "/**"           { pushState(); yybegin(DOC); return COMMENT_DOC; }
    "/*"            { pushState(); yybegin(BLOCK); return COMMENT_BLOCK; }
    "//" [^\r\n]*   { return COMMENT_LINE; }
    {WhiteSpace}    { return WHITESPACE; }
    "\"\"\""        { pushState(); yybegin(RAW); return RAW_STRING; }
    \"              { pushState(); yybegin(STR); return STRING; }
    "{"             { pushState(); return PUNCT; }
    "}"             { int resume = popState();
                      yybegin(resume);
                      return (resume == TEMPLATE || resume == YYINITIAL)
                              ? PUNCT : STRING; }
    {HexLiteral}    { return NUMBER; }
    {BinLiteral}    { return NUMBER; }
    {Number}        { return NUMBER; }
    {CharLiteral}   { return STRING; }
    {Keyword}       { return KEYWORD; }
    {Identifier}    { return IDENTIFIER; }
    {Operator}      { return OPERATOR; }
    {Punct}         { return PUNCT; }
    [^]             { return ERROR; }
}
