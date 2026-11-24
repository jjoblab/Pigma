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

/**
 * Vue d'édition minimale : le texte affiché est le tampon du cœur, chaque
 * retour de l'IME est une transaction (une unité d'annulation), le
 * curseur se place au toucher.
 *
 * <p>Le rendu est volontairement rudimentaire : police à chasse fixe, une
 * ligne visuelle par ligne du tampon, curseur rectangulaire, soulignement
 * de la zone de composition. Le défilement, la sélection et la gouttière
 * viendront avec la vue complète de la bibliothèque.</p>
 */
public final class EditorView extends View {

    private static final String INITIAL_TEXT = """
            package exemple;

            public final class Demo {
                // Saisie, annulation, rétablissement : le minimum vital.
                static int somme(int a, int b) {
                    return a + b;
                }
            }
            """;

    private static final int TEXT_COLOR = 0xFF1B1B1F;
    private static final int CARET_COLOR = 0xFF2962FF;
    private static final int COMPOSE_COLOR = 0xFF7A8199;

    private final TextBuffer buffer;
    private final UndoStack undoStack;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint caretPaint = new Paint();
    private final Paint composePaint = new Paint();
    private final Paint.FontMetrics fontMetrics;
    private final float charWidth;
    private final float lineHeight;
    private final float caretWidth;

    private int caretOffset;
    private int composingStart = -1;
    private int composingEnd = -1;

    private final TextListener modelListener = new TextListener() {
        @Override
        public void textChanged(TextBuffer changed, int start, int removedLength,
                                int insertedLength, int revision) {
            clampCaret();
            invalidate();
        }
    };

    public EditorView(Context context) {
        super(context);
        buffer = TextBuffer.create(INITIAL_TEXT);
        undoStack = new UndoStack(buffer);
        textPaint.setTypeface(Typeface.MONOSPACE);
        textPaint.setColor(TEXT_COLOR);
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
     * Pile d'annulation de la vue : les boutons de l'hôte l'appellent.
     *
     * @return la pile d'annulation, jamais {@code null}
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
        if (event.getAction() == MotionEvent.ACTION_UP) {
            requestFocus();
            placeCaretAt(event.getX(), event.getY());
            clearComposing();
            showSoftInput();
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int line = 0; line < buffer.getLineCount(); line++) {
            int start = buffer.getLineStart(line);
            int end = buffer.getLineEnd(line);
            float baseline = getPaddingTop() - fontMetrics.top + line * lineHeight;
            if (end > start) {
                canvas.drawText(buffer.getText(start, end), getPaddingLeft(), baseline, textPaint);
            }
            if (composingStart >= 0) {
                drawComposeUnderline(canvas, line, start, end, baseline);
            }
        }
        drawCaret(canvas);
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
        int column = offset - buffer.getLineStart(line);
        float x = getPaddingLeft() + column * charWidth;
        float top = getPaddingTop() + line * lineHeight;
        canvas.drawRect(x, top + dp(2), x + caretWidth, top + lineHeight - dp(2), caretPaint);
    }

    private void placeCaretAt(float x, float y) {
        int line = buffer.getLineCount() - 1;
        if (lineHeight > 0.0f) {
            line = clampToRange((int) ((y - getPaddingTop()) / lineHeight), 0,
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
