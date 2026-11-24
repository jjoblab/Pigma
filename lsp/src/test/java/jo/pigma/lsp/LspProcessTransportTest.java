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
}
