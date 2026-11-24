package jo.pigma.bench;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jo.pigma.bench.generated.JavaLexer;
import jo.pigma.bench.generated.JsonLexer;
import jo.pigma.bench.generated.LexerApi;
import jo.pigma.bench.generated.XmlLexer;

/**
 * Pilote du banc E0 : exactitude (sortie figee contre fichier de
 * reference), couverture (les jetons couvrent le texte sans trou ni
 * chevauchement), redemarrage (relexer depuis un point de controle en
 * etat initial redonne la suite exacte des jetons) et vitesse (passe
 * unique a JVM froide, passes rechauffees).
 *
 * <p>Les modes : {@code dump} fige la sortie des fichiers de reference
 * (a relire puis a committer), {@code checks} verifie dore, couverture
 * et redemarrage sur les corpus, {@code cold} mesure une passe unique,
 * {@code warm} mesure des passes rechauffees, {@code all} enchaine
 * checks puis warm. Le texte d'un jeton est echappe sur une ligne (les
 * commentaires portent des sauts de ligne). Le code de sortie distingue
 * reussite et echec.</p>
 */
public final class BenchRunner {

    private static final int ERROR_TYPE = 99;
    private static final int INITIAL_STATE = 0;
    private static final int MAX_CHECKPOINTS = 200;
    private static final int WARM_PASSES = 20;

    /** Un jeton reconnu : type, bornes absolues, texte. */
    record Token(int type, int start, int end, String text) {
    }

    /** Un lexage complet : jetons et etat d'entree de chaque jeton. */
    record Lexed(List<Token> tokens, int[] statesBefore) {
    }

    /** Fabrique de lexeur pour un texte donne. */
    interface LexerFactory {
        LexerApi create(String text);
    }

    /** Un langage du banc : son lexeur, ses noms de types, ses fichiers. */
    record Language(String name, LexerFactory factory, String[] typeNames,
                     Path reference, Path corpus, Path golden) {
    }

    private static final String[] JAVA_NAMES = {
        "?", "KEYWORD", "IDENTIFIER", "NUMBER", "STRING", "COMMENT_LINE",
        "COMMENT_BLOCK", "COMMENT_DOC", "OPERATOR", "PUNCT", "WHITESPACE"
    };

    private static final String[] XML_NAMES = {
        "?", "MARKUP", "NAME", "ATTR_VALUE", "COMMENT", "CDATA",
        "DOCTYPE", "ENTITY", "TEXT", "WHITESPACE"
    };

    private static final String[] JSON_NAMES = {
        "?", "PUNCT", "STRING", "NUMBER", "LITERAL", "WHITESPACE"
    };

    private BenchRunner() {
    }

    /**
     * Point d'entree du banc.
     *
     * @param args mode (dump, checks, cold, warm, all) et racine du depot
     * @throws Exception si un fichier manque ou si la lecture echoue
     */
    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        Path root = Path.of(args.length > 1 ? args[1] : ".");
        List<Language> languages = List.of(
                new Language("java",
                        text -> new JavaLexer(new StringReader(text)),
                        JAVA_NAMES,
                        root.resolve("bench/data/java-reference.java"),
                        root.resolve("bench/data/java-2000.java"),
                        root.resolve("bench/data/java-golden.txt")),
                new Language("xml",
                        text -> new XmlLexer(new StringReader(text)),
                        XML_NAMES,
                        root.resolve("bench/data/xml-reference.xml"),
                        root.resolve("bench/data/xml-2000.xml"),
                        root.resolve("bench/data/xml-golden.txt")),
                new Language("json",
                        text -> new JsonLexer(new StringReader(text)),
                        JSON_NAMES,
                        root.resolve("bench/data/json-reference.json"),
                        root.resolve("bench/data/json-2000.json"),
                        root.resolve("bench/data/json-golden.txt")));
        switch (mode) {
            case "dump" -> dumpGoldens(languages);
            case "checks" -> System.exit(runChecks(languages) ? 0 : 1);
            case "cold" -> runCold(languages);
            case "warm" -> runWarm(languages);
            default -> {
                boolean ok = runChecks(languages);
                runWarm(languages);
                if (!ok) {
                    System.exit(1);
                }
            }
        }
    }

    // ------------------------------------------------------------ modes

    private static void dumpGoldens(List<Language> languages) throws IOException {
        for (Language language : languages) {
            String text = Files.readString(language.reference(), StandardCharsets.UTF_8);
            Lexed lexed = lexTokens(language.factory().create(text));
            List<String> lines = new ArrayList<>();
            for (Token token : lexed.tokens()) {
                lines.add(typeName(language.typeNames(), token.type())
                        + "\t" + escape(token.text()));
            }
            Files.write(language.golden(), lines, StandardCharsets.UTF_8);
            System.out.println("FIGE " + language.name() + " "
                    + lexed.tokens().size() + " jetons -> " + language.golden());
        }
    }

    private static boolean runChecks(List<Language> languages) throws IOException {
        boolean ok = true;
        for (Language language : languages) {
            ok &= checkGolden(language);
            ok &= checkCoverage(language);
            ok &= checkRestart(language);
        }
        return ok;
    }

    private static void runCold(List<Language> languages) throws IOException {
        for (Language language : languages) {
            String text = Files.readString(language.corpus(), StandardCharsets.UTF_8);
            long start = System.nanoTime();
            Lexed lexed = lexTokens(language.factory().create(text));
            long elapsed = System.nanoTime() - start;
            System.out.printf("MESURE cold %s %d lignes %d caracteres "
                            + "%d jetons %.1f ms%n",
                    language.name(), countLines(text), text.length(),
                    lexed.tokens().size(), elapsed / 1_000_000.0);
        }
    }

    private static void runWarm(List<Language> languages) throws IOException {
        for (Language language : languages) {
            String text = Files.readString(language.corpus(), StandardCharsets.UTF_8);
            List<Token> tokens = List.of();
            double best = Double.MAX_VALUE;
            for (int pass = 0; pass < WARM_PASSES; pass++) {
                long start = System.nanoTime();
                tokens = lexTokens(language.factory().create(text)).tokens();
                double elapsed = (System.nanoTime() - start) / 1_000_000.0;
                best = Math.min(best, elapsed);
            }
            double megaBytes = text.length() / 1024.0 / 1024.0;
            System.out.printf("MESURE warm %s %d lignes %d caracteres "
                            + "%d jetons %.2f ms/passe (meilleure de %d) "
                            + "%.0f jetons/ms %.1f Mo/s%n",
                    language.name(), countLines(text), text.length(),
                    tokens.size(), best, WARM_PASSES,
                    tokens.size() / best, megaBytes / (best / 1000.0));
        }
    }

    // ------------------------------------------------------- verifications

    private static boolean checkGolden(Language language) throws IOException {
        String text = Files.readString(language.reference(), StandardCharsets.UTF_8);
        Lexed lexed = lexTokens(language.factory().create(text));
        List<String> expected = Files.readAllLines(language.golden(), StandardCharsets.UTF_8);
        List<Token> tokens = lexed.tokens();
        if (tokens.size() != expected.size()) {
            System.out.printf("ECHEC dore %s : %d jetons lus, %d attendus%n",
                    language.name(), tokens.size(), expected.size());
            return false;
        }
        for (int i = 0; i < tokens.size(); i++) {
            String actualLine = typeName(language.typeNames(), tokens.get(i).type())
                    + "\t" + escape(tokens.get(i).text());
            if (!actualLine.equals(expected.get(i))) {
                System.out.printf("ECHEC dore %s au jeton %d : lu [%s], attendu [%s]%n",
                        language.name(), i, actualLine, expected.get(i));
                return false;
            }
        }
        System.out.printf("VERIF dore %s OK (%d jetons)%n",
                language.name(), tokens.size());
        return true;
    }

    private static boolean checkCoverage(Language language) throws IOException {
        String text = Files.readString(language.corpus(), StandardCharsets.UTF_8);
        Lexed lexed = lexTokens(language.factory().create(text));
        List<Token> tokens = lexed.tokens();
        int errors = 0;
        int position = 0;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.type() == ERROR_TYPE) {
                errors++;
            }
            if (token.start() != position) {
                System.out.printf("ECHEC couverture %s au jeton %d : "
                                + "depart %d, attendu %d%n",
                        language.name(), i, token.start(), position);
                return false;
            }
            position = token.end();
        }
        if (position != text.length()) {
            System.out.printf("ECHEC couverture %s : fin %d, texte de %d%n",
                    language.name(), position, text.length());
            return false;
        }
        System.out.printf("VERIF couverture %s OK (%d jetons, %d erreur(s))%n",
                language.name(), tokens.size(), errors);
        return errors == 0;
    }

    private static boolean checkRestart(Language language) throws IOException {
        String text = Files.readString(language.corpus(), StandardCharsets.UTF_8);
        Lexed lexed = lexTokens(language.factory().create(text));
        List<Token> tokens = lexed.tokens();
        int[] statesBefore = lexed.statesBefore();
        List<Integer> checkpoints = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (statesBefore[i] == INITIAL_STATE) {
                checkpoints.add(i);
            }
        }
        int step = Math.max(1, checkpoints.size() / MAX_CHECKPOINTS);
        int checked = 0;
        for (int index = 0; index < checkpoints.size(); index += step) {
            int i = checkpoints.get(index);
            int from = tokens.get(i).start();
            List<Token> tail = lexTokens(
                    language.factory().create(text.substring(from))).tokens();
            if (tail.size() != tokens.size() - i) {
                System.out.printf("ECHEC redemarrage %s au point %d (offset %d) : "
                                + "%d jetons relus, %d attendus%n",
                        language.name(), i, from, tail.size(), tokens.size() - i);
                return false;
            }
            for (int j = 0; j < tail.size(); j++) {
                Token full = tokens.get(i + j);
                Token fresh = tail.get(j);
                if (full.type() != fresh.type() || !full.text().equals(fresh.text())) {
                    System.out.printf("ECHEC redemarrage %s au point %d jeton %d : "
                                    + "relu [%s], attendu [%s]%n",
                            language.name(), i, j, fresh.text(), full.text());
                    return false;
                }
            }
            checked++;
        }
        System.out.printf("VERIF redemarrage %s OK (%d points de controle, "
                        + "%d positions en etat initial)%n",
                language.name(), checked, checkpoints.size());
        return true;
    }

    // ---------------------------------------------------------- utilitaires

    private static Lexed lexTokens(LexerApi lexer) throws IOException {
        List<Token> tokens = new ArrayList<>();
        List<Integer> states = new ArrayList<>();
        int position = 0;
        while (true) {
            int stateBefore = lexer.yystate();
            int type = lexer.yylex();
            if (type == JavaLexer.YYEOF) {
                break;
            }
            int length = lexer.yylength();
            tokens.add(new Token(type, position, position + length, lexer.yytext()));
            states.add(stateBefore);
            position += length;
        }
        int[] statesBefore = new int[states.size()];
        for (int i = 0; i < statesBefore.length; i++) {
            statesBefore[i] = states.get(i);
        }
        return new Lexed(tokens, statesBefore);
    }

    private static String typeName(String[] names, int type) {
        if (type == ERROR_TYPE) {
            return "ERROR";
        }
        return type < names.length ? names[type] : "?";
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }

    private static int countLines(String text) {
        return (int) text.chars().filter(c -> c == '\n').count() + 1;
    }
}
