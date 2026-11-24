package jo.pigma.syntax.lexer;

import java.io.IOException;
import java.util.Objects;

/**
 * Fenêtre de lecture en mémoire : le lexeur généré lit le fragment
 * {@code [at, end)} d'un texte comme un flux de caractères, sans
 * copie. Le lecteur se réarme à chaque {@link #over(CharSequence, int,
 * int)} — l'adaptateur le réutilise pour éviter une allocation par
 * fenêtre.
 */
final class WindowReader extends java.io.Reader {

    private CharSequence text;
    private int at;
    private int end;

    /**
     * Arme le lecteur sur la fenêtre donnée.
     *
     * @param newText le texte porteur, jamais {@code null}
     * @param newAt   début de la fenêtre, inclus
     * @param newEnd  fin de la fenêtre, exclue
     */
    void over(CharSequence newText, int newAt, int newEnd) {
        text = Objects.requireNonNull(newText);
        if (newAt < 0 || newEnd < newAt || newEnd > newText.length()) {
            throw new IndexOutOfBoundsException(
                    "invalid window: [" + newAt + ", " + newEnd + ")");
        }
        at = newAt;
        end = newEnd;
    }

    @Override
    public int read() throws IOException {
        ensureArmed();
        return at < end ? text.charAt(at++) : -1;
    }

    @Override
    public int read(char[] buffer, int offset, int length) throws IOException {
        ensureArmed();
        if (buffer == null) {
            throw new NullPointerException("buffer must not be null");
        }
        if (offset < 0 || length < 0 || offset + length > buffer.length) {
            throw new IndexOutOfBoundsException("invalid read request");
        }
        if (length == 0) {
            return 0;
        }
        int available = end - at;
        if (available <= 0) {
            return -1;
        }
        int count = Math.min(length, available);
        for (int i = 0; i < count; i++) {
            buffer[offset + i] = text.charAt(at + i);
        }
        at += count;
        return count;
    }

    @Override
    public void close() {
        // Rien à fermer : la fenêtre vit dans le texte du tampon.
    }

    private void ensureArmed() throws IOException {
        if (text == null) {
            throw new IOException("reader not armed on a window");
        }
    }
}
