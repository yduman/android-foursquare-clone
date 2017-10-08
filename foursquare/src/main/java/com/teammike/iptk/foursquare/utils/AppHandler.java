package com.teammike.iptk.foursquare.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Handler class to get/set some important params
 * @author Yadullah Duman
 */
public final class AppHandler {
    private AppHandler() {}

    private static String token = "";
    private static String id = "";
    private static String username = "";

    public static void setToken(String newToken) {
        if (newToken != null)
            token = newToken;
    }

    public static void setId(String newId) {
        if (newId != null && !id.equals(newId))
            id = newId;
    }

    public static void setUsername(String newUsername) {
        if (username != null)
            username = newUsername;
    }

    public static String getToken() {
        return token;
    }

    public static String getId() {
        return id;
    }

    public static String getUsername() {
        return username;
    }

    public static void logError(Context context, String msg, Throwable t) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        Log.e("REQUEST_FAILURE", t.getMessage());
    }
}
