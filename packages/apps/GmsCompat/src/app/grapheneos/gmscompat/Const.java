package app.grapheneos.gmscompat;

import android.app.compat.gms.GmsCompat;

public interface Const {
    boolean ENABLE_LOGGING = true;
    boolean IS_DEV_BUILD = GmsCompat.isDevBuild();
}
