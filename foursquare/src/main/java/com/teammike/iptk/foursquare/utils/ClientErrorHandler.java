package com.teammike.iptk.foursquare.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Handler class to handler HTTP 4xx errors
 * @author Yadullah Duman
 */
public class ClientErrorHandler {

    public static void showMessage(Context context, String errorMsg) {
        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show();
    }

}
