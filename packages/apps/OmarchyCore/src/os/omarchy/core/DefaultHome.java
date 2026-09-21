package os.omarchy.core;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.util.Log;

/** Provision the system Home once, without overriding a user's chosen launcher. */
final class DefaultHome {
    private static final String SHELL = "os.omarchy.shell";

    static void ensure(Context context) {
        // Setup is temporarily the Home role holder. Do not mistake it for a
        // user's launcher choice or compete with Android's provisioning flow.
        if (android.provider.Settings.Secure.getInt(context.getContentResolver(),
                android.provider.Settings.Secure.USER_SETUP_COMPLETE, 0) == 0) return;
        android.content.SharedPreferences state = context.createDeviceProtectedStorageContext()
                .getSharedPreferences("home_provisioning", Context.MODE_PRIVATE);
        if (state.getBoolean("complete", false)) return;
        RoleManager roles = context.getSystemService(RoleManager.class);
        if (roles == null || !roles.isRoleAvailable(RoleManager.ROLE_HOME)) return;
        java.util.List<String> holders = roles.getRoleHolders(RoleManager.ROLE_HOME);
        if (!holders.isEmpty()) {
            // Setup can set USER_SETUP_COMPLETE before relinquishing HOME.
            // Treat its temporary role separately from an actual launcher choice.
            android.content.Intent setup = new android.content.Intent(android.content.Intent.ACTION_MAIN)
                    .addCategory("android.intent.category.SETUP_WIZARD");
            for (android.content.pm.ResolveInfo candidate : context.getPackageManager()
                    .queryIntentActivities(setup, PackageManager.MATCH_DISABLED_COMPONENTS)) {
                if (holders.contains(candidate.activityInfo.packageName)) return;
            }
            state.edit().putBoolean("complete", true).apply();
            return;
        }
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo shell = pm.getApplicationInfo(SHELL, 0);
            if ((shell.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                    || pm.checkSignatures(context.getPackageName(), SHELL)
                    != PackageManager.SIGNATURE_MATCH) return;
            roles.addRoleHolderAsUser(RoleManager.ROLE_HOME, SHELL, 0, Process.myUserHandle(),
                    context.getMainExecutor(), success -> {
                        if (success) state.edit().putBoolean("complete", true).apply();
                        else Log.w("OmarchyDefaultHome", "Unable to provision system Home");
                    });
        } catch (PackageManager.NameNotFoundException | RuntimeException error) {
            Log.w("OmarchyDefaultHome", "System Home is not ready", error);
        }
    }
}
