package com.teammike.iptk.foursquare.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.utils.Api;
import com.teammike.iptk.foursquare.utils.AppHandler;
import com.teammike.iptk.foursquare.utils.ClientErrorHandler;
import com.teammike.iptk.foursquare.utils.Constants;
import com.teammike.iptk.foursquare.adapters.FriendRequestsAdapter;
import com.teammike.iptk.foursquare.utils.RestClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
 * This fragment displays all friend requests
 * @author Yadullah Duman
 */
public class FriendRequestsListFragment extends Fragment {

    @BindView(R.id.friend_requests_container) ListView friendRequestsList;
    @BindView(R.id.no_friend_requests_msg_view) TextView noFriendRequestsView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend_requests_list_view, container, false);
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
     * gets all friend requests and fills the ListView with data
     * @param bundleExtra - response from the server
     */
    private void fillListView(String bundleExtra) {
        List<String> friendRequests = getFriendRequestsFromResponse(bundleExtra);
        setActivityTitle(friendRequests.size());

        if (friendRequests.isEmpty()) {
            int unicodeDisappointedEmoji = 0x1F61E;
            String disappointedEmoji = getEmojiByUnicode(unicodeDisappointedEmoji);
            String toDisplay = "A pity... no friend requests " + disappointedEmoji;
            noFriendRequestsView.setText(toDisplay);
        } else {
            noFriendRequestsView.setText(null);
            getImagesAndSetAdapter(friendRequests);
        }
    }

    private void setActivityTitle(int numOfFriends) {
        getActivity().setTitle(getResources().getQuantityString(R.plurals.friend_request_list_title, numOfFriends, numOfFriends));
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
     * @param friendRequests - all usernames, that sent a friendship request
     */
    private void getImagesAndSetAdapter(List<String> friendRequests) {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.postFriendImages(buildBody(friendRequests));

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        String jsonString = response.body().string();
                        JSONArray jsonArray = new JSONObject(jsonString).getJSONArray("imagePaths");
                        FriendRequestsAdapter adapter = new FriendRequestsAdapter(jsonArray, getContext());
                        friendRequestsList.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
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
                AppHandler.logError(getContext(), "Internal Server Error", t);
            }
        });
    }

    /**
     * helper to build postBody for getImagesAndSetAdapter()
     * @param friendRequests - all usernames, that sent a friendship request
     * @return JSON body
     */
    private RequestBody buildBody(List<String> friendRequests) {
        Map<String, Object> map = new HashMap<>();
        map.put("friends", friendRequests.toArray());
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new JSONObject(map).toString());
    }

    /**
     * gets all friend requests
     * @param response - response from the server
     * @return a list containing all friend requests
     */
    private List<String> getFriendRequestsFromResponse(String response) {
        List<String> allFriendRequests = new ArrayList<>();

        try {
            JSONObject json = new JSONObject(response);
            JSONArray friends = json.getJSONArray("friendRequests");

            for (int i = 0; i < friends.length(); i++) {
                String username = friends.getString(i);
                allFriendRequests.add(username);
            }

            return allFriendRequests;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
}
