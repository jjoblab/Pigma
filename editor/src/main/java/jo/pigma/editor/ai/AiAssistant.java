package jo.pigma.editor.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import jo.pigma.ai.AiBackend;
import jo.pigma.ai.AiCall;
import jo.pigma.ai.AiCapabilities;
import jo.pigma.ai.AiDiagnostic;
import jo.pigma.ai.AiError;
import jo.pigma.ai.AiListener;
import jo.pigma.ai.AiRequest;
import jo.pigma.ai.AiResult;
import jo.pigma.ai.AiTask;
import jo.pigma.ai.ContextBuilder;
import jo.pigma.core.TextBuffer;
import jo.pigma.editor.EditorView;
import jo.pigma.editor.popups.AiDiffCard;
import jo.pigma.editor.popups.DiagnosticCard;
import jo.pigma.editor.popups.PopupStrings;
import jo.pigma.editor.popups.QuickActionCard;
import jo.pigma.syntax.StructureDocument;

/**
 * La façade de l'assistant : l'IA DE L'HÔTE entre dans la vue — la
 * façon du serveur de langage ({@code LspEditor}), portée à la
 * couche {@code :ai}. Pigma ne connaît AUCUNE intelligence
 * artificielle (ni fournisseur, ni modèle, ni réseau) : la façade
 * branche LE MOTEUR DE L'HÔTE ({@code AiBackend}) à la vue — les
 * entrées naissent de ce que le moteur DÉCLARE accepter (une tâche
 * non déclarée ne se propose jamais), la demande part par le
 * préparateur ({@code ContextBuilder} — la politique et le filtre
 * de l'hôte s'y appliquent TOUJOURS), la réponse revient à la vue :
 * le texte qui se LIT s'y montre balisé dans la popup d'info, la
 * réponse qui ÉCRIT se PROPOSE à l'aperçu (Accepter / Rejeter /
 * Réessayer) — l'acceptation seule POSE, par UNE unité
 * d'annulation, après VÉRIFICATION : un texte qui a bougé depuis
 * la demande ne se pose pas.
 *
 * <p>LE VOL VIT ANNULABLE : la question part, la popup d'info vit
 * immédiate (« Réflexion… ») avec son bouton ANNULER armé au bas —
 * le doigt qui le tape coupe le vol (fermer la carte d'attente
 * l'abandonne pareillement : la question fermée ne se paie plus).
 * Les rappels du moteur peuvent venir de N'IMPORTE QUEL fil : la
 * façade y repasse par le fil de la vue avant d'agir. Une réponse
 * d'un vol TUÉ (annulé, remplacé par une question neuve) se jette :
 * la génération du vol s'y éprouve.</p>
 *
 * <p>Les tâches proposées : EXPLIQUER et DOCUMENTER à l'ampoule,
 * EXPLIQUER L'ERREUR et CORRIGER à l'étage des correctifs de la
 * carte de diagnostic (la correction se PROPOSE à l'aperçu — jamais
 * elle ne se pose d'elle-même), MODIFIER LA SÉLECTION à la pastille
 * de la barre de sélection (la barre d'instruction y demande CE que
 * l'utilisateur veut), ÉCRIRE LE TEST à l'ampoule (la réponse se
 * lit ET se rend à l'écouteur de l'hôte — lui seul sait où créer un
 * fichier, Pigma n'en crée jamais). Le document courant se dit à la
 * façade ({@link #setDocument}) : l'hôte qui change d'onglet y
 * suit.</p>
 */
public final class AiAssistant implements EditorView.AssistantProvider {

    /** Le budget de contexte par défaut (en caractères — Pigma
     * ignore les jetons d'un modèle, le moteur de l'hôte convertit). */
    private static final int DEFAULT_CONTEXT_CHARS = 4000;

    /** La vue qui porte les cartes (l'ampoule, le diagnostic, l'info). */
    private final EditorView view;

    /** Le moteur de l'hôte (null : la façade ne propose rien). */
    private final AiBackend backend;

    /** Le préparateur de la demande (la politique et le filtre de
     * l'hôte s'y appliquent toujours). */
    private final ContextBuilder context;

    /** Le compteur des vols (une réponse d'un vol tué se jette). */
    private final AtomicLong flights = new AtomicLong();

    /** Le vol vivant (null : aucun). */
    private volatile AiCall flying;

    /** Le nom du document courant (l'étiquette qui voyage). */
    private String documentName = "";

    /** Le langage du document courant (l'identifiant qui voyage). */
    private String languageId = "";

    /** Le budget de contexte (en caractères, zéro : le contexte ne
     * voyage pas). */
    private int contextChars = DEFAULT_CONTEXT_CHARS;

    /** L'écouteur des tests écrits (l'hôte seul sait où créer un
     * fichier). */
    private Consumer<String> testListener;

    /** Le libellé de la pastille « Modifier avec l'IA… » posée à la
     * barre de sélection (null : aucune — le moteur qui ne déclare
     * pas la tâche ne la propose pas). */
    private String posedEditLabel;

    /** La dernière question partie (la NOUVELLE ESSAIE la repose à
     * l'identique). */
    private Ask lastAsk;

    /** La proposition en attente de verdict (null : aucune). */
    private Proposal proposal;

    /** Le contrôle des erreurs nées en attente de la passe close
     * (null : aucun — la cadence ralentie retient la passe, le
     * contrôle y attend SON mot). */
    private NewErrorCheck newErrorCheck;

    /**
     * La façade se branche d'elle-même : le fournisseur des entrées
     * se pose sur la vue (l'ampoule et l'étage des correctifs y
     * trouvent leurs rangées), l'action d'annulation aussi (le
     * bouton de la carte d'attente y coupe le vol) — et la pastille
     * « Modifier avec l'IA… » se pose à la barre de sélection SI le
     * moteur déclare la tâche ET que la politique autorise le
     * document.
     *
     * @param view    la vue, jamais null
     * @param backend le moteur de l'hôte, jamais null
     * @param context le préparateur (la politique, le filtre), jamais
     *                null
     */
    public AiAssistant(EditorView view, AiBackend backend,
            ContextBuilder context) {
        this.view = view;
        this.backend = backend;
        this.context = context;
        view.setAssistantProvider(this);
        view.setInfoCancelAction(this::cancelFlight);
        view.setAnalysisListener(this::analysisSettled);
        refreshEditAction();
    }

    /**
     * Débranche la façade : le vol vivant se coupe (sans phrase —
     * l'écran s'en va), les entrées disparaissent de l'ampoule et de
     * l'étage des correctifs, la pastille de la sélection se range,
     * le bouton d'annulation se range, la proposition en attente se
     * jette.
     */
    public void detach() {
        killFlight();
        view.setAssistantProvider(null);
        view.setInfoCancelAction(null);
        view.setAnalysisListener(null);
        view.armInfoCancel(null);
        view.dismissAiEditPopup();
        view.dismissAiDiffPopup();
        proposal = null;
        lastAsk = null;
        newErrorCheck = null;
        refreshEditAction();
    }

    /**
     * Dit le document courant (l'hôte y suit ses onglets) : le nom
     * et le langage voyagent dans chaque demande, la politique de
     * l'hôte s'y éprouve (un document refusé ne propose RIEN — la
     * pastille de la sélection se retire).
     *
     * @param name     le nom du document (l'étiquette), vide permis
     * @param language l'identifiant du langage, vide permis
     */
    public void setDocument(String name, String language) {
        documentName = name == null ? "" : name;
        languageId = language == null ? "" : language;
        refreshEditAction();
    }

    /**
     * Pose le budget de contexte en caractères (zéro : le contexte ne
     * voyage pas — la tâche vit sans ; l'instruction et les
     * diagnostics voyagent toujours).
     *
     * @param chars le budget, positif ou nul
     */
    public void setContextBudget(int chars) {
        contextChars = Math.max(0, chars);
    }

    /**
     * Pose l'écouteur des tests écrits : la tâche d'écriture du test
     * rend son texte À L'HÔTE — lui seul sait où créer un fichier,
     * Pigma n'en crée jamais. La réponse se LIT aussi à la popup
     * (balisée telle quelle).
     *
     * @param listener l'écouteur du texte du test, null pour le
     *                 ranger
     */
    public void setTestListener(Consumer<String> listener) {
        testListener = listener;
    }

    /**
     * REPOSE la pastille « Modifier avec l'IA… » : elle ne vit que
     * si le moteur DÉCLARE la tâche ET que la politique AUTORISE le
     * document — la façade la repose au branchement et à chaque
     * document ; l'hôte dont le moteur change d'avis à la course y
     * repasse.
     */
    public void refreshEditAction() {
        String label = view.popupStrings().aiEditSelection();
        AiCapabilities capabilities = served();
        boolean wanted = capabilities != null
                && capabilities.accepts(AiTask.EDIT_SELECTION)
                && context.allowed(documentName, languageId);
        if (wanted && posedEditLabel == null) {
            view.addSelectionAction(label, this::openEditBar);
            posedEditLabel = label;
        } else if (!wanted && posedEditLabel != null) {
            view.removeSelectionAction(posedEditLabel);
            posedEditLabel = null;
        } else if (wanted && !label.equals(posedEditLabel)) {
            // les chaînes ont bougé : la pastille les suit
            view.removeSelectionAction(posedEditLabel);
            view.addSelectionAction(label, this::openEditBar);
            posedEditLabel = label;
        }
    }

    /**
     * La pastille ouvre la BARRE D'INSTRUCTION : elle ne vit qu'à la
     * SÉLECTION vivante (la tâche modifie CE qui est sélectionné) et
     * aux capacités et à la politique du moment (le moteur qui a
     * changé d'avis n'ouvre rien).
     */
    private void openEditBar() {
        AiCapabilities capabilities = served();
        if (!view.hasSelection() || capabilities == null
                || !capabilities.accepts(AiTask.EDIT_SELECTION)
                || !context.allowed(documentName, languageId)) {
            return;
        }
        view.showAiEditPopup(this::submitInstruction);
    }

    /**
     * L'instruction soumise : le vol part (la tâche EDIT_SELECTION,
     * l'instruction et la sélection voyagent, le préparateur bâtit
     * la demande).
     *
     * @param instruction l'instruction tapée, jamais vide
     * @return vrai (le vol part — la barre se range)
     */
    private boolean submitInstruction(String instruction) {
        start(AiTask.EDIT_SELECTION, instruction, List.of(), -1, -1);
        return true;
    }

    /**
     * Les entrées de l'ampoule : les tâches que le moteur DÉCLARE et
     * que la politique AUTORISE — rien d'autre ne se propose (une
     * tâche non acceptée ne paraît jamais, un document refusé ne
     * paraît pas non plus). L'action ancre au CURSEUR DU MOMENT du
     * tap (la vue y vit, la façade y lit).
     *
     * @param line   l'indice de la ligne (l'ancrage se lit au caret)
     * @param column la colonne du curseur (l'ancrage s'y lit)
     * @return les entrées, jamais null (vide : rien ne se propose)
     */
    @Override
    public List<QuickActionCard.Entry> quickActionsAt(int line, int column) {
        List<QuickActionCard.Entry> entries = new ArrayList<>();
        AiCapabilities capabilities = served();
        if (capabilities == null
                || !context.allowed(documentName, languageId)) {
            return entries;
        }
        PopupStrings strings = view.popupStrings();
        if (capabilities.accepts(AiTask.EXPLAIN)) {
            entries.add(new QuickActionCard.Entry(
                    strings.aiExplain(), () -> start(AiTask.EXPLAIN, "",
                            List.of(), -1, -1)));
        }
        if (capabilities.accepts(AiTask.DOCUMENT)) {
            entries.add(new QuickActionCard.Entry(
                    strings.aiDocument(), () -> start(AiTask.DOCUMENT, "",
                            List.of(), -1, -1)));
        }
        if (capabilities.accepts(AiTask.WRITE_TEST)) {
            entries.add(new QuickActionCard.Entry(
                    strings.aiWriteTest(), () -> start(AiTask.WRITE_TEST, "",
                            List.of(), -1, -1)));
        }
        return entries;
    }

    /**
     * Les rangées de l'étage des correctifs : l'explication de
     * l'erreur vit du MESSAGE seul, la correction exige une PLAGE
     * VRAIE (le remplacement la vise) — la ligne sans plage connue
     * n'y propose que l'explication.
     *
     * @param line    l'indice de la ligne
     * @param message le message du premier diagnostic, vide s'il
     *                n'y en a pas
     * @param start   le début de la plage du diagnostic (-1 :
     *                inconnue)
     * @param end     la fin de la plage du diagnostic (-1 :
     *                inconnue)
     * @return les rangées, jamais null (vide : rien ne se propose)
     */
    @Override
    public List<DiagnosticCard.Fix> diagnosticFixesAt(int line,
            String message, int start, int end) {
        List<DiagnosticCard.Fix> fixes = new ArrayList<>();
        AiCapabilities capabilities = served();
        if (capabilities == null
                || !context.allowed(documentName, languageId)
                || message == null || message.isEmpty()) {
            return fixes;
        }
        PopupStrings strings = view.popupStrings();
        if (capabilities.accepts(AiTask.EXPLAIN_ERROR)) {
            fixes.add(new DiagnosticCard.Fix(
                    strings.aiExplainError(), () -> start(
                            AiTask.EXPLAIN_ERROR, "",
                            List.of(new AiDiagnostic(
                                    AiDiagnostic.Severity.ERROR,
                                    message, 0, 0)),
                            -1, -1)));
        }
        if (capabilities.accepts(AiTask.FIX) && start >= 0 && end >= start) {
            fixes.add(new DiagnosticCard.Fix(
                    strings.aiFix(), () -> start(AiTask.FIX, "",
                            List.of(new AiDiagnostic(
                                    AiDiagnostic.Severity.ERROR,
                                    message, start, end)),
                            start, end)));
        }
        return fixes;
    }

    /**
     * Le moteur sert-il ? (Ses capacités se relisent à chaque
     * question : l'hôte qui arme son moteur à la course les y
     * change.)
     *
     * @return les capacités du moteur, null quand il n'y en a pas
     */
    private AiCapabilities served() {
        if (backend == null) {
            return null;
        }
        AiCapabilities capabilities = backend.capabilities();
        return capabilities == null ? AiCapabilities.NONE : capabilities;
    }

    /**
     * DEMARRE LE VOL : la source se lit de la vue (le tampon, le
     * curseur, la sélection, le bloc englobant déduit de la
     * profondeur d'accolades), la demande part par le préparateur
     * (la politique d'abord, le filtre ensuite — un refus rend la
     * phrase REFUSÉE, rien ne part), la carte d'attente vit
     * immédiate avec son bouton ANNULER armé. La question se garde
     * (la NOUVELLE ESSAIE la repose à l'identique).
     *
     * @param task        la tâche demandée
     * @param instruction l'instruction de la modification (vide :
     *                    aucune)
     * @param diagnostics les diagnostics qui voyagent (vide : aucun)
     * @param fixStart    la plage visée par une correction (-1 :
     *                    aucune)
     * @param fixEnd      la plage visée par une correction (-1 :
     *                    aucune)
     */
    private void start(AiTask task, String instruction,
            List<AiDiagnostic> diagnostics, int fixStart, int fixEnd) {
        AiCapabilities capabilities = served();
        if (capabilities == null || !capabilities.accepts(task)) {
            return;
        }
        TextBuffer buffer = view.getBuffer();
        int caret = Math.min(view.caretOffset(), buffer.length());
        int selectionStart = view.hasSelection()
                ? view.selectionStart() : -1;
        int selectionEnd = view.hasSelection() ? view.selectionEnd() : -1;
        ContextBuilder.Source source = new ContextBuilder.Source(
                documentName, languageId, buffer, caret,
                selectionStart, selectionEnd, diagnostics,
                enclosingBlockAt(caret));
        PopupStrings strings = view.popupStrings();
        // l'ancrage de la carte : la ligne du curseur, celle du
        // diagnostic pour les tâches de l'erreur, celle de la
        // sélection pour la modification par instruction
        int anchorLine = view.caretLine();
        if (fixStart >= 0) {
            anchorLine = buffer.lineOfOffset(
                    Math.min(fixStart, buffer.length()));
        } else if (task == AiTask.EDIT_SELECTION && selectionStart >= 0) {
            anchorLine = buffer.lineOfOffset(
                    Math.min(selectionStart, buffer.length()));
        }
        int anchorColumn = fixStart >= 0
                ? Math.min(fixStart, buffer.length())
                        - buffer.getLineStart(anchorLine)
                : caret - buffer.getLineStart(anchorLine);
        AiRequest request = context.prepare(task, instruction, source,
                contextChars, null);
        if (request == null) {
            // le crochet de l'hôte a refusé (le filtre d'ordinaire —
            // la politique venait d'être éprouvée) : la phrase le
            // dit, rien ne part
            view.showInfoPopup(strings.assistant(),
                    strings.aiRefused(), anchorLine, anchorColumn);
            return;
        }
        // la question se garde : la NOUVELLE ESSAIE la repose
        lastAsk = new Ask(task, instruction, diagnostics, fixStart, fixEnd);
        // le vol naît : la génération tue toute réponse d'avant, la
        // carte d'attente vit avec son bouton
        long flight = flights.incrementAndGet();
        Flight listener = new Flight(flight, task, instruction,
                buffer.getRevision(), anchorLine, anchorColumn);
        flying = backend.start(request, listener);
        view.showInfoPopup(strings.assistant(), strings.aiThinking(),
                anchorLine, anchorColumn);
        view.armInfoCancel(strings.aiCancel());
    }

    /**
     * Le bloc englobant le curseur : la profondeur d'accolades déjà
     * calculée par la structure le dit — le bloc le plus interne qui
     * enserre la position, du texte de sa première ligne à celui de
     * sa dernière. Vide au niveau racine (aucun bloc n'englobe le
     * curseur) ou sans structure.
     *
     * @param caret la position du curseur
     * @return le texte du bloc, vide s'il n'est pas déterminable
     */
    private String enclosingBlockAt(int caret) {
        StructureDocument structure = view.getStructureDocument();
        if (structure == null) {
            return "";
        }
        StructureDocument.BlockRange scope = structure.scopeAt(caret);
        if (scope == null) {
            return "";
        }
        TextBuffer buffer = view.getBuffer();
        int from = buffer.getLineStart(scope.fromLine());
        int to = buffer.getLineEnd(
                Math.min(scope.toLine(), buffer.getLineCount() - 1));
        return from < to ? buffer.getText(from, to).toString() : "";
    }

    /**
     * COUPE LE VOL AU DOIGT : le bouton ANNULER (ou la carte fermée
     * — fermer la question, c'est l'abandonner). La génération tue
     * la réponse en vol (si elle vient quand même, elle se jette),
     * la phrase ANNULÉE remplace le corps d'attente.
     */
    private void cancelFlight() {
        if (flying == null) {
            return;
        }
        killFlight();
        view.replaceInfoPopup(view.popupStrings().aiCancelled(), false,
                view.infoPopupLine(), view.infoPopupColumn());
    }

    /**
     * Tue le vol SANS phrase (le débranchement, un vol neuf) : la
     * poignée s'annule (idempotent), la génération avance, le
     * bouton se range.
     */
    private void killFlight() {
        AiCall inFlight = flying;
        flying = null;
        flights.incrementAndGet();
        if (inFlight != null) {
            inFlight.cancel();
        }
        view.armInfoCancel(null);
    }

    /**
     * La phrase d'une famille de panne (une par catégorie — le
     * détail du moteur ne se montre pas, la catégorie parle seule).
     *
     * @param category la famille de la panne
     * @return la phrase localisée
     */
    private String phraseOf(AiError.Category category) {
        PopupStrings strings = view.popupStrings();
        return switch (category) {
            case UNAVAILABLE -> strings.aiUnavailable();
            case CANCELLED -> strings.aiCancelled();
            case TIMEOUT -> strings.aiTimeout();
            case REFUSED -> strings.aiRefused();
            case INTERNAL -> strings.aiInternal();
        };
    }

    /**
     * LA PROPOSITION : la réponse qui ÉCRIT ne se pose JAMAIS
     * d'elle-même — l'aperçu la montre (le texte AVANT, le texte
     * APRÈS), les TROIS VERDICTS y vivent. La proposition se garde :
     * l'ACCEPTATION seule la pose (après vérification de la
     * révision), le REJET la jette, la NOUVELLE ESSAIE repose la
     * question à l'identique.
     *
     * @param task        la tâche qui a suscité la proposition
     * @param instruction l'instruction suscitante (vide : aucune)
     * @param start       le début de la plage visée
     * @param end         la fin de la plage visée
     * @param text        le texte neuf proposé
     * @param revision    la révision du tampon au départ
     * @param anchorLine   la ligne d'ancrage
     * @param anchorColumn la colonne d'ancrage
     */
    private void propose(AiTask task, String instruction, int start,
            int end, String text, int revision, int anchorLine,
            int anchorColumn) {
        TextBuffer buffer = view.getBuffer();
        int boundedStart = Math.max(0, Math.min(start, buffer.length()));
        int boundedEnd = Math.max(boundedStart,
                Math.min(end, buffer.length()));
        String before = buffer.getText(boundedStart, boundedEnd).toString();
        proposal = new Proposal(task, boundedStart, boundedEnd, text,
                revision, anchorLine, anchorColumn);
        view.showAiDiffPreview(instruction, before, text,
                new AiDiffCard.Verdicts() {
                    @Override
                    public void onAccept() {
                        applyProposal();
                    }

                    @Override
                    public void onReject() {
                        proposal = null;
                    }

                    @Override
                    public void onRetry() {
                        retryLastAsk();
                    }
                });
    }

    /**
     * L'ACCEPTATION : la proposition se POSE — après VÉRIFICATION de
     * la révision (un texte qui a bougé depuis la demande ne se pose
     * pas : la phrase le dit), par UNE unité d'annulation (la pose
     * se défait d'un geste) — puis le contrôle des ERREURS NÉES :
     * l'analyse relancée, une erreur neuve dans la zone posée se
     * SIGNALE (le modèle propose, le moteur local vérifie,
     * l'utilisateur valide — la seule vérification que Pigma peut
     * faire).
     */
    private void applyProposal() {
        Proposal pending = proposal;
        proposal = null;
        if (pending == null) {
            return;
        }
        PopupStrings strings = view.popupStrings();
        TextBuffer buffer = view.getBuffer();
        if (buffer.getRevision() != pending.revision()) {
            view.showInfoPopup(strings.assistant(), strings.aiStale(),
                    pending.anchorLine(), pending.anchorColumn());
            return;
        }
        // les erreurs d'avant se prennent (la zone visée, à la
        // révision d'avant la pose)
        int fromLine = buffer.lineOfOffset(pending.start());
        int toLine = buffer.lineOfOffset(
                Math.min(pending.end(), buffer.length()));
        Set<String> before = errorFingerprints(fromLine, toLine);
        // LA POSE : une seule unité d'annulation
        view.applyEdit(pending.start(), pending.end(), pending.text());
        int revision = buffer.getRevision();
        // la zone après la pose (le texte neuf peut compter des
        // lignes de plus) — le contrôle des ERREURS NÉES s'arme : la
        // passe close le dira
        int newLines = 0;
        for (int at = 0; at < pending.text().length(); at++) {
            if (pending.text().charAt(at) == '\n') {
                newLines++;
            }
        }
        newErrorCheck = new NewErrorCheck(fromLine,
                Math.max(toLine, fromLine + newLines), before, revision);
    }

    /**
     * Les empreintes des ERREURS d'une zone (un message par erreur —
     * la position dérive à la pose, le message parle).
     *
     * @param fromLine la première ligne de la zone
     * @param toLine   la dernière ligne de la zone
     * @return les empreintes, jamais null
     */
    private Set<String> errorFingerprints(int fromLine, int toLine) {
        Set<String> fingerprints = new HashSet<>();
        int last = Math.min(toLine, view.getBuffer().getLineCount() - 1);
        for (int line = Math.max(0, fromLine); line <= last; line++) {
            for (DiagnosticCard.Entry entry : view.diagnosticsOfLine(line)) {
                if (entry.severity() == DiagnosticCard.SEVERITY_ERROR
                        && entry.message() != null) {
                    fingerprints.add(entry.message());
                }
            }
        }
        return fingerprints;
    }

    /**
     * Le contrôle des ERREURS NÉES : l'analyse doit rattraper la
     * révision posée (la passe ralentie y vit — la question se
     * repose à la file tant qu'elle ne l'a pas rattrapée), puis les
     * erreurs de la zone se comparent à celles d'avant : une erreur
     * NEUVE dit SA phrase (la zone posée se vérifie à l'œil).
     *
     * @param fromLine la première ligne de la zone
     * @param toLine   la dernière ligne de la zone
     * @param before   les empreintes d'avant la pose
     * @param revision la révision posée
     * @param attempts les re-questions restantes
     */
    private void checkNewErrors(int fromLine, int toLine,
            Set<String> before, int revision, int attempts) {
        if (view.getBuffer().getRevision() != revision) {
            return; // le texte a encore bougé : la garde se perd
        }
        if (!view.analysisSettled()) {
            if (attempts > 0) {
                view.ensureAnalysis();
                view.post(() -> checkNewErrors(fromLine, toLine, before,
                        revision, attempts - 1));
            }
            return;
        }
        Set<String> after = errorFingerprints(fromLine, toLine);
        for (String fingerprint : after) {
            if (!before.contains(fingerprint)) {
                PopupStrings strings = view.popupStrings();
                view.showInfoPopup(strings.assistant(),
                        strings.aiNewErrors(), fromLine, 0);
                return;
            }
        }
    }

    /**
     * La NOUVELLE ESSAIE : la question se repose à l'identique (la
     * tâche, l'instruction, les diagnostics, la plage) — un vol
     * neuf, la réponse d'avant se jette (la génération du vol neuf
     * la tue).
     */
    private void retryLastAsk() {
        Ask ask = lastAsk;
        proposal = null;
        if (ask == null) {
            return;
        }
        start(ask.task(), ask.instruction(), ask.diagnostics(),
                ask.fixStart(), ask.fixEnd());
    }

    /**
     * La passe d'analyse vient de SE CLOSE : le contrôle des erreurs
     * nées s'y joue (la passe a rattrapé la révision posée — la
     * cadence ralentie la retenait). Une erreur NEUVE dans la zone
     * posée dit SA phrase (le modèle propose, le moteur local
     * vérifie, l'utilisateur valide — la seule vérification que
     * Pigma peut faire) ; un texte qui a encore bougé perd la garde
     * (le contrôle se jette).
     */
    private void analysisSettled() {
        NewErrorCheck check = newErrorCheck;
        if (check == null) {
            return;
        }
        if (view.getBuffer().getRevision() != check.revision()) {
            newErrorCheck = null; // le texte a encore bougé : la garde se perd
            return;
        }
        newErrorCheck = null;
        Set<String> after = errorFingerprints(check.fromLine(), check.toLine());
        for (String fingerprint : after) {
            if (!check.before().contains(fingerprint)) {
                PopupStrings strings = view.popupStrings();
                view.showInfoPopup(strings.assistant(),
                        strings.aiNewErrors(), check.fromLine(), 0);
                return;
            }
        }
    }

    /**
     * Une question partie (la NOUVELLE ESSAIE la repose à
     * l'identique).
     *
     * @param task        la tâche demandée
     * @param instruction l'instruction (vide : aucune)
     * @param diagnostics les diagnostics qui voyagent
     * @param fixStart    la plage visée (-1 : aucune)
     * @param fixEnd      la plage visée (-1 : aucune)
     */
    private record Ask(AiTask task, String instruction,
            List<AiDiagnostic> diagnostics, int fixStart, int fixEnd) {
    }

    /**
     * Le contrôle des erreurs nées : la zone vérifiée, les empreintes
     * d'avant la pose, la révision posée (un texte qui bouge encore
     * perd la garde).
     *
     * @param fromLine la première ligne de la zone
     * @param toLine   la dernière ligne de la zone
     * @param before   les empreintes d'avant la pose
     * @param revision la révision posée
     */
    private record NewErrorCheck(int fromLine, int toLine,
            Set<String> before, int revision) {
    }

    /**
     * Une proposition en attente de verdict (l'ACCEPTATION seule la
     * pose).
     *
     * @param task         la tâche suscitante
     * @param start        le début de la plage visée
     * @param end          la fin de la plage visée
     * @param text         le texte neuf proposé
     * @param revision     la révision du tampon au départ
     * @param anchorLine   la ligne d'ancrage
     * @param anchorColumn la colonne d'ancrage
     */
    private record Proposal(AiTask task, int start, int end, String text,
            int revision, int anchorLine, int anchorColumn) {
    }

    /**
     * L'écouteur d'un vol : les rappels viennent de N'IMPORTE QUEL
     * fil (le moteur de l'hôte travaille où il veut) — chaque issue
     * y repasse par le fil de la vue, et la GÉNÉRATION s'y éprouve
     * (la réponse d'un vol tué se jette).
     */
    private final class Flight implements AiListener {

        /** La génération du vol (une réponse d'un autre vol se jette). */
        private final long id;

        /** La tâche demandée (le test écrit se rend à son écouteur). */
        private final AiTask task;

        /** L'instruction de la modification (vide : aucune). */
        private final String instruction;

        /** La révision du tampon au départ (un texte qui a bougé ne
         * se pose pas). */
        private final int revision;

        /** La ligne d'ancrage de la carte d'attente. */
        private final int anchorLine;

        /** La colonne d'ancrage de la carte d'attente. */
        private final int anchorColumn;

        /**
         * Un vol neuf.
         *
         * @param id           la génération du vol
         * @param task         la tâche demandée
         * @param instruction  l'instruction (vide : aucune)
         * @param revision     la révision du tampon au départ
         * @param anchorLine   la ligne d'ancrage
         * @param anchorColumn la colonne d'ancrage
         */
        private Flight(long id, AiTask task, String instruction,
                int revision, int anchorLine, int anchorColumn) {
            this.id = id;
            this.task = task;
            this.instruction = instruction == null ? "" : instruction;
            this.revision = revision;
            this.anchorLine = anchorLine;
            this.anchorColumn = anchorColumn;
        }

        /** Le flux ne s'affiche pas en vol : l'issue remplace le
         * corps d'un trait (le flux vit au moteur qui l'envoie). */
        @Override
        public void onDelta(String textPart) {
            // rien : la carte d'attente demeure jusqu'à l'issue
        }

        @Override
        public void onComplete(AiResult result) {
            view.post(() -> {
                if (id != flights.get()) {
                    return; // le vol est mort : la réponse se jette
                }
                flying = null;
                view.armInfoCancel(null);
                PopupStrings strings = view.popupStrings();
                if (result instanceof AiResult.Text text) {
                    if (text.markdown().isEmpty()) {
                        replace(strings.aiEmpty(), false);
                        return;
                    }
                    if (task == AiTask.WRITE_TEST
                            && testListener != null) {
                        testListener.accept(text.markdown());
                    }
                    replace(text.markdown(), true);
                } else if (result instanceof AiResult.Replacement fix) {
                    if (view.getBuffer().getRevision() != revision) {
                        replace(strings.aiStale(), false);
                        return;
                    }
                    // la réponse qui ÉCRIT se PROPOSE : l'aperçu la
                    // montre, l'acceptation seule la pose
                    view.dismissInfoPopup();
                    propose(task, instruction, fix.start(), fix.end(),
                            fix.text(), revision, anchorLine, anchorColumn);
                } else if (result instanceof AiResult.Insertion insert) {
                    if (view.getBuffer().getRevision() != revision) {
                        replace(strings.aiStale(), false);
                        return;
                    }
                    view.dismissInfoPopup();
                    propose(task, instruction, insert.offset(),
                            insert.offset(), insert.text(), revision,
                            anchorLine, anchorColumn);
                }
            });
        }

        @Override
        public void onError(AiError error) {
            view.post(() -> {
                if (id != flights.get()) {
                    return; // le vol est mort : l'erreur se jette
                }
                flying = null;
                view.armInfoCancel(null);
                replace(phraseOf(error.category()), false);
            });
        }

        /**
         * La réponse remplace le corps d'attente — à SON ancrage
         * (une carte bougée ou rangée n'est pas évincée par une
         * réponse périmée).
         *
         * @param body     le corps de la réponse
         * @param markdown le corps est-il balisé ?
         */
        private void replace(String body, boolean markdown) {
            view.replaceInfoPopup(body, markdown,
                    anchorLine, anchorColumn);
        }
    }
}
