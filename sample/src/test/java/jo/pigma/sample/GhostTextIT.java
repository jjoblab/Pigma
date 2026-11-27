package jo.pigma.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import jo.pigma.ai.AiCapabilities;
import jo.pigma.ai.AiResult;
import jo.pigma.ai.AiTask;
import jo.pigma.ai.ContextBuilder;
import jo.pigma.editor.EditorView;
import jo.pigma.editor.ai.AiAssistant;
import jo.pigma.editor.ai.GhostTextState;
import jo.pigma.syntax.Languages;

/**
 * B4 : le TEXTE FANTÔME au vrai cadre — la proposition se devine au
 * caret, JAMAIS écrite au tampon tant qu'elle n'est pas acceptée.
 * L'arme de l'hôte (désactivée par défaut), la frappe qui déclenche
 * la question (la temporisation), la réponse fidèle qui s'affiche,
 * l'ACCEPTATION au TAB (tout, UNE unité d'annulation) et au
 * balayage (un mot — le reste demeure), le REJET au balayage
 * contraire, la frappe qui reprend tout, la réponse périmée qui se
 * jette, les masques (la sélection, la composition IME).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class GhostTextIT {

    private static final int VIEW_WIDTH = 1080;
    private static final int VIEW_HEIGHT = 1800;

    private Activity activity;
    private EditorView editor;
    private Bitmap surface;
    private Canvas canvas;
    private AiAssistantIT.FakeBackend backend;
    private AiAssistant assistant;

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
        backend = new AiAssistantIT.FakeBackend();
        assistant = new AiAssistant(editor, backend,
                new ContextBuilder(null, null));
        assistant.setDocument("A.java", "java");
        // la temporisation raccourcie (le harnais n'attend pas 400 ms —
        // l'horloge de la machine reste la sienne, le temps réel passe)
        assistant.setGhostTimeoutMs(1L);
        frame();
        idle();
    }

    /**
     * Désactivé PAR DÉFAUT : la frappe ne déclenche RIEN tant que
     * l'hôte n'arme pas la complétion — puis la question part à la
     * temporisation (le préfixe et le suffixe au caret voyagent), la
     * réponse fidèle AFFICHE le fantôme (rien ne s'écrit au tampon).
     */
    @Test
    public void theGhostIsOffByDefaultThenFollowsTheTyping() {
        String base = "class A {\n    int total() {\n        return\n    }\n}\n";
        editor.setLanguage(Languages.JAVA, base);
        settle();
        backend.capabilities = new AiCapabilities(
                Set.of(AiTask.INLINE_COMPLETION), false);
        int caret = base.indexOf("return") + "return".length();
        editor.caretTo(caret);
        android.view.inputmethod.InputConnection connection =
                editor.onCreateInputConnection(
                        new android.view.inputmethod.EditorInfo());
        // SANS L'ARME DE L'HÔTE : la frappe ne déclenche rien
        connection.commitText(" 1", 1);
        passTimeout();
        assertEquals("désactivé par défaut : rien ne part",
                GhostTextState.Phase.INACTIVE, editor.ghostPhase());
        // L'ARME POSÉE : la frappe déclenche la question
        assistant.setInlineCompletionEnabled(true);
        connection.commitText(" +", 1);
        passTimeout();
        assertNotNull("la question part", backend.lastRequest);
        assertEquals("la tâche de complétion voyage",
                AiTask.INLINE_COMPLETION, backend.lastRequest.task());
        assertTrue("le préfixe voyage (la frappe y vit)",
                backend.lastRequest.prefix().endsWith("return 1 +"));
        // la réponse fidèle AFFICHE (rien ne s'écrit au tampon)
        int after = Math.min(editor.caretOffset(),
                editor.getBuffer().length());
        int lengthBefore = editor.getBuffer().length();
        backend.pending.get().onComplete(
                new AiResult.Insertion(after, " total;"));
        idle();
        assertEquals("le fantôme affiche sa proposition", " total;",
                editor.ghostText());
        assertEquals("rien ne s'écrit au tampon", lengthBefore,
                editor.getBuffer().length());
    }

    /**
     * L'ACCEPTATION au TAB : la proposition ENTIÈRE se pose par UNE
     * unité d'annulation — le geste du retour défait la pose
     * entière ; le fantôme meurt.
     */
    @Test
    public void theTabAcceptsAllAsOneUndoUnit() {
        String base = "class A {\n    int total() {\n        return\n    }\n}\n";
        editor.setLanguage(Languages.JAVA, base);
        settle();
        backend.capabilities = new AiCapabilities(
                Set.of(AiTask.INLINE_COMPLETION), false);
        assistant.setInlineCompletionEnabled(true);
        int caret = base.indexOf("return") + "return".length();
        editor.caretTo(caret);
        android.view.inputmethod.InputConnection connection =
                editor.onCreateInputConnection(
                        new android.view.inputmethod.EditorInfo());
        connection.commitText(" 1", 1);
        passTimeout();
        int after = Math.min(editor.caretOffset(),
                editor.getBuffer().length());
        backend.pending.get().onComplete(
                new AiResult.Insertion(after, " total;"));
        idle();
        assertFalse("le fantôme vit", editor.ghostText().isEmpty());
        // LE TAB accepte TOUT
        assertTrue("le tab accepte la proposition",
                connection.sendKeyEvent(new KeyEvent(
                        KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB)));
        idle();
        assertEquals("la proposition entière se pose",
                base.replace("return\n", "return 1 total;\n"),
                editor.getBuffer().toString());
        assertEquals("le fantôme meurt", "", editor.ghostText());
        editor.undo();
        assertEquals("la pose vaut UNE unité d'annulation",
                base.replace("return\n", "return 1\n"),
                editor.getBuffer().toString());
    }

    /**
     * Les BALAYAGES décident : le court balayage à droite pose LE MOT
     * suivant (le reste demeure au caret, chaque mot SA pose), le
     * balayage ample à droite pose TOUT, le balayage à gauche
     * REJETE (le tampon n'a jamais bougé).
     */
    @Test
    public void theSwipesDecideWordAllAndReject() {
        String base = "class A {\n    int total() {\n        return\n    }\n}\n";
        editor.setLanguage(Languages.JAVA, base);
        settle();
        backend.capabilities = new AiCapabilities(
                Set.of(AiTask.INLINE_COMPLETION), false);
        assistant.setInlineCompletionEnabled(true);
        int caret = base.indexOf("return") + "return".length();
        editor.caretTo(caret);
        android.view.inputmethod.InputConnection connection =
                editor.onCreateInputConnection(
                        new android.view.inputmethod.EditorInfo());
        connection.commitText(" 1", 1);
        passTimeout();
        int after = Math.min(editor.caretOffset(),
                editor.getBuffer().length());
        backend.pending.get().onComplete(
                new AiResult.Insertion(after, " total; // fini"));
        idle();
        frame();
        assertFalse("le fantôme vit", editor.ghostText().isEmpty());
        // LE BALAYAGE COURT à droite : LE MOT suivant
        swipe(500, 900, 560, 905);
        idle();
        assertTrue("le premier mot se posa", editor.getBuffer().toString()
                .contains("return 1 total;"));
        assertEquals("le reste demeure au caret", "// fini",
                editor.ghostText());
        // LE BALAYAGE AMPLE à droite : TOUT
        swipe(500, 900, 760, 905);
        idle();
        assertTrue("le reste se posa", editor.getBuffer().toString()
                .contains("return 1 total; // fini"));
        assertEquals("le fantôme épuisé meurt", "", editor.ghostText());
        // LE BALAYAGE À GAUCHE : REJET (le tampon n'a jamais bougé)
        connection.commitText(" x", 1);
        passTimeout();
        int before = Math.min(editor.caretOffset(),
                editor.getBuffer().length());
        backend.pending.get().onComplete(
                new AiResult.Insertion(before, " perdu"));
        idle();
        frame();
        assertFalse("le fantôme revit", editor.ghostText().isEmpty());
        int lengthAtReject = editor.getBuffer().length();
        swipe(500, 900, 400, 905);
        idle();
        assertEquals("le rejet ne touche pas le tampon", lengthAtReject,
                editor.getBuffer().length());
        assertEquals("le fantôme rejeté meurt", "", editor.ghostText());
    }

    /**
     * Les MASQUES obligatoires : la frappe reprend tout (le fantôme
     * meurt, l'attente se réarme), une sélection vivante le masque,
     * une composition IME le masque — la proposition parlait du
     * texte d'avant.
     */
    @Test
    public void theMandatoryMasks() {
        String base = "class A {\n    int total() {\n        return\n    }\n}\n";
        editor.setLanguage(Languages.JAVA, base);
        settle();
        backend.capabilities = new AiCapabilities(
                Set.of(AiTask.INLINE_COMPLETION), false);
        assistant.setInlineCompletionEnabled(true);
        int caret = base.indexOf("return") + "return".length();
        editor.caretTo(caret);
        android.view.inputmethod.InputConnection connection =
                editor.onCreateInputConnection(
                        new android.view.inputmethod.EditorInfo());
        connection.commitText(" 1", 1);
        passTimeout();
        int after = Math.min(editor.caretOffset(),
                editor.getBuffer().length());
        backend.pending.get().onComplete(
                new AiResult.Insertion(after, " total;"));
        idle();
        frame();
        assertFalse("le fantôme vit", editor.ghostText().isEmpty());
        // LA FRAPPE reprend tout : le fantôme meurt (l'attente se réarme)
        connection.commitText(" 2", 1);
        idle();
        assertEquals("le fantôme meurt à la frappe", "",
                editor.ghostText());
        passTimeout();
        backend.pending.get().onComplete(
                new AiResult.Insertion(Math.min(editor.caretOffset(),
                        editor.getBuffer().length()), " trois"));
        idle();
        frame();
        assertFalse("le fantôme revit après l'attente",
                editor.ghostText().isEmpty());
        // LA SÉLECTION vivante le masque (la trame l'éprouve)
        int start = editor.getBuffer().toString().indexOf("return");
        editor.setSelection(start, start + 6);
        idle();
        frame();
        assertEquals("la sélection masque le fantôme", "",
                editor.ghostText());
        // LA COMPOSITION IME masque même la QUESTION : rien ne part (le
        // dernier vol demeure le dernier — aucun vol neuf ne naît)
        Object lastFlight = backend.pending.get();
        editor.caretTo(Math.min(editor.caretOffset(),
                editor.getBuffer().length()));
        connection.setComposingText("z", 1);
        passTimeout();
        assertSame("la composition masque même la question", lastFlight,
                backend.pending.get());
        connection.finishComposingText();
        idle();
    }

    /**
     * La réponse PÉRIMÉE se jette : une insertion qui vise une autre
     * position que le caret du départ ne s'affiche JAMAIS (la poser
     * serait un mensonge à l'écran).
     */
    @Test
    public void theStaleInsertionIsDropped() {
        String base = "class A {\n}\n";
        editor.setLanguage(Languages.JAVA, base);
        settle();
        backend.capabilities = new AiCapabilities(
                Set.of(AiTask.INLINE_COMPLETION), false);
        assistant.setInlineCompletionEnabled(true);
        editor.caretTo(base.length() - 1);
        android.view.inputmethod.InputConnection connection =
                editor.onCreateInputConnection(
                        new android.view.inputmethod.EditorInfo());
        connection.commitText("x", 1);
        passTimeout();
        int caret = Math.min(editor.caretOffset(),
                editor.getBuffer().length());
        // la réponse vise une AUTRE position : elle se jette
        backend.pending.get().onComplete(
                new AiResult.Insertion(caret + 5, " menteur"));
        idle();
        assertEquals("la réponse infidèle ne s'affiche pas", "",
                editor.ghostText());
    }

    /** La temporisation passe (le temps réel avance pour l'horloge de
     * la machine, la file se vide JUSQU'AU BOUT — l'échéance différée
     * y compris). */
    private void passTimeout() {
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        idle();
    }

    /** LE BALAYAGE : le doigt tombe, glisse, se relève (la vraie
     * route du toucher). */
    private void swipe(float fromX, float fromY, float toX, float toY) {
        editor.dispatchTouchEvent(MotionEvent.obtain(
                android.os.SystemClock.uptimeMillis(),
                android.os.SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN, fromX, fromY, 0));
        editor.dispatchTouchEvent(MotionEvent.obtain(
                android.os.SystemClock.uptimeMillis(),
                android.os.SystemClock.uptimeMillis() + 20L,
                MotionEvent.ACTION_MOVE, toX, toY, 0));
        editor.dispatchTouchEvent(MotionEvent.obtain(
                android.os.SystemClock.uptimeMillis(),
                android.os.SystemClock.uptimeMillis() + 40L,
                MotionEvent.ACTION_UP, toX, toY, 0));
    }

    private void settle() {
        frame();
        assertTrue("le remplissage de fond finit",
                editor.awaitBackgroundFill(20_000L));
        idle();
        frame();
        idle();
        frame();
        idle();
    }

    private void frame() {
        surface.eraseColor(0);
        editor.draw(canvas);
    }

    private void idle() {
        // la file se vide JUSQU'AU BOUT : les échéances différées y
        // courent (la temporisation du fantôme se pose à la file) —
        // l'horloge de la machine, elle, lit le temps RÉEL (le sommeil
        // du harnais l'a fait passer)
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
