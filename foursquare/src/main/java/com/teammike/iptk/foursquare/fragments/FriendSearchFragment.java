package com.teammike.iptk.foursquare.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;
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

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendSearchFragment extends Fragment {

    private static String TAG = "FriendSearchFragment";
    private JSONObject friendJSON;

    @BindView(R.id.searchView) SearchView searchView;
    @BindView(R.id.friend_image) ImageView friendImage;
    @BindView(R.id.friend_username) TextView friendUsername;

    @BindString(R.string.error_internal_server_error) String internalServerErrorMessage;
    @BindString(R.string.error_invalid_user) String invalidUsernameMessage;
    @BindString(R.string.enter_your_friend_s_username) String queryHint;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend_search, container, false);
        ButterKnife.bind(this, view);

        setListeners();
        return view;
    }

    private void setListeners() {
        friendUsername.setOnClickListener(v -> showFriendsProfile(friendJSON));
        searchView.setQueryHint(queryHint);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String username) {
                getUserByUsername(username);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

    private void showFriendsProfile(JSONObject friendJSON) {
        getContext().startActivity(new Intent(getContext(), ProfileActivity.class).putExtra(Constants.INTENT_KEY, friendJSON.toString()));
    }

    private void getUserByUsername(String username) {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.getUserByUsername(username);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        String jsonString = response.body().string();
                        JSONObject friend = new JSONObject(jsonString);
                        friendJSON = friend;
                        displayFriend(friend);
                    } catch (IOException e) {
                        Log.e(TAG, "Couldn't read body of JSON due to an IO Exception!");
                    } catch (JSONException e) {
                        Log.e(TAG, "Couldn't read JSON!");
                    }
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(getContext(), invalidUsernameMessage);
                } else if (response.code() == 404) {
                    ClientErrorHandler.showMessage(getContext(), invalidUsernameMessage);
                }
            }

            private void displayFriend(JSONObject user) throws JSONException {
                String username = user.getString("username");
                String imagePath = user.getString("imagePath");

                friendUsername.setText(username);
                if (imagePath.equals("no image"))
                    setDefaultImage();
                else
                    setImageWithPicasso(imagePath);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getContext(), internalServerErrorMessage, t);
            }
        });
    }

    private void setImageWithPicasso(String imagePath) {
        Picasso.with(getContext()).load(Constants.BASE_URL_STATIC_DEV + imagePath).into(friendImage);
    }

    private void setDefaultImage() {
        friendImage.setImageResource(R.drawable.ic_sentiment_satisfied_black_24dp);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle("Find your friend");
    }
}
