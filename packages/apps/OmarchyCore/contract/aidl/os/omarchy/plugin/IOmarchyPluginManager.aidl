package os.omarchy.plugin;

/** Platform service used by trusted Omarchy surfaces to inspect and control plugins. */
interface IOmarchyPluginManager {
    String[] listPluginIds();
    String[] listThemeIds();
    String activeTheme();
    String pluginLabel(String pluginId);
    String pluginDescription(String pluginId);
    String pluginVersion(String pluginId);
    String[] pluginCapabilities(String pluginId);
    int[] themePreviewColors(String pluginId);
    String themeMode(String pluginId);
    boolean isPluginEnabled(String pluginId);
    boolean setPluginEnabled(String pluginId, boolean enabled);
    void refresh();
    boolean applyTheme(String pluginId);
}
