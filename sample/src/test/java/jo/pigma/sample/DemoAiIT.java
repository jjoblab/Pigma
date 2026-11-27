package jo.pigma.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import jo.pigma.editor.EditorView;
import jo.pigma.editor.popups.QuickActionCard;

/**
 * B6 : l'« IA de démonstration » du sample — ÉTEINTE PAR DÉFAUT
 * (aucune action d'IA ne se propose sans l'interrupteur, la preuve
 * du plan), allumée elle montre les fonctions (les entrées naissent
 * des tâches déclarées, la réponse déterministe arrive morceau par
 * morceau, la complétion en ligne se devine) et ÉPROUVE LES ÉTATS
 * D'ERREUR (indisponible, délai, refus, réponse muette) : chaque
 * panne dit SA phrase. Aucun réseau — tout se joue dans le processus.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class DemoAiIT {

    private static final int VIEW_WIDTH = 1080;
    private static final int VIEW_HEIGHT = 1800;

    private Activity activity;
    private EditorView editor;
    private Bitmap surface;
    private Canvas canvas;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        editor = findEditor(activity.findViewById(android.R.id.content));
        assertTrue(editor != null);
        editor.measure(
                View.MeasureSpec.makeMeasureSpec(VIEW_WIDTH,
                        View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(VIEW_HEIGHT,
                        View.MeasureSpec.EXACTLY));
        editor.layout(0, 0, VIEW_WIDTH, VIEW_HEIGHT);
        surface = Bitmap.createBitmap(VIEW_WIDTH, VIEW_HEIGHT,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(surface);
        frame();
        idle();
    }

    /**
     * ÉTEINTE PAR DÉFAUT : l'ampoule ne porte AUCUNE entrée d'IA —
     * l'interrupteur seul les fait naître (et les retire à
     * l'extinction).
     */
    @Test
    public void theDemoIsOffByDefaultThenFollowsTheSwitch() {
        settle();
        assertNull("éteinte par défaut : aucun moteur", ((MainActivity) activity)
                .harnessDemoBackend());
        assertTrue("aucune entrée d'IA sans l'interrupteur",
                labelOf(editor.quickActionsOfLine(0), "Expliquer") == null);
        // L'ALLUMAGE fait naître les entrées des tâches déclarées
        ((MainActivity) activity).harnessToggleDemoAi();
        assertNotNull("le moteur vit allumé", ((MainActivity) activity)
                .harnessDemoBackend());
        assertTrue("l'explication se propose allumée",
                labelOf(editor.quickActionsOfLine(0), "Expliquer") != null);
        assertTrue("la documentation se propose",
                labelOf(editor.quickActionsOfLine(0), "Documenter") != null);
        assertTrue("l'écriture du test se propose",
                labelOf(editor.quickActionsOfLine(0), "Écrire le test") != null);
        // L'EXTINCTION les retire (la preuve du « désactivé par défaut »)
        ((MainActivity) activity).harnessToggleDemoAi();
        assertNull("le moteur se range éteint", ((MainActivity) activity)
                .harnessDemoBackend());
        assertTrue("aucune entrée d'IA à l'extinction",
                labelOf(editor.quickActionsOfLine(0), "Expliquer") == null);
    }

    /**
     * La réponse déterministe arrive MORCEAU PAR MORCEAU : l'attente
     * vit d'abord, l'explication de démonstration la remplace (elle
     * parle du document même — rien d'aléatoire).
     */
    @Test
    public void theAnswerStreamsThenExplains() {
        settle();
        ((MainActivity) activity).harnessToggleDemoAi();
        FakeAiBackend backend = ((MainActivity) activity).harnessDemoBackend();
        backend.setDelayMs(1L);
        QuickActionCard.Entry explain = labelOf(
                editor.quickActionsOfLine(0), "Expliquer");
        assertNotNull("l'explication se propose", explain);
        explain.action().run();
        // l'attente vit AVANT la file (la carte naît au départ même du
        // vol — la réponse la remplacera à son arrivée)
        assertEquals("l'attente vit d'abord", "Réflexion…",
                editor.infoPopupText());
        drain();
        assertTrue("l'explication de démonstration arrive (elle parle du"
                + " document)", editor.infoPopupText() != null
                && editor.infoPopupText().contains("Explication (démo)")
                && editor.infoPopupText().contains("Java"));
    }

    /**
     * Les états d'erreur disent CHACUN SA phrase : le délai, le refus,
     * la réponse muette — et l'indisponible ne propose RIEN.
     */
    @Test
    public void theFailureModesSayTheirPhrases() {
        settle();
        ((MainActivity) activity).harnessToggleDemoAi();
        FakeAiBackend backend = ((MainActivity) activity).harnessDemoBackend();
        backend.setDelayMs(1L);
        // LE DÉLAI
        backend.setMode(FakeAiBackend.Mode.DELAI);
        run("Expliquer");
        assertEquals("la phrase du délai",
                "Le temps de réponse est écoulé.", editor.infoPopupText());
        // LE REFUS
        backend.setMode(FakeAiBackend.Mode.REFUS);
        run("Expliquer");
        assertEquals("la phrase du refus",
                "La demande fut refusée.", editor.infoPopupText());
        // LA RÉPONSE MUETTE
        backend.setMode(FakeAiBackend.Mode.MUETTE);
        run("Expliquer");
        assertEquals("la phrase du silence",
                "La réponse est vide.", editor.infoPopupText());
        // L'INDISPONIBLE ne propose rien
        backend.setMode(FakeAiBackend.Mode.INDISPONIBLE);
        assertTrue("l'indisponible ne propose rien",
                labelOf(editor.quickActionsOfLine(0), "Expliquer") == null);
        // le retour à la normale rend les entrées
        backend.setMode(FakeAiBackend.Mode.NORMALE);
        assertTrue("la normale rend les entrées",
                labelOf(editor.quickActionsOfLine(0), "Expliquer") != null);
    }

    /**
     * La complétion en ligne se devine au caret : la frappe déclenche
     * la question, la proposition de démonstration vit (jamais écrite
     * au tampon), le TAB la pose.
     */
    @Test
    public void theInlineCompletionOfTheDemoShows() {
        settle();
        ((MainActivity) activity).harnessToggleDemoAi();
        FakeAiBackend backend = ((MainActivity) activity).harnessDemoBackend();
        backend.setDelayMs(1L);
        ((MainActivity) activity).harnessDemoAssistant()
                .setGhostTimeoutMs(1L);
        editor.caretTo(editor.getBuffer().length());
        android.view.inputmethod.InputConnection connection =
                editor.onCreateInputConnection(
                        new android.view.inputmethod.EditorInfo());
        connection.commitText("//", 1);
        int length = editor.getBuffer().length();
        passTimeout();
        drain();
        assertEquals("la proposition de démonstration se devine",
                " // complété par la démo", editor.ghostText());
        assertEquals("rien ne s'écrit au tampon sans acceptation", length,
                editor.getBuffer().length());
        assertTrue("le TAB pose la proposition",
                connection.sendKeyEvent(new android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_TAB)));
        drain();
        assertTrue("la proposition se posa", editor.getBuffer().toString()
                .endsWith("// // complété par la démo"));
    }

    /** Lance une entrée de l'ampoule par son libellé. */
    private void run(String label) {
        QuickActionCard.Entry entry = labelOf(
                editor.quickActionsOfLine(0), label);
        assertNotNull("l'entrée " + label + " se propose", entry);
        entry.action().run();
        drain();
    }

    /** Le libellé d'une entrée de l'ampoule (null : absente). */
    private static QuickActionCard.Entry labelOf(
            List<QuickActionCard.Entry> entries, String label) {
        for (QuickActionCard.Entry entry : entries) {
            if (label.equals(entry.label())) {
                return entry;
            }
        }
        return null;
    }

    /** La file se vide jusqu'au bout (les échéances différées y
     * courent — le temps réel passe à côté). */
    private void drain() {
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        idle();
    }

    /** La temporisation du fantôme passe (le temps réel avance). */
    private void passTimeout() {
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        idle();
    }

    private void settle() {
        frame();
        assertTrue("le remplissage de fond finit",
                editor.awaitBackgroundFill(20_000L));
        idle();
        frame();
        idle();
    }

    private void frame() {
        surface.eraseColor(0);
        editor.draw(canvas);
    }

    private void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks();
    }

    private static EditorView findEditor(View root) {
        if (root instanceof EditorView) {
            return (EditorView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int at = 0; at < group.getChildCount(); at++) {
                EditorView found = findEditor(group.getChildAt(at));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
