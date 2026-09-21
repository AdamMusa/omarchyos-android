import android.app.UiAutomation;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.HandlerThread;
import android.os.Looper;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import org.json.JSONArray;
import org.json.JSONObject;

public class NativeBarProbe {
  static JSONArray nodes = new JSONArray();
  static void visit(AccessibilityNodeInfo n) throws Exception {
    if (n == null) return;
    String pkg = String.valueOf(n.getPackageName());
    if (pkg.equals("com.android.systemui") && n.isVisibleToUser()) {
      Rect r = new Rect(); n.getBoundsInScreen(r);
      nodes.put(new JSONObject().put("text",String.valueOf(n.getText()))
        .put("description",String.valueOf(n.getContentDescription()))
        .put("class",String.valueOf(n.getClassName())).put("id",String.valueOf(n.getViewIdResourceName())).put("bounds",r.flattenToString()).put("clickable",n.isClickable()));
    }
    for (int i=0;i<n.getChildCount();i++) visit(n.getChild(i));
  }
  public static void main(String[] args) throws Exception {
    Looper.prepareMainLooper();
    HandlerThread thread = new HandlerThread("OmarchyNavProbe"); thread.start();
    Class<?> connection = Class.forName("android.app.IUiAutomationConnection");
    Object service = Class.forName("android.app.UiAutomationConnection").getConstructor().newInstance();
    UiAutomation ui = (UiAutomation)UiAutomation.class.getConstructor(Looper.class, connection)
      .newInstance(thread.getLooper(),service);
    try {
      UiAutomation.class.getMethod("connect").invoke(ui);
      AccessibilityServiceInfo info = ui.getServiceInfo();
      info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
      ui.setServiceInfo(info);
      Thread.sleep(300);
      for (AccessibilityWindowInfo window : ui.getWindows()) visit(window.getRoot());
      System.out.println(nodes.toString(2));
    } finally {
      UiAutomation.class.getMethod("disconnect").invoke(ui);
      thread.quitSafely();
    }
  }
}
