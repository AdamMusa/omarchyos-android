package os.omarchy.plugin;

/** Shared constants only; plugins communicate through AIDL, not shared implementation code. */
public final class PluginContract {
    public static final int API_VERSION = 1;

    public static final String ACTION_PLUGIN = "os.omarchy.intent.action.PLUGIN";
    public static final String ACTION_MANAGER = "os.omarchy.intent.action.PLUGIN_MANAGER";
    public static final String ACTION_SETTINGS = "os.omarchy.intent.action.PLUGIN_SETTINGS";

    public static final String PERMISSION_MANAGE = "os.omarchy.permission.MANAGE_PLUGINS";

    public static final String META_SCHEMA_VERSION = "os.omarchy.plugin.SCHEMA_VERSION";
    public static final String META_ID = "os.omarchy.plugin.ID";
    public static final String META_NAME = "os.omarchy.plugin.NAME";
    public static final String META_DESCRIPTION = "os.omarchy.plugin.DESCRIPTION";
    public static final String META_VERSION = "os.omarchy.plugin.VERSION";
    public static final String META_CAPABILITIES = "os.omarchy.plugin.CAPABILITIES";
    public static final String META_OVERLAYS = "os.omarchy.plugin.OVERLAYS";
    public static final String META_SEED_COLOR = "os.omarchy.plugin.SEED_COLOR";
    public static final String META_SEED_COLORS = "os.omarchy.plugin.SEED_COLORS";
    public static final String META_PREVIEW_COLORS = "os.omarchy.plugin.PREVIEW_COLORS";
    public static final String META_THEME_STYLE = "os.omarchy.plugin.THEME_STYLE";
    public static final String META_THEME_MODE = "os.omarchy.plugin.THEME_MODE";
    public static final String META_THEME_PACK = "os.omarchy.plugin.THEME_PACK";

    public static final String THEME_OVERLAY_TARGET = "os.omarchy.core";
    public static final String THEME_OVERLAY_CATEGORY = "os.omarchy.theme.core";

    public static final String EVENT_HOST_READY = "host-ready";
    public static final String EVENT_THEME_CHANGED = "theme-changed";
    public static final String EVENT_PLUGIN_ENABLED = "plugin-enabled";
    public static final String EXTRA_THEME_ID = "theme-id";
    public static final String EXTRA_THEME_MODE = "theme-mode";

    public static final String CAPABILITY_AGENT = "agent";
    public static final String CAPABILITY_THEME = "theme";
    public static final String CAPABILITY_SERVICE = "service";
    public static final String CAPABILITY_SETTINGS_PANEL = "settings-panel";
    public static final String CAPABILITY_SYSTEMUI_TILE = "systemui-tile";
    public static final String CAPABILITY_LAUNCHER_WIDGET = "launcher-widget";

    public static final String THEME_MODE_DARK = "dark";
    public static final String THEME_MODE_LIGHT = "light";
    public static final String THEME_MODE_SYSTEM = "system";

    private PluginContract() {}
}
