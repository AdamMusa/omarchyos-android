package os.omarchy.shell;

/** The upstream ttfx engine; every call must stay on the owning worker thread. */
final class OmarchyTextEffects implements AutoCloseable {
    static { System.loadLibrary("omarchy_dream"); }
    private long handle;
    OmarchyTextEffects(String text, int columns, int rows, long seed) {
        handle = nativeCreate(text, columns, rows, seed);
        if (handle == 0) throw new IllegalStateException("Could not create Omarchy animation");
    }
    String next() { return nativeNext(handle); }
    @Override public void close() {
        if (handle != 0) { nativeDestroy(handle); handle = 0; }
    }
    private static native long nativeCreate(String text, int columns, int rows, long seed);
    private static native String nativeNext(long handle);
    private static native void nativeDestroy(long handle);
}
