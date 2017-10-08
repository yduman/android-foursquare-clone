package com.teammike.iptk.foursquare.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.ipaulpro.afilechooser.utils.FileUtils;
import com.squareup.picasso.Picasso;
import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.utils.Api;
import com.teammike.iptk.foursquare.utils.AppHandler;
import com.teammike.iptk.foursquare.utils.ClientErrorHandler;
import com.teammike.iptk.foursquare.utils.Constants;
import com.teammike.iptk.foursquare.utils.RestClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Inside of the activity the user can post a comment for a specific venue.
 * @author Yadullah Duman
 */
public class CommentActivity extends AppCompatActivity {

    private static final String TAG = "CommentActivtiy";
    private String venue, venueId, imgPathSuffix;
    private int PICK_IMAGE_FROM_GALLERY_REQUEST = 1;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.comment_text) EditText commentary;
    @BindView(R.id.comment_post_image_button) ImageButton addImageButton;
    @BindView(R.id.comment_image_container) ImageView imageContainer;
    @BindString(R.string.error_internal_server_error) String internalServerErrorMsg;
    @BindString(R.string.error_invalid_venue_request) String invalidRequestMsg;
    @BindString(R.string.error_invalid_venue) String invalidVenueMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        imgPathSuffix = null;
        setVenue();
        setListeners();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        finish();
        startActivity(new Intent(this, VenueOverviewActivity.class).putExtra(Constants.INTENT_KEY, venue));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.comment_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_comment) {
            postComment();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_FROM_GALLERY_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            String path = FileUtils.getPath(this, uri);
            File file = new File(path);
            uploadImage(file, path);
        }
    }

    /**
     * set listener for add image button
     */
    private void setListeners() {
        addImageButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_FROM_GALLERY_REQUEST);
        });
    }

    /**
     * get the venue and init the id
     */
    private void setVenue() {
        try {
            JSONObject venue = new JSONObject(getVenueData());
            this.venue = venue.toString();
            venueId = venue.getString("_id");
            setTitle("Comment on " + venue.getString("name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * get venue data sent by @{@link VenueOverviewActivity}
     * @return JSON of venue as string
     */
    private String getVenueData() {
        Intent intent = getIntent();
        return intent.getStringExtra(Constants.INTENT_KEY);
    }

    /**
     * post comment for the venue
     */
    private void postComment() {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.postComment(venueId, buildCommentPostBody());

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        String responseBody = response.body().string();

                        Log.i(TAG, responseBody);
                        Log.i("RES_MSG", response.message());

                        finish();
                        startActivity(new Intent(CommentActivity.this, VenueOverviewActivity.class).putExtra(Constants.INTENT_KEY, responseBody));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(CommentActivity.this, invalidRequestMsg);
                } else if (response.code() == 404) {
                    ClientErrorHandler.showMessage(CommentActivity.this, invalidVenueMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getApplicationContext(), internalServerErrorMsg, t);
            }
        });
    }

    /**
     * upload image for the attached to comment
     * @param file - the image file from the device
     * @param path - the image path from the device
     */
    private void uploadImage(File file, String path) {
        RestClient client = Api.getInstance().getClient();

        MultipartBody.Part body = MultipartBody.Part.createFormData(
                "venue",
                file.getName(),
                RequestBody.create(MediaType.parse("image/*"), file)
        );

        Call<ResponseBody> call = client.uploadCommentImage(venueId, body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    Toast.makeText(CommentActivity.this, "Successfully uploaded image", Toast.LENGTH_SHORT).show();
                    try {
                        imgPathSuffix = response.body().string();
                        Picasso.with(CommentActivity.this).load(Constants.BASE_URL_STATIC_DEV + imgPathSuffix).into(imageContainer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(CommentActivity.this, invalidRequestMsg);
                } else if (response.code() == 404) {
                    ClientErrorHandler.showMessage(CommentActivity.this, invalidVenueMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(CommentActivity.this, internalServerErrorMsg, t);
            }
        });
    }

    /**
     * builds the JSON body when posting a comment
     * @return JSON for post comment
     */
    private RequestBody buildCommentPostBody() {
        Map<String, Object> map = new HashMap<>();
        map.put("text", commentary.getText().toString());
        if (imgPathSuffix != null)
            map.put("image", imgPathSuffix);
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new JSONObject(map).toString());
    }

}
