package jo.pigma.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jo.pigma.editor.EditorView;
import jo.pigma.editor.lsp.LspEditor;
import jo.pigma.lsp.LspDiagnostic;
import jo.pigma.lsp.LspSession;
import jo.pigma.syntax.Languages;

/**
 * A2 : le document par onglet. L'écran ne montre plus UN document
 * sous une URI unique : chaque onglet porte son URI, son langage et
 * son état — le passage sauvegarde l'onglet qui s'en va, rend celui
 * qui arrive, et le serveur de langage SUIT (le document change côté
 * protocole : le serveur ne voit jamais le contenu d'un onglet sous
 * l'URI d'un autre, la liaison d'un autre langage se débranche).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class TabDocumentIT {

    private static final int VIEW_WIDTH = 1080;
    private static final int VIEW_HEIGHT = 1800;

    private Activity activity;
    private EditorView editor;
    private Bitmap surface;
    private Canvas canvas;
    private MainActivity screen;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        screen = (MainActivity) activity;
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
        editor.awaitBackgroundFill(20_000L);
        idle();
        frame();
    }

    /**
     * LE CHANGEMENT DE DOCUMENT côté serveur : la façade ouvre le
     * document neuf à son URI, au texte courant de la vue — l'avis du
     * serveur (les diagnostics du document de démonstration) y prouve
     * le passage : publié pour l'URI du document NEUF, au compte de
     * SES parenthèses ; le même document ne repart pas.
     */
    @Test
    public void theSwitchOpensTheNewDocumentAtTheServer() throws Exception {
        // une vue à part : la façade éprouvée n'est pas celle de
        // l'écran (le serveur y parle au document de la vue, pas à
        // celui de l'onglet)
        EditorView own = new EditorView(activity);
        own.measure(View.MeasureSpec.makeMeasureSpec(VIEW_WIDTH,
                View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(VIEW_HEIGHT,
                        View.MeasureSpec.EXACTLY));
        own.layout(0, 0, VIEW_WIDTH, VIEW_HEIGHT);
        List<String> published = new CopyOnWriteArrayList<>();
        List<LspDiagnostic> last = new CopyOnWriteArrayList<>();
        // le serveur GÉNÉRIQUE (aucune URI pré-ouvrerte : les documents
        // y vivent par didOpen)
        DemoLanguageServer server = new DemoLanguageServer(null);
        LspSession session = LspSession.beside(server,
                new LspSession.Listener() {
                    @Override
                    public void onStopped(boolean byServer) {
                        // la liaison de l'essai ne meurt pas
                    }

                    @Override
                    public void onDiagnostics(String uri,
                            List<LspDiagnostic> fresh) {
                        published.add(uri);
                        last.clear();
                        last.addAll(fresh);
                    }
                });
        LspEditor lsp = LspEditor.attach(own);
        own.setLanguage(Languages.JAVA, "class A {\n    int x = 1;\n}\n");
        frame();
        idle();
        lsp.connect(session, "file:///a.java", "java");
        assertTrue("la liaison vit (pré-requis)", await(10_000L, lsp::isConnected));
        idle();
        frame();
        idle();
        assertTrue("le premier document s'ouvre à son URI (pré-requis)",
                published.contains("file:///a.java"));

        // LE PASSAGE : le document B (deux parenthèses orphelines) prend
        // la vue, la façade change de document côté protocole
        own.setLanguage(Languages.JAVA, "class B {\n}\n)\n)\n");
        frame();
        idle();
        lsp.switchDocument("file:///b.java", "java");
        assertTrue("le document neuf s'ouvre à SON URI (le serveur y publie)",
                await(10_000L, () -> published.contains("file:///b.java")));
        idle();
        frame();
        idle();
        assertEquals("l'avis du serveur parle du texte NEUF (ses deux"
                + " parenthèses)", 1, last.size());

        // LE RETOUR : le document A (propre) revient, son avis se vide
        own.setLanguage(Languages.JAVA, "class A {\n    int x = 1;\n}\n");
        frame();
        idle();
        lsp.switchDocument("file:///a.java", "java");
        assertTrue("le document de retour s'ouvre à son URI",
                await(10_000L, () -> published.stream()
                        .filter("file:///a.java"::equals).count() >= 2));
        idle();
        frame();
        idle();
        assertTrue("l'avis du retour est propre (le texte de A)",
                last.isEmpty());

        // LE MÊME DOCUMENT ne repart pas (le repassage ne rouvre rien)
        int before = published.size();
        lsp.switchDocument("file:///a.java", "java");
        idle();
        frame();
        idle();
        assertEquals("le même document ne se rouvre pas", before,
                published.size());
    }

    /**
     * LES ONGLETS DE L'ÉCRAN portent leurs documents : l'état de
     * l'onglet qu'on quitte se garde (l'annulation), celui qui arrive
     * se rend ; le serveur de démonstration (Java seul) se débranche à
     * l'onglet d'un autre langage — aucun document non-Java ne lui
     * part.
     */
    @Test
    public void theTabsCarryTheirDocuments() {
        // le serveur de démonstration se branche (le bouton de l'écran)
        Button lspButton = findButton(
                activity.findViewById(android.R.id.content), "LSP");
        assertTrue("le bouton LSP vit (pré-requis)", lspButton != null);
        lspButton.performClick();
        idle();
        frame();
        idle();
        frame();
        idle();
        // une édition propre à l'onglet Java (son annulation voyagera)
        String javaText = editor.getBuffer().toString();
        InputConnection connection =
                editor.onCreateInputConnection(new EditorInfo());
        editor.setSelection(javaText.length(), javaText.length());
        connection.commitText(")", 1);
        frame();
        idle();
        String edited = editor.getBuffer().toString();
        assertTrue("l'édition vit (pré-requis)", !edited.equals(javaText));

        // L'ONGLET XML : son document arrive, son état (vierge) se rend
        screen.harnessSelectTab(1);
        idle();
        frame();
        idle();
        assertEquals("le document XML prend la vue", DemoTexts.XML,
                editor.getBuffer().toString());
        assertEquals("l'URI de l'onglet vit côté hôte",
                "file:///demo.xml", editor.hostDocument());
        // le serveur de démonstration (Java) s'est débranché : aucun
        // document XML ne lui part
        Button after = findButton(
                activity.findViewById(android.R.id.content), "LSP");
        assertTrue("le serveur Java ne vit plus à l'onglet XML",
                after != null && "LSP".contentEquals(after.getText()));

        // LE RETOUR À JAVA : le document ET son annulation reviennent
        screen.harnessSelectTab(0);
        idle();
        frame();
        idle();
        assertEquals("le document Java revient", edited,
                editor.getBuffer().toString());
        editor.getUndoStack().undo();
        frame();
        assertEquals("l'annulation de l'onglet Java a survécu au voyage",
                javaText, editor.getBuffer().toString());
    }

    private void frame() {
        surface.eraseColor(0);
        editor.draw(canvas);
    }

    /** Attend une condition bornée (les fils de lecture lsp4j y
     * terminent leur poignée de main — le looper ne les draine pas). */
    private static boolean await(long millis, java.util.function.BooleanSupplier condition) {
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

    private void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private static Button findButton(View root, String label) {
        if (root instanceof Button
                && label.contentEquals(((Button) root).getText())) {
            return (Button) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int at = 0; at < group.getChildCount(); at++) {
                Button found = findButton(group.getChildAt(at), label);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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
