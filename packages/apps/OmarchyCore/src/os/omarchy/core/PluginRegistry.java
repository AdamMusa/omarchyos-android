package os.omarchy.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.om.OverlayInfo;
import android.content.om.OverlayManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import os.omarchy.plugin.PluginContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Discovers platform-signed executable plugins and code-free theme packs. */
final class PluginRegistry {
    private static final String TAG = "OmarchyPluginRegistry";
    private static final Pattern VALID_ID =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");
    private static final Set<String> SERVICE_CAPABILITIES = new ArraySet<>(Arrays.asList(
            PluginContract.CAPABILITY_AGENT,
            PluginContract.CAPABILITY_SERVICE,
            PluginContract.CAPABILITY_SETTINGS_PANEL,
            PluginContract.CAPABILITY_SYSTEMUI_TILE,
            PluginContract.CAPABILITY_LAUNCHER_WIDGET));

    private final Context mContext;
    private final PackageManager mPackageManager;
    private final OverlayManager mOverlayManager;
    private final Map<String, PluginRecord> mPlugins = new ArrayMap<>();

    PluginRegistry(Context context) {
        mContext = context;
        mPackageManager = context.getPackageManager();
        mOverlayManager = context.getSystemService(OverlayManager.class);
    }

    synchronized void refresh() {
        Map<String, PluginRecord> discovered = new ArrayMap<>();
        discoverThemePacks(discovered);
        discoverServicePlugins(discovered);

        mPlugins.clear();
        mPlugins.putAll(discovered);
        Log.i(TAG, "Loaded " + mPlugins.size() + " trusted plugin(s), including "
                + idsWithCapability(PluginContract.CAPABILITY_THEME).length + " theme pack(s)");
    }

    synchronized List<PluginRecord> all() {
        return new ArrayList<>(mPlugins.values());
    }

    synchronized PluginRecord find(String id) {
        return mPlugins.get(id);
    }

    synchronized String[] idsWithCapability(String capability) {
        List<String> ids = new ArrayList<>();
        for (PluginRecord plugin : mPlugins.values()) {
            if (plugin.hasCapability(capability)) {
                ids.add(plugin.id);
            }
        }
        Collections.sort(ids);
        return ids.toArray(new String[0]);
    }

    private void discoverServicePlugins(Map<String, PluginRecord> discovered) {
        Intent query = new Intent(PluginContract.ACTION_PLUGIN);
        int flags = PackageManager.GET_META_DATA | PackageManager.MATCH_SYSTEM_ONLY;

        for (ResolveInfo result : mPackageManager.queryIntentServices(query, flags)) {
            ServiceInfo service = result.serviceInfo;
            if (!isTrusted(service.applicationInfo)
                    || !PluginContract.PERMISSION_MANAGE.equals(service.permission)) {
                Log.w(TAG, "Ignoring untrusted plugin service " + service.getComponentName());
                continue;
            }

            Bundle metadata = service.metaData;
            String id = string(metadata, PluginContract.META_ID);
            List<String> capabilities = split(string(
                    metadata, PluginContract.META_CAPABILITIES));
            if (!validMetadata(metadata, id, capabilities)
                    || capabilities.contains(PluginContract.CAPABILITY_THEME)
                    || !SERVICE_CAPABILITIES.containsAll(capabilities)
                    || discovered.containsKey(id)) {
                Log.w(TAG, "Ignoring invalid or duplicate plugin " + service.getComponentName());
                continue;
            }

            String label = string(metadata, PluginContract.META_NAME);
            if (label.isEmpty()) {
                label = String.valueOf(service.applicationInfo.loadLabel(mPackageManager));
            }
            discovered.put(id, new PluginRecord(
                    id,
                    label,
                    string(metadata, PluginContract.META_DESCRIPTION),
                    valueOr(string(metadata, PluginContract.META_VERSION), "1.0"),
                    new ComponentName(service.packageName, service.name),
                    capabilities,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    new int[0],
                    "",
                    PluginContract.THEME_MODE_SYSTEM));
        }
    }

    private void discoverThemePacks(Map<String, PluginRecord> discovered) {
        if (mOverlayManager == null) {
            Log.w(TAG, "Overlay service is unavailable; no theme packs can be discovered");
            return;
        }

        int flags = PackageManager.GET_META_DATA | PackageManager.MATCH_SYSTEM_ONLY;
        for (ApplicationInfo application : mPackageManager.getInstalledApplications(flags)) {
            Bundle metadata = application.metaData;
            String id = string(metadata, PluginContract.META_ID);
            if (id.isEmpty() || metadata == null
                    || !metadata.getBoolean(PluginContract.META_THEME_PACK, false)) {
                continue;
            }

            OverlayInfo overlay = mOverlayManager.getOverlayInfo(
                    application.packageName, UserHandle.SYSTEM);
            List<Integer> seedColors = parseColors(string(
                    metadata, PluginContract.META_SEED_COLORS));
            String mode = string(metadata, PluginContract.META_THEME_MODE);
            boolean validOverlay = overlay != null
                    && PluginContract.THEME_OVERLAY_TARGET.equals(overlay.targetPackageName)
                    && PluginContract.THEME_OVERLAY_CATEGORY.equals(overlay.category);
            if (!isTrusted(application) || application.hasCode() || !validOverlay
                    || !validMetadata(metadata, id,
                            Collections.singletonList(PluginContract.CAPABILITY_THEME))
                    || seedColors.isEmpty() || !isValidMode(mode)
                    || discovered.containsKey(id)) {
                Log.w(TAG, "Ignoring invalid theme pack " + application.packageName);
                continue;
            }

            String label = string(metadata, PluginContract.META_NAME);
            if (label.isEmpty()) {
                label = String.valueOf(application.loadLabel(mPackageManager));
            }
            int[] preview = toIntArray(parseColors(string(
                    metadata, PluginContract.META_PREVIEW_COLORS)));
            if (preview.length == 0) {
                preview = toIntArray(seedColors);
            }
            discovered.put(id, new PluginRecord(
                    id,
                    label,
                    string(metadata, PluginContract.META_DESCRIPTION),
                    valueOr(string(metadata, PluginContract.META_VERSION), "1.0"),
                    null,
                    Collections.singletonList(PluginContract.CAPABILITY_THEME),
                    Collections.singletonList(application.packageName),
                    seedColors,
                    preview,
                    valueOr(string(metadata, PluginContract.META_THEME_STYLE), "TONAL_SPOT"),
                    mode));
        }
    }

    private boolean isTrusted(ApplicationInfo app) {
        boolean systemApp = (app.flags & (ApplicationInfo.FLAG_SYSTEM
                | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        boolean sameSignature = mPackageManager.checkSignatures(
                mContext.getPackageName(), app.packageName) == PackageManager.SIGNATURE_MATCH;
        return systemApp && sameSignature;
    }

    private static boolean validMetadata(
            Bundle metadata, String id, List<String> capabilities) {
        return metadata != null
                && metadata.getInt(PluginContract.META_SCHEMA_VERSION, 0)
                        == PluginContract.API_VERSION
                && VALID_ID.matcher(id).matches()
                && !capabilities.isEmpty();
    }

    private static boolean isValidMode(String mode) {
        return PluginContract.THEME_MODE_DARK.equals(mode)
                || PluginContract.THEME_MODE_LIGHT.equals(mode)
                || PluginContract.THEME_MODE_SYSTEM.equals(mode);
    }

    private static String string(Bundle metadata, String key) {
        if (metadata == null) {
            return "";
        }
        Object value = metadata.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String valueOr(String value, String fallback) {
        return value.isEmpty() ? fallback : value;
    }

    private static List<String> split(String value) {
        if (value.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .forEach(values::add);
        return values;
    }

    private static List<Integer> parseColors(String value) {
        List<Integer> colors = new ArrayList<>();
        for (String part : split(value)) {
            try {
                String normalized = part.startsWith("#") ? part.substring(1) : part;
                long parsed = Long.parseLong(normalized, 16);
                if (normalized.length() == 6) {
                    parsed |= 0xff000000L;
                }
                if (normalized.length() == 6 || normalized.length() == 8) {
                    colors.add((int) parsed);
                }
            } catch (NumberFormatException exception) {
                Log.w(TAG, "Ignoring malformed theme color " + part);
            }
        }
        return colors;
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = values.get(index);
        }
        return result;
    }
}
