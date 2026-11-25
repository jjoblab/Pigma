package jo.pigma.lsp;

import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureInformation;

/**
 * L'aide aux paramètres rapportée par un serveur : la ligne de la
 * signature active (le nom appelé, ses paramètres entre parenthèses)
 * et l'index du paramètre actif (celui que la frappe remplit — les
 * virgules le déplacent). L'adaptation du type lsp4j garde la
 * signature ACTIVE seule (le serveur peut en proposer plusieurs, les
 * surcharges ; l'active est celle qu'il désigne, la première sans
 * désignation) et borne son paramètre actif à zéro.
 *
 * @param label           la ligne de la signature active
 * @param activeParameter l'index du paramètre actif (0 : le premier)
 */
public record LspSignatureHelp(String label, int activeParameter) {

    /**
     * L'adaptation d'une réponse du protocole.
     *
     * @param help la réponse du serveur (null : le serveur ne sait pas)
     * @return la signature active avec son paramètre, null si le
     *         serveur n'en porte aucune
     */
    public static LspSignatureHelp fromLsp(SignatureHelp help) {
        if (help == null || help.getSignatures() == null
                || help.getSignatures().isEmpty()) {
            return null;
        }
        int activeSignature = help.getActiveSignature() == null
                ? 0 : help.getActiveSignature();
        if (activeSignature < 0 || activeSignature >= help.getSignatures().size()) {
            activeSignature = 0;
        }
        SignatureInformation signature = help.getSignatures().get(activeSignature);
        if (signature == null || signature.getLabel() == null
                || signature.getLabel().isEmpty()) {
            return null;
        }
        int activeParameter = help.getActiveParameter() == null
                ? 0 : Math.max(0, help.getActiveParameter());
        return new LspSignatureHelp(signature.getLabel(), activeParameter);
    }
}
