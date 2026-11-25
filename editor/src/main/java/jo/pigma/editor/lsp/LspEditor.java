package jo.pigma.editor.lsp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.lsp4j.services.LanguageServer;

import jo.pigma.core.TextBuffer;
import jo.pigma.core.TextListener;
import jo.pigma.editor.EditorView;
import jo.pigma.editor.popups.DiagnosticCard;
import jo.pigma.lsp.LspDiagnostic;
import jo.pigma.lsp.LspPosition;
import jo.pigma.lsp.LspSession;

/**
 * La façade du serveur de langage : l'hôte n'a plus RIEN à câbler.
 * L'étude de la référence (le module {@code editor-lsp} de Sora
 * editor) y a porté sa leçon — là-bas, {@code LspEditor} attache le
 * serveur à la vue, porte la synchronisation du document, les
 * fournisseurs et leurs caches ; l'hôte de la référence pose la vue,
 * branche, écrit. Ici pareillement : {@link #attach(EditorView)}
 * prend la vue, {@link #connectInProcess}/{@link #connectProcess}/
 * {@link #connect} branchent, le reste vit seul — la
 * synchronisation du document (le {@code didChange} amorti aux
 * 300 ms, l'ombre du texte au protocole), le survol (la réponse du
 * serveur prime sur l'index local, l'asynchrone remplace la
 * réponse lente), la complétion (le cache par préfixe, la réponse
 * asynchrone évince l'ancienne), l'aide aux paramètres (le cache
 * par curseur, la génération déclasse la réponse d'avant), les
 * diagnostics (la carte de diagnostic les porte, la gouttière en
 * vit). Les réglages de la référence y suivent : les fonctions se
 * coupent une à une ({@link #setHoverEnabled} et ses sœurs), le
 * temps d'attente du survol se pose ({@link #setRequestTimeoutMs}),
 * l'hôte lit les diagnostics pour son panneau
 * ({@link #setDiagnosticsListener}).
 *
 * <p>La façade vit attachée à UNE vue ; le débranchement
 * ({@link #disconnect}) coupe tout : les fournisseurs se retirent,
 * les caches se rangent, la liaison s'arrête (le processus meurt).
 * Une liaison neuve se branche à la même vue sans frais.</p>
 */
public final class LspEditor {

    /**
     * L'écouteur des diagnostics du serveur : le panneau de l'hôte s'y
     * rafraîchit (le compte, la liste). L'appel arrive sur le fil de
     * l'interface (la façade y marshalle l'avis du lecteur).
     */
    public interface DiagnosticsListener {

        /**
         * Le serveur a publié la liste ENTIÈRE des diagnostics du
         * document (le protocole est à remplacement).
         *
         * @param diagnostics la liste complète, jamais {@code null}
         */
        void onDiagnostics(List<LspDiagnostic> diagnostics);
    }

    /** Le délai amorti du document vers le serveur (les frappes rapides n'envoient qu'un envoi). */
    private static final long DOCUMENT_SYNC_MS = 300L;

    /** La vue branchée (les fournisseurs y vivent, le tampon y dort). */
    private final EditorView view;

    /** La liaison vivante (null : aucun serveur). */
    private LspSession session;

    /** Le document courant côté serveur (l'URI du protocole). */
    private String uri;

    /** Le survol demande-t-il au serveur (la fonction coupée retombe sur l'index local). */
    private boolean hoverEnabled = true;

    /** La complétion demande-t-elle au serveur (la fonction coupée rend la carte vide). */
    private boolean completionEnabled = true;

    /** L'aide aux paramètres demande-t-elle au serveur (la fonction coupée range la carte). */
    private boolean signatureEnabled = true;

    /** Les diagnostics montent-ils à la carte (la fonction coupée la rend muette). */
    private boolean diagnosticsEnabled = true;

    /** Le temps d'attente du survol en millisecondes (au-delà, la réponse lente arrivera en asynchrone). */
    private long requestTimeoutMs = 200L;

    /** Le préfixe du dernier envoi de complétion (la clef du cache). */
    private volatile String completionPrefix;

    /** Les libellés reçus du serveur pour ce préfixe (le fil du lecteur y écrit). */
    private final List<String> completionCache =
            Collections.synchronizedList(new ArrayList<>());

    /** La génération de la demande de signature vivante (le texte change : la réponse en vol se périme). */
    private final AtomicLong signatureGeneration = new AtomicLong();

    /** Le curseur de la dernière demande de signature (la clef du cache). */
    private volatile int signatureCaret = -1;

    /** La dernière signature reçue du serveur (null : pas de réponse). */
    private volatile EditorView.SignatureHelp signatureAnswer;

    /** Les diagnostics du dernier avis du serveur. */
    private volatile List<LspDiagnostic> diagnostics = List.of();

    /** L'écouteur de l'hôte (null : personne n'écoute). */
    private volatile DiagnosticsListener diagnosticsListener;

    /** Le report du changement de document vers le serveur (la synchronisation intégrale s'amortit). */
    private final Runnable documentSync = this::pushDocumentToServer;

    /** L'écouteur du tampon : chaque édition reprogramme l'envoi du document. */
    private final TextListener textListener = new TextListener() {
        @Override
        public void textChanged(TextBuffer changed, int start, int removedLength,
                int insertedLength, int revision) {
            scheduleDocumentSync();
        }
    };

    /**
     * Attache la façade à une vue : l'écouteur du tampon s'y pose (il
     * ne coûte rien sans liaison), les fournisseurs ne se posent
     * qu'au branchement.
     *
     * @param view la vue de l'éditeur, jamais {@code null}
     * @return la façade prête à recevoir un serveur
     */
    public static LspEditor attach(EditorView view) {
        return new LspEditor(view);
    }

    private LspEditor(EditorView view) {
        this.view = view;
        view.getBuffer().addListener(textListener);
    }

    /**
     * Branche un serveur DANS le même processus : lsp4j porte le
     * protocole (JSON-RPC et gson dessous), les tubes ne quittent
     * pas la machine. Le document part au serveur ({@code didOpen}
     * du texte courant), les fournisseurs se posent.
     *
     * @param server     le serveur de langage, jamais {@code null}
     * @param uri        l'URI du document au protocole
     * @param languageId l'identifiant du langage au protocole ("java", "lua"…)
     * @throws IOException si la liaison ne monte pas (les tubes)
     */
    public void connectInProcess(LanguageServer server, String uri, String languageId)
            throws IOException {
        connect(LspSession.beside(server, sessionListener()), uri, languageId);
    }

    /**
     * Branche un serveur lancé PAR PROCESSUS (l'exécutable vit hors
     * de la machine virtuelle) : lsp4j cadre les messages sur son
     * entrée et sa sortie standard.
     *
     * @param process    le serveur vivant, jamais {@code null}
     * @param uri       l'URI du document au protocole
     * @param languageId l'identifiant du langage au protocole
     */
    public void connectProcess(Process process, String uri, String languageId) {
        connect(LspSession.overProcess(process, sessionListener()), uri, languageId);
    }

    /**
     * Branche une liaison déjà montée (des flux donnés, un serveur
     * déjà lancé) : la négociation d'initialisation y court, le
     * document part au serveur, les fournisseurs se posent. Une
     * liaison vivante se débranche d'abord ({@link #disconnect()}).
     *
     * @param liaison   la liaison montée, jamais {@code null}
     * @param uri       l'URI du document au protocole
     * @param languageId l'identifiant du langage au protocole
     */
    public void connect(LspSession liaison, String uri, String languageId) {
        if (this.session != null) {
            disconnect();
        }
        this.session = liaison;
        this.uri = uri;
        resetProviders();
        liaison.initialize("pigma");
        liaison.didOpen(uri, view.getBuffer().toString(), languageId);
        attachProviders();
        // la trame qui suit porte les marqueurs du premier avis
        view.post(view::invalidate);
    }

    /**
     * La liaison vit-elle encore (un serveur branché, pas encore
     * débranché) ?
     *
     * @return vrai si un serveur y vit
     */
    public boolean isConnected() {
        return session != null;
    }

    /**
     * Débranche la liaison vivante : les fournisseurs se retirent, les
     * caches se rangent, la liaison s'arrête (le processus meurt),
     * les diagnostics s'oublient.
     */
    public void disconnect() {
        if (session == null) {
            return;
        }
        view.removeCallbacks(documentSync);
        view.setInfoProvider(null);
        view.setSuggestionProvider(null);
        view.setSignatureProvider(null);
        view.setDiagnosticProvider(null);
        resetProviders();
        session.shutdown();
        session = null;
        uri = null;
        view.post(view::invalidate);
    }

    /**
     * Le survol demande-t-il au serveur ?
     *
     * @param enabled vrai pour demander (vaut par défaut)
     */
    public void setHoverEnabled(boolean enabled) {
        hoverEnabled = enabled;
    }

    /**
     * La complétion demande-t-elle au serveur ?
     *
     * @param enabled vrai pour demander (vaut par défaut)
     */
    public void setCompletionEnabled(boolean enabled) {
        completionEnabled = enabled;
    }

    /**
     * L'aide aux paramètres demande-t-elle au serveur ?
     *
     * @param enabled vrai pour demander (vaut par défaut)
     */
    public void setSignatureHelpEnabled(boolean enabled) {
        signatureEnabled = enabled;
    }

    /**
     * Les diagnostics montent-ils à la carte (et à la gouttière) ?
     *
     * @param enabled vrai pour monter (vaut par défaut)
     */
    public void setDiagnosticsEnabled(boolean enabled) {
        diagnosticsEnabled = enabled;
        view.post(view::invalidate);
    }

    /**
     * Le temps d'attente du survol : au-delà, la réponse lente
     * gardera sa voie — elle remplacera la popup à son arrivée, si
     * la question vit toujours à la même position.
     *
     * @param timeoutMs le temps d'attente en millisecondes
     */
    public void setRequestTimeoutMs(long timeoutMs) {
        requestTimeoutMs = timeoutMs;
    }

    /**
     * L'écouteur des diagnostics du serveur : le panneau de l'hôte s'y
     * rafraîchit (l'appel arrive sur le fil de l'interface, la
     * liste y est entière).
     *
     * @param listener l'écouteur (null : personne n'écoute)
     */
    public void setDiagnosticsListener(DiagnosticsListener listener) {
        diagnosticsListener = listener;
    }

    /**
     * Les diagnostics du dernier avis du serveur.
     *
     * @return la liste entière, jamais {@code null} (vide : aucun)
     */
    public List<LspDiagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Le compte des diagnostics du dernier avis (le panneau y lit
     * son chiffre).
     *
     * @return le compte
     */
    public int diagnosticCount() {
        return diagnostics.size();
    }

    /**
     * L'écouteur commun des liaisons : les diagnostics du serveur
     * montent au panneau de l'hôte ET à la carte de diagnostic (le
     * compte se rafraîchit sur le fil de l'interface).
     *
     * @return l'écouteur branché à la liaison
     */
    private LspSession.Listener sessionListener() {
        return new LspSession.Listener() {
            @Override
            public void onDiagnostics(String published, List<LspDiagnostic> fresh) {
                diagnostics = List.copyOf(fresh);
                view.post(() -> {
                    if (diagnosticsListener != null) {
                        diagnosticsListener.onDiagnostics(diagnostics);
                    }
                    view.invalidate();
                });
            }
        };
    }

    /**
     * Les fournisseurs de la liaison branchée : la popup d'info
     * interroge le survol (la réponse du serveur prime sur l'index
     * local — le fournisseur rend null quand le serveur ne sait pas
     * ou quand la fonction se coupe) ; la complétion du serveur
     * monte à la carte (la vue tire à chaque trame — le fournisseur
     * cache par préfixe, la réponse revient asynchrone sur le fil du
     * lecteur : la neuve évince l'ancienne si le préfixe n'a pas
     * bougé, la trame suivante la porte) ; l'aide aux paramètres du
     * serveur prime sur l'index local (la réponse y arrive
     * pareillement asynchrone) ; les diagnostics de la ligne montent
     * à la carte (le marqueur de la gouttière en vit, le doigt y
     * l'ouvre).
     */
    private void attachProviders() {
        final String documentUri = uri;
        // le survol du serveur prime : la réponse attend au plus le
        // temps posé ; au-delà, la popup locale tient la place et la
        // réponse lente la remplacera à son arrivée (le survol
        // asynchrone — la voie du serveur réel)
        view.setInfoProvider((line, column, word) -> {
            if (!hoverEnabled || session == null) {
                return null;
            }
            CompletableFuture<String> hover =
                    session.hover(documentUri, new LspPosition(line, column));
            try {
                return hover.get(requestTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException late) {
                hover.whenComplete((answer, failure) -> {
                    if (answer != null && !answer.isEmpty()) {
                        view.post(() -> view.replaceInfoPopup(answer, line, column));
                    }
                });
            } catch (Exception noAnswer) {
                return null;
            }
            return null;
        });
        // la complétion du serveur à la carte : le cache par préfixe
        // (le même préfixe y rend la même liste, O(1)), la réponse
        // asynchrone évince l'ancienne si le préfixe n'a pas bougé
        view.setSuggestionProvider((prefix, line, column) -> {
            if (!completionEnabled || session == null || prefix.isEmpty()) {
                return List.of();
            }
            if (!prefix.equals(completionPrefix)) {
                completionPrefix = prefix;
                completionCache.clear();
                final String asked = prefix;
                session.completion(documentUri, new LspPosition(line, column))
                        .whenComplete((words, failure) -> {
                            if (asked.equals(completionPrefix) && words != null) {
                                completionCache.clear();
                                completionCache.addAll(words);
                                view.post(view::invalidate);
                            }
                        });
            }
            return List.copyOf(completionCache);
        });
        // l'aide aux paramètres du serveur prime : le cache par
        // curseur, la génération déclasse une réponse du texte
        // d'avant (un changement de document ou de curseur)
        view.setSignatureProvider(caret -> {
            if (!signatureEnabled || session == null) {
                return null;
            }
            if (caret != signatureCaret) {
                signatureCaret = caret;
                signatureAnswer = null;
                final long generation = signatureGeneration.incrementAndGet();
                TextBuffer buffer = view.getBuffer();
                int line = buffer.lineOfOffset(caret);
                session.signatureHelp(documentUri,
                                new LspPosition(line, caret - buffer.getLineStart(line)))
                        .whenComplete((help, failure) -> {
                            if (help != null && generation == signatureGeneration.get()) {
                                signatureAnswer = new EditorView.SignatureHelp(
                                        help.label(), help.activeParameter());
                                view.post(view::invalidate);
                            }
                        });
            }
            return signatureAnswer;
        });
        // les diagnostics de la ligne : la sévérité du protocole y
        // garde son rang (erreur, avertissement, information)
        view.setDiagnosticProvider(this::diagnosticsOfLine);
    }

    /**
     * Les diagnostics du serveur à une ligne (la carte de diagnostic
     * les lit au toucher du marqueur ou du souligné rouge).
     *
     * @param line l'indice de la ligne
     * @return les diagnostics du serveur sur la ligne, jamais null
     */
    private List<DiagnosticCard.Entry> diagnosticsOfLine(int line) {
        if (!diagnosticsEnabled || !isConnected()) {
            return List.of();
        }
        List<DiagnosticCard.Entry> found = new ArrayList<>();
        for (LspDiagnostic diagnostic : diagnostics) {
            if (diagnostic.range().start().line() == line) {
                found.add(new DiagnosticCard.Entry(
                        diagnostic.severity() == 1 ? DiagnosticCard.SEVERITY_ERROR
                                : diagnostic.severity() == 2 ? DiagnosticCard.SEVERITY_WARNING
                                : DiagnosticCard.SEVERITY_INFO,
                        diagnostic.message()));
            }
        }
        return found;
    }

    /**
     * Le document part au serveur — la liaison n'envoie que la plage
     * qui a bougé (l'ombre de la dernière synchronisation tient le
     * reste) ; chaque édition le reprogramme : les frappes rapides
     * n'envoient qu'un envoi, le dernier. Le document a changé côté
     * serveur : les complétions reçues décrivent l'ancien texte — le
     * cache se range, la trame suivante redemande ; la signature en
     * vol se périme pareillement (la génération avance).
     */
    private void pushDocumentToServer() {
        if (session != null && uri != null) {
            session.didChange(uri, view.getBuffer().toString());
            resetProviders();
            view.post(view::invalidate);
        }
    }

    /** Une édition arrive : l'envoi du document se reprogramme (l'hôte n'a rien à faire). */
    private void scheduleDocumentSync() {
        if (session == null) {
            return;
        }
        view.removeCallbacks(documentSync);
        view.postDelayed(documentSync, DOCUMENT_SYNC_MS);
    }

    /** Les caches et les avis en vol se rangent (le document a changé, la réponse d'avant ne vaut plus). */
    private void resetProviders() {
        completionPrefix = null;
        completionCache.clear();
        signatureGeneration.incrementAndGet();
        signatureCaret = -1;
        signatureAnswer = null;
    }
}
