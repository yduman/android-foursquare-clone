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
import com.teammike.iptk.foursquare.activities.LoginActivity;
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
 * This fragment displays the view for resetting a password when logged in.
 * @author Yadullah Duman
 */
public class ResetPasswordFragment extends Fragment {

    private static final String TAG = "ResetPasswordFragment";

    @BindView(R.id.text_reset_password_new) EditText newPassword;
    @BindView(R.id.text_reset_password_new_confirmed) EditText confirmedPassword;
    @BindView(R.id.reset_password_profile_button) Button resetButton;
    @BindString(R.string.error_field_required) String fieldRequiredMsg;
    @BindString(R.string.error_invalid_password) String invalidPassword;
    @BindString(R.string.error_invalid_confirmed_password) String invalidConfirmedPassword;
    @BindString(R.string.error_invalid_user) String invalidUser;
    @BindString(R.string.error_invalid_reset_password) String invalidResetting;
    @BindString(R.string.error_internal_server_error) String internalServerErrorMsg;
    @BindString(R.string.action_reset_password) String fragmentTitle;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reset_password, container, false);
        ButterKnife.bind(this, view);

        setListeners();
        getActivity().setTitle(fragmentTitle);

        return view;
    }

    private void setListeners() {
        resetButton.setOnClickListener(v -> attemptReset());
    }

    private void attemptReset() {
        // Reset errors
        newPassword.setError(null);
        confirmedPassword.setError(null);

        // Store values at the time of the request
        String newPassword = this.newPassword.getText().toString();
        String confirmedPassword = this.confirmedPassword.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for valid input for new password
        if (isValueEmptyFor(newPassword)) {
            displayFieldRequiredErrorMsgFor(this.newPassword);
            focusView = this.newPassword;
            cancel = true;
        } else if (!isValidPassword(newPassword)) {
            this.newPassword.setError(invalidPassword);
            focusView = this.newPassword;
            cancel = true;
        }

        // Check for valid input for confirmed password
        if (isValueEmptyFor(confirmedPassword)) {
            displayFieldRequiredErrorMsgFor(this.confirmedPassword);
            focusView = this.confirmedPassword;
            cancel = true;
        } else if (!isValidPassword(confirmedPassword)) {
            this.confirmedPassword.setError(invalidPassword);
            focusView = this.confirmedPassword;
            cancel = true;
        }

        // Check that both inputs are equal
        if (!newPassword.equals(confirmedPassword)) {
            this.confirmedPassword.setError(invalidConfirmedPassword);
            focusView = this.confirmedPassword;
            cancel = true;
        }

        if (cancel)
            focusView.requestFocus();
        else
            resetPassword();
    }

    private void resetPassword() {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.updateUserById(AppHandler.getId(), getInputFromClient());

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200)
                    startActivity(new Intent(getActivity(), LoginActivity.class).putExtra(Constants.ACTION_KEY, Constants.ACTION_RESET_PASSWORD_VALUE));
                else if (response.code() == 400)
                    ClientErrorHandler.showMessage(getContext(), invalidResetting);
                else if (response.code() == 404)
                    ClientErrorHandler.showMessage(getContext(), invalidUser);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getContext(), internalServerErrorMsg, t);
            }
        });
    }

    private RequestBody getInputFromClient() {
        Map<String, String> map = new HashMap<>();
        map.put("password", newPassword.getText().toString());
        map.put("action", "resetPassword");
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new JSONObject(map).toString());
    }

    private boolean isValueEmptyFor(String value) {
        return TextUtils.isEmpty(value);
    }

    private void displayFieldRequiredErrorMsgFor(EditText textField) {
        textField.setError(fieldRequiredMsg);
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 6;
    }

}
