package jo.pigma.editor.popups;

/**
 * LES RANGÉES VIRTUELLES (D-P9 du rapport des popups — « exposer les
 * rangées comme nœuds virtuels ») : la carte à rangées y dit ce que
 * le service qui parle y lit — le nombre de ses rangées VISIBLES, le
 * libellé de chacune (le service y lit le rang), l'action de
 * l'activation (le service y joue la rangée au doigt du lecteur
 * d'écran — la MÊME action que le doigt). La vue y trouve la carte
 * active et porte ses rangées au fournisseur de nœuds.
 */
public interface VirtualRows {

    /**
     * Le nombre des rangées VISIBLES de la carte (le service ne lit
     * que ce qui se voit — la fenêtre de la carte).
     *
     * @return le compte des rangées visibles, 0 quand la carte ne
     *         vit pas ou n'en porte pas
     */
    int virtualRowCount();

    /**
     * Le libellé d'une rangée visible (le service qui parle la lit).
     *
     * @param index l'indice de la rangée VISIBLE (0 à
     *              {@link #virtualRowCount()} moins un)
     * @return le libellé, vide quand la rangée ne se lit pas
     */
    String virtualRowLabel(int index);

    /**
     * L'activation d'une rangée visible (le service qui parle y joue
     * la rangée — la MÊME action que le doigt : la navigation, la
     * pose, la bascule).
     *
     * @param index l'indice de la rangée VISIBLE
     * @return vrai quand la rangée s'est jouée
     */
    boolean virtualRowActivate(int index);

    /**
     * L'ordonnée du centre d'une rangée visible (le nœud y pose ses
     * bornes — le service y lit sa place à l'écran).
     *
     * @param index l'indice de la rangée VISIBLE
     * @return l'ordonnée du centre (px, coordonnées de la vue), -1
     *         hors fenêtre
     */
    float virtualRowCenterY(int index);
}
