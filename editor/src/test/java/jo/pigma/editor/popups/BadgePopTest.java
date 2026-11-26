package jo.pigma.editor.popups;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Le pop de naissance de la pastille (JVM, sans canevas) : la
 * signature du ressort de la référence — l'échelle part de la
 * MOITIÉ, gagne UN en dépassant légèrement (l'ease-out-back), le
 * fondu se paie à la moitié du chemin, la course tient ses 300 ms
 * et le reset range tout.
 */
public final class BadgePopTest {

    /** L'horloge posée : le temps s'avance à la main. */
    private final AtomicLong now = new AtomicLong();

    /** Le pop éprouvé, à l'horloge posée. */
    private final BadgePop pop = new BadgePop(now::get);

    /**
     * LA COURSE ENTIÈRE : la moitié à la naissance, UN au repos —
     * en passant par un léger DÉPASSEMENT (le ressort amorti de la
     * référence y signe l'apparition).
     */
    @Test
    public void theSpringCourse() {
        now.set(0L);
        pop.born();
        assertTrue("le pop vit à sa naissance", pop.isRunning());
        assertEquals("l'échelle part de la moitié", 0.5f,
                pop.scale(), 0.001f);
        assertEquals("l'opacité part de zéro", 0.0f, pop.alpha(), 0.001f);
        // la moitié du chemin : le fondu s'y paie entier
        now.set(150L);
        assertEquals("le fondu se paie à la moitié", 1.0f,
                pop.alpha(), 0.001f);
        // le dépassement du ressort vit quelque part au chemin
        boolean overshot = false;
        for (long at = 1; at < 300; at++) {
            now.set(at);
            if (pop.scale() > 1.0f) {
                overshot = true;
            }
        }
        assertTrue("le ressort dépasse légèrement en chemin", overshot);
        // le repos : l'échelle pose à UN, la course meurt
        now.set(300L);
        assertEquals("l'échelle pose à un au repos", 1.0f,
                pop.scale(), 0.001f);
        assertFalse("la course meurt au repos", pop.isRunning());
        // le reset range tout
        now.set(0L);
        pop.born();
        now.set(10L);
        assertTrue("le pop renaît après le reset", pop.isRunning());
        pop.reset();
        assertFalse("le reset range la course", pop.isRunning());
        assertEquals("l'échelle reposée se lit", 1.0f,
                pop.scale(), 0.001f);
    }

    /**
     * Le pop AVANT sa naissance : au repos (une pastille neuve ne
     * saute pas — la vue ne la dessine qu'à sa naissance).
     */
    @Test
    public void atRestBeforeBirth() {
        assertFalse("un pop neuf ne vit pas", pop.isRunning());
        assertEquals("l'échelle neuve se lit posée", 1.0f,
                pop.scale(), 0.001f);
        assertEquals("l'opacité neuve se lit pleine", 1.0f,
                pop.alpha(), 0.001f);
    }
}
