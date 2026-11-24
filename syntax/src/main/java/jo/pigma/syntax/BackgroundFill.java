package jo.pigma.syntax;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fil de fond du remplissage : les tranches se prennent en cliché
 * ({@link SyntaxDocument#takeFillSlice(int)}), se lexent hors verrou
 * ({@link SyntaxDocument#lexFillSlice(FillSlice)}) et ne s'appliquent
 * que si la révision observée du moteur correspond encore au cliché
 * ({@link SyntaxDocument#applyFill(FillSlice, LineTokens[])}) — une
 * frappe entre deux tranches annule la tranche, jamais le document.
 *
 * <p>Le fil est unique, nommé, démon : il ne survit pas seul à la
 * bibliothèque. {@link #kick()} relance la boucle après une ouverture,
 * une édition, une demande — les relances en trop s'évanouissent, la
 * boucle s'arrête d'elle-même quand le préfixe est complet.
 * {@link #stop()} est l'annulation douce : la tranche en cours finit,
 * aucune ne repart ; un fill arrêté ne repart pas.</p>
 */
public final class BackgroundFill {

    /** Lignes par tranche du fil de fond. */
    static final int SLICE_LINES = 256;

    private final SyntaxDocument document;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ExecutorService executor;

    private BackgroundFill(SyntaxDocument document) {
        this.document = document;
        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "pigma-fill");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Démarre le fil de fond d'un document : aucune tranche ne part
     * avant {@link #kick()}.
     *
     * @param document le moteur à remplir, jamais {@code null}
     * @return le fil prêt
     */
    public static BackgroundFill start(SyntaxDocument document) {
        Objects.requireNonNull(document);
        return new BackgroundFill(document);
    }

    /**
     * Relance la boucle si du travail reste : après l'ouverture, une
     * édition, une demande. Sans effet sur un fil arrêté.
     */
    public void kick() {
        if (running.get() && document.remainingLines() > 0) {
            executor.execute(this::loop);
        }
    }

    /**
     * Annulation douce : la tranche en cours finit, aucune ne repart,
     * le fil se termine. Un fill arrêté ne repart pas.
     */
    public void stop() {
        running.set(false);
        executor.shutdown();
    }

    /**
     * Le fil est-il arrêté ?
     *
     * @return vrai après {@link #stop()}
     */
    public boolean stopped() {
        return !running.get();
    }

    /**
     * Attend que le préfixe soit complet (harnais, tests) : le
     * remplissage par clichés ne laisse jamais le document incohérent,
     * cette attente n'est qu'une commodité d'observation.
     *
     * @param timeoutMillis borne d'attente en millisecondes
     * @return vrai si le préfixe est complet au retour
     */
    public boolean awaitIdle(long timeoutMillis) {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (document.remainingLines() > 0) {
            if (System.nanoTime() >= deadline) {
                return false;
            }
            try {
                Thread.sleep(5L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return document.remainingLines() == 0;
            }
        }
        return true;
    }

    private void loop() {
        while (running.get()) {
            SyntaxDocument.FillSlice slice = document.takeFillSlice(SLICE_LINES);
            if (slice == null) {
                return;
            }
            try {
                SyntaxDocument.LineTokens[] entries = document.lexFillSlice(slice);
                document.applyFill(slice, entries);
            } catch (RuntimeException unexpected) {
                // un cliché déchiré par une édition concomitante peut
                // lever avant l'application ; la boucle s'arrête et le
                // prochain kick (chaque édition, chaque dessin) repart
                // sur un cliché propre
                return;
            }
        }
    }
}
