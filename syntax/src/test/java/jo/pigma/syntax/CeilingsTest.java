package jo.pigma.syntax;

import org.junit.Test;

import jo.pigma.core.TextBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Les plafonds (E3) : une ligne de plus de 20 000 colonnes n'est pas
 * colorée (jeton aucun, état reporté tel quel, les voisines restent
 * colorées) ; le mode gros fichier s'allume au-delà de 2,5 millions de
 * caractères ou 50 000 lignes — la coloration lexicale, elle, n'y est
 * jamais coupée.
 */
public class CeilingsTest {

    /** La ligne monstre : aucun jeton, l'état se reporte, voisines colorées. */
    @Test
    public void monsterLineIsNotColored() {
        StringBuilder text = new StringBuilder();
        text.append("int a = 1;\n");
        text.append("// ligne monstre de plus de 20 000 colonnes ");
        for (int i = 0; i < 20_100; i++) {
            text.append('x');
        }
        text.append('\n');
        text.append("int b = 2;\n");
        TextBuffer buffer = TextBuffer.create(text.toString());
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.JAVA);
        // la ligne 0 se lexte normalement
        assertTrue(document.lineTokens(0).tokens().length > 0);
        assertEquals(0, document.lineTokens(0).endState());
        // la ligne monstre : pas de coloration, l'état d'entrée se reporte
        assertEquals(0, document.lineTokens(1).tokens().length);
        assertEquals(document.lineTokens(0).endState(),
                document.lineTokens(1).endState());
        // la ligne suivante reste colorée : le moteur ne s'arrête pas
        assertTrue(document.lineTokens(2).tokens().length > 0);
        document.detach();
    }

    /** La frontière exacte : 20 000 colonnes se colorent encore. */
    @Test
    public void boundaryLineIsStillColored() {
        StringBuilder text = new StringBuilder();
        text.append("// ");
        for (int i = 0; i < 19_997; i++) {
            text.append('x');
        }
        TextBuffer buffer = TextBuffer.create(text.toString());
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.JAVA);
        assertTrue(document.lineTokens(0).tokens().length > 0);
        document.detach();
    }

    /** Le mode gros fichier : seuils de lignes et de caractères. */
    @Test
    public void bigFileModeThresholds() {
        TextBuffer lines = TextBuffer.create("\n".repeat(50_000) + "x");
        SyntaxDocument document = SyntaxDocument.open(lines, Languages.SQL);
        assertTrue(document.inBigFileMode());
        // la coloration L1 reste, même en mode gros fichier : la
        // dernière ligne (l'identifiant « x ») se lexte
        assertEquals(1, document.lineTokens(50_000).tokens().length);
        document.detach();

        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 26_000; i++) {
            huge.append("x".repeat(100)).append('\n');
        }
        TextBuffer chars = TextBuffer.create(huge.toString());
        SyntaxDocument byCharacters = SyntaxDocument.open(chars, Languages.SQL);
        assertTrue(byCharacters.inBigFileMode());
        byCharacters.detach();

        TextBuffer small = TextBuffer.create("select 1\n");
        SyntaxDocument normal = SyntaxDocument.open(small, Languages.SQL);
        assertFalse(normal.inBigFileMode());
        normal.detach();
    }
}
