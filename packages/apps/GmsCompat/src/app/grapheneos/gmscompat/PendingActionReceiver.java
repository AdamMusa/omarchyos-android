package app.grapheneos.gmscompat;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.GosPackageState;
import android.content.pm.GosPackageStateFlag;
import android.os.Bundle;

import java.util.UUID;

public class PendingActionReceiver extends BroadcastReceiver {
    private static final String ACTION_SET_PREF_BOOLEAN_AND_CANCEL_NOTIF = "set_pref_boolean_and_cancel_notif";
    private static final String ACTION_WRITE_GOS_PACKAGE_STATE_AND_CANCEL_NOTIF = "write_gosps_and_cancel_notif";
    private static final String EXTRA_BOOL_PREF_KEY = "bool_pref_key";
    private static final String EXTRA_BOOL_PREF_VALUE = "bool_pref_value";
    private static final String EXTRA_GOS_PACKAGE_STATE_FLAG = "gosps_flag";
    private static final String EXTRA_GOS_PACKAGE_STATE_FLAG_VALUE = "gosps_flag_value";
    private static final String EXTRA_NOTIF_ID = "notif_id";

    public static PendingIntent makeWriteBoolPrefAndCancelNotif(Context ctx, String key, boolean val, int notifId) {
        var i = new Intent(ACTION_SET_PREF_BOOLEAN_AND_CANCEL_NOTIF);
        i.putExtra(EXTRA_BOOL_PREF_KEY, key);
        i.putExtra(EXTRA_BOOL_PREF_VALUE, val);
        i.putExtra(EXTRA_NOTIF_ID, notifId);
        return getPendingIntent(ctx, i, PendingIntent.FLAG_ONE_SHOT);
    }

    public static PendingIntent makeWriteGosPackageStateAndCancelNotif(
            Context ctx, String pkgName,
            @GosPackageStateFlag.Enum int gosPsFlag, boolean gosPsFlagValue, int notifId) {
        var i = new Intent(ACTION_WRITE_GOS_PACKAGE_STATE_AND_CANCEL_NOTIF);
        i.putExtra(Intent.EXTRA_PACKAGE_NAME, pkgName);
        i.putExtra(EXTRA_GOS_PACKAGE_STATE_FLAG, gosPsFlag);
        i.putExtra(EXTRA_GOS_PACKAGE_STATE_FLAG_VALUE, gosPsFlagValue);
        i.putExtra(EXTRA_NOTIF_ID, notifId);
        return getPendingIntent(ctx, i, PendingIntent.FLAG_ONE_SHOT);
    }

    private static PendingIntent getPendingIntent(Context ctx, Intent intent, int extraFlags) {
        // distinctness check ignores intent extras, need to use a unique identifier instead
        intent.setIdentifier(UUID.randomUUID().toString());
        intent.setComponent(new ComponentName(ctx, PendingActionReceiver.class));
        return PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE | extraFlags);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        switch (intent.getAction()) {
            case ACTION_SET_PREF_BOOLEAN_AND_CANCEL_NOTIF -> {
                String key = extras.getString(EXTRA_BOOL_PREF_KEY);
                boolean value = extras.getBoolean2(EXTRA_BOOL_PREF_VALUE);
                SharedPreferences.Editor ed = App.preferences().edit();
                ed.putBoolean(key, value);
                ed.apply();
                int notifId = extras.getNumber(EXTRA_NOTIF_ID);
                Notifications.cancel(notifId);
            }
            case ACTION_WRITE_GOS_PACKAGE_STATE_AND_CANCEL_NOTIF -> {
                String pkgName = extras.getString(Intent.EXTRA_PACKAGE_NAME);
                @GosPackageStateFlag.Enum int gosPsFlag = extras.getNumber(EXTRA_GOS_PACKAGE_STATE_FLAG);
                boolean gosPsFlagValue = extras.getBoolean2(EXTRA_GOS_PACKAGE_STATE_FLAG_VALUE);

                GosPackageState.Editor ed = GosPackageState.edit(pkgName, context.getUser());
                ed.setFlagState(gosPsFlag, gosPsFlagValue);
                ed.apply();

                int notifId = extras.getNumber(EXTRA_NOTIF_ID);
                Notifications.cancel(notifId);
            }
        }
    }
}
