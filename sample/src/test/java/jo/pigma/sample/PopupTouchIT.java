package jo.pigma.sample;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import jo.pigma.editor.EditorView;

/**
 * Harnais d'enquête : le doigt RÉEL ne pose pas un DOWN et un UP à
 * la même milliseconde — il vit 80 à 150 ms, et deux trames de
 * gigue le séparent presque toujours du relâchement. Chaque scenario
 * ci-dessous éprouve une carte avec CE geste réel, à la recherche du
 * défaut signalé au terrain : « j'appuie sur les options, c'est
 * dismiss direct ».
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class PopupTouchIT {

    private static final int VIEW_WIDTH = 1080;
    private static final int VIEW_HEIGHT = 1800;

    private Activity activity;
    private EditorView editor;
    private InputConnection connection;
    private Bitmap surface;
    private Canvas canvas;
    private int failures;

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
        connection = editor.onCreateInputConnection(new EditorInfo());
        surface = Bitmap.createBitmap(VIEW_WIDTH, VIEW_HEIGHT,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(surface);
        frame();
        idle();
        frame();
        idle();
    }

    @Test
    public void pressWithJitterPlaysTheOption() {
        // une parenthèse ORPHELINE LIGNE 0 : le diagnostic vit loin
        // du caret (ligne 17) — le choix doit Y SAUTER, visible
        String original = editor.getBuffer().toString();
        editor.setSelection(0, 0);
        connection.commitText(")", 1);
        editor.caretTo(original.length());
        pause();
        frame();
        idle();
        // LA LISTE DES PROBLÈMES : le doigt réel (DOWN + 2 trames de
        // gigue + UP à +96 ms) doit JOUER la rangée visée
        editor.showProblemsPopup();
        frame();
        assertTrue("la liste des problèmes vit",
                editor.problemsCard() != null && editor.problemsCard().isShown());
        int problemLine = editor.problemsCard().problems().get(0).line();
        float rowY = editor.problemsCard().rowCenterY(1);
        System.out.println("rangée du problème à y=" + rowY
                + ", ligne visée=" + problemLine
                + ", caret avant=" + editor.caretLine());
        press(editor.problemsCard().poseX() + 40.0f, rowY);
        idle();
        frame();
        idle();
        frame();
        System.out.println("caret après=" + editor.caretLine()
                + ", carte vivante=" + editor.problemsCard().isShown());
        check("le choix du problème navigue au doigt réel",
                editor.caretLine() == problemLine);
        assertTrue(failures + " échec(s)", failures == 0);
    }

    @Test
    public void dragInsideCardDoesNotPlayTheRow() {
        editor.setSelection(0, 0);
        connection.commitText(")", 1);
        editor.caretTo(editor.getBuffer().length());
        pause();
        frame();
        idle();
        editor.showProblemsPopup();
        frame();
        assertTrue(editor.problemsCard() != null
                && editor.problemsCard().isShown());
        int before = editor.caretLine();
        // le doigt QUI TIRE (essai de défilement) : 60 px vers le bas
        // puis relâché — la rangée d'origine NE DOIT PAS se jouer
        drag(editor.problemsCard().poseX() + 40.0f,
                editor.problemsCard().rowCenterY(1), 60.0f);
        idle();
        frame();
        idle();
        System.out.println("drag : caret " + before + " -> " + editor.caretLine()
                + ", carte vivante=" + editor.problemsCard().isShown());
        check("le tir dans la carte ne joue pas la rangée",
                editor.caretLine() == before);
        assertTrue(failures + " échec(s)", failures == 0);
    }

    @Test
    public void searchFieldTakesTheKeysOverCompletion() {
        // la complétion vit (le mot tapé), la recherche s'ouvre par-
        // dessus : la frappe va à la RECHERCHE seule
        String original = editor.getBuffer().toString();
        editor.caretTo(original.length());
        frame();
        connection.commitText("grai", 1);
        idle();
        frame();
        idle();
        boolean completionShown = editor.completionCard() != null
                && editor.completionCard().isShown();
        System.out.println("complétion vivante avant la recherche : "
                + completionShown);
        editor.toggleSearchPopup();
        frame();
        idle();
        frame();
        System.out.println("complétion vivante après l'ouverture de la recherche : "
                + (editor.completionCard() != null
                        && editor.completionCard().isShown()));
        assertTrue("la complétion se range à l'ouverture de la recherche",
                editor.completionCard() == null
                        || !editor.completionCard().isShown());
        connection.commitText("ne", 1);
        idle();
        frame();
        System.out.println("requête : "
                + (editor.searchBar() == null ? "(null)"
                        : editor.searchBar().queryText())
                + ", tampon finit par : ..."
                + editor.getBuffer().toString().substring(
                        Math.max(0, editor.getBuffer().length() - 20)));
        assertTrue("la requête porte la frappe",
                editor.searchBar() != null
                        && editor.searchBar().queryText().contains("ne"));
        assertTrue("le tampon ne prend pas la frappe",
                editor.getBuffer().length() == original.length() + 4);
    }

    @Test
    public void dragScrollsTheCardList() {
        // HUIT orphelines sur huit lignes : la liste des problèmes
        // dépasse sa fenêtre — la traîne du doigt la DÉFILE (le retour
        // du terrain : « même celui avec Scroll je ne peux scroller »)
        editor.setSelection(0, 0);
        connection.commitText(")\n" .repeat(15), 1);
        pause();
        frame();
        idle();
        editor.showProblemsPopup();
        frame();
        assertTrue("la liste des problèmes vit",
                editor.problemsCard() != null
                        && editor.problemsCard().isShown());
        assertTrue("la liste dépasse sa fenêtre",
                editor.problemsCard().virtualRowCount() > 2);
        int topBefore = editor.problemsCard().topRow();
        // la traîne VERS LE HAUT (la façon de voir les rangées
        // d'après — le doigt tire la liste vers le haut)
        drag(editor.problemsCard().poseX() + 40.0f,
                editor.problemsCard().rowCenterY(0), -220.0f);
        idle();
        frame();
        System.out.println("scroll : topRow " + topBefore + " -> "
                + editor.problemsCard().topRow());
        check("la traîne défile la liste des problèmes",
                editor.problemsCard().topRow() > topBefore);
        assertTrue(failures + " échec(s)", failures == 0);
    }

    private void press(float x, float y) {
        long t = android.os.SystemClock.uptimeMillis();
        editor.dispatchTouchEvent(MotionEvent.obtain(t, t,
                MotionEvent.ACTION_DOWN, x, y, 0));
        editor.dispatchTouchEvent(MotionEvent.obtain(t + 16L, t + 16L,
                MotionEvent.ACTION_MOVE, x + 0.5f, y - 0.5f, 0));
        editor.dispatchTouchEvent(MotionEvent.obtain(t + 32L, t + 32L,
                MotionEvent.ACTION_MOVE, x + 1.0f, y + 1.0f, 0));
        editor.dispatchTouchEvent(MotionEvent.obtain(t + 96L, t + 96L,
                MotionEvent.ACTION_UP, x + 1.0f, y + 1.0f, 0));
    }

    private void drag(float x, float y, float span) {
        long t = android.os.SystemClock.uptimeMillis();
        editor.dispatchTouchEvent(MotionEvent.obtain(t, t,
                MotionEvent.ACTION_DOWN, x, y, 0));
        for (int step = 1; step <= 8; step++) {
            long at = t + step * 16L;
            editor.dispatchTouchEvent(MotionEvent.obtain(at, at,
                    MotionEvent.ACTION_MOVE, x, y + span * step / 8.0f, 0));
        }
        editor.dispatchTouchEvent(MotionEvent.obtain(t + 128L, t + 128L,
                MotionEvent.ACTION_UP, x, y + span, 0));
    }

    private void pause() {
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        idle();
    }

    private void frame() {
        surface.eraseColor(0);
        editor.draw(canvas);
    }

    private void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private void check(String what, boolean ok) {
        System.out.println((ok ? "  ok — " : "  ÉCHEC — ") + what);
        if (!ok) {
            failures++;
        }
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
