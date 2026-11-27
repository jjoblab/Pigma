package jo.pigma.editor.internal.render;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import jo.pigma.editor.render.LinePlan;

/**
 * Le PEINTRE du corps des lignes (le composant de DESSIN du découpage
 * de la vue — R1) : les glyphes du texte au plan cuit
 * (les segments, leurs couleurs, le gras sémantique), les chips de
 * couleur littérale, les ligatures d'opérateurs dessinées, le
 * placeholder du pli replié, le soulignement de composition.
 *
 * <p>Le peintre est PUR AU SENS DE LA GÉOMÉTRIE EXTRAITE : il vit de mesures
 * posées de l'extérieur (la chasse, l'ascendance et la descendance
 * de la police — la vue les tient de la sienne après la mesure), du
 * thème posé (la couleur du texte), et de plans déjà cuits ; il ne
 * connaît ni le tampon, ni les plis, ni le défilement — la vue
 * tranche les fenêtres (la coupe du pli, la rangée du soft wrap)
 * et lui passe le texte à peindre. Les pinceaux vivent chez lui ;
 * la vue partage {@link #textPaint()} (la loupe et la mesure y
 * lisent le même pinceau — un seul objet, un seul réglage).</p>
 */
public final class Painter {

    /** Le pinceau du texte (la vue le partage : loupe, mesure). */
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le soulignement de composition (l'IME y surligne son mot). */
    private final Paint composePaint = new Paint();

    /** Le placeholder du pli replié (l'encre atténuée du texte). */
    private final Paint placeholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Les ligatures d'opérateurs dessinées (FiraCode à la référence). */
    private final Paint ligaturePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le fond TEINTÉ du chip de couleur (le littéral se lit dessus). */
    private final Paint colorChipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le liseré du chip (la couleur PLEINE du littéral). */
    private final Paint colorChipBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** La chasse d'un glyphe (la vue la pose après la mesure). */
    private float charWidth = 1.0f;

    /** L'ascendance de la police (le dessus des glyphes). */
    private float ascent = -1.0f;

    /** La descendance de la police (le dessous des glyphes). */
    private float descent = 0.25f;

    /** La couleur du texte du thème (les ligatures, le placeholder). */
    private int defaultColor = 0xFF333333;

    /** La densité de l'écran (le dp en pixels). */
    private final float density;

    /**
     * Un peintre neuf, ses pinceaux à la police à espacement fixe.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public Painter(float density) {
        this.density = density;
        textPaint.setTypeface(Typeface.MONOSPACE);
        composePaint.setColor(0xFF7A8199);
        ligaturePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    /** Le pinceau du texte (la vue le partage — un seul objet). */
    public Paint textPaint() {
        return textPaint;
    }

    /**
     * Le corps du texte se pose (la vue mesure ensuite : la chasse
     * et les métriques se lisent du pinceau partagé).
     *
     * @param textPx le corps en pixels
     */
    public void applyTextSize(float textPx) {
        textPaint.setTextSize(textPx);
        // le placeholder suit le corps (le retour du terrain : il ne
        // se posait JAMAIS — la puce du pli vivait à la taille par
        // DÉFAUT du pinceau, minuscule à côté du texte)
        placeholderPaint.setTextSize(textPx);
        placeholderPaint.setTypeface(Typeface.MONOSPACE);
        placeholderPaint.setFakeBoldText(true);
    }

    /**
     * Les mesures posées de l'extérieur (la vue les tient de la
     * police réelle du cadre — après la mesure).
     *
     * @param charWidth la chasse d'un glyphe (px)
     * @param ascent    l'ascendance (négative, px)
     * @param descent   la descendance (positive, px)
     */
    public void applyMeasures(float charWidth, float ascent,
            float descent) {
        this.charWidth = Math.max(1.0f, charWidth);
        this.ascent = ascent;
        this.descent = descent;
    }

    /**
     * Le thème posé : la couleur du texte (les ligatures et le
     * placeholder s'y teintent).
     *
     * @param color la couleur du texte du thème (ARGB)
     */
    public void applyTheme(int color) {
        defaultColor = color;
    }

    /**
     * Les segments du plan, la COUPE respectée : les segments
     * au-delà de la coupe ne se peignent pas, celui qui l'englobe
     * se tranche (le retour de terrain sur les plis : le « {…} »
     * se peignait par-dessus l'ouvrant et sa suite).
     *
     * @param canvas     le canevas (déjà translaté du défilement)
     * @param plan       le plan cuit de la ligne
     * @param revealCols la coupe en colonnes (la fin visible)
     * @param left       l'abscisse du début de la ligne (px)
     * @param baseline   la ligne de base (px)
     */
    public void drawTokens(Canvas canvas, LinePlan plan, int revealCols,
            float left, float baseline) {
        String[] texts = plan.texts();
        int[] colors = plan.colors();
        int[] cols = plan.cols();
        float[] xs = plan.xs();
        boolean[] bolds = plan.bolds();
        for (int at = 0; at < texts.length; at++) {
            if (cols[at] >= revealCols) {
                break;
            }
            String piece = texts[at];
            if (cols[at] + piece.length() > revealCols) {
                piece = piece.substring(0, revealCols - cols[at]);
            }
            textPaint.setColor(colors[at]);
            // le gras sémantique (L3) : un drapeau de peinture, pas
            // une seconde police
            textPaint.setFakeBoldText(bolds != null && bolds[at]);
            canvas.drawText(piece, left + xs[at], baseline, textPaint);
            if (piece.length() < texts[at].length()) {
                break;
            }
        }
        textPaint.setFakeBoldText(false);
    }

    /**
     * La TRANCHE du plan entre deux colonnes (le rendu du soft
     * wrap : la rangée y dessine SES colonnes du plan cuit, le
     * décalage d'indent y pose la suite au premier caractère non
     * blanc de la ligne) — la COLONNE du segment y vit au plan
     * (l'abscisse est en pixels : la caste d'avant la lisait comme
     * une colonne, la rangée ne s'alignait qu'à chasse unitaire).
     *
     * @param canvas   le canevas (déjà translaté du défilement)
     * @param plan     le plan cuit de la ligne
     * @param fromCol  la première colonne de la rangée (incluse)
     * @param toCol    la dernière colonne de la rangée (exclue)
     * @param left     l'abscisse du début de la RANGÉE (px, l'indent
     *                 compris)
     * @param baseline la ligne de base de la rangée (px)
     */
    public void drawTokenRange(Canvas canvas, LinePlan plan, int fromCol,
            int toCol, float left, float baseline) {
        String[] texts = plan.texts();
        int[] colors = plan.colors();
        int[] cols = plan.cols();
        boolean[] bolds = plan.bolds();
        for (int at = 0; at < texts.length; at++) {
            int segFrom = cols[at];
            int segLen = texts[at].length();
            if (segFrom + segLen <= fromCol || segFrom >= toCol) {
                continue;
            }
            int cutStart = Math.max(0, fromCol - segFrom);
            int cutEnd = Math.min(segLen, toCol - segFrom);
            if (cutEnd <= cutStart) {
                continue;
            }
            String piece = texts[at].substring(cutStart, cutEnd);
            textPaint.setColor(colors[at]);
            textPaint.setFakeBoldText(bolds != null && bolds[at]);
            canvas.drawText(piece,
                    left + (segFrom + cutStart - fromCol) * charWidth,
                    baseline, textPaint);
        }
        textPaint.setFakeBoldText(false);
    }

    /**
     * Les CHIPS DE COULEUR (la leçon de la référence : son éditeur y
     * encadre chaque littéral hexadécimal DE SA PROPRE couleur) : la
     * fenêtre du texte se balaie, chaque littéral
     * #RGB/#RGBA/#RRGGBB/#RRGGBBAA porte un rectangle arrondi
     * rempli de sa couleur (alpha réduit — le littéral se lit
     * dessus), liseré discret PLEIN.
     *
     * @param canvas   le canevas
     * @param text     la fenêtre de la ligne (déjà tranchée par la vue)
     * @param window   la longueur utile de la fenêtre
     * @param left     l'abscisse du début de la ligne (px)
     * @param baseline la ligne de base (px)
     */
    public void drawColorChips(Canvas canvas, CharSequence text,
            int window, float left, float baseline) {
        float top = baseline + ascent * 0.82f;
        float bottom = baseline + descent * 0.55f;
        int at = 0;
        while (at < window) {
            if (text.charAt(at) != '#') {
                at++;
                continue;
            }
            // les chiffres hexadécimaux qui suivent le dièse
            int digits = 0;
            while (at + 1 + digits < window
                    && isHexDigit(text.charAt(at + 1 + digits))) {
                digits++;
            }
            if (digits == 3 || digits == 4 || digits == 6 || digits == 8) {
                String literal = text.subSequence(at, at + 1 + digits)
                        .toString();
                Integer color = parseColorLiteral(literal);
                if (color != null) {
                    float chipLeft = left + at * charWidth;
                    float chipRight = chipLeft + (1 + digits) * charWidth;
                    colorChipPaint.setColor(
                            (color & 0x00FFFFFF) | 0x30000000);
                    canvas.drawRoundRect(chipLeft, top, chipRight, bottom,
                            dp(3), dp(3), colorChipPaint);
                    colorChipBorderPaint.setColor(color | 0xFF000000);
                    colorChipBorderPaint.setStrokeWidth(
                            Math.max(1.0f, dp(1)));
                    colorChipBorderPaint.setStyle(Paint.Style.STROKE);
                    canvas.drawRoundRect(chipLeft, top, chipRight, bottom,
                            dp(3), dp(3), colorChipBorderPaint);
                }
                at += 1 + digits;
            } else {
                at++;
            }
        }
    }

    /**
     * Les LIGATURES d'opérateurs dessinées (la police monospace
     * d'Android n'en porte pas — le pinceau les fond comme FiraCode
     * à la référence) : chaque paire reconnue se recouvre de son
     * glyphe unifié — la barre du fond, le trait qui l'anime — à la
     * couleur du texte du thème, dans la case des DEUX caractères.
     *
     * @param canvas   le canevas
     * @param text     la fenêtre de la ligne (déjà tranchée par la vue)
     * @param window   la longueur utile de la fenêtre
     * @param left     l'abscisse du début de la ligne (px)
     * @param baseline la ligne de base (px)
     */
    public void drawLigatures(Canvas canvas, CharSequence text,
            int window, float left, float baseline) {
        ligaturePaint.setStrokeWidth(Math.max(1.2f, charWidth * 0.10f));
        float midY = baseline - ascent * 0.38f;
        for (int at = 0; at + 1 < window; at++) {
            char a = text.charAt(at);
            char b = text.charAt(at + 1);
            String ligature = ligatureOf(a, b);
            if (ligature == null) {
                continue;
            }
            float cx = left + at * charWidth + charWidth;
            float halfChar = charWidth * 0.5f;
            ligaturePaint.setColor(defaultColor);
            switch (ligature) {
                case "arrow" -> drawArrowLigature(canvas, cx, midY, halfChar);
                case "ne" -> drawNeLigature(canvas, cx, midY, halfChar);
                case "cmp" -> drawCmpLigature(canvas, cx, midY, halfChar);
                case "logic" -> drawLogicLigature(canvas, cx, midY, halfChar);
                case "incdec" -> drawIncDecLigature(canvas, cx, midY, halfChar);
                default -> {
                }
            }
        }
    }

    /**
     * Le placeholder du pli replié (la nature du pli se lit à son
     * ouvrant — « {…} » pour un bloc, « //... » pour un commentaire) :
     * LE TEXTE AU PLEIN dans une PUCE lisible (le retour du terrain :
     * le « {…} » nu se lisait trop petit — sa puce vivait à la taille
     * par défaut du pinceau), la puce teintée du thème et liserée,
     * coiffée d'un quart de la hauteur des glyphes.
     *
     * @param canvas      le canevas (déjà translaté : AUCUN scrollX ici)
     * @param placeholder le texte du placeholder
     * @param x            l'abscisse du placeholder (px)
     * @param baseline     la ligne de base (px)
     */
    public void drawFoldPlaceholder(Canvas canvas, String placeholder,
            float x, float baseline) {
        float pad = dp(4);
        float textW = placeholderPaint.measureText(placeholder);
        float top = baseline + ascent * 0.95f - dp(1);
        float bottom = baseline + descent * 0.5f + dp(1);
        colorChipPaint.setColor((defaultColor & 0x00FFFFFF) | 0x22000000);
        canvas.drawRoundRect(x, top, x + textW + 2 * pad, bottom,
                dp(6), dp(6), colorChipPaint);
        colorChipBorderPaint.setColor((defaultColor & 0x00FFFFFF) | 0x88000000);
        colorChipBorderPaint.setStrokeWidth(Math.max(1.0f, dp(1)));
        colorChipBorderPaint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(x, top, x + textW + 2 * pad, bottom,
                dp(6), dp(6), colorChipBorderPaint);
        placeholderPaint.setColor(defaultColor | 0xFF000000);
        canvas.drawText(placeholder, x + pad, baseline, placeholderPaint);
    }

    /**
     * Le soulignement de composition (l'IME y surligne son mot en
     * cours) entre deux colonnes.
     *
     * @param canvas   le canevas
     * @param fromCol  la première colonne (incluse)
     * @param toCol    la dernière colonne (exclue)
     * @param left     l'abscisse du début de la ligne (px)
     * @param baseline la ligne de base (px)
     */
    public void drawComposeUnderline(Canvas canvas, int fromCol, int toCol,
            float left, float baseline) {
        if (toCol <= fromCol) {
            return;
        }
        float x1 = left + fromCol * charWidth;
        float x2 = left + toCol * charWidth;
        canvas.drawRect(x1, baseline + dp(1), x2, baseline + dp(3),
                composePaint);
    }

    /** La paire reconnue dit son espèce de ligature (null : aucune) —
     * pure, sans état : le harnais JVM y lit sa table. */
    static String ligatureOf(char a, char b) {
        if (a == '=' && b == '>') {
            return "arrow";
        }
        if (a == '-' && b == '>') {
            return "arrow";
        }
        if (a == '!' && b == '=') {
            return "ne";
        }
        if ((a == '>' && b == '=') || (a == '<' && b == '=')
                || (a == '=' && b == '=')) {
            return "cmp";
        }
        if ((a == '&' && b == '&') || (a == '|' && b == '|')) {
            return "logic";
        }
        if ((a == '+' && b == '+') || (a == '-' && b == '-')) {
            return "incdec";
        }
        if ((a == '+' && b == '=') || (a == '-' && b == '=')
                || (a == '*' && b == '=') || (a == '/' && b == '=')
                || (a == '%' && b == '=')) {
            return "cmp";
        }
        return null;
    }

    /** La flèche unifiée (=> ou ->) : la barre traversante, la pointe à droite. */
    private void drawArrowLigature(Canvas canvas, float cx, float midY,
            float halfChar) {
        float fromX = cx - halfChar - charWidth * 0.25f;
        float toX = cx + halfChar + charWidth * 0.25f;
        canvas.drawLine(fromX, midY, toX, midY, ligaturePaint);
        float head = charWidth * 0.28f;
        canvas.drawLine(toX - head, midY - head * 0.8f, toX, midY,
                ligaturePaint);
        canvas.drawLine(toX - head, midY + head * 0.8f, toX, midY,
                ligaturePaint);
    }

    /** Le non-égal unifié (!=) : la barre traversante barrée. */
    private void drawNeLigature(Canvas canvas, float cx, float midY,
            float halfChar) {
        drawCmpLigature(canvas, cx, midY, halfChar);
        // la barre du NON : la diagonale qui refuse l'égalité
        float half = halfChar + charWidth * 0.25f;
        canvas.drawLine(cx - half * 0.5f, midY + half * 0.45f,
                cx + half * 0.5f, midY - half * 0.45f, ligaturePaint);
    }

    /** La comparaison unifiée (>=, <=, ==, +=...) : la barre double soudée. */
    private void drawCmpLigature(Canvas canvas, float cx, float midY,
            float halfChar) {
        float fromX = cx - halfChar - charWidth * 0.2f;
        float toX = cx + halfChar + charWidth * 0.2f;
        // le trait unique qui soude les deux signes (le premier se
        // recouvre, le second se fond)
        canvas.drawLine(fromX, midY, toX, midY, ligaturePaint);
    }

    /** La conjonction unifiée (&& ou ||) : les deux signes soudés par la barre. */
    private void drawLogicLigature(Canvas canvas, float cx, float midY,
            float halfChar) {
        float fromX = cx - halfChar - charWidth * 0.2f;
        float toX = cx + halfChar + charWidth * 0.2f;
        canvas.drawLine(fromX, midY, toX, midY, ligaturePaint);
    }

    /** L'incrémentation unifiée (++ ou --) : les deux signes soudés. */
    private void drawIncDecLigature(Canvas canvas, float cx, float midY,
            float halfChar) {
        float fromX = cx - halfChar - charWidth * 0.2f;
        float toX = cx + halfChar + charWidth * 0.2f;
        canvas.drawLine(fromX, midY, toX, midY, ligaturePaint);
    }

    /**
     * Le littéral hexadécimal se parse en couleur ARGB (null : il ne
     * se parse pas — le chip ne se peint pas).
     */
    static Integer parseColorLiteral(String literal) {
        try {
            long value = Long.parseLong(literal.substring(1), 16);
            return switch (literal.length() - 1) {
                case 3 -> 0xFF000000 | (int) (((value >> 8 & 0xF) * 17) << 16
                        | ((value >> 4 & 0xF) * 17) << 8
                        | (value & 0xF) * 17);
                case 4 -> (int) (((value & 0xF) * 17) << 24
                        | ((value >> 12 & 0xF) * 17) << 16
                        | ((value >> 8 & 0xF) * 17) << 8
                        | (value >> 4 & 0xF) * 17);
                case 6 -> (int) (0xFF000000L | value);
                case 8 -> (int) value;
                default -> null;
            };
        } catch (NumberFormatException error) {
            return null;
        }
    }

    /** Le chiffre hexadécimal ? */
    static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }

    private float dp(int value) {
        return value * density;
    }
}
