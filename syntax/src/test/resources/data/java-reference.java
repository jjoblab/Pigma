package bench;

import java.util.List;
import java.util.Map;

/**
 * Fichier de reference du lexeur Java du banc E0 : chaque forme de
 * jeton y parait au moins une fois, la sortie figee fait foi.
 */
public final class Reference {

    private static final int MAX = 0x1F_FF;
    private static final long TOTAL = 123_456_789L;
    private static final double RATIO = 0.125e-3;
    private static final char SURROGATE = '\u263A';

    /**
     * Additionne deux entiers.
     *
     * @param a premier operande
     * @param b second operande
     * @return la somme des deux operandes
     */
    public static int add(int a, int b) {
        // commentaire de fin de ligne
        /* commentaire de bloc */
        int sum = a + b;
        if (sum >= MAX && a > 0 || b != -1) {
            return sum % 0b1010_1010;
        }
        return (int) (sum * 2.0f / 3.0);
    }

    static void greet(String who) {
        String message = "Bonjour «" + who + "\t» \uD83D\uDE00 \\path\n";
        char quote = '\'';
        char newline = '\n';
        System.out.printf("%s: %c%n", message, quote);
    }

    static void catalogue(List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            Object value = row.getOrDefault("cle", null);
            assert value != null : "valeur absente";
            int index = 0;
            index++;
            index--;
            index += 10;
            index <<= 2;
            index >>>= 1;
            double d = 1.0;
            float f = .5f;
            long hex = 0xC0FEL;
            long l = TOTAL;
            boolean ok = value instanceof String s && !s.isBlank();
            System.out.println(index + " " + d + " " + f + " " + hex + " " + l + " " + ok);
        }
    }
}
