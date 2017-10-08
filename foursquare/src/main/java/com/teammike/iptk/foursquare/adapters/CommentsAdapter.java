package com.teammike.iptk.foursquare.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.activities.ProfileActivity;
import com.teammike.iptk.foursquare.utils.Api;
import com.teammike.iptk.foursquare.utils.AppHandler;
import com.teammike.iptk.foursquare.utils.ClientErrorHandler;
import com.teammike.iptk.foursquare.utils.Constants;
import com.teammike.iptk.foursquare.utils.RestClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This adapter is responsible for the list view of @{@link com.teammike.iptk.foursquare.activities.VenueOverviewActivity}
 * @author Yadullah Duman
 */
public class CommentsAdapter extends BaseAdapter implements ListAdapter {

    private static final String TAG = "CommentsAdapter";
    private List<JSONObject> list = new ArrayList<>();
    private Context context;
    private String username, comment, venueId, imagePath;
    private int likes;

    public CommentsAdapter(List<JSONObject> list, Context context, String venueId) {
        this.list = list;
        this.context = context;
        this.venueId = venueId;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        View view = convertView;
        JSONObject commentObject = (JSONObject) getItem(pos);

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.comment_item, null);
        }

        getCommentDataFrom(commentObject);
        setUp(view, commentObject);
        setLikeAndDislikeListeners(view, commentObject, pos);

        return view;
    }

    /**
     * helper to fetch the data from the comment object
     * @param commentObject - the comment represented as JSON
     */
    private void getCommentDataFrom(JSONObject commentObject) {
        try {
            this.username = commentObject.getString("username");
            this.comment = commentObject.getString("comment");
            this.likes = commentObject.getInt("likes");
            if (commentObject.has("image"))
                this.imagePath = commentObject.getString("image");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * create the view
     * @param view - current view
     * @param commentObject - current comment json
     */
    private void setUp(View view, JSONObject commentObject) {
        try {
            TextView username = view.findViewById(R.id.comment_username);
            TextView comment = view.findViewById(R.id.comment_content);
            TextView likes = view.findViewById(R.id.comment_likes_count);
            ImageView image = view.findViewById(R.id.comment_image);

            String currentUsername = commentObject.getString("username");

            username.setText(this.username);
            username.setOnClickListener(v -> showProfile(currentUsername));
            comment.setText(this.comment);
            likes.setText(String.valueOf(this.likes));

            if (imagePath != null)
                Picasso.with(context).load(Constants.BASE_URL_STATIC_DEV + imagePath).into(image);
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't fetch username field from commentObject!");
        }
    }

    /**
     * listeners for like/dislike buttons
     * @param view - the current view
     * @param comment - the JSON comment object
     * @param pos - the position of the comment in the list
     */
    private void setLikeAndDislikeListeners(View view, JSONObject comment, int pos) {
        ImageButton likeButton = view.findViewById(R.id.comment_like_button);
        ImageButton dislikeButton = view.findViewById(R.id.comment_dislike_button);

        likeButton.setOnClickListener(v -> checkIfUserLikedOrDisliked(venueId, pos, view, comment, true, likeButton, dislikeButton));
        dislikeButton.setOnClickListener(v -> checkIfUserLikedOrDisliked(venueId, pos, view, comment, false, likeButton, dislikeButton));
    }

    /**
     * helper to disable the like/dislike button after liking/disliking
     * @param likeButton - the like button
     * @param dislikeButton - the dislike button
     */
    private void disableButtons(ImageButton likeButton, ImageButton dislikeButton) {
        likeButton.setEnabled(false);
        dislikeButton.setEnabled(false);
    }

    /**
     *
     * @param venueId - the id of the current venue
     * @param pos - the position of comment in the list
     * @param view - the current view
     * @param comment - the comment object
     * @param isLike - if liked: true, if disliked: false
     * @param likeButton - the like button
     * @param dislikeButton - the dislike button
     */
    private void checkIfUserLikedOrDisliked(String venueId, int pos, View view, JSONObject comment, boolean isLike, ImageButton likeButton, ImageButton dislikeButton) {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.getUserForRatedComment(venueId, pos);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    // user did not like/dislike, he can rate the comment
                    updateLikes(view, comment, pos, isLike);
                    disableButtons(likeButton, dislikeButton);
                } else if (response.code() == 204) {
                    // user already liked/disliked comment, disable buttons
                    disableButtons(likeButton, dislikeButton);
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(context, "Bad Request");
                } else if (response.code() == 404) {
                    ClientErrorHandler.showMessage(context, "Could not find venue.");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(context, "Internal Server Error", t);
            }
        });
    }

    /**
     * updates likes count and sends the information to the server
     * @param view - the current view
     * @param comment - the comment object
     * @param pos - the position of the comment in the list
     * @param isLike - true, if liked and false, if disliked
     */
    private void updateLikes(View view, JSONObject comment, int pos, boolean isLike) {
        try {
            int likesCount = comment.getInt("likes");
            if (isLike)
                likesCount += 1;
            else
                likesCount -= 1;
            updateComment(view, likesCount, pos, isLike);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * call REST endpoint to update the comment
     * @param view - the current view
     * @param likes - the likes count
     * @param pos - the position of the comment in the list
     * @param isLike - true, if liked and false, if disliked
     */
    private void updateComment(View view, int likes, int pos, boolean isLike) {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.updateComment(venueId, buildLikesPostBody(likes, pos, isLike));

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    updateView();
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(context, "Bad Request");
                } else if (response.code() == 404) {
                    ClientErrorHandler.showMessage(context, "Couldn't find the venue.");
                }
            }

            private void updateView() {
                TextView likesView = view.findViewById(R.id.comment_likes_count);
                likesView.setText(String.valueOf(likes));
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(context, "Internal Server Error", t);
            }
        });
    }

    /**
     * helper to build post body for the updateComment() method
     * @param likes - the likes count
     * @param pos - the position of the comment in the list
     * @param isLike - true, if liked and false, if disliked
     * @return JSON post body for updateComment()
     */
    private RequestBody buildLikesPostBody(int likes, int pos, boolean isLike) {
        Map<String, Object> map = new HashMap<>();
        map.put("likes", likes);
        map.put("pos", pos);
        map.put("isLike", isLike);
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new JSONObject(map).toString());
    }

    /**
     * calls REST endpoint the display the profile of a friend
     * @param username - the username of the friend
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

                        context.startActivity(new Intent(context, ProfileActivity.class).putExtra(Constants.INTENT_KEY, responseBody));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(context, "Server responded with 400");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(context, "Internal Server Error.", t);
            }
        });
    }

}
