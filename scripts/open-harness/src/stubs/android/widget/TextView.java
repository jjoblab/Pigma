package android.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;

/**
 * Texte de démonstration : le texte se pose et se lit, la police et
 * le corps se retiennent.
 */
public class TextView extends View {

    private CharSequence text = "";
    private CharSequence hint = "";
    private Typeface typeface;
    private float textSize = 14.0f;

    /**
     * Une vue de texte neuve, vide.
     *
     * @param context le contexte porteur
     */
    public TextView(Context context) {
        super(context);
    }

    /**
     * Pose le texte.
     *
     * @param text le texte
     */
    public void setText(CharSequence text) {
        this.text = text;
    }

    /** Le texte courant. */
    public CharSequence getText() {
        return text;
    }

    /**
     * Pose l'indication (le champ vide la montre).
     *
     * @param hint l'indication
     */
    public void setHint(CharSequence hint) {
        this.hint = hint;
    }

    /** L'indication courante. */
    public CharSequence getHint() {
        return hint;
    }

    /**
     * Pose la police.
     *
     * @param typeface la police
     */
    public void setTypeface(Typeface typeface) {
        this.typeface = typeface;
    }

    /** La police courante. */
    public Typeface getTypeface() {
        return typeface;
    }

    /**
     * Pose le corps du texte.
     *
     * @param size le corps
     */
    public void setTextSize(float size) {
        this.textSize = size;
    }

    /** Le corps courant. */
    public float getTextSize() {
        return textSize;
    }
}
