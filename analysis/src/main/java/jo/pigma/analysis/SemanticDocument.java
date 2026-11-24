package jo.pigma.analysis;

import java.util.List;
import java.util.Objects;

import jo.pigma.core.TextBuffer;
import jo.pigma.syntax.SyntaxDocument;
import jo.pigma.syntax.StructureDocument;

/**
 * L3, l'annotateur sémantique : les annotations par ligne, par-dessus
 * les jetons de L1 et la profondeur de L2. Le producteur est
 * interchangeable — la démonstration lit des formes Java, les jetons
 * d'un serveur de langage s'y branchent : le modèle de plage est déjà
 * le leur. Aucun état n'est porté : le producteur est pur, sa passe
 * se paie par cuisson de plan (jamais par trame) et la fusion
 * ({@code L3 > L2 > L1} — seul un neutre de L1 peut se colorer) vit
 * au dessin.
 */
public final class SemanticDocument {

    private final TextBuffer buffer;
    private final SyntaxDocument syntax;
    private final StructureDocument structure;
    private final SemanticAnnotator annotator;

    private SemanticDocument(TextBuffer buffer, SyntaxDocument syntax,
            StructureDocument structure, SemanticAnnotator annotator) {
        this.buffer = buffer;
        this.syntax = syntax;
        this.structure = structure;
        this.annotator = annotator;
    }

    /**
     * Ouvre l'annotateur d'un document : aucune passe ne part avant la
     * première demande.
     *
     * @param buffer     le tampon du texte (plages absolues)
     * @param syntax     le moteur de coloration (jetons de L1)
     * @param structure  l'analyseur tolérant (profondeur de L2)
     * @param annotator  le producteur, jamais {@code null}
     * @return le document sémantique prêt, jamais {@code null}
     */
    public static SemanticDocument open(TextBuffer buffer, SyntaxDocument syntax,
            StructureDocument structure, SemanticAnnotator annotator) {
        Objects.requireNonNull(buffer);
        Objects.requireNonNull(syntax);
        Objects.requireNonNull(structure);
        Objects.requireNonNull(annotator);
        return new SemanticDocument(buffer, syntax, structure, annotator);
    }

    /**
     * L'annotation qui couvre un jeton de la ligne (la fusion au
     * dessin s'en sert) : la première dont la plage contient les
     * colonnes du jeton, ou {@code null}.
     *
     * @param line        indice de la ligne
     * @param columnStart colonne de début du jeton
     * @param columnEnd   colonne de fin du jeton
     * @return l'annotation couvrante, ou {@code null}
     */
    public SemanticAnnotation covering(int line, int columnStart, int columnEnd) {
        int lineStart = buffer.getLineStart(line);
        for (SemanticAnnotation note : annotationsAt(line)) {
            if (note.covers(lineStart + columnStart, lineStart + columnEnd)) {
                return note;
            }
        }
        return null;
    }

    /**
     * Les annotations d'une ligne : la passe du producteur, à la
     * demande (force le lexage de la ligne et l'analyse du préfixe,
     * comme le dessin).
     *
     * @param line indice de la ligne
     * @return les annotations, jamais {@code null}
     */
    public List<SemanticAnnotation> annotationsAt(int line) {
        SyntaxDocument.LineTokens entry = syntax.lineTokens(line);
        int depthIn = line == 0 ? 0 : structure.structureOf(line - 1).depthOut();
        int lineStart = buffer.getLineStart(line);
        CharSequence text = buffer.getText(lineStart, buffer.getLineEnd(line));
        return annotator.annotateLine(line, depthIn, entry, text, lineStart);
    }
}
