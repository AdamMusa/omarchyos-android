package os.omarchy.core;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.ResultReceiver;

/** Root-only development image probe. Not packaged in the OS. */
public final class AndroidPaletteProbe {
    public static void main(String[] args) throws Exception {
        Looper.prepareMainLooper();
        Bundle colors = new Bundle();
        for (int i = 2; i < args.length; i++) {
            String[] pair = args[i].split("=", 2); colors.putString(pair[0], pair[1]);
        }
        ResultReceiver result = new ResultReceiver(new Handler(Looper.getMainLooper())) {
            @Override protected void onReceiveResult(int code, Bundle data) {
                System.out.println("Palette result: " + code); System.exit(code == 1 ? 0 : 1);
            }
        };
        Parcel parcel = Parcel.obtain();
        result.writeToParcel(parcel, 0); parcel.setDataPosition(0);
        ResultReceiver remote = ResultReceiver.CREATOR.createFromParcel(parcel); parcel.recycle();
        android.app.ActivityManager.getService().startService(null, new Intent("os.omarchy.intent.action.APPLY_PALETTE")
                .setClassName("os.omarchy.core", "os.omarchy.core.OmarchyPluginManagerService")
                .putExtra("theme_id", args[0]).putExtra("mode", args[1])
                .putExtra("palette", colors).putExtra("result", remote), null, false, "com.android.shell", null, 0);
        new Handler(Looper.getMainLooper()).postDelayed(() -> { System.err.println("Palette timeout"); System.exit(2); }, 20000);
        Looper.loop();
    }
}
