package android.content;

/**
 * Clip de démonstration : une étiquette et des articles, le presse-
 * papiers les porte. L'API reproduit celle du système — l'article
 * porte le texte, le clip n'expose que ses articles (les raccourcis
 * du système y passent aussi : getItemCount, getItemAt).
 */
public final class ClipData {

    /** Un article du clip : le texte qu'il porte. */
    public static final class Item {

        private final CharSequence text;

        public Item(CharSequence text) {
            this.text = text;
        }

        /** Le texte de l'article (null : pas du texte). */
        public CharSequence getText() {
            return text;
        }
    }

    private final CharSequence label;

    private final Item[] items;

    private ClipData(CharSequence label, Item[] items) {
        this.label = label;
        this.items = items;
    }

    /**
     * Un clip de texte brut.
     *
     * @param label étiquette du clip
     * @param text  contenu
     * @return le clip neuf
     */
    public static ClipData newPlainText(CharSequence label, CharSequence text) {
        return new ClipData(label, new Item[] {new Item(text)});
    }

    /** L'étiquette du clip. */
    public CharSequence getLabel() {
        return label;
    }

    /** Le compte d'articles du clip. */
    public int getItemCount() {
        return items.length;
    }

    /**
     * L'article à la position.
     *
     * @param index la position demandée
     * @return l'article
     * @throws IndexOutOfBoundsException hors des bornes
     */
    public Item getItemAt(int index) {
        return items[index];
    }
}
