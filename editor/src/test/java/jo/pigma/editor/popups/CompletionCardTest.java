package jo.pigma.editor.popups;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * La carte de complétion aux candidats ENTIERS (JVM, sans canevas) :
 * l'entrée porte l'obsolescence, la documentation et le remplacement
 * propre du serveur — la documentation se pose DANS l'entrée (elle y
 * survit au re-filtre), le candidat nu garde sa pose d'avant, la
 * plage du remplacement se normalise (la plage inversée se laisse).
 * Le défaut du rapport des popups : le candidat arrivait réduit à
 * son libellé.
 */
public class CompletionCardTest {

    /** La carte éprouvée, à la densité unité. */
    private final CompletionCard card = new CompletionCard(1.0f);

    /**
     * L'entrée ENTIÈRE : l'obsolescence, la documentation balisée et
     * le remplacement propre du serveur y vivrent ; la documentation
     * se pose DANS l'entrée et y survit à la re-filtration.
     */
    @Test
    public void theEntireEntryCarriesTheServerFields() {
        CompletionCard.Entry entry = new CompletionCard.Entry("premiere",
                "la fonction première", CompletionCard.KIND_FUNCTION, true,
                "La **première** fonction.", true, "premiere", 12, 21);
        assertTrue("l'obsolescence se porte", entry.deprecated());
        assertEquals("la documentation se porte", "La **première** fonction.",
                entry.documentation());
        assertTrue("le genre du balisage se porte", entry.markdownDoc());
        assertEquals("le texte d'insertion se porte", "premiere",
                entry.insertText());
        assertEquals("la plage du remplacement se porte", 12, entry.insertStart());
        assertEquals("la fin de la plage se porte", 21, entry.insertEnd());
        // la plage INVERSÉE se laisse (le serveur peut dériver — le
        // préfixe tapé reste la règle)
        CompletionCard.Entry inverted = new CompletionCard.Entry("a", "", 0,
                false, "", false, "", 21, 12);
        assertEquals("la plage inversée se laisse", -1, inverted.insertStart());
        assertEquals("la plage inversée se laisse (fin)", -1, inverted.insertEnd());
        // le candidat NU garde sa pose d'avant (libellé seul, préfixe remplacé)
        CompletionCard.Entry bare = new CompletionCard.Entry("prime", "",
                CompletionCard.KIND_UNKNOWN);
        assertFalse("le candidat nu n'est pas obsolète", bare.deprecated());
        assertEquals("le candidat nu ne porte pas de documentation", "",
                bare.documentation());
        assertEquals("le candidat nu ne porte pas de texte propre", "",
                bare.insertText());
        assertEquals("le candidat nu remplace le mot tapé", -1, bare.insertStart());
    }

    /**
     * La documentation se pose DANS l'entrée : elle survit au
     * re-filtre QUI NE CHANGE PAS LA LISTE (mêmes candidats — la
     * documentation reste la sienne) ; un nom disparu de la carte
     * se laisse.
     */
    @Test
    public void theDocumentationSettlesInTheEntry() {
        card.set(List.of(new CompletionCard.Entry("premiere", "",
                CompletionCard.KIND_UNKNOWN)), "pre");
        assertTrue("la documentation se pose sur le candidat vivant",
                card.setDocumentation("premiere", "La documentation résolue.",
                        true));
        assertEquals("la documentation vit dans l'entrée",
                "La documentation résolue.",
                card.entries().get(0).documentation());
        assertTrue("le genre du balisage vit dans l'entrée",
                card.entries().get(0).markdownDoc());
        assertFalse("un nom disparu se laisse",
                card.setDocumentation("autre", "rien", false));
        // le re-filtre QUI CHANGE la liste repart SANS documentation
        // (l'entrée neuve la porte vide — la vue y redemande : la
        // résolution la rend) ; le re-filtre SANS changement la garde
        card.set(List.of(new CompletionCard.Entry("premiere", "",
                CompletionCard.KIND_UNKNOWN), new CompletionCard.Entry(
                        "prems", "", CompletionCard.KIND_UNKNOWN)), "prem");
        assertEquals("la liste changée repart sans documentation", "",
                card.entries().get(0).documentation());
    }

    /**
     * Le candidat choisi SURVIT au re-filtre ET porte sa
     * documentation à travers — la carte appelle set() sans
     * documentation, la documentation posée avant ne revient PAS
     * (l'entrée neuve remplace l'ancienne : la documentation arrive
     * APRÈS, la résolution la porte).
     */
    @Test
    public void theChosenCandidateSurvivesTheRefilter() {
        card.set(List.of(new CompletionCard.Entry("premier", "", 0),
                new CompletionCard.Entry("second", "", 0)), "pre");
        card.onArrow(1);
        card.onArrow(1);
        assertEquals("le candidat choisi vit", 1, card.highlighted());
        card.set(List.of(new CompletionCard.Entry("premier", "", 0),
                new CompletionCard.Entry("second", "", 0)), "prem");
        assertEquals("le candidat choisi survit", 1, card.highlighted());
        card.setDocumentation("second", "La doc du second.", false);
        assertEquals("la documentation du CHOISI se montre", "La doc du second.",
                card.entries().get(card.highlighted()).documentation());
    }
}
