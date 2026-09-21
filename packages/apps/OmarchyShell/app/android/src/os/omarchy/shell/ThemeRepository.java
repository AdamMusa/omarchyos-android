package os.omarchy.shell;

import android.content.Context;
import android.content.Intent;
import android.app.WallpaperManager;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.ResultReceiver;
import android.system.Os;
import android.util.AtomicFile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Phone theme catalog. Omarchy TOML and artwork are data; repository code is never installed. */
public final class ThemeRepository {
    private static ThemeRepository instance;
    private static final String REGISTRY = "https://codeload.github.com/omacom/omarchy-theme-registry/zip/refs/heads/master";
    private static final String ID = "[a-z0-9_][a-z0-9._+-]{0,79}";
    private final Context context;
    private final File home, bundled, extra, current;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean();
    private volatile String state = "{}";
    private JSONArray market = new JSONArray();
    private String message = "", error = "";

    public static synchronized ThemeRepository get(Context context) {
        if (instance == null) instance = new ThemeRepository(context.getApplicationContext());
        return instance;
    }
    private ThemeRepository(Context context) {
        this.context = context;
        home = context.getFilesDir();
        File system = new File("/product/etc/omarchy");
        String configured = System.getenv("OMARCHY_PATH");
        File tree = configured != null ? new File(configured) : new File(system, "shell/shell.qml").isFile() ? system : new File(home,"omarchy");
        bundled = new File(tree, "themes");
        extra = new File(home, ".config/omarchy/themes");
        current = new File(home, ".local/state/omarchy/current");
        extra.mkdirs(); current.mkdirs();
        worker.execute(() -> {
            try {
                File cache = new File(home, ".config/omarchy/marketplace.json");
                File snapshot = new File(bundled.getParentFile(), "mobile-themes/marketplace.json");
                market = new JSONArray(read(cache.isFile() ? cache : snapshot));
            } catch (Exception ignored) { }
            try {
                File image = wallpaper(activeDirectory());
                if (image.isFile()) link(new File(current, "background"), image);
                else Files.deleteIfExists(new File(current, "background").toPath());
            } catch (Exception ignored) { }
            publish();
            // Migrate the native wallpaper once; never reset a user's separate
            // wallpaper choice merely because Home was restarted.
            try { applyWallpaper(activeDirectory(), false); }
            catch (Exception e) { android.util.Log.w("OmarchyThemes", "Native theme wallpaper unavailable", e); }
        });
    }
    public String state() { return state; }
    private static String read(File file) throws IOException { return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8); }
    private static void write(File file, String text) throws IOException {
        file.getParentFile().mkdirs();
        AtomicFile atomic = new AtomicFile(file);
        FileOutputStream out = null;
        try { out = atomic.startWrite(); out.write(text.getBytes(StandardCharsets.UTF_8)); atomic.finishWrite(out); }
        catch (IOException e) { if (out != null) atomic.failWrite(out); throw e; }
    }
    private File activeDirectory() throws IOException { return new File(current, "theme").getCanonicalFile(); }
    private String activeId() {
        try { return activeDirectory().getName(); } catch (IOException e) { return "tokyo-night"; }
    }
    private File directory(String id) throws IOException {
        if (id == null || !id.matches(ID)) throw new IOException("Invalid theme name.");
        File original = new File(bundled, id);
        File file = original.isDirectory() ? original : new File(extra, id);
        if (!file.isDirectory() || !new File(file, "colors.toml").isFile()) throw new IOException("Theme is not installed.");
        return file;
    }
    private File wallpaper(File dir) {
        File file = new File(dir, "background.png");
        if (!file.isFile()) file = new File(bundled.getParentFile(), "mobile-themes/wallpapers/" + dir.getName() + ".jpg");
        return file;
    }
    /** Runs on the theme worker. Android owns cropping, storage and lock security. */
    private void applyWallpaper(File dir, boolean explicitApply) throws Exception {
        File image = wallpaper(dir);
        String background = ThemePalette.parse(read(new File(dir, "colors.toml"))).colors.get("background");
        String revision = dir.getCanonicalPath() + ":" + image.lastModified() + ":" + image.length() + ":" + background;
        android.content.SharedPreferences preferences = context.getSharedPreferences("native-theme-wallpaper", Context.MODE_PRIVATE);
        if (!explicitApply && revision.equals(preferences.getString("revision", ""))) return;
        WallpaperManager manager = WallpaperManager.getInstance(context);
        if (!manager.isWallpaperSupported() || !manager.isSetWallpaperAllowed()) {
            throw new IOException("Wallpaper changes are unavailable for this user.");
        }
        int flags = WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK;
        if (image.isFile()) {
            try (InputStream stream = new FileInputStream(image)) {
                manager.setStream(stream, null, false, flags);
            }
        } else {
            // A palette-only community theme must not retain another theme's art.
            Bitmap solid = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
            try {
                solid.eraseColor(Color.parseColor(background));
                manager.setBitmap(solid, null, false, flags);
            } finally { solid.recycle(); }
        }
        preferences.edit().putString("revision", revision).apply();
    }
    private JSONObject describe(File dir, boolean builtIn) throws Exception {
        ThemePalette p = ThemePalette.parse(read(new File(dir, "colors.toml")));
        JSONObject meta = new JSONObject();
        File metadata = new File(dir, "theme.json");
        if (metadata.isFile()) meta = new JSONObject(read(metadata));
        StringBuilder title = new StringBuilder();
        for (String word : dir.getName().split("-")) {
            if (word.isEmpty()) continue;
            if (title.length() > 0) title.append(' ');
            title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        JSONObject item = new JSONObject();
        item.put("id", dir.getName()).put("name", meta.optString("name", title.toString()))
            .put("mode", p.mode).put("accent", p.colors.get("accent"))
            .put("background", p.colors.get("background")).put("foreground", p.colors.get("foreground"))
            .put("builtIn", builtIn).put("repo", meta.optString("repo", ""));
        File preview = wallpaper(dir);
        item.put("hasWallpaper", preview.isFile());
        item.put("preview", preview.isFile() ? preview.toURI().toString() : "");
        return item;
    }
    private void publish() {
        try {
            JSONArray installed = new JSONArray();
            for (File parent : new File[]{bundled, extra}) {
                File[] dirs = parent.listFiles(File::isDirectory);
                if (dirs == null) continue;
                Arrays.sort(dirs, Comparator.comparing(File::getName));
                for (File dir : dirs) {
                    if (!dir.getName().matches(ID)) continue;
                    try { installed.put(describe(dir, parent.equals(bundled))); }
                    catch (Exception ignored) { }
                }
            }
            JSONObject snapshot = new JSONObject().put("installed", installed).put("marketplace", market)
                .put("active", activeId()).put("busy", busy.get()).put("message", message).put("error", error);
            try {
                File active = activeDirectory();
                snapshot.put("palette", ThemePalette.parse(read(new File(active, "colors.toml"))).toml());
                File image = wallpaper(active);
                snapshot.put("wallpaper", image.isFile() ? image.toURI().toString() : "");
            } catch (Exception ignored) { }
            state = snapshot.toString();
            ShellBridge.themesChanged();
        } catch (Exception ignored) { }
    }
    private boolean start(String status) {
        if (!busy.compareAndSet(false, true)) return false;
        worker.execute(() -> { message = status; error = ""; publish(); });
        return true;
    }
    private void finish(Exception failure, String success) {
        error = failure == null ? "" : (failure.getMessage() == null ? "Theme operation failed. Please try again." : failure.getMessage());
        message = failure == null ? success : "";
        busy.set(false); publish();
    }
    public void refreshMarket() {
        if (!start("Refreshing Omarchy marketplace…")) return;
        worker.execute(() -> {
            try {
                Map<String, byte[]> files = ThemeArchive.read(download(REGISTRY), true);
                List<JSONObject> entries = new ArrayList<>();
                for (byte[] bytes : files.values()) {
                    JSONObject entry = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
                    if (!entry.optString("slug").matches(ID) || !validRepo(entry.optString("repo"))) continue;
                    if (new File(bundled, entry.getString("slug")).isDirectory()) continue;
                    entries.add(entry);
                }
                entries.sort(Comparator.comparing(e -> e.optString("name").toLowerCase(Locale.ROOT)));
                if (entries.isEmpty()) throw new IOException("The marketplace returned no themes. Your saved catalog is still available.");
                JSONArray updated = new JSONArray(); for (JSONObject entry : entries) updated.put(entry);
                write(new File(home, ".config/omarchy/marketplace.json"), updated.toString());
                market = updated;
                finish(null, entries.size() + " community themes available.");
            } catch (Exception e) { finish(e, ""); }
        });
    }
    private static boolean validRepo(String repo) { return repo.matches("https://github\\.com/[A-Za-z0-9-]{1,39}/[A-Za-z0-9_.-]{1,100}"); }
    public void install(String slug) {
        if (!start("Downloading theme…")) return;
        worker.execute(() -> {
            File stage = null;
            try {
                JSONObject entry = null;
                for (int i=0; i<market.length(); i++) if (market.getJSONObject(i).optString("slug").equals(slug)) entry=market.getJSONObject(i);
                if (entry == null || !slug.matches(ID) || new File(bundled, slug).exists()) throw new IOException("Select a community theme from the marketplace.");
                File target = new File(extra, slug);
                if (target.exists()) throw new IOException("This theme is already installed. Choose Apply in Installed themes.");
                String repo = entry.getString("repo");
                if (!validRepo(repo)) throw new IOException("Unsupported theme repository.");
                String path = repo.substring("https://github.com/".length());
                String sha;
                try (InputStream in = download("https://api.github.com/repos/" + path + "/commits/HEAD")) {
                    sha = new JSONObject(new String(readLimited(in, 1024 * 1024), StandardCharsets.UTF_8)).getString("sha");
                }
                if (!sha.matches("[0-9a-f]{40}")) throw new IOException("Unable to identify the theme revision.");
                Map<String, byte[]> files = ThemeArchive.read(download("https://codeload.github.com/" + path + "/zip/" + sha), false);
                byte[] colors = files.get("colors.toml");
                if (colors == null) colors = files.get("alacritty.toml");
                if (colors == null) throw new IOException("This theme does not include an Omarchy palette.");
                ThemePalette palette = ThemePalette.parse(new String(colors, StandardCharsets.UTF_8));
                if (files.containsKey("light.mode")) palette.mode = "light";
                stage = Files.createTempDirectory(extra.toPath(), ".install-").toFile();
                write(new File(stage,"colors.toml"),palette.toml());
                boolean hasBackground = false;
                List<String> names = new ArrayList<>(files.keySet()); Collections.sort(names);
                for (String name : names) {
                    if (name.startsWith("backgrounds/") && !hasBackground) {
                        try { saveImage(files.get(name), new File(stage,"background.png")); hasBackground=true; }
                        catch (IOException ignored) { }
                    } else if (name.startsWith("preview.")) {
                        try { saveImage(files.get(name), new File(stage,"preview.png")); } catch (IOException ignored) { }
                    } else if (name.matches("(?i:LICENSE(?:\\.txt|\\.md)?|COPYING|README\\.md)")) {
                        write(new File(stage,name),new String(files.get(name),StandardCharsets.UTF_8));
                    }
                }
                write(new File(stage,"theme.json"),new JSONObject().put("name",entry.getString("name")).put("repo",repo).put("commit",sha).toString());
                Os.rename(stage.getAbsolutePath(),target.getAbsolutePath()); stage=null;
                finish(null, entry.getString("name") + " installed. Tap Apply to use it.");
            } catch (Exception e) { finish(e, ""); }
            finally { if (stage != null) delete(stage); }
        });
    }
    public void apply(String id) {
        if (!start("Applying theme…")) return;
        worker.execute(() -> {
            ThemePalette previous = null;
            String previousId = activeId();
            boolean systemApplied = false;
            try {
                File dir=directory(id);
                ThemePalette palette=ThemePalette.parse(read(new File(dir,"colors.toml")));
                try { previous=ThemePalette.parse(read(new File(activeDirectory(),"colors.toml"))); } catch(Exception ignored) { }
                if (!applySystem(id,palette)) throw new IOException("Android could not apply this theme. The previous theme was kept.");
                systemApplied=true;
                link(new File(current,"theme"),dir);
                File background=wallpaper(dir);
                if (background.isFile()) link(new File(current,"background"),background);
                else Files.deleteIfExists(new File(current,"background").toPath());
                // Palette and shell are already committed. Report an artwork
                // failure separately rather than pretending the palette rolled back.
                try {
                    applyWallpaper(dir, true);
                    finish(null,"Theme applied.");
                } catch (Exception wallpaperFailure) {
                    finish(new IOException("Theme applied, but Android could not update the wallpaper. Apply again to retry.", wallpaperFailure), "");
                }
            } catch(Exception e) {
                if (systemApplied && previous != null) {
                    try { applySystem(previousId,previous); File dir=directory(previousId); link(new File(current,"theme"),dir); if(wallpaper(dir).isFile()) link(new File(current,"background"),wallpaper(dir)); }
                    catch(Exception ignored) { }
                }
                finish(e, "");
            }
        });
    }
    private boolean applySystem(String id,ThemePalette palette) throws Exception {
        CountDownLatch done=new CountDownLatch(1); AtomicBoolean success=new AtomicBoolean();
        Bundle colors=new Bundle(); palette.colors.forEach(colors::putString);
        ResultReceiver result=new ResultReceiver(new Handler(Looper.getMainLooper())) {
            @Override protected void onReceiveResult(int code,Bundle data) { success.set(code==1); done.countDown(); }
        };
        // Marshal as the framework type, not our anonymous subclass, across processes.
        Parcel parcel = Parcel.obtain();
        ResultReceiver remote;
        try { result.writeToParcel(parcel, 0); parcel.setDataPosition(0); remote = ResultReceiver.CREATOR.createFromParcel(parcel); }
        finally { parcel.recycle(); }
        Intent intent=new Intent("os.omarchy.intent.action.APPLY_PALETTE")
            .setClassName("os.omarchy.core","os.omarchy.core.OmarchyPluginManagerService")
            .putExtra("theme_id",id).putExtra("mode",palette.mode).putExtra("palette",colors).putExtra("result",remote);
        context.startService(intent);
        if (!done.await(20,TimeUnit.SECONDS)) throw new IOException("Android theme service did not respond. Please try again.");
        return success.get();
    }
    private static void link(File link,File target) throws Exception {
        File next=new File(link.getParentFile(),link.getName()+".next");
        Files.deleteIfExists(next.toPath());
        Os.symlink(target.getAbsolutePath(),next.getAbsolutePath());
        Os.rename(next.getAbsolutePath(),link.getAbsolutePath());
    }
    private static void saveImage(byte[] bytes,File destination) throws IOException {
        BitmapFactory.Options opts=new BitmapFactory.Options(); opts.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);
        if (opts.outWidth<=0 || opts.outHeight<=0 || (long)opts.outWidth*opts.outHeight>40000000L) throw new IOException("Unsupported or oversized theme image.");
        opts.inSampleSize=1;
        while (Math.max(opts.outWidth,opts.outHeight)/opts.inSampleSize>2048) opts.inSampleSize*=2;
        opts.inJustDecodeBounds=false;
        Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);
        if (bitmap==null) throw new IOException("Theme image could not be decoded.");
        try(FileOutputStream out=new FileOutputStream(destination)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG,100,out)) throw new IOException("Theme image could not be saved.");
        } finally { bitmap.recycle(); }
    }
    private static InputStream download(String address) throws IOException {
        URL url=new URL(address);
        if (!url.getProtocol().equals("https") || !Arrays.asList("api.github.com","codeload.github.com").contains(url.getHost())) throw new IOException("Invalid theme download address.");
        HttpURLConnection connection=(HttpURLConnection)url.openConnection();
        connection.setConnectTimeout(15000); connection.setReadTimeout(20000); connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent","OmarchyOS-Android-Themes");
        int code=connection.getResponseCode();
        if (code!=200) { connection.disconnect(); throw new IOException(code==403 || code==429 ? "GitHub download limit reached. Try again later." : "Theme download failed ("+code+"). Your installed themes are unchanged."); }
        if (connection.getContentLengthLong()>64L*1024*1024) { connection.disconnect(); throw new IOException("Theme download is too large for the phone."); }
        return new FilterInputStream(connection.getInputStream()) {
            long readBytes;
            @Override public int read() throws IOException {
                int result = in.read();
                if (result >= 0 && ++readBytes > 64L*1024*1024) throw new IOException("Theme download exceeds the size limit.");
                return result;
            }
            @Override public int read(byte[] bytes,int offset,int count) throws IOException {
                int result=super.read(bytes,offset,count);
                if(result>0 && (readBytes+=result)>64L*1024*1024) throw new IOException("Theme download exceeds the size limit.");
                return result;
            }
            @Override public void close() throws IOException { try { super.close(); } finally { connection.disconnect(); } }
        };
    }
    private static byte[] readLimited(InputStream in,int maximum) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buffer=new byte[8192]; int count;
        while ((count=in.read(buffer))!=-1) { if(out.size()+count>maximum) throw new IOException("Theme metadata is too large."); out.write(buffer,0,count); }
        return out.toByteArray();
    }
    private static void delete(File file) { File[] children=file.listFiles(); if(children!=null)for(File child:children)delete(child); file.delete(); }
}
