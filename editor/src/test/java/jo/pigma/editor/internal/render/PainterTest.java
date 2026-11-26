package jo.pigma.editor.internal.render;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Les parties pures du peintre (JVM, sans canevas) : le chip de
 * couleur littérale (la couleur se parse de ses quatre formes) et
 * l'espèce des paires de ligatures (la flèche, le non-égal, la
 * comparaison, la conjonction, l'incrémentation).
 */
public final class PainterTest {

    /**
     * LE CHIP DE COULEUR : chaque forme du littéral hexadécimal se
     * parse en ARGB — #RGB, #RGBA (l'alpha y EST la quatrième
     * paire, à la CSS — l'avant la jetait), #RRGGBB, #RRGGBBAA.
     */
    @Test
    public void colorLiteralsParse() {
        assertEquals((int) 0xFFFF0000L, Painter.parseColorLiteral("#F00").intValue());
        // le #RGBA : l'alpha de la QUATRIÈME paire survit (8 ≈ 53 %,
        // à la CSS — l'avant la jetait)
        assertEquals(0x88FFFFFF, Painter.parseColorLiteral("#FFF8").intValue());
        assertEquals(0xFF3F51B5, Painter.parseColorLiteral("#3F51B5").intValue());
        assertEquals(0x803F51B5, Painter.parseColorLiteral("#803F51B5").intValue());
        assertNull("le littéral trop court ne se peint pas",
                Painter.parseColorLiteral("#F"));
        assertNull("le littéral de 5 paires ne se peint pas",
                Painter.parseColorLiteral("#F51B5"));
        assertNull("le littéral invalide ne se peint pas",
                Painter.parseColorLiteral("#GGG"));
    }

    /**
     * LES LIGATURES : chaque paire reconnue dit son espèce (la
     * flèche, le non-égal, la comparaison, la conjonction,
     * l'incrémentation), les paires inconnues ne disent rien —
     * la table est pure, le harnais JVM y lit sans pinceau.
     */
    @Test
    public void ligatureSpecies() {
        assertEquals("arrow", Painter.ligatureOf('=', '>'));
        assertEquals("arrow", Painter.ligatureOf('-', '>'));
        assertEquals("ne", Painter.ligatureOf('!', '='));
        assertEquals("cmp", Painter.ligatureOf('>', '='));
        assertEquals("cmp", Painter.ligatureOf('<', '='));
        assertEquals("cmp", Painter.ligatureOf('=', '='));
        assertEquals("cmp", Painter.ligatureOf('+', '='));
        assertEquals("logic", Painter.ligatureOf('&', '&'));
        assertEquals("logic", Painter.ligatureOf('|', '|'));
        assertEquals("incdec", Painter.ligatureOf('+', '+'));
        assertEquals("incdec", Painter.ligatureOf('-', '-'));
        assertNull("la paire quelconque ne se fond pas",
                Painter.ligatureOf('a', 'b'));
        assertNull("les deux signes de la flèche inversée ne se fondent pas",
                Painter.ligatureOf('<', '-'));
    }

    /** Le chiffre hexadécimal se reconnaît (les trois registres). */
    @Test
    public void hexDigits() {
        assertTrue(Painter.isHexDigit('0'));
        assertTrue(Painter.isHexDigit('a'));
        assertTrue(Painter.isHexDigit('F'));
        assertEquals(false, Painter.isHexDigit('g'));
    }
}
