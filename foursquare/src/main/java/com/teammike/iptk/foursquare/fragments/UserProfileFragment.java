package com.teammike.iptk.foursquare.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.utils.Api;
import com.teammike.iptk.foursquare.utils.AppHandler;
import com.teammike.iptk.foursquare.utils.ClientErrorHandler;
import com.teammike.iptk.foursquare.utils.Constants;
import com.teammike.iptk.foursquare.utils.RestClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This fragment displays the view of other user profiles and not of the logged in user.
 * The fragment for the logged in user is @{@link ProfileFragment}
 * @author Yadullah Duman
 */
public class UserProfileFragment extends Fragment {

    private String usernameFromJson;
    private boolean isFriend;

    @BindView(R.id.profile_username_view) TextView username;
    @BindView(R.id.profile_email_view) TextView email;
    @BindView(R.id.profile_realname_view) TextView realname;
    @BindView(R.id.profile_age_view) TextView age;
    @BindView(R.id.profile_domicile_view) TextView domicile;
    @BindView(R.id.profile_picture_button) ImageView profileImage;
    @BindView(R.id.friendship_button) FloatingActionButton friendShipButton;
    @BindString(R.string.error_profile_picture) String errorMsgProfilePicture;
    @BindString(R.string.error_internal_server_error) String internalServerErrorMsg;
    @BindString(R.string.info_friendship_success) String friendshipRequestSuccessMsg;
    @BindString(R.string.info_unfriend_success) String unfriendSuccessMsg;
    @BindString(R.string.error_invalid_user) String invalidUserMsg;
    @BindString(R.string.error_bad_request) String badRequestMsg;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile_view, container, false);
        ButterKnife.bind(this, view);

        setListeners();

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String bundleExtra = getArguments().getString(Constants.INTENT_KEY);
        readUserDataFrom(bundleExtra);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
    }

    @Override
    public void onStop() {
        super.onStop();
        ((AppCompatActivity) getActivity()).getSupportActionBar().show();
    }

    private void setListeners() {
        friendShipButton.setOnClickListener(view -> friendship());
    }

    /**
     * checks friendship state and executes friendship request appropriately
     */
    private void friendship() {
        if (!isFriend) {
            addFriend();
        } else {
            removeFriend();
        }
    }

    /**
     * calls REST endpoint to send friendship request
     */
    private void addFriend() {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.postFriendRequest(getUsername());

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    Toast.makeText(getContext(), friendshipRequestSuccessMsg, Toast.LENGTH_SHORT).show();
                    friendShipButton.setImageResource(R.drawable.ic_remove_black_24dp);
                    isFriend = true;
                } else if (response.code() == 404) {
                    ClientErrorHandler.showMessage(getContext(), invalidUserMsg);
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(getContext(), badRequestMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getContext(), internalServerErrorMsg, t);
            }
        });
    }

    /**
     * calls REST endpoint to remove friendship
     */
    private void removeFriend() {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.deleteFriendshipByUsername(usernameFromJson);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    Toast.makeText(getContext(), unfriendSuccessMsg, Toast.LENGTH_SHORT).show();
                    friendShipButton.setImageResource(R.drawable.ic_add_black_24dp);
                    isFriend = false;
                } else if (response.code() == 404) {
                    ClientErrorHandler.showMessage(getContext(), invalidUserMsg);
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(getContext(), badRequestMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getContext(), internalServerErrorMsg, t);
            }
        });
    }

    /**
     * builds post body for addFriend()
     * @return JSON body
     */
    private RequestBody getUsername() {
        Map<String, Object> map = new HashMap<>();
        map.put("username", usernameFromJson);
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new JSONObject(map).toString());
    }

    /**
     * helper to build the view from server response
     * @param bundleExtra - user object
     */
    private void readUserDataFrom(String bundleExtra) {
        try {
            JSONObject json = new JSONObject(bundleExtra);
            setUserData(json);
            setFriendshipButtonState(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * helper to set image with Picasso
     * @param imagePath - image path of the image
     */
    private void setProfileImageWithPicasso(String imagePath) {
        Picasso.with(getContext()).load(Constants.BASE_URL_STATIC_DEV + imagePath).into(profileImage);
    }

    /**
     * helper to set default image on failure events
     */
    private void setDefaultProfileImage() {
        profileImage.setImageResource(R.drawable.ic_sentiment_satisfied_black_24dp);
    }

    /**
     * setter for all the views
     * @param json - user object
     */
    private void setUserData(JSONObject json) {
        try {
            usernameFromJson = json.getString("username");
            username.setText(usernameFromJson);
            email.setText(json.getString("email"));
            realname.setText(json.getString("fullname"));
            age.setText(String.valueOf(json.getInt("age")));
            domicile.setText(json.getString("domicile"));
            if (!json.getString("imagePath").equals("no image"))
                setProfileImageWithPicasso(json.getString("imagePath"));
            else
                setDefaultProfileImage();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * helper to set friendship fab button state
     * @param json - user object
     */
    private void setFriendshipButtonState(JSONObject json) {
        try {
            JSONArray jsonfriends = json.getJSONArray("friends");
            List<String> friends = getFriends(jsonfriends);

            if (friends.contains(AppHandler.getUsername())) {
                friendShipButton.setImageResource(R.drawable.ic_remove_black_24dp);
                isFriend = true;
            } else {
                friendShipButton.setImageResource(R.drawable.ic_add_black_24dp);
                isFriend = false;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * helper to get all usernames from friends
     * @param jsonfriends - friends array on user object
     * @return list containing all usernames of friends
     */
    private List<String> getFriends(JSONArray jsonfriends) {
        try {
            ArrayList<String> list = new ArrayList<>();

            for (int i = 0; i < jsonfriends.length(); i++)
                list.add(jsonfriends.getString(i));

            return list;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
}
