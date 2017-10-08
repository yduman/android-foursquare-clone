package com.teammike.iptk.foursquare.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.activities.ProfileActivity;
import com.teammike.iptk.foursquare.utils.Api;
import com.teammike.iptk.foursquare.utils.AppHandler;
import com.teammike.iptk.foursquare.utils.ClientErrorHandler;
import com.teammike.iptk.foursquare.utils.Constants;
import com.teammike.iptk.foursquare.adapters.FriendsAdapter;
import com.teammike.iptk.foursquare.utils.RestClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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
 * This fragment displays all friends
 * @author Yadullah Duman
 */
public class FriendsListFragment extends Fragment {

    private static String TAG = "FriendsListFragment";

    @BindView(R.id.friends_container) ListView friendsList;
    @BindView(R.id.no_friends_msg_view) TextView noFriendsView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends_list_view, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String bundleExtra = getArguments().getString(Constants.INTENT_KEY);
        fillListView(bundleExtra);
    }

    /**
     * helper to fill the ListView with all friends
     * @param bundleExtra - response from the server
     */
    private void fillListView(String bundleExtra) {
        List<String> friends = getFriendsFromResponse(bundleExtra);
        setActivityTitle(friends.size());

        if (friends.isEmpty()) {
            int unicodeDisappointedEmoji = 0x1F61E;
            String disappointedEmoji = getEmojiByUnicode(unicodeDisappointedEmoji);
            String toDisplay = "Oops... it looks like you have no friends " + disappointedEmoji;
            noFriendsView.setText(toDisplay);
        } else {
            noFriendsView.setText(null);
            getImagesAndSetAdapter(friends);
        }
    }

    private void setActivityTitle(int numOfFriends) {
        getActivity().setTitle(getResources().getQuantityString(R.plurals.friend_list_title, numOfFriends, numOfFriends));
    }

    /**
     * helper that returns the string representation of unicode representation of an emoji
     * @param unicode - the emoji in unicode
     * @return the string representation of the emoji
     */
    private String getEmojiByUnicode(int unicode) {
        return new String(Character.toChars(unicode));
    }

    /**
     * gets all images of the requesters from the server and adds them to the adapter
     * @param friends - all usernames of friends
     */
    private void getImagesAndSetAdapter(List<String> friends) {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.postFriendImages(buildBody(friends));

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        String jsonString = response.body().string();
                        JSONArray jsonArray = new JSONObject(jsonString).getJSONArray("imagePaths");
                        FriendsAdapter friendsAdapter = new FriendsAdapter(getActivity(), R.layout.friends_item, jsonArray);
                        friendsList.setAdapter(friendsAdapter);
                        friendsList.setOnItemClickListener((adapterView, view, i, l) -> showProfile(friends.get(i)));
                        friendsAdapter.notifyDataSetChanged();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(getContext(), "Server responded with 400");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getContext(), getString(R.string.error_internal_server_error), t);
            }
        });
    }

    /**
     * helper to build postBody for getImagesAndSetAdapter()
     * @param friends - all usernames of friends
     * @return JSON body
     */
    private RequestBody buildBody(List<String> friends) {
        Map<String, Object> map = new HashMap<>();
        map.put("friends", friends.toArray());
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

                        getContext().startActivity(new Intent(getContext(), ProfileActivity.class).putExtra(Constants.INTENT_KEY, responseBody));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(getContext(), "Server responded with 400");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getContext(), "Internal Server Error.", t);
            }
        });
    }

    /**
     * gets all friends from the response
     * @param response - response from the server
     * @return a list containing all friends
     */
    private List<String> getFriendsFromResponse(String response) {
        List<String> allFriends = new ArrayList<>();

        try {
            JSONObject json = new JSONObject(response);
            JSONArray friends = json.getJSONArray("friends");

            for (int i = 0; i < friends.length(); i++) {
                String username = friends.getString(i);
                allFriends.add(username);
            }

            return allFriends;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
}
