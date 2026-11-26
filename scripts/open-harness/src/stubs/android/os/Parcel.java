package android.os;

/**
 * Le stub de la parcelle : les lectures et écritures des types
 * porteurs (le vrai cadre y sérialise, le harnais n'a besoin que du
 * type — les valeurs voyagent en mémoire).
 */
public final class Parcel {

    /** La fabrique du harnais (une parcelle neuve). */
    public static Parcel obtain() {
        return new Parcel();
    }

    private final java.util.ArrayList<Object> values = new java.util.ArrayList<>();
    private int readAt;

    /** Écrit un entier. */
    public void writeInt(int value) {
        values.add(value);
    }

    /** Lit un entier. */
    public int readInt() {
        return (Integer) values.get(readAt++);
    }

    /** Écrit un flottant. */
    public void writeFloat(float value) {
        values.add(value);
    }

    /** Lit un flottant. */
    public float readFloat() {
        return (Float) values.get(readAt++);
    }

    /** Écrit un pareclable. */
    public void writeParcelable(Parcelable value, int flags) {
        values.add(value);
    }

    /** Lit un pareclable. */
    public Parcelable readParcelable(ClassLoader loader) {
        return (Parcelable) values.get(readAt++);
    }
}
