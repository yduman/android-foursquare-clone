package com.teammike.iptk.foursquare.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.utils.Api;
import com.teammike.iptk.foursquare.utils.AppHandler;
import com.teammike.iptk.foursquare.utils.ClientErrorHandler;
import com.teammike.iptk.foursquare.utils.Constants;
import com.teammike.iptk.foursquare.utils.RestClient;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
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
 * This activity is responsible for the home page.
 * Users can search for a venue by a search field or by selection specific categories.
 * @author Yadullah Duman
 */
public class HomeActivity extends BaseActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "HomeActivity";

    private GoogleApiClient googleApiClient;
    private FusedLocationProviderApi locationProvider = LocationServices.FusedLocationApi;
    private LocationRequest locationRequest;
    private double lat;
    private double lng;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 101;
    private boolean permissionIsGranted = false;
    private boolean isChecked = false;

    @BindView(R.id.different_location_checkbox) CheckBox differentLocationCheckbox;
    @BindView(R.id.searchView) SearchView searchView;
    @BindView(R.id.breakfast_button) ImageButton breakfastButton;
    @BindView(R.id.lunch_button) ImageButton lunchButton;
    @BindView(R.id.dinner_button) ImageButton dinnerButton;
    @BindView(R.id.coffee_button) ImageButton coffeeAndTeaButton;
    @BindView(R.id.nightlife_button) ImageButton nightlifeButton;
    @BindView(R.id.entertainment_button) ImageButton entertainmentButton;
    @BindString(R.string.info_location_permission) String infoLocationPermissions;
    @BindString(R.string.error_internal_server_error) String internalServerErrorMsg;
    @BindString(R.string.error_fetching_venues) String errorFetchingVenuesMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildGoogleApiClient();
        buildLocationRequest();

        setListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (permissionIsGranted && googleApiClient.isConnected())
            requestLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (permissionIsGranted)
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (permissionIsGranted)
            googleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection got suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, connectionResult.toString());
        Log.i(TAG, String.valueOf(connectionResult.getErrorCode()));
        Log.i(TAG, connectionResult.getErrorMessage());
    }

    @Override
    public void onLocationChanged(Location location) {
        lat = location.getLatitude();
        lng = location.getLongitude();
        Log.i(TAG, "LAT, LNG: " + formattedLocation(lat, lng));

        sendLocation(lat, lng);
    }

    /**
     * helper to format the location propery for logs and path params
     * @param lat - latitude of user
     * @param lng - longtitude of user
     * @return formatted location
     */
    private String formattedLocation(double lat, double lng) {
        return String.valueOf(lat) + ", " + String.valueOf(lng);
    }

    /**
     * helper to save location
     * @param lat - latitude of user
     * @param lng - longtitude of user
     */
    private void sendLocation(double lat, double lng) {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.postLocation(buildLocationPostBody(lat, lng));

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    Log.i(TAG, "Successfully saved user location");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getApplicationContext(), internalServerErrorMsg, t);
            }
        });
    }

    /**
     * helper to build post body for sendLocation method
     * @param lat - latitude of user
     * @param lng - longtitude of user
     * @return JSON post body for sendLocation method
     */
    private RequestBody buildLocationPostBody(double lat, double lng) {
        Map<String, Object> map = new HashMap<>();
        map.put("lat", lat);
        map.put("lng", lng);
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new JSONObject(map).toString());
    }

    /**
     * set required listeners for each category button
     */
    private void setListeners() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!isChecked) {
                    searchQueryRequest(query);
                } else {
                    try {
                        String segments[] = query.split(",");
                        String searchTerm = segments[0].trim();
                        String otherLocation = segments[1].trim();
                        if (!otherLocation.isEmpty())
                            searchQueryRequestForDifferentLocation(searchTerm, otherLocation);
                        else
                            searchQueryRequest(searchTerm);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Log.e(TAG, "Wrong query provided. Querying with default behaviour now...");
                        searchQueryRequest(query);
                    }
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
        breakfastButton.setOnClickListener(view -> searchQueryRequest("breakfast"));
        lunchButton.setOnClickListener(view -> searchQueryRequest("lunch"));
        dinnerButton.setOnClickListener(view -> searchQueryRequest("dinner"));
        coffeeAndTeaButton.setOnClickListener(view -> categoryRequest("coffee"));
        nightlifeButton.setOnClickListener(view -> categoryRequest("drinks"));
        entertainmentButton.setOnClickListener(view -> categoryRequest("arts"));
    }

    public void onCheckBoxClicked(View view) {
        isChecked = ((CheckBox) view).isChecked();
    }

    private void searchQueryRequestForDifferentLocation(String query, String location) {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.getVenuesByQuery(location, query);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        isChecked = false;
                        showVenueResultsFor(response, location);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(HomeActivity.this, errorFetchingVenuesMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getApplicationContext(), internalServerErrorMsg, t);
            }
        });
    }

    /**
     * helper to get all venues by query endpoint of Foursquare API
     * @param query - the query to search for
     */
    private void searchQueryRequest(String query) {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.getVenuesByQuery(formattedLocation(lat, lng), query);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        showVenueResultsFor(response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(HomeActivity.this, errorFetchingVenuesMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getApplicationContext(), internalServerErrorMsg, t);
            }
        });
    }

    /**
     * helper to get all venues by category endpoint of Foursquare API
     * @param category - the category to search for
     */
    private void categoryRequest(String category) {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.getVenuesByCategory(formattedLocation(lat, lng), category);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        showVenueResultsFor(response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(HomeActivity.this, errorFetchingVenuesMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getApplicationContext(), internalServerErrorMsg, t);
            }
        });
    }

    /**
     * helper to bundle the response of the Foursquare API and send it to {@link VenuesActivity}
     * @param response - the JSON response from the Foursquare API
     */
    private void showVenueResultsFor(Response<ResponseBody> response, String... location) throws IOException {
        String responseBody = response.body().string();

        Log.i(TAG, responseBody);
        Log.i("RES_MSG", response.message());

        Bundle extras = new Bundle();
        extras.putString(Constants.INTENT_KEY, responseBody);
        extras.putDoubleArray(Constants.LOCATION_KEY, new double[]{lat, lng});
        if (location != null && location.length != 0)
            extras.putString(Constants.DIFF_LOCATION_KEY, location[0]);

        startActivity(new Intent(HomeActivity.this, VenuesActivity.class).putExtras(extras));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionIsGranted = true;
                } else {
                    permissionIsGranted = false;
                    Toast.makeText(getApplicationContext(), infoLocationPermissions, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /**
     * helper method for getting location updates
     */
    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    /**
     * helper method for creating a @{@link GoogleApiClient} object
     */
    private void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    /**
     * helper method for creating a @{@link LocationRequest} object
     */
    private void buildLocationRequest() {
        locationRequest = new LocationRequest()
                .setInterval(45 * 1000)
                .setFastestInterval(30 * 1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    int getContentViewId() {
        return R.layout.activity_home;
    }

    @Override
    int getNavigationMenuItem() {
        return R.id.navigation_home;
    }
}
