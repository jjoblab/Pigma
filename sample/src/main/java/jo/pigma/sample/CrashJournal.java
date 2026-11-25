package jo.pigma.sample;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Le journal des crashs : le filet des plantages sur appareil. Un
 * gestionnaire d'exceptions non interceptées s'y installe AVANT
 * que l'écran ne se construise : le plantage qui force la fermeture
 * s'écrit dans un fichier du stockage (la pile complète, le fil,
 * le scénario courant, l'horodatage) AVANT que le cadre n'achève
 * le plantage — l'écriture y est courte et directe, sans ressource
 * qui retarde la fin.
 *
 * <p>À la réouverture, l'écran lit le journal : les plantages non
 * consommés s'affichent (la pile au complet, la section où le
 * crash est arrivé — c'est elle qui dit à l'analyse ce qui se
 * passait : « XML (démo) », « gros fichier Java », « LSP »…).
 * L'utilisateur peut les copier, les effacer ; un plantage lu ne
 * s'affiche plus à la prochaine ouverture.</p>
 */
final class CrashJournal {

    /** Le nom du fichier du journal (il vit dans le stockage interne). */
    private static final String FILE_NAME = "crash-journal.txt";

    /** Les plantages gardés au plus (le fichier tourne au-delà). */
    private static final int KEPT_CRASHES = 8;

    /**
     * La section courante de l'application : le scénario à l'écran
     * au moment du plantage. Volatile : le fil du plantage (un fil
     * quelconque — le fond, l'interface) y lit la dernière posée
     * sans verrou, la pose n'attend jamais.
     */
    private static volatile String currentSection = "ouverture";

    private CrashJournal() {
    }

    /**
     * Installe le filet : le gestionnaire du fil par défaut prend
     * tout plantage non intercepté et l'écrit au journal avant de
     * laisser le cadre fermer l'application. Un plantage dans le
     * gestionnaire lui-même ne doit pas boucler : l'écriture y est
     * gardée, l'ancien gestionnaire reste en chaîne.
     *
     * @param storage le répertoire du stockage interne, jamais null
     */
    static void install(File storage) {
        final Thread.UncaughtExceptionHandler previous =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            writeCrash(storage, thread, throwable);
            // l'ancien gestionnaire suit (le cadre y ferme
            // l'application) ; s'il manque, la fin est brutale
            // mais le journal est déjà écrit
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            }
        });
    }

    /**
     * Pose la section courante : l'écran la tient à jour à chaque
     * changement (ouverture d'onglet, gros fichier, LSP, thème…).
     * La pose ne lève jamais — une chaîne null y devient
     * « inconnue ».
     *
     * @param section la section visible, null pour « inconnue »
     */
    static void setSection(String section) {
        currentSection = section == null ? "inconnue" : section;
    }

    /**
     * La section courante (celle qui s'écrira dans un plantage).
     *
     * @return le nom de la section, jamais null
     */
    static String currentSection() {
        return currentSection;
    }

    /**
     * Les plantages non consommés : lus à la réouverture, chaque
     * entrée du fichier s'y présente (l'entête datée et sectionnée,
     * la pile au complet). Un plantage affiché est consommé : la
     * prochaine ouverture ne le montre plus.
     *
     * @param storage le répertoire du stockage interne, jamais null
     * @return les plantages, liste vide si le journal est vide
     */
    static List<String> pendingCrashes(File storage) {
        File file = journalFile(storage);
        if (!file.isFile()) {
            return List.of();
        }
        List<String> entries = new ArrayList<>();
        try {
            java.util.Scanner scanner = new java.util.Scanner(file,
                    StandardCharsets.UTF_8.name());
            try {
                // le séparur d'entrées : une ligne de tirets entre
                // chaque plantage écrit
                StringBuilder entry = new StringBuilder();
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.startsWith("---")) {
                        if (entry.length() > 0) {
                            entries.add(entry.toString());
                            entry.setLength(0);
                        }
                    } else {
                        entry.append(line).append('\n');
                    }
                }
                if (entry.length() > 0) {
                    entries.add(entry.toString());
                }
            } finally {
                scanner.close();
            }
        } catch (IOException readFailed) {
            // un journal illisible se reprend à la prochaine écriture
            return List.of();
        }
        return entries;
    }

    /**
     * Efface le journal : les plantages consommés disparaissent du
     * stockage (l'écran le propose après affichage).
     *
     * @param storage le répertoire du stockage interne, jamais null
     */
    static void clear(File storage) {
        File file = journalFile(storage);
        if (file.isFile() && !file.delete()) {
            file.deleteOnExit();
        }
    }

    /**
     * Le fichier du journal dans le stockage donné.
     *
     * @param storage le répertoire du stockage
     * @return le fichier du journal, jamais null
     */
    private static File journalFile(File storage) {
        return new File(storage, FILE_NAME);
    }

    /**
     * Écrit un plantage au journal : l'entête (l'heure, le fil, la
     * section courante, le modèle et la version du cadre) et la
     * pile complète. L'écriture est appendue — les plantages
     * précédents restent — et bornée : au-delà de {@value
     * #KEPT_CRASHES}, les plus anciens laissent la place.
     *
     * @param storage  le répertoire du stockage, jamais null
     * @param thread   le fil qui a planté, jamais null
     * @param throwable le plantage, jamais null
     */
    private static void writeCrash(File storage, Thread thread,
            Throwable throwable) {
        try {
            File file = journalFile(storage);
            trimIfNeeded(file);
            Writer writer = new OutputStreamWriter(
                    new FileOutputStream(file, true),
                    StandardCharsets.UTF_8);
            try {
                writer.write("---\n");
                writer.write("crash "
                        + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                                Locale.ROOT).format(new Date()) + "\n");
                writer.write("section : " + currentSection + "\n");
                writer.write("fil : " + thread.getName() + "\n");
                writer.write("appareil : " + android.os.Build.MANUFACTURER
                        + " " + android.os.Build.MODEL
                        + ", Android " + android.os.Build.VERSION.RELEASE
                        + " (API " + android.os.Build.VERSION.SDK_INT
                        + ")\n");
                writer.write("pile :\n");
                writer.write(stackOf(throwable));
                writer.write('\n');
            } finally {
                writer.close();
            }
        } catch (Throwable writeFailed) {
            // l'écriture d'un crash ne doit JAMAIS lever : le cadre
            // achève le plantage original quoi qu'il arrive
        }
    }

    /**
     * La pile complète d'un plantage en texte (la chaîne causale au
     * complet : chaque cause suit la précédente).
     *
     * @param throwable le plantage
     * @return la pile en texte, jamais null
     */
    private static String stackOf(Throwable throwable) {
        StringBuilder stack = new StringBuilder();
        Throwable walk = throwable;
        String prefix = "";
        while (walk != null) {
            stack.append(prefix).append(walk).append('\n');
            for (StackTraceElement element : walk.getStackTrace()) {
                stack.append("  à ").append(element).append('\n');
            }
            walk = walk.getCause();
            prefix = "causé par ";
        }
        return stack.toString();
    }

    /**
     * Limite le journal : au-delà du quota, les plus anciens
     * plantages sortent (le fichier se réécrit des entrées
     * récentes).
     *
     * @param file le fichier du journal
     */
    private static void trimIfNeeded(File file) {
        if (!file.isFile()) {
            return;
        }
        List<String> entries = new ArrayList<>(pendingCrashes(file
                .getParentFile() == null
                        ? new File(".") : file.getParentFile()));
        while (entries.size() >= KEPT_CRASHES) {
            entries.remove(0);
        }
        if (entries.isEmpty()) {
            return;
        }
        try {
            Writer writer = new OutputStreamWriter(
                    new FileOutputStream(file, false),
                    StandardCharsets.UTF_8);
            try {
                for (String entry : entries) {
                    writer.write("---\n");
                    writer.write(entry);
                }
            } finally {
                writer.close();
            }
        } catch (IOException rewriteFailed) {
            // la reriture du journal échouée se reprendra au crash
            // suivant (l'append suit)
        }
    }
}
