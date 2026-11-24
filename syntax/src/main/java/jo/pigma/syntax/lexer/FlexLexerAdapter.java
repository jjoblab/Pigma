package jo.pigma.syntax.lexer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Adaptateur d'un lexeur généré vers le contrat {@link Lexer} : il
 * arme la fenêtre de lecture, traduit la fin de fenêtre en
 * {@link Lexer#NO_TOKEN} et accumule les offsets absolus (les jetons
 * d'une fenêtre couvrent le texte sans trou, la description couvre
 * chaque caractère, caractère de repli compris).
 */
public final class FlexLexerAdapter implements Lexer {

    private final FlexLexer scanner;
    private final WindowReader reader = new WindowReader();

    private CharSequence text;
    private int at;
    private int end;
    private int position;
    private boolean atWindowEnd;
    private int type;
    private int tokenStart;
    private int tokenEnd;

    /**
     * Un adaptateur par fabrique de lexeur généré.
     *
     * @param scannerFactory fabrique du scanner généré, jamais {@code null}
     */
    public FlexLexerAdapter(Supplier<? extends FlexLexer> scannerFactory) {
        Objects.requireNonNull(scannerFactory);
        scanner = scannerFactory.get();
    }

    @Override
    public void start(CharSequence newText, int newAt, int newEnd, int state) {
        text = Objects.requireNonNull(newText);
        if (newAt < 0 || newEnd < newAt || newEnd > newText.length()) {
            throw new IndexOutOfBoundsException(
                    "invalid window: [" + newAt + ", " + newEnd + ")");
        }
        at = newAt;
        end = newEnd;
        reader.over(newText, newAt, newEnd);
        scanner.yyreset(reader);
        scanner.yybegin(state);
        position = newAt;
        atWindowEnd = false;
        type = NO_TOKEN;
        tokenStart = newAt;
        tokenEnd = newAt;
    }

    @Override
    public void advance() {
        if (atWindowEnd) {
            return;
        }
        int raw;
        try {
            raw = scanner.yylex();
        } catch (IOException e) {
            throw new UncheckedIOException("lexer window read failed", e);
        }
        if (raw == FlexLexer.FLEX_EOF) {
            atWindowEnd = true;
            type = NO_TOKEN;
            tokenStart = end;
            tokenEnd = end;
            return;
        }
        type = raw;
        tokenStart = position;
        position += scanner.yylength();
        tokenEnd = position;
    }

    @Override
    public int tokenType() {
        return type;
    }

    @Override
    public int tokenStart() {
        return tokenStart;
    }

    @Override
    public int tokenEnd() {
        return tokenEnd;
    }

    @Override
    public int state() {
        return scanner.yystate();
    }
}
