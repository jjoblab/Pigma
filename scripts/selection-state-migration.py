#!/usr/bin/env python3
"""Chantier 52 : l'état de sélection migre au paquet selection/.
Le curseur et l'ancre quittent EditorView pour SelectionState ; les
accesseurs publics délèguent, les 59 sites se recâblent par regex
mono-ligne (les affectations d'abord, les lectures ensuite)."""
import re
from pathlib import Path

EDITOR = Path("editor/src/main/java/jo/pigma/editor/EditorView.java")
VIEW = EDITOR.read_text(encoding="utf-8")


def swap(old, new):
    global VIEW
    assert VIEW.count(old) == 1, f"introuvable ou multiple : {old[:60]!r} ({VIEW.count(old)})"
    VIEW = VIEW.replace(old, new)


# --- import
swap("import jo.pigma.editor.selection.SelectionRenderer;",
     "import jo.pigma.editor.selection.SelectionRenderer;\n"
     "import jo.pigma.editor.selection.SelectionState;\n")

# --- les champs partent (composing reste : état IME, déjà isolé par
#     le contrat du paquet input)
swap("""    private int caretOffset;
    private int composingStart = -1;
    private int composingEnd = -1;

    /** L'ancre de sélection (-1 : pas de sélection, le curseur seul). */
    private int selectionAnchor = -1;
""", """    private int composingStart = -1;
    private int composingEnd = -1;

    /** L'état de sélection — le curseur et l'ancre au paquet selection/. */
    private final SelectionState selection = new SelectionState();
""")

# --- les accesseurs dérivés délèguent (avant les recâblages : leurs
#     corps lisent encore les champs)
swap("""    public boolean hasSelection() {
        return selectionAnchor >= 0 && selectionAnchor != caretOffset;
    }
""", """    public boolean hasSelection() {
        return selection.hasSelection();
    }
""")
swap("""    public int selectionStart() {
        return selectionAnchor < 0
                ? caretOffset : Math.min(selectionAnchor, caretOffset);
    }
""", """    public int selectionStart() {
        return selection.start();
    }
""")
swap("""    public int selectionEnd() {
        return selectionAnchor < 0
                ? caretOffset : Math.max(selectionAnchor, caretOffset);
    }
""", """    public int selectionEnd() {
        return selection.end();
    }
""")

# --- les affectations (mono-lignes, vérifiées) deviennent des appels
VIEW, n1 = re.subn(r"^(\s*)caretOffset = (.+);$", r"\1selection.setCaret(\2);",
                   VIEW, flags=re.M)
VIEW, n2 = re.subn(r"^(\s*)selectionAnchor = (.+);$", r"\1selection.setAnchor(\2);",
                   VIEW, flags=re.M)
print("affectations recâblées :", n1, "carets,", n2, "ancres")

# --- les lectures restantes
before = VIEW.count("caretOffset") + VIEW.count("selectionAnchor")
VIEW = VIEW.replace("caretOffset", "selection.caret()")
VIEW = VIEW.replace("selectionAnchor", "selection.anchor()")
after = VIEW.count("caretOffset") + VIEW.count("selectionAnchor")
assert after == 0, "un champ survit"
print("lectures recâblées :", before)

EDITOR.write_text(VIEW, encoding="utf-8")
print("état de sélection migré :", len(VIEW.splitlines()), "lignes")
