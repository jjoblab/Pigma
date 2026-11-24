package exemple.kotlin

import java.util.Locale
import kotlin.math.abs

/**
 * Calcule le total pondéré d'un lot de lignes.
 * La documentation traverse les lignes, comme un bloc.
 */
data class Lot(val graine: Int, val taille: Int = 1) {

    // constantes de pondération : suffixes, préfixes, underscores
    private companion object {
        const val SEUIL = 0.125e-3
        const val MAX = 0x1F_FF
        const val BIN = 0b0101_1010u
        const val BIG = 1_000_000L
        const val EPSILON = 0.5f
        const val DEVISE = '\u20AC'
    }

    /* un bloc simple,
       imbriqué : /* le commentaire interne */ la suite du bloc */
    val poids: Double = if (graine > MAX && taille != -1) graine * taille * SEUIL else 0.0

    fun total(marge: Double = 0.0): Double = (poids - marge).coerceAtLeast(0.0)

    fun description(locale: Locale): String {
        val simple = "graine $graine sur $taille"
        val echappee = "tab\t retour\n dollar \$ guillemet \" ${'$'} et devises  ${DEVISE}"
        val gabarit = "total = ${String.format(locale, "%.2f", total())} pour $graine×$taille"
        return "$simple ; $echappee ; $gabarit"
    }

    fun brut(): String = """
        Lot(graine=$graine, taille=$taille) {
            poids = ${total()}
            binaire = ${if (BIN > 0u) "positif" else "nul"}
        }
    """.trimIndent()

    fun gabaritImbrique(): String {
        val interne = "marge ${total(0.5)} à ${DEVISE}"
        return "externe ${interne.length} lettres, interne \"$interne\", brut \"\"\"${brut().length}\"\"\""
    }

    fun chemin(mode: String): String = """
        debut
        ${if (mode == "long")
            "interieur sur deux lignes"
        else
            "court"}
        fin
    """.trimIndent()

    fun plages(n: Int): String {
        val pairs = (0 until n).filter { it % 2 == 0 }
        val segment = when {
            n <= 0 -> "vide"
            n in 1..9 -> "petit"
            n in 10..99 -> "moyen"
            n !in 100..<1_000 -> "moyen-grand"
            else -> "grand"
        }
        return "$pairs .. $segment"
    }
}

object Registre {
    private val lots = mutableMapOf<String, Lot>()

    fun enregistre(cle: String, lot: Lot) {
        lots[cle] = lot
    }

    fun cherche(cle: String): Lot? = lots[cle]

    fun nom(cle: String): String = cherche(cle)?.description(Locale.ROOT) ?: "inconnu"

    fun total(): Double = lots.values.sumOf { it.total() }

    inline fun <reified T : Any> typed(valeur: Any): T? = valeur as? T
}

fun main() {
    val lot = Lot(graine = 12, taille = 34)
    Registre.enregistre("demo", lot)
    val nom: String = Registre.nom("demo") ?: "sans nom"
    val boucle = StringBuilder()
    for (i in 0..3) {
        boucle.append("ligne $i, ")
    }
    val elvis: Int? = null
    val valeur = elvis ?: -1
    val securise = nom.length.toDouble()?.let { abs(it) } ?: 0.0
    println("""récapitulatif : $nom
        valeur = $valeur, sécurisé = $securise
        boucle = ${boucle.toString().dropLast(2)}""")
    println(lot.brut())
    println(lot.chemin("long"))
}
