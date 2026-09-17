package os.omarchy.agent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public final class AgentActivity extends Activity {
    private LinearLayout mMessages;
    private EditText mComposer;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_agent);
        mMessages = findViewById(R.id.messages);
        mComposer = findViewById(R.id.composer);
        View root = findViewById(R.id.agent_root);
        root.requestFocus();
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int imeBottom = insets.isVisible(WindowInsets.Type.ime())
                    ? insets.getInsets(WindowInsets.Type.ime()).bottom
                    : 0;
            view.setPadding(0, 0, 0, imeBottom);
            return insets;
        });
        findViewById(R.id.send).setOnClickListener(view -> submit());
        addMessage(getString(R.string.welcome), false);
    }

    private void submit() {
        String prompt = mComposer.getText().toString().trim();
        if (TextUtils.isEmpty(prompt)) {
            return;
        }
        addMessage(prompt, true);
        mComposer.setText("");
        routeLocalCommand(prompt);
    }

    private void routeLocalCommand(String prompt) {
        String normalized = prompt.toLowerCase(Locale.ROOT);
        Intent destination = null;

        if (normalized.contains("wifi") || normalized.contains("wi-fi")) {
            destination = new Intent(Settings.ACTION_WIFI_SETTINGS);
        } else if (normalized.contains("bluetooth")) {
            destination = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        } else if (normalized.contains("setting")) {
            destination = new Intent(Settings.ACTION_SETTINGS);
        } else {
            addMessage(getString(R.string.provider_needed), false);
        }

        if (destination != null && destination.resolveActivity(getPackageManager()) != null) {
            startActivity(destination);
        }
    }

    private void addMessage(String message, boolean fromUser) {
        TextView bubble = new TextView(this);
        bubble.setText(message);
        bubble.setTextColor(getColor(fromUser ? R.color.agent_user_text : R.color.agent_text));
        bubble.setTextSize(15);
        bubble.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        bubble.setBackgroundResource(fromUser
                ? R.drawable.bg_message_user
                : R.drawable.bg_message_agent);
        bubble.setPadding(dp(16), dp(12), dp(16), dp(12));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = fromUser ? Gravity.END : Gravity.START;
        params.topMargin = dp(10);
        params.leftMargin = fromUser ? dp(44) : 0;
        params.rightMargin = fromUser ? 0 : dp(44);
        mMessages.addView(bubble, params);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
