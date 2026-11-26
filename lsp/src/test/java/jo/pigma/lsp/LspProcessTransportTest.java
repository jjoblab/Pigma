package jo.pigma.lsp;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Le raccordement par processus : un vrai serveur dans un autre JVM,
 * lsp4j cadre les messages sur l'entrée et la sortie standard du
 * processus — le raccordement qu'un serveur de langage réel demande.
 */
public class LspProcessTransportTest {

    /**
     * Le protocole complet sur le processus : l'initialisation
     * négocie, le survol répond à travers les cadrages, l'arrêt ferme
     * le processus.
     *
     * @throws Exception si le sous-processus ne répond pas dans le délai
     */
    @Test
    public void protocolTravelsThroughARealProcess() throws Exception {
        String classpath = System.getProperty("java.class.path");
        String javaHome = System.getProperty("java.home");
        String java = javaHome + File.separator + "bin" + File.separator + "java";
        Process process = new ProcessBuilder(java, "-cp", classpath,
                "jo.pigma.lsp.TestServerMain").start();
        java.util.concurrent.BlockingQueue<List<LspDiagnostic>> received =
                new java.util.concurrent.ArrayBlockingQueue<>(4);
        LspSession session = LspSession.overProcess(process, new LspSession.Listener() {
            @Override
            public void onDiagnostics(String uri, List<LspDiagnostic> diagnostics) {
                received.add(diagnostics);
            }
        });

        org.eclipse.lsp4j.InitializeResult capabilities = session.initialize("pigma-process-test")
                .get(20, TimeUnit.SECONDS);
        assertEquals(Boolean.TRUE, capabilities.getCapabilities()
                .getHoverProvider().getLeft());

        String hover = session.hover("file:///demo.java", new LspPosition(0, 0))
                .get(20, TimeUnit.SECONDS);
        assertEquals("réponse du sous-processus", hover);

        session.didOpen("file:///demo.java", "class A {}", "java");
        List<LspDiagnostic> diagnostics = received.poll(20, TimeUnit.SECONDS);
        assertEquals("diagnostic du sous-processus", diagnostics.get(0).message());

        session.shutdown();
        assertTrue("le sous-processus meurt à l'arrêt",
                process.waitFor(10, TimeUnit.SECONDS));
    }

    /**
     * Le serveur bavard (D8) : 512 Ko sur l'erreur AVANT d'écouter —
     * sans vidange du tuyau d'erreur côté client, l'écriture s'y
     * bloque et le protocole se fige. Le drain du raccordement laisse
     * le serveur répondre, puis l'arrêt en deux temps le ferme.
     *
     * @throws Exception si le sous-processus ne répond pas dans le délai
     */
    @Test
    public void chattyServerStillServesThroughTheDrainedStderr()
            throws Exception {
        String classpath = System.getProperty("java.class.path");
        String javaHome = System.getProperty("java.home");
        String java = javaHome + File.separator + "bin" + File.separator + "java";
        Process process = new ProcessBuilder(java,
                "-Djo.pigma.test.chatty=true",
                "-cp", classpath, "jo.pigma.lsp.TestServerMain").start();
        LspSession session = LspSession.overProcess(process, null);
        // le bavard a vidé ses 512 Ko : l'initialisation répond quand
        // même (le fil de vidange tient le tuyau libre)
        org.eclipse.lsp4j.InitializeResult capabilities =
                session.initialize("pigma-chatty-test")
                        .get(20, TimeUnit.SECONDS);
        assertEquals(Boolean.TRUE, capabilities.getCapabilities()
                .getHoverProvider().getLeft());
        String hover = session.hover("file:///demo.java", new LspPosition(0, 0))
                .get(20, TimeUnit.SECONDS);
        assertEquals("réponse du sous-processus", hover);
        // l'arrêt en deux temps : la grâce puis le tue-loup, le
        // processus meurt sans destroyForcibly dans le cas ordinaire
        long begin = System.nanoTime();
        session.shutdown();
        assertTrue("le bavard meurt à l'arrêt",
                process.waitFor(10, TimeUnit.SECONDS));
        long stopNanos = System.nanoTime() - begin;
        assertTrue("l'arrêt se borne (la grâce de 500 ms puis le tue-loup)",
                stopNanos < TimeUnit.SECONDS.toNanos(3));
    }
}
