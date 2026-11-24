package jo.pigma.core;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Pureté du cœur : le modèle de texte reste un module pur JVM, sans aucune
 * dépendance à la plateforme hôte. Cette règle est la première ligne de
 * défense du découpage : la vue, la saisie et le rendu vivent ailleurs.
 */
public class PurityTest {

    /** Aucune classe du cœur ne dépend d'Android. */
    @Test
    public void coreNeverDependsOnAndroid() {
        noClasses()
                .that().resideInAPackage("jo.pigma.core..")
                .should().dependOnClassesThat().resideInAnyPackage("android..")
                .check(new ClassFileImporter().importPackages("jo.pigma.core"));
    }
}
