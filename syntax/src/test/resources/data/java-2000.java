package bench;

import java.util.List;
import java.util.Map;

/**
 * Corpus de 2000 lignes du banc E0 : motifs repetitifs, jetons varies.
 */
public final class Corpus {

    private static final int MAX = 1000000;
    private static final double STEP = 0.125e-3;

    /**
     * Calcule le poids du lot 0 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight0(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 1f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0000L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 2) / 2;
        }
        String label = "lot-0\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 1 ? 1 : total;
    }
    /**
     * Calcule le poids du lot 1 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight1(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 2f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0025L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 3) / 3;
        }
        String label = "lot-1\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 2 ? 2 : total;
    }
    /**
     * Calcule le poids du lot 2 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight2(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 3f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x004AL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 4) / 4;
        }
        String label = "lot-2\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 3 ? 3 : total;
    }
    /**
     * Calcule le poids du lot 3 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight3(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 4f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x006FL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 5) / 5;
        }
        String label = "lot-3\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 4 ? 4 : total;
    }
    /**
     * Calcule le poids du lot 4 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight4(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 5f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0094L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 6) / 6;
        }
        String label = "lot-4\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 5 ? 5 : total;
    }
    /**
     * Calcule le poids du lot 5 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight5(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 6f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x00B9L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 7) / 2;
        }
        String label = "lot-5\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 6 ? 6 : total;
    }
    /**
     * Calcule le poids du lot 6 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight6(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 7f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x00DEL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 8) / 3;
        }
        String label = "lot-6\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 7 ? 7 : total;
    }
    /**
     * Calcule le poids du lot 7 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight7(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 1f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0103L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 9) / 4;
        }
        String label = "lot-7\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 8 ? 8 : total;
    }
    /**
     * Calcule le poids du lot 8 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight8(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 2f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0128L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 10) / 5;
        }
        String label = "lot-8\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 9 ? 9 : total;
    }
    /**
     * Calcule le poids du lot 9 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight9(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 3f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x014DL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 11) / 6;
        }
        String label = "lot-9\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 10 ? 10 : total;
    }
    /**
     * Calcule le poids du lot 10 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight10(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 4f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0172L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 12) / 2;
        }
        String label = "lot-10\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 11 ? 11 : total;
    }
    /**
     * Calcule le poids du lot 11 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight11(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 5f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0197L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 13) / 3;
        }
        String label = "lot-11\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 1 ? 1 : total;
    }
    /**
     * Calcule le poids du lot 12 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight12(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 6f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x01BCL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 14) / 4;
        }
        String label = "lot-12\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 2 ? 2 : total;
    }
    /**
     * Calcule le poids du lot 13 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight13(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 7f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x01E1L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 2) / 5;
        }
        String label = "lot-13\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 3 ? 3 : total;
    }
    /**
     * Calcule le poids du lot 14 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight14(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 1f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0206L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 3) / 6;
        }
        String label = "lot-14\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 4 ? 4 : total;
    }
    /**
     * Calcule le poids du lot 15 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight15(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 2f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x022BL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 4) / 2;
        }
        String label = "lot-15\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 5 ? 5 : total;
    }
    /**
     * Calcule le poids du lot 16 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight16(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 3f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0250L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 5) / 3;
        }
        String label = "lot-16\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 6 ? 6 : total;
    }
    /**
     * Calcule le poids du lot 17 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight17(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 4f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0275L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 6) / 4;
        }
        String label = "lot-17\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 7 ? 7 : total;
    }
    /**
     * Calcule le poids du lot 18 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight18(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 5f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x029AL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 7) / 5;
        }
        String label = "lot-18\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 8 ? 8 : total;
    }
    /**
     * Calcule le poids du lot 19 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight19(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 6f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x02BFL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 8) / 6;
        }
        String label = "lot-19\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 9 ? 9 : total;
    }
    /**
     * Calcule le poids du lot 20 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight20(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 7f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x02E4L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 9) / 2;
        }
        String label = "lot-20\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 10 ? 10 : total;
    }
    /**
     * Calcule le poids du lot 21 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight21(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 1f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0309L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 10) / 3;
        }
        String label = "lot-21\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 11 ? 11 : total;
    }
    /**
     * Calcule le poids du lot 22 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight22(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 2f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x032EL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 11) / 4;
        }
        String label = "lot-22\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 1 ? 1 : total;
    }
    /**
     * Calcule le poids du lot 23 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight23(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 3f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0353L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 12) / 5;
        }
        String label = "lot-23\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 2 ? 2 : total;
    }
    /**
     * Calcule le poids du lot 24 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight24(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 4f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0378L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 13) / 6;
        }
        String label = "lot-24\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 3 ? 3 : total;
    }
    /**
     * Calcule le poids du lot 25 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight25(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 5f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x039DL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 14) / 2;
        }
        String label = "lot-25\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 4 ? 4 : total;
    }
    /**
     * Calcule le poids du lot 26 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight26(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 6f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x03C2L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 2) / 3;
        }
        String label = "lot-26\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 5 ? 5 : total;
    }
    /**
     * Calcule le poids du lot 27 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight27(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 7f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x03E7L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 3) / 4;
        }
        String label = "lot-27\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 6 ? 6 : total;
    }
    /**
     * Calcule le poids du lot 28 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight28(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 1f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x040CL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 4) / 5;
        }
        String label = "lot-28\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 7 ? 7 : total;
    }
    /**
     * Calcule le poids du lot 29 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight29(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 2f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0431L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 5) / 6;
        }
        String label = "lot-29\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 8 ? 8 : total;
    }
    /**
     * Calcule le poids du lot 30 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight30(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 3f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0456L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 6) / 2;
        }
        String label = "lot-30\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 9 ? 9 : total;
    }
    /**
     * Calcule le poids du lot 31 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight31(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 4f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x047BL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 7) / 3;
        }
        String label = "lot-31\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 10 ? 10 : total;
    }
    /**
     * Calcule le poids du lot 32 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight32(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 5f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x04A0L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 8) / 4;
        }
        String label = "lot-32\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 11 ? 11 : total;
    }
    /**
     * Calcule le poids du lot 33 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight33(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 6f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x04C5L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 9) / 5;
        }
        String label = "lot-33\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 1 ? 1 : total;
    }
    /**
     * Calcule le poids du lot 34 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight34(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 7f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x04EAL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 10) / 6;
        }
        String label = "lot-34\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 2 ? 2 : total;
    }
    /**
     * Calcule le poids du lot 35 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight35(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 1f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x050FL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 11) / 2;
        }
        String label = "lot-35\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 3 ? 3 : total;
    }
    /**
     * Calcule le poids du lot 36 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight36(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 2f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0534L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 12) / 3;
        }
        String label = "lot-36\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 4 ? 4 : total;
    }
    /**
     * Calcule le poids du lot 37 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight37(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 3f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0559L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 13) / 4;
        }
        String label = "lot-37\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 5 ? 5 : total;
    }
    /**
     * Calcule le poids du lot 38 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight38(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 4f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x057EL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 14) / 5;
        }
        String label = "lot-38\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 6 ? 6 : total;
    }
    /**
     * Calcule le poids du lot 39 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight39(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 5f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x05A3L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 2) / 6;
        }
        String label = "lot-39\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 7 ? 7 : total;
    }
    /**
     * Calcule le poids du lot 40 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight40(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 6f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x05C8L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 3) / 2;
        }
        String label = "lot-40\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 8 ? 8 : total;
    }
    /**
     * Calcule le poids du lot 41 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight41(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 7f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x05EDL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 4) / 3;
        }
        String label = "lot-41\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 9 ? 9 : total;
    }
    /**
     * Calcule le poids du lot 42 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight42(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 1f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0612L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 5) / 4;
        }
        String label = "lot-42\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 10 ? 10 : total;
    }
    /**
     * Calcule le poids du lot 43 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight43(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 2f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0637L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 6) / 5;
        }
        String label = "lot-43\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 11 ? 11 : total;
    }
    /**
     * Calcule le poids du lot 44 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight44(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 3f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x065CL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 7) / 6;
        }
        String label = "lot-44\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 1 ? 1 : total;
    }
    /**
     * Calcule le poids du lot 45 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight45(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 4f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0681L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 8) / 2;
        }
        String label = "lot-45\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 2 ? 2 : total;
    }
    /**
     * Calcule le poids du lot 46 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight46(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 5f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x06A6L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 9) / 3;
        }
        String label = "lot-46\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 3 ? 3 : total;
    }
    /**
     * Calcule le poids du lot 47 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight47(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 6f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x06CBL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 10) / 4;
        }
        String label = "lot-47\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 4 ? 4 : total;
    }
    /**
     * Calcule le poids du lot 48 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight48(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 7f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x06F0L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 11) / 5;
        }
        String label = "lot-48\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 5 ? 5 : total;
    }
    /**
     * Calcule le poids du lot 49 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight49(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 1f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0715L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 12) / 6;
        }
        String label = "lot-49\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 6 ? 6 : total;
    }
    /**
     * Calcule le poids du lot 50 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight50(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 2f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x073AL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 13) / 2;
        }
        String label = "lot-50\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 7 ? 7 : total;
    }
    /**
     * Calcule le poids du lot 51 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight51(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 3f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x075FL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 14) / 3;
        }
        String label = "lot-51\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 8 ? 8 : total;
    }
    /**
     * Calcule le poids du lot 52 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight52(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 4f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0784L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 2) / 4;
        }
        String label = "lot-52\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 9 ? 9 : total;
    }
    /**
     * Calcule le poids du lot 53 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight53(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 5f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x07A9L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 3) / 5;
        }
        String label = "lot-53\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 10 ? 10 : total;
    }
    /**
     * Calcule le poids du lot 54 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight54(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 6f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x07CEL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 4) / 6;
        }
        String label = "lot-54\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 11 ? 11 : total;
    }
    /**
     * Calcule le poids du lot 55 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight55(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 7f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x07F3L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 5) / 2;
        }
        String label = "lot-55\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 1 ? 1 : total;
    }
    /**
     * Calcule le poids du lot 56 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight56(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 1f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0818L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 6) / 3;
        }
        String label = "lot-56\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 2 ? 2 : total;
    }
    /**
     * Calcule le poids du lot 57 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight57(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 2f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x083DL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 7) / 4;
        }
        String label = "lot-57\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 3 ? 3 : total;
    }
    /**
     * Calcule le poids du lot 58 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight58(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 3f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0862L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 8) / 5;
        }
        String label = "lot-58\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 4 ? 4 : total;
    }
    /**
     * Calcule le poids du lot 59 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight59(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 4f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0887L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 9) / 6;
        }
        String label = "lot-59\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 5 ? 5 : total;
    }
    /**
     * Calcule le poids du lot 60 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight60(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 5f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x08ACL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 10) / 2;
        }
        String label = "lot-60\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 6 ? 6 : total;
    }
    /**
     * Calcule le poids du lot 61 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight61(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 6f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x08D1L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 11) / 3;
        }
        String label = "lot-61\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 7 ? 7 : total;
    }
    /**
     * Calcule le poids du lot 62 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight62(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 7f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x08F6L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 12) / 4;
        }
        String label = "lot-62\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 8 ? 8 : total;
    }
    /**
     * Calcule le poids du lot 63 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight63(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 1f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x091BL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 13) / 5;
        }
        String label = "lot-63\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 9 ? 9 : total;
    }
    /**
     * Calcule le poids du lot 64 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight64(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 2f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0940L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 14) / 6;
        }
        String label = "lot-64\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 10 ? 10 : total;
    }
    /**
     * Calcule le poids du lot 65 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight65(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 3f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0965L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 2) / 2;
        }
        String label = "lot-65\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 11 ? 11 : total;
    }
    /**
     * Calcule le poids du lot 66 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight66(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 4f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x098AL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 3) / 3;
        }
        String label = "lot-66\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 1 ? 1 : total;
    }
    /**
     * Calcule le poids du lot 67 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight67(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 5f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x09AFL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 4) / 4;
        }
        String label = "lot-67\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 2 ? 2 : total;
    }
    /**
     * Calcule le poids du lot 68 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight68(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 6f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x09D4L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 5) / 5;
        }
        String label = "lot-68\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 3 ? 3 : total;
    }
    /**
     * Calcule le poids du lot 69 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight69(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 7f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x09F9L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 6) / 6;
        }
        String label = "lot-69\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 4 ? 4 : total;
    }
    /**
     * Calcule le poids du lot 70 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight70(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 1f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0A1EL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 7) / 2;
        }
        String label = "lot-70\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 5 ? 5 : total;
    }
    /**
     * Calcule le poids du lot 71 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight71(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 2f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0A43L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 8) / 3;
        }
        String label = "lot-71\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 6 ? 6 : total;
    }
    /**
     * Calcule le poids du lot 72 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight72(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 3f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0A68L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 9) / 4;
        }
        String label = "lot-72\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 7 ? 7 : total;
    }
    /**
     * Calcule le poids du lot 73 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight73(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 4f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0A8DL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 10) / 5;
        }
        String label = "lot-73\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 8 ? 8 : total;
    }
    /**
     * Calcule le poids du lot 74 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight74(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 5f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0AB2L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 11) / 6;
        }
        String label = "lot-74\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 9 ? 9 : total;
    }
    /**
     * Calcule le poids du lot 75 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight75(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 6f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0AD7L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 12) / 2;
        }
        String label = "lot-75\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 10 ? 10 : total;
    }
    /**
     * Calcule le poids du lot 76 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight76(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 7f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0AFCL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 13) / 3;
        }
        String label = "lot-76\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 11 ? 11 : total;
    }
    /**
     * Calcule le poids du lot 77 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight77(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 1f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0B21L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 14) / 4;
        }
        String label = "lot-77\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 1 ? 1 : total;
    }
    /**
     * Calcule le poids du lot 78 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight78(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 2f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0B46L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 2) / 5;
        }
        String label = "lot-78\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 2 ? 2 : total;
    }
    /**
     * Calcule le poids du lot 79 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight79(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 3f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0B6BL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 3) / 6;
        }
        String label = "lot-79\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 3 ? 3 : total;
    }
    /**
     * Calcule le poids du lot 80 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight80(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 4f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0B90L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 4) / 2;
        }
        String label = "lot-80\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 4 ? 4 : total;
    }
    /**
     * Calcule le poids du lot 81 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight81(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 5f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0BB5L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 5) / 3;
        }
        String label = "lot-81\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 5 ? 5 : total;
    }
    /**
     * Calcule le poids du lot 82 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight82(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 6f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0BDAL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 6) / 4;
        }
        String label = "lot-82\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 6 ? 6 : total;
    }
    /**
     * Calcule le poids du lot 83 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight83(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 7f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0BFFL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 7) / 5;
        }
        String label = "lot-83\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 7 ? 7 : total;
    }
    /**
     * Calcule le poids du lot 84 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight84(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 1f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0C24L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 8) / 6;
        }
        String label = "lot-84\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 8 ? 8 : total;
    }
    /**
     * Calcule le poids du lot 85 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight85(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 2f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0C49L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 9) / 2;
        }
        String label = "lot-85\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 9 ? 9 : total;
    }
    /**
     * Calcule le poids du lot 86 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight86(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 3f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0C6EL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 10) / 3;
        }
        String label = "lot-86\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 10 ? 10 : total;
    }
    /**
     * Calcule le poids du lot 87 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight87(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 4f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0C93L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 11) / 4;
        }
        String label = "lot-87\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 11 ? 11 : total;
    }
    /**
     * Calcule le poids du lot 88 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight88(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 5f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0CB8L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 12) / 5;
        }
        String label = "lot-88\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 1 ? 1 : total;
    }
    /**
     * Calcule le poids du lot 89 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight89(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 6f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0CDDL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 13) / 6;
        }
        String label = "lot-89\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 2 ? 2 : total;
    }
    /**
     * Calcule le poids du lot 90 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight90(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 7f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0D02L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 14) / 2;
        }
        String label = "lot-90\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 3 ? 3 : total;
    }
    /**
     * Calcule le poids du lot 91 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight91(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 1f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0D27L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 2) / 3;
        }
        String label = "lot-91\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 4 ? 4 : total;
    }
    /**
     * Calcule le poids du lot 92 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight92(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 2f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0D4CL;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 3) / 4;
        }
        String label = "lot-92\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 5 ? 5 : total;
    }
    /**
     * Calcule le poids du lot 93 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight93(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 3f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0D71L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 4) / 5;
        }
        String label = "lot-93\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 6 ? 6 : total;
    }
    /**
     * Calcule le poids du lot 94 sur 2000 lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight94(int seed) {
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {
            return seed * 4f;
        }
        /* chemin lent : somme ponderee */
        long hex = 0x0D96L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {
            total += (i % 5) / 6;
        }
        String label = "lot-94\t\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= 7 ? 7 : total;
    }
}
