package android.graphics;

/** Le stub harnais : la classe Color d'Android (HSV et composantes). */
public final class Color {

    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;
    public static final int RED = 0xFFFF0000;
    public static final int YELLOW = 0xFFFFFF00;
    public static final int GREEN = 0xFF00FF00;
    public static final int CYAN = 0xFF00FFFF;
    public static final int BLUE = 0xFF0000FF;
    public static final int MAGENTA = 0xFFFF00FF;
    public static final int TRANSPARENT = 0x00000000;

    private Color() {
    }

    /** La couleur aux composantes HSV (la forme du sélecteur). */
    public static int HSVToColor(float[] hsv) {
        float h = hsv[0] % 360.0f;
        if (h < 0.0f) {
            h += 360.0f;
        }
        float s = clamp01(hsv[1]);
        float v = clamp01(hsv[2]);
        float c = v * s;
        float x = c * (1.0f - Math.abs((h / 60.0f) % 2.0f - 1.0f));
        float m = v - c;
        float r;
        float g;
        float b;
        if (h < 60.0f) {
            r = c; g = x; b = 0.0f;
        } else if (h < 120.0f) {
            r = x; g = c; b = 0.0f;
        } else if (h < 180.0f) {
            r = 0.0f; g = c; b = x;
        } else if (h < 240.0f) {
            r = 0.0f; g = x; b = c;
        } else if (h < 300.0f) {
            r = x; g = 0.0f; b = c;
        } else {
            r = c; g = 0.0f; b = x;
        }
        return 0xFF000000 | ((int) ((r + m) * 255.0f) << 16)
                | ((int) ((g + m) * 255.0f) << 8)
                | (int) ((b + m) * 255.0f);
    }

    /** Les composantes HSV d'une couleur opaque. */
    public static void colorToHSV(int color, float[] hsv) {
        float r = ((color >>> 16) & 0xFF) / 255.0f;
        float g = ((color >>> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        hsv[2] = max;
        hsv[1] = max == 0.0f ? 0.0f : delta / max;
        if (delta == 0.0f) {
            hsv[0] = 0.0f;
        } else if (max == r) {
            hsv[0] = 60.0f * (((g - b) / delta) % 6.0f);
        } else if (max == g) {
            hsv[0] = 60.0f * ((b - r) / delta + 2.0f);
        } else {
            hsv[0] = 60.0f * ((r - g) / delta + 4.0f);
        }
        if (hsv[0] < 0.0f) {
            hsv[0] += 360.0f;
        }
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
