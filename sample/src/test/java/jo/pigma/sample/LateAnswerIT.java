package jo.pigma.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import jo.pigma.editor.EditorView;
import jo.pigma.editor.lsp.LspEditor;
import jo.pigma.syntax.Languages;

/**
 * A4 : les réponses tardives d'un ancien document n'ouvrent AUCUNE
 * carte. La question part au serveur, le document change sous la
 * façade (l'hôte ouvre un autre texte sans passer par le passage
 * d'onglet), la réponse arrive ENSUITE — elle se jette : le
 * formatage ne pose rien (les remplacements parlaient de l'ancien
 * texte — les poser corromprait le neuf), la carte de renommage ne
 * s'ouvre pas.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class LateAnswerIT {

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
     * LE FORMATAGE TARDIF ne se pose pas : la réponse du document
     * d'avant arrive après le changement — la poser corromprait le
     * texte neuf (ses plages parlent de l'ancien).
     */
    @Test
    public void theLateFormattingDoesNotTouchTheNewDocument()
            throws Exception {
        LspEditor lsp = LspEditor.attach(editor);
        // le document d'avant : des espaces de fin (le formatage les
        // coupe — l'essai du harnais)
        editor.setLanguage(Languages.JAVA,
                "class A {\n    int x = 1;   \n}\n");
        frame();
        lsp.connectInProcess(new DemoLanguageServer(null),
                "file:///a.java", "java");
        assertTrue("la liaison vit (pré-requis)",
                await(10_000L, lsp::isConnected));
        idle();
        frame();
        idle();
        assertEquals("le document d'avant vit (pré-requis)", 30,
                editor.getBuffer().length());

        // LA QUESTION PART, le document change SOUS la façade, la
        // réponse arrive ensuite (le looper la porte APRÈS le
        // changement) — elle se jette
        lsp.formatDocument();
        editor.setLanguage(Languages.JAVA, "class B {\n}\n");
        frame();
        idle();
        frame();
        idle();
        try {
            Thread.sleep(200L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        idle();
        frame();
        idle();
        assertEquals("le formatage tardif ne touche pas le document neuf",
                "class B {\n}\n", editor.getBuffer().toString());

        // LE CONTRÔLE : la même question SANS changement pose ses
        // remplacements (la garde ne mange pas les réponses vivantes)
        editor.setLanguage(Languages.JAVA,
                "class C {\n    int y = 2;   \n}\n");
        frame();
        idle();
        // la pompe de synchronisation pousse le texte neuf au serveur
        // (l'envoi est amorti — le sommeil le laisse passer)
        try {
            Thread.sleep(400L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        idle();
        frame();
        lsp.formatDocument();
        try {
            Thread.sleep(200L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        idle();
        frame();
        idle();
        assertEquals("le formatage vivant coupe les espaces de fin (contrôle)",
                "class C {\n    int y = 2;\n}\n",
                editor.getBuffer().toString());
    }

    /**
     * LA CARTE DE RENOMMAGE TARDIVE ne s'ouvre pas : la réponse du
     * document d'avant arrive après le changement — la carte parlait
     * de l'ancien texte.
     */
    @Test
    public void theLateRenameCardDoesNotOpen() throws Exception {
        LspEditor lsp = LspEditor.attach(editor);
        editor.setLanguage(Languages.JAVA,
                "class A {\n    int graine = 1;\n}\n");
        frame();
        lsp.connectInProcess(new DemoLanguageServer(null),
                "file:///a.java", "java");
        assertTrue("la liaison vit (pré-requis)",
                await(10_000L, lsp::isConnected));
        idle();
        frame();
        idle();
        // le curseur sur le symbole « graine » (sa déclaration)
        int graine = editor.getBuffer().toString().indexOf("graine");
        editor.caretTo(graine + 1);
        frame();

        // LA QUESTION PART (le prepareRename ouvre la carte à sa
        // réponse), le document change, la réponse arrive ensuite —
        // aucune carte ne s'ouvre
        lsp.requestRename();
        editor.setLanguage(Languages.JAVA, "class B {\n}\n");
        frame();
        idle();
        frame();
        idle();
        try {
            Thread.sleep(200L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        idle();
        frame();
        idle();
        assertTrue("la carte de renommage ne s'ouvre pas du document d'avant",
                editor.renameBar() == null || !editor.renameBar().isShown());
    }

    private void frame() {
        surface.eraseColor(0);
        editor.draw(canvas);
    }

    private void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    /** Attend une condition bornée (la poignée de main vit au fil de
     * lecture lsp4j — le looper ne la draine pas). */
    private static boolean await(long millis,
            java.util.function.BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(20L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return condition.getAsBoolean();
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
