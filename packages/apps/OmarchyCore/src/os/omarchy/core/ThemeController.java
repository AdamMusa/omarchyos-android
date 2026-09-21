package os.omarchy.core;

import android.app.ThemeManager;
import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.om.OverlayInfo;
import android.content.om.FabricatedOverlay;
import android.content.om.OverlayIdentifier;
import android.content.om.OverlayManagerTransaction;
import android.os.Bundle;
import android.util.TypedValue;
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

    /** Android generates its dynamic overlay asynchronously after a palette request. */
    synchronized void maintainPalettePriority(int userId) {
        if (!preferences(userId).getString(KEY_ACTIVE, "").startsWith("omarchy.mobile.")) return;
        OverlayManager manager = mContext.getSystemService(OverlayManager.class);
        if (manager == null) return;
        OverlayIdentifier identifier = new OverlayIdentifier("os.omarchy.core", "mobile_system_palette_u" + userId);
        try {
            List<OverlayInfo> overlays = manager.getOverlayInfosForTarget("android", UserHandle.of(userId));
            OverlayInfo selected = null, lastEnabled = null;
            for (OverlayInfo overlay : overlays) {
                if (overlay.isEnabled()) lastEnabled = overlay;
                if (identifier.equals(overlay.getOverlayIdentifier())) selected = overlay;
            }
            if (selected != null && selected.isEnabled() && selected != lastEnabled) {
                // Enabling through a transaction also promotes the overlay. Do this
                // only when its order changed, so our own broadcast is a no-op.
                manager.commit(new OverlayManagerTransaction.Builder()
                        .setEnabled(identifier, true, userId).build());
            }
        } catch (RuntimeException failure) {
            Log.w(TAG, "Unable to retain the selected system palette", failure);
        }
    }

    synchronized boolean apply(PluginRecord plugin, int userId) {
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
            OverlayManagerTransaction.Builder cleanup = new OverlayManagerTransaction.Builder();
            for (String name : new String[]{"mobile_palette_u", "mobile_system_palette_u", "mobile_status_palette_u"}) {
                OverlayIdentifier mobile = new OverlayIdentifier("os.omarchy.core", name + userId);
                if (overlayManager.getOverlayInfo(mobile, UserHandle.of(userId)) != null)
                    cleanup.setEnabled(mobile, false, userId);
            }
            overlayManager.commit(cleanup.build());
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

    /** Only the platform-signed shell can send data to this service. No theme code is loaded. */
    synchronized boolean applyPalette(String id, String mode, Bundle palette, int userId) {
        if (id == null || !id.matches("[a-z0-9_][a-z0-9._+-]{0,79}") || palette == null
                || !("light".equals(mode) || "dark".equals(mode))) return false;
        Context context = mContext.createContextAsUser(UserHandle.of(userId), 0);
        ThemeManager themes = context.getSystemService(ThemeManager.class);
        OverlayManager overlays = context.getSystemService(OverlayManager.class);
        UiModeManager ui = context.getSystemService(UiModeManager.class);
        if (themes == null || overlays == null || ui == null) return false;
        ThemeSettings previous = themes.getThemeSettings();
        int previousMode = ui.getNightMode();
        boolean updated = false;
        try {
            int bg = paletteColor(palette, "background", null);
            int fg = paletteColor(palette, "foreground", null);
            int accent = paletteColor(palette, "accent", null);
            FabricatedOverlay.Builder overlay = new FabricatedOverlay.Builder(
                    "os.omarchy.core", "mobile_palette_u" + userId, "os.omarchy.core")
                    .setTargetOverlayable("OmarchyThemeTokens");
            String[] tokens = {"surface", "surface_elevated", "surface_high", "border", "text",
                    "muted", "accent", "positive", "warning", "destructive", "navigation"};
            int[] values = {bg, blend(bg, fg, .06f), blend(bg, fg, .12f), blend(bg, fg, .24f), fg,
                    blend(bg, fg, .65f), accent, paletteColor(palette, "green", "accent"),
                    paletteColor(palette, "yellow", "accent"), paletteColor(palette, "red", "accent"), bg};
            for (int i = 0; i < tokens.length; i++) overlay.setResourceValue(
                    "os.omarchy.core:color/omarchy_" + tokens[i], TypedValue.TYPE_INT_COLOR_ARGB8, values[i]);
            overlay.setResourceValue("os.omarchy.core:bool/omarchy_is_light", TypedValue.TYPE_INT_BOOLEAN,
                    "light".equals(mode) ? 1 : 0);
            updated = themes.updateThemeSettings(new ThemeSettings.Builder()
                    .setColorSource(FieldColorSource.VALUE_PRESET).setThemeStyle(ThemeStyle.TONAL_SPOT)
                    .setSeedColors(List.of(Color.valueOf(accent))).build());
            if (!updated) throw new IllegalStateException("Android palette rejected");
            ui.setNightMode("light".equals(mode) ? UiModeManager.MODE_NIGHT_NO : UiModeManager.MODE_NIGHT_YES);
            OverlayManagerTransaction.Builder transaction = new OverlayManagerTransaction.Builder()
                    .registerFabricatedOverlay(overlay.build())
                    .setEnabled(new OverlayIdentifier("os.omarchy.core", "mobile_palette_u" + userId), true, userId);
            FabricatedOverlay.Builder framework = new FabricatedOverlay.Builder(
                    "os.omarchy.core", "mobile_system_palette_u" + userId, "android");
            for (java.util.Map.Entry<String, Integer> entry : SystemPalette.colors(bg, fg, accent,
                    paletteColor(palette, "red", "accent")).entrySet()) {
                if (context.getResources().getIdentifier(entry.getKey(), "color", "android") != 0)
                    framework.setResourceValue("android:color/" + entry.getKey(),
                            TypedValue.TYPE_INT_COLOR_ARGB8, entry.getValue());
            }
            transaction.registerFabricatedOverlay(framework.build()).setEnabled(
                    new OverlayIdentifier("os.omarchy.core", "mobile_system_palette_u" + userId), true, userId);
            FabricatedOverlay.Builder systemUi = new FabricatedOverlay.Builder(
                    "os.omarchy.core", "mobile_status_palette_u" + userId, "com.android.systemui");
            int text = SystemPalette.readable(fg, bg);
            for (String token : new String[]{"dark_mode_icon_color_single_tone", "light_mode_icon_color_single_tone",
                    "dark_mode_icon_color_dual_tone_fill", "light_mode_icon_color_dual_tone_fill", "status_bar_clock_color"})
                systemUi.setResourceValue("com.android.systemui:color/" + token, TypedValue.TYPE_INT_COLOR_ARGB8, text);
            for (String token : new String[]{"dark_mode_icon_color_dual_tone_background", "light_mode_icon_color_dual_tone_background"})
                systemUi.setResourceValue("com.android.systemui:color/" + token, TypedValue.TYPE_INT_COLOR_ARGB8,
                        SystemPalette.blend(bg, text, .3));
            transaction.registerFabricatedOverlay(systemUi.build()).setEnabled(
                    new OverlayIdentifier("os.omarchy.core", "mobile_status_palette_u" + userId), true, userId);
            // Migrate the early preview's shared identifier to a separate overlay per user.
            OverlayIdentifier legacy = new OverlayIdentifier("os.omarchy.core", "mobile_palette");
            if (overlays.getOverlayInfo(legacy, UserHandle.of(userId)) != null) transaction.setEnabled(legacy, false, userId);
            overlays.commit(transaction.build());
            preferences(userId).edit().putString(KEY_ACTIVE, "omarchy.mobile." + id).remove(KEY_PENDING).commit();
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to apply mobile theme " + id, e);
            try { if (updated && previous != null) themes.updateThemeSettings(previous); ui.setNightMode(previousMode); }
            catch (RuntimeException rollback) { Log.e(TAG, "Unable to restore previous palette", rollback); }
            return false;
        }
    }

    private static int paletteColor(Bundle palette, String key, String fallback) {
        String value = palette.getString(key, fallback == null ? "" : palette.getString(fallback, ""));
        if (!value.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Invalid palette color: " + key);
        return Color.parseColor(value);
    }

    private static int blend(int background, int foreground, float amount) {
        return Color.rgb(Math.round(Color.red(background) * (1 - amount) + Color.red(foreground) * amount),
                Math.round(Color.green(background) * (1 - amount) + Color.green(foreground) * amount),
                Math.round(Color.blue(background) * (1 - amount) + Color.blue(foreground) * amount));
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
