package com.teammike.iptk.foursquare.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.ViewHolder;
import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.adapters.CommentsAdapter;
import com.teammike.iptk.foursquare.utils.Api;
import com.teammike.iptk.foursquare.utils.AppHandler;
import com.teammike.iptk.foursquare.utils.ClientErrorHandler;
import com.teammike.iptk.foursquare.utils.Constants;
import com.teammike.iptk.foursquare.utils.RestClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Time;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
 * This activity is responsible for displaying detailed information about a selected venue.
 * @author Yadullah Duman
 */
public class VenueOverviewActivity extends AppCompatActivity {

    private static final String TAG = "VenueOverviewActivity";
    private JSONObject venue;
    private String venueId;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.venue_overview_address) TextView venueAddress;
    @BindView(R.id.venue_overview_price) TextView venuePrice;
    @BindView(R.id.venue_overview_hours) TextView venueHours;
    @BindView(R.id.venue_overview_category) TextView venueCategory;
    @BindView(R.id.venue_overview_contact) TextView venueContact;
    @BindView(R.id.venue_overview_rating) TextView venueRating;
    @BindView(R.id.venue_overview_checkins) TextView venueCheckins;
    @BindView(R.id.venue_overview_checkin_king) TextView venueKing;
    @BindView(R.id.comments_container) ListView commentsList;
    @BindView(R.id.checkInFab) FloatingActionButton checkInButton;
    @BindView(R.id.rateFab) FloatingActionButton ratingButton;
    @BindView(R.id.venue_overview_last_checkin) TextView lastCheckInTimeStamp;
    @BindString(R.string.error_invalid_venue_request) String invalidRequestMsg;
    @BindString(R.string.error_invalid_venue) String invalidVenueMsg;
    @BindString(R.string.error_internal_server_error) String internalServerErrorMsg;
    @BindString(R.string.error_invalid_user) String invalidUserMsg;
    @BindString(R.string.toast_check_in_warning_msg) String checkInWarningMsg;
    @BindString(R.string.toast_check_in_success_msg) String checkInSuccessMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venue_overview);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setUpView();
        setListeners();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.venue_overview_settings, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_venue_img_album) {
            ArrayList<String> imagePaths = getImagePaths(venue);
            startActivity(new Intent(this, VenuePhotoAlbumActivity.class).putStringArrayListExtra(Constants.INTENT_KEY, imagePaths));
        }

        if (item.getItemId() == R.id.action_venue_comment) {
            finish();
            startActivity(new Intent(this, CommentActivity.class).putExtra(Constants.INTENT_KEY, venue.toString()));
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * helper to fill the view with data
     */
    private void setUpView() {
        try {
            JSONObject venue = new JSONObject(getVenueData());
            this.venue = venue;
            this.venueId = venue.getString("_id");

            setTitle(venue.getString("name"));
            setVenueDetails(venue);
            setComments(venue.getJSONArray("comments"));
            setVenueKing();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * helper to set listeners for the rating and check-in button
     */
    private void setListeners() {
        ratingButton.setOnClickListener(v -> {
            checkIfUserRatedVenue();
        });

        checkInButton.setOnClickListener(v -> {
            try {
                if (canCheckIn()) {
                    int checkInsCount = venue.getInt("checkIns") + 1;
                    updateCheckIns(checkInsCount);
                } else {
                    Toast.makeText(this, checkInWarningMsg, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * checks if user is allowed to check in to a certain venue.
     * every user can check-in every six hours to a venue.
     * @return true, if user can check-in, otherwise false
     */
    private boolean canCheckIn() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        long lastCheckInTime = preferences.getLong(venueId, 0);

        if (lastCheckInTime == 0) {
            editPreferences(preferences, new Date().getTime());
            return true;
        } else {
            long currentTime = new Date().getTime();
            long diff = currentTime - lastCheckInTime;
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            Log.i(TAG, "Hours: " + String.valueOf(hours) + " -- Diff: " + String.valueOf(diff));

            if (hours >= 6) {
                editPreferences(preferences, 0);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * helper to edit shared preferences with timestamps for venue check-ins
     * @param preferences - the shared preferences on the device
     * @param lastCheckInTime - the last time the user checked in to a certain venue
     */
    private void editPreferences(SharedPreferences preferences, long lastCheckInTime) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(venueId, lastCheckInTime).apply();
    }

    /**
     * helper to get the venue king and display it
     */
    private void setVenueKing() {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.getVenueKing(venueId);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        String responseBody = response.body().string();
                        String username = new JSONObject(responseBody).getString("username");
                        String king = "Venue King: " + username;
                        venueKing.setText(king);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    int unicodeSadEmoji = 0x1F622;
                    String sadEmoji = getEmojiByUnicode(unicodeSadEmoji);
                    String toDisplay = "There is currently no venue king " + sadEmoji;
                    venueKing.setText(toDisplay);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(VenueOverviewActivity.this, internalServerErrorMsg, t);
            }
        });
    }

    private String getEmojiByUnicode(int unicode) {
        return new String(Character.toChars(unicode));
    }

    /**
     * helper to check if the user already rated this venue.
     */
    private void checkIfUserRatedVenue() {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.getUserForRating(venueId);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    // user did not rate the venue, show dialog for rating
                    displayRatingDialog();
                } else if (response.code() == 204) {
                    // user already rated, display toast to notify him
                    Toast.makeText(VenueOverviewActivity.this, "You already rated this venue.", Toast.LENGTH_SHORT).show();
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(VenueOverviewActivity.this, invalidRequestMsg);
                } else if (response.code() == 404) {
                    ClientErrorHandler.showMessage(VenueOverviewActivity.this, invalidVenueMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(VenueOverviewActivity.this, internalServerErrorMsg, t);
            }
        });
    }

    /**
     * helper to display rating dialog
     */
    private void displayRatingDialog() {
        DialogPlus dialog = DialogPlus.newDialog(VenueOverviewActivity.this)
                .setHeader(R.layout.rating_header)
                .setFooter(R.layout.rating_footer)
                .setContentHolder(new ViewHolder(R.layout.rating_content))
                .setGravity(Gravity.CENTER)
                .create();
        RatingBar ratingBar = dialog.getHolderView().findViewById(R.id.ratingBar);
        Button rateButton = dialog.getFooterView().findViewById(R.id.rating_dialog_button);
        rateButton.setOnClickListener(v -> rateVenue(buildRatingPostBody(ratingBar.getRating())));
        dialog.show();
    }

    /**
     * gets all images for the venue
     * @param venue - JSON of the current venue
     * @return list of all image paths for the venue
     */
    private ArrayList<String> getImagePaths(JSONObject venue) {
        ArrayList<String> allPhotos = new ArrayList<>();
        try {
            JSONArray photos = venue.getJSONArray("photos");

            for (int i = 0; i < photos.length(); i++) {
                JSONObject photo = photos.getJSONObject(i);

                if (!photo.has("prefix"))
                    allPhotos.add(Constants.BASE_URL_STATIC_DEV + photo.getString("suffix"));
                else
                    allPhotos.add(photo.getString("prefix") + "717x959" + photo.getString("suffix"));
            }

            return allPhotos;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * calling REST endpoint for updating check-ins
     * @param updatedCheckInsCount - updated count of check-ins
     */
    private void updateCheckIns(int updatedCheckInsCount) {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.updateCheckIns(venueId, buildCheckInsPostBody(updatedCheckInsCount));

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        String responseBody = response.body().string();
                        updateCheckInsOfUser(venueId, responseBody);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(VenueOverviewActivity.this, invalidRequestMsg);
                } else if (response.code() == 404) {
                    ClientErrorHandler.showMessage(VenueOverviewActivity.this, invalidVenueMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getApplicationContext(), internalServerErrorMsg, t);
            }
        });
    }

    /**
     * calling REST endpoint to update the check-ins of the user
     * @param venueId - the id of the venue
     * @param responseBody - the response from the server
     */
    private void updateCheckInsOfUser(String venueId, String responseBody) {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.updateUserCheckIns(venueId);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    Toast.makeText(VenueOverviewActivity.this, checkInSuccessMsg, Toast.LENGTH_LONG).show();
                    finish();
                    startActivity(new Intent(VenueOverviewActivity.this, VenueOverviewActivity.class).putExtra(Constants.INTENT_KEY, responseBody));
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(VenueOverviewActivity.this, invalidRequestMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getApplicationContext(), internalServerErrorMsg, t);
            }
        });
    }

    /**
     * calling REST endpoint to rate the venue
     * @param postBody - post body for server
     */
    private void rateVenue(RequestBody postBody) {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.postRating(venueId, postBody);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        String responseBody = response.body().string();
                        finish();
                        startActivity(new Intent(VenueOverviewActivity.this, VenueOverviewActivity.class).putExtra(Constants.INTENT_KEY, responseBody));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(VenueOverviewActivity.this, invalidRequestMsg);
                } else if (response.code() == 404) {
                    ClientErrorHandler.showMessage(VenueOverviewActivity.this, invalidVenueMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getApplicationContext(), internalServerErrorMsg, t);
            }
        });
    }

    /**
     * helper to build post body for rating request
     * @param rating - the rating value
     * @return JSON body for rating request
     */
    private RequestBody buildRatingPostBody(float rating) {
        Map<String, Object> map = new HashMap<>();
        map.put("rating", rating);
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new JSONObject(map).toString());
    }

    /**
     * helper to build post body for check-in request
     * @param checkIns - check-ins count
     * @return JSON body for check-in request
     */
    private RequestBody buildCheckInsPostBody(int checkIns) {
        Map<String, Object> map = new HashMap<>();
        map.put("checkIns", checkIns);
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new JSONObject(map).toString());
    }

    /**
     * helper to fill the {@link CommentsAdapter}
     * @param comments
     */
    private void setComments(JSONArray comments) {
        List<JSONObject> allComments = getComments(comments);
        CommentsAdapter adapter = new CommentsAdapter(allComments, this, venueId);
        commentsList.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    /**
     * helper to fill the details of the venue with data
     * @param venue - the current venue
     */
    private void setVenueDetails(JSONObject venue) throws JSONException {
        venueAddress.setText(venue.getJSONArray("formattedAddress").getString(0));
        venuePrice.setText(getPrice(venue));
        venueHours.setText(getHours(venue));
        venueCategory.setText(venue.getJSONArray("categories").getJSONObject(0).getString("name"));
        venueContact.setText(getPhone(venue));
        venueRating.setText(getRating(venue));
        venueCheckins.setText(getCheckIns(venue));
        lastCheckInTimeStamp.setText(getLastCheckInTimestamp(venue));
    }

    /**
     * helper to extract last check-in time returned by the server and display it on the view
     * @param venue - current venue responded by the server
     * @return string that displays the last check-in by a user
     */
    private String getLastCheckInTimestamp(JSONObject venue) throws JSONException {
        if (venue.has("lastCheckInUsername") && venue.has("lastCheckInTimestamp"))
            return "Last check-in: " + venue.getString("lastCheckInUsername") + " on " + venue.getString("lastCheckInTimestamp");
        else
            return null;
    }

    /**
     * helper to get check-ins count for the venue and build the text for display
     * @param venue - the current venue
     * @return text to display for check-ins
     */
    private String getCheckIns(JSONObject venue) throws JSONException {
        return "Total check-ins: " + String.valueOf(venue.getInt("checkIns"));
    }

    /**
     * helper to fetch all comments for the venue
     * @param comments - all comments for the venue
     * @return list of all comments for the venue
     */
    private List<JSONObject> getComments(JSONArray comments) {
        try {
            List<JSONObject> allComments = new ArrayList<>();

            for (int i = 0; i < comments.length(); i++)
                allComments.add(comments.getJSONObject(i));

            return allComments;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    /**
     * helper to get the rating from the venue and set the text to display
     * @param venue - the current venue
     * @return the text to display for rating
     */
    private String getRating(JSONObject venue) throws JSONException {
        JSONArray ratings = venue.getJSONArray("ratings");

        if (ratings.length() == 0) {
            int unicodeSadEmoji = 0x1F622;
            String sadEmoji = getEmojiByUnicode(unicodeSadEmoji);
            return "There is currently no rating " + sadEmoji;
        }

        double ratingSum = 0;
        for (int i = 0; i < ratings.length(); i++) {
            ratingSum += ratings.getJSONObject(i).getDouble("rating");
        }

        double calculatedRating = ratingSum / ratings.length();
        DecimalFormat df = new DecimalFormat("#.##");

        return "Average rating: " + df.format(calculatedRating) + " of 5";
    }

    /**
     * helper to get the phone number from the venue and set the text to display
     * @param venue - the current venue
     * @return the text to display for the phone number
     */
    private String getPhone(JSONObject venue) throws JSONException {
        if (venue.has("phone"))
            return "Phone: " + venue.getString("phone");
        else
            return "";
    }

    /**
     * helper to get the price for the venue
     * @param venue - the current venue
     * @return the text to display for price
     */
    private String getPrice(JSONObject venue) throws JSONException {
        String price = null;
        if (venue.has("priceMessage"))
            price = venue.getString("priceMessage");

        if (price != null)
            return "Price: " + price;

        return null;
    }

    /**
     * helper to get the hours of the venue
     * @param venue - the current venue
     * @return the text to display for hours
     */
    private String getHours(JSONObject venue) throws JSONException {
        String hours = null;

        if (venue.has("isOpen")) {
            if (venue.getBoolean("isOpen"))
                hours = "Currently open";
            else
                hours = "Currently closed";
        }

        if (hours != null)
            return hours;

        return null;
    }

    /**
     * helper to get the data from {@link VenuesActivity}
     * @return JSON of the current venue as string
     */
    private String getVenueData() {
        Intent intent = getIntent();
        return intent.getStringExtra(Constants.INTENT_KEY);
    }
}
