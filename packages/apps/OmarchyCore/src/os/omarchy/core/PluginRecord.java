package os.omarchy.core;

import android.content.ComponentName;

import java.util.Collections;
import java.util.List;

final class PluginRecord {
    final String id;
    final String label;
    final String description;
    final String version;
    final ComponentName service;
    final List<String> capabilities;
    final List<String> overlays;
    final List<Integer> seedColors;
    final int[] previewColors;
    final String themeStyle;
    final String themeMode;

    PluginRecord(
            String id,
            String label,
            String description,
            String version,
            ComponentName service,
            List<String> capabilities,
            List<String> overlays,
            List<Integer> seedColors,
            int[] previewColors,
            String themeStyle,
            String themeMode) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.version = version;
        this.service = service;
        this.capabilities = Collections.unmodifiableList(capabilities);
        this.overlays = Collections.unmodifiableList(overlays);
        this.seedColors = Collections.unmodifiableList(seedColors);
        this.previewColors = previewColors.clone();
        this.themeStyle = themeStyle;
        this.themeMode = themeMode;
    }

    boolean hasCapability(String capability) {
        return capabilities.contains(capability);
    }

    boolean isTheme() {
        return hasCapability(os.omarchy.plugin.PluginContract.CAPABILITY_THEME);
    }
}
