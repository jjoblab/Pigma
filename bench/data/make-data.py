#!/usr/bin/env python3
"""Genere les corpus de 2000 lignes du banc E0, de facon deterministe.

Chaque fichier reproduit des motifs realistes (classes, elements,
objets) sans aleatoire : deux executions produisent le meme contenu,
les mesures et les jetons sont reproductibles.

Usage : python3 bench/data/make-data.py  (depuis la racine du depot).
"""

import pathlib

ROOT = pathlib.Path(__file__).resolve().parent

JAVA_BLOCK = """\
    /**
     * Calcule le poids du lot {index} sur {size} lignes.
     *
     * @param seed germe du calcul
     * @return le poids arrondi
     */
    public double weight{index}(int seed) {{
        // fast path : petit germe
        if (seed < 0b1010 && seed != -1) {{
            return seed * {ratio}f;
        }}
        /* chemin lent : somme ponderee */
        long hex = 0x{hex:04X}L;
        double total = 0.0;
        for (int i = 0; i < seed; i++) {{
            total += (i % {mod}) / {div};
        }}
        String label = "lot-{index}\\t\\u263A";
        System.out.printf("%s = %.3f%n", label, total + hex);
        return total >= {limit} ? {limit} : total;
    }}
"""


def make_java(lines=2000):
    body = []
    index = 0
    while sum(part.count("\n") for part in body) + 10 < lines:
        body.append(
            JAVA_BLOCK.format(
                index=index,
                size=lines,
                ratio=index % 7 + 1,
                hex=(index * 37) % 0xFFFF,
                mod=index % 13 + 2,
                div=(index % 5 + 2),
                limit=index % 11 + 1,
            )
        )
        index += 1
    header = [
        "package bench;",
        "",
        "import java.util.List;",
        "import java.util.Map;",
        "",
        "/**",
        " * Corpus de 2000 lignes du banc E0 : motifs repetitifs, jetons varies.",
        " */",
        "public final class Corpus {",
        "",
        "    private static final int MAX = 1000000;",
        "    private static final double STEP = 0.125e-3;",
        "",
    ]
    footer = ["}", ""]
    text = "\n".join(header) + "\n" + "".join(body) + "\n".join(footer)
    while text.count("\n") < lines:
        text += "    // ligne de remplissage pour atteindre le compte\n"
    return text


XML_ITEM = """\
    <!-- entree {index} du catalogue, poids {weight} -->
    <item id="{index}" position="{index}" poids="{weight}" actif="{active}">
        <nom>lot-{index}</nom>
        <description>description de l'entree {index}, avec ponctuation et accents</description>
        <limites min="{neg}" max="{limit}" ratio="{ratio}"/>
        <raw><![CDATA[contenu brut {index} : <balise> & symbole]]></raw>
        <entites amp="&amp;" lt="&lt;" code="&#169;" hex="&#x263A;"/>
    </item>
"""


def make_xml(lines=2000):
    body = []
    index = 1
    while sum(part.count("\n") for part in body) + 8 < lines:
        body.append(
            XML_ITEM.format(
                index=index,
                neg=-index,
                weight=round(index * 0.5, 1),
                active="true" if index % 2 else "false",
                limit=index * 10,
                ratio=round(index / 8, 3),
            )
        )
        index += 1
    header = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        '<!DOCTYPE catalogue SYSTEM "catalogue.dtd">',
        '<?instructions mode="corpus" ?>',
        "<catalogue id=\"corpus-e0\" version=\"1\">",
        "",
    ]
    footer = ["</catalogue>", "<!-- fin du corpus -->", ""]
    text = "\n".join(header) + "\n" + "".join(body) + "\n".join(footer)
    while text.count("\n") < lines:
        text += "    <!-- ligne de remplissage -->\n"
    return text


JSON_ITEM = """\
    {{
        "id": {index},
        "nom": "lot-{index}",
        "actif": {active},
        "annule": null,
        "poids": {weight},
        "rapport": {ratio}e-{exp},
        "mesures": [{index}, {double}, {neg}, {big}],
        "texte": "description \\"{index}\\" avec \\u00e9 et \\t",
        "detail": {{
            "min": {neg},
            "max": {limit},
            "liste": [{index}, {index2}, {index3}],
            "vide": {{}}
        }}
    }},
"""


def make_json(lines=2000):
    body = []
    index = 1
    while sum(part.count("\n") for part in body) + 4 < lines:
        body.append(
            JSON_ITEM.format(
                index=index,
                index2=index * 2,
                index3=index * 3,
                active="true" if index % 2 else "false",
                weight=round(index * 0.25, 2),
                ratio=index % 9 + 1,
                exp=index % 5 + 1,
                double=round(index / 4, 3),
                neg=-index,
                big=index * 1000000,
                limit=index * 10,
            )
        )
        index += 1
    header = ["[", ""]
    footer = ["", "]", ""]
    used = sum(part.count("\n") for part in body) + len(header) + len(footer)
    for _ in range(max(0, lines - used)):
        body.append("    0,\n")
    body[-1] = body[-1].rstrip().rstrip(",")
    text = "\n".join(header) + "\n" + "".join(body) + "\n".join(footer)
    return text


def main():
    (ROOT / "java-2000.java").write_text(make_java(), encoding="utf-8")
    (ROOT / "xml-2000.xml").write_text(make_xml(), encoding="utf-8")
    (ROOT / "json-2000.json").write_text(make_json(), encoding="utf-8")
    for name in ("java-2000.java", "xml-2000.xml", "json-2000.json"):
        content = (ROOT / name).read_text(encoding="utf-8")
        print(name, content.count("\n") + 1, "lines", len(content), "chars")


if __name__ == "__main__":
    main()
