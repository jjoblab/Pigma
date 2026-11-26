package android.os;

import java.util.HashMap;
import java.util.Map;

/**
 * Paquet d'état de démonstration : les types primitifs s'y posent
 * et s'y lisent (la sauvegarde et la restauration de la vue s'y
 * exercent au harnais comme sur l'appareil — le vrai Bundle y
 * implémente Parcelable, le stub suit).
 */
public final class Bundle implements Parcelable {

    /** L'écriture du stub (le paquet voyage en mémoire). */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }


    /** Les valeurs posées (les clefs y vivent). */
    private final Map<String, Object> values = new HashMap<>();

    /**
     * Pose un entier.
     *
     * @param key   la clef
     * @param value la valeur
     */
    public void putInt(String key, int value) {
        values.put(key, value);
    }

    /**
     * Lit un entier.
     *
     * @param key      la clef
     * @param fallback la valeur si la clef manque
     * @return la valeur posée, ou la valeur de repli
     */
    public int getInt(String key, int fallback) {
        Object found = values.get(key);
        return found instanceof Integer ? (Integer) found : fallback;
    }

    /**
     * Pose un flottant.
     *
     * @param key   la clef
     * @param value la valeur
     */
    public void putFloat(String key, float value) {
        values.put(key, value);
    }

    /**
     * Pose une chaîne.
     *
     * @param key   la clef
     * @param value la valeur (null l'efface)
     */
    public void putString(String key, String value) {
        values.put(key, value);
    }

    /**
     * Lit une chaîne.
     *
     * @param key la clef
     * @return la valeur posée, null si absente
     */
    public String getString(String key) {
        Object found = values.get(key);
        return found instanceof String ? (String) found : null;
    }

    /**
     * Lit un flottant.
     *
     * @param key      la clef
     * @param fallback la valeur si la clef manque
     * @return la valeur posée, ou la valeur de repli
     */
    public float getFloat(String key, float fallback) {
        Object found = values.get(key);
        return found instanceof Float ? (Float) found : fallback;
    }
}
