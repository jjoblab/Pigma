package jo.pigma.sample;

import android.graphics.Canvas;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import jo.pigma.core.TextBuffer;
import jo.pigma.editor.EditorView;
import jo.pigma.editor.popups.CompletionCard;
import jo.pigma.editor.popups.GotoBar;
import jo.pigma.editor.popups.HistoryCard;
import jo.pigma.editor.popups.LocalHistory;
import jo.pigma.editor.popups.SearchBar;
import jo.pigma.editor.popups.SignatureCard;
import jo.pigma.syntax.SyntaxDocument;

/**
 * Harnais d'ouverture : éprouve l'application de démonstration sur
 * machine virtuelle, sans appareil — la création, le premier dessin,
 * le remplissage du fond, les langages, le thème, la frappe et le
 * clavier par événements, la sélection et ses poignées, l'appui long
 * qui glisse, la barre de sélection et son débordement, la recherche et le
 * remplacement, le fichier hors
 * normes, les plis, la fusion sémantique, le serveur de langage de
 * démonstration et la copie des mesures. Chaque scénario s'imprime :
 * le résumé final dit tout vert ou non, le code de sortie suit.
 *
 * <p>Les stubs Android vivent dans {@code src/stubs/} : la géométrie
 * y est fixe (densité 2, corps de texte 28 pixels, colonne de 0,6
 * corps, ligne de 1,15 corps plus 2 dp) — les positions de contact
 * s'en dérivent, les lignes de base se lisent dans le canevas
 * enregistré.</p>
 */
public final class OpenHarness {

    /** Largeur de la vue éprouvée (pixels). */
    private static final int VIEW_WIDTH = 1080;

    /** Hauteur de la vue éprouvée (pixels). */
    private static final int VIEW_HEIGHT = 1800;

    /** Densité des métriques des stubs (dp en pixels). */
    private static final float DENSITY = 2.0f;

    /** Corps du texte de l'éditeur (14 sp à densité 2). */
    private static final float TEXT_BODY = 14.0f * DENSITY;

    /** Montée du plus haut glyphe (métrique des stubs : 0,9 corps). */
    private static final float FONT_TOP = -0.90f * TEXT_BODY;

    /** Hauteur d'une ligne (métrique des stubs : 1,15 corps + 2 dp). */
    private static final float LINE_HEIGHT =
            (float) Math.ceil(0.25f * TEXT_BODY - FONT_TOP) + 2.0f * DENSITY;

    private static MainActivity activity;
    private static EditorView editor;
    private static Canvas canvas;
    private static InputConnection connection;
    private static int failures;
    private static int scenarios;

    private OpenHarness() {
    }

    /**
     * Point d'entrée : déroule les scénarios et rend la main avec un
     * code de sortie nul si tout est vert.
     *
     * @param args aucun argument attendu
     * @throws Exception si un scénario lève (l'échec se voit)
     */
    public static void main(String[] args) throws Exception {
        System.out.println("harnais d'ouverture — Pigma");
        activity = new MainActivity();
        activity.harnessCreate();
        editor = findEditor(activity.harnessRootView());
        editor.harnessLayout(VIEW_WIDTH, VIEW_HEIGHT);
        canvas = new Canvas();
        connection = editor.onCreateInputConnection(new EditorInfo());

        opening();
        panel();
        themeToggle();
        kotlinDemo();
        imeTyping();
        keyEvents();
        enterDeleteStress("Kotlin", 28);
        propagatedStrike();
        bigFileAndStress();
        selectAllAndCopyText();
        doubleTapWord();
        handleDrag();
        loupe();
        longPressDrag();
        dragDropText();
        dragDropLoupeAndPreview();
        dragDropEdgeScroll();
        searchAndReplace();
        gotoLine();
        hugeFile();
        hugeFileCopyGesture();
        replaceAllHugeFile();
        hugeFileLanguages();
        folds();
        semanticFusion();
        completionCard();
        signatureCard();
        quickDoc();
        selectionBar();
        popupsSecondGeneration();
        languageServer();
        copyMeasures();

        System.out.println();
        System.out.println("résumé : " + (scenarios - failures) + "/" + scenarios
                + " scénario(s) vert(s)");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ---------------------------------------------------------- scénarios

    /** L'ouverture : la création, l'attachement, le premier dessin. */
    private static void opening() {
        begin("ouverture : création, attachement, premier dessin");
        frame();
        View.harnessDrain(1_000L);
        editor.awaitBackgroundFill(10_000L);
        View.harnessDrain(2_000L);
        frame();
        SyntaxDocument syntax = editor.getSyntaxDocument();
        check("18/18 lignes lexées à l'ouverture (la démo Java)",
                syntax.lexedLineCount() == 18 && syntax.lineCount() == 18);
        check("des jetons existent", syntax.tokenCount() > 0);
        check("la première coloration s'est chronométrée",
                syntax.firstLexNanos() > 0L);
        detail(syntax.lexedLineCount() + "/" + syntax.lineCount()
                + " lignes, " + syntax.tokenCount() + " jetons");
    }

    /** Le panneau de diagnostic et la ligne d'état se remplissent. */
    private static void panel() {
        begin("panneau : mesures et état");
        check("le panneau porte les mesures de coloration",
                textOf("coloration :") != null);
        check("la ligne d'état porte la révision",
                textOf("rév.") != null);
    }

    /** La bascule de thème : aucune retokenisation, le journal suit. */
    private static void themeToggle() {
        begin("thème : bascule sombre sans retokenisation");
        long tokens = editor.getSyntaxDocument().tokenCount();
        click("Sombre");
        frame();
        check("les jetons ne changent pas à la bascule",
                editor.getSyntaxDocument().tokenCount() == tokens);
        click("Clair");
        frame();
        check("les jetons ne changent pas au retour",
                editor.getSyntaxDocument().tokenCount() == tokens);
    }

    /** La démo Kotlin : 28 lignes, le remplissage complet du fond. */
    private static void kotlinDemo() {
        begin("Kotlin : ouverture et remplissage");
        openLanguage("Kotlin", 10_000L);
        SyntaxDocument syntax = editor.getSyntaxDocument();
        check("28/28 lignes lexées", syntax.lexedLineCount() == 28);
        check("307 jetons", syntax.tokenCount() == 307);
        detail(syntax.lexedLineCount() + "/28, " + syntax.tokenCount() + " jetons");
    }

    /** La frappe au clavier : commit, une ligne relue, annuler, rétablir. */
    private static void imeTyping() {
        begin("IME : frappe, annulation, rétablissement");
        String original = editor.getBuffer().toString();
        int before = original.length();
        connection.commitText("z", 1);
        check("le commit insère un caractère",
                editor.getBuffer().length() == before + 1);
        check("une seule ligne relue", editor.getSyntaxDocument().lastRelexedLines() == 1);
        click("Annuler");
        check("l'annulation retire le caractère",
                editor.getBuffer().length() == before);
        click("Rétablir");
        check("le rétablissement remet le caractère",
                editor.getBuffer().length() == before + 1);
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
        check("Suppr retire le caractère",
                editor.getBuffer().length() == before);
        check("le texte est exactement l'original",
                editor.getBuffer().toString().equals(original));
    }

    /** Le clavier par événements : Entrée, Suppr avant, flèches, action. */
    private static void keyEvents() {
        begin("clavier par événements : Entrée, Suppr avant, flèches");
        String original = editor.getBuffer().toString();
        int before = original.length();
        pressKey(KeyEvent.KEYCODE_ENTER);
        check("Entrée insère un saut de ligne",
                editor.getBuffer().length() == before + 1);
        pressKey(KeyEvent.KEYCODE_DEL);
        check("Suppr retire le saut de ligne",
                editor.getBuffer().length() == before);
        pressKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        pressKey(KeyEvent.KEYCODE_DPAD_DOWN);
        pressKey(KeyEvent.KEYCODE_DPAD_LEFT);
        pressKey(KeyEvent.KEYCODE_DPAD_UP);
        check("les flèches ne changent pas le texte",
                editor.getBuffer().length() == before);
        pressKey(KeyEvent.KEYCODE_FORWARD_DEL);
        check("Suppr avant retire un caractère",
                editor.getBuffer().length() == before - 1);
        click("Annuler");
        check("l'annulation rend le caractère",
                editor.getBuffer().length() == before);
        connection.setSelection(0, 0);
        check("le clavier peut déplacer la sélection",
                editor.getCaretOffset() == 0);
        connection.performEditorAction(0);
        check("la touche action insère un saut de ligne",
                editor.getBuffer().length() == before + 1);
        pressKey(KeyEvent.KEYCODE_DEL);
        check("le texte est exactement l'original",
                editor.getBuffer().toString().equals(original));
    }

    /**
     * Entrée puis Suppr à chaque début de ligne : la longueur revient
     * toujours (les bords et l'intérieur se croisent).
     *
     * @param language le bouton de langage à ouvrir avant le stress
     * @param expectedLines le nombre de lignes attendu du document
     */
    private static void enterDeleteStress(String language, int expectedLines) {
        begin("Entrée + Suppr à chaque début de ligne (" + language + ")");
        openLanguage(language, 20_000L);
        TextBuffer buffer = editor.getBuffer();
        String original = buffer.toString();
        check("le document a le nombre de lignes attendu",
                buffer.getLineCount() == expectedLines);
        for (int line = 0; line < buffer.getLineCount(); line++) {
            editor.caretTo(buffer.getLineStart(line));
            pressKey(KeyEvent.KEYCODE_ENTER);
            pressKey(KeyEvent.KEYCODE_DEL);
        }
        check("le texte revient identique après " + buffer.getLineCount()
                + " couples Entrée + Suppr", buffer.toString().equals(original));
        editor.getUndoStack().clear();
        detail(buffer.getLineCount() + " couples passés");
    }

    /** Le gros fichier : remplissage complet, puis le même stress. */
    private static void bigFileAndStress() {
        begin("gros fichier : 1 780 lignes au fond");
        openLanguage("Gros", 30_000L);
        SyntaxDocument syntax = editor.getSyntaxDocument();
        check("1 780/1 780 lignes lexées",
                syntax.lexedLineCount() == 1780 && syntax.lineCount() == 1780);
        check("18 095 jetons", syntax.tokenCount() == 18095);
        enterDeleteStress("Gros", 1780);
    }

    /** Tout sélectionner, lire la sélection, puis quitter. */
    private static void selectAllAndCopyText() {
        begin("sélection : tout, copie du texte, sortie");
        openLanguage("Java", 10_000L);
        int length = editor.getBuffer().length();
        click("Tout sélectionner");
        check("la sélection couvre le document",
                editor.hasSelection() && editor.selectionStart() == 0
                        && editor.selectionEnd() == length);
        click("Copier texte");
        android.content.ClipData copied =
                android.content.Context.harnessClipboard().getPrimaryClip();
        check("le presse-papiers porte le texte entier",
                copied != null && copied.getItemCount() == 1
                        && "sélection Pigma".contentEquals(copied.getLabel())
                        && copied.getItemAt(0).getText().length() == length);
        click("Quitter la sélection");
        check("la sélection se range", !editor.hasSelection());
    }

    /** Le double-tap sélectionne le mot sous le doigt. */
    private static void doubleTapWord() {
        begin("double-tap : le mot sous le doigt");
        String text = editor.getBuffer().toString();
        int wordAt = text.indexOf("graine");
        int wordEnd = wordAt + "graine".length();
        float[] spot = spotOfWord("graine");
        doubleTap(spot[0], spot[1]);
        check("la sélection épouse le mot",
                editor.hasSelection() && editor.selectionStart() == wordAt
                        && editor.selectionEnd() == wordEnd);
    }

    /** La poignée traînée : la sélection s'étend au doigt. */
    private static void handleDrag() {
        begin("poignée : la borne traînée étend la sélection");
        check("une sélection vivante précède le traînement", editor.hasSelection());
        TextBuffer buffer = editor.getBuffer();
        int start = editor.selectionStart();
        int end = editor.selectionEnd();
        int line = buffer.lineOfOffset(end);
        int column = end - buffer.getLineStart(line);
        float handleX = editor.getPaddingLeft() + column * editor.charWidth();
        float handleY = lineTop(line) + LINE_HEIGHT + 5.0f * DENSITY;
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN, handleX, handleY));
        // le glissement se juge à mi-hauteur de la ligne : la poignée
        // vit SOUS la borne, un déplacement à sa hauteur désignerait
        // la ligne suivante à la translation de position
        float lineMidY = lineTop(line) + LINE_HEIGHT / 2.0f;
        float targetX = editor.getPaddingLeft() + (column + 4) * editor.charWidth();
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_MOVE, targetX, lineMidY));
        frame();
        check("les deux poignées se dessinent pendant le traînement",
                canvas.harnessCircleCount() >= 2);
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP, targetX, lineMidY));
        check("la sélection s'étend de quatre colonnes",
                editor.selectionEnd() - editor.selectionStart()
                        == (end - start) + 4);
        detail("sélection " + editor.selectionStart() + " → " + editor.selectionEnd());
    }

    /** La loupe de précision : la borne traînée se lit agrandie. */
    private static void loupe() {
        begin("loupe : le rendu de la borne se blitte agrandi");
        doubleTapOfWord("graine");
        TextBuffer buffer = editor.getBuffer();
        int end = editor.selectionEnd();
        int line = buffer.lineOfOffset(end);
        int column = end - buffer.getLineStart(line);
        float handleX = editor.getPaddingLeft() + column * editor.charWidth();
        float handleY = lineTop(line) + LINE_HEIGHT + 5.0f * DENSITY;
        float lineMidY = lineTop(line) + LINE_HEIGHT / 2.0f;
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN, handleX, handleY));
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_MOVE, handleX, lineMidY));
        frame();
        // la loupe blitte : la ligne rendue UNE FOIS à l'échelle 1
        // (hors trame), la trame ne paie plus la rastérisation à
        // l'échelle 2 (le pire 16,55 du relevé 16 y trouvait sa cause)
        Canvas.BitmapOp blit = null;
        for (Canvas.BitmapOp op : canvas.harnessBitmapsSinceMark()) {
            blit = op;
        }
        check("le rendu de la loupe se blitte (rastérisé hors trame)",
                blit != null && blit.dstRight - blit.dstLeft > 0.0f);
        check("le blit agrandit (destination double de la source)",
                blit != null && (blit.dstRight - blit.dstLeft)
                        > 1.9f * Math.max(1.0f, blit.srcRight - blit.srcLeft));
        boolean bounded = false;
        for (Canvas.ClipOp op : canvas.harnessClipsSinceMark()) {
            if (op.right - op.left < VIEW_WIDTH / 2.0f) {
                bounded = true;
            }
        }
        check("la loupe découpe une fenêtre bornée", bounded);
        check("le cadre arrondi se dessine", canvas.harnessRoundRectCount() >= 1);
        detail(blit == null
                ? "aucun blit" : "blit : source [" + blit.srcLeft + ", "
                        + blit.srcRight + "] vers [" + blit.dstLeft + ", "
                        + blit.dstRight + "]");
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP, handleX, lineMidY));
        frame();
        check("la loupe se ferme au lâcher",
                canvas.harnessBitmapsSinceMark().isEmpty());
    }

    /** L'appui long qui glisse : la sélection naît du mot sous l'appui. */
    private static void longPressDrag() throws InterruptedException {        begin("appui long qui glisse : le mot se sème, la borne suit");
        tap(400.0f, 80.0f);
        check("la sélection se range avant l'appui long", !editor.hasSelection());
        float[] spot = spotOfWord("SEUIL");
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN, spot[0], spot[1]));
        Thread.sleep(600L);
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_MOVE,
                spot[0] + 40.0f, spot[1]));
        check("la sélection naît du mot sous l'appui", editor.hasSelection());
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP,
                spot[0] + 40.0f, spot[1]));
        frame();
    }

    /**
     * Le traîné-déposé : la sélection saisie dans son propre texte se
     * déplace au doigt — une seule unité à défaire, le dépôt sur une
     * autre ligne traverse les lignes.
     */
    private static void dragDropText() throws InterruptedException {
        begin("traîné-déposé : la sélection se déplace au doigt");
        openLanguage("Java", 10_000L);
        doubleTapOfWord("graine");
        frame();
        TextBuffer buffer = editor.getBuffer();
        int start = editor.selectionStart();
        int end = editor.selectionEnd();
        check("un mot est sélectionné", start < end);
        String moved = buffer.getText(start, end);
        String before = buffer.toString();
        int line = buffer.lineOfOffset(start);
        int column = start - buffer.getLineStart(line);
        // saisir au milieu de la sélection, traîner de quatre
        // colonnes vers la gauche : le dépôt précède la sélection
        float grabX = editor.getPaddingLeft()
                + (column + (end - start) / 2) * editor.charWidth();
        float dropX = editor.getPaddingLeft()
                + (column - 4) * editor.charWidth();
        float lineMidY = lineTop(line) + LINE_HEIGHT / 2.0f;
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_DOWN, grabX, lineMidY));
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_MOVE, (grabX + dropX) / 2.0f, lineMidY));
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_MOVE, dropX, lineMidY));
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_UP, dropX, lineMidY));
        frame();
        int drop = start - 4;
        check("le texte sélectionné s'est déplacé au point de dépôt",
                buffer.getText(drop, drop + moved.length()).equals(moved)
                        && buffer.length() == before.length());
        check("le curseur se pose après le texte déposé",
                editor.getCaretOffset() == drop + moved.length());
        click("Annuler");
        check("l'annulation rend le texte d'origine (une seule unité)",
                buffer.toString().equals(before));
        // deuxième traîné : une sélection LONGUE d'abord — l'appui
        // long qui glit sème le mot et l'étend (son milieu est loin
        // des poignées, un mot court y serait saisi par elles) — le
        // dépôt se fait sur la ligne AU-DESSUS
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_DOWN, grabX, lineMidY));
        Thread.sleep(600L);
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_MOVE, grabX + 3.5f * editor.charWidth(), lineMidY));
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_UP, grabX + 3.5f * editor.charWidth(), lineMidY));
        frame();
        start = editor.selectionStart();
        end = editor.selectionEnd();
        check("l'appui long qui glit a semé une sélection longue",
                end - start >= 5);
        moved = buffer.getText(start, end);
        before = buffer.toString();
        line = buffer.lineOfOffset(start);
        column = start - buffer.getLineStart(line);
        grabX = editor.getPaddingLeft()
                + (column + (end - start) / 2) * editor.charWidth();
        lineMidY = lineTop(line) + LINE_HEIGHT / 2.0f;
        float upY = lineTop(line - 1) + LINE_HEIGHT / 2.0f;
        // le point de dépôt attendu se calcule AVANT le geste : la
        // ligne cible change au dépôt (elle reçoit le text)
        int upLineStart = buffer.getLineStart(line - 1);
        int upDrop = upLineStart + Math.min(column + (end - start) / 2,
                buffer.getLineEnd(line - 1) - upLineStart);
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_DOWN, grabX, lineMidY));
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_MOVE, grabX, (lineMidY + upY) / 2.0f));
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_MOVE, grabX, upY));
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_UP, grabX, upY));
        frame();
        detail("déplacement «" + moved + "» [" + start + "," + end + ") déposé à "
                + upDrop + ", ligne " + (line - 1) + " après «"
                + buffer.getText(upLineStart, buffer.getLineEnd(line - 1)) + "»");
        check("le texte traverse les lignes au dépôt",
                buffer.getText(upDrop, upDrop + moved.length()).equals(moved)
                        && buffer.length() == before.length()
                        && editor.getCaretOffset() == upDrop + moved.length());
        click("Annuler");
        check("l'annulation rend encore le texte",
                buffer.toString().equals(before));
    }

    /**
     * La loupe et la pastille du traîné-déposé : la ligne du point de
     * dépôt se blitte agrandie (la loupe du traîné, la même que celle
     * des poignées), le texte traîné flotte sous le doigt en pastille
     * raccourcie (l'ellipse marque la coupe). Le dépôt dans la
     * sélection ne déplace rien : le geste se lâche sans trace.
     */
    private static void dragDropLoupeAndPreview() {
        begin("traîné-déposé : la loupe du dépôt, la pastille du texte");
        openLanguage("Gros", 10_000L);
        click("Tout sélectionner");
        frame();
        String before = editor.getBuffer().toString();
        float grabX = editor.getPaddingLeft() + 40.0f;
        float lineMidY = lineTop(0) + LINE_HEIGHT / 2.0f;
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN, grabX, lineMidY));
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_MOVE, grabX - 40.0f, lineMidY));
        frame();
        // la loupe du traîné : le rendu de la ligne de dépôt, blitté
        Canvas.BitmapOp blit = null;
        for (Canvas.BitmapOp op : canvas.harnessBitmapsSinceMark()) {
            blit = op;
        }
        check("la loupe du traîné blitte la ligne de dépôt",
                blit != null && blit.dstRight - blit.dstLeft > 0.0f);
        check("le blit agrandit (destination double de la source)",
                blit != null && (blit.dstRight - blit.dstLeft)
                        > 1.9f * Math.max(1.0f, blit.srcRight - blit.srcLeft));
        // la pastille : le texte traîné, raccourci à vingt colonnes —
        // la première ligne du gros fichier est longue, l'ellipse de
        // la coupe la distingue du texte de la ligne
        boolean chip = false;
        for (Canvas.TextOp op : canvas.harnessTextsSinceMark()) {
            if (op.text.endsWith("…")) {
                chip = true;
            }
        }
        check("la pastille du texte traîné se dessine (l'ellipse de la coupe)",
                chip);
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_UP, grabX - 40.0f, lineMidY));
        frame();
        check("la loupe se ferme au lâcher",
                canvas.harnessBitmapsSinceMark().isEmpty());
        check("le dépôt dans la sélection ne déplace rien",
                editor.getBuffer().toString().equals(before));
        // le dépôt sans déplacement range la sélection : le bouton
        // « Tout sélectionner » re-part de zéro au scénario suivant
    }

    /**
     * Le défilement au bord du traîné-déposé : le doigt tient la zone
     * du haut, la vue glitte par échéances (une demi-ligne par
     * échéance, la suite se reprogramme tant que le doigt tient) — le
     * point de dépôt reste dans la fenêtre visible. La sélection est
     * entière : les poignées vivent hors écran, la saisie au milieu de
     * l'écran arme le traîné (un mot court y serait saisi par ses
     * poignées — leur rayon domine).
     */
    private static void dragDropEdgeScroll() {
        begin("traîné-déposé : le défilement au bord suit le doigt");
        openLanguage("Gros", 10_000L);
        // le scénario précédent laisse la vue près de la fin (le
        // bouton de sélection amène le curseur au bout) : le bord HAUT
        // offre la profondeur — le doigt y tient, la vue remonte.
        // L'étiquette du bouton est restée « Quitter la sélection »
        // (le dépôt sans déplacement range la sélection sans la
        // rabattre) : sa branche autre sélectionne tout de neuf.
        click("Quitter la sélection");
        frame();
        TextBuffer buffer = editor.getBuffer();
        float grabX = editor.getPaddingLeft() + 40.0f;
        float grabY = VIEW_HEIGHT / 2.0f;
        float edgeY = editor.getPaddingTop() + 3.0f;
        int before = editor.firstVisibleLine();
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_DOWN, grabX, grabY));
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_MOVE, grabX, edgeY));
        // le doigt tient la zone du haut : les échéances défilent la vue
        View.harnessDrain(400L);
        int scrolled = editor.firstVisibleLine();
        check("la vue a défilé pendant que le doigt tient le bord",
                scrolled < before);
        detail("première ligne " + before + " → " + scrolled);
        int dropLine = buffer.lineOfOffset(editor.harnessDropOffset());
        check("le point de dépôt reste dans la fenêtre visible",
                dropLine >= editor.firstVisibleLine()
                        && dropLine <= editor.lastVisibleLine());
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_UP, grabX, edgeY));
        frame();
    }

    /**
     * La frappe propagée : supprimer la fermeture d'un commentaire
     * relit la cascade jusqu'à la nouvelle fermeture — la rangée « à
     * relever » du tableau de mesure vit enfin ses nombres.
     */
    private static void propagatedStrike() {
        begin("frappe propagée : la fermeture du bloc déportée relit la cascade");
        openLanguage("Java", 10_000L);
        TextBuffer buffer = editor.getBuffer();
        String text = buffer.toString();
        // la fermeture du commentaire de documentation : « */ »
        int close = text.indexOf("*/");
        int closeLine = buffer.lineOfOffset(close);
        editor.caretTo(close + 2);
        frame();
        // la première suppression (le « / » de la fermeture) ouvre le
        // commentaire : la cascade relit jusqu'à la fin du document —
        // c'est la frappe propagée, la rangée « à relever » du tableau
        pressKey(KeyEvent.KEYCODE_DEL);
        frame();
        jo.pigma.syntax.SyntaxDocument syntax = editor.getSyntaxDocument();
        int cascadeLines = syntax.lastRelexedLines();
        double cascadeMillis = syntax.lastEditNanos() / 1_000_000.0;
        check("la frappe propagée relit au-delà de la ligne",
                cascadeLines > 1);
        // la seconde suppression (le « * ») ne change pas l'état : le
        // commentaire reste ouvert — la paresse tient DANS le bloc, une
        // seule ligne relue
        pressKey(KeyEvent.KEYCODE_DEL);
        frame();
        check("les deux caractères de la fermeture sont partis",
                buffer.length() == text.length() - 2);
        check("la frappe suivante dans le bloc ouvert relit une ligne",
                syntax.lastRelexedLines() == 1);
        detail("frappe propagée : " + cascadeMillis + " ms, " + cascadeLines
                + " ligne(s) relue(s) — la cascade court de la ligne "
                + (closeLine + 1) + " à la fin du document ; la frappe "
                + "ordinaire dans le bloc : " + syntax.lastEditNanos() / 1_000_000.0
                + " ms, " + syntax.lastRelexedLines() + " ligne(s)");
        click("Annuler");
        click("Annuler");
        check("la double annulation rend la fermeture",
                buffer.toString().equals(text));
    }

    /** La carte de recherche : chercher, naviguer, remplacer, tout. */
    private static void searchAndReplace() {
        begin("recherche : compter, naviguer, remplacer, tout remplacer");
        click("Rechercher");
        SearchBar bar = editor.searchBar();
        check("la carte vit DANS la fenêtre de l'éditeur (celeris : jamais de PopupWindow)",
                bar != null && bar.isShown());
        frame();
        check("la carte se pose au caret — dessous ou dessus, dans les murs",
                (bar.poseY() > bar.ancreY() || bar.poseY() + bar.height() < bar.ancreY())
                        && bar.poseX() >= 0
                        && bar.poseX() + bar.width() <= editor.getWidth());
        String text = editor.getBuffer().toString();
        int expected = countOccurrences(text, "graine");
        // la recherche incrémentale : la frappe passe par le clavier
        // DE L'ÉDITEUR (la carte ne prend jamais le focus,
        // le contrat d'entrée lui route les touches)
        // le clavier logiciel COMPOSE (setComposingText) : chaque
        // mise à jour REMPLACE la précédente, la validation remplace
        // la composition — la carte le sait (onComposition)
        connection.setComposingText("g", 1);
        View.harnessDrain(400L);
        check("la recherche suit la première frappe (g)",
                editor.searchMatchCount() == countOccurrences(text, "g"));
        connection.setComposingText("gr", 1);
        View.harnessDrain(400L);
        check("la recherche suit la deuxième frappe (gr)",
                editor.searchMatchCount() == countOccurrences(text, "gr"));
        connection.setComposingText("graine", 1);
        View.harnessDrain(400L);
        check("le compte exact des correspondances est arrivé à la frappe finale",
                editor.searchMatchCount() == expected);
        connection.commitText("graine", 1);
        View.harnessDrain(400L);
        check("la validation remplace la composition (la requête tient)",
                bar.queryText().equals("graine"));
        check("le compte « i/n » se lit sur la carte",
                bar.counterLabel().endsWith("/" + expected));
        // le remplacement se replie sous la recherche : le rabat le
        // montre (le Ctrl+R de l'atelier)
        check("le remplacement se replie au départ", !bar.replaceRowShown());
        bar.showReplaceRow();
        check("le rabat montre le remplacement", bar.replaceRowShown());
        // la frappe va au champ actif (le remplacement du rabat)
        connection.commitText("GRAINE", 1);
        check("la frappe du rabat écrit le remplacement",
                bar.replacementText().equals("GRAINE"));
        bar.focusQueryField();
        // la navigation : les flèches du clavier déléguées à la carte
        int beforeNext = editor.searchCurrentIndex();
        pressKey(KeyEvent.KEYCODE_DPAD_DOWN);
        check("la flèche bas avance (le clavier délégué à la carte)",
                editor.searchCurrentIndex() == beforeNext + 1);
        pressKey(KeyEvent.KEYCODE_DPAD_UP);
        check("la flèche haut recule",
                editor.searchCurrentIndex() == beforeNext);
        bar.replaceCurrent();
        check("un remplacement a eu lieu",
                countOccurrences(editor.getBuffer().toString(), "graine") == expected - 1);
        click("Annuler");
        check("l'annulation rend le texte",
                editor.getBuffer().toString().equals(text));
        // la recherche revient (l'annulation a rendu le text)
        editor.search("graine");
        check("la recherche revenue compte les correspondances",
                editor.searchMatchCount() == expected);
        bar.replaceEverywhere();
        check("tout remplacer vide les correspondances",
                countOccurrences(editor.getBuffer().toString(), "graine") == 0);
        boolean toasted = false;
        for (CharSequence notice : Toast.harnessShown()) {
            toasted = notice.toString().contains("remplacement");
        }
        check("le compte des remplacements se notifie", toasted);
        click("Annuler");
        check("l'annulation rend le texte après le tout",
                editor.getBuffer().toString().equals(text));
        // le doigt sur la carte lui appartient (les popups
        // se dessinent et se touchent) : le tap s'y consomme, le
        // caret ne bouge pas
        int caretBefore = editor.selectionStart();
        float tapX = bar.poseX() + bar.width() * 0.4f;
        float tapY = bar.poseY() + bar.height() * 0.25f;
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN, tapX, tapY));
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP, tapX, tapY));
        check("le doigt sur la carte s'y consomme (le caret ne bouge pas)",
                editor.selectionStart() == caretBefore);
        bar.close();
        check("la carte se range et la recherche avec",
                !bar.isShown() && editor.searchMatchCount() == 0);
        // la carte suit le caret : ouverte ailleurs, elle s'y repose
        // (l'ancre X se déplace avec la colonne du caret — le
        // défilement n'y touche pas)
        click("Rechercher");
        frame();
        float firstAnchorX = editor.searchBar().ancreX();
        editor.caretTo(editor.getBuffer().getLineStart(5) + 3);
        frame();
        check("la carte suit le caret (l'ancre se repose à sa colonne)",
                editor.searchBar().ancreX() != firstAnchorX);
        editor.searchBar().close();
        detail(expected + " correspondances de « graine »");
    }

    /**
     * La carte aller-à-la-ligne : l'invite au caret, le numéro se
     * borne au fichier, la touche « Aller » du clavier valide, la
     * croix range la carte.
     */
    private static void gotoLine() {
        begin("aller à la ligne : invite, borne, touche d'action, fermeture");
        click("Aller à");
        GotoBar carte = editor.gotoBar();
        check("la carte aller-à vit DANS la fenêtre de l'éditeur",
                carte != null && carte.isShown());
        frame();
        check("la carte se pose au caret — dessous ou dessus, dans les murs",
                (carte.poseY() > carte.ancreY()
                        || carte.poseY() + carte.height() < carte.ancreY())
                        && carte.poseX() >= 0
                        && carte.poseX() + carte.width() <= editor.getWidth());
        check("l'ouverture compte le fichier",
                carte.feedbackLabel().contains("ligne"));
        // les chiffres passent par le clavier de l'éditeur (la carte
        // ne prend jamais le focus)
        connection.commitText("999999", 1);
        check("les chiffres se posent sur la carte",
                carte.digitsText().equals("999999"));
        pressKey(KeyEvent.KEYCODE_ENTER);
        check("le numéro au-delà va à la dernière ligne",
                editor.selectionStart() == editor.getBuffer().getLineStart(
                        editor.getBuffer().getLineCount() - 1));
        // le retour arrière appartient à la carte : les chiffres
        // s'effacent, le nouveau numéro s'écrit
        pressKey(KeyEvent.KEYCODE_DEL);
        pressKey(KeyEvent.KEYCODE_DEL);
        pressKey(KeyEvent.KEYCODE_DEL);
        pressKey(KeyEvent.KEYCODE_DEL);
        pressKey(KeyEvent.KEYCODE_DEL);
        pressKey(KeyEvent.KEYCODE_DEL);
        connection.commitText("1", 1);
        pressKey(KeyEvent.KEYCODE_ENTER);
        check("la première ligne se pose",
                editor.selectionStart() == 0);
        // le « 1 » s'efface (le retour arrière appartient à la carte)
        pressKey(KeyEvent.KEYCODE_DEL);
        connection.commitText("3", 1);
        pressKey(KeyEvent.KEYCODE_ENTER);
        check("la touche « Aller » du clavier valide",
                editor.selectionStart() == editor.getBuffer().getLineStart(2));
        check("le retour dit la ligne où le curseur est tombé",
                carte.feedbackLabel().contains("ligne 3"));
        // la frappe hors chiffres s'avale : rien ne s'écrit dans le
        // document tant que la carte vit
        String avant = editor.getBuffer().toString();
        connection.commitText("x", 1);
        check("la frappe hors chiffres ne touche pas au document",
                editor.getBuffer().toString().equals(avant));
        carte.close();
        check("la carte se ferme", !carte.isShown());
    }

    /** Le fichier hors normes : 52 004 lignes, 624 002 jetons. */
    private static void hugeFile() {
        begin("hors normes : 52 004 lignes, mode gros fichier");
        openLanguage("50k l.", 120_000L);
        SyntaxDocument syntax = editor.getSyntaxDocument();
        check("52 004/52 004 lignes lexées",
                syntax.lexedLineCount() == 52004 && syntax.lineCount() == 52004);
        check("624 002 jetons", syntax.tokenCount() == 624002);
        check("le mode gros fichier est actif", syntax.inBigFileMode());
        check("le panneau le dit", textOf("mode gros fichier") != null);
        detail(syntax.tokenCount() + " jetons, fond "
                + editor.backgroundFillNanos() / 1_000_000L + " ms");
        // la réouverture du même texte : le document est déjà exact —
        // le fond ne repart pas, les plans restent cuits (le relevé 15
        // a payé sept re-remplissages et une trame de 762 ms pour rien)
        long fillNanos = editor.backgroundFillNanos();
        long slices = editor.backgroundFillSlices();
        long bakes = editor.renderCacheBakes();
        click("50k l.");
        frame();
        check("la réouverture du même texte ne relance pas le fond",
                editor.backgroundFillNanos() == fillNanos
                        && editor.backgroundFillSlices() == slices
                        && editor.getSyntaxDocument().lexedLineCount() == 52004);
        check("les plans cuits restent (aucune recuisson à la réouverture)",
                editor.renderCacheBakes() == bakes);
        // un texte édité, lui, repart du document d'origine (le plein
        // chemin) — et l'historique à défaire repart vide au chargement
        connection.commitText("z", 1);
        frame();
        check("la frappe s'annule avant la réouverture",
                editor.getUndoStack().canUndo());
        click("50k l.");
        frame();
        check("la réouverture d'un texte édité repart du document d'origine",
                editor.getSyntaxDocument().lexedLineCount() < 52004);
        check("le remplissage repart (plein chemin) et achève",
                editor.awaitBackgroundFill(120_000L)
                        && editor.getSyntaxDocument().lexedLineCount() == 52004);
        check("le chargement n'est pas annulable (l'histoire repart vide)",
                !editor.getUndoStack().canUndo());
    }

    /**
     * Le geste de copie complet sur le hors normes : le parcours du
     * relevé — réouvertures alternées, thème sur le document plein,
     * un caractère tapé au milieu, double-tap, traînement de poignée
     * (la loupe vit dedans), trame par trame. Chaque trame du
     * traînement se chronomètre : la reproduction cherche la trame
     * qui dépasse le budget.
     */
    private static void hugeFileCopyGesture() {
        begin("hors normes : le geste de copie complet, trame par trame");
        long worstOpen = 0L;
        openLanguage("50k l.", 120_000L);
        worstOpen = Math.max(worstOpen, editor.lastFrameNanos());
        for (int round = 0; round < 2; round++) {
            openLanguage("Gros", 30_000L);
            worstOpen = Math.max(worstOpen, editor.lastFrameNanos());
            openLanguage("50k l.", 120_000L);
            worstOpen = Math.max(worstOpen, editor.lastFrameNanos());
        }
        long fullTokens = editor.getSyntaxDocument().tokenCount();
        click("Sombre");
        frame();
        long worstTheme = editor.lastFrameNanos();
        click("Clair");
        frame();
        worstTheme = Math.max(worstTheme, editor.lastFrameNanos());
        check("le thème sur le document plein ne retokenise pas (JVM)",
                editor.getSyntaxDocument().tokenCount() == fullTokens);
        // un caractère tapé au milieu du document (la frappe du relevé)
        TextBuffer buffer = editor.getBuffer();
        editor.caretTo(buffer.length() / 2);
        frame();
        connection.commitText("z", 1);
        frame();
        long strikeFrame = editor.lastFrameNanos();
        check("la frappe relit une ligne sur 52 004",
                editor.getSyntaxDocument().lastRelexedLines() == 1);
        // le double-tap sème la sélection sur le mot visible
        doubleTapOfWord("graine");
        frame();
        long worstTap = editor.lastFrameNanos();
        // le traînement de poignée : trente déplacements, une trame chacun
        float[] spot = spotOfWord("graine");
        float lineMidY = spot[1] + LINE_HEIGHT / 2.0f;
        int column = (editor.selectionEnd()
                - buffer.getLineStart(buffer.lineOfOffset(editor.selectionEnd())));
        float handleX = editor.getPaddingLeft() + column * editor.charWidth();
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_DOWN, handleX, spot[1] + 4.0f));
        long worstDrag = 0L;
        long totalDrag = 0L;
        int steps = 30;
        for (int step = 1; step <= steps; step++) {
            float x = editor.getPaddingLeft() + (column + step) * editor.charWidth();
            editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_MOVE, x, lineMidY));
            frame();
            long nanos = editor.lastFrameNanos();
            worstDrag = Math.max(worstDrag, nanos);
            totalDrag += nanos;
        }
        // l'horloge murale d'une machine chargée peut payer une pause du
        // collecteur dans N'IMPORTE QUELLE trame : la vérification porte
        // la moyenne (robuste aux pointes) et le moteur (déterministe) —
        // la pire s'y lit, elle ne s'y asserte pas
        check("la moyenne des trames de traînement tient et le moteur n'y paie jamais (JVM)",
                totalDrag / steps < 16_000_000L
                        && editor.worstFrameEngineNanos() < 16_000_000L);
        // la vérification de l'attribution : un ramasse-miettes forcé AU
        // MILIEU d'une trame de traînement (le geste vit encore). La
        // remise à zéro juste avant lui garantit que la trame forcée
        // DEVIENT la pire (un collecteur complet sur ce tas est long) ;
        // le delta qu'elle enregistre se lit — positif quand le
        // collecteur y rend (le cas que l'appareil nommera), négatif
        // quand un collecteur naturel est passé avant lui : la
        // sémantique se vérifie sur l'appareil, le raccordement (la
        // pire porte son attribution) au harnais
        byte[][] junk = new byte[32][];
        for (int at = 0; at < junk.length; at++) {
            junk[at] = new byte[1_048_576];
        }
        junk = null;
        editor.resetFrameStats();
        canvas.harnessDrawHook(System::gc);
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_MOVE,
                editor.getPaddingLeft() + (column + steps) * editor.charWidth(), lineMidY));
        frame();
        check("l'odomètre porte l'attribution de la pire trame",
                editor.worstFrameNanos() > 10_000_000L
                        && editor.worstFrameMemoryDelta() != 0L);
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_UP, editor.getPaddingLeft()
                        + (column + steps) * editor.charWidth(), lineMidY));
        frame();
        detail("trame la pire : ouverture " + worstOpen / 1_000_000.0
                + " ms, thème " + worstTheme / 1_000_000.0
                + " ms, frappe " + strikeFrame / 1_000_000.0
                + " ms, double-tap " + worstTap / 1_000_000.0
                + " ms, traînement (pire de " + steps + ") " + worstDrag / 1_000_000.0
                + " ms, moyenne " + totalDrag / 1_000_000.0 / steps + " ms"
                + " ; à la pire : moteur " + editor.worstFrameEngineNanos() / 1_000_000.0
                + " ms, Δ mémoire " + editor.worstFrameMemoryDelta() / 1_048_576.0 + " Mo");
    }

    /** Le « tout remplacer » du relevé 16 : dix mille correspondances
     * sur le hors normes gelaient l'application (ANR) — la table de
     * lignes du tampon se copiait sur elle-même à chaque
     * remplacement, et le panneau se rafraîchissait autant de fois. */
    private static void replaceAllHugeFile() {
        begin("hors normes : tout remplacer sans gel (l'ANR du relevé 16)");
        openLanguage("50k l.", 120_000L);
        String before = editor.getBuffer().toString();
        int total = countOccurrences(before, "graine");
        check("la démonstration hors normes porte plus de dix mille graines",
                total > 10_000);
        click("Rechercher");
        SearchBar bar = editor.searchBar();
        // la frappe passe par le clavier de l'éditeur (la carte ne
        // prend jamais le focus)
        connection.commitText("graine", 1);
        View.harnessDrain(400L);
        int matches = editor.searchMatchCount();
        check("la recherche plafonne à dix mille correspondances",
                matches == 10_000);
        bar.showReplaceRow();
        connection.commitText("GRAINE", 1);
        long startedAt = System.nanoTime();
        bar.replaceEverywhere();
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
        check("tout remplacer remplace exactement les correspondances portées",
                countOccurrences(editor.getBuffer().toString(), "graine") == total - matches
                        && countOccurrences(editor.getBuffer().toString(), "GRAINE") == matches);
        check("tout remplacer tient la demi-seconde (l'ANR n'a plus de quoi naître)",
                elapsedMs < 500L);
        detail("tout remplacer : " + matches + " remplacements en " + elapsedMs + " ms");
        long undoStartedAt = System.nanoTime();
        click("Annuler");
        long undoMs = (System.nanoTime() - undoStartedAt) / 1_000_000L;
        check("l'annulation rend le texte intact",
                editor.getBuffer().toString().equals(before));
        check("l'annulation tient aussi la demi-seconde",
                undoMs < 500L);
        detail("annulation : " + undoMs + " ms");
        click("Rechercher");
    }

    /**
     * La consolidation : le hors normes aux autres langages — le
     * bouton qui cycle y ouvre chacun, la fenêtre, le fond et le mode
     * gros fichier y tiennent, la frappe n'y relit qu'une ligne.
     */
    private static void hugeFileLanguages() {
        begin("consolidation : le hors normes aux autres langages");
        String[] expected = {"XML", "JSON", "KOTLIN", "SQL"};
        String[] openers = {"<!--", "[", "//", "--"};
        for (int round = 0; round < expected.length; round++) {
            click("50k ⨯");
            frame();
            View.harnessDrain(2_000L);
            editor.awaitBackgroundFill(120_000L);
            View.harnessDrain(2_000L);
            frame();
            SyntaxDocument syntax = editor.getSyntaxDocument();
            check("le hors normes " + expected[round] + " porte 52 000 lignes et plus",
                    syntax.lineCount() >= 52_000 && syntax.lexedLineCount() == syntax.lineCount());
            check("le mode gros fichier y est actif", syntax.inBigFileMode());
            check("le texte est bien du " + expected[round],
                    editor.getBuffer().getText(0, Math.min(4, editor.getBuffer().length()))
                            .startsWith(openers[round]));
            detail(expected[round] + " : " + syntax.lineCount() + " lignes, "
                    + syntax.tokenCount() + " jetons, fond "
                    + editor.backgroundFillNanos() / 1_000_000L + " ms");
        }
        // la frappe au milieu du dernier : la paresse de ligne tient
        // sur chaque langage
        TextBuffer buffer = editor.getBuffer();
        editor.caretTo(buffer.length() / 2);
        frame();
        connection.commitText("z", 1);
        frame();
        check("la frappe sur le hors normes SQL relit une ligne",
                editor.getSyntaxDocument().lastRelexedLines() == 1);
        click("Annuler");
    }

    /** Les plis : la gouttière retourne le pli, la ligne se cache. */
    private static void folds() {
        begin("plis : la gouttière cache puis rend");
        openLanguage("Java", 10_000L);
        int foldLine = -1;
        for (int line = 0; line < editor.getBuffer().getLineCount(); line++) {
            if (editor.getStructureDocument().foldStartsAt(line)) {
                foldLine = line;
                break;
            }
        }
        check("un pli existe dans la démo", foldLine >= 0);
        if (foldLine < 0) {
            return;
        }
        float y = lineTop(foldLine) + LINE_HEIGHT / 2.0f;
        tap(4.0f, y);
        check("une ligne se cache", editor.hiddenLineCount() > 0);
        tap(4.0f, y);
        check("le pli se rend", editor.hiddenLineCount() == 0);
    }

    /** La fusion sémantique : des annotations et des symboles existent. */
    private static void semanticFusion() {
        begin("fusion sémantique : annotations, symboles, mots");
        frame();
        check("des annotations vivent dans la fenêtre", editor.semanticVisibleCount() > 0);
        check("des symboles sont indexés", editor.symbolCount() > 0);
        check("des mots sont indexés", editor.wordCount() > 0);
        detail(editor.semanticVisibleCount() + " annotation(s), "
                + editor.symbolCount() + " symbole(s)");
    }

    /**
     * La carte de complétion : les candidats du mot courant en rangées
     * au caret — la flèche déléguée y navigue (bornée aux murs de la
     * liste), la validation y insère, le doigt sur une rangée aussi,
     * la carte se range quand le préfixe meurt.
     */
    private static void completionCard() {
        begin("complétion : la carte au caret, flèche, validation, doigt");
        // le document court d'abord : un candidat unique — la flèche y
        // reste bornée, la validation insère
        openLanguage("Java", 10_000L);
        String original = editor.getBuffer().toString();
        editor.caretTo(editor.getBuffer().length());
        frame();
        connection.commitText("ta", 1);
        frame();
        CompletionCard card = editor.completionCard();
        check("la carte vit au préfixe tapé (deux caractères)", card.isShown());
        check("le préfixe se porte", card.prefix().equals("ta"));
        check("les candidats poursuivent le préfixe",
                !card.words().isEmpty()
                        && card.words().get(0).startsWith("ta"));
        check("le premier candidat est le courant", card.highlighted() == 0);
        int caretLine = editor.getBuffer().getLineCount() - 1;
        check("la carte se pose au caret — dessous ou dessus, dans les murs",
                (card.poseY() >= lineTop(caretLine) + LINE_HEIGHT
                        || card.poseY() + card.height() <= lineTop(caretLine))
                        && card.poseX() >= 0
                        && card.poseX() + card.width() <= editor.getWidth());
        // la flèche déléguée sur un candidat unique reste bornée
        pressKey(KeyEvent.KEYCODE_DPAD_DOWN);
        frame();
        check("la flèche sur un candidat unique reste au courant",
                card.highlighted() == 0);
        // la validation déléguée insère : le préfixe se remplace par
        // le candidat entier
        String candidate = card.words().get(card.highlighted());
        pressKey(KeyEvent.KEYCODE_ENTER);
        frame();
        check("la validation insère le candidat courant (le préfixe remplacé)",
                editor.getBuffer().toString().endsWith(candidate));
        click("Annuler");
        click("Annuler");
        check("la double annulation rend le texte",
                editor.getBuffer().toString().equals(original));
        // le gros fichier : plusieurs candidats — la flèche y navigue,
        // le doigt sur une rangée insère aussi
        openLanguage("Gros", 30_000L);
        // le veilleur du panneau et les tranches d'analyse y courent
        // (l'index du gros fichier se bâtit par tranches postées — le
        // drainage les laisse finir)
        View.harnessDrain(2_000L);
        frame();
        View.harnessDrain(2_000L);
        frame();
        String grosOriginal = editor.getBuffer().toString();
        editor.caretTo(editor.getBuffer().length());
        frame();
        connection.commitText("bl", 1);
        frame();
        check("la carte porte plusieurs candidats", card.words().size() >= 2);
        check("les candidats du gros fichier poursuivent le préfixe",
                card.words().stream()
                        .allMatch(word -> word.startsWith("bl")));
        pressKey(KeyEvent.KEYCODE_DPAD_DOWN);
        pressKey(KeyEvent.KEYCODE_DPAD_DOWN);
        frame();
        check("deux flèches baissent descendent au troisième candidat",
                card.highlighted() == 2);
        pressKey(KeyEvent.KEYCODE_DPAD_UP);
        frame();
        check("la flèche haut remonte", card.highlighted() == 1);
        String second = card.words().get(1);
        tap(card.poseX() + card.width() / 2.0f, card.rowCenterY(1));
        frame();
        check("le doigt sur la rangée l'insère",
                editor.getBuffer().toString().endsWith(second));
        click("Annuler");
        click("Annuler");
        check("la double annulation rend le gros texte",
                editor.getBuffer().toString().equals(grosOriginal));
        // le préfixe meurt : la carte se range
        editor.caretTo(editor.getBuffer().length());
        frame();
        connection.commitText("b", 1);
        frame();
        check("un caractère seul ne montre rien", !card.isShown());
        connection.commitText("l", 1);
        frame();
        check("le deuxième caractère montre la carte", card.isShown());
        pressKey(KeyEvent.KEYCODE_DEL);
        pressKey(KeyEvent.KEYCODE_DEL);
        frame();
        check("le préfixe mort range la carte", !card.isShown());
        // les trois unités (le « b », le « l », les deux suppressions
        // n'en font qu'une ici) se défont — le scénario suivant part
        // d'un texte intact
        for (int undo = 0; undo < 4
                && !editor.getBuffer().toString().equals(grosOriginal); undo++) {
            click("Annuler");
        }
        check("le texte du gros fichier revient intact",
                editor.getBuffer().toString().equals(grosOriginal));
        detail(card.words().size() + " candidats au plus, pose [" + card.poseX() + ", "
                + card.poseY() + "] " + card.width() + "×" + card.height());
    }

    /**
     * La carte de signature : l'aide aux paramètres au curseur — la
     * signature de la déclaration s'y montre, les virgules y
     * déplacent le paramètre actif, la fermeture de l'appel la
     * range, la frappe d'un appel l'y fait naître.
     */
    private static void signatureCard() {
        begin("signature : l'aide aux paramètres au curseur");
        openLanguage("Java", 10_000L);
        String original = editor.getBuffer().toString();
        SignatureCard card = editor.signatureCard();
        // le curseur DANS les paramètres de la déclaration : la
        // signature s'y montre
        int args = original.indexOf("int graine");
        editor.caretTo(args + 3);
        frame();
        check("la carte vit dans les paramètres de l'appel", card.isShown());
        check("la signature porte le nom et les deux paramètres",
                card.label().contains("total") && card.label().contains("graine")
                        && card.label().contains("taille"));
        check("le premier paramètre est actif", card.activeParameter() == 0);
        int caretLine = editor.getBuffer().lineOfOffset(args + 3);
        check("la carte se pose au-dessus de la ligne du caret, dans les murs",
                card.poseY() + card.height() <= lineTop(caretLine) + LINE_HEIGHT
                        && card.poseX() >= 0
                        && card.poseX() + card.width() <= editor.getWidth());
        // la virgule déplace le paramètre actif
        int comma = original.indexOf(", int taille");
        editor.caretTo(comma + 1);
        frame();
        check("la virgule déplace le paramètre actif", card.activeParameter() == 1);
        // le curseur avant la fermante : le deuxième reste actif
        int closeParen = original.indexOf("taille)");
        editor.caretTo(closeParen + "taille".length());
        frame();
        check("le curseur avant la fermante garde le paramètre",
                card.activeParameter() == 1);
        // après la fermante : la carte se range
        editor.caretTo(closeParen + "taille".length() + 1);
        frame();
        check("la fermeture de l'appel range la carte", !card.isShown());
        // la frappe d'un appel NEUF : la carte y naît, les virgules y
        // déplacent l'actif, la fermante la range
        editor.caretTo(editor.getBuffer().length());
        frame();
        connection.commitText("total(", 1);
        frame();
        check("la carte naît de l'appel frappé", card.isShown()
                && card.activeParameter() == 0);
        connection.commitText("5,", 1);
        frame();
        check("la virgule frappée déplace l'actif", card.activeParameter() == 1);
        connection.commitText(" 2)", 1);
        frame();
        check("la fermante frappée range la carte", !card.isShown());
        check("l'appel frappé se lit dans le texte",
                editor.getBuffer().toString().endsWith("total(5, 2)"));
        click("Annuler");
        click("Annuler");
        click("Annuler");
        check("la triple annulation rend le texte",
                editor.getBuffer().toString().equals(original));
        detail("signature : " + card.label());
    }

    /**
     * La documentation rapide : le titre porte le mot questionné, le
     * corps se replie à la largeur — la description de l'index local
     * (la nature, la ligne de déclaration) y vit, l'appui suivant la
     * range.
     */
    private static void quickDoc() {
        begin("survol : la documentation rapide au mot sous le doigt");
        openLanguage("Java", 10_000L);
        doubleTapOfToken("total");
        frame();
        // la documentation s'ouvre par le « i » de la barre de
        // sélection (le double-tap ne fait que sélectionner)
        tap(editor.selectionBar().docsCenterX(), editor.selectionBar().centerY());
        frame();
        check("le titre porte le mot questionné",
                "total".equals(editor.infoPopupTitle()));
        String body = editor.infoPopupText();
        check("le corps décrit la déclaration (nature, ligne)",
                body != null && body.contains("méthode")
                        && body.contains("déclaré ligne"));
        // un mot ordinaire : le corps dit lui-même
        doubleTapOfWord("graine");
        frame();
        tap(editor.selectionBar().docsCenterX(), editor.selectionBar().centerY());
        frame();
        check("un mot ordinaire garde son titre et se dit lui-même",
                "graine".equals(editor.infoPopupTitle())
                        && "mot du texte".contentEquals(editor.infoPopupText()));
        // l'appui suivant masque la documentation
        tap(400.0f, 80.0f);
        frame();
        check("l'appui suivant masque la documentation",
                !editor.isInfoPopupVisible());
        detail("corps : " + body);
    }

    /**
     * La barre de sélection : les pastilles (Copier, Coller au
     * presse-papiers près, Tout copier), l'icône « i » de la
     * documentation, la puce « ⋯ » de débordement et ses actions
     * repliées.
     */
    private static void selectionBar() {
        begin("barre de sélection : pastilles, documentation, débordement");
        openLanguage("Java", 10_000L);
        // presse-papiers vide : la pastille inutile ne se promène pas
        android.content.Context.harnessClipboard().setPrimaryClip(null);
        doubleTapOfWord("graine");
        frame();
        check("la barre naît de la sélection",
                editor.selectionBar().isShown());
        check("sans presse-papiers, la pastille Coller ne vit pas",
                !editor.selectionBar().hasChip("Coller"));
        check("les pastilles Copier et Tout copier vivent",
                editor.selectionBar().hasChip("Copier")
                        && editor.selectionBar().hasChip("Tout copier"));
        int selLine = editor.getBuffer().lineOfOffset(editor.selectionStart());
        check("la barre flotte au-dessus de la ligne sélectionnée, dans les murs",
                editor.selectionBar().poseY() + editor.selectionBar().height()
                        <= lineTop(selLine) + LINE_HEIGHT
                        && editor.selectionBar().poseX() >= 0
                        && editor.selectionBar().poseX()
                                + editor.selectionBar().width() <= editor.getWidth());
        // Copier : la sélection au presse-papiers, la pastille Coller en naît
        tap(editor.selectionBar().chipCenterX("Copier"),
                editor.selectionBar().centerY());
        frame();
        CharSequence clip = android.content.Context.harnessClipboard()
                .getPrimaryClip().getItemAt(0).getText();
        check("Copier porte la sélection au presse-papiers",
                clip != null && "graine".contentEquals(clip));
        check("la pastille Coller naît du texte copié",
                editor.selectionBar().hasChip("Coller"));
        // le « i » : la documentation rapide du mot sélectionné
        tap(editor.selectionBar().docsCenterX(), editor.selectionBar().centerY());
        frame();
        check("le « i » ouvre la documentation du mot sélectionné",
                "graine".equals(editor.infoPopupTitle()));
        // la puce « ⋯ » : les étages d'actions repliées — la navigation,
        // l'édition, le presse-papiers, l'historique (la sélection vit)
        tap(editor.selectionBar().overflowCenterX(), editor.selectionBar().centerY());
        frame();
        java.util.List<String> repliees = editor.overflowCard().labels();
        java.util.List<String> etages = editor.overflowCard().sectionTitles();
        check("le débordement porte ses étages titrés",
                etages.size() == 4 && "Navigation".equals(etages.get(0))
                        && "Édition".equals(etages.get(1))
                        && "Presse-papiers".equals(etages.get(2))
                        && "Historique".equals(etages.get(3)));
        check("la navigation y vit (aller à la ligne, rechercher)",
                repliees.contains("Aller à la ligne…")
                        && repliees.contains("Rechercher…"));
        check("l'édition y vit (commenter, casse, dupliquer, supprimer)",
                repliees.contains("Commenter / décommenter")
                        && repliees.contains("En majuscules")
                        && repliees.contains("Dupliquer la ligne")
                        && repliees.contains("Supprimer la ligne"));
        check("le presse-papiers y vit (couper, tout sélectionner, quitter)",
                repliees.contains("Couper")
                        && repliees.contains("Tout sélectionner")
                        && repliees.contains("Quitter la sélection"));
        // la puce referme la carte (le menu se rabat)
        tap(editor.selectionBar().overflowCenterX(), editor.selectionBar().centerY());
        frame();
        check("la puce referme la carte de débordement",
                !editor.overflowCard().isShown());
        // Couper : la carte se referme avant d'exécuter, la sélection
        // copiée puis effacée, la barre se range avec elle
        tap(editor.selectionBar().overflowCenterX(), editor.selectionBar().centerY());
        frame();
        int avant = countOccurrences(editor.getBuffer().toString(), "graine");
        int cutAt = repliees.indexOf("Couper");
        tap(editor.overflowCard().poseX() + 10.0f,
                editor.overflowCard().rowCenterY(cutAt));
        frame();
        CharSequence coupe = android.content.Context.harnessClipboard()
                .getPrimaryClip().getItemAt(0).getText();
        check("Couper retire l'occurrence sélectionnée",
                countOccurrences(editor.getBuffer().toString(), "graine") == avant - 1);
        check("Couper copie la sélection au presse-papiers",
                coupe != null && "graine".contentEquals(coupe));
        check("la carte refermée, la barre se range avec la sélection morte",
                !editor.overflowCard().isShown() && !editor.selectionBar().isShown());
        click("Annuler");
        check("l'annulation rend le mot coupé",
                countOccurrences(editor.getBuffer().toString(), "graine") == avant);
        // Coller : le texte du presse-papiers remplace la sélection
        android.content.Context.harnessClipboard().setPrimaryClip(
                android.content.ClipData.newPlainText("essai", "grelin"));
        doubleTapOfWord("graine");
        frame();
        check("la pastille Coller vit du texte du presse-papiers",
                editor.selectionBar().hasChip("Coller"));
        tap(editor.selectionBar().chipCenterX("Coller"),
                editor.selectionBar().centerY());
        frame();
        check("Coller remplace la sélection par le texte du presse-papiers",
                countOccurrences(editor.getBuffer().toString(), "grelin") == 1
                        && countOccurrences(editor.getBuffer().toString(),
                                "graine") == avant - 1);
        click("Annuler");
        check("l'annulation rend le texte initial",
                countOccurrences(editor.getBuffer().toString(), "grelin") == 0
                        && countOccurrences(editor.getBuffer().toString(),
                                "graine") == avant);
        // Tout sélectionner, depuis le débordement
        doubleTapOfWord("graine");
        frame();
        tap(editor.selectionBar().overflowCenterX(), editor.selectionBar().centerY());
        frame();
        int toutAt = repliees.indexOf("Tout sélectionner");
        tap(editor.overflowCard().poseX() + 10.0f,
                editor.overflowCard().rowCenterY(toutAt));
        frame();
        check("Tout sélectionner prend le document entier",
                editor.hasSelection()
                        && editor.selectedText().length()
                                == editor.getBuffer().length());
        // Quitter la sélection, depuis le débordement
        tap(editor.selectionBar().overflowCenterX(), editor.selectionBar().centerY());
        frame();
        int quitterAt = repliees.indexOf("Quitter la sélection");
        tap(editor.overflowCard().poseX() + 10.0f,
                editor.overflowCard().rowCenterY(quitterAt));
        frame();
        check("Quitter la sélection rend le curseur seul",
                !editor.hasSelection() && !editor.selectionBar().isShown());
        // Tout copier : le document entier au presse-papiers
        android.content.Context.harnessClipboard().setPrimaryClip(null);
        doubleTapOfWord("graine");
        frame();
        tap(editor.selectionBar().chipCenterX("Tout copier"),
                editor.selectionBar().centerY());
        frame();
        CharSequence tout = android.content.Context.harnessClipboard()
                .getPrimaryClip().getItemAt(0).getText();
        check("Tout copier porte le document entier au presse-papiers",
                tout != null && tout.length() == editor.getBuffer().length());
        check("la pastille Coller vit après tout copier",
                editor.selectionBar().hasChip("Coller"));
        // la frappe remplace la sélection : la barre ne prend jamais le focus
        connection.commitText("P", 1);
        frame();
        check("la frappe remplace la sélection, la barre se range",
                countOccurrences(editor.getBuffer().toString(), "graine") == avant - 1
                        && !editor.selectionBar().isShown());
        click("Annuler");
        // le doigt hors de la barre poursuit sa vie : le curseur y va,
        // la sélection meurt, la barre se range
        doubleTapOfWord("graine");
        frame();
        tap(400.0f, 700.0f);
        frame();
        check("le doigt hors de la barre va au curseur, la barre se range",
                !editor.hasSelection() && !editor.selectionBar().isShown());
        click("Annuler");
        check("le texte demeure intact à la fin",
                countOccurrences(editor.getBuffer().toString(), "graine") == avant);
    }

    /**
     * Les popups de seconde génération : le clavier se lève au champ
     * visé, la complétion porte ses colonnes (icône de nature, détail
     * de droite, pied de carte), les étages du débordement AGISSENT
     * (casse, commentaire, duplication, suppression de ligne),
     * l'hôte pose ses pastilles sur la barre, la loupe se range au
     * lâcher (la barre y naît dans la même trame) et l'historique
     * local d'une partie du code se lit et se restaure.
     */
    private static void popupsSecondGeneration() throws InterruptedException {
        begin("popups de seconde génération : clavier, colonnes, étages, hôte, historique");
        openLanguage("Java", 10_000L);
        String original = editor.getBuffer().toString();
        android.view.inputmethod.InputMethodManager ime =
                (android.view.inputmethod.InputMethodManager) activity.getSystemService(
                        android.content.Context.INPUT_METHOD_SERVICE);
        // LE CLAVIER SE LÈVE À L'OUVERTURE : la carte naît clavier levé —
        // le doigt qui cherche le champ n'a pas à lever autre chose
        int before = ime.harnessShowCount();
        click("Rechercher");
        SearchBar bar = editor.searchBar();
        check("la carte de recherche naît clavier levé",
                ime.harnessShowCount() > before);
        // le doigt sur le champ de requête : le clavier se montre encore
        int shows = ime.harnessShowCount();
        tap(bar.poseX() + bar.width() * 0.4f, bar.poseY() + bar.height() * 0.25f);
        check("le champ visé lève le clavier (la carte ne prend jamais le focus)",
                ime.harnessShowCount() > shows);
        // la frappe route vers la carte : la recherche compte
        connection.commitText("gr", 1);
        View.harnessDrain(400L);
        check("la frappe route vers la carte (la recherche compte)",
                editor.searchMatchCount() > 0);
        bar.close();
        frame();
        // l'aller-à suit la même règle
        before = ime.harnessShowCount();
        click("Aller à");
        check("la carte d'aller-à naît clavier levé",
                ime.harnessShowCount() > before);
        editor.gotoBar().close();
        frame();
        // LA COMPLÉTION EN COLONNES : les candidats portent la nature
        // (l'icône) et le détail de droite (la ligne de déclaration)
        editor.caretTo(editor.getBuffer().length());
        frame();
        connection.commitText("to", 1);
        frame();
        CompletionCard card = editor.completionCard();
        check("la carte de complétion vit au préfixe", card.isShown());
        boolean detailed = false;
        for (CompletionCard.Entry entry : card.entries()) {
            if (!entry.detail().isEmpty()
                    && entry.kind() != CompletionCard.KIND_UNKNOWN
                    && entry.kind() != CompletionCard.KIND_WORD) {
                detailed = true;
            }
        }
        check("les candidats portent leurs colonnes (nature en icône, détail à droite)",
                detailed);
        check("la carte grand format dépasse le plancher de largeur",
                card.width() >= 240.0f * DENSITY * 0.99f);
        // le préfixe s'efface : le texte revient (une seule unité à défaire)
        click("Annuler");
        check("le texte revient après la complétion éprouvée",
                editor.getBuffer().toString().equals(original));
        // LES ÉTAGES AGISSENT : la casse de la sélection
        doubleTapOfWord("graine");
        frame();
        tap(editor.selectionBar().overflowCenterX(), editor.selectionBar().centerY());
        frame();
        java.util.List<String> repliees = editor.overflowCard().labels();
        int majAt = repliees.indexOf("En majuscules");
        tap(editor.overflowCard().poseX() + 10.0f,
                editor.overflowCard().rowCenterY(majAt));
        frame();
        check("la sélection passe en majuscules",
                editor.getBuffer().toString().contains("GRAINE"));
        click("Annuler");
        check("l'annulation rend la casse d'avant",
                editor.getBuffer().toString().equals(original));
        // le commentaire de ligne se pose et se retire
        doubleTapOfWord("SEUIL");
        frame();
        tap(editor.selectionBar().overflowCenterX(), editor.selectionBar().centerY());
        frame();
        int commentAt = repliees.indexOf("Commenter / décommenter");
        tap(editor.overflowCard().poseX() + 10.0f,
                editor.overflowCard().rowCenterY(commentAt));
        frame();
        check("la ligne se commente au préfixe du langage",
                editor.getBuffer().toString().contains("// private static final double SEUIL"));
        click("Annuler");
        // la duplication et la suppression de ligne se tiennent la main
        int lineCount = editor.getBuffer().getLineCount();
        doubleTapOfWord("graine");
        frame();
        tap(editor.selectionBar().overflowCenterX(), editor.selectionBar().centerY());
        frame();
        int dupAt = repliees.indexOf("Dupliquer la ligne");
        tap(editor.overflowCard().poseX() + 10.0f,
                editor.overflowCard().rowCenterY(dupAt));
        frame();
        check("la ligne se duplique", editor.getBuffer().getLineCount() == lineCount + 1);
        // la duplication range la sélection (le curseur descend sur la
        // copie) : le mot se reprend, la suppression ôte la ligne
        doubleTapOfWord("graine");
        frame();
        tap(editor.selectionBar().overflowCenterX(), editor.selectionBar().centerY());
        frame();
        int delAt = repliees.indexOf("Supprimer la ligne");
        tap(editor.overflowCard().poseX() + 10.0f,
                editor.overflowCard().rowCenterY(delAt));
        frame();
        check("la ligne dupliquée se supprime",
                editor.getBuffer().getLineCount() == lineCount);
        // la suppression et la duplication se défont : le texte revient
        click("Annuler");
        click("Annuler");
        check("le texte revient intact", editor.getBuffer().toString().equals(original));
        // L'HÔTE POSE SES PASTILLES : la barre de sélection s'ouvre à
        // l'application porteuse (le presse-papiers se vide : la
        // pastille Coller ne prend pas la place de celle de l'hôte)
        android.content.Context.harnessClipboard().setPrimaryClip(null);
        boolean[] ran = {false};
        editor.addSelectionAction("Test", () -> ran[0] = true);
        doubleTapOfWord("graine");
        frame();
        check("la pastille de l'hôte vit sur la barre",
                editor.selectionBar().hasChip("Test"));
        tap(editor.selectionBar().chipCenterX("Test"),
                editor.selectionBar().centerY());
        frame();
        check("la pastille de l'hôte agit au toucher", ran[0]);
        editor.clearSelectionActions();
        doubleTapOfWord("graine");
        frame();
        check("les pastilles de l'hôte se retirent",
                !editor.selectionBar().hasChip("Test"));
        // LA LOUPE SE RANGE AU LÂCHER : la barre de sélection naît dans
        // la même trame (l'appui long qui glisse étend la sélection)
        tap(400.0f, 80.0f);
        frame();
        check("la sélection se range avant l'appui long", !editor.hasSelection());
        float[] spot = spotOfWord("SEUIL");
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN, spot[0], spot[1]));
        Thread.sleep(600L);
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_MOVE,
                spot[0] + 40.0f, spot[1]));
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP,
                spot[0] + 40.0f, spot[1]));
        frame();
        check("la loupe se range au lâcher, la barre de sélection y naît",
                editor.selectionBar().isShown());
        // L'HISTORIQUE LOCAL D'UNE PARTIE DU CODE : les traces de la
        // ligne du SEUIL se lisent du dernier au premier, la
        // restauration remet la ligne du passé
        editor.caretTo(editor.getBuffer().toString().indexOf("SEUIL") + 5);
        frame();
        connection.commitText("2", 1);
        frame();
        check("une trace naît de la frappe",
                editor.localHistory().size() >= 1);
        // la ligne porte SEUIL2 : la sélection de la ligne ouvre
        // l'historique de cette partie du code
        doubleTapOfWord("SEUIL2");
        frame();
        tap(editor.selectionBar().overflowCenterX(), editor.selectionBar().centerY());
        frame();
        int histAt = repliees.indexOf("Historique de la sélection");
        check("l'étage historique vit de la sélection", histAt >= 0);
        tap(editor.overflowCard().poseX() + 10.0f,
                editor.overflowCard().rowCenterY(histAt));
        frame();
        HistoryCard history = editor.historyCard();
        check("la carte de l'historique local s'ouvre sur la plage",
                history.isShown());
        check("les traces de la plage se lisent", history.traceCount() >= 2);
        // la trace d'AVANT (la ligne sans le « 2 ») se déplie au doigt,
        // sa pastille restaure la ligne du passé
        tap(history.poseX() + history.width() * 0.5f, history.traceCenterY(1));
        frame();
        check("la trace se déplie au doigt", history.expandedTrace() == 1);
        tap(history.poseX() + 60.0f, history.restoreChipY(1));
        frame();
        check("la restauration remet la ligne du passé",
                editor.getBuffer().toString().contains("SEUIL")
                        && !editor.getBuffer().toString().contains("SEUIL2"));
        check("la carte se range après la restauration", !history.isShown());
        click("Annuler");
        click("Annuler");
        check("la double annulation rend la ligne d'avant",
                editor.getBuffer().toString().equals(original));
        detail("historique local : " + editor.localHistory().size() + " trace(s)");
    }

    /** Le serveur de langage : diagnostics, survol, débranchement. */
    private static void languageServer() throws Exception {
        begin("serveur de langage : diagnostics, survol, débranchement");
        click("LSP");
        check("les diagnostics du serveur arrivent", awaitText("LSP :", 8000L));
        String original = editor.getBuffer().toString();
        connection.commitText("(", 1);
        View.harnessDrain(2000L);
        check("une parenthèse non appariée se diagnostique",
                awaitText("LSP : 1", 8000L));
        // le second delta s'applique sur l'ombre du premier : la paire
        // se ferme, le compte revient à zéro
        connection.commitText(")", 1);
        View.harnessDrain(2000L);
        check("la parenthèse fermée ramène le compte à zéro (deux deltas)",
                awaitText("LSP : 0", 8000L));
        doubleTapOfWord("static");
        frame();
        // le survol du serveur se demande par le « i » de la barre de
        // sélection (le double-tap ne fait que sélectionner)
        tap(editor.selectionBar().docsCenterX(), editor.selectionBar().centerY());
        View.harnessDrain(2000L);
        String hover = editor.infoPopupText();
        check("le survol du serveur prime dans la popup",
                hover != null && hover.contains("classe"));
        check("le titre porte le mot questionné",
                "static".equals(editor.infoPopupTitle()));
        detail("survol : " + hover);
        // le survol asynchrone : une réponse tardive remplace la popup
        // À SA POSITION — une position périmée ne l'évince pas (la
        // popup a bougé, ou s'est rangée)
        int popupLine = editor.infoPopupLine();
        int popupColumn = editor.infoPopupColumn();
        check("le remplacement hors position s'ignore",
                !editor.replaceInfoPopup("périmé", popupLine + 1, popupColumn));
        check("la réponse tardive remplace la popup à sa position",
                editor.replaceInfoPopup("réponse tardive du serveur",
                        popupLine, popupColumn));
        check("la popup porte la réponse tardive",
                "réponse tardive du serveur".equals(editor.infoPopupText()));
        // la complétion du serveur au bandeau : « gr » y porte les
        // mots du document — l'index local n'y indexe pas « graine »
        // (paramètre, pas déclaration), le serveur si : la réponse
        // revient asynchrone sur le fil du lecteur (temps réel —
        // l'attente y est réelle, comme les diagnostics)
        editor.caretTo(0);
        frame();
        connection.commitText("gr", 1);
        frame();
        for (int wait = 0; wait < 100
                && !editor.suggestions().contains("graine"); wait++) {
            Thread.sleep(20L);
            View.harnessDrain(500L);
            frame();
        }
        java.util.List<String> completions = editor.suggestions();
        check("la complétion du serveur arrive à la carte",
                completions.contains("graine"));
        detail("complétions : " + completions);
        // l'aide aux paramètres du serveur : « total( » frappé, la
        // carte porte la signature du serveur dès l'arrivée — le
        // « : double » est sa façon d'écrire, l'index local n'écrit
        // jamais ce style (la trame suivante porte la réponse)
        editor.caretTo(editor.getBuffer().length());
        frame();
        connection.commitText("total(", 1);
        frame();
        for (int wait = 0; wait < 100 && !(editor.signatureCard().isShown()
                && editor.signatureCard().label().contains(" : double")); wait++) {
            Thread.sleep(20L);
            View.harnessDrain(500L);
            frame();
        }
        check("la signature du serveur arrive à la carte",
                editor.signatureCard().isShown()
                        && editor.signatureCard().label().contains(" : double"));
        detail("signature : " + editor.signatureCard().label());
        // la virgule frappée déplace le paramètre actif du serveur
        connection.commitText("5,", 1);
        frame();
        for (int wait = 0; wait < 100
                && editor.signatureCard().activeParameter() != 1; wait++) {
            Thread.sleep(20L);
            View.harnessDrain(500L);
            frame();
        }
        check("la virgule du serveur déplace l'actif",
                editor.signatureCard().activeParameter() == 1);
        click("Annuler");
        click("Annuler");
        click("Annuler");
        click("Annuler");
        click("Annuler");
        check("les cinq annulations rendent le texte",
                editor.getBuffer().toString().equals(original));
        click("LSP ✔");
        check("le débranchement rend le bouton",
                findButton(activity.harnessRootView(), "LSP") != null
                        && textOf("LSP :") == null);
    }

    /** La copie des mesures : le presse-papiers porte le tableau. */
    private static void copyMeasures() {
        begin("copie des mesures : le tableau complet au presse-papiers");
        click("Copier les mesures");
        CharSequence clip = android.content.Context.harnessClipboard()
                .getPrimaryClip().getItemAt(0).getText();
        String measures = clip == null ? "" : clip.toString();
        check("l'en-tête du relevé s'y trouve",
                measures.startsWith("Relevés appareil Pigma"));
        check("la ligne de titres s'y trouve", measures.contains("| événement |"));
        check("une première coloration y est (les plus anciennes tombent à 24 lignes)",
                measures.contains("première coloration - "));
        check("la fin de remplissage y est", measures.contains("remplissage fini"));
        check("la bascule de thème y est", measures.contains("thème Sombre"));
        check("la copie y est", measures.contains("copie - "));
        check("la pire du journal porte son attribution",
                measures.contains("Δ mémoire"));
        System.out.println();
        System.out.println("--- journal du scénario (presse-papiers) ---");
        System.out.println(measures);
        System.out.println("--- fin du journal ---");
    }

    // ---------------------------------------------------------- outils

    /**
     * Ouvre un document par son bouton : la première trame paie la
     * fenêtre et relance la salve du fond, les ticks du veilleur
     * voient le fond courir, l'attente réelle laisse la salve
     * achever, le dernier tick journalise la fin, la trame suivante
     * dessine l'état arrivé.
     *
     * @param label l'étiquette du bouton de langage
     * @param fillTimeoutMillis la borne d'attente du fond
     */
    private static void openLanguage(String label, long fillTimeoutMillis) {
        click(label);
        frame();
        View.harnessDrain(1_000L);
        editor.awaitBackgroundFill(fillTimeoutMillis);
        View.harnessDrain(2_000L);
        frame();
    }

    /** Ouvre un scénario à l'écran. */
    private static void begin(String name) {
        scenarios++;
        System.out.println();
        System.out.printf("%2d. %s%n", scenarios, name);
    }

    /**
     * Une vérification : s'imprime et se compte.
     *
     * @param what le fait vérifié
     * @param ok vrai si le fait tient
     */
    private static void check(String what, boolean ok) {
        if (!ok) {
            failures++;
        }
        System.out.printf("    [%s] %s%n", ok ? "OK" : "ÉCHEC", what);
    }

    /** Un détail chiffré à la suite des vérifications. */
    private static void detail(String what) {
        System.out.printf("    · %s%n", what);
    }

    /** Dessine une trame et borne les enregistrements du canevas. */
    private static void frame() {
        canvas.harnessMark();
        editor.draw(canvas);
    }

    /**
     * Un contact court : pose et lève.
     *
     * @param x abscisse
     * @param y ordonnée
     */
    private static void tap(float x, float y) {
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN, x, y));
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP, x, y));
    }

    /**
     * Un double-tap : deux contacts courts au même endroit.
     *
     * @param x abscisse
     * @param y ordonnée
     */
    private static void doubleTap(float x, float y) {
        tap(x, y);
        tap(x, y);
    }

    /**
     * Envoie une touche par événements (action vers le bas).
     *
     * @param code le code de touche
     */
    private static void pressKey(int code) {
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
    }

    /**
     * Cherche l'éditeur dans l'arbre de vues.
     *
     * @param root la racine du contenu
     * @return l'éditeur
     */
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

    /**
     * Cherche un bouton par son étiquette exacte.
     *
     * @param root la racine du contenu
     * @param label l'étiquette
     * @return le bouton, ou null
     */
    private static Button findButton(View root, String label) {
        if (root instanceof Button && label.contentEquals(((Button) root).getText())) {
            return (Button) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int at = 0; at < group.getChildCount(); at++) {
                Button found = findButton(group.getChildAt(at), label);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Déclenche le clic d'un bouton par son étiquette.
     *
     * @param label l'étiquette exacte
     */
    private static void click(String label) {
        clickIn(activity.harnessRootView(), label);
    }

    /**
     * Déclenche le clic d'un bouton par son étiquette, dans une racine
     * donnée (la barre d'une popup flottante).
     *
     * @param root  la racine visitée
     * @param label l'étiquette exacte
     */
    private static void clickIn(View root, String label) {
        Button button = findButton(root, label);
        if (button == null) {
            failures++;
            System.out.printf("    [ÉCHEC] bouton introuvable : %s%n", label);
            return;
        }
        button.performClick();
    }

    /**
     * Cherche une vue de texte par un extrait de son contenu.
     *
     * @param excerpt l'extrait cherché
     * @return le texte complet de la vue, ou null
     */
    private static String textOf(String excerpt) {
        return textOf(activity.harnessRootView(), excerpt);
    }

    /**
     * Cherche une vue de texte par un extrait, en profondeur.
     *
     * @param view la vue visitée
     * @param excerpt l'extrait cherché
     * @return le texte complet, ou null
     */
    private static String textOf(View view, String excerpt) {
        if (view instanceof TextView
                && ((TextView) view).getText().toString().contains(excerpt)) {
            return ((TextView) view).getText().toString();
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int at = 0; at < group.getChildCount(); at++) {
                String found = textOf(group.getChildAt(at), excerpt);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Attend qu'un extrait apparaisse dans une vue de texte (les
     * diagnostics du serveur arrivent d'un autre fil).
     *
     * @param excerpt l'extrait attendu
     * @param timeoutMillis la borne d'attente
     * @return vrai si l'extrait est arrivé
     * @throws InterruptedException si l'attente se rompt
     */
    private static boolean awaitText(String excerpt, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (textOf(excerpt) != null) {
                return true;
            }
            Thread.sleep(20L);
        }
        return textOf(excerpt) != null;
    }

    /**
     * Le point de contact d'un mot : sa première occurrence dessinée.
     *
     * @param word le mot cherché
     * @return abscisse et ordonnée du contact
     */
    private static float[] spotOfWord(String word) {
        for (Canvas.TextOp op : canvas.harnessTextsSinceMark()) {
            if (op.text.contains(word)) {
                return new float[] {op.x + 3.0f, op.y - 4.0f};
            }
        }
        return new float[] {100.0f, 100.0f};
    }

    /**
     * Le point de contact d'un jeton dessiné : sa première occurrence
     * EXACTE (le mot cherché comme jeton entier, pas noyé dans un
     * long jeton de commentaire).
     *
     * @param word le jeton cherché
     * @return abscisse et ordonnée du contact
     */
    private static float[] spotOfToken(String word) {
        for (Canvas.TextOp op : canvas.harnessTextsSinceMark()) {
            if (op.text.equals(word)) {
                return new float[] {op.x + 3.0f, op.y - 4.0f};
            }
        }
        return new float[] {100.0f, 100.0f};
    }

    /**
     * Le double-tap sur un jeton dessiné (sélection puis popup).
     *
     * @param word le jeton cherché
     */
    private static void doubleTapOfToken(String word) {
        float[] spot = spotOfToken(word);
        doubleTap(spot[0], spot[1]);
    }

    /**
     * Le double-tap sur un mot dessiné (sélection puis popup).
     *
     * @param word le mot cherché
     */
    private static void doubleTapOfWord(String word) {
        float[] spot = spotOfWord(word);
        doubleTap(spot[0], spot[1]);
    }

    /**
     * Le bord haut d'une ligne visible (défilement nul dans les
     * scénarios, aucun pli à cet endroit).
     *
     * @param line la ligne
     * @return l'ordonnée du bord haut
     */
    private static float lineTop(int line) {
        return editor.getPaddingTop() + line * LINE_HEIGHT;
    }

    /**
     * Compte les occurrences non chevauchantes d'un mot.
     *
     * @param text le texte
     * @param word le mot
     * @return le compte
     */
    private static int countOccurrences(String text, String word) {
        int count = 0;
        int at = text.indexOf(word);
        while (at >= 0) {
            count++;
            at = text.indexOf(word, at + word.length());
        }
        return count;
    }
}
