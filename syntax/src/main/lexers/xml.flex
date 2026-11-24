package jo.pigma.syntax.lexers.generated;

/*
 * Description du lexeur XML du moteur de coloration.
 *
 * Le modele fenetre-par-fenetre (une ligne, un etat de fin) s'applique
 * a toutes les constructions qui traversent les lignes : balise
 * ouverte, commentaire, CDATA, instruction de traitement, doctype —
 * chacune un etat. Seul YYINITIAL est redemarrable.
 *
 * Approximations documentees : une valeur d'attribut non fermee dans
 * la fenetre rend l'ouvrant en erreur (les valeurs multi-lignes sont
 * rares) ; le corps d'un commentaire evite "--" (forme interdite par
 * la specification) et celui d'un CDATA evite "]]".
 */

%%

%class XmlLexer
%public
%implements jo.pigma.syntax.lexer.FlexLexer
%unicode
%type int

%eofval{
    return YYEOF;
%eofval}

%{
    /* Types de jeton : delimiteurs de balisage, noms, valeurs
       d'attribut, commentaire, CDATA, doctype, entite, texte. */
    public static final int MARKUP = 1;
    public static final int NAME = 2;
    public static final int ATTR_VALUE = 3;
    public static final int COMMENT = 4;
    public static final int CDATA = 5;
    public static final int DOCTYPE = 6;
    public static final int ENTITY = 7;
    public static final int TEXT = 8;
    public static final int WHITESPACE = 9;
    public static final int ERROR = 99;
%}

%state TAG, IN_COMMENT, IN_CDATA, IN_PI, IN_DOCTYPE

Name = [A-Za-z_:] [A-Za-z0-9_.:-]*
AttrValue = \" [^\"]* \" | \' [^\']* \'
Entity = "&" [A-Za-z#0-9]+ ";"
WhiteSpace = [ \t\r\n]+
Text = [^<& \t\r\n]+

%%

<YYINITIAL> {
    "<!--"       { yybegin(IN_COMMENT); return COMMENT; }
    "<![CDATA["  { yybegin(IN_CDATA); return CDATA; }
    "<?"         { yybegin(IN_PI); return MARKUP; }
    "<!"         { yybegin(IN_DOCTYPE); return DOCTYPE; }
    "</"         { yybegin(TAG); return MARKUP; }
    "<"          { yybegin(TAG); return MARKUP; }
    {Entity}     { return ENTITY; }
    {WhiteSpace} { return WHITESPACE; }
    {Text}       { return TEXT; }
    [^]          { return ERROR; }
}

<TAG> {
    {AttrValue}  { return ATTR_VALUE; }
    {Name}       { return NAME; }
    "/>"         { yybegin(YYINITIAL); return MARKUP; }
    ">"          { yybegin(YYINITIAL); return MARKUP; }
    "="          { return MARKUP; }
    "/"          { return MARKUP; }
    {WhiteSpace} { return WHITESPACE; }
    [^]          { return ERROR; }
}

<IN_COMMENT> {
    "-->"                          { yybegin(YYINITIAL); return COMMENT; }
    ([^-] | \-+ [^->])+            { return COMMENT; }
    "-"                            { return COMMENT; }
}

<IN_CDATA> {
    "]]>"                          { yybegin(YYINITIAL); return CDATA; }
    ([^\]] | \]+ [^\]>])+          { return CDATA; }
    "]"                            { return CDATA; }
}

<IN_PI> {
    "?>"                           { yybegin(YYINITIAL); return MARKUP; }
    ([^?] | \?+ [^?>])+            { return MARKUP; }
    "?"                            { return MARKUP; }
}

<IN_DOCTYPE> {
    ">"                            { yybegin(YYINITIAL); return DOCTYPE; }
    [^>]+                          { return DOCTYPE; }
}
