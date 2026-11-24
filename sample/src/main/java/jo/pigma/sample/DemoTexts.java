package jo.pigma.sample;

/**
 * Textes de démonstration par langage : chacun exerce les formes de
 * jeton de son lexeur (mots, chaînes, nombres, commentaires, balises,
 * clés) et reste assez court pour lire la coloration d'un coup d'œil.
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

    private DemoTexts() {
    }
}
