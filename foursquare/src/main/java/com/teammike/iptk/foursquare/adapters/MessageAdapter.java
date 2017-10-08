package com.teammike.iptk.foursquare.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.teammike.iptk.foursquare.R;

import java.util.List;

/**
 * This adapter is responsible for view in {@link com.teammike.iptk.foursquare.activities.ChatActivity}
 * @author Yadullah Duman
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private List<Message> messages;
    private int[] usernameColors;

    public MessageAdapter(Context context, List<Message> messages) {
        this.messages = messages;
        usernameColors = context.getResources().getIntArray(R.array.username_colors);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout = -1;
        switch (viewType) {
            case Message.TYPE_MESSAGE:
                layout = R.layout.item_message;
                break;
            case Message.TYPE_LOG:
                layout = R.layout.item_log;
                break;
            case Message.TYPE_ACTION:
                layout = R.layout.item_action;
                break;
        }
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Message message = messages.get(position);
        viewHolder.setMessage(message.getMessage());
        viewHolder.setUsername(message.getUsername());
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView usernameView;
        private TextView messageView;

        public ViewHolder(View itemView) {
            super(itemView);

            usernameView = itemView.findViewById(R.id.username);
            messageView = itemView.findViewById(R.id.message);
        }

        public void setUsername(String username) {
            if (usernameView == null) return;
            usernameView.setText(username);
            usernameView.setTextColor(getUsernameColor(username));
        }

        public void setMessage(String message) {
            if (messageView == null) return;
            messageView.setText(message);
        }

        /**
         * helper to set different colors for usernames
         * @param username - the username of the user inside of chat
         * @return a color for username
         */
        private int getUsernameColor(String username) {
            int hash = 7;
            for (int i = 0, len = username.length(); i < len; i++) {
                hash = username.codePointAt(i) + (hash << 5) - hash;
            }
            int index = Math.abs(hash % usernameColors.length);
            return usernameColors[index];
        }
    }
}
