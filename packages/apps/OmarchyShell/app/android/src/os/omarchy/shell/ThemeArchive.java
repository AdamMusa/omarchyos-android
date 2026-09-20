package os.omarchy.shell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Reads data into memory by known names; archive paths are never used as disk destinations. */
public final class ThemeArchive {
    public static Map<String, byte[]> read(InputStream input, boolean registry) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        long total = 0, kept = 0;
        int entries = 0, images = 0;
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            byte[] buffer = new byte[16384];
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > 5000) throw new IOException("This theme archive has too many files.");
                String name = entry.getName();
                if (name.startsWith("/") || name.contains("\\") || name.contains("\u0000")) throw new IOException("Invalid theme archive path.");
                for (String segment : name.split("/")) if (segment.equals("..") || segment.equals(".")) throw new IOException("Invalid theme archive path.");
                int slash = name.indexOf('/');
                String relative = slash < 0 ? "" : name.substring(slash + 1);
                boolean isImage = relative.matches("backgrounds/[^/]+\\.(?i:png|jpe?g|webp|bmp)") || relative.matches("preview\\.(?i:png|jpe?g|webp)");
                boolean keep = registry ? relative.matches("themes/[a-z0-9_][a-z0-9._+-]*\\.json")
                    : relative.equals("colors.toml") || relative.equals("alacritty.toml") || relative.equals("light.mode")
                    || relative.matches("(?i:LICENSE(?:\\.txt|\\.md)?|COPYING|README\\.md)") || (isImage && images < 8);
                if (entry.isDirectory()) keep = false;
                ByteArrayOutputStream out = keep ? new ByteArrayOutputStream() : null;
                long length = 0;
                int count;
                while ((count = zip.read(buffer)) != -1) {
                    total += count; length += count;
                    if (total > 128L * 1024 * 1024) throw new IOException("This theme exceeds the phone download limit.");
                    if (keep) {
                        kept += count;
                        if (length > (isImage ? 24L * 1024 * 1024 : 131072) || kept > 48L * 1024 * 1024)
                            throw new IOException("Theme files exceed the phone size limit.");
                        out.write(buffer, 0, count);
                    }
                }
                if (keep) {
                    if (files.put(relative, out.toByteArray()) != null) throw new IOException("Duplicate theme file.");
                    if (isImage) images++;
                }
            }
        }
        return files;
    }
}
