package jo.pigma.editor.popups;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * L'HISTORIQUE LOCAL du presse-papiers (l'historique du presse-papiers du rapport des popups
 * — « Coller depuis l'historique ») : les dernières copies de
 * l'éditeur y vivent, la plus récente au sommet — le doigt y
 * reprend une copie d'avant (la façon de l'atelier : le presse le
 * garde, l'historique le rend). Le PLAFOND borne la mémoire (vingt
 * entrées — la copie qui déborde chasse la plus vieille), la copie
 * IDENTIQUE consécutive ne se duplique pas (la même chaîne repostée
 * rafraîchit son horodatage, rien de plus).
 */
public final class ClipboardHistory {

    /** Le plafond d'entrées (la plus vieille chassée au débordement). */
    private static final int CAPACITY = 20;

    /** Une entrée de l'historique : le texte posé, l'heure de la pose. */
    public record Entry(String text, long when) {
    }

    /** Les entrées, la plus récente au sommet. */
    private final Deque<Entry> entries = new ArrayDeque<>();

    /**
     * La copie se pose : la même chaîne consécutive ne se duplique
     * pas (son heure se rafraîchit), le plafond tient (la plus
     * vieille chasse).
     *
     * @param text le texte posé au presse-papiers
     * @param when l'heure de la pose (ms)
     */
    public void record(String text, long when) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Entry head = entries.peekFirst();
        if (head != null && head.text().contentEquals(text)) {
            entries.removeFirst();
            entries.addFirst(new Entry(text, when));
            return;
        }
        entries.addFirst(new Entry(text, when));
        while (entries.size() > CAPACITY) {
            entries.removeLast();
        }
    }

    /**
     * Les entrées vivantes, la plus récente au sommet (le harnais
     * les lit).
     *
     * @return les entrées, jamais null
     */
    public List<Entry> entries() {
        return new ArrayList<>(entries);
    }

    /** Le compte des entrées (le harnais le lit). */
    public int size() {
        return entries.size();
    }

    /** L'historique est-il vide ? */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** L'historique se vide (le document d'avant n'y désigne plus rien). */
    public void clear() {
        entries.clear();
    }

    /**
     * L'ancienneté lisible d'une entrée : « à l'instant », « il y a
     * N min », l'heure de la pose au-delà d'une heure.
     *
     * @param when l'heure de la pose (ms)
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
