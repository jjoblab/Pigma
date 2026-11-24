package jo.pigma.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jo.pigma.core.TextBuffer;
import jo.pigma.syntax.SyntaxDocument;
import jo.pigma.syntax.StructureDocument;

/**
 * Les diagnostics d'appariement de L2, localisés : chaque fermant
 * orphelin (la profondeur tolérante aurait passé sous zéro) et chaque
 * ouvrant non fermé (resté sur la pile à la fin de la passe). La
 * passe suit les points de contrôle de profondeur de L2 — jamais un
 * recalcul depuis le début — et se limite au préfixe analysé : le
 * diagnostic d'un ouvrant non fermé n'arrive qu'une fois sa zone
 * prouvée.
 */
public final class DiagnosticsDocument {

    /**
     * Un diagnostic : une plage et un message.
     *
     * @param start   début absolu de la plage
     * @param end     fin absolue de la plage
     * @param message le message, jamais {@code null}
     */
    public record Diagnostic(int start, int end, String message) {
    }

    private final TextBuffer buffer;

    private final List<Diagnostic> diagnostics;

    private DiagnosticsDocument(TextBuffer buffer, List<Diagnostic> diagnostics) {
        this.buffer = buffer;
        this.diagnostics = diagnostics;
    }

    /**
     * La passe des diagnostics du document entier (force l'analyse
     * des lignes parcourues, comme le dessin).
     *
     * @param buffer    le tampon (positions absolues)
     * @param syntax    le moteur (jetons de L1)
     * @param structure l'analyseur (points de contrôle de L2)
     * @return les diagnostics, jamais {@code null}
     */
    public static DiagnosticsDocument of(TextBuffer buffer, SyntaxDocument syntax,
            StructureDocument structure) {
        Objects.requireNonNull(buffer);
        Objects.requireNonNull(syntax);
        Objects.requireNonNull(structure);
        List<Diagnostic> found = new ArrayList<>();
        // la pile des ouvrants en cours (ligne, colonne) : ce qui
        // reste dessus à la fin n'est pas refermé
        List<int[]> openers = new ArrayList<>();
        int count = syntax.lineCount();
        for (int line = 0; line < count; line++) {
            int depth = line == 0 ? 0 : structure.structureOf(line - 1).depthOut();
            SyntaxDocument.Token[] tokens = syntax.lineTokens(line).tokens();
            int lineStart = buffer.getLineStart(line);
            CharSequence text = buffer.getText(lineStart, buffer.getLineEnd(line));
            for (SyntaxDocument.Token token : tokens) {
                char c = singleSign(token, text);
                if (c == 0) {
                    continue;
                }
                int at = lineStart + token.start();
                if (c == '(' || c == '{' || c == '[') {
                    openers.add(new int[] {line, token.start()});
                    depth++;
                } else if (depth == 0) {
                    // la profondeur tolérante refuse de passer sous
                    // zéro : ce fermant n'a pas d'ouvrant
                    found.add(new Diagnostic(at, at + 1,
                            "fermant orphelin : « " + c + " » sans ouvrant"));
                } else {
                    depth--;
                    if (!openers.isEmpty()) {
                        openers.remove(openers.size() - 1);
                    }
                }
            }
        }
        if (structure.mismatchCount() > 0) {
            for (int[] opener : openers) {
                int at = buffer.getLineStart(opener[0]) + opener[1];
                found.add(new Diagnostic(at, at + 1,
                        "ouvrant non fermé : la pile ne se referme pas"));
            }
        }
        return new DiagnosticsDocument(buffer, found);
    }

    /**
     * Les diagnostics, en ordre de document.
     *
     * @return la liste, jamais {@code null}
     */
    public List<Diagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Les diagnostics d'une plage de lignes (le dessin souligne la
     * fenêtre visible, pas le document).
     *
     * @param fromLine première ligne, incluse
     * @param toLine   dernière ligne, incluse
     * @return les diagnostics de la plage, jamais {@code null}
     */
    public List<Diagnostic> betweenLines(int fromLine, int toLine) {
        List<Diagnostic> inRange = new ArrayList<>();
        for (Diagnostic diagnostic : diagnostics) {
            int line = lineOf(diagnostic);
            if (line >= fromLine && line <= toLine) {
                inRange.add(diagnostic);
            }
        }
        return inRange;
    }

    private int lineOf(Diagnostic diagnostic) {
        return buffer.lineOfOffset(diagnostic.start());
    }

    private static char singleSign(SyntaxDocument.Token token, CharSequence text) {
        if (token.end() - token.start() != 1) {
            return 0;
        }
        char c = text.charAt(token.start());
        return c == '(' || c == ')' || c == '{' || c == '}' || c == '[' || c == ']'
                ? c : 0;
    }
}
