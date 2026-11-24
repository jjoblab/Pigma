package jo.pigma.core.internal;

import java.util.List;

/**
 * Unité d'annulation : suite d'éditions appliquées à un tampon, dans leur
 * ordre d'application. L'annulation rejoue les inverses en ordre inverse ;
 * le rétablissement rejoue les éditions dans l'ordre direct.
 *
 * <p>Classe interne (package {@code internal}) : publique par nécessité de
 * séparation de package, mais hors de l'API contractuelle du cœur, non
 * documentée pour l'hôte et susceptible de changer.</p>
 */
public final class UndoUnit {

    /**
     * Une édition appliquée : la plage remplacée et les deux textes, avant
     * et après.
     *
     * @param start        début de la plage remplacée
     * @param removedText  contenu ôté par l'édition, jamais {@code null}
     * @param insertedText contenu mis par l'édition, jamais {@code null}
     */
    public record Entry(int start, String removedText, String insertedText) {
    }

    private final List<Entry> entries;

    public UndoUnit(List<Entry> entries) {
        this.entries = List.copyOf(entries);
    }

    /**
     * Éditions de l'unité, dans l'ordre d'application.
     *
     * @return la liste des éditions, jamais {@code null}, non modifiable
     */
    public List<Entry> entries() {
        return entries;
    }
}
