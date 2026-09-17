package os.omarchy.core;

import android.app.ThemeManager;
import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.om.OverlayInfo;
import android.content.om.OverlayManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.theming.FieldColorSource;
import android.content.theming.ThemeSettings;
import android.content.theming.ThemeStyle;
import android.graphics.Color;
import android.os.UserHandle;
import android.util.Log;

import os.omarchy.plugin.PluginContract;

import java.util.ArrayList;
import java.util.List;

/** Applies a semantic theme pack to Android's per-user theme, UI mode, and RRO layers. */
final class ThemeController {
    private static final String TAG = "OmarchyThemeController";
    private static final String PREFS = "theme";
    private static final String KEY_ACTIVE = "active_plugin";
    private static final String KEY_PENDING = "pending_plugin";

    private final Context mContext;

    ThemeController(Context context) {
        mContext = context;
    }

    String activeTheme(int userId) {
        SharedPreferences preferences = preferences(userId);
        String active = preferences.getString(KEY_ACTIVE, "");
        if (active.equals(preferences.getString(KEY_PENDING, ""))) {
            preferences.edit().remove(KEY_PENDING).apply();
        }
        return active;
    }

    boolean apply(PluginRecord plugin, int userId) {
        if (plugin == null || !plugin.isTheme() || plugin.seedColors.isEmpty()) {
            return false;
        }

        Context userContext = mContext.createContextAsUser(UserHandle.of(userId), 0)
                .createDeviceProtectedStorageContext();
        ThemeManager themeManager = userContext.getSystemService(ThemeManager.class);
        OverlayManager overlayManager = userContext.getSystemService(OverlayManager.class);
        UiModeManager uiModeManager = userContext.getSystemService(UiModeManager.class);
        SharedPreferences preferences = preferences(userId);
        String previousId = preferences.getString(KEY_ACTIVE, "");
        ThemeSettings previousSettings = themeManager == null
                ? null : themeManager.getThemeSettings();
        int previousNightMode = uiModeManager == null
                ? UiModeManager.MODE_NIGHT_AUTO : uiModeManager.getNightMode();

        if (themeManager == null || overlayManager == null || uiModeManager == null
                || !validateOverlays(plugin, overlayManager, userId)) {
            Log.w(TAG, "Android theme services or trusted overlays are unavailable");
            return false;
        }

        // Record intent first. If the overlay relaunches this process, the next instance can
        // reconcile the selected theme instead of repeatedly applying it at boot.
        if (!preferences.edit()
                .putString(KEY_ACTIVE, plugin.id)
                .putString(KEY_PENDING, plugin.id)
                .commit()) {
            return false;
        }

        boolean themeUpdated = false;
        try {
            List<Color> seeds = new ArrayList<>();
            for (int seed : plugin.seedColors) {
                seeds.add(Color.valueOf(seed));
            }
            ThemeSettings settings = new ThemeSettings.Builder()
                    .setColorSource(FieldColorSource.VALUE_PRESET)
                    .setThemeStyle(ThemeStyle.valueOf(plugin.themeStyle))
                    .setSeedColors(seeds)
                    .build();
            themeUpdated = themeManager.updateThemeSettings(settings);
            if (!themeUpdated) {
                throw new IllegalStateException("Android rejected the theme palette");
            }

            uiModeManager.setNightMode(nightMode(plugin.themeMode, previousNightMode));
            for (String overlayPackage : plugin.overlays) {
                overlayManager.setEnabledExclusiveInCategory(
                        overlayPackage, UserHandle.of(userId));
            }
            preferences.edit().remove(KEY_PENDING).apply();
            return true;
        } catch (RuntimeException exception) {
            Log.e(TAG, "Unable to apply theme " + plugin.id + "; restoring previous state",
                    exception);
            if (themeUpdated && previousSettings != null) {
                try {
                    themeManager.updateThemeSettings(previousSettings);
                } catch (RuntimeException rollbackFailure) {
                    Log.e(TAG, "Unable to restore the previous Android palette", rollbackFailure);
                }
            }
            try {
                uiModeManager.setNightMode(previousNightMode);
            } catch (RuntimeException rollbackFailure) {
                Log.e(TAG, "Unable to restore the previous display mode", rollbackFailure);
            }
            preferences.edit()
                    .putString(KEY_ACTIVE, previousId)
                    .remove(KEY_PENDING)
                    .commit();
            return false;
        }
    }

    private boolean validateOverlays(
            PluginRecord plugin, OverlayManager overlayManager, int userId) {
        for (String packageName : plugin.overlays) {
            if (!isTrustedSystemPackage(packageName)) {
                return false;
            }
            OverlayInfo overlay = overlayManager.getOverlayInfo(
                    packageName, UserHandle.of(userId));
            if (overlay == null
                    || !PluginContract.THEME_OVERLAY_TARGET.equals(overlay.targetPackageName)
                    || !PluginContract.THEME_OVERLAY_CATEGORY.equals(overlay.category)) {
                Log.w(TAG, "Theme requested an invalid overlay: " + packageName);
                return false;
            }
        }
        return !plugin.overlays.isEmpty();
    }

    private boolean isTrustedSystemPackage(String packageName) {
        try {
            ApplicationInfo app = mContext.getPackageManager().getApplicationInfo(packageName, 0);
            boolean systemApp = (app.flags & (ApplicationInfo.FLAG_SYSTEM
                    | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            boolean codeFree = !app.hasCode();
            boolean sameSignature = mContext.getPackageManager().checkSignatures(
                    mContext.getPackageName(), packageName) == PackageManager.SIGNATURE_MATCH;
            return systemApp && codeFree && sameSignature;
        } catch (PackageManager.NameNotFoundException exception) {
            return false;
        }
    }

    private int nightMode(String mode, int fallback) {
        if (PluginContract.THEME_MODE_DARK.equals(mode)) {
            return UiModeManager.MODE_NIGHT_YES;
        }
        if (PluginContract.THEME_MODE_LIGHT.equals(mode)) {
            return UiModeManager.MODE_NIGHT_NO;
        }
        return fallback;
    }

    private SharedPreferences preferences(int userId) {
        Context userContext = mContext.createContextAsUser(UserHandle.of(userId), 0)
                .createDeviceProtectedStorageContext();
        return userContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
