#!/usr/bin/env python3
"""Renomme les identifiants français du paquet des popups vers l'anglais.

Renommages chirurgicaux : les méthodes se remplacent par leur appel
(les parenthèses suivent), les types et champs par leur nom exact, les
paramètres par leur position de déclaration. Les commentaires en
français restent intacts — la politique l'exige.
"""

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FILES = [
    ROOT / "editor/src/main/java/jo/pigma/editor/EditorView.java",
    ROOT / "editor/src/main/java/jo/pigma/editor/popups/SearchBar.java",
    ROOT / "editor/src/main/java/jo/pigma/editor/popups/GotoBar.java",
    ROOT / "scripts/open-harness/src/OpenHarness.java",
]

# (motif, remplacement) — méthodes appelées ou déclarées, parenthèses comprises
METHODS = [
    (r"\bsurTexte\b", "onText"),
    (r"\bsurComposition\b", "onComposition"),
    (r"\bsurRetourArriere\b", "onBackspace"),
    (r"\bsurFleche\b", "onArrow"),
    (r"\bsurValidation\b", "onEnter"),
    (r"\bsurTouch\b", "onTouchArea"),
    (r"\bmontrerRemplacement\b", "showReplaceRow"),
    (r"\bremplacementMontr\u00e9\b", "replaceRowShown"),
    (r"\bcompteLibell\u00e9\b", "counterLabel"),
    (r"\bretourLibell\u00e9\b", "feedbackLabel"),
    (r"\bchampRecherche\b", "focusQueryField"),
    (r"\bremplacerCourant\b", "replaceCurrent"),
    (r"\bremplacerTout\b", "replaceEverywhere"),
    (r"\brequ\u00eate\b(?=\()", "queryText"),
    (r"\bremplacement\b(?=\()", "replacementText"),
    (r"\bmontrer\b(?=\()", "show"),
    (r"\bcacher\b(?=\()", "hide"),
    (r"\bestMontr\u00e9e\b(?=\()", "isShown"),
    (r"\bfermer\b(?=\()", "close"),
    (r"\bhauteur\b(?=\()", "height"),
    (r"\blargeur\b(?=\()", "width"),
    (r"\bchiffres\b(?=\()", "digitsText"),
    (r"\bcarteOuverte\b(?=\()", "openCard"),
]

# (motif, remplacement) — types, champs, variables : nom exact, mot entier
IDENTIFIERS = [
    (r"\bCarteClavier\b", "KeyboardCard"),
    (r"\bdownSurCarte\b", "downOnCard"),
    (r"\bpremi\u00e8reAncreX\b", "firstAnchorX"),
]

# paramètre « texte » des méthodes du contrat clavier : déclaration puis usages
TEXT_PARAM = [
    (r"\(CharSequence texte\)", "(CharSequence text)"),
    (r"\btexte\.", "text."),
    (r"\btexte\)", "text)"),
    (r"\btexte ==\b", "text =="),
    (r"== null\) \|\| texte\b", "== null) || text"),
    (r"\btexte\.length\(\)", "text.length()"),
]

for path in FILES:
    source = path.read_text(encoding="utf-8")
    for pattern, repl in METHODS + IDENTIFIERS + TEXT_PARAM:
        source = re.sub(pattern, repl, source)
    path.write_text(source, encoding="utf-8")
    print("renommé :", path.name)
