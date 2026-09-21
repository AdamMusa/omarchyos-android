package os.omarchy.shell;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import java.nio.charset.StandardCharsets;

/** The boot identity stays above Qt until a complete Home frame is presented. */
final class BootCurtain extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String[] logo;
    private boolean delayed;

    BootCurtain(Context context) {
        super(context);
        setBackgroundColor(Color.rgb(7, 9, 15));
        setClickable(true);
        setContentDescription("Omarchy is starting");
        paint.setTypeface(Typeface.MONOSPACE);
        String text = "OMARCHY";
        try (java.io.InputStream in = getResources().openRawResource(R.raw.omarchy_wordmark)) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8).stripTrailing();
        } catch (java.io.IOException ignored) { }
        logo = text.split("\n");
    }

    void showDelay() {
        delayed = true;
        setContentDescription("Omarchy is taking longer to start. System navigation is available.");
        announceForAccessibility(getContentDescription());
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setTextSize(20);
        float widest = 1;
        for (String line : logo) widest = Math.max(widest, paint.measureText(line));
        paint.setTextSize(Math.min(20 * getWidth() * .82f / widest,
                getHeight() * .2f / (logo.length * 1.2f)));
        float step = paint.getFontSpacing();
        float top = (getHeight() - step * logo.length) / 2;
        paint.setColor(Color.rgb(105, 230, 197));
        for (int row = 0; row < logo.length; row++)
            canvas.drawText(logo[row], (getWidth() - paint.measureText(logo[row])) / 2,
                    top + row * step - paint.ascent(), paint);
        if (delayed) {
            paint.setTextSize(14 * getResources().getDisplayMetrics().scaledDensity);
            paint.setColor(Color.LTGRAY);
            String message = "Omarchy is taking longer to start";
            paint.setTextSize(Math.min(paint.getTextSize(), paint.getTextSize() * getWidth() * .9f / paint.measureText(message)));
            canvas.drawText(message, (getWidth() - paint.measureText(message)) / 2,
                    top + logo.length * step + paint.getFontSpacing() * 2, paint);
        }
    }
}
