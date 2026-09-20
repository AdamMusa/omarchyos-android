package os.omarchy.shell;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.service.dreams.DreamService;
import android.text.format.DateFormat;
import android.view.View;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;

/** Native Android screensaver: no Qt engine, network, or desktop processes. */
public final class OmarchyDreamService extends DreamService {
    private SaverView saver;

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(true); // Handle a tap explicitly; Android still owns the keyguard.
        setFullscreen(true);
        setScreenBright(false);
        saver = new SaverView(this);
        saver.setOnClickListener(view -> finish());
        setContentView(saver);
    }

    @Override public void onDreamingStarted() {
        super.onDreamingStarted();
        if (saver != null) saver.start();
    }

    @Override public void onDreamingStopped() {
        if (saver != null) saver.stop();
        super.onDreamingStopped();
    }

    @Override public void onDetachedFromWindow() {
        if (saver != null) saver.stop();
        saver = null;
        super.onDetachedFromWindow();
    }

    private static final class SaverView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final String[] logo;
        private int accent = Color.rgb(122, 162, 247);
        private int foreground = Color.rgb(192, 202, 245);
        private boolean running;
        private final Runnable tick = new Runnable() {
            @Override public void run() {
                if (!running) return;
                invalidate();
                postDelayed(this, 1000);
            }
        };

        SaverView(Context context) {
            super(context);
            setBackgroundColor(Color.BLACK);
            setContentDescription("Omarchy screensaver");
            paint.setTypeface(Typeface.MONOSPACE);
            String wordmark = "OMARCHY";
            try (java.io.InputStream in = getResources().openRawResource(R.raw.omarchy_wordmark)) {
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                for (int n; (n = in.read(buffer)) != -1;) bytes.write(buffer, 0, n);
                wordmark = new String(bytes.toByteArray(), StandardCharsets.UTF_8).replaceAll("\\s+$", "");
            } catch (Exception ignored) { }
            logo = wordmark.split("\n");
            // Read only the selected palette. Starting a dream must not initialize
            // ThemeRepository, the marketplace, JNI callbacks, or the Home activity.
            try {
                File colors = new File(context.getFilesDir(), ".local/state/omarchy/current/theme/colors.toml");
                if (colors.length() > 0 && colors.length() < 65536) {
                    ThemePalette palette = ThemePalette.parse(new String(Files.readAllBytes(colors.toPath()), StandardCharsets.UTF_8));
                    accent = Color.parseColor(palette.colors.get("accent"));
                    foreground = Color.parseColor(palette.colors.get("foreground"));
                    // Light themes can have nearly black text; retain visibility on OLED black.
                    if (Color.red(accent) + Color.green(accent) + Color.blue(accent) < 150)
                        accent = Color.LTGRAY;
                    if (Color.red(foreground) + Color.green(foreground) + Color.blue(foreground) < 240)
                        foreground = Color.LTGRAY;
                }
            } catch (Exception ignored) { }
        }

        void start() { stop(); running = true; post(tick); }
        void stop() { running = false; removeCallbacks(tick); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            float density = getResources().getDisplayMetrics().density;
            paint.setTextSize(16 * density);
            float widest = 1;
            for (String line : logo) widest = Math.max(widest, paint.measureText(line));
            float logoSize = Math.min(16 * density, 16 * density * w * .82f / widest);
            logoSize = Math.min(logoSize, h * .28f / (logo.length * 1.3f));
            paint.setTextSize(logoSize);
            float lineHeight = paint.getFontSpacing();
            float logoHeight = lineHeight * logo.length;
            float seconds = SystemClock.elapsedRealtime() / 1000f;
            // Slow drift avoids leaving the same bright pixels on screen.
            float x = w * .5f + (float) Math.sin(seconds / 53) * w * .05f;
            float padding = 24 * density;
            float groupHeight = logoHeight + 112 * density;
            float travel = Math.max(0, h - 2 * padding - groupHeight);
            float y = padding + lineHeight + travel * (.5f + .4f * (float) Math.sin(seconds / 71));
            paint.setColor(accent);
            paint.setTextAlign(Paint.Align.LEFT);
            float logoWidth = widest * logoSize / (16 * density);
            for (int i = 0; i < logo.length; i++)
                canvas.drawText(logo[i], x - logoWidth / 2, y + lineHeight * i, paint);
            paint.setColor(foreground);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(Math.min(32 * density, w / 8));
            String time = DateFormat.getTimeFormat(getContext()).format(new Date());
            canvas.drawText(time, x, y + logoHeight + 42 * density, paint);
            paint.setTextSize(13 * density);
            canvas.drawText(DateFormat.format("EEEE, MMM d", new Date()).toString(),
                    x, y + logoHeight + 70 * density, paint);
            paint.setTextSize(11 * density);
            paint.setAlpha(150);
            canvas.drawText("Swipe up to wake", x, y + logoHeight + 96 * density, paint);
            paint.setAlpha(255);
        }
    }
}
