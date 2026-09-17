/*
 * Copyright 2026 OmarchyOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.terminal;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import os.omarchy.plugin.IOmarchyPlugin;
import os.omarchy.plugin.PluginContract;

/** Bounded out-of-process registration with the Omarchy plugin host. */
public final class CodePluginService extends Service {
    private static final String TAG = "OmarchyCodePlugin";

    private final IOmarchyPlugin.Stub mBinder = new IOmarchyPlugin.Stub() {
        @Override
        public int apiVersion() {
            return PluginContract.API_VERSION;
        }

        @Override
        public String pluginId() {
            return "omarchy.code";
        }

        @Override
        public String[] capabilities() {
            return new String[] {PluginContract.CAPABILITY_SERVICE};
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
