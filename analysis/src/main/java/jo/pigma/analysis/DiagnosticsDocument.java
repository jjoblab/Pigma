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
        Builder builder = new Builder(buffer, syntax, structure);
        builder.addLines(0, syntax.lineCount());
        return builder.build();
    }

    /**
     * Le bâtisseur par tranches : la pile des ouvrants voyage entre
     * les tranches, la profondeur relit les points de contrôle de L2 à
     * chaque ligne — la vue nourrit la passe postée entre les trames,
     * la première construction ne force plus le document dans une
     * seule trame.
     */
    public static final class Builder {

        private final TextBuffer buffer;
        private final SyntaxDocument syntax;
        private final StructureDocument structure;
        private final List<Diagnostic> found = new ArrayList<>();
        // la pile des ouvrants en cours (ligne, colonne) : ce qui
        // reste dessus à la fin n'est pas refermé
        private final List<int[]> openers = new ArrayList<>();
        private int nextLine;

        /**
         * Le bâtisseur d'un document.
         *
         * @param buffer    le tampon (positions absolues)
         * @param syntax    le moteur (jetons de L1)
         * @param structure l'analyseur (points de contrôle de L2)
         */
        public Builder(TextBuffer buffer, SyntaxDocument syntax,
                StructureDocument structure) {
            this.buffer = Objects.requireNonNull(buffer);
            this.syntax = Objects.requireNonNull(syntax);
            this.structure = Objects.requireNonNull(structure);
        }

        /**
         * Avale une tranche de lignes (les tranches se suivent).
         *
         * @param from        première ligne, incluse
         * @param toExclusive première ligne exclue
         */
        public void addLines(int from, int toExclusive) {
            for (int line = Math.max(nextLine, from); line < toExclusive
                    && line < syntax.lineCount(); line++) {
                int depth = line == 0 ? 0 : structure.structureOf(line - 1).depthOut();
                SyntaxDocument.LineTokens entry = syntax.lineTokens(line);
                int lineStart = buffer.getLineStart(line);
                CharSequence text = buffer.getText(lineStart, buffer.getLineEnd(line));
                for (int token = 0; token < entry.tokenCount(); token++) {
                    char c = singleSign(entry, token, text);
                    if (c == 0) {
                        continue;
                    }
                    int at = lineStart + entry.startAt(token);
                    if (c == '(' || c == '{' || c == '[') {
                        openers.add(new int[] {line, entry.startAt(token)});
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
                nextLine = line + 1;
            }
        }

        /**
         * Les diagnostics construits des tranches avalées. La pile
         * restante ne se rapporte que si TOUT le document est parcouru
         * : sur un préfixe, la queue ouvrirait peut-être encore — un
         * ouvrant non refermé sur une passe partielle serait un faux
         * diagnostic.
         *
         * @return les diagnostics, jamais {@code null}
         */
        public DiagnosticsDocument build() {
            List<Diagnostic> all = new ArrayList<>(found);
            if (nextLine >= syntax.lineCount() && structure.mismatchCount() > 0) {
                for (int[] opener : openers) {
                    int at = buffer.getLineStart(opener[0]) + opener[1];
                    all.add(new Diagnostic(at, at + 1,
                            "ouvrant non fermé : la pile ne se referme pas"));
                }
            }
            return new DiagnosticsDocument(buffer, all);
        }
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

    private static char singleSign(SyntaxDocument.LineTokens entry, int token,
            CharSequence text) {
        if (entry.endAt(token) - entry.startAt(token) != 1) {
            return 0;
        }
        char c = text.charAt(entry.startAt(token));
        return c == '(' || c == ')' || c == '{' || c == '}' || c == '[' || c == ']'
                ? c : 0;
    }
}
