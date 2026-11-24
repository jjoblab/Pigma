package jo.pigma.syntax.generic;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Profil d'un langage de la famille générique : de la donnée, pas du
 * code. Un lexeur {@link GenericLexer} armé du profil sert la longue
 * traîne des petits langages — fichiers de configuration, scripts,
 * SQL — sans description dédiée ni génération.
 *
 * <p>Le profil est immuable et sain d'office : les délimiteurs sont
 * des paires exactes, les mots-clés normalisés en minuscules quand la
 * casse est ignorée.</p>
 *
 * @param name              nom affichable du langage profilé
 * @param lineComments      préfixes de commentaire de fin de ligne
 *                          (« -- », « # »…), éventuellement aucun
 * @param blockCommentOpen  ouvrant de commentaire de bloc (« "* »),
 *                          {@code null} si le langage n'en a pas
 * @param blockCommentClose fermant de commentaire de bloc,
 *                          {@code null} sans ouvrant
 * @param stringPairs       paires de délimiteurs de chaîne (chaque
 *                          paire a exactement deux caractères)
 * @param escapeChar        caractère d'échappement des chaînes, 0
 *                          pour aucun
 * @param keywords          mots-clés, comparés selon
 *                          {@link #caseSensitive}
 * @param caseSensitive     vrai si la casse distingue les mots-clés
 */
public record LexerProfile(String name, List<String> lineComments,
        String blockCommentOpen, String blockCommentClose,
        List<String> stringPairs, char escapeChar, Set<String> keywords,
        boolean caseSensitive) {

    /**
     * Le constructeur canonique valide et normalise.
     *
     * @param name              nom affichable
     * @param lineComments      préfixes de commentaire de ligne
     * @param blockCommentOpen  ouvrant de bloc
     * @param blockCommentClose fermant de bloc
     * @param stringPairs       paires de délimiteurs de chaîne
     * @param escapeChar        caractère d'échappement
     * @param keywords          mots-clés
     * @param caseSensitive     sensibilité à la casse
     */
    public LexerProfile {
        Objects.requireNonNull(name);
        Objects.requireNonNull(lineComments);
        Objects.requireNonNull(stringPairs);
        Objects.requireNonNull(keywords);
        lineComments = List.copyOf(lineComments);
        stringPairs = List.copyOf(stringPairs);
        for (String pair : stringPairs) {
            if (pair == null || pair.length() != 2) {
                throw new IllegalArgumentException("invalid string pair: " + pair);
            }
        }
        if ((blockCommentOpen == null) != (blockCommentClose == null)) {
            throw new IllegalArgumentException(
                    "block comment needs both open and close");
        }
        if (!caseSensitive) {
            Set<String> lowered = new HashSet<>();
            for (String keyword : keywords) {
                lowered.add(keyword.toLowerCase(Locale.ROOT));
            }
            keywords = Set.copyOf(lowered);
        } else {
            keywords = Set.copyOf(keywords);
        }
    }

    /**
     * Le mot donné (bornes relatives au texte) est-il un mot-clé ?
     *
     * @param text texte porteur
     * @param at   début du mot
     * @param end  fin du mot
     * @return vrai si le profil connaît ce mot-clé
     */
    public boolean isKeywordAt(CharSequence text, int at, int end) {
        String word = text.subSequence(at, end).toString();
        if (!caseSensitive) {
            word = word.toLowerCase(Locale.ROOT);
        }
        return keywords.contains(word);
    }
}
