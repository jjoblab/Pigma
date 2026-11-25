package jo.pigma.editor.popups;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * L'historique local du document : chaque écriture y laisse une trace
 * — le moment du geste, son étiquette (frappe, collage,
 * remplacement…), la ligne touchée, le texte de la ligne APRÈS le
 * geste. L'histoire d'une PARTIE du code s'y lit par sa plage de
 * lignes (la carte de l'historique y puise), la restauration y remet
 * la ligne d'une trace passée.
 *
 * <p>L'anneau borne la mémoire : les traces les plus anciennes
 * s'évincent quand le plafond se dépasse — l'historique local vit
 * de la main-d'œuvre récente, pas de la paléontologie. Les relevés
 * se posent en ordre chronologique, la carte les lit du dernier au
 * premier.</p>
 */
public final class LocalHistory {

    /** Le plafond de traces gardées (l'anneau évince au-delà). */
    private static final int MAX_TRACES = 64;

    /** Le plafond de caractères d'une ligne archivée. */
    private static final int MAX_LINE_CHARS = 400;

    /**
     * Une trace : le moment du geste, son étiquette, la ligne
     * touchée, le texte de la ligne après le geste (coupé au
     * plafond, le saut de fin exclu).
     *
     * @param millis   le moment du geste (l'horloge du mur)
     * @param gesture  l'étiquette du geste (frappe, collage…)
     * @param line     l'indice de la ligne touchée
     * @param lineText le texte de la ligne après le geste
     */
    public record Trace(long millis, String gesture, int line, String lineText) {
    }

    private final Deque<Trace> traces = new ArrayDeque<>();

    /**
     * Une trace neuve : le moment porte l'horloge du mur, la ligne se
     * coupe au plafond (le saut de fin exclu — la trace montre une
     * ligne, pas le document).
     *
     * @param millis   le moment du geste
     * @param gesture  l'étiquette du geste, jamais null (vide admise)
     * @param line     l'indice de la ligne touchée
     * @param lineText le texte de la ligne après le geste, null ou
     *                 vide pour une ligne vidée
     */
    public void record(long millis, String gesture, int line, String lineText) {
        String bounded = lineText == null ? ""
                : lineText.length() > MAX_LINE_CHARS
                        ? lineText.substring(0, MAX_LINE_CHARS) : lineText;
        traces.addLast(new Trace(millis, gesture == null ? "" : gesture,
                Math.max(0, line), bounded));
        while (traces.size() > MAX_TRACES) {
            traces.removeFirst();
        }
    }

    /**
     * Les traces en ordre chronologique (la plus ancienne d'abord).
     *
     * @return les traces, jamais null
     */
    public List<Trace> chronology() {
        return new ArrayList<>(traces);
    }

    /**
     * Les traces qui touchent la plage de lignes, du DERNIER au
     * premier (la carte de l'historique les lit ainsi).
     *
     * @param fromLine la première ligne de la plage, incluse
     * @param toLine   la dernière ligne de la plage, incluse
     * @return les traces de la plage, du dernier au premier
     */
    public List<Trace> between(int fromLine, int toLine) {
        List<Trace> found = new ArrayList<>();
        for (Trace trace : traces) {
            if (trace.line() >= Math.max(0, fromLine)
                    && trace.line() <= Math.max(0, toLine)) {
                found.add(trace);
            }
        }
        List<Trace> latestFirst = new ArrayList<>(found.size());
        for (int at = found.size() - 1; at >= 0; at--) {
            latestFirst.add(found.get(at));
        }
        return latestFirst;
    }

    /** Le nombre de traces gardées (le harnais le vérifie). */
    public int size() {
        return traces.size();
    }

    /** Range l'histoire (le nouveau document n'a plus de passé). */
    public void clear() {
        traces.clear();
    }
}
