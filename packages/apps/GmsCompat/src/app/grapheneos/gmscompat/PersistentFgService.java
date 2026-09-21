package app.grapheneos.gmscompat;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.ArrayMap;
import android.util.Log;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

// The purpose of this service is to raise GmsCompat app to foreground service level while GMS
// components are running. This allows to host long-living objects in GmsCompat app process and to
// temporarily raise GMS components and/or their clients to foreground level when that's required for
// starting a service, receiving a FCM notification, etc.
public class PersistentFgService extends Service {
    private static final String TAG = PersistentFgService.class.getSimpleName();

    public static CountDownLatch requestStart() {
        return command(CMD_START);
    }

    public static CountDownLatch release() {
        return command(CMD_MAYBE_STOP);
    }

    // access only from the main thread
    private static final ArrayMap<ParcelUuid, CountDownLatch> pendingCommands = new ArrayMap<>(7);

    private static CountDownLatch command(String cmd) {
        // otherwise CountDownLatch will deadlock
        UtilsKt.notMainThread();

        Context ctx = App.ctx();

        var latch = new CountDownLatch(1);

        // onStartCommand() is called on the main thread, see pendingCommands operations for why
        // this is important
        ctx.getMainThreadHandler().post(() -> {
            var i = new Intent(ctx, PersistentFgService.class);
            i.setAction(cmd);

            ParcelUuid commandId = new ParcelUuid(UUID.randomUUID());
            i.putExtra(EXTRA_COMMAND_ID, commandId);

            if (pendingCommands.put(commandId, latch) != null) {
                throw new IllegalStateException("duplicate commandId");
            }
            Log.d(TAG, "command " + cmd + ", id " + commandId);
            ctx.startForegroundService(i);
        });

        return latch;
    }

    private static final String CMD_START = "start";
    private static final String CMD_MAYBE_STOP = "maybe_stop";
    private static final String EXTRA_COMMAND_ID = "cmd_id";

    private int referenceCount;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String cmd = intent.getAction();
        ParcelUuid commandId = intent.getParcelableExtra(EXTRA_COMMAND_ID, ParcelUuid.class);
        int refCount = referenceCount;
        Log.d(TAG, "onStartCommand, cmd " + cmd + ", refCount " + refCount + ", id " + commandId);

        CountDownLatch latch = pendingCommands.remove(commandId);
        if (latch == null) {
            if (refCount != 0) {
                throw new IllegalStateException("invalid ref count: " + refCount + ", commandId " + commandId);
            }
            Log.d(TAG, "ignoring command from previous process instance");
            // OS requires that startForeground() is called after startForegroundService()
            startForeground(Notification.FOREGROUND_SERVICE_DEFERRED);
            stopSelf();
            return START_NOT_STICKY;
        }

        switch (cmd) {
            case CMD_START -> {
                ++refCount;
                if (refCount == 1) {
                    startForeground(Notification.FOREGROUND_SERVICE_IMMEDIATE);
                }
            }
            case CMD_MAYBE_STOP -> {
                --refCount;
                if (refCount == 0) {
                    stopSelf();
                }
            }
            default -> throw new IllegalStateException(cmd);
        }
        if (refCount < 0) {
            throw new IllegalStateException("invalid ref count: " + referenceCount);
        }
        referenceCount = refCount;
        latch.countDown();

        return START_NOT_STICKY;
    }

    private void startForeground(int notifBehavior) {
        Notification.Builder nb = Notifications.builder(Notifications.CH_PERSISTENT_FG_SERVICE);
        nb.setSmallIcon(android.R.drawable.ic_dialog_dialer);
        nb.setContentTitle(getText(R.string.persistent_fg_service_notif));
        nb.setContentIntent(PendingIntent.getActivity(this, 0,
            new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE));
        nb.setForegroundServiceBehavior(notifBehavior);
        nb.setGroup(Notifications.CH_PERSISTENT_FG_SERVICE);
        startForeground(Notifications.ID_PERSISTENT_FG_SERVICE, nb.build());
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new IllegalStateException(intent.toString());
    }
}
