package com.teammike.iptk.foursquare.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import com.teammike.iptk.foursquare.App;
import com.teammike.iptk.foursquare.R;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * This activity represents the entry to the chatroom
 * @author Yadullah Duman
 */
public class ChatLoginActivity extends BaseActivity {

    @BindView(R.id.username_input) EditText usernameView;
    @BindView(R.id.chat_login_button) Button loginButton;

    private String username;
    private Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App app = (App) getApplication();
        socket = app.getSocket();
        socket.connect();

        setListeners();
        socket.on("login", onLogin);
    }

    @Override
    public void onBackPressed() {
        finish();
        startActivity(new Intent(this, HomeActivity.class));
    }

    private void setListeners() {
        usernameView.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == R.id.login || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
        });

        loginButton.setOnClickListener(view -> attemptLogin());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socket.off("login", onLogin);
    }

    /**
     * helper to login user after providing username for chat login
     */
    private void attemptLogin() {
        usernameView.setError(null);

        String username = usernameView.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            usernameView.setError(getString(R.string.error_field_required));
            usernameView.requestFocus();
            return;
        }

        this.username = username;
        socket.emit("add user", username);
    }

    /**
     * listener for chat login
     */
    private Emitter.Listener onLogin = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];

            int numUsers;
            try {
                numUsers = data.getInt("numUsers");
            } catch (JSONException e) {
                return;
            }

            finish();
            startActivity(new Intent(ChatLoginActivity.this, ChatActivity.class)
                    .putExtra("username", username)
                    .putExtra("numUsers", numUsers));
        }
    };

    @Override
    int getContentViewId() {
        return R.layout.activity_chat_login;
    }

    @Override
    int getNavigationMenuItem() {
        return R.id.navigation_chat;
    }
}
