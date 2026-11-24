package jo.pigma.analysis;

import java.util.ArrayList;
import java.util.List;

import jo.pigma.syntax.SyntaxDocument;

/**
 * Le producteur de démonstration : les déclarations des formes Java —
 * un type derrière {@code class}, {@code interface}, {@code enum} ou
 * {@code record} ; une méthode (identifiant suivi d'une parenthèse et
 * précédé d'un type — les appels qualifiés comme
 * {@code System.out.printf(} n'ont pas de type devant, les mots-clés
 * de contrôle non plus) ; un champ en corps de classe (identifiant
 * suivi de {@code ;} ou {@code =} à la profondeur 1 — les locaux
 * vivent plus bas). Les formes, pas la sémantique : un instrument de
 * démonstration, un serveur de langage branchera les siennes.
 */
public final class JavaDeclarations implements SemanticAnnotator {

    /** Mots-clés qui ouvrent une déclaration de type. */
    private static final String[] TYPE_KEYWORDS = {
            "class", "interface", "enum", "record"
    };

    /** Types primitifs : ils précèdent une méthode comme un objet le fait. */
    private static final String[] PRIMITIVE_TYPES = {
            "int", "long", "short", "byte", "char", "float", "double",
            "boolean", "void"
    };

    @Override
    public List<SemanticAnnotation> annotateLine(int line, int depthIn,
            SyntaxDocument.LineTokens entry, CharSequence text, int lineStart) {
        List<SemanticAnnotation> notes = new ArrayList<>();
        SyntaxDocument.Token[] tokens = entry.tokens();
        // l'état de la marche : le texte du jeton précédent, et s'il
        // était neutre (un identifiant — un type d'objet, par exemple)
        String previous = "";
        boolean previousNeutral = false;
        for (int at = 0; at < tokens.length; at++) {
            SyntaxDocument.Token token = tokens[at];
            // les blancs (portée nulle, texte vide) ne comptent pas dans
            // l'adjacence : « class Demo » reste adjacent
            if (isBlank(token, text)) {
                continue;
            }
            String word = identifier(token, text);
            if (word == null) {
                previous = slice(token, text);
                previousNeutral = false;
                continue;
            }
            String next = nextSignificant(tokens, at + 1, text);
            if (isOneOf(previous, TYPE_KEYWORDS)) {
                notes.add(new SemanticAnnotation(
                        lineStart + token.start(), lineStart + token.end(),
                        "type", SemanticAnnotation.BOLD));
            } else if ("(".equals(next) && isTypeIsh(previous, previousNeutral)) {
                notes.add(new SemanticAnnotation(
                        lineStart + token.start(), lineStart + token.end(),
                        "méthode", 0));
            } else if (depthIn == 1 && (";".equals(next) || "=".equals(next))
                    && isTypeIsh(previous, previousNeutral)) {
                notes.add(new SemanticAnnotation(
                        lineStart + token.start(), lineStart + token.end(),
                        "champ", 0));
            }
            previous = word;
            previousNeutral = true;
        }
        return notes;
    }

    /**
     * Le mot d'un jeton identifiant : neutre de L1 (portée nulle — un
     * mot-clé ne l'est pas) et de forme d'identifiant.
     */
    private static String identifier(SyntaxDocument.Token token, CharSequence text) {
        if (token.scope() != null || token.end() - token.start() < 1) {
            return null;
        }
        String word = slice(token, text);
        if (!word.isEmpty() && !Character.isJavaIdentifierStart(word.charAt(0))) {
            return null;
        }
        for (int i = 1; i < word.length(); i++) {
            if (!Character.isJavaIdentifierPart(word.charAt(i))) {
                return null;
            }
        }
        return word;
    }

    /** Le texte d'un jeton. */
    private static String slice(SyntaxDocument.Token token, CharSequence text) {
        return text.subSequence(token.start(), token.end()).toString();
    }

    /** Un jeton blanc (les lexeurs émettent les espaces, portée nulle). */
    private static boolean isBlank(SyntaxDocument.Token token, CharSequence text) {
        return token.scope() == null && slice(token, text).isBlank();
    }

    /** Le texte du premier jeton non blanc à partir de l'indice donné. */
    private static String nextSignificant(SyntaxDocument.Token[] tokens,
            int from, CharSequence text) {
        for (int at = from; at < tokens.length; at++) {
            if (!isBlank(tokens[at], text)) {
                return slice(tokens[at], text);
            }
        }
        return "";
    }

    /** Le mot appartient-il au petit vocabulaire donné. */
    private static boolean isOneOf(String word, String[] vocabulary) {
        for (String candidate : vocabulary) {
            if (candidate.equals(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Le jeton précédent fait-il office de type : un identifiant
     * (l'objet de {@code String nom(}) ou un primitif.
     */
    private static boolean isTypeIsh(String previous, boolean previousNeutral) {
        return previousNeutral || isOneOf(previous, PRIMITIVE_TYPES);
    }
}
