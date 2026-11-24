package jo.pigma.syntax;

import jo.pigma.core.TextBuffer;
import jo.pigma.core.TextListener;

import java.util.Arrays;
import java.util.Objects;

/**
 * L2, l'analyseur tolérant : la structure d'appariement des
 * parenthèses, accolades et crochets, par-dessus les jetons de L1 —
 * jamais au-dessous : l'analyseur lit les lignes lexées du document
 * de coloration, il ne relit pas le texte brut.
 *
 * <p>Le modèle d'incrément calque celui du lexeur : un préfixe
 * analysé à la demande ({@link #structureOf(int)}), une marche
 * d'édition qui recalcule la zone remplacée puis s'arrête dès que la
 * chaîne rejoint — et la réinsertion exige DEUX preuves : la
 * profondeur de sortie rejoint l'ancienne ET les jetons de la ligne
 * sont les mêmes (par identité d'objet : un commentaire ouvert à la
 * ligne au-dessus change les jetons sans changer la profondeur — le
 * lexeur relit, la structure aussi). La queue épargnée est donc
 * l'intersection des épargnes des deux couches. Une borne stricte de
 * plus : la structure ne dépasse jamais les jetons disponibles (queue
 * du lexeur relâchée : la structure relâche aussi, la demande
 * reprendra avec eux).</p>
 *
 * <p>La tolérance : un fermant sans ouvrant se compte
 * ({@link LineStructure#mismatches()}) et la profondeur reste à
 * zéro (jamais négative) ; un ouvrant jamais fermé en fin de document
 * n'est pas un rompu — c'est un document ouvert ; l'appariement par
 * type (un {@code )} fermant une accolade) n'est pas vérifié en cette
 * version, tout fermant décompte — documenté, assumé.</p>
 *
 * <p>Fil unique : les éditions arrivent par l'écouteur du tampon (fil
 * de mutation), les demandes par le fil qui lit — le verrou d'état
 * couvre tout, les analyses sont pures (elles forcent le préfixe du
 * lexeur, comme le fait le dessin).</p>
 */
public final class StructureDocument {

    /**
     * Structure d'une ligne : la profondeur d'appariement en sortie de
     * ligne (ouvrants moins fermants, plancher à zéro) et le nombre
     * d'appariements rompus qu'elle porte (fermants sans ouvrant).
     *
     * @param depthOut   profondeur après la ligne
     * @param mismatches fermants sans ouvrant rencontrés dans la ligne
     */
    public record LineStructure(int depthOut, int mismatches) {
    }

    /**
     * Une paire d'appariement : le signe demandé (ligne, colonne) et
     * son partenaire dans la chaîne tolérante.
     *
     * @param fromLine   ligne du signe demandé
     * @param fromColumn colonne du signe demandé
     * @param toLine     ligne du partenaire
     * @param toColumn   colonne du partenaire
     */
    public record BracketPair(int fromLine, int fromColumn,
            int toLine, int toColumn) {
    }

    /**
     * Une plage de bloc : les lignes de l'ouvrant à son fermant (un
     * bloc jamais refermé va jusqu'à la dernière ligne).
     *
     * @param fromLine ligne de l'ouvrant
     * @param toLine   ligne du fermant
     */
    public record BlockRange(int fromLine, int toLine) {
    }

    /**
     * L'analyse interne porte en plus le timbre : l'objet de jetons
     * depuis lequel elle fut calculée. Identité préservée par l'épargne
     * du lexeur : un timbre inchangé prouve que les jetons n'ont pas
     * bougé, la réinsertion peut s'appuyer dessus.
     */
    private record LineAnalysis(SyntaxDocument.LineTokens stamp,
            int depthOut, int mismatches) {
    }

    private final TextBuffer buffer;
    private final SyntaxDocument syntax;
    private final Object stateLock = new Object();

    /** Lignes analysées [0..analyzedUpto), au-delà : rien de prouvé. */
    private LineAnalysis[] lines = new LineAnalysis[0];

    /** Portée de la marche d'appariement : au-delà, la paire est trop loin. */
    private static final int MATCH_WALK_MAX_LINES = 4000;

    private int analyzedUpto;

    /** Analyses de lignes effectuées (le compteur de la réinsertion). */
    private long analyses;

    private final TextListener modelListener = new TextListener() {
        @Override
        public void textChanged(TextBuffer changed, int start, int removedLength,
                int insertedLength, int revision) {
            edit(start, insertedLength);
        }
    };

    private StructureDocument(TextBuffer buffer, SyntaxDocument syntax) {
        this.buffer = buffer;
        this.syntax = syntax;
        buffer.addListenerFront(modelListener);
    }

    /**
     * Ouvre la structure d'un document lexé : aucune analyse ne part
     * avant la première demande.
     *
     * @param buffer le tampon du texte (géométrie des lignes)
     * @param syntax le moteur de coloration (jetons de L1)
     * @return la structure prête, jamais {@code null}
     */
    public static StructureDocument open(TextBuffer buffer, SyntaxDocument syntax) {
        Objects.requireNonNull(buffer);
        Objects.requireNonNull(syntax);
        return new StructureDocument(buffer, syntax);
    }

    /**
     * Détache l'écouteur : la structure se tait, les demandes restent
     * lisibles mais n'apprendront plus les éditions.
     */
    public void detach() {
        buffer.removeListener(modelListener);
    }

    /**
     * La structure d'une ligne : force l'analyse du préfixe jusqu'à
     * elle (et le lexage des jetons qu'elle lit, comme le dessin).
     *
     * @param line indice de la ligne, 0 à lineCount-1
     * @return sa structure, jamais {@code null}
     */
    public LineStructure structureOf(int line) {
        synchronized (stateLock) {
            if (line < 0) {
                throw new IndexOutOfBoundsException("ligne négative : " + line);
            }
            if (line >= buffer.getLineCount()) {
                throw new IndexOutOfBoundsException("ligne hors document : " + line);
            }
            ensureAnalyzed(line + 1);
            LineAnalysis analysis = lines[line];
            return new LineStructure(analysis.depthOut(), analysis.mismatches());
        }
    }

    /**
     * Le signe adjacent au curseur (à gauche d'abord — la frappe
     * referme à gauche) et son partenaire. La marche suit les jetons
     * depuis le signe, la profondeur de début de chaque ligne vient de
     * L2 (le point de contrôle : jamais de relecture depuis le début
     * du document). Un fermant orphelin ou un ouvrant non fermé n'a
     * pas de partenaire ({@code null}) ; une paire plus loin que
     * {@value #MATCH_WALK_MAX_LINES} lignes se déclare trop loin.
     *
     * @param caretOffset la position du curseur
     * @return la paire, ou {@code null} si aucun signe adjacent ne
     *         s'apparie
     */
    public BracketPair pairAt(int caretOffset) {
        synchronized (stateLock) {
            if (caretOffset < 0 || caretOffset > buffer.length()) {
                return null;
            }
            for (int side = 0; side < 2; side++) {
                int offset = side == 0 ? caretOffset - 1 : caretOffset;
                if (offset < 0 || offset >= buffer.length()) {
                    continue;
                }
                BracketPair pair = pairOfSign(offset);
                if (pair != null) {
                    return pair;
                }
            }
            return null;
        }
    }

    /**
     * La portée du curseur : le bloc le plus interne qui l'englobe —
     * la profondeur se lit à la position exacte du curseur (sur la
     * ligne de l'ouvrant, après l'accolade, c'est le bloc NOUVEAU ;
     * avant, c'est le parent), puis les bornes se cherchent par la
     * chaîne de profondeur : l'ouvrant vit après la dernière ligne
     * d'avant qui referme sous ce niveau, le fermant à la première
     * ligne qui y retombe. La portée surlignée et le pli se
     * branchent ici.
     *
     * @param caretOffset la position du curseur
     * @return la plage du bloc englobant, ou {@code null} au niveau
     *         racine (aucun bloc n'englobe le curseur)
     */
    public BlockRange scopeAt(int caretOffset) {
        synchronized (stateLock) {
            if (caretOffset < 0 || caretOffset > buffer.length()) {
                return null;
            }
            int line = buffer.lineOfOffset(caretOffset);
            ensureAnalyzed(line + 1);
            int depth = depthAtOffset(line, caretOffset - buffer.getLineStart(line));
            if (depth <= 0) {
                return null;
            }
            int count = buffer.getLineCount();
            int fromLine = 0;
            for (int m = line - 1; m >= 0; m--) {
                if (lines[m].depthOut() < depth) {
                    fromLine = m + 1;
                    break;
                }
            }
            int toLine = count - 1;
            for (int m = line; m < count; m++) {
                ensureAnalyzed(m + 1);
                if (lines[m].depthOut() < depth) {
                    toLine = m;
                    break;
                }
            }
            return new BlockRange(fromLine, toLine);
        }
    }

    /**
     * La ligne ouvre-t-elle un bloc repliable : profondeur de sortie
     * supérieure à celle d'entrée (la règle nette — une paire ouverte
     * ET refermée sur sa propre ligne ne plie pas, documenté).
     *
     * @param line indice de la ligne
     * @return vrai si la ligne ouvre un bloc
     */
    public boolean foldStartsAt(int line) {
        synchronized (stateLock) {
            if (line < 0 || line >= buffer.getLineCount()) {
                return false;
            }
            ensureAnalyzed(line + 1);
            return lines[line].depthOut() > depthInOf(line);
        }
    }

    /**
     * La ligne où referme le bloc ouvert à cette ligne : la première
     * ligne dont la profondeur de sortie retombe au niveau d'entrée.
     * Un bloc jamais refermé va jusqu'à la dernière ligne (document
     * ouvert).
     *
     * @param line indice de la ligne d'ouverture
     * @return l'indice de la ligne de fermeture
     */
    public int foldEndLine(int line) {
        synchronized (stateLock) {
            int count = buffer.getLineCount();
            if (line < 0 || line >= count) {
                return line;
            }
            ensureAnalyzed(line + 1);
            int depthIn = depthInOf(line);
            for (int m = line; m < count; m++) {
                ensureAnalyzed(m + 1);
                if (lines[m].depthOut() <= depthIn) {
                    return m;
                }
            }
            return count - 1;
        }
    }

    /**
     * Lignes analysées du préfixe courant.
     *
     * @return le nombre de lignes prouvées
     */
    public int analyzedLineCount() {
        synchronized (stateLock) {
            return analyzedUpto;
        }
    }

    /**
     * Appariements rompus du préfixe analysé : les fermants sans
     * ouvrant, ligne après ligne.
     *
     * @return le total des rompus
     */
    public int mismatchCount() {
        synchronized (stateLock) {
            int total = 0;
            for (int i = 0; i < analyzedUpto; i++) {
                total += lines[i].mismatches();
            }
            return total;
        }
    }

    /**
     * Analyses de lignes effectuées depuis l'ouverture : le compteur
     * qui prouve que la réinsertion épargne la queue.
     *
     * @return le nombre d'analyses
     */
    public long analyses() {
        synchronized (stateLock) {
            return analyses;
        }
    }

    // ------------------------------------------------------------ interne

    /**
     * La marche d'édition : recale le tableau (la queue glisse), puis
     * réanalyse la zone remplacée jusqu'à ce que la chaîne rejoigne —
     * profondeur ET timbre de jetons (les deux couches doivent s'être
     * réalignées). S'arrête en outre aux jetons disponibles : la
     * structure ne devance jamais son socle.
     */
    private void edit(int start, int insertedLength) {
        synchronized (stateLock) {
            int count = buffer.getLineCount();
            if (analyzedUpto == 0) {
                // rien de prouvé : le recalcul attendra la demande
                lines = new LineAnalysis[count];
                return;
            }
            int first = buffer.lineOfOffset(start);
            if (first > analyzedUpto) {
                // édition au-delà du préfixe analysé : la zone est
                // inconnue, le préfixe ne bouge pas
                LineAnalysis[] fresh = new LineAnalysis[count];
                System.arraycopy(lines, 0, fresh, 0, analyzedUpto);
                lines = fresh;
                return;
            }
            LineAnalysis oldFirst = lines[first];
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
            int shiftedBoundary = shiftBoundary(analyzedUpto, first, removeCount, insertCount);
            restructure(first, removeCount, insertCount, oldCount);
            int mustAnalyze = Math.min(first + insertCount, shiftedBoundary);
            int depth = first == 0 ? 0 : lines[first - 1].depthOut();
            int lastAnalyzed = first - 1;
            boolean released = false;
            for (int i = first; i < lines.length; i++) {
                if (i >= syntax.lexedLineCount()) {
                    // les jetons s'arrêtent là (queue relâchée par le
                    // lexeur ou jamais remplie) : la structure relâche
                    // aussi, la demande reprendra avec les jetons
                    releaseFrom(i);
                    released = true;
                    break;
                }
                LineAnalysis old = i == first ? oldFirst : lines[i];
                if (old == null && i >= mustAnalyze) {
                    // jamais analysé au-delà de la zone prouvable : la
                    // queue décalée redevient à la demande
                    releaseFrom(i);
                    released = true;
                    break;
                }
                LineAnalysis fresh = analyzeLine(i, depth);
                lines[i] = fresh;
                lastAnalyzed = i;
                // la sortie n'est légitime que si la zone remplacée est
                // entière (i - first >= insertCount - 1 : sinon des
                // slots neufs resteraient vides sous le préfixe) ET si
                // la profondeur rejoint l'ancienne ET si les jetons de
                // la ligne sont les mêmes — un commentaire ouvert plus
                // haut change les jetons sans changer la profondeur
                if (old != null && i - first >= insertCount - 1
                        && old.stamp() == fresh.stamp()
                        && old.depthOut() == fresh.depthOut()) {
                    break;
                }
                depth = fresh.depthOut();
            }
            analyzedUpto = released
                    ? lastAnalyzed + 1
                    : Math.min(lines.length, Math.max(shiftedBoundary, lastAnalyzed + 1));
        }
    }

    /**
     * Analyse le préfixe jusqu'à la ligne demandée : la profondeur
     * d'entrée vient de la ligne précédente, les jetons forcent le
     * lexage si besoin.
     */
    private void ensureAnalyzed(int toExclusive) {
        int count = buffer.getLineCount();
        int target = Math.min(toExclusive, count);
        if (analyzedUpto >= target) {
            return;
        }
        if (lines.length < count) {
            lines = Arrays.copyOf(lines, count);
        }
        int depth = analyzedUpto == 0 ? 0 : lines[analyzedUpto - 1].depthOut();
        for (int i = analyzedUpto; i < target; i++) {
            lines[i] = analyzeLine(i, depth);
            depth = lines[i].depthOut();
            analyzedUpto = i + 1;
        }
    }

    /**
     * Analyse une ligne depuis ses jetons : seuls les signes simples
     * comptent — une parenthèse dans une chaîne ou un commentaire
     * vit dans un jeton plus long, elle ne compte pas.
     */
    private LineAnalysis analyzeLine(int line, int depthIn) {
        SyntaxDocument.LineTokens entry = syntax.lineTokens(line);
        CharSequence text = buffer.getText(
                buffer.getLineStart(line), buffer.getLineEnd(line));
        int depth = depthIn;
        int mismatches = 0;
        for (SyntaxDocument.Token token : entry.tokens()) {
            if (token.end() - token.start() != 1) {
                continue;
            }
            char sign = text.charAt(token.start());
            if (sign == '(' || sign == '{' || sign == '[') {
                depth++;
            } else if (sign == ')' || sign == '}' || sign == ']') {
                if (depth == 0) {
                    // tolérant : le fermant orphelin se compte, la
                    // profondeur ne descend jamais sous zéro
                    mismatches++;
                } else {
                    depth--;
                }
            }
        }
        analyses++;
        return new LineAnalysis(entry, depth, mismatches);
    }

    /** Relâche la queue : rien n'est plus prouvé à partir de là. */
    private void releaseFrom(int from) {
        for (int i = from; i < lines.length; i++) {
            lines[i] = null;
        }
    }

    // ------------------------------------------------- appariement (L2)

    /**
     * La profondeur d'appariement à un offset de la ligne (la règle
     * tolérante de l'analyse, à l'identique) : les jetons d'avant la
     * position comptent, ceux d'après non.
     */
    private int depthAtOffset(int line, int rel) {
        SyntaxDocument.Token[] tokens = syntax.lineTokens(line).tokens();
        CharSequence text = buffer.getText(
                buffer.getLineStart(line), buffer.getLineEnd(line));
        int depth = depthInOf(line);
        for (SyntaxDocument.Token token : tokens) {
            if (token.start() >= rel) {
                break;
            }
            char c = singleSign(token, text);
            if (c == 0) {
                continue;
            }
            if (c == '(' || c == '{' || c == '[') {
                depth++;
            } else if (depth > 0) {
                depth--;
            }
        }
        return depth;
    }

    /** La profondeur d'entrée d'une ligne (le préfixe doit être analysé). */
    private int depthInOf(int line) {
        return line == 0 ? 0 : lines[line - 1].depthOut();
    }

    /**
     * Le partenaire du signe simple à l'offset donné, ou {@code null}.
     * La profondeur du signe se lit depuis le début de sa ligne (la
     * règle tolérante de l'analyse, à l'identique).
     */
    private BracketPair pairOfSign(int offset) {
        int line = buffer.lineOfOffset(offset);
        ensureAnalyzed(line + 1);
        int rel = offset - buffer.getLineStart(line);
        SyntaxDocument.Token[] tokens = syntax.lineTokens(line).tokens();
        CharSequence text = buffer.getText(
                buffer.getLineStart(line), buffer.getLineEnd(line));
        int depth = line == 0 ? 0 : lines[line - 1].depthOut();
        int signIndex = -1;
        char sign = 0;
        for (int at = 0; at < tokens.length; at++) {
            SyntaxDocument.Token token = tokens[at];
            char c = singleSign(token, text);
            if (c == 0) {
                continue;
            }
            if (token.start() == rel) {
                signIndex = at;
                sign = c;
                break;
            }
            if (c == '(' || c == '{' || c == '[') {
                depth++;
            } else if (depth > 0) {
                depth--;
            }
        }
        if (signIndex < 0) {
            return null;
        }
        if (sign == '(' || sign == '{' || sign == '[') {
            return matchForward(line, rel, signIndex, depth);
        }
        if (depth == 0) {
            // fermant orphelin : toléré, compté, sans partenaire
            return null;
        }
        return matchBackward(line, rel, signIndex);
    }

    /**
     * Le fermant d'un ouvrant donné : la marche suit les jetons depuis
     * l'ouvrant, ligne à ligne — le retour à la profondeur d'avant
     * l'ouvrant peut vivre AU MILIEU d'une ligne (l'ouvrant refermé,
     * puis un autre ouvert sur la même ligne : le saut par la seule
     * profondeur de fin de ligne le raterait). L2 donne le point de
     * contrôle de début de chaque ligne (pas de relecture depuis le
     * début du document), le jeton exact se lit dans la ligne.
     */
    private BracketPair matchForward(int line, int rel, int signIndex,
            int depthAtSign) {
        int count = buffer.getLineCount();
        int limit = Math.min(line + MATCH_WALK_MAX_LINES, count);
        for (int m = line; m < limit; m++) {
            ensureAnalyzed(m + 1);
            int column = closerColumn(m, line, signIndex, depthAtSign);
            if (column >= 0) {
                return new BracketPair(line, rel, m, column);
            }
        }
        // au-delà de la borne, la paire se déclare trop loin : la
        // marche resterait payable, la mesure dira s'il le faut
        return null;
    }

    /**
     * L'ouvrant d'un fermant donné : la marche suit les jetons en
     * arrière depuis le fermant, ligne à ligne — la file des fermants
     * à sauter se PORTE d'une ligne à l'autre (la règle de pile : un
     * point de contrôle par ligne la perdrait, l'ouvrant qui referme
     * peut vivre loin derrière son fermant), l'ouvrant qui la vide est
     * le partenaire.
     */
    private BracketPair matchBackward(int line, int rel, int signIndex) {
        int bound = Math.max(line - MATCH_WALK_MAX_LINES, -1);
        int counter = 0;
        for (int m = line; m > bound; m--) {
            SyntaxDocument.Token[] tokens = syntax.lineTokens(m).tokens();
            CharSequence text = buffer.getText(
                    buffer.getLineStart(m), buffer.getLineEnd(m));
            int from = m == line ? signIndex - 1 : tokens.length - 1;
            for (int at = from; at >= 0; at--) {
                SyntaxDocument.Token token = tokens[at];
                char c = singleSign(token, text);
                if (c == 0) {
                    continue;
                }
                if (c == '(' || c == '{' || c == '[') {
                    if (counter == 0) {
                        return new BracketPair(line, rel, m, token.start());
                    }
                    counter--;
                } else {
                    counter++;
                }
            }
        }
        return null;
    }

    /**
     * Le fermant exact dans une ligne de la marche : la première
     * décroissance qui rejoint la profondeur d'avant l'ouvrant. Sur
     * la ligne de l'ouvrant lui-même, la marche part juste après lui ;
     * ailleurs, la profondeur de départ vient de L2.
     */
    private int closerColumn(int matchLine, int openerLine, int openerIndex,
            int depthAtSign) {
        boolean sameLine = matchLine == openerLine;
        int from = sameLine ? openerIndex + 1 : 0;
        int running = sameLine
                ? depthAtSign + 1
                : lines[matchLine - 1].depthOut();
        SyntaxDocument.Token[] tokens = syntax.lineTokens(matchLine).tokens();
        CharSequence text = buffer.getText(
                buffer.getLineStart(matchLine), buffer.getLineEnd(matchLine));
        for (int at = from; at < tokens.length; at++) {
            SyntaxDocument.Token token = tokens[at];
            char c = singleSign(token, text);
            if (c == 0) {
                continue;
            }
            if (c == '(' || c == '{' || c == '[') {
                running++;
            } else if (running > 0) {
                running--;
                if (running == depthAtSign) {
                    return token.start();
                }
            }
        }
        return -1;
    }

    /**
     * Le caractère d'appariement d'un jeton simple : 0 si le jeton
     * n'est pas un signe (une parenthèse dans une chaîne ou un
     * commentaire vit dans un jeton plus long, elle n'apparie pas).
     */
    private static char singleSign(SyntaxDocument.Token token, CharSequence text) {
        if (token.end() - token.start() != 1) {
            return 0;
        }
        char c = text.charAt(token.start());
        if (c == '(' || c == ')' || c == '{' || c == '}'
                || c == '[' || c == ']') {
            return c;
        }
        return 0;
    }

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
     * remplacée devient {@code null} (à réanalyser), la queue glisse
     * sans être relue.
     */
    private void restructure(int first, int removeCount, int insertCount,
            int oldCount) {
        int newCount = oldCount - removeCount + insertCount;
        LineAnalysis[] fresh = new LineAnalysis[newCount];
        System.arraycopy(lines, 0, fresh, 0, first);
        int tailSource = first + removeCount;
        System.arraycopy(lines, tailSource, fresh, first + insertCount,
                oldCount - tailSource);
        lines = fresh;
    }

    /**
     * Sauts de ligne du texte inséré par le remplacement : la fenêtre
     * insérée commence à l'offset d'édition, pas au début de sa
     * ligne (même règle que le lexeur).
     */
    private int countBreaks(int start, int insertedLength) {
        CharSequence inserted = buffer.getText(start, start + insertedLength);
        int breaks = 0;
        for (int at = 0; at < inserted.length(); at++) {
            if (inserted.charAt(at) == '\n') {
                breaks++;
            }
        }
        return breaks;
    }
}
