package jo.pigma.bench.generated;

/*
 * Description du lexeur XML du banc E0 : faisabilite de la generation.
 * Contrairement aux lexeurs Java et JSON, XML exige un etat : l'interieur
 * d'une balise (noms d'attributs, egale, valeur citee) se lexe autrement
 * que le contenu. C'est la forme reelle du contrat de lexeur : un petit
 * entier d'etat, YYINITIAL seul etat redemarrable.
 *
 * Approximations documentees : le corps d'un commentaire ne contient pas
 * "--" (forme interdite par la specification XML) et le corps d'une
 * section CDATA ne contient pas "]]" — le moteur reel affinera.
 */

%%

%class XmlLexer
%public
%implements LexerApi
%unicode
%char
%type int

%eofval{
    return YYEOF;
%eofval}

%{
    /* Types de jeton du banc : delimiteurs de balisage, noms, valeurs
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

%state TAG

Comment = "<!--" ([^-] | "-" [^-])* "-->"
Cdata = "<![CDATA[" ([^\]] | "]" [^\]])* "]]>"
Pi = "<?" ([^?] | "?" [^>?])* "?>"
Doctype = "<!" [^>]* ">"

Name = [A-Za-z_:] [A-Za-z0-9_.:-]*
AttrValue = \" [^\"]* \" | \' [^\']* \'
Entity = "&" [A-Za-z#0-9]+ ";"
WhiteSpace = [ \t\r\n]+
Text = [^<& \t\r\n]+

%%

<YYINITIAL> {
    {Comment}    { return COMMENT; }
    {Cdata}      { return CDATA; }
    {Pi}         { return MARKUP; }
    {Doctype}    { return DOCTYPE; }
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
