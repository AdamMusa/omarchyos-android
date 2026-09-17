package os.omarchy.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.UserHandle;

/** Per-user enabled state. Trusted built-ins default to enabled, matching Omarchy. */
final class PluginStateStore {
    private static final String PREFS = "plugin-state";
    private static final String KEY_PREFIX = "enabled.";

    private final Context mContext;

    PluginStateStore(Context context) {
        mContext = context;
    }

    boolean isEnabled(PluginRecord plugin, int userId) {
        if (plugin == null) {
            return false;
        }
        if (plugin.isTheme()) {
            return true;
        }
        return preferences(userId).getBoolean(KEY_PREFIX + plugin.id, true);
    }

    boolean setEnabled(PluginRecord plugin, int userId, boolean enabled) {
        if (plugin == null || plugin.isTheme()) {
            return false;
        }
        return preferences(userId).edit().putBoolean(KEY_PREFIX + plugin.id, enabled).commit();
    }

    private SharedPreferences preferences(int userId) {
        Context userContext = mContext.createContextAsUser(UserHandle.of(userId), 0)
                .createDeviceProtectedStorageContext();
        return userContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
