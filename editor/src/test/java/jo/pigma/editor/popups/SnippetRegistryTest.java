package jo.pigma.editor.popups;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * LE REGISTRE DES EXTRAITS aux JVM pures : l'USINE y décale les bornes
 * du trou (les guillemets ôtés, le champ à sa place), les extraits
 * du langage s'y posent, l'inscription remplace le même libellé.
 */
public class SnippetRegistryTest {

    /** L'usine : le trou se décale, les guillemets s'ôtent. */
    @Test
    public void theFactoryParsesTheHole() {
        SnippetRegistry.Snippet forLoop = SnippetRegistry.of("for", "boucle",
                "for «i» = 1, 10 do\nend");
        assertEquals("le corps perd ses guillemets",
                "for i = 1, 10 do\nend", forLoop.body());
        assertEquals("le champ commence au trou", 4, forLoop.fieldAt());
        assertEquals("le champ finit au trou", 5, forLoop.fieldEnd());
        // sans trou : le champ tombe au bout du corps
        SnippetRegistry.Snippet plain = SnippetRegistry.of("x", "rien",
                "local x = ");
        assertEquals("sans trou, le champ tombe au bout",
                "local x = ".length(), plain.fieldAt());
        assertEquals("la fin suit", "local x = ".length(), plain.fieldEnd());
        // le trou mal fermé (guillemet ouvrant seul) : le bout tient lieu
        SnippetRegistry.Snippet broken = SnippetRegistry.of("x", "brisé",
                "local «x");
        assertEquals("le trou non fermé tombe au bout",
                "local «x".length(), broken.fieldAt());
    }

    /** Les extraits des langages de la démonstration s'y posent. */
    @Test
    public void theStandardSnippetsPerLanguage() {
        List<SnippetRegistry.Snippet> java = SnippetRegistry.standard("Java");
        assertTrue("Java porte ses extraits", java.size() >= 5);
        assertEquals("le for ouvre la liste", "for", java.get(0).label());
        assertTrue("le for porte son champ",
                java.get(0).fieldEnd() > java.get(0).fieldAt());
        List<SnippetRegistry.Snippet> lua = SnippetRegistry.standard("Lua");
        assertTrue("Lua porte ses extraits", lua.size() >= 5);
        assertEquals("le for numérique ouvre la liste Lua", "for",
                lua.get(0).label());
        List<SnippetRegistry.Snippet> sql = SnippetRegistry.standard("SQL");
        assertEquals("SQL porte la lecture et l'écriture", 2, sql.size());
        assertTrue("le langage inconnu ne porte rien",
                SnippetRegistry.standard("Xml").isEmpty());
        assertTrue("le langage vide ne porte rien",
                SnippetRegistry.standard(null).isEmpty());
    }

    /** L'inscription remplace le même libellé, la liste vit. */
    @Test
    public void theRegistrationReplacesTheSameLabel() {
        SnippetRegistry registry = new SnippetRegistry();
        assertTrue("le registre part vide", registry.snippets().isEmpty());
        registry.register(SnippetRegistry.of("for", "boucle", "for «i» do\nend"));
        registry.register(SnippetRegistry.of("if", "branche", "if «x» then\nend"));
        assertEquals("deux extraits vivrent", 2, registry.snippets().size());
        registry.register(SnippetRegistry.of("for", "boucle bornée",
                "for «i» = 1, 10 do\nend"));
        assertEquals("le même libellé se remplace (la place demeure)", 2,
                registry.snippets().size());
        assertEquals("la version neuve vit", "for i = 1, 10 do\nend",
                registry.snippets().get(0).body());
        // l'extrait nul ou au libellé vide ne s'inscrit pas
        registry.register(null);
        registry.register(SnippetRegistry.of("", "vide", "rien"));
        assertEquals("l'extrait vide ne s'inscrit pas", 2,
                registry.snippets().size());
        registry.clear();
        assertTrue("le registre se vide", registry.snippets().isEmpty());
    }
}
