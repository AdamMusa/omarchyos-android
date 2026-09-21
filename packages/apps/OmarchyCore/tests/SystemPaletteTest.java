package os.omarchy.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

public final class SystemPaletteTest {
    static int color(String source, String key) {
        var match = Pattern.compile("(?m)^" + key + "\\s*=\\s*[\"'](#[0-9a-fA-F]{6})[\"']").matcher(source);
        if (!match.find()) throw new AssertionError("Missing " + key);
        return 0xff000000 | Integer.parseInt(match.group(1).substring(1), 16);
    }
    public static void main(String[] args) throws Exception {
        int count = 0;
        for (String name : args) {
            String source = Files.readString(Path.of(name));
            int bg = color(source, "background");
            Map<String, Integer> values = SystemPalette.colors(bg, color(source, "foreground"), color(source, "accent"), color(source, "red"));
            if (values.get("system_surface_dark") != bg) throw new AssertionError("Background changed");
            for (String mode : new String[]{"light", "dark"}) {
                for (String role : new String[]{"surface", "primary", "secondary", "tertiary", "error", "primary_container", "error_container"}) {
                    double ratio = SystemPalette.contrast(values.get("system_" + role + "_" + mode), values.get("system_on_" + role + "_" + mode));
                    if (ratio < 4.5) throw new AssertionError(name + " " + role + " " + ratio);
                }
            }
            count++;
        }
        if (SystemPalette.contrast(SystemPalette.readable(0xff777777, 0xff777777), 0xff777777) < 4.5)
            throw new AssertionError("Unusable marketplace contrast fallback");
        System.out.println("PASS: " + count + " theme palettes retain backgrounds and readable Android text roles");
    }
}
