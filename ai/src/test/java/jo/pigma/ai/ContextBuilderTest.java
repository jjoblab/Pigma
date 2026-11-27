package jo.pigma.ai;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Le préparateur du contexte (B1) : la troncature au budget (le
 * début et la fin de fichier, le curseur au bord, le budget nul),
 * le bloc englobant absent, la sélection vivante, les extraits du
 * callback — et LES REFUS : la politique de l'hôte d'abord (rien ne
 * se construit), le filtre ensuite (la demande se modifie ou se
 * refuse). Le filtre par défaut masque les formes communes de
 * secret — un filet, jamais une politique.
 */
public class ContextBuilderTest {

    /** La politique qui refuse tout. */
    private static final ContextBuilder.AiPolicy REFUSE_ALL =
            (name, language) -> false;

    /** Le filtre qui refuse tout. */
    private static final ContextBuilder.ContextFilter REFUSE_EVERYTHING =
            request -> null;

    /** Un document long : quatre-vingts caractères numérotés. */
    private static String longText() {
        StringBuilder text = new StringBuilder();
        for (int at = 0; at < 80; at++) {
            text.append((char) ('a' + at % 26));
        }
        return text.toString();
    }

    /**
     * LA TRONCATURE : le préfixe et le suffixe se bornent à la
     * moitié du budget chacun — le curseur au BORD commence ou
     * termine le document, le budget NUL ne voyage pas, le curseur
     * au MILIEU coupe des deux côtés.
     */
    @Test
    public void theTruncationBoundsTheContext() {
        ContextBuilder builder = new ContextBuilder(null, null);
        String text = longText();
        // le curseur au MILIEU (40), budget 20 : dix avant, dix après
        AiRequest middle = builder.prepare(AiTask.EXPLAIN, "",
                new ContextBuilder.Source("A.java", "java", text, 40,
                        -1, -1, List.of(), ""), 20, null);
        assertEquals("le préfixe prend la moitié avant", 10,
                middle.prefix().length());
        assertEquals("le suffixe prend la moitié après", 10,
                middle.suffix().length());
        assertEquals("le préfixe finit au curseur",
                text.substring(30, 40), middle.prefix());
        assertEquals("le suffixe commence au curseur",
                text.substring(40, 50), middle.suffix());
        // le curseur au DÉBUT : le préfixe est vide
        AiRequest start = builder.prepare(AiTask.EXPLAIN, "",
                new ContextBuilder.Source("A.java", "java", text, 0,
                        -1, -1, List.of(), ""), 20, null);
        assertTrue("le curseur au début n'a pas de préfixe",
                start.prefix().isEmpty());
        assertEquals("le suffixe prend le budget", 20,
                start.suffix().length());
        // le curseur à la FIN : le suffixe est vide
        AiRequest end = builder.prepare(AiTask.EXPLAIN, "",
                new ContextBuilder.Source("A.java", "java", text,
                        text.length(), -1, -1, List.of(), ""), 20, null);
        assertTrue("le curseur à la fin n'a pas de suffixe",
                end.suffix().isEmpty());
        assertEquals("le préfixe prend le budget", 20,
                end.prefix().length());
        // le budget NUL : le contexte ne voyage pas
        AiRequest poor = builder.prepare(AiTask.EXPLAIN, "",
                new ContextBuilder.Source("A.java", "java", text, 40,
                        -1, -1, List.of(), ""), 0, null);
        assertTrue("budget nul : pas de préfixe", poor.prefix().isEmpty());
        assertTrue("budget nul : pas de suffixe", poor.suffix().isEmpty());
        assertEquals("le budget voyage pour l'hôte", 0,
                poor.maxContextChars());
    }

    /**
     * LA SÉLECTION voyage : le texte entre ses bornes, et le contexte
     * l'ENTOURE (le préfixe finit à son début, le suffixe commence à
     * sa fin).
     */
    @Test
    public void theSelectionTravelsSurrounded() {
        ContextBuilder builder = new ContextBuilder(null, null);
        String text = "aaaaSELECTIONzzzz";
        AiRequest request = builder.prepare(AiTask.EDIT_SELECTION, "réécris",
                new ContextBuilder.Source("A.java", "java", text, 4,
                        4, 13, List.of(), ""), 8, null);
        assertEquals("la sélection voyage", "SELECTION", request.selection());
        assertEquals("l'instruction voyage", "réécris", request.instruction());
        assertEquals("le préfixe s'arrête au début de la sélection",
                "aaaa", request.prefix());
        assertEquals("le suffixe reprend à la fin de la sélection",
                "zzzz", request.suffix());
    }

    /**
     * LE BLOC ENGLobANT ABSENT voyage vide — et présent, il voyage
     * tel quel (la profondeur d'accolades le déduit en amont).
     */
    @Test
    public void theEnclosingBlockPassesThrough() {
        ContextBuilder builder = new ContextBuilder(null, null);
        AiRequest empty = builder.prepare(AiTask.EXPLAIN, "",
                new ContextBuilder.Source("A.java", "java", "x", 0,
                        -1, -1, List.of(), null), 100, null);
        assertTrue("le bloc absent voyage vide",
                empty.enclosingBlock().isEmpty());
        AiRequest block = builder.prepare(AiTask.EXPLAIN, "",
                new ContextBuilder.Source("A.java", "java", "x", 0,
                        -1, -1, List.of(), "{ int x = 1; }"), 100, null);
        assertEquals("le bloc voyage tel quel", "{ int x = 1; }",
                block.enclosingBlock());
    }

    /**
     * LES REFUS : la politique d'abord (rien ne se construit, le
     * callback des extraits ne se demande PAS), le filtre ensuite
     * (la demande se refuse). Un crochet nul ne refuse rien.
     */
    @Test
    public void theHooksRefuseAndOrder() {
        boolean[] supplierAsked = {false};
        ContextBuilder.OtherDocumentsSupplier supplier = (name, language) -> {
            supplierAsked[0] = true;
            return List.of(new AiDocumentExcerpt("B.java", "class B {}"));
        };
        // LA POLITIQUE REFUSE : rien ne se construit, le callback ne
        // se demande pas
        ContextBuilder refused = new ContextBuilder(REFUSE_ALL, null);
        assertNull("la politique refuse : rien ne part",
                refused.prepare(AiTask.EXPLAIN, "",
                        new ContextBuilder.Source(".env", "properties",
                                "x", 0, -1, -1, List.of(), ""), 100,
                        supplier));
        assertFalse("le callback ne se demande pas au refus",
                supplierAsked[0]);
        // LE FILTRE REFUSE : la demande construite ne part pas
        ContextBuilder filtered = new ContextBuilder(null, REFUSE_EVERYTHING);
        assertNull("le filtre refuse : rien ne part",
                filtered.prepare(AiTask.EXPLAIN, "",
                        new ContextBuilder.Source("A.java", "java", "x", 0,
                                -1, -1, List.of(), ""), 100, supplier));
        assertTrue("le callback s'est demandé avant le filtre",
                supplierAsked[0]);
        // LE FILTRE MODIFIE : la demande modifiée part
        ContextBuilder trimmed = new ContextBuilder(null,
                request -> new AiRequest(request.requestId(),
                        request.task(), request.languageId(),
                        request.documentName(), "", "", request.selection(),
                        request.instruction(), request.diagnostics(),
                        request.enclosingBlock(), List.of(),
                        request.maxContextChars()));
        AiRequest request = trimmed.prepare(AiTask.EXPLAIN, "",
                new ContextBuilder.Source("A.java", "java", "abcdef", 3,
                        -1, -1, List.of(), ""), 100, supplier);
        assertTrue("le filtre a retiré le préfixe",
                request.prefix().isEmpty() && request.suffix().isEmpty());
        assertEquals("le filtre a retiré les extraits", 0,
                request.otherDocuments().size());
    }

    /**
     * LES EXTRAITS du callback voyagent — et le filtre par défaut
     * MASQUE les formes communes de secret (un filet, jamais une
     * politique : il ne refuse rien).
     */
    @Test
    public void theExcerptsTravelAndTheDefaultFilterMasks() {
        ContextBuilder plain = new ContextBuilder(null, null);
        AiRequest request = plain.prepare(AiTask.EXPLAIN, "",
                new ContextBuilder.Source("A.java", "java", "x", 0, -1, -1,
                        List.of(), ""), 100,
                (name, language) -> List.of(new AiDocumentExcerpt("B.java",
                        "class B {}")));
        assertEquals("l'extrait du callback voyage", 1,
                request.otherDocuments().size());
        assertEquals("l'extrait porte son nom", "B.java",
                request.otherDocuments().get(0).name());
        // LE FILTRE PAR DÉFAUT : les secrets se masquent, le reste
        // demeure — la demande ne se refuse jamais
        ContextBuilder masked = new ContextBuilder(null,
                new DefaultContextFilter());
        AiRequest secret = masked.prepare(AiTask.EXPLAIN, "",
                new ContextBuilder.Source("A.java", "java",
                        "String password = \"hunter2\";\nint x = 1;", 30,
                        -1, -1, List.of(), ""), 100, null);
        assertTrue("le secret se masque",
                secret.prefix().contains("…masqué…"));
        assertFalse("la valeur ne voyage pas",
                secret.prefix().contains("hunter2"));
        assertTrue("le reste du contexte demeure",
                secret.suffix().contains("x = 1"));
        AiRequest key = masked.prepare(AiTask.EXPLAIN, "",
                new ContextBuilder.Source("A.java", "java",
                        "-----BEGIN PRIVATE KEY-----\nabc\n-----END PRIVATE KEY-----",
                        40, -1, -1, List.of(), ""), 100, null);
        assertFalse("la clef PEM ne voyage pas", key.prefix().contains("abc"));
        assertTrue("la clef se masque", key.prefix().contains("…masqué…"));
    }

    /** La clef de corrélation : chaque demande porte la sienne. */
    @Test
    public void theRequestIdDiffers() {
        ContextBuilder builder = new ContextBuilder(null, null);
        ContextBuilder.Source source = new ContextBuilder.Source(
                "A.java", "java", "x", 0, -1, -1, List.of(), "");
        AiRequest first = builder.prepare(AiTask.EXPLAIN, "", source,
                10, null);
        AiRequest second = builder.prepare(AiTask.EXPLAIN, "", source,
                10, null);
        assertFalse("les clefs diffèrent",
                first.requestId().equals(second.requestId()));
        assertTrue("la clef parle de Pigma",
                first.requestId().startsWith("pigma-"));
    }
}
