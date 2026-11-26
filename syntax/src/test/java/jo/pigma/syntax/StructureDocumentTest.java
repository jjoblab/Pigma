package jo.pigma.syntax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

    /**
     * L'appariement parcourt le corpus : chaque signe simple interrogé
     * désigne son complémentaire, et la marche est symétrique —
     * interrogé, le partenaire désigne le signe demandé.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void pairAtWalksTheCorpusSymmetrically() throws IOException {
        TextBuffer buffer = TextBuffer.create(corpus());
        SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        StructureDocument structure = StructureDocument.open(buffer, syntax);
        structure.structureOf(buffer.getLineCount() - 1);
        // le corpus est un Java complet : la chaîne referme à zéro
        assertEquals(0, structure.structureOf(buffer.getLineCount() - 1).depthOut());
        int examined = 0;
        int paired = 0;
        outer:
        for (int line = 0; line < 60; line++) {
            for (SyntaxDocument.Token token : syntax.lineTokens(line).tokens()) {
                if (token.end() - token.start() != 1) {
                    continue;
                }
                char sign = charAt(buffer, line, token.start());
                if (!isSign(sign)) {
                    continue;
                }
                examined++;
                int offset = buffer.getLineStart(line) + token.start();
                StructureDocument.BracketPair pair = structure.pairAt(offset + 1);
                if (pair == null) {
                    continue;
                }
                paired++;
                assertEquals("le partenaire de la ligne " + line + ", colonne "
                        + token.start() + " est le complémentaire",
                        complement(sign), charAt(buffer, pair.toLine(), pair.toColumn()));
                int matchOffset = buffer.getLineStart(pair.toLine()) + pair.toColumn();
                StructureDocument.BracketPair back = structure.pairAt(matchOffset + 1);
                assertEquals("la marche est symétrique : le retour désigne la ligne",
                        line, back.toLine());
                assertEquals("la marche est symétrique : le retour désigne la colonne",
                        token.start(), back.toColumn());
                if (examined >= 200) {
                    break outer;
                }
            }
        }
        assertTrue("le corpus s'apparie presque partout (" + paired + "/"
                + examined + ")", paired > examined * 9 / 10);
        structure.detach();
        syntax.detach();
    }

    /**
     * Le type se vérifie (B1) : « (foo] » n'est pas une paire — la
     * marche ne l'encadre pas, le fermant du mauvais genre ne referme
     * jamais l'ouvrant.
     */
    @Test
    public void pairAtRefusesTheWrongCloserType() {
        TextBuffer buffer = TextBuffer.create("int f() { g(foo]; }\n");
        SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        StructureDocument structure = StructureDocument.open(buffer, syntax);
        // l'ouvrant « ( » à la colonne 10 : le « ] » ne le referme pas
        assertNull("« (foo] » ne s'encadre pas (B1)",
                structure.pairAt(11));
        // le fermant « ] » : sa marche arrière ne trouve pas son « [ »
        assertNull("le fermant orphelin de mauvais genre reste seul",
                structure.pairAt(15));
        // l'accolade du corps, elle, se referme normalement
        assertNotNull("les paires valides demeurent",
                structure.pairAt(9));
        structure.detach();
        syntax.detach();
    }

    /**
     * Les paires près et loin : la même ligne, plusieurs lignes, la
     * préférence du signe à gauche du curseur, et les deux orphelins
     * (fermant compté sans partenaire, ouvrant non fermé).
     */
    @Test
    public void pairAtMatchesNearAndFar() {
        TextBuffer buffer = TextBuffer.create(
                "int f() { return g(1); }\nclass B {\n  int x;\n}\n");
        SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        StructureDocument structure = StructureDocument.open(buffer, syntax);
        // le couple de g( : même ligne, colonnes 18 et 20
        StructureDocument.BracketPair near = structure.pairAt(19);
        assertEquals(0, near.fromLine());
        assertEquals(18, near.fromColumn());
        assertEquals(0, near.toLine());
        assertEquals(20, near.toColumn());
        // la préférence du signe à gauche : entre ( et ) de f(), la
        // marche part de l'ouvrante
        assertEquals(5, structure.pairAt(6).fromColumn());
        // le corps de f : l'accolade ligne 0 colonne 8 referme colonne 23
        StructureDocument.BracketPair body = structure.pairAt(9);
        assertEquals(0, body.fromLine());
        assertEquals(8, body.fromColumn());
        assertEquals(0, body.toLine());
        assertEquals(23, body.toColumn());
        // le corps de la classe B : plusieurs lignes, colonne 8 vers la
        // ligne 3 colonne 0
        StructureDocument.BracketPair far = structure.pairAt(
                buffer.getLineStart(1) + 9);
        assertEquals(1, far.fromLine());
        assertEquals(8, far.fromColumn());
        assertEquals(3, far.toLine());
        assertEquals(0, far.toColumn());
        // et la marche inverse, depuis la fermante
        StructureDocument.BracketPair back = structure.pairAt(
                buffer.getLineStart(3) + 1);
        assertEquals(1, back.toLine());
        assertEquals(8, back.toColumn());
        structure.detach();
        syntax.detach();
    }

    /**
     * Les orphelins n'apparient pas : le fermant sans ouvrant (compté
     * en rompu) et l'ouvrant jamais fermé (document ouvert).
     */
    @Test
    public void pairAtReturnsNullForOrphans() {
        TextBuffer buffer = TextBuffer.create(")\n(");
        SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        StructureDocument structure = StructureDocument.open(buffer, syntax);
        // le fermant orphelin : à droite du curseur posé juste après
        assertNull("fermant orphelin : pas de partenaire", structure.pairAt(1));
        // l'ouvrant jamais fermé : idem, document ouvert
        assertNull("ouvrant non fermé : pas de partenaire", structure.pairAt(3));
        // mais le fermant orphelin reste compté en rompu
        assertEquals(1, structure.mismatchCount());
        structure.detach();
        syntax.detach();
    }

    /**
     * La borne de la marche : une paire au-delà se déclare trop loin
     * ({@code null}) — la marche lit le texte de chaque ligne
     * traversée, la trame ne peut pas la payer sans limite. La même
     * paire rapprochée (la fermante ramenée à portée) s'apparie.
     */
    @Test
    public void pairBeyondWalkBoundIsTooFar() {
        StringBuilder text = new StringBuilder("class Far {\n");
        for (int i = 0; i < 700; i++) {
            text.append("  int field").append(i).append(";\n");
        }
        text.append("}\n");
        TextBuffer buffer = TextBuffer.create(text.toString());
        SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        StructureDocument structure = StructureDocument.open(buffer, syntax);
        // l'accolade ouvrante : sa partenaire vit à ~700 lignes, trop
        // loin pour la marche
        assertNull("paire au-delà de la borne : trop loin",
                structure.pairAt(10));
        // depuis la fermante, l'ouvrant vit tout aussi loin derrière
        int closeOffset = buffer.getLineStart(buffer.getLineCount() - 1) + 1;
        assertNull("paire en arrière au-delà de la borne : trop loin",
                structure.pairAt(closeOffset));
        structure.detach();
        syntax.detach();
    }

    /**
     * La portée du curseur : le bloc le plus interne qui l'englobe —
     * la classe depuis le corps, la méthode depuis le sien, le parent
     * sur la ligne de l'ouvrant AVANT l'accolade, le bloc nouveau
     * APRÈS, et rien au niveau racine.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void scopeAtFindsTheInnermostEnclosingBlock() throws IOException {
        TextBuffer buffer = TextBuffer.create(corpus());
        SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        StructureDocument structure = StructureDocument.open(buffer, syntax);
        // la fermante de la classe : la dernière ligne qui porte un « } »
        int lastLine = buffer.getLineCount() - 1;
        int classClose = lastLine;
        while (buffer.getText(buffer.getLineStart(classClose),
                buffer.getLineEnd(classClose)).toString().isBlank()) {
            classClose--;
        }
        // ligne 8 : « public final class Corpus { » — depuis le corps,
        // la portée est la classe entière
        StructureDocument.BlockRange classScope = structure.scopeAt(
                buffer.getLineStart(9) + 2);
        assertEquals(8, classScope.fromLine());
        assertEquals(classClose, classScope.toLine());
        // ligne 19 : « public double weight0(int seed) { » — depuis le
        // corps, la portée est la méthode ; la ligne de l'ouvrant, APRÈS
        // l'accolade, désigne la méthode, AVANT désigne la classe
        StructureDocument.BlockRange methodScope = structure.scopeAt(
                buffer.getLineStart(21) + 4);
        assertEquals(19, methodScope.fromLine());
        assertEquals(structure.foldEndLine(19), methodScope.toLine());
        assertEquals(19, structure.scopeAt(buffer.getLineEnd(19)).fromLine());
        assertEquals(8, structure.scopeAt(
                buffer.getLineStart(19) + 10).fromLine());
        // dans le javadoc de classe : profondeur nulle, pas de portée
        assertNull(structure.scopeAt(buffer.getLineStart(3) + 2));
        structure.detach();
        syntax.detach();
    }

    /**
     * Les plis : la ligne qui ouvre un bloc est une en-tête, la ligne
     * de fermeture se lit dans le texte, un bloc jamais refermé va
     * jusqu'à la dernière ligne.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void foldsOpenAtHeadersAndCloseAtTheReturnLine() throws IOException {
        TextBuffer buffer = TextBuffer.create(corpus());
        SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        StructureDocument structure = StructureDocument.open(buffer, syntax);
        assertTrue("la classe ouvre un pli", structure.foldStartsAt(8));
        assertTrue("la signature de méthode ouvre un pli", structure.foldStartsAt(19));
        assertFalse("le corps ne plie pas", structure.foldStartsAt(22));
        // la fermeture du pli de la méthode porte l'accolade fermante
        int close = structure.foldEndLine(19);
        assertTrue("la ligne de fermeture porte le « } » : ligne " + close,
                buffer.getText(buffer.getLineStart(close), buffer.getLineEnd(close))
                        .toString().stripLeading().startsWith("}"));
        // la classe referme à la dernière ligne portée
        assertEquals(classCloseOf(buffer), structure.foldEndLine(8));
        // un bloc jamais refermé : jusqu'à la dernière ligne
        TextBuffer open = TextBuffer.create("class A {\n  int x;\n");
        SyntaxDocument openSyntax = SyntaxDocument.open(open, Languages.JAVA);
        StructureDocument openStructure = StructureDocument.open(open, openSyntax);
        assertEquals(open.getLineCount() - 1, openStructure.foldEndLine(0));
        assertTrue(openStructure.foldStartsAt(0));
        openStructure.detach();
        openSyntax.detach();
        structure.detach();
        syntax.detach();
    }

    private static int classCloseOf(TextBuffer buffer) {
        int line = buffer.getLineCount() - 1;
        while (line > 0 && buffer.getText(buffer.getLineStart(line),
                buffer.getLineEnd(line)).toString().isBlank()) {
            line--;
        }
        return line;
    }

    private static char charAt(TextBuffer buffer, int line, int column) {
        int start = buffer.getLineStart(line);
        return buffer.getText(start + column, start + column + 1).charAt(0);
    }

    private static boolean isSign(char c) {
        return c == '(' || c == ')' || c == '{' || c == '}'
                || c == '[' || c == ']';
    }

    private static char complement(char sign) {
        return switch (sign) {
            case '(' -> ')';
            case ')' -> '(';
            case '{' -> '}';
            case '}' -> '{';
            case '[' -> ']';
            default -> '[';
        };
    }

    private String corpus() throws IOException {
        return Files.readString(Path.of("src/test/resources/data/java-2000.java"));
    }
}
