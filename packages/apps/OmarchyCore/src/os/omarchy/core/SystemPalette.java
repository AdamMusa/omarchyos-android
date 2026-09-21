package os.omarchy.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** Map Omarchy's palette to Android semantic roles, preserving hue and readable text. */
final class SystemPalette {
    static Map<String, Integer> colors(int background, int foreground, int accent, int red) {
        int text = readable(foreground, background);
        int primary = readable(accent, background);
        int error = readable(red, background);
        Map<String, Integer> result = new LinkedHashMap<>();
        // Both resource variants must match the selected OS palette. SystemUI's
        // status bar can be asked for light icons by an app independently of night mode.
        for (String mode : new String[]{"dark", "light"}) {
            put(result, mode, background, "background", "surface", "surface_dim", "surface_container_lowest");
            put(result, mode, blend(background, text, .04), "surface_container_low");
            put(result, mode, blend(background, text, .07), "surface_container", "surface_bright");
            put(result, mode, blend(background, text, .10), "surface_container_high");
            put(result, mode, blend(background, text, .13), "surface_container_highest", "surface_variant");
            put(result, mode, text, "on_background", "on_surface", "on_surface_variant");
            put(result, mode, readable(blend(background, text, .55), background), "outline");
            put(result, mode, blend(background, text, .28), "outline_variant");
            for (String role : new String[]{"primary", "secondary", "tertiary", "error"}) {
                int color = role.equals("error") ? error : primary;
                int container = blend(background, color, .20);
                put(result, mode, color, role);
                put(result, mode, readable(background, color), "on_" + role);
                put(result, mode, container, role + "_container");
                put(result, mode, readable(text, container), "on_" + role + "_container");
            }
            put(result, mode, primary, "surface_tint");
            put(result, mode, text, "inverse_surface");
            put(result, mode, background, "inverse_on_surface");
            put(result, mode, readable(accent, text), "inverse_primary");
        }
        return result;
    }
    private static void put(Map<String, Integer> result, String mode, int color, String... roles) {
        for (String role : roles) result.put("system_" + role + "_" + mode, color);
    }
    static int readable(int color, int background) {
        if (contrast(color, background) >= 4.5) return color;
        int endpoint = contrast(0xff000000, background) > contrast(0xffffffff, background)
                ? 0xff000000 : 0xffffffff;
        // Keep the theme's hue, changing only the amount mixed toward black or
        // white. Return the passing side of the search after RGB quantization.
        double low = 0, high = 1;
        for (int step = 0; step < 16; step++) {
            double amount = (low + high) / 2;
            if (contrast(blend(color, endpoint, amount), background) >= 4.5) high = amount;
            else low = amount;
        }
        return blend(color, endpoint, high);
    }
    static double contrast(int a, int b) {
        double x = luminance(a), y = luminance(b);
        return (Math.max(x, y) + .05) / (Math.min(x, y) + .05);
    }
    private static double luminance(int c) {
        return channel(c >> 16 & 255) * .2126 + channel(c >> 8 & 255) * .7152 + channel(c & 255) * .0722;
    }
    private static double channel(int value) {
        double c = value / 255.0;
        return c <= .04045 ? c / 12.92 : Math.pow((c + .055) / 1.055, 2.4);
    }
    static int blend(int a, int b, double amount) {
        int result = 0xff000000;
        for (int shift : new int[]{16, 8, 0})
            result |= (int) Math.round((a >> shift & 255) * (1 - amount) + (b >> shift & 255) * amount) << shift;
        return result;
    }
}
