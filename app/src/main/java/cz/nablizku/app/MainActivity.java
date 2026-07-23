package cz.nablizku.app;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private LinearLayout root;
    private WebView webView;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestSmsRole();
        requestRuntimePermissions();
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
        Button messages = button("SMS ochrana");
        home.setOnClickListener(v -> showWeb());
        messages.setOnClickListener(v -> showMessages());
        navigation.addView(home, new LinearLayout.LayoutParams(0, 56, 1));
        navigation.addView(messages, new LinearLayout.LayoutParams(0, 56, 1));
        root.addView(navigation);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.loadUrl("https://nablizku-seniorum.czechoslovakiaserver.chatgpt.site/");
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(17);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.TRANSPARENT);
        return button;
    }

    private void showWeb() {
        root.removeViews(1, root.getChildCount() - 1);
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void showMessages() {
        root.removeViews(1, root.getChildCount() - 1);
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(18, 20, 18, 30);

        TextView title = text("Moje zprávy", 30, Color.rgb(23, 57, 92));
        title.setGravity(Gravity.START);
        list.addView(title);

        if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            list.addView(text("Pro zobrazení zpráv povolte přístup k SMS a nastavte Nablízku jako výchozí SMS aplikaci.", 18, Color.DKGRAY));
        } else {
            Cursor cursor = getContentResolver().query(
                Telephony.Sms.Inbox.CONTENT_URI,
                new String[]{Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE},
                null, null, Telephony.Sms.DATE + " DESC LIMIT 100"
            );
            if (cursor != null) {
                while (cursor.moveToNext()) {
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
                    card.setBackgroundColor(score >= 4 ? Color.rgb(255, 232, 226) : Color.WHITE);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                    params.setMargins(0, 12, 0, 0);
                    list.addView(card, params);
                }
                cursor.close();
            }
        }
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private TextView text(String value, int size, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        return text;
    }

    private void requestSmsRole() {
        RoleManager manager = getSystemService(RoleManager.class);
        if (manager != null && manager.isRoleAvailable(RoleManager.ROLE_SMS) && !manager.isRoleHeld(RoleManager.ROLE_SMS)) {
            startActivityForResult(manager.createRequestRoleIntent(RoleManager.ROLE_SMS), 42);
        }
    }

    private void requestRuntimePermissions() {
        requestPermissions(new String[]{
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.POST_NOTIFICATIONS
        }, 43);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
