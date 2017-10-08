package com.teammike.iptk.foursquare.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.utils.Api;
import com.teammike.iptk.foursquare.utils.AppHandler;
import com.teammike.iptk.foursquare.utils.ClientErrorHandler;
import com.teammike.iptk.foursquare.utils.Constants;
import com.teammike.iptk.foursquare.utils.RestClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VenuesMapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static String TAG = "VenuesMapsActivity";

    private String response;
    private double lat, lng;
    private Map<Marker, String> mapData = new HashMap<>();

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.venues_map) MapView mapView;
    @BindString(R.string.your_position) String yourPosition;
    @BindString(R.string.error_invalid_venue_request) String invalidRequestMsg;
    @BindString(R.string.error_invalid_venue) String invalidVenueMsg;
    @BindString(R.string.error_internal_server_error) String internalServerErrorMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venues_maps);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Bundle extras = getIntentExtra();
        response = extras.getString(Constants.INTENT_KEY);
        double[] locationExtra = extras.getDoubleArray(Constants.LOCATION_KEY);
        lat = locationExtra != null ? locationExtra[0] : 0;
        lng = locationExtra != null ? locationExtra[1] : 0;

        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        mapView.getMapAsync(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.venues_maps, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_list)
            startActivity(new Intent(this, VenuesActivity.class).putExtras(getIntentExtra()));

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        map.getUiSettings().setAllGesturesEnabled(true);
        map.setOnInfoWindowClickListener(marker -> showVenue(mapData.get(marker)));
        map.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lng))
                .title(yourPosition)
                .icon(getBitmapDescriptor(R.drawable.ic_person_pin_circle_black_24dp))
        );
        fillMapWithVenues(response, map);
        fillMapWithFriends(map);
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

                        startActivity(new Intent(VenuesMapsActivity.this, VenueOverviewActivity.class).putExtra(Constants.INTENT_KEY, responseBody));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(VenuesMapsActivity.this, invalidRequestMsg);
                } else if (response.code() == 404) {
                    ClientErrorHandler.showMessage(VenuesMapsActivity.this, invalidVenueMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(VenuesMapsActivity.this, internalServerErrorMsg, t);
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
     * helper to place all markers for venues on the map
     * @param response - the Foursquare API response
     * @param map - Google Maps
     */
    private void fillMapWithVenues(String response, GoogleMap map) {
        try {
            JSONObject json = new JSONObject(response);
            JSONArray items = json.getJSONObject("response").getJSONArray("groups").getJSONObject(0).getJSONArray("items");
            setTitle(String.valueOf(items.length()) + " Results");

            for (int i = 0; i < items.length(); i++)
            {
                JSONObject venue = items.getJSONObject(i).getJSONObject("venue");

                // retrieve data from json
                double venueLat = venue.getJSONObject("location").getDouble("lat");
                double venueLng = venue.getJSONObject("location").getDouble("lng");
                String venueName = venue.getString("name");
                String venueCategory = venue.getJSONArray("categories").getJSONObject(0).getString("shortName");

                Marker marker = map.addMarker(new MarkerOptions()
                        .position(new LatLng(venueLat, venueLng))
                        .title(venueName)
                        .snippet(venueCategory)
                );

                mapData.put(marker, venue.toString());
            }
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't access venues from Foursquare API response!");
        }
    }

    /**
     * helper to place all markers for friends on the map
     * @param map - Google Maps
     */
    private void fillMapWithFriends(GoogleMap map) {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.getFriendsLocation();

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        String jsonString = response.body().string();
                        Log.i(TAG, jsonString);
                        JSONObject res = new JSONObject(jsonString);
                        JSONArray locations = res.getJSONArray("locations");

                        for (int i = 0; i < locations.length(); i++) {
                            double lat = locations.getJSONObject(i).getDouble("lat");
                            double lng = locations.getJSONObject(i).getDouble("lng");
                            String username = locations.getJSONObject(i).getString("username");

                            map.addMarker(new MarkerOptions()
                                    .position(new LatLng(lat, lng))
                                    .title(username)
                                    .icon(getBitmapDescriptor(R.drawable.ic_friends_location_maps_black_24dp))
                            );
                        }

                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 15));
                    } catch (JSONException e) {
                        Log.e(TAG, "Couldn't access locations array of JSON response!");
                    } catch (IOException e) {
                        Log.e(TAG, "Couldn't access response body!");
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(VenuesMapsActivity.this, internalServerErrorMsg, t);
            }
        });
    }

    /**
     * helper to generate a BitmapDescriptor object from a drawable to have a custom marker on the map.
     * @param id - the drawable id
     * @return BitmapDescriptor from Drawable
     */
    private BitmapDescriptor getBitmapDescriptor(int id) {
        Drawable vectorDrawable = getDrawable(id);
        int height = vectorDrawable.getIntrinsicHeight();
        int width = vectorDrawable.getIntrinsicWidth();
        vectorDrawable.setBounds(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
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
