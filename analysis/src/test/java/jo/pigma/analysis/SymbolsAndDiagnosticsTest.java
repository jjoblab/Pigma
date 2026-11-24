package jo.pigma.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import jo.pigma.core.TextBuffer;
import jo.pigma.syntax.Languages;
import jo.pigma.syntax.StructureDocument;
import jo.pigma.syntax.SyntaxDocument;

/**
 * L'index de symboles et les diagnostics : les déclarations comptent
 * double, la complétion répond par préfixe, les mésappariements de L2
 * se localisent.
 */
public class SymbolsAndDiagnosticsTest {

    private static final String DEMO = String.join("\n",
            "public final class Demo {",
            "    int count = 0;",
            "",
            "    public double weight(int seed) {",
            "        return seed * count;",
            "    }",
            "}",
            "");

    @Test
    public void completionAnswersByPrefixWithDeclarationsFirst() {
        TextBuffer buffer = TextBuffer.create(DEMO);
        SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        StructureDocument structure = StructureDocument.open(buffer, syntax);
        SymbolIndex index = SymbolIndex.of(buffer, syntax, structure,
                new JavaDeclarations());
        // les symboles : le type, le champ, la méthode — en ordre de
        // document
        List<SymbolIndex.Symbol> symbols = index.symbols();
        assertEquals(3, symbols.size());
        assertEquals("Demo", symbols.get(0).name());
        assertEquals("type", symbols.get(0).nature());
        assertEquals("count", symbols.get(1).name());
        assertEquals("weight", symbols.get(2).name());
        // la complétion : les déclarations d'abord, le préfixe filtre
        assertEquals(List.of("Demo"), index.query("Dem", 5));
        List<String> c = index.query("c", 5);
        assertTrue("le champ compte avant le mot : " + c, c.get(0).equals("count"));
        assertTrue("count est candidat", c.contains("count"));
        // un préfixe absent ne répond rien
        assertTrue(index.query("zzzz", 5).isEmpty());
        structure.detach();
        syntax.detach();
    }

    @Test
    public void diagnosticsLocateOrphanAndUnclosed() {
        // un fermant orphelin en tête, un ouvrant jamais refermé en
        // queue : les deux se localisent
        TextBuffer buffer = TextBuffer.create(
                "class A {\n  int x;\n}\n)\nclass B {\n");
        SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        StructureDocument structure = StructureDocument.open(buffer, syntax);
        DiagnosticsDocument diagnostics = DiagnosticsDocument.of(
                buffer, syntax, structure);
        List<DiagnosticsDocument.Diagnostic> found = diagnostics.diagnostics();
        assertEquals(2, found.size());
        assertEquals(')', buffer.getText(found.get(0).start(),
                found.get(0).end()).charAt(0));
        assertTrue(found.get(0).message().startsWith("fermant orphelin"));
        assertTrue(found.get(1).message().startsWith("ouvrant non fermé"));
        assertEquals('{', buffer.getText(found.get(1).start(),
                found.get(1).end()).charAt(0));
        // la fenêtre : la plage des lignes filtre
        assertEquals(1, diagnostics.betweenLines(3, 3).size());
        assertTrue(diagnostics.betweenLines(0, 1).isEmpty());
        structure.detach();
        syntax.detach();
    }

    @Test
    public void aBalancedDocumentCarriesNoDiagnostic() {
        TextBuffer buffer = TextBuffer.create(DEMO);
        SyntaxDocument syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        StructureDocument structure = StructureDocument.open(buffer, syntax);
        DiagnosticsDocument diagnostics = DiagnosticsDocument.of(
                buffer, syntax, structure);
        assertTrue(diagnostics.diagnostics().isEmpty());
        structure.detach();
        syntax.detach();
    }
}
