package jo.pigma.editor.internal.search;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Le chercheur (JVM, sans canevas) : la collecte au plafond, le choix
 * de la première correspondance après le curseur, le cycle qui
 * BOUCLE (après la dernière vient la première, avant la première
 * vient la dernière), la rangée silencieuse de l'édition, la
 * correspondance courante lue à l'hôte.
 */
public final class SearchControllerTest {

    /** L'hôte d'essai : le texte posé, le curseur posé, les aller-à
     * collectés. */
    private static final class RecordingHost
            implements SearchController.Host {

        private String text = "";

        private int caret;

        private final List<Integer> revealed = new ArrayList<>();

        @Override
        public CharSequence documentText() {
            return text;
        }

        @Override
        public int caret() {
            return caret;
        }

        @Override
        public void revealMatch(int start, int queryLength) {
            revealed.add(start);
        }

        @Override
        public void repaint() {
        }
    }

    private final RecordingHost host = new RecordingHost();

    private final SearchController controller = new SearchController(host);

    /**
     * LA COLLECTE : les correspondances se ramassent au plafond, la
     * première APRÈS le curseur devient courante (pas la première du
     * document — le curseur compte).
     */
    @Test
    public void collectAndFirstAfterCaret() {
        host.text = "un deux deux un deux";
        host.caret = 4;
        controller.search("deux");
        assertEquals("trois correspondances", 3, controller.matchCount());
        assertEquals("la première après le curseur est courante",
                1, controller.currentIndex());
        assertEquals(8, controller.matchAt(1));
        // la première correspondance s'amène à l'hôte (l'aller-à part)
        assertEquals(Integer.valueOf(8), host.revealed.get(0));
        // la requête vide range tout
        controller.search("");
        assertEquals(0, controller.matchCount());
        assertNull(controller.query());
        assertEquals(-1, controller.currentIndex());
    }

    /**
     * LE CYCLE BOUCLE : après la dernière vient la première, avant la
     * première vient la dernière — les deux sens.
     */
    @Test
    public void theCycleWrapsBothWays() {
        host.text = "a b c a b c";
        host.caret = 0;
        controller.search("b");
        assertEquals(0, controller.currentIndex());
        controller.next();
        assertEquals("la dernière correspondance devient courante",
                1, controller.currentIndex());
        controller.next();
        assertEquals("après la dernière vient la première",
                0, controller.currentIndex());
        controller.previous();
        assertEquals("avant la première vient la dernière",
                1, controller.currentIndex());
        // trois correspondances, le cycle les traverse toutes
        host.text = "x x x";
        host.caret = 0;
        controller.search("x");
        assertEquals(0, controller.currentIndex());
        controller.next();
        assertEquals(1, controller.currentIndex());
        controller.next();
        assertEquals(2, controller.currentIndex());
        controller.next();
        assertEquals("le cycle revient à la première",
                0, controller.currentIndex());
        controller.previous();
        assertEquals("l'arrière revient à la dernière",
                2, controller.currentIndex());
    }

    /**
     * LE PLAFOND : au-delà de la limite le compte s'arrête — le
     * chercheur ne remplit pas la mémoire d'un document piège.
     */
    @Test
    public void theCeilingStopsTheCount() {
        host.text = "z ".repeat(SearchController.MATCH_LIMIT + 500);
        host.caret = 0;
        controller.search("z");
        assertEquals("le compte s'arrête au plafond",
                SearchController.MATCH_LIMIT, controller.matchCount());
    }

    /**
     * LA RANGÉE SILENCIEUSE : une édition déplace tout ce qui suit —
     * les positions seraient des mensonges, la recherche se range
     * SANS réclamer la trame (l'hôte repeint déjà par ailleurs).
     */
    @Test
    public void theEditForgetsQuietly() {
        host.text = "mot mot";
        host.caret = 0;
        controller.search("mot");
        assertEquals(2, controller.matchCount());
        controller.forget();
        assertEquals(0, controller.matchCount());
        assertNull(controller.query());
        assertEquals(-1, controller.currentIndex());
    }

    /**
     * La sous-chaîne : la casse compte, le chevauchement ne se
     * collecte pas (la collecte repart APRÈS la correspondance).
     */
    @Test
    public void exactAndNonOverlapping() {
        host.text = "aaa";
        host.caret = 0;
        controller.search("aa");
        assertEquals("les correspondances ne se chevauchent pas",
                1, controller.matchCount());
        assertEquals(0, controller.matchAt(0));
        host.text = "Ab aB ab";
        host.caret = 0;
        controller.search("ab");
        assertEquals("la casse compte", 1, controller.matchCount());
        assertEquals(6, controller.matchAt(0));
    }
}
