package os.omarchy.core;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;

import os.omarchy.plugin.IOmarchyPluginManager;
import os.omarchy.plugin.PluginContract;

/** User-scoped host for theme packs and isolated, platform-signed plugins. */
public final class OmarchyPluginManagerService extends Service {
    private static final String DEFAULT_THEME = "omarchy.theme.tokyo-night";

    private PluginRegistry mRegistry;
    private PluginStateStore mState;
    private PluginEventDispatcher mEvents;
    private ThemeController mThemes;
    private final java.util.concurrent.ExecutorService mThemeWorker = java.util.concurrent.Executors.newSingleThreadExecutor();

    private final IOmarchyPluginManager.Stub mBinder = new IOmarchyPluginManager.Stub() {
        @Override
        public String[] listPluginIds() {
            enforceManagerPermission();
            return mRegistry.all().stream()
                    .map(plugin -> plugin.id)
                    .sorted()
                    .toArray(String[]::new);
        }

        @Override
        public String[] listThemeIds() {
            enforceManagerPermission();
            return mRegistry.idsWithCapability(PluginContract.CAPABILITY_THEME);
        }

        @Override
        public String activeTheme() {
            enforceManagerPermission();
            return mThemes.activeTheme(callingUserId());
        }

        @Override
        public String pluginLabel(String pluginId) {
            enforceManagerPermission();
            PluginRecord plugin = mRegistry.find(pluginId);
            return plugin == null ? "" : plugin.label;
        }

        @Override
        public String pluginDescription(String pluginId) {
            enforceManagerPermission();
            PluginRecord plugin = mRegistry.find(pluginId);
            return plugin == null ? "" : plugin.description;
        }

        @Override
        public String pluginVersion(String pluginId) {
            enforceManagerPermission();
            PluginRecord plugin = mRegistry.find(pluginId);
            return plugin == null ? "" : plugin.version;
        }

        @Override
        public String[] pluginCapabilities(String pluginId) {
            enforceManagerPermission();
            PluginRecord plugin = mRegistry.find(pluginId);
            return plugin == null
                    ? new String[0] : plugin.capabilities.toArray(new String[0]);
        }

        @Override
        public int[] themePreviewColors(String pluginId) {
            enforceManagerPermission();
            PluginRecord plugin = mRegistry.find(pluginId);
            return plugin == null ? new int[0] : plugin.previewColors.clone();
        }

        @Override
        public String themeMode(String pluginId) {
            enforceManagerPermission();
            PluginRecord plugin = mRegistry.find(pluginId);
            return plugin == null ? "" : plugin.themeMode;
        }

        @Override
        public boolean isPluginEnabled(String pluginId) {
            enforceManagerPermission();
            return mState.isEnabled(mRegistry.find(pluginId), callingUserId());
        }

        @Override
        public boolean setPluginEnabled(String pluginId, boolean enabled) {
            enforceManagerPermission();
            int userId = callingUserId();
            PluginRecord plugin = mRegistry.find(pluginId);
            long identity = Binder.clearCallingIdentity();
            try {
                boolean changed = mState.setEnabled(plugin, userId, enabled);
                if (changed && enabled) {
                    Bundle event = new Bundle();
                    event.putString("plugin-id", plugin.id);
                    mEvents.dispatch(plugin, PluginContract.EVENT_PLUGIN_ENABLED, event);
                }
                return changed;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public void refresh() {
            enforceManagerPermission();
            int userId = callingUserId();
            long identity = Binder.clearCallingIdentity();
            try {
                refreshAndNotify(userId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public boolean applyTheme(String pluginId) {
            enforceManagerPermission();
            int userId = callingUserId();
            long identity = Binder.clearCallingIdentity();
            try {
                PluginRecord theme = mRegistry.find(pluginId);
                if (!mThemes.apply(theme, userId)) {
                    return false;
                }
                dispatchThemeChanged(theme, userId);
                return true;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        ContextFactory contexts = new ContextFactory(createDeviceProtectedStorageContext());
        mRegistry = new PluginRegistry(contexts.deviceProtected);
        mState = new PluginStateStore(contexts.deviceProtected);
        mEvents = new PluginEventDispatcher(contexts.deviceProtected);
        mThemes = new ThemeController(contexts.deviceProtected);
        mRegistry.refresh();
        ensureDefaultTheme(UserHandle.myUserId());
        notifyHostReady(UserHandle.myUserId());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "os.omarchy.intent.action.APPLY_PALETTE".equals(intent.getAction())) {
            final android.os.ResultReceiver result;
            final String id, mode;
            final Bundle palette;
            try {
                result = intent.getParcelableExtra("result", android.os.ResultReceiver.class);
                id = intent.getStringExtra("theme_id");
                mode = intent.getStringExtra("mode");
                palette = intent.getBundleExtra("palette");
            } catch (RuntimeException malformed) {
                android.util.Log.w("OmarchyThemes", "Invalid palette request", malformed);
                return START_STICKY;
            }
            mThemeWorker.execute(() -> {
                boolean success = mThemes.applyPalette(id, mode, palette, UserHandle.myUserId());
                if (result != null) result.send(success ? 1 : 0, Bundle.EMPTY);
            });
        } else {
            refreshAndNotify(UserHandle.myUserId());
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mThemeWorker.shutdown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void refreshAndNotify(int userId) {
        mRegistry.refresh();
        ensureDefaultTheme(userId);
        notifyHostReady(userId);
    }

    private void ensureDefaultTheme(int userId) {
        if (!mThemes.activeTheme(userId).isEmpty()) {
            return;
        }
        mThemes.apply(mRegistry.find(DEFAULT_THEME), userId);
    }

    private void notifyHostReady(int userId) {
        for (PluginRecord plugin : mRegistry.all()) {
            if (mState.isEnabled(plugin, userId)) {
                mEvents.dispatch(plugin, PluginContract.EVENT_HOST_READY, Bundle.EMPTY);
            }
        }
    }

    private void dispatchThemeChanged(PluginRecord theme, int userId) {
        Bundle event = new Bundle();
        event.putString(PluginContract.EXTRA_THEME_ID, theme.id);
        event.putString(PluginContract.EXTRA_THEME_MODE, theme.themeMode);
        for (PluginRecord plugin : mRegistry.all()) {
            if (mState.isEnabled(plugin, userId)) {
                mEvents.dispatch(plugin, PluginContract.EVENT_THEME_CHANGED, event);
            }
        }
    }

    private int callingUserId() {
        return UserHandle.getUserId(Binder.getCallingUid());
    }

    private void enforceManagerPermission() {
        enforceCallingOrSelfPermission(
                PluginContract.PERMISSION_MANAGE, "Omarchy plugin access is OS-signature only");
    }

    /** Keeps context construction explicit so every store remains direct-boot safe. */
    private static final class ContextFactory {
        final android.content.Context deviceProtected;

        ContextFactory(android.content.Context deviceProtected) {
            this.deviceProtected = deviceProtected;
        }
    }
}
