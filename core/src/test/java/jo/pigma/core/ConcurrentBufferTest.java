package jo.pigma.core;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test de stress de la course de données : un fil édite le tampon en
 * boucle (le fil d'interface de l'application), un autre y prend des
 * clichés (le fil de fond du moteur de coloration). Le cliché doit
 * rester cohérent — texte, bornes et révision d'un seul tenant — et
 * aucune lecture ne lève, quoi que fasse l'éditeur entre-temps.
 * Avant le verrou de mutation, la prise pouvait lire un index de
 * lignes à moitié réécrit : l'exception d'indice y tombait.
 */
public class ConcurrentBufferTest {

    /** Le temps de course par test, en millisecondes. */
    private static final long RACE_MILLIS = 1500L;

    /** Les éditions par lot du fil éditeur. */
    private static final int EDITS_PER_BURST = 200;

    @Test
    public void snapshotsStayConsistentWhileEditorWrites() throws Exception {
        TextBuffer buffer = TextBuffer.create(seedText(600));
        AtomicInteger snapshots = new AtomicInteger();
        AtomicReference<Throwable> readerFailure = new AtomicReference<>();
        AtomicReference<Throwable> writerFailure = new AtomicReference<>();
        CountDownLatch stop = new CountDownLatch(1);
        ExecutorService crew = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "stress");
            thread.setDaemon(true);
            return thread;
        });
        try {
            Future<?> writer = crew.submit(() -> {
                Random random = new Random(7L);
                try {
                    while (stop.getCount() > 0) {
                        for (int i = 0; i < EDITS_PER_BURST; i++) {
                            // l'offset se calcule SOUS le verrou : le
                            // lecteur d'à côté fait grandir ou rétrécir
                            // le texte, une range lue avant l'écriture
                            // ne serait plus valide à l'application
                            boolean insertedNewline =
                                    random.nextInt(2) == 0;
                            buffer.snapshot((length, revision) -> {
                                int offset = random.nextInt(length);
                                int cut = Math.min(random.nextInt(4),
                                        length - offset);
                                UndoStack stack = new UndoStack(buffer, 64);
                                try (TextTransaction edit =
                                             stack.newTransaction()) {
                                    edit.replace(offset, offset + cut,
                                            insertedNewline ? "\n" : "xy");
                                }
                                return null;
                            });
                        }
                    }
                } catch (Throwable failure) {
                    writerFailure.set(failure);
                }
            });
            Future<?> reader = crew.submit(() -> {
                try {
                    while (stop.getCount() > 0) {
                        TextBuffer.RangeReader<Boolean> probe =
                                (length, revision) -> {
                                    // le cliché du moteur : texte entier,
                                    // bornes de chaque ligne, cohérence
                                    // longueur/révision d'un seul tenant
                                    String text = buffer.getText(0, length);
                                    int lines = buffer.getLineCount();
                                    for (int line = 0; line < lines; line++) {
                                        int start = buffer.getLineStart(line);
                                        int end = buffer.getLineEnd(line);
                                        assertTrue(start >= 0);
                                        assertTrue(start <= end);
                                        assertTrue(end <= length);
                                        if (line < lines - 1) {
                                            assertEquals('\n',
                                                    text.charAt(end));
                                        }
                                    }
                                    assertTrue(text.length() == length);
                                    snapshots.incrementAndGet();
                                    return true;
                                };
                        buffer.snapshot(probe);
                    }
                } catch (Throwable failure) {
                    readerFailure.set(failure);
                }
            });
            Thread.sleep(RACE_MILLIS);
            stop.countDown();
            writer.get(10L, TimeUnit.SECONDS);
            reader.get(10L, TimeUnit.SECONDS);
            assertTrue("le lecteur a pris " + snapshots.get()
                    + " cliché(s) cohérent(s), 0 attendu minimum",
                    snapshots.get() > 0);
            assertEquals("l'éditeur n'a pas levé", null, writerFailure.get());
            assertEquals("le lecteur n'a pas levé", null, readerFailure.get());
        } finally {
            crew.shutdownNow();
        }
    }

    /**
     * Un texte de départ à lignes régulières.
     *
     * @param lines le nombre de lignes
     * @return le texte, une ligne par indice
     */
    private static String seedText(int lines) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < lines; i++) {
            parts.add("ligne " + i + " du texte de course");
        }
        return String.join("\n", parts);
    }
}
