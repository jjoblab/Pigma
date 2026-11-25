package jo.pigma.syntax;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Les mots-clés de complétion de chaque langage livré (la leçon de la
 * référence : sa liste statique {@code sKeywords} y porte le bandeau
 * SANS serveur de langage — la complétion de base naît du langage
 * lui-même). La vue fusionne ces mots avec son index des symboles ;
 * le serveur branché y prime toujours (sa réponse évince le local).
 */
public final class LanguageKeywords {

    /** Les mots-clés par nom de langage (le registre livré). */
    private static final Map<String, List<String>> BY_LANGUAGE = Map.ofEntries(
            Map.entry("Java", List.of(
                    "abstract", "assert", "boolean", "break", "byte", "case",
                    "catch", "char", "class", "const", "continue", "default",
                    "do", "double", "else", "enum", "extends", "final",
                    "finally", "float", "for", "goto", "if", "implements",
                    "import", "instanceof", "int", "interface", "long",
                    "native", "new", "package", "private", "protected",
                    "public", "return", "short", "static", "strictfp",
                    "super", "switch", "synchronized", "this", "throw",
                    "throws", "transient", "try", "var", "void", "volatile",
                    "while", "sealed", "permits", "record", "yield")),
            Map.entry("XML", List.of(
                    "xml", "DOCTYPE", "CDATA", "xmlns", "encoding", "version",
                    "ELEMENT", "ATTLIST", "ENTITY")),
            Map.entry("JSON", List.of(
                    "true", "false", "null")),
            Map.entry("Kotlin", List.of(
                    "as", "break", "class", "continue", "do", "else",
                    "false", "for", "fun", "if", "in", "interface", "is",
                    "null", "object", "package", "return", "super", "this",
                    "throw", "true", "try", "typealias", "val", "var", "when",
                    "while", "data", "sealed", "suspend", "companion",
                    "internal", "private", "protected", "public", "import")),
            Map.entry("Lua", List.of(
                    "and", "break", "do", "else", "elseif", "end", "false",
                    "for", "function", "goto", "if", "in", "local", "nil",
                    "not", "or", "repeat", "return", "then", "true",
                    "until", "while",
                    // la bibliothèque standard y vit : le bandeau sans
                    // serveur y propose les fonctions utiles
                    "print", "require", "pcall", "pairs", "ipairs",
                    "tonumber", "tostring", "type", "string", "table",
                    "math")),
            Map.entry("SQL", List.of(
                    "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES",
                    "UPDATE", "SET", "DELETE", "CREATE", "TABLE", "DROP",
                    "ALTER", "ADD", "PRIMARY", "KEY", "FOREIGN", "REFERENCES",
                    "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "ON", "AS",
                    "AND", "OR", "NOT", "NULL", "IS", "IN", "LIKE", "GROUP",
                    "BY", "ORDER", "HAVING", "LIMIT", "OFFSET", "DISTINCT",
                    "COUNT", "SUM", "AVG", "MIN", "MAX", "UNION", "ALL",
                    "EXISTS", "BETWEEN", "CASE", "WHEN", "THEN", "ELSE",
                    "END", "INDEX", "VIEW", "TRIGGER", "BEGIN", "COMMIT",
                    "ROLLBACK")));

    /**
     * Les mots-clés d'un langage.
     *
     * @param language le langage courant, {@code null} admis (aucun
     *                 mot-clé alors)
     * @return les mots-clés triés (une copie), liste vide sans registre
     */
    public static List<String> of(LanguageSyntax language) {
        if (language == null) {
            return List.of();
        }
        List<String> keywords = BY_LANGUAGE.get(language.name());
        if (keywords == null) {
            return List.of();
        }
        List<String> sorted = List.copyOf(keywords);
        sorted = sorted.stream()
                .sorted(String::compareToIgnoreCase)
                .toList();
        return sorted;
    }

    /**
     * Les mots-clés d'un nom de langage (le registre livré s'y connaît
     * par nom, la recherche s'y fait insensible à la casse).
     *
     * @param name le nom du langage ("Java", "Lua"…), {@code null}
     *             ou inconnu admis
     * @return les mots-clés triés (une copie), liste vide sans registre
     */
    public static List<String> of(String name) {
        if (name == null) {
            return List.of();
        }
        List<String> keywords = BY_LANGUAGE.get(
                name.toLowerCase(Locale.ROOT));
        if (keywords != null) {
            return keywords.stream()
                    .sorted(String::compareToIgnoreCase)
                    .toList();
        }
        // le nom exact d'abord (les registres y vivrent par nom de
        // langage affichable), la casse insensible ensuite
        for (Map.Entry<String, List<String>> entry : BY_LANGUAGE.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue().stream()
                        .sorted(String::compareToIgnoreCase)
                        .toList();
            }
        }
        return List.of();
    }

    private LanguageKeywords() {
    }
}
