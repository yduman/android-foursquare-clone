package com.teammike.iptk.foursquare.fragments;


import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ipaulpro.afilechooser.utils.FileUtils;
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

import java.io.File;
import java.io.IOException;

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
 * This fragment displays the profile of the logged in user.
 * @author Yadullah Duman
 */
public class ProfileFragment extends Fragment {

    private static final int PERMISSIONS_REQUEST = 100;
    private int PICK_IMAGE_FROM_GALLERY_REQUEST = 1;
    private static final String TAG = "ProfileFragment";

    @BindView(R.id.profile_username_view) TextView username;
    @BindView(R.id.profile_email_view) TextView email;
    @BindView(R.id.profile_realname_view) TextView realname;
    @BindView(R.id.profile_age_view) TextView age;
    @BindView(R.id.profile_domicile_view) TextView domicile;
    @BindView(R.id.profile_picture_button) ImageView profileImage;
    @BindString(R.string.error_profile_picture) String errorMsgProfilePicture;
    @BindString(R.string.error_internal_server_error) String internalServerErrorMsg;
    @BindString(R.string.error_not_authenticated) String invalidAuthenticationMsg;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_view, container, false);
        ButterKnife.bind(this, view);

        checkPermissions();
        setListeners();

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String bundleExtra = getArguments().getString(Constants.INTENT_KEY);
        readUser(bundleExtra);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_FROM_GALLERY_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            String path = FileUtils.getPath(getContext(), uri);
            File file = new File(path);
            uploadProfileImage(file);
        }
    }

    /**
     * helper to set all required listeners
     */
    private void setListeners() {
        profileImage.setClipToOutline(true);
        profileImage.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_FROM_GALLERY_REQUEST);
        });
    }

    /**
     * multipart request to server for uploading an image
     * @param file - the image file
     */
    private void uploadProfileImage(File file) {
        RestClient client = Api.getInstance().getClient();

        MultipartBody.Part body = MultipartBody.Part.createFormData(
                "avatar",
                file.getName(),
                RequestBody.create(MediaType.parse("image/*"), file)
        );

        Call<ResponseBody> call = client.uploadProfileImage(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        String imagePath = response.body().string();
                        setImageWithPicasso(imagePath);
                        reloadActivity();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(getContext(), errorMsgProfilePicture);
                    setDefaultImage();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getContext(), internalServerErrorMsg, t);
            }
        });
    }

    private void reloadActivity() {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.getUser();

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        String responseBody = response.body().string();
                        startActivity(new Intent(getContext(), ProfileActivity.class).putExtra(Constants.INTENT_KEY, responseBody));
                    } catch (IOException e) {
                        Log.e(TAG, "Couldn't fetch body from response!");
                    }
                } else if (response.code() == 401) {
                    ClientErrorHandler.showMessage(getContext(), invalidAuthenticationMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getContext(), internalServerErrorMsg, t);
            }
        });
    }

    /**
     * helper to get the user data
     */
    private void readUser(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            String fullname = json.getString("fullname");
            username.setText(json.getString("username"));
            email.setText(json.getString("email"));
            realname.setText(fullname);
            age.setText(String.valueOf(json.getInt("age")));
            domicile.setText(json.getString("domicile"));
            if (!json.getString("imagePath").equals("no image"))
                setImageWithPicasso(json.getString("imagePath"));
            else
                setDefaultImage();
            setActivityTitle(fullname);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setActivityTitle(String realname) {
        getActivity().setTitle(realname);
    }

    public void setImageWithPicasso(String imagePath) {
        Picasso.with(getContext()).load(Constants.BASE_URL_STATIC_DEV + imagePath).into(profileImage);
    }

    public void setDefaultImage() {
        profileImage.setImageResource(R.drawable.ic_sentiment_satisfied_black_24dp);
    }

    /**
     * helper to ask for permission to read files
     */
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
        }
    }
}
