package android.content;

/**
 * Information d'application du stub : le répertoire des bibliothèques
 * natives (le serveur hors processus du sample s'y embarque sur
 * l'appareil — au harnais, il pointe sur un répertoire vide, le
 * bouton y répond « absent » sans lancer quoi que ce soit).
 */
public class ApplicationInfo {

    /** Le répertoire des bibliothèques natives extraites du paquetage. */
    public String nativeLibraryDir = System.getProperty("java.io.tmpdir", "/tmp")
            + "/pigma-harness-native";
}
