package com.android.settings.omarchy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/** Bridges the Android Settings hierarchy to the isolated Omarchy platform package. */
public final class OmarchySettingsActivity extends Activity {
    private static final String TAG = "OmarchySettings";
    private static final String ACTION_PLUGIN_SETTINGS =
            "os.omarchy.intent.action.PLUGIN_SETTINGS";
    private static final String OMARCHY_CORE_PACKAGE = "os.omarchy.core";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Intent destination = new Intent(ACTION_PLUGIN_SETTINGS)
                .setPackage(OMARCHY_CORE_PACKAGE);
        if (destination.resolveActivity(getPackageManager()) != null) {
            startActivity(destination);
        } else {
            Log.e(TAG, "Omarchy Core is not installed");
        }
        finish();
    }
}
