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
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import android.os.Looper;

import java.util.List;

import jo.pigma.editor.EditorView;
import jo.pigma.editor.popups.DiagnosticCard;

import static org.junit.Assert.assertTrue;

/**
 * Le rendu du diagnostic à la trame réelle (mode graphique NATIF, à
 * la leçon de la capture du guide) : le retour de terrain sur
 * l'icône du diagnostic — la pastille du tap et les icônes de
 * sévérité vivent AU TRAIT à la couleur de leur sévérité (rouge pour
 * l'erreur, ambre pour l'avertissement — la leçon de la référence,
 * étudiée dans {@code docs/etude-codeassist.md}), l'ampoule de
 * l'étage des correctifs vit à une COLONNE FIXE du cadre (le retour
 * du terrain : elle vivait à l'abscisse du CENTRE VERTICAL de sa
 * rangée — elle dérivait à chaque rangée), et la BARRE
 * annuler/rétablir COIFFE la minimap (les deux boutons EN HAUT de la
 * région, côte à côte — la demande d'origine, la carte du document
 * vit dessous).
 *
 * <p>Chaque fait s'y lit AUX PIXELS sur la carte ouverte à la main
 * (les entrées et leurs correctifs s'y posent directement — le
 * harnais réel éprouve déjà le tap qui l'ouvre).</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class DiagnosticRenderingIT {

    /** Largeur de l'éditeur éprouvé (pixels). */
    private static final int VIEW_WIDTH = 1080;

    /** Hauteur de l'éditeur éprouvé (pixels). */
    private static final int VIEW_HEIGHT = 1800;

    /** Le seuil d'encre d'un glyphe (pixels de la couleur visée). */
    private static final int ICON_INK = 12;

    /** La tolérance de teinte (la distance carrée d'un canal). */
    private static final int CHANNEL_TOLERANCE = 60;

    private Activity activity;
    private EditorView editor;
    private Bitmap surface;
    private Canvas canvas;
    private float density;

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
        density = editor.getContext().getResources()
                .getDisplayMetrics().density;
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

    /** Vide la file du fil d'interface (les différés y courent). */
    private void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    /**
     * Le compte de pixels dont la teinte vit à la distance de
     * tolérance de la couleur donnée (l'anticrénelage y respire).
     *
     * @param color la couleur visée (ARGB)
     * @param x1     la borne gauche (incluse)
     * @param x2     la borne droite (exclue)
     * @param y1     la borne haute (incluse)
     * @param y2     la borne basse (exclue)
     * @return le compte de pixels de la teinte
     */
    private int inkNear(int color, int x1, int x2, int y1, int y2) {
        int count = 0;
        for (int y = Math.max(0, y1); y < Math.min(VIEW_HEIGHT, y2); y++) {
            for (int x = Math.max(0, x1); x < Math.min(VIEW_WIDTH, x2); x++) {
                int at = surface.getPixel(x, y);
                if (closeEnough(at, color)) {
                    count++;
                }
            }
        }
        return count;
    }

    /** La teinte vit-elle à la tolérance de la couleur ? */
    private static boolean closeEnough(int at, int color) {
        int dr = ((at >> 16) & 0xFF) - ((color >> 16) & 0xFF);
        int dg = ((at >> 8) & 0xFF) - ((color >> 8) & 0xFF);
        int db = (at & 0xFF) - (color & 0xFF);
        return dr * dr + dg * dg + db * db
                <= 3 * CHANNEL_TOLERANCE * CHANNEL_TOLERANCE;
    }

    /** Le compte de pixels qui diffèrent du panneau translucide
     * (la référence vit à un point propre du panneau). */
    private int anyInk(int x1, int x2, int y1, int y2, int refX, int refY) {
        int reference = surface.getPixel(refX, refY);
        int count = 0;
        for (int y = Math.max(0, y1); y < Math.min(VIEW_HEIGHT, y2); y++) {
            for (int x = Math.max(0, x1); x < Math.min(VIEW_WIDTH, x2); x++) {
                if (surface.getPixel(x, y) != reference) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * La carte de diagnostic s'ouvre à la main sur la première ligne
     * (deux entrées — une ERREUR et un AVERTISSEMENT — chacune son
     * correctif : l'étage des correctifs y vit à deux rangées).
     */
    private void openCard() {
        editor.diagnosticCard().open(1, List.of(
                new DiagnosticCard.Entry(DiagnosticCard.SEVERITY_ERROR,
                        "Symbole non résolu : prntln",
                        List.of(new DiagnosticCard.Fix(
                                "Importer exemple.prntln", () -> { }))),
                new DiagnosticCard.Entry(DiagnosticCard.SEVERITY_WARNING,
                        "La variable locale n'est pas lue",
                        List.of(new DiagnosticCard.Fix(
                                "Préfixer d'un soulignement", () -> { })))));
        // le pop de naissance se laisse jouer : la carte pose à son
        // repos avant la lecture des pixels
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        frame();
    }

    /**
     * LES ICÔNES DE SÉVÉRITÉ AU TRAIT, à LEUR COULEUR : le cercle
     * barré ROUGE de l'erreur et le triangle AMBRE de l'avertissement
     * vivent à la COLONNE DES ICÔNES de la carte (la même pour les
     * deux — l'avant la peignait en forme pleine barrée de blanc).
     */
    @Test
    public void severityIconsAtTheirInk() {
        arrive();
        openCard();
        DiagnosticCard card = editor.diagnosticCard();
        int iconLeft = (int) (card.poseX() + 20 * density);
        int iconRight = (int) (card.poseX() + 34 * density);
        int rowsTop = (int) (card.poseY() + 10 * density);
        int rowsBottom = (int) (card.poseY() + card.height() - 34 * density);
        int red = inkNear(editor.popupStyle().errorArgb(),
                iconLeft, iconRight, rowsTop, rowsBottom);
        assertTrue("l'icône de l'erreur vit au trait rouge ("
                + red + " pixels)", red >= ICON_INK);
        int amber = inkNear(editor.popupStyle().warningArgb(),
                iconLeft, iconRight, rowsTop, rowsBottom);
        assertTrue("l'icône de l'avertissement vit au trait ambre ("
                + amber + " pixels)", amber >= ICON_INK);
    }

    /**
     * L'AMPOULE DES CORRECTIFS VIT À UNE COLONNE FIXE : l'étage des
     * correctifs porte sa petite ampoule au trait à la MÊME colonne
     * des icônes que les sévérités (le retour du terrain : elle vivait
     * à l'abscisse du CENTRE VERTICAL de sa rangée — elle dérivait à
     * chaque rangée, hors du cadre) ; le titre du correctif ne
     * déborde jamais du mur droit.
     */
    @Test
    public void fixStageIconColumnIsFixed() {
        arrive();
        openCard();
        DiagnosticCard card = editor.diagnosticCard();
        // l'étage des correctifs vit sous les rangées de messages,
        // au-dessus du pied (le pied prend 30 dp)
        int fixesTop = (int) (card.poseY() + card.height() - 30 * density);
        // deux rangées de correctifs : chacune porte son ampoule à la
        // MÊME colonne — l'étage entier se balaie d'une fenêtre
        int fixesBottom = (int) (card.poseY() + card.height() - 30 * density);
        int stageTop = fixesTop - (int) (48 * density);
        int stageBottom = fixesBottom - (int) (4 * density);
        int iconLeft = (int) (card.poseX() + 18 * density);
        int iconRight = (int) (card.poseX() + 36 * density);
        int bulbs = inkNear(editor.popupStyle().accentArgb(),
                iconLeft, iconRight, stageTop, stageBottom);
        assertTrue("l'ampoule des correctifs vit à la colonne fixe ("
                + bulbs + " pixels)", bulbs >= ICON_INK);
        // LE TITTE NE DÉBORDE PAS DU MUR : rien à l'accent au-delà du
        // mur droit (l'ellipsize y coupe, jamais au-delà)
        int wall = (int) (card.poseX() + card.width());
        int overflow = inkNear(editor.popupStyle().accentArgb(),
                wall + 2, wall + (int) (10 * density),
                stageTop, stageBottom);
        assertTrue("le titre du correctif reste dans le cadre ("
                + overflow + " pixels de débordement)", overflow == 0);
        // l'étage vit : deux rangées, deux ampoules — l'encre à
        // l'accent se lit à deux foyers verticaux distincts
        int upper = inkNear(editor.popupStyle().accentArgb(),
                iconLeft, iconRight, stageTop,
                (stageTop + stageBottom) / 2);
        int lower = inkNear(editor.popupStyle().accentArgb(),
                iconLeft, iconRight, (stageTop + stageBottom) / 2,
                stageBottom);
        assertTrue("la première rangée porte son ampoule ("
                + upper + " pixels)", upper >= 4);
        assertTrue("la seconde rangée porte son ampoule ("
                + lower + " pixels)", lower >= 4);
    }

    /**
     * LA BARRE ANNULER/RÉTABLIR COIFFE LA MINIMAP : les deux boutons
     * vivent EN HAUT de la région, l'un à côté de l'autre (la demande
     * d'origine — l'avant les portait en colonne latérale, hors de la
     * bande), et la carte du document vit DESSOUS (sa première
     * rangée naît sous la barre, rien au-dessus).
     */
    @Test
    public void minimapBarCapsTheMap() {
        arrive();
        float bandWidth = 42 * density;
        float barHeight = 40 * density;
        float bandLeft = VIEW_WIDTH - bandWidth;
        // la référence : un point PROPRE du panneau — l'écart entre
        // les deux boutons (le panneau y demeure nu)
        int refX = (int) (bandLeft + bandWidth / 2);
        int refY = (int) (barHeight / 2);
        // LES DEUX BOUTONS : l'encre vit dans CHAQUE moitié de la
        // barre (annuler à gauche, rétablir à droite)
        int leftInk = anyInk((int) (bandLeft + 2),
                (int) (bandLeft + bandWidth / 2 - 2),
                (int) (4 * density), (int) (barHeight - 4 * density),
                refX, refY);
        assertTrue("le bouton annuler vit en haut de la bande ("
                + leftInk + " pixels)", leftInk >= ICON_INK);
        int rightInk = anyInk((int) (bandLeft + bandWidth / 2 + 2),
                (int) (VIEW_WIDTH - 2),
                (int) (4 * density), (int) (barHeight - 4 * density),
                refX, refY);
        assertTrue("le bouton rétablir vit en haut de la bande ("
                + rightInk + " pixels)", rightInk >= ICON_INK);
        // LA CARTE VIT DESSOUS : sa première rangée (un tiret du
        // document) naît SOUS la barre
        int mapInk = anyInk((int) (bandLeft + 3),
                (int) (bandLeft + bandWidth - 3),
                (int) (barHeight + 1), (int) (barHeight + 8 * density),
                refX, refY);
        assertTrue("la carte du document vit sous la barre ("
                + mapInk + " pixels)", mapInk >= 3);
        // RIEN AU-DESSUS DE LA BARRE : la bande au-dessus des boutons
        // demeure au panneau (l'avant y peignait déjà la carte : sa
        // première rangée y naissait)
        int above = anyInk((int) (bandLeft + 4),
                (int) (bandLeft + bandWidth - 4),
                1, (int) (8 * density), refX, refY);
        assertTrue("rien ne vit au-dessus de la barre ("
                + above + " pixels)", above == 0);
    }

    /**
     * LA PASTILLE DU TAP : l'icône de sévérité (au trait, la couleur
     * de la pire — ROUGE pour l'erreur interne des paires) vit à la
     * colonne de la pastille à la GOUTTIÈRE de la ligne diagnostiquée,
     * et elle NAÎT par le pop (l'échelle y gagne, l'opacité suit).
     */
    @Test
    public void badgeSeverityIcon() {
        arrive();
        String original = editor.getBuffer().toString();
        editor.setSelection(original.length(), original.length());
        InputConnection connection =
                editor.onCreateInputConnection(new EditorInfo());
        connection.commitText(")", 1);
        // l'analyse est amortie : la passe repart à l'échéance, les
        // diagnostics de paire arrivent (la file s'y draine)
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        idle();
        frame();
        idle();
        frame();
        int caret = editor.getCaretOffset();
        int line = editor.getBuffer().lineOfOffset(caret);
        float y = editor.getPaddingTop()
                + line * editor.lineHeight() + editor.lineHeight() / 2.0f;
        float x = editor.getPaddingLeft()
                + (caret - 1 - editor.getBuffer().getLineStart(line))
                * editor.charWidth();
        // le premier tap pose la pastille ; le pop se laisse jouer
        // (150 ms : l'opacité y est pleine, l'échelle presque posée)
        tap(x, y);
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        frame();
        // L'ICÔNE AU TRAIT ROUGE vit à la colonne de la pastille (la
        // colonne propre, à l'écart des marqueurs de la ligne) — sa
        // CROIX centrale y vit (le cercle au trait est creux, la croix
        // y signe l'erreur : l'ancienne ampoule n'en portait pas)
        int badgeCx = (int) (editor.getPaddingLeft() - 40 * density);
        int badgeCy = (int) y;
        int red = inkNear(editor.popupStyle().errorArgb(),
                badgeCx - (int) (10 * density), badgeCx + (int) (10 * density),
                badgeCy - (int) (10 * density), badgeCy + (int) (10 * density));
        assertTrue("la pastille porte l'icône de sévérité rouge ("
                + red + " pixels)", red >= ICON_INK);
        int cross = inkNear(editor.popupStyle().errorArgb(),
                badgeCx - (int) (2 * density), badgeCx + (int) (2 * density),
                badgeCy - (int) (2 * density), badgeCy + (int) (2 * density));
        assertTrue("la croix de l'erreur signe le centre de la pastille ("
                + cross + " pixels)", cross >= 2);
        // le retour au propre (le scénario repart propre)
        editor.getUndoStack().undo();
        frame();
        assertTrue("le texte revient à l'original",
                editor.getBuffer().toString().equals(original));
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
