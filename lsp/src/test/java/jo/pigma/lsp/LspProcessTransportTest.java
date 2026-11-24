package jo.pigma.lsp;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Le transport par processus : un vrai serveur dans un autre JVM, les
 * messages cadrés par {@code Content-Length} sur l'entrée et la sortie
 * standard — le raccordement qu'un serveur de langage réel demande.
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
        LspTransport.ProcessTransport transport =
                new LspTransport.ProcessTransport(List.of(java, "-cp", classpath,
                        "jo.pigma.lsp.TestServerMain"));
        LspSession session = new LspSession(transport, null);
        session.start();

        JsonObject capabilities = session.initialize("pigma-process-test")
                .get(20, TimeUnit.SECONDS);
        assertEquals(Boolean.TRUE, capabilities.getObject("capabilities")
                .getBoolean("hoverProvider"));

        String hover = session.hover("file:///demo.java", new LspPosition(0, 0))
                .get(20, TimeUnit.SECONDS);
        assertEquals("réponse du sous-processus", hover);

        session.shutdown();
        assertTrue("le sous-processus meurt à l'arrêt",
                transport.process().waitFor(10, TimeUnit.SECONDS));
    }
}
