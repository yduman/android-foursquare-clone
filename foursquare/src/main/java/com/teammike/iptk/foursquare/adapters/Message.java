package com.teammike.iptk.foursquare.adapters;

/**
 * This class represents a message object for {@link com.teammike.iptk.foursquare.activities.ChatActivity}
 * @author Yadullah Duman
 */
public class Message {

    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_LOG = 1;
    public static final int TYPE_ACTION = 2;

    private int type;
    private String message;
    private String username;

    private Message() {}

    public int getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getUsername() {
        return username;
    }

    /**
     * builder pattern for message object
     */
    public static class Builder {
        private final int type;
        private String username;
        private String message;

        public Builder(int type) {
            this.type = type;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Message build() {
            Message message = new Message();
            message.type = this.type;
            message.username = this.username;
            message.message = this.message;
            return message;
        }
    }
}
