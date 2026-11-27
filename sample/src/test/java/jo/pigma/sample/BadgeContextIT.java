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
import jo.pigma.syntax.Languages;

/**
 * A1 : la pastille du diagnostic ne survit pas au changement de
 * document. Toucher un soulignement la fait naître (trois secondes de
 * vie) ; l'ouverture d'un AUTRE document — ou la RÉOUVERTURE du même
 * — la range : elle n'y est NI DESSINÉE ni TAPABLE (l'avant la
 * laissait vivre dans le texte neuf, ancrée à la ligne d'un autre
 * document : le tap y ouvrait la carte de CETTE ligne-là, parlant du
 * document d'avant).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class BadgeContextIT {

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

    /**
     * LA RÉOUVERTURE DU MÊME DOCUMENT range la pastille : la carte
     * d'avant meurt, la sélection se range, le défilement repart — la
     * pastille d'avant aussi.
     */
    @Test
    public void theBadgeDiesAtReopenOfSameDocument() {
        float[] badge = birthTheBadge();
        // LE MÊME TEXTE AU MÊME LANGAGE : la réouverture ne porte que
        // l'état de la vue — la pastille s'y range
        editor.setLanguage(Languages.JAVA, editor.getBuffer().toString());
        frame();
        check("la réouverture range la pastille (elle ne se dessine plus)",
                editor.diagnosticBadgeCenterY() == Float.MAX_VALUE);
        tap(badge[0], badge[1]);
        frame();
        check("la pastille morte n'ouvre plus la carte",
                editor.diagnosticCard() == null
                        || !editor.diagnosticCard().isShown());
        assertTrue(failures + " échec(s)", failures == 0);
    }

    /**
     * LE CHANGEMENT DE DOCUMENT range la pastille : elle n'y est ni
     * dessinée ni tapable — le tap à son ancienne place dans le
     * document neuf n'ouvre AUCUNE carte.
     */
    @Test
    public void theBadgeDiesAtDocumentChange() {
        birthTheBadge();
        editor.setLanguage(Languages.XML, DemoTexts.XML);
        frame();
        check("la pastille ne se dessine plus dans le document neuf",
                editor.diagnosticBadgeCenterY() == Float.MAX_VALUE);
        // la pastille morte ne se tape pas : son ancienne place
        // n'ouvre aucune carte de diagnostic
        int caret = editor.getCaretOffset();
        int line = editor.getBuffer().lineOfOffset(caret);
        float x = editor.getPaddingLeft() + 3.0f * editor.charWidth();
        float y = editor.getPaddingTop()
                + (line + 0.5f) * editor.lineHeight();
        tap(x, y);
        frame();
        check("aucune carte ne s'ouvre au tap dans le document neuf",
                editor.diagnosticCard() == null
                        || !editor.diagnosticCard().isShown());
        assertTrue(failures + " échec(s)", failures == 0);
    }

    /**
     * L'ÉDITION range la pastille pareillement : l'ancre ne désigne
     * plus son mot quand le texte bouge.
     */
    @Test
    public void theBadgeDiesAtEdit() {
        birthTheBadge();
        String text = editor.getBuffer().toString();
        editor.setSelection(text.length() / 2, text.length() / 2);
        connection.commitText("x", 1);
        frame();
        check("l'édition range la pastille",
                editor.diagnosticBadgeCenterY() == Float.MAX_VALUE);
        assertTrue(failures + " échec(s)", failures == 0);
    }

    /**
     * Fait naître la pastille : la parenthèse orpheline commise en fin
     * de texte porte le diagnostic local, le tap sur son soulignement
     * pose la pastille au-dessus du mot.
     *
     * @return la position de la pastille née {x, y}
     */
    private float[] birthTheBadge() {
        String original = editor.getBuffer().toString();
        editor.setSelection(original.length(), original.length());
        connection.commitText(")", 1);
        pause();
        frame();
        idle();
        int caret = editor.getCaretOffset();
        int line = editor.getBuffer().lineOfOffset(caret);
        // le caractère diagnostiqué vit à caret-1 (le curseur s'est
        // posé APRÈS le « ) » commis) : le doigt vise SA colonne
        int signOffset = caret - 1;
        float x = editor.getPaddingLeft()
                + (signOffset - editor.getBuffer().getLineStart(line))
                        * editor.charWidth();
        float y = editor.getPaddingTop()
                + (line + 0.5f) * editor.lineHeight();
        tap(x, y);
        frame();
        check("le tap sur le mot diagnostiqué pose la pastille (pré-requis)",
                editor.diagnosticBadgeCenterY() != Float.MAX_VALUE);
        return new float[] {
                editor.diagnosticBadgeCenterX(),
                editor.diagnosticBadgeCenterY() };
    }

    private void tap(float x, float y) {
        editor.dispatchTouchEvent(MotionEvent.obtain(
                android.os.SystemClock.uptimeMillis(),
                android.os.SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN, x, y, 0));
        editor.dispatchTouchEvent(MotionEvent.obtain(
                android.os.SystemClock.uptimeMillis(),
                android.os.SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP, x, y, 0));
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
