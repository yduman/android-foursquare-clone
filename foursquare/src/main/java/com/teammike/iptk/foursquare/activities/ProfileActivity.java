package com.teammike.iptk.foursquare.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.fragments.DeleteProfileFragment;
import com.teammike.iptk.foursquare.fragments.EditProfileFragment;
import com.teammike.iptk.foursquare.fragments.ProfileFragment;
import com.teammike.iptk.foursquare.fragments.ResetPasswordFragment;
import com.teammike.iptk.foursquare.fragments.UserProfileFragment;
import com.teammike.iptk.foursquare.utils.ClientErrorHandler;
import com.teammike.iptk.foursquare.utils.Constants;
import com.teammike.iptk.foursquare.utils.AppHandler;
import com.teammike.iptk.foursquare.utils.FragmentHandler;
import com.teammike.iptk.foursquare.utils.RestClient;
import com.teammike.iptk.foursquare.utils.Api;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindString;
import butterknife.BindView;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This activity is responsible for all profile actions.
 * Users can view, edit or delete a profile. Furthermore they can log out of the application.
 * @author Yadullah Duman
 */
public class ProfileActivity extends BaseActivity {

    private static final String TAG = "ProfileActivity";
    private FragmentManager fragmentManager = getFragmentManager();
    private int fragmentContainer = R.id.activity_profile;
    private ProfileFragment profileFragment;
    private UserProfileFragment userProfileFragment;
    private EditProfileFragment editProfileFragment;
    private DeleteProfileFragment deleteProfileFragment;
    private ResetPasswordFragment resetPasswordFragment;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindString(R.string.error_invalid_log_out) String invalidLogoutMsg;
    @BindString(R.string.error_internal_server_error) String internalServerErrorMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        initFragments();

        String userId = getUserIdFromIntent();

        if (AppHandler.getId().equals(userId)) {
            FragmentHandler.addFragment(fragmentContainer, profileFragment, fragmentManager);
        } else {
            FragmentHandler.addFragment(fragmentContainer, userProfileFragment, fragmentManager);
        }
    }

    private String getUserIdFromIntent() {
        try {
            String jsonString = getIntentExtra().getString(Constants.INTENT_KEY);
            JSONObject json = new JSONObject(jsonString);
            return json.getString("_id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        int count = getFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            finish();
            startActivity(new Intent(this, HomeActivity.class));
        } else
            getFragmentManager().popBackStack();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings_edit_profile)
            FragmentHandler.replaceFragment(fragmentContainer, editProfileFragment, fragmentManager);

        if (item.getItemId() == R.id.settings_reset_password)
            FragmentHandler.replaceFragment(fragmentContainer, resetPasswordFragment, fragmentManager);

        if (item.getItemId() == R.id.settings_delete_account)
            FragmentHandler.replaceFragment(fragmentContainer, deleteProfileFragment, fragmentManager);

        if (item.getItemId() == R.id.settings_log_out)
            logout();

        return super.onOptionsItemSelected(item);
    }

    /**
     * helper to get response from the previous intent, calling this activity
     * @return user object returned from server
     */
    private Bundle getIntentExtra() {
        Intent intent = getIntent();
        return intent.getExtras();
    }

    /**
     * helper method to initialize fragments used by this activity
     */
    private void initFragments() {
        profileFragment = (ProfileFragment) Fragment.instantiate(this, ProfileFragment.class.getName(), getIntentExtra());
        userProfileFragment = (UserProfileFragment) Fragment.instantiate(this, UserProfileFragment.class.getName(), getIntentExtra());
        editProfileFragment = (EditProfileFragment) Fragment.instantiate(this, EditProfileFragment.class.getName(), getIntentExtra());
        deleteProfileFragment = (DeleteProfileFragment) Fragment.instantiate(this, DeleteProfileFragment.class.getName(), getIntentExtra());
        resetPasswordFragment = (ResetPasswordFragment) Fragment.instantiate(this, ResetPasswordFragment.class.getName(), getIntentExtra());
    }

    /**
     * send logout request to server
     */
    private void logout() {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.logout();

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200)
                    startActivity(new Intent(ProfileActivity.this, LoginActivity.class).putExtra(Constants.ACTION_KEY, Constants.ACTION_LOGOUT_VALUE));
                else if (response.code() == 400)
                    ClientErrorHandler.showMessage(ProfileActivity.this, invalidLogoutMsg);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getApplicationContext(), internalServerErrorMsg, t);
            }
        });
    }

    @Override
    int getContentViewId() {
        return R.layout.activity_profile;
    }

    @Override
    int getNavigationMenuItem() {
        return R.id.navigation_profile;
    }

}
