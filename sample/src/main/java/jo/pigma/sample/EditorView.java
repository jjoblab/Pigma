package jo.pigma.sample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import jo.pigma.core.TextBuffer;
import jo.pigma.core.TextListener;
import jo.pigma.core.TextTransaction;
import jo.pigma.core.UndoStack;
import jo.pigma.syntax.BackgroundFill;
import jo.pigma.syntax.LanguageSyntax;
import jo.pigma.syntax.Languages;
import jo.pigma.syntax.SyntaxDocument;
import jo.pigma.syntax.SyntaxTheme;

/**
 * Vue d'édition minimale : le texte affiché est le tampon du cœur,
 * coloré par le moteur de syntaxe, chaque retour de l'IME est une
 * transaction (une unité d'annulation), le curseur se place au toucher
 * et le doigt fait défiler verticalement.
 *
 * <p>Le rendu reste volontairement rudimentaire : police à chasse
 * fixe, une ligne visuelle par ligne du tampon, curseur rectangulaire,
 * soulignement de la zone de composition. Il ne dessine que les
 * lignes visibles — c'est la fenêtre paresseuse du moteur qui s'y
 * paie, le remplissage du reste court sur le fil de fond annulable :
 * les tranches s'y prennent en clichés et ne s'appliquent que si la
 * révision observée du moteur n'a pas bougé — une frappe entre deux
 * tranches annule la tranche, jamais le document. Changer de thème ne
 * retokenise rien : la résolution des couleurs se fait à la cuisson
 * du plan. Le dessin d'une ligne s'arrête à {@value #MAX_DRAWN_COLUMNS}
 * colonnes, une ligne monstre (hors plafond moteur) s'affiche en
 * texte brut. Les plans de rendu passent par un cache (validité par
 * identité d'entrée, partage par contenu pour les lignes identiques)
 * et la durée de la dernière trame s'y lit — le budget par image du
 * panneau. La sélection et la gouttière viendront avec la vue
 * complète de la bibliothèque.</p>
 */
public final class EditorView extends View {

    /** Plan de rendu d'une ligne : textes tranchés, couleurs résolues, abscisses. */
    record LinePlan(String[] texts, int[] colors, float[] xs) {
    }

    private static final String INITIAL_TEXT = DemoTexts.JAVA;

    private static final int CARET_COLOR = 0xFF2962FF;
    private static final int COMPOSE_COLOR = 0xFF7A8199;

    /** Colonnes au-delà desquelles le dessin d'une ligne s'arrête. */
    private static final int MAX_DRAWN_COLUMNS = 10_000;

    /** Plans partagés par contenu, au-delà le plus ancien s'évince. */
    private static final int SHARED_PLANS = 256;

    private final TextBuffer buffer;
    private final UndoStack undoStack;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint caretPaint = new Paint();
    private final Paint composePaint = new Paint();
    private final Paint.FontMetrics fontMetrics;
    private final float charWidth;
    private final float lineHeight;
    private final float caretWidth;

    private SyntaxDocument syntax;
    private SyntaxTheme theme = SampleThemes.LIGHT;
    private int themeBackground = SampleThemes.LIGHT_BACKGROUND;

    private int caretOffset;
    private int composingStart = -1;
    private int composingEnd = -1;

    private float scrollY;
    private float touchStartX;
    private float touchStartY;
    private boolean dragging;

    /** Le fil de fond du remplissage (clichés révision-vérifiés). */
    private BackgroundFill backgroundFill;

    private final RenderCache<LinePlan> renderCache = new RenderCache<>(SHARED_PLANS);

    /** Durée de la dernière trame (budget par image, diagnostic). */
    private long lastFrameNanos;

    /** Part moteur de la dernière trame : le temps passé dans lineTokens. */
    private long lastFrameEngineNanos;

    /** Pire trame depuis le dernier scénario (l'odomètre du budget). */
    private long worstFrameNanos;

    private long frameCount;

    private final TextListener modelListener = new TextListener() {
        @Override
        public void textChanged(TextBuffer changed, int start, int removedLength,
                                int insertedLength, int revision) {
            clampCaret();
            ensureCaretVisible();
            ensureFill();
            invalidate();
        }
    };

    public EditorView(Context context) {
        super(context);
        buffer = TextBuffer.create(INITIAL_TEXT);
        undoStack = new UndoStack(buffer);
        syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        backgroundFill = BackgroundFill.start(syntax);
        textPaint.setTypeface(Typeface.MONOSPACE);
        textPaint.setTextSize(14.0f * getResources().getDisplayMetrics().scaledDensity);
        fontMetrics = textPaint.getFontMetrics();
        charWidth = textPaint.measureText("M");
        lineHeight = (float) Math.ceil(fontMetrics.bottom - fontMetrics.top) + dp(2);
        caretWidth = dp(2);
        caretPaint.setColor(CARET_COLOR);
        composePaint.setColor(COMPOSE_COLOR);
        buffer.addListener(modelListener);
        int padding = dp(12);
        setPadding(padding, dp(16), padding, dp(16));
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    /**
     * Tampon édité par cette vue : un hôte peut y brancher ses propres
     * écouteurs (ligne d'état, diagnostics).
     *
     * @return le tampon, jamais {@code null}
     */
    public TextBuffer getBuffer() {
        return buffer;
    }

    /**
     * Document de coloration de la vue : le panneau de diagnostic y
     * lit ses mesures.
     *
     * @return le moteur de coloration, jamais {@code null}
     */
    public SyntaxDocument getSyntaxDocument() {
        return syntax;
    }

    /**
     * Durée de la dernière trame dessinée — le budget par image du
     * panneau de diagnostic.
     *
     * @return la durée en nanosecondes, 0 avant le premier dessin
     */
    public long lastFrameNanos() {
        return lastFrameNanos;
    }

    /**
     * Part moteur de la dernière trame : le temps cumulé passé à demander
     * les jetons (verrou compris — un fil de fond goumand s'y verrait).
     * Trame haute avec moteur bas : le coût est au rendu, pas au moteur.
     *
     * @return la durée moteur en nanosecondes, 0 avant le premier dessin
     */
    public long lastFrameEngineNanos() {
        return lastFrameEngineNanos;
    }

    /**
     * Pire trame depuis l'ouverture du scénario courant : les pointes que
     * le tick du panneau peut manquer.
     *
     * @return la durée en nanosecondes, 0 avant le premier dessin
     */
    public long worstFrameNanos() {
        return worstFrameNanos;
    }

    /**
     * Durée de la dernière passe du fil de fond (ouverture à remplissage
     * complet) : la montre du fond.
     *
     * @return la durée en nanosecondes, 0 si aucune passe n'a fini
     */
    public long backgroundFillNanos() {
        return backgroundFill.fillNanos();
    }

    /**
     * Tranches lexées par la dernière passe du fil de fond.
     *
     * @return le nombre de tranches, 0 si aucune passe
     */
    public long backgroundFillSlices() {
        return backgroundFill.sliceCount();
    }

    /**
     * Remet l'odomètre à zéro : la pire trame repart au changement de
     * scénario (la vue le fait d'elle-même à chaque langage).
     */
    public void resetFrameStats() {
        worstFrameNanos = 0L;
    }

    /**
     * Nombre de trames dessinées depuis la construction (diagnostic).
     *
     * @return le nombre de dessins
     */
    public long frameCount() {
        return frameCount;
    }

    /**
     * Cuissons de plans de rendu depuis la dernière invalidation :
     * après quelques trames, le cache travaille et ce compte stagne.
     *
     * @return le nombre de cuissons
     */
    public long renderCacheBakes() {
        return renderCache.bakes();
    }

    /**
     * Reprises par contenu depuis la dernière invalidation : les lignes
     * identiques partagent leur rendu.
     *
     * @return le nombre de reprises
     */
    public long renderCacheKeyHits() {
        return renderCache.keyHits();
    }

    /**
     * Passages par le chemin rapide (identité d'entrée, aucune lecture
     * de texte par trame).
     *
     * @return le nombre de passages
     */
    public long renderCacheSlotHits() {
        return renderCache.slotHits();
    }

    /**
     * Change de langage : le texte est remplacé (une transaction, donc
     * annulable) et un nouveau document de coloration s'ouvre — le
     * haut du document s'affiche, le remplissage repart.
     *
     * @param language descripteur du langage, jamais {@code null}
     * @param text     texte de démonstration, jamais {@code null}
     */
    public void setLanguage(LanguageSyntax language, String text) {
        // le fil de fond s'arrête avant le détachement : sa tranche en
        // cours finit sur l'ancien document, le nouveau partira sien
        backgroundFill.stop();
        syntax.detach();
        try (TextTransaction transaction = undoStack.newTransaction()) {
            transaction.replace(0, buffer.length(), text);
        }
        syntax = SyntaxDocument.open(buffer, language);
        backgroundFill = BackgroundFill.start(syntax);
        resetFrameStats();
        // autre langage : les plans portent des couleurs résolues, tout
        // recuit (sans retokeniser — le nouveau document lexe à la demande)
        renderCache.invalidate();
        scrollY = 0.0f;
        clampCaret();
        ensureCaretVisible();
        ensureFill();
        invalidate();
    }

    /**
     * Change de thème à chaud : aucune retokenisation, les jetons
     * portent des portées, la couleur se résout au dessin.
     *
     * @param newTheme     thème résolu, jamais {@code null}
     * @param newBackground couleur ARGB du fond
     */
    public void applyTheme(SyntaxTheme newTheme, int newBackground) {
        theme = newTheme;
        themeBackground = newBackground;
        // les plans portent des couleurs résolues : re-rendu, jamais
        // retokenisation (la première coloration du panneau ne bouge pas)
        renderCache.invalidate();
        invalidate();
    }

    /**
     * Pile d'annulation de la vue : les boutons de l'hôte l'appellent.
     *
     * @return la pile, jamais {@code null}
     */
    public UndoStack getUndoStack() {
        return undoStack;
    }

    /**
     * Offset du curseur, en unités de code UTF-16.
     *
     * @return la position du curseur dans le texte
     */
    public int getCaretOffset() {
        return caretOffset;
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN;
        return new EditorInputConnection(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                dragging = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!dragging && Math.abs(event.getY() - touchStartY) > touchSlop()) {
                    dragging = true;
                }
                if (dragging) {
                    scrollY = clampToRange(scrollY - (event.getY() - touchStartY),
                            0.0f, maxScroll());
                    touchStartY = event.getY();
                    touchStartX = event.getX();
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (dragging) {
                    dragging = false;
                } else {
                    requestFocus();
                    placeCaretAt(event.getX(), event.getY());
                    clearComposing();
                    showSoftInput();
                }
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long frameBegin = System.nanoTime();
        lastFrameEngineNanos = 0L;
        super.onDraw(canvas);
        canvas.drawColor(themeBackground);
        int count = buffer.getLineCount();
        int first = firstVisibleLine();
        int last = lastVisibleLine();
        for (int line = first; line <= last && line < count; line++) {
            int start = buffer.getLineStart(line);
            int end = buffer.getLineEnd(line);
            float baseline = getPaddingTop() - fontMetrics.top
                    + line * lineHeight - scrollY;
            if (end > start) {
                drawTokens(canvas, line, start, baseline);
            }
            if (composingStart >= 0) {
                drawComposeUnderline(canvas, line, start, end, baseline);
            }
        }
        drawCaret(canvas);
        // la fenêtre visible est payée : le reste remplit au fil de fond
        ensureFill();
        lastFrameNanos = System.nanoTime() - frameBegin;
        worstFrameNanos = Math.max(worstFrameNanos, lastFrameNanos);
        frameCount++;
    }

    /**
     * Dessine la ligne depuis son plan de rendu : le plan se valide par
     * identité d'entrée (aucune lecture de texte sur le chemin rapide),
     * se partage par contenu (les lignes identiques ne cuisent qu'une
     * fois) et porte les couleurs résolues.
     */
    private void drawTokens(Canvas canvas, int line, int start, float baseline) {
        long engineBegin = System.nanoTime();
        SyntaxDocument.LineTokens entry = syntax.lineTokens(line);
        lastFrameEngineNanos += System.nanoTime() - engineBegin;
        int lineEnd = buffer.getLineEnd(line);
        LinePlan plan = renderCache.frame(line, entry,
                () -> buffer.getText(start, lineEnd), this::bakeLine);
        String[] texts = plan.texts();
        int[] colors = plan.colors();
        float[] xs = plan.xs();
        float left = getPaddingLeft();
        for (int at = 0; at < texts.length; at++) {
            textPaint.setColor(colors[at]);
            canvas.drawText(texts[at], left + xs[at], baseline, textPaint);
        }
    }

    /**
     * Cuit le plan d'une ligne : textes tranchés jusqu'au plafond de
     * dessin, couleurs résolues depuis les portées, abscisses en
     * colonnes (la marge s'ajoute au dessin). Une ligne sans jeton
     * (ligne monstre, hors plafond moteur) se cuit en texte brut.
     */
    private LinePlan bakeLine(int line, Object stamp, CharSequence content) {
        SyntaxDocument.Token[] tokens = ((SyntaxDocument.LineTokens) stamp).tokens();
        if (tokens.length == 0) {
            int drawn = Math.min(content.length(), MAX_DRAWN_COLUMNS);
            return new LinePlan(
                    new String[] {content.subSequence(0, drawn).toString()},
                    new int[] {theme.defaultColor()},
                    new float[] {0.0f});
        }
        int kept = 0;
        for (SyntaxDocument.Token token : tokens) {
            if (token.end() <= token.start()) {
                continue;
            }
            if (token.start() >= MAX_DRAWN_COLUMNS) {
                break;
            }
            kept++;
        }
        String[] texts = new String[kept];
        int[] colors = new int[kept];
        float[] xs = new float[kept];
        int at = 0;
        for (SyntaxDocument.Token token : tokens) {
            if (token.end() <= token.start()) {
                continue;
            }
            if (token.start() >= MAX_DRAWN_COLUMNS) {
                break;
            }
            int endColumn = Math.min(token.end(), MAX_DRAWN_COLUMNS);
            texts[at] = content.subSequence(token.start(), endColumn).toString();
            colors[at] = token.scope() == null
                    ? theme.defaultColor() : theme.colorOf(token.scope());
            xs[at] = token.start() * charWidth;
            at++;
        }
        return new LinePlan(texts, colors, xs);
    }

    // ------------------------------------------------------------ interne

    private void drawComposeUnderline(Canvas canvas, int line, int start, int end, float baseline) {
        int from = Math.max(composingStart, start);
        int to = Math.min(composingEnd, end);
        if (to <= from) {
            return;
        }
        float x1 = getPaddingLeft() + (from - start) * charWidth;
        float x2 = getPaddingLeft() + (to - start) * charWidth;
        canvas.drawRect(x1, baseline + dp(1), x2, baseline + dp(3), composePaint);
    }

    private void drawCaret(Canvas canvas) {
        int offset = Math.min(caretOffset, buffer.length());
        int line = buffer.lineOfOffset(offset);
        if (line < firstVisibleLine() || line > lastVisibleLine()) {
            return;
        }
        int column = offset - buffer.getLineStart(line);
        float x = getPaddingLeft() + column * charWidth;
        float top = getPaddingTop() + line * lineHeight - scrollY;
        canvas.drawRect(x, top + dp(2), x + caretWidth, top + lineHeight - dp(2), caretPaint);
    }

    private void placeCaretAt(float x, float y) {
        int line = buffer.getLineCount() - 1;
        if (lineHeight > 0.0f) {
            line = clampToRange(
                    (int) ((y - getPaddingTop() + scrollY) / lineHeight), 0,
                    buffer.getLineCount() - 1);
        }
        int lineStart = buffer.getLineStart(line);
        int lineEnd = buffer.getLineEnd(line);
        int column = charWidth > 0.0f
                ? clampToRange((int) ((x - getPaddingLeft()) / charWidth), 0, lineEnd - lineStart)
                : 0;
        caretOffset = buffer.offsetOfPosition(line, column);
        invalidate();
    }

    /** Amène le curseur dans la fenêtre visible (frappe en bord bas). */
    private void ensureCaretVisible() {
        int line = buffer.lineOfOffset(Math.min(caretOffset, buffer.length()));
        float top = getPaddingTop() + line * lineHeight - scrollY;
        float bottom = top + lineHeight;
        if (top < 0.0f) {
            scrollY += top;
        } else if (bottom > getHeight()) {
            scrollY += bottom - getHeight();
        }
        scrollY = clampToRange(scrollY, 0.0f, maxScroll());
    }

    /** Première ligne visible (indice). */
    private int firstVisibleLine() {
        if (lineHeight <= 0.0f) {
            return 0;
        }
        return clampToRange((int) (scrollY / lineHeight), 0,
                Math.max(0, buffer.getLineCount() - 1));
    }

    /** Dernière ligne visible (indice, bornée au document). */
    private int lastVisibleLine() {
        if (lineHeight <= 0.0f) {
            return 0;
        }
        return clampToRange(
                (int) Math.ceil((scrollY + getHeight()) / lineHeight), 0,
                Math.max(0, buffer.getLineCount() - 1));
    }

    /** Borne haute du défilement : la dernière ligne visible en bas. */
    private float maxScroll() {
        float content = getPaddingTop() + buffer.getLineCount() * lineHeight
                + getPaddingBottom();
        return Math.max(0.0f, content - getHeight());
    }

    /** Seuil de déplacement avant qu'un contact devienne un défilement. */
    private float touchSlop() {
        return dp(8);
    }

    /** Relance le fil de fond si du travail reste (dessin, édition, ouverture). */
    private void ensureFill() {
        backgroundFill.kick();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (backgroundFill != null && backgroundFill.stopped()) {
            backgroundFill = BackgroundFill.start(syntax);
        }
        ensureFill();
    }

    @Override
    protected void onDetachedFromWindow() {
        // annulation douce : la tranche en cours finit, le fil s'arrête
        backgroundFill.stop();
        super.onDetachedFromWindow();
    }

    /**
     * Attend le remplissage complet (harnais, tests) : le fil de fond
     * ne laisse jamais le document incohérent, l'attente n'observe.
     *
     * @param timeoutMillis borne d'attente en millisecondes
     * @return vrai si le préfixe est complet au retour
     */
    public boolean awaitBackgroundFill(long timeoutMillis) {
        return backgroundFill.awaitIdle(timeoutMillis);
    }

    private void applyEdit(int start, int end, CharSequence text) {
        try (TextTransaction transaction = undoStack.newTransaction()) {
            transaction.replace(start, end, text);
        }
        caretOffset = Math.min(start + text.length(), buffer.length());
        invalidate();
    }

    private void clearComposing() {
        if (composingStart < 0) {
            return;
        }
        composingStart = -1;
        composingEnd = -1;
        invalidate();
    }

    private void clampCaret() {
        if (caretOffset > buffer.length()) {
            caretOffset = buffer.length();
        }
    }

    private void showSoftInput() {
        InputMethodManager manager =
                (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.showSoftInput(this, 0);
        }
    }

    private static int clampToRange(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampToRange(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    /**
     * Connexion IME : chaque retour du clavier est une transaction, donc
     * une unité d'annulation. La zone de composition est remplacée à chaque
     * mise à jour de l'IME puis validée telle quelle au commit.
     */
    private static final class EditorInputConnection extends BaseInputConnection {

        private final EditorView editor;

        EditorInputConnection(EditorView target) {
            super(target, false);
            editor = target;
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            int start = editor.composingStart >= 0 ? editor.composingStart : editor.caretOffset;
            int end = editor.composingStart >= 0 ? editor.composingEnd : editor.caretOffset;
            editor.applyEdit(start, end, text);
            editor.clearComposing();
            return true;
        }

        @Override
        public boolean setComposingText(CharSequence text, int newCursorPosition) {
            int start = editor.composingStart >= 0 ? editor.composingStart : editor.caretOffset;
            int end = editor.composingStart >= 0 ? editor.composingEnd : editor.caretOffset;
            editor.applyEdit(start, end, text);
            editor.composingStart = start;
            editor.composingEnd = Math.min(start + text.length(), editor.buffer.length());
            return true;
        }

        @Override
        public boolean deleteSurroundingText(int leftLength, int rightLength) {
            editor.clearComposing();
            int start = Math.max(0, editor.caretOffset - leftLength);
            int end = Math.min(editor.buffer.length(), editor.caretOffset + rightLength);
            if (end > start) {
                editor.applyEdit(start, end, "");
            }
            return true;
        }

        @Override
        public boolean finishComposingText() {
            editor.clearComposing();
            return true;
        }
    }
}
