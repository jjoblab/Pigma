package jo.pigma.editor.popups;

import java.util.ArrayList;
import java.util.List;

/**
 * Le document balisé rendu par les popups (le sous-ensemble des
 * serveurs : titres, paragraphes, listes, code, filets — l'analyseur
 * est JVM PUR, le dessin vit ailleurs). Le défaut du rapport des
 * popups : le survol fondait le balisage en texte nu — les titres,
 * listes et code des serveurs mouraient en chemin. L'analyseur porte
 * le document en BLOCS, chaque bloc en PORTÉES de style (gras,
 * italique, code, lien) — la popup d'info et le panneau de
 * documentation de complétion y dessinent la même facture.
 *
 * <p>Le balisage reconnu : les titres {@code # ## ###} (au-delà de
 * trois, le rang se borne), les paragraphes (les lignes voisines se
 * fondent en un bloc), les listes à puces {@code - * +} et numérotées
 * {@code 1. 1)}, les blocs de code clôturés par
 * {@code ```} (le fichier du langage se lit puis s'ignore), les filets
 * {@code ---} ; en ligne : le gras {@code **…**}, l'italique
 * {@code *…*}, le code {@code `…`} et les liens
 * {@code [libellé](adresse)} — le libellé se montre, l'adresse se
 * garde (le doigt y mènera quand l'hôte l'ouvrira).</p>
 */
public final class MarkdownDoc {

    /** Un titre : son rang (1 à 3) et ses portées. */
    public static final int HEADING = 0;

    /** Un paragraphe et ses portées. */
    public static final int PARAGRAPH = 1;

    /** Une entrée de liste à puce et ses portées. */
    public static final int BULLET = 2;

    /** Une entrée de liste numérotée et ses portées. */
    public static final int NUMBER = 3;

    /** Un bloc de code : son texte BRUT (aucune portée dedans). */
    public static final int CODE = 4;

    /** Un filet horizontal. */
    public static final int RULE = 5;

    /**
     * Un bloc du document : son genre, son rang (le titre), ses
     * portées (vides pour le code et le filet), son texte brut (le
     * code seul) et sa MARQUE de liste (la numérotation d'une entrée
     * numérotée — « 1. », « 2. » ; vide pour les autres).
     *
     * @param kind    le genre (une des constantes ci-dessus)
     * @param level   le rang du titre (0 pour les autres)
     * @param spans   les portées du bloc (vide pour le code, le filet)
     * @param rawText le texte brut (le code du bloc de code ; vide sinon)
     * @param marker  la marque de liste (« 1. » ; vide sinon)
     */
    public record Block(int kind, int level, List<Span> spans,
            String rawText, String marker) {

        /** Le bloc normalisé : portées et textes jamais nuls. */
        public Block {
            spans = spans == null ? List.of() : List.copyOf(spans);
            rawText = rawText == null ? "" : rawText;
            marker = marker == null ? "" : marker;
        }
    }

    /**
     * Une portée de texte : sa valeur, ses drapeaux de style et son
     * éventuelle adresse de lien (le libellé se montre, l'adresse se
     * garde).
     *
     * @param text   la valeur montrée
     * @param bold   le gras
     * @param italic l'italique
     * @param code   le code en ligne (l'encre encre et le fond teinté)
     * @param link   l'adresse du lien (vide pour aucun)
     */
    public record Span(String text, boolean bold, boolean italic,
            boolean code, String link) {

        /** La portée normalisée : valeur jamais nulle. */
        public Span {
            text = text == null ? "" : text;
            link = link == null ? "" : link;
        }
    }

    private MarkdownDoc() {
        // l'analyseur vit en ses méthodes seules
    }

    /**
     * Analyse un document balisé : les blocs s'y suivent dans l'ordre
     * du texte ; un document vide ou nul rend la liste vide.
     *
     * @param markdown le document (nu : la liste vide ; balisé : ses
     *                 blocs)
     * @return les blocs, jamais null
     */
    public static List<Block> parse(String markdown) {
        List<Block> blocks = new ArrayList<>();
        if (markdown == null || markdown.isEmpty()) {
            return blocks;
        }
        String[] lines = markdown.split("\n", -1);
        List<Span> pending = null;
        String fence = null;
        StringBuilder code = null;
        for (String rawLine : lines) {
            String line = rawLine.stripTrailing();
            if (code != null) {
                if (line.strip().equals(fence)) {
                    blocks.add(new Block(CODE, 0, List.of(), code.toString(), ""));
                    code = null;
                    fence = null;
                } else {
                    code.append(line).append('\n');
                }
                continue;
            }
            String trimmed = line.strip();
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                flushParagraph(blocks, pending);
                pending = null;
                fence = trimmed.substring(0, 3);
                code = new StringBuilder();
                continue;
            }
            if (trimmed.isEmpty()) {
                flushParagraph(blocks, pending);
                pending = null;
                continue;
            }
            int[] heading = headingOf(trimmed);
            if (heading != null) {
                flushParagraph(blocks, pending);
                pending = null;
                blocks.add(new Block(HEADING, heading[0],
                        inline(trimmed.substring(heading[1])), "", ""));
                continue;
            }
            if (trimmed.matches("[-*_]{3,}")) {
                flushParagraph(blocks, pending);
                pending = null;
                blocks.add(new Block(RULE, 0, List.of(), "", ""));
                continue;
            }
            int bulletAt = bulletAt(trimmed);
            if (bulletAt >= 0) {
                flushParagraph(blocks, pending);
                pending = null;
                blocks.add(new Block(BULLET, 0,
                        inline(trimmed.substring(bulletAt + 1).strip()), "", ""));
                continue;
            }
            int numberAt = numberAt(trimmed);
            if (numberAt >= 0) {
                flushParagraph(blocks, pending);
                pending = null;
                blocks.add(new Block(NUMBER, 0,
                        inline(trimmed.substring(numberAt).strip()), "",
                        trimmed.substring(0, numberAt).strip()));
                continue;
            }
            if (pending == null) {
                pending = new ArrayList<>();
            }
            pending.addAll(inline(line.strip()));
            pending.add(new Span(" ", false, false, false, ""));
        }
        flushParagraph(blocks, pending);
        return blocks;
    }

    /**
     * Le document porte-t-il plus que du texte nu ? (le rendu balisé
     * ne se paie que pour le balisé — le texte nu court tout seul).
     *
     * @param markdown le document en question
     * @return vrai quand un bloc est plus qu'un paragraphe nu
     */
    public static boolean isRich(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return false;
        }
        for (int at = 0; at + 1 < markdown.length(); at++) {
            char c = markdown.charAt(at);
            if (c == '`' || c == '*' || c == '#' || c == '['
                    || c == '\n' && markdown.charAt(at + 1) == '\n') {
                return true;
            }
        }
        return false;
    }

    /** Le paragraphe assemblé se pose (l'espace de queue s'y coupe). */
    private static void flushParagraph(List<Block> blocks, List<Span> pending) {
        if (pending == null || pending.isEmpty()) {
            return;
        }
        Span last = pending.get(pending.size() - 1);
        if (last.text().equals(" ")) {
            pending.remove(pending.size() - 1);
        }
        if (!pending.isEmpty()) {
            blocks.add(new Block(PARAGRAPH, 0, pending, "", ""));
        }
    }

    /**
     * Le rang et la coupe d'un titre : de UN à SIX dièses, suivis
     * d'une espace ; au-delà du rang trois le titre se borne (le
     * dessin ne distingue pas plus).
     */
    private static int[] headingOf(String line) {
        if (!line.startsWith("#")) {
            return null;
        }
        int level = 0;
        while (level < line.length() && line.charAt(level) == '#') {
            level++;
        }
        if (level == 0 || level >= line.length()
                || !Character.isWhitespace(line.charAt(level))) {
            return null;
        }
        return new int[] {Math.min(3, level), level};
    }

    /** La coupe de la puce d'une entrée de liste, -1 pour aucune. */
    private static int bulletAt(String line) {
        if (line.length() < 2) {
            return -1;
        }
        char first = line.charAt(0);
        if ((first == '-' || first == '*' || first == '+')
                && Character.isWhitespace(line.charAt(1))) {
            return 1;
        }
        return -1;
    }

    /** La coupe de la marque d'une entrée numérotée, -1 pour aucune. */
    private static int numberAt(String line) {
        int digits = 0;
        while (digits < line.length() && Character.isDigit(line.charAt(digits))) {
            digits++;
        }
        if (digits == 0 || digits + 1 >= line.length()) {
            return -1;
        }
        char after = line.charAt(digits);
        if ((after == '.' || after == ')')
                && Character.isWhitespace(line.charAt(digits + 1))) {
            return digits + 2;
        }
        return -1;
    }

    /**
     * Les portées d'une ligne : le code en ligne, le gras, l'italique
     * et les liens s'y découpent — le reste court en portées nues.
     * Le gras et l'italique s'entrelacent (le gras d'abord, l'italique
     * dans le gras ensuite) ; les paires qui ne se ferment pas
     * demeurent du texte.
     */
    private static List<Span> inline(String line) {
        List<Span> spans = new ArrayList<>();
        int at = 0;
        StringBuilder plain = new StringBuilder();
        while (at < line.length()) {
            char c = line.charAt(at);
            if (c == '`') {
                int close = line.indexOf('`', at + 1);
                if (close > at + 1) {
                    flushPlain(spans, plain);
                    spans.add(new Span(line.substring(at + 1, close),
                            false, false, true, ""));
                    at = close + 1;
                    continue;
                }
            }
            if (c == '*' && at + 1 < line.length() && line.charAt(at + 1) == '*') {
                int close = line.indexOf("**", at + 2);
                if (close > at + 1) {
                    flushPlain(spans, plain);
                    spans.addAll(italicsInside(line.substring(at + 2, close)));
                    at = close + 2;
                    continue;
                }
            }
            if (c == '*' && isBoundarySpace(line, at)) {
                int close = line.indexOf('*', at + 1);
                if (close > at) {
                    flushPlain(spans, plain);
                    spans.add(new Span(line.substring(at + 1, close),
                            false, true, false, ""));
                    at = close + 1;
                    continue;
                }
            }
            if (c == '[') {
                int linkClose = line.indexOf(']', at + 1);
                if (linkClose > at && linkClose + 1 < line.length()
                        && line.charAt(linkClose + 1) == '(') {
                    int addressClose = line.indexOf(')', linkClose + 2);
                    if (addressClose > linkClose) {
                        flushPlain(spans, plain);
                        spans.add(new Span(line.substring(at + 1, linkClose),
                                false, false, false,
                                line.substring(linkClose + 2, addressClose)));
                        at = addressClose + 1;
                        continue;
                    }
                }
            }
            plain.append(c);
            at++;
        }
        flushPlain(spans, plain);
        return spans;
    }

    /**
     * L'italique DANS un gras : la découpe du gras porte déjà, ses
     * astérisques simples y découpent l'italique (le gras reste sur
     * chaque morceau).
     */
    private static List<Span> italicsInside(String inside) {
        List<Span> spans = new ArrayList<>();
        int at = 0;
        StringBuilder plain = new StringBuilder();
        while (at < inside.length()) {
            char c = inside.charAt(at);
            if (c == '*') {
                int close = inside.indexOf('*', at + 1);
                if (close > at) {
                    flushPlain(spans, plain, true);
                    spans.add(new Span(inside.substring(at + 1, close),
                            true, true, false, ""));
                    at = close + 1;
                    continue;
                }
            }
            plain.append(c);
            at++;
        }
        flushPlain(spans, plain, true);
        return spans;
    }

    /** Le texte court se pose en portée nue (le gras posé s'y garde). */
    private static void flushPlain(List<Span> spans, StringBuilder plain,
            boolean bold) {
        if (plain.length() > 0) {
            spans.add(new Span(plain.toString(), bold, false, false, ""));
            plain.setLength(0);
        }
    }

    /** Le texte court se pose en portée nue. */
    private static void flushPlain(List<Span> spans, StringBuilder plain) {
        flushPlain(spans, plain, false);
    }

    /**
     * L'astérisque ouvre-t-elle l'italique ? (une espace DEVANT
     * l'astérisque l'ouvre ; collée au mot qui précède, elle est du
     * texte — la multiplication collée ne vole pas le mot).
     */
    private static boolean isBoundarySpace(String line, int at) {
        return at == 0 || Character.isWhitespace(line.charAt(at - 1));
    }
}
