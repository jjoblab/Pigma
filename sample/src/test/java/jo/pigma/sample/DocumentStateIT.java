package jo.pigma.sample;

import static org.junit.Assert.assertEquals;
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
 * A3 : l'état par document. L'hôte à onglets sauvegarde l'état d'un
 * onglet ({@code saveState()}) avant d'en ouvrir un autre, et le rend
 * au retour ({@code restoreState(...)}) — l'aller-retour A → B → A
 * rend à A son texte, son curseur, sa sélection, son défilement, ses
 * plis et SON annulation ; l'annulation de B n'est pas celle de A
 * (l'avant TOUT remettait à zéro à chaque changement : annulation,
 * position et plis perdais).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class DocumentStateIT {

    private static final int VIEW_WIDTH = 1080;
    private static final int VIEW_HEIGHT = 1800;

    private Activity activity;
    private EditorView editor;
    private InputConnection connection;
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
        connection = editor.onCreateInputConnection(new EditorInfo());
        surface = Bitmap.createBitmap(VIEW_WIDTH, VIEW_HEIGHT,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(surface);
        frame();
        idle();
        // le remplissage du fond se termine AVANT tout scénario (les
        // bornes des plis y dépendent : un lexage partiel n'y connaît
        // pas encore les fermantes)
        editor.awaitBackgroundFill(20_000L);
        idle();
        frame();
    }

    /**
     * L'ALLER-RETOUR A → B → A : le texte, le curseur, la sélection,
     * les plis et l'annulation de A sont intacts ; l'annulation de B
     * n'est pas celle de A.
     */
    @Test
    public void theRoundTripKeepsEverythingOfA() {
        // A : le document de démonstration Java, identifié par l'hôte
        editor.setHostDocument("file:///demo.java");
        String textA = editor.getBuffer().toString();
        // le pli d'une ligne (le tap à la gouttière replie)
        int foldLine = firstFoldLine();
        assertTrue("un pli existe dans la démo (pré-requis)", foldLine >= 0);
        tap(editor.getPaddingLeft() * 0.3f,
                lineTop(foldLine) + editor.lineHeight() / 2.0f);
        frame();
        assertTrue("la ligne se replie (pré-requis)",
                editor.hiddenLineCount() > 0);
        // l'annulation d'une édition propre à A
        editor.setSelection(textA.length(), textA.length());
        connection.commitText(" /* A */", 1);
        frame();
        String editedA = editor.getBuffer().toString();
        assertTrue("l'édition vit (pré-requis)", !editedA.equals(textA));
        // la sélection et le curseur propres à A — AVANT la région
        // repliée (l'invariant de la vue demeure au retour : un
        // curseur restauré DANS un pli l'ouvrirait, le pli ne peut
        // pas le cacher)
        editor.setSelection(10, 30);
        frame();
        EditorView.DocumentState stateA = editor.saveState();

        // B : un autre document, une autre annulation
        editor.setLanguage(Languages.XML, DemoTexts.XML);
        editor.setHostDocument("file:///demo.xml");
        frame();
        String textB = editor.getBuffer().toString();
        editor.setSelection(textB.length(), textB.length());
        connection.commitText(" <!-- B -->", 1);
        frame();
        assertTrue("l'édition de B vit (pré-requis)",
                !editor.getBuffer().toString().equals(textB));
        EditorView.DocumentState stateB = editor.saveState();

        // LE RETOUR À A : tout revient
        editor.restoreState(stateA);
        frame();
        assertEquals("le texte de A revient", editedA,
                editor.getBuffer().toString());
        assertEquals("le curseur de A revient",
                Math.min(stateA.caret(), editedA.length()),
                editor.getCaretOffset());
        assertEquals("la sélection de A revient", 10,
                editor.selectionStart());
        assertEquals("la sélection de A revient (borne haute)", 30,
                editor.selectionEnd());
        assertTrue("les plis de A reviennent",
                editor.hiddenLineCount() > 0);
        assertEquals("l'identité de l'hôte revient",
                "file:///demo.java", editor.hostDocument());
        // L'ANNULATION DE A n'est pas celle de B : défaire y retire
        // l'édition de A (le commentaire), pas celle de B
        editor.getUndoStack().undo();
        frame();
        assertEquals("l'annulation de A défait l'édition de A", textA,
                editor.getBuffer().toString());
        // et le rétablissement la ramène
        editor.getUndoStack().redo();
        frame();
        assertEquals("le rétablissement ramène l'édition de A", editedA,
                editor.getBuffer().toString());

        // LE RETOUR À B : l'annulation de B n'est pas celle de A
        editor.restoreState(stateB);
        frame();
        assertEquals("le texte de B revient", textB + " <!-- B -->",
                editor.getBuffer().toString());
        assertEquals("l'identité de B revient", "file:///demo.xml",
                editor.hostDocument());
        editor.getUndoStack().undo();
        frame();
        assertEquals("l'annulation de B défait l'édition de B", textB,
                editor.getBuffer().toString());
    }

    /**
     * LE DÉFILEMENT revient aussi : le gros fichier défile, la
     * position sauvegardée est la vérité du voyage.
     */
    @Test
    public void theRoundTripKeepsTheScroll() {
        String huge = DemoTexts.hugeFile("JAVA");
        editor.setLanguage(Languages.JAVA, huge);
        frame();
        editor.awaitBackgroundFill(20_000L);
        // le curseur à la fin : la fenêtre suit (le défilement vit)
        editor.caretTo(editor.getBuffer().length());
        frame();
        float scrolled = editor.scrollY();
        assertTrue("le gros fichier défile (pré-requis)", scrolled > 0.0f);
        EditorView.DocumentState stateA = editor.saveState();

        editor.setLanguage(Languages.XML, DemoTexts.XML);
        frame();
        assertEquals("le changement de document ramène le défilement à zéro",
                0.0f, editor.scrollY(), 0.01f);

        editor.restoreState(stateA);
        frame();
        assertEquals("le défilement de A revient", scrolled,
                editor.scrollY(), 0.01f);
    }

    /** Le premier en-tête de pli du document (au document de structure). */
    private int firstFoldLine() {
        for (int line = 0; line < editor.getBuffer().getLineCount(); line++) {
            if (editor.getStructureDocument().foldStartsAt(line)) {
                return line;
            }
        }
        return -1;
    }

    private float lineTop(int line) {
        return editor.getPaddingTop() + line * editor.lineHeight();
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

    private void frame() {
        surface.eraseColor(0);
        editor.draw(canvas);
    }

    private void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
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
