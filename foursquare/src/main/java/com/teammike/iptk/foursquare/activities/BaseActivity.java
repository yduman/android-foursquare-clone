package com.teammike.iptk.foursquare.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.utils.AppHandler;
import com.teammike.iptk.foursquare.utils.ClientErrorHandler;
import com.teammike.iptk.foursquare.utils.RestClient;
import com.teammike.iptk.foursquare.utils.Constants;
import com.teammike.iptk.foursquare.utils.Api;

import java.io.IOException;
import java.lang.reflect.Field;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This class represents all activites, that will make use of the BottomNavigationView.
 * The BottomNavigationView is actually responsible for handling fragments, but we are using it for handling activities.
 * @author Yadullah Duman
 */
public abstract class BaseActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "BaseActivity";

    @BindView(R.id.navigation) BottomNavigationView navigation;
    @BindString(R.string.error_not_authenticated) String invalidAuthenticationMsg;
    @BindString(R.string.error_internal_server_error) String internalServerErrorMsg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewId());
        ButterKnife.bind(this);

        navigation.setOnNavigationItemSelectedListener(this);
        disableShiftMode(navigation);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateNavigationBarState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }

    /**
     * if we select the home button, we start the  @{@link HomeActivity}.
     * if we select the profile button, we start the @{@link ProfileActivity}
     * if we select the friends button, we start the @{@link FriendsActivity}
     * if we select the check-ins button, we start the {@link CheckInsActivity}
     * @param item - represents an item on the BottomNavigationBar
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        navigation.postDelayed(() -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(this, HomeActivity.class));
            } else if (itemId == R.id.navigation_profile) {
                getUser(ProfileActivity.class);
            } else if (itemId == R.id.navigation_friends) {
                getUser(FriendsActivity.class);
            } else if (itemId == R.id.navigation_checkIns) {
                getUser(CheckInsActivity.class);
            } else if (itemId == R.id.navigation_chat) {
                startActivity(new Intent(this, ChatLoginActivity.class));
            }
            finish();
        }, 0);
        return true;
    }

    /**
     * this methods updates the navigation bar state, when selecting another menu item.
     */
    private void updateNavigationBarState() {
        int itemId = getNavigationMenuItem();
        selectBottomNavigationBarItem(itemId);
    }

    /**
     * this method sets a checked state for a given menu item.
     * @param itemId - the id of the menu item
     */
    void selectBottomNavigationBarItem(int itemId) {
        Menu menu = navigation.getMenu();
        for (int i = 0, size = menu.size(); i < size; i++) {
            MenuItem item = menu.getItem(i);
            boolean shouldBeChecked = item.getItemId() == itemId;
            if (shouldBeChecked) {
                item.setChecked(true);
                break;
            }
        }
    }

    /**
     * client calling REST API for getting friend requests and adding the response to the intent as a bundle.
     */
    private void getUser(Class activityToGo) {
        RestClient client = Api.getInstance().getClient();
        Call<ResponseBody> call = client.getUser();

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        String responseBody = response.body().string();
                        logResponse(response, responseBody);
                        startActivity(new Intent(BaseActivity.this, activityToGo).putExtra(Constants.INTENT_KEY, responseBody));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 401) {
                    ClientErrorHandler.showMessage(BaseActivity.this, invalidAuthenticationMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getApplicationContext(), internalServerErrorMsg, t);
            }
        });
    }

    /**
     * method for logging response for more information
     * @param response - the response object from retrofit
     * @param responseBody - the json returned from the server
     */
    private void logResponse(Response<ResponseBody> response, String responseBody) {
        Log.i(TAG, responseBody);
        Log.i("RES_MSG", response.message());
    }

    @SuppressLint("RestrictedApi")
    private void disableShiftMode(BottomNavigationView view) {
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) view.getChildAt(0);
        try {
            Field shiftingMode = menuView.getClass().getDeclaredField("mShiftingMode");
            shiftingMode.setAccessible(true);
            shiftingMode.setBoolean(menuView, false);
            shiftingMode.setAccessible(false);
            for (int i = 0; i < menuView.getChildCount(); i++) {
                BottomNavigationItemView item = (BottomNavigationItemView) menuView.getChildAt(i);
                item.setShiftingMode(false);
                // set once again checked value, so view will be updated
                item.setChecked(item.getItemData().isChecked());
            }
        } catch (NoSuchFieldException e) {
            Log.e("BNVHelper", "Unable to get shift mode field", e);
        } catch (IllegalAccessException e) {
            Log.e("BNVHelper", "Unable to change value of shift mode", e);
        }
    }

    /**
     * returns the layout id of the activity that extends this activity
     * @return the layout id of the activity that extends this activity
     */
    abstract int getContentViewId();

    /**
     * returns the menu item id of the activity that extends this activity
     * @return the menu item id of the activity that extends this activity
     */
    abstract int getNavigationMenuItem();
}
