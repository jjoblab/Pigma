package jo.pigma.core;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Modèle de texte mutable, avec index de lignes maintenu de façon
 * incrémentale.
 *
 * <p>Le texte est porté par un tampon à trou (gap buffer) : les lectures et
 * les écritures autour de la position d'édition sont proches d'un coût
 * constant, sans recopie globale du texte à chaque frappe. Les offsets sont
 * des unités de code UTF-16, comme dans {@link String} : une paire de
 * substitution occupe deux offsets. Les remplacements à cheval sur une
 * paire sont autorisés et ne la corrompent jamais : annulés, ils rendent le
 * texte exactement antérieur.</p>
 *
 * <p>L'index de lignes sépare sur le caractère {@code '\n'} seul ; un
 * retour chariot {@code '\r'} est un caractère ordinaire (les textes à
 * fins de ligne Windows portent deux lignes logiques par saut). Chaque
 * tampon compte une révision, incrémentée à chaque remplacement effectif :
 * un résultat calculé sur une révision donnée est périmé dès que la
 * révision avance.</p>
 *
 * <p>Les écritures publiques passent par {@link TextTransaction} (voir
 * {@link UndoStack#newTransaction()}) ; le remplacement direct est réservé
 * aux mécanismes internes du même package.</p>
 */
public final class TextBuffer implements CharSequence {

    private static final int INITIAL_CAPACITY = 64;

    /**
     * Le verrou de mutation : les écritures (le remplacement et ses
     * tableaux : contenu, trou, index de lignes, révision) y passent
     * ENTIERES, et les clichés d'un autre fil (le moteur de
     * coloration) le prennent par {@link #snapshot(RangeReader)} —
     * une lecture ne voit plus jamais un tableau à moitié réécrit.
     */
    final Object editLock = new Object();

    private char[] content;
    private int gapStart;
    private int gapEnd;
    private int textLength;
    private int[] lineStarts;
    private int lineCount;
    private volatile int revision;
    private final List<TextListener> headListeners = new CopyOnWriteArrayList<>();

    private final List<TextListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Crée un tampon contenant le texte donné.
     *
     * @param initialText texte initial, jamais {@code null} (peut être vide)
     * @return tampon prêt à lire, révision 0, une écoute possible
     */
    public static TextBuffer create(CharSequence initialText) {
        if (initialText == null) {
            throw new NullPointerException("initialText must not be null");
        }
        return new TextBuffer(initialText.toString());
    }

    private TextBuffer(String initialText) {
        textLength = initialText.length();
        content = new char[Math.max(INITIAL_CAPACITY, textLength * 2)];
        initialText.getChars(0, textLength, content, 0);
        gapStart = textLength;
        gapEnd = content.length;
        lineStarts = new int[Math.max(8, countLines(initialText) + 2)];
        lineCount = 1;
        for (int i = 0; i < textLength; i++) {
            if (initialText.charAt(i) == '\n') {
                if (lineCount == lineStarts.length) {
                    lineStarts = grow(lineStarts, lineCount + 8);
                }
                lineStarts[lineCount++] = i + 1;
            }
        }
    }

    private static int countLines(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }

    private static int[] grow(int[] array, int needed) {
        return java.util.Arrays.copyOf(array, Math.max(needed, array.length * 2));
    }

    // ------------------------------------------------------------ lecture

    /**
     * Longueur du texte en unités de code UTF-16.
     *
     * @return la longueur, jamais négative
     */
    public int length() {
        return textLength;
    }

    /**
     * Caractère à l'offset donné, en unités de code UTF-16.
     *
     * @param index offset, entre 0 et {@link #length()} exclu
     * @return l'unité de code à cet offset
     */
    public char charAt(int index) {
        if (index < 0 || index >= textLength) {
            throw new IndexOutOfBoundsException("index out of range: " + index);
        }
        return index < gapStart ? content[index] : content[index + (gapEnd - gapStart)];
    }

    /**
     * Texte de la plage {@code [start, end)} (copie).
     *
     * @param start début de la plage, inclus
     * @param end   fin de la plage, exclue
     * @return le texte de la plage, jamais {@code null}
     */
    public String getText(int start, int end) {
        checkRange(start, end);
        char[] result = new char[end - start];
        if (end <= gapStart) {
            System.arraycopy(content, start, result, 0, end - start);
        } else if (start >= gapStart) {
            System.arraycopy(content, start + (gapEnd - gapStart), result, 0, end - start);
        } else {
            int head = gapStart - start;
            System.arraycopy(content, start, result, 0, head);
            System.arraycopy(content, gapEnd, result, head, end - start - head);
        }
        return new String(result);
    }

    /**
     * Plage en tant que séquence de caractères : le tampon se prête
     * aux moteurs qui lisent par {@link CharSequence} (lexage sans
     * copie du texte).
     *
     * @param start début de la plage, inclus
     * @param end   fin de la plage, exclue
     * @return le texte de la plage, jamais {@code null}
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        return getText(start, end);
    }

    /**
     * Texte complet (copie, à réserver aux tests et aux diagnostics).
     *
     * @return le texte entier, jamais {@code null}
     */
    @Override
    public String toString() {
        return getText(0, textLength);
    }

    /**
     * Révision du texte : incrémentée à chaque remplacement effectif, jamais
     * remise à zéro. Un résultat calculé sur une révision doit être jeté dès
     * que celle-ci avance. La lecture est volatile : un autre fil (le
     * moteur de coloration) y voit la dernière valeur posée sans
     * attendre un verrou.
     *
     * @return la révision courante
     */
    public int getRevision() {
        return revision;
    }

    /**
     * Lit le tampon d'un seul tenant sous le verrou de mutation :
     * le lecteur reçoit le texte, les bornes et la révision
     * cohérents — une écriture du fil d'interface ne peut pas
     * se glisser dans la lecture (le cliché resterait déchiré).
     * Réservé aux lecteurs d'un AUTRE fil (le moteur de fond) ;
     * le fil d'interface lit hors verrou, ses propres écritures
     * suivent son ordre programme.
     *
     * @param reader le lecteur appelé sous verrou, jamais {@code null}
     * @param <R>    le type du résultat du lecteur
     * @return le résultat du lecteur
     */
    public <R> R snapshot(RangeReader<R> reader) {
        synchronized (editLock) {
            return reader.read(length(), revision);
        }
    }

    /**
     * Le lecteur d'un cliché : le texte n'y vit pas, les bornes et
     * la longueur y passent — le lecteur y appelle les lectures
     * publiques, le verrou tient déjà.
     */
    @FunctionalInterface
    public interface RangeReader<R> {

        /**
         * Lit le tampon (sous le verrou de mutation).
         *
         * @param length   la longueur du texte au moment de la lecture
         * @param revision la révision au moment de la lecture
         * @return le résultat du lecteur
         */
        R read(int length, int revision);
    }

    // ------------------------------------------------------------- lignes

    /**
     * Nombre de lignes logiques : au moins 1, même pour un texte vide ; un
     * texte finissant par {@code '\n'} compte une dernière ligne vide.
     *
     * @return le nombre de lignes
     */
    public int getLineCount() {
        return lineCount;
    }

    /**
     * Offset de début de la ligne donnée.
     *
     * @param line numéro de ligne, entre 0 et {@link #getLineCount()} exclu
     * @return l'offset de début, en unités de code UTF-16
     */
    public int getLineStart(int line) {
        checkLine(line);
        return lineStarts[line];
    }

    /**
     * Offset de fin de la ligne donnée, son saut de ligne final exclu : le
     * contenu de la ligne est la plage
     * {@code [getLineStart(line), getLineEnd(line))}.
     *
     * @param line numéro de ligne, entre 0 et {@link #getLineCount()} exclu
     * @return l'offset de fin, hors terminateur
     */
    public int getLineEnd(int line) {
        checkLine(line);
        return line < lineCount - 1 ? lineStarts[line + 1] - 1 : textLength;
    }

    /**
     * Ligne contenant l'offset donné : l'offset {@link #length()} appartient
     * à la dernière ligne.
     *
     * @param offset offset, entre 0 et {@link #length()}
     * @return le numéro de ligne contenant cet offset
     */
    public int lineOfOffset(int offset) {
        if (offset < 0 || offset > textLength) {
            throw new IndexOutOfBoundsException("offset out of range: " + offset);
        }
        int lo = 0;
        int hi = lineCount - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (lineStarts[mid] <= offset) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo;
    }

    /**
     * Colonne de l'offset donné dans sa ligne : l'offset moins le début de
     * la ligne.
     *
     * @param offset offset, entre 0 et {@link #length()}
     * @return la colonne en unités de code UTF-16
     */
    public int columnOfOffset(int offset) {
        return offset - getLineStart(lineOfOffset(offset));
    }

    /**
     * Offset de la position donnée, avec contrôles de bornes : la colonne ne
     * doit pas dépasser la longueur de la ligne.
     *
     * @param line   numéro de ligne, entre 0 et {@link #getLineCount()} exclu
     * @param column colonne, entre 0 et la longueur de la ligne
     * @return l'offset correspondant
     */
    public int offsetOfPosition(int line, int column) {
        int start = getLineStart(line);
        int end = getLineEnd(line);
        if (column < 0 || start + column > end) {
            throw new IndexOutOfBoundsException(
                    "column out of range: " + column + " for line " + line);
        }
        return start + column;
    }

    /**
     * Position de l'offset donné.
     *
     * @param offset offset, entre 0 et {@link #length()}
     * @return la position (ligne, colonne), jamais {@code null}
     */
    public TextPosition positionOfOffset(int offset) {
        return new TextPosition(lineOfOffset(offset), columnOfOffset(offset));
    }

    // ---------------------------------------------------------- écouteurs

    /**
     * Ajoute un écouteur des modifications (appelé sur le fil d'application).
     *
     * @param listener écouteur à ajouter, jamais {@code null}
     */
    public void addListener(TextListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener must not be null");
        }
        listeners.add(listener);
    }

    /**
     * Ajoute un écouteur EN TÊTE de chaîne : les documents (coloration,
     * structure) s'y inscrivent — leurs caches se réalignent sur le
     * texte AVANT que tout lecteur d'interface ne réagisse à
     * l'édition. Un document ré-ouvert (changement de langage) se
     * ré-enregistre en tête : l'ordre document-avant-interface tient
     * quoi qu'il arrive, sinon un panneau peut lire des jetons
     * périmés au beau milieu du remplacement — le plantage arrive là.
     *
     * @param listener écouteur à ajouter, jamais {@code null}
     */
    public void addListenerFront(TextListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener must not be null");
        }
        headListeners.add(listener);
    }

    /**
     * Retire un écouteur précédemment ajouté (en tête ou en queue) ; un
     * écouteur absent est ignoré silencieusement.
     *
     * @param listener écouteur à retirer
     */
    public void removeListener(TextListener listener) {
        headListeners.remove(listener);
        listeners.remove(listener);
    }

    // ------------------------------------------------------------ édition

    /**
     * Remplace la plage {@code [start, end)} par le texte donné. Réservé au
     * package : les écritures publiques passent par {@link TextTransaction}.
     * Une édition sans effet (plage vide, texte vide) ne modifie ni la
     * révision, ni les écouteurs.
     *
     * @param start       début de la plage, inclus
     * @param end         fin de la plage, exclue
     * @param replacement texte de remplacement, jamais {@code null}
     */
    void replace(int start, int end, CharSequence replacement) {
        checkRange(start, end);
        String text = replacement.toString();
        int removed = end - start;
        int inserted = text.length();
        if (removed == 0 && inserted == 0) {
            return;
        }
        List<TextListener> snapshot;
        synchronized (editLock) {
            updateLineStarts(start, end, removed, inserted, text);
            moveGap(start);
            gapEnd += removed;
            textLength -= removed;
            if (inserted > 0) {
                ensureGapCapacity(inserted);
                text.getChars(0, inserted, content, gapStart);
                gapStart += inserted;
                textLength += inserted;
            }
            revision++;
            // les documents d'abord (en tête), les lecteurs ensuite :
            // l'ordre document-avant-interface est l'invariant qui
            // protège les lectures de mi-remplacement. Les listes
            // copiées à l'écriture (D10) : l'écouteur qui s'abonne ou
            // se retire PENDANT la notification ne lève plus
            snapshot = new ArrayList<>(headListeners.size() + listeners.size());
            snapshot.addAll(headListeners);
            snapshot.addAll(listeners);
        }
        // la notification sort du verrou : un écouteur qui relit le
        // tampon (le moteur de coloration y reprend un cliché) ne
        // s'empile pas sur la mutation
        for (TextListener listener : snapshot) {
            listener.textChanged(this, start, removed, inserted, revision);
        }
    }

    // ------------------------------------------------------------ interne

    private void checkRange(int start, int end) {
        if (start < 0 || end < start || end > textLength) {
            throw new IndexOutOfBoundsException(
                    "invalid range: [" + start + ", " + end + ") for length " + textLength);
        }
    }

    private void checkLine(int line) {
        if (line < 0 || line >= lineCount) {
            throw new IndexOutOfBoundsException("line out of range: " + line);
        }
    }

    private void ensureGapCapacity(int needed) {
        if (gapEnd - gapStart >= needed) {
            return;
        }
        int newCapacity = Math.max(content.length * 2, textLength + needed + 16);
        char[] grown = new char[newCapacity];
        int tailLength = textLength - gapStart;
        System.arraycopy(content, 0, grown, 0, gapStart);
        System.arraycopy(content, gapEnd, grown, newCapacity - tailLength, tailLength);
        content = grown;
        gapEnd = newCapacity - tailLength;
    }

    private void moveGap(int position) {
        if (position < gapStart) {
            int count = gapStart - position;
            System.arraycopy(content, position, content, gapEnd - count, count);
            gapStart -= count;
            gapEnd -= count;
        } else if (position > gapStart) {
            int count = position - gapStart;
            System.arraycopy(content, gapEnd, content, gapStart, count);
            gapStart += count;
            gapEnd += count;
        }
    }

    /**
     * Met à jour l'index de lignes pour le remplacement donné, en unités
     * logiques (avant tout déplacement du trou).
     */
    private void updateLineStarts(int start, int end, int removed, int inserted, String text) {
        int from = firstLineStartAfter(start);
        int to = firstLineStartAfter(end);
        int destroyed = to - from;
        int delta = inserted - removed;
        int newCount = 0;
        for (int i = 0; i < inserted; i++) {
            if (text.charAt(i) == '\n') {
                newCount++;
            }
        }
        int newLineCount = lineCount - destroyed + newCount;
        if (newLineCount > lineStarts.length) {
            lineStarts = grow(lineStarts, newLineCount);
        }
        int destination = from + newCount;
        int tailLength = lineCount - to;
        // rien à décaler : la queue se recopierait sur elle-même à
        // l'identique (le « tout remplacer » du relevé 16 : dix mille
        // remplacements à longueur égale, chacun recopiant la table
        // entière pour un delta nul — six secondes au harnais, l'ANR
        // et la fermeture force sur l'appareil)
        if (destination != to || delta != 0) {
            if (destination < to) {
                for (int i = 0; i < tailLength; i++) {
                    lineStarts[destination + i] = lineStarts[to + i] + delta;
                }
            } else {
                for (int i = tailLength - 1; i >= 0; i--) {
                    lineStarts[destination + i] = lineStarts[to + i] + delta;
                }
            }
        }
        int index = from;
        for (int i = 0; i < inserted; i++) {
            if (text.charAt(i) == '\n') {
                lineStarts[index++] = start + i + 1;
            }
        }
        lineCount = newLineCount;
    }

    /**
     * Indice de la première ligne commençant strictement après l'offset
     * donné ; vaut {@link #lineCount} si aucune ne suit.
     */
    private int firstLineStartAfter(int offset) {
        int lo = 0;
        int hi = lineCount;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (lineStarts[mid] > offset) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        return lo;
    }
}
