package jo.pigma.sample;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jo.pigma.ai.AiBackend;
import jo.pigma.ai.AiCall;
import jo.pigma.ai.AiCapabilities;
import jo.pigma.ai.AiError;
import jo.pigma.ai.AiListener;
import jo.pigma.ai.AiRequest;
import jo.pigma.ai.AiResult;
import jo.pigma.ai.AiTask;

/**
 * LE MOTEUR DE DÉMONSTRATION (B6) : une « IA de démonstration »,
 * JAMAIS un fournisseur — déterministe (les réponses se devinent de
 * la demande même, rien n'aléatoire), le flux y vit PAR MORCEAUX, le
 * délai se règle, les pannes s'y jouent (indisponible, délai, refus,
 * réponse muette, réponse démesurée) : il montre les fonctions de
 * l'assistant et éprouve ses états d'erreur. AUCUN réseau — tout se
 * joue dans le processus, à la file du thread principal (le plan
 * l'exige : aucune permission, aucun SDK, aucune clé).
 *
 * <p>Les réponses sont des DONNÉES de démonstration : elles parlent
 * de la demande (le nom du document, la sélection, le diagnostic) et
 * se posent par les vraies voies de l'assistant (l'aperçu, la
 * lecture, le fantôme) — jamais elles n'écrivent au tampon
 * d'elles-mêmes.</p>
 */
public final class FakeAiBackend implements AiBackend {

    /** Les états jouables du moteur (la démonstration et l'éprouve). */
    public enum Mode {

        /** Les réponses arrivent, morceau par morceau. */
        NORMALE,

        /** Le moteur ne déclare RIEN (aucune entrée ne se propose). */
        INDISPONIBLE,

        /** Le temps de réponse se consomme (la phrase du délai). */
        DELAI,

        /** La demande se refuse (la phrase du refus). */
        REFUS,

        /** La réponse arrive vide (la phrase du silence). */
        MUETTE,

        /** La réponse arrive démesurée (l'aperçu la borne). */
        DEMESUREE
    }

    /** Le délai de la réponse (ms — la démonstration respire). */
    private long delayMs = 500;

    /** L'état joué (la démonstration commence à la normale). */
    private Mode mode = Mode.NORMALE;

    /** La file du thread principal (aucun fil neuf, aucun réseau). */
    private final Handler queue = new Handler(Looper.getMainLooper());

    /**
     * Le délai se règle (la démonstration respire plus ou moins — le
     * harnais le raccourcit).
     *
     * @param millis le délai, en millisecondes
     */
    public void setDelayMs(long millis) {
        delayMs = Math.max(0, millis);
    }

    /**
     * L'état se joue (la démonstration et l'éprouve des états
     * d'erreur).
     *
     * @param fresh l'état joué
     */
    public void setMode(Mode fresh) {
        mode = fresh == null ? Mode.NORMALE : fresh;
    }

    /** L'état joué (le harnais le lit). */
    public Mode mode() {
        return mode;
    }

    /** Les tâches déclarées : TOUTES (sauf l'indisponible qui n'en
     * déclare aucune — aucune entrée ne se propose). */
    @Override
    public AiCapabilities capabilities() {
        if (mode == Mode.INDISPONIBLE) {
            return AiCapabilities.NONE;
        }
        return new AiCapabilities(Set.of(
                AiTask.EXPLAIN, AiTask.EXPLAIN_ERROR, AiTask.FIX,
                AiTask.EDIT_SELECTION, AiTask.INLINE_COMPLETION,
                AiTask.DOCUMENT, AiTask.WRITE_TEST), true);
    }

    /**
     * La demande part : la réponse se joue À LA FILE du thread
     * principal — les morceaux du flux d'abord (trois d'ordinaire),
     * l'issue ensuite ; la poignée coupe tout (les morceaux non
     * joués se retirent de la file).
     */
    @Override
    public AiCall start(AiRequest request, AiListener listener) {
        List<Runnable> queued = new ArrayList<>();
        if (mode == Mode.DELAI) {
            // le temps se consume : la phrase du délai arrive tard
            queued.add(() -> listener.onError(
                    new AiError(AiError.Category.TIMEOUT, "délai de démo")));
        } else if (mode == Mode.REFUS) {
            queued.add(() -> listener.onError(
                    new AiError(AiError.Category.REFUSED, "refus de démo")));
        } else {
            AiResult result = answerOf(request);
            String whole = textOf(result);
            // LE FLUX PAR MORCEAUX : trois partages d'ordinaire, la
            // démesurée en compte plus (la lecture n'en montre pas
            // plus pour autant)
            int chunks = mode == Mode.DEMESUREE ? 6 : 3;
            for (int at = 0; at < chunks; at++) {
                final String part = slice(whole, at, chunks);
                queued.add(() -> listener.onDelta(part));
            }
            queued.add(() -> listener.onComplete(result));
        }
        long step = Math.max(1, delayMs / (queued.size() + 1));
        long at = step;
        for (Runnable tick : queued) {
            queue.postDelayed(tick, at);
            at += step;
        }
        return () -> {
            for (Runnable tick : queued) {
                queue.removeCallbacks(tick);
            }
        };
    }

    /**
     * La réponse déterministe d'une demande : elle parle de la
     * demande (le nom, la sélection, le diagnostic) — rien
     * d'aléatoire, la démonstration se devine du code même.
     *
     * @param request la demande partie
     * @return la réponse jouée
     */
    private AiResult answerOf(AiRequest request) {
        if (mode == Mode.MUETTE) {
            return request.task() == AiTask.INLINE_COMPLETION
                    || request.task() == AiTask.DOCUMENT
                    || request.task() == AiTask.EDIT_SELECTION
                    || request.task() == AiTask.FIX
                    ? new AiResult.Insertion(caretOf(request), "")
                    : new AiResult.Text("");
        }
        if (mode == Mode.DEMESUREE) {
            return new AiResult.Text("(démo) ".repeat(30_000));
        }
        String name = request.documentName().isEmpty()
                ? "ce document" : request.documentName();
        switch (request.task()) {
            case EXPLAIN:
                return new AiResult.Text("## Explication (démo)\n\n"
                        + "Voici `" + name + "` : la ligne du curseur dit\n\n"
                        + "> " + lastLine(request.prefix()) + "\n\n"
                        + "*Le moteur de démonstration n'apprend rien — "
                        + "la vraie intelligence vit chez l'hôte.*\n");
            case EXPLAIN_ERROR:
                return new AiResult.Text("## L'erreur (démo)\n\n"
                        + (request.diagnostics().isEmpty()
                        ? "aucun diagnostic dans la demande"
                        : "Elle dit : « "
                                + request.diagnostics().get(0).message()
                                + " » — le signe mésapparié se lit au "
                                + "souligné, le correctif local le retire.\n"));
            case FIX:
                if (request.diagnostics().isEmpty()) {
                    return new AiResult.Text("");
                }
                return new AiResult.Replacement(
                        request.diagnostics().get(0).start(),
                        request.diagnostics().get(0).end(), "");
            case EDIT_SELECTION:
                return new AiResult.Replacement(
                        selectionStartOf(request),
                        selectionEndOf(request),
                        request.selection() + " (revu par la démo)");
            case INLINE_COMPLETION:
                return new AiResult.Insertion(caretOf(request),
                        " // complété par la démo");
            case DOCUMENT:
                return new AiResult.Insertion(caretOf(request),
                        "/** Documenté par la démo. */\n");
            case WRITE_TEST:
            default:
                return new AiResult.Text("public class DemoTest {\n"
                        + "    // le test de `"
                        + name + "` (démo) — l'hôte seul crée le fichier\n"
                        + "}\n");
        }
    }

    /** La position du curseur de la demande (le préfixe la porte). */
    private static int caretOf(AiRequest request) {
        return request.prefix().length();
    }

    /** Le début de la sélection de la demande (le préfixe la porte). */
    private static int selectionStartOf(AiRequest request) {
        return request.prefix().length();
    }

    /** La fin de la sélection de la demande (elle suit le début). */
    private static int selectionEndOf(AiRequest request) {
        return request.prefix().length() + request.selection().length();
    }

    /** La dernière ligne du préfixe (l'explication la cite). */
    private static String lastLine(String prefix) {
        int at = prefix.lastIndexOf('\n');
        String line = at < 0 ? prefix : prefix.substring(at + 1);
        return line.trim().isEmpty() ? "(ligne vide)" : line.trim();
    }

    /** Le texte d'une réponse (le flux s'y partage). */
    private static String textOf(AiResult result) {
        if (result instanceof AiResult.Text text) {
            return text.markdown();
        }
        if (result instanceof AiResult.Replacement fix) {
            return fix.text();
        }
        if (result instanceof AiResult.Insertion insert) {
            return insert.text();
        }
        return "";
    }

    /** Une part du texte (le morceau du flux). */
    private static String slice(String whole, int index, int count) {
        if (whole.isEmpty()) {
            return "";
        }
        int from = Math.min(whole.length(), whole.length() * index / count);
        int to = Math.min(whole.length(), whole.length() * (index + 1) / count);
        return whole.substring(from, to);
    }
}
