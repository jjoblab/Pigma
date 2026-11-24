package jo.pigma.syntax;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import jo.pigma.syntax.lexer.Lexer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Redémarrage des lexeurs générés : relexer depuis un début de ligne
 * en état initial (seul état redémarrable) enchaîne exactement les
 * mêmes jetons et les mêmes états que le lexage depuis le début. C'est
 * la garantie que la règle d'arrêt du moteur ne relit que ce qu'il
 * faut. La couverture vérifie en sus que les jetons tapissent chaque
 * ligne sans trou.
 */
public class LexersRestartTest {

    private static final int MAX_CHECKPOINTS = 120;

    /**
     * Redémarrage et couverture du lexeur Java sur les deux corpus.
     *
     * @throws IOException si un corpus ne se lit pas
     */
    @Test
    public void javaRestartsFromInitialLines() throws IOException {
        checkRestart(Languages.JAVA, "data/java-2000.java");
        checkRestart(Languages.JAVA, "data/java-reference.java");
    }

    /**
     * Redémarrage et couverture du lexeur XML sur les deux corpus.
     *
     * @throws IOException si un corpus ne se lit pas
     */
    @Test
    public void xmlRestartsFromInitialLines() throws IOException {
        checkRestart(Languages.XML, "data/xml-2000.xml");
        checkRestart(Languages.XML, "data/xml-reference.xml");
    }

    /**
     * Redémarrage et couverture du lexeur JSON sur les deux corpus.
     *
     * @throws IOException si un corpus ne se lit pas
     */
    @Test
    public void jsonRestartsFromInitialLines() throws IOException {
        checkRestart(Languages.JSON, "data/json-2000.json");
        checkRestart(Languages.JSON, "data/json-reference.json");
    }

    private void checkRestart(LanguageSyntax language, String corpusName)
            throws IOException {
        String text = Files.readString(LexersGoldenTest.resource(corpusName),
                StandardCharsets.UTF_8);
        List<LineResult> full = lexAllLines(language, text);
        for (LineResult line : full) {
            assertEquals("trou dans la couverture ligne " + line.index,
                    line.length, line.coverage);
        }
        List<Integer> checkpoints = new ArrayList<>();
        for (LineResult line : full) {
            if (line.stateBefore == 0) {
                checkpoints.add(line.index);
            }
        }
        assertTrue("aucun point de contrôle en état initial", !checkpoints.isEmpty());
        int step = Math.max(1, checkpoints.size() / MAX_CHECKPOINTS);
        int checked = 0;
        for (int pick = 0; pick < checkpoints.size(); pick += step) {
            int from = checkpoints.get(pick);
            List<LineResult> tail = lexAllLines(language, text, from);
            for (int j = 0; j < tail.size(); j++) {
                LineResult reference = full.get(from + j);
                LineResult fresh = tail.get(j);
                assertEquals("jetons différents ligne " + (from + j),
                        reference.describeTokens(), fresh.describeTokens());
                assertEquals("état différent ligne " + (from + j),
                        reference.stateAfter, fresh.stateAfter);
            }
            checked++;
        }
        assertTrue("points de contrôle trop peu nombreux : " + checked, checked >= 2);
    }

    private static List<LineResult> lexAllLines(LanguageSyntax language,
            CharSequence text) {
        return lexAllLines(language, text, 0);
    }

    private static List<LineResult> lexAllLines(LanguageSyntax language,
            CharSequence text, int fromLine) {
        Lexer lexer = language.newLexer();
        List<LineResult> results = new ArrayList<>();
        List<int[]> lineBounds = lineBounds(text);
        int state = 0;
        for (int i = fromLine; i < lineBounds.size(); i++) {
            int[] bounds = lineBounds.get(i);
            LineResult result = new LineResult(i, bounds[0], bounds[1] - bounds[0],
                    state);
            lexer.start(text, bounds[0], bounds[1], state);
            lexer.advance();
            int coverage = 0;
            while (lexer.tokenType() != Lexer.NO_TOKEN) {
                result.tokens.add(lexer.tokenType() + "@"
                        + (lexer.tokenEnd() - lexer.tokenStart()));
                coverage += lexer.tokenEnd() - lexer.tokenStart();
                lexer.advance();
            }
            result.coverage = coverage;
            result.stateAfter = lexer.state();
            state = lexer.state();
            results.add(result);
        }
        return results;
    }

    private static List<int[]> lineBounds(CharSequence text) {
        List<int[]> bounds = new ArrayList<>();
        int at = 0;
        while (at <= text.length()) {
            int endOfLine = at;
            while (endOfLine < text.length() && text.charAt(endOfLine) != '\n') {
                endOfLine++;
            }
            bounds.add(new int[] {at, endOfLine});
            if (endOfLine >= text.length()) {
                break;
            }
            at = endOfLine + 1;
        }
        return bounds;
    }

    /** Une ligne lexée : jetons, couverture, états d'entrée et de sortie. */
    private static final class LineResult {
        final int index;
        final int start;
        final int length;
        final int stateBefore;
        final List<String> tokens = new ArrayList<>();
        int coverage;
        int stateAfter;

        LineResult(int index, int start, int length, int stateBefore) {
            this.index = index;
            this.start = start;
            this.length = length;
            this.stateBefore = stateBefore;
        }

        String describeTokens() {
            return String.join(",", tokens);
        }
    }
}
