package jo.pigma.syntax;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Portées et thème : l'internement donne la même instance pour un même
 * nom, la chaîne des parents suit les points, et le thème résout par
 * repli — une clé absente n'est jamais une couleur absente.
 */
public class ScopeThemeTest {

    /** Deux demandes du même nom rendent la même instance. */
    @Test
    public void scopesAreInterned() {
        assertSame(Scope.of("comment.doc"), Scope.of("comment.doc"));
        assertSame(Scope.of("string"), Scope.of("string.escape").parent());
        assertEquals("string", Scope.of("string.escape").parent().name());
        assertNull(Scope.of("keyword").parent());
        assertNotEquals(Scope.of("keyword"), Scope.of("keyword.control"));
    }

    /** Les noms mal formés se refusent. */
    @Test(expected = IllegalArgumentException.class)
    public void malformedScopeRejected() {
        Scope.of("comment..doc");
    }

    /** Le thème résout par la portée, puis par parent, puis par défaut. */
    @Test
    public void themeFallsBackByParent() {
        SyntaxTheme theme = SyntaxTheme.of(java.util.Map.of(
                "comment", 0xFF888888,
                "string", 0xFF00AA00), 0xFF111111);
        assertEquals(0xFF888888, theme.colorOf(Scope.of("comment")));
        assertEquals(0xFF888888, theme.colorOf(Scope.of("comment.doc")));
        assertEquals(0xFF00AA00, theme.colorOf(Scope.of("string.escape")));
        assertEquals(0xFF111111, theme.colorOf(Scope.of("number")));
        assertEquals(0xFF111111, theme.colorOf(null));
    }

    /** La couleur par défaut du thème reste accessible. */
    @Test
    public void defaultColorExposed() {
        SyntaxTheme theme = SyntaxTheme.of(java.util.Map.of(), 0xFFABCDEF);
        assertEquals(0xFFABCDEF, theme.defaultColor());
    }
}
