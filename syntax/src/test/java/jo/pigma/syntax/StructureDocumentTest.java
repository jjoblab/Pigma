package jo.pigma.syntax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jo.pigma.core.TextBuffer;
import jo.pigma.core.TextTransaction;
import jo.pigma.core.UndoStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import org.junit.Test;

/**
 * L2, l'analyseur tolérant : la structure d'appariement par-dessus
 * les jetons — paresse (préfixe à la demande), tolérance (fermant
 * orphelin compté, profondeur jamais négative), incrément (la marche
 * d'édition réanalyse la zone puis épargne la queue dès que la
 * profondeur rejoint).
 */
public class StructureDocumentTest {

    private static final String[] REPLACEMENTS = {
        "x", "", "\n", "/*", "*/", "{", "}", "(", ")", "[", "]", "\n\n"
    };

    /**
     * Le stress du lexeur, transposé : des éditions aléatoires (dont
     * des ouvrants et fermants isolés qui décalent la profondeur),
     * des demandes au fil de l'eau, puis la comparaison exhaustive
     * avec une structure neuve sur le texte final — profondeur et
     * rompus, ligne par ligne.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void randomEditsMatchFreshAnalysis() throws IOException {
        for (int seed = 1; seed <= 20; seed++) {
            TextBuffer buffer = TextBuffer.create(corpus());
            UndoStack undo = new UndoStack(buffer);
            SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
            StructureDocument structure = StructureDocument.open(buffer, syntax);
            Random random = new Random(20261125L + seed);
            for (int round = 0; round < 24; round++) {
                int line = random.nextInt(200);
                while (buffer.getLineEnd(line) - buffer.getLineStart(line) < 4) {
                    line = random.nextInt(200);
                }
                int at = buffer.getLineStart(line) + 1;
                try (TextTransaction transaction = undo.newTransaction()) {
                    transaction.replace(at, at + 1,
                            REPLACEMENTS[random.nextInt(REPLACEMENTS.length)]);
                }
                if (round % 3 == 0) {
                    structure.structureOf(100 + random.nextInt(300));
                }
            }
            TextBuffer reference = TextBuffer.create(buffer.getText(0, buffer.length()));
            SyntaxDocument freshSyntax = SyntaxDocument.open(reference, Languages.JAVA);
            StructureDocument fresh = StructureDocument.open(reference, freshSyntax);
            fresh.structureOf(reference.getLineCount() - 1);
            structure.structureOf(buffer.getLineCount() - 1);
            assertEquals("nombre de lignes, graine " + seed,
                    reference.getLineCount(), buffer.getLineCount());
            for (int line = 0; line < reference.getLineCount(); line++) {
                StructureDocument.LineStructure expected = fresh.structureOf(line);
                StructureDocument.LineStructure actual = structure.structureOf(line);
                assertEquals("profondeur de la ligne " + line + ", graine " + seed,
                        expected.depthOut(), actual.depthOut());
                assertEquals("rompus de la ligne " + line + ", graine " + seed,
                        expected.mismatches(), actual.mismatches());
            }
            assertEquals("rompus du préfixe, graine " + seed,
                    fresh.mismatchCount(), structure.mismatchCount());
            structure.detach();
            fresh.detach();
            syntax.detach();
            freshSyntax.detach();
        }
    }

    /**
     * La paresse : demander la ligne 10 n'analyse que le préfixe
     * jusqu'à elle — le reste du document reste à la demande.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void analysisIsLazyUpToTheDemand() throws IOException {
        TextBuffer buffer = TextBuffer.create(corpus());
        SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        StructureDocument structure = StructureDocument.open(buffer, syntax);
        structure.structureOf(10);
        assertEquals(11, structure.analyzedLineCount());
        assertTrue(structure.analyzedLineCount() < buffer.getLineCount());
        structure.detach();
        syntax.detach();
    }

    /**
     * La tolérance : un fermant orphelin se compte, la profondeur
     * reste à zéro — jamais négative ; un ouvrant en fin de document
     * est un document ouvert, pas un rompu.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void unbalancedCloserCountsAndClamps() throws IOException {
        TextBuffer buffer = TextBuffer.create(")\n)\n{");
        SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        StructureDocument structure = StructureDocument.open(buffer, syntax);
        assertEquals(0, structure.structureOf(0).depthOut());
        assertEquals(1, structure.structureOf(0).mismatches());
        assertEquals(0, structure.structureOf(1).depthOut());
        assertEquals(1, structure.structureOf(1).mismatches());
        assertEquals(1, structure.structureOf(2).depthOut());
        assertEquals(0, structure.structureOf(2).mismatches());
        assertEquals(2, structure.mismatchCount());
        structure.detach();
        syntax.detach();
    }

    /**
     * La réinsertion épargne la queue : une édition neutre en
     * profondeur ne réanalyse que sa zone (une ligne) ; une édition
     * qui ouvre propage, la marche va au bout.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void rejoinSparesTheTail() throws IOException {
        StringBuilder balanced = new StringBuilder("class B {\n");
        for (int i = 0; i < 300; i++) {
            balanced.append("x = (1 + 2);\n");
        }
        balanced.append("}\n");
        TextBuffer buffer = TextBuffer.create(balanced.toString());
        UndoStack undo = new UndoStack(buffer);
        SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        StructureDocument structure = StructureDocument.open(buffer, syntax);
        structure.structureOf(buffer.getLineCount() - 1);
        long before = structure.analyses();
        int at = buffer.getLineStart(50) + 1;
        // neutre : un couple s'inscrit, la profondeur de sortie rejoint
        // — la ligne éditée relue par le lexeur, la suivante dont les
        // jetons (épargnés) et la profondeur rejoignent : deux lignes
        // analysées, la queue est épargnée
        try (TextTransaction transaction = undo.newTransaction()) {
            transaction.replace(at, at + 1, "()");
        }
        structure.structureOf(buffer.getLineCount() - 1);
        assertEquals("l'édition neutre n'analyse que sa zone et sa jointure",
                before + 2L, structure.analyses());
        before = structure.analyses();
        // une ouverture pure : la profondeur dérive, la marche va au bout
        try (TextTransaction transaction = undo.newTransaction()) {
            transaction.replace(at, at, "{");
        }
        structure.structureOf(buffer.getLineCount() - 1);
        assertTrue("l'édition ouvrante propage jusqu'au bout",
                structure.analyses() - before > 100L);
        // la classe se referme, l'ouvrant égaré de la ligne 50 reste
        // ouvert : la dernière ligne sort à profondeur 1
        assertEquals(1, structure.structureOf(buffer.getLineCount() - 1).depthOut());
        structure.detach();
        syntax.detach();
    }

    /**
     * Les signes d'appariement vivant dans une chaîne ou un
     * commentaire appartiennent à un jeton plus long : ils ne
     * comptent pas.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void bracketsInsideStringsAndCommentsDoNotCount() throws IOException {
        TextBuffer buffer = TextBuffer.create("String s = \"(\"; // {\nint x = 1;");
        SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        StructureDocument structure = StructureDocument.open(buffer, syntax);
        assertEquals(0, structure.structureOf(0).depthOut());
        assertEquals(0, structure.structureOf(0).mismatches());
        assertEquals(0, structure.structureOf(1).depthOut());
        assertEquals(0, structure.structureOf(1).mismatches());
        structure.detach();
        syntax.detach();
    }

    /**
     * Une édition au-delà du préfixe analysé ne le touche pas : la
     * zone est inconnue, le préfixe reste prouvé, la demande reprend
     * après.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void editBeyondTheAnalyzedPrefixKeepsItSparse() throws IOException {
        TextBuffer buffer = TextBuffer.create(corpus());
        UndoStack undo = new UndoStack(buffer);
        SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        StructureDocument structure = StructureDocument.open(buffer, syntax);
        structure.structureOf(4);
        int at = buffer.getLineStart(100) + 1;
        try (TextTransaction transaction = undo.newTransaction()) {
            transaction.replace(at, at + 1, "x");
        }
        assertEquals(5, structure.analyzedLineCount());
        TextBuffer reference = TextBuffer.create(buffer.getText(0, buffer.length()));
        SyntaxDocument freshSyntax = SyntaxDocument.open(reference, Languages.JAVA);
        StructureDocument fresh = StructureDocument.open(reference, freshSyntax);
        fresh.structureOf(reference.getLineCount() - 1);
        structure.structureOf(buffer.getLineCount() - 1);
        for (int line = 0; line < reference.getLineCount(); line++) {
            assertEquals("profondeur de la ligne " + line,
                    fresh.structureOf(line).depthOut(),
                    structure.structureOf(line).depthOut());
        }
        structure.detach();
        fresh.detach();
        syntax.detach();
        freshSyntax.detach();
    }

    private String corpus() throws IOException {
        return Files.readString(Path.of("src/test/resources/data/java-2000.java"));
    }
}
