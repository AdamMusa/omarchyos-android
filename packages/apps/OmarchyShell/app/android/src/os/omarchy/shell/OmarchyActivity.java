package os.omarchy.shell;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.qtproject.qt.android.bindings.QtActivity;

/**
 * The shell's activity. Qt keeps its own activity handle package-private, so
 * the shell keeps a reference here: ShellBridge and ShellSurfaces need the
 * context to reach system services and to set window flags.
 *
 * It also owns the z-order of Qt's own SurfaceView. Qt adds that surface below
 * the activity window (sub-layer -2), which relies on the window punching a
 * transparent region through its background for the scene to be visible. A
 * marker file ("top" or "overlay" in files/.surface-z) lifts the surface above
 * the window instead, without needing a rebuild to try the other arrangement.
 */
public class OmarchyActivity extends QtActivity {

    private static final String TAG = "omarchy-shell";

    private static OmarchyActivity sInstance;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Set<SurfaceView> mSeen = new HashSet<>();
    private String mZMode = "";
    private int mSweeps = 0;
    private BootCurtain mBootCurtain;
    private android.window.SplashScreenView mSplash;
    private boolean mHomeReady;

    public static OmarchyActivity get() { return sInstance; }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        sInstance = this;
        super.onCreate(savedInstanceState);
        // Qt's SurfaceView needs a transparent activity background. The opaque
        // curtain is a child above that surface, so rendering can start below it.
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mBootCurtain = new BootCurtain(this);
        // Qt replaces the activity content while loading its native libraries.
        // Attach to the decor instead so that replacement cannot cover/remove it.
        ((ViewGroup) getWindow().getDecorView()).addView(mBootCurtain, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        getSplashScreen().setOnExitAnimationListener(splash -> {
            mSplash = splash;
            // Qt can report a window before its scene has a complete frame.
            // Retain Android's Omarchy starting surface through that interval.
            if (mHomeReady) {
                splash.remove();
                mSplash = null;
            }
        });
        mHandler.postDelayed(() -> {
            if (mBootCurtain != null && !mHomeReady) {
                Log.e(TAG, "Home startup exceeded 15 seconds; keeping recovery navigation available");
                mBootCurtain.showDelay();
            }
        }, 15000);
        takeOverTheScreen();
        mZMode = readMarker();
        Log.i(TAG, "activity: surface z mode = '" + mZMode + "'");
        scheduleSweep();
    }

    /** Called only after Qt swaps a frame containing the bar and wallpaper. */
    public static void onShellFrameReady() {
        OmarchyActivity activity = sInstance;
        if (activity == null) return;
        activity.mHandler.post(() -> {
            if (sInstance != activity || activity.mHomeReady) return;
            activity.mHomeReady = true;
            if (activity.mBootCurtain != null) {
                ViewGroup parent = (ViewGroup) activity.mBootCurtain.getParent();
                if (parent != null) parent.removeView(activity.mBootCurtain);
                activity.mBootCurtain = null;
            }
            if (activity.mSplash != null) {
                activity.mSplash.remove();
                activity.mSplash = null;
            }
            activity.reportFullyDrawn();
            Log.i(TAG, "Home frame ready; boot curtain removed");
        });
    }

    @Override
    public void onDestroy() {
        if (sInstance == this) sInstance = null;
        mHandler.removeCallbacksAndMessages(null);
        if (mSplash != null) mSplash.remove();
        super.onDestroy();
    }

    /**
     * Omarchy's bar replaces Android's status bar, so that one is hidden and
     * the window is laid out edge to edge behind the cutout.
     *
     * The navigation bar is deliberately left in place. Back, home and recents
     * are Android services the shell inherits rather than reimplements, and in
     * gesture mode they work over every app on the device, including ones the
     * shell is not drawing. Hiding those insets would make the first swipe
     * reveal the bar instead of navigating, which is exactly the feature this
     * fork exists to keep. It is made transparent instead, so what shows is a
     * handle over Omarchy's own background rather than a second system chrome.
     *
     * This uses the insets controller rather than setting the QML window's
     * visibility to Window.FullScreen: asking Qt to change the platform
     * window's visibility state left the QtSurface and the QQuickWindow out of
     * step, and the scene stopped being rendered at all.
     */
    private void takeOverTheScreen() {
        getWindow().setDecorFitsSystemWindows(true);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        // Without this Android paints a scrim behind a transparent nav bar.
        getWindow().setNavigationBarContrastEnforced(false);
        getWindow().setStatusBarContrastEnforced(false);
        WindowInsetsController insets = getWindow().getInsetsController();
        if (insets == null) return;
        insets.hide(WindowInsets.Type.statusBars());
        insets.show(WindowInsets.Type.navigationBars());
        insets.setSystemBarsBehavior(
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Android restores its bars whenever focus comes back from a dialog.
        if (hasFocus) takeOverTheScreen();
    }

    private String readMarker() {
        File f = new File(getFilesDir(), ".surface-z");
        if (!f.isFile()) return "";
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[64];
            int n = in.read(buf);
            if (n <= 0) return "";
            return new String(buf, 0, n, StandardCharsets.UTF_8).trim().toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Qt creates its surface asynchronously, so the view tree is swept a few
     * times after start rather than once.
     */
    private void scheduleSweep() {
        mHandler.postDelayed(new Runnable() {
            @Override public void run() {
                try {
                    sweep(getWindow().getDecorView());
                    if (mBootCurtain != null) mBootCurtain.bringToFront();
                } catch (Exception e) {
                    Log.w(TAG, "activity: surface sweep failed", e);
                }
                if (++mSweeps < 12) scheduleSweep();
            }
        }, 500);
    }

    private void sweep(View v) {
        if (v instanceof SurfaceView) {
            SurfaceView sv = (SurfaceView) v;
            if (mSeen.add(sv)) {
                Log.i(TAG, "activity: qt surface " + sv.getWidth() + "x" + sv.getHeight()
                        + " visible=" + (sv.getVisibility() == View.VISIBLE)
                        + " class=" + sv.getClass().getName());
                if ("top".equals(mZMode)) {
                    sv.setZOrderOnTop(true);
                    Log.i(TAG, "activity: surface lifted above the window");
                } else if ("overlay".equals(mZMode)) {
                    sv.setZOrderMediaOverlay(true);
                    Log.i(TAG, "activity: surface set as media overlay");
                }
            }
            return;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) sweep(g.getChildAt(i));
        }
    }
}
