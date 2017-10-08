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
 * This activity is responsible for the registration process.
 * @author Yadullah Duman
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    @BindView(R.id.register_email) EditText email;
    @BindView(R.id.register_password) EditText password;
    @BindView(R.id.register_password_confirm) EditText passwordConfirm;
    @BindView(R.id.register_username) EditText username;
    @BindView(R.id.register_first_name) EditText firstname;
    @BindView(R.id.register_last_name) EditText lastname;
    @BindView(R.id.register_age) EditText age;
    @BindView(R.id.register_domicile) EditText domicile;
    @BindView(R.id.register_sign_in_button) Button registerButton;
    @BindView(R.id.register_security_question_1) EditText securityQuestion1;
    @BindView(R.id.register_security_question_2) EditText securityQuestion2;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindString(R.string.error_invalid_registration) String invalidRegistration;
    @BindString(R.string.error_invalid_password) String invalidPassword;
    @BindString(R.string.error_invalid_email) String invalidEmail;
    @BindString(R.string.error_field_required) String fieldRequiredMsg;
    @BindString(R.string.error_internal_server_error) String internalServerErrorMsg;
    @BindString(R.string.error_invalid_username) String invalidUsername;
    @BindString(R.string.error_invalid_password_matching) String invalidPasswordMatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setListeners();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * set the required listeners
     */
    private void setListeners() {
        registerButton.setOnClickListener(view -> attemptRegister());
    }

    /**
     * checks if all fields fulfill the requirements.
     * if the requirements are fulfilled, the client sends the request to the server
     * otherwise the user gets a notified that certain fields do not fulfill the requirements.
     */
    private void attemptRegister() {
        // Reset errors.
        email.setError(null);
        password.setError(null);
        passwordConfirm.setError(null);
        username.setError(null);
        firstname.setError(null);
        lastname.setError(null);
        age.setError(null);
        domicile.setError(null);
        securityQuestion1.setError(null);
        securityQuestion2.setError(null);

        // Store values at the time of the login attempt.
        String email = this.email.getText().toString();
        String password = this.password.getText().toString();
        String passwordConfirm = this.passwordConfirm.getText().toString();
        String username = this.username.getText().toString();
        String firstname = this.firstname.getText().toString();
        String lastname = this.lastname.getText().toString();
        String age = this.age.getText().toString();
        String domicile = this.domicile.getText().toString();
        String securityQuestion1 = this.securityQuestion1.getText().toString();
        String securtiyQuestion2 = this.securityQuestion2.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (isEmptyValue(email)) {
            displayFieldRequiredErrorMsgFor(this.email);
            focusView = this.email;
            cancel = true;
        } else if (!isEmailValid(email)) {
            this.email.setError(invalidEmail);
            focusView = this.email;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (!isEmptyValue(password) && !isValidPassword(password)) {
            this.password.setError(invalidPassword);
            focusView = this.password;
            cancel = true;
        } else if (isEmptyValue(password)) {
            displayFieldRequiredErrorMsgFor(this.password);
            focusView = this.password;
            cancel = true;
        }

        // Check for a valid confirmed, if the user entered one.
        if (!isEmptyValue(passwordConfirm) && !isValidPassword(passwordConfirm)) {
            this.passwordConfirm.setError(invalidPassword);
            focusView = this.passwordConfirm;
            cancel = true;
        } else if (isEmptyValue(passwordConfirm)) {
            displayFieldRequiredErrorMsgFor(this.passwordConfirm);
            focusView = this.passwordConfirm;
            cancel = true;
        }

        // Check that password and confirmed password are matching
        if (!password.equals(passwordConfirm)) {
            this.passwordConfirm.setError(invalidPasswordMatch);
            focusView = this.passwordConfirm;
            cancel = true;
        }

        // Check that other fields are not empty and length is valid
        if (isEmptyValue(username)) {
            displayFieldRequiredErrorMsgFor(this.username);
            focusView = this.username;
            cancel = true;
        } else if (!isValidUsername(username)) {
            this.username.setError(invalidUsername);
            focusView = this.username;
            cancel = true;
        }

        if (isEmptyValue(firstname)) {
            displayFieldRequiredErrorMsgFor(this.firstname);
            focusView = this.firstname;
            cancel = true;
        }

        if (isEmptyValue(lastname)) {
            displayFieldRequiredErrorMsgFor(this.lastname);
            focusView = this.lastname;
            cancel = true;
        }

        if (isEmptyValue(age)) {
            displayFieldRequiredErrorMsgFor(this.age);
            focusView = this.age;
            cancel = true;
        }

        if (isEmptyValue(domicile)) {
            displayFieldRequiredErrorMsgFor(this.domicile);
            focusView = this.domicile;
            cancel = true;
        }

        if (isEmptyValue(securityQuestion1)) {
            displayFieldRequiredErrorMsgFor(this.securityQuestion1);
            focusView = this.securityQuestion1;
            cancel = true;
        }

        if (isEmptyValue(securtiyQuestion2)) {
            displayFieldRequiredErrorMsgFor(this.securityQuestion2);
            focusView = this.securityQuestion2;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            register();
        }
    }

    /**
     * send registration request to server
     */
    private void register() {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.createUser(getUserFromInput());

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        String responseBody = response.body().string();

                        Log.i(TAG, responseBody);
                        Log.i("RES_MSG", response.message());

                        setUserParams(response.headers(), new JSONObject(responseBody));
                        startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 400) {
                    ClientErrorHandler.showMessage(RegisterActivity.this, invalidRegistration);
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
     * helper method to get the user input
     * @return @{@link RequestBody} that represents JSON
     */
    private RequestBody getUserFromInput() {
        Map<String, Object> map = new HashMap<>();
        map.put("email", email.getText().toString());
        map.put("password", password.getText().toString());
        map.put("username", username.getText().toString());
        map.put("firstname", firstname.getText().toString());
        map.put("lastname", lastname.getText().toString());
        map.put("age", Integer.valueOf(age.getText().toString()));
        map.put("domicile", domicile.getText().toString());
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
    private boolean isValidPassword(String password) {
        return password.length() >= 6;
    }

    /**
     * checks if the username typed in by the user is valid
     * the maximum length is 11, due to displaying issues in other activities & fragments
     * @param username - the username typed in by the user
     * @return true, if length is between 1 - 11, otherwise false
     */
    private boolean isValidUsername(String username) {
        return username.length() >= 1 && username.length() <= 11;
    }

    /**
     * helper method to check if a field is left empty
     * @param value - the value inside of the field
     * @return true, if value is empty, otherwise false
     */
    private boolean isEmptyValue(String value) {
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

