package cz.nablizku.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.view.Gravity;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int SMS_PERMISSION_REQUEST = 43;
    private static final int CONTACTS_PERMISSION_REQUEST = 44;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 45;
    private static final String REMINDER_CHANNEL = "nablizku_reminders";
    private LinearLayout root;
    private WebView webView;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        showHome();
    }

    private void showHome() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(248, 250, 249));

        LinearLayout navigation = new LinearLayout(this);
        navigation.setPadding(12, 12, 12, 12);
        navigation.setBackgroundColor(Color.rgb(23, 57, 92));
        Button home = button("Nablízku");
        Button contacts = button("Kontakty");
        Button messages = button("SMS ochrana");
        home.setOnClickListener(v -> showWeb());
        contacts.setOnClickListener(v -> requestContacts());
        messages.setOnClickListener(v -> showMessages());
        navigation.addView(home, new LinearLayout.LayoutParams(0, 56, 1));
        navigation.addView(contacts, new LinearLayout.LayoutParams(0, 56, 1));
        navigation.addView(messages, new LinearLayout.LayoutParams(0, 56, 1));
        root.addView(navigation);

        try {
            webView = new WebView(this);
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            webView.addJavascriptInterface(new NativeBridge(), "NablizkuAndroid");
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageCommitVisible(WebView view, String url) {
                    super.onPageCommitVisible(view, url);
                    injectNativeCompatibility();
                }
            });
            webView.loadUrl("https://nablizku-seniorum.czechoslovakiaserver.chatgpt.site/");
            root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        } catch (RuntimeException error) {
            TextView message = text(
                "Webovou část aplikace se nepodařilo otevřít. Aktualizujte prosím Android System WebView.",
                19,
                Color.rgb(145, 53, 43)
            );
            message.setPadding(24, 30, 24, 30);
            root.addView(message, new LinearLayout.LayoutParams(-1, -2));
        }
        setContentView(root);
    }

    private Button button(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(17);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.TRANSPARENT);
        return button;
    }

    private void showWeb() {
        if (webView == null) return;
        if (root.getChildCount() > 1) root.removeViews(1, root.getChildCount() - 1);
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void showMessages() {
        if (root.getChildCount() > 1) root.removeViews(1, root.getChildCount() - 1);
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(18, 20, 18, 30);

        TextView title = text("Moje zprávy", 30, Color.rgb(23, 57, 92));
        title.setGravity(Gravity.START);
        list.addView(title);

        Button importSms = new Button(this);
        importSms.setText("Načíst SMS z telefonu");
        importSms.setTextSize(18);
        importSms.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                showMessages();
                Toast.makeText(this, "SMS byly znovu načteny a zkontrolovány.", Toast.LENGTH_SHORT).show();
            } else {
                requestRuntimePermissions();
            }
        });
        LinearLayout.LayoutParams importParams = new LinearLayout.LayoutParams(-1, -2);
        importParams.setMargins(0, 16, 0, 20);
        list.addView(importSms, importParams);

        if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            list.addView(text(
                "Pro zobrazení zpráv povolte přístup k SMS a nastavte Nablízku jako výchozí SMS aplikaci.",
                18,
                Color.DKGRAY
            ));
            Button enableSms = new Button(this);
            enableSms.setText("Povolit ochranu SMS");
            enableSms.setTextSize(18);
            enableSms.setOnClickListener(v -> requestRuntimePermissions());
            LinearLayout.LayoutParams enableParams = new LinearLayout.LayoutParams(-1, -2);
            enableParams.setMargins(0, 20, 0, 0);
            list.addView(enableSms, enableParams);
        } else {
            loadMessages(list);
        }
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void loadMessages(LinearLayout list) {
        int loaded = 0;
        try (Cursor cursor = getContentResolver().query(
            Telephony.Sms.Inbox.CONTENT_URI,
            new String[]{Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE},
            null,
            null,
            Telephony.Sms.DATE + " DESC LIMIT 5"
        )) {
            if (cursor == null) return;
            while (cursor.moveToNext()) {
                loaded++;
                String sender = cursor.getString(0);
                String body = cursor.getString(1);
                int score = ScamAnalyzer.score(body);
                TextView card = text(
                    (sender == null ? "Neznámé číslo" : sender) + "\n" +
                    ScamAnalyzer.label(body) + "\n\n" + body,
                    17,
                    score >= 4 ? Color.rgb(145, 53, 43) : Color.rgb(32, 48, 57)
                );
                card.setPadding(18, 18, 18, 18);
                card.setBackgroundColor(
                    score >= 4
                        ? Color.rgb(255, 232, 226)
                        : score >= 2 ? Color.rgb(255, 244, 214) : Color.WHITE
                );
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                params.setMargins(0, 12, 0, 0);
                list.addView(card, params);
            }
            if (loaded == 0) {
                list.addView(text(
                    "V telefonu nebyly nalezeny žádné přijaté SMS.",
                    18,
                    Color.DKGRAY
                ));
            }
        } catch (RuntimeException error) {
            list.addView(text("Zprávy se nepodařilo načíst.", 18, Color.rgb(145, 53, 43)));
        }
    }

    private int countRiskyMessages() {
        if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return 0;
        }
        int risky = 0;
        try (Cursor cursor = getContentResolver().query(
            Telephony.Sms.Inbox.CONTENT_URI,
            new String[]{Telephony.Sms.BODY},
            null,
            null,
            Telephony.Sms.DATE + " DESC LIMIT 5"
        )) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (ScamAnalyzer.score(cursor.getString(0)) >= 2) risky++;
                }
            }
        } catch (RuntimeException ignored) {
            return 0;
        }
        return risky;
    }

    private TextView text(String value, int size, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        return text;
    }

    private void requestSmsRole() {
        try {
            RoleManager manager = getSystemService(RoleManager.class);
            if (manager != null
                && manager.isRoleAvailable(RoleManager.ROLE_SMS)
                && !manager.isRoleHeld(RoleManager.ROLE_SMS)) {
                startActivityForResult(manager.createRequestRoleIntent(RoleManager.ROLE_SMS), 42);
            }
        } catch (RuntimeException error) {
            Toast.makeText(this, "Ochranu SMS se nepodařilo zapnout na tomto telefonu.", Toast.LENGTH_LONG).show();
        }
    }

    private void requestRuntimePermissions() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(new String[]{
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.POST_NOTIFICATIONS
                }, SMS_PERMISSION_REQUEST);
            } else {
                requestPermissions(new String[]{
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.SEND_SMS
                }, SMS_PERMISSION_REQUEST);
            }
        } catch (RuntimeException error) {
            Toast.makeText(this, "Oprávnění se nepodařilo otevřít.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST) {
            showMessages();
            requestSmsRole();
        }
        if (requestCode == CONTACTS_PERMISSION_REQUEST
            && grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            sendContactsToWeb();
        }
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST
            && grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showNativeNotification("Nablízku", "Upozornění jsou zapnutá.", 1001);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 42) showMessages();
    }

    private void requestContacts() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            sendContactsToWeb();
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, CONTACTS_PERMISSION_REQUEST);
        }
    }

    private void sendContactsToWeb() {
        JSONArray contacts = new JSONArray();
        try (Cursor cursor = getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            },
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    JSONObject contact = new JSONObject();
                    contact.put("name", cursor.getString(0));
                    contact.put("phone", cursor.getString(1));
                    contacts.put(contact);
                }
            }
        } catch (Exception error) {
            Toast.makeText(this, "Kontakty se nepodařilo načíst.", Toast.LENGTH_LONG).show();
        }
        final String payload = JSONObject.quote(contacts.toString());
        webView.evaluateJavascript(
            "localStorage.setItem('nablizku-contacts'," + payload + ");" +
            "window.dispatchEvent(new CustomEvent('nablizku-contacts',{detail:JSON.parse(" + payload + ")}));" +
            "location.reload();",
            null
        );
    }

    private void enableNativeNotifications() {
        createReminderChannel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                NOTIFICATION_PERMISSION_REQUEST
            );
        } else {
            showNativeNotification("Nablízku", "Upozornění jsou zapnutá.", 1001);
        }
    }

    private void createReminderChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(new NotificationChannel(
                REMINDER_CHANNEL,
                "Připomínky léků a plánů",
                NotificationManager.IMPORTANCE_HIGH
            ));
        }
    }

    private void showNativeNotification(String title, String body, int id) {
        createReminderChannel();
        NotificationManager manager = getSystemService(NotificationManager.class);
        android.app.Notification notification =
            new android.app.Notification.Builder(this, REMINDER_CHANNEL)
                .setSmallIcon(cz.nablizku.app.R.drawable.app_icon)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new android.app.Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .build();
        manager.notify(id, notification);
    }

    private void scheduleNativeReminders(String json) {
        try {
            JSONArray reminders = new JSONArray(json);
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            for (int index = 0; index < reminders.length(); index++) {
                JSONObject reminder = reminders.getJSONObject(index);
                long timestamp = reminder.getLong("timestamp");
                if (timestamp <= System.currentTimeMillis()) continue;
                int id = reminder.getString("id").hashCode();
                Intent intent = new Intent(this, ReminderReceiver.class);
                intent.putExtra("title", reminder.getString("title"));
                intent.putExtra("body", reminder.getString("body"));
                intent.putExtra("notificationId", id);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    id,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp, pendingIntent);
            }
        } catch (Exception error) {
            Toast.makeText(this, "Připomínky se nepodařilo naplánovat.", Toast.LENGTH_LONG).show();
        }
    }

    private void scheduleSingleReminder(long timestamp, String title, String body, String id) {
        try {
            JSONArray reminders = new JSONArray();
            JSONObject reminder = new JSONObject();
            reminder.put("timestamp", timestamp);
            reminder.put("title", title);
            reminder.put("body", body);
            reminder.put("id", id);
            reminders.put(reminder);
            scheduleNativeReminders(reminders.toString());
        } catch (Exception error) {
            Toast.makeText(this, "Připomínku se nepodařilo naplánovat.", Toast.LENGTH_LONG).show();
        }
    }

    private void injectNativeCompatibility() {
        String script =
            "(function(){" +
            "if(window.__nablizkuNativeReady)return;window.__nablizkuNativeReady=true;" +
            "var pendingTimes=[];var originalTimeout=window.setTimeout.bind(window);" +
            "window.setTimeout=function(fn,delay){" +
                "var args=Array.prototype.slice.call(arguments,2);" +
                "if(Number(delay)>5000){" +
                    "pendingTimes.push(Date.now()+Number(delay));" +
                    "return originalTimeout(function(){fn.apply(window,args);},0);" +
                "}" +
                "return originalTimeout(function(){fn.apply(window,args);},delay);" +
            "};" +
            "var registration={showNotification:function(title,options){" +
                "var at=pendingTimes.length?pendingTimes.shift():Date.now();" +
                "var body=options&&options.body?String(options.body):'';" +
                "var id=options&&options.tag?String(options.tag):String(Date.now());" +
                "if(at>Date.now()+1000)NablizkuAndroid.scheduleReminder(at,String(title),body,id);" +
                "else NablizkuAndroid.showNotification(String(title),body);" +
                "return Promise.resolve();" +
            "}};" +
            "try{Object.defineProperty(navigator,'serviceWorker',{configurable:true,value:{" +
                "register:function(){return Promise.resolve(registration);}," +
                "ready:Promise.resolve(registration)" +
            "}});}catch(e){}" +
            "try{Object.defineProperty(navigator,'contacts',{configurable:true,value:{" +
                "select:function(){return new Promise(function(resolve,reject){" +
                    "var handler=function(event){" +
                        "window.removeEventListener('nablizku-contacts',handler);" +
                        "var data=event.detail||[];" +
                        "resolve(data.map(function(item){return{name:[item.name],tel:[item.phone]};}));" +
                    "};" +
                    "window.addEventListener('nablizku-contacts',handler);" +
                    "NablizkuAndroid.importContacts();" +
                "});}" +
            "}});}catch(e){}" +
            "try{window.Notification=function(){};" +
                "Object.defineProperty(window.Notification,'permission',{value:'granted'});" +
                "window.Notification.requestPermission=function(){NablizkuAndroid.enableNotifications();return Promise.resolve('granted');};" +
            "}catch(e){}" +
            "function patchSmsTile(){" +
                "var buttons=document.querySelectorAll('button.tile.mint');" +
                "for(var i=0;i<buttons.length;i++){" +
                    "var strong=buttons[i].querySelector('strong');" +
                    "if(!strong||strong.textContent.indexOf('Moje zpr')<0)continue;" +
                    "buttons[i].setAttribute('data-native-sms','true');" +
                    "var count=Number(NablizkuAndroid.getRiskySmsCount());" +
                    "var label=buttons[i].querySelector('span');" +
                    "if(label)label.textContent=count===1?'1 riziková zpráva':count+' rizikových zpráv';" +
                "}" +
            "}" +
            "document.addEventListener('click',function(event){" +
                "var target=event.target&&event.target.closest?event.target.closest('button[data-native-sms=true]'):null;" +
                "if(!target)return;" +
                "event.preventDefault();event.stopPropagation();event.stopImmediatePropagation();" +
                "NablizkuAndroid.openSmsProtection();" +
            "},true);" +
            "new MutationObserver(patchSmsTile).observe(document.documentElement,{childList:true,subtree:true});" +
            "patchSmsTile();" +
            "window.dispatchEvent(new Event('nablizku-native-ready'));" +
            "})();";
        webView.evaluateJavascript(script, null);
    }

    private final class NativeBridge {
        @JavascriptInterface
        public void openSmsProtection() {
            runOnUiThread(() -> showMessages());
        }

        @JavascriptInterface
        public int getRiskySmsCount() {
            return countRiskyMessages();
        }

        @JavascriptInterface
        public void importContacts() {
            runOnUiThread(() -> requestContacts());
        }

        @JavascriptInterface
        public void enableNotifications() {
            runOnUiThread(() -> enableNativeNotifications());
        }

        @JavascriptInterface
        public void sendTestNotification() {
            runOnUiThread(() -> showNativeNotification(
                "Test připomínky léku",
                "Oznámení funguje správně.",
                (int) (System.currentTimeMillis() & 0x7fffffff)
            ));
        }

        @JavascriptInterface
        public void scheduleReminders(String json) {
            runOnUiThread(() -> scheduleNativeReminders(json));
        }

        @JavascriptInterface
        public void scheduleReminder(long timestamp, String title, String body, String id) {
            runOnUiThread(() -> scheduleSingleReminder(timestamp, title, body, id));
        }

        @JavascriptInterface
        public void showNotification(String title, String body) {
            runOnUiThread(() -> showNativeNotification(
                title,
                body,
                (int) (System.currentTimeMillis() & 0x7fffffff)
            ));
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
