package os.omarchy.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.ArraySet;
import android.util.Log;

import os.omarchy.plugin.IOmarchyPlugin;
import os.omarchy.plugin.PluginContract;

import java.util.Arrays;
import java.util.Set;

/** Delivers bounded, out-of-process lifecycle events without loading plugin code into the host. */
final class PluginEventDispatcher {
    private static final String TAG = "OmarchyPluginEvents";
    private static final long BIND_TIMEOUT_MILLIS = 3_000;

    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    PluginEventDispatcher(Context context) {
        mContext = context;
    }

    void dispatch(PluginRecord plugin, String event, Bundle extras) {
        if (plugin == null || plugin.service == null) {
            return;
        }
        Bundle immutableExtras = extras == null ? Bundle.EMPTY : new Bundle(extras);
        mHandler.post(() -> bindAndDispatch(plugin, event, immutableExtras));
    }

    private void bindAndDispatch(PluginRecord plugin, String event, Bundle extras) {
        EventConnection connection = new EventConnection(plugin, event, extras);
        Intent intent = new Intent(PluginContract.ACTION_PLUGIN).setComponent(plugin.service);
        try {
            if (!mContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
                Log.w(TAG, "Unable to bind " + plugin.id);
                return;
            }
            connection.bound = true;
            mHandler.postDelayed(connection.timeout, BIND_TIMEOUT_MILLIS);
        } catch (RuntimeException exception) {
            Log.w(TAG, "Unable to dispatch " + event + " to " + plugin.id, exception);
        }
    }

    private final class EventConnection implements ServiceConnection {
        final PluginRecord plugin;
        final String event;
        final Bundle extras;
        final Runnable timeout = this::finish;
        boolean bound;
        boolean finished;

        EventConnection(PluginRecord plugin, String event, Bundle extras) {
            this.plugin = plugin;
            this.event = event;
            this.extras = extras;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            try {
                IOmarchyPlugin remote = IOmarchyPlugin.Stub.asInterface(binder);
                if (remote.apiVersion() != PluginContract.API_VERSION
                        || !plugin.id.equals(remote.pluginId())
                        || !capabilitiesMatch(remote.capabilities())) {
                    Log.w(TAG, "Runtime contract mismatch for " + plugin.id);
                    return;
                }
                remote.onHostEvent(event, extras);
            } catch (RemoteException | RuntimeException exception) {
                Log.w(TAG, "Plugin event failed for " + plugin.id, exception);
            } finally {
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            finish();
        }

        @Override
        public void onBindingDied(ComponentName name) {
            finish();
        }

        @Override
        public void onNullBinding(ComponentName name) {
            finish();
        }

        boolean capabilitiesMatch(String[] reported) {
            Set<String> expected = new ArraySet<>(plugin.capabilities);
            Set<String> actual = new ArraySet<>(Arrays.asList(
                    reported == null ? new String[0] : reported));
            return expected.equals(actual);
        }

        void finish() {
            if (finished) {
                return;
            }
            finished = true;
            mHandler.removeCallbacks(timeout);
            if (bound) {
                try {
                    mContext.unbindService(this);
                } catch (IllegalArgumentException ignored) {
                    // The framework may already have released a dead binding.
                }
            }
        }
    }
}
