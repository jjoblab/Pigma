#!/usr/bin/env python3
"""Génère le corpus Lua de 2000 lignes du module syntax.

Le fichier produit (lua-2000.lua) nourrit LexersRestartTest : des
constructions variées y traversent les lignes (longues chaînes à
niveaux, longues commentaires) au milieu de code ordinaire, pour
éprouver le transport d'état de fenêtre en fenêtre. Déterministe :
deux exécutions produisent le même octets.
"""

from pathlib import Path

TARGET = Path(__file__).resolve().parent.parent / "syntax" / "src" / "test" / "resources" / "data" / "lua-2000.lua"
BLOCS = 76


def build() -> str:
    lines = []
    lines.append("-- Corpus Lua de 2000 lignes : constructions variées,")
    lines.append("-- longues chaînes et commentaires qui traversent les fenêtres.")
    lines.append("")
    lines.append("local corpus = {}")
    for bloc in range(BLOCS):
        lines.append("")
        lines.append(f"--[==[ Bloc {bloc} : longue commentaire à niveau 2.")
        lines.append("    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]")
        lines.append(f"local bloc{bloc} = {{")
        for i in range(8):
            value = (bloc * 8 + i) * 3 + 1
            lines.append(
                f"  cle{i} = {value}, -- hexa {hex(value)}, flottant {value / 7:.3f},"
                f" exposant {value}e-2")
        lines.append("}")
        lines.append(f"function corpus.somme{bloc}(a, b)")
        lines.append("  local total = (a or 0) + (b or 0)")
        lines.append("  if total > 100 then")
        lines.append("    total = total // 2")
        lines.append("  else")
        lines.append("    total = total & 0xFF")
        lines.append("  end")
        lines.append("  return total")
        lines.append("end")
        lines.append(f"local texte{bloc} = [[")
        lines.append(f"Longue chaîne du bloc {bloc}, ligne une.")
        lines.append("Ligne deux : \\n reste écrit tel quel, sans fermeture.")
        lines.append("]]")
        if bloc % 5 == 0:
            lines.append(f"local longue{bloc} = [==[")
            lines.append(f"Niveau 2 du bloc {bloc} : ]] et ]=] restent dans la chaîne.")
            lines.append("]==]")
        if bloc % 7 == 0:
            lines.append(f"  if bloc{bloc}.cle1 ~= nil then")
            lines.append(f"    print('bloc {bloc}', \"total\", bloc{bloc}.cle1)")
            lines.append("  end")
    lines.append("")
    lines.append("return corpus")
    return "\n".join(lines) + "\n"


def main() -> None:
    content = build()
    line_count = content.count("\n")
    TARGET.write_text(content, encoding="utf-8")
    print(f"{TARGET} : {line_count} lignes, {len(content)} octets")


if __name__ == "__main__":
    main()
