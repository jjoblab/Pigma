package jo.pigma.sample;

import android.graphics.Canvas;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import jo.pigma.core.TextBuffer;
import jo.pigma.editor.EditorView;
import jo.pigma.syntax.SyntaxDocument;

/**
 * Harnais d'ouverture : éprouve l'application de démonstration sur
 * machine virtuelle, sans appareil — la création, le premier dessin,
 * le remplissage du fond, les langages, le thème, la frappe et le
 * clavier par événements, la sélection et ses poignées, l'appui long
 * qui glisse, la recherche et le remplacement, le fichier hors
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
        bigFileAndStress();
        selectAllAndCopyText();
        doubleTapWord();
        handleDrag();
        loupe();
        longPressDrag();
        dragDropText();
        searchAndReplace();
        hugeFile();
        hugeFileCopyGesture();
        replaceAllHugeFile();
        folds();
        semanticFusion();
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
                copied != null && "sélection Pigma".contentEquals(copied.getLabel())
                        && copied.getText().length() == length);
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
        begin("loupe : le texte de la borne se dessine agrandi");
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
        String magnifiedText = null;
        for (Canvas.TextOp op : canvas.harnessTextsSinceMark()) {
            if (op.scaleX > 1.5f) {
                magnifiedText = op.text;
            }
        }
        check("le texte de la ligne de la borne se dessine agrandi",
                magnifiedText != null);
        boolean bounded = false;
        for (Canvas.ClipOp op : canvas.harnessClipsSinceMark()) {
            if (op.right - op.left < VIEW_WIDTH / 2.0f) {
                bounded = true;
            }
        }
        check("la loupe découpe une fenêtre bornée", bounded);
        check("le cadre arrondi se dessine", canvas.harnessRoundRectCount() >= 1);
        detail(magnifiedText == null
                ? "aucun texte agrandi" : "agrandi : « " + magnifiedText + " »");
        editor.onTouchEvent(MotionEvent.obtain(MotionEvent.ACTION_UP, handleX, lineMidY));
        frame();
        boolean closed = true;
        for (Canvas.TextOp op : canvas.harnessTextsSinceMark()) {
            if (op.scaleX > 1.5f) {
                closed = false;
            }
        }
        check("la loupe se ferme au lâcher", closed);
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
        // ligne cible change au dépôt (elle reçoit le texte)
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

    /** La feuille de recherche : chercher, naviguer, remplacer, tout. */
    private static void searchAndReplace() {
        begin("recherche : compter, naviguer, remplacer, tout remplacer");
        click("Rechercher");
        View sheet = findButton(activity.harnessRootView(), "Chercher")
                .harnessParent().harnessParent();
        check("la feuille s'ouvre", sheet.getVisibility() == View.VISIBLE);
        String text = editor.getBuffer().toString();
        int expected = countOccurrences(text, "graine");
        // la recherche incrémentale : le champ tape lettre à lettre,
        // chaque frappe relance (amortie) — le compte suit la frappe
        android.widget.EditText query = findField("chercher…");
        query.setText("g");
        View.harnessDrain(400L);
        check("la recherche suit la première frappe (g)",
                editor.searchMatchCount() == countOccurrences(text, "g"));
        query.setText("gr");
        View.harnessDrain(400L);
        check("la recherche suit la deuxième frappe (gr)",
                editor.searchMatchCount() == countOccurrences(text, "gr"));
        query.setText("graine");
        View.harnessDrain(400L);
        check("le compte exact des correspondances est arrivé à la frappe finale",
                editor.searchMatchCount() == expected);
        check("la feuille porte le compte",
                textOf("correspondance") != null);
        // la recherche incrémentale a navigué en tapant (la
        // correspondance la plus proche du curseur) : l'index de départ
        // n'est plus zéro — l'avance et le recul se vérifient relatifs
        int beforeNext = editor.searchCurrentIndex();
        click("›");
        check("la navigation avance", editor.searchCurrentIndex() == beforeNext + 1);
        click("‹");
        check("la navigation recule", editor.searchCurrentIndex() == beforeNext);
        findField("remplacer par…").setText("GRAINE");
        click("Remplacer");
        check("un remplacement a eu lieu",
                countOccurrences(editor.getBuffer().toString(), "graine") == expected - 1);
        click("Annuler");
        check("l'annulation rend le texte",
                editor.getBuffer().toString().equals(text));
        click("Chercher");
        click("Tout");
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
        click("✕");
        check("la feuille se ferme et la recherche se range",
                sheet.getVisibility() == View.GONE && editor.searchMatchCount() == 0);
        detail(expected + " correspondances de « graine »");
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
        android.widget.EditText query = findField("chercher…");
        query.setText("graine");
        View.harnessDrain(400L);
        int matches = editor.searchMatchCount();
        check("la recherche plafonne à dix mille correspondances",
                matches == 10_000);
        findField("remplacer par…").setText("GRAINE");
        long startedAt = System.nanoTime();
        click("Tout");
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
        View.harnessDrain(2000L);
        String hover = editor.infoPopupText();
        check("le survol du serveur prime dans la popup",
                hover != null && hover.contains("classe"));
        detail("survol : " + hover);
        click("Annuler");
        click("Annuler");
        check("la double annulation rend le texte",
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
        CharSequence clip = android.content.Context.harnessClipboard().getPrimaryClip().getText();
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
        Button button = findButton(activity.harnessRootView(), label);
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
     * Cherche un champ de saisie par son indication.
     *
     * @param hint l'indication du champ
     * @return le champ
     */
    private static EditText findField(String hint) {
        return findField(activity.harnessRootView(), hint);
    }

    /**
     * Cherche un champ de saisie par son indication, en profondeur.
     *
     * @param view la vue visitée
     * @param hint l'indication du champ
     * @return le champ, ou null
     */
    private static EditText findField(View view, String hint) {
        if (view instanceof EditText
                && hint.contentEquals(((EditText) view).getHint())) {
            return (EditText) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int at = 0; at < group.getChildCount(); at++) {
                EditText found = findField(group.getChildAt(at), hint);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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
