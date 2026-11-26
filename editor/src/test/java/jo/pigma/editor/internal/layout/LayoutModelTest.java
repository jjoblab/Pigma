package jo.pigma.editor.internal.layout;

import static org.junit.Assert.assertEquals;

import jo.pigma.core.TextBuffer;
import jo.pigma.editor.folding.FoldModel;
import org.junit.Test;


/**
 * La géométrie de la vue EN JVM : la correspondance offset ↔ pixel,
 * les plis, la coupe du soft wrap et les bornes de défilement —
 * sans Android, sans canevas. Les invariants du LayoutModel s'y
 * éprouvent sur des documents minuscules et lisibles.
 */
public class LayoutModelTest {

    private static TextBuffer bufferOf(String... lines) {
        // PAS de saut final : « aa\nbb » fait DEUX lignes (un saut
        // final en porterait une TROISIÈME, vide — la règle du tampon)
        return TextBuffer.create(String.join("\n", lines));
    }

    /** Le modèle des plis : un pli y cache les lignes 2 et 3. */
    private static FoldModel foldedModel(TextBuffer buffer) {
        FoldModel folds = new FoldModel();
        // bornes factices : le pli de la ligne 1 ferme à la ligne 3
        var bounds = new jo.pigma.editor.folding.FoldBounds() {
            @Override
            public boolean foldStartsAt(int line) {
                return line == 1;
            }

            @Override
            public int foldEndLine(int header) {
                return header == 1 ? 3 : -1;
            }
        };
        folds.rebuild(bounds, buffer.getLineCount());
        folds.fold(1);
        folds.rebuild(bounds, buffer.getLineCount());
        return folds;
    }

    private static LayoutModel model(TextBuffer buffer, FoldModel folds) {
        // mesures lisibles : interligne 10, colonne 10, vue 100 (10 rangées)
        LayoutModel model = new LayoutModel(buffer, folds);
        model.metrics(new LayoutModel.Metrics(0.0f, 10.0f, 10.0f, 100.0f));
        return model;
    }

    /** Sans pli ni coupe : chaque ligne tient une rangée, l'indice suit. */
    @Test
    public void flatDocumentMapsLinesToRows() {
        TextBuffer buffer = bufferOf("aa", "bb", "cc");
        LayoutModel model = model(buffer, new FoldModel());
        assertEquals(3, model.visibleRowCount());
        assertEquals(0, model.visibleIndexOf(0));
        assertEquals(1, model.visibleIndexOf(1));
        assertEquals(2, model.visibleIndexOf(2));
        assertEquals(0, model.lineOfVisibleIndex(0));
        assertEquals(1, model.lineOfVisibleIndex(1));
        assertEquals(2, model.lineOfVisibleIndex(2));
        assertEquals(20.0f, model.lineTop(2), 0.0f);
    }

    /** Une ligne cachée vaut l'indice de la visible qui la suit. */
    @Test
    public void hiddenLinesShareTheFollowingIndex() {
        TextBuffer buffer = bufferOf("aa", "bb", "cc", "dd", "ee");
        LayoutModel model = model(buffer, foldedModel(buffer));
        // lignes 2 et 3 cachées : 3 rangées visibles (aa, bb, ee).
        // L'INDICE d'une cachée : le COMPTE DES VISIBLES AVANT elle
        // (aa et bb : 2) — l'en-tête, les cachées et la visible qui
        // les suit y partagent le même compte, seule la HAUTEUR
        // d'une cachée s'aligne sur son en-tête (lineTop)
        assertEquals(3, model.visibleRowCount());
        assertEquals(1, model.visibleIndexOf(1));
        assertEquals(2, model.visibleIndexOf(2));
        assertEquals(2, model.visibleIndexOf(3));
        assertEquals(2, model.visibleIndexOf(4));
        // le doigt sur la rangée 2 tombe sur la ligne 4 (ee)
        assertEquals(4, model.lineOfVisibleIndex(2));
        assertEquals(20.0f, model.lineTop(4), 0.0f);
        // les cachées dessinent à la hauteur de leur en-tête : le
        // compte des visibles AVANT elles (aa, bb : 2) — l'en-tête
        // vit à 10 px, les cachées et la suite à 20 px
        assertEquals(10.0f, model.lineTop(1), 0.0f);
        assertEquals(20.0f, model.lineTop(3), 0.0f);
        assertEquals(20.0f, model.lineTop(4), 0.0f);
    }

    /** Le document court ne défile pas : la borne reste zéro. */
    @Test
    public void shortDocumentNeverScrolls() {
        TextBuffer buffer = bufferOf("aa", "bb");
        LayoutModel model = model(buffer, new FoldModel());
        assertEquals(0.0f, model.maxScrollY(), 0.0f);
    }

    /** La borne verticale : le contenu moins la vue. */
    @Test
    public void tallDocumentScrollsByContentMinusView() {
        TextBuffer buffer = bufferOf("aa", "bb", "cc", "dd", "ee", "ff");
        LayoutModel model = model(buffer, new FoldModel());
        // 6 rangées × 10 px tiennent dans 100 px de vue
        assertEquals(0.0f, model.maxScrollY(), 0.0f);
        model.metrics(new LayoutModel.Metrics(0.0f, 10.0f, 10.0f, 50.0f));
        // 6 rangées × 10 − 50
        assertEquals(10.0f, model.maxScrollY(), 0.0f);
    }

    /** Le pli raccourcit le contenu : la borne le suit. */
    @Test
    public void foldingShrinksTheScrollBound() {
        TextBuffer buffer = bufferOf("aa", "bb", "cc", "dd", "ee", "ff");
        LayoutModel flat = model(buffer, new FoldModel());
        flat.metrics(new LayoutModel.Metrics(0.0f, 10.0f, 10.0f, 50.0f));
        assertEquals(10.0f, flat.maxScrollY(), 0.0f);
        LayoutModel folded = model(buffer, foldedModel(buffer));
        folded.metrics(new LayoutModel.Metrics(0.0f, 10.0f, 10.0f, 50.0f));
        // 3 rangées visibles × 10 − 50 : plancher
        assertEquals(0.0f, folded.maxScrollY(), 0.0f);
    }

    /** Le soft wrap éteint le défilement horizontal (plancher zéro). */
    @Test
    public void softWrapKillsHorizontalScroll() {
        TextBuffer buffer = TextBuffer.create("une ligne tres longue");
        LayoutModel model = model(buffer, new FoldModel());
        // sans coupe : 21 colonnes × 10 px − 100 px de vue
        assertEquals(110.0f, model.maxScrollX(100.0f), 0.0f);
        // la coupe à 10 colonnes : plus rien ne dépasse ; 21 colonnes
        // coupent en 10 + 8 + 3 : TROIS rangées
        model.rebuildWrap(true, 100.0f);
        assertEquals(0.0f, model.maxScrollX(100.0f), 0.0f);
        assertEquals(3, model.rowsOf(0));
        assertEquals(3, model.visibleRowCount());
    }

    /** La révélation ramène la ligne ENTIÈRE dans la fenêtre. */
    @Test
    public void revealBringsTheWholeLineIntoView() {
        TextBuffer buffer = bufferOf("aa", "bb", "cc", "dd", "ee", "ff");
        LayoutModel model = model(buffer, new FoldModel());
        model.metrics(new LayoutModel.Metrics(0.0f, 10.0f, 10.0f, 30.0f));
        // 6 rangées, vue de 3 : la borne vaut 30 ; la ligne 5 vit à
        // 50 px, sa fin à 60 — vue de 10, elle exige 30 (borne) ;
        // vue de 30 déjà, elle y reste
        assertEquals(30.0f, model.maxScrollY(), 0.0f);
        assertEquals(30.0f, model.revealLine(5, 0.0f), 0.0f);
        assertEquals(30.0f, model.revealLine(5, 10.0f), 0.0f);
        // remonter : la ligne 0 vue de 30 remonte à 0
        assertEquals(0.0f, model.revealLine(0, 30.0f), 0.0f);
        // la ligne 2 vue de 30 : son haut à -10, elle remonte à 20
        assertEquals(20.0f, model.revealLine(2, 30.0f), 0.0f);
    }

    /** La ligne coupée se révèle ENTIÈRE : ses rangées comptent. */
    @Test
    public void revealCountsWrappedRows() {
        TextBuffer buffer = TextBuffer.create(
                "une ligne tres longue qui coupe");
        LayoutModel model = model(buffer, new FoldModel());
        model.rebuildWrap(true, 100.0f);
        // 31 colonnes, coupe à 10 puis des pas de 10 : 10+10+10+1
        assertEquals(4, model.rowsOf(0));
        model.metrics(new LayoutModel.Metrics(0.0f, 10.0f, 10.0f, 30.0f));
        // 4 rangées × 10 − 30 = 10 px de défilement
        assertEquals(10.0f, model.maxScrollY(), 0.0f);
        assertEquals(10.0f, model.revealLine(0, 0.0f), 0.0f);
    }

    /** Le doigt : la ligne sous l'ordonnée, la colonne bornée à la ligne. */
    @Test
    public void fingerFallsOnLineAndBoundedColumn() {
        TextBuffer buffer = bufferOf("aa", "bb", "cc");
        LayoutModel model = model(buffer, new FoldModel());
        assertEquals(1, model.lineAt(15.0f, 100.0f));
        assertEquals(2, model.lineAt(99.0f, 100.0f));
        // sous le texte : la dernière ligne
        assertEquals(2, model.lineAt(500.0f, 100.0f));
        // la colonne se borne à la longueur (2) : le doigt à droite
        assertEquals(2, model.columnAt(150.0f, 0));
        assertEquals(0, model.columnAt(-5.0f, 0));
    }

    /** Le doigt sur la coupe : la rangée 2 de la ligne coupée. */
    @Test
    public void fingerOnWrappedRowFindsItsLine() {
        TextBuffer buffer = TextBuffer.create(
                "une ligne tres longue qui coupe");
        LayoutModel model = model(buffer, new FoldModel());
        model.rebuildWrap(true, 100.0f);
        assertEquals(4, model.visibleRowCount());
        // toutes les rangées appartiennent à la ligne 0 ; la rangée 3
        // vit à 30-40 px — sous la vue de 30 px elle se borne à la
        // dernière RANGÉE visible (la 2) : la ligne reste 0
        for (int row = 0; row < 3; row++) {
            assertEquals(0, model.lineAt(row * 10.0f + 5.0f, 100.0f));
        }
        // l'indice visuel de la ligne 0 reste 0, sa hauteur 4 rangées
        assertEquals(0, model.visibleIndexOf(0));
    }

    /** Le modèle du pli sans coupe garde la voix de FoldModel. */
    @Test
    public void wrapOffDefersToFoldModel() {
        TextBuffer buffer = bufferOf("aa", "bb", "cc", "dd", "ee");
        FoldModel folds = foldedModel(buffer);
        LayoutModel model = model(buffer, folds);
        assertEquals(folds.visibleIndexOf(4), model.visibleIndexOf(4));
        assertEquals(folds.lineOfVisibleIndex(2, buffer.getLineCount()),
                model.lineOfVisibleIndex(2));
    }

    /** Les mesures nulles ne cassent rien : la ligne du doigt tombe à la fin. */
    @Test
    public void degenerateMetricsStaySafe() {
        TextBuffer buffer = bufferOf("aa", "bb");
        LayoutModel model = new LayoutModel(buffer, new FoldModel());
        model.metrics(new LayoutModel.Metrics(0.0f, 0.0f, 0.0f, 0.0f));
        assertEquals(1, model.lineAt(50.0f, 100.0f));
        assertEquals(0, model.columnAt(50.0f, 0));
        assertEquals(0.0f, model.maxScrollY(), 0.0f);
    }

    /** La coupe se reconstruit : éteinte puis rallumée, les rangées reviennent. */
    @Test
    public void wrapRebuildToggles() {
        TextBuffer buffer = TextBuffer.create(
                "une ligne tres longue qui coupe");
        LayoutModel model = model(buffer, new FoldModel());
        assertEquals(1, model.rowsOf(0));
        model.rebuildWrap(true, 100.0f);
        assertEquals(4, model.rowsOf(0));
        model.rebuildWrap(false, 100.0f);
        assertEquals(1, model.rowsOf(0));
        model.rebuildWrap(true, 100.0f);
        assertEquals(4, model.rowsOf(0));
    }

    /** Le pas de coupe : la largeur MOINS l'indent, au plancher de 8. */
    @Test
    public void unindentedLineStepsByFullWidth() {
        // 26 colonnes sans blanc de tête, coupe à 10 : le pas vaut
        // max(8, 10-0) = 10 — 10 + 10 + 6 : TROIS rangées
        TextBuffer buffer = TextBuffer.create("abcdefghijklmnopqrstuvwxyz");
        LayoutModel model = model(buffer, new FoldModel());
        model.rebuildWrap(true, 100.0f);
        assertEquals(3, model.rowsOf(0));
        // indentée de 4 : le pas se resserre à max(8, 10-4) = 8 —
        // 10 + 8 + 6 : TROIS rangées aussi, la suite avance moins vite
        TextBuffer indented = TextBuffer.create("    abcdefghijklmnopqrstuv");
        LayoutModel other = model(indented, new FoldModel());
        other.rebuildWrap(true, 100.0f);
        assertEquals(3, other.rowsOf(0));
        // la même sans coupe : une rangée
        LayoutModel flat = model(indented, new FoldModel());
        assertEquals(1, flat.rowsOf(0));
    }
}
