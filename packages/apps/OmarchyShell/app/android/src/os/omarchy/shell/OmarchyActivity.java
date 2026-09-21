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
    private static final java.util.concurrent.atomic.AtomicReference<String> sBarAction =
            new java.util.concurrent.atomic.AtomicReference<>("");

    public static boolean hasNativeSystemBar() {
        return "Omarchy".equalsIgnoreCase(android.os.Build.BRAND);
    }

    public static String takeSystemBarAction() { return sBarAction.getAndSet(""); }

    private void acceptSystemBarAction(android.content.Intent intent) {
        String action = intent == null ? null : intent.getStringExtra("os.omarchy.SYSTEM_BAR_ACTION");
        if (!"menu".equals(action) && !"calendar".equals(action) && !"settings".equals(action)) return;
        sBarAction.set(action);
        intent.removeExtra("os.omarchy.SYSTEM_BAR_ACTION");
        if (mHomeReady) ShellBridge.systemBarActionChanged();
    }

    @Override public void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        acceptSystemBarAction(intent);
    }

    public static OmarchyActivity get() { return sInstance; }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        sInstance = this;
        super.onCreate(savedInstanceState);
        acceptSystemBarAction(getIntent());
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
            ShellBridge.systemBarActionChanged();
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

    /** Keep the OS-owned Omarchy bar on Home, just as on native service pages.
     * Stock-Android development hosts retain the local QML bar instead.
     * Qt must keep its ordinary visible state to preserve SurfaceView rendering.
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
        if (hasNativeSystemBar()) insets.show(WindowInsets.Type.statusBars());
        else insets.hide(WindowInsets.Type.statusBars());
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
