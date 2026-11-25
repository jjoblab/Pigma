package jo.pigma.editor;

import java.util.Map;

import jo.pigma.syntax.SyntaxTheme;

/**
 * Les six palettes de la bibliothèque : claire, sombre, solaire,
 * forêt, océan, café (un hôte peut apporter la sienne). Le thème ne
 * connaît que des portées ; le fond est porté à côté (il appartient à
 * la vue, pas au moteur).
 */
public final class EditorThemes {

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

    /** Fond du thème solaire (papier chaud). */
    public static final int SOLAR_BACKGROUND = 0xFFFDF6E3;

    /** Fond du thème forêt (vert profond). */
    public static final int FOREST_BACKGROUND = 0xFF10281E;

    /** Fond du thème océan (bleu nuit). */
    public static final int OCEAN_BACKGROUND = 0xFF0F1B2D;

    /** Fond du thème café (brun chaud). */
    public static final int COFFEE_BACKGROUND = 0xFF2B2118;

    /** Palette solaire : l'encre chaude sur papier crème. */
    public static final SyntaxTheme SOLAR = SyntaxTheme.of(Map.ofEntries(
            Map.entry("keyword", 0xFF859900),
            Map.entry("keyword.control", 0xFFCB4B16),
            Map.entry("comment", 0xFF93A1A1),
            Map.entry("comment.doc", 0xFF93A1A1),
            Map.entry("string", 0xFF2AA198),
            Map.entry("string.escape", 0xFFDC322F),
            Map.entry("number", 0xFFD33682),
            Map.entry("operator", 0xFF586E75),
            Map.entry("punct", 0xFF586E75),
            Map.entry("tag", 0xFF268BD2),
            Map.entry("entity", 0xFFB58900),
            Map.entry("key", 0xFF268BD2)), 0xFF586E75);

    /** Palette forêt : les teintes vives sur vert profond. */
    public static final SyntaxTheme FOREST = SyntaxTheme.of(Map.ofEntries(
            Map.entry("keyword", 0xFF7FDA66),
            Map.entry("keyword.control", 0xFFE6DB74),
            Map.entry("comment", 0xFF75715E),
            Map.entry("comment.doc", 0xFF75715E),
            Map.entry("string", 0xFFE6DB74),
            Map.entry("string.escape", 0xFFFFEE99),
            Map.entry("number", 0xFFAE81FF),
            Map.entry("operator", 0xFFF8F8F2),
            Map.entry("punct", 0xFFA5C261),
            Map.entry("tag", 0xFF66D9EF),
            Map.entry("entity", 0xFFFD971F),
            Map.entry("key", 0xFF66D9EF)), 0xFFF8F8F2);

    /** Palette océan : les tons froids calmes sur bleu nuit. */
    public static final SyntaxTheme OCEAN = SyntaxTheme.of(Map.ofEntries(
            Map.entry("keyword", 0xFF82AAFF),
            Map.entry("keyword.control", 0xFFC792EA),
            Map.entry("comment", 0xFF546E7A),
            Map.entry("comment.doc", 0xFF546E7A),
            Map.entry("string", 0xFFC3E88D),
            Map.entry("string.escape", 0xFF89DDFF),
            Map.entry("number", 0xFFF78C6C),
            Map.entry("operator", 0xFFB2CCD6),
            Map.entry("punct", 0xFF89AA93),
            Map.entry("tag", 0xFF82AAFF),
            Map.entry("entity", 0xFFFFCB6B),
            Map.entry("key", 0xFF80CBC4)), 0xFFB2CCD6);

    /** Palette café : les bruns chauds sur brun profond. */
    public static final SyntaxTheme COFFEE = SyntaxTheme.of(Map.ofEntries(
            Map.entry("keyword", 0xFFD19A66),
            Map.entry("keyword.control", 0xFFE78A4E),
            Map.entry("comment", 0xFF7F6A56),
            Map.entry("comment.doc", 0xFF7F6A56),
            Map.entry("string", 0xFFB8A37E),
            Map.entry("string.escape", 0xFFD7B98E),
            Map.entry("number", 0xFFCA7E5A),
            Map.entry("operator", 0xFFC8BEB4),
            Map.entry("punct", 0xFFA08E7E),
            Map.entry("tag", 0xFFD9A0A0),
            Map.entry("entity", 0xFFE8C39E),
            Map.entry("key", 0xFFC8A887)), 0xFFC8BEB4);

    /** Natures sémantiques (L3) de la palette claire. */
    private static final Map<String, Integer> LIGHT_SEMANTIC = Map.of(
            "type", 0xFF267F99, "méthode", 0xFF795E26, "champ", 0xFF001080,
            "constante", 0xFF875A7B);

    /** Natures sémantiques (L3) de la palette sombre. */
    private static final Map<String, Integer> DARK_SEMANTIC = Map.of(
            "type", 0xFF4EC9B0, "méthode", 0xFFDCDCAA, "champ", 0xFF9CDCFE,
            "constante", 0xFFC586C0);

    /** Natures sémantiques (L3) de la palette solaire. */
    private static final Map<String, Integer> SOLAR_SEMANTIC = Map.of(
            "type", 0xFFB58900, "méthode", 0xFFCB4B16, "champ", 0xFF268BD2,
            "constante", 0xFFD33682);

    /** Natures sémantiques (L3) de la palette forêt. */
    private static final Map<String, Integer> FOREST_SEMANTIC = Map.of(
            "type", 0xFF66D9EF, "méthode", 0xFFFD971F, "champ", 0xFFA5C261,
            "constante", 0xFFAE81FF);

    /** Natures sémantiques (L3) de la palette océan. */
    private static final Map<String, Integer> OCEAN_SEMANTIC = Map.of(
            "type", 0xFF80CBC4, "méthode", 0xFFFFCB6B, "champ", 0xFF89DDFF,
            "constante", 0xFFF78C6C);

    /** Natures sémantiques (L3) de la palette café. */
    private static final Map<String, Integer> COFFEE_SEMANTIC = Map.of(
            "type", 0xFFE8C39E, "méthode", 0xFFE78A4E, "champ", 0xFFC8A887,
            "constante", 0xFFD19A66);

    /** Les natures sémantiques par thème livré (le registre). */
    private static final Map<SyntaxTheme, Map<String, Integer>> SEMANTIC =
            Map.of(LIGHT, LIGHT_SEMANTIC, DARK, DARK_SEMANTIC,
                    SOLAR, SOLAR_SEMANTIC, FOREST, FOREST_SEMANTIC,
                    OCEAN, OCEAN_SEMANTIC, COFFEE, COFFEE_SEMANTIC);

    /** Le genre d'un jeton du serveur à sa nature de palette (la fusion L4). */
    private static final Map<String, String> SERVER_TOKEN_NATURES = Map.ofEntries(
            Map.entry("function", "méthode"),
            Map.entry("method", "méthode"),
            Map.entry("variable", "champ"),
            Map.entry("property", "champ"),
            Map.entry("parameter", "champ"),
            Map.entry("field", "champ"),
            Map.entry("constant", "constante"),
            Map.entry("enumMember", "constante"),
            Map.entry("class", "type"),
            Map.entry("type", "type"),
            Map.entry("interface", "type"),
            Map.entry("struct", "type"),
            Map.entry("enum", "type"),
            Map.entry("namespace", "type"));

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
        Map<String, Integer> palette = SEMANTIC.get(theme);
        Integer color = palette == null ? null : palette.get(nature);
        return color == null ? theme.defaultColor() : color;
    }

    /**
     * La nature de palette d'un genre de jeton du serveur (la fusion
     * L4 s'en sert) : « function » y vaut la méthode, « constant » la
     * constante, « class » le type… ; un genre inconnu rend null —
     * la couleur d'avant demeure (la chaîne ouverte ne casse jamais
     * le rendu, le lexer garde son mot).
     *
     * @param type le genre du jeton (le nom de la légende du serveur)
     * @return la nature de palette, null pour un genre inconnu
     */
    public static String serverTokenNature(String type) {
        return type == null ? null : SERVER_TOKEN_NATURES.get(type);
    }

    private EditorThemes() {
    }
}
