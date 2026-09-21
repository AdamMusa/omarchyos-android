package app.grapheneos.gmscompat.lib.playintegrity;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.GosPackageState;
import android.ext.PackageId;
import android.ext.settings.app.AswBlockPlayIntegrityApi;
import android.os.IBinder;

import java.util.function.UnaryOperator;

import app.grapheneos.gmscompat.lib.util.ServiceConnectionWrapper;

import static android.app.compat.gms.GmsCompat.appContext;

public class PlayIntegrityUtils {
    private static final String TAG = "PlayIntegrityUtils";

    public static ServiceConnection maybeReplaceServiceConnection(Intent service, ServiceConnection orig) {
        if (PackageId.PLAY_STORE_NAME.equals(service.getPackage())) {
            UnaryOperator<IBinder> binderOverride = null;

            final String CLASSIC_SERVICE =
                    "com.google.android.play.core.integrityservice.BIND_INTEGRITY_SERVICE";
            final String STANDARD_SERVICE =
                    "com.google.android.play.core.expressintegrityservice.BIND_EXPRESS_INTEGRITY_SERVICE";

            String action = service.getAction();
            if (STANDARD_SERVICE.equals(action)) {
                binderOverride = StandardPlayIntegrityServiceWrapper::new;
            } else if (CLASSIC_SERVICE.equals(action)) {
                binderOverride = ClassicPlayIntegrityServiceWrapper::new;
            }

            if (binderOverride != null) {
                return new ServiceConnectionWrapper(orig, binderOverride);
            }
        }
        return null;
    }

    static boolean isPlayIntegrityBlocked() {
        Context ctx = appContext();
        return AswBlockPlayIntegrityApi.I.get(ctx, ctx.getUserId(), ctx.getApplicationInfo(), GosPackageState.getForSelf(ctx));
    }
}
