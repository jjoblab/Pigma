#!/usr/bin/env python3
"""Chantiers 48-50 : input/, folding/, selection/ sortent d'EditorView.
- input/ : EditorContract (pont interne) + EditorInputConnection extraite.
- folding/ : FoldModel porte l'état des plis et ses caches.
- selection/ : SelectionRenderer porte le pinceau et le dessin.
Chaque remplacement vérifie son unicité ; les textes exacts viennent
des lectures de session."""
import re
from pathlib import Path

EDITOR = Path("editor/src/main/java/jo/pigma/editor/EditorView.java")
VIEW = EDITOR.read_text(encoding="utf-8")


def swap(old, new):
    global VIEW
    assert VIEW.count(old) == 1, f"introuvable ou multiple : {old[:60]!r} ({VIEW.count(old)})"
    VIEW = VIEW.replace(old, new)


# --- imports des trois paquets
swap("import jo.pigma.editor.gutter.Gutter;",
     "import jo.pigma.editor.folding.FoldModel;\n"
     "import jo.pigma.editor.gutter.Gutter;\n"
     "import jo.pigma.editor.input.EditorContract;\n"
     "import jo.pigma.editor.input.EditorInputConnection;\n"
     "import jo.pigma.editor.selection.SelectionRenderer;\n")

# --- champs : plis, sélection, pont clavier
swap("""    /** En-têtes de plis repliés (indices de lignes). */
    private final TreeSet<Integer> folded = new TreeSet<>();

    /** Lignes cachées par les plis, null quand rien n'est replié. */
    private boolean[] hiddenByFold;

    /** Lignes visibles strictement avant chaque ligne (les plis compactent). */
    private int[] visibleBefore;

    /** Lignes cachées en cours (le panneau le lit). */
    private int hiddenLineCount;
""", """    /** Les plis — l'état et ses caches au paquet folding/. */
    private final FoldModel folds = new FoldModel();

    /** Le rendu de la sélection — pinceau et dessin au paquet selection/. */
    private final SelectionRenderer selectionRenderer = new SelectionRenderer();

    /** Le texte vu du paquet selection (contrat interne, hors API). */
    private final SelectionRenderer.Lines selectionLines =
            new SelectionRenderer.Lines() {
                @Override
                public int lineStart(int line) {
                    return buffer.getLineStart(line);
                }

                @Override
                public int lineEnd(int line) {
                    return buffer.getLineEnd(line);
                }

                @Override
                public boolean isHidden(int line) {
                    return folds.isHidden(line);
                }

                @Override
                public int visibleIndexOf(int line) {
                    return EditorView.this.visibleIndexOf(line);
                }
            };

    /** Le pont du clavier (contrat interne du paquet input, hors API). */
    private EditorContract inputContract;
""")

# --- le pinceau de sélection part chez le rendu
needle = "    private final Paint selectionPaint = new Paint();\n"
at = VIEW.index(needle)
start = at
prefix = VIEW[:at]
if prefix.endswith("    */\n"):
    j = prefix.rfind("    /**\n")
    if j >= 0:
        start = j
VIEW = VIEW[:start] + VIEW[at + len(needle):]
assert "selectionPaint = new Paint()" not in VIEW
swap("""        selectionPaint.setStyle(Paint.Style.FILL);
        selectionPaint.setColor(0x332979FF);
""", "")

# --- le dessin de la sélection délègue
swap("""    private void drawSelection(Canvas canvas, int first, int last, int count) {
        if (!hasSelection()) {
            return;
        }
        int from = selectionStart();
        int to = selectionEnd();
        for (int line = first; line <= last && line < count; line++) {
            if (isHidden(line)) {
                continue;
            }
            int lineStart = buffer.getLineStart(line);
            int lineEnd = buffer.getLineEnd(line);
            int start = Math.max(from, lineStart);
            int end = Math.min(to, lineEnd);
            if (end <= start) {
                continue;
            }
            float left = getPaddingLeft() + (start - lineStart) * charWidth;
            float right = getPaddingLeft() + (end - lineStart) * charWidth;
            float top = getPaddingTop() + visibleIndexOf(line) * lineHeight - scrollY;
            canvas.drawRect(left, top, right, top + lineHeight, selectionPaint);
        }
    }
""", """    private void drawSelection(Canvas canvas, int first, int last, int count) {
        if (!hasSelection()) {
            return;
        }
        selectionRenderer.draw(canvas, selectionLines, selectionStart(),
                selectionEnd(), first, last, count,
                new SelectionRenderer.Geometry(getPaddingLeft(), getPaddingTop(),
                        lineHeight, charWidth, scrollY));
    }
""")

# --- les plis : le modèle délègue (les corps exacts partent)
swap("""    private void revealCaret() {
        if (hiddenByFold == null) {
            return;
        }
        int line = buffer.lineOfOffset(Math.min(caretOffset, buffer.length()));
        // la reconstruction peut annuler le tableau (dernier pli ouvert) :
        // la garde se relit à chaque tour
        while (hiddenByFold != null && line < hiddenByFold.length
                && hiddenByFold[line]) {
            int container = -1;
            for (int header : folded) {
                if (header < line && line <= structure.foldEndLine(header)) {
                    container = header;
                }
            }
            if (container < 0) {
                return;
            }
            folded.remove(container);
            rebuildFoldLayout();
        }
    }
""", """    private void revealCaret() {
        if (!folds.hasFolds()) {
            return;
        }
        int line = buffer.lineOfOffset(Math.min(caretOffset, buffer.length()));
        // la reconstruction peut annuler les plis (dernier ouvert) :
        // la garde se relit à chaque tour
        while (folds.isHidden(line)) {
            int container = folds.headerHiding(line, structure);
            if (container < 0) {
                return;
            }
            folds.unfold(container);
            rebuildFoldLayout();
        }
    }
""")

swap("""    private void rebuildFoldLayout() {
        int count = buffer.getLineCount();
        trackedLineCount = count;
        if (folded.isEmpty()) {
            hiddenByFold = null;
            visibleBefore = null;
            hiddenLineCount = 0;
            return;
        }
        boolean[] hidden = new boolean[count];
        for (int header : folded) {
            if (header < 0 || header >= count || !structure.foldStartsAt(header)) {
                continue; // l'édition a déplacé la structure : plus d'ouvrant
            }
            int end = structure.foldEndLine(header);
            for (int line = header + 1; line <= end && line < count; line++) {
                hidden[line] = true;
            }
        }
        int[] before = new int[count];
        int visible = 0;
        for (int line = 0; line < count; line++) {
            before[line] = visible;
            if (!hidden[line]) {
                visible++;
            }
        }
        hiddenByFold = hidden;
        visibleBefore = before;
        hiddenLineCount = count - visible;
    }
""", """    private void rebuildFoldLayout() {
        trackedLineCount = buffer.getLineCount();
        folds.rebuild(structure, trackedLineCount);
    }
""")

swap("""    /** La ligne est-elle cachée par un pli. */
    private boolean isHidden(int line) {
        return hiddenByFold != null && line < hiddenByFold.length
                && hiddenByFold[line];
    }
""", """    /** La ligne est-elle cachée par un pli. */
    private boolean isHidden(int line) {
        return folds.isHidden(line);
    }
""")

swap("""    /** L'indice visible d'une ligne (les plis compactent la hauteur). */
    private int visibleIndexOf(int line) {
        return visibleBefore == null ? line : visibleBefore[line];
    }
""", """    /** L'indice visible d'une ligne (les plis compactent la hauteur). */
    private int visibleIndexOf(int line) {
        return folds.visibleIndexOf(line);
    }
""")

swap("""    public int hiddenLineCount() {
        return hiddenLineCount;
    }
""", """    public int hiddenLineCount() {
        return folds.hiddenLineCount();
    }
""")

swap("        return buffer.getLineCount() - hiddenLineCount;",
     "        return buffer.getLineCount() - folds.hiddenLineCount();")
swap("        int visible = visibleBefore == null ? line : visibleBefore[line];",
     "        int visible = folds.visibleIndexOf(line);")

# --- foldedCount et le décalage des plis
swap("        return folded.size();", "        return folds.count();")
swap("""    private void shiftFolds(int start) {
        if (folded.isEmpty()) {
            trackedLineCount = buffer.getLineCount();
            return;
        }
        int line = buffer.lineOfOffset(start);
        int delta = buffer.getLineCount() - trackedLineCount;
        TreeSet<Integer> shifted = new TreeSet<>();
        for (int header : folded) {
            int moved = header < line ? header : header + delta;
            if (moved >= 0 && moved < buffer.getLineCount()) {
                shifted.add(moved);
            }
        }
        folded.clear();
        folded.addAll(shifted);
        rebuildFoldLayout();
        revealCaret();
    }
""", """    private void shiftFolds(int start) {
        if (!folds.hasFolds()) {
            trackedLineCount = buffer.getLineCount();
            return;
        }
        int line = buffer.lineOfOffset(start);
        int delta = buffer.getLineCount() - trackedLineCount;
        folds.shift(line, delta, buffer.getLineCount());
        rebuildFoldLayout();
        revealCaret();
    }
""")

# --- les renoms résiduels des plis (toggleFoldAt, drawStructure)
assert VIEW.count("folded.clear();") >= 2
VIEW = VIEW.replace("folded.clear();", "folds.clear();")
VIEW = VIEW.replace("folded.contains(", "folds.isFolded(")
VIEW = VIEW.replace("folded.remove(", "folds.unfold(")
VIEW = VIEW.replace("folded.add(", "folds.fold(")
assert "folded" not in VIEW.replace("foldedCount", ""), "un folded survit"

# --- la connexion IME : la classe imbriquée part au paquet input
VIEW = re.sub(r"\n    /\*\*\n     \* Connexion IME :.*?\n    \}\n\}\n", "\n}\n",
              VIEW, count=1, flags=re.S)
assert "EditorInputConnection extends BaseInputConnection" not in VIEW
swap("        return new EditorInputConnection(this);",
     "        return new EditorInputConnection(this, inputContract);")

swap("""    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN;
        return new EditorInputConnection(this, inputContract);
    }
""", """    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN;
        return new EditorInputConnection(this, inputContract);
    }

    /**
     * Le pont du clavier vers l'éditeur : le contrat interne du paquet
     * input — la classe anonyme lit les membres privés de la vue, rien
     * ne se publie (l'API publique reste petite).
     */
    private EditorContract createInputContract() {
        return new EditorContract() {
            @Override
            public int caret() {
                return caretOffset;
            }

            @Override
            public int composingStart() {
                return composingStart;
            }

            @Override
            public int composingEnd() {
                return composingEnd;
            }

            @Override
            public void setComposing(int start, int end) {
                composingStart = start;
                composingEnd = end;
            }

            @Override
            public boolean hasSelection() {
                return EditorView.this.hasSelection();
            }

            @Override
            public int selectionStart() {
                return EditorView.this.selectionStart();
            }

            @Override
            public int selectionEnd() {
                return EditorView.this.selectionEnd();
            }

            @Override
            public CharSequence selectedText() {
                return EditorView.this.selectedText();
            }

            @Override
            public int length() {
                return buffer.length();
            }

            @Override
            public void applyEdit(int start, int end, CharSequence text) {
                EditorView.this.applyEdit(start, end, text);
            }

            @Override
            public void clearComposing() {
                EditorView.this.clearComposing();
            }

            @Override
            public void deleteAround(int left, int right) {
                EditorView.this.deleteAround(left, right);
            }

            @Override
            public void moveCaret(int delta) {
                EditorView.this.moveCaret(delta);
            }

            @Override
            public void moveCaretVertically(int delta) {
                EditorView.this.moveCaretVertically(delta);
            }

            @Override
            public void setSelection(int start, int end) {
                EditorView.this.setSelection(start, end);
            }
        };
    }
""")

# --- le pont se pose au constructeur
swap("        gutter = new Gutter(dp(1));\n",
     "        gutter = new Gutter(dp(1));\n"
     "        inputContract = createInputContract();\n")

EDITOR.write_text(VIEW, encoding="utf-8")
print("input/, folding/, selection/ branchés :", len(VIEW.splitlines()), "lignes")
