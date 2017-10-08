package com.teammike.iptk.foursquare.activities;

import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

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
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This activity is the entry point of the app.
 * The user can either log in or sign up or reset his password.
 * @author Yadullah Duman
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    @BindView(R.id.login_layout) LinearLayout loginLayout;
    @BindView(R.id.email) EditText email;
    @BindView(R.id.password) EditText password;
    @BindView(R.id.email_sign_in_button) Button signInButton;
    @BindView(R.id.register_button) Button registerButton;
    @BindView(R.id.forgot_password_button) Button forgotPasswordButton;
    @BindString(R.string.error_invalid_password) String invalidPasswordMessage;
    @BindString(R.string.error_invalid_email) String invalidEmailMessage;
    @BindString(R.string.error_invalid_login) String invalidLoginMessage;
    @BindString(R.string.error_field_required) String fieldRequiredMessage;
    @BindString(R.string.info_profile_deletion) String profileDeletedMessage;
    @BindString(R.string.info_profile_reset_password) String resetPasswordMessage;
    @BindString(R.string.error_internal_server_error) String internalServerErrorMsg;
    @BindString(R.string.info_successful_logout) String logoutMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        setListeners();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            displayInfoToast();
    }

    /**
     * helper method for setting required listeners
     */
    private void setListeners() {
        signInButton.setOnClickListener(view -> attemptLogin());
        registerButton.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        forgotPasswordButton.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    /**
     * looks for certain flags inside the json and displays Toast accordingly
     * the flags are: resetPassword, deleteProfile, logout
     */
    private void displayInfoToast() {
        String action = getIntent().getStringExtra(Constants.ACTION_KEY);
        if (action != null) {
            switch (action) {
                case Constants.ACTION_RESET_PASSWORD_VALUE:
                    Snackbar.make(loginLayout, resetPasswordMessage, Snackbar.LENGTH_LONG).show();
                    break;
                case Constants.ACTION_DELETE_PROFILE_VALUE:
                    Snackbar.make(loginLayout, profileDeletedMessage, Snackbar.LENGTH_LONG).show();
                    break;
                case Constants.ACTION_LOGOUT_VALUE:
                    Snackbar.make(loginLayout, logoutMessage, Snackbar.LENGTH_LONG).show();
                    break;
            }
        }
    }

    /**
     * helper method to get input from user
     * @return @{@link RequestBody} object representing JSON
     */
    private RequestBody getUserFromInput() {
        Map<String, Object> map = new HashMap<>();
        map.put("email", email.getText().toString());
        map.put("password", password.getText().toString());
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new JSONObject(map).toString());
    }

    /**
     * checks if all fields fulfill the requirements.
     * if the requirements are fulfilled, the client sends the request to the server
     * otherwise the user gets a notified that certain fields do not fulfill the requirements.
     */
    private void attemptLogin() {
        // Reset errors.
        email.setError(null);
        password.setError(null);

        // Store values at the time of the login attempt.
        String email = this.email.getText().toString();
        String password = this.password.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            this.password.setError(invalidPasswordMessage);
            focusView = this.password;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            this.email.setError(fieldRequiredMessage);
            focusView = this.email;
            cancel = true;
        } else if (!isEmailValid(email)) {
            this.email.setError(invalidEmailMessage);
            focusView = this.email;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            login();
        }
    }

    /**
     * helper method to send login request to the server
     */
    private void login() {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.login(getUserFromInput());

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        String responseBody = response.body().string();

                        Log.i(TAG, responseBody);
                        Log.i("RES_MSG", response.message());

                        setUserParams(response.headers(), new JSONObject(responseBody));
                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(LoginActivity.this, invalidLoginMessage);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getApplicationContext(), internalServerErrorMsg, t);
            }
        });
    }

    /**
     * set important values, that will be used accross the application
     * @param headers - contains HTTP headers
     * @param json - response from the server
     */
    private void setUserParams(Headers headers, JSONObject json) throws IOException, JSONException {
        AppHandler.setId(json.getString("_id"));
        AppHandler.setToken(headers.get("x-auth"));
        AppHandler.setUsername(json.getString("username"));
    }

    /**
     * checks if the email input is valid
     * @param email - the email string
     * @return true, if the email contains the <p>@</p> symbol, otherwise false
     */
    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    /**
     * checks if the password is valid
     * @param password - the password string
     * @return true, if the password is greater than or equal to six, otherwise false
     */
    private boolean isPasswordValid(String password) {
        return password.length() >= 6;
    }
}

