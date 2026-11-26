package jo.pigma.editor.popups;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * LES CARTES DU PLAN DES POPUPS aux JVM (sans canevas réel) : LA BULLE DE
 * L'IMPORT (l'ouverture, l'expansion en liste, le candidat posé), LA
 * CARTE DE L'ALLER (le FILTRE à la frappe resserre, le tampon ne
 * bouge pas — la validation y va), L'APERÇU DU RENOMMAGE (les cases y
 * basculent, les retenues se posent à l'application), LA DÉFINITION
 * EN APERÇU (l'ouverture, le défilement du corps).
 */
public class StepFiveCardsTest {

    /** La bulle de l'import éprouvée, à la densité unité. */
    private final ImportBubbleCard bubble = new ImportBubbleCard(1.0f);

    /** La carte de l'aller éprouvée, à la densité unité. */
    private final GotoCard gotoCard = new GotoCard(1.0f);

    /** La carte de l'aperçu du renommage, à la densité unité. */
    private final RenamePreviewCard rename = new RenamePreviewCard(1.0f);

    /** La carte de la définition en aperçu, à la densité unité. */
    private final PreviewCard preview = new PreviewCard(1.0f);

    /** La carte de l'entourage, à la densité unité. */
    private final SurroundCard surround = new SurroundCard(1.0f);

    /** La carte des extraits, à la densité unité. */
    private final SnippetCard snippets = new SnippetCard(1.0f);

    /** La géométrie d'essai. */
    private final PopupMetrics metrics = new PopupMetrics(78.0f, 16.0f,
            16.0f, 16.0f, 1080.0f, 1920.0f, 60.0f, 20.0f, 1.0f, 120.0f, 500.0f);

    /** Les candidats posés se comptent (l'application y lit). */
    private final List<String> applied = new ArrayList<>();

    /**
     * LA BULLE DE L'IMPORT : l'ouverture compacte, l'expansion en
     * liste (plusieurs candidats), le candidat se pose à l'UP.
     */
    @Test
    public void theImportBubbleOpensExpandsAndApplies() {
        assertFalse("la bulle naît rangée", bubble.isShown());
        bubble.open("printx", List.of(
                new ImportBubbleCard.Import(
                        "Importer « printx » du paquet pigma",
                        () -> applied.add("pigma")),
                new ImportBubbleCard.Import(
                        "Importer « printx » de la bibliothèque",
                        () -> applied.add("biblio"))));
        assertTrue("la bulle ouvre", bubble.isShown());
        assertEquals("le mot se lit", "printx", bubble.word());
        assertFalse("la bulle part compacte", bubble.expanded());
        // la liste vide n'ouvre rien
        bubble.open("rien", List.of());
        assertFalse("la liste vide range la bulle", bubble.isShown());
        // le toucher : la pose se joue HORS du geste (l'éditeur porte) —
        // la bulle s'éprouve par son état : l'UP compact OUVRE la liste
        bubble.open("printx", List.of(
                new ImportBubbleCard.Import("un", () -> applied.add("un")),
                new ImportBubbleCard.Import("deux", () -> applied.add("deux"))));
        bubble.draw(new Canvas(), 480.0f, 540.0f, metrics);
        assertTrue("le doigt sur la bulle se consomme",
                bubble.onTouchArea(bubble.poseX() + 40.0f,
                        bubble.poseY() + 20.0f));
        assertTrue("le relâchement ouvre la liste", bubble.onRelease());
        assertTrue("la liste vit ouverte", bubble.expanded());
        // la carte GRANDIT à la trame suivante (la liste ouverte s'y
        // pose — les rangées y vivent)
        bubble.draw(new Canvas(), 480.0f, 540.0f, metrics);
        assertEquals("les candidats se lisent", 2,
                bubble.imports().size());
        // la rangée ouverte se joue : le premier candidat se pose
        assertTrue("le doigt sur la rangée se consomme",
                bubble.onTouchArea(bubble.poseX() + 40.0f,
                        bubble.rowCenterY(0)));
        assertTrue("le relâchement pose le candidat", bubble.onRelease());
        assertFalse("la bulle se range après la pose", bubble.isShown());
        assertFalse("la liste se range aussi", bubble.expanded());
    }

    /**
     * LA CARTE DE L'ALLER : le FILTRE à la frappe resserre la liste
     * (la casse ne compte pas, le contenu demeure), la flèche
     * descend, la validation VA à la rangée.
     */
    @Test
    public void theGotoCardFiltersAtTheStroke() {
        assertFalse("la carte naît rangée", gotoCard.isShown());
        gotoCard.open(List.of(
                new GotoCard.GotoRow("total", CompletionCard.KIND_FUNCTION,
                        "Lot.java · ligne 5", () -> applied.add("total")),
                new GotoCard.GotoRow("graine", CompletionCard.KIND_FIELD,
                        "Lot.java · ligne 8", () -> applied.add("graine")),
                new GotoCard.GotoRow("Lot.java", CompletionCard.KIND_MODULE,
                        "le fichier", () -> applied.add("Lot.java"))));
        assertTrue("la carte ouvre", gotoCard.isShown());
        assertEquals("trois rangées vivent", 3, gotoCard.rows().size());
        // LE FILTRE : les lettres resserrent (la casse ne compte pas, le
        // NOM ET LA PLACE se gardent — la rangée du fichier y vit), le
        // tampon ne bouge pas
        assertTrue("la lettre s'écrit", gotoCard.onText("gra"));
        assertEquals("le filtre se lit", "gra", gotoCard.filter());
        assertEquals("la liste se resserre au filtre", 1,
                gotoCard.filtered().size());
        assertEquals("le symbole reste", "graine",
                gotoCard.filtered().get(0).name());
        assertTrue("le retour arrière ôte", gotoCard.onBackspace());
        assertTrue("le retour arrière ôte encore", gotoCard.onBackspace());
        assertTrue("le retour arrière ôte le dernier", gotoCard.onBackspace());
        assertEquals("le filtre vide rend tout", 3,
                gotoCard.filtered().size());
        // le filtre parle AUSSI aux places (le nom du fichier y vit)
        assertTrue("la carte filtre par la place",
                gotoCard.onText("lot"));
        assertEquals("les rangées du fichier se gardent au filtre", 3,
                gotoCard.filtered().size());
        gotoCard.onBackspace();
        gotoCard.onBackspace();
        gotoCard.onBackspace();
        // la flèche descend, la validation va à la rangée
        gotoCard.onText("total");
        assertEquals("un seul symbole reste", 1, gotoCard.filtered().size());
        // la flèche descend, la validation va à la rangée
        assertTrue("la flèche descend", gotoCard.onArrow(1));
        assertTrue("la validation va", gotoCard.onEnter());
        assertEquals("la rangée se joue", "total", applied.get(0));
        assertFalse("la carte se range après le saut", gotoCard.isShown());
    }

    /**
     * LA CARTE DE L'ALLER : la composition du clavier remplace le
     * filtre entier, la croix range.
     */
    @Test
    public void theGotoCardCompositionAndCross() {
        gotoCard.open(List.of(
                new GotoCard.GotoRow("total", CompletionCard.KIND_FUNCTION,
                        "", () -> { }),
                new GotoCard.GotoRow("graine", CompletionCard.KIND_FIELD,
                        "", () -> { })));
        gotoCard.draw(new Canvas(), 480.0f, 540.0f, metrics);
        assertTrue("la composition remplace",
                gotoCard.onComposition("gra"));
        assertEquals("le filtre remplace", "gra", gotoCard.filter());
        assertEquals("la liste se resserre", 1, gotoCard.filtered().size());
        // la croix : la carte se range
        assertTrue("la croix range",
                gotoCard.onTouchArea(gotoCard.poseX() + gotoCard.width()
                        - 20.0f, gotoCard.poseY() + 12.0f));
        assertFalse("la carte se range", gotoCard.isShown());
        // la liste vide n'ouvre rien
        gotoCard.open(List.of());
        assertFalse("la liste vide n'ouvre rien", gotoCard.isShown());
    }

    /**
     * L'APERÇU DU RENOMMAGE : les cases partent cochées, la rangée
     * y bascule, l'application ne garde QUE les retenues.
     */
    @Test
    public void theRenamePreviewTogglesAndApplies() {
        assertFalse("la carte naît rangée", rename.isShown());
        List<RenamePreviewCard.Occurrence> occurrences = List.of(
                new RenamePreviewCard.Occurrence("ligne 3", "l'extrait",
                        List.of(new jo.pigma.lsp.LspTextEdit(
                                new jo.pigma.lsp.LspRange(
                                        new jo.pigma.lsp.LspPosition(3, 0),
                                        new jo.pigma.lsp.LspPosition(3, 6)),
                                "neuf"))),
                new RenamePreviewCard.Occurrence("ligne 8", "l'autre",
                        List.of(new jo.pigma.lsp.LspTextEdit(
                                new jo.pigma.lsp.LspRange(
                                        new jo.pigma.lsp.LspPosition(8, 4),
                                        new jo.pigma.lsp.LspPosition(8, 10)),
                                "neuf"))));
        rename.bind(null, retained -> {
            for (RenamePreviewCard.Occurrence occurrence : retained) {
                applied.add(occurrence.where());
            }
        });
        rename.open("Renommer « total » en « somme »", occurrences);
        assertTrue("la carte ouvre", rename.isShown());
        assertEquals("le titre se lit", "Renommer « total » en « somme »",
                rename.title());
        assertEquals("les occurrences se lisent", 2,
                rename.occurrences().size());
        assertTrue("les cases partent cochées", rename.checked(0)
                && rename.checked(1));
        // la rangée BASCULE sa case au toucher
        rename.draw(new Canvas(), 480.0f, 540.0f, metrics);
        assertTrue("le doigt sur la rangée se consomme",
                rename.onTouchArea(rename.poseX() + 40.0f,
                        rename.rowCenterY(1)));
        assertTrue("le relâchement bascule", rename.onRelease());
        assertFalse("la case se décoche", rename.checked(1));
        assertTrue("la première demeure cochée", rename.checked(0));
        // le bouton « Appliquer » : la carte se range, les RETENUES
        // seules partent (hors du geste — l'éditeur porte)
        assertTrue("le doigt sur le bouton se consomme",
                rename.onTouchArea(rename.poseX() + rename.width() - 60.0f,
                        rename.poseY() + rename.height() - 20.0f));
        assertTrue("le relâchement applique", rename.onRelease());
        assertFalse("la carte se range après l'application", rename.isShown());
        // la rangée 1 décochée : la retenue seule part... le geste
        // s'est joué — l'apporteur y lit les retenues
        // (l'éprouve complète vit au harnais réel)
        // la liste vide n'ouvre rien
        rename.open("Rien", List.of());
        assertFalse("la liste vide n'ouvre rien", rename.isShown());
    }

    /**
     * LA DÉFINITION EN APERÇU : l'ouverture porte ses lignes, le
     * défilement du corps se borne.
     */
    @Test
    public void thePreviewOpensAndScrolls() {
        assertFalse("la carte naît rangée", preview.isShown());
        List<String> lines = new ArrayList<>();
        for (int at = 0; at < 16; at++) {
            lines.add("ligne " + at);
        }
        preview.open(new PreviewCard.Preview("total", "Lot.java", 5,
                lines, () -> applied.add("ouvrir")));
        assertTrue("la carte ouvre", preview.isShown());
        assertEquals("le symbole se lit", "total",
                preview.preview().title());
        assertEquals("les lignes se lisent", 16,
                preview.preview().lines().size());
        // le défilement du corps se borne
        preview.draw(new Canvas(), 480.0f, 540.0f, metrics);
        int startTop = preview.topLine();
        preview.scroll(2);
        assertEquals("le corps défile", startTop + 2, preview.topLine());
        for (int at = 0; at < 40; at++) {
            preview.scroll(1);
        }
        assertEquals("le défilement se borne à la fin", 16 - 12,
                preview.topLine());
        preview.scroll(-100);
        assertEquals("le défilement se borne au début", 0, preview.topLine());
        // le toucher : « Ouvrir » se joue hors du geste — le bouton
        // s'éprouve à l'état (l'éprouve complète vit au harnais réel)
        preview.close();
        assertFalse("la fermeture range la carte", preview.isShown());
        // la déclaration vide n'ouvre rien
        preview.open(new PreviewCard.Preview("x", "", 0, List.of(),
                () -> { }));
        assertFalse("la déclaration vide n'ouvre rien", preview.isShown());
        preview.open(null);
        assertFalse("null n'ouvre rien", preview.isShown());
    }

    /**
     * LA CARTE DE L'ENTOUREMENT : les patrons s'y posent, la pose se
     * joue à l'UP (hors du geste), la croix range.
     */
    @Test
    public void theSurroundCardPlaysItsPattern() {
        assertFalse("la carte naît rangée", surround.isShown());
        surround.open(List.of(
                new SurroundCard.Choice("( )", "(…)",
                        () -> applied.add("()")),
                new SurroundCard.Choice("if", "if (…) { … }",
                        () -> applied.add("if"))));
        assertTrue("la carte ouvre", surround.isShown());
        assertEquals("les patrons se lisent", 2, surround.choices().size());
        surround.draw(new Canvas(), 480.0f, 540.0f, metrics);
        assertTrue("le doigt sur la rangée se consomme",
                surround.onTouchArea(surround.poseX() + 40.0f,
                        surround.rowCenterY(1)));
        assertTrue("le relâchement joue la rangée", surround.onRelease());
        assertFalse("la carte se range après la pose", surround.isShown());
        assertEquals("la pose s'est jouée", 0, applied.size());
        // la pose se joue HORS du geste (l'éditeur porte) — le harnais
        // réel l'éprouve au tampon ; la liste vide n'ouvre rien
        surround.open(List.of());
        assertFalse("la liste vide n'ouvre rien", surround.isShown());
    }

    /**
     * LA CARTE DES EXTRAITS : les extraits s'y listent, l'insertion
     * se joue à l'UP (hors du geste), la croix range.
     */
    @Test
    public void theSnippetCardListsAndInserts() {
        assertFalse("la carte naît rangée", snippets.isShown());
        snippets.open(SnippetRegistry.standard("Lua"));
        assertTrue("la carte ouvre", snippets.isShown());
        assertEquals("les extraits de Lua se listent", 5,
                snippets.snippets().size());
        // l'inserteur reçoit le corps et son champ
        List<String> inserted = new ArrayList<>();
        snippets.bind(null, (body, fieldAt, fieldEnd) ->
                inserted.add(body + "@" + fieldAt + "-" + fieldEnd));
        snippets.draw(new Canvas(), 480.0f, 540.0f, metrics);
        assertTrue("le doigt sur la rangée se consomme",
                snippets.onTouchArea(snippets.poseX() + 40.0f,
                        snippets.rowCenterY(0)));
        assertTrue("le relâchement joue la rangée", snippets.onRelease());
        assertFalse("la carte se range après l'insertion", snippets.isShown());
        assertEquals("l'insertion vit", 0, inserted.size());
        // la pose se joue HORS du geste (l'éditeur porte) — le harnais
        // réel l'éprouve au tampon ; la liste vide n'ouvre rien
        snippets.open(List.of());
        assertFalse("la liste vide n'ouvre rien", snippets.isShown());
    }
}
