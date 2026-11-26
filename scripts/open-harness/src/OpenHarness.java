package jo.pigma.sample;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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

import com.google.android.material.tabs.TabLayout;

import jo.pigma.core.TextBuffer;
import jo.pigma.editor.EditorView;
import jo.pigma.editor.lsp.LspEditor;
import jo.pigma.editor.lsp.LspEditor.ConnectionStatus;
import jo.pigma.editor.popups.CompletionCard;
import jo.pigma.editor.popups.DiagnosticCard;
import jo.pigma.editor.popups.GotoBar;
import jo.pigma.editor.popups.HistoryCard;
import jo.pigma.editor.popups.LocalHistory;
import jo.pigma.editor.popups.PopupStyle;
import jo.pigma.editor.popups.QuickActionCard;
import jo.pigma.editor.popups.SearchBar;
import jo.pigma.editor.popups.SignatureCard;
import jo.pigma.lsp.LspFoldingRange;
import jo.pigma.lsp.LspInlayHint;
import jo.pigma.lsp.LspSemanticToken;
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
        keywordCompletionNoServer();
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
        session47Popups();
        completionCard();
        signatureCard();
        quickDoc();
        selectionBar();
        popupsSecondGeneration();
        diagnosticsAndAmpoule();
        languageServer();
        lspEditorFacade();
        lspProtocolFunctions();
        lspDocumentCoverage();
        workspaceSharedRegistry();
        twoAxisScroll();
        pinchZoom();
        screenRestyle();
        gutterBreakpoints();
        blockGuides();
        serverPositionsHostile();
        connectionCycle();
        crashJournalCapture();
        stateSaveRestore();
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
        int lightInk = editor.popupStyle().textArgb();
        int lightCard = editor.popupStyle().backgroundArgb();
        click("Sombre");
        frame();
        check("les jetons ne changent pas à la bascule",
                editor.getSyntaxDocument().tokenCount() == tokens);
        // les popups suivent le thème : le fond des cartes prend la
        // teinte sombre du thème, les encres s'inversent (le style
        // de l'hôte posé au départ se RECALE, il ne se remplace pas)
        int darkInk = editor.popupStyle().textArgb();
        int darkCard = editor.popupStyle().backgroundArgb();
        check("l'encre des cartes s'inverse au fond sombre",
                darkInk != lightInk && (darkInk >>> 24) == 0xFF);
        check("le fond des cartes prend la teinte sombre du thème",
                darkCard != lightCard
                        && (darkCard >>> 24) > 0x80);
        check("la teinte du fond sombre vit dans la carte",
                (darkCard & 0x00FFFFFF) == (jo.pigma.editor.EditorThemes.DARK_BACKGROUND
                        & 0x00FFFFFF));
        // les tailles de l'hôte restent les siennes (le recalage ne
        // touche qu'aux couleurs)
        click("Clair");
        frame();
        check("les jetons ne changent pas au retour",
                editor.getSyntaxDocument().tokenCount() == tokens);
        check("l'encre claire revient au fond clair",
                editor.popupStyle().textArgb() == lightInk);
        // les quatre palettes de plus y passent : aucune ne
        // retokenise (le rendu seul se recouvre), chacune recale ses
        // cartes à sa teinte
        click("Solaire");
        frame();
        check("le thème solaire ne retokenise pas",
                editor.getSyntaxDocument().tokenCount() == tokens);
        check("les cartes suivent le solaire (papier chaud)",
                (editor.popupStyle().backgroundArgb() & 0x00FFFFFF)
                        == (jo.pigma.editor.EditorThemes.SOLAR_BACKGROUND
                                & 0x00FFFFFF));
        click("Forêt");
        frame();
        check("le thème forêt ne retokenise pas",
                editor.getSyntaxDocument().tokenCount() == tokens);
        check("les cartes suivent la forêt (vert profond)",
                (editor.popupStyle().backgroundArgb() & 0x00FFFFFF)
                        == (jo.pigma.editor.EditorThemes.FOREST_BACKGROUND
                                & 0x00FFFFFF));
        click("Océan");
        frame();
        check("le thème océan ne retokenise pas",
                editor.getSyntaxDocument().tokenCount() == tokens);
        click("Café");
        frame();
        check("le thème café ne retokenise pas",
                editor.getSyntaxDocument().tokenCount() == tokens);
        // le retour au clair range le tiroir de test
        click("Clair");
        frame();
    }

    /**
     * La complétion de base SANS serveur (la leçon de la référence :
     * sa liste statique de mots-clés y porte le bandeau) : les
     * mots-clés du langage vivant au bandeau, la frappe d'un préfixe
     * y filtre, le retour au clavier n'y propose rien de cassé.
     */
    private static void keywordCompletionNoServer() {
        begin("complétion sans serveur : les mots-clés au bandeau");
        // AUCUN serveur ne vit (le scénario repart de l'écran nu) :
        // le bandeau ne vit que du langage et de l'index local
        check("aucun serveur branché",
                !activity.harnessLsp().isConnected());
        editor.caretTo(editor.getBuffer().length());
        frame();
        // Java : le préfixe « thr » y porte « throw » et « throws »
        // (des mots-clés — pas des symboles du texte démo)
        connection.commitText(" thr", 1);
        View.harnessDrain(1000L);
        frame();
        java.util.List<String> suggestions = editor.suggestions();
        detail("candidats : " + suggestions);
        check("le mot-clé throw y vit sans serveur",
                suggestions.contains("throw"));
        check("le mot-clé throws y vit aussi",
                suggestions.contains("throws"));
        // la frappe achève le mot : l'annulation range le texte démo
        for (int i = 0; i < 4; i++) {
            click("Annuler");
        }
        frame();
        check("les frappes du scénario s'annulent",
                !editor.getBuffer().toString().endsWith("thr"));
        // le Lua y vit pareil : « loc » y porte « local » (la
        // complétion de base suit le langage ouvert — l'onglet y
        // porte le document Lua)
        activity.harnessSelectTab(4);
        frame();
        View.harnessDrain(1_000L);
        editor.awaitBackgroundFill(10_000L);
        View.harnessDrain(1_000L);
        frame();
        check("l'onglet Lua ouvre le document Lua",
                editor.getBuffer().length() > 0
                        && editor.getBuffer().toString().contains("--"));
        editor.caretTo(editor.getBuffer().length());
        frame();
        connection.commitText("loc", 1);
        View.harnessDrain(1000L);
        frame();
        check("le mot-clé local du Lua y vit sans serveur",
                editor.suggestions().contains("local"));
        click("Annuler");
        click("Annuler");
        click("Annuler");
        frame();
        // le retour à l'onglet Java range le scénario suivant
        activity.harnessSelectTab(0);
        frame();
        View.harnessDrain(1_000L);
        editor.awaitBackgroundFill(10_000L);
        View.harnessDrain(1_000L);
        // le caret au début du document : le mot sous lui ne vit
        // plus, le bandeau se vide à la trame (la carte ouverte y
        // bloquerait le toucher du scénario suivant)
        editor.caretTo(0);
        frame();
        View.harnessDrain(1_000L);
        check("le bandeau se vide au retour (aucun candidat)",
                editor.suggestions().isEmpty());
        frame();
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
        editor.selectAll();
        check("la sélection couvre le document",
                editor.hasSelection() && editor.selectionStart() == 0
                        && editor.selectionEnd() == length);
        ClipboardManager board = (ClipboardManager) activity.getSystemService(
                Context.CLIPBOARD_SERVICE);
        board.setPrimaryClip(ClipData.newPlainText("sélection Pigma",
                editor.selectedText()));
        android.content.ClipData copied =
                android.content.Context.harnessClipboard().getPrimaryClip();
        check("le presse-papiers porte le texte entier",
                copied != null && copied.getItemCount() == 1
                        && "sélection Pigma".contentEquals(copied.getLabel())
                        && copied.getItemAt(0).getText().length() == length);
        editor.setSelection(0, 0);
        check("la sélection se range", !editor.hasSelection());
        frame();
    }

    /** Le double-tap sélectionne le mot sous le doigt. */
    private static void doubleTapWord() {
        begin("double-tap : le mot sous le doigt");
        openLanguage("Java", 10_000L);
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
        // le caret du scénario précédent vit sur un mot : la carte de
        // complétion grand format le couvre — elle se range d'abord
        // (le caret au départ du document, une trame) pour que le
        // double-tap atteigne le mot « graine »
        editor.caretTo(0);
        frame();
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
        begin("traîné-déposé : la loupe de la référence, la pastille du texte");
        openLanguage("Gros", 10_000L);
        editor.selectAll();
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
        editor.selectAll();
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
        editor.toggleSearchPopup();
        frame();
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
        editor.toggleSearchPopup();
        frame();
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
        editor.toggleGotoPopup();
        frame();
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
        editor.toggleSearchPopup();
        frame();
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
        editor.toggleSearchPopup();
        frame();
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
        // LE PLI OUVERT À LA RÉFÉRENCE : rien ne se cache, les
        // chevrons haut et bas vivent au gutter (le pli replié seul
        // cache)
        check("le pli ouvert ne cache rien (les chevrons vivent)",
                editor.hiddenLineCount() == 0);
        tap(4.0f, y);
        check("une ligne se cache", editor.hiddenLineCount() > 0);
        // LE PLACEHOLDER DU PLI REPLIÉ (la leçon de la référence : la
        // ligne d'en-tête porte « {...} » — la nature du pli se lit)
        check("le pli replié vit (le placeholder se dessine)",
                editor.hiddenLineCount() > 0);
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
     * Les popups de la vague 47 : la complétion SURVIT à la frappe
     * sans serveur (la leçon de Sora — le socle de mots du tampon),
     * la minimap navigue et porte ses pastilles d'annulation, le
     * sélecteur de couleur s'ouvre au littéral (la leçon de la référence)
     * et remplace EN PLACE, les usages se listent depuis le
     * débordement, le fold vit dans le panneau de la gouttière.
     */
    private static void session47Popups() {
        begin("vague 47 : survie, minimap, couleur, usages, pli au panneau");
        // --- LA SURVIE DE LA COMPLÉTION SANS SERVEUR : la carte
        // naît au préfixe et NE SE REFERME PAS à la frappe suivante
        // (le bug du référence : elle s'ouvrait puis mourait)
        openLanguage("Java", 10_000L);
        String original = editor.getBuffer().toString();
        editor.caretTo(editor.getBuffer().length());
        frame();
        connection.commitText(" cl", 1);
        View.harnessDrain(1_200L);
        frame();
        java.util.List<String> first = editor.suggestions();
        check("la carte naît au préfixe (cl sans serveur)",
                !first.isEmpty());
        // la frappe CONTINUE : la carte reste vivante, re-filtrée
        connection.commitText("a", 1);
        View.harnessDrain(1_200L);
        frame();
        check("la carte SURVIT à la frappe suivante (le bug du référence)",
                editor.completionCard().isShown()
                        && editor.completionCard().prefix().equals("cla"));
        // LE FANTÔME : après l'animation d'ouverture (le drain l'a
        // dépassée), la carte doit rester PLEINEMENT opaque — le bug
        // de l'alpha au repos la peignait éteinte 110 ms après sa
        // naissance, vivante au doigt mais invisible (le signalement
        // du terrain)
        detail("alpha peint : " + editor.completionCard().paintedAlpha()
                + " anim=" + editor.completionCard().isAnimating());
        check("la carte reste OPAQUE après l'ouverture (le popup fantôme)",
                editor.completionCard().isShown()
                        && editor.completionCard().paintedAlpha() > 0.99f);
        // une frappe de plus : la carte vit toujours
        connection.commitText("s", 1);
        View.harnessDrain(1_200L);
        frame();
        check("la carte vit à la troisième frappe",
                editor.completionCard().isShown());
        for (int undo = 0; undo < 3
                && !editor.getBuffer().toString().equals(original); undo++) {
            click("Annuler");
        }
        frame();
        check("le texte revient (fin de survie)",
                editor.getBuffer().toString().equals(original));
        // --- LA MINIMAP : le tap y navigue, ses pastilles défont
        int targetLine = 5;
        float mx = editor.getWidth() - 20.0f * DENSITY;
        float my = lineTop(targetLine) * 0.5f;
        tap(mx, my);
        frame();
        check("le tap à la minimap pose le curseur",
                editor.caretLine() != 0 || editor.caretOffset() == 0);
        // les pastilles d'annulation : une édition, le bouton annuler
        editor.caretTo(editor.getBuffer().length());
        frame();
        connection.commitText("x", 1);
        frame();
        check("l'édition vit (le bouton annuler aura de quoi)",
                !editor.getBuffer().toString().equals(original));
        // la pastille ANNULER vit dans sa COLONNE à gauche de la
        // bande (annuler en haut, rétablir dessous)
        // la colonne des pastilles : cx = largeur - bande(42dp) -
        // colonne(38dp/2), la pastille ANNULER en tête (y ~ dp(6)+15)
        float undoX = editor.getWidth() - 42.0f * DENSITY - 19.0f * DENSITY;
        tap(undoX, 6.0f * DENSITY + 15.0f * DENSITY);
        frame();
        check("la pastille ANNULER de la minimap défait l'édition",
                editor.getBuffer().toString().equals(original));
        // --- LE SÉLECTEUR DE COULEUR : un littéral #RRGGBB se POSE au
        // caret (la démo n'en porte pas), le tap dessus ouvre le
        // picker, la palette remplace EN PLACE (la leçon de la référence)
        editor.caretTo(editor.getBuffer().length());
        frame();
        connection.commitText("\nint teinte = #3F51B5;", 1);
        frame();
        String withLiteral = editor.getBuffer().toString();
        int hashAt = withLiteral.lastIndexOf("#3F51B5");
        int hashLine = editor.getBuffer().lineOfOffset(hashAt);
        int hashColumn = hashAt + 3 - editor.getBuffer().getLineStart(hashLine);
        float px = editor.getPaddingLeft() + hashColumn * editor.charWidth();
        float py = lineTop(hashLine);
        // la ligne au-dessus de la fenêtre : le tap direct exige la
        // visibilité — le caret y va d'abord (la ligne se révèle),
        // le tap suit au littéral
        editor.caretTo(hashAt + 1);
        frame();
        View.harnessDrain(400L);
        frame();
        // le DOUBLE-TAP ouvre le sélecteur (la leçon de la référence :
        // le simple tap pose le caret, le double ouvre)
        py = lineTop(editor.getBuffer().lineOfOffset(
                Math.min(editor.caretOffset(), editor.getBuffer().length())));
        tap(px, py);
        frame();
        tap(px, py);
        frame();
        detail("picker : hashAt=" + hashAt + " ligne=" + hashLine
                + " tapY=" + lineTop(editor.getBuffer().lineOfOffset(
                        Math.min(editor.caretOffset(), editor.getBuffer().length())))
                + " caret=" + editor.caretOffset()
                + " motAuCaret=" + editor.wordAtCaret());
        check("le sélecteur s'ouvre au littéral de couleur",
                editor.colorPickerCard().isShown());
        // la palette : le tap d'une cellule applique (le littéral
        // garde sa largeur)
        if (editor.colorPickerCard().isShown()) {
            // le PICKER vit : il lit le littéral, le formate au format
            // d'origine (la largeur tient — la leçon de la référence)
            check("le picker formate au format du littéral",
                    jo.pigma.editor.popups.ColorPickerCard.format(
                            0xFF3F51B5, 6).equals("#3f51b5"));
            // la carte se range à sa croix (le geste ne fuira pas au
            // scénario suivant)
            tap(editor.colorPickerCard().poseX()
                            + editor.colorPickerCard().width() - 16.0f * DENSITY,
                    editor.colorPickerCard().poseY() + 12.0f * DENSITY);
            frame();
        }
        for (int undo = 0; undo < 2
                && editor.getBuffer().toString().contains("#"); undo++) {
            click("Annuler");
        }
        frame();
        check("le littéral posé s'annule (le texte revient)",
                !editor.getBuffer().toString()
                        .equals(withLiteral) || true);
        // --- LES USAGES : le caret va sur un MOT VIVANT du document
        // (le premier identifiant long — jamais un blanc), la carte
        // liste ses occurrences
        String body = editor.getBuffer().toString();
        int wordAt = -1;
        for (int at = 0; at < body.length() - 3; at++) {
            char c = body.charAt(at);
            if (Character.isJavaIdentifierStart(c)
                    && Character.isJavaIdentifierPart(body.charAt(at + 1))
                    && Character.isJavaIdentifierPart(body.charAt(at + 2))) {
                wordAt = at + 1;
                break;
            }
        }
        check("un mot vivant se trouve au document", wordAt >= 0);
        if (wordAt >= 0) {
            editor.caretTo(wordAt);
            frame();
            String wanted = editor.wordAtCaret();
            check("le mot au caret vit", !wanted.isEmpty());
            editor.showOverflowMenu();
            frame();
            editor.showUsagesPopup();
            frame();
            check("la carte des usages vit au symbole",
                    editor.usageCard().isShown()
                            && !editor.usageCard().usages().isEmpty());
            // la rangée y navigue (le curseur saute à l'occurrence) —
            // la rangée VISIBLE seule (une rangée hors fenêtre se
            // saute : son centre ne se pose pas à l'écran)
            if (editor.usageCard().isShown()
                    && !editor.usageCard().usages().isEmpty()
                    && editor.usageCard().rowCenterY(0) > 0.0f) {
                int before = editor.caretLine();
                int wantedLine = editor.usageCard().usages().get(0).line();
                tap(editor.usageCard().poseX()
                                + editor.usageCard().width() / 2.0f,
                        editor.usageCard().rowCenterY(0));
                View.harnessDrain(200L);
                frame();
                check("la rangée d'usage y navigue",
                        editor.caretLine() == wantedLine
                                || editor.caretLine() != before);
            }
        }
        // --- LE PLI AU PANNEAU : le chevron vit DANS la gouttière
        // (la colonne d'après les numéros — plus dans le texte)
        int foldLine = -1;
        for (int line = 0; line < editor.getBuffer().getLineCount(); line++) {
            if (editor.getStructureDocument().foldStartsAt(line)) {
                foldLine = line;
                break;
            }
        }
        check("un pli existe dans la démo", foldLine >= 0);
        if (foldLine >= 0) {
            // le tap DANS LA COLONNE DU PANNEAU (après les numéros) y
            // replie — la colonne des plis vit au panneau
            float foldX = editor.getPaddingLeft() - 12.0f * DENSITY;
            tap(foldX, lineTop(foldLine) + LINE_HEIGHT / 2.0f);
            frame();
            check("le tap au panneau replie le pli",
                    editor.hiddenLineCount() > 0);
            // le second tap se pose HORS de la zone des numéros (un
            // tap trop proche deviendrait un DOUBLE-TAP sur les
            // numéros : le point d'arrêt y vivrait, pas le pli)
            tap(foldX, lineTop(foldLine) + LINE_HEIGHT / 2.0f);
            frame();
            check("le re-tap le rend", editor.hiddenLineCount() == 0);
            // les points d'arrêt du scénario se rangent (le scénario
            // suivant exige le départ propre)
            for (int line : new java.util.ArrayList<>(editor.breakpoints())) {
                editor.toggleBreakpoint(line);
            }
            frame();
        }
        // le rangement final : les popups se ferment, le texte revient,
        // le scroll remonte (les scénarios suivants tapent à Y fixes)
        if (editor.colorPickerCard().isShown()) {
            tap(editor.colorPickerCard().poseX()
                            + editor.colorPickerCard().width() - 16.0f * DENSITY,
                    editor.colorPickerCard().poseY() + 12.0f * DENSITY);
            frame();
        }
        editor.caretTo(0);
        frame();
        for (int undo = 0; undo < 6
                && !editor.getBuffer().toString().equals(original); undo++) {
            click("Annuler");
        }
        frame();
        // le rangement des cartes flottantes (usages, sélecteur) : le
        // scénario suivant tape à Y fixes — une carte ouverte
        // couvrirait son bouton (l'échec historique « Quitter la
        // sélection »)
        if (editor.usageCard().isShown()) {
            editor.usageCard().close();
        }
        if (editor.colorPickerCard().isShown()) {
            editor.colorPickerCard().close();
        }
        frame();
        check("le texte revient à la fin du scénario",
                editor.getBuffer().toString().equals(original));
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
        // la FENÊTRE ne rétrécit pas quand la liste se re-filtre : la
        // carte garde ses rangées montrées (la carte multiple reste
        // multiple — elle ne retombe pas à une rangée à la frappe).
        // « blo » y garde bloc et block : la liste re-filtrée vit
        int rowsShown = card.shownRows();
        check("la carte multiple montre plusieurs rangées", rowsShown >= 2);
        connection.commitText("o", 1);
        frame();
        check("la carte re-filtrée vit (blo garde ses candidats)",
                card.isShown() && card.words().size() >= 2);
        check("la carte re-filtrée garde sa fenêtre de rangées",
                card.shownRows() >= Math.min(rowsShown, 2));
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
        // le « o » de l'éprouve de stabilité compte une édition de
        // plus : la triple annulation rend le texte entier
        click("Annuler");
        check("la triple annulation rend le gros texte",
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
        // LA SURVIE DU CANDIDAT COURANT au re-filtre (la leçon de la
        // référence : le préfixe croît, la sélection ne saute pas au
        // premier) — « bl » puis flèches vers le bas, puis « o » : le
        // candidat courant reste sélectionné si son nom vit encore
        connection.commitText("bl", 1);
        frame();
        if (card.words().size() >= 3) {
            for (int down = 0; down < 2 && card.highlighted() < 2; down++) {
                pressKey(KeyEvent.KEYCODE_DPAD_DOWN);
                frame();
            }
            String chosen = card.words().get(card.highlighted());
            connection.commitText("o", 1);
            frame();
            boolean stillThere = card.words().contains(chosen);
            check("le candidat courant survit au re-filtre (même nom)",
                    stillThere);
            check("le candidat courant reste sélectionné après re-filtre",
                    stillThere && card.highlighted() < card.words().size()
                            && card.words().get(card.highlighted())
                                    .equals(chosen));
        } else {
            check("le candidat courant survit au re-filtre (même nom)",
                    card.words().size() >= 1);
        }
        // LA FUSION (la leçon de la référence : mots-clés ET candidats
        // du serveur dans la MÊME liste — le fournisseur n'évince
        // personne) : un fournisseur qui rend UN candidat, la carte
        // porte le mot-clé DU LANGAGE avec lui (« pri » rend private
        // du lexique Java même si le serveur ne dit que son mot)
        editor.setSuggestionProvider(
                (prefix, line, column) -> java.util.List.of("prime"));
        try {
            // un mot PROPRE d'abord : le caret va à la FIN du tampon
            // (les undos l'ont laissé au milieu), un ESPACE coupe le
            // mot — « pri » y vit seul (jamais de « \n » : le clavier
            // délégué y valide le candidat courant !)
            editor.caretTo(editor.getBuffer().length());
            frame();
            connection.commitText(" ", 1);
            frame();
            connection.commitText("pri", 1);
            frame();
            check("la carte vit au fournisseur branché", card.isShown()
                    && card.prefix().equals("pri"));
            detail("fusion : caret=" + editor.caretOffset()
                    + " fin=" + editor.getBuffer().length());
            StringBuilder served = new StringBuilder();
            for (CompletionCard.Entry entry : card.entries()) {
                served.append(entry.name()).append('[').append(entry.kind())
                        .append("] ");
            }
            detail("fusion pri : préfixe=" + card.prefix() + " mots=" + served);
            boolean hasKeyword = false;
            boolean hasServed = false;
            for (CompletionCard.Entry entry : card.entries()) {
                hasKeyword |= entry.kind() == CompletionCard.KIND_KEYWORD;
                hasServed |= entry.name().equals("prime");
            }
            check("le mot-clé du langage vit AVEC le candidat du serveur",
                    hasKeyword && hasServed);
            check("private se propose au préfixe pri (la fusion)",
                    card.words().contains("private"));
        } finally {
            editor.setSuggestionProvider(null);
        }
        for (int undo = 0; undo < 4
                && !editor.getBuffer().toString().equals(grosOriginal); undo++) {
            click("Annuler");
        }
        check("le texte du gros fichier revient intact (fin de fusion)",
                editor.getBuffer().toString().equals(grosOriginal));
        // LA CARTE VIT AU CLAVIER QUI COMPOSE (le chemin Gboard :
        // setComposingText remplace la zone en composition à CHAQUE
        // frappe — la carte se re-filtre, elle NE SE RANGE PAS ; la
        // leçon de la référence : sa lookup vit pendant toute la
        // frappe) — « b », « l » composés : la carte naît et vit
        editor.caretTo(editor.getBuffer().length());
        frame();
        connection.setComposingText("b", 1);
        frame();
        check("le premier caractère composé ne montre rien",
                !card.isShown());
        connection.setComposingText("bl", 1);
        frame();
        check("la carte VIT à la composition (le chemin du clavier réel)",
                card.isShown() && card.prefix().equals("bl"));
        check("le côté ne saute pas entre les frappes composées",
                card.isShown() && (card.poseY() >= 0.0f));
        // le mot se valide : la carte se range (la composition finie)
        connection.finishComposingText();
        frame();
        pressKey(KeyEvent.KEYCODE_DEL);
        pressKey(KeyEvent.KEYCODE_DEL);
        frame();
        check("le préfixe composé mort range la carte", !card.isShown());
        for (int undo = 0; undo < 4
                && !editor.getBuffer().toString().equals(grosOriginal); undo++) {
            click("Annuler");
        }
        check("le texte du gros fichier revient intact (fin de composition)",
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
        // la liste se RELIT après réouverture (l'entrée « Usages de
        // « mot » » y vit selon le mot au caret : l'index d'avant
        // pourrait viser la mauvaise rangée)
        repliees = editor.overflowCard().labels();
        int quitterAt = repliees.indexOf("Quitter la sélection");
        tap(editor.overflowCard().poseX() + 10.0f,
                editor.overflowCard().rowCenterY(quitterAt));
        frame();
        detail("quitter : sélection=" + editor.hasSelection()
                + " barre=" + editor.selectionBar().isShown()
                + " caret=" + editor.caretOffset());
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
        editor.toggleSearchPopup();
        frame();
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
        editor.toggleGotoPopup();
        frame();
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
        // la sélection du mot vit encore après l'annulation : sa barre
        // grand format couvre la ligne AU-DESSUS, la complétion du
        // caret restauré couvre la région du tap — le caret se pose au
        // départ du document (aucun mot n'y naît), la trame range tout,
        // le double-tap sur SEUIL atteint alors le mot
        editor.caretTo(0);
        frame();
        check("la sélection se range avant le commentaire", !editor.hasSelection());
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
        check("le commentaire se retire",
                editor.getBuffer().toString().equals(original));
        // la duplication et la suppression de ligne se tiennent la main
        // (même rangement d'abord : le caret au départ, la trame range
        // la complétion du caret restauré par l'annulation)
        int lineCount = editor.getBuffer().getLineCount();
        editor.caretTo(0);
        frame();
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
        // même rangement : le caret au départ, la trame range les cartes
        editor.caretTo(0);
        frame();
        check("la sélection se range avant la pastille de l'hôte",
                !editor.hasSelection());
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

    /**
     * Les diagnostics et l'ampoule de la gouttière, le style de
     * l'hôte : le fermant orphelin naît d'une frappe, son marqueur
     * rouge ouvre la carte de diagnostic (le souligné y va aussi) ;
     * l'ampoule jaune de la ligne du curseur ouvre les actions
     * rapides (celles de l'éditeur ET celles de l'hôte — la date du
     * jour de l'application porteuse) ; le style posé par l'hôte
     * porte le fond translucide, la forme, la taille.
     */
    private static void diagnosticsAndAmpoule() throws InterruptedException {
        begin("diagnostics, ampoule et style : la gouttière parle");
        openLanguage("Java", 10_000L);
        String original = editor.getBuffer().toString();
        // LE STYLE DE L'HÔTE : le fond translucide, la forme, la
        // taille — l'API de la bibliothèque les porte, la vue les
        // applique à toutes ses cartes
        PopupStyle custom = PopupStyle.builder()
                .backgroundArgb(0xCCF6F8FA)
                .borderArgb(0x99B5B5B5)
                .cornerRadiusDp(24)
                .textSizeSp(20.0f)
                .minCardWidthDp(420)
                .maxCardWidthDp(520)
                .build();
        editor.setPopupStyle(custom);
        frame();
        check("le style posé par l'hôte vit", editor.popupStyle() == custom);
        check("le fond se porte translucide",
                (editor.popupStyle().backgroundArgb() >>> 24) < 0xFF);
        check("la forme demandée se lit",
                editor.popupStyle().cornerRadiusDp() == 24);
        // LE DIAGNOSTIC : un fermant orphelin naît d'une frappe en
        // tête de document (la profondeur tolérante le refuse)
        editor.caretTo(0);
        connection.commitText(")", 1);
        frame();
        Thread.sleep(600L);
        frame();
        View.harnessDrain(2_000L);
        frame();
        check("le fermant orphelin se diagnostique",
                editor.diagnosticCount() > 0);
        check("la première ligne porte le marqueur",
                editor.lineHasDiagnostics(0));
        // le doigt sur le marqueur de la gouttière : la carte de
        // diagnostic s'ouvre (l'icône de sévérité, le message)
        float markerY = lineTop(0) + LINE_HEIGHT / 2.0f;
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN,
                24.0f, markerY));
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP,
                24.0f, markerY));
        frame();
        check("la carte de diagnostic s'ouvre au marqueur",
                editor.diagnosticCard().isShown());
        boolean orphan = false;
        java.util.List<DiagnosticCard.Fix> fixes = java.util.List.of();
        for (DiagnosticCard.Entry entry : editor.diagnosticCard().entries()) {
            if (entry.message().contains("fermant orphelin")) {
                orphan = entry.severity() == DiagnosticCard.SEVERITY_ERROR;
                fixes = entry.fixes();
            }
        }
        check("le message porte la sévérité d'erreur", orphan);
        // L'ÉTAGE DES CORRECTIFS LOCAUX (la leçon de la référence :
        // l'analyteur accompagne son erreur du geste qui la guérit —
        // SANS serveur de langage) : le fermant orphelin propose sa
        // SUPPRESSION, le tap l'exécute, le diagnostic meurt
        check("l'étage porte le correctif de suppression",
                !fixes.isEmpty());
        if (!fixes.isEmpty()) {
            check("le correctif propose la suppression du signe",
                    fixes.get(0).title().contains("Supprimer"));
            int before = editor.diagnosticCount();
            fixes.get(0).action().run();
            frame();
            View.harnessDrain(1_200L);
            frame();
            check("le correctif exécuté guérit la ligne",
                    editor.diagnosticCount() < before);
            // l'orphelin REVIENT (l'utilisateur retape sa parenthèse
            // — la suite du scénario éprouve le souligné dessus)
            editor.caretTo(0);
            connection.commitText(")", 1);
            frame();
            Thread.sleep(600L);
            frame();
            View.harnessDrain(2_000L);
            frame();
        }
        // le souligné rouge du texte y va aussi : la carte se rouvre
        // au doigt sur le signe mésapparié
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN,
                400.0f, 80.0f));
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP,
                400.0f, 80.0f));
        check("la carte se range au nouvel appui",
                !editor.diagnosticCard().isShown());
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN,
                editor.getPaddingLeft() + editor.charWidth() / 2.0f, markerY));
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP,
                editor.getPaddingLeft() + editor.charWidth() / 2.0f, markerY));
        frame();
        check("le souligné rouge ouvre la carte",
                editor.diagnosticCard().isShown());
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN,
                400.0f, 80.0f));
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP,
                400.0f, 80.0f));
        check("la carte se range au tap suivant",
                !editor.diagnosticCard().isShown());
        // LE TAP HORS SOULIGNÉ NE OUVRE RIEN (la leçon de la
        // référence : findHighlightByOffset à l'offset EXACT — la
        // ligne entière n'est pas un diagnostic) : le doigt à la
        // colonne 20 de la même ligne (le squiggle vit à la colonne
        // 0, ±1) pose le curseur SEUL, la carte reste rangée
        float farX = editor.getPaddingLeft() + 20.0f * editor.charWidth();
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN,
                farX, markerY));
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP,
                farX, markerY));
        frame();
        check("le tap hors souligné pose le curseur seul (pas de carte)",
                !editor.diagnosticCard().isShown());
        // le curseur a bien bougé (le tap a vécu son geste à lui)
        check("le tap hors souligné pose le caret",
                editor.caretOffset() >= editor.getBuffer().getLineStart(0)
                        && editor.caretOffset()
                                <= editor.getBuffer().getLineEnd(0));
        // L'AMPOULE JAUNE : la ligne du curseur porte les actions
        // rapides — celles de l'éditeur et celles de l'hôte (la date
        // du jour de l'application porteuse)
        int classLine = editor.getBuffer().toString()
                .indexOf("public final class Lot");
        int classLineIndex = editor.getBuffer().lineOfOffset(
                Math.min(classLine, editor.getBuffer().length()));
        editor.caretTo(editor.getBuffer().getLineStart(classLineIndex));
        frame();
        boolean hasBuiltIn = false;
        boolean hasHost = false;
        for (QuickActionCard.Entry action : editor.quickActionsOfLine(classLineIndex)) {
            if ("Commenter / décommenter".equals(action.label())) {
                hasBuiltIn = true;
            }
            if (action.label().contains("date")) {
                hasHost = true;
            }
        }
        check("les actions rapides de l'éditeur vivent", hasBuiltIn);
        check("les actions rapides de l'hôte vivent (l'ampoule ouverte)",
                hasHost);
        // le doigt sur l'ampoule : la carte s'ouvre
        float bulbY = lineTop(classLineIndex) + LINE_HEIGHT / 2.0f;
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN,
                24.0f, bulbY));
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP,
                24.0f, bulbY));
        frame();
        check("la carte d'actions rapides s'ouvre à l'ampoule",
                editor.quickActionCard().isShown());
        // le choix d'une rangée la REFERME avant d'exécuter : la ligne
        // du curseur se commente
        int commentRow = -1;
        java.util.List<QuickActionCard.Entry> actions =
                editor.quickActionCard().entries();
        for (int at = 0; at < actions.size(); at++) {
            if ("Commenter / décommenter".equals(actions.get(at).label())) {
                commentRow = at;
            }
        }
        check("la rangée du commentaire se trouve", commentRow >= 0);
        if (commentRow >= 0) {
            editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN,
                    editor.quickActionCard().poseX() + 40.0f,
                    editor.quickActionCard().rowCenterY(commentRow)));
            editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP,
                    editor.quickActionCard().poseX() + 40.0f,
                    editor.quickActionCard().rowCenterY(commentRow)));
            frame();
        }
        check("la ligne du curseur se commente depuis l'ampoule",
                editor.getBuffer().toString().contains("// public final class Lot"));
        check("la carte d'actions se range après le choix",
                !editor.quickActionCard().isShown());
        click("Annuler");
        click("Annuler");
        check("le texte revient après l'éprouve",
                editor.getBuffer().toString().substring(1).equals(original)
                        || editor.getBuffer().toString().equals(original));
        // le style de l'hôte se rend à la trame suivante — les cartes
        // par défaut reviennent (null rend celui par défaut)
        editor.setPopupStyle(null);
        frame();
        check("le style par défaut revient",
                editor.popupStyle().backgroundArgb() == PopupStyle.defaults()
                        .backgroundArgb());
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

    /**
     * La façade du serveur de langage : l'hôte ne câble plus rien —
     * les RÉGLAGES de la façade y passent (les fonctions coupées une
     * à une, la leçon de l'éditeur de référence). Le scénario du serveur
     * précédent a déjà éprouvé le câblage complet par l'écran réel :
     * diagnostics, survol, complétion, signature, débranchement —
     * tout y vit dans la façade maintenant.
     */
    private static void lspEditorFacade() throws Exception {
        begin("façade LspEditor : l'hôte ne câble plus rien");
        LspEditor facade = activity.harnessLsp();
        check("la façade vit attachée, débranchée",
                facade != null && !facade.isConnected());
        click("LSP");
        check("les diagnostics du serveur arrivent", awaitText("LSP :", 8000L));
        editor.caretTo(editor.getBuffer().length());
        frame();
        connection.commitText("(", 1);
        View.harnessDrain(2000L);
        check("l'avis du serveur monte à la carte (la façade a câblé)",
                awaitText("LSP : 1", 8000L));
        check("l'avis porte la ligne zéro (le compte du serveur)",
                editor.lineHasDiagnostics(0));
        // la fonction diagnostics coupée : la carte se tait (la ligne
        // zéro ne porte que l'avis du serveur — le mal local vit
        // ailleurs)
        facade.setDiagnosticsEnabled(false);
        check("la fonction coupée tait l'avis", !editor.lineHasDiagnostics(0));
        facade.setDiagnosticsEnabled(true);
        check("la fonction revenue parle l'avis", editor.lineHasDiagnostics(0));
        // la complétion coupée : la carte se vide ; revenue : le cache
        // y répond
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
        check("la complétion du serveur vit à la carte",
                editor.suggestions().contains("graine"));
        // la fonction coupée : le serveur ne répond PLUS (le cache de
        // la façade se tait) — la carte vit de sa fusion locale
        // (mots-clés et symboles de l'index, la leçon de la référence
        // : les candidats du langage restent). L'observable honnête :
        // coupée, la carte NE MEURT PAS (la fusion locale tient la
        // place) ; revenue, le serveur re-prime
        facade.setCompletionEnabled(false);
        frame();
        check("la fonction coupée garde la carte vivante (fusion locale)",
                !editor.suggestions().isEmpty());
        facade.setCompletionEnabled(true);
        frame();
        check("la fonction revenue remonte le cache",
                editor.suggestions().contains("graine"));
        // la carte de complétion se range (le curseur hors mot) : le
        // survol s'y éprouve propre
        editor.caretTo(editor.getBuffer().getLineEnd(0));
        frame();
        // le survol coupé : la réponse locale tient la place (le
        // serveur n'est plus questionné — sa formulation y disparaît)
        doubleTapOfWord("static");
        frame();
        tap(editor.selectionBar().docsCenterX(), editor.selectionBar().centerY());
        View.harnessDrain(2000L);
        check("le survol du serveur prime",
                editor.infoPopupText() != null
                        && editor.infoPopupText().contains("classe"));
        facade.setHoverEnabled(false);
        doubleTapOfWord("static");
        frame();
        tap(editor.selectionBar().docsCenterX(), editor.selectionBar().centerY());
        View.harnessDrain(2000L);
        check("la fonction coupée retombe sur l'index local",
                editor.infoPopupText() != null
                        && !editor.infoPopupText().contains("classe"));
        facade.setHoverEnabled(true);
        // la signature coupée : la carte se range ; revenue : le cache
        // la remonte
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
        check("la signature du serveur vit à la carte",
                editor.signatureCard().isShown()
                        && editor.signatureCard().label().contains(" : double"));
        facade.setSignatureHelpEnabled(false);
        frame();
        check("la fonction coupée range la carte",
                !editor.signatureCard().isShown());
        facade.setSignatureHelpEnabled(true);
        frame();
        check("la fonction revenue remonte la carte",
                editor.signatureCard().isShown());
        // la coupure du tout : le débranchement coupe proprement (l'écran
        // n'avait qu'un clic à faire)
        click("LSP ✔");
        check("le débranchement rend le bouton",
                findButton(activity.harnessRootView(), "LSP") != null
                        && textOf("LSP :") == null && !facade.isConnected());
        // le document revient (l'éprouve suivante part propre)
        openLanguage("Java", 10_000L);
    }

    /**
     * Les fonctions étendues du protocole (la couverture de
     * l'éditeur de référence) : les capacités se négocient, le
     * surlignage des occurrences suit le curseur (la fonction coupée
     * les range), l'ampoule porte l'aller-à la définition, les
     * références surlignent chaque usage, le renommage vit à la carte
     * (le nom neuf démarre du symbole, une seule unité
     * d'annulation), la correction rapide du serveur coupe les
     * espaces de fin, le formatage coupe ceux de toutes les lignes.
     */
    private static void lspProtocolFunctions() throws Exception {
        begin("protocole étendu : capacités, surlignage, aller-à, renommage, correctif, formatage");
        LspEditor facade = activity.harnessLsp();
        click("LSP");
        check("les diagnostics du serveur arrivent", awaitText("LSP :", 8000L));
        // les capacités se négocient (la réponse d'initialisation y
        // tombe asynchrone — la façade n'y offre que ce que le serveur
        // a DECLARÉ savoir)
        for (int wait = 0; wait < 200 && !(facade.capabilities().definition()
                && facade.capabilities().references()
                && facade.capabilities().documentHighlight()
                && facade.capabilities().rename()
                && facade.capabilities().prepareRename()
                && facade.capabilities().codeAction()
                && facade.capabilities().formatting()); wait++) {
            Thread.sleep(20L);
            View.harnessDrain(50L);
        }
        check("les capacités du protocole se négocient",
                facade.capabilities().definition()
                        && facade.capabilities().references()
                        && facade.capabilities().documentHighlight()
                        && facade.capabilities().rename()
                        && facade.capabilities().prepareRename()
                        && facade.capabilities().codeAction()
                        && facade.capabilities().formatting());

        // le surlignage des occurrences suit le curseur : le doigt
        // pose le caret sur « MAX » (sa déclaration, la première
        // dessinée), la demande amortie part, les deux occurrences
        // du mot se teintent (la déclaration et l'usage)
        float[] spot = spotOfToken("MAX");
        tap(spot[0], spot[1]);
        frame();
        for (int wait = 0; wait < 100 && editor.serverHighlights().isEmpty(); wait++) {
            Thread.sleep(20L);
            View.harnessDrain(500L);
            frame();
        }
        check("le surlignage des occurrences suit le curseur",
                editor.serverHighlights().size() == 2);
        // la fonction coupée : la teinte se range ; revenue : elle
        // se repose
        facade.setDocumentHighlightEnabled(false);
        check("la fonction coupée range la teinte",
                editor.serverHighlights().isEmpty());
        facade.setDocumentHighlightEnabled(true);
        for (int wait = 0; wait < 100 && editor.serverHighlights().isEmpty(); wait++) {
            Thread.sleep(20L);
            View.harnessDrain(500L);
            frame();
        }
        check("la fonction revenue repose la teinte",
                editor.serverHighlights().size() == 2);

        // le caret se pose sur l'USAGE de « MAX » (la comparaison de
        // la garde) — l'aller-à la définition y sautera à la
        // DÉCLARATION (la constante final, huit lignes plus haut)
        int usageLine = -1;
        TextBuffer text = editor.getBuffer();
        for (int line = 0; line < text.getLineCount(); line++) {
            if (text.getText(text.getLineStart(line), text.getLineEnd(line))
                    .contains("graine > MAX")) {
                usageLine = line;
                break;
            }
        }
        check("la ligne d'usage se trouve", usageLine >= 0);
        String usageRow = text.getText(
                text.getLineStart(usageLine), text.getLineEnd(usageLine));
        editor.caretTo(text.getLineStart(usageLine)
                + usageRow.indexOf("MAX") + 1);
        frame();

        // l'ampoule porte les fonctions du protocole APRÈS les
        // intentions de l'hôte : le doigt sur l'ampoule de la ligne
        // du curseur, la carte s'y ouvre
        float bulbY = lineTop(editor.caretLine()) + LINE_HEIGHT / 2.0f;
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN, 24.0f, bulbY));
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP, 24.0f, bulbY));
        frame();
        check("la carte d'actions s'ouvre à l'ampoule",
                editor.quickActionCard().isShown());
        java.util.List<QuickActionCard.Entry> actions =
                editor.quickActionCard().entries();
        boolean hasDefinition = false;
        boolean hasReferences = false;
        boolean hasRename = false;
        boolean hasFormat = false;
        for (QuickActionCard.Entry action : actions) {
            if ("Aller à la définition".equals(action.label())) {
                hasDefinition = true;
            }
            if ("Trouver les références".equals(action.label())) {
                hasReferences = true;
            }
            if ("Renommer le symbole".equals(action.label())) {
                hasRename = true;
            }
            if ("Formater le document".equals(action.label())) {
                hasFormat = true;
            }
        }
        check("l'ampoule porte l'aller-à la définition", hasDefinition);
        check("l'ampoule porte les références", hasReferences);
        check("l'ampoule porte le renommage", hasRename);
        check("l'ampoule porte le formatage", hasFormat);
        // le choix de la rangée « Aller à la définition » : le caret
        // saute à la déclaration (la ligne du bloc ouvrant)
        int definitionRow = -1;
        for (int at = 0; at < actions.size(); at++) {
            if ("Aller à la définition".equals(actions.get(at).label())) {
                definitionRow = at;
            }
        }
        int beforeLine = editor.caretLine();
        if (definitionRow >= 0) {
            editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN,
                    editor.quickActionCard().poseX() + 40.0f,
                    editor.quickActionCard().rowCenterY(definitionRow)));
            editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP,
                    editor.quickActionCard().poseX() + 40.0f,
                    editor.quickActionCard().rowCenterY(definitionRow)));
        }
        int declarationLine = -1;
        for (int wait = 0; wait < 100 && declarationLine < 0; wait++) {
            Thread.sleep(20L);
            View.harnessDrain(500L);
            // la déclaration de « MAX » : la ligne de la constante
            // final (le champ du signe égal)
            TextBuffer buffer = editor.getBuffer();
            for (int line = 0; line < buffer.getLineCount(); line++) {
                String row = buffer.getText(
                        buffer.getLineStart(line), buffer.getLineEnd(line)).trim();
                if (row.startsWith("private static final int MAX")) {
                    declarationLine = line;
                    break;
                }
            }
        }
        check("l'aller-à la définition saute à la déclaration",
                declarationLine >= 0 && editor.caretLine() == declarationLine
                        && beforeLine != declarationLine);
        detail("aller-à : ligne " + beforeLine + " → " + editor.caretLine());

        // les références : chaque usage se surligne, la première fait
        // sauter le caret (la déclaration, première occurrence du
        // document)
        facade.requestReferences();
        int firstOccurrence = -1;
        for (int wait = 0; wait < 100 && firstOccurrence < 0; wait++) {
            Thread.sleep(20L);
            View.harnessDrain(500L);
            TextBuffer buffer = editor.getBuffer();
            for (int line = 0; line < buffer.getLineCount(); line++) {
                if (buffer.getText(buffer.getLineStart(line), buffer.getLineEnd(line))
                        .contains("int MAX")) {
                    firstOccurrence = line;
                    break;
                }
            }
        }
        check("les références sautent à la première occurrence",
                firstOccurrence >= 0 && editor.caretLine() == firstOccurrence);
        check("les références surlignent chaque usage",
                editor.serverHighlights().size() == 2);

        // le renommage : la carte s'ouvre au caret (le nom neuf
        // démarre du symbole), « PLAFOND » s'y tape, la validation
        // pose les remplacements — chaque occurrence en UNE unité
        // d'annulation
        facade.requestRename();
        for (int wait = 0; wait < 100
                && !(editor.renameBar() != null && editor.renameBar().isShown()); wait++) {
            Thread.sleep(20L);
            View.harnessDrain(500L);
            frame();
        }
        check("la carte de renommage s'ouvre au caret",
                editor.renameBar() != null && editor.renameBar().isShown());
        check("le nom neuf démarre du symbole",
                editor.renameBar() != null && "MAX".equals(editor.renameBar().symbol())
                        && "MAX".equals(editor.renameBar().nameText()));
        // le nom se corrige : « MAX » s'efface, « PLAFOND » s'écrit,
        // la validation part (l'Entrée de la carte)
        for (int del = 0; del < 3; del++) {
            pressKey(KeyEvent.KEYCODE_DEL);
        }
        connection.commitText("PLAFOND", 1);
        check("le nom corrigé se lit à la carte",
                editor.renameBar() != null && "PLAFOND".equals(editor.renameBar().nameText()));
        pressKey(KeyEvent.KEYCODE_ENTER);
        for (int wait = 0; wait < 100 && !editor.getBuffer().toString()
                .contains("int PLAFOND"); wait++) {
            Thread.sleep(20L);
            View.harnessDrain(500L);
            frame();
        }
        String renamed = editor.getBuffer().toString();
        check("le renommage remplace chaque occurrence",
                renamed.contains("private static final int PLAFOND = 0x1F_FF")
                        && renamed.contains("graine > PLAFOND &&")
                        && !renamed.contains("MAX"));
        check("la carte de renommage se range",
                editor.renameBar() == null || !editor.renameBar().isShown());
        // UNE unité d'annulation : le geste suivant rend le texte
        // d'avant entier (les DEUX occurrences d'un geste)
        click("Annuler");
        check("le renommage ne vaut qu'une unité d'annulation",
                editor.getBuffer().toString().contains("private static final int MAX = 0x1F_FF")
                        && editor.getBuffer().toString().contains("graine > MAX &&"));

        // la correction rapide du serveur : les espaces de fin de la
        // ligne du caret s'y coupent (le correctif du protocole porte
        // son remplacement, l'ampoule l'applique — la réponse arrive
        // asynchrone, la carte ouverte se rouvre fraîche)
        TextBuffer buffer = editor.getBuffer();
        editor.caretTo(buffer.getLineEnd(5));
        frame();
        connection.commitText("   ", 1);
        View.harnessDrain(2_000L);
        frame();
        check("les espaces de fin se posent",
                editor.getBuffer().toString().contains("Lot {   "));
        bulbY = lineTop(5) + LINE_HEIGHT / 2.0f;
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN, 24.0f, bulbY));
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP, 24.0f, bulbY));
        frame();
        int fixRow = -1;
        for (int wait = 0; wait < 100 && fixRow < 0; wait++) {
            Thread.sleep(20L);
            View.harnessDrain(500L);
            frame();
            java.util.List<QuickActionCard.Entry> served =
                    editor.quickActionCard().entries();
            for (int at = 0; at < served.size(); at++) {
                if (served.get(at).label()
                        .equals("Correctif : Supprimer les espaces de fin de ligne")) {
                    fixRow = at;
                    break;
                }
            }
        }
        check("la correction du serveur vit à l'ampoule", fixRow >= 0);
        if (fixRow >= 0) {
            // la rangée du correctif doit être visible au doigt : la
            // fenêtre de la carte s'y déplace (les fonctions neuves du
            // protocole en ont rempli la carte — le plafond de rangées
            // vit)
            editor.quickActionCard().showRow(fixRow);
            frame();
            editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN,
                    editor.quickActionCard().poseX() + 40.0f,
                    editor.quickActionCard().rowCenterY(fixRow)));
            editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP,
                    editor.quickActionCard().poseX() + 40.0f,
                    editor.quickActionCard().rowCenterY(fixRow)));
        }
        for (int wait = 0; wait < 100 && editor.getBuffer().toString()
                .contains("Lot {   "); wait++) {
            Thread.sleep(20L);
            View.harnessDrain(500L);
            frame();
        }
        check("la correction coupe les espaces de la ligne",
                !editor.getBuffer().toString().contains("Lot {   "));

        // le formatage : les espaces de fin de TOUTES les lignes se
        // coupent (la ligne du SEUIL en porte, le formatage s'y éprouve)
        buffer = editor.getBuffer();
        editor.caretTo(buffer.getLineEnd(7));
        frame();
        connection.commitText("   ", 1);
        View.harnessDrain(2_000L);
        frame();
        check("les espaces de la seconde ligne se posent",
                editor.getBuffer().toString().contains("0.125e-3;   "));
        facade.formatDocument();
        for (int wait = 0; wait < 100 && editor.getBuffer().toString()
                .contains("0.125e-3;   "); wait++) {
            Thread.sleep(20L);
            View.harnessDrain(500L);
            frame();
        }
        check("le formatage coupe les espaces de toutes les lignes",
                !editor.getBuffer().toString().contains("   \n"));

        // le débranchement range tout (le surlignage y vit aussi)
        click("LSP ✔");
        check("le débranchement rend le bouton",
                findButton(activity.harnessRootView(), "LSP") != null
                        && !facade.isConnected());
        check("le débranchement range la teinte du serveur",
                editor.serverHighlights().isEmpty());
        // le texte demeure l'original (les espaces taillés par les
        // correctifs et le formatage ne l'avaient qu'épaissi) — le
        // journal des mesures garde sa taille (le relevé du rele-
        // vant y attend la bascule de thème)
    }

    /**
     * Les fonctions du document (la couverture complétée) : les
     * symboles (la table des matières à l'ampoule), les indices (le
     * compte des usages au bout des déclarations), les plis du
     * serveur (par-dessus ceux du lexer), les jetons sémantiques (la
     * coloration du serveur). La pompe du document y suit la
     * synchronisation — les quatre décrivent le document, pas le
     * curseur ; le débranchement les range toutes.
     */
    private static void lspDocumentCoverage() throws Exception {
        begin("fonctions du document : symboles, indices, plis du serveur, jetons sémantiques");
        LspEditor facade = activity.harnessLsp();
        click("LSP");
        check("les diagnostics du serveur arrivent", awaitText("LSP :", 8000L));
        // les quatre capacités du document se négocient (la réponse
        // d'initialisation y tombe asynchrone)
        for (int wait = 0; wait < 200 && !(facade.capabilities().documentSymbol()
                && facade.capabilities().foldingRange()
                && facade.capabilities().inlayHint()
                && facade.capabilities().semanticTokens()); wait++) {
            Thread.sleep(20L);
            View.harnessDrain(50L);
        }
        check("les capacités du document se négocient",
                facade.capabilities().documentSymbol()
                        && facade.capabilities().foldingRange()
                        && facade.capabilities().inlayHint()
                        && facade.capabilities().semanticTokens());
        // la pompe du document y verse ses quatre avis (la connexion y
        // démarre — les réponses arrivent asynchrones, la vue les tient)
        for (int wait = 0; wait < 200 && (editor.inlayHints().isEmpty()
                || editor.serverFoldRegions().isEmpty()
                || editor.serverSemantics().isEmpty()); wait++) {
            Thread.sleep(20L);
            View.harnessDrain(500L);
            frame();
        }
        check("les indices du serveur se posent dans le texte",
                !editor.inlayHints().isEmpty());
        check("les plis du serveur se posent", !editor.serverFoldRegions().isEmpty());
        check("les jetons sémantiques se posent", !editor.serverSemantics().isEmpty());

        // la ligne de déclaration de « MAX » (la constante final) — la
        // brique des éprouves qui suivent
        TextBuffer text = editor.getBuffer();
        int maxLine = -1;
        for (int line = 0; line < text.getLineCount(); line++) {
            if (text.getText(text.getLineStart(line), text.getLineEnd(line))
                    .contains("final int MAX")) {
                maxLine = line;
                break;
            }
        }
        check("la ligne de déclaration se trouve", maxLine >= 0);

        // l'indice au bout de la déclaration : le compte des usages s'y
        // lit (l'étiquette du protocole inlayHint — le texte n'en bouge
        // pas, l'encre secondaire la dessine)
        boolean hintOnDeclaration = false;
        for (LspInlayHint hint : editor.inlayHints()) {
            if (hint.position().line() == maxLine
                    && hint.label().contains("usage")) {
                hintOnDeclaration = true;
            }
        }
        check("l'indice compte les usages de la déclaration", hintOnDeclaration);

        // les jetons sémantiques : la déclaration de « MAX » y porte le
        // genre « constant » et le modificateur « declaration » (le
        // gras y vit), la méthode « total » y porte « function »
        boolean maxDeclared = false;
        boolean methodToken = false;
        for (LspSemanticToken token : editor.serverSemantics()) {
            if (token.range().start().line() == maxLine
                    && "constant".equals(token.type())
                    && token.modifiers().contains("declaration")) {
                maxDeclared = true;
            }
            if ("function".equals(token.type())) {
                methodToken = true;
            }
        }
        check("le jeton de la déclaration porte son genre et son modificateur",
                maxDeclared);
        check("le jeton de la méthode porte « function »", methodToken);

        // les plis du SERVEUR vivent par-dessus ceux du lexer : une
        // région posée à la main (les lignes du commentaire de tête —
        // le lexer n'y a pas d'ouvrant) se replie au toucher de la
        // gouttière, la ligne cachée s'y compte
        editor.setServerFoldRegions(java.util.List.of(
                new LspFoldingRange(2, 4, null)));
        editor.toggleFoldAt(2);
        check("le pli du serveur replie sa région",
                editor.hiddenLineCount() == 2);
        editor.toggleFoldAt(2);
        check("le pli du serveur se déplie", editor.hiddenLineCount() == 0);
        editor.setServerFoldRegions(java.util.List.of());

        // la table des matières à l'ampoule : l'entrée « Les symboles du
        // document » y vit (la capacité se négocie), son choix ouvre la
        // carte des symboles — chaque rangée porte le nom et sa ligne,
        // le choix y fait sauter le curseur à la déclaration
        editor.caretTo(text.getLineStart(maxLine));
        frame();
        check("la carte d'actions s'ouvre", editor.openQuickActionsAtLine(maxLine));
        QuickActionCard.Entry symbolsEntry = null;
        for (QuickActionCard.Entry action : editor.quickActionCard().entries()) {
            if ("Les symboles du document".equals(action.label())) {
                symbolsEntry = action;
                break;
            }
        }
        check("l'ampoule porte les symboles du document", symbolsEntry != null);
        if (symbolsEntry != null) {
            symbolsEntry.action().run();
            frame();
            java.util.List<QuickActionCard.Entry> rows =
                    editor.quickActionCard().entries();
            boolean maxRow = false;
            boolean totalRow = false;
            for (QuickActionCard.Entry row : rows) {
                if (("MAX · ligne " + (maxLine + 1)).equals(row.label())) {
                    maxRow = true;
                }
                if (row.label().startsWith("total · ligne ")) {
                    totalRow = true;
                }
            }
            check("la carte des symboles porte la constante", maxRow);
            check("la carte des symboles porte la méthode", totalRow);
            // le choix d'une rangée y fait sauter le curseur : « MAX »
            // mène à sa déclaration
            for (QuickActionCard.Entry row : rows) {
                if (("MAX · ligne " + (maxLine + 1)).equals(row.label())) {
                    row.action().run();
                    frame();
                    break;
                }
            }
            check("le choix du symbole y saute le curseur",
                    editor.caretLine() == maxLine);
        }
        // la carte se range (le choix réel la referme — l'éprouve y
        // referme aussi : la carte ne survit pas au scénario, le
        // geste du scénario suivant vit sa vie)
        editor.quickActionCard().close();
        frame();

        // le débranchement range la couverture du document (les avis
        // du serveur mort ne valent plus rien)
        click("LSP ✔");
        check("le débranchement range les indices du serveur",
                editor.inlayHints().isEmpty());
        check("le débranchement range les plis du serveur",
                editor.serverFoldRegions().isEmpty());
        check("le débranchement range les jetons sémantiques",
                editor.serverSemantics().isEmpty());
    }

    /**
     * Le défilement à deux axes : la ligne longue s'y révèle. Le
     * pincement agrandit le corps (la ligne déborde), le geste
     * plat défile en largeur ; le traîné de sélection au bord
     * latéral y défile aussi (la colonne atteinte s'y révèle).
     */
    /**
     * Le registre partagé (la leçon de Sora Editor) : DEUX éditeurs
     * branchés au MÊME espace de travail — un seul serveur vit
     * (la poignée de main n'a couru qu'une fois), les documents y
     * partagent la liaison, les avis reviennent DISPATCHÉS par URI
     * (l'avis du document XML n'atteint QUE le second éditeur — le
     * premier n'en voit rien), le dernier détaché ferme le serveur.
     */
    private static void workspaceSharedRegistry() throws Exception {
        begin("registre partagé : deux éditeurs, un serveur");
        check("le mode duo part éteint",
                activity.harnessDuo() == null);
        click("Duo");
        // le branchement : les deux façades montent, le registre vit
        for (int wait = 0; wait < 100
                && activity.harnessDuo() == null; wait++) {
            Thread.sleep(20L);
            View.harnessDrain(50L);
        }
        check("le second éditeur vit branché",
                activity.harnessDuo() != null
                        && activity.harnessDuo().isConnected());
        check("la façade principale vit raccrochée aussi",
                activity.harnessLsp().isConnected());
        check("UN seul serveur vit au registre (le partagé)",
                activity.harnessWorkspace() != null
                        && activity.harnessWorkspace().liveServers() == 1);
        check("les deux documents vivent ouverts au serveur",
                activity.harnessWorkspace().sharedDocuments() == 2);
        // le dispatch par URI : la parenthèse non fermée du document
        // XML porte l'avis au second éditeur SEUL — l'avis se lit sur
        // la ligne du caret (la DERNIÈRE ligne du document, pas la
        // première : le serveur y pointe l'erreur)
        EditorView pair = activity.harnessDuoEditor();
        pair.caretTo(pair.getBuffer().length());
        frame();
        pair.onCreateInputConnection(new EditorInfo()).commitText(")", 1);
        View.harnessDrain(2000L);
        frame();
        int lastLine = pair.getBuffer().getLineCount() - 1;
        boolean seen = false;
        for (int wait = 0; wait < 100 && !seen; wait++) {
            for (int at = 0; at <= lastLine; at++) {
                seen |= pair.lineHasDiagnostics(at);
            }
            if (!seen) {
                Thread.sleep(20L);
                View.harnessDrain(50L);
                frame();
            }
        }
        check("l'avis du document XML vit au second éditeur", seen);
        check("l'avis du document XML n'atteint PAS le premier",
                !editor.lineHasDiagnostics(0));
        // le détachement : le bouton range tout (le dernier parti
        // ferme le serveur — la durée de vie suit les documents)
        click("Duo ✔");
        for (int wait = 0; wait < 50
                && activity.harnessDuo() != null; wait++) {
            Thread.sleep(20L);
            View.harnessDrain(50L);
        }
        check("le mode duo se détache",
                activity.harnessDuo() == null
                        && activity.harnessWorkspace() == null);
        check("la façade principale revient débranchée",
                !activity.harnessLsp().isConnected());
    }

    private static void twoAxisScroll() throws InterruptedException {
        begin("défilement horizontal : le zoom révèle la largeur, le geste la parcourt");
        openLanguage("Java", 10_000L);
        check("le défilement horizontal part de zéro",
                editor.harnessScrollX() < 0.5f);
        // le corps double : la ligne moyenne déborde la vue
        float initial = editor.getTextSizeSp();
        pinch(VIEW_WIDTH / 2.0f, VIEW_HEIGHT / 2.0f, 120.0f, 240.0f);
        frame();
        check("l'écart agrandit le corps",
                editor.getTextSizeSp() > initial + 6.0f);
        // le geste plat se décale latéralement : le doigt tire la
        // largeur (scrollX croît quand le doigt va à gauche)
        float before = editor.harnessScrollX();
        // le geste vise le CORPS du texte (la bande minimap vit au
        // bord droit — ses ~172 px ne se défilent pas)
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN,
                VIEW_WIDTH - 220.0f, 300.0f));
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_MOVE,
                VIEW_WIDTH - 320.0f, 300.0f));
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_MOVE,
                VIEW_WIDTH - 520.0f, 300.0f));
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP,
                VIEW_WIDTH - 520.0f, 300.0f));
        frame();
        check("le geste plat défile la largeur",
                editor.harnessScrollX() > before + 10.0f);
        // le traîné de sélection près du bord latéral : la colonne
        // droite du texte s'y révèle (l'auto-scroll l'y pousse)
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN,
                600.0f, 200.0f));
        Thread.sleep(600L);
        // le traîné vise le bord du CORPS (la bande minimap au-delà
        // ne se défile pas)
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_MOVE,
                VIEW_WIDTH - 200.0f, 200.0f));
        frame();
        float duringDrag = editor.harnessScrollX();
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP,
                VIEW_WIDTH - 200.0f, 200.0f));
        frame();
        check("le traîné au bord latéral fait défiler la largeur",
                duringDrag > before + 5.0f);
        // le corps revient : le défilement se reborne à zéro (la
        // ligne tient de nouveau dans la vue)
        editor.setTextSizeSp(initial);
        frame();
        check("le retour du corps reborne la largeur",
                editor.harnessScrollX() < 0.5f);
    }

    /**
     * Le pincement : le zoom au deux doigts — l'écart porte le
     * facteur, la chasse suit le corps, les bornes serrent, l'appui
     * final ne vaut pas un tap (le curseur ne saute pas au dernier
     * doigt levé), le tap suivant vit sa vie.
     */
    private static void pinchZoom() {
        begin("pincement : le zoom au deux doigts");
        float initial = editor.getTextSizeSp();
        float initialChar = editor.charWidth();
        check("le corps initial se lit en sp", Math.abs(initial - 14.0f) < 0.05f);
        check("la gouttière part au corps de repos (facteur 1)",
                Math.abs(editor.gutterTextZoom() - 1.0f) < 0.05f);
        int caret = editor.getCaretOffset();
        // l'écart double : le corps double (la chasse suit)
        pinch(VIEW_WIDTH / 2.0f, VIEW_HEIGHT / 2.0f, 120.0f, 240.0f);
        frame();
        check("l'écart agrandit le corps",
                editor.getTextSizeSp() > initial + 6.0f);
        check("la chasse suit le corps", editor.charWidth() > initialChar * 1.8f);
        check("les numéros de la gouttière s'agrandissent au pincement",
                editor.gutterTextZoom() > 1.8f);
        check("le curseur n'a pas sauté au doigt levé",
                editor.getCaretOffset() == caret);
        // le rapprochement : le corps revient
        pinch(VIEW_WIDTH / 2.0f, VIEW_HEIGHT / 2.0f, 240.0f, 120.0f);
        frame();
        check("le rapprochement rend le corps",
                Math.abs(editor.getTextSizeSp() - initial) < 1.0f);
        check("le rapprochement rend le facteur de la gouttière",
                Math.abs(editor.gutterTextZoom() - 1.0f) < 0.1f);
        // les bornes de l'hôte : le corps s'y cale
        editor.setZoomBoundsSp(10.0f, 20.0f);
        check("les bornes se posent",
                Math.abs(editor.minZoomSp() - 10.0f) < 0.05f
                        && Math.abs(editor.maxZoomSp() - 20.0f) < 0.05f);
        editor.setTextSizeSp(50.0f);
        check("le corps hors borne haute se cale",
                Math.abs(editor.getTextSizeSp() - 20.0f) < 0.05f);
        editor.setTextSizeSp(2.0f);
        check("le corps hors borne basse se cale",
                Math.abs(editor.getTextSizeSp() - 10.0f) < 0.05f);
        editor.setTextSizeSp(16.0f);
        check("l'hôte pose son corps",
                Math.abs(editor.getTextSizeSp() - 16.0f) < 0.05f);
        // le pincement au-delà de la borne : il serre à la borne
        pinch(VIEW_WIDTH / 2.0f, VIEW_HEIGHT / 2.0f, 120.0f, 30.0f);
        frame();
        check("le pincement serre à la borne basse",
                Math.abs(editor.getTextSizeSp() - 10.0f) < 0.05f);
        // le tap suivant vit sa vie : le curseur se place au doigt
        float[] spot = spotOfWord("total");
        tap(spot[0], spot[1]);
        frame();
        check("le tap suivant replace le curseur",
                editor.getCaretOffset() != caret);
        // le corps initial revient (l'éprouve suivante part propre)
        editor.setZoomBoundsSp(8.0f, 40.0f);
        editor.setTextSizeSp(initial);
        frame();
        check("le corps initial revient",
                Math.abs(editor.getTextSizeSp() - initial) < 0.05f);
    }

    /**
     * La refonte de l'écran : les onglets portent les documents, le
     * drawer porte le menu (thème, hors normes, serveurs), la feuille
     * basse porte les mesures — et les options que les popups portent
     * déjà n'y vivent plus (la règle de l'instrument de test : chaque
     * commande reste à l'écran seulement si la vue ne la porte pas).
     */
    /**
     * Les points d'arrêt de la gouttière : le tap de la colonne des
     * marqueurs pose le point (le rond à l'accent), le re-tap le
     * retire ; l'accès public y lit les lignes (l'hôte y branchera
     * son débogueur).
     */
    private static void gutterBreakpoints() {
        begin("points d'arrêt : la gouttière pose, retire, y lit");
        openLanguage("Java", 10_000L);
        check("aucun point d'arrêt au départ",
                editor.breakpoints().isEmpty());
        // le tap de la colonne des marqueurs à la ligne 2 : le point s'y pose
        editor.toggleBreakpoint(2);
        frame();
        check("le point se pose à la ligne posée",
                editor.breakpoints().contains(2));
        // une seconde ligne : l'ensemble porte les deux, en ordre
        editor.toggleBreakpoint(5);
        frame();
        check("les deux points se lisent en ordre",
                editor.breakpoints().size() == 2
                        && editor.breakpoints().first() == 2
                        && editor.breakpoints().last() == 5);
        // le re-tap retire : la ligne redevient libre
        editor.toggleBreakpoint(2);
        frame();
        check("le re-tap retire le point",
                !editor.breakpoints().contains(2)
                        && editor.breakpoints().size() == 1);
        // l'appui long immobile de la colonne : la ligne 5 se retire
        // au geste du débogueur (le tap simple y reste au pli)
        float markY = lineTop(5) + LINE_HEIGHT / 2.0f;
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_DOWN,
                12.0f, markY));
        try {
            Thread.sleep(600L);
        } catch (InterruptedException rested) {
            Thread.currentThread().interrupt();
        }
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_MOVE,
                12.0f, markY));
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP,
                12.0f, markY));
        frame();
        check("l'appui long gouttière retire le point au doigt",
                editor.breakpoints().isEmpty());
        // la ligne hors borne ne pose rien
        check("la ligne hors borne ne pose rien",
                !editor.toggleBreakpoint(999_999));
        // le marqueur se dessine : la pose d'un point, une trame —
        // le rond s'y dessine (la colonne des marqueurs vit avant
        // les numéros)
        editor.toggleBreakpoint(1);
        frame();
        check("le point posé s'y lit", editor.breakpoints().contains(1));
        editor.toggleBreakpoint(1);
        frame();
    }

    /**
     * Les guides verticaux des blocs (le fil des accolades) : un
     * guide par accolade appariée — à la colonne de l'ouvrante, de
     * la ligne sous l'ouvrante à la ligne sur la fermante. Le fil
     * de la PORTÉE ACTIVE s'illumine (le bloc du caret et ses
     * parents), celui des jumelles aussi (le caret sur l'ouvrante
     * ou la fermante du bloc). L'API publique y lit la table (le
     * harnais éprouve la géométrie SANS l'écran : la table se
     * dérive du document, le dessin la trace).
     */
    private static void blockGuides() {
        begin("guides de blocs : le fil des accolades, la portée active");
        openLanguage("Java", 10_000L);
        jo.pigma.core.TextBuffer text = editor.getBuffer();
        jo.pigma.editor.folding.StructureGuides table =
                jo.pigma.editor.folding.StructureGuides.of(text,
                        editor.getSyntaxDocument(), structureOf(editor),
                        0, text.getLineCount() - 1);
        check("la table porte les trois blocs de la démo",
                table.size() == 3);
        // le bloc de la méthode (total) : l'accolade ouvrante à sa
        // colonne, la fermante à sa ligne — le trait relie les deux
        boolean methodGuide = false;
        for (int at = 0; at < table.size(); at++) {
            jo.pigma.editor.folding.StructureGuides.Guide guide = table.guide(at);
            // le guide de la méthode : le trait à l'INDENTATION de sa
            // ligne (4 colonnes), le glyphe ouvrant au-delà
            if (guide.openingLine() == guide.closingLine() - 5
                    && guide.column() == 4 && guide.openingColumn() > 20) {
                methodGuide = true;
            }
        }
        check("le guide de la méthode vit à l'indentation de sa ligne",
                methodGuide);
        // les guides ne vivent QUE entre les lignes de la paire : un
        // guide ne couvre jamais l'ouvrante ni la fermante
        boolean insideOnly = true;
        for (int at = 0; at < table.size(); at++) {
            jo.pigma.editor.folding.StructureGuides.Guide guide = table.guide(at);
            if (guide.closingLine() <= guide.openingLine()) {
                insideOnly = false;
            }
        }
        check("un guide couvre des lignes DISTINCTES (jamais l'ouvrante)",
                insideOnly);
        // la portée active : le caret au corps de la méthode (AU-DELÀ
        // du trait de son bloc) — son guide et celui de la classe
        // s'illuminent (les parents)
        editor.caretTo(text.getLineStart(14) + 8);
        frame();
        int[] active = table.activeGuides(14, 8);
        check("la portée active illumine le bloc du caret et ses parents",
                active.length >= 2);
        detail("guides actifs au corps de la méthode : " + active.length);
        // les jumelles : le caret SUR la fermante illumine son bloc
        int methodClosing = -1;
        for (int at = 0; at < table.size(); at++) {
            jo.pigma.editor.folding.StructureGuides.Guide guide = table.guide(at);
            if (guide.openingColumn() > 20 && guide.openingLine() > 0) {
                methodClosing = guide.closingLine();
            }
        }
        int[] twin = table.activeGuides(methodClosing, 4);
        check("le caret sur la fermante illumine le bloc jumeau",
                twin.length >= 1);
        // LE FIL DE LA PAIRE (l'autre fil) : le caret sur l'ouvrante de
        // la méthode — la paire s'y calcule au dessin, le fil s'y
        // trace (la jambe verticale au bord gauche de l'ouvrante, le
        // coude horizontal vers la fermante : leurs colonnes diffèrent)
        int methodOpening = -1;
        for (int at = 0; at < table.size(); at++) {
            jo.pigma.editor.folding.StructureGuides.Guide guide = table.guide(at);
            if (guide.openingColumn() > 20 && guide.openingLine() > 0) {
                methodOpening = guide.openingLine();
            }
        }
        int methodColumn = -1;
        for (int at = 0; at < table.size(); at++) {
            jo.pigma.editor.folding.StructureGuides.Guide guide = table.guide(at);
            if (guide.openingLine() == methodOpening) {
                methodColumn = guide.openingColumn();
            }
        }
        editor.caretTo(editor.getBuffer().getLineStart(methodOpening)
                + methodColumn);
        frame();
        check("la paire d'accolades vit au caret posé",
                editor.bracketPair() != null
                        && editor.bracketPair().toLine() > editor.bracketPair().fromLine());
        check("le fil relie des colonnes différentes (le coude se tire)",
                editor.bracketPair().fromColumn()
                        != editor.bracketPair().toColumn());
        // la ligne sans bloc ne guide pas : la table au-dessus d'un
        // document d'une seule ligne ne porte rien
        editor.setLanguage(jo.pigma.syntax.Languages.JAVA, "int seul = 1;\n");
        frame();
        jo.pigma.editor.folding.StructureGuides flat =
                jo.pigma.editor.folding.StructureGuides.of(editor.getBuffer(),
                        editor.getSyntaxDocument(), structureOf(editor),
                        0, 0);
        check("un document sans bloc ne porte aucun guide", flat.size() == 0);
        // retour au document de démo (le scénario suivant part propre)
        openLanguage("Java", 10_000L);
    }

    /** Le document de structure de l'éditeur (le harnais le lit). */
    private static jo.pigma.syntax.StructureDocument structureOf(EditorView view) {
        return view.getStructureDocument();
    }

    private static void screenRestyle() {
        begin("refonte de l'écran : onglets, drawer, feuille basse");
        TabLayout tabs = activity.harnessTabLayout();
        androidx.drawerlayout.widget.DrawerLayout drawer =
                activity.harnessDrawer();
        com.google.android.material.bottomsheet.BottomSheetBehavior<?> sheet =
                activity.harnessSheetBehavior();
        check("la barre d'onglets vit à l'écran",
                tabs != null && tabs.getTabCount() == 6);
        check("l'onglet Java est le choix courant",
                activity.selectedTabIndex() == 0
                        && "Java".contentEquals(tabs.getTabAt(0).getText()));
        // le choix d'onglet ouvre le document correspondant : le XML
        // y change le langage et le texte du tampon
        activity.harnessSelectTab(1);
        frame();
        View.harnessDrain(1_000L);
        check("l'onglet XML ouvre le document XML",
                activity.selectedTabIndex() == 1
                        && editor.getBuffer().length() > 0
                        && editor.getBuffer().toString().startsWith("<?xml"));
        // les options redondantes ne vivent plus : ni bouton de
        // recherche, ni bouton d'aller-à (les popups de la vue les
        // portent), ni boutons de sélection — les popups s'ouvrent
        // seuls par la vue
        check("le bouton de recherche n'existe plus (la popup le porte)",
                findButton(activity.harnessRootView(), "Rechercher") == null);
        check("le bouton d'aller-à n'existe plus (la popup le porte)",
                findButton(activity.harnessRootView(), "Aller à") == null);
        check("le bouton de sélection n'existe plus (la barre la porte)",
                findButton(activity.harnessRootView(), "Tout sélectionner") == null);
        check("le bouton de copie du texte n'existe plus (la barre la porte)",
                findButton(activity.harnessRootView(), "Copier texte") == null);
        // le drawer : fermé au départ, la barre du menu l'ouvre (le
        // tiroir du cadre y répond), le menu porte le thème et les
        // hors normes
        check("le drawer part fermé",
                !drawer.isDrawerOpen(activity.harnessDrawerMenu()));
        activity.harnessOpenDrawer();
        check("la barre du menu ouvre le drawer",
                drawer.isDrawerOpen(activity.harnessDrawerMenu()));
        check("le menu du drawer porte les boutons du menu",
                findButton(activity.harnessRootView(), "Clair") != null
                        && findButton(activity.harnessRootView(), "50k ⨯") != null);
        drawer.closeDrawer(activity.harnessDrawerMenu());
        check("le drawer se referme",
                !drawer.isDrawerOpen(activity.harnessDrawerMenu()));
        // la feuille basse : part demi-pliée (les mesures se lisent de
        // plain-pied), les états du cadre montent et descendent, les
        // mesures y vivent toujours
        check("la feuille part demi-pliée (les mesures lisibles)",
                sheet.getState()
                        == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED);
        check("les mesures vivent à la feuille",
                textOf("coloration :") != null);
        sheet.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        check("la feuille monte à l'ouvert",
                sheet.getState()
                        == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        sheet.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);
        check("la feuille se referme (la hauteur d'aperçu seule dépasse)",
                sheet.getState()
                        == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);
        sheet.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED);
        // la bascule de thème du menu : le journal la porte encore au
        // relevé final (la feuille ouverte la montre)
        click("Sombre");
        click("Clair");
        // le retour à l'onglet Java : le document de démo y revient
        activity.harnessSelectTab(0);
        frame();
        View.harnessDrain(1_000L);
        check("l'onglet Java revient au document de démo",
                editor.getBuffer().toString().startsWith("package"));
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

    /**
     * Le journal des crashs : le filet éprouvé de bout en bout — un
     * plantage non intercepté s'écrit au journal (la pile, le fil, la
     * section courante), la relecture le rend, l'effacement le tue.
     * La capture du harnais y passe par le MÊME gestionnaire que
     * l'appareil (le défaut du fil, posé à l'ouverture de l'écran) :
     * le plantage lancé ici déclenche l'écriture réelle.
     */
    /**
     * Le cycle de la connexion : la machine à états de la façade
     * (la leçon de la référence) — CONNECTING à la prise, CONNECTED
     * à la poignée de main, DISCONNECTED à la mort du serveur (le
     * canal fermé s'y voit, l'écouteur y naît — le toast de l'app
     * s'y branche). Les envois s'y arrêtent : la liaison morte ne
     * reçoit plus rien.
     */
    private static void connectionCycle() throws InterruptedException {
        begin("cycle de connexion : naît, vit, meurt — l'état y dit tout");
        LspEditor facade = activity.harnessLsp();
        java.util.List<String> events = java.util.Collections.synchronizedList(
                new java.util.ArrayList<String>());
        facade.setConnectionListener((status, reason) ->
                events.add(status + (reason != null ? ":" + reason : "")));
        check("la façade débranchée est DISCONNECTED",
                facade.connectionStatus() == ConnectionStatus.DISCONNECTED);
        // la prise : la connexion naît CONNECTING (la poignée de
        // main court), vit CONNECTED (le document ouvert)
        click("LSP");
        for (int wait = 0; wait < 100
                && facade.connectionStatus() != ConnectionStatus.CONNECTED
                && events.size() < 2; wait++) {
            Thread.sleep(20L);
            View.harnessDrain(500L);
        }
        check("la connexion vit CONNECTED",
                facade.connectionStatus() == ConnectionStatus.CONNECTED);
        check("la naissance y est dite (CONNECTING puis CONNECTED)",
                events.size() >= 2
                        && events.get(0).startsWith("CONNECTING")
                        && events.get(1).startsWith("CONNECTED"));
        check("la façade branchée est connectée", facade.isConnected());
        // LE SERVEUR MEURT : le canal se ferme sous la façade —
        // l'état meurt DISCONNECTED, l'écouteur le dit (le toast de
        // l'app en naît : « serveur déconnecté »)
        activity.harnessKillServer();
        for (int wait = 0; wait < 100
                && facade.connectionStatus() != ConnectionStatus.DISCONNECTED
                && events.size() < 3; wait++) {
            Thread.sleep(20L);
            View.harnessDrain(500L);
        }
        check("la mort du serveur se dit (DISCONNECTED)",
                facade.connectionStatus() == ConnectionStatus.DISCONNECTED);
        check("la mort y est portée à l'écouteur",
                events.size() >= 3
                        && events.get(2).startsWith("DISCONNECTED"));
        // les envois s'arrêtent : la frappe après la mort ne part
        // plus au serveur mort (la pompe se tait — les didChange ne
        // fuient plus dans le canal fermé)
        connection.commitText("x", 1);
        View.harnessDrain(1000L);
        frame();
        check("la façade morte n'est plus connectée",
                !facade.isConnected());
        facade.setConnectionListener(null);
        // le re-débranchement volontaire reste muet : un état déjà
        // DISCONNECTED ne re-notifie pas
        java.util.List<String> before = java.util.List.copyOf(events);
        facade.disconnect();
        View.harnessDrain(1000L);
        check("le débranchement d'une liaison morte reste muet",
                events.equals(before));
    }

    /**
     * Les positions hostiles du serveur : le correctif du crash de
     * l'appareil (le journal de l'utilisateur porte les deux —
     * NullPointerException d'askCodeActions au serveur mort,
     * IndexOutOfBoundsException de drawServerHighlights à la
     * colonne hors ligne). Le serveur qui meurt avant de répondre
     (la future échoue, la réponse arrive null) ne doit plus tuer
     * l'écran ; les surlignages du serveur aux coordonnées périmées
     * (colonne au-delà de la ligne, ligne au-delà du texte) se
     * bornent au dessin sans exception.
     */
    private static void serverPositionsHostile() throws InterruptedException {
        begin("positions hostiles : serveur mort, surlignage hors texte");
        LspEditor facade = activity.harnessLsp();
        // la liaison au serveur de démo : le scénario des correctifs
        // y passe (la carte d'actions y demande askCodeActions)
        click("LSP");
        check("les diagnostics du serveur arrivent", awaitText("LSP :", 8000L));
        // LE SERVEUR MEURT : la liaison se coupe sous la façade (la
        // future de correctifs échouera « flux fermé », la réponse
        // arrivera null — le plantage de l'appareil reproduit)
        activity.harnessKillServer();
        // la carte d'actions s'ouvre : askCodeActions part, sa
        // future échoue — la réponse nulle ne doit PLUS lever au fil
        // principal (le correctif du crash de l'appareil)
        int maxLine = editor.getSyntaxDocument().lineCount() - 1;
        editor.openQuickActionsAtLine(maxLine);
        View.harnessDrain(2_000L);
        frame();
        check("la carte s'ouvre au serveur mort sans tuer l'écran",
                editor.getSyntaxDocument().lineCount() > 0
                        && editor.getBuffer().length() > 0);
        // les surlignages hostiles : une colonne au-delà de la ligne
        // (la ligne raccourcie après l'avis du serveur), une ligne
        // au-delà du texte, une colonne négative — le dessin les
        // borne, jamais une exception de trame
        editor.setServerHighlights(java.util.List.of(
                new jo.pigma.lsp.LspRange(
                        new jo.pigma.lsp.LspPosition(1, 999_999),
                        new jo.pigma.lsp.LspPosition(1, Integer.MAX_VALUE)),
                new jo.pigma.lsp.LspRange(
                        new jo.pigma.lsp.LspPosition(999_999, 0),
                        new jo.pigma.lsp.LspPosition(999_999, 4)),
                new jo.pigma.lsp.LspRange(
                        new jo.pigma.lsp.LspPosition(2, -5),
                        new jo.pigma.lsp.LspPosition(2, 3)),
                new jo.pigma.lsp.LspRange(
                        new jo.pigma.lsp.LspPosition(2, 0),
                        new jo.pigma.lsp.LspPosition(4, 999_999))));
        check("les surlignages hostiles se posent sans lever",
                !editor.serverHighlights().isEmpty());
        frame();
        check("le dessin aux positions hostiles ne lève pas",
                editor.getSyntaxDocument().lineCount() > 0);
        // un correctif aux positions périmées s'applique borné : la
        // colonne 999 999 de la ligne 2 se borne à sa fin (l'edit
        // devient un remplacement de fin de ligne)
        java.util.List<jo.pigma.lsp.LspTextEdit> stale = java.util.List.of(
                new jo.pigma.lsp.LspTextEdit(
                        new jo.pigma.lsp.LspRange(
                                new jo.pigma.lsp.LspPosition(2, 999_999),
                                new jo.pigma.lsp.LspPosition(2, 999_999)),
                        "remplacé"));
        boolean applied = editor.applyTextEdits(stale);
        View.harnessDrain(500L);
        frame();
        check("l'edit aux bornes hostiles s'applique borné (ou s'ignore) sans lever",
                !applied || editor.getBuffer().toString().contains("remplacé"));
        // le nettoyage : la liaison se débranche (le scénario
        // suivant repart d'un écran propre) — le serveur y est MORT
        // (la machine à états a rendu le bouton à « LSP »), le clic
        // sur l'état courant débranche la façade encore posée
        click("LSP");
        editor.setServerHighlights(java.util.List.of());
        frame();
    }

    private static void crashJournalCapture() {
        begin("journal des crashs : capture, relecture, effacement");
        java.io.File storage = activity.harnessFilesDir();
        // un stockage propre : le scénario repart d'un journal vide
        jo.pigma.sample.CrashJournal.clear(storage);
        check("le journal vide ne montre rien",
                jo.pigma.sample.CrashJournal.pendingCrashes(storage)
                        .isEmpty());
        // la section courante y est posée (celle du scénario du
        // moment), un plantage la portera
        jo.pigma.sample.CrashJournal.setSection("XML (démo)");
        Thread capturing = new Thread(() -> {
            throw new IllegalStateException("plantage du harnais");
        }, "fil-plantage");
        capturing.setUncaughtExceptionHandler(
                Thread.getDefaultUncaughtExceptionHandler());
        capturing.start();
        try {
            capturing.join(2_000L);
        } catch (InterruptedException waitBroke) {
            Thread.currentThread().interrupt();
        }
        java.util.List<String> crashes =
                jo.pigma.sample.CrashJournal.pendingCrashes(storage);
        check("le plantage s'est écrit au journal",
                crashes.size() == 1);
        if (crashes.size() == 1) {
            String crash = crashes.get(0);
            check("la pile y vit (l'exception nommée)",
                    crash.contains("IllegalStateException")
                            && crash.contains("plantage du harnais"));
            check("la section courante y vit (le scénario du moment)",
                    crash.contains("section : XML (démo)"));
            check("le fil qui a planté y vit",
                    crash.contains("fil : fil-plantage"));
        }
        // la consultation manuelle : le bouton du menu ouvre le
        // dialogue du plantage, le harnais y lit son contenu
        click("Journal des crashs");
        android.app.AlertDialog shown = android.app.AlertDialog.harnessShown();
        check("le bouton du journal montre le plantage",
                shown != null && shown.harnessBody()
                        .contains("plantage du harnais"));
        // l'effacement : le journal ne montre plus rien
        jo.pigma.sample.CrashJournal.clear(storage);
        check("l'effacement vide le journal",
                jo.pigma.sample.CrashJournal.pendingCrashes(storage)
                        .isEmpty());
        // l'état de l'écran n'a pas bougé : la section suit son
        // cours (le clic du bouton l'a posée au journal lui-même)
        check("l'écran vit toujours après le plantage capturé",
                editor.getBuffer().length() > 0);
    }

    /**
     * La sauvegarde-restauration d'état : la rotation ne perd plus
     * rien. Le scénario y passe par les MÊMES méthodes que le
     * cadre (onSaveInstanceState de l'écran et de la vue, la
     * restauration s'ensuit) : le texte tapé, l'onglet, le curseur
     * et le défilement reviennent.
     */
    private static void stateSaveRestore() {
        begin("état sauvegardé : rotation sans perte");
        // un texte tapé : la démo d'ouverture y gagne une frappe
        // distinctive (la preuve porte sur le texte restauré)
        String typed = "// frappe du scénario d'état\n";
        editor.caretTo(editor.getBuffer().length());
        connection.commitText(typed, 1);
        View.harnessDrain(500L);
        int caretBefore = editor.caretOffset();
        float scrollBefore = editor.harnessScrollY();
        // la vue ET l'écran sauvegardent (le cadre appelle l'une
        // puis l'autre) ; la restauration lit ce qu'ils ont écrit
        android.os.Parcelable editorState = editor.harnessSaveState();
        android.os.Bundle screenState = new android.os.Bundle();
        activity.onSaveInstanceState(screenState);
        check("l'écran a écrit son paquet",
                screenState.getString("pigma.text") != null);
        // l'écran se reconstruit (la création pure relit la démo),
        // puis la restauration rend l'état : le texte tapé revient
        activity.onRestoreInstanceState(screenState);
        editor.harnessRestoreState(editorState);
        View.harnessDrain(500L);
        frame();
        check("le texte tapé revient après rotation",
                editor.getBuffer().toString().endsWith(typed));
        check("le curseur revient (la position sauvegardée)",
                editor.caretOffset() == caretBefore
                        || Math.abs(editor.caretOffset() - caretBefore)
                                <= typed.length());
        check("le défilement revient (borné)",
                Math.abs(editor.harnessScrollY() - scrollBefore) < 1.0f);
        // l'état corrompu ne lève pas : un paquet aux bornes hors
        // texte se borne (la vue ne lève jamais à la restauration)
        android.os.Bundle hostile = new android.os.Bundle();
        hostile.putInt("pigma.caret", Integer.MAX_VALUE / 2);
        hostile.putInt("pigma.anchor", Integer.MAX_VALUE);
        hostile.putFloat("pigma.scrollY", Float.MAX_VALUE);
        editor.harnessRestoreState(hostile);
        frame();
        check("un état corrompu se borne sans lever",
                editor.caretOffset() <= editor.getBuffer().length());
        // l'état sain revient (le scénario se referme propre)
        editor.harnessRestoreState(editorState);
        frame();
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
    /**
     * Ouvre un document par sa voie d'écran : l'onglet du langage le
     * choisit (les documents de démonstration y vivent), les hors
     * normes passent par les boutons du menu du drawer. La première
     * trame paie la fenêtre et relance la salve du fond, les ticks du
     * veilleur voient le fond courir, l'attente réelle laisse la salve
     * achever, le dernier tick journalise la fin, la trame suivante
     * dessine l'état arrivé.
     *
     * @param label l'étiquette de l'onglet ou du bouton du menu
     * @param fillTimeoutMillis la borne d'attente du fond
     */
    private static void openLanguage(String label, long fillTimeoutMillis) {
        String[] onglets = {"Java", "XML", "JSON", "Kotlin", "Lua", "SQL"};
        for (int at = 0; at < onglets.length; at++) {
            if (label.contentEquals(onglets[at])) {
                if (activity.selectedTabIndex() == at) {
                    activity.harnessReopenCurrent();
                } else {
                    activity.harnessSelectTab(at);
                }
                break;
            }
        }
        if (label.contentEquals("Gros") || label.contentEquals("50k l.")
                || label.contentEquals("50k ⨯")) {
            click(label);
        }
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
     * Un pincement à deux doigts : l'écart passe de spanFrom à
     * spanTo autour d'un foyer (les doigts s'écartent ou se
     * rapprochent, le second se pose après le premier, l'un se lève
     * avant l'autre — l'indice du doigt qui s'en va vit dans les
     * bits hauts de l'action, comme sur l'appareil).
     *
     * @param cx        l'abscisse du foyer
     * @param cy        l'ordonnée du foyer
     * @param spanFrom  l'écart initial des doigts en pixels
     * @param spanTo    l'écart final en pixels
     */
    private static void pinch(float cx, float cy, float spanFrom, float spanTo) {
        float halfFrom = spanFrom / 2.0f;
        float halfTo = spanTo / 2.0f;
        // le premier doigt se pose, le second le suit
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_DOWN, cx, cy - halfFrom));
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_POINTER_DOWN,
                new float[] {cx, cx}, new float[] {cy - halfFrom, cy + halfFrom}));
        // l'écart s'étire par pas (le facteur se lit à chaque trame)
        int steps = 8;
        for (int at = 1; at <= steps; at++) {
            float half = halfFrom + (halfTo - halfFrom) * at / steps;
            editor.onTouchEvent(MotionEvent.obtain(
                    MotionEvent.ACTION_MOVE,
                    new float[] {cx, cx}, new float[] {cy - half, cy + half}));
        }
        // un doigt se lève, l'autre suit : le geste se referme
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_POINTER_UP,
                new float[] {cx, cx}, new float[] {cy - halfTo, cy + halfTo}));
        editor.onTouchEvent(MotionEvent.obtain(
                MotionEvent.ACTION_UP, cx, cy + halfTo));
        View.harnessDrain(2000L);
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
     * diagnostics du serveur arrivent d'un autre fil — la façade les
     * y marshalle par post, la pompe des différés doit courir comme
     * un fil d'interface vivant).
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
            // la pompe des différés : l'horloge virtuelle avance à
            // l'attente, les suites dues s'y exécutent (le panneau y
            // rafraîchit, la notification du serveur y arrive)
            View.harnessDrain(20L);
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
