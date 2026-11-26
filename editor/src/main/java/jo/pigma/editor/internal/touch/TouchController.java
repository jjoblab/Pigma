package jo.pigma.editor.internal.touch;

import java.util.function.LongSupplier;

/**
 * L'ÉTAT DU TOUCHER (la suite du contrôleur des gestes du découpage —
 * R1, la suite du pincement) : QUI possède le contact. Un appui naît,
 * puis GRANDIT — le tap reste tap, ou franchit le seuil de glisse et
 * devient balayage, ou tient assez longtemps et devient appui long,
 * ou saisit une poignée de sélection, ou arme le traîné-déposé d'une
 * sélection vivante, ou tient la carte du document (la minimap), ou
 * se double d'un second doigt (le pincement, son propre contrôleur).
 *
 * <p>Le contrôleur tient la MÉMOIRE du geste — l'horodatage de
 * l'appui (l'appui long), celui du dernier tap (le double-tap), le
 * point de départ (le seuil de glisse), la borne traînée et sa
 * fixe, le point de dépôt du traîné, le doigt vivant (la loupe et
 * l'aperçu flottant le suivent) — et rend SES VERDICTS : l'appui a-t-il
 * durée, le tap en suit-il un autre, le doigt a-t-il franchi le
 * seuil, le lâcher vaut-il un tap (aucun geste n'a grandi).</p>
 *
 * <p>Pure et sans fil d'horloge : l'horloge s'injecte (le harnais JVM
 * y pose la sienne), les positions se passent en paramètres — le
 * contrôleur ne connaît ni canevas ni MotionEvent, la GÉOMÉTRIE reste
 * à la vue (où tombe un doigt : quel offset, quelle ligne, quel
 * marqueur), la DÉCISION vit ici.</p>
 */
public final class TouchController {

    /** Le seuil de l'appui long (ns) — le geste qui sélectionne le mot. */
    private static final long LONG_PRESS_NANOS = 500_000_000L;

    /** La fenêtre du double-tap (ns) — deux taps au même endroit. */
    private static final long DOUBLE_TAP_NANOS = 350_000_000L;

    /** L'horloge (le harnais y pose la sienne). */
    private final LongSupplier clock;

    /** L'horodatage de l'appui courant (0 : aucun appui). */
    private long downNanos;

    /** L'horodatage du dernier tap compté (0 : aucun). */
    private long lastTapNanos;

    /** L'abscisse du dernier tap compté. */
    private float lastTapX;

    /** L'ordonnée du dernier tap compté. */
    private float lastTapY;

    /** Le point de départ de l'appui (le seuil de glisse s'y lit). */
    private float startX;

    /** L'ordonnée de départ de l'appui. */
    private float startY;

    /** Le balayage vit (le doigt a franchi le seuil, la vue suit). */
    private boolean dragging;

    /** La carte du document est tenue (la minimap navigue au doigt). */
    private boolean mapDragging;

    /**
     * La poignée traînée (0 : aucune, 1 : la borne de début, 2 : la
     * borne de fin — l'appui long qui glisse s'y raccorde : la borne
     * la plus proche du doigt).
     */
    private int draggedBound;

    /** La borne fixe du traînement (l'autre bout de la sélection). */
    private int fixedBound = -1;

    /**
     * Le traîné-déposé : l'appui dans le texte d'une sélection vivante
     * l'arme (le glissé qui suit déplacera le texte au doigt).
     */
    private boolean textArmed;

    /** Le traîné-déposé engagé : le curseur de dépôt suit le doigt. */
    private boolean textDragging;

    /** Le décalage de dépôt courant du traîné (-1 : hors geste). */
    private int dropOffset = -1;

    /** L'abscisse du doigt pendant le traînement (l'aperçu flottant la suit). */
    private float fingerX;

    /** L'ordonnée du doigt pendant le traînement (la loupe la fuit). */
    private float fingerY;

    /** Un contrôleur neuf, à l'horloge du système. */
    public TouchController() {
        this(System::nanoTime);
    }

    /** Un contrôleur neuf, à l'horloge posée (le harnais JVM y avance le temps). */
    TouchController(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * Le contact NAÎT (le DOWN) : tout geste antérieur se range (la
     * vue y a déjà éprouvé ses cartes et sa minimap — le contact qui
     * demeure est le sien), l'appui s'horodate, son point de départ
     * se pose (le seuil de glisse s'y lira).
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     */
    public void begin(float x, float y) {
        settle();
        downNanos = clock.getAsLong();
        startX = x;
        startY = y;
    }

    /**
     * Le point de départ du doigt se REPOSE (le balayage y suit son
     * delta : le doigt demeure, l'appui le suit cran après cran).
     *
     * @param x l'abscisse du doigt (px)
     * @param y l'ordonnée du doigt (px)
     */
    public void slideTo(float x, float y) {
        startX = x;
        startY = y;
    }

    /**
     * L'appui se REPREND après un pincement dénoué : le doigt qui
     * demeure repart de SA position vivante (le défilement n'y saute
     * pas), le balayage s'y rend ; un doigt ABSENT (ses coordonnées
     * ne se lisent pas) ne déplace pas le point de départ — le
     * balayage s'y rend quand même.
     *
     * @param x l'abscisse du doigt restant (px)
     * @param y l'ordonnée du doigt restant (px)
     */
    public void restartAt(float x, float y) {
        if (!Float.isNaN(x) && !Float.isNaN(y)) {
            slideTo(x, y);
        }
        dragging = false;
    }

    /**
     * L'appui a-t-il tenu au-delà du seuil de l'appui long ? (le mot
     * sous le doigt s'y sélectionne, le point d'arrêt s'y pose).
     *
     * @return vrai au-delà du seuil
     */
    public boolean heldLongEnough() {
        return downNanos > 0L && clock.getAsLong() - downNanos >= LONG_PRESS_NANOS;
    }

    /**
     * Le doigt a-t-il franchi le SEUIL de glisse depuis son départ ?
     * (le geste GRANDIT : balayage, ou l'engagement du traîné).
     *
     * @param x    l'abscisse courante du doigt (px)
     * @param y    l'ordonnée courante du doigt (px)
     * @param slop le seuil de glisse (px)
     * @return vrai au-delà du seuil
     */
    public boolean beyondSlop(float x, float y, float slop) {
        return Math.abs(x - startX) > slop || Math.abs(y - startY) > slop;
    }

    /**
     * Le tap en suit-il un AUTRE de près, au même endroit ? (le
     * double-tap sélectionne le mot, pose le point d'arrêt du numéro
     * de ligne).
     *
     * @param x    l'abscisse du tap (px)
     * @param y    l'ordonnée du tap (px)
     * @param slop le seuil de glisse (px — le même endroit s'y lit)
     * @return vrai quand le tap double son prédécesseur
     */
    public boolean tappedTwice(float x, float y, float slop) {
        return lastTapNanos > 0L
                && clock.getAsLong() - lastTapNanos < DOUBLE_TAP_NANOS
                && Math.abs(x - lastTapX) < slop
                && Math.abs(y - lastTapY) < slop;
    }

    /**
     * Le lâcher compte pour un TAP : la mémoire s'y pose (le tap
     * suivant s'y doublera).
     *
     * @param x l'abscisse du tap (px)
     * @param y l'ordonnée du tap (px)
     */
    public void rememberTap(float x, float y) {
        lastTapNanos = clock.getAsLong();
        lastTapX = x;
        lastTapY = y;
    }

    /** La mémoire du dernier tap s'efface (le geste a grandi). */
    public void forgetTap() {
        lastTapNanos = 0L;
    }

    /** L'abscisse de départ de l'appui (px — la colonne des marqueurs s'y éprouve, le mot de l'appui long s'y lit). */
    public float startX() {
        return startX;
    }

    /** L'ordonnée de départ de l'appui (px — le mot de l'appui long s'y lit). */
    public float startY() {
        return startY;
    }

    /** Le balayage vit-il ? (le lâcher ne vaut pas un tap). */
    public boolean isDragging() {
        return dragging;
    }

    /**
     * Le balayage s'engage (le seuil franchi) : la vue suivra le
     * doigt à deux axes.
     */
    public void engageScroll() {
        dragging = true;
    }

    /**
     * La carte du document est tenue : la navigation suit le doigt à
     * CHAQUE cran (le contact lui appartient — son lâcher y reste).
     */
    public void holdsMap() {
        mapDragging = true;
    }

    /** La carte du document est-elle tenue ? */
    public boolean isMapDragging() {
        return mapDragging;
    }

    /** La carte du document se lâche (le doigt s'en va — le curseur y demeure). */
    public void releaseMap() {
        mapDragging = false;
    }

    /**
     * Une POIGNÉE se saisit (ou l'appui long qui glisse s'y raccorde) :
     * la borne donnée devient la borne traînée, l'autre se fixe, le
     * doigt vivant se pose (la loupe le suivra).
     *
     * @param bound       la borne traînée (1 : début, 2 : fin)
     * @param fixed       la borne fixe (l'autre bout de la sélection)
     * @param fingerY     l'ordonnée du doigt (px)
     */
    public void holdsHandle(int bound, int fixed, float fingerY) {
        draggedBound = bound;
        fixedBound = fixed;
        this.fingerY = fingerY;
    }

    /** La poignée traînée (0 : aucune, 1 : début, 2 : fin). */
    public int draggedBound() {
        return draggedBound;
    }

    /** La borne fixe du traînement. */
    public int fixedBound() {
        return fixedBound;
    }

    /** Une poignée vit-elle traînée ? */
    public boolean isHandleDragging() {
        return draggedBound != 0;
    }

    /** La poignée se lâche (la sélection reste posée, la borne fixe se rend). */
    public void releaseHandle() {
        draggedBound = 0;
        fixedBound = -1;
    }

    /**
     * Le traîné-déposé s'ARME : l'appui est tombé dans le texte d'une
     * sélection vivante (la vue y éprouve l'éligibilité — ses murs,
     * sa sélection, l'offset sous le doigt) ; le glissé qui suit,
     * avant le seuil de l'appui long, ENGAGERA.
     *
     * @param armed l'éligibilité calculée par la vue
     */
    public void armTextDrag(boolean armed) {
        textArmed = armed;
    }

    /** Le traîné-déposé est-il armé ? */
    public boolean isTextArmed() {
        return textArmed;
    }

    /**
     * Le traîné-déposé s'engage (le glissé a franchi le seuil AVANT
     * l'appui long) : le curseur de dépôt suivra le doigt jusqu'au
     * dépôt.
     */
    public void engageTextDrag() {
        textDragging = true;
    }

    /** Le traîné-déposé vit-il engagé ? (l'aperçu flotte, le dépôt suit). */
    public boolean isTextDragging() {
        return textDragging;
    }

    /**
     * Le doigt vivant se pose (la loupe le fuit, l'aperçu flottant
     * le suit, le point de dépôt s'y relit).
     *
     * @param x l'abscisse du doigt (px)
     * @param y l'ordonnée du doigt (px)
     */
    public void finger(float x, float y) {
        fingerX = x;
        fingerY = y;
    }

    /** L'abscisse du doigt vivant (px — l'aperçu flottant la suit). */
    public float fingerX() {
        return fingerX;
    }

    /** L'ordonnée du doigt vivant (px — la loupe la fuit). */
    public float fingerY() {
        return fingerY;
    }

    /**
     * Le curseur de dépôt se pose (le caractère le plus proche du
     * doigt — la vue y calcule l'offset).
     *
     * @param offset le décalage de dépôt
     */
    public void dropAt(int offset) {
        dropOffset = offset;
    }

    /** Le décalage de dépôt courant (-1 : hors geste). */
    public int dropOffset() {
        return dropOffset;
    }

    /** Le traîné se dépose : le texte déplacé, le geste se rend. */
    public void releaseTextDrag() {
        textDragging = false;
        textArmed = false;
        dropOffset = -1;
    }

    /**
     * Les gestes se rangent (un NOUVEAU contact naît, le système
     * REPREND le geste, le pincement s'engage sur une vue nette) : le
     * balayage se rend, la poignée se lâche (la sélection reste
     * posée), le traîné meurt sans dépôt, la carte se relâche — la
     * mémoire du tap et l'horodatage de l'appui demeurent (le
     * pincement qui se dénoue garde son appui : le doigt restant y
     * reprend, le long qui suit y vit encore ; seule la vue dit le
     * geste qui a grandi).
     */
    public void settle() {
        dragging = false;
        mapDragging = false;
        draggedBound = 0;
        fixedBound = -1;
        textArmed = false;
        textDragging = false;
        dropOffset = -1;
    }
}
