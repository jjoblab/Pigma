package jo.pigma.editor.popups;

import java.util.ArrayList;
import java.util.List;

/**
 * LE REGISTRE DES EXTRAITS (les extraits du rapport des popups — les « live
 * templates » de l'atelier portés au tactile) : les squelettes que
 * la carte des extraits propose — l'ABRÉVIATION se lit à gauche,
 * l'APERÇU du corps à droite, le choix INSCRIT le squelette au
 * caret. Le CHAMP À REMPLIR y vit : le gabarit porte son trou entre
 * GUILLEMETS (« champ » — l'usine de l'extrait y décale les bornes),
 * le champ se pose SÉLECTIONNÉ au caret (la frappe le remplace).
 *
 * <p>Le registre PART vide : l'HÔTE y pose ses squelettes (les
 * squelettes du langage — l'hôte sait son langage, le rapport y
 * dit « l'hôte y vit »). {@link #standard(String)} y rend les
 * squelettes des langages de la démonstration (Java, Lua, SQL).</p>
 */
public final class SnippetRegistry {

    /**
     * Un extrait : son abréviation (le nom court qui se lit), son
     * corps (le squelette entier, trou délimité), les bornes du champ
     * à remplir dans le corps.
     *
     * @param label    l'abréviation (« for », « fun »)
     * @param description ce que l'extrait pose
     * @param body     le corps du squelette (guillemets ôtés)
     * @param fieldAt  le début du champ dans le corps (la sélection
     *                 y pose son curseur)
     * @param fieldEnd la fin du champ dans le corps (le champ se
     *                 pose sélectionné — la frappe le remplace)
     */
    public record Snippet(String label, String description, String body,
            int fieldAt, int fieldEnd) {
    }

    /** Les extraits enregistrés, l'ordre d'inscription y vit. */
    private final List<Snippet> snippets = new ArrayList<>();

    /**
     * L'extrait s'y inscrit (le même libellé se REMPLACE — l'hôte
     * repose sa version).
     *
     * @param snippet l'extrait à inscrire, jamais null
     */
    public void register(Snippet snippet) {
        if (snippet == null || snippet.label() == null
                || snippet.label().isEmpty() || snippet.body() == null) {
            return;
        }
        for (int at = 0; at < snippets.size(); at++) {
            if (snippets.get(at).label().equals(snippet.label())) {
                snippets.set(at, snippet);
                return;
            }
        }
        snippets.add(snippet);
    }

    /**
     * Les extraits enregistrés (le harnais les lit).
     *
     * @return les extraits, jamais null
     */
    public List<Snippet> snippets() {
        return List.copyOf(snippets);
    }

    /** Le registre se vide. */
    public void clear() {
        snippets.clear();
    }

    /**
     * L'USINE d'un extrait : le GABARIT porte son trou entre
     * guillemets (« champ ») — les bornes du trou s'y décalent, le
     * corps s'y écrit sans les guillemets. Sans trou : le champ
     * tombe au bout du corps (le curseur s'y pose, rien de sélectionné).
     *
     * @param label    l'abréviation de l'extrait
     * @param description ce qu'il pose
     * @param template  le gabarit (son trou entre guillemets)
     * @return l'extrait né du gabarit
     */
    public static Snippet of(String label, String description,
            String template) {
        int open = template.indexOf('«');
        int close = template.indexOf('»');
        if (open < 0 || close < 0 || close < open) {
            return new Snippet(label, description, template,
                    template.length(), template.length());
        }
        String body = template.substring(0, open)
                + template.substring(open + 1, close)
                + template.substring(close + 1);
        return new Snippet(label, description, body, open, close - 1);
    }

    /**
     * LES EXTRAITS D'UN LANGAGE de la démonstration : Java (le for
     * borné, le if, le while, le try/catch, la fonction), Lua (le for
     * numérique, le if … then, le while … do, la fonction locale, la
     * variable), SQL (la lecture, l'écriture). Langage inconnu : AUCUN
     * (l'hôte y pose les siens).
     *
     * @param language le nom affichable du langage (« Java », « Lua »,
     *                 « SQL » — celui du descripteur)
     * @return les extraits du langage, jamais null
     */
    public static List<Snippet> standard(String language) {
        List<Snippet> found = new ArrayList<>();
        if ("Java".equals(language)) {
            found.add(of("for", "boucle bornée",
                    "for (int «i» = 0; i < 10; i++) {\n    \n}"));
            found.add(of("if", "branche conditionnelle",
                    "if («true») {\n    \n}"));
            found.add(of("while", "boucle de condition",
                    "while («true») {\n    \n}"));
            found.add(of("try", "bloc surveillé",
                    "try {\n    \n} catch («Exception e») {\n    \n}"));
            found.add(of("fun", "fonction privée",
                    "private int «nom»() {\n    return 0;\n}"));
        } else if ("Lua".equals(language)) {
            found.add(of("for", "boucle numérique",
                    "for «i» = 1, 10 do\n    \nend"));
            found.add(of("if", "branche conditionnelle",
                    "if «true» then\n    \nend"));
            found.add(of("while", "boucle de condition",
                    "while «true» do\n    \nend"));
            found.add(of("fun", "fonction locale",
                    "local function «nom»()\n    \nend"));
            found.add(of("local", "variable locale",
                    "local «nom» = "));
        } else if ("SQL".equals(language)) {
            found.add(of("select", "requête de lecture",
                    "SELECT *\nFROM «table»\nWHERE colonne = "));
            found.add(of("insert", "requête d'écriture",
                    "INSERT INTO «table» (colonne)\nVALUES ()"));
        }
        return found;
    }
}
