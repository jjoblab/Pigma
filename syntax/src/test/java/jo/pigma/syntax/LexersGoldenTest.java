package jo.pigma.syntax;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import jo.pigma.syntax.lexer.Lexer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Sortie figée des lexeurs générés : le lexage ligne par ligne (une
 * fenêtre par ligne, l'état de fin enchaîné) des fichiers de référence
 * doit reproduire exactement le fichier doré. Le doré absent s'écrit
 * au premier passage — il se relit puis se committe ; toute retouche
 * d'une description casse ce test, c'est le filet de sécurité du
 * moteur.
 *
 * <p>Chaque ligne du doré : un jeton (« type &lt;id&gt; &lt;portée&gt;
 * '&lt;texte échappé&gt;' », la portée {@code -} marquant le neutre) ou
 * l'état de fin de la ligne (« @ état &lt;valeur&gt; »). Les identifiants
 * de types se lisent dans les descriptions de lexeurs du module.</p>
 */
public class LexersGoldenTest {

    /**
     * Le doré Java se reproduit à l'identique.
     *
     * @throws IOException si un fichier ne se lit pas
     */
    @Test
    public void javaGoldenMatches() throws IOException {
        checkGolden(Languages.JAVA, "java-reference.java", "java-golden.txt");
    }

    /**
     * Le doré XML se reproduit à l'identique.
     *
     * @throws IOException si un fichier ne se lit pas
     */
    @Test
    public void xmlGoldenMatches() throws IOException {
        checkGolden(Languages.XML, "xml-reference.xml", "xml-golden.txt");
    }

    /**
     * Le doré JSON se reproduit à l'identique.
     *
     * @throws IOException si un fichier ne se lit pas
     */
    @Test
    public void jsonGoldenMatches() throws IOException {
        checkGolden(Languages.JSON, "json-reference.json", "json-golden.txt");
    }

    private void checkGolden(LanguageSyntax language, String referenceName,
            String goldenName) throws IOException {
        String text = Files.readString(resource("data/" + referenceName),
                StandardCharsets.UTF_8);
        List<String> actual = lexLineByLine(language, text);
        Path sourceGolden = Paths.get("src", "test", "resources", "data", goldenName)
                .toAbsolutePath().normalize();
        var goldenUrl = getClass().getClassLoader().getResource("data/" + goldenName);
        if (goldenUrl == null) {
            Files.write(sourceGolden, actual, StandardCharsets.UTF_8);
            System.out.println("dore genere (a relire puis a committer) : "
                    + sourceGolden);
            return;
        }
        List<String> expected = Files.readAllLines(resource("data/" + goldenName),
                StandardCharsets.UTF_8);
        assertEquals("le doré " + goldenName + " a dérivé", expected, actual);
    }

    /**
     * Le lexage ligne par ligne : chaque ligne est une fenêtre, l'état
     * de fin enchaîne, les jetons portent leur type, leur portée et
     * leur texte échappé.
     */
    static List<String> lexLineByLine(LanguageSyntax language, CharSequence text) {
        Lexer lexer = language.newLexer();
        List<String> output = new ArrayList<>();
        int at = 0;
        int state = 0;
        while (at <= text.length()) {
            int endOfLine = indexOf(text, '\n', at);
            int end = endOfLine < 0 ? text.length() : endOfLine;
            lexer.start(text, at, end, state);
            lexer.advance();
            while (lexer.tokenType() != Lexer.NO_TOKEN) {
                Scope scope = language.scopeOf(lexer.tokenType());
                output.add("type " + lexer.tokenType() + " "
                        + (scope == null ? "-" : scope.name()) + " '"
                        + escape(text.subSequence(lexer.tokenStart(),
                                lexer.tokenEnd()).toString()) + "'");
                lexer.advance();
            }
            output.add("@ etat " + lexer.state());
            state = lexer.state();
            if (endOfLine < 0) {
                break;
            }
            at = endOfLine + 1;
        }
        return output;
    }

    static Path resource(String name) throws IOException {
        var url = LexersGoldenTest.class.getClassLoader().getResource(name);
        assertNotNull("ressource manquante : " + name, url);
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IOException("chemin de ressource invalide : " + name, e);
        }
    }

    static String escape(String raw) {
        return raw.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }

    private static int indexOf(CharSequence text, char candidate, int from) {
        for (int i = from; i < text.length(); i++) {
            if (text.charAt(i) == candidate) {
                return i;
            }
        }
        return -1;
    }
}
