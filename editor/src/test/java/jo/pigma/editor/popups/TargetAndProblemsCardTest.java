package jo.pigma.editor.popups;

import java.util.List;

import android.graphics.Canvas;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Le CHOIX DE CIBLES et la LISTE DES PROBLÈMES aux JVM (sans canevas
 * réel) : l'état de vie, la pose aux murs, la croix qui range, la
 * rangée armée au DOWN qui se joue à l'UP — les familles de
 * problèmes s'y GROUPENT (les erreurs d'abord, l'étiquette coiffe).
 */
public class TargetAndProblemsCardTest {

    /** La carte de cibles éprouvée, à la densité unité. */
    private final TargetCard targets = new TargetCard(1.0f);

    /** La carte de problèmes éprouvée, à la densité unité. */
    private final ProblemsCard problems = new ProblemsCard(1.0f);

    /** La géométrie d'essai. */
    private final PopupMetrics metrics = new PopupMetrics(78.0f, 16.0f,
            16.0f, 16.0f, 1080.0f, 1920.0f, 60.0f, 20.0f, 1.0f, 120.0f, 500.0f);

    /** L'état de vie de la carte de cibles. */
    @Test
    public void theTargetCardLife() {
        assertFalse("la carte naît rangée", targets.isShown());
        targets.open("Définitions de « graine »", List.of(
                new TargetCard.Target("graine", "", 3, "la ligne de la cible", () -> { }),
                new TargetCard.Target("graine", "Autre.java", 8, "l'extrait", () -> { })));
        assertTrue("la carte ouvre", targets.isShown());
        assertEquals("le titre se lit", "Définitions de « graine »",
                targets.title());
        assertEquals("les cibles se lisent", 2, targets.targets().size());
        targets.close();
        assertFalse("la fermeture range la carte", targets.isShown());
        assertTrue("les cibles se ramassent", targets.targets().isEmpty());
        targets.open("Rien", List.of());
        assertFalse("la liste vide n'ouvre rien", targets.isShown());
    }

    /** La pose, la croix et la rangée de la carte de cibles. */
    @Test
    public void theTargetCardTouch() {
        targets.open("Définitions de « graine »", List.of(
                new TargetCard.Target("graine", "", 3, "l'extrait", () -> { }),
                new TargetCard.Target("graine", "Autre.java", 8, "l'extrait", () -> { })));
        targets.draw(new Canvas(), 480.0f, 540.0f, metrics);
        assertTrue("la carte se pose dans les murs (dessous l'ancre)",
                targets.poseX() >= 0.0f
                        && targets.poseX() + targets.width() <= 1080.0f
                        && targets.poseY() >= 540.0f);
        assertFalse("le doigt hors carte ne prend rien",
                targets.onTouchArea(10.0f, 10.0f));
        // la croix : la carte se range
        assertTrue("le doigt sur la carte se consomme",
                targets.onTouchArea(targets.poseX() + targets.width() - 20.0f,
                        targets.poseY() + 12.0f));
        assertFalse("la croix range la carte", targets.isShown());
        // la rangée s'arme au DOWN, se joue à l'UP
        targets.open("Définitions de « graine »", List.of(
                new TargetCard.Target("graine", "", 3, "l'extrait", () -> { })));
        targets.draw(new Canvas(), 480.0f, 540.0f, metrics);
        float rowY = targets.rowCenterY(0);
        assertTrue("le doigt sur la rangée se consomme",
                targets.onTouchArea(targets.poseX() + 40.0f, rowY));
        assertTrue("le relâchement joue la rangée", targets.onRelease());
        assertFalse("la carte se range après le choix", targets.isShown());
    }

    /**
     * Les familles de problèmes se GROUPENT : les erreurs d'abord,
     * les avertissements ensuite — l'étiquette coiffe sa famille,
     * la rangée d'une étiquette n'arme rien.
     */
    @Test
    public void theProblemFamiliesGroup() {
        assertFalse("la carte naît rangée", problems.isShown());
        problems.open(List.of(
                new ProblemsCard.Problem(DiagnosticCard.SEVERITY_WARNING,
                        "attention", 5, () -> { }),
                new ProblemsCard.Problem(DiagnosticCard.SEVERITY_ERROR,
                        "oups", 2, () -> { }),
                new ProblemsCard.Problem(DiagnosticCard.SEVERITY_ERROR,
                        "encore", 7, () -> { })));
        assertTrue("la carte ouvre", problems.isShown());
        List<ProblemsCard.Problem> listed = problems.problems();
        assertEquals("les problèmes se lisent (les étiquettes exclues)", 3,
                listed.size());
        assertEquals("les erreurs d'abord", DiagnosticCard.SEVERITY_ERROR,
                listed.get(0).severity());
        assertEquals("l'avertissement ensuite", DiagnosticCard.SEVERITY_WARNING,
                listed.get(2).severity());
        // la pose et le toucher : la croix range, la rangée s'arme et se joue
        problems.draw(new Canvas(), 480.0f, 540.0f, metrics);
        assertTrue("la carte se pose dans les murs",
                problems.poseX() >= 0.0f
                        && problems.poseX() + problems.width() <= 1080.0f);
        assertFalse("le doigt hors carte ne prend rien",
                problems.onTouchArea(10.0f, 10.0f));
        assertTrue("le doigt sur la carte se consomme",
                problems.onTouchArea(problems.poseX() + 40.0f,
                        problems.rowCenterY(1)));
        assertTrue("le relâchement joue la rangée", problems.onRelease());
        assertFalse("la carte se range après le choix", problems.isShown());
        problems.open(List.of());
        assertFalse("la liste vide n'ouvre rien", problems.isShown());
    }
}
