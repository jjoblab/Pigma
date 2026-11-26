package jo.pigma.editor.popups;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * L'analyseur du document balisé (JVM, sans canevas) : le sous-ensemble
 * des serveurs — les titres, les paragraphes fondus, les listes à
 * puces et numérotées, les blocs de code clôturés, les filets, et en
 * ligne le gras, l'italique, le code et les liens. Le défaut du
 * rapport des popups : le balisage mourait fondu en texte nu.
 */
public class MarkdownDocTest {

    /** Les blocs d'un document simple : deux paragraphes séparés au blanc. */
    @Test
    public void theParagraphsSplitAtTheBlank() {
        List<MarkdownDoc.Block> blocks = MarkdownDoc.parse("premier dit.\n\nsecond dit.");
        assertEquals("deux paragraphes", 2, blocks.size());
        assertEquals(MarkdownDoc.PARAGRAPH, blocks.get(0).kind());
        assertEquals("le premier vit seul", "premier dit.",
                spanText(blocks.get(0)));
        assertEquals("le second vit seul", "second dit.",
                spanText(blocks.get(1)));
    }

    /** Les lignes voisines d'un paragraphe se fondent (le blanc seul coupe). */
    @Test
    public void theNeighbourLinesMerge() {
        List<MarkdownDoc.Block> blocks = MarkdownDoc.parse("le premier\nle second\nle tiers");
        assertEquals("un seul bloc", 1, blocks.size());
        assertEquals("les lignes se fondent en une phrase",
                "le premier le second le tiers", spanText(blocks.get(0)));
    }

    /** Les titres : leur rang se borne, la coupe suit les dièses. */
    @Test
    public void theHeadingsCarryTheirLevel() {
        List<MarkdownDoc.Block> blocks = MarkdownDoc.parse(
                "# le un\n## le deux\n### le trois\n###### le six borne à trois");
        assertEquals("quatre titres", 4, blocks.size());
        assertEquals(1, blocks.get(0).level());
        assertEquals(2, blocks.get(1).level());
        assertEquals(3, blocks.get(2).level());
        assertEquals("au-delà du rang trois, le rang se borne", 3,
                blocks.get(3).level());
        assertEquals("la coupe suit les dièses", "le un",
                spanText(blocks.get(0)));
    }

    /** Les listes : la puce et la numérotation, la marque se garde. */
    @Test
    public void theListsCarryTheirMarker() {
        List<MarkdownDoc.Block> blocks = MarkdownDoc.parse(
                "- premier\n- second\n1. le un\n2) le deux");
        assertEquals("quatre entrées", 4, blocks.size());
        assertEquals(MarkdownDoc.BULLET, blocks.get(0).kind());
        assertEquals(MarkdownDoc.BULLET, blocks.get(1).kind());
        assertEquals(MarkdownDoc.NUMBER, blocks.get(2).kind());
        assertEquals("la marque numérotée se garde", "1.",
                blocks.get(2).marker());
        assertEquals("2)", blocks.get(3).marker());
        assertEquals("la coupe suit la marque", "le un",
                spanText(blocks.get(2)));
    }

    /** Les blocs de code : la clôture les borne, le texte y demeure BRUT. */
    @Test
    public void theFencedCodeStaysRaw() {
        List<MarkdownDoc.Block> blocks = MarkdownDoc.parse(
                "avant\n```java\nint a = 1;\nint b = 2;\n```\naprès");
        assertEquals("trois blocs", 3, blocks.size());
        assertEquals(MarkdownDoc.CODE, blocks.get(1).kind());
        assertEquals("le code demeure brut", "int a = 1;\nint b = 2;\n",
                blocks.get(1).rawText());
        assertEquals("la clôture ne vit pas au document",
                MarkdownDoc.PARAGRAPH, blocks.get(2).kind());
    }

    /** Le filet horizontal : trois traits d'affilée le disent. */
    @Test
    public void theRulesStandAlone() {
        List<MarkdownDoc.Block> blocks = MarkdownDoc.parse(
                "avant\n---\naprès");
        assertEquals("trois blocs", 3, blocks.size());
        assertEquals(MarkdownDoc.RULE, blocks.get(1).kind());
        assertTrue("le filet ne porte rien", blocks.get(1).spans().isEmpty());
    }

    /** En ligne : le gras, l'italique et le code découpent leurs portées. */
    @Test
    public void theInlineStylesSplit() {
        List<MarkdownDoc.Block> blocks = MarkdownDoc.parse(
                "le **gras** et *l'italique* et `le code`");
        assertEquals(1, blocks.size());
        List<MarkdownDoc.Span> spans = blocks.get(0).spans();
        assertEquals("six portées", 6, spans.size());
        assertEquals("le ", spans.get(0).text());
        assertTrue("le gras se porte", spans.get(1).bold());
        assertEquals("gras", spans.get(1).text());
        assertTrue("l'italique se porte", spans.get(3).italic());
        assertTrue("le code se porte", spans.get(5).code());
        assertFalse("les portées nues restent nues", spans.get(0).bold());
    }

    /** Le gras dans l'italique : les deux se portent ensemble. */
    @Test
    public void theBoldCarriesTheItalicInside() {
        List<MarkdownDoc.Block> blocks = MarkdownDoc.parse(
                "le **gras et *l'italique* fin**");
        List<MarkdownDoc.Span> spans = blocks.get(0).spans();
        boolean boldItalic = false;
        for (MarkdownDoc.Span span : spans) {
            if (span.bold() && span.italic() && span.text().equals("l'italique")) {
                boldItalic = true;
            }
        }
        assertTrue("l'italique dans le gras se porte double", boldItalic);
    }

    /** Les liens : le libellé se montre, l'adresse se garde. */
    @Test
    public void theLinksKeepTheirAddress() {
        List<MarkdownDoc.Block> blocks = MarkdownDoc.parse(
                "voir [la page](https://exemple.fr) du manuel");
        List<MarkdownDoc.Span> spans = blocks.get(0).spans();
        boolean linkSeen = false;
        for (MarkdownDoc.Span span : spans) {
            if (span.text().equals("la page")) {
                linkSeen = true;
                assertEquals("l'adresse se garde", "https://exemple.fr",
                        span.link());
            }
        }
        assertTrue("le libellé du lien se montre", linkSeen);
    }

    /** Les paires non fermées demeurent du texte (rien ne se vole). */
    @Test
    public void theUnbalancedMarksStayPlain() {
        List<MarkdownDoc.Block> blocks = MarkdownDoc.parse("2*3 et 4**5");
        List<MarkdownDoc.Span> spans = blocks.get(0).spans();
        for (MarkdownDoc.Span span : spans) {
            assertFalse("l'astérisque collée reste du texte", span.bold());
            assertFalse("l'astérisque collée reste du texte", span.italic());
        }
        assertEquals("le texte demeure entier", "2*3 et 4**5",
                spanText(blocks.get(0)));
    }

    /** Le document vide ne porte rien ; le texte nu se reconnaît. */
    @Test
    public void theEmptyAndPlainDocumentsTellThemselves() {
        assertTrue("le vide ne porte rien", MarkdownDoc.parse("").isEmpty());
        assertTrue("le null ne porte rien", MarkdownDoc.parse(null).isEmpty());
        assertFalse("le texte nu ne se dit pas balisé",
                MarkdownDoc.isRich("un texte nu"));
        assertTrue("les dièses se disent balisés",
                MarkdownDoc.isRich("# titre"));
        assertTrue("les apostrophes doubles se disent balisées",
                MarkdownDoc.isRich("du **gras**"));
        assertTrue("le code en ligne se dit balisé", MarkdownDoc.isRich("du `code`"));
        assertTrue("les lignes vides se disent balisées",
                MarkdownDoc.isRich("un\n\ndeux"));
    }

    /** Le texte d'un bloc : ses portées mises bout à bout. */
    private String spanText(MarkdownDoc.Block block) {
        StringBuilder text = new StringBuilder();
        for (MarkdownDoc.Span span : block.spans()) {
            text.append(span.text());
        }
        return text.toString().strip();
    }
}
