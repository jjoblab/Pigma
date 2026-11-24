package jo.pigma.core;

import jo.pigma.core.internal.UndoUnit;

import java.util.ArrayList;
import java.util.List;

/**
 * Groupe d'éditions appliquées immédiatement au tampon et annulables en
 * une seule opération.
 *
 * <p>Chaque édition est visible dans le tampon dès son appel (les
 * écouteurs sont notifiés) ; l'engagement pousse l'unité sur la pile
 * d'annulation, le retour arrière rejoue les inverses et le tampon revient
 * exactement à son état d'avant la transaction. Une transaction fermée
 * refuse toute nouvelle édition.</p>
 *
 * <p>Fermeture : {@link #commit()} engage les éditions,
 * {@link #rollback()} les défait, et {@link #close()} engage si la
 * transaction est encore ouverte — compatible try-with-resources. Une
 * exception levée dans le bloc try n'annule donc rien : un appelant qui
 * veut revenir en arrière doit appeler {@link #rollback()} explicitement
 * dans son bloc catch.</p>
 */
public final class TextTransaction implements AutoCloseable {

    private final TextBuffer buffer;
    private final UndoStack stack;
    private final List<UndoUnit.Entry> entries = new ArrayList<>();
    private boolean closed;

    TextTransaction(TextBuffer buffer, UndoStack stack) {
        this.buffer = buffer;
        this.stack = stack;
    }

    /**
     * Remplace la plage {@code [start, end)} par le texte donné, appliqué
     * immédiatement. Une édition sans effet (plage vide, texte vide) n'est
     * pas mémorisée.
     *
     * @param start début de la plage, inclus
     * @param end   fin de la plage, exclue
     * @param text  texte de remplacement, jamais {@code null}
     */
    public void replace(int start, int end, CharSequence text) {
        if (closed) {
            throw new IllegalStateException("transaction is closed");
        }
        String removed = buffer.getText(start, end);
        String replacement = text.toString();
        buffer.replace(start, end, replacement);
        if (removed.isEmpty() && replacement.isEmpty()) {
            return;
        }
        entries.add(new UndoUnit.Entry(start, removed, replacement));
    }

    /**
     * Insère le texte à l'offset donné (application immédiate).
     *
     * @param offset position d'insertion
     * @param text  texte inséré, jamais {@code null}
     */
    public void insert(int offset, CharSequence text) {
        replace(offset, offset, text);
    }

    /**
     * Supprime la plage {@code [start, end)} (application immédiate).
     *
     * @param start début de la plage, inclus
     * @param end   fin de la plage, exclue
     */
    public void delete(int start, int end) {
        replace(start, end, "");
    }

    /**
     * Engage les éditions : elles deviennent une unité annulable. Ne fait
     * rien si la transaction est déjà fermée ou si aucune édition
     * effective n'a eu lieu.
     */
    public void commit() {
        if (closed) {
            return;
        }
        closed = true;
        if (entries.isEmpty()) {
            stack.discard(this);
            return;
        }
        stack.push(new UndoUnit(entries));
    }

    /**
     * Défait toutes les éditions de la transaction : le tampon retrouve
     * exactement son contenu d'avant, les écouteurs sont notifiés des
     * inverses, la révision avance. Aucune unité n'est engagée.
     */
    public void rollback() {
        if (closed) {
            return;
        }
        closed = true;
        for (int i = entries.size() - 1; i >= 0; i--) {
            UndoUnit.Entry entry = entries.get(i);
            buffer.replace(entry.start(),
                    entry.start() + entry.insertedText().length(),
                    entry.removedText());
        }
        entries.clear();
        stack.discard(this);
    }

    /**
     * Ferme la transaction en l'engageant si elle est encore ouverte.
     */
    @Override
    public void close() {
        commit();
    }
}
