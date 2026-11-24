package android.app;

import android.content.ApplicationInfo;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import java.io.File;

/**
 * Activité de démonstration : le contenu se pose et s'attache, la
 * création se déclenche explicitement — le harnais tient le rôle du
 * cadre applicatif (l'information d'application et le répertoire de
 * cache y vivent aussi : le serveur hors processus du sample s'y
 * branche).
 */
public class Activity extends Context {

    private View root;

    /** L'information d'application (le répertoire natif s'y lit). */
    private final ApplicationInfo applicationInfo = new ApplicationInfo();

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

    /**
     * L'information d'application : le répertoire natif y pointe sur
     * un répertoire du harnais (le serveur Lua embarqué s'y cherche).
     *
     * @return l'information, jamais null
     */
    public ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }

    /**
     * Le répertoire de cache de l'application (un répertoire
     * temporaire du bac, créé à la demande).
     *
     * @return le répertoire, jamais null
     */
    public File getCacheDir() {
        return new File(System.getProperty("java.io.tmpdir", "/tmp"),
                "pigma-harness-cache");
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
