package com.teammike.iptk.foursquare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.adapters.CheckInsAdapter;
import com.teammike.iptk.foursquare.utils.Api;
import com.teammike.iptk.foursquare.utils.AppHandler;
import com.teammike.iptk.foursquare.utils.ClientErrorHandler;
import com.teammike.iptk.foursquare.utils.Constants;
import com.teammike.iptk.foursquare.utils.RestClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindString;
import butterknife.BindView;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This activity is responsible for displaying the history of check-ins for the logged in user.
 * @author Yadullah Duman
 */
public class CheckInsActivity extends BaseActivity {

    private static String TAG = "CheckInsActivity";
    private List<JSONObject> venues;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.venues_container) ListView venuesList;
    @BindView(R.id.no_check_ins_message) TextView noCheckInsMessage;
    @BindString(R.string.title_check_ins_activity) String checkInsTitle;
    @BindString(R.string.error_invalid_venue_request) String invalidRequestMsg;
    @BindString(R.string.error_invalid_venue) String invalidVenueMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSupportActionBar(toolbar);
        setTitle(checkInsTitle);

        String bundleExtra = getIntent().getStringExtra(Constants.INTENT_KEY);
        fillListView(bundleExtra);
    }

    @Override
    public void onBackPressed() {
        finish();
        startActivity(new Intent(this, HomeActivity.class));
    }

    /**
     * helper to fill list view with check-ins the user checked in
     * @param bundleExtra - JSON response from the server, returning the venue-ids of check-ins
     */
    private void fillListView(String bundleExtra) {
        try {
            JSONArray checkIns = new JSONObject(bundleExtra).getJSONArray("checkIns");

            if (checkIns.length() == 0) {
                int unicodeDisappointedEmoji = 0x1F61E;
                String disappointedEmoji = getEmojiByUnicode(unicodeDisappointedEmoji);
                String toDisplay = "You have no check-ins at the moment " + disappointedEmoji;
                noCheckInsMessage.setText(toDisplay);
            } else {
                noCheckInsMessage.setVisibility(View.GONE);
                getVenues(checkIns);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
     * retrieves all venues by id and builds the view
     * @param checkIns - JSONArray containing the venue ids of all checked-in venues
     */
    private void getVenues(JSONArray checkIns) {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.getCheckedInVenues(getCheckInsBody(checkIns));

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        List<JSONObject> checkedInVenues = new ArrayList<>();
                        String responseBody = response.body().string();

                        JSONArray allVenuesWithTimestamps = new JSONObject(responseBody).getJSONArray("venues");

                        for (int i = 0; i < allVenuesWithTimestamps.length(); i++) {
                            checkedInVenues.add(allVenuesWithTimestamps.getJSONObject(i));
                        }

                        venues = checkedInVenues;

                        CheckInsAdapter checkInsAdapter = new CheckInsAdapter(checkedInVenues, CheckInsActivity.this);
                        venuesList.setAdapter(checkInsAdapter);
                        venuesList.setOnItemClickListener((adapterView, view, pos, id) -> {
                            try {
                                sendVenueOverviewRequestToServer(venues.get(pos).getJSONObject("venue").toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });
                        checkInsAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(CheckInsActivity.this, invalidRequestMsg);
                } else if (response.code() == 404) {
                    ClientErrorHandler.showMessage(CheckInsActivity.this, invalidVenueMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(CheckInsActivity.this, internalServerErrorMsg, t);
            }

        });
    }

    /**
     * sends user to {@link VenueOverviewActivity} when clicking a venue on the history
     * @param jsonString
     */
    private void sendVenueOverviewRequestToServer(String jsonString) {
        String id = getIdFrom(jsonString);
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.getVenueById(id);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        String responseBody = response.body().string();
                        startActivity(new Intent(CheckInsActivity.this, VenueOverviewActivity.class).putExtra(Constants.INTENT_KEY, responseBody));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(CheckInsActivity.this, invalidRequestMsg);
                } else if (response.code() == 404) {
                    ClientErrorHandler.showMessage(CheckInsActivity.this, invalidVenueMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getApplicationContext(), internalServerErrorMsg, t);
            }
        });
    }

    /**
     * helper to fetch the id of the venue
     * @param jsonString - JSON of the venue as string
     * @return the id of the venue
     */
    private String getIdFrom(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            return json.getString("_id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * builds the post body for POST /venues/checkins
     * @param checkIns - JSONArray containing ids of the checked-in venues
     * @return JSON post body for POST /venues/checkins
     */
    private RequestBody getCheckInsBody(JSONArray checkIns) {
        Map<String, Object> map = new HashMap<>();
        map.put("checkIns", checkIns);
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new JSONObject(map).toString());
    }

    @Override
    int getContentViewId() {
        return R.layout.activity_check_ins;
    }

    @Override
    int getNavigationMenuItem() {
        return R.id.navigation_checkIns;
    }
}
