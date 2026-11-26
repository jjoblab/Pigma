package jo.pigma.lsp;

import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;

/**
 * Ce que le serveur déclare savoir à l'initialisation : une fonction
 * par fournisseur (le survol, la complétion, la définition, le
 * renommage…). La façade y lit ce qu'elle peut brancher — demander
 * une fonction que le serveur n'a pas déclarée y offense le protocole
 * (la leçon de l'éditeur de référence : les capacités y
 * décident aussi — le formateur ne se pose que sur la déclaration du
 * serveur).
 *
 * @param hover             le survol
 * @param completion        la complétion
 * @param completionResolve la résolution des candidats de complétion
 *                          (la documentation à la sélection)
 * @param signatureHelp     l'aide aux paramètres
 * @param definition        l'aller-à la définition
 * @param references        les références du symbole
 * @param prepareRename     la préparation du renommage (la plage
 *                          renommable)
 * @param rename            le renommage du symbole
 * @param codeAction        les corrections rapides
 * @param documentHighlight les occurrences du symbole
 * @param formatting        le formatage du document
 * @param documentSymbol    les symboles du document (la table des
 *                          matières)
 * @param foldingRange      les plis du serveur
 * @param inlayHint         les indices posés dans le texte
 * @param semanticTokens    les jetons sémantiques (la coloration du
 *                          serveur)
 */
public record LspCapabilities(boolean hover, boolean completion,
        boolean completionResolve, boolean signatureHelp,
        boolean definition, boolean references, boolean prepareRename, boolean rename,
        boolean codeAction, boolean documentHighlight, boolean formatting,
        boolean documentSymbol, boolean foldingRange, boolean inlayHint,
        boolean semanticTokens) {

    /** Le serveur qui n'a rien déclaré (la liaison sans initialisation). */
    public static final LspCapabilities NONE = new LspCapabilities(
            false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false);

    /**
     * Les capacités de la réponse d'initialisation : chaque
     * fournisseur déclaré vaut vrai (la valeur nue ou les options —
     * les deux formes du protocole ; l'absence vaut non).
     *
     * @param result la réponse du protocole, null pour rien
     * @return les capacités du serveur
     */
    public static LspCapabilities fromResult(InitializeResult result) {
        if (result == null || result.getCapabilities() == null) {
            return NONE;
        }
        return fromLsp(result.getCapabilities());
    }

    /**
     * Les capacités d'un objet de capacités lsp4j.
     *
     * @param capabilities les capacités du protocole
     * @return les capacités du domaine
     */
    public static LspCapabilities fromLsp(ServerCapabilities capabilities) {
        boolean renameDeclared = declared(capabilities.getRenameProvider());
        Boolean prepareDeclared = renameDeclared
                && capabilities.getRenameProvider().getRight() != null
                && Boolean.TRUE.equals(
                        capabilities.getRenameProvider().getRight().getPrepareProvider())
                ? Boolean.TRUE : Boolean.FALSE;
        return new LspCapabilities(
                declared(capabilities.getHoverProvider()),
                capabilities.getCompletionProvider() != null,
                capabilities.getCompletionProvider() != null
                        && Boolean.TRUE.equals(
                                capabilities.getCompletionProvider().getResolveProvider()),
                capabilities.getSignatureHelpProvider() != null,
                declared(capabilities.getDefinitionProvider()),
                declared(capabilities.getReferencesProvider()),
                prepareDeclared,
                renameDeclared,
                declared(capabilities.getCodeActionProvider()),
                declared(capabilities.getDocumentHighlightProvider()),
                declared(capabilities.getDocumentFormattingProvider()),
                declared(capabilities.getDocumentSymbolProvider()),
                declared(capabilities.getFoldingRangeProvider()),
                declared(capabilities.getInlayHintProvider()),
                capabilities.getSemanticTokensProvider() != null);
    }

    /**
     * Le fournisseur déclaré : la valeur nue vraie ou les options
     * (les deux formes du protocole) ; l'absence ou la valeur fausse
     * vaut non.
     *
     * @param provider la déclaration du protocole
     * @return vrai quand le serveur sait faire
     */
    private static boolean declared(Either<Boolean, ?> provider) {
        return provider != null && (Boolean.TRUE.equals(provider.getLeft())
                || provider.getRight() != null);
    }
}
