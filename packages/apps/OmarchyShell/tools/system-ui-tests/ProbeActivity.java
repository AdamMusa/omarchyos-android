package os.omarchy.uicheck;
import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.media.AudioManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
public class ProbeActivity extends Activity {
 public void onCreate(Bundle state) {
  super.onCreate(state); showRequest();
 }
 public void onNewIntent(Intent intent) {
  super.onNewIntent(intent); setIntent(intent); showRequest();
 }
 private void showRequest() {
  TextView body=new TextView(this); body.setText("Omarchy system UI validation"); setContentView(body);
  String mode=getIntent().getStringExtra("mode");
  if ("notification".equals(mode)) {
   NotificationManager manager=getSystemService(NotificationManager.class);
   manager.createNotificationChannel(new NotificationChannel("check", "UI validation", NotificationManager.IMPORTANCE_DEFAULT));
   PendingIntent open=PendingIntent.getActivity(this,1,new Intent(this,ProbeActivity.class).setAction("os.omarchy.uicheck.OPEN").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("mode","opened"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   PendingIntent action=PendingIntent.getActivity(this,2,new Intent(this,ProbeActivity.class).setAction("os.omarchy.uicheck.ACTION").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("mode","action"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   manager.notify(1,new Notification.Builder(this,"check").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Omarchy notification").setContentText("Native notification content remains readable.").setStyle(new Notification.BigTextStyle().bigText("Native notification content remains readable. This is a local UI validation notification.")).setContentIntent(open).setAutoCancel(true).addAction(new Notification.Action.Builder(null,"Check action",action).build()).build());
  } else if ("opened".equals(mode) || "action".equals(mode)) {
   body.setText("Notification " + mode);
   android.util.Log.i("OmarchyUiCheck","notification result="+mode);
  } else if ("dialog".equals(mode)) {
   new AlertDialog.Builder(this).setTitle("Omarchy system dialog").setMessage("Native dialogs should follow the selected Omarchy palette and typography.").setNegativeButton("Cancel",(d,w)->finish()).setPositiveButton("Done",(d,w)->finish()).show();
  } else if ("volume".equals(mode)) {
   AudioManager audio=getSystemService(AudioManager.class);
   setVolumeControlStream(AudioManager.STREAM_MUSIC);
   LinearLayout layout=new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
   layout.setFitsSystemWindows(true);
   setContentView(layout);
   body.setText("Volume validation: opening the panel leaves media volume unchanged.");
   layout.addView(body,new LinearLayout.LayoutParams(-1,-2));
   Button show=new Button(this); show.setText("Show native volume controls");
   layout.addView(show,new LinearLayout.LayoutParams(-1,-2));
   show.setOnClickListener(v -> {
    int before=audio.getStreamVolume(AudioManager.STREAM_MUSIC);
    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_SAME,AudioManager.FLAG_SHOW_UI);
    android.util.Log.i("OmarchyUiCheck","volume before="+before+" after="+audio.getStreamVolume(AudioManager.STREAM_MUSIC));
   });
  } else requestPermissions(new String[]{"android.permission.RECORD_AUDIO"},17);
 }
 public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
  super.onRequestPermissionsResult(request,permissions,results);
  android.util.Log.i("OmarchyUiCheck","permission result="+java.util.Arrays.toString(results)); finish();
 }
}
