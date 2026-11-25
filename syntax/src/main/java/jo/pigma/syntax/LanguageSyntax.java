package jo.pigma.syntax;

import java.util.List;
import java.util.Objects;

import java.util.function.IntPredicate;
import java.util.function.Supplier;

import jo.pigma.syntax.lexer.Lexer;

/**
 * Descripteur de langage L1 : les extensions servies, la fabrique de
 * lexeur, la table de types vers portées et les états du lexeur
 * déclarés redémarrables. Un langage ajouté ne touche ni le cœur, ni
 * le moteur : c'est une donnée.
 *
 * <p>Un type absent de la table rend une portée {@code null} : le
 * jeton est neutre, le thème y applique sa couleur par défaut. Les
 * types commencent à 1 ; l'index 0 de la table est ignoré.</p>
 */
public final class LanguageSyntax {

    private final String name;
    private final List<String> extensions;
    private final Supplier<Lexer> lexerFactory;
    private final Scope[] scopesByType;
    private final IntPredicate restartableStates;

    /**
     * Un descripteur complet.
     *
     * @param name             nom affichable, jamais {@code null}
     * @param extensions       extensions servies (sans point), jamais
     *                         {@code null}
     * @param lexerFactory     fabrique de lexeur, jamais {@code null} ;
     *                         le moteur lexant hors fil d'interface, la
     *                         fabrique rend une instance neuve à chaque
     *                         demande
     * @param scopesByType     table de types vers portées, indexée par
     *                         type, jamais {@code null} ; une case
     *                         {@code null} marque un jeton neutre
     * @param restartableStates états du lexeur depuis lesquels relexer
     *                         une fenêtre rend le flux exact du lexage
     *                         complet, jamais {@code null}
     */
    public LanguageSyntax(String name, List<String> extensions,
            Supplier<Lexer> lexerFactory, Scope[] scopesByType,
            IntPredicate restartableStates) {
        this.name = Objects.requireNonNull(name);
        this.extensions = List.copyOf(extensions);
        this.lexerFactory = Objects.requireNonNull(lexerFactory);
        this.scopesByType = Objects.requireNonNull(scopesByType).clone();
        this.restartableStates = Objects.requireNonNull(restartableStates);
    }

    /**
     * La table des portées par type, l'instance du descripteur : les
     * jetons compacts d'une ligne la portent par référence (une par
     * ligne, pas une par jeton — la table reste partagée).
     *
     * @return la table, indexée par type, jamais {@code null}
     */
    Scope[] scopeTable() {
        return scopesByType;
    }

    /**
     * Le nom affichable du langage.
     *
     * @return le nom, jamais {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Les extensions servies, sans point initial.
     *
     * @return les extensions, jamais {@code null}
     */
    public List<String> extensions() {
        return extensions;
    }

    /**
     * Un lexeur neuf, armé par le moteur.
     *
     * @return le lexeur, jamais {@code null}
     */
    public Lexer newLexer() {
        return lexerFactory.get();
    }

    /**
     * La portée du type de jeton donné.
     *
     * @param type type rendu par le lexeur, {@code 0} jamais demandé
     * @return la portée, ou {@code null} pour un jeton neutre
     */
    public Scope scopeOf(int type) {
        return type > 0 && type < scopesByType.length ? scopesByType[type] : null;
    }

    /**
     * L'état initial est-il redémarrable pour ce langage ?
     *
     * @param state état du lexeur
     * @return vrai si une fenêtre démarrant dans cet état rexée rend
     *         le flux exact du lexage complet
     */
    public boolean isRestartableState(int state) {
        return restartableStates.test(state);
    }

    /**
     * L'extension (sans point) est-elle servie par ce langage ?
     *
     * @param extension extension en minuscules, sans point
     * @return vrai si ce langage s'ouvre pour cette extension
     */
    public boolean servesExtension(String extension) {
        return extensions.contains(extension);
    }

    /**
     * Les états redémarrables usuels : l'état initial seul.
     *
     * @return le prédicat d'état initial
     */
    public static IntPredicate initialStateOnly() {
        return state -> state == 0;
    }
}
