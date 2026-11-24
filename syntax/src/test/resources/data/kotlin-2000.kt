// Corpus volumineux de reference, genere pour le banc et le test
// de redemarrage : ~2000 lignes Kotlin, gabarits multi-lignes.

fun bloc1(niveau: Int): String {
    // ligne de commentaire n1
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x01_FF + 1L + 0b0101_0001u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"1\" lignes"
    return """bloc 1
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre1(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -1
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 1 (${entries.size} entrees)
        ${if (seuil > 1) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc2(degre: Int): String {
    // ligne de commentaire n2
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x02_FF + 2L + 0b0101_0010u
    val chaine = "bloc $degre : total ${degre * constante} sur \"2\" lignes"
    return """bloc 2
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre2(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -2
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 2 (${entries.size} entrees)
        ${if (seuil > 2) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc3(niveau: Int): String {
    // ligne de commentaire n3
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x03_FF + 3L + 0b0101_0011u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"3\" lignes"
    return """bloc 3
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre3(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -3
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 3 (${entries.size} entrees)
        ${if (seuil > 3) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc4(degre: Int): String {
    // ligne de commentaire n4
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x04_FF + 4L + 0b0101_0100u
    val chaine = "bloc $degre : total ${degre * constante} sur \"4\" lignes"
    return """bloc 4
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre4(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -4
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 4 (${entries.size} entrees)
        ${if (seuil > 4) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc5(niveau: Int): String {
    // ligne de commentaire n5
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x05_FF + 5L + 0b0101_0101u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"5\" lignes"
    return """bloc 5
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre5(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -5
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 5 (${entries.size} entrees)
        ${if (seuil > 5) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc6(degre: Int): String {
    // ligne de commentaire n6
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x06_FF + 6L + 0b0101_0110u
    val chaine = "bloc $degre : total ${degre * constante} sur \"6\" lignes"
    return """bloc 6
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre6(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -6
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 6 (${entries.size} entrees)
        ${if (seuil > 6) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc7(niveau: Int): String {
    // ligne de commentaire n7
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x07_FF + 7L + 0b0101_0111u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"7\" lignes"
    return """bloc 7
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre7(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -7
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 7 (${entries.size} entrees)
        ${if (seuil > 7) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc8(degre: Int): String {
    // ligne de commentaire n8
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x08_FF + 8L + 0b0101_1000u
    val chaine = "bloc $degre : total ${degre * constante} sur \"8\" lignes"
    return """bloc 8
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre8(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -8
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 8 (${entries.size} entrees)
        ${if (seuil > 8) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc9(niveau: Int): String {
    // ligne de commentaire n9
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x09_FF + 9L + 0b0101_1001u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"9\" lignes"
    return """bloc 9
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre9(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -9
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 9 (${entries.size} entrees)
        ${if (seuil > 9) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc10(degre: Int): String {
    // ligne de commentaire n10
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x0A_FF + 10L + 0b0101_1010u
    val chaine = "bloc $degre : total ${degre * constante} sur \"10\" lignes"
    return """bloc 10
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre10(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -10
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 10 (${entries.size} entrees)
        ${if (seuil > 10) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc11(niveau: Int): String {
    // ligne de commentaire n11
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x0B_FF + 11L + 0b0101_1011u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"11\" lignes"
    return """bloc 11
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre11(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -11
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 11 (${entries.size} entrees)
        ${if (seuil > 11) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc12(degre: Int): String {
    // ligne de commentaire n12
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x0C_FF + 12L + 0b0101_1100u
    val chaine = "bloc $degre : total ${degre * constante} sur \"12\" lignes"
    return """bloc 12
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre12(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -12
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 12 (${entries.size} entrees)
        ${if (seuil > 12) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc13(niveau: Int): String {
    // ligne de commentaire n13
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x0D_FF + 13L + 0b0101_1101u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"13\" lignes"
    return """bloc 13
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre13(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -13
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 13 (${entries.size} entrees)
        ${if (seuil > 13) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc14(degre: Int): String {
    // ligne de commentaire n14
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x0E_FF + 14L + 0b0101_1110u
    val chaine = "bloc $degre : total ${degre * constante} sur \"14\" lignes"
    return """bloc 14
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre14(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -14
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 14 (${entries.size} entrees)
        ${if (seuil > 14) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc15(niveau: Int): String {
    // ligne de commentaire n15
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x0F_FF + 15L + 0b0101_1111u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"15\" lignes"
    return """bloc 15
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre15(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -15
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 15 (${entries.size} entrees)
        ${if (seuil > 15) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc16(degre: Int): String {
    // ligne de commentaire n16
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x10_FF + 16L + 0b0101_10000u
    val chaine = "bloc $degre : total ${degre * constante} sur \"16\" lignes"
    return """bloc 16
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre16(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -16
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 16 (${entries.size} entrees)
        ${if (seuil > 16) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc17(niveau: Int): String {
    // ligne de commentaire n17
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x11_FF + 17L + 0b0101_10001u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"17\" lignes"
    return """bloc 17
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre17(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -17
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 17 (${entries.size} entrees)
        ${if (seuil > 17) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc18(degre: Int): String {
    // ligne de commentaire n18
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x12_FF + 18L + 0b0101_10010u
    val chaine = "bloc $degre : total ${degre * constante} sur \"18\" lignes"
    return """bloc 18
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre18(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -18
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 18 (${entries.size} entrees)
        ${if (seuil > 18) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc19(niveau: Int): String {
    // ligne de commentaire n19
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x13_FF + 19L + 0b0101_10011u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"19\" lignes"
    return """bloc 19
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre19(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -19
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 19 (${entries.size} entrees)
        ${if (seuil > 19) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc20(degre: Int): String {
    // ligne de commentaire n20
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x14_FF + 20L + 0b0101_10100u
    val chaine = "bloc $degre : total ${degre * constante} sur \"20\" lignes"
    return """bloc 20
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre20(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -20
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 20 (${entries.size} entrees)
        ${if (seuil > 20) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc21(niveau: Int): String {
    // ligne de commentaire n21
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x15_FF + 21L + 0b0101_10101u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"21\" lignes"
    return """bloc 21
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre21(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -21
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 21 (${entries.size} entrees)
        ${if (seuil > 21) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc22(degre: Int): String {
    // ligne de commentaire n22
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x16_FF + 22L + 0b0101_10110u
    val chaine = "bloc $degre : total ${degre * constante} sur \"22\" lignes"
    return """bloc 22
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre22(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -22
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 22 (${entries.size} entrees)
        ${if (seuil > 22) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc23(niveau: Int): String {
    // ligne de commentaire n23
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x17_FF + 23L + 0b0101_10111u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"23\" lignes"
    return """bloc 23
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre23(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -23
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 23 (${entries.size} entrees)
        ${if (seuil > 23) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc24(degre: Int): String {
    // ligne de commentaire n24
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x18_FF + 24L + 0b0101_11000u
    val chaine = "bloc $degre : total ${degre * constante} sur \"24\" lignes"
    return """bloc 24
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre24(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -24
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 24 (${entries.size} entrees)
        ${if (seuil > 24) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc25(niveau: Int): String {
    // ligne de commentaire n25
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x19_FF + 25L + 0b0101_11001u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"25\" lignes"
    return """bloc 25
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre25(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -25
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 25 (${entries.size} entrees)
        ${if (seuil > 25) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc26(degre: Int): String {
    // ligne de commentaire n26
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x1A_FF + 26L + 0b0101_11010u
    val chaine = "bloc $degre : total ${degre * constante} sur \"26\" lignes"
    return """bloc 26
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre26(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -26
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 26 (${entries.size} entrees)
        ${if (seuil > 26) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc27(niveau: Int): String {
    // ligne de commentaire n27
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x1B_FF + 27L + 0b0101_11011u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"27\" lignes"
    return """bloc 27
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre27(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -27
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 27 (${entries.size} entrees)
        ${if (seuil > 27) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc28(degre: Int): String {
    // ligne de commentaire n28
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x1C_FF + 28L + 0b0101_11100u
    val chaine = "bloc $degre : total ${degre * constante} sur \"28\" lignes"
    return """bloc 28
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre28(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -28
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 28 (${entries.size} entrees)
        ${if (seuil > 28) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc29(niveau: Int): String {
    // ligne de commentaire n29
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x1D_FF + 29L + 0b0101_11101u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"29\" lignes"
    return """bloc 29
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre29(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -29
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 29 (${entries.size} entrees)
        ${if (seuil > 29) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc30(degre: Int): String {
    // ligne de commentaire n30
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x1E_FF + 30L + 0b0101_11110u
    val chaine = "bloc $degre : total ${degre * constante} sur \"30\" lignes"
    return """bloc 30
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre30(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -30
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 30 (${entries.size} entrees)
        ${if (seuil > 30) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc31(niveau: Int): String {
    // ligne de commentaire n31
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x1F_FF + 31L + 0b0101_11111u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"31\" lignes"
    return """bloc 31
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre31(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -31
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 31 (${entries.size} entrees)
        ${if (seuil > 31) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc32(degre: Int): String {
    // ligne de commentaire n32
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x20_FF + 32L + 0b0101_100000u
    val chaine = "bloc $degre : total ${degre * constante} sur \"32\" lignes"
    return """bloc 32
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre32(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -32
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 32 (${entries.size} entrees)
        ${if (seuil > 32) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc33(niveau: Int): String {
    // ligne de commentaire n33
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x21_FF + 33L + 0b0101_100001u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"33\" lignes"
    return """bloc 33
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre33(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -33
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 33 (${entries.size} entrees)
        ${if (seuil > 33) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc34(degre: Int): String {
    // ligne de commentaire n34
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x22_FF + 34L + 0b0101_100010u
    val chaine = "bloc $degre : total ${degre * constante} sur \"34\" lignes"
    return """bloc 34
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre34(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -34
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 34 (${entries.size} entrees)
        ${if (seuil > 34) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc35(niveau: Int): String {
    // ligne de commentaire n35
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x23_FF + 35L + 0b0101_100011u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"35\" lignes"
    return """bloc 35
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre35(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -35
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 35 (${entries.size} entrees)
        ${if (seuil > 35) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc36(degre: Int): String {
    // ligne de commentaire n36
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x24_FF + 36L + 0b0101_100100u
    val chaine = "bloc $degre : total ${degre * constante} sur \"36\" lignes"
    return """bloc 36
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre36(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -36
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 36 (${entries.size} entrees)
        ${if (seuil > 36) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc37(niveau: Int): String {
    // ligne de commentaire n37
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x25_FF + 37L + 0b0101_100101u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"37\" lignes"
    return """bloc 37
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre37(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -37
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 37 (${entries.size} entrees)
        ${if (seuil > 37) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc38(degre: Int): String {
    // ligne de commentaire n38
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x26_FF + 38L + 0b0101_100110u
    val chaine = "bloc $degre : total ${degre * constante} sur \"38\" lignes"
    return """bloc 38
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre38(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -38
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 38 (${entries.size} entrees)
        ${if (seuil > 38) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc39(niveau: Int): String {
    // ligne de commentaire n39
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x27_FF + 39L + 0b0101_100111u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"39\" lignes"
    return """bloc 39
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre39(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -39
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 39 (${entries.size} entrees)
        ${if (seuil > 39) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc40(degre: Int): String {
    // ligne de commentaire n40
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x28_FF + 40L + 0b0101_101000u
    val chaine = "bloc $degre : total ${degre * constante} sur \"40\" lignes"
    return """bloc 40
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre40(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -40
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 40 (${entries.size} entrees)
        ${if (seuil > 40) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc41(niveau: Int): String {
    // ligne de commentaire n41
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x29_FF + 41L + 0b0101_101001u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"41\" lignes"
    return """bloc 41
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre41(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -41
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 41 (${entries.size} entrees)
        ${if (seuil > 41) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc42(degre: Int): String {
    // ligne de commentaire n42
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x2A_FF + 42L + 0b0101_101010u
    val chaine = "bloc $degre : total ${degre * constante} sur \"42\" lignes"
    return """bloc 42
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre42(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -42
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 42 (${entries.size} entrees)
        ${if (seuil > 42) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc43(niveau: Int): String {
    // ligne de commentaire n43
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x2B_FF + 43L + 0b0101_101011u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"43\" lignes"
    return """bloc 43
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre43(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -43
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 43 (${entries.size} entrees)
        ${if (seuil > 43) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc44(degre: Int): String {
    // ligne de commentaire n44
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x2C_FF + 44L + 0b0101_101100u
    val chaine = "bloc $degre : total ${degre * constante} sur \"44\" lignes"
    return """bloc 44
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre44(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -44
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 44 (${entries.size} entrees)
        ${if (seuil > 44) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc45(niveau: Int): String {
    // ligne de commentaire n45
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x2D_FF + 45L + 0b0101_101101u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"45\" lignes"
    return """bloc 45
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre45(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -45
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 45 (${entries.size} entrees)
        ${if (seuil > 45) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc46(degre: Int): String {
    // ligne de commentaire n46
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x2E_FF + 46L + 0b0101_101110u
    val chaine = "bloc $degre : total ${degre * constante} sur \"46\" lignes"
    return """bloc 46
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre46(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -46
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 46 (${entries.size} entrees)
        ${if (seuil > 46) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc47(niveau: Int): String {
    // ligne de commentaire n47
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x2F_FF + 47L + 0b0101_101111u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"47\" lignes"
    return """bloc 47
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre47(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -47
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 47 (${entries.size} entrees)
        ${if (seuil > 47) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc48(degre: Int): String {
    // ligne de commentaire n48
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x30_FF + 48L + 0b0101_110000u
    val chaine = "bloc $degre : total ${degre * constante} sur \"48\" lignes"
    return """bloc 48
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre48(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -48
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 48 (${entries.size} entrees)
        ${if (seuil > 48) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc49(niveau: Int): String {
    // ligne de commentaire n49
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x31_FF + 49L + 0b0101_110001u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"49\" lignes"
    return """bloc 49
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre49(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -49
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 49 (${entries.size} entrees)
        ${if (seuil > 49) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc50(degre: Int): String {
    // ligne de commentaire n50
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x32_FF + 50L + 0b0101_110010u
    val chaine = "bloc $degre : total ${degre * constante} sur \"50\" lignes"
    return """bloc 50
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre50(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -50
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 50 (${entries.size} entrees)
        ${if (seuil > 50) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc51(niveau: Int): String {
    // ligne de commentaire n51
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x33_FF + 51L + 0b0101_110011u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"51\" lignes"
    return """bloc 51
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre51(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -51
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 51 (${entries.size} entrees)
        ${if (seuil > 51) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc52(degre: Int): String {
    // ligne de commentaire n52
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x34_FF + 52L + 0b0101_110100u
    val chaine = "bloc $degre : total ${degre * constante} sur \"52\" lignes"
    return """bloc 52
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre52(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -52
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 52 (${entries.size} entrees)
        ${if (seuil > 52) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc53(niveau: Int): String {
    // ligne de commentaire n53
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x35_FF + 53L + 0b0101_110101u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"53\" lignes"
    return """bloc 53
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre53(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -53
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 53 (${entries.size} entrees)
        ${if (seuil > 53) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc54(degre: Int): String {
    // ligne de commentaire n54
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x36_FF + 54L + 0b0101_110110u
    val chaine = "bloc $degre : total ${degre * constante} sur \"54\" lignes"
    return """bloc 54
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre54(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -54
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 54 (${entries.size} entrees)
        ${if (seuil > 54) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc55(niveau: Int): String {
    // ligne de commentaire n55
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x37_FF + 55L + 0b0101_110111u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"55\" lignes"
    return """bloc 55
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre55(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -55
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 55 (${entries.size} entrees)
        ${if (seuil > 55) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc56(degre: Int): String {
    // ligne de commentaire n56
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x38_FF + 56L + 0b0101_111000u
    val chaine = "bloc $degre : total ${degre * constante} sur \"56\" lignes"
    return """bloc 56
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre56(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -56
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 56 (${entries.size} entrees)
        ${if (seuil > 56) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc57(niveau: Int): String {
    // ligne de commentaire n57
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x39_FF + 57L + 0b0101_111001u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"57\" lignes"
    return """bloc 57
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre57(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -57
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 57 (${entries.size} entrees)
        ${if (seuil > 57) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc58(degre: Int): String {
    // ligne de commentaire n58
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x3A_FF + 58L + 0b0101_111010u
    val chaine = "bloc $degre : total ${degre * constante} sur \"58\" lignes"
    return """bloc 58
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre58(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -58
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 58 (${entries.size} entrees)
        ${if (seuil > 58) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc59(niveau: Int): String {
    // ligne de commentaire n59
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x3B_FF + 59L + 0b0101_111011u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"59\" lignes"
    return """bloc 59
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre59(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -59
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 59 (${entries.size} entrees)
        ${if (seuil > 59) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc60(degre: Int): String {
    // ligne de commentaire n60
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x3C_FF + 60L + 0b0101_111100u
    val chaine = "bloc $degre : total ${degre * constante} sur \"60\" lignes"
    return """bloc 60
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre60(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -60
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 60 (${entries.size} entrees)
        ${if (seuil > 60) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc61(niveau: Int): String {
    // ligne de commentaire n61
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x3D_FF + 61L + 0b0101_111101u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"61\" lignes"
    return """bloc 61
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre61(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -61
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 61 (${entries.size} entrees)
        ${if (seuil > 61) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc62(degre: Int): String {
    // ligne de commentaire n62
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x3E_FF + 62L + 0b0101_111110u
    val chaine = "bloc $degre : total ${degre * constante} sur \"62\" lignes"
    return """bloc 62
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre62(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -62
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 62 (${entries.size} entrees)
        ${if (seuil > 62) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc63(niveau: Int): String {
    // ligne de commentaire n63
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x3F_FF + 63L + 0b0101_111111u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"63\" lignes"
    return """bloc 63
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre63(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -63
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 63 (${entries.size} entrees)
        ${if (seuil > 63) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc64(degre: Int): String {
    // ligne de commentaire n64
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x40_FF + 64L + 0b0101_1000000u
    val chaine = "bloc $degre : total ${degre * constante} sur \"64\" lignes"
    return """bloc 64
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre64(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -64
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 64 (${entries.size} entrees)
        ${if (seuil > 64) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc65(niveau: Int): String {
    // ligne de commentaire n65
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x41_FF + 65L + 0b0101_1000001u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"65\" lignes"
    return """bloc 65
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre65(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -65
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 65 (${entries.size} entrees)
        ${if (seuil > 65) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc66(degre: Int): String {
    // ligne de commentaire n66
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x42_FF + 66L + 0b0101_1000010u
    val chaine = "bloc $degre : total ${degre * constante} sur \"66\" lignes"
    return """bloc 66
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre66(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -66
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 66 (${entries.size} entrees)
        ${if (seuil > 66) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc67(niveau: Int): String {
    // ligne de commentaire n67
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x43_FF + 67L + 0b0101_1000011u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"67\" lignes"
    return """bloc 67
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre67(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -67
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 67 (${entries.size} entrees)
        ${if (seuil > 67) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc68(degre: Int): String {
    // ligne de commentaire n68
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x44_FF + 68L + 0b0101_1000100u
    val chaine = "bloc $degre : total ${degre * constante} sur \"68\" lignes"
    return """bloc 68
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre68(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -68
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 68 (${entries.size} entrees)
        ${if (seuil > 68) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc69(niveau: Int): String {
    // ligne de commentaire n69
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x45_FF + 69L + 0b0101_1000101u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"69\" lignes"
    return """bloc 69
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre69(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -69
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 69 (${entries.size} entrees)
        ${if (seuil > 69) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc70(degre: Int): String {
    // ligne de commentaire n70
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x46_FF + 70L + 0b0101_1000110u
    val chaine = "bloc $degre : total ${degre * constante} sur \"70\" lignes"
    return """bloc 70
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre70(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -70
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 70 (${entries.size} entrees)
        ${if (seuil > 70) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc71(niveau: Int): String {
    // ligne de commentaire n71
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x47_FF + 71L + 0b0101_1000111u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"71\" lignes"
    return """bloc 71
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre71(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -71
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 71 (${entries.size} entrees)
        ${if (seuil > 71) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc72(degre: Int): String {
    // ligne de commentaire n72
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x48_FF + 72L + 0b0101_1001000u
    val chaine = "bloc $degre : total ${degre * constante} sur \"72\" lignes"
    return """bloc 72
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre72(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -72
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 72 (${entries.size} entrees)
        ${if (seuil > 72) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc73(niveau: Int): String {
    // ligne de commentaire n73
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x49_FF + 73L + 0b0101_1001001u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"73\" lignes"
    return """bloc 73
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre73(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -73
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 73 (${entries.size} entrees)
        ${if (seuil > 73) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc74(degre: Int): String {
    // ligne de commentaire n74
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x4A_FF + 74L + 0b0101_1001010u
    val chaine = "bloc $degre : total ${degre * constante} sur \"74\" lignes"
    return """bloc 74
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre74(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -74
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 74 (${entries.size} entrees)
        ${if (seuil > 74) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc75(niveau: Int): String {
    // ligne de commentaire n75
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x4B_FF + 75L + 0b0101_1001011u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"75\" lignes"
    return """bloc 75
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre75(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -75
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 75 (${entries.size} entrees)
        ${if (seuil > 75) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc76(degre: Int): String {
    // ligne de commentaire n76
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x4C_FF + 76L + 0b0101_1001100u
    val chaine = "bloc $degre : total ${degre * constante} sur \"76\" lignes"
    return """bloc 76
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre76(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -76
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 76 (${entries.size} entrees)
        ${if (seuil > 76) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc77(niveau: Int): String {
    // ligne de commentaire n77
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x4D_FF + 77L + 0b0101_1001101u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"77\" lignes"
    return """bloc 77
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre77(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -77
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 77 (${entries.size} entrees)
        ${if (seuil > 77) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc78(degre: Int): String {
    // ligne de commentaire n78
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x4E_FF + 78L + 0b0101_1001110u
    val chaine = "bloc $degre : total ${degre * constante} sur \"78\" lignes"
    return """bloc 78
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre78(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -78
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 78 (${entries.size} entrees)
        ${if (seuil > 78) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc79(niveau: Int): String {
    // ligne de commentaire n79
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x4F_FF + 79L + 0b0101_1001111u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"79\" lignes"
    return """bloc 79
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre79(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -79
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 79 (${entries.size} entrees)
        ${if (seuil > 79) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc80(degre: Int): String {
    // ligne de commentaire n80
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x50_FF + 80L + 0b0101_1010000u
    val chaine = "bloc $degre : total ${degre * constante} sur \"80\" lignes"
    return """bloc 80
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre80(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -80
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 80 (${entries.size} entrees)
        ${if (seuil > 80) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc81(niveau: Int): String {
    // ligne de commentaire n81
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x51_FF + 81L + 0b0101_1010001u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"81\" lignes"
    return """bloc 81
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre81(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -81
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 81 (${entries.size} entrees)
        ${if (seuil > 81) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc82(degre: Int): String {
    // ligne de commentaire n82
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x52_FF + 82L + 0b0101_1010010u
    val chaine = "bloc $degre : total ${degre * constante} sur \"82\" lignes"
    return """bloc 82
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre82(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -82
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 82 (${entries.size} entrees)
        ${if (seuil > 82) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc83(niveau: Int): String {
    // ligne de commentaire n83
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x53_FF + 83L + 0b0101_1010011u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"83\" lignes"
    return """bloc 83
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre83(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -83
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 83 (${entries.size} entrees)
        ${if (seuil > 83) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc84(degre: Int): String {
    // ligne de commentaire n84
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x54_FF + 84L + 0b0101_1010100u
    val chaine = "bloc $degre : total ${degre * constante} sur \"84\" lignes"
    return """bloc 84
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre84(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -84
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 84 (${entries.size} entrees)
        ${if (seuil > 84) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc85(niveau: Int): String {
    // ligne de commentaire n85
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x55_FF + 85L + 0b0101_1010101u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"85\" lignes"
    return """bloc 85
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre85(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -85
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 85 (${entries.size} entrees)
        ${if (seuil > 85) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc86(degre: Int): String {
    // ligne de commentaire n86
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x56_FF + 86L + 0b0101_1010110u
    val chaine = "bloc $degre : total ${degre * constante} sur \"86\" lignes"
    return """bloc 86
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre86(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -86
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 86 (${entries.size} entrees)
        ${if (seuil > 86) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc87(niveau: Int): String {
    // ligne de commentaire n87
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x57_FF + 87L + 0b0101_1010111u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"87\" lignes"
    return """bloc 87
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre87(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -87
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 87 (${entries.size} entrees)
        ${if (seuil > 87) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc88(degre: Int): String {
    // ligne de commentaire n88
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x58_FF + 88L + 0b0101_1011000u
    val chaine = "bloc $degre : total ${degre * constante} sur \"88\" lignes"
    return """bloc 88
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre88(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -88
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 88 (${entries.size} entrees)
        ${if (seuil > 88) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc89(niveau: Int): String {
    // ligne de commentaire n89
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x59_FF + 89L + 0b0101_1011001u
    val chaine = "bloc $niveau : total ${niveau * constante} sur \"89\" lignes"
    return """bloc 89
        argument = niveau (${niveau})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre89(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -89
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 89 (${entries.size} entrees)
        ${if (seuil > 89) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}

fun bloc90(degre: Int): String {
    // ligne de commentaire n90
    /* bloc imbriqué : /* interne */ suite */
    val constante = 0x5A_FF + 90L + 0b0101_1011010u
    val chaine = "bloc $degre : total ${degre * constante} sur \"90\" lignes"
    return """bloc 90
        argument = degre (${degre})
        gabarit = ${chaine.length} lettres, ${chaine}
        constante = $constante
    """.trimIndent()
}

class Registre90(private val entries: Map<String, Int> = emptyMap()) {
    fun cherche(cle: String): Int? = entries[cle] ?: -90
    fun nom(cle: String): String = "${cle} = ${cherche(cle)}"
    fun gabaritLong(seuil: Int): String = """
        registre 90 (${entries.size} entrees)
        ${if (seuil > 90) "au-dessus" else "en-dessous"}
        total = ${entries.values.sum()}
    """.trimIndent()
}
