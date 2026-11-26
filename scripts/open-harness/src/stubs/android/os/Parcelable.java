package android.os;

/**
 * Le pareil du stub : l'état sauvé d'une vue s'y porte (le vrai
 * cadre y tient ses protocholes de parcelle, le harnais n'en porte
 * que le type).
 */
public interface Parcelable {

    /** Le créateur d'état du cadre (le stub y porte le type). */
    interface Creator<T> {

        /** La relecture d'une parcelle. */
        T createFromParcel(Parcel source);

        /** Le tableau neuf. */
        T[] newArray(int size);
    }

    /** L'écriture vers une parcelle. */
    void writeToParcel(Parcel dest, int flags);
}
