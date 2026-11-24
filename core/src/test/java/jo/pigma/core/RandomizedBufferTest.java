package jo.pigma.core;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test différentiel aléatoire : le tampon et sa pile d'annulation contre
 * un modèle de référence écrit directement sur {@link StringBuilder}. Les
 * suites d'éditions, annulations, rétablissements et retours arrière
 * doivent produire exactement le même texte, la même structure de lignes
 * et des conversions offset/ligne justes.
 */
public class RandomizedBufferTest {

    private static final int OPERATIONS_PER_SEED = 600;

    /** Graines fixes : toute régression se reproduit à l'identique. */
    @Test
    public void randomOpsMatchReferenceModel() {
        runWithSeed(20261124L);
        runWithSeed(42L);
        runWithSeed(987654321L);
        runWithSeed(1337L);
    }

    private static void runWithSeed(long seed) {
        Random random = new Random(seed);
        TextBuffer buffer = TextBuffer.create("ligne un\nligne deux\n");
        UndoStack stack = new UndoStack(buffer, 10000);
        ReferenceModel model = new ReferenceModel("ligne un\nligne deux\n");
        int lastRevision = buffer.getRevision();

        for (int op = 0; op < OPERATIONS_PER_SEED; op++) {
            int kind = random.nextInt(100);
            if (kind < 45) {
                int offset = randomOffset(random, model);
                String text = randomText(random);
                applyBoth(stack, model, offset, offset, text);
            } else if (kind < 70) {
                int[] range = randomRange(random, model);
                applyBoth(stack, model, range[0], range[1], "");
            } else if (kind < 85) {
                int[] range = randomRange(random, model);
                String text = randomText(random);
                applyBoth(stack, model, range[0], range[1], text);
            } else if (kind < 93) {
                boolean modelUndone = model.undoOne();
                boolean bufferUndone = bufferUndo(stack);
                assertEquals("annulation, graine " + seed, modelUndone, bufferUndone);
            } else if (kind < 98) {
                boolean modelRedone = model.redoOne();
                boolean bufferRedone = bufferRedo(stack);
                assertEquals("rétablissement, graine " + seed, modelRedone, bufferRedone);
            } else {
                rollbackRandomTransaction(random, stack, model);
            }
            verifyAgainstModel(buffer, stack, model, seed, op);
            assertTrue(buffer.getRevision() >= lastRevision);
            lastRevision = buffer.getRevision();
            if (op % 50 == 0) {
                verifyOffsetToLineConversions(random, buffer, model);
            }
        }
    }

    // ----------------------------------------------------------- actions

    private static void applyBoth(UndoStack stack, ReferenceModel model,
                                  int start, int end, String text) {
        ReferenceModel.Entry entry = model.edit(start, end, text);
        try (TextTransaction transaction = stack.newTransaction()) {
            transaction.replace(start, end, text);
        }
        if (entry != null) {
            model.commit(entry);
        }
    }

    private static boolean bufferUndo(UndoStack stack) {
        return !stack.undo().isEmpty();
    }

    private static boolean bufferRedo(UndoStack stack) {
        return !stack.redo().isEmpty();
    }

    private static void rollbackRandomTransaction(Random random, UndoStack stack,
                                                  ReferenceModel model) {
        List<ReferenceModel.Entry> entries = new ArrayList<>();
        try (TextTransaction transaction = stack.newTransaction()) {
            for (int i = 0; i < 1 + random.nextInt(3); i++) {
                int[] range = randomRange(random, model);
                String text = randomText(random);
                transaction.replace(range[0], range[1], text);
                ReferenceModel.Entry entry = model.edit(range[0], range[1], text);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            transaction.rollback();
        }
        for (int i = entries.size() - 1; i >= 0; i--) {
            model.reverse(entries.get(i));
        }
    }

    // ------------------------------------------------------- vérifications

    private static void verifyAgainstModel(TextBuffer buffer, UndoStack stack,
                                           ReferenceModel model, long seed, int op) {
        assertEquals("texte, graine " + seed + " opération " + op,
                model.text.toString(), buffer.toString());
        assertEquals("révision, graine " + seed + " opération " + op,
                model.text.length(), buffer.length());
        assertEquals("nombre de lignes, graine " + seed + " opération " + op,
                model.lineCount(), buffer.getLineCount());
        for (int line = 0; line < model.lineCount(); line++) {
            assertEquals("début de ligne " + line + ", graine " + seed,
                    model.lineStart(line), buffer.getLineStart(line));
        }
        assertEquals("annulable, graine " + seed + " opération " + op,
                model.canUndo(), stack.canUndo());
        assertEquals("rétablissable, graine " + seed + " opération " + op,
                model.canRedo(), stack.canRedo());
    }

    private static void verifyOffsetToLineConversions(Random random, TextBuffer buffer,
                                                      ReferenceModel model) {
        for (int probe = 0; probe < 3; probe++) {
            int offset = random.nextInt(model.text.length() + 1);
            assertEquals(model.lineOfOffset(offset), buffer.lineOfOffset(offset));
            assertEquals(offset - model.lineStart(model.lineOfOffset(offset)),
                    buffer.columnOfOffset(offset));
            TextPosition position = buffer.positionOfOffset(offset);
            assertEquals(offset, buffer.offsetOfPosition(position.line(), position.column()));
        }
    }

    // ------------------------------------------------------------ aléatoire

    private static int randomOffset(Random random, ReferenceModel model) {
        return model.text.isEmpty() ? 0 : random.nextInt(model.text.length() + 1);
    }

    private static int[] randomRange(Random random, ReferenceModel model) {
        int length = model.text.length();
        if (length == 0) {
            return new int[] {0, 0};
        }
        int start = random.nextInt(length + 1);
        int end = start + random.nextInt(length + 1 - start);
        return new int[] {start, end};
    }

    private static String randomText(Random random) {
        int length = random.nextInt(8);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int kind = random.nextInt(100);
            if (kind < 10) {
                builder.append('\n');
            } else if (kind < 15) {
                builder.append('\r');
            } else if (kind < 30) {
                builder.append((char) (0xD800 + random.nextInt(0x400)));
                builder.append((char) (0xDC00 + random.nextInt(0x400)));
            } else if (kind < 33) {
                builder.append((char) (0xD800 + random.nextInt(0x400)));
            } else {
                builder.append((char) ('a' + random.nextInt(26)));
            }
        }
        return builder.toString();
    }

    // --------------------------------------------------------------- modèle

    /**
     * Modèle de référence : texte naïf et journaux d'annulation écrits à la
     * main, indépendants du code sous test.
     */
    private static final class ReferenceModel {

        /** Une édition du modèle : plage remplacée et les deux textes. */
        record Entry(int start, String removed, String inserted) {
        }

        final StringBuilder text = new StringBuilder();
        final Deque<Entry> undoLog = new ArrayDeque<>();
        final Deque<Entry> redoLog = new ArrayDeque<>();

        ReferenceModel(String initialText) {
            text.append(initialText);
        }

        Entry edit(int start, int end, String replacement) {
            String removed = text.substring(start, end);
            text.replace(start, end, replacement);
            if (removed.isEmpty() && replacement.isEmpty()) {
                return null;
            }
            return new Entry(start, removed, replacement);
        }

        void commit(Entry entry) {
            undoLog.addLast(entry);
            redoLog.clear();
        }

        boolean undoOne() {
            if (undoLog.isEmpty()) {
                return false;
            }
            Entry entry = undoLog.removeLast();
            reverse(entry);
            redoLog.addLast(entry);
            return true;
        }

        boolean redoOne() {
            if (redoLog.isEmpty()) {
                return false;
            }
            Entry entry = redoLog.removeLast();
            text.replace(entry.start(), entry.start() + entry.removed().length(),
                    entry.inserted());
            undoLog.addLast(entry);
            return true;
        }

        void reverse(Entry entry) {
            text.replace(entry.start(), entry.start() + entry.inserted().length(),
                    entry.removed());
        }

        boolean canUndo() {
            return !undoLog.isEmpty();
        }

        boolean canRedo() {
            return !redoLog.isEmpty();
        }

        int lineCount() {
            int count = 1;
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    count++;
                }
            }
            return count;
        }

        int lineStart(int line) {
            if (line == 0) {
                return 0;
            }
            int current = 0;
            for (int i = 0; i < text.length() && current < line; i++) {
                if (text.charAt(i) == '\n') {
                    current++;
                    if (current == line) {
                        return i + 1;
                    }
                }
            }
            return text.length();
        }

        int lineOfOffset(int offset) {
            int line = 0;
            for (int i = 0; i < offset; i++) {
                if (text.charAt(i) == '\n') {
                    line++;
                }
            }
            return line;
        }
    }
}
