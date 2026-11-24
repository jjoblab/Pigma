package jo.pigma.syntax;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Portée nommée d'un jeton, hiérarchique par points : {@code
 * comment.doc} a pour parent {@code comment}. Les portées sont
 * internées : deux demandes du même nom rendent la même instance, si
 * bien que la comparaison par identité suffit partout. La couleur ne
 * vit pas ici : une portée ne dit que ce qu'est le jeton, jamais
 * comment le peindre.
 */
public final class Scope {

    private static final Map<String, Scope> INTERNED = new ConcurrentHashMap<>();

    private final String name;
    private final Scope parent;

    private Scope(String name, Scope parent) {
        this.name = name;
        this.parent = parent;
    }

    /**
     * La portée du nom donné, internée : le parent est la portée du
     * nom amputé de son dernier segment ; un nom sans point n'a pas de
     * parent.
     *
     * @param name nom hiérarchique, non vide, sans point initial ni
     *             final, segments non vides
     * @return la portée, jamais {@code null}
     */
    public static Scope of(String name) {
        return INTERNED.computeIfAbsent(name, Scope::create);
    }

    private static Scope create(String name) {
        if (name == null || name.isEmpty() || name.startsWith(".")
                || name.endsWith(".") || name.contains("..")) {
            throw new IllegalArgumentException("invalid scope name: " + name);
        }
        int dot = name.lastIndexOf('.');
        Scope parent = dot < 0 ? null : of(name.substring(0, dot));
        return new Scope(name, parent);
    }

    /**
     * Le nom complet, points compris.
     *
     * @return le nom, jamais {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * La portée parente, ou {@code null} pour une portée racine.
     *
     * @return le parent, éventuellement absent
     */
    public Scope parent() {
        return parent;
    }

    @Override
    public String toString() {
        return name;
    }
}
