package os.omarchy.plugin;

import android.os.Bundle;

/** Binder boundary implemented by an OS-signed Omarchy plugin. */
interface IOmarchyPlugin {
    int apiVersion();
    String pluginId();
    String[] capabilities();
    String status();
    oneway void onHostEvent(String event, in Bundle extras);
}
