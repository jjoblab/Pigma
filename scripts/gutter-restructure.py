#!/usr/bin/env python3
"""Chantier 47 : la gouttière (numéros de ligne) au paquet gutter/.
La vue élargit sa marge gauche, dessine les numéros sous tout le reste
et teinte le gris au thème."""
from pathlib import Path

EDITOR = Path("editor/src/main/java/jo/pigma/editor/EditorView.java")
VIEW = EDITOR.read_text(encoding="utf-8")


def swap(old, new):
    global VIEW
    assert VIEW.count(old) == 1, f"introuvable ou multiple : {old[:70]!r} ({VIEW.count(old)})"
    VIEW = VIEW.replace(old, new)


swap("import jo.pigma.editor.popups.InfoPopup;",
     "import jo.pigma.editor.gutter.Gutter;\n"
     "import jo.pigma.editor.popups.InfoPopup;")

# le champ vit près des autres paquets de fonctionnalité
swap("""    /** Le bandeau de complétion — état et dessin au paquet des popups. */
    private SuggestionBand suggestionBand;
""", """    /** Le bandeau de complétion — état et dessin au paquet des popups. */
    private SuggestionBand suggestionBand;

    /** La gouttière — les numéros de ligne, au paquet du même nom. */
    private Gutter gutter;
""")

# la construction, au chaud auprès du bandeau (dp(1) y est sûr)
swap("        suggestionBand = new SuggestionBand(dp(1));\n",
     "        suggestionBand = new SuggestionBand(dp(1));\n"
     "        gutter = new Gutter(dp(1));\n")

# la marge gauche accueille les numéros (44 dp : ~26 dp de chiffres,
# 3 dp de souffle, 9 dp pour le marqueur de pli)
swap("""        int padding = dp(12);
        setPadding(padding, dp(16), padding, dp(16));
""", """        int padding = dp(12);
        setPadding(dp(44), dp(16), padding, dp(16));
""")

# le dessin : les numéros sous tout le reste (la gouttière se lit
# d'abord, tout se dessine dessus)
swap("""        drawScopeBand(canvas, first, last, count);
""", """        drawGutter(canvas, first, last, count);
        drawScopeBand(canvas, first, last, count);
""")

# la méthode de dessin, après la popup d'info (elle-même suivie du
# patron de métriques)
swap("""    /**
     * Dessine la structure de L2 par-dessus les plans : les guides
""", """    /**
     * La gouttière : le numéro de chaque ligne visible, aligné à
     * droite avant la zone du marqueur de pli — le pinceau vit au
     * paquet, la vue ne fournit que la géométrie (la baseline, le
     * bord droit du champ des numéros).
     */
    private void drawGutter(Canvas canvas, int first, int last, int count) {
        for (int line = first; line <= last && line < count; line++) {
            if (isHidden(line)) {
                continue;
            }
            gutter.drawNumber(canvas, line, lineBaseline(line),
                    getPaddingLeft() - dp(11));
        }
    }

    /**
     * Dessine la structure de L2 par-dessus les plans : les guides
""")

# le gris suit le thème (la luminance du fond décide)
swap("""    public void applyTheme(SyntaxTheme newTheme, int newBackground) {
""", """    public void applyTheme(SyntaxTheme newTheme, int newBackground) {
        int luminance = ((newBackground >>> 16) & 0xFF) * 30
                + ((newBackground >>> 8) & 0xFF) * 59
                + (newBackground & 0xFF) * 11;
        gutter.setTextColor(luminance >= 12800 ? 0xFF8A8A8E : 0xFF8E8E93);
""")

EDITOR.write_text(VIEW, encoding="utf-8")
print("gouttière branchée :", len(VIEW.splitlines()), "lignes")
