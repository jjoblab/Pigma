package jo.pigma.lsp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Une place pointée par le serveur : l'URI du document et l'intervalle
 * qui y vit. L'aller-à la définition en rend (la déclaration), les
 * références en rendent autant (les usages du symbole) — la réponse
 * du protocole porte les deux formes (la position nue ou le lien qui
 * compte la cible).
 *
 * @param uri   le document de la place
 * @param range l'intervalle de la place dans ce document
 */
public record LspLocation(String uri, LspRange range) {

    /**
     * L'adaptation d'une position du protocole.
     *
     * @param location la position nue du protocole
     * @return la place du domaine
     */
    public static LspLocation fromLsp(Location location) {
        return new LspLocation(location.getUri(),
                LspRange.fromLsp(location.getRange()));
    }

    /**
     * L'adaptation d'un lien du protocole (la cible compte, l'origine
     * est celle du doigt — le lien s'y ouvre).
     *
     * @param link le lien de position du protocole
     * @return la place pointée par le lien
     */
    public static LspLocation fromLink(LocationLink link) {
        return new LspLocation(link.getTargetUri(),
                LspRange.fromLsp(link.getTargetRange()));
    }

    /**
     * L'adaptation de la réponse de l'aller-à : les positions nues ou
     * les liens, la liste du domaine en sort (vide si rien).
     *
     * @param answer la réponse du protocole
     * @return les places, jamais null
     */
    public static List<LspLocation> fromAnswer(
            Either<List<? extends Location>, List<? extends LocationLink>> answer) {
        List<LspLocation> places = new ArrayList<>();
        if (answer == null) {
            return places;
        }
        if (answer.isLeft() && answer.getLeft() != null) {
            for (Location location : answer.getLeft()) {
                if (location != null && location.getUri() != null) {
                    places.add(fromLsp(location));
                }
            }
        } else if (answer.isRight() && answer.getRight() != null) {
            for (LocationLink link : answer.getRight()) {
                if (link != null && link.getTargetUri() != null) {
                    places.add(fromLink(link));
                }
            }
        }
        return places;
    }

    /**
     * L'adaptation d'une liste de positions du protocole (la réponse
     * des références).
     *
     * @param locations les positions du protocole
     * @return les places du domaine, jamais null
     */
    public static List<LspLocation> fromLsp(List<? extends Location> locations) {
        List<LspLocation> places = new ArrayList<>();
        if (locations != null) {
            for (Location location : locations) {
                if (location != null && location.getUri() != null) {
                    places.add(fromLsp(location));
                }
            }
        }
        return places;
    }
}
