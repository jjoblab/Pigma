package jo.pigma.editor.folding;

import java.util.ArrayList;
import java.util.List;

import jo.pigma.core.TextBuffer;
import jo.pigma.syntax.SyntaxDocument;
import jo.pigma.syntax.StructureDocument;

/**
 * La table des guides verticaux structurels — un guide par BLOC du
 * document, jamais un trait par cran de blancs : la ligne qui porte
 * l'accolade ouvrante donne SA colonne au guide, le trait se tient
 * devant le premier caractère de cette ligne ; le trait part de la
 * ligne APRÈS l'ouvrante et meurt à la ligne AVANT la fermante :
 * jamais sur l'ouvrante, jamais sur la fermante, et les lignes vides
 * du corps se joignent en lui — une seule ligne continue par bloc.
 *
 * <p>La structure décide, pas la géométrie des blancs : les signes
 * s'y lisent par les JETONS du lexeur (une accolade dans une chaîne
 * ou un commentaire vit dans un jeton plus long, elle n'ouvre rien)
 * et la profondeur de chaque ligne vient du document de structure —
 * le guide se tire collé à l'analyse courante, jamais depuis une
 * lecture périmée. Les blocs ouverts et jamais refermés (document
 * ouvert) ne portent PAS de guide : leur fermant ne vit nulle part,
 * le trait n'aurait pas de fin.</p>
 *
 * <p>La table porte la RÉVISION du tampon qui l'a portée : le dessin
 * ne la lit que collée au texte courant (une table d'une autre
 * révision ne s'aligne jamais sur les rangées vivantes). La table se
 * bâtit à la demande, sur la fenêtre visible et son pourtour — le
 * coût se paie par fenêtre, pas par document.</p>
 */
public final class StructureGuides {

    /**
     * La marge de la fenêtre élargie : les paires se cherchent au-delà
     * de la fenêtre visible (l'ouvrant au-dessus, le fermant plus
     * bas) — la table n'y paie que le pourtour, le document entier
     * reste à la demande.
     */
    private static final int WINDOW_MARGIN_LINES = 64;

    /**
     * Un guide structurel : la colonne du trait (l'INDENTATION de la
     * ligne qui porte l'ouvrante — le fil se tient devant le premier
     * caractère de cette ligne, à sa marge), la colonne du GLYPHE
     * ouvrant (les accolades jumelles s'y lisent), la ligne qui porte
     * l'ouvrante, la ligne qui porte la fermante (le trait couvre les
     * lignes STRICTEMENT entre les deux).
     *
     * @param column        l'indentation de la ligne ouvrante (le trait)
     * @param openingColumn la colonne du glyphe ouvrant dans sa ligne
     * @param openingLine   la ligne qui porte l'ouvrante
     * @param closingLine   la ligne qui porte la fermante
     */
    public record Guide(int column, int openingColumn, int openingLine,
            int closingLine) {
    }

    /** La table vide : aucune révision ne la porte, rien ne se trace. */
    public static final StructureGuides EMPTY =
            new StructureGuides(-1, List.of());

    private final int revision;
    private final List<Guide> guides;

    private StructureGuides(int revision, List<Guide> guides) {
        this.revision = revision;
        this.guides = guides;
    }

    /** La révision du tampon qui a porté la dérivation. */
    public int revision() {
        return revision;
    }

    /** Le nombre de guides de la table. */
    public int size() {
        return guides.size();
    }

    /**
     * Le guide d'indice donné — la table se lit à l'indice (aucun
     * itérateur alloué dans la boucle de dessin).
     *
     * @param index l'indice du guide, dans {@code [0, size())}
     * @return le guide
     */
    public Guide guide(int index) {
        return guides.get(index);
    }

    /**
     * La table des guides d'une fenêtre : les lignes visibles et un
     * pourtour d'une ligne de chaque côté (l'ouvrant ou le fermant y
     * vivent souvent hors fenêtre — la paire se cherche un peu au-delà
     * de ce que l'œil voit). La pile des ouvrants vit en colonnes de
     * texte ; les fermants y désemplissent : un guide naît quand la
     * paire referme, sur des lignes DISTINCTES (une paire sur sa
     * propre ligne n'aligne rien). La recherche s'étend au-delà de la
     * fenêtre tant que la pile porte des ouvrants de la fenêtre : un
     * bloc ouvert en vue peut refermer des centaines de lignes plus
     * bas.
     *
     * @param buffer    le tampon du texte
     * @param syntax    le document de coloration (les jetons de L1)
     * @param structure le document de structure (les profondeurs)
     * @param fromLine  la première ligne de la fenêtre élargie
     * @param toLine    la dernière ligne de la fenêtre élargie (inclusive)
     * @return la table, ou {@link #EMPTY} sans structure
     */
    public static StructureGuides of(TextBuffer buffer, SyntaxDocument syntax,
            StructureDocument structure, int fromLine, int toLine) {
        if (structure == null || syntax == null) {
            return EMPTY;
        }
        int count = buffer.getLineCount();
        int first = Math.max(0, fromLine);
        int last = Math.min(toLine, count - 1);
        if (first > last) {
            return EMPTY;
        }
        // la fenêtre ÉLARGIE : les ouvrants et fermants vivent souvent
        // hors fenêtre — la paire se cherche au-delà de ce que l'œil
        // voit (l'ouvrant au-dessus, le fermant des centaines de
        // lignes plus bas), la pile ne se vide qu'aux ouvrants sûrs
        int scanLast = Math.min(last + WINDOW_MARGIN_LINES, count - 1);
        int scanFirst = Math.max(0, first - WINDOW_MARGIN_LINES);
        // la profondeur d'entrée de la première ligne lue : le point
        // de contrôle de la structure (jamais de relecture du début)
        int depth = scanFirst == 0 ? 0
                : structure.structureOf(scanFirst - 1).depthOut();
        List<int[]> openStack = new ArrayList<>();
        List<Guide> found = new ArrayList<>();
        for (int line = scanFirst; line <= scanLast; line++) {
            SyntaxDocument.LineTokens entry = syntax.lineTokens(line);
            int lineStart = buffer.getLineStart(line);
            for (int at = 0; at < entry.tokenCount(); at++) {
                if (entry.endAt(at) - entry.startAt(at) != 1) {
                    continue;
                }
                char sign = buffer.charAt(lineStart + entry.startAt(at));
                if (sign == '{' || sign == '(' || sign == '[') {
                    openStack.add(new int[] {line, entry.startAt(at), depth});
                    depth++;
                } else if (sign == '}' || sign == ')' || sign == ']') {
                    if (depth == 0 || openStack.isEmpty()) {
                        // fermant orphelin OU ouvrant hors fenêtre : le
                        // trait ne peut pas naître, la profondeur de la
                        // structure re-synchronisera à la ligne suivante
                        continue;
                    }
                    depth--;
                    int[] opened = openStack.remove(openStack.size() - 1);
                    if (opened[0] != line && opened[0] >= first
                            && opened[0] <= last) {
                        int lineEnd = buffer.getLineEnd(opened[0]);
                        found.add(new Guide(
                                indentationOf(buffer, lineStartOf(buffer,
                                        opened[0]), lineEnd),
                                opened[1], opened[0], line));
                    }
                }
            }
            // l'entrée de la ligne suivante (le point de contrôle —
            // les signes hors jetons ne comptent pas, la structure
            // seule dit la profondeur)
            if (line < scanLast) {
                depth = structure.structureOf(line).depthOut();
            }
        }
        return new StructureGuides(buffer.getRevision(), List.copyOf(found));
    }

    /**
     * Les indices des guides ACCENTUÉS : les blocs qui ENTOURENT le
     * caret (le plus profond l'illumine, ses parents aussi — la
     * chaîne de portée se lit d'un coup d'œil), et celui du bloc de
     * l'accolade APPARIÉE quand le caret touche son ouvrante ou sa
     * fermante (les accolades jumelles). Un seul accent par image :
     * la structure se lit sans crier.
     *
     * @param caretLine   la ligne du caret
     * @param caretColumn la colonne du caret dans sa ligne
     * @return les indices distincts, vide sans accent
     */
    public int[] activeGuides(int caretLine, int caretColumn) {
        List<Integer> active = new ArrayList<>(4);
        for (int at = 0; at < guides.size(); at++) {
            Guide guide = guides.get(at);
            boolean inside = caretLine > guide.openingLine()
                    && caretLine < guide.closingLine();
            if (inside && caretColumn > guide.column()) {
                active.add(at);
                continue;
            }
            // les accolades jumelles : le caret TOUCHE l'ouvrante ou
            // la fermante du bloc (au GLYPHE, pas à la marge du trait)
            boolean onOpening = caretLine == guide.openingLine()
                    && caretColumn >= guide.openingColumn()
                    && caretColumn <= guide.openingColumn() + 1;
            boolean onClosing = caretLine == guide.closingLine();
            if (onOpening || onClosing) {
                active.add(at);
            }
        }
        int[] output = new int[active.size()];
        for (int at = 0; at < output.length; at++) {
            output[at] = active.get(at);
        }
        return output;
    }

    /** Le début de la ligne (le raccourci du tampon). */
    private static int lineStartOf(TextBuffer buffer, int line) {
        return buffer.getLineStart(line);
    }

    /**
     * L'indentation visuelle d'une ligne : l'espace compte un, la
     * tabulation au taquet de 4 (le fil se tient à la marge du texte
     * qui ouvre le bloc, comme le premier caractère de sa ligne).
     */
    private static int indentationOf(TextBuffer buffer, int start, int end) {
        int width = 0;
        for (int at = start; at < end; at++) {
            char c = buffer.charAt(at);
            if (c == ' ') {
                width++;
            } else if (c == '\t') {
                width += 4 - (width % 4);
            } else {
                break;
            }
        }
        return width;
    }
}
