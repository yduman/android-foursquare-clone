package com.teammike.iptk.foursquare;

import android.app.Application;
import com.teammike.iptk.foursquare.utils.Constants;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * The application class, which is responsible for building the box store for objectbox.
 * @author Yadullah Duman
 */
public class App extends Application {

    public static final String TAG = "App";
    private Socket socket;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            socket = IO.socket(Constants.CHAT_URL_DEV);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket() {
        return socket;
    }
}
