package os.omarchy.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import os.omarchy.plugin.PluginContract;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(PluginContract.ACTION_MANAGER)
                .setPackage(context.getPackageName());
        context.startService(service);
    }
}
