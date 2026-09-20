package os.omarchy.shell;

import android.view.WindowManager;


/**
 * Turns Quickshell's layer-shell vocabulary into Android window behaviour.
 *
 * A PanelWindow with an exclusive zone is a shell surface that other windows
 * must not cover, which on Android means publishing an inset; a surface on the
 * overlay layer must sit above the status bar and the keyguard; a popup that
 * grabs focus outside itself needs FLAG_WATCH_OUTSIDE_TOUCH.
 */
public class ShellSurfaces {

    private static ShellSurfaces sInstance;
    private int topInset;
    private int bottomInset;

    public static synchronized ShellSurfaces get() {
        if (sInstance == null) sInstance = new ShellSurfaces();
        return sInstance;
    }

    /**
     * Records what the shell has claimed at each edge.
     *
     * This deliberately does not touch the activity window. Publishing an
     * exclusive zone is a compositor operation; the nearest Android equivalent
     * is a system-level inset, which an app cannot set for other apps. An
     * earlier version called Window.setAttributes() here, which did nothing for
     * layout but forced a relayout of the activity window on every inset the
     * shell published — and each relayout recreated the SurfaceView that Qt was
     * rendering into, so the scene stopped reaching the display and the screen
     * froze on whatever had been drawn first.
     */
    public void setInset(String surface, String edge, int size, int layer, int keyboardFocus) {
        if ("top".equals(edge)) topInset = size;
        else if ("bottom".equals(edge)) bottomInset = size;
    }

    /** Space claimed at the top of the display, in pixels. */
    public int topInset() { return topInset; }

    /** Space claimed at the bottom of the display, in pixels. */
    public int bottomInset() { return bottomInset; }

    public void setOutsideTouchGrab(boolean active) {
        if (OmarchyActivity.get() == null) return;
        OmarchyActivity.get().runOnUiThread(new Runnable() {
            @Override public void run() {
                int flag = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
                if (active) OmarchyActivity.get().getWindow().addFlags(flag);
                else OmarchyActivity.get().getWindow().clearFlags(flag);
            }
        });
    }

    public void setLockVisible(boolean visible) {
        if (OmarchyActivity.get() == null) return;
        OmarchyActivity.get().runOnUiThread(new Runnable() {
            @Override public void run() {
                int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
                if (visible) OmarchyActivity.get().getWindow().addFlags(flags);
                else OmarchyActivity.get().getWindow().clearFlags(flags);
            }
        });
    }
}
