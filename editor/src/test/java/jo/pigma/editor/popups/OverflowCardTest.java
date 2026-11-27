package jo.pigma.editor.popups;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * La carte de débordement porte les natures de ses rangées (JVM,
 * sans Android) : chaque rangée SA nature au trait (le retour du
 * terrain des icônes, la leçon de l'ampoule), la rangée sans nature
 * garde l'éclair — la nature voyage de la rangée au dessin sans se
 * perdre, l'étage d'édition lit CELLE du registre (la même définition
 * que la carte d'actions rapides).
 */
public final class OverflowCardTest {

    /**
     * LA RANGÉE PORTE SA NATURE : le constructeur dit la nature, la
     * rangée la garde ; la rangée SANS nature garde l'éclair (le
     * défaut honnête — les rangées existantes ne changent pas).
     */
    @Test
    public void theRowCarriesItsGlyph() {
        OverflowCard.Row cut = new OverflowCard.Row("Couper",
                () -> { }, QuickActionCard.Glyph.CUT);
        assertEquals("la nature dite se lit", QuickActionCard.Glyph.CUT,
                cut.glyph());
        OverflowCard.Row bare = new OverflowCard.Row("L'action de l'hôte",
                () -> { });
        assertEquals("sans nature dite : l'éclair demeure",
                QuickActionCard.Glyph.ACTION, bare.glyph());
    }

    /**
     * L'ÉTAGE D'ÉDITION lit la nature du REGISTRE (la même
     * définition que la carte d'actions rapides — le commentaire de
     * l'éditeur y porte ses barres, la poubelle sa benne).
     */
    @Test
    public void theEditionStageReadsTheRegistryGlyphs() {
        ActionRegistry registry = new ActionRegistry()
                .add("Commenter / décommenter", QuickActionCard.Glyph.COMMENT,
                        () -> { }, () -> true)
                .add("Dupliquer la ligne", QuickActionCard.Glyph.DUPLICATE,
                        () -> { }, () -> true);
        List<OverflowCard.Row> edition = new java.util.ArrayList<>();
        for (ActionRegistry.Action action : registry.live()) {
            edition.add(new OverflowCard.Row(action.label(), action::run,
                    action.glyph()));
        }
        OverflowCard card = new OverflowCard(1.0f);
        card.openSections(List.of(new OverflowCard.Section("Édition", edition)));
        assertTrue("la carte vit", card.isShown());
        assertEquals("les libellés se lisent", List.of(
                "Commenter / décommenter", "Dupliquer la ligne"),
                card.labels());
        // les natures voyagent à la rangée (le dessin les lira au
        // peintre partagé — la nature ne se perd pas au voyage)
        assertEquals(QuickActionCard.Glyph.COMMENT, edition.get(0).glyph());
        assertEquals(QuickActionCard.Glyph.DUPLICATE, edition.get(1).glyph());
    }

    /**
     * La carte vide se range : les sections nulles ou vides ne se
     * posent pas, la liste vide range la carte.
     */
    @Test
    public void theEmptySectionsFoldTheCard() {
        OverflowCard card = new OverflowCard(1.0f);
        card.openSections(List.of());
        assertTrue("la carte vide se range", !card.isShown());
        card.openSections(List.of(new OverflowCard.Section("Étage",
                List.of())));
        assertTrue("l'étage vide ne se montre pas", !card.isShown());
    }
}
