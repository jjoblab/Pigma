package jo.pigma.editor.popups;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 * LES CHAÎNES DES POPUPS aux JVM pures : les défauts portent le
 * français courant (Rien ne change sans consigne de l'hôte), le
 * bâtisseur y remplace UNE valeur, les compteurs s'y composent (le
 * singulier vit aussi), l'objet figé ne se relit jamais à moitié.
 */
public class PopupStringsTest {

    /** Les défauts portent le français courant. */
    @Test
    public void theDefaultsCarryTheCurrentFrench() {
        PopupStrings strings = PopupStrings.defaults();
        assertEquals("le titre de la structure", "Structure",
                strings.structure());
        assertEquals("le titre des problèmes", "Problèmes",
                strings.problems());
        assertEquals("le titre des récents", "Récents", strings.recents());
        assertEquals("l'aller", "Aller à", strings.gotoTitle());
        assertEquals("l'entourage", "Entourer de", strings.surround());
        assertEquals("les extraits", "Extraits", strings.snippets());
        assertEquals("le presse-papiers", "Presse-papiers",
                strings.clipboard());
        assertEquals("le bouton d'ouverture", "Ouvrir", strings.open());
        assertEquals("le bouton d'application", "Appliquer",
                strings.apply());
        // les rangées du débordement
        assertEquals("l'aller à la ligne", "Aller à la ligne…",
                strings.gotoLineRow());
        assertEquals("la recherche", "Rechercher…", strings.searchRow());
        assertEquals("couper", "Couper", strings.cutRow());
        assertEquals("l'aller au symbole", "Aller au symbole…",
                strings.gotoSymbolRow());
        // les entrées de la façade
        assertEquals("l'aller à la définition", "Aller à la définition",
                strings.definitionEntry());
        assertEquals("le correctif", "Correctif : Couper les espaces",
                strings.fixTitle("Couper les espaces"));
        // les chaînes de l'assistant
        assertEquals("le titre de l'assistant", "Assistant",
                strings.assistant());
        assertEquals("le corps de l'attente", "Réflexion…",
                strings.aiThinking());
        assertEquals("le bouton d'annulation", "Annuler",
                strings.aiCancel());
        assertEquals("l'entrée d'explication", "Expliquer",
                strings.aiExplain());
        assertEquals("l'entrée d'explication de l'erreur",
                "Expliquer l'erreur", strings.aiExplainError());
        assertEquals("l'entrée de correction",
                "Corriger avec l'assistant", strings.aiFix());
        assertEquals("l'entrée de documentation", "Documenter",
                strings.aiDocument());
        assertEquals("l'entrée du test", "Écrire le test",
                strings.aiWriteTest());
        // une phrase par famille de panne
        assertEquals("la panne d'indisponibilité",
                "L'assistant n'est pas disponible.",
                strings.aiUnavailable());
        assertEquals("la panne d'annulation",
                "La demande fut annulée.", strings.aiCancelled());
        assertEquals("la panne de délai",
                "Le temps de réponse est écoulé.",
                strings.aiTimeout());
        assertEquals("la panne de refus",
                "La demande fut refusée.", strings.aiRefused());
        assertEquals("la panne interne",
                "Panne interne de l'assistant.",
                strings.aiInternal());
        assertEquals("le texte changé",
                "Le texte a changé depuis la demande — la réponse ne se pose pas.",
                strings.aiStale());
        assertEquals("la réponse muette",
                "La réponse est vide.", strings.aiEmpty());
    }

    /** Le bâtisseur y remplace UNE valeur — le reste demeure. */
    @Test
    public void theBuilderReplacesOneValue() {
        PopupStrings strings = PopupStrings.builder()
                .structure("Outline")
                .gotoLineRow("Go to line…")
                .build();
        assertEquals("la valeur posée vit", "Outline", strings.structure());
        assertEquals("la rangée posée vit", "Go to line…",
                strings.gotoLineRow());
        assertEquals("les autres demeurent (le français courant)",
                "Problèmes", strings.problems());
        assertEquals("l'invite de l'aller demeure", "Ouvrir",
                strings.open());
        // l'objet figé : les défauts ne bougent pas
        assertNotSame("un objet neuf à chaque défaut",
                PopupStrings.defaults(), PopupStrings.defaults());
    }

    /** Le bâtisseur localise l'assistant comme le reste (l'hôte y
     * pose sa langue — le moteur de l'hôte n'y traduit rien). */
    @Test
    public void theBuilderReplacesTheAssistantStrings() {
        PopupStrings strings = PopupStrings.builder()
                .assistant("Assistant AI")
                .aiThinking("Thinking…")
                .aiCancel("Cancel")
                .aiExplain("Explain")
                .aiExplainError("Explain the error")
                .aiFix("Fix with the assistant")
                .aiDocument("Document")
                .aiWriteTest("Write the test")
                .aiUnavailable("The assistant is unavailable.")
                .aiCancelled("The request was cancelled.")
                .aiTimeout("The response timed out.")
                .aiRefused("The request was refused.")
                .aiInternal("The assistant failed internally.")
                .aiStale("The text changed since the request.")
                .aiEmpty("The response is empty.")
                .build();
        assertEquals("le titre posé", "Assistant AI", strings.assistant());
        assertEquals("l'attente posée", "Thinking…", strings.aiThinking());
        assertEquals("l'annulation posée", "Cancel", strings.aiCancel());
        assertEquals("l'explication posée", "Explain", strings.aiExplain());
        assertEquals("l'erreur posée", "Explain the error",
                strings.aiExplainError());
        assertEquals("la correction posée", "Fix with the assistant",
                strings.aiFix());
        assertEquals("la documentation posée", "Document",
                strings.aiDocument());
        assertEquals("le test posé", "Write the test",
                strings.aiWriteTest());
        assertEquals("la panne posée", "The assistant is unavailable.",
                strings.aiUnavailable());
        assertEquals("l'annulation d'attente posée",
                "The request was cancelled.", strings.aiCancelled());
        assertEquals("le délai posé", "The response timed out.",
                strings.aiTimeout());
        assertEquals("le refus posé", "The request was refused.",
                strings.aiRefused());
        assertEquals("la panne interne posée",
                "The assistant failed internally.", strings.aiInternal());
        assertEquals("le texte changé posé",
                "The text changed since the request.", strings.aiStale());
        assertEquals("la réponse vide posée", "The response is empty.",
                strings.aiEmpty());
        // le reste demeure (le français courant)
        assertEquals("les correctifs du serveur demeurent",
                "Correctif : Couper", strings.fixTitle("Couper"));
    }

    /** Les compteurs se composent — le singulier vit aussi. */
    @Test
    public void theCountsCompose() {
        PopupStrings strings = PopupStrings.defaults();
        assertEquals("le singulier d'une cible", "1 cible",
                strings.targetsCount(1));
        assertEquals("le pluriel des cibles", "3 cibles",
                strings.targetsCount(3));
        assertEquals("le singulier d'un résultat", "1 résultat",
                strings.resultsCount(1));
        assertEquals("le pluriel des résultats", "2 résultats",
                strings.resultsCount(2));
        assertEquals("le singulier d'un candidat", "1 candidat",
                strings.candidatesCount(1));
        assertEquals("le pluriel des candidats", "2 candidats",
                strings.candidatesCount(2));
        assertEquals("les lignes d'aperçu", "12 lignes",
                strings.bodyLinesCount(12));
        assertEquals("les patrons", "4 patrons",
                strings.patternsCount(4));
        assertEquals("les extraits", "5 extraits",
                strings.snippetsCount(5));
        assertEquals("les récents composés", "2 fichiers · 3 emplacements",
                strings.recentsCount(2, 3));
        assertEquals("le singulier des récents", "1 fichier · 1 emplacement",
                strings.recentsCount(1, 1));
        assertEquals("les symboles filtrés", "3 symboles sur 12",
                strings.filteredCount(3, 12));
        assertEquals("les copies", "4 copies", strings.copiesCount(4));
        assertEquals("l'application porte son compte", "Appliquer (2)",
                strings.applyWith(2));
    }

    /** La place d'une rangée se compose (le mot de l'hôte y passe). */
    @Test
    public void thePlacesCompose() {
        PopupStrings strings = PopupStrings.defaults();
        assertEquals("la ligne seule", "ligne 5", strings.lineAt(4));
        assertEquals("le fichier et sa ligne", "Lot.java · ligne 8",
                strings.fileLineAt("Lot.java", 7));
        assertEquals("sans fichier, la ligne seule", "ligne 8",
                strings.fileLineAt("", 7));
        assertEquals("la question de l'import",
                "Importer « graine2 » ?",
                strings.importQuestion("graine2"));
        assertEquals("les usages d'un symbole", "Usages de « total »…",
                strings.usagesRow("total"));
        assertEquals("les usages du serveur", "Usages · total",
                strings.usagesOf("total"));
        assertEquals("les actions rapides à la ligne",
                "Actions rapides · ligne 3",
                strings.quickActionsAt(2));
        assertEquals("les diagnostics à la ligne", "Diagnostics · ligne 7",
                strings.diagnosticsAt(6));
        assertEquals("l'invite de la barre", "Aller à la ligne : ",
                strings.gotoBarInvite());
    }

    /** La pose nulle rend les défauts (l'hôte y retire sa consigne). */
    @Test
    public void theNullPoseRestoresDefaults() {
        PopupStrings strings = PopupStrings.builder().structure("X").build();
        PopupStringsTest holder = new PopupStringsTest();
        // la carte reçoit null : les défauts reviennent — la preuve en
        // passe par la carte (setStrings(null) rend les défauts)
        PreviewCard card = new PreviewCard(1.0f);
        card.setStrings(strings);
        assertSame("la consigne de l'hôte vit", strings, card.strings());
        card.setStrings(null);
        assertNotSame("les défauts reviennent", strings, card.strings());
        assertEquals("les défauts portent le français", "Ouvrir",
                card.strings().open());
    }
}
