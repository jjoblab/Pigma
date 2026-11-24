package jo.pigma.sample;

/**
 * Informations affichées par l'application de démonstration.
 *
 * <p>Classe volontairement pure, sans dépendance à Android : elle peut être
 * testée sur la seule machine virtuelle Java, ce qui valide le harnais de
 * tests dès le premier module du dépôt.</p>
 */
public final class SampleInfo {

    /** Nom du projet affiché à l'écran. */
    static final String PROJECT_NAME = "Pigma";

    private SampleInfo() {
        // Classe d'informations : aucune instance utile.
    }

    /**
     * Construit le texte d'accueil de l'écran principal.
     *
     * @return texte non vide, commençant par le nom du projet
     */
    static String welcomeText() {
        return PROJECT_NAME + "\nApplication de démonstration.";
    }
}
