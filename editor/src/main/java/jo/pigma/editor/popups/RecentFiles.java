package jo.pigma.editor.popups;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * LA MÉMOIRE DES FICHIERS RÉCENTS (les fichiers récents du rapport des popups — le «
 * Ctrl+E » de l'atelier porté au tactile) : revenir à un document ou
 * à un emplacement d'avant. Les FICHIERS s'y posent (l'hôte y
 * enregistre ses documents ouverts — l'application sait ouvrir), les
 * EMPLACEMENTS s'y suivent (les navigations du document — le fichier,
 * la ligne, l'extrait s'y vit, la demande du rapport). Le PLAFOND
 * borne la mémoire (dix fichiers, dix emplacements — le plus vieux
 * chasse), l'entrée IDENTIQUE consécutive ne se duplique pas (son
 * heure se rafraîchit).
 *
 * <p>JVM pure : la mémoire vit chez la vue, l'hôte y pose, la carte
 * y lit — l'heure s'y passe (le harnais éprouve les âges).</p>
 */
public final class RecentFiles {

    /** Le plafond de fichiers (le plus vieux chassé au débordement). */
    private static final int FILE_CAPACITY = 10;

    /** Le plafond d'emplacements (le plus vieux chassé au débordement). */
    private static final int LOCATION_CAPACITY = 10;

    /** Un fichier récent : son nom, son heure d'ouverture, sa réouverture. */
    public record RecentFile(String name, long when, Runnable open) {
    }

    /** Un emplacement récent : le fichier, la ligne, l'extrait, le retour. */
    public record RecentLocation(String file, int line, String excerpt,
            long when, Runnable go) {
    }

    /** Les fichiers, le plus récent au sommet. */
    private final Deque<RecentFile> files = new ArrayDeque<>();

    /** Les emplacements, le plus récent au sommet. */
    private final Deque<RecentLocation> locations = new ArrayDeque<>();

    /**
     * Le fichier s'y pose : le même nom consécutif ne se duplique pas
     * (son heure se rafraîchit, SA réouverture vit), le plafond tient.
     *
     * @param name  le nom du fichier
     * @param when  l'heure d'ouverture (ms)
     * @param open  la réouverture (le choix y part)
     */
    public void recordFile(String name, long when, Runnable open) {
        if (name == null || name.isEmpty() || open == null) {
            return;
        }
        RecentFile head = files.peekFirst();
        if (head != null && head.name().equals(name)) {
            files.removeFirst();
            files.addFirst(new RecentFile(name, when, open));
            return;
        }
        files.addFirst(new RecentFile(name, when, open));
        while (files.size() > FILE_CAPACITY) {
            files.removeLast();
        }
    }

    /**
     * L'emplacement s'y pose (une navigation du document — le retour
     * d'un saut) : le même fichier ET la même ligne consécutifs ne se
     * dupliquent pas (son heure se rafraîchit), le plafond tient.
     *
     * @param file    le nom du fichier
     * @param line    l'indice de la ligne
     * @param excerpt l'extrait de la ligne (lue dans la carte)
     * @param when    l'heure de la navigation (ms)
     * @param go      le retour à l'emplacement
     */
    public void recordLocation(String file, int line, String excerpt,
            long when, Runnable go) {
        if (file == null || file.isEmpty() || go == null) {
            return;
        }
        RecentLocation head = locations.peekFirst();
        if (head != null && head.file().equals(file)
                && head.line() == line) {
            locations.removeFirst();
            locations.addFirst(new RecentLocation(file, line, excerpt,
                    when, go));
            return;
        }
        locations.addFirst(new RecentLocation(file, line, excerpt, when, go));
        while (locations.size() > LOCATION_CAPACITY) {
            locations.removeLast();
        }
    }

    /**
     * Les fichiers vivants, le plus récent au sommet (le harnais les
     * lit).
     *
     * @return les fichiers, jamais null
     */
    public List<RecentFile> files() {
        return new ArrayList<>(files);
    }

    /**
     * Les emplacements vivants, le plus récent au sommet (le harnais
     * les lit).
     *
     * @return les emplacements, jamais null
     */
    public List<RecentLocation> locations() {
        return new ArrayList<>(locations);
    }

    /** La mémoire est-elle vide (ni fichier ni emplacement) ? */
    public boolean isEmpty() {
        return files.isEmpty() && locations.isEmpty();
    }

    /** La mémoire se vide (le document d'avant n'y désigne plus rien). */
    public void clear() {
        files.clear();
        locations.clear();
    }

    /**
     * L'ancienneté lisible d'une entrée : « à l'instant », « il y a
     * N min », « il y a N h », le jour au-delà.
     *
     * @param when l'heure de l'entrée (ms)
     * @param now  l'heure du moment (ms)
     * @return l'ancienneté lisible
     */
    public static String ageOf(long when, long now) {
        long minutes = Math.max(0L, (now - when) / 60_000L);
        if (minutes <= 0L) {
            return "à l'instant";
        }
        if (minutes < 60L) {
            return "il y a " + minutes + " min";
        }
        long hours = minutes / 60L;
        if (hours < 24L) {
            return "il y a " + hours + " h";
        }
        return "il y a " + (hours / 24L) + " j";
    }
}
