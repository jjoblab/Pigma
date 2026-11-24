package jo.pigma.syntax;

import java.io.StringReader;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jo.pigma.syntax.generic.GenericLexer;
import jo.pigma.syntax.generic.LexerProfile;
import jo.pigma.syntax.lexer.FlexLexerAdapter;
import jo.pigma.syntax.lexers.generated.JavaLexer;
import jo.pigma.syntax.lexers.generated.JsonLexer;
import jo.pigma.syntax.lexers.generated.XmlLexer;

/**
 * Registre des langages livrés avec le moteur : les trois lexeurs
 * générés (Java, XML, JSON) et la famille générique armée d'un profil
 * SQL. Un hôte ajoute ses langages par ses propres descripteurs ; ce
 * registre ne sert que de point de départ cohérent.
 */
public final class Languages {

    /** Langage Java (lexeur généré). */
    public static final LanguageSyntax JAVA = new LanguageSyntax(
            "Java", List.of("java", "jav"),
            () -> new FlexLexerAdapter(
                    () -> new JavaLexer(new StringReader(""))),
            new Scope[] {
                    null,
                    Scope.of("keyword"),
                    null,
                    Scope.of("number"),
                    Scope.of("string"),
                    Scope.of("comment"),
                    Scope.of("comment"),
                    Scope.of("comment.doc"),
                    Scope.of("operator"),
                    Scope.of("punct"),
                    null
            },
            LanguageSyntax.initialStateOnly());

    /** Langage XML (lexeur généré). */
    public static final LanguageSyntax XML = new LanguageSyntax(
            "XML", List.of("xml", "xsd", "xsl"),
            () -> new FlexLexerAdapter(
                    () -> new XmlLexer(new StringReader(""))),
            new Scope[] {
                    null,
                    Scope.of("punct"),
                    Scope.of("tag"),
                    Scope.of("string"),
                    Scope.of("comment"),
                    Scope.of("string"),
                    Scope.of("comment"),
                    Scope.of("entity"),
                    null,
                    null
            },
            LanguageSyntax.initialStateOnly());

    /** Langage JSON (lexeur généré). */
    public static final LanguageSyntax JSON = new LanguageSyntax(
            "JSON", List.of("json", "jsonc"),
            () -> new FlexLexerAdapter(
                    () -> new JsonLexer(new StringReader(""))),
            new Scope[] {
                    null,
                    Scope.of("punct"),
                    Scope.of("string"),
                    Scope.of("number"),
                    Scope.of("keyword"),
                    null,
                    Scope.of("key")
            },
            LanguageSyntax.initialStateOnly());

    /** Profil SQL de la famille générique. */
    public static final LexerProfile SQL_PROFILE = new LexerProfile(
            "SQL",
            List.of("--"),
            "/*",
            "*/",
            List.of("''"),
            (char) 0,
            Set.of("select", "from", "where", "and", "or", "not", "insert",
                    "into", "values", "update", "set", "delete", "create",
                    "table", "drop", "alter", "join", "left", "right",
                    "inner", "outer", "on", "as", "order", "by", "group",
                    "having", "limit", "offset", "distinct", "null",
                    "true", "false", "count", "sum", "avg", "min", "max",
                    "primary", "key", "foreign", "references", "index",
                    "view", "if", "exists", "case", "when", "then",
                    "else", "end", "begin", "commit", "rollback",
                    "with", "union", "all", "between", "like", "in",
                    "is", "asc", "desc"),
            false);

    /** Langage SQL (famille générique). */
    public static final LanguageSyntax SQL = new LanguageSyntax(
            "SQL", List.of("sql"),
            () -> new GenericLexer(SQL_PROFILE),
            new Scope[] {
                    null,
                    Scope.of("keyword"),
                    null,
                    Scope.of("number"),
                    Scope.of("string"),
                    Scope.of("comment"),
                    Scope.of("operator"),
                    Scope.of("punct"),
                    null
            },
            LanguageSyntax.initialStateOnly());

    private Languages() {
    }

    /**
     * Le langage servi pour l'extension d'un nom de fichier, parmi les
     * langages livrés.
     *
     * @param fileName nom de fichier, jamais {@code null}
     * @return le descripteur, ou {@code null} si aucun ne sert cette
     *         extension
     */
    public static LanguageSyntax byFileName(String fileName) {
        String name = fileName.toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? name : name.substring(dot + 1);
        for (LanguageSyntax candidate : new LanguageSyntax[] {JAVA, XML, JSON, SQL}) {
            if (candidate.servesExtension(extension)) {
                return candidate;
            }
        }
        return null;
    }
}
