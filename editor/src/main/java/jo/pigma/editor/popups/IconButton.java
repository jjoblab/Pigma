package jo.pigma.editor.popups;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.widget.Button;

/**
 * Le bouton d'icône des barres flottantes, à la façon d'Android
 * Studio : pas de libellé, un glyphe dessiné au trait — le chevron
 * de navigation, la croix de fermeture, le rabat du remplacement.
 * La description de rôle porte le sens (la lecture d'écran la
 * prononce, le harnais la cherche).
 */
public final class IconButton extends Button {

    /** Les glyphes dessinés au trait. */
    public enum Icon {

        /** Le chevron vers le haut (correspondance précédente). */
        CHEVRON_UP,

        /** Le chevron vers le bas (correspondance suivante). */
        CHEVRON_DOWN,

        /** La croix de fermeture. */
        CLOSE,

        /** Deux chevrons tête-bêche (le rabat du remplacement). */
        TOGGLE,

        /** Une flèche vers une barre (remplacer la correspondance). */
        REPLACE,

        /** Deux flèches vers une barre (remplacer toutes). */
        REPLACE_ALL
    }

    /** Le glyphe porté. */
    private final Icon icon;

    /** Le trait du glyphe. */
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Un bouton d'icône.
     *
     * @param context     le contexte porteur
     * @param icon        le glyphe dessiné
     * @param description le rôle du bouton (accessibilité, harnais)
     * @param listener    le clic
     */
    public IconButton(Context context, Icon icon, String description,
                      View.OnClickListener listener) {
        super(context);
        this.icon = icon;
        setText("");
        setContentDescription(description);
        setOnClickListener(listener);
        setMinWidth(dp(40));
        setMinHeight(dp(36));
        paint.setColor(0xFF5F6368);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.5f, dp(1) * 1.6f));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2.0f;
        float cy = getHeight() / 2.0f;
        float s = dp(5);
        float k = s * 0.45f;
        switch (icon) {
            case CHEVRON_UP: {
                canvas.drawLine(cx - s, cy + k, cx, cy - k, paint);
                canvas.drawLine(cx, cy - k, cx + s, cy + k, paint);
                break;
            }
            case CHEVRON_DOWN: {
                canvas.drawLine(cx - s, cy - k, cx, cy + k, paint);
                canvas.drawLine(cx, cy + k, cx + s, cy - k, paint);
                break;
            }
            case CLOSE: {
                canvas.drawLine(cx - s, cy - s, cx + s, cy + s, paint);
                canvas.drawLine(cx - s, cy + s, cx + s, cy - s, paint);
                break;
            }
            case TOGGLE: {
                // deux chevrons tête-bêche : la rangée du
                // remplacement se replie sous la recherche
                float t = s * 0.55f;
                canvas.drawLine(cx - t, cy - k, cx, cy - k - t, paint);
                canvas.drawLine(cx, cy - k - t, cx + t, cy - k, paint);
                canvas.drawLine(cx - t, cy + k, cx, cy + k + t, paint);
                canvas.drawLine(cx, cy + k + t, cx + t, cy + k, paint);
                break;
            }
            case REPLACE: {
                // la flèche file vers la barre du texte remplacé
                canvas.drawLine(cx + s, cy - s, cx + s, cy + s, paint);
                canvas.drawLine(cx - s, cy, cx + s * 0.45f, cy, paint);
                canvas.drawLine(cx + s * 0.45f - k, cy - k, cx + s * 0.45f, cy, paint);
                canvas.drawLine(cx + s * 0.45f - k, cy + k, cx + s * 0.45f, cy, paint);
                break;
            }
            case REPLACE_ALL: {
                canvas.drawLine(cx + s, cy - s, cx + s, cy + s, paint);
                for (int row = 0; row < 2; row++) {
                    float y = cy + (row == 0 ? -s * 0.45f : s * 0.45f);
                    canvas.drawLine(cx - s, y, cx + s * 0.45f, y, paint);
                    canvas.drawLine(cx + s * 0.45f - k, y - k, cx + s * 0.45f, y, paint);
                    canvas.drawLine(cx + s * 0.45f - k, y + k, cx + s * 0.45f, y, paint);
                }
                break;
            }
            default:
                break;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
