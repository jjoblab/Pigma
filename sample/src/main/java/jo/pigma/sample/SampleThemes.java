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

    private SampleThemes() {
    }
}
