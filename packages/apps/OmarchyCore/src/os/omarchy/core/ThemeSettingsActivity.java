package os.omarchy.core;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import os.omarchy.plugin.IOmarchyPluginManager;
import os.omarchy.plugin.PluginContract;

import java.util.Arrays;

/** Mobile settings surface for code-free themes and isolated OS extensions. */
public final class ThemeSettingsActivity extends Activity {
    private LinearLayout mThemeList;
    private LinearLayout mPluginList;
    private TextView mStatus;
    private TextView mThemeCount;
    private TextView mPluginCount;
    private IOmarchyPluginManager mManager;
    private boolean mBound;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mManager = IOmarchyPluginManager.Stub.asInterface(binder);
            render();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mManager = null;
            mStatus.setText(R.string.plugin_manager_unavailable);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_theme_settings);
        mThemeList = findViewById(R.id.theme_list);
        mPluginList = findViewById(R.id.plugin_list);
        mStatus = findViewById(R.id.theme_status);
        mThemeCount = findViewById(R.id.theme_count);
        mPluginCount = findViewById(R.id.plugin_count);
        findViewById(R.id.theme_root).requestFocus();
        styleSystemBars();

        Intent managerIntent = new Intent(PluginContract.ACTION_MANAGER)
                .setPackage(getPackageName());
        mBound = bindService(managerIntent, mConnection, Context.BIND_AUTO_CREATE);
        if (!mBound) {
            mStatus.setText(R.string.plugin_manager_unavailable);
        }
    }

    @Override
    protected void onDestroy() {
        if (mBound) {
            unbindService(mConnection);
        }
        super.onDestroy();
    }

    private void render() {
        if (mManager == null) {
            return;
        }
        try {
            mManager.refresh();
            String activeTheme = mManager.activeTheme();
            String[] themeIds = mManager.listThemeIds();
            String[] pluginIds = mManager.listPluginIds();
            mThemeList.removeAllViews();
            mPluginList.removeAllViews();

            for (String themeId : themeIds) {
                mThemeList.addView(createThemeCard(themeId, themeId.equals(activeTheme)));
            }

            int extensionCount = 0;
            for (String pluginId : pluginIds) {
                if (!contains(mManager.pluginCapabilities(pluginId),
                        PluginContract.CAPABILITY_THEME)) {
                    mPluginList.addView(createPluginRow(pluginId));
                    extensionCount++;
                }
            }

            mThemeCount.setText(getResources().getQuantityString(
                    R.plurals.theme_count, themeIds.length, themeIds.length));
            mPluginCount.setText(getResources().getQuantityString(
                    R.plurals.plugin_count, extensionCount, extensionCount));
            mStatus.setText(themeIds.length == 0
                    ? R.string.no_themes : R.string.signed_plugins_only);
        } catch (RemoteException exception) {
            mStatus.setText(R.string.plugin_manager_unavailable);
        }
    }

    private View createThemeCard(String id, boolean active) throws RemoteException {
        View card = LayoutInflater.from(this).inflate(
                R.layout.item_theme, mThemeList, false);
        TextView title = card.findViewById(R.id.theme_name);
        TextView subtitle = card.findViewById(R.id.theme_mode);
        TextView selected = card.findViewById(R.id.theme_selected);
        LinearLayout swatches = card.findViewById(R.id.theme_swatches);

        String label = mManager.pluginLabel(id);
        String mode = mManager.themeMode(id);
        title.setText(label);
        subtitle.setText(PluginContract.THEME_MODE_LIGHT.equals(mode)
                ? R.string.theme_mode_light : R.string.theme_mode_dark);
        selected.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
        card.setBackground(cardBackground(active));
        card.setContentDescription(active
                ? getString(R.string.theme_accessibility_active, label)
                : getString(R.string.theme_accessibility, label));

        int[] previewColors = mManager.themePreviewColors(id);
        for (int index = 0; index < Math.min(previewColors.length, 4); index++) {
            View swatch = new View(this);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(previewColors[index]);
            circle.setStroke(dp(1), getColor(R.color.omarchy_border));
            swatch.setBackground(circle);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(18), dp(18));
            if (index > 0) {
                params.setMarginStart(dp(6));
            }
            swatches.addView(swatch, params);
        }

        card.setOnClickListener(view -> {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
            applyTheme(id);
        });
        return card;
    }

    private View createPluginRow(String id) throws RemoteException {
        View row = LayoutInflater.from(this).inflate(
                R.layout.item_plugin, mPluginList, false);
        TextView icon = row.findViewById(R.id.plugin_icon);
        TextView title = row.findViewById(R.id.plugin_name);
        TextView subtitle = row.findViewById(R.id.plugin_description);
        Switch enabled = row.findViewById(R.id.plugin_enabled);
        String label = mManager.pluginLabel(id);
        String description = mManager.pluginDescription(id);

        icon.setText(label.isEmpty() ? "•" : label.substring(0, 1).toUpperCase());
        title.setText(label);
        subtitle.setText(description.isEmpty()
                ? getString(R.string.plugin_service_description) : description);
        enabled.setChecked(mManager.isPluginEnabled(id));
        enabled.setContentDescription(getString(R.string.plugin_toggle_accessibility, label));
        enabled.setOnCheckedChangeListener(
                (CompoundButton button, boolean checked) -> setPluginEnabled(id, checked));
        row.setBackground(cardBackground(false));
        return row;
    }

    private void applyTheme(String id) {
        try {
            if (mManager != null && mManager.applyTheme(id)) {
                render();
            } else {
                mStatus.setText(R.string.theme_apply_failed);
            }
        } catch (RemoteException exception) {
            mStatus.setText(R.string.theme_apply_failed);
        }
    }

    private void setPluginEnabled(String id, boolean enabled) {
        try {
            if (mManager == null || !mManager.setPluginEnabled(id, enabled)) {
                mStatus.setText(R.string.plugin_state_failed);
                render();
            } else {
                mStatus.setText(enabled
                        ? R.string.plugin_enabled : R.string.plugin_disabled);
            }
        } catch (RemoteException exception) {
            mStatus.setText(R.string.plugin_state_failed);
        }
    }

    private GradientDrawable cardBackground(boolean active) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(getColor(R.color.omarchy_surface_elevated));
        background.setCornerRadius(dp(20));
        background.setStroke(dp(active ? 2 : 1),
                getColor(active ? R.color.omarchy_accent : R.color.omarchy_border));
        return background;
    }

    private void styleSystemBars() {
        getWindow().setStatusBarColor(getColor(R.color.omarchy_surface));
        getWindow().setNavigationBarColor(getColor(R.color.omarchy_navigation));
        boolean light = getResources().getBoolean(R.bool.omarchy_is_light);
        int flags = 0;
        if (light) {
            flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private static boolean contains(String[] values, String expected) {
        return Arrays.asList(values == null ? new String[0] : values).contains(expected);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
