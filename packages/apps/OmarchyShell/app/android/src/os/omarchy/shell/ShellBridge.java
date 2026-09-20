package os.omarchy.shell;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.HashSet;
import java.io.File;
import java.io.FileOutputStream;

/**
 * The Android half of quickshell-compat. Every Linux daemon Omarchy's shell
 * talks to has one method here: PipeWire becomes AudioManager, UPower becomes
 * BatteryManager, NetworkManager becomes ConnectivityManager/WifiManager, and
 * Wayland toplevels become the recents task list.
 *
 * Structured results are returned as JSON so there is a single marshalling
 * path back into C++ rather than a JNI signature per field.
 */
public class ShellBridge {

    private static ShellBridge sInstance;

    public static synchronized ShellBridge get() {
        if (sInstance == null) sInstance = new ShellBridge();
        return sInstance;
    }

    /** Called once from C++; wires up the broadcast receivers. */
    public static void attach() {
        get().registerReceivers();
        ThemeRepository.get(get().context());
    }

    public static void themesChanged() { nativeStateChanged("themes"); }
    public String themeStateJson() { return ThemeRepository.get(context()).state(); }
    public void refreshThemeMarketplace() { ThemeRepository.get(context()).refreshMarket(); }
    public void installTheme(String id) { ThemeRepository.get(context()).install(id); }
    public void applyTheme(String id) { ThemeRepository.get(context()).apply(id); }

    private static native void nativeStateChanged(String what);

    private Context context() {
        return OmarchyActivity.get();
    }

    private void registerReceivers() {
        Context ctx = context();
        if (ctx == null) return;

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(AudioManager.ACTION_HEADSET_PLUG);
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        // Idleness: Omarchy's screensaver and lock run off the seat's idle
        // notifications upstream, and the display turning off is Android's
        // equivalent signal.
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        ctx.registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;
                if (action.equals(Intent.ACTION_BATTERY_CHANGED)
                        || action.equals(Intent.ACTION_POWER_CONNECTED)
                        || action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
                    nativeStateChanged("battery");
                } else if (action.startsWith("android.media")) {
                    nativeStateChanged("audio");
                } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    nativeStateChanged("network");
                } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    nativeStateChanged("bluetooth");
                } else if (action.equals(Intent.ACTION_SCREEN_ON)
                        || action.equals(Intent.ACTION_USER_PRESENT)) {
                    nativeStateChanged("screen-on");
                } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    nativeStateChanged("screen-off");
                }
            }
        }, filter, Context.RECEIVER_EXPORTED);

        IntentFilter packages = new IntentFilter();
        packages.addAction(Intent.ACTION_PACKAGE_ADDED);
        packages.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packages.addDataScheme("package");
        ctx.registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent intent) { nativeStateChanged("packages"); }
        }, packages, Context.RECEIVER_EXPORTED);
    }

    // -------------------------------------------------------------- idle

    /** Whether the display is on. A dark screen is idle whatever the timer says. */
    public boolean isScreenOn() {
        Context ctx = context();
        if (ctx == null) return true;
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        return pm == null || pm.isInteractive();
    }

    /**
     * Android's equivalent of a Wayland idle inhibitor. Held wake locks are not
     * readable by an app — PowerManager exposes no query — so the shell relies
     * on the display state instead and treats "no inhibitor" as the answer.
     * A foreground app keeping the screen on shows up as the screen staying
     * interactive, which the idle timer already accounts for.
     */
    public boolean isWakeLockHeld() {
        return false;
    }

    /** Where the packaged bash lands; the only place an app may execute from. */
    public String nativeLibraryDir() {
        Context ctx = context();
        return ctx == null ? "" : ctx.getApplicationInfo().nativeLibraryDir;
    }

    // ------------------------------------------------------------ battery

    private BatteryManager battery() {
        return (BatteryManager) context().getSystemService(Context.BATTERY_SERVICE);
    }

    public int batteryPercent() {
        BatteryManager bm = battery();
        return bm == null ? 100 : bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    public boolean isCharging() {
        BatteryManager bm = battery();
        return bm != null && bm.isCharging();
    }

    public int batteryTimeToEmpty() {
        BatteryManager bm = battery();
        if (bm == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return 0;
        long millis = bm.computeChargeTimeRemaining();
        return millis > 0 ? (int) (millis / 1000) : 0;
    }

    public int batteryTimeToFull() {
        return isCharging() ? batteryTimeToEmpty() : 0;
    }

    // -------------------------------------------------------------- audio

    private AudioManager audio() {
        return (AudioManager) context().getSystemService(Context.AUDIO_SERVICE);
    }

    public float volume() {
        AudioManager am = audio();
        if (am == null) return 0.5f;
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return max == 0 ? 0f : (float) am.getStreamVolume(AudioManager.STREAM_MUSIC) / max;
    }

    public void setVolume(float value) {
        AudioManager am = audio();
        if (am == null) return;
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(value * max), 0);
        nativeStateChanged("audio");
    }

    public boolean isMuted() {
        AudioManager am = audio();
        return am != null && am.isStreamMute(AudioManager.STREAM_MUSIC);
    }

    public void setMuted(boolean muted) {
        AudioManager am = audio();
        if (am == null) return;
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                muted ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, 0);
        nativeStateChanged("audio");
    }

    public float micVolume() { return 1.0f; }

    public boolean isMicMuted() {
        AudioManager am = audio();
        return am != null && am.isMicrophoneMute();
    }

    public String audioOutputName() {
        AudioManager am = audio();
        if (am == null) return "Speaker";
        if (am.isBluetoothA2dpOn()) return "Bluetooth";
        if (am.isWiredHeadsetOn()) return "Headphones";
        return "Speaker";
    }

    // ------------------------------------------------------------ network

    public String networkType() {
        ConnectivityManager cm = (ConnectivityManager) context().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return "disconnected";
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (caps == null) return "disconnected";
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "wifi";
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "cellular";
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "ethernet";
        return "disconnected";
    }

    public String wifiSsid() {
        WifiManager wm = (WifiManager) context().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wm == null) return "";
        WifiInfo info = wm.getConnectionInfo();
        if (info == null || info.getSSID() == null) return "";
        return info.getSSID().replace("\"", "");
    }

    public int wifiSignal() {
        WifiManager wm = (WifiManager) context().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wm == null) return 0;
        WifiInfo info = wm.getConnectionInfo();
        return info == null ? 0 : WifiManager.calculateSignalLevel(info.getRssi(), 5);
    }

    public String wifiNetworksJson() { return "[]"; }

    public void connectWifi(String ssid, String password) { /* handled by Settings panel */ }

    public void setWifiEnabled(boolean enabled) { /* privileged; routed through omarchy-mobile */ }

    // ---------------------------------------------------------- bluetooth

    public boolean bluetoothEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null && adapter.isEnabled();
    }

    public String bluetoothDevicesJson() {
        JSONArray out = new JSONArray();
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) return out.toString();
            for (BluetoothDevice device : adapter.getBondedDevices()) {
                JSONObject o = new JSONObject();
                o.put("name", device.getName());
                o.put("address", device.getAddress());
                o.put("connected", false);
                out.put(o);
            }
        } catch (Exception ignored) { }
        return out.toString();
    }

    public void setBluetoothEnabled(boolean enabled) { /* privileged; via omarchy-mobile */ }

    // --------------------------------------------------------------- apps

    // Android owns settings, permissions, installation and removal. The shell
    // opens the platform screens instead of trying Linux administration tools.
    public void openSettings(String page) {
        String action;
        switch (page) {
            case "wifi": action = Settings.ACTION_WIFI_SETTINGS; break;
            case "bluetooth": action = Settings.ACTION_BLUETOOTH_SETTINGS; break;
            case "sound": action = Settings.ACTION_SOUND_SETTINGS; break;
            case "display": action = Settings.ACTION_DISPLAY_SETTINGS; break;
            case "apps": action = Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS; break;
            case "battery": action = Settings.ACTION_BATTERY_SAVER_SETTINGS; break;
            case "storage": action = Settings.ACTION_INTERNAL_STORAGE_SETTINGS; break;
            case "accessibility": action = Settings.ACTION_ACCESSIBILITY_SETTINGS; break;
            case "security": action = Settings.ACTION_SECURITY_SETTINGS; break;
            default: action = Settings.ACTION_SETTINGS;
        }
        openSystemScreen(new Intent(action));
    }

    public void openAppInfo(String packageName) {
        openSystemScreen(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)));
    }

    private void openSystemScreen(Intent intent) {
        OmarchyActivity activity = OmarchyActivity.get();
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            try {
                activity.startActivity(intent);
            } catch (android.content.ActivityNotFoundException e) {
                activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
    }

    public String installedAppsJson() {
        JSONArray out = new JSONArray();
        Context ctx = context();
        if (ctx == null) return out.toString();
        PackageManager pm = ctx.getPackageManager();
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(launcher, 0);
        HashSet<String> seen = new HashSet<>();
        for (ResolveInfo info : apps) {
            try {
                ApplicationInfo app = info.activityInfo.applicationInfo;
                if (!seen.add(app.packageName)) continue;
                JSONObject o = new JSONObject();
                // Field names match what Omarchy's AppLibrary expects from a
                // .desktop entry, so its search and menu code is unchanged.
                o.put("id", app.packageName);
                o.put("name", pm.getApplicationLabel(app).toString());
                o.put("icon", app.packageName);
                o.put("comment", "");
                o.put("execString", app.packageName);
                o.put("noDisplay", false);
                out.put(o);
            } catch (Exception ignored) { }
        }
        return out.toString();
    }

    public void launchApp(String packageName) {
        Context ctx = context();
        if (ctx == null) return;
        Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        }
    }

    public String iconUrl(String name) {
        // Cache the platform drawable once per package version. An unregistered
        // image:// provider left every native app icon blank in the menu.
        Context ctx = context();
        if (ctx == null || !name.matches("[A-Za-z0-9._]+")) return "";
        try {
            PackageManager pm = ctx.getPackageManager();
            long version = pm.getPackageInfo(name, 0).lastUpdateTime;
            File dir = new File(ctx.getCacheDir(), "app-icons");
            if (!dir.isDirectory() && !dir.mkdirs()) return "";
            File file = new File(dir, name + "-" + version + ".png");
            if (!file.isFile()) {
                Drawable drawable = pm.getApplicationIcon(name);
                Bitmap bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
                drawable.setBounds(0, 0, 128, 128);
                drawable.draw(new Canvas(bitmap));
                try (FileOutputStream out = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                } finally {
                    bitmap.recycle();
                }
            }
            return Uri.fromFile(file).toString();
        } catch (Exception e) {
            return "";
        }
    }

    // -------------------------------------------------------------- tasks

    public String recentTasksJson() {
        JSONArray out = new JSONArray();
        Context ctx = context();
        if (ctx == null) return out.toString();
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return out.toString();
        try {
            List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(10);
            int frontTaskId = tasks.isEmpty() ? -1 : tasks.get(0).taskId;
            for (ActivityManager.RunningTaskInfo task : tasks) {
                JSONObject o = new JSONObject();
                o.put("id", task.taskId);
                o.put("appId", task.topActivity != null ? task.topActivity.getPackageName() : "");
                o.put("title", task.topActivity != null ? task.topActivity.getShortClassName() : "");
                o.put("activated", task.taskId == frontTaskId);
                out.put(o);
            }
        } catch (Exception ignored) { }
        return out.toString();
    }

    public void moveToTask(int taskId) {
        ActivityManager am = (ActivityManager) context().getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) am.moveTaskToFront(taskId, 0);
    }

    public void closeForegroundTask() { /* requires REMOVE_TASKS; routed via omarchy-mobile */ }

    // ------------------------------------------------------------ surfaces

    public void setShellInset(String surface, String edge, int size, int layer, int keyboardFocus) {
        ShellSurfaces.get().setInset(surface, edge, size, layer, keyboardFocus);
    }

    public void setOutsideTouchGrab(boolean active) { ShellSurfaces.get().setOutsideTouchGrab(active); }

    public void setLockVisible(boolean visible) { ShellSurfaces.get().setLockVisible(visible); }

    // ----------------------------------------------- notifications + auth

    public String notificationsJson() { return NotificationBridge.activeJson(); }

    public String foregroundServicesJson() { return "[]"; }

    public void dismissNotification(String key) { NotificationBridge.dismiss(key); }

    public String mediaSessionsJson() { return "[]"; }

    public void beginAuth() { /* BiometricPrompt, wired with the lock plugin */ }

    public void cancelAuth() { }

    public void submitPassword(String password) { }
}
