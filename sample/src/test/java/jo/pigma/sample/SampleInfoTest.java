package jo.pigma.sample;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Tests des informations affichées par l'application de démonstration. */
public class SampleInfoTest {

    /** Le texte d'accueil porte le nom du projet. */
    @Test
    public void welcomeTextStartsWithProjectName() {
        assertTrue(SampleInfo.welcomeText().startsWith(SampleInfo.PROJECT_NAME));
    }

    /** Le texte d'accueil n'est jamais vide. */
    @Test
    public void welcomeTextIsNotEmpty() {
        assertFalse(SampleInfo.welcomeText().isEmpty());
    }
}
