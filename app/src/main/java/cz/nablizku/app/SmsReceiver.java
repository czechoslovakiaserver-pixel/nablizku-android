package cz.nablizku.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "sms-protection";

    @Override
    public void onReceive(Context context, Intent intent) {
        for (SmsMessage sms : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
            String sender = sms.getOriginatingAddress();
            String body = sms.getMessageBody();

            ContentValues values = new ContentValues();
            values.put(Telephony.Sms.ADDRESS, sender);
            values.put(Telephony.Sms.BODY, body);
            values.put(Telephony.Sms.DATE, System.currentTimeMillis());
            values.put(Telephony.Sms.READ, 0);
            values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX);
            context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, values);

            int score = ScamAnalyzer.score(body);
            if (score >= 2) showWarning(context, sender, body, score);
        }
    }

    private void showWarning(Context context, String sender, String body, int score) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(new NotificationChannel(
                CHANNEL, "Ochrana zpráv", NotificationManager.IMPORTANCE_HIGH
            ));
        }
        android.app.Notification notification = new android.app.Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(score >= 4 ? "Pozor, možný podvod" : "Podezřelá SMS")
            .setContentText((sender == null ? "Neznámé číslo" : sender) + ": " + body)
            .setStyle(new android.app.Notification.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build();
        manager.notify((sender + body).hashCode(), notification);
    }
}
