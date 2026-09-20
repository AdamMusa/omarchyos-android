package os.omarchy.shell;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Feeds Android notifications to Omarchy's notification plugin, which renders
 * the popups and the history exactly as it does on the desktop.
 */
public class NotificationBridge extends NotificationListenerService {

    private static NotificationBridge sInstance;

    @Override public void onListenerConnected() { sInstance = this; }
    @Override public void onListenerDisconnected() { sInstance = null; }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        ShellBridge.get();
        notifyChanged();
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) { notifyChanged(); }

    private void notifyChanged() {
        try {
            ShellBridge.class.getDeclaredMethod("get").invoke(null);
        } catch (Exception ignored) { }
    }

    static String activeJson() {
        JSONArray out = new JSONArray();
        NotificationBridge service = sInstance;
        if (service == null) return out.toString();
        try {
            StatusBarNotification[] active = service.getActiveNotifications();
            if (active == null) return out.toString();
            for (StatusBarNotification sbn : active) {
                JSONObject o = new JSONObject();
                o.put("key", sbn.getKey());
                o.put("appName", sbn.getPackageName());
                CharSequence title = sbn.getNotification().extras.getCharSequence("android.title");
                CharSequence text = sbn.getNotification().extras.getCharSequence("android.text");
                o.put("summary", title == null ? "" : title.toString());
                o.put("body", text == null ? "" : text.toString());
                o.put("urgency", 1);
                out.put(o);
            }
        } catch (Exception ignored) { }
        return out.toString();
    }

    static void dismiss(String key) {
        NotificationBridge service = sInstance;
        if (service != null) service.cancelNotification(key);
    }
}
