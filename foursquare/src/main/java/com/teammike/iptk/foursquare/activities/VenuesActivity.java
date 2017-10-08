package com.teammike.iptk.foursquare.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.adapters.VenuesAdapter;
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
import java.util.Collections;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This activity is responsible for displaying results of searched venues.
 * @author Yadullah Duman
 */
public class VenuesActivity extends AppCompatActivity {

    private static final String TAG = "VenuesActivity";

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.venues_container) ListView venuesList;
    @BindString(R.string.error_invalid_venue_request) String invalidRequestMsg;
    @BindString(R.string.error_invalid_venue) String invalidVenueMsg;
    @BindString(R.string.error_internal_server_error) String internalServerErrorMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venues);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        String bundleExtra = getIntentExtra().getString(Constants.INTENT_KEY);
        fillListView(bundleExtra);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        finish();
        startActivity(new Intent(this, HomeActivity.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.venues_list, menu);

        // if the user searches for venues in another location, just provide list results
        if (getIntentExtra().getString(Constants.DIFF_LOCATION_KEY) != null) {
            MenuItem mapsIcon = menu.findItem(R.id.action_maps);
            mapsIcon.setVisible(false);
            invalidateOptionsMenu();
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_maps)
            startActivity(new Intent(this, VenuesMapsActivity.class).putExtras(getIntentExtra()));

        return super.onOptionsItemSelected(item);
    }

    /**
     * helper to populate adapter
     * @param bundleExtra - Foursquare API response
     */
    private void fillListView(String bundleExtra) {
        List<JSONObject> venues = getVenuesFromApiResponse(bundleExtra);

        setTitle(String.valueOf(venues.size()) + " Results");

        VenuesAdapter adapter = new VenuesAdapter(venues, this);
        venuesList.setAdapter(adapter);
        venuesList.setOnItemClickListener((adapterView, view, pos, id) -> showVenue(venues.get(pos).toString()));

        adapter.notifyDataSetChanged();
    }

    /**
     * helper to filter out the venues from the Foursquare API
     * @param response - Foursquare API response
     * @return list of all venues from the Foursquare API response
     */
    private List<JSONObject> getVenuesFromApiResponse(String response) {
        List<JSONObject> venues = new ArrayList<>();

        try {
            JSONObject json = new JSONObject(response);
            JSONArray items = json.getJSONObject("response").getJSONArray("groups").getJSONObject(0).getJSONArray("items");

            for (int i = 0; i < items.length(); i++) {
                JSONObject venue = items.getJSONObject(i).getJSONObject("venue");
                venues.add(venue);
            }

            return venues;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    /**
     * call REST endpoint to display detailed information about the selected venue
     * @param json - the json from the venue represented as string
     */
    private void showVenue(String json) {
        String id = getIdFrom(json);
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.getVenueById(id);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        String responseBody = response.body().string();

                        Log.i(TAG, responseBody);
                        Log.i("RES_MSG", response.message());

                        startActivity(new Intent(VenuesActivity.this, VenueOverviewActivity.class).putExtra(Constants.INTENT_KEY, responseBody));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(VenuesActivity.this, invalidRequestMsg);
                } else if (response.code() == 404) {
                    ClientErrorHandler.showMessage(VenuesActivity.this, invalidVenueMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(VenuesActivity.this, internalServerErrorMsg, t);
            }
        });
    }

    /**
     * helper to fetch the venue id from json
     * @param jsonString - the json of the venue represented as string
     * @return the venue id
     */
    private String getIdFrom(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            return json.getString("id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * helper method to get the response from the server and pass them to the fragments
     * @return String extra, which is the JSON returned from the server
     */
    private Bundle getIntentExtra() {
        Intent intent = getIntent();
        return intent.getExtras();
    }
}
