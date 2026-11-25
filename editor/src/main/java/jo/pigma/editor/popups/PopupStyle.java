package jo.pigma.editor.popups;

/**
 * Le style des cartes de popup, PORTÉ PAR L'HÔTE : l'application
 * porteuse peut tout y régler — le fond TRANSLUCIDE des cartes (la
 * vue se dessine dessous), les encres (texte, indication, secondaire,
 * accent), les teintes de diagnostic (erreur, avertissement) et
 * d'ampoule, la taille du texte principal et du texte secondaire, la
 * hauteur des rangées, les bornes de largeur des cartes, la forme (le
 * rayon des coins). Les valeurs par défaut portent le grand format :
 * texte de 18 sp, rangées de 54 dp, cartes de 380 à 480 dp, fond à
 * 91 pour cent d'opacité — l'éditeur les applique à toutes ses
 * cartes ; l'hôte qui veut le sien le bâtit et le pose
 * ({@code setPopupStyle} de la vue).
 *
 * <p>La classe est IMMUAABLE : le bâtisseur pose tout, puis
 * {@link #build()} rend le style figé — une carte ne le relit jamais
 * à moitié.</p>
 */
public final class PopupStyle {

    /** L'opacité du fond translucide par défaut (91 pour cent). */
    private static final int DEFAULT_BACKGROUND = 0xE9F6F8FA;

    /** L'opacité du filet par défaut (78 pour cent). */
    private static final int DEFAULT_BORDER = 0xC8B5B5B5;

    /** Le corps du texte principal par défaut (sp). */
    private static final float DEFAULT_TEXT_SIZE = 18.0f;

    /** Le corps du texte secondaire par défaut (sp). */
    private static final float DEFAULT_SECONDARY_SIZE = 14.0f;

    /** La hauteur d'une rangée par défaut (dp). */
    private static final int DEFAULT_ROW_HEIGHT = 54;

    /** La hauteur d'un titre d'étage par défaut (dp). */
    private static final int DEFAULT_HEADER_HEIGHT = 32;

    /** La borne basse de largeur d'une carte par défaut (dp). */
    private static final int DEFAULT_MIN_CARD_WIDTH = 380;

    /** La borne haute de largeur d'une carte par défaut (dp). */
    private static final int DEFAULT_MAX_CARD_WIDTH = 480;

    /** Le rayon des coins d'une carte par défaut (dp). */
    private static final int DEFAULT_CORNER_RADIUS = 16;

    private final int backgroundArgb;
    private final int borderArgb;
    private final int accentArgb;
    private final int textArgb;
    private final int hintArgb;
    private final int secondaryArgb;
    private final int errorArgb;
    private final int warningArgb;
    private final int lightbulbArgb;
    private final float textSizeSp;
    private final float secondarySizeSp;
    private final int rowHeightDp;
    private final int headerHeightDp;
    private final int minCardWidthDp;
    private final int maxCardWidthDp;
    private final int cornerRadiusDp;

    private PopupStyle(Builder builder) {
        backgroundArgb = builder.backgroundArgb;
        borderArgb = builder.borderArgb;
        accentArgb = builder.accentArgb;
        textArgb = builder.textArgb;
        hintArgb = builder.hintArgb;
        secondaryArgb = builder.secondaryArgb;
        errorArgb = builder.errorArgb;
        warningArgb = builder.warningArgb;
        lightbulbArgb = builder.lightbulbArgb;
        textSizeSp = builder.textSizeSp;
        secondarySizeSp = builder.secondarySizeSp;
        rowHeightDp = builder.rowHeightDp;
        headerHeightDp = builder.headerHeightDp;
        minCardWidthDp = builder.minCardWidthDp;
        maxCardWidthDp = builder.maxCardWidthDp;
        cornerRadiusDp = builder.cornerRadiusDp;
    }

    /**
     * Le style par défaut : le grand format, le fond translucide —
     * celui que l'éditeur pose sans consigne de l'hôte.
     *
     * @return le style par défaut, jamais null
     */
    public static PopupStyle defaults() {
        return builder().build();
    }

    /**
     * Un bâtisseur neuf : tout y part des valeurs par défaut, l'hôte
     * n'y change que ce qu'il veut.
     *
     * @return le bâtisseur du style
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Le fond translucide des cartes (ARGB — l'alpha porte la translucence). */
    public int backgroundArgb() {
        return backgroundArgb;
    }

    /** Le filet des cartes (ARGB). */
    public int borderArgb() {
        return borderArgb;
    }

    /** L'encre d'accent : la sélection courante, les titres d'étages (ARGB). */
    public int accentArgb() {
        return accentArgb;
    }

    /** L'encre du texte principal (ARGB). */
    public int textArgb() {
        return textArgb;
    }

    /** L'encre des indications, les textes atténués (ARGB). */
    public int hintArgb() {
        return hintArgb;
    }

    /** L'encre secondaire : les compteurs, les pieds de carte (ARGB). */
    public int secondaryArgb() {
        return secondaryArgb;
    }

    /** La teinte d'une erreur (le rond rouge de la gouttière et de la carte). */
    public int errorArgb() {
        return errorArgb;
    }

    /** La teinte d'un avertissement (le triangle ambre). */
    public int warningArgb() {
        return warningArgb;
    }

    /** La teinte de l'ampoule jaune (les actions rapides de la ligne). */
    public int lightbulbArgb() {
        return lightbulbArgb;
    }

    /** Le corps du texte principal (sp). */
    public float textSizeSp() {
        return textSizeSp;
    }

    /** Le corps du texte secondaire (sp). */
    public float secondarySizeSp() {
        return secondarySizeSp;
    }

    /** La hauteur d'une rangée (dp). */
    public int rowHeightDp() {
        return rowHeightDp;
    }

    /** La hauteur d'un titre d'étage (dp). */
    public int headerHeightDp() {
        return headerHeightDp;
    }

    /** La borne basse de largeur d'une carte (dp). */
    public int minCardWidthDp() {
        return minCardWidthDp;
    }

    /** La borne haute de largeur d'une carte (dp). */
    public int maxCardWidthDp() {
        return maxCardWidthDp;
    }

    /** Le rayon des coins d'une carte (dp) — la forme de la carte. */
    public int cornerRadiusDp() {
        return cornerRadiusDp;
    }

    /**
     * Le bâtisseur du style : chaque poseur y remplace UNE valeur,
     * {@link #build()} fige le tout.
     */
    public static final class Builder {

        private int backgroundArgb = DEFAULT_BACKGROUND;
        private int borderArgb = DEFAULT_BORDER;
        private int accentArgb = 0xFF1A73E8;
        private int textArgb = 0xFF202124;
        private int hintArgb = 0xFF80868B;
        private int secondaryArgb = 0xFF5F6368;
        private int errorArgb = 0xFFD93025;
        private int warningArgb = 0xFFF9AB00;
        private int lightbulbArgb = 0xFFF5C518;
        private float textSizeSp = DEFAULT_TEXT_SIZE;
        private float secondarySizeSp = DEFAULT_SECONDARY_SIZE;
        private int rowHeightDp = DEFAULT_ROW_HEIGHT;
        private int headerHeightDp = DEFAULT_HEADER_HEIGHT;
        private int minCardWidthDp = DEFAULT_MIN_CARD_WIDTH;
        private int maxCardWidthDp = DEFAULT_MAX_CARD_WIDTH;
        private int cornerRadiusDp = DEFAULT_CORNER_RADIUS;

        /** Un bâtisseur aux valeurs par défaut. */
        public Builder() {
        }

        /**
         * Pose le fond des cartes (l'alpha porte la translucence).
         *
         * @param color le fond, en ARGB
         * @return le bâtisseur
         */
        public Builder backgroundArgb(int color) {
            backgroundArgb = color;
            return this;
        }

        /**
         * Pose le filet des cartes.
         *
         * @param color le filet, en ARGB
         * @return le bâtisseur
         */
        public Builder borderArgb(int color) {
            borderArgb = color;
            return this;
        }

        /**
         * Pose l'encre d'accent.
         *
         * @param color l'accent, en ARGB
         * @return le bâtisseur
         */
        public Builder accentArgb(int color) {
            accentArgb = color;
            return this;
        }

        /**
         * Pose l'encre du texte principal.
         *
         * @param color l'encre, en ARGB
         * @return le bâtisseur
         */
        public Builder textArgb(int color) {
            textArgb = color;
            return this;
        }

        /**
         * Pose l'encre des indications.
         *
         * @param color l'encre, en ARGB
         * @return le bâtisseur
         */
        public Builder hintArgb(int color) {
            hintArgb = color;
            return this;
        }

        /**
         * Pose l'encre secondaire.
         *
         * @param color l'encre, en ARGB
         * @return le bâtisseur
         */
        public Builder secondaryArgb(int color) {
            secondaryArgb = color;
            return this;
        }

        /**
         * Pose la teinte d'erreur.
         *
         * @param color la teinte, en ARGB
         * @return le bâtisseur
         */
        public Builder errorArgb(int color) {
            errorArgb = color;
            return this;
        }

        /**
         * Pose la teinte d'avertissement.
         *
         * @param color la teinte, en ARGB
         * @return le bâtisseur
         */
        public Builder warningArgb(int color) {
            warningArgb = color;
            return this;
        }

        /**
         * Pose la teinte de l'ampoule.
         *
         * @param color la teinte, en ARGB
         * @return le bâtisseur
         */
        public Builder lightbulbArgb(int color) {
            lightbulbArgb = color;
            return this;
        }

        /**
         * Pose le corps du texte principal.
         *
         * @param size le corps, en sp
         * @return le bâtisseur
         */
        public Builder textSizeSp(float size) {
            textSizeSp = size;
            return this;
        }

        /**
         * Pose le corps du texte secondaire.
         *
         * @param size le corps, en sp
         * @return le bâtisseur
         */
        public Builder secondarySizeSp(float size) {
            secondarySizeSp = size;
            return this;
        }

        /**
         * Pose la hauteur des rangées.
         *
         * @param height la hauteur, en dp
         * @return le bâtisseur
         */
        public Builder rowHeightDp(int height) {
            rowHeightDp = height;
            return this;
        }

        /**
         * Pose la hauteur des titres d'étages.
         *
         * @param height la hauteur, en dp
         * @return le bâtisseur
         */
        public Builder headerHeightDp(int height) {
            headerHeightDp = height;
            return this;
        }

        /**
         * Pose la borne basse de largeur des cartes.
         *
         * @param width la largeur, en dp
         * @return le bâtisseur
         */
        public Builder minCardWidthDp(int width) {
            minCardWidthDp = width;
            return this;
        }

        /**
         * Pose la borne haute de largeur des cartes.
         *
         * @param width la largeur, en dp
         * @return le bâtisseur
         */
        public Builder maxCardWidthDp(int width) {
            maxCardWidthDp = width;
            return this;
        }

        /**
         * Pose le rayon des coins — la forme des cartes.
         *
         * @param radius le rayon, en dp
         * @return le bâtisseur
         */
        public Builder cornerRadiusDp(int radius) {
            cornerRadiusDp = radius;
            return this;
        }

        /**
         * Fige le style : les cartes ne le relisent jamais à moitié.
         *
         * @return le style posé
         */
        public PopupStyle build() {
            return new PopupStyle(this);
        }
    }
}
