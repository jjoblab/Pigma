package jo.pigma.sample;

/**
 * Textes de démonstration par langage : chacun exerce les formes de
 * jeton de son lexeur (mots, chaînes, nombres, commentaires, balises,
 * clés) et reste assez court pour lire la coloration d'un coup d'œil.
 * Le gros fichier, généré, sert la fenêtre paresseuse : seule la
 * partie visible se paie à l'ouverture.
 */
public final class DemoTexts {

    /** Démonstration Java : mots-clés, javadoc, bloc, chaînes, nombres. */
    public static final String JAVA = """
            package exemple;

            /**
             * Calcule le total pondéré d'un lot.
             */
            public final class Lot {
                // constantes de pondération
                private static final double SEUIL = 0.125e-3;
                private static final int MAX = 0x1F_FF;

                static double total(int graine, int taille) {
                    if (graine > MAX && taille != -1) {
                        return graine * taille * SEUIL;
                    }
                    return (double) graine / taille;
                }
            }
            """;

    /** Démonstration XML : commentaire, CDATA, entités, attributs. */
    public static final String XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!-- configuration de démonstration -->
            <config id="demo" version="2">
                <owner name="Pigma" role="editor"/>
                <limites min="0" max="1000000" ratio="0.5"/>
                <entites code="&#169;" hex="&#x263A;"/>
                <mesures consolidation="vrai"
                         unite="ko">
                    12, 34, 56
                </mesures>
                <brut><![CDATA[donnée brute <non> interprétée]]></brut>
            </config>
            """;

    /** Démonstration JSON : clés, chaînes, nombres, littéraux. */
    public static final String JSON = """
            {
              "fichier": "démonstration du moteur",
              "version": 3,
              "actif": true,
              "cache": null,
              "nombres": [0, 42, -7, 3.5, 2.5e-8],
              "textes": ["simple", "échappé \\" et \\\\", "unicode \\u263A"],
              "imbrication": {
                "cle": "valeur",
                "liste": [1, 2, 3]
              }
            }
            """;

    /** Démonstration SQL : mots-clés sans casse, commentaires, chaîne. */
    public static final String SQL = """
            -- requête de démonstration (famille générique)
            SELECT nom, age, poids
            FROM usagers
            WHERE age BETWEEN 18 AND 99
              AND actif = true
            ORDER BY nom DESC;

            /* un commentaire
               de plusieurs lignes */
            SELECT COUNT(*) AS total FROM 'commandes';
            """;

    /**
     * Démonstration Kotlin : gabarits de chaîne (le traversant
     * multi-lignes compris, pile de contextes à l'appui), chaîne brute,
     * commentaire imbriqué, formes de nombres.
     */
    public static final String KOTLIN = """
            package exemple

            data class Lot(val graine: Int, val taille: Int = 1) {
                /* imbriqué : /* interne */ la suite */
                val poids = if (graine > 0x1F_FF) graine * 0.125e-3 else 0.5f

                fun total(marge: Double = 0.0) = (poids - marge).coerceAtLeast(0.0)

                fun template(n: Int): String {
                    val simple = "graine $graine sur $taille"
                    val imbrique = "total ${total()} pour ${"n = $n"} chars"
                    return "$simple ; $imbrique"
                }

                fun brut(n: Int): String = \"\"\"
                    Lot(graine=$graine) {
                        template = ${template(n).length} chars
                        ${if (n in 1..9) "petit" else "grand"}
                    }
                \"\"\".trimIndent()
            }

            fun main() {
                val lot = Lot(graine = 12)
                println(lot.template(3))
                println(lot.brut(3))
            }
            """;


    /**
     * Gros fichier Java de démonstration, généré : environ deux mille
     * lignes, une ligne monstre au milieu (hors plafond moteur de
     * coloration, dessin coupé) — la fenêtre visible seule se paie à
     * l'ouverture, le remplissage suit par tranches.
     *
     * @return le texte du gros fichier, jamais {@code null}
     */
    public static String bigFile() {
        StringBuilder text = new StringBuilder();
        text.append("// Gros fichier de demonstration : seule la fenetre visible se paie a l'ouverture.\n");
        for (int block = 0; block < 111; block++) {
            text.append("/* Bloc ").append(block)
                    .append(" : le remplissage suit en arriere-plan, par tranches. */\n");
            text.append("package bloc").append(block).append(";\n\n");
            text.append("/** Calculs du bloc ").append(block).append(". */\n");
            text.append("public final class Bloc").append(block).append(" {\n");
            text.append("    private static final double SEUIL = 0.125e-3;\n");
            text.append("    private static final int MAX = 0x1F_FF;\n\n");
            text.append("    static double total(int graine, int taille) {\n");
            text.append("        if (graine > MAX && taille != -1) {\n");
            text.append("            return graine * taille * SEUIL;\n");
            text.append("        }\n");
            text.append("        return (double) graine / taille;\n");
            text.append("    }\n");
            text.append("}\n\n");
            if (block == 2) {
                text.append("// ligne monstre : non coloree au-dela du plafond moteur, dessin coupe\n");
                text.append("// ").append("x".repeat(25_000)).append('\n');
            }
        }
        return text.toString();
    }

    /**
     * Le fichier hors normes du plan : 52 000 lignes, plus de 2,5
     * millions de caractères — le mode gros fichier s'y active (L1 y
     * reste, la fenêtre se paie, le fond remplit par tranches). Le
     * texte se génère au premier appel puis se cache : le bouton ne
     * paie la construction qu'une fois.
     *
     * @return le texte de 52 000 lignes environ
     */
    public static synchronized String hugeFile() {
        return hugeFile("JAVA");
    }

    /**
     * Le fichier hors normes pour chaque langage (la consolidation) :
     * le bloc propre au langage répété jusqu'à 52 000 lignes — la
     * fenêtre, les tranches et le mode gros fichier s'y éprouvent sur
     * chacun. Chaque texte se génère au premier appel puis se cache.
     *
     * @param language la clef du langage : "JAVA", "XML", "JSON",
     *                 "KOTLIN" ou "SQL"
     * @return le texte de 52 000 lignes environ
     */
    public static synchronized String hugeFile(String language) {
        String cached = hugeFileCaches.get(language);
        if (cached != null) {
            return cached;
        }
        String block;
        String header;
        switch (language) {
            case "XML":
                header = "<!-- Fichier hors normes : 52 000 lignes, mode gros fichier. -->\n\n";
                block = "<!-- Bloc 0. -->\n"
                        + "<bloc numero=\"0\">\n"
                        + "  <seuil unite=\"e-3\">0.125</seuil>\n"
                        + "  <max>8191</max>\n"
                        + "  <total graine=\"12\" taille=\"24\">\n"
                        + "    <calcule>graine * taille * seuil</calcule>\n"
                        + "  </total>\n"
                        + "</bloc>\n\n";
                break;
            case "JSON":
                header = "[\n";
                block = "{\n"
                        + "  \"bloc\": 0,\n"
                        + "  \"seuil\": 0.125e-3,\n"
                        + "  \"max\": 8191,\n"
                        + "  \"graine\": 12,\n"
                        + "  \"taille\": 24,\n"
                        + "  \"calcule\": \"graine * taille * seuil\",\n"
                        + "  \"reparti\": [1, 2, 3, 5, 8],\n"
                        + "  \"actif\": true\n"
                        + "},\n";
                break;
            case "KOTLIN":
                header = "// Fichier hors normes : 52 000 lignes, mode gros fichier.\n\n";
                block = "/* Bloc 0. */\n"
                        + "class G0 {\n"
                        + "    private val seuil = 0.125e-3\n\n"
                        + "    fun total(graine: Int, taille: Int): Double {\n"
                        + "        if (graine > 8191 && taille != -1) {\n"
                        + "            return graine * taille * seuil\n"
                        + "        }\n"
                        + "        return graine.toDouble() / taille\n"
                        + "    }\n"
                        + "}\n\n";
                break;
            case "SQL":
                header = "-- Fichier hors normes : 52 000 lignes, mode gros fichier.\n\n";
                block = "-- Bloc 0.\n"
                        + "CREATE TABLE bloc_0 (\n"
                        + "    id INTEGER PRIMARY KEY,\n"
                        + "    seuil REAL DEFAULT 0.125e-3,\n"
                        + "    graine INTEGER NOT NULL,\n"
                        + "    taille INTEGER NOT NULL,\n"
                        + "    calcule TEXT DEFAULT 'graine * taille'\n"
                        + ");\n\n"
                        + "INSERT INTO bloc_0 (graine, taille) VALUES (12, 24);\n\n";
                break;
            default:
                header = "// Fichier hors normes de demonstration : 52 000 lignes, mode gros fichier.\n"
                        + "// La fenetre visible se paie, le remplissage suit en arriere-plan.\n\n";
                block = "/* Bloc 0. */\n"
                        + "public final class G0 {\n"
                        + "    private static final double SEUIL = 0.125e-3;\n"
                        + "    private static final int MAX = 0x1F_FF;\n\n"
                        + "    static double total(int graine, int taille) {\n"
                        + "        if (graine > MAX && taille != -1) {\n"
                        + "            return graine * taille * SEUIL;\n"
                        + "        }\n"
                        + "        return (double) graine / taille;\n"
                        + "    }\n"
                        + "}\n\n";
                break;
        }
        int blockLines = countNewlines(block);
        int headerLines = countNewlines(header);
        int blocks = (52_000 - headerLines + blockLines - 1) / blockLines;
        StringBuilder text = new StringBuilder(3_000_000);
        text.append(header);
        for (int at = 0; at < blocks; at++) {
            text.append(block.replace("Bloc 0", "Bloc " + at)
                    .replace("G0", "G" + at)
                    .replace("bloc_0", "bloc_" + at)
                    .replace("numero=\"0\"", "numero=\"" + at + "\"")
                    .replace("\"bloc\": 0", "\"bloc\": " + at));
        }
        if ("JSON".equals(language)) {
            text.append("]\n");
        }
        String built = text.toString();
        hugeFileCaches.put(language, built);
        return built;
    }

    private static int countNewlines(String text) {
        int count = 0;
        for (int at = 0; at < text.length(); at++) {
            if (text.charAt(at) == '\n') {
                count++;
            }
        }
        return count;
    }

    private static final java.util.Map<String, String> hugeFileCaches =
            new java.util.HashMap<>();

    private DemoTexts() {
    }
}
