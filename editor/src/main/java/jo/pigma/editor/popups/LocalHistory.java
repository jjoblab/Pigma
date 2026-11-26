package jo.pigma.editor.popups;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * L'historique local du document (B7 du rapport, repensé) : chaque
 * GESTE y laisse une version — la plage remplacée et son texte
 * AVANT, l'étiquette du geste, le moment. La frappe s'y regroupe
 * par pause (deux secondes sans écriture ferment la version) : une
 * phrase tapée tient UNE version, pas une par caractère ; un
 * collage ou une complétion vit sa version à soi.
 *
 * <p>Le budget se compte en OCTETS de texte archivé (le rapport
 * demandait un plafond qui tienne l'espace, pas le nombre de
 * traces) : les versions les plus anciennes s'évincent quand le
 * budget se dépasse. La restauration remet la PLAGE EXACTE d'avant
 * (plus d'écrasement d'une autre ligne par un numéro décalé).</p>
 */
public final class LocalHistory {

    /** Le budget de caractères archivés (les versions anciennes s'évincent). */
    private static final int BUDGET_CHARS = 32_768;

    /** La pause qui ferme une version de frappe (millisecondes). */
    private static final long TYPING_PAUSE_MILLIS = 2_000L;

    /**
     * Une version : la plage remplacée et son texte AVANT le geste.
     *
     * @param millis   le moment du geste (l'horloge du mur)
     * @param gesture  l'étiquette du geste (frappe, collage…)
     * @param line     l'indice de la ligne du début (la lecture par
     *                 plage de lignes s'y entend)
     * @param start    le début de la plage remplacée (décalage du
     *                 document d'AVANT le geste)
     * @param end      la fin de la plage remplacée
     * @param before   le texte d'avant la plage (le contenu qui
     *                 reviendra à la restauration)
     */
    public record Version(long millis, String gesture, int line,
            int start, int end, String before) {
    }

    private final Deque<Version> versions = new ArrayDeque<>();

    /** Les caractères archivés (le budget se tient). */
    private int archivedChars;

    /**
     * Une écriture du tampon s'enregistre. La frappe se regroupe :
     * une écriture qui suit la précédente de moins de la pause, sur
     * la même région, ÉTEND la version ouverte (le moment se rafraîchit)
     * — une phrase tapée tient une version.
     *
     * @param millis   le moment du geste
     * @param gesture  l'étiquette, null pour la frappe
     * @param line     l'indice de la ligne du début de la plage
     * @param start    le début de la plage remplacée (dans le document
     *                 d'avant)
     * @param end      la fin de la plage remplacée (dans le document
     *                 d'avant)
     * @param before   le texte d'avant la plage, jamais null
     */
    public void record(long millis, String gesture, int line,
            int start, int end, String before) {
        if (before == null) {
            before = "";
        }
        String label = gesture == null ? "frappe" : gesture;
        // le regroupement : la version de frappe ouverte s'étend si
        // le geste la touche encore (pause non écoulée, étiquette de
        // frappe des deux côtés, plages qui se touchent)
        Version last = versions.peekLast();
        if (last != null && gesture == null && "frappe".equals(last.gesture())
                && millis - last.millis() < TYPING_PAUSE_MILLIS
                && start <= last.end && end >= last.start) {
            versions.removeLast();
            archivedChars -= last.before().length();
            versions.addLast(new Version(millis, "frappe",
                    Math.min(last.line(), line),
                    Math.min(last.start(), start),
                    Math.max(last.end(), end),
                    mergeBefore(last, start, end, before)));
            archivedChars += versions.peekLast().before().length();
        } else {
            versions.addLast(new Version(millis, label, Math.max(0, line),
                    Math.max(0, start), Math.max(0, end), before));
            archivedChars += before.length();
        }
        evictOverBudget();
    }

    /**
     * Le texte d'avant de la version étendue : le devant et le derrière
     * de l'ancienne se gardent, le milieu prend le nouveau (les plages
     * se recouvrent).
     */
    private static String mergeBefore(Version last, int start, int end,
            String before) {
        if (start >= last.end()) {
            return last.before() + before;
        }
        if (end <= last.start()) {
            return before + last.before();
        }
        // le recouvrement : le devant de l'ancienne, le nouveau, le
        // derrière de l'ancienne — aux recouvrements près, l'ordre se
        // garde (la restauration remet la plage fusionnée)
        int overlapStart = Math.max(last.start(), start) - last.start();
        int overlapEnd = Math.min(last.end(), end) - last.start();
        StringBuilder merged = new StringBuilder(last.before().length()
                + before.length());
        merged.append(last.before(), 0, Math.min(overlapStart,
                last.before().length()));
        merged.append(before);
        if (overlapEnd < last.before().length()) {
            merged.append(last.before(), overlapEnd, last.before().length());
        }
        return merged.toString();
    }

    /** Évince les plus anciennes jusqu'au budget. */
    private void evictOverBudget() {
        while (archivedChars > BUDGET_CHARS && versions.size() > 1) {
            Version evicted = versions.removeFirst();
            archivedChars -= evicted.before().length();
        }
    }

    /**
     * Les versions en ordre chronologique (la plus ancienne d'abord).
     *
     * @return les versions, jamais null
     */
    public List<Version> chronology() {
        return new ArrayList<>(versions);
    }

    /**
     * Les versions qui touchent la plage de lignes, du DERNIER au
     * premier (la carte de l'historique les lit ainsi).
     *
     * @param fromLine la première ligne de la plage, incluse
     * @param toLine   la dernière ligne de la plage, incluse
     * @return les versions de la plage, du dernier au premier
     */
    public List<Version> between(int fromLine, int toLine) {
        List<Version> found = new ArrayList<>();
        for (Version version : versions) {
            if (version.line() >= Math.max(0, fromLine)
                    && version.line() <= Math.max(0, toLine)) {
                found.add(version);
            }
        }
        List<Version> latestFirst = new ArrayList<>(found.size());
        for (int at = found.size() - 1; at >= 0; at--) {
            latestFirst.add(found.get(at));
        }
        return latestFirst;
    }

    /** Le nombre de versions gardées (le harnais le vérifie). */
    public int size() {
        return versions.size();
    }

    /** Range l'histoire (le nouveau document n'a plus de passé). */
    public void clear() {
        versions.clear();
        archivedChars = 0;
    }
}
