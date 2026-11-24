package jo.pigma.syntax;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Pureté du moteur de coloration : le module reste JVM pur, sans
 * dépendance à la plateforme hôte — la couleur se décide en portées,
 * le dessin vit dans la vue.
 */
public class PurityTest {

    /** Aucune classe du moteur ne dépend d'Android. */
    @Test
    public void syntaxNeverDependsOnAndroid() {
        noClasses()
                .that().resideInAPackage("jo.pigma.syntax..")
                .should().dependOnClassesThat().resideInAnyPackage("android..")
                .check(new ClassFileImporter().importPackages("jo.pigma.syntax"));
    }
}
