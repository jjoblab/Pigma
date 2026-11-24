package jo.pigma.lsp;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Pureté du client : le module reste JVM pur, sans plateforme hôte ni
 * moteur de coloration — un client de protocole ne connaît que le
 * protocole et le tampon qu'il convertit.
 */
public class PurityTest {

    /** Aucune classe du client ne dépend d'Android. */
    @Test
    public void lspNeverDependsOnAndroid() {
        noClasses()
                .that().resideInAPackage("jo.pigma.lsp..")
                .should().dependOnClassesThat().resideInAnyPackage("android..")
                .check(new ClassFileImporter().importPackages("jo.pigma.lsp"));
    }

    /** Le client ne connaît ni le moteur ni l'éditeur : le protocole et le tampon. */
    @Test
    public void lspNeverDependsOnEngineOrEditor() {
        noClasses()
                .that().resideInAPackage("jo.pigma.lsp..")
                .should().dependOnClassesThat().resideInAnyPackage("jo.pigma.syntax..",
                        "jo.pigma.editor..", "jo.pigma.analysis..")
                .check(new ClassFileImporter().importPackages("jo.pigma.lsp"));
    }
}
