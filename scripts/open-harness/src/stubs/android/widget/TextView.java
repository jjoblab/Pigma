package android.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

/**
 * Texte de démonstration : le texte se pose et se lit, la police et
 * le corps se retiennent — et les écouteurs de changement s'y
 * notifient (le champ s'y pose d'un bloc, comme une frappe).
 */
public class TextView extends View {

    private CharSequence text = "";
    private CharSequence hint = "";
    private Typeface typeface;
    private float textSize = 14.0f;
    private final List<TextWatcher> watchers = new ArrayList<>();

    /**
     * Une vue de texte neuve, vide.
     *
     * @param context le contexte porteur
     */
    public TextView(Context context) {
        super(context);
    }

    /**
     * Pose le texte : les écouteurs s'y notifient (avant, pendant,
     * après — l'après reçoit le texte livré comme modifiable).
     *
     * @param text le texte
     */
    public void setText(CharSequence text) {
        CharSequence before = this.text;
        for (TextWatcher watcher : watchers) {
            watcher.beforeTextChanged(before, 0, before.length(), 0);
        }
        this.text = text == null ? "" : text;
        for (TextWatcher watcher : watchers) {
            watcher.onTextChanged(this.text, 0, before.length(), this.text.length());
        }
        Editable delivered = new SimpleEditable(this.text);
        for (TextWatcher watcher : watchers) {
            watcher.afterTextChanged(delivered);
        }
    }

    /** Le texte courant. */
    public CharSequence getText() {
        return text;
    }

    /**
     * Écoute les changements du texte (la recherche incrémentale s'y
     * branche).
     *
     * @param watcher l'écouteur à ajouter
     */
    public void addTextChangedListener(TextWatcher watcher) {
        watchers.add(watcher);
    }

    /**
     * Retire un écouteur.
     *
     * @param watcher l'écouteur à retirer
     */
    public void removeTextChangedListener(TextWatcher watcher) {
        watchers.remove(watcher);
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

    /** Le type de saisie demandé au clavier (InputType.TYPE_CLASS_*). */
    private int inputType;

    /**
     * Pose le type de saisie (le clavier s'y règle — nombre pour
     * l'aller-à-la-ligne).
     *
     * @param inputType le type (InputType.TYPE_CLASS_*)
     */
    public void setInputType(int inputType) {
        this.inputType = inputType;
    }

    /** Le type de saisie posé (le harnais le lit). */
    public int getInputType() {
        return inputType;
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

    /**
     * Le texte livré comme modifiable : une enveloppe en lecture
     * seule (les éditions du champ ne passent pas par ici au
     * harnais).
     */
    private static final class SimpleEditable implements Editable {

        private final CharSequence content;

        private SimpleEditable(CharSequence content) {
            this.content = content;
        }

        @Override
        public int length() {
            return content.length();
        }

        @Override
        public char charAt(int index) {
            return content.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return content.subSequence(start, end);
        }

        @Override
        public String toString() {
            return content.toString();
        }
    }
}
