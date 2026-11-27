package jo.pigma.editor.popups;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * LE GESTIONNAIRE DES POPUPS aux JVM pures (sans Android) : l'état
 * unique « carte active », l'exclusion mutuelle (une carte
 * flottante à la fois — les cartes de la frappe COHABITENT), la
 * fermeture au changement de contexte (la liste inscrite y vit
 * ENTIÈRE — le popup fantôme d'une carte oubliée y meurt), la
 * famille de la sélection, l'annonce des ouvertures (la source
 * unique). Le défaut du rapport des popups : 23 fermetures
 * manuelles dans la vue, l'exclusion codée à la main.
 */
public class PopupHostTest {

    /** Une carte d'essai : sa vie se compte, sa fermeture se voit. */
    private static final class Probe {
        private boolean shown;

        void open() {
            shown = true;
        }

        void close() {
            shown = false;
        }

        boolean isShown() {
            return shown;
        }
    }

    /** Le gestionnaire éprouvé. */
    private final PopupHost host = new PopupHost();

    /** Les treize rôles de la vue, en essai (cinq flottantes, trois à champ, deux de frappe, trois de sélection). */
    private final Probe info = new Probe();
    private final Probe usages = new Probe();
    private final Probe picker = new Probe();
    private final Probe diagnostic = new Probe();
    private final Probe quickActions = new Probe();
    private final Probe search = new Probe();
    private final Probe gotoBar = new Probe();
    private final Probe rename = new Probe();
    private final Probe completion = new Probe();
    private final Probe signature = new Probe();
    private final Probe selectionBar = new Probe();
    private final Probe overflow = new Probe();
    private final Probe history = new Probe();
    /** La pastille du tap (A1) — le rôle ANCRE. */
    private final Probe badge = new Probe();

    /** Inscrit les quatorze, l'ordre de la vue. */
    private void registerAll() {
        host.register(info, PopupHost.Role.FLOATING, info::close, info::isShown);
        host.register(usages, PopupHost.Role.FLOATING, usages::close, usages::isShown);
        host.register(picker, PopupHost.Role.FLOATING, picker::close, picker::isShown);
        host.register(diagnostic, PopupHost.Role.FLOATING, diagnostic::close,
                diagnostic::isShown);
        host.register(quickActions, PopupHost.Role.FLOATING, quickActions::close,
                quickActions::isShown);
        host.register(search, PopupHost.Role.KEYBOARD, search::close, search::isShown);
        host.register(gotoBar, PopupHost.Role.KEYBOARD, gotoBar::close, gotoBar::isShown);
        host.register(rename, PopupHost.Role.KEYBOARD, rename::close, rename::isShown);
        host.register(completion, PopupHost.Role.TYPING, completion::close,
                completion::isShown);
        host.register(signature, PopupHost.Role.TYPING, signature::close,
                signature::isShown);
        host.register(selectionBar, PopupHost.Role.SELECTION, selectionBar::close,
                selectionBar::isShown);
        host.register(overflow, PopupHost.Role.SELECTION, overflow::close,
                overflow::isShown);
        host.register(history, PopupHost.Role.SELECTION, history::close,
                history::isShown);
        host.register(badge, PopupHost.Role.ANCHOR, badge::close,
                badge::isShown);
    }

    /**
     * L'EXCLUSION MUTUELLE : la carte flottante qui ouvre devient LA
     * carte active — toute autre carte flottante OU à champ vivante
     * se range ; les cartes de la frappe et la famille de la
     * sélection ne bougent pas.
     */
    @Test
    public void theFloatingOpeningRulesTheOthers() {
        registerAll();
        info.open();
        usages.open();
        picker.open();
        diagnostic.open();
        quickActions.open();
        search.open();
        gotoBar.open();
        rename.open();
        completion.open();
        signature.open();
        selectionBar.open();
        overflow.open();
        history.open();
        host.shown(usages);
        assertTrue("la carte ouverte demeure", usages.isShown());
        assertFalse("l'info d'avant se range", info.isShown());
        assertFalse("le sélecteur d'avant se range", picker.isShown());
        assertFalse("le diagnostic d'avant se range", diagnostic.isShown());
        assertFalse("les actions rapides d'avant se rangent",
                quickActions.isShown());
        assertFalse("la recherche d'avant se range", search.isShown());
        assertFalse("l'aller-à d'avant se range", gotoBar.isShown());
        assertFalse("le renommage d'avant se range", rename.isShown());
        // LA FRAPPE COHABITE : la complétion et la signature demeurent
        assertTrue("la complétion cohabite avec l'ouverture",
                completion.isShown());
        assertTrue("l'aide aux paramètres cohabite", signature.isShown());
        // LA SÉLECTION VIT SA VIE d'ancre
        assertTrue("la barre de sélection demeure", selectionBar.isShown());
        assertTrue("le débordement demeure", overflow.isShown());
        assertTrue("l'historique demeure", history.isShown());
        assertSame("la carte ACTIVE se lit", usages, host.activeFloating());
    }

    /**
     * La barre à CHAMP vit l'exclusion pareillement (une carte à la
     * fois — la recherche qui ouvre range les flottantes ET les
     * autres barres).
     */
    @Test
    public void theKeyboardOpeningRulesTheFloatingToo() {
        registerAll();
        usages.open();
        diagnostic.open();
        gotoBar.open();
        search.open();
        host.shown(search);
        assertTrue("la barre ouverte demeure", search.isShown());
        assertFalse("les usages d'avant se rangent", usages.isShown());
        assertFalse("le diagnostic d'avant se range", diagnostic.isShown());
        assertFalse("l'aller-à d'avant se range", gotoBar.isShown());
        assertSame("la barre devient la carte active", search,
                host.activeFloating());
    }

    /**
     * La carte de la FRAPPE ne vit AUCUNE exclusion : la complétion
     * qui s'ouvre ne range personne (elle cohabite avec la frappe),
     * l'active flottante ne bouge pas.
     */
    @Test
    public void theTypingCardCohabitsWithEveryone() {
        registerAll();
        usages.open();
        host.shown(usages);
        completion.open();
        host.shown(completion);
        assertTrue("la complétion demeure", completion.isShown());
        assertTrue("les usages demeurent (la frappe n'évince personne)",
                usages.isShown());
        assertSame("l'active flottante demeure", usages,
                host.activeFloating());
    }

    /**
     * LE TAP AU TEXTE : la famille flottante se range d'un trait —
     * les barres à champ et les cartes de frappe demeurent (leur
     * rangement leur appartient).
     */
    @Test
    public void dismissFloatingRangsOnlyTheFloatingFamily() {
        registerAll();
        info.open();
        usages.open();
        diagnostic.open();
        search.open();
        completion.open();
        host.dismissFloating();
        assertFalse("l'info se range", info.isShown());
        assertFalse("les usages se rangent", usages.isShown());
        assertFalse("le diagnostic se range", diagnostic.isShown());
        assertTrue("la recherche demeure (son rangement lui appartient)",
                search.isShown());
        assertTrue("la complétion demeure", completion.isShown());
        assertNull("l'active se vide", host.activeFloating());
        assertFalse("aucune flottante ne vit", host.anyFloatingShown());
    }

    /** La sélection morte : SA famille se range, les autres demeurent. */
    @Test
    public void dismissSelectionRangsOnlyTheSelectionFamily() {
        registerAll();
        selectionBar.open();
        overflow.open();
        history.open();
        usages.open();
        host.dismissSelection();
        assertFalse("la barre se range", selectionBar.isShown());
        assertFalse("le débordement se range", overflow.isShown());
        assertFalse("l'historique se range", history.isShown());
        assertTrue("les usages demeurent", usages.isShown());
    }

    /**
     * LE CHANGEMENT DE CONTEXTE range TOUT — la liste inscrite y vit
     * ENTIÈRE : le popup fantôme (la carte oubliée d'une liste de
     * fermeture, vivante et invisible) n'a plus où naître.
     */
    @Test
    public void contextLostRangsEverythingRegistered() {
        registerAll();
        info.open();
        usages.open();
        picker.open();
        diagnostic.open();
        quickActions.open();
        search.open();
        gotoBar.open();
        rename.open();
        completion.open();
        signature.open();
        selectionBar.open();
        overflow.open();
        history.open();
        host.contextLost();
        List<Probe> everyone = List.of(info, usages, picker, diagnostic,
                quickActions, search, gotoBar, rename, completion,
                signature, selectionBar, overflow, history);
        for (Probe probe : everyone) {
            assertFalse("la carte se range : " + probe, probe.isShown());
        }
        assertNull("l'active se vide", host.activeFloating());
        assertFalse("aucune flottante ne vit", host.anyFloatingShown());
    }

    /**
     * LE RÔLE ANCRE (A1) : la pastille du tap ne vit AUCUNE exclusion
     * — ni le tap au texte (le doigt y revient la TAPER), ni
     * l'ouverture d'une carte flottante (elle meurt de son heure ou
     * de son geste, pas de la présence d'une autre) ; seule la PERTE
     * DE CONTEXTE la range (le document change : la pastille d'un
     * texte n'a pas à survivre au texte).
     */
    @Test
    public void theAnchorSurvivesEverythingButContextLost() {
        registerAll();
        badge.open();
        // le tap au texte ne la range pas
        host.dismissFloating();
        assertTrue("le tap au texte ne range pas la pastille",
                badge.isShown());
        // l'ouverture d'une carte flottante ne l'évince pas
        usages.open();
        host.shown(usages);
        assertTrue("l'ouverture d'une carte n'évince pas la pastille",
                badge.isShown());
        // la sélection morte ne la range pas
        host.dismissSelection();
        assertTrue("la sélection morte ne range pas la pastille",
                badge.isShown());
        // LA PERTE DE CONTEXTE seule la range
        host.contextLost();
        assertFalse("la perte de contexte range la pastille",
                badge.isShown());
    }

    /**
     * Une carte se range SEULE (la croix, le choix, la ligne
     * changée) — les autres ne bougent pas.
     */
    @Test
    public void dismissOneCardLeavesTheOthers() {
        registerAll();
        usages.open();
        diagnostic.open();
        host.dismiss(diagnostic);
        assertFalse("la carte visée se range", diagnostic.isShown());
        assertTrue("les usages demeurent", usages.isShown());
        assertNull("l'active se vide quand la carte active se range",
                host.activeFloating());
    }

    /** L'étrangère et la carte morte : rien ne se passe. */
    @Test
    public void theStrangerAndTheDeadCardDoNothing() {
        registerAll();
        Probe stranger = new Probe();
        stranger.open();
        host.dismiss(stranger);
        assertTrue("l'étrangère ne se range pas (elle n'est pas inscrite)",
                stranger.isShown());
        host.dismiss(usages);
        Probe dead = new Probe();
        host.shown(dead);
        assertFalse("la carte jamais inscrite n'ouvre rien", dead.isShown());
        assertNull("aucune active", host.activeFloating());
    }

    /**
     * L'ANNONCE DES OUVERTURES (la source unique de l'accessibilité) :
     * l'ouverture d'une carte flottante ou à champ s'y annonce ; la
     * carte de frappe et celle de la sélection ne s'y annoncent pas
     * (elles cohabitent, l'annonce y mentirait).
     */
    @Test
    public void theOpeningAnnouncesItself() {
        registerAll();
        AtomicInteger announces = new AtomicInteger();
        AtomicReference<Object> announced = new AtomicReference<>();
        host.setAnnouncer(card -> {
            announces.incrementAndGet();
            announced.set(card);
        });
        host.shown(diagnostic);
        assertEquals("l'ouverture s'annonce une fois", 1, announces.get());
        assertSame("la carte annoncée se lit", diagnostic, announced.get());
        host.shown(completion);
        host.shown(selectionBar);
        assertEquals("la frappe et la sélection ne s'annoncent pas",
                1, announces.get());
    }

    /** L'inscription se REMPLACE (la même carte réinscrite, sa ligne se met à jour). */
    @Test
    public void registeringTwiceReplacesTheLine() {
        registerAll();
        Probe reboxed = new Probe();
        host.register(usages, PopupHost.Role.FLOATING, reboxed::close,
                reboxed::isShown);
        usages.open();
        host.dismiss(usages);
        assertTrue("l'ancienne carte vit encore (sa ligne fut remplacée — la fermeture parle à la neuve)",
                usages.isShown());
        // la réinscription partielle s'ignore (null n'inscrit rien)
        host.register(null, PopupHost.Role.FLOATING, () -> { }, () -> true);
        assertNull("la carte nulle ne s'inscrit pas",
                host.roleOf(null));
    }
}
