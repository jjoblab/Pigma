package jo.pigma.core;

import jo.pigma.core.internal.UndoUnit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Pile d'annulation d'un tampon : les transactions engagées y deviennent
 * des unités annulables une à une, dans l'ordre inverse de leur
 * engagement.
 *
 * <p>Une nouvelle transaction engageée efface le rétablissement (une
 * édition fraîche rend les branches abandonnées incohérentes). L'annulation
 * et le rétablissement d'une unité renotifient les écouteurs du tampon et
 * font avancer sa révision. Une seule transaction peut être ouverte à la
 * fois ; annuler ou rétablir au milieu d'une transaction ouverte est
 * interdit.</p>
 */
public final class UndoStack {

    private static final int DEFAULT_LIMIT = 256;

    private final TextBuffer buffer;
    private final Deque<UndoUnit> undoUnits = new ArrayDeque<>();
    private final Deque<UndoUnit> redoUnits = new ArrayDeque<>();
    private int limit;
    private TextTransaction openTransaction;

    /**
     * Pile sur le tampon donné, capacité par défaut de
     * {@value #DEFAULT_LIMIT} unités.
     *
     * @param buffer tampon annulé par cette pile, jamais {@code null}
     */
    public UndoStack(TextBuffer buffer) {
        this(buffer, DEFAULT_LIMIT);
    }

    /**
     * Pile sur le tampon donné, capacité limitée : au-delà, les unités les
     * plus anciennes sont oubliées (elles ne sont plus annulables).
     *
     * @param buffer tampon annulé par cette pile, jamais {@code null}
     * @param limit  nombre maximal d'unités retenues, au moins 1
     */
    public UndoStack(TextBuffer buffer, int limit) {
        if (buffer == null) {
            throw new NullPointerException("buffer must not be null");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        this.buffer = buffer;
        this.limit = limit;
    }

    /**
     * Ouvre une transaction sur le tampon : les éditions y sont appliquées
     * immédiatement et l'engagement les regroupe en une unité annulable.
     *
     * @return la transaction ouverte, jamais {@code null}
     */
    public TextTransaction newTransaction() {
        if (openTransaction != null) {
            throw new IllegalStateException("a transaction is already open");
        }
        openTransaction = new TextTransaction(buffer, this);
        return openTransaction;
    }

    /**
     * Oublie toute l'histoire : ni annulable, ni rétablissable. L'hôte
     * l'appelle après un chargement de contenu intégral (ouvrir un
     * fichier) : le chargement n'est pas une édition à défaire.
     */
    public void clear() {
        undoUnits.clear();
        redoUnits.clear();
    }

    /**
     * Indique si au moins une unité est annulable.
     *
     * @return vrai s'il reste quelque chose à annuler
     */
    public boolean canUndo() {
        return !undoUnits.isEmpty();
    }

    /**
     * Indique si au moins une unité est rétablissable.
     *
     * @return vrai s'il reste quelque chose à rétablir
     */
    public boolean canRedo() {
        return !redoUnits.isEmpty();
    }

    /**
     * Annule la dernière unité engagée : les inverses des éditions sont
     * rejoués en ordre inverse, le tampon revient à l'état d'avant.
     *
     * @return les changements appliqués au tampon par l'annulation, dans
     *         l'ordre où ils ont été appliqués ; liste vide si rien à annuler
     */
    public List<TextChange> undo() {
        if (openTransaction != null) {
            throw new IllegalStateException("a transaction is open");
        }
        if (undoUnits.isEmpty()) {
            return List.of();
        }
        UndoUnit unit = undoUnits.removeLast();
        List<TextChange> changes = replay(unit, true);
        redoUnits.addLast(unit);
        trim(redoUnits);
        return changes;
    }

    /**
     * Rétablit la dernière unité annulée : les éditions sont rejouées dans
     * leur ordre d'application.
     *
     * @return les changements appliqués au tampon par le rétablissement ;
     *         liste vide si rien à rétablir
     */
    public List<TextChange> redo() {
        if (openTransaction != null) {
            throw new IllegalStateException("a transaction is open");
        }
        if (redoUnits.isEmpty()) {
            return List.of();
        }
        UndoUnit unit = redoUnits.removeLast();
        List<TextChange> changes = replay(unit, false);
        undoUnits.addLast(unit);
        trim(undoUnits);
        return changes;
    }

    /**
     * Capacité maximale de la pile ; la réduire aussitôt oublie les unités
     * les plus anciennes.
     *
     * @return le nombre maximal d'unités retenues
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Change la capacité maximale de la pile, au moins 1 ; les unités les
     * plus anciennes au-delà de la nouvelle capacité sont oubliées.
     *
     * @param newLimit nombre maximal d'unités retenues
     */
    public void setLimit(int newLimit) {
        if (newLimit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        limit = newLimit;
        trim(undoUnits);
        trim(redoUnits);
    }

    // ------------------------------------------------------------ interne

    /**
     * Rejoue une unité dans le sens voulu et décrit chaque changement
     * appliqué au tampon.
     */
    private List<TextChange> replay(UndoUnit unit, boolean backwards) {
        List<UndoUnit.Entry> entries = unit.entries();
        List<TextChange> changes = new ArrayList<>(entries.size());
        if (backwards) {
            for (int i = entries.size() - 1; i >= 0; i--) {
                UndoUnit.Entry entry = entries.get(i);
                int end = entry.start() + entry.insertedText().length();
                buffer.replace(entry.start(), end, entry.removedText());
                changes.add(TextChange.replace(entry.start(), end, entry.removedText()));
            }
        } else {
            for (UndoUnit.Entry entry : entries) {
                int end = entry.start() + entry.removedText().length();
                buffer.replace(entry.start(), end, entry.insertedText());
                changes.add(TextChange.replace(entry.start(), end, entry.insertedText()));
            }
        }
        return changes;
    }

    /**
     * Oublie les unités les plus anciennes au-delà de la capacité.
     */
    private void trim(Deque<UndoUnit> units) {
        while (units.size() > limit) {
            units.removeFirst();
        }
    }

    void push(UndoUnit unit) {
        undoUnits.addLast(unit);
        trim(undoUnits);
        redoUnits.clear();
        openTransaction = null;
    }

    void discard(TextTransaction transaction) {
        if (openTransaction == transaction) {
            openTransaction = null;
        }
    }
}
