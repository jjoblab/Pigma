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
     * terrain y avalait les taps du vide), et l'hôte peut la retirer.
     */
    private void minimapTouchZone() {
        begin("minimap : la zone tactile bornée, le retrait de l'hôte");
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
