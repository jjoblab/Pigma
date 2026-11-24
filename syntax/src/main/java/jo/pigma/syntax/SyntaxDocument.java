package jo.pigma.syntax;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jo.pigma.core.TextBuffer;
import jo.pigma.core.TextListener;
import jo.pigma.syntax.lexer.Lexer;

/**
 * Moteur de coloration d'un document : les jetons ligne par ligne,
 * calculés paresseusement jusqu'à la dernière ligne demandée, tenus à
 * jour à l'édition par la règle d'arrêt.
 *
 * <p>Ouvrir ne lexe rien : {@link #lineTokens(int)} étend le préfixe
 * calculé jusqu'à la ligne demandée — la vue qui ne dessine que les
 * lignes visibles ne paie que ce qu'elle montre. Chaque ligne porte
 * ses jetons (offsets relatifs au début de ligne), son état de fin de
 * lexeur — celui qui ouvre la ligne suivante, un commentaire de bloc
 * ouvert traversant ainsi les lignes — et la révision du tampon à
 * laquelle ses jetons ont été calculés : la comparaison d'un entier
 * suffit à savoir qu'un rendu caché est périmé.</p>
 *
 * <p>Après un remplacement, on relexe les lignes de la zone remplacée
 * puis on continue tant que l'état de fin d'une ligne change ; quand
 * il se stabilise, la suite des entrées est encore valide (même état
 * d'entrée, même contenu) et ne se relexe pas. Des points de contrôle
 * d'état (une ligne sur {@value #CHECKPOINT_INTERVAL}) se posent en
 * lexant : un saut lointain repart du plus proche, et une édition qui
 * propage un état jusqu'à une zone jamais lexée y pose un point de
 * contrôle plutôt que de lexe le reste. {@link #releaseEntries(int)}
 * laisse la vue libérer les entrées lointaines : les points de
 * contrôle restent et servent le retour. {@link #advanceLexing(int)}
 * remplit par tranches bornées — le budget de temps du fil
 * d'interface ; un vrai fil de fond annulable, dont les résultats ne
 * s'appliqueraient que si la révision du tampon correspond, viendra
 * avec la vue complète : le moteur vit pour l'instant sur le fil qui
 * édite le tampon.</p>
 *
 * <p>Plafonds : une ligne de plus de {@value #MAX_COLORED_COLUMNS}
 * colonnes n'est pas colorée (jeton aucun, état reporté tel quel) ;
 * au-delà de {@value #BIG_FILE_CHARACTERS} caractères ou
 * {@value #BIG_FILE_LINES} lignes, {@link #inBigFileMode()} signale
 * le mode gros fichier — la coloration lexicale (L1) n'y est jamais
 * coupée, les couches L2 et L3 à venir s'y couperont.</p>
 */
public final class SyntaxDocument implements TextListener {

    /** Colonnes au-delà desquelles une ligne n'est plus colorée. */
    public static final int MAX_COLORED_COLUMNS = 20_000;

    /** Caractères au-delà desquels le mode gros fichier s'allume. */
    public static final long BIG_FILE_CHARACTERS = 2_500_000L;

    /** Lignes au-delà desquelles le mode gros fichier s'allume. */
    public static final int BIG_FILE_LINES = 50_000;

    /** Une ligne sur ce nombre reçoit un point de contrôle d'état. */
    static final int CHECKPOINT_INTERVAL = 256;

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

    /** Point de contrôle : l'état qui ouvre la ligne, à cette révision. */
    private record Checkpoint(int state, int revision) {
    }

    private final TextBuffer buffer;
    private final LanguageSyntax language;
    private final Lexer lexer;
    private final Map<Integer, Checkpoint> checkpoints = new HashMap<>();

    private LineTokens[] lines;
    private int lexedUpto;

    private boolean measuredFirstRequest;
    private long firstLexNanos;
    private long lastEditNanos;
    private int lastRelexedLines;
    private int lastRequestLines;

    private SyntaxDocument(TextBuffer buffer, LanguageSyntax language) {
        this.buffer = buffer;
        this.language = language;
        this.lexer = language.newLexer();
        this.lines = new LineTokens[buffer.getLineCount()];
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
     * Jetons de la ligne demandée : le calcul étend le préfixe lexé
     * jusqu'à elle (au besoin depuis le dernier point de contrôle), il
     * ne lexe jamais au-delà.
     *
     * @param line numéro de ligne, entre 0 et le compte de lignes exclu
     * @return les jetons, jamais {@code null} (aucun jeton pour une
     *         ligne vide ou une ligne hors plafond)
     */
    public LineTokens lineTokens(int line) {
        int count = buffer.getLineCount();
        if (line < 0 || line >= count) {
            throw new IndexOutOfBoundsException(
                    "line out of range: " + line + " for " + count + " lines");
        }
        if (lines[line] != null) {
            lastRequestLines = 0;
            return lines[line];
        }
        long begin = System.nanoTime();
        int from = restartPoint(line);
        int state = restartState(from);
        int revision = buffer.getRevision();
        for (int i = from; i <= line; i++) {
            LineTokens fresh = lexLine(i, state, revision);
            lines[i] = fresh;
            state = fresh.endState();
        }
        if (from == lexedUpto) {
            lexedUpto = line + 1;
        }
        lastRequestLines = line - from + 1;
        if (!measuredFirstRequest) {
            measuredFirstRequest = true;
            firstLexNanos = System.nanoTime() - begin;
        }
        return lines[line];
    }

    /**
     * Nombre de lignes du document (ne lexe rien).
     *
     * @return le compte de lignes du tampon
     */
    public int lineCount() {
        return buffer.getLineCount();
    }

    /**
     * Longueur du préfixe contigu lexé : la fenêtre déjà payée.
     *
     * @return le nombre de lignes calculées d'affilée depuis le début
     */
    public int lexedLineCount() {
        return lexedUpto;
    }

    /**
     * Lignes restant à lexer pour le préfixe contigu : ce que les
     * tranches de {@link #advanceLexing(int)} remplissent.
     *
     * @return le nombre de lignes non lexées du préfixe
     */
    public int remainingLines() {
        return Math.max(0, buffer.getLineCount() - lexedUpto);
    }

    /**
     * Remplit le préfixe lexé par tranches bornées : le budget de
     * temps du fil d'interface (une ligne coûte des microsecondes, un
     * document entier des dizaines de millisecondes).
     *
     * @param maxLines nombre maximal de lignes à lexer cette tranche
     * @return le nombre de lignes effectivement lexées
     */
    public int advanceLexing(int maxLines) {
        if (maxLines <= 0) {
            return 0;
        }
        int count = buffer.getLineCount();
        if (lexedUpto >= count) {
            return 0;
        }
        int budget = Math.min(maxLines, count - lexedUpto);
        int state = lexedUpto == 0 ? 0 : lines[lexedUpto - 1].endState();
        int revision = buffer.getRevision();
        for (int i = 0; i < budget; i++) {
            LineTokens fresh = lexLine(lexedUpto + i, state, revision);
            lines[lexedUpto + i] = fresh;
            state = fresh.endState();
        }
        lexedUpto += budget;
        return budget;
    }

    /**
     * Libère les entrées au-delà de la ligne donnée : la vue s'en
     * servira quand la mémoire presse. Les points de contrôle restent
     * — un retour dans la zone libérée repart du plus proche.
     *
     * @param fromLine première ligne libérée, 0 libère tout
     */
    public void releaseEntries(int fromLine) {
        int from = Math.max(0, fromLine);
        for (int i = Math.min(from, lines.length); i < lines.length; i++) {
            lines[i] = null;
        }
        lexedUpto = Math.min(lexedUpto, from);
    }

    /**
     * Le document est-il en mode gros fichier (L2 et L3 s'y couperont,
     * la coloration lexicale jamais) ?
     *
     * @return vrai au-delà des seuils de caractères ou de lignes
     */
    public boolean inBigFileMode() {
        return buffer.length() > BIG_FILE_CHARACTERS
                || buffer.getLineCount() > BIG_FILE_LINES;
    }

    /**
     * Total des jetons des lignes déjà lexées (ne force aucun lexage).
     *
     * @return le nombre de jetons calculés
     */
    public int tokenCount() {
        int total = 0;
        for (LineTokens entry : lines) {
            if (entry != null) {
                total += entry.tokens.length;
            }
        }
        return total;
    }

    /**
     * Durée de la première demande (la fenêtre demandée, pas le
     * document entier), en nanosecondes (diagnostic).
     *
     * @return la durée, 0 si aucune demande n'a eu lieu
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

    /**
     * Lignes lexées par la dernière demande (diagnostic) : la fenêtre
     * payée, le point de contrôle compris.
     *
     * @return le compte, 0 si la ligne était déjà calculée
     */
    public int lastRequestLines() {
        return lastRequestLines;
    }

    @Override
    public void textChanged(TextBuffer changed, int start, int removedLength,
            int insertedLength, int revision) {
        long begin = System.nanoTime();
        int count = buffer.getLineCount();
        if (lexedUpto == 0) {
            // rien de calculé : les états enregistrés décrivent un texte
            // antérieur, tout se reconstruit à la demande
            lines = new LineTokens[count];
            finishEdit(begin, 0);
            return;
        }
        int first = buffer.lineOfOffset(start);
        if (first > lexedUpto) {
            // édition au-delà du préfixe calculé : la zone touchée est
            // inconnue (jamais lexée ou île périmée) — la demande relira
            LineTokens[] fresh = new LineTokens[count];
            System.arraycopy(lines, 0, fresh, 0, lexedUpto);
            lines = fresh;
            finishEdit(begin, 0);
            return;
        }
        LineTokens oldFirst = lines[first];
        int oldCount = lines.length;
        int insertedBreaks = countBreaks(first, insertedLength);
        int removedBreaks = insertedBreaks - (count - oldCount);
        if (removedBreaks < 0) {
            // un tampon bien formé tient l'invariant ; on refuse de
            // corrompre le tableau par prudence
            throw new IllegalStateException("negative removed lines: " + removedBreaks);
        }
        int insertCount = insertedBreaks + 1;
        int removeCount = removedBreaks + 1;
        int shiftedBoundary = shiftBoundary(lexedUpto, first, removeCount, insertCount);
        restructure(first, removeCount, insertCount, oldCount);
        // la zone remplacée ne se lexe que jusqu'à la queue décalée (si
        // l'ancien préfixe l'atteint) : au-delà, le contenu n'a jamais
        // été calculé et reste à la demande — remplacer tout le tampon
        // ne paie donc que la fenêtre
        int mustLex = Math.min(first + insertCount, shiftedBoundary);
        int state = first == 0 ? 0 : lines[first - 1].endState();
        int relexed = 0;
        int lastLexed = first - 1;
        int stopLine = -1;
        boolean matched = false;
        boolean planted = false;
        for (int i = first; i < lines.length; i++) {
            LineTokens old = i == first ? oldFirst : lines[i];
            if (old == null && i >= mustLex) {
                // jamais calculé au-delà de la zone prouvable : un point
                // de contrôle porte l'état, la queue décalée (que plus
                // rien ne prouve) redevient à la demande
                checkpoints.put(i, new Checkpoint(state, revision));
                for (int j = i; j < lines.length; j++) {
                    lines[j] = null;
                }
                stopLine = i;
                planted = true;
                break;
            }
            LineTokens fresh = lexLine(i, state, revision);
            lines[i] = fresh;
            relexed++;
            lastLexed = i;
            if (old != null && old.endState() == fresh.endState()) {
                stopLine = i;
                matched = true;
                break;
            }
            state = fresh.endState();
        }
        if (first <= shiftedBoundary) {
            // en cas de point de contrôle posé, la queue a été libérée :
            // le préfixe s'arrête à la dernière ligne relue
            lexedUpto = planted
                    ? lastLexed + 1
                    : Math.min(lines.length, Math.max(shiftedBoundary, lastLexed + 1));
        }
        if (matched) {
            // la chaîne d'états s'est rejointe : tous les points de
            // contrôle restent valides, ils passent à la révision courante
            for (Map.Entry<Integer, Checkpoint> entry : checkpoints.entrySet()) {
                Checkpoint checkpoint = entry.getValue();
                if (checkpoint.revision() != revision) {
                    entry.setValue(new Checkpoint(checkpoint.state(), revision));
                }
            }
        }
        finishEdit(begin, relexed);
    }

    // ------------------------------------------------------------ interne

    private void finishEdit(long begin, int relexed) {
        lastRelexedLines = relexed;
        lastEditNanos = System.nanoTime() - begin;
    }

    /**
     * Le point de départ du lexage pour une ligne non calculée : le
     * plus proche point de contrôle valide, sinon la fin du préfixe.
     */
    private int restartPoint(int line) {
        int best = lexedUpto;
        int revision = buffer.getRevision();
        for (Map.Entry<Integer, Checkpoint> entry : checkpoints.entrySet()) {
            int at = entry.getKey();
            if (at > best && at <= line && entry.getValue().revision() == revision) {
                best = at;
            }
        }
        return best;
    }

    /** L'état qui ouvre la ligne de départ choisie. */
    private int restartState(int from) {
        if (from == lexedUpto) {
            return lexedUpto == 0 ? 0 : lines[lexedUpto - 1].endState();
        }
        Checkpoint checkpoint = checkpoints.get(from);
        return checkpoint == null ? 0 : checkpoint.state();
    }

    /**
     * La borne du préfixe après restructuration : décalée par la
     * différence de lignes, rabattue sur la zone remplacée si elle la
     * traversait.
     */
    private static int shiftBoundary(int boundary, int first, int removeCount,
            int insertCount) {
        if (boundary <= first) {
            return boundary;
        }
        if (boundary >= first + removeCount) {
            return boundary - removeCount + insertCount;
        }
        return first;
    }

    /**
     * Recale le tableau des lignes sur le remplacement : la zone
     * remplacée devient {@code null} (à relexer), la queue glisse sans
     * être relue.
     */
    private void restructure(int first, int removeCount, int insertCount,
            int oldCount) {
        int newCount = oldCount - removeCount + insertCount;
        LineTokens[] fresh = new LineTokens[newCount];
        System.arraycopy(lines, 0, fresh, 0, first);
        int tailSource = first + removeCount;
        System.arraycopy(lines, tailSource, fresh, first + insertCount,
                oldCount - tailSource);
        lines = fresh;
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
        if (line % CHECKPOINT_INTERVAL == 0) {
            checkpoints.put(line, new Checkpoint(state, revision));
        }
        int start = buffer.getLineStart(line);
        int end = buffer.getLineEnd(line);
        if (end - start > MAX_COLORED_COLUMNS) {
            // ligne monstre : pas de coloration, l'état se reporte tel
            // quel (approximation documentée — la couche L1 ne s'y
            // arrête jamais, elle s'y contente de ne rien découper)
            return new LineTokens(new Token[0], state, revision);
        }
        lexer.start(buffer, start, end, state);
        Token[] collected = new Token[4];
        int count = 0;
        lexer.advance();
        while (lexer.tokenType() != Lexer.NO_TOKEN) {
            if (count == collected.length) {
                collected = java.util.Arrays.copyOf(collected, count * 2);
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
