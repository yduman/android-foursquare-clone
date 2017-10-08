package com.teammike.iptk.foursquare.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.utils.AppHandler;
import com.teammike.iptk.foursquare.utils.ClientErrorHandler;
import com.teammike.iptk.foursquare.utils.RestClient;
import com.teammike.iptk.foursquare.utils.Constants;
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
 * This activity is called, when the user is not logged in and forgot his password
 * @author Yadullah Duman
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";

    @BindView(R.id.text_forgot_password_email) EditText email;
    @BindView(R.id.text_forgot_password_password) EditText password;
    @BindView(R.id.text_forgot_password_password_confirmed) EditText confirmedPassword;
    @BindView(R.id.text_forgot_password_security_question_1) EditText securityQuestion1;
    @BindView(R.id.text_forgot_password_security_question_2) EditText securityQuestion2;
    @BindView(R.id.forgot_password_send_button) Button sendButton;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindString(R.string.error_field_required) String fieldRequiredMsg;
    @BindString(R.string.error_invalid_email) String invalidEmailMsg;
    @BindString(R.string.error_invalid_password) String invalidPasswordMsg;
    @BindString(R.string.error_invalid_confirmed_password) String invalidConfirmedPasswordMsg;
    @BindString(R.string.error_invalid_reset_password) String invalidResettingMsg;
    @BindString(R.string.error_invalid_credentials) String invalidCredentialsMsg;
    @BindString(R.string.error_internal_server_error) String internalServerErrorMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        sendButton.setOnClickListener(view -> attemptSend());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * checks if all fields fulfill the requirements.
     * if the requirements are fulfilled, the client sends the request to the server
     * otherwise the user gets a notified that certain fields do not fulfill the requirements.
     */
    private void attemptSend() {
        // Reset errors
        email.setError(null);
        password.setError(null);
        confirmedPassword.setError(null);
        securityQuestion1.setError(null);
        securityQuestion2.setError(null);

        // Store values at the time of the request
        String email = this.email.getText().toString();
        String password = this.password.getText().toString();
        String confirmedPassword = this.confirmedPassword.getText().toString();
        String securityQuestion1 = this.securityQuestion1.getText().toString();
        String securityQuestion2 = this.securityQuestion2.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for valid input for email
        if (isValueEmptyFor(email)) {
            displayFieldRequiredErrorMsgFor(this.email);
            focusView = this.email;
            cancel = true;
        } else if (!isEmailValid(email)) {
            this.email.setError(invalidEmailMsg);
            focusView = this.email;
            cancel = true;
        }

        // Check for securtiy question 1
        if (isValueEmptyFor(securityQuestion1)) {
            displayFieldRequiredErrorMsgFor(this.securityQuestion1);
            focusView = this.securityQuestion1;
            cancel = true;
        }

        // Check for securtiy question 2
        if (isValueEmptyFor(securityQuestion2)) {
            displayFieldRequiredErrorMsgFor(this.securityQuestion2);
            focusView = this.securityQuestion2;
            cancel = true;
        }

        // Check for password
        if (isValueEmptyFor(password)) {
            displayFieldRequiredErrorMsgFor(this.password);
            focusView = this.password;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            this.password.setError(invalidPasswordMsg);
            focusView = this.password;
            cancel = true;
        }

        // Check for confirmed password
        if (isValueEmptyFor(confirmedPassword)) {
            displayFieldRequiredErrorMsgFor(this.confirmedPassword);
            focusView = this.confirmedPassword;
            cancel = true;
        } else if (!isPasswordValid(confirmedPassword)) {
            this.confirmedPassword.setError(invalidPasswordMsg);
            focusView = this.confirmedPassword;
            cancel = true;
        }

        // Check that both password inputs are equal
        if (!password.equals(confirmedPassword)) {
            this.confirmedPassword.setError(invalidConfirmedPasswordMsg);
            focusView = this.confirmedPassword;
            cancel = true;
        }

        if (cancel)
            focusView.requestFocus();
        else
            resetPassword();
    }

    /**
     * call REST endpoint to reset password
     */
    private void resetPassword() {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.updateUser(getInputFromClient());

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200)
                    startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class).putExtra(Constants.ACTION_KEY, Constants.ACTION_RESET_PASSWORD_VALUE));
                else if (response.code() == 400)
                    ClientErrorHandler.showMessage(ForgotPasswordActivity.this, invalidResettingMsg);
                else if (response.code() == 404)
                    ClientErrorHandler.showMessage(ForgotPasswordActivity.this, invalidCredentialsMsg);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getApplicationContext(), internalServerErrorMsg, t);
            }
        });
    }

    /**
     * retrieving all input data from the client so that we can pass this to the server.
     * @return a Retrofit RequestBody object that is actually just json.
     */
    private RequestBody getInputFromClient() {
        Map<String, Object> map = new HashMap<>();
        map.put("email", email.getText().toString());
        map.put("password", password.getText().toString());
        map.put("secQuestion1", securityQuestion1.getText().toString());
        map.put("secQuestion2", securityQuestion2.getText().toString());
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
     * checks if the password typed in by the user is greater than or equal to six
     * @param password - password typed in by the user
     * @return true, if the password is greater than or equal to six, otherwise false
     */
    private boolean isPasswordValid(String password) {
        return password.length() >= 6;
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

