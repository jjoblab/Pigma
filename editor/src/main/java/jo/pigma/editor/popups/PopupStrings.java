package jo.pigma.editor.popups;

import java.util.Locale;

/**
 * LES CHAÎNES DES POPUPS (D-P8 du rapport des popups — « libellés et
 * chaînes codés en dur en français ») : la source unique des
 * libellés que les popups portent à l'écran — les titres des cartes,
 * les boutons, les étages du débordement, les entrées de l'ampoule,
 * les compteurs. L'HÔTE la pose ({@code setPopupStrings} de la vue —
 * les ressources de l'application, la locale du système) ; les
 * défauts portent le français actuel : sans consigne de l'hôte, RIEN
 * ne change à l'écran.
 *
 * <p>La classe est IMMUABLE : le bâtisseur pose tout, puis {@link
 * #build()} rend les chaînes figées — une carte ne les relit jamais à
 * moitié. Les compteurs s'y composent ({@link #targetsCount(int)},
 * {@link #resultsCount(int)}…) : le nombre porte, le nom suit — le
 * singulier y vit aussi (« 1 cible », « 2 cibles »).</p>
 *
 * <p>LES CHAÎNES DE L'ASSISTANT (B2) y vivent aussi : les entrées de
 * l'IA de l'hôte (« Expliquer », « Corriger avec l'assistant »…),
 * l'attente annulable, une phrase par famille de panne — la vue ne
 * connaît aucune IA, elle y lit ses libellés comme toute carte.</p>
 */
public final class PopupStrings {

    // LES TITRES des cartes

    /** Le titre de la carte de structure. */
    private final String structure;
    /** Le titre de la carte des problèmes. */
    private final String problems;
    /** Le titre de la carte des récents. */
    private final String recents;
    /** Le titre de la carte de l'aller. */
    private final String gotoTitle;
    /** Le titre de la carte d'entourage. */
    private final String surround;
    /** Le titre de la carte des extraits. */
    private final String snippets;
    /** Le titre de la carte du presse-papiers. */
    private final String clipboard;
    /** Le titre de la carte de l'historique local. */
    private final String history;

    // LES MOTS des cartes (les boutons, les familles, les lignes)

    /** Le bouton « Ouvrir » de l'aperçu de définition. */
    private final String open;
    /** Le bouton « Appliquer » de l'aperçu du renommage. */
    private final String apply;
    /** L'étiquette de la famille des fichiers (les récents). */
    private final String filesFamily;
    /** L'étiquette de la famille des emplacements (les récents). */
    private final String placesFamily;
    /** L'étiquette de la famille des erreurs (les problèmes). */
    private final String errorsFamily;
    /** L'étiquette de la famille des avertissements. */
    private final String warningsFamily;
    /** L'étiquette de la famille des informations. */
    private final String infosFamily;
    /** Le mot « ligne » (la place d'une rangée). */
    private final String lineWord;
    /** Le nom compté d'une cible. */
    private final String targetWord;
    /** Le nom compté d'un résultat de l'aller. */
    private final String resultWord;
    /** Le nom compté d'un candidat d'import. */
    private final String candidateWord;
    /** Le nom compté d'une ligne de l'aperçu. */
    private final String bodyLineWord;
    /** Le nom compté d'un patron d'entourage. */
    private final String patternWord;
    /** Le nom compté d'un extrait. */
    private final String snippetWord;
    /** Le nom compté d'un fichier (les récents). */
    private final String fileWord;
    /** Le nom compté d'un emplacement (les récents). */
    private final String placeWord;
    /** Le nom compté d'un symbole (la structure). */
    private final String symbolWord;
    /** Le nom compté d'une copie (le presse-papiers). */
    private final String copyWord;
    /** Le titre des actions rapides (la carte de l'ampoule). */
    private final String quickActions;
    /** Le titre des diagnostics (la carte de la ligne). */
    private final String diagnostics;
    /** Le titre des usages (la carte des occurrences). */
    private final String usages;
    /** L'invite de la barre d'aller à la ligne. */
    private final String gotoBarInvite;

    // LES ÉTAGES du débordement

    /** L'étage de navigation. */
    private final String navigation;
    /** L'étage d'édition. */
    private final String edition;
    /** L'étage du presse-papiers. */
    private final String clipboardFamily;
    /** L'étage de l'historique de la sélection. */
    private final String historyFamily;
    /** L'étage des actions de l'hôte. */
    private final String hostFamily;

    // LES RANGÉES du débordement

    /** La rangée « Aller à la ligne… ». */
    private final String gotoLineRow;
    /** La rangée « Rechercher… ». */
    private final String searchRow;
    /** La rangée « Liste des problèmes… ». */
    private final String problemsRow;
    /** La rangée « Aller au symbole… ». */
    private final String gotoSymbolRow;
    /** La rangée « Fichiers récents… ». */
    private final String recentFilesRow;
    /** La rangée « Couper ». */
    private final String cutRow;
    /** La rangée « Tout sélectionner ». */
    private final String selectAllRow;
    /** La rangée « Quitter la sélection ». */
    private final String quitSelectionRow;
    /** La rangée « Historique du presse-papiers… ». */
    private final String clipboardHistoryRow;
    /** La rangée « Historique de la sélection ». */
    private final String selectionHistoryRow;
    /** La rangée « Entourer de… ». */
    private final String surroundRow;
    /** La rangée « Insérer un extrait… ». */
    private final String snippetsRow;

    // LES ENTRÉES de l'ampoule du serveur (la façade)

    /** L'entrée « Aller à la définition ». */
    private final String definitionEntry;
    /** L'entrée « Aperçu de la définition ». */
    private final String definitionPreviewEntry;
    /** L'entrée « Trouver les usages ». */
    private final String referencesEntry;
    /** L'entrée « Renommer le symbole ». */
    private final String renameEntry;
    /** L'entrée « Formater le document ». */
    private final String formatEntry;
    /** L'entrée « Les symboles du document ». */
    private final String symbolsEntry;
    /** Le préfixe « Correctif : » des correctifs du serveur. */
    private final String fixPrefix;

    // LES CHAÎNES DE L'ASSISTANT (B2 — l'IA de l'hôte dans la vue)

    /** Le titre de la carte de l'assistant (ses réponses s'y lisent). */
    private final String assistant;
    /** Le corps de l'ATTENTE (la réponse vole). */
    private final String aiThinking;
    /** L'étiquette du bouton ANNULER (la réponse en vol). */
    private final String aiCancel;
    /** L'entrée « Expliquer » (l'ampoule). */
    private final String aiExplain;
    /** L'entrée « Expliquer l'erreur » (l'étage des correctifs). */
    private final String aiExplainError;
    /** L'entrée « Corriger avec l'assistant » (l'étage des correctifs). */
    private final String aiFix;
    /** L'entrée « Documenter » (l'ampoule). */
    private final String aiDocument;
    /** L'entrée « Écrire le test » (l'ampoule). */
    private final String aiWriteTest;
    /** La phrase de la panne INDISPONIBLE. */
    private final String aiUnavailable;
    /** La phrase de la panne ANNULÉE. */
    private final String aiCancelled;
    /** La phrase de la panne DÉLAI. */
    private final String aiTimeout;
    /** La phrase de la panne REFUSÉE. */
    private final String aiRefused;
    /** La phrase de la panne INTERNE. */
    private final String aiInternal;
    /** La phrase du texte CHANGÉ depuis la demande (la réponse ne se pose pas). */
    private final String aiStale;
    /** La phrase de la réponse MUETTE (vide). */
    private final String aiEmpty;

    // LES CHAÎNES DE LA MODIFICATION PAR INSTRUCTION (B3 — l'IA écrit
    // à la sélection)

    /** La pastille « Modifier avec l'IA… » (la barre de sélection). */
    private final String aiEditSelection;
    /** L'invite de la barre d'instruction. */
    private final String aiEditInvite;
    /** L'indication du champ vierge de la barre. */
    private final String aiEditHint;
    /** Le retour d'une instruction vide (la validation sans texte). */
    private final String aiEditEmpty;
    /** Le titre de l'aperçu de la modification proposée. */
    private final String aiDiffTitle;
    /** L'étiquette de la section « Avant » de l'aperçu. */
    private final String aiDiffBefore;
    /** L'étiquette de la section « Après » de l'aperçu. */
    private final String aiDiffAfter;
    /** Le bouton « Accepter » de l'aperçu. */
    private final String aiDiffAccept;
    /** Le bouton « Rejeter » de l'aperçu. */
    private final String aiDiffReject;
    /** Le bouton « Réessayer » de l'aperçu. */
    private final String aiDiffRetry;
    /** La phrase des erreurs NÉES de la modification posée. */
    private final String aiNewErrors;
    /** L'annonce de la suggestion disponible (le fantôme au caret). */
    private final String aiGhostReady;
    /** L'annonce d'ouverture de la carte de l'assistant (sa propre
     * voix — pas celle de la documentation). */
    private final String aiAnnounced;

    private PopupStrings(Builder builder) {
        structure = builder.structure;
        problems = builder.problems;
        recents = builder.recents;
        gotoTitle = builder.gotoTitle;
        surround = builder.surround;
        snippets = builder.snippets;
        clipboard = builder.clipboard;
        history = builder.history;
        open = builder.open;
        apply = builder.apply;
        filesFamily = builder.filesFamily;
        placesFamily = builder.placesFamily;
        errorsFamily = builder.errorsFamily;
        warningsFamily = builder.warningsFamily;
        infosFamily = builder.infosFamily;
        lineWord = builder.lineWord;
        targetWord = builder.targetWord;
        resultWord = builder.resultWord;
        candidateWord = builder.candidateWord;
        bodyLineWord = builder.bodyLineWord;
        patternWord = builder.patternWord;
        snippetWord = builder.snippetWord;
        fileWord = builder.fileWord;
        placeWord = builder.placeWord;
        symbolWord = builder.symbolWord;
        copyWord = builder.copyWord;
        quickActions = builder.quickActions;
        diagnostics = builder.diagnostics;
        usages = builder.usages;
        gotoBarInvite = builder.gotoBarInvite;
        navigation = builder.navigation;
        edition = builder.edition;
        clipboardFamily = builder.clipboardFamily;
        historyFamily = builder.historyFamily;
        hostFamily = builder.hostFamily;
        gotoLineRow = builder.gotoLineRow;
        searchRow = builder.searchRow;
        problemsRow = builder.problemsRow;
        gotoSymbolRow = builder.gotoSymbolRow;
        recentFilesRow = builder.recentFilesRow;
        cutRow = builder.cutRow;
        selectAllRow = builder.selectAllRow;
        quitSelectionRow = builder.quitSelectionRow;
        clipboardHistoryRow = builder.clipboardHistoryRow;
        selectionHistoryRow = builder.selectionHistoryRow;
        surroundRow = builder.surroundRow;
        snippetsRow = builder.snippetsRow;
        definitionEntry = builder.definitionEntry;
        definitionPreviewEntry = builder.definitionPreviewEntry;
        referencesEntry = builder.referencesEntry;
        renameEntry = builder.renameEntry;
        formatEntry = builder.formatEntry;
        symbolsEntry = builder.symbolsEntry;
        fixPrefix = builder.fixPrefix;
        assistant = builder.assistant;
        aiThinking = builder.aiThinking;
        aiCancel = builder.aiCancel;
        aiExplain = builder.aiExplain;
        aiExplainError = builder.aiExplainError;
        aiFix = builder.aiFix;
        aiDocument = builder.aiDocument;
        aiWriteTest = builder.aiWriteTest;
        aiUnavailable = builder.aiUnavailable;
        aiCancelled = builder.aiCancelled;
        aiTimeout = builder.aiTimeout;
        aiRefused = builder.aiRefused;
        aiInternal = builder.aiInternal;
        aiStale = builder.aiStale;
        aiEmpty = builder.aiEmpty;
        aiEditSelection = builder.aiEditSelection;
        aiEditInvite = builder.aiEditInvite;
        aiEditHint = builder.aiEditHint;
        aiEditEmpty = builder.aiEditEmpty;
        aiDiffTitle = builder.aiDiffTitle;
        aiDiffBefore = builder.aiDiffBefore;
        aiDiffAfter = builder.aiDiffAfter;
        aiDiffAccept = builder.aiDiffAccept;
        aiDiffReject = builder.aiDiffReject;
        aiDiffRetry = builder.aiDiffRetry;
        aiNewErrors = builder.aiNewErrors;
        aiGhostReady = builder.aiGhostReady;
        aiAnnounced = builder.aiAnnounced;
    }

    /**
     * Les chaînes par défaut : le français courant — l'hôte qui n'en
     * pose pas ne voit RIEN changer à l'écran.
     *
     * @return les chaînes par défaut, jamais null
     */
    public static PopupStrings defaults() {
        return builder().build();
    }

    /**
     * Un bâtisseur neuf : tout y part des valeurs par défaut, l'hôte
     * n'y change que ce qu'il veut.
     *
     * @return le bâtisseur des chaînes
     */
    public static Builder builder() {
        return new Builder();
    }

    // LES TITRES

    /** Le titre de la carte de structure. */
    public String structure() {
        return structure;
    }

    /** Le titre de la carte des problèmes. */
    public String problems() {
        return problems;
    }

    /** Le titre de la carte des récents. */
    public String recents() {
        return recents;
    }

    /** Le titre de la carte de l'aller. */
    public String gotoTitle() {
        return gotoTitle;
    }

    /** Le titre de la carte d'entourage. */
    public String surround() {
        return surround;
    }

    /** Le titre de la carte des extraits. */
    public String snippets() {
        return snippets;
    }

    /** Le titre de la carte du presse-papiers. */
    public String clipboard() {
        return clipboard;
    }

    /** Le titre de la carte de l'historique local. */
    public String history() {
        return history;
    }

    // LES MOTS

    /** Le bouton « Ouvrir » de l'aperçu de définition. */
    public String open() {
        return open;
    }

    /** Le bouton « Appliquer » de l'aperçu du renommage. */
    public String apply() {
        return apply;
    }

    /** L'étiquette de la famille des fichiers (les récents). */
    public String filesFamily() {
        return filesFamily;
    }

    /** L'étiquette de la famille des emplacements (les récents). */
    public String placesFamily() {
        return placesFamily;
    }

    /** L'étiquette de la famille des erreurs (les problèmes). */
    public String errorsFamily() {
        return errorsFamily;
    }

    /** L'étiquette de la famille des avertissements. */
    public String warningsFamily() {
        return warningsFamily;
    }

    /** L'étiquette de la famille des informations. */
    public String infosFamily() {
        return infosFamily;
    }

    /** La place d'une rangée : « ligne N ». */
    public String lineAt(int index) {
        return lineWord + " " + (index + 1);
    }

    /** Le titre de l'aperçu : « fichier · ligne N ». */
    public String fileLineAt(String file, int index) {
        return (file == null || file.isEmpty() ? "" : file + " · ")
                + lineWord + " " + (index + 1);
    }

    /** Le compte des cibles (« 1 cible », « 2 cibles »). */
    public String targetsCount(int count) {
        return count + " " + targetWord + (count > 1 ? "s" : "");
    }

    /** Le compte des résultats de l'aller. */
    public String resultsCount(int count) {
        return count + " " + resultWord + (count > 1 ? "s" : "");
    }

    /** Le compte des candidats d'import. */
    public String candidatesCount(int count) {
        return count + " " + candidateWord + (count > 1 ? "s" : "");
    }

    /** Le compte des lignes de l'aperçu de définition. */
    public String bodyLinesCount(int count) {
        return count + " " + bodyLineWord + (count > 1 ? "s" : "");
    }

    /** Le compte des patrons d'entourage. */
    public String patternsCount(int count) {
        return count + " " + patternWord + (count > 1 ? "s" : "");
    }

    /** Le compte des extraits. */
    public String snippetsCount(int count) {
        return count + " " + snippetWord + (count > 1 ? "s" : "");
    }

    /** Le compte des fichiers et emplacements (le pied des récents). */
    public String recentsCount(int files, int places) {
        return files + " " + fileWord + (files > 1 ? "s" : "")
                + " · " + places + " " + placeWord + (places > 1 ? "s" : "");
    }

    /** Le compte des symboles (le pied de la structure). */
    public String symbolsCount(int count) {
        return count + " " + symbolWord + (count > 1 ? "s" : "");
    }

    /** Le compte filtré (le pied de la structure : « N sur M »). */
    public String filteredCount(int kept, int total) {
        return kept + " " + symbolWord + (kept > 1 ? "s" : "")
                + " sur " + total;
    }

    /** Le compte des copies (le pied du presse-papiers). */
    public String copiesCount(int count) {
        return count + " " + copyWord + (count > 1 ? "s" : "");
    }

    /** Le bouton « Appliquer » portant son compte. */
    public String applyWith(int count) {
        return apply + " (" + count + ")";
    }

    /** Le titre des actions rapides (avec sa ligne). */
    public String quickActionsAt(int line) {
        return quickActions + " · " + lineWord + " " + (line + 1);
    }

    /** Le titre des diagnostics (avec sa ligne). */
    public String diagnosticsAt(int line) {
        return diagnostics + " · " + lineWord + " " + (line + 1);
    }

    /** Le titre des usages (avec le symbole). */
    public String usagesOf(String symbol) {
        return usages + " · " + (symbol == null ? "" : symbol);
    }

    /** L'invite de la barre d'aller à la ligne. */
    public String gotoBarInvite() {
        return gotoBarInvite;
    }

    /** La question de la bulle d'import (« Importer « x » ? »). */
    public String importQuestion(String word) {
        return "Importer « " + (word == null ? "" : word) + " » ?";
    }

    // LES ÉTAGES

    /** L'étage de navigation. */
    public String navigation() {
        return navigation;
    }

    /** L'étage d'édition. */
    public String edition() {
        return edition;
    }

    /** L'étage du presse-papiers. */
    public String clipboardFamily() {
        return clipboardFamily;
    }

    /** L'étage de l'historique de la sélection. */
    public String historyFamily() {
        return historyFamily;
    }

    /** L'étage des actions de l'hôte. */
    public String hostFamily() {
        return hostFamily;
    }

    // LES RANGÉES

    /** La rangée « Aller à la ligne… ». */
    public String gotoLineRow() {
        return gotoLineRow;
    }

    /** La rangée « Rechercher… ». */
    public String searchRow() {
        return searchRow;
    }

    /** La rangée des usages d'un symbole. */
    public String usagesRow(String word) {
        return "Usages de « " + (word == null ? "" : word) + " »…";
    }

    /** La rangée « Liste des problèmes… ». */
    public String problemsRow() {
        return problemsRow;
    }

    /** La rangée « Aller au symbole… ». */
    public String gotoSymbolRow() {
        return gotoSymbolRow;
    }

    /** La rangée « Fichiers récents… ». */
    public String recentFilesRow() {
        return recentFilesRow;
    }

    /** La rangée « Couper ». */
    public String cutRow() {
        return cutRow;
    }

    /** La rangée « Tout sélectionner ». */
    public String selectAllRow() {
        return selectAllRow;
    }

    /** La rangée « Quitter la sélection ». */
    public String quitSelectionRow() {
        return quitSelectionRow;
    }

    /** La rangée « Historique du presse-papiers… ». */
    public String clipboardHistoryRow() {
        return clipboardHistoryRow;
    }

    /** La rangée « Historique de la sélection ». */
    public String selectionHistoryRow() {
        return selectionHistoryRow;
    }

    /** La rangée « Entourer de… ». */
    public String surroundRow() {
        return surroundRow;
    }

    /** La rangée « Insérer un extrait… ». */
    public String snippetsRow() {
        return snippetsRow;
    }

    // LES ENTRÉES

    /** L'entrée « Aller à la définition ». */
    public String definitionEntry() {
        return definitionEntry;
    }

    /** L'entrée « Aperçu de la définition ». */
    public String definitionPreviewEntry() {
        return definitionPreviewEntry;
    }

    /** L'entrée « Trouver les usages ». */
    public String referencesEntry() {
        return referencesEntry;
    }

    /** L'entrée « Renommer le symbole ». */
    public String renameEntry() {
        return renameEntry;
    }

    /** L'entrée « Formater le document ». */
    public String formatEntry() {
        return formatEntry;
    }

    /** L'entrée « Les symboles du document ». */
    public String symbolsEntry() {
        return symbolsEntry;
    }

    /** Le titre du correctif du serveur (« Correctif : … »). */
    public String fixTitle(String serverTitle) {
        return fixPrefix + " " + (serverTitle == null ? "" : serverTitle);
    }

    // LES CHAÎNES DE L'ASSISTANT (B2)

    /** Le titre de la carte de l'assistant (ses réponses s'y lisent). */
    public String assistant() {
        return assistant;
    }

    /** Le corps de l'ATTENTE (la réponse vole). */
    public String aiThinking() {
        return aiThinking;
    }

    /** L'étiquette du bouton ANNULER (la réponse en vol). */
    public String aiCancel() {
        return aiCancel;
    }

    /** L'entrée « Expliquer » (l'ampoule). */
    public String aiExplain() {
        return aiExplain;
    }

    /** L'entrée « Expliquer l'erreur » (l'étage des correctifs). */
    public String aiExplainError() {
        return aiExplainError;
    }

    /** L'entrée « Corriger avec l'assistant » (l'étage des correctifs). */
    public String aiFix() {
        return aiFix;
    }

    /** L'entrée « Documenter » (l'ampoule). */
    public String aiDocument() {
        return aiDocument;
    }

    /** L'entrée « Écrire le test » (l'ampoule). */
    public String aiWriteTest() {
        return aiWriteTest;
    }

    /** La phrase de la panne INDISPONIBLE. */
    public String aiUnavailable() {
        return aiUnavailable;
    }

    /** La phrase de la panne ANNULÉE. */
    public String aiCancelled() {
        return aiCancelled;
    }

    /** La phrase de la panne DÉLAI. */
    public String aiTimeout() {
        return aiTimeout;
    }

    /** La phrase de la panne REFUSÉE. */
    public String aiRefused() {
        return aiRefused;
    }

    /** La phrase de la panne INTERNE. */
    public String aiInternal() {
        return aiInternal;
    }

    /** La phrase du texte CHANGÉ depuis la demande (la réponse ne
     * se pose pas). */
    public String aiStale() {
        return aiStale;
    }

    /** La phrase de la réponse MUETTE (vide). */
    public String aiEmpty() {
        return aiEmpty;
    }

    // LES CHAÎNES DE LA MODIFICATION PAR INSTRUCTION (B3)

    /** La pastille « Modifier avec l'IA… » (la barre de sélection). */
    public String aiEditSelection() {
        return aiEditSelection;
    }

    /** L'invite de la barre d'instruction. */
    public String aiEditInvite() {
        return aiEditInvite;
    }

    /** L'indication du champ vierge de la barre. */
    public String aiEditHint() {
        return aiEditHint;
    }

    /** Le retour d'une instruction vide (la validation sans texte). */
    public String aiEditEmpty() {
        return aiEditEmpty;
    }

    /** Le titre de l'aperçu de la modification proposée. */
    public String aiDiffTitle() {
        return aiDiffTitle;
    }

    /** L'étiquette de la section « Avant » de l'aperçu. */
    public String aiDiffBefore() {
        return aiDiffBefore;
    }

    /** L'étiquette de la section « Après » de l'aperçu. */
    public String aiDiffAfter() {
        return aiDiffAfter;
    }

    /** Le bouton « Accepter » de l'aperçu. */
    public String aiDiffAccept() {
        return aiDiffAccept;
    }

    /** Le bouton « Rejeter » de l'aperçu. */
    public String aiDiffReject() {
        return aiDiffReject;
    }

    /** Le bouton « Réessayer » de l'aperçu. */
    public String aiDiffRetry() {
        return aiDiffRetry;
    }

    /** La phrase des erreurs NÉES de la modification posée. */
    public String aiNewErrors() {
        return aiNewErrors;
    }

    /** L'annonce de la suggestion disponible (le fantôme au caret). */
    public String aiGhostReady() {
        return aiGhostReady;
    }

    /** L'annonce d'ouverture de la carte de l'assistant (sa propre
     * voix — pas celle de la documentation). */
    public String aiAnnounced() {
        return aiAnnounced;
    }

    /**
     * Le bâtisseur des chaînes : chaque poseur y remplace UNE valeur,
     * {@link #build()} fige le tout.
     */
    public static final class Builder {

        private String structure = "Structure";
        private String problems = "Problèmes";
        private String recents = "Récents";
        private String gotoTitle = "Aller à";
        private String surround = "Entourer de";
        private String snippets = "Extraits";
        private String clipboard = "Presse-papiers";
        private String history = "Historique local";
        private String open = "Ouvrir";
        private String apply = "Appliquer";
        private String filesFamily = "Fichiers";
        private String placesFamily = "Emplacements";
        private String errorsFamily = "Erreurs";
        private String warningsFamily = "Avertissements";
        private String infosFamily = "Informations";
        private String lineWord = "ligne";
        private String targetWord = "cible";
        private String resultWord = "résultat";
        private String candidateWord = "candidat";
        private String bodyLineWord = "ligne";
        private String patternWord = "patron";
        private String snippetWord = "extrait";
        private String fileWord = "fichier";
        private String placeWord = "emplacement";
        private String symbolWord = "symbole";
        private String copyWord = "copie";
        private String quickActions = "Actions rapides";
        private String diagnostics = "Diagnostics";
        private String usages = "Usages";
        private String gotoBarInvite = "Aller à la ligne : ";
        private String navigation = "Navigation";
        private String edition = "Édition";
        private String clipboardFamily = "Presse-papiers";
        private String historyFamily = "Historique";
        private String hostFamily = "Hôte";
        private String gotoLineRow = "Aller à la ligne…";
        private String searchRow = "Rechercher…";
        private String problemsRow = "Liste des problèmes…";
        private String gotoSymbolRow = "Aller au symbole…";
        private String recentFilesRow = "Fichiers récents…";
        private String cutRow = "Couper";
        private String selectAllRow = "Tout sélectionner";
        private String quitSelectionRow = "Quitter la sélection";
        private String clipboardHistoryRow = "Historique du presse-papiers…";
        private String selectionHistoryRow = "Historique de la sélection";
        private String surroundRow = "Entourer de…";
        private String snippetsRow = "Insérer un extrait…";
        private String definitionEntry = "Aller à la définition";
        private String definitionPreviewEntry = "Aperçu de la définition";
        private String referencesEntry = "Trouver les usages";
        private String renameEntry = "Renommer le symbole";
        private String formatEntry = "Formater le document";
        private String symbolsEntry = "Les symboles du document";
        private String fixPrefix = "Correctif :";
        private String assistant = "Assistant";
        private String aiThinking = "Réflexion…";
        private String aiCancel = "Annuler";
        private String aiExplain = "Expliquer";
        private String aiExplainError = "Expliquer l'erreur";
        private String aiFix = "Corriger avec l'assistant";
        private String aiDocument = "Documenter";
        private String aiWriteTest = "Écrire le test";
        private String aiUnavailable = "L'assistant n'est pas disponible.";
        private String aiCancelled = "La demande fut annulée.";
        private String aiTimeout = "Le temps de réponse est écoulé.";
        private String aiRefused = "La demande fut refusée.";
        private String aiInternal = "Panne interne de l'assistant.";
        private String aiStale = "Le texte a changé depuis la demande — la réponse ne se pose pas.";
        private String aiEmpty = "La réponse est vide.";
        private String aiEditSelection = "Modifier avec l'IA…";
        private String aiEditInvite = "Instruction : ";
        private String aiEditHint = "que changer ?";
        private String aiEditEmpty = "l'instruction est vide";
        private String aiDiffTitle = "Modification proposée";
        private String aiDiffBefore = "Avant";
        private String aiDiffAfter = "Après";
        private String aiDiffAccept = "Accepter";
        private String aiDiffReject = "Rejeter";
        private String aiDiffRetry = "Réessayer";
        private String aiNewErrors = "De nouvelles erreurs sont nées de la modification — vérifie la zone.";
        private String aiGhostReady = "Suggestion disponible";
        private String aiAnnounced = "Assistant ouvert";

        /** Un bâtisseur aux valeurs par défaut. */
        public Builder() {
        }

        /** Pose le titre de la carte de structure. */
        public Builder structure(String value) {
            structure = value;
            return this;
        }

        /** Pose le titre de la carte des problèmes. */
        public Builder problems(String value) {
            problems = value;
            return this;
        }

        /** Pose le titre de la carte des récents. */
        public Builder recents(String value) {
            recents = value;
            return this;
        }

        /** Pose le titre de la carte de l'aller. */
        public Builder gotoTitle(String value) {
            gotoTitle = value;
            return this;
        }

        /** Pose le titre de la carte d'entourage. */
        public Builder surround(String value) {
            surround = value;
            return this;
        }

        /** Pose le titre de la carte des extraits. */
        public Builder snippets(String value) {
            snippets = value;
            return this;
        }

        /** Pose le titre de la carte du presse-papiers. */
        public Builder clipboard(String value) {
            clipboard = value;
            return this;
        }

        /** Pose le titre de la carte de l'historique local. */
        public Builder history(String value) {
            history = value;
            return this;
        }

        /** Pose le bouton « Ouvrir ». */
        public Builder open(String value) {
            open = value;
            return this;
        }

        /** Pose le bouton « Appliquer ». */
        public Builder apply(String value) {
            apply = value;
            return this;
        }

        /** Pose l'étiquette de la famille des fichiers. */
        public Builder filesFamily(String value) {
            filesFamily = value;
            return this;
        }

        /** Pose l'étiquette de la famille des emplacements. */
        public Builder placesFamily(String value) {
            placesFamily = value;
            return this;
        }

        /** Pose l'étiquette de la famille des erreurs. */
        public Builder errorsFamily(String value) {
            errorsFamily = value;
            return this;
        }

        /** Pose l'étiquette de la famille des avertissements. */
        public Builder warningsFamily(String value) {
            warningsFamily = value;
            return this;
        }

        /** Pose l'étiquette de la famille des informations. */
        public Builder infosFamily(String value) {
            infosFamily = value;
            return this;
        }

        /** Pose le mot « ligne ». */
        public Builder lineWord(String value) {
            lineWord = value;
            return this;
        }

        /** Pose le nom compté d'une cible. */
        public Builder targetWord(String value) {
            targetWord = value;
            return this;
        }

        /** Pose le nom compté d'un résultat. */
        public Builder resultWord(String value) {
            resultWord = value;
            return this;
        }

        /** Pose le nom compté d'un candidat d'import. */
        public Builder candidateWord(String value) {
            candidateWord = value;
            return this;
        }

        /** Pose le nom compté d'une ligne d'aperçu. */
        public Builder bodyLineWord(String value) {
            bodyLineWord = value;
            return this;
        }

        /** Pose le nom compté d'un patron. */
        public Builder patternWord(String value) {
            patternWord = value;
            return this;
        }

        /** Pose le nom compté d'un extrait. */
        public Builder snippetWord(String value) {
            snippetWord = value;
            return this;
        }

        /** Pose le nom compté d'un fichier. */
        public Builder fileWord(String value) {
            fileWord = value;
            return this;
        }

        /** Pose le nom compté d'un emplacement. */
        public Builder placeWord(String value) {
            placeWord = value;
            return this;
        }

        /** Pose le nom compté d'un symbole. */
        public Builder symbolWord(String value) {
            symbolWord = value;
            return this;
        }

        /** Pose le nom compté d'une copie. */
        public Builder copyWord(String value) {
            copyWord = value;
            return this;
        }

        /** Pose le titre des actions rapides. */
        public Builder quickActions(String value) {
            quickActions = value;
            return this;
        }

        /** Pose le titre des diagnostics. */
        public Builder diagnostics(String value) {
            diagnostics = value;
            return this;
        }

        /** Pose le titre des usages. */
        public Builder usages(String value) {
            usages = value;
            return this;
        }

        /** Pose l'invite de la barre d'aller à la ligne. */
        public Builder gotoBarInvite(String value) {
            gotoBarInvite = value;
            return this;
        }

        /** Pose l'étage de navigation. */
        public Builder navigation(String value) {
            navigation = value;
            return this;
        }

        /** Pose l'étage d'édition. */
        public Builder edition(String value) {
            edition = value;
            return this;
        }

        /** Pose l'étage du presse-papiers. */
        public Builder clipboardFamily(String value) {
            clipboardFamily = value;
            return this;
        }

        /** Pose l'étage de l'historique. */
        public Builder historyFamily(String value) {
            historyFamily = value;
            return this;
        }

        /** Pose l'étage de l'hôte. */
        public Builder hostFamily(String value) {
            hostFamily = value;
            return this;
        }

        /** Pose la rangée « Aller à la ligne… ». */
        public Builder gotoLineRow(String value) {
            gotoLineRow = value;
            return this;
        }

        /** Pose la rangée « Rechercher… ». */
        public Builder searchRow(String value) {
            searchRow = value;
            return this;
        }

        /** Pose la rangée « Liste des problèmes… ». */
        public Builder problemsRow(String value) {
            problemsRow = value;
            return this;
        }

        /** Pose la rangée « Aller au symbole… ». */
        public Builder gotoSymbolRow(String value) {
            gotoSymbolRow = value;
            return this;
        }

        /** Pose la rangée « Fichiers récents… ». */
        public Builder recentFilesRow(String value) {
            recentFilesRow = value;
            return this;
        }

        /** Pose la rangée « Couper ». */
        public Builder cutRow(String value) {
            cutRow = value;
            return this;
        }

        /** Pose la rangée « Tout sélectionner ». */
        public Builder selectAllRow(String value) {
            selectAllRow = value;
            return this;
        }

        /** Pose la rangée « Quitter la sélection ». */
        public Builder quitSelectionRow(String value) {
            quitSelectionRow = value;
            return this;
        }

        /** Pose la rangée « Historique du presse-papiers… ». */
        public Builder clipboardHistoryRow(String value) {
            clipboardHistoryRow = value;
            return this;
        }

        /** Pose la rangée « Historique de la sélection ». */
        public Builder selectionHistoryRow(String value) {
            selectionHistoryRow = value;
            return this;
        }

        /** Pose la rangée « Entourer de… ». */
        public Builder surroundRow(String value) {
            surroundRow = value;
            return this;
        }

        /** Pose la rangée « Insérer un extrait… ». */
        public Builder snippetsRow(String value) {
            snippetsRow = value;
            return this;
        }

        /** Pose l'entrée « Aller à la définition ». */
        public Builder definitionEntry(String value) {
            definitionEntry = value;
            return this;
        }

        /** Pose l'entrée « Aperçu de la définition ». */
        public Builder definitionPreviewEntry(String value) {
            definitionPreviewEntry = value;
            return this;
        }

        /** Pose l'entrée « Trouver les usages ». */
        public Builder referencesEntry(String value) {
            referencesEntry = value;
            return this;
        }

        /** Pose l'entrée « Renommer le symbole ». */
        public Builder renameEntry(String value) {
            renameEntry = value;
            return this;
        }

        /** Pose l'entrée « Formater le document ». */
        public Builder formatEntry(String value) {
            formatEntry = value;
            return this;
        }

        /** Pose l'entrée « Les symboles du document ». */
        public Builder symbolsEntry(String value) {
            symbolsEntry = value;
            return this;
        }

        /** Pose le préfixe des correctifs du serveur. */
        public Builder fixPrefix(String value) {
            fixPrefix = value;
            return this;
        }

        /** Pose le titre de la carte de l'assistant. */
        public Builder assistant(String value) {
            assistant = value;
            return this;
        }

        /** Pose le corps de l'attente. */
        public Builder aiThinking(String value) {
            aiThinking = value;
            return this;
        }

        /** Pose l'étiquette du bouton annuler. */
        public Builder aiCancel(String value) {
            aiCancel = value;
            return this;
        }

        /** Pose l'entrée « Expliquer ». */
        public Builder aiExplain(String value) {
            aiExplain = value;
            return this;
        }

        /** Pose l'entrée « Expliquer l'erreur ». */
        public Builder aiExplainError(String value) {
            aiExplainError = value;
            return this;
        }

        /** Pose l'entrée « Corriger avec l'assistant ». */
        public Builder aiFix(String value) {
            aiFix = value;
            return this;
        }

        /** Pose l'entrée « Documenter ». */
        public Builder aiDocument(String value) {
            aiDocument = value;
            return this;
        }

        /** Pose l'entrée « Écrire le test ». */
        public Builder aiWriteTest(String value) {
            aiWriteTest = value;
            return this;
        }

        /** Pose la phrase de la panne indisponible. */
        public Builder aiUnavailable(String value) {
            aiUnavailable = value;
            return this;
        }

        /** Pose la phrase de la panne annulée. */
        public Builder aiCancelled(String value) {
            aiCancelled = value;
            return this;
        }

        /** Pose la phrase de la panne délai. */
        public Builder aiTimeout(String value) {
            aiTimeout = value;
            return this;
        }

        /** Pose la phrase de la panne refusée. */
        public Builder aiRefused(String value) {
            aiRefused = value;
            return this;
        }

        /** Pose la phrase de la panne interne. */
        public Builder aiInternal(String value) {
            aiInternal = value;
            return this;
        }

        /** Pose la phrase du texte changé. */
        public Builder aiStale(String value) {
            aiStale = value;
            return this;
        }

        /** Pose la phrase de la réponse vide. */
        public Builder aiEmpty(String value) {
            aiEmpty = value;
            return this;
        }

        /** Pose la pastille « Modifier avec l'IA… ». */
        public Builder aiEditSelection(String value) {
            aiEditSelection = value;
            return this;
        }

        /** Pose l'invite de la barre d'instruction. */
        public Builder aiEditInvite(String value) {
            aiEditInvite = value;
            return this;
        }

        /** Pose l'indication du champ vierge de la barre. */
        public Builder aiEditHint(String value) {
            aiEditHint = value;
            return this;
        }

        /** Pose le retour d'une instruction vide. */
        public Builder aiEditEmpty(String value) {
            aiEditEmpty = value;
            return this;
        }

        /** Pose le titre de l'aperçu de la modification. */
        public Builder aiDiffTitle(String value) {
            aiDiffTitle = value;
            return this;
        }

        /** Pose l'étiquette de la section « Avant ». */
        public Builder aiDiffBefore(String value) {
            aiDiffBefore = value;
            return this;
        }

        /** Pose l'étiquette de la section « Après ». */
        public Builder aiDiffAfter(String value) {
            aiDiffAfter = value;
            return this;
        }

        /** Pose le bouton « Accepter ». */
        public Builder aiDiffAccept(String value) {
            aiDiffAccept = value;
            return this;
        }

        /** Pose le bouton « Rejeter ». */
        public Builder aiDiffReject(String value) {
            aiDiffReject = value;
            return this;
        }

        /** Pose le bouton « Réessayer ». */
        public Builder aiDiffRetry(String value) {
            aiDiffRetry = value;
            return this;
        }

        /** Pose la phrase des erreurs nées de la modification. */
        public Builder aiNewErrors(String value) {
            aiNewErrors = value;
            return this;
        }

        /** Pose l'annonce de la suggestion disponible. */
        public Builder aiGhostReady(String value) {
            aiGhostReady = value;
            return this;
        }

        /** Pose l'annonce d'ouverture de l'assistant. */
        public Builder aiAnnounced(String value) {
            aiAnnounced = value;
            return this;
        }

        /**
         * Fige les chaînes : les cartes ne les relisent jamais à
         * moitié.
         *
         * @return les chaînes posées
         */
        public PopupStrings build() {
            return new PopupStrings(this);
        }
    }
}
