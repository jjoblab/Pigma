package jo.pigma.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jo.pigma.core.TextBuffer;
import jo.pigma.syntax.StructureDocument;
import jo.pigma.syntax.SyntaxDocument;

/**
 * L'index de symboles et de mots : les déclarations (les natures de
 * l'annotateur) et les identifiants du préfixe lexé, contés — la
 * complétion de base s'y sert (les déclarations d'abord, puis les
 * mots par fréquence). La passe se paie à la construction, jamais par
 * trame ; l'hôte la reconstruit quand son contenu l'exige (la
 * révision du tampon dit quand).
 *
 * <p>Un serveur de langage branchera son propre index — le modèle
 * (nom, nature, position) est déjà le sien.</p>
 */
public final class SymbolIndex {

    /**
     * Un symbole : un nom, sa nature (chaîne ouverte — « type »,
     * « méthode », « champ », ou « mot » pour un simple identifiant),
     * sa position.
     *
     * @param name   le nom, jamais vide
     * @param nature la nature, jamais {@code null}
     * @param offset la position absolue du début
     * @param line   l'indice de la ligne
     */
    public record Symbol(String name, String nature, int offset, int line) {
    }

    private final List<Symbol> symbols;

    private final Map<String, int[]> words;

    private SymbolIndex(List<Symbol> symbols, Map<String, int[]> words) {
        this.symbols = symbols;
        this.words = words;
    }

    /**
     * Construit l'index du document entier (force le lexage des lignes
     * parcourues, comme le dessin — l'ouverture a déjà rempli le fond,
     * la passe ne paie que les lignes restantes).
     *
     * @param buffer    le tampon (positions absolues)
     * @param syntax    le moteur (jetons de L1)
     * @param structure l'analyseur (profondeur de L2 : la règle « champ »
     *                  en dépend)
     * @param annotator le producteur (les déclarations comptent double)
     * @return l'index, jamais {@code null}
     */
    public static SymbolIndex of(TextBuffer buffer, SyntaxDocument syntax,
            StructureDocument structure, SemanticAnnotator annotator) {
        Builder builder = new Builder(buffer, syntax, structure, annotator);
        builder.addLines(0, syntax.lineCount());
        return builder.build();
    }

    /**
     * Le bâtisseur par tranches : la vue nourrit l'analyse ligne par
     * ligne, postée entre les trames — la première construction d'un
     * gros fichier ne peut plus forcer le lexage entier dans une seule
     * trame (le relevé 8 l'a mesuré : 108 ms). Chaque ligne porte sa
     * profondeur depuis les points de contrôle de L2 — les tranches se
     * suivent sans état à transporter.
     */
    public static final class Builder {

        private final TextBuffer buffer;
        private final SyntaxDocument syntax;
        private final StructureDocument structure;
        private final SemanticAnnotator annotator;
        private final List<Symbol> symbols = new ArrayList<>();
        private final Map<String, int[]> words = new HashMap<>();

        /**
         * Le bâtisseur d'un document.
         *
         * @param buffer    le tampon (positions absolues)
         * @param syntax    le moteur (jetons de L1)
         * @param structure l'analyseur (points de contrôle de L2)
         * @param annotator le producteur (les déclarations comptent double)
         */
        public Builder(TextBuffer buffer, SyntaxDocument syntax,
                StructureDocument structure, SemanticAnnotator annotator) {
            this.buffer = Objects.requireNonNull(buffer);
            this.syntax = Objects.requireNonNull(syntax);
            this.structure = Objects.requireNonNull(structure);
            this.annotator = Objects.requireNonNull(annotator);
        }

        /**
         * Avale une tranche de lignes (les tranches se suivent, les
         * bornes hors document se bornent).
         *
         * @param from        première ligne, incluse
         * @param toExclusive première ligne exclue
         */
        public void addLines(int from, int toExclusive) {
            for (int line = Math.max(0, from); line < toExclusive
                    && line < syntax.lineCount(); line++) {
                SyntaxDocument.LineTokens entry = syntax.lineTokens(line);
                int lineStart = buffer.getLineStart(line);
                CharSequence text = buffer.getText(lineStart, buffer.getLineEnd(line));
                int depthIn = line == 0 ? 0 : structure.structureOf(line - 1).depthOut();
                for (SemanticAnnotation note : annotator.annotateLine(line, depthIn,
                        entry, text, lineStart)) {
                    String name = text.subSequence(
                            note.start() - lineStart, note.end() - lineStart).toString();
                    symbols.add(new Symbol(name, note.nature(), note.start(), line));
                    bump(words, name, note.nature());
                }
                for (SyntaxDocument.Token token : entry.tokens()) {
                    if (token.scope() != null || token.end() - token.start() < 2) {
                        continue;
                    }
                    String word = text.subSequence(token.start(), token.end()).toString();
                    if (!isWord(word)) {
                        continue;
                    }
                    bump(words, word, "mot");
                }
            }
        }

        /**
         * L'index construit des tranches avalées.
         *
         * @return l'index, jamais {@code null}
         */
        public SymbolIndex build() {
            return new SymbolIndex(new ArrayList<>(symbols), new HashMap<>(words));
        }
    }

    /**
     * Les déclarations du préfixe, en ordre de document.
     *
     * @return les symboles, jamais {@code null}
     */
    public List<Symbol> symbols() {
        return symbols;
    }

    /**
     * Les candidats de complétion d'un préfixe : les déclarations qui
     * le portent d'abord, puis les mots par fréquence décroissante.
     *
     * @param prefix le préfixe, vide admis (alors les plus fréquents)
     * @param limit  le nombre maximal de candidats
     * @return les noms, jamais {@code null}
     */
    public List<String> query(String prefix, int limit) {
        List<String> candidates = new ArrayList<>();
        for (Symbol symbol : symbols) {
            if (candidates.size() >= limit) {
                break;
            }
            if (symbol.name().startsWith(prefix) && !candidates.contains(symbol.name())) {
                candidates.add(symbol.name());
            }
        }
        List<Map.Entry<String, int[]>> ranked = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : words.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                ranked.add(entry);
            }
        }
        ranked.sort((a, b) -> {
            int byCount = Integer.compare(b.getValue()[1], a.getValue()[1]);
            return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
        });
        for (Map.Entry<String, int[]> entry : ranked) {
            if (candidates.size() >= limit) {
                break;
            }
            if (!candidates.contains(entry.getKey())) {
                candidates.add(entry.getKey());
            }
        }
        return candidates;
    }

    /** La fréquence d'un mot (l'index des vues du panneau s'en sert). */
    public int wordCount() {
        return words.size();
    }

    private static void bump(Map<String, int[]> words, String word, String nature) {
        int[] cell = words.computeIfAbsent(word, key -> new int[2]);
        cell[0] = "mot".equals(nature) ? cell[0] : cell[0] + 1;
        cell[1]++;
    }

    private static boolean isWord(String word) {
        if (!Character.isJavaIdentifierStart(word.charAt(0))) {
            return false;
        }
        for (int i = 1; i < word.length(); i++) {
            if (!Character.isJavaIdentifierPart(word.charAt(i))) {
                return false;
            }
        }
        return true;
    }

}
