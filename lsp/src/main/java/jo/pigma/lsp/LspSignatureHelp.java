package jo.pigma.lsp;

import org.eclipse.lsp4j.ParameterInformation;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Tuple.Two;
import org.eclipse.lsp4j.MarkupContent;

import java.util.ArrayList;
import java.util.List;

/**
 * L'aide aux paramètres du serveur, AVEC SES SURCHARGES : le défaut
 * du rapport des popups — l'adaptation d'avant gardait la signature
 * ACTIVE seule, les surcharges du serveur mouraient en chemin (une
 * rangée par surcharge devait vivre, le sélecteur « 1 sur 3 » avec).
 * Le domaine porte TOUTES les signatures, chacune avec sa
 * documentation et ses paramètres documentés ; l'index de la
 * signature active et du paramètre actif s'y ajoutent (la frappe les
 * déplace — les virgules font avancer le paramètre, le changement
 * d'appel peut changer la signature active).
 *
 * @param signatures       les surcharges, au moins une
 * @param activeSignature   l'index de la signature active (0 sans désignation)
 * @param activeParameter   l'index du paramètre actif (0 sans désignation)
 */
public record LspSignatureHelp(List<LspSignature> signatures,
        int activeSignature, int activeParameter) {

    /**
     * Une surcharge : sa ligne de signature (le nom appelé, ses
     * paramètres entre parenthèses), sa documentation, ses paramètres
     * — chacun avec sa ligne et sa documentation propre.
     *
     * @param label         la ligne de la signature
     * @param documentation la documentation de la signature (vide pour aucune)
     * @param parameters    les paramètres, chacun avec sa documentation
     */
    public record LspSignature(String label, String documentation,
            List<LspParameter> parameters) {
    }

    /**
     * Un paramètre de signature : sa ligne (le type et le nom) et sa
     * documentation propre (le serveur la porte souvent — la carte
     * la montre sur le paramètre ACTIF).
     *
     * @param label         la ligne du paramètre
     * @param documentation la documentation du paramètre (vide pour aucune)
     */
    public record LspParameter(String label, String documentation) {
    }

    /**
     * L'adaptation d'une réponse du protocole : les surcharges ENTIERES
     * y rentrent (la documentation de chacune, les paramètres et la
     * leur), l'index de la signature active se BORNE à la liste (le
     * serveur peut désigner hors bornes — la première y vit, jamais
     * l'effondrement), le paramètre actif ne descend jamais sous zéro.
     *
     * @param help la réponse du serveur (null : le serveur ne sait pas)
     * @return l'aide complète, null si le serveur n'en porte aucune
     */
    public static LspSignatureHelp fromLsp(SignatureHelp help) {
        if (help == null || help.getSignatures() == null
                || help.getSignatures().isEmpty()) {
            return null;
        }
        List<LspSignature> signatures = new ArrayList<>();
        for (SignatureInformation information : help.getSignatures()) {
            if (information == null) {
                continue;
            }
            signatures.add(signatureOf(information));
        }
        if (signatures.isEmpty()) {
            return null;
        }
        int activeSignature = help.getActiveSignature() == null ? 0
                : help.getActiveSignature();
        if (activeSignature < 0 || activeSignature >= signatures.size()) {
            activeSignature = 0;
        }
        int activeParameter = help.getActiveParameter() == null ? 0
                : Math.max(0, help.getActiveParameter());
        return new LspSignatureHelp(signatures, activeSignature, activeParameter);
    }

    /** L'adaptation d'une signature du protocole. */
    private static LspSignature signatureOf(SignatureInformation information) {
        String label = information.getLabel() == null ? "" : information.getLabel();
        String documentation = documentationText(information.getDocumentation());
        List<LspParameter> parameters = new ArrayList<>();
        if (information.getParameters() != null) {
            for (ParameterInformation parameter : information.getParameters()) {
                if (parameter == null) {
                    continue;
                }
                String parameterLabel = parameterLabel(parameter, label);
                parameters.add(new LspParameter(parameterLabel,
                        documentationText(parameter.getDocumentation())));
            }
        }
        return new LspSignature(label, documentation, List.copyOf(parameters));
    }

    /**
     * La ligne d'un paramètre : les deux formes du protocole s'y
     * lisent (la chaîne nue, la paire de décalages dans la ligne de
     * signature — la coupe s'y extrait, bornée à la ligne).
     */
    private static String parameterLabel(ParameterInformation parameter,
            String signatureLabel) {
        Either<String, Two<Integer, Integer>> label = parameter.getLabel();
        if (label == null) {
            return "";
        }
        if (label.isLeft()) {
            return label.getLeft() == null ? "" : label.getLeft();
        }
        Two<Integer, Integer> offsets = label.getRight();
        if (offsets == null || offsets.getFirst() == null
                || offsets.getSecond() == null) {
            return "";
        }
        int from = Math.max(0, Math.min(offsets.getFirst(), signatureLabel.length()));
        int to = Math.max(from, Math.min(offsets.getSecond(), signatureLabel.length()));
        return signatureLabel.substring(from, to);
    }

    /** Le texte d'une documentation du protocole (nue ou balisée). */
    private static String documentationText(
            Either<String, MarkupContent> documentation) {
        if (documentation == null) {
            return "";
        }
        if (documentation.isLeft()) {
            return documentation.getLeft() == null ? "" : documentation.getLeft();
        }
        return documentation.getRight() == null
                || documentation.getRight().getValue() == null
                ? "" : documentation.getRight().getValue();
    }
}
