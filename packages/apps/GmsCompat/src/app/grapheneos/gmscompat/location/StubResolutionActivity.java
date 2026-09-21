package app.grapheneos.gmscompat.location;

import android.annotation.Nullable;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import app.grapheneos.gmscompat.R;

public class StubResolutionActivity extends Activity {
    private static final String TAG = "StubResolutionActivity";

    public static final String ID_LOCATION_ACCESS_IS_GLOBALLY_DISABLED = "location_is_globally_disabled";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate, callingActivity: " + getCallingActivity());
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        String id = getIntent().getIdentifier();
        if (id != null) {
            switch (id) {
                case ID_LOCATION_ACCESS_IS_GLOBALLY_DISABLED ->
                    showToast(getText(R.string.toast_app_location_access_is_globally_off));
            }
        }
        finish();
    }

    private static Toast prevToast;

    private void showToast(CharSequence text) {
        Toast prev = prevToast;
        if (prev != null) {
            prev.cancel();
            prevToast = null;
        }
        Toast t = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        t.show();
        prevToast = t;
    }
}
