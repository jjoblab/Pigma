package jo.pigma.syntax;

import java.util.Arrays;
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
 * d'interface ; le remplissage peut aussi courir sur un fil de fond :
 * {@link #takeFillSlice(int)} prend un cliché (texte, bornes de
 * lignes, état d'ouverture, révision observée), {@link #lexFillSlice}
 * le lexe sans toucher l'état partagé, et {@link #applyFill} n'écrit
 * que si la révision observée du moteur correspond encore au cliché —
 * un résultat périmé ne s'applique jamais : une frappe entre deux
 * tranches annule la tranche, jamais le document. L'état du moteur
 * vit sous un verrou court : les méthodes publiques s'y croisent, le
 * lexage d'un cliché ne le tient jamais.</p>
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

    /**
     * Les jetons d'une ligne, compactés : les types en {@code short[]}
     * et les bornes de fin en {@code int[]} — un jeton n'est plus un
     * objet (624 002 jetons par passe du hors normes faisaient ~23 Mo
     * d'objets à recycler par remplissage ; six octets par jeton et
     * deux tableaux par ligne les tiennent). Le début du jeton
     * d'indice i est la fin du précédent — les jetons tapissent la
     * ligne sans trou. La portée se résout par la table du langage,
     * portée par référence : une par ligne, pas une par jeton. Les
     * tableaux se partagent tels quels — ne pas les muter.
     */
    public record LineTokens(short[] types, int[] ends, Scope[] scopeTable,
            int endState, int revision) {

        /** Le nombre de jetons de la ligne. */
        public int tokenCount() {
            return types.length;
        }

        /** Le type du jeton d'indice donné (entier du lexeur, positif). */
        public int typeAt(int index) {
            return types[index];
        }

        /** La borne de FIN du jeton, relative à la ligne. */
        public int endAt(int index) {
            return ends[index];
        }

        /** La borne de DÉBUT du jeton : la fin du précédent (zéro au premier). */
        public int startAt(int index) {
            return index == 0 ? 0 : ends[index - 1];
        }

        /** La portée du jeton (nulle : le jeton est neutre). */
        public Scope scopeAt(int index) {
            int type = types[index];
            return type < scopeTable.length ? scopeTable[type] : null;
        }

        /**
         * La vue jeton par jeton : les chemins de production (dessin,
         * analyse, appariement) y vont par indice — aucun objet par
         * jeton ; la vue ne vit que pour les tests, qui la
         * matérialisent à la lecture.
         *
         * @return les jetons de la ligne, vus en objets
         */
        public Token[] tokens() {
            Token[] view = new Token[types.length];
            for (int at = 0; at < view.length; at++) {
                view[at] = new Token(typeAt(at), scopeAt(at), startAt(at), endAt(at));
            }
            return view;
        }
    }

    /**
     * Un jeton d'une ligne, la vue de lecture : type, portée, bornes
     * relatives à la ligne (jamais allouée en production — cf.
     * {@link LineTokens#tokens()}).
     */
    public record Token(int type, Scope scope, int start, int end) {
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

    /** Verrou de l'état : le fil d'interface et le fil de fond s'y croisent. */
    private final Object stateLock = new Object();

    /**
     * La révision du tampon que le moteur a observée : elle monte dans
     * {@link #textChanged}, sous verrou. La révision brute du tampon
     * peut être en avance le temps d'une notification — c'est la
     * révision observée qui valide l'application d'un cliché.
     */
    private int observedRevision;

    private boolean measuredFirstRequest;
    private long firstLexNanos;
    private long lastEditNanos;
    private long lastRequestNanos;
    private int lastRelexedLines;
    private int lastRequestLines;

    private SyntaxDocument(TextBuffer buffer, LanguageSyntax language) {
        this.buffer = buffer;
        this.language = language;
        this.lexer = language.newLexer();
        this.lines = new LineTokens[buffer.getLineCount()];
        this.observedRevision = buffer.getRevision();
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
        buffer.addListenerFront(document);
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
        synchronized (stateLock) {
        if (lines[line] != null) {
            lastRequestLines = 0;
            lastRequestNanos = 0L;
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
        lastRequestNanos = System.nanoTime() - begin;
        if (!measuredFirstRequest) {
            measuredFirstRequest = true;
            firstLexNanos = System.nanoTime() - begin;
        }
        return lines[line];
        }
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
        synchronized (stateLock) {
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
    }

    /**
     * Cliché d'une tranche de remplissage : texte et bornes de lignes
     * immuables, état qui ouvre la tranche, révision observée à la
     * prise. Le fil de fond le lexe hors verrou ;
     * {@link #applyFill(FillSlice, LineTokens[])} ne l'écrit que si
     * cette révision est encore celle du moteur.
     *
     * @param revision   révision observée du moteur à la prise
     * @param fromLine   première ligne du cliché
     * @param startState état qui ouvre la première ligne du cliché
     * @param text       texte de la tranche, immuable
     * @param lineStarts débuts de ligne, relatifs au texte du cliché
     * @param lineEnds   fins de ligne, relatives au texte du cliché
     */
    public record FillSlice(int revision, int fromLine, int startState,
            String text, int[] lineStarts, int[] lineEnds) {
    }

    /**
     * Prend une tranche à remplir : un cliché du texte et des bornes
     * des lignes à lexer, l'état qui ouvre la tranche et la révision
     * observée. Retourne {@code null} quand le préfixe est complet ou
     * quand le tampon bouge sous la prise (l'édition en cours
     * rebranchera le fil de fond à sa notification).
     *
     * @param maxLines nombre maximal de lignes du cliché
     * @return le cliché, ou {@code null} si rien ne se prend
     */
    public FillSlice takeFillSlice(int maxLines) {
        if (maxLines <= 0) {
            return null;
        }
        synchronized (stateLock) {
            int count = buffer.getLineCount();
            if (lexedUpto >= count) {
                return null;
            }
            int from = lexedUpto;
            int startState = from == 0 ? 0 : lines[from - 1].endState();
            int last = Math.min(from + maxLines, count) - 1;
            int base = buffer.getLineStart(from);
            int textEnd = buffer.getLineEnd(last);
            if (base < 0 || textEnd < base || textEnd > buffer.length()) {
                // le tampon mute avant de notifier (bornes neuves,
                // longueur ancienne) : le cliché serait déchiré, on
                // n'en prend pas — l'édition relancera le fil de fond
                return null;
            }
            int linesInSlice = last - from + 1;
            int[] starts = new int[linesInSlice];
            int[] ends = new int[linesInSlice];
            for (int i = 0; i < linesInSlice; i++) {
                starts[i] = buffer.getLineStart(from + i) - base;
                ends[i] = buffer.getLineEnd(from + i) - base;
            }
            String text = buffer.getText(base, textEnd);
            if (buffer.getRevision() != observedRevision) {
                // le tampon est déjà à une révision que le moteur n'a
                // pas observée (la notification dort encore) : le
                // cliché porterait un texte neuf sous une révision
                // ancienne — on n'en prend pas, la révision observée
                // montera à la notification et le fil repartira
                return null;
            }
            return new FillSlice(observedRevision, from, startState, text, starts, ends);
        }
    }

    /**
     * Lexe le cliché hors verrou : aucun état partagé n'est lu ni
     * écrit, le lexeur est privé à l'appel — appelable depuis un autre
     * fil que celui du moteur.
     *
     * @param slice le cliché pris par {@link #takeFillSlice(int)}
     * @return les entrées des lignes du cliché, dans son ordre
     */
    public LineTokens[] lexFillSlice(FillSlice slice) {
        Objects.requireNonNull(slice);
        Lexer own = language.newLexer();
        int[] starts = slice.lineStarts();
        int[] ends = slice.lineEnds();
        LineTokens[] entries = new LineTokens[starts.length];
        int state = slice.startState();
        for (int i = 0; i < entries.length; i++) {
            entries[i] = lexWindow(own, slice.text(), starts[i], ends[i], state,
                    slice.revision());
            state = entries[i].endState();
        }
        return entries;
    }

    /**
     * Applique les entrées d'un cliché : seules celles au-delà du
     * préfixe déjà lexé s'écrivent, et rien ne s'écrit si la révision
     * observée a bougé depuis la prise — le contrat du fil de fond
     * annulable, un résultat périmé ne s'applique jamais.
     *
     * @param slice   le cliché pris par {@link #takeFillSlice(int)}
     * @param entries les entrées calculées par {@link #lexFillSlice}
     * @return le nombre d'entrées appliquées, 0 si le cliché est périmé
     */
    public int applyFill(FillSlice slice, LineTokens[] entries) {
        Objects.requireNonNull(slice);
        Objects.requireNonNull(entries);
        synchronized (stateLock) {
            // double garde : la révision observée (le moteur a vu la
            // même version du texte) ET la révision brute du tampon
            // (le cœur monte la révision avant de notifier — pendant
            // cette fenêtre, un cliché porterait un texte neuf sous
            // une révision ancienne ; la révision brute le refuse)
            if (observedRevision != slice.revision()
                    || buffer.getRevision() != slice.revision()) {
                return 0;
            }
            int from = Math.max(lexedUpto, slice.fromLine());
            if (from != lexedUpto) {
                // le préfixe a reculé (libération d'entrées) : le
                // cliché ne peut pas combler les trous, la demande
                // s'en chargera
                return 0;
            }
            int offset = from - slice.fromLine();
            int available = Math.min(entries.length - offset, lines.length - from);
            if (available <= 0) {
                return 0;
            }
            int state = offset == 0
                    ? slice.startState() : lines[from - 1].endState();
            for (int i = 0; i < available; i++) {
                int line = from + i;
                lines[line] = entries[offset + i];
                if (line % CHECKPOINT_INTERVAL == 0) {
                    checkpoints.put(line, new Checkpoint(state, slice.revision()));
                }
                state = entries[offset + i].endState();
            }
            lexedUpto = from + available;
            return available;
        }
    }

    /**
     * Libère les entrées au-delà de la ligne donnée : la vue s'en
     * servira quand la mémoire presse. Les points de contrôle restent
     * — un retour dans la zone libérée repart du plus proche.
     *
     * @param fromLine première ligne libérée, 0 libère tout
     */
    public void releaseEntries(int fromLine) {
        synchronized (stateLock) {
        int from = Math.max(0, fromLine);
        for (int i = Math.min(from, lines.length); i < lines.length; i++) {
            lines[i] = null;
        }
        lexedUpto = Math.min(lexedUpto, from);
        }
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
        synchronized (stateLock) {
        int total = 0;
        for (LineTokens entry : lines) {
            if (entry != null) {
                total += entry.tokenCount();
            }
        }
        return total;
        }
    }

    /**
     * Durée de la première demande (la fenêtre demandée, pas le
     * document entier), en nanosecondes (diagnostic).
     *
     * @return la durée, 0 si aucune demande n'a eu lieu
     */
    public long firstLexNanos() {
        synchronized (stateLock) {
            return firstLexNanos;
        }
    }

    /**
     * Durée du dernier relexage incrémental, en nanosecondes
     * (diagnostic).
     *
     * @return la durée, 0 si aucune édition n'a eu lieu
     */
    public long lastEditNanos() {
        synchronized (stateLock) {
            return lastEditNanos;
        }
    }

    /**
     * Durée de la dernière demande (diagnostic) : la fenêtre payée au
     * lexage à la demande — un saut lointain relancé depuis un point
     * de contrôle s'y lit. 0 si la ligne demandée était calculée.
     *
     * @return la durée en nanosecondes, 0 si aucune demande n'a lexé
     */
    public long lastRequestNanos() {
        synchronized (stateLock) {
            return lastRequestNanos;
        }
    }

    /**
     * Nombre de lignes relues au dernier relexage (diagnostic) : la
     * zone remplacée, plus la propagation de la règle d'arrêt.
     *
     * @return le compte, 0 si aucune édition n'a eu lieu
     */
    public int lastRelexedLines() {
        synchronized (stateLock) {
            return lastRelexedLines;
        }
    }

    /**
     * Lignes lexées par la dernière demande (diagnostic) : la fenêtre
     * payée, le point de contrôle compris.
     *
     * @return le compte, 0 si la ligne était déjà calculée
     */
    public int lastRequestLines() {
        synchronized (stateLock) {
            return lastRequestLines;
        }
    }

    @Override
    public void textChanged(TextBuffer changed, int start, int removedLength,
            int insertedLength, int revision) {
        long begin = System.nanoTime();
        synchronized (stateLock) {
        observedRevision = revision;
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
        int insertedBreaks = countBreaks(start, insertedLength);
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
            // la sortie sur état rejoint n'est légitime que si la zone
            // remplacée est entière (i - first >= insertCount - 1) : une
            // insertion de sauts laisse des slots neufs dans la zone,
            // sortir avant de les lexer laisserait des trous sous le
            // préfixe. Pour une zone d'une ligne, le premier slot est
            // aussi le dernier : la frappe ordinaire garde sa sortie
            // immédiate.
            if (old != null && i - first >= insertCount - 1
                    && old.endState() == fresh.endState()) {
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
        // même nombre de lignes : les indices ne bougent pas — la
        // recopie du tableau entier (52 004 entrées, un tableau neuf
        // par édition) ne servirait à rien. Le « tout remplacer » du
        // relevé 16 en a payé dix mille d'affilée : six secondes de
        // gel au harnais, l'ANR et la fermeture force sur l'appareil ;
        // la frappe ordinaire n'y paie plus le tableau non plus
        if (removeCount == insertCount) {
            return;
        }
        int newCount = oldCount - removeCount + insertCount;
        LineTokens[] fresh = new LineTokens[newCount];
        System.arraycopy(lines, 0, fresh, 0, first);
        int tailSource = first + removeCount;
        System.arraycopy(lines, tailSource, fresh, first + insertCount,
                oldCount - tailSource);
        lines = fresh;
    }

    /**
     * Nombre de sauts de ligne du texte inséré par le remplacement :
     * la fenêtre insérée commence à l'offset d'édition, pas au début
     * de sa ligne (une insertion en milieu de ligne peut y introduire
     * des sauts).
     */
    private int countBreaks(int start, int insertedLength) {
        int breaks = 0;
        for (int i = 0; i < insertedLength; i++) {
            if (buffer.charAt(start + i) == '\n') {
                breaks++;
            }
        }
        return breaks;
    }

    /** Les jetons d'une passe de lexeur, compactés (types, bornes de fin). */
    private record Run(short[] types, int[] ends) {
    }

    /** Types vides partagés (la ligne monstre, hors plafond, n'émet rien). */
    private static final short[] NO_TYPES = new short[0];

    /** Bornes vides partagées (la ligne monstre, hors plafond, n'émet rien). */
    private static final int[] NO_ENDS = new int[0];

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
            return new LineTokens(NO_TYPES, NO_ENDS, language.scopeTable(),
                    state, revision);
        }
        lexer.start(buffer, start, end, state);
        Run run = collectTokens(lexer, start);
        return new LineTokens(run.types(), run.ends(), language.scopeTable(),
                lexer.state(), revision);
    }

    /**
     * Lexe une fenêtre du cliché : la ligne monstre s'y reporte comme
     * au lexeur du moteur, les bornes sont relatives au texte du
     * cliché.
     */
    private LineTokens lexWindow(Lexer own, CharSequence text, int start, int end,
            int state, int revision) {
        if (end - start > MAX_COLORED_COLUMNS) {
            return new LineTokens(NO_TYPES, NO_ENDS, language.scopeTable(),
                    state, revision);
        }
        own.start(text, start, end, state);
        Run run = collectTokens(own, start);
        return new LineTokens(run.types(), run.ends(), language.scopeTable(),
                own.state(), revision);
    }

    /**
     * Collecte les jetons du lexeur prêt, en bornes relatives à la
     * ligne — types et bornes en tableaux primitifs, sans objet par
     * jeton (la compaction : la croissance double, la coupe finale
     * ajuste).
     */
    private Run collectTokens(Lexer ready, int start) {
        short[] types = new short[8];
        int[] ends = new int[8];
        int count = 0;
        ready.advance();
        while (ready.tokenType() != Lexer.NO_TOKEN) {
            if (count == types.length) {
                types = Arrays.copyOf(types, count * 2);
                ends = Arrays.copyOf(ends, count * 2);
            }
            types[count] = (short) ready.tokenType();
            ends[count] = ready.tokenEnd() - start;
            count++;
            ready.advance();
        }
        return new Run(Arrays.copyOf(types, count), Arrays.copyOf(ends, count));
    }
}
