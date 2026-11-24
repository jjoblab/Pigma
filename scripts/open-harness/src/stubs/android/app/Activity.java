package android.app;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

/**
 * Activité de démonstration : le contenu se pose et s'attache, la
 * création se déclenche explicitement — le harnais tient le rôle du
 * cadre applicatif.
 */
public class Activity extends Context {

    private View root;

    /**
     * La création (les activités concrètes la surchargent).
     *
     * @param savedInstanceState l'état sauvegardé (null ici)
     */
    protected void onCreate(Bundle savedInstanceState) {
    }

    /**
     * Pose le contenu : la racine s'attache (les attachements
     * descendent récursivement).
     *
     * @param view la racine du contenu
     */
    public void setContentView(View view) {
        root = view;
        view.harnessAttach();
    }

    /**
     * Exécute la suite sur le fil de l'interface (ici : tout de
     * suite, le fil appelant est le nôtre).
     *
     * @param action la suite
     */
    public void runOnUiThread(Runnable action) {
        action.run();
    }

    /** La racine du contenu posé (null si aucune). */
    public View harnessRootView() {
        return root;
    }

    /** Déclenche la création (l'état sauvegardé est nul). */
    public void harnessCreate() {
        onCreate(null);
    }
}
