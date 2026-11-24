package jo.pigma.syntax;

import java.util.Objects;

import jo.pigma.core.TextBuffer;
import jo.pigma.core.TextListener;
import jo.pigma.syntax.lexer.Lexer;

/**
 * Moteur de coloration d'un document : les jetons ligne par ligne,
 * tenus à jour à l'édition par la règle d'arrêt.
 *
 * <p>Chaque ligne porte ses jetons (offsets relatifs au début de
 * ligne), son état de fin de lexeur — celui qui ouvre la ligne
 * suivante, un commentaire de bloc ouvert traversant ainsi les lignes —
 * et la révision du tampon à laquelle ses jetons ont été calculés : la
 * comparaison d'un entier suffit à savoir qu'un rendu cached est
 * périmé. Après un remplacement, on relexe les lignes de la zone
 * remplacée puis on continue tant que l'état de fin d'une ligne
 * change ; quand il se stabilise, la suite des entrées est encore
 * valide (même état d'entrée, même contenu) et ne se relexe pas.</p>
 *
 * <p>La première demande lexte tout le document ; la tokenisation
 * paresseuse par fenêtre (ne lexe que le préfixe demandé) suivra avec
 * les points de contrôle. Le moteur vit sur le fil qui édite le
 * tampon ; les diagnostics (premier lexage, dernier relexage, nombre
 * de lignes relues) servent le panneau de mesures du sample.</p>
 */
public final class SyntaxDocument implements TextListener {

    /** Un jeton d'une ligne : type, portée, bornes relatives à la ligne. */
    public record Token(int type, Scope scope, int start, int end) {
    }

    /** Les jetons d'une ligne : état de fin ouvrant la ligne suivante. */
    public record LineTokens(Token[] tokens, int endState, int revision) {

        /** Jetons de la ligne, éventuellement aucun. */
        public Token[] tokens() {
            return tokens.clone();
        }
    }

    private final TextBuffer buffer;
    private final LanguageSyntax language;
    private final Lexer lexer;

    private LineTokens[] lines;
    private boolean pendingFirstPass = true;

    private long firstLexNanos;
    private long lastEditNanos;
    private int lastRelexedLines;

    private SyntaxDocument(TextBuffer buffer, LanguageSyntax language) {
        this.buffer = buffer;
        this.language = language;
        this.lexer = language.newLexer();
    }

    /**
     * Ouvre le document : le moteur écoute le tampon jusqu'au
     * {@link #detach()}.
     *
     * @param buffer   tampon du document, jamais {@code null}
     * @param language descripteur du langage, jamais {@code null}
     * @return le moteur prêt, premier lexage à la première demande
     */
    public static SyntaxDocument open(TextBuffer buffer, LanguageSyntax language) {
        Objects.requireNonNull(buffer);
        Objects.requireNonNull(language);
        SyntaxDocument document = new SyntaxDocument(buffer, language);
        buffer.addListener(document);
        return document;
    }

    /**
     * Détache le moteur du tampon : plus aucun lexage, plus aucune
     * écoute. Un moteur détaché ne se rattache pas.
     */
    public void detach() {
        buffer.removeListener(this);
    }

    /**
     * Jetons de la ligne demandée (le premier appel lexte tout le
     * document ; les appels suivants suivent la règle d'arrêt).
     *
     * @param line numéro de ligne, entre 0 et le compte de lignes exclu
     * @return les jetons, jamais {@code null}
     */
    public LineTokens lineTokens(int line) {
        if (pendingFirstPass) {
            firstPass();
        }
        if (line < 0 || line >= lines.length) {
            throw new IndexOutOfBoundsException(
                    "line out of range: " + line + " for " + lines.length + " lines");
        }
        return lines[line];
    }

    /**
     * Nombre de lignes lexées (celles du tampon).
     *
     * @return le compte de lignes
     */
    public int lineCount() {
        if (pendingFirstPass) {
            firstPass();
        }
        return lines.length;
    }

    /**
     * Total des jetons du document (diagnostic : parcourt les lignes).
     *
     * @return le nombre de jetons
     */
    public int tokenCount() {
        if (pendingFirstPass) {
            firstPass();
        }
        int count = 0;
        for (LineTokens entry : lines) {
            count += entry.tokens.length;
        }
        return count;
    }

    /**
     * Durée du premier lexage complet, en nanosecondes (diagnostic).
     *
     * @return la durée, 0 si le document n'a pas encore été lexé
     */
    public long firstLexNanos() {
        return firstLexNanos;
    }

    /**
     * Durée du dernier relexage incrémental, en nanosecondes
     * (diagnostic).
     *
     * @return la durée, 0 si aucune édition n'a eu lieu
     */
    public long lastEditNanos() {
        return lastEditNanos;
    }

    /**
     * Nombre de lignes relues au dernier relexage (diagnostic) : la
     * zone remplacée, plus la propagation de la règle d'arrêt.
     *
     * @return le compte, 0 si aucune édition n'a eu lieu
     */
    public int lastRelexedLines() {
        return lastRelexedLines;
    }

    @Override
    public void textChanged(TextBuffer changed, int start, int removedLength,
            int insertedLength, int revision) {
        if (pendingFirstPass) {
            // Rien n'est calculé : la prochaine demande lexera l'état
            // courant du tampon en entier.
            return;
        }
        long begin = System.nanoTime();
        int first = buffer.lineOfOffset(start);
        // l'entrée de la ligne éditée sert au test d'arrêt : la
        // restructuration la remplace par une case à relexer
        LineTokens oldFirst = lines[first];
        restructure(first, removedLength, insertedLength);
        int state = first == 0 ? 0 : lines[first - 1].endState();
        int relexed = 0;
        for (int i = first; i < lines.length; i++) {
            LineTokens old = i == first ? oldFirst : lines[i];
            LineTokens fresh = lexLine(i, state, revision);
            lines[i] = fresh;
            relexed++;
            if (old != null && old.endState() == fresh.endState()) {
                break;
            }
            state = fresh.endState();
        }
        lastRelexedLines = relexed;
        lastEditNanos = System.nanoTime() - begin;
    }

    // ------------------------------------------------------------ interne

    private void firstPass() {
        long begin = System.nanoTime();
        int count = buffer.getLineCount();
        lines = new LineTokens[count];
        int state = 0;
        for (int i = 0; i < count; i++) {
            LineTokens entry = lexLine(i, state, buffer.getRevision());
            lines[i] = entry;
            state = entry.endState();
        }
        pendingFirstPass = false;
        firstLexNanos = System.nanoTime() - begin;
    }

    /**
     * Recale le tableau des lignes sur le remplacement : la zone
     * remplacée disparaît (ses entrées deviennent {@code null}, à
     * relexer), la queue glisse sans être relue.
     */
    private void restructure(int first, int removedLength, int insertedLength) {
        int newCount = buffer.getLineCount();
        int oldCount = lines.length;
        int insertedBreaks = countBreaks(first, insertedLength);
        int removedBreaks = insertedBreaks - (newCount - oldCount);
        int removeCount = removedBreaks + 1;
        int insertCount = insertedBreaks + 1;
        if (removeCount < 1) {
            // Les retours à la ligne retirés ne peuvent être négatifs :
            // un tampon bien formé tient l'invariant, on refuse de
            // corrompre le tableau par prudence.
            throw new IllegalStateException("negative removed lines: " + removedBreaks);
        }
        LineTokens[] recalibrated = new LineTokens[oldCount - removeCount + insertCount];
        System.arraycopy(lines, 0, recalibrated, 0, first);
        int tailSource = first + removeCount;
        System.arraycopy(lines, tailSource, recalibrated, first + insertCount,
                oldCount - tailSource);
        lines = recalibrated;
    }

    /** Nombre de sauts de ligne du texte inséré par le remplacement. */
    private int countBreaks(int first, int insertedLength) {
        int start = buffer.getLineStart(first);
        int breaks = 0;
        for (int i = 0; i < insertedLength; i++) {
            if (buffer.charAt(start + i) == '\n') {
                breaks++;
            }
        }
        return breaks;
    }

    private LineTokens lexLine(int line, int state, int revision) {
        int start = buffer.getLineStart(line);
        int end = buffer.getLineEnd(line);
        lexer.start(buffer, start, end, state);
        Token[] collected = new Token[end - start > 0 ? 4 : 0];
        int count = 0;
        lexer.advance();
        while (lexer.tokenType() != Lexer.NO_TOKEN) {
            if (count == collected.length) {
                collected = java.util.Arrays.copyOf(collected, Math.max(4, count * 2));
            }
            collected[count++] = new Token(lexer.tokenType(),
                    language.scopeOf(lexer.tokenType()),
                    lexer.tokenStart() - start,
                    lexer.tokenEnd() - start);
            lexer.advance();
        }
        return new LineTokens(java.util.Arrays.copyOf(collected, count),
                lexer.state(), revision);
    }
}
