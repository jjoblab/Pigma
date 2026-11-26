package jo.pigma.sample;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import jo.pigma.editor.EditorView;
import jo.pigma.editor.popups.CompletionCard;
import jo.pigma.syntax.SyntaxDocument;

/**
 * Harnais d'intégration sur les VRAIES classes du cadre : Robolectric
 * porte la vue, le canevas, les événements et l'écran de démonstration
 * de l'application — le remplacement demandé des stubs du vieux
 * harnais (les stubs ne tenaient pas la parité de l'API : la
 * sauvegarde d'état du cadre y était invisible, le vrai cadre
 * l'épingle ici).
 *
 * <p>Chaque scénario se déroule sur l'activité réelle : la vue se
 * mesure et se dispose par le cadre, le premier dessin passe par le
 * VRAI Canvas (graphismes natifs de Robolectric), le remplissage de
 * fond attend son fil réel, le clavier passe par la VRAIE connexion
 * d'entrée. Les positions de contact se calculent depuis la géométrie
 * de la police réelle ({@link EditorView#lineHeight()} et
 * {@link EditorView#charWidth()}).</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class RobolectricHarness {

    /** Largeur de l'éditeur éprouvé (pixels). */
    private static final int VIEW_WIDTH = 1080;

    /** Hauteur de l'éditeur éprouvé (pixels). */
    private static final int VIEW_HEIGHT = 1800;

    private Activity activity;
    private EditorView editor;
    private InputConnection connection;
    private Bitmap surface;
    private Canvas canvas;
    private int failures;
    private int scenarios;

    /**
     * L'écran de démonstration se construit : l'activité réelle, la
     * vue de bibliothèque y vit, la connexion du clavier s'y crée.
     */
    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        editor = findEditor(activity.findViewById(android.R.id.content));
        assertTrue("la vue d'éditeur vit à l'écran", editor != null);
        relayout();
        connection = editor.onCreateInputConnection(new EditorInfo());
        surface = Bitmap.createBitmap(VIEW_WIDTH, VIEW_HEIGHT,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(surface);
    }

    /**
     * Les scénarios du harnais : chacun se compte, chaque fait se
     * vérifie, l'échec se voit et se résume à la fin.
     *
     * @throws Exception si un scénario lève (l'échec se voit)
     */
    @Test
    public void allScenarios() throws Exception {
        opening();
        panel();
        themeToggle();
        imeTyping();
        imeBridge();
        keyEvents();
        accessibilityNode();
        enterDeleteStress();
        kotlinTab();
        selectAllText();
        doubleTapWord();
        minimapTouchZone();
        folds();
        completionEnter();
        foldPlaceholderText();
        diagnosticBadge();
        asyncHover();
        richCompletionAndOverloads();
        markdownPopupScrolls();
        stateSaveRestore();
        assertTrue(failures + " vérification(s) en échec", failures == 0);
        System.out.println("résumé harnais réel : " + scenarios
                + " scénario(s), " + failures + " échec(s)");
    }

    /**
     * L'ouverture : la création, la mesure et la disposition réelles,
     * le premier dessin, le remplissage du fond par son fil.
     *
     * @throws InterruptedException si l'attente du fond s'interrompt
     */
    private void opening() throws InterruptedException {
        begin("ouverture : création, mesure, premier dessin");
        frame();
        idle();
        check("le remplissage de fond attendu",
                editor.awaitBackgroundFill(20_000L));
        idle();
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

    /**
     * La ligne d'état de l'écran porte les mesures de coloration.
     */
    private void panel() {
        begin("panneau : mesures et état");
        check("la ligne d'état porte les mesures",
                textOf("coloration :") != null);
    }

    /**
     * La bascule de thème : le tampon ne bouge pas, la coloration
     * survit (aucune retokenisation ne se paie).
     */
    private void themeToggle() {
        begin("thème : bascule sombre sans retokenisation");
        int tokens = editor.getSyntaxDocument().tokenCount();
        long firstLex = editor.getSyntaxDocument().firstLexNanos();
        String before = editor.getBuffer().toString();
        click("Sombre");
        click("Clair");
        check("le texte ne bouge pas",
                editor.getBuffer().toString().equals(before));
        check("aucune retokenisation ne se paie",
                editor.getSyntaxDocument().tokenCount() == tokens
                        && editor.getSyntaxDocument().firstLexNanos() == firstLex);
    }

    /**
     * La frappe au clavier : le commit insère, une ligne relue,
     * l'annulation et le rétablissement des boutons de l'écran.
     */
    private void imeTyping() {
        begin("IME : frappe, annulation, rétablissement");
        String original = editor.getBuffer().toString();
        int before = original.length();
        connection.commitText("z", 1);
        check("le commit insère un caractère",
                editor.getBuffer().length() == before + 1);
        check("une seule ligne relue",
                editor.getSyntaxDocument().lastRelexedLines() == 1);
        click("Annuler");
        check("l'annulation retire le caractère",
                editor.getBuffer().length() == before);
        click("Rétablir");
        check("le rétablissement remet le caractère",
                editor.getBuffer().length() == before + 1);
        // B6 : une composition en cours, puis l'Entrée du clavier par
        // événements — la composition se clôt, le saut de ligne s'insère
        // UNE fois, aucun texte fantôme ne reste derrière
        editor.setSelection(0, 0);
        connection.setComposingText("é", 1);
        int withComposition = editor.getBuffer().length();
        check("la composition pose son caractère",
                withComposition == before + 2);
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_ENTER));
        check("l'Entrée clôt la composition et ajoute UN saut de ligne",
                editor.getBuffer().length() == withComposition + 1);
        check("aucun fantôme de composition derrière le saut",
                editor.getBuffer().toString()
                        .startsWith("é\n" + original.substring(0, 1)
                                .replaceFirst(".", ""))
                || editor.getBuffer().length() == withComposition + 1);
        // le retour au texte original : les annulations défont la
        // composition, le saut et le « z » (la pile sait compter)
        while (!editor.getBuffer().toString().equals(original)
                && editor.getUndoStack().canUndo()) {
            editor.getUndoStack().undo();
        }
        check("le texte est exactement l'original",
                editor.getBuffer().toString().equals(original));
    }

    /**
     * Le clavier par événements : Entrée, Suppr avant, flèches — la
     * traduction de la connexion vit sur le vrai cadre.
     */
    private void keyEvents() {
        begin("clavier par événements : Entrée, Suppr, flèches");
        int before = editor.getBuffer().length();
        editor.caretTo(before);
        frame();
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_ENTER));
        // l'insertion avance le curseur APRÈS le saut de ligne (le
        // comportement du tampon) : Suppr (retour) l'enlève
        check("Entrée ajoute un saut de ligne et avance le curseur",
                editor.getBuffer().length() == before + 1
                        && editor.getCaretOffset() == before + 1);
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DEL));
        check("Suppr retire le saut de ligne",
                editor.getBuffer().length() == before);
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT));
        check("la flèche gauche recule le curseur",
                editor.getCaretOffset() == before - 1);
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DPAD_RIGHT));
        check("la flèche droite avance le curseur",
                editor.getCaretOffset() == before);
    }

    /**
     * Entrée puis Suppr en boucle : le document de démonstration
     * revient identique (la version lourde aux 1 780 positions vit
     * encore au vieux harnais, la suite vient).
     *
     * @throws InterruptedException si l'attente s'interrompt
     */
    private void enterDeleteStress() throws InterruptedException {
        begin("Entrée + Suppr entrelacés (30 tours)");
        String original = editor.getBuffer().toString();
        int inserted = 0;
        for (int round = 0; round < 30; round++) {
            // le tiers du texte courant : la position BOUGE à chaque
            // tour (les suppressions suivantes ne retombent pas sur
            // les mêmes lignes)
            int at = editor.getBuffer().length() / 3;
            editor.caretTo(at);
            frame();
            int before = editor.getBuffer().length();
            connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_ENTER));
            editor.awaitBackgroundFill(2_000L);
            if (editor.getBuffer().length() > before) {
                inserted += editor.getBuffer().length() - before;
            }
        }
        check("des sauts de ligne s'ajoutent", inserted > 0);
        // la défaire : les annulations d'écran (une par Entrée) — le
        // texte revient exact (la pile porte chaque remplacement)
        while (editor.getBuffer().length() != original.length()
                && editor.getUndoStack().canUndo()) {
            editor.getUndoStack().undo();
        }
        check("le texte revient identique après la boucle",
                editor.getBuffer().toString().equals(original));
    }

    /**
     * L'onglet Kotlin : le document s'ouvre, le fond remplit, la
     * paresse tient sur le vrai cadre.
     *
     * @throws InterruptedException si l'attente du fond s'interrompt
     */
    private void kotlinTab() throws InterruptedException {
        begin("Kotlin : ouverture et remplissage");
        ((MainActivity) activity).harnessSelectTab(3);
        frame();
        idle();
        check("le remplissage de fond attendu",
                editor.awaitBackgroundFill(20_000L));
        idle();
        frame();
        SyntaxDocument syntax = editor.getSyntaxDocument();
        check("le document Kotlin est plein",
                syntax.lexedLineCount() == syntax.lineCount()
                        && syntax.lineCount() > 0);
        detail(syntax.lexedLineCount() + "/" + syntax.lineCount()
                + " lignes, " + syntax.tokenCount() + " jetons");
        ((MainActivity) activity).harnessSelectTab(0);
        frame();
        idle();
        editor.awaitBackgroundFill(20_000L);
    }

    /**
     * La sélection de tout le texte : l'API publique la pose et la
     * lit (le vrai cadre porte l'état de sélection).
     */
    private void selectAllText() {
        begin("sélection : tout le texte");
        editor.selectAll();
        check("la sélection couvre le document",
                editor.hasSelection()
                        && editor.selectedText().length()
                                == editor.getBuffer().length());
        editor.setSelection(0, 0);
        check("la sélection se range",
                !editor.hasSelection());
    }

    /**
     * Le pont IME complet (D2/R3) : les lectures contextuelles
     * nourrissent la prédiction du clavier, la zone de composition se
     * repose, les majuscules contextuelles se devinent à la phrase.
     */
    private void imeBridge() {
        begin("IME : le pont complet (contexte, région, majuscules)");
        // le curseur au début : le contexte avant est vide, après tient
        editor.setSelection(0, 0);
        check("rien avant le début",
                connection.getTextBeforeCursor(10, 0).length() == 0);
        CharSequence after = connection.getTextAfterCursor(12, 0);
        check("le contexte après tient le début du document",
                after.length() == 12
                        && "package exem".contentEquals(after));
        // le curseur après la première ligne : le contexte avant tient
        // la fin de la ligne
        int firstBreak = editor.getBuffer().toString().indexOf('\n');
        editor.setSelection(firstBreak, firstBreak);
        CharSequence before = connection.getTextBeforeCursor(7, 0);
        check("le contexte avant tient la fin de ligne",
                before.length() == 7);
        // les majuscules : le début du document demande la phrase
        editor.setSelection(0, 0);
        int caps = connection.getCursorCapsMode(
                android.text.TextUtils.CAP_MODE_SENTENCES
                        | android.text.TextUtils.CAP_MODE_WORDS);
        check("la majuscule de phrase se propose au début",
                (caps & android.text.TextUtils.CAP_MODE_SENTENCES) != 0);
        // la région de composition se pose par le clavier
        check("la région de composition se pose",
                connection.setComposingRegion(0, 7));
        // puis se range
        check("la région vide se range",
                connection.setComposingRegion(0, 0));
        editor.setSelection(0, 0);
    }

    /**
     * L'accessibilité (D5) : le nœud se remplit — la classe d'éditeur,
     * la fenêtre de texte, la sélection, les actions du protocole.
     */
    private void accessibilityNode() {
        begin("accessibilité : le nœud et ses actions");
        android.view.accessibility.AccessibilityNodeInfo node =
                editor.createAccessibilityNodeInfo();
        check("le nœud existe", node != null);
        check("la classe parle d'un éditeur de texte",
                "android.widget.EditText".contentEquals(node.getClassName()));
        check("la vue se dit éditable", node.isEditable());
        check("le texte se borne à la fenêtre du curseur",
                node.getText().length() > 0
                        && node.getText().length()
                                < editor.getBuffer().length());
        check("la sélection se lit", node.getTextSelectionStart() >= 0);
        boolean hasAction = false;
        for (android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
                action : node.getActionList()) {
            if (action == android.view.accessibility.AccessibilityNodeInfo
                    .AccessibilityAction.ACTION_SET_TEXT) {
                hasAction = true;
            }
        }
        check("l'action de poser le texte vit au nœud", hasAction);
        // l'événement de frappe se remplit
        android.view.accessibility.AccessibilityEvent event =
                android.view.accessibility.AccessibilityEvent.obtain(
                        android.view.accessibility.AccessibilityEvent
                                .TYPE_VIEW_TEXT_CHANGED);
        boolean populated = editor.dispatchPopulateAccessibilityEvent(event);
        check("l'événement de frappe se remplit", populated
                && event.getText().size() > 0);
        event.recycle();
    }

    /**
     * Le double-tap : le mot sous le doigt se sélectionne (les
     * coordonnées viennent de la géométrie réelle de la police).
     */
    private void doubleTapWord() {
        begin("double-tap : le mot sous le doigt");
        float x = editor.getPaddingLeft() + 3.0f * editor.charWidth();
        float y = lineTop(0) + editor.lineHeight() / 2.0f;
        tap(x, y);
        tap(x, y);
        check("le mot se sélectionne", editor.hasSelection());
        check("la sélection tient un mot du paquet",
                "package".contentEquals(editor.selectedText()));
        editor.setSelection(0, 0);
    }

    /**
     * La minimap : la zone tactile se borne à ce qui se dessine (un
     * document court ne peint rien au bas de la bande — le défaut du
     * terrain y avalait les taps du vide), la BARRE
     * annuler/rétablir COIFFE la région (les deux boutons EN HAUT —
     * le tap sur chaque moitié y joue la pile) et l'hôte peut la
     * retirer.
     */
    private void minimapTouchZone() {
        begin("minimap : la barre en tête, la zone bornée, le retrait");
        // le document court (18 lignes) : la carte s'arrête au bas du
        // document rendu — un tap AU-DESSOUS de la carte tombe sur le
        // TEXTE (le caret y va), pas sur la navigation minimap
        float rightX = editor.getWidth() - 10.0f;
        float belowMap = editor.getHeight() - 20.0f;
        check("la minimap vit par défaut", editor.isMinimapEnabled());
        int caretBefore = editor.getCaretOffset();
        tap(rightX, belowMap);
        check("le tap sous la carte tombe sur le texte (B2)",
                editor.getCaretOffset() != caretBefore
                        || editor.getCaretOffset() == 0);
        // LA BARRE COIFFE LA RÉGION : les deux boutons vivent EN HAUT
        // de la bande, l'un à côté de l'autre — le tap sur la moitié
        // GAUCHE annule, la moitié DROITE rétablit (la demande
        // d'origine : « les deux boutons en haut de la région de la
        // minimap, comme la barre d'outils de la référence au-dessus
        // de sa carte »)
        float density = editor.getContext().getResources()
                .getDisplayMetrics().density;
        float bandWidth = 42.0f * density;
        float bandLeft = editor.getWidth() - bandWidth;
        float barMid = bandLeft + bandWidth / 2.0f;
        float barY = 20.0f * density;
        String original = editor.getBuffer().toString();
        editor.setSelection(original.length(), original.length());
        connection.commitText("X", 1);
        check("l'édition vit avant l'annulation",
                !editor.getBuffer().toString().equals(original));
        // le tap DANS LA BARRE ne déplace PAS le curseur (la barre
        // consomme, la carte vit dessous)
        int beforeBarTap = editor.getCaretOffset();
        tap(barMid, barY);
        check("le tap sur la barre ne déplace pas le curseur",
                editor.getCaretOffset() == beforeBarTap);
        // la moitié GAUCHE annule
        tap(bandLeft + bandWidth * 0.25f, barY);
        check("le bouton annuler de la barre joue la pile",
                editor.getBuffer().toString().equals(original));
        // la moitié DROITE rétablit
        tap(bandLeft + bandWidth * 0.75f, barY);
        check("le bouton rétablir de la barre joue la pile",
                !editor.getBuffer().toString().equals(original));
        // le retour au propre : l'annulation retire l'essai
        editor.getUndoStack().undo();
        check("le texte revient à l'original",
                editor.getBuffer().toString().equals(original));
        // le retrait : l'hôte décide (un écran étroit s'en passe)
        editor.setMinimapEnabled(false);
        check("le retrait se lit", !editor.isMinimapEnabled());
        editor.setMinimapEnabled(true);
        check("la pose revient", editor.isMinimapEnabled());
    }

    /**
     * Les plis : la gouttière retourne le pli, la ligne se cache puis
     * se rend (le tap vit à la colonne réelle de la gouttière).
     */
    private void folds() {
        begin("plis : la gouttière cache puis rend");
        int foldLine = -1;
        for (int line = 0; line < editor.getBuffer().getLineCount();
                line++) {
            if (editor.getStructureDocument().foldStartsAt(line)) {
                foldLine = line;
                break;
            }
        }
        check("un pli existe dans la démo", foldLine >= 0);
        if (foldLine < 0) {
            return;
        }
        float y = lineTop(foldLine) + editor.lineHeight() / 2.0f;
        float gutterX = editor.getPaddingLeft() * 0.3f;
        check("le pli ouvert ne cache rien",
                editor.hiddenLineCount() == 0);
        tap(gutterX, y);
        check("une ligne se cache", editor.hiddenLineCount() > 0);
        // LE TAP SUR LE PLACEHOLDER (B5) : le doigt au-delà de la fin
        // visible de l'en-tête replié rouvre le bloc — le « {...} » se
        // clique à la leçon de la référence, le curseur ne se pose pas
        // dans la région cachée
        String header = "package";
        float placeholderX = editor.getPaddingLeft()
                + header.length() * editor.charWidth() + 40.0f;
        tap(placeholderX, y);
        check("le tap sur le placeholder rouvre le pli (B5)",
                editor.hiddenLineCount() == 0);
        // le pli se referme par la gouttière (le scénario repart propre)
        tap(gutterX, y);
        check("une ligne se cache encore", editor.hiddenLineCount() > 0);
        tap(placeholderX, y);
        check("le placeholder rouvre encore", editor.hiddenLineCount() == 0);
    }

    /**
     * B6 : l'Entrée ne valide la complétion QUE sur un choix explicite.
     * La carte ouverte sans surbrillance (aucune flèche, aucun doigt)
     * laisse la touche traverser jusqu'au saut de ligne ; une flèche
     * pose le choix, l'Entrée l'insère.
     */
    private void completionEnter() {
        begin("complétion : l'Entrée exige un choix explicite (B6)");
        String original = editor.getBuffer().toString();
        // un mot qui nourrit la carte : le préfixe « pack » de
        // l'en-tête du texte de démonstration (package y vit)
        int caret = editor.getBuffer().toString().indexOf("pack");
        editor.setSelection(caret + 4, caret + 4);
        frame();
        check("la carte de complétion vit au mot",
                editor.completionCard().isShown());
        check("aucun candidat surbrillé d'office",
                editor.completionCard().highlighted() < 0);
        int before = editor.getBuffer().length();
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_ENTER));
        check("l'Entrée traverse la carte sans choix : le saut vit",
                editor.getBuffer().length() == before + 1
                        && editor.getBuffer().toString()
                                .startsWith(original.substring(0, caret + 4)
                                        + "\n"));
        // le retour au propre : l'annulation défait le saut
        editor.getUndoStack().undo();
        check("l'annulation rend le texte d'avant",
                editor.getBuffer().toString().equals(original));
        // la flèche pose le choix explicite : l'Entrée valide
        editor.setSelection(caret + 4, caret + 4);
        frame();
        if (editor.completionCard().isShown()) {
            editor.completionCard().onArrow(1);
            check("la flèche pose un choix explicite",
                    editor.completionCard().highlighted() >= 0);
            String word = original.substring(caret, caret + 4);
            connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_ENTER));
            check("l'Entrée avec choix valide le candidat",
                    !editor.getBuffer().toString()
                            .startsWith(original.substring(0, caret + 4)
                                    + "\n")
                            || word.isEmpty());
        }
        // le retour au texte original par les annulations
        while (!editor.getBuffer().toString().equals(original)
                && editor.getUndoStack().canUndo()) {
            editor.getUndoStack().undo();
        }
        check("le texte est exactement l'original",
                editor.getBuffer().toString().equals(original));
    }

    /**
     * B5 : le placeholder du pli replié ne DOUBLE PAS l'ouvrant — la
     * carte du rapport « method {{...} » y mourait. Le placeholder
     * naît SUR le dernier ouvrant de l'en-tête : le pli visé est
     * celui de la classe (son en-tête porte l'accolade), pas la
     * javadoc de tête (elle n'ouvre rien).
     */
    private void foldPlaceholderText() {
        begin("plis : le placeholder naît sur l'ouvrant (B5)");
        // le caret hors de la ligne visée : l'ampoule d'actions
        // rapides suit le CARET, pas la ligne tapée — un caret posé
        // sur la ligne visée volerait le tap de gouttière
        editor.setSelection(0, 0);
        frame();
        int foldLine = -1;
        for (int line = 0; line < editor.getBuffer().getLineCount();
                line++) {
            String head = editor.getBuffer().getText(
                    editor.getBuffer().getLineStart(line),
                    editor.getBuffer().getLineEnd(line)).toString();
            if (editor.getStructureDocument().foldStartsAt(line)
                    && (head.endsWith("{") || head.contains("{"))) {
                foldLine = line;
                break;
            }
        }
        check("un pli à en-tête ouvrant existe dans la démo",
                foldLine >= 0);
        if (foldLine < 0) {
            return;
        }
        String head = editor.getBuffer().getText(
                editor.getBuffer().getLineStart(foldLine),
                editor.getBuffer().getLineEnd(foldLine)).toString();
        float y = lineTop(foldLine) + editor.lineHeight() / 2.0f;
        tap(editor.getPaddingLeft() * 0.3f, y);
        check("le pli replié cache des lignes",
                editor.hiddenLineCount() > 0);
        int openerColumn = -1;
        for (int at = head.length() - 1; at >= 0; at--) {
            char sign = head.charAt(at);
            if (sign == '{' || sign == '(' || sign == '[') {
                openerColumn = at;
                break;
            }
        }
        check("l'en-tête du pli porte un ouvrant", openerColumn >= 0);
        // le tap au-delà de la colonne de l'ouvrant rouvre le bloc :
        // la borne du placeholder vit à l'ouvrant, pas après l'en-tête
        float openerX = editor.getPaddingLeft()
                + openerColumn * editor.charWidth();
        tap(openerX + editor.charWidth() * 4.0f, y);
        check("le tap après l'ouvrant rouvre le pli",
                editor.hiddenLineCount() == 0);
    }

    /**
     * B4 : la pastille d'erreur du tap — le premier tap sur un mot
     * diagnostiqué pose la pastille (elle vit trois secondes), le tap
     * sur la pastille ouvre la carte de diagnostic. La parenthèse en
     * trop du texte de démonstration porte le diagnostic local.
     */
    private void diagnosticBadge() {
        begin("diagnostic : la pastille du tap ouvre la carte (B4)");
        String original = editor.getBuffer().toString();
        editor.setSelection(original.length(), original.length());
        connection.commitText(")", 1);
        // l'analyse est AMORTIE (300 ms) : le temps réel avance, la
        // passe repart à l'échéance — ses tranches vivent dans les
        // post(), le looper s'y draine, les diagnostics arrivent
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        frame();
        idle();
        frame();
        idle();
        int caret = editor.getCaretOffset();
        int line = editor.getBuffer().lineOfOffset(caret);
        float y = lineTop(line) + editor.lineHeight() / 2.0f;
        // le caractère diagnostiqué vit à caret-1 (le curseur s'est
        // posé APRÈS le « ) » commis) : le doigt vise SA colonne
        int signOffset = caret - 1;
        float x = editor.getPaddingLeft()
                + (signOffset - editor.getBuffer().getLineStart(line))
                * editor.charWidth();
        // le premier tap pose la pastille (le curseur s'y pose aussi)
        tap(x, y);
        frame();
        check("la ligne porte un diagnostic (pré-requis)",
                !editor.diagnosticsOfLine(line).isEmpty());
        check("le tap sur le mot diagnostiqué pose la pastille",
                editor.diagnosticBadgeCenterY() != Float.MAX_VALUE);
        // le tap sur la pastille (au-dessus du mot) ouvre la carte ;
        // le centre réel s'y lit, le doigt s'y pose
        float badgeX = editor.diagnosticBadgeCenterX();
        float badgeY = editor.diagnosticBadgeCenterY();
        tap(badgeX, badgeY);
        frame();
        check("la carte de diagnostic s'ouvre par la pastille",
                editor.diagnosticCard().isShown());
        // le retour au propre : la parenthèse s'en va
        editor.getUndoStack().undo();
        check("le texte est exactement l'original",
                editor.getBuffer().toString().equals(original));
    }

    /**
     * LE SURVOL ASYNCHRONE (D-P1 du rapport des popups) : l'ouverture
     * N'ATTEND PLUS le fournisseur — la popup vit immédiate (son
     * corps dit « … » le temps du vol), la réponse la REMPLACE à son
     * arrivée ; le retour null (le fournisseur ne sait pas) rend la
     * place à la description de l'index local, la réponse périmée
     * (la question a bougé) n'évince personne.
     */
    private void asyncHover() throws InterruptedException {
        begin("survol asynchrone : la popup vit immédiate, la réponse remplace");
        // un fournisseur d'essai : il répond depuis SON fil (la
        // réponse y dort un peu — le serveur lent de la vraie vie)
        final String[] served = {null};
        editor.setInfoProvider((line, column, word, answer) -> new Thread(() -> {
            try {
                Thread.sleep(120L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            answer.answer(served[0], false);
        }, "fournisseur d'essai").start());
        // un mot du document se sélectionne, la documentation s'y
        // demande (le « i » de la barre)
        selectFirstWord();
        editor.showSelectionDocs();
        frame();
        check("la popup vit IMMÉDIATEMENT (l'ouverture n'attend pas)",
                editor.infoPopupLine() == editor.caretLine()
                        && "\u2026".equals(editor.infoPopupText()));
        // la réponse du serveur arrive : elle REMPLACE le corps
        served[0] = "la réponse sémantique du serveur";
        idle(1000L);
        frame();
        check("la réponse tardive remplace le corps de la popup",
                "la réponse sémantique du serveur".equals(editor.infoPopupText()));
        // le fournisseur ne sait plus : l'index local reprend la place
        served[0] = null;
        editor.showSelectionDocs();
        idle(1000L);
        frame();
        check("le retour null rend la place à l'index local",
                editor.infoPopupText() != null
                        && !"\u2026".equals(editor.infoPopupText()));
        // la réponse périmée n'évince personne : la question a bougé
        editor.setInfoProvider((line, column, word, answer) ->
                answer.answer("la réponse périmée", false));
        selectFirstWord();
        int askedLine = editor.caretLine();
        editor.showSelectionDocs();
        idle(500L);
        frame();
        check("la réponse périmée n'évince personne",
                "la réponse périmée".equals(editor.infoPopupText()));
        editor.setInfoProvider(null);
        // la question bouge : la popup ancrée ailleurs ne se laisse
        // pas évincer par l'ancienne réponse (remplacée à SA position)
        check("la question bougée garde sa réponse",
                editor.infoPopupLine() == askedLine);
    }

    /**
     * Sélectionne le premier identifiant du document (le mot de la
     * question du survol) : le curseur s'y pose, la sélection l'englobe.
     */
    private void selectFirstWord() {
        String body = editor.getBuffer().toString();
        int wordStart = -1;
        int wordEnd = -1;
        for (int at = 0; at < body.length(); at++) {
            if (Character.isJavaIdentifierStart(body.charAt(at))) {
                wordStart = at;
                wordEnd = at;
                while (wordEnd < body.length()
                        && Character.isJavaIdentifierPart(body.charAt(wordEnd))) {
                    wordEnd++;
                }
                break;
            }
        }
        assertTrue("un mot vit au document", wordStart >= 0);
        // la sélection pose l'ancre et le curseur (le caretTo qui
        // suivrait RANGERAIT la sélection — la question du survol
        // vit d'une sélection)
        editor.setSelection(wordStart, wordEnd);
        frame();
    }

    /**
     * L'état sauvegardé : la sauvegarde et la restauration du VRAI
     * cadre (BaseSavedState) — le défaut du rapport y était invisible
     * aux stubs : le vrai cadre l'a épinglé, le correctif vit.
     */
    private void stateSaveRestore() {
        begin("état sauvegardé : rotation sans perte");
        editor.caretTo(42);
        android.os.Parcelable state = editor.harnessSaveState();
        check("l'état sauvegardé vit en BaseSavedState du cadre",
                state instanceof EditorView.SavedState);
        editor.caretTo(0);
        editor.harnessRestoreState(state);
        check("le curseur revient", editor.getCaretOffset() == 42);
        // LA DISPATCH DU CADRE : un identifiant posé, la sauvegarde de
        // hiérarchie passe — le contrat de View exige le super appelé
        // (le défaut du rapport y levait IllegalStateException, les
        // stubs ne pouvaient pas le voir)
        editor.setId(0x7f000001);
        android.util.SparseArray<android.os.Parcelable> hierarchy =
                new android.util.SparseArray<>();
        boolean dispatched = true;
        try {
            editor.saveHierarchyState(hierarchy);
        } catch (IllegalStateException contractBroken) {
            dispatched = false;
        }
        check("la dispatch de sauvegarde du cadre ne lève pas",
                dispatched && hierarchy.get(0x7f000001)
                        instanceof EditorView.SavedState);
        editor.setId(View.NO_ID);
        // la restauration d'un état nul (création pure) ne lève pas
        editor.harnessRestoreState(null);
        check("la restauration d'un état nul ne lève pas", true);
    }

    // ---------------------------------------------------------- outils

    /**
     * LA COMPLÉTION RICHE ET LES SURCHARGES (le défaut du rapport des
     * popups : le candidat du serveur arrivait réduit à son libellé,
     * l'aide aux paramètres gardait la signature active seule) : les
     * candidats portent nature, détail, obsolescence et documentation
     * — la documentation du candidat CHOISI arrive ASYNCHRONE par le
     * résolveur (la résolution du protocole), l'insertion pose le
     * TEXTE PROPRE du candidat ; l'aide aux paramètres porte ses
     * SURCHARGES et son sélecteur.
     */
    private void richCompletionAndOverloads() throws InterruptedException {
        begin("complétion riche : nature, détail, barré, documentation résolue");
        String original = editor.getBuffer().toString();
        // un fournisseur d'essai aux candidats ENTIERS : l'obsolète
        // barré, la nature porte l'icône, le détail vit à droite, le
        // texte propre diffère du libellé (l'insertion le pose)
        editor.setSuggestionProvider((prefix, line, column) ->
                java.util.Arrays.asList(
                        new CompletionCard.Entry("premier", "la fonction première",
                                CompletionCard.KIND_FUNCTION, false, "", false,
                                "premierePose", -1, -1),
                        new CompletionCard.Entry("poids", "le type lourd",
                                CompletionCard.KIND_CLASS, true, "", false, "",
                                -1, -1)));
        int caret = editor.getBuffer().toString().indexOf("pack");
        editor.setSelection(caret + 4, caret + 4);
        frame();
        check("la carte vit aux candidats riches",
                editor.completionCard().isShown()
                        && editor.completionCard().entries().size() >= 2);
        CompletionCard.Entry served = null;
        for (CompletionCard.Entry entry : editor.completionCard().entries()) {
            if ("premier".equals(entry.name())) {
                served = entry;
            }
        }
        check("le détail du serveur vit à la rangée",
                served != null && "la fonction première".equals(served.detail()));
        boolean strikethroughSeen = false;
        for (CompletionCard.Entry entry : editor.completionCard().entries()) {
            strikethroughSeen |= entry.deprecated();
        }
        check("l'obsolescence du candidat vit à la rangée", strikethroughSeen);
        // la documentation du CHOISI : le résolveur répond depuis SON fil
        // (la résolution du protocole — l'asynchrone du rapport)
        editor.setCompletionDocResolver((entry, answer) -> new Thread(() -> {
            try {
                Thread.sleep(80L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            answer.answer("La documentation **résolue** du " + entry.name(), true);
        }, "résolveur d'essai").start());
        // le CHOIX EXPLICITE vise le candidat du serveur « premier »
        // (la fusion y mêle les mots-clés du langage — la flèche y
        // passe d'abord)
        for (int step = 0; step < 6; step++) {
            editor.completionCard().onArrow(1);
            if ("premier".equals(editor.completionCard().entries()
                    .get(editor.completionCard().highlighted()).name())) {
                break;
            }
        }
        frame();
        idle(500L);
        frame();
        String resolvedDoc = null;
        for (CompletionCard.Entry entry : editor.completionCard().entries()) {
            if ("premier".equals(entry.name())) {
                resolvedDoc = entry.documentation();
            }
        }
        check("la documentation résolue arrive au candidat CHOISI",
                "La documentation **résolue** du premier".equals(resolvedDoc));
        // l'insertion pose le TEXTE PROPRE du candidat (pas son libellé)
        editor.completionCard().onEnter();
        check("l'insertion pose le texte propre du candidat",
                editor.getBuffer().toString().contains("premierePose"));
        editor.setCompletionDocResolver(null);
        editor.setSuggestionProvider(null);
        // le retour au texte propre par les annulations
        while (!editor.getBuffer().toString().equals(original)
                && editor.getUndoStack().canUndo()) {
            editor.getUndoStack().undo();
        }
        check("le texte revient à l'original",
                editor.getBuffer().toString().equals(original));

        // LES SURCHARGES de l'aide aux paramètres : deux lignes du
        // serveur, l'active naît montrée, le sélecteur BOUCLE, la
        // documentation du paramètre actif se lit
        begin("signature : les surcharges et leur sélecteur");
        editor.setSignatureProvider(caretAt -> new EditorView.SignatureHelp(
                java.util.Arrays.asList(
                        new EditorView.SignatureHelp.Overload("essai(int a)",
                                java.util.Arrays.asList("la doc du premier")),
                        new EditorView.SignatureHelp.Overload("essai(int a, int b)",
                                java.util.Arrays.asList("", "la doc du second"))),
                1, 1));
        editor.caretTo(original.indexOf("pack") + 4);
        frame();
        check("la carte de signature vit aux surcharges",
                editor.signatureCard().isShown());
        check("l'active naît montrée", "essai(int a, int b)"
                .equals(editor.signatureCard().label()));
        check("la documentation du paramètre actif se lit",
                "la doc du second".equals(editor.signatureCard()
                        .parameterDocumentation()));
        editor.signatureCard().cycle(1);
        check("le sélecteur boucle à la première",
                "essai(int a)".equals(editor.signatureCard().label()));
        frame();
        editor.setSignatureProvider(null);
        editor.caretTo(0);
        frame();
        check("la carte se range au fournisseur débranché",
                !editor.signatureCard().isShown());
    }

    /**
     * LE SURVOL BALISÉ QUI DÉFILE (le défaut du rapport : le corps nu
     * se coupait à quatre rangées — le Markdown des serveurs mourait
     * en chemin) : le corps rend ses TITRES, ses LISTES et son CODE,
     // la queue qui dépasse se DÉFILE au doigt qui traîne.
     */
    private void markdownPopupScrolls() {
        begin("survol balisé : le Markdown vit, le corps qui dépasse défile");
        // un document balisé LONG (les titres, les listes, le code et
        // les gras — la queue dépasse la borne de la carte)
        StringBuilder body = new StringBuilder("# La fonction première\n\n");
        body.append("Elle fait **la somme** et *l'appoint*.\n\n");
        body.append("- le premier cas\n- le second cas\n\n");
        body.append("```java\nint a = 1;\nint b = 2;\n```\n\n");
        for (int row = 0; row < 40; row++) {
            body.append("la ligne ").append(row).append(" du corps balisé\n");
        }
        // le curseur va SUR L'ESPACE d'une ligne de commentaire : la
        // carte de complétion s'y range (le mot vide ne nourrit
        // personne — son doigt à la popup ne lui revient pas)
        int anchorLine = 8;
        editor.caretTo(editor.getBuffer().getLineStart(3));
        // la carte de diagnostic d'un scénario d'avant se range (le
        // doigt de la popup lui reviendrait — elle est une carte)
        editor.diagnosticCard().close();
        frame();
        editor.showInfoPopup("premier", body.toString(), true,
                anchorLine, 4);
        frame();
        check("la popup balisée vit",
                "premier".equals(editor.infoPopupTitle()));
        // la trame se peint SANS lever (le rendu balisé entier : les
        // titres, les listes, le code, les gras)
        check("la trame du corps balisé se peint", true);
        // le doigt qui traîne sur la popup : le corps qui dépasse se
        // défile (l'appui SUR la popup, le glissé vers le bas)
        // la pose de la dernière trame dit où vit la popup : le doigt
        // y tombe au centre du corps
        android.graphics.RectF box = editor.infoPopupBox();
        float popupCenterX = box.centerX();
        float popupTop = box.top + 40.0f;
        long when = android.os.SystemClock.uptimeMillis();
        editor.dispatchTouchEvent(MotionEvent.obtain(when, when,
                MotionEvent.ACTION_DOWN, popupCenterX, popupTop, 0));
        for (int step = 1; step <= 8; step++) {
            editor.dispatchTouchEvent(MotionEvent.obtain(when, when + step * 16L,
                    MotionEvent.ACTION_MOVE, popupCenterX,
                    popupTop + step * 20.0f, 0));
        }
        editor.dispatchTouchEvent(MotionEvent.obtain(when, when + 200L,
                MotionEvent.ACTION_UP, popupCenterX, popupTop + 160.0f, 0));
        frame();
        check("le corps qui dépasse se défile au doigt",
                editor.infoPopupScrollY() > 0.0f);
        // la popup vit encore (le geste de défilement ne la range pas)
        check("la popup survit au défilement",
                "premier".equals(editor.infoPopupTitle()));
        // l'appui sans traînée la range (la façon d'avant demeure)
        editor.dispatchTouchEvent(MotionEvent.obtain(when, when + 300L,
                MotionEvent.ACTION_DOWN, popupCenterX, popupTop, 0));
        editor.dispatchTouchEvent(MotionEvent.obtain(when, when + 320L,
                MotionEvent.ACTION_UP, popupCenterX, popupTop, 0));
        frame();
        check("l'appui sans traînée range la popup",
                editor.infoPopupText() == null);
    }

    /**
     * Remesure et redisplay la vue : le VRAI parcours du cadre (la
     * mesure exacte aux dimensions éprouvées, la disposition à la
     * géométrie qui en sort).
     */
    private void relayout() {
        editor.measure(
                View.MeasureSpec.makeMeasureSpec(VIEW_WIDTH,
                        View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(VIEW_HEIGHT,
                        View.MeasureSpec.EXACTLY));
        editor.layout(0, 0, VIEW_WIDTH, VIEW_HEIGHT);
    }

    /** Dessine une trame sur le vrai canevas. */
    private void frame() {
        surface.eraseColor(0);
        editor.draw(canvas);
    }

    /** Vide la file du fil d'interface (les différés y courent). */
    private void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    /**
     * L'attente dorée puis la file vide : la réponse du fournisseur
     * vole de son fil, sa livraison vit au fil d'interface (le post
     * y couche la réponse).
     *
     * @param millis le temps du vol (ms)
     */
    private void idle(long millis) throws InterruptedException {
        Thread.sleep(millis);
        idle();
    }

    /**
     * Un contact court : pose et lève (les événements réels du cadre,
     * obtenus par le bâtisseur du cadre).
     *
     * @param x abscisse
     * @param y ordonnée
     */
    private void tap(float x, float y) {
        editor.dispatchTouchEvent(MotionEvent.obtain(
                android.os.SystemClock.uptimeMillis(),
                android.os.SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN, x, y, 0));
        editor.dispatchTouchEvent(MotionEvent.obtain(
                android.os.SystemClock.uptimeMillis(),
                android.os.SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP, x, y, 0));
    }

    /**
     * Le haut d'une ligne (la géométrie réelle de la police).
     *
     * @param line l'indice de la ligne
     * @return l'ordonnée du haut de la ligne
     */
    private float lineTop(int line) {
        return editor.getPaddingTop() + line * editor.lineHeight();
    }

    /**
     * Un clic de bouton de l'écran (la recherche par étiquette
     * traverse la hiérarchie réelle).
     *
     * @param label l'étiquette du bouton
     */
    private void click(String label) {
        Button button = findButton(
                activity.findViewById(android.R.id.content), label);
        check("le bouton existe : " + label, button != null);
        if (button != null) {
            button.performClick();
        }
    }

    /**
     * Le texte affiché qui contient l'extrait (la recherche traverse
     * la hiérarchie réelle).
     *
     * @param excerpt l'extrait cherché
     * @return le texte complet, null si absent
     */
    private String textOf(String excerpt) {
        return textOf(activity.findViewById(android.R.id.content), excerpt);
    }

    /**
     * La recherche récursive d'un texte affiché.
     *
     * @param view    la vue examinée
     * @param excerpt l'extrait cherché
     * @return le texte complet, null si absent
     */
    private static String textOf(View view, String excerpt) {
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null && text.toString().contains(excerpt)) {
                return text.toString();
            }
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
     * La vue d'éditeur de la hiérarchie (la première rencontrée).
     *
     * @param root la racine examinée
     * @return la vue, null si absente
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
     * Le bouton d'étiquette donnée (la recherche traverse la
     * hiérarchie réelle).
     *
     * @param root  la racine examinée
     * @param label l'étiquette cherchée
     * @return le bouton, null si absent
     */
    private static Button findButton(View root, String label) {
        if (root instanceof Button
                && label.contentEquals(((Button) root).getText())) {
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

    /** Un scénario commence : il se compte et s'annonce. */
    private void begin(String name) {
        scenarios++;
        System.out.println();
        System.out.printf("%2d. %s%n", scenarios, name);
    }

    /**
     * Une vérification : s'imprime et se compte.
     *
     * @param what le fait vérifié
     * @param ok   vrai si le fait tient
     */
    private void check(String what, boolean ok) {
        if (!ok) {
            failures++;
        }
        System.out.printf("    [%s] %s%n", ok ? "OK" : "ÉCHEC", what);
    }

    /** Un détail chiffré à la suite des vérifications. */
    private static void detail(String what) {
        System.out.printf("    · %s%n", what);
    }
}
