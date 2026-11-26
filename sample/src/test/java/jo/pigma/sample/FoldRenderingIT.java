package jo.pigma.sample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.util.List;

import jo.pigma.editor.EditorView;
import jo.pigma.lsp.LspFoldingRange;

import static org.junit.Assert.assertTrue;

/**
 * Le rendu des plis à la trame réelle (mode graphique NATIF, à la
 * leçon de la capture du guide) : le retour de terrain sur les plis
 * — l'icône du pli OUVERT vivait au panneau (le correctif du chevron
 * bas l'avait tuée avec le sien : plus AUCUNE icône) et la coupe de
 * l'en-tête replié ne s'appliquait pas (le « &#123;…&#125; » se
 * peignait PAR-DESSUS l'ouvrant et sa suite).
 *
 * <p>Chaque fait s'y lit aux PIXELS : la gouttière du pli diffère de
 * la rangée nue (le chevron), l'en-tête replié ne montre rien au-delà
 * de sa coupe, le placeholder vit à son ouvrant — ou à la fin du
 * texte visible quand l'en-tête n'ouvre rien (le pli du serveur sans
 * ouvrant : la colonne −1 de l'ouvrant manquant le posait dans la
 * gouttière).</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class FoldRenderingIT {

    /** Largeur de l'éditeur éprouvé (pixels). */
    private static final int VIEW_WIDTH = 1080;

    /** Hauteur de l'éditeur éprouvé (pixels). */
    private static final int VIEW_HEIGHT = 1800;

    /** Le seuil d'encre d'une icône de pli (pixels distincts du panneau). */
    private static final int ICON_INK = 12;

    private Activity activity;
    private EditorView editor;
    private Bitmap surface;
    private Canvas canvas;

    /**
     * L'écran de démonstration se construit, la vue respire, la
     * première trame se paie.
     */
    private void arrive() {
        activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        ViewGroup root = (ViewGroup) activity.findViewById(android.R.id.content);
        editor = findEditor(root);
        assertTrue("la vue d'éditeur vit à l'écran", editor != null);
        editor.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(VIEW_WIDTH,
                        android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(VIEW_HEIGHT,
                        android.view.View.MeasureSpec.EXACTLY));
        editor.layout(0, 0, VIEW_WIDTH, VIEW_HEIGHT);
        editor.awaitBackgroundFill(20_000L);
        editor.setSelection(0, 0);
        surface = Bitmap.createBitmap(VIEW_WIDTH, VIEW_HEIGHT,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(surface);
        frame();
    }

    /** Dessine une trame fraîche sur le vrai canevas natif. */
    private void frame() {
        surface.eraseColor(0);
        editor.draw(canvas);
    }

    /**
     * Le premier pli d'en-tête à ouvrant de la démo (la classe Java —
     * la javadoc de tête n'ouvre rien localement).
     *
     * @return l'indice de la ligne d'en-tête, -1 si aucune
     */
    private int openerFoldLine() {
        for (int line = 0; line < editor.getBuffer().getLineCount(); line++) {
            String head = editor.getBuffer().getText(
                    editor.getBuffer().getLineStart(line),
                    editor.getBuffer().getLineEnd(line)).toString();
            if (editor.getStructureDocument().foldStartsAt(line)
                    && head.contains("{")) {
                return line;
            }
        }
        return -1;
    }

    /**
     * L'encre d'icône de pli d'une ligne : les pixels de la MOITIÉ
     * GAUCHE de la boîte du chevron — strictement AVANT le filet du
     * panneau et son ombre (le bord droit du chevron les chevauche :
     * le filet s'y dessine SANS le pli, il ne compte pas) — qui
     * diffèrent du panneau nu ; la référence se lit À LA MÊME RANGÉE,
     * dans la bande propre entre les numéros et le chevron.
     *
     * @param line la ligne éprouvée
     * @return le compte de pixels distincts du panneau
     */
    private int gutterInk(int line) {
        int x1 = editor.getPaddingLeft() - 16;
        int x2 = editor.getPaddingLeft() - 11;
        int reference = surface.getPixel(
                (int) (editor.getPaddingLeft() - 24.0f),
                (int) (lineTop(line) + editor.lineHeight() / 2.0f));
        int mid = (int) (lineTop(line) + editor.lineHeight()) - 11;
        int diffs = 0;
        for (int y = mid - 7; y <= mid + 7; y++) {
            for (int x = x1; x < x2; x++) {
                if (surface.getPixel(x, y) != reference) {
                    diffs++;
                }
            }
        }
        return diffs;
    }

    /**
     * L'encre d'une tranche de colonnes d'une ligne : les pixels qui
     * diffèrent du fond (aucun texte — le pli y coupe, le placeholder
     * y naît, la frappe y vit).
     *
     * @param line    la ligne éprouvée
     * @param fromCol la première colonne (incluse)
     * @param toCol   la dernière colonne (exclue)
     * @return le compte de pixels encrés
     */
    private int textInk(int line, int fromCol, int toCol) {
        int background = surface.getPixel(300, 600);
        // la fenêtre couvre LA boîte de la ligne : les glyphes y siège
        // au bas (la baseline vit au bord bas de la boîte) — sans
        // toucher les boîtes voisines
        int y1 = (int) (editor.getPaddingTop()
                + line * editor.lineHeight()) + 1;
        int y2 = (int) (editor.getPaddingTop()
                + line * editor.lineHeight()) + 22;
        int x1 = (int) (editor.getPaddingLeft()
                + fromCol * editor.charWidth());
        int x2 = (int) (editor.getPaddingLeft()
                + toCol * editor.charWidth());
        int ink = 0;
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                if (surface.getPixel(x, y) != background) {
                    ink++;
                }
            }
        }
        return ink;
    }

    /** Le haut d'une ligne (la géométrie réelle de la police). */
    private float lineTop(int line) {
        return editor.getPaddingTop() + line * editor.lineHeight();
    }

    /** Un contact court : pose et lève. */
    private void tap(float x, float y) {
        editor.dispatchTouchEvent(android.view.MotionEvent.obtain(
                android.os.SystemClock.uptimeMillis(),
                android.os.SystemClock.uptimeMillis(),
                android.view.MotionEvent.ACTION_DOWN, x, y, 0));
        editor.dispatchTouchEvent(android.view.MotionEvent.obtain(
                android.os.SystemClock.uptimeMillis(),
                android.os.SystemClock.uptimeMillis(),
                android.view.MotionEvent.ACTION_UP, x, y, 0));
    }

    /**
     * LE CHEVRON DU PLI OUVERT VIT AU PANNEAU (le retour de terrain :
     * le correctif du chevron bas l'avait supprimé avec
     * le sien — le pli ouvert ne portait plus AUCUNE icône, l'écran ne
     * montrait plus où plier). Le pli replié garde son carré au plus.
     */
    @Test
    public void foldIcons() {
        arrive();
        int foldLine = openerFoldLine();
        assertTrue("un pli à en-tête ouvrant existe dans la démo", foldLine >= 0);
        float y = lineTop(foldLine) + editor.lineHeight() / 2.0f;
        float gutterX = editor.getPaddingLeft() * 0.3f;
        // LE PLI OUVERT : son chevron haut teinte la boîte — le panneau
        // nu n'y vit pas (le filet pointillé ne l'atteint pas : il naît
        // au bas de la boîte)
        frame();
        int openInk = gutterInk(foldLine);
        assertTrue("le chevron du pli ouvert vit au panneau ("
                + openInk + " pixels)", openInk >= ICON_INK);
        // LE PLI REPLIÉ : son carré au plus y vit pareil
        tap(gutterX, y);
        frame();
        assertTrue("le pli replié cache des lignes",
                editor.hiddenLineCount() > 0);
        int foldedInk = gutterInk(foldLine);
        assertTrue("le carré du pli replié vit au panneau ("
                + foldedInk + " pixels)", foldedInk >= ICON_INK);
        // le retour au repos (le scénario repart propre)
        tap(gutterX, y);
        frame();
        assertTrue("le pli se rend", editor.hiddenLineCount() == 0);
    }

    /**
     * LA COUPE DE L'EN-TÊTE REPLIÉ S'APPLIQUE : l'ouvrant et sa suite
     * ne se dessinent plus — le « &#123;…&#125; » naît sur l'ouvrant,
     * RIEN ne vit au-delà de lui (l'avant le dessinait entier : le
     * placeholder se peignait par-dessus, l'écran montrait le double).
     */
    @Test
    public void foldedHeaderCut() {
        arrive();
        int foldLine = openerFoldLine();
        assertTrue("un pli à en-tête ouvrant existe dans la démo", foldLine >= 0);
        // l'en-tête gagne une suite après son ouvrant (la coupe y se
        // lit mieux : « class Lot {  // note » — le note mourra au pli)
        String original = editor.getBuffer().toString();
        int lineEnd = editor.getBuffer().getLineEnd(foldLine);
        InputConnection connection =
                editor.onCreateInputConnection(new EditorInfo());
        editor.setSelection(lineEnd, lineEnd);
        connection.commitText("  // note", 1);
        // le caret quitte la ligne visée (l'ampoule d'actions rapides
        // suit le CARET — un caret sur la ligne tapée volerait le tap
        // de gouttière) et la coupe (le curseur ne vit jamais caché)
        editor.setSelection(0, 0);
        float y = lineTop(foldLine) + editor.lineHeight() / 2.0f;
        float gutterX = editor.getPaddingLeft() * 0.3f;
        tap(gutterX, y);
        frame();
        assertTrue("le pli replié cache des lignes",
                editor.hiddenLineCount() > 0);
        // LE PLACEHOLDER VIT SUR L'OUVRANT : la colonne du « { » porte
        // de l'encre (le triolet atténué y naît)
        int placeholderInk = textInk(foldLine, 22, 28);
        assertTrue("le placeholder vit sur l'ouvrant ("
                + placeholderInk + " pixels)", placeholderInk >= 3);
        // RIEN NE VIT AU-DELÀ DE LA COUPE : la suite de l'en-tête (la
        // note et l'ouvrant d'origine) ne se dessine plus
        int beyondInk = textInk(foldLine, 28, 45);
        assertTrue("rien ne vit au-delà de la coupe (" + beyondInk
                + " pixels)", beyondInk == 0);
        // le retour au texte original : le pli se rend, l'annulation
        // retire la note (le scénario repart propre)
        tap(gutterX, y);
        editor.getUndoStack().undo();
        frame();
        assertTrue("le texte revient à l'original",
                editor.getBuffer().toString().equals(original));
    }

    /**
     * LE PLACEHOLDER SANS OUVRANT VIT À LA FIN DU TEXTE : le pli du
     * serveur sur la ligne de paquet (aucun ouvrant) replie — l'en-tête
     * s'affiche entier, le « … » suit SA FIN (l'avant le posait à la
     * colonne −1 : il vivait dans la gouttière). Le tap au-delà de la
     * fin visible rouvre le bloc.
     */
    @Test
    public void noOpenerPlaceholder() {
        arrive();
        // LA RÉGION DU SERVEUR : la ligne de paquet ouvre un pli sans
        // ouvrant (la ligne vide qui suit se cache)
        editor.setServerFoldRegions(List.of(
                new LspFoldingRange(0, 1, "region")));
        // le caret HORS de la ligne visée (l'ampoule d'actions rapides
        // suit le CARET — un caret sur la ligne tapée volerait le tap
        // de gouttière) et hors de la région repliée
        editor.setSelection(editor.getBuffer().length(),
                editor.getBuffer().length());
        float y = lineTop(0) + editor.lineHeight() / 2.0f;
        float gutterX = editor.getPaddingLeft() * 0.3f;
        tap(gutterX, y);
        frame();
        assertTrue("le pli sans ouvrant cache la ligne vide",
                editor.hiddenLineCount() > 0);
        // LE « … » SUIT LA FIN DE L'EN-TÊTE : la paquet « package
        // exemple; » s'affiche entier, l'encre vit juste au-delà de sa
        // fin — pas dans la gouttière
        String head = editor.getBuffer().getText(
                editor.getBuffer().getLineStart(0),
                editor.getBuffer().getLineEnd(0)).toString();
        int placeholderInk = textInk(0, head.length(), head.length() + 5);
        assertTrue("le placeholder sans ouvrant suit la fin du texte ("
                + placeholderInk + " pixels)", placeholderInk >= 3);
        // LE TAP AU-DELÀ DE LA FIN VISIBLE ROUVRE (la borne tactile
        // vit à la fin du texte, pas à la colonne −1)
        tap(editor.getPaddingLeft()
                + (head.length() + 2) * editor.charWidth(), y);
        frame();
        assertTrue("le tap au-delà de la fin rouvre le pli",
                editor.hiddenLineCount() == 0);
        // le retour au repos (le scénario repart propre)
        editor.setServerFoldRegions(List.of());
        frame();
    }

    private static EditorView findEditor(android.view.View view) {
        if (view instanceof EditorView found) {
            return found;
        }
        if (view instanceof ViewGroup group) {
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
