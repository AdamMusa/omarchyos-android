package os.omarchy.shell;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class ThemeDataTest {
    interface Checked { void run() throws Exception; }
    static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); }
    static void rejects(Checked action) throws Exception {
        try { action.run(); throw new AssertionError("Accepted invalid theme data"); }
        catch (IOException expected) { }
    }
    static byte[] zip(String[] names, byte[][] contents) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (int i=0;i<names.length;i++) { out.putNextEntry(new ZipEntry(names[i])); out.write(contents[i]); out.closeEntry(); }
        }
        return bytes.toByteArray();
    }
    public static void main(String[] args) throws Exception {
        int count = 0;
        try (var paths = Files.list(Path.of(args[0]))) {
            for (Path dir : paths.toList()) {
                if (!Files.isDirectory(dir)) continue;
                ThemePalette palette = ThemePalette.parse(Files.readString(dir.resolve("colors.toml")));
                check(ThemePalette.parse(palette.toml()).colors.equals(palette.colors), "Palette round trip: " + dir);
                Path wallpaper = Path.of(args[1], dir.getFileName() + ".jpg");
                check(Files.isRegularFile(wallpaper), "Missing default wallpaper: " + dir);
                var image = javax.imageio.ImageIO.read(wallpaper.toFile());
                check(image != null && image.getWidth() > 0 && image.getHeight() > 0,
                        "Wallpaper must decode: " + dir);
                count++;
            }
        }
        check(count == 22, "All 22 default themes");
        ThemePalette legacy = ThemePalette.parse("[colors.primary]\nbackground='#ffffff'\nforeground='#000000'\n[colors.normal]\nblue='#123456'\n[colors.bright]\nred='#ff0000'\n");
        check(legacy.mode.equals("light") && legacy.colors.get("accent").equals("#123456") && legacy.colors.get("bright_red").equals("#ff0000"), "Legacy palette mapping");
        rejects(() -> ThemePalette.parse("background='#nope00'\nforeground='#000000'\naccent='#ff0000'"));
        rejects(() -> ThemePalette.parse("background='#000000'"));
        byte[] safe = zip(new String[]{"theme/colors.toml", "theme/install.sh", "theme/evil.qml", "theme/backgrounds/a.png"}, new byte[][]{"test".getBytes(), "do not run".getBytes(), "do not run".getBytes(), new byte[]{1}});
        var kept = ThemeArchive.read(new ByteArrayInputStream(safe), false);
        check(kept.size()==2 && kept.containsKey("colors.toml") && !kept.containsKey("install.sh"), "Only theme data retained");
        for (String path : new String[]{"theme/../colors.toml", "/theme/colors.toml", "theme/./colors.toml", "theme\\colors.toml"}) {
            rejects(() -> ThemeArchive.read(new ByteArrayInputStream(zip(new String[]{path}, new byte[][]{{1}})), false));
        }
        rejects(() -> ThemeArchive.read(new ByteArrayInputStream(zip(new String[]{"a/colors.toml","b/colors.toml"}, new byte[][]{{1},{2}})), false));
        rejects(() -> ThemeArchive.read(new ByteArrayInputStream(zip(new String[]{"a/colors.toml"}, new byte[][]{new byte[131073]})), false));
        byte[] registry = zip(new String[]{"registry/themes/foo.json","registry/scripts/install.sh"}, new byte[][]{"{}".getBytes(),{1}});
        check(ThemeArchive.read(new ByteArrayInputStream(registry), true).size()==1,"Registry file allowlist");
        System.out.println("PASS: 22 palettes and wallpapers, legacy conversion, invalid colors, archive traversal, duplicates, limits and code exclusion");
    }
}
