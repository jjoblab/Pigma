package android.text;

/**
 * Texte modifiable de démonstration : le type suffit (le contenu se
 * lit par {@link CharSequence}, les éditions du champ ne passent pas
 * par ici au harnais — le champ se pose d'un bloc par
 * {@code setText}).
 */
public interface Editable extends CharSequence {
}
