package os.omarchy.shell;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.View;
import java.nio.charset.StandardCharsets;

/** Android Dream host for Omarchy's unmodified upstream ttfx animations. */
public final class OmarchyDreamService extends DreamService {
    private SaverView saver;
    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(true);
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
        private final String wordmark;
        private final int logoColumns;
        private int columns, rows;
        private float cellWidth, cellHeight, baseline;
        private volatile TerminalEffectFrame frame;
        private boolean dreaming;
        private Renderer renderer;

        SaverView(Context context) {
            super(context);
            setBackgroundColor(Color.BLACK);
            setContentDescription("Omarchy animated screensaver. Swipe up to wake.");
            paint.setTypeface(Typeface.MONOSPACE);
            String text = "OMARCHY";
            try (java.io.InputStream in = getResources().openRawResource(R.raw.omarchy_wordmark)) {
                text = new String(in.readAllBytes(), StandardCharsets.UTF_8).stripTrailing();
            } catch (java.io.IOException error) { Log.w("OmarchyDream", "Wordmark unavailable", error); }
            wordmark = text;
            int width = 1;
            for (String line : text.split("\n")) width = Math.max(width, line.length());
            logoColumns = width;
        }
        void start() { dreaming = true; restart(); }
        void stop() {
            dreaming = false;
            if (renderer != null) { renderer.close(); renderer = null; }
        }
        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            if (width <= 0 || height <= 0) return;
            columns = Math.min(120, Math.max(16, logoColumns + 8));
            paint.setTextSize(20);
            paint.setTextSize(20 * width / (columns * paint.measureText("M")));
            cellWidth = paint.measureText("M");
            cellHeight = paint.getFontSpacing();
            baseline = -paint.ascent();
            rows = Math.max(8, Math.min(160, (int) (height / cellHeight)));
            restart();
        }
        private TerminalEffectFrame stillFrame() {
            String[] lines = wordmark.split("\n");
            StringBuilder text = new StringBuilder("\033[38;2;122;162;247m");
            for (int i = 0; i < Math.max(0, (rows - lines.length) / 2); i++) text.append('\n');
            String padding = " ".repeat(Math.max(0, (columns - logoColumns) / 2));
            for (String line : lines) text.append(padding).append(line).append('\n');
            return TerminalEffectFrame.decode(text.toString(), columns, rows);
        }
        private void restart() {
            if (renderer != null) { renderer.close(); renderer = null; }
            if (!dreaming || columns == 0 || rows == 0) return;
            frame = stillFrame();
            invalidate();
            if (ValueAnimator.areAnimatorsEnabled()) {
                renderer = new Renderer(columns, rows);
                renderer.start();
            }
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            TerminalEffectFrame current = frame;
            if (current == null) return;
            float left = (getWidth() - current.columns * cellWidth) / 2;
            float top = (getHeight() - current.rows * cellHeight) / 2;
            for (int at = 0; at < current.glyphs.length; at++) {
                int style = current.styles[at], fg = current.foreground[at], bg = current.background[at];
                if ((style & TerminalEffectFrame.REVERSE) != 0) { int swap = fg; fg = bg; bg = swap; }
                float x = left + (at % current.columns) * cellWidth;
                float y = top + (at / current.columns) * cellHeight;
                if (bg != Color.BLACK) {
                    paint.setColor(bg); paint.setAlpha(255);
                    canvas.drawRect(x, y, x + cellWidth, y + cellHeight, paint);
                }
                if (current.glyphs[at] == ' ' || (style & TerminalEffectFrame.HIDDEN) != 0) continue;
                paint.setColor(fg);
                paint.setAlpha((style & TerminalEffectFrame.DIM) != 0 ? 128 : 255);
                paint.setFakeBoldText((style & TerminalEffectFrame.BOLD) != 0);
                paint.setUnderlineText((style & TerminalEffectFrame.UNDERLINE) != 0);
                paint.setStrikeThruText((style & TerminalEffectFrame.STRIKE) != 0);
                paint.setTextSkewX((style & TerminalEffectFrame.ITALIC) != 0 ? -.2f : 0);
                canvas.drawText(current.glyphs, at, 1, x, y + baseline, paint);
            }
        }
        private final class Renderer implements AutoCloseable {
            private final HandlerThread thread = new HandlerThread("Omarchy-ttfx");
            private final int width, height;
            private Handler worker;
            private volatile boolean active = true;
            private OmarchyTextEffects effect;
            Renderer(int width, int height) { this.width = width; this.height = height; }
            void start() { thread.start(); worker = new Handler(thread.getLooper()); worker.post(tick); }
            private final Runnable tick = new Runnable() {
                @Override public void run() {
                    if (!active) return;
                    long started = SystemClock.uptimeMillis();
                    try {
                        if (effect == null) effect = new OmarchyTextEffects(wordmark, width, height, System.nanoTime());
                        String output = effect.next();
                        if (output == null) {
                            effect.close(); effect = null;
                            worker.postDelayed(this, 1200);
                        } else {
                            TerminalEffectFrame decoded = TerminalEffectFrame.decode(output, width, height);
                            if (active) { frame = decoded; postInvalidateOnAnimation(); }
                            worker.postDelayed(this, Math.max(1, 34 - (SystemClock.uptimeMillis() - started)));
                        }
                    } catch (RuntimeException | LinkageError error) {
                        Log.e("OmarchyDream", "Animation stopped", error);
                        if (effect != null) { effect.close(); effect = null; }
                        active = false;
                        thread.quitSafely();
                    }
                }
            };
            @Override public void close() {
                active = false;
                worker.removeCallbacksAndMessages(null);
                worker.post(() -> {
                    if (effect != null) { effect.close(); effect = null; }
                    thread.quitSafely();
                });
            }
        }
    }
}
