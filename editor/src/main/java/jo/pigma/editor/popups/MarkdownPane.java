package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

/**
 * Le panneau de documentation balisée : la MISE EN PAGE et le DESSIN
 * des blocs d'un {@link MarkdownDoc} — les titres en gras plus fiers,
 * les paragraphes repliés à la largeur, les listes à leur marque (les
 * suites retirées de la marque), le code à son fond teinté, les liens
 * à l'encre d'accent soulignés. La popup d'info et le panneau de
 * documentation de la complétion y dessinent la même facture ; la
 * classe ne connaît ni la pose ni le tampon — elle reçoit ses blocs,
 * sa largeur et son coin.
 */
final class MarkdownPane {

    /** Les drapeaux de style d'une portée dessinée. */
    static final int BOLD = 1;
    static final int ITALIC = 2;
    static final int CODE = 4;
    static final int LINK = 8;

    /** Le facteur de taille des titres par rang (le rang trois reste sobre). */
    private static final float[] HEADING_SCALE = {1.28f, 1.16f, 1.06f};

    /**
     * Une rangée mise en page : ses portées dessinées, sa hauteur, la
     * taille de son texte (les titres courent plus fiers), le fond de
     * code qui la teinte, le filet qui s'y trace seul, et le retrait
     * de la suite (la continuation d'une entrée de liste).
     */
    record Row(List<Run> runs, float height, float sizeSp, boolean code,
            boolean rule, float indent) {
    }

    /**
     * Une portée dessinée : son texte et ses drapeaux de style (le
     * gras, l'italique, le code, le lien).
     */
    record Run(String text, int style) {
    }

    private final Paint codePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint codeBackground = new Paint();
    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linkUnderline = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float density = 1.0f;

    private PopupStyle style = PopupStyle.defaults();

    /** Un panneau neuf au style par défaut. */
    MarkdownPane(float density) {
        this.density = density;
        applyStyle();
    }

    /** Pose le style : les encres s'y reconfigurent. */
    void setStyle(PopupStyle fresh) {
        style = fresh == null ? PopupStyle.defaults() : fresh;
        applyStyle();
    }

    /** Reconfigure les pinceaux au style courant. */
    private void applyStyle() {
        codePaint.setColor(style.textArgb());
        codePaint.setTextSize(style.textSizeSp() * density);
        codeBackground.setColor((style.borderArgb() & 0x00FFFFFF) | 0x2A000000);
        codeBackground.setStyle(Paint.Style.FILL);
        rulePaint.setColor(style.borderArgb());
        rulePaint.setStrokeWidth(1.0f);
        linkUnderline.setColor(style.accentArgb());
        linkUnderline.setStrokeWidth(1.0f);
    }

    /**
     * Met en page les blocs à la largeur : les titres en gras et plus
     * fiers selon leur rang, les paragraphes repliés au mot, les
     * entrées de liste à leur marque (les suites se retirent de la
     * marque), le code ligne à ligne à son fond plein. Le plafond
     * borne les rangées : la queue s'y abrège en « … » (zéro :
     * aucune borne).
     *
     * @param blocks  les blocs du document
     * @param width   la largeur du panneau (px)
     * @param maxRows le plafond de rangées (0 : aucune borne)
     * @return les rangées mises en page, jamais null
     */
    List<Row> layout(List<MarkdownDoc.Block> blocks, float width, int maxRows) {
        List<Row> rows = new ArrayList<>();
        for (MarkdownDoc.Block block : blocks) {
            switch (block.kind()) {
                case MarkdownDoc.HEADING -> layoutHeading(rows, block, width);
                case MarkdownDoc.PARAGRAPH -> layoutText(rows,
                        block.spans(), width, 0.0f, "");
                case MarkdownDoc.BULLET -> layoutText(rows, block.spans(),
                        width, dp(12), "\u2022  ");
                case MarkdownDoc.NUMBER -> layoutText(rows, block.spans(),
                        width, dp(12), block.marker());
                case MarkdownDoc.CODE -> layoutCode(rows, block);
                case MarkdownDoc.RULE -> rows.add(new Row(List.of(), dp(12),
                        style.textSizeSp() * density, false, true, 0.0f));
                default -> {
                    // le genre inconnu se tait
                }
            }
        }
        return maxRows > 0 ? capped(rows, maxRows) : rows;
    }

    /** La hauteur des rangées mises en page. */
    float height(List<Row> rows) {
        float total = 0.0f;
        for (Row row : rows) {
            total += row.height();
        }
        return total;
    }

    /**
     * Dessine les rangées au coin posé : le code à son fond teinté,
     * les portées à leurs encres (gras, italique penchée, code à
     * l'encre d'accent, lien souligné), le filet seul sur sa rangée.
     * La BASELINE vit aux quatre-cinquièmes de la rangée — la hauteur
     * se dit de la TAILLE du texte (la taille et son quart la portent :
     * le corps y respire SANS les mesures de la police — la hauteur
     * demeure déterministe, le doigt qui défile y voit la même carte).
     *
     * @param canvas le canevas du panneau
     * @param rows   les rangées mises en page
     * @param left   le bord gauche du panneau
     * @param top    le bord haut du panneau
     * @param width  la largeur du panneau
     */
    void draw(Canvas canvas, List<Row> rows, float left, float top, float width) {
        float rowTop = top;
        for (Row row : rows) {
            if (row.rule()) {
                float mid = rowTop + row.height() / 2.0f;
                canvas.drawLine(left + dp(2), mid, left + width - dp(2), mid,
                        rulePaint);
                rowTop += row.height();
                continue;
            }
            if (row.code()) {
                canvas.drawRoundRect(left, rowTop, left + width,
                        rowTop + row.height() - dp(2), dp(2), dp(2),
                        codeBackground);
            }
            float cursor = left + row.indent();
            float baseline = rowTop + row.height() * 0.8f;
            for (Run run : row.runs()) {
                Paint paint = paintOf(run.style(), row.sizeSp());
                float runW = paint.measureText(run.text());
                canvas.drawText(run.text(), cursor, baseline, paint);
                if ((run.style() & LINK) != 0) {
                    canvas.drawLine(cursor, baseline + dp(2), cursor + runW,
                            baseline + dp(2), linkUnderline);
                }
                cursor += runW;
            }
            rowTop += row.height();
        }
    }

    /** Les rangées au plafond : la dernière porte l'abréviation de queue. */
    private List<Row> capped(List<Row> rows, int maxRows) {
        if (rows.size() <= maxRows) {
            return rows;
        }
        List<Row> kept = new ArrayList<>(rows.subList(0, maxRows));
        Row last = kept.get(maxRows - 1);
        List<Run> runs = new ArrayList<>(last.runs());
        if (!runs.isEmpty()) {
            Run tail = runs.get(runs.size() - 1);
            runs.set(runs.size() - 1, new Run(tail.text() + "\u2026", tail.style()));
        } else {
            runs.add(new Run("\u2026", 0));
        }
        kept.set(maxRows - 1, new Row(runs, last.height(), last.sizeSp(),
                last.code(), last.rule(), last.indent()));
        return kept;
    }

    /** Les titres : gras et plus fiers selon leur rang. */
    private void layoutHeading(List<Row> rows, MarkdownDoc.Block block,
            float width) {
        int level = Math.max(1, Math.min(3, block.level()));
        float scale = HEADING_SCALE[level - 1];
        float sizeSp = style.textSizeSp() * density * scale;
        List<Run> runs = new ArrayList<>();
        for (MarkdownDoc.Span span : block.spans()) {
            runs.add(new Run(span.text(), spanStyle(span) | BOLD));
        }
        wrapRuns(rows, runs, width, 0.0f, dp(5), sizeSp);
    }

    /** Un paragraphe, une entrée de liste : replié au mot, suites retirées. */
    private void layoutText(List<Row> rows, List<MarkdownDoc.Span> spans,
            float width, float indent, String marker) {
        List<Run> runs = new ArrayList<>();
        if (!marker.isEmpty()) {
            runs.add(new Run(marker, 0));
        }
        for (MarkdownDoc.Span span : spans) {
            runs.add(new Run(span.text(), spanStyle(span)));
        }
        wrapRuns(rows, runs, width, indent, dp(3),
                style.textSizeSp() * density);
    }

    /** Le code : ligne à ligne, au fond plein de sa rangée. */
    private void layoutCode(List<Row> rows, MarkdownDoc.Block block) {
        String text = block.rawText();
        int at = 0;
        while (at < text.length()) {
            int end = text.indexOf('\n', at);
            if (end < 0) {
                end = text.length();
            }
            String line = text.substring(at, end);
            List<Run> runs = List.of(new Run(line.isEmpty() ? " " : line, 0));
            rows.add(new Row(runs, codeRowHeight(),
                    style.textSizeSp() * density, true, false, dp(4)));
            at = end + 1;
        }
    }

    /** Les drapeaux d'une portée du document. */
    private static int spanStyle(MarkdownDoc.Span span) {
        int bits = 0;
        if (span.bold()) {
            bits |= BOLD;
        }
        if (span.italic()) {
            bits |= ITALIC;
        }
        if (span.code()) {
            bits |= CODE;
        }
        if (!span.link().isEmpty()) {
            bits |= LINK;
        }
        return bits;
    }

    /**
     * Le repli des portées à la largeur : les mots s'y suivent (un mot
     * seul plus large que la rangée s'y coupe), la PREMIÈRE rangée
     * part du bord (la marque y vit), les suites se retirent de
     * l'indentation posée, l'interligne respire.
     */
    private void wrapRuns(List<Row> rows, List<Run> runs, float width,
            float indent, float spacing, float sizeSp) {
        float available = width;
        List<Run> line = new ArrayList<>();
        float lineW = 0.0f;
        boolean firstRow = true;
        for (Run run : runs) {
            Paint paint = paintOf(run.style(), sizeSp);
            String text = run.text();
            int at = 0;
            while (at < text.length()) {
                int wordEnd = at;
                while (wordEnd < text.length()
                        && !Character.isWhitespace(text.charAt(wordEnd))) {
                    wordEnd++;
                }
                String word = text.substring(at, wordEnd);
                float wordW = paint.measureText(word);
                float room = available - (firstRow ? 0.0f : indent);
                if (!line.isEmpty() && lineW + wordW > room) {
                    rows.add(new Row(line, rowHeight(sizeSp) + spacing, sizeSp,
                            false, false, firstRow ? 0.0f : indent));
                    line = new ArrayList<>();
                    lineW = 0.0f;
                    firstRow = false;
                }
                if (lineW + wordW <= room || line.isEmpty()) {
                    if (wordW > room && line.isEmpty() && word.length() > 1) {
                        // le mot seul plus large que la rangée s'y coupe
                        while (word.length() > 1 && paint.measureText(word) > room) {
                            word = word.substring(0, word.length() - 1);
                        }
                        wordW = paint.measureText(word);
                    }
                    line.add(new Run(word, run.style()));
                    lineW += wordW;
                }
                at = wordEnd + 1;
            }
        }
        if (!line.isEmpty()) {
            rows.add(new Row(line, rowHeight(sizeSp) + spacing, sizeSp,
                    false, false, firstRow ? 0.0f : indent));
        }
    }

    /** La hauteur d'une rangée à la taille posée (la taille et son
     * quart la portent — le corps y respire, la hauteur demeure
     * déterministe). */
    private float rowHeight(float sizeSp) {
        return sizeSp * 1.25f;
    }

    /** La hauteur d'une rangée de code (l'interligne y respire). */
    private float codeRowHeight() {
        return codePaint.getTextSize() * 1.25f + dp(6);
    }

    /** Le pinceau d'une portée : ses drapeaux y configurent l'encre. */
    private Paint paintOf(int styleBits, float sizeSp) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(style.hintArgb());
        paint.setTextSize(sizeSp);
        if ((styleBits & CODE) != 0 || (styleBits & LINK) != 0) {
            paint.setColor(style.accentArgb());
        }
        if ((styleBits & BOLD) != 0) {
            paint.setFakeBoldText(true);
        }
        if ((styleBits & ITALIC) != 0) {
            paint.setTextSkewX(-0.25f);
        }
        return paint;
    }

    private float dp(int value) {
        return value * density;
    }
}
