package jo.pigma.syntax.lexer;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Objects;

import jo.pigma.syntax.lexers.generated.LuaLexer;

/**
 * Adaptateur du lexeur Lua généré vers le contrat {@link Lexer}. Même
 * armage de fenêtre que {@link FlexLexerAdapter} ; la différence vit
 * dans l'état : le contexte Lua se compose d'un état plat (longue
 * chaîne, longue commentaire) et du niveau de crochets (« [==[ »),
 * encode dans un seul entier par le lexeur généré. {@code yyreset}
 * laisse l'état intact : {@code restoreContext} reprend le plat et le
 * niveau depuis l'entier transporté par le moteur.
 */
public final class LuaLexerAdapter implements Lexer {

    private final LuaLexer scanner;
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
     * Un adaptateur par lexeur Lua généré : la fabrique du descripteur
     * en rend un neuf à chaque demande.
     */
    public LuaLexerAdapter() {
        scanner = new LuaLexer(new StringReader(""));
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
        scanner.restoreContext(state);
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
        return scanner.contextState();
    }
}
