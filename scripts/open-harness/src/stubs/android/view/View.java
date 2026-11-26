package android.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Vue de démonstration : la géométrie se pose à la main (taille,
 * marges intérieures), le clic déclenche l'auditeur, le dessin passe
 * par {@link #draw(Canvas)} qui appelle {@link #onDraw(Canvas)}. Les
 * différés ({@link #post} et {@link #postDelayed}) vivent dans une
 * file à horloge virtuelle que le harnais draine : rien ne tick
 * sans lui, tout est déterministe.
 */
public class View {

    /** Visibilité ordinaire. */
    public static final int VISIBLE = 0;

    /** Invisible, mais la place reste. */
    public static final int INVISIBLE = 4;

    /** Invisible, la place se rend. */
    public static final int GONE = 8;

    /**
     * L'auditeur de clic : le bouton du harnais le déclenche par
     * {@link #performClick()}.
     */
    public interface OnClickListener {

        /** Le clic arrive. */
        void onClick(View view);
    }

    /**
     * L'état de base du cadre : le porteur du parent y vit (le
     * contrat du vrai cadre — la vue y accroche son état propre).
     */
    public static class BaseSavedState implements android.os.Parcelable {

        /** L'état vide du cadre. */
        public static final BaseSavedState EMPTY_STATE =
                new BaseSavedState((android.os.Parcelable) null);

        /** L'état du parent (null à la racine). */
        private final android.os.Parcelable superState;

        /**
         * Un état neuf.
         *
         * @param superState l'état du parent
         */
        public BaseSavedState(android.os.Parcelable superState) {
            this.superState = superState;
        }

        /**
         * La relecture du pareclable (le constructeur du cadre — les
         * sous-classes du vrai cadre y passent leur source).
         *
         * @param source la source
         */
        public BaseSavedState(android.os.Parcel source) {
            this.superState = null;
        }

        /** L'état du parent (l'EMPTY_STATE si aucun). */
        public final android.os.Parcelable getSuperState() {
            return superState;
        }

        @Override
        public void writeToParcel(android.os.Parcel dest, int flags) {
        }
    }

    /** Le contexte porteur. */
    private final Context context;

    private int width;
    private int height;
    private int paddingLeft;
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;
    private int visibility = VISIBLE;
    private boolean enabled = true;
    private boolean invalidated;
    private OnClickListener clickListener;
    private View parent;

    /** La translation verticale posée (le rendu de la feuille y entre). */
    private float translationY;

    /** L'horloge virtuelle des différés (millisecondes). */
    private static long clockMillis;

    /** Le compteur d'ordre d'arrivée des différés. */
    private static long sequence;

    /** Les différés en attente, par échéance puis ordre d'arrivée. */
    private static final PriorityQueue<Deferred> deferredQueue =
            new PriorityQueue<>(Comparator.comparingLong((Deferred d) -> d.time)
                    .thenComparingLong(d -> d.order));

    /** La vue qui tient le focus. */
    private static View focusedView;

    /**
     * Un différé : son échéance, son rang d'arrivée, sa suite.
     */
    private static final class Deferred {
        private final long time;
        private final long order;
        private final Runnable runnable;

        private Deferred(long time, long order, Runnable runnable) {
            this.time = time;
            this.order = order;
            this.runnable = runnable;
        }
    }

    /**
     * Une vue neuve.
     *
     * @param context le contexte porteur
     */
    public View(Context context) {
        this.context = context;
    }

    /** Le contexte porteur. */
    public final Context getContext() {
        return context;
    }

    /** Les ressources du contexte. */
    public Resources getResources() {
        return context.getResources();
    }

    /**
     * Programme une suite dès que possible (à l'horloge virtuelle
     * courante).
     *
     * @param action la suite
     * @return vrai si acceptée
     */
    public boolean post(Runnable action) {
        return postDelayed(action, 0L);
    }

    /**
     * Programme une suite à échéance (à l'horloge virtuelle).
     *
     * @param action la suite
     * @param delayMillis l'échéance en millisecondes
     * @return vrai si acceptée
     */
    public boolean postDelayed(Runnable action, long delayMillis) {
        deferredQueue.add(new Deferred(clockMillis + Math.max(0L, delayMillis),
                sequence++, action));
        return true;
    }

    /**
     * Retire les programmations d'une suite.
     *
     * @param action la suite à retirer
     * @return vrai si quelque chose fut retiré
     */
    public boolean removeCallbacks(Runnable action) {
        boolean removed = false;
        Iterator<Deferred> each = deferredQueue.iterator();
        while (each.hasNext()) {
            if (each.next().runnable == action) {
                each.remove();
                removed = true;
            }
        }
        return removed;
    }

    /** Demande un redessin : se compte, le harnais dessine lui-même. */
    public void invalidate() {
        invalidated = true;
    }

    /** Le redessin fut-il demandé depuis la remise à zéro ? */
    public boolean harnessInvalidated() {
        return invalidated;
    }

    /** Remet le compteur de redessin demandé. */
    public void harnessClearInvalidated() {
        invalidated = false;
    }

    /**
     * Demande le focus (une seule vue le tient à la fois).
     *
     * @return vrai
     */
    public boolean requestFocus() {
        focusedView = this;
        return true;
    }

    /** La vue tient-elle le focus ? */
    public boolean hasFocus() {
        return focusedView == this;
    }

    /**
     * Annonce un changement à l'accessibilité (la source unique des
     * ouvertures de popups s'y branche — le vrai service s'en charge
     * à l'écran, le harnais n'en fait rien).
     *
     * @param text l'annonce
     */
    public void announceForAccessibility(CharSequence text) {
        // le harnais n'a pas de service d'accessibilité : l'annonce se
        // tait (le décompte vivrait à la demande)
    }

    /**
     * Déclenche l'auditeur de clic.
     *
     * @return vrai si un auditeur a couru
     */
    public boolean performClick() {
        if (clickListener != null) {
            clickListener.onClick(this);
            return true;
        }
        return false;
    }

    /**
     * Pose l'auditeur de clic.
     *
     * @param listener l'auditeur (null : aucun)
     */
    public void setOnClickListener(OnClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Pose la visibilité.
     *
     * @param visibility VISIBLE, INVISIBLE ou GONE
     */
    public void setVisibility(int visibility) {
        this.visibility = visibility;
    }

    /** La visibilité courante. */
    public int getVisibility() {
        return visibility;
    }

    /**
     * Pose l'état d'activation.
     *
     * @param enabled vrai pour activer
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** L'état d'activation. */
    public boolean isEnabled() {
        return enabled;
    }

    /** La largeur posée. */
    public int getWidth() {
        return width;
    }

    /** La hauteur posée. */
    public int getHeight() {
        return height;
    }

    /** La marge intérieure gauche. */
    public int getPaddingLeft() {
        return paddingLeft;
    }

    /** La marge intérieure haute. */
    public int getPaddingTop() {
        return paddingTop;
    }

    /** La marge intérieure droite. */
    public int getPaddingRight() {
        return paddingRight;
    }

    /** La marge intérieure basse. */
    public int getPaddingBottom() {
        return paddingBottom;
    }

    /**
     * Pose les marges intérieures.
     *
     * @param left   gauche
     * @param top    haut
     * @param right  droite
     * @param bottom bas
     */
    public void setPadding(int left, int top, int right, int bottom) {
        paddingLeft = left;
        paddingTop = top;
        paddingRight = right;
        paddingBottom = bottom;
    }

    /** Focusable : accepté, sans effet. */
    public void setFocusable(boolean focusable) {
    }

    /** Focusable au toucher : accepté, sans effet. */
    public void setFocusableInTouchMode(boolean focusableInTouchMode) {
    }

    /**
     * L'événement tactile (les vues filles traduisent).
     *
     * @param event l'événement
     * @return vrai si consommé
     */
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    /**
     * Le dessin public : appelle {@link #onDraw(Canvas)} — le harnais
     * déclenche la trame.
     *
     * @param canvas le canevas d'enregistrement
     */
    public void draw(Canvas canvas) {
        onDraw(canvas);
    }

    /**
     * Le dessin de la vue (les vues filles surchargent).
     *
     * @param canvas le canevas
     */
    protected void onDraw(Canvas canvas) {
    }

    /** L'attachement à la fenêtre (les vues filles surchargent). */
    protected void onAttachedToWindow() {
    }

    /** Le détachement de la fenêtre (les vues filles surchargent). */
    protected void onDetachedFromWindow() {
    }

    /** La description d'accessibilité posée (null : aucune). */
    private CharSequence contentDescription;

    /**
     * Pose la description d'accessibilité (le lecteur d'écran y
     * lit la vue).
     *
     * @param description la description, null pour l'ôter
     */
    public void setContentDescription(CharSequence description) {
        contentDescription = description;
    }

    /** La description d'accessibilité posée (null : aucune). */
    public CharSequence getContentDescription() {
        return contentDescription;
    }

    /**
     * La sauvegarde d'état (les vues filles surchargent, le
     * harnais y exerce la sauvegarde-restauration).
     *
     * @return l'état sauvé, jamais null
     */
    protected android.os.Parcelable onSaveInstanceState() {
        return null;
    }

    /**
     * La restauration d'état (les vues filles surchargent).
     *
     * @param state l'état sauvé, null à la création pure
     */
    protected void onRestoreInstanceState(android.os.Parcelable state) {
    }

    /**
     * La connexion de saisie (les vues éditables surchargent).
     *
     * @param outAttrs les attributs à remplir
     * @return la connexion, ou null
     */
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return null;
    }

    /** La vue est-elle un éditeur de texte ? */
    /**
     * Le nœud d'accessibilité (le service qui parle y lit la vue —
     * la forme du vrai cadre, le remplissage vit à la vue).
     */
    public android.view.accessibility.AccessibilityNodeInfo createAccessibilityNodeInfo() {
        android.view.accessibility.AccessibilityNodeInfo node =
                new android.view.accessibility.AccessibilityNodeInfo();
        onInitializeAccessibilityNodeInfo(node);
        return node;
    }

    /** Le remplissage du nœud (la vue y écrit sa description). */
    /** Le fournisseur de nœuds virtuels (les rangées — le vrai cadre y vit, la nullité suffit). */
    public android.view.accessibility.AccessibilityNodeProvider getAccessibilityNodeProvider() {
        return null;
    }

    public void onInitializeAccessibilityNodeInfo(
            android.view.accessibility.AccessibilityNodeInfo node) {
    }

    /**
     * La dispatch de remplissage d'événement (la vue y écrit ses
     * textes — le vrai cadre l'appelle avant l'envoi).
     */
    public boolean dispatchPopulateAccessibilityEvent(
            android.view.accessibility.AccessibilityEvent event) {
        return false;
    }

    public boolean onCheckIsTextEditor() {
        return false;
    }

    /**
     * Attache la vue à la fenêtre (le harnais le fait après la pose
     * du contenu).
     */
    public void harnessAttach() {
        onAttachedToWindow();
    }

    /**
     * Pose la géométrie (aucune mesure réelle : le harnais dimensionne
     * la vue qu'il éprouve).
     *
     * @param newWidth  largeur en pixels
     * @param newHeight hauteur en pixels
     */
    public void harnessLayout(int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;
    }

    /** Le parent direct (null à la racine) — le harnais y remonte. */
    public View harnessParent() {
        return parent;
    }

    /** Pose le parent direct (à l'ajout ou au retrait d'un groupe). */
    public void harnessSetParent(View parent) {
        this.parent = parent;
    }

    /**
     * Demande une nouvelle mesure et disposition de l'arbre (accepté,
     * sans effet : le harnais dimensionne la vue qu'il éprouve).
     */
    public void requestLayout() {
    }

    /**
     * Pose le drapeau « ne dessine pas » des groupes sans arrière-plan
     * (accepté, sans effet : le harnais déclenche le dessin lui-même).
     *
     * @param willNotDraw vrai pour marquer la vue sans dessin propre
     */
    public void setWillNotDraw(boolean willNotDraw) {
    }

    /**
     * Pose la translation verticale de la vue (accepté, rangé sans
     * effet : le harnais ne rend pas la géométrie).
     *
     * @param translationY la translation en pixels
     */
    public void setTranslationY(float translationY) {
        this.translationY = translationY;
    }

    /** La translation verticale posée. */
    public float getTranslationY() {
        return translationY;
    }

    /**
     * La mesure de la vue (accepté, sans effet : les dimensions mesurées
     * restent nulles — le harnais dimensionne la vue qu'il éprouve).
     *
     * @param widthMeasureSpec  la contrainte de largeur
     * @param heightMeasureSpec la contrainte de hauteur
     */
    public void measure(int widthMeasureSpec, int heightMeasureSpec) {
    }

    /**
     * La mesure propre de la vue (accepté, sans effet : les
     * conteneurs de l'écran la surchargent pour le cadre réel — le
     * harnais n'y mesure jamais).
     *
     * @param widthMeasureSpec  la contrainte de largeur
     * @param heightMeasureSpec la contrainte de hauteur
     */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    }

    /**
     * Pose les dimensions mesurées (accepté, sans effet).
     *
     * @param measuredWidth  la largeur mesurée
     * @param measuredHeight la hauteur mesurée
     */
    protected final void setMeasuredDimension(int measuredWidth, int measuredHeight) {
    }

    /** La largeur mesurée (nulle au harnais : la mesure n'y court pas). */
    public final int getMeasuredWidth() {
        return 0;
    }

    /** La hauteur mesurée (nulle au harnais : la mesure n'y court pas). */
    public final int getMeasuredHeight() {
        return 0;
    }

    /**
     * La disposition de la vue : la géométrie s'y pose (taille comprise),
     * sans parent ni enfants réels.
     *
     * @param left   bord gauche
     * @param top    bord haut
     * @param right  bord droit
     * @param bottom bord bas
     */
    public void layout(int left, int top, int right, int bottom) {
        width = right - left;
        height = bottom - top;
    }

    /**
     * Les contraintes de mesure du cadre : un mode et une taille codés
     * dans un entier (les modes du cadre y vivent pour la compilation
     * des conteneurs ; le harnais n'en fabrique jamais).
     */
    public static final class MeasureSpec {

        /** Le parent impose la taille exacte. */
        public static final int EXACTLY = 1 << 30;

        /** La taille est un plafond. */
        public static final int AT_MOST = 2 << 30;

        /** Aucune contrainte. */
        public static final int UNSPECIFIED = 0;

        private MeasureSpec() {
        }

        /**
         * Fabrique une contrainte.
         *
         * @param size la taille
         * @param mode le mode
         * @return la contrainte codée
         */
        public static int makeMeasureSpec(int size, int mode) {
            return mode | (size & ~(-1 << 30));
        }

        /**
         * La taille de la contrainte.
         *
         * @param measureSpec la contrainte codée
         * @return la taille
         */
        public static int getSize(int measureSpec) {
            return measureSpec & (~(-1 << 30));
        }

        /**
         * Le mode de la contrainte.
         *
         * @param measureSpec la contrainte codée
         * @return le mode
         */
        public static int getMode(int measureSpec) {
            return measureSpec & (-1 << 30);
        }
    }

    /** L'horloge virtuelle courante (millisecondes). */
    public static long harnessClockMillis() {
        return clockMillis;
    }

    /**
     * Draine les différés dus dans l'horizon : l'horloge virtuelle
     * avance à chaque échéance, les suites repostées s'exécutent dans
     * le même drainage si leur échéance tombe dans l'horizon.
     *
     * @param horizonMillis la largeur de l'horizon en millisecondes
     */
    public static void harnessDrain(long horizonMillis) {
        long horizon = clockMillis + Math.max(0L, horizonMillis);
        while (!deferredQueue.isEmpty() && deferredQueue.peek().time <= horizon) {
            Deferred next = deferredQueue.remove();
            clockMillis = Math.max(clockMillis, next.time);
            next.runnable.run();
        }
    }
}
