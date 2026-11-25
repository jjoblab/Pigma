#!/usr/bin/env python3
"""Chantier popups + arborescence : extrait les popups d'EditorView vers
le paquet jo.pigma.editor.popups (InfoPopup, SuggestionBand, PopupMetrics)
et ouvre jo.pigma.editor.render (RenderCache). L'API publique d'EditorView
ne change pas : elle délègue."""
import subprocess
from pathlib import Path

EDITOR = Path("editor/src/main/java/jo/pigma/editor/EditorView.java")
VIEW = EDITOR.read_text(encoding="utf-8")


def swap(old, new):
    global VIEW
    assert VIEW.count(old) == 1, f"introuvable ou multiple : {old[:70]!r} ({VIEW.count(old)})"
    VIEW = VIEW.replace(old, new)


# --- imports du paquet des popups
swap("package jo.pigma.editor;\n",
     "package jo.pigma.editor;\n\nimport jo.pigma.editor.popups.InfoPopup;\n"
     "import jo.pigma.editor.popups.PopupMetrics;\n"
     "import jo.pigma.editor.popups.SuggestionBand;\n")

# --- champs : la popup et le bandeau remplacent leurs états
swap("""    /** Le texte de la popup d'info ancrée (null : masquée). */
    private String infoPopupText;

    /** La ligne d'ancrage de la popup d'info. */
    private int infoPopupLine;

    /** La colonne d'ancrage de la popup d'info. */
    private int infoPopupColumn;
""", """    /** La popup d'info ancrée — état et dessin au paquet des popups. */
    private InfoPopup infoPopup;
""")
swap("""    /** Les candidats du bandeau de complétion (le mot courant). */
    private List<String> suggestions = List.of();

    /** La borne de début du mot courant (l'insertion remplace le préfixe). */
    private int suggestionWordStart = -1;

    /** Les abscisses des candidats dessinés (le toucher s'y teste). */
    private float[] suggestionXs = new float[0];
""", """    /** Le bandeau de complétion — état et dessin au paquet des popups. */
    private SuggestionBand suggestionBand;

    /** La borne de début du mot courant (l'insertion remplace le préfixe). */
    private int suggestionWordStart = -1;
""")

# --- peintures : les classes les portent
swap("""    /** Le texte de la popup d'info ancrée. */
    private final Paint infoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le fond de la popup d'info. */
    private final Paint infoBackground = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le bord de la popup d'info. */
    private final Paint infoBorder = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le souligné des diagnostics d'appariement. */
    private final Paint diagnosticPaint = new Paint();

    /** Le texte des candidats de complétion (bandeau). */
    private final Paint suggestionPaint = new Paint();

    /** Le fond du bandeau de complétion. */
    private final Paint suggestionBackground = new Paint();
""", """    /** Le souligné des diagnostics d'appariement. */
    private final Paint diagnosticPaint = new Paint();
""")
swap("""        infoPaint.setColor(0xFF1B1B1F);
        infoPaint.setTextSize(dp(12));
        infoBackground.setStyle(Paint.Style.FILL);
        infoBackground.setColor(0xF2FFFFFF);
        infoBorder.setStyle(Paint.Style.STROKE);
        infoBorder.setStrokeWidth(dp(1));
        infoBorder.setColor(0xFF1B6FE0);
""", """        infoPopup = new InfoPopup(dp(1));
""")
swap("""        suggestionPaint.setColor(0xFF1B1B1F);
        suggestionPaint.setTextSize(dp(13));
        suggestionBackground.setStyle(Paint.Style.FILL);
        suggestionBackground.setColor(0x11000000);
""", """        suggestionBand = new SuggestionBand(dp(1));
""")

# --- les états résiduels
swap("""        if (infoPopupText != null) {
            infoPopupText = null;
""", """        if (infoPopup.text() != null) {
            infoPopup.dismiss();
""")
VIEW = VIEW.replace("        infoPopupText = null;", "        infoPopup.dismiss();")
VIEW = VIEW.replace("        return infoPopupText != null;", "        return infoPopup.text() != null;")
swap("""        infoPopupText = text == null || text.isEmpty() ? null : text;
        infoPopupLine = line;
        infoPopupColumn = column;
""", """        infoPopup.show(text, line, column);
""")
swap("""        if (infoPopupText == null || infoPopupLine != line
                || infoPopupColumn != column) {
            return false;
        }
        infoPopupText = text == null || text.isEmpty() ? infoPopupText : text;
""", """        if (!infoPopup.replace(text, line, column)) {
            return false;
        }
""")
VIEW = VIEW.replace("        return infoPopupText == null ? -1 : infoPopupLine;",
                    "        return infoPopup.line();")
VIEW = VIEW.replace("        return infoPopupText == null ? -1 : infoPopupColumn;",
                    "        return infoPopup.column();")
VIEW = VIEW.replace("        return infoPopupText;", "        return infoPopup.text();")
VIEW = VIEW.replace("            suggestions = List.of();",
                    "            suggestionBand.set(List.of());")
swap("""        suggestions = symbolIndex == null
                ? List.of() : symbolIndex.query(prefix, 8);
""", """        suggestionBand.set(symbolIndex == null
                ? List.of() : symbolIndex.query(prefix, 8));
""")
swap("            suggestions = suggestionProvider.completionsFor(prefix,",
     "            suggestionBand.set(suggestionProvider.completionsFor(prefix,")
VIEW = VIEW.replace("        suggestions = List.of();\n        suggestionWordStart = -1;",
                    "        suggestionBand.set(List.of());\n        suggestionWordStart = -1;")
VIEW = VIEW.replace("} else if (!suggestions.isEmpty() && event.getY() > getHeight()",
                    "} else if (!suggestionBand.words().isEmpty()"
                    " && event.getY() > getHeight()")

# --- insertSuggestion : le bandeau porte les abscisses
swap("""        for (int at = 0; at < suggestionXs.length; at++) {
            float next = at + 1 < suggestionXs.length
                    ? suggestionXs[at + 1] : getWidth();
            if (x >= suggestionXs[at] && x < next) {
                applyEdit(suggestionWordStart, Math.min(caretOffset, buffer.length()),
                        suggestions.get(at));
""", """        for (int at = 0; at < suggestionBand.drawnCount(); at++) {
            float next = at + 1 < suggestionBand.drawnCount()
                    ? suggestionBand.xAt(at + 1) : getWidth();
            if (x >= suggestionBand.xAt(at) && x < next) {
                applyEdit(suggestionWordStart, Math.min(caretOffset, buffer.length()),
                        suggestionBand.words().get(at));
""")

# --- drawInfoPopup : la géométrie part, le dessin délègue
swap("""    private void drawInfoPopup(Canvas canvas) {
        if (infoPopupText == null) {
            return;
        }
        float textWidth = infoPaint.measureText(infoPopupText);
        float padding = dp(8);
        float boxWidth = Math.min(textWidth + 2 * padding,
                getWidth() - getPaddingLeft() - getPaddingRight());
        float boxHeight = -infoPaint.getFontMetrics().top + dp(2) + padding;
        float anchorTop = getPaddingTop() + visibleIndexOf(infoPopupLine) * lineHeight
                - scrollY;
        float left = Math.min(
                Math.max(getPaddingLeft(),
                        getPaddingLeft() + infoPopupColumn * charWidth - padding),
                getWidth() - getPaddingRight() - boxWidth);
        float top;
        if (anchorTop - boxHeight - dp(4) >= getPaddingTop()) {
            top = anchorTop - boxHeight - dp(4);
        } else {
            top = Math.min(anchorTop + lineHeight + dp(4),
                    getHeight() - boxHeight);
        }
        canvas.drawRoundRect(left, top, left + boxWidth, top + boxHeight,
                dp(4), dp(4), infoBackground);
        canvas.drawRoundRect(left, top, left + boxWidth, top + boxHeight,
                dp(4), dp(4), infoBorder);
        // le texte coupé à la place disponible (les mots longs se
        // tronquent, la popup reste dans la vue)
        float maxChars = Math.max(1.0f, (boxWidth - 2 * padding) / charWidth);
        String shown = infoPopupText.length() > maxChars
                ? infoPopupText.substring(0, (int) maxChars) : infoPopupText;
        canvas.drawText(shown, left + padding,
                top - infoPaint.getFontMetrics().top, infoPaint);
    }
""", """    private void drawInfoPopup(Canvas canvas) {
        if (infoPopup.text() == null) {
            return;
        }
        float anchorTop = getPaddingTop()
                + visibleIndexOf(infoPopup.line()) * lineHeight - scrollY;
        infoPopup.draw(canvas, anchorTop, popupMetrics());
    }

    /**
     * La géométrie de la vue au dessin des popups : le paquet des
     * popups n'en connaît que ces nombres.
     */
    private PopupMetrics popupMetrics() {
        return new PopupMetrics(getPaddingLeft(), getPaddingTop(),
                getPaddingRight(), getPaddingBottom(), getWidth(), getHeight(),
                lineHeight, charWidth, dp(1));
    }
""")

# --- drawSuggestions : le bandeau dessine
swap("""    private void drawSuggestions(Canvas canvas) {
        if (suggestions.isEmpty()) {
            suggestionXs = new float[0];
            return;
        }
        float height = dp(28);
        float top = getHeight() - height - getPaddingBottom();
        if (top < 0.0f) {
            suggestionXs = new float[0];
            return;
        }
        canvas.drawRect(0.0f, top, getWidth(), top + height, suggestionBackground);
        float x = getPaddingLeft();
        float limit = getWidth() - getPaddingRight();
        float[] xs = new float[suggestions.size()];
        int drawn = 0;
        for (String candidate : suggestions) {
            float width = suggestionPaint.measureText(candidate) + dp(16);
            if (x + width > limit) {
                break;
            }
            xs[drawn] = x;
            canvas.drawText(candidate, x + dp(8),
                    top + height / 2.0f + dp(4), suggestionPaint);
            x += width;
            drawn++;
        }
        suggestionXs = drawn == xs.length ? xs : java.util.Arrays.copyOf(xs, drawn);
    }
""", """    private void drawSuggestions(Canvas canvas) {
        suggestionBand.draw(canvas, popupMetrics());
    }
""")
VIEW = VIEW.replace("        return suggestions;\n    }",
                    "        return suggestionBand.words();\n    }")

EDITOR.write_text(VIEW, encoding="utf-8")
print("EditorView recâblé :", len(VIEW.splitlines()), "lignes")

# --- RenderCache vers le paquet render/
render = Path("editor/src/main/java/jo/pigma/editor/render")
render.mkdir(parents=True, exist_ok=True)
subprocess.run(["git", "mv", "editor/src/main/java/jo/pigma/editor/RenderCache.java",
                str(render / "RenderCache.java")], check=True)
cache = (render / "RenderCache.java")
text = cache.read_text(encoding="utf-8")
text = text.replace("package jo.pigma.editor;",
                    "package jo.pigma.editor.render;")
cache.write_text(text, encoding="utf-8")
view = EDITOR.read_text(encoding="utf-8")
view = view.replace("import jo.pigma.editor.popups.InfoPopup;",
                    "import jo.pigma.editor.popups.InfoPopup;\n"
                    "import jo.pigma.editor.render.RenderCache;")
EDITOR.write_text(view, encoding="utf-8")
test = Path("editor/src/test/java/jo/pigma/editor/RenderCacheTest.java")
if test.is_file():
    body = test.read_text(encoding="utf-8")
    body = body.replace("import jo.pigma.editor.RenderCache;",
                        "import jo.pigma.editor.render.RenderCache;")
    test.write_text(body, encoding="utf-8")
    print("RenderCacheTest réimporté")
print("RenderCache → render/")
