package os.omarchy.agent;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import os.omarchy.plugin.IOmarchyPlugin;
import os.omarchy.plugin.PluginContract;

public final class AgentPluginService extends Service {
    private static final String TAG = "OmarchyAgentPlugin";

    private final IOmarchyPlugin.Stub mBinder = new IOmarchyPlugin.Stub() {
        @Override
        public int apiVersion() {
            return PluginContract.API_VERSION;
        }

        @Override
        public String pluginId() {
            return "omarchy.agent";
        }

        @Override
        public String[] capabilities() {
            return new String[] {PluginContract.CAPABILITY_AGENT};
        }

        @Override
        public String status() {
            return "ready";
        }

        @Override
        public void onHostEvent(String event, Bundle extras) {
            Log.i(TAG, "Received host event " + event);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
