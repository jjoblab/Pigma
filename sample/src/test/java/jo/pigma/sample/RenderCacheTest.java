package jo.pigma.sample;

import java.util.function.Supplier;

import org.junit.Test;

import jo.pigma.sample.RenderCache.PlanBaker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Le cache de rendu ligne par ligne : validité par identité d'entrée
 * (le chemin rapide ne touche pas le texte), partage par contenu (deux
 * lignes identiques, un seul rendu), invalidation globale (thème,
 * langage), borne d'éviction et sécurité au décalage de lignes (une
 * suppression au-dessus déplace les entrées : l'identité d'entrée ne
 * peut pas confondre deux contenus).
 */
public class RenderCacheTest {

    /** Compteur de cuissons observable par le test. */
    private int baked;

    private final PlanBaker<String> baker = (line, stamp, content) -> {
        baked++;
        return "plan<" + content + ">";
    };

    /** Le fournisseur paresseux : ne doit servir que hors chemin rapide. */
    private static final class Lazy implements Supplier<CharSequence> {

        private final String value;
        private int served;

        Lazy(String value) {
            this.value = value;
        }

        @Override
        public CharSequence get() {
            served++;
            return value;
        }
    }

    /**
     * Chemin rapide : même entrée, deux trames — le plan est le même
     * objet, le texte n'est pas resservi.
     */
    @Test
    public void fastPathServesWithoutReadingText() {
        RenderCache<String> cache = new RenderCache<>(8);
        Object stamp = new Object();
        Lazy content = new Lazy("val x = 1");
        String first = cache.frame(0, stamp, content, baker);
        int servedAfterFirst = content.served;
        String second = cache.frame(0, stamp, content, baker);
        assertSame(first, second);
        assertEquals("une seule cuisson", 1, baked);
        assertEquals("le chemin rapide ne resert pas le texte",
                servedAfterFirst, content.served);
        assertEquals(1, cache.slotHits());
    }

    /**
     * Partage par contenu : une autre entrée (relexage, décalage) de
     * même texte reprend le plan sans recuire.
     */
    @Test
    public void sameContentDifferentStampSharesPlan() {
        RenderCache<String> cache = new RenderCache<>(8);
        String first = cache.frame(0, new Object(), new Lazy("}"), baker);
        String second = cache.frame(0, new Object(), new Lazy("}"), baker);
        assertSame("deux piles égales... deux lignes égales, même plan", first, second);
        assertEquals(1, baked);
        assertEquals(1, cache.keyHits());
    }

    /**
     * Deux lignes identiques du document partagent un seul plan.
     */
    @Test
    public void identicalLinesShareOneBake() {
        RenderCache<String> cache = new RenderCache<>(8);
        String line3 = cache.frame(3, new Object(), new Lazy("    }"), baker);
        String line7 = cache.frame(7, new Object(), new Lazy("    }"), baker);
        assertSame(line3, line7);
        assertEquals(1, baked);
    }

    /**
     * L'invalidation (thème, langage) vide tout : le même contenu
     * recuit — re-rendu, jamais retokenisation.
     */
    @Test
    public void invalidateForcesRebake() {
        RenderCache<String> cache = new RenderCache<>(8);
        Object stamp = new Object();
        cache.frame(0, stamp, new Lazy("}"), baker);
        cache.invalidate();
        String after = cache.frame(0, stamp, new Lazy("}"), baker);
        assertEquals("l'invalidation recuit", 2, baked);
        assertTrue("le plan est une cuisson neuve", after.startsWith("plan<"));
    }

    /**
     * La borne du partage évince le plus ancien : le troisième contenu
     * chasse le premier, qui recuit à son retour.
     */
    @Test
    public void sharedBoundEvictsOldest() {
        RenderCache<String> cache = new RenderCache<>(2);
        cache.frame(0, new Object(), new Lazy("a"), baker);
        cache.frame(1, new Object(), new Lazy("b"), baker);
        cache.frame(2, new Object(), new Lazy("c"), baker);
        assertEquals(3, baked);
        cache.frame(3, new Object(), new Lazy("a"), baker);
        assertEquals("« a » évincé, il recuit", 4, baked);
    }

    /**
     * Sécurité au décalage : un tampon de ligne tient une entrée
     * d'avant des suppressions ; l'entrée courante est un autre objet —
     * l'identité force la rechamp, le contenu différent recuit.
     */
    @Test
    public void lineShiftCannotServeAStalePlan() {
        RenderCache<String> cache = new RenderCache<>(8);
        String before = cache.frame(5, new Object(), new Lazy("x"), baker);
        String after = cache.frame(5, new Object(), new Lazy("y"), baker);
        assertNotSame(before, after);
        assertEquals(2, baked);
    }

    /**
     * Les lignes au-delà de la capacité initiale des tampons
     * (croissance) se servent comme les autres.
     */
    @Test
    public void slotsGrowWithTheDocument() {
        RenderCache<String> cache = new RenderCache<>(8);
        String plan = cache.frame(500, new Object(), new Lazy("loin"), baker);
        assertEquals("plan<loin>", plan);
        assertEquals(1, baked);
    }

    /**
     * Le fournisseur paresseux n'est jamais servi sur le chemin rapide
     * — un fournisseur qui lève après la première cuisson prouve le
     * contrat.
     */
    @Test
    public void throwingSupplierProvesTheLazyContract() {
        RenderCache<String> cache = new RenderCache<>(8);
        Object stamp = new Object();
        cache.frame(0, stamp, new Lazy("texte"), baker);
        Supplier<CharSequence> bomb = () -> {
            throw new IllegalStateException("le chemin rapide ne lit pas le texte");
        };
        int bakesBefore = baked;
        String fast = cache.frame(0, stamp, bomb, baker);
        assertEquals("aucune cuisson sur le chemin rapide", bakesBefore, baked);
        assertTrue(fast.startsWith("plan<"));
    }
}
