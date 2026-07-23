package cz.nablizku.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "nablizku_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");
        int id = intent.getIntExtra("notificationId", (int) System.currentTimeMillis());
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(new NotificationChannel(
                CHANNEL,
                "Připomínky léků a plánů",
                NotificationManager.IMPORTANCE_HIGH
            ));
        }
        android.app.Notification notification =
            new android.app.Notification.Builder(context, CHANNEL)
                .setSmallIcon(cz.nablizku.app.R.drawable.app_icon)
                .setContentTitle(title == null ? "Nablízku" : title)
                .setContentText(body == null ? "" : body)
                .setStyle(new android.app.Notification.BigTextStyle().bigText(body == null ? "" : body))
                .setAutoCancel(true)
                .build();
        manager.notify(id, notification);
    }
}
