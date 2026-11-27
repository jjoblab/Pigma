package jo.pigma.ai;

import com.tngtech.archunit.core.importer.ClassFileImporter;

import org.junit.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Pureté de la couche d'intégration de l'IA : le module reste un
 * module PUR JVM, sans aucune dépendance à la plateforme hôte —
 * Pigma n'y connaît ni Android, ni réseau, ni fournisseur : des
 * types de données et des interfaces, rien d'autre.
 */
public class PurityTest {

    /** Aucune classe de la couche ne dépend d'Android. */
    @Test
    public void aiNeverDependsOnAndroid() {
        noClasses()
                .that().resideInAPackage("jo.pigma.ai..")
                .should().dependOnClassesThat().resideInAnyPackage("android..")
                .check(new ClassFileImporter().importPackages("jo.pigma.ai"));
    }
}
