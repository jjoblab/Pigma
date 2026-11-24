package jo.pigma.sample;

import java.util.Map;

import jo.pigma.syntax.SyntaxTheme;

/**
 * Les deux palettes du sample : claire et sombre. Le thème ne connaît
 * que des portées ; le fond est porté à côté (il appartient à la vue,
 * pas au moteur).
 */
public final class SampleThemes {

    /** Fond du thème clair. */
    public static final int LIGHT_BACKGROUND = 0xFFFDFDFD;
    /** Fond du thème sombre. */
    public static final int DARK_BACKGROUND = 0xFF1E1E22;

    /** Palette claire : texte sombre sur fond clair. */
    public static final SyntaxTheme LIGHT = SyntaxTheme.of(Map.ofEntries(
            Map.entry("keyword", 0xFF0033B3),
            Map.entry("keyword.control", 0xFFAF00DB),
            Map.entry("comment", 0xFF008000),
            Map.entry("comment.doc", 0xFF008000),
            Map.entry("string", 0xFFA31515),
            Map.entry("string.escape", 0xFFA31515),
            Map.entry("number", 0xFF098658),
            Map.entry("operator", 0xFF1B1B1F),
            Map.entry("punct", 0xFF1B1B1F),
            Map.entry("tag", 0xFF800000),
            Map.entry("entity", 0xFFA31515),
            Map.entry("key", 0xFF0451A5)), 0xFF1B1B1F);

    /** Palette sombre : texte clair sur fond sombre. */
    public static final SyntaxTheme DARK = SyntaxTheme.of(Map.ofEntries(
            Map.entry("keyword", 0xFF569CD6),
            Map.entry("keyword.control", 0xFFC586C0),
            Map.entry("comment", 0xFF6A9955),
            Map.entry("comment.doc", 0xFF6A9955),
            Map.entry("string", 0xFFCE9178),
            Map.entry("string.escape", 0xFFD7BA7D),
            Map.entry("number", 0xFFB5CEA8),
            Map.entry("operator", 0xFFD4D4D4),
            Map.entry("punct", 0xFF808080),
            Map.entry("tag", 0xFF569CD6),
            Map.entry("entity", 0xFFD7BA7D),
            Map.entry("key", 0xFF9CDCFE)), 0xFFD4D4D4);

    /** Natures sémantiques (L3) de la palette claire. */
    private static final Map<String, Integer> LIGHT_SEMANTIC = Map.of(
            "type", 0xFF267F99, "méthode", 0xFF795E26, "champ", 0xFF001080);

    /** Natures sémantiques (L3) de la palette sombre. */
    private static final Map<String, Integer> DARK_SEMANTIC = Map.of(
            "type", 0xFF4EC9B0, "méthode", 0xFFDCDCAA, "champ", 0xFF9CDCFE);

    /**
     * La couleur d'une nature sémantique (la fusion L3 au dessin s'en
     * sert) : la palette du thème donné (les deux du sample par
     * identité, un thème tiers retombe sur la claire) ; une nature
     * inconnue retombe sur le neutre du thème — la chaîne ouverte ne
     * casse jamais le rendu.
     *
     * @param theme  le thème courant
     * @param nature la nature de l'annotation
     * @return la couleur ARGB
     */
    public static int semanticColor(SyntaxTheme theme, String nature) {
        Map<String, Integer> palette = theme == DARK ? DARK_SEMANTIC : LIGHT_SEMANTIC;
        Integer color = palette.get(nature);
        return color == null ? theme.defaultColor() : color;
    }

    private SampleThemes() {
    }
}
