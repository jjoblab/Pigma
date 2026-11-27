package jo.pigma.editor.ai;

import java.util.ArrayList;
import java.util.List;
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
 * le texte qui se LIT s'y montre balisé dans la popup d'info, le
 * remplacement et l'insertion s'y POSENT par UNE unité
 * d'annulation — après VÉRIFICATION : un texte qui a bougé depuis
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
 * carte de diagnostic, ÉCRIRE LE TEST à l'ampoule (la réponse se
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

    /**
     * La façade se branche d'elle-même : le fournisseur des entrées
     * se pose sur la vue (l'ampoule et l'étage des correctifs y
     * trouvent leurs rangées), l'action d'annulation aussi (le
     * bouton de la carte d'attente y coupe le vol).
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
    }

    /**
     * Débranche la façade : le vol vivant se coupe (sans phrase —
     * l'écran s'en va), les entrées disparaissent de l'ampoule et de
     * l'étage des correctifs, le bouton d'annulation se range.
     */
    public void detach() {
        killFlight();
        view.setAssistantProvider(null);
        view.setInfoCancelAction(null);
        view.armInfoCancel(null);
    }

    /**
     * Dit le document courant (l'hôte y suit ses onglets) : le nom
     * et le langage voyagent dans chaque demande, la politique de
     * l'hôte s'y éprouve (un document refusé ne propose RIEN).
     *
     * @param name     le nom du document (l'étiquette), vide permis
     * @param language l'identifiant du langage, vide permis
     */
    public void setDocument(String name, String language) {
        documentName = name == null ? "" : name;
        languageId = language == null ? "" : language;
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
                    strings.aiExplain(), () -> start(AiTask.EXPLAIN,
                            List.of(), -1, -1)));
        }
        if (capabilities.accepts(AiTask.DOCUMENT)) {
            entries.add(new QuickActionCard.Entry(
                    strings.aiDocument(), () -> start(AiTask.DOCUMENT,
                            List.of(), -1, -1)));
        }
        if (capabilities.accepts(AiTask.WRITE_TEST)) {
            entries.add(new QuickActionCard.Entry(
                    strings.aiWriteTest(), () -> start(AiTask.WRITE_TEST,
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
                            AiTask.EXPLAIN_ERROR,
                            List.of(new AiDiagnostic(
                                    AiDiagnostic.Severity.ERROR,
                                    message, 0, 0)),
                            -1, -1)));
        }
        if (capabilities.accepts(AiTask.FIX) && start >= 0 && end >= start) {
            fixes.add(new DiagnosticCard.Fix(
                    strings.aiFix(), () -> start(AiTask.FIX,
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
     * immédiate avec son bouton ANNULER armé.
     *
     * @param task        la tâche demandée
     * @param diagnostics les diagnostics qui voyagent (vide : aucun)
     * @param fixStart    la plage visée par une correction (-1 :
     *                    aucune)
     * @param fixEnd      la plage visée par une correction (-1 :
     *                    aucune)
     */
    private void start(AiTask task, List<AiDiagnostic> diagnostics,
            int fixStart, int fixEnd) {
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
        // diagnostic pour les tâches de l'erreur
        int anchorLine = view.caretLine();
        if (fixStart >= 0) {
            anchorLine = buffer.lineOfOffset(
                    Math.min(fixStart, buffer.length()));
        }
        int anchorColumn = fixStart >= 0
                ? Math.min(fixStart, buffer.length())
                        - buffer.getLineStart(anchorLine)
                : caret - buffer.getLineStart(anchorLine);
        AiRequest request = context.prepare(task, "", source,
                contextChars, null);
        if (request == null) {
            // le crochet de l'hôte a refusé (le filtre d'ordinaire —
            // la politique venait d'être éprouvée) : la phrase le
            // dit, rien ne part
            view.showInfoPopup(strings.assistant(),
                    strings.aiRefused(), anchorLine, anchorColumn);
            return;
        }
        // le vol naît : la génération tue toute réponse d'avant, la
        // carte d'attente vit avec son bouton
        long flight = flights.incrementAndGet();
        Flight listener = new Flight(flight, task,
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
         * @param revision     la révision du tampon au départ
         * @param anchorLine   la ligne d'ancrage
         * @param anchorColumn la colonne d'ancrage
         */
        private Flight(long id, AiTask task, int revision,
                int anchorLine, int anchorColumn) {
            this.id = id;
            this.task = task;
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
                    TextBuffer buffer = view.getBuffer();
                    int start = Math.max(0,
                            Math.min(fix.start(), buffer.length()));
                    int end = Math.max(start,
                            Math.min(fix.end(), buffer.length()));
                    view.applyEdit(start, end, fix.text());
                    view.dismissInfoPopup();
                } else if (result instanceof AiResult.Insertion insert) {
                    if (view.getBuffer().getRevision() != revision) {
                        replace(strings.aiStale(), false);
                        return;
                    }
                    TextBuffer buffer = view.getBuffer();
                    int at = Math.max(0,
                            Math.min(insert.offset(), buffer.length()));
                    view.applyEdit(at, at, insert.text());
                    view.dismissInfoPopup();
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
