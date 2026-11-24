package jo.pigma.bench;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jcodings.specific.UTF8Encoding;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Syntax;
import org.joni.WarnCallback;

/**
 * Profil du chargement de grammaires du depot de reference : pour
 * chaque grammaire TextMate, mesure l'analyse JSON (meme moteur que le
 * chargeur) puis la compilation joni de chaque motif (meme options que
 * la fabrique d'expressions du chargeur). Ce sont les deux couts
 * dominants du chargement d'une grammaire ; le tissage des regles
 * (resolution des inclusions, moisson des mots reserves) s'y ajoute —
 * la mesure minore donc le cout reel.
 *
 * <p>Deux passes : la premiere a JIT froide (le cas du telephone au
 * premier chargement), la seconde rechauffee. Les motifs que la
 * compilation directe refuse (fermetures a references, formes
 * pre-traitees par le chargeur reel) se comptent a part.</p>
 */
public final class ProfileGrammars {

    private static final List<String> PATTERN_KEYS = List.of("match", "begin", "end", "while");

    private ProfileGrammars() {
    }

    /**
     * Point d'entree : profil de toutes les grammaires sous les
     * racines donnees.
     *
     * @param args racines a parcourir (chaque grammar.json trouve)
     * @throws IOException si un fichier ne se lit pas
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("usage : ProfileGrammars racine [racine...]");
            return;
        }
        List<Path> files = new ArrayList<>();
        for (String root : args) {
            files.addAll(findGrammars(Path.of(root)));
        }
        files.sort(Path::compareTo);
        runPass(files, 1);
        runPass(files, 2);
    }

    private static void runPass(List<Path> files, int pass) {
        System.out.println("== passe " + pass + (pass == 1 ? " (JIT froide)" : " (rechauffee)")
                + " ==");
        long totalParseNanos = 0;
        long totalCompileNanos = 0;
        long totalPatterns = 0;
        long totalFailures = 0;
        long totalBytes = 0;
        for (Path file : files) {
            String text;
            try {
                text = Files.readString(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.out.println("ILLISIBLE " + file);
                continue;
            }
            long parseStart = System.nanoTime();
            JsonObject tree = JsonParser.parseString(text).getAsJsonObject();
            long parseNanos = System.nanoTime() - parseStart;
            List<String> patterns = new ArrayList<>();
            collect(tree, patterns);
            long compileStart = System.nanoTime();
            int failures = 0;
            for (String pattern : patterns) {
                if (!compile(pattern)) {
                    failures++;
                }
            }
            long compileNanos = System.nanoTime() - compileStart;
            totalParseNanos += parseNanos;
            totalCompileNanos += compileNanos;
            totalPatterns += patterns.size();
            totalFailures += failures;
            totalBytes += text.length();
            String label = file.getFileName().toString().endsWith(".tmLanguage.json")
                    ? file.getFileName().toString()
                    : file.getParent().getFileName().toString();
            System.out.printf("GRAMMAIRE %-24s %8d octets %4d motifs %3d refus "
                            + "parse %6.1f ms compile %7.1f ms%n",
                    label, text.length(), patterns.size(),
                    failures, parseNanos / 1_000_000.0, compileNanos / 1_000_000.0);
        }
        System.out.printf("TOTAL %d grammaires %d octets %d motifs %d refus "
                        + "parse %.0f ms compile %.0f ms%n",
                files.size(), totalBytes, totalPatterns, totalFailures,
                totalParseNanos / 1_000_000.0, totalCompileNanos / 1_000_000.0);
    }

    private static boolean compile(String pattern) {
        try {
            byte[] bytes = pattern.getBytes(StandardCharsets.UTF_8);
            new Regex(bytes, 0, bytes.length, Option.CAPTURE_GROUP,
                    UTF8Encoding.INSTANCE, Syntax.DEFAULT, WarnCallback.NONE);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static void collect(JsonElement element, List<String> patterns) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (String key : PATTERN_KEYS) {
                JsonElement value = object.get(key);
                if (value != null && value.isJsonPrimitive()) {
                    patterns.add(value.getAsString());
                }
            }
            for (java.util.Map.Entry<String, JsonElement> entry : object.entrySet()) {
                collect(entry.getValue(), patterns);
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                collect(child, patterns);
            }
        }
    }

    private static List<Path> findGrammars(Path root) throws IOException {
        List<Path> found = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return found;
        }
        try (var stream = Files.walk(root, 4)) {
            stream.filter(p -> {
                String name = p.getFileName() == null ? "" : p.getFileName().toString();
                return name.equals("grammar.json") || name.endsWith(".tmLanguage.json");
            }).forEach(found::add);
        }
        return found;
    }
}
