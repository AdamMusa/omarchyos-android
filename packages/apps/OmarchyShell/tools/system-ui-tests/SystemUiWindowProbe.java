import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.UiAutomation;
import android.graphics.Rect;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import org.json.JSONArray;
import org.json.JSONObject;

/** Run through app_process on a development device, never inside the product. */
public class SystemUiWindowProbe {
    private static final JSONArray nodes = new JSONArray();

    private static void visit(AccessibilityNodeInfo node) throws Exception {
        if (node == null) return;
        if ("com.android.systemui".contentEquals(String.valueOf(node.getPackageName()))
                && node.isVisibleToUser()) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            JSONObject item = new JSONObject()
                    .put("text", String.valueOf(node.getText()))
                    .put("description", String.valueOf(node.getContentDescription()))
                    .put("class", String.valueOf(node.getClassName()))
                    .put("id", String.valueOf(node.getViewIdResourceName()))
                    .put("bounds", bounds.flattenToString())
                    .put("clickable", node.isClickable());
            AccessibilityNodeInfo.RangeInfo range = node.getRangeInfo();
            if (range != null) {
                item.put("range", new JSONObject().put("min", range.getMin())
                        .put("max", range.getMax()).put("current", range.getCurrent()));
            }
            nodes.put(item);
        }
        for (int i = 0; i < node.getChildCount(); i++) visit(node.getChild(i));
    }

    public static void main(String[] args) throws Exception {
        Looper.prepareMainLooper();
        HandlerThread thread = new HandlerThread("OmarchySystemUiProbe");
        thread.start();
        Class<?> connection = Class.forName("android.app.IUiAutomationConnection");
        Object service = Class.forName("android.app.UiAutomationConnection")
                .getConstructor().newInstance();
        UiAutomation ui = (UiAutomation) UiAutomation.class
                .getConstructor(Looper.class, connection).newInstance(thread.getLooper(), service);
        try {
            UiAutomation.class.getMethod("connect").invoke(ui);
            AccessibilityServiceInfo info = ui.getServiceInfo();
            info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                    | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            ui.setServiceInfo(info);
            Thread.sleep(300);
            for (AccessibilityWindowInfo window : ui.getWindows()) visit(window.getRoot());
            System.out.println(nodes.toString());
        } finally {
            UiAutomation.class.getMethod("disconnect").invoke(ui);
            thread.quitSafely();
        }
    }
}
