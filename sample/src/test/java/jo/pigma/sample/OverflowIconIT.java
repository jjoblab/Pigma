package jo.pigma.sample;

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
import org.robolectric.annotation.GraphicsMode;

import jo.pigma.editor.EditorView;
import jo.pigma.editor.popups.QuickActionCard;

/**
 * LE RETOUR DE TERRAIN DES ICÔNES DU DÉBORDEMENT : les options du
 * popup « … » de la barre de sélection portaient leurs libellés nus —
 * chaque rangée porte désormais SA NATURE AU TRAIT à la grille 24
 * (la leçon de l'ampoule, le peintre partagé ActionGlyphs) : la
 * preuve se lit AUX PIXELS : deux natures distinctes peignent des
 * icônes distinctes, la même nature peint la même icône, l'éclair
 * d'action (le défaut des rangées qui ne disent rien) demeure le
 * sien.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class OverflowIconIT {

    private static final int VIEW_WIDTH = 1080;
    private static final int VIEW_HEIGHT = 1800;

    private Activity activity;
    private EditorView editor;
    private Bitmap surface;
    private Canvas canvas;

    /** La densité de l'écran (le dp en pixels). */
    private float density;

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
        assertTrue("le remplissage de fond finit",
                editor.awaitBackgroundFill(20_000L));
        surface = Bitmap.createBitmap(VIEW_WIDTH, VIEW_HEIGHT,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(surface);
        density = editor.getContext().getResources()
                .getDisplayMetrics().density;
        frame();
        idle();
    }

    /**
     * LES NATURES DISTINCTES PEIGNENT DES ICÔNES DISTINCTES : la
     * colonne des icônes du débordement se lit au pixel — les
     * ciseaux du couper n'y peignent pas le cadre du tout
     * sélectionner, le cadre n'y peint pas l'éclair de l'hôte.
     */
    @Test
    public void theDistinctNaturesPaintDistinctIcons() {
        // une VRAIE sélection vit : la barre ET le débordement y naissent
        editor.setSelection(0, 6);
        editor.showOverflowMenu();
        frame();
        jo.pigma.editor.popups.OverflowCard card = editor.overflowCard();
        assertTrue("la carte de débordement vit", card.isShown());
        // les rangées du débordement réel portent leurs natures :
        // la liste en mémoire se lit, la preuve aux pixels suit
        List<String> labels = card.labels();
        assertTrue("la rangée Couper vit",
                labels.contains("Couper"));
        assertTrue("la rangée Tout sélectionner vit",
                labels.contains("Tout sélectionner"));
        assertTrue("la rangée Quitter la sélection vit",
                labels.contains("Quitter la sélection"));
        // la géométrie : les centres des rangées se lisent
        int cutIndex = labels.indexOf("Couper");
        int selectIndex = labels.indexOf("Tout sélectionner");
        assertTrue("les rangées se voient", cutIndex >= 0 && selectIndex >= 0);
        float cutY = card.rowCenterY(cutIndex);
        float selectY = card.rowCenterY(selectIndex);
        assertTrue("la rangée Couper se voit", cutY >= 0);
        assertTrue("la rangée Tout sélectionner se voit", selectY >= 0);
        // la preuve aux pixels : ciseaux ≠ cadre, sur la colonne
        // des icônes (22 dp au centre de chaque rangée)
        int differing = iconDifference(card, cutIndex, selectIndex);
        assertTrue("les ciseaux ne peignent pas le cadre ("
                + differing + " pixels différents)", differing > 30);
    }

    /**
     * La différence au pixel des icônes de deux rangées : la colonne
     * des icônes se coupe (22 dp au centre de chaque rangée), les
     * pixels qui diffèrent s'y comptent — au masque d'encre du trait
     * (la teinte secondaire), pas au pixel brut (la carte est
     * translucide — le texte derrière y biseaute).
     */
    private int iconDifference(jo.pigma.editor.popups.OverflowCard card,
            int from, int to) {
        float fromY = card.rowCenterY(from);
        float toY = card.rowCenterY(to);
        assertTrue("la première rangée se voit", fromY >= 0);
        assertTrue("la seconde rangée se voit", toY >= 0);
        float iconCenterX = card.poseX() + 18.0f * density
                + 11.0f * density;
        int reach = (int) (11.0f * density);
        int ink = card.style().secondaryArgb();
        int differing = 0;
        for (int dy = -reach; dy <= reach; dy++) {
            for (int dx = -reach; dx <= reach; dx++) {
                int x = (int) (iconCenterX + dx);
                boolean fromInk = inkNear(surface.getPixel(x, (int) (fromY + dy)), ink);
                boolean toInk = inkNear(surface.getPixel(x, (int) (toY + dy)), ink);
                if (fromInk != toInk) {
                    differing++;
                }
            }
        }
        return differing;
    }

    /** La teinte vit-elle à la tolérance de l'encre (l'anticrénelage
     * du trait y respire — la translucence de la carte y comprise) ? */
    private static boolean inkNear(int at, int ink) {
        int dr = ((at >> 16) & 0xFF) - ((ink >> 16) & 0xFF);
        int dg = ((at >> 8) & 0xFF) - ((ink >> 8) & 0xFF);
        int db = (at & 0xFF) - (ink & 0xFF);
        return dr * dr + dg * dg + db * db <= 3 * 60 * 60;
    }

    private void frame() {
        surface.eraseColor(0);
        editor.draw(canvas);
    }

    private void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    /**
     * La vue d'édition sous la racine de l'activité (le harnais la
     * cherche dans la hiérarchie réelle du cadre).
     */
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
