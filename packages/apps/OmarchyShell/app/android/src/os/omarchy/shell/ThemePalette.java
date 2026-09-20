package os.omarchy.shell;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The color-only portion of Omarchy's existing TOML format. No theme code is evaluated. */
public final class ThemePalette {
    private static final Pattern COLOR = Pattern.compile("^\\s*([a-zA-Z0-9_-]+)\\s*=\\s*[\\\"'](#[0-9a-fA-F]{6})[\\\"']\\s*(?:#.*)?$");
    private static final Pattern MODE = Pattern.compile("^\\s*mode\\s*=\\s*[\\\"'](light|dark)[\\\"']\\s*(?:#.*)?$");
    public final Map<String, String> colors = new LinkedHashMap<>();
    public String mode;

    public static ThemePalette parse(String text) throws IOException {
        if (text.length() > 131072) throw new IOException("Theme palette is too large.");
        ThemePalette palette = new ThemePalette();
        String section = "";
        for (String line : text.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[")) { section = trimmed; continue; }
            Matcher mode = MODE.matcher(line);
            if (section.isEmpty() && mode.matches()) palette.mode = mode.group(1);
            Matcher color = COLOR.matcher(line);
            if (!color.matches()) continue;
            String key = color.group(1), value = color.group(2).toLowerCase(java.util.Locale.ROOT);
            if (section.equals("[colors.bright]")) key = "bright_" + key;
            else if (!section.isEmpty() && !section.equals("[colors.primary]") && !section.equals("[colors.normal]")) continue;
            palette.colors.put(key, value);
        }
        palette.alias("background", "color0");
        palette.alias("foreground", "color7");
        palette.alias("accent", "color4");
        palette.alias("accent", "blue");
        String[] names = {"black", "red", "green", "yellow", "blue", "magenta", "cyan", "white"};
        for (int i = 0; i < names.length; i++) { palette.alias(names[i], "color" + i); palette.alias("bright_" + names[i], "color" + (i + 8)); }
        for (String key : new String[]{"background", "foreground", "accent"})
            if (!palette.colors.containsKey(key)) throw new IOException("Theme is missing a valid " + key + " color.");
        if (palette.mode == null) {
            int rgb = Integer.parseInt(palette.colors.get("background").substring(1), 16);
            palette.mode = (((rgb >> 16) & 255) * 299 + ((rgb >> 8) & 255) * 587 + (rgb & 255) * 114) > 150000 ? "light" : "dark";
        }
        return palette;
    }
    private void alias(String to, String from) {
        if (!colors.containsKey(to) && colors.containsKey(from)) colors.put(to, colors.get(from));
    }
    public String color(String key, String fallback) { return colors.getOrDefault(key, colors.get(fallback)); }
    public String toml() {
        StringBuilder out = new StringBuilder("mode = \"" + mode + "\"\n");
        colors.forEach((key,value) -> out.append(key).append(" = \"").append(value).append("\"\n"));
        return out.toString();
    }
}
