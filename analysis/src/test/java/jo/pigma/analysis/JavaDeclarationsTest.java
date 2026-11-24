package jo.pigma.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import jo.pigma.core.TextBuffer;
import jo.pigma.syntax.Languages;
import jo.pigma.syntax.StructureDocument;
import jo.pigma.syntax.SyntaxDocument;

/**
 * Le producteur de démonstration et son document : les formes Java se
 * désignent (type, méthode, champ), les appels qualifiés et les
 * locaux ne se désignent pas — la profondeur de L2 fait la part.
 */
public class JavaDeclarationsTest {

    private static final String DEMO = String.join("\n",
            "public final class Demo {",
            "    int count = 0;",
            "",
            "    public double weight(int seed) {",
            "        long local = 42L;",
            "        System.out.printf(\"x\");",
            "        return seed * local;",
            "    }",
            "}",
            "");

    @Test
    public void declarationsAreAnnotatedByForm() {
        TextBuffer buffer = TextBuffer.create(DEMO);
        SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        StructureDocument structure = StructureDocument.open(buffer, syntax);
        SemanticDocument semantic = SemanticDocument.open(
                buffer, syntax, structure, new JavaDeclarations());
        // ligne 0 : « Demo » derrière « class » — un type, en gras
        List<SemanticAnnotation> types = semantic.annotationsAt(0);
        assertEquals(1, types.size());
        assertNote(buffer, types.get(0), "Demo", "type", SemanticAnnotation.BOLD);
        // ligne 1 : « count » en corps de classe — un champ
        List<SemanticAnnotation> fields = semantic.annotationsAt(1);
        assertEquals(1, fields.size());
        assertNote(buffer, fields.get(0), "count", "champ", 0);
        // ligne 3 : « weight » précédé d'un type et suivi d'une
        // parenthèse — une méthode ; le paramètre ne se désigne pas
        List<SemanticAnnotation> methods = semantic.annotationsAt(3);
        assertEquals(1, methods.size());
        assertNote(buffer, methods.get(0), "weight", "méthode", 0);
        assertNull("le paramètre ne s'annote pas",
                semantic.covering(3, columnOf(buffer, 3, "seed"),
                        columnOf(buffer, 3, "seed") + "seed".length()));
        // lignes 4 à 6 : un local (profondeur 2), un appel qualifié, un
        // retour — rien ne se désigne
        for (int line = 4; line <= 6; line++) {
            assertTrue("la ligne " + line + " ne porte rien",
                    semantic.annotationsAt(line).isEmpty());
        }
        // la fusion s'enquiert par jeton : le mot-clé n'est couvert
        // par aucune annotation
        assertNull(semantic.covering(3, 0, 6));
        structure.detach();
        syntax.detach();
    }

    @Test
    public void coveringAnswersTheFusionQuestion() {
        TextBuffer buffer = TextBuffer.create(DEMO);
        SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        StructureDocument structure = StructureDocument.open(buffer, syntax);
        SemanticDocument semantic = SemanticDocument.open(
                buffer, syntax, structure, new JavaDeclarations());
        int column = columnOf(buffer, 3, "weight");
        SemanticAnnotation note = semantic.covering(3, column, column + 6);
        assertEquals("méthode", note.nature());
        assertEquals(column, note.start() - buffer.getLineStart(3));
        structure.detach();
        syntax.detach();
    }

    private static void assertNote(TextBuffer buffer, SemanticAnnotation note,
            String word, String nature, int modifiers) {
        assertEquals(nature, note.nature());
        assertEquals(modifiers, note.modifiers());
        assertEquals(word, buffer.getText(note.start(), note.end()).toString());
        assertEquals(word.length(), note.end() - note.start());
    }

    private static int columnOf(TextBuffer buffer, int line, String word) {
        CharSequence text = buffer.getText(
                buffer.getLineStart(line), buffer.getLineEnd(line));
        return text.toString().indexOf(word);
    }
}
