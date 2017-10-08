package com.teammike.iptk.foursquare.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.activities.ProfileActivity;
import com.teammike.iptk.foursquare.utils.AppHandler;
import com.teammike.iptk.foursquare.utils.ClientErrorHandler;
import com.teammike.iptk.foursquare.utils.Constants;
import com.teammike.iptk.foursquare.utils.RestClient;
import com.teammike.iptk.foursquare.utils.Api;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
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
 * This fragment displays the view for editing a profile
 * @author Yadullah Duman
 */
public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFragment";

    @BindView(R.id.edit_profile_email) EditText email;
    @BindView(R.id.edit_profile_username) EditText username;
    @BindView(R.id.edit_profile_first_name) EditText firstname;
    @BindView(R.id.edit_profile_last_name) EditText lastname;
    @BindView(R.id.edit_profile_age) EditText age;
    @BindView(R.id.edit_profile_domicile) EditText domicile;
    @BindView(R.id.edit_profile_button) Button editButton;
    @BindString(R.string.error_field_required) String fieldRequiredMsg;
    @BindString(R.string.error_invalid_email) String invalidEmail;
    @BindString(R.string.error_invalid_registration) String invalidEdit;
    @BindString(R.string.error_invalid_user) String invalidUser;
    @BindString(R.string.error_internal_server_error) String internalServerErrorMsg;
    @BindString(R.string.action_edit_profile) String editProfileString;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        ButterKnife.bind(this, view);

        setListeners();

        return view;
    }

    /**
     * helper to set required listeners
     */
    private void setListeners() {
        editButton.setOnClickListener(v -> attemptEdit());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String bundleExtra = getArguments().getString(Constants.INTENT_KEY);
        readUser(bundleExtra);
        getActivity().setTitle(editProfileString);
    }

    /**
     * reads the user from objectbox and pre-populates the fields
     */
    private void readUser(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            email.setText(json.getString("email"));
            username.setText(json.getString("username"));
            firstname.setText(json.getString("firstname"));
            lastname.setText(json.getString("lastname"));
            age.setText(String.valueOf(json.getInt("age")));
            domicile.setText(json.getString("domicile"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * checks if all fields fulfill the requirements.
     * if the requirements are fulfilled, the client sends the request to the server
     * otherwise the user gets a notified that certain fields do not fulfill the requirements.
     */
    private void attemptEdit() {
        // Reset errors.
        email.setError(null);
        username.setError(null);
        firstname.setError(null);
        lastname.setError(null);
        age.setError(null);
        domicile.setError(null);

        // Store values at the time of the request.
        String email = this.email.getText().toString();
        String username = this.username.getText().toString();
        String firstname = this.firstname.getText().toString();
        String lastname = this.lastname.getText().toString();
        String age = this.age.getText().toString();
        String domicile = this.domicile.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (isValueEmptyFor(email)) {
            displayFieldRequiredErrorMsgFor(this.email);
            focusView = this.email;
            cancel = true;
        } else if (!isEmailValid(email)) {
            this.email.setError(invalidEmail);
            focusView = this.email;
            cancel = true;
        }

        // Check that other fields are not empty
        if (isValueEmptyFor(username)) {
            displayFieldRequiredErrorMsgFor(this.username);
            focusView = this.username;
            cancel = true;
        } else if (isValueEmptyFor(firstname)) {
            displayFieldRequiredErrorMsgFor(this.firstname);
            focusView = this.firstname;
            cancel = true;
        } else if (isValueEmptyFor(lastname)) {
            displayFieldRequiredErrorMsgFor(this.lastname);
            focusView = this.lastname;
            cancel = true;
        } else if (isValueEmptyFor(age)) {
            displayFieldRequiredErrorMsgFor(this.age);
            focusView = this.age;
            cancel = true;
        } else if (isValueEmptyFor(domicile)) {
            displayFieldRequiredErrorMsgFor(this.domicile);
            focusView = this.domicile;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            editProfile();
        }
    }

    /**
     * sends edit profile request to server
     */
    private void editProfile() {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.updateUserById(AppHandler.getId(), getInputFromClient());

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        String responseBody = response.body().string();

                        Log.i(TAG, responseBody);
                        Log.i("RES_MSG", response.message());

                        startActivity(new Intent(getContext(), ProfileActivity.class).putExtra(Constants.INTENT_KEY, responseBody));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(getContext(), invalidEdit);
                } else if (response.code() == 404) {
                    ClientErrorHandler.showMessage(getContext(), invalidUser);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getContext(), internalServerErrorMsg, t);
            }
        });
    }

    /**
     * reads the input from the client and creates a JSON for the request
     * @return @{@link RequestBody} object representing JSON
     */
    private RequestBody getInputFromClient() {
        Map<String, String> map = new HashMap<>();
        map.put("email", email.getText().toString());
        map.put("username", username.getText().toString());
        map.put("firstname", firstname.getText().toString());
        map.put("lastname", lastname.getText().toString());
        map.put("age", age.getText().toString());
        map.put("domicile", domicile.getText().toString());
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new JSONObject(map).toString());
    }

    /**
     * checks if the type email address is valid
     * @param email - email address typed in by the user
     * @return true, if our string contains the @ symbol, otherwise false
     */
    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    /**
     * helper method to check if a field is left empty
     * @param value - the value inside of the field
     * @return true, if value is empty, otherwise false
     */
    private boolean isValueEmptyFor(String value) {
        return TextUtils.isEmpty(value);
    }

    /**
     * helper method to display an error messagen for required fields
     * @param textField - the text fields of the activity
     */
    private void displayFieldRequiredErrorMsgFor(EditText textField) {
        textField.setError(fieldRequiredMsg);
    }
}
