package com.teammike.iptk.foursquare.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.activities.ProfileActivity;
import com.teammike.iptk.foursquare.utils.Api;
import com.teammike.iptk.foursquare.utils.AppHandler;
import com.teammike.iptk.foursquare.utils.Constants;
import com.teammike.iptk.foursquare.utils.RestClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This adapter is responsible for the list view of @{@link com.teammike.iptk.foursquare.fragments.FriendRequestsListFragment}
 * @author Yadullah Duman
 */
public class FriendRequestsAdapter extends BaseAdapter implements ListAdapter {

    private static final String TAG = "FriendRequestsAdapter";
    private JSONArray jsonArray;
    private Context context;

    public FriendRequestsAdapter(JSONArray jsonArray, Context context) {
        this.jsonArray = jsonArray;
        this.context = context;
    }

    @Override
    public int getCount() {
        return jsonArray.length();
    }

    @Override
    public JSONObject getItem(int i) {
        try {
            return jsonArray.getJSONObject(i);
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't fetch JSON field!");
            return null;
        }
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        try {
            View view = convertView;
            String username = getItem(pos).getString("username");
            String imagePath = getItem(pos).getString("imagePath");

            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.friend_requests_item, null);
            }

            Button usernameButton = view.findViewById(R.id.friend_requests_username_button);
            ImageView profileImageView = view.findViewById(R.id.friend_requests_image);
            ImageButton acceptFriendshipButton = view.findViewById(R.id.friend_requests_accept_button);
            ImageButton rejectFriendshipButton = view.findViewById(R.id.friend_requests_reject_button);

            if (!imagePath.equals("no image"))
                setImageWithPicasso(imagePath, profileImageView);
            else
                setDefaultImage(profileImageView);

            usernameButton.setText(username);
            usernameButton.setOnClickListener(v -> showProfile(username));
            acceptFriendshipButton.setOnClickListener(v -> updateFriendship(username, "accept", pos));
            rejectFriendshipButton.setOnClickListener(v -> updateFriendship(username, "reject", pos));

            return view;
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't fetch data from JSON!");
        }
        return null;
    }

    /**
     * helper to set image with Picasso
     * @param imagePath - the image path
     * @param profileImageView - the view it will be displayed
     */
    private void setImageWithPicasso(String imagePath, ImageView profileImageView) {
        Picasso.with(context).load(Constants.BASE_URL_STATIC_DEV + imagePath).into(profileImageView);
    }

    /**
     * helper to set default image on failure events
     */
    private void setDefaultImage(ImageView imageView) {
        imageView.setImageResource(R.drawable.ic_sentiment_satisfied_black_24dp);
    }

    /**
     * call REST endpoint to update friendship
     * @param username - the username of the user who wants to be your friend
     * @param approval - the action for the friendship
     * @param pos - the position of the request in the list
     */
    private void updateFriendship(String username, String approval, int pos) {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.updateFriendship(buildFriendshipPostBody(username, approval));

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    jsonArray.remove(pos);
                    notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(context, "Internal Server Error.", t);
            }
        });
    }

    /**
     * helper to build post body for updateFriendship()
     * @param username - the user of the user who wants to be your friend
     * @param approval - the action for the friendship
     * @return JSON post body for updateFriendship()
     */
    private RequestBody buildFriendshipPostBody(String username, String approval) {
        Map<String, Object> map = new HashMap<>();
        map.put("username", username);
        map.put("approval", approval);
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new JSONObject(map).toString());
    }

    /**
     * calls REST endpoint the show the profile of the user who wants to be your friend
     * @param username - the username of the user who wants to be your friend
     */
    private void showProfile(String username) {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.getUserByUsername(username);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        String responseBody = response.body().string();

                        Log.i(TAG, responseBody);
                        Log.i("RES_MSG", response.message());

                        context.startActivity(new Intent(context, ProfileActivity.class)
                                .putExtra(Constants.INTENT_KEY, responseBody));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    Log.i(TAG, "Server responded with 400");
                    Toast.makeText(context, "Server responded with 400", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(context, "Internal Server Error.", t);
            }
        });
    }

}
