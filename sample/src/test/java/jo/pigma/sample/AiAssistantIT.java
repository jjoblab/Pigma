package jo.pigma.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import jo.pigma.ai.AiBackend;
import jo.pigma.ai.AiCall;
import jo.pigma.ai.AiCapabilities;
import jo.pigma.ai.AiError;
import jo.pigma.ai.AiListener;
import jo.pigma.ai.AiRequest;
import jo.pigma.ai.AiResult;
import jo.pigma.ai.AiTask;
import jo.pigma.ai.ContextBuilder;
import jo.pigma.editor.EditorView;
import jo.pigma.editor.ai.AiAssistant;
import jo.pigma.editor.popups.DiagnosticCard;
import jo.pigma.editor.popups.QuickActionCard;
import jo.pigma.syntax.Languages;

/**
 * B2 : l'IA de l'hôte entre dans la vue — la façade branche un
 * moteur FAUX (le harnais y écrit ses réponses) et éprouve le
 * contrat entier : les entrées naissent des CAPACITÉS déclarées et
 * de la POLITIQUE posée, le vol vit ANNULABLE (la carte d'attente,
 * son bouton, la fermeture qui abandonne), la réponse qui se LIT
 * remplace le corps balisé, la réponse qui se POSE se vérifie (un
 * texte qui a bougé ne se pose pas), la panne dit SA phrase — et la
 * demande part avec le CONTEXTE PRÉPARÉ : le bloc englobant déduit
 * de la profondeur d'accolades, la sélection, le préfixe et le
 * suffixe bornés.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class AiAssistantIT {

    private static final int VIEW_WIDTH = 1080;
    private static final int VIEW_HEIGHT = 1800;

    /** Le moteur faux : le harnais y pose ses capacités et son
     * scénario (la réponse immédiate, le vol différé, la panne). */
    static final class FakeBackend implements AiBackend {

        /** Les capacités déclarées (rien ne se propose au-delà). */
        AiCapabilities capabilities = AiCapabilities.NONE;

        /** La dernière demande partie (le harnais l'inspecte). */
        AiRequest lastRequest;

        /** La poignée du dernier vol (le harnais y lit l'annulation). */
        AiCall lastCall;

        /** Le vol fut-il annulé par sa poignée ? */
        boolean cancelled;

        /** L'écouteur du vol en cours (null : aucun vol différé). */
        final AtomicReference<AiListener> pending = new AtomicReference<>();

        /** Le scénario du vol (null : l'écouteur se garde, le vol
         * différé attend sa réponse). */
        Consumer<AiListener> script;

        @Override
        public AiCapabilities capabilities() {
            return capabilities;
        }

        @Override
        public AiCall start(AiRequest request, AiListener listener) {
            lastRequest = request;
            lastCall = new AiCall() {
                @Override
                public void cancel() {
                    cancelled = true;
                }
            };
            pending.set(listener);
            if (script != null) {
                script.accept(listener);
            }
            return lastCall;
        }
    }

    private Activity activity;
    private EditorView editor;
    private Bitmap surface;
    private Canvas canvas;
    private FakeBackend backend;
    private AiAssistant assistant;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        editor = findEditor(activity.findViewById(android.R.id.content));
        assertTrue(editor != null);
        editor.measure(
                View.MeasureSpec.makeMeasureSpec(VIEW_WIDTH,
                        View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(VIEW_HEIGHT,
                        View.MeasureSpec.EXACTLY));
        editor.layout(0, 0, VIEW_WIDTH, VIEW_HEIGHT);
        surface = Bitmap.createBitmap(VIEW_WIDTH, VIEW_HEIGHT,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(surface);
        backend = new FakeBackend();
        assistant = new AiAssistant(editor, backend,
                new ContextBuilder(null, null));
        assistant.setDocument("A.java", "java");
        frame();
        idle();
    }

    /**
     * Les entrées naissent des CAPACITÉS seules : le moteur qui
     * déclare l'explication la propose, celui qui ne déclare rien ne
     * propose RIEN — et la POLITIQUE de l'hôte retranche le
     * document refusé (le filtre par défaut demeure un filet, la
     * politique parle avant).
     */
    @Test
    public void theEntriesFollowTheCapabilitiesAndThePolicy() {
        editor.setLanguage(Languages.JAVA,
                "class A {\n    int total() {\n        return 1;\n    }\n}\n");
        settle();
        backend.capabilities = new AiCapabilities(java.util.Set.of(
                AiTask.EXPLAIN, AiTask.DOCUMENT, AiTask.WRITE_TEST,
                AiTask.EXPLAIN_ERROR, AiTask.FIX), false);
        List<QuickActionCard.Entry> entries = editor.quickActionsOfLine(2);
        assertTrue("l'explication se propose",
                labelOf(entries, "Expliquer") != null);
        assertTrue("la documentation se propose",
                labelOf(entries, "Documenter") != null);
        assertTrue("le test se propose",
                labelOf(entries, "Écrire le test") != null);
        // la correction ne vit pas à l'ampoule : elle vit à l'étage
        // des correctifs de la carte de diagnostic
        assertTrue("la correction ne vit pas à l'ampoule",
                labelOf(entries, "Corriger avec l'assistant") == null);
        // le moteur muet ne propose rien
        backend.capabilities = AiCapabilities.NONE;
        assertTrue("le moteur muet ne propose rien",
                labelOf(editor.quickActionsOfLine(2), "Expliquer") == null);
        // la politique retranche le document refusé
        backend.capabilities = new AiCapabilities(java.util.Set.of(
                AiTask.EXPLAIN), false);
        ContextBuilder strict = new ContextBuilder(
                (name, language) -> false, null);
        new AiAssistant(editor, backend, strict).setDocument(
                "A.java", "java");
        assertTrue("le document refusé ne propose rien",
                labelOf(editor.quickActionsOfLine(2), "Expliquer") == null);
    }

    /**
     * Le vol vit : la carte d'attente paraît immédiate (« Réflexion… »)
     * avec son bouton ANNULER posé à la trame, la réponse qui se LIT
     * remplace le corps BALISÉ — et la demande part avec le contexte
     * préparé : le bloc englobant déduit de la profondeur
     * d'accolades, la sélection, le préfixe et le suffixe autour.
     */
    @Test
    public void theFlightShowsWaitingThenTheAnswer() {
        String code = "class A {\n"
                + "    int total() {\n"
                + "        return 1;\n"
                + "    }\n"
                + "}\n";
        editor.setLanguage(Languages.JAVA, code);
        settle();
        // le curseur AU BLOC du return : le bloc englobant le dit
        int inside = code.indexOf("return");
        editor.caretTo(inside);
        backend.capabilities = new AiCapabilities(
                java.util.Set.of(AiTask.EXPLAIN), false);
        QuickActionCard.Entry explain = labelOf(
                editor.quickActionsOfLine(2), "Expliquer");
        assertNotNull("l'explication se propose", explain);
        explain.action().run();
        idle();
        assertEquals("la carte d'attente vit immédiate",
                "Réflexion…", editor.infoPopupText());
        assertTrue("le bouton d'annulation vit armé",
                editor.isInfoCancelArmed());
        frame();
        RectF pill = editor.infoPopupCancelRect();
        assertFalse("le bouton se pose à la trame", pill.isEmpty());
        // la demande part avec le contexte préparé
        assertNotNull("la demande est partie", backend.lastRequest);
        assertEquals("la tâche voyage", AiTask.EXPLAIN,
                backend.lastRequest.task());
        assertEquals("le nom du document voyage", "A.java",
                backend.lastRequest.documentName());
        assertEquals("le langage voyage", "java",
                backend.lastRequest.languageId());
        assertTrue("le bloc englobant voyage (la profondeur d'accolades le dit)",
                backend.lastRequest.enclosingBlock()
                        .contains("int total()"));
        assertTrue("le préfixe voyage (le texte entier avant le curseur)",
                backend.lastRequest.prefix()
                        .equals(code.substring(0, code.indexOf("return"))));
        assertTrue("le suffixe voyage (le texte entier après le curseur)",
                backend.lastRequest.suffix()
                        .equals(code.substring(code.indexOf("return"))));
        // la réponse qui se LIT remplace le corps balisé
        backend.pending.get().onComplete(new AiResult.Text(
                "Le `total` rend **1**.\n"));
        idle();
        assertEquals("la réponse remplace l'attente",
                "Le `total` rend **1**.\n", editor.infoPopupText());
        assertFalse("le bouton se range à l'issue",
                editor.isInfoCancelArmed());
    }

    /**
     * Le bouton ANNULER coupe le vol AU DOIGT : le tap sur la
     * pastille (la vraie route du toucher) annule la poignée, la
     * phrase ANNULÉE remplace l'attente, la carte VIT (le geste se
     * consomme au bouton) — et la réponse du vol tué SE JETTE.
     */
    @Test
    public void theCancelPillCutsTheFlight() {
        editor.setLanguage(Languages.JAVA, "class A {\n}\n");
        settle();
        editor.caretTo(3);
        backend.capabilities = new AiCapabilities(
                java.util.Set.of(AiTask.EXPLAIN), false);
        QuickActionCard.Entry explain = labelOf(
                editor.quickActionsOfLine(0), "Expliquer");
        assertNotNull("l'explication se propose", explain);
        explain.action().run();
        idle();
        frame();
        RectF pill = editor.infoPopupCancelRect();
        assertFalse("le bouton se pose à la trame", pill.isEmpty());
        // LE TAP RÉEL sur la pastille
        editor.dispatchTouchEvent(MotionEvent.obtain(
                android.os.SystemClock.uptimeMillis(),
                android.os.SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN, pill.centerX(), pill.centerY(), 0));
        editor.dispatchTouchEvent(MotionEvent.obtain(
                android.os.SystemClock.uptimeMillis(),
                android.os.SystemClock.uptimeMillis() + 40L,
                MotionEvent.ACTION_UP, pill.centerX(), pill.centerY(), 0));
        idle();
        assertTrue("la poignée fut annulée", backend.cancelled);
        assertEquals("la phrase d'annulation remplace l'attente",
                "La demande fut annulée.", editor.infoPopupText());
        assertTrue("la carte vit (le geste se consomme au bouton)",
                editor.isInfoPopupVisible());
        // la réponse du vol tué se jette
        backend.pending.get().onComplete(
                new AiResult.Text("Trop tard.\n"));
        idle();
        assertEquals("la réponse du vol tué se jette",
                "La demande fut annulée.", editor.infoPopupText());
    }

    /**
     * La fermeture de la carte d'attente ABANDONNE la question : le
     * vol se coupe (fermer la question, c'est ne plus la payer).
     */
    @Test
    public void closingTheWaitingCardCancelsTheFlight() {
        editor.setLanguage(Languages.JAVA, "class A {\n}\n");
        settle();
        backend.capabilities = new AiCapabilities(
                java.util.Set.of(AiTask.EXPLAIN), false);
        QuickActionCard.Entry explain = labelOf(
                editor.quickActionsOfLine(0), "Expliquer");
        assertNotNull("l'explication se propose", explain);
        explain.action().run();
        idle();
        assertTrue("le vol vit", editor.isInfoCancelArmed());
        editor.dismissInfoPopup();
        idle();
        assertTrue("la fermeture coupe le vol", backend.cancelled);
        assertFalse("le bouton se range", editor.isInfoCancelArmed());
    }

    /**
     * La correction de l'assistant se PROPOSE à l'aperçu (B3 : la
     * réponse qui ÉCRIT ne se pose JAMAIS d'elle-même) : la rangée
     * « Corriger avec l'assistant » vit à l'étage avec les correctifs
     * locaux (elle les RALLONGE), la réponse ouvre LA CARTE
     * (Avant / Après, les trois verdicts), LE TAP RÉEL sur
     * « Accepter » POSE le remplacement par UNE unité d'annulation.
     */
    @Test
    public void theFixProposesItsPreviewThenAppliesAsOneUndoUnit() {
        // une parenthèse ORPHELINE à la fin : le diagnostic local la
        // porte AVEC son correctif propre (l'étage s'y rallonge)
        String base = "class A {\n    int total() {\n        return 1;\n    }\n}\n";
        editor.setLanguage(Languages.JAVA, base);
        settle();
        editor.setSelection(base.length(), base.length());
        android.view.inputmethod.InputConnection connection =
                editor.onCreateInputConnection(
                        new android.view.inputmethod.EditorInfo());
        connection.commitText(")", 1);
        pause();
        frame();
        idle();
        frame();
        idle();
        String code = base + ")";
        int diagLine = editor.getBuffer().getLineCount() - 1;
        List<DiagnosticCard.Entry> before = editor.diagnosticsOfLine(diagLine);
        assertTrue("le diagnostic local vit", !before.isEmpty());
        assertTrue("le correctif local vit",
                !before.get(0).fixes().isEmpty());
        backend.capabilities = new AiCapabilities(java.util.Set.of(
                AiTask.FIX, AiTask.EXPLAIN_ERROR), false);
        List<DiagnosticCard.Entry> found = editor.diagnosticsOfLine(diagLine);
        DiagnosticCard.Fix fix = fixOf(found, "Corriger avec l'assistant");
        assertNotNull("la rangée de l'assistant vit à l'étage", fix);
        assertTrue("l'explication de l'erreur vit aussi",
                fixOf(found, "Expliquer l'erreur") != null);
        assertTrue("les rangées de l'assistant rallongent l'étage",
                found.get(0).fixes().size() >= 3);
        // la demande porte le diagnostic (message et plage vraie)
        fix.action().run();
        idle();
        assertEquals("la tâche de correction voyage", AiTask.FIX,
                backend.lastRequest.task());
        assertEquals("le diagnostic voyage (la plage de l'orphelin)",
                code.length() - 1,
                backend.lastRequest.diagnostics().get(0).start());
        // la réponse ouvre L'APERÇU (rien ne se pose sans verdict)
        backend.pending.get().onComplete(new AiResult.Replacement(
                code.length() - 1, code.length(), ""));
        idle();
        frame();
        assertTrue("l'aperçu de la modification vit",
                editor.aiDiffCard() != null && editor.aiDiffCard().isShown());
        assertEquals("l'aperçu porte l'AVANT (l'orphelin)", ")",
                editor.aiDiffCard().beforeText());
        assertEquals("l'aperçu porte l'APRÈS (le retrait)", "",
                editor.aiDiffCard().afterText());
        assertEquals("rien ne se pose sans verdict", code,
                editor.getBuffer().toString());
        // LE TAP RÉEL sur « Accepter » POSE le remplacement
        tap(editor.aiDiffCard().verdictCenterX(0),
                editor.aiDiffCard().verdictCenterY());
        idle();
        assertEquals("le remplacement accepté se pose", base,
                editor.getBuffer().toString());
        editor.undo();
        assertEquals("la pose vaut UNE unité d'annulation", code,
                editor.getBuffer().toString());
        // la pose ne née aucune erreur neuve : aucune phrase
        pause();
        frame();
        idle();
        assertFalse("aucune erreur née n'est signalée",
                editor.infoPopupText() != null
                && editor.infoPopupText().contains("erreurs"));
    }

    /**
     * Le texte qui bouge pendant le vol coupe le vol : la carte
     * d'attente EST la question, l'édition la masque (l'ancre ne
     * désigne plus rien) — sa fermeture l'annule (la réponse
     * parla de l'ancien texte : la poser corromprait le neuf). La
     * réponse tardive du vol tué SE JETTE : rien ne se pose.
     */
    @Test
    public void theEditDuringFlightCancelsItAndTheAnswerIsDropped() {
        String base = "class A {\n    int total() {\n        return 1;\n    }\n}\n";
        editor.setLanguage(Languages.JAVA, base);
        settle();
        editor.setSelection(base.length(), base.length());
        android.view.inputmethod.InputConnection connection =
                editor.onCreateInputConnection(
                        new android.view.inputmethod.EditorInfo());
        connection.commitText(")", 1);
        pause();
        frame();
        idle();
        frame();
        idle();
        String code = base + ")";
        int diagLine = editor.getBuffer().getLineCount() - 1;
        backend.capabilities = new AiCapabilities(
                java.util.Set.of(AiTask.FIX), false);
        DiagnosticCard.Fix fix = fixOf(editor.diagnosticsOfLine(diagLine),
                "Corriger avec l'assistant");
        assertNotNull("la rangée de l'assistant vit à l'étage", fix);
        fix.action().run();
        idle();
        assertTrue("le vol vit", editor.isInfoCancelArmed());
        // le texte BOUGE pendant le vol : la carte meurt (l'ancre ne
        // désigne plus rien) — sa fermeture coupe le vol
        editor.applyEdit(0, 0, "// parti\n");
        idle();
        assertTrue("l'édition coupe le vol", backend.cancelled);
        assertFalse("la carte d'attente meurt à l'édition",
                editor.isInfoPopupVisible());
        // la réponse du vol tué se jette : le texte demeure
        backend.pending.get().onComplete(new AiResult.Replacement(
                0, editor.getBuffer().length(), "tout neuf"));
        idle();
        assertEquals("la réponse du vol tué ne se pose pas",
                "// parti\n" + code,
                editor.getBuffer().toString());
    }

    /**
     * LA PASTILLE « Modifier avec l'IA… » (B3) ne vit qu'aux
     * CAPACITÉS déclarées et à la POLITIQUE du document : le moteur
     * qui ne déclare pas EDIT_SELECTION ne la pose pas, celui qui
     * la déclare la pose, le document refusé la retire — LE TAP RÉEL
     * sur la pastille ouvre la BARRE D'INSTRUCTION.
     */
    @Test
    public void theEditSelectionPillFollowsTheCapabilitiesAndThePolicy() {
        String base = "class A {\n    int total() {\n        return 1;\n    }\n}\n";
        editor.setLanguage(Languages.JAVA, base);
        settle();
        // la SÉLECTION vit (la pastille ne s'ouvre qu'à elle)
        editor.setSelection(base.indexOf("return"), base.indexOf("return") + 6);
        frame();
        idle();
        assertTrue("la barre de sélection vit",
                editor.selectionBar().isShown());
        assertFalse("le moteur muet ne pose pas la pastille",
                editor.selectionBar().hasChip("Modifier avec l'IA…"));
        // le moteur qui DÉCLARE la tâche pose la pastille
        backend.capabilities = new AiCapabilities(
                java.util.Set.of(AiTask.EDIT_SELECTION), false);
        assistant.refreshEditAction();
        frame();
        idle();
        assertTrue("la pastille vit aux capacités déclarées",
                editor.selectionBar().hasChip("Modifier avec l'IA…"));
        // LE TAP RÉEL sur la pastille ouvre la barre d'instruction
        tap(editor.selectionBar().chipCenterX("Modifier avec l'IA…"),
                editor.selectionBar().centerY());
        idle();
        assertTrue("la barre d'instruction vit",
                editor.aiEditBar() != null && editor.aiEditBar().isShown());
        editor.dismissAiEditPopup();
        // le moteur qui RETIRE la tâche ôte la pastille
        backend.capabilities = AiCapabilities.NONE;
        assistant.refreshEditAction();
        frame();
        idle();
        assertFalse("la pastille meurt aux capacités retirées",
                editor.selectionBar().hasChip("Modifier avec l'IA…"));
        // la politique refusée ôte la pastille aussi
        backend.capabilities = new AiCapabilities(
                java.util.Set.of(AiTask.EDIT_SELECTION), false);
        ContextBuilder strict = new ContextBuilder(
                (name, language) -> false, null);
        AiAssistant refused = new AiAssistant(editor, backend, strict);
        refused.setDocument("A.java", "java");
        frame();
        idle();
        assertFalse("le document refusé ne porte pas la pastille",
                editor.selectionBar().hasChip("Modifier avec l'IA…"));
        refused.detach();
        assistant.refreshEditAction();
        frame();
        idle();
        assertTrue("la pastille revient au document autorisé",
                editor.selectionBar().hasChip("Modifier avec l'IA…"));
    }

    /**
     * L'INSTRUCTION voyage (B3) : la frappe déléguée écrit la barre
     * (rien ne s'écrit dans le document), la validation démarre le
     * vol — la tâche EDIT_SELECTION, l'instruction ET la sélection
     * voyagent dans la demande.
     */
    @Test
    public void theInstructionTravelsToTheFlight() {
        String base = "class A {\n    int total() {\n        return 1;\n    }\n}\n";
        editor.setLanguage(Languages.JAVA, base);
        settle();
        int selStart = base.indexOf("1");
        editor.setSelection(selStart, selStart + 1);
        backend.capabilities = new AiCapabilities(
                java.util.Set.of(AiTask.EDIT_SELECTION), false);
        assistant.refreshEditAction();
        frame();
        idle();
        // LE TAP RÉEL ouvre la barre
        tap(editor.selectionBar().chipCenterX("Modifier avec l'IA…"),
                editor.selectionBar().centerY());
        idle();
        assertTrue("la barre d'instruction vit",
                editor.aiEditBar() != null && editor.aiEditBar().isShown());
        // la frappe DÉLÉGUÉE écrit la barre (le document ne bouge pas)
        android.view.inputmethod.InputConnection connection =
                editor.onCreateInputConnection(
                        new android.view.inputmethod.EditorInfo());
        connection.commitText("double la valeur", 1);
        idle();
        assertEquals("l'instruction s'écrit à la barre",
                "double la valeur", editor.aiEditBar().instructionText());
        assertEquals("rien ne s'écrit dans le document", base,
                editor.getBuffer().toString());
        // la VALIDATION démarre le vol : la tâche, l'instruction ET
        // la sélection voyagent
        connection.commitText("\n", 1);
        idle();
        assertNotNull("la demande est partie", backend.lastRequest);
        assertEquals("la tâche de modification voyage",
                AiTask.EDIT_SELECTION, backend.lastRequest.task());
        assertEquals("l'instruction voyage", "double la valeur",
                backend.lastRequest.instruction());
        assertEquals("la sélection voyage (son texte)", "1",
                backend.lastRequest.selection());
        assertFalse("la barle se range au départ du vol",
                editor.aiEditBar().isShown());
        assertTrue("la carte d'attente vit",
                editor.isInfoCancelArmed());
    }

    /**
     * Les TROIS VERDICTS de l'aperçu (B3) : la réponse qui ÉCRIT se
     * PROPOSE — REJETER jette (le tampon n'a jamais bougé),
     * RÉESSAYER repose la question à l'identique (un vol neuf), et
     * la réponse périmée (le texte a bougé depuis la demande) ne se
     * POSE pas : la phrase du texte changé le dit.
     */
    @Test
    public void theVerdictsOfThePreviewDecide() {
        String base = "class A {\n    int total() {\n        return 1;\n    }\n}\n";
        editor.setLanguage(Languages.JAVA, base);
        settle();
        int selStart = base.indexOf("1");
        editor.setSelection(selStart, selStart + 1);
        backend.capabilities = new AiCapabilities(
                java.util.Set.of(AiTask.EDIT_SELECTION), false);
        assistant.refreshEditAction();
        frame();
        idle();
        tap(editor.selectionBar().chipCenterX("Modifier avec l'IA…"),
                editor.selectionBar().centerY());
        idle();
        android.view.inputmethod.InputConnection connection =
                editor.onCreateInputConnection(
                        new android.view.inputmethod.EditorInfo());
        connection.commitText("double la valeur", 1);
        idle();
        connection.commitText("\n", 1);
        idle();
        // la réponse PROPOSE : la carte vit, rien ne se pose
        backend.pending.get().onComplete(new AiResult.Replacement(
                selStart, selStart + 1, "2"));
        idle();
        frame();
        assertTrue("l'aperçu vit",
                editor.aiDiffCard() != null && editor.aiDiffCard().isShown());
        // REJETER : le tampon n'a jamais bougé
        tap(editor.aiDiffCard().verdictCenterX(1),
                editor.aiDiffCard().verdictCenterY());
        idle();
        assertFalse("le rejet range l'aperçu",
                editor.aiDiffCard().isShown());
        assertEquals("le rejet ne touche pas le tampon", base,
                editor.getBuffer().toString());
        // RÉESSAYER : la question repart à l'identique
        connection = editor.onCreateInputConnection(
                new android.view.inputmethod.EditorInfo());
        // rouvrir par la pastille (la carte se range au rejet)
        backend.pending.set(null);
        tap(editor.selectionBar().chipCenterX("Modifier avec l'IA…"),
                editor.selectionBar().centerY());
        idle();
        assertTrue("la barre rouverte vit",
                editor.aiEditBar() != null && editor.aiEditBar().isShown());
        connection.commitText("encore", 1);
        idle();
        connection.commitText("\n", 1);
        idle();
        assertTrue("le vol neuf vit", editor.isInfoCancelArmed());
        backend.pending.get().onComplete(new AiResult.Replacement(
                selStart, selStart + 1, "3"));
        idle();
        frame();
        assertTrue("l'aperçu revit", editor.aiDiffCard().isShown());
        // LE TAP RÉEL sur « Réessayer » repose la question
        tap(editor.aiDiffCard().verdictCenterX(2),
                editor.aiDiffCard().verdictCenterY());
        idle();
        assertNotNull("la question repart", backend.lastRequest);
        assertEquals("la tâche repart à l'identique",
                AiTask.EDIT_SELECTION, backend.lastRequest.task());
        assertTrue("la carte d'attente revit", editor.isInfoCancelArmed());
        // la réponse périmée ne se POSE pas : le texte a bougé depuis
        // la demande — la phrase le dit, l'aperçu ne rouvre pas
        editor.applyEdit(0, 0, "// parti\n");
        idle();
        assertFalse("l'édition masque l'attente (le vol se coupe)",
                editor.isInfoCancelArmed());
        assertEquals("le tampon demeure (l'attente morte ne pose rien)",
                "// parti\n" + base, editor.getBuffer().toString());
    }

    /**
     * Les ERREURS NÉES se signalent (B3) : l'acceptation qui fait
     * naître une erreur neuve dans la zone posée le DIT — le modèle
     * propose, le moteur local vérifie, l'utilisateur valide (la
     * seule vérification que Pigma peut faire).
     */
    @Test
    public void theNewErrorsAreSignalled() {
        String base = "class A {\n    int total() {\n        return 1;\n    }\n}\n";
        editor.setLanguage(Languages.JAVA, base);
        settle();
        int selStart = base.indexOf("1");
        editor.setSelection(selStart, selStart + 1);
        backend.capabilities = new AiCapabilities(
                java.util.Set.of(AiTask.EDIT_SELECTION), false);
        assistant.refreshEditAction();
        frame();
        idle();
        tap(editor.selectionBar().chipCenterX("Modifier avec l'IA…"),
                editor.selectionBar().centerY());
        idle();
        android.view.inputmethod.InputConnection connection =
                editor.onCreateInputConnection(
                        new android.view.inputmethod.EditorInfo());
        connection.commitText("un orphelin", 1);
        idle();
        connection.commitText("\n", 1);
        idle();
        // la réponse PROPOSE un orphelin : l'acceptation le pose
        backend.pending.get().onComplete(new AiResult.Replacement(
                selStart, selStart + 1, ")"));
        idle();
        frame();
        assertTrue("l'aperçu vit",
                editor.aiDiffCard() != null && editor.aiDiffCard().isShown());
        tap(editor.aiDiffCard().verdictCenterX(0),
                editor.aiDiffCard().verdictCenterY());
        idle();
        assertTrue("la pose s'est faite", editor.getBuffer().toString()
                .contains("return )"));
        // la passe close dit l'erreur née
        pause();
        frame();
        idle();
        pause();
        frame();
        idle();
        assertEquals("l'erreur née dit SA phrase",
                "De nouvelles erreurs sont nées de la modification — vérifie la zone.",
                editor.infoPopupText());
    }

    /**
     * B5 — LE COMMENTAIRE DE DOCUMENTATION se pose AU-DESSUS DE LA
     * DÉCLARATION : le moteur rend le texte, la STRUCTURE dit le
     * point de pose (la déclaration qui enserre le curseur — la plus
     * interne, la méthode devant sa classe) ; l'aperçu le propose,
     * l'acceptation le pose par UNE unité d'annulation.
     */
    @Test
    public void theDocumentLandsAboveTheDeclaration() {
        String base = "class A {\n"
                + "    int total() {\n"
                + "        return 1;\n"
                + "    }\n"
                + "}\n";
        editor.setLanguage(Languages.JAVA, base);
        settle();
        // le curseur DANS la méthode (la ligne 2) : la déclaration qui
        // l'enserre vit à la ligne 1
        editor.caretTo(base.indexOf("return"));
        backend.capabilities = new AiCapabilities(
                java.util.Set.of(AiTask.DOCUMENT), false);
        QuickActionCard.Entry document = labelOf(
                editor.quickActionsOfLine(2), "Documenter");
        assertNotNull("la documentation se propose", document);
        document.action().run();
        idle();
        assertEquals("la tâche de documentation voyage", AiTask.DOCUMENT,
                backend.lastRequest.task());
        // le moteur rend un texte qui visait le CARET : la façade le
        // ré-ancre À LA DÉCLARATION
        backend.pending.get().onComplete(new AiResult.Insertion(
                base.indexOf("return"), "/** Le total rendu. */\n"));
        idle();
        frame();
        assertTrue("l'aperçu de l'insertion vit",
                editor.aiDiffCard() != null && editor.aiDiffCard().isShown());
        assertEquals("l'aperçu porte l'AVANT vide (une insertion)", "",
                editor.aiDiffCard().beforeText());
        assertEquals("l'aperçu porte l'APRÈS (le commentaire)",
                "/** Le total rendu. */\n",
                editor.aiDiffCard().afterText());
        // L'ACCEPTATION pose le commentaire AU-DESSUS de la déclaration
        tap(editor.aiDiffCard().verdictCenterX(0),
                editor.aiDiffCard().verdictCenterY());
        idle();
        String expected = "class A {\n"
                + "/** Le total rendu. */\n"
                + "    int total() {\n"
                + "        return 1;\n"
                + "    }\n"
                + "}\n";
        assertEquals("le commentaire se pose au-dessus de la déclaration",
                expected, editor.getBuffer().toString());
        editor.undo();
        assertEquals("la pose vaut UNE unité d'annulation", base,
                editor.getBuffer().toString());
        // un commentaire ne née aucune erreur : aucune phrase
        pause();
        frame();
        idle();
        assertFalse("aucune erreur née n'est signalée",
                editor.infoPopupText() != null
                && editor.infoPopupText().contains("erreurs"));
    }

    /**
     * B5 — LE TEST ÉCRIT se rend À L'HÔTE (lui seul sait où créer un
     * fichier — Pigma n'en crée jamais) ET se lit à la popup (balisé
     * tel quel : l'aperçu du texte qui se lit).
     */
    @Test
    public void theWrittenTestIsRenderedToTheHost() {
        editor.setLanguage(Languages.JAVA, "class A {\n}\n");
        settle();
        backend.capabilities = new AiCapabilities(
                java.util.Set.of(AiTask.WRITE_TEST), false);
        java.util.List<String> received = new java.util.ArrayList<>();
        assistant.setTestListener(received::add);
        QuickActionCard.Entry writeTest = labelOf(
                editor.quickActionsOfLine(0), "Écrire le test");
        assertNotNull("l'écriture du test se propose", writeTest);
        writeTest.action().run();
        idle();
        assertEquals("la tâche du test voyage", AiTask.WRITE_TEST,
                backend.lastRequest.task());
        String test = "public class ATest {\n"
                + "    @Test public void total() {\n"
                + "        assertEquals(1, new A().total());\n"
                + "    }\n"
                + "}\n";
        backend.pending.get().onComplete(new AiResult.Text(test));
        idle();
        assertEquals("le test se rend à l'hôte (une fois)", 1, received.size());
        assertEquals("le texte rendu est le texte du moteur", test,
                received.get(0));
        assertEquals("le test se lit à la popup (balisé tel quel)", test,
                editor.infoPopupText());
    }

    /**
     * B7 — L'ANNONCE DÉDIÉE : la carte de l'assistant s'ouvre à SA
     * voix (« Assistant ouvert » — le lecteur d'écran sait QUI
     * parle), la documentation garde la sienne ; et LA CIBLE du
     * bouton ANNULER respire à 48 dp (la pastille dessinée à 28 —
     * le doigt vise plus large, borné à la boîte).
     */
    @Test
    public void theAssistantSpeaksItsOwnAnnouncement() {
        editor.setLanguage(Languages.JAVA, "class A {\n}\n");
        settle();
        backend.capabilities = new AiCapabilities(
                java.util.Set.of(AiTask.EXPLAIN), false);
        // LA VOIX DE L'ASSISTANT : l'attente s'annonce à lui
        QuickActionCard.Entry explain = labelOf(
                editor.quickActionsOfLine(0), "Expliquer");
        assertNotNull("l'explication se propose", explain);
        explain.action().run();
        idle();
        assertEquals("l'attente s'annonce à la voix de l'assistant",
                "Assistant ouvert", editor.lastAnnouncedPopupLabel());
        editor.dismissInfoPopup();
        idle();
        // LA VOIX DE LA DOCUMENTATION demeure la sienne
        editor.showInfoPopup("mot", "le corps de la documentation", 0, 0);
        idle();
        assertEquals("la documentation garde sa voix",
                "Documentation ouverte", editor.lastAnnouncedPopupLabel());
        editor.dismissInfoPopup();
        idle();
        // LA CIBLE ≥ 48 dp : le doigt 10 dp AU-DESSUS de la pastille
        // dessinée coupe le vol pareillement
        explain.action().run();
        idle();
        frame();
        RectF pill = editor.infoPopupCancelRect();
        assertFalse("le bouton se pose à la trame", pill.isEmpty());
        tap(pill.centerX(), pill.top - 10.0f);
        assertTrue("la cible élargie coupe le vol", backend.cancelled);
        assertEquals("la phrase d'annulation remplace l'attente",
                "La demande fut annulée.", editor.infoPopupText());
    }

    /**
     * La panne dit SA phrase (une par famille — le détail du moteur
     * ne se montre pas) ; la réponse muette dit la sienne.
     */
    @Test
    public void theErrorSaysItsFamilyPhrase() {
        editor.setLanguage(Languages.JAVA, "class A {\n}\n");
        settle();
        backend.capabilities = new AiCapabilities(
                java.util.Set.of(AiTask.EXPLAIN), false);
        QuickActionCard.Entry explain = labelOf(
                editor.quickActionsOfLine(0), "Expliquer");
        assertNotNull("l'explication se propose", explain);
        backend.script = listener -> listener.onError(
                new AiError(AiError.Category.TIMEOUT, "détail du moteur"));
        explain.action().run();
        idle();
        assertEquals("la phrase de la panne de délai",
                "Le temps de réponse est écoulé.", editor.infoPopupText());
        assertFalse("le bouton se range à l'issue",
                editor.isInfoCancelArmed());
    }

    /** Le libellé d'une entrée de l'ampoule (null : absente). */
    private static QuickActionCard.Entry labelOf(
            List<QuickActionCard.Entry> entries, String label) {
        for (QuickActionCard.Entry entry : entries) {
            if (label.equals(entry.label())) {
                return entry;
            }
        }
        return null;
    }

    /** La rangée d'un correctif par son titre (null : absente). */
    private static DiagnosticCard.Fix fixOf(
            List<DiagnosticCard.Entry> entries, String title) {
        for (DiagnosticCard.Entry entry : entries) {
            for (DiagnosticCard.Fix fix : entry.fixes()) {
                if (title.equals(fix.title())) {
                    return fix;
                }
            }
        }
        return null;
    }

    private void pause() {
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        idle();
    }

    /** LE TAP RÉEL : le doigt tombe puis se relève (la vraie route du
     * toucher — le DOWN arme, l'UP joue). */
    private void tap(float x, float y) {
        editor.dispatchTouchEvent(MotionEvent.obtain(
                android.os.SystemClock.uptimeMillis(),
                android.os.SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN, x, y, 0));
        editor.dispatchTouchEvent(MotionEvent.obtain(
                android.os.SystemClock.uptimeMillis(),
                android.os.SystemClock.uptimeMillis() + 40L,
                MotionEvent.ACTION_UP, x, y, 0));
        idle();
    }

    /**
     * Le document se pose : la trame lance le remplissage de fond,
     * l'attente dorée le laisse finir (le lexage complet y précède
     * l'analyse), la trame suivante lance la passe, la file vide ses
     * tranches — les diagnostics du document s'y lisent.
     */
    private void settle() {
        frame();
        assertTrue("le remplissage de fond finit",
                editor.awaitBackgroundFill(20_000L));
        idle();
        frame();
        idle();
        frame();
        idle();
    }

    private void frame() {
        surface.eraseColor(0);
        editor.draw(canvas);
    }

    private void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private static EditorView findEditor(View root) {
        if (root instanceof EditorView) {
            return (EditorView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int at = 0; at < group.getChildCount(); at++) {
                EditorView found = findEditor(group.getChildAt(at));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
