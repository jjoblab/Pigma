package jo.pigma.editor.popups;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * La carte de signature AUX SURCHARGES (JVM, sans canevas) : le défaut
 * du rapport des popups — l'adaptation d'avant gardait la signature
 * active seule, les autres mouraient en chemin. La carte porte TOUTES
 * les surcharges, le sélecteur y boucle (la dernière repart à la
 * première), la documentation du paramètre ACTIF de la surcharge
 * MONTRÉE se lit.
 */
public class SignatureCardTest {

    /** La carte éprouvée, à la densité unité. */
    private final SignatureCard card = new SignatureCard(1.0f);

    /** Deux surcharges : l'active est la seconde, son second paramètre est documenté. */
    private List<SignatureCard.Overload> overloads() {
        return List.of(
                new SignatureCard.Overload("essai(int a)",
                        List.of("la documentation du premier paramètre", "")),
                new SignatureCard.Overload("essai(int a, int b)",
                        List.of("", "la documentation du second paramètre")));
    }

    /**
     * Les SURCHARGES vivent : la carte en porte deux, l'ACTIVE naît
     * montrée, sa ligne se lit, le paramètre actif suit.
     */
    @Test
    public void theOverloadsLive() {
        card.set(overloads(), 1, 1);
        assertTrue("la carte vit à deux surcharges", card.isShown());
        assertEquals("la signature ACTIVE naît montrée", "essai(int a, int b)",
                card.label());
        assertEquals("les surcharges comptent", 2, card.overloadCount());
        assertEquals("l'index montré naît à l'active", 1, card.shownSignature());
        assertEquals("le paramètre actif suit", 1, card.activeParameter());
        assertEquals("la documentation du paramètre actif se lit",
                "la documentation du second paramètre",
                card.parameterDocumentation());
    }

    /**
     * Le SÉLECTEUR boucle : le cycle avance, borne et REPART (la
     * dernière repart à la première — le parcours ne meurt pas aux
     * murs) ; la documentation suit la surcharge montrée.
     */
    @Test
    public void theSelectorLoops() {
        card.set(overloads(), 0, 0);
        assertEquals("la première naît montrée", "essai(int a)", card.label());
        assertEquals("la documentation du paramètre actif de la première",
                "la documentation du premier paramètre",
                card.parameterDocumentation());
        card.cycle(1);
        assertEquals("le cycle avance", 1, card.shownSignature());
        card.cycle(1);
        assertEquals("la dernière REPART à la première", 0, card.shownSignature());
        card.cycle(-1);
        assertEquals("le recul à la dernière boucle", 1, card.shownSignature());
    }

    /**
     * La documentation du paramètre actif suit la SURCHARGE MONTRÉE :
     * la surcharge sans documentation du paramètre actif se tait
     * (l'index hors bornes ne lève pas — le serveur peut dire un
     * paramètre que la surcharge ne porte pas).
     */
    @Test
    public void theParameterDocumentationBounds() {
        card.set(overloads(), 1, 5);
        assertEquals("l'index du paramètre se porte tel quel", 5,
                card.activeParameter());
        assertEquals("le paramètre hors bornes se tait", "",
                card.parameterDocumentation());
    }

    /** La carte VIDE se range : une liste nulle ou vide la tait. */
    @Test
    public void theEmptyHelpRangesTheCard() {
        card.set(List.of(), 0, 0);
        assertFalse("la liste vide range la carte", card.isShown());
        assertNull("aucune signature ne se lit", card.label());
        card.set(null, 0, 0);
        assertFalse("la liste nulle range la carte", card.isShown());
    }

    /** L'index d'active hors bornes se borne (le serveur peut dériver). */
    @Test
    public void theActiveSignatureBounds() {
        card.set(overloads(), 9, 0);
        assertEquals("l'index hors bornes retombe à la première", 0,
                card.shownSignature());
        card.set(overloads(), -2, 0);
        assertEquals("l'index négatif retombe à la première", 0,
                card.shownSignature());
    }
}
