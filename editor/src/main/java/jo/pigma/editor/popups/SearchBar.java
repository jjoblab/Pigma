package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.Toast;

import java.util.Locale;

import jo.pigma.editor.EditorView;

/**
 * La carte de recherche et remplacement, DESSINÉE DANS LA VUE
 * (jamais de fenêtre flottante, pas de jeton de fenêtre, pas de
 * conflit de focus avec l'IME) ; rien ne prend le focus, le clavier
 * reste à l'éditeur, qui rend les verdicts des touches à la carte
 * (KeyboardCard) — et quand le doigt vise un champ, l'éditeur y lève
 * le clavier (le champ s'y voit actif : le doigt ne cherche pas
 * ailleurs). La pose : AU CARET — dessous si elle tient, dessus
 * sinon — bornée aux murs de la vue, et le caret la suit à chaque
 * dessin. L'habillage reste celui d'Android Studio : le compte « i/n »
 * qui rougit quand rien ne correspond, les chevrons, la croix, et le
 * remplacement replié sous la recherche (le rabat le montre).
 *
 * <p>La recherche est incrémentale (amortie de 150 ms — les frappes
 * rapides n'envoient que la dernière) ; remplacer la correspondance
 * courante fait une unité d'annulation, tout remplacer une seule
 * (les remplacements vont de la fin vers le début — les positions
 * antérieures restent valides).</p>
 */
public final class SearchBar implements KeyboardCard {

    /** L'amortissement de la recherche incrémentale (ms). */
    private static final long SEARCH_DEBOUNCE_MS = 150L;

    /** La hauteur d'une rangée de la carte (dp). */
    private static final int ROW_HEIGHT_DP = 44;

    /** La respiration intérieure de la carte (dp). */
    private static final int PAD_DP = 8;

    /** La largeur d'un glyphe d'action (dp). */
    private static final int GLYPH_DP = 44;

    /** La largeur plancher de la carte (dp). */
    private static final int MIN_WIDTH_DP = 320;

    /** La largeur plafond de la carte (dp). */
    private static final int MAX_WIDTH_DP = 400;

    private final StringBuilder query = new StringBuilder();

    private final StringBuilder replacement = new StringBuilder();

    /** La rangée du remplacement, repliée au départ (le rabat la montre). */
    private boolean replaceShown;

    /** Le champ actif du clavier délégué : la requête, ou le remplacement. */
    private boolean activeReplacement;

    /** Ce que la dernière composition a posé dans le champ actif (la composition remplace). */
    private int composedLength;

    /** Le doigt vient-il de viser un champ ? (l'éditeur y lève le clavier). */
    private boolean fieldTapped;

    private boolean shown;

    /** La pose de la carte (px, coordonnées de la vue) — le harnais la lit. */
    private float poseX;
    private float poseY;
    private float cardW;
    private float cardH;
    private float ancreX;
    private float ancreY;
    private float density;

    private final Runnable searchSync = this::runIncrementalSearch;

    private EditorView editor;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint counterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint zeroCounterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activePaint = new Paint();

    /**
     * Une carte neuve, ses pinceaux propres.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public SearchBar(float density) {
        cardPaint.setColor(0xFFF3F3F3);
        borderPaint.setColor(0xFFB5B5B5);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f);
        textPaint.setColor(0xFF202124);
        textPaint.setTextSize(16.0f * density);
        hintPaint.setColor(0xFF80868B);
        hintPaint.setTextSize(16.0f * density);
        counterPaint.setColor(0xFF5F6368);
        counterPaint.setTextSize(13.0f * density);
        zeroCounterPaint.setColor(0xFFC62828);
        zeroCounterPaint.setTextSize(13.0f * density);
        glyphPaint.setColor(0xFF5F6368);
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.6f * density);
        activePaint.setColor(0xFF1A73E8);
    }

    /**
     * Branche la carte à son éditeur : les actions y parlent, le
     * clavier délégué y passe, la fermeture lui revient.
     *
     * @param editor l'éditeur recherché et remplacé
     */
    public void bind(EditorView editor) {
        this.editor = editor;
    }

    /** La carte se montre : la recherche part d'un champ vierge. */
    public void show() {
        shown = true;
    }

    /**
     * La carte se range : la requête et le remplacement se vident, le
     * rabat se replie, la recherche amortie se range avec.
     */
    public void hide() {
        shown = false;
        query.setLength(0);
        replacement.setLength(0);
        replaceShown = false;
        activeReplacement = false;
        fieldTapped = false;
        if (editor != null) {
            editor.removeCallbacks(searchSync);
        }
    }

    /** La carte vit-elle ? */
    public boolean isShown() {
        return shown;
    }

    /**
     * Le doigt vient-il de viser un CHAMP de la carte ? (la requête,
     * le remplacement — l'éditeur y lève le clavier logiciel après le
     * toucher ; le drapeau se lit une fois, le toucher suivant le
     * repose).
     *
     * @return vrai quand le dernier toucher visait un champ
     */
    public boolean keyboardWanted() {
        return fieldTapped;
    }

    /**
     * Pose la carte au caret : dessous si elle tient, dessus sinon,
     * l'abscisse du caret bornée
     * aux murs. La pose se recalcule à chaque dessin : le caret la
     * suit (défilement, navigation).
     *
     * @param metrics la géométrie courante de la vue
     */
    public void poser(PopupMetrics metrics) {
        density = metrics.density();
        ancreX = metrics.caretX();
        ancreY = metrics.caretY();
        float margin = dp(8);
        float pad = dp(PAD_DP);
        float rowHeight = dp(ROW_HEIGHT_DP);
        cardW = Math.max(Math.min(metrics.viewWidth() - 2 * margin,
                dp(MAX_WIDTH_DP)), Math.min(dp(MIN_WIDTH_DP),
                        metrics.viewWidth() - 2 * margin));
        cardH = 2 * pad + rowHeight * (replaceShown ? 2 : 1);
        float gap = dp(4);
        float below = ancreY + metrics.lineHeight() + gap;
        poseY = below + cardH <= metrics.viewHeight() - margin
                ? below
                : Math.max(margin, ancreY - gap - cardH);
        poseX = Math.max(margin,
                Math.min(ancreX, metrics.viewWidth() - cardW - margin));
    }

    /**
     * Dessine la carte (la pose se recalcule d'abord — le caret la
     * suit), par-dessus tout.
     *
     * @param canvas  le canevas de la vue
     * @param metrics la géométrie courante de la vue
     */
    public void draw(Canvas canvas, PopupMetrics metrics) {
        poser(metrics);
        float radius = dp(12);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, borderPaint);
        float pad = dp(PAD_DP);
        float rowHeight = dp(ROW_HEIGHT_DP);
        float row1Top = poseY + pad;
        float row2Top = row1Top + rowHeight;
        drawRow1(canvas, row1Top);
        if (replaceShown) {
            drawRow2(canvas, row2Top);
        }
    }

    /**
     * Le doigt tombé sur la carte lui appartient (les popups se
     * dessinent et se touchent) : la zone agit, le geste
     * se consomme. Hors de la carte : faux — le geste poursuit sa
     * vie vers l'éditeur.
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     * @return vrai quand la carte a pris le geste
     */
    public boolean onTouchArea(float x, float y) {
        if (!shown || x < poseX || x >= poseX + cardW
                || y < poseY || y >= poseY + cardH) {
            return false;
        }
        float pad = dp(PAD_DP);
        float rowHeight = dp(ROW_HEIGHT_DP);
        float glyphW = dp(GLYPH_DP);
        boolean inRow1 = y < poseY + pad + rowHeight;
        if (inRow1) {
            float right = poseX + cardW - dp(2);
            if (x >= right - glyphW) {
                close();
                return true;
            }
            if (x >= right - 2 * glyphW) {
                editor.searchNext();
                return true;
            }
            if (x >= right - 3 * glyphW) {
                editor.searchPrevious();
                return true;
            }
            if (x < poseX + dp(2) + glyphW) {
                replaceShown = !replaceShown;
                activeReplacement = replaceShown;
                return true;
            }
            // la rangée de la requête : le clavier délégué lui revient —
            // l'éditeur y lève le clavier
            activeReplacement = false;
            fieldTapped = true;
            return true;
        }
        if (replaceShown) {
            float right = poseX + cardW - dp(2);
            if (x >= right - glyphW) {
                replaceEverywhere();
                return true;
            }
            if (x >= right - 2 * glyphW) {
                replaceCurrent();
                return true;
            }
            // la rangée du remplacement : le clavier délégué lui revient
            activeReplacement = true;
            fieldTapped = true;
            return true;
        }
        return true;
    }

    /**
     * Le texte tapé va au champ actif (insert neuf : la composition
     * qui suit, s'il y en a une, remplacera ces caractères).
     */
    @Override
    public boolean onText(CharSequence text) {
        if (text == null || text.length() == 0 || text.charAt(0) == '\n') {
            return true;
        }
        if (activeReplacement) {
            replacement.append(text);
        } else {
            query.append(text);
            scheduleIncrementalSearch();
        }
        composedLength = text.length();
        return true;
    }

    /**
     * La composition du clavier remplace ce qu'elle avait posé : le
     * champ actif rend sa dernière composition, la nouvelle s'y
     * écrit (la requête relance la recherche amortie).
     */
    @Override
    public boolean onComposition(CharSequence text) {
        if (text == null || text.length() == 0) {
            return true;
        }
        StringBuilder champ = activeReplacement ? replacement : query;
        champ.setLength(Math.max(0, champ.length() - composedLength));
        champ.append(text);
        composedLength = text.length();
        if (!activeReplacement) {
            scheduleIncrementalSearch();
        }
        return true;
    }

    /** Le retour arrière efface dans le champ actif (la composition se rompt). */
    @Override
    public boolean onBackspace() {
        composedLength = 0;
        if (activeReplacement && replacement.length() > 0) {
            replacement.setLength(replacement.length() - 1);
        } else if (!activeReplacement && query.length() > 0) {
            query.setLength(query.length() - 1);
            scheduleIncrementalSearch();
        }
        return true;
    }

    /** La flèche verticale navigue : bas = suivante, haut = précédente. */
    @Override
    public boolean onArrow(int pas) {
        if (pas > 0) {
            editor.searchNext();
        } else {
            editor.searchPrevious();
        }
        return true;
    }

    /** La validation (Entrée) va à la correspondance suivante. */
    @Override
    public boolean onEnter() {
        editor.searchNext();
        return true;
    }

    /** Le rabat montre le remplacement (le clavier délégué y passe). */
    public void showReplaceRow() {
        replaceShown = true;
        activeReplacement = true;
    }

    /** Le remplacement se montre-t-il ? (le harnais le lit) */
    public boolean replaceRowShown() {
        return replaceShown;
    }

    /** Le clavier délégué revient à la requête. */
    public void focusQueryField() {
        activeReplacement = false;
    }

    /** La requête courante (le harnais la lit). */
    public String queryText() {
        return query.toString();
    }

    /** Le remplacement courant (le harnais le lit). */
    public String replacementText() {
        return replacement.toString();
    }

    /** Le compte « i/n » — rouge quand rien ne correspond. */
    public String counterLabel() {
        if (editor == null) {
            return "0/0";
        }
        int total = editor.searchMatchCount();
        int current = total == 0 ? 0 : editor.searchCurrentIndex() + 1;
        return String.format(Locale.ROOT, "%d/%d", current, total);
    }

    /**
     * Remplace la correspondance courante : une unité d'annulation.
     */
    public void replaceCurrent() {
        editor.replaceCurrent(replacement.toString());
    }

    /**
     * Remplace toutes les correspondances : une seule unité
     * d'annulation, le compte se notifie.
     */
    public void replaceEverywhere() {
        int count = editor.replaceAll(replacement.toString());
        Toast.makeText(editor.getContext(), count + " remplacement(s)",
                Toast.LENGTH_SHORT).show();
    }

    /** La croix range la carte (la recherche se range avec). */
    public void close() {
        editor.dismissSearchPopup();
    }

    /** L'abscisse posée (le harnais la lit). */
    public float poseX() {
        return poseX;
    }

    /** L'ordonnée posée (le harnais la lit). */
    public float poseY() {
        return poseY;
    }

    /** L'ordonnée du caret d'ancrage (le harnais la lit). */
    public float ancreY() {
        return ancreY;
    }

    /** L'abscisse du caret d'ancrage (le harnais la lit). */
    public float ancreX() {
        return ancreX;
    }

    /** La largeur de la carte (le harnais la lit). */
    public float width() {
        return cardW;
    }

    /** La hauteur de la carte (le harnais la lit). */
    public float height() {
        return cardH;
    }

    /**
     * Une frappe arrive : la recherche se reprogramme — les frappes
     * rapides n'envoient qu'une recherche, la dernière.
     */
    private void scheduleIncrementalSearch() {
        if (editor == null) {
            return;
        }
        editor.removeCallbacks(searchSync);
        editor.postDelayed(searchSync, SEARCH_DEBOUNCE_MS);
    }

    /**
     * La recherche incrémentale : le champ vient de bouger, la
     * recherche le suit (le champ vide range la recherche). La
     * navigation va à la correspondance la plus proche du curseur.
     */
    private void runIncrementalSearch() {
        if (query.length() == 0) {
            editor.clearSearch();
        } else {
            editor.search(query.toString());
        }
    }

    /** La rangée de recherche : le rabat, la requête, le compte, les chevrons, la croix. */
    private void drawRow1(Canvas canvas, float rowTop) {
        float glyphW = dp(GLYPH_DP);
        float rowHeight = dp(ROW_HEIGHT_DP);
        float baseline = rowTop + rowHeight * 0.7f;
        // le rabat : deux chevrons tête-bêche
        float toggleCx = poseX + dp(2) + glyphW / 2;
        float toggleCy = rowTop + rowHeight / 2;
        float s = dp(6);
        float k = s * 0.45f;
        float t = s * 0.55f;
        canvas.drawLine(toggleCx - t, toggleCy - k,
                toggleCx, toggleCy - k - t, glyphPaint);
        canvas.drawLine(toggleCx, toggleCy - k - t,
                toggleCx + t, toggleCy - k, glyphPaint);
        canvas.drawLine(toggleCx - t, toggleCy + k,
                toggleCx, toggleCy + k + t, glyphPaint);
        canvas.drawLine(toggleCx, toggleCy + k + t,
                toggleCx + t, toggleCy + k, glyphPaint);
        // la requête (l'indication si le champ est vierge)
        float textX = poseX + dp(2) + glyphW + dp(8);
        float counterX = poseX + cardW - dp(2) - 3 * glyphW - dp(8);
        String label = coupe(activeReplacement || query.length() == 0
                ? "chercher…" : query.toString(),
                textPaint, counterX - textX - dp(8));
        canvas.drawText(label, textX, baseline,
                query.length() == 0 ? hintPaint : textPaint);
        // le compte « i/n » — rouge quand rien ne correspond
        String counter = counterLabel();
        int total = editor == null ? 0 : editor.searchMatchCount();
        canvas.drawText(counter,
                counterX - counterPaint.measureText(counter), baseline,
                total == 0 ? zeroCounterPaint : counterPaint);
        // les chevrons et la croix, de droite à gauche
        float right = poseX + cardW - dp(2);
        drawCross(canvas, right - glyphW + glyphW / 2, toggleCy);
        drawChevron(canvas, right - 2 * glyphW + glyphW / 2, toggleCy, true);
        drawChevron(canvas, right - 3 * glyphW + glyphW / 2, toggleCy, false);
        // le champ actif se souligne — le clavier délégué s'y voit
        if (!activeReplacement) {
            canvas.drawRect(textX, rowTop + rowHeight - dp(3),
                    counterX - dp(8), rowTop + rowHeight, activePaint);
        }
    }

    /** La rangée du remplacement : le champ, remplacer, tout remplacer. */
    private void drawRow2(Canvas canvas, float rowTop) {
        float glyphW = dp(GLYPH_DP);
        float rowHeight = dp(ROW_HEIGHT_DP);
        float baseline = rowTop + rowHeight * 0.7f;
        float textX = poseX + dp(10);
        float actionsX = poseX + cardW - dp(2) - 2 * glyphW;
        String label = coupe(activeReplacement && replacement.length() > 0
                ? replacement.toString() : "remplacer par…",
                textPaint, actionsX - textX - dp(8));
        canvas.drawText(label, textX, baseline,
                replacement.length() == 0 ? hintPaint : textPaint);
        float right = poseX + cardW - dp(2);
        float cy = rowTop + rowHeight / 2;
        drawReplaceAll(canvas, right - glyphW + glyphW / 2, cy);
        drawReplace(canvas, right - 2 * glyphW + glyphW / 2, cy);
        if (activeReplacement) {
            canvas.drawRect(textX, rowTop + rowHeight - dp(3),
                    actionsX - dp(8), rowTop + rowHeight, activePaint);
        }
    }

    /** Un chevron vers le haut (haut) ou le bas (bas). */
    private void drawChevron(Canvas canvas, float cx, float cy, boolean up) {
        float s = dp(6);
        float k = s * 0.45f;
        if (up) {
            canvas.drawLine(cx - s, cy + k, cx, cy - k, glyphPaint);
            canvas.drawLine(cx, cy - k, cx + s, cy + k, glyphPaint);
        } else {
            canvas.drawLine(cx - s, cy - k, cx, cy + k, glyphPaint);
            canvas.drawLine(cx, cy + k, cx + s, cy - k, glyphPaint);
        }
    }

    /** La croix de fermeture. */
    private void drawCross(Canvas canvas, float cx, float cy) {
        float s = dp(6);
        canvas.drawLine(cx - s, cy - s, cx + s, cy + s, glyphPaint);
        canvas.drawLine(cx - s, cy + s, cx + s, cy - s, glyphPaint);
    }

    /** La flèche de remplacement : le texte file vers la barre. */
    private void drawReplace(Canvas canvas, float cx, float cy) {
        float s = dp(6);
        float k = s * 0.45f;
        canvas.drawLine(cx + s, cy - s, cx + s, cy + s, glyphPaint);
        canvas.drawLine(cx - s, cy, cx + s * 0.45f, cy, glyphPaint);
        canvas.drawLine(cx + s * 0.45f - k, cy - k, cx + s * 0.45f, cy, glyphPaint);
        canvas.drawLine(cx + s * 0.45f - k, cy + k, cx + s * 0.45f, cy, glyphPaint);
    }

    /** Le tout-remplacer : deux flèches vers la barre. */
    private void drawReplaceAll(Canvas canvas, float cx, float cy) {
        float s = dp(6);
        float k = s * 0.45f;
        canvas.drawLine(cx + s, cy - s, cx + s, cy + s, glyphPaint);
        for (int row = 0; row < 2; row++) {
            float y = cy + (row == 0 ? -s * 0.45f : s * 0.45f);
            canvas.drawLine(cx - s, y, cx + s * 0.45f, y, glyphPaint);
            canvas.drawLine(cx + s * 0.45f - k, y - k, cx + s * 0.45f, y, glyphPaint);
            canvas.drawLine(cx + s * 0.45f - k, y + k, cx + s * 0.45f, y, glyphPaint);
        }
    }

    /**
     * Coupe un texte à la largeur disponible (la queue se garde —
     * la tête s'abrège en « … »).
     */
    private String coupe(String text, Paint paint, float maxWidth) {
        if (paint.measureText(text) <= maxWidth || text.length() < 2) {
            return text;
        }
        String tail = text.substring(1);
        while (tail.length() > 1
                && paint.measureText("…" + tail) > maxWidth) {
            tail = tail.substring(1);
        }
        return "…" + tail;
    }

    private float dp(int value) {
        return value * density;
    }
}
