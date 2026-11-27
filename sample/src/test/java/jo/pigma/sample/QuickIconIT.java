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
 * LE RETOUR DU TERRAIN des icônes : « les options du popup de
 * l'ampoule ont toutes la même icône » — la carte d'actions rapides
 * (l'ampoule de la gouttière) agrégait cinq familles sous un éclair
 * unique. Chaque action porte désormais SA NATURE, un glyphe AU TRAIT
 * à la grille 24 — la preuve se lit AUX PIXELS : deux natures
 * distinctes peignent des icônes distinctes, la même nature peint la
 * même icône, et l'éclair d'action (le défaut) demeure le sien.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class QuickIconIT {

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

    /** La densité de l'écran (le dp en pixels). */
    private float density;

    /**
     * LES NATURES DISTINCTES PEIGNENT DES ICÔNES DISTINCTES : la
     * colonne des icônes se lit au pixel — le commentaire n'y peint
     * pas la loupe, la loupe n'y peint pas l'éclair ; la MÊME nature
     * peint la MÊME icône (la comparaison se prouve à elle-même).
     */
    @Test
    public void theDistinctNaturesPaintDistinctIcons() {
        editor.openQuickActions(List.of(
                new QuickActionCard.Entry("Un commentaire",
                        () -> { }, QuickActionCard.Glyph.COMMENT),
                new QuickActionCard.Entry("Une loupe",
                        () -> { }, QuickActionCard.Glyph.SEARCH),
                new QuickActionCard.Entry("Un éclair",
                        () -> { }, QuickActionCard.Glyph.ACTION),
                new QuickActionCard.Entry("Un commentaire encore",
                        () -> { }, QuickActionCard.Glyph.COMMENT)));
        frame();
        QuickActionCard card = editor.quickActionCard();
        assertTrue("la carte vit", card.isShown());
        // la colonne des icônes : les 22 dp au centre de chaque rangée
        int differing = iconDifference(card, 0, 1);
        assertTrue("le commentaire ne peint pas la loupe ("
                + differing + " pixels différents)", differing > 30);
        differing = iconDifference(card, 1, 2);
        assertTrue("la loupe ne peint pas l'éclair ("
                + differing + " pixels différents)", differing > 30);
        // la MÊME nature peint la MÊME icône (la preuve se prouve)
        differing = iconDifference(card, 0, 3);
        assertTrue("la même nature peint la même icône ("
                + differing + " pixels différents)", differing <= 2);
    }

    /**
     * Les VRAIES familles de l'ampoule portent leurs natures : les
     * actions d'édition de l'éditeur (le commentaire, la casse, le
     * double, la poubelle) — les natures s'y lisent, l'éclair unique
     * d'avant y est mort.
     */
    @Test
    public void theEditorActionsCarryTheirNatures() {
        editor.setLanguage(jo.pigma.syntax.Languages.JAVA,
                "class A {\n    int total() {\n        return 1;\n    }\n}\n");
        frame();
        idle();
        frame();
        List<QuickActionCard.Entry> actions = editor.quickActionsOfLine(2);
        assertEqualsNature("le commentaire porte sa nature",
                QuickActionCard.Glyph.COMMENT, actions,
                "Commenter / décommenter");
        assertEqualsNature("le double porte sa nature",
                QuickActionCard.Glyph.DUPLICATE, actions,
                "Dupliquer la ligne");
        assertEqualsNature("la poubelle porte sa nature",
                QuickActionCard.Glyph.TRASH, actions,
                "Supprimer la ligne");
    }

    /** La nature d'une rangée par son libellé (la casse exige une
     * sélection — l'ordre ne se devine pas). */
    private static void assertEqualsNature(String message,
            QuickActionCard.Glyph expected, List<QuickActionCard.Entry> actions,
            String label) {
        for (QuickActionCard.Entry action : actions) {
            if (label.equals(action.label())) {
                assertTrue(message + " — la nature se lit",
                        expected == action.glyph());
                return;
            }
        }
        throw new AssertionError(message + " (la rangée " + label
                + " ne vit pas)");
    }

    /**
     * La différence au pixel des icônes de deux rangées : la colonne
     * des icônes se coupe (22 dp au centre de chaque rangée), les
     * pixels qui diffèrent s'y comptent.
     *
     * @param card la carte vivante
     * @param from l'indice de la première rangée
     * @param to   l'indice de la seconde rangée
     * @return le nombre de pixels qui diffèrent
     */
    private int iconDifference(QuickActionCard card, int from, int to) {
        float fromY = card.rowCenterY(from);
        float toY = card.rowCenterY(to);
        assertTrue("la première rangée se voit", fromY >= 0);
        assertTrue("la seconde rangée se voit", toY >= 0);
        float iconCenterX = card.poseX() + 14.0f * density
                + 11.0f * density;
        int reach = (int) (11.0f * density);
        // LE MASQUE D'ENCRE : la carte est TRANSLUCIDE (le texte
        // derrière y biseaute) — la comparaison se fait au masque du
        // TRAIT (la teinte de l'encre secondaire), pas au pixel brut
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
