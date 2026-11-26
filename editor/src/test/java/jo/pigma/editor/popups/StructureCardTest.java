package jo.pigma.editor.popups;

import java.util.List;

import android.graphics.Canvas;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * La STRUCTURE DU FICHIER aux JVM (sans canevas réel) : le FILTRE à
 * la frappe (les lettres tapées resserrent la liste — le tampon ne
 * bouge pas), la flèche descend la mise en évidence, la validation Y
 * VA, le symbole courant se détache (le gras se lit du curseur),
 * la croix range.
 */
public class StructureCardTest {

    /** La carte éprouvée, à la densité unité. */
    private final StructureCard card = new StructureCard(1.0f);

    /** Un symbole de compteur (la navigation y compte). */
    private int jumped;

    private StructureCard.Symbol symbol(String name, int kind, int line) {
        return new StructureCard.Symbol(name, kind, line, () -> jumped++);
    }

    private java.util.List<StructureCard.Symbol> document() {
        return List.of(symbol("graine", CompletionCard.KIND_FIELD, 0),
                symbol("Pigma", CompletionCard.KIND_CLASS, 2),
                symbol("total", CompletionCard.KIND_FUNCTION, 5),
                symbol("SEUIL", CompletionCard.KIND_FIELD, 8));
    }

    /** L'état de vie : la liste vide range la carte, la croix la range. */
    @Test
    public void theLifeOfTheCard() {
        assertFalse("la carte naît rangée", card.isShown());
        card.open(List.of());
        assertFalse("la liste vide n'ouvre rien", card.isShown());
        card.open(document());
        assertTrue("la carte ouvre", card.isShown());
        assertEquals("les symboles se lisent", 4, card.symbols().size());
        assertEquals("le filtre part vide", "", card.filter());
        card.close();
        assertFalse("la fermeture range la carte", card.isShown());
        assertTrue("les symboles se ramassent", card.symbols().isEmpty());
    }

    /**
     * LE FILTRE À LA FRAPPE : les lettres tapées resserrent la liste
     * (la casse ne compte pas, le nom qui CONTIENT demeure), le
     * retour arrière ôte, la composition remplace.
     */
    @Test
    public void theFilterLivesAtTheTyping() {
        card.open(document());
        assertTrue("la lettre se prend", card.onText("g"));
        assertEquals("le filtre se pose", "g", card.filter());
        assertEquals("la liste se resserre (les noms qui contiennent « g » : graine, Pigma)", 2,
                card.filtered().size());
        // la lettre suivante resserre encore : « gr » ne garde que graine
        assertTrue("la seconde lettre se prend", card.onText("r"));
        assertEquals("le filtre se pose", "gr", card.filter());
        assertEquals("la liste se resserre au nom qui contient", 1,
                card.filtered().size());
        assertEquals("le symbole filtré se lit", "graine",
                card.filtered().get(0).name());
        // la casse ne compte pas : « P » s'ajoute — « grP » ne garde rien
        assertTrue("la majuscule se prend", card.onText("P"));
        assertEquals("le filtre se pose", "grP", card.filter());
        assertEquals("la liste ne garde rien (aucun nom ne contient « grp »)", 0,
                card.filtered().size());
        // le retour arrière ôte la dernière lettre : « gr » revient
        assertTrue("le retour arrière se prend", card.onBackspace());
        assertEquals("le retour arrière ôte", "gr", card.filter());
        assertEquals("la liste revient", 1, card.filtered().size());
        // la composition remplace le filtre entier
        assertTrue("la composition remplace", card.onComposition("PI"));
        assertEquals("la composition se pose", "PI", card.filter());
        assertEquals("la casse ne compte pas", 1, card.filtered().size());
        assertEquals("le symbole composé se lit", "Pigma",
                card.filtered().get(0).name());
        // le filtre vide rend tout le document
        card.onComposition("");
        assertEquals("le filtre vide rend tout", 4, card.filtered().size());
        assertFalse("le retour arrière au filtre vide ne se prend pas",
                card.onBackspace());
    }

    /** La flèche descend la mise en évidence, la validation Y VA. */
    @Test
    public void theArrowDescendsTheEmphasisTheEnterGoes() {
        card.open(document());
        assertTrue("la flèche se prend", card.onArrow(1));
        assertTrue("la validation se prend", card.onEnter());
        assertEquals("le symbole mis en évidence s'est joué", 1, jumped);
        assertFalse("la carte se range après le saut", card.isShown());
        // la flèche monte pareillement ; la carte rangée ne prend
        // plus rien
        card.open(document());
        assertTrue("la flèche monte se prend", card.onArrow(-1));
        card.close();
        assertFalse("la carte rangée ne prend pas la lettre", card.onText("x"));
        assertFalse("la carte rangée ne prend pas la flèche", card.onArrow(1));
        assertFalse("la carte rangée ne prend pas la validation", card.onEnter());
        assertFalse("la carte rangée ne prend pas le retour arrière",
                card.onBackspace());
    }

    /** La pose, la croix et la rangée du toucher. */
    @Test
    public void thePoseTheCrossAndTheRow() {
        card.open(document());
        PopupMetrics metrics = new PopupMetrics(78.0f, 16.0f, 16.0f, 16.0f,
                1080.0f, 1920.0f, 60.0f, 20.0f, 1.0f, 120.0f, 500.0f);
        card.draw(new Canvas(), 480.0f, 540.0f, 2, metrics);
        assertTrue("la carte se pose dans les murs",
                card.poseX() >= 0.0f && card.poseX() + card.width() <= 1080.0f
                        && card.poseY() >= 540.0f);
        assertFalse("le doigt hors carte ne prend rien",
                card.onTouchArea(10.0f, 10.0f));
        // la croix (le coin haut droit de l'en-tête) : la carte se range
        assertTrue("le doigt sur la carte se consomme",
                card.onTouchArea(card.poseX() + card.width() - 20.0f,
                        card.poseY() + 12.0f));
        assertFalse("la croix range la carte", card.isShown());
        // la rangée s'arme au DOWN, se joue à l'UP (la navigation
        // part à la vue liée — le harnais réel l'y éprouve)
        card.open(document());
        card.draw(new Canvas(), 480.0f, 540.0f, 2, metrics);
        float rowY = card.rowCenterY(0);
        assertTrue("le doigt sur la rangée se consomme",
                card.onTouchArea(card.poseX() + 40.0f, rowY));
        assertTrue("le relâchement joue la rangée", card.onRelease());
        assertFalse("la carte se range après le choix", card.isShown());
    }
}
