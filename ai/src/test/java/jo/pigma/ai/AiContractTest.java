package jo.pigma.ai;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Le contrat des types (B0) : l'immuable qui vit de ses valeurs
 * (l'égalité des enregistrements, le figé des listes), les nuls
 * qui se vident (aucun nul ne voyage), les arguments qui se
 * vérifient (les bornes, le budget, la tâche).
 */
public class AiContractTest {

    /** La demande : les nuls se vident, les listes se figent, le
     * budget négatif se refuse, la tâche se exige. */
    @Test
    public void theRequestNormalizesAndValidates() {
        List<AiDiagnostic> mutable = new ArrayList<>();
        mutable.add(new AiDiagnostic(AiDiagnostic.Severity.ERROR,
                "parenthèse orpheline", 3, 4));
        AiRequest request = new AiRequest(null, AiTask.EXPLAIN_ERROR,
                null, null, null, null, null, null, mutable, null, null, 0);
        assertEquals("la clef nulle se vide", "", request.requestId());
        assertEquals("le langage nul se vide", "", request.languageId());
        assertEquals("le préfixe nul se vide", "", request.prefix());
        assertEquals("le suffixe nul se vide", "", request.suffix());
        assertEquals("les diagnostics vivent", 1, request.diagnostics().size());
        assertEquals("les autres documents nuls se vident", 0,
                request.otherDocuments().size());
        assertEquals("le budget nul vit", 0, request.maxContextChars());
        assertThrows("la liste rendue est figée",
                UnsupportedOperationException.class,
                () -> request.diagnostics().clear());
        assertThrows("la tâche nulle se refuse",
                NullPointerException.class,
                () -> new AiRequest("", null, "", "", "", "", "", "",
                        null, "", null, 100));
        assertThrows("le budget négatif se refuse",
                IllegalArgumentException.class,
                () -> new AiRequest("", AiTask.EXPLAIN, "", "", "", "",
                        "", "", null, "", null, -1));
    }

    /** L'égalité des enregistrements : deux demandes de mêmes
     * valeurs s'égalent (la clef de cache, le test du hôte). */
    @Test
    public void theRecordsEqualByValue() {
        AiDiagnostic one = new AiDiagnostic(AiDiagnostic.Severity.ERROR,
                "oups", 1, 2);
        AiDiagnostic same = new AiDiagnostic(AiDiagnostic.Severity.ERROR,
                "oups", 1, 2);
        assertEquals("le diagnostic s'égale à sa copie", one, same);
        List<AiDiagnostic> diagnostics = List.of(one);
        AiRequest first = new AiRequest("r1", AiTask.FIX, "java", "A.java",
                "pre", "post", "sel", "corrige", diagnostics, "{ }",
                List.of(new AiDocumentExcerpt("B.java", "class B {}")), 500);
        AiRequest second = new AiRequest("r1", AiTask.FIX, "java", "A.java",
                "pre", "post", "sel", "corrige", List.of(same), "{ }",
                List.of(new AiDocumentExcerpt("B.java", "class B {}")), 500);
        assertEquals("la demande s'égale à sa copie", first, second);
        assertEquals("le hachage suit", first.hashCode(), second.hashCode());
        AiRequest other = new AiRequest("r2", AiTask.FIX, "java", "A.java",
                "pre", "post", "sel", "corrige", diagnostics, "{ }",
                List.of(), 500);
        assertNotEquals("la clef autre diffère", first, other);
    }

    /** Les bornes : la plage du diagnostic, le remplacement et
     * l'insertion se vérifient ; l'erreur et les capacités se
     * normalisent. */
    @Test
    public void theRangesValidateAndTheCapsNormalize() {
        assertThrows("la plage inversée se refuse",
                IllegalArgumentException.class,
                () -> new AiDiagnostic(AiDiagnostic.Severity.INFO, "x", 5, 4));
        assertThrows("la plage négative se refuse",
                IllegalArgumentException.class,
                () -> new AiDiagnostic(AiDiagnostic.Severity.INFO, "x", -1, 4));
        assertThrows("le remplacement inversé se refuse",
                IllegalArgumentException.class,
                () -> new AiResult.Replacement(5, 4, "x"));
        assertThrows("l'insertion négative se refuse",
                IllegalArgumentException.class,
                () -> new AiResult.Insertion(-1, "x"));
        assertThrows("la catégorie nulle se refuse",
                NullPointerException.class,
                () -> new AiError(null, "détail"));
        assertEquals("le message nul se vide", "",
                new AiError(AiError.Category.TIMEOUT, null).message());
        AiCapabilities none = AiCapabilities.NONE;
        assertFalse("rien n'est accepté sans déclaration",
                none.accepts(AiTask.EXPLAIN));
        assertFalse("sans flux", none.streaming());
        AiCapabilities caps = new AiCapabilities(
                java.util.Set.of(AiTask.EXPLAIN, AiTask.FIX), true);
        assertTrue("la tâche déclarée s'accepte", caps.accepts(AiTask.FIX));
        assertFalse("l'autre non", caps.accepts(AiTask.DOCUMENT));
        AiResult.Text text = new AiResult.Text(null);
        assertEquals("le markdown nul se vide", "", text.markdown());
    }
}
