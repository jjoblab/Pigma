package jo.pigma.lsp;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.InitializeResult;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Le protocole complet contre le VRAI serveur Lua (EmmyLua 0.25.1),
 * par processus : l'initialisation négocie, le document part, le
 * survol et la complétion répondent, l'édition se propage par delta,
 * les diagnostics d'un texte erroné remontent, l'arrêt ferme le
 * serveur. Le raccordement qu'un serveur de langage réel demande —
 * celui que le sample branche sur l'appareil (le binaire Android y
 * vit dans nativeLibraryDir, ici c'est le binaire de bureau).
 *
 * <p>Le binaire se cherche par la variable d'environnement
 * {@code EMMYLUA_LS}, sinon l'emplacement du bac à sable. Absent ou
 * non exécutable, le test s'ignore (le build reste vert là où le
 * serveur n'est pas installé).</p>
 */
public class EmmyLuaProcessTest {

    /**
     * Le texte du harnais : une table, une fonction, un appel — de quoi
     * interroger le survol et la complétion sur des symboles réels.
     */
    private static final String DEMO_LUA = """
            -- démo du harnais : le serveur réel analyse ce texte.
            local caisse = {}

            function caisse.total(graine, taille)
              local calcule = graine * taille
              return calcule
            end

            local x = caisse.total(12, 24)
            local somme = ca
            """;

    /** Le texte erroné : un « if » sans « end » — diagnostic attendu. */
    private static final String ERRONE_LUA = """
            if caisse then
              local y = 1
            """;

    /**
     * Le protocole complet sur le processus réel.
     *
     * @throws Exception si le serveur ne répond pas dans le délai
     */
    @Test
    public void realLuaServerSpeaksTheProtocolOverTheProcess() throws Exception {
        File executable = serverExecutable();
        Assume.assumeTrue("binaire emmylua_ls absent (EMMYLUA_LS)", executable.canExecute());
        Path workspace = Files.createTempDirectory("pigma-emmylua-");
        File resources = workspace.resolve("resources").toFile();
        File demo = workspace.resolve("demo.lua").toFile();
        File errone = workspace.resolve("errone.lua").toFile();
        Files.writeString(demo.toPath(), DEMO_LUA, StandardCharsets.UTF_8);
        Files.writeString(errone.toPath(), ERRONE_LUA, StandardCharsets.UTF_8);
        Process process = new ProcessBuilder(executable.getAbsolutePath(),
                "--resources-path", resources.getAbsolutePath())
                .directory(workspace.toFile())
                .redirectError(workspace.resolve("stderr.log").toFile())
                .start();
        BlockingQueue<List<LspDiagnostic>> received = new ArrayBlockingQueue<>(8);
        LspSession session = LspSession.overProcess(process, new LspSession.Listener() {
            @Override
            public void onDiagnostics(String uri, List<LspDiagnostic> diagnostics) {
                received.add(diagnostics);
            }
        });
        try {
            InitializeResult capabilities = session.initialize("pigma-harnais")
                    .get(60, TimeUnit.SECONDS);
            assertNotNull("l'initialisation négocie", capabilities);

            String uri = demo.toURI().toString();
            session.didOpen(uri, DEMO_LUA, "lua");

            // le survol : « caisse » à la ligne 8 (0-based), le serveur y
            // décrit la table locale
            LspHover hover = session.hover(uri, new LspPosition(8, 10))
                    .get(60, TimeUnit.SECONDS);
            assertNotNull("le survol répond", hover);
            assertTrue("le survol parle de caisse : " + hover.value(),
                    hover.value().contains("caisse"));

            // la complétion : au bout de « local somme = ca », la table
            // locale et la bibliothèque standard répondent
            List<LspCompletionItem> completions = session.completion(
                    uri, new LspPosition(9, 15))
                    .get(60, TimeUnit.SECONDS);
            boolean caisseServed = false;
            for (LspCompletionItem candidate : completions) {
                caisseServed |= candidate.label().contains("caisse");
            }
            assertTrue("la complétion propose la table locale : " + completions.size(),
                    caisseServed);

            // l'édition par delta : le document change, le survol suit
            String changed = DEMO_LUA.replace("local caisse = {}",
                    "local caisse = { seuil = 1 }");
            session.didChange(uri, changed);
            LspHover hoverAfterChange = session.hover(uri, new LspPosition(8, 10))
                    .get(60, TimeUnit.SECONDS);
            assertNotNull("le survol suit l'édition", hoverAfterChange);

            // les diagnostics : le texte erroné (un « if » sans « end »)
            // doit publier au moins un diagnostic
            String errorUri = errone.toURI().toString();
            session.didOpen(errorUri, ERRONE_LUA, "lua");
            boolean diagnosticSeen = false;
            for (int at = 0; at < 8 && !diagnosticSeen; at++) {
                List<LspDiagnostic> diagnostics = received.poll(30, TimeUnit.SECONDS);
                if (diagnostics != null && !diagnostics.isEmpty()) {
                    diagnosticSeen = true;
                }
            }
            assertTrue("le texte erroné publie un diagnostic", diagnosticSeen);
        } finally {
            session.shutdown();
        }
        assertTrue("le serveur meurt à l'arrêt",
                process.waitFor(10, TimeUnit.SECONDS));
    }

    /**
     * Le binaire du serveur : la variable d'environnement
     * {@code EMMYLUA_LS} d'abord, l'emplacement du bac à sable ensuite.
     *
     * @return le fichier exécutable, jamais null
     */
    private static File serverExecutable() {
        String explicit = System.getenv("EMMYLUA_LS");
        Path candidate = explicit == null || explicit.isBlank()
                ? Path.of("/home/z/my-project/tools/vendor/emmylua/emmylua_ls")
                : Path.of(explicit.trim());
        return candidate.toFile();
    }
}
