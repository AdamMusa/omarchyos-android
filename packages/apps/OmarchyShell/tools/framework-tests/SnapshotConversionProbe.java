import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Exercises snapshot conversion with a GPU-only buffer on the actual driver. */
public final class SnapshotConversionProbe {
    private static Object snapshot(HardwareBuffer buffer) throws Exception {
        Class<?> type = Class.forName("android.window.TaskSnapshot");
        Constructor<?> constructor = type.getConstructor(long.class, long.class,
                ComponentName.class, HardwareBuffer.class, ColorSpace.class, int.class,
                int.class, Point.class, Rect.class, Rect.class, boolean.class,
                boolean.class, int.class, int.class, boolean.class, boolean.class,
                int.class, int.class);
        return constructor.newInstance(1L, 1L,
                new ComponentName("os.omarchy.uicheck", "SnapshotProbe"), buffer,
                ColorSpace.get(ColorSpace.Named.SRGB), 1, 0, new Point(64, 64),
                new Rect(), new Rect(), false, true, 1, 0, false, false, 0x21, 160);
    }

    public static void main(String[] args) throws Exception {
        Class<?> type = Class.forName("com.android.server.wm.TaskSnapshotConvertUtil");
        Class<?> snapshotType = Class.forName("android.window.TaskSnapshot");
        Method direct = type.getDeclaredMethod("copyToSwBitmapDirect",
                int.class, int.class, int.class, snapshotType);
        Method guarded = type.getDeclaredMethod("copySWBitmap", snapshotType);
        direct.setAccessible(true);
        guarded.setAccessible(true);
        long usage = HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE | HardwareBuffer.USAGE_GPU_COLOR_OUTPUT;
        boolean directFailed = false;
        try (HardwareBuffer buffer = HardwareBuffer.create(64, 64, HardwareBuffer.RGBA_8888, 1, usage)) {
            try {
                Bitmap result = (Bitmap) direct.invoke(null, 64, 64, HardwareBuffer.RGBA_8888,
                        snapshot(buffer));
                if (result != null) result.recycle();
            } catch (InvocationTargetException failure) {
                if (!(failure.getCause() instanceof RuntimeException)) throw failure;
                directFailed = true;
                System.out.println("Direct conversion rejected GPU-only buffer: " + failure.getCause());
            }
        }
        try (HardwareBuffer buffer = HardwareBuffer.create(64, 64, HardwareBuffer.RGBA_8888, 1, usage)) {
            Bitmap result = (Bitmap) guarded.invoke(null, snapshot(buffer));
            if (result != null) {
                if (result.getWidth() != 64 || result.getHeight() != 64) {
                    throw new AssertionError("Snapshot dimensions changed");
                }
                result.recycle();
            }
            System.out.println("PASS guarded snapshot conversion; direct failure exercised=" + directFailed);
        }
    }
}
