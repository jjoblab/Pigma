package jo.pigma.syntax.generic;

import java.util.Objects;

import jo.pigma.syntax.lexer.Lexer;

/**
 * Lexeur de la famille générique : un moteur d'analyse lexicale écrit
 * une fois, paramétré par un {@link LexerProfile}, conforme au modèle
 * du moteur (une fenêtre, un état de fin).
 *
 * <p>Une chaîne qui ferme dans la fenêtre forme un seul jeton ; une
 * chaîne ouverte à la fin de fenêtre rend son fragment et l'état de
 * fin porte la paire active ({@code 2 + indice de paire}), si bien
 * qu'une chaîne multi-lignes se referme à la fenêtre suivante. Un
 * commentaire de bloc ouvert se comporte de même (état 1). Seul
 * l'état 0 (code) est redémarrable.</p>
 *
 * <p>Types de jeton : 1 mot-clé, 2 identifiant (neutre), 3 nombre
 * (chiffres, point, lettres de suffixe et trait de soulignement),
 * 4 chaîne, 5 commentaire, 6 opérateur, 7 ponctuation, 8 blancs.</p>
 */
public final class GenericLexer implements Lexer {

    /** Type du mot-clé. */
    public static final int KEYWORD = 1;
    /** Type de l'identifiant (neutre). */
    public static final int IDENTIFIER = 2;
    /** Type du nombre. */
    public static final int NUMBER = 3;
    /** Type de la chaîne. */
    public static final int STRING = 4;
    /** Type du commentaire, de bloc comme de fin de ligne. */
    public static final int COMMENT = 5;
    /** Type de l'opérateur. */
    public static final int OPERATOR = 6;
    /** Type de la ponctuation. */
    public static final int PUNCT = 7;
    /** Type des blancs. */
    public static final int WHITESPACE = 8;

    /** État : code (seul état redémarrable). */
    public static final int STATE_CODE = 0;
    /** État : commentaire de bloc ouvert. */
    public static final int STATE_BLOCK = 1;
    /** Base des états de chaîne ouverte : base + indice de paire. */
    public static final int STATE_STRING_BASE = 2;

    private static final String OPERATOR_CHARS = "+-*/%=<>!&|^~";

    private final LexerProfile profile;

    private CharSequence text;
    private int end;
    private int position;
    private boolean atWindowEnd;
    private boolean producedToken;
    private int type;
    private int tokenStart;
    private int tokenEnd;

    private int entryState;
    private int exitState;

    /**
     * Un lexeur armé du profil donné.
     *
     * @param profile profil du langage, jamais {@code null}
     */
    public GenericLexer(LexerProfile profile) {
        this.profile = Objects.requireNonNull(profile);
    }

    @Override
    public void start(CharSequence newText, int newAt, int newEnd, int state) {
        text = Objects.requireNonNull(newText);
        if (newAt < 0 || newEnd < newAt || newEnd > newText.length()) {
            throw new IndexOutOfBoundsException(
                    "invalid window: [" + newAt + ", " + newEnd + ")");
        }
        end = newEnd;
        position = newAt;
        atWindowEnd = false;
        producedToken = false;
        type = NO_TOKEN;
        tokenStart = newAt;
        tokenEnd = newAt;
        entryState = normalize(state);
        exitState = entryState;
    }

    @Override
    public void advance() {
        if (atWindowEnd) {
            // la fenêtre est épuisée : le type du dernier jeton ne doit
            // plus être visible, sinon le moteur le relirait sans fin
            type = NO_TOKEN;
            tokenStart = end;
            tokenEnd = end;
            return;
        }
        if (position >= end) {
            // fenêtre vide ou épuisée : pas de jeton, l'état se reporte
            atWindowEnd = true;
            type = NO_TOKEN;
            tokenStart = end;
            tokenEnd = end;
            return;
        }
        int dispatchState = producedToken ? exitState : entryState;
        if (dispatchState == STATE_BLOCK) {
            scanBlockChunk();
        } else if (dispatchState >= STATE_STRING_BASE) {
            scanStringChunk(dispatchState - STATE_STRING_BASE);
        } else {
            scanCode();
        }
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
        return exitState;
    }

    // ------------------------------------------------------------ interne

    /**
     * Ramène l'état reçu aux états connus : commentaire de bloc, paire
     * de chaîne, sinon code.
     */
    private int normalize(int state) {
        if (state == STATE_BLOCK) {
            return STATE_BLOCK;
        }
        if (state >= STATE_STRING_BASE
                && state < STATE_STRING_BASE + profile.stringPairs().size()) {
            return state;
        }
        return STATE_CODE;
    }

    private void scanBlockChunk() {
        tokenStart = position;
        String close = profile.blockCommentClose();
        if (matchesHere(close)) {
            position += close.length();
        } else {
            int stop = findAhead(close);
            position = stop < 0 ? end : stop;
        }
        finishToken(COMMENT);
    }

    /**
     * Le fragment de chaîne ouvert dans une fenêtre précédente : il
     * court jusqu'au fermant de la paire active, échappements
     * respectés, fermant compris.
     */
    private void scanStringChunk(int pairIndex) {
        String pair = profile.stringPairs().get(pairIndex);
        tokenStart = position;
        int closeAt = scanStringEnd(position, pair);
        position = closeAt < 0 ? end : closeAt + 1;
        finishToken(STRING);
    }

    private void scanCode() {
        tokenStart = position;
        char current = text.charAt(position);

        for (String prefix : profile.lineComments()) {
            if (matchesHere(prefix)) {
                position = end;
                finishToken(COMMENT);
                return;
            }
        }
        if (profile.blockCommentOpen() != null
                && matchesHere(profile.blockCommentOpen())) {
            position += profile.blockCommentOpen().length();
            finishToken(COMMENT);
            return;
        }
        int pairIndex = pairOpeningAt(position);
        if (pairIndex >= 0) {
            String pair = profile.stringPairs().get(pairIndex);
            int closeAt = scanStringEnd(position + 1, pair);
            position = closeAt < 0 ? end : closeAt + 1;
            finishToken(STRING);
            return;
        }
        if (Character.isWhitespace(current)) {
            while (position < end && Character.isWhitespace(text.charAt(position))) {
                position++;
            }
            finishToken(WHITESPACE);
            return;
        }
        if (Character.isDigit(current)) {
            while (position < end && isNumberPart(text.charAt(position))) {
                position++;
            }
            finishToken(NUMBER);
            return;
        }
        if (Character.isJavaIdentifierStart(current)) {
            while (position < end
                    && Character.isJavaIdentifierPart(text.charAt(position))) {
                position++;
            }
            if (profile.isKeywordAt(text, tokenStart, position)) {
                finishToken(KEYWORD);
            } else {
                finishToken(IDENTIFIER);
            }
            return;
        }
        if (OPERATOR_CHARS.indexOf(current) >= 0) {
            while (position < end
                    && OPERATOR_CHARS.indexOf(text.charAt(position)) >= 0) {
                position++;
            }
            finishToken(OPERATOR);
            return;
        }
        position++;
        finishToken(PUNCT);
    }

    /**
     * Offset du fermant de la paire depuis {@code from}, échappements
     * respectés, -1 si la fenêtre se ferme avant.
     */
    private int scanStringEnd(int from, String pair) {
        char close = pair.charAt(1);
        char escape = profile.escapeChar();
        int i = from;
        while (i < end) {
            char current = text.charAt(i);
            if (escape != 0 && current == escape && i + 1 < end) {
                i += 2;
                continue;
            }
            if (current == close) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private int pairOpeningAt(int offset) {
        char candidate = text.charAt(offset);
        for (int i = 0; i < profile.stringPairs().size(); i++) {
            if (profile.stringPairs().get(i).charAt(0) == candidate) {
                return i;
            }
        }
        return -1;
    }

    private boolean matchesHere(String word) {
        if (word == null || position + word.length() > end) {
            return false;
        }
        for (int i = 0; i < word.length(); i++) {
            if (text.charAt(position + i) != word.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private int findAhead(String word) {
        for (int i = position; i + word.length() <= end; i++) {
            boolean same = true;
            for (int j = 0; j < word.length(); j++) {
                if (text.charAt(i + j) != word.charAt(j)) {
                    same = false;
                    break;
                }
            }
            if (same) {
                return i;
            }
        }
        return -1;
    }

    private boolean isNumberPart(char candidate) {
        return Character.isLetterOrDigit(candidate) || candidate == '.' || candidate == '_';
    }

    /**
     * Clôt le jeton et calcule l'état de fin : un commentaire ne
     * s'ouvre que s'il ne porte pas son fermant, une chaîne que si le
     * fermant de SA paire manque dans le jeton.
     */
    private void finishToken(int newType) {
        type = newType;
        tokenEnd = position;
        producedToken = true;
        exitState = carriedExitState();
        if (position >= end) {
            atWindowEnd = true;
        }
    }

    private int carriedExitState() {
        if (type == COMMENT) {
            String close = profile.blockCommentClose();
            return close != null && endsWith(tokenStart, tokenEnd, close)
                    ? STATE_CODE : STATE_BLOCK;
        }
        if (type == STRING) {
            int pairIndex = openingPairOfToken();
            if (pairIndex >= 0) {
                return STATE_STRING_BASE + pairIndex;
            }
        }
        return STATE_CODE;
    }

    /**
     * L'indice de la paire dont la chaîne du jeton courant est encore
     * ouverte, -1 si fermée : l'ouvrant doit ouvrir le jeton et le
     * fermant de la paire doit manquer.
     */
    private int openingPairOfToken() {
        char opener = text.charAt(tokenStart);
        for (int i = 0; i < profile.stringPairs().size(); i++) {
            String pair = profile.stringPairs().get(i);
            if (pair.charAt(0) != opener) {
                continue;
            }
            int closeAt = scanStringEnd(tokenStart + 1, pair);
            if (closeAt < 0 || closeAt >= tokenEnd) {
                return i;
            }
        }
        return -1;
    }

    private boolean endsWith(int from, int to, String word) {
        if (to - from < word.length()) {
            return false;
        }
        for (int i = 0; i < word.length(); i++) {
            if (text.charAt(to - word.length() + i) != word.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
