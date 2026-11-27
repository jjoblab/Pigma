package jo.pigma.editor.popups;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * LE GESTIONNAIRE des popups (D-P4 — pas de gestionnaire : 23 appels
 * de fermeture manuels dans la vue, l'exclusion mutuelle codée à la
 * main, une carte oubliée d'une liste de fermeture restait vivante
 * et invisible — LE POPUP FANTÔME). Un seul objet sait quelle popup
 * est ACTIVE.
 *
 * <p>Chaque popup de la vue s'y INSCRIT avec son rôle :
 * {@link Role#FLOATING} (les cartes ancrées — l'info, les usages, le
 * sélecteur, le diagnostic, les actions rapides), {@link Role#KEYBOARD}
 * (les barres à champ — la recherche, l'aller-à, le renommage),
 * {@link Role#TYPING} (la complétion et l'aide aux paramètres — elles
 * COHABITENT avec la frappe) et {@link Role#SELECTION} (la barre de
 * sélection et ses cartes — elles vivent de la sélection).</p>
 *
 * <p>L'EXCLUSION MUTUELLE : une carte FLOTTANTE à la fois (flottante
 * ou à champ — l'ouverture de l'une range TOUTES les autres,
 * {@link #shown(Object)}). La complétion et l'aide aux paramètres
 * COHABITENT avec la frappe — SAUF devant une barre à CHAMP : la
 * barre qui ouvre (recherche, aller-à, renommage) prend le clavier,
 * la complétion qui demeure au-dessus d'elle intercepte le doigt et
 * la frappe (le retour du terrain : « pendant que je tape dans la
 * recherche, la complétion intercepte aussi ce que j'écris ») — une
 * barre à champ range AUSSI la famille de la frappe ; une carte
 * flottante la laisse (la bulle d'import vit PENDANT la frappe, à
 * côté de la complétion). La famille de la sélection vit sa vie
 * d'ancre.</p>
 *
 * <p>LA FERMETURE DE CONTEXTE : {@link #contextLost()} range TOUT —
 * le changement de document, la rotation, la perte de focus ; la
 * liste y vit ENTIÈRE (l'inscription unique — aucune carte ne peut
 * plus rester sur le carreau, le fantôme meurt). Le tap au texte
 * range la famille flottante ({@link #dismissFloating()}), la
 * sélection morte range la sienne ({@link #dismissSelection()}).</p>
 *
 * <p>LA SOURCE UNIQUE des annonces : l'ouverture d'une carte flottante
 * s'y annonce à l'écouteur ({@link #setAnnouncer(Consumer)} —
 * l'accessibilité de la vue y branche son « carte ouverte », un seul
 * point d'émission pour toutes les cartes).</p>
 */
public final class PopupHost {

    /** Le rôle d'une popup auprès du gestionnaire. */
    public enum Role {

        /** La carte ancroit le texte : une seule vit à la fois. */
        FLOATING,

        /** La barre à champ : le clavier s'y délègue, une seule vit. */
        KEYBOARD,

        /** La carte de la frappe : elle COHABITE (complétion, signature). */
        TYPING,

        /** La carte de la sélection : elle vit de l'ancre vivante. */
        SELECTION,

        /** L'ANCRE HORS EXCLUSION : la pastille du tap (A1) — seule
         * la PERTE DE CONTEXTE la range (elle vit trois secondes et
         * le doigt y revient la rouvrir : ni l'exclusion mutuelle
         * des cartes flottantes, ni le tap au texte ne la touchent —
         * son heure ou son geste seuls). */
        ANCHOR
    }

    /** Une popup inscrite : sa fermeture et sa vie. */
    private record Registered(Object card, Role role, Runnable closer,
            BooleanSupplier alive) {
    }

    /** Les popups inscrites — l'ordre d'inscription y demeure. */
    private final List<Registered> cards = new ArrayList<>();

    /** La carte flottante ACTIVE (la dernière ouverte, vivante). */
    private Object activeFloating;

    /** L'écouteur des ouvertures (l'accessibilité y vit). */
    private Consumer<Object> announcer;

    /**
     * Inscrit une popup auprès du gestionnaire — sa fermeture et sa
     * vie y demeurent ; la même carte réinscrite y remplace sa ligne.
     *
     * @param card   la carte (son identité)
     * @param role   son rôle
     * @param closer sa fermeture (le gestionnaire la déclenche)
     * @param alive  sa vie (le gestionnaire la sonde — jamais deux
     *               fois de suite la même réponse périmée)
     */
    public void register(Object card, Role role, Runnable closer,
            BooleanSupplier alive) {
        if (card == null || role == null || closer == null || alive == null) {
            return;
        }
        for (int at = 0; at < cards.size(); at++) {
            if (cards.get(at).card() == card) {
                cards.set(at, new Registered(card, role, closer, alive));
                return;
            }
        }
        cards.add(new Registered(card, role, closer, alive));
    }

    /**
     * La carte qui S'OUVRE : l'exclusion mutuelle s'y joue — toute
     * autre carte flottante ou à champ vivante SE RANGE (la complétion
     * et l'aide aux paramètres cohabitent, la famille de la sélection
     * vit sa vie) ; la carte ouverte devient l'ACTIVE. L'ouverture
     * s'y ANNONCE (la source unique des événements).
     *
     * @param card la carte qui ouvre (inscrite)
     */
    public void shown(Object card) {
        Registered found = find(card);
        if (found == null) {
            return;
        }
        if (found.role() == Role.TYPING || found.role() == Role.SELECTION) {
            // la carte de la frappe COHABITE (complétion et signature
            // y vivent ensemble à la frappe), la carte de la sélection
            // vit de son ancre — aucune exclusion ne les touche
            return;
        }
        for (Registered other : cards) {
            if (other.card() == card) {
                continue;
            }
            if (other.role() == Role.FLOATING || other.role() == Role.KEYBOARD
                    || (other.role() == Role.TYPING && found.role() == Role.KEYBOARD)) {
                if (other.alive().getAsBoolean()) {
                    other.closer().run();
                }
            }
        }
        activeFloating = card;
        announce(card);
    }

    /**
     * Une carte se range (la croix, le choix, la ligne changée) — la
     * carte seule, son rang dans l'état s'y règle.
     *
     * @param card la carte à ranger
     */
    public void dismiss(Object card) {
        Registered found = find(card);
        if (found != null && found.alive().getAsBoolean()) {
            found.closer().run();
        }
        if (card == activeFloating) {
            activeFloating = null;
        }
    }

    /**
     * La famille FLOTTANTE se range — le doigt qui reprend le texte
     * ne doit jamais tomber sur une carte flottante d'avant (les
     * barres à champ y demeurent : leur rangement leur appartient).
     */
    public void dismissFloating() {
        dismissRole(Role.FLOATING);
    }

    /** La famille de la SÉLECTION se range (l'ancre est morte). */
    public void dismissSelection() {
        dismissRole(Role.SELECTION);
    }

    /**
     * LE CHANGEMENT DE CONTEXTE : le document change, la rotation
     * retourne l'écran, le focus se perd — TOUT se range, la liste
     * y vit ENTIÈRE (aucune carte inscrite ne reste sur le carreau —
     * le fantôme meurt là où il naissait : la liste de fermeture
     * oubliée d'une carte ; l'ancre du tap y vit pareillement, la
     * pastille d'un document ne survit pas au document suivant).
     */
    public void contextLost() {
        for (Registered card : cards) {
            if (card.alive().getAsBoolean()) {
                card.closer().run();
            }
        }
        activeFloating = null;
    }

    /**
     * La carte flottante ACTIVE — la dernière ouverte (null quand
     * aucune ne vit).
     */
    public Object activeFloating() {
        if (activeFloating != null) {
            Registered found = find(activeFloating);
            if (found == null || !found.alive().getAsBoolean()) {
                activeFloating = null;
            }
        }
        return activeFloating;
    }

    /** Une carte flottante vit-elle ? */
    public boolean anyFloatingShown() {
        for (Registered card : cards) {
            if (card.role() == Role.FLOATING && card.alive().getAsBoolean()) {
                return true;
            }
        }
        return false;
    }

    /**
     * L'annonce des ouvertures : l'écouteur s'y pose (l'accessibilité
     * de la vue — un seul point d'émission pour toutes les cartes).
     *
     * @param listener l'écouteur des cartes qui ouvrent, null pour
     *                 taire
     */
    public void setAnnouncer(Consumer<Object> listener) {
        announcer = listener;
    }

    /** Le rôle d'une carte inscrite (null si étrangère). */
    public Role roleOf(Object card) {
        Registered found = find(card);
        return found == null ? null : found.role();
    }

    /** La ligne d'une carte (l'inscription remplace). */
    private Registered find(Object card) {
        for (Registered registered : cards) {
            if (registered.card() == card) {
                return registered;
            }
        }
        return null;
    }

    /** Toutes les cartes d'un rôle vivantes se rangent. */
    private void dismissRole(Role role) {
        for (Registered card : cards) {
            if (card.role() == role && card.alive().getAsBoolean()) {
                card.closer().run();
            }
        }
        if (role == Role.FLOATING) {
            activeFloating = null;
        }
    }

    /** L'ouverture s'y annonce — la source unique. */
    private void announce(Object card) {
        if (announcer != null) {
            announcer.accept(card);
        }
    }
}
