package jo.pigma.editor.input;

/**
 * Le contrat interne du paquet input (l'arborescence du prompt, §3) :
 * ce dont la connexion IME a besoin de la vue. HORS API publique —
 * l'hôte ne le voit jamais, il ne se documente pas, il peut changer.
 *
 * <p>L'implémentation vit dans la vue (une classe anonyme y accède à
 * ses membres privés) : les douze membres internes ne se publient
 * pas, l'API publique reste petite.</p>
 */
public interface EditorContract {

    /** La position du curseur (décalage UTF-16). */
    int caret();

    /** Le début de la zone de composition (-1 : pas de composition). */
    int composingStart();

    /** La fin de la zone de composition (-1 : pas de composition). */
    int composingEnd();

    /**
     * La zone de composition se pose (chaque mise à jour de l'IME la
     * remplace).
     *
     * @param start le début posé
     * @param end   la fin posée
     */
    void setComposing(int start, int end);

    /** Une sélection tient-elle (l'ancre et le curseur diffèrent) ? */
    boolean hasSelection();

    /** Le bord gauche de la sélection. */
    int selectionStart();

    /** Le bord droit de la sélection. */
    int selectionEnd();

    /** Le texte sélectionné (null : rien). */
    CharSequence selectedText();

    /** La longueur du document (les bornes s'y amarrent). */
    int length();

    /**
     * Le texte du document entre deux bornes (la lecture
     * contextuelle du clavier — ses prédictions et sa capitalisation
     * s'y calculent).
     *
     * @param start le début (inclus)
     * @param end   la fin (exclue)
     * @return le texte demandé
     */
    CharSequence textBetween(int start, int end);

    /**
     * Applique une édition atomique (une unité d'annulation).
     *
     * @param start le début remplacé
     * @param end   la fin remplacée
     * @param text  le texte posé
     */
    void applyEdit(int start, int end, CharSequence text);

    /** Range la zone de composition. */
    void clearComposing();

    /**
     * Supprime autour du curseur.
     *
     * @param left  les caractères avant
     * @param right les caractères après
     */
    void deleteAround(int left, int right);

    /** Déplace le curseur (delta en caractères). */
    void moveCaret(int delta);

    /** Déplace le curseur verticalement (delta en lignes). */
    void moveCaretVertically(int delta);

    /**
     * Pose la sélection (le clavier la déplace : le champ de
     * recherche du clavier, la poignée du texte).
     *
     * @param start le bord gauche
     * @param end   le bord droit
     */
    void setSelection(int start, int end);
}
