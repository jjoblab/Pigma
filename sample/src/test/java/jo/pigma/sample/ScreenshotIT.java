package jo.pigma.sample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import jo.pigma.editor.EditorThemes;
import jo.pigma.editor.EditorView;

/**
 * La capture d'écran du guide d'intégration : le VRAI rendu de
 * l'écran de démonstration (le Canvas réel de Robolectric) vers un
 * PNG — la capture que la racine du dépôt montre.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class ScreenshotIT {

    /**
     * Dessine l'écran de démonstration et l'écrit en PNG.
     *
     * @throws Exception si l'écriture échoue
     */
    @Test
    public void capture() throws Exception {
        Activity activity =
                Robolectric.buildActivity(MainActivity.class).setup().get();
        View root = activity.findViewById(android.R.id.content);
        EditorView editor = findEditor(root);
        if (editor == null) {
            throw new IllegalStateException("editor view missing");
        }
        // le thème clair de la vue (le fond du guide) : la capture
        // montre l'éditeur au repos dans son apparence de lecture
        editor.applyTheme(EditorThemes.LIGHT, EditorThemes.LIGHT_BACKGROUND);
        // la vue respire : le remplissage de fond finit, le premier
        // dessin se paie, la capture part d'un écran arrivé
        editor.awaitBackgroundFill(20_000L);
        int width = 1080;
        int height = 1800;
        root.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, width, height);
        // le défilement repart du haut (la capture montre le début
        // du document — la disposition de la feuille basse laisse
        // l'écran précédent défilé)
        editor.harnessRestoreState(new EditorView.SavedState(
                EditorView.SavedState.EMPTY_STATE, 0, -1, 0.0f));
        Bitmap surface = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        surface.eraseColor(EditorThemes.LIGHT_BACKGROUND);
        root.draw(new android.graphics.Canvas(surface));
        File out = new File("build/capture/guide.png");
        out.getParentFile().mkdirs();
        try (OutputStream stream = new FileOutputStream(out)) {
            surface.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        System.out.println("capture écrite : " + out.getAbsolutePath());
    }

    private static EditorView findEditor(View view) {
        if (view instanceof EditorView) {
            return (EditorView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
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
