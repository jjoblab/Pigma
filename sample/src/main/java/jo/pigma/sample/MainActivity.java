package jo.pigma.sample;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Écran principal de l'application de démonstration.
 *
 * <p>À ce stade, l'écran n'affiche que le nom du projet : il prouve que
 * l'application démarre et que la chaîne de build produit un APK. Les
 * fonctions d'édition arriveront avec les modules de la bibliothèque.</p>
 */
public final class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView view = new TextView(this);
        view.setText(SampleInfo.welcomeText());
        view.setTextSize(20.0f);
        int padding = 48;
        view.setPadding(padding, padding, padding, padding);
        setContentView(view);
    }
}
