package jo.pigma.analysis;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Pureté de l'annotateur : L2 et L3 restent des modules purs JVM, sans
 * aucune dépendance à la plateforme hôte — la fusion au dessin vit dans
 * la vue, jamais ici.
 */
public class PurityTest {

    /** Aucune classe de l'annotateur ne dépend d'Android. */
    @Test
    public void analysisNeverDependsOnAndroid() {
        noClasses()
                .that().resideInAPackage("jo.pigma.analysis..")
                .should().dependOnClassesThat().resideInAnyPackage("android..")
                .check(new ClassFileImporter().importPackages("jo.pigma.analysis"));
    }
}
