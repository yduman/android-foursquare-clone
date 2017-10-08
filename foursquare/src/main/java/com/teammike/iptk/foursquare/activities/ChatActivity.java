package com.teammike.iptk.foursquare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.teammike.iptk.foursquare.App;
import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.adapters.Message;
import com.teammike.iptk.foursquare.adapters.MessageAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * This activity represents the chatroom
 * @author Yadullah Duman
 */
public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final int TYPING_TIMER_LENGTH = 600;

    private List<Message> messages = new ArrayList<>();
    private RecyclerView.Adapter adapter;
    private boolean isTyping = false;
    private Handler TypingHandler = new Handler();
    private String username;
    private Socket socket;
    private Boolean isConnected = true;

    @BindView(R.id.messages) RecyclerView messagesView;
    @BindView(R.id.message_input) EditText inputMessageView;
    @BindView(R.id.send_button) ImageButton sendButton;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindString(R.string.connect) String connected;
    @BindString(R.string.disconnect) String disconnected;
    @BindString(R.string.error_connect) String failedToConnect;
    @BindString(R.string.title_chat) String activityTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);
        adapter = new MessageAdapter(this, messages);

        App app = (App) getApplication();

        setSupportActionBar(toolbar);
        setTitle(activityTitle);
        setIntentExtras();
        setSockets(app);
        setViewAndListeners();
    }

    @Override
    public void onBackPressed() {
        leave();
    }

    /**
     * get username, greet user and update number of users participating in chat
     */
    private void setIntentExtras() {
        username = getIntent().getStringExtra("username");
        int numUsers = getIntent().getIntExtra("numUsers", 1);
        addLog(getResources().getString(R.string.message_welcome));
        addParticipantsLog(numUsers);
    }

    /**
     * init socket events
     * @param app - {@link App}
     */
    private void setSockets(App app) {
        socket = app.getSocket();
        socket.on(Socket.EVENT_CONNECT, onConnect);
        socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        socket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        socket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        socket.on("new message", onNewMessage);
        socket.on("user joined", onUserJoined);
        socket.on("user left", onUserLeft);
        socket.on("typing", onTyping);
        socket.on("stop typing", onStopTyping);
        socket.connect();
    }

    /**
     * helper to set up adapter and listeners
     */
    private void setViewAndListeners() {
        messagesView.setLayoutManager(new LinearLayoutManager(this));
        messagesView.setAdapter(adapter);
        sendButton.setOnClickListener(v -> attemptSend());

        inputMessageView.setOnEditorActionListener((v, id, event) -> {
            if (id == EditorInfo.IME_ACTION_SEND || id == EditorInfo.IME_NULL) {
                attemptSend();
                return true;
            }
            return false;
        });

        inputMessageView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (null == username) return;
                if (!socket.connected()) return;

                if (!isTyping) {
                    isTyping = true;
                    socket.emit("typing");
                }

                TypingHandler.removeCallbacks(onTypingTimeout);
                TypingHandler.postDelayed(onTypingTimeout, TYPING_TIMER_LENGTH);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        socket.disconnect();

        socket.off(Socket.EVENT_CONNECT, onConnect);
        socket.off(Socket.EVENT_DISCONNECT, onDisconnect);
        socket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        socket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        socket.off("new message", onNewMessage);
        socket.off("user joined", onUserJoined);
        socket.off("user left", onUserLeft);
        socket.off("typing", onTyping);
        socket.off("stop typing", onStopTyping);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_leave) {
            leave();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * helper to display log messages like greeting, user joined/left etc.
     * @param message
     */
    private void addLog(String message) {
        messages.add(new Message.Builder(Message.TYPE_LOG)
                .message(message)
                .build());
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    /**
     * helper for displaying number of users
     * @param numUsers - number of users
     */
    private void addParticipantsLog(int numUsers) {
        addLog(getResources().getQuantityString(R.plurals.message_participants, numUsers, numUsers));
    }

    /**
     * helper for displaying a new message
     * @param username - the username of the user who sent the message
     * @param message - the message itself
     */
    private void addMessage(String username, String message) {
        messages.add(new Message.Builder(Message.TYPE_MESSAGE)
                .username(username)
                .message(message)
                .build());
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    /**
     * helper to show, that user is typing
     * @param username - the username of the user who is typing
     */
    private void addTyping(String username) {
        messages.add(new Message.Builder(Message.TYPE_ACTION)
                .username(username)
                .build());
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    /**
     * helper to remove the notification, that a user is typing
     * @param username - the username of the user who was typing
     */
    private void removeTyping(String username) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message.getType() == Message.TYPE_ACTION && message.getUsername().equals(username)) {
                messages.remove(i);
                adapter.notifyItemRemoved(i);
            }
        }
    }

    /**
     * responsible for sending the message
     */
    private void attemptSend() {
        if (null == username) {
            Log.i(TAG, "username is null");
            return;
        }
        if (!socket.connected()) {
            Log.i(TAG, "socket is not connected");
            return;
        }

        isTyping = false;

        String message = inputMessageView.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            inputMessageView.requestFocus();
            return;
        }

        inputMessageView.setText("");
        addMessage(username, message);

        // perform the sending message attempt.
        socket.emit("new message", message);
    }

    /**
     * helper to leave chat
     */
    private void leave() {
        username = null;
        socket.disconnect();
        finish();
        startActivity(new Intent(this, HomeActivity.class));
    }

    /**
     * helper to scroll to bottom on certain events (e.g. sending message)
     */
    private void scrollToBottom() {
        messagesView.scrollToPosition(adapter.getItemCount() - 1);
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            ChatActivity.this.runOnUiThread(() -> {
                if (!isConnected) {
                    if (null != username)
                        socket.emit("add user", username);
                    Toast.makeText(ChatActivity.this, connected, Toast.LENGTH_LONG).show();
                    isConnected = true;
                }
            });
        }
    };

    /**
     * listener for disconnection
     */
    private Emitter.Listener onDisconnect = args -> ChatActivity.this.runOnUiThread(() -> {
        Log.i(TAG, "diconnected");
        isConnected = false;
        Toast.makeText(ChatActivity.this, disconnected, Toast.LENGTH_LONG).show();
    });

    /**
     * listener for connection error
     */
    private Emitter.Listener onConnectError = args -> ChatActivity.this.runOnUiThread(() -> {
        Log.e(TAG, "Error connecting");
        Toast.makeText(ChatActivity.this.getApplicationContext(), failedToConnect, Toast.LENGTH_LONG).show();
    });

    /**
     * listener for new message
     */
    private Emitter.Listener onNewMessage = args -> ChatActivity.this.runOnUiThread(() -> {
        JSONObject data = (JSONObject) args[0];
        String username;
        String message;
        try {
            username = data.getString("username");
            message = data.getString("message");
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        removeTyping(username);
        addMessage(username, message);
    });

    /**
     * listener for joining chat
     */
    private Emitter.Listener onUserJoined = args -> ChatActivity.this.runOnUiThread(() -> {
        JSONObject data = (JSONObject) args[0];
        String username;
        int numUsers;
        try {
            username = data.getString("username");
            numUsers = data.getInt("numUsers");
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        addLog(getResources().getString(R.string.message_user_joined, username));
        addParticipantsLog(numUsers);
    });

    /**
     * listener for leaving chat
     */
    private Emitter.Listener onUserLeft = args -> ChatActivity.this.runOnUiThread(() -> {
        JSONObject data = (JSONObject) args[0];
        String username;
        int numUsers;
        try {
            username = data.getString("username");
            numUsers = data.getInt("numUsers");
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        addLog(getResources().getString(R.string.message_user_left, username));
        addParticipantsLog(numUsers);
        removeTyping(username);
    });

    /**
     * listener for typing event
     */
    private Emitter.Listener onTyping = args -> ChatActivity.this.runOnUiThread(() -> {
        JSONObject data = (JSONObject) args[0];
        String username;
        try {
            username = data.getString("username");
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return;
        }
        addTyping(username);
    });

    /**
     * listener for stop typing event
     */
    private Emitter.Listener onStopTyping = args -> ChatActivity.this.runOnUiThread(() -> {
        JSONObject data = (JSONObject) args[0];
        String username;
        try {
            username = data.getString("username");
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return;
        }
        removeTyping(username);
    });

    /**
     * helper to set up a timeout for typing event
     */
    private Runnable onTypingTimeout = new Runnable() {
        @Override
        public void run() {
            if (!isTyping) return;

            isTyping = false;
            socket.emit("stop typing");
        }
    };

}
