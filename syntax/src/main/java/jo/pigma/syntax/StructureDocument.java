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
        buffer.addListener(modelListener);
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
