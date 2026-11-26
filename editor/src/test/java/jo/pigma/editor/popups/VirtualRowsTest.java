package jo.pigma.editor.popups;

import java.util.List;

import android.graphics.Canvas;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * LES RANGÉES VIRTUELLES aux JVM (la carte vivante s'y lit au service
 * qui parle — chaque rangée porte son LIBELLÉ, son ACTION y joue la
 * MÊME chose que le doigt) : l'aller, la cible, les récents, le
 * renommage (la bascule de case), la bulle d'import ouverte en
 * liste.
 */
public class VirtualRowsTest {

    /** La carte de l'aller éprouvée. */
    private final GotoCard gotoCard = new GotoCard(1.0f);

    /** La carte de cibles éprouvée. */
    private final TargetCard targets = new TargetCard(1.0f);

    /** La carte des récents éprouvée (sa mémoire vit). */
    private final RecentCard recents = new RecentCard(1.0f);

    /** La carte de l'aperçu du renommage éprouvée. */
    private final RenamePreviewCard rename = new RenamePreviewCard(1.0f);

    /** La bulle de l'import éprouvée. */
    private final ImportBubbleCard bubble = new ImportBubbleCard(1.0f);

    /** La géométrie d'essai. */
    private final PopupMetrics metrics = new PopupMetrics(78.0f, 16.0f,
            16.0f, 16.0f, 1080.0f, 1920.0f, 60.0f, 20.0f, 1.0f, 120.0f, 500.0f);

    /** L'aller : les rangées s'y lisent, l'action y navigue. */
    @Test
    public void theGotoCardReadsAndPlays() {
        assertEquals("aucune rangée hors vie", 0, gotoCard.virtualRowCount());
        gotoCard.open(List.of(
                new GotoCard.GotoRow("total", CompletionCard.KIND_FUNCTION,
                        "Lot.java · ligne 8", () -> played.add("total")),
                new GotoCard.GotoRow("graine", CompletionCard.KIND_FIELD,
                        "Lot.java · ligne 14", () -> played.add("graine"))));
        assertEquals("les rangées VISIBLES se comptent", 2,
                gotoCard.virtualRowCount());
        assertEquals("le libellé se lit", "total, Lot.java · ligne 8",
                gotoCard.virtualRowLabel(0));
        assertEquals("le second libellé se lit", "graine, Lot.java · ligne 14",
                gotoCard.virtualRowLabel(1));
        assertEquals("hors rangée, le libellé se tait", "",
                gotoCard.virtualRowLabel(2));
        gotoCard.draw(new Canvas(), 480.0f, 540.0f, metrics);
        assertTrue("l'activation joue la rangée",
                gotoCard.virtualRowActivate(0));
        assertFalse("la carte se range après le jeu", gotoCard.isShown());
        assertFalse("hors rangée, l'activation ne joue pas",
                gotoCard.virtualRowActivate(0));
    }

    /** La cible : le libellé porte le nom et la place. */
    @Test
    public void theTargetCardReads() {
        targets.open("Définitions de « total »", List.of(
                new TargetCard.Target("total", "Autre.java", 5, "l'extrait",
                        () -> { })));
        assertEquals("une rangée vit", 1, targets.virtualRowCount());
        assertTrue("le libellé porte le nom et la place",
                targets.virtualRowLabel(0).contains("total")
                        && targets.virtualRowLabel(0).contains("Autre.java")
                        && targets.virtualRowLabel(0).contains("ligne 6"));
    }

    /** Les récents : les étiquettes se lisent, les rangées se jouent. */
    @Test
    public void theRecentCardReadsAndPlays() {
        assertEquals("aucune rangée hors vie", 0, recents.virtualRowCount());
        RecentFiles memory = new RecentFiles();
        memory.recordFile("Java (démo)", 1_000L, () -> played.add("fichier"));
        memory.recordLocation("Java (démo)", 3, "l'extrait", 2_000L,
                () -> played.add("emplacement"));
        recents.open(memory, 3_000L);
        // les rangées : l'étiquette Fichiers, le fichier, l'étiquette
        // Emplacements, l'emplacement
        assertEquals("quatre rangées vivent", 4, recents.virtualRowCount());
        assertEquals("l'étiquette des fichiers se lit", "Fichiers",
                recents.virtualRowLabel(0));
        assertTrue("le fichier se lit",
                recents.virtualRowLabel(1).contains("Java (démo)"));
        assertFalse("l'étiquette n'agit pas", recents.virtualRowActivate(0));
        assertTrue("la rangée du fichier se joue",
                recents.virtualRowActivate(1));
        assertFalse("la carte se range après le jeu", recents.isShown());
    }

    /** Le renommage : la rangée BASCULE sa case (l'action du doigt). */
    @Test
    public void theRenamePreviewToggles() {
        rename.bind(null, retained -> { });
        rename.open("Renommer « MAX » en « PLAFOND »", List.of(
                new RenamePreviewCard.Occurrence("ligne 6", "l'extrait",
                        List.of(new jo.pigma.lsp.LspTextEdit(
                                new jo.pigma.lsp.LspRange(
                                        new jo.pigma.lsp.LspPosition(6, 0),
                                        new jo.pigma.lsp.LspPosition(6, 3)),
                                "PLAFOND"))),
                new RenamePreviewCard.Occurrence("ligne 13", "l'autre",
                        List.of(new jo.pigma.lsp.LspTextEdit(
                                new jo.pigma.lsp.LspRange(
                                        new jo.pigma.lsp.LspPosition(13, 0),
                                        new jo.pigma.lsp.LspPosition(13, 3)),
                                "PLAFOND")))));
        assertEquals("deux rangées vivent", 2, rename.virtualRowCount());
        assertTrue("le libellé porte la place et l'extrait",
                rename.virtualRowLabel(1).contains("ligne 13")
                        && rename.virtualRowLabel(1).contains("l'autre"));
        assertTrue("la rangée BASCULE sa case", rename.virtualRowActivate(1));
        assertFalse("la case s'est décochée", rename.checked(1));
        assertTrue("la carte DEMEURE (la bascule ne range pas)",
                rename.isShown());
        assertTrue("la rangée re-bascule", rename.virtualRowActivate(1));
        assertTrue("la case s'est recochée", rename.checked(1));
    }

    /** La bulle d'import : les rangées vivent à la LISTE OUVERTE. */
    @Test
    public void theImportBubbleReadsAtItsOpenList() {
        assertEquals("aucune rangée hors vie", 0, bubble.virtualRowCount());
        bubble.open("printx", List.of(
                new ImportBubbleCard.Import(
                        "Importer « printx » du paquet pigma",
                        () -> played.add("pigma")),
                new ImportBubbleCard.Import(
                        "Importer « printx » de la bibliothèque",
                        () -> played.add("biblio"))));
        assertEquals("la bulle compacte n'expose rien (une rangée : la bulle même)", 0,
                bubble.virtualRowCount());
        bubble.draw(new Canvas(), 480.0f, 540.0f, metrics);
        // l'ouverture en liste (le tap de la bulle compacte)
        bubble.onTouchArea(bubble.poseX() + 40.0f, bubble.poseY() + 20.0f);
        bubble.onRelease();
        assertTrue("la liste vit ouverte", bubble.expanded());
        assertEquals("les candidats s'exposent", 2, bubble.virtualRowCount());
        assertEquals("le candidat se lit",
                "Importer « printx » du paquet pigma",
                bubble.virtualRowLabel(0));
        assertTrue("l'activation pose le candidat",
                bubble.virtualRowActivate(0));
        assertFalse("la bulle se range après la pose", bubble.isShown());
    }

    /** Les rangées jouées (la preuve y collecte). */
    private final java.util.List<String> played = new java.util.ArrayList<>();
}
